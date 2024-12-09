package com.cedarsoftware.util.convert;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
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
class CollectionConversionTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    private enum Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY
    }

    @Test
    void testCollectionToArray() {
        // Test List to various array types
        List<String> stringList = Arrays.asList("one", "two", "three");

        // To String array
        String[] stringArray = converter.convert(stringList, String[].class);
        assertArrayEquals(new String[]{"one", "two", "three"}, stringArray);

        // To Object array
        Object[] objectArray = converter.convert(stringList, Object[].class);
        assertArrayEquals(new Object[]{"one", "two", "three"}, objectArray);

        // To custom type array with conversion
        List<String> numberStrings = Arrays.asList("1", "2", "3");
        Integer[] intArray = converter.convert(numberStrings, Integer[].class);
        assertArrayEquals(new Integer[]{1, 2, 3}, intArray);

        // Test Set to array
        Set<String> stringSet = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        String[] setToArray = converter.convert(stringSet, String[].class);
        assertArrayEquals(new String[]{"a", "b", "c"}, setToArray);

        // Test Queue to array
        Queue<String> queue = new LinkedList<>(Arrays.asList("x", "y", "z"));
        String[] queueToArray = converter.convert(queue, String[].class);
        assertArrayEquals(new String[]{"x", "y", "z"}, queueToArray);
    }

    @Test
    void testArrayToCollection() {
        String[] source = {"one", "two", "three"};

        // To List
        List<String> list = converter.convert(source, List.class);
        assertEquals(Arrays.asList("one", "two", "three"), list);

        // To Set
        Set<String> set = converter.convert(source, Set.class);
        assertEquals(new LinkedHashSet<>(Arrays.asList("one", "two", "three")), set);

        // To specific collection types
        assertInstanceOf(ArrayList.class, converter.convert(source, ArrayList.class));
        assertInstanceOf(LinkedList.class, converter.convert(source, LinkedList.class));
        assertInstanceOf(HashSet.class, converter.convert(source, HashSet.class));
        assertInstanceOf(LinkedHashSet.class, converter.convert(source, LinkedHashSet.class));
        assertInstanceOf(TreeSet.class, converter.convert(source, TreeSet.class));
        assertInstanceOf(ConcurrentSkipListSet.class, converter.convert(source, ConcurrentSkipListSet.class));
        assertInstanceOf(CopyOnWriteArrayList.class, converter.convert(source, CopyOnWriteArrayList.class));
        assertInstanceOf(CopyOnWriteArraySet.class, converter.convert(source, CopyOnWriteArraySet.class));
    }

    @Test
    void testArrayToArray() {
        // Test primitive array conversions
        int[] intArray = {1, 2, 3};
        long[] longArray = converter.convert(intArray, long[].class);
        assertArrayEquals(new long[]{1L, 2L, 3L}, longArray);

        // Test wrapper array conversions
        Integer[] integerArray = {1, 2, 3};
        Long[] longWrapperArray = converter.convert(integerArray, Long[].class);
        assertArrayEquals(new Long[]{1L, 2L, 3L}, longWrapperArray);

        // Test string to number array conversion
        String[] stringArray = {"1", "2", "3"};
        Integer[] convertedIntArray = converter.convert(stringArray, Integer[].class);
        assertArrayEquals(new Integer[]{1, 2, 3}, convertedIntArray);

        // Test mixed type array conversion
        Object[] mixedArray = {1, "2", 3.0};
        Long[] convertedLongArray = converter.convert(mixedArray, Long[].class);
        assertArrayEquals(new Long[]{1L, 2L, 3L}, convertedLongArray);
    }

    @Test
    void testEnumSetConversions() {
        // Create source EnumSet
        EnumSet<Day> days = EnumSet.of(Day.MONDAY, Day.WEDNESDAY, Day.FRIDAY);

        // Test EnumSet to arrays
        Object[] objectArray = converter.convert(days, Object[].class);
        assertEquals(3, objectArray.length);
        assertTrue(objectArray[0] instanceof Day);

        String[] stringArray = converter.convert(days, String[].class);
        assertArrayEquals(new String[]{"MONDAY", "WEDNESDAY", "FRIDAY"}, stringArray);

        Integer[] ordinalArray = converter.convert(days, Integer[].class);
        assertArrayEquals(new Integer[]{0, 2, 4}, ordinalArray);

        // Test EnumSet to collections
        List<Day> list = converter.convert(days, List.class);
        assertEquals(3, list.size());
        assertTrue(list.contains(Day.MONDAY));

        Set<Day> set = converter.convert(days, Set.class);
        assertEquals(3, set.size());
        assertTrue(set.contains(Day.WEDNESDAY));
    }

    @Test
    void testToEnumSet() {
        // Test array of enums to EnumSet
        Day[] dayArray = {Day.MONDAY, Day.WEDNESDAY};
        EnumSet<Day> fromEnumArray = (EnumSet<Day>)(Object)converter.convert(dayArray, Day.class);
        assertTrue(fromEnumArray.contains(Day.MONDAY));
        assertTrue(fromEnumArray.contains(Day.WEDNESDAY));

        // Test array of strings to EnumSet
        String[] stringArray = {"MONDAY", "FRIDAY"};
        EnumSet<Day> fromStringArray = (EnumSet<Day>)(Object)converter.convert(stringArray, Day.class);
        assertTrue(fromStringArray.contains(Day.MONDAY));
        assertTrue(fromStringArray.contains(Day.FRIDAY));

        // Test array of numbers (ordinals) to EnumSet
        Integer[] ordinalArray = {0, 4}; // MONDAY and FRIDAY
        EnumSet<Day> fromOrdinalArray = (EnumSet<Day>)(Object)converter.convert(ordinalArray, Day.class);
        assertTrue(fromOrdinalArray.contains(Day.MONDAY));
        assertTrue(fromOrdinalArray.contains(Day.FRIDAY));

        // Test collection to EnumSet
        List<String> stringList = Arrays.asList("TUESDAY", "THURSDAY");
        EnumSet<Day> fromCollection = (EnumSet<Day>)(Object)converter.convert(stringList, Day.class);
        assertTrue(fromCollection.contains(Day.TUESDAY));
        assertTrue(fromCollection.contains(Day.THURSDAY));

        // Test mixed array to EnumSet
        Object[] mixedArray = {Day.MONDAY, "WEDNESDAY", 4}; // Enum, String, and ordinal
        EnumSet<Day> fromMixed = (EnumSet<Day>)(Object)converter.convert(mixedArray, Day.class);
        assertTrue(fromMixed.contains(Day.MONDAY));
        assertTrue(fromMixed.contains(Day.WEDNESDAY));
        assertTrue(fromMixed.contains(Day.FRIDAY));
    }

    @Test
    void testCollectionToCollection() {
        List<String> source = Arrays.asList("1", "2", "3");

        // Test conversion to various collection types
        assertInstanceOf(ArrayList.class, converter.convert(source, ArrayList.class));
        assertInstanceOf(LinkedList.class, converter.convert(source, LinkedList.class));
        assertInstanceOf(Vector.class, converter.convert(source, Vector.class));
        assertInstanceOf(Stack.class, converter.convert(source, Stack.class));
        assertInstanceOf(HashSet.class, converter.convert(source, HashSet.class));
        assertInstanceOf(LinkedHashSet.class, converter.convert(source, LinkedHashSet.class));
        assertInstanceOf(TreeSet.class, converter.convert(source, TreeSet.class));

        // Test concurrent collections
        assertInstanceOf(ConcurrentSkipListSet.class, converter.convert(source, ConcurrentSkipListSet.class));
        assertInstanceOf(CopyOnWriteArrayList.class, converter.convert(source, CopyOnWriteArrayList.class));
        assertInstanceOf(CopyOnWriteArraySet.class, converter.convert(source, CopyOnWriteArraySet.class));

        // Test queues
        assertInstanceOf(ArrayDeque.class, converter.convert(source, ArrayDeque.class));
        assertInstanceOf(PriorityQueue.class, converter.convert(source, PriorityQueue.class));
        assertInstanceOf(ConcurrentLinkedQueue.class, converter.convert(source, ConcurrentLinkedQueue.class));

        // Test blocking queues
        assertInstanceOf(LinkedBlockingQueue.class, converter.convert(source, LinkedBlockingQueue.class));
        assertInstanceOf(ArrayBlockingQueue.class, converter.convert(source, ArrayBlockingQueue.class));
        assertInstanceOf(PriorityBlockingQueue.class, converter.convert(source, PriorityBlockingQueue.class));
        assertInstanceOf(LinkedBlockingDeque.class, converter.convert(source, LinkedBlockingDeque.class));
    }

    @Test
    void testInvalidEnumSetTarget() {
        Object[] array = {Day.MONDAY, Day.TUESDAY};
        Executable conversion = () -> converter.convert(array, EnumSet.class);
        assertThrows(IllegalArgumentException.class, conversion, "To convert to EnumSet, specify the Enum class to convert to.  See convert() Javadoc for example.");
    }

    @Test
    void testInvalidEnumOrdinal() {
        Integer[] invalidOrdinals = {0, 99}; // 99 is out of range
        Executable conversion = () -> converter.convert(invalidOrdinals, Day.class);
        assertThrows(IllegalArgumentException.class, conversion, "99 is out of range");
    }

    @Test
    void testNullHandling() {
        List<String> listWithNull = Arrays.asList("one", null, "three");

        // Null elements should be preserved in Object arrays
        Object[] objectArray = converter.convert(listWithNull, Object[].class);
        assertArrayEquals(new Object[]{"one", null, "three"}, objectArray);

        // Null elements should be preserved in String arrays
        String[] stringArray = converter.convert(listWithNull, String[].class);
        assertArrayEquals(new String[]{"one", null, "three"}, stringArray);

        // Null elements should be preserved in collections
        List<String> convertedList = converter.convert(listWithNull, List.class);
        assertEquals(Arrays.asList("one", null, "three"), convertedList);
    }

    @Test
    void testCollectionToCollection2() {
        Collection<String> source = Arrays.asList("a", "b", "c");
        Collection<String> result = converter.convert(source, Collection.class);
        assertEquals(source.size(), result.size());
        assertTrue(result.containsAll(source));
    }

    private static class DefaultConverterOptions implements ConverterOptions {
        // Use all defaults
    }
}