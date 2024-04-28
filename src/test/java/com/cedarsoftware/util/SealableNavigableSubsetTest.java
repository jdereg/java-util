package com.cedarsoftware.util;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class SealableNavigableSubsetTest {
    private SealableNavigableSet<Integer> unmodifiableSet;
    private volatile boolean sealedState = false;
    private final Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setUp() {
        NavigableSet<Integer> testSet = new TreeSet<>();
        for (int i = 10; i <= 100; i += 10) {
            testSet.add(i);
        }
        unmodifiableSet = new SealableNavigableSet<>(testSet, sealedSupplier);
    }

    @Test
    void testSubSet() {
        NavigableSet<Integer> subSet = unmodifiableSet.subSet(30, true, 70, true);
        assertEquals(5, subSet.size(), "SubSet size should initially include 30, 40, 50, 60, 70");

        assertThrows(IllegalArgumentException.class, () -> subSet.add(25), "Adding 25 should fail as it is outside the bounds");
        assertTrue(subSet.add(35), "Adding 35 should succeed");
        assertEquals(6, subSet.size(), "SubSet size should now be 6");
        assertEquals(11, unmodifiableSet.size(), "Enclosing set should reflect the addition");

        assertFalse(subSet.remove(10), "Removing 10 should fail as it is outside the bounds");
        assertTrue(subSet.remove(40), "Removing 40 should succeed");
        assertEquals(5, subSet.size(), "SubSet size should be back to 5 after removal");
        assertEquals(10, unmodifiableSet.size(), "Enclosing set should reflect the removal");

        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> subSet.add(60), "Modification should fail when sealed");

    }

    @Test
    void testHeadSet() {
        NavigableSet<Integer> headSet = unmodifiableSet.headSet(50, true);
        assertEquals(5, headSet.size(), "HeadSet should include 10, 20, 30, 40, 50");

        assertThrows(IllegalArgumentException.class, () -> headSet.add(55), "Adding 55 should fail as it is outside the bounds");
        assertTrue(headSet.add(5), "Adding 5 should succeed");
        assertEquals(6, headSet.size(), "HeadSet size should now be 6");
        assertEquals(11, unmodifiableSet.size(), "Enclosing set should reflect the addition");

        assertFalse(headSet.remove(60), "Removing 60 should fail as it is outside the bounds");
        assertTrue(headSet.remove(20), "Removing 20 should succeed");
        assertEquals(5, headSet.size(), "HeadSet size should be back to 5 after removal");
        assertEquals(10, unmodifiableSet.size(), "Enclosing set should reflect the removal");
        
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> headSet.add(10), "Modification should fail when sealed");
    }

    @Test
    void testTailSet() {
        NavigableSet<Integer> tailSet = unmodifiableSet.tailSet(50, true);
        assertEquals(6, tailSet.size(), "TailSet should include 50, 60, 70, 80, 90, 100");

        assertThrows(IllegalArgumentException.class, () -> tailSet.add(45), "Adding 45 should fail as it is outside the bounds");
        assertTrue(tailSet.add(110), "Adding 110 should succeed");
        assertEquals(7, tailSet.size(), "TailSet size should now be 7");
        assertEquals(11, unmodifiableSet.size(), "Enclosing set should reflect the addition");

        assertFalse(tailSet.remove(40), "Removing 40 should fail as it is outside the bounds");
        assertTrue(tailSet.remove(60), "Removing 60 should succeed");
        assertEquals(6, tailSet.size(), "TailSet size should be back to 6 after removal");
        assertEquals(10, unmodifiableSet.size(), "Enclosing set should reflect the removal");

        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> tailSet.add(80), "Modification should fail when sealed");
    }
}
