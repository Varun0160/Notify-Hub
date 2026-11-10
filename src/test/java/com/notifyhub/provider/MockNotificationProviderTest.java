package com.notifyhub.provider;

import com.notifyhub.common.enums.Channel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MockNotificationProviderTest {

    private final MockNotificationProvider provider = new MockNotificationProvider(Channel.EMAIL);

    private OutboundMessage msg(String to, String body) {
        return new OutboundMessage(Channel.EMAIL, to, "subject", body);
    }

    @Test
    void normalMessageSucceedsWithAnId() {
        SendOutcome outcome = provider.send(msg("ravi@example.com", "hello"));
        assertEquals(SendOutcome.Status.SUCCESS, outcome.status());
        assertNotNull(outcome.providerMessageId());
    }

    @Test
    void retryMarkerGivesRetryableFailure() {
        assertEquals(SendOutcome.Status.RETRYABLE_FAILURE,
                provider.send(msg("ravi+fail-retry@example.com", "hello")).status());
    }

    @Test
    void permanentMarkerGivesPermanentFailure() {
        assertEquals(SendOutcome.Status.PERMANENT_FAILURE,
                provider.send(msg("ravi@example.com", "please fail-permanent")).status());
    }

    @Test
    void reportsItsChannelAndName() {
        assertEquals(Channel.EMAIL, provider.channel());
        assertEquals("mock-email", provider.name());
    }
}