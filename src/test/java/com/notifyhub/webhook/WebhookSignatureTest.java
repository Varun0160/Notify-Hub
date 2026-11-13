package com.notifyhub.webhook;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSignatureTest {

    private static final String SECRET = "a-test-secret-that-is-long-enough-123";
    private static final String BODY = "{\"eventId\":\"e1\",\"providerMessageId\":\"m1\",\"type\":\"delivered\"}";
    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");
    private static final long TS = NOW.getEpochSecond();

    private boolean check(String timestamp, String signature, String body) {
        return WebhookSignature.isValid(SECRET, timestamp, signature, body, NOW, 300);
    }

    @Test
    void hmacMatchesTheOfficialTestVector() {
        // RFC 4231, test case 2
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843",
                WebhookSignature.hmacHex("Jefe", "what do ya want for nothing?"));
    }

    @Test
    void validSignatureIsAccepted() {
        assertTrue(check(String.valueOf(TS), WebhookSignature.sign(SECRET, TS, BODY), BODY));
    }

    @Test
    void uppercaseHexIsAcceptedToo() {
        assertTrue(check(String.valueOf(TS), WebhookSignature.sign(SECRET, TS, BODY).toUpperCase(), BODY));
    }

    @Test
    void changingOneCharacterOfTheBodyBreaksIt() {
        String sig = WebhookSignature.sign(SECRET, TS, BODY);
        assertFalse(check(String.valueOf(TS), sig, BODY.replace("delivered", "bounced")));
    }

    @Test
    void wrongSecretIsRejected() {
        String sig = WebhookSignature.sign("some-other-secret-that-is-long-enough-1", TS, BODY);
        assertFalse(check(String.valueOf(TS), sig, BODY));
    }

    @Test
    void signatureOfADifferentTimestampIsRejected() {
        // An attacker cannot move a captured signature to a fresh timestamp
        String sigForOldTime = WebhookSignature.sign(SECRET, TS - 10, BODY);
        assertFalse(check(String.valueOf(TS), sigForOldTime, BODY));
    }

    @Test
    void oldRequestsAreRejectedAsReplays() {
        long old = TS - 301;
        assertFalse(check(String.valueOf(old), WebhookSignature.sign(SECRET, old, BODY), BODY));
    }

    @Test
    void requestsInsideTheToleranceAreAccepted() {
        long recent = TS - 299;
        assertTrue(check(String.valueOf(recent), WebhookSignature.sign(SECRET, recent, BODY), BODY));
    }

    @Test
    void timestampsFromTheFutureAreRejected() {
        long future = TS + 301;
        assertFalse(check(String.valueOf(future), WebhookSignature.sign(SECRET, future, BODY), BODY));
    }

    @Test
    void missingOrMalformedInputIsRejectedWithoutThrowing() {
        String sig = WebhookSignature.sign(SECRET, TS, BODY);
        assertFalse(check(null, sig, BODY));
        assertFalse(check(String.valueOf(TS), null, BODY));
        assertFalse(check("not-a-number", sig, BODY));
        assertFalse(check(String.valueOf(TS), "", BODY));
        assertFalse(check(String.valueOf(TS), "abc", BODY));
    }
}