package com.notifyhub.webhook;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "notifyhub.webhooks")
public record WebhookProperties(String secret, @DefaultValue("300") long toleranceSeconds) {

    public WebhookProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("notifyhub.webhooks.secret must be at least 32 bytes");
        }
        if (toleranceSeconds < 1) {
            throw new IllegalStateException("notifyhub.webhooks.tolerance-seconds must be positive");
        }
    }
}