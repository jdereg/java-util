package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for MultiKeyMap's lock striping implementation.
 * Tests concurrent operations, performance characteristics, and correctness.
 */
class MultiKeyMapLockStripingTest {

    private static final Logger LOG = Logger.getLogger(MultiKeyMapLockStripingTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }

    private MultiKeyMap<String> map;
    private static final int NUM_THREADS = 16;
    private static final int OPERATIONS_PER_THREAD = 1000;
    
    @BeforeEach
    void setUp() {
        map = new MultiKeyMap<>(64); // Start with reasonable capacity
    }
    
    @Test
    void testBasicConcurrentPuts() throws InterruptedException {
        LOG.info("=== Starting Basic Concurrent Puts Test ===");
        LOG.info("Threads: " + NUM_THREADS + ", Operations per thread: " + OPERATIONS_PER_THREAD);
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        AtomicInteger errors = new AtomicInteger(0);
        
        long startTime = System.nanoTime();
        
        // Each thread puts unique keys
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        String key = "thread" + threadId + "_key" + i;
                        String value = "value_" + threadId + "_" + i;
                        map.put(key, value);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        LOG.info("Test completed in " + (totalTime / 1_000_000) + "ms");
        LOG.info("Operations per second: " + (NUM_THREADS * OPERATIONS_PER_THREAD * 1_000_000_000L / totalTime));
        
        map.printContentionStatistics();
        
        assertEquals(0, errors.get(), "No errors should occur during concurrent puts");
        assertEquals(NUM_THREADS * OPERATIONS_PER_THREAD, map.size(), "All entries should be present");
        
        // Verify all entries are accessible
        for (int t = 0; t < NUM_THREADS; t++) {
            for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                String key = "thread" + t + "_key" + i;
                String expectedValue = "value_" + t + "_" + i;
                assertEquals(expectedValue, map.get(key), "Value should match for key: " + key);
            }
        }
    }
    
    @Test
    void testConcurrentMultiKeyOperations() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        AtomicInteger errors = new AtomicInteger(0);
        
        // Each thread works with multi-dimensional keys
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < OPERATIONS_PER_THREAD / 10; i++) { // Fewer ops for multi-key
                        String value = "multiValue_" + threadId + "_" + i;
                        // 3D keys
                        map.putMultiKey(value, "dim1_" + threadId, "dim2_" + i, "dim3_" + (i % 5));
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertEquals(0, errors.get(), "No errors should occur during concurrent multi-key puts");
        
        // Verify entries
        for (int t = 0; t < NUM_THREADS; t++) {
            for (int i = 0; i < OPERATIONS_PER_THREAD / 10; i++) {
                String expectedValue = "multiValue_" + t + "_" + i;
                String actualValue = map.getMultiKey("dim1_" + t, "dim2_" + i, "dim3_" + (i % 5));
                assertEquals(expectedValue, actualValue, "Multi-key value should match");
            }
        }
    }
    
    @Test
    void testConcurrentMixedOperations() throws InterruptedException {
        LOG.info("=== Starting Concurrent Mixed Operations Test ===");
        
        // Pre-populate with some data
        for (int i = 0; i < 100; i++) {
            map.put("initial_" + i, "initialValue_" + i);
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicInteger totalOps = new AtomicInteger(0);
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    Random random = new Random(threadId); // Reproducible randomness
                    for (int i = 0; i < OPERATIONS_PER_THREAD / 4; i++) {
                        String key = "key_" + threadId + "_" + i;
                        String value = "value_" + threadId + "_" + i;
                        
                        int operation = random.nextInt(10);
                        try {
                            if (operation < 4) {
                                // 40% puts
                                map.put(key, value);
                            } else if (operation < 7) {
                                // 30% gets
                                map.get(key);
                            } else if (operation < 8) {
                                // 10% removes
                                map.remove(key);
                            } else if (operation < 9) {
                                // 10% putIfAbsent
                                map.putIfAbsent(key, value);
                            } else {
                                // 10% computeIfAbsent
                                final int finalThreadId = threadId;
                                final int finalI = i;
                                map.computeIfAbsent(key, k -> "computed_" + finalThreadId + "_" + finalI);
                            }
                            totalOps.incrementAndGet();
                        } catch (Exception ex) {
                            LOG.info("Thread " + threadId + " operation " + operation + " failed: " + ex.getMessage());
                            ex.printStackTrace();
                            throw ex;
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        LOG.info("Mixed operations test completed in " + (totalTime / 1_000_000) + "ms");
        LOG.info("Operations per second: " + (totalOps.get() * 1_000_000_000L / totalTime));
        LOG.info("Expected operations: " + (NUM_THREADS * OPERATIONS_PER_THREAD / 4) + ", Actual: " + totalOps.get());
        
        map.printContentionStatistics();
        
        assertEquals(0, errors.get(), "No errors should occur during mixed operations");
        assertTrue(totalOps.get() > 0, "Operations should have been performed");
        LOG.info("Completed " + totalOps.get() + " concurrent operations successfully");
    }
    
    @Test 
    void testConcurrentResizeOperations() throws InterruptedException {
        LOG.info("=== Starting Concurrent Resize Operations Test ===");
        
        // Start with small capacity to force resizes
        map = new MultiKeyMap<>(8);
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        AtomicInteger errors = new AtomicInteger(0);
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // Rapidly add entries to trigger multiple resizes
                    for (int i = 0; i < OPERATIONS_PER_THREAD / 2; i++) {
                        String key = "resize_thread_" + threadId + "_" + i;
                        String value = "resize_value_" + threadId + "_" + i;
                        map.put(key, value);
                        
                        // Occasionally read to mix operations during resize
                        if (i % 10 == 0) {
                            map.get(key);
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        
        LOG.info("Resize operations test completed in " + (totalTime / 1_000_000) + "ms");
        LOG.info("Final map size: " + map.size());
        
        map.printContentionStatistics();
        
        assertEquals(0, errors.get(), "No errors should occur during concurrent resize operations");
        
        // Verify data integrity after resizes
        for (int t = 0; t < NUM_THREADS; t++) {
            for (int i = 0; i < OPERATIONS_PER_THREAD / 2; i++) {
                String key = "resize_thread_" + t + "_" + i;
                String expectedValue = "resize_value_" + t + "_" + i;
                assertEquals(expectedValue, map.get(key), "Value should survive resize operations");
            }
        }
    }
    
    @Test
    void testConcurrentMapInterface() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        AtomicInteger errors = new AtomicInteger(0);
        
        // Test ConcurrentMap interface methods under concurrency
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < OPERATIONS_PER_THREAD / 10; i++) {
                        String key = "concurrent_" + threadId + "_" + i;
                        String value = "value_" + threadId + "_" + i;
                        String newValue = "newValue_" + threadId + "_" + i;
                        
                        // Test atomic operations
                        assertNull(map.putIfAbsent(key, value));
                        assertEquals(value, map.putIfAbsent(key, "different"));
                        
                        String computed = map.computeIfAbsent(key + "_new", k -> "computed_" + threadId);
                        assertTrue(computed.startsWith("computed_"));
                        
                        boolean replaced = map.replace(key, value, newValue);
                        if (replaced) {
                            assertEquals(newValue, map.get(key));
                        }
                        
                        // Test merge
                        map.merge(key + "_merge", "initial", (old, val) -> old + "_" + val);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertEquals(0, errors.get(), "No errors should occur during concurrent ConcurrentMap operations");
    }
    
    @Test
    void testConcurrentClearOperations() throws InterruptedException {
        // Pre-populate
        for (int i = 0; i < 1000; i++) {
            map.put("pre_" + i, "value_" + i);
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        AtomicBoolean clearCalled = new AtomicBoolean(false);
        AtomicInteger errors = new AtomicInteger(0);
        
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    if (threadId == 0 && !clearCalled.getAndSet(true)) {
                        // One thread clears the map
                        Thread.sleep(50); // Let other threads start working
                        map.clear();
                    } else {
                        // Other threads perform regular operations
                        for (int i = 0; i < 100; i++) {
                            String key = "thread_" + threadId + "_" + i;
                            map.put(key, "value_" + i);
                            map.get(key);
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        
        assertEquals(0, errors.get(), "No errors should occur during concurrent clear operations");
        // Note: We can't assert exact size due to race conditions, but no errors should occur
    }
    
    @RepeatedTest(5)
    void testStripeLockDistribution() {
        // Test that different hash values use different stripe locks
        Map<String, Set<String>> stripeToKeys = new HashMap<>();
        
        // Use reflection or a test-friendly method to verify stripe distribution
        // For now, we'll test indirectly by ensuring good concurrency performance
        long start = System.nanoTime();
        
        // Simulate concurrent operations that would benefit from good stripe distribution
        ExecutorService executor = Executors.newFixedThreadPool(32);
        List<Future<?>> futures = new ArrayList<>();
        
        for (int t = 0; t < 32; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                for (int i = 0; i < 100; i++) {
                    String key = "stripe_test_" + threadId + "_" + i + "_" + System.nanoTime();
                    map.put(key, "value_" + i);
                }
            }));
        }
        
        // Wait for completion
        futures.forEach(future -> {
            try {
                future.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Stripe distribution test failed: " + e.getMessage());
            }
        });
        
        executor.shutdown();
        long duration = System.nanoTime() - start;
        
        // With good stripe distribution, this should complete quickly
        assertTrue(duration < TimeUnit.SECONDS.toNanos(5), 
                   "Operations should complete quickly with good stripe distribution");
        assertEquals(32 * 100, map.size(), "All entries should be present");
    }
    
    @Test
    void testDeadlockPrevention() throws InterruptedException {
        // Test that our lock ordering prevents deadlocks
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        AtomicInteger errors = new AtomicInteger(0);
        AtomicBoolean deadlockDetected = new AtomicBoolean(false);
        
        // Create a scenario that could cause deadlock with poor lock ordering
        for (int t = 0; t < NUM_THREADS; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < 50; i++) {
                        // Operations that might access different stripes
                        String key1 = "deadlock_" + threadId + "_" + i;
                        String key2 = "deadlock_" + ((threadId + 1) % NUM_THREADS) + "_" + i;
                        
                        map.put(key1, "value1");
                        map.put(key2, "value2");
                        
                        // Force potential resize (global operation)
                        if (i == 25) {
                            map.clear(); // Global operation
                        }
                        
                        map.get(key1);
                        map.get(key2);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Set up deadlock detection
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (latch.getCount() > 0) {
                    deadlockDetected.set(true);
                    // Interrupt all threads to break potential deadlock
                    executor.shutdownNow();
                }
            }
        }, 15000); // 15 second timeout for deadlock detection
        
        boolean completed = latch.await(20, TimeUnit.SECONDS);
        timer.cancel();
        executor.shutdown();
        
        assertFalse(deadlockDetected.get(), "No deadlock should be detected");
        assertTrue(completed, "All operations should complete without deadlock");
        assertEquals(0, errors.get(), "No errors should occur during deadlock prevention test");
    }
    
    @Test
    void testPerformanceWithStriping() {
        // Compare performance characteristics with and without contention
        map = new MultiKeyMap<>(1024); // Large enough to avoid resizes

        // Warmup phase - let JIT compile hot paths
        for (int warmup = 0; warmup < 3; warmup++) {
            map.clear();
            for (int i = 0; i < 5000; i++) {
                map.put("warmup_" + warmup + "_" + i, "value_" + i);
            }
        }
        map.clear();

        // Single-threaded baseline
        long singleThreadStart = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            map.put("single_" + i, "value_" + i);
        }
        long singleThreadTime = System.nanoTime() - singleThreadStart;

        map.clear();
        
        // Multi-threaded with striping
        long multiThreadStart = System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        
        for (int t = 0; t < 8; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                for (int i = 0; i < 1250; i++) { // 8 * 1250 = 10000 total
                    map.put("multi_" + threadId + "_" + i, "value_" + i);
                }
            }));
        }
        
        futures.forEach(future -> {
            try {
                future.get();
            } catch (Exception e) {
                fail("Performance test failed: " + e.getMessage());
            }
        });
        
        long multiThreadTime = System.nanoTime() - multiThreadStart;
        executor.shutdown();
        
        assertEquals(10000, map.size(), "All entries should be present");
        
        // With 32 stripes and 8 threads, we should see some performance benefit
        double actualSlowdownFactor = (double) multiThreadTime / singleThreadTime;
        // Allow 5x slowdown tolerance for CI environments with system variability
        double expectedMaxSlowdown = 5.0;

        LOG.info("Single-threaded time: " + (singleThreadTime / 1_000_000) + "ms");
        LOG.info("Multi-threaded time:  " + (multiThreadTime / 1_000_000) + "ms");
        LOG.info("Actual slowdown factor: " + String.format("%.2f", actualSlowdownFactor) + "x");
        LOG.info("Expected max slowdown: " + expectedMaxSlowdown + "x");

        // The multi-threaded version should not be significantly slower
        // (allowing for overhead and CI variability, it should be at most 5x slower)
        assertTrue(multiThreadTime < singleThreadTime * expectedMaxSlowdown,
                   String.format("Multi-threaded version is too slow: %.2fx slower (expected max: %.1fx). " +
                                 "Single-threaded: %dms, Multi-threaded: %dms",
                                 actualSlowdownFactor, expectedMaxSlowdown,
                                 singleThreadTime / 1_000_000, multiThreadTime / 1_000_000));
    }
}