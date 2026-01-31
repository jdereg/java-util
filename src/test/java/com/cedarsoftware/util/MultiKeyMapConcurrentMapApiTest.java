package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for additional ConcurrentMap APIs on MultiKeyMap.
 */
class MultiKeyMapConcurrentMapApiTest {

    @Test
    void testRemoveWithValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put((Object) "a", (String) "val");  // Use Map interface explicitly
        assertFalse(map.remove("a", "x"));
        assertTrue(map.remove("a", "val"));
        assertFalse(map.containsKey("a"));
    }

    @Test
    void testReplaceMethods() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        assertNull(map.replace("a", "x"));
        map.put((Object) "a", (String) "orig");  // Use Map interface explicitly
        assertTrue(map.replace("a", "orig", "new"));
        assertEquals("new", map.get("a"));
        assertEquals("new", map.replace("a", "latest"));
        assertEquals("latest", map.get("a"));
    }
}
