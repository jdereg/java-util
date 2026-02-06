package com.cedarsoftware.util;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for bug: Storing the EMPTY_MAP sentinel value corrupts state detection.
 *
 * Bug: EMPTY_MAP is a static final String "_︿_ψ_☼" used as a sentinel
 * for the empty state. If a user stores this exact string as a value
 * (where the key matches getSingleValueKey()), val is set to that string,
 * and val == EMPTY_MAP becomes true due to string interning, making the
 * map appear empty even though it has one entry (silent data loss).
 *
 * Fix: Change EMPTY_MAP from a String to a unique Object instance that
 * cannot collide with any user-supplied value.
 */
class CompactMapSentinelValueTest {

    /** The sentinel string used internally by CompactMap */
    private static final String SENTINEL = "_︿_ψ_☼";

    /**
     * Storing the sentinel string as a value under the singleValueKey
     * (default "id") should not corrupt the map's state.
     */
    @Test
    void testPutSentinelValueOnSingleValueKey() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();

        map.put("id", SENTINEL);

        assertEquals(1, map.size(), "Map should have size 1");
        assertFalse(map.isEmpty(), "Map should not be empty");
        assertTrue(map.containsKey("id"), "Map should contain key 'id'");
        assertEquals(SENTINEL, map.get("id"), "Should retrieve the sentinel string as value");
        assertTrue(map.containsValue(SENTINEL), "Map should contain sentinel as a value");
    }

    /**
     * Overwriting an existing single entry with the sentinel value
     * (via handleSingleEntryPut) should not corrupt state.
     */
    @Test
    void testOverwriteWithSentinelValue() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();

        map.put("id", "original");
        assertEquals("original", map.get("id"));

        // Overwrite with sentinel
        Object prev = map.put("id", SENTINEL);
        assertEquals("original", prev, "Previous value should be returned");
        assertEquals(1, map.size(), "Map should still have size 1");
        assertFalse(map.isEmpty(), "Map should not be empty");
        assertEquals(SENTINEL, map.get("id"), "Should retrieve the sentinel string");
    }

    /**
     * After removing the only entry with sentinel value, the map
     * should properly be empty.
     */
    @Test
    void testRemoveSentinelValue() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();

        map.put("id", SENTINEL);
        assertEquals(1, map.size());

        Object removed = map.remove("id");
        assertEquals(SENTINEL, removed, "Remove should return the sentinel value");
        assertEquals(0, map.size(), "Map should be empty after remove");
        assertTrue(map.isEmpty(), "Map should be empty after remove");
    }

    /**
     * The sentinel string should work fine as a value in compact array state.
     */
    @Test
    void testSentinelValueInCompactArray() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();

        map.put("key1", SENTINEL);
        map.put("key2", "normal");

        assertEquals(2, map.size());
        assertEquals(SENTINEL, map.get("key1"));
        assertEquals("normal", map.get("key2"));
    }

    /**
     * Transition from compact array to single entry when the remaining
     * entry has the sentinel as its value.
     */
    @Test
    void testTransitionToSingleEntryWithSentinelValue() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();

        map.put("id", SENTINEL);
        map.put("other", "value2");
        assertEquals(2, map.size());

        // Remove the other entry, leaving only the sentinel-valued entry
        map.remove("other");
        assertEquals(1, map.size(), "Map should have 1 entry after removal");
        assertFalse(map.isEmpty(), "Map should not be empty");
        assertEquals(SENTINEL, map.get("id"), "Sentinel value should be retrievable");
    }

    /**
     * Iterator should correctly handle entries with sentinel values.
     */
    @Test
    void testIteratorWithSentinelValue() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();

        map.put("id", SENTINEL);

        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        assertTrue(it.hasNext(), "Iterator should have one entry");
        Map.Entry<String, Object> entry = it.next();
        assertEquals("id", entry.getKey());
        assertEquals(SENTINEL, entry.getValue());
        assertFalse(it.hasNext(), "Iterator should have no more entries");
    }

    /**
     * clear() followed by put of sentinel should work correctly.
     */
    @Test
    void testClearThenPutSentinel() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();

        map.put("id", "normal");
        map.clear();
        assertTrue(map.isEmpty());

        map.put("id", SENTINEL);
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertEquals(SENTINEL, map.get("id"));
    }

    /**
     * putAll with sentinel values should not corrupt state.
     */
    @Test
    void testPutAllWithSentinelValue() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
        CompactMap<String, Object> source = CompactMap.<String, Object>builder().build();

        source.put("id", SENTINEL);
        map.putAll(source);

        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertEquals(SENTINEL, map.get("id"));
    }
}
