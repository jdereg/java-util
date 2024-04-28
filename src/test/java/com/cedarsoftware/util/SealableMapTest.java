package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.MapUtilities.mapOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
class SealableMapTest {

    private SealableMap<String, Integer> map;
    private volatile boolean sealedState = false;
    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setUp() {
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
    }

    @Test
    void testPutWhenUnsealed() {
        assertEquals(1, map.get("one"));
        map.put("four", 4);
        assertEquals(4, map.get("four"));
    }

    @Test
    void testRemoveWhenUnsealed() {
        assertEquals(1, map.get("one"));
        map.remove("one");
        assertNull(map.get("one"));
    }

    @Test
    void testPutWhenSealed() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> map.put("five", 5));
    }

    @Test
    void testRemoveWhenSealed() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> map.remove("one"));
    }

    @Test
    void testModifyEntrySetWhenSealed() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> entries.removeIf(e -> e.getKey().equals("one")));
        assertThrows(UnsupportedOperationException.class, () -> entries.iterator().remove());
    }

    @Test
    void testModifyKeySetWhenSealed() {
        Set<String> keys = map.keySet();
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> keys.remove("two"));
        assertThrows(UnsupportedOperationException.class, keys::clear);
    }

    @Test
    void testModifyValuesWhenSealed() {
        Collection<Integer> values = map.values();
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> values.remove(3));
        assertThrows(UnsupportedOperationException.class, values::clear);
    }

    @Test
    void testClearWhenSealed() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, map::clear);
    }

    @Test
    void testPutAllWhenSealed() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> map.putAll(mapOf("ten", 10)));
    }

    @Test
    void testSealAndUnseal() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> map.put("six", 6));
        sealedState = false;
        map.put("six", 6);
        assertEquals(6, map.get("six"));
    }

    @Test
    void testEntrySetFunctionality() {
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertNotNull(entries);
        assertTrue(entries.stream().anyMatch(e -> e.getKey().equals("one") && e.getValue().equals(1)));

        sealedState = true;
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleImmutableEntry<>("five", 5);
        assertThrows(UnsupportedOperationException.class, () -> entries.add(entry));
    }

    @Test
    void testKeySetFunctionality() {
        Set<String> keys = map.keySet();
        assertNotNull(keys);
        assertTrue(keys.contains("two"));

        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> keys.add("five"));
    }

    @Test
    void testValuesFunctionality() {
        Collection<Integer> values = map.values();
        assertNotNull(values);
        assertTrue(values.contains(3));

        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> values.add(5));
    }

    @Test
    void testMapEquality() {
        SealableMap<String, Integer> anotherMap = new SealableMap<>(sealedSupplier);
        anotherMap.put("one", 1);
        anotherMap.put("two", 2);
        anotherMap.put("three", 3);

        assertEquals(map, anotherMap);
    }
}
