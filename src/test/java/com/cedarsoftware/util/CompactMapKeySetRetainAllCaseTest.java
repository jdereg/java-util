package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for bug: keySet().retainAll() case-sensitivity mismatch.
 *
 * Bug: keySet().retainAll() creates a lookup map via getNewMap(). For legacy
 * subclasses that override isCaseInsensitive() to return true while
 * getNewMap() returns a plain HashMap, the lookup is case-sensitive.
 * Keys that differ only in case from the retain collection are incorrectly
 * removed.
 *
 * Example: CompactMap has key "Id", retain collection contains "id".
 * The lookup HashMap stores "id", and HashMap.containsKey("Id") returns
 * false (case-sensitive), so "Id" is incorrectly removed.
 *
 * Fix: When isCaseInsensitive() is true, use a CaseInsensitiveMap for
 * the lookup instead of getNewMap().
 */
class CompactMapKeySetRetainAllCaseTest {

    /**
     * Legacy-style subclass: case-insensitive with proper CaseInsensitiveMap backing.
     */
    private static class CaseInsensitiveLegacyMap<K, V> extends CompactMap<K, V> {
        @Override
        protected boolean isCaseInsensitive() { return true; }

        @Override
        protected Map<K, V> getNewMap() {
            return new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>(compactSize() + 1));
        }

        @Override
        protected int compactSize() { return 4; }
    }

    /**
     * retainAll with keys that differ only in case should retain them
     * when the map is case-insensitive.
     */
    @Test
    void testRetainAllCaseInsensitivePreservesKeys() {
        CompactMap<String, Integer> map = new CaseInsensitiveLegacyMap<>();
        map.put("Id", 1);
        map.put("Name", 2);
        map.put("Age", 3);

        // Retain "id" and "name" (different case than stored keys)
        map.keySet().retainAll(Arrays.asList("id", "name"));

        assertEquals(2, map.size(), "Should retain 2 entries (case-insensitive match)");
        assertTrue(map.containsKey("Id"), "Should retain 'Id' (matched by 'id')");
        assertTrue(map.containsKey("Name"), "Should retain 'Name' (matched by 'name')");
    }

    /**
     * retainAll should still remove keys not in the retain collection.
     */
    @Test
    void testRetainAllCaseInsensitiveRemovesNonMatching() {
        CompactMap<String, Integer> map = new CaseInsensitiveLegacyMap<>();
        map.put("Id", 1);
        map.put("Name", 2);
        map.put("Age", 3);

        map.keySet().retainAll(Arrays.asList("id"));

        assertEquals(1, map.size(), "Should retain only 1 entry");
        assertTrue(map.containsKey("Id"), "Should retain 'Id'");
    }

    /**
     * retainAll with exact case should work regardless.
     */
    @Test
    void testRetainAllExactCaseWorks() {
        CompactMap<String, Integer> map = new CaseInsensitiveLegacyMap<>();
        map.put("Id", 1);
        map.put("Name", 2);
        map.put("Age", 3);

        map.keySet().retainAll(Arrays.asList("Id", "Name"));

        assertEquals(2, map.size());
        assertTrue(map.containsKey("Id"));
        assertTrue(map.containsKey("Name"));
    }

    /**
     * retainAll in Map state (> compactSize entries) should also respect
     * case-insensitivity.
     */
    @Test
    void testRetainAllInMapState() {
        CompactMap<String, Integer> map = new CaseInsensitiveLegacyMap<>();
        // compactSize=4, so 5 entries → Map state
        map.put("Id", 1);
        map.put("Name", 2);
        map.put("Age", 3);
        map.put("City", 4);
        map.put("Zip", 5);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        map.keySet().retainAll(Arrays.asList("id", "name", "city"));

        assertEquals(3, map.size(), "Should retain 3 entries");
        assertTrue(map.containsKey("Id"));
        assertTrue(map.containsKey("Name"));
        assertTrue(map.containsKey("City"));
    }

    /**
     * retainAll on a case-sensitive map should remain case-sensitive.
     */
    @Test
    void testRetainAllCaseSensitiveMapUnaffected() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .caseSensitive(true)
                .build();
        map.put("Id", 1);
        map.put("Name", 2);
        map.put("Age", 3);

        // "id" does NOT match "Id" in case-sensitive mode
        map.keySet().retainAll(Arrays.asList("id", "Name"));

        assertEquals(1, map.size(), "Should retain only 'Name' (exact case match)");
        assertTrue(map.containsKey("Name"));
    }
}
