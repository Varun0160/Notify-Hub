package com.notifyhub.notification.queue;

import com.notifyhub.observability.CorrelationId;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
public class UnpublishedNotificationSweeper {

    private static final int BATCH_SIZE = 100;

    private final SweepClaimer claimer;
    private final NotificationPublisher publisher;
    private final long minAgeSeconds;
    private final long leaseSeconds;

    public UnpublishedNotificationSweeper(
            SweepClaimer claimer,
            NotificationPublisher publisher,
            @Value("${notifyhub.sweeper.min-age-seconds:15}") long minAgeSeconds,
            @Value("${notifyhub.sweeper.lease-seconds:60}") long leaseSeconds) {
        this.claimer = claimer;
        this.publisher = publisher;
        this.minAgeSeconds = minAgeSeconds;
        this.leaseSeconds = leaseSeconds;
    }

    // Deliberately NOT @Transactional: claiming is one short transaction, publishing happens outside.
    @Scheduled(fixedDelayString = "${notifyhub.sweeper.interval-ms:2000}")
    public void sweep() {
        MDC.put(CorrelationId.MDC_KEY, "sweep-" + CorrelationId.newId().substring(0, 8));
        try {
            Instant now = Instant.now();
            List<SweepClaimer.Claimed> batch = claimer.claimBatch(
                    now, now.minusSeconds(minAgeSeconds), BATCH_SIZE, Duration.ofSeconds(leaseSeconds));

            if (batch.isEmpty()) {
                return;
            }
            log.info("Sweeper claimed {} notification(s)", batch.size());
            for (SweepClaimer.Claimed c : batch) {
                if (!publisher.publish(c.id(), c.publicId(), c.channel())) {
                    break;   // broker probably down: the leases expire and a later sweep retries
                }
            }
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
        }
    }
}