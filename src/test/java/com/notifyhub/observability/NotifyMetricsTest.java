package com.notifyhub.observability;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.common.enums.NotificationStatus;
import com.notifyhub.provider.SendOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotifyMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final NotifyMetrics metrics = new NotifyMetrics(registry);

    @Test
    void acceptedNotificationsAreCountedPerChannelAndStatus() {
        metrics.accepted(Channel.EMAIL, NotificationStatus.QUEUED);
        metrics.accepted(Channel.EMAIL, NotificationStatus.QUEUED);
        metrics.accepted(Channel.EMAIL, NotificationStatus.SKIPPED);

        assertEquals(2.0, registry.get("notifyhub.notifications.accepted")
                .tag("channel", "email").tag("status", "queued").counter().count());
        assertEquals(1.0, registry.get("notifyhub.notifications.accepted")
                .tag("channel", "email").tag("status", "skipped").counter().count());
    }

    @Test
    void outcomesUseFriendlyNames() {
        metrics.outcome("email", SendOutcome.Status.SUCCESS);
        metrics.outcome("email", SendOutcome.Status.RETRYABLE_FAILURE);
        metrics.outcome("email", SendOutcome.Status.PERMANENT_FAILURE);

        for (String outcome : new String[]{"sent", "retry", "failed"}) {
            assertEquals(1.0, registry.get("notifyhub.notifications.outcome")
                    .tag("channel", "email").tag("outcome", outcome).counter().count());
        }
    }

    @Test
    void providerCallsAreTimed() {
        metrics.providerCall("sendgrid", SendOutcome.Status.SUCCESS, Duration.ofMillis(200));
        metrics.providerCall("sendgrid", SendOutcome.Status.SUCCESS, Duration.ofMillis(400));

        var timer = registry.get("notifyhub.provider.latency")
                .tag("provider", "sendgrid").tag("outcome", "success").timer();
        assertEquals(2, timer.count());
        assertEquals(600.0, timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS), 0.5);
    }

    @Test
    void webhookResultsAreCounted() {
        metrics.webhook("applied");
        metrics.webhook("rejected");
        metrics.webhook("rejected");

        assertEquals(2.0, registry.get("notifyhub.webhooks.received").tag("result", "rejected").counter().count());
    }
}