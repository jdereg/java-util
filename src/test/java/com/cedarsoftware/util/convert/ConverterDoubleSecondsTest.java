package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A double count of seconds converts to a date or time as the decimal it prints: 1.001 seconds is 1001 milliseconds,
 * and 1700000000.123 is .123 of a second. Date and Calendar multiplied the double by 1000 and truncated (1.001 gave
 * 1000 ms, 64.1 gave 64099: 741 of the first 100,000 millisecond values lost one); Instant, Duration and the zoned
 * types rounded its binary fraction to the nanosecond, which at today's epoch seconds is off by hundreds of
 * nanoseconds (0.12299990654 gave .122999907).
 * Truncation stays where it was: a Date keeps whole milliseconds, toward zero.
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
class ConverterDoubleSecondsTest {

    private final Converter converter = new Converter(new DefaultConverterOptions() {
        @Override
        public ZoneId getZoneId() {
            return ZoneId.of("UTC");
        }
    });

    @Test
    void dateAndCalendarTakeTheMillisecondsOfTheDecimal() {
        assertEquals(1001L, converter.convert(1.001, Date.class).getTime());           // times 1000: 1000.99999...
        assertEquals(64100L, converter.convert(64.1, Date.class).getTime());           // times 1000: 64099.99999...
        assertEquals(-1001L, converter.convert(-1.001, Date.class).getTime());
        assertEquals(65655123L, converter.convert(65655.123, Date.class).getTime());
        assertEquals(1700000000123L, converter.convert(1700000000.123, Date.class).getTime());
        assertEquals(1001L, converter.convert(1.001, Calendar.class).getTimeInMillis());
        assertEquals(1000L, converter.convert(1.0005, Date.class).getTime());          // a Date truncates, as before
        assertEquals(-1000L, converter.convert(-1.0005, Date.class).getTime());        // toward zero
    }

    @Test
    void instantAndDurationTakeTheNanosecondsOfTheDecimal() {
        assertEquals(Instant.ofEpochSecond(1700000000L, 123_000_000), converter.convert(1700000000.123, Instant.class));
        assertEquals(Instant.ofEpochSecond(65655L, 123_000_000), converter.convert(65655.123, Instant.class));
        assertEquals(Instant.ofEpochSecond(-2L, 500_000_000), converter.convert(-1.5, Instant.class));
        assertEquals(Duration.ofSeconds(1700000000L, 123_000_000), converter.convert(1700000000.123, Duration.class));
        assertEquals(Duration.ofSeconds(-1L, -250_000_000), converter.convert(-1.25, Duration.class));
        Timestamp ts = converter.convert(1700000000.123, Timestamp.class);
        assertEquals(123_000_000, ts.getNanos());
    }

    @Test
    void zonedAndLocalTypesTakeTheNanosecondsOfTheDecimal() {
        assertEquals(123_000_000, converter.convert(1700000000.123, ZonedDateTime.class).getNano());
        assertEquals(123_000_000, converter.convert(1700000000.123, OffsetDateTime.class).getNano());
        assertEquals(123_000_000, converter.convert(1700000000.123, LocalDateTime.class).getNano());
        assertEquals(123_000_000, converter.convert(1700000000.123, OffsetTime.class).getNano());
        assertEquals(LocalTime.of(18, 14, 15, 123_000_000), converter.convert(65655.123, LocalTime.class));
        assertEquals(LocalTime.of(23, 59, 59, 999_999_999), converter.convert(86399.9999999996, LocalTime.class));
    }

    /** At a long's end the double's own value still converts: Date(Long.MIN_VALUE) round-trips through seconds. */
    @Test
    void theEndsOfALongStillConvert() {
        double seconds = converter.convert(new Date(Long.MIN_VALUE), Double.class);
        assertEquals(Long.MIN_VALUE, converter.convert(seconds, Date.class).getTime());
    }
}
