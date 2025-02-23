package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompactMapLegacyConfigTest {
    private static final int TEST_COMPACT_SIZE = 3;

    @Test
    public void testLegacyCompactSizeTransitions() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() { return new HashMap<>(); }
        };

        // Test size transitions
        map.put("A", "alpha");
        assertEquals(1, map.size());

        map.put("B", "bravo");
        assertEquals(2, map.size());

        map.put("C", "charlie");
        assertEquals(3, map.size());

        // This should transition to backing map
        map.put("D", "delta");
        assertEquals(4, map.size());
        assertInstanceOf(Map.class, map.val);
    }

    @Test
    public void testLegacyReverseCaseSensitive() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() {
                return new TreeMap<>(Collections.reverseOrder());
            }
        };

        verifyMapBehavior(map, true, true); // reverse=true, caseSensitive=true
    }

    @Test
    public void testLegacyReverseCaseInsensitive() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() {
                return new TreeMap<>(Collections.reverseOrder(String.CASE_INSENSITIVE_ORDER));
            }
            protected boolean isCaseInsensitive() { return true; }
        };

        verifyMapBehavior(map, true, false); // reverse=true, caseSensitive=false
    }

    @Test
    public void testLegacySortedCaseSensitive() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() { return new TreeMap<>(); }
        };

        verifyMapBehavior(map, false, true); // reverse=false, caseSensitive=true
    }

    @Test
    public void testLegacySortedCaseInsensitive() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() {
                return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            }
            protected boolean isCaseInsensitive() { return true; }
        };

        verifyMapBehavior(map, false, false); // reverse=false, caseSensitive=false
    }

    @Test
    public void testLegacySequenceCaseSensitive() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() { return new LinkedHashMap<>(); }
        };

        verifySequenceMapBehavior(map, true); // caseSensitive=true
    }

    @Test
    public void testLegacySequenceCaseInsensitive() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() {
                return new CaseInsensitiveMap<>(Collections.emptyMap(), new LinkedHashMap<>());
            }
            protected boolean isCaseInsensitive() { return true; }
        };

        verifySequenceMapBehavior(map, false); // caseSensitive=false
    }

    @Test
    public void testLegacyUnorderedCaseSensitive() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() { return new HashMap<>(); }
        };

        verifyUnorderedMapBehavior(map, true); // caseSensitive=true
    }

    @Test
    public void testLegacyUnorderedCaseInsensitive() {
        CompactMap<String, String> map = new CompactMap<String, String>() {
            protected int compactSize() { return TEST_COMPACT_SIZE; }
            protected Map<String, String> getNewMap() {
                return new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>());
            }
            protected boolean isCaseInsensitive() { return true; }
        };

        verifyUnorderedMapBehavior(map, false); // caseSensitive=false
    }

    @Test
    public void testLegacyConfigurationMismatch() {
        assertThrows(IllegalStateException.class, () -> {
            new CompactMap<String, String>() {
                protected int compactSize() { return TEST_COMPACT_SIZE; }
                protected Map<String, String> getNewMap() {
                    return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                }
                protected boolean isCaseInsensitive() { return false; } // Mismatch!
            };
        });
    }

    // Helper methods for verification
    private void verifyMapBehavior(CompactMap<String, String> map, boolean reverse, boolean caseSensitive) {
        // Test at size 1
        map.put("C", "charlie");
        verifyMapState(map, 1, reverse, caseSensitive);

        // Test at size 2
        map.put("A", "alpha");
        verifyMapState(map, 2, reverse, caseSensitive);

        // Test at size 3 (compact array)
        map.put("B", "bravo");
        verifyMapState(map, 3, reverse, caseSensitive);

        // Test at size 4 (backing map)
        map.put("D", "delta");
        verifyMapState(map, 4, reverse, caseSensitive);
    }

    private void verifyMapState(CompactMap<String, String> map, int expectedSize, boolean reverse, boolean caseSensitive) {
        assertEquals(expectedSize, map.size());

        // Get the actual keys that are in the map
        List<String> keys = new ArrayList<>(map.keySet());

        // Verify case sensitivity using first actual key
        if (expectedSize > 0) {
            String actualKey = keys.get(0);
            String variantKey = actualKey.toLowerCase().equals(actualKey) ?
                    actualKey.toUpperCase() : actualKey.toLowerCase();

            if (!caseSensitive) {
                assertTrue(map.containsKey(variantKey));
            } else {
                assertFalse(map.containsKey(variantKey));
            }
        }

        // Verify ordering if size > 1
        if (expectedSize > 1) {
            if (reverse) {
                assertTrue(keys.get(0).compareToIgnoreCase(keys.get(1)) > 0);
            } else {
                assertTrue(keys.get(0).compareToIgnoreCase(keys.get(1)) < 0);
            }
        }
    }
    
    private void verifySequenceMapBehavior(CompactMap<String, String> map, boolean caseSensitive) {
        List<String> insertOrder = Arrays.asList("C", "A", "B", "D");
        for (String key : insertOrder) {
            map.put(key, key.toLowerCase());
            // Verify insertion order is maintained
            assertEquals(insertOrder.subList(0, map.size()), new ArrayList<>(map.keySet()));
            // Verify case sensitivity
            if (!caseSensitive) {
                assertTrue(map.containsKey(key.toLowerCase()));
            }
        }
    }

    private void verifyUnorderedMapBehavior(CompactMap<String, String> map, boolean caseSensitive) {
        map.put("A", "alpha");
        map.put("B", "bravo");
        map.put("C", "charlie");
        map.put("D", "delta");

        // Only verify size and case sensitivity for unordered maps
        assertEquals(4, map.size());
        if (!caseSensitive) {
            assertTrue(map.containsKey("a"));
            assertTrue(map.containsKey("A"));
        } else {
            if (map.containsKey("A")) assertFalse(map.containsKey("a"));
            if (map.containsKey("a")) assertFalse(map.containsKey("A"));
        }
    }
}
