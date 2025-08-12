package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that typed array processing optimizations work correctly
 * and use type-specific fast paths instead of reflection-based Array.get().
 */
public class MultiKeyMapTypedArrayProcessingTest {
    
    @Test
    void testStringArrayProcessing() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // String[] should use optimized processing without reflection
        String[] stringArray = {"alpha", "beta", "gamma"};
        map.put(stringArray, "string_array_value");
        
        // Verify lookup works
        String[] lookupArray = {"alpha", "beta", "gamma"};
        assertEquals("string_array_value", map.get(lookupArray));
        assertTrue(map.containsKey(lookupArray));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testIntArrayProcessing() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // int[] should use optimized processing without reflection
        int[] intArray = {1, 2, 3, 4, 5};
        map.put(intArray, "int_array_value");
        
        // Verify lookup works
        int[] lookupArray = {1, 2, 3, 4, 5};
        assertEquals("int_array_value", map.get(lookupArray));
        assertTrue(map.containsKey(lookupArray));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testLongArrayProcessing() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // long[] should use optimized processing without reflection
        long[] longArray = {1L, 2L, 3L, 4L, 5L};
        map.put(longArray, "long_array_value");
        
        // Verify lookup works
        long[] lookupArray = {1L, 2L, 3L, 4L, 5L};
        assertEquals("long_array_value", map.get(lookupArray));
        assertTrue(map.containsKey(lookupArray));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testDoubleArrayProcessing() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // double[] should use optimized processing without reflection
        double[] doubleArray = {1.0, 2.0, 3.0, 4.0, 5.0};
        map.put(doubleArray, "double_array_value");
        
        // Verify lookup works
        double[] lookupArray = {1.0, 2.0, 3.0, 4.0, 5.0};
        assertEquals("double_array_value", map.get(lookupArray));
        assertTrue(map.containsKey(lookupArray));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testBooleanArrayProcessing() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // boolean[] should use optimized processing without reflection
        boolean[] boolArray = {true, false, true, false};
        map.put(boolArray, "boolean_array_value");
        
        // Verify lookup works
        boolean[] lookupArray = {true, false, true, false};
        assertEquals("boolean_array_value", map.get(lookupArray));
        assertTrue(map.containsKey(lookupArray));
        
        assertEquals(1, map.size());
    }
    
    @Test
    void testSingleElementOptimizationTypedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Single element arrays NO LONGER collapse - they stay as arrays
        String[] singleString = {"single"};
        int[] singleInt = {42};
        long[] singleLong = {123L};
        double[] singleDouble = {3.14};
        boolean[] singleBoolean = {true};
        
        map.put(singleString, "single_string");
        map.put(singleInt, "single_int");
        map.put(singleLong, "single_long");
        map.put(singleDouble, "single_double");
        map.put(singleBoolean, "single_boolean");
        
        // Direct values are NOT stored - arrays don't collapse
        assertNull(map.get("single"));
        assertNull(map.get(42));
        assertNull(map.get(123L));
        assertNull(map.get(3.14));
        assertNull(map.get(true));
        
        // But arrays work
        assertEquals("single_string", map.get(new String[]{"single"}));
        assertEquals("single_int", map.get(new int[]{42}));
        assertEquals("single_long", map.get(new long[]{123L}));
        assertEquals("single_double", map.get(new double[]{3.14}));
        assertEquals("single_boolean", map.get(new boolean[]{true}));
        
        // Should have 5 different keys (each array)
        assertEquals(5, map.size());
    }
    
    @Test
    void testEmptyArraysTypedProcessing() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Empty arrays should all be equivalent
        String[] emptyString = {};
        int[] emptyInt = {};
        long[] emptyLong = {};
        double[] emptyDouble = {};
        boolean[] emptyBoolean = {};
        
        map.put(emptyString, "empty_string");
        map.put(emptyInt, "empty_int");  // Should overwrite empty_string
        map.put(emptyLong, "empty_long"); // Should overwrite empty_int
        map.put(emptyDouble, "empty_double"); // Should overwrite empty_long
        map.put(emptyBoolean, "empty_boolean"); // Should overwrite empty_double
        
        // All empty arrays should be equivalent
        assertEquals("empty_boolean", map.get(emptyString));
        assertEquals("empty_boolean", map.get(emptyInt));
        assertEquals("empty_boolean", map.get(emptyLong));
        assertEquals("empty_boolean", map.get(emptyDouble));
        assertEquals("empty_boolean", map.get(emptyBoolean));
        
        // Should have only 1 key (all empty arrays are equivalent)
        assertEquals(1, map.size());
    }
    
    @Test
    void testNullElementsInTypedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // String arrays can contain nulls
        String[] stringWithNull = {"before", null, "after"};
        String[] anotherStringWithNull = {"before", null, "after"};
        
        map.put(stringWithNull, "string_null_value");
        assertEquals("string_null_value", map.get(anotherStringWithNull));
        
        // Primitive arrays can't contain nulls, so they're always 1D
        int[] intArray = {1, 2, 3};
        long[] longArray = {1L, 2L, 3L};
        double[] doubleArray = {1.0, 2.0, 3.0};
        boolean[] boolArray = {true, false, true};
        
        map.put(intArray, "int_value");
        map.put(longArray, "long_value");
        map.put(doubleArray, "double_value");
        map.put(boolArray, "bool_value");
        
        assertEquals("int_value", map.get(new int[]{1, 2, 3}));
        assertEquals("long_value", map.get(new long[]{1L, 2L, 3L}));
        assertEquals("double_value", map.get(new double[]{1.0, 2.0, 3.0}));
        assertEquals("bool_value", map.get(new boolean[]{true, false, true}));
        
        assertEquals(5, map.size()); // string + 4 primitive arrays
    }
    
    @Test
    void testTypedArrayProcessingPerformance() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create many typed arrays to test performance improvement
        List<int[]> intArrays = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            int[] array = {i, i + 1, i + 2};
            intArrays.add(array);
            map.put(array, "value" + i);
        }
        
        // Test lookup performance - should use fast typed processing
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            int[] lookupArray = {i, i + 1, i + 2};
            String result = map.get(lookupArray);
            assertEquals("value" + i, result);
        }
        long endTime = System.nanoTime();
        
        // Performance test should complete reasonably quickly
        long durationMs = (endTime - startTime) / 1_000_000;
        assertTrue(durationMs < 100, "int[] processing should be fast, took " + durationMs + "ms");
        
        assertEquals(1000, map.size());
    }
    
    @Test
    void testMixedTypedArrayTypes() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Different typed arrays with similar content should be different keys
        String[] stringArray = {"1", "2", "3"};
        int[] intArray = {1, 2, 3};
        long[] longArray = {1L, 2L, 3L};
        double[] doubleArray = {1.0, 2.0, 3.0};
        
        map.put(stringArray, "string_version");
        map.put(intArray, "int_version");
        map.put(longArray, "long_version");
        map.put(doubleArray, "double_version");
        
        // Each should be a separate key
        assertEquals("string_version", map.get(stringArray));
        assertEquals("int_version", map.get(intArray));
        assertEquals("long_version", map.get(longArray));
        assertEquals("double_version", map.get(doubleArray));
        
        // Should have 4 different keys
        assertEquals(4, map.size());
    }
    
    @Test
    void testGenericArrayFallback() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test with less common array types that should fall back to generic processing
        Float[] floatObjectArray = {1.0f, 2.0f, 3.0f};
        Short[] shortObjectArray = {(short) 1, (short) 2, (short) 3};
        
        map.put(floatObjectArray, "float_object_value");
        map.put(shortObjectArray, "short_object_value");
        
        // These should work through the generic array fallback
        assertEquals("float_object_value", map.get(new Float[]{1.0f, 2.0f, 3.0f}));
        assertEquals("short_object_value", map.get(new Short[]{(short) 1, (short) 2, (short) 3}));
        
        assertEquals(2, map.size());
    }
}