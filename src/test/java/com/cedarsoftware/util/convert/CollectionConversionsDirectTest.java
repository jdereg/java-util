package com.cedarsoftware.util.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.cedarsoftware.util.CollectionUtilities;
import com.cedarsoftware.util.convert.CollectionsWrappers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollectionConversionsDirectTest {

    @Test
    void arrayToCollectionHandlesNestedArrays() {
        Object[] array = {"a", new String[]{"b", "c"}};
        Collection<?> result = CollectionConversions.arrayToCollection(array, List.class);
        assertEquals(2, result.size());
        assertTrue(result.contains("a"));
        Object nested = result.stream().filter(e -> e instanceof Collection).findFirst().orElse(null);
        assertNotNull(nested);
        assertEquals(CollectionUtilities.listOf("b", "c"), new ArrayList<>((Collection<?>) nested));
        assertDoesNotThrow(() -> ((Collection<Object>) result).add("d"));
    }

    @Test
    void arrayToCollectionCreatesUnmodifiable() {
        Class<? extends Collection<?>> type = CollectionsWrappers.getUnmodifiableCollectionClass();
        Collection<?> result = CollectionConversions.arrayToCollection(new Integer[]{1, 2}, type);
        assertTrue(CollectionUtilities.isUnmodifiable(result.getClass()));
        assertThrows(UnsupportedOperationException.class,
                () -> ((Collection<Object>) result).add(3));
    }

    @Test
    void arrayToCollectionCreatesSynchronized() {
        Class<? extends Collection<?>> type = CollectionsWrappers.getSynchronizedCollectionClass();
        Collection<?> result = CollectionConversions.arrayToCollection(new String[]{"x"}, type);
        assertTrue(CollectionUtilities.isSynchronized(result.getClass()));
        assertDoesNotThrow(() -> ((Collection<Object>) result).add("y"));
    }

    @Test
    void collectionToCollectionHandlesNestedCollections() {
        List<Object> source = Arrays.asList("a", Arrays.asList("b", "c"));
        Collection<?> result = (Collection<?>) CollectionConversions.collectionToCollection(source, Set.class);
        assertEquals(2, result.size());
        assertTrue(result.contains("a"));
        Object nested = result.stream().filter(e -> e instanceof Collection).findFirst().orElse(null);
        assertNotNull(nested);
        assertInstanceOf(Set.class, nested);
        assertEquals(CollectionUtilities.setOf("b", "c"), new HashSet<>((Collection<?>) nested));
    }

    @Test
    void collectionToCollectionProducesUnmodifiable() {
        Class<?> type = Collections.unmodifiableCollection(new ArrayList<>()).getClass();
        Collection<?> result = (Collection<?>) CollectionConversions.collectionToCollection(CollectionUtilities.listOf(1, 2), type);
        assertTrue(CollectionUtilities.isUnmodifiable(result.getClass()));
        assertThrows(UnsupportedOperationException.class,
                () -> ((Collection<Object>) result).add(3));
    }

    @Test
    void collectionToCollectionProducesSynchronized() {
        Class<?> type = Collections.synchronizedCollection(new ArrayList<>()).getClass();
        Collection<?> result = (Collection<?>) CollectionConversions.collectionToCollection(CollectionUtilities.listOf("a"), type);
        assertTrue(CollectionUtilities.isSynchronized(result.getClass()));
        assertDoesNotThrow(() -> ((Collection<Object>) result).add("b"));
    }
}

