package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CaseInsensitiveMap.keySet().removeAll() was AbstractSet's: when the collection was at least as large as the key
 * set it asked the collection's own contains() -- case-sensitive for a List or HashSet -- and otherwise removed each
 * element through remove(), which is case-insensitive. So whether "name" removed "Name" depended on the sizes. It now
 * always matches as remove() and retainAll() do, whatever the sizes and whatever the collection.
 */
class CaseInsensitiveMapKeySetRemoveAllTest {

    private static CaseInsensitiveMap<String, Integer> map(String... keys) {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        for (int i = 0; i < keys.length; i++) {
            map.put(keys[i], i);
        }
        return map;
    }

    @Test
    void anElementInAnotherCaseRemovesTheKeyWhateverTheSizes() {
        // collection smaller than, as large as, and larger than the key set; a List and a HashSet
        for (Collection<String> c : Arrays.<Collection<String>>asList(Collections.singletonList("NAME"),
                Arrays.asList("NAME", "x"), Arrays.asList("NAME", "x", "y", "z"),
                new HashSet<>(Arrays.asList("NAME", "x", "y", "z")))) {
            for (CaseInsensitiveMap<String, Integer> map : Arrays.asList(map("Name"), map("Name", "Age"))) {
                int size = map.size();
                String what = map.keySet() + " removeAll " + c;
                assertTrue(map.keySet().removeAll(c), what);
                assertFalse(map.containsKey("name"), what);
                assertEquals(size - 1, map.size(), what);
            }
        }
    }

    @Test
    void elementsThatFindNothingChangeNothing() {
        CaseInsensitiveMap<String, Integer> map = map("Name", "Age");
        assertFalse(map.keySet().removeAll(Arrays.asList("x", "y", "z")));
        assertFalse(map.keySet().removeAll(Collections.emptyList()));
        assertEquals(2, map.size());
        assertThrows(NullPointerException.class, () -> map.keySet().removeAll(null));
    }

    @Test
    void aConcurrentBackingMatchesTheSameWay() {
        Map<String, Integer> map = new CaseInsensitiveMap<>(Collections.emptyMap(), new ConcurrentHashMap<>());
        map.put("Name", 1);
        assertTrue(map.keySet().removeAll(Arrays.asList("NAME", "other")));
        assertTrue(map.isEmpty());
    }
}
