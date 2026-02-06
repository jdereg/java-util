package com.cedarsoftware.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for iterator remove() during Map-to-array transition in CompactMap.
 *
 * Bug: When iterating a Map-backed CompactMap and calling iterator.remove()
 * causes the size to drop to compactSize(), the code bypasses mapIterator.remove()
 * and instead calls CompactMap.this.remove() directly, which triggers a
 * Map-to-Object[] transition. The iterator then switches from map-based
 * iteration to array-based iteration using an index that was tracking map
 * position — a fragile coupling that depends on the backing map maintaining
 * stable iteration order after direct (non-iterator) removal.
 *
 * Fix: Always use mapIterator.remove() when in Map state, deferring the
 * compact array transition to the next non-iterator mutation.
 */
class CompactMapIteratorRemoveTransitionTest {

    /**
     * Remove during iteration triggers Map-to-array transition.
     * Verifies all entries are visited exactly once.
     */
    @Test
    void testIteratorRemoveTriggeringMapToArrayTransition() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(4)
                .build();

        for (int i = 1; i <= 5; i++) {
            map.put("key" + i, i);
        }
        assertEquals(5, map.size());

        Set<String> visited = new HashSet<>();
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            visited.add(entry.getKey());
            if (visited.size() == 1) {
                it.remove();
            }
        }

        assertEquals(5, visited.size(), "Should visit all 5 entries. Visited: " + visited);
        assertEquals(4, map.size());
    }

    /**
     * Remove in the middle of iteration triggers transition.
     */
    @Test
    void testIteratorRemoveInMiddleTriggeringTransition() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(4)
                .build();

        for (int i = 1; i <= 5; i++) {
            map.put("key" + i, i);
        }

        Set<String> visited = new HashSet<>();
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        int count = 0;
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            visited.add(entry.getKey());
            count++;
            if (count == 3) {
                it.remove();
            }
        }

        assertEquals(5, visited.size(), "Should visit all 5 entries. Visited: " + visited);
        assertEquals(4, map.size());
    }

    /**
     * keySet iterator also handles the transition correctly.
     */
    @Test
    void testKeySetIteratorRemoveTriggeringTransition() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(4)
                .build();

        for (int i = 1; i <= 5; i++) {
            map.put("key" + i, i);
        }

        Set<String> visited = new HashSet<>();
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            visited.add(key);
            if (visited.size() == 1) {
                it.remove();
            }
        }

        assertEquals(5, visited.size(), "Should visit all 5 keys. Visited: " + visited);
        assertEquals(4, map.size());
    }

    /**
     * Multiple removes that cross the transition boundary.
     * First remove keeps Map state, second triggers transition.
     */
    @Test
    void testMultipleRemovesCrossingTransition() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(4)
                .build();

        for (int i = 1; i <= 6; i++) {
            map.put("key" + i, i);
        }
        assertEquals(6, map.size());

        Set<String> visited = new HashSet<>();
        Set<String> removed = new HashSet<>();
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        int removeCount = 0;
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            visited.add(entry.getKey());
            if (removeCount < 2) {
                it.remove();
                removed.add(entry.getKey());
                removeCount++;
            }
        }

        assertEquals(6, visited.size(), "Should visit all 6 entries. Visited: " + visited);
        assertEquals(4, map.size());
        for (String key : visited) {
            if (!removed.contains(key)) {
                assertTrue(map.containsKey(key), "Key " + key + " should still be in map");
            }
        }
    }

    /**
     * After iterator remove with transition, the map remains fully functional.
     */
    @Test
    void testMapFunctionalAfterIteratorRemoveTransition() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(4)
                .build();

        for (int i = 1; i <= 5; i++) {
            map.put("key" + i, i);
        }

        Iterator<String> it = map.keySet().iterator();
        String removedKey = it.next();
        it.remove();

        while (it.hasNext()) {
            it.next();
        }

        assertEquals(4, map.size());
        assertFalse(map.containsKey(removedKey));

        // Map should still be fully functional
        map.put("newKey", 99);
        assertEquals(99, (int) map.get("newKey"));
        map.remove("newKey");
        assertEquals(4, map.size());
    }

    /**
     * After the fix, iterator.remove() in Map state should use mapIterator.remove()
     * and defer the compact array transition. The map may stay in Map state with
     * compactSize entries until the next non-iterator mutation.
     */
    @Test
    void testDeferredTransitionAfterIteratorRemove() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(4)
                .build();

        for (int i = 1; i <= 5; i++) {
            map.put("key" + i, i);
        }

        // Remove one entry via iterator, bringing size to 4 == compactSize
        Iterator<String> it = map.keySet().iterator();
        it.next();
        it.remove();

        // Exhaust the iterator
        while (it.hasNext()) {
            it.next();
        }

        assertEquals(4, map.size());

        // The next put/remove outside iteration should cause proper transition
        // Map should still work correctly regardless of internal state
        map.put("extra", 100);
        assertEquals(5, map.size());
        assertEquals(100, (int) map.get("extra"));
    }
}
