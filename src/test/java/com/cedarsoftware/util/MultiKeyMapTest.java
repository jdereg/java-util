package com.cedarsoftware.util;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.logging.Logger;

public class MultiKeyMapTest {
    private static final Logger LOG = Logger.getLogger(MultiKeyMapTest.class.getName());
    @Test
    void testSingleElementArrayKeys() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(true).build();

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");
        map.put(new String[][]{{"a"}}, "[[alpha]]");
        map.put(new String[][][]{{{"a"}}}, "[[[alpha]]]");

        assert map.size() == 1;
        assertEquals("[[[alpha]]]", map.get("a"));      // last put

        assert map.containsKey("a");
        assert map.containsKey(new String[]{"a"});
        assert map.containsKey(new String[][]{{"a"}});
        assert map.containsKey(new String[][][]{{{"a"}}});
        
        assert map.containsMultiKey("a");
        assert map.containsMultiKey((Object) new String[]{"a"});
        assert map.containsMultiKey((Object) new String[][]{{"a"}});
        assert map.containsMultiKey((Object) new String[][][]{{{"a"}}});
        
        map.remove("a");
        assert map.isEmpty();
        
        map.putMultiKey("alpha", "a");
        map.putMultiKey("[alpha]", (Object) new String[]{"a"});
        map.putMultiKey("[[alpha]]", (Object) new String[][]{{"a"}});
        map.putMultiKey("[[[alpha]]]", (Object) new String[][][]{{{"a"}}});

        map.removeMultiKey("a");
        assert map.isEmpty();

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");
        map.put(new String[][]{{"a"}}, "[[alpha]]");
        map.put(new String[][][]{{{"a"}}}, "[[[alpha]]]");

        map.remove(new String[][][]{{{"a"}}});
        assert map.isEmpty();

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");
        map.put(new String[][]{{"a"}}, "[[alpha]]");
        map.put(new String[][][]{{{"a"}}}, "[[[alpha]]]");

        map.removeMultiKey((Object) new String[][][]{{{"a"}}});
        assert map.isEmpty();
    }

    @Test
    void testSingleElementArrayKeysFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(true).build());

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");

        assert map.size() == 1;
        assertEquals("[alpha]", map.get("A"));      // last put - different case

        assert map.containsKey("A");
        assert map.containsKey(new String[]{"A"});

        map.remove("A");
        assert map.isEmpty();
    }

    @Test
    void testSingleElementArrayKeysNoFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(false).build());

        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");  // This should overwrite "alpha" since single-element arrays are equivalent to single keys

        LOG.info("Map size: " + map.size());
        assert map.size() == 1;  // Single-element array overwrites single key
        assertEquals("[alpha]", map.get("A"));                    // different case - should get the array value
        assertEquals("[alpha]", map.get(new String[]{"A"}));      // different case - should get the array value

        assert map.containsKey("A");
        assert map.containsKey(new String[]{"A"});

        map.remove("A");  // This removes both "a" and equivalent new String[]{"a"} since they're equivalent keys
        assert map.isEmpty();
    }

    @Test
    void testSingleElementCollectionKeysFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(true).build());

        map.put("a", "alpha");
        map.put(CollectionUtilities.listOf("a"), "[alpha]");

        assert map.size() == 1;
        assertEquals("[alpha]", map.get("A"));      // last put - different case

        assert map.containsKey("A");
        assert map.containsKey(CollectionUtilities.listOf("A"));

        map.remove("A");
        assert map.isEmpty();
    }

    @Test
    void testSingleElementCollectionKeysNoFlattenInCaseInsensitiveMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), MultiKeyMap.<String>builder().flattenDimensions(false).build());

        map.put("a", "alpha");
        map.put(CollectionUtilities.listOf("a"), "[alpha]");  // This should overwrite "alpha" since single-element collections are equivalent to single keys

        assert map.size() == 1;  // Single-element collection overwrites single key
        assertEquals("[alpha]", map.get("A"));                    // string key value - different case - should get the collection value
        assertEquals("[alpha]", map.get(CollectionUtilities.listOf("A")));      // collection key value - different case - should get the collection value

        assert map.containsKey("A");
        assert map.containsKey(CollectionUtilities.listOf("A"));

        map.remove("A");  // This removes both "a" and equivalent CollectionUtilities.listOf("a") since they're equivalent keys
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
        
        // Test array with null element
        map.put(new String[]{null}, "array with null");
        assertEquals("array with null", map.get(new String[]{null}));
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
        
        // All keys are separate when not flattened, but some equivalences due to NULL_SENTINEL uniformity
        assert map.size() == 3;  // Keys: NULL_SENTINEL (null + single null containers), "" (empty string + single empty string containers), empty containers
        
        // Test removal of edge cases with NULL_SENTINEL uniformity
        assertEquals("collection with null", map.remove(null));  // null equivalent to single null containers (last put wins)
        assertEquals("collection with empty string", map.remove(""));  // empty string equivalent to single empty string containers (last put wins)
        assertEquals("empty collection value", map.remove(new String[0]));  // empty array/collection are same
        // Note: null-related keys and empty string-related keys were already removed above due to equivalence
        
        assert map.isEmpty();
    }

    @Test
    void testCollectionKeyImmutability() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test with ArrayList - modify after put
        java.util.ArrayList<String> mutableList = new java.util.ArrayList<>();
        mutableList.add("a");
        mutableList.add("b");
        
        map.put(mutableList, "original");
        
        // Verify key works before modification
        assertEquals("original", map.get(mutableList));
        assertTrue(map.containsKey(mutableList));
        
        // Modify the original list
        mutableList.add("c");
        
        // Key should still work with original content (a,b) due to defensive copy
        java.util.ArrayList<String> lookupList = new java.util.ArrayList<>();
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
        java.util.HashSet<String> mutableSet = new java.util.HashSet<>();
        mutableSet.add("x");
        mutableSet.add("y");
        
        map.put(mutableSet, "hashset");
        
        // Modify the original set
        mutableSet.add("z");
        
        // Key should still work with original content (x,y) due to defensive copy
        java.util.HashSet<String> lookupSet = new java.util.HashSet<>();
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
}
