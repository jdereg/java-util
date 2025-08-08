package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test the new 1-key and 2-key optimizations with informed handoff.
 */
public class MultiKeyMapOptimizationTest {

    @Test
    void testSingleKeyOptimizations() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test simple object (fast path)
        map.put("simpleKey", "simpleValue");
        assertEquals("simpleValue", map.get("simpleKey"));
        
        // Test null key (fast path)
        map.put(null, "nullValue");
        assertEquals("nullValue", map.get(null));
        
        // Test array key (informed handoff path)
        String[] arrayKey = {"array", "key"};
        map.put(arrayKey, "arrayValue");
        assertEquals("arrayValue", map.get(arrayKey));
        
        // Test equivalent array key (should find same entry)
        String[] arrayKey2 = {"array", "key"};
        assertEquals("arrayValue", map.get(arrayKey2));
    }
    
    @Test
    void testTwoKeyOptimizations() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test two simple keys (ultra-fast path)
        map.putMultiKey("twoSimpleValue", "key1", "key2");
        assertEquals("twoSimpleValue", map.getMultiKey("key1", "key2"));
        
        // Test mixed simple and array keys (informed handoff)
        String[] arrayPart = {"nested", "array"};
        map.putMultiKey("mixedValue", "simple", arrayPart);
        assertEquals("mixedValue", map.getMultiKey("simple", arrayPart));
        
        // Test two null keys
        map.putMultiKey("nullsValue", null, null);
        assertEquals("nullsValue", map.getMultiKey(null, null));
    }
    
    @Test
    void testPerformanceImplications() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Warm up the JIT
        for (int i = 0; i < 1000; i++) {
            map.put("warmup" + i, "value" + i);
            map.get("warmup" + i);
            
            map.putMultiKey("warmup2_" + i, "key1_" + i, "key2_" + i);
            map.getMultiKey("key1_" + i, "key2_" + i);
        }
        
        // Test that optimizations don't break functionality
        long startTime = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            // Simple 1-key operations
            map.put("perf" + i, "value" + i);
            assertEquals("value" + i, map.get("perf" + i));
            
            // Simple 2-key operations  
            map.putMultiKey("perf2_" + i, "k1_" + i, "k2_" + i);
            assertEquals("perf2_" + i, map.getMultiKey("k1_" + i, "k2_" + i));
        }
        long endTime = System.nanoTime();
        
        // Should complete reasonably fast (this is more of a functionality test)
        assertTrue(endTime - startTime < 100_000_000, "Operations took too long: " + (endTime - startTime) + "ns");
    }
    
    @Test
    void testInformedHandoffCorrectness() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test that informed handoff produces same results as general path
        int[] intArray = {1, 2, 3};
        String[] stringArray = {"a", "b", "c"};
        
        // Store via general path
        map.putMultiKey("generalValue", intArray, stringArray);
        
        // Retrieve via optimized path should find same entry
        assertEquals("generalValue", map.getMultiKey(intArray, stringArray));
        
        // Different but equivalent arrays should also work
        int[] intArray2 = {1, 2, 3};
        String[] stringArray2 = {"a", "b", "c"};
        assertEquals("generalValue", map.getMultiKey(intArray2, stringArray2));
        
        // Nested arrays should work
        int[][] nested = {{1, 2}, {3, 4}};
        map.put(nested, "nestedValue");
        assertEquals("nestedValue", map.get(nested));
        
        int[][] nested2 = {{1, 2}, {3, 4}};
        assertEquals("nestedValue", map.get(nested2));
    }
}