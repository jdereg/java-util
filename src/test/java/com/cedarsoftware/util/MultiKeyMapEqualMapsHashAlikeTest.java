package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two MultiKeyMaps that are equal hash alike: hashCode() hashes each key by the map's own rules, the rules equals()
 * compares by. In the case-sensitive mode (the default) it hashed the reported keys with their own hashCode(), so
 * maps holding 1 and 1.0, or (1, 2) and (1L, 2.0), were equal both ways but hashed apart, and a HashSet of maps did
 * not find an equal one. The case-insensitive mode already hashed by the map's rules. Like CaseInsensitiveMap, a
 * MultiKeyMap can be equal to a plain Map whose keys hash by their own hashCode() while the two hash differently.
 */
class MultiKeyMapEqualMapsHashAlikeTest {

    private static Set<Object> set(Object... o) {
        return new LinkedHashSet<>(Arrays.asList(o));
    }

    private static void assertEqualAndHashAlike(Consumer<MultiKeyMap<String>> fill1,
                                                Consumer<MultiKeyMap<String>> fill2, String what) {
        for (boolean caseSensitive : new boolean[]{true, false}) {
            MultiKeyMap<String> m1 = MultiKeyMap.<String>builder().caseSensitive(caseSensitive).build();
            MultiKeyMap<String> m2 = MultiKeyMap.<String>builder().caseSensitive(caseSensitive).build();
            fill1.accept(m1);
            fill2.accept(m2);
            String mode = what + (caseSensitive ? " (case-sensitive)" : " (case-insensitive)");
            assertEquals(m1, m2, mode);
            assertEquals(m2, m1, mode);
            assertEquals(m1.hashCode(), m2.hashCode(), mode + ": equal maps hash alike");
            assertTrue(new HashSet<>(Collections.singletonList(m1)).contains(m2), mode);
        }
    }

    @Test
    void mapsEqualByValueBasedEqualityHashAlike() {
        assertEqualAndHashAlike(m -> m.put(1, "v"), m -> m.put(1.0, "v"), "1 and 1.0");
        assertEqualAndHashAlike(m -> m.put(1, "v"), m -> m.put(new BigDecimal("1.00"), "v"), "1 and 1.00");
        assertEqualAndHashAlike(m -> m.putMultiKey("v", 1, 2), m -> m.putMultiKey("v", 1L, 2.0),
                "(1, 2) and (1L, 2.0)");
        assertEqualAndHashAlike(m -> m.put(set(1, 2), "v"), m -> m.put(set(2.0, 1.0), "v"), "{1, 2} and {2.0, 1.0}");
        assertEqualAndHashAlike(m -> m.put(new Object[]{"x", Arrays.asList(1, 2)}, "v"),
                m -> m.put(Arrays.asList("x", new double[]{1, 2}), "v"), "[x, [1, 2]] and [x, [1.0, 2.0]]");
    }

    @Test
    void mapsEqualIgnoringCaseHashAlike() {
        MultiKeyMap<String> m1 = MultiKeyMap.<String>builder().caseSensitive(false).build();
        MultiKeyMap<String> m2 = MultiKeyMap.<String>builder().caseSensitive(false).build();
        m1.putMultiKey("v", "Alpha", 1);
        m2.putMultiKey("v", "ALPHA", 1.0);
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void aCopyHashesAsTheMapInEveryMode() {
        for (MultiKeyMap.CollectionKeyMode mode : MultiKeyMap.CollectionKeyMode.values()) {
            for (boolean flatten : new boolean[]{false, true}) {
                MultiKeyMap<String> map = MultiKeyMap.<String>builder().collectionKeyMode(mode)
                        .flattenDimensions(flatten).build();
                map.put("a", "1");
                map.put(new Object[]{"b", 2}, "2");
                map.put(Arrays.asList("c", set(3, 4)), "3");
                map.put(new int[]{5, 6}, "4");
                map.put(null, "5");
                MultiKeyMap<String> copy = MultiKeyMap.<String>builder().collectionKeyMode(mode)
                        .flattenDimensions(flatten).build();
                copy.putAll(map);
                assertEquals(map, copy, mode + " flatten=" + flatten);
                assertEquals(map.hashCode(), copy.hashCode(), mode + " flatten=" + flatten);
            }
        }
    }

    /** Equal to a HashMap both ways, as before; the hash is the map's own, as CaseInsensitiveMap's is. */
    @Test
    void aPlainMapWithTheSameEntriesIsEqual() {
        MultiKeyMap<String> mkm = new MultiKeyMap<>();
        Map<Object, String> hashMap = new HashMap<>();
        mkm.put(1, "v");
        hashMap.put(1.0, "v");
        assertEquals(mkm, hashMap);
        assertEquals(hashMap, mkm);
    }
}
