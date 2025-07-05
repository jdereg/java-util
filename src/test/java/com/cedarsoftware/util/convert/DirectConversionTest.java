package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test direct conversions to understand what works
 */
class DirectConversionTest {
    
    @Test
    void testDirectConversions() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        // Test what direct conversions work for long
        try {
            long result1 = converter.convert(Integer.valueOf(123), long.class);
            System.out.println("✓ Integer to long: " + result1);
        } catch (Exception e) {
            System.out.println("✗ Integer to long failed: " + e.getMessage());
        }
        
        try {
            long result2 = converter.convert(Boolean.valueOf(true), long.class);
            System.out.println("✓ Boolean to long: " + result2);
        } catch (Exception e) {
            System.out.println("✗ Boolean to long failed: " + e.getMessage());
        }
        
        // Test if AtomicInteger→Integer works
        try {
            Integer result3 = converter.convert(new java.util.concurrent.atomic.AtomicInteger(456), Integer.class);
            System.out.println("✓ AtomicInteger to Integer: " + result3);
        } catch (Exception e) {
            System.out.println("✗ AtomicInteger to Integer failed: " + e.getMessage());
        }
    }
}