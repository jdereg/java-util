package com.cedarsoftware.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Coverage for the strict-ISO fast path in {@link DateUtilities#parseDate(String, ZoneId, boolean)}
 * (4.104.0). Fully-zoned ISO-8601 date-times now parse via {@link ZonedDateTime#parse}
 * instead of the regex-driven flexible recognizer. Output must be identical to the
 * flexible path's historical results — these expectations were captured against the
 * pre-change implementation. Everything non-ISO must keep flowing through the flexible
 * path untouched.
 */
class DateUtilitiesIsoFastPathTest {
    private static final ZoneId CHICAGO = ZoneId.of("America/Chicago");

    @Test
    void zSuffixKeepsZoneZ() {
        ZonedDateTime zdt = DateUtilities.parseDate("2026-06-10T17:45:30Z", CHICAGO, true);
        assertEquals(ZoneOffset.UTC, zdt.getZone());
        assertEquals(Instant.parse("2026-06-10T17:45:30Z"), zdt.toInstant());
    }

    @Test
    void nanosecondPrecisionPreserved() {
        ZonedDateTime zdt = DateUtilities.parseDate("2026-06-10T17:45:30.123456789Z", CHICAGO, true);
        assertEquals(123456789, zdt.getNano());
    }

    @Test
    void bareOffsetNormalizesToGmtPrefixedZone() {
        // Historical flexible-path behavior: "+05:30" produces zone GMT+05:30 (not the
        // plain offset) — the fast path must match exactly.
        ZonedDateTime zdt = DateUtilities.parseDate("2026-06-10T13:45:30+05:30", CHICAGO, true);
        assertEquals(ZoneId.of("GMT+05:30"), zdt.getZone());
        assertEquals(LocalDateTime.of(2026, 6, 10, 13, 45, 30), zdt.toLocalDateTime());

        // "+00:00" historically maps to GMT (NOT Z)
        ZonedDateTime utc = DateUtilities.parseDate("2026-06-10T13:45:30.123+00:00", CHICAGO, true);
        assertEquals(ZoneId.of("GMT"), utc.getZone());

        // negative offset
        ZonedDateTime neg = DateUtilities.parseDate("2026-06-10T13:45:30-04:00", CHICAGO, true);
        assertEquals(ZoneId.of("GMT-04:00"), neg.getZone());
    }

    @Test
    void bracketedRegionZonePreserved() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2026-06-10T13:45:30-04:00[America/New_York]", CHICAGO, true);
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
        assertEquals(LocalDateTime.of(2026, 6, 10, 13, 45, 30), zdt.toLocalDateTime());
    }

    @Test
    void bracketedFormsDeferToFlexiblePath() {
        // Bracketed [zone] forms must NOT be handled by the strict fast path — it would skip
        // GMT->Etc/GMT normalization and trust a minute-truncated offset over the region,
        // shifting the instant for ancient LMT dates. They defer to the flexible path instead.
        // (Regression fix: a 4.104.0 fast path mishandled these.)

        // (a) GMT normalization preserved.
        ZonedDateTime gmt = DateUtilities.parseDate("2024-02-02T12:00:00Z[GMT]", CHICAGO, true);
        assertEquals(ZoneId.of("Etc/GMT"), gmt.getZone());

        // (b) Ancient LMT date: the region's true offset (-04:56:02) wins over the written,
        // minute-truncated -04:56, so the instant is preserved (no 2-second drift).
        ZonedDateTime lmt = DateUtilities.parseDate(
                "0003-01-31T12:03:58-04:56[America/New_York]", CHICAGO, true);
        assertEquals(ZoneId.of("America/New_York"), lmt.getZone());
        assertEquals(ZoneOffset.ofTotalSeconds(-17762), lmt.getOffset());   // -04:56:02
    }

    @Test
    void zoneLessIsoStillGetsDefaultZone() {
        ZonedDateTime zdt = DateUtilities.parseDate("2026-06-10T13:45:30", CHICAGO, true);
        assertEquals(CHICAGO, zdt.getZone());
        assertEquals(LocalDateTime.of(2026, 6, 10, 13, 45, 30), zdt.toLocalDateTime());
    }

    @Test
    void flexibleFormatsUnaffected() {
        ZonedDateTime human = DateUtilities.parseDate("January 5, 2024", CHICAGO, true);
        assertEquals(LocalDate.of(2024, 1, 5), human.toLocalDate());
        assertEquals(CHICAGO, human.getZone());

        ZonedDateTime epoch = DateUtilities.parseDate("1700000000000", CHICAGO, true);
        assertEquals(Instant.ofEpochMilli(1700000000000L), epoch.toInstant());

        ZonedDateTime dateOnly = DateUtilities.parseDate("2026-06-10", CHICAGO, true);
        assertEquals(LocalDate.of(2026, 6, 10), dateOnly.toLocalDate());
        assertEquals(CHICAGO, dateOnly.getZone());
    }

    @Test
    void lowercaseTStillRejected() {
        // The flexible path rejects lowercase 't' — the shape gate must not change that.
        assertThrows(IllegalArgumentException.class,
                () -> DateUtilities.parseDate("2026-06-10t13:45:30Z", CHICAGO, true));
    }

    @Test
    void surroundingWhitespaceTrimmedBeforeFastPath() {
        ZonedDateTime zdt = DateUtilities.parseDate("  2026-06-10T17:45:30Z  ", CHICAGO, true);
        assertEquals(Instant.parse("2026-06-10T17:45:30Z"), zdt.toInstant());
    }

    @Test
    void converterTemporalConversionsRideTheFastPath() {
        Converter converter = new Converter(new DefaultConverterOptions());

        assertEquals(Instant.parse("2026-06-10T17:45:30.123Z"),
                converter.convert("2026-06-10T17:45:30.123Z", Instant.class));
        assertEquals(LocalDate.of(2026, 6, 10),
                converter.convert("2026-06-10T13:45:30-04:00[America/New_York]", LocalDate.class));
        assertEquals(LocalDateTime.of(2026, 6, 10, 13, 45, 30),
                converter.convert("2026-06-10T13:45:30+05:30", LocalDateTime.class));
        assertEquals(ZonedDateTime.parse("2026-06-10T13:45:30-04:00[America/New_York]"),
                converter.convert("2026-06-10T13:45:30-04:00[America/New_York]", ZonedDateTime.class));
    }
}
