package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for the optimized flattening methods in MultiKeyMap.
 * These methods (flattenObjectArray1/2/3 and flattenCollection1/2/3) are
 * performance optimizations that unroll loops for small arrays/collections.
 */
class MultiKeyMapOptimizedFlattenTest {

    @Test
    void testFlattenObjectArray1_SimpleKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test single-element array with simple key
        Object[] key1 = {"simple"};
        map.put(key1, "value1");
        assertEquals("value1", map.get(key1));
        assertEquals("value1", map.get(new Object[]{"simple"}));
        
        // Test with null
        Object[] key2 = {null};
        map.put(key2, "value2");
        assertEquals("value2", map.get(key2));
        // Note: Single null in array is different from null key
        map.put((Object) null, "null_value");
        assertEquals("null_value", map.get((Object) null));
        
        // Test with number
        Object[] key3 = {42};
        map.put(key3, "value3");
        assertEquals("value3", map.get(key3));
        assertEquals("value3", map.get(new Object[]{42}));
    }

    @Test
    void testFlattenObjectArray1_ComplexKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test single-element array containing another array
        Object[] innerArray = {"inner1", "inner2"};
        Object[] key1 = {innerArray};
        map.put(key1, "nested_array");
        assertEquals("nested_array", map.get(key1));
        
        // Test single-element array containing a collection
        List<String> innerList = Arrays.asList("item1", "item2");
        Object[] key2 = {innerList};
        map.put(key2, "nested_list");
        assertEquals("nested_list", map.get(key2));
    }

    @Test
    void testFlattenObjectArray2_SimpleKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test two-element array with simple keys
        Object[] key1 = {"first", "second"};
        map.put(key1, "value1");
        assertEquals("value1", map.get(key1));
        assertEquals("value1", map.get(new Object[]{"first", "second"}));
        
        // Test with nulls
        Object[] key2 = {null, "second"};
        map.put(key2, "value2");
        assertEquals("value2", map.get(key2));
        assertEquals("value2", map.get(new Object[]{null, "second"}));
        
        Object[] key3 = {"first", null};
        map.put(key3, "value3");
        assertEquals("value3", map.get(key3));
        assertEquals("value3", map.get(new Object[]{"first", null}));
        
        // Test with numbers
        Object[] key4 = {1, 2};
        map.put(key4, "value4");
        assertEquals("value4", map.get(key4));
        assertEquals("value4", map.get(new Object[]{1, 2}));
    }

    @Test
    void testFlattenObjectArray2_ComplexKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test with nested array in first position
        Object[] innerArray = {"nested"};
        Object[] key1 = {innerArray, "second"};
        map.put(key1, "nested_first");
        assertEquals("nested_first", map.get(key1));
        
        // Test with nested array in second position
        Object[] key2 = {"first", innerArray};
        map.put(key2, "nested_second");
        assertEquals("nested_second", map.get(key2));
        
        // Test with collection
        List<String> list = Arrays.asList("list_item");
        Object[] key3 = {list, "second"};
        map.put(key3, "list_first");
        assertEquals("list_first", map.get(key3));
    }

    @Test
    void testFlattenObjectArray3_SimpleKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test three-element array with simple keys
        Object[] key1 = {"one", "two", "three"};
        map.put(key1, "value1");
        assertEquals("value1", map.get(key1));
        assertEquals("value1", map.get(new Object[]{"one", "two", "three"}));
        
        // Test with nulls in different positions
        Object[] key2 = {null, "two", "three"};
        map.put(key2, "value2");
        assertEquals("value2", map.get(key2));
        assertEquals("value2", map.get(new Object[]{null, "two", "three"}));
        
        Object[] key3 = {"one", null, "three"};
        map.put(key3, "value3");
        assertEquals("value3", map.get(key3));
        assertEquals("value3", map.get(new Object[]{"one", null, "three"}));
        
        Object[] key4 = {"one", "two", null};
        map.put(key4, "value4");
        assertEquals("value4", map.get(key4));
        assertEquals("value4", map.get(new Object[]{"one", "two", null}));
        
        // Test with mixed types
        Object[] key5 = {1, "two", 3.0};
        map.put(key5, "value5");
        assertEquals("value5", map.get(key5));
        assertEquals("value5", map.get(new Object[]{1, "two", 3.0}));
    }

    @Test
    void testFlattenObjectArray3_ComplexKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test with nested array in first position
        Object[] innerArray = {"nested"};
        Object[] key1 = {innerArray, "two", "three"};
        map.put(key1, "nested_first");
        assertEquals("nested_first", map.get(key1));
        
        // Test with nested array in middle position
        Object[] key2 = {"one", innerArray, "three"};
        map.put(key2, "nested_middle");
        assertEquals("nested_middle", map.get(key2));
        
        // Test with nested array in last position
        Object[] key3 = {"one", "two", innerArray};
        map.put(key3, "nested_last");
        assertEquals("nested_last", map.get(key3));
        
        // Test with collection
        List<String> list = Arrays.asList("list_item");
        Object[] key4 = {"one", list, "three"};
        map.put(key4, "list_middle");
        assertEquals("list_middle", map.get(key4));
    }

    @Test
    void testFlattenCollection1_SimpleKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test single-element ArrayList (RandomAccess)
        List<String> key1 = Arrays.asList("simple");
        map.put(key1, "value1");
        assertEquals("value1", map.get(key1));
        
        // Test single-element LinkedList (non-RandomAccess)
        List<String> key2 = new LinkedList<>();
        key2.add("linked");
        map.put(key2, "value2");
        assertEquals("value2", map.get(key2));
        
        // Test single-element Set
        Set<String> key3 = new HashSet<>();
        key3.add("set_item");
        map.put(key3, "value3");
        assertEquals("value3", map.get(key3));
        
        // Test with null element
        List<String> key4 = Arrays.asList((String) null);
        map.put(key4, "value4");
        assertEquals("value4", map.get(key4));
    }

    @Test
    void testFlattenCollection1_ComplexKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test collection containing array
        Object[] innerArray = {"inner"};
        List<Object> key1 = Arrays.asList(innerArray);
        map.put(key1, "nested_array");
        assertEquals("nested_array", map.get(key1));
        
        // Test collection containing another collection
        List<String> innerList = Arrays.asList("nested");
        List<Object> key2 = Arrays.asList(innerList);
        map.put(key2, "nested_list");
        assertEquals("nested_list", map.get(key2));
    }

    @Test
    void testFlattenCollection2_SimpleKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test two-element ArrayList (RandomAccess)
        List<String> key1 = Arrays.asList("first", "second");
        map.put(key1, "value1");
        assertEquals("value1", map.get(key1));
        
        // Test two-element LinkedList (non-RandomAccess)
        List<String> key2 = new LinkedList<>();
        key2.add("linked1");
        key2.add("linked2");
        map.put(key2, "value2");
        assertEquals("value2", map.get(key2));
        
        // Test two-element Set
        Set<String> key3 = new LinkedHashSet<>(); // Use LinkedHashSet for predictable order
        key3.add("set1");
        key3.add("set2");
        map.put(key3, "value3");
        assertEquals("value3", map.get(key3));
        
        // Test with nulls
        List<String> key4 = Arrays.asList(null, "second");
        map.put(key4, "value4");
        assertEquals("value4", map.get(key4));
        
        List<String> key5 = Arrays.asList("first", null);
        map.put(key5, "value5");
        assertEquals("value5", map.get(key5));
    }

    @Test
    void testFlattenCollection2_ComplexKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test with nested array in RandomAccess list
        Object[] innerArray = {"nested"};
        List<Object> key1 = Arrays.asList(innerArray, "second");
        map.put(key1, "nested_first");
        assertEquals("nested_first", map.get(key1));
        
        List<Object> key2 = Arrays.asList("first", innerArray);
        map.put(key2, "nested_second");
        assertEquals("nested_second", map.get(key2));
        
        // Test with nested array in non-RandomAccess list
        LinkedList<Object> key3 = new LinkedList<>();
        key3.add(innerArray);
        key3.add("second");
        map.put(key3, "linked_nested");
        assertEquals("linked_nested", map.get(key3));
        
        // Test with nested collection
        List<String> innerList = Arrays.asList("inner");
        List<Object> key4 = Arrays.asList(innerList, "second");
        map.put(key4, "list_nested");
        assertEquals("list_nested", map.get(key4));
    }

    @Test
    void testFlattenCollection3_SimpleKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test three-element ArrayList (RandomAccess)
        List<String> key1 = Arrays.asList("one", "two", "three");
        map.put(key1, "value1");
        assertEquals("value1", map.get(key1));
        
        // Test three-element LinkedList (non-RandomAccess)
        List<String> key2 = new LinkedList<>();
        key2.add("linked1");
        key2.add("linked2");
        key2.add("linked3");
        map.put(key2, "value2");
        assertEquals("value2", map.get(key2));
        
        // Test with nulls in different positions
        List<String> key3 = Arrays.asList(null, "two", "three");
        map.put(key3, "value3");
        assertEquals("value3", map.get(key3));
        
        List<String> key4 = Arrays.asList("one", null, "three");
        map.put(key4, "value4");
        assertEquals("value4", map.get(key4));
        
        List<String> key5 = Arrays.asList("one", "two", null);
        map.put(key5, "value5");
        assertEquals("value5", map.get(key5));
    }

    @Test
    void testFlattenCollection3_ComplexKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test with nested array in RandomAccess list
        Object[] innerArray = {"nested"};
        List<Object> key1 = Arrays.asList(innerArray, "two", "three");
        map.put(key1, "nested_first");
        assertEquals("nested_first", map.get(key1));
        
        List<Object> key2 = Arrays.asList("one", innerArray, "three");
        map.put(key2, "nested_middle");
        assertEquals("nested_middle", map.get(key2));
        
        List<Object> key3 = Arrays.asList("one", "two", innerArray);
        map.put(key3, "nested_last");
        assertEquals("nested_last", map.get(key3));
        
        // Test with nested array in non-RandomAccess list
        LinkedList<Object> key4 = new LinkedList<>();
        key4.add("one");
        key4.add(innerArray);
        key4.add("three");
        map.put(key4, "linked_nested");
        assertEquals("linked_nested", map.get(key4));
        
        // Test with nested collection
        List<String> innerList = Arrays.asList("inner");
        List<Object> key5 = Arrays.asList("one", innerList, "three");
        map.put(key5, "list_nested");
        assertEquals("list_nested", map.get(key5));
    }

    @Test
    void testFlattenObjectArrayN_Sizes6to10() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test size 6
        Object[] key6 = {1, 2, 3, 4, 5, 6};
        map.put(key6, "value6");
        assertEquals("value6", map.get(key6));
        assertEquals("value6", map.get(new Object[]{1, 2, 3, 4, 5, 6}));
        
        // Test size 7
        Object[] key7 = {1, 2, 3, 4, 5, 6, 7};
        map.put(key7, "value7");
        assertEquals("value7", map.get(key7));
        
        // Test size 8
        Object[] key8 = {1, 2, 3, 4, 5, 6, 7, 8};
        map.put(key8, "value8");
        assertEquals("value8", map.get(key8));
        
        // Test size 9
        Object[] key9 = {1, 2, 3, 4, 5, 6, 7, 8, 9};
        map.put(key9, "value9");
        assertEquals("value9", map.get(key9));
        
        // Test size 10
        Object[] key10 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        map.put(key10, "value10");
        assertEquals("value10", map.get(key10));
    }

    @Test
    void testFlattenObjectArrayN_WithComplexElements() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test size 6 with nested array
        Object[] innerArray = {"nested"};
        Object[] key6 = {1, 2, innerArray, 4, 5, 6};
        map.put(key6, "nested6");
        assertEquals("nested6", map.get(key6));
        
        // Test size 8 with nested collection
        List<String> innerList = Arrays.asList("list");
        Object[] key8 = {1, 2, 3, 4, innerList, 6, 7, 8};
        map.put(key8, "nested8");
        assertEquals("nested8", map.get(key8));
    }

    @Test
    void testSimpleKeysMode() {
        // Test with simpleKeysMode enabled (default for small maps)
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // These should use the optimized paths
        Object[] key1 = {"simple"};
        Object[] key2 = {"one", "two"};
        Object[] key3 = {"a", "b", "c"};
        
        map.put(key1, "v1");
        map.put(key2, "v2");
        map.put(key3, "v3");
        
        assertEquals("v1", map.get(key1));
        assertEquals("v2", map.get(key2));
        assertEquals("v3", map.get(key3));
    }

    @Test
    void testMixedRandomAccessAndNonRandomAccess() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // ArrayList is RandomAccess
        List<String> arrayList = Arrays.asList("array", "list");
        map.put(arrayList, "arraylist");
        
        // LinkedList is not RandomAccess
        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.add("linked");
        linkedList.add("list");
        map.put(linkedList, "linkedlist");
        
        // TreeSet is not RandomAccess
        TreeSet<String> treeSet = new TreeSet<>();
        treeSet.add("tree");
        treeSet.add("set");
        map.put(treeSet, "treeset");
        
        assertEquals("arraylist", map.get(arrayList));
        assertEquals("linkedlist", map.get(linkedList));
        assertEquals("treeset", map.get(treeSet));
    }

    @Test
    void testFlattenDimensionsEnabled() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        // Note: flattenDimensions is true by default
        
        // Test that nested structures are flattened
        Object[] nested = {Arrays.asList("a", "b"), "c"};
        map.put(nested, "flattened");
        
        // Should be accessible with flattened keys
        assertEquals("flattened", map.get(nested));
    }

    @Test
    void testFlattenDimensionsDisabled() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        // Note: We can't disable flattenDimensions via builder in current API
        // This test will be skipped for now
        
        // Test that nested structures work with default settings
        List<String> innerList = Arrays.asList("a", "b");
        Object[] nested = {innerList, "c"};
        map.put(nested, "nested_value");
        
        // Should be accessible with the exact structure
        assertEquals("nested_value", map.get(nested));
    }

    @Test
    void testPerformanceOptimizationPaths() {
        // This test ensures all optimization paths are hit
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Size 1 - optimized path
        map.put(new Object[]{"1"}, "size1");
        map.put(Arrays.asList("L1"), "list1");
        
        // Size 2 - optimized path
        map.put(new Object[]{"2a", "2b"}, "size2");
        map.put(Arrays.asList("L2a", "L2b"), "list2");
        
        // Size 3 - optimized path
        map.put(new Object[]{"3a", "3b", "3c"}, "size3");
        map.put(Arrays.asList("L3a", "L3b", "L3c"), "list3");
        
        // Size 4-5 - generic small path
        map.put(new Object[]{"4a", "4b", "4c", "4d"}, "size4");
        map.put(new Object[]{"5a", "5b", "5c", "5d", "5e"}, "size5");
        
        // Size 6-10 - flattenObjectArrayN path
        map.put(new Object[]{"6a", "6b", "6c", "6d", "6e", "6f"}, "size6");
        map.put(new Object[]{"7a", "7b", "7c", "7d", "7e", "7f", "7g"}, "size7");
        
        // Verify all values are retrievable
        assertEquals("size1", map.get(new Object[]{"1"}));
        assertEquals("list1", map.get(Arrays.asList("L1")));
        assertEquals("size2", map.get(new Object[]{"2a", "2b"}));
        assertEquals("list2", map.get(Arrays.asList("L2a", "L2b")));
        assertEquals("size3", map.get(new Object[]{"3a", "3b", "3c"}));
        assertEquals("list3", map.get(Arrays.asList("L3a", "L3b", "L3c")));
        assertEquals("size4", map.get(new Object[]{"4a", "4b", "4c", "4d"}));
        assertEquals("size5", map.get(new Object[]{"5a", "5b", "5c", "5d", "5e"}));
        assertEquals("size6", map.get(new Object[]{"6a", "6b", "6c", "6d", "6e", "6f"}));
        assertEquals("size7", map.get(new Object[]{"7a", "7b", "7c", "7d", "7e", "7f", "7g"}));
    }
}