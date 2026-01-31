package com.cedarsoftware.util.convert;

import java.util.logging.Logger;

import com.cedarsoftware.util.LoggingConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Quick test to verify ConversionTripleMap integration works.
 */
class QuickConverterTest {
    private static final Logger LOG = Logger.getLogger(QuickConverterTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }
    
    @Test
    void testBasicStringToBooleanConversion() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test basic conversions
        try {
            Boolean result = converter.convert("true", Boolean.class);
            assertEquals(true, result);
            LOG.info("✓ String to Boolean conversion works: " + result);
        } catch (Exception e) {
            LOG.info("✗ String to Boolean conversion failed: " + e.getMessage());
            e.printStackTrace();
            fail("String to Boolean conversion should work");
        }
    }
    
    @Test 
    void testBasicStringToIntegerConversion() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        try {
            Integer result = converter.convert("42", Integer.class);
            assertEquals(42, result);
            LOG.info("✓ String to Integer conversion works: " + result);
        } catch (Exception e) {
            LOG.info("✗ String to Integer conversion failed: " + e.getMessage());
            e.printStackTrace();
            fail("String to Integer conversion should work");
        }
    }
}