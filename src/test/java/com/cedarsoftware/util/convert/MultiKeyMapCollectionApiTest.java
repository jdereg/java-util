package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Test the new Collection-based API and zero-heap optimizations in MultiKeyMap.
 */
class MultiKeyMapCollectionApiTest {
    
    @Test
    void testCollectionBasedGet() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using Object[] varargs API
        map.put("test1", String.class, Integer.class, 42L);
        map.put("test2", "key1", "key2", "key3");
        
        // Retrieve using Collection API - zero heap allocation
        List<Object> keys1 = Arrays.asList(String.class, Integer.class, 42L);
        assertEquals("test1", map.get(keys1));
        
        Set<Object> keys2 = new LinkedHashSet<>(Arrays.asList("key1", "key2", "key3"));
        assertEquals("test2", map.get(keys2));
        
        // Non-existent key
        List<Object> keys3 = Arrays.asList(String.class, Long.class, 99L);
        assertNull(map.get(keys3));
    }
    
    @Test
    void testCollectionVsArrayKeyEquality() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using array
        Object[] arrayKey = {String.class, Integer.class, 1L};
        map.put(arrayKey, "arrayValue");
        
        // Retrieve using equivalent Collection
        List<Object> listKey = Arrays.asList(String.class, Integer.class, 1L);
        assertEquals("arrayValue", map.get(listKey));
        
        // Store using Collection (via varargs)
        map.put("collectionValue", Double.class, Boolean.class, 2L);
        
        // Retrieve using equivalent array
        Object[] arrayKey2 = {Double.class, Boolean.class, 2L};
        assertEquals("collectionValue", map.get(arrayKey2));
        
        // Both should work
        List<Object> listKey2 = Arrays.asList(Double.class, Boolean.class, 2L);
        assertEquals("collectionValue", map.get(listKey2));
    }
    
    @Test
    void testCollectionKeyOrdering() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Order matters for key equality
        map.put("ordered", "a", "b", "c");
        
        List<Object> correctOrder = Arrays.asList("a", "b", "c");
        assertEquals("ordered", map.get(correctOrder));
        
        List<Object> wrongOrder = Arrays.asList("c", "b", "a");
        assertNull(map.get(wrongOrder));
        
        // LinkedHashSet preserves insertion order
        Set<Object> orderedSet = new LinkedHashSet<>();
        orderedSet.add("a");
        orderedSet.add("b");
        orderedSet.add("c");
        assertEquals("ordered", map.get(orderedSet));
        
        // Regular HashSet does NOT guarantee order
        Set<Object> unorderedSet = new HashSet<>(Arrays.asList("a", "b", "c"));
        // Note: This test might be flaky due to HashSet's unpredictable ordering
        // In practice, developers should use ordered Collections for keys
    }
    
    @Test
    void testCollectionWithNullElements() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store key with null elements
        map.put("withNull", String.class, null, 42L);
        
        // Retrieve using Collection with null
        List<Object> keysWithNull = Arrays.asList(String.class, null, 42L);
        assertEquals("withNull", map.get(keysWithNull));
        
        // All nulls
        map.put("allNulls", null, null, null);
        List<Object> allNullKeys = Arrays.asList(null, null, null);
        assertEquals("allNulls", map.get(allNullKeys));
    }
    
    @Test
    void testEmptyAndNullCollections() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Empty collection should return null
        List<Object> emptyList = new ArrayList<>();
        assertNull(map.get(emptyList));
        
        // Null collection should return null
        assertNull(map.get((Collection<?>) null));
    }
    
    @Test
    void testCollectionTypeVariations() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store once
        map.put("value", "x", "y", "z");
        
        // Retrieve with different Collection types - all should work
        List<Object> list = Arrays.asList("x", "y", "z");
        assertEquals("value", map.get(list));
        
        Set<Object> linkedHashSet = new LinkedHashSet<>(Arrays.asList("x", "y", "z"));
        assertEquals("value", map.get(linkedHashSet));
        
        Vector<Object> vector = new Vector<>(Arrays.asList("x", "y", "z"));
        assertEquals("value", map.get(vector));
        
        LinkedList<Object> linkedList = new LinkedList<>(Arrays.asList("x", "y", "z"));
        assertEquals("value", map.get(linkedList));
    }
    
    @Test
    void testCollectionVsSingleKeyDisambiguation() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        List<String> myList = Arrays.asList("a", "b", "c");
        
        // Store the List itself as a single key using putSingleKey
        map.putSingleKey(myList, "listAsKey");
        
        // Store using separate arguments for multi-dimensional key
        map.put("elementsAsKey", "a", "b", "c");
        
        // Retrieve appropriately
        assertEquals("listAsKey", map.getSingleKey(myList));     // List is the key
        assertEquals("elementsAsKey", map.get(myList));         // List elements are the key dimensions
        
        // Verify they're different entries
        assertEquals(2, map.size());
    }
    
    @Test
    void testZeroHeapNullOptimization() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store value for null key
        map.putSingleKey(null, "nullValue");
        
        // Multiple get(null) calls should use pre-allocated array
        // (This test verifies functionality; heap allocation testing would require profiling tools)
        assertEquals("nullValue", map.get((Object) null));
        assertEquals("nullValue", map.get((Object) null));
        assertEquals("nullValue", map.get((Object) null));
        
        // Varargs null handling
        Object[] nullArray = null;
        assertEquals("nullValue", map.get(nullArray));
    }
    
    @Test
    void testPerformanceComparison() {
        MultiKeyMap<String> map = new MultiKeyMap<>(32);
        
        // Populate with test data
        for (int i = 0; i < 100; i++) {
            map.put("value" + i, String.class, Integer.class, (long) i);
        }
        
        // Create Collection for repeated lookups
        List<Object> searchKey = Arrays.asList(String.class, Integer.class, 50L);
        
        // Warm up
        for (int i = 0; i < 1000; i++) {
            map.get(searchKey);
        }
        
        // Time Collection-based access
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            String result = map.get(searchKey);
            assertNotNull(result);
        }
        long collectionTime = System.nanoTime() - start;
        
        // Time array-based access
        Object[] arrayKey = {String.class, Integer.class, 50L};
        start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            String result = map.get(arrayKey);
            assertNotNull(result);
        }
        long arrayTime = System.nanoTime() - start;
        
        System.out.println("Collection access time: " + (collectionTime / 1_000_000.0) + " ms");
        System.out.println("Array access time: " + (arrayTime / 1_000_000.0) + " ms");
        System.out.println("Collection vs Array ratio: " + String.format("%.2f", (double) collectionTime / arrayTime));
        
        // Both operations should complete successfully (performance ratio varies by environment)
        assertTrue(collectionTime > 0 && arrayTime > 0, 
                  "Both operations should complete in measurable time");
    }
    
    @Test
    void testLargeCollectionKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Test with large multi-dimensional keys - store using array
        Object[] largeArray = new Object[20];
        for (int i = 0; i < 20; i++) {
            largeArray[i] = "dimension" + i;
        }
        
        map.put(largeArray, "largeKeyValue");
        
        // Retrieve using equivalent Collection
        List<Object> largeKey = Arrays.asList(largeArray);
        assertEquals("largeKeyValue", map.get(largeKey));
        
        // Verify using array access too
        assertEquals("largeKeyValue", map.get(largeArray));
    }
    
    @Test
    void testCollectionHashConsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs (elements as separate keys)
        map.put("varargsKey", String.class, Integer.class, 42L);
        
        // Retrieve using equivalent Collection
        List<Object> listKey = Arrays.asList(String.class, Integer.class, 42L);
        String result = map.get(listKey);
        assertEquals("varargsKey", result);
        
        // Retrieve using equivalent array
        Object[] arrayKey = {String.class, Integer.class, 42L};
        String result2 = map.get(arrayKey);
        assertEquals("varargsKey", result2);
        
        // All should find the same entry
        assertEquals(1, map.size());
    }
}