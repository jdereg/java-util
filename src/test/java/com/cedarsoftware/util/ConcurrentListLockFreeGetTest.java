package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ConcurrentList.get(index) reads without the lock when no write is under way, checked by a version that every write
 * moves, and falls back to the read lock otherwise. A read that mixed two states of the list -- the head of one and
 * the element slot of another -- would see a slot a write had emptied (null: the list holds none here) or step back
 * in a window that only slides forward. Readers hammer get() while writers slide the window from both ends and
 * insert and remove in the middle, across bucket boundaries.
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
class ConcurrentListLockFreeGetTest {

    private static final int WINDOW = 1500;          // spans two buckets of 1024
    private static final int READERS = 4;

    /** The window slides forward: element i only grows, and is never missing. */
    @Test
    @Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void aSlidingWindowNeverStepsBack() throws Exception {
        ConcurrentList<Long> list = new ConcurrentList<>();
        for (long i = 0; i < WINDOW; i++) {
            list.addLast(i);
        }
        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<String> failure = new AtomicReference<>();
        Thread writer = new Thread(() -> {
            long next = WINDOW;
            while (!stop.get()) {
                list.addLast(next++);                // the window moves forward by one: [a + 1, a + WINDOW]
                list.removeFirst();
            }
        });
        List<Thread> readers = new ArrayList<>();
        CountDownLatch done = new CountDownLatch(READERS);
        for (int r = 0; r < READERS; r++) {
            final int offset = r * 211;
            readers.add(new Thread(() -> {
                long[] last = new long[WINDOW - 1];
                for (int round = 0; round < 3_000 && failure.get() == null; round++) {
                    for (int k = 0; k < WINDOW - 1; k++) {
                        int i = (k + offset) % (WINDOW - 1);
                        Long value = list.get(i);
                        if (value == null || value < last[i]) {
                            failure.compareAndSet(null, "get(" + i + ") = " + value + " after " + last[i]);
                            break;
                        }
                        last[i] = value;
                    }
                }
                done.countDown();
            }));
        }
        writer.start();
        readers.forEach(Thread::start);
        done.await(50, TimeUnit.SECONDS);
        stop.set(true);
        writer.join();
        assertNull(failure.get(), failure.get());
    }

    /** Middle inserts and removes shift whole runs of slots; a reader never sees an emptied one. */
    @Test
    @Timeout(value = 60, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    void middleShiftsNeverShowAMissingElement() throws Exception {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        for (int i = 0; i < WINDOW; i++) {
            list.addLast(i);
        }
        AtomicBoolean stop = new AtomicBoolean();
        AtomicReference<String> failure = new AtomicReference<>();
        Thread writer = new Thread(() -> {
            int k = 0;
            while (!stop.get()) {
                int at = 1 + (k++ * 7919) % (WINDOW - 2);
                Integer moved = list.remove(at);             // front or back side shifts, by where `at` falls
                list.add(at, moved);
                list.addFirst(list.removeLast());            // and the ends move too, across buckets
                list.addLast(list.removeFirst());
            }
        });
        List<Thread> readers = new ArrayList<>();
        CountDownLatch done = new CountDownLatch(READERS);
        for (int r = 0; r < READERS; r++) {
            readers.add(new Thread(() -> {
                for (int round = 0; round < 3_000 && failure.get() == null; round++) {
                    for (int i = 0; i < WINDOW - 1; i++) {
                        Integer value = list.get(i);
                        if (value == null) {
                            failure.compareAndSet(null, "get(" + i + ") = null");
                            break;
                        }
                    }
                }
                done.countDown();
            }));
        }
        writer.start();
        readers.forEach(Thread::start);
        done.await(50, TimeUnit.SECONDS);
        stop.set(true);
        writer.join();
        assertNull(failure.get(), failure.get());
        assertEquals(WINDOW, list.size());
    }
}
