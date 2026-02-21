package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test to prove [crit-2]: Hash/Equals contract violation for floating-point numbers.
 *
 * The contract states: If deepEquals(a, b) returns true, then deepHashCode(a) must equal deepHashCode(b).
 * This test demonstrates that the contract is maintained for nearly-equal floating-point values.
 */
public class TestHashEqualsContractViolation {

    @Test
    public void testHashEqualsContractViolation_DirectValues() {
        // These two values are within epsilon tolerance (1e-10)
        double a = 1.0000000001;
        double b = 0.9999999999;

        // They should be considered equal by deepEquals (epsilon tolerance)
        boolean areEqual = DeepEquals.deepEquals(a, b);

        // Get their hash codes
        int hashA = DeepEquals.deepHashCode(a);
        int hashB = DeepEquals.deepHashCode(b);

        if (areEqual) {
            // CONTRACT: Equal objects must have equal hash codes
            assertEquals(hashA, hashB,
                "HASH/EQUALS CONTRACT VIOLATED: deepEquals(a,b)==true but deepHashCode(a)!=deepHashCode(b)");
        }
    }

    @Test
    public void testHashEqualsContractViolation_InCollections() {
        // Create two sets with nearly-equal doubles
        Set<Double> set1 = new HashSet<>();
        set1.add(1.0000000001);

        Set<Double> set2 = new HashSet<>();
        set2.add(0.9999999999);

        // The values are within epsilon, so sets should be equal
        boolean setsEqual = DeepEquals.deepEquals(set1, set2);

        // Get hash codes of the sets
        int hash1 = DeepEquals.deepHashCode(set1);
        int hash2 = DeepEquals.deepHashCode(set2);

        if (setsEqual) {
            // CONTRACT: Equal collections must have equal hash codes
            assertEquals(hash1, hash2,
                "HASH/EQUALS CONTRACT VIOLATED: deepEquals(set1,set2)==true but deepHashCode(set1)!=deepHashCode(set2)");
        }
    }

    @Test
    public void testHashEqualsContractViolation_UnorderedCollectionFailure() {
        // This is the REAL-WORLD IMPACT: unordered collection comparison

        // Create two lists with mathematically equivalent values
        List<Double> list1 = Arrays.asList(
            Math.log(Math.pow(Math.E, 2)),  // = 2.0 (mathematically)
            Math.tan(Math.PI / 4)            // = 1.0 (mathematically)
        );

        List<Double> list2 = Arrays.asList(2.0, 1.0);

        // Convert to Sets (unordered)
        Set<Double> set1 = new HashSet<>(list1);
        Set<Double> set2 = new HashSet<>(list2);

        // Check if individual elements are equal
        boolean elem0Equal = DeepEquals.deepEquals(list1.get(0), list2.get(0));
        boolean elem1Equal = DeepEquals.deepEquals(list1.get(1), list2.get(1));

        // Verify hash contract for individual elements
        if (elem0Equal) {
            int hash1_0 = DeepEquals.deepHashCode(list1.get(0));
            int hash2_0 = DeepEquals.deepHashCode(list2.get(0));
            assertEquals(hash1_0, hash2_0,
                "Element 0 hash contract violated: " + list1.get(0) + " vs " + list2.get(0));
        }

        if (elem1Equal) {
            int hash1_1 = DeepEquals.deepHashCode(list1.get(1));
            int hash2_1 = DeepEquals.deepHashCode(list2.get(1));
            assertEquals(hash1_1, hash2_1,
                "Element 1 hash contract violated: " + list1.get(1) + " vs " + list2.get(1));
        }

        // Now compare the sets
        boolean setsEqual = DeepEquals.deepEquals(set1, set2);
        int setHash1 = DeepEquals.deepHashCode(set1);
        int setHash2 = DeepEquals.deepHashCode(set2);

        // Since all elements are equal, sets should be equal
        if (elem0Equal && elem1Equal) {
            assertTrue(setsEqual,
                "BUG: Elements are deepEquals-equal but sets are not equal due to hash mismatch!");
        }

        if (setsEqual) {
            assertEquals(setHash1, setHash2,
                "Equal sets must have equal hash codes (set1=" + setHash1 + ", set2=" + setHash2 + ")");
        }
    }

    @Test
    public void testHashCodeContract_MultipleNearbyValues() {
        // Test the hash code contract with multiple values near each other
        double[] values = {
            0.9999999999,
            1.0,
            1.0000000001
        };

        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                double a = values[i];
                double b = values[j];

                boolean equal = DeepEquals.deepEquals(a, b);
                int hashA = DeepEquals.deepHashCode(a);
                int hashB = DeepEquals.deepHashCode(b);

                if (equal) {
                    assertEquals(hashA, hashB,
                        String.format("Contract violated for %.10f and %.10f", a, b));
                }
            }
        }
    }
}
