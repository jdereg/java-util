package com.cedarsoftware.util;

import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * retainAll() on a CompactMap's keySet() and entrySet() looks the given elements up in a map that matches keys as the
 * CompactMap does. An element that map could not hold -- a null for a ConcurrentHashMap, a key a sorted map cannot
 * compare -- threw, where the JDK's maps skip it: nothing in such a map can match it. And the entry set's helper was
 * one anonymous CompactMap class configured from whichever map called it, so the null policy worked out for it once
 * came from the first map and applied to every later one: after a ConcurrentHashMap-backed map met a null there, a
 * HashMap-backed map's entrySet().retainAll() threw on a null key too. And the entry set kept one value per key, so a
 * collection pairing a key with two values kept only the last one's entry.
 */
class CompactMapRetainAllTest {

    private static CompactMap<Object, Object> concurrentBacked() {
        CompactMap<Object, Object> map = CompactMap.builder().mapType(ConcurrentHashMap.class).build();
        map.put("x", 1);
        map.put("y", 2);
        return map;
    }

    @Test
    void aNullInTheCollectionMatchesNothingInAMapThatCannotHoldOne() {
        CompactMap<Object, Object> map = concurrentBacked();
        List<Object> entries = Arrays.asList(new SimpleEntry<>("x", 1), new SimpleEntry<>(null, 2),
                new SimpleEntry<>("y", null));
        assertTrue(map.entrySet().retainAll(entries));
        assertEquals(Collections.singletonMap("x", 1), map);

        map = concurrentBacked();
        assertTrue(map.keySet().retainAll(Arrays.asList("y", null)));
        assertEquals(Collections.singletonMap("y", 2), map);
    }

    @Test
    void oneMapsNullPolicyNoLongerReachesAnother() {
        concurrentBacked().entrySet().retainAll(Collections.singletonList(new SimpleEntry<>(null, 1)));

        CompactMap<Object, Object> plain = CompactMap.builder().build();
        plain.put(null, "n");
        plain.put("k", "v");
        assertTrue(plain.entrySet().retainAll(Collections.singletonList(new SimpleEntry<>(null, "n"))));
        assertEquals(Collections.singletonMap(null, "n"), plain);
    }

    @Test
    void aKeyASortedMapCannotCompareMatchesNothing() {
        CompactMap<Object, Object> sorted = new CompactMap<Object, Object>() {
            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<Object, Object> getNewMap() {
                return new TreeMap<>();
            }
        };
        sorted.put("a", 1);
        sorted.put("b", 2);
        sorted.put("c", 3);                                     // in its TreeMap, which cannot compare 7 to "a"
        assertTrue(sorted.keySet().retainAll(Arrays.asList(7, "a")));
        assertEquals(Collections.singletonMap("a", 1), sorted);
        assertTrue(sorted.entrySet().retainAll(Collections.singletonList(new SimpleEntry<>(7, 1))));
        assertTrue(sorted.isEmpty());
    }

    @Test
    void anEntryIsKeptWhenTheCollectionPairsItsKeyWithSeveralValues() {
        CompactMap<Object, Object> map = CompactMap.builder().build();
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(map.entrySet().retainAll(Arrays.asList(new SimpleEntry<>("a", 1), new SimpleEntry<>("a", 9))));
        assertEquals(Collections.singletonMap("a", 1), map);
    }

    @Test
    void aCaseInsensitiveMapStillMatchesAcrossCase() {
        CompactMap<Object, Object> map = CompactMap.builder().caseSensitive(false).build();
        map.put("Name", 1);
        map.put("Age", 2);
        assertTrue(map.keySet().retainAll(Collections.singletonList("NAME")));
        assertEquals(1, map.get("name"));
        map.put("Age", 2);
        assertTrue(map.entrySet().retainAll(Collections.singletonList(new SimpleEntry<>("AGE", 2))));
        assertEquals(1, map.size());
        assertEquals(2, map.get("age"));
    }
}
