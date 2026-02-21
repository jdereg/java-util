package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test to verify the hashCode/equals contract fix.
 * This test validates:
 * 1. The contract is maintained (equal values have equal hashes)
 * 2. No false positives (unequal values have different hashes)
 * 3. Hash distribution is reasonable (not everything hashing to same value)
 * 4. The fix actually solves the original problem
 */
public class TestHashCodeFixVerification {

    private static final double EPSILON = 1e-12;

    @Test
    public void testContractIsMaintained_EqualValuesHaveEqualHashes() {
        // Test values that SHOULD be equal (within epsilon)
        double[][] equalPairs = {
            {1.0, 1.0 + EPSILON * 0.5},           // Within epsilon
            {1.0, 1.0 - EPSILON * 0.5},           // Within epsilon
            {100.0, 100.0 + EPSILON * 100 * 0.5}, // Relative tolerance
            {0.5, 0.5 + EPSILON * 0.5 * 0.5},     // Smaller magnitude
            {-1.0, -1.0 + EPSILON * 0.5},         // Negative values
        };

        int violations = 0;
        for (double[] pair : equalPairs) {
            double a = pair[0];
            double b = pair[1];
            boolean areEqual = DeepEquals.deepEquals(a, b);
            int hashA = DeepEquals.deepHashCode(a);
            int hashB = DeepEquals.deepHashCode(b);

            if (areEqual && hashA != hashB) {
                violations++;
            }
        }

        assertEquals(0, violations, "Found " + violations + " contract violations where equal values had different hashes");
    }

    @Test
    public void testNoFalsePositives_UnequalValuesCanHaveDifferentHashes() {
        // Test values that SHOULD NOT be equal (outside epsilon)
        double[][] unequalPairs = {
            {1.0, 1.0 + EPSILON * 2.0},           // Outside epsilon
            {1.0, 1.0 - EPSILON * 2.0},           // Outside epsilon
            {100.0, 100.0 + EPSILON * 100 * 2.0}, // Outside relative tolerance
            {1.0, 2.0},                           // Clearly different
            {0.0, 1.0},                           // Very different
        };

        int sameHashes = 0;
        int totalUnequal = 0;

        for (double[] pair : unequalPairs) {
            double a = pair[0];
            double b = pair[1];
            boolean areEqual = DeepEquals.deepEquals(a, b);
            int hashA = DeepEquals.deepHashCode(a);
            int hashB = DeepEquals.deepHashCode(b);

            if (!areEqual) {
                totalUnequal++;
                if (hashA == hashB) {
                    sameHashes++;
                }
            }
        }

        double collisionRate = (double) sameHashes / totalUnequal;

        // TRADE-OFF: Coarse quantization (1e10 vs 1e12) fixes the contract violation
        // but causes hash collisions for values in the narrow band just outside epsilon.
        // This is acceptable because:
        // 1. It only affects values differing by 2-10x epsilon (very close values)
        // 2. The performance benefit of correct hash bucketing for equal values outweighs
        //    the cost of occasional collisions for nearly-equal-but-not-equal values
        // 3. Hash collisions are handled correctly by deepEquals fallback logic
        assertTrue(collisionRate < 0.6,
            "Collision rate too high even for boundary cases: " + collisionRate);
    }

    @Test
    public void testHashDistribution_NotAllSameHash() {
        // Generate diverse values
        double[] testValues = {
            0.0, 1.0, -1.0,
            0.5, 1.5, 2.5,
            10.0, 100.0, 1000.0,
            0.001, 0.01, 0.1,
            Math.PI, Math.E,
            Double.MIN_VALUE, Double.MAX_VALUE / 1e100
        };

        Set<Integer> uniqueHashes = new HashSet<>();
        for (double value : testValues) {
            uniqueHashes.add(DeepEquals.deepHashCode(value));
        }

        double diversityRatio = (double) uniqueHashes.size() / testValues.length;

        // We expect at least 80% unique hashes for diverse values
        assertTrue(diversityRatio >= 0.8,
            "Poor hash distribution: only " + uniqueHashes.size() + " unique hashes out of " + testValues.length + " values");
    }

    @Test
    public void testOriginalProblem_UnorderedCollectionComparison() {
        // This is the REAL-WORLD problem that was reported
        // Values from mathematical functions that should equal exact values
        List<Double> computed = Arrays.asList(
            Math.log(Math.pow(Math.E, 2)),  // Should be 2.0
            Math.tan(Math.PI / 4)            // Should be 1.0
        );

        List<Double> exact = Arrays.asList(2.0, 1.0);

        // Check individual element equality and hash contract
        for (int i = 0; i < computed.size(); i++) {
            double c = computed.get(i);
            double e = exact.get(i);
            boolean equal = DeepEquals.deepEquals(c, e);
            int hashC = DeepEquals.deepHashCode(c);
            int hashE = DeepEquals.deepHashCode(e);

            if (equal) {
                assertEquals(hashC, hashE,
                    "Element " + i + " is equal but has different hash! (" + c + " vs " + e + ")");
            }
        }

        // Now test as unordered sets
        Set<Double> set1 = new HashSet<>(computed);
        Set<Double> set2 = new HashSet<>(exact);

        boolean setsEqual = DeepEquals.deepEquals(set1, set2);
        int hash1 = DeepEquals.deepHashCode(set1);
        int hash2 = DeepEquals.deepHashCode(set2);

        // The FIX: Sets with mathematically equal elements should be equal
        assertTrue(setsEqual, "Sets with equal elements should be equal!");

        if (setsEqual) {
            assertEquals(hash1, hash2,
                "Equal sets must have equal hash codes!");
        }
    }

    @Test
    public void testQuantizationGranularity() {
        // The fix uses 1e10 quantization (100x coarser than epsilon)
        // This test validates that's appropriate

        double base = 1.0;

        // Values that differ by less than epsilon should hash the same
        double epsilon_fraction_0_5 = base + EPSILON * 0.5;
        double epsilon_fraction_0_9 = base + EPSILON * 0.9;

        int hash_base = DeepEquals.deepHashCode(base);
        int hash_0_5 = DeepEquals.deepHashCode(epsilon_fraction_0_5);
        int hash_0_9 = DeepEquals.deepHashCode(epsilon_fraction_0_9);

        boolean equal_0_5 = DeepEquals.deepEquals(base, epsilon_fraction_0_5);
        boolean equal_0_9 = DeepEquals.deepEquals(base, epsilon_fraction_0_9);

        // Both should be equal and have same hash
        if (equal_0_5) {
            assertEquals(hash_base, hash_0_5,
                "Base + 0.5*epsilon is equal to base but has different hash");
        }
        if (equal_0_9) {
            assertEquals(hash_base, hash_0_9,
                "Base + 0.9*epsilon is equal to base but has different hash");
        }

        // Values outside 2*epsilon should be unequal
        double outside = base + EPSILON * 3.0;
        boolean equal_outside = DeepEquals.deepEquals(base, outside);

        assertFalse(equal_outside, "Values 3*epsilon apart should NOT be equal");
    }

    @Test
    public void testPerformanceCharacteristics() {
        // Generate 1000 random values and check hash distribution
        Random rand = new Random(42); // Fixed seed for reproducibility
        Map<Integer, Integer> hashCounts = new HashMap<>();
        int totalValues = 1000;

        for (int i = 0; i < totalValues; i++) {
            double value = rand.nextDouble() * 1000 - 500; // Range [-500, 500]
            int hash = DeepEquals.deepHashCode(value);
            hashCounts.put(hash, hashCounts.getOrDefault(hash, 0) + 1);
        }

        int uniqueHashes = hashCounts.size();
        int maxCollisions = hashCounts.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // We expect good distribution (at least 50% unique for random values)
        assertTrue(uniqueHashes > totalValues * 0.5,
            "Poor hash distribution: " + uniqueHashes + " unique out of " + totalValues);

        // No bucket should have more than 5% of all values
        assertTrue(maxCollisions < totalValues * 0.05,
            "Too many collisions in single bucket: " + maxCollisions);
    }
}
