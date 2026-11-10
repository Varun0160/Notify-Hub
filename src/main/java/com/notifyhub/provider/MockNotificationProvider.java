package com.notifyhub.provider;

import com.notifyhub.common.enums.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class MockNotificationProvider implements NotificationProvider {

    private final Channel channel;

    public MockNotificationProvider(Channel channel) {
        this.channel = channel;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public String name() {
        return "mock-" + channel.name().toLowerCase();
    }

    @Override
    public SendOutcome send(OutboundMessage message) {
        // Test hooks: put these markers in the recipient address or the body to simulate failures.
        String text = (message.to() + " " + message.body()).toLowerCase();
        if (text.contains("fail-permanent")) {
            return SendOutcome.permanent("Mock: permanent failure requested");
        }
        if (text.contains("fail-retry")) {
            return SendOutcome.retryable("Mock: temporary failure requested");
        }
        log.info("[{}] pretending to send a message", name());   // no recipient details in logs
        return SendOutcome.success("mock-" + UUID.randomUUID());
    }
}