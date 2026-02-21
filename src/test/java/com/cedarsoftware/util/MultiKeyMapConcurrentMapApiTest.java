package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for ConcurrentMap APIs on MultiKeyMap, including regression tests for
 * null key/value correctness bugs found by Codex 5.3 review.
 *
 * MKM contract: null keys AND null values are fully supported.
 */
class MultiKeyMapConcurrentMapApiTest {

    @Test
    void testRemoveWithValue() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put((Object) "a", (String) "val");
        assertFalse(map.remove("a", "x"));
        assertTrue(map.remove("a", "val"));
        assertFalse(map.containsKey("a"));
    }

    @Test
    void testReplaceMethods() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        assertNull(map.replace("a", "x"));
        map.put((Object) "a", (String) "orig");
        assertTrue(map.replace("a", "orig", "new"));
        assertEquals("new", map.get("a"));
        assertEquals("new", map.replace("a", "latest"));
        assertEquals("latest", map.get("a"));
    }

    // ---------------------------------------------------------------
    // Bug 1: remove(key, value) false positive for absent key + null value
    // ---------------------------------------------------------------

    @Test
    void testRemoveAbsentKeyWithNullValue_returnsFalse() {
        // BUG: remove(absentKey, null) returned true because getNoLock returns null
        // for absent keys, and Objects.equals(null, null) is true
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        assertFalse(map.remove("absent", null));
        assertEquals(0, map.size());
    }

    @Test
    void testRemoveAbsentNullKeyWithNullValue_returnsFalse() {
        // Same bug but with the null key itself
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        assertFalse(map.remove(null, null));
        assertEquals(0, map.size());
    }

    @Test
    void testRemovePresentKeyWithNullValue_removesEntry() {
        // Ensures the fix doesn't break: key exists, value IS null -> should remove
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put((Object) "key", (String) null);
        assertTrue(map.containsKey("key"));
        assertEquals(1, map.size());

        assertTrue(map.remove("key", null));
        assertFalse(map.containsKey("key"));
        assertEquals(0, map.size());
    }

    @Test
    void testRemoveNullKeyWithNullValue_removesEntry() {
        // null key mapped to null value -> remove(null, null) should succeed
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertEquals(1, map.size());

        assertTrue(map.remove(null, null));
        assertFalse(map.containsKey(null));
        assertEquals(0, map.size());
    }

    @Test
    void testRemoveNullKeyWithNonNullValue_returnsFalse() {
        // null key mapped to "hello" -> remove(null, null) should fail (values don't match)
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put(null, "hello");
        assertFalse(map.remove(null, null));
        assertTrue(map.containsKey(null));
        assertEquals("hello", map.get(null));
    }

    @Test
    void testRemovePresentKeyWithNonNullValue_mismatch() {
        // key mapped to non-null -> remove(key, null) should return false
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put((Object) "key", (String) "value");
        assertFalse(map.remove("key", null));
        assertTrue(map.containsKey("key"));
    }

    // ---------------------------------------------------------------
    // Bug 2: replace(key, oldValue, newValue) inserting on absent key
    // ---------------------------------------------------------------

    @Test
    void testReplaceThreeArgAbsentKeyWithNullOldValue_returnsFalse() {
        // BUG: replace(absentKey, null, newValue) inserted a new entry because
        // getNoLock returns null for absent keys, and Objects.equals(null, null) is true
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        assertFalse(map.replace("absent", null, "newValue"));
        assertEquals(0, map.size());
        assertFalse(map.containsKey("absent"));
    }

    @Test
    void testReplaceThreeArgAbsentNullKeyWithNullOldValue_returnsFalse() {
        // Same bug but with null key itself
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        assertFalse(map.replace(null, null, "newValue"));
        assertEquals(0, map.size());
        assertFalse(map.containsKey(null));
    }

    @Test
    void testReplaceThreeArgPresentKeyWithNullValue_succeeds() {
        // Ensures fix doesn't break: key exists with null value -> replace should work
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put((Object) "key", (String) null);
        assertTrue(map.containsKey("key"));

        assertTrue(map.replace("key", null, "updated"));
        assertEquals("updated", map.get("key"));
        assertEquals(1, map.size());
    }

    @Test
    void testReplaceThreeArgNullKeyWithNullValue_succeeds() {
        // null key mapped to null -> replace(null, null, "new") should succeed
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put(null, null);
        assertTrue(map.containsKey(null));

        assertTrue(map.replace(null, null, "updated"));
        assertEquals("updated", map.get(null));
        assertEquals(1, map.size());
    }

    @Test
    void testReplaceThreeArgPresentKeyValueMismatch_returnsFalse() {
        // key exists with non-null value -> replace(key, null, ...) should fail
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put((Object) "key", (String) "value");
        assertFalse(map.replace("key", null, "new"));
        assertEquals("value", map.get("key"));
    }

    // ---------------------------------------------------------------
    // replace(key, value) — two-arg form with null edge cases
    // ---------------------------------------------------------------

    @Test
    void testReplaceTwoArgAbsentKey_returnsNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        assertNull(map.replace("absent", "value"));
        assertEquals(0, map.size());
    }

    @Test
    void testReplaceTwoArgAbsentNullKey_returnsNull() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        assertNull(map.replace(null, "value"));
        assertEquals(0, map.size());
    }

    @Test
    void testReplaceTwoArgPresentKeyWithNullValue_succeeds() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put((Object) "key", (String) null);
        assertNull(map.replace("key", "updated"));
        assertEquals("updated", map.get("key"));
    }

    @Test
    void testReplaceTwoArgNullKeyPresent_succeeds() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put(null, "original");
        assertEquals("original", map.replace(null, "updated"));
        assertEquals("updated", map.get(null));
    }

    // ---------------------------------------------------------------
    // Bug 3: BigDecimal/BigInteger vs Infinity/NaN — NumberFormatException
    // ---------------------------------------------------------------

    @Test
    void testBigDecimalVsInfinityComparison() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put(new BigDecimal("100"), "bigdec");
        assertDoesNotThrow(() -> map.get(Double.POSITIVE_INFINITY));
        assertNull(map.get(Double.POSITIVE_INFINITY));
        assertNull(map.get(Double.NEGATIVE_INFINITY));
        assertNull(map.get(Double.NaN));
    }

    @Test
    void testBigIntegerVsInfinityComparison() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put(BigInteger.TEN, "bigint");
        assertDoesNotThrow(() -> map.get(Double.POSITIVE_INFINITY));
        assertNull(map.get(Double.POSITIVE_INFINITY));
        assertNull(map.get(Double.NaN));
    }

    @Test
    void testInfinityKeyVsBigDecimalLookup() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put(Double.POSITIVE_INFINITY, "inf");
        assertDoesNotThrow(() -> map.get(new BigDecimal("100")));
        assertNull(map.get(new BigDecimal("100")));
        assertEquals("inf", map.get(Double.POSITIVE_INFINITY));
    }

    @Test
    void testFloatInfinityVsBigDecimal() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);
        map.put(new BigDecimal("999"), "bd");
        assertDoesNotThrow(() -> map.get(Float.POSITIVE_INFINITY));
        assertDoesNotThrow(() -> map.get(Float.NaN));
        assertNull(map.get(Float.POSITIVE_INFINITY));
        assertNull(map.get(Float.NaN));
    }
}
