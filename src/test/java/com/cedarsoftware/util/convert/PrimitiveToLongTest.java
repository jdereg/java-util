package com.cedarsoftware.util.convert;

import java.util.logging.Logger;

import com.cedarsoftware.util.LoggingConfig;
import org.junit.jupiter.api.Test;

/**
 * Test primitive to long conversions
 */
class PrimitiveToLongTest {
    private static final Logger LOG = Logger.getLogger(PrimitiveToLongTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }
    
    @Test
    void testPrimitiveToLongConversions() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test if int → long works
        try {
            long result1 = converter.convert(123, long.class);  // int literal → long
            LOG.info("✓ int to long: " + result1);
        } catch (Exception e) {
            LOG.info("✗ int to long failed: " + e.getMessage());
        }
        
        // Test if boolean → long works  
        try {
            long result2 = converter.convert(true, long.class);  // boolean literal → long
            LOG.info("✓ boolean to long: " + result2);
        } catch (Exception e) {
            LOG.info("✗ boolean to long failed: " + e.getMessage());
        }
        
        // Test atomic integer to int (should work)
        try {
            int result3 = converter.convert(new java.util.concurrent.atomic.AtomicInteger(456), int.class);
            LOG.info("✓ AtomicInteger to int: " + result3);
        } catch (Exception e) {
            LOG.info("✗ AtomicInteger to int failed: " + e.getMessage());
        }
    }
}