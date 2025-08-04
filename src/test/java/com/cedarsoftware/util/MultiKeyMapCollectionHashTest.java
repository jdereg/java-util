package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that collection keys in COLLECTIONS_NOT_EXPANDED mode
 * use the collection's own hashCode() for hash computation, ensuring
 * equivalent collections are treated as the same key.
 */
public class MultiKeyMapCollectionHashTest {
    
    @Test
    void testCollectionHashConsistency() {
        // Use COLLECTIONS_NOT_EXPANDED mode to test the optimization
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(false)
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED)
                .build();

        // Test 0 elements - empty collections
        List<Object> emptyList1 = new ArrayList<>();
        List<Object> emptyList2 = new ArrayList<>();  // Same type
        List<Object> emptyLinkedList = new LinkedList<>();
        Set<Object> emptySet = new HashSet<>();
        
        map.put(emptyList1, "empty1");
        // Same collection type with same content should be equivalent (like regular Map)
        assertEquals("empty1", map.get(emptyList2));
        assertTrue(map.containsKey(emptyList2));
        
        // Different collection types may or may not be equivalent - depends on their equals() implementation
        // LinkedList.equals() works with any List, so this should work
        assertEquals("empty1", map.get(emptyLinkedList));
        assertTrue(map.containsKey(emptyLinkedList));
        
        // But HashSet.equals() only works with other Sets, so this should not work
        assertNull(map.get(emptySet));
        assertFalse(map.containsKey(emptySet));
        
        // Test 1 element with null
        List<Object> nullList1 = Arrays.asList((Object) null);
        List<Object> nullList2 = new ArrayList<>();
        nullList2.add(null);
        
        map.put(nullList1, "null_element");
        assertEquals("null_element", map.get(nullList2));
        assertTrue(map.containsKey(nullList2));
        
        // Test 1 element with "a" - in NOT_EXPANDED mode, no single-element optimization
        List<String> singleA1 = Arrays.asList("a");
        List<String> singleA2 = new ArrayList<>();
        singleA2.add("a");
        
        map.put(singleA1, "single_a");
        // In NOT_EXPANDED mode, collections are stored as-is, no single-element optimization
        assertNull(map.get("a"));  // Direct string lookup should not find collection entry
        assertEquals("single_a", map.get(singleA2));  // Collection lookup should work
        assertFalse(map.containsKey("a"));
        assertTrue(map.containsKey(singleA2));
        
        // Test 1 element with nested array [["a"]]  
        // In NOT_EXPANDED mode, collections are stored as-is
        String[][] nestedArray = {{"a"}};
        List<String[][]> singleNested1 = new ArrayList<>();
        singleNested1.add(nestedArray);
        List<String[][]> singleNested2 = new ArrayList<>();
        singleNested2.add(nestedArray);
        
        map.put(singleNested1, "nested_array");
        // In NOT_EXPANDED mode, collection is stored as-is, not extracted
        assertNull(map.get(nestedArray));  // Direct nested array lookup should not find collection entry
        assertEquals("nested_array", map.get(singleNested2));  // Collection lookup should work
        assertFalse(map.containsKey(nestedArray));
        assertTrue(map.containsKey(singleNested2));
        
        // Verify the map size - should still be counting previous entries plus this one
        // We have: empty collections, null element, single "a", and nested array = at least 4 keys
        // But some might be equivalent due to single-element optimization
        
        // Test 2 elements (null, null)
        List<Object> doubleNull1 = Arrays.asList(null, null);
        List<Object> doubleNull2 = new ArrayList<>();
        doubleNull2.add(null);
        doubleNull2.add(null);
        
        map.put(doubleNull1, "double_null");
        assertEquals("double_null", map.get(doubleNull2));
        assertTrue(map.containsKey(doubleNull2));
        
        // Test 2 elements (null, "a")
        List<Object> nullA1 = Arrays.asList(null, "a");
        List<Object> nullA2 = new ArrayList<>();
        nullA2.add(null);
        nullA2.add("a");
        
        map.put(nullA1, "null_a");
        assertEquals("null_a", map.get(nullA2));
        assertTrue(map.containsKey(nullA2));
        
        // Test 2 elements ("a", "b")
        List<String> ab1 = Arrays.asList("a", "b");
        List<String> ab2 = new ArrayList<>();
        ab2.add("a");
        ab2.add("b");
        
        map.put(ab1, "a_b");
        assertEquals("a_b", map.get(ab2));
        assertTrue(map.containsKey(ab2));
        
        // Test 2 elements with nested structure (["a"], ["b"])
        String[] arrayA = {"a"};
        String[] arrayB = {"b"};
        List<String[]> nestedAB1 = Arrays.asList(arrayA, arrayB);
        List<String[]> nestedAB2 = new ArrayList<>();
        nestedAB2.add(arrayA);
        nestedAB2.add(arrayB);
        
        map.put(nestedAB1, "nested_a_b");
        assertEquals("nested_a_b", map.get(nestedAB2));
        assertTrue(map.containsKey(nestedAB2));
    }
    
    @Test
    void testCollectionHashNonEquivalenceInNotExpandedMode() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(false)
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED)
                .build();


        // Test that collections with same content but different types are NOT equivalent in NOT_EXPANDED mode
        List<String> list = Arrays.asList("x", "y", "z");
        Set<String> set = new LinkedHashSet<>(Arrays.asList("x", "y", "z")); // Maintain order
        
        map.put(list, "xyz_list");
        map.put(set, "xyz_set");
        
        // In NOT_EXPANDED mode, List and Set should be different keys (like in regular Map)
        assertEquals("xyz_list", map.get(list));
        assertEquals("xyz_set", map.get(set));
        assertEquals("xyz_list", map.get(Arrays.asList("x", "y", "z"))); // Same content list should find entry
        
        // Should have two separate entries
        assertEquals(2, map.size());
        
        assertTrue(map.containsKey(list));
        assertTrue(map.containsKey(set));
    }
    
    @Test
    void testCollectionHashEquivalenceInExpandedMode() {
        // Test the opposite - in normal expanded mode, List and Set with same elements should be equivalent
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(true).build(); // Default expanded mode
        
        List<String> list = Arrays.asList("x", "y", "z");
        Set<String> set = new LinkedHashSet<>(Arrays.asList("x", "y", "z")); // Maintain order
        
        map.put(list, "xyz_list");
        map.put(set, "xyz_set_overwrites"); // Should overwrite because they expand to same representation
        
        // In expanded mode, both should resolve to the same key
        assertEquals("xyz_set_overwrites", map.get(list));
        assertEquals("xyz_set_overwrites", map.get(set));
        
        // Should have only one entry (they're equivalent when expanded)
        assertEquals(1, map.size());
    }
    
    @Test
    void testCollectionModificationIsolation() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(false)
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED)
                .build();

        // Test that modifying original collection doesn't affect stored key
        List<String> mutableList = new ArrayList<>();
        mutableList.add("original");
        mutableList.add("content");
        
        map.put(mutableList, "original_value");
        
        // Modify the original collection
        mutableList.add("modified");
        
        // Should still find with original content due to defensive copying
        List<String> lookupList = Arrays.asList("original", "content");
        assertEquals("original_value", map.get(lookupList));
        
        // Modified list should not find the entry
        assertNull(map.get(mutableList));
    }
    
    @Test
    void testNoSingleElementOptimizationInNotExpandedMode() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(false)
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED)
                .build();

        // In NOT_EXPANDED mode, no single-element optimization should occur
        List<String> singleElement = Arrays.asList("test");
        map.put(singleElement, "value");
        
        // Should NOT be accessible via direct element (no single-element optimization)
        assertNull(map.get("test"));  // Direct element access should fail
        assertEquals("value", map.get(Arrays.asList("test")));  // Collection access should work
        
        // Only one entry should exist
        assertEquals(1, map.size());
        
        // Only collection key should work
        assertFalse(map.containsKey("test"));
        assertTrue(map.containsKey(Arrays.asList("test")));
    }
}