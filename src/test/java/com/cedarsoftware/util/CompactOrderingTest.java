package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests focusing on CompactMap's ordering behavior and storage transitions.
 */
class CompactOrderingTest {
    private static final int COMPACT_SIZE = 3;

    // Test data
    private static final String[] MIXED_CASE_KEYS = {"Apple", "banana", "CHERRY", "Date"};
    private static final Integer[] VALUES = {1, 2, 3, 4};

    @ParameterizedTest
    @MethodSource("sizeThresholdScenarios")
    void testDefaultCaseInsensitiveWithNoComparator(int itemCount, String[] inputs, String[] expectedOrder) {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.COMPACT_SIZE, COMPACT_SIZE);
        options.put(CompactMap.ORDERING, CompactMap.SORTED);
        options.put(CompactMap.CASE_SENSITIVE, false);
        options.put(CompactMap.MAP_TYPE, TreeMap.class);
        Map<String, Integer> map = CompactMap.newMap(options);

        // Add items and verify order after each addition
        for (int i = 0; i < itemCount; i++) {
            map.put(inputs[i], i);
            String[] expectedSubset = Arrays.copyOfRange(expectedOrder, 0, i + 1);
            assertArrayEquals(expectedSubset, map.keySet().toArray(new String[0]),
                    String.format("Order mismatch with %d items", i + 1));
        }
    }

    @ParameterizedTest
    @MethodSource("customComparatorScenarios")
    void testCaseSensitivityIgnoredWithCustomComparator(int itemCount, String[] inputs, String[] expectedOrder) {
        Comparator<String> lengthThenAlpha = Comparator
                .comparingInt(String::length)
                .thenComparing(String::compareTo);

        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.COMPACT_SIZE, COMPACT_SIZE);
        options.put(CompactMap.ORDERING, CompactMap.SORTED);
        options.put(CompactMap.CASE_SENSITIVE, false);  // Should be ignored
        options.put(CompactMap.COMPARATOR, lengthThenAlpha);
        options.put(CompactMap.MAP_TYPE, TreeMap.class);
        Map<String, Integer> map = CompactMap.newMap(options);

        // Add items and verify order after each addition
        for (int i = 0; i < itemCount; i++) {
            map.put(inputs[i], i);
            String[] expectedSubset = Arrays.copyOfRange(expectedOrder, 0, i + 1);
            assertArrayEquals(expectedSubset, map.keySet().toArray(new String[0]),
                    String.format("Order mismatch with %d items", i + 1));
        }
    }

    @ParameterizedTest
    @MethodSource("reverseSortedScenarios")
    void testCaseInsensitiveReverseSorted(int itemCount, String[] inputs, String[] expectedOrder) {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.COMPACT_SIZE, COMPACT_SIZE);
        options.put(CompactMap.ORDERING, CompactMap.REVERSE);
        options.put(CompactMap.CASE_SENSITIVE, false);
        options.put(CompactMap.MAP_TYPE, TreeMap.class);
        Map<String, Integer> map = CompactMap.newMap(options); // Ensure a new map per test scenario

        // Add items and verify order after each addition
        for (int i = 0; i < itemCount; i++) {
            map.put(inputs[i], i);
            String[] expectedSubset = Arrays.copyOfRange(expectedOrder, 0, i + 1);
            System.out.println("After inserting '" + inputs[i] + "': " + map.keySet());
            System.out.println("Expected order: " + Arrays.toString(expectedSubset));
            assertArrayEquals(expectedSubset, map.keySet().toArray(new String[0]),
                    String.format("Order mismatch with %d items", i + 1));
        }
    }

    @Test
    void testRemovalsBetweenStorageTypes() {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.COMPACT_SIZE, COMPACT_SIZE);
        options.put(CompactMap.ORDERING, CompactMap.SORTED);
        options.put(CompactMap.CASE_SENSITIVE, false);
        options.put(CompactMap.MAP_TYPE, TreeMap.class);
        Map<String, Integer> map = CompactMap.newMap(options);

        // Add all entries first
        String[] inputs = {"Dog", "cat", "BIRD", "fish"};
        for (String input : inputs) {
            map.put(input, 1);
        }

        // Now at size 4 (Map storage) - verify order
        assertArrayEquals(new String[]{"BIRD", "cat", "Dog", "fish"},
                map.keySet().toArray(new String[0]), "Initial map order incorrect");

        // Remove to size 3 (should switch to compact array)
        map.remove("fish");
        assertArrayEquals(new String[]{"BIRD", "cat", "Dog"},
                map.keySet().toArray(new String[0]), "Order after removal to size 3 incorrect");

        // Remove to size 2
        map.remove("Dog");
        assertArrayEquals(new String[]{"BIRD", "cat"},
                map.keySet().toArray(new String[0]), "Order after removal to size 2 incorrect");

        // Remove to size 1
        map.remove("cat");
        assertArrayEquals(new String[]{"BIRD"},
                map.keySet().toArray(new String[0]), "Order after removal to size 1 incorrect");

        // Add back to verify ordering is maintained during growth
        map.put("cat", 1);
        assertArrayEquals(new String[]{"BIRD", "cat"},
                map.keySet().toArray(new String[0]), "Order after adding back to size 2 incorrect");

        map.put("Dog", 1);
        assertArrayEquals(new String[]{"BIRD", "cat", "Dog"},
                map.keySet().toArray(new String[0]), "Order after adding back to size 3 incorrect");
    }

    @Test
    void testClearAndRebuildWithSortedOrder() {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.COMPACT_SIZE, COMPACT_SIZE);
        options.put(CompactMap.ORDERING, CompactMap.SORTED);
        options.put(CompactMap.MAP_TYPE, TreeMap.class);
        Map<String, Integer> map = CompactMap.newMap(options);

        // Fill past compact size
        for (int i = 0; i < MIXED_CASE_KEYS.length; i++) {
            map.put(MIXED_CASE_KEYS[i], VALUES[i]);
        }

        // Clear and verify empty
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // Rebuild and verify ordering maintained
        for (int i = 0; i < COMPACT_SIZE; i++) {
            map.put(MIXED_CASE_KEYS[i], VALUES[i]);
        }

        String[] expectedOrder = {"Apple", "CHERRY", "banana"};
        assertArrayEquals(expectedOrder, map.keySet().toArray(new String[0]));
    }

    @Test
    void testClearAndRebuildWithInsertionOrder() {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.COMPACT_SIZE, COMPACT_SIZE);
        options.put(CompactMap.ORDERING, CompactMap.INSERTION);
        options.put(CompactMap.MAP_TYPE, LinkedHashMap.class);
        Map<String, Integer> map = CompactMap.newMap(options);

        // Fill past compact size
        for (int i = 0; i < MIXED_CASE_KEYS.length; i++) {
            map.put(MIXED_CASE_KEYS[i], VALUES[i]);
        }

        // Clear and verify empty
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // Rebuild and verify ordering maintained
        for (int i = 0; i < COMPACT_SIZE; i++) {
            map.put(MIXED_CASE_KEYS[i], VALUES[i]);
        }

        String[] expectedOrder = {"Apple", "banana", "CHERRY"};
        assertArrayEquals(expectedOrder, map.keySet().toArray(new String[0]));
    }

    @Test
    void testInsertionOrderPreservationDuringTransition() {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.COMPACT_SIZE, COMPACT_SIZE);
        options.put(CompactMap.ORDERING, CompactMap.INSERTION);
        options.put(CompactMap.MAP_TYPE, LinkedHashMap.class);
        Map<String, Integer> map = CompactMap.newMap(options);

        // Add entries one by one and verify order
        for (int i = 0; i < MIXED_CASE_KEYS.length; i++) {
            map.put(MIXED_CASE_KEYS[i], VALUES[i]);
            String[] expectedOrder = Arrays.copyOfRange(MIXED_CASE_KEYS, 0, i + 1);
            assertArrayEquals(expectedOrder, map.keySet().toArray(new String[0]),
                    String.format("Order mismatch with %d items", i + 1));
        }
    }

    @Test
    void testUnorderedBehavior() {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.COMPACT_SIZE, COMPACT_SIZE);
        options.put(CompactMap.ORDERING, CompactMap.UNORDERED);
        options.put(CompactMap.MAP_TYPE, HashMap.class);
        Map<String, Integer> map = CompactMap.newMap(options);

        // Add entries and verify contents (not order)
        for (int i = 0; i < MIXED_CASE_KEYS.length; i++) {
            map.put(MIXED_CASE_KEYS[i], VALUES[i]);
            assertEquals(i + 1, map.size(), "Size mismatch after adding item " + (i + 1));

            // Verify all added items are present
            for (int j = 0; j <= i; j++) {
                assertTrue(map.containsKey(MIXED_CASE_KEYS[j]),
                        "Missing key " + MIXED_CASE_KEYS[j] + " after adding " + (i + 1) + " items");
                assertEquals(VALUES[j], map.get(MIXED_CASE_KEYS[j]),
                        "Incorrect value for key " + MIXED_CASE_KEYS[j]);
            }
        }
    }

    @Test
    void minimalTestCaseInsensitiveReverseSorted() {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.COMPACT_SIZE, 80);
        options.put(CompactMap.ORDERING, CompactMap.REVERSE);
        options.put(CompactMap.CASE_SENSITIVE, false);
        options.put(CompactMap.MAP_TYPE, TreeMap.class);
        Map<String, Integer> map = CompactMap.newMap(options);

        // Insert "DDD"
        map.put("DDD", 0);
        System.out.println("After inserting 'DDD': " + map.keySet());
        assertArrayEquals(new String[]{"DDD"}, map.keySet().toArray(new String[0]),
                "Order mismatch after inserting 'DDD'");
    }

    @Test
    void focusedReverseCaseInsensitiveTest() {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.COMPACT_SIZE, 80);
        options.put(CompactMap.ORDERING, CompactMap.REVERSE);
        options.put(CompactMap.CASE_SENSITIVE, false);
        options.put(CompactMap.MAP_TYPE, TreeMap.class);
        Map<String, Integer> map = CompactMap.newMap(options);

        // Insert multiple keys
        map.put("aaa", 0);
        System.out.println("After inserting 'aaa': " + map.keySet());
        map.put("BBB", 1);
        System.out.println("After inserting 'BBB': " + map.keySet());
        map.put("ccc", 2);
        System.out.println("After inserting 'ccc': " + map.keySet());
        map.put("DDD", 3);
        System.out.println("After inserting 'DDD': " + map.keySet());

        // Expected Order: DDD, ccc, BBB, aaa
        String[] expectedOrder = {"DDD", "ccc", "BBB", "aaa"};
        assertArrayEquals(expectedOrder, map.keySet().toArray(new String[0]),
                "Order mismatch after multiple insertions");
    }

    private static Stream<Arguments> sizeThresholdScenarios() {
        String[] inputs = {"apple", "BANANA", "Cherry", "DATE"};
        String[] expectedOrder = {"apple", "BANANA", "Cherry", "DATE"};
        return Stream.of(
                Arguments.of(1, inputs, expectedOrder),
                Arguments.of(2, inputs, expectedOrder),
                Arguments.of(3, inputs, expectedOrder),
                Arguments.of(4, inputs, expectedOrder)
        );
    }

    private static Stream<Arguments> customComparatorScenarios() {
        String[] inputs = {"D", "BB", "aaa", "cccc"};
        String[] expectedOrder = {"D", "BB", "aaa", "cccc"};
        return Stream.of(
                Arguments.of(1, inputs, expectedOrder),
                Arguments.of(2, inputs, expectedOrder),
                Arguments.of(3, inputs, expectedOrder),
                Arguments.of(4, inputs, expectedOrder)
        );
    }

    private static Stream<Arguments> reverseSortedScenarios() {
        String[] allInputs = {"aaa", "BBB", "ccc", "DDD"};
        Comparator<String> reverseCaseInsensitiveComparator = (s1, s2) -> String.CASE_INSENSITIVE_ORDER.compare(s2, s1);

        return Stream.of(1, 2, 3, 4)
                .map(itemCount -> {
                    String[] currentInputs = Arrays.copyOfRange(allInputs, 0, itemCount);
                    String[] currentExpectedOrder = Arrays.copyOf(currentInputs, itemCount);
                    Arrays.sort(currentExpectedOrder, reverseCaseInsensitiveComparator);
                    return Arguments.of(itemCount, currentInputs, currentExpectedOrder);
                });
    }
}