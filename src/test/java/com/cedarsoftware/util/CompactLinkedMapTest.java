package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompactLinkedMapTest {

    @Test
    public void testExpansionAndOrdering() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        // exceed the compact size to force backing map creation
        int limit = map.compactSize() + 3;

        map.put("FoO", 99);
        for (int i = 0; i < limit; i++) {
            map.put("k" + i, i);
        }

        assertEquals(limit + 1, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertTrue(map.val instanceof LinkedHashMap);

        List<String> expected = new ArrayList<>();
        expected.add("FoO");
        for (int i = 0; i < limit; i++) {
            expected.add("k" + i);
        }
        assertEquals(expected, new ArrayList<>(map.keySet()));

        assertTrue(map.containsKey("FoO"));
        assertFalse(map.containsKey("foo"));
    }

    @Test
    public void testCopyConstructor() {
        CompactLinkedMap<String, Integer> original = new CompactLinkedMap<>();
        original.put("a", 1);
        original.put("b", 2);

        CompactLinkedMap<String, Integer> copy = new CompactLinkedMap<>(original);
        assertEquals(original, copy);
        assertNotSame(original, copy);
        assertEquals(new ArrayList<>(original.keySet()), new ArrayList<>(copy.keySet()));

        copy.put("c", 3);
        assertTrue(copy.containsKey("c"));
        assertFalse(original.containsKey("c"));
    }
}
