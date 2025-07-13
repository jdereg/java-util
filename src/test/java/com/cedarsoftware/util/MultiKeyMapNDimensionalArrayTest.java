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
        
        // Should be stored as 4D key: "a", "b", "c", "d"
        String result = map.getMultiKey("a", "b", "c", "d");
        assertEquals("value2D", result);
        
        // Verify containsKey
        assertTrue(map.containsMultiKey("a", "b", "c", "d"));
        assertFalse(map.containsMultiKey("a", "b", "c"));
        
        // Verify remove
        assertEquals("value2D", map.removeMultiKey("a", "b", "c", "d"));
        assertNull(map.getMultiKey("a", "b", "c", "d"));
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
        
        // Should expand to: "string", 42, true, 3.14
        String result = map.getMultiKey("string", 42, true, 3.14);
        assertEquals("mixedValue", result);
        
        assertTrue(map.containsMultiKey("string", 42, true, 3.14));
        assertEquals("mixedValue", map.removeMultiKey("string", 42, true, 3.14));
    }

    @Test
    void testArraysWithNullElements() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test arrays containing null elements
        String[][] arrayWithNulls = {{"a", null}, {"b", "c"}};
        map.put(arrayWithNulls, "nullValue");
        
        // Should expand to: "a", null, "b", "c"
        String result = map.getMultiKey("a", null, "b", "c");
        assertEquals("nullValue", result);
        
        assertTrue(map.containsMultiKey("a", null, "b", "c"));
        assertEquals("nullValue", map.removeMultiKey("a", null, "b", "c"));
    }

    @Test
    void testJaggedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test jagged arrays (arrays with different sub-array lengths)
        String[][] jagged = {{"a"}, {"b", "c", "d"}, {"e", "f"}};
        map.put(jagged, "jaggedValue");
        
        // Should expand to: "a", "b", "c", "d", "e", "f"
        String result = map.getMultiKey("a", "b", "c", "d", "e", "f");
        assertEquals("jaggedValue", result);
        
        assertTrue(map.containsMultiKey("a", "b", "c", "d", "e", "f"));
        assertEquals("jaggedValue", map.removeMultiKey("a", "b", "c", "d", "e", "f"));
    }

    @Test
    void testEmptyNestedArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test arrays containing empty sub-arrays
        String[][] withEmpty = {{"a", "b"}, {}, {"c"}};
        map.put(withEmpty, "emptyValue");
        
        // Should expand to: "a", "b", "c" (empty arrays contribute no elements)
        String result = map.getMultiKey("a", "b", "c");
        assertEquals("emptyValue", result);
        
        assertTrue(map.containsMultiKey("a", "b", "c"));
        assertEquals("emptyValue", map.removeMultiKey("a", "b", "c"));
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
        
        // Should expand to: 1, 2, 3, 4, 5
        String result = map.getMultiKey(1, 2, 3, 4, 5);
        assertEquals("intValue", result);
        
        assertTrue(map.containsMultiKey(1, 2, 3, 4, 5));
        assertEquals("intValue", map.removeMultiKey(1, 2, 3, 4, 5));
    }

    @Test
    void testMapInterfaceVsMultiKeyInterface() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test both interfaces work with n-dimensional arrays
        String[][] array = {{"map", "interface"}, {"test"}};
        
        // Store via Map interface
        map.put(array, "mapValue");
        
        // Retrieve via MultiKeyMap interface 
        String result1 = map.getMultiKey("map", "interface", "test");
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
        
        // Test that different ways of representing the same nested structure 
        // result in the same expanded key sequence
        String[][] array1 = {{"a", "b"}, {"c"}};
        String[][] array2 = {{"a"}, {"b", "c"}};
        
        map.put(array1, "value1");
        map.put(array2, "value2");
        
        // Both arrays expand to the same key sequence ["a", "b", "c"]
        // So value2 should overwrite value1
        assertEquals("value2", map.getMultiKey("a", "b", "c"));
        
        // Only one entry should exist
        assertEquals(1, map.size());
        assertEquals("value2", map.getMultiKey("a", "b", "c"));
    }

    @Test
    void testSingleElementArrays() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test arrays that contain single elements when nested
        String[][] singleElements = {{"only"}, {"one"}, {"each"}};
        map.put(singleElements, "singleValue");
        
        // Should expand to: "only", "one", "each"
        assertEquals("singleValue", map.getMultiKey("only", "one", "each"));
        assertTrue(map.containsMultiKey("only", "one", "each"));
        assertEquals("singleValue", map.removeMultiKey("only", "one", "each"));
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

    @Test
    void testArrayExpansionOptimization() {
        MultiKeyMap<String> map = new MultiKeyMap<>();
        
        // Test that flat arrays are returned directly without heap allocations
        Object[] flatArray = {"a", "b", "c", 123, true};
        Object[] expandedFlat = MultiKeyMap.expandMultiDimensionalArray(flatArray);
        
        // Should return the exact same array reference (zero heap allocations!)
        assertSame(flatArray, expandedFlat);
        assertEquals(5, expandedFlat.length);
        assertEquals("a", expandedFlat[0]);
        assertEquals("b", expandedFlat[1]);
        assertEquals("c", expandedFlat[2]);
        assertEquals(123, expandedFlat[3]);
        assertEquals(true, expandedFlat[4]);
        
        // Test that nested arrays still get expanded (with heap allocations as needed)
        Object[] subArray1 = {"x", "y"};
        Object[] subArray2 = {"z"};
        Object[] nestedArray = {subArray1, subArray2};
        Object[] expandedNested = MultiKeyMap.expandMultiDimensionalArray(nestedArray);
        
        // Should return a different array reference (expanded with sentinels)
        assertNotSame(nestedArray, expandedNested);
        assertEquals(8, expandedNested.length); // HAS_SENTINELS + LEVEL_DOWN + x + y + LEVEL_UP + LEVEL_DOWN + z + LEVEL_UP
        assertEquals(MultiKeyMap.HAS_SENTINELS, expandedNested[0]);
        assertEquals(MultiKeyMap.LEVEL_DOWN, expandedNested[1]);
        assertEquals("x", expandedNested[2]);
        assertEquals("y", expandedNested[3]);
        assertEquals(MultiKeyMap.LEVEL_UP, expandedNested[4]);
        assertEquals(MultiKeyMap.LEVEL_DOWN, expandedNested[5]);
        assertEquals("z", expandedNested[6]);
        assertEquals(MultiKeyMap.LEVEL_UP, expandedNested[7]);
        
        // Test that both flat and nested arrays work correctly in MultiKeyMap
        map.put(flatArray, "flatValue");
        map.put(nestedArray, "nestedValue");
        
        assertEquals("flatValue", map.get(flatArray));
        assertEquals("nestedValue", map.get(nestedArray));
        
        // Test edge case: array with null elements (should still be optimized)
        Object[] arrayWithNulls = {"a", null, "c"};
        Object[] expandedWithNulls = MultiKeyMap.expandMultiDimensionalArray(arrayWithNulls);
        assertSame(arrayWithNulls, expandedWithNulls);
        
        // Test typed array optimization: String[] is Object[] so gets zero heap allocation!
        String[] stringArray = {"x", "y", "z"};
        Object[] expandedString = MultiKeyMap.expandMultiDimensionalArray(stringArray);
        
        // String[] instanceof Object[] is true, so should return same reference (zero heap!)
        assertSame(stringArray, expandedString);
        assertEquals(3, expandedString.length);
        assertEquals("x", expandedString[0]);
        assertEquals("y", expandedString[1]);
        assertEquals("z", expandedString[2]);
        
        // Test int[] array optimization: int[] is NOT Object[] so uses minimal conversion
        int[] intArray = {1, 2, 3};
        Object[] expandedInt = MultiKeyMap.expandMultiDimensionalArray(intArray);
        
        // int[] instanceof Object[] is false, so needs conversion but minimal allocation
        assertNotSame(intArray, expandedInt);
        assertEquals(3, expandedInt.length);
        assertEquals(1, expandedInt[0]);
        assertEquals(2, expandedInt[1]);
        assertEquals(3, expandedInt[2]);
        
        // Test that typed arrays work correctly in MultiKeyMap
        map.put(stringArray, "stringValue");
        map.put(intArray, "intValue");
        assertEquals("stringValue", map.getMultiKey("x", "y", "z"));
        assertEquals("intValue", map.getMultiKey(1, 2, 3));
    }
}