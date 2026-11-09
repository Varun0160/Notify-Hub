package com.notifyhub.notification.queue;

import com.notifyhub.common.enums.Channel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoutingKeyTest {

    @Test
    void routingKeysAreLowercaseChannelNames() {
        assertEquals("email", RabbitConfig.routingKey(Channel.EMAIL));
        assertEquals("sms", RabbitConfig.routingKey(Channel.SMS));
        assertEquals("push", RabbitConfig.routingKey(Channel.PUSH));
    }
}