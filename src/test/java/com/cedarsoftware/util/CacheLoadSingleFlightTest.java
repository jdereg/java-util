package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * computeIfAbsent loads single-flight per key, outside the cache-wide lock: loads of different keys run in parallel,
 * a key is loaded once however many threads ask for it at once, and no other operation waits for a loader. LRUCache
 * (LOCKING) and TTLCache ran the loader under the one lock of the cache, so loads ran one at a time and an unrelated
 * put waited for each (eight 50 ms loads took 469 ms, and a put waited 464 ms).
 */
class CacheLoadSingleFlightTest {

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final List<TTLCache<?, ?>> ttlCaches = new ArrayList<>();

    @AfterEach
    void shutDown() {
        pool.shutdownNow();
        for (TTLCache<?, ?> cache : ttlCaches) {
            cache.close();
        }
    }

    private List<Supplier<Map<String, String>>> caches() {
        List<Supplier<Map<String, String>>> caches = new ArrayList<>();
        caches.add(() -> new LRUCache<>(100, LRUCache.StrategyType.LOCKING));
        caches.add(() -> track(new TTLCache<>(60_000, 100)));
        caches.add(() -> track(new TTLCache<>(60_000)));
        return caches;
    }

    private <K, V> TTLCache<K, V> track(TTLCache<K, V> cache) {
        ttlCaches.add(cache);
        return cache;
    }

    /** Eight loaders of eight keys must all be running at once to finish: one at a time, none ever would. */
    @Test
    void differentKeysLoadInParallel() throws Exception {
        for (Supplier<Map<String, String>> make : caches()) {
            Map<String, String> cache = make.get();
            CountDownLatch allLoading = new CountDownLatch(8);
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                String key = "k" + i;
                results.add(pool.submit(() -> cache.computeIfAbsent(key, k -> {
                    allLoading.countDown();
                    try {
                        return allLoading.await(5, TimeUnit.SECONDS) ? "v-" + k : "timed out";
                    } catch (InterruptedException e) {
                        throw new IllegalStateException(e);
                    }
                })));
            }
            for (int i = 0; i < 8; i++) {
                assertEquals("v-k" + i, results.get(i).get(10, TimeUnit.SECONDS), cache.getClass().getName());
                assertEquals("v-k" + i, cache.get("k" + i));
            }
        }
    }

    /** Eight threads asking for one key at once: it is loaded once, and all eight get that value. */
    @Test
    void aKeyIsLoadedOnce() throws Exception {
        for (Supplier<Map<String, String>> make : caches()) {
            Map<String, String> cache = make.get();
            AtomicInteger loads = new AtomicInteger();
            CountDownLatch release = new CountDownLatch(1);
            CyclicBarrier start = new CyclicBarrier(8);
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                results.add(pool.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    return cache.computeIfAbsent("key", k -> {
                        loads.incrementAndGet();
                        try {
                            release.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            throw new IllegalStateException(e);
                        }
                        return "loaded";
                    });
                }));
            }
            Thread.sleep(300);              // all eight are asking while the first load is held
            release.countDown();
            for (Future<String> result : results) {
                assertEquals("loaded", result.get(10, TimeUnit.SECONDS));
            }
            assertEquals(1, loads.get(), cache.getClass().getName());
        }
    }

    /** While a loader runs, other keys can be written and read. */
    @Test
    void otherOperationsDoNotWaitForALoader() throws Exception {
        for (Supplier<Map<String, String>> make : caches()) {
            Map<String, String> cache = make.get();
            CountDownLatch loading = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            Future<String> load = pool.submit(() -> cache.computeIfAbsent("slow", k -> {
                loading.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
                return "slow-value";
            }));
            assertTrue(loading.await(5, TimeUnit.SECONDS));
            Future<String> other = pool.submit(() -> {
                cache.put("other", "x");
                cache.remove("gone");
                return cache.computeIfAbsent("fast", k -> "fast-value") + cache.get("other");
            });
            try {
                assertEquals("fast-valuex", other.get(2, TimeUnit.SECONDS), cache.getClass().getName());
            } finally {
                release.countDown();
            }
            assertEquals("slow-value", load.get(10, TimeUnit.SECONDS));
        }
    }

    /**
     * A put of the same key while its loader runs is kept, and computeIfAbsent returns it: in either order of the
     * two, the put is the last write. (The loader runs outside the lock, so the put does not wait for it.)
     */
    @Test
    void aPutWhileLoadingIsKept() throws Exception {
        for (Supplier<Map<String, String>> make : caches()) {
            Map<String, String> cache = make.get();
            CountDownLatch loading = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            Future<String> load = pool.submit(() -> cache.computeIfAbsent("key", k -> {
                loading.countDown();
                try {
                    release.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
                return "loaded";
            }));
            assertTrue(loading.await(5, TimeUnit.SECONDS));
            pool.submit(() -> cache.put("key", "put")).get(5, TimeUnit.SECONDS);
            release.countDown();
            assertEquals("put", load.get(10, TimeUnit.SECONDS), cache.getClass().getName());
            assertEquals("put", cache.get("key"));
            assertEquals(1, cache.size());
        }
    }

    /** A loader's exception reaches each thread that waited for it, and nothing is cached; the next call loads. */
    @Test
    void aFailedLoadReachesItsWaitersAndCachesNothing() throws Exception {
        for (Supplier<Map<String, String>> make : caches()) {
            Map<String, String> cache = make.get();
            CountDownLatch release = new CountDownLatch(1);
            CyclicBarrier start = new CyclicBarrier(4);
            AtomicInteger loads = new AtomicInteger();
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                results.add(pool.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    return cache.computeIfAbsent("bad", k -> {
                        loads.incrementAndGet();
                        try {
                            release.await(5, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            throw new IllegalStateException(e);
                        }
                        throw new UnsupportedOperationException("no " + k);
                    });
                }));
            }
            Thread.sleep(300);
            release.countDown();
            for (Future<String> result : results) {
                java.util.concurrent.ExecutionException e = assertThrows(java.util.concurrent.ExecutionException.class,
                        () -> result.get(10, TimeUnit.SECONDS));
                assertTrue(e.getCause() instanceof UnsupportedOperationException, String.valueOf(e.getCause()));
            }
            assertEquals(1, loads.get(), cache.getClass().getName());
            assertFalse(cache.containsKey("bad"));
            assertEquals("good", cache.computeIfAbsent("bad", k -> "good"));
        }
    }

    /**
     * A loader that loads its own key through the same cache would wait for itself: it throws
     * IllegalStateException, as ConcurrentHashMap does ("Recursive update"). A loader may load other keys.
     */
    @Test
    void aLoaderLoadingItsOwnKeyThrows() {
        for (Supplier<Map<String, String>> make : caches()) {
            Map<String, String> cache = make.get();
            assertThrows(IllegalStateException.class,
                    () -> cache.computeIfAbsent("self", k -> cache.computeIfAbsent("self", k2 -> "inner")),
                    cache.getClass().getName());
            assertFalse(cache.containsKey("self"));
            assertEquals("outer+inner", cache.computeIfAbsent("a",
                    k -> "outer+" + cache.computeIfAbsent("b", k2 -> "inner")));
            assertEquals("inner", cache.get("b"));
        }
    }

    /**
     * TTLCaches share one purge thread, and purging a size-limited cache's expired entries takes its lock: a load
     * holding that lock stalled purging for every TTLCache (another cache kept 1,000 expired entries for 1.85 s against a 10 ms
     * purge interval). A load holds no lock now. Read through the backing map, as size() cleans up itself.
     */
    @Test
    void aSlowLoadDoesNotStallPurgingOfOtherCaches() throws Exception {
        // the loading cache has entries expiring too, so its purge needs its lock while the load runs; they live
        // 500 ms, so they expire after the load has begun (purged before it, they would need no lock later)
        TTLCache<String, String> slow = track(new TTLCache<>(500, 100, 20));
        TTLCache<String, String> other = track(new TTLCache<>(500, 1000, 20));
        for (int i = 0; i < 100; i++) {
            other.put("k" + i, "v");
            if (i < 10) {
                slow.put("s" + i, "v");
            }
        }
        java.lang.reflect.Field backing = TTLCache.class.getDeclaredField("cacheMap");
        backing.setAccessible(true);
        Map<?, ?> otherEntries = (Map<?, ?>) backing.get(other);
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Future<String> load = pool.submit(() -> slow.computeIfAbsent("slow", k -> {
            loading.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            return "v";
        }));
        try {
            assertTrue(loading.await(5, TimeUnit.SECONDS));
            long deadline = System.currentTimeMillis() + 3000;
            while (!otherEntries.isEmpty() && System.currentTimeMillis() < deadline) {
                Thread.sleep(20);
            }
            assertEquals(0, otherEntries.size(), "expired entries left while another cache loads");
        } finally {
            release.countDown();
        }
        assertEquals("v", load.get(10, TimeUnit.SECONDS));
    }

    /** null keys, null results, and a hit, as before. */
    @Test
    void nullKeysNullResultsAndHits() {
        for (Supplier<Map<String, String>> make : caches()) {
            Map<String, String> cache = make.get();
            assertNull(cache.computeIfAbsent("none", k -> null));
            assertFalse(cache.containsKey("none"));
            cache.put("here", "v");
            assertEquals("v", cache.computeIfAbsent("here", k -> "not called"));
            if (cache instanceof LRUCache) {
                assertEquals("null-key", cache.computeIfAbsent(null, k -> "null-key"));
                assertEquals("null-key", cache.get(null));
            }
        }
    }

    /** The size limit holds when many loads finish at once. */
    @Test
    void theSizeLimitHoldsUnderConcurrentLoads() throws Exception {
        Map<String, String> lru = new LRUCache<>(4, LRUCache.StrategyType.LOCKING);
        Map<String, String> ttl = track(new TTLCache<>(60_000, 4));
        for (Map<String, String> cache : new Map[] {lru, ttl}) {
            CyclicBarrier start = new CyclicBarrier(32);
            List<Future<String>> results = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                String key = "k" + i;
                results.add(pool.submit(() -> {
                    start.await(5, TimeUnit.SECONDS);
                    return cache.computeIfAbsent(key, k -> "v-" + k);
                }));
            }
            for (int i = 0; i < 32; i++) {
                assertEquals("v-k" + i, results.get(i).get(10, TimeUnit.SECONDS));
            }
            assertTrue(cache.size() <= 4, cache.getClass().getName() + " size " + cache.size());
        }
    }
}
