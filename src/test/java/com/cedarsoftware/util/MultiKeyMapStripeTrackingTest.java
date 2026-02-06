package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test for bug #5: Stripe contention diagnostics track the wrong stripe.
 *
 * Bug: putInternal and removeInternal computed the stripe index for contention
 * tracking as {@code hash & STRIPE_MASK}, but getStripeLock computes it as
 * {@code (spread(hash) & tableMask) & STRIPE_MASK}. When the table is smaller
 * than STRIPE_COUNT, these produce different values, so per-stripe metrics
 * were attributed to incorrect stripes.
 */
class MultiKeyMapStripeTrackingTest {

    /** Mirrors MultiKeyMap.spread() */
    private static int spread(int h) {
        return h ^ (h >>> 16);
    }

    @Test
    void testPutTracksAcquisitionOnCorrectStripe() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);

        // Access internals via reflection
        Field stripeAcqField = MultiKeyMap.class.getDeclaredField("stripeLockAcquisitions");
        stripeAcqField.setAccessible(true);
        AtomicInteger[] acq = (AtomicInteger[]) stripeAcqField.get(map);

        Field stripeMaskField = MultiKeyMap.class.getDeclaredField("STRIPE_MASK");
        stripeMaskField.setAccessible(true);
        int stripeMask = stripeMaskField.getInt(null);

        Field bucketsField = MultiKeyMap.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        AtomicReferenceArray<?> table = (AtomicReferenceArray<?>) bucketsField.get(map);
        int tableMask = table.length() - 1;

        // Bug only manifests when tableMask < stripeMask
        if (tableMask >= stripeMask) {
            return; // Can't trigger on this machine/config
        }

        // Find a key where the correct stripe (with spread) differs from
        // the old buggy stripe (hash & STRIPE_MASK without spread or tableMask)
        String testKey = null;
        int testHash = 0;
        for (int i = 0; i < 10000; i++) {
            String candidate = "key" + i;
            int h = candidate.hashCode();
            int correctStripe = (spread(h) & tableMask) & stripeMask;
            int buggyStripe = h & stripeMask;
            if (correctStripe != buggyStripe) {
                testKey = candidate;
                testHash = h;
                break;
            }
        }
        assertNotNull(testKey, "Should find a key where correct and buggy stripes differ");

        int expectedStripe = (spread(testHash) & tableMask) & stripeMask;

        // Perform a put
        map.put(testKey, "value");

        // Verify acquisition was tracked on the correct stripe
        assertEquals(1, acq[expectedStripe].get(),
                "Acquisition should be tracked on stripe " + expectedStripe + " (matching getStripeIndex)");
    }

    @Test
    void testRemoveTracksAcquisitionOnCorrectStripe() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);

        Field stripeAcqField = MultiKeyMap.class.getDeclaredField("stripeLockAcquisitions");
        stripeAcqField.setAccessible(true);
        AtomicInteger[] acq = (AtomicInteger[]) stripeAcqField.get(map);

        Field stripeMaskField = MultiKeyMap.class.getDeclaredField("STRIPE_MASK");
        stripeMaskField.setAccessible(true);
        int stripeMask = stripeMaskField.getInt(null);

        Field bucketsField = MultiKeyMap.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        AtomicReferenceArray<?> table = (AtomicReferenceArray<?>) bucketsField.get(map);
        int tableMask = table.length() - 1;

        if (tableMask >= stripeMask) {
            return;
        }

        String testKey = null;
        int testHash = 0;
        for (int i = 0; i < 10000; i++) {
            String candidate = "key" + i;
            int h = candidate.hashCode();
            int correctStripe = (spread(h) & tableMask) & stripeMask;
            int buggyStripe = h & stripeMask;
            if (correctStripe != buggyStripe) {
                testKey = candidate;
                testHash = h;
                break;
            }
        }
        assertNotNull(testKey);

        int expectedStripe = (spread(testHash) & tableMask) & stripeMask;

        // Put the key first (this also increments the correct stripe's counter)
        map.put(testKey, "value");
        int acqAfterPut = acq[expectedStripe].get();

        // Now remove it - should also track on the correct stripe
        map.remove(testKey);

        assertEquals(acqAfterPut + 1, acq[expectedStripe].get(),
                "Remove acquisition should be tracked on stripe " + expectedStripe);
    }

    @Test
    void testNoAcquisitionsAboveTableSizeStripes() throws Exception {
        MultiKeyMap<String> map = new MultiKeyMap<>(16);

        Field stripeAcqField = MultiKeyMap.class.getDeclaredField("stripeLockAcquisitions");
        stripeAcqField.setAccessible(true);
        AtomicInteger[] acq = (AtomicInteger[]) stripeAcqField.get(map);

        Field bucketsField = MultiKeyMap.class.getDeclaredField("buckets");
        bucketsField.setAccessible(true);
        AtomicReferenceArray<?> table = (AtomicReferenceArray<?>) bucketsField.get(map);
        int tableSize = table.length();

        if (tableSize >= acq.length) {
            return; // Bug can't manifest
        }

        // Put several keys (but not enough to trigger resize: capacity 16 * 0.75 = 12)
        for (int i = 0; i < 10; i++) {
            map.put("item" + i, "val" + i);
        }

        // With table size 16 and stripe count 32, the stripe index is
        // (spread(hash) & 15) & 31 which can only produce values in [0, 15].
        // Stripes [16, 31] should never receive acquisitions.
        for (int s = tableSize; s < acq.length; s++) {
            assertEquals(0, acq[s].get(),
                    "Stripe " + s + " is above table size " + tableSize
                            + " and should have no acquisitions");
        }
    }
}
