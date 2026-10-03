package com.cedarsoftware.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A fraction of a second with more than 9 digits -- "10:00:00.1234567891" -- was rejected ("other characters present:
 * 1Z"), although ISO 8601 sets no limit on the digits and DateUtilities' own conversion truncates past nanoseconds.
 * The time pattern took at most 9 digits, so that truncation could never run. Now any number of digits is read and
 * truncated to nanoseconds.
 */
class DateUtilitiesFractionDigitsTest {

    private static ZonedDateTime utc(String text) {
        return DateUtilities.parseDate(text, ZoneId.of("UTC"), true);
    }

    @Test
    void digitsPastNanosecondsAreTruncated() {
        assertEquals(ZonedDateTime.parse("2024-01-15T10:00:00.123456789Z"), utc("2024-01-15T10:00:00.1234567891Z"));
        assertEquals(ZonedDateTime.parse("2024-01-15T10:00:00.123456789Z[UTC]"), utc("2024-01-15 10:00:00.123456789987"));
        assertEquals(ZonedDateTime.parse("2024-01-15T10:00:00.999999999Z[UTC]"), utc("Jan 15, 2024 10:00:00.99999999999"));
    }

    @Test
    void nineOrFewerAreAsBefore() {
        assertEquals(ZonedDateTime.parse("2024-01-15T10:00:00.5Z[UTC]"), utc("2024-01-15 10:00:00.5"));
        assertEquals(ZonedDateTime.parse("2024-01-15T10:00:00.123456789Z[UTC]"), utc("2024-01-15 10:00:00.123456789"));
    }
}
