package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Additional tests to cover specific uncovered lines identified in code coverage analysis.
 * Focuses on non-RandomAccess collection paths and specific condition branches.
 */
public class MultiKeyMapAdditionalCoverageTest {

    @Test
    void testFlattenCollection2NonRandomAccessWithComplexElements() {
        // Test flattenCollection2 non-RandomAccess path with complex elements
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(true) // This will trigger expandWithHash calls
            .build();

        // Create a non-RandomAccess collection with 2 elements, one complex
        // This should hit lines 952-960 (Non-RandomAccess path) and then
        // the expandWithHash call on line 958
        LinkedList<Object> nonRandomAccess = new LinkedList<>();
        nonRandomAccess.add("simple");
        nonRandomAccess.add(new String[]{"complex"}); // Complex element

        map.put(nonRandomAccess, "nonRandomComplex");
        assertEquals("nonRandomComplex", map.get(nonRandomAccess));
    }

    @Test
    void testFlattenCollection2NonRandomAccessWithoutComplexElements() {
        // Test flattenCollection2 non-RandomAccess path without complex elements
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Create a non-RandomAccess collection with 2 simple elements
        // This should hit lines 952-964 (Non-RandomAccess path) and compute hash
        LinkedList<String> nonRandomAccess = new LinkedList<>();
        nonRandomAccess.add("first");
        nonRandomAccess.add("second");

        map.put(nonRandomAccess, "nonRandomSimple");
        assertEquals("nonRandomSimple", map.get(nonRandomAccess));
    }

    @Test
    void testFlattenCollection3NonRandomAccessWithComplexElements() {
        // Test flattenCollection3 non-RandomAccess path with complex elements
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(true) // This will trigger expandWithHash calls
            .build();

        // Create a non-RandomAccess collection with 3 elements, one complex
        // This should hit lines 985-997 (Non-RandomAccess path) and then
        // the expandWithHash call on line 995
        LinkedList<Object> nonRandomAccess = new LinkedList<>();
        nonRandomAccess.add("simple1");
        nonRandomAccess.add(Arrays.asList("complex")); // Complex element
        nonRandomAccess.add("simple3");

        map.put(nonRandomAccess, "nonRandom3Complex");
        assertEquals("nonRandom3Complex", map.get(nonRandomAccess));
    }

    @Test
    void testFlattenCollection3NonRandomAccessWithoutComplexElements() {
        // Test flattenCollection3 non-RandomAccess path without complex elements  
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Create a non-RandomAccess collection with 3 simple elements
        // This should hit lines 985-1002 (Non-RandomAccess path) and compute hash
        LinkedList<String> nonRandomAccess = new LinkedList<>();
        nonRandomAccess.add("first");
        nonRandomAccess.add("second");
        nonRandomAccess.add("third");

        map.put(nonRandomAccess, "nonRandom3Simple");
        assertEquals("nonRandom3Simple", map.get(nonRandomAccess));
    }

    @Test
    void testFlattenCollection2StructurePreservingModeWithComplexElements() {
        // Test flattenCollection2 with flattenDimensions=false and complex elements
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(false) // Structure preserving mode
            .build();

        // RandomAccess path with complex elements - should call process1DCollection
        List<Object> randomAccessComplex = new ArrayList<>();
        randomAccessComplex.add("simple");
        randomAccessComplex.add(new int[]{1, 2}); // Complex element

        map.put(randomAccessComplex, "randomComplex");
        assertEquals("randomComplex", map.get(randomAccessComplex));

        // Non-RandomAccess path with complex elements - should also call process1DCollection
        LinkedList<Object> nonRandomAccessComplex = new LinkedList<>();
        nonRandomAccessComplex.add("simple");
        nonRandomAccessComplex.add(new String[]{"nested"}); // Complex element

        map.put(nonRandomAccessComplex, "nonRandomComplex");
        assertEquals("nonRandomComplex", map.get(nonRandomAccessComplex));
    }

    @Test
    void testFlattenCollection3StructurePreservingModeWithComplexElements() {
        // Test flattenCollection3 with flattenDimensions=false and complex elements
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(false) // Structure preserving mode
            .build();

        // RandomAccess path with complex elements - should call process1DCollection
        List<Object> randomAccessComplex = new ArrayList<>();
        randomAccessComplex.add("simple1");
        randomAccessComplex.add(Arrays.asList("complex")); // Complex element
        randomAccessComplex.add("simple3");

        map.put(randomAccessComplex, "random3Complex");
        assertEquals("random3Complex", map.get(randomAccessComplex));

        // Non-RandomAccess path with complex elements - should also call process1DCollection
        LinkedList<Object> nonRandomAccessComplex = new LinkedList<>();
        nonRandomAccessComplex.add("simple1");
        nonRandomAccessComplex.add(new double[]{1.0, 2.0}); // Complex element  
        nonRandomAccessComplex.add("simple3");

        map.put(nonRandomAccessComplex, "nonRandom3Complex");
        assertEquals("nonRandom3Complex", map.get(nonRandomAccessComplex));
    }

    @Test
    void testSpecificComplexElementPositions() {
        // Test collections where complex elements are in different positions
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(true)
            .build();

        // Collection2 with complex element in first position
        LinkedList<Object> complexFirst = new LinkedList<>();
        complexFirst.add(new String[]{"complex"});
        complexFirst.add("simple");

        map.put(complexFirst, "complexFirst2");
        assertEquals("complexFirst2", map.get(complexFirst));

        // Collection3 with complex element in first position
        LinkedList<Object> complexFirst3 = new LinkedList<>();
        complexFirst3.add(new int[]{1});
        complexFirst3.add("simple2");
        complexFirst3.add("simple3");

        map.put(complexFirst3, "complexFirst3");
        assertEquals("complexFirst3", map.get(complexFirst3));

        // Collection3 with complex element in second position
        LinkedList<Object> complexSecond3 = new LinkedList<>();
        complexSecond3.add("simple1");
        complexSecond3.add(Arrays.asList("complex"));
        complexSecond3.add("simple3");

        map.put(complexSecond3, "complexSecond3");
        assertEquals("complexSecond3", map.get(complexSecond3));

        // Collection3 with complex element in third position
        LinkedList<Object> complexThird3 = new LinkedList<>();
        complexThird3.add("simple1");
        complexThird3.add("simple2");
        complexThird3.add(new boolean[]{true});

        map.put(complexThird3, "complexThird3");
        assertEquals("complexThird3", map.get(complexThird3));
    }

    @Test
    void testMultipleComplexElementsInCollection() {
        // Test collections with multiple complex elements
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .flattenDimensions(false) // Structure preserving
            .build();

        // Collection2 with both elements complex
        LinkedList<Object> bothComplex2 = new LinkedList<>();
        bothComplex2.add(new String[]{"complex1"});
        bothComplex2.add(Arrays.asList("complex2"));

        map.put(bothComplex2, "bothComplex2");
        assertEquals("bothComplex2", map.get(bothComplex2));

        // Collection3 with all elements complex
        LinkedList<Object> allComplex3 = new LinkedList<>();
        allComplex3.add(new int[]{1});
        allComplex3.add(new double[]{2.0});
        allComplex3.add(Arrays.asList("complex3"));

        map.put(allComplex3, "allComplex3");
        assertEquals("allComplex3", map.get(allComplex3));
    }

    @Test
    void testEdgeCaseCollections() {
        // Test various edge cases for collection handling
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // LinkedHashSet (non-RandomAccess) with 2 elements
        LinkedHashSet<String> linkedSet2 = new LinkedHashSet<>();
        linkedSet2.add("first");
        linkedSet2.add("second");

        map.put(linkedSet2, "linkedSet2");
        assertEquals("linkedSet2", map.get(linkedSet2));

        // LinkedHashSet with 3 elements
        LinkedHashSet<String> linkedSet3 = new LinkedHashSet<>();
        linkedSet3.add("first");
        linkedSet3.add("second");
        linkedSet3.add("third");

        map.put(linkedSet3, "linkedSet3");
        assertEquals("linkedSet3", map.get(linkedSet3));

        // TreeSet (non-RandomAccess) with complex element
        TreeSet<Object> treeSetComplex = new TreeSet<>((a, b) -> Objects.toString(a).compareTo(Objects.toString(b)));
        treeSetComplex.add("simple");
        // Note: Can't add arrays to TreeSet easily due to comparison, so test with string representation
        treeSetComplex.add("[complex]"); // String that looks like array

        map.put(treeSetComplex, "treeSetComplex");
        assertEquals("treeSetComplex", map.get(treeSetComplex));
    }

    @Test
    void testRandomAccessVsNonRandomAccessBehaviorDifferences() {
        // Verify that RandomAccess and non-RandomAccess paths work correctly
        MultiKeyMap<String> map = new MultiKeyMap<>();

        // Test with different data so they don't collide as equivalent keys
        // RandomAccess collection (ArrayList)
        List<String> randomAccessList = new ArrayList<>(Arrays.asList("rand1", "rand2"));
        
        // Non-RandomAccess collection (LinkedList) 
        List<String> nonRandomAccessList = new LinkedList<>(Arrays.asList("link1", "link2"));

        map.put(randomAccessList, "randomAccess");
        map.put(nonRandomAccessList, "nonRandomAccess");

        // Both should be found with their respective keys
        assertEquals("randomAccess", map.get(randomAccessList));
        assertEquals("nonRandomAccess", map.get(nonRandomAccessList));

        // Verify both are in the map
        assertTrue(map.containsKey(randomAccessList));
        assertTrue(map.containsKey(nonRandomAccessList));

        // Test with 3 elements as well to exercise flattenCollection3 paths
        List<String> randomAccess3 = new ArrayList<>(Arrays.asList("r1", "r2", "r3"));
        List<String> nonRandomAccess3 = new LinkedList<>(Arrays.asList("l1", "l2", "l3"));

        map.put(randomAccess3, "random3");
        map.put(nonRandomAccess3, "nonRandom3");

        assertEquals("random3", map.get(randomAccess3));
        assertEquals("nonRandom3", map.get(nonRandomAccess3));

        // Verify the map now contains all 4 entries
        assertEquals(4, map.size());
    }
}