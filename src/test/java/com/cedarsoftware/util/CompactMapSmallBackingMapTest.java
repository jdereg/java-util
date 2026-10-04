package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A CompactMap keeps one entry alone, 2 to compactSize entries in an array, and more in a backing map. putAll()
 * switched to the backing map by counting the incoming keys, but a case-insensitive map collapses keys that differ
 * only in case ("ID" and "Id"), so it could be left holding a backing map of compactSize entries or fewer. That state
 * was never undone, and an iterator removal from it built a one-pair array, a state the rest of the class does not
 * expect: the map then read the array itself as its single value, handed it out as a value, and lost entries. A
 * backing map that shrinks to compactSize entries or fewer, however it got there, now becomes an array, a single
 * entry or the empty state.
 */
class CompactMapSmallBackingMapTest {

    private static CompactMap<String, Object> ciMap(int compactSize) {
        return CompactMap.<String, Object>builder().caseSensitive(false).compactSize(compactSize).build();
    }

    private static Map<String, Object> ordered(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    private static void assertStateMatchesSize(CompactMap<?, ?> map, int compactSize) {
        int size = map.size();
        CompactMap.LogicalValueType expected = size == 0 ? CompactMap.LogicalValueType.EMPTY
                : size > compactSize ? CompactMap.LogicalValueType.MAP
                : size >= 2 ? CompactMap.LogicalValueType.ARRAY : null;
        CompactMap.LogicalValueType actual = map.getLogicalValueType();
        if (expected == null) {
            assertEquals(true,
                    actual == CompactMap.LogicalValueType.OBJECT || actual == CompactMap.LogicalValueType.ENTRY,
                    "size 1 in state " + actual);
        } else {
            assertEquals(expected, actual, "size " + size);
        }
    }

    @Test
    void putAllOfKeysThatCollapseLeavesTheStateItsSizeCallsFor() {
        CompactMap<String, Object> map = ciMap(3);
        map.putAll(ordered("a", 1, "A", 2, "b", 3, "B", 4));
        assertEquals(2, map.size());
        assertEquals(2, map.get("a"));
        assertEquals(4, map.get("b"));
        assertStateMatchesSize(map, 3);

        CompactMap<String, Object> single = ciMap(2);
        single.putAll(ordered("x", 1, "X", 2, "Y", 3, "y", 4, "x", 5));
        assertEquals(2, single.size());
        assertStateMatchesSize(single, 2);
    }

    /** The sequence the differential fuzz found: the map lost "A" and read its own array as a value. */
    @Test
    void entrySetRetainAllAfterACollapsedPutAllKeepsTheRetainedEntry() {
        CompactMap<String, Object> map = ciMap(2);
        map.putAll(ordered("ID", 7, "Id", 9, "k1", 6));
        map.remove("k1");
        map.put("A", 15);
        assertEquals(2, map.size());
        map.entrySet().retainAll(Collections.singletonList(new AbstractMap.SimpleEntry<>("A", 15)));
        assertEquals(Collections.singletonMap("A", 15), new LinkedHashMap<>(map));
        assertStateMatchesSize(map, 2);
    }

    @Test
    void iteratorRemovalFromTheBackingMapReachesEveryState() {
        CompactMap<String, Object> map = ciMap(3);
        for (int i = 0; i < 6; i++) {
            map.put("k" + i, i);
        }
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        int seen = 0;
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            seen++;
            assertEquals(entry.getValue(), Integer.valueOf(entry.getKey().substring(1)));
            it.remove();
            assertStateMatchesSize(map, 3);
        }
        assertEquals(6, seen);
        assertEquals(0, map.size());
    }
}
