package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

/**
 * A Timestamp's nanoseconds survive conversion to every type that can hold them.
 */
class ConverterTimestampNanosTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /**
     * Timestamp to Timestamp, to ZonedDateTime, and to the types reached through ZonedDateTime (LocalTime) kept only
     * the milliseconds: they used the java.util.Date conversions, which read getTime(). Instant, LocalDateTime,
     * OffsetDateTime, String and BigInteger already kept them.
     */
    @Test
    void nanosecondsSurviveEveryConversionThatCanHoldThem() {
        Timestamp ts = Timestamp.from(Instant.parse("2024-01-15T10:20:30.123456789Z"));
        Timestamp copy = converter.convert(ts, Timestamp.class);
        assertEquals(ts, copy);
        assertEquals(123_456_789, copy.getNanos());
        assertNotSame(ts, copy, "a copy, as for any mutable source");
        assertEquals(123_456_789, converter.convert(ts, ZonedDateTime.class).getNano());
        assertEquals(123_456_789, converter.convert(ts, LocalTime.class).getNano());
        assertEquals(123_456_789, converter.convert(ts, Instant.class).getNano());
        assertEquals(123_456_789, converter.convert(ts, LocalDateTime.class).getNano());
        assertEquals(123_456_789, converter.convert(ts, OffsetDateTime.class).getNano());
        assertEquals(ts, converter.convert(converter.convert(ts, ZonedDateTime.class), Timestamp.class));
    }
}
