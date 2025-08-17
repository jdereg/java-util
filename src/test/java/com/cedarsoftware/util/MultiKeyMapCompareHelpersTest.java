package com.cedarsoftware.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class MultiKeyMapCompareHelpersTest {

    // Helper: tiny map with a single bucket so mismatched hashes still compare
    private static <V> MultiKeyMap<V> map(boolean valueBasedEquality) {
        return MultiKeyMap.<V>builder()
                .capacity(1)                 // force all entries into same bucket
                .valueBasedEquality(valueBasedEquality)
                .flattenDimensions(false)    // keep 1D containers as containers
                .build();
    }

    // -------- compareObjectArrayToRandomAccess (value-based = true) --------

    @Test
    void objectArray_vs_randomAccess_valueMode_identityAndValueEqual() {
        MultiKeyMap<String> m = map(true);

        Object shared = new Object();
        Object[] arrKey = { shared, 1, 2 };
        m.put(arrKey, "ok");

        // RandomAccess list (ArrayList) to trigger compareObjectArrayToRandomAccess
        List<Object> raListLookup = new ArrayList<>(Arrays.asList(shared, 1.0, 2.0));
        assertEquals("ok", m.get(raListLookup)); // identity fast-path for index 0; valueEquals for others
    }

    @Test
    void objectArray_vs_randomAccess_valueMode_notEqual_returnsNull() {
        MultiKeyMap<String> m = map(true);

        m.put(new Object[]{1, 2}, "v");
        // Index 1 differs -> valueEquals returns false -> method returns false
        List<Object> raListLookup = new ArrayList<>(Arrays.asList(1, 3.0));
        assertNull(m.get(raListLookup));
    }

    // -------- compareObjectArrayToRandomAccess (value-based = false) --------

    @Test
    void objectArray_vs_randomAccess_typeStrict_atomicEqual_isTrue() {
        MultiKeyMap<String> m = map(false);

        Object[] arrKey = { 1, "x", new AtomicInteger(5) };
        m.put(arrKey, "hit");

        // Same content types; atomicValueEquals(true) path is taken
        List<Object> raListLookup = new ArrayList<>(Arrays.asList(1, "x", new AtomicInteger(5)));
        assertEquals("hit", m.get(raListLookup));
    }

    @Test
    void objectArray_vs_randomAccess_typeStrict_atomicNotEqual_returnsNull() {
        MultiKeyMap<String> m = map(false);

        m.put(new Object[]{ new AtomicInteger(1) }, "a");
        List<Object> raListLookup = new ArrayList<>(Arrays.asList(new AtomicInteger(2)));
        assertNull(m.get(raListLookup)); // atomicValueEquals(false) -> return false
    }

    @Test
    void objectArray_vs_randomAccess_typeStrict_mismatchedWrapperTypes_returnsNull() {
        MultiKeyMap<String> m = map(false);

        m.put(new Object[]{ 1 }, "z");
        List<Object> raListLookup = new ArrayList<>(Arrays.asList(1L)); // Integer vs Long (strict) -> Objects.equals false
        assertNull(m.get(raListLookup));
    }

    // -------- compareRandomAccessToObjectArray (delegation path) --------

    @Test
    void randomAccess_vs_objectArray_valueMode_delegatesAndMatches() {
        MultiKeyMap<String> m = map(true);

        // Store RandomAccess list key
        List<Object> raListKey = new ArrayList<>(Arrays.asList(1, 2));
        m.put(raListKey, "ok");

        // Lookup with Object[] so we hit compareRandomAccessToObjectArray -> delegate to compareObjectArrayToRandomAccess
        Object[] arrLookup = { 1.0, 2.0 };
        assertEquals("ok", m.get(arrLookup));
    }

    // -------- compareObjectArrayToCollection (non-RandomAccess iterator path) --------

    @Test
    void objectArray_vs_nonRandomAccess_valueMode_iteratorMatches() {
        MultiKeyMap<String> m = map(true);

        m.put(new Object[]{ 1, 2 }, "val");

        // LinkedList is not RandomAccess -> triggers compareObjectArrayToCollection
        Collection<Object> nonRaLookup = new LinkedList<>(Arrays.asList(1, 2.0));
        assertEquals("val", m.get(nonRaLookup));
    }

    @Test
    void objectArray_vs_nonRandomAccess_typeStrict_mismatch_returnsNull() {
        MultiKeyMap<String> m = map(false);

        m.put(new Object[]{ 1 }, "v");
        Collection<Object> nonRaLookup = new LinkedList<>(Arrays.asList(1L)); // Integer vs Long in strict mode
        assertNull(m.get(nonRaLookup));
    }

    @Test
    void objectArray_vs_nonRandomAccess_identityFastPathContinues() {
        MultiKeyMap<String> m = map(true);

        Object shared = new Object();
        m.put(new Object[]{ shared, "x" }, "id");

        Collection<Object> nonRaLookup = new LinkedList<>(Arrays.asList(shared, "x"));
        assertEquals("id", m.get(nonRaLookup)); // exercises (a == b) continue branch with iterator
    }
}