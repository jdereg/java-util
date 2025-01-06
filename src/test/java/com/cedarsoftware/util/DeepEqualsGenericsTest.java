package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeepEqualsGenericsTest {

    @Test
    void testListWithDifferentGenerics() {
        List<String> stringList = new ArrayList<>();
        List<Object> objectList = new ArrayList<>();

        stringList.add("test");
        objectList.add("test");

        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(stringList, objectList, options),
                "Lists with different generic types but same content should be equal");
    }

    @Test
    void testMapWithDifferentGenerics() {
        Map<String, Integer> stringIntMap = new HashMap<>();
        Map<Object, Number> objectNumberMap = new HashMap<>();

        stringIntMap.put("key", 1);
        objectNumberMap.put("key", 1);

        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(stringIntMap, objectNumberMap, options),
                "Maps with compatible generic types and same content should be equal");
    }

    @Test
    void testNestedGenerics() {
        List<List<String>> nestedStringList = new ArrayList<>();
        List<List<Object>> nestedObjectList = new ArrayList<>();

        nestedStringList.add(Arrays.asList("test"));
        nestedObjectList.add(Arrays.asList("test"));

        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(nestedStringList, nestedObjectList, options),
                "Nested lists with different generic types but same content should be equal");
    }
    
    @Test
    void testListWithNumbers() {
        List<Number> numberList = new ArrayList<>();
        List<Integer> integerList = new ArrayList<>();
        List<Double> doubleList = new ArrayList<>();

        numberList.add(1);
        integerList.add(1);
        doubleList.add(1.0);

        Map<String, Object> options = new HashMap<>();

        // Number vs Integer
        assertTrue(DeepEquals.deepEquals(numberList, integerList, options));

        // Number vs Double
        assertTrue(DeepEquals.deepEquals(numberList, doubleList, options));

        // Integer vs Double (should be equal because 1 == 1.0)
        assertTrue(DeepEquals.deepEquals(integerList, doubleList, options));
    }

    @Test
    void testMapWithNumbers() {
        Map<String, Number> numberMap = new HashMap<>();
        Map<String, Integer> integerMap = new HashMap<>();
        Map<String, Double> doubleMap = new HashMap<>();

        numberMap.put("key", 1);
        integerMap.put("key", 1);
        doubleMap.put("key", 1.0);

        Map<String, Object> options = new HashMap<>();

        // Number vs Integer
        assertTrue(DeepEquals.deepEquals(numberMap, integerMap, options));

        // Number vs Double
        assertTrue(DeepEquals.deepEquals(numberMap, doubleMap, options));

        // Integer vs Double
        assertTrue(DeepEquals.deepEquals(integerMap, doubleMap, options));
    }

    @Test
    void testNumberEdgeCases() {
        List<Number> list1 = new ArrayList<>();
        List<Number> list2 = new ArrayList<>();

        // Test epsilon comparison
        list1.add(1.0);
        list2.add(1.0 + Math.ulp(1.0));  // Smallest possible difference

        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(list1, list2, options));

        // Test BigDecimal
        list1.clear();
        list2.clear();
        list1.add(new BigDecimal("1.0"));
        list2.add(1.0);
        assertTrue(DeepEquals.deepEquals(list1, list2, options));
    }

    @Test
    void testListWithDifferentContent() {
        List<String> stringList = new ArrayList<>();
        List<Object> objectList = new ArrayList<>();

        stringList.add("test");
        objectList.add(new Object());  // Different content type

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(stringList, objectList, options));
        assertTrue(getDiff(options).contains("collection element mismatch"));
    }

    @Test
    void testMapWithDifferentContent() {
        Map<String, Integer> stringIntMap = new HashMap<>();
        Map<Object, Number> objectNumberMap = new HashMap<>();

        stringIntMap.put("key", 1);
        objectNumberMap.put("key", 1.5);  // Different number value

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(stringIntMap, objectNumberMap, options));
        assertTrue(getDiff(options).contains("value mismatch"));
    }

    @Test
    void testWildcardGenerics() {
        List<?> wildcardList1 = new ArrayList<>();
        List<?> wildcardList2 = new ArrayList<>();
        
        wildcardList1 = Arrays.asList("test", 123, new Date());
        wildcardList2 = Arrays.asList("test", 123, new Date());

        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(wildcardList1, wildcardList2, options),
                "Lists with wildcard generics containing same elements should be equal");
    }

    @Test
    void testBoundedWildcards() {
        List<? extends Number> numberList1 = Arrays.asList(1, 2.0, 3L);
        List<? extends Number> numberList2 = Arrays.asList(1, 2.0, 3L);
        List<? extends Integer> integerList = Arrays.asList(1, 2, 3);

        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(numberList1, numberList2, options),
                "Lists with bounded wildcards containing same numbers should be equal");

        // Test with different number types
        List<? extends Number> mixedNumbers1 = Arrays.asList(1, 2.0, new BigDecimal("3"));
        List<? extends Number> mixedNumbers2 = Arrays.asList(1.0, 2, 3.0);
        assertTrue(DeepEquals.deepEquals(mixedNumbers1, mixedNumbers2, options),
                "Lists with different number types but equal values should be equal");
    }

    @Test
    void testMultipleTypeParameters() {
        class Pair<K, V> {
            K key;
            V value;
            Pair(K key, V value) {
                this.key = key;
                this.value = value;
            }
        }

        Pair<String, Integer> pair1 = new Pair<>("test", 1);
        Pair<Object, Number> pair2 = new Pair<>("test", 1);

        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(pair1, pair2, options),
                "Objects with different but compatible generic types should be equal");
    }

    @Test
    void testComplexGenerics() {
        Map<String, List<? extends Number>> map1 = new HashMap<>();
        Map<String, List<? extends Number>> map2 = new HashMap<>();

        map1.put("key", Arrays.asList(1, 2.0, 3L));
        map2.put("key", Arrays.asList(1.0, 2L, 3));

        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(map1, map2, options),
                "Maps with complex generic types and equivalent values should be equal");
    }

    @Test
    void testNestedWildcards() {
        List<Map<?, ? extends Number>> list1 = new ArrayList<>();
        List<Map<?, ? extends Number>> list2 = new ArrayList<>();

        Map<String, Integer> innerMap1 = new HashMap<>();
        Map<String, Double> innerMap2 = new HashMap<>();
        innerMap1.put("test", 1);
        innerMap2.put("test", 1.0);

        list1.add(innerMap1);
        list2.add(innerMap2);

        Map<String, Object> options = new HashMap<>();
        assertTrue(DeepEquals.deepEquals(list1, list2, options),
                "Nested structures with wildcards should compare based on actual values");
    }
    
    String getDiff(Map<String, Object> options) {
        return (String) options.get(DeepEquals.DIFF);
    }
}