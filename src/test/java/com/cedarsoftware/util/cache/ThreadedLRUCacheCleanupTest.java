package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The THREADED strategy's shared cleanup thread, which trims every THREADED cache in the JVM.
 */
class ThreadedLRUCacheCleanupTest {

    private static final String CLEANUP_THREAD = "LRUCache-Cleanup-Thread";

    /** A key whose hashCode() throws an Error on the cleanup thread while poisoned. */
    static final class PoisonKey {
        static volatile boolean poisoned;
        private final int id;

        PoisonKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            if (poisoned && Thread.currentThread().getName().equals(CLEANUP_THREAD)) {
                throw new AssertionError("an Error while evicting");
            }
            return id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof PoisonKey && ((PoisonKey) o).id == id;
        }
    }

    private static void awaitAtMost(ThreadedLRUCacheStrategy<?, ?> cache, int size, long millis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + millis;
        while (cache.size() > size && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
    }

    /**
     * An Error while trimming one cache (an OutOfMemoryError, a StackOverflowError from a key) must not end the
     * cleanup task: the executor silently cancels a periodic task that throws, and every THREADED cache in the
     * JVM then grew without bound.
     */
    @Test
    void anErrorWhileTrimmingDoesNotStopTheCleanupOfOtherCaches() throws Exception {
        ThreadedLRUCacheStrategy<Object, String> victim = new ThreadedLRUCacheStrategy<>(10);
        ThreadedLRUCacheStrategy<Object, String> other = new ThreadedLRUCacheStrategy<>(10);
        try {
            PoisonKey.poisoned = true;
            for (int i = 0; i < 20; i++) {
                victim.put(new PoisonKey(i), "v");
            }
            Thread.sleep(1200);                     // at least two cleanup cycles meet the Error
            PoisonKey.poisoned = false;
            victim.clear();
            for (int i = 0; i < 1000; i++) {
                other.put(i, "v");
            }
            awaitAtMost(other, 10, 3000);
            assertTrue(other.size() <= 10, "the cleanup thread stopped trimming: size " + other.size());
        } finally {
            PoisonKey.poisoned = false;
            victim.close();
            other.close();
        }
    }

    /**
     * Each sampled eviction started a new iteration at the front of the table, walking past the bins earlier
     * evictions had emptied, so evicting n entries cost on the order of n squared: a 100,000-entry surplus took
     * many 400 ms cleanup budgets and seconds of CPU. One cleanup pass now drains it.
     */
    @Test
    void oneCleanupPassDrainsASurplusAsLargeAsTheCapacity() {
        ThreadedLRUCacheStrategy<Integer, Integer> cache = new ThreadedLRUCacheStrategy<>(100_000);
        try {
            for (int i = 0; i < 200_000; i++) {
                cache.put(i, i);
            }
            cache.forceCleanup();
            assertTrue(cache.size() <= 100_000, "one cleanup pass left " + cache.size());
        } finally {
            cache.close();
        }
    }
}
