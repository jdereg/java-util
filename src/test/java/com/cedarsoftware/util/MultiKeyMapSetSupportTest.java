package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive tests for Set support in MultiKeyMap.
 * Sets are treated as order-agnostic collections that only match other Sets.
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
class MultiKeyMapSetSupportTest {

    @Test
    void testBasicSetStorageAndRetrieval() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<Integer> key = new HashSet<>(Arrays.asList(1, 2, 3));
        map.put(key, "value");

        assertEquals("value", map.get(key));
        assertTrue(map.containsKey(key));
        assertEquals(1, map.size());
    }

    @Test
    void testSetOrderAgnosticMatching() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Store with one order
        Set<String> key1 = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        map.put(key1, "value");

        // Retrieve with different order
        Set<String> key2 = new LinkedHashSet<>(Arrays.asList("c", "a", "b"));
        assertEquals("value", map.get(key2));

        // HashSet with different iteration order
        Set<String> key3 = new HashSet<>(Arrays.asList("b", "c", "a"));
        assertEquals("value", map.get(key3));

        // TreeSet (sorted order)
        Set<String> key4 = new TreeSet<>(Arrays.asList("b", "a", "c"));
        assertEquals("value", map.get(key4));
    }

    @Test
    void testSetDoesNotMatchList() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> list = Arrays.asList(1, 2, 3);

        map.put(set, "set-value");
        map.put(list, "list-value");

        // Sets and Lists are distinct keys
        assertEquals("set-value", map.get(set));
        assertEquals("list-value", map.get(list));
        assertEquals(2, map.size());
    }

    @Test
    void testSetDoesNotMatchArray() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<String> set = new HashSet<>(Arrays.asList("x", "y", "z"));
        String[] array = {"x", "y", "z"};

        map.put(set, "set-value");
        map.put(array, "array-value");

        // Sets and Arrays are distinct keys
        assertEquals("set-value", map.get(set));
        assertEquals("array-value", map.get(array));
        assertEquals(2, map.size());
    }

    @Test
    void testNestedSetsOrderAgnostic() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Nested Sets
        Set<Object> inner1 = new HashSet<>(Arrays.asList(1, 2));
        Set<Object> inner2 = new HashSet<>(Arrays.asList(3, 4));
        Set<Object> outer = new HashSet<>(Arrays.asList(inner1, inner2));

        map.put(outer, "nested-value");

        // Create equivalent Set with different order
        Set<Object> inner1_rev = new HashSet<>(Arrays.asList(2, 1)); // Same elements, possibly different order
        Set<Object> inner2_rev = new HashSet<>(Arrays.asList(4, 3));
        Set<Object> outer_rev = new HashSet<>(Arrays.asList(inner2_rev, inner1_rev));

        assertEquals("nested-value", map.get(outer_rev));
    }

    @Test
    void testArrayContainingSet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        Object[] key = {set, "extra"};

        map.put(key, "value");

        // Retrieve with Set in different order
        Set<String> set2 = new LinkedHashSet<>(Arrays.asList("c", "b", "a"));
        Object[] key2 = {set2, "extra"};

        assertEquals("value", map.get(key2));
    }

    @Test
    void testListContainingSet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Object> key = Arrays.asList(set, "marker");

        map.put(key, "value");

        // Retrieve with Set in different order
        Set<Integer> set2 = new TreeSet<>(Arrays.asList(3, 1, 2));
        List<Object> key2 = Arrays.asList(set2, "marker");

        assertEquals("value", map.get(key2));
    }

    @Test
    void testSetInFlattenMode() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(true)
                .build();

        // Sets preserve their markers even in flatten mode
        Set<String> set = new HashSet<>(Arrays.asList("x", "y", "z"));
        map.put(set, "set-value");

        // Should match with different order
        Set<String> set2 = new LinkedHashSet<>(Arrays.asList("z", "x", "y"));
        assertEquals("set-value", map.get(set2));

        // Should NOT match List (different semantics)
        List<String> list = Arrays.asList("x", "y", "z");
        assertNull(map.get(list));
    }

    @Test
    void testSetWithCOLLECTIONS_NOT_EXPANDED() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED)
                .build();

        Set<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> set2 = new HashSet<>(Arrays.asList("c", "a", "b"));

        map.put(set1, "value");

        // Sets use their own equals() in NOT_EXPANDED mode
        assertEquals("value", map.get(set2)); // Should work because Set.equals is order-agnostic
    }

    @Test
    void testEmptySet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<Object> emptySet = Collections.emptySet();
        map.put(emptySet, "empty-value");

        assertEquals("empty-value", map.get(emptySet));
        assertEquals("empty-value", map.get(Collections.emptySet()));
    }

    @Test
    void testEmptySetVsEmptyList() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<Object> emptySet = Collections.emptySet();
        List<Object> emptyList = Collections.emptyList();

        map.put(emptySet, "empty-set");
        map.put(emptyList, "empty-list");

        // Empty Set and empty List are distinct keys
        assertEquals("empty-set", map.get(emptySet));
        assertEquals("empty-list", map.get(emptyList));
        assertEquals(2, map.size());
    }

    @Test
    void testSetWithDuplicatesInLookup() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        map.put(set, "value");

        // List with duplicates should NOT match Set
        List<Integer> listWithDups = Arrays.asList(1, 2, 2, 3);
        assertNull(map.get(listWithDups));
    }

    @Test
    void testSetRemove() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        map.put(set, "value");

        // Remove with different order
        Set<String> set2 = new LinkedHashSet<>(Arrays.asList("c", "b", "a"));
        assertEquals("value", map.remove(set2));
        assertEquals(0, map.size());
    }

    @Test
    void testSetReplace() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
        map.put(set1, "old-value");

        // Replace with different order
        Set<Integer> set2 = new TreeSet<>(Arrays.asList(3, 2, 1));
        assertEquals("old-value", map.replace(set2, "new-value"));
        assertEquals("new-value", map.get(set1));
    }

    @Test
    void testKeySetExternalizationWithSets() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        map.put(set, "value");

        Set<Object> keys = map.keySet();
        assertEquals(1, keys.size());

        // The externalized key should be a Set (preserving original type)
        Object externalizedKey = keys.iterator().next();
        assertTrue(externalizedKey instanceof Set);

        // Should contain the Set elements
        @SuppressWarnings("unchecked")
        Set<String> keySet = (Set<String>) externalizedKey;
        assertEquals(3, keySet.size());
        assertTrue(keySet.containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    void testEntrySetExternalizationWithSets() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        map.put(set, "value");

        Set<Map.Entry<Object, String>> entries = map.entrySet();
        assertEquals(1, entries.size());

        Map.Entry<Object, String> entry = entries.iterator().next();
        assertEquals("value", entry.getValue());

        // Key should be externalized as Set (preserving original type)
        assertTrue(entry.getKey() instanceof Set);
    }

    @Test
    void testMultipleSetsWithSameElements() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<String> set1 = new HashSet<>(Arrays.asList("x", "y", "z"));
        Set<String> set2 = new TreeSet<>(Arrays.asList("z", "y", "x"));
        Set<String> set3 = new LinkedHashSet<>(Arrays.asList("y", "z", "x"));

        map.put(set1, "first");

        // All should resolve to same key (order-agnostic)
        assertEquals("first", map.get(set2));
        assertEquals("first", map.get(set3));

        // Overwriting
        map.put(set2, "second");
        assertEquals("second", map.get(set1));
        assertEquals(1, map.size());
    }

    @Test
    void testSetContainsKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<Long> set = new HashSet<>(Arrays.asList(100L, 200L, 300L));
        map.put(set, "value");

        // Check with different order
        assertTrue(map.containsKey(new HashSet<>(Arrays.asList(300L, 100L, 200L))));
        assertTrue(map.containsKey(new TreeSet<>(Arrays.asList(200L, 300L, 100L))));

        // Should not match List
        assertFalse(map.containsKey(Arrays.asList(100L, 200L, 300L)));
    }

    @Test
    void testSetWithNullElements() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Note: Set.of() doesn't allow nulls, so use HashSet
        Set<String> set = new HashSet<>(Arrays.asList("a", null, "b"));
        map.put(set, "value");

        Set<String> set2 = new HashSet<>(Arrays.asList("b", null, "a"));
        assertEquals("value", map.get(set2));
    }

    @Test
    void testNestedSetInArray() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<String> innerSet = new HashSet<>(Arrays.asList("inner1", "inner2"));
        Object[] outerArray = {"prefix", innerSet, "suffix"};

        map.put(outerArray, "value");

        // Retrieve with Set in different order
        Set<String> innerSet2 = new LinkedHashSet<>(Arrays.asList("inner2", "inner1"));
        Object[] outerArray2 = {"prefix", innerSet2, "suffix"};

        assertEquals("value", map.get(outerArray2));
    }

    @Test
    void testSetOfSets() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        Set<Integer> inner1 = new HashSet<>(Arrays.asList(1, 2));
        Set<Integer> inner2 = new HashSet<>(Arrays.asList(3, 4));
        Set<Set<Integer>> outer = new HashSet<>(Arrays.asList(inner1, inner2));

        map.put(outer, "nested-sets");

        // Retrieve with inner sets in different order
        Set<Integer> inner1_diff = new HashSet<>(Arrays.asList(2, 1));
        Set<Integer> inner2_diff = new HashSet<>(Arrays.asList(4, 3));
        Set<Set<Integer>> outer_diff = new HashSet<>(Arrays.asList(inner2_diff, inner1_diff));

        assertEquals("nested-sets", map.get(outer_diff));
    }
}
