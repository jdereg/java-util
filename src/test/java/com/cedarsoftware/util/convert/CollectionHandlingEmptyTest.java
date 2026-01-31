package com.cedarsoftware.util.convert;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectionHandlingEmptyTest {

    @Test
    void createEmptyCollection() {
        List<String> source = Arrays.asList("a", "b");
        Collection<String> result = (Collection<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getEmptyCollectionClass());
        assertSame(Collections.emptyList(), result);
        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.add("c"));
    }

    @Test
    void createEmptyList() {
        List<String> source = Arrays.asList("x", "y");
        List<String> result = (List<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getEmptyListClass());
        assertSame(Collections.emptyList(), result);
        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.add("z"));
    }

    @Test
    void createEmptySet() {
        Set<String> source = new LinkedHashSet<>(Arrays.asList("1", "2"));
        Set<String> result = (Set<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getEmptySetClass());
        assertSame(Collections.emptySet(), result);
        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.add("3"));
    }

    @Test
    void createEmptySortedSet() {
        SortedSet<String> source = new TreeSet<>(Arrays.asList("m", "n"));
        SortedSet<String> result = (SortedSet<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getEmptySortedSetClass());
        assertSame(Collections.emptySortedSet(), result);
        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.add("o"));
    }

    @Test
    void createEmptyNavigableSet() {
        NavigableSet<String> source = new TreeSet<>(Arrays.asList("p", "q"));
        NavigableSet<String> result = (NavigableSet<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getEmptyNavigableSetClass());
        assertSame(Collections.emptyNavigableSet(), result);
        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.add("r"));
    }
}
