package com.cedarsoftware.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LRUCacheTest {

    private LRUCache<Integer, String> lruCache;

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
        if (lruCache != null) {
            lruCache.shutdown();
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
        lruCache.get(1);
        lruCache.put(4, "D");

        long startTime = System.currentTimeMillis();
        long timeout = 5000;
        while (System.currentTimeMillis() - startTime < timeout) {
            if (!lruCache.containsKey(2) && lruCache.containsKey(1) && lruCache.containsKey(4)) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

        assertNull(lruCache.get(2), "Entry for key 2 should be evicted");
        assertEquals("A", lruCache.get(1), "Entry for key 1 should still be present");
        assertEquals("D", lruCache.get(4), "Entry for key 4 should be present");
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
        assertNotEquals(cache1.hashCode(), cache2.hashCode());
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
            if (lruCache.size() == 3 &&
                    lruCache.containsKey(4) &&
                    lruCache.containsKey(5) &&
                    lruCache.containsKey(6) &&
                    !lruCache.containsKey(1) &&
                    !lruCache.containsKey(2) &&
                    !lruCache.containsKey(3)) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }

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

    @ParameterizedTest
    @MethodSource("strategies")
    void testCacheBlast(LRUCache.StrategyType strategy) {
        lruCache = new LRUCache<>(1000, strategy);
        for (int i = 0; i < 10000000; i++) {
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
                Thread.sleep(100);
                System.out.println(strategy + " cache size: " + lruCache.size());
            } catch (InterruptedException ignored) {
            }
        }

        assertEquals(1000, lruCache.size());
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
        assertNotEquals(0, lruCache.hashCode());
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
        assertNotEquals(0, lruCache.hashCode());
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
        assertNotEquals(0, lruCache.hashCode());

        LRUCache<Integer, String> cache1 = new LRUCache<>(3, strategy);
        cache1.put(null, null);
        LRUCache<Integer, String> cache2 = new LRUCache<>(3, strategy);
        cache2.put(null, null);
        assertTrue(cache1.equals(cache2));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testSpeed(LRUCache.StrategyType strategy) {
        setUp(strategy);
        long startTime = System.currentTimeMillis();
        LRUCache<Integer, Boolean> cache = new LRUCache<>(10000000, strategy);
        for (int i = 0; i < 10000000; i++) {
            cache.put(i, true);
        }
        long endTime = System.currentTimeMillis();
        System.out.println(strategy + " speed: " + (endTime - startTime) + "ms");
    }
}
