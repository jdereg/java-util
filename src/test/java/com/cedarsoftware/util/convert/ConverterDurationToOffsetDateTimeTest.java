package com.cedarsoftware.util.convert;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test Duration to OffsetDateTime conversion
 */
public class ConverterDurationToOffsetDateTimeTest {

    @Test
    void testDurationToOffsetDateTimeBasic() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test PT0S (zero duration)
        Duration zeroDuration = Duration.ZERO;
        OffsetDateTime result = converter.convert(zeroDuration, OffsetDateTime.class);
        
        // Should be epoch (1970-01-01T00:00:00Z) in the converter's timezone
        TimeZone tz = converter.getOptions().getTimeZone();
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(tz.getOffset(System.currentTimeMillis()) / 1000);
        OffsetDateTime expected = Instant.EPOCH.atOffset(expectedOffset);
        
        assertEquals(expected, result);
    }

    @Test
    void testDurationToOffsetDateTimePositive() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test PT1H (1 hour)
        Duration duration = Duration.ofHours(1);
        OffsetDateTime result = converter.convert(duration, OffsetDateTime.class);
        
        // Should be epoch + 1 hour
        TimeZone tz = converter.getOptions().getTimeZone();
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(tz.getOffset(System.currentTimeMillis()) / 1000);
        OffsetDateTime expected = Instant.EPOCH.plus(duration).atOffset(expectedOffset);
        
        assertEquals(expected, result);
    }

    @Test
    void testDurationToOffsetDateTimeNegative() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test PT-1H (negative 1 hour)  
        Duration duration = Duration.ofHours(-1);
        OffsetDateTime result = converter.convert(duration, OffsetDateTime.class);
        
        // Should be epoch - 1 hour
        TimeZone tz = converter.getOptions().getTimeZone();
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(tz.getOffset(System.currentTimeMillis()) / 1000);
        OffsetDateTime expected = Instant.EPOCH.plus(duration).atOffset(expectedOffset);
        
        assertEquals(expected, result);
    }

    @Test
    void testDurationToOffsetDateTimeComplexDuration() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test PT1H30M45S (1 hour, 30 minutes, 45 seconds)
        Duration duration = Duration.ofHours(1).plusMinutes(30).plusSeconds(45);
        OffsetDateTime result = converter.convert(duration, OffsetDateTime.class);
        
        // Should be epoch + duration
        TimeZone tz = converter.getOptions().getTimeZone();
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(tz.getOffset(System.currentTimeMillis()) / 1000);
        OffsetDateTime expected = Instant.EPOCH.plus(duration).atOffset(expectedOffset);
        
        assertEquals(expected, result);
    }

    @Test
    void testDurationToOffsetDateTimeCustomTimeZone() {
        ConverterOptions options = new DefaultConverterOptions() {
            @Override
            public TimeZone getTimeZone() {
                return TimeZone.getTimeZone("UTC");
            }
        };
        Converter converter = new Converter(options);
        
        // Test PT24H (24 hours)
        Duration duration = Duration.ofHours(24);
        OffsetDateTime result = converter.convert(duration, OffsetDateTime.class);
        
        // Should be epoch + 24 hours in UTC
        OffsetDateTime expected = Instant.EPOCH.plus(duration).atOffset(ZoneOffset.UTC);
        
        assertEquals(expected, result);
    }

    @Test
    void testDurationToOffsetDateTimeNanosecondPrecision() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test nanosecond precision with PT1.123456789S
        Duration duration = Duration.ofSeconds(1, 123456789);
        OffsetDateTime result = converter.convert(duration, OffsetDateTime.class);
        
        // Should preserve nanosecond precision
        TimeZone tz = converter.getOptions().getTimeZone();
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(tz.getOffset(System.currentTimeMillis()) / 1000);
        OffsetDateTime expected = Instant.EPOCH.plus(duration).atOffset(expectedOffset);
        
        assertEquals(expected, result);
        assertEquals(123456789, result.getNano());
    }
}