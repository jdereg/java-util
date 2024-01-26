package com.cedarsoftware.util;

import com.cedarsoftware.util.io.MetaUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionUtilitiesTests {
    static class Rec {
        final String s;
        final int i;
        Rec(String s, int i) {
            this.s = s;
            this.i = i;
        }

        Rec       link;
        List<Rec> ilinks;
        List<Rec> mlinks;

        Map<String, Rec> smap;
    }

    @Test
    void testListOf() {
        final List<String> list = CollectionUtilities.listOf();
        assertEquals(0, list.size());
    }

    @Test
    void testListOf_producesImmutableList() {
        final List<String> list = CollectionUtilities.listOf();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> list.add("One"));
    }

    @Test
    void testListOfOne() {
        final List<String> list = CollectionUtilities.listOf("One");
        assertEquals(1, list.size());
        assertEquals("One", list.get(0));
    }

    @Test
    void testListOfTwo() {
        final List<String> list = CollectionUtilities.listOf("One", "Two");
        assertEquals(2, list.size());
        assertEquals("One", list.get(0));
        assertEquals("Two", list.get(1));
    }

    @Test
    void testListOfThree() {
        final List<String> list = CollectionUtilities.listOf("One", "Two", "Three");
        assertEquals(3, list.size());
        assertEquals("One", list.get(0));
        assertEquals("Two", list.get(1));
        assertEquals("Three", list.get(2));
    }

    @Test
    void testSetOf() {
        final Set<?> set = CollectionUtilities.setOf();
        assertEquals(0, set.size());
    }

    @Test
    void testSetOf_producesImmutableSet() {
        final Set<String> set = CollectionUtilities.setOf();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> set.add("One"));
    }


    @Test
    void testSetOfOne() {
        final Set<String> set = CollectionUtilities.setOf("One");
        assertEquals(1, set.size());
        assertTrue(set.contains("One"));
    }

    @Test
    public void testSetOfTwo() {
        final Set<String> set = CollectionUtilities.setOf("One", "Two");
        assertEquals(2, set.size());
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
    }

    @Test
    public void testSetOfThree() {
        final Set<String> set = CollectionUtilities.setOf("One", "Two", "Three");
        assertEquals(3, set.size());
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
        assertTrue(set.contains("Three"));
    }
}
