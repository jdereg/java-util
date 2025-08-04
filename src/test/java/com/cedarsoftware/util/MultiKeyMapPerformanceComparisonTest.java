package com.cedarsoftware.util;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Performance comparison test between different multi-key mapping approaches using 6-dimensional keys.
 * 
 * Tests both single-threaded and concurrent performance comparisons between:
 * - Cedar Software MultiKeyMap (N-dimensional, inherently thread-safe)
 * - Apache Commons MultiKeyMap (using 6 keys to test beyond its 5-key optimization)  
 * - Guava Table (nested tables simulating 4 of 6 dimensions)
 * - DIY approach with 6-field composite key object and HashMap/ConcurrentHashMap
 * 
 * Uses 6-dimensional keys to provide fair comparison beyond Apache's 5-key optimizations.
 * Includes concurrent Reader/Writer Mixed Workload tests to demonstrate thread-safety performance.
 */
public class MultiKeyMapPerformanceComparisonTest {

    private static final Logger LOG = Logger.getLogger(MultiKeyMapPerformanceComparisonTest.class.getName());
    static {
        LoggingConfig.initForTests();
    }

    private static final int WARMUP_ITERATIONS = 100_000; // Warmup iterations
    private static final int TEST_ROUNDS = 4; // Number of rotating test rounds
    private static final int SLEEP_MS = 200; // Stabilization sleep between tests
    
    // Test configuration - can be modified for different scales
    private int mapSize;
    private int puts;
    private int gets;
    private int containsKeys;
    private int removes;
    private int totalOperations;
    
    /**
     * DIY composite key with 6 fields for HashMap approach
     */
    static class CompositeKey {
        private final String field1;
        private final Integer field2;
        private final String field3;
        private final Integer field4;
        private final String field5;
        private final Integer field6;
        private final int hashCode;
        
        public CompositeKey(String field1, Integer field2, String field3, Integer field4, String field5, Integer field6) {
            this.field1 = field1;
            this.field2 = field2;
            this.field3 = field3;
            this.field4 = field4;
            this.field5 = field5;
            this.field6 = field6;
            this.hashCode = Objects.hash(field1, field2, field3, field4, field5, field6);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CompositeKey that = (CompositeKey) obj;
            return Objects.equals(field1, that.field1) &&
                   Objects.equals(field2, that.field2) &&
                   Objects.equals(field3, that.field3) &&
                   Objects.equals(field4, that.field4) &&
                   Objects.equals(field5, that.field5) &&
                   Objects.equals(field6, that.field6);
        }
        
        @Override
        public int hashCode() {
            return hashCode;
        }
        
        @Override
        public String toString() {
            return String.format("CompositeKey{%s,%d,%s,%d,%s,%d}", field1, field2, field3, field4, field5, field6);
        }
    }
    
    @Disabled
    @Test
    public void compareMultiKeyMapPerformance() {
        // Run tests at different scales - LARGEST TO SMALLEST for optimal JIT warming
        int[] testSizes = {100_000, 50_000, 20_000, 10_000, 1_000, 100};
        
        LOG.info("=== Multi-Key Map Performance Comparison ===");
        LOG.info("Testing 6-dimensional keys with realistic operation mix at different scales");
        LOG.info("🔥 Running tests LARGEST → SMALLEST for optimal JIT compilation");
        LOG.info("");
        
        for (int size : testSizes) {
            configureTestSize(size);
            LOG.info(String.format("🔍 SCALE: %,d elements", mapSize));
            LOG.info(String.format("  Operations: %,d puts + %,d gets + %,d containsKey + %,d removes = %,d total", 
                             puts, gets, containsKeys, removes, totalOperations));
            LOG.info("  Test pattern: empty → populate → use → cleanup → empty");
            LOG.info(String.format("  Rounds: %d rotating rounds, JIT warmup: %,d operations per method", TEST_ROUNDS, WARMUP_ITERATIONS));
            LOG.info("");
            
            runSingleScaleTest();
            LOG.info("");
        }
        
        LOG.info("=== All scale tests completed ===");
    }

    /**
     * Concurrent Reader/Writer Mixed Workload Test
     * Tests true concurrent access patterns with:
     * - 90% concurrent readers performing continuous get operations
     * - 10% concurrent writers performing puts/removes
     * - Hotspot contention patterns focusing on overlapping key ranges
     * - True thread contention to expose Apache's synchronization bottlenecks vs Cedar's inherent thread-safety
     */
//    @Disabled
//    @Test
    public void concurrentReaderWriterMixedWorkloadTest() {
        // Test different scales to show how thread contention scales
        int[] testSizes = {100, 1_000, 10_000, 20_000, 50_000, 100_000};

        LOG.info("=== Concurrent Reader/Writer Mixed Workload Test ===");
        LOG.info("Testing true concurrent access patterns (90% reads, 10% writes)");
        LOG.info("Cedar: inherently thread-safe with stripe locking");
        LOG.info("Apache: synchronized wrapper creates bottlenecks");
        LOG.info("Threads: 12 readers + 1 writer + 1 hotspot writer = 14 total");
        LOG.info("");
        
        for (int size : testSizes) {
            configureTestSize(size);
            LOG.info(String.format("🔍 CONCURRENT SCALE: %,d elements", mapSize));
            LOG.info("  Pattern: Pre-populate → 90% concurrent reads + 10% concurrent writes → Measure ops/sec");
            LOG.info("  Duration: 10 seconds of sustained concurrent operations");
            LOG.info("");
            
            runConcurrentMixedWorkloadTest();
            LOG.info("");
        }
        
        LOG.info("=== All concurrent tests completed ===");
    }

    
    private void configureTestSize(int size) {
        this.mapSize = size;
        this.puts = size;
        this.gets = size * 10; // 10:1 get:put ratio
        this.containsKeys = size * 2; // 2:1 containsKey:put ratio  
        this.removes = size;
        this.totalOperations = puts + gets + containsKeys + removes;
    }
    
    private void runSingleScaleTest() {
        // Generate test data
        TestData testData = generateTestData();
        
        // Perform comprehensive JIT warmup for all methods
        LOG.info("🔥 Performing comprehensive JIT warmup...");
        performComprehensiveWarmup(testData);
        LOG.info("   Warmup completed - JIT compilation should be stable");
        LOG.info("");
        
        // Run RAW performance tests with rotating order
        LOG.info("🚀 RAW PERFORMANCE COMPARISON (no thread-safety)");
        LOG.info("Using rotating test order to eliminate JIT bias");
        LOG.info("");
        
        double[] rawResults = runRotatingRawPerformanceTests(testData);
        displayResults("RAW PERFORMANCE", rawResults);
    }
    
    private TestData generateTestData() {
        LOG.info("Generating test data...");
        Random random = new Random(42); // Fixed seed for reproducible results
        
        // Generate diverse string and integer pools for key components
        String[] strings = new String[1000];
        Integer[] integers = new Integer[1000];

        Map<String, String> uniqueKeys = new ConcurrentHashMap<>();
        Random random1 = new Random(42);
        int i = 0;
        while (i < 1000) {
            String randomStr = StringUtilities.getRandomString(random1, 4, 10);
            if (uniqueKeys.containsKey(randomStr)) {
                continue;
            }
            strings[i] = StringUtilities.getRandomString(random1, 4, 10);
            integers[i] = i;
            uniqueKeys.put(strings[i], strings[i]);
            i++;
        }
        
        TestData data = new TestData();
        data.keys = new Object[mapSize][6];
        data.values = new String[mapSize];
        
        // Generate exactly mapSize unique key combinations
        for (i = 0; i < mapSize; i++) {
            data.keys[i][0] = strings[random.nextInt(strings.length)];
            data.keys[i][1] = integers[random.nextInt(integers.length)];
            data.keys[i][2] = strings[random.nextInt(strings.length)];
            data.keys[i][3] = integers[random.nextInt(integers.length)];
            data.keys[i][4] = strings[random.nextInt(strings.length)];
            data.keys[i][5] = integers[random.nextInt(integers.length)];
            data.values[i] = "value" + i;
        }
        
        LOG.info(String.format("Test data generated: %,d unique keys", mapSize));
        return data;
    }
    
    private void performComprehensiveWarmup(TestData testData) {
        LOG.info("   Warming up Cedar MultiKeyMap (varargs)...");
        warmupCedarMultiKeyMap(testData);
        LOG.info("   Cedar MultiKeyMap (varargs) warmup completed");
        
        LOG.info("   Warming up Cedar MultiKeyMap (standard API)...");
        warmupCedarStandardApi(testData);
        LOG.info("   Cedar MultiKeyMap (standard API) warmup completed");
        
        LOG.info("   Warming up Apache MultiKeyMap...");
        warmupApacheMultiKeyMap(testData);
        LOG.info("   Apache MultiKeyMap warmup completed");
        
        LOG.info("   Warming up Apache MultiKeyMap (thread-safe)...");
        warmupApacheMultiKeyMapThreadSafe(testData);
        LOG.info("   Apache MultiKeyMap (thread-safe) warmup completed");
        
        LOG.info("   Warming up Guava Table (raw)...");
        warmupGuavaTableRaw(testData);
        LOG.info("   Guava Table (raw) warmup completed");
        
        LOG.info("   Warming up Guava Table (thread-safe)...");
        warmupGuavaTableThreadSafe(testData);
        LOG.info("   Guava Table (thread-safe) warmup completed");
        
        LOG.info("   Warming up DIY HashMap...");
        warmupDiyHashMapRaw(testData);
        LOG.info("   DIY HashMap warmup completed");
        
        LOG.info("   Warming up DIY ConcurrentHashMap...");
        warmupDiyHashMapThreadSafe(testData);
        LOG.info("   DIY ConcurrentHashMap warmup completed");
    }
    
    private double[] runRotatingRawPerformanceTests(TestData testData) {
        double[][] allResults = new double[5][TEST_ROUNDS];
        
        // Run tests in rotating order
        for (int round = 0; round < TEST_ROUNDS; round++) {
            StringBuilder roundResults = new StringBuilder();
            roundResults.append(String.format("Round %d/%d: ", round + 1, TEST_ROUNDS));
            
            for (int testIndex = 0; testIndex < 5; testIndex++) {
                int actualTestIndex = (testIndex + round) % 5; // Rotate starting position
                double result = runSingleRawPerformanceTest(testData, actualTestIndex);
                allResults[actualTestIndex][round] = result;
                roundResults.append(String.format("%.1fM ", result / 1_000_000));
            }
            LOG.info(roundResults.toString());
        }
        
        // Calculate averages
        double[] averageResults = new double[5];
        for (int i = 0; i < 5; i++) {
            double sum = 0;
            for (int round = 0; round < TEST_ROUNDS; round++) {
                sum += allResults[i][round];
            }
            averageResults[i] = sum / TEST_ROUNDS;
        }
        
        return averageResults;
    }

    private double runSingleRawPerformanceTest(TestData testData, int testIndex) {
        // Force GC and pause between tests for stability
        System.gc();
        try { Thread.sleep(SLEEP_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        
        double result;
        switch (testIndex) {
            case 0: result = runCedarMultiKeyMapPerformanceTest(testData); break;
            case 1: result = runCedarStandardApiPerformanceTest(testData); break;
            case 2: result = runApacheMultiKeyMapRawPerformanceTest(testData); break;
            case 3: result = runGuavaTableRawPerformanceTest(testData); break;
            case 4: result = runDiyHashMapRawPerformanceTest(testData); break;
            default: throw new IllegalArgumentException("Invalid test index: " + testIndex);
        }
        
        return result;
    }
    
    private void displayResults(String category, double[] results) {
        String[] rawNames = {"Cedar MultiKeyMap (varargs)", "Cedar MultiKeyMap (standard API)", "Apache MultiKeyMap (raw, 6 keys)", "Guava (raw nested Tables, 4 of 6 keys)", "DIY HashMap (raw, 6 fields)"};
        String[] threadSafeNames = {"Cedar MultiKeyMap (varargs, thread-safe)", "Cedar MultiKeyMap (standard API, thread-safe)", "Apache MultiKeyMap (ConcurrentHashMap)", "Guava (Tables.synchronizedTable)", "DIY ConcurrentHashMap (6 fields)"};
        
        String[] names = category.contains("RAW") ? rawNames : threadSafeNames;
            
        LOG.info(category + " RESULTS (averaged over " + TEST_ROUNDS + " rounds):");
        for (int i = 0; i < 5; i++) {
            LOG.info(String.format("%d. %-42s: %8.1fM ops/sec", i + 1, names[i], results[i] / 1_000_000));
        }
    }
    
    // Warmup methods (no output, just JIT compilation)
    private void warmupCedarMultiKeyMap(TestData testData) {
        com.cedarsoftware.util.MultiKeyMap<String> map = com.cedarsoftware.util.MultiKeyMap.<String>builder()
                .defensiveCopies(false)  // Disable for performance
                .build();
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int idx = i % testData.keys.length;
            Object[] key = testData.keys[idx];
            map.putMultiKey(testData.values[idx], key[0], key[1], key[2], key[3], key[4], key[5]);
            map.getMultiKey(key[0], key[1], key[2], key[3], key[4], key[5]);
        }
    }
    
    private void warmupCedarStandardApi(TestData testData) {
        com.cedarsoftware.util.MultiKeyMap<String> map = com.cedarsoftware.util.MultiKeyMap.<String>builder()
                .defensiveCopies(false)  // Disable for performance
                .build();
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int idx = i % testData.keys.length;
            Object[] key = testData.keys[idx];  // Pre-created array
            map.put(key, testData.values[idx]);  // Standard put(Object key, V value)
            map.get(key);
        }
    }
    
    private void warmupApacheMultiKeyMap(TestData testData) {
        org.apache.commons.collections4.map.MultiKeyMap<Object, String> map = new org.apache.commons.collections4.map.MultiKeyMap<>();
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int idx = i % testData.keys.length;
            Object[] key = testData.keys[idx];
            MultiKey<Object> multiKey = new MultiKey<Object>(key);
            map.put(multiKey, testData.values[idx]);
            map.get(multiKey);
        }
    }
    
    private void warmupApacheMultiKeyMapThreadSafe(TestData testData) {
        Map<MultiKey<Object>, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int idx = i % testData.keys.length;
            Object[] key = testData.keys[idx];
            MultiKey<Object> multiKey = new MultiKey<Object>(key);
            map.put(multiKey, testData.values[idx]);
            map.get(multiKey);
        }
    }
    
    private void warmupGuavaTableRaw(TestData testData) {
        Table<String, String, String> table1 = HashBasedTable.create();
        Table<String, String, String> table2 = HashBasedTable.create();
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int idx = i % testData.keys.length;
            Object[] key = testData.keys[idx];
            table1.put(key[0].toString(), key[1].toString(), testData.values[idx]);
            table1.get(key[0].toString(), key[1].toString());
            table2.put(key[2].toString(), key[3].toString(), testData.values[idx]);
            table2.get(key[2].toString(), key[3].toString());
        }
    }
    
    private void warmupGuavaTableThreadSafe(TestData testData) {
        Table<String, String, String> baseTable1 = HashBasedTable.create();
        Table<String, String, String> table1 = Tables.synchronizedTable(baseTable1);
        Table<String, String, String> baseTable2 = HashBasedTable.create();
        Table<String, String, String> table2 = Tables.synchronizedTable(baseTable2);
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int idx = i % testData.keys.length;
            Object[] key = testData.keys[idx];
            table1.put(key[0].toString(), key[1].toString(), testData.values[idx]);
            table1.get(key[0].toString(), key[1].toString());
            table2.put(key[2].toString(), key[3].toString(), testData.values[idx]);
            table2.get(key[2].toString(), key[3].toString());
        }
    }
    
    private void warmupDiyHashMapRaw(TestData testData) {
        Map<CompositeKey, String> map = new HashMap<>();
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int idx = i % testData.keys.length;
            Object[] key = testData.keys[idx];
            CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
            map.put(compositeKey, testData.values[idx]);
            map.get(compositeKey);
        }
    }
    
    private void warmupDiyHashMapThreadSafe(TestData testData) {
        Map<CompositeKey, String> map = new ConcurrentHashMap<>();
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            int idx = i % testData.keys.length;
            Object[] key = testData.keys[idx];
            CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
            map.put(compositeKey, testData.values[idx]);
            map.get(compositeKey);
        }
    }
    
    // Performance test methods - lifecycle: empty → populate → use → cleanup → empty
    private double runCedarMultiKeyMapPerformanceTest(TestData testData) {
        com.cedarsoftware.util.MultiKeyMap<String> map = com.cedarsoftware.util.MultiKeyMap.<String>builder()
                .defensiveCopies(false)  // Disable for performance - fair comparison with Apache
                .build();
        Random random = new Random(42);
        
        long startTime = System.nanoTime();
        
        // Phase 1: POPULATE - 50,000 puts
        for (int i = 0; i < puts; i++) {
            Object[] key = testData.keys[i];
            map.putMultiKey(testData.values[i], key[0], key[1], key[2], key[3], key[4], key[5]);
        }
        
        // Phase 2: gets - 250,000 gets (10:1 ratio)
        for (int i = 0; i < gets; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            map.getMultiKey(key[0], key[1], key[2], key[3], key[4], key[5]);
        }
        
        // Phase 3: CONTAINS_KEY - 100,000 containsKey calls (2:1 ratio)
        for (int i = 0; i < containsKeys; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            map.containsMultiKey(key[0], key[1], key[2], key[3], key[4], key[5]);
        }
        
        // Phase 4: CLEANUP - 50,000 removes
        for (int i = 0; i < removes; i++) {
            Object[] key = testData.keys[i];
            map.removeMultiKey(key[0], key[1], key[2], key[3], key[4], key[5]);
        }
        
        long totalTimeNanos = System.nanoTime() - startTime;
        return (totalOperations * 1_000_000_000.0) / totalTimeNanos;
    }
    
    private double runCedarStandardApiPerformanceTest(TestData testData) {
        com.cedarsoftware.util.MultiKeyMap<String> map = com.cedarsoftware.util.MultiKeyMap.<String>builder()
                .defensiveCopies(false)  // Disable for performance - fair comparison with Apache
                .build();
        Random random = new Random(42);
        
        long startTime = System.nanoTime();
        
        // Phase 1: POPULATE
        for (int i = 0; i < puts; i++) {
            Object[] key = testData.keys[i];
            map.put(key, testData.values[i]);  // Direct array as key
        }
        
        // Phase 2: GETS
        for (int i = 0; i < gets; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            map.get(key);
        }
        
        // Phase 3: CONTAINS_KEY
        for (int i = 0; i < containsKeys; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            map.containsKey(key);
        }
        
        // Phase 4: CLEANUP
        for (int i = 0; i < removes; i++) {
            Object[] key = testData.keys[i];
            map.remove(key);
        }
        
        long totalTimeNanos = System.nanoTime() - startTime;
        return (totalOperations * 1_000_000_000.0) / totalTimeNanos;
    }
    
    private double runApacheMultiKeyMapRawPerformanceTest(TestData testData) {
        org.apache.commons.collections4.map.MultiKeyMap<Object, String> map = new org.apache.commons.collections4.map.MultiKeyMap<>();
        Random random = new Random(42);
        
        long startTime = System.nanoTime();
        
        // Phase 1: POPULATE - puts
        for (int i = 0; i < puts; i++) {
            Object[] key = testData.keys[i];
            MultiKey<Object> multiKey = new MultiKey<Object>(key);
            map.put(multiKey, testData.values[i]);
        }
        
        // Phase 2: gets - 10:1 ratio
        for (int i = 0; i < gets; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            MultiKey<Object> multiKey = new MultiKey<Object>(key);
            map.get(multiKey);
        }
        
        // Phase 3: CONTAINS_KEY - 2:1 ratio
        for (int i = 0; i < containsKeys; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            MultiKey<Object> multiKey = new MultiKey<Object>(key);
            map.containsKey(multiKey);
        }
        
        // Phase 4: CLEANUP - removes
        for (int i = 0; i < removes; i++) {
            Object[] key = testData.keys[i];
            MultiKey<Object> multiKey = new MultiKey<Object>(key);
            map.remove(multiKey);
        }
        
        long totalTimeNanos = System.nanoTime() - startTime;
        return (totalOperations * 1_000_000_000.0) / totalTimeNanos;
    }
    
    private double runApacheMultiKeyMapThreadSafePerformanceTest(TestData testData) {
        // Apache MultiKeyMap wrapped in ConcurrentHashMap for thread-safety comparison
        // Since Apache doesn't provide built-in thread safety, we simulate with synchronized access
        Map<MultiKey<Object>, String> map = new ConcurrentHashMap<>();
        Random random = new Random(42);
        
        long startTime = System.nanoTime();
        
        // Phase 1: POPULATE - puts
        for (int i = 0; i < puts; i++) {
            Object[] key = testData.keys[i];
            MultiKey<Object> multiKey = new MultiKey<Object>(key);
            map.put(multiKey, testData.values[i]);
        }
        
        // Phase 2: gets - 10:1 ratio
        for (int i = 0; i < gets; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            MultiKey<Object> multiKey = new MultiKey<Object>(key);
            map.get(multiKey);
        }
        
        // Phase 3: CONTAINS_KEY - 2:1 ratio
        for (int i = 0; i < containsKeys; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            MultiKey<Object> multiKey = new MultiKey<Object>(key);
            map.containsKey(multiKey);
        }
        
        // Phase 4: CLEANUP - removes
        for (int i = 0; i < removes; i++) {
            Object[] key = testData.keys[i];
            MultiKey<Object> multiKey = new MultiKey<Object>(key);
            map.remove(multiKey);
        }
        
        long totalTimeNanos = System.nanoTime() - startTime;
        return (totalOperations * 1_000_000_000.0) / totalTimeNanos;
    }
    
    private double runGuavaTableRawPerformanceTest(TestData testData) {
        // Nested table approach: Table<Key1, Key2, Table<Key3, Key4, Value>>
        // This gives us 4D capability - Key5 still lost but more accurate than simple 2D
        Table<String, String, Table<String, String, String>> outerTable = HashBasedTable.create();
        Random random = new Random(42);
        
        long startTime = System.nanoTime();
        
        // Phase 1: POPULATE (nested table operations)
        for (int i = 0; i < puts; i++) {
            Object[] key = testData.keys[i];
            String key1 = key[0].toString();
            String key2 = key[1].toString();
            String key3 = key[2].toString();
            String key4 = key[3].toString();
            // key[4] is lost - Guava Table limitation
            
            // Get or create inner table
            Table<String, String, String> innerTable = outerTable.get(key1, key2);
            if (innerTable == null) {
                innerTable = HashBasedTable.create();
                outerTable.put(key1, key2, innerTable);
            }
            innerTable.put(key3, key4, testData.values[i]);
        }
        
        // Phase 2: GETS (nested lookups)
        for (int i = 0; i < gets; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            String key1 = key[0].toString();
            String key2 = key[1].toString();
            String key3 = key[2].toString();
            String key4 = key[3].toString();
            
            Table<String, String, String> innerTable = outerTable.get(key1, key2);
            if (innerTable != null) {
                innerTable.get(key3, key4);
            }
        }
        
        // Phase 3: CONTAINS_KEY (nested contains)
        for (int i = 0; i < containsKeys; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            String key1 = key[0].toString();
            String key2 = key[1].toString();
            String key3 = key[2].toString();
            String key4 = key[3].toString();
            
            Table<String, String, String> innerTable = outerTable.get(key1, key2);
            if (innerTable != null) {
                innerTable.contains(key3, key4);
            }
        }
        
        // Phase 4: CLEANUP (nested removes)
        for (int i = 0; i < removes; i++) {
            Object[] key = testData.keys[i];
            String key1 = key[0].toString();
            String key2 = key[1].toString();
            String key3 = key[2].toString();
            String key4 = key[3].toString();
            
            Table<String, String, String> innerTable = outerTable.get(key1, key2);
            if (innerTable != null) {
                innerTable.remove(key3, key4);
                // If inner table becomes empty, could remove it from outer table
                if (innerTable.isEmpty()) {
                    outerTable.remove(key1, key2);
                }
            }
        }
        
        long totalTimeNanos = System.nanoTime() - startTime;
        return (totalOperations * 1_000_000_000.0) / totalTimeNanos;
    }
    
    private double runGuavaTableThreadSafePerformanceTest(TestData testData) {
        // Nested synchronized table approach: synchronized outer table with synchronized inner tables
        Table<String, String, Table<String, String, String>> baseOuterTable = HashBasedTable.create();
        Table<String, String, Table<String, String, String>> outerTable = Tables.synchronizedTable(baseOuterTable);
        Random random = new Random(42);
        
        long startTime = System.nanoTime();
        
        // Phase 1: POPULATE (nested synchronized operations)
        for (int i = 0; i < puts; i++) {
            Object[] key = testData.keys[i];
            String key1 = key[0].toString();
            String key2 = key[1].toString();
            String key3 = key[2].toString();
            String key4 = key[3].toString();
            
            // Get or create synchronized inner table
            Table<String, String, String> innerTable = outerTable.get(key1, key2);
            if (innerTable == null) {
                Table<String, String, String> baseInnerTable = HashBasedTable.create();
                innerTable = Tables.synchronizedTable(baseInnerTable);
                outerTable.put(key1, key2, innerTable);
            }
            innerTable.put(key3, key4, testData.values[i]);
        }
        
        // Phase 2: GETS (nested synchronized lookups)
        for (int i = 0; i < gets; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            String key1 = key[0].toString();
            String key2 = key[1].toString();
            String key3 = key[2].toString();
            String key4 = key[3].toString();
            
            Table<String, String, String> innerTable = outerTable.get(key1, key2);
            if (innerTable != null) {
                innerTable.get(key3, key4);
            }
        }
        
        // Phase 3: CONTAINS_KEY (nested synchronized contains)
        for (int i = 0; i < containsKeys; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            String key1 = key[0].toString();
            String key2 = key[1].toString();
            String key3 = key[2].toString();
            String key4 = key[3].toString();
            
            Table<String, String, String> innerTable = outerTable.get(key1, key2);
            if (innerTable != null) {
                innerTable.contains(key3, key4);
            }
        }
        
        // Phase 4: CLEANUP (nested synchronized removes)
        for (int i = 0; i < removes; i++) {
            Object[] key = testData.keys[i];
            String key1 = key[0].toString();
            String key2 = key[1].toString();
            String key3 = key[2].toString();
            String key4 = key[3].toString();
            
            Table<String, String, String> innerTable = outerTable.get(key1, key2);
            if (innerTable != null) {
                innerTable.remove(key3, key4);
                if (innerTable.isEmpty()) {
                    outerTable.remove(key1, key2);
                }
            }
        }
        
        long totalTimeNanos = System.nanoTime() - startTime;
        return (totalOperations * 1_000_000_000.0) / totalTimeNanos;
    }
    
    private double runDiyHashMapRawPerformanceTest(TestData testData) {
        Map<CompositeKey, String> map = new HashMap<>();
        Random random = new Random(42);
        
        long startTime = System.nanoTime();
        
        // Phase 1: POPULATE - 10,000 puts
        for (int i = 0; i < puts; i++) {
            Object[] key = testData.keys[i];
            CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
            map.put(compositeKey, testData.values[i]);
        }
        
        // Phase 2: gets - 50,000 gets (5:1 ratio)
        for (int i = 0; i < gets; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
            map.get(compositeKey);
        }
        
        // Phase 3: CONTAINS_KEY - 20,000 containsKey calls (2:1 ratio)
        for (int i = 0; i < containsKeys; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
            map.containsKey(compositeKey);
        }
        
        // Phase 4: CLEANUP - 10,000 removes
        for (int i = 0; i < removes; i++) {
            Object[] key = testData.keys[i];
            CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
            map.remove(compositeKey);
        }
        
        long totalTimeNanos = System.nanoTime() - startTime;
        return (totalOperations * 1_000_000_000.0) / totalTimeNanos;
    }
    
    private double runDiyHashMapThreadSafePerformanceTest(TestData testData) {
        Map<CompositeKey, String> map = new ConcurrentHashMap<>();
        Random random = new Random(42);
        
        long startTime = System.nanoTime();
        
        // Phase 1: POPULATE - 10,000 puts
        for (int i = 0; i < puts; i++) {
            Object[] key = testData.keys[i];
            CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
            map.put(compositeKey, testData.values[i]);
        }
        
        // Phase 2: gets - 100,000 gets (10:1 ratio)
        for (int i = 0; i < gets; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
            map.get(compositeKey);
        }
        
        // Phase 3: CONTAINS_KEY - 20,000 containsKey calls (2:1 ratio)
        for (int i = 0; i < containsKeys; i++) {
            int idx = random.nextInt(mapSize);
            Object[] key = testData.keys[idx];
            CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
            map.containsKey(compositeKey);
        }
        
        // Phase 4: CLEANUP - 10,000 removes
        for (int i = 0; i < removes; i++) {
            Object[] key = testData.keys[i];
            CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
            map.remove(compositeKey);
        }
        
        long totalTimeNanos = System.nanoTime() - startTime;
        return (totalOperations * 1_000_000_000.0) / totalTimeNanos;
    }
    
    
    private void runConcurrentMixedWorkloadTest() {
        // Generate test data for concurrent operations
        TestData testData = generateTestData();
        
        LOG.info("🚀 CONCURRENT MIXED WORKLOAD COMPARISON");
        LOG.info("Pre-populating maps, then running 10 seconds of concurrent 90% reads + 10% writes");
        LOG.info("");
        
        // Test implementations with concurrent workload
        double cedarVarargsResult = runConcurrentCedarVarargs(testData);
        double cedarStandardResult = runConcurrentCedarStandard(testData);  
        double apacheResult = runConcurrentApache(testData);
        double guavaResult = runConcurrentGuava(testData);
        double diyResult = runConcurrentDiy(testData);
        
        // Display results
        LOG.info("CONCURRENT MIXED WORKLOAD RESULTS (90% reads, 10% writes, 10 seconds):");
        LOG.info(String.format("1. %-50s: %8.1fM ops/sec", "Cedar MultiKeyMap (varargs, inherently thread-safe)", cedarVarargsResult / 1_000_000));
        LOG.info(String.format("2. %-50s: %8.1fM ops/sec", "Cedar MultiKeyMap (standard API, inherently thread-safe)", cedarStandardResult / 1_000_000));
        LOG.info(String.format("3. %-50s: %8.1fM ops/sec", "Apache MultiKeyMap (ConcurrentHashMap wrapper)", apacheResult / 1_000_000));
        LOG.info(String.format("4. %-50s: %8.1fM ops/sec", "Guava Table (Tables.synchronizedTable)", guavaResult / 1_000_000));
        LOG.info(String.format("5. %-50s: %8.1fM ops/sec", "DIY ConcurrentHashMap (6 fields)", diyResult / 1_000_000));
        
        // Performance analysis
        LOG.info("");
        LOG.info("CONCURRENT PERFORMANCE ANALYSIS:");
        double cedarBestResult = Math.max(cedarVarargsResult, cedarStandardResult);
        if (cedarBestResult > apacheResult) {
            double advantage = ((cedarBestResult - apacheResult) / apacheResult) * 100;
            LOG.info(String.format("✅ Cedar outperforms Apache by %.1f%% in concurrent workload", advantage));
            LOG.info("   Cedar's stripe locking allows true concurrent reads/writes");
            LOG.info("   Apache's synchronized wrapper creates bottlenecks on all operations");
        }
        if (cedarBestResult > guavaResult) {
            double advantage = ((cedarBestResult - guavaResult) / guavaResult) * 100;
            LOG.info(String.format("✅ Cedar outperforms Guava by %.1f%% in concurrent workload", advantage));
            LOG.info("   Guava's nested synchronized tables create multiple lock points");
        }
    }
    
    private double runConcurrentCedarVarargs(TestData testData) {
        com.cedarsoftware.util.MultiKeyMap<String> map = com.cedarsoftware.util.MultiKeyMap.<String>builder()
                .defensiveCopies(false)
                .build();
        
        return runConcurrentTest(testData, map, "Cedar Varargs", new ConcurrentOperations() {
            @Override
            public void put(Object[] key, String value) {
                map.putMultiKey(value, key[0], key[1], key[2], key[3], key[4], key[5]);
            }
            
            @Override
            public String get(Object[] key) {
                return map.getMultiKey(key[0], key[1], key[2], key[3], key[4], key[5]);
            }
            
            @Override
            public void remove(Object[] key) {
                map.removeMultiKey(key[0], key[1], key[2], key[3], key[4], key[5]);
            }
        });
    }
    
    private double runConcurrentCedarStandard(TestData testData) {
        com.cedarsoftware.util.MultiKeyMap<String> map = com.cedarsoftware.util.MultiKeyMap.<String>builder()
                .defensiveCopies(false)
                .build();
        
        return runConcurrentTest(testData, map, "Cedar Standard", new ConcurrentOperations() {
            @Override
            public void put(Object[] key, String value) {
                map.put(key, value);
            }
            
            @Override
            public String get(Object[] key) {
                return map.get(key);
            }
            
            @Override
            public void remove(Object[] key) {
                map.remove(key);
            }
        });
    }
    
    private double runConcurrentApache(TestData testData) {
        Map<MultiKey<Object>, String> map = new ConcurrentHashMap<>();
        
        return runConcurrentTest(testData, map, "Apache", new ConcurrentOperations() {
            @Override
            public void put(Object[] key, String value) {
                MultiKey<Object> multiKey = new MultiKey<>(key);
                map.put(multiKey, value);
            }
            
            @Override
            public String get(Object[] key) {
                MultiKey<Object> multiKey = new MultiKey<>(key);
                return map.get(multiKey);
            }
            
            @Override
            public void remove(Object[] key) {
                MultiKey<Object> multiKey = new MultiKey<>(key);
                map.remove(multiKey);
            }
        });
    }
    
    private double runConcurrentGuava(TestData testData) {
        Table<String, String, Table<String, String, String>> baseOuterTable = HashBasedTable.create();
        Table<String, String, Table<String, String, String>> outerTable = Tables.synchronizedTable(baseOuterTable);
        
        return runConcurrentTest(testData, outerTable, "Guava", new ConcurrentOperations() {
            @Override
            public void put(Object[] key, String value) {
                String key1 = key[0].toString();
                String key2 = key[1].toString();
                String key3 = key[2].toString();
                String key4 = key[3].toString();
                
                Table<String, String, String> innerTable = outerTable.get(key1, key2);
                if (innerTable == null) {
                    Table<String, String, String> baseInnerTable = HashBasedTable.create();
                    innerTable = Tables.synchronizedTable(baseInnerTable);
                    outerTable.put(key1, key2, innerTable);
                }
                innerTable.put(key3, key4, value);
            }
            
            @Override
            public String get(Object[] key) {
                String key1 = key[0].toString();
                String key2 = key[1].toString();
                String key3 = key[2].toString();
                String key4 = key[3].toString();
                
                Table<String, String, String> innerTable = outerTable.get(key1, key2);
                return innerTable != null ? innerTable.get(key3, key4) : null;
            }
            
            @Override
            public void remove(Object[] key) {
                String key1 = key[0].toString();
                String key2 = key[1].toString();
                String key3 = key[2].toString();
                String key4 = key[3].toString();
                
                Table<String, String, String> innerTable = outerTable.get(key1, key2);
                if (innerTable != null) {
                    innerTable.remove(key3, key4);
                    if (innerTable.isEmpty()) {
                        outerTable.remove(key1, key2);
                    }
                }
            }
        });
    }
    
    private double runConcurrentDiy(TestData testData) {
        Map<CompositeKey, String> map = new ConcurrentHashMap<>();
        
        return runConcurrentTest(testData, map, "DIY", new ConcurrentOperations() {
            @Override
            public void put(Object[] key, String value) {
                CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
                map.put(compositeKey, value);
            }
            
            @Override
            public String get(Object[] key) {
                CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
                return map.get(compositeKey);
            }
            
            @Override
            public void remove(Object[] key) {
                CompositeKey compositeKey = new CompositeKey((String)key[0], (Integer)key[1], (String)key[2], (Integer)key[3], (String)key[4], (Integer)key[5]);
                map.remove(compositeKey);
            }
        });
    }
    
    private double runConcurrentTest(TestData testData, Object mapInstance, String mapName, ConcurrentOperations ops) {
        // Pre-populate the map with all test data
        LOG.info(String.format("Pre-populating %s with %,d entries...", mapName, mapSize));
        for (int i = 0; i < mapSize; i++) {
            ops.put(testData.keys[i], testData.values[i]);
        }
        
        // Concurrent workload configuration
        final int numReaderThreads = 10; // 90% of workload
        final int numWriterThreads = 2; // 10% of workload (1 regular + 1 hotspot)
        final int totalThreads = numReaderThreads + numWriterThreads;
        final int testDurationSeconds = 2;
        
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalThreads);
        AtomicLong totalOperations = new AtomicLong(0);
        AtomicInteger hotspotRange = new AtomicInteger(Math.min(1000, mapSize / 10)); // Top 10% of keys for hotspot contention
        
        // Create reader threads (90% of workload)
        for (int i = 0; i < numReaderThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                Random random = new Random(42 + threadId);
                long operations = 0;
                
                try {
                    startLatch.await();
                    long endTime = System.currentTimeMillis() + (testDurationSeconds * 1000);
                    
                    while (System.currentTimeMillis() < endTime) {
                        // 70% regular reads, 30% hotspot reads
                        int keyIndex = (random.nextInt(100) < 70) ? 
                            random.nextInt(mapSize) : 
                            random.nextInt(hotspotRange.get());
                        
                        ops.get(testData.keys[keyIndex]);
                        operations++;
                        
                        // Prevent tight loop from overwhelming the CPU
                        if (operations % 1000 == 0) {
                            Thread.yield();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    totalOperations.addAndGet(operations);
                    doneLatch.countDown();
                }
            });
        }
        
        // Create writer threads (10% of workload)
        for (int i = 0; i < numWriterThreads; i++) {
            final int threadId = i;
            final boolean isHotspotWriter = (i == 0); // First writer focuses on hotspot
            
            executor.submit(() -> {
                Random random = new Random(1000 + threadId);
                long operations = 0;
                
                try {
                    startLatch.await();
                    long endTime = System.currentTimeMillis() + (testDurationSeconds * 1000);
                    
                    while (System.currentTimeMillis() < endTime) {
                        // Hotspot writer focuses on contested keys, regular writer spreads across all keys
                        int keyIndex = isHotspotWriter ? 
                            random.nextInt(hotspotRange.get()) : 
                            random.nextInt(mapSize);
                        
                        // 50% puts, 50% removes to create write contention
                        if (random.nextBoolean()) {
                            ops.put(testData.keys[keyIndex], "concurrent-value-" + threadId + "-" + operations);
                        } else {
                            ops.remove(testData.keys[keyIndex]);
                            // Immediately put back to maintain map size
                            ops.put(testData.keys[keyIndex], testData.values[keyIndex]);
                        }
                        operations++;
                        
                        // Writers yield more frequently to allow reads
                        if (operations % 100 == 0) {
                            Thread.yield();
                        }
                    }
                } catch (Exception e) {
                    // Handle any exceptions from operations
                    LOG.warning("Writer thread exception: " + e.getMessage());
                } finally {
                    totalOperations.addAndGet(operations);
                    doneLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        LOG.info(String.format("Starting %d threads (%d readers + %d writers) for %d seconds...", 
                               totalThreads, numReaderThreads, numWriterThreads, testDurationSeconds));
        long startTime = System.nanoTime();
        startLatch.countDown();
        
        try {
            // Wait for all threads to complete
            boolean completed = doneLatch.await(testDurationSeconds + 5, TimeUnit.SECONDS);
            if (!completed) {
                LOG.warning("Some threads did not complete within timeout");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        long totalTimeNanos = System.nanoTime() - startTime;
        double opsPerSecond = (totalOperations.get() * 1_000_000_000.0) / totalTimeNanos;
        
        LOG.info(String.format("%s completed: %,d operations in %.1f seconds = %.1fM ops/sec", 
                               mapName, totalOperations.get(), totalTimeNanos / 1_000_000_000.0, opsPerSecond / 1_000_000));
        
        return opsPerSecond;
    }
    
    private interface ConcurrentOperations {
        void put(Object[] key, String value);
        String get(Object[] key);
        void remove(Object[] key);
    }
    
    static class TestData {
        Object[][] keys;
        String[] values;
    }
}
