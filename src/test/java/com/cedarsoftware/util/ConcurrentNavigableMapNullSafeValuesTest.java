package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the values() view of ConcurrentNavigableMapNullSafe.
 */
class ConcurrentNavigableMapNullSafeValuesTest {

    @Test
    void testValuesViewOperations() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", null);
        map.put("c", 3);
        map.put(null, 2);

        Collection<Integer> values = map.values();

        // Size and contains checks
        assertEquals(4, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(null));
        assertTrue(values.contains(2));
        assertFalse(values.contains(5));

        // Verify iteration order and unmasking
        Iterator<Integer> it = values.iterator();
        assertEquals(1, it.next());
        assertNull(it.next());
        assertEquals(3, it.next());
        assertEquals(2, it.next());
        assertFalse(it.hasNext());

        // Remove using iterator and verify map is updated
        it = values.iterator();
        assertEquals(1, it.next());
        it.remove();
        assertFalse(map.containsKey("a"));
        assertEquals(3, values.size());

        // Clear the values view and ensure map is empty
        values.clear();
        assertTrue(map.isEmpty());
    }
}
