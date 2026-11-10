package com.notifyhub.ratelimit;

public record RateLimitDecision(boolean allowed, long remaining, long retryAfterMs) {

    /** Rounded up, and at least 1: "Retry-After: 0" would invite an instant retry. */
    public long retryAfterSeconds() {
        return Math.max(1, (long) Math.ceil(retryAfterMs / 1000.0));
    }
}