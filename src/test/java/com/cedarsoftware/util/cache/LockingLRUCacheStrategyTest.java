package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LockingLRUCacheStrategyTest {
    @Test
    void testGetCapacity() {
        LockingLRUCacheStrategy<String, Integer> cache = new LockingLRUCacheStrategy<>(5);
        assertEquals(5, cache.getCapacity());
    }
}
