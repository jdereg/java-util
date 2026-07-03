package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThreadedLRUCacheStrategyTest {

    /**
     * Recency tracking must survive access: a frequently-accessed old entry must NOT be evicted
     * ahead of cold, more-recently-inserted entries. Regression guard for the {@code updateTimestamp}
     * freeze bug, where gating the timestamp refresh on the node's own (frozen) timestamp
     * (`this.timestamp & 0x7`) turned the cache from LRU into FIFO — a key hammered thousands of
     * times was still evicted first because its recency stamp never advanced past insertion time.
     */
    @Test
    void hotButOldEntrySurvivesEvictionOverColdRecentEntries() {
        int capacity = 100;
        int survived = 0;
        int trials = 40;
        for (int t = 0; t < trials; t++) {
            ThreadedLRUCacheStrategy<String, Integer> cache = new ThreadedLRUCacheStrategy<>(capacity);
            try {
                for (int i = 0; i < capacity; i++) {
                    cache.put("k" + i, i);          // k0 is the oldest by insertion
                }
                // Hammer k0 so, under real LRU, it becomes the most-recently-used entry.
                for (int i = 0; i < 50_000; i++) {
                    assertNotNull(cache.get("k0"));
                }
                // Moderate overshoot (surplus <= capacity) → sample-10, timestamp-based eviction path.
                for (int i = 0; i < capacity / 2; i++) {
                    cache.put("x" + i, i);
                }
                cache.forceCleanup();               // do the eviction the elves would do
                if (cache.get("k0") != null) {
                    survived++;
                }
            } finally {
                cache.shutdown();
            }
        }
        // With correct recency tracking this is ~100%; the frozen-timestamp bug scored 0%.
        assertTrue(survived >= (trials * 9) / 10,
                "hot-but-old key should survive eviction under LRU; survived " + survived + "/" + trials);
    }

    @Test
    void testGetCapacityReturnsConstructorValue() {
        ThreadedLRUCacheStrategy<Integer, String> cache = new ThreadedLRUCacheStrategy<>(5, 50);
        assertEquals(5, cache.getCapacity());
    }

    @Test
    void testGetCapacityAfterPuts() {
        ThreadedLRUCacheStrategy<Integer, String> cache = new ThreadedLRUCacheStrategy<>(2, 50);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        assertEquals(2, cache.getCapacity());
    }
}
