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
 * newInstance() gives the values to the most specific parameters first, so a broad parameter (Object) takes what the
 * others leave. Parameters were filled in declaration order, each taking the nearest value it could hold, so an
 * Object parameter declared first took a value only a later parameter could hold, and that parameter got a default:
 * (Object any, CharSequence text) given ["x", list] gave any="x" and text an empty StringBuilder, dropping the list.
 * With the class hierarchy following the JLS, (Object any, Collection coll) given [set, sb] went the same way: the
 * set and the StringBuilder are both 2 steps from Object, so any took the set.
 */
class ClassUtilitiesParameterSpecificityTest {

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
}
