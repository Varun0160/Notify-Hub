package com.notifyhub.recipient;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuietHoursTest {

    private static final LocalTime START = LocalTime.of(22, 0);
    private static final LocalTime END = LocalTime.of(7, 0);
    private static final String IST = "Asia/Kolkata";   // UTC+5:30

    @Test
    void lateEveningIsQuietWhenWindowCrossesMidnight() {
        // 17:00 UTC = 22:30 IST
        assertTrue(QuietHours.isQuiet(START, END, IST, Instant.parse("2026-11-03T17:00:00Z")));
    }

    @Test
    void earlyMorningIsQuietWhenWindowCrossesMidnight() {
        // 20:00 UTC = 01:30 IST (next day)
        assertTrue(QuietHours.isQuiet(START, END, IST, Instant.parse("2026-11-03T20:00:00Z")));
    }

    @Test
    void daytimeIsNotQuiet() {
        // 06:00 UTC = 11:30 IST
        assertFalse(QuietHours.isQuiet(START, END, IST, Instant.parse("2026-11-03T06:00:00Z")));
    }

    @Test
    void endBoundaryIsNotQuiet() {
        // 01:30 UTC = 07:00 IST exactly
        assertFalse(QuietHours.isQuiet(START, END, IST, Instant.parse("2026-11-03T01:30:00Z")));
    }

    @Test
    void startBoundaryIsQuiet() {
        // 16:30 UTC = 22:00 IST exactly
        assertTrue(QuietHours.isQuiet(START, END, IST, Instant.parse("2026-11-03T16:30:00Z")));
    }

    @Test
    void sameDayWindowWorks() {
        LocalTime s = LocalTime.of(13, 0);
        LocalTime e = LocalTime.of(15, 0);
        assertTrue(QuietHours.isQuiet(s, e, "UTC", Instant.parse("2026-11-03T14:00:00Z")));
        assertFalse(QuietHours.isQuiet(s, e, "UTC", Instant.parse("2026-11-03T16:00:00Z")));
    }

    @Test
    void noWindowMeansNeverQuiet() {
        assertFalse(QuietHours.isQuiet(null, null, "UTC", Instant.parse("2026-11-03T03:00:00Z")));
    }

    @Test
    void nextEndAfterMidnightCrossingEveningIsTomorrowMorning() {
        // 17:00 UTC = 22:30 IST -> quiet ends 07:00 IST next day = 01:30 UTC
        Instant next = QuietHours.nextEnd(END, IST, Instant.parse("2026-11-03T17:00:00Z"));
        assertEquals(Instant.parse("2026-11-04T01:30:00Z"), next);
    }

    @Test
    void nextEndInEarlyMorningIsSameMorning() {
        // 20:00 UTC = 01:30 IST (Nov 4) -> quiet ends 07:00 IST the same day = 01:30 UTC
        Instant next = QuietHours.nextEnd(END, IST, Instant.parse("2026-11-03T20:00:00Z"));
        assertEquals(Instant.parse("2026-11-04T01:30:00Z"), next);
    }

    @Test
    void nextEndForSameDayWindow() {
        Instant next = QuietHours.nextEnd(LocalTime.of(15, 0), "UTC", Instant.parse("2026-11-03T14:00:00Z"));
        assertEquals(Instant.parse("2026-11-03T15:00:00Z"), next);
    }

    @Test
    void nextEndExactlyAtEndTimeMovesToNextDay() {
        // now = 07:00 IST exactly (01:30 UTC): window just ended, so next end is tomorrow
        Instant next = QuietHours.nextEnd(END, IST, Instant.parse("2026-11-04T01:30:00Z"));
        assertEquals(Instant.parse("2026-11-05T01:30:00Z"), next);
    }
}