package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompactSetMethodsTest {

    @Test
    void testContainsAll() {
        CompactSet<Integer> set = new CompactSet<>();
        set.addAll(Arrays.asList(1, 2, 3));

        assertTrue(set.containsAll(Arrays.asList(1, 2, 3)));
        assertFalse(set.containsAll(Arrays.asList(1, 4)));
    }

    @Test
    void testRetainAll() {
        CompactSet<Integer> set = new CompactSet<>();
        set.addAll(Arrays.asList(1, 2, 3, 4));

        assertTrue(set.retainAll(Arrays.asList(2, 3)));
        assertEquals(new HashSet<>(Arrays.asList(2, 3)), new HashSet<>(set));

        assertFalse(set.retainAll(Arrays.asList(2, 3)));
    }

    @Test
    void testRemoveAll() {
        CompactSet<String> set = new CompactSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));

        assertTrue(set.removeAll(Arrays.asList("a", "c")));
        assertEquals(new HashSet<>(Arrays.asList("b")), new HashSet<>(set));

        assertFalse(set.removeAll(Arrays.asList("x", "y")));
        assertEquals(1, set.size());
    }

    @Test
    void testToArray() {
        CompactSet<String> set = CompactSet.<String>builder().insertionOrder().build();
        set.add("one");
        set.add("two");

        String[] small = set.toArray(new String[0]);
        assertArrayEquals(new String[]{"one", "two"}, small);

        String[] large = set.toArray(new String[3]);
        assertArrayEquals(new String[]{"one", "two", null}, large);
    }

    @Test
    void testHashCodeAndToString() {
        CompactSet<String> set1 = CompactSet.<String>builder().insertionOrder().build();
        set1.add("a");
        set1.add("b");

        CompactSet<String> set2 = CompactSet.<String>builder().insertionOrder().build();
        set2.add("b");
        set2.add("a");

        assertEquals(set1.hashCode(), set2.hashCode());
        assertNotEquals(set1.toString(), set2.toString());

        CompactSet<String> set3 = CompactSet.<String>builder().insertionOrder().build();
        set3.add("a");
        set3.add("c");

        assertNotEquals(set1.hashCode(), set3.hashCode());
        assertNotEquals(set1.toString(), set3.toString());
    }
}
