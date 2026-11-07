package com.notifyhub.notification;

import com.notifyhub.common.exception.InvalidRequestException;

import java.util.regex.Pattern;

public final class IdempotencyKeys {

    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9._:-]{1,100}$");

    private IdempotencyKeys() {
    }

    /** Returns null when no header was sent; rejects malformed keys. */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim();
        if (!VALID.matcher(key).matches()) {
            throw new InvalidRequestException(
                    "Idempotency-Key must be 1-100 characters: letters, digits, '.', '_', ':' or '-'");
        }
        return key;
    }
}