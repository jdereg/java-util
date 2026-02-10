package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for LocalDateConversions bugs.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class LocalDateConversionsTest {

    private static Converter converterWithZone(ZoneId zoneId) {
        ConverterOptions options = new ConverterOptions() {
            @Override
            public <T> T getCustomOption(String name) { return null; }

            @Override
            public ZoneId getZoneId() { return zoneId; }
        };
        return new Converter(options);
    }

    // ---- Bug #1: toTimestamp ignores converter's timezone (hardcodes UTC via toEpochDay * 86400 * 1000) ----

    @Test
    void toTimestamp_shouldRespectConverterTimezone() {
        LocalDate date = LocalDate.of(1970, 1, 1);
        Converter tokyoConverter = converterWithZone(ZoneId.of("Asia/Tokyo"));

        Timestamp result = LocalDateConversions.toTimestamp(date, tokyoConverter);

        // Tokyo midnight 1970-01-01 = 1969-12-31T15:00:00Z = epoch millis -32400000
        // Bug: produces epoch millis 0 (UTC midnight) instead
        assertEquals(-32400000L, result.getTime());
    }

    @Test
    void toTimestamp_shouldBeConsistentWithToLong() {
        LocalDate date = LocalDate.of(1970, 1, 2);
        Converter tokyoConverter = converterWithZone(ZoneId.of("Asia/Tokyo"));

        long longResult = LocalDateConversions.toLong(date, tokyoConverter);
        Timestamp tsResult = LocalDateConversions.toTimestamp(date, tokyoConverter);

        // Both should represent the same instant (midnight in converter's zone)
        assertEquals(longResult, tsResult.getTime());
    }

    // ---- Bug #3: toOffsetDateTime uses System.currentTimeMillis() for offset ----

    @Test
    void toOffsetDateTime_summerDate_shouldUseDateTimeForOffset() {
        // July 15 — during US Eastern Daylight Time (EDT = UTC-4)
        LocalDate summerDate = LocalDate.of(2024, 7, 15);
        Converter nyConverter = converterWithZone(ZoneId.of("America/New_York"));

        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(summerDate, nyConverter);

        // Should be -04:00 (EDT), not -05:00 (EST from current winter time)
        assertEquals(ZoneOffset.ofHours(-4), result.getOffset());
        assertEquals(2024, result.getYear());
        assertEquals(7, result.getMonthValue());
        assertEquals(15, result.getDayOfMonth());
        assertEquals(0, result.getHour());
    }

    @Test
    void toOffsetDateTime_winterDate_shouldUseCorrectOffset() {
        // January 15 — during US Eastern Standard Time (EST = UTC-5)
        LocalDate winterDate = LocalDate.of(2024, 1, 15);
        Converter nyConverter = converterWithZone(ZoneId.of("America/New_York"));

        OffsetDateTime result = LocalDateConversions.toOffsetDateTime(winterDate, nyConverter);

        assertEquals(ZoneOffset.ofHours(-5), result.getOffset());
    }

    @Test
    void toOffsetDateTime_shouldBeConsistentWithToZonedDateTime() {
        LocalDate date = LocalDate.of(2024, 7, 15);
        Converter nyConverter = converterWithZone(ZoneId.of("America/New_York"));

        OffsetDateTime offsetResult = LocalDateConversions.toOffsetDateTime(date, nyConverter);
        ZonedDateTime zdtResult = LocalDateConversions.toZonedDateTime(date, nyConverter);

        // Both should represent the same instant
        assertEquals(zdtResult.toInstant(), offsetResult.toInstant());
    }
}
