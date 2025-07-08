package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Comprehensive concurrency test for MultiKeyMap.
 * Tests thread safety under various concurrent access patterns.
 */
class MultiKeyMapConcurrencyTest {
    
    private static final Logger LOG = Logger.getLogger(MultiKeyMapConcurrencyTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }
    
    // Test configuration - you can adjust these for your manual testing
    private static final int CAPACITY = 16;
    private static final int NUM_THREADS = 8;
    private static final int OPERATIONS_PER_THREAD = 10000;
    private static final int TEST_DURATION_SECONDS = 5;
    
    @Test
    void testConcurrentReadsAndWrites() throws InterruptedException {
        LOG.info("=== Concurrent Reads and Writes Test ===");
        LOG.info("Threads: " + NUM_THREADS + ", Operations per thread: " + OPERATIONS_PER_THREAD);
        
        MultiKeyMap<String> map = new MultiKeyMap<>(CAPACITY);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        AtomicBoolean testFailed = new AtomicBoolean(false);
        AtomicReference<Exception> firstException = new AtomicReference<>();
        
        long testStartTime = System.nanoTime();
        
        // Create threads that perform mixed read/write operations
        for (int threadId = 0; threadId < NUM_THREADS; threadId++) {
            final int id = threadId;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    Random random = new Random(id * 12345);
                    
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        Class<?> source = getRandomClass(random);
                        Class<?> target = getRandomClass(random);
                        long instanceId = random.nextInt(10);
                        
                        if (random.nextBoolean()) {
                            // Write operation
                            String value = "thread" + id + "-op" + i;
                            map.put(value, source, target, instanceId);
                        } else {
                            // Read operation
                            String result = map.get(source, target, instanceId);
                            // Result can be null or any valid string
                        }
                    }
                } catch (Exception e) {
                    testFailed.set(true);
                    firstException.compareAndSet(null, e);
                } finally {
                    doneLatch.countDown();
                }
            });
            thread.start();
        }
        
        startLatch.countDown(); // Start all threads
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Test should complete within 30 seconds");
        
        long testEndTime = System.nanoTime();
        long totalTime = testEndTime - testStartTime;
        
        if (testFailed.get()) {
            fail("Concurrency test failed: " + firstException.get().getMessage(), firstException.get());
        }
        
        LOG.info("Test completed in " + (totalTime / 1_000_000) + "ms");
        LOG.info("Operations per second: " + (NUM_THREADS * OPERATIONS_PER_THREAD * 1_000_000_000L / totalTime));
        LOG.info("Final map size: " + map.size());
        LOG.info("Max chain length: " + map.getMaxChainLength());
        LOG.info("Load factor: " + String.format("%.2f", map.getLoadFactor()));
        
        map.printContentionStatistics();
        
        assertTrue(map.size() > 0, "Map should have some entries after concurrent operations");
        assertTrue(map.getMaxChainLength() >= 1, "Should have at least one chain");
    }
    
    @Test
    void testConcurrentWritesSameKey() throws InterruptedException {
        LOG.info("=== Concurrent Writes Same Key Test ===");
        
        MultiKeyMap<String> map = new MultiKeyMap<>(CAPACITY);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        
        // All threads write to the same key
        Class<?> source = String.class;
        Class<?> target = Integer.class;
        long instanceId = 42L;
        
        for (int threadId = 0; threadId < NUM_THREADS; threadId++) {
            final int id = threadId;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        String value = "thread" + id + "-op" + i;
                        map.put(value, source, target, instanceId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            thread.start();
        }
        
        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        
        // Verify the map has exactly one entry for this key
        assertEquals(1, map.size(), "Should have exactly one entry for the shared key");
        
        String finalValue = map.get(source, target, instanceId);
        assertNotNull(finalValue, "Final value should not be null");
        assertTrue(finalValue.startsWith("thread"), "Final value should be from one of the threads");
        
        LOG.info("Final value: " + finalValue);
        LOG.info("Map size: " + map.size());
    }
    
    @Test
    void testHighContentionScenario() throws InterruptedException {
        LOG.info("=== High Contention Scenario Test ===");
        LOG.info("This test uses LIMITED KEY SET to force high lock contention");
        
        MultiKeyMap<String> map = new MultiKeyMap<>(CAPACITY);
        ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger completedOps = new AtomicInteger(0);
        AtomicInteger writeOps = new AtomicInteger(0);
        AtomicInteger readOps = new AtomicInteger(0);
        
        // Limited set of keys to force high contention while ensuring better stripe distribution
        // Note: Using diverse Class types and prime numbers for instanceIds to avoid hash clustering
        // that can occur with consecutive values (0,1,2) and similar wrapper classes
        Class<?>[] sources = {String.class, Integer.class, Long.class, Double.class, Boolean.class};
        Class<?>[] targets = {Byte.class, Short.class, Float.class, Character.class, java.util.List.class};
        long[] instanceIds = {7L, 23L, 47L, 89L, 157L};  // Prime numbers for better hash distribution
        
        LOG.info("Key combinations: " + (sources.length * targets.length * instanceIds.length) + 
                          " (designed to create contention)");
        
        long testStartTime = System.nanoTime();
        
        for (int threadId = 0; threadId < NUM_THREADS; threadId++) {
            final int id = threadId;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    Random random = new Random(id * 54321);
                    
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        Class<?> source = sources[random.nextInt(sources.length)];
                        Class<?> target = targets[random.nextInt(targets.length)];
                        long instanceId = instanceIds[random.nextInt(instanceIds.length)];
                        
                        if (random.nextFloat() < 0.7f) { // 70% writes, 30% reads
                            String value = "thread" + id + "-op" + i;
                            map.put(value, source, target, instanceId);
                            writeOps.incrementAndGet();
                        } else {
                            map.get(source, target, instanceId);
                            readOps.incrementAndGet();
                        }
                        
                        completedOps.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        startLatch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        
        long testEndTime = System.nanoTime();
        long totalTime = testEndTime - testStartTime;
        
        LOG.info("High contention test completed in " + (totalTime / 1_000_000) + "ms");
        LOG.info("Operations per second: " + (completedOps.get() * 1_000_000_000L / totalTime));
        LOG.info("Completed operations: " + completedOps.get());
        LOG.info("Write operations: " + writeOps.get());
        LOG.info("Read operations: " + readOps.get());
        LOG.info("Final map size: " + map.size());
        
        map.printContentionStatistics();
        
        // Verify all expected operations completed
        assertEquals(NUM_THREADS * OPERATIONS_PER_THREAD, completedOps.get());
        assertEquals(writeOps.get() + readOps.get(), completedOps.get());
    }
    
    @Test
    void testLongRunningStressTest() throws InterruptedException {
        LOG.info("=== Long Running Stress Test ===");
        
        MultiKeyMap<String> map = new MultiKeyMap<>(CAPACITY);
        AtomicBoolean shouldStop = new AtomicBoolean(false);
        AtomicLong totalOperations = new AtomicLong(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        List<Thread> threads = new ArrayList<>();
        
        // Create worker threads
        for (int threadId = 0; threadId < NUM_THREADS; threadId++) {
            final int id = threadId;
            Thread thread = new Thread(() -> {
                Random random = new Random(id * 98765);
                long ops = 0;
                
                try {
                    while (!shouldStop.get()) {
                        Class<?> source = getRandomClass(random);
                        Class<?> target = getRandomClass(random);
                        long instanceId = random.nextInt(20);
                        
                        if (random.nextFloat() < 0.8f) { // 80% writes
                            String value = "thread" + id + "-op" + ops;
                            map.put(value, source, target, instanceId);
                        } else { // 20% reads
                            map.get(source, target, instanceId);
                        }
                        ops++;
                        
                        // Occasional yield to allow other threads
                        if (ops % 100 == 0) {
                            Thread.yield();
                        }
                    }
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                    LOG.info("Thread " + id + " encountered exception: " + e.getMessage());
                } finally {
                    totalOperations.addAndGet(ops);
                }
            });
            threads.add(thread);
            thread.start();
        }
        
        // Let it run for the specified duration
        Thread.sleep(TEST_DURATION_SECONDS * 1000);
        shouldStop.set(true);
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(5000); // 5 second timeout per thread
        }
        
        LOG.info("Test duration: " + TEST_DURATION_SECONDS + " seconds");
        LOG.info("Total operations: " + totalOperations.get());
        LOG.info("Operations per second: " + (totalOperations.get() / TEST_DURATION_SECONDS));
        LOG.info("Exceptions encountered: " + exceptionCount.get());
        LOG.info("Final map size: " + map.size());
        
        map.printContentionStatistics();
        
        assertEquals(0, exceptionCount.get(), "Should not encounter any exceptions during stress test");
        assertTrue(totalOperations.get() > 0, "Should have completed some operations");
    }
    
    @Test
    void testDataIntegrity() throws InterruptedException {
        LOG.info("=== Data Integrity Test ===");
        
        MultiKeyMap<String> map = new MultiKeyMap<>(CAPACITY);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(NUM_THREADS);
        
        // Each thread writes unique keys, then we verify all are present
        Set<String> allExpectedKeys = Collections.synchronizedSet(new HashSet<>());
        
        for (int threadId = 0; threadId < NUM_THREADS; threadId++) {
            final int id = threadId;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        Class<?> source = String.class;
                        Class<?> target = Integer.class;
                        long instanceId = (long) id * OPERATIONS_PER_THREAD + i; // Unique per thread
                        String value = "thread" + id + "-op" + i;
                        
                        map.put(value, source, target, instanceId);
                        allExpectedKeys.add(makeKey(source, target, instanceId));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
            thread.start();
        }
        
        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        
        // Verify all keys are present and have correct values
        int foundCount = 0;
        for (String expectedKey : allExpectedKeys) {
            String[] parts = expectedKey.split(":");
            Class<?> source = String.class;
            Class<?> target = Integer.class;
            long instanceId = Long.parseLong(parts[2]);
            
            String value = map.get(source, target, instanceId);
            if (value != null) {
                foundCount++;
            }
        }
        
        LOG.info("Expected keys: " + allExpectedKeys.size());
        LOG.info("Found keys: " + foundCount);
        LOG.info("Map size: " + map.size());
        
        assertEquals(allExpectedKeys.size(), foundCount, "All expected keys should be found");
        assertEquals(allExpectedKeys.size(), map.size(), "Map size should match expected keys");
    }
    
    private Class<?> getRandomClass(Random random) {
        Class<?>[] classes = {
            String.class, Integer.class, Long.class, Double.class, Boolean.class,
            Byte.class, Short.class, Float.class, Character.class,
            List.class, Set.class, Map.class
        };
        return classes[random.nextInt(classes.length)];
    }
    
    private String makeKey(Class<?> source, Class<?> target, long instanceId) {
        return source.getSimpleName() + ":" + target.getSimpleName() + ":" + instanceId;
    }
}