package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for entrySet contains() and remove() methods inherited from
 * {@link AbstractConcurrentNullSafeMap}.
 */
class AbstractConcurrentNullSafeMapEntrySetTest {

    @Test
    void testEntrySetContains() {
        ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();
        map.put("a", "alpha");
        map.put(null, "nullVal");
        map.put("b", null);

        Set<Map.Entry<String, String>> entries = map.entrySet();

        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>("a", "alpha")));
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>(null, "nullVal")));
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>("b", null)));
        assertFalse(entries.contains(new AbstractMap.SimpleEntry<>("c", "gamma")));
    }

    @Test
    void testEntrySetRemove() {
        ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();
        map.put("a", "alpha");
        map.put(null, "nullVal");
        map.put("b", null);

        Set<Map.Entry<String, String>> entries = map.entrySet();

        assertTrue(entries.remove(new AbstractMap.SimpleEntry<>("a", "alpha")));
        assertFalse(map.containsKey("a"));

        assertTrue(entries.remove(new AbstractMap.SimpleEntry<>(null, "nullVal")));
        assertFalse(map.containsKey(null));

        assertFalse(entries.remove(new AbstractMap.SimpleEntry<>("b", "beta")));
        assertTrue(map.containsKey("b"));

        assertTrue(entries.remove(new AbstractMap.SimpleEntry<>("b", null)));
        assertFalse(map.containsKey("b"));
    }

    @Test
    void testEntrySetEntryEqualityHashAndToString() {
        ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();
        map.put("a", "alpha");
        map.put(null, "nullVal");
        map.put("b", null);

        for (Map.Entry<String, String> entry : map.entrySet()) {
            Map.Entry<String, String> other = new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue());
            assertTrue(entry.equals(other));
            assertEquals(other.hashCode(), entry.hashCode());

            String expected = entry.getKey() + "=" + entry.getValue();
            assertEquals(expected, entry.toString());
            assertFalse(entry.toString().contains("@"));
        }
    }
}
