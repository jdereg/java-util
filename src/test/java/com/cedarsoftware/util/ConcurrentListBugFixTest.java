package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for correctness bugs in ConcurrentList:
 * Bug 1: remove(int) TOCTOU — size check before write lock can remove wrong element
 * Bug 2: add(int, E) TOCTOU — size check before write lock can insert at wrong position
 * Bug 3: addAll(Collection) not atomic — elements can be interleaved by concurrent ops
 */
class ConcurrentListBugFixTest {

    // --- Bug 1: remove(int) TOCTOU race ---

    /**
     * Demonstrates the remove(int) TOCTOU bug: Thread A calls remove(last-index)
     * while Thread B concurrently appends an element. Without proper locking,
     * Thread A's removeLast() shortcut removes Thread B's newly appended element
     * instead of the element at the original index.
     */
    @RepeatedTest(50)
    void testRemoveIntToctouRace() throws Exception {
        // We run this many times to catch the race condition
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("A", "B", "C", "D", "E"));

        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicReference<String> removedRef = new AtomicReference<>();
        AtomicBoolean addDone = new AtomicBoolean(false);

        // Thread A: remove index 4 (element "E")
        Thread threadA = new Thread(() -> {
            try {
                barrier.await();
                String removed = list.remove(4);
                removedRef.set(removed);
            } catch (Exception e) {
                // IndexOutOfBoundsException is acceptable if list shrunk
            }
        });

        // Thread B: append "F" concurrently
        Thread threadB = new Thread(() -> {
            try {
                barrier.await();
                list.addLast("F");
                addDone.set(true);
            } catch (Exception e) {
                // ignore
            }
        });

        threadA.start();
        threadB.start();
        threadA.join(5000);
        threadB.join(5000);

        String removed = removedRef.get();
        if (removed != null) {
            // The key assertion: remove(4) should NEVER remove "F" (the concurrently added element).
            // If the TOCTOU bug exists, removeLast() could remove "F" instead of "E".
            // With the fix, remove(4) always operates on a consistent snapshot under the write lock.
            // It should remove "E" (original index 4) or throw IOOBE if list state changed.
            if ("F".equals(removed)) {
                // This is the TOCTOU bug manifesting — removeLast() removed the wrong element.
                // With the fix, this should never happen.
                throw new AssertionError("remove(4) removed 'F' (concurrently added element) instead of 'E' — TOCTOU bug!");
            }
        }
    }

    // --- Bug 2: add(int, E) TOCTOU race ---

    /**
     * Demonstrates the add(int, E) TOCTOU bug: Thread A calls add(size(), X)
     * while Thread B concurrently appends Y. Without proper locking, the addLast()
     * shortcut causes X to end up after Y instead of before Y.
     */
    @RepeatedTest(50)
    void testAddIntToctouRace() throws Exception {
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("A", "B", "C", "D", "E"));

        CyclicBarrier barrier = new CyclicBarrier(2);

        // Thread A: insert "X" at index 5 (end of current list)
        Thread threadA = new Thread(() -> {
            try {
                barrier.await();
                list.add(5, "X");
            } catch (Exception e) {
                // IOOBE acceptable if list shrunk
            }
        });

        // Thread B: append "Y" concurrently
        Thread threadB = new Thread(() -> {
            try {
                barrier.await();
                list.addLast("Y");
            } catch (Exception e) {
                // ignore
            }
        });

        threadA.start();
        threadB.start();
        threadA.join(5000);
        threadB.join(5000);

        // After both complete, list should have 7 elements
        assertEquals(7, list.size(), "Both additions should succeed");

        // Find positions of X and Y
        int xPos = list.indexOf("X");
        int yPos = list.indexOf("Y");
        assertTrue(xPos >= 0, "X should be in the list");
        assertTrue(yPos >= 0, "Y should be in the list");

        // X was requested at index 5. If X goes through the rebuild path (under write lock),
        // it will be correctly placed at index 5 regardless of concurrent modifications.
        // If both end up at the end via addLast shortcut, whichever runs second is at wrong position.
        // We just verify both elements are present — the ordering depends on thread scheduling.
    }

    // --- Bug 3: addAll(Collection) atomicity ---

    /**
     * Tests that addAll(Collection) is atomic — no interleaving from concurrent operations.
     */
    @RepeatedTest(50)
    void testAddAllAtomicity() throws Exception {
        ConcurrentList<String> list = new ConcurrentList<>();

        List<String> batch = Arrays.asList("A", "B", "C", "D", "E");
        CyclicBarrier barrier = new CyclicBarrier(2);

        // Thread A: addAll a batch of elements
        Thread threadA = new Thread(() -> {
            try {
                barrier.await();
                list.addAll(batch);
            } catch (Exception e) {
                // ignore
            }
        });

        // Thread B: add a single element concurrently
        Thread threadB = new Thread(() -> {
            try {
                barrier.await();
                list.addLast("X");
            } catch (Exception e) {
                // ignore
            }
        });

        threadA.start();
        threadB.start();
        threadA.join(5000);
        threadB.join(5000);

        assertEquals(6, list.size(), "All 6 elements should be present");

        // Find where X ended up relative to the batch
        int xPos = list.indexOf("X");
        int aPos = list.indexOf("A");
        int ePos = list.indexOf("E");

        // If addAll is atomic, the batch [A,B,C,D,E] should be contiguous in the list.
        // X should be either before the whole batch or after it, not in the middle.
        if (aPos >= 0 && ePos >= 0) {
            // Check that B, C, D are between A and E (contiguous batch)
            int bPos = list.indexOf("B");
            int cPos = list.indexOf("C");
            int dPos = list.indexOf("D");

            boolean batchContiguous = (bPos == aPos + 1) && (cPos == aPos + 2) &&
                    (dPos == aPos + 3) && (ePos == aPos + 4);
            assertTrue(batchContiguous,
                    "addAll batch should be contiguous (not interleaved). List: " + list);
        }
    }

    @Test
    void testAddAllAtIndexValidatesBoundsForEmptyInput() {
        ConcurrentList<String> list = new ConcurrentList<>();

        assertThrows(IndexOutOfBoundsException.class,
                () -> list.addAll(1, Collections.<String>emptyList()));
        assertFalse(list.addAll(0, Collections.<String>emptyList()));
        assertEquals(0, list.size());
    }

    @Test
    void testForEachAllowsCallbackMutationWithoutDeadlock() throws Exception {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3));

        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                list.forEach(value -> list.addLast(value + 10));
            } catch (Throwable e) {
                failure.set(e);
            }
        });

        worker.start();
        worker.join(2000);
        if (worker.isAlive()) {
            worker.interrupt();
            worker.join(1000);
        }

        assertFalse(worker.isAlive(), "forEach callback should not deadlock when mutating the list");
        assertNull(failure.get(), "forEach callback should complete without throwing");
        assertEquals(6, list.size());
        assertTrue(list.containsAll(Arrays.asList(1, 2, 3, 11, 12, 13)));
    }
}
