package com.notifyhub.notification.worker;

import com.notifyhub.common.exception.InvalidRequestException;
import com.notifyhub.provider.NotificationProvider;
import com.notifyhub.provider.OutboundMessage;
import com.notifyhub.provider.ProviderRegistry;
import com.notifyhub.provider.SendOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDeliverer {

    public record Delivery(SendOutcome outcome, String providerName) {
    }

    private final NotificationProcessor processor;
    private final ProviderRegistry registry;

    @Value("${notifyhub.worker.simulated-delay-ms:0}")
    private long simulatedDelayMs;

    // Deliberately NOT @Transactional. Never throws: every problem becomes an outcome.
    public Delivery deliver(String publicId) {
        OutboundMessage message;
        try {
            message = processor.prepare(publicId);
        } catch (InvalidRequestException ex) {
            return new Delivery(SendOutcome.permanent("Cannot render message: " + ex.getMessage()), "none");
        } catch (Exception ex) {
            log.warn("Could not prepare notification {}", publicId, ex);
            return new Delivery(SendOutcome.retryable("Internal error while preparing message"), "none");
        }

        NotificationProvider provider = registry.forChannel(message.channel());
        pause();
        try {
            return new Delivery(provider.send(message), provider.name());
        } catch (RuntimeException ex) {
            // A provider broke its own contract (it must not throw). Treat as retryable.
            log.error("Provider {} threw an exception", provider.name(), ex);
            return new Delivery(SendOutcome.retryable("Provider error: " + ex.getClass().getSimpleName()),
                    provider.name());
        }
    }

    private void pause() {
        if (simulatedDelayMs > 0) {
            try {
                Thread.sleep(simulatedDelayMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}