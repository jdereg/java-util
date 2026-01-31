package com.cedarsoftware.util;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Additional tests for ConcurrentNavigableSetNullSafe covering
 * constructors and comparator retrieval.
 */
class ConcurrentNavigableSetNullSafeExtraTest {

    @Test
    void testDefaultComparatorIsNull() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        assertNull(set.comparator());
    }

    @Test
    void testCustomComparatorRetention() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(reverse);
        assertSame(reverse, set.comparator());

        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals("c", set.first());
    }
}
