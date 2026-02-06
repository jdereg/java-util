package com.cedarsoftware.util;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test for bug: ConcurrentModificationException detection uses size, not modCount.
 *
 * Bug: The iterator checks expectedSize != size() to detect concurrent modification.
 * If one entry is added and another removed during iteration (net size unchanged),
 * the modification goes undetected — unlike standard Java collections which use modCount.
 *
 * Fix: Add a modCount field that increments on every structural modification
 * (add/remove). The iterator tracks expectedModCount and checks it instead of size.
 */
class CompactMapConcurrentModDetectionTest {

    /**
     * Add a new key and remove a different key during iteration.
     * Size stays the same, but structure changed. Should throw CME.
     * Uses 2-entry map so there's a second next() call to detect the modification.
     */
    @Test
    void testAddAndRemoveSameSizeDetected_singleEntry() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        map.put("a", 1);
        map.put("b", 2);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        it.next();

        // Remove "b" and add "c" — size stays 2
        map.remove("b");
        map.put("c", 3);

        assertThrows(ConcurrentModificationException.class, () -> it.next(),
                "Should detect structural modification even though size is unchanged");
    }

    /**
     * Same scenario in compact array state.
     */
    @Test
    void testAddAndRemoveSameSizeDetected_compactArray() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        it.next();

        // Remove "c" and add "d" — size stays 3
        map.remove("c");
        map.put("d", 4);

        assertThrows(ConcurrentModificationException.class, () -> it.next(),
                "Should detect add+remove in compact array state");
    }

    /**
     * Same scenario in Map state.
     */
    @Test
    void testAddAndRemoveSameSizeDetected_mapState() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(2).build();
        // 3 entries > compactSize(2) → Map state
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        it.next();

        // Remove "c" and add "d" — size stays 3
        map.remove("c");
        map.put("d", 4);

        assertThrows(ConcurrentModificationException.class, () -> it.next(),
                "Should detect add+remove in map state");
    }

    /**
     * clear() followed by re-population to same size should be detected.
     */
    @Test
    void testClearAndRepopulateDetected() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        map.put("a", 1);
        map.put("b", 2);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        it.next();

        // Clear and re-add same entries — size returns to 2
        map.clear();
        map.put("a", 1);
        map.put("b", 2);

        assertThrows(ConcurrentModificationException.class, () -> it.next(),
                "Should detect clear + re-populate even though size is the same");
    }

    /**
     * Iterator remove() should still work correctly (no false CME).
     */
    @Test
    void testIteratorRemoveDoesNotTriggerFalseCME() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        it.next();
        it.remove(); // Should NOT throw CME
        it.next();   // Should NOT throw CME
        it.remove(); // Should NOT throw CME
        it.next();   // Should NOT throw CME
        it.remove(); // Should NOT throw CME
    }

    /**
     * Normal iteration without modification should not throw CME.
     */
    @Test
    void testNormalIterationNoCME() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        int count = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            count++;
        }
        assert count == 3;
    }
}
