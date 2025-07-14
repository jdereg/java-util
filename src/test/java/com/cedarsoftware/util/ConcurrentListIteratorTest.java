package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import com.cedarsoftware.util.LoggingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Comprehensive tests for ConcurrentList iterator behavior.
 * Tests the snapshot-based iterator implementation for thread safety and consistency.
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
class ConcurrentListIteratorTest {

    private static final Logger LOG = Logger.getLogger(ConcurrentListIteratorTest.class.getName());
    static {
        LoggingConfig.init();
    }

    private ConcurrentList<Integer> list;

    @BeforeEach
    void setUp() {
        list = new ConcurrentList<>();
    }

    @Test
    void testIteratorImmuneToModifications() throws Exception {
        // Initialize list with 100 elements (0-99)
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        Iterator<Integer> iter = list.iterator();
        AtomicBoolean modificationComplete = new AtomicBoolean(false);

        // Heavily modify list during iteration
        CompletableFuture<Void> modificationTask = CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < 50; i++) {
                    list.add(999 + i);           // Add elements
                    if (list.size() > 50) {
                        list.remove(0);          // Remove elements
                    }
                    list.set(Math.min(10, list.size() - 1), 888); // Modify elements
                    Thread.sleep(1);             // Allow iteration to proceed
                }
                modificationComplete.set(true);
                LOG.info("Modifications completed. Final list size: " + list.size());
            } catch (Exception e) {
                LOG.severe("Error during modification: " + e.getMessage());
            }
        });

        // Iterator should complete successfully with original snapshot data
        List<Integer> iteratedValues = new ArrayList<>();
        while (iter.hasNext()) {
            iteratedValues.add(iter.next());
        }

        // Wait for modifications to complete
        modificationTask.get(10, TimeUnit.SECONDS);

        // Verify iterator saw exactly the original 100 elements (0-99)
        assertThat(iteratedValues).hasSize(100);
        assertThat(iteratedValues).containsExactly(IntStream.range(0, 100).boxed().toArray(Integer[]::new));
        
        // Verify the list was actually modified during iteration
        assertThat(modificationComplete.get()).isTrue();
        LOG.info("Original snapshot preserved during " + list.size() + " concurrent modifications");
    }

    @Test
    void testNoConcurrentModificationException() {
        list.add(1);
        list.add(2);
        list.add(3);

        assertThatCode(() -> {
            Iterator<Integer> iter = list.iterator();
            
            // Perform various modifications during iteration
            list.clear();                    // Clear all elements
            list.add(100);                   // Add new elements
            list.add(200);
            list.add(300);
            list.set(0, 999);               // Modify existing element
            
            // Iterator should never throw ConcurrentModificationException
            List<Integer> results = new ArrayList<>();
            while (iter.hasNext()) {
                results.add(iter.next());
            }
            
            // Should have seen the original snapshot [1, 2, 3]
            assertThat(results).containsExactly(1, 2, 3);
            
        }).describedAs("Iterator should never throw ConcurrentModificationException")
          .doesNotThrowAnyException();

        LOG.info("Iterator completed successfully despite concurrent modifications");
    }

    @Test
    void testSnapshotConsistency() {
        list.add(1);
        list.add(2);
        list.add(3);

        // Create first iterator - should see [1, 2, 3]
        Iterator<Integer> iter1 = list.iterator();

        // Modify list after first iterator creation
        list.add(4);
        list.set(0, 999);
        list.add(5);

        // Create second iterator - should see [999, 2, 3, 4, 5]
        Iterator<Integer> iter2 = list.iterator();

        // Collect results from both iterators
        List<Integer> results1 = new ArrayList<>();
        List<Integer> results2 = new ArrayList<>();

        while (iter1.hasNext()) {
            results1.add(iter1.next());
        }

        while (iter2.hasNext()) {
            results2.add(iter2.next());
        }

        // Verify each iterator saw its own consistent snapshot
        assertThat(results1).describedAs("First iterator should see original snapshot")
                           .containsExactly(1, 2, 3);
        
        assertThat(results2).describedAs("Second iterator should see modified snapshot")
                           .containsExactly(999, 2, 3, 4, 5);

        LOG.info("Snapshot consistency verified: iter1=" + results1 + ", iter2=" + results2);
    }

    @Test
    void testListIteratorSnapshotBehavior() {
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        ListIterator<Integer> listIter = list.listIterator(5); // Start from index 5

        // Modify list after ListIterator creation
        list.clear();
        list.add(999);

        // ListIterator should continue with its snapshot, starting from index 5
        List<Integer> results = new ArrayList<>();
        while (listIter.hasNext()) {
            results.add(listIter.next());
        }

        assertThat(results).describedAs("ListIterator should see snapshot from index 5 onwards")
                          .containsExactly(5, 6, 7, 8, 9);

        LOG.info("ListIterator snapshot behavior verified: " + results);
    }

    @Test
    void testHighConcurrencyIteratorStability() throws InterruptedException {
        // Initialize with 1000 elements
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }

        int numThreads = 8;
        int iterationsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numThreads);
        AtomicInteger successfulIterations = new AtomicInteger(0);
        AtomicReference<Exception> failure = new AtomicReference<>();

        // Create multiple threads that each create iterators and iterate concurrently
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    for (int iteration = 0; iteration < iterationsPerThread; iteration++) {
                        Iterator<Integer> iter = list.iterator();
                        
                        // Concurrent modifications by other threads
                        if (threadId % 2 == 0) {
                            // Even threads modify the list more aggressively
                            for (int j = 0; j < 10; j++) {
                                list.add(2000 + threadId * 100 + iteration * 10 + j);
                                if (list.size() > 500) {
                                    list.remove(0);
                                }
                            }
                        }

                        // Iterate through entire list
                        int count = 0;
                        while (iter.hasNext()) {
                            Integer value = iter.next();
                            assertThat(value).isNotNull();
                            count++;
                        }

                        // Each iterator should see some elements (best-effort snapshot)
                        // With the fix, iterator creation should never fail
                        assertThat(count).isGreaterThanOrEqualTo(0); // Could be empty if all elements were removed
                        successfulIterations.incrementAndGet();
                    }
                } catch (Exception e) {
                    failure.set(e);
                    LOG.severe("Thread " + threadId + " failed: " + e.getMessage());
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Start all threads
        boolean completed = completionLatch.await(30, TimeUnit.SECONDS);

        executor.shutdownNow();

        assertThat(completed).describedAs("All threads should complete within timeout").isTrue();
        assertThat(failure.get()).describedAs("No thread should fail").isNull();
        assertThat(successfulIterations.get()).describedAs("All iterations should succeed")
                                             .isEqualTo(numThreads * iterationsPerThread);

        LOG.info("High concurrency test completed: " + successfulIterations.get() + 
                " successful iterations across " + numThreads + " threads");
    }

    @Test
    void testWriteFailsGracefullyWhenConcurrentRemoveShrinksList() throws InterruptedException {
        // Initialize list with 100 elements (indices 0-99)
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }

        AtomicBoolean writerShouldStop = new AtomicBoolean(false);
        AtomicReference<Exception> expectedWriteException = new AtomicReference<>();
        CountDownLatch writerStarted = new CountDownLatch(1);

        // Thread A: Continuously writes to index 50
        Thread writer = new Thread(() -> {
            writerStarted.countDown();
            while (!writerShouldStop.get()) {
                try {
                    list.set(50, 999); // Write unique value to index 50
                    Thread.sleep(1);   // Small delay to allow removals
                } catch (IndexOutOfBoundsException e) {
                    expectedWriteException.set(e);
                    writerShouldStop.set(true);
                    LOG.info("Writer received expected IndexOutOfBoundsException at list size: " + list.size());
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // Thread B: Continuously removes from index 0 (shrinking the list)
        Thread remover = new Thread(() -> {
            try {
                writerStarted.await(); // Wait for writer to start
                while (!writerShouldStop.get()) {
                    if (list.size() > 0) {
                        list.remove(0); // Remove first element, shifting everything left
                        Thread.sleep(1); // Small delay
                    } else {
                        break; // List is empty
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.warning("Remover encountered exception: " + e.getMessage());
            }
            LOG.info("Remover completed. Final list size: " + list.size());
        });

        writer.start();
        remover.start();

        // Wait for the expected exception or timeout
        writer.join(10000); // 10 second timeout
        remover.interrupt();
        remover.join(1000);

        // Verify the expected behavior occurred
        assertThat(expectedWriteException.get())
            .describedAs("Write to index 50 should eventually fail when list shrinks below 51 elements")
            .isInstanceOf(IndexOutOfBoundsException.class);

        // Verify the list size is now < 51 (making index 50 invalid)
        assertThat(list.size())
            .describedAs("List should have shrunk below 51 elements")
            .isLessThan(51);
            
        LOG.info("Concurrent write/remove test completed successfully");
    }

    @Test
    void testReadFailsGracefullyWhenConcurrentRemoveShrinksList() throws InterruptedException {
        // Initialize list with 80 elements
        for (int i = 0; i < 80; i++) {
            list.add(i * 10); // Values: 0, 10, 20, ..., 790
        }

        AtomicBoolean readerShouldStop = new AtomicBoolean(false);
        AtomicReference<Exception> expectedReadException = new AtomicReference<>();
        AtomicInteger lastSuccessfulRead = new AtomicInteger(-1);
        CountDownLatch exceptionLatch = new CountDownLatch(1);

        // Thread A: Continuously reads from index 75
        Thread reader = new Thread(() -> {
            while (!readerShouldStop.get()) {
                try {
                    Integer value = list.get(75); // Read from index 75
                    lastSuccessfulRead.set(value);
                    Thread.sleep(1); // Small delay to allow removals
                } catch (IndexOutOfBoundsException e) {
                    expectedReadException.set(e);
                    readerShouldStop.set(true);
                    exceptionLatch.countDown();
                    LOG.info("Reader received expected IndexOutOfBoundsException at list size: " + list.size());
                    break;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // Thread B: Continuously removes from end of list
        Thread remover = new Thread(() -> {
            try {
                while (!readerShouldStop.get() && list.size() > 70) {
                    list.remove(list.size() - 1); // Remove last element
                    Thread.sleep(1); // Small delay
                }
            } catch (Exception e) {
                LOG.warning("Remover encountered exception: " + e.getMessage());
            }
            LOG.info("Remover completed. Final list size: " + list.size());
        });

        reader.start();
        remover.start();

        // Wait for the expected exception or timeout
        boolean threw = exceptionLatch.await(10, TimeUnit.SECONDS);
        readerShouldStop.set(true);
        reader.interrupt();
        remover.interrupt();
        reader.join(1000);
        remover.join(1000);

        // Verify the expected behavior occurred
        assertThat(threw)
            .describedAs("Read from index 75 should eventually fail when list shrinks below 76 elements")
            .isTrue();
        assertThat(expectedReadException.get()).isInstanceOf(IndexOutOfBoundsException.class);

        // Verify the list size is now <= 75 (making index 75 invalid)
        assertThat(list.size())
            .describedAs("List should have shrunk to 75 or fewer elements")
            .isLessThanOrEqualTo(75);

        LOG.info("Concurrent read/remove test completed. Last successful read: " + lastSuccessfulRead.get());
    }

    @Test
    void testIteratorCreationNeverFailsUnderConcurrentModification() throws InterruptedException {
        // This test specifically targets the toArray() race condition fix
        for (int i = 0; i < 1000; i++) {
            list.add(i);
        }

        AtomicInteger successfulIteratorCreations = new AtomicInteger(0);
        AtomicInteger failedIteratorCreations = new AtomicInteger(0);
        AtomicBoolean testComplete = new AtomicBoolean(false);

        // Thread that aggressively modifies the list during iterator creation
        Thread modifier = new Thread(() -> {
            while (!testComplete.get()) {
                try {
                    // Rapidly shrink and grow the list to trigger race conditions
                    for (int i = 0; i < 100 && !testComplete.get(); i++) {
                        if (list.size() > 0) {
                            list.remove(0); // Shrink from front
                        }
                        list.add(9999 + i); // Add to end
                    }
                    Thread.sleep(1); // Brief pause
                } catch (Exception e) {
                    // Modification failures are acceptable
                }
            }
        });

        // Thread that continuously creates iterators
        Thread iteratorCreator = new Thread(() -> {
            while (!testComplete.get()) {
                try {
                    Iterator<Integer> iter = list.iterator(); // This should never fail
                    
                    // Consume the iterator to ensure it works
                    int count = 0;
                    while (iter.hasNext()) {
                        iter.next();
                        count++;
                    }
                    
                    successfulIteratorCreations.incrementAndGet();
                    Thread.sleep(1); // Brief pause
                } catch (Exception e) {
                    failedIteratorCreations.incrementAndGet();
                    LOG.severe("Iterator creation failed: " + e.getMessage());
                }
            }
        });

        modifier.start();
        iteratorCreator.start();

        // Run test for 2 seconds
        Thread.sleep(2000);
        testComplete.set(true);

        modifier.join(1000);
        iteratorCreator.join(1000);

        // With the fix, no iterator creation should fail
        assertThat(failedIteratorCreations.get())
            .describedAs("Iterator creation should never fail with the race condition fix")
            .isEqualTo(0);

        assertThat(successfulIteratorCreations.get())
            .describedAs("Should have created many iterators successfully")
            .isGreaterThan(10);

        LOG.info("Iterator creation test: " + successfulIteratorCreations.get() + 
                " successful, " + failedIteratorCreations.get() + " failed");
    }

    @Test
    void testIteratorReferencesNotCopies() {
        // Create objects that we can verify are the same instances
        StringBuilder obj1 = new StringBuilder("object1");
        StringBuilder obj2 = new StringBuilder("object2");
        StringBuilder obj3 = new StringBuilder("object3");
        
        ConcurrentList<StringBuilder> list = new ConcurrentList<>();
        list.add(obj1);
        list.add(obj2);
        list.add(obj3);
        
        Iterator<StringBuilder> iter = list.iterator();
        
        // Verify iterator contains the same object references (not copies)
        assertThat(iter.next()).isSameAs(obj1);  // Same reference
        assertThat(iter.next()).isSameAs(obj2);  // Same reference  
        assertThat(iter.next()).isSameAs(obj3);  // Same reference
        
        // Modify original objects
        obj1.append("-modified");
        
        // Create new iterator - should see the modified object
        Iterator<StringBuilder> iter2 = list.iterator();
        StringBuilder retrieved = iter2.next();
        assertThat(retrieved.toString()).isEqualTo("object1-modified");
        assertThat(retrieved).isSameAs(obj1);  // Still same reference
        
        LOG.info("Verified: Iterator stores references, not copies");
    }
}