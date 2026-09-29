package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * In flatten mode a Set's Lists and arrays dissolve into its berries, which match in any order: {[0, 2], [1]},
 * {[2, 0], [1]} and {0, 1, 2} are one Set portion. Matching already compared the berries one by one, but the hash
 * combined each inner List as one element, so arrangements of the same berries hashed apart and missed each other,
 * and the key keySet() reported ({0, 2, 1}) did not find its own entry.
 */
class MultiKeyMapFlattenSetTest {

    private static Set<Object> set(Object... o) {
        return new LinkedHashSet<>(Arrays.asList(o));
    }

    private static List<Object> list(Object... o) {
        return Arrays.asList(o);
    }

    @Test
    void aSetPortionIsItsBerriesInAnyOrder() {
        for (MultiKeyMap.CollectionKeyMode mode : MultiKeyMap.CollectionKeyMode.values()) {
            MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(true).collectionKeyMode(mode)
                    .build();
            map.put(new Object[]{"p", set(list(0, 2), list(1))}, "v");
            assertEquals("v", map.get(new Object[]{"p", set(list(1), list(0, 2))}), mode.name());
            assertEquals("v", map.get(new Object[]{"p", set(list(2, 0), list(1))}), mode.name());
            assertEquals("v", map.get(new Object[]{"p", set(0, 1, 2)}), mode.name());
            assertEquals("v", map.get(new Object[]{"p", set(new int[]{0}, list(1L, 2.0))}), mode.name());
            assertNull(map.get(new Object[]{"p", set(0, 1)}), mode.name());
            assertNull(map.get(new Object[]{"p", set(list(0, 1), list(2, 2))}), mode.name());
            Object reported = map.keySet().iterator().next();
            assertEquals("v", map.get(reported), mode.name());
        }
    }

    /** Repeated berries count: {[3, 0], [0, 3]} is 3, 0, 0, 3 -- not {3, 0}. */
    @Test
    void repeatedBerriesCount() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder().flattenDimensions(true).build();
        map.put(set(list(3, 0), list(0, 3)), "v");
        assertEquals("v", map.get(set(list(0, 0), list(3, 3))));
        assertEquals("v", map.get(set(list(3, 3, 0, 0))));
        assertNull(map.get(set(3, 0)));
        Object reported = map.keySet().iterator().next();
        assertEquals("v", map.get(reported));
        MultiKeyMap<String> copy = MultiKeyMap.<String>builder().flattenDimensions(true).build();
        copy.putAll(map);
        assertEquals(map, copy);
        assertEquals(map.hashCode(), copy.hashCode());
    }
}
