package com.cedarsoftware.util;

import org.apache.commons.collections4.map.MultiKeyMap;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Performance comparison between Cedar's MultiKeyMap and Apache Commons Collections' MultiKeyMap.
 * Tests various key counts (1-8) and data sizes (100-250,000).
 * 
 * This test ensures fair comparison by:
 * 1. Warming up the JIT compiler
 * 2. Running tests in randomized order
 * 3. Using identical key sets for both implementations
 * 4. Measuring both put and get operations
 * 5. Running multiple iterations and averaging results
 */
public class MultiKeyMapPerformanceComparisonTest {
    
    private static final Logger LOG = Logger.getLogger(MultiKeyMapPerformanceComparisonTest.class.getName());
    
    private static final int WARMUP_ITERATIONS = 50;
    private static final int TEST_ITERATIONS = 10;
    private static final Random random = new Random(42); // Fixed seed for reproducibility
    
    // Test configurations
    private static final int[] KEY_COUNTS = {1, 2, 3, 4, 5, 6};
    private static final int[] DATA_SIZES = {100, 1000, 10000, 25000, 50000, 100000, 250000};
    
    private static class TestConfig {
        final int keyCount;
        final int dataSize;
        final String name;
        
        TestConfig(int keyCount, int dataSize) {
            this.keyCount = keyCount;
            this.dataSize = dataSize;
            this.name = keyCount + " keys, " + String.format("%,d", dataSize) + " entries";
        }
    }
    
    private static class TestResult {
        final String implementation;
        final long putNanos;
        final long getNanos;
        final int operations;
        
        TestResult(String implementation, long putNanos, long getNanos, int operations) {
            this.implementation = implementation;
            this.putNanos = putNanos;
            this.getNanos = getNanos;
            this.operations = operations;
        }
        
        double putOpsPerMs() {
            return (operations * 1000000.0) / putNanos;
        }
        
        double getOpsPerMs() {
            return (operations * 1000000.0) / getNanos;
        }
        
        double avgPutNanos() {
            return (double) putNanos / operations;
        }
        
        double avgGetNanos() {
            return (double) getNanos / operations;
        }
    }

    @Disabled
    @Test
    void comparePerformance() {
        LOG.info("=== Cedar vs Apache MultiKeyMap Performance Comparison ===");
        LOG.info("Warming up JIT compiler...");
        
        // Warm up JIT
        warmupJIT();
        
        LOG.info("JIT warmup complete. Starting performance tests...\n");
        
        // Create all test configurations
        List<TestConfig> configs = new ArrayList<>();
        for (int keyCount : KEY_COUNTS) {
            for (int dataSize : DATA_SIZES) {
                configs.add(new TestConfig(keyCount, dataSize));
            }
        }
        
        // Shuffle to avoid order bias
        Collections.shuffle(configs, random);
        
        // Run tests and collect results
        LOG.info("Running " + configs.size() + " test configurations...\n");
        LOG.info(String.format("%-30s | %-12s | %15s | %15s | %15s | %15s | %10s",
                "Configuration", "Implementation", "Put (ops/ms)", "Get (ops/ms)", 
                "Avg Put (ns)", "Avg Get (ns)", "Winner"));
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < 145; i++) separator.append("-");
        LOG.info(separator.toString());
        
        for (TestConfig config : configs) {
            runComparison(config);
        }
    }
    
    private void warmupJIT() {
        // Warm up both implementations with various key counts
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            for (int keyCount : KEY_COUNTS) {
                // Cedar warmup - no defensive copies for maximum performance
                com.cedarsoftware.util.MultiKeyMap<String> cedarMap = com.cedarsoftware.util.MultiKeyMap.<String>builder()
                    .simpleKeysMode(true)
                    .build();
                Object[][] keys = generateKeys(1000, keyCount);
                for (Object[] key : keys) {
                    if (keyCount == 1) {
                        cedarMap.put(key[0], "value");
                        cedarMap.get(key[0]);
                    } else {
                        cedarMap.putMultiKey("value", key);
                        cedarMap.getMultiKey(key);
                    }
                }
                
                // Apache warmup
                MultiKeyMap<Object, String> apacheMap = new MultiKeyMap<>();
                for (Object[] key : keys) {
                    if (keyCount == 1) {
                        // For single key, create a MultiKey with one element array
                        apacheMap.put(new MultiKey<Object>(new Object[]{key[0]}), "value");
                        apacheMap.get(new MultiKey<Object>(new Object[]{key[0]}));
                    } else {
                        apacheMap.put(new MultiKey<Object>(key), "value");
                        apacheMap.get(new MultiKey<Object>(key));
                    }
                }
            }
        }
    }
    
    private void runComparison(TestConfig config) {
        // Generate test data once for both implementations
        Object[][] keys = generateKeys(config.dataSize, config.keyCount);
        String[] values = generateValues(config.dataSize);
        
        // Randomize test order
        boolean runCedarFirst = random.nextBoolean();
        
        TestResult cedarResult;
        TestResult apacheResult;
        
        if (runCedarFirst) {
            cedarResult = testCedar(keys, values, config);
            // Small pause to let GC settle
            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            apacheResult = testApache(keys, values, config);
        } else {
            apacheResult = testApache(keys, values, config);
            // Small pause to let GC settle
            System.gc();
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            cedarResult = testCedar(keys, values, config);
        }
        
        // Print results
        printResults(config, cedarResult);
        printResults(config, apacheResult);
        
        // Determine winner
        String winner = determineWinner(cedarResult, apacheResult);
        LOG.info(String.format("%-30s | %-12s | %15s | %15s | %15s | %15s | %10s",
                "", "", "", "", "", "", winner));
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < 145; i++) separator.append("-");
        LOG.info(separator.toString());
    }
    
    private TestResult testCedar(Object[][] keys, String[] values, TestConfig config) {
        long totalPutNanos = 0;
        long totalGetNanos = 0;
        
        for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
            // MultiKeyMap doesn't do defensive copying for maximum performance
            com.cedarsoftware.util.MultiKeyMap<String> map = com.cedarsoftware.util.MultiKeyMap.<String>builder()
                .simpleKeysMode(true)
                .capacity(config.dataSize)
                .build();
            
            // Test PUT operations
            long putStart = System.nanoTime();
            for (int i = 0; i < keys.length; i++) {
                if (config.keyCount == 1) {
                    map.put(keys[i][0], values[i]);
                } else {
                    map.putMultiKey(values[i], keys[i]);
                }
            }
            long putEnd = System.nanoTime();
            totalPutNanos += (putEnd - putStart);
            
            // Test GET operations
            long getStart = System.nanoTime();
            for (Object[] key : keys) {
                if (config.keyCount == 1) {
                    map.get(key[0]);
                } else {
                    map.getMultiKey(key);
                }
            }
            long getEnd = System.nanoTime();
            totalGetNanos += (getEnd - getStart);
        }
        
        return new TestResult("Cedar", 
                totalPutNanos / TEST_ITERATIONS, 
                totalGetNanos / TEST_ITERATIONS, 
                config.dataSize);
    }
    
    private TestResult testApache(Object[][] keys, String[] values, TestConfig config) {
        long totalPutNanos = 0;
        long totalGetNanos = 0;
        
        for (int iter = 0; iter < TEST_ITERATIONS; iter++) {
            MultiKeyMap<Object, String> map = new MultiKeyMap<>();
            
            // Test PUT operations
            long putStart = System.nanoTime();
            if (config.keyCount == 1) {
                // Apache MultiKeyMap with single key
                for (int i = 0; i < keys.length; i++) {
                    map.put(new MultiKey<Object>(new Object[]{keys[i][0]}), values[i]);
                }
            } else {
                // Apache MultiKeyMap with multiple keys
                for (int i = 0; i < keys.length; i++) {
                    map.put(new MultiKey<Object>(keys[i]), values[i]);
                }
            }
            long putEnd = System.nanoTime();
            totalPutNanos += (putEnd - putStart);
            
            // Test GET operations
            long getStart = System.nanoTime();
            if (config.keyCount == 1) {
                for (Object[] key : keys) {
                    map.get(new MultiKey<Object>(new Object[]{key[0]}));
                }
            } else {
                for (Object[] key : keys) {
                    map.get(new MultiKey<Object>(key));
                }
            }
            long getEnd = System.nanoTime();
            totalGetNanos += (getEnd - getStart);
        }
        
        return new TestResult("Apache", 
                totalPutNanos / TEST_ITERATIONS, 
                totalGetNanos / TEST_ITERATIONS, 
                config.dataSize);
    }
    
    private Object[][] generateKeys(int count, int keyCount) {
        Object[][] keys = new Object[count][keyCount];
        for (int i = 0; i < count; i++) {
            for (int j = 0; j < keyCount; j++) {
                // Mix of different key types for realistic testing
                switch (j % 4) {
                    case 0:
                        keys[i][j] = "key" + i + "_" + j;
                        break;
                    case 1:
                        keys[i][j] = Integer.valueOf(i * 1000 + j);
                        break;
                    case 2:
                        keys[i][j] = Long.valueOf(i * 1000000L + j);
                        break;
                    case 3:
                        keys[i][j] = Double.valueOf(i + j / 10.0);
                        break;
                }
            }
        }
        return keys;
    }
    
    private String[] generateValues(int count) {
        String[] values = new String[count];
        for (int i = 0; i < count; i++) {
            values[i] = "value_" + i;
        }
        return values;
    }
    
    private void printResults(TestConfig config, TestResult result) {
        LOG.info(String.format("%-30s | %-12s | %,15.1f | %,15.1f | %,15.1f | %,15.1f |",
                config.name,
                result.implementation,
                result.putOpsPerMs(),
                result.getOpsPerMs(),
                result.avgPutNanos(),
                result.avgGetNanos()));
    }
    
    private String determineWinner(TestResult cedar, TestResult apache) {
        // Compare based on average of put and get performance
        double cedarAvg = (cedar.putOpsPerMs() + cedar.getOpsPerMs()) / 2;
        double apacheAvg = (apache.putOpsPerMs() + apache.getOpsPerMs()) / 2;
        
        if (cedarAvg > apacheAvg * 1.1) {
            return "Cedar++";
        } else if (apacheAvg > cedarAvg * 1.1) {
            return "Apache++";
        } else {
            return "Tie";
        }
    }
}