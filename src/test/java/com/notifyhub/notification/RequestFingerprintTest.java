package com.notifyhub.notification;

import com.notifyhub.common.enums.Channel;
import com.notifyhub.notification.dto.SendNotificationRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class RequestFingerprintTest {

    private SendNotificationRequest req(Map<String, String> vars, Instant at, Boolean urgent) {
        return new SendNotificationRequest("rec-1", Channel.EMAIL, "welcome", vars, at, urgent);
    }

    @Test
    void sameRequestGivesSameFingerprint() {
        assertEquals(RequestFingerprint.of(req(Map.of("name", "Ravi"), null, null)),
                RequestFingerprint.of(req(Map.of("name", "Ravi"), null, null)));
    }

    @Test
    void variableOrderDoesNotMatter() {
        Map<String, String> a = new LinkedHashMap<>();
        a.put("x", "1");
        a.put("y", "2");
        Map<String, String> b = new LinkedHashMap<>();
        b.put("y", "2");
        b.put("x", "1");
        assertEquals(RequestFingerprint.of(req(a, null, null)), RequestFingerprint.of(req(b, null, null)));
    }

    @Test
    void differentValueGivesDifferentFingerprint() {
        assertNotEquals(RequestFingerprint.of(req(Map.of("name", "Ravi"), null, null)),
                RequestFingerprint.of(req(Map.of("name", "Raj"), null, null)));
    }

    @Test
    void differentScheduleGivesDifferentFingerprint() {
        assertNotEquals(
                RequestFingerprint.of(req(Map.of(), Instant.parse("2026-12-01T10:00:00Z"), null)),
                RequestFingerprint.of(req(Map.of(), null, null)));
    }

    @Test
    void urgentNullAndFalseAreTheSame() {
        assertEquals(RequestFingerprint.of(req(Map.of(), null, null)),
                RequestFingerprint.of(req(Map.of(), null, false)));
        assertNotEquals(RequestFingerprint.of(req(Map.of(), null, false)),
                RequestFingerprint.of(req(Map.of(), null, true)));
    }

    @Test
    void fieldBoundariesCannotBeFaked() {
        // Without length prefixes, {"ab":"c"} and {"a":"bc"} would concatenate identically
        assertNotEquals(RequestFingerprint.of(req(Map.of("ab", "c"), null, null)),
                RequestFingerprint.of(req(Map.of("a", "bc"), null, null)));
    }

    @Test
    void fingerprintIs64HexCharacters() {
        assertEquals(64, RequestFingerprint.of(req(Map.of(), null, null)).length());
    }
}