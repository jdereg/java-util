package com.cedarsoftware.util;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Micro-benchmark to test the performance impact of the flattenKey() optimization.
 * This test specifically measures the performance of the most common case:
 * simple single object keys (String, Integer, etc.) that are not arrays or collections.
 */
public class MultiKeyMapFlattenKeyOptimizationTest {

    private static final Logger LOG = Logger.getLogger(MultiKeyMapFlattenKeyOptimizationTest.class.getName());
    
    private static final int WARMUP_ITERATIONS = 100_000;
    private static final int TEST_ITERATIONS = 1_000_000;
    
    @Test
    public void testSimpleKeyPerformance() {
        LOG.info("=== MultiKeyMap flattenKey() Optimization Test ===");
        LOG.info("Testing simple key performance (String keys)");
        
        // Create test map
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Prepare test data
        String[] keys = new String[1000];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = "TestKey_" + i;
        }
        
        // Populate map
        for (String key : keys) {
            map.put(key, "value_" + key);
        }
        
        // Warm up JVM
        LOG.info("Warming up JVM...");
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String key = keys[i % keys.length];
            map.get(key);
        }
        
        // Test GET performance
        LOG.info("Measuring GET performance with simple String keys...");
        long startTime = System.nanoTime();
        
        for (int i = 0; i < TEST_ITERATIONS; i++) {
            String key = keys[i % keys.length];
            map.get(key);
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        double avgTimeNanos = (double) totalTime / TEST_ITERATIONS;
        
        LOG.info(String.format("Total time: %,d ms", totalTime / 1_000_000));
        LOG.info(String.format("Average GET time: %.2f nanoseconds", avgTimeNanos));
        LOG.info(String.format("Throughput: %,.0f operations/second", 1_000_000_000.0 / avgTimeNanos));
        
        // Test PUT performance
        LOG.info("Measuring PUT performance with simple String keys...");
        MultiKeyMap<String> newMap = new MultiKeyMap<>();
        
        startTime = System.nanoTime();
        
        for (int i = 0; i < keys.length; i++) {
            newMap.put(keys[i], "value_" + keys[i]);
        }
        
        endTime = System.nanoTime();
        totalTime = endTime - startTime;
        avgTimeNanos = (double) totalTime / keys.length;
        
        LOG.info(String.format("Total time to PUT %d entries: %,d microseconds",
                         keys.length, totalTime / 1_000));
        LOG.info(String.format("Average PUT time: %.2f nanoseconds", avgTimeNanos));
        
        // Test with mixed key types (still simple, non-collection/array)
        LOG.info("=== Testing with mixed simple key types ===");
        MultiKeyMap<Object> mixedMap = new MultiKeyMap<>();
        
        // Add different types of simple keys
        mixedMap.put("string_key", "string_value");
        mixedMap.put(42, "integer_value");
        mixedMap.put(3.14159, "double_value");
        mixedMap.put(true, "boolean_value");
        mixedMap.put('A', "char_value");
        
        // Measure lookup performance for mixed types
        startTime = System.nanoTime();
        for (int i = 0; i < 100_000; i++) {
            mixedMap.get("string_key");
            mixedMap.get(42);
            mixedMap.get(3.14159);
            mixedMap.get(true);
            mixedMap.get('A');
        }
        endTime = System.nanoTime();
        
        totalTime = endTime - startTime;
        avgTimeNanos = (double) totalTime / (100_000 * 5);
        
        LOG.info(String.format("Average GET time for mixed simple types: %.2f nanoseconds", avgTimeNanos));

        LOG.info("=== Optimization Summary ===");
        LOG.info("The optimization reorders checks in flattenKey() to:");
        LOG.info("1. Check instanceof Collection first (faster than getClass().isArray())");
        LOG.info("2. For non-Collections, only then check isArray()");
        LOG.info("3. Return immediately for simple objects (most common case)");
        LOG.info("This avoids unnecessary getClass() calls for Collections");
        LOG.info("and provides fastest path for simple keys.");
    }
}