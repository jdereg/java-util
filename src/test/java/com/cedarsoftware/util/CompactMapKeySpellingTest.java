package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A case-insensitive CompactMap keeps each key as it was first written, as CaseInsensitiveMap does, but not at size
 * one. A key that matched the single-value key ignoring case ("ID" for "id") had its value stored alone and was then
 * reported as "id"; and overwriting the one entry with a key spelled otherwise replaced the stored spelling ("Name"
 * became "NAME"), where two or more entries keep it. The value is now stored alone only for the single-value key as
 * spelled, and an overwrite at size one keeps the stored key, as at every other size.
 */
class CompactMapKeySpellingTest {

    private static List<Object> keys(java.util.Map<?, ?> map) {
        return new ArrayList<>(map.keySet());
    }

    @Test
    void aKeyMatchingTheSingleValueKeyIgnoringCaseKeepsItsSpelling() {
        CompactMap<String, Object> legacy = new CompactCIHashMap<>();
        legacy.put("ID", 1);
        assertEquals(Collections.singletonList("ID"), keys(legacy));
        assertEquals(1, legacy.get("id"));

        CompactMap<String, Object> built = CompactMap.<String, Object>builder().caseSensitive(false).build();
        built.put("Id", 1);
        assertEquals(Collections.singletonList("Id"), keys(built));
        assertEquals(1, built.get("ID"));
    }

    @Test
    void theSingleValueKeyAsSpelledIsStillStoredAlone() {
        CompactMap<String, Object> ci = new CompactCIHashMap<>();
        ci.put("id", 1);
        assertEquals(CompactMap.LogicalValueType.OBJECT, ci.getLogicalValueType());
        ci.put("ID", 2);            // an overwrite keeps the stored spelling, and the value alone
        assertEquals(CompactMap.LogicalValueType.OBJECT, ci.getLogicalValueType());
        assertEquals(Collections.singletonList("id"), keys(ci));
        assertEquals(2, ci.get("Id"));

        CompactMap<String, Object> cs = new CompactMap<>();
        cs.put("id", 1);
        assertEquals(CompactMap.LogicalValueType.OBJECT, cs.getLogicalValueType());
    }

    @Test
    void anOverwriteInADifferentCaseKeepsTheStoredKeyAtEverySize() {
        for (int others : new int[]{0, 1, 60}) {
            for (CompactMap<String, Object> map : Arrays.<CompactMap<String, Object>>asList(new CompactCIHashMap<>(),
                    new CompactCILinkedMap<>(),
                    CompactMap.<String, Object>builder().caseSensitive(false).sortedOrder().build())) {
                map.put("Name", 1);
                for (int i = 0; i < others; i++) {
                    map.put("k" + i, i);
                }
                map.put("NAME", 2);
                String stored = null;
                for (String key : map.keySet()) {
                    if (key.equalsIgnoreCase("name")) {
                        stored = key;
                    }
                }
                assertEquals("Name", stored, map.getClass().getSimpleName() + " with " + (others + 1) + " entries");
                assertEquals(2, map.get("name"));
            }
        }
    }

    @Test
    void aCaseInsensitiveCompactSetKeepsItsElementsSpelling() {
        Set<String> built = CompactSet.<String>builder().caseSensitive(false).build();
        built.add("ID");
        assertEquals(Collections.singletonList("ID"), new ArrayList<>(built));
        Set<String> legacy = new CompactCIHashSet<>();
        legacy.add("Id");
        assertEquals(Collections.singletonList("Id"), new ArrayList<>(legacy));
    }
}
