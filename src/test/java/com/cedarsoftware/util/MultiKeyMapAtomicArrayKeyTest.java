package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An AtomicIntegerArray, AtomicLongArray or AtomicReferenceArray in a MultiKeyMap key is a container like an array,
 * read as the array of its current values -- on every path and at any depth. put(), get(), containsKey() and remove()
 * took one as a plain object compared by identity, while putIfAbsent(), the compute methods and merge() read it as
 * its values: a key stored by computeIfAbsent() was not found by get() with the same object, put() of it made a
 * second entry, and two atomic arrays with the same values were two keys. Nested in a key it was always an identity
 * element.
 */
class MultiKeyMapAtomicArrayKeyTest {

    private static AtomicIntegerArray ints(int... values) {
        return new AtomicIntegerArray(values);
    }

    @Test
    void everyWritePathAndLookupReadsTheValues() {
        java.util.Map<String, BiConsumer<MultiKeyMap<String>, Object>> writers = new java.util.LinkedHashMap<>();
        writers.put("put", (m, k) -> m.put(k, "v"));
        writers.put("putIfAbsent", (m, k) -> m.putIfAbsent(k, "v"));
        writers.put("computeIfAbsent", (m, k) -> m.computeIfAbsent(k, x -> "v"));
        writers.put("compute", (m, k) -> m.compute(k, (x, y) -> "v"));
        writers.put("merge", (m, k) -> m.merge(k, "v", (x, y) -> "v"));
        for (java.util.Map.Entry<String, BiConsumer<MultiKeyMap<String>, Object>> w : writers.entrySet()) {
            AtomicIntegerArray key = ints(1, 2, 3);
            MultiKeyMap<String> map = new MultiKeyMap<>();
            w.getValue().accept(map, key);
            String how = w.getKey();
            assertEquals("v", map.get(key), how);
            assertTrue(map.containsKey(key), how);
            assertEquals("v", map.get(ints(1, 2, 3)), how);
            assertEquals("v", map.get(new int[]{1, 2, 3}), how);
            assertEquals("v", map.get(Arrays.asList(1L, 2L, 3L)), how);
            assertEquals("v", map.getMultiKey(1, 2, 3), how);
            map.put(key, "again");
            assertEquals(1, map.size(), how + ": no second entry");
            assertEquals("again", map.remove(ints(1, 2, 3)), how);
            assertTrue(map.isEmpty(), how);
        }
    }

    @Test
    void eachAtomicArrayTypeIsItsValues() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new AtomicLongArray(new long[]{4, 5}), "longs");
        map.put(new AtomicReferenceArray<>(new Object[]{"a", 6}), "refs");
        assertEquals("longs", map.get(new long[]{4, 5}));
        assertEquals("longs", map.get(new AtomicLongArray(new long[]{4, 5})));
        assertEquals("refs", map.get(new Object[]{"a", 6L}));
        assertEquals("refs", map.get(new AtomicReferenceArray<>(new Object[]{"a", 6})));
        assertEquals(2, map.size());
    }

    /** Nested at any depth, in a List, an array or a Set, and in flatten mode. */
    @Test
    void aNestedAtomicArrayIsItsValues() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{"x", new AtomicLongArray(new long[]{4, 5})}, "nested");
        map.put(new LinkedHashSet<>(Arrays.asList("s", ints(7, 8))), "in a set");
        assertEquals("nested", map.get(new Object[]{"x", new AtomicLongArray(new long[]{4, 5})}));
        assertEquals("nested", map.get(Arrays.asList("x", new long[]{4, 5})));
        assertEquals("nested", map.get(new Object[]{"x", Arrays.asList(4, 5)}));
        assertNull(map.get(new Object[]{"x", 4, 5}), "nesting still counts");
        assertEquals("in a set", map.get(new LinkedHashSet<>(Arrays.asList(new int[]{7, 8}, "s"))));

        MultiKeyMap<String> flat = MultiKeyMap.<String>builder().flattenDimensions(true).build();
        flat.put(new Object[]{"x", ints(1, 2)}, "flat");
        assertEquals("flat", flat.get(new Object[]{"x", 1, 2}));
        assertEquals("flat", flat.get(Arrays.asList(new AtomicReferenceArray<>(new Object[]{"x"}), 1, 2)));
    }

    /** The key is the values when it was stored: changing the atomic array afterwards cannot corrupt the map. */
    @Test
    void theKeyIsTheValuesAtTheTimeOfTheCall() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        AtomicIntegerArray key = ints(1, 2, 3);
        map.put(key, "v");
        key.set(0, 9);
        assertNull(map.get(key));
        assertEquals("v", map.get(new int[]{1, 2, 3}));
        Object reported = map.keySet().iterator().next();
        assertArrayEquals(new int[]{1, 2, 3}, (int[]) reported);
    }

    /** An AtomicReferenceArray holding itself is a cycle, as an array holding itself is, not endless recursion. */
    @Test
    void anAtomicReferenceArrayHoldingItselfIsACycle() {
        AtomicReferenceArray<Object> self = new AtomicReferenceArray<>(2);
        self.set(0, "a");
        self.set(1, self);
        for (boolean flatten : new boolean[]{false, true}) {
            MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(flatten).build();
            map.put(self, "self");
            map.put(new Object[]{"k", self}, "nested");
            assertEquals("self", map.get(self));
            assertEquals("nested", map.get(new Object[]{"k", self}));
            assertEquals(2, map.size());
        }
    }

    /** A lookup that only reads a value: the same answer for the atomic array and its values, every time. */
    @Test
    void getOrDefaultAndComputeIfPresentAgree() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new int[]{1, 2}, "v");
        Function<Object, String> read = k -> map.getOrDefault(k, "none");
        assertEquals("v", read.apply(ints(1, 2)));
        assertEquals("v!", map.computeIfPresent(ints(1, 2), (k, v) -> v + "!"));
        assertEquals("v!", map.get(ints(1, 2)));
    }
}
