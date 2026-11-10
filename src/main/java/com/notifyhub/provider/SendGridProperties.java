package com.notifyhub.provider;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "notifyhub.providers.sendgrid")
public record SendGridProperties(
        String apiKey,
        String fromEmail,
        @DefaultValue("https://api.sendgrid.com") String baseUrl,
        @DefaultValue("3000") long connectTimeoutMs,
        @DefaultValue("5000") long readTimeoutMs) {
}