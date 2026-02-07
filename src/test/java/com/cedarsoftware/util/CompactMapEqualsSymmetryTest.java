package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for CompactMap.equals() symmetry and correctness across
 * case-sensitive, case-insensitive, and cross-map-type scenarios.
 */
class CompactMapEqualsSymmetryTest {

    /**
     * When keys match exactly (same case), both directions should return true.
     */
    @Test
    void testEqualsSymmetrySameCaseKeys() {
        CompactMap<String, Integer> compact = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .build();
        compact.put("id", 1);

        Map<String, Integer> hash = new HashMap<>();
        hash.put("id", 1);

        assertTrue(compact.equals(hash), "Same-case keys should be equal");
        assertTrue(hash.equals(compact), "Same-case keys should be equal (reverse)");
    }

    /**
     * Two case-insensitive CompactMaps with same-case keys should be equal.
     */
    @Test
    void testTwoCaseInsensitiveMapsEqualSameCase() {
        CompactMap<String, Integer> map1 = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .build();
        map1.put("id", 1);
        map1.put("name", 2);

        CompactMap<String, Integer> map2 = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .build();
        map2.put("id", 1);
        map2.put("name", 2);

        assertTrue(map1.equals(map2));
        assertTrue(map2.equals(map1));
    }

    /**
     * Case-sensitive CompactMap should not be affected — equals works normally.
     */
    @Test
    void testCaseSensitiveUnaffected() {
        CompactMap<String, Integer> compact = CompactMap.<String, Integer>builder()
                .caseSensitive(true)
                .build();
        compact.put("id", 1);

        Map<String, Integer> hash = new HashMap<>();
        hash.put("id", 1);

        assertTrue(compact.equals(hash));
        assertTrue(hash.equals(compact));
    }

    /**
     * Case-sensitive CompactMap with different keys correctly returns false.
     */
    @Test
    void testCaseSensitiveDifferentKeys() {
        CompactMap<String, Integer> compact = CompactMap.<String, Integer>builder()
                .caseSensitive(true)
                .build();
        compact.put("id", 1);

        Map<String, Integer> hash = new HashMap<>();
        hash.put("ID", 1);

        assertFalse(compact.equals(hash));
        assertFalse(hash.equals(compact));
    }

    /**
     * Same-case keys in compact array state should be equal in both directions.
     */
    @Test
    void testSameCaseMultipleEntries() {
        CompactMap<String, Integer> compact = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .build();
        compact.put("name", 1);
        compact.put("age", 2);

        Map<String, Integer> hash = new HashMap<>();
        hash.put("name", 1);
        hash.put("age", 2);

        assertTrue(compact.equals(hash), "Same-case multi-entry should be equal");
        assertTrue(hash.equals(compact), "Same-case multi-entry should be equal (reverse)");
    }
}
