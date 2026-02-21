package com.cedarsoftware.util;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestHashCodeBucketing {

    @Test
    public void testValuesNearBoundary() {
        // Test values that are JUST within and JUST outside epsilon tolerance
        // Epsilon is 1e-12, so let's test around that boundary

        double base = 1.0;
        double[] testValues = {
            base - 5e-13,  // Within epsilon (should be equal to base)
            base - 1e-12,  // At epsilon boundary
            base - 2e-12,  // Outside epsilon (should NOT be equal)
            base,
            base + 5e-13,  // Within epsilon
            base + 1e-12,  // At epsilon boundary
            base + 2e-12   // Outside epsilon
        };

        int hashBase = DeepEquals.deepHashCode(base);

        for (double val : testValues) {
            boolean isEqual = DeepEquals.deepEquals(val, base);
            int hashVal = DeepEquals.deepHashCode(val);

            // Hash/equals contract: equal objects MUST have equal hash codes
            if (isEqual) {
                assertEquals(hashBase, hashVal,
                    "CONTRACT VIOLATION: Value " + val + " is equal to base " + base +
                    " but has different hash code (val=" + hashVal + ", base=" + hashBase + ")");
            }
        }
    }

    @Test
    public void testUnorderedSetWithNearbyValues() {
        // Create sets with values that are equal but might hash differently
        double base = 100.0;
        double nearby = base + 1e-13;  // Within epsilon

        // Check if they're equal
        boolean equal = DeepEquals.deepEquals(base, nearby);
        int hash1 = DeepEquals.deepHashCode(base);
        int hash2 = DeepEquals.deepHashCode(nearby);

        assertTrue(equal, "Values within epsilon should be considered equal");
        assertEquals(hash1, hash2,
            "Equal values must have equal hash codes (base hash=" + hash1 + ", nearby hash=" + hash2 + ")");

        // Now put them in sets and compare
        Set<Double> set1 = new HashSet<>();
        set1.add(base);

        Set<Double> set2 = new HashSet<>();
        set2.add(nearby);

        boolean setsEqual = DeepEquals.deepEquals(set1, set2);
        int setHash1 = DeepEquals.deepHashCode(set1);
        int setHash2 = DeepEquals.deepHashCode(set2);

        assertTrue(setsEqual, "Sets containing equal values should be equal");
        assertEquals(setHash1, setHash2,
            "Equal sets must have equal hash codes (set1 hash=" + setHash1 + ", set2 hash=" + setHash2 + ")");
    }
}
