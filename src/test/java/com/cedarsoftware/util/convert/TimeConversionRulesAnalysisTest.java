package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test to analyze the current time conversion rules in the original implementation
 * and compare them to the proposed business rules.
 */
class TimeConversionRulesAnalysisTest {
    private Converter converter;

    @BeforeEach
    void setUp() {
        this.converter = new Converter(new DefaultConverterOptions());
    }

    @Test
    void analyzeCurrentTimeConversionRules() {
        // Create a test time: 2023-01-01 12:00:00.123 UTC
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 123);
        
        long expectedMillis = cal.getTimeInMillis();
        Instant instant = cal.toInstant();
        Date date = cal.getTime();
        
        System.out.println("=== CURRENT IMPLEMENTATION ANALYSIS ===");
        System.out.println("Test time: " + cal.getTime());
        System.out.println("Expected millis: " + expectedMillis);
        System.out.println();
        
        // Test Calendar conversions (legacy class)
        System.out.println("CALENDAR (Legacy - millisecond precision internally):");
        long calToLong = converter.convert(cal, long.class);
        BigInteger calToBigInteger = converter.convert(cal, BigInteger.class);
        BigDecimal calToBigDecimal = converter.convert(cal, BigDecimal.class);
        double calToDouble = converter.convert(cal, double.class);
        
        System.out.printf("  Calendar → long: %d (ratio to millis: %.3f)\n", 
            calToLong, (double)calToLong / expectedMillis);
        System.out.printf("  Calendar → BigInteger: %s (ratio to millis: %.3f)\n", 
            calToBigInteger, calToBigInteger.doubleValue() / expectedMillis);
        System.out.printf("  Calendar → BigDecimal: %s (seconds)\n", calToBigDecimal);
        System.out.printf("  Calendar → double: %.6f (seconds)\n", calToDouble);
        System.out.println();
        
        // Test Instant conversions (modern class)
        System.out.println("INSTANT (Modern - nanosecond precision internally):");
        long instantToLong = converter.convert(instant, long.class);
        BigInteger instantToBigInteger = converter.convert(instant, BigInteger.class);
        BigDecimal instantToBigDecimal = converter.convert(instant, BigDecimal.class);
        double instantToDouble = converter.convert(instant, double.class);
        
        System.out.printf("  Instant → long: %d (ratio to millis: %.3f)\n", 
            instantToLong, (double)instantToLong / expectedMillis);
        System.out.printf("  Instant → BigInteger: %s (ratio to millis: %.3f)\n", 
            instantToBigInteger, instantToBigInteger.doubleValue() / expectedMillis);
        System.out.printf("  Instant → BigDecimal: %s (seconds)\n", instantToBigDecimal);
        System.out.printf("  Instant → double: %.6f (seconds)\n", instantToDouble);
        System.out.println();
        
        // Test Date conversions (legacy class)
        System.out.println("DATE (Legacy - millisecond precision internally):");
        long dateToLong = converter.convert(date, long.class);
        BigInteger dateToBigInteger = converter.convert(date, BigInteger.class);
        BigDecimal dateToBigDecimal = converter.convert(date, BigDecimal.class);
        double dateToDouble = converter.convert(date, double.class);
        
        System.out.printf("  Date → long: %d (ratio to millis: %.3f)\n", 
            dateToLong, (double)dateToLong / expectedMillis);
        System.out.printf("  Date → BigInteger: %s (ratio to millis: %.3f)\n", 
            dateToBigInteger, dateToBigInteger.doubleValue() / expectedMillis);
        System.out.printf("  Date → BigDecimal: %s (seconds)\n", dateToBigDecimal);
        System.out.printf("  Date → double: %.6f (seconds)\n", dateToDouble);
        System.out.println();
        
        // Reverse conversions - what do numbers get interpreted as?
        System.out.println("REVERSE CONVERSIONS (Number → Time):");
        
        // Test long → various time types
        long testLong = expectedMillis;
        Calendar longToCal = converter.convert(testLong, Calendar.class);
        Instant longToInstant = converter.convert(testLong, Instant.class);
        Date longToDate = converter.convert(testLong, Date.class);
        
        System.out.printf("  long %d → Calendar: %s (millis: %d)\n", 
            testLong, longToCal.getTime(), longToCal.getTimeInMillis());
        System.out.printf("  long %d → Instant: %s\n", testLong, longToInstant);
        System.out.printf("  long %d → Date: %s\n", testLong, longToDate);
        System.out.println();
        
        // Test BigInteger → various time types  
        BigInteger testBigInteger = BigInteger.valueOf(expectedMillis);
        Calendar bigIntToCal = converter.convert(testBigInteger, Calendar.class);
        Instant bigIntToInstant = converter.convert(testBigInteger, Instant.class);
        Date bigIntToDate = converter.convert(testBigInteger, Date.class);
        
        System.out.printf("  BigInteger %s → Calendar: %s (millis: %d)\n", 
            testBigInteger, bigIntToCal.getTime(), bigIntToCal.getTimeInMillis());
        System.out.printf("  BigInteger %s → Instant: %s\n", testBigInteger, bigIntToInstant);
        System.out.printf("  BigInteger %s → Date: %s\n", testBigInteger, bigIntToDate);
        System.out.println();
        
        System.out.println("=== ANALYSIS ===");
        System.out.println("Current patterns observed:");
        if (calToLong == expectedMillis) {
            System.out.println("✓ Calendar → long = milliseconds");
        } else {
            System.out.println("✗ Calendar → long ≠ milliseconds (ratio: " + ((double)calToLong / expectedMillis) + ")");
        }
        
        if (instantToLong == expectedMillis) {
            System.out.println("✓ Instant → long = milliseconds");
        } else {
            System.out.println("✗ Instant → long ≠ milliseconds (ratio: " + ((double)instantToLong / expectedMillis) + ")");
        }
        
        double calBigIntRatio = calToBigInteger.doubleValue() / expectedMillis;
        double instantBigIntRatio = instantToBigInteger.doubleValue() / expectedMillis;
        
        System.out.printf("Calendar → BigInteger ratio to millis: %.3f\n", calBigIntRatio);
        System.out.printf("Instant → BigInteger ratio to millis: %.3f\n", instantBigIntRatio);
        
        if (Math.abs(calBigIntRatio - 1.0) < 0.001) {
            System.out.println("✓ Calendar → BigInteger = milliseconds");
        } else if (Math.abs(calBigIntRatio - 1000.0) < 0.001) {
            System.out.println("✓ Calendar → BigInteger = microseconds");
        } else if (Math.abs(calBigIntRatio - 1000000.0) < 0.001) {
            System.out.println("✓ Calendar → BigInteger = nanoseconds");
        } else {
            System.out.println("? Calendar → BigInteger = unknown scale");
        }
        
        if (Math.abs(instantBigIntRatio - 1.0) < 0.001) {
            System.out.println("✓ Instant → BigInteger = milliseconds");
        } else if (Math.abs(instantBigIntRatio - 1000.0) < 0.001) {
            System.out.println("✓ Instant → BigInteger = microseconds");
        } else if (Math.abs(instantBigIntRatio - 1000000.0) < 0.001) {
            System.out.println("✓ Instant → BigInteger = nanoseconds");
        } else {
            System.out.println("? Instant → BigInteger = unknown scale");
        }
    }
}