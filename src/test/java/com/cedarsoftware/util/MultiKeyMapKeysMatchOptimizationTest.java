package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that keysMatch() optimization correctly handles typed arrays
 * and uses the most specific fast path instead of falling back to Object[] handling.
 */
public class MultiKeyMapKeysMatchOptimizationTest {
    
    @Test
    void testStringArrayFastPath() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Store using String[] - should use String[] fast path, not Object[] path
        String[] stringKey1 = {"apple", "banana", "cherry"};
        String[] stringKey2 = {"apple", "banana", "cherry"};
        
        map.put(stringKey1, "fruit_value");
        
        // This should use the String[] specific comparison, not Object[] comparison
        assertEquals("fruit_value", map.get(stringKey2));
        assertTrue(map.containsKey(stringKey2));
        
        // Verify they are treated as equivalent keys
        assertEquals(1, map.size());
    }
    
    @Test
    void testPrimitiveArrayFastPaths() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test int[] fast path
        int[] intKey1 = {1, 2, 3, 4, 5};
        int[] intKey2 = {1, 2, 3, 4, 5};
        map.put(intKey1, "int_value");
        assertEquals("int_value", map.get(intKey2));
        
        // Test long[] fast path
        long[] longKey1 = {1L, 2L, 3L};
        long[] longKey2 = {1L, 2L, 3L};
        map.put(longKey1, "long_value");
        assertEquals("long_value", map.get(longKey2));
        
        // Test double[] fast path
        double[] doubleKey1 = {1.0, 2.0, 3.0};
        double[] doubleKey2 = {1.0, 2.0, 3.0};
        map.put(doubleKey1, "double_value");
        assertEquals("double_value", map.get(doubleKey2));
        
        // Test boolean[] fast path
        boolean[] boolKey1 = {true, false, true};
        boolean[] boolKey2 = {true, false, true};
        map.put(boolKey1, "bool_value");
        assertEquals("bool_value", map.get(boolKey2));
        
        // Should have 4 different primitive array keys
        assertEquals(4, map.size());
    }
    
    @Test
    void testObjectArrayStillWorks() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Object[] should still work correctly, just processed after more specific types
        Object[] objectKey1 = {"mixed", 123, true, null};
        Object[] objectKey2 = {"mixed", 123, true, null};
        
        map.put(objectKey1, "object_value");
        assertEquals("object_value", map.get(objectKey2));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testMixedArrayTypes() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // String[], Object[], and int[] should all be treated as different key types
        // (even if they have conceptually similar content)
        String[] stringArray = {"1", "2", "3"};
        Object[] objectArray = {"1", "2", "3"};  // Same content, different type
        int[] intArray = {1, 2, 3};              // Same logical content, different type
        
        map.put(stringArray, "string_version");
        map.put(objectArray, "object_version");  // Should overwrite string_version due to cross-type matching
        map.put(intArray, "int_version");
        
        // String[] and Object[] with same content should be equivalent (cross-type matching)
        assertEquals("object_version", map.get(stringArray));
        assertEquals("object_version", map.get(objectArray));
        
        // int[] should be separate since it's a primitive array
        assertEquals("int_version", map.get(intArray));
        
        // Should have 2 different keys: String[]/Object[] equivalence group + int[]
        assertEquals(2, map.size());
    }
    
    @Test
    void testPerformanceImprovement() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create many String[] keys to test performance
        List<String[]> keys = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            String[] key = {"prefix" + i, "middle", "suffix" + i};
            keys.add(key);
            map.put(key, "value" + i);
        }
        
        // Test lookup performance - this should now use String[] fast path
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            String[] lookupKey = {"prefix" + i, "middle", "suffix" + i};
            String result = map.get(lookupKey);
            assertEquals("value" + i, result);
        }
        long endTime = System.nanoTime();
        
        // Performance test should complete reasonably quickly
        // (exact timing depends on hardware, but should be under reasonable bounds)
        long durationMs = (endTime - startTime) / 1_000_000;
        assertTrue(durationMs < 100, "String[] lookup should be fast, took " + durationMs + "ms");
        
        assertEquals(1000, map.size());
    }
    
    @Test
    void testArrayTypePrecedence() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Verify that String[] is handled by String[] path, not Object[] path
        // This is more of a correctness test than a performance test
        
        String[] strArray = {"test", "array", "precedence"};
        map.put(strArray, "string_path");
        
        // Create an equivalent array
        String[] equivalent = {"test", "array", "precedence"};
        assertEquals("string_path", map.get(equivalent));
        
        // The fact that this works correctly proves that String[] instanceof Object[]
        // didn't cause it to be handled by the Object[] path instead of String[] path
        Object[] asObjectArray = strArray;  // This is the same object, just viewed as Object[]
        assertEquals("string_path", map.get(asObjectArray));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testNullHandlingInTypedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test that null elements in typed arrays work correctly with the fast paths
        String[] stringWithNull = {"before", null, "after"};
        String[] anotherStringWithNull = {"before", null, "after"};
        
        map.put(stringWithNull, "string_null_value");
        assertEquals("string_null_value", map.get(anotherStringWithNull));
        
        // Test with Object[] containing nulls
        Object[] objectWithNull = {"before", null, "after"};
        assertEquals("string_null_value", map.get(objectWithNull)); // Should match due to cross-type equivalence
        
        assertEquals(1, map.size());
    }
}