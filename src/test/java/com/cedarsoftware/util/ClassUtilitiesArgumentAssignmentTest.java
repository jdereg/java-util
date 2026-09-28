package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * newInstance() places as many supplied values, as themselves, as any assignment of them to the parameters could,
 * and keeps the order they were supplied in as far as the types allow. Parameters were filled in declaration order,
 * each taking the value fewest inheritance steps away, so an Object parameter declared first took a value only a
 * later parameter could hold, and that parameter got a default: (Object any, CharSequence text) given ["x", list]
 * gave any="x" and text an empty StringBuilder, dropping the list. And the steps, not the order, decided between
 * values: (Object a, Object b) given [5, "x"] gave a="x", a String being fewer steps from Object than an Integer.
 */
class ClassUtilitiesArgumentAssignmentTest {

    public static class Pair {
        final Object any;
        final CharSequence text;

        public Pair(Object any, CharSequence text) {
            this.any = any;
            this.text = text;
        }
    }

    public static class Colls {
        final Object any;
        final Collection<?> coll;

        public Colls(Object any, Collection<?> coll) {
            this.any = any;
            this.coll = coll;
        }
    }

    public static class Mixed {
        final String name;
        final Object any;
        final int count;

        public Mixed(String name, Object any, int count) {
            this.name = name;
            this.any = any;
            this.count = count;
        }
    }

    public static class Two {
        final Object a;
        final Object b;

        public Two(Object a, Object b) {
            this.a = a;
            this.b = b;
        }
    }

    public static class Mix {
        final CharSequence text;
        final Object any;
        final Collection<?> coll;
        final Number number;

        public Mix(CharSequence text, Object any, Collection<?> coll, Number number) {
            this.text = text;
            this.any = any;
            this.coll = coll;
            this.number = number;
        }
    }

    public static class Serial {
        final java.io.Serializable serializable;
        final Collection<?> coll;
        final CharSequence text;

        public Serial(java.io.Serializable serializable, Collection<?> coll, CharSequence text) {
            this.serializable = serializable;
            this.coll = coll;
            this.text = text;
        }
    }

    public static class Chain {
        final Object any;
        final Collection<?> coll;
        final List<?> list;

        public Chain(Object any, Collection<?> coll, List<?> list) {
            this.any = any;
            this.coll = coll;
            this.list = list;
        }
    }

    private static List<List<Object>> orders(Object... values) {
        List<List<Object>> out = new ArrayList<>();
        permute(new ArrayList<>(Arrays.asList(values)), 0, out);
        return out;
    }

    private static void permute(List<Object> values, int k, List<List<Object>> out) {
        if (k == values.size()) {
            out.add(new ArrayList<>(values));
            return;
        }
        for (int i = k; i < values.size(); i++) {
            Collections.swap(values, k, i);
            permute(values, k + 1, out);
            Collections.swap(values, k, i);
        }
    }

    @Test
    void aBroadParameterTakesWhatTheSpecificOnesLeave() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2));
        for (List<Object> order : orders("x", list)) {
            Pair pair = (Pair) ClassUtilities.newInstance(Pair.class, order);
            assertSame(list, pair.any, order.toString());
            assertEquals("x", pair.text, order.toString());
        }

        Set<Integer> set = new HashSet<>(list);
        StringBuilder sb = new StringBuilder("sb");
        for (List<Object> order : orders(set, sb)) {
            Colls colls = (Colls) ClassUtilities.newInstance(Colls.class, order);
            assertSame(sb, colls.any, order.toString());
            assertSame(set, colls.coll, order.toString());
        }
    }

    /** int ranks as Integer, below Object: the Integer goes to the int, the list to the Object. */
    @Test
    void aPrimitiveParameterCountsAsItsWrapper() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2));
        for (List<Object> order : orders("n", list, 7)) {
            Mixed mixed = (Mixed) ClassUtilities.newInstance(Mixed.class, order);
            assertEquals("n", mixed.name, order.toString());
            assertSame(list, mixed.any, order.toString());
            assertEquals(7, mixed.count, order.toString());
        }
    }

    @Test
    void aChainOfParameterTypesIsFilledFromTheMostSpecific() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2));
        Set<Integer> set = new HashSet<>(list);
        for (List<Object> order : orders(list, set, "s")) {
            Chain chain = (Chain) ClassUtilities.newInstance(Chain.class, order);
            assertSame(list, chain.list, order.toString());
            assertSame(set, chain.coll, order.toString());
            assertEquals("s", chain.any, order.toString());
        }
    }

    @Test
    void valuesKeepTheOrderTheyWereSuppliedIn() {
        Two two = (Two) ClassUtilities.newInstance(Two.class, Arrays.asList(5, "x"));
        assertEquals(5, two.a);
        assertEquals("x", two.b);
        two = (Two) ClassUtilities.newInstance(Two.class, Arrays.asList("x", 5));
        assertEquals("x", two.a);
        assertEquals(5, two.b);
    }

    /**
     * The oracle: the most values any assignment could place as themselves (a maximum bipartite matching, value to a
     * parameter whose type it is an instance of), computed here; newInstance() must place that many, for every order
     * of the values.
     */
    @Test
    void asManyValuesArePlacedAsAnyAssignmentCould() {
        List<Object> pool = Arrays.asList("s", 5, 2.5d, new ArrayList<>(Arrays.asList(1)),
                new HashSet<>(Arrays.asList(2)), new StringBuilder("sb"), Collections.singletonMap("k", 1));
        Class<?>[] mixParams = {CharSequence.class, Object.class, Collection.class, Number.class};
        Class<?>[] serialParams = {java.io.Serializable.class, Collection.class, CharSequence.class};
        for (List<Object> values : subsets(pool, 4)) {
            for (List<Object> order : orders(values.toArray())) {
                Mix mix = (Mix) ClassUtilities.newInstance(Mix.class, order);
                assertEquals(maximumMatching(mixParams, order),
                        placed(order, mix.text, mix.any, mix.coll, mix.number), "Mix " + order);
            }
        }
        for (List<Object> values : subsets(pool, 3)) {
            for (List<Object> order : orders(values.toArray())) {
                Serial serial = (Serial) ClassUtilities.newInstance(Serial.class, order);
                assertEquals(maximumMatching(serialParams, order),
                        placed(order, serial.serializable, serial.coll, serial.text), "Serial " + order);
            }
        }
    }

    /** How many of the parameters received one of the supplied values as itself. */
    private static int placed(List<Object> supplied, Object... received) {
        int placed = 0;
        for (Object r : received) {
            for (Object v : supplied) {
                if (r == v) {
                    placed++;
                }
            }
        }
        return placed;
    }

    private static List<List<Object>> subsets(List<Object> pool, int size) {
        List<List<Object>> out = new ArrayList<>();
        subsets(pool, size, 0, new ArrayList<>(), out);
        return out;
    }

    private static void subsets(List<Object> pool, int size, int from, List<Object> current, List<List<Object>> out) {
        if (current.size() == size) {
            out.add(new ArrayList<>(current));
            return;
        }
        for (int i = from; i < pool.size(); i++) {
            current.add(pool.get(i));
            subsets(pool, size, i + 1, current, out);
            current.remove(current.size() - 1);
        }
    }

    private static int maximumMatching(Class<?>[] params, List<Object> values) {
        int[] owner = new int[params.length];
        Arrays.fill(owner, -1);
        int size = 0;
        for (int v = 0; v < values.size(); v++) {
            if (augment(v, params, values, owner, new boolean[params.length])) {
                size++;
            }
        }
        return size;
    }

    private static boolean augment(int v, Class<?>[] params, List<Object> values, int[] owner, boolean[] seen) {
        for (int p = 0; p < params.length; p++) {
            if (seen[p] || !params[p].isInstance(values.get(v))) {
                continue;
            }
            seen[p] = true;
            if (owner[p] < 0 || augment(owner[p], params, values, owner, seen)) {
                owner[p] = v;
                return true;
            }
        }
        return false;
    }
}
