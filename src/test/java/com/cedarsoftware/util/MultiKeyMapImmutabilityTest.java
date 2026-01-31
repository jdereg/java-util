package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that MultiKeyMap properly prevents mutation of internal state through
 * external access to keys, ensures proper equals/hashCode with List representations,
 * and prevents corruption through defensive copying.
 */
class MultiKeyMapImmutabilityTest {
    
    @Test
    void testKeySetReturnImmutableListsForMultiKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.putMultiKey("value1", "key1", "key2");
        map.putMultiKey("value2", "key3", "key4", "key5");
        
        Set<Object> keySet = map.keySet();
        
        for (Object key : keySet) {
            if (key instanceof List) {
                List<?> listKey = (List<?>) key;
                // Should be unmodifiable
                assertThrows(UnsupportedOperationException.class, () -> {
                    ((List<Object>) listKey).set(0, "modified");
                }, "List keys should be unmodifiable");
                
                assertThrows(UnsupportedOperationException.class, () -> {
                    ((List<Object>) listKey).add("extra");
                }, "List keys should not allow additions");
                
                assertThrows(UnsupportedOperationException.class, () -> {
                    ((List<Object>) listKey).remove(0);
                }, "List keys should not allow removals");
            }
        }
    }
    
    @Test
    void testEntrySetReturnImmutableListsForMultiKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.putMultiKey("value1", "key1", "key2");
        map.putMultiKey("value2", "key3", "key4", "key5");
        
        Set<Map.Entry<Object, String>> entrySet = map.entrySet();
        
        for (Map.Entry<Object, String> entry : entrySet) {
            Object key = entry.getKey();
            if (key instanceof List) {
                List<?> listKey = (List<?>) key;
                // Should be unmodifiable
                assertThrows(UnsupportedOperationException.class, () -> {
                    ((List<Object>) listKey).set(0, "modified");
                }, "List keys in entries should be unmodifiable");
            }
        }
    }
    
    @Test
    void testMultiKeyEntryNoDefensiveCopy() {
        // MultiKeyMap does NOT make defensive copies for maximum performance
        // Users must not modify arrays after putting them in the map
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Object[] originalKeys = {"key1", "key2", "key3"};
        map.put(originalKeys, "value");
        
        // The map references the original array directly (no defensive copy)
        assertEquals("value", map.get(originalKeys), "Should find value with original array");
        assertEquals("value", map.get(new Object[]{"key1", "key2", "key3"}), 
                    "Should find value with equivalent array");
        
        // WARNING: Modifying the original array after put will corrupt the map
        // This is documented behavior - users must not modify arrays after putting them
        // For defensive copying, users should use a separate utility class
        
        // Note: entries() exposes internal arrays for performance
        // Users should NOT modify these arrays - doing so would corrupt the map
        // This is by design for zero-allocation performance
    }
    
    @Test
    void testEqualsWithListRepresentation() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>(16);
        map1.putMultiKey("value1", "key1", "key2");
        map1.putMultiKey("value2", "key3");
        
        // Create a HashMap with the same logical content using Lists
        Map<Object, String> map2 = new HashMap<>();
        map2.put(Arrays.asList("key1", "key2"), "value1");
        map2.put("key3", "value2");
        
        // They should be equal
        assertEquals(map1, map2, "MultiKeyMap should equal HashMap with List keys");
        assertEquals(map2, map1, "Equality should be symmetric");
    }
    
    @Test
    void testHashCodeConsistencyWithListRepresentation() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>(16);
        map1.putMultiKey("value1", "key1", "key2");
        map1.putMultiKey("value2", "key3");
        
        // Create a HashMap with the same logical content using Lists
        Map<Object, String> map2 = new HashMap<>();
        map2.put(Arrays.asList("key1", "key2"), "value1");
        map2.put("key3", "value2");
        
        // If they're equal, they must have the same hashCode
        assertEquals(map1.hashCode(), map2.hashCode(), 
                    "Equal maps must have equal hash codes");
    }
    
    @Test
    void testEqualsWithNullValues() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>(16);
        map1.putMultiKey(null, "key1", "key2");
        map1.putMultiKey("value", "key3");
        
        Map<Object, String> map2 = new HashMap<>();
        map2.put(Arrays.asList("key1", "key2"), null);
        map2.put("key3", "value");
        
        assertEquals(map1, map2, "Maps with null values should be equal");
        assertEquals(map1.hashCode(), map2.hashCode(), 
                    "Maps with null values should have equal hash codes");
    }
    
    @Test
    void testKeySetDoesNotExposeInternalArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        Object[] keys = {"key1", "key2"};
        map.put(keys, "value");

        Set<Object> keySet = map.keySet();
        Object retrievedKey = keySet.iterator().next();

        // Should be a List, not the raw internal array
        assertTrue(retrievedKey instanceof List, "Multi-keys should be exposed as Lists");
        List<?> listKey = (List<?>) retrievedKey;

        // Verify content is correct
        assertEquals(2, listKey.size(), "Should have 2 elements");
        assertEquals("key1", listKey.get(0));
        assertEquals("key2", listKey.get(1));
    }
    
    @Test
    void testEntrySetConsistencyWithOtherMaps() {
        MultiKeyMap<Integer> map = new MultiKeyMap<>(16);
        map.putMultiKey(100, "a", "b");
        map.putMultiKey(200, "c");

        // Convert to regular HashMap through entrySet
        Map<Object, Integer> regularMap = new HashMap<>();
        for (Map.Entry<Object, Integer> entry : map.entrySet()) {
            regularMap.put(entry.getKey(), entry.getValue());
        }

        // Note: Maps won't be equal because MultiKeyMap uses internal representation
        // while regularMap uses List keys from entrySet()
        assertEquals(2, regularMap.size(), "Should have same size");

        // Should be able to look up using List keys (entrySet returns Lists for multi-keys)
        boolean foundMultiKey = false;
        List<Object> expectedKey = Arrays.asList("a", "b");
        for (Object key : regularMap.keySet()) {
            if (key instanceof List && key.equals(expectedKey)) {
                assertEquals(100, regularMap.get(key), "Should find value with List key");
                foundMultiKey = true;
            }
        }
        assertTrue(foundMultiKey, "Should have found multi-key entry");
        assertEquals(200, regularMap.get("c"),
                    "Should find value with single key");
    }
    
    @Test
    void testHashCodeStability() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.putMultiKey("value1", "key1", "key2", "key3");
        map.putMultiKey("value2", "key4");
        
        int hash1 = map.hashCode();
        int hash2 = map.hashCode();
        
        assertEquals(hash1, hash2, "HashCode should be stable across calls");
        
        // Create another map with same content
        MultiKeyMap<String> map2 = new MultiKeyMap<>(16);
        map2.putMultiKey("value1", "key1", "key2", "key3");
        map2.putMultiKey("value2", "key4");
        
        assertEquals(map.hashCode(), map2.hashCode(), 
                    "Maps with same content should have same hashCode");
    }
}