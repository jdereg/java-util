package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Performance comparison between CaseInsensitiveMap and MultiKeyMap.
 * Tests three scenarios:
 * 1. CaseInsensitiveMap (the baseline)
 * 2. MultiKeyMap with single String key (non-array, non-collection)
 * 3. MultiKeyMap with String[] containing 1 element
 * 
 * Tests with different data sizes: small (100), medium (10,000), large (100,000)
 * 
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class CaseInsensitiveMapVsMultiKeyMapPerformanceTest {

    private static final int WARMUP_ITERATIONS = 10_000;
    private static final int MEASUREMENT_ITERATIONS = 100_000;
    private static final int SMALL_SIZE = 100;
    private static final int MEDIUM_SIZE = 10_000;
    private static final int LARGE_SIZE = 100_000;
    
    // Test data
    private String[] testKeys;
    private String[] lookupKeys;
    private static final String TEST_VALUE = "testValue";
    
    @Test
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    public void comparePerformance() {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("CaseInsensitiveMap vs MultiKeyMap Performance Comparison");
        System.out.println(repeat("=", 80));
        
        // Test with different sizes
        runComparison(SMALL_SIZE, "SMALL (100 entries)");
        runComparison(MEDIUM_SIZE, "MEDIUM (10,000 entries)");
        runComparison(LARGE_SIZE, "LARGE (100,000 entries)");
        
        System.out.println("\n" + repeat("=", 80));
        System.out.println("Performance Analysis Summary");
        System.out.println(repeat("=", 80));
    }
    
    private void runComparison(int size, String sizeLabel) {
        System.out.println("\n" + repeat("-", 80));
        System.out.println("Testing with " + sizeLabel);
        System.out.println(repeat("-", 80));
        
        // Generate test data
        generateTestData(size);
        
        // Create maps - CaseInsensitiveMap backed by ConcurrentHashMap for fair comparison
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(
            Collections.emptyMap(), 
            new ConcurrentHashMap<String, String>(size)
        );
        MultiKeyMap<String> mkMapSingle = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .capacity(size)
            .build();
        MultiKeyMap<String> mkMapArray = MultiKeyMap.<String>builder()
            .caseSensitive(false)
            .capacity(size)
            .build();
        
        // Populate maps
        for (String key : testKeys) {
            ciMap.put(key, TEST_VALUE);
            mkMapSingle.put(key, TEST_VALUE);
            mkMapArray.put(new String[]{key}, TEST_VALUE);
        }
        
        // Warm up JVM
        System.out.println("\nWarming up JVM...");
        warmUp(ciMap, mkMapSingle, mkMapArray);
        
        // Measure PUT performance
        System.out.println("\n📊 PUT Performance (nanoseconds per operation):");
        long ciPutTime = measurePuts(new CaseInsensitiveMap<>(Collections.emptyMap(), new ConcurrentHashMap<String, String>(size)), 
                                     "CaseInsensitiveMap");
        long mkSinglePutTime = measurePuts(MultiKeyMap.<String>builder().caseSensitive(false).capacity(size).build(), 
                                           "MultiKeyMap (single)", false);
        long mkArrayPutTime = measurePuts(MultiKeyMap.<String>builder().caseSensitive(false).capacity(size).build(), 
                                          "MultiKeyMap (array)", true);
        
        // Measure GET performance
        System.out.println("\n📊 GET Performance (nanoseconds per operation):");
        long ciGetTime = measureGets(ciMap, "CaseInsensitiveMap", false);
        long mkSingleGetTime = measureGets(mkMapSingle, "MultiKeyMap (single)", false);
        long mkArrayGetTime = measureGets(mkMapArray, "MultiKeyMap (array)", true);
        
        // Measure MIXED case GET performance (more realistic)
        System.out.println("\n📊 MIXED-CASE GET Performance (nanoseconds per operation):");
        long ciMixedTime = measureMixedCaseGets(ciMap, "CaseInsensitiveMap", false);
        long mkSingleMixedTime = measureMixedCaseGets(mkMapSingle, "MultiKeyMap (single)", false);
        long mkArrayMixedTime = measureMixedCaseGets(mkMapArray, "MultiKeyMap (array)", true);
        
        // Calculate relative performance
        System.out.println("\n📈 Relative Performance (lower is better):");
        System.out.println("PUT operations:");
        System.out.printf("  MultiKeyMap (single) is %.2fx %s than CaseInsensitiveMap\n", 
                         (double)mkSinglePutTime / ciPutTime,
                         mkSinglePutTime < ciPutTime ? "faster" : "slower");
        System.out.printf("  MultiKeyMap (array)  is %.2fx %s than CaseInsensitiveMap\n", 
                         (double)mkArrayPutTime / ciPutTime,
                         mkArrayPutTime < ciPutTime ? "faster" : "slower");
        
        System.out.println("GET operations:");
        System.out.printf("  MultiKeyMap (single) is %.2fx %s than CaseInsensitiveMap\n", 
                         (double)mkSingleGetTime / ciGetTime,
                         mkSingleGetTime < ciGetTime ? "faster" : "slower");
        System.out.printf("  MultiKeyMap (array)  is %.2fx %s than CaseInsensitiveMap\n", 
                         (double)mkArrayGetTime / ciGetTime,
                         mkArrayGetTime < ciGetTime ? "faster" : "slower");
        
        System.out.println("MIXED-CASE GET operations:");
        System.out.printf("  MultiKeyMap (single) is %.2fx %s than CaseInsensitiveMap\n", 
                         (double)mkSingleMixedTime / ciMixedTime,
                         mkSingleMixedTime < ciMixedTime ? "faster" : "slower");
        System.out.printf("  MultiKeyMap (array)  is %.2fx %s than CaseInsensitiveMap\n", 
                         (double)mkArrayMixedTime / ciMixedTime,
                         mkArrayMixedTime < ciMixedTime ? "faster" : "slower");
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
            // Randomly uppercase some characters
            if (random.nextBoolean()) {
                c = Character.toUpperCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    private void warmUp(Map<String, String> ciMap, Map<Object, String> mkMapSingle, Map<Object, String> mkMapArray) {
        // Warm up with mixed operations
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            String key = testKeys[i % testKeys.length];
            String lookupKey = lookupKeys[i % lookupKeys.length];
            
            // CaseInsensitiveMap
            ciMap.get(lookupKey);
            ciMap.containsKey(lookupKey);
            
            // MultiKeyMap single
            mkMapSingle.get(lookupKey);
            mkMapSingle.containsKey(lookupKey);
            
            // MultiKeyMap array
            mkMapArray.get(new String[]{lookupKey});
            mkMapArray.containsKey(new String[]{lookupKey});
        }
    }
    
    private long measurePuts(Map map, String mapType) {
        return measurePuts(map, mapType, false);
    }
    
    private long measurePuts(Map map, String mapType, boolean useArray) {
        // Clear any existing data
        map.clear();
        
        // Force GC before measurement
        System.gc();
        
        long startTime = System.nanoTime();
        
        if (useArray) {
            for (String key : testKeys) {
                map.put(new String[]{key}, TEST_VALUE);
            }
        } else {
            for (String key : testKeys) {
                map.put(key, TEST_VALUE);
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        long avgTime = totalTime / testKeys.length;
        
        System.out.printf("  %-25s: %,10d ns/op (total: %,d ms)\n", 
                         mapType, avgTime, totalTime / 1_000_000);
        
        return avgTime;
    }
    
    private long measureGets(Map map, String mapType, boolean useArray) {
        // Force GC before measurement
        System.gc();
        
        int iterations = MEASUREMENT_ITERATIONS;
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            String key = testKeys[i % testKeys.length];
            if (useArray) {
                map.get(new String[]{key});
            } else {
                map.get(key);
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        long avgTime = totalTime / iterations;
        
        System.out.printf("  %-25s: %,10d ns/op (total: %,d ms for %,d ops)\n", 
                         mapType, avgTime, totalTime / 1_000_000, iterations);
        
        return avgTime;
    }
    
    private long measureMixedCaseGets(Map map, String mapType, boolean useArray) {
        // Force GC before measurement
        System.gc();
        
        int iterations = MEASUREMENT_ITERATIONS;
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            String key = lookupKeys[i % lookupKeys.length];  // Use mixed-case lookup keys
            if (useArray) {
                map.get(new String[]{key});
            } else {
                map.get(key);
            }
        }
        
        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        long avgTime = totalTime / iterations;
        
        System.out.printf("  %-25s: %,10d ns/op (total: %,d ms for %,d ops)\n", 
                         mapType, avgTime, totalTime / 1_000_000, iterations);
        
        return avgTime;
    }
    
    @Test
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    public void detailedMemoryAndCollisionAnalysis() {
        System.out.println("\n" + repeat("=", 80));
        System.out.println("Memory and Collision Analysis");
        System.out.println(repeat("=", 80));
        
        int[] sizes = {100, 1000, 10_000, 100_000};
        
        for (int size : sizes) {
            System.out.println("\n" + repeat("-", 80));
            System.out.println("Size: " + String.format("%,d", size) + " entries");
            System.out.println(repeat("-", 80));
            
            generateTestData(size);
            
            // Create and populate maps - CaseInsensitiveMap backed by ConcurrentHashMap
            CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(
                Collections.emptyMap(),
                new ConcurrentHashMap<String, String>(size)
            );
            MultiKeyMap<String> mkMap = MultiKeyMap.<String>builder()
                .caseSensitive(false)
                .capacity(size)
                .build();
            
            long ciMemBefore = getUsedMemory();
            for (String key : testKeys) {
                ciMap.put(key, TEST_VALUE);
            }
            long ciMemAfter = getUsedMemory();
            
            System.gc();
            Thread.yield();
            
            long mkMemBefore = getUsedMemory();
            for (String key : testKeys) {
                mkMap.put(key, TEST_VALUE);
            }
            long mkMemAfter = getUsedMemory();
            
            System.out.println("Approximate memory usage:");
            System.out.printf("  CaseInsensitiveMap: %,d bytes\n", (ciMemAfter - ciMemBefore));
            System.out.printf("  MultiKeyMap:        %,d bytes\n", (mkMemAfter - mkMemBefore));
            
            // Check actual sizes
            System.out.println("Map sizes (should match):");
            System.out.printf("  CaseInsensitiveMap size: %,d\n", ciMap.size());
            System.out.printf("  MultiKeyMap size:        %,d\n", mkMap.size());
        }
    }
    
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder(str.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}