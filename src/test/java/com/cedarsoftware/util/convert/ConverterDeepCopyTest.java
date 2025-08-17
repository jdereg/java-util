package com.cedarsoftware.util.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class demonstrating that Converter.convert() creates deep copies of arrays and collections
 * when converting between different but compatible types. This validates the design decision that
 * MultiKeyMap doesn't need built-in defensive copying, since users can easily create deep copies
 * using Converter.convert() from the same java-util library.
 * 
 * <p>Deep copy behavior: Creates new "branches" (container structures) while leaving 
 * "berries" (leaf elements) untouched - no cloning of individual objects.</p>
 * 
 * <p>Note: Converting to the same exact type (e.g., String[] to String[]) returns the same 
 * object for performance reasons. To force duplication, convert to a compatible but different
 * type (e.g., String[] to Object[] and back, or List to ArrayList).</p>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
class ConverterDeepCopyTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        ConverterOptions options = new DefaultConverterOptions();
        converter = new Converter(options);
    }

    @Nested
    @DisplayName("Array Deep Copy Tests")
    class ArrayDeepCopyTests {

        @Test
        @DisplayName("Array-to-array conversion creates independent copy when types differ")
        void testArrayToArrayCreatesIndependentCopy() {
            // Original array
            String[] original = {"apple", "banana", "cherry"};
            
            // Convert to Object[] to force copying, then back to String[]
            Object[] intermediateArray = converter.convert(original, Object[].class);
            String[] copy = converter.convert(intermediateArray, String[].class);
            
            // Verify arrays are equal but not the same object
            assertArrayEquals(original, copy, "Arrays should have identical content");
            assertNotSame(original, copy, "Arrays should be different objects (independent copies)");
            assertNotSame(original, intermediateArray, "Original and intermediate should be different objects");
            
            // Verify berries are untouched (same String objects)
            assertSame(original[0], copy[0], "String objects should be same (berries untouched)");
            
            // Modify original array structure - copy should remain independent
            original[0] = "modified";
            assertEquals("modified", original[0], "Original should be modified");
            assertEquals("apple", copy[0], "Copy should remain unchanged (independent structure)");
        }

        @Test
        @DisplayName("Nested array conversion creates deep copy with new branches")
        void testNestedArrayDeepCopy() {
            // Original nested array
            String[][] original = {
                {"level1-a", "level1-b"}, 
                {"level1-c", "level1-d"}
            };
            
            // Convert to Object[][] to force copying, then back to String[][]
            Object[][] intermediateArray = converter.convert(original, Object[][].class);
            String[][] copy = converter.convert(intermediateArray, String[][].class);
            
            // Verify content is identical
            assertEquals(original.length, copy.length, "Outer arrays should have same length");
            for (int i = 0; i < original.length; i++) {
                assertArrayEquals(original[i], copy[i], "Inner arrays should have identical content");
            }
            
            // Verify structure independence (new branches)
            assertNotSame(original, copy, "Outer arrays should be different objects");
            assertNotSame(original[0], copy[0], "Inner arrays should be different objects (deep copy)");
            assertNotSame(original[1], copy[1], "Inner arrays should be different objects (deep copy)");
            
            // Verify berries are untouched (same String objects)
            assertSame(original[0][0], copy[0][0], "String objects should be same (berries untouched)");
            assertSame(original[1][1], copy[1][1], "String objects should be same (berries untouched)");
            
            // Modify original structure - copy should remain independent
            original[0][0] = "modified";
            assertEquals("modified", original[0][0], "Original should be modified");
            assertEquals("level1-a", copy[0][0], "Copy should remain unchanged (independent structure)");
        }

        @Test
        @DisplayName("Three-dimensional array creates deep copy")
        void testThreeDimensionalArrayDeepCopy() {
            // Original 3D array
            Integer[][][] original = {
                {{1, 2}, {3, 4}},
                {{5, 6}, {7, 8}}
            };
            
            // Convert to Object[][][] to force copying, then back to Integer[][][]
            Object[][][] intermediateArray = converter.convert(original, Object[][][].class);
            Integer[][][] copy = converter.convert(intermediateArray, Integer[][][].class);
            
            // Verify independence at all levels
            assertNotSame(original, copy, "Level 0: Different objects");
            assertNotSame(original[0], copy[0], "Level 1: Different objects (deep copy)");
            assertNotSame(original[0][0], copy[0][0], "Level 2: Different objects (deep copy)");
            
            // Verify berries are untouched (same Integer objects for same values)
            assertSame(original[0][0][0], copy[0][0][0], "Integer objects should be same (berries untouched)");
            
            // Verify content equality
            assertEquals(original[0][0][0], copy[0][0][0], "Content should be identical");
            assertEquals(original[1][1][1], copy[1][1][1], "Content should be identical");
        }

        @Test
        @DisplayName("Array of custom objects - new branches, same berries")
        void testArrayOfCustomObjectsDeepCopy() {
            // Custom objects (our "berries")
            StringBuilder sb1 = new StringBuilder("object1");
            StringBuilder sb2 = new StringBuilder("object2");
            StringBuilder sb3 = new StringBuilder("object3");
            
            // Original array structure (our "branches")
            StringBuilder[] original = {sb1, sb2, sb3};
            
            // Convert to Object[] to force copying, then back to StringBuilder[]
            Object[] intermediateArray = converter.convert(original, Object[].class);
            StringBuilder[] copy = converter.convert(intermediateArray, StringBuilder[].class);
            
            // Verify new branch (different array object)
            assertNotSame(original, copy, "Array structure should be different (new branches)");
            
            // Verify same berries (same StringBuilder objects)
            assertSame(original[0], copy[0], "StringBuilder objects should be same (berries untouched)");
            assertSame(original[1], copy[1], "StringBuilder objects should be same (berries untouched)");
            assertSame(original[2], copy[2], "StringBuilder objects should be same (berries untouched)");
            
            // Modify berry through original reference
            sb1.append("-modified");
            
            // Both arrays should see the change (same berries)
            assertEquals("object1-modified", original[0].toString(), "Original should see berry modification");
            assertEquals("object1-modified", copy[0].toString(), "Copy should see berry modification (same berries)");
        }
    }

    @Nested
    @DisplayName("Collection Deep Copy Tests")
    class CollectionDeepCopyTests {

        @Test
        @DisplayName("List-to-list conversion creates independent copy via different collection types")
        void testListToListCreatesIndependentCopy() {
            // Original list
            List<String> original = new ArrayList<>(Arrays.asList("alpha", "beta", "gamma"));
            
            // Convert to Set (different type) to force copying, then back to List
            Set<String> intermediateSet = converter.convert(original, Set.class);
            List<String> copy = converter.convert(intermediateSet, List.class);
            
            // Verify content (note: Set may reorder, so check contains)
            assertEquals(original.size(), copy.size(), "Lists should have same size");
            assertTrue(copy.containsAll(original), "Copy should contain all original elements");
            assertNotSame(original, copy, "Lists should be different objects (independent copies)");
            
            // Verify berries are untouched (same String objects)
            for (String str : original) {
                assertTrue(copy.contains(str), "Copy should contain original string");
                // Find the same string in copy and verify it's the same object
                for (String copyStr : copy) {
                    if (str.equals(copyStr)) {
                        assertSame(str, copyStr, "String objects should be same (berries untouched)");
                        break;
                    }
                }
            }
            
            // Modify original - copy should remain independent
            original.set(0, "modified");
            assertEquals("modified", original.get(0), "Original should be modified");
            assertFalse(copy.contains("modified"), "Copy should not contain modified element (independent)");
        }

        @Test
        @DisplayName("Set-to-set conversion creates independent copy")
        void testSetToSetCreatesIndependentCopy() {
            // Original set
            Set<String> original = new HashSet<>(Arrays.asList("red", "green", "blue"));
            
            // Convert to LinkedHashSet (different Set type) to force copying
            LinkedHashSet<String> copy = converter.convert(original, LinkedHashSet.class);
            
            // Verify sets have same content but are different objects
            assertEquals(original.size(), copy.size(), "Sets should have same size");
            assertTrue(copy.containsAll(original), "Copy should contain all original elements");
            assertNotSame(original, copy, "Sets should be different objects (independent copies)");
            
            // Modify original - copy should remain unchanged
            original.add("yellow");
            assertTrue(original.contains("yellow"), "Original should contain new element");
            assertFalse(copy.contains("yellow"), "Copy should not contain new element (independent)");
        }

        @Test
        @DisplayName("Nested collection conversion creates deep copy with new branches")
        void testNestedCollectionDeepCopy() {
            // Original nested collection
            List<List<String>> original = new ArrayList<>();
            original.add(new ArrayList<>(Arrays.asList("list1-a", "list1-b")));
            original.add(new ArrayList<>(Arrays.asList("list1-c", "list1-d")));
            
            // Convert to create deep copy
            List<List<String>> copy = converter.convert(original, List.class);
            
            // Verify content is identical
            assertEquals(original.size(), copy.size(), "Outer lists should have same size");
            for (int i = 0; i < original.size(); i++) {
                assertEquals(original.get(i), copy.get(i), "Inner lists should have identical content");
            }
            
            // Verify structure independence (new branches)
            assertNotSame(original, copy, "Outer lists should be different objects");
            assertNotSame(original.get(0), copy.get(0), "Inner lists should be different objects (deep copy)");
            assertNotSame(original.get(1), copy.get(1), "Inner lists should be different objects (deep copy)");
            
            // Verify berries are untouched (same String objects)
            assertSame(original.get(0).get(0), copy.get(0).get(0), "String objects should be same (berries untouched)");
            assertSame(original.get(1).get(1), copy.get(1).get(1), "String objects should be same (berries untouched)");
            
            // Modify original structure - copy should remain independent
            original.get(0).set(0, "modified");
            assertEquals("modified", original.get(0).get(0), "Original should be modified");
            assertEquals("list1-a", copy.get(0).get(0), "Copy should remain unchanged (independent structure)");
        }

        @Test
        @DisplayName("Collection of custom objects - new branches, same berries")
        void testCollectionOfCustomObjectsDeepCopy() {
            // Custom objects (our "berries")
            Map<String, String> map1 = new HashMap<>();
            map1.put("key1", "value1");
            Map<String, String> map2 = new HashMap<>();
            map2.put("key2", "value2");
            
            // Original collection structure (our "branches")
            List<Map<String, String>> original = new ArrayList<>(Arrays.asList(map1, map2));
            
            // Convert to create copy
            List<Map<String, String>> copy = converter.convert(original, List.class);
            
            // Verify new branch (different list object)
            assertNotSame(original, copy, "List structure should be different (new branches)");
            
            // Verify same berries (same Map objects)
            assertSame(original.get(0), copy.get(0), "Map objects should be same (berries untouched)");
            assertSame(original.get(1), copy.get(1), "Map objects should be same (berries untouched)");
            
            // Modify berry through original reference
            map1.put("key1", "modified-value");
            
            // Both collections should see the change (same berries)
            assertEquals("modified-value", original.get(0).get("key1"), "Original should see berry modification");
            assertEquals("modified-value", copy.get(0).get("key1"), "Copy should see berry modification (same berries)");
        }
    }

    @Nested
    @DisplayName("Cross-Container Conversion Tests")
    class CrossContainerConversionTests {

        @Test
        @DisplayName("Array to collection conversion creates independent copy")
        void testArrayToCollectionCreatesIndependentCopy() {
            // Original array
            String[] originalArray = {"one", "two", "three"};
            
            // Convert to collection
            List<String> convertedList = converter.convert(originalArray, List.class);
            
            // Verify content is identical
            assertEquals(originalArray.length, convertedList.size(), "Should have same number of elements");
            for (int i = 0; i < originalArray.length; i++) {
                assertEquals(originalArray[i], convertedList.get(i), "Elements should be identical");
                assertSame(originalArray[i], convertedList.get(i), "String objects should be same (berries untouched)");
            }
            
            // Modify original array - collection should remain independent
            originalArray[0] = "modified";
            assertEquals("modified", originalArray[0], "Original array should be modified");
            assertEquals("one", convertedList.get(0), "Converted list should remain unchanged (independent)");
        }

        @Test
        @DisplayName("Collection to array conversion creates independent copy")
        void testCollectionToArrayCreatesIndependentCopy() {
            // Original collection
            List<String> originalList = new ArrayList<>(Arrays.asList("alpha", "beta", "gamma"));
            
            // Convert to array
            String[] convertedArray = converter.convert(originalList, String[].class);
            
            // Verify content is identical
            assertEquals(originalList.size(), convertedArray.length, "Should have same number of elements");
            for (int i = 0; i < originalList.size(); i++) {
                assertEquals(originalList.get(i), convertedArray[i], "Elements should be identical");
                assertSame(originalList.get(i), convertedArray[i], "String objects should be same (berries untouched)");
            }
            
            // Modify original collection - array should remain independent
            originalList.set(0, "modified");
            assertEquals("modified", originalList.get(0), "Original list should be modified");
            assertEquals("alpha", convertedArray[0], "Converted array should remain unchanged (independent)");
        }

        @Test
        @DisplayName("Mixed nested structures conversion creates new collection structure")
        void testMixedNestedStructuresDeepCopy() {
            // Original: List containing both arrays and collections
            List<Object> original = new ArrayList<>();
            original.add(new String[]{"array-element-1", "array-element-2"});
            original.add(new ArrayList<>(Arrays.asList("list-element-1", "list-element-2")));
            
            // Convert to Set to force copying, then back to List
            Set<Object> intermediateSet = converter.convert(original, Set.class);
            List<Object> copy = converter.convert(intermediateSet, List.class);
            
            // Verify structure independence at the collection level
            assertNotSame(original, copy, "Outer lists should be different objects");
            assertNotSame(original.get(1), copy.get(1), "Inner list should be different object");
            
            // Note: Arrays within collections are just moved, not copied (as expected)
            // This demonstrates that Converter creates new collection structures but doesn't
            // perform universal deep cloning of all nested objects
            String[] originalArray = (String[]) original.get(0);
            String[] copiedArray = (String[]) copy.get(0);
            assertSame(originalArray, copiedArray, "Arrays within collections are moved, not copied");
            
            // Verify berries are untouched at all levels
            assertSame(originalArray[0], copiedArray[0], "Array elements should be same (berries untouched)");
            
            @SuppressWarnings("unchecked")
            List<String> originalInnerList = (List<String>) original.get(1);
            @SuppressWarnings("unchecked")
            List<String> copiedInnerList = (List<String>) copy.get(1);
            assertSame(originalInnerList.get(0), copiedInnerList.get(0), "List elements should be same (berries untouched)");
        }
    }

    @Nested
    @DisplayName("Practical MultiKeyMap Usage Examples")
    class MultiKeyMapUsageExamples {

        @Test
        @DisplayName("Demonstrate safe key modification using Converter.convert()")
        void testSafeKeyModificationPattern() {
            // User has a mutable array they want to use as MultiKeyMap key
            String[] userArray = {"config", "database", "connection"};
            
            // Create defensive copy using Converter.convert() with type conversion to force copying
            Object[] intermediateArray = converter.convert(userArray, Object[].class);
            String[] keyForMap = converter.convert(intermediateArray, String[].class);
            
            // Verify we have independent copies
            assertNotSame(userArray, keyForMap, "Arrays should be different objects");
            assertArrayEquals(userArray, keyForMap, "Arrays should have identical content");
            
            // Verify berries are untouched (same String objects)
            assertSame(userArray[0], keyForMap[0], "String objects should be same (berries untouched)");
            
            // User can safely modify their original array structure
            userArray[0] = "modified-config";
            
            // Key for map remains unchanged (protected structure)
            assertEquals("modified-config", userArray[0], "User's array should be modified");
            assertEquals("config", keyForMap[0], "Map key should remain unchanged (protected structure)");
        }

        @Test
        @DisplayName("Demonstrate collection key protection pattern")
        void testCollectionKeyProtectionPattern() {
            // User has a mutable collection they want to use as MultiKeyMap key
            List<String> userList = new ArrayList<>(Arrays.asList("user", "permissions", "read"));
            
            // Create defensive copy using Converter.convert() before using as key
            List<String> keyForMap = converter.convert(userList, List.class);
            
            // Verify we have independent copies
            assertNotSame(userList, keyForMap, "Lists should be different objects");
            assertEquals(userList, keyForMap, "Lists should have identical content");
            
            // User can safely modify their original collection
            userList.add("write");
            
            // Key for map remains unchanged (protected)
            assertEquals(4, userList.size(), "User's list should have additional element");
            assertEquals(3, keyForMap.size(), "Map key should remain unchanged (protected)");
        }

        @Test
        @DisplayName("Demonstrate nested structure protection pattern requires array conversion")
        void testNestedStructureProtectionPattern() {
            // User has nested mutable structure
            List<String[]> userNestedStructure = new ArrayList<>();
            userNestedStructure.add(new String[]{"path", "to", "resource"});
            userNestedStructure.add(new String[]{"another", "path"});
            
            // Create defensive copy by explicitly converting arrays to ensure copying
            List<Object[]> intermediateList = new ArrayList<>();
            for (String[] array : userNestedStructure) {
                Object[] convertedArray = converter.convert(array, Object[].class);
                intermediateList.add(convertedArray);
            }
            
            // Convert back to original structure
            List<String[]> keyForMap = new ArrayList<>();
            for (Object[] array : intermediateList) {
                String[] convertedArray = converter.convert(array, String[].class);
                keyForMap.add(convertedArray);
            }
            
            // Verify independence
            assertNotSame(userNestedStructure, keyForMap, "Outer lists should be different objects");
            assertNotSame(userNestedStructure.get(0), keyForMap.get(0), "Inner arrays should be different objects");
            
            // Verify same berries (String objects not cloned)
            assertSame(userNestedStructure.get(0)[0], keyForMap.get(0)[0], "String objects should be same (berries untouched)");
            
            // User can safely modify nested structure
            userNestedStructure.get(0)[0] = "modified-path";
            
            // Key for map remains unchanged (protected at structural level)
            assertEquals("modified-path", userNestedStructure.get(0)[0], "User's structure should be modified");
            assertEquals("path", keyForMap.get(0)[0], "Map key should remain unchanged (protected structure)");
        }
    }
}