package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

/**
 * Test to identify exactly where the performance issue is:
 * - Is it LRUCache itself?
 * - Is it LRUCache when used for CaseInsensitiveString caching?
 * - Is it something else?
 */
public class CaseInsensitiveCacheTypeComparisonTest {

    private static final int SIZE = 10_000;
    private static final int ITERATIONS = 100_000;

    @Test
    public void reproduceOriginalSlowPerformance() {
        // This test tries to reproduce the original 9,000 ns/op slowdown
        // User says it happened specifically with map size of 10,000
        System.out.println("\n" + repeat("=", 90));
        System.out.println("Attempting to reproduce original 9,000 ns/op slowdown");
        System.out.println(repeat("=", 90));

        int mapSize = 10_000;
        int[] cacheSizes = {100, 500, 1_000, 5_000, 10_000};  // Test different cache sizes

        String[] testKeys = new String[mapSize];
        for (int i = 0; i < mapSize; i++) {
            String key = "Key_" + i + "_" + generateRandomString(10);
            testKeys[i] = key;
        }

        System.out.println("\nMap size: " + mapSize + " unique keys");
        System.out.println("Testing with different LRUCache sizes to find the issue...\n");

        for (int cacheSize : cacheSizes) {
            System.out.printf("--- Cache size: %,d (%.1fx map size) ---%n",
                    cacheSize, (double) cacheSize / mapSize);

            // Use LRUCache THREADED
            CaseInsensitiveMap.replaceCache(new LRUCache<>(cacheSize, LRUCache.StrategyType.THREADED));

            CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(
                    java.util.Collections.emptyMap(),
                    new java.util.concurrent.ConcurrentHashMap<>(mapSize)
            );

            // Measure PUT
            System.gc();
            long startPut = System.nanoTime();
            for (String key : testKeys) {
                ciMap.put(key, "value");
            }
            long endPut = System.nanoTime();
            long putTime = (endPut - startPut) / testKeys.length;

            // Measure GET
            System.gc();
            int iterations = 100_000;
            long startGet = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                ciMap.get(testKeys[i % testKeys.length]);
            }
            long endGet = System.nanoTime();
            long getTime = (endGet - startGet) / iterations;

            System.out.printf("  PUT: %,6d ns/op, GET: %,6d ns/op", putTime, getTime);
            if (putTime > 5000 || getTime > 5000) {
                System.out.println("  ⚠️ Cache pressure detected - consider larger cache");
            } else {
                System.out.println();
            }
        }

        CaseInsensitiveMap.resetCacheToDefault();
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        java.util.Random random = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            char c = (char) ('a' + random.nextInt(26));
            if (random.nextBoolean()) {
                c = Character.toUpperCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    @Test
    public void testWithOldStyleLRUCache() {
        // Simulate the OLD behavior: LRUCache with strict timestamp updates
        // by using LRUCache and doing many operations
        System.out.println("\n" + repeat("=", 90));
        System.out.println("Testing if LRUCache has O(n^2) behavior in CaseInsensitiveMap context");
        System.out.println(repeat("=", 90));

        int[] sizes = {1_000, 2_000, 5_000, 10_000};

        // Use LRUCache THREADED (current implementation with probabilistic timestamps)
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));

        System.out.println("\nWith LRUCache THREADED (probabilistic timestamps):");
        for (int size : sizes) {
            long time = measureMapOperationsForSize(size);
            System.out.printf("  Size %,6d: %,6d ns/op%n", size, time);
        }

        // Reset
        CaseInsensitiveMap.resetCacheToDefault();
    }

    private long measureMapOperationsForSize(int size) {
        String[] keys = new String[size];
        for (int i = 0; i < size; i++) {
            keys[i] = "Key_" + i + "_" + Integer.toHexString(i * 31);
        }

        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();

        // Measure PUT
        long start = System.nanoTime();
        for (String key : keys) {
            map.put(key, "value");
        }
        long end = System.nanoTime();

        return (end - start) / size;
    }

    @Test
    public void compareBackingCacheTypes() {
        System.out.println("\n" + repeat("=", 90));
        System.out.println("CaseInsensitiveMap Performance: LRUCache vs ConcurrentHashMap backing");
        System.out.println(repeat("=", 90));

        // Generate test keys
        String[] keys = new String[SIZE];
        for (int i = 0; i < SIZE; i++) {
            keys[i] = "TestKey_" + i + "_" + Integer.toHexString(i * 31);
        }

        // Test 1: With ConcurrentHashMap (current default)
        System.out.println("\n--- Test 1: ConcurrentHashMap backing (current default) ---");
        CaseInsensitiveMap.resetCacheToDefault();
        long concurrentMapTime = measureCaseInsensitiveMapPerformance(keys, "ConcurrentHashMap");

        // Test 2: With LRUCache THREADED
        System.out.println("\n--- Test 2: LRUCache THREADED backing ---");
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
        long lruThreadedTime = measureCaseInsensitiveMapPerformance(keys, "LRUCache THREADED");

        // Test 3: With LRUCache LOCKING
        System.out.println("\n--- Test 3: LRUCache LOCKING backing ---");
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.LOCKING));
        long lruLockingTime = measureCaseInsensitiveMapPerformance(keys, "LRUCache LOCKING");

        // Summary
        System.out.println("\n" + repeat("=", 90));
        System.out.println("SUMMARY");
        System.out.println(repeat("=", 90));
        System.out.printf("ConcurrentHashMap:  %,6d ns/op (baseline)%n", concurrentMapTime);
        System.out.printf("LRUCache THREADED:  %,6d ns/op (%.1fx slower)%n",
                lruThreadedTime, (double) lruThreadedTime / concurrentMapTime);
        System.out.printf("LRUCache LOCKING:   %,6d ns/op (%.1fx slower)%n",
                lruLockingTime, (double) lruLockingTime / concurrentMapTime);

        if (lruThreadedTime > concurrentMapTime * 10) {
            System.out.println("\n⚠️  LRUCache is significantly slower - there may be an issue!");
        } else {
            System.out.println("\n✓ All cache types perform reasonably well");
        }

        // Reset to default
        CaseInsensitiveMap.resetCacheToDefault();
    }

    private long measureCaseInsensitiveMapPerformance(String[] keys, String cacheType) {
        // Create a fresh map
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();

        // Warmup
        for (int i = 0; i < Math.min(1000, keys.length); i++) {
            map.put(keys[i], "value");
            map.get(keys[i]);
        }
        map.clear();

        // Measure PUT
        long startPut = System.nanoTime();
        for (String key : keys) {
            map.put(key, "value");
        }
        long endPut = System.nanoTime();
        long putTime = (endPut - startPut) / keys.length;

        // Measure GET (cache should be warm now)
        long startGet = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            map.get(keys[i % keys.length]);
        }
        long endGet = System.nanoTime();
        long getTime = (endGet - startGet) / ITERATIONS;

        System.out.printf("  %s: PUT %,d ns/op, GET %,d ns/op%n", cacheType, putTime, getTime);

        return (putTime + getTime) / 2;  // Average
    }

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
