package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test null formatting with the ∅ symbol
 */
class MultiKeyMapNullFormattingTest {
    private static final Logger log = Logger.getLogger(MultiKeyMapNullFormattingTest.class.getName());

    @Test
    void testNullFormattingInToString() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test simple null key
        map.put(null, "null_key_value");
        
        // Test null in array
        map.put(new Object[]{"key", null, "key2"}, "array_with_null");
        
        // Test null in collection  
        map.put(Arrays.asList("list", null, "item"), "list_with_null");
        
        log.info("=== Null Formatting Test ===");
        log.info("Map with various null scenarios:");
        log.info(map.toString());
        
        // Verify the output contains the ∅ symbol for nulls
        String mapString = map.toString();
        assertTrue(mapString.contains("∅"), "Map toString should contain ∅ symbol for nulls");
        
        log.info("=== Simple lookups ===");
        log.info("Null key lookup: " + map.get(null));
        log.info("Array with null lookup: " + map.get(new Object[]{"key", null, "key2"}));
        log.info("List with null lookup: " + map.get(Arrays.asList("list", null, "item")));
    }
}