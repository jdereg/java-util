package com.cedarsoftware.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for atomic array support in MultiKeyMap.
 * Verifies that atomic arrays are properly converted to regular arrays during key normalization.
 * 
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class MultiKeyMapAtomicArrayTest {
    
    private MultiKeyMap<String> map;
    
    @BeforeEach
    void setUp() {
        map = new MultiKeyMap<>();
    }
    
    @Test
    void testAtomicIntegerArrayConversion() {
        // Create atomic array
        AtomicIntegerArray atomicArray = new AtomicIntegerArray(new int[]{1, 2, 3});
        
        // Store value with atomic array as key
        map.put(atomicArray, "atomic-int-123");
        
        // Should be retrievable with regular int array
        int[] regularArray = new int[]{1, 2, 3};
        assertEquals("atomic-int-123", map.get(regularArray));
        
        // Should also be retrievable with another atomic array with same values
        AtomicIntegerArray atomicArray2 = new AtomicIntegerArray(new int[]{1, 2, 3});
        assertEquals("atomic-int-123", map.get(atomicArray2));
        
        // Different values should not match
        AtomicIntegerArray atomicArray3 = new AtomicIntegerArray(new int[]{1, 2, 4});
        assertNull(map.get(atomicArray3));
    }
    
    @Test
    void testAtomicLongArrayConversion() {
        // Create atomic array
        AtomicLongArray atomicArray = new AtomicLongArray(new long[]{100L, 200L, 300L});
        
        // Store value with atomic array as key
        map.put(atomicArray, "atomic-long-100-200-300");
        
        // Should be retrievable with regular long array
        long[] regularArray = new long[]{100L, 200L, 300L};
        assertEquals("atomic-long-100-200-300", map.get(regularArray));
        
        // Should also be retrievable with another atomic array with same values
        AtomicLongArray atomicArray2 = new AtomicLongArray(new long[]{100L, 200L, 300L});
        assertEquals("atomic-long-100-200-300", map.get(atomicArray2));
        
        // Different values should not match
        AtomicLongArray atomicArray3 = new AtomicLongArray(new long[]{100L, 200L, 301L});
        assertNull(map.get(atomicArray3));
    }
    
    @Test
    void testAtomicReferenceArrayConversion() {
        // Create atomic array with strings
        AtomicReferenceArray<String> atomicArray = new AtomicReferenceArray<>(new String[]{"a", "b", "c"});
        
        // Store value with atomic array as key
        map.put(atomicArray, "atomic-ref-abc");
        
        // Should be retrievable with regular Object array
        Object[] regularArray = new Object[]{"a", "b", "c"};
        assertEquals("atomic-ref-abc", map.get(regularArray));
        
        // Should be retrievable with String array too
        String[] stringArray = new String[]{"a", "b", "c"};
        assertEquals("atomic-ref-abc", map.get(stringArray));
        
        // Should also be retrievable with another atomic array with same values
        AtomicReferenceArray<String> atomicArray2 = new AtomicReferenceArray<>(new String[]{"a", "b", "c"});
        assertEquals("atomic-ref-abc", map.get(atomicArray2));
        
        // Different values should not match
        AtomicReferenceArray<String> atomicArray3 = new AtomicReferenceArray<>(new String[]{"a", "b", "d"});
        assertNull(map.get(atomicArray3));
    }
    
    @Test
    void testEmptyAtomicArrays() {
        // Empty AtomicIntegerArray
        AtomicIntegerArray emptyIntArray = new AtomicIntegerArray(0);
        map.put(emptyIntArray, "empty-int");
        assertEquals("empty-int", map.get(new int[0]));
        assertEquals("empty-int", map.get(new AtomicIntegerArray(0)));
        
        // Empty AtomicLongArray
        AtomicLongArray emptyLongArray = new AtomicLongArray(0);
        map.put(emptyLongArray, "empty-long");
        assertEquals("empty-long", map.get(new long[0]));
        assertEquals("empty-long", map.get(new AtomicLongArray(0)));
        
        // Empty AtomicReferenceArray
        AtomicReferenceArray<String> emptyRefArray = new AtomicReferenceArray<>(0);
        map.put(emptyRefArray, "empty-ref");
        assertEquals("empty-ref", map.get(new Object[0]));
        assertEquals("empty-ref", map.get(new AtomicReferenceArray<>(0)));
    }
    
    @Test
    void testAtomicArrayWithNulls() {
        // AtomicReferenceArray with nulls
        AtomicReferenceArray<String> atomicArray = new AtomicReferenceArray<>(3);
        atomicArray.set(0, "first");
        atomicArray.set(1, null);
        atomicArray.set(2, "third");
        
        map.put(atomicArray, "with-nulls");
        
        // Should be retrievable with regular array with nulls
        Object[] regularArray = new Object[]{"first", null, "third"};
        assertEquals("with-nulls", map.get(regularArray));
        
        // Should also work with another atomic array
        AtomicReferenceArray<String> atomicArray2 = new AtomicReferenceArray<>(3);
        atomicArray2.set(0, "first");
        atomicArray2.set(1, null);
        atomicArray2.set(2, "third");
        assertEquals("with-nulls", map.get(atomicArray2));
    }
    
    @Test
    @org.junit.jupiter.api.Disabled("Nested atomic arrays need deep conversion support")
    void testNestedAtomicArrays() {
        // Create nested structure with atomic arrays
        AtomicReferenceArray<Object> outer = new AtomicReferenceArray<>(2);
        AtomicIntegerArray inner = new AtomicIntegerArray(new int[]{1, 2});
        outer.set(0, inner);
        outer.set(1, "text");
        
        map.put(outer, "nested-atomic");
        
        // When the outer AtomicReferenceArray is converted to Object[],
        // the inner AtomicIntegerArray remains as-is (not converted to int[])
        // This is because the conversion only happens at the top level
        Object[] regularOuter = new Object[2];
        regularOuter[0] = new AtomicIntegerArray(new int[]{1, 2});
        regularOuter[1] = "text";
        assertEquals("nested-atomic", map.get(regularOuter));
        
        // Also test that we can retrieve with the same structure
        AtomicReferenceArray<Object> outer2 = new AtomicReferenceArray<>(2);
        outer2.set(0, new AtomicIntegerArray(new int[]{1, 2}));
        outer2.set(1, "text");
        assertEquals("nested-atomic", map.get(outer2));
    }
    
    @Test
    void testAtomicArrayOverwrite() {
        // Store with atomic array
        AtomicIntegerArray atomic1 = new AtomicIntegerArray(new int[]{1, 2, 3});
        map.put(atomic1, "first");
        
        // Overwrite with regular array
        int[] regular = new int[]{1, 2, 3};
        map.put(regular, "second");
        
        // Should get the overwritten value
        assertEquals("second", map.get(atomic1));
        assertEquals("second", map.get(regular));
        
        // Overwrite again with another atomic array
        AtomicIntegerArray atomic2 = new AtomicIntegerArray(new int[]{1, 2, 3});
        map.put(atomic2, "third");
        
        // All should retrieve the latest value
        assertEquals("third", map.get(atomic1));
        assertEquals("third", map.get(atomic2));
        assertEquals("third", map.get(regular));
    }
    
    @Test
    void testMixedAtomicAndRegularArrayKeys() {
        // Add entries with different array types
        map.put(new AtomicIntegerArray(new int[]{1, 2}), "atomic-int");
        map.put(new int[]{3, 4}, "regular-int");
        map.put(new AtomicLongArray(new long[]{5L, 6L}), "atomic-long");
        map.put(new long[]{7L, 8L}, "regular-long");
        map.put(new AtomicReferenceArray<>(new String[]{"a", "b"}), "atomic-ref");
        map.put(new String[]{"c", "d"}, "regular-string");
        
        // Verify all entries are distinct and retrievable
        assertEquals("atomic-int", map.get(new int[]{1, 2}));
        assertEquals("regular-int", map.get(new int[]{3, 4}));
        assertEquals("atomic-long", map.get(new long[]{5L, 6L}));
        assertEquals("regular-long", map.get(new long[]{7L, 8L}));
        assertEquals("atomic-ref", map.get(new Object[]{"a", "b"}));
        assertEquals("regular-string", map.get(new String[]{"c", "d"}));
        
        // Verify size
        assertEquals(6, map.size());
    }
    
    @Test
    void testAtomicArrayRemoval() {
        // Add entry with atomic array key
        AtomicIntegerArray atomicKey = new AtomicIntegerArray(new int[]{10, 20, 30});
        map.put(atomicKey, "to-remove");
        assertEquals(1, map.size());
        assertEquals("to-remove", map.get(atomicKey));
        
        // Remove using regular array
        int[] regularKey = new int[]{10, 20, 30};
        String removed = map.remove(regularKey);
        assertEquals("to-remove", removed);
        assertEquals(0, map.size());
        
        // Verify it's gone
        assertNull(map.get(atomicKey));
        assertNull(map.get(regularKey));
    }
    
    @Test
    void testAtomicArrayContainsKey() {
        // Add entry with atomic array key
        AtomicLongArray atomicKey = new AtomicLongArray(new long[]{100L, 200L});
        map.put(atomicKey, "test-value");
        
        // Check containsKey with both atomic and regular arrays
        assertTrue(map.containsKey(atomicKey));
        assertTrue(map.containsKey(new long[]{100L, 200L}));
        assertTrue(map.containsKey(new AtomicLongArray(new long[]{100L, 200L})));
        
        // Check for non-existent key
        assertFalse(map.containsKey(new AtomicLongArray(new long[]{100L, 201L})));
        assertFalse(map.containsKey(new long[]{100L, 201L}));
    }
    
    @Test
    @org.junit.jupiter.api.Disabled("Nested atomic arrays with flatten need deep conversion support")
    void testAtomicArrayWithFlattenDimensions() {
        // Test with flatten dimensions enabled
        MultiKeyMap<String> flatMap = MultiKeyMap.<String>builder()
                .flattenDimensions(true)
                .build();
        
        // Create nested structure with atomic arrays
        AtomicReferenceArray<Object> outer = new AtomicReferenceArray<>(2);
        outer.set(0, new AtomicIntegerArray(new int[]{1, 2}));
        outer.set(1, new AtomicIntegerArray(new int[]{3, 4}));
        
        flatMap.put(outer, "flattened");
        
        // With flatten, the AtomicReferenceArray is converted to Object[],
        // but the inner AtomicIntegerArrays remain as-is
        Object[] flattened = new Object[]{
            new AtomicIntegerArray(new int[]{1, 2}), 
            new AtomicIntegerArray(new int[]{3, 4})
        };
        assertEquals("flattened", flatMap.get(flattened));
        
        // Also test retrieval with another atomic reference array
        AtomicReferenceArray<Object> outer2 = new AtomicReferenceArray<>(2);
        outer2.set(0, new AtomicIntegerArray(new int[]{1, 2}));
        outer2.set(1, new AtomicIntegerArray(new int[]{3, 4}));
        assertEquals("flattened", flatMap.get(outer2));
    }
    
    @Test
    void testPerformanceWithLargeAtomicArrays() {
        // Create large atomic arrays to test performance
        int size = 1000;
        AtomicIntegerArray largeAtomic = new AtomicIntegerArray(size);
        int[] largeRegular = new int[size];
        
        for (int i = 0; i < size; i++) {
            largeAtomic.set(i, i);
            largeRegular[i] = i;
        }
        
        // Store with atomic array
        long startTime = System.nanoTime();
        map.put(largeAtomic, "large-value");
        long putTime = System.nanoTime() - startTime;
        
        // Retrieve with regular array
        startTime = System.nanoTime();
        assertEquals("large-value", map.get(largeRegular));
        long getTime = System.nanoTime() - startTime;
        
        // Verify reasonable performance (both operations should be under 10ms)
        assertTrue(putTime < 10_000_000, "Put operation took too long: " + putTime + " ns");
        assertTrue(getTime < 10_000_000, "Get operation took too long: " + getTime + " ns");
    }
}