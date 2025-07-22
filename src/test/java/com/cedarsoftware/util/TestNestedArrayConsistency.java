package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that get() and getMultiKey() handle nested arrays consistently.
 */
public class TestNestedArrayConsistency {
    
    @Test
    void testNestedArrayConsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put using nested array via get()
        map.put(new Object[]{new String[]{"a"}, "b"}, "nestedValue");
        
        // Should be retrievable both ways
        assertEquals("nestedValue", map.get(new Object[]{new String[]{"a"}, "b"}));
        assertEquals("nestedValue", map.getMultiKey(new String[]{"a"}, "b"));
        
        // Put using getMultiKey syntax  
        map.putMultiKey("multiValue", new String[]{"x"}, "y");
        
        // Should be retrievable both ways
        assertEquals("multiValue", map.get(new Object[]{new String[]{"x"}, "y"}));  
        assertEquals("multiValue", map.getMultiKey(new String[]{"x"}, "y"));
    }
}