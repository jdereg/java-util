package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link CompactCIHashMap}.
 */
class CompactCIHashMapTest {

    @Test
    void caseInsensitiveLookup() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        map.put("FoO", 1);

        assertEquals(1, map.get("foo"));
        assertTrue(map.containsKey("FOO"));

        map.put("foo", 2);
        assertEquals(1, map.size(), "put should overwrite existing key case-insensitively");
        assertEquals(2, map.get("fOo"));

        map.remove("FOO");
        assertTrue(map.isEmpty());
    }

    @Test
    void copyConstructorPreservesEntries() {
        Map<String, Integer> src = new HashMap<>();
        src.put("One", 1);
        src.put("Two", 2);

        CompactCIHashMap<String, Integer> copy = new CompactCIHashMap<>(src);
        assertEquals(2, copy.size());
        assertEquals(1, copy.get("one"));
        assertEquals(2, copy.get("TWO"));
    }

    @Test
    void storageTransitionToMap() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<String, Integer>() {
            @Override
            protected int compactSize() { return 2; }
        };

        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        map.put("c", 3); // exceed compact size
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        assertFalse(map.isDefaultCompactMap());
        Map<String, Object> config = map.getConfig();
        assertEquals(false, config.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, config.get(CompactMap.COMPACT_SIZE));
        assertEquals(HashMap.class, config.get(CompactMap.MAP_TYPE));
    }
}
