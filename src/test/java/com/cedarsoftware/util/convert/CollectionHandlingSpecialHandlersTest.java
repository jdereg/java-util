package com.cedarsoftware.util.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.TreeSet;

import com.cedarsoftware.util.CollectionUtilities;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionHandlingSpecialHandlersTest {

    @Test
    void createEmptyListSingleton() {
        List<String> source = Arrays.asList("a", "b");
        List<String> result1 = (List<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getEmptyListClass());
        List<String> result2 = (List<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getEmptyListClass());
        assertSame(Collections.emptyList(), result1);
        assertSame(result1, result2);
        assertThrows(UnsupportedOperationException.class, () -> result1.add("x"));
    }

    @Test
    void createEmptyNavigableSetSingleton() {
        NavigableSet<String> source = new TreeSet<>(Arrays.asList("x", "y"));
        NavigableSet<String> result1 = (NavigableSet<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getEmptyNavigableSetClass());
        NavigableSet<String> result2 = (NavigableSet<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getEmptyNavigableSetClass());
        assertSame(Collections.emptyNavigableSet(), result1);
        assertSame(result1, result2);
        assertThrows(UnsupportedOperationException.class, () -> result1.add("z"));
    }

    @Test
    void createSynchronizedList() {
        List<String> source = Arrays.asList("a", "b");
        List<String> result = (List<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getSynchronizedListClass());
        Class<?> expected = Collections.synchronizedList(new ArrayList<>()).getClass();
        assertSame(expected, result.getClass());
        assertTrue(CollectionUtilities.isSynchronized(result.getClass()));
        synchronized (result) {
            result.add("c");
        }
        assertTrue(result.contains("c"));
    }

    @Test
    void createSynchronizedSortedSet() {
        SortedSet<String> source = new TreeSet<>(Arrays.asList("1", "2"));
        SortedSet<String> result = (SortedSet<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getSynchronizedSortedSetClass());
        Class<?> expected = Collections.synchronizedSortedSet(new TreeSet<>()).getClass();
        assertSame(expected, result.getClass());
        assertTrue(CollectionUtilities.isSynchronized(result.getClass()));
        synchronized (result) {
            result.add("3");
        }
        assertTrue(result.contains("3"));
    }

    @Test
    void createSynchronizedNavigableSet() {
        NavigableSet<String> source = new TreeSet<>(Arrays.asList("x", "y"));
        NavigableSet<String> result = (NavigableSet<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getSynchronizedNavigableSetClass());
        Class<?> expected = Collections.synchronizedNavigableSet(new TreeSet<>()).getClass();
        assertSame(expected, result.getClass());
        assertTrue(CollectionUtilities.isSynchronized(result.getClass()));
        synchronized (result) {
            result.add("z");
        }
        assertTrue(result.contains("z"));
    }
}
