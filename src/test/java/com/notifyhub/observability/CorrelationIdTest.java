package com.notifyhub.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CorrelationIdTest {

    @Test
    void aWellFormedIdIsKeptAndTrimmed() {
        assertEquals("demo-trace-001", CorrelationId.fromHeaderOrNew("demo-trace-001"));
        assertEquals("demo-trace-001", CorrelationId.fromHeaderOrNew("  demo-trace-001  "));
    }

    @Test
    void aMissingIdGetsAFreshUuid() {
        String a = CorrelationId.fromHeaderOrNew(null);
        String b = CorrelationId.fromHeaderOrNew(null);
        assertEquals(36, a.length());
        assertNotEquals(a, b);
    }

    @Test
    void unsafeValuesAreReplacedNotKept() {
        String[] bad = {"has spaces", "line1\r\nINFO fake log line", "x".repeat(65), "", "   ", "semi;colon"};
        for (String value : bad) {
            String result = CorrelationId.fromHeaderOrNew(value);
            assertNotEquals(value, result);
            assertEquals(36, result.length(), "should be a generated UUID for: " + value);
        }
    }
}