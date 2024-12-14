package com.cedarsoftware.util.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrappedCollectionsConversionTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void testToUnmodifiableCollection() {
        List<String> source = Arrays.asList("apple", "banana", "cherry");

        // Convert to UnmodifiableCollection
        Collection<String> unmodifiableCollection = converter.convert(source, WrappedCollections.getUnmodifiableCollection());
        // Assert that the result is an instance of the expected unmodifiable collection class
        assertInstanceOf(WrappedCollections.getUnmodifiableCollection(), unmodifiableCollection);
        assertTrue(unmodifiableCollection.containsAll(source));
        // Ensure UnsupportedOperationException is thrown for modifications
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableCollection.add("pear"));

        // Convert to UnmodifiableList
        List<String> unmodifiableList = converter.convert(source, WrappedCollections.getUnmodifiableList());
        // Assert that the result is an instance of the expected unmodifiable list class
        assertInstanceOf(WrappedCollections.getUnmodifiableList(), unmodifiableList);
        assertEquals(source, unmodifiableList);
        // Ensure UnsupportedOperationException is thrown for modifications
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableList.add("pear"));
    }

    @Test
    void testToCheckedCollections() {
        List<Object> source = Arrays.asList(1, "two", 3);

        // Filter source to include only Integer elements
        List<Integer> integerSource = new ArrayList<>();
        for (Object item : source) {
            if (item instanceof Integer) {
                integerSource.add((Integer) item);
            }
        }

        // Convert to CheckedCollection with Integer type
        Collection<Integer> checkedCollection = converter.convert(integerSource, WrappedCollections.getCheckedCollection());
        assertInstanceOf(WrappedCollections.getCheckedCollection(), checkedCollection);
        assertThrows(ClassCastException.class, () -> checkedCollection.add((Integer) (Object) "notAnInteger"));

        // Convert to CheckedSet with Integer type
        Set<Integer> checkedSet = converter.convert(integerSource, WrappedCollections.getCheckedSet());
        assertInstanceOf(WrappedCollections.getCheckedSet(), checkedSet);
        assertThrows(ClassCastException.class, () -> checkedSet.add((Integer) (Object) "notAnInteger"));
    }

    @Test
    void testToSynchronizedCollections() {
        List<String> source = Arrays.asList("alpha", "beta", "gamma");

        // Convert to SynchronizedCollection
        Collection<String> synchronizedCollection = converter.convert(source, WrappedCollections.getSynchronizedCollection());
        // Assert that the result is an instance of the expected synchronized collection class
        assertInstanceOf(WrappedCollections.getSynchronizedCollection(), synchronizedCollection);
        assertTrue(synchronizedCollection.contains("alpha"));

        // Convert to SynchronizedSet
        Set<String> synchronizedSet = converter.convert(source, WrappedCollections.getSynchronizedSet());
        // Assert that the result is an instance of the expected synchronized set class
        assertInstanceOf(WrappedCollections.getSynchronizedSet(), synchronizedSet);
        synchronized (synchronizedSet) {
            assertTrue(synchronizedSet.contains("beta"));
        }
    }

    @Test
    void testToEmptyCollections() {
        List<String> source = Collections.emptyList();

        // Convert to EmptyCollection
        Collection<String> emptyCollection = converter.convert(source, WrappedCollections.getEmptyCollection());
        // Assert that the result is an instance of the expected empty collection class
        assertInstanceOf(WrappedCollections.getEmptyCollection(), emptyCollection);
        assertTrue(emptyCollection.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyCollection.add("newElement"));

        // Convert to EmptyList
        List<String> emptyList = converter.convert(source, WrappedCollections.getEmptyList());
        // Assert that the result is an instance of the expected empty list class
        assertInstanceOf(WrappedCollections.getEmptyList(), emptyList);
        assertTrue(emptyList.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyList.add("newElement"));
    }

    @Test
    void testNestedStructuresWithWrappedCollections() {
        List<Object> source = Arrays.asList(
                Arrays.asList("a", "b", "c"),      // List<String>
                Arrays.asList(1, 2, 3),           // List<Integer>
                Arrays.asList(4.0, 5.0, 6.0)      // List<Double>
        );

        // Convert to Nested SynchronizedCollection
        Collection<Object> nestedSync = converter.convert(source, WrappedCollections.getSynchronizedCollection());
        // Verify top-level collection is synchronized
        assertInstanceOf(WrappedCollections.getSynchronizedCollection(), nestedSync);

        // Nested collections are not expected to be synchronized
        Class<?> synchronizedClass = WrappedCollections.getSynchronizedCollection();
        for (Object subCollection : nestedSync) {
            assertFalse(synchronizedClass.isAssignableFrom(subCollection.getClass()));
        }

        // Convert to Nested CheckedCollection
        Collection<Object> nestedChecked = converter.convert(source, WrappedCollections.getCheckedCollection());
        // Verify top-level collection is checked
        assertInstanceOf(WrappedCollections.getCheckedCollection(), nestedChecked);

        // Adding a valid collection should succeed
        assertDoesNotThrow(() -> nestedChecked.add(Arrays.asList(7, 8, 9)));
        // Adding an invalid type should throw ClassCastException
        assertThrows(ClassCastException.class, () -> nestedChecked.add("invalid"));
    }

    @Test
    void testWrappedCollectionsWithMixedTypes() {
        List<Object> source = Arrays.asList(1, "two", 3.0);

        // Filter source to include only Integer elements
        List<Integer> integerSource = new ArrayList<>();
        for (Object item : source) {
            if (item instanceof Integer) {
                integerSource.add((Integer) item);
            }
        }

        // Convert to CheckedCollection with Integer type
        Collection<Integer> checkedCollection = converter.convert(integerSource, WrappedCollections.getCheckedCollection());
        assertInstanceOf(WrappedCollections.getCheckedCollection(), checkedCollection);
        // Ensure adding incompatible types throws a ClassCastException
        assertThrows(ClassCastException.class, () -> checkedCollection.add((Integer) (Object) "notAnInteger"));

        // Convert to SynchronizedCollection
        Collection<Object> synchronizedCollection = converter.convert(source, WrappedCollections.getSynchronizedCollection());
        assertInstanceOf(WrappedCollections.getSynchronizedCollection(), synchronizedCollection);
        assertTrue(synchronizedCollection.contains(1));
    }

    @Test
    void testEmptyAndUnmodifiableInteraction() {
        // EmptyList to UnmodifiableList
        List<String> emptyList = converter.convert(Collections.emptyList(), WrappedCollections.getEmptyList());
        List<String> unmodifiableList = converter.convert(emptyList, WrappedCollections.getUnmodifiableList());

        // Verify type and immutability
        assertInstanceOf(List.class, unmodifiableList);
        assertTrue(unmodifiableList.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableList.add("newElement"));
    }

    @Test
    void testNavigableSetToUnmodifiableNavigableSet() {
        NavigableSet<String> source = new TreeSet<>(Arrays.asList("a", "b", "c"));
        NavigableSet<String> result = converter.convert(source, WrappedCollections.getUnmodifiableNavigableSet());

        assertInstanceOf(NavigableSet.class, result);
        assertTrue(result.contains("a"));
        assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
    }

    @Test
    void testSortedSetToUnmodifiableSortedSet() {
        SortedSet<String> source = new TreeSet<>(Arrays.asList("x", "y", "z"));
        SortedSet<String> result = converter.convert(source, WrappedCollections.getUnmodifiableSortedSet());

        assertInstanceOf(SortedSet.class, result);
        assertEquals("x", result.first());
        assertThrows(UnsupportedOperationException.class, () -> result.add("w"));
    }

    @Test
    void testListToUnmodifiableList() {
        List<String> source = Arrays.asList("alpha", "beta", "gamma");
        List<String> result = converter.convert(source, WrappedCollections.getUnmodifiableList());

        assertInstanceOf(List.class, result);
        assertEquals(3, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.add("delta"));
    }

    @Test
    void testMixedCollectionToUnmodifiable() {
        Collection<Object> source = new ArrayList<>(Arrays.asList("one", 2, 3.0));
        Collection<Object> result = converter.convert(source, WrappedCollections.getUnmodifiableCollection());

        assertInstanceOf(Collection.class, result);
        assertTrue(result.contains(2));
        assertThrows(UnsupportedOperationException.class, () -> result.add("four"));
    }
}