package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CollectionHandlingCheckedTest {

    @Test
    void createCheckedNavigableSet() {
        NavigableSet<String> source = new TreeSet<>(Arrays.asList("a", "b"));
        NavigableSet<String> result = (NavigableSet<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getCheckedNavigableSetClass());
        assertInstanceOf(CollectionsWrappers.getCheckedNavigableSetClass(), result);
        result.add("c");
        assertTrue(result.contains("c"));
        assertThrows(ClassCastException.class, () -> ((NavigableSet) result).add(1));
    }

    @Test
    void createCheckedSortedSet() {
        SortedSet<String> source = new TreeSet<>(Arrays.asList("x", "y"));
        SortedSet<String> result = (SortedSet<String>) CollectionHandling.createCollection(source,
                CollectionsWrappers.getCheckedSortedSetClass());
        assertInstanceOf(CollectionsWrappers.getCheckedSortedSetClass(), result);
        result.add("z");
        assertTrue(result.contains("z"));
        assertThrows(ClassCastException.class, () -> ((SortedSet) result).add(2));
    }
}
