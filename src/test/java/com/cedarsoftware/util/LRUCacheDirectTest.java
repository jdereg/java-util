package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

/**
 * Direct test of LRUCache to understand its behavior and performance characteristics.
 */
public class LRUCacheDirectTest {

    @Test
    public void testLRUCacheBehaviorWhenFull() {
        System.out.println("\n=== LRUCache Behavior Analysis ===\n");

        int capacity = 1000;
        int numKeys = 5000;

        LRUCache<String, String> cache = new LRUCache<>(capacity, LRUCache.StrategyType.THREADED);

        System.out.println("Cache capacity: " + capacity);
        System.out.println("Number of unique keys: " + numKeys);
        System.out.println("softCap (1.5x): " + (int)(capacity * 1.5));
        System.out.println("hardCap (2.0x): " + (int)(capacity * 2.0));

        // Phase 1: Fill the cache
        System.out.println("\n--- Phase 1: Initial fill ---");
        long start = System.nanoTime();
        for (int i = 0; i < numKeys; i++) {
            cache.put("key" + i, "value" + i);
        }
        long elapsed = System.nanoTime() - start;
        System.out.println("After " + numKeys + " puts:");
        System.out.println("  Cache size: " + cache.size());
        System.out.println("  Time: " + (elapsed / 1_000_000) + " ms");
        System.out.println("  Per-op: " + (elapsed / numKeys) + " ns");

        // Wait for background cleanup
        System.out.println("\n--- Waiting 2 seconds for background cleanup ---");
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        System.out.println("  Cache size after cleanup: " + cache.size());

        // Phase 2: Access pattern - repeatedly access same keys
        System.out.println("\n--- Phase 2: Repeated access of first 100 keys ---");
        start = System.nanoTime();
        for (int round = 0; round < 100; round++) {
            for (int i = 0; i < 100; i++) {
                cache.get("key" + i);
            }
        }
        elapsed = System.nanoTime() - start;
        System.out.println("  10,000 gets: " + (elapsed / 10_000) + " ns/op");
        System.out.println("  Cache size: " + cache.size());

        // Phase 3: New keys (simulating working set change)
        System.out.println("\n--- Phase 3: Adding new keys (working set change) ---");
        start = System.nanoTime();
        for (int i = numKeys; i < numKeys + 1000; i++) {
            cache.put("key" + i, "value" + i);
        }
        elapsed = System.nanoTime() - start;
        System.out.println("  1000 new puts: " + (elapsed / 1000) + " ns/op");
        System.out.println("  Cache size: " + cache.size());

        // Wait for background cleanup
        System.out.println("\n--- Waiting 2 seconds for background cleanup ---");
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        System.out.println("  Cache size after cleanup: " + cache.size());

        // Phase 4: Check if new keys are cached
        System.out.println("\n--- Phase 4: Verify new keys are accessible ---");
        int hits = 0;
        for (int i = numKeys; i < numKeys + 1000; i++) {
            if (cache.get("key" + i) != null) hits++;
        }
        System.out.println("  New keys still in cache: " + hits + "/1000");

        // Phase 5: Check if old keys were evicted
        System.out.println("\n--- Phase 5: Check old keys ---");
        hits = 0;
        for (int i = 0; i < 100; i++) {
            if (cache.get("key" + i) != null) hits++;
        }
        System.out.println("  Old keys (0-99) still in cache: " + hits + "/100");

        cache.shutdown();
        System.out.println("\n=== Analysis Complete ===");
    }

    @Test
    public void testEvictionCost() {
        System.out.println("\n=== Eviction Cost Analysis ===\n");

        int capacity = 1000;
        LRUCache<String, String> cache = new LRUCache<>(capacity, LRUCache.StrategyType.THREADED);

        // Pre-fill to capacity
        for (int i = 0; i < capacity; i++) {
            cache.put("key" + i, "value" + i);
        }
        System.out.println("Pre-filled to capacity: " + cache.size());

        // Wait for any cleanup
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        System.out.println("After 1s wait: " + cache.size());

        // Now measure cost of puts that WILL trigger eviction
        // (we're at capacity, so adding new keys must evict)
        System.out.println("\n--- Measuring put cost when at capacity ---");

        int[] batchSizes = {100, 500, 1000, 2000};
        for (int batchSize : batchSizes) {
            // Reset cache
            cache.clear();
            for (int i = 0; i < capacity; i++) {
                cache.put("key" + i, "value" + i);
            }
            try { Thread.sleep(500); } catch (InterruptedException e) {}

            // Measure
            long start = System.nanoTime();
            for (int i = 0; i < batchSize; i++) {
                cache.put("newkey" + i, "value");
            }
            long elapsed = System.nanoTime() - start;

            System.out.printf("  %d puts: %d ns/op, final size: %d%n",
                    batchSize, elapsed / batchSize, cache.size());
        }

        cache.shutdown();
    }
}
