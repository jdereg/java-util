package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test the getMultiKey optimization for flat arrays.
 */
public class TestGetMultiKeyOptimization {
    
    @Test
    void testFlatArrayOptimization() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put with multi-key
        map.putMultiKey("value1", "a", "b", "c");
        
        // Get with getMultiKey - should use fast path (no expansion)
        assertEquals("value1", map.getMultiKey("a", "b", "c"));
        
        // Also works with regular get
        assertEquals("value1", map.get(new String[]{"a", "b", "c"}));
    }
    
    @Test
    void testNestedArrayExpansion() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put with nested array - this SHOULD expand to ["a", "b", "c", "d"]
        map.putMultiKey("value2", "a", new String[]{"b", "c"}, "d");
        
        // Should also work with the nested form (this uses the fast path since it needs expansion)
        assertEquals("value2", map.getMultiKey("a", new String[]{"b", "c"}, "d"));
        
        // And the expanded form should work when stored that way
        map.putMultiKey("value3", "a", "b", "c", "d");
        assertEquals("value3", map.getMultiKey("a", "b", "c", "d"));
    }
    
    @Test
    void testSingleKeyOptimization() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Single key
        map.put("single", "value3");
        
        // getMultiKey with single key should work
        assertEquals("value3", map.getMultiKey("single"));
        
        // Also test null
        map.put(null, "nullValue");
        assertEquals("nullValue", map.getMultiKey((Object[]) null));
    }
    
    @Test
    void testCollectionInKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put with a collection in the keys - this needs expansion
        map.putMultiKey("value4", "a", java.util.Arrays.asList("b", "c"), "d");
        
        // Should work with the same form
        assertEquals("value4", map.getMultiKey("a", java.util.Arrays.asList("b", "c"), "d"));
    }
}