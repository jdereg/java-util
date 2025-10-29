package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Test the Map interface implementation of MultiKeyMap.
 */
class MultiKeyMapMapInterfaceTest {
    
    @Test
    void testMapInterfaceBasicOperations() {
        Map<Object, String> map = new MultiKeyMap<>(16);
        
        // Test Map interface methods
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        
        // Test put/get via Map interface
        assertNull(map.put("key1", "value1"));
        assertEquals("value1", map.put("key1", "value1Updated"));
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        
        // Test get
        assertEquals("value1Updated", map.get("key1"));
        assertNull(map.get("nonexistent"));
        
        // Test containsKey/containsValue
        assertTrue(map.containsKey("key1"));
        assertFalse(map.containsKey("nonexistent"));
        assertTrue(map.containsValue("value1Updated"));
        assertFalse(map.containsValue("nonexistent"));
        
        // Test remove
        assertEquals("value1Updated", map.remove("key1"));
        assertNull(map.remove("nonexistent"));
        assertTrue(map.isEmpty());
    }
    
    @Test
    void testMapInterfaceWithArrayKeys() {
        Map<Object, String> map = new MultiKeyMap<>(16);
        
        // Test with Object[] keys via Map interface
        Object[] arrayKey = {"key1", "key2", "key3"};
        map.put(arrayKey, "arrayValue");
        
        assertEquals("arrayValue", map.get(arrayKey));
        assertTrue(map.containsKey(arrayKey));
        assertTrue(map.containsValue("arrayValue"));
        assertEquals(1, map.size());
        
        assertEquals("arrayValue", map.remove(arrayKey));
        assertTrue(map.isEmpty());
    }
    
    @Test
    void testPutMultiKeyAll() {
        MultiKeyMap<String> source = new MultiKeyMap<>();
        source.put("key1", "value1");
        source.put("key2", "value2");
        source.putMultiKey("multiValue", "multi", "key");
        
        Map<Object, String> multiKeyMap = new MultiKeyMap<>(16);
        multiKeyMap.putAll(source);
        
        assertEquals(3, multiKeyMap.size());
        assertEquals("value1", multiKeyMap.get("key1"));
        assertEquals("value2", multiKeyMap.get("key2"));
        assertEquals("multiValue", multiKeyMap.get(Arrays.asList("multi", "key")));
    }
    
    @Test
    void testKeySet() {
        Map<Object, String> map = new MultiKeyMap<>(16);
        
        map.put("singleKey", "value1");
        map.put(new Object[]{"multi", "key"}, "value2");
        
        Set<Object> keys = map.keySet();
        assertEquals(2, keys.size());
        
        // Check that keys contain the expected values
        boolean hasSingleKey = false;
        boolean hasListKey = false;
        List<Object> expectedListKey = Arrays.asList("multi", "key");

        for (Object key : keys) {
            if ("singleKey".equals(key)) {
                hasSingleKey = true;
            } else if (key instanceof List && key.equals(expectedListKey)) {
                // Multi-keys are now exposed as List (ordered) or Set (unordered)
                hasListKey = true;
            }
        }

        assertTrue(hasSingleKey, "Should contain single key");
        assertTrue(hasListKey, "Should contain List key");
    }
    
    @Test
    void testValues() {
        Map<Object, String> map = new MultiKeyMap<>(16);
        
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put(new Object[]{"multi", "key"}, "value3");
        
        Collection<String> values = map.values();
        assertEquals(3, values.size());
        assertTrue(values.contains("value1"));
        assertTrue(values.contains("value2"));
        assertTrue(values.contains("value3"));
    }
    
    @Test
    void testEntrySet() {
        Map<Object, String> map = new MultiKeyMap<>(16);
        
        map.put("singleKey", "value1");
        Object[] arrayKey = {"multi", "key"};
        map.put(arrayKey, "value2");
        
        Set<Map.Entry<Object, String>> entries = map.entrySet();
        assertEquals(2, entries.size());
        
        // Verify entries
        Map<Object, String> entryMap = new HashMap<>();
        for (Map.Entry<Object, String> entry : entries) {
            entryMap.put(entry.getKey(), entry.getValue());
        }
        
        // Check single key entry
        assertTrue(entryMap.containsKey("singleKey"));
        assertEquals("value1", entryMap.get("singleKey"));

        // Check List key entry - multi-keys are now exposed as List (ordered) or Set (unordered)
        String listValue = null;
        List<Object> expectedListKey = Arrays.asList("multi", "key");
        for (Map.Entry<Object, String> entry : entryMap.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof List && key.equals(expectedListKey)) {
                listValue = entry.getValue();
                break;
            }
        }
        assertEquals("value2", listValue);
    }
    
    @Test
    void testClear() {
        Map<Object, String> map = new MultiKeyMap<>(16);
        
        map.put("key1", "value1");
        map.put("key2", "value2");
        assertEquals(2, map.size());
        
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }
    
    @Test
    void testEqualsAndHashCode() {
        Map<Object, String> map1 = new MultiKeyMap<>(16);
        Map<Object, String> map2 = new MultiKeyMap<>(16);
        
        // Empty maps should be equal
        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
        
        // Add same entries to both
        map1.put("key1", "value1");
        map2.put("key1", "value1");
        
        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
        
        // Add different entry to one
        map1.put("key2", "value2");
        assertNotEquals(map1, map2);
        
        // Add same entry to other
        map2.put("key2", "value2");
        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }
    
    @Test
    void testToString() {
        Map<Object, String> map = new MultiKeyMap<>(16);
        
        // Empty map
        assertEquals("{}", map.toString());
        
        // Single entry
        map.put("key1", "value1");
        assertEquals("{\n  🆔 key1 → 🟣 value1\n}", map.toString());
        
        // Clear and test with array key
        map.clear();
        map.put(new Object[]{"multi", "key"}, "arrayValue");
        String result = map.toString();
        assertTrue(result.contains("arrayValue"));
        assertTrue(result.contains("🆔 [multi, key]"));
    }
    
    @Test
    void testMapInterfacePolymorphism() {
        // Test that MultiKeyMap can be used polymorphically as Map
        Map<Object, String> map = createMap();
        
        map.put("test", "value");
        assertEquals("value", map.get("test"));
        assertEquals(1, map.size());
    }
    
    private Map<Object, String> createMap() {
        return new MultiKeyMap<>(16);
    }
    
    @Test
    void testNullHandling() {
        Map<Object, String> map = new MultiKeyMap<>(16);
        
        // Test null key
        map.put(null, "nullKeyValue");
        assertEquals("nullKeyValue", map.get(null));
        assertTrue(map.containsKey(null));
        
        // Test null value
        map.put("nullValueKey", null);
        assertNull(map.get("nullValueKey"));
        assertTrue(map.containsKey("nullValueKey"));
        assertTrue(map.containsValue(null));
        
        assertEquals(2, map.size());
    }
    
    @Test
    void testMultiKeyMapSpecificFeatures() {
        // Test that MultiKeyMap-specific features still work with Map interface
        MultiKeyMap<String> multiMap = new MultiKeyMap<>(16);
        Map<Object, String> map = multiMap; // Polymorphic reference
        
        // Test varargs put (MultiKeyMap specific)
        multiMap.putMultiKey("multiValue", "key1", "key2", "key3");
        
        // Should be accessible via Map interface with Object[] key
        Object[] keys = {"key1", "key2", "key3"};
        assertEquals("multiValue", map.get(keys));
        assertTrue(map.containsKey(keys));
        
        // Test Collection-based access (MultiKeyMap specific)
        List<Object> keyList = Arrays.asList("key1", "key2", "key3");
        assertEquals("multiValue", multiMap.get(keyList));
        assertTrue(multiMap.containsKey(keyList));
    }
}