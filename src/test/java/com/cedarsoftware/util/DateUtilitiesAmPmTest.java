package com.cedarsoftware.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A 12-hour time -- "3:30 PM", "3:30PM", "3:30pm" -- and a one-digit hour ("9:30") were not understood: the time
 * pattern took exactly two hour digits, and "PM" was left over as text or taken for a time zone. A time now takes one
 * or two hour digits and an optional AM/PM in any case, with or without a space: 12 AM is midnight, 12 PM noon, and
 * with AM/PM the hour runs 1 to 12. A zone or offset may follow.
 */
class DateUtilitiesAmPmTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static ZonedDateTime utc(String text) {
        return DateUtilities.parseDate(text, UTC, true);
    }

    @Test
    void afternoonInEverySpelling() {
        ZonedDateTime expected = ZonedDateTime.parse("2024-01-05T15:30Z[UTC]");
        for (String text : new String[]{"Jan 5, 2024 3:30 PM", "Jan 5, 2024 3:30PM", "Jan 5, 2024 3:30pm",
                "Jan 5, 2024 3:30 Pm", "Jan 5, 2024 03:30 pm", "2024-01-05 3:30 PM", "2024-01-05T3:30PM",
                "3:30 PM Jan 5, 2024", "01/05/2024 3:30 PM"}) {
            assertEquals(expected, utc(text), text);
        }
    }

    @Test
    void midnightNoonAndTheHoursBetween() {
        assertEquals(ZonedDateTime.parse("2024-01-15T00:00Z[UTC]"), utc("2024-01-15 12:00 AM"));
        assertEquals(ZonedDateTime.parse("2024-01-15T00:30Z[UTC]"), utc("2024-01-15 12:30 am"));
        assertEquals(ZonedDateTime.parse("2024-01-15T12:00Z[UTC]"), utc("2024-01-15 12:00 PM"));
        assertEquals(ZonedDateTime.parse("2024-01-15T01:05Z[UTC]"), utc("2024-01-15 1:05 am"));
        assertEquals(ZonedDateTime.parse("2024-01-15T23:59:59.500Z[UTC]"), utc("2024-01-15 11:59:59.5 PM"));
    }

    @Test
    void aZoneOrOffsetMayFollow() {
        assertEquals(Instant.parse("2024-01-15T20:30:00Z"), utc("2024-01-15 3:30 PM EST").toInstant());
        assertEquals(Instant.parse("2024-01-15T10:00:00Z"), utc("2024-01-15 3:30 PM+05:30").toInstant());
    }

    @Test
    void aOneDigitHourOnTheTwentyFourHourClock() {
        assertEquals(ZonedDateTime.parse("2024-01-15T09:30Z[UTC]"), utc("2024-01-15 9:30"));
        assertEquals(ZonedDateTime.parse("2024-01-05T09:30:15Z[UTC]"), utc("Jan 5, 2024 9:30:15"));
        assertEquals(ZonedDateTime.parse("2024-01-15T15:30Z[UTC]"), utc("2024-01-15 15:30"));    // as before
    }

    @Test
    void anHourAmPmCannotHaveIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> utc("2024-01-15 13:30 PM"));
        assertThrows(IllegalArgumentException.class, () -> utc("2024-01-15 0:30 AM"));
        assertThrows(IllegalArgumentException.class, () -> utc("2024-01-15 25:00"));
    }
}
