package com.cedarsoftware.util;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class SealableNavigableMapSubsetTest {
    private SealableNavigableMap<Integer, String> unmodifiableMap;
    private volatile boolean sealedState = false;
    private final Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setUp() {
        NavigableMap<Integer, String> testMap = new TreeMap<>();
        for (int i = 10; i <= 100; i += 10) {
            testMap.put(i, String.valueOf(i));
        }
        unmodifiableMap = new SealableNavigableMap<>(testMap, sealedSupplier);
    }

    @Test
    void testSubMap() {
        NavigableMap<Integer, String> subMap = unmodifiableMap.subMap(30, true, 70, true);
        assertEquals(5, subMap.size(), "SubMap size should initially include keys 30, 40, 50, 60, 70");

        assertThrows(IllegalArgumentException.class, () -> subMap.put(25, "25"), "Adding key 25 should fail as it is outside the bounds");
        assertNull(subMap.put(35, "35"), "Adding key 35 should succeed");
        assertEquals(6, subMap.size(), "SubMap size should now be 6");
        assertEquals(11, unmodifiableMap.size(), "Enclosing map should reflect the addition");

        assertNull(subMap.remove(10), "Removing key 10 should fail as it is outside the bounds");
        assertEquals("40", subMap.remove(40), "Removing key 40 should succeed");
        assertEquals(5, subMap.size(), "SubMap size should be back to 5 after removal");
        assertEquals(10, unmodifiableMap.size(), "Enclosing map should reflect the removal");

        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> subMap.put(60, "60"), "Modification should fail when sealed");
    }

    @Test
    void testHeadMap() {
        NavigableMap<Integer, String> headMap = unmodifiableMap.headMap(50, true);
        assertEquals(5, headMap.size(), "HeadMap should include keys up to and including 50");

        assertThrows(IllegalArgumentException.class, () -> headMap.put(55, "55"), "Adding key 55 should fail as it is outside the bounds");
        assertNull(headMap.put(5, "5"), "Adding key 5 should succeed");
        assertEquals(6, headMap.size(), "HeadMap size should now be 6");
        assertEquals(11, unmodifiableMap.size(), "Enclosing map should reflect the addition");

        assertNull(headMap.remove(60), "Removing key 60 should fail as it is outside the bounds");
        assertEquals("20", headMap.remove(20), "Removing key 20 should succeed");
        assertEquals(5, headMap.size(), "HeadMap size should be back to 5 after removal");
        assertEquals(10, unmodifiableMap.size(), "Enclosing map should reflect the removal");

        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> headMap.put(10, "10"), "Modification should fail when sealed");
    }

    @Test
    void testTailMap() {
        NavigableMap<Integer, String> tailMap = unmodifiableMap.tailMap(50, true);
        assertEquals(6, tailMap.size(), "TailMap should include keys from 50 to 100");

        assertThrows(IllegalArgumentException.class, () -> tailMap.put(45, "45"), "Adding key 45 should fail as it is outside the bounds");
        assertNull(tailMap.put(110, "110"), "Adding key 110 should succeed");
        assertEquals(7, tailMap.size(), "TailMap size should now be 7");
        assertEquals(11, unmodifiableMap.size(), "Enclosing map should reflect the addition");

        assertNull(tailMap.remove(40), "Removing key 40 should fail as it is outside the bounds");
        assertEquals("60", tailMap.remove(60), "Removing key 60 should succeed");
        assertEquals(6, tailMap.size(), "TailMap size should be back to 6 after removal");
        assertEquals(10, unmodifiableMap.size(), "Enclosing map should reflect the removal");

        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> tailMap.put(80, "80"), "Modification should fail when sealed");
    }
}