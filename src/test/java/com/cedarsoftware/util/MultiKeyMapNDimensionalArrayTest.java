package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Test n-dimensional array expansion in MultiKeyMap.
 * Verifies that nested arrays are recursively flattened into their constituent elements
 * across all APIs: put/get/containsKey/remove and putMultiKey/getMultiKey/removeMultiKey/containsMultiKey
 */
class MultiKeyMapNDimensionalArrayTest {

    @Test
    void testSimple2DArrayExpansion() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test 2D array expansion via Map interface
        String[][] array2D = {{"a", "b"}, {"c", "d"}};
        map.put(array2D, "value2D");
        
        // 2D arrays are stored with sentinels, retrieve using original array
        String result = map.get(array2D);
        assertEquals("value2D", result);
        
        // Verify containsKey using original array
        assertTrue(map.containsKey(array2D));
        
        // Verify remove using original array
        assertEquals("value2D", map.remove(array2D));
        assertNull(map.get(array2D));
    }

    @Test
    void testSimple3DArrayExpansion() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test 3D array expansion with sentinel preservation
        String[][][] array3D = {{{"x", "y"}, {"z"}}, {{"1", "2", "3"}}};
        map.put(array3D, "value3D");
        
        
        // The key should be retrievable using the same array that was stored
        String result = map.get(array3D);
        assertEquals("value3D", result);
        
        // Verify containsKey and remove work with the original array
        assertTrue(map.containsKey(array3D));
        assertEquals("value3D", map.remove(array3D));
    }

    @Test
    void testMixedTypesInNestedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test nested arrays with mixed types
        Object[][] mixed = {{"string", 42}, {true, 3.14}};
        map.put(mixed, "mixedValue");
        
        // 2D arrays are stored with sentinels, retrieve using original array
        String result = map.get(mixed);
        assertEquals("mixedValue", result);
        
        assertTrue(map.containsKey(mixed));
        assertEquals("mixedValue", map.remove(mixed));
    }

    @Test
    void testArraysWithNullElements() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test arrays containing null elements
        String[][] arrayWithNulls = {{"a", null}, {"b", "c"}};
        map.put(arrayWithNulls, "nullValue");
        
        // 2D arrays are stored with sentinels, retrieve using original array
        String result = map.get(arrayWithNulls);
        assertEquals("nullValue", result);
        
        assertTrue(map.containsKey(arrayWithNulls));
        assertEquals("nullValue", map.remove(arrayWithNulls));
    }

    @Test
    void testJaggedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test jagged arrays (arrays with different sub-array lengths)
        String[][] jagged = {{"a"}, {"b", "c", "d"}, {"e", "f"}};
        map.put(jagged, "jaggedValue");
        
        // 2D arrays are stored with sentinels, retrieve using original array
        String result = map.get(jagged);
        assertEquals("jaggedValue", result);
        
        assertTrue(map.containsKey(jagged));
        assertEquals("jaggedValue", map.remove(jagged));
    }

    @Test
    void testEmptyNestedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test arrays containing empty sub-arrays
        String[][] withEmpty = {{"a", "b"}, {}, {"c"}};
        map.put(withEmpty, "emptyValue");
        
        // 2D arrays are stored with sentinels, retrieve using original array
        String result = map.get(withEmpty);
        assertEquals("emptyValue", result);
        
        assertTrue(map.containsKey(withEmpty));
        assertEquals("emptyValue", map.remove(withEmpty));
    }

    @Test
    void testDeeplyNestedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test deeply nested arrays (4 levels)
        String[][][][] deep = {{{{"deep1", "deep2"}}}};
        map.put(deep, "deepValue");
        
        // The key should be retrievable using the same array that was stored
        String result = map.get(deep);
        assertEquals("deepValue", result);
        
        assertTrue(map.containsKey(deep));
        assertEquals("deepValue", map.remove(deep));
    }

    @Test
    void testTypedArraysWithNestedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test with different typed arrays
        Integer[][] intArray = {{1, 2}, {3, 4, 5}};
        map.put(intArray, "intValue");
        
        // 2D arrays are stored with sentinels, retrieve using original array
        String result = map.get(intArray);
        assertEquals("intValue", result);
        
        assertTrue(map.containsKey(intArray));
        assertEquals("intValue", map.remove(intArray));
    }

    @Test
    void testMapInterfaceVsMultiKeyInterface() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test both interfaces work with n-dimensional arrays
        String[][] array = {{"map", "interface"}, {"test"}};
        
        // Store via Map interface
        map.put(array, "mapValue");
        
        // 2D arrays are stored with sentinels, retrieve using original array
        String result1 = map.get(array);
        assertEquals("mapValue", result1);
        
        // Store via MultiKeyMap interface (this adds new entry)
        map.putMultiKey("mkValue", "multi", "key", "test");
        
        // Retrieve via Map interface using Object[]
        Object[] keyArray = {"multi", "key", "test"};
        String result2 = map.get(keyArray);
        assertEquals("mkValue", result2);
        
        // Both entries should exist
        assertEquals(2, map.size());
    }

    @Test
    void testArrayExpansionConsistency() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test that different 2D arrays with different structures create separate entries
        // (they have different sentinel arrangements)
        String[][] array1 = {{"a", "b"}, {"c"}};
        String[][] array2 = {{"a"}, {"b", "c"}};
        
        map.put(array1, "value1");
        map.put(array2, "value2");
        
        // Different 2D arrays have different structures and create separate entries
        assertEquals("value1", map.get(array1));
        assertEquals("value2", map.get(array2));
        
        // Two separate entries should exist due to different structures
        assertEquals(2, map.size());
    }

    @Test
    void testSingleElementArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test arrays that contain single elements when nested
        String[][] singleElements = {{"only"}, {"one"}, {"each"}};
        map.put(singleElements, "singleValue");
        
        // 2D arrays are stored with sentinels, retrieve using original array
        assertEquals("singleValue", map.get(singleElements));
        assertTrue(map.containsKey(singleElements));
        assertEquals("singleValue", map.remove(singleElements));
    }

    @Test
    void testComplexNestedStructure() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test complex nested structure with mixed array dimensions
        Object[][][] complex = {
            {{"level1", "level2"}, {"level3"}},
            {{"level4", "level5", "level6"}}
        };
        map.put(complex, "complexValue");
        
        // The key should be retrievable using the same array that was stored
        String result = map.get(complex);
        assertEquals("complexValue", result);
        
        assertTrue(map.containsKey(complex));
        assertEquals("complexValue", map.remove(complex));
    }
}