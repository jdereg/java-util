package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The caches that keep their entries in a linked list -- LRUCache's LOCKING strategy and TTLCache -- leave no link in
 * an entry they unlink. A removed node that still pointed at its neighbors would keep them reachable from wherever it
 * lingers (an old-generation node, live until the next full collection, holding young ones: GC nepotism), and a chain
 * of such nodes would keep a cache's whole history alive. Every path that unlinks is checked: remove, eviction for
 * size, replacement of a key, expiry and clear.
 *
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
class CacheUnlinkedNodeTest {

    // ---------- LRUCache, LOCKING strategy ----------

    private static Object lockingNode(LRUCache<String, String> cache, String key) throws Exception {
        Object strategy = field(LRUCache.class, "strategy").get(cache);
        Map<?, ?> nodes = (Map<?, ?>) field(strategy.getClass(), "cache").get(strategy);
        Object node = nodes.get(key);
        assertNotNull(node, key + " is cached");
        return node;
    }

    private static LRUCache<String, String> lockingCache(int capacity, int entries) {
        LRUCache<String, String> cache = new LRUCache<>(capacity, LRUCache.StrategyType.LOCKING);
        for (int i = 0; i < entries; i++) {
            cache.put("k" + i, "v" + i);
        }
        return cache;
    }

    @Test
    void lockingRemove() throws Exception {
        LRUCache<String, String> cache = lockingCache(10, 5);
        Object node = lockingNode(cache, "k2");
        cache.remove("k2");
        assertUnlinked(node);
    }

    @Test
    void lockingEvictionForSize() throws Exception {
        LRUCache<String, String> cache = lockingCache(5, 5);
        Object oldest = lockingNode(cache, "k0");
        cache.put("k5", "v5");                       // k0, never read, has no second chance
        assertNull(cache.get("k0"));
        assertUnlinked(oldest);
    }

    @Test
    void lockingClear() throws Exception {
        LRUCache<String, String> cache = lockingCache(10, 5);
        Object first = lockingNode(cache, "k0");
        Object middle = lockingNode(cache, "k2");
        cache.clear();
        assertUnlinked(first);
        assertUnlinked(middle);
    }

    @Test
    void lockingRemovalThroughCompute() throws Exception {
        LRUCache<String, String> cache = lockingCache(10, 5);
        Object node = lockingNode(cache, "k3");
        cache.compute("k3", (k, v) -> null);
        assertUnlinked(node);
    }

    // ---------- TTLCache ----------

    private static Object ttlNode(TTLCache<String, String> cache, String key) throws Exception {
        Map<?, ?> entries = (Map<?, ?>) field(TTLCache.class, "cacheMap").get(cache);
        Object entry = entries.get(key);
        assertNotNull(entry, key + " is cached");
        return field(entry.getClass(), "node").get(entry);
    }

    private static TTLCache<String, String> ttlCache(long ttlMillis, int maxSize, int entries) {
        TTLCache<String, String> cache = new TTLCache<>(ttlMillis, maxSize);
        for (int i = 0; i < entries; i++) {
            cache.put("k" + i, "v" + i);
        }
        return cache;
    }

    @Test
    void ttlRemove() throws Exception {
        TTLCache<String, String> cache = ttlCache(60_000, 10, 5);
        try {
            Object node = ttlNode(cache, "k2");
            cache.remove("k2");
            assertUnlinked(node);
        } finally {
            cache.close();
        }
    }

    @Test
    void ttlEvictionForSize() throws Exception {
        TTLCache<String, String> cache = ttlCache(60_000, 5, 5);
        try {
            Object oldest = ttlNode(cache, "k0");
            cache.put("k5", "v5");
            assertNull(cache.get("k0"));
            assertUnlinked(oldest);
        } finally {
            cache.close();
        }
    }

    /** Putting a key again replaces its node: the old one is unlinked. */
    @Test
    void ttlReplacement() throws Exception {
        TTLCache<String, String> cache = ttlCache(60_000, 10, 5);
        try {
            Object old = ttlNode(cache, "k2");
            cache.put("k2", "again");
            if (ttlNode(cache, "k2") != old) {
                assertUnlinked(old);
            }
        } finally {
            cache.close();
        }
    }

    @Test
    void ttlExpiry() throws Exception {
        TTLCache<String, String> cache = ttlCache(1, 10, 5);
        try {
            Object node = ttlNode(cache, "k2");
            Thread.sleep(20);
            assertNull(cache.get("k2"));                 // an expired entry is removed when it is read
            assertUnlinked(node);
        } finally {
            cache.close();
        }
    }

    @Test
    void ttlClear() throws Exception {
        TTLCache<String, String> cache = ttlCache(60_000, 10, 5);
        try {
            Object first = ttlNode(cache, "k0");
            Object middle = ttlNode(cache, "k2");
            cache.clear();
            assertUnlinked(first);
            assertUnlinked(middle);
        } finally {
            cache.close();
        }
    }

    private static void assertUnlinked(Object node) throws Exception {
        assertNull(field(node.getClass(), "prev").get(node), "an unlinked node keeps its prev");
        assertNull(field(node.getClass(), "next").get(node), "an unlinked node keeps its next");
    }

    private static Field field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
