package com.cedarsoftware.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompactMapPutAllTest {
    private static final int TEST_COMPACT_SIZE = 3;

    @Test
    public void testPutAllSwitchesToMapWhenThresholdExceeded() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() { return new LinkedHashMap<>(); }
        };

        map.put("A", "alpha");
        map.put("B", "bravo");

        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("C", "charlie");
        extra.put("D", "delta");

        map.putAll(extra);

        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertEquals("alpha", map.get("A"));
        assertEquals("delta", map.get("D"));
    }

    @Test
    public void testPutAllStaysArrayWhenWithinThreshold() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() { return new LinkedHashMap<>(); }
        };

        map.put("A", "alpha");
        map.put("B", "bravo");

        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("C", "charlie");

        map.putAll(extra);

        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertEquals("charlie", map.get("C"));
    }

    @Test
    public void testPutAllWithOnlyOverwritesDoesNotForceMapState() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() { return new LinkedHashMap<>(); }
        };

        map.put("A", "alpha");
        map.put("B", "bravo");

        // Upper-bound size would be 4 (> compactSize), but true unique-key size remains 2.
        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("A", "alpha-2");
        extra.put("B", "bravo-2");

        map.putAll(extra);

        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertEquals("alpha-2", map.get("A"));
        assertEquals("bravo-2", map.get("B"));

        map.remove("A");
        map.remove("B");
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }
}
