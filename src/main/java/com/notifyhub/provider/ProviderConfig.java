package com.notifyhub.provider;

import com.notifyhub.common.enums.Channel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(SendGridProperties.class)
public class ProviderConfig {

    @Bean
    @ConditionalOnProperty(name = "notifyhub.providers.email", havingValue = "sendgrid")
    NotificationProvider sendGridEmailProvider(SendGridProperties props, ObjectMapper objectMapper) {
        if (isBlank(props.apiKey()) || isBlank(props.fromEmail())) {
            throw new IllegalStateException(
                    "notifyhub.providers.email=sendgrid needs SENDGRID_API_KEY and SENDGRID_FROM_EMAIL");
        }
        return new SendGridEmailProvider(props.baseUrl(), props.apiKey(), props.fromEmail(),
                Duration.ofMillis(props.connectTimeoutMs()),
                Duration.ofMillis(props.readTimeoutMs()), objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "notifyhub.providers.email", havingValue = "mock", matchIfMissing = true)
    NotificationProvider mockEmailProvider() {
        return new MockNotificationProvider(Channel.EMAIL);
    }

    @Bean
    NotificationProvider mockSmsProvider() {
        return new MockNotificationProvider(Channel.SMS);
    }

    @Bean
    NotificationProvider mockPushProvider() {
        return new MockNotificationProvider(Channel.PUSH);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}