package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentSetAdditionalTest {

    @Test
    void testConstructorFromCollection() {
        Collection<String> col = new ArrayList<>(Arrays.asList("a", null, "b"));
        ConcurrentSet<String> set = new ConcurrentSet<>(col);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains(null));

        col.add("c");
        assertFalse(set.contains("c"));
    }

    @Test
    void testConstructorFromSet() {
        Set<String> orig = new HashSet<>(Arrays.asList("x", null));
        ConcurrentSet<String> set = new ConcurrentSet<>(orig);
        assertEquals(2, set.size());
        assertTrue(set.contains("x"));
        assertTrue(set.contains(null));

        orig.add("y");
        assertFalse(set.contains("y"));
    }

    @Test
    void testToStringOutput() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        assertEquals("[]", set.toString());

        set.add("a");
        set.add(null);
        set.add("b");

        String str = set.toString();
        assertTrue(str.startsWith("[") && str.endsWith("]"), "String should use standard [] brackets");
        String content = str.substring(1, str.length() - 1);
        String[] parts = content.split(", ");
        Set<String> tokens = new HashSet<>(Arrays.asList(parts));
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "null")), tokens);
    }

    @Test
    void testRemoveAllSelfClearsSet() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", null, "b"));

        assertTrue(set.removeAll(set));
        assertTrue(set.isEmpty());
    }

    @Test
    void testRetainAllSelfIsNoOp() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", null, "b"));

        assertFalse(set.retainAll(set));
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains(null));
    }
}
