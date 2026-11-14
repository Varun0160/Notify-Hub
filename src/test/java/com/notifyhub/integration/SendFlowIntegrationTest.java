package com.notifyhub.integration;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.notification.Notification;
import com.notifyhub.notification.NotificationAttempt;
import com.notifyhub.recipient.Recipient;
import com.notifyhub.recipient.RecipientPreference;
import com.notifyhub.support.BaseIntegrationTest;
import com.notifyhub.support.TestApi;
import com.notifyhub.webhook.WebhookSignature;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SendFlowIntegrationTest extends BaseIntegrationTest {

    private Map<String, String> key(Tenancy t) {
        return Map.of("X-API-Key", t.apiKey());
    }

    private NotificationStatus awaitStatus(String publicId, NotificationStatus expected) throws Exception {
        NotificationStatus last = null;
        for (int i = 0; i < 150; i++) {                      // up to 15 seconds
            last = notificationRepository.findByPublicId(publicId).orElseThrow().getStatus();
            if (last == expected) {
                return last;
            }
            Thread.sleep(100);
        }
        return last;
    }

    @Test
    void anAcceptedNotificationIsDeliveredByTheWorker() throws Exception {
        Tenancy t = newTenant();
        Recipient r = newRecipient(t.tenant());

        TestApi.Response response = api().post("/api/v1/notifications", sendBody(r.getPublicId()), key(t));

        assertEquals(202, response.status());
        assertTrue(response.header("Location").endsWith(response.id()));
        assertTrue(response.header("X-Correlation-Id") != null);
        assertEquals(NotificationStatus.SENT, awaitStatus(response.id(), NotificationStatus.SENT));

        Notification n = notificationRepository.findByPublicId(response.id()).orElseThrow();
        assertEquals(1, attemptRepository.findByNotificationIdOrderByAttemptNoAsc(n.getId()).size());
    }

    @Test
    void aSignedWebhookMovesSentToDelivered() throws Exception {
        Tenancy t = newTenant();
        Recipient r = newRecipient(t.tenant());
        String id = api().post("/api/v1/notifications", sendBody(r.getPublicId()), key(t)).id();
        assertEquals(NotificationStatus.SENT, awaitStatus(id, NotificationStatus.SENT));

        Notification n = notificationRepository.findByPublicId(id).orElseThrow();
        NotificationAttempt attempt = attemptRepository.findByNotificationIdOrderByAttemptNoAsc(n.getId()).get(0);

        String body = "{\"eventId\":\"evt-" + id + "\",\"providerMessageId\":\""
                + attempt.getProviderMessageId() + "\",\"type\":\"delivered\"}";
        long ts = Instant.now().getEpochSecond();
        Map<String, String> headers = Map.of("X-Timestamp", String.valueOf(ts),
                "X-Signature", WebhookSignature.sign(webhookSecret, ts, body));

        TestApi.Response ok = api().post("/api/v1/webhooks/provider", body, headers);
        TestApi.Response replay = api().post("/api/v1/webhooks/provider", body, headers);
        TestApi.Response forged = api().post("/api/v1/webhooks/provider", body,
                Map.of("X-Timestamp", String.valueOf(ts), "X-Signature", "0".repeat(64)));

        assertEquals(200, ok.status());
        assertTrue(ok.body().contains("APPLIED"));
        assertTrue(replay.body().contains("DUPLICATE"));
        assertEquals(401, forged.status());
        assertEquals(NotificationStatus.DELIVERED, awaitStatus(id, NotificationStatus.DELIVERED));
    }

    @Test
    void anotherTenantCannotSeeOrUseSomeoneElsesData() {
        Tenancy a = newTenant();
        Tenancy b = newTenant();
        Recipient recipientOfA = newRecipient(a.tenant());
        String id = api().post("/api/v1/notifications", sendBody(recipientOfA.getPublicId()), key(a)).id();

        // B cannot read A's notification, or send to A's recipient. 404 (not 403) leaks nothing.
        assertEquals(404, api().get("/api/v1/notifications/" + id, key(b)).status());
        assertEquals(404, api().get("/api/v1/recipients/" + recipientOfA.getPublicId(), key(b)).status());
        assertEquals(404, api().post("/api/v1/notifications",
                sendBody(recipientOfA.getPublicId()), key(b)).status());
        // A still can
        assertEquals(200, api().get("/api/v1/notifications/" + id, key(a)).status());
    }

    @Test
    void authenticationAndAuthorizationAreEnforced() {
        Tenancy t = newTenant();

        assertEquals(401, api().get("/api/v1/me", Map.of()).status());
        assertEquals(401, api().get("/api/v1/me", Map.of("X-API-Key", "nh_wrong")).status());
        assertEquals(200, api().get("/api/v1/me", key(t)).status());
        // authenticated as a tenant, but tenants may not use admin endpoints
        assertEquals(403, api().get("/api/v1/tenants/" + t.tenant().getPublicId(), key(t)).status());
        // metrics are admin-only
        assertEquals(403, api().get("/actuator/metrics", key(t)).status());
        assertEquals(200, api().get("/actuator/health", Map.of()).status());
    }

    @Test
    void aRetriedRequestWithTheSameKeyReplaysTheOriginal() {
        Tenancy t = newTenant();
        Recipient r = newRecipient(t.tenant());
        Map<String, String> headers = Map.of("X-API-Key", t.apiKey(), "Idempotency-Key", "order-1");

        TestApi.Response first = api().post("/api/v1/notifications", sendBody(r.getPublicId()), headers);
        TestApi.Response second = api().post("/api/v1/notifications", sendBody(r.getPublicId()), headers);
        TestApi.Response different = api().post("/api/v1/notifications",
                sendBody(r.getPublicId()).replace("Ravi", "Raj"), headers);

        assertEquals(202, first.status());
        assertEquals(first.id(), second.id());
        assertEquals("true", second.header("Idempotent-Replayed"));
        assertEquals(422, different.status());
    }

    @Test
    void concurrentDuplicateRequestsCreateExactlyOneNotification() throws Exception {
        Tenancy t = newTenant();
        Recipient r = newRecipient(t.tenant());
        Map<String, String> headers = Map.of("X-API-Key", t.apiKey(), "Idempotency-Key", "race-1");
        int threads = 8;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<TestApi.Response>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<TestApi.Response> call = () -> {
                go.await();                                           // everyone starts together
                return api().post("/api/v1/notifications", sendBody(r.getPublicId()), headers);
            };
            futures.add(pool.submit(call));
        }
        go.countDown();

        Set<String> ids = new HashSet<>();
        for (Future<TestApi.Response> f : futures) {
            TestApi.Response response = f.get();
            assertEquals(202, response.status(), response.body());     // losers get the winner's result, not an error
            ids.add(response.id());
        }
        pool.shutdown();

        assertEquals(1, ids.size(), "all callers must see the same notification");
        assertTrue(notificationRepository.findByTenantIdAndIdempotencyKey(t.tenant().getId(), "race-1").isPresent());
    }

    @Test
    void anOptedOutRecipientGetsASkippedRecordAndNoDelivery() throws Exception {
        Tenancy t = newTenant();
        Recipient r = newRecipient(t.tenant());
        preferenceRepository.save(new RecipientPreference(r, Channel.EMAIL, false));

        TestApi.Response response = api().post("/api/v1/notifications", sendBody(r.getPublicId()), key(t));

        assertEquals(202, response.status());
        assertTrue(response.body().contains("SKIPPED"));
        Thread.sleep(1500);   // give a (wrongly) published message time to be processed
        Notification n = notificationRepository.findByPublicId(response.id()).orElseThrow();
        assertEquals(NotificationStatus.SKIPPED, n.getStatus());
        assertEquals(0, attemptRepository.findByNotificationIdOrderByAttemptNoAsc(n.getId()).size());
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.notifyhub.recipient.RecipientPreferenceRepository preferenceRepository;
}