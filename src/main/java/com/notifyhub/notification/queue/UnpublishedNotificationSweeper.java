package com.notifyhub.notification.queue;

import com.notifyhub.notification.Notification;
import com.notifyhub.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UnpublishedNotificationSweeper {

    private static final int BATCH_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationPublisher publisher;

    @Value("${notifyhub.sweeper.min-age-seconds:15}")
    private long minAgeSeconds;

    @Scheduled(fixedDelayString = "${notifyhub.sweeper.interval-ms:10000}")
    public void sweep() {
        Instant now = Instant.now();
        List<Notification> pending = notificationRepository.findUnpublished(
                now, now.minus(Duration.ofSeconds(minAgeSeconds)), PageRequest.of(0, BATCH_SIZE));

        if (pending.isEmpty()) {
            return;
        }
        log.info("Sweeper found {} unpublished notification(s)", pending.size());
        for (Notification n : pending) {
            if (!publisher.publish(n.getId(), n.getPublicId(), n.getChannel())) {
                break;   // the broker is probably down: stop and try again next round
            }
        }
    }
}