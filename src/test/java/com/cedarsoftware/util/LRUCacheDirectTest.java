package com.cedarsoftware.util;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Direct test of LRUCache to understand its behavior and performance characteristics.
 */
public class LRUCacheDirectTest {

    private static final Logger LOG = Logger.getLogger(LRUCacheDirectTest.class.getName());

    @Test
    public void testLRUCacheBehaviorWhenFull() {
        LOG.info("=== LRUCache Behavior Analysis ===");

        int capacity = 1000;
        int numKeys = 5000;

        LRUCache<String, String> cache = new LRUCache<>(capacity, LRUCache.StrategyType.THREADED);

        LOG.info("Cache capacity: " + capacity);
        LOG.info("Number of unique keys: " + numKeys);
        LOG.info("softCap (1.5x): " + (int)(capacity * 1.5));
        LOG.info("hardCap (2.0x): " + (int)(capacity * 2.0));

        // Phase 1: Fill the cache
        LOG.info("--- Phase 1: Initial fill ---");
        long start = System.nanoTime();
        for (int i = 0; i < numKeys; i++) {
            cache.put("key" + i, "value" + i);
        }
        long elapsed = System.nanoTime() - start;
        LOG.info("After " + numKeys + " puts:");
        LOG.info("  Cache size: " + cache.size());
        LOG.info("  Time: " + (elapsed / 1_000_000) + " ms");
        LOG.info("  Per-op: " + (elapsed / numKeys) + " ns");

        // Wait for background cleanup
        LOG.info("--- Waiting 2 seconds for background cleanup ---");
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        LOG.info("  Cache size after cleanup: " + cache.size());

        // Phase 2: Access pattern - repeatedly access same keys
        LOG.info("--- Phase 2: Repeated access of first 100 keys ---");
        start = System.nanoTime();
        for (int round = 0; round < 100; round++) {
            for (int i = 0; i < 100; i++) {
                cache.get("key" + i);
            }
        }
        elapsed = System.nanoTime() - start;
        LOG.info("  10,000 gets: " + (elapsed / 10_000) + " ns/op");
        LOG.info("  Cache size: " + cache.size());

        // Phase 3: New keys (simulating working set change)
        LOG.info("--- Phase 3: Adding new keys (working set change) ---");
        start = System.nanoTime();
        for (int i = numKeys; i < numKeys + 1000; i++) {
            cache.put("key" + i, "value" + i);
        }
        elapsed = System.nanoTime() - start;
        LOG.info("  1000 new puts: " + (elapsed / 1000) + " ns/op");
        LOG.info("  Cache size: " + cache.size());

        // Wait for background cleanup
        LOG.info("--- Waiting 2 seconds for background cleanup ---");
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        LOG.info("  Cache size after cleanup: " + cache.size());

        // Phase 4: Check if new keys are cached
        LOG.info("--- Phase 4: Verify new keys are accessible ---");
        int hits = 0;
        for (int i = numKeys; i < numKeys + 1000; i++) {
            if (cache.get("key" + i) != null) hits++;
        }
        LOG.info("  New keys still in cache: " + hits + "/1000");

        // Phase 5: Check if old keys were evicted
        LOG.info("--- Phase 5: Check old keys ---");
        hits = 0;
        for (int i = 0; i < 100; i++) {
            if (cache.get("key" + i) != null) hits++;
        }
        LOG.info("  Old keys (0-99) still in cache: " + hits + "/100");

        cache.shutdown();
        LOG.info("=== Analysis Complete ===");
    }

    @Test
    public void testEvictionCost() {
        LOG.info("=== Eviction Cost Analysis ===");

        int capacity = 1000;
        LRUCache<String, String> cache = new LRUCache<>(capacity, LRUCache.StrategyType.THREADED);

        // Pre-fill to capacity
        for (int i = 0; i < capacity; i++) {
            cache.put("key" + i, "value" + i);
        }
        LOG.info("Pre-filled to capacity: " + cache.size());

        // Wait for any cleanup
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        LOG.info("After 1s wait: " + cache.size());

        // Now measure cost of puts that WILL trigger eviction
        // (we're at capacity, so adding new keys must evict)
        LOG.info("--- Measuring put cost when at capacity ---");

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

            LOG.info(String.format("  %d puts: %d ns/op, final size: %d",
                    batchSize, elapsed / batchSize, cache.size()));
        }

        cache.shutdown();
    }
}
