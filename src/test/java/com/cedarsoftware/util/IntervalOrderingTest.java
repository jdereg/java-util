package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

class IntervalOrderingTest {

    @Test
    void testNaturalOrdering() {
        IntervalSet.Interval<Integer> intervalA = new IntervalSet.Interval<>(5, 10);
        IntervalSet.Interval<Integer> intervalB = new IntervalSet.Interval<>(1, 3);
        IntervalSet.Interval<Integer> intervalC = new IntervalSet.Interval<>(7, 9);
        IntervalSet.Interval<Integer> intervalD = new IntervalSet.Interval<>(2, 6);
        IntervalSet.Interval<Integer> intervalE = new IntervalSet.Interval<>(2, 4);  // same start as D but shorter end

        TreeSet<IntervalSet.Interval<Integer>> set = new TreeSet<>();
        set.add(intervalA);
        set.add(intervalB);
        set.add(intervalC);
        set.add(intervalD);
        set.add(intervalE);

        List<IntervalSet.Interval<Integer>> actualOrder = new ArrayList<>(set);
        List<IntervalSet.Interval<Integer>> expectedOrder = Arrays.asList(
                intervalB,
                intervalE,
                intervalD,
                intervalA,
                intervalC
        );
        assertEquals(expectedOrder, actualOrder, "Intervals should be ordered by start, then end");

        // Verify equals() and hashCode()
        IntervalSet.Interval<Integer> duplicateA = new IntervalSet.Interval<>(5, 10);
        assertEquals(intervalA, duplicateA, "Intervals with same start and end should be equal");
        assertEquals(intervalA.hashCode(), duplicateA.hashCode(), "Equal intervals should have same hash code");
        assertTrue(set.contains(duplicateA), "TreeSet should recognize equal interval by compareTo and equals");
    }
}
