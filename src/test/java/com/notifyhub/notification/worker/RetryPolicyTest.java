package com.notifyhub.notification.worker;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetryPolicyTest {

    private final RetryPolicy policy =
            new RetryPolicy(5, Duration.ofSeconds(10), Duration.ofSeconds(300));

    @Test
    void waitDoublesAfterEachFailure() {
        assertEquals(Duration.ofSeconds(10), policy.delay(1, 1.0));
        assertEquals(Duration.ofSeconds(20), policy.delay(2, 1.0));
        assertEquals(Duration.ofSeconds(40), policy.delay(3, 1.0));
    }

    @Test
    void jitterNeverGoesBelowHalfOfTheBackoff() {
        assertEquals(Duration.ofSeconds(5), policy.delay(1, 0.0));
        assertEquals(Duration.ofSeconds(10), policy.delay(2, 0.0));
    }

    @Test
    void waitIsCappedAtTheMaximum() {
        assertEquals(Duration.ofSeconds(300), policy.delay(10, 1.0));
        assertEquals(Duration.ofSeconds(150), policy.delay(10, 0.0));
    }

    @Test
    void hugeAttemptNumbersDoNotOverflow() {
        assertEquals(Duration.ofSeconds(300), policy.delay(1000, 1.0));
    }

    @Test
    void nextAttemptStaysInsideTheJitterWindow() {
        Instant now = Instant.parse("2026-11-10T10:00:00Z");
        for (int i = 0; i < 200; i++) {
            Instant next = policy.nextAttemptAt(now, 2);          // window: 10 s to 20 s
            assertFalse(next.isBefore(now.plusSeconds(10)));
            assertTrue(!next.isAfter(now.plusSeconds(20)));
        }
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new RetryPolicy(0, Duration.ofSeconds(1), Duration.ofSeconds(10)));
        assertThrows(IllegalArgumentException.class,
                () -> new RetryPolicy(3, Duration.ofSeconds(10), Duration.ofSeconds(1)));
    }
}