package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test to verify normalization behavior of different collection types in MultiKeyMap.
 */
class MultiKeyMapNormalizationDebugTest {

    @Test
    void debugWhatHappensToCollections() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .capacity(1) // Single bucket to force comparisons
                .valueBasedEquality(true)
                .flattenDimensions(false)
                .build();

        // Test 1: Store Object[], lookup with non-RandomAccess collection
        Object[] array = {1, 2};
        map.put(array, "array_stored");

        LinkedList<Integer> linkedList = new LinkedList<>();
        linkedList.add(1);
        linkedList.add(2);

        String result1 = map.get(linkedList);
        assertEquals("array_stored", result1, "Object[] stored, LinkedList lookup should find the value");

        map.clear();

        // Test 2: Store non-RandomAccess collection, lookup with Object[]
        map.put(linkedList, "linkedlist_stored");
        String result2 = map.get(array);
        assertEquals("linkedlist_stored", result2, "LinkedList stored, Object[] lookup should find the value");

        map.clear();

        // Test 3: Store RandomAccess collection (ArrayList), lookup with Object[]
        ArrayList<Integer> arrayList = new ArrayList<>();
        arrayList.add(1);
        arrayList.add(2);
        map.put(arrayList, "arraylist_stored");
        String result3 = map.get(array);
        assertEquals("arraylist_stored", result3, "ArrayList stored, Object[] lookup should find the value");

        map.clear();

        // Test 4: Store Object[], lookup with RandomAccess collection
        map.put(array, "array_stored_2");
        String result4 = map.get(arrayList);
        assertEquals("array_stored_2", result4, "Object[] stored, ArrayList lookup should find the value");

        // Test 5: Custom non-RandomAccess collection
        class NonRACollection<T> implements Collection<T> {
            private final List<T> backing = new ArrayList<>();

            @SafeVarargs
            NonRACollection(T... items) {
                backing.addAll(Arrays.asList(items));
            }

            @Override public int size() { return backing.size(); }
            @Override public boolean isEmpty() { return backing.isEmpty(); }
            @Override public boolean contains(Object o) { return backing.contains(o); }
            @Override public Iterator<T> iterator() { return backing.iterator(); }
            @Override public Object[] toArray() { return backing.toArray(); }
            @Override public <U> U[] toArray(U[] a) { return backing.toArray(a); }
            @Override public boolean add(T t) { return backing.add(t); }
            @Override public boolean remove(Object o) { return backing.remove(o); }
            @Override public boolean containsAll(Collection<?> c) { return backing.containsAll(c); }
            @Override public boolean addAll(Collection<? extends T> c) { return backing.addAll(c); }
            @Override public boolean removeAll(Collection<?> c) { return backing.removeAll(c); }
            @Override public boolean retainAll(Collection<?> c) { return backing.retainAll(c); }
            @Override public void clear() { backing.clear(); }
        }

        map.clear();
        NonRACollection<Integer> nonRA = new NonRACollection<>(1, 2);
        map.put(array, "array_stored_3");
        String result5 = map.get(nonRA);
        assertNotNull(result5, "Custom non-RandomAccess collection lookup should find a value");
        assertEquals("array_stored_3", result5, "Custom non-RA collection should match stored Object[] key");
    }
}
