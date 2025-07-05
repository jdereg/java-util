package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the new N-Key functionality in MultiKeyMap.
 */
class MultiKeyMapNKeyTest {
    
    @Test
    void testVarargsAPI() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test putting with Object[] arrays
        assertNull(map.put(new Object[]{String.class, Integer.class, 1L}, "converter1"));
        assertNull(map.put(new Object[]{String.class, Long.class, 2L}, "converter2"));
        
        // Test getting with varargs
        assertEquals("converter1", map.get(String.class, Integer.class, 1L));
        assertEquals("converter2", map.get(String.class, Long.class, 2L));
        assertNull(map.get(String.class, Double.class, 3L));
        
        assertEquals(2, map.size());
    }
    
    @Test
    void testMapInterfaceAPI() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test single key (Map interface compatible)
        assertNull(map.put("singleKey", "singleValue"));
        assertEquals("singleValue", map.get("singleKey"));
        assertTrue(map.containsKey("singleKey"));
        
        // Test N-Key via Object[]
        Object[] nKey = {String.class, Integer.class, 42L};
        assertNull(map.put(nKey, "nKeyValue"));
        assertEquals("nKeyValue", map.get(nKey));
        assertTrue(map.containsKey(nKey));
        
        assertEquals(2, map.size());
        
        // Test removal
        assertEquals("singleValue", map.remove("singleKey"));
        assertEquals("nKeyValue", map.remove(nKey));
        assertTrue(map.isEmpty());
    }
    
    @Test
    void testLegacyAPICompatibility() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Use legacy API
        map.put(String.class, Integer.class, 1L, "legacyValue");
        assertEquals("legacyValue", map.get(String.class, Integer.class, 1L));
        assertTrue(map.containsKey(String.class, Integer.class, 1L));
        
        // Should also work with new APIs
        assertEquals("legacyValue", map.get(new Object[]{String.class, Integer.class, 1L}));
        assertTrue(map.containsKey(new Object[]{String.class, Integer.class, 1L}));
        
        assertEquals(1, map.size());
        
        // Remove using new API
        assertEquals("legacyValue", map.remove(String.class, Integer.class, 1L));
        assertTrue(map.isEmpty());
    }
    
    @Test
    void testFlexibleKeyDimensions() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // 2-dimensional key  
        map.put(new Object[]{"sessionId", "requestId"}, "2D");
        
        // 3-dimensional key
        map.put(new Object[]{String.class, Integer.class, 1L}, "3D");
        
        // 4-dimensional key
        map.put(new Object[]{"country", "state", "city", "zipCode"}, "4D");
        
        // 5-dimensional key
        map.put(new Object[]{"year", "month", "day", "hour", "minute"}, "5D");
        
        // Verify all work correctly
        assertEquals("2D", map.get("sessionId", "requestId"));
        assertEquals("3D", map.get(String.class, Integer.class, 1L));
        assertEquals("4D", map.get("country", "state", "city", "zipCode"));
        assertEquals("5D", map.get("year", "month", "day", "hour", "minute"));
        
        assertEquals(4, map.size());
    }
    
    @Test
    void testNullKeyHandling() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test null in various key positions
        map.put(new Object[]{null, "key2", "key3"}, "nullFirst");
        map.put(new Object[]{"key1", null, "key3"}, "nullMiddle");
        map.put(new Object[]{"key1", "key2", null}, "nullLast");
        
        assertEquals("nullFirst", map.get(null, "key2", "key3"));
        assertEquals("nullMiddle", map.get("key1", null, "key3"));
        assertEquals("nullLast", map.get("key1", "key2", null));
        
        assertEquals(3, map.size());
    }
    
    @Test
    void testKeyHashingConsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Same logical key should hash consistently
        Object[] key1 = {String.class, Integer.class, 42L};
        Object[] key2 = {String.class, Integer.class, 42L};
        
        map.put(key1, "value1");
        
        // Should find the value using equivalent but different array
        assertEquals("value1", map.get(key2));
        assertTrue(map.containsKey(key2));
        
        // Should update the same entry
        assertEquals("value1", map.put(key2, "value2"));
        assertEquals("value2", map.get(key1));
        assertEquals(1, map.size());
    }
}