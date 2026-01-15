package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IdentitySet - a high-performance Set using identity comparison.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         Copyright (c) Cedar Software LLC
 *         Licensed under the Apache License, Version 2.0
 */
class IdentitySetTest {

    @Test
    void testBasicAddAndContains() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object obj1 = new Object();
        Object obj2 = new Object();

        assertTrue(set.add(obj1));
        assertTrue(set.contains(obj1));
        assertFalse(set.contains(obj2));
        assertEquals(1, set.size());

        assertTrue(set.add(obj2));
        assertTrue(set.contains(obj1));
        assertTrue(set.contains(obj2));
        assertEquals(2, set.size());
    }

    @Test
    void testAddDuplicate() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object obj = new Object();

        assertTrue(set.add(obj));
        assertEquals(1, set.size());

        // Adding same object again should return false
        assertFalse(set.add(obj));
        assertEquals(1, set.size());
    }

    @Test
    void testIdentityVsEquals() {
        IdentitySet<String> set = new IdentitySet<>();

        // Two strings with same content but different identity
        String s1 = new String("test");
        String s2 = new String("test");

        // Verify they are equal but not identical
        assertEquals(s1, s2);
        assertNotSame(s1, s2);

        // Both should be added because identity is different
        assertTrue(set.add(s1));
        assertTrue(set.add(s2));
        assertEquals(2, set.size());

        // Only the identical object should be found
        assertTrue(set.contains(s1));
        assertTrue(set.contains(s2));

        // A third string with same content should not be found
        String s3 = new String("test");
        assertFalse(set.contains(s3));
    }

    @Test
    void testRemove() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object obj3 = new Object();

        set.add(obj1);
        set.add(obj2);
        set.add(obj3);
        assertEquals(3, set.size());

        assertTrue(set.remove(obj2));
        assertEquals(2, set.size());
        assertFalse(set.contains(obj2));
        assertTrue(set.contains(obj1));
        assertTrue(set.contains(obj3));

        // Removing non-existent object
        assertFalse(set.remove(obj2));
        assertFalse(set.remove(new Object()));
    }

    @Test
    void testRemoveAndReAdd() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object obj = new Object();

        set.add(obj);
        assertTrue(set.contains(obj));

        set.remove(obj);
        assertFalse(set.contains(obj));

        // Should be able to add again after removal
        assertTrue(set.add(obj));
        assertTrue(set.contains(obj));
    }

    @Test
    void testClear() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object obj1 = new Object();
        Object obj2 = new Object();

        set.add(obj1);
        set.add(obj2);
        assertEquals(2, set.size());

        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
        assertFalse(set.contains(obj1));
        assertFalse(set.contains(obj2));
    }

    @Test
    void testIsEmpty() {
        IdentitySet<Object> set = new IdentitySet<>();
        assertTrue(set.isEmpty());

        Object obj = new Object();
        set.add(obj);
        assertFalse(set.isEmpty());

        set.remove(obj);
        assertTrue(set.isEmpty());
    }

    @Test
    void testNullElement() {
        IdentitySet<Object> set = new IdentitySet<>();

        assertThrows(NullPointerException.class, () -> set.add(null));
        assertFalse(set.contains(null));
        assertFalse(set.remove(null));
    }

    @Test
    void testResize() {
        // Start with small capacity and add many elements to trigger resize
        IdentitySet<Object> set = new IdentitySet<>(4);
        Object[] objects = new Object[100];

        for (int i = 0; i < 100; i++) {
            objects[i] = new Object();
            assertTrue(set.add(objects[i]));
        }

        assertEquals(100, set.size());

        // Verify all objects are still accessible after multiple resizes
        for (int i = 0; i < 100; i++) {
            assertTrue(set.contains(objects[i]));
        }
    }

    @Test
    void testCustomInitialCapacity() {
        IdentitySet<Object> set = new IdentitySet<>(1000);
        Object obj = new Object();

        set.add(obj);
        assertTrue(set.contains(obj));
        assertEquals(1, set.size());
    }

    @Test
    void testMixedOperations() {
        IdentitySet<Object> set = new IdentitySet<>();
        Object[] objects = new Object[50];

        // Add all
        for (int i = 0; i < 50; i++) {
            objects[i] = new Object();
            set.add(objects[i]);
        }
        assertEquals(50, set.size());

        // Remove even indices
        for (int i = 0; i < 50; i += 2) {
            assertTrue(set.remove(objects[i]));
        }
        assertEquals(25, set.size());

        // Verify odd indices still present, even indices gone
        for (int i = 0; i < 50; i++) {
            if (i % 2 == 0) {
                assertFalse(set.contains(objects[i]));
            } else {
                assertTrue(set.contains(objects[i]));
            }
        }

        // Re-add some removed elements
        for (int i = 0; i < 10; i += 2) {
            assertTrue(set.add(objects[i]));
        }
        assertEquals(30, set.size());
    }

    @Test
    void testProbeChainAfterRemoval() {
        // This tests that removal with DELETED sentinel doesn't break probe chains
        IdentitySet<Object> set = new IdentitySet<>(4);

        // Add objects that might hash to same bucket
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object obj3 = new Object();

        set.add(obj1);
        set.add(obj2);
        set.add(obj3);

        // Remove middle element
        set.remove(obj2);

        // obj3 should still be findable (probe chain not broken)
        assertTrue(set.contains(obj1));
        assertFalse(set.contains(obj2));
        assertTrue(set.contains(obj3));
    }

    @Test
    void testWithInternedStrings() {
        IdentitySet<String> set = new IdentitySet<>();

        String s1 = "interned";  // Interned string
        String s2 = "interned";  // Same interned string

        // These should be the same object (interned)
        assertSame(s1, s2);

        assertTrue(set.add(s1));
        assertFalse(set.add(s2));  // Same object, already present
        assertEquals(1, set.size());
    }

    @Test
    void testWithIntegerCaching() {
        IdentitySet<Integer> set = new IdentitySet<>();

        // Small integers are cached by JVM
        Integer i1 = 100;
        Integer i2 = 100;
        assertSame(i1, i2);  // Same cached object

        assertTrue(set.add(i1));
        assertFalse(set.add(i2));  // Same object
        assertEquals(1, set.size());

        // Large integers are not cached
        Integer i3 = 1000;
        Integer i4 = 1000;
        assertNotSame(i3, i4);  // Different objects

        assertTrue(set.add(i3));
        assertTrue(set.add(i4));  // Different object
        assertEquals(3, set.size());
    }

    // ============== Tests for Set interface compliance ==============

    @Test
    void testIterator() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");
        String s3 = new String("c");

        set.add(s1);
        set.add(s2);
        set.add(s3);

        List<String> iterated = new ArrayList<>();
        for (String s : set) {
            iterated.add(s);
        }

        assertEquals(3, iterated.size());
        assertTrue(iterated.contains(s1));
        assertTrue(iterated.contains(s2));
        assertTrue(iterated.contains(s3));
    }

    @Test
    void testIteratorRemove() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");
        String s3 = new String("c");

        set.add(s1);
        set.add(s2);
        set.add(s3);

        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String s = it.next();
            if (s == s2) {
                it.remove();
            }
        }

        assertEquals(2, set.size());
        assertTrue(set.contains(s1));
        assertFalse(set.contains(s2));
        assertTrue(set.contains(s3));
    }

    @Test
    void testIteratorEmptySet() {
        IdentitySet<Object> set = new IdentitySet<>();
        Iterator<Object> it = set.iterator();

        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void testIteratorRemoveWithoutNext() {
        IdentitySet<Object> set = new IdentitySet<>();
        set.add(new Object());

        Iterator<Object> it = set.iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void testIteratorDoubleRemove() {
        IdentitySet<Object> set = new IdentitySet<>();
        set.add(new Object());

        Iterator<Object> it = set.iterator();
        it.next();
        it.remove();
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void testConstructorFromCollection() {
        List<String> list = Arrays.asList(
                new String("a"),
                new String("b"),
                new String("c")
        );

        IdentitySet<String> set = new IdentitySet<>(list);

        assertEquals(3, set.size());
        for (String s : list) {
            assertTrue(set.contains(s));
        }
    }

    @Test
    void testAddAll() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");

        set.addAll(Arrays.asList(s1, s2));

        assertEquals(2, set.size());
        assertTrue(set.contains(s1));
        assertTrue(set.contains(s2));
    }

    @Test
    void testRemoveAll() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");
        String s3 = new String("c");

        set.add(s1);
        set.add(s2);
        set.add(s3);

        set.removeAll(Arrays.asList(s1, s3));

        assertEquals(1, set.size());
        assertFalse(set.contains(s1));
        assertTrue(set.contains(s2));
        assertFalse(set.contains(s3));
    }

    @Test
    void testRetainAll() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");
        String s3 = new String("c");

        set.add(s1);
        set.add(s2);
        set.add(s3);

        set.retainAll(Arrays.asList(s2));

        assertEquals(1, set.size());
        assertFalse(set.contains(s1));
        assertTrue(set.contains(s2));
        assertFalse(set.contains(s3));
    }

    @Test
    void testContainsAll() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");
        String s3 = new String("c");

        set.add(s1);
        set.add(s2);
        set.add(s3);

        assertTrue(set.containsAll(Arrays.asList(s1, s2)));
        assertTrue(set.containsAll(Arrays.asList(s1, s2, s3)));
        assertFalse(set.containsAll(Arrays.asList(s1, new String("d"))));
    }

    @Test
    void testToArray() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");

        set.add(s1);
        set.add(s2);

        Object[] arr = set.toArray();
        assertEquals(2, arr.length);

        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains(s1));
        assertTrue(list.contains(s2));
    }

    @Test
    void testToArrayTyped() {
        IdentitySet<String> set = new IdentitySet<>();
        String s1 = new String("a");
        String s2 = new String("b");

        set.add(s1);
        set.add(s2);

        String[] arr = set.toArray(new String[0]);
        assertEquals(2, arr.length);

        List<String> list = Arrays.asList(arr);
        assertTrue(list.contains(s1));
        assertTrue(list.contains(s2));
    }

    @Test
    void testAsSetInterface() {
        // Verify it can be used as Set<T>
        Set<Object> set = new IdentitySet<>();
        Object obj = new Object();

        set.add(obj);
        assertTrue(set.contains(obj));
        assertEquals(1, set.size());
    }

    @Test
    void testWithClassType() {
        // Common use case: tracking visited classes
        Set<Class<?>> visited = new IdentitySet<>();

        visited.add(String.class);
        visited.add(Integer.class);
        visited.add(Object.class);

        assertTrue(visited.contains(String.class));
        assertTrue(visited.contains(Integer.class));
        assertFalse(visited.contains(Long.class));
        assertEquals(3, visited.size());
    }
}
