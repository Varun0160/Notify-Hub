package com.notifyhub.recipient;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

public final class QuietHours {

    private QuietHours() {
    }

    public static boolean isQuiet(LocalTime start, LocalTime end, String timezone, Instant now) {
        if (start == null || end == null || start.equals(end)) {
            return false;                       // no window configured
        }
        LocalTime local = now.atZone(ZoneId.of(timezone)).toLocalTime();

        if (start.isBefore(end)) {
            // same-day window, e.g. 13:00 to 15:00
            return !local.isBefore(start) && local.isBefore(end);
        }
        // crosses midnight, e.g. 22:00 to 07:00
        return !local.isBefore(start) || local.isBefore(end);
    }
}