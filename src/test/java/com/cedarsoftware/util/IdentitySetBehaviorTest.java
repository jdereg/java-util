package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    /**
     * removeAll keeps what the argument does not contain by the argument's own rule, as the JDK's identity set
     * does, whatever the sizes. The inherited AbstractSet.removeAll asked the argument when this set was no
     * larger, and removed the argument's elements by identity otherwise: a string equal to (not the same as) an
     * element was removed from a one-element set but not from a two-element one.
     */
    @Test
    void removeAllAnswersTheSameWhateverTheSizes() {
        String s1 = new String("k");
        for (int extra = 0; extra < 3; extra++) {
            IdentitySet<Object> set = new IdentitySet<>();
            Set<Object> jdk = Collections.newSetFromMap(new IdentityHashMap<>());
            set.add(s1);
            jdk.add(s1);
            for (int i = 0; i < extra; i++) {
                Object o = new Object();
                set.add(o);
                jdk.add(o);
            }
            List<Object> equalStranger = Collections.singletonList(new String("k"));
            assertEquals(jdk.removeAll(equalStranger), set.removeAll(equalStranger), "extra " + extra);
            assertEquals(jdk.contains(s1), set.contains(s1), "extra " + extra);
            assertEquals(jdk.size(), set.size(), "extra " + extra);
        }
    }

    /**
     * hashCode is the sum of the elements' identity hash codes, as the JDK's identity set's is, so equal sets of
     * either kind hash alike; it called each element's own hashCode(), which also overflowed the stack for a set
     * that contains itself.
     */
    @Test
    void hashCodeIsTheJdkIdentitySetsHashCode() {
        IdentitySet<Object> set = new IdentitySet<>();
        Set<Object> jdk = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object o : Arrays.asList("a", 7, new Object(), new ArrayList<>())) {
            set.add(o);
            jdk.add(o);
        }
        assertEquals(jdk.hashCode(), set.hashCode());
        assertEquals(jdk, set);
        assertEquals(set, jdk);

        IdentitySet<Object> self = new IdentitySet<>();
        self.add(self);
        assertEquals(System.identityHashCode(self), self.hashCode());
    }

    /**
     * Adding an element already present does not grow the table. At the resize threshold, add() doubled the table
     * before finding the element was there: 16 slots became 32 for an add that returned false.
     */
    @Test
    void addingAnElementAlreadyPresentDoesNotGrowTheTable() throws Exception {
        IdentitySet<Object> set = new IdentitySet<>();
        List<Object> twelve = new ArrayList<>();
        for (int i = 0; i < 12; i++) {                // the default 16 slots at load factor 0.75: the threshold
            Object o = new Object();
            twelve.add(o);
            set.add(o);
        }
        java.lang.reflect.Field elements = IdentitySet.class.getDeclaredField("elements");
        elements.setAccessible(true);
        int before = ((Object[]) elements.get(set)).length;
        assertFalse(set.add(twelve.get(5)));
        assertEquals(before, ((Object[]) elements.get(set)).length);
        assertEquals(12, set.size());
        assertTrue(set.add(new Object()));            // a new element still grows it
        assertEquals(before * 2, ((Object[]) elements.get(set)).length);
    }
}
