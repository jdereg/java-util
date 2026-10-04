package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.NavigableSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IntervalSet edge behavior found by review.
 */
class IntervalSetBehaviorTest {

    /**
     * The key sets are read-only views. They were the skip list's own writable views, so pollFirst() or remove()
     * on them deleted intervals without the set's lock, beside its merges and splits.
     */
    @Test
    void keySetsAreReadOnlyViews() {
        IntervalSet<Integer> set = new IntervalSet<>();
        set.add(10, 20);
        set.add(30, 40);
        for (NavigableSet<Integer> keys : Arrays.asList(set.keySet(), set.descendingKeySet())) {
            assertThrows(UnsupportedOperationException.class, keys::pollFirst);
            assertThrows(UnsupportedOperationException.class, () -> keys.remove(10));
            assertThrows(UnsupportedOperationException.class, keys::clear);
            assertThrows(UnsupportedOperationException.class, () -> keys.headSet(30).clear());
        }
        assertEquals(2, set.size());
        set.add(50, 60);
        assertEquals(3, set.keySet().size(), "still a view: it shows later writes");
        assertEquals(Integer.valueOf(50), set.descendingKeySet().first());
    }

    /**
     * removeExact matches the end as the set orders values (compareTo), as it already matched the start: 2.00 is
     * the end 2.0. It compared the end with equals(), so the scale had to match too.
     */
    @Test
    void removeExactMatchesTheEndAsTheSetOrdersValues() {
        IntervalSet<BigDecimal> set = new IntervalSet<>();
        set.add(new BigDecimal("1.0"), new BigDecimal("2.0"));
        assertFalse(set.removeExact(new BigDecimal("1.0"), new BigDecimal("2.01")));
        assertTrue(set.removeExact(new BigDecimal("1.00"), new BigDecimal("2.00")));
        assertTrue(set.isEmpty());
    }
}
