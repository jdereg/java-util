package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test null formatting with the ∅ symbol
 */
class MultiKeyMapNullFormattingTest {

    @Test
    void testNullFormattingInToString() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test simple null key
        map.put(null, "null_key_value");
        
        // Test null in array
        map.put(new Object[]{"key", null, "key2"}, "array_with_null");
        
        // Test null in collection  
        map.put(Arrays.asList("list", null, "item"), "list_with_null");
        
        System.out.println("=== Null Formatting Test ===");
        System.out.println("Map with various null scenarios:");
        System.out.println(map.toString());
        
        // Verify the output contains the ∅ symbol for nulls
        String mapString = map.toString();
        assertTrue(mapString.contains("∅"), "Map toString should contain ∅ symbol for nulls");
        
        System.out.println("\nSimple lookups:");
        System.out.println("Null key lookup: " + map.get(null));
        System.out.println("Array with null lookup: " + map.get(new Object[]{"key", null, "key2"}));
        System.out.println("List with null lookup: " + map.get(Arrays.asList("list", null, "item")));
    }
}