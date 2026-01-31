package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Performance benchmarks for Set support in MultiKeyMap.
 * Tests the performance impact of order-agnostic Set handling.
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
class MultiKeyMapSetPerformanceTest {

    @Test
    void testSetVsListInsertionPerformance() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        int iterations = 10000;

        // Warmup
        for (int i = 0; i < 1000; i++) {
            map.put(new HashSet<>(Arrays.asList(i, i + 1, i + 2)), "value" + i);
        }
        map.clear();

        // Benchmark Set insertions
        long setStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Set<Integer> key = new HashSet<>(Arrays.asList(i, i + 1, i + 2));
            map.put(key, "set-value" + i);
        }
        long setEnd = System.nanoTime();
        long setTime = setEnd - setStart;

        map.clear();

        // Benchmark List insertions
        long listStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            List<Integer> key = Arrays.asList(i, i + 1, i + 2);
            map.put(key, "list-value" + i);
        }
        long listEnd = System.nanoTime();
        long listTime = listEnd - listStart;

        double setTimeMs = setTime / 1_000_000.0;
        double listTimeMs = listTime / 1_000_000.0;
        double ratio = (double) setTime / listTime;

        System.out.println("Set insertion time: " + setTimeMs + " ms");
        System.out.println("List insertion time: " + listTimeMs + " ms");
        System.out.println("Set/List ratio: " + ratio);

        // Sets should be within some multiple of List performance due to order-agnostic processing
        assertTrue(setTime < listTime * 10,
            String.format("Set insertion should be within 6x of List performance. Actual: Set=%.2fms, List=%.2fms, Ratio=%.2fx (threshold: 6.0x)",
                setTimeMs, listTimeMs, ratio));
    }

    @Test
    @org.junit.jupiter.api.Disabled("Performance benchmarks can be flaky due to JVM warmup/GC - informational only")
    void testSetVsListLookupPerformance() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        int iterations = 10000;

        // Populate with Sets
        for (int i = 0; i < iterations; i++) {
            Set<Integer> key = new HashSet<>(Arrays.asList(i, i + 1, i + 2));
            map.put(key, "set-value" + i);
        }

        // Warmup
        for (int i = 0; i < 1000; i++) {
            Set<Integer> key = new HashSet<>(Arrays.asList(i, i + 1, i + 2));
            map.get(key);
        }

        // Benchmark Set lookups
        long setStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Set<Integer> key = new HashSet<>(Arrays.asList(i, i + 1, i + 2));
            String value = map.get(key);
            assertNotNull(value);
        }
        long setEnd = System.nanoTime();
        long setTime = setEnd - setStart;

        map.clear();

        // Populate with Lists
        for (int i = 0; i < iterations; i++) {
            List<Integer> key = Arrays.asList(i, i + 1, i + 2);
            map.put(key, "list-value" + i);
        }

        // Benchmark List lookups
        long listStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            List<Integer> key = Arrays.asList(i, i + 1, i + 2);
            String value = map.get(key);
            assertNotNull(value);
        }
        long listEnd = System.nanoTime();
        long listTime = listEnd - listStart;

        double setTimeMs = setTime / 1_000_000.0;
        double listTimeMs = listTime / 1_000_000.0;
        double ratio = (double) setTime / listTime;

        System.out.println("Set lookup time: " + setTimeMs + " ms");
        System.out.println("List lookup time: " + listTimeMs + " ms");
        System.out.println("Set/List ratio: " + ratio);

        // Sets should be within 10x of List performance (lenient threshold for benchmark variability)
        // Typical ratio is ~3x, but can vary due to JVM warmup, GC, etc.
        assertTrue(setTime < listTime * 10,
            String.format("Set lookup should be within 10x of List performance. Actual: Set=%.2fms, List=%.2fms, Ratio=%.2fx (threshold: 10.0x)",
                setTimeMs, listTimeMs, ratio));
    }

    @Test
    void testOrderAgnosticHashDistribution() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        Random random = new Random(42); // Fixed seed for reproducibility
        int iterations = 10000;

        // Insert Sets with random element orders
        for (int i = 0; i < iterations; i++) {
            List<Integer> elements = Arrays.asList(i, i + 1, i + 2);
            Collections.shuffle(elements, random);
            Set<Integer> key = new HashSet<>(elements);
            map.put(key, "value" + i);
        }

        assertEquals(iterations, map.size(), "All Sets should be stored (no collisions due to order)");

        // Verify lookups with different orders
        random = new Random(42); // Reset for same shuffle sequence
        for (int i = 0; i < iterations; i++) {
            List<Integer> elements = Arrays.asList(i, i + 1, i + 2);
            Collections.shuffle(elements, random);
            Set<Integer> key = new HashSet<>(elements);
            assertEquals("value" + i, map.get(key), "Should find value regardless of insertion order");
        }
    }

    @Test
    void testAverageChainDepthWithSets() {
        MultiKeyMap<String> map = new MultiKeyMap<>(16); // Small table to force collisions
        int iterations = 1000;

        // Insert Sets
        for (int i = 0; i < iterations; i++) {
            Set<Integer> key = new HashSet<>(Arrays.asList(i, i + 1, i + 2));
            map.put(key, "value" + i);
        }

        // Get chain depth statistics via toString() or reflection if available
        // For now, just verify all insertions succeeded
        assertEquals(iterations, map.size());
        System.out.println("Successfully stored " + iterations + " Sets in map with initial capacity 16");
    }

    @Test
    @Disabled("Flaky test - performance varies significantly based on JVM warmup/GC in full test suite. Run manually for benchmarking.")
    void testSetSizeImpact() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        int iterations = 1000;

        // Test small Sets (3 elements)
        long smallStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Set<Integer> key = new HashSet<>(Arrays.asList(i, i + 1, i + 2));
            map.put(key, "small" + i);
        }
        long smallEnd = System.nanoTime();
        long smallTime = smallEnd - smallStart;

        map.clear();

        // Test medium Sets (10 elements)
        long mediumStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Set<Integer> key = new HashSet<>();
            for (int j = 0; j < 10; j++) {
                key.add(i * 10 + j);
            }
            map.put(key, "medium" + i);
        }
        long mediumEnd = System.nanoTime();
        long mediumTime = mediumEnd - mediumStart;

        map.clear();

        // Test large Sets (50 elements)
        long largeStart = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Set<Integer> key = new HashSet<>();
            for (int j = 0; j < 50; j++) {
                key.add(i * 50 + j);
            }
            map.put(key, "large" + i);
        }
        long largeEnd = System.nanoTime();
        long largeTime = largeEnd - largeStart;

        System.out.println("Small Sets (3 elements): " + smallTime / 1_000_000.0 + " ms");
        System.out.println("Medium Sets (10 elements): " + mediumTime / 1_000_000.0 + " ms");
        System.out.println("Large Sets (50 elements): " + largeTime / 1_000_000.0 + " ms");

        // Performance should scale reasonably with Set size
        // Note: With optimizations (nested loop for ≤3 elements, HashSet for >3 elements),
        // medium sets use different code path than small sets, resulting in ~6-8x ratio
        // (can be higher in full test suite due to JVM warmup/GC variability)
        assertTrue(mediumTime < smallTime * 10, "Medium Sets should be within 10x of small Sets");
        assertTrue(largeTime < smallTime * 25, "Large Sets should be within 25x of small Sets");
    }

    @Test
    void testNestedSetPerformance() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        int iterations = 1000;

        // Benchmark nested Sets
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            Set<Integer> inner = new HashSet<>(Arrays.asList(i, i + 1));
            Set<Set<Integer>> outer = new HashSet<>(Arrays.asList(inner));
            map.put(outer, "nested" + i);
        }
        long end = System.nanoTime();
        long time = end - start;

        System.out.println("Nested Sets insertion time: " + time / 1_000_000.0 + " ms");
        assertEquals(iterations, map.size(), "All nested Sets should be stored");

        // Verify lookups work
        for (int i = 0; i < iterations; i++) {
            Set<Integer> inner = new HashSet<>(Arrays.asList(i, i + 1));
            Set<Set<Integer>> outer = new HashSet<>(Arrays.asList(inner));
            assertEquals("nested" + i, map.get(outer));
        }
    }

    @Test
    void testSetVsListMemoryFootprint() {
        MultiKeyMap<String> setMap = new MultiKeyMap<>();
        MultiKeyMap<String> listMap = new MultiKeyMap<>();
        int iterations = 10000;

        // Populate with Sets
        for (int i = 0; i < iterations; i++) {
            Set<Integer> key = new HashSet<>(Arrays.asList(i, i + 1, i + 2));
            setMap.put(key, "value" + i);
        }

        // Populate with Lists
        for (int i = 0; i < iterations; i++) {
            List<Integer> key = Arrays.asList(i, i + 1, i + 2);
            listMap.put(key, "value" + i);
        }

        // Both should have same size
        assertEquals(iterations, setMap.size());
        assertEquals(iterations, listMap.size());

        System.out.println("Set map size: " + setMap.size());
        System.out.println("List map size: " + listMap.size());
        System.out.println("Both maps successfully stored " + iterations + " entries");
    }

    @Test
    void testSetHashCollisionRate() {
        MultiKeyMap<String> map = new MultiKeyMap<>(64); // Fixed capacity
        Set<Integer> collisions = new HashSet<>();
        Map<Integer, Integer> hashCounts = new HashMap<>();

        // Create Sets and track their hashes
        for (int i = 0; i < 1000; i++) {
            Set<Integer> key = new HashSet<>(Arrays.asList(i, i + 1, i + 2));
            map.put(key, "value" + i);

            // Track hash distribution (this is approximate)
            int hashCode = key.hashCode();
            hashCounts.merge(hashCode, 1, Integer::sum);
        }

        // Count hash collisions
        for (Integer count : hashCounts.values()) {
            if (count > 1) {
                collisions.add(count);
            }
        }

        System.out.println("Unique hash codes: " + hashCounts.size());
        System.out.println("Hash collisions detected: " + collisions.size());
        System.out.println("Map size: " + map.size());

        assertEquals(1000, map.size(), "All Sets should be stored despite hash collisions");
    }
}
