package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Performance test to compare different Collection storage strategies:
 * 1. Current: Convert non-RandomAccess Collections to Object[]
 * 2. Modified: Store Collections as-is, use iterators
 * 3. Apache Commons: Baseline comparison (if available)
 * 
 * Focus: Test with LinkedList (non-RandomAccess) vs ArrayList (RandomAccess)
 * to measure the impact of iterator allocation vs direct array access.
 */
public class MultiKeyMapCollectionStoragePerformanceTest {
    
    private static final int WARMUP_ITERATIONS = 10_000;
    private static final int TEST_ITERATIONS = 1_000_000;
    private static final int KEY_SIZE = 5;  // 5-element keys
    
    private List<LinkedList<Integer>> linkedListKeys;
    private List<ArrayList<Integer>> arrayListKeys;
    private List<Object[]> objectArrayKeys;
    private List<int[]> primitiveArrayKeys;
    
    @BeforeEach
    void setUp() {
        // Pre-create all test keys to avoid allocation during timing
        linkedListKeys = new ArrayList<>(TEST_ITERATIONS);
        arrayListKeys = new ArrayList<>(TEST_ITERATIONS);
        objectArrayKeys = new ArrayList<>(TEST_ITERATIONS);
        primitiveArrayKeys = new ArrayList<>(TEST_ITERATIONS);
        
        Random rand = new Random(42); // Fixed seed for reproducibility
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            LinkedList<Integer> llKey = new LinkedList<>();
            ArrayList<Integer> alKey = new ArrayList<>(KEY_SIZE);
            Object[] oaKey = new Object[KEY_SIZE];
            int[] paKey = new int[KEY_SIZE];
            
            for (int j = 0; j < KEY_SIZE; j++) {
                int value = rand.nextInt(1000);
                llKey.add(value);
                alKey.add(value);
                oaKey[j] = value;
                paKey[j] = value;
            }
            
            linkedListKeys.add(llKey);
            arrayListKeys.add(alKey);
            objectArrayKeys.add(oaKey);
            primitiveArrayKeys.add(paKey);
        }
    }
    
    @Test
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    void testCurrentImplementation() {
        System.out.println("\n=== CURRENT IMPLEMENTATION (Converts non-RandomAccess to Object[]) ===\n");
        
        // Test with LinkedList keys (non-RandomAccess - gets converted to Object[])
        MultiKeyMap<String> mapLL = new MultiKeyMap<>();
        testPerformance("LinkedList keys (converted to Object[])", mapLL, linkedListKeys);
        
        // Test with ArrayList keys (RandomAccess - stays as-is)
        MultiKeyMap<String> mapAL = new MultiKeyMap<>();
        testPerformance("ArrayList keys (stays as Collection)", mapAL, arrayListKeys);
        
        // Test with Object[] keys (baseline - no conversion)
        MultiKeyMap<String> mapOA = new MultiKeyMap<>();
        testPerformance("Object[] keys (no conversion)", mapOA, objectArrayKeys);
        
        // Test with primitive array keys (best case - no boxing)
        MultiKeyMap<String> mapPA = new MultiKeyMap<>();
        testPerformance("int[] keys (primitive, no boxing)", mapPA, primitiveArrayKeys);
        
        // Mixed scenario: Store with LinkedList, lookup with ArrayList
        testMixedScenario();
    }
    
    private void testPerformance(String description, MultiKeyMap<String> map, List<?> keys) {
        System.out.println(description + ":");
        
        // Populate map
        long populateStart = System.nanoTime();
        for (int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), "value" + i);
        }
        long populateTime = System.nanoTime() - populateStart;
        System.out.printf("  Populate: %,d entries in %,d ms%n", 
            keys.size(), TimeUnit.NANOSECONDS.toMillis(populateTime));
        
        // Warmup
        for (int w = 0; w < WARMUP_ITERATIONS; w++) {
            map.get(keys.get(w % keys.size()));
        }
        
        // Measure lookups
        long lookupStart = System.nanoTime();
        int hits = 0;
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            if (map.get(keys.get(i)) != null) {
                hits++;
            }
        }
        long lookupTime = System.nanoTime() - lookupStart;
        
        double avgLookupNs = (double) lookupTime / TEST_ITERATIONS;
        System.out.printf("  Lookup: %,d hits in %,d ms (%.1f ns/lookup)%n", 
            hits, TimeUnit.NANOSECONDS.toMillis(lookupTime), avgLookupNs);
        System.out.printf("  Throughput: %,.0f lookups/second%n%n",
            1_000_000_000.0 / avgLookupNs);
    }
    
    private void testMixedScenario() {
        System.out.println("Mixed scenario (store LinkedList, lookup with ArrayList):");
        
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Store with LinkedList keys
        long storeStart = System.nanoTime();
        for (int i = 0; i < linkedListKeys.size(); i++) {
            map.put(linkedListKeys.get(i), "value" + i);
        }
        long storeTime = System.nanoTime() - storeStart;
        System.out.printf("  Store with LinkedList: %,d ms%n", 
            TimeUnit.NANOSECONDS.toMillis(storeTime));
        
        // Lookup with ArrayList keys (same values, different container type)
        long lookupStart = System.nanoTime();
        int hits = 0;
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            if (map.get(arrayListKeys.get(i)) != null) {
                hits++;
            }
        }
        long lookupTime = System.nanoTime() - lookupStart;
        
        System.out.printf("  Lookup with ArrayList: %,d hits in %,d ms%n", 
            hits, TimeUnit.NANOSECONDS.toMillis(lookupTime));
        System.out.printf("  Cross-container lookup rate: %,.0f lookups/second%n%n",
            (double) TEST_ITERATIONS * 1_000_000_000 / lookupTime);
    }
    
    // TODO: Add test for modified implementation (Collections stored as-is)
    // This will require a modified version of MultiKeyMap or a flag to control behavior
    
    // TODO: Add Apache Commons Collections comparison if available
}