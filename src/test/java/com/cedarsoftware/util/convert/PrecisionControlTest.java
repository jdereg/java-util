package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;

/**
 * Test class for time conversion precision control options.
 * Tests all 3 configurable precision rules:
 * 1. Modern Time Classes Long Precision (Instant, LocalDateTime, etc.)
 * 2. Duration Long Precision 
 * 3. LocalTime Long Precision
 */
class PrecisionControlTest {

    private String originalModernTimePrecision;
    private String originalDurationPrecision;  
    private String originalLocalTimePrecision;

    @BeforeEach
    void setUp() {
        // Save original system properties
        originalModernTimePrecision = System.getProperty("cedarsoftware.converter.modern.time.long.precision");
        originalDurationPrecision = System.getProperty("cedarsoftware.converter.duration.long.precision");
        originalLocalTimePrecision = System.getProperty("cedarsoftware.converter.localtime.long.precision");
    }

    @AfterEach
    void tearDown() {
        // Restore original system properties
        clearSystemProperty("cedarsoftware.converter.modern.time.long.precision", originalModernTimePrecision);
        clearSystemProperty("cedarsoftware.converter.duration.long.precision", originalDurationPrecision);
        clearSystemProperty("cedarsoftware.converter.localtime.long.precision", originalLocalTimePrecision);
    }

    private void clearSystemProperty(String key, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }

    @Test
    void testModernTimePrecisionSystemPropertyMilliseconds() {
        // Test system property with milliseconds
        System.setProperty("cedarsoftware.converter.modern.time.long.precision", "millis");
        
        Converter converter = new Converter(new DefaultConverterOptions());
        long epochMilli = 1687612649729L; // 2023-06-24T15:57:29.729Z
        
        Instant instant = converter.convert(epochMilli, Instant.class);
        assertThat(instant).isEqualTo(Instant.ofEpochMilli(epochMilli));
        
        // Test round trip
        long backToLong = converter.convert(instant, Long.class);
        assertThat(backToLong).isEqualTo(epochMilli);
    }

    @Test
    void testModernTimePrecisionSystemPropertyNanoseconds() {
        // Test system property with nanoseconds
        System.setProperty("cedarsoftware.converter.modern.time.long.precision", "nanos");
        
        Converter converter = new Converter(new DefaultConverterOptions());
        long epochNanos = 1687612649729000000L; // 2023-06-24T15:57:29.729Z in nanos
        
        Instant instant = converter.convert(epochNanos, Instant.class);
        assertThat(instant).isEqualTo(Instant.ofEpochSecond(epochNanos / 1_000_000_000L, epochNanos % 1_000_000_000L));
        
        // Test round trip
        long backToLong = converter.convert(instant, Long.class);
        assertThat(backToLong).isEqualTo(epochNanos);
    }

    @Test
    void testModernTimePrecisionConverterOptionWhenNoSystemProperty() {
        // Test converter option fallback when no system property is set
        // Clear any system property
        System.clearProperty("cedarsoftware.converter.modern.time.long.precision");
        
        // Create converter with nanoseconds option
        ConverterOptions options = new DefaultConverterOptions();
        options.getCustomOptions().put("modern.time.long.precision", "nanos");
        
        Converter converter = new Converter(options);
        long epochNanos = 1687612649729000000L; // 2023-06-24T15:57:29.729Z in nanos
        
        Instant instant = converter.convert(epochNanos, Instant.class);
        assertThat(instant).isEqualTo(Instant.ofEpochSecond(epochNanos / 1_000_000_000L, epochNanos % 1_000_000_000L));
    }

    @Test
    void testDurationPrecisionSystemPropertyMilliseconds() {
        // Test Duration precision with milliseconds
        System.setProperty("cedarsoftware.converter.duration.long.precision", "millis");
        
        Converter converter = new Converter(new DefaultConverterOptions());
        long millis = 5000L; // 5 seconds
        
        Duration duration = converter.convert(millis, Duration.class);
        assertThat(duration).isEqualTo(Duration.ofMillis(millis));
        
        // Test round trip
        long backToLong = converter.convert(duration, Long.class);
        assertThat(backToLong).isEqualTo(millis);
    }

    @Test
    void testDurationPrecisionSystemPropertyNanoseconds() {
        // Test Duration precision with nanoseconds
        System.setProperty("cedarsoftware.converter.duration.long.precision", "nanos");
        
        Converter converter = new Converter(new DefaultConverterOptions());
        long nanos = 5000000000L; // 5 seconds in nanos
        
        Duration duration = converter.convert(nanos, Duration.class);
        assertThat(duration).isEqualTo(Duration.ofNanos(nanos));
        
        // Test round trip
        long backToLong = converter.convert(duration, Long.class);
        assertThat(backToLong).isEqualTo(nanos);
    }

    @Test
    void testLocalTimePrecisionSystemPropertyMilliseconds() {
        // Test LocalTime precision with milliseconds
        System.setProperty("cedarsoftware.converter.localtime.long.precision", "millis");
        
        Converter converter = new Converter(new DefaultConverterOptions());
        long millis = 3661123L; // 1 hour, 1 minute, 1 second, 123 milliseconds
        
        LocalTime localTime = converter.convert(millis, LocalTime.class);
        assertThat(localTime).isEqualTo(LocalTime.ofNanoOfDay(millis * 1_000_000L));
        
        // Test round trip
        long backToLong = converter.convert(localTime, Long.class);
        assertThat(backToLong).isEqualTo(millis);
    }

    @Test
    void testLocalTimePrecisionSystemPropertyNanoseconds() {
        // Test LocalTime precision with nanoseconds (use small valid value)
        System.setProperty("cedarsoftware.converter.localtime.long.precision", "nanos");
        
        Converter converter = new Converter(new DefaultConverterOptions());
        long nanos = 3661123000000L; // 1 hour, 1 minute, 1 second, 123 milliseconds in nanos
        
        LocalTime localTime = converter.convert(nanos, LocalTime.class);
        assertThat(localTime).isEqualTo(LocalTime.ofNanoOfDay(nanos));
        
        // Test round trip
        long backToLong = converter.convert(localTime, Long.class);
        assertThat(backToLong).isEqualTo(nanos);
    }

    @Test
    void testMultiplePrecisionOptionsWorkingTogether() {
        // Test all 3 precision options working together with different settings
        System.setProperty("cedarsoftware.converter.modern.time.long.precision", "millis");
        System.setProperty("cedarsoftware.converter.duration.long.precision", "nanos");
        System.setProperty("cedarsoftware.converter.localtime.long.precision", "millis");
        
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test Instant (should use milliseconds)
        long epochMilli = 1687612649729L;
        Instant instant = converter.convert(epochMilli, Instant.class);
        assertThat(instant).isEqualTo(Instant.ofEpochMilli(epochMilli));
        
        // Test Duration (should use nanoseconds)
        long nanos = 5000000000L;
        Duration duration = converter.convert(nanos, Duration.class);
        assertThat(duration).isEqualTo(Duration.ofNanos(nanos));
        
        // Test LocalTime (should use milliseconds)
        long millis = 3661123L;
        LocalTime localTime = converter.convert(millis, LocalTime.class);
        assertThat(localTime).isEqualTo(LocalTime.ofNanoOfDay(millis * 1_000_000L));
    }

    @Test
    void testTwoConverterInstancesWithDifferentPrecisionOptions() {
        // Clear system properties to test converter options
        System.clearProperty("cedarsoftware.converter.modern.time.long.precision");
        
        // Converter 1: milliseconds for modern time
        ConverterOptions options1 = new DefaultConverterOptions();
        options1.getCustomOptions().put("modern.time.long.precision", "millis");
        Converter converter1 = new Converter(options1);
        
        // Converter 2: nanoseconds for modern time  
        ConverterOptions options2 = new DefaultConverterOptions();
        options2.getCustomOptions().put("modern.time.long.precision", "nanos");
        Converter converter2 = new Converter(options2);
        
        // Test same long value with both converters
        long value = 1687612649729L;
        
        // Converter 1 should treat as milliseconds
        Instant instant1 = converter1.convert(value, Instant.class);
        assertThat(instant1).isEqualTo(Instant.ofEpochMilli(value));
        
        // Converter 2 should treat as nanoseconds
        Instant instant2 = converter2.convert(value, Instant.class);
        assertThat(instant2).isEqualTo(Instant.ofEpochSecond(value / 1_000_000_000L, value % 1_000_000_000L));
        
        // Results should be different
        assertThat(instant1).isNotEqualTo(instant2);
    }

    @Test
    void testDefaultBehaviorWithoutPrecisionOptions() {
        // Test that default behavior is milliseconds when no precision options are set
        System.clearProperty("cedarsoftware.converter.modern.time.long.precision");
        System.clearProperty("cedarsoftware.converter.duration.long.precision");
        System.clearProperty("cedarsoftware.converter.localtime.long.precision");
        
        Converter converter = new Converter(new DefaultConverterOptions());
        
        long epochMilli = 1687612649729L;
        
        // Should default to milliseconds
        Instant instant = converter.convert(epochMilli, Instant.class);
        assertThat(instant).isEqualTo(Instant.ofEpochMilli(epochMilli));
        
        // Duration should also default to milliseconds  
        long millis = 5000L;
        Duration duration = converter.convert(millis, Duration.class);
        assertThat(duration).isEqualTo(Duration.ofMillis(millis));
        
        // LocalTime should also default to milliseconds (use small value)
        long localTimeMillis = 3661123L; // valid LocalTime range
        LocalTime localTime = converter.convert(localTimeMillis, LocalTime.class);
        assertThat(localTime).isEqualTo(LocalTime.ofNanoOfDay(localTimeMillis * 1_000_000L));
    }

    @Test
    void testSystemPropertyTakesPrecedenceOverConverterOption() {
        // Test that system property overrides converter option
        System.setProperty("cedarsoftware.converter.modern.time.long.precision", "millis");
        
        // Create converter with nanoseconds option (should be ignored)
        ConverterOptions options = new DefaultConverterOptions();
        options.getCustomOptions().put("modern.time.long.precision", "nanos");
        
        Converter converter = new Converter(options);
        long epochMilli = 1687612649729L;
        
        // Should use system property (milliseconds), not converter option (nanoseconds)
        Instant instant = converter.convert(epochMilli, Instant.class);
        assertThat(instant).isEqualTo(Instant.ofEpochMilli(epochMilli));
    }
}