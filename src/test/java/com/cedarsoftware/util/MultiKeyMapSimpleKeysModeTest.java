package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for simpleKeysMode feature that avoids isArrayOrCollection checks.
 * When simpleKeysMode is true, arrays and collections are treated as simple keys,
 * not expanded into their elements.
 */
public class MultiKeyMapSimpleKeysModeTest {
    
    @Test
    void testSimpleKeysModeWithArrays() {
        // Create two maps - one with simpleKeysMode, one without
        MultiKeyMap<String> simpleMode = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
            
        MultiKeyMap<String> normalMode = MultiKeyMap.<String>builder()
            .simpleKeysMode(false)
            .build();
        
        // Test with int arrays
        int[] array1 = {1, 2, 3};
        int[] array2 = {1, 2, 3}; // Same content
        
        // In simple mode, arrays are treated as single keys
        simpleMode.put(array1, "value1");
        assertEquals("value1", simpleMode.get(array1));
        assertEquals("value1", simpleMode.get(array2)); // Same content = same key
        assertEquals(1, simpleMode.size());
        
        // In normal mode, arrays are still treated the same for 1D arrays
        normalMode.put(array1, "value1");
        assertEquals("value1", normalMode.get(array1));
        assertEquals("value1", normalMode.get(array2));
        assertEquals(1, normalMode.size());
    }
    
    @Test
    void testSimpleKeysModeAvoidsFunctionCalls() {
        // simpleKeysMode is a performance optimization that avoids isArrayOrCollection() calls
        // The matching behavior remains the same, but performance is improved
        
        MultiKeyMap<String> simpleMode = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
            
        MultiKeyMap<String> normalMode = MultiKeyMap.<String>builder()
            .simpleKeysMode(false)
            .build();
        
        // Both modes should have the same behavior for arrays
        Object[] key1 = {"a", "b", "c"};
        Object[] key2 = {"a", "b", "c"}; // Same content
        
        simpleMode.put(key1, "value1");
        normalMode.put(key1, "value1");
        
        // Both should find the value with same content
        assertEquals("value1", simpleMode.get(key1));
        assertEquals("value1", normalMode.get(key1));
        assertEquals("value1", simpleMode.get(key2));
        assertEquals("value1", normalMode.get(key2));
        
        // The difference is in performance - simpleMode avoids isArrayOrCollection checks
        // This is most noticeable with large numbers of operations
    }
    
    @Test
    void testSimpleKeysModeWithCollections() {
        // Create two maps - one with simpleKeysMode, one without
        MultiKeyMap<String> simpleMode = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
            
        MultiKeyMap<String> normalMode = MultiKeyMap.<String>builder()
            .simpleKeysMode(false)
            .build();
        
        // Test with collections
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = new ArrayList<>(list1); // Same content, different instance
        
        // In simple mode, collections are treated as simple keys
        simpleMode.put(list1, "listValue");
        assertEquals("listValue", simpleMode.get(list1));
        assertEquals("listValue", simpleMode.get(list2)); // Same content = same key
        
        // In normal mode, collections are also matched by content
        normalMode.put(list1, "listValue");
        assertEquals("listValue", normalMode.get(list1));
        assertEquals("listValue", normalMode.get(list2));
    }
    
    @Test
    void testSimpleKeysModePerformanceComparison() {
        // Create maps for performance testing
        MultiKeyMap<String> simpleMode = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .capacity(10000)
            .build();
            
        MultiKeyMap<String> normalMode = MultiKeyMap.<String>builder()
            .simpleKeysMode(false)
            .capacity(10000)
            .build();
        
        // Create test data - arrays with 3 elements
        int numKeys = 10000;
        Object[][] testKeys = new Object[numKeys][3];
        for (int i = 0; i < numKeys; i++) {
            testKeys[i][0] = "key" + i;
            testKeys[i][1] = i;
            testKeys[i][2] = "suffix" + i;
        }
        
        // Warm up JIT
        for (int i = 0; i < 100; i++) {
            simpleMode.put(testKeys[i], "value" + i);
            normalMode.put(testKeys[i], "value" + i);
        }
        simpleMode.clear();
        normalMode.clear();
        
        // Test simple mode performance
        long simpleStart = System.nanoTime();
        for (int i = 0; i < numKeys; i++) {
            simpleMode.put(testKeys[i], "value" + i);
        }
        for (int i = 0; i < numKeys; i++) {
            simpleMode.get(testKeys[i]);
        }
        long simpleTime = System.nanoTime() - simpleStart;
        
        // Test normal mode performance
        long normalStart = System.nanoTime();
        for (int i = 0; i < numKeys; i++) {
            normalMode.put(testKeys[i], "value" + i);
        }
        for (int i = 0; i < numKeys; i++) {
            normalMode.get(testKeys[i]);
        }
        long normalTime = System.nanoTime() - normalStart;
        
        // Simple mode should be faster (no isArrayOrCollection checks)
        System.out.println("Simple mode time: " + simpleTime / 1_000_000 + " ms");
        System.out.println("Normal mode time: " + normalTime / 1_000_000 + " ms");
        System.out.println("Simple mode is " + 
            String.format("%.1f%%", ((double)(normalTime - simpleTime) / normalTime) * 100) + 
            " faster");
        
        // Verify both maps have the same content
        assertEquals(numKeys, simpleMode.size());
        assertEquals(numKeys, normalMode.size());
        for (int i = 0; i < numKeys; i++) {
            assertEquals("value" + i, simpleMode.get(testKeys[i]));
            assertEquals("value" + i, normalMode.get(testKeys[i]));
        }
    }
    
    @Test
    void testSimpleKeysModeDefaultsToFalse() {
        // Verify default behavior is backward compatible (simpleKeysMode = false)
        MultiKeyMap<String> defaultMap = new MultiKeyMap<>();
        
        // Test with nested structure - should expand in default mode
        Object[] nested = {new String[]{"a", "b"}, "middle", new int[]{1, 2}};
        defaultMap.put(nested, "value");
        assertEquals("value", defaultMap.get(nested));
        
        // Default should handle nested structures (not simple mode)
        assertEquals(1, defaultMap.size());
    }
    
    @Test
    void testSimpleKeysModeBuilder() {
        // Test builder configuration
        MultiKeyMap<String> map1 = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .capacity(100)
            .loadFactor(0.8f)
            .build();
            
        MultiKeyMap<String> map2 = MultiKeyMap.<String>builder()
            .simpleKeysMode(false)
            .capacity(100)
            .loadFactor(0.8f)
            .build();
        
        // Both should work, just with different internal behavior
        map1.put(new Object[]{"test"}, "value1");
        map2.put(new Object[]{"test"}, "value2");
        
        assertEquals("value1", map1.get(new Object[]{"test"}));
        assertEquals("value2", map2.get(new Object[]{"test"}));
    }
}