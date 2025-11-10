package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the fix for [thread-3] Unsafe visited set publication.
 *
 * The issue: HashSet copy constructor `new HashSet<>(visited)` is not atomic and could throw
 * ConcurrentModificationException if the source set was being modified concurrently.
 *
 * The fix: Use ConcurrentSet instead of HashSet for the visited set. ConcurrentSet is backed
 * by ConcurrentHashMap, which provides weakly consistent iterators that never throw
 * ConcurrentModificationException.
 */
public class TestVisitedSetConcurrency {

    /**
     * Test that basic comparison still works with ConcurrentSet.
     */
    @Test
    public void testBasicComparison() {
        Set<String> set1 = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> set2 = new HashSet<>(Arrays.asList("a", "b", "c"));

        assertTrue(DeepEquals.deepEquals(set1, set2), "Equal sets should be equal");

        set2.add("d");
        assertFalse(DeepEquals.deepEquals(set1, set2), "Different sets should not be equal");
    }

    /**
     * Test comparison of unordered collections (which uses visitedCopy).
     */
    @Test
    public void testUnorderedCollectionComparison() {
        // Create sets with custom objects
        Set<Person> set1 = new HashSet<>();
        set1.add(new Person("Alice", 30));
        set1.add(new Person("Bob", 25));
        set1.add(new Person("Charlie", 35));

        Set<Person> set2 = new HashSet<>();
        set2.add(new Person("Alice", 30));
        set2.add(new Person("Bob", 25));
        set2.add(new Person("Charlie", 35));

        assertTrue(DeepEquals.deepEquals(set1, set2), "Sets with same people should be equal");
    }

    /**
     * Test that circular references still work correctly.
     */
    @Test
    public void testCircularReferencesWithConcurrentSet() {
        Set<Object> set1 = new HashSet<>();
        set1.add("a");
        set1.add(set1);  // Circular!

        Set<Object> set2 = new HashSet<>();
        set2.add("a");
        set2.add(set2);  // Circular!

        assertTrue(DeepEquals.deepEquals(set1, set2), "Circular sets should be equal");
    }

    /**
     * Test multi-threaded comparison without concurrent modification.
     * This verifies that ConcurrentSet doesn't break normal multi-threaded usage.
     */
    @Test
    public void testMultiThreadedComparisonsNoConcurrentMod() throws Exception {
        final int THREAD_COUNT = 10;
        final int ITERATIONS = 100;
        final AtomicInteger successCount = new AtomicInteger(0);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < ITERATIONS; j++) {
                        Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));
                        Set<Integer> set2 = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5));

                        if (DeepEquals.deepEquals(set1, set2)) {
                            successCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();

        assertEquals(THREAD_COUNT * ITERATIONS, successCount.get(),
            "All comparisons should succeed");
    }

    /**
     * Test that deeply nested structures work correctly.
     */
    @Test
    public void testDeeplyNestedStructures() {
        Map<String, Object> deep1 = new HashMap<>();
        deep1.put("level", "deep");
        deep1.put("data", new HashSet<>(Arrays.asList(1, 2, 3)));

        Map<String, Object> mid1 = new HashMap<>();
        mid1.put("level", "mid");
        mid1.put("child", deep1);

        Map<String, Object> top1 = new HashMap<>();
        top1.put("level", "top");
        top1.put("child", mid1);

        // Create identical structure
        Map<String, Object> deep2 = new HashMap<>();
        deep2.put("level", "deep");
        deep2.put("data", new HashSet<>(Arrays.asList(1, 2, 3)));

        Map<String, Object> mid2 = new HashMap<>();
        mid2.put("level", "mid");
        mid2.put("child", deep2);

        Map<String, Object> top2 = new HashMap<>();
        top2.put("level", "top");
        top2.put("child", mid2);

        assertTrue(DeepEquals.deepEquals(top1, top2),
            "Deeply nested structures should be equal");
    }

    /**
     * Test sequential comparisons to ensure no state leakage.
     */
    @Test
    public void testSequentialComparisons() {
        for (int i = 0; i < 1000; i++) {
            Set<Integer> set1 = new HashSet<>(Arrays.asList(1, 2, 3));
            Set<Integer> set2 = new HashSet<>(Arrays.asList(1, 2, i % 10 == 0 ? 3 : 4));

            boolean result = DeepEquals.deepEquals(set1, set2);

            if (i % 10 == 0) {
                assertTrue(result, "Sets should be equal when i is divisible by 10");
            } else {
                assertFalse(result, "Sets should not be equal otherwise");
            }
        }
    }

    /**
     * Test with concurrent collections as inputs.
     * Even though inputs are thread-safe, the comparison should still work.
     */
    @Test
    public void testConcurrentCollectionsAsInputs() {
        Set<String> set1 = ConcurrentHashMap.newKeySet();
        set1.addAll(Arrays.asList("a", "b", "c"));

        Set<String> set2 = ConcurrentHashMap.newKeySet();
        set2.addAll(Arrays.asList("a", "b", "c"));

        assertTrue(DeepEquals.deepEquals(set1, set2),
            "ConcurrentHashMap key sets should be equal");
    }

    /**
     * Test with large unordered collections.
     * This exercises the hash-based matching logic extensively.
     */
    @Test
    public void testLargeUnorderedCollections() {
        Set<Integer> set1 = new HashSet<>();
        Set<Integer> set2 = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            set1.add(i);
            set2.add(i);
        }

        assertTrue(DeepEquals.deepEquals(set1, set2),
            "Large sets with same elements should be equal");

        set2.remove(500);
        assertFalse(DeepEquals.deepEquals(set1, set2),
            "Sets should not be equal after removing element");
    }

    /**
     * Test mixed collection types.
     */
    @Test
    public void testMixedCollectionTypes() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("set", new HashSet<>(Arrays.asList(1, 2, 3)));
        map1.put("list", Arrays.asList("a", "b", "c"));

        Map<String, Object> map2 = new HashMap<>();
        map2.put("set", new HashSet<>(Arrays.asList(3, 2, 1)));  // Different order
        map2.put("list", Arrays.asList("a", "b", "c"));

        assertTrue(DeepEquals.deepEquals(map1, map2),
            "Maps with unordered sets should be equal regardless of set element order");
    }

    /**
     * Simple Person class for testing.
     */
    static class Person {
        String name;
        int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }
}
