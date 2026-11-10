package com.notifyhub.notification.worker;

import com.notifyhub.notification.queue.RabbitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final NotificationProcessor processor;
    private final NotificationDeliverer deliverer;

    @RabbitListener(queues = RabbitConfig.EMAIL_QUEUE)
    public void onEmail(String publicId, com.rabbitmq.client.Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        handle("email", publicId, channel, tag);
    }

    @RabbitListener(queues = RabbitConfig.SMS_QUEUE)
    public void onSms(String publicId, com.rabbitmq.client.Channel channel,
                      @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        handle("sms", publicId, channel, tag);
    }

    @RabbitListener(queues = RabbitConfig.PUSH_QUEUE)
    public void onPush(String publicId, com.rabbitmq.client.Channel channel,
                       @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        handle("push", publicId, channel, tag);
    }

    private void handle(String queue, String publicId,
                        com.rabbitmq.client.Channel channel, long tag) throws IOException {
        try {
            ClaimResult claim = processor.claim(publicId);
            if (claim != ClaimResult.CLAIMED) {
                log.info("[{}] Skipping {}: {}", queue, publicId, claim);
                channel.basicAck(tag, false);
                return;
            }

            NotificationDeliverer.Delivery delivery = deliverer.deliver(publicId);
            processor.complete(publicId, delivery.outcome(), delivery.providerName());
            channel.basicAck(tag, false);
            log.info("[{}] {} -> {} via {}", queue, publicId, delivery.outcome().status(),
                    delivery.providerName());

        } catch (ObjectOptimisticLockingFailureException ex) {
            log.info("[{}] Lost the race for {}: another worker owns it", queue, publicId);
            channel.basicAck(tag, false);

        } catch (Exception ex) {
            log.error("[{}] Failed to process {}", queue, publicId, ex);
            channel.basicNack(tag, false, false);
        }
    }
}