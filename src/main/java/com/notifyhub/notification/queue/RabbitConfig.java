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

    public static String routingKey(Channel channel) {
        return channel.name().toLowerCase();
    }

    @Bean
    DirectExchange notificationExchange() {
        return new DirectExchange(EXCHANGE, true, false);   // durable, not auto-delete
    }

    @Bean
    Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).build();
    }

    @Bean
    Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE).build();
    }

    @Bean
    Queue pushQueue() {
        return QueueBuilder.durable(PUSH_QUEUE).build();
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
}