package com.notifyhub.provider;

import com.notifyhub.common.enums.Channel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ProviderRegistry {

    private final Map<Channel, NotificationProvider> byChannel = new EnumMap<>(Channel.class);

    public ProviderRegistry(List<NotificationProvider> providers) {
        for (NotificationProvider provider : providers) {
            NotificationProvider previous = byChannel.put(provider.channel(), provider);
            if (previous != null) {
                throw new IllegalStateException("Two providers configured for channel " + provider.channel());
            }
        }
        for (Channel channel : Channel.values()) {
            if (!byChannel.containsKey(channel)) {
                throw new IllegalStateException("No provider configured for channel " + channel);
            }
        }
    }

    public NotificationProvider forChannel(Channel channel) {
        return byChannel.get(channel);
    }
}