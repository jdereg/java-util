package com.cedarsoftware.util;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
class SealableNavigableSetTest {

    private SealableNavigableSet<Integer> set;
    private volatile boolean sealedState = false;
    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setUp() {
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(null);
        set.add(30);
        set.add(10);
        set.add(20);
    }

    @Test
    void testIteratorModificationException() {
        Iterator<Integer> iterator = set.iterator();
        sealedState = true;
        assertDoesNotThrow(iterator::next);
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @Test
    void testDescendingIteratorModificationException() {
        Iterator<Integer> iterator = set.descendingIterator();
        sealedState = true;
        assertDoesNotThrow(iterator::next);
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @Test
    void testTailSetModificationException() {
        NavigableSet<Integer> tailSet = set.tailSet(20, true);
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> tailSet.add(40));
        assertThrows(UnsupportedOperationException.class, tailSet::clear);
    }

    @Test
    void testHeadSetModificationException() {
        NavigableSet<Integer> headSet = set.headSet(20, false);
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> headSet.add(5));
        assertThrows(UnsupportedOperationException.class, headSet::clear);
    }

    @Test
    void testSubSetModificationException() {
        NavigableSet<Integer> subSet = set.subSet(10, true, 30, true);
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> subSet.add(25));
        assertThrows(UnsupportedOperationException.class, subSet::clear);
    }

    @Test
    void testDescendingSetModificationException() {
        NavigableSet<Integer> descendingSet = set.descendingSet();
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.add(5));
        assertThrows(UnsupportedOperationException.class, descendingSet::clear);
    }

    @Test
    void testSealAfterModification() {
        Iterator<Integer> iterator = set.iterator();
        NavigableSet<Integer> tailSet = set.tailSet(20, true);
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, iterator::remove);
        assertThrows(UnsupportedOperationException.class, () -> tailSet.add(40));
    }

    @Test
    void testSubset()
    {
        Set<Integer> subset = set.subSet(5, true, 25, true);
        assertEquals(subset.size(), 2);
        subset.add(5);
        assertEquals(subset.size(), 3);
        subset.add(25);
        assertEquals(subset.size(), 4);
        assertThrows(IllegalArgumentException.class, () -> subset.add(26));
        assertEquals(set.size(), 6);
    }

    @Test
    void testSubset2()
    {
        Set<Integer> subset = set.subSet(5, 25);
        assertEquals(subset.size(), 2);
        assertThrows(IllegalArgumentException.class, () -> subset.add(4));
        subset.add(5);
        assertEquals(subset.size(), 3);
        assertThrows(IllegalArgumentException.class, () -> subset.add(25));
        assertEquals(5, set.size());
    }
}
