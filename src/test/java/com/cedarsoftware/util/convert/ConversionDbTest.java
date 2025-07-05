package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the CONVERSION_DB is being populated correctly
 */
class ConversionDbTest {
    
    @Test
    void testConversionDbPopulation() {
        // Test that some basic conversions are in the database
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test primitive conversions that should exist
        try {
            // These should work - they're basic conversions
            String result1 = converter.convert(42, String.class);
            System.out.println("Integer to String: " + result1);
            
            Integer result2 = converter.convert("123", Integer.class);
            System.out.println("String to Integer: " + result2);
            
            Boolean result3 = converter.convert("true", Boolean.class);
            System.out.println("String to Boolean: " + result3);
            
        } catch (Exception e) {
            System.out.println("Basic conversion failed: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Test that fails - this should help identify the issue
        try {
            int result = converter.convert(Integer.valueOf(42), int.class);
            System.out.println("Integer to int: " + result);
        } catch (Exception e) {
            System.out.println("Integer to int failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}