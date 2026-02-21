package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Test to measure the overhead of iterator creation vs direct indexed access for RandomAccess collections.
 * This will help determine if the RandomAccess optimization in keysMatchCrossType is worth the code complexity.
 */
public class MultiKeyMapIteratorOverheadTest {

    private static final Logger LOG = Logger.getLogger(MultiKeyMapIteratorOverheadTest.class.getName());

    private static final int WARMUP_ITERATIONS = 10000;
    private static final int TEST_ITERATIONS = 1000000;
    private static final int[] SIZES = {2, 3, 5, 10, 20, 50};
    
    @Test
    void measureIteratorOverhead() {
        LOG.info("=== Iterator Creation Overhead Test ===");
        LOG.info("Comparing RandomAccess direct access vs Iterator access");
        LOG.info("Test iterations: " + String.format("%,d", TEST_ITERATIONS));
        
        for (int size : SIZES) {
            LOG.info("--- Array size: " + size + " ---");

            // Create test data
            Object[] array = new Object[size];
            List<Object> arrayList = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                String value = "value" + i;
                array[i] = value;
                arrayList.add(value);
            }
            
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                compareWithDirectAccess(array, arrayList, size);
                compareWithIterator(array, arrayList, size);
            }
            
            // Test direct indexed access
            long startDirect = System.nanoTime();
            boolean resultDirect = false;
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                resultDirect = compareWithDirectAccess(array, arrayList, size);
            }
            long timeDirect = System.nanoTime() - startDirect;
            
            // Test iterator access
            long startIterator = System.nanoTime();
            boolean resultIterator = false;
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                resultIterator = compareWithIterator(array, arrayList, size);
            }
            long timeIterator = System.nanoTime() - startIterator;
            
            // Calculate overhead
            double directNsPerOp = (double) timeDirect / TEST_ITERATIONS;
            double iteratorNsPerOp = (double) timeIterator / TEST_ITERATIONS;
            double overhead = iteratorNsPerOp - directNsPerOp;
            double overheadPercent = (overhead / directNsPerOp) * 100;
            
            LOG.info(String.format("  Direct access:    %,8.2f ns/op", directNsPerOp));
            LOG.info(String.format("  Iterator access:  %,8.2f ns/op", iteratorNsPerOp));
            LOG.info(String.format("  Iterator overhead: %,7.2f ns/op (%.1f%% slower)", overhead, overheadPercent));
            
            // Verify both methods return same result
            assert resultDirect == resultIterator : "Methods returned different results!";
        }
        
        LOG.info("=== Cross-Container Comparison Test ===");
        LOG.info("Testing the actual MultiKeyMap scenario: Object[] vs ArrayList");
        
        // Test the actual use case - cross container comparison
        for (int size : SIZES) {
            LOG.info("--- Array size: " + size + " ---");

            // Test with matching values (worst case - must compare all elements)
            Object[] array1 = new Object[size];
            Object[] array2 = new Object[size];
            List<Object> list1 = new ArrayList<>(size);
            List<Object> list2 = new ArrayList<>(size);
            
            for (int i = 0; i < size; i++) {
                String value = "value" + i;
                array1[i] = value;
                array2[i] = value;
                list1.add(value);
                list2.add(value);
            }
            
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                compareArrayToRandomAccess(array1, list1, size);
                compareArrayToCollection(array1, list1, size);
            }
            
            // Test optimized version (direct access)
            long startOpt = System.nanoTime();
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                compareArrayToRandomAccess(array1, list1, size);
            }
            long timeOpt = System.nanoTime() - startOpt;
            
            // Test unoptimized version (iterator)
            long startUnopt = System.nanoTime();
            for (int i = 0; i < TEST_ITERATIONS; i++) {
                compareArrayToCollection(array1, list1, size);
            }
            long timeUnopt = System.nanoTime() - startUnopt;
            
            double optNsPerOp = (double) timeOpt / TEST_ITERATIONS;
            double unoptNsPerOp = (double) timeUnopt / TEST_ITERATIONS;
            double savings = unoptNsPerOp - optNsPerOp;
            double savingsPercent = (savings / unoptNsPerOp) * 100;
            
            LOG.info(String.format("  Optimized (direct):   %,8.2f ns/op", optNsPerOp));
            LOG.info(String.format("  Unoptimized (iter):   %,8.2f ns/op", unoptNsPerOp));
            LOG.info(String.format("  Optimization savings: %,7.2f ns/op (%.1f%% faster)", savings, savingsPercent));
        }
        
        LOG.info("=== Memory Allocation Test ===");
        
        // Measure actual heap allocation
        List<Object> testList = Arrays.asList("a", "b", "c", "d", "e");
        
        // Force GC before measurement
        System.gc();
        Thread.yield();
        System.gc();
        
        long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        // Create many iterators
        List<Iterator<?>> iterators = new ArrayList<>(10000);
        for (int i = 0; i < 10000; i++) {
            iterators.add(testList.iterator());
        }
        
        long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memUsed = memAfter - memBefore;
        double bytesPerIterator = (double) memUsed / 10000;
        
        LOG.info(String.format("Memory per iterator: ~%.1f bytes", bytesPerIterator));
        LOG.info("(Note: This includes ArrayList growth and other overhead)");

        // Keep reference to prevent GC
        LOG.info("Created " + iterators.size() + " iterators");
    }
    
    // Direct indexed access (current optimization)
    private boolean compareWithDirectAccess(Object[] array, List<?> list, int size) {
        for (int i = 0; i < size; i++) {
            if (!Objects.equals(array[i], list.get(i))) {
                return false;
            }
        }
        return true;
    }
    
    // Iterator access (what we'd do without optimization)
    private boolean compareWithIterator(Object[] array, List<?> list, int size) {
        Iterator<?> iter = list.iterator();
        for (int i = 0; i < size; i++) {
            if (!Objects.equals(array[i], iter.next())) {
                return false;
            }
        }
        return true;
    }
    
    // Simulate the actual optimized comparison method
    private boolean compareArrayToRandomAccess(Object[] array, List<?> list, int arity) {
        for (int i = 0; i < arity; i++) {
            if (!Objects.equals(array[i], list.get(i))) {
                return false;
            }
        }
        return true;
    }
    
    // Simulate the unoptimized version using iterator
    private boolean compareArrayToCollection(Object[] array, Collection<?> coll, int arity) {
        Iterator<?> iter = coll.iterator();
        for (int i = 0; i < arity; i++) {
            if (!Objects.equals(array[i], iter.next())) {
                return false;
            }
        }
        return true;
    }
}