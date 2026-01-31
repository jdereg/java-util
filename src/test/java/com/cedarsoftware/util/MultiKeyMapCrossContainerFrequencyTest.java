package com.cedarsoftware.util;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Test to measure how often cross-container comparisons actually occur in typical MultiKeyMap usage.
 * This helps determine if the optimization is worth the code complexity.
 */
public class MultiKeyMapCrossContainerFrequencyTest {
    
    @Test
    void measureCrossContainerFrequency() {
        System.out.println("\n=== Cross-Container Comparison Frequency Test ===\n");
        System.out.println("Testing how often Object[] vs Collection comparisons occur in practice");
        
        MultiKeyMap<String> map = new MultiKeyMap<>();
        int operations = 10000;
        
        // Scenario 1: Consistent usage (always arrays or always lists)
        System.out.println("\n--- Scenario 1: Consistent Usage (all arrays) ---");
        map.clear();
        for (int i = 0; i < operations; i++) {
            map.put(new Object[]{"key1", "key2", i}, "value" + i);
        }
        
        int hits = 0;
        int misses = 0;
        for (int i = 0; i < operations; i++) {
            // Query with same type (array)
            if (map.get(new Object[]{"key1", "key2", i}) != null) hits++;
            else misses++;
        }
        System.out.printf("  Same container type queries: %d hits, %d misses\n", hits, misses);
        System.out.println("  Cross-container comparisons: 0 (0%)");
        
        // Scenario 2: Mixed usage (common in real applications)
        System.out.println("\n--- Scenario 2: Mixed Usage (put with arrays, get with lists) ---");
        map.clear();
        for (int i = 0; i < operations; i++) {
            map.put(new Object[]{"key1", "key2", i}, "value" + i);
        }
        
        hits = 0;
        misses = 0;
        for (int i = 0; i < operations; i++) {
            // Query with different type (list)
            if (map.get(Arrays.asList("key1", "key2", i)) != null) hits++;
            else misses++;
        }
        System.out.printf("  Cross-container type queries: %d hits, %d misses\n", hits, misses);
        System.out.printf("  Cross-container comparisons: %d (100%%)\n", operations);
        
        // Scenario 3: Real-world pattern (builder methods vs direct arrays)
        System.out.println("\n--- Scenario 3: Real-world Pattern (mixed puts and gets) ---");
        map.clear();
        
        // Some users use arrays
        for (int i = 0; i < operations / 2; i++) {
            map.put(new String[]{"user", "config", "item" + i}, "arrayValue" + i);
        }
        
        // Some users use varargs (which become arrays internally)
        for (int i = operations / 2; i < operations; i++) {
            map.putMultiKey("varargsValue" + i, "user", "config", "item" + i);
        }
        
        // Queries might come from different sources
        int arrayQueries = 0;
        int listQueries = 0;
        int varargQueries = 0;
        
        // Some queries use arrays
        for (int i = 0; i < operations / 3; i++) {
            if (map.get(new String[]{"user", "config", "item" + i}) != null) arrayQueries++;
        }
        
        // Some queries use lists (common when keys come from parsed data)
        for (int i = operations / 3; i < 2 * operations / 3; i++) {
            if (map.get(Arrays.asList("user", "config", "item" + i)) != null) listQueries++;
        }
        
        // Some queries use varargs
        for (int i = 2 * operations / 3; i < operations; i++) {
            if (map.getMultiKey("user", "config", "item" + i) != null) varargQueries++;
        }
        
        System.out.printf("  Array queries: %d\n", arrayQueries);
        System.out.printf("  List queries: %d (these cause cross-container comparisons)\n", listQueries);
        System.out.printf("  Vararg queries: %d\n", varargQueries);
        System.out.printf("  Estimated cross-container comparison rate: ~%.1f%%\n", 
                         (listQueries * 100.0) / (arrayQueries + listQueries + varargQueries));
        
        // Scenario 4: Performance impact measurement
        System.out.println("\n--- Scenario 4: Performance Impact ---");
        map.clear();
        
        // Fill map with array keys
        for (int i = 0; i < 1000; i++) {
            map.put(new Object[]{"key1", "key2", "key3", i}, "value" + i);
        }
        
        int iterations = 100000;
        
        // Measure same-type access (array to array)
        long startSame = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            map.get(new Object[]{"key1", "key2", "key3", iter % 1000});
        }
        long timeSame = System.nanoTime() - startSame;
        
        // Measure cross-type access (array to list)
        long startCross = System.nanoTime();
        for (int iter = 0; iter < iterations; iter++) {
            map.get(Arrays.asList("key1", "key2", "key3", iter % 1000));
        }
        long timeCross = System.nanoTime() - startCross;
        
        double samensPerOp = (double) timeSame / iterations;
        double crossNsPerOp = (double) timeCross / iterations;
        double overhead = ((crossNsPerOp - samensPerOp) / samensPerOp) * 100;
        
        System.out.printf("  Same-type access: %.2f ns/op\n", samensPerOp);
        System.out.printf("  Cross-type access: %.2f ns/op\n", crossNsPerOp);
        System.out.printf("  Cross-type overhead: %.1f%%\n", overhead);
        
        System.out.println("\n=== Conclusion ===");
        System.out.println("Cross-container comparisons occur when:");
        System.out.println("  1. Put with Object[], get with List (or vice versa)");
        System.out.println("  2. Keys come from different sources (arrays vs collections)");
        System.out.println("  3. APIs mix array and collection usage");
        System.out.println("\nIn real applications, this can be 0-100% of operations depending on usage patterns.");
    }
}