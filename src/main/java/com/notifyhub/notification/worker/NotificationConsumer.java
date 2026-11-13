package com.notifyhub.notification.worker;

import com.notifyhub.notification.queue.RabbitConfig;
import com.notifyhub.observability.CorrelationId;
import com.notifyhub.observability.NotifyMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private static final String NOTIFICATION_KEY = "notificationId";

    private final NotificationProcessor processor;
    private final NotificationDeliverer deliverer;
    private final NotifyMetrics metrics;

    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void onEmail(String publicId, com.rabbitmq.client.Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                        @Header(name = CorrelationId.HEADER, required = false) String correlationId)
            throws IOException {
        handle("email", publicId, channel, tag, correlationId);
    }

    @RabbitListener(queues = RabbitConfig.SMS_QUEUE)
    public void onSms(String publicId, com.rabbitmq.client.Channel channel,
                      @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                      @Header(name = CorrelationId.HEADER, required = false) String correlationId)
            throws IOException {
        handle("sms", publicId, channel, tag, correlationId);
    }

    @RabbitListener(queues = RabbitConfig.PUSH_QUEUE)
    public void onPush(String publicId, com.rabbitmq.client.Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                       @Header(name = CorrelationId.HEADER, required = false) String correlationId)
            throws IOException {
        handle("push", publicId, channel, tag, correlationId);
    }

    private void handle(String queue, String publicId, com.rabbitmq.client.Channel channel,
                        long tag, String correlationHeader) throws IOException {
        // Consumer threads are reused, so set both keys now and ALWAYS remove them in finally.
        MDC.put(CorrelationId.MDC_KEY, CorrelationId.fromHeaderOrNew(correlationHeader));
        MDC.put(NOTIFICATION_KEY, publicId);
        try {
            ClaimResult claim = processor.claim(publicId);
            if (claim != ClaimResult.CLAIMED) {
                log.info("[{}] Skipping {}: {}", queue, publicId, claim);
                channel.basicAck(tag, false);
                return;
            }

            NotificationDeliverer.Delivery delivery = deliverer.deliver(publicId);
            processor.complete(publicId, delivery.outcome(), delivery.providerName());
            metrics.outcome(queue, delivery.outcome().status());
            channel.basicAck(tag, false);
            log.info("[{}] {} -> {} via {}", queue, publicId, delivery.outcome().status(),
                    delivery.providerName());

        } catch (ObjectOptimisticLockingFailureException ex) {
            log.info("[{}] Lost the race for {}: another worker owns it", queue, publicId);
            channel.basicAck(tag, false);

        } catch (Exception ex) {
            log.error("[{}] Failed to process {}", queue, publicId, ex);
            channel.basicNack(tag, false, false);       // goes to the dead-letter queue (Day 13)
        } finally {
            MDC.remove(CorrelationId.MDC_KEY);
            MDC.remove(NOTIFICATION_KEY);
        }
    }
}