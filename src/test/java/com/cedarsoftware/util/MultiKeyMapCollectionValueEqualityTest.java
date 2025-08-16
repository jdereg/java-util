package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that verifies the KIND_COLLECTION fast path properly respects value-based equality mode.
 * When valueBasedEquality=true, collections with numerically equivalent but type-different elements
 * should match (e.g., [1, 2] should match [1L, 2L]).
 */
public class MultiKeyMapCollectionValueEqualityTest {

    @Test
    public void testCollectionFastPathWithValueBasedEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(true)
            .build(); // value-based equality mode
        
        // Test 1: Integer list vs Long list with same numeric values
        List<Integer> intList = Arrays.asList(1, 2, 3);
        List<Long> longList = Arrays.asList(1L, 2L, 3L);
        
        map.put(intList, "int-list");
        
        // With value-based equality, the Long list should find the Integer list entry
        assertEquals("int-list", map.get(longList), 
            "Value-based equality should match [1,2,3] Integer list with [1L,2L,3L] Long list");
        
        // Test 2: Double list with whole numbers vs Integer list
        List<Double> doubleList = Arrays.asList(1.0, 2.0, 3.0);
        assertEquals("int-list", map.get(doubleList),
            "Value-based equality should match [1,2,3] Integer list with [1.0,2.0,3.0] Double list");
        
        // Test 3: Mixed numeric types in ArrayList
        ArrayList<Number> mixedList = new ArrayList<>();
        mixedList.add(1);  // Integer
        mixedList.add(2L); // Long  
        mixedList.add(3.0); // Double
        
        map.put(mixedList, "mixed-list");
        
        // All of these should find the mixed list
        List<Integer> allInts = Arrays.asList(1, 2, 3);
        List<Long> allLongs = Arrays.asList(1L, 2L, 3L);
        List<Double> allDoubles = Arrays.asList(1.0, 2.0, 3.0);
        
        assertEquals("mixed-list", map.get(allInts),
            "Value-based equality should match mixed [1,2L,3.0] with [1,2,3] Integer list");
        assertEquals("mixed-list", map.get(allLongs),
            "Value-based equality should match mixed [1,2L,3.0] with [1L,2L,3L] Long list");
        assertEquals("mixed-list", map.get(allDoubles),
            "Value-based equality should match mixed [1,2L,3.0] with [1.0,2.0,3.0] Double list");
    }
    
    @Test
    public void testCollectionFastPathWithTypeBasedEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(false)
            .build(); // type-based equality mode
        
        // Test 1: Integer list vs Long list with same numeric values
        List<Integer> intList = Arrays.asList(1, 2, 3);
        List<Long> longList = Arrays.asList(1L, 2L, 3L);
        
        map.put(intList, "int-list");
        map.put(longList, "long-list");
        
        // With type-based equality, these are different keys
        assertEquals("int-list", map.get(intList),
            "Type-based equality should find exact Integer list");
        assertEquals("long-list", map.get(longList),
            "Type-based equality should find exact Long list");
        
        // Cross-type lookups should not match
        List<Double> doubleList = Arrays.asList(1.0, 2.0, 3.0);
        assertNull(map.get(doubleList),
            "Type-based equality should not match Double list with Integer or Long lists");
    }
    
    @Test
    public void testNaNHandlingInCollectionsWithValueBasedEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(true)
            .build(); // value-based equality mode
        
        // Test NaN equality in collections (value-based mode treats NaN == NaN)
        List<Double> listWithNaN1 = Arrays.asList(1.0, Double.NaN, 3.0);
        List<Double> listWithNaN2 = Arrays.asList(1.0, Double.NaN, 3.0);
        
        map.put(listWithNaN1, "has-nan");
        
        // With value-based equality, NaN should equal NaN
        assertEquals("has-nan", map.get(listWithNaN2),
            "Value-based equality should treat NaN == NaN in collections");
        
        // Float NaN should also match Double NaN
        List<Number> mixedNaN = new ArrayList<>();
        mixedNaN.add(1.0);
        mixedNaN.add(Float.NaN);
        mixedNaN.add(3.0);
        
        assertEquals("has-nan", map.get(mixedNaN),
            "Value-based equality should treat Float.NaN == Double.NaN in collections");
    }
    
    @Test
    public void testNaNHandlingInCollectionsWithTypeBasedEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(false)
            .build(); // type-based equality mode
        
        // Test NaN handling with standard Java equals
        // Note: List.equals() uses element.equals(), and Double.valueOf(NaN).equals(Double.valueOf(NaN)) returns true
        List<Double> listWithNaN1 = Arrays.asList(1.0, Double.NaN, 3.0);
        List<Double> listWithNaN2 = Arrays.asList(1.0, Double.NaN, 3.0);
        
        map.put(listWithNaN1, "has-nan-1");
        
        // With type-based equality using List.equals(), Double.NaN.equals(Double.NaN) is true
        // So the lists ARE equal when they're the same type
        assertEquals("has-nan-1", map.get(listWithNaN2),
            "Type-based equality with List.equals() uses Double.equals() which treats NaN == NaN");
    }
    
    @Test
    public void testDifferentCollectionTypesWithSameElements() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(true)
            .build(); // value-based equality mode
        
        // ArrayList vs LinkedList with same Integer elements
        ArrayList<Integer> arrayList = new ArrayList<>(Arrays.asList(1, 2, 3));
        LinkedList<Integer> linkedList = new LinkedList<>(Arrays.asList(1, 2, 3));
        
        map.put(arrayList, "array-list");
        
        // Different collection types but same elements - should NOT use fast path
        // Instead should fall through to element-wise comparison
        assertEquals("array-list", map.get(linkedList),
            "Value-based equality should match ArrayList and LinkedList with same elements");
        
        // Now with mixed numeric types
        ArrayList<Number> arrayListMixed = new ArrayList<>();
        arrayListMixed.add(1);   // Integer
        arrayListMixed.add(2L);  // Long
        arrayListMixed.add(3.0); // Double
        
        LinkedList<Number> linkedListMixed = new LinkedList<>();
        linkedListMixed.add(1L);  // Long
        linkedListMixed.add(2);   // Integer  
        linkedListMixed.add(3.0); // Double
        
        map.put(arrayListMixed, "mixed-array");
        
        assertEquals("mixed-array", map.get(linkedListMixed),
            "Value-based equality should match different collection types with numerically equal elements");
    }
    
    @Test
    public void testZeroHandlingInCollections() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(true)
            .build(); // value-based equality mode
        
        // Test that -0.0 and +0.0 are treated as equal
        List<Double> listWithPosZero = Arrays.asList(1.0, 0.0, 3.0);
        List<Double> listWithNegZero = Arrays.asList(1.0, -0.0, 3.0);
        
        map.put(listWithPosZero, "has-zero");
        
        assertEquals("has-zero", map.get(listWithNegZero),
            "Value-based equality should treat +0.0 == -0.0 in collections");
        
        // Integer zero should also match
        List<Number> listWithIntZero = Arrays.asList(1.0, 0, 3.0);
        assertEquals("has-zero", map.get(listWithIntZero),
            "Value-based equality should treat Integer 0 == Double 0.0 in collections");
    }
    
    @Test
    public void testSameCollectionTypeOptimization() {
        // This test verifies that when NOT using value-based equality,
        // same collection types use the fast path (built-in equals)
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
            .valueBasedEquality(false)
            .build(); // type-based equality
        
        ArrayList<Integer> list1 = new ArrayList<>(Arrays.asList(1, 2, 3));
        ArrayList<Integer> list2 = new ArrayList<>(Arrays.asList(1, 2, 3));
        ArrayList<Integer> list3 = new ArrayList<>(Arrays.asList(1, 2, 4));
        
        map.put(list1, "list-123");
        
        // Same type, same elements - should use fast path and match
        assertEquals("list-123", map.get(list2),
            "Type-based equality with same collection type should use fast path");
        
        // Same type, different elements - should use fast path and not match
        assertNull(map.get(list3),
            "Type-based equality with same collection type but different elements should not match");
    }
}