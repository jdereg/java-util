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
 * Bug: putInternal and removeInternal compute the stripe index for contention
 * tracking as {@code hash & STRIPE_MASK}, but getStripeLock computes it as
 * {@code (hash & tableMask) & STRIPE_MASK}. When the table is smaller than
 * STRIPE_COUNT (e.g., default capacity 16 vs STRIPE_COUNT 32), these produce
 * different values. For example, with hash=112, tableMask=15, STRIPE_MASK=31:
 *   - getStripeLock:  (112 & 15) & 31 = 0  (actual lock acquired on stripe 0)
 *   - putInternal:     112 & 31      = 16  (contention tracked on stripe 16)
 *
 * This means per-stripe contention metrics are attributed to the wrong stripes.
 */
class MultiKeyMapStripeTrackingTest {

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

        // Find a key whose hash has bits set in (stripeMask & ~tableMask),
        // causing the buggy and correct stripe computations to differ.
        int diffBits = stripeMask & ~tableMask;

        String testKey = null;
        int testHash = 0;
        for (int i = 0; i < 10000; i++) {
            String candidate = "key" + i;
            int h = candidate.hashCode();
            if ((h & diffBits) != 0) {
                testKey = candidate;
                testHash = h;
                break;
            }
        }
        assertNotNull(testKey, "Should find a key with hash bits in the differing range");

        int correctStripe = (testHash & tableMask) & stripeMask;
        int buggyStripe = testHash & stripeMask;
        assertNotEquals(correctStripe, buggyStripe,
                "Precondition: correct and buggy stripes must differ for this key");

        // Perform a put
        map.put(testKey, "value");

        // Verify acquisition was tracked on the correct stripe (the one matching getStripeLock)
        assertEquals(1, acq[correctStripe].get(),
                "Acquisition should be tracked on stripe " + correctStripe + " (matching getStripeLock)");
        assertEquals(0, acq[buggyStripe].get(),
                "Stripe " + buggyStripe + " should have no acquisitions (wrong stripe)");
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

        int diffBits = stripeMask & ~tableMask;

        String testKey = null;
        int testHash = 0;
        for (int i = 0; i < 10000; i++) {
            String candidate = "key" + i;
            int h = candidate.hashCode();
            if ((h & diffBits) != 0) {
                testKey = candidate;
                testHash = h;
                break;
            }
        }
        assertNotNull(testKey);

        int correctStripe = (testHash & tableMask) & stripeMask;
        int buggyStripe = testHash & stripeMask;
        assertNotEquals(correctStripe, buggyStripe);

        // Put the key first (this also increments the correct stripe's counter)
        map.put(testKey, "value");
        int acqAfterPut = acq[correctStripe].get();

        // Now remove it - should also track on the correct stripe
        map.remove(testKey);

        assertEquals(acqAfterPut + 1, acq[correctStripe].get(),
                "Remove acquisition should be tracked on stripe " + correctStripe);
        assertEquals(0, acq[buggyStripe].get(),
                "Stripe " + buggyStripe + " should have no acquisitions");
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

        // With table size 16 and stripe count 32, the correct stripe index
        // is (hash & 15) & 31 which can only produce values in [0, 15].
        // Stripes [16, 31] should never receive acquisitions.
        for (int s = tableSize; s < acq.length; s++) {
            assertEquals(0, acq[s].get(),
                    "Stripe " + s + " is above table size " + tableSize
                            + " and should have no acquisitions");
        }
    }
}
