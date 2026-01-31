package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultiKeyMapTest {
    private static final Logger LOG = Logger.getLogger(MultiKeyMapTest.class.getName());
    @Test
    void testSingleElementArrayKeys() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(true).build();

        // With flatten=true, nested arrays are flattened, but single-element arrays don't collapse to their contents
        // So "a" and ["a"] are different keys, but [["a"]] flattens to ["a"]
        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");
        map.put(new String[][]{{"a"}}, "[[alpha]]");  // Flattens to ["a"], overwrites previous
        map.put(new String[][][]{{{"a"}}}, "[[[alpha]]]");  // Flattens to ["a"], overwrites again

        assert map.size() == 2;  // "a" and ["a"] are different
        assertEquals("alpha", map.get("a"));  // "a" keeps its own value
        assertEquals("[[[alpha]]]", map.get(new String[]{"a"}));  // Flattened [["a"]] and [[["a"]]] overwrite ["a"]

        assert map.containsKey("a");
        assert map.containsKey(new String[]{"a"});
        assert map.containsKey(new String[][]{{"a"}});  // Flattens to ["a"]
        assert map.containsKey(new String[][][]{{{"a"}}});  // Flattens to ["a"]
        
        assert map.containsMultiKey("a");
        assert map.containsMultiKey((Object) new String[]{"a"});
        assert map.containsMultiKey((Object) new String[][]{{"a"}});
        assert map.containsMultiKey((Object) new String[][][]{{{"a"}}});
        
        map.remove("a");
        assert map.size() == 1;  // Only ["a"] remains
        map.remove(new String[]{"a"});
        assert map.isEmpty();
        
        map.putMultiKey("alpha", "a");
        map.putMultiKey("[alpha]", (Object) new String[]{"a"});
        map.putMultiKey("[[alpha]]", (Object) new String[][]{{"a"}});  // Flattens to ["a"], overwrites
        map.putMultiKey("[[[alpha]]]", (Object) new String[][][]{{{"a"}}});  // Flattens to ["a"], overwrites

        assert map.size() == 2;
        map.removeMultiKey("a");
        assert map.size() == 1;
        map.removeMultiKey((Object) new String[]{"a"});
        assert map.isEmpty();

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");
        map.put(new String[][]{{"a"}}, "[[alpha]]");  // Flattens to "a", overwrites
        map.put(new String[][][]{{{"a"}}}, "[[[alpha]]]");  // Flattens to "a", overwrites again

        assert map.size() == 2;
        map.remove(new String[][][]{{{"a"}}});  // Removes ["a"] (flattened 3D becomes 1D)
        assert map.size() == 1;  // Only "a" remains
        map.remove("a");
        assert map.isEmpty();

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");
        map.put(new String[][]{{"a"}}, "[[alpha]]");
        map.put(new String[][][]{{{"a"}}}, "[[[alpha]]]");

        assert map.size() == 2;
        map.removeMultiKey((Object) new String[][][]{{{"a"}}});  // Removes ["a"] (flattened)
        assert map.size() == 1;
        map.removeMultiKey("a");  // Remove "a"
        assert map.isEmpty();
    }

    @Test
    void testSingleElementArrayKeysFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(true).build());

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");

        assert map.size() == 2;  // No collapse - two different keys
        assertEquals("alpha", map.get("A"));  // Case insensitive single key
        assertEquals("[alpha]", map.get(new String[]{"A"}));  // Case insensitive array

        assert map.containsKey("A");
        assert map.containsKey(new String[]{"A"});

        map.remove("A");
        assert map.size() == 1;
        map.remove(new String[]{"A"});
        assert map.isEmpty();
    }

    @Test
    void testSingleElementArrayKeysNoFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(false).build());

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");  // This should overwrite "alpha" since single-element arrays are equivalent to single keys

        LOG.info("Map size: " + map.size());
        assert map.size() == 2;  // No collapse - two different keys
        assertEquals("alpha", map.get("A"));  // Case insensitive single key
        assertEquals("[alpha]", map.get(new String[]{"A"}));  // Case insensitive array key

        assert map.containsKey("A");
        assert map.containsKey(new String[]{"A"});

        map.remove("A");  // Only removes "a"
        assert map.size() == 1;
        map.remove(new String[]{"A"});
        assert map.isEmpty();
    }

    @Test
    void testSingleElementCollectionKeysFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(true).build());

        map.put("a", "alpha");
        map.put(CollectionUtilities.listOf("a"), "[alpha]");

        assert map.size() == 2;  // No collapse - two different keys
        assertEquals("alpha", map.get("A"));  // Case insensitive single key
        assertEquals("[alpha]", map.get(CollectionUtilities.listOf("A")));  // Case insensitive collection

        assert map.containsKey("A");
        assert map.containsKey(CollectionUtilities.listOf("A"));

        map.remove("A");
        assert map.size() == 1;
        map.remove(CollectionUtilities.listOf("A"));
        assert map.isEmpty();
    }

    @Test
    void testSingleElementCollectionKeysNoFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(false).build());

        // No collapse: "a" and collection ["a"] are different keys
        map.put("a", "alpha");
        map.put(CollectionUtilities.listOf("a"), "[alpha]");  // Different key, does not overwrite

        assert map.size() == 2;  // Two different keys
        assertEquals("alpha", map.get("A"));  // Case insensitive single key
        assertEquals("[alpha]", map.get(CollectionUtilities.listOf("A")));  // Case insensitive collection

        assert map.containsKey("A");
        assert map.containsKey(CollectionUtilities.listOf("A"));

        map.remove("A");  // Only removes "a"
        assert map.size() == 1;  // Collection remains
        map.remove(CollectionUtilities.listOf("A"));
        assert map.isEmpty();
    }

    @Test
    void testSingleElementArrayKeys3() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(true).build();

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(new String[]{"a", "b", "c"}, "[alpha, beta, gamma]");
        map.put(new String[][]{{"a", "b", "c"}}, "[[alpha, beta, gamma]]");
        map.put(new String[][][]{{{"a", "b", "c"}}}, "[[[alpha, beta, gamma]]]");

        // When flattenDimensions=true, multi-dimensional arrays with same elements should be treated as same key
        // So we should have: "a", "b", "c", and the flattened array key ["a", "b", "c"] 
        assert map.size() == 4;  // "a", "b", "c", and the flattened multi-dimensional key
        assertEquals("alpha", map.get("a"));
        assertEquals("beta", map.get("b"));
        assertEquals("gamma", map.get("c"));
        assertEquals("[[[alpha, beta, gamma]]]", map.get(new String[]{"a", "b", "c"}));      // last put for flattened key
        
        assert map.containsKey("a");
        assert map.containsKey("b");
        assert map.containsKey("c");
        assert map.containsKey(new String[]{"a", "b", "c"});
        assert map.containsKey(new String[][]{{"a", "b", "c"}});
        assert map.containsKey(new String[][][]{{{"a", "b", "c"}}});
        
        assert map.containsMultiKey("a");
        assert map.containsMultiKey("b");
        assert map.containsMultiKey("c");
        assert map.containsMultiKey((Object) new String[]{"a", "b", "c"});
        assert map.containsMultiKey((Object) new String[][]{{"a", "b", "c"}});
        assert map.containsMultiKey((Object) new String[][][]{{{"a", "b", "c"}}});
        
        map.remove("a");
        assert map.size() == 3;
        map.remove("b");
        assert map.size() == 2;
        map.remove("c");
        assert map.size() == 1;
        map.remove(new String[]{"a", "b", "c"});
        assert map.isEmpty();
        
        map.putMultiKey("alpha", "a");
        map.putMultiKey("beta", "b");
        map.putMultiKey("gamma", "c");
        map.putMultiKey("[alpha, beta, gamma]", (Object) new String[]{"a", "b", "c"});
        map.putMultiKey("[[alpha, beta, gamma]]", (Object) new String[][]{{"a", "b", "c"}});
        map.putMultiKey("[[[alpha, beta, gamma]]]", (Object) new String[][][]{{{"a", "b", "c"}}});
        map.putMultiKey("collection: [alpha, beta, gamma]", (Object) CollectionUtilities.listOf("a", "b", "c"));

        // When flattenDimensions=true, arrays/collections with same elements flatten to same key
        // So we have: "a", "b", "c", and the flattened multi-element key (arrays + collection = same key)
        assert map.size() == 4;  // "a", "b", "c", and the flattened multi-element key
        
        map.removeMultiKey("a");
        assert map.size() == 3;
        map.removeMultiKey("b");
        assert map.size() == 2;
        map.removeMultiKey("c");
        assert map.size() == 1;
        map.removeMultiKey((Object) new String[]{"a", "b", "c"});
        assert map.isEmpty();

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(new String[]{"a", "b", "c"}, "[alpha, beta, gamma]");
        map.put(new String[][]{{"a", "b", "c"}}, "[[alpha, beta, gamma]]");
        map.put(new String[][][]{{{"a", "b", "c"}}}, "[[[alpha, beta, gamma]]]");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "collection: [alpha, beta, gamma]");

        map.remove(new String[][][]{{{"a", "b", "c"}}});
        assert map.size() == 3;  // Still have "a", "b", "c" (the flattened multi-element key was removed)

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(new String[]{"a", "b", "c"}, "[alpha, beta, gamma]");
        map.put(new String[][]{{"a", "b", "c"}}, "[[alpha, beta, gamma]]");
        map.put(new String[][][]{{{"a", "b", "c"}}}, "[[[alpha, beta, gamma]]]");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "collection: [alpha, beta, gamma]");

        map.removeMultiKey((Object) new String[][][]{{{"a", "b", "c"}}});
        assert map.size() == 3;  // Still have "a", "b", "c" (the flattened multi-element key was removed)
    }

    @Test
    void testSingleElementArrayKeysFlattenInCaseInsensitiveMap3() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(true).build());

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(new String[]{"a", "b", "c"}, "[alpha, beta, gamma]");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "collection: [alpha, beta, gamma]");

        assert map.size() == 4;  // Individual keys and array/collection keys are different when flattened
        assertEquals("alpha", map.get("A"));                    // different case
        assertEquals("beta", map.get("B"));                     // different case
        assertEquals("gamma", map.get("C"));                    // different case
        assertEquals("collection: [alpha, beta, gamma]", map.get(new String[]{"A", "B", "C"}));      // different case

        assert map.containsKey("A");
        assert map.containsKey("B");
        assert map.containsKey("C");
        assert map.containsKey(new String[]{"A", "B", "C"});
        assert map.containsKey(CollectionUtilities.listOf("A", "B", "C"));

        map.remove("A");
        assert map.size() == 3;
        map.remove("B");
        assert map.size() == 2;
        map.remove("C");
        assert map.size() == 1;
        map.remove(new String[]{"A", "B", "C"});
        assert map.isEmpty();
    }

    @Test
    void testSingleElementArrayKeysNoFlattenInCaseInsensitiveMap3() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(false).build());

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(new String[]{"a", "b", "c"}, "[alpha, beta, gamma]");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "collection: [alpha, beta, gamma]");

        assert map.size() == 4;  // 3 string keys + 1 array/collection key (array and collection are equivalent in case-insensitive map)
        assertEquals("alpha", map.get("A"));                    // different case
        assertEquals("beta", map.get("B"));                     // different case
        assertEquals("gamma", map.get("C"));                    // different case
        assertEquals("collection: [alpha, beta, gamma]", map.get(new String[]{"A", "B", "C"}));  // Array key equivalent to collection in case-insensitive map
        assertEquals("collection: [alpha, beta, gamma]", map.get(CollectionUtilities.listOf("A", "B", "C")));

        assert map.containsKey("A");
        assert map.containsKey("B");
        assert map.containsKey("C");
        assert map.containsKey(new String[]{"A", "B", "C"});
        assert map.containsKey(CollectionUtilities.listOf("A", "B", "C"));

        map.remove("A");
        assert map.size() == 3;
        map.remove("B");
        assert map.size() == 2;
        map.remove("C");
        assert map.size() == 1;
        map.remove(new String[]{"A", "B", "C"});
        assert map.isEmpty();
    }

    @Test
    void testSingleElementCollectionKeysFlattenInCaseInsensitiveMap3() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(true).build());

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "[alpha, beta, gamma]");
        map.put(new String[]{"a", "b", "c"}, "array: [alpha, beta, gamma]");

        assert map.size() == 4;  // Individual keys and collection/array keys are different when flattened
        assertEquals("alpha", map.get("A"));                    // different case
        assertEquals("beta", map.get("B"));                     // different case
        assertEquals("gamma", map.get("C"));                    // different case
        assertEquals("array: [alpha, beta, gamma]", map.get(CollectionUtilities.listOf("A", "B", "C")));      // different case

        assert map.containsKey("A");
        assert map.containsKey("B");
        assert map.containsKey("C");
        assert map.containsKey(CollectionUtilities.listOf("A", "B", "C"));
        assert map.containsKey(new String[]{"A", "B", "C"});

        map.remove("A");
        assert map.size() == 3;
        map.remove("B");
        assert map.size() == 2;
        map.remove("C");
        assert map.size() == 1;
        map.remove(CollectionUtilities.listOf("A", "B", "C"));
        assert map.isEmpty();
    }

    @Test
    void testSingleElementCollectionKeysNoFlattenInCaseInsensitiveMap3() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(false).build());

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        map.put(CollectionUtilities.listOf("a", "b", "c"), "[alpha, beta, gamma]");
        map.put(new String[]{"a", "b", "c"}, "array: [alpha, beta, gamma]");

        assert map.size() == 4;  // Keys when not flattened: "a", "b", "c", and collection/array (treated as same)
        assertEquals("alpha", map.get("A"));                    // different case
        assertEquals("beta", map.get("B"));                     // different case
        assertEquals("gamma", map.get("C"));                    // different case
        assertEquals("array: [alpha, beta, gamma]", map.get(CollectionUtilities.listOf("A", "B", "C")));  // different case
        assertEquals("array: [alpha, beta, gamma]", map.get(new String[]{"A", "B", "C"}));

        assert map.containsKey("A");
        assert map.containsKey("B");
        assert map.containsKey("C");
        assert map.containsKey(CollectionUtilities.listOf("A", "B", "C"));
        assert map.containsKey(new String[]{"A", "B", "C"});

        map.remove("A");
        assert map.size() == 3;
        map.remove("B");
        assert map.size() == 2;
        map.remove("C");
        assert map.size() == 1;
        map.remove(CollectionUtilities.listOf("A", "B", "C"));
        assert map.isEmpty();
    }

    @Test
    void testMultiKeyMapEdgeCases() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(false).build();  // Use false to avoid flattening confusion

        // Test null key
        map.put(null, "null value");
        assertEquals("null value", map.get(null));
        assertTrue(map.containsKey(null));
        
        // Test empty string key
        map.put("", "empty string value");
        assertEquals("empty string value", map.get(""));
        assertTrue(map.containsKey(""));
        
        // Test that null and empty string are different keys in same map
        assert map.size() == 2;
        
        // Test empty array
        map.put(new String[0], "empty array value");
        assertEquals("empty array value", map.get(new String[0]));
        assertTrue(map.containsKey(new String[0]));
        
        // Test empty collection
        map.put(CollectionUtilities.listOf(), "empty collection value");
        assertEquals("empty collection value", map.get(CollectionUtilities.listOf()));
        assertTrue(map.containsKey(CollectionUtilities.listOf()));
        
        // Test array with null element - no collapse, so [null] is different from null
        map.put((Object) null, "direct null");
        map.put(new String[]{null}, "array with null");
        assertEquals("direct null", map.get((Object) null));
        assertEquals("array with null", map.get(new String[]{null}));
        assertTrue(map.containsKey((Object) null));
        assertTrue(map.containsKey(new String[]{null}));
        
        // Test array with empty string element
        map.put(new String[]{""}, "array with empty string");
        assertEquals("array with empty string", map.get(new String[]{""}));
        assertTrue(map.containsKey(new String[]{""}));
        
        // Test collection with null element
        map.put(CollectionUtilities.listOf((String) null), "collection with null");
        assertEquals("collection with null", map.get(CollectionUtilities.listOf((String) null)));
        assertTrue(map.containsKey(CollectionUtilities.listOf((String) null)));
        
        // Test collection with empty string element
        map.put(CollectionUtilities.listOf(""), "collection with empty string");
        assertEquals("collection with empty string", map.get(CollectionUtilities.listOf("")));
        assertTrue(map.containsKey(CollectionUtilities.listOf("")));
        
        // With no collapse, all containers are separate keys
        // But containers with same content are equivalent (berries not branches)
        assert map.size() == 5;  // Keys: null, "", empty containers, [null] containers, [""] containers
        
        // Test removal - no collapse, so each key is separate
        assertEquals("direct null", map.remove(null));  // Removes direct null only
        assertEquals("empty string value", map.remove(""));  // Removes empty string only
        assertEquals("empty collection value", map.remove(new String[0]));  // Removes empty containers
        assertEquals("collection with null", map.remove(new String[]{null}));  // Removes [null] containers
        assertEquals("collection with empty string", map.remove(new String[]{""}));  // Removes [""] containers
        
        assert map.isEmpty();
    }

    @Test
    @org.junit.jupiter.api.Disabled("TODO: Re-enable after implementing DeepCloner utility for defensive copying")
    void testCollectionKeyImmutability() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test with ArrayList - modify after put
        ArrayList<String> mutableList = new ArrayList<>();
        mutableList.add("a");
        mutableList.add("b");
        
        map.put(mutableList, "original");
        
        // Verify key works before modification
        assertEquals("original", map.get(mutableList));
        assertTrue(map.containsKey(mutableList));
        
        // Modify the original list
        mutableList.add("c");
        
        // Key should still work with original content (a,b) due to defensive copy
        ArrayList<String> lookupList = new ArrayList<>();
        lookupList.add("a");
        lookupList.add("b");
        assertEquals("original", map.get(lookupList));
        assertTrue(map.containsKey(lookupList));
        
        // Modified list (a,b,c) should NOT find the entry
        assertEquals(null, map.get(mutableList));
        
        // Test with LinkedList - modify after put
        java.util.LinkedList<Integer> mutableLinkedList = new java.util.LinkedList<>();
        mutableLinkedList.add(1);
        mutableLinkedList.add(2);
        
        map.put(mutableLinkedList, "linkedlist");
        
        // Modify the original linked list
        mutableLinkedList.add(3);
        
        // Key should still work with original content (1,2) due to defensive copy
        java.util.LinkedList<Integer> lookupLinkedList = new java.util.LinkedList<>();
        lookupLinkedList.add(1);
        lookupLinkedList.add(2);
        assertEquals("linkedlist", map.get(lookupLinkedList));
        
        // Test with HashSet - modify after put
        HashSet<String> mutableSet = new HashSet<>();
        mutableSet.add("x");
        mutableSet.add("y");
        
        map.put(mutableSet, "hashset");
        
        // Modify the original set
        mutableSet.add("z");
        
        // Key should still work with original content (x,y) due to defensive copy
        HashSet<String> lookupSet = new HashSet<>();
        lookupSet.add("x");
        lookupSet.add("y");
        assertEquals("hashset", map.get(lookupSet));
        
        // Test remove with modified collection
        mutableList.clear();
        mutableList.add("a");
        mutableList.add("b");
        assertEquals("original", map.remove(mutableList));
        
        // Verify all entries can be removed
        assert map.size() == 2;
        map.clear();
        assert map.isEmpty();
    }

    @Test
    void testDeeplyNestedSetAsKey() {
        // Test for bug where Set<List<Set<Integer>>> key fails after deserialization
        // The issue: expanded size (19 with markers) != original size (2)
        // This test proves the bug exists before fix

        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_EXPANDED)
                .build();

        // Create Set<List<Set<Integer>>> - use LinkedHashSet to control iteration order
        List<Set<Integer>> innerList1 = new ArrayList<>();
        innerList1.add(new HashSet<>(Arrays.asList(1, 2, 3)));
        innerList1.add(new HashSet<>(Arrays.asList(4, 5)));

        List<Set<Integer>> innerList2 = new ArrayList<>();
        innerList2.add(new HashSet<>(Arrays.asList(6, 7)));

        Set<List<Set<Integer>>> setListSet = new LinkedHashSet<>();
        setListSet.add(innerList1);
        setListSet.add(innerList2);

        // Put the nested Set as a key
        map.put(setListSet, "setListSetValue");

        // This should work - get with same key
        assertEquals("setListSetValue", map.get(setListSet));
        assertTrue(map.containsKey(setListSet));

        // Create an equivalent Set with REVERSED iteration order
        // This simulates what happens after deserialization
        List<Set<Integer>> lookupList1 = new ArrayList<>();
        lookupList1.add(new HashSet<>(Arrays.asList(1, 2, 3)));
        lookupList1.add(new HashSet<>(Arrays.asList(4, 5)));

        List<Set<Integer>> lookupList2 = new ArrayList<>();
        lookupList2.add(new HashSet<>(Arrays.asList(6, 7)));

        // Add in REVERSE order to force different iteration order (use LinkedHashSet)
        Set<List<Set<Integer>>> lookupSet = new LinkedHashSet<>();
        lookupSet.add(lookupList2);  // Reverse: add list2 first
        lookupSet.add(lookupList1);  // Then list1

        // Verify they're equal but have different iteration order
        assertEquals(setListSet, lookupSet);  // Sets are equal

        // Log iteration orders to show they differ
        StringBuilder original = new StringBuilder("Original order: ");
        for (List<Set<Integer>> elem : setListSet) {
            original.append(elem.hashCode()).append(" ");
        }
        StringBuilder lookup = new StringBuilder("Lookup order: ");
        for (List<Set<Integer>> elem : lookupSet) {
            lookup.append(elem.hashCode()).append(" ");
        }
        LOG.info(original.toString());
        LOG.info(lookup.toString());

        // This should work - get with equivalent key even with different iteration order
        assertEquals("setListSetValue", map.get(lookupSet));
        assertTrue(map.containsKey(lookupSet));

        LOG.info("testDeeplyNestedSetAsKey passed");
    }
}
