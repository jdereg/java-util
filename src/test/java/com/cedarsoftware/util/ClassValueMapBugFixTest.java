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
}
