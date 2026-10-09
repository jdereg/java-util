package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ConcurrentList inserts and removes in the middle in place: the shorter side moves one slot, and the list keeps
 * exactly the buckets its elements span. It copied the whole list into an ArrayList and rebuilt every bucket from it.
 * Checked against ArrayList over random operations, across bucket boundaries (1024 elements each) and with the head
 * run into negative positions.
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
class ConcurrentListMiddleOpsTest {

    @Test
    void middleInsertsAndRemovesMatchArrayList() throws Exception {
        for (long seed = 1; seed <= 4; seed++) {
            SplittableRandom r = new SplittableRandom(seed);
            ConcurrentList<Integer> list = new ConcurrentList<>();
            List<Integer> expected = new ArrayList<>();
            int next = 0;
            for (int op = 0; op < 50_000; op++) {
                int size = expected.size();
                int kind = r.nextInt(10);
                if (kind < 3 || size < 2) {                          // grow in the middle
                    int at = size == 0 ? 0 : r.nextInt(size + 1);
                    list.add(at, next);
                    expected.add(at, next++);
                } else if (kind < 5) {                               // shrink in the middle
                    int at = r.nextInt(size);
                    assertEquals(expected.remove(at), list.remove(at), "remove " + at);
                } else if (kind == 5) {
                    list.addFirst(next);
                    expected.add(0, next++);
                } else if (kind == 6) {
                    list.addLast(next);
                    expected.add(next++);
                } else if (kind == 7) {
                    assertEquals(expected.remove(0), list.removeFirst());
                } else if (kind == 8) {
                    assertEquals(expected.remove(size - 1), list.removeLast());
                } else {
                    int at = r.nextInt(size);
                    assertEquals(expected.get(at), list.get(at), "get " + at);
                }
                if (op % 997 == 0 || (size > 0 && r.nextInt(64) == 0)) {
                    assertEquals(expected, new ArrayList<>(list), "seed " + seed + " op " + op);
                    assertBucketsSpanTheElements(list);
                }
            }
            assertEquals(expected, new ArrayList<>(list));
            assertBucketsSpanTheElements(list);
        }
    }

    /** Large lists, so the shift crosses several buckets in each direction. */
    @Test
    void shiftsAcrossBuckets() throws Exception {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            list.addFirst(-i);                                       // head well below zero
            expected.add(0, -i);
            list.addLast(i);
            expected.add(i);
        }
        for (int at : new int[] {1, 2047, 4999, 5000, 5001, 7000, 9998}) {
            list.add(at, 100_000 + at);
            expected.add(at, 100_000 + at);
            assertEquals(expected, new ArrayList<>(list), "add " + at);
            assertBucketsSpanTheElements(list);
        }
        for (int at : new int[] {1, 1024, 3000, 5000, 6500, 9000, 9990}) {
            assertEquals(expected.remove(at), list.remove(at), "remove " + at);
            assertEquals(expected, new ArrayList<>(list), "remove " + at);
            assertBucketsSpanTheElements(list);
        }
    }

    private static void assertBucketsSpanTheElements(ConcurrentList<?> list) throws Exception {
        long head = ((AtomicLong) field("head").get(list)).get();
        long tail = ((AtomicLong) field("tail").get(list)).get();
        Map<?, ?> buckets = (Map<?, ?>) field("buckets").get(list);
        assertEquals(tail - head, list.size());
        long spanned = head == tail ? 0 : (Math.floorDiv(tail - 1, 1024) - Math.floorDiv(head, 1024) + 1);
        assertEquals(spanned, buckets.size(), "buckets held for " + head + ".." + tail);
    }

    private static Field field(String name) throws Exception {
        Field f = ConcurrentList.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
