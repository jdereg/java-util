package com.cedarsoftware.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassValueSetTest {

    @Test
    void testBasicSetOperations() {
        // Setup
        ClassValueSet set = new ClassValueSet();

        // Test initial state
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // Test add and contains
        assertTrue(set.add(String.class));
        assertEquals(1, set.size());
        assertTrue(set.contains(String.class));

        // Test contains
        assertTrue(set.contains(String.class));
        assertFalse(set.contains(Integer.class));

        // Test null key handling
        assertTrue(set.add(null));
        assertEquals(2, set.size());
        assertTrue(set.contains(null));

        // Test add duplicate
        assertFalse(set.add(String.class));
        assertEquals(2, set.size());

        // Test remove
        assertTrue(set.remove(String.class));
        assertEquals(1, set.size());
        assertFalse(set.contains(String.class));

        // Test clear
        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
        assertFalse(set.contains(null));
    }

    @Test
    void testIterator() {
        ClassValueSet set = new ClassValueSet();
        set.add(String.class);
        set.add(Integer.class);
        set.add(Double.class);
        set.add(null);

        // Count elements via iterator
        int count = 0;
        Set<Class<?>> encountered = new HashSet<>();
        boolean foundNull = false;

        for (Iterator<Class<?>> it = set.iterator(); it.hasNext(); ) {
            Class<?> value = it.next();
            count++;

            if (value == null) {
                foundNull = true;
            } else {
                encountered.add(value);
            }
        }

        assertEquals(4, count);
        assertEquals(3, encountered.size());
        assertTrue(encountered.contains(String.class));
        assertTrue(encountered.contains(Integer.class));
        assertTrue(encountered.contains(Double.class));
        assertTrue(foundNull);
    }

    @Test
    void testConstructorWithCollection() {
        // Create a source collection with various Class elements
        Collection<Class<?>> sourceCollection = new ArrayList<>();
        sourceCollection.add(String.class);
        sourceCollection.add(Integer.class);
        sourceCollection.add(Double.class);
        sourceCollection.add(null);

        // Create a ClassValueSet using the constructor
        ClassValueSet classValueSet = new ClassValueSet(sourceCollection);

        // Verify all elements were copied correctly
        assertEquals(4, classValueSet.size());
        assertTrue(classValueSet.contains(String.class));
        assertTrue(classValueSet.contains(Integer.class));
        assertTrue(classValueSet.contains(Double.class));
        assertTrue(classValueSet.contains(null));

        // Verify the set is independent (modifications to original don't affect the new set)
        sourceCollection.add(Boolean.class);
        sourceCollection.remove(String.class);

        assertEquals(4, classValueSet.size());
        assertTrue(classValueSet.contains(String.class));
        assertFalse(classValueSet.contains(Boolean.class));

        // Test that null collection throws NullPointerException
        assertThrows(NullPointerException.class, () -> new ClassValueSet(null));
    }

    @Test
    void testCollectionOperations() {
        ClassValueSet set = new ClassValueSet();

        // Test addAll
        List<Class<?>> toAdd = Arrays.asList(String.class, Integer.class, Double.class);
        assertTrue(set.addAll(toAdd));
        assertEquals(3, set.size());

        // Test containsAll
        assertTrue(set.containsAll(Arrays.asList(String.class, Integer.class)));
        assertFalse(set.containsAll(Arrays.asList(String.class, Boolean.class)));

        // Test removeAll
        assertTrue(set.removeAll(Arrays.asList(String.class, Boolean.class)));
        assertEquals(2, set.size());
        assertFalse(set.contains(String.class));
        assertTrue(set.contains(Integer.class));
        assertTrue(set.contains(Double.class));

        // Test retainAll - now supports this operation
        assertTrue(set.retainAll(Arrays.asList(Integer.class, Boolean.class)));
        assertEquals(1, set.size());
        assertTrue(set.contains(Integer.class));
        assertFalse(set.contains(Double.class));

        // Test retainAll with no changes
        assertFalse(set.retainAll(Arrays.asList(Integer.class, Boolean.class)));
        assertEquals(1, set.size());
        assertTrue(set.contains(Integer.class));

        // Test toArray()
        Object[] array = set.toArray();
        assertEquals(1, array.length);
        assertEquals(Integer.class, array[0]);

        // Test toArray(T[])
        Class<?>[] typedArray = new Class<?>[1];
        Class<?>[] resultArray = set.toArray(typedArray);
        assertSame(typedArray, resultArray);
        assertEquals(Integer.class, resultArray[0]);

        // Test toArray(T[]) with larger array
        typedArray = new Class<?>[2];
        resultArray = set.toArray(typedArray);
        assertSame(typedArray, resultArray);
        assertEquals(2, resultArray.length);
        assertEquals(Integer.class, resultArray[0]);
        assertNull(resultArray[1]); // Second element should be null

        // Test adding null
        assertTrue(set.add(null));
        assertEquals(2, set.size());
        assertTrue(set.contains(null));

        // Test toArray() with null
        array = set.toArray();
        assertEquals(2, array.length);
        Set<Object> arrayElements = new HashSet<>(Arrays.asList(array));
        assertTrue(arrayElements.contains(Integer.class));
        assertTrue(arrayElements.contains(null));

        // Test retainAll with null
        assertTrue(set.retainAll(Collections.singleton(null)));
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
        assertFalse(set.contains(Integer.class));

        // Test toArray(T[]) with smaller array after retaining null
        typedArray = new Class<?>[0];
        resultArray = set.toArray(typedArray);
        assertNotSame(typedArray, resultArray);
        assertEquals(1, resultArray.length);
        assertNull(resultArray[0]);
    }
    
    @Test
    void testWithNonClassElements() {
        ClassValueSet set = new ClassValueSet();
        set.add(String.class);

        // Test contains with non-Class elements
        assertFalse(set.contains("not a class"));
        assertFalse(set.contains(123));
        assertFalse(set.contains(new Object()));

        // Test remove with non-Class elements
        assertFalse(set.remove("not a class"));
        assertFalse(set.remove(123));
        assertFalse(set.remove(new Object()));

        // Verify the set is intact
        assertEquals(1, set.size());
        assertTrue(set.contains(String.class));
    }

    @Test
    void testClearWithElements() {
        ClassValueSet set = new ClassValueSet();
        set.add(String.class);
        set.add(Integer.class);
        set.add(Double.class);
        set.add(null);

        assertEquals(4, set.size());
        assertFalse(set.isEmpty());

        set.clear();

        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
        assertFalse(set.contains(String.class));
        assertFalse(set.contains(Integer.class));
        assertFalse(set.contains(Double.class));
        assertFalse(set.contains(null));
    }

    @Test
    void testEqualsAndHashCode() {
        ClassValueSet set1 = new ClassValueSet();
        set1.add(String.class);
        set1.add(Integer.class);
        set1.add(null);

        ClassValueSet set2 = new ClassValueSet();
        set2.add(String.class);
        set2.add(Integer.class);
        set2.add(null);

        ClassValueSet set3 = new ClassValueSet();
        set3.add(String.class);
        set3.add(Double.class);
        set3.add(null);

        // Test equals
        assertEquals(set1, set2);
        assertNotEquals(set1, set3);

        // Test hashCode
        assertEquals(set1.hashCode(), set2.hashCode());

        // Test with regular HashSet
        Set<Class<?>> regularSet = new HashSet<>();
        regularSet.add(String.class);
        regularSet.add(Integer.class);
        regularSet.add(null);

        assertEquals(set1, regularSet);
        assertEquals(regularSet, set1);
        assertEquals(set1.hashCode(), regularSet.hashCode());
    }

    @Test
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    void testConcurrentAccess() throws InterruptedException {
        final int THREAD_COUNT = 20;
        final int CLASS_COUNT = 100;
        final long TEST_DURATION_MS = 5000;

        // Create a set
        final ClassValueSet set = new ClassValueSet();
        final Class<?>[] testClasses = new Class<?>[CLASS_COUNT];

        // Create test classes array
        for (int i = 0; i < CLASS_COUNT; i++) {
            testClasses[i] = getClassForIndex(i);
            set.add(testClasses[i]);
        }

        // Add null element too
        set.add(null);

        // Tracking metrics
        final AtomicInteger readCount = new AtomicInteger(0);
        final AtomicInteger writeCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        final AtomicBoolean running = new AtomicBoolean(true);
        final CountDownLatch startLatch = new CountDownLatch(1);

        // Create and start threads
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int t = 0; t < THREAD_COUNT; t++) {
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    Random random = new Random();

                    while (running.get()) {
                        // Pick a random class or null
                        int index = random.nextInt(CLASS_COUNT + 1); // +1 for null
                        Class<?> value = (index < CLASS_COUNT) ? testClasses[index] : null;

                        // Determine operation (80% reads, 20% writes)
                        boolean isRead = random.nextDouble() < 0.8;

                        if (isRead) {
                            // Read operation
                            set.contains(value);
                            readCount.incrementAndGet();
                        } else {
                            // Write operation
                            if (random.nextBoolean()) {
                                // Add operation
                                set.add(value);
                            } else {
                                // Remove operation
                                set.remove(value);
                            }
                            writeCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    e.printStackTrace();
                }
            });
        }

        // Start the test
        startLatch.countDown();

        // Let the test run for the specified duration
        Thread.sleep(TEST_DURATION_MS);
        running.set(false);

        // Shutdown the executor and wait for all tasks to complete
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.SECONDS);

        // Log results
        System.out.println("=== Concurrent FastClassSet Test Results ===");
        System.out.println("Read operations: " + readCount.get());
        System.out.println("Write operations: " + writeCount.get());
        System.out.println("Total operations: " + (readCount.get() + writeCount.get()));
        System.out.println("Errors: " + errorCount.get());

        // Verify no errors occurred
        assertEquals(0, errorCount.get(), "Errors occurred during concurrent access");

        // Create a brand new set for verification to avoid state corruption
        System.out.println("\nVerifying set operations with clean state...");
        ClassValueSet freshSet = new ClassValueSet();

        // Test basic operations with diagnostics
        for (int i = 0; i < 10; i++) {
            Class<?> cls = testClasses[i];
            System.out.println("Testing with class: " + cls);

            // Test add
            boolean addResult = freshSet.add(cls);
            System.out.println("  add result: " + addResult);
            assertTrue(addResult, "Add should return true for class " + cls);

            // Test contains
            boolean containsResult = freshSet.contains(cls);
            System.out.println("  contains result: " + containsResult);
            assertTrue(containsResult, "Contains should return true for class " + cls + " after adding");

            // Test remove
            boolean removeResult = freshSet.remove(cls);
            System.out.println("  remove result: " + removeResult);
            assertTrue(removeResult, "Remove should return true for class " + cls);

            // Test contains after remove
            boolean containsAfterRemove = freshSet.contains(cls);
            System.out.println("  contains after remove: " + containsAfterRemove);
            assertFalse(containsAfterRemove, "Contains should return false for class " + cls + " after removing");

            // Test add again
            boolean addAgainResult = freshSet.add(cls);
            System.out.println("  add again result: " + addAgainResult);
            assertTrue(addAgainResult, "Add should return true for class " + cls + " after removing");

            // Test contains again
            boolean containsAgain = freshSet.contains(cls);
            System.out.println("  contains again result: " + containsAgain);
            assertTrue(containsAgain, "Contains should return true for class " + cls + " after adding again");
        }

        // Test with null
        System.out.println("Testing with null:");

        // Test add null
        boolean addNullResult = freshSet.add(null);
        System.out.println("  add null result: " + addNullResult);
        assertTrue(addNullResult, "Add should return true for null");

        // Test contains null
        boolean containsNullResult = freshSet.contains(null);
        System.out.println("  contains null result: " + containsNullResult);
        assertTrue(containsNullResult, "Contains should return true for null after adding");

        // Test remove null
        boolean removeNullResult = freshSet.remove(null);
        System.out.println("  remove null result: " + removeNullResult);
        assertTrue(removeNullResult, "Remove should return true for null");

        // Test contains null after remove
        boolean containsNullAfterRemove = freshSet.contains(null);
        System.out.println("  contains null after remove: " + containsNullAfterRemove);
        assertFalse(containsNullAfterRemove, "Contains should return false for null after removing");
    }
    
    @Test
    void testUnmodifiableView() {
        ClassValueSet set = new ClassValueSet();
        set.add(String.class);
        set.add(Integer.class);
        set.add(null);

        Set<Class<?>> unmodifiableSet = Collections.unmodifiableSet(set);

        // Test that view reflects the original set
        assertEquals(3, unmodifiableSet.size());
        assertTrue(unmodifiableSet.contains(String.class));
        assertTrue(unmodifiableSet.contains(Integer.class));
        assertTrue(unmodifiableSet.contains(null));

        // Test that changes to the original set are reflected in the view
        set.add(Double.class);
        assertEquals(4, unmodifiableSet.size());
        assertTrue(unmodifiableSet.contains(Double.class));

        // Test that the view is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSet.add(Boolean.class));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSet.remove(String.class));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSet.clear());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSet.addAll(Arrays.asList(Boolean.class)));
    }

    // Helper method to get a Class object for an index
    private Class<?> getClassForIndex(int index) {
        // A selection of common classes for testing
        Class<?>[] commonClasses = {
                String.class, Integer.class, Double.class, Boolean.class,
                Long.class, Float.class, Character.class, Byte.class,
                Short.class, Void.class, Object.class, Class.class,
                Enum.class, Number.class, Math.class, System.class,
                Runtime.class, Thread.class, Exception.class, Error.class,
                Throwable.class, IOException.class, RuntimeException.class,
                StringBuilder.class, StringBuffer.class, Iterable.class,
                Collection.class, List.class, Set.class, Map.class
        };

        if (index < commonClasses.length) {
            return commonClasses[index];
        }

        // For indices beyond the common classes length, use array classes
        // of varying dimensions to get more unique Class objects
        int dimensions = (index - commonClasses.length) / 4 + 1;
        int baseTypeIndex = (index - commonClasses.length) % 4;

        switch (baseTypeIndex) {
            case 0: return getArrayClass(int.class, dimensions);
            case 1: return getArrayClass(String.class, dimensions);
            case 2: return getArrayClass(Double.class, dimensions);
            case 3: return getArrayClass(Boolean.class, dimensions);
            default: return Object.class;
        }
    }

    // Helper to create array classes of specified dimensions
    private Class<?> getArrayClass(Class<?> componentType, int dimensions) {
        Class<?> arrayClass = componentType;
        for (int i = 0; i < dimensions; i++) {
            arrayClass = java.lang.reflect.Array.newInstance(arrayClass, 0).getClass();
        }
        return arrayClass;
    }

    @Test
    public void testRemoveNull() {
        ClassValueSet set = new ClassValueSet();
        set.add(String.class);
        set.add(null);

        // Test removing null
        assertTrue(set.remove(null));
        assertEquals(1, set.size());
        assertFalse(set.contains(null));

        // Test removing null when not present
        assertFalse(set.remove(null));
        assertEquals(1, set.size());

        // Verify other elements remain
        assertTrue(set.contains(String.class));
    }

    @Test
    public void testToSet() {
        // Create a ClassValueSet
        ClassValueSet original = new ClassValueSet();
        original.add(String.class);
        original.add(Integer.class);
        original.add(null);

        // Convert to standard Set
        Set<Class<?>> standardSet = original.toSet();

        // Verify contents
        assertEquals(3, standardSet.size());
        assertTrue(standardSet.contains(String.class));
        assertTrue(standardSet.contains(Integer.class));
        assertTrue(standardSet.contains(null));

        // Verify it's a new independent copy
        original.add(Double.class);
        assertEquals(3, standardSet.size());
        assertFalse(standardSet.contains(Double.class));

        // Verify modifying the returned set doesn't affect original
        standardSet.add(Boolean.class);
        assertEquals(4, original.size());
        assertFalse(original.contains(Boolean.class));
    }

    @Test
    public void testFrom() {
        // Create a source set
        Set<Class<?>> source = new HashSet<>();
        source.add(String.class);
        source.add(Integer.class);
        source.add(null);

        // Create ClassValueSet using from()
        ClassValueSet set = ClassValueSet.from(source);

        // Verify contents
        assertEquals(3, set.size());
        assertTrue(set.contains(String.class));
        assertTrue(set.contains(Integer.class));
        assertTrue(set.contains(null));

        // Verify it's independent of source
        source.add(Double.class);
        assertEquals(3, set.size());
        assertFalse(set.contains(Double.class));

        // Test with null source
        assertThrows(NullPointerException.class, () -> ClassValueSet.from(null));

        // Test with empty source
        ClassValueSet emptySet = ClassValueSet.from(Collections.emptySet());
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testOf() {
        // Test with no arguments
        ClassValueSet emptySet = ClassValueSet.of();
        assertTrue(emptySet.isEmpty());

        // Test with single argument
        ClassValueSet singleSet = ClassValueSet.of(String.class);
        assertEquals(1, singleSet.size());
        assertTrue(singleSet.contains(String.class));

        // Test with multiple arguments
        ClassValueSet multiSet = ClassValueSet.of(String.class, Integer.class, null);
        assertEquals(3, multiSet.size());
        assertTrue(multiSet.contains(String.class));
        assertTrue(multiSet.contains(Integer.class));
        assertTrue(multiSet.contains(null));

        // Test with duplicate arguments
        ClassValueSet duplicateSet = ClassValueSet.of(String.class, String.class, Integer.class);
        assertEquals(2, duplicateSet.size());
        assertTrue(duplicateSet.contains(String.class));
        assertTrue(duplicateSet.contains(Integer.class));
    }

    @Test
    public void testUnmodifiableView2() {
        // Create original set
        ClassValueSet original = new ClassValueSet();
        original.add(String.class);
        original.add(Integer.class);
        original.add(null);

        // Get unmodifiable view
        Set<Class<?>> view = original.unmodifiableView();

        // Test size and contents
        assertEquals(3, view.size());
        assertTrue(view.contains(String.class));
        assertTrue(view.contains(Integer.class));
        assertTrue(view.contains(null));

        // Test modifications are rejected
        assertThrows(UnsupportedOperationException.class, () -> view.add(Double.class));
        assertThrows(UnsupportedOperationException.class, () -> view.remove(String.class));
        assertThrows(UnsupportedOperationException.class, () -> view.clear());
        assertThrows(UnsupportedOperationException.class, () -> view.addAll(Collections.singleton(Double.class)));
        assertThrows(UnsupportedOperationException.class, () -> view.removeAll(Collections.singleton(String.class)));
        assertThrows(UnsupportedOperationException.class, () -> view.retainAll(Collections.singleton(String.class)));

        // Test iterator remove is rejected
        Iterator<Class<?>> iterator = view.iterator();
        if (iterator.hasNext()) {
            iterator.next();
            assertThrows(UnsupportedOperationException.class, iterator::remove);
        }

        // Test that changes to original are reflected in view
        original.add(Double.class);
        assertEquals(4, view.size());
        assertTrue(view.contains(Double.class));

        original.remove(String.class);
        assertEquals(3, view.size());
        assertFalse(view.contains(String.class));

        // Test that view preserves ClassValue performance benefits
        ClassValueSet performanceTest = new ClassValueSet();
        performanceTest.add(String.class);
        Set<Class<?>> unmodifiable = performanceTest.unmodifiableView();

        // This would use the fast path in the original implementation
        assertTrue(unmodifiable.contains(String.class));

        // For comparison, standard unmodifiable view
        Set<Class<?>> standardUnmodifiable = Collections.unmodifiableSet(performanceTest);
        assertTrue(standardUnmodifiable.contains(String.class));
    }
}