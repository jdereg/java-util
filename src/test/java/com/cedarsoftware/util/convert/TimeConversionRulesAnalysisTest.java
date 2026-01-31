package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

import com.cedarsoftware.util.LoggingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test to analyze the current time conversion rules in the original implementation
 * and compare them to the proposed business rules.
 */
class TimeConversionRulesAnalysisTest {
    
    private static final Logger LOG = Logger.getLogger(TimeConversionRulesAnalysisTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }
    
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
        
        LOG.info("=== CURRENT IMPLEMENTATION ANALYSIS ===");
        LOG.info("Test time: " + cal.getTime());
        LOG.info("Expected millis: " + expectedMillis);
        LOG.info("");
        
        // Test Calendar conversions (legacy class)
        LOG.info("CALENDAR (Legacy - millisecond precision internally):");
        long calToLong = converter.convert(cal, long.class);
        BigInteger calToBigInteger = converter.convert(cal, BigInteger.class);
        BigDecimal calToBigDecimal = converter.convert(cal, BigDecimal.class);
        double calToDouble = converter.convert(cal, double.class);
        
        LOG.info(String.format("  Calendar → long: %d (ratio to millis: %.3f)", 
            calToLong, (double)calToLong / expectedMillis));
        LOG.info(String.format("  Calendar → BigInteger: %s (ratio to millis: %.3f)", 
            calToBigInteger, calToBigInteger.doubleValue() / expectedMillis));
        LOG.info(String.format("  Calendar → BigDecimal: %s (seconds)", calToBigDecimal));
        LOG.info(String.format("  Calendar → double: %.6f (seconds)", calToDouble));
        LOG.info("");
        
        // Test Instant conversions (modern class)
        LOG.info("INSTANT (Modern - nanosecond precision internally):");
        long instantToLong = converter.convert(instant, long.class);
        BigInteger instantToBigInteger = converter.convert(instant, BigInteger.class);
        BigDecimal instantToBigDecimal = converter.convert(instant, BigDecimal.class);
        double instantToDouble = converter.convert(instant, double.class);
        
        LOG.info(String.format("  Instant → long: %d (ratio to millis: %.3f)", 
            instantToLong, (double)instantToLong / expectedMillis));
        LOG.info(String.format("  Instant → BigInteger: %s (ratio to millis: %.3f)", 
            instantToBigInteger, instantToBigInteger.doubleValue() / expectedMillis));
        LOG.info(String.format("  Instant → BigDecimal: %s (seconds)", instantToBigDecimal));
        LOG.info(String.format("  Instant → double: %.6f (seconds)", instantToDouble));
        LOG.info("");
        
        // Test Date conversions (legacy class)
        LOG.info("DATE (Legacy - millisecond precision internally):");
        long dateToLong = converter.convert(date, long.class);
        BigInteger dateToBigInteger = converter.convert(date, BigInteger.class);
        BigDecimal dateToBigDecimal = converter.convert(date, BigDecimal.class);
        double dateToDouble = converter.convert(date, double.class);
        
        LOG.info(String.format("  Date → long: %d (ratio to millis: %.3f)", 
            dateToLong, (double)dateToLong / expectedMillis));
        LOG.info(String.format("  Date → BigInteger: %s (ratio to millis: %.3f)", 
            dateToBigInteger, dateToBigInteger.doubleValue() / expectedMillis));
        LOG.info(String.format("  Date → BigDecimal: %s (seconds)", dateToBigDecimal));
        LOG.info(String.format("  Date → double: %.6f (seconds)", dateToDouble));
        LOG.info("");
        
        // Reverse conversions - what do numbers get interpreted as?
        LOG.info("REVERSE CONVERSIONS (Number → Time):");
        
        // Test long → various time types
        long testLong = expectedMillis;
        Calendar longToCal = converter.convert(testLong, Calendar.class);
        Instant longToInstant = converter.convert(testLong, Instant.class);
        Date longToDate = converter.convert(testLong, Date.class);
        
        LOG.info(String.format("  long %d → Calendar: %s (millis: %d)", 
            testLong, longToCal.getTime(), longToCal.getTimeInMillis()));
        LOG.info(String.format("  long %d → Instant: %s", testLong, longToInstant));
        LOG.info(String.format("  long %d → Date: %s", testLong, longToDate));
        LOG.info("");
        
        // Test BigInteger → various time types  
        BigInteger testBigInteger = BigInteger.valueOf(expectedMillis);
        Calendar bigIntToCal = converter.convert(testBigInteger, Calendar.class);
        Instant bigIntToInstant = converter.convert(testBigInteger, Instant.class);
        Date bigIntToDate = converter.convert(testBigInteger, Date.class);
        
        LOG.info(String.format("  BigInteger %s → Calendar: %s (millis: %d)", 
            testBigInteger, bigIntToCal.getTime(), bigIntToCal.getTimeInMillis()));
        LOG.info(String.format("  BigInteger %s → Instant: %s", testBigInteger, bigIntToInstant));
        LOG.info(String.format("  BigInteger %s → Date: %s", testBigInteger, bigIntToDate));
        LOG.info("");
        
        LOG.info("=== ANALYSIS ===");
        LOG.info("Current patterns observed:");
        if (calToLong == expectedMillis) {
            LOG.info("✓ Calendar → long = milliseconds");
        } else {
            LOG.info("✗ Calendar → long ≠ milliseconds (ratio: " + ((double)calToLong / expectedMillis) + ")");
        }
        
        if (instantToLong == expectedMillis) {
            LOG.info("✓ Instant → long = milliseconds");
        } else {
            LOG.info("✗ Instant → long ≠ milliseconds (ratio: " + ((double)instantToLong / expectedMillis) + ")");
        }
        
        double calBigIntRatio = calToBigInteger.doubleValue() / expectedMillis;
        double instantBigIntRatio = instantToBigInteger.doubleValue() / expectedMillis;
        
        LOG.info(String.format("Calendar → BigInteger ratio to millis: %.3f", calBigIntRatio));
        LOG.info(String.format("Instant → BigInteger ratio to millis: %.3f", instantBigIntRatio));
        
        if (Math.abs(calBigIntRatio - 1.0) < 0.001) {
            LOG.info("✓ Calendar → BigInteger = milliseconds");
        } else if (Math.abs(calBigIntRatio - 1000.0) < 0.001) {
            LOG.info("✓ Calendar → BigInteger = microseconds");
        } else if (Math.abs(calBigIntRatio - 1000000.0) < 0.001) {
            LOG.info("✓ Calendar → BigInteger = nanoseconds");
        } else {
            LOG.info("? Calendar → BigInteger = unknown scale");
        }
        
        if (Math.abs(instantBigIntRatio - 1.0) < 0.001) {
            LOG.info("✓ Instant → BigInteger = milliseconds");
        } else if (Math.abs(instantBigIntRatio - 1000.0) < 0.001) {
            LOG.info("✓ Instant → BigInteger = microseconds");
        } else if (Math.abs(instantBigIntRatio - 1000000.0) < 0.001) {
            LOG.info("✓ Instant → BigInteger = nanoseconds");
        } else {
            LOG.info("? Instant → BigInteger = unknown scale");
        }
    }
}