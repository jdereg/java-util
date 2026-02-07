package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for correctness bugs in CaseInsensitiveSet:
 * Bug 1: hashCode() is case-sensitive (uses String.hashCode instead of case-insensitive hash)
 * Bug 2: retainAll() is case-sensitive when given a standard collection
 */
class CaseInsensitiveSetBugFixTest {

    // --- Bug 1: hashCode() should be case-insensitive ---

    @Test
    void testHashCodeSameCaseSameHash() {
        CaseInsensitiveSet<String> set1 = new CaseInsensitiveSet<>();
        set1.add("Hello");
        set1.add("World");

        CaseInsensitiveSet<String> set2 = new CaseInsensitiveSet<>();
        set2.add("Hello");
        set2.add("World");

        assertEquals(set1.hashCode(), set2.hashCode(), "Same case sets should have same hashCode");
    }

    @Test
    void testHashCodeDifferentCaseSameHash() {
        CaseInsensitiveSet<String> set1 = new CaseInsensitiveSet<>();
        set1.add("Hello");
        set1.add("World");

        CaseInsensitiveSet<String> set2 = new CaseInsensitiveSet<>();
        set2.add("HELLO");
        set2.add("WORLD");

        // These sets are equal (case-insensitive)
        assertTrue(set1.equals(set2), "Sets should be equal case-insensitively");
        // So their hashCodes MUST be equal
        assertEquals(set1.hashCode(), set2.hashCode(),
                "Equal sets with different-case strings must have same hashCode");
    }

    @Test
    void testHashCodeConsistency() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("Foo");
        set.add("Bar");

        int h1 = set.hashCode();
        int h2 = set.hashCode();
        assertEquals(h1, h2, "hashCode must be consistent across calls");
    }

    @Test
    void testHashCodeWithNonStringElements() {
        CaseInsensitiveSet<Object> set1 = new CaseInsensitiveSet<>();
        set1.add("Hello");
        set1.add(42);

        CaseInsensitiveSet<Object> set2 = new CaseInsensitiveSet<>();
        set2.add("HELLO");
        set2.add(42);

        assertTrue(set1.equals(set2));
        assertEquals(set1.hashCode(), set2.hashCode(),
                "Mixed-type sets that are equal must have same hashCode");
    }

    @Test
    void testHashCodeEmptySet() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        assertEquals(0, set.hashCode(), "Empty set hashCode should be 0");
    }

    // --- Bug 2: retainAll() should be case-insensitive ---

    @Test
    void testRetainAllWithStandardList() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("Hello");
        set.add("World");
        set.add("Foo");

        // retainAll with a standard list containing different-case strings
        List<String> retain = Arrays.asList("HELLO", "FOO");
        boolean changed = set.retainAll(retain);

        assertTrue(changed, "Set should have changed");
        assertEquals(2, set.size());
        assertTrue(set.contains("Hello"), "Should retain Hello (matches HELLO case-insensitively)");
        assertTrue(set.contains("Foo"), "Should retain Foo (matches FOO case-insensitively)");
        assertFalse(set.contains("World"), "World should have been removed");
    }

    @Test
    void testRetainAllWithStandardHashSet() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("Alpha");
        set.add("Beta");
        set.add("Gamma");

        Set<String> retain = new HashSet<>(Arrays.asList("ALPHA", "GAMMA"));
        boolean changed = set.retainAll(retain);

        assertTrue(changed);
        assertEquals(2, set.size());
        assertTrue(set.contains("Alpha"));
        assertFalse(set.contains("Beta"));
        assertTrue(set.contains("Gamma"));
    }

    @Test
    void testRetainAllWithCaseInsensitiveSet() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("Hello");
        set.add("World");

        CaseInsensitiveSet<String> retain = new CaseInsensitiveSet<>();
        retain.add("HELLO");

        boolean changed = set.retainAll(retain);

        assertTrue(changed);
        assertEquals(1, set.size());
        assertTrue(set.contains("Hello"));
    }

    @Test
    void testRetainAllNoChange() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("Hello");
        set.add("World");

        List<String> retain = Arrays.asList("HELLO", "WORLD");
        boolean changed = set.retainAll(retain);

        assertFalse(changed, "All elements match case-insensitively, no change");
        assertEquals(2, set.size());
    }

    @Test
    void testRetainAllWithNonStringElements() {
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
        set.add("Hello");
        set.add(42);
        set.add("World");

        List<Object> retain = Arrays.asList("HELLO", 42);
        boolean changed = set.retainAll(retain);

        assertTrue(changed);
        assertEquals(2, set.size());
        assertTrue(set.contains("Hello"));
        assertTrue(set.contains(42));
        assertFalse(set.contains("World"));
    }

    @Test
    void testRetainAllEmpty() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("Hello");

        boolean changed = set.retainAll(Arrays.asList());

        assertTrue(changed);
        assertEquals(0, set.size());
    }
}
