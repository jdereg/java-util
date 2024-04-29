package com.cedarsoftware.util;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LRUCacheTest {

    private LRUCache<Integer, String> lruCache;

    @BeforeEach
    void setUp() {
        lruCache = new LRUCache<>(3);
    }

    @Test
    void testPutAndGet() {
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");

        assertEquals("A", lruCache.get(1));
        assertEquals("B", lruCache.get(2));
        assertEquals("C", lruCache.get(3));
    }

    @Test
    void testEvictionPolicy() {
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        lruCache.get(1);
        lruCache.put(4, "D");

        assertNull(lruCache.get(2));
        assertEquals("A", lruCache.get(1));
    }

    @Test
    void testSize() {
        lruCache.put(1, "A");
        lruCache.put(2, "B");

        assertEquals(2, lruCache.size());
    }

    @Test
    void testIsEmpty() {
        assertTrue(lruCache.isEmpty());

        lruCache.put(1, "A");

        assertFalse(lruCache.isEmpty());
    }

    @Test
    void testRemove() {
        lruCache.put(1, "A");
        lruCache.remove(1);

        assertNull(lruCache.get(1));
    }

    @Test
    void testContainsKey() {
        lruCache.put(1, "A");

        assertTrue(lruCache.containsKey(1));
        assertFalse(lruCache.containsKey(2));
    }

    @Test
    void testContainsValue() {
        lruCache.put(1, "A");

        assertTrue(lruCache.containsValue("A"));
        assertFalse(lruCache.containsValue("B"));
    }

    @Test
    void testKeySet() {
        lruCache.put(1, "A");
        lruCache.put(2, "B");

        assertTrue(lruCache.keySet().contains(1));
        assertTrue(lruCache.keySet().contains(2));
    }

    @Test
    void testValues() {
        lruCache.put(1, "A");
        lruCache.put(2, "B");

        assertTrue(lruCache.values().contains("A"));
        assertTrue(lruCache.values().contains("B"));
    }

    @Test
    void testClear() {
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.clear();

        assertTrue(lruCache.isEmpty());
    }

    @Test
    void testPutAll() {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "A");
        map.put(2, "B");
        lruCache.putAll(map);

        assertEquals("A", lruCache.get(1));
        assertEquals("B", lruCache.get(2));
    }

    @Test
    void testEntrySet() {
        lruCache.put(1, "A");
        lruCache.put(2, "B");

        assertEquals(2, lruCache.entrySet().size());
    }

    @Test
    void testPutIfAbsent() {
        lruCache.putIfAbsent(1, "A");
        lruCache.putIfAbsent(1, "B");

        assertEquals("A", lruCache.get(1));
    }
    
    @Test
    void testConcurrency() throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(3);
        lruCache = new LRUCache<>(100000);

        // Perform a mix of put and get operations from multiple threads
        int max = 10000;
        int attempts = 0;
        Random random = new SecureRandom();
        while (attempts++ < max) {
            final int key = random.nextInt(max);
            final String value = "V" + key;

            service.submit(() -> lruCache.put(key, value));
            service.submit(() -> lruCache.get(key));
            service.submit(() -> lruCache.size());
            service.submit(() -> {
                lruCache.keySet().remove(random.nextInt(max));
            });
            service.submit(() -> {
                lruCache.values().remove("V" + random.nextInt(max));
            });
            final int attemptsCopy = attempts;
            service.submit(() -> {
                Iterator i = lruCache.entrySet().iterator();
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
//        System.out.println("lruCache = " + lruCache);
//        System.out.println("lruCache = " + lruCache.size());
//        System.out.println("attempts =" + attempts);
    }
}
