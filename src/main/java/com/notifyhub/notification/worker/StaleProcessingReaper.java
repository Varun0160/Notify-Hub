package com.notifyhub.notification.worker;

import com.notifyhub.notification.Notification;
import com.notifyhub.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaleProcessingReaper {

    private static final int BATCH_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationProcessor processor;

    @Value("${notifyhub.reaper.stale-after-seconds:120}")
    private long staleAfterSeconds;

    @Scheduled(fixedDelayString = "${notifyhub.reaper.interval-ms:30000}")
    public void reap() {
        Instant cutoff = Instant.now().minusSeconds(staleAfterSeconds);
        List<Notification> stale = notificationRepository.findStaleProcessing(
                cutoff, PageRequest.of(0, BATCH_SIZE));

        for (Notification n : stale) {
            try {
                if (processor.recoverStale(n.getId())) {
                    log.warn("Recovered stale notification {} (stuck in PROCESSING)", n.getPublicId());
                }
            } catch (ObjectOptimisticLockingFailureException ex) {
                log.info("Notification {} changed while being recovered: leaving it alone", n.getPublicId());
            } catch (Exception ex) {
                log.error("Could not recover notification {}", n.getPublicId(), ex);
            }
        }
    }
}