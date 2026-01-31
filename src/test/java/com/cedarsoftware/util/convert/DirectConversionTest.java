package com.cedarsoftware.util.convert;

import java.util.logging.Logger;

import com.cedarsoftware.util.LoggingConfig;
import org.junit.jupiter.api.Test;

/**
 * Test direct conversions to understand what works
 */
class DirectConversionTest {
    private static final Logger LOG = Logger.getLogger(DirectConversionTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }
    
    @Test
    void testDirectConversions() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test what direct conversions work for long
        try {
            long result1 = converter.convert(Integer.valueOf(123), long.class);
            LOG.info("✓ Integer to long: " + result1);
        } catch (Exception e) {
            LOG.info("✗ Integer to long failed: " + e.getMessage());
        }
        
        try {
            long result2 = converter.convert(Boolean.valueOf(true), long.class);
            LOG.info("✓ Boolean to long: " + result2);
        } catch (Exception e) {
            LOG.info("✗ Boolean to long failed: " + e.getMessage());
        }
        
        // Test if AtomicInteger→Integer works
        try {
            Integer result3 = converter.convert(new java.util.concurrent.atomic.AtomicInteger(456), Integer.class);
            LOG.info("✓ AtomicInteger to Integer: " + result3);
        } catch (Exception e) {
            LOG.info("✗ AtomicInteger to Integer failed: " + e.getMessage());
        }
    }
}