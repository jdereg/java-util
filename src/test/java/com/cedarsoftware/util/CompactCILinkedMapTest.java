package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CompactCILinkedMapTest {

    @Test
    void verifyCaseInsensitiveAndOrdering() {
        CompactCILinkedMap<String, Integer> map = new CompactCILinkedMap<>();
        int size = map.compactSize() + 5;

        for (int i = 0; i < size; i++) {
            map.put("Key" + i, i);
        }

        assertEquals(Integer.valueOf(0), map.get("key0"));
        assertEquals(Integer.valueOf(0), map.get("KEY0"));
        assertEquals(Integer.valueOf(size - 1), map.get("KEY" + (size - 1)));

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        for (int i = 0; i < size; i++) {
            Map.Entry<String, Integer> entry = it.next();
            assertEquals("Key" + i, entry.getKey());
            assertEquals(Integer.valueOf(i), entry.getValue());
        }
    }

    @Test
    void copyConstructorPreservesBehavior() {
        CompactCILinkedMap<String, Integer> original = new CompactCILinkedMap<>();
        original.put("Foo", 1);

        CompactCILinkedMap<String, Integer> copy = new CompactCILinkedMap<>(original);

        assertTrue(copy.containsKey("FOO"));
        assertEquals(Integer.valueOf(1), copy.get("foo"));
        assertEquals(original, copy);
        assertNotSame(original, copy);
    }
}
