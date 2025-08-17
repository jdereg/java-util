package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for edge cases and untested paths in MultiKeyMap.
 * Focuses on boundary conditions, error paths, and special configurations.
 */
class MultiKeyMapEdgeCasesTest {

    @Test
    void testSimpleKeysBehavior() {
        // Test with default configuration
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // With default config, nested structures should work
        Object[] nestedKey = {Arrays.asList("a", "b"), "c"};
        map.put(nestedKey, "nested_value");
        assertEquals("nested_value", map.get(nestedKey));
        
        // Test that optimization paths are used for simple keys
        Object[] key1 = {"simple1"};
        Object[] key2 = {"s1", "s2"};
        Object[] key3 = {"s1", "s2", "s3"};
        
        map.put(key1, "v1");
        map.put(key2, "v2");
        map.put(key3, "v3");
        
        assertEquals("v1", map.get(key1));
        assertEquals("v2", map.get(key2));
        assertEquals("v3", map.get(key3));
    }

    @Test
    void testCopyConstructor() {
        // Create source map
        MultiKeyMap<String> source = new MultiKeyMap<>(100);
        
        source.put(new Object[]{"key1"}, "value1");
        source.put(new Object[]{"k1", "k2"}, "value2");
        
        // Copy constructor should preserve content
        MultiKeyMap<String> copy = new MultiKeyMap<>(source);
        
        assertEquals("value1", copy.get(new Object[]{"key1"}));
        assertEquals("value2", copy.get(new Object[]{"k1", "k2"}));
    }

    @Test
    void testLargeArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test arrays larger than 10 elements (should use generic path)
        Object[] largeKey = new Object[15];
        for (int i = 0; i < 15; i++) {
            largeKey[i] = "element" + i;
        }
        
        map.put(largeKey, "large_value");
        assertEquals("large_value", map.get(largeKey));
        
        // Test very large array
        Object[] veryLargeKey = new Object[100];
        for (int i = 0; i < 100; i++) {
            veryLargeKey[i] = i;
        }
        
        map.put(veryLargeKey, "very_large_value");
        assertEquals("very_large_value", map.get(veryLargeKey));
    }

    @Test
    void testLargeCollections() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test collections larger than 10 elements
        List<String> largeList = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            largeList.add("item" + i);
        }
        
        map.put(largeList, "large_list_value");
        assertEquals("large_list_value", map.get(largeList));
        
        // Test with LinkedList (non-RandomAccess)
        LinkedList<Integer> linkedLarge = new LinkedList<>();
        for (int i = 0; i < 20; i++) {
            linkedLarge.add(i);
        }
        
        map.put(linkedLarge, "linked_large");
        assertEquals("linked_large", map.get(linkedLarge));
    }

    @Test
    void testAtomicNumberTypesWithValueBasedEquality() {
        // AtomicInteger and AtomicLong extend Number
        // Default is now value-based equality
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        AtomicInteger atomic1 = new AtomicInteger(42);
        AtomicLong atomic2 = new AtomicLong(42L);
        Integer regular1 = 42;
        Long regular2 = 42L;
        
        // All should be equal in value-based mode (default)
        map.put(new Object[]{atomic1}, "atomic_int");
        assertEquals("atomic_int", map.get(new Object[]{regular1}));
        assertEquals("atomic_int", map.get(new Object[]{atomic2}));
        assertEquals("atomic_int", map.get(new Object[]{regular2}));
    }

    @Test
    void testAtomicNumberTypesDistinct() {
        // Test that we can store different atomic types with different values
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        AtomicInteger atomic1 = new AtomicInteger(42);
        AtomicLong atomic2 = new AtomicLong(43L);
        Integer regular1 = 44;
        Long regular2 = 45L;
        
        map.put(new Object[]{atomic1}, "atomic_int");
        map.put(new Object[]{atomic2}, "atomic_long");
        map.put(new Object[]{regular1}, "regular_int");
        map.put(new Object[]{regular2}, "regular_long");
        
        // Each should retrieve its own value
        assertEquals("atomic_int", map.get(new Object[]{new AtomicInteger(42)}));
        assertEquals("atomic_long", map.get(new Object[]{new AtomicLong(43L)}));
        assertEquals("regular_int", map.get(new Object[]{44}));
        assertEquals("regular_long", map.get(new Object[]{45L}));
    }

    @Test
    void testEmptyArrayAndCollectionKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Empty array as key
        Object[] emptyArray = {};
        map.put(emptyArray, "empty_array");
        assertEquals("empty_array", map.get(emptyArray));
        
        // Empty collection as key
        List<String> emptyList = new ArrayList<>();
        map.put(emptyList, "empty_list");
        assertEquals("empty_list", map.get(emptyList));
        
        // Empty set as key
        Set<String> emptySet = new HashSet<>();
        map.put(emptySet, "empty_set");
        assertEquals("empty_set", map.get(emptySet));
    }

    @Test
    void testSingleNullKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Single null as key (not in array)
        map.put((Object) null, "null_value");
        assertEquals("null_value", map.get((Object) null));
        
        // Verify it's different from array containing null
        Object[] arrayWithNull = {null};
        map.put(arrayWithNull, "array_null");
        assertEquals("array_null", map.get(arrayWithNull));
        
        // Both should coexist
        assertEquals("null_value", map.get((Object) null));
        assertEquals("array_null", map.get(arrayWithNull));
    }

    @Test
    void testVeryDeepNesting() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create very deep nesting
        Object deepest = "leaf";
        for (int i = 0; i < 10; i++) {
            deepest = new Object[]{deepest};
        }
        
        map.put(deepest, "very_deep");
        assertEquals("very_deep", map.get(deepest));
    }

    @Test
    void testMixedPrimitiveArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Different primitive array types with same numeric values
        int[] intArray = {1, 2, 3};
        double[] doubleArray = {1.0, 2.0, 3.0};
        byte[] byteArray = {1, 2, 3};
        
        map.put(intArray, "int_array");
        map.put(doubleArray, "double_array");
        map.put(byteArray, "byte_array");
        
        // With value-based equality, all three arrays with values {1,2,3} are considered equal
        // The last put wins (byte_array)
        assertEquals("byte_array", map.get(intArray)); // byte overwrote int
        assertEquals("byte_array", map.get(doubleArray)); // byte overwrote double too
        assertEquals("byte_array", map.get(byteArray));
        
        // Test with different values to ensure they're distinct
        int[] intArray2 = {4, 5, 6};
        map.put(intArray2, "int_array2");
        assertEquals("int_array2", map.get(intArray2));
        assertNotEquals(map.get(intArray), map.get(intArray2));
    }

    @Test
    void testSpecialFloatingPointValues() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test NaN handling
        Double nanDouble = Double.NaN;
        Float nanFloat = Float.NaN;
        
        map.put(new Object[]{nanDouble}, "double_nan");
        map.put(new Object[]{nanFloat}, "float_nan");
        
        // In value-based equality (default), NaN equals NaN, and float/double NaN are considered equal
        // Both keys map to the same entry because Float.NaN and Double.NaN are value-equal
        assertEquals("float_nan", map.get(new Object[]{Double.NaN})); // float_nan overwrote double_nan
        assertEquals("float_nan", map.get(new Object[]{Float.NaN}));
        
        // Test infinity values
        map.put(new Object[]{Double.POSITIVE_INFINITY}, "pos_inf");
        map.put(new Object[]{Double.NEGATIVE_INFINITY}, "neg_inf");
        
        assertEquals("pos_inf", map.get(new Object[]{Double.POSITIVE_INFINITY}));
        assertEquals("neg_inf", map.get(new Object[]{Double.NEGATIVE_INFINITY}));
    }

    @Test
    void testCollectionWithNullElements() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // List with multiple nulls
        List<String> listWithNulls = Arrays.asList(null, "middle", null);
        map.put(listWithNulls, "list_nulls");
        assertEquals("list_nulls", map.get(listWithNulls));
        
        // Set with null (HashSet allows one null)
        Set<String> setWithNull = new HashSet<>();
        setWithNull.add(null);
        setWithNull.add("element");
        map.put(setWithNull, "set_null");
        assertEquals("set_null", map.get(setWithNull));
    }

    @Test
    void testRandomAccessOptimization() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // ArrayList implements RandomAccess
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add("a");
        arrayList.add("b");
        
        // Vector also implements RandomAccess
        Vector<String> vector = new Vector<>();
        vector.add("a");
        vector.add("b");
        
        // LinkedList does NOT implement RandomAccess
        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add("a");
        linkedList.add("b");
        
        map.put(arrayList, "arraylist");
        map.put(vector, "vector");
        map.put(linkedList, "linkedlist");
        
        // With value-based equality (default), lists with same content are equal
        // The last one put will overwrite the previous ones
        assertEquals("linkedlist", map.get(arrayList)); // all have same content
        assertEquals("linkedlist", map.get(vector));    // all have same content
        assertEquals("linkedlist", map.get(linkedList));
    }

    @Test
    void testCustomCollectionImplementations() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Custom collection that implements RandomAccess
        class CustomRandomAccessList<E> extends ArrayList<E> implements RandomAccess {
            CustomRandomAccessList(Collection<? extends E> c) {
                super(c);
            }
        }
        
        CustomRandomAccessList<String> customList = new CustomRandomAccessList<>(Arrays.asList("custom", "list"));
        map.put(customList, "custom");
        assertEquals("custom", map.get(customList));
        
        // Queue implementation
        Queue<String> queue = new LinkedList<>();
        queue.add("queue1");
        queue.add("queue2");
        map.put(queue, "queue");
        assertEquals("queue", map.get(queue));
        
        // Deque implementation
        Deque<String> deque = new ArrayDeque<>();
        deque.add("deque1");
        deque.add("deque2");
        map.put(deque, "deque");
        assertEquals("deque", map.get(deque));
    }

    @Test
    void testConcurrentCollections() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // ConcurrentLinkedQueue
        ConcurrentLinkedQueue<String> concurrentQueue = new ConcurrentLinkedQueue<>();
        concurrentQueue.add("concurrent1");
        concurrentQueue.add("concurrent2");
        map.put(concurrentQueue, "concurrent_queue");
        assertEquals("concurrent_queue", map.get(concurrentQueue));
        
        // CopyOnWriteArrayList (implements RandomAccess)
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
        cowList.add("cow1");
        cowList.add("cow2");
        map.put(cowList, "cow_list");
        assertEquals("cow_list", map.get(cowList));
        
        // CopyOnWriteArraySet
        CopyOnWriteArraySet<String> cowSet = new CopyOnWriteArraySet<>();
        cowSet.add("cow_set1");
        cowSet.add("cow_set2");
        map.put(cowSet, "cow_set");
        assertEquals("cow_set", map.get(cowSet));
    }

    @Test
    void testBoundaryCapacities() {
        // Test with minimum capacity
        MultiKeyMap<String> minMap = new MultiKeyMap<>(1);
        
        minMap.put(new Object[]{"key"}, "value");
        assertEquals("value", minMap.get(new Object[]{"key"}));
        
        // Test with large capacity
        MultiKeyMap<String> largeMap = new MultiKeyMap<>(10000);
        
        for (int i = 0; i < 100; i++) {
            largeMap.put(new Object[]{"key" + i}, "value" + i);
        }
        
        assertEquals("value50", largeMap.get(new Object[]{"key50"}));
    }

    @Test
    void testCharacterArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // char[] arrays
        char[] chars1 = {'a', 'b', 'c'};
        char[] chars2 = {'a', 'b', 'c'};
        char[] chars3 = {'x', 'y', 'z'};
        
        map.put(chars1, "abc");
        map.put(chars3, "xyz");
        
        // With value-based equality, identical char arrays should match
        assertEquals("abc", map.get(chars2));
        assertEquals("xyz", map.get(chars3));
    }

    @Test
    void testBooleanArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // boolean[] arrays
        boolean[] bools1 = {true, false, true};
        boolean[] bools2 = {true, false, true};
        boolean[] bools3 = {false, true, false};
        
        map.put(bools1, "tft");
        map.put(bools3, "ftf");
        
        // With value-based equality, identical boolean arrays should match
        assertEquals("tft", map.get(bools2));
        assertEquals("ftf", map.get(bools3));
    }

    @Test
    void testMixedNullHandling() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Mix of nulls and non-nulls in different positions
        Object[] key1 = {null, "a", null, "b", null};
        Object[] key2 = {"a", null, "b", null, "c"};
        Object[] key3 = {null, null, null};
        
        map.put(key1, "pattern1");
        map.put(key2, "pattern2");
        map.put(key3, "all_nulls");
        
        assertEquals("pattern1", map.get(key1));
        assertEquals("pattern2", map.get(key2));
        assertEquals("all_nulls", map.get(key3));
        
        // Verify they're distinct
        assertNotEquals(map.get(key1), map.get(key2));
        assertNotEquals(map.get(key1), map.get(key3));
    }

    @Test
    void testPerformanceOptimizationBoundaries() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test exact boundary sizes for optimization paths
        // Size 4 - just beyond optimized unrolled methods
        Object[] size4 = {"a", "b", "c", "d"};
        map.put(size4, "size4");
        assertEquals("size4", map.get(size4));
        
        // Size 5 - still in small generic path
        Object[] size5 = {"a", "b", "c", "d", "e"};
        map.put(size5, "size5");
        assertEquals("size5", map.get(size5));
        
        // Size 11 - just beyond flattenObjectArrayN range
        Object[] size11 = new Object[11];
        for (int i = 0; i < 11; i++) {
            size11[i] = "elem" + i;
        }
        map.put(size11, "size11");
        assertEquals("size11", map.get(size11));
    }
}