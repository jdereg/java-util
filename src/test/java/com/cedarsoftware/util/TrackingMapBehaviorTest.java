package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    private static TreeMap<String, Integer> abcd() {
        TreeMap<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        return map;
    }

    /**
     * A subMap, headMap or tailMap view shares its parent's record of reads, and forgets only reads of keys it
     * holds. expungeUnused() on a view reset the shared record to the view's own keys, and clear() or
     * replaceContents() on a view emptied it, so the parent's next expungeUnused() removed entries it had read.
     */
    @Test
    void aViewForgetsOnlyReadsOfItsOwnKeys() {
        TrackingMap<String, Integer> parent = new TrackingMap<>(abcd());
        parent.get("a");
        parent.get("d");
        TrackingMap<String, Integer> middle = parent.subMap("b", true, "c", true);
        middle.get("b");
        middle.expungeUnused();                                     // removes c, which nobody read
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "d")), new HashSet<>(parent.keysUsed()));
        parent.expungeUnused();
        assertEquals(Arrays.asList("a", "b", "d"), new ArrayList<>(parent.keySet()));

        TrackingMap<String, Integer> cleared = new TrackingMap<>(abcd());
        cleared.get("a");
        cleared.get("d");
        cleared.headMap("b").clear();                               // removes a, and forgets reading it
        assertEquals(new HashSet<>(Arrays.asList("d")), new HashSet<>(cleared.keysUsed()));
        cleared.expungeUnused();
        assertEquals(Arrays.asList("d"), new ArrayList<>(cleared.keySet()));

        TrackingMap<String, Integer> replaced = new TrackingMap<>(abcd());
        replaced.get("a");
        replaced.get("d");
        replaced.tailMap("c").replaceContents(Collections.singletonMap("c", 30));
        assertEquals(new HashSet<>(Arrays.asList("a")), new HashSet<>(replaced.keysUsed()));
        replaced.expungeUnused();
        assertEquals(Arrays.asList("a"), new ArrayList<>(replaced.keySet()));
    }

    /**
     * equals() between two TrackingMaps compares their wrapped maps, so it records no reads in the other one.
     * It compared against the other TrackingMap itself, whose get() recorded every key as read, so that map's
     * next expungeUnused() kept entries the program never read.
     */
    @Test
    void equalsRecordsNoReadsInTheOtherMap() {
        TrackingMap<String, Integer> one = new TrackingMap<>(new HashMap<>(abcd()));
        TrackingMap<String, Integer> two = new TrackingMap<>(new HashMap<>(abcd()));
        assertTrue(one.equals(two));
        assertTrue(two.equals(one));
        assertTrue(one.keysUsed().isEmpty());
        assertTrue(two.keysUsed().isEmpty());
        two.put("e", 5);
        assertFalse(one.equals(two));
        assertFalse(two.equals(one));
        assertEquals(one, abcd());
        assertTrue(two.keysUsed().isEmpty());
        two.expungeUnused();
        assertTrue(two.isEmpty());
    }
}
