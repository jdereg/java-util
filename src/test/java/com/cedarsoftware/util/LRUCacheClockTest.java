package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * LRUCache's LOCKING strategy evicts by CLOCK (second chance): a read sets the entry's "used" flag, taking no lock and
 * moving nothing; eviction walks from the oldest entry, clears a flagged entry's flag and passes it over once, and
 * evicts the first unflagged one. Reads took the cache's lock to move the entry to the front (exact LRU), so every
 * read contended on one lock, and a read that found it held lost its use. Simulated against exact LRU over seven
 * request patterns, CLOCK's hit rate was never lower, and on skewed traffic its victims were among the least recent
 * tenth of the cache.
 */
class LRUCacheClockTest {

    /** A read made while another thread holds the cache's lock still counts as a use. */
    @Test
    void aReadWhileTheLockIsHeldIsNotLost() throws Exception {
        LRUCache<String, String> cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        Object strategy = strategyField.get(cache);
        Field lockField = strategy.getClass().getDeclaredField("lock");
        lockField.setAccessible(true);
        Lock lock = (Lock) lockField.get(strategy);

        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread holder = new Thread(() -> {
            lock.lock();
            try {
                held.countDown();
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                // test thread
            } finally {
                lock.unlock();
            }
        });
        holder.setDaemon(true);
        holder.start();
        assertTrue(held.await(5, TimeUnit.SECONDS));
        assertTrue("1".equals(cache.get("a")));      // "a" is the oldest entry, read while the lock is held
        release.countDown();
        holder.join(5000);

        cache.put("d", "4");                          // full: one entry goes, and it is not the one just read
        assertTrue(cache.containsKey("a"), "the entry read under contention was evicted: " + cache.keySet());
        assertFalse(cache.containsKey("b"));
    }

    /**
     * CLOCK stays close to exact LRU: on a skewed request stream (Zipf 0.99 over 10,000 keys, a cache of 1,000), the
     * cache's hit rate is within half a point of LinkedHashMap's exact LRU (it measured about one point higher).
     */
    @Test
    void theHitRateStaysWithinHalfAPointOfExactLru() {
        int keys = 10_000;
        int capacity = 1000;
        double[] cdf = new double[keys];
        double sum = 0;
        for (int i = 0; i < keys; i++) {
            sum += 1.0 / Math.pow(i + 1, 0.99);
            cdf[i] = sum;
        }
        java.util.Random random = new java.util.Random(42);
        LRUCache<Integer, Boolean> cache = new LRUCache<>(capacity, LRUCache.StrategyType.LOCKING);
        java.util.LinkedHashMap<Integer, Boolean> exact = new java.util.LinkedHashMap<Integer, Boolean>(2048, 0.75f,
                true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<Integer, Boolean> eldest) {
                return size() > capacity;
            }
        };
        int requests = 300_000;
        int cacheHits = 0;
        int exactHits = 0;
        for (int i = 0; i < requests; i++) {
            int index = java.util.Arrays.binarySearch(cdf, random.nextDouble() * sum);
            int key = index < 0 ? -index - 1 : index;
            if (cache.get(key) != null) {
                cacheHits++;
            } else {
                cache.put(key, Boolean.TRUE);
            }
            if (exact.get(key) != null) {
                exactHits++;
            } else {
                exact.put(key, Boolean.TRUE);
            }
        }
        double cacheRate = cacheHits * 100.0 / requests;
        double exactRate = exactHits * 100.0 / requests;
        assertTrue(cacheRate >= exactRate - 0.5, "hit rate " + cacheRate + "% against exact LRU's " + exactRate + "%");
    }

    /**
     * The second-chance rule, on a sequence where it differs from exact LRU: a, b, c in; a and c read; d in (b goes,
     * a's flag is spent); a read again; e in. Exact LRU evicts c, read before d went in. CLOCK evicts d, never read:
     * a and c were read since they were last passed over.
     */
    @Test
    void evictionGivesReadEntriesASecondChance() {
        LRUCache<String, String> cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");
        cache.get("a");
        cache.get("c");
        cache.put("d", "4");
        assertFalse(cache.containsKey("b"));
        cache.get("a");
        cache.put("e", "5");
        assertTrue(cache.containsKey("a") && cache.containsKey("c") && cache.containsKey("e"), cache.keySet()
                .toString());
        assertFalse(cache.containsKey("d"));
    }
}
