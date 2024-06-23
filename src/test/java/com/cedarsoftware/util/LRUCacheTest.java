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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
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

        // Wait for the background cleanup thread to perform the eviction
        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 5 seconds timeout
        while (System.currentTimeMillis() - startTime < timeout) {
            if (!lruCache.containsKey(2) && lruCache.containsKey(1) && lruCache.containsKey(4)) {
                break;
            }
            try {
                Thread.sleep(100); // Check every 100ms
            } catch (InterruptedException ignored) {
            }
        }

        // Assert the expected cache state
        assertNull(lruCache.get(2), "Entry for key 2 should be evicted");
        assertEquals("A", lruCache.get(1), "Entry for key 1 should still be present");
        assertEquals("D", lruCache.get(4), "Entry for key 4 should be present");
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
    void testSmallSizes()
    {
        // Testing with different sizes
        for (int capacity : new int[]{1, 3, 5, 10}) {
            LRUCache<Integer, String> cache = new LRUCache<>(capacity);
            for (int i = 0; i < capacity; i++) {
                cache.put(i, "Value" + i);
            }
            for (int i = 0; i < capacity; i++) {
                cache.get(i);
            }
            for (int i = 0; i < capacity; i++) {
                cache.remove(i);
            }

            assert cache.isEmpty();
            cache.clear();
        }
    }
    
    @Test
    void testConcurrency() throws InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(3);
        lruCache = new LRUCache<>(10000);

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
    }

    @Test
    public void testConcurrency2() throws InterruptedException {
        int initialEntries = 100;
        lruCache = new LRUCache<>(initialEntries);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        // Add initial entries
        for (int i = 0; i < initialEntries; i++) {
            lruCache.put(i, "true");
        }

        SecureRandom random = new SecureRandom();
        // Perform concurrent operations
        for (int i = 0; i < 100000; i++) {
            final int key = random.nextInt(100);
            executor.submit(() -> {
                lruCache.put(key, "true"); // Add
                lruCache.remove(key); // Remove
                lruCache.put(key, "false"); // Update
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.MINUTES));

        // Check some values to ensure correctness
        for (int i = 0; i < initialEntries; i++) {
            final int key = i;
            assertTrue(lruCache.containsKey(key));
        }

        assert lruCache.size() == 100;
        assertEquals(initialEntries, lruCache.size());
    }

    @Test
    void testEquals() {
        LRUCache<Integer, String> cache1 = new LRUCache<>(3);
        LRUCache<Integer, String> cache2 = new LRUCache<>(3);

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

    @Test
    void testHashCode() {
        LRUCache<Integer, String> cache1 = new LRUCache<>(3);
        LRUCache<Integer, String> cache2 = new LRUCache<>(3);

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

    @Test
    void testToString() {
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");

        assert lruCache.toString().contains("1=A");
        assert lruCache.toString().contains("2=B");
        assert lruCache.toString().contains("3=C");

        Map<String, String> cache = new LRUCache(100);
        assert cache.toString().equals("{}");
        assert cache.size() == 0;
    }

    @Test
    void testFullCycle() {
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        lruCache.put(4, "D");
        lruCache.put(5, "E");
        lruCache.put(6, "F");

        long startTime = System.currentTimeMillis();
        long timeout = 5000; // 5 seconds timeout
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
                Thread.sleep(100); // Check every 100ms
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
    
    @Test
    void testCacheWhenEmpty() {
        // The cache is initially empty, so any get operation should return null
        assertNull(lruCache.get(1));
    }

    @Test
    void testCacheClear() {
        // Add elements to the cache
        lruCache.put(1, "A");
        lruCache.put(2, "B");

        // Clear the cache
        lruCache.clear();

        // The cache should be empty, so any get operation should return null
        assertNull(lruCache.get(1));
        assertNull(lruCache.get(2));
    }

    @Test
    void testCacheBlast() {
        // Jam 10M items to the cache
        lruCache = new LRUCache<>(1000);
        for (int i = 0; i < 10000000; i++) {
            lruCache.put(i, "" + i);
        }

        // Wait until the cache size stabilizes to 1000
        int expectedSize = 1000;
        long startTime = System.currentTimeMillis();
        long timeout = 10000; // wait up to 10 seconds (will never take this long)
        while (System.currentTimeMillis() - startTime < timeout) {
            if (lruCache.size() <= expectedSize) {
                break;
            }
            try {
                Thread.sleep(100); // Check every 100ms
                System.out.println("Cache size: " + lruCache.size());
            } catch (InterruptedException ignored) {
            }
        }

        assertEquals(1000, lruCache.size());
    }

    @Test
    void testNullValue()
    {
        lruCache = new LRUCache<>(100, 1);
        lruCache.put(1, null);
        assert lruCache.containsKey(1);
        assert lruCache.containsValue(null);
        assert lruCache.toString().contains("1=null");
        assert lruCache.hashCode() != 0;
    }

    @Test
    void testNullKey()
    {
        lruCache = new LRUCache<>(100, 1);
        lruCache.put(null, "true");
        assert lruCache.containsKey(null);
        assert lruCache.containsValue("true");
        assert lruCache.toString().contains("null=true");
        assert lruCache.hashCode() != 0;
    }

    @Test
    void testNullKeyValue()
    {
        lruCache = new LRUCache<>(100, 1);
        lruCache.put(null, null);
        assert lruCache.containsKey(null);
        assert lruCache.containsValue(null);
        assert lruCache.toString().contains("null=null");
        assert lruCache.hashCode() != 0;

        LRUCache<Integer, String> cache1 = new LRUCache<>(3);
        cache1.put(null, null);
        LRUCache<Integer, String> cache2 = new LRUCache<>(3);
        cache2.put(null, null);
        assert cache1.equals(cache2);
    }

    @Test
    void testSpeed()
    {
        long startTime = System.currentTimeMillis();
        LRUCache<Integer, Boolean> cache = new LRUCache<>(30000000);
        for (int i = 0; i < 30000000; i++) {
            cache.put(i, true);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Speed: " + (endTime - startTime));
    }
}
