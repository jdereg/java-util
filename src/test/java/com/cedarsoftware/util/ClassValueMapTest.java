package com.cedarsoftware.util;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
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
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/*
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
class ClassValueMapTest {

    private static final Logger LOG = Logger.getLogger(ClassValueMapTest.class.getName());
    @Test
    void testBasicMapOperations() {
        // Setup
        ClassValueMap<String> map = new ClassValueMap<>();

        // Test initial state
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // Test put and get
        assertNull(map.put(String.class, "StringValue"));
        assertEquals(1, map.size());
        assertEquals("StringValue", map.get(String.class));

        // Test containsKey
        assertTrue(map.containsKey(String.class));
        assertFalse(map.containsKey(Integer.class));

        // Test null key handling
        assertNull(map.put(null, "NullKeyValue"));
        assertEquals(2, map.size());
        assertEquals("NullKeyValue", map.get(null));
        assertTrue(map.containsKey(null));

        // Test remove
        assertEquals("StringValue", map.remove(String.class));
        assertEquals(1, map.size());
        assertFalse(map.containsKey(String.class));
        assertNull(map.get(String.class));

        // Test clear
        map.clear();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get(null));
    }

    @Test
    void testEntrySetAndKeySet() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "StringValue");
        map.put(Integer.class, "IntegerValue");
        map.put(Double.class, "DoubleValue");
        map.put(null, "NullKeyValue");

        // Test entrySet
        Set<Map.Entry<Class<?>, String>> entries = map.entrySet();
        assertEquals(4, entries.size());

        int count = 0;
        for (Map.Entry<Class<?>, String> entry : entries) {
            count++;
            if (entry.getKey() == null) {
                assertEquals("NullKeyValue", entry.getValue());
            } else if (entry.getKey() == String.class) {
                assertEquals("StringValue", entry.getValue());
            } else if (entry.getKey() == Integer.class) {
                assertEquals("IntegerValue", entry.getValue());
            } else if (entry.getKey() == Double.class) {
                assertEquals("DoubleValue", entry.getValue());
            } else {
                fail("Unexpected entry: " + entry);
            }
        }
        assertEquals(4, count);

        // Test keySet
        Set<Class<?>> keys = map.keySet();
        assertEquals(4, keys.size());
        assertTrue(keys.contains(null));
        assertTrue(keys.contains(String.class));
        assertTrue(keys.contains(Integer.class));
        assertTrue(keys.contains(Double.class));
    }

    @Test
    void testValues() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "StringValue");
        map.put(Integer.class, "IntegerValue");
        map.put(Double.class, "DoubleValue");
        map.put(null, "NullKeyValue");

        assertTrue(map.values().contains("StringValue"));
        assertTrue(map.values().contains("IntegerValue"));
        assertTrue(map.values().contains("DoubleValue"));
        assertTrue(map.values().contains("NullKeyValue"));
        assertEquals(4, map.values().size());
    }

    @Test
    void testConcurrentMapMethods() {
        ClassValueMap<String> map = new ClassValueMap<>();

        // Test putIfAbsent
        assertNull(map.putIfAbsent(String.class, "StringValue"));
        assertEquals("StringValue", map.putIfAbsent(String.class, "NewStringValue"));
        assertEquals("StringValue", map.get(String.class));

        assertNull(map.putIfAbsent(null, "NullKeyValue"));
        assertEquals("NullKeyValue", map.putIfAbsent(null, "NewNullKeyValue"));
        assertEquals("NullKeyValue", map.get(null));

        // Test replace
        assertNull(map.replace(Integer.class, "IntegerValue"));
        assertEquals("StringValue", map.replace(String.class, "ReplacedStringValue"));
        assertEquals("ReplacedStringValue", map.get(String.class));

        // Test replace with old value condition
        assertFalse(map.replace(String.class, "WrongValue", "NewValue"));
        assertEquals("ReplacedStringValue", map.get(String.class));
        assertTrue(map.replace(String.class, "ReplacedStringValue", "NewStringValue"));
        assertEquals("NewStringValue", map.get(String.class));

        // Test remove with value condition
        assertFalse(map.remove(String.class, "WrongValue"));
        assertEquals("NewStringValue", map.get(String.class));
        assertTrue(map.remove(String.class, "NewStringValue"));
        assertNull(map.get(String.class));
    }

    @Test
    void testUnmodifiableView() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "StringValue");
        map.put(Integer.class, "IntegerValue");
        map.put(null, "NullKeyValue");

        Map<Class<?>, String> unmodifiableMap = map.unmodifiableView();

        // Test that view reflects the original map
        assertEquals(3, unmodifiableMap.size());
        assertEquals("StringValue", unmodifiableMap.get(String.class));
        assertEquals("IntegerValue", unmodifiableMap.get(Integer.class));
        assertEquals("NullKeyValue", unmodifiableMap.get(null));

        // Test that changes to the original map are reflected in the view
        map.put(Double.class, "DoubleValue");
        assertEquals(4, unmodifiableMap.size());
        assertEquals("DoubleValue", unmodifiableMap.get(Double.class));

        // Test that the view is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.put(Boolean.class, "BooleanValue"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.remove(String.class));
        assertThrows(UnsupportedOperationException.class, unmodifiableMap::clear);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.putAll(new HashMap<>()));
    }

    @Test
    @EnabledIfSystemProperty(named = "performRelease", matches = "true")
    void testConcurrentAccess() throws InterruptedException {
        final int THREAD_COUNT = 10;
        final int CLASS_COUNT = 100;
        final long TEST_DURATION_MS = 5000;

        // Create a map
        final ClassValueMap<String> map = new ClassValueMap<>();
        final Class<?>[] testClasses = new Class<?>[CLASS_COUNT];

        // Create test classes array and prefill map
        for (int i = 0; i < CLASS_COUNT; i++) {
            testClasses[i] = getClassForIndex(i);
            map.put(testClasses[i], "Value-" + i);
        }
        map.put(null, "NullKeyValue");

        // Tracking metrics
        final AtomicInteger readCount = new AtomicInteger(0);
        final AtomicInteger writeCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);
        final AtomicBoolean running = new AtomicBoolean(true);
        final CountDownLatch startLatch = new CountDownLatch(1);

        // Create and start threads
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadNum = t;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    Random random = new Random();

                    while (running.get()) {
                        try {
                            // Pick a random class or null
                            int index = random.nextInt(CLASS_COUNT + 1); // +1 for null
                            Class<?> key = (index < CLASS_COUNT) ? testClasses[index] : null;

                            if (random.nextDouble() < 0.8) {
                                // READ operation (80%)
                                map.get(key);
                                readCount.incrementAndGet();
                            } else {
                                // WRITE operation (20%)
                                String newValue = "Thread-" + threadNum + "-" + System.nanoTime();

                                if (random.nextBoolean()) {
                                    // Use put
                                    map.put(key, newValue);
                                } else {
                                    // Use putIfAbsent
                                    map.putIfAbsent(key, newValue);
                                }
                                writeCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            LOG.warning("Error in thread " + Thread.currentThread().getName() + ": " + e.getMessage());
                            e.printStackTrace();
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
        LOG.info("Concurrent ClassValueMap Test Results:");
        LOG.info("Read operations: " + readCount.get());
        LOG.info("Write operations: " + writeCount.get());
        LOG.info("Total operations: " + (readCount.get() + writeCount.get()));
        LOG.info("Errors: " + errorCount.get());

        // Verify no errors occurred
        assertEquals(0, errorCount.get(), "Errors occurred during concurrent access");

        // Test the map still works after stress testing
        ClassValueMap<String> freshMap = new ClassValueMap<>();
        freshMap.put(String.class, "test");
        assertEquals("test", freshMap.get(String.class));
        freshMap.put(String.class, "updated");
        assertEquals("updated", freshMap.get(String.class));
        freshMap.remove(String.class);
        assertNull(freshMap.get(String.class));
    }
    
    @Test
    void testConcurrentModificationExceptionInEntrySet() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "StringValue");
        map.put(Integer.class, "IntegerValue");

        Iterator<Map.Entry<Class<?>, String>> iterator = map.entrySet().iterator();

        // This should throw ConcurrentModificationException
        assertThrows(UnsupportedOperationException.class, () -> {
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        });
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
    void testGetWithNonClassKey() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "StringValue");

        // Test get with a non-Class key
        assertNull(map.get("not a class"));
        assertNull(map.get(123));
        assertNull(map.get(new Object()));
    }

    @Test
    void testRemoveNullAndNonClassKey() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "StringValue");
        map.put(null, "NullKeyValue");

        // Test remove with null key
        assertEquals("NullKeyValue", map.remove(null));
        assertFalse(map.containsKey(null));
        assertNull(map.get(null));

        // Test remove with non-Class key
        assertNull(map.remove("not a class"));
        assertNull(map.remove(123));
        assertNull(map.remove(new Object()));

        // Verify the rest of the map is intact
        assertEquals(1, map.size());
        assertEquals("StringValue", map.get(String.class));
    }

    @Test
    void testClearWithItems() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "StringValue");
        map.put(Integer.class, "IntegerValue");
        map.put(Double.class, "DoubleValue");
        map.put(null, "NullKeyValue");

        assertEquals(4, map.size());
        assertFalse(map.isEmpty());

        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get(String.class));
        assertNull(map.get(Integer.class));
        assertNull(map.get(Double.class));
        assertNull(map.get(null));
    }

    @Test
    void testRemoveWithKeyAndValue() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "StringValue");
        map.put(Integer.class, "IntegerValue");
        map.put(null, "NullKeyValue");

        // Test with null key
        assertTrue(map.remove(null, "NullKeyValue"));
        assertFalse(map.containsKey(null));

        // Test with wrong value
        assertFalse(map.remove(String.class, "WrongValue"));
        assertEquals("StringValue", map.get(String.class));

        // Test with correct value
        assertTrue(map.remove(String.class, "StringValue"));
        assertNull(map.get(String.class));

        // Test with non-Class key
        assertFalse(map.remove("not a class", "any value"));
        assertFalse(map.remove(123, "any value"));
        assertFalse(map.remove(new Object(), "any value"));

        // Verify the rest of the map is intact
        assertEquals(1, map.size());
        assertEquals("IntegerValue", map.get(Integer.class));
    }

    @Test
    void testReplaceWithNullKey() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(null, "NullKeyValue");

        // Test replace(null, newValue)
        assertEquals("NullKeyValue", map.replace(null, "NewNullKeyValue"));
        assertEquals("NewNullKeyValue", map.get(null));

        // Test replace(null, oldValue, newValue) with wrong oldValue
        assertFalse(map.replace(null, "WrongValue", "AnotherValue"));
        assertEquals("NewNullKeyValue", map.get(null));

        // Test replace(null, oldValue, newValue) with correct oldValue
        assertTrue(map.replace(null, "NewNullKeyValue", "UpdatedNullKeyValue"));
        assertEquals("UpdatedNullKeyValue", map.get(null));
    }

    @Test
    void testUnmodifiableViewMethods() {
        ClassValueMap<String> map = new ClassValueMap<>();
        map.put(String.class, "StringValue");
        map.put(Integer.class, "IntegerValue");
        map.put(null, "NullKeyValue");

        Map<Class<?>, String> unmodifiableMap = map.unmodifiableView();

        // Test entrySet
        Set<Map.Entry<Class<?>, String>> entries = unmodifiableMap.entrySet();
        assertEquals(3, entries.size());

        // Verify entries are unmodifiable
        Iterator<Map.Entry<Class<?>, String>> iterator = entries.iterator();
        Map.Entry<Class<?>, String> firstEntry = iterator.next();
        assertThrows(UnsupportedOperationException.class, () -> firstEntry.setValue("NewValue"));

        // Test containsKey
        assertTrue(unmodifiableMap.containsKey(String.class));
        assertTrue(unmodifiableMap.containsKey(Integer.class));
        assertTrue(unmodifiableMap.containsKey(null));
        assertFalse(unmodifiableMap.containsKey(Double.class));
        assertFalse(unmodifiableMap.containsKey("not a class"));

        // Test keySet
        Set<Class<?>> keys = unmodifiableMap.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.contains(String.class));
        assertTrue(keys.contains(Integer.class));
        assertTrue(keys.contains(null));

        // Verify keySet is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> keys.remove(String.class));

        // Test values
        Collection<String> values = unmodifiableMap.values();
        assertEquals(3, values.size());
        assertTrue(values.contains("StringValue"));
        assertTrue(values.contains("IntegerValue"));
        assertTrue(values.contains("NullKeyValue"));

        // Verify values is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> values.remove("StringValue"));

        // Verify original map changes are reflected in view
        map.put(Double.class, "DoubleValue");
        assertEquals(4, unmodifiableMap.size());
        assertEquals("DoubleValue", unmodifiableMap.get(Double.class));
        assertTrue(unmodifiableMap.containsKey(Double.class));
    }

    @Test
    void testConstructorWithMap() {
        // Create a source map with various Class keys and values
        Map<Class<?>, String> sourceMap = new HashMap<>();
        sourceMap.put(String.class, "StringValue");
        sourceMap.put(Integer.class, "IntegerValue");
        sourceMap.put(Double.class, "DoubleValue");
        sourceMap.put(null, "NullKeyValue");

        // Create a ClassValueMap using the constructor
        ClassValueMap<String> classValueMap = new ClassValueMap<>(sourceMap);

        // Verify all mappings were copied correctly
        assertEquals(4, classValueMap.size());
        assertEquals("StringValue", classValueMap.get(String.class));
        assertEquals("IntegerValue", classValueMap.get(Integer.class));
        assertEquals("DoubleValue", classValueMap.get(Double.class));
        assertEquals("NullKeyValue", classValueMap.get(null));

        // Verify the map is independent (modifications to original don't affect the new map)
        sourceMap.put(Boolean.class, "BooleanValue");
        sourceMap.remove(String.class);

        assertEquals(4, classValueMap.size());
        assertTrue(classValueMap.containsKey(String.class));
        assertEquals("StringValue", classValueMap.get(String.class));
        assertFalse(classValueMap.containsKey(Boolean.class));

        // Test that null map throws NullPointerException
        assertThrows(NullPointerException.class, () -> new ClassValueMap<String>(null));
    }
}