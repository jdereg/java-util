package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Detailed performance comparison between CaseInsensitiveMap and MultiKeyMap
 * focusing on single String keys (non-array, non-collection types).
 * 
 * Tests with sizes: 100, 1000, 10000, 100000
 */
public class CaseInsensitiveMapVsMultiKeyMapDetailedTest {

    private static final int WARMUP_ITERATIONS = 50_000;
    private static final int MEASUREMENT_ITERATIONS = 500_000;
    
    // Test data
    private String[] testKeys;
    private String[] lookupKeys;
    private static final String TEST_VALUE = "testValue";
    
    @Test
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    public void detailedPerformanceComparison() {
        System.out.println("\n" + repeat("=", 100));
        System.out.println("DETAILED: CaseInsensitiveMap vs MultiKeyMap - Single String Keys Only");
        System.out.println(repeat("=", 100));
        System.out.println("Focus: Non-array, non-collection types (the most common use case)");
        System.out.println("Methodology: Average of multiple runs after JVM warmup");
        
        // Test with different sizes
        int[] sizes = {100, 1000, 10_000, 100_000};
        
        // Store results for summary
        Map<Integer, PerformanceResults> results = new LinkedHashMap<>();
        
        for (int size : sizes) {
            PerformanceResults result = runDetailedComparison(size);
            results.put(size, result);
        }
        
        // Print summary table
        printSummaryTable(results);
    }
    
    private PerformanceResults runDetailedComparison(int size) {
        System.out.println("\n" + repeat("-", 100));
        System.out.printf("Testing with %,d entries\n", size);
        System.out.println(repeat("-", 100));
        
        // Generate test data
        generateTestData(size);
        
        PerformanceResults results = new PerformanceResults(size);
        
        // Run multiple rounds and average
        int rounds = 3;
        
        for (int round = 1; round <= rounds; round++) {
            System.out.printf("\nRound %d/%d:\n", round, rounds);
            
            // Create fresh maps for each round
            CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(
                Collections.emptyMap(),
                new ConcurrentHashMap<>(size)
            );
            MultiKeyMap<String> mkMap = MultiKeyMap.<String>builder()
                .caseSensitive(false)
                .capacity(size)
                .build();
            
            // Warm up
            if (round == 1) {
                System.out.println("  Warming up JVM...");
                warmUp(ciMap, mkMap);
            }
            
            // Measure PUT performance
            long ciPutTime = measurePuts(ciMap, "CaseInsensitiveMap");
            long mkPutTime = measureSingleKeyPuts(mkMap, "MultiKeyMap");
            results.addPutTimes(ciPutTime, mkPutTime);
            
            // Ensure maps are populated for GET tests
            if (ciMap.isEmpty()) {
                for (String key : testKeys) {
                    ciMap.put(key, TEST_VALUE);
                    mkMap.put(key, TEST_VALUE);
                }
            }
            
            // Measure GET performance  
            long ciGetTime = measureGets(ciMap, "CaseInsensitiveMap");
            long mkGetTime = measureSingleKeyGets(mkMap, "MultiKeyMap");
            results.addGetTimes(ciGetTime, mkGetTime);
            
            // Measure MIXED-CASE GET performance
            long ciMixedTime = measureMixedCaseGets(ciMap, "CaseInsensitiveMap");
            long mkMixedTime = measureSingleKeyMixedCaseGets(mkMap, "MultiKeyMap");
            results.addMixedTimes(ciMixedTime, mkMixedTime);
        }
        
        // Print averages for this size
        results.printAverages();
        
        return results;
    }
    
    private void generateTestData(int size) {
        testKeys = new String[size];
        lookupKeys = new String[size];
        Random random = ThreadLocalRandom.current();
        
        for (int i = 0; i < size; i++) {
            // Generate random keys with mixed case
            String key = "Key_" + i + "_" + generateRandomString(random, 10);
            testKeys[i] = key;
            
            // Create lookup keys with different case
            if (i % 2 == 0) {
                lookupKeys[i] = key.toLowerCase();
            } else {
                lookupKeys[i] = key.toUpperCase();
            }
        }
    }
    
    private String generateRandomString(Random random, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char c = (char) ('a' + random.nextInt(26));
            if (random.nextBoolean()) {
                c = Character.toUpperCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    private void warmUp(Map<String, String> ciMap, Map<Object, String> mkMap) {
        // Populate maps if empty
        if (ciMap.isEmpty()) {
            for (String key : testKeys) {
                ciMap.put(key, TEST_VALUE);
                mkMap.put(key, TEST_VALUE);
            }
        }
        
        // Warm up with mixed operations
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String key = testKeys[i % testKeys.length];
            String lookupKey = lookupKeys[i % lookupKeys.length];
            
            ciMap.get(lookupKey);
            mkMap.get(lookupKey);
            
            if (i % 100 == 0) {
                ciMap.containsKey(lookupKey);
                mkMap.containsKey(lookupKey);
            }
        }
    }
    
    private long measurePuts(Map<String, String> map, String mapType) {
        map.clear();
        System.gc();
        
        long startTime = System.nanoTime();
        for (String key : testKeys) {
            map.put(key, TEST_VALUE);
        }
        long endTime = System.nanoTime();
        
        long totalTime = endTime - startTime;
        long avgTime = totalTime / testKeys.length;
        
        System.out.printf("    %s PUT: %,d ns/op\n", mapType, avgTime);
        return avgTime;
    }
    
    private long measureSingleKeyPuts(Map<Object, String> map, String mapType) {
        map.clear();
        System.gc();
        
        long startTime = System.nanoTime();
        for (String key : testKeys) {
            map.put(key, TEST_VALUE);  // Single String key - no array
        }
        long endTime = System.nanoTime();
        
        long totalTime = endTime - startTime;
        long avgTime = totalTime / testKeys.length;
        
        System.out.printf("    %s PUT: %,d ns/op\n", mapType, avgTime);
        return avgTime;
    }
    
    private long measureGets(Map<String, String> map, String mapType) {
        System.gc();
        
        int iterations = Math.min(MEASUREMENT_ITERATIONS, testKeys.length * 100);
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            String key = testKeys[i % testKeys.length];
            map.get(key);
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        long avgTime = totalTime / iterations;
        
        System.out.printf("    %s GET: %,d ns/op\n", mapType, avgTime);
        return avgTime;
    }
    
    private long measureSingleKeyGets(Map<Object, String> map, String mapType) {
        System.gc();
        
        int iterations = Math.min(MEASUREMENT_ITERATIONS, testKeys.length * 100);
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            String key = testKeys[i % testKeys.length];
            map.get(key);  // Single String key - no array
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        long avgTime = totalTime / iterations;
        
        System.out.printf("    %s GET: %,d ns/op\n", mapType, avgTime);
        return avgTime;
    }
    
    private long measureMixedCaseGets(Map<String, String> map, String mapType) {
        System.gc();
        
        int iterations = Math.min(MEASUREMENT_ITERATIONS, lookupKeys.length * 100);
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            String key = lookupKeys[i % lookupKeys.length];
            map.get(key);
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        long avgTime = totalTime / iterations;
        
        System.out.printf("    %s Mixed-Case GET: %,d ns/op\n", mapType, avgTime);
        return avgTime;
    }
    
    private long measureSingleKeyMixedCaseGets(Map<Object, String> map, String mapType) {
        System.gc();
        
        int iterations = Math.min(MEASUREMENT_ITERATIONS, lookupKeys.length * 100);
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            String key = lookupKeys[i % lookupKeys.length];
            map.get(key);  // Single String key - no array
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        long avgTime = totalTime / iterations;
        
        System.out.printf("    %s Mixed-Case GET: %,d ns/op\n", mapType, avgTime);
        return avgTime;
    }
    
    private void printSummaryTable(Map<Integer, PerformanceResults> results) {
        System.out.println("\n" + repeat("=", 100));
        System.out.println("PERFORMANCE SUMMARY TABLE");
        System.out.println(repeat("=", 100));
        
        // Header
        System.out.println("\n┌──────────┬───────────────────────────┬───────────────────────────┬───────────────────────────┐");
        System.out.println("│   Size   │      PUT Performance      │      GET Performance      │  Mixed-Case Performance   │");
        System.out.println("│          │   (MultiKeyMap vs CaseInsensitive)                                               │");
        System.out.println("├──────────┼───────────────────────────┼───────────────────────────┼───────────────────────────┤");
        
        for (Map.Entry<Integer, PerformanceResults> entry : results.entrySet()) {
            int size = entry.getKey();
            PerformanceResults res = entry.getValue();
            
            double putRatio = res.getAvgMkPut() / (double) res.getAvgCiPut();
            double getRatio = res.getAvgMkGet() / (double) res.getAvgCiGet();
            double mixedRatio = res.getAvgMkMixed() / (double) res.getAvgCiMixed();
            
            String putStatus = formatRatio(putRatio);
            String getStatus = formatRatio(getRatio);
            String mixedStatus = formatRatio(mixedRatio);
            
            System.out.printf("│ %,8d │ %s │ %s │ %s │\n",
                size, putStatus, getStatus, mixedStatus);
        }
        
        System.out.println("└──────────┴───────────────────────────┴───────────────────────────┴───────────────────────────┘");
        
        System.out.println("\nLegend:");
        System.out.println("  🟢 = MultiKeyMap faster (ratio < 1.0)");
        System.out.println("  🟡 = Similar performance (ratio 1.0-1.5)");
        System.out.println("  🔴 = MultiKeyMap slower (ratio > 1.5)");
        
        System.out.println("\n" + repeat("=", 100));
        System.out.println("KEY FINDINGS:");
        System.out.println(repeat("=", 100));
        
        // Analyze trends
        System.out.println("\n1. PUT Operations:");
        for (Map.Entry<Integer, PerformanceResults> entry : results.entrySet()) {
            int size = entry.getKey();
            PerformanceResults res = entry.getValue();
            double ratio = res.getAvgMkPut() / (double) res.getAvgCiPut();
            System.out.printf("   - At %,d entries: MultiKeyMap is %.2fx %s\n", 
                size, ratio, ratio < 1.0 ? "faster" : "slower");
        }
        
        System.out.println("\n2. GET Operations:");
        for (Map.Entry<Integer, PerformanceResults> entry : results.entrySet()) {
            int size = entry.getKey();
            PerformanceResults res = entry.getValue();
            double ratio = res.getAvgMkGet() / (double) res.getAvgCiGet();
            System.out.printf("   - At %,d entries: MultiKeyMap is %.2fx %s\n", 
                size, ratio, ratio < 1.0 ? "faster" : "slower");
        }
        
        System.out.println("\n3. Mixed-Case GET Operations:");
        for (Map.Entry<Integer, PerformanceResults> entry : results.entrySet()) {
            int size = entry.getKey();
            PerformanceResults res = entry.getValue();
            double ratio = res.getAvgMkMixed() / (double) res.getAvgCiMixed();
            System.out.printf("   - At %,d entries: MultiKeyMap is %.2fx %s\n", 
                size, ratio, ratio < 1.0 ? "faster" : "slower");
        }
    }
    
    private String formatRatio(double ratio) {
        String icon;
        if (ratio < 1.0) {
            icon = "🟢";
        } else if (ratio <= 1.5) {
            icon = "🟡";
        } else {
            icon = "🔴";
        }
        return String.format("%s %.2fx %-14s", icon, ratio, 
            ratio < 1.0 ? "faster" : (ratio <= 1.5 ? "comparable" : "slower"));
    }
    
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    // Helper class to track performance results
    private static class PerformanceResults {
        private final int size;
        private final List<Long> ciPutTimes = new ArrayList<>();
        private final List<Long> mkPutTimes = new ArrayList<>();
        private final List<Long> ciGetTimes = new ArrayList<>();
        private final List<Long> mkGetTimes = new ArrayList<>();
        private final List<Long> ciMixedTimes = new ArrayList<>();
        private final List<Long> mkMixedTimes = new ArrayList<>();
        
        PerformanceResults(int size) {
            this.size = size;
        }
        
        void addPutTimes(long ci, long mk) {
            ciPutTimes.add(ci);
            mkPutTimes.add(mk);
        }
        
        void addGetTimes(long ci, long mk) {
            ciGetTimes.add(ci);
            mkGetTimes.add(mk);
        }
        
        void addMixedTimes(long ci, long mk) {
            ciMixedTimes.add(ci);
            mkMixedTimes.add(mk);
        }
        
        long getAvgCiPut() { return average(ciPutTimes); }
        long getAvgMkPut() { return average(mkPutTimes); }
        long getAvgCiGet() { return average(ciGetTimes); }
        long getAvgMkGet() { return average(mkGetTimes); }
        long getAvgCiMixed() { return average(ciMixedTimes); }
        long getAvgMkMixed() { return average(mkMixedTimes); }
        
        private long average(List<Long> times) {
            return times.stream().mapToLong(Long::longValue).sum() / times.size();
        }
        
        void printAverages() {
            System.out.println("\nAverages for " + size + " entries:");
            System.out.printf("  PUT:   CaseInsensitive=%,d ns, MultiKeyMap=%,d ns (%.2fx)\n",
                getAvgCiPut(), getAvgMkPut(), getAvgMkPut() / (double) getAvgCiPut());
            System.out.printf("  GET:   CaseInsensitive=%,d ns, MultiKeyMap=%,d ns (%.2fx)\n",
                getAvgCiGet(), getAvgMkGet(), getAvgMkGet() / (double) getAvgCiGet());
            System.out.printf("  MIXED: CaseInsensitive=%,d ns, MultiKeyMap=%,d ns (%.2fx)\n",
                getAvgCiMixed(), getAvgMkMixed(), getAvgMkMixed() / (double) getAvgCiMixed());
        }
    }
}