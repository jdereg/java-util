package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify NULL_SENTINEL uniformity - all null values within arrays/collections
 * should be treated consistently with top-level null keys.
 */
public class MultiKeyMapNullUniformityTest {
    
    @Test
    void testTopLevelNullVsSingleElementArrayWithNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Store top-level null
        map.put(null, "top_level_null");
        
        // Store single-element array with null
        map.put(new Object[]{null}, "array_with_null");
        
        // These are DIFFERENT keys - no collapse behavior
        assertEquals(2, map.size(), "Top-level null and single-element array with null should be different keys");
        
        // Each lookup returns its own value
        assertEquals("top_level_null", map.get(null));
        assertEquals("array_with_null", map.get(new Object[]{null}));
        
        // Both keys exist independently
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey(new Object[]{null}));
    }
    
    @Test
    void testTopLevelNullVsSingleElementCollectionWithNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Store top-level null
        map.put(null, "top_level_null");
        
        // Store single-element collection with null
        List<Object> listWithNull = Arrays.asList((Object) null);
        map.put(listWithNull, "collection_with_null");
        
        // These are DIFFERENT keys - no collapse behavior
        assertEquals(2, map.size(), "Top-level null and single-element collection with null should be different keys");
        
        // Each lookup returns its own value
        assertEquals("top_level_null", map.get(null));
        assertEquals("collection_with_null", map.get(listWithNull));
        
        // Both keys exist independently
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey(listWithNull));
    }
    
    @Test
    void testNullEquivalenceAcrossContainerTypes() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Different ordered container types, all with single null element
        Object[] objectArray = {null};
        String[] stringArray = {null};
        List<Object> list = Arrays.asList((Object) null);

        // Store using one type
        map.put(objectArray, "stored_value");

        // All ordered containers (Arrays/Lists) with same content are equivalent
        assertEquals(1, map.size());
        assertEquals("stored_value", map.get(objectArray));
        assertEquals("stored_value", map.get(stringArray));
        assertEquals("stored_value", map.get(list));

        // All should be recognized as containing the key
        assertTrue(map.containsKey(objectArray));
        assertTrue(map.containsKey(stringArray));
        assertTrue(map.containsKey(list));

        // Sets are semantically distinct - they don't match Arrays/Lists
        Set<Object> set = new HashSet<>(Arrays.asList((Object) null));
        assertNull(map.get(set));
        assertFalse(map.containsKey(set));
    }
    
    @Test
    void testMultiElementArraysWithNulls() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Arrays with same null pattern should be equivalent
        Object[] objectArray = {null, "a", null};
        String[] stringArray = {null, "a", null};
        List<Object> list = Arrays.asList(null, "a", null);
        
        map.put(objectArray, "multi_null_value");
        
        // Should be equivalent due to NULL_SENTINEL uniformity
        assertEquals("multi_null_value", map.get(stringArray));
        assertEquals("multi_null_value", map.get(list));
        
        assertEquals(1, map.size(), "All containers with same null pattern should be equivalent");
    }
    
    @Test
    void testNullOnlyArraysAreEquivalent() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Arrays containing only nulls
        Object[] objectNulls = {null, null, null};
        String[] stringNulls = {null, null, null};
        List<Object> listNulls = Arrays.asList(null, null, null);
        
        map.put(objectNulls, "all_nulls");
        
        // Should all be equivalent
        assertEquals("all_nulls", map.get(stringNulls));
        assertEquals("all_nulls", map.get(listNulls));
        
        assertEquals(1, map.size(), "All containers with only nulls should be equivalent");
    }
    
    @Test
    void testNestedArraysWithNulls() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Nested arrays containing nulls
        Object[][] nested1 = {{null, "a"}, {null}};
        Object[][] nested2 = {{null, "a"}, {null}};
        
        map.put(nested1, "nested_with_nulls");
        
        // Should be equivalent
        assertEquals("nested_with_nulls", map.get(nested2));
        assertEquals(1, map.size());
    }
    
    @Test
    void testNullVsEmptyDistinction() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // These should be DIFFERENT keys: null vs empty containers
        map.put(null, "null_key");
        map.put(new Object[0], "empty_array");
        map.put(new ArrayList<>(), "empty_collection");  // This overwrites empty_array since they're equivalent
        
        // null key should remain distinct from empty containers
        assertEquals("null_key", map.get(null));
        assertEquals("empty_collection", map.get(new Object[0]));  // empty array and collection are equivalent
        assertEquals("empty_collection", map.get(new ArrayList<>()));
        
        // Should have 2 different keys: null vs empty containers (empty array/collection are same)
        assertEquals(2, map.size(), "null should be distinct from empty containers, but empty array/collection are equivalent");
    }
    
    @Test
    void testHashConsistencyWithNulls() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test that hash computation is consistent for equivalent null patterns
        Object[] key1 = {null, "test", null};
        String[] key2 = {null, "test", null};
        List<Object> key3 = Arrays.asList(null, "test", null);
        
        // Store and retrieve multiple times to test hash consistency
        for (int i = 0; i < 100; i++) {
            map.clear();
            map.put(key1, "value" + i);
            
            // All equivalent keys should retrieve the same value
            assertEquals("value" + i, map.get(key2));
            assertEquals("value" + i, map.get(key3));
            assertEquals(1, map.size());
        }
    }
    
    @Test
    void testNullInBusinessObjectArray() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test with business object arrays containing nulls
        String[] businessObjects1 = {"obj1", null, "obj3"};
        String[] businessObjects2 = {"obj1", null, "obj3"};
        Object[] mixedArray = {"obj1", null, "obj3"};
        
        map.put(businessObjects1, "business_value");
        
        // Should be equivalent
        assertEquals("business_value", map.get(businessObjects2));
        assertEquals("business_value", map.get(mixedArray));
        assertEquals(1, map.size());
    }
    
    @Test
    void testNullSentinelNotExposedToUser() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Store arrays with nulls
        Object[] arrayWithNull = {null, "data"};
        map.put(arrayWithNull, "test_value");
        
        // Verify that user never sees NULL_SENTINEL in public APIs
        Set<Object> keySet = map.keySet();
        assertEquals(1, keySet.size());
        
        Object retrievedKey = keySet.iterator().next();

        // The key should be exposed as a List for proper serialization
        assertTrue(retrievedKey instanceof List, "Multi-keys should be exposed as Lists");
        List<?> retrievedList = (List<?>) retrievedKey;

        // The list should contain actual null, not NULL_SENTINEL
        assertNull(retrievedList.get(0), "User should see actual null, not NULL_SENTINEL");
        assertEquals("data", retrievedList.get(1));
    }
}