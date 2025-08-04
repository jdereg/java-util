package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify NULL_SENTINEL single-element optimization in expandWithHash method.
 * This test ensures that single-element arrays/collections containing null are properly
 * handled with the correct hash value and normalization.
 */
public class MultiKeyMapNullSentinelSingleElementTest {
    
    @Test
    void testSingleElementArrayWithNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Single-element array containing null
        Object[] singleNullArray = {null};
        map.put(singleNullArray, "null_array_value");
        
        // This should be normalized to NULL_SENTINEL and have hash 0
        // The key should be accessible as null due to single-element optimization
        assertEquals("null_array_value", map.get((Object) null));
        assertTrue(map.containsKey((Object) null));
        
        // Also verify direct access with the array
        assertEquals("null_array_value", map.get(singleNullArray));
        assertTrue(map.containsKey(singleNullArray));
        
        // Should have only 1 entry (null normalization)
        assertEquals(1, map.size());
    }
    
    @Test
    void testSingleElementCollectionWithNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Single-element List containing null
        List<Object> singleNullList = new ArrayList<>();
        singleNullList.add(null);
        map.put(singleNullList, "null_list_value");
        
        // This should be normalized to NULL_SENTINEL and have hash 0
        assertEquals("null_list_value", map.get((Object) null));
        assertTrue(map.containsKey((Object) null));
        
        // Also verify direct access with the list
        assertEquals("null_list_value", map.get(singleNullList));
        assertTrue(map.containsKey(singleNullList));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testSingleElementSetWithNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Single-element Set containing null
        Set<Object> singleNullSet = new HashSet<>();
        singleNullSet.add(null);
        map.put(singleNullSet, "null_set_value");
        
        // This should be normalized to NULL_SENTINEL and have hash 0
        assertEquals("null_set_value", map.get((Object) null));
        assertTrue(map.containsKey((Object) null));
        
        // Also verify direct access with the set
        assertEquals("null_set_value", map.get(singleNullSet));
        assertTrue(map.containsKey(singleNullSet));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testMixedSingleElementNullContainers() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Different single-element containers with null should all be equivalent
        Object[] nullArray = {null};
        List<Object> nullList = Arrays.asList((Object) null);
        Set<Object> nullSet = new HashSet<>();
        nullSet.add(null);
        Vector<Object> nullVector = new Vector<>();
        nullVector.add(null);
        
        map.put(nullArray, "first_null");
        map.put(nullList, "second_null");    // Should overwrite first_null
        map.put(nullSet, "third_null");      // Should overwrite second_null
        map.put(nullVector, "fourth_null");  // Should overwrite third_null
        
        // All should resolve to the same normalized null key
        assertEquals("fourth_null", map.get((Object) null));
        assertEquals("fourth_null", map.get(nullArray));
        assertEquals("fourth_null", map.get(nullList));
        assertEquals("fourth_null", map.get(nullSet));
        assertEquals("fourth_null", map.get(nullVector));
        
        // Should have only 1 entry (all normalize to NULL_SENTINEL)
        assertEquals(1, map.size());
    }
    
    @Test
    void testSingleElementNullVsDirectNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put direct null
        map.put(null, "direct_null");
        
        // Put single-element array with null - should overwrite direct null
        Object[] singleNull = {null};
        map.put(singleNull, "array_null");
        
        // Both should resolve to the same key
        assertEquals("array_null", map.get((Object) null));
        assertEquals("array_null", map.get(singleNull));
        
        // Should have only 1 entry (both normalize to NULL_SENTINEL with hash 0)
        assertEquals(1, map.size());
    }
    
    @Test
    void testSingleElementNullHashConsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put different single-element null containers
        Object[] nullArray = {null};
        List<Object> nullList = Collections.singletonList(null);
        
        map.put(nullArray, "value1");
        map.put(nullList, "value2");  // Should overwrite value1
        
        // Verify they hash to the same bucket by checking they're equivalent
        assertEquals("value2", map.get(nullArray));
        assertEquals("value2", map.get(nullList));
        assertEquals("value2", map.get((Object) null));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testNullSentinelSpecificCase() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // This test specifically targets the NULL_SENTINEL single-element optimization
        // by using a single-element array that, after expansion, contains only NULL_SENTINEL
        
        // Create an array with a single null element
        Object[] singleNullArray = new Object[1];
        singleNullArray[0] = null;  // This will become NULL_SENTINEL internally
        
        // Put this array as a key
        map.put(singleNullArray, "sentinel_test_value");
        
        // The expansion should result in a single NULL_SENTINEL element
        // which should trigger the special case: hashPass[0] = 0; return NULL_SENTINEL;
        
        // Verify the behavior matches expectations
        assertEquals("sentinel_test_value", map.get((Object) null));
        assertEquals("sentinel_test_value", map.get(singleNullArray));
        
        // Create another single-null array to test equivalence
        Object[] anotherNullArray = {null};
        assertEquals("sentinel_test_value", map.get(anotherNullArray));
        
        // Test that putting another equivalent single-null container overwrites
        List<Object> singleNullList = new ArrayList<>();
        singleNullList.add(null);
        map.put(singleNullList, "overwritten_value");
        
        // Should all resolve to the same key due to NULL_SENTINEL optimization
        assertEquals("overwritten_value", map.get((Object) null));
        assertEquals("overwritten_value", map.get(singleNullArray));
        assertEquals("overwritten_value", map.get(anotherNullArray));
        assertEquals("overwritten_value", map.get(singleNullList));
        
        // Should still have only one entry
        assertEquals(1, map.size());
        
        // Test containsKey for all equivalent forms
        assertTrue(map.containsKey((Object) null));
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
        
        // Verify all entries are accessible
        assertEquals("regular_value", map.get("regular_key"));
        assertEquals("multi_value", map.get(Arrays.asList("multi", "key")));
        assertEquals("null_value", map.get((Object) null));
        assertEquals("null_value", map.get(singleNull));
        
        assertEquals(3, map.size());
    }
    
    @Test
    void testRemoveSingleElementNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Put single-element null array
        Object[] singleNull = {null};
        map.put(singleNull, "null_value");
        
        assertEquals(1, map.size());
        assertEquals("null_value", map.get((Object) null));
        
        // Remove via direct null
        String removed = map.remove((Object) null);
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
        // This ensures that when expandAndHash processes a single-element array with null,
        // it results in exactly [NULL_SENTINEL] without OPEN/CLOSE markers
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(true).build(); // flattenDimensions = true
        
        // Single-element array containing null
        Object[] singleNullArray = {null};
        map.put(singleNullArray, "flattened_null_value");
        
        // This should trigger the NULL_SENTINEL single-element optimization in expandWithHash:
        // - expandAndHash produces [NULL_SENTINEL] (no OPEN/CLOSE due to flatten=true)
        // - expanded.size() == 1 and expanded.get(0) == NULL_SENTINEL
        // - This triggers: hashPass[0] = 0; return NULL_SENTINEL;
        
        // Verify the result
        assertEquals("flattened_null_value", map.get((Object) null));
        assertEquals("flattened_null_value", map.get(singleNullArray));
        
        // Test with collection too
        List<Object> singleNullList = Arrays.asList((Object) null);
        map.put(singleNullList, "flattened_null_list");
        
        // Should overwrite due to same normalized key
        assertEquals("flattened_null_list", map.get((Object) null));
        assertEquals("flattened_null_list", map.get(singleNullArray));
        assertEquals("flattened_null_list", map.get(singleNullList));
        
        assertEquals(1, map.size());
    }
    
    @Test  
    void testNullSentinelOptimizationWithFlattenFalse() {
        // Create a MultiKeyMap with flattenDimensions = false
        // This ensures OPEN/CLOSE markers are added, so single-element array with null
        // becomes [OPEN, NULL_SENTINEL, CLOSE] - not single-element, different behavior
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(false).build(); // flattenDimensions = false
        
        // Single-element array containing null
        Object[] singleNullArray = {null};
        map.put(singleNullArray, "structured_null_value");
        
        // With flatten=false, expandAndHash produces [OPEN, NULL_SENTINEL, CLOSE]
        // This is NOT a single-element case, so the NULL_SENTINEL optimization doesn't apply
        
        // However, the single-element optimization in expandWithHash still applies
        // because it checks the original key structure first
        assertEquals("structured_null_value", map.get((Object) null));
        assertEquals("structured_null_value", map.get(singleNullArray));
        
        assertEquals(1, map.size());
    }
}