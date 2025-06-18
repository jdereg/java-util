package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for CompactMap.CompactMapComparator.
 */
public class CompactMapComparatorTest {

    @Test
    public void testToString() {
        CompactMap.CompactMapComparator comparator = new CompactMap.CompactMapComparator(true, true);
        String expected = "CompactMapComparator{caseInsensitive=true, reverse=true}";
        assertEquals(expected, comparator.toString());
    }
}
