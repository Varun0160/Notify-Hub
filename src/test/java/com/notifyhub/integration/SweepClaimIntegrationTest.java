package com.notifyhub.integration;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.notification.Notification;
import com.notifyhub.notification.queue.SweepClaimer;
import com.notifyhub.recipient.Recipient;
import com.notifyhub.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Several "instances" claim at once. No row may be claimed twice. Needs real MySQL (SKIP LOCKED). */
class SweepClaimIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SweepClaimer claimer;

    @Test
    void concurrentSweepersNeverClaimTheSameRow() throws Exception {
        Tenancy t = newTenant();
        Recipient r = newRecipient(t.tenant());
        var template = templateRepository.findByTenantIdAndNameAndChannelAndActiveTrue(
                t.tenant().getId(), "welcome", Channel.EMAIL).orElseThrow();

        Set<Long> ours = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            Notification n = new Notification(t.tenant(), r, template, Channel.EMAIL);
            n.setPayload("{\"name\":\"Ravi\"}");
            n.setScheduledAt(Instant.now().minusSeconds(60));          // due, unpublished, no lease
            ours.add(notificationRepository.save(n).getId());
        }

        int sweepers = 3;
        ExecutorService pool = Executors.newFixedThreadPool(sweepers);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<List<SweepClaimer.Claimed>>> futures = new ArrayList<>();
        for (int i = 0; i < sweepers; i++) {
            futures.add(pool.submit(() -> {
                go.await();
                Instant now = Instant.now();
                return claimer.claimBatch(now, now.minusSeconds(15), 10, Duration.ofSeconds(60));
            }));
        }
        go.countDown();

        List<Long> claimedOurs = new ArrayList<>();
        for (Future<List<SweepClaimer.Claimed>> f : futures) {
            for (SweepClaimer.Claimed c : f.get()) {
                if (ours.contains(c.id())) {
                    claimedOurs.add(c.id());
                }
            }
        }
        pool.shutdown();

        assertEquals(claimedOurs.size(), new HashSet<>(claimedOurs).size(), "a row was claimed twice");
        assertEquals(20, claimedOurs.size(), "every due row must be claimed by someone");
        assertTrue(notificationRepository.findById(claimedOurs.get(0)).orElseThrow().getLeaseUntil() != null);
    }
}