package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UniqueIdGeneratorEdgeCaseTest {

    @Test
    void testServerIdNormalizationHandlesIntegerMinValue() throws Exception {
        String key = "UNIQUE_ID_TEST_MIN_VALUE";
        System.setProperty(key, String.valueOf(Integer.MIN_VALUE));
        try {
            Method method = UniqueIdGenerator.class.getDeclaredMethod("getServerIdFromVarName", String.class);
            method.setAccessible(true);

            int serverId = (Integer) method.invoke(null, key);
            assertEquals(48, serverId);
        } finally {
            System.clearProperty(key);
        }
    }

    @Test
    void testGetUniqueId19ThrowsWhenLongRangeExhausted() throws Exception {
        assertThrowsWhenRangeExhausted("LAST_ID_19", UniqueIdGenerator::getUniqueId19);
    }

    @Test
    void testGetUniqueIdThrowsWhenLongRangeExhausted() throws Exception {
        assertThrowsWhenRangeExhausted("LAST_ID_16", UniqueIdGenerator::getUniqueId);
    }

    private void assertThrowsWhenRangeExhausted(String fieldName, IdSupplier supplier) throws Exception {
        Field field = UniqueIdGenerator.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        AtomicLong counter = (AtomicLong) field.get(null);

        long original = counter.get();
        int serverId = UniqueIdGenerator.getServerIdConfigured();
        long nearMaxWithServerSuffix = Long.MAX_VALUE - Math.floorMod(Long.MAX_VALUE - serverId, 100L);

        synchronized (UniqueIdGenerator.class) {
            try {
                counter.set(nearMaxWithServerSuffix);
                assertThrows(IllegalStateException.class, supplier::get);
            } finally {
                counter.set(original);
            }
        }
    }

    @FunctionalInterface
    private interface IdSupplier {
        long get();
    }
}
