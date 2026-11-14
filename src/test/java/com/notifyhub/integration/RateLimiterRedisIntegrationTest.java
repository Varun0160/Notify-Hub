package com.notifyhub.integration;

import com.notifyhub.ratelimit.RateLimitDecision;
import com.notifyhub.ratelimit.RateLimiter;
import com.notifyhub.support.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the real Lua script against a real Redis: the part unit tests could not cover. */
class RateLimiterRedisIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RateLimiter limiter;

    private String newKey() {
        return "notifyhub:rl:test:" + UUID.randomUUID();
    }

    @Test
    void theBucketStartsFullThenRejectsAndTellsWhenToRetry() {
        String key = newKey();

        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.tryConsume(key, 3, 0.001).allowed(), "token " + (i + 1));
        }
        RateLimitDecision denied = limiter.tryConsume(key, 3, 0.001);

        assertFalse(denied.allowed());
        assertTrue(denied.retryAfterSeconds() >= 1);
        assertEquals(0, denied.remaining());
    }

    @Test
    void parallelCallsNeverLetMoreThanCapacityThrough() throws Exception {
        String key = newKey();
        int capacity = 5;
        int threads = 40;
        AtomicInteger allowed = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                go.await();
                if (limiter.tryConsume(key, capacity, 0.001).allowed()) {
                    allowed.incrementAndGet();
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        // A non-atomic read-then-write would let several extra requests slip through
        assertEquals(capacity, allowed.get());
    }

    @Test
    void differentKeysHaveIndependentBuckets() {
        String a = newKey();
        String b = newKey();
        limiter.tryConsume(a, 1, 0.001);

        assertFalse(limiter.tryConsume(a, 1, 0.001).allowed());
        assertTrue(limiter.tryConsume(b, 1, 0.001).allowed());
    }
}