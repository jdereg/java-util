package com.cedarsoftware.util;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The class hierarchy (getClassHierarchyInfo, and through it computeInheritanceDistance, findClosest,
 * findLowestCommonSupertypes, the security checks and the Converter) follows the Java Language Specification's direct
 * supertypes, JLS 4.10.2 and 4.10.3. It followed getSuperclass() and getInterfaces() only, so an interface never
 * reached Object and an array never reached the arrays of its component's supertypes. The oracle here restates the
 * specification, and Java's own isAssignableFrom() checks it.
 */
class ClassUtilitiesJlsSupertypesTest {

    interface Deep {
    }

    interface Middle extends Deep {
    }

    interface Root {
    }

    interface TwoBranches extends Middle, Root {
    }

    static class Impl implements TwoBranches, RandomAccess {
    }

    private static final Class<?>[] TYPES = {
            Object.class, String.class, CharSequence.class, Comparable.class, Integer.class, Number.class, Long.class,
            ArrayList.class, LinkedList.class, List.class, Collection.class, Iterable.class, RandomAccess.class,
            HashMap.class, AbstractMap.class, Map.class, Queue.class, ArrayDeque.class, Runnable.class, Thread.class,
            Cloneable.class, Serializable.class, Deprecated.class, TimeUnit.class, Deep.class, Middle.class,
            Root.class, TwoBranches.class, Impl.class,
            Object[].class, String[].class, CharSequence[].class, Comparable[].class, Runnable[].class, List[].class,
            ArrayList[].class, Integer[].class, Number[].class, Cloneable[].class, Serializable[].class,
            TwoBranches[].class, Impl[].class, int[].class, long[].class, int[][].class, String[][].class,
            Object[][].class, Serializable[][].class, Comparable[][].class};

    /** JLS 4.10.2 (classes and interfaces) and 4.10.3 (arrays), restated: the direct supertypes of a type. */
    private static Set<Class<?>> jlsDirectSupertypes(Class<?> type) {
        Set<Class<?>> direct = new HashSet<>();
        if (type.isArray()) {
            Class<?> s = type.getComponentType();
            if (s.isPrimitive() || s == Object.class) {
                // "Object >1 Object[]", "Cloneable >1 Object[]", "java.io.Serializable >1 Object[]"; the same for P[]
                direct.add(Object.class);
                direct.add(Cloneable.class);
                direct.add(Serializable.class);
            } else {
                // "If S and T are both reference types, then S[] >1 T[] iff S >1 T."
                for (Class<?> t : jlsDirectSupertypes(s)) {
                    direct.add(Array.newInstance(t, 0).getClass());
                }
            }
            return direct;
        }
        if (type.getSuperclass() != null) {
            direct.add(type.getSuperclass());                           // the direct superclass
        }
        direct.addAll(Arrays.asList(type.getInterfaces()));             // the direct superinterfaces
        if (type.isInterface() && type.getInterfaces().length == 0) {
            direct.add(Object.class);                                   // "if C is an interface with no direct superinterfaces"
        }
        return direct;
    }

    /** The fewest steps from a type to each of its supertypes, over the direct supertypes above. */
    private static Map<Class<?>, Integer> jlsDistances(Class<?> type) {
        Map<Class<?>, Integer> distances = new HashMap<>();
        distances.put(type, 0);
        List<Class<?>> frontier = new ArrayList<>(Arrays.asList(type));
        for (int step = 1; !frontier.isEmpty(); step++) {
            List<Class<?>> next = new ArrayList<>();
            for (Class<?> t : frontier) {
                for (Class<?> u : jlsDirectSupertypes(t)) {
                    if (!distances.containsKey(u)) {
                        distances.put(u, step);
                        next.add(u);
                    }
                }
            }
            frontier = next;
        }
        return distances;
    }

    /** The hierarchy's distances, less the primitive a wrapper is one step from (java-util's, not the JLS's). */
    private static Map<Class<?>, Integer> hierarchy(Class<?> type) {
        Map<Class<?>, Integer> distances = new HashMap<>(ClassUtilities.getClassHierarchyInfo(type).getDistanceMap());
        distances.keySet().removeIf(Class::isPrimitive);
        return distances;
    }

    @Test
    void theHierarchyIsTheSpecificationsDirectSupertypesAtTheFewestSteps() {
        for (Class<?> type : TYPES) {
            assertEquals(jlsDistances(type), hierarchy(type), type.getName());
        }
    }

    @Test
    void aTypeIsInItsHierarchyExactlyWhenJavaWouldAssignToIt() {
        for (Class<?> type : TYPES) {
            Map<Class<?>, Integer> distances = hierarchy(type);
            for (Class<?> supertype : distances.keySet()) {
                assertTrue(supertype.isAssignableFrom(type), type.getName() + " -> " + supertype.getName());
            }
            for (Class<?> candidate : TYPES) {
                assertEquals(candidate.isAssignableFrom(type), distances.containsKey(candidate),
                        type.getName() + " -> " + candidate.getName());
            }
        }
    }

    /** Distances worked out by hand from the specification. */
    @Test
    void distancesFromTheSpecification() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(String[].class, Object[].class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(String[].class, CharSequence[].class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(String[].class, Object.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(String[].class, Cloneable.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Object[].class, Object.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int[].class, Object.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int[][].class, Object[].class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(int[][].class, Object.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Runnable[].class, Object[].class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Map.class, Object.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Collection.class, Object.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(ArrayList.class, Object.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(TwoBranches.class, Object.class));
        assertEquals(3, ClassUtilities.computeInheritanceDistance(Impl.class, Deep.class));     // TwoBranches, Middle
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Impl[].class, Root[].class)); // TwoBranches[]
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, Map.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Map.class, Collection.class));
    }

    @Test
    void arraysShareTheArraysOfTheirComponentsCommonSupertypes() {
        Set<Class<?>> common = ClassUtilities.findLowestCommonSupertypes(String[].class, Integer[].class);
        assertTrue(common.contains(Comparable[].class), common.toString());
        assertTrue(common.contains(Serializable[].class), common.toString());
        assertFalse(common.contains(Object[].class), "an ancestor of Comparable[]: " + common);
    }
}
