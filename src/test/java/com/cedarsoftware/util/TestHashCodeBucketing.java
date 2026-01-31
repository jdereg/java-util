package com.cedarsoftware.util;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

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
        
        System.out.println("=== Testing Hash Code Bucketing Around Epsilon Boundary ===");
        System.out.println("Base value: " + base);
        System.out.println("Epsilon: 1e-12");
        System.out.println();
        
        for (double val : testValues) {
            boolean isEqual = DeepEquals.deepEquals(val, base);
            int hashVal = DeepEquals.deepHashCode(val);
            int hashBase = DeepEquals.deepHashCode(base);
            
            System.out.printf("Value: %.15f (diff: %.2e)%n", val, Math.abs(val - base));
            System.out.println("  Equal to base: " + isEqual);
            System.out.println("  Hash: " + hashVal);
            System.out.println("  Base hash: " + hashBase);
            System.out.println("  Hashes match: " + (hashVal == hashBase));
            
            if (isEqual && hashVal != hashBase) {
                System.out.println("  *** BUG: Equal values have different hashes! ***");
            }
            System.out.println();
        }
    }

    @Test
    public void testUnorderedSetWithNearbyValues() {
        // Create sets with values that are equal but might hash differently
        double base = 100.0;
        double nearby = base + 1e-13;  // Within epsilon
        
        System.out.println("=== Unordered Set Test with Nearby Values ===");
        System.out.println("Base: " + base);
        System.out.println("Nearby: " + nearby);
        System.out.println("Difference: " + (nearby - base));
        System.out.println();
        
        // Check if they're equal
        boolean equal = DeepEquals.deepEquals(base, nearby);
        int hash1 = DeepEquals.deepHashCode(base);
        int hash2 = DeepEquals.deepHashCode(nearby);
        
        System.out.println("deepEquals: " + equal);
        System.out.println("Hash base: " + hash1);
        System.out.println("Hash nearby: " + hash2);
        System.out.println("Hashes match: " + (hash1 == hash2));
        System.out.println();
        
        if (equal) {
            // Now put them in sets and compare
            Set<Double> set1 = new HashSet<>();
            set1.add(base);
            
            Set<Double> set2 = new HashSet<>();
            set2.add(nearby);
            
            boolean setsEqual = DeepEquals.deepEquals(set1, set2);
            int setHash1 = DeepEquals.deepHashCode(set1);
            int setHash2 = DeepEquals.deepHashCode(set2);
            
            System.out.println("Set1: " + set1);
            System.out.println("Set2: " + set2);
            System.out.println("Sets equal: " + setsEqual);
            System.out.println("Set hash1: " + setHash1);
            System.out.println("Set hash2: " + setHash2);
            System.out.println();
            
            if (!setsEqual) {
                System.out.println("*** BUG FOUND: Values are equal but sets containing them are not! ***");
            }
            if (setHash1 != setHash2) {
                System.out.println("*** BUG FOUND: Equal sets have different hash codes! ***");
            }
        }
    }
}
