package com.notifyhub.notification;

import com.notifyhub.common.exception.InvalidRequestException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class PageCursor {

    private PageCursor() {
    }

    public static String encode(long lastId) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(Long.toString(lastId).getBytes(StandardCharsets.UTF_8));
    }

    /** Returns null for "first page". Rejects anything we did not produce. */
    public static Long decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(new String(Base64.getUrlDecoder().decode(cursor.trim()),
                    StandardCharsets.UTF_8));
            if (id < 1) {
                throw new InvalidRequestException("Invalid cursor");
            }
            return id;
        } catch (IllegalArgumentException ex) {      // bad base64, or not a number
            throw new InvalidRequestException("Invalid cursor");
        }
    }
}