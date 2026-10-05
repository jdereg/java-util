package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IntervalSet and Interval are equal by value, as every other IntervalSet operation compares: [1, 2.0) equals
 * [1, 2.00), with the same hash code. equals used BigDecimal.equals, which also compares the number of decimal places,
 * so two sets holding the same ranges were unequal, a HashSet could not find one by the other, and a set's equality
 * depended on which spelling of a bound it happened to store first.
 */
class IntervalSetScaleEqualityTest {

    private static IntervalSet<BigDecimal> set(String start, String end) {
        IntervalSet<BigDecimal> set = new IntervalSet<>();
        set.add(new BigDecimal(start), new BigDecimal(end));
        return set;
    }

    @Test
    void theSameRangesInAnotherScaleAreEqual() {
        IntervalSet<BigDecimal> one = set("1", "2.0");
        IntervalSet<BigDecimal> two = set("1", "2.00");
        assertEquals(one, two);
        assertEquals(one.hashCode(), two.hashCode());
        Set<IntervalSet<BigDecimal>> hashed = new HashSet<>(Collections.singleton(one));
        assertTrue(hashed.contains(two));

        assertEquals(one.first(), two.first());
        assertEquals(one.first().hashCode(), two.first().hashCode());

        assertEquals(set("0.0", "1"), set("0", "1.000"));
        assertEquals(set("0.0", "1").hashCode(), set("0", "1.000").hashCode());
        assertEquals(set("-0.00", "1E+1"), set("0", "10"));
        assertEquals(set("-0.00", "1E+1").hashCode(), set("0", "10").hashCode());
    }

    /** Whichever spelling of a bound is stored, the set is the same. */
    @Test
    void theOrderOfAddingDoesNotMatter() {
        IntervalSet<BigDecimal> three = new IntervalSet<>();
        three.add(new BigDecimal("1"), new BigDecimal("2.00"));
        three.add(new BigDecimal("1.0"), new BigDecimal("2.0"));
        assertEquals(1, three.size());
        assertEquals(set("1", "2.0"), three);
        assertEquals(set("1", "2.00"), three);
        assertEquals(set("1", "2.0").hashCode(), three.hashCode());
    }

    @Test
    void differentValuesAreStillUnequal() {
        assertNotEquals(set("1", "2.0"), set("1", "2.1"));
        assertNotEquals(set("1", "2.0").first(), set("1.5", "2.0").first());

        IntervalSet<Integer> ints = new IntervalSet<>();
        ints.add(1, 2);
        IntervalSet<Long> longs = new IntervalSet<>();
        longs.add(1L, 2L);
        assertFalse(ints.equals(longs));                    // different types: unequal, and no exception
        assertFalse(ints.first().equals(longs.first()));

        IntervalSet<Integer> sameInts = new IntervalSet<>();
        sameInts.add(1, 2);
        assertEquals(ints, sameInts);
        assertEquals(ints.hashCode(), sameInts.hashCode());
    }
}
