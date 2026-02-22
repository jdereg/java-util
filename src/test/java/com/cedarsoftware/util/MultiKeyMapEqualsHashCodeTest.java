package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Comprehensive tests for MultiKeyMap equals() and hashCode() implementation.
 * Verifies that two MultiKeyMaps with equivalent entries compare as equal,
 * including complex scenarios with mixed List and Set keys where Sets can be
 * in different orders but should still match.
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
class MultiKeyMapEqualsHashCodeTest {

    /**
     * Test basic equals() and hashCode() contract for empty maps
     */
    @Test
    void testEmptyMapsAreEqual() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        // Empty maps should be equal
        assertEquals(map1, map2);
        assertEquals(map2, map1);

        // Empty maps should have same hashCode
        assertEquals(map1.hashCode(), map2.hashCode());

        // Reflexive
        assertEquals(map1, map1);
        assertEquals(map2, map2);
    }

    /**
     * Test equals() and hashCode() with simple single keys
     */
    @Test
    void testSimpleKeysEqual() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        map1.put("key1", "value1");
        map1.put("key2", "value2");

        map2.put("key1", "value1");
        map2.put("key2", "value2");

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    /**
     * Test equals() and hashCode() with array keys
     */
    @Test
    void testArrayKeysEqual() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        map1.put(new Object[]{"a", "b", "c"}, "value1");
        map1.put(new Object[]{"x", "y", "z"}, "value2");

        map2.put(new Object[]{"a", "b", "c"}, "value1");
        map2.put(new Object[]{"x", "y", "z"}, "value2");

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    /**
     * Test equals() and hashCode() with List keys
     */
    @Test
    void testListKeysEqual() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        map1.put(Arrays.asList(1, 2, 3), "value1");
        map1.put(Arrays.asList(4, 5, 6), "value2");

        map2.put(Arrays.asList(1, 2, 3), "value1");
        map2.put(Arrays.asList(4, 5, 6), "value2");

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    /**
     * Test equals() and hashCode() with Set keys
     * Sets with same elements in different order should make maps equal
     */
    @Test
    void testSetKeysEqual_DifferentOrder() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        // Store with Set in one order
        map1.put(new HashSet<>(Arrays.asList("a", "b", "c")), "value1");
        map1.put(new HashSet<>(Arrays.asList("x", "y", "z")), "value2");

        // Store with Set in different order - should still be equal
        map2.put(new HashSet<>(Arrays.asList("c", "b", "a")), "value1");
        map2.put(new HashSet<>(Arrays.asList("z", "x", "y")), "value2");

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    /**
     * Test equals() and hashCode() with Set keys using different Set implementations
     */
    @Test
    void testSetKeysEqual_DifferentSetTypes() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        // Use HashSet in map1
        map1.put(new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)), "value1");

        // Use TreeSet in map2 (different order, different type)
        map2.put(new TreeSet<>(Arrays.asList(5, 3, 1, 4, 2)), "value1");

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    /**
     * CORE TEST: Mixed List and Set keys with Set in different order
     * This is the primary use case mentioned by the user
     */
    @Test
    void testMixedListSetKeys_SetInDifferentOrder() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        // Map1: List(1,2,3) + Set(4,5,6) in one order
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        Set<Integer> set1 = new HashSet<>(Arrays.asList(4, 5, 6));
        map1.put(new Object[]{list1, set1}, "halfAndHalf");

        // Map2: Same List + Set(4,5,6) in DIFFERENT order
        List<Integer> list2 = Arrays.asList(1, 2, 3);
        Set<Integer> set2 = new HashSet<>(Arrays.asList(6, 5, 4));  // Different order
        map2.put(new Object[]{list2, set2}, "halfAndHalf");

        // Maps should be equal despite Set order difference
        assertEquals(map1, map2, "Maps with same List and equivalent Sets should be equal");
        assertEquals(map1.hashCode(), map2.hashCode(), "Maps with same List and equivalent Sets should have same hashCode");
    }

    /**
     * Test mixed List and Set keys with multiple entries
     */
    @Test
    void testMixedListSetKeys_MultipleEntries() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        // Entry 1: List + Set
        map1.put(new Object[]{
            Arrays.asList("a", "b", "c"),
            new HashSet<>(Arrays.asList("x", "y", "z"))
        }, "entry1");

        // Entry 2: Set + List
        map1.put(new Object[]{
            new HashSet<>(Arrays.asList(1, 2, 3)),
            Arrays.asList("p", "q", "r")
        }, "entry2");

        // Entry 3: List + Set + List
        map1.put(new Object[]{
            Arrays.asList("alpha", "beta"),
            new HashSet<>(Arrays.asList("one", "two")),
            Arrays.asList("gamma", "delta")
        }, "entry3");

        // Map2 has same entries but Sets in different order
        map2.put(new Object[]{
            Arrays.asList("a", "b", "c"),
            new HashSet<>(Arrays.asList("z", "x", "y"))  // Different order
        }, "entry1");

        map2.put(new Object[]{
            new HashSet<>(Arrays.asList(3, 1, 2)),  // Different order
            Arrays.asList("p", "q", "r")
        }, "entry2");

        map2.put(new Object[]{
            Arrays.asList("alpha", "beta"),
            new HashSet<>(Arrays.asList("two", "one")),  // Different order
            Arrays.asList("gamma", "delta")
        }, "entry3");

        assertEquals(map1, map2, "Maps with multiple mixed List/Set entries should be equal");
        assertEquals(map1.hashCode(), map2.hashCode(), "Maps with multiple mixed List/Set entries should have same hashCode");
    }

    /**
     * Test that maps with Set in same key position but different elements are NOT equal
     */
    @Test
    void testMixedListSetKeys_DifferentSetElements() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        // Map1: List(1,2,3) + Set(4,5,6)
        map1.put(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList(4, 5, 6))
        }, "value");

        // Map2: List(1,2,3) + Set(4,5,7) - different element
        map2.put(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList(4, 5, 7))  // 7 instead of 6
        }, "value");

        assertNotEquals(map1, map2, "Maps with different Set elements should NOT be equal");
    }

    /**
     * Test that maps with List in different order are NOT equal
     */
    @Test
    void testMixedListSetKeys_DifferentListOrder() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        // Map1: List(1,2,3) + Set(4,5,6)
        map1.put(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList(4, 5, 6))
        }, "value");

        // Map2: List(2,1,3) + Set(4,5,6) - different List order
        map2.put(new Object[]{
            Arrays.asList(2, 1, 3),  // Different order
            new HashSet<>(Arrays.asList(4, 5, 6))
        }, "value");

        assertNotEquals(map1, map2, "Maps with different List order should NOT be equal");
    }

    /**
     * Test equals() and hashCode() with nested structures
     */
    @Test
    void testNestedStructures() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        // Nested: List containing a Set
        map1.put(Arrays.asList(
            "prefix",
            new HashSet<>(Arrays.asList("a", "b", "c"))
        ), "nested1");

        map2.put(Arrays.asList(
            "prefix",
            new HashSet<>(Arrays.asList("c", "a", "b"))  // Set in different order
        ), "nested1");

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    /**
     * Test equals() with null values
     */
    @Test
    void testNullValues() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        map1.put(Arrays.asList(1, 2, 3), null);
        map2.put(Arrays.asList(1, 2, 3), null);

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    /**
     * Test equals() with different values for same key
     */
    @Test
    void testDifferentValues() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        map1.put(Arrays.asList(1, 2, 3), "value1");
        map2.put(Arrays.asList(1, 2, 3), "value2");  // Different value

        assertNotEquals(map1, map2);
    }

    /**
     * Test equals() with different number of entries
     */
    @Test
    void testDifferentSizes() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        map1.put(Arrays.asList(1, 2, 3), "value1");
        map1.put(Arrays.asList(4, 5, 6), "value2");

        map2.put(Arrays.asList(1, 2, 3), "value1");
        // map2 has only one entry

        assertNotEquals(map1, map2);
    }

    /**
     * Test equals() is reflexive
     */
    @Test
    void testReflexive() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList("a", "b", "c"))
        }, "value");

        assertEquals(map, map, "Map should equal itself (reflexive)");
    }

    /**
     * Test equals() is symmetric
     */
    @Test
    void testSymmetric() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        map1.put(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList("x", "y", "z"))
        }, "value");

        map2.put(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList("z", "y", "x"))
        }, "value");

        assertEquals(map1, map2, "map1 equals map2");
        assertEquals(map2, map1, "map2 equals map1 (symmetric)");
    }

    /**
     * Test equals() is transitive
     */
    @Test
    void testTransitive() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();
        MultiKeyMap<String> map3 = new MultiKeyMap<>();

        // All three maps have same content, Sets in different orders
        map1.put(new Object[]{
            Arrays.asList("a", "b"),
            new HashSet<>(Arrays.asList(1, 2, 3))
        }, "value");

        map2.put(new Object[]{
            Arrays.asList("a", "b"),
            new HashSet<>(Arrays.asList(3, 1, 2))
        }, "value");

        map3.put(new Object[]{
            Arrays.asList("a", "b"),
            new HashSet<>(Arrays.asList(2, 3, 1))
        }, "value");

        assertEquals(map1, map2, "map1 equals map2");
        assertEquals(map2, map3, "map2 equals map3");
        assertEquals(map1, map3, "map1 equals map3 (transitive)");
    }

    /**
     * Test equals() with null
     */
    @Test
    void testNotEqualToNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(Arrays.asList(1, 2, 3), "value");

        assertNotEquals(null, map);
        assertNotEquals(map, null);
    }

    /**
     * Test equals() with different object type
     */
    @Test
    void testNotEqualToDifferentType() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(Arrays.asList(1, 2, 3), "value");

        assertNotEquals(map, "not a map");
        assertNotEquals(map, new HashMap<>());
    }

    /**
     * Test equals() with standard HashMap containing same data
     * MultiKeyMap should be comparable to other Map implementations
     */
    @Test
    void testEqualsWithStandardHashMap() {
        MultiKeyMap<String> mkm = new MultiKeyMap<>();
        Map<Object, String> hashMap = new HashMap<>();

        // Both maps have same key-value pairs
        Object key1 = Arrays.asList(1, 2, 3);
        Object key2 = new HashSet<>(Arrays.asList("a", "b", "c"));

        mkm.put(key1, "value1");
        mkm.put(key2, "value2");

        hashMap.put(key1, "value1");
        hashMap.put(key2, "value2");

        // MultiKeyMap should equal HashMap with same entries
        assertEquals(mkm, hashMap, "MultiKeyMap should equal HashMap with same entries");
        assertEquals(hashMap, mkm, "HashMap should equal MultiKeyMap (symmetric)");
        assertEquals(mkm.hashCode(), hashMap.hashCode(), "hashCode should match HashMap");
    }

    @Test
    void testEqualsRejectsDuplicateEquivalentNumericKeysInComparedMap() {
        MultiKeyMap<String> mkm = new MultiKeyMap<>();
        mkm.put(1, "value");
        mkm.put(2, "other");

        Map<Object, String> other = new HashMap<>();
        other.put(1, "value");
        other.put(1L, "value");

        assertNotEquals(mkm, other,
                "Compared map has duplicate keys that collapse under MultiKeyMap equivalence and must not be equal");
    }

    @Test
    void testEqualsRejectsDuplicateEquivalentCaseInsensitiveKeysInComparedMap() {
        MultiKeyMap<String> mkm = MultiKeyMap.<String>builder().caseSensitive(false).build();
        mkm.put("id", "value");
        mkm.put("other", "x");

        Map<Object, String> other = new HashMap<>();
        other.put("id", "value");
        other.put("ID", "value");

        assertNotEquals(mkm, other,
                "Compared map has duplicate keys that collapse under case-insensitive equivalence "
                        + "and must not be equal");
    }

    /**
     * Test hashCode consistency
     * Multiple calls to hashCode() on same object should return same value
     */
    @Test
    void testHashCodeConsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList("x", "y", "z"))
        }, "value");

        int hash1 = map.hashCode();
        int hash2 = map.hashCode();
        int hash3 = map.hashCode();

        assertEquals(hash1, hash2, "Multiple hashCode calls should return same value");
        assertEquals(hash2, hash3, "Multiple hashCode calls should return same value");
    }

    /**
     * Test hashCode changes when map is modified
     */
    @Test
    void testHashCodeChangesOnModification() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(Arrays.asList(1, 2, 3), "value1");

        int hashBefore = map.hashCode();

        // Modify map
        map.put(Arrays.asList(4, 5, 6), "value2");

        int hashAfter = map.hashCode();

        assertNotEquals(hashBefore, hashAfter, "hashCode should change when map is modified");
    }

    /**
     * Test large maps with many mixed List/Set entries
     */
    @Test
    void testLargeMapsWithMixedKeys() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        // Add 100 entries with mixed List+Set keys
        for (int i = 0; i < 100; i++) {
            List<Integer> list = Arrays.asList(i, i + 1, i + 2);
            Set<String> set = new HashSet<>(Arrays.asList("a" + i, "b" + i, "c" + i));

            map1.put(new Object[]{list, set}, "value" + i);

            // Map2 has same entries but Sets constructed in reverse order
            Set<String> setReverse = new LinkedHashSet<>(Arrays.asList("c" + i, "b" + i, "a" + i));
            map2.put(new Object[]{list, setReverse}, "value" + i);
        }

        assertEquals(map1, map2, "Large maps with 100 mixed List/Set entries should be equal");
        assertEquals(map1.hashCode(), map2.hashCode(), "Large maps should have same hashCode");
    }

    /**
     * Test that inserting entries in different order still results in equal maps
     */
    @Test
    void testInsertionOrderDoesNotMatterForEquality() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        // Map1: Insert in order A, B, C
        map1.put(new Object[]{Arrays.asList(1, 2), new HashSet<>(Arrays.asList("a", "b"))}, "A");
        map1.put(new Object[]{Arrays.asList(3, 4), new HashSet<>(Arrays.asList("c", "d"))}, "B");
        map1.put(new Object[]{Arrays.asList(5, 6), new HashSet<>(Arrays.asList("e", "f"))}, "C");

        // Map2: Insert in order C, A, B (different insertion order)
        map2.put(new Object[]{Arrays.asList(5, 6), new HashSet<>(Arrays.asList("f", "e"))}, "C");
        map2.put(new Object[]{Arrays.asList(1, 2), new HashSet<>(Arrays.asList("b", "a"))}, "A");
        map2.put(new Object[]{Arrays.asList(3, 4), new HashSet<>(Arrays.asList("d", "c"))}, "B");

        assertEquals(map1, map2, "Maps with same entries in different insertion order should be equal");
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    /**
     * Test equals() and hashCode() with complex nested mixed structures
     */
    @Test
    void testComplexNestedMixedStructures() {
        MultiKeyMap<String> map1 = new MultiKeyMap<>();
        MultiKeyMap<String> map2 = new MultiKeyMap<>();

        // Create complex nested structure: List containing (Set containing Lists)
        Set<List<Integer>> innerSet1 = new HashSet<>(Arrays.asList(
            Arrays.asList(1, 2),
            Arrays.asList(3, 4)
        ));

        List<Object> outerList1 = Arrays.asList("prefix", innerSet1, "suffix");
        map1.put(outerList1, "complex");

        // Same structure but Set constructed in different order
        Set<List<Integer>> innerSet2 = new HashSet<>(Arrays.asList(
            Arrays.asList(3, 4),  // Different order in Set construction
            Arrays.asList(1, 2)
        ));

        List<Object> outerList2 = Arrays.asList("prefix", innerSet2, "suffix");
        map2.put(outerList2, "complex");

        assertEquals(map1, map2, "Maps with complex nested structures should be equal");
        assertEquals(map1.hashCode(), map2.hashCode());
    }
}
