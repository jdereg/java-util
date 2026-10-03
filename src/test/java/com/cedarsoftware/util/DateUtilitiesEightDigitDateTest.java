package com.cedarsoftware.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Eight digits that make a date -- "20240115", ISO 8601's basic form of 2024-01-15 -- read as that date. They were read
 * as epoch milliseconds: "20240115" came out as 1970-01-01T05:37:20.115Z. Eight-digit epoch milliseconds only reach the
 * first 28 hours of 1970, so almost nothing is lost. Eight digits that are not a date (month 13, day 32, February 30),
 * any other number of digits, and a signed number are still epoch milliseconds.
 */
class DateUtilitiesEightDigitDateTest {

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    @Test
    void eightDigitsThatMakeADateAreThatDate() {
        assertEquals(ZonedDateTime.parse("2024-01-15T00:00-05:00[America/New_York]"),
                DateUtilities.parseDate("20240115", NEW_YORK, true));
        assertEquals(ZonedDateTime.parse("1970-01-01T00:00-05:00[America/New_York]"),
                DateUtilities.parseDate("19700101", NEW_YORK, true));
        assertEquals(ZonedDateTime.parse("2024-02-29T00:00-05:00[America/New_York]"),
                DateUtilities.parseDate("20240229", NEW_YORK, true));
        // as "2024-01-15" reads, in the default zone
        assertEquals(DateUtilities.parseDate("2024-01-15", NEW_YORK, true), DateUtilities.parseDate("20240115", NEW_YORK, true));
        assertEquals(DateUtilities.parseDate("2024-01-15"), DateUtilities.parseDate("20240115"));
    }

    @Test
    void anythingElseIsStillEpochMilliseconds() {
        for (String millis : new String[]{"20241315", "20240132", "20230229", "12345678", "00000000", "1705314600000",
                "1234567", "123456789", "202401151", "-20240115"}) {
            assertEquals(Instant.ofEpochMilli(Long.parseLong(millis)).atZone(NEW_YORK),
                    DateUtilities.parseDate(millis, NEW_YORK, true), millis);
        }
        assertEquals(new Date(1705314600000L), DateUtilities.parseDate("1705314600000"));
    }
}
