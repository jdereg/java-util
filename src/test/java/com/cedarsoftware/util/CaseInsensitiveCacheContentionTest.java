package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Benchmark to measure LRU cache contention in CaseInsensitiveMap.
 * Tests the performance impact of the CaseInsensitiveString cache under concurrent access.
 */
public class CaseInsensitiveCacheContentionTest {

    private static final int WARMUP_ITERATIONS = 50_000;
    private static final int MEASUREMENT_ITERATIONS = 500_000;
    private static final int PROGRESS_INTERVAL = 20_000;

    @AfterEach
    public void cleanup() {
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    @Test
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    public void measureCacheContention() throws Exception {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("CaseInsensitiveString Cache Contention Analysis");
        System.out.println(repeat("=", 80));

        int[] threadCounts = {1, 2, 4, 8, 16};

        for (int threads : threadCounts) {
            System.out.println("\n" + repeat("-", 60));
            System.out.println("Thread count: " + threads);
            System.out.println(repeat("-", 60));

            // Test 1: Cache hits (same keys repeatedly)
            long cacheHitTime = measureCacheHits(threads);

            // Test 2: Cache misses (unique keys)
            long cacheMissTime = measureCacheMisses(threads);

            // Test 3: Mixed workload (realistic)
            long mixedTime = measureMixedWorkload(threads);

            System.out.printf("  Cache hits (same 100 keys):     %,10d ns/op\n", cacheHitTime);
            System.out.printf("  Cache misses (unique keys):     %,10d ns/op\n", cacheMissTime);
            System.out.printf("  Mixed workload (80/20 hit/miss):%,10d ns/op\n", mixedTime);
        }

        // Show scaling analysis
        System.out.println("\n" + repeat("=", 80));
        System.out.println("Scaling Analysis (comparing single-threaded to multi-threaded)");
        System.out.println(repeat("=", 80));

        long singleThreaded = measureCacheHits(1);
        long eightThreads = measureCacheHits(8);
        double scalingFactor = (double) eightThreads / singleThreaded;

        System.out.printf("\nSingle-threaded: %,d ns/op\n", singleThreaded);
        System.out.printf("8 threads:       %,d ns/op\n", eightThreads);
        System.out.printf("Scaling factor:  %.2fx (ideal = 1.0x, >1.0 indicates contention)\n", scalingFactor);

        if (scalingFactor > 2.0) {
            System.out.println("\nWARNING: Significant contention detected!");
        } else if (scalingFactor > 1.5) {
            System.out.println("\nNOTE: Moderate contention detected.");
        } else {
            System.out.println("\nGOOD: Low contention.");
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    public void compareWithAndWithoutCache() throws Exception {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("Cache vs No-Cache Performance Comparison");
        System.out.println(repeat("=", 80));

        int threads = 8;

        // Warm up
        measureCacheHits(threads);

        // Test with cache (default)
        System.out.println("\n--- With LRU Cache (default) ---");
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
        long withCacheTime = measureMapOperations(threads, true);

        // Test with very large cache (essentially unbounded ConcurrentHashMap behavior)
        System.out.println("\n--- With Large ConcurrentHashMap-backed Cache ---");
        CaseInsensitiveMap.replaceCache(new LRUCache<>(1_000_000, LRUCache.StrategyType.THREADED));
        long largeCacheTime = measureMapOperations(threads, true);

        // Test bypassing cache entirely (set max length to 0-like behavior)
        System.out.println("\n--- Bypassing Cache (very short max length) ---");
        CaseInsensitiveMap.setMaxCacheLengthString(10); // Only cache very short strings
        long noCacheTime = measureMapOperations(threads, true);

        System.out.println("\n" + repeat("-", 60));
        System.out.println("Results Summary (8 threads):");
        System.out.printf("  With 5K LRU Cache:     %,10d ns/op\n", withCacheTime);
        System.out.printf("  With 1M entry cache:   %,10d ns/op\n", largeCacheTime);
        System.out.printf("  Bypassing cache:       %,10d ns/op\n", noCacheTime);

        double cacheOverhead = (double) withCacheTime / noCacheTime;
        System.out.printf("\nCache overhead factor: %.2fx\n", cacheOverhead);
    }

    @Test
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    public void compareThreadedVsLockingStrategy() throws Exception {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("THREADED vs LOCKING LRU Strategy Comparison");
        System.out.println(repeat("=", 80));

        int[] threadCounts = {1, 2, 4, 8, 16};

        System.out.println("\nCache Hits (same keys, read-heavy):");
        System.out.println(repeat("-", 70));
        System.out.printf("%-10s %15s %15s %15s\n", "Threads", "THREADED", "LOCKING", "Difference");
        System.out.println(repeat("-", 70));

        for (int threads : threadCounts) {
            // Test THREADED strategy
            CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
            long threadedTime = measureCacheHitsInternal(threads);

            // Test LOCKING strategy
            CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.LOCKING));
            long lockingTime = measureCacheHitsInternal(threads);

            double ratio = (double) threadedTime / lockingTime;
            String winner = ratio > 1.0 ? "LOCKING wins" : "THREADED wins";

            System.out.printf("%-10d %,12d ns %,12d ns %12.2fx (%s)\n",
                    threads, threadedTime, lockingTime, ratio, winner);
        }

        System.out.println("\n\nMixed Workload (80% hits, 20% misses):");
        System.out.println(repeat("-", 70));
        System.out.printf("%-10s %15s %15s %15s\n", "Threads", "THREADED", "LOCKING", "Difference");
        System.out.println(repeat("-", 70));

        for (int threads : threadCounts) {
            // Test THREADED strategy
            CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
            long threadedTime = measureMixedWorkloadInternal(threads);

            // Test LOCKING strategy
            CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.LOCKING));
            long lockingTime = measureMixedWorkloadInternal(threads);

            double ratio = (double) threadedTime / lockingTime;
            String winner = ratio > 1.0 ? "LOCKING wins" : "THREADED wins";

            System.out.printf("%-10d %,12d ns %,12d ns %12.2fx (%s)\n",
                    threads, threadedTime, lockingTime, ratio, winner);
        }

        System.out.println("\n\nCache Misses (unique keys, write-heavy):");
        System.out.println(repeat("-", 70));
        System.out.printf("%-10s %15s %15s %15s\n", "Threads", "THREADED", "LOCKING", "Difference");
        System.out.println(repeat("-", 70));

        for (int threads : threadCounts) {
            // Test THREADED strategy
            CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
            long threadedTime = measureCacheMissesInternal(threads);

            // Test LOCKING strategy
            CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.LOCKING));
            long lockingTime = measureCacheMissesInternal(threads);

            double ratio = (double) threadedTime / lockingTime;
            String winner = ratio > 1.0 ? "LOCKING wins" : "THREADED wins";

            System.out.printf("%-10d %,12d ns %,12d ns %12.2fx (%s)\n",
                    threads, threadedTime, lockingTime, ratio, winner);
        }
    }

    private long measureCacheHitsInternal(int threadCount) throws Exception {
        String[] keys = new String[100];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = "key_" + i;
        }

        // Warm up the cache
        CaseInsensitiveMap<String, String> warmupMap = new CaseInsensitiveMap<>();
        for (String key : keys) {
            warmupMap.put(key, "value");
        }

        // Warm up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            warmupMap.get(keys[i % keys.length]);
        }

        return measureConcurrentAccess(threadCount, keys, MEASUREMENT_ITERATIONS);
    }

    private long measureMixedWorkloadInternal(int threadCount) throws Exception {
        String[] hotKeys = new String[80];
        for (int i = 0; i < hotKeys.length; i++) {
            hotKeys[i] = "hot_key_" + i;
        }

        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        for (String key : hotKeys) {
            map.put(key, "value");
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int opsPerThread = MEASUREMENT_ITERATIONS / threadCount;
        AtomicLong coldKeyCounter = new AtomicLong(0);
        AtomicLong progress = new AtomicLong(0);

        // Warm up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            map.get(hotKeys[i % hotKeys.length]);
        }

        long startTime = System.nanoTime();

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < opsPerThread; i++) {
                    if (random.nextInt(100) < 80) {
                        map.get(hotKeys[random.nextInt(hotKeys.length)]);
                    } else {
                        String coldKey = "cold_" + threadId + "_" + coldKeyCounter.incrementAndGet();
                        map.put(coldKey, "value");
                    }
                    reportProgress(progress, MEASUREMENT_ITERATIONS, "MixedInternal");
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }

        long endTime = System.nanoTime();
        executor.shutdown();

        return (endTime - startTime) / MEASUREMENT_ITERATIONS;
    }

    private long measureCacheMissesInternal(int threadCount) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();

        int opsPerThread = MEASUREMENT_ITERATIONS / threadCount;
        AtomicLong progress = new AtomicLong(0);

        long startTime = System.nanoTime();

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                for (int i = 0; i < opsPerThread; i++) {
                    String key = "unique_" + threadId + "_" + i;
                    map.put(key, "value");
                    reportProgress(progress, MEASUREMENT_ITERATIONS, "CacheMissesInternal");
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }

        long endTime = System.nanoTime();
        executor.shutdown();

        return (endTime - startTime) / MEASUREMENT_ITERATIONS;
    }

    private long measureCacheHits(int threadCount) throws Exception {
        // Pre-populate with known keys
        String[] keys = new String[100];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = "key_" + i;
        }

        // Warm up the cache
        CaseInsensitiveMap<String, String> warmupMap = new CaseInsensitiveMap<>();
        for (String key : keys) {
            warmupMap.put(key, "value");
        }

        return measureConcurrentAccess(threadCount, keys, MEASUREMENT_ITERATIONS);
    }

    private long measureCacheMisses(int threadCount) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();

        int opsPerThread = MEASUREMENT_ITERATIONS / threadCount;
        AtomicLong progress = new AtomicLong(0);

        // Warm up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            map.put("warmup_" + i, "value");
        }
        map.clear();

        // Reset cache for clean measurement
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));

        long startTime = System.nanoTime();

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                for (int i = 0; i < opsPerThread; i++) {
                    String key = "unique_" + threadId + "_" + i;
                    map.put(key, "value");
                    reportProgress(progress, MEASUREMENT_ITERATIONS, "CacheMisses");
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }

        long endTime = System.nanoTime();
        executor.shutdown();

        return (endTime - startTime) / MEASUREMENT_ITERATIONS;
    }

    private long measureMixedWorkload(int threadCount) throws Exception {
        // 80% hits, 20% misses
        String[] hotKeys = new String[80];
        for (int i = 0; i < hotKeys.length; i++) {
            hotKeys[i] = "hot_key_" + i;
        }

        // Pre-populate hot keys
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        for (String key : hotKeys) {
            map.put(key, "value");
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int opsPerThread = MEASUREMENT_ITERATIONS / threadCount;
        AtomicLong coldKeyCounter = new AtomicLong(0);
        AtomicLong progress = new AtomicLong(0);

        // Warm up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            map.get(hotKeys[i % hotKeys.length]);
        }

        long startTime = System.nanoTime();

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < opsPerThread; i++) {
                    if (random.nextInt(100) < 80) {
                        // Cache hit - use hot key
                        map.get(hotKeys[random.nextInt(hotKeys.length)]);
                    } else {
                        // Cache miss - use unique key
                        String coldKey = "cold_" + threadId + "_" + coldKeyCounter.incrementAndGet();
                        map.put(coldKey, "value");
                    }
                    reportProgress(progress, MEASUREMENT_ITERATIONS, "MixedWorkload");
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }

        long endTime = System.nanoTime();
        executor.shutdown();

        return (endTime - startTime) / MEASUREMENT_ITERATIONS;
    }

    private long measureMapOperations(int threadCount, boolean includeWrites) throws Exception {
        String[] keys = new String[1000];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = "mapkey_" + i + "_suffix";
        }

        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();

        // Pre-populate
        for (String key : keys) {
            map.put(key, "value");
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int opsPerThread = MEASUREMENT_ITERATIONS / threadCount;
        AtomicLong progress = new AtomicLong(0);

        // Warm up
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            map.get(keys[i % keys.length]);
        }

        long startTime = System.nanoTime();

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            futures.add(executor.submit(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < opsPerThread; i++) {
                    String key = keys[random.nextInt(keys.length)];
                    if (includeWrites && random.nextInt(10) == 0) {
                        map.put(key, "newvalue");
                    } else {
                        map.get(key);
                    }
                    reportProgress(progress, MEASUREMENT_ITERATIONS, "MapOperations");
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }

        long endTime = System.nanoTime();
        executor.shutdown();

        return (endTime - startTime) / MEASUREMENT_ITERATIONS;
    }

    private long measureConcurrentAccess(int threadCount, String[] keys, int totalOps) throws Exception {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();

        // Pre-populate map
        for (String key : keys) {
            map.put(key, "value");
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        int opsPerThread = totalOps / threadCount;
        AtomicLong progress = new AtomicLong(0);

        long startTime = System.nanoTime();

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            futures.add(executor.submit(() -> {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                for (int i = 0; i < opsPerThread; i++) {
                    String key = keys[random.nextInt(keys.length)];
                    map.get(key);
                    reportProgress(progress, totalOps, "CacheHits");
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get();
        }

        long endTime = System.nanoTime();
        executor.shutdown();

        return (endTime - startTime) / totalOps;
    }

    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    /**
     * Reports progress periodically during long-running measurement loops.
     * Only prints when the count crosses a PROGRESS_INTERVAL boundary.
     */
    private static void reportProgress(AtomicLong progressCounter, int total, String testName) {
        long count = progressCounter.incrementAndGet();
        if (count % PROGRESS_INTERVAL == 0) {
            double pct = (100.0 * count) / total;
            System.out.printf("  [%s] Progress: %,d / %,d (%.1f%%)\n", testName, count, total, pct);
        }
    }
}
