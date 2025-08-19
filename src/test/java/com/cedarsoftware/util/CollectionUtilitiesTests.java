package com.cedarsoftware.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.CollectionUtilities.getCheckedCollection;
import static com.cedarsoftware.util.CollectionUtilities.getEmptyCollection;
import static com.cedarsoftware.util.CollectionUtilities.getSynchronizedCollection;
import static com.cedarsoftware.util.CollectionUtilities.getUnmodifiableCollection;
import static com.cedarsoftware.util.CollectionUtilities.hasContent;
import static com.cedarsoftware.util.CollectionUtilities.isEmpty;
import static com.cedarsoftware.util.CollectionUtilities.listOf;
import static com.cedarsoftware.util.CollectionUtilities.setOf;
import static com.cedarsoftware.util.CollectionUtilities.size;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
class CollectionUtilitiesTests {
    static class Rec {
        final String s;
        final int i;
        Rec(String s, int i) {
            this.s = s;
            this.i = i;
        }
    }
    
    // Test enum for EnumSet tests
    enum Color { RED, GREEN, BLUE, YELLOW, ORANGE }

    @Test
    void testCollectionSize() {
        assertEquals(0, size(null));
        assertEquals(0, size(new ArrayList<>()));

        final List<String> list = listOf("alpha", "bravo", "charlie");
        assertEquals(3, size(list));
    }

    @Test
    void testIsEmpty() {
        assertTrue(isEmpty(null));
        assertTrue(isEmpty(new ArrayList<>()));
        assertFalse(isEmpty(listOf("alpha", "bravo", "charlie")));
    }

    @Test
    void testHasContent() {
        assertFalse(hasContent(null));
        assertFalse(hasContent(new ArrayList<>()));
        assertTrue(hasContent(listOf("alpha", "bravo", "charlie")));
    }

    @Test
    void testListOf() {
        final List<Rec> list = listOf(new Rec("alpha", 1), new Rec("bravo", 2), new Rec("charlie", 3));
        assertEquals(3, list.size());
        assertEquals("alpha", list.get(0).s);
        assertEquals(1, list.get(0).i);
        assertEquals("bravo", list.get(1).s);
        assertEquals(2, list.get(1).i);
        assertEquals("charlie", list.get(2).s);
        assertEquals(3, list.get(2).i);
    }

    @Test
    void testSetOf() {
        final Set<Rec> set = setOf(new Rec("alpha", 1), new Rec("bravo", 2), new Rec("charlie", 3));
        assertEquals(3, set.size());
        int i = 1;
        for (Rec rec : set) {
            if (i == 1) {
                assertEquals("alpha", rec.s);
                assertEquals(1, rec.i);
            } else if (i == 2) {
                assertEquals("bravo", rec.s);
                assertEquals(2, rec.i);
            } else if (i == 3) {
                assertEquals("charlie", rec.s);
                assertEquals(3, rec.i);
            }
            i++;
        }
    }
    
    @Test
    void testGetUnmodifiableCollection() {
        List<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        Collection<String> unmodifiableList = getUnmodifiableCollection(list);
        assertEquals(2, unmodifiableList.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> unmodifiableList.add("three"));

        Set<String> set = new HashSet<>();
        set.add("three");
        set.add("four");
        Collection<String> unmodifiableSet = getUnmodifiableCollection(set);
        assertEquals(2, unmodifiableSet.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> unmodifiableSet.add("five"));

        SortedSet<String> sortedSet = new TreeSet<>();
        sortedSet.add("five");
        sortedSet.add("six");
        Collection<String> unmodifiableSortedSet = getUnmodifiableCollection(sortedSet);
        assertEquals(2, unmodifiableSortedSet.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> unmodifiableSortedSet.add("seven"));

        NavigableSet<String> navigableSet = new TreeSet<>();
        navigableSet.add("seven");
        navigableSet.add("eight");
        Collection<String> unmodifiableNavigableSet = getUnmodifiableCollection(navigableSet);
        assertEquals(2, unmodifiableNavigableSet.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> unmodifiableNavigableSet.add("nine"));

        Collection<String> regularCollection = new ArrayList<>();
        regularCollection.add("nine");
        regularCollection.add("ten");
        Collection<String> unmodifiableCollection = getUnmodifiableCollection(regularCollection);
        assertEquals(2, unmodifiableCollection.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> unmodifiableCollection.add("eleven"));
    }

    @Test
    void testGetEmptyCollection() {
        List<String> list = new ArrayList<>();
        Collection<String> emptyList = getEmptyCollection(list);
        assertEquals(0, emptyList.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> emptyList.add("one"));

        Set<String> set = new HashSet<>();
        Collection<String> emptySet = getEmptyCollection(set);
        assertEquals(0, emptySet.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> emptySet.add("one"));

        SortedSet<String> sortedSet = new TreeSet<>();
        Collection<String> emptySortedSet = getEmptyCollection(sortedSet);
        assertEquals(0, emptySortedSet.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> emptySortedSet.add("one"));

        NavigableSet<String> navigableSet = new TreeSet<>();
        Collection<String> emptyNavigableSet = getEmptyCollection(navigableSet);
        assertEquals(0, emptyNavigableSet.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> emptyNavigableSet.add("one"));

        Collection<String> regularCollection = new ArrayList<>();
        Collection<String> emptyCollection = getEmptyCollection(regularCollection);
        assertEquals(0, emptyCollection.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> emptyCollection.add("one"));
    }

    @Test
    void testGetCheckedCollection() {
        List<String> list = new ArrayList<>();
        Collection<String> checkedList = getCheckedCollection(list, String.class);
        checkedList.add("one");
        checkedList.add("two");
        assertEquals(2, checkedList.size());

        Set<String> set = new HashSet<>();
        Collection<String> checkedSet = getCheckedCollection(set, String.class);
        checkedSet.add("three");
        checkedSet.add("four");
        assertEquals(2, checkedSet.size());

        SortedSet<String> sortedSet = new TreeSet<>();
        Collection<String> checkedSortedSet = getCheckedCollection(sortedSet, String.class);
        checkedSortedSet.add("five");
        checkedSortedSet.add("six");
        assertEquals(2, checkedSortedSet.size());

        NavigableSet<String> navigableSet = new TreeSet<>();
        Collection<String> checkedNavigableSet = getCheckedCollection(navigableSet, String.class);
        checkedNavigableSet.add("seven");
        checkedNavigableSet.add("eight");
        assertEquals(2, checkedNavigableSet.size());

        Collection<String> regularCollection = new ArrayList<>();
        Collection<String> checkedCollection = getCheckedCollection(regularCollection, String.class);
        checkedCollection.add("nine");
        checkedCollection.add("ten");
        assertEquals(2, checkedCollection.size());
    }

    @Test
    void testGetSynchronizedCollection() {
        List<String> list = new ArrayList<>();
        Collection<String> synchronizedList = getSynchronizedCollection(list);
        synchronizedList.add("one");
        synchronizedList.add("two");
        assertEquals(2, synchronizedList.size());
        assertTrue(synchronizedList.contains("one"));

        Set<String> set = new HashSet<>();
        Collection<String> synchronizedSet = getSynchronizedCollection(set);
        synchronizedSet.add("three");
        synchronizedSet.add("four");
        assertEquals(2, synchronizedSet.size());
        assertTrue(synchronizedSet.contains("three"));

        SortedSet<String> sortedSet = new TreeSet<>();
        sortedSet.add("five");
        Collection<String> synchronizedSortedSet = getSynchronizedCollection(sortedSet);

        synchronizedSortedSet.add("six");
        assertTrue(synchronizedSortedSet.contains("five"));

        NavigableSet<String> navigableSet = new TreeSet<>();
        navigableSet.add("seven");
        Collection<String> synchronizedNavigableSet = getSynchronizedCollection(navigableSet);

        synchronizedNavigableSet.add("eight");
        assertTrue(synchronizedNavigableSet.contains("seven"));

        Collection<String> regularCollection = new ArrayList<>();
        regularCollection.add("nine");
        Collection<String> synchronizedCollection = getSynchronizedCollection(regularCollection);

        synchronizedCollection.add("ten");
        assertTrue(synchronizedCollection.contains("nine"));
    }

    @Test
    void testGetEmptyCollectionSpecificTypes() {
        SortedSet<String> sortedSet = new TreeSet<>();
        Collection<String> emptySortedSet = getEmptyCollection(sortedSet);
        assertEquals(0, emptySortedSet.size());
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> emptySortedSet.add("one"));
    }

    @Test
    void testDeepCopyContainers_SimpleList() {
        List<String> original = Arrays.asList("a", "b", "c");
        List<String> copy = CollectionUtilities.deepCopyContainers(original);
        
        assertNotSame(original, copy);
        assertEquals(original, copy);
        
        // Berries should be same references
        for (String s : original) {
            assertTrue(copy.contains(s));
        }
    }

    @Test
    void testDeepCopyContainers_NestedLists() {
        List<List<String>> original = Arrays.asList(
            Arrays.asList("a", "b"),
            Arrays.asList("c", "d", "e")
        );
        
        List<List<String>> copy = CollectionUtilities.deepCopyContainers(original);
        
        // All collections should be different
        assertNotSame(original, copy);
        assertNotSame(original.get(0), copy.get(0));
        assertNotSame(original.get(1), copy.get(1));
        
        // But content equal
        assertEquals(original, copy);
    }

    @Test
    void testDeepCopyContainers_SetTypes() {
        // Test TreeSet becomes TreeSet
        TreeSet<String> treeSet = new TreeSet<>(Arrays.asList("c", "a", "b"));
        TreeSet<String> treeCopy = CollectionUtilities.deepCopyContainers(treeSet);
        
        assertNotSame(treeSet, treeCopy);
        assertTrue(treeCopy instanceof TreeSet);
        assertEquals(treeSet, treeCopy);
        
        // Test HashSet becomes LinkedHashSet
        HashSet<String> hashSet = new HashSet<>(Arrays.asList("x", "y", "z"));
        Set<String> hashCopy = CollectionUtilities.deepCopyContainers(hashSet);
        
        assertNotSame(hashSet, hashCopy);
        assertTrue(hashCopy instanceof LinkedHashSet);
        assertEquals(hashSet, hashCopy);
    }

    @Test
    void testDeepCopyContainers_TreeSetWithCustomComparator() {
        // Test TreeSet with custom comparator (reverse order)
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        TreeSet<String> treeSetWithComparator = new TreeSet<>(reverseComparator);
        treeSetWithComparator.addAll(Arrays.asList("apple", "zebra", "banana"));
        
        TreeSet<String> copy = CollectionUtilities.deepCopyContainers(treeSetWithComparator);
        
        // Should be different instance
        assertNotSame(treeSetWithComparator, copy);
        
        // Should preserve the comparator
        assertNotNull(copy.comparator());
        assertEquals(treeSetWithComparator.comparator(), copy.comparator());
        
        // Should maintain the same ordering
        assertEquals(treeSetWithComparator.first(), copy.first());
        assertEquals(treeSetWithComparator.last(), copy.last());
        assertEquals(treeSetWithComparator, copy);
        
        // Verify reverse order is maintained
        String[] originalOrder = treeSetWithComparator.toArray(new String[0]);
        String[] copyOrder = copy.toArray(new String[0]);
        assertEquals("zebra", originalOrder[0]);
        assertEquals("zebra", copyOrder[0]);
        assertEquals("apple", originalOrder[2]);
        assertEquals("apple", copyOrder[2]);
        
        // Test TreeSet with null comparator (natural ordering)
        TreeSet<String> naturalOrderSet = new TreeSet<>();
        naturalOrderSet.addAll(Arrays.asList("charlie", "alpha", "bravo"));
        
        TreeSet<String> naturalCopy = CollectionUtilities.deepCopyContainers(naturalOrderSet);
        
        assertNotSame(naturalOrderSet, naturalCopy);
        assertNull(naturalCopy.comparator()); // Should be null for natural ordering
        assertEquals(naturalOrderSet, naturalCopy);
        assertEquals("alpha", naturalCopy.first());
        assertEquals("charlie", naturalCopy.last());
    }

    @Test
    void testDeepCopyContainers_CollectionWithArrays() {
        String[] array1 = {"x", "y"};
        String[] array2 = {"p", "q", "r"};
        List<Object> original = Arrays.asList(array1, array2, "standalone");
        
        List<Object> copy = CollectionUtilities.deepCopyContainers(original);
        
        assertNotSame(original, copy);
        
        // Arrays should also be deep copied (containers)
        assertNotSame(original.get(0), copy.get(0));
        assertNotSame(original.get(1), copy.get(1));
        
        // But the standalone string should be the same
        assertSame("standalone", copy.get(2));
        
        // Array content should be equal
        assertArrayEquals(array1, (String[])copy.get(0));
        assertArrayEquals(array2, (String[])copy.get(1));
    }

    @Test
    void testDeepCopyContainers_MapAsBerry() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        
        List<Object> original = Arrays.asList(map, "text", Arrays.asList("x", "y"));
        List<Object> copy = CollectionUtilities.deepCopyContainers(original);
        
        assertNotSame(original, copy);
        
        // Map should be same reference (berry)
        assertSame(map, copy.get(0));
        
        // String is also a berry
        assertSame("text", copy.get(1));
        
        // But nested list is copied
        assertNotSame(original.get(2), copy.get(2));
        assertEquals(original.get(2), copy.get(2));
    }

    @Test
    void testDeepCopyContainers_NullHandling() {
        assertNull(CollectionUtilities.deepCopyContainers(null));
        
        List<String> original = Arrays.asList("a", null, "c");
        List<String> copy = CollectionUtilities.deepCopyContainers(original);
        
        assertNotSame(original, copy);
        assertEquals(original, copy);
        assertNull(copy.get(1));
    }

    @Test
    void testDeepCopyContainers_EmptyCollections() {
        List<String> emptyList = new ArrayList<>();
        List<String> copyList = CollectionUtilities.deepCopyContainers(emptyList);
        
        assertNotSame(emptyList, copyList);
        assertEquals(0, copyList.size());
        
        Set<String> emptySet = new HashSet<>();
        Set<String> copySet = CollectionUtilities.deepCopyContainers(emptySet);
        
        assertNotSame(emptySet, copySet);
        assertEquals(0, copySet.size());
    }

    @Test
    void testDeepCopyContainers_CircularReference() {
        List<Object> list1 = new ArrayList<>();
        List<Object> list2 = new ArrayList<>();
        
        list1.add("a");
        list1.add(list2);
        list2.add("b");
        list2.add(list1);  // Circular reference
        
        List<Object> copy = CollectionUtilities.deepCopyContainers(list1);
        
        assertNotSame(list1, copy);
        assertEquals("a", copy.get(0));
        
        List<Object> copiedList2 = (List<Object>) copy.get(1);
        assertNotSame(list2, copiedList2);
        assertEquals("b", copiedList2.get(0));
        
        // Verify circular structure is maintained
        assertSame(copy, copiedList2.get(1));
    }

    @Test
    void testDeepCopyContainers_NonContainer() {
        // Non-containers return same reference
        String text = "hello";
        assertSame(text, CollectionUtilities.deepCopyContainers(text));
        
        Integer number = 42;
        assertSame(number, CollectionUtilities.deepCopyContainers(number));
        
        Map<String, String> map = new HashMap<>();
        assertSame(map, CollectionUtilities.deepCopyContainers(map));
    }

    @Test
    void testDeepCopyContainers_ComplexNestedStructure() {
        // Create complex nested structure with arrays, lists, and sets
        List<Object> innerList = Arrays.asList("x", "y");
        Set<Object> innerSet = new HashSet<>(Arrays.asList(1, 2, 3));
        String[] innerArray = {"p", "q"};
        
        List<Object> complex = new ArrayList<>();
        complex.add(innerList);
        complex.add(innerSet);
        complex.add(innerArray);
        complex.add(Arrays.asList(innerArray, innerList, innerSet));
        
        List<Object> copy = CollectionUtilities.deepCopyContainers(complex);
        
        // Everything should be deep copied
        assertNotSame(complex, copy);
        assertNotSame(complex.get(0), copy.get(0));
        assertNotSame(complex.get(1), copy.get(1));
        assertNotSame(complex.get(2), copy.get(2));
        assertNotSame(complex.get(3), copy.get(3));
        
        // Verify content equality
        assertEquals(innerList, copy.get(0));
        assertEquals(innerSet, copy.get(1));
        assertArrayEquals(innerArray, (String[])copy.get(2));
        
        // Nested list should also have deep copied contents
        List<Object> nestedCopy = (List<Object>) copy.get(3);
        assertNotSame(innerArray, nestedCopy.get(0));
        assertNotSame(innerList, nestedCopy.get(1));
        assertNotSame(innerSet, nestedCopy.get(2));
    }

    private void assertArrayEquals(String[] expected, String[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }

    @Test
    void testDeepCopyContainers_EnumSet() {
        // Test EnumSet copy
        EnumSet<Color> original = EnumSet.of(Color.RED, Color.BLUE, Color.GREEN);
        EnumSet<Color> copy = CollectionUtilities.deepCopyContainers(original);
        
        assertNotSame(original, copy);
        assertTrue(copy instanceof EnumSet);
        assertEquals(original, copy);
        
        // Test empty EnumSet
        EnumSet<Color> emptyOriginal = EnumSet.noneOf(Color.class);
        EnumSet<Color> emptyCopy = CollectionUtilities.deepCopyContainers(emptyOriginal);
        
        assertNotSame(emptyOriginal, emptyCopy);
        assertTrue(emptyCopy instanceof EnumSet);
        assertEquals(emptyOriginal, emptyCopy);
        assertEquals(0, emptyCopy.size());
        
        // Test EnumSet with all elements
        EnumSet<Color> fullOriginal = EnumSet.allOf(Color.class);
        EnumSet<Color> fullCopy = CollectionUtilities.deepCopyContainers(fullOriginal);
        
        assertNotSame(fullOriginal, fullCopy);
        assertTrue(fullCopy instanceof EnumSet);
        assertEquals(fullOriginal, fullCopy);
        assertEquals(5, fullCopy.size());
    }

    @Test
    void testDeepCopyContainers_QueueAndDeque() {
        // Test Queue normalization - ArrayDeque is a Deque, so it becomes LinkedList
        Queue<String> queue = new ArrayDeque<>();
        queue.offer("first");
        queue.offer("second");
        queue.offer("third");
        
        Collection<String> queueCopy = CollectionUtilities.deepCopyContainers(queue);
        
        assertNotSame(queue, queueCopy);
        assertTrue(queueCopy instanceof LinkedList); // ArrayDeque is a Deque, becomes LinkedList
        assertEquals(3, queueCopy.size());
        // Cast to List to access by index
        List<String> queueList = (List<String>) queueCopy;
        assertEquals("first", queueList.get(0));
        assertEquals("second", queueList.get(1));
        assertEquals("third", queueList.get(2));
        
        // Test Deque normalization to LinkedList
        Deque<Integer> deque = new ArrayDeque<>();
        deque.addFirst(1);
        deque.addLast(2);
        deque.addFirst(0);
        
        Collection<Integer> dequeCopy = CollectionUtilities.deepCopyContainers(deque);
        
        assertNotSame(deque, dequeCopy);
        assertTrue(dequeCopy instanceof LinkedList); // Deque becomes LinkedList
        assertEquals(3, dequeCopy.size());
        // Order preserved as it appears in iteration
        List<Integer> dequeList = (List<Integer>) dequeCopy;
        int index = 0;
        for (Integer val : deque) {
            assertEquals(val, dequeList.get(index++));
        }
    }

    @Test
    void testDeepCopyContainers_PrimitiveArrayAsRoot() {
        // Test that primitive arrays are correctly copied when they're the root
        int[] primitiveArray = {1, 2, 3, 4, 5};
        int[] copy = CollectionUtilities.deepCopyContainers(primitiveArray);
        
        assertNotSame(primitiveArray, copy);
        assertArrayEquals(primitiveArray, copy);
        
        // Modify original to ensure they're independent
        primitiveArray[0] = 99;
        assertEquals(1, copy[0]); // Copy should be unchanged
        
        // Test with double array
        double[] doubleArray = {1.1, 2.2, 3.3};
        double[] doubleCopy = CollectionUtilities.deepCopyContainers(doubleArray);
        
        assertNotSame(doubleArray, doubleCopy);
        assertEquals(doubleArray.length, doubleCopy.length);
        for (int i = 0; i < doubleArray.length; i++) {
            assertEquals(doubleArray[i], doubleCopy[i], 0.0001);
        }
        
        // Test with boolean array
        boolean[] boolArray = {true, false, true};
        boolean[] boolCopy = CollectionUtilities.deepCopyContainers(boolArray);
        
        assertNotSame(boolArray, boolCopy);
        for (int i = 0; i < boolArray.length; i++) {
            assertEquals(boolArray[i], boolCopy[i]);
        }
    }
    
    @Test
    void testDeepCopyContainers_DequePreservation() {
        // Test that Deque is preserved as LinkedList with deque operations
        ArrayDeque<String> deque = new ArrayDeque<>();
        deque.addFirst("first");
        deque.addLast("middle");
        deque.addLast("last");
        
        Deque<String> copy = CollectionUtilities.deepCopyContainers(deque);
        
        assertNotSame(deque, copy);
        assertTrue(copy instanceof LinkedList);
        assertEquals(3, copy.size());
        
        // Verify deque operations work
        assertEquals("first", copy.removeFirst());
        assertEquals("last", copy.removeLast());
        assertEquals("middle", copy.peek());
        
        // Test with null elements (LinkedList supports nulls, ArrayDeque doesn't)
        LinkedList<String> linkedDeque = new LinkedList<>();
        linkedDeque.add("a");
        linkedDeque.add(null);
        linkedDeque.add("b");
        
        LinkedList<String> linkedCopy = CollectionUtilities.deepCopyContainers(linkedDeque);
        assertNotSame(linkedDeque, linkedCopy);
        // LinkedList is both Deque and List - Deque check comes first, so it becomes LinkedList
        assertTrue(linkedCopy instanceof LinkedList);
        assertEquals(3, linkedCopy.size());
        assertNull(linkedCopy.get(1)); // Verify null was preserved
    }
    
    @Test
    void testDeepCopyContainers_PriorityQueuePreservation() {
        // Test with natural ordering
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        pq.addAll(Arrays.asList(5, 1, 3, 2, 4));
        
        PriorityQueue<Integer> copy = CollectionUtilities.deepCopyContainers(pq);
        
        assertNotSame(pq, copy);
        assertTrue(copy instanceof PriorityQueue);
        assertEquals(pq.size(), copy.size());
        
        // Verify priority ordering is preserved
        assertEquals(Integer.valueOf(1), copy.poll());
        assertEquals(Integer.valueOf(2), copy.poll());
        
        // Test with custom comparator
        PriorityQueue<String> pqWithComparator = new PriorityQueue<>(Comparator.reverseOrder());
        pqWithComparator.addAll(Arrays.asList("apple", "zebra", "banana"));
        
        PriorityQueue<String> copyWithComparator = CollectionUtilities.deepCopyContainers(pqWithComparator);
        
        assertNotSame(pqWithComparator, copyWithComparator);
        assertNotNull(copyWithComparator.comparator());
        assertEquals(pqWithComparator.comparator(), copyWithComparator.comparator());
        
        // Verify reverse ordering is preserved
        assertEquals("zebra", copyWithComparator.poll());
        assertEquals("banana", copyWithComparator.poll());
        assertEquals("apple", copyWithComparator.poll());
    }
    
    @Test
    void testDeepCopyContainers_OtherQueueTypes() {
        // Test that other Queue types become LinkedList
        Queue<String> queue = new LinkedList<>();
        queue.offer("first");
        queue.offer("second");
        
        Queue<String> copy = CollectionUtilities.deepCopyContainers(queue);
        
        assertNotSame(queue, copy);
        assertTrue(copy instanceof LinkedList);
        
        // Verify queue operations work
        assertEquals("first", copy.poll());
        assertEquals("second", copy.poll());
        assertNull(copy.poll());
    }
    
    private void assertArrayEquals(int[] expected, int[] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i]);
        }
    }
}