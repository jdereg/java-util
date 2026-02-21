package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Test to measure how often cross-container comparisons actually occur in typical MultiKeyMap usage.
 * This helps determine if the optimization is worth the code complexity.
 */
public class MultiKeyMapCrossContainerFrequencyTest {

    private static final Logger LOG = Logger.getLogger(MultiKeyMapCrossContainerFrequencyTest.class.getName());
    
    @Test
    void measureCrossContainerFrequency() {
        LOG.info("=== Cross-Container Comparison Frequency Test ===");
        LOG.info("Testing how often Object[] vs Collection comparisons occur in practice");
        
        MultiKeyMap<String> map = new MultiKeyMap<>();
        int operations = 10000;
        
        // Scenario 1: Consistent usage (always arrays or always lists)
        LOG.info("--- Scenario 1: Consistent Usage (all arrays) ---");
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
        LOG.info(String.format("  Same container type queries: %d hits, %d misses", hits, misses));
        LOG.info("  Cross-container comparisons: 0 (0%)");
        
        // Scenario 2: Mixed usage (common in real applications)
        LOG.info("--- Scenario 2: Mixed Usage (put with arrays, get with lists) ---");
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
        LOG.info(String.format("  Cross-container type queries: %d hits, %d misses", hits, misses));
        LOG.info(String.format("  Cross-container comparisons: %d (100%%)", operations));
        
        // Scenario 3: Real-world pattern (builder methods vs direct arrays)
        LOG.info("--- Scenario 3: Real-world Pattern (mixed puts and gets) ---");
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
        
        LOG.info(String.format("  Array queries: %d", arrayQueries));
        LOG.info(String.format("  List queries: %d (these cause cross-container comparisons)", listQueries));
        LOG.info(String.format("  Vararg queries: %d", varargQueries));
        LOG.info(String.format("  Estimated cross-container comparison rate: ~%.1f%%",
                         (listQueries * 100.0) / (arrayQueries + listQueries + varargQueries)));
        
        // Scenario 4: Performance impact measurement
        LOG.info("--- Scenario 4: Performance Impact ---");
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
        
        LOG.info(String.format("  Same-type access: %.2f ns/op", samensPerOp));
        LOG.info(String.format("  Cross-type access: %.2f ns/op", crossNsPerOp));
        LOG.info(String.format("  Cross-type overhead: %.1f%%", overhead));

        LOG.info("=== Conclusion ===");
        LOG.info("Cross-container comparisons occur when:");
        LOG.info("  1. Put with Object[], get with List (or vice versa)");
        LOG.info("  2. Keys come from different sources (arrays vs collections)");
        LOG.info("  3. APIs mix array and collection usage");
        LOG.info("In real applications, this can be 0-100% of operations depending on usage patterns.");
    }
}