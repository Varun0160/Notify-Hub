package com.notifyhub.notification.queue;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.notification.NotificationQueuedEvent;
import com.notifyhub.notification.NotificationRepository;
import com.notifyhub.observability.CorrelationId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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
        String correlationId = MDC.get(CorrelationId.MDC_KEY);   // set by the request filter or the sweeper
        try {
            rabbitTemplate.invoke(ops -> {
                ops.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.routingKey(channel), publicId,
                        message -> {
                            if (correlationId != null) {
                                message.getMessageProperties().setHeader(CorrelationId.HEADER, correlationId);
                            }
                            return message;
                        });
                ops.waitForConfirmsOrDie(CONFIRM_TIMEOUT_MS);
                return true;
            });
            notificationRepository.markPublished(id, Instant.now());
            log.info("Published notification {} to {}", publicId, RabbitConfig.routingKey(channel));
            return true;
        } catch (Exception ex) {
            log.warn("Could not publish notification {}: {}", publicId, ex.getMessage());
            return false;
        }
    }
}