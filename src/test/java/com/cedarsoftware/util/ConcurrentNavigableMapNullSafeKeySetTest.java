package com.cedarsoftware.util;

import java.util.Iterator;
import java.util.NavigableSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the keySet() view of ConcurrentNavigableMapNullSafe.
 */
class ConcurrentNavigableMapNullSafeKeySetTest {

    @Test
    void testKeySetOperations() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 3);

        NavigableSet<String> keys = map.keySet();

        assertEquals(3, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains(null));
        assertFalse(keys.contains("c"));

        assertTrue(keys.remove("b"));
        assertFalse(map.containsKey("b"));
        assertEquals(2, keys.size());

        assertFalse(keys.remove("c"));

        assertTrue(keys.remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(1, keys.size());

        keys.clear();
        assertTrue(keys.isEmpty());
        assertTrue(map.isEmpty());
    }

    @Test
    void testIteratorRemove() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 0);

        NavigableSet<String> keys = map.keySet();
        Iterator<String> it = keys.iterator();

        while (it.hasNext()) {
            String key = it.next();
            it.remove();
            assertFalse(map.containsKey(key));
        }

        assertTrue(map.isEmpty());
        assertTrue(keys.isEmpty());
    }
}
