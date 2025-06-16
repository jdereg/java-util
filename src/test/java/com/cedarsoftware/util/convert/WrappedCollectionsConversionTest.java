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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WrappedCollectionsConversionTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void testUnmodifiableCollection() {
        List<String> source = Arrays.asList("apple", "banana", "cherry");

        // Convert to UnmodifiableCollection
        Collection<String> unmodifiableCollection = converter.convert(source, CollectionsWrappers.getUnmodifiableCollectionClass());
        // Assert that the result is an instance of the expected unmodifiable collection class
        assertInstanceOf(CollectionsWrappers.getUnmodifiableCollectionClass(), unmodifiableCollection);
        assertTrue(unmodifiableCollection.containsAll(source));
        // Ensure UnsupportedOperationException is thrown for modifications
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableCollection.add("pear"));

        // Convert to UnmodifiableList
        List<String> unmodifiableList = converter.convert(source, CollectionsWrappers.getUnmodifiableListClass());
        // Assert that the result is an instance of the expected unmodifiable list class
        assertInstanceOf(CollectionsWrappers.getUnmodifiableListClass(), unmodifiableList);
        assertEquals(source, unmodifiableList);
        // Ensure UnsupportedOperationException is thrown for modifications
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableList.add("pear"));
    }

    @Test
    void testCheckedCollections() {
        List<Object> source = Arrays.asList(1, "two", 3);

        // Filter source to include only Integer elements
        List<Integer> integerSource = new ArrayList<>();
        for (Object item : source) {
            if (item instanceof Integer) {
                integerSource.add((Integer) item);
            }
        }

        // Convert to CheckedCollection with Integer type
        Collection<Integer> checkedCollection = converter.convert(integerSource, CollectionsWrappers.getCheckedCollectionClass());
        assertInstanceOf(CollectionsWrappers.getCheckedCollectionClass(), checkedCollection);
        checkedCollection.add(16);
        assertThrows(ClassCastException.class, () -> checkedCollection.add((Integer) (Object) "notAnInteger"));

        // Convert to CheckedSet with Integer type
        Set<Integer> checkedSet = converter.convert(integerSource, CollectionsWrappers.getCheckedSetClass());
        assertInstanceOf(CollectionsWrappers.getCheckedSetClass(), checkedSet);
        assertThrows(ClassCastException.class, () -> checkedSet.add((Integer) (Object) "notAnInteger"));
    }

    @Test
    void testSynchronizedCollections() {
        List<String> source = Arrays.asList("alpha", "beta", "gamma");

        // Convert to SynchronizedCollection
        Collection<String> synchronizedCollection = converter.convert(source, CollectionsWrappers.getSynchronizedCollectionClass());
        // Assert that the result is an instance of the expected synchronized collection class
        assertInstanceOf(CollectionsWrappers.getSynchronizedCollectionClass(), synchronizedCollection);
        assertTrue(synchronizedCollection.contains("alpha"));

        // Convert to SynchronizedSet
        Set<String> synchronizedSet = converter.convert(source, CollectionsWrappers.getSynchronizedSetClass());
        // Assert that the result is an instance of the expected synchronized set class
        assertInstanceOf(CollectionsWrappers.getSynchronizedSetClass(), synchronizedSet);
        synchronized (synchronizedSet) {
            assertTrue(synchronizedSet.contains("beta"));
        }
    }

    @Test
    void testEmptyCollections() {
        List<String> source = Collections.emptyList();

        // Convert to EmptyCollection
        Collection<String> emptyCollection = converter.convert(source, CollectionsWrappers.getEmptyCollectionClass());
        // Assert that the result is an instance of the expected empty collection class
        assertInstanceOf(CollectionsWrappers.getEmptyCollectionClass(), emptyCollection);
        assertTrue(emptyCollection.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyCollection.add("newElement"));

        // Convert to EmptyList
        List<String> emptyList = converter.convert(source, CollectionsWrappers.getEmptyListClass());
        // Assert that the result is an instance of the expected empty list class
        assertInstanceOf(CollectionsWrappers.getEmptyListClass(), emptyList);
        assertTrue(emptyList.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyList.add("newElement"));

        // Convert to EmptyNavigableSet
        NavigableSet<String> emptyNavigableSet = converter.convert(source, CollectionsWrappers.getEmptyNavigableSetClass());
        assertInstanceOf(CollectionsWrappers.getEmptyNavigableSetClass(), emptyNavigableSet);
        assertTrue(emptyNavigableSet.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> emptyNavigableSet.add("newElement"));
    }

    @Test
    void testNestedStructuresWithUnmodifiableCollection() {
        List<Object> source = Arrays.asList(
                Arrays.asList("a", "b", "c"),      // List<String>
                Arrays.asList(1, 2, 3),           // List<Integer>
                Arrays.asList(4.0, 5.0, 6.0)      // List<Double>
        );

        // Convert to Nested UnmodifiableCollection
        Collection<Object> nestedUnmodifiable = converter.convert(source, CollectionsWrappers.getUnmodifiableCollectionClass());

        // Verify top-level collection is unmodifiable
        assertInstanceOf(CollectionsWrappers.getUnmodifiableCollectionClass(), nestedUnmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> nestedUnmodifiable.add(Arrays.asList(7, 8, 9)));

        // Verify nested collections are also unmodifiable ("turtles all the way down.")
        for (Object subCollection : nestedUnmodifiable) {
            assertInstanceOf(CollectionsWrappers.getUnmodifiableCollectionClass(), subCollection);

            // Cast to Collection<Object> for clarity and explicit testing
            Collection<Object> castSubCollection = (Collection<Object>) subCollection;

            // Adding an element should throw an UnsupportedOperationException
            assertThrows(UnsupportedOperationException.class, () -> castSubCollection.add("should fail"));
        }
    }

    @Test
    void testNestedStructuresWithSynchronizedCollection() {
        List<Object> source = Arrays.asList(
                Arrays.asList("a", "b", "c"),      // List<String>
                Arrays.asList(1, 2, 3),           // List<Integer>
                Arrays.asList(4.0, 5.0, 6.0)      // List<Double>
        );

        // Convert to Nested SynchronizedCollection
        Collection<Object> nestedSync = converter.convert(source, CollectionsWrappers.getSynchronizedCollectionClass());
        // Verify top-level collection is synchronized
        assertInstanceOf(CollectionsWrappers.getSynchronizedCollectionClass(), nestedSync);

        // Verify nested collections are also synchronized ("turtles all the way down.")
        for (Object subCollection : nestedSync) {
            assertInstanceOf(CollectionsWrappers.getSynchronizedCollectionClass(), subCollection);
        }
    }

    @Test
    void testNestedStructuresWithCheckedCollection() {
        List<Object> source = Arrays.asList(
                Arrays.asList("a", "b", "c"),      // List<String>
                Arrays.asList(1, 2, 3),           // List<Integer>
                Arrays.asList(4.0, 5.0, 6.0)      // List<Double>
        );

        // Convert to Nested CheckedCollection
        assertThrows(ClassCastException.class, () -> converter.convert(source, CollectionsWrappers.getCheckedCollectionClass()));
    }

    @Test
    void testNestedStructuresWithEmptyCollection() {
        List<Object> source = Arrays.asList(
                Arrays.asList("a", "b", "c"),      // List<String>
                Arrays.asList(1, 2, 3),           // List<Integer>
                Arrays.asList(4.0, 5.0, 6.0)      // List<Double>
        );

        // Convert to Nested EmptyCollection
        assertThrows(UnsupportedOperationException.class, () -> converter.convert(source, CollectionsWrappers.getEmptyCollectionClass()));

        Collection<String> strings = converter.convert(new ArrayList<>(), CollectionsWrappers.getEmptyCollectionClass());
        assert CollectionsWrappers.getEmptyCollectionClass().isAssignableFrom(strings.getClass());
        assert strings.isEmpty();
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
        Collection<Integer> checkedCollection = converter.convert(integerSource, CollectionsWrappers.getCheckedCollectionClass());
        assertInstanceOf(CollectionsWrappers.getCheckedCollectionClass(), checkedCollection);
        // Ensure adding incompatible types throws a ClassCastException
        assertThrows(ClassCastException.class, () -> checkedCollection.add((Integer) (Object) "notAnInteger"));

        // Convert to SynchronizedCollection
        Collection<Object> synchronizedCollection = converter.convert(source, CollectionsWrappers.getSynchronizedCollectionClass());
        assertInstanceOf(CollectionsWrappers.getSynchronizedCollectionClass(), synchronizedCollection);
        assertTrue(synchronizedCollection.contains(1));
    }

    @Test
    void testEmptyAndUnmodifiableInteraction() {
        // EmptyList to UnmodifiableList
        List<String> emptyList = converter.convert(Collections.emptyList(), CollectionsWrappers.getEmptyListClass());
        List<String> unmodifiableList = converter.convert(emptyList, CollectionsWrappers.getUnmodifiableListClass());

        // Verify type and immutability
        assertInstanceOf(List.class, unmodifiableList);
        assertTrue(unmodifiableList.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableList.add("newElement"));
    }

    @Test
    void testNavigableSetToUnmodifiableNavigableSet() {
        NavigableSet<String> source = new TreeSet<>(Arrays.asList("a", "b", "c"));
        NavigableSet<String> result = converter.convert(source, CollectionsWrappers.getUnmodifiableNavigableSetClass());

        assertInstanceOf(NavigableSet.class, result);
        assertTrue(result.contains("a"));
        assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
    }

    @Test
    void testSortedSetToUnmodifiableSortedSet() {
        SortedSet<String> source = new TreeSet<>(Arrays.asList("x", "y", "z"));
        SortedSet<String> result = converter.convert(source, CollectionsWrappers.getUnmodifiableSortedSetClass());

        assertInstanceOf(SortedSet.class, result);
        assertEquals("x", result.first());
        assertThrows(UnsupportedOperationException.class, () -> result.add("w"));
    }

    @Test
    void testListToUnmodifiableList() {
        List<String> source = Arrays.asList("alpha", "beta", "gamma");
        List<String> result = converter.convert(source, CollectionsWrappers.getUnmodifiableListClass());

        assertInstanceOf(List.class, result);
        assertEquals(3, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.add("delta"));
    }

    @Test
    void testMixedCollectionToUnmodifiable() {
        Collection<Object> source = new ArrayList<>(Arrays.asList("one", 2, 3.0));
        Collection<Object> result = converter.convert(source, CollectionsWrappers.getUnmodifiableCollectionClass());

        assertInstanceOf(Collection.class, result);
        assertTrue(result.contains(2));
        assertThrows(UnsupportedOperationException.class, () -> result.add("four"));
    }
}
