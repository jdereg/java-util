package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test enhanced nested structure display with proper bracket notation
 */
class MultiKeyMapNestedStructureDisplayTest {
    private static final Logger log = Logger.getLogger(MultiKeyMapNestedStructureDisplayTest.class.getName());

    @Test
    void testNestedArrayKeyDisplay() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create nested array structure: [[a, null, b], middle, [x, y], null]
        Object[] innerArray1 = {"a", null, "b"};
        Object[] innerArray2 = {"x", "y"};
        Object[] nestedKey = {innerArray1, "middle", innerArray2, null};
        
        map.put(nestedKey, "found_via_nested_array");
        
        String result = map.toString();
        log.info("Nested array display:");
        log.info(result);
        
        // Should show proper nested structure with brackets
        assertTrue(result.contains("🆔 [[a, ∅, b], middle, [x, y], ∅]"));
        assertTrue(result.contains("🟣 found_via_nested_array"));
    }

    @Test
    void testNestedCollectionKeyDisplay() {
        MultiKeyMap<Object> map = new MultiKeyMap<>();
        
        // Create nested collection structure
        List<Object> innerList1 = Arrays.asList("a", null, "b");
        List<Object> innerList2 = Arrays.asList("x", "y");
        List<Object> nestedKey = Arrays.asList(innerList1, "middle", innerList2, null);
        
        // Use the complex nested structure as both key AND value
        map.put(nestedKey, nestedKey);
        
        String result = map.toString();
        log.info("Nested collection display (complex key as both key and value):");
        log.info(result);
        
        // Should show proper nested structure with brackets in both key and value positions
        assertTrue(result.contains("🆔 [[a, ∅, b], middle, [x, y], ∅]"));
        assertTrue(result.contains("🟣 [[a, ∅, b], middle, [x, y], ∅]"));
    }

    @Test
    void testMixedNestedStructureDisplay() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Mix of arrays and collections
        Object[] innerArray = {"array_elem1", "array_elem2"};
        List<String> innerList = Arrays.asList("list_elem1", "list_elem2");
        Object[] mixedKey = {innerArray, "separator", innerList};
        
        map.put(mixedKey, "mixed_structure_value");
        
        String result = map.toString();
        log.info("Mixed structure display:");
        log.info(result);
        
        // Should show proper nested structure
        assertTrue(result.contains("🆔 [[array_elem1, array_elem2], separator, [list_elem1, list_elem2]]"));
        assertTrue(result.contains("🟣 mixed_structure_value"));
    }

    @Test
    void testEmptyNestedStructureDisplay() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Nested structure with empty arrays/collections
        Object[] emptyArray = {};
        List<Object> emptyList = new ArrayList<>();
        Object[] keyWithEmpties = {emptyArray, "middle", emptyList};
        
        map.put(keyWithEmpties, "has_empties");
        
        String result = map.toString();
        log.info("Empty nested structure display:");
        log.info(result);
        
        // Should show empty brackets for empty structures
        assertTrue(result.contains("🆔 [[], middle, []]"));
        assertTrue(result.contains("🟣 has_empties"));
    }

    @Test
    void testDeeplyNestedStructureDisplay() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create deeply nested structure: [[[deep]]]
        Object[] deepest = {"deep"};
        Object[] middle = {deepest};
        Object[] outermost = {middle};
        
        map.put(outermost, "deeply_nested");
        
        String result = map.toString();
        log.info("Deeply nested structure display:");
        log.info(result);
        
        // Should show proper nesting levels
        assertTrue(result.contains("🆔 [[[deep]]]"));
        assertTrue(result.contains("🟣 deeply_nested"));
    }
}