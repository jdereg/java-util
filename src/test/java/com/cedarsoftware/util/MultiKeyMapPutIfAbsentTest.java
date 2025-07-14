package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the putIfAbsent API on MultiKeyMap.
 */
class MultiKeyMapPutIfAbsentTest {

    @Test
    void testPutMultiKeyOnAbsentKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);

        assertNull(map.putIfAbsent("a", "value"));
        assertEquals("value", map.get("a"));
    }

    @Test
    void testNoOverwriteWhenPresent() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put("existing", "value");

        assertEquals("value", map.putIfAbsent("existing", "new"));
        assertEquals("value", map.get("existing"));
    }

    @Test
    void testReplaceNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put("nullKey", (String) null);

        assertNull(map.putIfAbsent("nullKey", "filled"));
        assertEquals("filled", map.get("nullKey"));
    }

    @Test
    void testMultiKeyArrayAndCollection() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);

        Object[] arrayKey = {"x", "y"};
        assertNull(map.putIfAbsent(arrayKey, "array"));
        assertEquals("array", map.getMultiKey("x", "y"));

        assertNull(map.putIfAbsent(Arrays.asList("a", "b"), "list"));
        assertEquals("list", map.getMultiKey("a", "b"));
    }
}
