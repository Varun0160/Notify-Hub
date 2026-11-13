package com.notifyhub.webhook;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

public final class WebhookSignature {

    private static final String ALGORITHM = "HmacSHA256";

    private WebhookSignature() {
    }

    public static String hmacHex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 not available", ex);
        }
    }

    /** What the provider computes: HMAC(secret, "<timestamp>.<rawBody>"). */
    public static String sign(String secret, long timestamp, String rawBody) {
        return hmacHex(secret, timestamp + "." + rawBody);
    }

    public static boolean isValid(String secret, String timestampHeader, String signatureHeader,
                                  String rawBody, Instant now, long toleranceSeconds) {
        if (timestampHeader == null || signatureHeader == null || rawBody == null) {
            return false;
        }
        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException ex) {
            return false;
        }
        // Too old (replay) or too far in the future (clock trouble or forgery)
        if (Math.abs(now.getEpochSecond() - timestamp) > toleranceSeconds) {
            return false;
        }
        byte[] expected = sign(secret, timestamp, rawBody).getBytes(StandardCharsets.US_ASCII);
        byte[] provided = signatureHeader.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, provided);   // constant time
    }
}