package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test that LRUCache THREADED strategy delivers fast, consistent put() throughput
 * with zero inline eviction, and that the background "elves" eventually drain
 * the cache back to capacity.
 *
 * Since the THREADED strategy now does pure ConcurrentHashMap delegation for puts
 * (zero eviction overhead), both early and late overflow batches should have nearly
 * identical throughput. The elves handle eviction asynchronously in the background.
 */
class LRUCacheOverflowTest {

    @Test
    void testNoPerformanceCliffAtCapacity() {
        int capacity = 10_000;
        int overflowPerBatch = 20_000;
        int rounds = 5;

        LRUCache<Integer, Integer> cache = new LRUCache<>(capacity, LRUCache.StrategyType.THREADED);

        // Warmup: fill, overflow, clear — lets JIT compile hot paths
        for (int i = 0; i < capacity + overflowPerBatch; i++) {
            cache.put(i, i);
        }
        cache.clear();

        int keyCounter = 0;
        long bestEarlyNsPerOp = Long.MAX_VALUE;
        long bestLateNsPerOp = Long.MAX_VALUE;

        for (int round = 0; round < rounds; round++) {
            cache.clear();

            // Fill to capacity
            for (int i = 0; i < capacity; i++) {
                cache.put(keyCounter++, i);
            }

            // Early overflow batch: first puts beyond capacity
            long start = System.nanoTime();
            for (int i = 0; i < overflowPerBatch; i++) {
                cache.put(keyCounter++, 1);
            }
            long earlyNsPerOp = (System.nanoTime() - start) / overflowPerBatch;

            // Late overflow batch: next puts (cache has been overflowing continuously)
            start = System.nanoTime();
            for (int i = 0; i < overflowPerBatch; i++) {
                cache.put(keyCounter++, 1);
            }
            long lateNsPerOp = (System.nanoTime() - start) / overflowPerBatch;

            bestEarlyNsPerOp = Math.min(bestEarlyNsPerOp, earlyNsPerOp);
            bestLateNsPerOp = Math.min(bestLateNsPerOp, lateNsPerOp);
        }

        // Report (using best times across rounds to filter GC/scheduling outliers)
        double ratio = (double) bestLateNsPerOp / Math.max(1, bestEarlyNsPerOp);
        System.out.println("LRUCache THREADED Overflow Performance Test (" + rounds + " rounds, best-of)");
        System.out.println("  Capacity:       " + capacity);
        System.out.println("  Early overflow: " + bestEarlyNsPerOp + " ns/op (" + overflowPerBatch + " puts)");
        System.out.println("  Late overflow:  " + bestLateNsPerOp + " ns/op (" + overflowPerBatch + " puts)");
        System.out.printf("  Ratio (late/early): %.1fx%n", ratio);
        System.out.println("  Cache size: " + cache.size());

        // Assert: late overflow should not be dramatically slower than early overflow.
        // With pure puts (no inline eviction), expect ~1x (both batches do the same work).
        // Using best-of-N rounds filters GC/JIT outliers; 5x threshold handles CI variance.
        assertTrue(bestLateNsPerOp < bestEarlyNsPerOp * 5,
                "Performance cliff detected! Late overflow (" + bestLateNsPerOp +
                        " ns/op) is more than 5x slower than early overflow (" + bestEarlyNsPerOp + " ns/op)." +
                        " Ratio: " + String.format("%.1fx", ratio));

        cache.shutdown();
    }

    @Test
    void testElvesEventuallyDrainToCapacity() throws InterruptedException {
        int capacity = 1_000;
        int overflow = 5_000;

        LRUCache<Integer, Integer> cache = new LRUCache<>(capacity, LRUCache.StrategyType.THREADED);

        // Fill well beyond capacity
        for (int i = 0; i < capacity + overflow; i++) {
            cache.put(i, i);
        }

        // Cache should be over capacity right now (puts don't evict)
        int sizeAfterBurst = cache.size();
        System.out.println("  Size after burst: " + sizeAfterBurst);
        assertTrue(sizeAfterBurst > capacity,
                "Expected cache to exceed capacity after burst, but size=" + sizeAfterBurst);

        // Wait for the elves to drain. At 500ms intervals with 10ms budget,
        // 5K excess should drain in a few cycles.
        long maxWaitMs = 10_000;  // generous timeout
        long deadline = System.currentTimeMillis() + maxWaitMs;
        while (cache.size() > capacity && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }

        int finalSize = cache.size();
        System.out.println("  Final size after drain: " + finalSize);

        // The elves should have brought it back to capacity (or close to it).
        // Allow a small tolerance for concurrent timing.
        assertTrue(finalSize <= capacity + 10,
                "Elves failed to drain cache! Expected ~" + capacity + " but got " + finalSize);

        cache.shutdown();
    }
}
