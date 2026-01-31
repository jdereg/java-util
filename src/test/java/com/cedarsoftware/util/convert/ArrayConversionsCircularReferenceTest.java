package com.cedarsoftware.util.convert;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for circular reference handling in ArrayConversions.
 * Verifies that arrayToArray() and collectionToArray() can handle:
 * - Self-referencing arrays/collections
 * - Deep nesting without stack overflow
 * - Cycles at various levels of nesting
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
class ArrayConversionsCircularReferenceTest {

    private Converter converter;

    @BeforeEach
    void setUp() {
        converter = new Converter(new DefaultConverterOptions());
    }

    @Test
    void testArrayToArray_selfReferenceDirectCycle() {
        // Create an array that contains itself
        Object[] source = new Object[2];
        source[0] = "hello";
        source[1] = source;  // Circular reference

        // Convert Object[] to Object[] (preserve cycle)
        Object[] result = (Object[]) ArrayConversions.arrayToArray(source, Object[].class, converter);

        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals("hello", result[0]);
        assertSame(result, result[1], "Circular reference should be preserved");
    }

    @Test
    void testArrayToArray_nestedArrayWithCycle() {
        // Create nested arrays with a cycle
        Object[][] source = new Object[2][2];
        source[0][0] = "a";
        source[0][1] = "b";
        source[1][0] = "c";
        source[1][1] = source[0];  // Reference to first sub-array (cycle)

        // Convert Object[][] to Object[][]
        Object[][] result = (Object[][]) ArrayConversions.arrayToArray(source, Object[][].class, converter);

        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(2, result[0].length);
        assertEquals(2, result[1].length);
        assertEquals("a", result[0][0]);
        assertEquals("b", result[0][1]);
        assertEquals("c", result[1][0]);
        assertSame(result[0], result[1][1], "Circular reference to sub-array should be preserved");
    }

    @Test
    void testArrayToArray_deeplyNestedArraysNoStackOverflow() {
        // Create a deeply nested array structure (10,000 levels)
        Object current = "leaf";
        for (int i = 0; i < 10_000; i++) {
            Object[] newArray = new Object[1];
            newArray[0] = current;
            current = newArray;
        }

        // Should not cause StackOverflowError
        final Object deeplyNestedArray = current;
        assertDoesNotThrow(() -> {
            ArrayConversions.arrayToArray(deeplyNestedArray, Object[].class, converter);
        });
    }

    @Test
    void testArrayToArray_multipleCyclicReferences() {
        // Create a more complex cycle: A -> B -> C -> A
        Object[] arrayA = new Object[2];
        Object[] arrayB = new Object[2];
        Object[] arrayC = new Object[2];

        arrayA[0] = "A";
        arrayA[1] = arrayB;

        arrayB[0] = "B";
        arrayB[1] = arrayC;

        arrayC[0] = "C";
        arrayC[1] = arrayA;  // Cycle back to A

        // Convert to Object[] (can't be String[] since it contains nested arrays)
        Object[] resultA = (Object[]) ArrayConversions.arrayToArray(arrayA, Object[].class, converter);

        assertNotNull(resultA);
        assertEquals(2, resultA.length);
        assertEquals("A", resultA[0]);

        Object[] resultB = (Object[]) resultA[1];
        assertEquals("B", resultB[0]);

        Object[] resultC = (Object[]) resultB[1];
        assertEquals("C", resultC[0]);

        assertSame(resultA, resultC[1], "Cycle should loop back to first array");
    }

    @Test
    void testCollectionToArray_selfReferenceDirectCycle() {
        // Create a collection of lists, where one list is the collection itself (cycle)
        List<Object> innerList1 = new ArrayList<>();
        innerList1.add("hello");

        List<Object> innerList2 = new ArrayList<>();
        innerList2.add("world");

        List<Object> source = new ArrayList<>();
        source.add(innerList1);
        source.add(innerList2);
        source.add(source);  // Circular reference - list contains itself

        // Convert to Object[][]
        Object[][] result = (Object[][]) ArrayConversions.collectionToArray(source, Object[][].class, converter);

        assertNotNull(result);
        assertEquals(3, result.length);

        // First two elements should be converted inner lists
        assertEquals(1, result[0].length);
        assertEquals("hello", result[0][0]);
        assertEquals(1, result[1].length);
        assertEquals("world", result[1][0]);

        // Third element should be the result array itself (cycle preserved)
        assertSame(result, result[2], "Circular reference should be preserved");
    }

    @Test
    void testCollectionToArray_nestedCollectionWithCycle() {
        // Create properly nested collections with a cycle
        List<Object> innerList1 = new ArrayList<>();
        innerList1.add("inner1");
        innerList1.add("inner2");

        List<Object> innerList2 = new ArrayList<>();
        innerList2.add("other");

        List<Object> outerList = new ArrayList<>();
        outerList.add(innerList1);
        outerList.add(innerList2);

        // Create cycle: innerList1 references back to outerList
        innerList1.add(outerList);

        // Convert to Object[][] (each list becomes an array, strings stay as strings)
        Object[][] result = (Object[][]) ArrayConversions.collectionToArray(outerList, Object[][].class, converter);

        assertNotNull(result);
        assertEquals(2, result.length);

        // Verify first nested array (innerList1 converted)
        Object[] innerArray1 = result[0];
        assertEquals(3, innerArray1.length);
        assertEquals("inner1", innerArray1[0]);
        assertEquals("inner2", innerArray1[1]);

        // Verify cycle: third element of innerArray1 should reference the root result
        assertSame(result, innerArray1[2], "Circular reference back to outer array should be preserved");

        // Verify second nested array (innerList2 converted)
        Object[] innerArray2 = result[1];
        assertEquals(1, innerArray2.length);
        assertEquals("other", innerArray2[0]);
    }

    @Test
    void testCollectionToArray_deeplyNestedCollectionsNoStackOverflow() {
        // Create a deeply nested collection structure (10,000 levels)
        List<Object> current = new ArrayList<>();
        current.add("leaf");

        for (int i = 0; i < 10_000; i++) {
            List<Object> newList = new ArrayList<>();
            newList.add(current);
            current = newList;
        }

        // Should not cause StackOverflowError
        final List<Object> deeplyNestedList = current;
        assertDoesNotThrow(() -> {
            ArrayConversions.collectionToArray(deeplyNestedList, Object[][].class, converter);
        });
    }

    @Test
    void testCollectionToArray_multipleCyclicReferences() {
        // Create a complex cycle: listA -> listB -> listC -> listA
        List<Object> listA = new ArrayList<>();
        List<Object> listB = new ArrayList<>();
        List<Object> listC = new ArrayList<>();

        listA.add("A");
        listA.add(listB);

        listB.add("B");
        listB.add(listC);

        listC.add("C");
        listC.add(listA);  // Cycle back to listA

        // Convert to Object[] (nested lists stay as lists when target is Object[])
        Object[] resultA = (Object[]) ArrayConversions.collectionToArray(listA, Object[].class, converter);

        assertNotNull(resultA);
        assertEquals(2, resultA.length);

        // Verify first element is "A"
        assertEquals("A", resultA[0]);

        // Verify second element is listB (stays as List)
        @SuppressWarnings("unchecked")
        List<Object> listBResult = (List<Object>) resultA[1];
        assertEquals(2, listBResult.size());
        assertEquals("B", listBResult.get(0));

        // Verify listC (stays as List)
        @SuppressWarnings("unchecked")
        List<Object> listCResult = (List<Object>) listBResult.get(1);
        assertEquals(2, listCResult.size());
        assertEquals("C", listCResult.get(0));

        // Verify cycle back to original listA (circular reference preserved)
        assertSame(listA, listCResult.get(1), "Cycle should loop back to first list");
    }

    @Test
    void testArrayToArray_extremeDepthExceedsStackSpace() {
        // Create an extremely deeply nested array structure (100,000 levels)
        // This would definitely cause StackOverflowError with recursive implementation
        // Typical JVM stack can only handle ~1,000-2,000 recursive calls
        Object current = "leaf";
        for (int i = 0; i < 100_000; i++) {
            Object[] newArray = new Object[1];
            newArray[0] = current;
            current = newArray;
        }

        // Should not cause StackOverflowError even at 100k depth
        final Object extremelyDeepArray = current;
        assertDoesNotThrow(() -> {
            Object result = ArrayConversions.arrayToArray(extremelyDeepArray, Object[].class, converter);
            assertNotNull(result, "Should successfully convert extremely deep structure");
        }, "Iterative implementation should handle 100,000 levels without stack overflow");
    }

    @Test
    void testCollectionToArray_extremeDepthExceedsStackSpace() {
        // Create an extremely deeply nested collection structure (100,000 levels)
        // This would definitely cause StackOverflowError with recursive implementation
        List<Object> current = new ArrayList<>();
        current.add("leaf");

        for (int i = 0; i < 100_000; i++) {
            List<Object> newList = new ArrayList<>();
            newList.add(current);
            current = newList;
        }

        // Should not cause StackOverflowError even at 100k depth
        final List<Object> extremelyDeepList = current;
        assertDoesNotThrow(() -> {
            Object result = ArrayConversions.collectionToArray(extremelyDeepList, Object[][].class, converter);
            assertNotNull(result, "Should successfully convert extremely deep structure");
        }, "Iterative implementation should handle 100,000 levels without stack overflow");
    }
}
