package com.cedarsoftware.util;

import java.security.SecureRandom;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TTLCacheTest {

    private TTLCache<Integer, String> ttlCache;
    private static final Logger LOG = Logger.getLogger(TTLCacheTest.class.getName());

    @AfterAll
    static void tearDown() {
        TTLCache.shutdown();
    }

    @Test
    void testPutAndGet() {
        ttlCache = new TTLCache<>(10000, -1); // TTL of 10 seconds, no LRU
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");

        assertEquals("A", ttlCache.get(1));
        assertEquals("B", ttlCache.get(2));
        assertEquals("C", ttlCache.get(3));
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testEntryExpiration() throws InterruptedException {
        ttlCache = new TTLCache<>(200, -1, 100); // TTL of 1 second, no LRU
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");

        // Entries should be present initially
        assertEquals(3, ttlCache.size());
        assertTrue(ttlCache.containsKey(1));
        assertTrue(ttlCache.containsKey(2));
        assertTrue(ttlCache.containsKey(3));

        // Wait for TTL to expire
        Thread.sleep(350);

        // Entries should have expired
        assertEquals(0, ttlCache.size());
        assertFalse(ttlCache.containsKey(1));
        assertFalse(ttlCache.containsKey(2));
        assertFalse(ttlCache.containsKey(3));
    }

    @Test
    void testLRUEviction() {
        ttlCache = new TTLCache<>(10000, 3); // TTL of 10 seconds, max size of 3
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        ttlCache.get(1); // Access key 1 to make it recently used
        ttlCache.put(4, "D"); // This should evict key 2 (least recently used)

        assertNull(ttlCache.get(2), "Entry for key 2 should be evicted");
        assertEquals("A", ttlCache.get(1), "Entry for key 1 should still be present");
        assertEquals("D", ttlCache.get(4), "Entry for key 4 should be present");
    }

    @Test
    void testSize() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");

        assertEquals(2, ttlCache.size());
    }

    @Test
    void testIsEmpty() {
        ttlCache = new TTLCache<>(10000, -1);
        assertTrue(ttlCache.isEmpty());

        ttlCache.put(1, "A");

        assertFalse(ttlCache.isEmpty());
    }

    @Test
    void testRemove() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");
        ttlCache.remove(1);

        assertNull(ttlCache.get(1));
    }

    @Test
    void testContainsKey() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");

        assertTrue(ttlCache.containsKey(1));
        assertFalse(ttlCache.containsKey(2));
    }

    @Test
    void testContainsValue() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");

        assertTrue(ttlCache.containsValue("A"));
        assertFalse(ttlCache.containsValue("B"));
    }

    @Test
    void testKeySet() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");

        Set<Integer> keys = ttlCache.keySet();
        assertTrue(keys.contains(1));
        assertTrue(keys.contains(2));
    }

    @Test
    void testValues() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");

        Collection<String> values = ttlCache.values();
        assertTrue(values.contains("A"));
        assertTrue(values.contains("B"));
    }

    @Test
    void testClear() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.clear();

        assertTrue(ttlCache.isEmpty());
    }

    @Test
    void testPutAll() {
        ttlCache = new TTLCache<>(10000, -1);
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "A");
        map.put(2, "B");
        ttlCache.putAll(map);

        assertEquals("A", ttlCache.get(1));
        assertEquals("B", ttlCache.get(2));
    }

    @Test
    void testEntrySet() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");

        assertEquals(2, ttlCache.entrySet().size());
    }

    @Test
    void testPutIfAbsentTreatsNullValueAsAbsent() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, null);

        assertNull(ttlCache.putIfAbsent(1, "A"));
        assertEquals("A", ttlCache.get(1));
    }

    @Test
    void testComputeIfAbsentTreatsNullValueAsAbsent() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, null);

        assertEquals("A", ttlCache.computeIfAbsent(1, k -> "A"));
        assertEquals("A", ttlCache.get(1));
    }

    @Test
    void testEntrySetSizeIsBestEffortForExpiredEntries() throws InterruptedException {
        ttlCache = new TTLCache<>(30, -1, 60_000);
        ttlCache.put(1, "A");

        Thread.sleep(60);

        assertEquals(0, ttlCache.entrySet().stream().count());
        int approximateSize = ttlCache.entrySet().size();
        assertTrue(approximateSize >= 0 && approximateSize <= 1);
    }

    @Test
    void testSnapshotViewsAreUnmodifiable() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");

        assertThrows(UnsupportedOperationException.class, () -> ttlCache.keySet().remove(1));
        assertThrows(UnsupportedOperationException.class, () -> ttlCache.values().remove("A"));
    }

    @Test
    void testSmallSizes() {
        for (int capacity : new int[]{1, 3, 5, 10}) {
            ttlCache = new TTLCache<>(10000, capacity);
            for (int i = 0; i < capacity; i++) {
                ttlCache.put(i, "Value" + i);
            }
            for (int i = 0; i < capacity; i++) {
                ttlCache.get(i);
            }
            for (int i = 0; i < capacity; i++) {
                ttlCache.remove(i);
            }

            assertTrue(ttlCache.isEmpty());
            ttlCache.clear();
        }
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testConcurrency() throws InterruptedException {
        ttlCache = new TTLCache<>(10000, 10000);
        ExecutorService service = Executors.newFixedThreadPool(10);

        int max = 10000;
        int attempts = 0;
        Random random = new SecureRandom();
        while (attempts++ < max) {
            final int key = random.nextInt(max);
            final String value = "V" + key;

            service.submit(() -> ttlCache.put(key, value));
            service.submit(() -> ttlCache.get(key));
            service.submit(() -> ttlCache.size());
            service.submit(() -> ttlCache.keySet().remove(random.nextInt(max)));
            service.submit(() -> ttlCache.values().remove("V" + random.nextInt(max)));
            final int attemptsCopy = attempts;
            service.submit(() -> {
                Iterator<Map.Entry<Integer, String>> i = ttlCache.entrySet().iterator();
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
            service.submit(() -> ttlCache.remove(random.nextInt(max)));
        }

        service.shutdown();
        assertTrue(service.awaitTermination(1, TimeUnit.MINUTES));
    }

    @Test
    void testEquals() {
        TTLCache<Integer, String> cache1 = new TTLCache<>(10000, 3);
        TTLCache<Integer, String> cache2 = new TTLCache<>(10000, 3);

        cache1.put(1, "A");
        cache1.put(2, "B");
        cache1.put(3, "C");

        cache2.put(1, "A");
        cache2.put(2, "B");
        cache2.put(3, "C");

        assertEquals(cache1, cache2);
        assertEquals(cache2, cache1);

        cache2.put(4, "D");
        assertNotEquals(cache1, cache2);
        assertNotEquals(cache2, cache1);

        assertNotEquals(cache1, Boolean.TRUE);

        assertEquals(cache1, cache1);
    }

    @Test
    void testHashCode() {
        TTLCache<Integer, String> cache1 = new TTLCache<>(10000, 3);
        TTLCache<Integer, String> cache2 = new TTLCache<>(10000, 3);

        cache1.put(1, "A");
        cache1.put(2, "B");
        cache1.put(3, "C");

        cache2.put(1, "A");
        cache2.put(2, "B");
        cache2.put(3, "C");

        assertEquals(cache1.hashCode(), cache2.hashCode());

        cache2.put(4, "D");

        // cache2 should now contain {2=B,3=C,4=D}; verify contents match
        Map<Integer, String> expected = new LinkedHashMap<>();
        expected.put(2, "B");
        expected.put(3, "C");
        expected.put(4, "D");
        assertEquals(expected, cache2); // equals() is valid, hashCode() equality is not required
    }

    @Test
    void testHashCodeConsistencyAfterOperations() {
        TTLCache<Integer, String> cache = new TTLCache<>(10000, 3);
        cache.put(1, "A");
        cache.put(2, "B");

        int initial = cache.hashCode();

        cache.put(3, "C");
        cache.remove(3);
        cache.put(2, "B");

        assertEquals(initial, cache.hashCode());

        cache.put(2, "Z");
        assertNotEquals(initial, cache.hashCode());
    }
     
    @Test
    void testUpdateDoesNotCreateExtraNodes() throws Exception {
        TTLCache<Integer, String> cache = new TTLCache<>(10000, 2);
        cache.put(1, "A");
        int nodeCount = getNodeCount(cache);

        cache.put(1, "B");
        assertEquals(nodeCount, getNodeCount(cache), "Updating key should not add LRU nodes");

        cache.put(2, "C");
        cache.put(3, "D");

        assertEquals(2, cache.size());
        assertFalse(cache.containsKey(1));
    }

    @Test
    void testHashCodeAfterUpdate() {
        TTLCache<Integer, String> cache1 = new TTLCache<>(10000, 3);
        TTLCache<Integer, String> cache2 = new TTLCache<>(10000, 3);

        cache1.put(1, "A");
        cache2.put(1, "A");

        cache1.put(1, "B");
        cache2.put(1, "B");

        cache1.put(2, "C");
        cache2.put(2, "C");

        assertEquals(cache1.hashCode(), cache2.hashCode());
    }

    // Helper method to count the number of nodes in the LRU list
    private static int getNodeCount(TTLCache<?, ?> cache) throws Exception {
        java.lang.reflect.Field headField = TTLCache.class.getDeclaredField("head");
        headField.setAccessible(true);
        Object head = headField.get(cache);

        java.lang.reflect.Field tailField = TTLCache.class.getDeclaredField("tail");
        tailField.setAccessible(true);
        Object tail = tailField.get(cache);

        java.lang.reflect.Field nextField = head.getClass().getDeclaredField("next");

        int count = 0;
        Object node = nextField.get(head);
        while (node != tail) {
            count++;
            node = nextField.get(node);
        }
        return count;
    }

    @Test
    void testToString() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");

        String cacheString = ttlCache.toString();
        assertTrue(cacheString.contains("1=A"));
        assertTrue(cacheString.contains("2=B"));
        assertTrue(cacheString.contains("3=C"));

        TTLCache<String, String> cache = new TTLCache<>(10000, 100);
        assertEquals("{}", cache.toString());
        assertEquals(0, cache.size());
    }

    @Test
    void testFullCycle() {
        ttlCache = new TTLCache<>(10000, 3);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        ttlCache.put(4, "D");
        ttlCache.put(5, "E");
        ttlCache.put(6, "F");

        // Only the last 3 entries should be present due to LRU eviction
        assertEquals(3, ttlCache.size(), "Cache size should be 3 after eviction");
        assertTrue(ttlCache.containsKey(4));
        assertTrue(ttlCache.containsKey(5));
        assertTrue(ttlCache.containsKey(6));
        assertFalse(ttlCache.containsKey(1));
        assertFalse(ttlCache.containsKey(2));
        assertFalse(ttlCache.containsKey(3));

        assertEquals("D", ttlCache.get(4));
        assertEquals("E", ttlCache.get(5));
        assertEquals("F", ttlCache.get(6));

        ttlCache.remove(6);
        ttlCache.remove(5);
        ttlCache.remove(4);
        assertEquals(0, ttlCache.size(), "Cache should be empty after removing all elements");
    }

    @Test
    void testCacheWhenEmpty() {
        ttlCache = new TTLCache<>(10000, -1);
        assertNull(ttlCache.get(1));
    }

    @Test
    void testCacheClear() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.clear();

        assertNull(ttlCache.get(1));
        assertNull(ttlCache.get(2));
    }

    @Test
    void testPutTwiceSameKey() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");
        ttlCache.put(1, "B");

        assertEquals(1, ttlCache.size());
        assertEquals("B", ttlCache.get(1));

        TTLCache<Integer, String> expected = new TTLCache<>(10000, -1);
        expected.put(1, "B");
        assertEquals(expected.hashCode(), ttlCache.hashCode());
    }

    @Test
    void testNullValue() {
        ttlCache = new TTLCache<>(10000, 100);
        ttlCache.put(1, null);
        assertTrue(ttlCache.containsKey(1));
        assertTrue(ttlCache.containsValue(null));
        assertTrue(ttlCache.toString().contains("1=null"));
        assertNotEquals(0, ttlCache.hashCode());
    }

    @Test
    void testNullKey() {
        ttlCache = new TTLCache<>(10000, 100);
        ttlCache.put(null, "true");
        assertTrue(ttlCache.containsKey(null));
        assertTrue(ttlCache.containsValue("true"));
        assertTrue(ttlCache.toString().contains("null=true"));
        assertNotEquals(0, ttlCache.hashCode());
    }

    @Test
    void testNullKeyValue() {
        ttlCache = new TTLCache<>(10000, 100);
        ttlCache.put(null, null);
        assertTrue(ttlCache.containsKey(null));
        assertTrue(ttlCache.containsValue(null));
        assertTrue(ttlCache.toString().contains("null=null"));
        assertEquals(0, ttlCache.hashCode()); // null key ^ null value = 0, finalizeHash(0) = 0

        TTLCache<Integer, String> cache1 = new TTLCache<>(10000, 3);
        cache1.put(null, null);
        TTLCache<Integer, String> cache2 = new TTLCache<>(10000, 3);
        cache2.put(null, null);
        assertEquals(cache1, cache2);
        assertEquals(cache1.hashCode(), cache2.hashCode());
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testSpeed() {
        long startTime = System.currentTimeMillis();
        TTLCache<Integer, Boolean> cache = new TTLCache<>(100000, 1000000);
        for (int i = 0; i < 1000000; i++) {
            cache.put(i, true);
        }
        long endTime = System.currentTimeMillis();
        LOG.info("TTLCache speed: " + (endTime - startTime) + "ms");
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testTTLWithoutLRU() throws InterruptedException {
        ttlCache = new TTLCache<>(2000, -1); // TTL of 2 seconds, no LRU
        ttlCache.put(1, "A");

        // Immediately check that the entry exists
        assertEquals("A", ttlCache.get(1));

        // Wait for less than TTL
        Thread.sleep(1000);
        assertEquals("A", ttlCache.get(1));

        // Wait for TTL to expire
        Thread.sleep(1500);
        assertNull(ttlCache.get(1), "Entry should have expired after TTL");
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testTTLWithLRU() throws InterruptedException {
        ttlCache = new TTLCache<>(2000, 2); // TTL of 2 seconds, max size of 2
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C"); // This should evict key 1 (least recently used)

        assertNull(ttlCache.get(1), "Entry for key 1 should be evicted due to LRU");
        assertEquals("B", ttlCache.get(2));
        assertEquals("C", ttlCache.get(3));

        // Wait for TTL to expire
        Thread.sleep(2500);
        assertNull(ttlCache.get(2), "Entry for key 2 should have expired due to TTL");
        assertNull(ttlCache.get(3), "Entry for key 3 should have expired due to TTL");
    }

    @Test
    void testAccessResetsLRUOrder() {
        ttlCache = new TTLCache<>(10000, 3);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");

        // Access key 1 and 2
        ttlCache.get(1);
        ttlCache.get(2);

        // Add another entry to trigger eviction
        ttlCache.put(4, "D");

        // Key 3 should be evicted (least recently used)
        assertNull(ttlCache.get(3), "Entry for key 3 should be evicted");
        assertEquals("A", ttlCache.get(1));
        assertEquals("B", ttlCache.get(2));
        assertEquals("D", ttlCache.get(4));
    }

    @Test
    void testIteratorRemove() {
        ttlCache = new TTLCache<>(10000, -1);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");

        Iterator<Map.Entry<Integer, String>> iterator = ttlCache.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, String> entry = iterator.next();
            if (entry.getKey().equals(2)) {
                iterator.remove();
            }
        }

        assertEquals(2, ttlCache.size());
        assertFalse(ttlCache.containsKey(2));
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testExpirationDuringIteration() throws InterruptedException {
        ttlCache = new TTLCache<>(1000, -1, 100);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");

        // Wait for TTL to expire
        Thread.sleep(1500);

        int count = 0;
        for (Map.Entry<Integer, String> entry : ttlCache.entrySet()) {
            count++;
        }

        assertEquals(0, count, "No entries should be iterated after TTL expiry");
    }

    // Use this test to "See" the pattern, by adding a LOG.info(toString()) of the cache contents to the top
    // of the purgeExpiredEntries() method.
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testTwoIndependentCaches()
    {
        TTLCache<Integer, String> ttlCache1 = new TTLCache<>(1000, -1, 100);
        ttlCache1.put(1, "A");
        ttlCache1.put(2, "B");

        TTLCache<Integer, String> ttlCache2 = new TTLCache<>(2000, -1, 200);
        ttlCache2.put(10, "X");
        ttlCache2.put(20, "Y");
        ttlCache2.put(30, "Z");

        try {
            Thread.sleep(1500);
            assert ttlCache1.isEmpty();
            assert !ttlCache2.isEmpty();
            Thread.sleep(1000);
            assert ttlCache2.isEmpty();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCloseCancelsFuture() {
        TTLCache<Integer, String> cache = new TTLCache<>(1000, -1, 100);
        ScheduledFuture<?> future = cache.getPurgeFuture();
        assertFalse(future.isCancelled());
        cache.close();
        assertTrue(future.isCancelled());
    }
}
