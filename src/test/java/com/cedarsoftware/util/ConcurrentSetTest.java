package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class is used in conjunction with the Executor class.  Example
 * usage:<pre>
 * Executor exec = new Executor()
 * exec.execute("ls -l")
 * String result = exec.getOut()
 * </pre>
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
class ConcurrentSetTest {

    @Test
    void testAddAndRemove() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        assertTrue(set.add(1), "Should return true when adding a new element");
        assertTrue(set.contains(1), "Set should contain the element 1 after addition");
        assertEquals(1, set.size(), "Set size should be 1");

        assertFalse(set.add(1), "Should return false when adding a duplicate element");
        assertTrue(set.remove(1), "Should return true when removing an existing element");
        assertFalse(set.contains(1), "Set should not contain the element 1 after removal");
        assertTrue(set.isEmpty(), "Set should be empty after removing elements");
    }

    @Test
    void testAddAllAndRemoveAll() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));

        assertEquals(3, set.size(), "Set should have 3 elements after addAll");
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3)), "Set should contain all added elements");

        set.removeAll(Arrays.asList(1, 3));
        assertTrue(set.contains(2) && !set.contains(1) && !set.contains(3), "Set should only contain the element 2 after removeAll");
    }

    @Test
    void testRetainAll() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        set.retainAll(Arrays.asList(2, 3, 5));

        assertTrue(set.containsAll(Arrays.asList(2, 3, 5)), "Set should contain elements 2, 3, and 5");
        assertFalse(set.contains(1) || set.contains(4), "Set should not contain elements 1 and 4");
    }

    @Test
    void testClear() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        set.clear();

        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size(), "Set size should be 0 after clear");
    }

    @Test
    void testIterator() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));

        int sum = 0;
        for (Integer i : set) {
            sum += i;
        }
        assertEquals(6, sum, "Sum of elements should be 6");
    }

    @Test
    void testToArray() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));

        Object[] array = set.toArray();
        HashSet<Object> arrayContent = new HashSet<>(Arrays.asList(array));
        assertTrue(arrayContent.containsAll(Arrays.asList(1, 2, 3)), "Array should contain all the set elements");

        Integer[] intArray = new Integer[3];
        intArray = set.toArray(intArray);
        HashSet<Integer> intArrayContent = new HashSet<>(Arrays.asList(intArray));
        assertTrue(intArrayContent.containsAll(Arrays.asList(1, 2, 3)), "Integer array should contain all the set elements");
    }

    @Test
    void testIsEmptyAndSize() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        assertTrue(set.isEmpty(), "New set should be empty");

        set.add(1);
        assertFalse(set.isEmpty(), "Set should not be empty after adding an element");
        assertEquals(1, set.size(), "Size of set should be 1 after adding one element");
    }
}
