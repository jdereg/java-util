package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test primitive to long conversions
 */
class PrimitiveToLongTest {
    
    @Test
    void testPrimitiveToLongConversions() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test if int → long works
        try {
            long result1 = converter.convert(123, long.class);  // int literal → long
            System.out.println("✓ int to long: " + result1);
        } catch (Exception e) {
            System.out.println("✗ int to long failed: " + e.getMessage());
        }
        
        // Test if boolean → long works  
        try {
            long result2 = converter.convert(true, long.class);  // boolean literal → long
            System.out.println("✓ boolean to long: " + result2);
        } catch (Exception e) {
            System.out.println("✗ boolean to long failed: " + e.getMessage());
        }
        
        // Test atomic integer to int (should work)
        try {
            int result3 = converter.convert(new java.util.concurrent.atomic.AtomicInteger(456), int.class);
            System.out.println("✓ AtomicInteger to int: " + result3);
        } catch (Exception e) {
            System.out.println("✗ AtomicInteger to int failed: " + e.getMessage());
        }
    }
}