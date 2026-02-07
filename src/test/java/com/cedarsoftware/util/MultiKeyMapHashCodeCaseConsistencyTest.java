package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for bug: MultiKeyMap.hashCode() uses case-sensitive String.hashCode()
 * even when the map is configured as case-insensitive.
 *
 * Bug: hashCode() iterates entrySet() which reconstructs keys as original-case
 * Strings, then uses Objects.hashCode(key) → String.hashCode() (case-sensitive).
 * Two equal case-insensitive MultiKeyMaps with different-case keys produce
 * different hashCodes, violating the hashCode contract.
 *
 * Fix: Use the pre-computed case-insensitive MultiKey.hash for the key portion
 * of the hash computation instead of going through entrySet().
 */
class MultiKeyMapHashCodeCaseConsistencyTest {

    /**
     * Two case-insensitive MultiKeyMaps with same-case keys should have same hashCode.
     * (This already works — sanity check.)
     */
    @Test
    void testSameCaseKeysSameHashCode() {
        MultiKeyMap<String> map1 = MultiKeyMap.<String>builder()
                .caseSensitive(false)
                .build();
        map1.put("hello", "value1");

        MultiKeyMap<String> map2 = MultiKeyMap.<String>builder()
                .caseSensitive(false)
                .build();
        map2.put("hello", "value1");

        assertTrue(map1.equals(map2), "Same-case maps should be equal");
        assertEquals(map1.hashCode(), map2.hashCode(),
                "Same-case equal maps must have same hashCode");
    }

    /**
     * THE BUG: Two case-insensitive MultiKeyMaps with different-case keys
     * are equal but have different hashCodes.
     */
    @Test
    void testDifferentCaseKeysSameHashCode() {
        MultiKeyMap<String> map1 = MultiKeyMap.<String>builder()
                .caseSensitive(false)
                .build();
        map1.put("hello", "value1");
        map1.put("world", "value2");

        MultiKeyMap<String> map2 = MultiKeyMap.<String>builder()
                .caseSensitive(false)
                .build();
        map2.put("HELLO", "value1");
        map2.put("WORLD", "value2");

        assertTrue(map1.equals(map2), "Case-insensitive maps with different-case keys should be equal");
        assertTrue(map2.equals(map1), "Equality should be symmetric");
        assertEquals(map1.hashCode(), map2.hashCode(),
                "Equal case-insensitive maps must have same hashCode (contract violation)");
    }

    /**
     * Multiple entries with mixed-case keys should produce consistent hashCodes.
     */
    @Test
    void testMultipleEntriesMixedCase() {
        MultiKeyMap<Integer> map1 = MultiKeyMap.<Integer>builder()
                .caseSensitive(false)
                .build();
        map1.put("Name", 1);
        map1.put("Age", 2);
        map1.put("City", 3);

        MultiKeyMap<Integer> map2 = MultiKeyMap.<Integer>builder()
                .caseSensitive(false)
                .build();
        map2.put("name", 1);
        map2.put("AGE", 2);
        map2.put("CITY", 3);

        assertTrue(map1.equals(map2));
        assertEquals(map1.hashCode(), map2.hashCode(),
                "Equal maps with mixed-case keys must have same hashCode");
    }

    /**
     * Case-sensitive mode should be unaffected — same-case keys produce same hashCode.
     */
    @Test
    void testCaseSensitiveUnaffected() {
        MultiKeyMap<String> map1 = MultiKeyMap.<String>builder()
                .caseSensitive(true)
                .build();
        map1.put("hello", "value1");

        MultiKeyMap<String> map2 = MultiKeyMap.<String>builder()
                .caseSensitive(true)
                .build();
        map2.put("hello", "value1");

        assertTrue(map1.equals(map2));
        assertEquals(map1.hashCode(), map2.hashCode());
    }
}
