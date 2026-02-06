package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test for bug #4: Small Set comparison (≤6 elements) doesn't track consumed matches.
 *
 * Bug: In the O(n²) small-set comparison path, when a match is found in set2,
 * the matched element is not marked as consumed. Under valueBasedEquality,
 * two distinct elements in set1 can match the SAME element in set2, producing
 * a false positive. The large-set path (>6 elements) correctly removes matched
 * elements via iterator removal, but the small-set path does not.
 *
 * Example: set1 = {Integer(1), Long(1L)}, set2 = {Integer(1), Long(4294967296L)}
 * With valueBasedEquality=true:
 *   - Integer(1) matches Integer(1) in set2
 *   - Long(1L) ALSO matches Integer(1) in set2 (cross-type numeric equality)
 *   - Buggy code returns true (both found) even though Long(4294967296L) != Long(1L)
 *
 * Hash collision setup: Integer(1), Long(1L), and Long(4294967296L) all have
 * hashLong value of 1, so their elemHash is 32 (= 1*31 + 1). Any size-2 set
 * composed of these elements has setHash = rotateLeft(32,1) ^ rotateLeft(32,1) = 0,
 * ensuring the overall MultiKey hashes collide and the comparison code is reached.
 */
class MultiKeyMapSmallSetDoubleMatchTest {

    @Test
    void testSmallSetDoesNotDoubleMatchWithValueEquality() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(true)
                .valueBasedEquality(true)
                .build();

        // Store key with set {Integer(1), Long(1L)}
        // Under value-based equality, Integer(1) and Long(1L) are "equal",
        // but they are distinct in Java Set semantics (Integer.equals(Long) is false)
        Set<Number> storedSet = new HashSet<>();
        storedSet.add(1);          // Integer(1)
        storedSet.add(1L);         // Long(1L)
        assertEquals(2, storedSet.size(), "Precondition: Integer and Long are distinct in HashSet");

        map.put(new Object[]{storedSet}, "value1");

        // Lookup with set {Integer(1), Long(4294967296L)} (4294967296L = 1L << 32)
        // Long(4294967296L) has the same hashLong(1) as Integer(1) and Long(1L),
        // so the overall key hash matches, triggering element-by-element comparison.
        // But Long(4294967296L) != Long(1L) by value, so the sets are different.
        Set<Number> lookupSet = new HashSet<>();
        lookupSet.add(1);              // Integer(1)
        lookupSet.add(4294967296L);    // Long(1L << 32) - different value, same hash
        assertEquals(2, lookupSet.size(), "Precondition: Integer and Long are distinct in HashSet");

        String result = (String) map.get(new Object[]{lookupSet});
        assertNull(result, "Sets differ (Long(1L) vs Long(4294967296L)); should not match");
    }

    @Test
    void testSmallSetMatchesWhenTrulyEqual() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(true)
                .valueBasedEquality(true)
                .build();

        Set<Number> storedSet = new HashSet<>();
        storedSet.add(1);     // Integer(1)
        storedSet.add(1L);    // Long(1L)

        map.put(new Object[]{storedSet}, "value1");

        // Lookup with equivalent set (same elements)
        Set<Number> lookupSet = new HashSet<>();
        lookupSet.add(1);     // Integer(1)
        lookupSet.add(1L);    // Long(1L)

        String result = (String) map.get(new Object[]{lookupSet});
        assertEquals("value1", result, "Sets are identical; should match");
    }

    @Test
    void testSmallSetWithThreeElements() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(true)
                .valueBasedEquality(true)
                .build();

        // set1 = {Integer(1), Long(1L), Short((short)1)}
        // All three are value-equal under valueBasedEquality but distinct in Java Set
        Set<Number> storedSet = new HashSet<>();
        storedSet.add(1);            // Integer(1)
        storedSet.add(1L);           // Long(1L)
        storedSet.add((short) 1);    // Short(1)
        assertEquals(3, storedSet.size(), "Precondition: all three types are distinct in HashSet");

        map.put(new Object[]{storedSet}, "value1");

        // set2 = {Integer(1), Long(4294967296L), Short((short)1)}
        // Integer(1) and Short(1) are in both sets, but Long(4294967296L) != Long(1L)
        Set<Number> lookupSet = new HashSet<>();
        lookupSet.add(1);              // Integer(1)
        lookupSet.add(4294967296L);    // Long(1L << 32) - different value
        lookupSet.add((short) 1);      // Short(1)
        assertEquals(3, lookupSet.size());

        String result = (String) map.get(new Object[]{lookupSet});
        assertNull(result, "Sets differ (Long(1L) vs Long(4294967296L)); should not match");
    }

    @Test
    void testSmallSetDoubleMatchBothDirections() {
        // Test that the fix works regardless of iteration order by using a symmetric case
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(true)
                .valueBasedEquality(true)
                .build();

        // set1 = {Integer(1), Long(1L)} where both value-equal to "1"
        Set<Number> storedSet = new HashSet<>();
        storedSet.add(1);     // Integer(1)
        storedSet.add(1L);    // Long(1L)

        map.put(new Object[]{storedSet}, "value1");

        // set2 = {Long(4294967296L), Short((short)4294967296)} - wait, short overflows.
        // Use: {Long(4294967296L), Integer(2)} - clearly different
        Set<Number> lookupSet = new HashSet<>();
        lookupSet.add(4294967296L);    // Long(1L << 32)
        lookupSet.add(2);              // Integer(2)
        assertEquals(2, lookupSet.size());

        String result = (String) map.get(new Object[]{lookupSet});
        assertNull(result, "Sets are completely different; should not match");
    }
}
