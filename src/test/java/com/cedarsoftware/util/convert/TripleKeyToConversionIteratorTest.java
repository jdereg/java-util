package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

/**
 * Test thread safety of MultiKeyMap iterator under concurrent modifications.
 */
class MultiKeyMapIteratorTest {
    
    private static final int INITIAL_ENTRIES = 100;
    private static final int CONCURRENT_OPERATIONS = 500;
    private static final int WRITER_THREADS = 4;
    
    @Test
    void testIteratorThreadSafetyUnderConcurrentModifications() throws InterruptedException {
        System.out.println("=== Iterator Thread Safety Test ===");
        
        MultiKeyMap<String> map = new MultiKeyMap<>(16, 0.70f);
        
        // Pre-populate with some entries
        for (int i = 0; i < INITIAL_ENTRIES; i++) {
            map.put(String.class, Integer.class, (long) i, "initial" + i);
        }
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(WRITER_THREADS + 1); // +1 for iterator thread
        AtomicBoolean testFailed = new AtomicBoolean(false);
        AtomicInteger iteratorCount = new AtomicInteger(0);
        AtomicInteger writerOpsCompleted = new AtomicInteger(0);
        
        // Start writer threads that continuously modify the map
        for (int threadId = 0; threadId < WRITER_THREADS; threadId++) {
            final int id = threadId;
            Thread writerThread = new Thread(() -> {
                try {
                    startLatch.await();
                    
                    for (int i = 0; i < CONCURRENT_OPERATIONS; i++) {
                        // Add new entries
                        map.put(String.class, Long.class, (long) (id * 1000 + i), "writer" + id + "-" + i);
                        
                        // Update existing entries occasionally
                        if (i % 10 == 0) {
                            map.put(String.class, Integer.class, (long) (i % INITIAL_ENTRIES), "updated" + id + "-" + i);
                        }
                        
                        writerOpsCompleted.incrementAndGet();
                        
                        // Small delay to allow more interleaving
                        if (i % 50 == 0) {
                            Thread.yield();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Writer thread " + id + " failed: " + e.getMessage());
                    testFailed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
            writerThread.start();
        }
        
        // Start iterator thread that continuously iterates
        Thread iteratorThread = new Thread(() -> {
            try {
                startLatch.await();
                
                // Perform multiple iterations while writers are active
                for (int iteration = 0; iteration < 10; iteration++) {
                    int count = 0;
                    Set<String> seenKeys = new HashSet<>();
                    
                    for (MultiKeyMap.MultiKeyEntry<String> entry : map.entries()) {
                        count++;
                        
                        // Verify entry integrity
                        assertNotNull(entry.keys[0], "Entry source should not be null");
                        assertNotNull(entry.keys[1], "Entry target should not be null");
                        assertNotNull(entry.value, "Entry value should not be null");
                        
                        // Check for duplicates in this iteration
                        String key = ((Class<?>) entry.keys[0]).getSimpleName() + ":" + ((Class<?>) entry.keys[1]).getSimpleName() + ":" + entry.keys[2];
                        assertFalse(seenKeys.contains(key), "Duplicate key found in iteration: " + key);
                        seenKeys.add(key);
                    }
                    
                    iteratorCount.addAndGet(count);
                    
                    // Small delay between iterations
                    Thread.sleep(1);
                }
            } catch (Exception e) {
                System.err.println("Iterator thread failed: " + e.getMessage());
                e.printStackTrace();
                testFailed.set(true);
            } finally {
                doneLatch.countDown();
            }
        });
        iteratorThread.start();
        
        // Start all threads
        startLatch.countDown();
        
        // Wait for completion
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Test should complete within 30 seconds");
        
        if (testFailed.get()) {
            fail("Iterator thread safety test failed - see error messages above");
        }
        
        System.out.println("Writer operations completed: " + writerOpsCompleted.get());
        System.out.println("Total iterator entries processed: " + iteratorCount.get());
        System.out.println("Final map size: " + map.size());
        
        // Verify that we processed a reasonable number of entries
        assertTrue(iteratorCount.get() > 0, "Iterator should have processed some entries");
        assertTrue(writerOpsCompleted.get() == WRITER_THREADS * CONCURRENT_OPERATIONS, 
                  "All writer operations should have completed");
    }
    
    @Test
    void testIteratorConsistencyDuringResize() throws InterruptedException {
        System.out.println("\n=== Iterator Consistency During Resize Test ===");
        
        // Start with small capacity to force resizing
        MultiKeyMap<String> map = new MultiKeyMap<>(4, 0.60f);
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicBoolean testFailed = new AtomicBoolean(false);
        List<Integer> iterationCounts = new ArrayList<>();
        
        // Writer thread that adds many entries to force multiple resizes
        Thread writerThread = new Thread(() -> {
            try {
                startLatch.await();
                
                for (int i = 0; i < 200; i++) {
                    map.put(String.class, Integer.class, (long) i, "resize-test-" + i);
                    
                    // Occasional pause to allow iterator to run
                    if (i % 20 == 0) {
                        Thread.sleep(1);
                    }
                }
            } catch (Exception e) {
                System.err.println("Writer thread failed: " + e.getMessage());
                testFailed.set(true);
            } finally {
                doneLatch.countDown();
            }
        });
        
        // Iterator thread that iterates during resizing
        Thread iteratorThread = new Thread(() -> {
            try {
                startLatch.await();
                
                for (int iteration = 0; iteration < 5; iteration++) {
                    int count = 0;
                    Set<Long> seenInstanceIds = new HashSet<>();
                    
                    for (MultiKeyMap.MultiKeyEntry<String> entry : map.entries()) {
                        count++;
                        
                        // Verify no duplicate instance IDs in this iteration
                        long instanceId = (Long) entry.keys[2];
                        assertFalse(seenInstanceIds.contains(instanceId), 
                                   "Duplicate instanceId found: " + instanceId);
                        seenInstanceIds.add(instanceId);
                        
                        // Verify entry consistency
                        assertEquals(String.class, entry.keys[0]);
                        assertEquals(Integer.class, entry.keys[1]);
                        assertTrue(entry.value.startsWith("resize-test-"));
                    }
                    
                    iterationCounts.add(count);
                    Thread.sleep(2); // Small delay between iterations
                }
            } catch (Exception e) {
                System.err.println("Iterator thread failed: " + e.getMessage());
                e.printStackTrace();
                testFailed.set(true);
            } finally {
                doneLatch.countDown();
            }
        });
        
        writerThread.start();
        iteratorThread.start();
        
        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        
        if (testFailed.get()) {
            fail("Iterator consistency test failed during resize");
        }
        
        System.out.println("Iteration counts: " + iterationCounts);
        System.out.println("Final map size: " + map.size());
        
        // Verify we got some iterations and they show increasing counts as entries were added
        assertFalse(iterationCounts.isEmpty(), "Should have completed some iterations");
        assertTrue(iterationCounts.get(iterationCounts.size() - 1) > 0, "Final iteration should have entries");
    }
    
    @Test 
    void testMultipleConcurrentIterators() throws InterruptedException {
        System.out.println("\n=== Multiple Concurrent Iterators Test ===");
        
        MultiKeyMap<String> map = new MultiKeyMap<>(32, 0.75f);
        
        // Pre-populate map
        for (int i = 0; i < 50; i++) {
            map.put(String.class, Integer.class, (long) i, "value" + i);
        }
        
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3); // 2 iterators + 1 writer
        AtomicBoolean testFailed = new AtomicBoolean(false);
        AtomicInteger totalIterations = new AtomicInteger(0);
        
        // Start multiple iterator threads
        for (int iteratorId = 0; iteratorId < 2; iteratorId++) {
            final int id = iteratorId;
            Thread iteratorThread = new Thread(() -> {
                try {
                    startLatch.await();
                    
                    for (int iteration = 0; iteration < 5; iteration++) {
                        int count = 0;
                        for (MultiKeyMap.MultiKeyEntry<String> entry : map.entries()) {
                            count++;
                            // Verify entry is valid
                            assertNotNull(entry.keys[0]);
                            assertNotNull(entry.keys[1]);
                            assertNotNull(entry.value);
                        }
                        totalIterations.addAndGet(count);
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    System.err.println("Iterator " + id + " failed: " + e.getMessage());
                    testFailed.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
            iteratorThread.start();
        }
        
        // Writer thread adding more entries
        Thread writerThread = new Thread(() -> {
            try {
                startLatch.await();
                
                for (int i = 50; i < 100; i++) {
                    map.put(String.class, Long.class, (long) i, "concurrent" + i);
                    Thread.sleep(1);
                }
            } catch (Exception e) {
                System.err.println("Writer failed: " + e.getMessage());
                testFailed.set(true);
            } finally {
                doneLatch.countDown();
            }
        });
        writerThread.start();
        
        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        
        if (testFailed.get()) {
            fail("Multiple concurrent iterators test failed");
        }
        
        System.out.println("Total iterations completed: " + totalIterations.get());
        System.out.println("Final map size: " + map.size());
        
        assertTrue(totalIterations.get() > 0, "Should have completed iterations");
    }
}