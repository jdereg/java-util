package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class specifically for simpleKeysMode optimization paths in MultiKeyMap.
 * This mode assumes keys contain no nested structures and enables aggressive optimizations.
 * Tests target the specific uncovered lines in the flattenObjectArray and flattenCollection methods.
 */
class MultiKeyMapSimpleKeysModeTest {

    @Test
    void testSimpleKeysMode_FlattenObjectArray1_UnrolledPath() {
        // Create map with simpleKeysMode enabled to hit the optimized path
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Test size 1 arrays - should use flattenObjectArray1 optimization
        // This specifically tests lines 1107-1109 in simpleKeysMode
        Object[] key1 = {"simple1"};
        map.put(key1, "value1");
        assertEquals("value1", map.get(key1));
        
        Object[] key2 = {42};
        map.put(key2, "value2");
        assertEquals("value2", map.get(key2));
        
        Object[] key3 = {null};
        map.put(key3, "value3");
        assertEquals("value3", map.get(key3));
        
        // Even with complex element, simpleKeysMode skips the check (line 1108)
        Object[] key4 = {new Object[]{1, 2}};  // nested array
        map.put(key4, "value4");
        assertEquals("value4", map.get(key4));
    }

    @Test
    void testSimpleKeysMode_FlattenObjectArray2_UnrolledPath() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Test size 2 arrays - should use flattenObjectArray2 optimization
        // This specifically tests lines 1125-1128 in simpleKeysMode
        Object[] key1 = {"first", "second"};
        map.put(key1, "value1");
        assertEquals("value1", map.get(key1));
        
        Object[] key2 = {1, 2};
        map.put(key2, "value2");
        assertEquals("value2", map.get(key2));
        
        Object[] key3 = {null, "second"};
        map.put(key3, "value3");
        assertEquals("value3", map.get(key3));
        
        Object[] key4 = {"first", null};
        map.put(key4, "value4");
        assertEquals("value4", map.get(key4));
        
        // With simpleKeysMode, even nested structures are hashed directly (line 1126-1127)
        Object[] key5 = {Arrays.asList("a", "b"), "second"};
        map.put(key5, "value5");
        assertEquals("value5", map.get(key5));
    }

    @Test
    void testSimpleKeysMode_FlattenObjectArray3_UnrolledPath() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Test size 3 arrays - should use flattenObjectArray3 optimization
        // This specifically tests lines 1140-1144 in simpleKeysMode
        Object[] key1 = {"one", "two", "three"};
        map.put(key1, "value1");
        assertEquals("value1", map.get(key1));
        
        Object[] key2 = {1, 2, 3};
        map.put(key2, "value2");
        assertEquals("value2", map.get(key2));
        
        Object[] key3 = {null, null, null};
        map.put(key3, "value3");
        assertEquals("value3", map.get(key3));
        
        // With simpleKeysMode, nested structures don't trigger expansion (lines 1141-1143)
        Object[] key4 = {new int[]{1, 2}, "middle", Arrays.asList("x", "y")};
        map.put(key4, "value4");
        assertEquals("value4", map.get(key4));
    }

    @Test
    void testSimpleKeysMode_FlattenObjectArrayN_Size6() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Test size 6 - uses flattenObjectArrayN with simpleKeysMode path
        // This specifically tests lines 1258-1261 in simpleKeysMode
        Object[] key1 = {"a", "b", "c", "d", "e", "f"};
        map.put(key1, "value6");
        assertEquals("value6", map.get(key1));
        
        // With nulls
        Object[] key2 = {null, "b", null, "d", null, "f"};
        map.put(key2, "value6_nulls");
        assertEquals("value6_nulls", map.get(key2));
        
        // With numbers
        Object[] key3 = {1, 2, 3, 4, 5, 6};
        map.put(key3, "value6_nums");
        assertEquals("value6_nums", map.get(key3));
    }

    @Test
    void testSimpleKeysMode_FlattenObjectArrayN_Size7to10() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Size 7 - tests flattenObjectArrayN with simpleKeysMode
        Object[] key7 = new Object[7];
        for (int i = 0; i < 7; i++) key7[i] = "elem" + i;
        map.put(key7, "value7");
        assertEquals("value7", map.get(key7));
        
        // Size 8
        Object[] key8 = new Object[8];
        for (int i = 0; i < 8; i++) key8[i] = i;
        map.put(key8, "value8");
        assertEquals("value8", map.get(key8));
        
        // Size 9
        Object[] key9 = new Object[9];
        for (int i = 0; i < 9; i++) key9[i] = "s" + i;
        map.put(key9, "value9");
        assertEquals("value9", map.get(key9));
        
        // Size 10
        Object[] key10 = new Object[10];
        for (int i = 0; i < 10; i++) key10[i] = i * 10;
        map.put(key10, "value10");
        assertEquals("value10", map.get(key10));
    }

    @Test
    void testSimpleKeysMode_FlattenCollection1_RandomAccessPath() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // ArrayList (RandomAccess) - size 1
        // This tests lines 1157-1161 in simpleKeysMode for RandomAccess path
        List<String> list1 = Arrays.asList("single");
        map.put(list1, "list1");
        assertEquals("list1", map.get(list1));
        
        // With null - tests line 1161
        List<String> nullList = Arrays.asList((String) null);
        map.put(nullList, "null_list");
        assertEquals("null_list", map.get(nullList));
        
        // With nested structure (simpleKeysMode doesn't check) - line 1158
        List<Object> nestedList = Arrays.asList(Arrays.asList("nested"));
        map.put(nestedList, "nested_list");
        assertEquals("nested_list", map.get(nestedList));
    }

    @Test
    void testSimpleKeysMode_FlattenCollection1_NonRandomAccessPath() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // LinkedList (non-RandomAccess) - size 1
        // This tests lines 1166-1169 in simpleKeysMode for non-RandomAccess path
        LinkedList<String> linked1 = new LinkedList<>();
        linked1.add("single");
        map.put(linked1, "linked1");
        assertEquals("linked1", map.get(linked1));
        
        // HashSet - size 1 (also non-RandomAccess)
        Set<String> set1 = new HashSet<>();
        set1.add("single");
        map.put(set1, "set1");
        assertEquals("set1", map.get(set1));
    }

    @Test
    void testSimpleKeysMode_FlattenCollection2_RandomAccessPath() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // ArrayList (RandomAccess) - size 2
        // This tests lines 1181-1193 in simpleKeysMode for RandomAccess path
        List<String> list2 = Arrays.asList("first", "second");
        map.put(list2, "list2");
        assertEquals("list2", map.get(list2));
        
        // With nulls - tests hash computation lines 1191-1192
        List<String> nullList = Arrays.asList(null, "second");
        map.put(nullList, "null_list2");
        assertEquals("null_list2", map.get(nullList));
        
        // With nested (simpleKeysMode processes as-is) - lines 1186-1188 skipped
        List<Object> nestedList = Arrays.asList(new int[]{1, 2}, "second");
        map.put(nestedList, "nested_list2");
        assertEquals("nested_list2", map.get(nestedList));
    }

    @Test
    void testSimpleKeysMode_FlattenCollection2_NonRandomAccessPath() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // LinkedList (non-RandomAccess) - size 2
        // This tests lines 1196-1209 in simpleKeysMode for non-RandomAccess path
        LinkedList<String> linked2 = new LinkedList<>();
        linked2.add("first");
        linked2.add("second");
        map.put(linked2, "linked2");
        assertEquals("linked2", map.get(linked2));
        
        // TreeSet (non-RandomAccess) - size 2
        TreeSet<String> treeSet2 = new TreeSet<>();
        treeSet2.add("a");
        treeSet2.add("b");
        map.put(treeSet2, "treeset2");
        assertEquals("treeset2", map.get(treeSet2));
    }

    @Test
    void testSimpleKeysMode_FlattenCollection3_RandomAccessPath() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // ArrayList (RandomAccess) - size 3
        // This tests lines 1214-1228 in simpleKeysMode for RandomAccess path
        List<String> list3 = Arrays.asList("one", "two", "three");
        map.put(list3, "list3");
        assertEquals("list3", map.get(list3));
        
        // With nulls - tests hash computation lines 1225-1227
        List<String> nullList = Arrays.asList(null, null, null);
        map.put(nullList, "null_list3");
        assertEquals("null_list3", map.get(nullList));
    }

    @Test
    void testSimpleKeysMode_FlattenCollection3_NonRandomAccessPath() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // LinkedList (non-RandomAccess) - size 3
        // This tests lines 1231-1247 in simpleKeysMode for non-RandomAccess path
        LinkedList<String> linked3 = new LinkedList<>();
        linked3.add("one");
        linked3.add("two");
        linked3.add("three");
        map.put(linked3, "linked3");
        assertEquals("linked3", map.get(linked3));
        
        // HashSet with 3 elements (ordering not guaranteed, but size is 3)
        Set<String> set3 = new LinkedHashSet<>(); // Use LinkedHashSet for predictable ordering
        set3.add("s1");
        set3.add("s2");
        set3.add("s3");
        map.put(set3, "set3");
        assertEquals("set3", map.get(set3));
    }

    @Test
    void testSimpleKeysMode_CollectionN_Size6to10() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Test collection sizes 6-10 that use flattenCollectionN with simpleKeysMode
        // This tests lines 1292-1295 in simpleKeysMode
        
        // Size 6
        List<Integer> list6 = Arrays.asList(1, 2, 3, 4, 5, 6);
        map.put(list6, "list6");
        assertEquals("list6", map.get(list6));
        
        // Size 7
        List<String> list7 = Arrays.asList("a", "b", "c", "d", "e", "f", "g");
        map.put(list7, "list7");
        assertEquals("list7", map.get(list7));
        
        // Size 8
        List<Integer> list8 = Arrays.asList(10, 20, 30, 40, 50, 60, 70, 80);
        map.put(list8, "list8");
        assertEquals("list8", map.get(list8));
        
        // Size 9
        Set<String> set9 = new LinkedHashSet<>();
        for (int i = 1; i <= 9; i++) set9.add("s" + i);
        map.put(set9, "set9");
        assertEquals("set9", map.get(set9));
        
        // Size 10
        List<Object> list10 = new ArrayList<>();
        for (int i = 0; i < 10; i++) list10.add(i);
        map.put(list10, "list10");
        assertEquals("list10", map.get(list10));
    }

    @Test
    void testSimpleKeysMode_ArraysSizes4and5() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Size 4 - tests generic small array path with simpleKeysMode
        Object[] key4 = {"a", "b", "c", "d"};
        map.put(key4, "value4");
        assertEquals("value4", map.get(key4));
        
        // Size 5
        Object[] key5 = {1, 2, 3, 4, 5};
        map.put(key5, "value5");
        assertEquals("value5", map.get(key5));
    }

    @Test
    void testSimpleKeysMode_CollectionsSizes4and5() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Size 4 - uses generic small collection path
        List<String> list4 = Arrays.asList("a", "b", "c", "d");
        map.put(list4, "list4");
        assertEquals("list4", map.get(list4));
        
        // Size 5 - uses generic small collection path
        List<String> list5 = Arrays.asList("1", "2", "3", "4", "5");
        map.put(list5, "list5");
        assertEquals("list5", map.get(list5));
    }

    @Test
    void testSimpleKeysMode_LargeArraysBeyondOptimized() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Size 11 - beyond optimized range, tests generic path
        Object[] key11 = new Object[11];
        for (int i = 0; i < 11; i++) key11[i] = "e" + i;
        map.put(key11, "value11");
        assertEquals("value11", map.get(key11));
        
        // Size 20
        Object[] key20 = new Object[20];
        for (int i = 0; i < 20; i++) key20[i] = i;
        map.put(key20, "value20");
        assertEquals("value20", map.get(key20));
    }

    @Test
    void testSimpleKeysMode_LargeCollectionsBeyondOptimized() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Size 11 - beyond optimized range
        List<String> list11 = new ArrayList<>();
        for (int i = 0; i < 11; i++) list11.add("item" + i);
        map.put(list11, "list11");
        assertEquals("list11", map.get(list11));
        
        // Size 50
        List<Integer> list50 = new ArrayList<>();
        for (int i = 0; i < 50; i++) list50.add(i);
        map.put(list50, "list50");
        assertEquals("list50", map.get(list50));
    }

    @Test
    void testSimpleKeysMode_VerifyNoExpansion() {
        // This test verifies that simpleKeysMode truly skips expansion
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Create a complex nested structure that would normally be expanded
        List<List<String>> deeplyNested = Arrays.asList(
            Arrays.asList("a", "b"),
            Arrays.asList("c", "d")
        );
        
        Object[] complexKey = {deeplyNested, "middle", new Object[]{1, 2, 3}};
        map.put(complexKey, "complex");
        
        // Should retrieve with exact same structure (no expansion happened)
        assertEquals("complex", map.get(complexKey));
        
        // Should NOT be retrievable with expanded form
        Object[] expandedForm = {Arrays.asList("a", "b"), Arrays.asList("c", "d"), "middle", 1, 2, 3};
        assertNull(map.get(expandedForm));
    }

    @Test
    void testSimpleKeysMode_MixedNullsInAllSizes() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Test null handling in all optimized paths
        
        // Size 1 with null
        map.put(new Object[]{null}, "null1");
        assertEquals("null1", map.get(new Object[]{null}));
        
        // Size 2 with nulls
        map.put(new Object[]{null, null}, "null2");
        assertEquals("null2", map.get(new Object[]{null, null}));
        
        // Size 3 with nulls
        map.put(new Object[]{null, "mid", null}, "null3");
        assertEquals("null3", map.get(new Object[]{null, "mid", null}));
        
        // Size 6 with nulls (flattenObjectArrayN path)
        map.put(new Object[]{null, null, null, null, null, null}, "null6");
        assertEquals("null6", map.get(new Object[]{null, null, null, null, null, null}));
    }

    @Test
    void testSimpleKeysMode_RandomAccessCollectionsWithNulls() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Test RandomAccess collections with nulls in all sizes
        
        // Size 1 ArrayList with null
        ArrayList<String> list1 = new ArrayList<>();
        list1.add(null);
        map.put(list1, "arraylist1_null");
        assertEquals("arraylist1_null", map.get(list1));
        
        // Size 2 ArrayList with nulls
        ArrayList<String> list2 = new ArrayList<>();
        list2.add(null);
        list2.add("second");
        map.put(list2, "arraylist2_null");
        assertEquals("arraylist2_null", map.get(list2));
        
        // Size 3 ArrayList with nulls
        ArrayList<String> list3 = new ArrayList<>();
        list3.add("first");
        list3.add(null);
        list3.add("third");
        map.put(list3, "arraylist3_null");
        assertEquals("arraylist3_null", map.get(list3));
    }

    @Test
    void testSimpleKeysMode_NonRandomAccessCollectionsAllSizes() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Test non-RandomAccess collections in all optimized sizes
        
        // LinkedList size 1
        LinkedList<String> ll1 = new LinkedList<>();
        ll1.add("item1");
        map.put(ll1, "ll1");
        assertEquals("ll1", map.get(ll1));
        
        // LinkedList size 2
        LinkedList<String> ll2 = new LinkedList<>();
        ll2.add("item1");
        ll2.add("item2");
        map.put(ll2, "ll2");
        assertEquals("ll2", map.get(ll2));
        
        // LinkedList size 3
        LinkedList<String> ll3 = new LinkedList<>();
        ll3.add("item1");
        ll3.add("item2");
        ll3.add("item3");
        map.put(ll3, "ll3");
        assertEquals("ll3", map.get(ll3));
        
        // TreeSet size 2
        TreeSet<Integer> ts2 = new TreeSet<>();
        ts2.add(1);
        ts2.add(2);
        map.put(ts2, "ts2");
        assertEquals("ts2", map.get(ts2));
    }

    @Test
    void testSimpleKeysMode_AllSizesComprehensive() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();
        
        // Test all sizes 1-15 to ensure all code paths are hit
        for (int size = 1; size <= 15; size++) {
            // Test arrays
            Object[] arrayKey = new Object[size];
            for (int i = 0; i < size; i++) {
                arrayKey[i] = "arr_elem" + i;
            }
            map.put(arrayKey, "array" + size);
            assertEquals("array" + size, map.get(arrayKey));
            
            // Test RandomAccess collections
            List<String> listKey = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                listKey.add("list_elem" + i);
            }
            map.put(listKey, "list" + size);
            assertEquals("list" + size, map.get(listKey));
            
            // Test non-RandomAccess collections (for small sizes)
            if (size <= 3) {
                LinkedList<String> linkedKey = new LinkedList<>();
                for (int i = 0; i < size; i++) {
                    linkedKey.add("linked_elem" + i);
                }
                map.put(linkedKey, "linked" + size);
                assertEquals("linked" + size, map.get(linkedKey));
            }
        }
    }
}