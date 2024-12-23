package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompactMapBuilderConfigTest {
    private static final int TEST_COMPACT_SIZE = 3;

    @Test
    public void testBuilderCompactSizeTransitions() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(TEST_COMPACT_SIZE)
                .mapType(HashMap.class)
                .build();

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
        assertTrue(map.val instanceof Map);
    }

    @Test
    public void testBuilderReverseCaseSensitive() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(TEST_COMPACT_SIZE)
                .mapType(TreeMap.class)
                .reverseOrder()
                .caseSensitive(true)
                .build();

        verifyMapBehavior(map, true, true); // reverse=true, caseSensitive=true
    }

    @Test
    public void testBuilderReverseCaseInsensitive() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(TEST_COMPACT_SIZE)
                .mapType(TreeMap.class)
                .reverseOrder()
                .caseSensitive(false)
                .comparator(String.CASE_INSENSITIVE_ORDER)
                .build();

        verifyMapBehavior(map, true, false); // reverse=true, caseSensitive=false
    }

    @Test
    public void testBuilderSortedCaseSensitive() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(TEST_COMPACT_SIZE)
                .mapType(TreeMap.class)
                .sortedOrder()
                .caseSensitive(true)
                .build();

        verifyMapBehavior(map, false, true); // reverse=false, caseSensitive=true
    }

    @Test
    public void testBuilderSortedCaseInsensitive() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(TEST_COMPACT_SIZE)
                .mapType(TreeMap.class)
                .sortedOrder()
                .caseSensitive(false)
                .comparator(String.CASE_INSENSITIVE_ORDER)
                .build();

        verifyMapBehavior(map, false, false); // reverse=false, caseSensitive=false
    }

    @Test
    public void testBuilderSequenceCaseSensitive() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(TEST_COMPACT_SIZE)
                .mapType(LinkedHashMap.class)
                .insertionOrder()
                .caseSensitive(true)
                .build();

        verifySequenceMapBehavior(map, true); // caseSensitive=true
    }

    @Test
    public void testBuilderSequenceCaseInsensitive() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(TEST_COMPACT_SIZE)
                .mapType(LinkedHashMap.class)
                .insertionOrder()
                .caseSensitive(false)
                .build();

        verifySequenceMapBehavior(map, false); // caseSensitive=false
    }

    @Test
    public void testBuilderUnorderedCaseSensitive() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(TEST_COMPACT_SIZE)
                .mapType(HashMap.class)
                .noOrder()
                .caseSensitive(true)
                .build();

        verifyUnorderedMapBehavior(map, true); // caseSensitive=true
    }

    @Test
    public void testBuilderUnorderedCaseInsensitive() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(TEST_COMPACT_SIZE)
                .mapType(HashMap.class)
                .noOrder()
                .caseSensitive(false)
                .build();

        verifyUnorderedMapBehavior(map, false); // caseSensitive=false
    }

    @Test
    public void testInvalidMapTypeOrdering() {
        // HashMap doesn't support sorted order
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                CompactMap.builder()
                        .mapType(HashMap.class)
                        .sortedOrder()
                        .build()
        );

        assertEquals("Map type HashMap is not compatible with ordering 'sorted'", exception.getMessage());
    }

    @Test
    public void testAutoDetectDescendingOrder() {
        // Create a custom map class name that includes "descending" to test the auto-detection
        class DescendingTreeMap<K,V> extends TreeMap<K,V> { }

        // We need to pass in our own options map to verify what's being set
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.MAP_TYPE, DescendingTreeMap.class);

        // Create the map using the options directly
        CompactMap.validateAndFinalizeOptions(options);

        // Verify that the ORDERING was set to REVERSE due to "descending" in class name
        assertEquals(CompactMap.REVERSE, options.get(CompactMap.ORDERING));
    }

    @Test
    public void testAutoDetectReverseOrder() {
        // Create a custom map class name that includes "reverse" to test the auto-detection
        class ReverseTreeMap<K,V> extends TreeMap<K,V> { }

        // Create options map to verify what's being set
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.MAP_TYPE, ReverseTreeMap.class);

        // Create the map using the options directly
        CompactMap.validateAndFinalizeOptions(options);

        // Verify that the ORDERING was set to REVERSE due to "reverse" in class name
        assertEquals(CompactMap.REVERSE, options.get(CompactMap.ORDERING));
    }
    
    @Test
    public void testDescendingOrderWithComparator() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .mapType(TreeMap.class)
                .reverseOrder()
                .comparator(Collections.reverseOrder())
                .build();

        map.put("C", "charlie");
        map.put("A", "alpha");
        map.put("B", "bravo");

        List<String> keys = new ArrayList<>(map.keySet());
        assertTrue(keys.get(0).compareToIgnoreCase(keys.get(1)) > 0);
        assertTrue(keys.get(1).compareToIgnoreCase(keys.get(2)) > 0);
    }

    @Test
    public void testAutoDetectSortedOrder() {
        // Create a custom sorted map that doesn't have "reverse" or "descending" in name
        class CustomSortedMap<K,V> extends TreeMap<K,V> { }

        // Create options map to verify what's being set
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.MAP_TYPE, CustomSortedMap.class);

        // Create the map using the options directly
        CompactMap.validateAndFinalizeOptions(options);

        // Verify that the ORDERING was set to SORTED since it's a SortedMap without reverse/descending in name
        assertEquals(CompactMap.SORTED, options.get(CompactMap.ORDERING));
    }

    @Test
    public void testDefaultMapTypeForSortedOrder() {
        // Create options map without specifying a map type
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.ORDERING, CompactMap.SORTED);

        // Create the map using the options directly
        CompactMap.validateAndFinalizeOptions(options);

        // Verify that TreeMap was chosen as the default map type for sorted ordering
        assertEquals(TreeMap.class, options.get(CompactMap.MAP_TYPE));
    }

    @Test
    public void testIdentityHashMapRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                CompactMap.builder()
                        .mapType(IdentityHashMap.class)
                        .build()
        );

        assertEquals("IdentityHashMap is not supported as it compares keys by reference identity",
                exception.getMessage());
    }

    @Test
    public void testWeakHashMapRejected() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                CompactMap.builder()
                        .mapType(WeakHashMap.class)
                        .build()
        );

        assertEquals("WeakHashMap is not supported as it can unpredictably remove entries",
                exception.getMessage());
    }

    @Test
    public void testReverseOrderWithCaseInsensitiveStrings() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .caseSensitive(false)  // Enable case-insensitive mode
                .reverseOrder()        // Request reverse ordering
                .build();

        // Add mixed-case strings
        map.put("Alpha", "value1");
        map.put("alpha", "value2");
        map.put("BETA", "value3");
        map.put("beta", "value4");
        map.put("CHARLIE", "value5");
        map.put("charlie", "value6");

        // Get keys to verify ordering
        List<String> keys = new ArrayList<>(map.keySet());

        // Should be in reverse alphabetical order, case-insensitively
        assertEquals(3, keys.size());

        // Verify reverse alphabetical order
        assertEquals("CHARLIE", keys.get(0));
        assertEquals("BETA", keys.get(1));
        assertEquals("alpha", keys.get(2));

        // Test that it works with CaseInsensitiveString instances too
        CaseInsensitiveMap.CaseInsensitiveString cisKey =
                new CaseInsensitiveMap.CaseInsensitiveString("DELTA");
        map.put(cisKey.toString(), "value7");

        keys = new ArrayList<>(map.keySet());
        assertEquals(4, keys.size());

        // Verify complete reverse alphabetical order after adding DELTA
        assertEquals("DELTA", keys.get(0));
        assertEquals("CHARLIE", keys.get(1));
        assertEquals("BETA", keys.get(2));
        assertEquals("alpha", keys.get(3));
    }

    @Test
    public void testReverseOrderCaseInsensitiveNullComparator() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .reverseOrder()
                .caseSensitive(false)
                .mapType(TreeMap.class)
                .build();

        // Add strings in non-reverse order
        map.put("AAA", "value1");
        map.put("BBB", "value2");
        map.put("CCC", "value3");

        List<String> keys = new ArrayList<>(map.keySet());

        // In reverse order, CCC should be first, then BBB, then AAA
        assertEquals(3, keys.size());
        assertEquals("CCC", keys.get(0));
        assertEquals("BBB", keys.get(1));
        assertEquals("AAA", keys.get(2));

        // Test case insensitivity
        assertTrue(map.containsKey("aaa"));
        assertTrue(map.containsKey("bbb"));
        assertTrue(map.containsKey("ccc"));

        // Add a mixed case key
        map.put("DdD", "value4");
        keys = new ArrayList<>(map.keySet());
        assertEquals("DdD", keys.get(0)); // Should be first in reverse order
    }

    @Test
    public void testReverseOrderWithCaseInsensitiveString() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .reverseOrder()
                .caseSensitive(false)
                .mapType(TreeMap.class)
                .build();

        CaseInsensitiveMap.CaseInsensitiveString cisKey =
                new CaseInsensitiveMap.CaseInsensitiveString("BBB");
        map.put(cisKey.toString(), "value1");
        map.put("AAA", "value2");
        map.put("CCC", "value3");

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(3, keys.size());
        assertEquals("CCC", keys.get(0));
        assertEquals("BBB", keys.get(1));
        assertEquals("AAA", keys.get(2));
    }

    @Test
    public void testReverseOrderNullComparatorCreation() {
        Map<String, Object> options = new HashMap<>();
        // Set up the exact conditions needed:
        // 1. ORDERING must be REVERSE
        // 2. CASE_SENSITIVE must be false
        // 3. COMPARATOR must be null
        // 4. No other conditions should trigger comparator creation before our target code
        options.put(CompactMap.ORDERING, CompactMap.REVERSE);
        options.put(CompactMap.CASE_SENSITIVE, false);
        options.remove(CompactMap.COMPARATOR);

        // Don't set MAP_TYPE to avoid other comparator creation paths

        CompactMap.validateAndFinalizeOptions(options);

        // Get and test the created comparator
        @SuppressWarnings("unchecked")
        Comparator<Object> comparator = (Comparator<Object>) options.get(CompactMap.COMPARATOR);

        // Test the comparator with both String and CaseInsensitiveString
        CaseInsensitiveMap.CaseInsensitiveString cis =
                new CaseInsensitiveMap.CaseInsensitiveString("BBB");

        // In reverse order, BBB should be greater than AAA
        assertTrue(comparator.compare(cis, "AAA") < 0);
        assertTrue(comparator.compare("BBB", "AAA") < 0);

        // Case insensitive check
        assertEquals(0, comparator.compare(cis, "bbb"));
        assertEquals(0, comparator.compare("BBB", "bbb"));
    }

    @Test
    public void testReverseOrderComparatorCreation() {
        Map<String, Object> options = new HashMap<>();

        // Important sequence:
        // 1. Set TreeMap as map type since we need a SortedMap for reverse ordering
        options.put(CompactMap.MAP_TYPE, TreeMap.class);

        // 2. Set case insensitive without a comparator
        options.put(CompactMap.CASE_SENSITIVE, false);
        options.put(CompactMap.ORDERING, CompactMap.REVERSE);

        // 3. Explicitly ensure no comparator
        options.remove(CompactMap.COMPARATOR);

        // Before validation, verify our preconditions
        assertNull(options.get(CompactMap.COMPARATOR));
        assertEquals(false, options.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.REVERSE, options.get(CompactMap.ORDERING));

        CompactMap.validateAndFinalizeOptions(options);

        // Get and test the comparator
        @SuppressWarnings("unchecked")
        Comparator<Object> comparator = (Comparator<Object>) options.get(CompactMap.COMPARATOR);
        assertNotNull(comparator);

        // Test both case sensitivity and reverse ordering
        CaseInsensitiveMap.CaseInsensitiveString cisKey =
                new CaseInsensitiveMap.CaseInsensitiveString("BBB");

        // Test case insensitivity
        int equalityResult = comparator.compare(cisKey, "bbb");
        assertEquals(0, equalityResult,
                "BBB and bbb should be equal (got comparison result: " + equalityResult + ")");

        // Test reverse ordering
        int reverseResult = comparator.compare("BBB", "AAA");
        assertTrue(reverseResult < 0,
                "BBB should be less than AAA in reverse order (got comparison result: " + reverseResult + ")");
    }

    @Test
    public void testComparatorCreationFlow() {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.MAP_TYPE, TreeMap.class);
        options.put(CompactMap.CASE_SENSITIVE, false);
        options.put(CompactMap.ORDERING, CompactMap.REVERSE);

        // Verify initial state
        assertNull(options.get(CompactMap.COMPARATOR));
        assertFalse((Boolean)options.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.REVERSE, options.get(CompactMap.ORDERING));

        // This call will hit the first block and set the comparator
        CompactMap.validateAndFinalizeOptions(options);

        // Verify final state - comparator should be non-null
        assertNotNull(options.get(CompactMap.COMPARATOR));
    }

    @Test
    public void testSourceMapOrderingConflict() {
        // Create a TreeMap (naturally sorted) as the source
        TreeMap<String, String> sourceMap = new TreeMap<>();
        sourceMap.put("A", "value1");
        sourceMap.put("B", "value2");

        // Create options requesting REVERSE ordering with a SORTED source map
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.SOURCE_MAP, sourceMap);  // SORTED source map
        options.put(CompactMap.ORDERING, CompactMap.REVERSE);  // Conflicting REVERSE order request

        // This should throw IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                CompactMap.validateAndFinalizeOptions(options)
        );

        // Verify the exact error message
        String expectedMessage = "Requested ordering 'reverse' conflicts with source map's ordering 'sorted'. " +
                "Map structure: " + MapUtilities.getMapStructureString(sourceMap);
        assertEquals(expectedMessage, exception.getMessage());
    }

    // Static inner class that tracks capacity
    private static class CapacityTrackingHashMap<K,V> extends HashMap<K,V> {
        private static int lastCapacityUsed;

        public CapacityTrackingHashMap() {
            super();
        }

        public CapacityTrackingHashMap(int initialCapacity) {
            super(initialCapacity);
            lastCapacityUsed = initialCapacity;
        }

        public static int getLastCapacityUsed() {
            return lastCapacityUsed;
        }
    }

    @Test
    public void testFactoryCompactMapCapacityMethodCalled() {
        // Create CompactMap with custom settings
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.COMPACT_SIZE, 2);  // Small compact size to force transition
        options.put(CompactMap.CAPACITY, 42);     // Custom capacity
        options.put(CompactMap.MAP_TYPE, CompactMapBuilderConfigTest.CapacityTrackingHashMap.class);

        CompactMap<String, String> map = CompactMap.newMap(options);

        // Add entries to force creation of backing map
        map.put("A", "1");
        map.put("B", "2");
        map.put("C", "3");  // This should trigger backing map creation

        // Verify the capacity was used when creating the HashMap
        assertEquals(42, CompactMapBuilderConfigTest.CapacityTrackingHashMap.getLastCapacityUsed(),
                "Backing map was not created with the expected capacity");
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