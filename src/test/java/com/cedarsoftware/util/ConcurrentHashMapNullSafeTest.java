package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit 5 Test Suite for ConcurrentHashMapNullSafe.
 * This test suite exercises all public methods of ConcurrentHashMapNullSafe,
 * ensuring correct behavior, including handling of null keys and values.
 */
class ConcurrentHashMapNullSafeTest {

    private ConcurrentHashMapNullSafe<String, Integer> map;

    @BeforeEach
    void setUp() {
        map = new ConcurrentHashMapNullSafe<>();
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
        assertEquals(0, map.merge("one", -3, (oldVal, newVal) -> oldVal + newVal));
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
    void testKeySet() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        Set<String> keys = map.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
        assertTrue(keys.contains(null));

        // Remove a key via keySet
        keys.remove("one");
        assertFalse(map.containsKey("one"));
        assertEquals(2, map.size());

        // Remove null key via keySet
        keys.remove(null);
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }

    @Test
    void testValues() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", null);

        Collection<Integer> values = map.values();
        assertEquals(3, values.size());

        int nullCount = 0;
        int oneCount = 0;
        int twoCount = 0;

        for (Integer val : values) {
            if (Objects.equals(val, 2)) {
                twoCount++;
            } else if (Objects.equals(val, 1)) {
                oneCount++;
            } else if (val == null) {
                nullCount++;
            }
        }

        assertEquals(1, nullCount);
        assertEquals(1, oneCount);
        assertEquals(1, twoCount);

        assertTrue(values.contains(null));
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertFalse(values.contains(3));
    }

    @Test
    void testEntrySet() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(3, entries.size());

        // Check for specific entries
        boolean containsOne = entries.stream().anyMatch(e -> "one".equals(e.getKey()) && Integer.valueOf(1).equals(e.getValue()));
        boolean containsTwo = entries.stream().anyMatch(e -> "two".equals(e.getKey()) && Integer.valueOf(2).equals(e.getValue()));
        boolean containsNull = entries.stream().anyMatch(e -> e.getKey() == null && Integer.valueOf(100).equals(e.getValue()));

        assertTrue(containsOne);
        assertTrue(containsTwo);
        assertTrue(containsNull);

        // Modify an entry
        for (Map.Entry<String, Integer> entry : entries) {
            if ("one".equals(entry.getKey())) {
                entry.setValue(10);
            }
        }
        assertEquals(10, map.get("one"));

        // Remove an entry via entrySet
        entries.removeIf(e -> "two".equals(e.getKey()));
        assertFalse(map.containsKey("two"));
        assertEquals(2, map.size());

        // Remove null key via entrySet
        entries.removeIf(e -> e.getKey() == null);
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }

    @Test
    void testPutAll() {
        Map<String, Integer> otherMap = new HashMap<>();
        otherMap.put("one", 1);
        otherMap.put("two", 2);
        otherMap.put(null, 100);
        otherMap.put("three", null);

        map.putAll(otherMap);

        assertEquals(4, map.size());
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertEquals(100, map.get(null));
        assertNull(map.get("three"));
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
    void testKeySetView() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, 100);

        Set<String> keys = map.keySet();
        assertEquals(4, keys.size());
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
        assertTrue(keys.contains("three"));
        assertTrue(keys.contains(null));

        // Modify the map via keySet
        keys.remove("two");
        assertFalse(map.containsKey("two"));
        assertEquals(3, map.size());

        keys.remove(null);
        assertFalse(map.containsKey(null));
        assertEquals(2, map.size());
    }

    @Test
    void testValuesView() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", null);

        Collection<Integer> values = map.values();
        assertEquals(4, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        assertTrue(values.contains(null));

        // Modify the map via values
        values.remove(2);
        assertFalse(map.containsKey("two"));
        assertEquals(3, map.size());

        values.remove(null);
        assertFalse(map.containsKey("four"));
        assertEquals(2, map.size());
    }

    @Test
    void testEntrySetView() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(3, entries.size());

        // Check for specific entries
        boolean containsOne = entries.stream().anyMatch(e -> "one".equals(e.getKey()) && Integer.valueOf(1).equals(e.getValue()));
        boolean containsTwo = entries.stream().anyMatch(e -> "two".equals(e.getKey()) && Integer.valueOf(2).equals(e.getValue()));
        boolean containsNull = entries.stream().anyMatch(e -> e.getKey() == null && Integer.valueOf(100).equals(e.getValue()));

        assertTrue(containsOne);
        assertTrue(containsTwo);
        assertTrue(containsNull);

        // Modify an entry
        for (Map.Entry<String, Integer> entry : entries) {
            if ("one".equals(entry.getKey())) {
                entry.setValue(10);
            }
        }
        assertEquals(10, map.get("one"));

        // Remove an entry via entrySet
        entries.removeIf(e -> "two".equals(e.getKey()));
        assertFalse(map.containsKey("two"));
        assertEquals(2, map.size());

        // Remove null key via entrySet
        entries.removeIf(e -> e.getKey() == null);
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }

    @Test
    void testHashCodeAndEquals() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        ConcurrentHashMapNullSafe<String, Integer> anotherMap = new ConcurrentHashMapNullSafe<>();
        anotherMap.put("one", 1);
        anotherMap.put("two", 2);
        anotherMap.put(null, 100);

        assertEquals(map, anotherMap);
        assertEquals(map.hashCode(), anotherMap.hashCode());

        // Modify one map
        anotherMap.put("three", 3);
        assertNotEquals(map, anotherMap);
        assertNotEquals(map.hashCode(), anotherMap.hashCode());
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
    void testComputeIfPresent() {
        // Test case 1: Compute on existing key
        map.put("key1", 10);
        Integer result1 = map.computeIfPresent("key1", (k, v) -> v + 5);
        assertEquals(15, result1);
        assertEquals(15, map.get("key1"));

        // Test case 2: Compute on non-existing key
        Integer result2 = map.computeIfPresent("key2", (k, v) -> v + 5);
        assertNull(result2);
        assertFalse(map.containsKey("key2"));

        // Test case 3: Compute to null (should remove the entry)
        map.put("key3", 20);
        Integer result3 = map.computeIfPresent("key3", (k, v) -> null);
        assertNull(result3);
        assertFalse(map.containsKey("key3"));

        // Test case 4: Compute with null key (should not throw exception)
        map.put(null, 30);
        Integer result4 = map.computeIfPresent(null, (k, v) -> v + 10);
        assertEquals(40, result4);
        assertEquals(40, map.get(null));

        // Test case 5: Compute with exception in remapping function
        map.put("key5", 50);
        assertThrows(RuntimeException.class, () ->
                map.computeIfPresent("key5", (k, v) -> { throw new RuntimeException("Test exception"); })
        );
        assertEquals(50, map.get("key5")); // Original value should remain unchanged

        // Test case 6: Ensure atomic operation (no concurrent modification)
        map.put("key6", 60);
        AtomicInteger callCount = new AtomicInteger(0);
        BiFunction<String, Integer, Integer> remappingFunction = (k, v) -> {
            callCount.incrementAndGet();
            return v + 1;
        };
        Integer result6 = map.computeIfPresent("key6", remappingFunction);
        assertEquals(61, result6);
        assertEquals(1, callCount.get());

        // Test case 7: Compute with null value (edge case)
        map.put("key7", null);
        Integer result7 = map.computeIfPresent("key7", (k, v) -> v == null ? 70 : v + 1);
        assertNull(result7); // Should not compute as the value is null
        assertNull(map.get("key7"));

        // Test case 8: Ensure correct behavior with ConcurrentModification
        map.put("key8", 80);
        Integer result8 = map.computeIfPresent("key8", (k, v) -> {
            map.put("newKey", 100); // Concurrent modification
            return v + 1;
        });
        assertEquals(81, result8);
        assertEquals(81, map.get("key8"));
        assertEquals(100, map.get("newKey"));
    }

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
            final int threadNum = i;
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

    static class CustomKey {
        private final String id;
        private final int number;

        CustomKey(String id, int number) {
            this.id = id;
            this.number = number;
        }

        // Getters, equals, and hashCode methods
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CustomKey)) return false;
            CustomKey that = (CustomKey) o;
            return number == that.number && Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, number);
        }
    }

    @Test
    void testCustomKeyHandling() {
        ConcurrentHashMapNullSafe<CustomKey, String> customMap = new ConcurrentHashMapNullSafe<>();
        CustomKey key1 = new CustomKey("alpha", 1);
        CustomKey key2 = new CustomKey("beta", 2);
        CustomKey key3 = new CustomKey("alpha", 1); // Same as key1

        customMap.put(key1, "First");
        customMap.put(key2, "Second");

        // Verify that key3, which is equal to key1, retrieves the same value
        assertEquals("First", customMap.get(key3));

        // Verify containsKey with key3
        assertTrue(customMap.containsKey(key3));

        // Remove using key3
        customMap.remove(key3);
        assertFalse(customMap.containsKey(key1));
        assertFalse(customMap.containsKey(key3));
        assertEquals(1, customMap.size());
    }
    
    @Test
    void testEqualsAndHashCode() {
        ConcurrentHashMapNullSafe<String, Integer> map1 = new ConcurrentHashMapNullSafe<>();
        ConcurrentHashMapNullSafe<String, Integer> map2 = new ConcurrentHashMapNullSafe<>();

        map1.put("one", 1);
        map1.put("two", 2);
        map1.put(null, 100);

        map2.put("one", 1);
        map2.put("two", 2);
        map2.put(null, 100);

        // Test equality
        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());

        // Modify map2 and test inequality
        map2.put("three", 3);
        assertNotEquals(map1, map2);
        assertNotEquals(map1.hashCode(), map2.hashCode());

        // Remove "three" and test equality again
        map2.remove("three");
        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());

        // Modify a value
        map2.put("one", 10);
        assertNotEquals(map1, map2);
    }

    @Test
    void testLargeDataSet() {
        int numEntries = 100_000;
        for (int i = 0; i < numEntries; i++) {
            String key = "key-" + i;
            Integer value = i;
            map.put(key, value);
        }

        assertEquals(numEntries, map.size());

        // Verify random entries
        assertEquals(500, map.get("key-500"));
        assertEquals(99999, map.get("key-99999"));
        assertNull(map.get("key-100000")); // Non-existent key
    }

    @Test
    void testClearViaKeySet() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        Set<String> keys = map.keySet();
        keys.clear();

        assertTrue(map.isEmpty());
    }

    @Test
    void testClearViaValues() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        Collection<Integer> values = map.values();
        values.clear();

        assertTrue(map.isEmpty());
    }

    @Test
    void testClearViaVEntries() {
        map.put("one", 1);
        map.put("two", 2);
        map.put(null, 100);

        Set<Map.Entry<String, Integer>> set = map.entrySet();
        set.clear();

        assertTrue(map.isEmpty());
    }

    /**
     * Tests for exception handling in ConcurrentHashMapNullSafe.
     */
    @Test
    void testNullRemappingFunctionInComputeIfAbsent() {
        ConcurrentHashMapNullSafe<String, Integer> map = new ConcurrentHashMapNullSafe<>();
        map.put("one", 1);

        // Attempt to pass a null remapping function
        assertThrows(NullPointerException.class, () -> {
            map.computeIfAbsent("two", null);
        });
    }

    @Test
    void testNullRemappingFunctionInCompute() {
        ConcurrentHashMapNullSafe<String, Integer> map = new ConcurrentHashMapNullSafe<>();
        map.put("one", 1);

        // Attempt to pass a null remapping function
        assertThrows(NullPointerException.class, () -> {
            map.compute("one", null);
        });
    }

    @Test
    void testNullRemappingFunctionInMerge() {
        ConcurrentHashMapNullSafe<String, Integer> map = new ConcurrentHashMapNullSafe<>();
        map.put("one", 1);

        // Attempt to pass a null remapping function
        assertThrows(NullPointerException.class, () -> {
            map.merge("one", 2, null);
        });
    }

    @Test
    void testGetOrDefault() {
        ConcurrentHashMapNullSafe<String, Integer> map = new ConcurrentHashMapNullSafe<>();
        map.put("one", 1);
        map.put(null, null);

        // Existing key with non-null value
        assertEquals(1, map.getOrDefault("one", 10));

        // Existing key with null value
        assertNull(map.getOrDefault(null, 100));

        // Non-existing key
        assertEquals(20, map.getOrDefault("two", 20));
    }
}
