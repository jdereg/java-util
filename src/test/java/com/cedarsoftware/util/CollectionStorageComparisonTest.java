package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

/**
 * Simplified performance comparison between:
 * 1. Converting non-RandomAccess Collections to Object[] (current MultiKeyMap approach)
 * 2. Storing Collections as-is and using iterators
 * 3. Apache Commons approach (using standard HashMap with List keys)
 */
public class CollectionStorageComparisonTest {
    
    private static final int ITERATIONS = 1_000_000;
    private static final int KEY_SIZE = 5;
    private static final int WARMUP = 10_000;
    
    @Test
    void compareStorageStrategies() {
        System.out.println("\n=== Collection Storage Strategy Comparison ===\n");
        System.out.println("Test parameters:");
        System.out.println("  Iterations: " + ITERATIONS);
        System.out.println("  Key size: " + KEY_SIZE + " elements");
        System.out.println("  Collection type: LinkedList (non-RandomAccess)\n");
        
        // Create test data
        List<LinkedList<Integer>> linkedListKeys = createLinkedListKeys();
        List<Object[]> convertedArrayKeys = convertToArrays(linkedListKeys);
        
        // Test 1: Current MultiKeyMap approach (LinkedList converted to Object[])
        testMultiKeyMapCurrent(linkedListKeys);
        
        // Test 2: Simulated "as-is" storage with iterator comparison
        testAsIsStorageWithIterators(linkedListKeys);
        
        // Test 3: Direct array comparison (what MultiKeyMap does after conversion)
        testDirectArrayComparison(convertedArrayKeys);
        
        // Test 4: Standard HashMap with List keys (Apache-style)
        testStandardHashMap(linkedListKeys);
        
        // Test 5: ConcurrentHashMap with List keys
        testConcurrentHashMap(linkedListKeys);
    }
    
    private List<LinkedList<Integer>> createLinkedListKeys() {
        List<LinkedList<Integer>> keys = new ArrayList<>(ITERATIONS);
        Random rand = new Random(42);
        
        for (int i = 0; i < ITERATIONS; i++) {
            LinkedList<Integer> key = new LinkedList<>();
            for (int j = 0; j < KEY_SIZE; j++) {
                key.add(rand.nextInt(1000));
            }
            keys.add(key);
        }
        return keys;
    }
    
    private List<Object[]> convertToArrays(List<LinkedList<Integer>> lists) {
        List<Object[]> arrays = new ArrayList<>(lists.size());
        for (LinkedList<Integer> list : lists) {
            arrays.add(list.toArray());
        }
        return arrays;
    }
    
    private void testMultiKeyMapCurrent(List<LinkedList<Integer>> keys) {
        System.out.println("1. Current MultiKeyMap (converts LinkedList to Object[]):");
        
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Populate
        long start = System.nanoTime();
        for (int i = 0; i < keys.size(); i++) {
            map.put(keys.get(i), "value" + i);
        }
        long populateTime = System.nanoTime() - start;
        
        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            map.get(keys.get(i % keys.size()));
        }
        
        // Lookup
        start = System.nanoTime();
        int hits = 0;
        for (LinkedList<Integer> key : keys) {
            if (map.get(key) != null) hits++;
        }
        long lookupTime = System.nanoTime() - start;
        
        printResults(populateTime, lookupTime, hits);
    }
    
    private void testAsIsStorageWithIterators(List<LinkedList<Integer>> keys) {
        System.out.println("2. Simulated as-is storage (using iterators for comparison):");
        
        // Simulate storing Collections as-is and comparing with iterators
        Map<CollectionWrapper, String> map = new HashMap<>();
        
        // Populate
        long start = System.nanoTime();
        for (int i = 0; i < keys.size(); i++) {
            map.put(new CollectionWrapper(keys.get(i)), "value" + i);
        }
        long populateTime = System.nanoTime() - start;
        
        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            map.get(new CollectionWrapper(keys.get(i % keys.size())));
        }
        
        // Lookup
        start = System.nanoTime();
        int hits = 0;
        for (LinkedList<Integer> key : keys) {
            if (map.get(new CollectionWrapper(key)) != null) hits++;
        }
        long lookupTime = System.nanoTime() - start;
        
        printResults(populateTime, lookupTime, hits);
    }
    
    private void testDirectArrayComparison(List<Object[]> arrays) {
        System.out.println("3. Direct Object[] comparison (post-conversion):");
        
        Map<ArrayWrapper, String> map = new HashMap<>();
        
        // Populate
        long start = System.nanoTime();
        for (int i = 0; i < arrays.size(); i++) {
            map.put(new ArrayWrapper(arrays.get(i)), "value" + i);
        }
        long populateTime = System.nanoTime() - start;
        
        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            map.get(new ArrayWrapper(arrays.get(i % arrays.size())));
        }
        
        // Lookup
        start = System.nanoTime();
        int hits = 0;
        for (Object[] array : arrays) {
            if (map.get(new ArrayWrapper(array)) != null) hits++;
        }
        long lookupTime = System.nanoTime() - start;
        
        printResults(populateTime, lookupTime, hits);
    }
    
    private void testStandardHashMap(List<LinkedList<Integer>> keys) {
        System.out.println("4. Standard HashMap with List keys (Apache-style):");
        
        Map<List<Integer>, String> map = new HashMap<>();
        
        // Populate
        long start = System.nanoTime();
        for (int i = 0; i < keys.size(); i++) {
            map.put(new ArrayList<>(keys.get(i)), "value" + i);  // Copy to ArrayList for fair comparison
        }
        long populateTime = System.nanoTime() - start;
        
        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            map.get(new ArrayList<>(keys.get(i % keys.size())));
        }
        
        // Lookup
        start = System.nanoTime();
        int hits = 0;
        for (LinkedList<Integer> key : keys) {
            if (map.get(new ArrayList<>(key)) != null) hits++;
        }
        long lookupTime = System.nanoTime() - start;
        
        printResults(populateTime, lookupTime, hits);
    }
    
    private void testConcurrentHashMap(List<LinkedList<Integer>> keys) {
        System.out.println("5. ConcurrentHashMap with List keys:");
        
        Map<List<Integer>, String> map = new ConcurrentHashMap<>();
        
        // Populate
        long start = System.nanoTime();
        for (int i = 0; i < keys.size(); i++) {
            map.put(new ArrayList<>(keys.get(i)), "value" + i);
        }
        long populateTime = System.nanoTime() - start;
        
        // Warmup
        for (int i = 0; i < WARMUP; i++) {
            map.get(new ArrayList<>(keys.get(i % keys.size())));
        }
        
        // Lookup
        start = System.nanoTime();
        int hits = 0;
        for (LinkedList<Integer> key : keys) {
            if (map.get(new ArrayList<>(key)) != null) hits++;
        }
        long lookupTime = System.nanoTime() - start;
        
        printResults(populateTime, lookupTime, hits);
    }
    
    private void printResults(long populateNanos, long lookupNanos, int hits) {
        System.out.printf("  Populate: %,d ms%n", populateNanos / 1_000_000);
        System.out.printf("  Lookup: %,d hits in %,d ms (%.1f ns/lookup)%n",
            hits, lookupNanos / 1_000_000, (double) lookupNanos / ITERATIONS);
        System.out.printf("  Throughput: %,.0f lookups/second%n%n",
            ITERATIONS * 1_000_000_000.0 / lookupNanos);
    }
    
    // Wrapper that uses iterators for equality (simulates Collection stored as-is)
    private static class CollectionWrapper {
        private final Collection<?> coll;
        
        CollectionWrapper(Collection<?> coll) {
            this.coll = coll;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CollectionWrapper)) return false;
            CollectionWrapper other = (CollectionWrapper) obj;
            if (coll.size() != other.coll.size()) return false;
            
            // Use iterators for comparison (simulates non-RandomAccess comparison)
            Iterator<?> iter1 = coll.iterator();
            Iterator<?> iter2 = other.coll.iterator();
            while (iter1.hasNext()) {
                if (!Objects.equals(iter1.next(), iter2.next())) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public int hashCode() {
            int h = 1;
            for (Object o : coll) {
                h = h * 31 + (o == null ? 0 : o.hashCode());
            }
            return h;
        }
    }
    
    // Wrapper for Object[] with direct indexed access
    private static class ArrayWrapper {
        private final Object[] array;
        
        ArrayWrapper(Object[] array) {
            this.array = array;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ArrayWrapper)) return false;
            ArrayWrapper other = (ArrayWrapper) obj;
            if (array.length != other.array.length) return false;
            
            // Direct indexed access (what MultiKeyMap does after conversion)
            for (int i = 0; i < array.length; i++) {
                if (!Objects.equals(array[i], other.array[i])) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public int hashCode() {
            int h = 1;
            for (Object o : array) {
                h = h * 31 + (o == null ? 0 : o.hashCode());
            }
            return h;
        }
    }
}