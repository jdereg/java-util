package com.cedarsoftware.util;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
