package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactLinkedSetTest {

    @Test
    void defaultConstructorMaintainsOrder() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.add("first");
        set.add("second");
        set.add("third");
        set.add("FIRST");

        assertEquals(Arrays.asList("first", "second", "third", "FIRST"), new ArrayList<>(set));
    }

    @Test
    void collectionConstructorHonorsOrder() {
        List<String> src = Arrays.asList("a", "b", "A", "c");
        CompactLinkedSet<String> set = new CompactLinkedSet<>(src);

        assertEquals(Arrays.asList("a", "b", "A", "c"), new ArrayList<>(set));
        assertTrue(set.contains("A"));
    }
}
