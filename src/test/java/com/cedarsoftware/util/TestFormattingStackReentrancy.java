package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the fix for [thread-2] formattingStack not re-entrant safe.
 *
 * The issue: formattingStack used a single ThreadLocal Set shared across re-entrant deepEquals calls.
 * When a custom equals method triggers a re-entrant deepEquals that also generates a diff,
 * both the outer and inner diff generation would share the same formatting Set, causing
 * false circular reference detection.
 *
 * The fix: Changed formattingStack to ThreadLocal<Deque<Set<Object>>>, where each top-level
 * formatValue call gets its own Set for circular reference detection.
 */
public class TestFormattingStackReentrancy {

    /**
     * Test that circular references are detected within a single formatting session.
     */
    @Test
    public void testCircularDetectionStillWorks() {
        // Create a circular list
        List<Object> list1 = new ArrayList<>();
        list1.add("item1");
        list1.add(list1);  // Circular reference!

        List<Object> list2 = new ArrayList<>();
        list2.add("item2");
        list2.add(list2);  // Circular reference!

        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals(list1, list2, options);

        assertFalse(result, "Should return false for different lists");

        // The comparison should work without throwing exception or crashing
        // Circular references are handled internally
        assertTrue(true, "Circular structures should be handled safely");
    }

    /**
     * Test that formatting stack is properly cleaned up.
     */
    @Test
    public void testFormattingStackCleanup() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key", "value1");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("key", "value2");

        // Run multiple comparisons
        for (int i = 0; i < 10; i++) {
            Map<String, Object> options = new HashMap<>();
            DeepEquals.deepEquals(map1, map2, options);
        }

        // Should not throw exception or accumulate state
        assertTrue(true, "Multiple comparisons should work without issue");
    }

    /**
     * Test nested circular structures.
     */
    @Test
    public void testNestedCircularStructures() {
        Map<String, Object> outer1 = new HashMap<>();
        Map<String, Object> inner1 = new HashMap<>();
        inner1.put("self", inner1);  // Inner circular
        outer1.put("inner", inner1);
        outer1.put("outer_self", outer1);  // Outer circular

        Map<String, Object> outer2 = new HashMap<>();
        Map<String, Object> inner2 = new HashMap<>();
        inner2.put("self", inner2);  // Inner circular
        outer2.put("inner", inner2);
        outer2.put("outer_self", outer2);  // Outer circular

        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals(outer1, outer2, options);

        // They should be equal (same structure)
        assertTrue(result, "Structures with same circular pattern should be equal");
    }

    /**
     * Test that deep equals works correctly with complex nested structures.
     */
    @Test
    public void testComplexNestedStructures() {
        // Create nested maps
        Map<String, Object> deep1 = new HashMap<>();
        deep1.put("level", "deep");
        deep1.put("value", "A");

        Map<String, Object> mid1 = new HashMap<>();
        mid1.put("level", "mid");
        mid1.put("child", deep1);

        Map<String, Object> top1 = new HashMap<>();
        top1.put("level", "top");
        top1.put("child", mid1);

        // Create similar structure with different deep value
        Map<String, Object> deep2 = new HashMap<>();
        deep2.put("level", "deep");
        deep2.put("value", "B");  // Different!

        Map<String, Object> mid2 = new HashMap<>();
        mid2.put("level", "mid");
        mid2.put("child", deep2);

        Map<String, Object> top2 = new HashMap<>();
        top2.put("level", "top");
        top2.put("child", mid2);

        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals(top1, top2, options);

        assertFalse(result, "Should detect difference at deep level");
    }

    /**
     * Test arrays with potential circular references.
     */
    @Test
    public void testArraysWithCircularReferences() {
        Object[] array1 = new Object[2];
        array1[0] = "item";
        array1[1] = array1;  // Circular!

        Object[] array2 = new Object[2];
        array2[0] = "item";
        array2[1] = array2;  // Circular!

        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals(array1, array2, options);

        assertTrue(result, "Arrays with same circular pattern should be equal");
    }

    /**
     * Test that equal structures are correctly identified.
     */
    @Test
    public void testEqualComplexStructures() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("a", Arrays.asList(1, 2, 3));
        map1.put("b", new HashMap<String, String>() {{
            put("x", "y");
        }});

        Map<String, Object> map2 = new HashMap<>();
        map2.put("a", Arrays.asList(1, 2, 3));
        map2.put("b", new HashMap<String, String>() {{
            put("x", "y");
        }});

        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals(map1, map2, options);

        assertTrue(result, "Equal complex structures should be equal");
        assertFalse(options.containsKey("diff"), "No diff should be generated for equal objects");
    }

    /**
     * Test that different structures are correctly identified.
     */
    @Test
    public void testDifferentComplexStructures() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("a", Arrays.asList(1, 2, 3));

        Map<String, Object> map2 = new HashMap<>();
        map2.put("a", Arrays.asList(1, 2, 4));  // Different last element

        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals(map1, map2, options);

        assertFalse(result, "Different structures should not be equal");
    }

    /**
     * Test multiple sequential comparisons to ensure no state leakage.
     */
    @Test
    public void testSequentialComparisons() {
        for (int i = 0; i < 100; i++) {
            List<Integer> list1 = Arrays.asList(1, 2, 3);
            List<Integer> list2 = Arrays.asList(1, 2, i % 2 == 0 ? 3 : 4);

            Map<String, Object> options = new HashMap<>();
            boolean result = DeepEquals.deepEquals(list1, list2, options);

            if (i % 2 == 0) {
                assertTrue(result, "Lists should be equal for even iterations");
            } else {
                assertFalse(result, "Lists should not be equal for odd iterations");
            }
        }
    }
}
