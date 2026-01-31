package com.cedarsoftware.util;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify primitive arrays work correctly as keys without boxing.
 * Tests all 8 primitive types: int, long, double, float, boolean, byte, short, char
 */
public class MultiKeyMapPrimitiveArrayTest {
    
    @Test
    void testIntArrayAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        int[] key1 = {1, 2, 3};
        int[] key2 = {1, 2, 3};  // Same content as key1
        int[] key3 = {4, 5, 6};  // Different content
        
        map.put(key1, "value1");
        
        // Same content should find the same value
        assertEquals("value1", map.get(key1));
        assertEquals("value1", map.get(key2));
        assertTrue(map.containsKey(key1));
        assertTrue(map.containsKey(key2));
        
        // Different content should not find it
        assertNull(map.get(key3));
        assertFalse(map.containsKey(key3));
        
        map.put(key3, "value3");
        assertEquals("value3", map.get(key3));
        assertEquals(2, map.size());
    }
    
    @Test
    void testLongArrayAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        long[] key1 = {100L, 200L, 300L};
        long[] key2 = {100L, 200L, 300L};  // Same content
        
        map.put(key1, "long_value");
        assertEquals("long_value", map.get(key1));
        assertEquals("long_value", map.get(key2));
    }
    
    @Test
    void testDoubleArrayAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        double[] key1 = {1.1, 2.2, 3.3};
        double[] key2 = {1.1, 2.2, 3.3};  // Same content
        
        map.put(key1, "double_value");
        assertEquals("double_value", map.get(key1));
        assertEquals("double_value", map.get(key2));
    }
    
    @Test
    void testFloatArrayAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        float[] key1 = {1.1f, 2.2f, 3.3f};
        float[] key2 = {1.1f, 2.2f, 3.3f};  // Same content
        
        map.put(key1, "float_value");
        assertEquals("float_value", map.get(key1));
        assertEquals("float_value", map.get(key2));
    }
    
    @Test
    void testBooleanArrayAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        boolean[] key1 = {true, false, true};
        boolean[] key2 = {true, false, true};  // Same content
        boolean[] key3 = {false, true, false};  // Different content
        
        map.put(key1, "bool_value");
        assertEquals("bool_value", map.get(key1));
        assertEquals("bool_value", map.get(key2));
        assertNull(map.get(key3));
    }
    
    @Test
    void testByteArrayAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        byte[] key1 = {1, 2, 3};
        byte[] key2 = {1, 2, 3};  // Same content
        
        map.put(key1, "byte_value");
        assertEquals("byte_value", map.get(key1));
        assertEquals("byte_value", map.get(key2));
    }
    
    @Test
    void testShortArrayAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        short[] key1 = {10, 20, 30};
        short[] key2 = {10, 20, 30};  // Same content
        
        map.put(key1, "short_value");
        assertEquals("short_value", map.get(key1));
        assertEquals("short_value", map.get(key2));
    }
    
    @Test
    void testCharArrayAsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        char[] key1 = {'a', 'b', 'c'};
        char[] key2 = {'a', 'b', 'c'};  // Same content
        char[] key3 = {'x', 'y', 'z'};  // Different content
        
        map.put(key1, "char_value");
        assertEquals("char_value", map.get(key1));
        assertEquals("char_value", map.get(key2));
        assertNull(map.get(key3));
        
        map.put(key3, "xyz_value");
        assertEquals("xyz_value", map.get(key3));
        assertEquals(2, map.size());
    }
    
    @Test
    void testEmptyPrimitiveArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        int[] emptyInt = {};
        long[] emptyLong = {};
        double[] emptyDouble = {};
        
        map.put(emptyInt, "empty_int");
        map.put(emptyLong, "empty_long");  // Overwrites - all empty arrays are equal
        map.put(emptyDouble, "empty_double");  // Overwrites again
        
        // All empty arrays are considered equal (same hash, same content)
        assertEquals("empty_double", map.get(emptyInt));
        assertEquals("empty_double", map.get(emptyLong));
        assertEquals("empty_double", map.get(emptyDouble));
        assertEquals(1, map.size());  // Only one entry since all are equal
    }
    
    @Test
    void testLargePrimitiveArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test with a large array to ensure hash computation works for all elements
        int[] largeKey = new int[1000];
        for (int i = 0; i < 1000; i++) {
            largeKey[i] = i;
        }
        
        int[] sameLargeKey = Arrays.copyOf(largeKey, largeKey.length);
        
        map.put(largeKey, "large_value");
        assertEquals("large_value", map.get(largeKey));
        assertEquals("large_value", map.get(sameLargeKey));
        assertEquals(1, map.size());
    }
    
    @Test
    void testMixedPrimitiveAndObjectArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Primitive array
        int[] primitiveKey = {1, 2, 3};
        
        // Object array with same values (boxed)
        Object[] objectKey = {1, 2, 3};
        
        map.put(primitiveKey, "primitive_value");
        map.put(objectKey, "object_value");  // Overwrites - considered equal after boxing
        
        // They are considered equal (keysMatch uses Array.get which boxes primitives)
        assertEquals("object_value", map.get(primitiveKey));
        assertEquals("object_value", map.get(objectKey));
        assertEquals(1, map.size());  // Only one entry since they're equal
    }
    
    @Test
    void testPrimitiveArrayWithNullSentinel() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test that primitive arrays don't interfere with null handling
        int[] key = {0};  // 0 is not null, just a value
        
        map.put(key, "zero_value");
        map.put(null, "null_value");
        
        assertEquals("zero_value", map.get(key));
        assertEquals("null_value", map.get((Object) null));
        assertEquals(2, map.size());
    }
    
    @Test
    void testRemovePrimitiveArrayKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        int[] key1 = {1, 2, 3};
        int[] key2 = {1, 2, 3};  // Same content
        
        map.put(key1, "value");
        assertEquals(1, map.size());
        
        // Remove using different array with same content
        String removed = map.remove(key2);
        assertEquals("value", removed);
        assertEquals(0, map.size());
        assertNull(map.get(key1));
    }
    
    @Test
    void testPrimitiveArraysInMultiKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Using primitive arrays as part of a multi-key
        int[] part1 = {1, 2};
        String part2 = "test";
        
        map.put(new Object[]{part1, part2}, "composite_value");
        
        // Different array with same content should find it
        int[] samePart1 = {1, 2};
        assertEquals("composite_value", map.get(new Object[]{samePart1, part2}));
    }
}