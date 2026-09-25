package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StringBuilder / StringBuffer have identity equals()/hashCode(); only JDK 11+ adds a content compareTo().
 * DeepEquals used whichever the JDK offered, so on JDK 8 two builders holding the same text compared
 * UNEQUAL, and on every JDK a container of builders had a deepHashCode() that disagreed with
 * deepEquals(). Both now go by the characters, on every JDK.
 */
class DeepEqualsStringBuilderTest {

    /** Same text, different internal capacity. */
    private static StringBuilder builder(String s, int capacity) {
        StringBuilder sb = new StringBuilder(capacity);
        sb.append(s);
        return sb;
    }

    @Test
    void equalTextIsEqual() {
        assertTrue(DeepEquals.deepEquals(new StringBuilder("0"), new StringBuilder("0")));
        assertTrue(DeepEquals.deepEquals(builder("hello", 5), builder("hello", 500)));
        assertTrue(DeepEquals.deepEquals(new StringBuffer("hi"), new StringBuffer("hi")));
        assertTrue(DeepEquals.deepEquals(new StringBuilder(), new StringBuilder()));
    }

    @Test
    void differentTextIsUnequalWithDiff() {
        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(new StringBuilder("a"), new StringBuilder("b"), options));
        assertNotNull(options.get("diff"));
        assertTrue(options.get("diff").toString().contains("value mismatch"));
        assertFalse(DeepEquals.deepEquals(new StringBuilder("a"), new StringBuilder("a ")));
    }

    /** Unchanged: a StringBuilder and a StringBuffer are different types, whatever they hold. */
    @Test
    void builderAndBufferAreDifferentTypes() {
        assertFalse(DeepEquals.deepEquals(new StringBuilder("x"), new StringBuffer("x")));
    }

    @Test
    void deepHashCodeFollowsTheText() {
        assertEquals(DeepEquals.deepHashCode(builder("abc", 3)), DeepEquals.deepHashCode(builder("abc", 300)));
        assertEquals(DeepEquals.deepHashCode(new StringBuffer("abc")), DeepEquals.deepHashCode(new StringBuffer("abc")));
    }

    @Test
    void containersOfBuildersHonorTheEqualsHashCodeContract() {
        List<Object> l1 = Arrays.asList(builder("a", 1), new StringBuffer("b"));
        List<Object> l2 = Arrays.asList(builder("a", 99), new StringBuffer("b"));
        assertTrue(DeepEquals.deepEquals(l1, l2));
        assertEquals(DeepEquals.deepHashCode(l1), DeepEquals.deepHashCode(l2));

        Object[] a1 = {new StringBuilder("x"), new StringBuilder("y")};
        Object[] a2 = {new StringBuilder("x"), new StringBuilder("y")};
        assertTrue(DeepEquals.deepEquals(a1, a2));
        assertEquals(DeepEquals.deepHashCode(a1), DeepEquals.deepHashCode(a2));

        Map<Object, Object> m1 = new HashMap<>();
        m1.put("k", new StringBuilder("v"));
        Map<Object, Object> m2 = new HashMap<>();
        m2.put("k", new StringBuilder("v"));
        assertTrue(DeepEquals.deepEquals(m1, m2));
        assertEquals(DeepEquals.deepHashCode(m1), DeepEquals.deepHashCode(m2));
    }

    @Test
    void unorderedContainersMatchBuildersByText() {
        Set<Object> s1 = new HashSet<>(Arrays.asList(new StringBuilder("a"), new StringBuilder("b")));
        Set<Object> s2 = new HashSet<>(Arrays.asList(new StringBuilder("b"), new StringBuilder("a")));
        assertTrue(DeepEquals.deepEquals(s1, s2));

        Map<Object, Object> m1 = new HashMap<>();
        m1.put(new StringBuilder("key"), 1);
        Map<Object, Object> m2 = new HashMap<>();
        m2.put(new StringBuilder("key"), 1);
        assertTrue(DeepEquals.deepEquals(m1, m2));

        List<Object> inner1 = new ArrayList<>(Collections.singletonList(new StringBuilder("z")));
        List<Object> inner2 = new ArrayList<>(Collections.singletonList(new StringBuilder("z")));
        assertTrue(DeepEquals.deepEquals(new HashSet<>(Collections.singleton(inner1)),
                new HashSet<>(Collections.singleton(inner2))));
    }
}
