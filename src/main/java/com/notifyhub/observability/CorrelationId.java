package com.notifyhub.observability;

import java.util.UUID;
import java.util.regex.Pattern;

public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    // Safe alphabet and length: blocks CR/LF and other log-injection tricks.
    private static final Pattern VALID = Pattern.compile("^[A-Za-z0-9._:-]{1,64}$");

    private CorrelationId() {
    }

    /** Keeps a well-formed incoming id, otherwise generates a fresh one. */
    public static String fromHeaderOrNew(String raw) {
        if (raw != null) {
            String trimmed = raw.trim();
            if (VALID.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return newId();
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }
}