package com.cedarsoftware.util;

import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Pins the {@link java.util.Map#getOrDefault} contract for the null-value-supporting
 * ConcurrentMap implementations in java-util.
 * <p>
 * {@code ConcurrentMap}'s default {@code getOrDefault} assumes the map cannot contain null
 * values (a null return from {@code get()} is treated as "no mapping"), and its javadoc
 * requires implementations that support null values to override it. CaseInsensitiveMap and
 * MultiKeyMap both implement ConcurrentMap and both permit null values, but inherited the
 * default — so a key present but explicitly mapped to null incorrectly returned
 * {@code defaultValue} instead of null. (ClassValueMap had the same bug; its cases live in
 * ClassValueMapReviewFixesTest.)
 */
class GetOrDefaultNullValueContractTest {

    // --- CaseInsensitiveMap ---

    @Test
    void testCaseInsensitiveMapNullValuedKeyReturnsNull() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Key", null);

        assertNull(map.getOrDefault("Key", "DEFAULT"), "Present, mapped to null → null");
        assertNull(map.getOrDefault("KEY", "DEFAULT"), "Case-varied lookup must see the same null mapping");
        assertNull(map.getOrDefault("key", "DEFAULT"), "Case-varied lookup must see the same null mapping");
    }

    @Test
    void testCaseInsensitiveMapAbsentAndNonNullCases() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Present", "value");

        assertEquals("value", map.getOrDefault("PRESENT", "DEFAULT"), "Present non-null, case-varied");
        assertEquals("DEFAULT", map.getOrDefault("absent", "DEFAULT"), "Absent key → default");
    }

    @Test
    void testCaseInsensitiveMapNullKeyMappedToNull() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put(null, null);

        assertNull(map.getOrDefault(null, "DEFAULT"), "Null key mapped to null → null");
        map.remove(null);
        assertEquals("DEFAULT", map.getOrDefault(null, "DEFAULT"), "Absent null key → default");
    }

    @Test
    void testCaseInsensitiveMapNonStringKeys() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
        map.put(42, null);

        assertNull(map.getOrDefault(42, "DEFAULT"), "Non-String key mapped to null → null");
        assertEquals("DEFAULT", map.getOrDefault(99, "DEFAULT"), "Absent non-String key → default");
    }

    @Test
    void testCaseInsensitiveMapConcurrentBackingUnchanged() {
        // ConcurrentHashMap backing cannot hold null values; getOrDefault semantics
        // there are purely present/absent — must be unaffected by the override.
        CaseInsensitiveMap<String, String> map =
                new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
        map.put("Key", "value");

        assertEquals("value", map.getOrDefault("KEY", "DEFAULT"));
        assertEquals("DEFAULT", map.getOrDefault("absent", "DEFAULT"));
    }

    // --- MultiKeyMap ---

    @Test
    void testMultiKeyMapNullValuedKeyReturnsNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("key", null);

        assertNull(map.getOrDefault("key", "DEFAULT"), "Present, mapped to null → null");
        assertEquals("DEFAULT", map.getOrDefault("absent", "DEFAULT"), "Absent key → default");
    }

    @Test
    void testMultiKeyMapNonNullAndNullKeyCases() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put("present", "value");
        map.put(null, null);

        assertEquals("value", map.getOrDefault("present", "DEFAULT"));
        assertNull(map.getOrDefault(null, "DEFAULT"), "Null key mapped to null → null");
    }

    @Test
    void testMultiKeyMapMultiDimensionalKeyMappedToNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        map.put(new Object[]{"a", "b"}, null);

        assertNull(map.getOrDefault(new Object[]{"a", "b"}, "DEFAULT"),
                "Flattened multi-key mapped to null → null");
        assertEquals("DEFAULT", map.getOrDefault(new Object[]{"a", "c"}, "DEFAULT"),
                "Absent multi-key → default");
    }
}
