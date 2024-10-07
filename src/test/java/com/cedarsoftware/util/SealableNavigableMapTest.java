package com.cedarsoftware.util;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
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
class SealableNavigableMapTest {

    private NavigableMap<String, Integer> map;
    private SealableNavigableMap<String, Integer> unmodifiableMap;
    private Supplier<Boolean> sealedSupplier;
    private boolean sealed;

    @BeforeEach
    void setUp() {
        sealed = false;
        sealedSupplier = () -> sealed;

        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("three", 3);
        map.put(null, null);
        map.put("one", 1);
        map.put("two", 2);

        unmodifiableMap = new SealableNavigableMap<>(map, sealedSupplier);
    }

    @Test
    void testMutationsWhenUnsealed() {
        assertFalse(sealedSupplier.get(), "Map should start unsealed.");
        assertEquals(4, unmodifiableMap.size());
        unmodifiableMap.put("four", 4);
        assertEquals(Integer.valueOf(4), unmodifiableMap.get("four"));
        assertTrue(unmodifiableMap.containsKey("four"));
    }

    @Test
    void testSealedMutationsThrowException() {
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.put("five", 5));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.remove("one"));
        assertThrows(UnsupportedOperationException.class, unmodifiableMap::clear);
    }

    @Test
    void testEntrySetValueWhenSealed() {
        Map.Entry<String, Integer> entry = unmodifiableMap.firstEntry();
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(10));
    }
    
    @Test
    void testKeySetViewReflectsChanges() {
        unmodifiableMap.put("five", 5);
        assertTrue(unmodifiableMap.keySet().contains("five"));
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.keySet().remove("five"));
    }

    @Test
    void testValuesViewReflectsChanges() {
        unmodifiableMap.put("six", 6);
        assertTrue(unmodifiableMap.values().contains(6));
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.values().remove(6));
    }
    
    @Test
    void testSubMapViewReflectsChanges2() {
        // SubMap from "one" to "three", only includes "one" and "three"
        NavigableMap<String, Integer> subMap = unmodifiableMap.subMap("one", true, "three", true);
        assertEquals(2, subMap.size()); // Should only include "one" and "three"
        assertTrue(subMap.containsKey("one") && subMap.containsKey("three"));
        assertFalse(subMap.containsKey("two")); // "two" should not be included

        // Adding a key that's lexicographically after "three"
        unmodifiableMap.put("two-and-half", 2);
        assertFalse(subMap.containsKey("two-and-half")); // Should not be visible in the submap
        assertEquals(2, subMap.size()); // Size should remain as "two-and-half" is out of range
        unmodifiableMap.put("pop", 93);
        assertTrue(subMap.containsKey("pop"));

        subMap.put("poop", 37);
        assertTrue(unmodifiableMap.containsKey("poop"));
        
        // Sealing and testing immutability
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> subMap.put("zero", 0));         // Immutable (and outside range)
        sealed = false;
        assertThrows(java.lang.IllegalArgumentException.class, () -> subMap.put("zero", 0));    // outside range
    }

    @Test
    void testIteratorsThrowWhenSealed() {
        Iterator<String> keyIterator = unmodifiableMap.navigableKeySet().iterator();
        Iterator<Map.Entry<String, Integer>> entryIterator = unmodifiableMap.entrySet().iterator();

        while (keyIterator.hasNext()) {
            keyIterator.next();
            sealed = true;
            assertThrows(UnsupportedOperationException.class, keyIterator::remove);
            sealed = false;
        }

        while (entryIterator.hasNext()) {
            Map.Entry<String, Integer> entry = entryIterator.next();
            sealed = true;
            assertThrows(UnsupportedOperationException.class, () -> entry.setValue(999));
            sealed = false;
        }
    }

    @Test
    void testDescendingMapReflectsChanges() {
        unmodifiableMap.put("zero", 0);
        NavigableMap<String, Integer> descendingMap = unmodifiableMap.descendingMap();
        assertTrue(descendingMap.containsKey("zero"));

        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> descendingMap.put("minus one", -1));
    }
}
