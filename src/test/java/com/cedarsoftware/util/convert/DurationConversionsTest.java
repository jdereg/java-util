package com.cedarsoftware.util.convert;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for DurationConversions bugs.
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
class DurationConversionsTest {

    private static Converter converterWithZone(ZoneId zoneId) {
        ConverterOptions options = new ConverterOptions() {
            @Override
            public <T> T getCustomOption(String name) { return null; }

            @Override
            public ZoneId getZoneId() { return zoneId; }
        };
        return new Converter(options);
    }

    // ---- Bug #1: toOffsetDateTime uses System.currentTimeMillis() for offset ----

    @Test
    void toOffsetDateTime_summerDuration_shouldUseDurationTimeForOffset() {
        // Duration that lands in July 2024 (during EDT = UTC-4)
        Instant summerInstant = Instant.parse("2024-07-15T12:00:00Z");
        Duration duration = Duration.between(Instant.EPOCH, summerInstant);
        Converter nyConverter = converterWithZone(ZoneId.of("America/New_York"));

        OffsetDateTime result = DurationConversions.toOffsetDateTime(duration, nyConverter);

        // Offset should be -04:00 (EDT), not -05:00 (EST from current winter time)
        assertEquals(ZoneOffset.ofHours(-4), result.getOffset());
    }

    @Test
    void toOffsetDateTime_shouldBeConsistentWithToZonedDateTime() {
        Duration duration = Duration.ofDays(365 * 54 + 197); // lands in summer
        Converter nyConverter = converterWithZone(ZoneId.of("America/New_York"));

        OffsetDateTime offsetResult = DurationConversions.toOffsetDateTime(duration, nyConverter);
        ZonedDateTime zdtResult = DurationConversions.toZonedDateTime(duration, nyConverter);

        // Both should represent the same instant
        assertEquals(zdtResult.toInstant(), offsetResult.toInstant());
        assertEquals(zdtResult.toOffsetDateTime().getOffset(), offsetResult.getOffset());
    }

    // ---- Bug #2: toSqlDate hardcodes UTC while sibling methods use converter's zone ----

    @Test
    void toSqlDate_shouldRespectConverterTimezone() {
        // Duration of 23 hours — in UTC this is still Jan 1, but in Tokyo (+9) it's already Jan 2
        Duration duration = Duration.ofHours(23);
        Converter tokyoConverter = converterWithZone(ZoneId.of("Asia/Tokyo"));

        java.sql.Date sqlDate = DurationConversions.toSqlDate(duration, tokyoConverter);
        java.time.LocalDate localDate = DurationConversions.toLocalDate(duration, tokyoConverter);

        // toSqlDate and toLocalDate should give the same date
        assertEquals(localDate, sqlDate.toLocalDate());
    }

    @Test
    void toSqlDate_shouldBeConsistentWithToLocalDate() {
        // Duration that lands near midnight boundary — 15 hours after epoch
        // UTC: still Jan 1 (15:00 UTC)
        // Tokyo (+9): Jan 2 (00:00 JST)
        Duration duration = Duration.ofHours(15);
        Converter tokyoConverter = converterWithZone(ZoneId.of("Asia/Tokyo"));

        java.sql.Date sqlDate = DurationConversions.toSqlDate(duration, tokyoConverter);
        java.time.LocalDate localDate = DurationConversions.toLocalDate(duration, tokyoConverter);

        // Both should agree on the date in the converter's zone
        assertEquals(localDate, sqlDate.toLocalDate());
    }
}
