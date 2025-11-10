package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to prove [crit-2]: Hash/Equals contract violation for floating-point numbers.
 *
 * The contract states: If deepEquals(a, b) returns true, then deepHashCode(a) must equal deepHashCode(b).
 * This test demonstrates that the contract is violated for nearly-equal floating-point values.
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

        System.out.println("=== Direct Value Test ===");
        System.out.println("Value A: " + a);
        System.out.println("Value B: " + b);
        System.out.println("deepEquals(a, b): " + areEqual);
        System.out.println("deepHashCode(a): " + hashA);
        System.out.println("deepHashCode(b): " + hashB);
        System.out.println("Hash codes equal: " + (hashA == hashB));
        System.out.println();

        if (areEqual) {
            // CONTRACT VIOLATION: Objects are equal but have different hash codes
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

        System.out.println("=== Collection Test ===");
        System.out.println("Set 1: " + set1);
        System.out.println("Set 2: " + set2);
        System.out.println("deepEquals(set1, set2): " + setsEqual);
        System.out.println("deepHashCode(set1): " + hash1);
        System.out.println("deepHashCode(set2): " + hash2);
        System.out.println("Hash codes equal: " + (hash1 == hash2));
        System.out.println();

        if (setsEqual) {
            // CONTRACT VIOLATION in collections
            assertEquals(hash1, hash2,
                "HASH/EQUALS CONTRACT VIOLATED: deepEquals(set1,set2)==true but deepHashCode(set1)!=deepHashCode(set2)");
        }
    }

    @Test
    public void testHashEqualsContractViolation_UnorderedCollectionFailure() {
        // This is the REAL-WORLD IMPACT: unordered collection comparison fails

        // Create two lists with mathematically equivalent values
        List<Double> list1 = Arrays.asList(
            Math.log(Math.pow(Math.E, 2)),  // = 2.0 (mathematically)
            Math.tan(Math.PI / 4)            // = 1.0 (mathematically)
        );

        List<Double> list2 = Arrays.asList(2.0, 1.0);

        System.out.println("=== Unordered Collection Impact Test ===");
        System.out.println("List 1 (computed): " + list1);
        System.out.println("List 2 (exact):    " + list2);
        System.out.println("list1[0] = " + list1.get(0) + " (should be ~2.0)");
        System.out.println("list1[1] = " + list1.get(1) + " (should be ~1.0)");
        System.out.println();

        // Convert to Sets (unordered)
        Set<Double> set1 = new HashSet<>(list1);
        Set<Double> set2 = new HashSet<>(list2);

        // Check if individual elements are equal
        boolean elem0Equal = DeepEquals.deepEquals(list1.get(0), list2.get(0));
        boolean elem1Equal = DeepEquals.deepEquals(list1.get(1), list2.get(1));

        System.out.println("Element 0 equal: " + elem0Equal + " (" + list1.get(0) + " vs " + list2.get(0) + ")");
        System.out.println("Element 1 equal: " + elem1Equal + " (" + list1.get(1) + " vs " + list2.get(1) + ")");

        // Get hash codes for each element
        int hash1_0 = DeepEquals.deepHashCode(list1.get(0));
        int hash2_0 = DeepEquals.deepHashCode(list2.get(0));
        int hash1_1 = DeepEquals.deepHashCode(list1.get(1));
        int hash2_1 = DeepEquals.deepHashCode(list2.get(1));

        System.out.println();
        System.out.println("Hash codes for element 0:");
        System.out.println("  list1[0]: " + hash1_0);
        System.out.println("  list2[0]: " + hash2_0);
        System.out.println("  Equal: " + (hash1_0 == hash2_0));

        System.out.println("Hash codes for element 1:");
        System.out.println("  list1[1]: " + hash1_1);
        System.out.println("  list2[1]: " + hash2_1);
        System.out.println("  Equal: " + (hash1_1 == hash2_1));
        System.out.println();

        // Now compare the sets
        boolean setsEqual = DeepEquals.deepEquals(set1, set2);
        int setHash1 = DeepEquals.deepHashCode(set1);
        int setHash2 = DeepEquals.deepHashCode(set2);

        System.out.println("Set comparison:");
        System.out.println("  deepEquals(set1, set2): " + setsEqual);
        System.out.println("  deepHashCode(set1): " + setHash1);
        System.out.println("  deepHashCode(set2): " + setHash2);
        System.out.println("  Hashes equal: " + (setHash1 == setHash2));
        System.out.println();

        // THE PROBLEM: Elements are equal, but due to hash mismatch,
        // unordered collection comparison may fail
        if (elem0Equal && elem1Equal) {
            System.out.println("EXPECTED: Since all elements are equal, sets should be equal");
            System.out.println("ACTUAL:   Sets equal = " + setsEqual);

            if (!setsEqual) {
                System.out.println("*** BUG CONFIRMED: Elements are equal but sets are not equal! ***");
                System.out.println("*** This is because hash codes don't match, causing bucket lookup failures ***");
            }

            // This assertion will likely FAIL, proving the bug
            assertTrue(setsEqual,
                "BUG: Elements are deepEquals-equal but sets are not equal due to hash mismatch!");
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

        System.out.println("=== Multiple Nearby Values Test ===");
        System.out.println("Testing hash code contract for values near 1.0:");
        System.out.println();

        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                double a = values[i];
                double b = values[j];

                boolean equal = DeepEquals.deepEquals(a, b);
                int hashA = DeepEquals.deepHashCode(a);
                int hashB = DeepEquals.deepHashCode(b);
                boolean hashesEqual = (hashA == hashB);

                System.out.printf("Comparing %.10f vs %.10f%n", a, b);
                System.out.println("  deepEquals: " + equal);
                System.out.println("  hashCode A: " + hashA);
                System.out.println("  hashCode B: " + hashB);
                System.out.println("  Hashes equal: " + hashesEqual);

                if (equal && !hashesEqual) {
                    System.out.println("  *** CONTRACT VIOLATION ***");
                }
                System.out.println();

                if (equal) {
                    assertEquals(hashA, hashB,
                        String.format("Contract violated for %.10f and %.10f", a, b));
                }
            }
        }
    }
}
