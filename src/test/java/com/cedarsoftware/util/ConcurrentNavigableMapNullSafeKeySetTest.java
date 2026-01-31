package com.cedarsoftware.util;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void testSubHeadTailAndSortedViews() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put("date", 4);
        map.put(null, 0);

        NavigableSet<String> keys = map.keySet();

        NavigableSet<String> sub = keys.subSet("banana", true, "date", false);
        Iterator<String> it = sub.iterator();
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertFalse(it.hasNext());
        SortedSet<String> simpleSub = keys.subSet("banana", "date");
        assertEquals(sub, simpleSub);
        assertThrows(IllegalArgumentException.class,
                () -> keys.subSet("date", true, "banana", false));

        NavigableSet<String> headEx = keys.headSet("cherry", false);
        assertTrue(headEx.contains("apple"));
        assertFalse(headEx.contains("cherry"));
        assertEquals(2, headEx.size());
        SortedSet<String> headSimple = keys.headSet("cherry");
        assertEquals(headEx, headSimple);

        NavigableSet<String> headIn = keys.headSet("cherry", true);
        assertTrue(headIn.contains("cherry"));
        assertEquals(3, headIn.size());

        NavigableSet<String> tailEx = keys.tailSet("banana", false);
        assertFalse(tailEx.contains("banana"));
        assertTrue(tailEx.contains(null));
        assertEquals(3, tailEx.size());
        SortedSet<String> tailSimple = keys.tailSet("banana");
        assertTrue(tailSimple.contains("banana"));
        assertEquals(4, tailSimple.size());
    }

    @Test
    void testDescendingSet() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put("date", 4);
        map.put(null, 0);

        NavigableSet<String> descending = map.keySet().descendingSet();
        Iterator<String> it = descending.iterator();
        assertEquals(null, it.next());
        assertEquals("date", it.next());
        assertEquals("cherry", it.next());
        assertEquals("banana", it.next());
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());

        assertTrue(descending.remove("date"));
        assertFalse(map.containsKey("date"));
    }
}
