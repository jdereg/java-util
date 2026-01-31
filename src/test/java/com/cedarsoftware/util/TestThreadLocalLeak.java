package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify that ThreadLocal memory leaks are prevented by proper cleanup.
 *
 * Tests for [crit-3] ThreadLocal memory leak issue.
 *
 * The issue: formattingStack ThreadLocal was not cleaned up in all exit paths,
 * particularly in deepHashCode(). In thread pools, this causes the Set to persist
 * across multiple invocations, leading to memory leaks.
 */
public class TestThreadLocalLeak {

    /**
     * Test that deepHashCode() properly cleans up ThreadLocal even in normal operation.
     */
    @Test
    public void testDeepHashCodeCleansUpThreadLocal() throws Exception {
        Object testObj = new Object[]{"test", 123, 45.6};

        // Call deepHashCode
        int hash = DeepEquals.deepHashCode(testObj);
        assertNotEquals(0, hash);

        // Verify ThreadLocal is cleaned up
        assertThreadLocalIsClean();
    }

    /**
     * Test that deepEquals() properly cleans up ThreadLocal even in normal operation.
     */
    @Test
    public void testDeepEqualsCleansUpThreadLocal() throws Exception {
        Object a = new Object[]{"test", 123};
        Object b = new Object[]{"test", 123};

        // Call deepEquals
        boolean result = DeepEquals.deepEquals(a, b);
        assertTrue(result);

        // Verify ThreadLocal is cleaned up
        assertThreadLocalIsClean();
    }

    /**
     * Test that deepEquals with options properly cleans up ThreadLocal.
     */
    @Test
    public void testDeepEqualsWithOptionsCleansUpThreadLocal() throws Exception {
        Object a = new Object[]{"test", 123};
        Object b = new Object[]{"test", 123};
        Map<String, Object> options = new HashMap<>();

        // Call deepEquals with options
        boolean result = DeepEquals.deepEquals(a, b, options);
        assertTrue(result);

        // Verify ThreadLocal is cleaned up
        assertThreadLocalIsClean();
    }

    /**
     * Test that ThreadLocal is cleaned up even when exceptions occur.
     * This simulates the case where an error occurs during processing.
     */
    @Test
    public void testThreadLocalCleanupOnException() throws Exception {
        // Create a pathological object that might cause issues
        Object[] circular = new Object[1];
        circular[0] = circular;

        try {
            // This should work due to circular reference handling
            int hash = DeepEquals.deepHashCode(circular);
            assertNotEquals(0, hash);
        } catch (Exception e) {
            // Even if exception occurs, ThreadLocal should be cleaned
        }

        // Verify ThreadLocal is cleaned up
        assertThreadLocalIsClean();
    }

    /**
     * Test ThreadLocal cleanup in a thread pool scenario.
     * This is the REAL-WORLD scenario where memory leaks occur.
     *
     * Thread pools reuse threads, so if ThreadLocal is not cleaned up,
     * the Set will accumulate objects across multiple tasks.
     */
    @Test
    public void testThreadPoolScenario_NoMemoryLeak() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // Submit 10 tasks to the same thread pool
            for (int i = 0; i < 10; i++) {
                final int iteration = i;
                Future<Boolean> future = executor.submit(() -> {
                    // Each task does some deepEquals/deepHashCode operations
                    Object obj1 = new Object[]{iteration, "test", 123.45};
                    Object obj2 = new Object[]{iteration, "test", 123.45};

                    DeepEquals.deepEquals(obj1, obj2);
                    DeepEquals.deepHashCode(obj1);

                    // After operations complete, verify ThreadLocal is clean
                    // (This check happens IN the worker thread)
                    try {
                        assertThreadLocalIsClean();
                        return true;
                    } catch (Exception e) {
                        throw new RuntimeException("ThreadLocal not cleaned up!", e);
                    }
                });

                // Verify task succeeded
                assertTrue(future.get(5, TimeUnit.SECONDS),
                    "Task " + i + " should succeed with clean ThreadLocal");
            }
        } finally {
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
    }

    /**
     * Test that repeated calls don't accumulate objects in ThreadLocal.
     * This would detect if cleanup is not happening on EVERY call.
     */
    @Test
    public void testRepeatedCallsDontAccumulateInThreadLocal() throws Exception {
        // Make 100 calls in rapid succession
        for (int i = 0; i < 100; i++) {
            Object obj = new Object[]{i, "test" + i};
            DeepEquals.deepHashCode(obj);

            // After each call, ThreadLocal should be clean
            assertThreadLocalIsClean();
        }
    }

    /**
     * Test complex nested objects to ensure cleanup works for deep structures.
     */
    @Test
    public void testComplexNestedObjectsCleanup() throws Exception {
        // Create a deeply nested structure
        Map<String, Object> root = new HashMap<>();
        Map<String, Object> level1 = new HashMap<>();
        Map<String, Object> level2 = new HashMap<>();

        level2.put("deep", new Object[]{"value", 123});
        level1.put("nested", level2);
        root.put("top", level1);

        DeepEquals.deepHashCode(root);

        // Verify ThreadLocal is cleaned up even after processing complex structures
        assertThreadLocalIsClean();
    }

    /**
     * Helper method to verify that the formattingStack ThreadLocal is cleaned up.
     * Uses reflection to access the private ThreadLocal field.
     */
    private void assertThreadLocalIsClean() throws Exception {
        // Use reflection to access the private formattingStack ThreadLocal
        Field field = DeepEquals.class.getDeclaredField("formattingStack");
        field.setAccessible(true);
        ThreadLocal<java.util.Deque<Set<Object>>> formattingStack =
            (ThreadLocal<java.util.Deque<Set<Object>>>) field.get(null);

        // Get the value for current thread
        java.util.Deque<Set<Object>> stack = formattingStack.get();

        // It should be empty (or null if removed and re-initialized)
        // The initialValue() creates an empty Deque, so we check that it's empty
        assertTrue(stack.isEmpty(),
            "formattingStack ThreadLocal should be empty after cleanup, but contains "
            + stack.size() + " sets");
    }
}
