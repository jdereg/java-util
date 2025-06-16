package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CollectionsWrappersTest {

    @Test
    void testGetCheckedListClass() {
        List<String> checked = Collections.checkedList(new ArrayList<>(), String.class);
        assertSame(checked.getClass(), CollectionsWrappers.getCheckedListClass());
        checked.add("a");
        assertThrows(ClassCastException.class, () -> ((List) checked).add(1));
    }

    @Test
    void testGetCheckedSortedSetClass() {
        SortedSet<String> checked = Collections.checkedSortedSet(new TreeSet<>(), String.class);
        assertSame(checked.getClass(), CollectionsWrappers.getCheckedSortedSetClass());
        checked.add("a");
        assertThrows(ClassCastException.class, () -> ((SortedSet) checked).add(1));
    }

    @Test
    void testGetCheckedNavigableSetClass() {
        NavigableSet<String> checked = Collections.checkedNavigableSet(new TreeSet<>(), String.class);
        assertSame(checked.getClass(), CollectionsWrappers.getCheckedNavigableSetClass());
        checked.add("a");
        assertThrows(ClassCastException.class, () -> ((NavigableSet) checked).add(1));
    }

    @Test
    void testGetEmptyCollectionClass() {
        Collection<Object> empty = Collections.emptyList();
        assertSame(empty.getClass(), CollectionsWrappers.getEmptyCollectionClass());
        assertTrue(empty.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> empty.add("x"));
    }

    @Test
    void testGetEmptySetClass() {
        Set<Object> empty = Collections.emptySet();
        assertSame(empty.getClass(), CollectionsWrappers.getEmptySetClass());
        assertTrue(empty.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> empty.add("x"));
    }

    @Test
    void testGetEmptySortedSetClass() {
        SortedSet<Object> empty = Collections.emptySortedSet();
        assertSame(empty.getClass(), CollectionsWrappers.getEmptySortedSetClass());
        assertTrue(empty.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> empty.add("x"));
    }

    @Test
    void testGetEmptyNavigableSetClass() {
        NavigableSet<Object> empty = Collections.emptyNavigableSet();
        assertSame(empty.getClass(), CollectionsWrappers.getEmptyNavigableSetClass());
        assertTrue(empty.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> empty.add("x"));
    }

    @Test
    void testGetSynchronizedListClass() {
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        assertSame(syncList.getClass(), CollectionsWrappers.getSynchronizedListClass());
        synchronized (syncList) {
            syncList.add("a");
        }
        assertTrue(syncList.contains("a"));
    }

    @Test
    void testGetSynchronizedSortedSetClass() {
        SortedSet<String> syncSet = Collections.synchronizedSortedSet(new TreeSet<>());
        assertSame(syncSet.getClass(), CollectionsWrappers.getSynchronizedSortedSetClass());
        synchronized (syncSet) {
            syncSet.add("a");
        }
        assertTrue(syncSet.contains("a"));
    }

    @Test
    void testGetSynchronizedNavigableSetClass() {
        NavigableSet<String> syncNav = Collections.synchronizedNavigableSet(new TreeSet<>());
        assertSame(syncNav.getClass(), CollectionsWrappers.getSynchronizedNavigableSetClass());
        synchronized (syncNav) {
            syncNav.add("a");
        }
        assertTrue(syncNav.contains("a"));
    }
}
