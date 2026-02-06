package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for bug: equals() symmetry violation for case-insensitive maps.
 *
 * Bug: A case-insensitive CompactMap's equals() used containsKey()/get()
 * with case-insensitive matching, so compact.equals(hashMap) returned true
 * even when keys differed only in case. This violated the Map.equals()
 * contract which is formally defined as m1.entrySet().equals(m2.entrySet()),
 * where Entry.equals uses Object.equals for keys.
 *
 * Fix: For case-insensitive maps, equals() now uses strict Object.equals()
 * for key comparison via a temporary HashMap lookup, matching the formal
 * Map.equals() contract.
 *
 * Note: HashMap.equals(compactMap) calls compactMap.get()/containsKey()
 * which remain case-insensitive (by design). This means full symmetry
 * cannot be achieved from CompactMap's side alone — it is inherent to
 * case-insensitive maps. The fix ensures CompactMap's own equals() follows
 * the Map contract.
 */
class CompactMapEqualsSymmetryTest {

    /**
     * Case-insensitive CompactMap's equals() must use strict key comparison
     * per the Map.equals() contract. compact.equals(hash) should be false
     * when keys differ only in case.
     */
    @Test
    void testCompactEqualsUsesStrictKeysForSingleEntry() {
        CompactMap<String, Integer> compact = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .build();
        compact.put("id", 1);

        Map<String, Integer> hash = new HashMap<>();
        hash.put("ID", 1);

        // compact.equals(hash) should be false: "id" != "ID" by Object.equals
        assertFalse(compact.equals(hash),
                "Case-insensitive CompactMap.equals() should use strict key comparison per Map contract");
    }

    /**
     * Same test in compact array state (multiple entries).
     */
    @Test
    void testCompactEqualsUsesStrictKeysForCompactArray() {
        CompactMap<String, Integer> compact = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .build();
        compact.put("name", 1);
        compact.put("age", 2);

        Map<String, Integer> hash = new HashMap<>();
        hash.put("Name", 1);
        hash.put("Age", 2);

        assertFalse(compact.equals(hash),
                "Case-insensitive CompactMap.equals() should use strict key comparison in array state");
    }

    /**
     * Same test in Map state (> compactSize entries).
     */
    @Test
    void testCompactEqualsUsesStrictKeysForMapState() {
        CompactMap<String, Integer> compact = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .compactSize(2)
                .build();
        compact.put("a", 1);
        compact.put("b", 2);
        compact.put("c", 3);

        Map<String, Integer> hash = new HashMap<>();
        hash.put("A", 1);
        hash.put("B", 2);
        hash.put("C", 3);

        assertFalse(compact.equals(hash),
                "Case-insensitive CompactMap.equals() should use strict key comparison in map state");
    }

    /**
     * When keys match exactly (same case), both directions should return true.
     * This is the symmetric case.
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
