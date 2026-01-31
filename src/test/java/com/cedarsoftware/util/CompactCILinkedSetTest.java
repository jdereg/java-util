package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactCILinkedSetTest {

    @Test
    void defaultConstructorMaintainsOrder() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.add("A");
        set.add("B");
        set.add("C");
        set.add("a"); // duplicate in different case

        assertEquals(Arrays.asList("A", "B", "C"), new ArrayList<>(set));
    }

    @Test
    void collectionConstructorHonorsOrder() {
        List<String> src = Arrays.asList("x", "y", "X", "z");
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>(src);

        assertEquals(Arrays.asList("x", "y", "z"), new ArrayList<>(set));
        assertTrue(set.contains("X"));
    }
}
