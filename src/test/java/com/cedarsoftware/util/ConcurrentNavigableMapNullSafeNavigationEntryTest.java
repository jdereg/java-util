package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests Map.Entry instances returned by navigation methods of ConcurrentNavigableMapNullSafe.
 * Navigation entries are snapshots (SimpleImmutableEntry) per the NavigableMap contract.
 */
class ConcurrentNavigableMapNullSafeNavigationEntryTest {

    @Test
    void testFirstEntryIsSnapshotWithEqualsHashCodeAndToString() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);

        Map.Entry<String, Integer> entry = map.firstEntry();

        // Snapshot entries do not support setValue per NavigableMap contract
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(10));

        // Entry should have snapshot values
        assertEquals("a", entry.getKey());
        assertEquals(Integer.valueOf(1), entry.getValue());

        Map.Entry<String, Integer> same = new AbstractMap.SimpleEntry<>("a", 1);
        Map.Entry<String, Integer> diffKey = new AbstractMap.SimpleEntry<>("c", 1);
        Map.Entry<String, Integer> diffVal = new AbstractMap.SimpleEntry<>("a", 11);

        assertEquals(entry, same);
        assertEquals(entry.hashCode(), same.hashCode());
        assertNotEquals(entry, diffKey);
        assertNotEquals(entry, diffVal);

        assertEquals("a=1", entry.toString());
    }

    @Test
    void testFloorEntryWithNullKeyAndValue() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(null, null);

        Map.Entry<String, Integer> entry = map.floorEntry(null);

        // Snapshot entries do not support setValue
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(5));

        assertNull(entry.getKey());
        assertNull(entry.getValue());

        Map.Entry<String, Integer> same = new AbstractMap.SimpleEntry<>(null, null);
        assertEquals(entry, same);
        assertEquals(Objects.hashCode(null) ^ Objects.hashCode(null), entry.hashCode());
        assertEquals("null=null", entry.toString());
    }

    @Test
    void testCeilingEntrySnapshotWithNullValue() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("x", 7);

        Map.Entry<String, Integer> entry = map.ceilingEntry("x");

        // Snapshot entries do not support setValue
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(null));

        assertEquals("x", entry.getKey());
        assertEquals(Integer.valueOf(7), entry.getValue());
        assertEquals("x=7", entry.toString());
    }

    @Test
    void testEqualsWithNonEntryObject() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("key", 42);

        Map.Entry<String, Integer> entry = map.firstEntry();

        assertNotEquals(entry, "notAnEntry");
    }
}
