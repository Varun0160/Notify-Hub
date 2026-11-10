package com.notifyhub.notification.queue;

import com.notifyhub.common.enums.Channel;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "notifyhub.notifications";
    public static final String EMAIL_QUEUE = "notifications.email";
    public static final String SMS_QUEUE = "notifications.sms";
    public static final String PUSH_QUEUE = "notifications.push";

    public static final String DEAD_LETTER_EXCHANGE = "notifyhub.notifications.dlx";
    public static final String DEAD_LETTER_QUEUE = "notifications.dlq";
    private static final String DEAD_KEY = "dead";

    public static String routingKey(Channel channel) {
        return channel.name().toLowerCase();
    }

    // Every main queue sends rejected messages to the dead-letter exchange.
    private Queue mainQueue(String name) {
        return QueueBuilder.durable(name)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_KEY)
                .build();
    }

    @Bean
    DirectExchange notificationExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    Queue emailQueue() {
        return mainQueue(EMAIL_QUEUE);
    }

    @Bean
    Queue smsQueue() {
        return mainQueue(SMS_QUEUE);
    }

    @Bean
    Queue pushQueue() {
        return mainQueue(PUSH_QUEUE);
    }

    @Bean
    Binding emailBinding(DirectExchange notificationExchange, Queue emailQueue) {
        return BindingBuilder.bind(emailQueue).to(notificationExchange).with(routingKey(Channel.EMAIL));
    }

    @Bean
    Binding smsBinding(DirectExchange notificationExchange, Queue smsQueue) {
        return BindingBuilder.bind(smsQueue).to(notificationExchange).with(routingKey(Channel.SMS));
    }

    @Bean
    Binding pushBinding(DirectExchange notificationExchange, Queue pushQueue) {
        return BindingBuilder.bind(pushQueue).to(notificationExchange).with(routingKey(Channel.PUSH));
    }

    // ---- dead-letter topology ----

    @Bean
    DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    Binding deadLetterBinding(DirectExchange deadLetterExchange, Queue deadLetterQueue) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_KEY);
    }
}