package com.cedarsoftware.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntPredicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * IntervalSet readers while another thread writes: a read sees the set before or after a write, never halfway.
 */
class IntervalSetConsistencyTest {

    /**
     * Merges 3 intervals into 1 and splits them back, over and over: 0-9, 20-29, 100-139 and 160-199 stay covered,
     * and nothing below 0, from 30 to 99, or from 200 up, is ever covered.
     */
    private static Thread toggler(IntervalSet<Integer> set, AtomicBoolean stop) {
        return new Thread(() -> {
            while (!stop.get()) {
                set.add(10, 20);
                set.remove(10, 20);
                set.remove(140, 160);
                set.add(140, 160);
                set.add(5, 25);
                set.removeRange(10, 20);
                set.add(10, 20);
                set.remove(10, 20);
            }
        });
    }

    /**
     * A value covered before, during and after a split is reported covered. A lock-free floorEntry found the left
     * interval before the split linked the right fragment, then read the left interval's end after the split
     * shrank it, and reported a right-fragment value uncovered (up to thousands of times a second). The values
     * probed are the right fragments' (20-29, 160-199), the only ones a split can hide.
     */
    @Test
    void readersNeverMissCoverageDuringASplit() throws Exception {
        Map<String, Long> wrong = askWhileToggling(new int[]{20, 25, 29, 160, 199}, true, 1000);
        assertEquals(new TreeMap<>(), wrong, "reads that found an always-covered value uncovered");
    }

    /**
     * A value never covered is never reported covered. contains() trusts a covering entry it finds mid-write
     * without validating, which is sound only while every step of a write stores coverage the set held before or
     * will hold after; this pins that.
     */
    @Test
    void readersNeverFindCoverageThatIsNotThere() throws Exception {
        Map<String, Long> wrong = askWhileToggling(new int[]{-1, 30, 50, 99, 200}, false, 500);
        assertEquals(new TreeMap<>(), wrong, "reads that found a never-covered value covered");
    }

    /**
     * While the toggler writes, asks each reader, on its own thread (so one that waits on the writer does not slow
     * the others), whether {@code values} are covered, for {@code millis}; returns the wrong answers per reader.
     */
    private static Map<String, Long> askWhileToggling(int[] values, boolean covered, long millis)
            throws InterruptedException {
        IntervalSet<Integer> set = new IntervalSet<>();
        set.add(0, 10);
        set.add(20, 30);
        set.add(100, 200);
        Map<String, IntPredicate> readers = new LinkedHashMap<>();      // does the reader say v is covered?
        readers.put("contains", set::contains);
        readers.put("intervalContaining", v -> set.intervalContaining(v) != null);
        readers.put("previousInterval", v -> {
            IntervalSet.Interval<Integer> previous = set.previousInterval(v);
            return previous != null && previous.getEnd() > v;
        });
        readers.put("nextInterval", v -> {
            IntervalSet.Interval<Integer> next = set.nextInterval(v);
            return next != null && next.getStart() <= v && next.getEnd() > v;
        });
        AtomicBoolean stop = new AtomicBoolean();
        Thread writer = toggler(set, stop);
        writer.start();
        Map<String, Long> wrong = new ConcurrentSkipListMap<>();
        List<Thread> threads = new ArrayList<>();
        try {
            for (Map.Entry<String, IntPredicate> reader : readers.entrySet()) {
                Thread thread = new Thread(() -> {
                    long misreads = 0;
                    long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
                    while (System.nanoTime() < end) {
                        for (int v : values) {
                            if (reader.getValue().test(v) != covered) {
                                misreads++;
                            }
                        }
                    }
                    if (misreads > 0) {
                        wrong.put(reader.getKey(), misreads);
                    }
                });
                threads.add(thread);
                thread.start();
            }
            for (Thread thread : threads) {
                thread.join();
            }
        } finally {
            stop.set(true);
            writer.join();
        }
        return wrong;
    }

    /**
     * A copy taken while another thread writes is a set the original really held: sorted, no overlap. The copy
     * constructor (behind union and difference too) copied the map with no lock, and kept a half-made write:
     * {[0-100), [60-100)}, its total duration 140.
     */
    @Test
    void aCopyNeverHoldsAHalfMadeWrite() throws Exception {
        IntervalSet<Integer> set = new IntervalSet<>();
        set.add(0, 100);
        AtomicBoolean stop = new AtomicBoolean();
        Thread writer = new Thread(() -> {
            while (!stop.get()) {
                set.remove(40, 60);
                set.add(40, 60);
            }
        });
        writer.start();
        long broken = 0;
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(400);
        try {
            while (System.nanoTime() < end) {
                if (overlaps(new IntervalSet<>(set).snapshot())) {
                    broken++;
                }
                if (overlaps(set.union(new IntervalSet<>()).snapshot())) {
                    broken++;
                }
                if (overlaps(set.snapshot())) {
                    broken++;
                }
            }
        } finally {
            stop.set(true);
            writer.join();
        }
        assertEquals(0, broken, "copies holding overlapping intervals");
    }

    /**
     * The readers that walk the whole set (totalDuration, equals, hashCode, toString, intersection, the range
     * lists) answer for a state the set really held: [0-100) or [0-40) + [60-100). Walking the live map they
     * counted a half-made write, or a state no write produced: [0-40) alone (40 ns), [0-100) + [60-100) (140 ns),
     * and equals found the set equal to {[0-40)} or {[0-60)}.
     */
    @Test
    void wholeSetReadersSeeAStateTheSetHeld() throws Exception {
        IntervalSet<Integer> set = new IntervalSet<>();
        set.add(0, 100);
        IntervalSet<Integer> whole = new IntervalSet<>(set);
        IntervalSet<Integer> split = new IntervalSet<>(set);
        split.remove(40, 60);
        IntervalSet<Integer> everything = new IntervalSet<>(whole);
        IntervalSet<Integer> onlyLeft = new IntervalSet<>();       // shapes a read saw part-way through add(40, 60)
        onlyLeft.add(0, 40);
        IntervalSet<Integer> leftTo60 = new IntervalSet<>();
        leftTo60.add(0, 60);
        Set<String> strings = new HashSet<>(Arrays.asList(whole.toString(), split.toString()));
        Set<Integer> hashes = new HashSet<>(Arrays.asList(whole.hashCode(), split.hashCode()));
        Set<Duration> durations = new HashSet<>(Arrays.asList(Duration.ofNanos(100), Duration.ofNanos(80)));
        AtomicBoolean stop = new AtomicBoolean();
        Thread writer = new Thread(() -> {
            while (!stop.get()) {
                set.remove(40, 60);
                set.add(40, 60);
            }
        });
        writer.start();
        Map<String, Integer> wrong = new TreeMap<>();
        long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(400);
        try {
            while (System.nanoTime() < end) {
                count(wrong, "totalDuration", !durations.contains(set.totalDuration()));
                count(wrong, "toString", !strings.contains(set.toString()));
                count(wrong, "hashCode", !hashes.contains(set.hashCode()));
                count(wrong, "equals", set.equals(onlyLeft) || set.equals(leftTo60) || leftTo60.equals(set));
                count(wrong, "intersection", !durations.contains(set.intersection(everything).totalDuration()));
                List<IntervalSet.Interval<Integer>> range = set.getIntervalsInRange(0, 100);
                count(wrong, "getIntervalsInRange", overlaps(range) || range.size() > 2);
            }
        } finally {
            stop.set(true);
            writer.join();
        }
        assertEquals(new TreeMap<>(), wrong, "readers that answered for a state the set never held");
    }

    private static void count(Map<String, Integer> wrong, String reader, boolean isWrong) {
        if (isWrong) {
            wrong.merge(reader, 1, Integer::sum);
        }
    }

    private static boolean overlaps(List<IntervalSet.Interval<Integer>> intervals) {
        for (int i = 0; i < intervals.size(); i++) {
            IntervalSet.Interval<Integer> interval = intervals.get(i);
            if (interval.getStart() >= interval.getEnd()) {
                return true;
            }
            if (i > 0 && intervals.get(i - 1).getEnd() >= interval.getStart()) {
                return true;
            }
        }
        return false;
    }
}
