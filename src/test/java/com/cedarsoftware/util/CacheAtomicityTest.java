package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * merge, compute, computeIfPresent, replace and remove(key, value) on LRUCache (both strategies) and TTLCache
 * (size-limited and unbounded) are atomic: increments from many threads all count. They were the Map defaults, a
 * get then a put, so concurrent increments overwrote each other.
 */
class CacheAtomicityTest {

    private static final int THREADS = 8;
    private static final int PER_THREAD = 20_000;

    @AfterAll
    static void shutdown() {
        TTLCache.shutdown();
    }

    private static Map<String, Supplier<Map<String, Integer>>> caches() {
        Map<String, Supplier<Map<String, Integer>>> caches = new LinkedHashMap<>();
        caches.put("LRUCache LOCKING", () -> new LRUCache<>(100, LRUCache.StrategyType.LOCKING));
        caches.put("LRUCache THREADED", () -> new LRUCache<>(100, LRUCache.StrategyType.THREADED));
        caches.put("TTLCache size-limited", () -> new TTLCache<>(3_600_000, 100));
        caches.put("TTLCache unbounded", () -> new TTLCache<>(3_600_000));
        return caches;
    }

    private static void race(Map<String, Integer> cache, Consumer<Map<String, Integer>> increment) throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < THREADS; t++) {
            Thread thread = new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int i = 0; i < PER_THREAD; i++) {
                    increment.accept(cache);
                }
            });
            threads.add(thread);
            thread.start();
        }
        go.countDown();
        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    void mergeCountsEveryIncrement() throws Exception {
        for (Map.Entry<String, Supplier<Map<String, Integer>>> e : caches().entrySet()) {
            Map<String, Integer> cache = e.getValue().get();
            race(cache, c -> c.merge("k", 1, Integer::sum));
            assertEquals(Integer.valueOf(THREADS * PER_THREAD), cache.get("k"), e.getKey());
        }
    }

    @Test
    void computeCountsEveryIncrement() throws Exception {
        for (Map.Entry<String, Supplier<Map<String, Integer>>> e : caches().entrySet()) {
            Map<String, Integer> cache = e.getValue().get();
            race(cache, c -> c.compute("k", (k, v) -> v == null ? 1 : v + 1));
            assertEquals(Integer.valueOf(THREADS * PER_THREAD), cache.get("k"), e.getKey());
        }
    }

    @Test
    void computeIfPresentCountsEveryIncrement() throws Exception {
        for (Map.Entry<String, Supplier<Map<String, Integer>>> e : caches().entrySet()) {
            Map<String, Integer> cache = e.getValue().get();
            cache.put("k", 0);
            race(cache, c -> c.computeIfPresent("k", (k, v) -> v + 1));
            assertEquals(Integer.valueOf(THREADS * PER_THREAD), cache.get("k"), e.getKey());
        }
    }

    /** replace(key, old, new) as a compare-and-set: a lost race must report false, so retrying counts once. */
    @Test
    void replaceIsACompareAndSet() throws Exception {
        for (Map.Entry<String, Supplier<Map<String, Integer>>> e : caches().entrySet()) {
            Map<String, Integer> cache = e.getValue().get();
            cache.put("k", 0);
            race(cache, c -> {
                Integer v;
                do {
                    v = c.get("k");
                } while (!c.replace("k", v, v + 1));
            });
            assertEquals(Integer.valueOf(THREADS * PER_THREAD), cache.get("k"), e.getKey());
        }
    }

    /** remove(key, value) racing: exactly one of the threads removes each key. */
    @Test
    void removeOfAKeyAndValueSucceedsOnce() throws Exception {
        for (Map.Entry<String, Supplier<Map<String, Integer>>> e : caches().entrySet()) {
            Map<String, Integer> cache = e.getValue().get();
            AtomicInteger removed = new AtomicInteger();
            for (int r = 0; r < 200; r++) {
                cache.put("k", r);
                int value = r;
                CountDownLatch go = new CountDownLatch(1);
                List<Thread> threads = new ArrayList<>();
                for (int t = 0; t < THREADS; t++) {
                    Thread thread = new Thread(() -> {
                        try {
                            go.await();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                        if (cache.remove("k", value)) {
                            removed.incrementAndGet();
                        }
                    });
                    threads.add(thread);
                    thread.start();
                }
                go.countDown();
                for (Thread thread : threads) {
                    thread.join();
                }
                assertNull(cache.get("k"), e.getKey() + " round " + r);
            }
            assertEquals(200, removed.get(), e.getKey());
        }
    }

    /** The single-threaded answers are the Map contract's, null values included. */
    @Test
    void singleThreadedAnswersFollowTheMapContract() {
        for (Map.Entry<String, Supplier<Map<String, Integer>>> e : caches().entrySet()) {
            Map<String, Integer> cache = e.getValue().get();
            String n = e.getKey();
            assertEquals(Integer.valueOf(5), cache.merge("m", 5, Integer::sum), n);
            assertEquals(Integer.valueOf(7), cache.merge("m", 2, Integer::sum), n);
            assertNull(cache.merge("m", 1, (a, b) -> null), n);
            assertTrue(!cache.containsKey("m"), n);
            assertNull(cache.computeIfPresent("absent", (k, v) -> 1), n);
            assertTrue(!cache.containsKey("absent"), n);
            cache.put("nullValued", null);
            assertNull(cache.computeIfPresent("nullValued", (k, v) -> 1), n);
            assertEquals(Integer.valueOf(3), cache.compute("nullValued", (k, v) -> v == null ? 3 : -1), n);
            assertNull(cache.compute("nullValued", (k, v) -> null), n);
            assertTrue(!cache.containsKey("nullValued"), n);
            assertNull(cache.replace("absent", 1), n);
            assertTrue(!cache.containsKey("absent"), n);
            cache.put("r", 1);
            assertEquals(Integer.valueOf(1), cache.replace("r", 2), n);
            assertTrue(!cache.replace("r", 1, 3), n);
            assertTrue(cache.replace("r", 2, 3), n);
            assertEquals(Integer.valueOf(3), cache.get("r"), n);
            assertTrue(!cache.remove("r", 2), n);
            assertTrue(cache.remove("r", 3), n);
            assertTrue(!cache.containsKey("r"), n);
        }
    }

    /**
     * A function that writes the key it is computing breaks the Map contract, but must not hang the caller. The
     * compare-and-set loops retried such a function forever, since its own write overtook every attempt. After two
     * attempts they now run it under the map's own lock for the key: it completes, or the map refuses it.
     */
    @Test
    void aFunctionThatWritesItsOwnKeyCannotSpinForever() throws Exception {
        for (Map.Entry<String, Supplier<Map<String, Integer>>> e : caches().entrySet()) {
            for (String op : new String[]{"compute", "merge", "computeIfPresent"}) {
                Map<String, Integer> cache = e.getValue().get();
                cache.put("k", 0);
                Thread thread = new Thread(() -> {
                    try {
                        if (op.equals("compute")) {
                            cache.compute("k", (k, v) -> {
                                cache.put("k", 1);
                                return 2;
                            });
                        } else if (op.equals("merge")) {
                            cache.merge("k", 2, (a, b) -> {
                                cache.put("k", 1);
                                return b;
                            });
                        } else {
                            cache.computeIfPresent("k", (k, v) -> {
                                cache.put("k", 1);
                                return 2;
                            });
                        }
                    } catch (IllegalStateException | ConcurrentModificationException refused) {
                        // the backing map may refuse a function that modifies it
                    }
                });
                thread.setDaemon(true);
                thread.start();
                thread.join(5000);
                assertTrue(!thread.isAlive(), e.getKey() + " " + op + " is still running");
                cache.put("after", 3);
                assertEquals(Integer.valueOf(3), cache.get("after"), e.getKey() + " " + op);
            }
        }
    }

    /**
     * A function that writes the key it is computing (against the Map contract) must not leave a size-limited
     * cache holding two entries for one key (computeIfAbsent keeps the function's write: it loads outside the lock). LOCKING linked a second node for the key, past capacity for good:
     * capacity 3 then held 4 entries after any number of puts.
     */
    @Test
    void aFunctionThatWritesItsOwnKeyLeavesTheCacheWithinCapacity() {
        Map<String, Supplier<Map<String, Integer>>> limited = new LinkedHashMap<>();
        limited.put("LRUCache LOCKING", () -> new LRUCache<>(3, LRUCache.StrategyType.LOCKING));
        limited.put("TTLCache size-limited", () -> new TTLCache<>(3_600_000, 3));
        for (Map.Entry<String, Supplier<Map<String, Integer>>> e : limited.entrySet()) {
            for (String op : new String[]{"computeIfAbsent", "compute", "merge", "computeIfPresent"}) {
                Map<String, Integer> cache = e.getValue().get();
                if (op.equals("merge") || op.equals("computeIfPresent")) {
                    cache.put("k", 0);
                }
                if (op.equals("computeIfAbsent")) {
                    cache.computeIfAbsent("k", k -> {
                        cache.put("k", 1);
                        return 2;
                    });
                } else if (op.equals("compute")) {
                    cache.compute("k", (k, v) -> {
                        cache.remove("k");
                        cache.put("k", 1);
                        return 2;
                    });
                } else if (op.equals("merge")) {
                    cache.merge("k", 2, (a, b) -> {
                        cache.remove("k");
                        cache.put("k", 1);
                        return b;
                    });
                } else {
                    cache.computeIfPresent("k", (k, v) -> {
                        cache.remove("k");
                        cache.put("k", 1);
                        return 2;
                    });
                }
                // computeIfAbsent loads outside the cache's lock: a write made while its function ran is kept (from
                // another thread it is the last write in any order); compute, merge and computeIfPresent write theirs
                Integer expected = op.equals("computeIfAbsent") ? 1 : 2;
                assertEquals(expected, cache.get("k"), e.getKey() + " " + op);
                for (int i = 0; i < 10; i++) {
                    cache.put("x" + i, i);
                }
                assertEquals(3, cache.size(), e.getKey() + " " + op);
                assertEquals(new HashSet<>(Arrays.asList("x7", "x8", "x9")), cache.keySet(), e.getKey() + " " + op);
            }
        }
    }
}
