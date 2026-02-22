package com.cedarsoftware.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Micro-benchmark to directly compare ConcurrentHashMap vs LRUCache operations.
 */
public class LRUCacheMicroBenchmark {

    private static final Logger LOG = Logger.getLogger(LRUCacheMicroBenchmark.class.getName());

    private static final int WARMUP = 100_000;
    private static final int ITERATIONS = 1_000_000;

    @Test
    public void compareRawOperations() {
        LOG.info(repeat("=", 80));
        LOG.info("Micro-benchmark: ConcurrentHashMap vs LRUCache THREADED");
        LOG.info(repeat("=", 80));

        int capacity = 10_000;
        String[] keys = new String[capacity];
        for (int i = 0; i < capacity; i++) {
            keys[i] = "key_" + i;
        }

        // Test 1: ConcurrentHashMap
        LOG.info("--- ConcurrentHashMap ---");
        ConcurrentHashMap<String, String> chm = new ConcurrentHashMap<>(capacity);

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            chm.put(keys[i % capacity], "value");
            chm.get(keys[i % capacity]);
        }
        chm.clear();

        // Measure PUT
        long start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            chm.put(keys[i % capacity], "value");
        }
        long chmPutTime = (System.nanoTime() - start) / ITERATIONS;

        // Measure GET
        start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            chm.get(keys[i % capacity]);
        }
        long chmGetTime = (System.nanoTime() - start) / ITERATIONS;

        LOG.info(String.format("  PUT: %3d ns/op", chmPutTime));
        LOG.info(String.format("  GET: %3d ns/op", chmGetTime));

        // Test 2: LRUCache THREADED
        LOG.info("--- LRUCache THREADED ---");
        LRUCache<String, String> lru = new LRUCache<>(capacity, LRUCache.StrategyType.THREADED);

        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            lru.put(keys[i % capacity], "value");
            lru.get(keys[i % capacity]);
        }
        lru.clear();

        // Measure PUT
        start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            lru.put(keys[i % capacity], "value");
        }
        long lruPutTime = (System.nanoTime() - start) / ITERATIONS;

        // Measure GET
        start = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            lru.get(keys[i % capacity]);
        }
        long lruGetTime = (System.nanoTime() - start) / ITERATIONS;

        LOG.info(String.format("  PUT: %3d ns/op", lruPutTime));
        LOG.info(String.format("  GET: %3d ns/op", lruGetTime));

        lru.shutdown();

        // Summary
        LOG.info(repeat("-", 80));
        LOG.info("COMPARISON");
        LOG.info(repeat("-", 80));
        LOG.info(String.format("PUT overhead: LRUCache is %.1fx slower (%d ns vs %d ns)",
                (double) lruPutTime / chmPutTime, lruPutTime, chmPutTime));
        LOG.info(String.format("GET overhead: LRUCache is %.1fx slower (%d ns vs %d ns)",
                (double) lruGetTime / chmGetTime, lruGetTime, chmGetTime));
    }

    @Test
    public void measureIndividualOverheads() {
        LOG.info(repeat("=", 80));
        LOG.info("Measuring individual overhead sources");
        LOG.info(repeat("=", 80));

        int iterations = 10_000_000;

        // 1. System.nanoTime() cost
        long start = System.nanoTime();
        long dummy = 0;
        for (int i = 0; i < iterations; i++) {
            dummy += System.nanoTime();
        }
        long nanoTimeCost = (System.nanoTime() - start) / iterations;
        LOG.info(String.format("System.nanoTime(): %d ns/op", nanoTimeCost));

        // 2. AtomicLong.incrementAndGet() cost
        java.util.concurrent.atomic.AtomicLong counter = new java.util.concurrent.atomic.AtomicLong(0);
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            counter.incrementAndGet();
        }
        long atomicIncCost = (System.nanoTime() - start) / iterations;
        LOG.info(String.format("AtomicLong.incrementAndGet(): %d ns/op", atomicIncCost));

        // 3. ThreadLocal.get() cost
        ThreadLocal<Integer> tl = ThreadLocal.withInitial(() -> 0);
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            tl.get();
        }
        long tlGetCost = (System.nanoTime() - start) / iterations;
        LOG.info(String.format("ThreadLocal.get(): %d ns/op", tlGetCost));

        // 4. ThreadLocal.set() cost
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            tl.set(i);
        }
        long tlSetCost = (System.nanoTime() - start) / iterations;
        LOG.info(String.format("ThreadLocal.set(): %d ns/op", tlSetCost));

        // 5. Object allocation cost (simple object)
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Object obj = new Object();
        }
        long allocCost = (System.nanoTime() - start) / iterations;
        LOG.info(String.format("new Object(): %d ns/op", allocCost));

        // 6. ConcurrentHashMap.get() cost
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        map.put("key", "value");
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.get("key");
        }
        long chmGetCost = (System.nanoTime() - start) / iterations;
        LOG.info(String.format("ConcurrentHashMap.get(): %d ns/op", chmGetCost));

        // 7. ConcurrentHashMap.put() cost (replacement)
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.put("key", "value");
        }
        long chmPutCost = (System.nanoTime() - start) / iterations;
        LOG.info(String.format("ConcurrentHashMap.put(): %d ns/op", chmPutCost));

        // 8. Volatile write cost
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            counter.set(i);
        }
        long volatileWriteCost = (System.nanoTime() - start) / iterations;
        LOG.info(String.format("Volatile write (AtomicLong.set()): %d ns/op", volatileWriteCost));

        // Prevent dead code elimination
        if (dummy == 0) LOG.info("dummy");
    }

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
