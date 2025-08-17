package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that intentionally use the public API to exercise the internal
 * compareObjectArrayToCollection(...) and compareCollectionToObjectArray(...)
 * helpers, with meaningful assertions.
 */
class MultiKeyMap_ArrayVsCollectionComparisonTest {

    // Small helper: force one bucket so key collisions still compare
    private static <V> MultiKeyMap<V> map(boolean valueBased) {
        return MultiKeyMap.<V>builder()
                .capacity(1)
                .valueBasedEquality(valueBased)
                .flattenDimensions(false)
                .build();
    }

    // --- compareObjectArrayToCollection (non-RandomAccess) ---

    @Test
    void objectArray_vs_nonRandomAccess_valueBased_identityAndNumericEquality_returnsHit() {
        MultiKeyMap<String> m = map(true);

        Object shared = new Object();
        // Store as Object[] key
        m.put(new Object[]{ shared, 1, 2.0, null }, "OK");

        // Lookup with a non-RandomAccess Collection (LinkedList → iterator path)
        Collection<Object> lookup = new LinkedList<>(Arrays.asList(shared, 1.0, 2, null));

        // Expect match: index 0 hits identity fast-path (a == b),
        // index 1 & 2 use value-based numeric equality, index 3 is null==null.
        assertEquals("OK", m.get(lookup));
    }

    @Test
    void objectArray_vs_nonRandomAccess_valueBased_mismatch_returnsNull() {
        MultiKeyMap<String> m = map(true);

        m.put(new Object[]{ 1, 2 }, "V");
        // Second element mismatches → valueEquals false → early return false from helper
        Collection<Object> lookup = new LinkedList<>(Arrays.asList(1, 3.0));
        assertNull(m.get(lookup));
    }

    @Test
    void objectArray_vs_nonRandomAccess_typeStrict_atomicEqual_returnsHit() {
        MultiKeyMap<String> m = map(false);

        // Include an AtomicInteger so the strict-mode "atomicValueEquals" branch is executed
        m.put(new Object[]{ new AtomicInteger(5) }, "HIT");

        Collection<Object> lookup = new LinkedList<>(Arrays.asList(new AtomicInteger(5)));
        assertEquals("HIT", m.get(lookup));
    }

    @Test
    void objectArray_vs_nonRandomAccess_typeStrict_atomicNotEqual_returnsNull() {
        MultiKeyMap<String> m = map(false);

        m.put(new Object[]{ new AtomicInteger(1) }, "X");
        Collection<Object> lookup = new LinkedList<>(Arrays.asList(new AtomicInteger(2)));
        assertNull(m.get(lookup));   // atomicValueEquals → false → early return
    }

    @Test
    void objectArray_vs_nonRandomAccess_typeStrict_wrapperTypeMismatch_returnsNull() {
        MultiKeyMap<String> m = map(false);

        m.put(new Object[]{ 1 }, "Z");
        // Integer vs Long in strict mode → Objects.equals false → early return
        Collection<Object> lookup = new LinkedList<>(Arrays.asList(1L));
        assertNull(m.get(lookup));
    }

    @Test
    void objectArray_vs_nonRandomAccess_typeStrict_identityFastPath_then_equals_returnsHit() {
        MultiKeyMap<String> m = map(false);

        Object shared = new Object();
        m.put(new Object[]{ shared, 42 }, "ID");

        Collection<Object> lookup = new LinkedList<>(Arrays.asList(shared, 42));
        assertEquals("ID", m.get(lookup));   // exercises (a == b) continue w/ iterator path
    }

    // --- compareCollectionToObjectArray (delegation) ---
    // To reach this method, we need the stored key to normalize as a Collection (not List-based),
    // yet be RandomAccess so process1DCollection keeps it as a Collection. We create a custom
    // Collection that implements RandomAccess but is NOT a List.

    private static final class RACollection<E> implements Collection<E>, RandomAccess {
        private final ArrayList<E> delegate = new ArrayList<>();
        RACollection(@SuppressWarnings("unchecked") E... items) { delegate.addAll(Arrays.asList(items)); }
        @Override public int size() { return delegate.size(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public boolean contains(Object o) { return delegate.contains(o); }
        @Override public Iterator<E> iterator() { return delegate.iterator(); }
        @Override public Object[] toArray() { return delegate.toArray(); }
        @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
        @Override public boolean add(E e) { return delegate.add(e); }
        @Override public boolean remove(Object o) { return delegate.remove(o); }
        @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
        @Override public boolean addAll(Collection<? extends E> c) { return delegate.addAll(c); }
        @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
        @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
        @Override public void clear() { delegate.clear(); }
    }

    @Test
    void nonListRandomAccessCollection_vs_objectArray_valueBased_delegatesAndMatches() {
        MultiKeyMap<String> m = map(true);

        // Stored as a RandomAccess (but not List) Collection => stays a Collection in normalization.
        m.put(new RACollection<>(1, 2), "OK");

        // Lookup with Object[] triggers "Collection vs Object[]" case,
        // non-List path calls compareCollectionToObjectArray(...) which delegates to compareObjectArrayToCollection(...)
        Object[] lookup = { 1.0, 2.0 };
        assertEquals("OK", m.get(lookup));
    }

    @Test
    void nonListRandomAccessCollection_vs_objectArray_typeStrict_mismatch_returnsNull() {
        MultiKeyMap<String> m = map(false);

        m.put(new RACollection<>(1), "X");
        Object[] lookup = { 1L };   // Integer vs Long in strict mode → mismatch
        assertNull(m.get(lookup));
    }
}