package com.notifyhub.ratelimit;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisTokenBucketLimiterTest {

    private final RateLimitProperties props = new RateLimitProperties(true,
            new RateLimitProperties.Limit(20, 5), new RateLimitProperties.Limit(3, 0.0166));

    @Test
    void whenRedisIsUnreachableTheLimiterFailsOpen() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }   // closed again: nothing listens there

        LettuceConnectionFactory factory = new LettuceConnectionFactory("127.0.0.1", closedPort);
        factory.afterPropertiesSet();
        try {
            RedisTokenBucketLimiter limiter =
                    new RedisTokenBucketLimiter(new StringRedisTemplate(factory), props);

            RateLimitDecision decision = limiter.tryConsume("notifyhub:rl:test", 5, 1.0);

            assertTrue(decision.allowed(), "a dead Redis must not block traffic");
        } finally {
            factory.destroy();
        }
    }

    @Test
    void retryAfterIsRoundedUpAndNeverZero() {
        assertEquals(4, new RateLimitDecision(false, 0, 3200).retryAfterSeconds());
        assertEquals(1, new RateLimitDecision(false, 0, 10).retryAfterSeconds());
        assertEquals(1, new RateLimitDecision(false, 0, 0).retryAfterSeconds());
    }
}