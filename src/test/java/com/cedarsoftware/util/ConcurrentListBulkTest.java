package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ConcurrentList's bulk operations given another ConcurrentList. Each held its own write lock while iterating or
 * querying the other list, which takes that list's read lock: a.op(b) and b.op(a) at the same moment deadlocked.
 */
class ConcurrentListBulkTest {

    private static void raceBothWays(String name, BiConsumer<ConcurrentList<Integer>, ConcurrentList<Integer>> op)
            throws Exception {
        for (int round = 0; round < 300; round++) {
            ConcurrentList<Integer> a = new ConcurrentList<>();
            ConcurrentList<Integer> b = new ConcurrentList<>();
            for (int i = 0; i < 50; i++) {
                a.add(i);
                b.add(i * 2);
            }
            CyclicBarrier together = new CyclicBarrier(2);
            Thread first = new Thread(() -> {
                await(together);
                op.accept(a, b);
            });
            Thread second = new Thread(() -> {
                await(together);
                op.accept(b, a);
            });
            first.setDaemon(true);
            second.setDaemon(true);
            first.start();
            second.start();
            first.join(5000);
            second.join(5000);
            assertFalse(first.isAlive() || second.isAlive(), name + " deadlocked in round " + round);
        }
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void addAllOfEachOtherDoesNotDeadlock() throws Exception {
        raceBothWays("addAll", ConcurrentList::addAll);
    }

    @Test
    void addAllAtAnIndexOfEachOtherDoesNotDeadlock() throws Exception {
        raceBothWays("addAll(index)", (x, y) -> x.addAll(1, y));
    }

    @Test
    void removeAllOfEachOtherDoesNotDeadlock() throws Exception {
        raceBothWays("removeAll", ConcurrentList::removeAll);
    }

    @Test
    void retainAllOfEachOtherDoesNotDeadlock() throws Exception {
        raceBothWays("retainAll", ConcurrentList::retainAll);
    }

    /**
     * remove(Object) ran equals under the write lock. With lists that hold each other, a.remove(probe) compared
     * the probe with b, iterating b under b's read lock, while b.remove(probe) held b's write lock and iterated a.
     */
    @Test
    void removeComparingWithTheOtherListDoesNotDeadlock() throws Exception {
        for (int round = 0; round < 300; round++) {
            ConcurrentList<Object> a = new ConcurrentList<>();
            ConcurrentList<Object> b = new ConcurrentList<>();
            a.add(b);
            b.add(a);
            CyclicBarrier together = new CyclicBarrier(2);
            Thread first = new Thread(() -> {
                await(together);
                a.remove(new ArrayList<>());
            });
            Thread second = new Thread(() -> {
                await(together);
                b.remove(new ArrayList<>());
            });
            first.setDaemon(true);
            second.setDaemon(true);
            first.start();
            second.start();
            first.join(5000);
            second.join(5000);
            assertFalse(first.isAlive() || second.isAlive(), "remove(Object) deadlocked in round " + round);
        }
    }

    /** The bulk operations still answer as ArrayList's do, the list given itself included. */
    @Test
    void bulkOperationsAnswerAsArrayListDoes() {
        List<Integer> source = Arrays.asList(5, 1, 4, 1, 5, 9, 2, 6, 5, 3);
        List<Integer> other = Arrays.asList(1, 5, 7);
        for (int op = 0; op < 7; op++) {
            ConcurrentList<Integer> list = new ConcurrentList<>(source);
            List<Integer> reference = new ArrayList<>(source);
            boolean expected;
            boolean actual;
            switch (op) {
                case 0:
                    expected = reference.removeAll(other);
                    actual = list.removeAll(other);
                    break;
                case 1:
                    expected = reference.retainAll(other);
                    actual = list.retainAll(other);
                    break;
                case 2:
                    expected = reference.addAll(3, other);
                    actual = list.addAll(3, other);
                    break;
                case 3:
                    expected = reference.remove((Object) 5);
                    actual = list.remove((Object) 5);
                    break;
                case 4:
                    expected = reference.removeAll(reference);
                    actual = list.removeAll(list);
                    break;
                case 5:
                    expected = reference.retainAll(reference);
                    actual = list.retainAll(list);
                    break;
                default:
                    expected = reference.addAll(reference);
                    actual = list.addAll(list);
                    break;
            }
            assertEquals(expected, actual, "op " + op);
            assertEquals(reference, new ArrayList<>(list), "op " + op);
        }
        ConcurrentList<Integer> list = new ConcurrentList<>(source);
        assertTrue(list.removeLastOccurrence(5));
        assertEquals(Arrays.asList(5, 1, 4, 1, 5, 9, 2, 6, 3), new ArrayList<>(list));
    }

    /**
     * remove(Object) searches in doubling chunks; a match at any chunk's last index (or a bucket's) is removed, and
     * the elements after it stay.
     */
    @Test
    void removeFindsAMatchAtEveryChunkBoundary() {
        for (int at : new int[]{0, 15, 16, 31, 32, 63, 64, 127, 1023, 1024, 1999}) {
            ConcurrentList<Integer> list = new ConcurrentList<>();
            for (int i = 0; i < 2000; i++) {
                list.add(i == at ? -1 : i);
            }
            assertTrue(list.remove((Object) (-1)), "match at " + at);
            assertEquals(1999, list.size(), "match at " + at);
            assertEquals(-1, list.indexOf(-1), "match at " + at);
            assertEquals(Integer.valueOf(at == 1999 ? 1998 : 1999), list.get(1998), "match at " + at);
            assertFalse(list.remove((Object) (-1)), "match at " + at);
        }
    }

    /**
     * removeIf, replaceAll and sort (and Collections.sort) worked through the snapshot iterator's remove and set,
     * which throw: each failed with UnsupportedOperationException. They now answer as ArrayList does.
     */
    @Test
    void removeIfReplaceAllAndSortAnswerAsArrayListDoes() {
        List<Integer> source = Arrays.asList(5, 1, 4, 1, 5, 9, 2, 6, 5, 3);
        ConcurrentList<Integer> list = new ConcurrentList<>(source);
        List<Integer> reference = new ArrayList<>(source);
        assertEquals(reference.removeIf(x -> x == 5), list.removeIf(x -> x == 5));
        assertEquals(reference, new ArrayList<>(list));
        assertFalse(list.removeIf(x -> x > 100));

        list.replaceAll(x -> x * 10);
        reference.replaceAll(x -> x * 10);
        assertEquals(reference, new ArrayList<>(list));

        list.sort(null);
        reference.sort(null);
        assertEquals(reference, new ArrayList<>(list));

        list.sort(Collections.reverseOrder());
        reference.sort(Collections.reverseOrder());
        assertEquals(reference, new ArrayList<>(list));

        ConcurrentList<String> words = new ConcurrentList<>(Arrays.asList("pear", "fig", "apple"));
        Collections.sort(words);
        assertEquals(Arrays.asList("apple", "fig", "pear"), new ArrayList<>(words));
    }
}
