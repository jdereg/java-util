package com.cedarsoftware.util;

import java.util.Comparator;
import java.util.NavigableSet;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests for ConcurrentNavigableMapNullSafe covering
 * constructors and navigation APIs that were not previously tested.
 */
class ConcurrentNavigableMapNullSafeExtraTest {

    @Test
    void testConstructorsAndComparator() {
        // Default constructor should have null comparator
        ConcurrentNavigableMapNullSafe<String, Integer> defaultMap = new ConcurrentNavigableMapNullSafe<>();
        assertNull(defaultMap.comparator());

        // Comparator constructor should retain the comparator instance
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> customMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        assertSame(reverse, customMap.comparator());

        customMap.put("a", 1);
        customMap.put("b", 2);
        // With reverse order comparator, firstKey() should return "b"
        assertEquals("b", customMap.firstKey());
    }

    @Test
    void testSimpleRangeViews() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put("date", 4);
        map.put(null, 0);

        ConcurrentNavigableMap<String, Integer> sub = map.subMap("banana", "date");
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("banana"));
        assertTrue(sub.containsKey("cherry"));
        assertFalse(sub.containsKey("date"));

        ConcurrentNavigableMap<String, Integer> head = map.headMap("cherry");
        assertEquals(2, head.size());
        assertTrue(head.containsKey("apple"));
        assertFalse(head.containsKey("cherry"));

        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("banana");
        assertEquals(4, tail.size());
        assertTrue(tail.containsKey(null));
        assertFalse(tail.containsKey("apple"));
    }

    @Test
    void testEntryNavigationMethods() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        Map.Entry<String, Integer> lower = map.lowerEntry("banana");
        assertEquals("apple", lower.getKey());

        Map.Entry<String, Integer> floor = map.floorEntry("banana");
        assertEquals("banana", floor.getKey());

        Map.Entry<String, Integer> ceiling = map.ceilingEntry("banana");
        assertEquals("banana", ceiling.getKey());

        Map.Entry<String, Integer> higher = map.higherEntry("banana");
        assertEquals("cherry", higher.getKey());

        assertEquals("cherry", map.lowerEntry(null).getKey());
        assertEquals(null, map.floorEntry(null).getKey());
        assertEquals(null, map.ceilingEntry(null).getKey());
        assertNull(map.higherEntry(null));
    }

    @Test
    void testKeySetNavigationMethods() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        NavigableSet<String> keys = map.keySet();
        assertNull(keys.comparator());
        assertEquals("apple", keys.first());
        assertEquals(null, keys.last());
        assertEquals("apple", keys.lower("banana"));
        assertEquals("banana", keys.floor("banana"));
        assertEquals("banana", keys.ceiling("banana"));
        assertEquals("cherry", keys.higher("banana"));
    }
}
