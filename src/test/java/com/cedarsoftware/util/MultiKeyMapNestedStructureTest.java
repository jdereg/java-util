package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test complex nested structures with Object arrays containing Collections
 * and Collections containing Object arrays, ensuring cross-compatibility.
 */
class MultiKeyMapNestedStructureTest {
    private static final Logger log = Logger.getLogger(MultiKeyMapNestedStructureTest.class.getName());

    @Test
    void testObjectArrayWithCollectionVsCollectionWithObjectArray() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create berries data with null and string elements
        String[] berries1D = {"strawberry", null, "blueberry"};
        String[][] berries2D = {{"raspberry", "blackberry"}, {null, "cranberry"}};
        
        // Scenario 1: Object array containing Collections
        List<String> berriesList1D = Arrays.asList("strawberry", null, "blueberry");
        List<List<String>> berriesList2D = Arrays.asList(
            Arrays.asList("raspberry", "blackberry"),
            Arrays.asList(null, "cranberry")
        );
        
        Object[] outerArray = {berriesList1D, "middle_string", berriesList2D, null};
        
        // Scenario 2: Collection containing Object arrays (same berries)
        List<Object> outerCollection = Arrays.asList(berries1D, "middle_string", berries2D, null);
        
        // Store first structure - this will set the value
        map.put(outerArray, "found_via_array_structure");
        
        log.info("=== Complex Nested Structure Test ===");
        log.info("Map contents after putting array structure:");
        log.info(map.toString());
        log.info("");
        
        // Store second structure - this should OVERWRITE the first since they're equivalent
        String previousValue = map.put(outerCollection, "found_via_collection_structure");
        
        log.info("Previous value when putting collection: " + previousValue);
        log.info("Map contents after putting collection structure:");
        log.info(map.toString());
        log.info("");
        
        // Verify they map to the same flattened structure (collections and arrays with same content are equivalent)
        String resultFromArray = map.get(outerArray);
        String resultFromCollection = map.get(outerCollection);
        
        log.info("Lookup results:");
        log.info("Array structure lookup: " + resultFromArray);
        log.info("Collection structure lookup: " + resultFromCollection);
        
        // They should both return the same value since they're equivalent keys
        assertEquals("found_via_collection_structure", resultFromArray);
        assertEquals("found_via_collection_structure", resultFromCollection);
        assertEquals("found_via_array_structure", previousValue); // The overwritten value
        
        // Test cross-compatibility - create equivalent keys with different container types
        Object[] equivalentArray = {
            Arrays.asList("strawberry", null, "blueberry"), 
            "middle_string", 
            Arrays.asList(
                Arrays.asList("raspberry", "blackberry"),
                Arrays.asList(null, "cranberry")
            ), 
            null
        };
        
        List<Object> equivalentCollection = Arrays.asList(
            new String[]{"strawberry", null, "blueberry"},
            "middle_string",
            new String[][]{{"raspberry", "blackberry"}, {null, "cranberry"}},
            null
        );
        
        // These should find the original values since the content is equivalent
        String crossResultArray = map.get(equivalentArray);
        String crossResultCollection = map.get(equivalentCollection);
        
        log.info("\nCross-compatibility test:");
        log.info("Equivalent array lookup: " + crossResultArray);
        log.info("Equivalent collection lookup: " + crossResultCollection);
        
        assertEquals("found_via_collection_structure", crossResultArray);
        assertEquals("found_via_collection_structure", crossResultCollection);
        
        // Test individual element access
        log.info("\nTesting individual element access:");
        
        // Should not find with just the inner elements
        assertNull(map.get(berriesList1D));
        assertNull(map.get(berries1D));
        
        log.info("Individual 1D berries list lookup: " + map.get(berriesList1D));
        log.info("Individual 1D berries array lookup: " + map.get(berries1D));
        
        // Verify map size - should be 1 since the structures are equivalent
        assertEquals(1, map.size());
        
        log.info("\nFinal map size: " + map.size());
        log.info("Test completed successfully!");
    }
    
    @Test
    void testDeeplyNestedNullHandling() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create deeply nested structure with nulls at various levels
        Object[][][] deep3D = {
            {{null, "level3"}, {"more", null}},
            {{null, null}, {null, "deep"}}
        };
        
        List<List<List<String>>> deepList3D = Arrays.asList(
            Arrays.asList(
                Arrays.asList(null, "level3"),
                Arrays.asList("more", null)
            ),
            Arrays.asList(
                Arrays.asList(null, null),
                Arrays.asList(null, "deep")
            )
        );
        
        map.put(deep3D, "3d_array_value");
        String previousValue = map.put(deepList3D, "3d_list_value");
        
        log.info("\n=== Deeply Nested Null Handling Test ===");
        log.info("Map with deeply nested nulls:");
        log.info(map.toString());
        
        // Verify both structures are equivalent and return the latest value
        assertEquals("3d_list_value", map.get(deep3D));
        assertEquals("3d_list_value", map.get(deepList3D));
        assertEquals("3d_array_value", previousValue);
        
        log.info("\nDeep 3D array lookup: " + map.get(deep3D));
        log.info("Deep 3D list lookup: " + map.get(deepList3D));
    }
    
    @Test
    void testMixedTypeNestedStructures() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Create mixed type structures
        Object[] mixedArray = {
            42,                                    // Integer
            Arrays.asList("a", null, "c"),        // List<String>
            new int[]{1, 2, 3},                   // int[]
            null,                                 // null
            new Object[]{"nested", null, 99}      // Object[]
        };
        
        List<Object> mixedList = Arrays.asList(
            42,                                    // Integer  
            new String[]{"a", null, "c"},         // String[]
            Arrays.asList(1, 2, 3),               // List<Integer>
            null,                                 // null
            Arrays.asList("nested", null, 99)     // List<Object>
        );
        
        map.put(mixedArray, "mixed_array_value");
        String previousValue = map.put(mixedList, "mixed_list_value");
        
        log.info("\n=== Mixed Type Nested Structures Test ===");
        log.info("Map with mixed types and nested structures:");
        log.info(map.toString());
        
        // Both should return the latest value since they're equivalent
        assertEquals("mixed_list_value", map.get(mixedArray));
        assertEquals("mixed_list_value", map.get(mixedList));
        assertEquals("mixed_array_value", previousValue);
        
        log.info("\nMixed array lookup: " + map.get(mixedArray));
        log.info("Mixed list lookup: " + map.get(mixedList));
        
        assertEquals(1, map.size());
    }
}