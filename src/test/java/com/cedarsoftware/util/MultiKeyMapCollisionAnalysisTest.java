package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Test to analyze collision patterns in MultiKeyMap with different MAX_HASH_ELEMENTS values
 */
public class MultiKeyMapCollisionAnalysisTest {
    private static final Logger LOG = Logger.getLogger(MultiKeyMapCollisionAnalysisTest.class.getName());
    
    @Test
    public void analyzeCollisionsWithDifferentMaxHashElements() throws Exception {
        int numElements = 1_000_000;
        
        LOG.info("=== MultiKeyMap Collision Analysis ===");
        LOG.info(String.format("Testing with %,d elements", numElements));
        LOG.info("");
        
        // Test with MAX_HASH_ELEMENTS = 4 (current)
        LOG.info("Testing with MAX_HASH_ELEMENTS = 4:");
        testPerformanceAndCollisions(numElements, 4);
        
        // Now let's simulate what would happen with MAX_HASH_ELEMENTS = 5
        LOG.info("\nSimulating with MAX_HASH_ELEMENTS = 5:");
        testPerformanceAndCollisions(numElements, 5);
        
        // And with 3 for comparison
        LOG.info("\nSimulating with MAX_HASH_ELEMENTS = 3:");
        testPerformanceAndCollisions(numElements, 3);
    }
    
    private void testPerformanceAndCollisions(int numElements, int maxHashElements) {
        // Generate test data
        List<Object[]> testData = generateTestData(numElements);
        
        // Measure unique hashes and collisions
        Map<Integer, List<Integer>> hashToIndices = new HashMap<>();
        
        LOG.info("Computing hashes...");
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numElements; i++) {
            Object[] key = testData.get(i);
            int hash = computeHashWithLimit(key, maxHashElements);
            hashToIndices.computeIfAbsent(hash, k -> new ArrayList<>()).add(i);
        }
        
        long hashTime = System.currentTimeMillis() - startTime;
        LOG.info(String.format("Hash computation completed in %,d ms", hashTime));
        
        // Analyze collision patterns
        int uniqueHashes = hashToIndices.size();
        int maxCollisions = 0;
        Map<Integer, Integer> collisionDistribution = new TreeMap<>();
        
        for (List<Integer> indices : hashToIndices.values()) {
            int collisionCount = indices.size() - 1; // -1 because first entry isn't a collision
            if (collisionCount > 0) {
                collisionDistribution.merge(collisionCount, 1, Integer::sum);
                if (indices.size() > maxCollisions) {
                    maxCollisions = indices.size();
                }
            }
        }
        
        // Calculate statistics
        int totalCollisions = numElements - uniqueHashes;
        double collisionRate = (double) totalCollisions / numElements * 100;
        
        LOG.info("\nCollision Statistics:");
        LOG.info(String.format("Total entries: %,d", numElements));
        LOG.info(String.format("Unique hashes: %,d", uniqueHashes));
        LOG.info(String.format("Total collisions: %,d (%.3f%%)", totalCollisions, collisionRate));
        LOG.info(String.format("Max collisions at single hash: %d", maxCollisions));
        
        // Show collision distribution
        LOG.info("\nCollision Distribution (showing entries with collisions):");
        LOG.info("Collisions | Count");
        LOG.info("-----------|-------");
        for (Map.Entry<Integer, Integer> entry : collisionDistribution.entrySet()) {
            LOG.info(String.format("    %6d | %,d", entry.getKey(), entry.getValue()));
        }
        
        // Test actual MultiKeyMap performance
        LOG.info("\nTesting actual MultiKeyMap performance:");
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        startTime = System.currentTimeMillis();
        for (int i = 0; i < numElements; i++) {
            map.put(testData.get(i), "value" + i);
        }
        long putTime = System.currentTimeMillis() - startTime;
        LOG.info(String.format("Put %,d entries in %,d ms (%.1f ops/ms)", 
            numElements, putTime, (double) numElements / putTime));
        
        // Test retrieval
        startTime = System.nanoTime();
        Random random = ThreadLocalRandom.current();
        int lookups = 100_000;
        for (int i = 0; i < lookups; i++) {
            Object[] key = testData.get(random.nextInt(numElements));
            map.get(key);
        }
        long getTime = System.nanoTime() - startTime;
        double avgGetNanos = (double) getTime / lookups;
        LOG.info(String.format("Average get time: %.1f ns", avgGetNanos));
    }
    
    private int computeHashWithLimit(Object[] array, int maxElements) {
        int h = 1;
        int limit = Math.min(array.length, maxElements);
        
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
    
    private List<Object[]> generateTestData(int count) {
        List<Object[]> data = new ArrayList<>(count);
        Random random = ThreadLocalRandom.current();
        
        for (int i = 0; i < count; i++) {
            // Create 6-element arrays with realistic data patterns
            Object[] key = new Object[6];
            
            // Simulate real-world key patterns
            key[0] = "user" + (i % 10000);          // User IDs with some repetition
            key[1] = random.nextInt(1000);          // Numeric ID
            key[2] = "type" + random.nextInt(50);   // Limited set of types
            key[3] = random.nextDouble() * 100;     // Floating point data
            key[4] = "cat" + random.nextInt(20);    // Categories
            key[5] = System.nanoTime() + i;         // Timestamps (unique)
            
            data.add(key);
        }
        
        return data;
    }
}