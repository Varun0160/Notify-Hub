package com.notifyhub.ratelimit;

public interface RateLimiter {

    /** Takes one token from the bucket named by `key`. Must never throw. */
    RateLimitDecision tryConsume(String key, int capacity, double refillPerSecond);
}