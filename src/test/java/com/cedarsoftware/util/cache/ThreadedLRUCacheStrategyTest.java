package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThreadedLRUCacheStrategyTest {

    @Test
    void testGetCapacityReturnsConstructorValue() {
        ThreadedLRUCacheStrategy<Integer, String> cache = new ThreadedLRUCacheStrategy<>(5, 50);
        assertEquals(5, cache.getCapacity());
    }

    @Test
    void testGetCapacityAfterPuts() {
        ThreadedLRUCacheStrategy<Integer, String> cache = new ThreadedLRUCacheStrategy<>(2, 50);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        assertEquals(2, cache.getCapacity());
    }
}
