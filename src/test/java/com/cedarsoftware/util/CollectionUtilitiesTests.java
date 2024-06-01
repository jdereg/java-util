package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.CollectionUtilities.hasContent;
import static com.cedarsoftware.util.CollectionUtilities.isEmpty;
import static com.cedarsoftware.util.CollectionUtilities.listOf;
import static com.cedarsoftware.util.CollectionUtilities.setOf;
import static com.cedarsoftware.util.CollectionUtilities.size;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
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
        final List<String> list = listOf();
        assertEquals(0, list.size());
    }

    @Test
    void testListOf_producesImmutableList() {
        final List<String> list = listOf();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> list.add("One"));
    }

    @Test
    void testListOfOne() {
        final List<String> list = listOf("One");
        assertEquals(1, list.size());
        assertEquals("One", list.get(0));
    }

    @Test
    void testListOfTwo() {
        final List<String> list = listOf("One", "Two");
        assertEquals(2, list.size());
        assertEquals("One", list.get(0));
        assertEquals("Two", list.get(1));
    }

    @Test
    void testListOfThree() {
        final List<String> list = listOf("One", "Two", "Three");
        assertEquals(3, list.size());
        assertEquals("One", list.get(0));
        assertEquals("Two", list.get(1));
        assertEquals("Three", list.get(2));
    }

    @Test
    void testSetOf() {
        final Set<?> set = setOf();
        assertEquals(0, set.size());
    }

    @Test
    void testSetOf_producesImmutableSet() {
        final Set<String> set = setOf();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> set.add("One"));
    }


    @Test
    void testSetOfOne() {
        final Set<String> set = setOf("One");
        assertEquals(1, set.size());
        assertTrue(set.contains("One"));
    }

    @Test
    void testSetOfTwo() {
        final Set<String> set = setOf("One", "Two");
        assertEquals(2, set.size());
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
    }

    @Test
    void testSetOfThree() {
        final Set<String> set = setOf("One", "Two", "Three");
        assertEquals(3, set.size());
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
        assertTrue(set.contains("Three"));
    }

    @Test
    void testIsEmpty() {
        assertTrue(isEmpty(null));
        assertTrue(isEmpty(new ArrayList<>()));
        assertTrue(isEmpty(new HashSet<>()));
        assertFalse(isEmpty(setOf("one")));
        assertFalse(isEmpty(listOf("one")));
    }

    @Test
    void testHasContent() {
        assertFalse(hasContent(null));
        assertFalse(hasContent(new ArrayList<>()));
        assertFalse(hasContent(new HashSet<>()));
        assertTrue(hasContent(setOf("one")));
        assertTrue(hasContent(listOf("one")));
    }

    @Test
    void testSize() {
        assertEquals(0, size(null));
        assertEquals(0, size(new ArrayList<>()));
        assertEquals(0, size(new HashSet<>()));
        assertEquals(1, size(setOf("one")));
        assertEquals(1, size(listOf("one")));
        assertEquals(2, size(setOf("one", "two")));
        assertEquals(2, size(listOf("one", "two")));
    }
}
