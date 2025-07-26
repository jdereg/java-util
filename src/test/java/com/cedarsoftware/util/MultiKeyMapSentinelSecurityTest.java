package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that user-provided strings matching sentinel values don't cause key collisions.
 * This ensures the security fix using custom sentinel objects is working correctly.
 */
class MultiKeyMapSentinelSecurityTest {
    private static final Logger log = Logger.getLogger(MultiKeyMapSentinelSecurityTest.class.getName());

    @Test
    void testUserStringsDontCollidWithSentinels() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create a nested key that will generate sentinels internally: [["a"]]
        Object[] nestedArray = {new String[]{"a"}};
        map.put(nestedArray, "nested_value");
        
        // Create a flat key with strings that match the sentinel string values
        Object[] flatKeyWithSentinelStrings = {"[", "a", "]"};
        map.put(flatKeyWithSentinelStrings, "flat_value");
        
        // These should be different keys and not collide
        String nestedResult = map.get(nestedArray);
        String flatResult = map.get(flatKeyWithSentinelStrings);
        
        log.info("=== Sentinel Security Test ===");
        log.info("Map contents:");
        log.info(map.toString());
        log.info("Nested array lookup: " + nestedResult);
        log.info("Flat key with sentinel strings lookup: " + flatResult);
        
        // Verify they don't collide
        assertEquals("nested_value", nestedResult);
        assertEquals("flat_value", flatResult);
        assertEquals(2, map.size()); // Should have 2 distinct entries
        
        // Test with ∅ string as well
        map.put(new Object[]{"key", "∅", "key2"}, "empty_symbol_value");
        map.put(new Object[]{"key", null, "key2"}, "actual_null_value");
        
        String emptySymbolResult = map.get(new Object[]{"key", "∅", "key2"});
        String actualNullResult = map.get(new Object[]{"key", null, "key2"});
        
        log.info("Empty symbol string lookup: " + emptySymbolResult);
        log.info("Actual null lookup: " + actualNullResult);
        
        // These should also be different
        assertEquals("empty_symbol_value", emptySymbolResult);
        assertEquals("actual_null_value", actualNullResult);
        assertEquals(4, map.size()); // Should now have 4 distinct entries
    }
    
    @Test
    void testComplexSentinelCollisionPrevention() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create nested collection structure that will generate sentinels
        List<String> nestedList = Arrays.asList("nested", "content");
        Object[] complexKey = {nestedList, "middle"};
        map.put(complexKey, "complex_nested_value");
        
        // Create flat structure with sentinel-like strings
        Object[] flatWithSentinels = {"[", "nested", "content", "]", "middle"};
        map.put(flatWithSentinels, "flat_with_sentinels_value");
        
        // Another variation with different arrangement
        Object[] anotherFlat = {"[", "nested", "]", "[", "content", "]", "middle"};
        map.put(anotherFlat, "another_flat_value");
        
        log.info("=== Complex Sentinel Collision Test ===");
        log.info("Map contents:");
        log.info(map.toString());
        
        // All should be distinct
        assertEquals("complex_nested_value", map.get(complexKey));
        assertEquals("flat_with_sentinels_value", map.get(flatWithSentinels));
        assertEquals("another_flat_value", map.get(anotherFlat));
        assertEquals(3, map.size());
        
        log.info("Complex nested lookup: " + map.get(complexKey));
        log.info("Flat with sentinels lookup: " + map.get(flatWithSentinels));
        log.info("Another flat lookup: " + map.get(anotherFlat));
        log.info("Final map size: " + map.size());
    }
}