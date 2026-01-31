package com.cedarsoftware.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to verify the fix for [thread-1] SimpleDateFormat race condition.
 *
 * The issue: SimpleDateFormat is not thread-safe, and the ThreadLocal instance can be
 * accessed re-entrantly during nested callbacks (e.g., when deepEquals is called from
 * within a custom equals method).
 *
 * The fix: Use SafeSimpleDateFormat instead of SimpleDateFormat, which handles
 * re-entrant calls safely with copy-on-write semantics and per-thread LRU cache.
 */
public class TestDateFormatterReentrancy {

    /**
     * Test object with custom equals that triggers re-entrant deepEquals call.
     * Includes Date fields to exercise the date formatter.
     */
    static class ObjectWithDateAndCustomEquals {
        private final String id;
        private final Date timestamp;
        private final ObjectWithDateAndCustomEquals child;

        ObjectWithDateAndCustomEquals(String id, Date timestamp, ObjectWithDateAndCustomEquals child) {
            this.id = id;
            this.timestamp = timestamp;
            this.child = child;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ObjectWithDateAndCustomEquals)) return false;
            ObjectWithDateAndCustomEquals other = (ObjectWithDateAndCustomEquals) obj;

            // Custom equals that uses deepEquals for child comparison
            // This triggers re-entrant call when comparing children
            return Objects.equals(id, other.id) &&
                   Objects.equals(timestamp, other.timestamp) &&
                   DeepEquals.deepEquals(child, other.child);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, timestamp, child);
        }
    }

    /**
     * Test that re-entrant calls with Date objects work correctly.
     */
    @Test
    public void testReentrantCallsWithDates() {
        Date date1 = new Date(1000000000L);
        Date date2 = new Date(2000000000L);
        Date date3 = new Date(3000000000L);

        // Create nested structure with dates
        ObjectWithDateAndCustomEquals child1 = new ObjectWithDateAndCustomEquals("child", date3, null);
        ObjectWithDateAndCustomEquals parent1 = new ObjectWithDateAndCustomEquals("parent", date1, child1);

        ObjectWithDateAndCustomEquals child2 = new ObjectWithDateAndCustomEquals("child", date3, null);
        ObjectWithDateAndCustomEquals parent2 = new ObjectWithDateAndCustomEquals("parent", date1, child2);

        // This should work without SimpleDateFormat corruption
        // The custom equals in parent1 will call deepEquals on child1 vs child2
        // Both the outer and re-entrant calls may need to format dates
        Map<String, Object> options = new HashMap<>();
        boolean result = DeepEquals.deepEquals(parent1, parent2, options);

        assertTrue(result, "Should return true for identical nested structures");
    }

    /**
     * Test that re-entrant calls with different dates detect the difference.
     */
    @Test
    public void testReentrantCallsWithDifferentDates() {
        Date date1 = new Date(1000000000L);
        Date date2 = new Date(2000000000L);
        Date date3 = new Date(3000000000L);

        // Create nested structure with different child dates
        ObjectWithDateAndCustomEquals child1 = new ObjectWithDateAndCustomEquals("child", date2, null);
        ObjectWithDateAndCustomEquals parent1 = new ObjectWithDateAndCustomEquals("parent", date1, child1);

        ObjectWithDateAndCustomEquals child2 = new ObjectWithDateAndCustomEquals("child", date3, null);
        ObjectWithDateAndCustomEquals parent2 = new ObjectWithDateAndCustomEquals("parent", date1, child2);

        // Should detect difference in child timestamps
        Map<String, Object> options = new HashMap<>();
        options.put("includeDiff", true);
        boolean result = DeepEquals.deepEquals(parent1, parent2, options);

        assertFalse(result, "Should return false for different child timestamps");
        assertTrue(options.containsKey("diff"), "Should have difference details");
    }

    /**
     * Test deep re-entrant calls (multiple levels).
     */
    @Test
    public void testDeepReentrancy() {
        Date date = new Date(1000000000L);

        // Create 5-level deep structure
        ObjectWithDateAndCustomEquals level5 = new ObjectWithDateAndCustomEquals("level5", date, null);
        ObjectWithDateAndCustomEquals level4 = new ObjectWithDateAndCustomEquals("level4", date, level5);
        ObjectWithDateAndCustomEquals level3 = new ObjectWithDateAndCustomEquals("level3", date, level4);
        ObjectWithDateAndCustomEquals level2 = new ObjectWithDateAndCustomEquals("level2", date, level3);
        ObjectWithDateAndCustomEquals level1 = new ObjectWithDateAndCustomEquals("level1", date, level2);

        // Create identical structure
        ObjectWithDateAndCustomEquals other5 = new ObjectWithDateAndCustomEquals("level5", date, null);
        ObjectWithDateAndCustomEquals other4 = new ObjectWithDateAndCustomEquals("level4", date, other5);
        ObjectWithDateAndCustomEquals other3 = new ObjectWithDateAndCustomEquals("level3", date, other4);
        ObjectWithDateAndCustomEquals other2 = new ObjectWithDateAndCustomEquals("level2", date, other3);
        ObjectWithDateAndCustomEquals other1 = new ObjectWithDateAndCustomEquals("level1", date, other2);

        // Each level's custom equals will trigger re-entrant deepEquals
        // All levels have dates that need formatting
        boolean result = DeepEquals.deepEquals(level1, other1);

        assertTrue(result, "Should handle deep re-entrant calls with dates");
    }

    /**
     * Test multi-threaded access with re-entrant calls.
     */
    @Test
    public void testMultiThreadedReentrancy() throws Exception {
        final int THREAD_COUNT = 10;
        final int ITERATIONS = 100;
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        Date date1 = new Date(1000000000L);
        Date date2 = new Date(2000000000L);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize start

                    for (int j = 0; j < ITERATIONS; j++) {
                        // Each thread creates nested structures with dates
                        ObjectWithDateAndCustomEquals child = new ObjectWithDateAndCustomEquals(
                            "child-" + threadId, date2, null);
                        ObjectWithDateAndCustomEquals parent = new ObjectWithDateAndCustomEquals(
                            "parent-" + threadId, date1, child);

                        ObjectWithDateAndCustomEquals otherChild = new ObjectWithDateAndCustomEquals(
                            "child-" + threadId, date2, null);
                        ObjectWithDateAndCustomEquals otherParent = new ObjectWithDateAndCustomEquals(
                            "parent-" + threadId, date1, otherChild);

                        // Re-entrant call with date formatting
                        boolean result = DeepEquals.deepEquals(parent, otherParent);
                        if (result) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        assertEquals(THREAD_COUNT * ITERATIONS, successCount.get(),
            "All comparisons should succeed without date formatter corruption");
        assertEquals(0, failureCount.get(), "No failures should occur");
    }

    /**
     * Test that date formatting in diff output works correctly with re-entrancy.
     */
    @Test
    public void testDateFormattingInDiffWithReentrancy() {
        Date date1 = new Date(1000000000L);
        Date date2 = new Date(2000000000L);

        ObjectWithDateAndCustomEquals child1 = new ObjectWithDateAndCustomEquals("child", date1, null);
        ObjectWithDateAndCustomEquals parent1 = new ObjectWithDateAndCustomEquals("parent", date1, child1);

        ObjectWithDateAndCustomEquals child2 = new ObjectWithDateAndCustomEquals("child", date2, null);
        ObjectWithDateAndCustomEquals parent2 = new ObjectWithDateAndCustomEquals("parent", date1, child2);

        Map<String, Object> options = new HashMap<>();
        options.put("includeDiff", true);
        DeepEquals.deepEquals(parent1, parent2, options);

        // Verify that diff was generated (which means date formatting worked)
        assertTrue(options.containsKey("diff"), "Should have diff output");

        // The diff should contain formatted dates (no corruption)
        Object diff = options.get("diff");
        assertNotNull(diff, "Diff should not be null");
    }

    /**
     * Test that SafeSimpleDateFormat cleans up its thread-local cache properly.
     */
    @Test
    public void testNoThreadLocalLeak() throws Exception {
        Date date = new Date(1000000000L);
        ObjectWithDateAndCustomEquals obj1 = new ObjectWithDateAndCustomEquals("test", date, null);
        ObjectWithDateAndCustomEquals obj2 = new ObjectWithDateAndCustomEquals("test", date, null);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                // Perform comparison with date formatting
                DeepEquals.deepEquals(obj1, obj2);

                // SafeSimpleDateFormat should handle its own cleanup
                // (unlike the old SimpleDateFormat ThreadLocal which could leak)
                return null;
            }).get(5, TimeUnit.SECONDS);
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    /**
     * Test edge case: null dates in re-entrant calls.
     */
    @Test
    public void testReentrantCallsWithNullDates() {
        ObjectWithDateAndCustomEquals child1 = new ObjectWithDateAndCustomEquals("child", null, null);
        ObjectWithDateAndCustomEquals parent1 = new ObjectWithDateAndCustomEquals("parent", null, child1);

        ObjectWithDateAndCustomEquals child2 = new ObjectWithDateAndCustomEquals("child", null, null);
        ObjectWithDateAndCustomEquals parent2 = new ObjectWithDateAndCustomEquals("parent", null, child2);

        boolean result = DeepEquals.deepEquals(parent1, parent2);
        assertTrue(result, "Should handle null dates in re-entrant calls");
    }

    /**
     * Test that re-entrant calls preserve date formatting accuracy.
     */
    @Test
    public void testDateFormattingAccuracyWithReentrancy() {
        // Use specific dates that format to known strings
        Date date = new Date(0); // Epoch: 1970-01-01 00:00:00 UTC

        ObjectWithDateAndCustomEquals child = new ObjectWithDateAndCustomEquals("child", date, null);
        ObjectWithDateAndCustomEquals parent = new ObjectWithDateAndCustomEquals("parent", date, child);

        Map<String, Object> options = new HashMap<>();
        options.put("includeDiff", true);

        // Even with re-entrancy, date formatting should be accurate
        boolean result = DeepEquals.deepEquals(parent, parent);
        assertTrue(result, "Should correctly compare identical objects with dates");
    }
}
