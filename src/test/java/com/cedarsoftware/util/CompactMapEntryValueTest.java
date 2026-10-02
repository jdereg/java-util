package com.cedarsoftware.util;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * With one entry, a CompactMap stores the value alone when the key is its single-value key ("id" by default), and
 * otherwise a CompactMapEntry holding key and value. It told the two apart by asking whether the stored object is a
 * CompactMapEntry, so a value that was one -- an entry from any CompactMap's entrySet(), for instance -- stored under
 * "id" was read as the map's own entry: the map then reported that entry's key and value, and get("id") answered
 * null. Such a value is now kept in an entry of its own, like a Map or Object[] value.
 */
class CompactMapEntryValueTest {

    private static Map.Entry<String, Object> entryOf(CompactMap<String, Object> map) {
        return map.entrySet().iterator().next();
    }

    @Test
    void anEntryFromAnotherMapIsAValueLikeAnyOther() {
        CompactMap<String, Object> other = new CompactMap<>();
        other.put("a", "A");
        other.put("b", "B");
        Map.Entry<String, Object> entry = entryOf(other);

        CompactMap<String, Object> map = new CompactMap<>();
        map.put("id", entry);
        assertSame(entry, map.get("id"));
        assertTrue(map.containsKey("id"));
        assertEquals(Collections.singleton("id"), map.keySet());
        assertEquals(1, map.size());
        Map.Entry<String, Object> only = entryOf(map);
        assertEquals("id", only.getKey());
        assertSame(entry, only.getValue());
    }

    @Test
    void theMapsOwnSingleEntryStoredAsAValue() {
        CompactMap<String, Object> map = new CompactMap<>();
        map.put("x", 1);
        Map.Entry<String, Object> entry = entryOf(map);       // the map's own stored entry
        map.clear();
        map.put("id", entry);
        assertSame(entry, map.get("id"));
        assertEquals(Collections.singleton("id"), map.keySet());
    }

    @Test
    void anEntryValueSurvivesEveryWayIntoTheSingleEntryState() {
        CompactMap<String, Object> source = new CompactMap<>();
        source.put("q", 1);
        Map.Entry<String, Object> entry = entryOf(source);

        CompactMap<String, Object> overwritten = new CompactMap<>();
        overwritten.put("id", 1);
        overwritten.put("id", entry);                          // overwrite at size 1
        assertSame(entry, overwritten.get("id"));
        assertEquals(Collections.singleton("id"), overwritten.keySet());

        CompactMap<String, Object> shrunk = new CompactMap<>();
        shrunk.put("id", entry);
        shrunk.put("other", 2);
        shrunk.remove("other");                                // two entries back to one
        assertSame(entry, shrunk.get("id"));
        assertEquals(Collections.singleton("id"), shrunk.keySet());
    }
}
