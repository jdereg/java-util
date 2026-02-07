package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for correctness bugs in AbstractConcurrentNullSafeMap.
 */
class AbstractConcurrentNullSafeMapBugFixTest {

    // --- Bug 1: merge() invokes remapping function when existing value is null ---

    @Test
    void testMergeWithNullExistingValue_shouldInsertDirectly() {
        ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();
        map.put("key", null);

        // Per merge() contract: if existing value is null, just insert the new value
        // without calling the remapping function
        String result = map.merge("key", "newVal", (old, nw) -> {
            throw new AssertionError("Remapping function should NOT be called when existing value is null");
        });

        assertEquals("newVal", result);
        assertEquals("newVal", map.get("key"));
    }

    @Test
    void testMergeWithNullExistingValue_absentKey() {
        ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();

        // Key absent — should insert without calling function
        String result = map.merge("key", "val", (old, nw) -> {
            throw new AssertionError("Should not be called for absent key");
        });

        assertEquals("val", result);
        assertEquals("val", map.get("key"));
    }

    @Test
    void testMergeWithNonNullExistingValue_shouldCallFunction() {
        ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();
        map.put("key", "old");

        String result = map.merge("key", "new", (old, nw) -> old + "+" + nw);

        assertEquals("old+new", result);
        assertEquals("old+new", map.get("key"));
    }

    @Test
    void testMergeWithNullKey() {
        ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();
        map.put(null, null);

        // null key mapped to null value — should insert directly
        String result = map.merge(null, "val", (old, nw) -> {
            throw new AssertionError("Should not be called when existing value is null");
        });

        assertEquals("val", result);
        assertEquals("val", map.get(null));
    }

    @Test
    void testMergeFunctionReturnsNull_removesEntry() {
        ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();
        map.put("key", "old");

        // When function returns null, entry should be removed
        String result = map.merge("key", "new", (old, nw) -> null);

        assertNull(result);
        assertFalse(map.containsKey("key"));
    }

    // --- Bug 2: equals() double lookup ---

    @Test
    void testEqualsWithStandardHashMap() {
        ConcurrentHashMapNullSafe<String, Integer> safe = new ConcurrentHashMapNullSafe<>();
        safe.put("a", 1);
        safe.put("b", 2);
        safe.put("c", null);

        Map<String, Integer> hash = new HashMap<>();
        hash.put("a", 1);
        hash.put("b", 2);
        hash.put("c", null);

        assertTrue(safe.equals(hash));
        assertTrue(hash.equals(safe));
        assertEquals(safe.hashCode(), hash.hashCode());
    }

    @Test
    void testEqualsWithNullKey() {
        ConcurrentHashMapNullSafe<String, Integer> safe = new ConcurrentHashMapNullSafe<>();
        safe.put(null, 1);
        safe.put("a", 2);

        Map<String, Integer> hash = new HashMap<>();
        hash.put(null, 1);
        hash.put("a", 2);

        assertTrue(safe.equals(hash));
        assertTrue(hash.equals(safe));
        assertEquals(safe.hashCode(), hash.hashCode());
    }

    @Test
    void testHashCodeConsistency() {
        ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();
        map.put("a", "1");
        map.put(null, null);
        map.put("b", null);

        int h1 = map.hashCode();
        int h2 = map.hashCode();
        assertEquals(h1, h2, "hashCode must be consistent across calls");
    }
}
