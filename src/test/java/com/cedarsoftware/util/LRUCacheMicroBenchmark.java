package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Micro-benchmark to directly compare ConcurrentHashMap vs LRUCache operations.
 */
public class LRUCacheMicroBenchmark {

    private static final int WARMUP = 100_000;
    private static final int ITERATIONS = 1_000_000;

    @Test
    public void compareRawOperations() {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("Micro-benchmark: ConcurrentHashMap vs LRUCache THREADED");
        System.out.println(repeat("=", 80));

        int capacity = 10_000;
        String[] keys = new String[capacity];
        for (int i = 0; i < capacity; i++) {
            keys[i] = "key_" + i;
        }

        // Test 1: ConcurrentHashMap
        System.out.println("\n--- ConcurrentHashMap ---");
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

        System.out.printf("  PUT: %3d ns/op%n", chmPutTime);
        System.out.printf("  GET: %3d ns/op%n", chmGetTime);

        // Test 2: LRUCache THREADED
        System.out.println("\n--- LRUCache THREADED ---");
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

        System.out.printf("  PUT: %3d ns/op%n", lruPutTime);
        System.out.printf("  GET: %3d ns/op%n", lruGetTime);

        lru.shutdown();

        // Summary
        System.out.println("\n" + repeat("-", 80));
        System.out.println("COMPARISON");
        System.out.println(repeat("-", 80));
        System.out.printf("PUT overhead: LRUCache is %.1fx slower (%d ns vs %d ns)%n",
                (double) lruPutTime / chmPutTime, lruPutTime, chmPutTime);
        System.out.printf("GET overhead: LRUCache is %.1fx slower (%d ns vs %d ns)%n",
                (double) lruGetTime / chmGetTime, lruGetTime, chmGetTime);
    }

    @Test
    public void measureIndividualOverheads() {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("Measuring individual overhead sources");
        System.out.println(repeat("=", 80));

        int iterations = 10_000_000;

        // 1. System.nanoTime() cost
        long start = System.nanoTime();
        long dummy = 0;
        for (int i = 0; i < iterations; i++) {
            dummy += System.nanoTime();
        }
        long nanoTimeCost = (System.nanoTime() - start) / iterations;
        System.out.printf("%nSystem.nanoTime(): %d ns/op%n", nanoTimeCost);

        // 2. AtomicLong.incrementAndGet() cost
        java.util.concurrent.atomic.AtomicLong counter = new java.util.concurrent.atomic.AtomicLong(0);
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            counter.incrementAndGet();
        }
        long atomicIncCost = (System.nanoTime() - start) / iterations;
        System.out.printf("AtomicLong.incrementAndGet(): %d ns/op%n", atomicIncCost);

        // 3. ThreadLocal.get() cost
        ThreadLocal<Integer> tl = ThreadLocal.withInitial(() -> 0);
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            tl.get();
        }
        long tlGetCost = (System.nanoTime() - start) / iterations;
        System.out.printf("ThreadLocal.get(): %d ns/op%n", tlGetCost);

        // 4. ThreadLocal.set() cost
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            tl.set(i);
        }
        long tlSetCost = (System.nanoTime() - start) / iterations;
        System.out.printf("ThreadLocal.set(): %d ns/op%n", tlSetCost);

        // 5. Object allocation cost (simple object)
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Object obj = new Object();
        }
        long allocCost = (System.nanoTime() - start) / iterations;
        System.out.printf("new Object(): %d ns/op%n", allocCost);

        // 6. ConcurrentHashMap.get() cost
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        map.put("key", "value");
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.get("key");
        }
        long chmGetCost = (System.nanoTime() - start) / iterations;
        System.out.printf("ConcurrentHashMap.get(): %d ns/op%n", chmGetCost);

        // 7. ConcurrentHashMap.put() cost (replacement)
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            map.put("key", "value");
        }
        long chmPutCost = (System.nanoTime() - start) / iterations;
        System.out.printf("ConcurrentHashMap.put(): %d ns/op%n", chmPutCost);

        // 8. Volatile write cost
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            counter.set(i);
        }
        long volatileWriteCost = (System.nanoTime() - start) / iterations;
        System.out.printf("Volatile write (AtomicLong.set()): %d ns/op%n", volatileWriteCost);

        // Prevent dead code elimination
        if (dummy == 0) System.out.println("dummy");
    }

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
