package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the computeIfAbsent API on MultiKeyMap.
 */
class MultiKeyMapComputeIfAbsentTest {

    @Test
    void testComputeOnAbsentKey() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);

        String value = map.computeIfAbsent("a", k -> "computed");
        assertEquals("computed", value);
        assertEquals("computed", map.get("a"));
    }

    @Test
    void testNoRecomputeWhenPresent() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put("existing", "value");
        AtomicInteger calls = new AtomicInteger();

        String value = map.computeIfAbsent("existing", k -> {
            calls.incrementAndGet();
            return "new";
        });

        assertEquals("value", value);
        assertEquals(0, calls.get());
    }

    @Test
    void testReplaceNullValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put("nullKey", null);

        String value = map.computeIfAbsent("nullKey", k -> "filled");
        assertEquals("filled", value);
        assertEquals("filled", map.get("nullKey"));
    }

    @Test
    void testMultiKeyArrayAndCollection() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);

        Object[] arrayKey = {"x", "y"};
        String v1 = map.computeIfAbsent(arrayKey, k -> "array");
        assertEquals("array", v1);
        assertEquals("array", map.get("x", "y"));

        String v2 = map.computeIfAbsent(Arrays.asList("a", "b"), k -> "list");
        assertEquals("list", v2);
        assertEquals("list", map.get("a", "b"));
    }
}
