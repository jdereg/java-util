package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for MultiKeyMap with mixed List and Set keys.
 * This demonstrates that Lists are order-sensitive while Sets are order-insensitive
 * when both are used together as part of a multi-dimensional key.
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
class MultiKeyMapMixedListSetTest {

    /**
     * Core test: List + Set combination
     * List portion must match in exact order
     * Set portion can match in any order
     */
    @Test
    void testListThenSet_BasicBehavior() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Store: List(1,2,3) + Set(4,5,6)
        List<Integer> list = Arrays.asList(1, 2, 3);
        Set<Integer> set = new HashSet<>(Arrays.asList(4, 5, 6));
        Object[] key = {list, set};

        map.put(key, "halfAndHalf");

        // Should match: List in same order, Set in any order
        assertEquals("halfAndHalf", map.get(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList(4, 5, 6))
        }));

        assertEquals("halfAndHalf", map.get(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList(6, 5, 4))
        }));

        assertEquals("halfAndHalf", map.get(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList(5, 4, 6))
        }));

        assertEquals("halfAndHalf", map.get(new Object[]{
            Arrays.asList(1, 2, 3),
            new LinkedHashSet<>(Arrays.asList(6, 4, 5))
        }));

        assertEquals("halfAndHalf", map.get(new Object[]{
            Arrays.asList(1, 2, 3),
            new TreeSet<>(Arrays.asList(5, 6, 4))  // TreeSet sorts: 4,5,6
        }));

        // Should NOT match: List in different order (even though Set order varies)
        assertNull(map.get(new Object[]{
            Arrays.asList(2, 1, 3),
            new HashSet<>(Arrays.asList(4, 5, 6))
        }));

        assertNull(map.get(new Object[]{
            Arrays.asList(3, 2, 1),
            new HashSet<>(Arrays.asList(4, 5, 6))
        }));

        assertNull(map.get(new Object[]{
            Arrays.asList(1, 3, 2),
            new HashSet<>(Arrays.asList(6, 5, 4))
        }));

        // Should NOT match: Missing element in Set
        assertNull(map.get(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList(4, 5))  // Missing 6
        }));

        // Should NOT match: Missing element in List
        assertNull(map.get(new Object[]{
            Arrays.asList(1, 2),  // Missing 3
            new HashSet<>(Arrays.asList(4, 5, 6))
        }));

        // Should NOT match: Extra element in Set
        assertNull(map.get(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList(4, 5, 6, 7))
        }));

        assertEquals(1, map.size());
    }

    /**
     * Test: Flattened key (using individual elements instead of wrapping in Object[])
     * This is a common usage pattern where users pass varargs
     */
    @Test
    void testListThenSet_FlattenedKeyUsage() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Store with explicit array
        List<Integer> list = Arrays.asList(1, 2, 3);
        Set<Integer> set = new HashSet<>(Arrays.asList(4, 5, 6));
        map.put(new Object[]{list, set}, "value");

        // Retrieve with same structure
        assertEquals("value", map.get(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList(6, 5, 4))
        }));

        // Map only has one entry (the List+Set combo)
        assertEquals(1, map.size());
    }

    /**
     * Test: Set + List combination (reversed order)
     * Set first (order-insensitive), then List (order-sensitive)
     */
    @Test
    void testSetThenList_BasicBehavior() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Store: Set(1,2,3) + List(4,5,6)
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> list = Arrays.asList(4, 5, 6);
        map.put(new Object[]{set, list}, "setFirst");

        // Should match: Set in any order, List in same order
        assertEquals("setFirst", map.get(new Object[]{
            new HashSet<>(Arrays.asList(3, 1, 2)),
            Arrays.asList(4, 5, 6)
        }));

        assertEquals("setFirst", map.get(new Object[]{
            new HashSet<>(Arrays.asList(2, 3, 1)),
            Arrays.asList(4, 5, 6)
        }));

        assertEquals("setFirst", map.get(new Object[]{
            new LinkedHashSet<>(Arrays.asList(1, 3, 2)),
            Arrays.asList(4, 5, 6)
        }));

        // Should NOT match: List in different order
        assertNull(map.get(new Object[]{
            new HashSet<>(Arrays.asList(1, 2, 3)),
            Arrays.asList(4, 6, 5)
        }));

        assertNull(map.get(new Object[]{
            new HashSet<>(Arrays.asList(3, 2, 1)),
            Arrays.asList(6, 5, 4)
        }));

        assertEquals(1, map.size());
    }

    /**
     * Test: Multiple List and Set combinations in one key
     * Pattern: List + Set + List + Set
     */
    @Test
    void testMultipleListsAndSetsInterleaved() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Store: List(1,2) + Set(3,4) + List(5,6) + Set(7,8)
        map.put(new Object[]{
            Arrays.asList(1, 2),
            new HashSet<>(Arrays.asList(3, 4)),
            Arrays.asList(5, 6),
            new HashSet<>(Arrays.asList(7, 8))
        }, "complex");

        // Should match: Lists in exact order, Sets in any order
        assertEquals("complex", map.get(new Object[]{
            Arrays.asList(1, 2),           // List: must match exactly
            new HashSet<>(Arrays.asList(4, 3)),  // Set: any order
            Arrays.asList(5, 6),           // List: must match exactly
            new HashSet<>(Arrays.asList(8, 7))   // Set: any order
        }));

        // Should NOT match: First List in wrong order
        assertNull(map.get(new Object[]{
            Arrays.asList(2, 1),           // Wrong order
            new HashSet<>(Arrays.asList(3, 4)),
            Arrays.asList(5, 6),
            new HashSet<>(Arrays.asList(7, 8))
        }));

        // Should NOT match: Second List in wrong order
        assertNull(map.get(new Object[]{
            Arrays.asList(1, 2),
            new HashSet<>(Arrays.asList(4, 3)),
            Arrays.asList(6, 5),           // Wrong order
            new HashSet<>(Arrays.asList(7, 8))
        }));

        assertEquals(1, map.size());
    }

    /**
     * Test: String values in List and Set
     */
    @Test
    void testListThenSet_StringValues() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Store: List("a","b","c") + Set("x","y","z")
        map.put(new Object[]{
            Arrays.asList("a", "b", "c"),
            new HashSet<>(Arrays.asList("x", "y", "z"))
        }, "strings");

        // Should match: Different Set order
        assertEquals("strings", map.get(new Object[]{
            Arrays.asList("a", "b", "c"),
            new HashSet<>(Arrays.asList("z", "x", "y"))
        }));

        // Should NOT match: Different List order
        assertNull(map.get(new Object[]{
            Arrays.asList("b", "a", "c"),
            new HashSet<>(Arrays.asList("x", "y", "z"))
        }));

        assertEquals(1, map.size());
    }

    /**
     * Test: Empty List and Set combinations
     */
    @Test
    void testEmptyListAndSet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Empty List + Set(1,2,3)
        map.put(new Object[]{
            Collections.emptyList(),
            new HashSet<>(Arrays.asList(1, 2, 3))
        }, "emptyList");

        assertEquals("emptyList", map.get(new Object[]{
            Collections.emptyList(),
            new HashSet<>(Arrays.asList(3, 2, 1))
        }));

        // List(1,2,3) + Empty Set
        map.put(new Object[]{
            Arrays.asList(1, 2, 3),
            Collections.emptySet()
        }, "emptySet");

        assertEquals("emptySet", map.get(new Object[]{
            Arrays.asList(1, 2, 3),
            Collections.emptySet()
        }));

        // Empty List + Empty Set
        map.put(new Object[]{
            Collections.emptyList(),
            Collections.emptySet()
        }, "bothEmpty");

        assertEquals("bothEmpty", map.get(new Object[]{
            Collections.emptyList(),
            Collections.emptySet()
        }));

        assertEquals(3, map.size());
    }

    /**
     * Test: Single-element List and Set
     */
    @Test
    void testSingleElementListAndSet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        map.put(new Object[]{
            Arrays.asList(1),
            Collections.singleton(2)
        }, "singles");

        assertEquals("singles", map.get(new Object[]{
            Arrays.asList(1),
            Collections.singleton(2)
        }));

        // Single-element Set has no order ambiguity, but should still work
        assertEquals("singles", map.get(new Object[]{
            Arrays.asList(1),
            new HashSet<>(Collections.singletonList(2))
        }));

        assertEquals(1, map.size());
    }

    /**
     * Test: Nested structures - List containing a Set
     */
    @Test
    void testListContainingSet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Outer List contains: [element, Set(a,b,c)]
        List<Object> outerList = Arrays.asList(
            "element",
            new HashSet<>(Arrays.asList("a", "b", "c"))
        );

        map.put(outerList, "nestedSet");

        // Should match: Set inside List can be in any order
        assertEquals("nestedSet", map.get(Arrays.asList(
            "element",
            new HashSet<>(Arrays.asList("c", "a", "b"))
        )));

        assertEquals(1, map.size());
    }

    /**
     * Test: Nested structures - Set containing Lists
     * Note: Sets compare using equals(), so Lists in Sets must match exactly
     */
    @Test
    void testSetContainingLists() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Outer Set contains two Lists
        Set<List<Integer>> outerSet = new HashSet<>(Arrays.asList(
            Arrays.asList(1, 2),
            Arrays.asList(3, 4)
        ));

        map.put(outerSet, "nestedLists");

        // Should match: Outer Set order can vary, but inner Lists must match exactly
        assertEquals("nestedLists", map.get(new HashSet<>(Arrays.asList(
            Arrays.asList(3, 4),  // Set order changed
            Arrays.asList(1, 2)   // but List contents must be exact
        ))));

        // Should NOT match: Inner List order changed
        assertNull(map.get(new HashSet<>(Arrays.asList(
            Arrays.asList(2, 1),  // Wrong order in List
            Arrays.asList(3, 4)
        ))));

        assertEquals(1, map.size());
    }

    /**
     * Test: Remove operations with mixed List/Set keys
     */
    @Test
    void testRemoveWithMixedListSet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        map.put(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList(4, 5, 6))
        }, "value");

        // Remove with Set in different order should work
        assertEquals("value", map.remove(new Object[]{
            Arrays.asList(1, 2, 3),
            new HashSet<>(Arrays.asList(6, 5, 4))
        }));

        assertTrue(map.isEmpty());
    }

    /**
     * Test: Replace operations with mixed List/Set keys
     */
    @Test
    void testReplaceWithMixedListSet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        map.put(new Object[]{
            Arrays.asList("a", "b"),
            new HashSet<>(Arrays.asList("x", "y"))
        }, "old");

        // Replace with Set in different order should work
        assertEquals("old", map.replace(new Object[]{
            Arrays.asList("a", "b"),
            new HashSet<>(Arrays.asList("y", "x"))
        }, "new"));

        assertEquals("new", map.get(new Object[]{
            Arrays.asList("a", "b"),
            new HashSet<>(Arrays.asList("x", "y"))
        }));

        assertEquals(1, map.size());
    }

    /**
     * Test: ContainsKey with mixed List/Set keys
     */
    @Test
    void testContainsKeyWithMixedListSet() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        map.put(new Object[]{
            Arrays.asList(10, 20),
            new HashSet<>(Arrays.asList(30, 40))
        }, "value");

        // Should find with Set in different order
        assertTrue(map.containsKey(new Object[]{
            Arrays.asList(10, 20),
            new HashSet<>(Arrays.asList(40, 30))
        }));

        // Should NOT find with List in different order
        assertFalse(map.containsKey(new Object[]{
            Arrays.asList(20, 10),
            new HashSet<>(Arrays.asList(30, 40))
        }));

        assertEquals(1, map.size());
    }

    /**
     * Test: Multiple distinct keys with different List/Set combinations
     */
    @Test
    void testMultipleDistinctListSetKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Key 1: List(1,2) + Set(3,4)
        map.put(new Object[]{
            Arrays.asList(1, 2),
            new HashSet<>(Arrays.asList(3, 4))
        }, "key1");

        // Key 2: List(1,2) + Set(5,6) - Same List, different Set
        map.put(new Object[]{
            Arrays.asList(1, 2),
            new HashSet<>(Arrays.asList(5, 6))
        }, "key2");

        // Key 3: List(3,4) + Set(3,4) - Different List, Set equals first List
        map.put(new Object[]{
            Arrays.asList(3, 4),
            new HashSet<>(Arrays.asList(3, 4))
        }, "key3");

        assertEquals(3, map.size());

        // Verify each key is independently accessible
        assertEquals("key1", map.get(new Object[]{
            Arrays.asList(1, 2),
            new HashSet<>(Arrays.asList(4, 3))  // Set order varies
        }));

        assertEquals("key2", map.get(new Object[]{
            Arrays.asList(1, 2),
            new HashSet<>(Arrays.asList(6, 5))  // Set order varies
        }));

        assertEquals("key3", map.get(new Object[]{
            Arrays.asList(3, 4),
            new HashSet<>(Arrays.asList(4, 3))  // Set order varies
        }));

        // Should NOT match with List in wrong order
        assertNull(map.get(new Object[]{
            Arrays.asList(2, 1),
            new HashSet<>(Arrays.asList(3, 4))
        }));
    }

    /**
     * Test: List and Set with null elements
     */
    @Test
    void testListAndSetWithNullElements() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // List can contain nulls
        List<String> listWithNull = Arrays.asList("a", null, "b");
        Set<String> setWithNull = new HashSet<>(Arrays.asList("x", null, "y"));

        map.put(new Object[]{listWithNull, setWithNull}, "nulls");

        // Should match with Set in different order (null position can vary)
        assertEquals("nulls", map.get(new Object[]{
            Arrays.asList("a", null, "b"),  // List order must match
            new HashSet<>(Arrays.asList(null, "y", "x"))  // Set order varies
        }));

        // Should NOT match with List null in different position
        assertNull(map.get(new Object[]{
            Arrays.asList(null, "a", "b"),  // null moved
            new HashSet<>(Arrays.asList("x", "y", null))
        }));

        assertEquals(1, map.size());
    }

    /**
     * Test: Large List and Set combinations (performance check)
     */
    @Test
    void testLargeListAndSetCombinations() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Create large List (100 elements) and large Set (100 elements)
        List<Integer> largeList = new ArrayList<>();
        Set<Integer> largeSet = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            largeList.add(i);
            largeSet.add(i + 100);
        }

        map.put(new Object[]{largeList, largeSet}, "large");

        // Create lookup with Set in shuffled order
        List<Integer> shuffledSetElements = new ArrayList<>(largeSet);
        Collections.shuffle(shuffledSetElements);
        Set<Integer> shuffledSet = new HashSet<>(shuffledSetElements);

        assertEquals("large", map.get(new Object[]{largeList, shuffledSet}));

        // Create lookup with List in different order (should NOT match)
        List<Integer> shuffledList = new ArrayList<>(largeList);
        Collections.shuffle(shuffledList);

        assertNull(map.get(new Object[]{shuffledList, largeSet}));

        assertEquals(1, map.size());
    }

    /**
     * Test: List and Set equality comparison semantics
     * Verifies that List uses sequential equals() while Set uses unordered equals()
     */
    @Test
    void testListSetEqualitySemantics() {
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Store original
        map.put(new Object[]{
            Arrays.asList("alpha", "beta", "gamma"),
            new HashSet<>(Arrays.asList("one", "two", "three"))
        }, "original");

        // Different List type, same content and order -> should match
        assertEquals("original", map.get(new Object[]{
            new ArrayList<>(Arrays.asList("alpha", "beta", "gamma")),
            new HashSet<>(Arrays.asList("three", "one", "two"))
        }));

        // Different Set type, same content -> should match
        assertEquals("original", map.get(new Object[]{
            Arrays.asList("alpha", "beta", "gamma"),
            new TreeSet<>(Arrays.asList("two", "three", "one"))  // TreeSet sorts differently
        }));

        assertEquals(1, map.size());
    }
}
