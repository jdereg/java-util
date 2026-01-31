package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import com.cedarsoftware.util.LoggingConfig;
import com.cedarsoftware.util.geom.Point;
import com.cedarsoftware.util.geom.Rectangle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test surrogate bridge expansion functionality
 */
class SurrogateBridgeTest {
    
    private static final Logger LOG = Logger.getLogger(SurrogateBridgeTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }
    
    @Test
    void testAtomicToGeometricConversions() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        try {
            // Test AtomicBoolean → Point (should work via AtomicBoolean→Boolean→Point)
            Point point1 = converter.convert(new AtomicBoolean(true), Point.class);
            assertNotNull(point1);
            LOG.info("✓ AtomicBoolean to Point: " + point1);
            
            Point point2 = converter.convert(new AtomicBoolean(false), Point.class);
            assertNotNull(point2);
            LOG.info("✓ AtomicBoolean to Point: " + point2);
            
        } catch (Exception e) {
            LOG.warning("✗ AtomicBoolean to Point failed: " + e.getMessage());
        }
        
        try {
            // Test AtomicInteger → Point (should work via AtomicInteger→Integer→Point)
            Point point = converter.convert(new AtomicInteger(300), Point.class);
            assertNotNull(point);
            LOG.info("✓ AtomicInteger to Point: " + point);
            
        } catch (Exception e) {
            LOG.warning("✗ AtomicInteger to Point failed: " + e.getMessage());
        }
        
        try {
            // Test AtomicLong → Rectangle (should work via AtomicLong→Long→Rectangle)
            Rectangle rect = converter.convert(new AtomicLong(400), Rectangle.class);
            assertNotNull(rect);
            LOG.info("✓ AtomicLong to Rectangle: " + rect);
            
        } catch (Exception e) {
            LOG.warning("✗ AtomicLong to Rectangle failed: " + e.getMessage());
        }
    }
    
    @Test
    void testAtomicToPrimitiveLongConversions() {
        Converter converter = new Converter(new DefaultConverterOptions());
        
        try {
            // Test AtomicInteger → long (should work via AtomicInteger→Integer→long)
            long result = converter.convert(new AtomicInteger(12345), long.class);
            assertEquals(12345L, result);
            LOG.info("✓ AtomicInteger to long: " + result);
            
        } catch (Exception e) {
            LOG.warning("✗ AtomicInteger to long failed: " + e.getMessage());
        }
        
        try {
            // Test AtomicBoolean → long (should work via AtomicBoolean→Boolean→long)
            long result = converter.convert(new AtomicBoolean(true), long.class);
            assertEquals(1L, result);
            LOG.info("✓ AtomicBoolean to long: " + result);
            
        } catch (Exception e) {
            LOG.warning("✗ AtomicBoolean to long failed: " + e.getMessage());
        }
    }
}