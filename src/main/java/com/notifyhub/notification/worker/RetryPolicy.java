package com.notifyhub.notification.worker;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

public class RetryPolicy {

    private final int maxAttempts;
    private final long baseDelayMs;
    private final long maxDelayMs;

    public RetryPolicy(int maxAttempts, Duration baseDelay, Duration maxDelay) {
        if (maxAttempts < 1 || baseDelay.isZero() || baseDelay.isNegative() || maxDelay.compareTo(baseDelay) < 0) {
            throw new IllegalArgumentException("Invalid retry settings");
        }
        this.maxAttempts = maxAttempts;
        this.baseDelayMs = baseDelay.toMillis();
        this.maxDelayMs = maxDelay.toMillis();
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    /** Wait after the given failed attempt (1 = first failure). `random` must be in [0, 1]. */
    public Duration delay(int attemptNo, double random) {
        int shift = Math.min(Math.max(attemptNo - 1, 0), 20);   // cap the shift: no overflow
        long exp = Math.min(maxDelayMs, baseDelayMs * (1L << shift));
        long half = exp / 2;
        return Duration.ofMillis(half + (long) (half * random));
    }

    public Instant nextAttemptAt(Instant now, int attemptNo) {
        return now.plus(delay(attemptNo, ThreadLocalRandom.current().nextDouble()));
    }
}