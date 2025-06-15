package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConcurrentHashMapNullSafeConstructorTest {

    @Test
    void testCapacityAndLoadFactorConstructor() {
        ConcurrentHashMapNullSafe<String, Integer> map =
                new ConcurrentHashMapNullSafe<>(16, 0.5f);
        map.put("one", 1);
        map.put(null, 2);
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get(null));
    }

    @Test
    void testCapacityLoadFactorConcurrencyConstructor() {
        ConcurrentHashMapNullSafe<String, Integer> map =
                new ConcurrentHashMapNullSafe<>(8, 0.75f, 2);
        map.put("a", 10);
        map.put(null, 20);
        assertEquals(10, map.get("a"));
        assertEquals(20, map.get(null));
    }

    @Test
    void testMapConstructorCopiesEntries() {
        Map<String, Integer> src = new HashMap<>();
        src.put("x", 1);
        src.put(null, 2);
        ConcurrentHashMapNullSafe<String, Integer> map = new ConcurrentHashMapNullSafe<>(src);
        assertEquals(2, map.size());
        assertEquals(1, map.get("x"));
        assertEquals(2, map.get(null));
    }

    @Test
    void testMapConstructorNull() {
        assertThrows(NullPointerException.class, () -> new ConcurrentHashMapNullSafe<>(null));
    }

    @Test
    void testInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new ConcurrentHashMapNullSafe<>(-1, 0.75f));
        assertThrows(IllegalArgumentException.class, () -> new ConcurrentHashMapNullSafe<>(1, 0.0f));
        assertThrows(IllegalArgumentException.class, () -> new ConcurrentHashMapNullSafe<>(1, 0.75f, 0));
    }
}
