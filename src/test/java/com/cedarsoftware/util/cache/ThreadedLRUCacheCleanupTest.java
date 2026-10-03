package com.cedarsoftware.util.cache;

import java.util.concurrent.atomic.AtomicBoolean;

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

    /**
     * A writer faster than the cleanup cycle must not grow the cache without bound. The cleanup thread runs every
     * 500 ms, and one writer put millions of entries in between (an OutOfMemoryError within a second on a 1 GB
     * heap). Past twice its capacity, a writer now trims the cache back, whichever method inserted the key; if the
     * cleanup thread is evicting at that moment the writer leaves the work to it, and past four times the capacity
     * it waits and trims. One new node in 32 looks at the size, so the cache can pass those marks by a few dozen.
     */
    @Test
    void aFastWriterCannotGrowTheCachePastTwiceItsCapacity() {
        for (String method : new String[]{"put", "putIfAbsent", "computeIfAbsent"}) {
            ThreadedLRUCacheStrategy<Integer, Integer> cache = new ThreadedLRUCacheStrategy<>(1000);
            try {
                int most = 0;
                for (int i = 0; i < 2_000_000; i++) {
                    Integer key = i;
                    if (method.equals("put")) {
                        cache.put(key, key);
                    } else if (method.equals("putIfAbsent")) {
                        cache.putIfAbsent(key, key);
                    } else {
                        cache.computeIfAbsent(key, k -> k);
                    }
                    if ((i & 255) == 0) {
                        most = Math.max(most, cache.size());
                    }
                }
                most = Math.max(most, cache.size());
                assertTrue(most <= 5000, method + ": capacity 1,000, grew to " + most);
            } finally {
                cache.close();
            }
        }
    }

    /**
     * The trimming writer and the evictor (the cleanup thread, here forceCleanup in a loop) must not shut each
     * other out. The evictor held the eviction lock for its whole pass, up to 400 ms, evicting one sampled entry at
     * a time, while the writer's attempts to trim found the lock taken; the cache grew to hundreds of thousands of
     * entries. The evictor now takes the lock per step and evicts in bulk past twice the capacity, and past four
     * times a writer waits for the lock and trims.
     */
    @Test
    void aWriterStillTrimsWhileTheEvictorIsWorking() throws Exception {
        ThreadedLRUCacheStrategy<Integer, Integer> cache = new ThreadedLRUCacheStrategy<>(1000);
        AtomicBoolean stop = new AtomicBoolean();
        Thread evictor = new Thread(() -> {
            while (!stop.get()) {
                cache.forceCleanup();
            }
        });
        try {
            evictor.start();
            int most = 0;
            for (int i = 0; i < 2_000_000; i++) {
                cache.put(i, i);
                if ((i & 255) == 0) {
                    most = Math.max(most, cache.size());
                }
            }
            assertTrue(most <= 5000, "capacity 1,000, grew to " + most);
        } finally {
            stop.set(true);
            evictor.join();
            cache.close();
        }
    }

    /**
     * Every read is recorded. Entries read once after a hundred newer ones were inserted must count as more recent
     * than those, so the newcomers are what sampled eviction finds oldest. Reads used to be recorded one time in
     * eight, so most of the entries just read still looked older than the newcomers and were evicted in their
     * place. The keys are spread over the table (a sample is ten neighbouring entries, and keys with consecutive
     * hash codes are neighbours), so a sample holds newcomers and older entries in proportion.
     */
    @Test
    void anEntryReadOnceAfterNewerInsertsCountsAsMoreRecent() {
        ThreadedLRUCacheStrategy<Integer, Integer> cache = new ThreadedLRUCacheStrategy<>(1000);
        try {
            for (int i = 0; i < 1000; i++) {
                cache.put(scattered(i), i);
            }
            for (int i = 1000; i < 1100; i++) {
                cache.put(scattered(i), i);
            }
            for (int i = 0; i < 1000; i++) {
                cache.get(scattered(i));
            }
            cache.forceCleanup();
            int newcomersEvicted = 0;
            for (int i = 1000; i < 1100; i++) {
                if (!cache.containsKey(scattered(i))) {
                    newcomersEvicted++;
                }
            }
            assertTrue(newcomersEvicted >= 30, "of 100 evictions, " + newcomersEvicted + " were newcomers");
        } finally {
            cache.close();
        }
    }

    private static Integer scattered(int i) {
        return i * 0x9E3779B1;                      // Fibonacci hashing: consecutive i land far apart
    }
}
