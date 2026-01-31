package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify single-element container behavior.
 * Key principles:
 * 1. No collapse: [x] never becomes x
 * 2. Container type doesn't matter: {null} == Arrays.asList(null) == LinkedList with null
 * 3. Direct values are different from containers: null != [null]
 */
public class MultiKeyMapNullSentinelSingleElementTest {
    
    @Test
    void testSingleElementArrayWithNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Single-element array containing null
        Object[] singleNullArray = {null};
        map.put(singleNullArray, "null_array_value");
        
        // [null] and null are DIFFERENT - no collapse
        assertNull(map.get((Object) null));
        assertFalse(map.containsKey((Object) null));
        
        // Getting with the array should work
        assertEquals("null_array_value", map.get(singleNullArray));
        assertTrue(map.containsKey(singleNullArray));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testSingleElementCollectionWithNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Single-element List containing null
        List<Object> singleNullList = new ArrayList<>();
        singleNullList.add(null);
        map.put(singleNullList, "null_list_value");
        
        // Direct null is different from [null]
        assertNull(map.get((Object) null));
        assertFalse(map.containsKey((Object) null));
        
        // Getting with the list should work
        assertEquals("null_list_value", map.get(singleNullList));
        assertTrue(map.containsKey(singleNullList));
        
        // Array with same content should find the same entry (container type doesn't matter)
        assertEquals("null_list_value", map.get(new Object[]{null}));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testSingleElementSetWithNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Single-element Set containing null
        Set<Object> singleNullSet = new HashSet<>();
        singleNullSet.add(null);
        map.put(singleNullSet, "null_set_value");
        
        // Direct null is different
        assertNull(map.get((Object) null));
        assertFalse(map.containsKey((Object) null));
        
        // Getting with the set should work
        assertEquals("null_set_value", map.get(singleNullSet));
        assertTrue(map.containsKey(singleNullSet));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testMixedSingleElementNullContainers() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Different single-element ordered containers with null - all map to same key
        Object[] nullArray = {null};
        List<Object> nullList = Arrays.asList((Object) null);  // Non-RandomAccess, becomes array
        Vector<Object> nullVector = new Vector<>();  // RandomAccess
        nullVector.add(null);

        map.put(nullArray, "first_null");
        map.put(nullList, "second_null");    // Same key - OVERWRITES
        map.put(nullVector, "third_null");  // Same key - OVERWRITES

        // All Lists/Arrays resolve to same key (same content)
        assertNull(map.get((Object) null)); // Direct null is different
        assertEquals("third_null", map.get(nullArray));
        assertEquals("third_null", map.get(nullList));
        assertEquals("third_null", map.get(nullVector));

        // Sets are semantically distinct - they don't match Lists/Arrays
        Set<Object> nullSet = new HashSet<>();
        nullSet.add(null);
        map.put(nullSet, "set_null");  // Different key - doesn't overwrite

        assertEquals("set_null", map.get(nullSet));
        assertEquals("third_null", map.get(nullArray));  // Still has List/Array value

        // Should have 2 entries (List/Array key + Set key)
        assertEquals(2, map.size());
    }
    
    @Test
    void testSingleElementNullVsDirectNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put direct null
        map.put(null, "direct_null");
        
        // Put single-element array with null - different key
        Object[] singleNull = {null};
        map.put(singleNull, "array_null");
        
        // Each is a different key
        assertEquals("direct_null", map.get((Object) null));
        assertEquals("array_null", map.get(singleNull));
        
        // Should have 2 entries
        assertEquals(2, map.size());
    }
    
    @Test
    void testSingleElementNullHashConsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put different single-element null containers
        Object[] nullArray = {null};
        List<Object> nullList = Collections.singletonList(null); // Non-RandomAccess
        
        map.put(nullArray, "value1");
        map.put(nullList, "value2");  // Same content - OVERWRITES
        
        // They map to the same key (content-based equality)
        assertEquals("value2", map.get(nullArray));
        assertEquals("value2", map.get(nullList));
        assertNull(map.get((Object) null)); // Direct null is different
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testNullSentinelSpecificCase() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create an array with a single null element
        Object[] singleNullArray = new Object[1];
        singleNullArray[0] = null;
        
        // Put this array as a key
        map.put(singleNullArray, "sentinel_test_value");
        
        // Direct null is different
        assertNull(map.get((Object) null));
        assertEquals("sentinel_test_value", map.get(singleNullArray));
        
        // Another array with same content should find it
        Object[] anotherNullArray = {null};
        assertEquals("sentinel_test_value", map.get(anotherNullArray));
        
        // List with same content should also find it (content-based)
        List<Object> singleNullList = new ArrayList<>();
        singleNullList.add(null);
        map.put(singleNullList, "list_value");  // OVERWRITES
        
        // All containers with [null] map to same key
        assertNull(map.get((Object) null)); // Direct null still different
        assertEquals("list_value", map.get(singleNullArray));
        assertEquals("list_value", map.get(anotherNullArray));
        assertEquals("list_value", map.get(singleNullList));
        
        // Should have only 1 entry
        assertEquals(1, map.size());
        
        // Test containsKey
        assertFalse(map.containsKey((Object) null));
        assertTrue(map.containsKey(singleNullArray));
        assertTrue(map.containsKey(anotherNullArray));
        assertTrue(map.containsKey(singleNullList));
    }
    
    @Test
    void testSingleElementNullWithMultipleEntriesInMap() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Add various entries
        map.put("regular_key", "regular_value");
        map.put(Arrays.asList("multi", "key"), "multi_value");
        
        // Add single-element null array
        Object[] singleNull = {null};
        map.put(singleNull, "null_value");
        
        // Add direct null (different key)
        map.put((Object) null, "direct_null");
        
        // Verify all entries are accessible
        assertEquals("regular_value", map.get("regular_key"));
        assertEquals("multi_value", map.get(Arrays.asList("multi", "key")));
        assertEquals("null_value", map.get(singleNull));
        assertEquals("direct_null", map.get((Object) null));
        
        assertEquals(4, map.size());
    }
    
    @Test
    void testRemoveSingleElementNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put single-element null array
        Object[] singleNull = {null};
        map.put(singleNull, "null_value");
        
        assertEquals(1, map.size());
        assertNull(map.get((Object) null)); // Direct null not stored
        assertEquals("null_value", map.get(singleNull));
        
        // Remove via array key
        String removed = map.remove(singleNull);
        assertEquals("null_value", removed);
        assertEquals(0, map.size());
        
        // Should not be accessible anymore
        assertNull(map.get((Object) null));
        assertNull(map.get(singleNull));
        assertFalse(map.containsKey((Object) null));
        assertFalse(map.containsKey(singleNull));
    }
    
    @Test
    void testNullSentinelOptimizationWithFlattenTrue() {
        // Create a MultiKeyMap with flattenDimensions = true
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(true).build();
        
        // Single-element array containing null
        Object[] singleNullArray = {null};
        map.put(singleNullArray, "flattened_null_value");
        
        // Direct null should be different - arrays don't collapse
        assertNull(map.get((Object) null));
        assertEquals("flattened_null_value", map.get(singleNullArray));
        
        // List with same content - with flatten=true, lists go through expandWithHash
        // while arrays don't, so they end up as different keys
        List<Object> singleNullList = Arrays.asList((Object) null);
        map.put(singleNullList, "flattened_null_list");  // Different key with flatten=true
        
        // Direct null is still different from [null]
        assertNull(map.get((Object) null));
        assertEquals("flattened_null_value", map.get(singleNullArray)); // Array keeps its value
        assertEquals("flattened_null_list", map.get(singleNullList)); // List has its own value with flatten=true
        
        assertEquals(2, map.size()); // Two entries with flatten=true: array and expanded list
    }
    
    @Test  
    void testNullSentinelOptimizationWithFlattenFalse() {
        // Create a MultiKeyMap with flattenDimensions = false
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(false).build();
        
        // Single-element array containing null
        Object[] singleNullArray = {null};
        map.put(singleNullArray, "structured_null_value");
        
        // Direct null is different
        assertNull(map.get((Object) null));
        assertEquals("structured_null_value", map.get(singleNullArray));
        
        assertEquals(1, map.size());
    }
}