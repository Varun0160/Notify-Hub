package com.notifyhub.provider;

import com.notifyhub.common.enums.Channel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProviderRegistryTest {

    private NotificationProvider mock(Channel c) {
        return new MockNotificationProvider(c);
    }

    @Test
    void mapsEachChannelToItsProvider() {
        NotificationProvider email = mock(Channel.EMAIL);
        ProviderRegistry registry = new ProviderRegistry(
                List.of(email, mock(Channel.SMS), mock(Channel.PUSH)));
        assertSame(email, registry.forChannel(Channel.EMAIL));
    }

    @Test
    void twoProvidersForOneChannelIsRejected() {
        assertThrows(IllegalStateException.class, () -> new ProviderRegistry(
                List.of(mock(Channel.EMAIL), mock(Channel.EMAIL), mock(Channel.SMS), mock(Channel.PUSH))));
    }

    @Test
    void aMissingChannelIsRejected() {
        assertThrows(IllegalStateException.class, () -> new ProviderRegistry(
                List.of(mock(Channel.EMAIL), mock(Channel.SMS))));
    }
}