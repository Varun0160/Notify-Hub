package com.notifyhub.notification;

import com.notifyhub.notification.dto.SendNotificationRequest;
import com.notifyhub.security.ApiKeyGenerator;

import java.util.Map;
import java.util.TreeMap;

public final class RequestFingerprint {

    private RequestFingerprint() {
    }

    public static String of(SendNotificationRequest r) {
        StringBuilder sb = new StringBuilder();
        field(sb, r.recipientId() == null ? "" : r.recipientId().trim());
        field(sb, String.valueOf(r.channel()));
        field(sb, r.template() == null ? "" : r.template().trim());
        field(sb, r.scheduledAt() == null ? "" : r.scheduledAt().toString());
        field(sb, String.valueOf(Boolean.TRUE.equals(r.urgent())));

        Map<String, String> variables = (r.variables() == null) ? Map.of() : r.variables();
        new TreeMap<>(variables).forEach((name, value) -> {
            field(sb, name);
            field(sb, value == null ? "\u0000null" : value);
        });
        return ApiKeyGenerator.hash(sb.toString());   // SHA-256, 64 hex characters
    }

    // Length-prefix every field so ("ab","c") and ("a","bc") can never collide.
    private static void field(StringBuilder sb, String value) {
        sb.append(value.length()).append(':').append(value);
    }
}