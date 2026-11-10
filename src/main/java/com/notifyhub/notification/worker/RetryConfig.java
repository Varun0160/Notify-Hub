package com.notifyhub.notification.worker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RetryConfig {

    @Bean
    RetryPolicy retryPolicy(@Value("${notifyhub.retry.max-attempts:5}") int maxAttempts,
                            @Value("${notifyhub.retry.base-delay-seconds:10}") long baseSeconds,
                            @Value("${notifyhub.retry.max-delay-seconds:3600}") long maxSeconds) {
        return new RetryPolicy(maxAttempts, Duration.ofSeconds(baseSeconds), Duration.ofSeconds(maxSeconds));
    }
}