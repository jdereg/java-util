package com.cedarsoftware.util;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for bug: removeFromMap doesn't sort the resulting array.
 *
 * Bug: When transitioning from Map back to Object[], entries are copied
 * from the map's entrySet() iteration order. For sorted/reverse CompactMaps,
 * subsequent containsKey()/get() use binary search via CompactMapComparator,
 * which expects sorted order. If the backing map's iteration order doesn't
 * exactly match CompactMapComparator's order, binary search fails to find keys.
 *
 * This is triggered when a legacy subclass provides a sorted TreeMap with
 * natural ordering but isCaseInsensitive() returns true. The TreeMap iterates
 * in natural (case-sensitive) order, but CompactMapComparator uses
 * case-insensitive ordering. Example: natural order puts "Banana" before
 * "apple" (uppercase before lowercase), but case-insensitive order puts
 * "apple" before "Banana" (alphabetically).
 *
 * Fix: Call sortCompactArray() after building the array in removeFromMap()
 * and in the iterator's Map-to-array transition path.
 */
class CompactMapRemoveFromMapSortTest {

    /**
     * Legacy-style subclass: sorted + case-insensitive, but the TreeMap
     * uses natural ordering (no case-insensitive comparator).
     * This creates a mismatch between the Map's iteration order and
     * CompactMapComparator's expected order.
     */
    private static class SortedCaseInsensitiveLegacyMap<K, V> extends CompactMap<K, V> {
        @Override
        protected int compactSize() { return 3; }

        @Override
        protected boolean isCaseInsensitive() { return true; }

        @Override
        protected String getOrdering() { return SORTED; }

        @Override
        protected Map<K, V> getNewMap() {
            // Plain TreeMap uses natural String ordering (case-sensitive).
            // CompactMapComparator uses case-insensitive ordering.
            // These produce different orderings for mixed-case keys.
            return new TreeMap<>();
        }
    }

    /**
     * After removing an entry that triggers Map-to-array transition,
     * all remaining keys should still be findable via containsKey()/get().
     *
     * Keys chosen to expose the ordering mismatch:
     * - Natural order:          "Banana" < "Cherry" < "apple" < "date"
     * - Case-insensitive order: "apple" < "Banana" < "Cherry" < "date"
     */
    @Test
    void testRemoveFromMapSortsArrayForCaseInsensitiveSorted() {
        CompactMap<String, Integer> map = new SortedCaseInsensitiveLegacyMap<>();

        // compactSize=3, so 4 entries transitions to Map state
        map.put("apple", 1);
        map.put("Banana", 2);
        map.put("Cherry", 3);
        map.put("date", 4);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType(),
                "Should be in MAP state with 4 entries (compactSize=3)");

        // Remove one entry → size drops to 3 == compactSize → transitions to Object[]
        map.remove("date");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType(),
                "Should transition to ARRAY state after removal");
        assertEquals(3, map.size());

        // These should all be findable via binary search.
        // Before fix: binary search uses case-insensitive order but array is in
        // natural order ["Banana","Cherry","apple"], so "apple" can't be found.
        assertTrue(map.containsKey("apple"),
                "containsKey('apple') should return true after Map-to-array transition");
        assertTrue(map.containsKey("Banana"),
                "containsKey('Banana') should return true after Map-to-array transition");
        assertTrue(map.containsKey("Cherry"),
                "containsKey('Cherry') should return true after Map-to-array transition");

        assertEquals(Integer.valueOf(1), map.get("apple"),
                "get('apple') should return 1 after Map-to-array transition");
        assertEquals(Integer.valueOf(2), map.get("Banana"),
                "get('Banana') should return 2 after Map-to-array transition");
        assertEquals(Integer.valueOf(3), map.get("Cherry"),
                "get('Cherry') should return 3 after Map-to-array transition");
    }

    /**
     * Same test but removing a different key to ensure it's not key-specific.
     */
    @Test
    void testRemoveFromMapDifferentKeyRemoved() {
        CompactMap<String, Integer> map = new SortedCaseInsensitiveLegacyMap<>();

        map.put("apple", 1);
        map.put("Banana", 2);
        map.put("Cherry", 3);
        map.put("date", 4);

        // Remove "Cherry" → transitions back to array
        map.remove("Cherry");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        assertTrue(map.containsKey("apple"), "Should find 'apple'");
        assertTrue(map.containsKey("Banana"), "Should find 'Banana'");
        assertTrue(map.containsKey("date"), "Should find 'date'");

        assertEquals(Integer.valueOf(1), map.get("apple"));
        assertEquals(Integer.valueOf(2), map.get("Banana"));
        assertEquals(Integer.valueOf(4), map.get("date"));
    }

    /**
     * Test the iterator remove path: when iterator.remove() triggers
     * Map-to-array transition, the resulting array must also be sorted.
     */
    @Test
    void testIteratorRemoveTriggeringTransitionSortsArray() {
        CompactMap<String, Integer> map = new SortedCaseInsensitiveLegacyMap<>();

        map.put("apple", 1);
        map.put("Banana", 2);
        map.put("Cherry", 3);
        map.put("date", 4);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // Remove one entry via iterator to trigger transition
        java.util.Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            if ("date".equalsIgnoreCase(entry.getKey())) {
                it.remove();
                break;
            }
        }

        assertEquals(3, map.size());
        // May or may not have transitioned depending on iterator path,
        // but if it did transition, the array must be sorted.
        // After all iteration and removal, verify all remaining keys are findable.
        assertTrue(map.containsKey("apple"), "Should find 'apple' after iterator remove");
        assertTrue(map.containsKey("Banana"), "Should find 'Banana' after iterator remove");
        assertTrue(map.containsKey("Cherry"), "Should find 'Cherry' after iterator remove");

        assertEquals(Integer.valueOf(1), map.get("apple"));
        assertEquals(Integer.valueOf(2), map.get("Banana"));
        assertEquals(Integer.valueOf(3), map.get("Cherry"));
    }

    /**
     * Verify that after the transition, new puts still work correctly
     * (the array stays sorted and binary search finds the right insertion point).
     */
    @Test
    void testPutAfterRemoveFromMapTransition() {
        CompactMap<String, Integer> map = new SortedCaseInsensitiveLegacyMap<>();

        map.put("apple", 1);
        map.put("Banana", 2);
        map.put("Cherry", 3);
        map.put("date", 4);

        map.remove("date");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Put a new entry into the array — binary search must find correct insertion point
        map.put("avocado", 5);
        assertNotNull(map.get("avocado"), "New entry should be findable");
        assertEquals(Integer.valueOf(5), map.get("avocado"));

        // Existing entries should still be findable
        assertEquals(Integer.valueOf(1), map.get("apple"));
        assertEquals(Integer.valueOf(2), map.get("Banana"));
        assertEquals(Integer.valueOf(3), map.get("Cherry"));
    }
}
