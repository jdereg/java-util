package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Random operations on a ConcurrentList and an ArrayList, compared after every step: contents, indexOf,
 * lastIndexOf, contains, hashCode, equals both ways and toString. Runs push the list across bucket boundaries
 * (1,024 elements a bucket) at both ends, head below zero included, where the bucket arithmetic is easiest to get
 * wrong.
 */
class ConcurrentListDifferentialTest {

    @Test
    void everyStepAnswersAsArrayListDoes() {
        for (long seed = 1; seed <= 12; seed++) {
            Random random = new Random(seed);
            ConcurrentList<Integer> list = new ConcurrentList<>();
            List<Integer> reference = new ArrayList<>();
            for (int step = 0; step < 400; step++) {
                String op = step(random, list, reference);
                check("seed " + seed + " step " + step + " " + op, list, reference, random);
            }
        }
    }

    private static String step(Random random, ConcurrentList<Integer> list, List<Integer> reference) {
        int value = random.nextInt(50);
        int size = reference.size();
        switch (random.nextInt(16)) {
            case 0:
            case 1: {
                int n = 1 + random.nextInt(900);
                List<Integer> block = new ArrayList<>();
                for (int i = 0; i < n; i++) {
                    block.add(random.nextInt(50));
                }
                list.addAll(block);
                reference.addAll(block);
                return "addAll " + n;
            }
            case 2: {
                int n = 1 + random.nextInt(700);
                for (int i = 0; i < n; i++) {
                    int v = random.nextInt(50);
                    list.addFirst(v);
                    reference.add(0, v);
                }
                return "addFirst x" + n;
            }
            case 3: {
                int n = random.nextInt(Math.max(1, size));
                for (int i = 0; i < n; i++) {
                    assertEquals(reference.remove(0), list.pollFirst());
                }
                return "pollFirst x" + n;
            }
            case 4: {
                int n = random.nextInt(Math.max(1, size));
                for (int i = 0; i < n; i++) {
                    assertEquals(reference.remove(reference.size() - 1), list.pollLast());
                }
                return "pollLast x" + n;
            }
            case 5:
                list.add(value);
                reference.add(value);
                return "add";
            case 6: {
                int index = random.nextInt(size + 1);
                list.add(index, value);
                reference.add(index, value);
                return "add(" + index + ")";
            }
            case 7: {
                if (size == 0) {
                    return "noop";
                }
                int index = random.nextInt(size);
                assertEquals(reference.remove(index), list.remove(index));
                return "remove(" + index + ")";
            }
            case 8: {
                if (size == 0) {
                    return "noop";
                }
                int index = random.nextInt(size);
                assertEquals(reference.set(index, value), list.set(index, value));
                return "set(" + index + ")";
            }
            case 9:
                assertEquals(reference.remove((Object) value), list.remove((Object) value));
                return "remove(Object)";
            case 10: {
                int last = reference.lastIndexOf(value);
                boolean removed = last >= 0;
                if (removed) {
                    reference.remove(last);
                }
                assertEquals(removed, list.removeLastOccurrence(value));
                return "removeLastOccurrence";
            }
            case 11: {
                List<Integer> other = Arrays.asList(value, value + 1, value + 2);
                assertEquals(reference.removeAll(other), list.removeAll(other));
                return "removeAll";
            }
            case 12: {
                List<Integer> other = new ArrayList<>();
                for (int i = 0; i < 40; i++) {
                    other.add(random.nextInt(50));
                }
                assertEquals(reference.retainAll(other), list.retainAll(other));
                return "retainAll";
            }
            case 13: {
                int index = random.nextInt(size + 1);
                List<Integer> block = Arrays.asList(value, value, value + 3);
                assertEquals(reference.addAll(index, block), list.addAll(index, block));
                return "addAll(" + index + ")";
            }
            case 14:
                if (random.nextInt(8) == 0) {
                    list.clear();
                    reference.clear();
                    return "clear";
                }
                return "noop";
            default: {
                Object[] array = list.toArray(new Integer[0]);
                assertEquals(reference, Arrays.asList(array));
                return "toArray(T[])";
            }
        }
    }

    private static void check(String where, ConcurrentList<Integer> list, List<Integer> reference, Random random) {
        assertEquals(reference.size(), list.size(), where);
        assertEquals(reference, Arrays.asList(list.toArray()), where);
        for (int k = 0; k < 3; k++) {
            int value = random.nextInt(52);
            assertEquals(reference.indexOf(value), list.indexOf(value), where + " indexOf " + value);
            assertEquals(reference.lastIndexOf(value), list.lastIndexOf(value), where + " lastIndexOf " + value);
            assertEquals(reference.contains(value), list.contains(value), where + " contains " + value);
        }
        assertEquals(reference.hashCode(), list.hashCode(), where);
        assertEquals(reference, list, where);
        assertEquals(list, reference, where);
        assertEquals(reference.toString(), list.toString(), where);
        if (!reference.isEmpty()) {
            int index = random.nextInt(reference.size());
            assertEquals(reference.get(index), list.get(index), where + " get " + index);
        }
    }
}
