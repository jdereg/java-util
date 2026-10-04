package com.cedarsoftware.util.convert;

import java.time.Duration;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A double of seconds converts to Duration, LocalTime and OffsetTime rounded to the nearest nanosecond, as it
 * converts to Instant, so a millisecond value survives a round trip through double.
 */
class ConverterDoubleToTimeTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    /**
     * The fraction was truncated: 1.001 is a little below 1.001 as a double, so Duration.ofMillis(1001) came back
     * as PT1.000999999S, and about half of all millisecond values came back a nanosecond short.
     */
    @Test
    void millisecondValuesSurviveARoundTripThroughDouble() {
        for (long millis = 0; millis < 200_000; millis += 7) {
            Duration duration = Duration.ofMillis(millis);
            double seconds = converter.convert(duration, double.class);
            assertEquals(duration, converter.convert(seconds, Duration.class), "Duration " + millis);
            LocalTime time = LocalTime.ofNanoOfDay(millis * 1_000_000L);
            double ofDay = converter.convert(time, double.class);
            assertEquals(time, converter.convert(ofDay, LocalTime.class), "LocalTime " + millis);
        }
        assertEquals(Duration.ofMillis(1001), converter.convert(1.001, Duration.class));
        assertEquals(LocalTime.of(0, 0, 1, 1_000_000), converter.convert(1.001, LocalTime.class));
        OffsetTime offsetTime = converter.convert(1.001, OffsetTime.class);
        assertEquals(1_000_000, offsetTime.getNano(), String.valueOf(offsetTime));
        // just under midnight rounds to the last nanosecond of the day, not to an invalid 24:00
        assertEquals(LocalTime.MAX, converter.convert(86399.99999999999, LocalTime.class));
        assertEquals(Duration.ofMillis(-1), converter.convert(-0.001, Duration.class));
    }
}
