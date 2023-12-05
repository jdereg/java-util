package com.cedarsoftware.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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

        // Perform a mix of put and get operations from multiple threads
        for (int i = 0; i < 10000; i++) {
            final int key = i % 3;  // Keys will be 0, 1, 2
            final String value = "Value" + i;

            service.submit(() -> lruCache.put(key, value));
            service.submit(() -> lruCache.get(key));
        }

        service.shutdown();
        assertTrue(service.awaitTermination(1, TimeUnit.MINUTES));

        // Assert the final state of the cache
        assertEquals(3, lruCache.size());
        Set<Integer> keys = lruCache.keySet();
        assertTrue(keys.contains(0) || keys.contains(1) || keys.contains(2));
    }
}
