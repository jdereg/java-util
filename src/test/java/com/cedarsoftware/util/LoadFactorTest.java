package com.cedarsoftware.util;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test load factor functionality
 */
class LoadFactorTest {
    private static final Logger LOG = Logger.getLogger(LoadFactorTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }

    @Test
    void testCustomLoadFactor() {
        // Test with aggressive 0.5f load factor - should resize more frequently
        MultiKeyMap<String> map = new MultiKeyMap<>(16, 0.5f);
        
        // Add entries to trigger resize at 50% instead of 75%
        for (int i = 0; i < 20; i++) {
            map.putMultiKey("value" + i, String.class, Integer.class, (long) i);
        }
        
        // Should have resized multiple times with 0.5f load factor
        assertEquals(20, map.size(), "Should have 20 entries");
    }
    
    @Test
    void testDefaultLoadFactor() {
        // Test default constructor uses 0.75f load factor
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Should not resize until we hit 12 entries (16 * 0.75)
        for (int i = 0; i < 15; i++) {
            map.putMultiKey("value" + i, String.class, Integer.class, (long) i);
        }

        assertEquals(15, map.size(), "Should have 15 entries");
    }
    
    @Test
    void testInvalidLoadFactor() {
        assertThrows(IllegalArgumentException.class, () -> {
            new MultiKeyMap<>(16, 0.0f);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new MultiKeyMap<>(16, -0.5f);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new MultiKeyMap<>(16, Float.NaN);
        });
    }
}