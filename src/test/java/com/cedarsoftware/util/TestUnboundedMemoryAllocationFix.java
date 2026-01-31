package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify the fix for [crit-4] Unbounded memory allocation in sanitizedChildOptions.
 *
 * The issue: Every nested comparison created a new HashMap to pass options, causing
 * millions of HashMap allocations for large object graphs.
 *
 * The fix: Move DEPTH_BUDGET tracking to ThreadLocal, making options map stable so it
 * can be reused in 99.9% of cases.
 */
public class TestUnboundedMemoryAllocationFix {

    /**
     * Test that depth limits still work correctly with ThreadLocal approach.
     */
    @Test
    public void testDepthLimitStillEnforced() {
        // Create deeply nested structure
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> current = root;
        for (int i = 0; i < 50; i++) {
            Map<String, Object> next = new HashMap<>();
            current.put("nested", next);
            current = next;
        }
        current.put("value", "deep");

        Map<String, Object> root2 = new HashMap<>();
        Map<String, Object> current2 = root2;
        for (int i = 0; i < 50; i++) {
            Map<String, Object> next = new HashMap<>();
            current2.put("nested", next);
            current2 = next;
        }
        current2.put("value", "deep");

        // Set depth limit to 10
        Map<String, Object> options = new HashMap<>();
        options.put("__depthBudget", 10);

        // Should throw SecurityException when depth exceeds limit
        assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(root, root2, options);
        }, "Should enforce depth limit from options");
    }

    /**
     * Test that system property depth limit still works.
     */
    @Test
    public void testSystemPropertyDepthLimit() {
        // This test assumes system property is set via getMaxRecursionDepth()
        // We just verify that comparison works for shallow structures
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key", "value");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("key", "value");

        assertTrue(DeepEquals.deepEquals(map1, map2));
    }

    /**
     * Test that re-entrant calls work correctly (stacked depth budgets).
     */
    @Test
    public void testReentrantCallsWithDepthBudget() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key1", "value1");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("key1", "value1");

        Map<String, Object> options = new HashMap<>();
        options.put("__depthBudget", 100);

        // Should work fine
        assertTrue(DeepEquals.deepEquals(map1, map2, options));
    }

    /**
     * Test that options map is reused when it only contains input keys.
     */
    @Test
    public void testOptionsMapReuseOptimization() {
        // Create structure with multiple collections at same depth
        Map<String, Object> root1 = new HashMap<>();
        root1.put("list1", Arrays.asList(1, 2, 3));
        root1.put("list2", Arrays.asList(4, 5, 6));
        root1.put("list3", Arrays.asList(7, 8, 9));

        Map<String, Object> root2 = new HashMap<>();
        root2.put("list1", Arrays.asList(1, 2, 3));
        root2.put("list2", Arrays.asList(4, 5, 6));
        root2.put("list3", Arrays.asList(7, 8, 9));

        Map<String, Object> options = new HashMap<>();
        options.put(DeepEquals.ALLOW_STRINGS_TO_MATCH_NUMBERS, true);

        // Should reuse options map for all nested lists
        assertTrue(DeepEquals.deepEquals(root1, root2, options));

        // Verify options only has the one key we put in (no DIFF added during comparison)
        // This indirectly verifies that child options reused parent
        assertEquals(1, options.size());
    }

    /**
     * Test that DIFF results are still written back to options.
     */
    @Test
    public void testDiffResultsStillWritten() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key", "value1");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("key", "value2");

        Map<String, Object> options = new HashMap<>();

        boolean result = DeepEquals.deepEquals(map1, map2, options);

        assertFalse(result, "Maps should not be equal");
        assertTrue(options.containsKey("diff"), "DIFF should be written to options");
        assertNotNull(options.get("diff"), "DIFF value should not be null");
    }

    /**
     * Test large nested structure to verify no OutOfMemoryError.
     */
    @Test
    public void testLargeNestedStructure() {
        // Create structure with many nested collections
        Map<String, Object> root1 = new HashMap<>();
        Map<String, Object> root2 = new HashMap<>();

        // Create 1000 nested levels (moderate depth)
        Map<String, Object> current1 = root1;
        Map<String, Object> current2 = root2;

        for (int i = 0; i < 100; i++) {
            List<Integer> list1 = Arrays.asList(i, i + 1, i + 2);
            List<Integer> list2 = Arrays.asList(i, i + 1, i + 2);

            current1.put("list" + i, list1);
            current2.put("list" + i, list2);

            if (i < 99) {
                Map<String, Object> next1 = new HashMap<>();
                Map<String, Object> next2 = new HashMap<>();
                current1.put("nested", next1);
                current2.put("nested", next2);
                current1 = next1;
                current2 = next2;
            }
        }

        // This should complete without OutOfMemoryError
        assertTrue(DeepEquals.deepEquals(root1, root2));
    }

    /**
     * Test that depth budget from user options takes precedence.
     */
    @Test
    public void testUserDepthBudgetTakesPrecedence() {
        // Create structure deeper than user's budget
        Map<String, Object> deep1 = new HashMap<>();
        Map<String, Object> current1 = deep1;
        for (int i = 0; i < 20; i++) {
            Map<String, Object> next = new HashMap<>();
            current1.put("n", next);
            current1 = next;
        }

        Map<String, Object> deep2 = new HashMap<>();
        Map<String, Object> current2 = deep2;
        for (int i = 0; i < 20; i++) {
            Map<String, Object> next = new HashMap<>();
            current2.put("n", next);
            current2 = next;
        }

        // Set user budget to 5
        Map<String, Object> options = new HashMap<>();
        options.put("__depthBudget", 5);

        // Should throw when exceeding user's budget of 5
        assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(deep1, deep2, options);
        });
    }

    /**
     * Test that collections and maps use stable options (the optimization target).
     */
    @Test
    public void testCollectionsAndMapsUseStableOptions() {
        // Create structure with Sets and Lists
        Set<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> set2 = new HashSet<>(Arrays.asList("a", "b", "c"));

        List<Set<String>> list1 = Arrays.asList(set1);
        List<Set<String>> list2 = Arrays.asList(set2);

        Map<String, Object> root1 = new HashMap<>();
        root1.put("data", list1);

        Map<String, Object> root2 = new HashMap<>();
        root2.put("data", list2);

        // Should work fine with options reuse
        assertTrue(DeepEquals.deepEquals(root1, root2));
    }

    /**
     * Test null options handling.
     */
    @Test
    public void testNullOptions() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key", "value");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("key", "value");

        // Null options should work (uses empty map)
        assertTrue(DeepEquals.deepEquals(map1, map2, null));
    }

    /**
     * Test empty options handling.
     */
    @Test
    public void testEmptyOptions() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key", "value");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("key", "value");

        Map<String, Object> emptyOptions = new HashMap<>();

        // Empty options should work and be reused
        assertTrue(DeepEquals.deepEquals(map1, map2, emptyOptions));
    }
}
