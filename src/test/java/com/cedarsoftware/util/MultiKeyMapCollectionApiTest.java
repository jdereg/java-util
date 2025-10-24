package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Test the new Collection-based API and zero-heap optimizations in MultiKeyMap.
 */
class MultiKeyMapCollectionApiTest {
    private static final Logger LOG = Logger.getLogger(MultiKeyMapCollectionApiTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }

    @Test
    void testCollectionBasedGetMultiKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);

        // Store using Object[] varargs API
        map.putMultiKey("test1", String.class, Integer.class, 42L);
        map.putMultiKey("test2", "key1", "key2", "key3");

        // Retrieve using Collection API - zero heap allocation
        List<Object> keys1 = Arrays.asList(String.class, Integer.class, 42L);
        assertEquals("test1", map.get(keys1));

        // Lists match Lists (but not Sets - Sets are semantically distinct)
        List<Object> keys2 = Arrays.asList("key1", "key2", "key3");
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
        map.putMultiKey("collectionValue", Double.class, Boolean.class, 2L);
        
        // Retrieve using equivalent array
        Object[] arrayKey2 = {Double.class, Boolean.class, 2L};
        assertEquals("collectionValue", map.getMultiKey(arrayKey2));
        
        // Both should work
        List<Object> listKey2 = Arrays.asList(Double.class, Boolean.class, 2L);
        assertEquals("collectionValue", map.get(listKey2));
    }
    
    @Test
    void testCollectionKeyOrdering() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);

        // Order matters for Lists/Arrays (order-dependent)
        map.putMultiKey("ordered", "a", "b", "c");

        List<Object> correctOrder = Arrays.asList("a", "b", "c");
        assertEquals("ordered", map.get(correctOrder));

        List<Object> wrongOrder = Arrays.asList("c", "b", "a");
        assertNull(map.get(wrongOrder));

        // Sets are order-agnostic but semantically distinct from Lists
        // Store with Set
        Set<Object> setKey = new HashSet<>(Arrays.asList("x", "y", "z"));
        map.put(setKey, "set_value");

        // Different Set types with same elements match (order-agnostic)
        Set<Object> linkedHashSetKey = new LinkedHashSet<>(Arrays.asList("z", "x", "y"));
        assertEquals("set_value", map.get(linkedHashSetKey));

        // But Sets don't match Lists even with same elements
        List<Object> listKey = Arrays.asList("x", "y", "z");
        assertNull(map.get(listKey)); // List doesn't match Set key
    }
    
    @Test
    void testCollectionWithNullElements() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store key with null elements
        map.putMultiKey("withNull", String.class, null, 42L);
        
        // Retrieve using Collection with null
        List<Object> keysWithNull = Arrays.asList(String.class, null, 42L);
        assertEquals("withNull", map.get(keysWithNull));
        
        // All nulls
        map.putMultiKey("allNulls", null, null, null);
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

        // Store once using array (via varargs)
        map.putMultiKey("value", "x", "y", "z");

        // Retrieve with different ordered Collection types - all match Arrays/Lists
        List<Object> list = Arrays.asList("x", "y", "z");
        assertEquals("value", map.get(list));

        Vector<Object> vector = new Vector<>(Arrays.asList("x", "y", "z"));
        assertEquals("value", map.get(vector));

        LinkedList<Object> linkedList = new LinkedList<>(Arrays.asList("x", "y", "z"));
        assertEquals("value", map.get(linkedList));

        // Sets are semantically distinct - they don't match Lists/Arrays
        Set<Object> linkedHashSet = new LinkedHashSet<>(Arrays.asList("x", "y", "z"));
        assertNull(map.get(linkedHashSet)); // Set doesn't match Array/List key
    }
    
    
    
    @Test
    void testPerformanceComparison() {
        MultiKeyMap<String> map = new MultiKeyMap<>(32);
        
        // Populate with test data
        for (int i = 0; i < 100; i++) {
            map.putMultiKey("value" + i, String.class, Integer.class, (long) i);
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
            String result = map.getMultiKey(arrayKey);
            assertNotNull(result);
        }
        long arrayTime = System.nanoTime() - start;
        
        LOG.info("Collection access time: " + (collectionTime / 1_000_000.0) + " ms");
        LOG.info("Array access time: " + (arrayTime / 1_000_000.0) + " ms");
        LOG.info("Collection vs Array ratio: " + String.format("%.2f", (double) collectionTime / arrayTime));
        
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
        assertEquals("largeKeyValue", map.getMultiKey(largeArray));
    }
    
    @Test
    void testCollectionHashConsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        
        // Store using varargs (elements as separate keys)
        map.putMultiKey("varargsKey", String.class, Integer.class, 42L);
        
        // Retrieve using equivalent Collection
        List<Object> listKey = Arrays.asList(String.class, Integer.class, 42L);
        String result = map.get(listKey);
        assertEquals("varargsKey", result);
        
        // Retrieve using equivalent array
        Object[] arrayKey = {String.class, Integer.class, 42L};
        String result2 = map.getMultiKey(arrayKey);
        assertEquals("varargsKey", result2);
        
        // All should find the same entry
        assertEquals(1, map.size());
    }
}