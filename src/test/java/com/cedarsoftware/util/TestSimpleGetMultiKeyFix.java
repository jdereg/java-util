package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test the simple fix: getMultiKey() delegates to get()
 */
public class TestSimpleGetMultiKeyFix {
    
    @Test
    void testSimplestInconsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Store a value using get() with a nested array key
        map.put(new Object[]{new String[]{"a"}, "b"}, "value");
        
        // This works - same path as storage
        assertEquals("value", map.get(new Object[]{new String[]{"a"}, "b"}));
        
        // This should now work too!
        assertEquals("value", map.getMultiKey(new String[]{"a"}, "b"));
    }
    
    @Test
    void testRegularMultiKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Regular multi-key usage should still work
        map.putMultiKey("value1", "a", "b", "c");
        assertEquals("value1", map.getMultiKey("a", "b", "c"));
        
        // Single key should work
        map.putMultiKey("value2", "x");
        assertEquals("value2", map.getMultiKey("x"));
        
        // Empty should work
        map.putMultiKey("value3");
        assertEquals("value3", map.getMultiKey());
    }
}