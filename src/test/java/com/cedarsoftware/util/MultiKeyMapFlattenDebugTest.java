package com.cedarsoftware.util;

import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Debug test to understand flattening behavior with typed arrays
 */
public class MultiKeyMapFlattenDebugTest {
    private static final Logger LOG = Logger.getLogger(MultiKeyMapFlattenDebugTest.class.getName());
    
    @Test
    void debugIntArrayFlattening() {
        MultiKeyMap<String> map = new MultiKeyMap<>(); // Default is flattenDimensions=true
        
        LOG.info("=== Testing int array flattening ===");
        
        // Test 2D int array with single element
        int[][] int2D = {{42}};
        map.put(int2D, "int_2d_value");
        LOG.info("Put int[][]{{42}} -> 'int_2d_value'");
        LOG.info("Map size: " + map.size());
        
        // Test what keys exist
        LOG.info("Keys in map:");
        for (Object key : map.keySet()) {
            LOG.info("  Key: " + key + " (type: " + key.getClass() + ")");
        }
        
        // Test various lookups
        LOG.info("\n=== Lookup tests ===");
        LOG.info("Lookup with int[][]{{42}}: " + map.get(new int[][]{{42}}));
        LOG.info("Lookup with int[]{42}: " + map.get(new int[]{42}));
        LOG.info("Lookup with 42: " + map.get(42));
        
        // Test containsKey
        LOG.info("\n=== ContainsKey tests ===");
        LOG.info("Contains int[][]{{42}}: " + map.containsKey(new int[][]{{42}}));
        LOG.info("Contains int[]{42}: " + map.containsKey(new int[]{42}));
        LOG.info("Contains 42: " + map.containsKey(42));
        
        // Clear and test String arrays for comparison
        map.clear();
        LOG.info("\n=== String array comparison ===");
        
        String[][] string2D = {{"hello"}};
        map.put(string2D, "string_2d_value");
        LOG.info("Put String[][]{{\"hello\"}} -> 'string_2d_value'");
        LOG.info("Map size: " + map.size());
        
        LOG.info("Keys in map:");
        for (Object key : map.keySet()) {
            LOG.info("  Key: " + key + " (type: " + key.getClass() + ")");
        }
        
        LOG.info("Lookup with String[][]{{\"hello\"}}: " + map.get(new String[][]{{"hello"}}));
        LOG.info("Lookup with String[]{\"hello\"}: " + map.get(new String[]{"hello"}));
        LOG.info("Lookup with \"hello\": " + map.get("hello"));
        
        // Clear and test Object arrays (like in existing tests)
        map.clear();
        LOG.info("\n=== Object array comparison (like existing tests) ===");
        
        map.put("a", "alpha");
        map.put(new String[]{"a"}, "[alpha]");
        map.put(new String[][]{{"a"}}, "[[alpha]]");
        LOG.info("Map size after Object array test: " + map.size());
        
        LOG.info("Keys in map:");
        for (Object key : map.keySet()) {
            LOG.info("  Key: " + key + " (type: " + key.getClass() + ")");
        }
        
        LOG.info("Lookup with \"a\": " + map.get("a"));
        LOG.info("Lookup with String[]{\"a\"}: " + map.get(new String[]{"a"}));
        LOG.info("Lookup with String[][]{{\"a\"}}: " + map.get(new String[][]{{"a"}}));
    }
}