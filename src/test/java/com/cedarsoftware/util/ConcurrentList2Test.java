package com.cedarsoftware.util;

import java.security.SecureRandom;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
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
class ConcurrentList2Test {

    @Test
    void testConcurrentOperations() throws InterruptedException {
        final int numberOfThreads = 6;
        final int numberOfElements = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ConcurrentList<Integer> list = new ConcurrentList<>();

        // Initialize the list with 100 elements (1-100)
        for (int i = 1; i <= numberOfElements; i++) {
            list.add(i);
        }

        // Define random operations on the list
        Runnable modifierRunnable = () -> {
            Random random = new SecureRandom();
            while (true) {
                try {
                    int operation = random.nextInt(3);
                    int value = random.nextInt(1000) + 1000;
                    int index = random.nextInt(list.size());
                    
                    switch (operation) {
                        case 0:
                            list.add(index, value);
                            break;
                        case 1:
                            list.remove(index);
                            break;
                        case 2:
                            list.set(index, value);
                            break;
                    }
                } catch (IndexOutOfBoundsException | IllegalArgumentException | NoSuchElementException e) {
                }
            }
        };

        Runnable iteratorRunnable = () -> {
            Random random = new SecureRandom();
            while (true) {
                try {
                    int start = random.nextInt(random.nextInt(list.size()));
                    Iterator<Integer> it = list.iterator();
                    while (it.hasNext()) { it.next(); }
                } catch (UnsupportedOperationException | IllegalArgumentException | IndexOutOfBoundsException e) {
                }
            }
        };

        Runnable listIteratorRunnable = () -> {
            Random random = new SecureRandom();
            while (true) {
                try {
                    int start = random.nextInt(random.nextInt(list.size()));
                    ListIterator<Integer> it = list.listIterator();
                    while (it.hasNext()) { it.next(); }
                } catch (UnsupportedOperationException | IllegalArgumentException | IndexOutOfBoundsException e) {
                }
            }
        };

        Runnable subListRunnable = () -> {
            Random random = new SecureRandom();
            while (true) {
                try {
                    int x = random.nextInt(99);
                    int y = random.nextInt(99);
                    if (x > y) {
                        int temp = x;
                        x = y;
                        y = temp;
                    }
                    List list2 = list.subList(x, y);
                    Iterator i = list2.iterator();
                    while (i.hasNext()) { i.next(); }
                } catch (IndexOutOfBoundsException e) {
                }
            }
        };

        // Execute the threads
        executor.execute(modifierRunnable);
        executor.execute(modifierRunnable);
        executor.execute(iteratorRunnable);
        executor.execute(iteratorRunnable);
        executor.execute(listIteratorRunnable);
        executor.execute(listIteratorRunnable);

        // Wait for threads to complete (except the continuous validator)
        latch.await(250, TimeUnit.MILLISECONDS);
        executor.shutdownNow();
    }
}
