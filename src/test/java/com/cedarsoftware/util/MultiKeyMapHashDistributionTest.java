package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Test to analyze hash distribution quality with different MAX_HASH_ELEMENTS values
 */
public class MultiKeyMapHashDistributionTest {
    private static final Logger LOG = Logger.getLogger(MultiKeyMapHashDistributionTest.class.getName());
    
    @Test
    public void analyzeHashDistribution() {
        // Test different array sizes and measure collision rates
        int[] testSizes = {1000, 10000, 100000};
        int[] elementCounts = {1, 2, 3, 4, 5, 6, 8, 10};
        
        LOG.info("=== Hash Distribution Analysis ===");
        LOG.info("Testing collision rates with different numbers of elements used for hashing");
        LOG.info("");
        
        for (int size : testSizes) {
            LOG.info(String.format("Dataset size: %,d arrays", size));
            
            // Generate test data - arrays with 10 elements each
            List<Object[]> testArrays = generateTestArrays(size, 10);
            
            for (int elementsToHash : elementCounts) {
                int collisions = measureCollisions(testArrays, elementsToHash);
                double collisionRate = (double) collisions / size * 100;
                LOG.info(String.format("  Elements hashed: %2d -> Collisions: %,6d (%.3f%%)", 
                    elementsToHash, collisions, collisionRate));
            }
            LOG.info("");
        }
        
        // Test with different array lengths
        LOG.info("Testing with different array lengths (using 4 elements for hash):");
        int[] arrayLengths = {4, 6, 10, 20, 50, 100};
        
        for (int arrayLen : arrayLengths) {
            List<Object[]> testArrays = generateTestArrays(10000, arrayLen);
            int collisions = measureCollisions(testArrays, 4);
            double collisionRate = (double) collisions / 10000 * 100;
            LOG.info(String.format("  Array length: %3d -> Collisions: %,6d (%.3f%%)", 
                arrayLen, collisions, collisionRate));
        }
        
        // Test worst-case scenario - arrays that differ only after 4th element
        LOG.info("");
        LOG.info("Worst-case test - arrays differ only after element 4:");
        testWorstCase();
    }
    
    private List<Object[]> generateTestArrays(int count, int arrayLength) {
        List<Object[]> arrays = new ArrayList<>();
        Random random = ThreadLocalRandom.current();
        
        for (int i = 0; i < count; i++) {
            Object[] array = new Object[arrayLength];
            for (int j = 0; j < arrayLength; j++) {
                // Mix of different types for realistic distribution
                switch (random.nextInt(4)) {
                    case 0:
                        array[j] = "str" + random.nextInt(1000);
                        break;
                    case 1:
                        array[j] = random.nextInt(1000);
                        break;
                    case 2:
                        array[j] = random.nextDouble();
                        break;
                    case 3:
                        array[j] = random.nextBoolean();
                        break;
                }
            }
            arrays.add(array);
        }
        
        return arrays;
    }
    
    private int measureCollisions(List<Object[]> arrays, int elementsToHash) {
        Map<Integer, Integer> hashCounts = new HashMap<>();
        int collisions = 0;
        
        for (Object[] array : arrays) {
            int hash = computeHash(array, elementsToHash);
            int count = hashCounts.getOrDefault(hash, 0);
            if (count > 0) {
                collisions++;
            }
            hashCounts.put(hash, count + 1);
        }
        
        return collisions;
    }
    
    private int computeHash(Object[] array, int elementsToHash) {
        int h = 1;
        int limit = Math.min(array.length, elementsToHash);
        
        for (int i = 0; i < limit; i++) {
            Object e = array[i];
            if (e == null) {
                h *= 31;
            } else {
                h = h * 31 + e.hashCode();
            }
        }
        
        // Apply MurmurHash3 finalization
        return finalizeHash(h);
    }
    
    private int finalizeHash(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }
    
    private void testWorstCase() {
        // Create 1000 arrays that have identical first 4 elements
        List<Object[]> arrays = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Object[] array = new Object[10];
            // First 4 elements are identical
            array[0] = "same";
            array[1] = 42;
            array[2] = 3.14;
            array[3] = true;
            // Rest are different
            for (int j = 4; j < 10; j++) {
                array[j] = "unique" + i + "_" + j;
            }
            arrays.add(array);
        }
        
        // Measure collisions with different element counts
        for (int elements : new int[]{3, 4, 5, 6}) {
            int collisions = measureCollisions(arrays, elements);
            LOG.info(String.format("  Elements hashed: %d -> Collisions: %,d (%.1f%%)", 
                elements, collisions, (double) collisions / 1000 * 100));
        }
    }
}