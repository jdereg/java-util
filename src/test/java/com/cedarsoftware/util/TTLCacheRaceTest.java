package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    /** A String-like key whose first hashCode() on each of the named threads waits until both threads are there. */
    static final class RendezvousKey {
        private final String text;
        private final Set<Thread> threads;
        private final CyclicBarrier barrier;

        RendezvousKey(String text, Set<Thread> threads, CyclicBarrier barrier) {
            this.text = text;
            this.threads = threads;
            this.barrier = barrier;
        }

        @Override
        public int hashCode() {
            if (threads.remove(Thread.currentThread())) {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            return text.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof RendezvousKey && ((RendezvousKey) o).text.equals(text);
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
     * A key that is mapped the whole time, its value replaced over and over by another thread: get() and values()
     * must never see null. A size-limited cache unlinks the replaced node, which must not clear the value a reader
     * already holding that node is about to return.
     */
    @Test
    void aKeyMappedTheWholeTimeIsNeverReadAsNull() throws Exception {
        TTLCache<String, Integer> cache = new TTLCache<>(3_600_000, 100, 3_600_000);
        try {
            cache.put("k", 0);
            AtomicBoolean stop = new AtomicBoolean();
            Thread writer = new Thread(() -> {
                for (int i = 1; !stop.get(); i++) {
                    cache.put("k", i);
                }
            });
            writer.start();
            long nullGets = 0;
            long nullValues = 0;
            long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(400);
            try {
                while (System.nanoTime() < end) {
                    for (int i = 0; i < 1000; i++) {
                        if (cache.get("k") == null) {
                            nullGets++;
                        }
                    }
                    if (cache.values().contains(null)) {
                        nullValues++;
                    }
                }
            } finally {
                stop.set(true);
                writer.join();
            }
            assertEquals(0, nullGets, "get(k) returned null");
            assertEquals(0, nullValues, "values() held null");
        } finally {
            cache.close();
        }
    }

    /**
     * clear() racing a put that holds the lock. Afterwards the key is either fully in the cache or fully out of it:
     * an entry left in the map but not in the LRU list could never be evicted, and reordering it later cut the
     * list. After more puts the cache must hold exactly the most recent ones.
     */
    @Test
    void clearRacingAPutLeavesTheCacheConsistent() throws Exception {
        TTLCache<Object, String> cache = new TTLCache<>(3_600_000, 3, 3_600_000);
        try {
            GateKey key = new GateKey("k");
            Thread putter = new Thread(() -> cache.put(key, "v"));
            key.arm(putter, 1);                     // put pauses inside the map write, holding the cache lock
            putter.start();
            key.awaitReached();
            Thread clearer = new Thread(cache::clear);
            clearer.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (clearer.getState() != Thread.State.WAITING && System.nanoTime() < deadline) {
                Thread.sleep(1);
            }
            key.release();
            putter.join();
            clearer.join();
            for (int i = 0; i < 100; i++) {
                cache.put("a" + i, "v");
            }
            cache.get(key);
            for (int i = 0; i < 100; i++) {
                cache.put("b" + i, "v");
            }
            assertEquals(new HashSet<>(Arrays.asList("b97", "b98", "b99")), cache.keySet());
        } finally {
            cache.close();
        }
    }

    /**
     * Unbounded, put() takes no lock, so putIfAbsent() must not overwrite a put that lands between its lookup and
     * its write: the put's value stays, and putIfAbsent reports it.
     */
    @Test
    void unboundedPutIfAbsentNeverOverwritesAConcurrentPut() throws Exception {
        TTLCache<Object, String> cache = new TTLCache<>(3_600_000, -1, 3_600_000);
        try {
            GateKey key = new GateKey("k");
            String[] returned = new String[1];
            Thread thread = new Thread(() -> returned[0] = cache.putIfAbsent(key, "fromPutIfAbsent"));
            key.arm(thread, 2);                     // between its lookup and its write
            thread.start();
            key.awaitReached();
            String previous = cache.put(key, "fromPut");
            key.release();
            thread.join();
            assertEquals(null, previous);
            assertEquals("fromPut", returned[0]);
            assertEquals("fromPut", cache.get(key));
        } finally {
            cache.close();
        }
    }

    /**
     * Unbounded, a put() landing while computeIfAbsent() runs its function must not be overwritten by the
     * computed value.
     */
    @Test
    void unboundedComputeIfAbsentNeverOverwritesAConcurrentPut() throws Exception {
        TTLCache<String, String> cache = new TTLCache<>(3_600_000, -1, 3_600_000);
        try {
            CountDownLatch computing = new CountDownLatch(1);
            CountDownLatch proceed = new CountDownLatch(1);
            String[] returned = new String[1];
            Thread thread = new Thread(() -> returned[0] = cache.computeIfAbsent("k", k -> {
                computing.countDown();
                try {
                    proceed.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "computed";
            }));
            thread.start();
            assertTrue(computing.await(10, TimeUnit.SECONDS));
            String previous = cache.put("k", "fromPut");
            proceed.countDown();
            thread.join();
            assertEquals(null, previous);
            assertEquals("fromPut", cache.get("k"));
            assertEquals("fromPut", returned[0]);
        } finally {
            cache.close();
        }
    }

    /**
     * Two size-limited caches compared with each other at the same moment, each holding live an entry the other
     * has expired. equals() held its own lock while asking the other cache, which takes the other's lock to drop
     * its expired entry: each thread waited for the other's lock forever.
     */
    @Test
    void twoCachesComparedWithEachOtherAtOnceDoNotDeadlock() throws Exception {
        TTLCache<Object, String> a = new TTLCache<>(TTL, 10, 3_600_000);
        TTLCache<Object, String> b = new TTLCache<>(TTL, 10, 3_600_000);
        boolean deadlocked = false;
        try {
            Set<Thread> comparing = ConcurrentHashMap.newKeySet();
            CyclicBarrier bothInside = new CyclicBarrier(2);
            RendezvousKey x = new RendezvousKey("x", comparing, bothInside);
            RendezvousKey y = new RendezvousKey("y", comparing, bothInside);
            b.put(x, "v");
            a.put(y, "v");
            Thread.sleep(TTL + 50);
            a.put(x, "v");                          // a: x live, y expired
            b.put(y, "v");                          // b: y live, x expired
            Thread first = new Thread(() -> a.equals(b));
            Thread second = new Thread(() -> b.equals(a));
            first.setDaemon(true);
            second.setDaemon(true);
            comparing.add(first);
            comparing.add(second);
            first.start();
            second.start();
            first.join(5000);
            second.join(5000);
            deadlocked = first.isAlive() || second.isAlive();
            assertTrue(!deadlocked, "a.equals(b) and b.equals(a) deadlocked");
        } finally {
            if (!deadlocked) {                      // close() takes the lock a deadlocked thread holds
                a.close();
                b.close();
            }
        }
    }

    /**
     * toString() and hashCode() run the values' own code. A slow one must not hold up writers: they ran under the
     * cache lock, which every write of a size-limited cache needs.
     */
    @Test
    void slowValueCodeInToStringOrHashCodeDoesNotBlockWriters() throws Exception {
        for (boolean viaToString : new boolean[]{true, false}) {
            TTLCache<String, Object> cache = new TTLCache<>(3_600_000, 10, 3_600_000);
            try {
                CountDownLatch inside = new CountDownLatch(1);
                CountDownLatch finish = new CountDownLatch(1);
                cache.put("slow", new Object() {
                    @Override
                    public String toString() {
                        await();
                        return "slow";
                    }

                    @Override
                    public int hashCode() {
                        await();
                        return 1;
                    }

                    private void await() {
                        inside.countDown();
                        try {
                            finish.await(10, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
                Thread reader = new Thread(viaToString ? cache::toString : cache::hashCode);
                reader.start();
                assertTrue(inside.await(10, TimeUnit.SECONDS));
                Thread writer = new Thread(() -> cache.put("other", "v"));
                writer.start();
                writer.join(2000);
                boolean blocked = writer.isAlive();
                finish.countDown();
                reader.join();
                writer.join();
                assertTrue(!blocked, (viaToString ? "toString" : "hashCode") + " held up a put");
            } finally {
                cache.close();
            }
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
