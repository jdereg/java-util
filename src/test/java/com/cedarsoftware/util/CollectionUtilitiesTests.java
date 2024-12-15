package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.CollectionUtilities.getCheckedCollection;
import static com.cedarsoftware.util.CollectionUtilities.getEmptyCollection;
import static com.cedarsoftware.util.CollectionUtilities.getSynchronizedCollection;
import static com.cedarsoftware.util.CollectionUtilities.getUnmodifiableCollection;
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

    @Test
    void testGetUnmodifiableCollection() {
        List<String> list = new ArrayList<>(listOf("one", "two"));
        Collection<String> unmodifiableList = getUnmodifiableCollection(list);

        assertEquals(2, unmodifiableList.size());
        assertTrue(unmodifiableList.contains("one"));
        assertTrue(unmodifiableList.contains("two"));

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> unmodifiableList.add("three"));
    }

    @Test
    void testGetCheckedCollection() {
        List<Object> list = new ArrayList<>(listOf("one", "two"));
        Collection<String> checkedCollection = getCheckedCollection(list, String.class);

        assertEquals(2, checkedCollection.size());
        assertTrue(checkedCollection.contains("one"));
        assertTrue(checkedCollection.contains("two"));

        assertThatExceptionOfType(ClassCastException.class)
                .isThrownBy(() -> (checkedCollection).add((String)(Object)1));
    }

    @Test
    void testGetSynchronizedCollection() {
        List<String> list = new ArrayList<>(listOf("one", "two"));
        Collection<String> synchronizedCollection = getSynchronizedCollection(list);

        assertEquals(2, synchronizedCollection.size());
        assertTrue(synchronizedCollection.contains("one"));
        assertTrue(synchronizedCollection.contains("two"));

        synchronized (synchronizedCollection) {
            synchronizedCollection.add("three");
        }
        assertTrue(synchronizedCollection.contains("three"));
    }

    @Test
    void testGetEmptyCollection() {
        List<String> list = new ArrayList<>();
        Collection<String> emptyList = getEmptyCollection(list);
        assertEquals(0, emptyList.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> emptyList.add("one"));

        Set<String> set = new HashSet<>();
        Collection<String> emptySet = getEmptyCollection(set);
        assertEquals(0, emptySet.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> emptySet.add("one"));
    }

    @Test
    void testGetUnmodifiableCollectionSpecificTypes() {
        NavigableSet<String> navigableSet = new TreeSet<>(setOf("one", "two"));
        Collection<String> unmodifiableNavigableSet = getUnmodifiableCollection(navigableSet);
        assertEquals(2, unmodifiableNavigableSet.size());
        assertTrue(unmodifiableNavigableSet.contains("one"));

        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> unmodifiableNavigableSet.add("three"));
    }

    @Test
    void testGetCheckedCollectionSpecificTypes() {
        NavigableSet<Object> navigableSet = new TreeSet<>(setOf("one", "two"));
        Collection<String> checkedNavigableSet = getCheckedCollection(navigableSet, String.class);
        assertEquals(2, checkedNavigableSet.size());
        assertTrue(checkedNavigableSet.contains("one"));

        assertThatExceptionOfType(ClassCastException.class)
                .isThrownBy(() -> checkedNavigableSet.add((String)(Object)1));
    }

    @Test
    void testGetSynchronizedCollectionSpecificTypes() {
        SortedSet<String> sortedSet = new TreeSet<>(setOf("one", "two"));
        Collection<String> synchronizedSortedSet = getSynchronizedCollection(sortedSet);
        assertEquals(2, synchronizedSortedSet.size());
        assertTrue(synchronizedSortedSet.contains("one"));

        synchronizedSortedSet.add("three");
        assertTrue(synchronizedSortedSet.contains("three"));
    }

    @Test
    void testGetEmptyCollectionSpecificTypes() {
        SortedSet<String> sortedSet = new TreeSet<>();
        Collection<String> emptySortedSet = getEmptyCollection(sortedSet);
        assertEquals(0, emptySortedSet.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> emptySortedSet.add("one"));
    }
}
