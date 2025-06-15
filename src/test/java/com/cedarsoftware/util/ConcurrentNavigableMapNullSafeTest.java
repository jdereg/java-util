package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 Test Suite for ConcurrentNavigableMapNullSafe.
 * This test suite exercises all public methods of ConcurrentNavigableMapNullSafe,
 * ensuring correct behavior, including handling of null keys and values,
 * as well as navigational capabilities.
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
class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setUp() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }
    
    @Test
    void testPutAndGet() {
        // Test normal insertion
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertEquals(3, map.get("three"));

        // Test updating existing key
        map.put("one", 10);
        assertEquals(10, map.get("one"));

        // Test inserting null key
        map.put(null, 100);
        assertEquals(100, map.get(null));

        // Test inserting null value
        map.put("four", null);
        assertNull(map.get("four"));
    }

    @Test
    void testRemove() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        // Remove existing key
        assertEquals(1, map.remove("one"));
        assertNull(map.get("one"));
        assertEquals(2, map.size());

        // Remove non-existing key
        assertNull(map.remove("three"));
        assertEquals(2, map.size());

        // Remove null key
        assertEquals(100, map.remove(null));
        assertNull(map.get(null));
        assertEquals(1, map.size());
    }

    @Test
    void testContainsKey() {
        map.put("one", 1);
        map.put(null, 100);

        assertTrue(map.containsKey("one"));
        assertTrue(map.containsKey(null));
        assertFalse(map.containsKey("two"));
    }

    @Test
    void testContainsValue() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", null);

        assertTrue(map.containsValue(1));
        assertTrue(map.containsValue(2));
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue(3));
    }

    @Test
    void testSizeAndIsEmpty() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("one", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.put(null, null);
        assertEquals(2, map.size());

        map.remove("one");
        map.remove(null);
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    void testClear() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        assertFalse(map.isEmpty());
        assertEquals(3, map.size());

        map.clear();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("one"));
        assertNull(map.get(null));
    }

    @Test
    void testPutIfAbsent() {
        // Put if absent on new key
        assertNull(map.putIfAbsent("one", 1));
        assertEquals(1, map.get("one"));

        // Put if absent on existing key
        assertEquals(1, map.putIfAbsent("one", 10));
        assertEquals(1, map.get("one"));

        // Put if absent with null key
        assertNull(map.putIfAbsent(null, 100));
        assertEquals(100, map.get(null));

        // Attempt to put if absent with existing null key
        assertEquals(100, map.putIfAbsent(null, 200));
        assertEquals(100, map.get(null));
    }

    @Test
    void testReplace() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        // Replace existing key
        assertEquals(1, map.replace("one", 10));
        assertEquals(10, map.get("one"));

        // Replace non-existing key
        assertNull(map.replace("three", 3));
        assertFalse(map.containsKey("three"));

        // Replace with null value
        assertEquals(2, map.replace("two", null));
        assertNull(map.get("two"));

        // Replace null key
        assertEquals(100, map.replace(null, 200));
        assertEquals(200, map.get(null));
    }

    @Test
    void testReplaceWithCondition() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        // Successful replace
        assertTrue(map.replace("one", 1, 10));
        assertEquals(10, map.get("one"));

        // Unsuccessful replace due to wrong old value
        assertFalse(map.replace("one", 1, 20));
        assertEquals(10, map.get("one"));

        // Replace with null value condition
        assertFalse(map.replace("two", 3, 30));
        assertEquals(2, map.get("two"));

        // Replace null key with correct old value
        assertTrue(map.replace(null, 100, 200));
        assertEquals(200, map.get(null));

        // Replace null key with wrong old value
        assertFalse(map.replace(null, 100, 300));
        assertEquals(200, map.get(null));
    }

    @Test
    void testRemoveWithCondition() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, null);

        // Successful removal
        assertTrue(map.remove("one", 1));
        assertFalse(map.containsKey("one"));

        // Unsuccessful removal due to wrong value
        assertFalse(map.remove("two", 3));
        assertTrue(map.containsKey("two"));

        // Remove null key with correct value
        assertTrue(map.remove(null, null));
        assertFalse(map.containsKey(null));

        // Attempt to remove null key with wrong value
        map.put(null, 100);
        assertFalse(map.remove(null, null));
        assertTrue(map.containsKey(null));
    }

    @Test
    void testComputeIfAbsent() {
        // Test with non-existent key
        assertEquals(1, map.computeIfAbsent("one", k -> 1));
        assertEquals(1, map.get("one"));

        // Test with existing key (should not compute)
        assertEquals(1, map.computeIfAbsent("one", k -> 2));
        assertEquals(1, map.get("one"));

        // Test with null key
        assertEquals(100, map.computeIfAbsent(null, k -> 100));
        assertEquals(100, map.get(null));

        // Test where mapping function returns null for non-existent key
        assertNull(map.computeIfAbsent("nullValue", k -> null));
        assertFalse(map.containsKey("nullValue"));

        // Ensure mapping function is not called for existing non-null values
        AtomicInteger callCount = new AtomicInteger(0);
        map.computeIfAbsent("one", k -> {
            callCount.incrementAndGet();
            return 5;
        });
        assertEquals(0, callCount.get());
        assertEquals(1, map.get("one")); // Value should remain unchanged

        // Test with existing key mapped to null value
        map.put("existingNull", null);
        assertEquals(10, map.computeIfAbsent("existingNull", k -> 10));
        assertEquals(10, map.get("existingNull")); // New value should be computed and set

        // Test with existing key mapped to non-null value
        map.put("existingNonNull", 20);
        assertEquals(20, map.computeIfAbsent("existingNonNull", k -> 30)); // Should return existing value
        assertEquals(20, map.get("existingNonNull")); // Value should remain unchanged

        // Test computing null for existing null value (should remove the entry)
        map.put("removeMe", null);
        assertNull(map.computeIfAbsent("removeMe", k -> null));
        assertFalse(map.containsKey("removeMe"));
    }

    @Test
    void testCompute() {
        // Compute on new key
        assertEquals(1, map.compute("one", (k, v) -> v == null ? 1 : v + 1));
        assertEquals(1, map.get("one"));

        // Compute on existing key
        assertEquals(2, map.compute("one", (k, v) -> v + 1));
        assertEquals(2, map.get("one"));

        // Compute to remove entry
        map.put("one", 0);
        assertNull(map.compute("one", (k, v) -> null));
        assertFalse(map.containsKey("one"));

        // Compute with null key
        assertEquals(100, map.compute(null, (k, v) -> 100));
        assertEquals(100, map.get(null));

        // Compute with null value
        map.put("two", null);
        assertEquals(0, map.compute("two", (k, v) -> v == null ? 0 : v + 1));
        assertEquals(0, map.get("two"));
    }

    @Test
    void testMerge() {
        // Merge on new key
        assertEquals(1, map.merge("one", 1, Integer::sum));
        assertEquals(1, map.get("one"));

        // Merge on existing key
        assertEquals(3, map.merge("one", 2, Integer::sum));
        assertEquals(3, map.get("one"));

        // Merge to update value to 0 (does not remove the key)
        assertEquals(0, map.merge("one", -3, Integer::sum));
        assertEquals(0, map.get("one"));
        assertTrue(map.containsKey("one")); // Key should still exist

        // Merge with remapping function that removes the key when sum is 0
        assertNull(map.merge("one", 0, (oldVal, newVal) -> (oldVal + newVal) == 0 ? null : oldVal + newVal));
        assertFalse(map.containsKey("one")); // Key should be removed

        // Merge with null key
        assertEquals(100, map.merge(null, 100, Integer::sum));
        assertEquals(100, map.get(null));

        // Merge with existing null key
        assertEquals(200, map.merge(null, 100, Integer::sum));
        assertEquals(200, map.get(null));

        // Merge with null value
        map.put("two", null);
        assertEquals(0, map.merge("two", 0, (oldVal, newVal) -> oldVal == null ? newVal : oldVal + newVal));
        assertEquals(0, map.get("two"));
    }

    @Test
    void testFirstKeyAndLastKey() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        assertEquals("apple", map.firstKey()); // "apple" is the first key
        assertEquals(null, map.lastKey());     // Null key is considered greater than any other key
    }
    
    @Test
    void testNavigableKeySet() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        NavigableSet<String> keySet = map.navigableKeySet();
        Iterator<String> it = keySet.iterator();

        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }


    @Test
    void testDescendingKeySet() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        NavigableSet<String> keySet = map.descendingKeySet();
        Iterator<String> it = keySet.iterator();

        assertEquals(null, it.next());
        assertEquals("cherry", it.next());
        assertEquals("banana", it.next());
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
    }


    @Test
    void testKeySetDescendingIterator() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        Iterator<String> it = map.keySet().descendingIterator();

        assertEquals(null, it.next());
        it.remove();
        assertFalse(map.containsKey(null));

        assertEquals("cherry", it.next());
        it.remove();
        assertFalse(map.containsKey("cherry"));

        assertEquals("banana", it.next());
        it.remove();
        assertFalse(map.containsKey("banana"));

        assertEquals("apple", it.next());
        it.remove();
        assertFalse(it.hasNext());
        assertTrue(map.isEmpty());
    }


    @Test
    void testSubMap() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put("date", 4);
        map.put(null, 0);

        ConcurrentNavigableMap<String, Integer> subMap = map.subMap("banana", true, "date", false);
        assertEquals(2, subMap.size());
        assertTrue(subMap.containsKey("banana"));
        assertTrue(subMap.containsKey("cherry"));
        assertFalse(subMap.containsKey("apple"));
        assertFalse(subMap.containsKey("date"));
        assertFalse(subMap.containsKey(null));
    }

    @Test
    void testHeadMap() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        ConcurrentNavigableMap<String, Integer> headMap = map.headMap("cherry", false);
        assertEquals(2, headMap.size());
        assertTrue(headMap.containsKey("apple"));
        assertTrue(headMap.containsKey("banana"));
        assertFalse(headMap.containsKey("cherry"));
        assertFalse(headMap.containsKey(null));
    }


    @Test
    void testTailMap() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put("date", 4);
        map.put(null, 0);

        ConcurrentNavigableMap<String, Integer> tailMap = map.tailMap("banana", true);
        assertEquals(4, tailMap.size());
        assertTrue(tailMap.containsKey("banana"));
        assertTrue(tailMap.containsKey("cherry"));
        assertTrue(tailMap.containsKey("date"));
        assertFalse(tailMap.containsKey("apple"));
        assertTrue(tailMap.containsKey(null));
    }

    @Test
    void testCeilingKey() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        assertEquals("apple", map.ceilingKey("aardvark"));
        assertEquals("banana", map.ceilingKey("banana"));
        assertEquals(null, map.ceilingKey(null));
        assertNull(map.ceilingKey("daisy"));
    }

    @Test
    void testFloorKey() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        assertEquals(null, map.floorKey("aardvark"));
        assertEquals("banana", map.floorKey("banana"));
        assertEquals("cherry", map.floorKey("daisy"));
        assertEquals(null, map.floorKey(null));
    }

    @Test
    void testLowerKey() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        assertEquals(null, map.lowerKey("apple"));           // No key less than "apple"
        assertEquals("apple", map.lowerKey("banana"));       // "apple" is less than "banana"
        assertEquals("banana", map.lowerKey("cherry"));      // "banana" is less than "cherry"
        assertEquals("cherry", map.lowerKey("date"));        // "cherry" is less than "date"
        assertEquals("cherry", map.lowerKey(null));          // "cherry" is less than null
    }

    @Test
    void testHigherKey() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        assertNull(map.higherKey(null));                // No key higher than null
        assertEquals("banana", map.higherKey("apple")); // Correct
        assertEquals("cherry", map.higherKey("banana"));// Correct
        assertEquals(null, map.higherKey("cherry"));    // Null key is higher than "cherry"
    }

    @Test
    void testFirstEntryAndLastEntry() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put(null, 0);

        Map.Entry<String, Integer> firstEntry = map.firstEntry();
        Map.Entry<String, Integer> lastEntry = map.lastEntry();

        assertEquals("apple", firstEntry.getKey());
        assertEquals(1, firstEntry.getValue());

        assertEquals(null, lastEntry.getKey());
        assertEquals(0, lastEntry.getValue());
    }

    @Test
    void testPollFirstEntryAndPollLastEntry() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put(null, 0);

        // Poll the first entry (should be "apple")
        Map.Entry<String, Integer> firstEntry = map.pollFirstEntry();
        assertEquals("apple", firstEntry.getKey());
        assertEquals(1, firstEntry.getValue());
        assertFalse(map.containsKey("apple"));

        // Poll the last entry (should be null)
        Map.Entry<String, Integer> lastEntry = map.pollLastEntry();
        assertEquals(null, lastEntry.getKey());
        assertEquals(0, lastEntry.getValue());
        assertFalse(map.containsKey(null));

        // Only "banana" should remain
        assertEquals(1, map.size());
        assertTrue(map.containsKey("banana"));
    }

    @Test
    void testDescendingMap() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);
        map.put(null, 0);

        ConcurrentNavigableMap<String, Integer> descendingMap = map.descendingMap();
        Iterator<Map.Entry<String, Integer>> it = descendingMap.entrySet().iterator();

        Map.Entry<String, Integer> firstEntry = it.next();
        assertEquals(null, firstEntry.getKey());
        assertEquals(0, firstEntry.getValue());

        Map.Entry<String, Integer> secondEntry = it.next();
        assertEquals("cherry", secondEntry.getKey());
        assertEquals(3, secondEntry.getValue());

        Map.Entry<String, Integer> thirdEntry = it.next();
        assertEquals("banana", thirdEntry.getKey());
        assertEquals(2, thirdEntry.getValue());

        Map.Entry<String, Integer> fourthEntry = it.next();
        assertEquals("apple", fourthEntry.getKey());
        assertEquals(1, fourthEntry.getValue());

        assertFalse(it.hasNext());
    }

    @Test
    void testSubMapViewModification() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put("cherry", 3);

        ConcurrentNavigableMap<String, Integer> subMap = map.subMap("apple", true, "cherry", false);
        assertEquals(2, subMap.size());

        // Adding a key outside the submap's range should throw an exception
        assertThrows(IllegalArgumentException.class, () -> subMap.put("aardvark", 0));

        // Verify that "aardvark" was not added to the main map
        assertFalse(map.containsKey("aardvark"));

        // Remove a key within the submap's range
        subMap.remove("banana");
        assertFalse(map.containsKey("banana"));
    }

    @Test
    void testNavigableKeySetPollMethods() {
        map.put("apple", 1);
        map.put("banana", 2);
        map.put(null, 0);

        NavigableSet<String> keySet = map.navigableKeySet();

        // Poll the first key (should be "apple")
        assertEquals("apple", keySet.pollFirst());
        assertFalse(map.containsKey("apple"));

        // Poll the next first key (should be "banana")
        assertEquals("banana", keySet.pollFirst());
        assertFalse(map.containsKey("banana"));

        // Poll the last key (should be null)
        assertEquals(null, keySet.pollLast());
        assertFalse(map.containsKey(null));

        assertTrue(map.isEmpty());
    }

    @Test
    void testConcurrentAccess() throws InterruptedException, ExecutionException {
        int numThreads = 10;
        int numIterations = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            tasks.add(() -> {
                for (int j = 0; j < numIterations; j++) {
                    String key = "key-" + (threadNum * numIterations + j);
                    map.put(key, j);
                    assertEquals(j, map.get(key));
                    if (j % 2 == 0) {
                        map.remove(key);
                        assertNull(map.get(key));
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get(); // Ensure all tasks completed successfully
        }

        executor.shutdown();

        // Verify final size (only odd iterations remain)
        int expectedSize = numThreads * numIterations / 2;
        assertEquals(expectedSize, map.size());
    }

    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    @Test
    void testHighConcurrency() throws InterruptedException, ExecutionException {
        int numThreads = 20;
        int numOperationsPerThread = 5000;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            tasks.add(() -> {
                for (int j = 0; j < numOperationsPerThread; j++) {
                    String key = "key-" + (threadNum * numOperationsPerThread + j);
                    map.put(key, j);
                    assertEquals(j, map.get(key));
                    if (j % 100 == 0) {
                        map.remove(key);
                        assertNull(map.get(key));
                    }
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get(); // Ensure all tasks completed successfully
        }

        executor.shutdown();

        // Verify final size
        int expectedSize = numThreads * numOperationsPerThread - (numThreads * (numOperationsPerThread / 100));
        assertEquals(expectedSize, map.size());
    }

    @Test
    void testConcurrentCompute() throws InterruptedException, ExecutionException {
        int numThreads = 10;
        int numIterations = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            tasks.add(() -> {
                for (int j = 0; j < numIterations; j++) {
                    String key = "counter";
                    map.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
                }
                return null;
            });
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get();
        }

        executor.shutdown();

        // The expected value is numThreads * numIterations
        assertEquals(numThreads * numIterations, map.get("counter"));
    }

    @Test
    void testNullKeysAndValues() {
        // Insert multiple null keys and values
        map.put(null, null);
        map.put("one", null);
        map.put(null, 1); // Overwrite null key
        map.put("two", 2);

        assertEquals(3, map.size());
        assertEquals(1, map.get(null));
        assertNull(map.get("one"));
        assertEquals(2, map.get("two"));

        // Remove null key
        map.remove(null);
        assertFalse(map.containsKey(null));
        assertEquals(2, map.size());
    }

    @Test
    void testLargeDataSet() {
        int numEntries = 100_000;
        for (int i = 0; i < numEntries; i++) {
            String key = String.format("%06d", i);
            Integer value = i;
            map.put(key, value);
        }

        assertEquals(numEntries, map.size());

        // Verify random entries
        assertEquals(500, map.get("000500"));
        assertEquals(99999, map.get("099999"));
        assertNull(map.get("100000")); // Non-existent key
    }

    @Test
    void testEqualsAndHashCode() {
        ConcurrentNavigableMapNullSafe<String, Integer> map1 = new ConcurrentNavigableMapNullSafe<>();
        ConcurrentNavigableMapNullSafe<String, Integer> map2 = new ConcurrentNavigableMapNullSafe<>();

        map1.put("one", 1);
        map1.put("two", 2);
        map1.put(null, 100);

        map2.put("one", 1);
        map2.put("two", 2);
        map2.put(null, 100);

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());

        // Modify map2
        map2.put("three", 3);
        assertNotEquals(map1, map2);
        assertNotEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    void testToString() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        String mapString = map.toString();
        assertTrue(mapString.contains("one=1"));
        assertTrue(mapString.contains("two=2"));
        assertTrue(mapString.contains("null=100"));
    }

    @Test
    void testGetOrDefault() {
        map.put("one", 1);
        map.put(null, null);

        // Existing key with non-null value
        assertEquals(1, map.getOrDefault("one", 10));

        // Existing key with null value
        map.put("two", null);
        assertNull(map.getOrDefault("two", 20));

        // Non-existing key
        assertEquals(30, map.getOrDefault("three", 30));

        // Null key with null value
        assertNull(map.getOrDefault(null, 40));

        // Null key with non-null value
        map.put(null, 50);
        assertEquals(50, map.getOrDefault(null, 60));
    }
}
