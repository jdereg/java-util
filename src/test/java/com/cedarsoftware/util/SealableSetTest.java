package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.CollectionUtilities.setOf;
import static com.cedarsoftware.util.DeepEquals.deepEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class SealableSetTest {

    private SealableSet<Integer> set;
    private volatile boolean sealed = false;
    private Supplier<Boolean> sealedSupplier = () -> sealed;

    @BeforeEach
    void setUp() {
        set = new SealableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(null);
    }

    @Test
    void testAdd() {
        assertTrue(set.add(30));
        assertTrue(set.contains(30));
    }

    @Test
    void testRemove() {
        assertTrue(set.remove(20));
        assertFalse(set.contains(20));
    }

    @Test
    void testAddWhenSealed() {
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> set.add(40));
    }

    @Test
    void testRemoveWhenSealed() {
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> set.remove(10));
    }

    @Test
    void testIteratorRemoveWhenSealed() {
        Iterator<Integer> iterator = set.iterator();
        sealed = true;
        iterator.next(); // Move to first element
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @Test
    void testClearWhenSealed() {
        sealed = true;
        assertThrows(UnsupportedOperationException.class, set::clear);
    }

    @Test
    void testIterator() {
        // Set items could be in any order
        Iterator<Integer> iterator = set.iterator();
        assertTrue(iterator.hasNext());
        Integer value = iterator.next();
        assert value == null || value == 10 || value == 20;
        value = iterator.next();
        assert value == null || value == 10 || value == 20;
        value = iterator.next();
        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void testRootSealStateHonored() {
        Iterator<Integer> iterator = set.iterator();
        iterator.next();
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> iterator.remove());
        sealed = false;
        iterator.remove();
        assertEquals(set.size(), 2);
        iterator.next();
        iterator.remove();
        assertEquals(set.size(), 1);
        iterator.next();
        iterator.remove();
        assertEquals(set.size(), 0);
    }

    @Test
    void testContainsAll() {
        assertTrue(set.containsAll(Arrays.asList(10, 20)));
        assertFalse(set.containsAll(Arrays.asList(10, 30)));
    }

    @Test
    void testRetainAll() {
        set.retainAll(Arrays.asList(10));
        assertTrue(set.contains(10));
        assertFalse(set.contains(20));
    }

    @Test
    void testRetainAllWhenSealed() {
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> set.retainAll(Arrays.asList(10)));
    }

    @Test
    void testAddAll() {
        set.addAll(Arrays.asList(30, 40));
        assertTrue(set.containsAll(Arrays.asList(30, 40)));
    }

    @Test
    void testAddAllWhenSealed() {
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> set.addAll(Arrays.asList(30, 40)));
    }

    @Test
    void testRemoveAll() {
        set.removeAll(Arrays.asList(10, 20, null));
        assertTrue(set.isEmpty());
    }

    @Test
    void testRemoveAllWhenSealed() {
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> set.removeAll(Arrays.asList(10, 20)));
    }

    @Test
    void testSize() {
        assertEquals(3, set.size());
    }

    @Test
    void testIsEmpty() {
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    void testToArray() {
        assert deepEquals(setOf(10, 20, null), set);
    }

    @Test
    void testNullValueSupport() {
        int size = set.size();
        set.add(null);
        assert size == set.size();
    }

    @Test
    void testToArrayGenerics() {
        Integer[] arr = set.toArray(new Integer[0]);
        boolean found10 = false;
        boolean found20 = false;
        boolean foundNull = false;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == null) {
                foundNull = true;
                continue;
            }
            if (arr[i] == 10) {
                found10 = true;
            }
            if (arr[i] == 20) {
                found20 = true;
            }
        }
        assertTrue(foundNull);
        assertTrue(found10);
        assertTrue(found20);
        assert arr.length == 3;
    }

    @Test
    void testEquals() {
        SealableSet<Integer> other = new SealableSet<>(sealedSupplier);
        other.add(10);
        other.add(20);
        other.add(null);
        assertEquals(set, other);
        other.add(30);
        assertNotEquals(set, other);
    }

    @Test
    void testHashCode() {
        int expectedHashCode = set.hashCode();
        set.add(30);
        assertNotEquals(expectedHashCode, set.hashCode());
    }
}
