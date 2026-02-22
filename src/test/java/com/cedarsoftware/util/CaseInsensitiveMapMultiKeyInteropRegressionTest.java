package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseInsensitiveMapMultiKeyInteropRegressionTest {

    @Test
    void testPutIfAbsentWithArrayKeyStoresRetrievableEntry() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>());

        assertNull(map.putIfAbsent(new Object[]{"Dept", "Engineering"}, "v1"));
        assertEquals("v1", map.get(new Object[]{"dept", "engineering"}));
    }

    @Test
    void testComputeIfAbsentWithArrayKeyStoresRetrievableEntry() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>());

        assertEquals("v1", map.computeIfAbsent(new Object[]{"Dept", "Engineering"}, k -> "v1"));
        assertEquals("v1", map.get(new Object[]{"dept", "engineering"}));
    }

    @Test
    void testReplaceWithArrayKeyKeepsEntryRetrievable() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>());
        map.put(new Object[]{"Dept", "Engineering"}, "v1");

        assertTrue(map.replace(new Object[]{"dept", "engineering"}, "v1", "v2"));
        assertEquals("v2", map.get(new Object[]{"DEPT", "ENGINEERING"}));
    }

    @Test
    void testCopyConstructorWithMultiKeyBackingNormalizesArrayKeys() {
        Map<Object, String> source = new LinkedHashMap<>();
        source.put(new Object[]{"Dept", "Engineering"}, "v1");

        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(source, new MultiKeyMap<>());
        assertEquals("v1", map.get(new Object[]{"dept", "engineering"}));
    }

    @Test
    void testEntrySetSetValueUpdatesEntryState() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Key", "v1");

        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        assertEquals("v1", entry.setValue("v2"));
        assertEquals("v2", entry.getValue());
        assertEquals("v2", map.get("key"));
    }

    @Test
    void testSetKeysRemainOrderInsensitiveWithMultiKeyBacking() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>());

        Set<String> key = new LinkedHashSet<>(Arrays.asList("Dept", "Engineering"));
        map.put(key, "v1");

        Set<String> lookup = new LinkedHashSet<>(Arrays.asList("engineering", "DEPT"));
        assertEquals("v1", map.get(lookup));
    }

    @Test
    void testKeySetIteratorRemoveMutatesMultiKeyBackingMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>());
        map.put(new Object[]{"Dept", "Engineering"}, "v1");

        Iterator<Object> iterator = map.keySet().iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        iterator.remove();

        assertTrue(map.isEmpty());
        assertFalse(map.containsKey(new Object[]{"Dept", "Engineering"}));
    }

    @Test
    void testEntrySetIteratorRemoveMutatesMultiKeyBackingMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>());
        map.put(new Object[]{"Dept", "Engineering"}, "v1");

        Iterator<Map.Entry<Object, String>> iterator = map.entrySet().iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        iterator.remove();

        assertTrue(map.isEmpty());
        assertFalse(map.containsKey(new Object[]{"Dept", "Engineering"}));
    }

    @Test
    void testValuesIteratorRemoveMutatesMultiKeyBackingMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>());
        map.put(new Object[]{"Dept", "Engineering"}, "v1");

        Iterator<String> iterator = map.values().iterator();
        assertTrue(iterator.hasNext());
        assertEquals("v1", iterator.next());
        iterator.remove();

        assertTrue(map.isEmpty());
    }

    @Test
    void testKeySetRetainAllMutatesMultiKeyBackingMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>());
        map.put(new Object[]{"Dept", "Engineering"}, "v1");
        map.put(new Object[]{"Dept", "Marketing"}, "v2");

        boolean changed = map.keySet().retainAll(Collections.singleton(Arrays.asList("dept", "engineering")));

        assertTrue(changed);
        assertEquals(1, map.size());
        assertEquals("v1", map.get(new Object[]{"DEPT", "ENGINEERING"}));
    }

    @Test
    void testEntrySetRetainAllMutatesMultiKeyBackingMap() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new MultiKeyMap<>());
        map.put(new Object[]{"Dept", "Engineering"}, "v1");
        map.put(new Object[]{"Dept", "Marketing"}, "v2");

        Map.Entry<Object, String> retainEntry =
                new AbstractMap.SimpleEntry<Object, String>(Arrays.asList("dept", "engineering"), "v1");

        boolean changed = map.entrySet().retainAll(Collections.singleton(retainEntry));

        assertTrue(changed);
        assertEquals(1, map.size());
        assertEquals("v1", map.get(new Object[]{"DEPT", "ENGINEERING"}));
    }
}
