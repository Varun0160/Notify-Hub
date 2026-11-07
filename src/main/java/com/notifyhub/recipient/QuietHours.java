package com.notifyhub.recipient;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class QuietHours {

    private QuietHours() {
    }

    public static boolean isQuiet(LocalTime start, LocalTime end, String timezone, Instant now) {
        if (start == null || end == null || start.equals(end)) {
            return false;
        }
        LocalTime local = now.atZone(ZoneId.of(timezone)).toLocalTime();

        if (start.isBefore(end)) {
            return !local.isBefore(start) && local.isBefore(end);
        }
        return !local.isBefore(start) || local.isBefore(end);
    }

    public static Instant nextEnd(LocalTime end, String timezone, Instant from) {
        ZoneId zone = ZoneId.of(timezone);
        ZonedDateTime local = from.atZone(zone);

        ZonedDateTime candidate = local.toLocalDate().atTime(end).atZone(zone);
        if (!candidate.toInstant().isAfter(from)) {
            candidate = local.toLocalDate().plusDays(1).atTime(end).atZone(zone);
        }
        return candidate.toInstant();
    }
}