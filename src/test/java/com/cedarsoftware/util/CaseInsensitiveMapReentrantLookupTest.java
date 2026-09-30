package com.cedarsoftware.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A String lookup (get, getOrDefault, containsKey, remove) reused one key object per thread, refilled on each call.
 * A backing map that looked something up in another CaseInsensitiveMap while serving a lookup -- a cache, a logging
 * or tracking map -- refilled it midway, and the outer lookup then compared against the other map's key: get()
 * answered null for a key that was there. Each lookup now has its own key.
 */
class CaseInsensitiveMapReentrantLookupTest {

    /** A backing map whose lookups consult another CaseInsensitiveMap first, on the same thread. */
    private static Map<String, Integer> consulting(CaseInsensitiveMap<String, Integer> other) {
        return new LinkedHashMap<String, Integer>() {
            @Override
            public Integer get(Object key) {
                other.get("SomethingElse");
                return super.get(key);
            }

            @Override
            public boolean containsKey(Object key) {
                other.containsKey("SomethingElse");
                return super.containsKey(key);
            }

            @Override
            public Integer getOrDefault(Object key, Integer defaultValue) {
                other.get("SomethingElse");
                return super.getOrDefault(key, defaultValue);
            }

            @Override
            public Integer remove(Object key) {
                other.get("SomethingElse");
                return super.remove(key);
            }
        };
    }

    @Test
    void aLookupSurvivesTheBackingMapLookingUpAnotherMap() {
        CaseInsensitiveMap<String, Integer> other = new CaseInsensitiveMap<>();
        other.put("somethingelse", 9);
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>(Collections.emptyMap(), consulting(other));
        map.put("Alpha", 1);
        map.put("Beta", 2);

        assertEquals(1, map.get("ALPHA"));
        assertEquals(2, map.get("beta"));
        assertTrue(map.containsKey("aLpHa"));
        assertEquals(2, map.getOrDefault("BETA", -1));
        assertEquals(1, map.remove("alpha"));
        assertNull(map.get("Alpha"));
        assertEquals(1, map.size());
        assertEquals(9, other.get("SOMETHINGELSE"));
    }
}
