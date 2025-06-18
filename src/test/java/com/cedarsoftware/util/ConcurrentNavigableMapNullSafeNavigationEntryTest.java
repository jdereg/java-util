package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests Map.Entry instances returned by navigation methods of ConcurrentNavigableMapNullSafe.
 */
class ConcurrentNavigableMapNullSafeNavigationEntryTest {

    @Test
    void testFirstEntrySetValueEqualsHashCodeAndToString() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);

        Map.Entry<String, Integer> entry = map.firstEntry();

        assertEquals(1, entry.setValue(10));
        assertEquals(Integer.valueOf(10), map.get("a"));

        Map.Entry<String, Integer> same = new AbstractMap.SimpleEntry<>("a", 10);
        Map.Entry<String, Integer> diffKey = new AbstractMap.SimpleEntry<>("c", 10);
        Map.Entry<String, Integer> diffVal = new AbstractMap.SimpleEntry<>("a", 11);

        assertEquals(entry, same);
        assertEquals(entry.hashCode(), same.hashCode());
        assertNotEquals(entry, diffKey);
        assertNotEquals(entry, diffVal);

        assertEquals("a=10", entry.toString());
    }

    @Test
    void testFloorEntryWithNullKeyAndValue() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(null, null);

        Map.Entry<String, Integer> entry = map.floorEntry(null);

        assertNull(entry.setValue(5));
        assertEquals(Integer.valueOf(5), map.get(null));

        Map.Entry<String, Integer> same = new AbstractMap.SimpleEntry<>(null, 5);
        assertEquals(entry, same);
        assertEquals(Objects.hashCode(null) ^ Objects.hashCode(5), entry.hashCode());
        assertEquals("null=5", entry.toString());
    }

    @Test
    void testSetValueToNullAndToString() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("x", 7);

        Map.Entry<String, Integer> entry = map.ceilingEntry("x");

        assertEquals(Integer.valueOf(7), entry.setValue(null));
        assertNull(map.get("x"));
        assertEquals("x=null", entry.toString());
    }

    @Test
    void testEqualsWithNonEntryObject() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("key", 42);

        Map.Entry<String, Integer> entry = map.firstEntry();

        assertNotEquals(entry, "notAnEntry");
    }
}
