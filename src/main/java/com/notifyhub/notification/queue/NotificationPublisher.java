package com.notifyhub.notification.queue;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.notification.NotificationQueuedEvent;
import com.notifyhub.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private static final long CONFIRM_TIMEOUT_MS = 5_000;

    private final RabbitTemplate rabbitTemplate;
    private final NotificationRepository notificationRepository;

    // Runs only if the transaction that published the event actually COMMITTED.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueued(NotificationQueuedEvent event) {
        publish(event.id(), event.publicId(), event.channel());
    }

    /** Returns true if the broker confirmed the message. Never throws: failures are left for the sweeper. */
    public boolean publish(Long id, String publicId, Channel channel) {
        try {
            rabbitTemplate.invoke(ops -> {
                ops.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.routingKey(channel), publicId);
                ops.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);   // wait for the broker's ack
                return true;
            });
            notificationRepository.markPublished(id, Instant.now());
            log.info("Published notification {} to {}", publicId, RabbitConfig.routingKey(channel));
            return true;
        } catch (Exception ex) {
            // The row stays QUEUED with published_at = NULL, so the sweeper will retry.
            log.warn("Could not publish notification {}: {}", publicId, ex.getMessage());
            return false;
        }
    }
}