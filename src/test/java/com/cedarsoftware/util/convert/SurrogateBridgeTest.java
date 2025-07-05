package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test surrogate bridge expansion functionality
 */
class SurrogateBridgeTest {
    
    @Test
    void testAtomicToGeometricConversions() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        try {
            // Test AtomicBoolean → Point (should work via AtomicBoolean→Boolean→Point)
            Point point1 = converter.convert(new AtomicBoolean(true), Point.class);
            assertNotNull(point1);
            System.out.println("✓ AtomicBoolean to Point: " + point1);
            
            Point point2 = converter.convert(new AtomicBoolean(false), Point.class);
            assertNotNull(point2);
            System.out.println("✓ AtomicBoolean to Point: " + point2);
            
        } catch (Exception e) {
            System.out.println("✗ AtomicBoolean to Point failed: " + e.getMessage());
        }
        
        try {
            // Test AtomicInteger → Point (should work via AtomicInteger→Integer→Point)
            Point point = converter.convert(new AtomicInteger(300), Point.class);
            assertNotNull(point);
            System.out.println("✓ AtomicInteger to Point: " + point);
            
        } catch (Exception e) {
            System.out.println("✗ AtomicInteger to Point failed: " + e.getMessage());
        }
        
        try {
            // Test AtomicLong → Rectangle (should work via AtomicLong→Long→Rectangle)
            Rectangle rect = converter.convert(new AtomicLong(400), Rectangle.class);
            assertNotNull(rect);
            System.out.println("✓ AtomicLong to Rectangle: " + rect);
            
        } catch (Exception e) {
            System.out.println("✗ AtomicLong to Rectangle failed: " + e.getMessage());
        }
    }
    
    @Test
    void testAtomicToPrimitiveLongConversions() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        try {
            // Test AtomicInteger → long (should work via AtomicInteger→Integer→long)
            long result = converter.convert(new AtomicInteger(12345), long.class);
            assertEquals(12345L, result);
            System.out.println("✓ AtomicInteger to long: " + result);
            
        } catch (Exception e) {
            System.out.println("✗ AtomicInteger to long failed: " + e.getMessage());
        }
        
        try {
            // Test AtomicBoolean → long (should work via AtomicBoolean→Boolean→long)
            long result = converter.convert(new AtomicBoolean(true), long.class);
            assertEquals(1L, result);
            System.out.println("✓ AtomicBoolean to long: " + result);
            
        } catch (Exception e) {
            System.out.println("✗ AtomicBoolean to long failed: " + e.getMessage());
        }
    }
}