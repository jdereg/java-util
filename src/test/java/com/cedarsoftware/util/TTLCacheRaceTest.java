package com.cedarsoftware.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Interleavings of TTLCache operations that ordinary keys hit only by chance, forced with a key whose hashCode()
 * pauses one chosen thread: an expired entry being removed while a fresh value for the same key is written.
 */
class TTLCacheRaceTest {

    private static final long TTL = 200;

    /** A String-like key whose n-th hashCode() call on one chosen thread waits until released. */
    static final class GateKey {
        private final String text;
        private volatile Thread target;
        private volatile int remaining;
        private final CountDownLatch reached = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        GateKey(String text) {
            this.text = text;
        }

        void arm(Thread thread, int call) {
            remaining = call;
            target = thread;
        }

        void awaitReached() throws InterruptedException {
            assertTrue(reached.await(10, TimeUnit.SECONDS), "the paused thread never reached the gate");
        }

        void release() {
            release.countDown();
        }

        @Override
        public int hashCode() {
            if (Thread.currentThread() == target && --remaining == 0) {
                target = null;
                reached.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return text.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof GateKey && ((GateKey) o).text.equals(text);
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * The size() cleanup finds "old" expired; before it removes it, a put writes "fresh". Only "old" may go: the
     * fresh value must survive, and the cache must keep evicting down to maxSize afterwards.
     */
    @Test
    void sizeCleanupRemovesOnlyTheExpiredEntryItInspected() throws Exception {
        TTLCache<Object, String> cache = new TTLCache<>(TTL, 3, 3_600_000);
        try {
            GateKey key = new GateKey("k");
            cache.put(key, "old");
            Thread.sleep(TTL + 50);
            Thread cleaner = new Thread(cache::size);
            key.arm(cleaner, 1);
            cleaner.start();
            key.awaitReached();
            cache.put(key, "fresh");
            key.release();
            cleaner.join();

            assertEquals("fresh", cache.get(key));
            for (int i = 0; i < 50; i++) {
                cache.put("x" + i, "v");
            }
            assertTrue(cache.size() <= 3, "maxSize 3, size " + cache.size());
        } finally {
            cache.close();
        }
    }

    /** get() finds "old" expired; before it removes it, a put writes "fresh". The fresh value must survive. */
    @Test
    void getRemovesOnlyTheExpiredEntryItInspected() throws Exception {
        for (int maxSize : new int[]{-1, 10}) {
            assertFreshSurvivesExpiredRemoval(maxSize, true);
        }
    }

    /** containsKey() finds "old" expired; before it removes it, a put writes "fresh". The fresh value must survive. */
    @Test
    void containsKeyRemovesOnlyTheExpiredEntryItInspected() throws Exception {
        for (int maxSize : new int[]{-1, 10}) {
            assertFreshSurvivesExpiredRemoval(maxSize, false);
        }
    }

    private static void assertFreshSurvivesExpiredRemoval(int maxSize, boolean viaGet) throws Exception {
        TTLCache<Object, String> cache = new TTLCache<>(TTL, maxSize, 3_600_000);
        try {
            GateKey key = new GateKey("k");
            cache.put(key, "old");
            Thread.sleep(TTL + 50);
            Thread reader = new Thread(() -> {
                if (viaGet) {
                    cache.get(key);
                } else {
                    cache.containsKey(key);
                }
            });
            key.arm(reader, 2);     // the 1st hashCode() is the lookup, the 2nd is the removal
            reader.start();
            key.awaitReached();
            cache.put(key, "fresh");
            key.release();
            reader.join();
            assertEquals("fresh", cache.get(key), "maxSize " + maxSize + (viaGet ? " get" : " containsKey"));
        } finally {
            cache.close();
        }
    }

    /**
     * The cleanup has taken expired "old" out of the map but not yet out of the LRU list when a put writes "fresh"
     * for the same key and the next put evicts. Eviction meets the stale node first: it must not take "fresh" with
     * it, and must still bring the cache down to maxSize.
     */
    @Test
    void evictionNeverRemovesANewerEntryOfTheSameKey() throws Exception {
        TTLCache<Object, String> cache = new TTLCache<>(TTL, 2, 3_600_000);
        try {
            GateKey key = new GateKey("k");         // hash 107: iterated before "m" (109) in a 16-slot table
            GateKey other = new GateKey("m");
            cache.put(key, "old");
            cache.put(other, "old");
            Thread.sleep(TTL + 50);
            Thread cleaner = new Thread(cache::size);
            other.arm(cleaner, 1);                  // pause after k is out of the map, before its node is unlinked
            cleaner.start();
            other.awaitReached();
            cache.put(key, "fresh");
            cache.put("a", "1");                    // over maxSize: eviction starts at k's stale node
            other.release();
            cleaner.join();

            assertEquals("fresh", cache.get(key));
            assertEquals("1", cache.get("a"));
            assertEquals(2, cache.size());
        } finally {
            cache.close();
        }
    }
}
