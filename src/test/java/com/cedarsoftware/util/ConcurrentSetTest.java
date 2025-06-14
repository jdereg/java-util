package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

    @Test
    void testNullSupport() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(null);
        assert set.size() == 1;
        set.add(null);
        assert set.size() == 1;

        Iterator<Integer> iterator = set.iterator();
        Object x = iterator.next();
        assert x == null;
        assert !iterator.hasNext();
    }

    @Test
    void testNullIteratorRemoveSupport() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(null);

        Iterator<Integer> iterator = set.iterator();
        iterator.next();
        iterator.remove();
        assert !iterator.hasNext();
    }

    @Test
    void testConcurrentModification() throws InterruptedException {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        int threadCount = 10;
        int itemsPerThread = 1000;
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int threadNum = i;
            new Thread(() -> {
                for (int j = 0; j < itemsPerThread; j++) {
                    set.add(threadNum * itemsPerThread + j);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(threadCount * itemsPerThread, set.size(), "Set should contain all added elements");
    }

    @Test
    void testConcurrentReads() throws InterruptedException {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));

        int threadCount = 5;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger totalSum = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                int sum = set.stream().mapToInt(Integer::intValue).sum();
                totalSum.addAndGet(sum);
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(75, totalSum.get(), "Sum should be correct across all threads");
    }

    @Test
    void testNullEquality() {
        ConcurrentSet<String> set1 = new ConcurrentSet<>();
        ConcurrentSet<String> set2 = new ConcurrentSet<>();

        set1.add(null);
        set2.add(null);

        assertEquals(set1, set2, "Sets with null should be equal");
        assertEquals(set1.hashCode(), set2.hashCode(), "Hash codes should be equal for sets with null");
    }

    @Test
    void testMixedNullAndNonNull() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add(null);
        set.add("a");
        set.add("b");

        assertEquals(3, set.size(), "Set should contain null and non-null elements");
        assertTrue(set.contains(null), "Set should contain null");
        assertTrue(set.contains("a"), "Set should contain 'a'");
        assertTrue(set.contains("b"), "Set should contain 'b'");

        set.remove(null);
        assertEquals(2, set.size(), "Set should have 2 elements after removing null");
        assertFalse(set.contains(null), "Set should not contain null after removal");
    }

    @Test
    void testRetainAllWithNull() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", null, "b", "c"));

        set.retainAll(Arrays.asList(null, "b"));

        assertEquals(2, set.size(), "Set should retain null and 'b'");
        assertTrue(set.contains(null), "Set should contain null");
        assertTrue(set.contains("b"), "Set should contain 'b'");
        assertFalse(set.contains("a"), "Set should not contain 'a'");
        assertFalse(set.contains("c"), "Set should not contain 'c'");
    }

    @Test
    void testToArrayWithNull() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", null, "b"));

        Object[] array = set.toArray();
        assertEquals(3, array.length, "Array should have 3 elements");
        assertTrue(Arrays.asList(array).contains(null), "Array should contain null");

        String[] strArray = set.toArray(new String[0]);
        assertEquals(3, strArray.length, "String array should have 3 elements");
        assertTrue(Arrays.asList(strArray).contains(null), "String array should contain null");
    }

    @Test
    void testConcurrentAddAndRemove() throws InterruptedException {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        int threadCount = 5;
        int operationsPerThread = 10000;
        CountDownLatch latch = new CountDownLatch(threadCount * 2);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    set.add(j);
                }
                latch.countDown();
            }).start();

            new Thread(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    set.remove(j);
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertTrue(set.size() >= 0 && set.size() <= operationsPerThread,
                "Set size should be between 0 and " + operationsPerThread);
    }

    @Test
    void testSerializationRoundTrip() throws Exception {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("hello");
        set.add(null);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(set);
        out.close();

        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bout.toByteArray()));
        ConcurrentSet<String> copy = (ConcurrentSet<String>) in.readObject();

        assertEquals(set, copy);
        assertNotSame(set, copy);
    }
}
