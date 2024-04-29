package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
class SealableListTest {
    
    private SealableList<Integer> list;
    private volatile boolean sealedState = false;
    private Supplier<Boolean> sealedSupplier = () -> sealedState;
    
    @BeforeEach
    void setUp() {
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @Test
    void testAdd() {
        assertFalse(list.isEmpty());
        assertEquals(3, list.size());
        list.add(40);
        assertTrue(list.contains(40));
        assertEquals(4, list.size());
    }

    @Test
    void testRemove() {
        assertTrue(list.remove(Integer.valueOf(20)));
        assertFalse(list.contains(20));
        assertEquals(2, list.size());
    }

    @Test
    void testAddWhenSealed() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> list.add(50));
    }

    @Test
    void testRemoveWhenSealed() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> list.remove(Integer.valueOf(10)));
    }

    @Test
    void testIteratorWhenSealed() {
        Iterator<Integer> it = list.iterator();
        sealedState = true;
        assertTrue(it.hasNext());
        assertEquals(10, it.next());
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void testListIteratorSetWhenSealed() {
        ListIterator<Integer> it = list.listIterator();
        sealedState = true;
        it.next();
        assertThrows(UnsupportedOperationException.class, () -> it.set(100));
    }

    @Test
    void testSubList() {
        List<Integer> sublist = list.subList(0, 2);
        assertEquals(2, sublist.size());
        assertTrue(sublist.contains(10));
        assertTrue(sublist.contains(20));
        assertFalse(sublist.contains(30));
        sublist.add(25);
        assertTrue(sublist.contains(25));
        assertEquals(3, sublist.size());
    }

    @Test
    void testSubListWhenSealed() {
        List<Integer> sublist = list.subList(0, 2);
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> sublist.add(35));
        assertThrows(UnsupportedOperationException.class, () -> sublist.remove(Integer.valueOf(10)));
    }

    @Test
    void testClearWhenSealed() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, list::clear);
    }

    @Test
    void testSetWhenSealed() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> list.set(1, 100));
    }

    @Test
    void testListIteratorAddWhenSealed() {
        ListIterator<Integer> it = list.listIterator();
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> it.add(45));
    }

    @Test
    void testAddAllWhenSealed() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> list.addAll(Arrays.asList(50, 60)));
    }

    @Test
    void testIteratorTraversal() {
        Iterator<Integer> it = list.iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(10), it.next());
        assertEquals(Integer.valueOf(20), it.next());
        assertEquals(Integer.valueOf(30), it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void testListIteratorPrevious() {
        ListIterator<Integer> it = list.listIterator(2);
        assertEquals(Integer.valueOf(20), it.previous());
        assertTrue(it.hasPrevious());

        Iterator it2 = list.listIterator(0);
        assertEquals(Integer.valueOf(10), it2.next());
        assertEquals(Integer.valueOf(20), it2.next());
        assertEquals(Integer.valueOf(30), it2.next());
        assertThrows(NoSuchElementException.class, () -> it2.next());
    }

    @Test
    void testEquals() {
        SealableList<Integer> other = new SealableList<>(sealedSupplier);
        other.add(10);
        other.add(20);
        other.add(30);
        assertEquals(list, other);
        other.add(40);
        assertNotEquals(list, other);
    }

    @Test
    void testHashCode() {
        SealableList<Integer> other = new SealableList<>(sealedSupplier);
        other.add(10);
        other.add(20);
        other.add(30);
        assertEquals(list.hashCode(), other.hashCode());
        other.add(40);
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    void testNestingHonorsOuterSeal()
    {
        List<Integer> l2 = list.subList(0, list.size());
        List<Integer> l3 = l2.subList(0, l2.size());
        List<Integer> l4 = l3.subList(0, l3.size());
        List<Integer> l5 = l4.subList(0, l4.size());
        l5.add(40);
        assertEquals(list.size(), 4);
        assertEquals(list.get(3), 40);
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> l5.add(50));
        sealedState = false;
        l5.add(50);
        assertEquals(list.size(), 5);
    }
}
