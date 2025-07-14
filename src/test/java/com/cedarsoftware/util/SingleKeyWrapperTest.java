package com.cedarsoftware.util;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the "coconut wrapper" mechanism for Map interface single-key compliance.
 */
class SingleKeyWrapperTest {
    
    @Test
    void testSingleKeyMapInterfaceCompliance() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test single key operations via Map interface
        assertNull(map.put("singleKey", "singleValue"));
        assertEquals("singleValue", map.get("singleKey"));
        assertTrue(map.containsKey("singleKey"));
        assertEquals(1, map.size());
        
        // Update single key
        assertEquals("singleValue", map.put("singleKey", "updatedValue"));
        assertEquals("updatedValue", map.get("singleKey"));
        assertEquals(1, map.size());
        
        // Remove single key
        assertEquals("updatedValue", map.remove("singleKey"));
        assertNull(map.get("singleKey"));
        assertFalse(map.containsKey("singleKey"));
        assertTrue(map.isEmpty());
    }
    
    @Test
    void testMixedSingleAndMultiKeyOperations() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Add single key via Map interface
        map.put("single", "singleValue");
        
        // Add multi-key via varargs
        map.putMultiKey("tripleValue", "key1", "key2", "key3");
        
        // Add multi-key via Object[] array
        map.put(new Object[]{"array1", "array2"}, "arrayValue");
        
        // Verify all can be retrieved correctly
        assertEquals("singleValue", map.get("single"));
        assertEquals("tripleValue", map.getMultiKey("key1", "key2", "key3"));
        assertEquals("arrayValue", map.getMultiKey(new Object[]{"array1", "array2"}));
        
        assertEquals(3, map.size());
        
        // Verify containsKey works for all types
        assertTrue(map.containsKey("single"));
        assertTrue(map.containsMultiKey("key1", "key2", "key3"));
        assertTrue(map.containsMultiKey(new Object[]{"array1", "array2"}));
        
        // Verify remove works for all types
        assertEquals("singleValue", map.remove("single"));
        assertEquals("tripleValue", map.removeMultiKey("key1", "key2", "key3"));
        assertEquals("arrayValue", map.removeMultiKey(new Object[]{"array1", "array2"}));
        
        assertTrue(map.isEmpty());
    }
    
    @Test 
    void testCoconutWrapperIsolation() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // The same logical key should work whether accessed as single key or wrapped in Object[]
        String testKey = "isolationKey";
        
        // Store via single key Map interface
        map.put(testKey, "mapValue");
        
        // Should NOT be retrievable via Object[] because coconut wrapper creates different key
        assertNull(map.getMultiKey(new Object[]{testKey}));
        
        // But should be retrievable via single key Map interface
        assertEquals("mapValue", map.get(testKey));
        
        // Now store via Object[] 
        map.put(new Object[]{testKey}, "arrayValue");
        
        // Now we should have two separate entries
        assertEquals("mapValue", map.get(testKey));  // Single key (coconut wrapped)
        assertEquals("arrayValue", map.getMultiKey(new Object[]{testKey}));  // Array key (direct)
        
        assertEquals(2, map.size());
    }
    
    @Test
    void testNullKeySupport() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test null single key
        map.put(null, "nullValue");
        assertEquals("nullValue", map.get((Object) null));
        assertTrue(map.containsKey((Object) null));
        
        // Test null in array
        map.put(new Object[]{null, "second"}, "arrayWithNull");
        assertEquals("arrayWithNull", map.getMultiKey(new Object[]{null, "second"}));
        
        assertEquals(2, map.size());
        
        // Remove null entries
        assertEquals("nullValue", map.remove((Object) null));
        assertEquals("arrayWithNull", map.removeMultiKey(new Object[]{null, "second"}));
        
        assertTrue(map.isEmpty());
    }
}