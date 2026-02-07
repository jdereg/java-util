package com.cedarsoftware.util;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for the wrapEntry() snapshot bug in ConcurrentNavigableMapNullSafe.
 *
 * Bug: wrapEntry().getValue() does a live lookup via internalMap.get(keyObj) instead
 * of using the entry's snapshot value. This means if the map is modified between
 * getting the entry and calling getValue(), the wrong value (or null) is returned.
 *
 * The NavigableMap contract states that firstEntry/lastEntry/lowerEntry/floorEntry/
 * ceilingEntry/higherEntry return snapshot entries.
 */
class ConcurrentNavigableMapNullSafeBugFixTest {

    // --- Bug: wrapEntry getValue() should return snapshot, not live value ---

    @Test
    void testFirstEntryValueIsSnapshot() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", "original");

        Map.Entry<String, String> entry = map.firstEntry();
        assertNotNull(entry);
        assertEquals("original", entry.getValue(), "Entry value should be snapshot at time of call");

        // Modify the map after getting the entry
        map.put("a", "modified");

        // The entry should still show the snapshot value, not the live value
        assertEquals("original", entry.getValue(),
                "wrapEntry().getValue() should return snapshot value, not live lookup");
    }

    @Test
    void testLastEntryValueIsSnapshot() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("z", "original");

        Map.Entry<String, String> entry = map.lastEntry();
        assertNotNull(entry);

        map.put("z", "modified");
        assertEquals("original", entry.getValue(),
                "lastEntry() should return snapshot value");
    }

    @Test
    void testLowerEntryValueIsSnapshot() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", "original");
        map.put("b", "value-b");

        Map.Entry<String, String> entry = map.lowerEntry("b");
        assertNotNull(entry);
        assertEquals("a", entry.getKey());

        map.put("a", "modified");
        assertEquals("original", entry.getValue(),
                "lowerEntry() should return snapshot value");
    }

    @Test
    void testFloorEntryValueIsSnapshot() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", "original");

        Map.Entry<String, String> entry = map.floorEntry("a");
        assertNotNull(entry);

        map.put("a", "modified");
        assertEquals("original", entry.getValue(),
                "floorEntry() should return snapshot value");
    }

    @Test
    void testCeilingEntryValueIsSnapshot() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", "original");

        Map.Entry<String, String> entry = map.ceilingEntry("a");
        assertNotNull(entry);

        map.put("a", "modified");
        assertEquals("original", entry.getValue(),
                "ceilingEntry() should return snapshot value");
    }

    @Test
    void testHigherEntryValueIsSnapshot() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", "value-a");
        map.put("b", "original");

        Map.Entry<String, String> entry = map.higherEntry("a");
        assertNotNull(entry);
        assertEquals("b", entry.getKey());

        map.put("b", "modified");
        assertEquals("original", entry.getValue(),
                "higherEntry() should return snapshot value");
    }

    @Test
    void testEntrySnapshotAfterKeyRemoved() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", "original");

        Map.Entry<String, String> entry = map.firstEntry();
        assertNotNull(entry);

        // Remove the key entirely
        map.remove("a");

        // The snapshot should still show the original value, not null
        assertEquals("original", entry.getValue(),
                "Entry snapshot should retain value even after key is removed from map");
    }

    @Test
    void testFirstEntryWithNullValueIsSnapshot() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", null);

        Map.Entry<String, String> entry = map.firstEntry();
        assertNotNull(entry);
        assertEquals(null, entry.getValue(), "Null value should be captured in snapshot");

        // Change value from null to something
        map.put("a", "modified");

        assertEquals(null, entry.getValue(),
                "Entry snapshot should retain null value even after map is updated");
    }

    @Test
    void testFirstEntryWithNullKeyIsSnapshot() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(null, "original");

        // null key sorts last, so it should be lastEntry
        Map.Entry<String, String> entry = map.lastEntry();
        assertNotNull(entry);
        assertEquals(null, entry.getKey());

        map.put(null, "modified");
        assertEquals("original", entry.getValue(),
                "Null-key entry snapshot should retain original value");
    }
}
