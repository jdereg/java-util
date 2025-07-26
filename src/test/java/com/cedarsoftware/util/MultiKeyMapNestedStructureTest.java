package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test complex nested structures with Object arrays containing Collections
 * and Collections containing Object arrays, ensuring cross-compatibility.
 */
class MultiKeyMapNestedStructureTest {

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
        
        System.out.println("=== Complex Nested Structure Test ===");
        System.out.println("Map contents after putting array structure:");
        System.out.println(map.toString());
        System.out.println();
        
        // Store second structure - this should OVERWRITE the first since they're equivalent
        String previousValue = map.put(outerCollection, "found_via_collection_structure");
        
        System.out.println("Previous value when putting collection: " + previousValue);
        System.out.println("Map contents after putting collection structure:");
        System.out.println(map.toString());
        System.out.println();
        
        // Verify they map to the same flattened structure (collections and arrays with same content are equivalent)
        String resultFromArray = map.get(outerArray);
        String resultFromCollection = map.get(outerCollection);
        
        System.out.println("Lookup results:");
        System.out.println("Array structure lookup: " + resultFromArray);
        System.out.println("Collection structure lookup: " + resultFromCollection);
        
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
        
        System.out.println("\nCross-compatibility test:");
        System.out.println("Equivalent array lookup: " + crossResultArray);
        System.out.println("Equivalent collection lookup: " + crossResultCollection);
        
        assertEquals("found_via_collection_structure", crossResultArray);
        assertEquals("found_via_collection_structure", crossResultCollection);
        
        // Test individual element access
        System.out.println("\nTesting individual element access:");
        
        // Should not find with just the inner elements
        assertNull(map.get(berriesList1D));
        assertNull(map.get(berries1D));
        
        System.out.println("Individual 1D berries list lookup: " + map.get(berriesList1D));
        System.out.println("Individual 1D berries array lookup: " + map.get(berries1D));
        
        // Verify map size - should be 1 since the structures are equivalent
        assertEquals(1, map.size());
        
        System.out.println("\nFinal map size: " + map.size());
        System.out.println("Test completed successfully!");
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
        
        System.out.println("\n=== Deeply Nested Null Handling Test ===");
        System.out.println("Map with deeply nested nulls:");
        System.out.println(map.toString());
        
        // Verify both structures are equivalent and return the latest value
        assertEquals("3d_list_value", map.get(deep3D));
        assertEquals("3d_list_value", map.get(deepList3D));
        assertEquals("3d_array_value", previousValue);
        
        System.out.println("\nDeep 3D array lookup: " + map.get(deep3D));
        System.out.println("Deep 3D list lookup: " + map.get(deepList3D));
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
        
        System.out.println("\n=== Mixed Type Nested Structures Test ===");
        System.out.println("Map with mixed types and nested structures:");
        System.out.println(map.toString());
        
        // Both should return the latest value since they're equivalent
        assertEquals("mixed_list_value", map.get(mixedArray));
        assertEquals("mixed_list_value", map.get(mixedList));
        assertEquals("mixed_array_value", previousValue);
        
        System.out.println("\nMixed array lookup: " + map.get(mixedArray));
        System.out.println("Mixed list lookup: " + map.get(mixedList));
        
        assertEquals(1, map.size());
    }
}