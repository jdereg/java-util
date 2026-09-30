package com.cedarsoftware.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * equals() between two CaseInsensitiveMaps looks each entry up in the other's backing map by its stored key, with no
 * copy; with any other map it normalizes that map's keys first, which is what catches a map holding both "a" and "A".
 * An old-vs-new differential over 600,000 random pairs (five backings, case variants, null keys and values, Integer
 * keys) found no answer changed; these pin the cases it turned up.
 */
class CaseInsensitiveMapEqualsTest {

    @SuppressWarnings("unchecked")
    private static final Supplier<Map<Object, Object>>[] BACKINGS = new Supplier[]{
            CaseInsensitiveMap::new,
            () -> new CaseInsensitiveMap<>(Collections.emptyMap(), new TreeMap<>()),
            CaseInsensitiveMap::concurrent,
            CaseInsensitiveMap::concurrentSorted,
            () -> new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>())};

    @Test
    void mapsThatDifferOnlyInTheCaseOfTheirKeysAreEqualWhateverTheirBacking() {
        for (Supplier<Map<Object, Object>> a : BACKINGS) {
            for (Supplier<Map<Object, Object>> b : BACKINGS) {
                Map<Object, Object> x = a.get();
                Map<Object, Object> y = b.get();
                x.put("Alpha", 1);
                x.put("beta", null);
                x.put("Straße", "s");
                y.put("ALPHA", 1);
                y.put("BETA", null);
                y.put("straße", "s");
                assertTrue(x.equals(y) && y.equals(x), x + " vs " + y);
                assertEquals(x.hashCode(), y.hashCode());

                y.put("alpha", 2);
                assertFalse(x.equals(y) || y.equals(x), "a value differs");
                y.put("alpha", 1);
                y.remove("beta");
                y.put("gamma", null);
                assertFalse(x.equals(y) || y.equals(x), "a null value is not an absent key");
            }
        }
    }

    /** A sorted backing map can hold keys of mixed types, which the other map cannot look up: compared as any map. */
    @Test
    void aSortedMapOfMixedKeyTypesIsComparedAsAnyMap() {
        Map<Object, Object> x = new CaseInsensitiveMap<>(Collections.emptyMap(), new TreeMap<>());
        Map<Object, Object> y = new CaseInsensitiveMap<>(Collections.emptyMap(), new TreeMap<>());
        // An Integer first: later String keys compare themselves to it. With three of them the tree rotates a String
        // key to its root, and looking the Integer up there then compares it to that String: ClassCastException.
        x.put(1, "one");
        x.put("Alpha", 2);
        x.put("delta", 3);
        x.put("Echo", 4);
        y.put(1, "one");
        y.put("ALPHA", 2);
        y.put("DELTA", 3);
        y.put("echo", 4);
        assertTrue(x.equals(y));
        assertTrue(y.equals(x));
    }

    @Test
    void anotherKindOfMapWithKeysDifferingOnlyInCaseIsNotEqual() {
        Map<String, Integer> both = new HashMap<>();
        both.put("a", 1);
        both.put("A", 1);
        Map<String, Integer> ci = new CaseInsensitiveMap<>();
        ci.put("a", 1);
        ci.put("b", 1);
        assertFalse(ci.equals(both));

        Map<String, Integer> same = new HashMap<>();
        same.put("A", 1);
        same.put("B", 1);
        assertTrue(ci.equals(same));
    }
}
