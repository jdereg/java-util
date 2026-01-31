package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test to verify the fix for [high-6] Integer overflow in size calculations.
 *
 * The issue: When collection/map size approaches Integer.MAX_VALUE, the calculation
 * `size * 4 / 3` for HashMap pre-sizing could overflow, resulting in negative capacity
 * or ArithmeticException.
 *
 * The fix: Created safeHashMapCapacity() method that:
 * 1. Checks for overflow conditions before multiplying
 * 2. Uses long arithmetic to prevent overflow
 * 3. Caps result at Integer.MAX_VALUE
 * 4. Ensures minimum capacity of 16
 */
public class TestIntegerOverflowFix {

    /**
     * Test that the safeHashMapCapacity method exists and is accessible via reflection.
     */
    @Test
    public void testSafeHashMapCapacityMethodExists() throws Exception {
        Method method = DeepEquals.class.getDeclaredMethod("safeHashMapCapacity", int.class);
        assertNotNull(method, "safeHashMapCapacity method should exist");
        method.setAccessible(true);

        // Test normal size
        int capacity = (int) method.invoke(null, 100);
        assertEquals(133, capacity, "Capacity for 100 should be 133 (100*4/3)");
    }

    /**
     * Test that negative sizes are handled gracefully.
     */
    @Test
    public void testNegativeSizeHandling() throws Exception {
        Method method = DeepEquals.class.getDeclaredMethod("safeHashMapCapacity", int.class);
        method.setAccessible(true);

        int capacity = (int) method.invoke(null, -1);
        assertEquals(16, capacity, "Negative size should return default capacity of 16");

        capacity = (int) method.invoke(null, -1000);
        assertEquals(16, capacity, "Large negative size should return default capacity of 16");
    }

    /**
     * Test that zero and small sizes return minimum capacity.
     */
    @Test
    public void testSmallSizes() throws Exception {
        Method method = DeepEquals.class.getDeclaredMethod("safeHashMapCapacity", int.class);
        method.setAccessible(true);

        int capacity = (int) method.invoke(null, 0);
        assertEquals(16, capacity, "Zero size should return minimum capacity of 16");

        capacity = (int) method.invoke(null, 1);
        assertEquals(16, capacity, "Size 1 should return minimum capacity of 16");

        capacity = (int) method.invoke(null, 10);
        assertEquals(16, capacity, "Size 10 should return minimum capacity of 16");

        capacity = (int) method.invoke(null, 12);
        assertEquals(16, capacity, "Size 12 should return minimum capacity of 16");
    }

    /**
     * Test normal capacity calculations.
     */
    @Test
    public void testNormalCapacityCalculations() throws Exception {
        Method method = DeepEquals.class.getDeclaredMethod("safeHashMapCapacity", int.class);
        method.setAccessible(true);

        // Test various normal sizes
        int capacity = (int) method.invoke(null, 100);
        assertEquals(133, capacity, "100 * 4 / 3 = 133");

        capacity = (int) method.invoke(null, 1000);
        assertEquals(1333, capacity, "1000 * 4 / 3 = 1333");

        capacity = (int) method.invoke(null, 1_000_000);
        assertEquals(1_333_333, capacity, "1M * 4 / 3 = 1,333,333");

        capacity = (int) method.invoke(null, 100_000_000);
        assertEquals(133_333_333, capacity, "100M * 4 / 3 = 133,333,333");
    }

    /**
     * Test that sizes near overflow threshold are handled safely.
     */
    @Test
    public void testNearOverflowSizes() throws Exception {
        Method method = DeepEquals.class.getDeclaredMethod("safeHashMapCapacity", int.class);
        method.setAccessible(true);

        // Test size at the threshold: 1,610,612,735 (max safe size)
        // This is the largest size where size * 4 doesn't overflow
        int threshold = 1_610_612_735;
        int capacity = (int) method.invoke(null, threshold);
        assertTrue(capacity > 0, "Capacity at threshold should be positive");
        assertTrue(capacity <= Integer.MAX_VALUE, "Capacity should not exceed Integer.MAX_VALUE");

        // Test size just below threshold
        capacity = (int) method.invoke(null, threshold - 1);
        assertTrue(capacity > 0, "Capacity below threshold should be positive");

        // Test size just above threshold - should cap at Integer.MAX_VALUE
        capacity = (int) method.invoke(null, threshold + 1);
        assertEquals(Integer.MAX_VALUE, capacity, "Size above threshold should return Integer.MAX_VALUE");
    }

    /**
     * Test that very large sizes are capped at Integer.MAX_VALUE.
     */
    @Test
    public void testVeryLargeSizes() throws Exception {
        Method method = DeepEquals.class.getDeclaredMethod("safeHashMapCapacity", int.class);
        method.setAccessible(true);

        // Test size at Integer.MAX_VALUE
        int capacity = (int) method.invoke(null, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, capacity,
            "Size at Integer.MAX_VALUE should return Integer.MAX_VALUE");

        // Test size close to Integer.MAX_VALUE
        capacity = (int) method.invoke(null, Integer.MAX_VALUE - 1000);
        assertEquals(Integer.MAX_VALUE, capacity,
            "Very large size should return Integer.MAX_VALUE");

        capacity = (int) method.invoke(null, 2_000_000_000);
        assertEquals(Integer.MAX_VALUE, capacity,
            "Size of 2B should return Integer.MAX_VALUE");
    }

    /**
     * Test that comparing collections doesn't crash with large sizes.
     * Note: We can't actually create collections with billions of elements in tests,
     * but we verify the method handles the calculation correctly.
     */
    @Test
    public void testLargeCollectionComparison() {
        // Test with reasonably large collections (not billions, but enough to verify)
        List<Integer> list1 = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();

        for (int i = 0; i < 10_000; i++) {
            list1.add(i);
            list2.add(i);
        }

        // Should complete without overflow
        assertTrue(DeepEquals.deepEquals(list1, list2),
            "Large equal collections should be equal");

        list2.set(5000, 99999);
        assertFalse(DeepEquals.deepEquals(list1, list2),
            "Large different collections should not be equal");
    }

    /**
     * Test that comparing maps doesn't crash with large sizes.
     */
    @Test
    public void testLargeMapComparison() {
        Map<Integer, String> map1 = new HashMap<>();
        Map<Integer, String> map2 = new HashMap<>();

        for (int i = 0; i < 10_000; i++) {
            map1.put(i, "value" + i);
            map2.put(i, "value" + i);
        }

        // Should complete without overflow
        assertTrue(DeepEquals.deepEquals(map1, map2),
            "Large equal maps should be equal");

        map2.put(5000, "different");
        assertFalse(DeepEquals.deepEquals(map1, map2),
            "Large different maps should not be equal");
    }

    /**
     * Test that comparing sets doesn't crash with large sizes.
     */
    @Test
    public void testLargeSetComparison() {
        Set<Integer> set1 = new HashSet<>();
        Set<Integer> set2 = new HashSet<>();

        for (int i = 0; i < 10_000; i++) {
            set1.add(i);
            set2.add(i);
        }

        // Should complete without overflow
        assertTrue(DeepEquals.deepEquals(set1, set2),
            "Large equal sets should be equal");

        set2.remove(5000);
        assertFalse(DeepEquals.deepEquals(set1, set2),
            "Large different sets should not be equal");
    }

    /**
     * Test edge case: collection size exactly at overflow risk.
     */
    @Test
    public void testOverflowBoundary() throws Exception {
        Method method = DeepEquals.class.getDeclaredMethod("safeHashMapCapacity", int.class);
        method.setAccessible(true);

        // Calculate the exact boundary where overflow would occur
        // size * 4 overflows when size > Integer.MAX_VALUE / 4 = 536,870,911
        // But we use a safer threshold of 1,610,612,735 (Integer.MAX_VALUE * 3 / 4)

        int[] testSizes = {
            536_870_911,   // Integer.MAX_VALUE / 4
            1_000_000_000, // 1 billion
            1_500_000_000, // 1.5 billion
            1_610_612_734, // Just below threshold
            1_610_612_735, // At threshold
            1_610_612_736, // Just above threshold
            2_000_000_000, // 2 billion
            Integer.MAX_VALUE // Maximum
        };

        for (int size : testSizes) {
            int capacity = (int) method.invoke(null, size);

            // Verify capacity is always positive
            assertTrue(capacity > 0,
                "Capacity for size " + size + " should be positive, got: " + capacity);

            // Verify capacity doesn't exceed Integer.MAX_VALUE
            assertTrue(capacity <= Integer.MAX_VALUE,
                "Capacity for size " + size + " should not exceed Integer.MAX_VALUE");
        }
    }

    /**
     * Test that no ArithmeticException is thrown for any valid size.
     */
    @Test
    public void testNoArithmeticException() throws Exception {
        Method method = DeepEquals.class.getDeclaredMethod("safeHashMapCapacity", int.class);
        method.setAccessible(true);

        // Test a wide range of sizes - none should throw
        for (int size : new int[]{0, 1, 100, 1000, 1_000_000, 100_000_000,
                                   1_000_000_000, Integer.MAX_VALUE}) {
            try {
                int capacity = (int) method.invoke(null, size);
                assertTrue(capacity > 0, "Capacity should be positive for size " + size);
            } catch (Exception e) {
                fail("Should not throw exception for size " + size + ": " + e.getMessage());
            }
        }
    }
}
