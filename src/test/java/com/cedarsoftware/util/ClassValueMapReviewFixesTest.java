package com.cedarsoftware.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the 4.106.0 review fixes to ClassValueMap:
 * <p>
 * Fix 1: getOrDefault() previously inherited ConcurrentMap's default implementation, which
 * assumes the map cannot contain null values (null from get() is treated as "absent").
 * ClassValueMap supports null values, so a key explicitly mapped to null incorrectly
 * returned the default value instead of null. The override distinguishes "mapped to null"
 * from "no mapping" for both Class keys and the null key.
 * <p>
 * Fix 2: computeIfAbsent() hit path now reads through the ClassValue cache (identity-based)
 * instead of a backingMap hash probe. These tests pin the semantics of the new hit path:
 * no function invocation on hit, correct interplay with cache invalidation (put/remove/clear),
 * and single computation under concurrent racing.
 */
class ClassValueMapReviewFixesTest {

    // --- Fix 1: getOrDefault with null-value mappings ---

    @Test
    void testGetOrDefaultReturnsNullForNullValuedClassKey() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, null);

        assertNull(map.getOrDefault(String.class, "DEFAULT"),
                "Key present and mapped to null must return null, not the default");
    }

    @Test
    void testGetOrDefaultReturnsNullForNullValuedNullKey() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(null, null);

        assertNull(map.getOrDefault(null, "DEFAULT"),
                "Null key mapped to null must return null, not the default");
    }

    @Test
    void testGetOrDefaultReturnsValueForNonNullMappings() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(Integer.class, "int");
        map.put(null, "nullKey");

        assertEquals("int", map.getOrDefault(Integer.class, "DEFAULT"));
        assertEquals("nullKey", map.getOrDefault(null, "DEFAULT"));
    }

    @Test
    void testGetOrDefaultReturnsDefaultWhenAbsent() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(Integer.class, "int");

        assertEquals("DEFAULT", map.getOrDefault(Long.class, "DEFAULT"), "Absent Class key");
        assertEquals("DEFAULT", map.getOrDefault(null, "DEFAULT"), "Absent null key");
        assertEquals("DEFAULT", map.getOrDefault("notAClass", "DEFAULT"), "Non-Class key can never be present");
    }

    @Test
    void testGetOrDefaultTracksRemoveAndClear() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, null);
        assertNull(map.getOrDefault(String.class, "DEFAULT"));

        map.remove(String.class);
        assertEquals("DEFAULT", map.getOrDefault(String.class, "DEFAULT"),
                "After remove, the mapping is gone and the default must be returned");

        map.put(String.class, null);
        map.put(null, null);
        map.clear();
        assertEquals("DEFAULT", map.getOrDefault(String.class, "DEFAULT"),
                "After clear, the null-valued mapping is gone (cache instance swapped)");
        assertEquals("DEFAULT", map.getOrDefault(null, "DEFAULT"),
                "After clear, the null-key mapping is gone");
    }

    // --- Fix 2: computeIfAbsent hit path through the ClassValue cache ---

    @Test
    void testComputeIfAbsentHitDoesNotInvokeFunction() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "existing");

        AtomicInteger calls = new AtomicInteger();
        String result = map.computeIfAbsent(String.class, k -> {
            calls.incrementAndGet();
            return "computed";
        });

        assertEquals("existing", result);
        assertEquals(0, calls.get(), "Mapping function must not run when a non-null value exists");
    }

    @Test
    void testComputeIfAbsentAfterRemoveRecomputes() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "first");
        assertEquals("first", map.computeIfAbsent(String.class, k -> "second"));

        map.remove(String.class);
        assertEquals("second", map.computeIfAbsent(String.class, k -> "second"),
                "remove() must invalidate the cached value so computeIfAbsent recomputes");
        assertEquals("second", map.get(String.class));
    }

    @Test
    void testComputeIfAbsentResultVisibleThroughAllReadPaths() {
        ClassValueMap<String> map = new ClassValueMap<>();
        // Prime the negative cache first so a stale NO_VALUE entry would be exposed
        assertNull(map.get(String.class));
        assertNull(map.getByClass(String.class));

        String computed = map.computeIfAbsent(String.class, k -> "value");
        assertEquals("value", computed);
        assertEquals("value", map.get(String.class), "get() must see the installed value");
        assertEquals("value", map.getByClass(String.class), "getByClass() must see the installed value");
        assertEquals("value", map.getOrDefault(String.class, "DEFAULT"));
        assertTrue(map.containsKey(String.class));
    }

    @Test
    void testComputeIfAbsentReplacesNullValuedMapping() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, null);
        // Prime the cache with the null-valued mapping
        assertNull(map.get(String.class));

        String result = map.computeIfAbsent(String.class, k -> "computed");
        assertEquals("computed", result, "Null-valued mapping is treated as absent per Map spec");
        assertEquals("computed", map.get(String.class));
    }

    @Test
    void testComputeIfAbsentConcurrentRacersComputeOnce() throws Exception {
        final int THREADS = 8;
        ClassValueMap<Object> map = new ClassValueMap<>();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            java.util.List<Future<Object>> futures = new java.util.ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return map.computeIfAbsent(String.class, k -> {
                        calls.incrementAndGet();
                        return new Object();
                    });
                }));
            }
            start.countDown();
            Object first = futures.get(0).get(10, TimeUnit.SECONDS);
            for (Future<Object> f : futures) {
                assertSame(first, f.get(10, TimeUnit.SECONDS),
                        "All racing threads must observe the same installed instance");
            }
            assertEquals(1, calls.get(), "Mapping function must run exactly once across racers");
        } finally {
            pool.shutdownNow();
        }
    }
}
