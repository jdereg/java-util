package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentListAdditionalTest {

    @Test
    void testConstructorWithSize() {
        List<Integer> list = new ConcurrentList<>(10);
        assertTrue(list.isEmpty(), "List should be empty after construction with capacity");
        list.add(1);
        assertEquals(1, list.size());
    }

    @Test
    void testConstructorCopiesExistingList() {
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b"));
        ConcurrentList<String> list = new ConcurrentList<>(backing);
        list.add("c");
        // New implementation copies rather than wraps, so backing list is unmodified
        assertEquals(Arrays.asList("a", "b"), backing);
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    void testConstructorRejectsNullList() {
        assertThrows(NullPointerException.class, () -> new ConcurrentList<>((Collection<String>) null));
    }

    @Test
    void testEqualsHashCodeAndToString() {
        ConcurrentList<Integer> list1 = new ConcurrentList<>();
        list1.addAll(Arrays.asList(1, 2, 3));
        ConcurrentList<Integer> list2 = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 2, 3)));

        assertEquals(list1, list2);
        assertEquals(list1.hashCode(), list2.hashCode());
        assertEquals(Arrays.asList(1, 2, 3).toString(), list1.toString());
    }

    @Test
    void testListIteratorStartingAtIndex() {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(0, 1, 2, 3, 4));
        ListIterator<Integer> iterator = list.listIterator(2);

        // Test iteration without concurrent modification
        List<Integer> snapshot = new ArrayList<>();
        while (iterator.hasNext()) {
            snapshot.add(iterator.next());
        }

        assertEquals(Arrays.asList(2, 3, 4), snapshot);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), list); // Original list unchanged
    }

    // Note: testWithReadLockVoid() removed as it was specific to the old lock-based implementation
    // The new map-based implementation doesn't require this internal method
}

