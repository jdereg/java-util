package com.cedarsoftware.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
