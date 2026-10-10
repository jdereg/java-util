package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TTLCache at the edges of its arguments.
 */
class TTLCacheLimitsTest {

    /**
     * A TTL so long that now + TTL passes Long.MAX_VALUE means "practically forever": every way of writing an
     * entry keeps it, unbounded and size-limited.
     */
    @Test
    void aHugeTtlNeverExpiresAnEntryAtBirth() {
        for (long ttl : new long[]{Long.MAX_VALUE, Long.MAX_VALUE - 1000, Long.MAX_VALUE / 2 + 1}) {
            for (int maxSize : new int[]{-1, 10}) {
                TTLCache<String, String> cache = new TTLCache<>(ttl, maxSize, 3_600_000);
                try {
                    String where = "ttl " + ttl + ", maxSize " + maxSize;
                    cache.put("put", "v");
                    assertEquals("v", cache.computeIfAbsent("computed", k -> "v"), where);
                    cache.putIfAbsent("putIfAbsent", "v");
                    for (String key : new String[]{"put", "computed", "putIfAbsent"}) {
                        assertEquals("v", cache.get(key), where + ", " + key);
                        assertTrue(cache.containsKey(key), where + ", " + key);
                    }
                    assertEquals(3, cache.keySet().size(), where);
                } finally {
                    cache.close();
                }
            }
        }
    }

    /** What a cache was built with reads back: all a copy, or a serializer, needs to build another like it. */
    @Test
    void itsSettingsReadBack() {
        try (TTLCache<String, String> cache = new TTLCache<>(5000, 10, 250)) {
            assertEquals(5000, cache.getTtlMillis());
            assertEquals(10, cache.getMaxSize());
            assertEquals(250, cache.getCleanupIntervalMillis());
        }
        try (TTLCache<String, String> cache = new TTLCache<>(5000)) {     // no size limit, purged every minute
            assertEquals(-1, cache.getMaxSize());
            assertEquals(60000, cache.getCleanupIntervalMillis());
        }
        try (TTLCache<String, String> cache = new TTLCache<>(Long.MAX_VALUE, 3)) {
            assertEquals(Long.MAX_VALUE / 2, cache.getTtlMillis());     // as held: "practically forever"
            assertEquals(3, cache.getMaxSize());
        }
    }
}
