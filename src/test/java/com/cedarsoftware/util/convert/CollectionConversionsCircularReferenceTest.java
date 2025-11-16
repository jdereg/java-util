package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for circular reference handling in CollectionConversions.
 * Verifies that arrayToCollection() and collectionToCollection() can handle:
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
class CollectionConversionsCircularReferenceTest {

    @Test
    void testArrayToCollection_selfReferenceDirectCycle() {
        // Create an array that contains itself
        Object[] source = new Object[2];
        source[0] = "hello";
        source[1] = source;  // Circular reference

        // Convert to List
        @SuppressWarnings("unchecked")
        List<Object> result = CollectionConversions.arrayToCollection(source, List.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("hello", result.get(0));
        assertSame(result, result.get(1), "Circular reference should be preserved");
    }

    @Test
    void testArrayToCollection_nestedArrayWithCycle() {
        // Create nested arrays with a cycle
        Object[][] source = new Object[2][2];
        source[0][0] = "a";
        source[0][1] = "b";
        source[1][0] = "c";
        source[1][1] = source[0];  // Reference to first sub-array (cycle)

        // Convert to List
        @SuppressWarnings("unchecked")
        List<List<Object>> result = CollectionConversions.arrayToCollection(source, List.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).size());
        assertEquals(2, result.get(1).size());
        assertEquals("a", result.get(0).get(0));
        assertEquals("b", result.get(0).get(1));
        assertEquals("c", result.get(1).get(0));
        assertSame(result.get(0), result.get(1).get(1), "Circular reference to sub-list should be preserved");
    }

    @Test
    void testArrayToCollection_deeplyNestedArraysNoStackOverflow() {
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
            CollectionConversions.arrayToCollection(deeplyNestedArray, List.class);
        });
    }

    @Test
    void testArrayToCollection_multipleCyclicReferences() {
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

        // Convert to List
        @SuppressWarnings("unchecked")
        List<Object> resultA = CollectionConversions.arrayToCollection(arrayA, List.class);

        assertNotNull(resultA);
        assertEquals(2, resultA.size());
        assertEquals("A", resultA.get(0));

        @SuppressWarnings("unchecked")
        List<Object> listB = (List<Object>) resultA.get(1);
        assertEquals("B", listB.get(0));

        @SuppressWarnings("unchecked")
        List<Object> listC = (List<Object>) listB.get(1);
        assertEquals("C", listC.get(0));

        assertSame(resultA, listC.get(1), "Cycle should loop back to first list");
    }

    @Test
    void testCollectionToCollection_selfReferenceDirectCycle() {
        // Create a list that contains itself
        List<Object> source = new ArrayList<>();
        source.add("hello");
        source.add(source);  // Circular reference - list contains itself

        // Convert to another List (Sets don't support circular references due to hashCode())
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) CollectionConversions.collectionToCollection(source, ArrayList.class);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("hello", result.get(0));
        assertSame(result, result.get(1), "Circular reference should be preserved");
    }

    @Test
    void testCollectionToCollection_nestedCollectionWithCycle() {
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

        // Convert to ArrayList (Sets don't support circular references due to hashCode())
        @SuppressWarnings("unchecked")
        List<Object> result = (List<Object>) CollectionConversions.collectionToCollection(outerList, ArrayList.class);

        assertNotNull(result);
        assertEquals(2, result.size());

        // Verify first nested list (innerList1 converted)
        @SuppressWarnings("unchecked")
        List<Object> innerResult1 = (List<Object>) result.get(0);
        assertEquals(3, innerResult1.size());
        assertEquals("inner1", innerResult1.get(0));
        assertEquals("inner2", innerResult1.get(1));

        // Verify cycle: third element of innerResult1 should reference the root result
        assertSame(result, innerResult1.get(2), "Circular reference back to outer list should be preserved");

        // Verify second nested list (innerList2 converted)
        @SuppressWarnings("unchecked")
        List<Object> innerResult2 = (List<Object>) result.get(1);
        assertEquals(1, innerResult2.size());
        assertEquals("other", innerResult2.get(0));
    }

    @Test
    void testCollectionToCollection_deeplyNestedCollectionsNoStackOverflow() {
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
            CollectionConversions.collectionToCollection(deeplyNestedList, ArrayList.class);
        });
    }

    @Test
    void testCollectionToCollection_multipleCyclicReferences() {
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

        // Convert to ArrayList (Sets don't support circular references due to hashCode())
        @SuppressWarnings("unchecked")
        List<Object> resultA = (List<Object>) CollectionConversions.collectionToCollection(listA, ArrayList.class);

        assertNotNull(resultA);
        assertEquals(2, resultA.size());

        // Verify first element is "A"
        assertEquals("A", resultA.get(0));

        // Verify second element is listB converted
        @SuppressWarnings("unchecked")
        List<Object> resultB = (List<Object>) resultA.get(1);
        assertEquals(2, resultB.size());
        assertEquals("B", resultB.get(0));

        // Verify listC converted
        @SuppressWarnings("unchecked")
        List<Object> resultC = (List<Object>) resultB.get(1);
        assertEquals(2, resultC.size());
        assertEquals("C", resultC.get(0));

        // Verify cycle back to listA
        assertSame(resultA, resultC.get(1), "Cycle should loop back to first list");
    }

    @Test
    void testArrayToCollection_extremeDepthExceedsStackSpace() {
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
            List<?> result = CollectionConversions.arrayToCollection(extremelyDeepArray, List.class);
            assertNotNull(result, "Should successfully convert extremely deep structure");
        }, "Iterative implementation should handle 100,000 levels without stack overflow");
    }

    @Test
    void testCollectionToCollection_extremeDepthExceedsStackSpace() {
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
            Object result = CollectionConversions.collectionToCollection(extremelyDeepList, ArrayList.class);
            assertNotNull(result, "Should successfully convert extremely deep structure");
        }, "Iterative implementation should handle 100,000 levels without stack overflow");
    }
}
