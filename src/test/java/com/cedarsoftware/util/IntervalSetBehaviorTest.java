package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.NavigableSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
