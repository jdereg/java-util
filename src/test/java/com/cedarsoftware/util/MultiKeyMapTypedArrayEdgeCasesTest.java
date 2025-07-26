package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test edge cases for typed arrays (String[], int[], etc.) and multi-dimensional
 * typed arrays (String[][], int[][], etc.) in MultiKeyMap to verify proper
 * normalization and hash computation.
 */
public class MultiKeyMapTypedArrayEdgeCasesTest {
    
    @Test
    void testTypedArraysVsObjectArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test String[] vs Object[] with same content
        String[] stringArray = {"a", "b", "c"};
        Object[] objectArray = {"a", "b", "c"};
        
        map.put(stringArray, "string_array");
        map.put(objectArray, "object_array"); // This should overwrite since content is the same
        
        // Both should return the same value (last put wins)
        assertEquals("object_array", map.get(stringArray));
        assertEquals("object_array", map.get(objectArray));
        
        // Cross-lookup should work since they have same content
        String[] anotherStringArray = {"a", "b", "c"};
        Object[] anotherObjectArray = {"a", "b", "c"};
        
        assertEquals("object_array", map.get(anotherStringArray));
        assertEquals("object_array", map.get(anotherObjectArray));
        
        // All array types with same content find each other
        assertEquals("object_array", map.get(anotherObjectArray.clone()));
        
        assertEquals(1, map.size()); // Should be only 1 key since content is the same
    }
    
    @Test
    void testMultiDimensionalTypedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test 2D typed arrays
        int[][] int2D = {{1, 2}, {3, 4}};
        String[][] string2D = {{"a", "b"}, {"c", "d"}};
        Object[][] object2D = {{"a", "b"}, {"c", "d"}};
        
        map.put(int2D, "int_2d");
        map.put(string2D, "string_2d");
        map.put(object2D, "object_2d"); // This overwrites string_2d since content is the same
        
        // Verify each can be retrieved
        assertEquals("int_2d", map.get(new int[][]{{1, 2}, {3, 4}}));
        assertEquals("object_2d", map.get(new String[][]{{"a", "b"}, {"c", "d"}})); // Same content as object2D
        assertEquals("object_2d", map.get(new Object[][]{{"a", "b"}, {"c", "d"}}));
        
        // Verify keys exist
        assertTrue(map.containsKey(int2D));
        assertTrue(map.containsKey(string2D)); // Works because content matches object2D
        assertTrue(map.containsKey(object2D));
        
        assertEquals(2, map.size()); // Only 2 unique content patterns: int2D and string2D/object2D
    }
    
    @Test
    void testSingleElementTypedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test single-element optimization with typed arrays
        int[] singleInt = {42};
        String[] singleString = {"hello"};
        Object[] singleObject = {"hello"};
        
        map.put(singleInt, "single_int");
        map.put(singleString, "single_string");  
        map.put(singleObject, "single_object"); // Overwrites single_string
        
        // Single element optimization should extract the element
        assertEquals("single_int", map.get(42));              // Direct int lookup
        assertEquals("single_object", map.get("hello"));      // Direct string lookup - last put wins
        
        // Array lookups should also work
        assertEquals("single_int", map.get(new int[]{42}));
        // Both should return "single_object" since String[] and Object[] with same content are equivalent
        String stringResult = map.get(new String[]{"hello"});
        String objectResult = map.get(new Object[]{"hello"});
        
        assertEquals("single_object", stringResult);
        assertEquals("single_object", objectResult);
        
        assertEquals(2, map.size()); // Two keys: 42 and "hello"
    }
    
    @Test
    void testSingleElementMultiDimensionalTypedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test single-element optimization with 2D arrays containing single elements
        int[][] singleElementInt2D = {{42}};
        String[][] singleElementString2D = {{"hello"}};
        Object[][] singleElementObject2D = {{"hello"}};
        
        map.put(singleElementInt2D, "single_elem_int_2d");
        map.put(singleElementString2D, "single_elem_string_2d");
        map.put(singleElementObject2D, "single_elem_object_2d"); // Overwrites string_2d
        
        // Test what we can retrieve
        assertEquals("single_elem_int_2d", map.get(new int[][]{{42}}));
        assertEquals("single_elem_object_2d", map.get(new String[][]{{"hello"}})); // Same content as object_2d
        assertEquals("single_elem_object_2d", map.get(new Object[][]{{"hello"}}));
        
        // 2D arrays are expanded, not flattened to single elements
        // They maintain their structure and don't collapse to simple values
        assertNull(map.get(new int[]{42}));     // 1D array doesn't match 2D structure
        assertNull(map.get(new String[]{"hello"})); // 1D array doesn't match 2D structure
        assertNull(map.get(42));                // Direct value doesn't match 2D structure
        assertNull(map.get("hello"));           // Direct value doesn't match 2D structure
        
        assertEquals(2, map.size()); // Two different expanded structures
    }
    
    @Test
    void testMixedTypedArrayDimensions() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test arrays with same content but different dimensions
        int[] int1D = {1, 2, 3};
        int[][] int2D = {{1, 2, 3}};
        int[][][] int3D = {{{1, 2, 3}}};
        
        String[] string1D = {"a", "b", "c"};
        String[][] string2D = {{"a", "b", "c"}};
        String[][][] string3D = {{{"a", "b", "c"}}};
        
        map.put(int1D, "int_1d");
        map.put(int2D, "int_2d");
        map.put(int3D, "int_3d");
        map.put(string1D, "string_1d");
        map.put(string2D, "string_2d");
        map.put(string3D, "string_3d");
        
        // All should be retrievable with exact same structure
        assertEquals("int_1d", map.get(new int[]{1, 2, 3}));
        assertEquals("int_2d", map.get(new int[][]{{1, 2, 3}}));
        assertEquals("int_3d", map.get(new int[][][]{{{1, 2, 3}}}));
        assertEquals("string_1d", map.get(new String[]{"a", "b", "c"}));
        assertEquals("string_2d", map.get(new String[][]{{"a", "b", "c"}}));
        assertEquals("string_3d", map.get(new String[][][]{{{"a", "b", "c"}}}));
        
        // Verify the expected behavior
        assertEquals("int_2d", map.get(new int[][]{{1, 2, 3}}));
        
        // If flattening is working correctly, different dimensions should be different keys
        assertEquals(6, map.size());
    }
    
    @Test
    void testTypedArraysInCollections() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test collections containing typed arrays
        int[] intArray = {1, 2, 3};
        String[] stringArray = {"a", "b", "c"};
        Object[] objectArray = {"a", "b", "c"};
        
        List<int[]> listWithIntArray = new ArrayList<>();
        listWithIntArray.add(intArray);
        List<String[]> listWithStringArray = new ArrayList<>();
        listWithStringArray.add(stringArray);
        List<Object[]> listWithObjectArray = new ArrayList<>();
        listWithObjectArray.add(objectArray);
        
        map.put(listWithIntArray, "list_int_array");
        map.put(listWithStringArray, "list_string_array");
        map.put(listWithObjectArray, "list_object_array"); // Overwrites list_string_array
        
        // Should be able to retrieve with equivalent collections
        List<int[]> lookupIntList = new ArrayList<>();
        lookupIntList.add(new int[]{1, 2, 3});
        List<String[]> lookupStringList = new ArrayList<>();
        lookupStringList.add(new String[]{"a", "b", "c"});
        List<Object[]> lookupObjectList = new ArrayList<>();
        lookupObjectList.add(new Object[]{"a", "b", "c"});
        
        assertEquals("list_int_array", map.get(lookupIntList));
        assertEquals("list_object_array", map.get(lookupStringList)); // Same content as objectArray
        assertEquals("list_object_array", map.get(lookupObjectList));
        
        assertEquals(2, map.size()); // Two unique content patterns
    }
    
    @Test
    void testEmptyTypedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test empty arrays of different types
        int[] emptyInt = {};
        String[] emptyString = {};
        Object[] emptyObject = {};
        int[][] empty2DInt = {};
        String[][] empty2DString = {};
        
        map.put(emptyInt, "empty_int");
        map.put(emptyString, "empty_string"); // Overwrites empty_int
        map.put(emptyObject, "empty_object"); // Overwrites empty_string
        map.put(empty2DInt, "empty_2d_int"); // Overwrites empty_object
        map.put(empty2DString, "empty_2d_string"); // Overwrites empty_2d_int
        
        // All empty arrays are equivalent (same content = nothing)
        assertEquals("empty_2d_string", map.get(new int[]{}));     // Last put wins
        assertEquals("empty_2d_string", map.get(new String[]{}));  // Last put wins
        assertEquals("empty_2d_string", map.get(new Object[]{}));  // Last put wins
        assertEquals("empty_2d_string", map.get(new int[][]{}));   // Last put wins
        assertEquals("empty_2d_string", map.get(new String[][]{})); // Last put wins
        
        // Should be 1 key (all empty arrays are equivalent)
        assertEquals(1, map.size());
    }
}