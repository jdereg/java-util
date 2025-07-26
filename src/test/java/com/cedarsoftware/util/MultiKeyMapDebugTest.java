package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.logging.Logger;

/**
 * Debug test to understand single-element optimization behavior
 */
public class MultiKeyMapDebugTest {
    private static final Logger LOG = Logger.getLogger(MultiKeyMapDebugTest.class.getName());
    
    @Test
    void debugSingleElementOptimization() {
        MultiKeyMap<String> map = new MultiKeyMap<>(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED, false);
        
        // Test simple case first
        List<String> simpleList = new ArrayList<>();
        simpleList.add("test");
        
        LOG.info("=== Simple String Test ===");
        map.put(simpleList, "simple_value");
        LOG.info("Stored List<String> with single element 'test'");
        LOG.info("Direct string lookup: " + map.get("test"));
        LOG.info("List lookup: " + map.get(Arrays.asList("test")));
        LOG.info("Map size: " + map.size());
        
        // Clear and test nested array
        map.clear();
        LOG.info("\n=== Direct Nested Array Test ===");
        
        String[][] nestedArray = {{"a"}};
        map.put(nestedArray, "direct_nested");
        LOG.info("Stored String[][] directly");
        LOG.info("Direct nested array lookup: " + map.get(nestedArray));
        
        map.clear();
        LOG.info("\n=== Collection with Nested Array Test ===");
        
        List<String[][]> nestedList = new ArrayList<>();
        nestedList.add(nestedArray);
        
        map.put(nestedList, "nested_value");
        LOG.info("Stored List<String[][]> with single nested array element");
        LOG.info("Direct nested array lookup: " + map.get(nestedArray));
        LOG.info("List lookup: " + map.get(Arrays.asList(nestedArray)));
        
        // Test with same reference to check if it's a normalization issue
        List<String[][]> sameNestedList = new ArrayList<>();
        sameNestedList.add(nestedArray);
        LOG.info("Same reference list lookup: " + map.get(sameNestedList));
        LOG.info("Map size: " + map.size());
        
        // Let's also test what keys are actually in the map
        LOG.info("\nKeys in map:");
        for (Object key : map.keySet()) {
            LOG.info("Key: " + key + " (type: " + key.getClass() + ")");
            if (key.getClass().isArray()) {
                LOG.info("  Array contents: " + Arrays.deepToString((Object[])key));
            }
        }
        
        // Test the hash codes
        LOG.info("\nHash codes:");
        LOG.info("nestedArray.hashCode(): " + nestedArray.hashCode());
        LOG.info("Arrays.hashCode(nestedArray): " + Arrays.hashCode(nestedArray));
        LOG.info("Arrays.deepHashCode(nestedArray): " + Arrays.deepHashCode(nestedArray));
        
        // Test collection equivalence in NOT_EXPANDED mode
        map.clear();
        LOG.info("\n=== Collection Equivalence Test ===");
        
        List<String> list1 = Arrays.asList("x", "y", "z");
        List<String> list2 = Arrays.asList("x", "y", "z");
        
        map.put(list1, "list1_value");
        LOG.info("Stored list1: " + list1 + " (hashCode: " + list1.hashCode() + ")");
        LOG.info("Lookup with list2: " + map.get(list2) + " (hashCode: " + list2.hashCode() + ")");
        LOG.info("Are they same instance? " + (list1 == list2));
        LOG.info("Are they equal? " + list1.equals(list2));
    }
}