package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Map.Entry instances returned by ConcurrentNavigableMapNullSafe.
 */
class ConcurrentNavigableMapNullSafeEntryTest {

    @Test
    void testEntrySetValueEqualsHashCodeAndToString() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);

        Map.Entry<String, Integer> entry = map.entrySet().stream()
                .filter(e -> "a".equals(e.getKey()))
                .findFirst()
                .orElseThrow(NoSuchElementException::new);

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
    void testNullKeyAndValueEntry() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(null, null);

        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();

        assertNull(entry.setValue(5));
        assertEquals(Integer.valueOf(5), map.get(null));

        Map.Entry<String, Integer> same = new AbstractMap.SimpleEntry<>(null, 5);
        assertEquals(entry, same);
        assertEquals(Objects.hashCode(null) ^ Objects.hashCode(5), entry.hashCode());
        assertEquals("null=5", entry.toString());
    }
}
