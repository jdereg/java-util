package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * CompactLinkedMap and CompactCILinkedMap keep their entries in insertion order, but do not override getOrdering(),
 * so getConfig() reported them "unordered" -- and so did the config json-io writes for them, and the copies
 * withConfig() makes. A legacy subclass backed by a TreeMap, which keeps its entries sorted, was "unordered" too.
 * getConfig() and withConfig() now report the order such a map keeps. The plain legacy CompactMap is still unordered.
 */
class CompactLinkedMapOrderingTest {

    private static List<String> scrambledKeys() {
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            keys.add("k" + (i * 37 % 60));          // past the compact size of 50, in no sorted order
        }
        return keys;
    }

    @Test
    void theLinkedMapsReportInsertionOrder() {
        for (CompactMap<String, Object> map : Arrays.<CompactMap<String, Object>>asList(new CompactLinkedMap<>(),
                new CompactCILinkedMap<>())) {
            assertEquals(CompactMap.INSERTION, map.getConfig().get(CompactMap.ORDERING), map.getClass().getName());
            assertFalse(map.isDefaultCompactMap());
        }
        assertEquals(CompactMap.UNORDERED, new CompactMap<String, Object>().getConfig().get(CompactMap.ORDERING));
    }

    @Test
    void aCopyMadeFromTheirConfigurationIsInsertionOrderedToo() {
        for (CompactMap<String, Object> map : Arrays.<CompactMap<String, Object>>asList(new CompactLinkedMap<>(),
                new CompactCILinkedMap<>())) {
            List<String> keys = scrambledKeys();
            for (String key : keys) {
                map.put(key, key.length());
            }
            CompactMap<String, Object> copy = map.withConfig(Collections.<String, Object>emptyMap());
            Map<String, Object> config = copy.getConfig();
            assertEquals(CompactMap.INSERTION, config.get(CompactMap.ORDERING), map.getClass().getName());
            assertEquals(keys, new ArrayList<>(copy.keySet()), map.getClass().getName());
        }
    }

    @Test
    void aLegacySubclassBackedByASortedMapReportsItsOrder() {
        CompactMap<String, Object> sorted = new CompactMap<String, Object>() {
            @Override
            protected Map<String, Object> getNewMap() {
                return new TreeMap<>();
            }
        };
        CompactMap<String, Object> reverse = new CompactMap<String, Object>() {
            @Override
            protected Map<String, Object> getNewMap() {
                return new TreeMap<>(Collections.reverseOrder());
            }
        };
        assertEquals(CompactMap.SORTED, sorted.getConfig().get(CompactMap.ORDERING));
        assertEquals(CompactMap.REVERSE, reverse.getConfig().get(CompactMap.ORDERING));
        for (String key : Arrays.asList("b", "c", "a")) {
            reverse.put(key, key);
        }
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(reverse.withConfig(
                Collections.<String, Object>emptyMap()).keySet()));
    }
}
