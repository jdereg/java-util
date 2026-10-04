package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * IdentitySet beyond the basic Set contract.
 */
class IdentitySetBehaviorTest {

    /**
     * Removing the last element cleared the whole table, which stays as large as the set ever was: once a set had
     * held a million elements, every add-then-remove cycle that emptied it cost a pass over two million slots.
     * Traverser's queue empties at every step of a chain, so a graph that was once wide made each later step pay.
     */
    @Test
    void emptyingASetThatWasLargeIsCheap() {
        IdentitySet<Object> set = new IdentitySet<>();
        List<Object> many = new ArrayList<>();
        for (int i = 0; i < 1_000_000; i++) {
            Object o = new Object();
            many.add(o);
            set.add(o);
        }
        for (Object o : many) {
            set.remove(o);
        }
        assertTrue(set.isEmpty());
        Object one = new Object();
        long start = System.nanoTime();
        for (int i = 0; i < 2000; i++) {
            set.add(one);
            set.remove(one);
        }
        long millis = (System.nanoTime() - start) / 1_000_000;
        assertTrue(millis < 200, "2,000 add/remove cycles took " + millis + " ms");
        assertTrue(set.isEmpty());
        assertFalse(set.contains(one));
        set.add(one);
        assertTrue(set.contains(one));
        assertEquals(1, set.size());
    }
}
