package com.cedarsoftware.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LRUCacheTest {

    private LRUCache<Integer, String> lruCache;
    private static final Logger LOG = Logger.getLogger(LRUCacheTest.class.getName());

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(
                LRUCache.StrategyType.LOCKING,
                LRUCache.StrategyType.THREADED
        );
    }

    void setUp(LRUCache.StrategyType strategyType) {
        lruCache = new LRUCache<>(3, strategyType);
    }

    @AfterEach
    void tearDown() {
        // Drop the reference so the cache is collectable. No explicit System.gc() here: this class has
        // ~30 test methods run against 2 strategies, so a collection per test meant ~60 forced full GCs
        // for no benefit -- releasing the reference is what actually matters, and the JVM will collect
        // when it needs to. The former multi-hundred-MB tests that motivated it now bound their own
        // memory (see the perf metrics below).
        if (lruCache != null) {
            lruCache.clear();
            lruCache = null;
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testInvalidCapacityThrows(LRUCache.StrategyType strategy) {
        assertThrows(IllegalArgumentException.class, () -> new LRUCache<>(0, strategy));
        assertThrows(IllegalArgumentException.class, () -> new LRUCache<>(-5, strategy));
        if (strategy == LRUCache.StrategyType.THREADED) {
            assertThrows(IllegalArgumentException.class, () -> new LRUCache<>(0, 10));
            assertThrows(IllegalArgumentException.class, () -> new LRUCache<>(-1, 10));
        } else {
            assertThrows(IllegalArgumentException.class, () -> new LRUCache<>(0));
            assertThrows(IllegalArgumentException.class, () -> new LRUCache<>(-1));
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testGetCapacity(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        assertEquals(5, cache.getCapacity());

        if (strategy == LRUCache.StrategyType.THREADED) {
            LRUCache<Integer, String> threaded = new LRUCache<>(2, 25);
            assertEquals(2, threaded.getCapacity());
        } else {
            LRUCache<Integer, String> locking = new LRUCache<>(4);
            assertEquals(4, locking.getCapacity());
        }
    }
    
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAndGet(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");

        assertEquals("A", lruCache.get(1));
        assertEquals("B", lruCache.get(2));
        assertEquals("C", lruCache.get(3));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEvictionPolicy(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        lruCache.get(1);  // Access key 1 to make it "more recently used"
        lruCache.put(4, "D");

        // Wait for eviction to occur (ThreadedLRUCacheStrategy uses background cleanup)
        long startTime = System.currentTimeMillis();
        long timeout = 5000;
        while (System.currentTimeMillis() - startTime < timeout) {
            // Check if at least one entry was evicted (size should be at or below capacity)
            if (lruCache.size() <= 3) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        // Verify cache size is bounded (approximate LRU - we don't guarantee which specific entry is evicted)
        assertTrue(lruCache.size() <= 3, "Cache size should be at or below capacity after eviction");
        // Key 4 (most recently added) should always be present
        assertEquals("D", lruCache.get(4), "Most recently added entry should be present");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testSize(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");

        assertEquals(2, lruCache.size());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmpty(LRUCache.StrategyType strategy) {
        setUp(strategy);
        assertTrue(lruCache.isEmpty());

        lruCache.put(1, "A");

        assertFalse(lruCache.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testRemove(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.remove(1);

        assertNull(lruCache.get(1));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testContainsKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");

        assertTrue(lruCache.containsKey(1));
        assertFalse(lruCache.containsKey(2));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testContainsValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");

        assertTrue(lruCache.containsValue("A"));
        assertFalse(lruCache.containsValue("B"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySet(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");

        assertTrue(lruCache.keySet().contains(1));
        assertTrue(lruCache.keySet().contains(2));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValues(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");

        assertTrue(lruCache.values().contains("A"));
        assertTrue(lruCache.values().contains("B"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testClear(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.clear();

        assertTrue(lruCache.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAll(LRUCache.StrategyType strategy) {
        setUp(strategy);
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "A");
        map.put(2, "B");
        lruCache.putAll(map);

        assertEquals("A", lruCache.get(1));
        assertEquals("B", lruCache.get(2));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEntrySet(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");

        assertEquals(2, lruCache.entrySet().size());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testPutIfAbsent(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.putIfAbsent(1, "A");
        lruCache.putIfAbsent(1, "B");

        assertEquals("A", lruCache.get(1));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testPutIfAbsentTreatsNullMappedEntryAsAbsent(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, null);

        assertNull(lruCache.putIfAbsent(1, "A"));
        assertEquals("A", lruCache.get(1));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testComputeIfAbsentTreatsNullMappedEntryAsAbsent(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, null);

        assertEquals("A", lruCache.computeIfAbsent(1, k -> "A"));
        assertEquals("A", lruCache.get(1));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testSmallSizes(LRUCache.StrategyType strategy) {
        for (int capacity : new int[]{1, 3, 5, 10}) {
            LRUCache<Integer, String> cache = new LRUCache<>(capacity, strategy);
            for (int i = 0; i < capacity; i++) {
                cache.put(i, "Value" + i);
            }
            for (int i = 0; i < capacity; i++) {
                cache.get(i);
            }
            for (int i = 0; i < capacity; i++) {
                cache.remove(i);
            }

            assertTrue(cache.isEmpty());
            cache.clear();
        }
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @ParameterizedTest
    @MethodSource("strategies")
    void testConcurrency(LRUCache.StrategyType strategy) throws InterruptedException {
        setUp(strategy);
        ExecutorService service = Executors.newFixedThreadPool(3);
        lruCache = new LRUCache<>(10000, strategy);

        int max = 10000;
        int attempts = 0;
        Random random = new SecureRandom();
        while (attempts++ < max) {
            final int key = random.nextInt(max);
            final String value = "V" + key;

            service.submit(() -> lruCache.put(key, value));
            service.submit(() -> lruCache.get(key));
            service.submit(() -> lruCache.size());
            service.submit(() -> lruCache.keySet().remove(random.nextInt(max)));
            service.submit(() -> lruCache.values().remove("V" + random.nextInt(max)));
            final int attemptsCopy = attempts;
            service.submit(() -> {
                Iterator<Map.Entry<Integer, String>> i = lruCache.entrySet().iterator();
                int walk = random.nextInt(attemptsCopy);
                while (i.hasNext() && walk-- > 0) {
                    i.next();
                }
                int chunk = 10;
                while (i.hasNext() && chunk-- > 0) {
                    i.remove();
                    i.next();
                }
            });
            service.submit(() -> lruCache.remove(random.nextInt(max)));
        }

        service.shutdown();
        assertTrue(service.awaitTermination(1, TimeUnit.MINUTES));
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @ParameterizedTest
    @MethodSource("strategies")
    void testConcurrency2(LRUCache.StrategyType strategy) throws InterruptedException {
        setUp(strategy);
        int initialEntries = 100;
        lruCache = new LRUCache<>(initialEntries, strategy);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < initialEntries; i++) {
            lruCache.put(i, "true");
        }

        SecureRandom random = new SecureRandom();
        for (int i = 0; i < 100000; i++) {
            final int key = random.nextInt(100);
            executor.submit(() -> {
                lruCache.put(key, "true");
                lruCache.remove(key);
                lruCache.put(key, "false");
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.MINUTES));

        for (int i = 0; i < initialEntries; i++) {
            final int key = i;
            assertTrue(lruCache.containsKey(key));
        }

        assertEquals(initialEntries, lruCache.size());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testEquals(LRUCache.StrategyType strategy) {
        setUp(strategy);
        LRUCache<Integer, String> cache1 = new LRUCache<>(3, strategy);
        LRUCache<Integer, String> cache2 = new LRUCache<>(3, strategy);

        cache1.put(1, "A");
        cache1.put(2, "B");
        cache1.put(3, "C");

        cache2.put(1, "A");
        cache2.put(2, "B");
        cache2.put(3, "C");

        assertTrue(cache1.equals(cache2));
        assertTrue(cache2.equals(cache1));

        cache2.put(4, "D");
        assertFalse(cache1.equals(cache2));
        assertFalse(cache2.equals(cache1));

        assertFalse(cache1.equals(Boolean.TRUE));

        assertTrue(cache1.equals(cache1));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testHashCode(LRUCache.StrategyType strategy) {
        setUp(strategy);
        LRUCache<Integer, String> cache1 = new LRUCache<>(3, strategy);
        LRUCache<Integer, String> cache2 = new LRUCache<>(3, strategy);

        cache1.put(1, "A");
        cache1.put(2, "B");
        cache1.put(3, "C");

        cache2.put(1, "A");
        cache2.put(2, "B");
        cache2.put(3, "C");

        assertEquals(cache1.hashCode(), cache2.hashCode());

        cache2.put(4, "D");
        assertFalse(cache1.equals(cache2));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testHashCodeMatchesEqualStandardMap(LRUCache.StrategyType strategy) {
        setUp(strategy);
        LRUCache<Integer, String> cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");

        Map<Integer, String> standardMap = new HashMap<>();
        standardMap.put(3, "C");
        standardMap.put(1, "A");
        standardMap.put(2, "B");

        assertTrue(cache.equals(standardMap));
        assertTrue(standardMap.equals(cache));
        assertEquals(standardMap.hashCode(), cache.hashCode());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testSnapshotViewsAreUnmodifiable(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");

        assertThrows(UnsupportedOperationException.class, () -> lruCache.keySet().remove(1));
        assertThrows(UnsupportedOperationException.class, () -> lruCache.values().remove("A"));
        assertThrows(UnsupportedOperationException.class, () -> {
            Iterator<Map.Entry<Integer, String>> iterator = lruCache.entrySet().iterator();
            iterator.next();
            iterator.remove();
        });
        assertEquals(2, lruCache.size());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testToString(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");

        assertTrue(lruCache.toString().contains("1=A"));
        assertTrue(lruCache.toString().contains("2=B"));
        assertTrue(lruCache.toString().contains("3=C"));

        Map<String, String> cache = new LRUCache<>(100, strategy);
        assertEquals("{}", cache.toString());
        assertEquals(0, cache.size());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testFullCycle(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        lruCache.put(4, "D");
        lruCache.put(5, "E");
        lruCache.put(6, "F");

        long startTime = System.currentTimeMillis();
        long timeout = 5000;
        while (System.currentTimeMillis() - startTime < timeout) {
            if (strategy == LRUCache.StrategyType.THREADED) {
                if (lruCache.size() <= 3) {
                    break;
                }
            } else {
                if (lruCache.size() == 3 &&
                        lruCache.containsKey(4) &&
                        lruCache.containsKey(5) &&
                        lruCache.containsKey(6) &&
                        !lruCache.containsKey(1) &&
                        !lruCache.containsKey(2) &&
                        !lruCache.containsKey(3)) {
                    break;
                }
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        if (strategy == LRUCache.StrategyType.THREADED) {
            assertTrue(lruCache.size() <= 3, "Cache size should be at or below capacity after cleanup");
            assertEquals("F", lruCache.get(6), "Most recently added entry should be retained");
        } else {
            assertEquals(3, lruCache.size(), "Cache size should be 3 after eviction");
            assertTrue(lruCache.containsKey(4));
            assertTrue(lruCache.containsKey(5));
            assertTrue(lruCache.containsKey(6));
            assertEquals("D", lruCache.get(4));
            assertEquals("E", lruCache.get(5));
            assertEquals("F", lruCache.get(6));

            lruCache.remove(6);
            lruCache.remove(5);
            lruCache.remove(4);
            assertEquals(0, lruCache.size(), "Cache should be empty after removing all elements");
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testCacheWhenEmpty(LRUCache.StrategyType strategy) {
        setUp(strategy);
        assertNull(lruCache.get(1));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testCacheClear(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.clear();

        assertNull(lruCache.get(1));
        assertNull(lruCache.get(2));
    }

    /**
     * Sustained insert pressure far above capacity must still settle at capacity.
     * <p>
     * Was 10,000,000 puts per strategy (20 million across both) into a 1000-entry cache — 99.99% of the
     * work evicted on arrival — to assert one number. 200,000 puts is 200x the capacity, which exercises
     * the same eviction path under the same "always over capacity" condition, and runs ~50x faster.
     */
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @ParameterizedTest
    @MethodSource("strategies")
    void testCacheBlast(LRUCache.StrategyType strategy) {
        lruCache = new LRUCache<>(1000, strategy);
        for (int i = 0; i < 200000; i++) {
            lruCache.put(i, "" + i);
        }

        int expectedSize = 1000;
        long startTime = System.currentTimeMillis();
        long timeout = 10000;
        while (System.currentTimeMillis() - startTime < timeout) {
            if (lruCache.size() <= expectedSize) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }

        assertEquals(1000, lruCache.size());
        lruCache.clear();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testNullValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache = new LRUCache<>(100, strategy);
        lruCache.put(1, null);
        assertTrue(lruCache.containsKey(1));
        assertTrue(lruCache.containsValue(null));
        assertTrue(lruCache.toString().contains("1=null"));
        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, null);
        assertEquals(expected.hashCode(), lruCache.hashCode());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testNullKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache = new LRUCache<>(100, strategy);
        lruCache.put(null, "true");
        assertTrue(lruCache.containsKey(null));
        assertTrue(lruCache.containsValue("true"));
        assertTrue(lruCache.toString().contains("null=true"));
        Map<Integer, String> expected = new HashMap<>();
        expected.put(null, "true");
        assertEquals(expected.hashCode(), lruCache.hashCode());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testNullKeyValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache = new LRUCache<>(100, strategy);
        lruCache.put(null, null);
        assertTrue(lruCache.containsKey(null));
        assertTrue(lruCache.containsValue(null));
        assertTrue(lruCache.toString().contains("null=null"));
        Map<Integer, String> expected = new HashMap<>();
        expected.put(null, null);
        assertEquals(expected.hashCode(), lruCache.hashCode());

        LRUCache<Integer, String> cache1 = new LRUCache<>(3, strategy);
        cache1.put(null, null);
        LRUCache<Integer, String> cache2 = new LRUCache<>(3, strategy);
        cache2.put(null, null);
        assertTrue(cache1.equals(cache2));
    }

    /**
     * Put cost for each strategy, as a ratio against {@link HashMap} — the floor an LRU cache builds on,
     * so the number reads as "what recency tracking costs over a plain hash map".
     * <p>
     * Replaces a version that put 10,000,000 entries per strategy into a cache sized to hold them all
     * (~800MB each, per its own comment), timed the whole thing, logged one millisecond figure and
     * <b>asserted nothing</b>. It could not fail, could not detect a regression, and its number was not
     * comparable across machines. The ratio here is.
     */
    /**
     * Tracks the LOCKING strategy only, deliberately.
     * <p>
     * THREADED was measured too and dropped: its ratio ranged 1.14x-3.48x across four otherwise
     * identical runs, because its reads are serviced alongside a background cleanup thread whose
     * scheduling differs run to run. Worse, that variance is invisible to the harness — trials within
     * a single run agreed to 2.6%, so the self-calibrating bar would have stayed tight and reported
     * confident regressions on nothing but thread timing. A number that cannot be reproduced should not
     * be ratcheted against; LOCKING's read path is deterministic and reproduces to a few percent.
     * THREADED's behavior remains covered by the functional tests in this class.
     */
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void perfGetRatioVsHashMap() {
        final LRUCache.StrategyType strategy = LRUCache.StrategyType.LOCKING;
        final int entries = 20000;

        // Populated once, outside the timed region. Measuring get rather than cache construction is
        // both the more meaningful number -- a cache's hot path is reads -- and the far steadier one:
        // timing construction made this benchmark allocation-bound, and the resulting GC scatter put
        // its interquartile noise at 150-250%, wide enough that no regression could ever be flagged.
        // The timed region below allocates nothing beyond the LRU's own recency bookkeeping.
        final LRUCache<Integer, Boolean> cache = new LRUCache<>(entries, strategy);
        final Map<Integer, Boolean> yardstickMap = new HashMap<>((int) (entries / 0.75f) + 1);
        for (int i = 0; i < entries; i++) {
            cache.put(i, true);
            yardstickMap.put(i, true);
        }

        final Runnable subject = () -> {
            for (int i = 0; i < entries; i++) {
                PerfRatchet.sink = cache.get(i);
            }
        };
        final Runnable yardstick = () -> {
            for (int i = 0; i < entries; i++) {
                PerfRatchet.sink = yardstickMap.get(i);
            }
        };

        PerfRatchet.warmAll(3, subject, yardstick);
        PerfRatchet.Metric m = PerfRatchet.compare(
                "lrucache.get." + strategy.name().toLowerCase(java.util.Locale.ROOT), entries,
                "LRUCache.get(" + strategy + ")", subject, "HashMap.get", yardstick);

        if (PerfRatchet.isStrict() && m.getVerdict() == PerfRatchet.Verdict.REGRESSED) {
            org.junit.jupiter.api.Assertions.fail("Performance regression (perf.strict): ratio " + m.getRatio());
        }
    }
}
