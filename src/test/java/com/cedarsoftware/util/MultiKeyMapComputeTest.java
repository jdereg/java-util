package com.cedarsoftware.util;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests for the compute API on MultiKeyMap.
 */
class MultiKeyMapComputeTest {

    @Test
    void testComputeNewKey() {
        MultiKeyMap<Integer> map = new MultiKeyMap<>(16);
        Integer result = map.compute("a", (k, v) -> 1);
        assertEquals(1, result);
        assertEquals(1, map.get("a"));
    }

    @Test
    void testComputeExistingKey() {
        MultiKeyMap<Integer> map = new MultiKeyMap<>(16);
        map.put((Object) "a", (Integer) 1);  // Use Map interface semantics
        Integer result = map.compute("a", (k, v) -> v + 1);
        assertEquals(2, result);
        assertEquals(2, map.get("a"));
    }

    @Test
    void testComputeToNullRemoves() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put("x", "a");
        map.compute("a", (k, v) -> null);
        assertFalse(map.containsKey("a"));
    }

    @Test
    void testComputeWithArrayKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        Object[] key = {"k1", "k2"};
        map.putMultiKey("v1", key);
        map.compute(key, (k, v) -> v + "2");
        assertEquals("v12", map.getMultiKey("k1", "k2"));
    }

    @Test
    void testComputeWithCollectionKeys() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        java.util.List<String> listKey = Arrays.asList("a", "b");
        map.put(listKey, "v");  // Use Collection as single key (Map interface semantics)
        map.compute(listKey, (k, v) -> v + "3");
        assertEquals("v3", map.get(listKey));
    }
}
