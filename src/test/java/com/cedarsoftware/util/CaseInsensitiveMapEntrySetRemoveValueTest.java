package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for bugs: entrySet().remove() and entrySet().removeAll() ignore the
 * entry value, violating the Set&lt;Map.Entry&gt; contract.
 *
 * Per the Set contract, remove(entry) should only remove if both key AND value
 * match. The current implementation only checks the key.
 */
class CaseInsensitiveMapEntrySetRemoveValueTest {

    // --- entrySet().remove() ---

    @Test
    void testRemoveMatchingKeyAndValue() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("name", 1);

        boolean removed = map.entrySet().remove(new AbstractMap.SimpleEntry<>("name", 1));
        assertTrue(removed, "Should remove when both key and value match");
        assertEquals(0, map.size());
    }

    @Test
    void testRemoveMatchingKeyWrongValue() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("name", 1);

        boolean removed = map.entrySet().remove(new AbstractMap.SimpleEntry<>("name", 999));
        assertFalse(removed, "Should NOT remove when value doesn't match");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("name"));
    }

    @Test
    void testRemoveCaseInsensitiveKeyMatchingValue() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("Name", 1);

        boolean removed = map.entrySet().remove(new AbstractMap.SimpleEntry<>("NAME", 1));
        assertTrue(removed, "Should remove when key matches case-insensitively and value matches");
        assertEquals(0, map.size());
    }

    @Test
    void testRemoveCaseInsensitiveKeyWrongValue() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("Name", 1);

        boolean removed = map.entrySet().remove(new AbstractMap.SimpleEntry<>("NAME", 999));
        assertFalse(removed, "Should NOT remove when value doesn't match even if key matches case-insensitively");
        assertEquals(1, map.size());
    }

    @Test
    void testRemoveWithNullValue() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("name", null);

        // Matching null value should remove
        boolean removed = map.entrySet().remove(new AbstractMap.SimpleEntry<>("name", null));
        assertTrue(removed, "Should remove when both key and null value match");
        assertEquals(0, map.size());
    }

    @Test
    void testRemoveNullValueMismatch() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("name", 1);

        // null value doesn't match 1
        boolean removed = map.entrySet().remove(new AbstractMap.SimpleEntry<>("name", null));
        assertFalse(removed, "Should NOT remove when stored value is non-null but entry has null");
        assertEquals(1, map.size());
    }

    // --- entrySet().removeAll() ---

    @Test
    void testRemoveAllMatchingValues() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        boolean changed = map.entrySet().removeAll(Arrays.asList(
                new AbstractMap.SimpleEntry<>("a", 1),
                new AbstractMap.SimpleEntry<>("b", 2)
        ));
        assertTrue(changed);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("c"));
    }

    @Test
    void testRemoveAllWrongValues() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("a", 1);
        map.put("b", 2);

        boolean changed = map.entrySet().removeAll(Arrays.asList(
                new AbstractMap.SimpleEntry<>("a", 999),
                new AbstractMap.SimpleEntry<>("b", 888)
        ));
        assertFalse(changed, "Should NOT remove any entries when values don't match");
        assertEquals(2, map.size());
    }

    @Test
    void testRemoveAllMixedMatch() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        boolean changed = map.entrySet().removeAll(Arrays.asList(
                new AbstractMap.SimpleEntry<>("a", 1),    // matches
                new AbstractMap.SimpleEntry<>("b", 999)   // key matches but value doesn't
        ));
        assertTrue(changed, "Should remove only matching entries");
        assertEquals(2, map.size());
        assertFalse(map.containsKey("a"), "'a' should have been removed");
        assertTrue(map.containsKey("b"), "'b' should remain (value mismatch)");
        assertTrue(map.containsKey("c"));
    }

    @Test
    void testRemoveAllCaseInsensitive() {
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("Name", 1);
        map.put("Age", 2);

        boolean changed = map.entrySet().removeAll(Arrays.asList(
                new AbstractMap.SimpleEntry<>("NAME", 1),  // matches case-insensitively + value
                new AbstractMap.SimpleEntry<>("AGE", 999)  // matches key but not value
        ));
        assertTrue(changed);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("Age"), "'Age' should remain (value mismatch)");
    }
}
