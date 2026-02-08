package com.cedarsoftware.util;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for correctness bugs in ClassValueMap:
 * Bug 1: remove(null, value) uses identity comparison (==) instead of equals()
 * Bug 2: replace(null, oldValue, newValue) uses identity comparison (==) instead of equals()
 * Bug 3: computeIfAbsent() returns computed value but doesn't update map when key maps to null
 * Bug 4: compute() uses default ConcurrentMap impl that can't distinguish absent from null-mapped
 */
class ClassValueMapBugFixTest {

    // --- Bug 1: remove(null, value) identity comparison ---

    @Test
    void testRemoveNullKeyWithEqualsEquivalentValue() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(null, "hello");

        // Use new String() to ensure a different object reference (not interned)
        boolean removed = map.remove(null, new String("hello"));
        assertTrue(removed, "remove(null, value) should use equals(), not ==");
        assertFalse(map.containsKey(null));
    }

    @Test
    void testRemoveNullKeyWithNullValue() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(null, null);

        // null == null works with identity comparison, but verify it works
        boolean removed = map.remove(null, null);
        assertTrue(removed);
        assertFalse(map.containsKey(null));
    }

    @Test
    void testRemoveNullKeyWithWrongValue() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(null, "hello");

        boolean removed = map.remove(null, "world");
        assertFalse(removed, "Should not remove when value doesn't match");
        assertTrue(map.containsKey(null));
        assertEquals("hello", map.get(null));
    }

    @Test
    void testRemoveNullKeyWithNonStringEquals() {
        // Use Integer beyond cache range to force different object references
        ClassValueMap<Integer> map = new ClassValueMap<>();
        map.put(null, 1000);

        boolean removed = map.remove(null, new Integer(1000));
        assertTrue(removed, "remove(null, value) should use equals(), not ==");
        assertFalse(map.containsKey(null));
    }

    @Test
    void testRemoveNullKeyNoMapping() {
        ClassValueMap<String> map = new ClassValueMap<>();

        boolean removed = map.remove(null, "hello");
        assertFalse(removed, "Should return false when no mapping exists");
    }

    // --- Bug 2: replace(null, oldValue, newValue) identity comparison ---

    @Test
    void testReplaceNullKeyWithEqualsEquivalentOldValue() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(null, "hello");

        // Use new String() to ensure different object reference
        boolean replaced = map.replace(null, new String("hello"), "world");
        assertTrue(replaced, "replace(null, old, new) should use equals(), not ==");
        assertEquals("world", map.get(null));
    }

    @Test
    void testReplaceNullKeyWithNullOldValue() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(null, null);

        boolean replaced = map.replace(null, null, "world");
        assertTrue(replaced);
        assertEquals("world", map.get(null));
    }

    @Test
    void testReplaceNullKeyWithWrongOldValue() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(null, "hello");

        boolean replaced = map.replace(null, "wrong", "world");
        assertFalse(replaced, "Should not replace when oldValue doesn't match");
        assertEquals("hello", map.get(null));
    }

    @Test
    void testReplaceNullKeyWithNonStringEquals() {
        ClassValueMap<Integer> map = new ClassValueMap<>();
        map.put(null, 1000);

        boolean replaced = map.replace(null, new Integer(1000), 2000);
        assertTrue(replaced, "replace(null, old, new) should use equals(), not ==");
        assertEquals(Integer.valueOf(2000), map.get(null));
    }

    @Test
    void testReplaceNullKeyNoMapping() {
        ClassValueMap<String> map = new ClassValueMap<>();

        boolean replaced = map.replace(null, "hello", "world");
        assertFalse(replaced, "Should return false when no mapping exists");
    }

    // --- Bug 3: computeIfAbsent() with null-value mappings ---

    @Test
    void testComputeIfAbsentWithNullValueMapping_NonNullKey() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, null);  // key exists, mapped to null

        // Per Map spec: "or is mapped to null" → should compute
        String result = map.computeIfAbsent(String.class, k -> "computed");

        // The computed value should be stored AND returned
        assertEquals("computed", result, "computeIfAbsent should return computed value");
        assertEquals("computed", map.get(String.class), "computeIfAbsent should store computed value in map");
    }

    @Test
    void testComputeIfAbsentWithNullValueMapping_NullKey() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(null, null);  // null key exists, mapped to null

        String result = map.computeIfAbsent(null, k -> "computed");

        assertEquals("computed", result, "computeIfAbsent should return computed value for null key");
        assertEquals("computed", map.get(null), "computeIfAbsent should store computed value for null key");
    }

    @Test
    void testComputeIfAbsentWithExistingNonNullValue() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "existing");

        String result = map.computeIfAbsent(String.class, k -> "computed");

        assertEquals("existing", result, "Should return existing non-null value");
        assertEquals("existing", map.get(String.class), "Should not modify existing non-null value");
    }

    @Test
    void testComputeIfAbsentWithAbsentKey() {
        ClassValueMap<String> map = new ClassValueMap<>();

        String result = map.computeIfAbsent(String.class, k -> "computed");

        assertEquals("computed", result);
        assertEquals("computed", map.get(String.class));
    }

    @Test
    void testComputeIfAbsentFunctionReturnsNull() {
        ClassValueMap<String> map = new ClassValueMap<>();

        String result = map.computeIfAbsent(String.class, k -> null);

        assertNull(result, "Should return null when function returns null");
        assertFalse(map.containsKey(String.class), "Should not create mapping when function returns null");
    }

    @Test
    void testComputeIfAbsentNullMappedFunctionReturnsNull() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, null);

        String result = map.computeIfAbsent(String.class, k -> null);

        assertNull(result, "Should return null when function returns null");
        assertTrue(map.containsKey(String.class), "Null mapping should remain when function returns null");
    }

    // --- Bug 4: compute() with null-value mappings ---

    @Test
    void testComputeWithNullValueMapping_ReturnsNonNull() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, null);  // key exists, mapped to null

        String result = map.compute(String.class, (k, oldVal) -> {
            assertNull(oldVal, "Old value should be null");
            return "new";
        });

        assertEquals("new", result, "compute should return new value");
        assertEquals("new", map.get(String.class), "compute should store new value");
    }

    @Test
    void testComputeWithNullValueMapping_ReturnsNull_RemovesMapping() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, null);  // key exists, mapped to null

        String result = map.compute(String.class, (k, oldVal) -> null);

        assertNull(result, "compute should return null");
        assertFalse(map.containsKey(String.class), "compute returning null should remove the mapping");
    }

    @Test
    void testComputeWithNullValueMapping_NullKey() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(null, null);  // null key mapped to null

        String result = map.compute(null, (k, oldVal) -> {
            assertNull(k, "Key should be null");
            assertNull(oldVal, "Old value should be null");
            return "computed";
        });

        assertEquals("computed", result);
        assertEquals("computed", map.get(null));
    }

    @Test
    void testComputeWithAbsentKey() {
        ClassValueMap<String> map = new ClassValueMap<>();

        String result = map.compute(String.class, (k, oldVal) -> {
            assertNull(oldVal, "Old value should be null for absent key");
            return "new";
        });

        assertEquals("new", result);
        assertEquals("new", map.get(String.class));
    }

    @Test
    void testComputeWithAbsentKey_ReturnsNull() {
        ClassValueMap<String> map = new ClassValueMap<>();

        String result = map.compute(String.class, (k, oldVal) -> null);

        assertNull(result);
        assertFalse(map.containsKey(String.class), "No mapping should be created");
    }

    @Test
    void testComputeReplaceExistingNonNull() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "old");

        String result = map.compute(String.class, (k, oldVal) -> {
            assertEquals("old", oldVal);
            return "new";
        });

        assertEquals("new", result);
        assertEquals("new", map.get(String.class));
    }
}
