package com.notifyhub.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class RedisTokenBucketLimiter implements RateLimiter {

    private final StringRedisTemplate redis;
    private final RateLimitProperties props;
    private final DefaultRedisScript<List> script;

    public RedisTokenBucketLimiter(StringRedisTemplate redis, RateLimitProperties props) {
        this.redis = redis;
        this.props = props;
        this.script = new DefaultRedisScript<>();
        this.script.setLocation(new ClassPathResource("scripts/token_bucket.lua"));
        this.script.setResultType(List.class);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public RateLimitDecision tryConsume(String key, int capacity, double refillPerSecond) {
        if (!props.enabled()) {
            return new RateLimitDecision(true, capacity, 0);
        }
        try {
            List result = redis.execute(script, List.of(key),
                    String.valueOf(capacity), String.valueOf(refillPerSecond), "1");
            if (result == null || result.size() < 3) {
                throw new IllegalStateException("Unexpected script result: " + result);
            }
            boolean allowed = ((Number) result.get(0)).longValue() == 1;
            long remaining = ((Number) result.get(1)).longValue();
            long retryAfterMs = ((Number) result.get(2)).longValue();
            return new RateLimitDecision(allowed, remaining, retryAfterMs);

        } catch (Exception ex) {
            // Fail OPEN (concept 6): a broken limiter must not take the API down with it.
            log.warn("Rate limiter unavailable, allowing request: {}", ex.getMessage());
            return new RateLimitDecision(true, capacity, 0);
        }
    }
}