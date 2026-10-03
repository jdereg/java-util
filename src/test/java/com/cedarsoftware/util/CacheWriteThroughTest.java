package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * replaceAll and Map.Entry.setValue on LRUCache (both strategies) and TTLCache (size-limited and unbounded): they
 * either change the cache or refuse, never silently do nothing.
 */
class CacheWriteThroughTest {

    @AfterAll
    static void shutdown() {
        TTLCache.shutdown();
    }

    private static Map<String, Supplier<Map<String, Integer>>> caches() {
        Map<String, Supplier<Map<String, Integer>>> caches = new LinkedHashMap<>();
        caches.put("LRUCache LOCKING", () -> new LRUCache<>(10, LRUCache.StrategyType.LOCKING));
        caches.put("LRUCache THREADED", () -> new LRUCache<>(10, LRUCache.StrategyType.THREADED));
        caches.put("TTLCache size-limited", () -> new TTLCache<>(3_600_000, 10));
        caches.put("TTLCache unbounded", () -> new TTLCache<>(3_600_000));
        return caches;
    }

    /** replaceAll used to write through the entry set's snapshot entries: the cache never changed. */
    @Test
    void replaceAllReplacesEveryValue() {
        for (Map.Entry<String, Supplier<Map<String, Integer>>> e : caches().entrySet()) {
            Map<String, Integer> cache = e.getValue().get();
            cache.put("a", 1);
            cache.put("b", 2);
            cache.put("c", null);
            cache.replaceAll((k, v) -> v == null ? -1 : v * 10);
            assertEquals(Integer.valueOf(10), cache.get("a"), e.getKey());
            assertEquals(Integer.valueOf(20), cache.get("b"), e.getKey());
            assertEquals(Integer.valueOf(-1), cache.get("c"), e.getKey());
            assertEquals(3, cache.size(), e.getKey());
        }
    }

    /**
     * An entry from the entry set either writes through (TTLCache, whose entry set is a view of the cache) or
     * refuses (LRUCache, whose entry set is a snapshot); setValue used to change only a copy.
     */
    @Test
    void entrySetValueWritesThroughOrRefuses() {
        for (Map.Entry<String, Supplier<Map<String, Integer>>> e : caches().entrySet()) {
            Map<String, Integer> cache = e.getValue().get();
            cache.put("a", 1);
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(cache.entrySet());
            Map.Entry<String, Integer> entry = entries.get(0);
            if (cache instanceof TTLCache) {
                assertEquals(Integer.valueOf(1), entry.setValue(2), e.getKey());
                assertEquals(Integer.valueOf(2), cache.get("a"), e.getKey());
            } else {
                assertThrows(UnsupportedOperationException.class, () -> entry.setValue(2), e.getKey());
                assertEquals(Integer.valueOf(1), cache.get("a"), e.getKey());
            }
        }
    }
}
