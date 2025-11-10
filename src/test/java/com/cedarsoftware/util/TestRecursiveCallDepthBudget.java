package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the fix for [crit-1] Deep recursion StackOverflowError.
 *
 * The issue: When custom equals fails, deepEquals makes a recursive call to find the reason.
 * If each level of a deep object graph has custom equals, this could cause many recursive calls.
 *
 * The fix: Recursive calls inherit the remaining depth budget (not the full budget), naturally
 * limiting total recursion depth.
 */
public class TestRecursiveCallDepthBudget {

    /**
     * Test object with custom equals that delegates to a child field.
     */
    static class NodeWithCustomEquals {
        private final String id;
        private final NodeWithCustomEquals child;

        NodeWithCustomEquals(String id, NodeWithCustomEquals child) {
            this.id = id;
            this.child = child;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof NodeWithCustomEquals)) return false;
            NodeWithCustomEquals other = (NodeWithCustomEquals) obj;
            // Custom equals that will fail if ids don't match
            // This triggers recursive deepEquals call to find WHY
            return Objects.equals(id, other.id) && Objects.equals(child, other.child);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, child);
        }
    }

    /**
     * Test that recursive calls inherit the remaining depth budget.
     */
    @Test
    public void testRecursiveCallInheritsRemainingBudget() {
        // Create a chain of 20 nodes with custom equals
        NodeWithCustomEquals node1 = createChain("a", 20);
        NodeWithCustomEquals node2 = createChain("b", 20);  // Different IDs will cause equals to fail

        Map<String, Object> options = new HashMap<>();
        options.put("__depthBudget", 10);  // Set budget to 10 (less than chain length)

        // Should throw SecurityException when depth exceeds budget
        // The recursive calls should inherit the remaining budget and hit the limit
        assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(node1, node2, options);
        }, "Should enforce depth limit even with recursive calls");
    }

    /**
     * Test that recursive calls work correctly within budget.
     */
    @Test
    public void testRecursiveCallsWorkWithinBudget() {
        // Create a chain of 5 nodes
        NodeWithCustomEquals node1 = createChain("a", 5);
        NodeWithCustomEquals node2 = createChain("b", 5);

        Map<String, Object> options = new HashMap<>();
        options.put("__depthBudget", 20);  // Plenty of budget

        // Should complete successfully and return false (different IDs)
        boolean result = DeepEquals.deepEquals(node1, node2, options);
        assertFalse(result, "Should return false for different node chains");

        // Should have DIFF in options
        assertTrue(options.containsKey("diff"), "Should have difference details");
    }

    /**
     * Test that equal chains with custom equals work correctly.
     */
    @Test
    public void testEqualChainsWithCustomEquals() {
        // Create identical chains
        NodeWithCustomEquals node1 = createChain("same", 10);
        NodeWithCustomEquals node2 = createChain("same", 10);

        Map<String, Object> options = new HashMap<>();
        options.put("__depthBudget", 20);

        // Should return true (same IDs)
        boolean result = DeepEquals.deepEquals(node1, node2, options);
        assertTrue(result, "Should return true for identical node chains");
    }

    /**
     * Test without depth budget (unlimited).
     */
    @Test
    public void testRecursiveCallWithUnlimitedBudget() {
        // Create moderate chain
        NodeWithCustomEquals node1 = createChain("a", 15);
        NodeWithCustomEquals node2 = createChain("b", 15);

        // No depth budget set
        Map<String, Object> options = new HashMap<>();

        // Should work fine without budget
        boolean result = DeepEquals.deepEquals(node1, node2, options);
        assertFalse(result, "Should return false for different chains");
    }

    /**
     * Test that budget exhaustion in recursive call prevents further recursion.
     */
    @Test
    public void testBudgetExhaustionInRecursiveCall() {
        // Create chain longer than budget
        NodeWithCustomEquals node1 = createChain("a", 100);
        NodeWithCustomEquals node2 = createChain("b", 100);

        Map<String, Object> options = new HashMap<>();
        options.put("__depthBudget", 5);  // Very limited budget

        // Should throw when budget is exhausted
        assertThrows(SecurityException.class, () -> {
            DeepEquals.deepEquals(node1, node2, options);
        }, "Should throw when recursive call exhausts budget");
    }

    /**
     * Test mixed object graph with some custom equals.
     */
    @Test
    public void testMixedGraphWithCustomEquals() {
        // Create a graph with both custom equals objects and regular objects
        Map<String, Object> map1 = new HashMap<>();
        map1.put("node", createChain("a", 5));
        map1.put("data", "value");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("node", createChain("b", 5));
        map2.put("data", "value");

        Map<String, Object> options = new HashMap<>();
        options.put("__depthBudget", 15);

        // Should work and find the difference in the node chain
        boolean result = DeepEquals.deepEquals(map1, map2, options);
        assertFalse(result, "Should return false due to different node chains");
    }

    /**
     * Helper to create a chain of nodes with custom equals.
     */
    private NodeWithCustomEquals createChain(String idPrefix, int depth) {
        if (depth <= 0) {
            return null;
        }
        return new NodeWithCustomEquals(
            idPrefix + depth,
            createChain(idPrefix, depth - 1)
        );
    }
}
