package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    /**
     * Changing the set while iterating it is detected, as the JDK's identity set (an IdentityHashMap's key set)
     * detects it. The iterator went on regardless: a remove after another removal left size() disagreeing with
     * the elements reachable, after which an emptying remove lost the rest; and a remove after an add had grown
     * the table took a different element than next() had returned.
     */
    @Test
    void changingTheSetWhileIteratingIsDetected() {
        Object a = new Object();
        Object b = new Object();
        Object c = new Object();
        IdentitySet<Object> set = new IdentitySet<>(Arrays.asList(a, b, c));
        Iterator<Object> it = set.iterator();
        Object first = it.next();
        set.remove(first == a ? b : a);
        assertThrows(ConcurrentModificationException.class, it::remove);
        assertThrows(ConcurrentModificationException.class, it::next);
        assertEquals(2, set.size());

        IdentitySet<Object> growing = new IdentitySet<>();
        for (int i = 0; i < 12; i++) {
            growing.add(new Object());
        }
        Iterator<Object> it2 = growing.iterator();
        Object seen = it2.next();
        for (int i = 0; i < 20; i++) {
            growing.add(new Object());               // grows the table
        }
        assertThrows(ConcurrentModificationException.class, it2::remove);
        assertTrue(growing.contains(seen));
        assertEquals(32, growing.size());

        IdentitySet<Object> own = new IdentitySet<>(Arrays.asList(a, b, c));
        Iterator<Object> it3 = own.iterator();
        it3.next();
        it3.remove();                                // the iterator's own changes are fine
        it3.next();
        it3.remove();
        assertEquals(1, own.size());
        assertTrue(it3.hasNext());
        it3.next();
        assertFalse(it3.hasNext());
    }
}
