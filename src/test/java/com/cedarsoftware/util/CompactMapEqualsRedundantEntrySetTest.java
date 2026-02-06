package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for bug: equals() missing return true for compact array path.
 *
 * Bug: When val instanceof Object[] (compact array state, sizes 2 to compactSize),
 * the loop at lines 1190-1206 verifies all entries match, then falls through
 * without returning true. Execution reaches the size==1 fallthrough at line 1215:
 *   return entrySet().equals(other.entrySet());
 * This redundantly checks equality a second time. The result is correct but
 * entrySet() is called unnecessarily, allocating objects and re-checking all entries.
 *
 * Fix: Add "return true;" after the loop for the compact array path.
 */
class CompactMapEqualsRedundantEntrySetTest {

    /**
     * Tracks how many times entrySet() is called during equals().
     * For the compact array path, entrySet() should NOT be called at all
     * if the fast path correctly returns true after verifying all entries.
     */
    @Test
    void testEqualsOnCompactArrayDoesNotCallEntrySet() {
        AtomicInteger entrySetCallCount = new AtomicInteger(0);

        // Create a CompactMap that counts entrySet() calls
        CompactMap<String, String> map = new CompactMap<String, String>() {
            @Override
            protected int compactSize() { return 10; }

            @Override
            public Set<Map.Entry<String, String>> entrySet() {
                entrySetCallCount.incrementAndGet();
                return super.entrySet();
            }
        };

        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        // Verify we're in compact array state
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Create an equal map to compare against
        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put("b", "2");
        other.put("c", "3");

        // Reset counter before equals()
        entrySetCallCount.set(0);

        boolean result = map.equals(other);

        assertTrue(result, "Maps should be equal");
        assertEquals(0, entrySetCallCount.get(),
                "entrySet() should NOT be called during equals() for compact array state. " +
                "The fast-path loop should return true directly without falling through to entrySet().equals()");
    }

    /**
     * Verify that equals() still works correctly for compact array state
     * when maps are NOT equal (different values).
     */
    @Test
    void testEqualsOnCompactArrayReturnsFalseForDifferentValues() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(10).build();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put("b", "DIFFERENT");
        other.put("c", "3");

        assertFalse(map.equals(other), "Maps should NOT be equal (different values)");
    }

    /**
     * Verify that equals() still works correctly for compact array state
     * when maps are NOT equal (different keys).
     */
    @Test
    void testEqualsOnCompactArrayReturnsFalseForDifferentKeys() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(10).build();
        map.put("a", "1");
        map.put("b", "2");

        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put("x", "2");

        assertFalse(map.equals(other), "Maps should NOT be equal (different keys)");
    }

    /**
     * Verify equals() works correctly with null values in compact array state.
     */
    @Test
    void testEqualsOnCompactArrayWithNullValues() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(10).build();
        map.put("a", null);
        map.put("b", "2");

        Map<String, String> other = new HashMap<>();
        other.put("a", null);
        other.put("b", "2");

        assertTrue(map.equals(other), "Maps with null values should be equal");
    }

    /**
     * Verify that single-entry equals() still works (it legitimately uses
     * the entrySet().equals() fallthrough path at line 1215).
     */
    @Test
    void testEqualsOnSingleEntryStillWorks() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().build();
        map.put("id", "value");

        Map<String, String> other = new HashMap<>();
        other.put("id", "value");

        assertTrue(map.equals(other), "Single-entry maps should be equal");
    }
}
