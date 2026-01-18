package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

/**
 * Performance test to verify LRUCache doesn't have O(n^2) behavior.
 * Tests at multiple sizes to detect scaling issues.
 */
public class LRUCachePerformanceTest {

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }

    @Test
    public void testLRUCacheScaling() {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("LRUCache (THREADED) Scaling Test - Checking for O(n^2) behavior");
        System.out.println(repeat("=", 80));

        int[] sizes = {1_000, 5_000, 10_000, 50_000};
        long[] putTimes = new long[sizes.length];
        long[] getTimes = new long[sizes.length];

        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            LRUCache<String, String> cache = new LRUCache<>(size, LRUCache.StrategyType.THREADED);

            // Generate test keys
            String[] keys = new String[size];
            for (int j = 0; j < size; j++) {
                keys[j] = "key_" + j + "_" + Integer.toHexString(j * 31);
            }

            // Warmup
            for (int j = 0; j < Math.min(1000, size); j++) {
                cache.put(keys[j], "value");
                cache.get(keys[j]);
            }
            cache.clear();

            // Measure PUT
            long startPut = System.nanoTime();
            for (String key : keys) {
                cache.put(key, "value");
            }
            long endPut = System.nanoTime();
            putTimes[i] = (endPut - startPut) / size;

            // Measure GET
            long startGet = System.nanoTime();
            for (String key : keys) {
                cache.get(key);
            }
            long endGet = System.nanoTime();
            getTimes[i] = (endGet - startGet) / size;

            System.out.printf("\nSize %,6d: PUT %,4d ns/op, GET %,4d ns/op%n", size, putTimes[i], getTimes[i]);

            cache.shutdown();
        }

        // Check for O(n^2) behavior: if O(n^2), time per op would increase linearly with size
        // For O(1) or O(n), time per op should stay roughly constant
        System.out.println("\n" + repeat("-", 80));
        System.out.println("Scaling Analysis (comparing to smallest size):");
        System.out.println(repeat("-", 80));

        for (int i = 1; i < sizes.length; i++) {
            double sizeRatio = (double) sizes[i] / sizes[0];
            double putRatio = (double) putTimes[i] / putTimes[0];
            double getRatio = (double) getTimes[i] / getTimes[0];

            String putStatus = putRatio < sizeRatio * 0.5 ? "✓ O(1)" : (putRatio < sizeRatio ? "~ O(log n)" : "⚠ O(n) or worse");
            String getStatus = getRatio < sizeRatio * 0.5 ? "✓ O(1)" : (getRatio < sizeRatio ? "~ O(log n)" : "⚠ O(n) or worse");

            System.out.printf("Size %,6d vs %,6d (%.1fx size): PUT %.2fx %s, GET %.2fx %s%n",
                    sizes[i], sizes[0], sizeRatio, putRatio, putStatus, getRatio, getStatus);
        }

        System.out.println("\n" + repeat("=", 80));
        System.out.println("If O(n^2): time/op would grow linearly with size (e.g., 50x size = 50x time/op)");
        System.out.println("If O(1):   time/op stays constant regardless of size");
        System.out.println(repeat("=", 80));
    }
}
