package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the ultra-fast path optimization in MultiKeyMap
 * correctly handles null keys and null values without getting tricked.
 */
public class MultiKeyMapNullHandlingTest {
    
    @Test
    public void testNullKeyHandling() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test null key with non-null value
        map.put(null, "null-key-value");
        assertEquals("null-key-value", map.get(null));
        assertTrue(map.containsKey(null));
        
        // Test null key with null value
        map.put(null, null);
        assertNull(map.get(null));
        assertTrue(map.containsKey(null));  // Key exists even with null value
        
        // Remove null key
        map.remove(null);
        assertNull(map.get(null));
        assertFalse(map.containsKey(null));
    }
    
    @Test
    public void testNullValueHandling() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test non-null key with null value
        map.put("key1", null);
        assertNull(map.get("key1"));
        assertTrue(map.containsKey("key1"));  // Key exists even with null value
        
        // Test that we can distinguish between no key and null value
        assertFalse(map.containsKey("nonexistent"));
        assertNull(map.get("nonexistent"));  // Also returns null but key doesn't exist
        
        // Update null value to non-null
        map.put("key1", "updated");
        assertEquals("updated", map.get("key1"));
        assertTrue(map.containsKey("key1"));
    }
    
    @Test
    public void testMixedNullScenarios() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Add multiple entries including nulls
        map.put("key1", "value1");
        map.put("key2", null);
        map.put(null, "null-key-value");
        map.put("key3", "value3");
        map.put(null, null);  // Overwrite null key with null value
        
        // Verify all lookups work correctly
        assertEquals("value1", map.get("key1"));
        assertNull(map.get("key2"));
        assertNull(map.get(null));
        assertEquals("value3", map.get("key3"));
        
        // Verify containsKey works for all
        assertTrue(map.containsKey("key1"));
        assertTrue(map.containsKey("key2"));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("key3"));
        assertFalse(map.containsKey("nonexistent"));
        
        // Verify size
        assertEquals(4, map.size());
    }
    
    @Test
    public void testNullInArrayKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test array key containing null
        Object[] keyWithNull = new Object[]{"first", null, "third"};
        map.put(keyWithNull, "array-with-null");
        assertEquals("array-with-null", map.get(keyWithNull));
        assertTrue(map.containsKey(keyWithNull));
        
        // Test array key that is entirely nulls
        Object[] allNulls = new Object[]{null, null, null};
        map.put(allNulls, "all-nulls");
        assertEquals("all-nulls", map.get(allNulls));
        assertTrue(map.containsKey(allNulls));
    }
    
    @Test
    public void testCaseInsensitiveWithNull() {
        // Test case-insensitive mode with null handling
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .build();
        
        map.put(null, "null-value");
        map.put("KEY", "key-value");
        
        // Null key should work
        assertEquals("null-value", map.get(null));
        assertTrue(map.containsKey(null));
        
        // Case-insensitive lookup should work
        assertEquals("key-value", map.get("key"));
        assertEquals("key-value", map.get("KEY"));
        assertEquals("key-value", map.get("Key"));
        
        // Null is distinct from any string
        assertNotEquals(map.get(null), map.get("null"));
    }
}