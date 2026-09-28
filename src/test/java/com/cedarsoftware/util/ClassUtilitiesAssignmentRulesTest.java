package com.cedarsoftware.util;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * computeInheritanceDistance() answers for primitives and wrappers as Java assigns them (JLS 5.2): a distance exists
 * exactly when a value of the source type can be assigned to the destination type. Wrappers were answered as their
 * primitives widen, so Integer to Long was 1 and int to Long was 1, though Java assigns neither; a wrapper takes only
 * its own primitive, and no wrapper is assigned to another. Unboxing and then widening to a primitive (Integer to
 * long) stays, as does boxing to a supertype of the wrapper (int to Number).
 */
class ClassUtilitiesAssignmentRulesTest {

    private static final List<Class<?>> PRIMITIVES = Arrays.asList(boolean.class, byte.class, short.class, char.class,
            int.class, long.class, float.class, double.class);
    private static final List<Class<?>> WRAPPERS = Arrays.asList(Boolean.class, Byte.class, Short.class,
            Character.class, Integer.class, Long.class, Float.class, Double.class);

    /** JLS 5.1.2, restated: the widening primitive conversions. */
    private static final Map<Class<?>, List<Class<?>>> WIDENS = new HashMap<>();

    static {
        WIDENS.put(byte.class, Arrays.asList(short.class, int.class, long.class, float.class, double.class));
        WIDENS.put(short.class, Arrays.asList(int.class, long.class, float.class, double.class));
        WIDENS.put(char.class, Arrays.asList(int.class, long.class, float.class, double.class));
        WIDENS.put(int.class, Arrays.asList(long.class, float.class, double.class));
        WIDENS.put(long.class, Arrays.asList(float.class, double.class));
        WIDENS.put(float.class, Arrays.asList(double.class));
    }

    private static Class<?> box(Class<?> primitive) {
        return WRAPPERS.get(PRIMITIVES.indexOf(primitive));
    }

    private static Class<?> unbox(Class<?> type) {
        int i = WRAPPERS.indexOf(type);
        return i < 0 ? null : PRIMITIVES.get(i);
    }

    private static boolean widens(Class<?> from, Class<?> to) {
        return from == to || WIDENS.getOrDefault(from, Arrays.asList()).contains(to);
    }

    /** JLS 5.2, restated: may a value of type s be assigned to a variable of type d? */
    private static boolean assignable(Class<?> s, Class<?> d) {
        if (s.isPrimitive() && d.isPrimitive()) {
            return widens(s, d);                                // identity or widening primitive
        }
        if (d.isPrimitive()) {
            Class<?> unboxed = unbox(s);                        // unboxing, then widening primitive
            return unboxed != null && widens(unboxed, d);
        }
        if (s.isPrimitive()) {
            return d.isAssignableFrom(box(s));                  // boxing, then widening reference
        }
        return d.isAssignableFrom(s);                           // widening reference
    }

    @Test
    void aDistanceExistsExactlyWhenJavaWouldAssign() {
        List<Class<?>> types = new java.util.ArrayList<>(PRIMITIVES);
        types.addAll(WRAPPERS);
        types.addAll(Arrays.asList(Number.class, Object.class, Comparable.class, Serializable.class,
                CharSequence.class, String.class));
        for (Class<?> s : types) {
            for (Class<?> d : types) {
                int distance = ClassUtilities.computeInheritanceDistance(s, d);
                assertEquals(assignable(s, d), distance >= 0,
                        s.getSimpleName() + " -> " + d.getSimpleName() + " (" + distance + ")");
            }
        }
    }

    @Test
    void theDistancesThatRemain() {
        assertEquals(0, ClassUtilities.computeInheritanceDistance(int.class, Integer.class));
        assertEquals(0, ClassUtilities.computeInheritanceDistance(Integer.class, int.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int.class, long.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer.class, long.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(Byte.class, int.class));
        assertEquals(5, ClassUtilities.computeInheritanceDistance(byte.class, double.class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int.class, Number.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer.class, Long.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int.class, Long.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(long.class, Integer.class));
    }
}
