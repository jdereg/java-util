package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TrackingMap behavior found by review.
 */
class TrackingMapBehaviorTest {

    /**
     * A null key works wherever the wrapped map takes one. The tracking set of a concurrent map was a
     * ConcurrentHashMap key set, which rejects null: get(null), containsKey(null) and remove(null) threw
     * NullPointerException on ConcurrentHashMapNullSafe, ConcurrentNavigableMapNullSafe and CaseInsensitiveMap,
     * and remove(null) left the entry in the map.
     */
    @Test
    void nullKeysWorkWhereverTheWrappedMapTakesThem() {
        List<Map<String, Integer>> maps = Arrays.asList(new ConcurrentHashMapNullSafe<>(),
                new ConcurrentNavigableMapNullSafe<>(), new CaseInsensitiveMap<>(), new HashMap<>());
        for (Map<String, Integer> map : maps) {
            String name = map.getClass().getSimpleName();
            map.put(null, 7);
            map.put("a", 1);
            TrackingMap<String, Integer> tracking = new TrackingMap<>(map);
            assertEquals(Integer.valueOf(7), tracking.get(null), name);
            assertTrue(tracking.containsKey(null), name);
            assertTrue(tracking.keysUsed().contains(null), name);
            tracking.expungeUnused();
            assertEquals(new HashSet<>(Arrays.asList((String) null)), new HashSet<>(map.keySet()), name);
            assertEquals(Integer.valueOf(7), tracking.remove(null), name);
            assertFalse(map.containsKey(null), name);
        }
    }

    /**
     * A read keeps the entry the wrapped map matched, by the map's own key matching. The tracking set matched by
     * equals(), so expungeUnused() dropped an entry read through a spelling the map matches but equals() does not
     * (a case-insensitive TreeMap read as "APPLE", a BigDecimal key read as 2.00), and kept an IdentityHashMap
     * entry that was never read, for an equal but different key object.
     */
    @Test
    void aReadKeepsTheEntryTheWrappedMapMatched() {
        List<Map<String, Integer>> caseInsensitive = Arrays.asList(new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER),
                new ConcurrentNavigableMapNullSafe<>(String.CASE_INSENSITIVE_ORDER));
        for (Map<String, Integer> map : caseInsensitive) {
            map.put("apple", 1);
            map.put("pear", 2);
            TrackingMap<String, Integer> tracking = new TrackingMap<>(map);
            assertEquals(Integer.valueOf(1), tracking.get("APPLE"));
            tracking.expungeUnused();
            assertEquals(new HashSet<>(Arrays.asList("apple")), new HashSet<>(map.keySet()),
                    map.getClass().getSimpleName());
        }

        TreeMap<BigDecimal, String> decimals = new TreeMap<>();
        decimals.put(new BigDecimal("2.0"), "two");
        decimals.put(new BigDecimal("3.0"), "three");
        TrackingMap<BigDecimal, String> trackingDecimals = new TrackingMap<>(decimals);
        assertEquals("two", trackingDecimals.get(new BigDecimal("2.00")));
        trackingDecimals.expungeUnused();
        assertEquals(1, decimals.size());
        assertEquals("two", decimals.get(new BigDecimal("2")));

        IdentityHashMap<String, Integer> identity = new IdentityHashMap<>();
        String read = new String("k");
        String unread = new String("k");
        identity.put(read, 1);
        identity.put(unread, 2);
        TrackingMap<String, Integer> trackingIdentity = new TrackingMap<>(identity);
        assertEquals(Integer.valueOf(1), trackingIdentity.get(read));
        assertEquals(null, trackingIdentity.get(new String("k")));       // a miss: no entry is this object
        trackingIdentity.expungeUnused();
        assertEquals(1, identity.size());
        assertSame(read, identity.keySet().iterator().next());
    }
}
