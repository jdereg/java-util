package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * TTLCache's background purge keeps running after a purge fails.
 */
class TTLCachePurgeTest {

    private static final String PURGE_THREAD = "TTLCache-Purge-Thread";

    @AfterAll
    static void shutdown() {
        TTLCache.shutdown();
    }

    /** A key whose hashCode() throws on the purge thread while poisoned. */
    static final class PoisonKey {
        static volatile boolean poisoned;
        private final int id;

        PoisonKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            if (poisoned && Thread.currentThread().getName().equals(PURGE_THREAD)) {
                throw new IllegalStateException("transient failure");
            }
            return id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof PoisonKey && ((PoisonKey) o).id == id;
        }
    }

    private static int backingSize(TTLCache<?, ?> cache) throws Exception {
        Field field = TTLCache.class.getDeclaredField("cacheMap");
        field.setAccessible(true);
        return ((Map<?, ?>) field.get(cache)).size();
    }

    private static void awaitEmptyBackingMap(TTLCache<?, ?> cache, long millis) throws Exception {
        long deadline = System.currentTimeMillis() + millis;
        while (backingSize(cache) > 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
    }

    /** One purge throwing must not end the periodic purge: later expired entries are still removed. */
    @Test
    void aFailingPurgeDoesNotStopLaterPurges() throws Exception {
        TTLCache<Object, String> cache = new TTLCache<>(20, -1, 10);
        try {
            PoisonKey.poisoned = true;
            cache.put(new PoisonKey(1), "v");
            Thread.sleep(150);                      // the entry expired and purges failed on it
            PoisonKey.poisoned = false;
            for (int i = 0; i < 100; i++) {
                cache.put("s" + i, "v");
            }
            awaitEmptyBackingMap(cache, 2000);
            assertFalse(cache.getPurgeFuture().isDone(), "the periodic purge ended");
            assertEquals(0, backingSize(cache));
        } finally {
            PoisonKey.poisoned = false;
            cache.close();
        }
    }
}
