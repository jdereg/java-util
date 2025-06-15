package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentListAdditionalTest {

    @Test
    void testConstructorWithSize() {
        List<Integer> list = new ConcurrentList<>(10);
        assertTrue(list.isEmpty(), "List should be empty after construction with capacity");
        list.add(1);
        assertEquals(1, list.size());
    }

    @Test
    void testConstructorWrapsExistingList() {
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b"));
        ConcurrentList<String> list = new ConcurrentList<>(backing);
        list.add("c");
        assertEquals(Arrays.asList("a", "b", "c"), backing);
    }

    @Test
    void testConstructorRejectsNullList() {
        assertThrows(IllegalArgumentException.class, () -> new ConcurrentList<>(null));
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
    void testListIteratorReturnsSnapshotStartingAtIndex() {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(0, 1, 2, 3, 4));
        ListIterator<Integer> iterator = list.listIterator(2);

        list.add(5); // modify after iterator creation

        List<Integer> snapshot = new ArrayList<>();
        while (iterator.hasNext()) {
            snapshot.add(iterator.next());
            iterator.remove();
        }

        assertEquals(Arrays.asList(2, 3, 4), snapshot);
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), list);
    }

    @Test
    void testWithReadLockVoid() throws Exception {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        AtomicBoolean flag = new AtomicBoolean(false);
        Method m = ConcurrentList.class.getDeclaredMethod("withReadLockVoid", Runnable.class);
        m.setAccessible(true);
        m.invoke(list, (Runnable) () -> flag.set(true));
        assertTrue(flag.get());

        m.invoke(list, (Runnable) () -> { throw new RuntimeException("boom"); });
        list.add(1); // should not deadlock if lock released
        assertEquals(1, list.size());
    }
}

