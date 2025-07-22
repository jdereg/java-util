package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the optimization for getMultiKey, putMultiKey, containsMultiKey, and removeMultiKey 
 * to avoid heap allocations for flat arrays.
 */
public class TestMultiKeyOptimization {
    
    @Test
    void testGetMultiKeyOptimization() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test flat array (fast path)
        map.putMultiKey("value1", "a", "b", "c");
        assertEquals("value1", map.getMultiKey("a", "b", "c"));
        
        // Test nested array (expansion path)
        map.putMultiKey("value2", "x", new String[]{"y", "z"});
        assertEquals("value2", map.getMultiKey("x", new String[]{"y", "z"}));
    }
    
    @Test
    void testContainsMultiKeyOptimization() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test flat array (fast path)
        map.putMultiKey("value1", "a", "b", "c");
        assertTrue(map.containsMultiKey("a", "b", "c"));
        assertFalse(map.containsMultiKey("a", "b", "d"));
        
        // Test nested array (expansion path)
        map.putMultiKey("value2", "x", new String[]{"y", "z"});
        assertTrue(map.containsMultiKey("x", new String[]{"y", "z"}));
        assertFalse(map.containsMultiKey("x", new String[]{"y", "w"}));
    }
    
    @Test
    void testRemoveMultiKeyOptimization() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test flat array (fast path)
        map.putMultiKey("value1", "a", "b", "c");
        assertTrue(map.containsMultiKey("a", "b", "c"));
        assertEquals("value1", map.removeMultiKey("a", "b", "c"));
        assertFalse(map.containsMultiKey("a", "b", "c"));
        
        // Test nested array (expansion path)
        map.putMultiKey("value2", "x", new String[]{"y", "z"});
        assertTrue(map.containsMultiKey("x", new String[]{"y", "z"}));
        assertEquals("value2", map.removeMultiKey("x", new String[]{"y", "z"}));
        assertFalse(map.containsMultiKey("x", new String[]{"y", "z"}));
    }
    
    @Test
    void testPutMultiKeyOptimization() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test flat array (fast path) - this should avoid expandKeySequence
        String oldValue1 = map.putMultiKey("value1", "a", "b", "c");
        assertNull(oldValue1);
        assertEquals("value1", map.getMultiKey("a", "b", "c"));
        
        // Test replacing value
        String oldValue2 = map.putMultiKey("newValue1", "a", "b", "c");
        assertEquals("value1", oldValue2);
        assertEquals("newValue1", map.getMultiKey("a", "b", "c"));
        
        // Test nested array (expansion path)
        String oldValue3 = map.putMultiKey("value2", "x", new String[]{"y", "z"});
        assertNull(oldValue3);
        assertEquals("value2", map.getMultiKey("x", new String[]{"y", "z"}));
    }
    
    @Test
    void testNullAndEmptyKeyHandling() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test null keys
        map.put(null, "nullValue");
        assertTrue(map.containsMultiKey((Object[]) null));
        assertEquals("nullValue", map.getMultiKey((Object[]) null));
        assertEquals("nullValue", map.removeMultiKey((Object[]) null));
        assertFalse(map.containsMultiKey((Object[]) null));
        
        // Test empty array keys - should behave same as null
        map.put(null, "emptyValue");
        assertTrue(map.containsMultiKey(new Object[]{}));
        assertEquals("emptyValue", map.getMultiKey(new Object[]{}));
        assertEquals("emptyValue", map.removeMultiKey(new Object[]{}));
        assertFalse(map.containsMultiKey(new Object[]{}));
    }
}