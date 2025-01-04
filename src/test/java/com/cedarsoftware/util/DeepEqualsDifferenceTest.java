package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeepEqualsDifferenceTest {

    @Test
    public void testArrayDirectCycleDifference() {
        Object[] array1 = new Object[1];
        array1[0] = array1;  // Direct cycle

        Object[] array2 = new Object[1];
        array2[0] = array2;  // Direct cycle but different length
        array2 = Arrays.copyOf(array2, 2);  // Make arrays different lengths

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(array1, array2, options));
        String diff = (String) options.get("diff");
        assertNotNull("Difference description should be generated", diff);
        System.out.println(diff);  // Verify output doesn't contain endless recursion
    }

    @Test
    public void testCollectionDirectCycleDifference() {
        List<Object> list1 = new ArrayList<>();
        list1.add(list1);  // Direct cycle
        list1.add("extra");

        List<Object> list2 = new ArrayList<>();
        list2.add(list2);  // Direct cycle
        // list2 missing "extra" element

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(list1, list2, options));
        String diff = (String) options.get("diff");
        assertNotNull("Difference description should be generated", diff);
        System.out.println(diff);
    }

    @Test
    public void testMapValueCycleDifference() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("key", map1);  // Cycle in value
        map1.put("diff", "value1");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("key", map2);  // Cycle in value
        map2.put("diff", "value2");  // Different value

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(map1, map2, options));
        String diff = (String) options.get("diff");
        assertNotNull("Difference description should be generated", diff);
        System.out.println(diff);
    }

    @Test
    public void testObjectFieldCycleDifference() {
        class CyclicObject {
            CyclicObject self;
            String value;

            CyclicObject(String value) {
                this.value = value;
                this.self = this;  // Direct cycle
            }
        }

        CyclicObject obj1 = new CyclicObject("value1");
        CyclicObject obj2 = new CyclicObject("value2");  // Different value

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(obj1, obj2, options));
        String diff = (String) options.get("diff");
        assertNotNull("Difference description should be generated", diff);
        System.out.println(diff);
    }

    @Test
    public void testArrayIndirectCycleDifference() {
        class ArrayHolder {
            Object[] array;
            String value;

            ArrayHolder(String value) {
                this.value = value;
            }
        }

        Object[] array1 = new Object[1];
        ArrayHolder holder1 = new ArrayHolder("value1");
        holder1.array = array1;
        array1[0] = holder1;  // Indirect cycle

        Object[] array2 = new Object[1];
        ArrayHolder holder2 = new ArrayHolder("value2");  // Different value
        holder2.array = array2;
        array2[0] = holder2;  // Indirect cycle

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(array1, array2, options));
        String diff = (String) options.get("diff");
        assertNotNull("Difference description should be generated", diff);
        System.out.println(diff);
    }

    @Test
    public void testCollectionIndirectCycleDifference() {
        class CollectionHolder {
            Collection<Object> collection;
            String value;

            CollectionHolder(String value) {
                this.value = value;
            }
        }

        List<Object> list1 = new ArrayList<>();
        CollectionHolder holder1 = new CollectionHolder("value1");
        holder1.collection = list1;
        list1.add(holder1);  // Indirect cycle

        List<Object> list2 = new ArrayList<>();
        CollectionHolder holder2 = new CollectionHolder("value2");  // Different value
        holder2.collection = list2;
        list2.add(holder2);  // Indirect cycle

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(list1, list2, options));
        String diff = (String) options.get("diff");
        assertNotNull("Difference description should be generated", diff);
        System.out.println(diff);
    }

    @Test
    public void testMapValueIndirectCycleDifference() {
        class MapHolder {
            Map<String, Object> map;
            String value;

            MapHolder(String value) {
                this.value = value;
            }
        }

        Map<String, Object> map1 = new HashMap<>();
        MapHolder holder1 = new MapHolder("value1");
        holder1.map = map1;
        map1.put("key", holder1);  // Indirect cycle

        Map<String, Object> map2 = new HashMap<>();
        MapHolder holder2 = new MapHolder("value2");  // Different value
        holder2.map = map2;
        map2.put("key", holder2);  // Indirect cycle

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(map1, map2, options));
        String diff = (String) options.get("diff");
        assertNotNull("Difference description should be generated", diff);
        System.out.println(diff);
    }

    @Test
    public void testObjectIndirectCycleDifference() {
        class ObjectA {
            Object refToB;
            String value;

            ObjectA(String value) {
                this.value = value;
            }
        }

        class ObjectB {
            ObjectA refToA;
        }

        ObjectA objA1 = new ObjectA("value1");
        ObjectB objB1 = new ObjectB();
        objA1.refToB = objB1;
        objB1.refToA = objA1;  // Indirect cycle

        ObjectA objA2 = new ObjectA("value2");  // Different value
        ObjectB objB2 = new ObjectB();
        objA2.refToB = objB2;
        objB2.refToA = objA2;  // Indirect cycle

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(objA1, objA2, options));
        String diff = (String) options.get("diff");
        assertNotNull("Difference description should be generated", diff);
        System.out.println(diff);
    }
}