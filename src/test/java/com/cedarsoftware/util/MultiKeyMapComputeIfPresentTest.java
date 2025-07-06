package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the computeIfPresent API on MultiKeyMap.
 */
class MultiKeyMapComputeIfPresentTest {

    @Test
    void testComputeIfPresentOnExistingKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put("value", "a");

        String result = map.computeIfPresent("a", (k, v) -> v + "-new");
        assertEquals("value-new", result);
        assertEquals("value-new", map.get("a"));
    }

    @Test
    void testComputeIfPresentOnMissingKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        assertNull(map.computeIfPresent("missing", (k, v) -> "x"));
        assertFalse(map.containsKey("missing"));
    }

    @Test
    void testComputeIfPresentRemovesWhenNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put("gone", "bye");
        String result = map.computeIfPresent("gone", (k, v) -> null);
        assertNull(result);
        assertFalse(map.containsKey("gone"));
    }

    @Test
    void testComputeIfPresentWithArraysAndCollections() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        Object[] arrayKey = {"x", "y"};
        map.put("val", arrayKey);
        map.computeIfPresent(arrayKey, (k, v) -> v + "1");
        assertEquals("val1", map.get("x", "y"));

        map.put("list", Arrays.asList("a", "b"));
        map.computeIfPresent(Arrays.asList("a", "b"), (k, v) -> v + "2");
        assertEquals("list2", map.get("a", "b"));
    }
}
