package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for CompactMap correctness bugs found by Codex analysis:
 *
 * 1. entrySet().remove()/removeAll() ignored entry values — removed by key only
 * 2. CompactMapComparator.compare() returned 0 for distinct non-Comparable same-class keys
 * 3. Legacy isCaseInsensitive()==true + HashMap backing silently lost CI in MAP state
 */
class CompactMapCorrectnessTest {

    // ---------------------------------------------------------------
    // Bug 1: entrySet().remove() must check both key AND value
    // ---------------------------------------------------------------

    @Test
    void testEntrySetRemoveChecksValue_singleEntry() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("k", "correct");

        // Try to remove with wrong value — should NOT remove
        assertFalse(map.entrySet().remove(new AbstractMap.SimpleEntry<>("k", "wrong")));
        assertEquals(1, map.size());
        assertEquals("correct", map.get("k"));

        // Remove with correct value — should remove
        assertTrue(map.entrySet().remove(new AbstractMap.SimpleEntry<>("k", "correct")));
        assertEquals(0, map.size());
    }

    @Test
    void testEntrySetRemoveChecksValue_arrayState() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // Wrong value — should NOT remove
        assertFalse(map.entrySet().remove(new AbstractMap.SimpleEntry<>("b", 999)));
        assertEquals(3, map.size());
        assertEquals(2, map.get("b"));

        // Correct value — should remove
        assertTrue(map.entrySet().remove(new AbstractMap.SimpleEntry<>("b", 2)));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("b"));
    }

    @Test
    void testEntrySetRemoveChecksValue_nullValue() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("k", null);

        // Wrong value (non-null vs null stored) — should NOT remove
        assertFalse(map.entrySet().remove(new AbstractMap.SimpleEntry<>("k", "x")));
        assertEquals(1, map.size());

        // Correct value (null matches null) — should remove
        assertTrue(map.entrySet().remove(new AbstractMap.SimpleEntry<>("k", null)));
        assertEquals(0, map.size());
    }

    @Test
    void testEntrySetRemoveAllChecksValues() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // removeAll with wrong values — should NOT remove anything
        assertFalse(map.entrySet().removeAll(
                Collections.singleton(new AbstractMap.SimpleEntry<>("b", 999))));
        assertEquals(3, map.size());

        // removeAll with correct value — should remove only matching entry
        assertTrue(map.entrySet().removeAll(
                Collections.singleton(new AbstractMap.SimpleEntry<>("b", 2))));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("b"));
    }

    // ---------------------------------------------------------------
    // Bug 2: CompactMapComparator must not return 0 for distinct
    //        non-Comparable keys of the same class
    // ---------------------------------------------------------------

    /** A non-Comparable class for testing. */
    static class NonComparableKey {
        final String label;
        NonComparableKey(String label) { this.label = label; }
        @Override public String toString() { return "NCK(" + label + ")"; }
        @Override public int hashCode() { return label.hashCode(); }
        @Override public boolean equals(Object o) {
            return o instanceof NonComparableKey && ((NonComparableKey) o).label.equals(label);
        }
    }

    @Test
    void testComparatorDistinguishesNonComparableSameClassKeys() {
        NonComparableKey k1 = new NonComparableKey("alpha");
        NonComparableKey k2 = new NonComparableKey("beta");

        CompactMap.CompactMapComparator comp = CompactMap.CompactMapComparator.get(false, false);
        int result = comp.compare(k1, k2);

        // Must not return 0 for distinct objects
        assertTrue(result != 0,
                "Comparator must not return 0 for distinct non-Comparable keys of the same class");
    }

    @Test
    void testSortedCompactMapPreservesDistinctNonComparableKeys() {
        CompactMap<NonComparableKey, String> map = CompactMap.<NonComparableKey, String>builder()
                .sortedOrder()
                .build();

        NonComparableKey k1 = new NonComparableKey("alpha");
        NonComparableKey k2 = new NonComparableKey("beta");

        map.put(k1, "v1");
        map.put(k2, "v2");

        // Both entries must survive — second put must NOT overwrite first
        assertEquals(2, map.size(), "Sorted map must preserve both distinct non-Comparable keys");
        assertEquals("v1", map.get(k1));
        assertEquals("v2", map.get(k2));
    }

    // ---------------------------------------------------------------
    // Bug 3: Legacy isCaseInsensitive()==true + HashMap backing
    // ---------------------------------------------------------------

    @Test
    void testLegacyCIMisconfigurationDetected() {
        // Legacy subclass that says CI=true but returns plain HashMap
        assertThrows(IllegalStateException.class, () -> {
            new CompactMap<String, String>() {
                @Override
                protected boolean isCaseInsensitive() { return true; }
                @Override
                protected Map<String, String> getNewMap() { return new HashMap<>(); }
            };
        });
    }

    @Test
    void testLegacyCIWithCaseInsensitiveMapIsAllowed() {
        // Properly configured legacy subclass — should NOT throw
        CompactMap<String, String> map = new CompactMap<String, String>() {
            @Override
            protected boolean isCaseInsensitive() { return true; }
            @Override
            protected Map<String, String> getNewMap() {
                return new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>(compactSize() + 1));
            }
        };
        map.put("Hello", "world");
        assertEquals("world", map.get("hello"));
    }

    @Test
    void testRecursiveCompactMapBackingDetected() {
        // CompactMap as backing map is recursive and wasteful — should be rejected
        assertThrows(IllegalStateException.class, () -> {
            new CompactMap<String, String>() {
                @Override
                protected Map<String, String> getNewMap() { return new CompactMap<>(); }
            };
        });
    }

    @Test
    void testRecursiveCompactCIHashMapBackingDetected() {
        // CompactCIHashMap as backing map for a CI CompactMap is recursive — should be rejected
        assertThrows(IllegalStateException.class, () -> {
            new CompactMap<String, String>() {
                @Override
                protected boolean isCaseInsensitive() { return true; }
                @Override
                protected Map<String, String> getNewMap() { return new CompactCIHashMap<>(); }
            };
        });
    }
}
