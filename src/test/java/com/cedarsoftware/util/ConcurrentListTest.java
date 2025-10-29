package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static com.cedarsoftware.util.DeepEquals.deepEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentListTest {

    @Test
    void testAddAndSize() {
        List<Integer> list = new ConcurrentList<>();
        assertTrue(list.isEmpty(), "List should be initially empty");

        list.add(1);
        assertFalse(list.isEmpty(), "List should not be empty after add");
        assertEquals(1, list.size(), "List size should be 1 after adding one element");

        list.add(2);
        assertEquals(2, list.size(), "List size should be 2 after adding another element");
    }

    @Test
    void testSetAndGet() {
        List<Integer> list = new ConcurrentList<>();
        list.add(1);
        list.add(2);

        list.set(1, 3);
        assertEquals(3, list.get(1), "Element at index 1 should be updated to 3");
    }

    @Test
    void testAddAll() {
        List<Integer> list = new ConcurrentList<>();
        List<Integer> toAdd = new ArrayList<>(Arrays.asList(1, 2, 3));

        list.addAll(toAdd);
        assertEquals(3, list.size(), "List should contain all added elements");
    }

    @Test
    void testRemove() {
        List<Integer> list = new ConcurrentList<>();
        list.add(1);
        list.add(2);

        assertTrue(list.remove(Integer.valueOf(1)), "Element should be removed successfully");
        assertEquals(1, list.size(), "List size should decrease after removal");
        assertFalse(list.contains(1), "List should not contain removed element");
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testConcurrency() throws InterruptedException {
        List<Integer> list = new ConcurrentList<>();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        int numberOfAdds = 1000;

        // Add elements in parallel
        for (int i = 0; i < numberOfAdds; i++) {
            int finalI = i;
            executor.submit(() -> list.add(finalI));
        }

        // Shutdown executor and wait for all tasks to complete
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.MINUTES), "Tasks did not complete in time");

        // Check the list size after all additions
        assertEquals(numberOfAdds, list.size(), "List size should match the number of added elements");

        // Check if all elements were added
        for (int i = 0; i < numberOfAdds; i++) {
            assertTrue(list.contains(i), "List should contain the element added by the thread");
        }
    }

    @Test
    void testSubList() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));

        List<Integer> subList = null;
        assertThrows(UnsupportedOperationException.class, () -> list.subList(1, 4));
    }

    @Test
    void testClearAndIsEmpty() {
        List<Integer> list = new ConcurrentList<>();
        list.add(1);
        list.clear();
        assertTrue(list.isEmpty(), "List should be empty after clear operation");
    }

    @Test
    void testIterator() {
        List<Integer> list = new ConcurrentList<>();
        list.add(1);
        list.add(2);
        list.add(3);

        int sum = 0;
        for (Integer value : list) {
            sum += value;
        }
        assertEquals(6, sum, "Sum of elements should be equal to the sum of 1, 2, and 3");
    }

    @Test
    void testIndexOf() {
        List<Integer> list = new ConcurrentList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(2);

        assertEquals(1, list.indexOf(2), "Index of the first occurrence of 2 should be 1");
        assertEquals(3, list.lastIndexOf(2), "Index of the last occurrence of 2 should be 3");
    }

    @Test
    void testAddRemoveAndSize() {
        List<Integer> list = new ConcurrentList<>();
        assertTrue(list.isEmpty(), "List should be initially empty");

        list.add(1);
        list.add(2);
        assertFalse(list.isEmpty(), "List should not be empty after additions");
        assertEquals(2, list.size(), "List size should be 2 after adding two elements");

        list.remove(Integer.valueOf(1));
        assertTrue(list.contains(2) && !list.contains(1), "List should contain 2 but not 1 after removal");
        assertEquals(1, list.size(), "List size should be 1 after removing one element");

        list.add(3);
        list.add(3);
        assertTrue(list.remove(Integer.valueOf(3)), "First occurrence of 3 should be removed");
        assertEquals(2, list.size(), "List should be 2 after removing one occurrence of 3");
    }

    @Test
    void testRetainAll() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));

        list.retainAll(Arrays.asList(1, 2, 3));
        assertEquals(3, list.size(), "List should only retain elements 1, 2, and 3");
        assertTrue(list.containsAll(Arrays.asList(1, 2, 3)), "List should contain 1, 2, and 3");
        assertFalse(list.contains(4) || list.contains(5), "List should not contain 4 or 5");
    }

    @Test
    void testRemoveAll() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));

        list.removeAll(Arrays.asList(4, 5));
        assertEquals(3, list.size(), "List should have size 3 after removing 4 and 5");
        assertFalse(list.contains(4) || list.contains(5), "List should not contain 4 or 5");
    }

    @Test
    void testContainsAll() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));

        assertTrue(list.containsAll(Arrays.asList(1, 2, 3)), "List should contain 1, 2, and 3");
        assertFalse(list.containsAll(Arrays.asList(6, 7)), "List should not contain 6 or 7");
    }

    @Test
    void testToArray() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));

        Object[] array = list.toArray();
        assertArrayEquals(new Object[]{1, 2, 3, 4, 5}, array, "toArray should return correct elements");

        Integer[] integerArray = new Integer[5];
        integerArray = list.toArray(integerArray);
        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5}, integerArray, "toArray(T[] a) should return correct elements");
    }

    @Test
    void testAddAtIndex() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 3, 4));

        // Test adding at start
        list.add(0, 0);
        assert deepEquals(Arrays.asList(0, 1, 3, 4), list);

        // Test adding at middle
        list.add(2, 2);
        assert deepEquals(Arrays.asList(0, 1, 2, 3, 4), list);

        // Test adding at end
        list.add(5, 5);
        assert deepEquals(Arrays.asList(0, 1, 2, 3, 4, 5), list);
    }

    @Test
    void testRemoveAtIndex() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(0, 1, 2, 3, 4));

        // Remove element at index 2 (which is '2')
        assertEquals(2, list.remove(2), "Element 2 should be removed from index 2");
        assert deepEquals(Arrays.asList(0, 1, 3, 4), list);

        // Remove element at index 0 (which is '0')
        assertEquals(0, list.remove(0), "Element 0 should be removed from index 0");
        assert deepEquals(Arrays.asList(1, 3, 4), list);

        // Remove element at last index (which is '4')
        assertEquals(4, list.remove(2), "Element 4 should be removed from the last index");
        assert deepEquals(Arrays.asList(1, 3), list);
    }

    @Test
    void testAddAllAtIndex() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 5));

        // Add multiple elements at start
        list.addAll(0, Arrays.asList(-1, 0));
        assert deepEquals(Arrays.asList(-1, 0, 1, 5), list);

        // Add multiple elements at middle
        list.addAll(2, Arrays.asList(2, 3, 4));
        assert deepEquals(Arrays.asList(-1, 0, 2, 3, 4, 1, 5), list);

        // Add multiple elements at end
        list.addAll(7, Arrays.asList(6, 7));
        assert deepEquals(Arrays.asList(-1, 0, 2, 3, 4, 1, 5, 6, 7), list);
    }

    /**
     * Test that null elements are properly preserved in toArray() methods.
     * With pollFirst/pollLast now using write locks, toArray() (which uses read lock)
     * cannot run concurrently with poll operations, so null values are safe.
     */
    @Test
    void testNullElementsInToArray() {
        // Test with nulls in various positions
        List<String> list = new ConcurrentList<>();
        list.add("a");
        list.add(null);
        list.add("b");
        list.add(null);
        list.add("c");

        // Test toArray()
        Object[] array = list.toArray();
        assertEquals(5, array.length, "toArray() should preserve all elements including nulls");
        assertEquals("a", array[0]);
        assertEquals(null, array[1]);
        assertEquals("b", array[2]);
        assertEquals(null, array[3]);
        assertEquals("c", array[4]);

        // Test toArray(T[])
        String[] typedArray = new String[5];
        String[] result = list.toArray(typedArray);
        assertEquals(5, result.length, "toArray(T[]) should preserve all elements including nulls");
        assertEquals("a", result[0]);
        assertEquals(null, result[1]);
        assertEquals("b", result[2]);
        assertEquals(null, result[3]);
        assertEquals("c", result[4]);

        // Test with null at start
        List<String> list2 = new ConcurrentList<>();
        list2.add(null);
        list2.add("x");
        list2.add("y");
        Object[] array2 = list2.toArray();
        assertEquals(3, array2.length, "Should preserve null at start");
        assertEquals(null, array2[0]);
        assertEquals("x", array2[1]);
        assertEquals("y", array2[2]);

        // Test with null at end
        List<String> list3 = new ConcurrentList<>();
        list3.add("x");
        list3.add("y");
        list3.add(null);
        Object[] array3 = list3.toArray();
        assertEquals(3, array3.length, "Should preserve null at end");
        assertEquals("x", array3[0]);
        assertEquals("y", array3[1]);
        assertEquals(null, array3[2]);

        // Test with all nulls
        List<String> list4 = new ConcurrentList<>();
        list4.add(null);
        list4.add(null);
        list4.add(null);
        Object[] array4 = list4.toArray();
        assertEquals(3, array4.length, "Should preserve all null elements");
        assertEquals(null, array4[0]);
        assertEquals(null, array4[1]);
        assertEquals(null, array4[2]);
    }
}
