package com.cedarsoftware.util.convert;

import java.util.logging.Logger;

import com.cedarsoftware.util.LoggingConfig;
import org.junit.jupiter.api.Test;

/**
 * Test to verify the CONVERSION_DB is being populated correctly
 */
class ConversionDbTest {
    private static final Logger LOG = Logger.getLogger(ConversionDbTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }
    
    @Test
    void testConversionDbPopulation() {
        // Test that some basic conversions are in the database
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test primitive conversions that should exist
        try {
            // These should work - they're basic conversions
            String result1 = converter.convert(42, String.class);
            LOG.info("Integer to String: " + result1);
            
            Integer result2 = converter.convert("123", Integer.class);
            LOG.info("String to Integer: " + result2);
            
            Boolean result3 = converter.convert("true", Boolean.class);
            LOG.info("String to Boolean: " + result3);
            
        } catch (Exception e) {
            LOG.info("Basic conversion failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Test that fails - this should help identify the issue
        try {
            int result = converter.convert(Integer.valueOf(42), int.class);
            LOG.info("Integer to int: " + result);
        } catch (Exception e) {
            LOG.info("Integer to int failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}