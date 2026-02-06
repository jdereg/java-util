package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test for bug #3: compareCollections skips trailing elements after Set sections.
 *
 * Bug: In compareCollections(), the unconditional i++ at the end of the while loop
 * overcounts after the Set branch, which already increments i for SET_OPEN,
 * each set element, and SET_CLOSE. This causes the loop to terminate early,
 * skipping comparison of elements that follow a Set in the expanded key.
 *
 * The bug is only observable when:
 * 1. flattenDimensions=true (no outer OPEN/CLOSE wrapper to absorb the overcount)
 * 2. Both stored and lookup keys are expanded to Collections (triggering compareCollections)
 * 3. A hash collision forces compareCollections to be called on keys that differ
 *    only in trailing (post-Set) elements
 *
 * We use "Aa" and "BB" which have the same Java String hashCode (2112) to force
 * the hash collision that triggers the element-by-element comparison.
 */
class MultiKeyMapCompareCollectionsSetBugTest {

    @Test
    void testTrailingElementAfterSetIsCompared() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(true)
                .build();

        Set<String> set = new HashSet<>(Arrays.asList("x", "y"));

        // Store with trailing element "Aa"
        map.put(new Object[]{set, "Aa"}, "found");

        // Lookup with trailing element "BB" (different value, same hashCode as "Aa")
        // This should NOT find the entry because "Aa" != "BB"
        String result = (String) map.get(new Object[]{new HashSet<>(Arrays.asList("x", "y")), "BB"});
        assertNull(result, "Keys differ in trailing element after Set; should not match");
    }

    @Test
    void testTrailingElementAfterSetMatchesCorrectly() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(true)
                .build();

        Set<String> set = new HashSet<>(Arrays.asList("x", "y"));

        // Store with trailing element "Aa"
        map.put(new Object[]{set, "Aa"}, "found");

        // Lookup with the same trailing element "Aa" - should find the entry
        String result = (String) map.get(new Object[]{new HashSet<>(Arrays.asList("x", "y")), "Aa"});
        assertEquals("found", result, "Keys are identical; should match");
    }

    @Test
    void testMultipleTrailingElementsAfterSet() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(true)
                .build();

        Set<String> set = new HashSet<>(Arrays.asList("a", "b"));

        // Store: [SET_OPEN, "a", "b", SET_CLOSE, "Aa", "extra"]
        map.put(new Object[]{set, "Aa", "extra"}, "found");

        // Lookup with different trailing element "BB" instead of "Aa"
        String result = (String) map.get(new Object[]{new HashSet<>(Arrays.asList("a", "b")), "BB", "extra"});
        assertNull(result, "Keys differ in first trailing element after Set; should not match");
    }

    @Test
    void testSetInMiddleWithTrailingElements() {
        MultiKeyMap<String> map = MultiKeyMap.<String>builder()
                .flattenDimensions(true)
                .build();

        Set<String> set = new HashSet<>(Arrays.asList("x", "y"));

        // Store: ["prefix", SET_OPEN, "x", "y", SET_CLOSE, "Aa"]
        map.put(new Object[]{"prefix", set, "Aa"}, "found");

        // Lookup with different trailing element
        String result = (String) map.get(new Object[]{"prefix", new HashSet<>(Arrays.asList("x", "y")), "BB"});
        assertNull(result, "Keys differ in trailing element after Set (with prefix); should not match");
    }
}
