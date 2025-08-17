package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to improve code coverage for MultiKeyMap by exercising uncovered code paths.
 * Targets specific uncovered lines identified through code coverage analysis.
 */
public class MultiKeyMapCoverageImprovementTest {

    @Test
    void testFlattenDimensionsWithComplexArrays() {
        // Test flatten dimensions = true with nested structures to hit expandWithHash paths
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(true)
            .build();

        // Single nested array (hits flattenObjectArray1 -> expandWithHash)
        Object[] singleNested = {new int[]{1, 2}};
        map.put(singleNested, "single");
        assertEquals("single", map.get(singleNested));

        // Two element array with one complex (hits flattenObjectArray2 -> expandWithHash)
        Object[] twoElementComplex = {"simple", new String[]{"nested"}};
        map.put(twoElementComplex, "two");
        assertEquals("two", map.get(twoElementComplex));

        // Three element array with complex (hits flattenObjectArray3 -> expandWithHash)
        Object[] threeElementComplex = {"a", new int[]{1}, "c"};
        map.put(threeElementComplex, "three");
        assertEquals("three", map.get(threeElementComplex));
    }

    @Test
    void testFlattenDimensionsWithComplexCollections() {
        // Test flatten dimensions = true with nested collections
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(true)
            .build();

        // Single collection with nested (hits flattenCollection1 -> expandWithHash)
        List<Object> singleNested = Arrays.asList(Arrays.asList("nested"));
        map.put(singleNested, "single");
        assertEquals("single", map.get(singleNested));

        // Two element collection with RandomAccess (hits flattenCollection2 RandomAccess -> expandWithHash)
        List<Object> twoElementComplex = new ArrayList<>(Arrays.asList("simple", Arrays.asList("nested")));
        map.put(twoElementComplex, "two");
        assertEquals("two", map.get(twoElementComplex));

        // Two element collection without RandomAccess (hits flattenCollection2 non-RandomAccess -> expandWithHash)
        LinkedList<Object> twoElementLinked = new LinkedList<>(Arrays.asList("simple", Arrays.asList("nested")));
        map.put(twoElementLinked, "linked");
        assertEquals("linked", map.get(twoElementLinked));

        // Three element collection with RandomAccess (hits flattenCollection3 RandomAccess -> expandWithHash)
        List<Object> threeElementComplex = new ArrayList<>(Arrays.asList("a", Arrays.asList("nested"), "c"));
        map.put(threeElementComplex, "three");
        assertEquals("three", map.get(threeElementComplex));

        // Three element collection without RandomAccess (hits flattenCollection3 non-RandomAccess -> expandWithHash)
        LinkedList<Object> threeElementLinked = new LinkedList<>(Arrays.asList("a", Arrays.asList("nested"), "c"));
        map.put(threeElementLinked, "linkedThree");
        assertEquals("linkedThree", map.get(threeElementLinked));
    }

    @Test
    void testSimpleKeysModeWithLargerArrays() {
        // Test simpleKeysMode=true with arrays of size 4+ to hit flattenObjectArrayN
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();

        // Test 4+ element arrays (hits flattenObjectArrayN in simpleKeysMode)
        Object[] fourElements = {"a", "b", "c", "d"};
        map.put(fourElements, "four");
        assertEquals("four", map.get(fourElements));

        Object[] fiveElements = {"a", "b", "c", "d", "e"};
        map.put(fiveElements, "five");
        assertEquals("five", map.get(fiveElements));
    }

    @Test
    void testSimpleKeysModeWithLargerCollections() {
        // Test simpleKeysMode=true with collections of size 4+ to hit flattenCollectionN
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(true)
            .build();

        // Test 4+ element collections (hits flattenCollectionN in simpleKeysMode)
        List<String> fourElements = Arrays.asList("a", "b", "c", "d");
        map.put(fourElements, "four");
        assertEquals("four", map.get(fourElements));

        // Test with non-RandomAccess collection
        LinkedList<String> fiveElements = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e"));
        map.put(fiveElements, "five");
        assertEquals("five", map.get(fiveElements));
    }

    @Test
    void testNormalModeWithLargerArraysAndComplexElements() {
        // Test normal mode (not simpleKeysMode) with arrays 4+ that have complex elements
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(false)
            .flattenDimensions(false) // Structure preserving
            .build();

        // Array with 6 elements, one complex - should hit flattenObjectArrayN -> process1DObjectArray
        Object[] sixElements = {"a", "b", "c", "d", "e", new int[]{1, 2}};
        map.put(sixElements, "six");
        assertEquals("six", map.get(sixElements));

        // Array with 8 elements, one complex
        Object[] eightElements = {"a", "b", "c", "d", "e", "f", "g", new String[]{"nested"}};
        map.put(eightElements, "eight");
        assertEquals("eight", map.get(eightElements));
    }

    @Test 
    void testNormalModeWithLargerCollectionsAndComplexElements() {
        // Test normal mode with collections 4+ that have complex elements
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .simpleKeysMode(false)
            .flattenDimensions(false)
            .build();

        // Collection with 6 elements, one complex - hits flattenCollectionN -> process1DCollection
        List<Object> sixElements = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e", Arrays.asList("nested")));
        map.put(sixElements, "six");
        assertEquals("six", map.get(sixElements));

        // Non-RandomAccess collection with complex elements
        LinkedList<Object> eightElements = new LinkedList<>(Arrays.asList("a", "b", "c", "d", "e", "f", "g", new int[]{1}));
        map.put(eightElements, "eight");
        assertEquals("eight", map.get(eightElements));
    }

    @Test
    void testExpandWithHashCyclicReferences() {
        // Test cyclic references to hit the cycle detection code in expandAndHash
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(true)
            .build();

        // Create a cyclic structure
        List<Object> cyclicList = new ArrayList<>();
        cyclicList.add("element");
        cyclicList.add(cyclicList); // Self-reference creates cycle

        map.put(cyclicList, "cyclic");
        assertEquals("cyclic", map.get(cyclicList));

        // Test with cyclic array
        Object[] cyclicArray = new Object[2];
        cyclicArray[0] = "element";
        cyclicArray[1] = cyclicArray; // Self-reference

        map.put(cyclicArray, "cyclicArray");
        assertEquals("cyclicArray", map.get(cyclicArray));
    }

    @Test
    void testExpandWithHashNullElements() {
        // Test null elements to hit null handling in expandAndHash
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(true)
            .build();

        // Array with null elements
        Object[] arrayWithNull = {"a", null, "c"};
        map.put(arrayWithNull, "nullArray");
        assertEquals("nullArray", map.get(arrayWithNull));

        // Collection with null elements
        List<Object> collectionWithNull = Arrays.asList("a", null, "c");
        map.put(collectionWithNull, "nullCollection");
        assertEquals("nullCollection", map.get(collectionWithNull));

        // Test null key itself
        map.put(null, "nullKey");
        assertEquals("nullKey", map.get(null));
    }

    @Test
    void testFlattenVsStructurePreservingModes() {
        // Test both flatten and structure preserving modes with complex data
        MultiKeyMap<String> flattenMap = MultiKeyMap.<String>builder()
            .flattenDimensions(true)
            .build();

        MultiKeyMap<String> structureMap = MultiKeyMap.<String>builder()
            .flattenDimensions(false)
            .build();

        // Complex nested structure
        Object[] nested = {new String[]{"inner"}, "outer"};

        flattenMap.put(nested, "flatten");
        structureMap.put(nested, "structure");

        assertEquals("flatten", flattenMap.get(nested));
        assertEquals("structure", structureMap.get(nested));

        // They should handle the same key differently (one flattens, one preserves structure)
        assertNotNull(flattenMap.get(nested));
        assertNotNull(structureMap.get(nested));
    }

    @Test
    void testLargeArraysWithMixedTypes() {
        // Test arrays > 10 elements to hit the default case in size switches
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Array with 12 elements (hits default case in flattenObjectArrayN routing)
        Object[] largeArray = new Object[12];
        for (int i = 0; i < 12; i++) {
            largeArray[i] = "element" + i;
        }
        map.put(largeArray, "large");
        assertEquals("large", map.get(largeArray));

        // Large array with complex element
        Object[] largeComplexArray = new Object[15];
        for (int i = 0; i < 14; i++) {
            largeComplexArray[i] = "element" + i;
        }
        largeComplexArray[14] = new int[]{1, 2}; // Complex element
        map.put(largeComplexArray, "largeComplex");
        assertEquals("largeComplex", map.get(largeComplexArray));
    }

    @Test
    void testLargeCollectionsWithMixedTypes() {
        // Test collections > 10 elements to hit the default case
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Collection with 12 elements
        List<Object> largeCollection = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            largeCollection.add("element" + i);
        }
        map.put(largeCollection, "large");
        assertEquals("large", map.get(largeCollection));

        // Large collection with complex element
        List<Object> largeComplexCollection = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            largeComplexCollection.add("element" + i);
        }
        largeComplexCollection.add(Arrays.asList("nested")); // Complex element
        map.put(largeComplexCollection, "largeComplex");
        assertEquals("largeComplex", map.get(largeComplexCollection));
    }

    @Test
    void testEstimatedSizeCalculations() {
        // Test different scenarios to exercise size estimation logic in expandWithHash
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(true)
            .build();

        // Large array to test size estimation capping at 64
        Object[] veryLargeArray = new Object[100];
        Arrays.fill(veryLargeArray, "element");
        veryLargeArray[50] = new int[]{1}; // Force expansion
        map.put(veryLargeArray, "veryLarge");
        assertEquals("veryLarge", map.get(veryLargeArray));

        // Large collection to test size estimation
        List<Object> veryLargeCollection = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            veryLargeCollection.add("element" + i);
        }
        veryLargeCollection.set(50, Arrays.asList("nested")); // Force expansion
        map.put(veryLargeCollection, "veryLargeCollection");
        assertEquals("veryLargeCollection", map.get(veryLargeCollection));

        // Test with null key for different size estimation path
        map.put(null, "nullEstimation");
        assertEquals("nullEstimation", map.get(null));
    }

    @Test
    void testNonFlattenWithOpenCloseMarkers() {
        // Test structure preserving mode to hit OPEN/CLOSE marker logic
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(false)
            .build();

        // Nested array structure - should preserve with markers
        Object[] nestedStructure = {
            new Object[]{"level1", new Object[]{"level2"}},
            "top"
        };
        map.put(nestedStructure, "markers");
        assertEquals("markers", map.get(nestedStructure));

        // Nested collection structure
        List<Object> nestedCollectionStructure = Arrays.asList(
            Arrays.asList("level1", Arrays.asList("level2")),
            "top"
        );
        map.put(nestedCollectionStructure, "collectionMarkers");
        assertEquals("collectionMarkers", map.get(nestedCollectionStructure));
    }
}