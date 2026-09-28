package com.cedarsoftware.util;

import java.io.Externalizable;
import java.io.Serializable;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * findLowestCommonSupertypes() never returns a type together with one of its own ancestors. Candidates were taken in
 * order of their summed distance from the two classes, which is not an order by inheritance, so an ancestor could be
 * taken first: ArrayList and LinkedList gave [List, AbstractList], and findLowestCommonSupertype() returned List.
 */
class ClassUtilitiesLowestCommonSupertypesTest {

    interface I1 {
    }

    interface I2 extends I1 {
    }

    static class P implements I2 {
    }

    /** I1 directly (distance 1) and through P and I2 (distance 3). */
    static class A extends P implements I1 {
    }

    static class B extends P implements I1 {
    }

    private static final Set<Class<?>> SKIP = new HashSet<>(Arrays.asList(
            Object.class, Serializable.class, Externalizable.class, Cloneable.class));

    @Test
    void siblingListsShareAbstractListOnly() {
        assertEquals(Collections.singleton(AbstractList.class),
                ClassUtilities.findLowestCommonSupertypes(ArrayList.class, LinkedList.class));
        assertSame(AbstractList.class, ClassUtilities.findLowestCommonSupertype(ArrayList.class, LinkedList.class));
    }

    @Test
    void anInterfaceReachedDirectlyIsDroppedForTheClassThatImplementsIt() {
        assertEquals(Collections.singleton(P.class), ClassUtilities.findLowestCommonSupertypes(A.class, B.class));
    }

    /** The oracle is Java's own subtype test: the common supertypes that no other common supertype descends from. */
    @Test
    void theResultIsExactlyTheMostSpecificCommonSupertypes() {
        Class<?>[] types = {ArrayList.class, LinkedList.class, TreeSet.class, HashSet.class, LinkedHashSet.class,
                ArrayDeque.class, HashMap.class, TreeMap.class, ConcurrentHashMap.class, Integer.class, Double.class,
                Long.class, String.class, StringBuilder.class, StringBuffer.class, Thread.class, Runnable.class,
                A.class, B.class, P.class, IllegalStateException.class, UnsupportedOperationException.class};
        for (Class<?> a : types) {
            for (Class<?> b : types) {
                if (a == b) {
                    continue;
                }
                Set<Class<?>> actual = ClassUtilities.findLowestCommonSupertypes(a, b);
                assertEquals(mostSpecificCommon(a, b), actual, a.getSimpleName() + " & " + b.getSimpleName());
            }
        }
    }

    private static Set<Class<?>> mostSpecificCommon(Class<?> a, Class<?> b) {
        Set<Class<?>> common = new HashSet<>();
        for (Class<?> t : supertypes(a)) {
            if (t.isAssignableFrom(b) && !SKIP.contains(t)) {
                common.add(t);
            }
        }
        Set<Class<?>> lowest = new HashSet<>();
        for (Class<?> t : common) {
            boolean ancestor = false;
            for (Class<?> u : common) {
                ancestor |= u != t && t.isAssignableFrom(u);
            }
            if (!ancestor) {
                lowest.add(t);
            }
        }
        return lowest;
    }

    private static Set<Class<?>> supertypes(Class<?> c) {
        Set<Class<?>> all = new HashSet<>();
        List<Class<?>> todo = new ArrayList<>(Collections.singletonList(c));
        while (!todo.isEmpty()) {
            Class<?> t = todo.remove(todo.size() - 1);
            if (t != null && all.add(t)) {
                todo.add(t.getSuperclass());
                todo.addAll(Arrays.asList(t.getInterfaces()));
            }
        }
        return all;
    }
}
