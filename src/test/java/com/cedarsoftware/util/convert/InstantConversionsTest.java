package com.cedarsoftware.util.convert;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for InstantConversions bugs.
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
class InstantConversionsTest {

    private static Converter converterWithZone(ZoneId zoneId) {
        ConverterOptions options = new ConverterOptions() {
            @Override
            public <T> T getCustomOption(String name) { return null; }

            @Override
            public ZoneId getZoneId() { return zoneId; }
        };
        return new Converter(options);
    }

    // ---- Bug: toOffsetDateTime uses System.currentTimeMillis() for offset calculation ----
    // It should use the instant's own epoch millis to determine the correct DST offset.

    @Test
    void toOffsetDateTime_summerInstant_shouldUseInstantTimeForOffset() {
        // July 15, 2024 12:00 UTC — during US Eastern Daylight Time (EDT = UTC-4)
        Instant summerInstant = LocalDate.of(2024, 7, 15)
                .atTime(LocalTime.NOON)
                .toInstant(ZoneOffset.UTC);

        Converter nyConverter = converterWithZone(ZoneId.of("America/New_York"));

        OffsetDateTime result = InstantConversions.toOffsetDateTime(summerInstant, nyConverter);

        // The offset should be -04:00 (EDT), not -05:00 (EST)
        // Before fix: uses System.currentTimeMillis() which is February (EST = -05:00)
        assertEquals(ZoneOffset.ofHours(-4), result.getOffset());
        assertEquals(8, result.getHour()); // 12:00 UTC = 08:00 EDT
    }

    @Test
    void toOffsetDateTime_winterInstant_shouldUseInstantTimeForOffset() {
        // January 15, 2024 12:00 UTC — during US Eastern Standard Time (EST = UTC-5)
        Instant winterInstant = LocalDate.of(2024, 1, 15)
                .atTime(LocalTime.NOON)
                .toInstant(ZoneOffset.UTC);

        Converter nyConverter = converterWithZone(ZoneId.of("America/New_York"));

        OffsetDateTime result = InstantConversions.toOffsetDateTime(winterInstant, nyConverter);

        // The offset should be -05:00 (EST)
        assertEquals(ZoneOffset.ofHours(-5), result.getOffset());
        assertEquals(7, result.getHour()); // 12:00 UTC = 07:00 EST
    }

    @Test
    void toOffsetDateTime_utcZone_alwaysZeroOffset() {
        Instant instant = Instant.parse("2024-07-15T12:00:00Z");
        Converter utcConverter = converterWithZone(ZoneId.of("UTC"));

        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, utcConverter);

        assertEquals(ZoneOffset.UTC, result.getOffset());
        assertEquals(12, result.getHour());
    }

    @Test
    void toOffsetDateTime_fixedOffsetZone_works() {
        Instant instant = Instant.parse("2024-07-15T12:00:00Z");
        Converter tokyoConverter = converterWithZone(ZoneId.of("Asia/Tokyo"));

        OffsetDateTime result = InstantConversions.toOffsetDateTime(instant, tokyoConverter);

        // Tokyo is always UTC+9 (no DST)
        assertEquals(ZoneOffset.ofHours(9), result.getOffset());
        assertEquals(21, result.getHour()); // 12:00 UTC = 21:00 JST
    }
}
