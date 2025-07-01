package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for verifying CaseInsensitiveSet concurrent functionality and parity with CaseInsensitiveMap.
 * Tests concurrent bulk operations, iterator behavior, and backing map access.
 */
class CaseInsensitiveSetConcurrentTest {

    private CaseInsensitiveSet<String> concurrentSet;
    private CaseInsensitiveSet<String> regularSet;

    @BeforeEach
    void setUp() {
        // Set backed by ConcurrentHashMap via CaseInsensitiveMap
        concurrentSet = new CaseInsensitiveSet<>(new ConcurrentSet<>());
        concurrentSet.add("Alpha");
        concurrentSet.add("BETA");
        concurrentSet.add("gamma");
        concurrentSet.add("Delta");

        // Regular set for comparison
        regularSet = new CaseInsensitiveSet<>();
        regularSet.add("Alpha");
        regularSet.add("BETA");
        regularSet.add("gamma");
        regularSet.add("Delta");
    }

    @AfterEach
    void tearDown() {
        concurrentSet = null;
        regularSet = null;
    }

    @Test
    void testElementCount_ConcurrentBacking() {
        // Test elementCount with concurrent backing
        assertEquals(4L, concurrentSet.elementCount());
        
        // Add more elements
        for (int i = 0; i < 100; i++) {
            concurrentSet.add("element" + i);
        }
        
        assertEquals(104L, concurrentSet.elementCount());
    }

    @Test
    void testElementCount_RegularBacking() {
        // Test elementCount with regular backing (should delegate to size())
        assertEquals(4L, regularSet.elementCount());
        
        // Verify it delegates to size() for non-concurrent backing
        assertEquals((long) regularSet.size(), regularSet.elementCount());
    }

    @Test
    void testForEach_ParallelExecution() {
        // Test parallel forEach with concurrent backing
        AtomicInteger counter = new AtomicInteger(0);
        Set<String> processedElements = ConcurrentHashMap.newKeySet();
        
        concurrentSet.forEach(1L, element -> {
            counter.incrementAndGet();
            processedElements.add(element.toLowerCase());
        });
        
        assertEquals(4, counter.get());
        assertEquals(4, processedElements.size());
        assertTrue(processedElements.contains("alpha"));
        assertTrue(processedElements.contains("beta"));
        assertTrue(processedElements.contains("gamma"));
        assertTrue(processedElements.contains("delta"));
    }

    @Test
    void testForEach_SequentialExecution() {
        // Test sequential forEach
        AtomicInteger counter = new AtomicInteger(0);
        
        concurrentSet.forEach(Long.MAX_VALUE, element -> counter.incrementAndGet());
        
        assertEquals(4, counter.get());
    }

    @Test
    void testForEach_WithNullFunction() {
        // Test that null action throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            concurrentSet.forEach(1L, null);
        });
    }

    @Test
    void testSearchElements_FindElement() {
        // Test searching for an element that exists
        String result = concurrentSet.searchElements(1L, element -> {
            if (element.toLowerCase().equals("beta")) {
                return "Found: " + element;
            }
            return null;
        });
        
        assertEquals("Found: BETA", result);
    }

    @Test
    void testSearchElements_ElementNotFound() {
        // Test searching for an element that doesn't exist
        String result = concurrentSet.searchElements(1L, element -> {
            if (element.toLowerCase().equals("nonexistent")) {
                return "Found: " + element;
            }
            return null;
        });
        
        assertNull(result);
    }

    @Test
    void testSearchElements_FirstNonNullResult() {
        // Test that search returns first non-null result
        String result = concurrentSet.searchElements(1L, element -> {
            if (element.toLowerCase().contains("a")) {
                return "Contains 'a': " + element;
            }
            return null;
        });
        
        assertNotNull(result);
        assertTrue(result.startsWith("Contains 'a':"));
    }

    @Test
    void testSearchElements_WithNullFunction() {
        // Test that null search function throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            concurrentSet.searchElements(1L, null);
        });
    }

    @Test
    void testReduceElements_StringConcatenation() {
        // Test reducing elements by concatenating lengths
        Integer totalLength = concurrentSet.reduceElements(1L,
            element -> element.length(),
            Integer::sum
        );
        
        // Alpha(5) + BETA(4) + gamma(5) + Delta(5) = 19
        assertEquals(19, totalLength.intValue());
    }

    @Test
    void testReduceElements_EmptySet() {
        // Test reduce on empty set
        CaseInsensitiveSet<String> emptySet = new CaseInsensitiveSet<>();
        
        String result = emptySet.reduceElements(1L,
            element -> element.toUpperCase(),
            (a, b) -> a + "," + b
        );
        
        assertNull(result);
    }

    @Test
    void testReduceElements_SingleElement() {
        // Test reduce with single element
        CaseInsensitiveSet<String> singleSet = new CaseInsensitiveSet<>();
        singleSet.add("Only");
        
        String result = singleSet.reduceElements(1L,
            element -> element.toUpperCase(),
            (a, b) -> a + "," + b
        );
        
        assertEquals("ONLY", result);
    }

    @Test
    void testReduceElements_WithNullTransformer() {
        // Test that null transformer throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            concurrentSet.reduceElements(1L, null, (String a, String b) -> a + b);
        });
    }

    @Test
    void testReduceElements_WithNullReducer() {
        // Test that null reducer throws NullPointerException
        assertThrows(NullPointerException.class, () -> {
            concurrentSet.reduceElements(1L, element -> element, null);
        });
    }

    @Test
    void testGetBackingMap_Access() {
        // Test that we can access the backing map
        Map<String, Object> backingMap = concurrentSet.getBackingMap();
        
        assertNotNull(backingMap);
        assertEquals(4, backingMap.size());
        assertTrue(backingMap.containsKey("Alpha"));
        assertTrue(backingMap.containsKey("beta")); // Case-insensitive
        assertTrue(backingMap.containsKey("GAMMA")); // Case-insensitive
    }

    @Test
    void testGetBackingMap_CaseInsensitiveMap() {
        // Test that backing map is indeed a CaseInsensitiveMap
        Map<String, Object> backingMap = concurrentSet.getBackingMap();
        
        assertTrue(backingMap instanceof CaseInsensitiveMap);
    }

    @Test
    void testIterator_ConcurrentModificationTolerance() throws InterruptedException {
        // Test that iterator tolerates concurrent modifications when using concurrent backing
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger exceptions = new AtomicInteger(0);
        AtomicInteger elementsIterated = new AtomicInteger(0);
        
        // Thread 1: Iterate through set
        Thread iterator = new Thread(() -> {
            try {
                startLatch.await();
                Iterator<String> iter = concurrentSet.iterator();
                while (iter.hasNext()) {
                    iter.next();
                    elementsIterated.incrementAndGet();
                    Thread.sleep(10); // Slow down iteration
                }
            } catch (ConcurrentModificationException e) {
                exceptions.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
        
        // Thread 2: Modify set during iteration
        Thread modifier = new Thread(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < 5; i++) {
                    concurrentSet.add("concurrent" + i);
                    Thread.sleep(5);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });
        
        iterator.start();
        modifier.start();
        startLatch.countDown();
        
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        
        // With concurrent backing, we should not get ConcurrentModificationException
        assertEquals(0, exceptions.get());
        // Should have iterated over at least the original elements
        assertTrue(elementsIterated.get() >= 4);
    }

    @Test
    void testConcurrentHighLoad() throws InterruptedException {
        // Test high-concurrency operations
        int numThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        // Each thread adds elements, searches, and performs reductions
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // Add elements
                    for (int i = 0; i < operationsPerThread; i++) {
                        concurrentSet.add("thread" + threadId + "_element" + i);
                    }
                    
                    // Perform bulk operations
                    concurrentSet.forEach(1L, element -> {
                        // Just consume the element
                    });
                    
                    String searchResult = concurrentSet.searchElements(1L, element -> {
                        if (element.contains("thread" + threadId)) {
                            return element;
                        }
                        return null;
                    });
                    
                    Integer lengthSum = concurrentSet.reduceElements(1L,
                        String::length,
                        Integer::sum
                    );
                    
                    // Verify we got reasonable results
                    if (searchResult != null) {
                        assertTrue(searchResult.contains("thread" + threadId));
                    }
                    assertTrue(lengthSum > 0);
                    
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        
        // Verify set is in a consistent state
        assertTrue(concurrentSet.size() > 4); // Original 4 plus added elements
        assertTrue(concurrentSet.elementCount() > 4);
    }

    @Test
    void testFallbackBehavior_NonConcurrentBacking() {
        // Test that fallback behavior works for non-concurrent backing
        AtomicInteger counter = new AtomicInteger(0);
        
        // Test forEach fallback
        regularSet.forEach(1L, element -> counter.incrementAndGet());
        assertEquals(4, counter.get());
        
        // Test searchElements fallback
        String result = regularSet.searchElements(1L, element -> {
            if (element.toLowerCase().equals("alpha")) {
                return "Found: " + element;
            }
            return null;
        });
        assertEquals("Found: Alpha", result);
        
        // Test reduceElements fallback
        Integer totalLength = regularSet.reduceElements(1L,
            String::length,
            Integer::sum
        );
        assertEquals(19, totalLength.intValue());
    }

    @Test
    void testCaseInsensitiveOperations() {
        // Test that concurrent operations respect case-insensitive semantics
        concurrentSet.add("test");
        concurrentSet.add("TEST"); // Should not add duplicate
        concurrentSet.add("Test"); // Should not add duplicate
        
        assertEquals(5, concurrentSet.size()); // Original 4 + 1 new unique element
        
        // Test search with case-insensitive behavior
        String result = concurrentSet.searchElements(1L, element -> {
            if (element.equalsIgnoreCase("TEST")) {
                return "Found: " + element;
            }
            return null;
        });
        
        assertEquals("Found: test", result); // Should find the first added version
    }

    @Test
    void testThreadSafety_ElementCount() throws InterruptedException {
        // Test that elementCount is thread-safe
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicReference<Exception> exception = new AtomicReference<>();
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        long count = concurrentSet.elementCount();
                        assertTrue(count >= 4); // At least the original 4 elements
                        concurrentSet.add("thread_element_" + Thread.currentThread().getId() + "_" + j);
                    }
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        assertNull(exception.get(), "Should not have thrown any exceptions");
        assertTrue(concurrentSet.elementCount() > 4);
    }

    @Test
    void testParallelismThresholdBehavior() {
        // Test different parallelism threshold values
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        
        // High threshold (should be sequential)
        concurrentSet.forEach(Long.MAX_VALUE, element -> counter1.incrementAndGet());
        
        // Low threshold (should be parallel for concurrent backing)
        concurrentSet.forEach(1L, element -> counter2.incrementAndGet());
        
        // Both should process all elements
        assertEquals(4, counter1.get());
        assertEquals(4, counter2.get());
    }

    @Test
    void testMixedTypeHandling() {
        // Test that concurrent operations work with mixed types (not just String)
        CaseInsensitiveSet<Object> mixedSet = new CaseInsensitiveSet<>(new ConcurrentSet<>());
        mixedSet.add("String1");
        mixedSet.add(42);
        mixedSet.add("string2");
        mixedSet.add(3.14);
        
        // Test elementCount
        assertEquals(4L, mixedSet.elementCount());
        
        // Test forEach
        AtomicInteger stringCount = new AtomicInteger(0);
        mixedSet.forEach(1L, element -> {
            if (element instanceof String) {
                stringCount.incrementAndGet();
            }
        });
        assertEquals(2, stringCount.get());
        
        // Test search for non-String element
        Object numberResult = mixedSet.searchElements(1L, element -> {
            if (element instanceof Number) {
                return element;
            }
            return null;
        });
        assertNotNull(numberResult);
        assertTrue(numberResult instanceof Number);
    }
}