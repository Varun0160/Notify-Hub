package com.notifyhub.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notifyhub.ratelimit")
public record RateLimitProperties(boolean enabled, Limit tenant, Limit recipient) {

    public record Limit(int capacity, double refillPerSecond) {
        public Limit {
            if (capacity < 1 || refillPerSecond <= 0) {
                throw new IllegalArgumentException(
                        "Rate limit needs capacity >= 1 and refill-per-second > 0");
            }
        }
    }
}