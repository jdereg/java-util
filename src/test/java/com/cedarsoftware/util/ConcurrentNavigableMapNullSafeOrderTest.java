package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * ConcurrentNavigableMapNullSafe orders keys as its comparator() says: natural order when it returns null (for keys
 * whose classes differ but are related), and the given comparator's own placement of null.
 */
class ConcurrentNavigableMapNullSafeOrderTest {

    /** An enum whose constants A and C have bodies, so their classes are Op$1 and Op$2, not Op. */
    enum Op {
        A {
            @Override
            int apply(int x) {
                return x + 1;
            }
        },
        B,
        C {
            @Override
            int apply(int x) {
                return x * 2;
            }
        };

        int apply(int x) {
            return x;
        }
    }

    static class Base implements Comparable<Base> {
        final int rank;

        Base(int rank) {
            this.rank = rank;
        }

        @Override
        public int compareTo(Base o) {
            return Integer.compare(rank, o.rank);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Base && ((Base) o).rank == rank;
        }

        @Override
        public int hashCode() {
            return rank;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + rank;
        }
    }

    static class Sub extends Base {
        Sub(int rank) {
            super(rank);
        }
    }

    /**
     * Natural order compared keys only when their classes were identical, and otherwise ordered them by class
     * name: enum constants with bodies (A, C) sorted apart from B, and a TreeMap copy, trusting natural order,
     * then could not find B.
     */
    @Test
    void enumConstantsWithBodiesSortInDeclarationOrder() {
        ConcurrentNavigableMapNullSafe<Op, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(Op.C, "c");
        map.put(Op.B, "b");
        map.put(Op.A, "a");
        assertEquals(Arrays.asList(Op.A, Op.B, Op.C), new ArrayList<>(map.keySet()));
        assertEquals(Arrays.asList(Op.A), new ArrayList<>(map.headMap(Op.B).keySet()));
        assertEquals("b", new TreeMap<>(map).get(Op.B));
    }

    /** A subclass of a Comparable class sorts by compareTo among its superclass's instances. */
    @Test
    void subclassesOfAComparableClassSortByCompareTo() {
        ConcurrentNavigableMapNullSafe<Base, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(new Sub(3), "s3");
        map.put(new Base(2), "b2");
        map.put(new Sub(1), "s1");
        map.put(new Base(4), "b4");
        List<Integer> ranks = new ArrayList<>();
        for (Base b : map.keySet()) {
            ranks.add(b.rank);
        }
        assertEquals(Arrays.asList(1, 2, 3, 4), ranks);
        assertEquals("s3", map.get(new Base(3)));
    }

    /**
     * Keys of unrelated classes still sort without throwing, by class name, a plain Object among them (Object is a
     * superclass of String, but not a Comparable one).
     */
    @Test
    void unrelatedClassesStillSortByClassName() {
        ConcurrentNavigableMapNullSafe<Object, String> map = new ConcurrentNavigableMapNullSafe<>();
        Object plain = new Object();
        map.put("s", "string");
        map.put(plain, "object");
        map.put(7, "integer");
        map.put(7L, "long");
        assertEquals(Arrays.asList(7, 7L, plain, "s"), new ArrayList<>(map.keySet()));
        assertEquals("object", map.get(plain));
    }

    /**
     * A comparator that places null (Comparator.nullsFirst) decides where the null key goes; the map put null last
     * whatever its comparator() said, so the order disagreed with comparator() and with a TreeMap of it.
     */
    @Test
    void aComparatorThatPlacesNullDecidesWhereTheNullKeyGoes() {
        Comparator<String> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>(nullsFirst);
        TreeMap<String, Integer> reference = new TreeMap<>(nullsFirst);
        for (String k : Arrays.asList("b", null, "a")) {
            map.put(k, 1);
            reference.put(k, 1);
        }
        assertEquals(new ArrayList<>(reference.keySet()), new ArrayList<>(map.keySet()));
        assertNull(map.firstKey());
        assertEquals(new ArrayList<>(reference.descendingKeySet()), new ArrayList<>(map.descendingKeySet()));
        assertEquals(Integer.valueOf(1), new TreeMap<>(map).get(null));

        Comparator<String> rejectsNull = Comparator.naturalOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> fallback = new ConcurrentNavigableMapNullSafe<>(rejectsNull);
        for (String k : Arrays.asList("b", null, "a")) {
            fallback.put(k, 1);
        }
        assertEquals(Arrays.asList("a", "b", null), new ArrayList<>(fallback.keySet()));
    }
}
