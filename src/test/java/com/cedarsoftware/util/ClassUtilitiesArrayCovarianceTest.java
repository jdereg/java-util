package com.cedarsoftware.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * computeInheritanceDistance() and constructor matching know the two assignments Java allows beyond superclasses and
 * interfaces: an array of references to an array of its component's supertype, and an interface to Object (JLS
 * 4.10.2, 4.10.3). Both were -1, so newInstance(Holder(Object[]), [String[]]) did not pass the array: it went to
 * conversion and the constructor received an empty Object[].
 */
class ClassUtilitiesArrayCovarianceTest {

    public static class Holder {
        final Object[] items;

        public Holder(Object[] items) {
            this.items = items;
        }
    }

    public static class Texts {
        final CharSequence[] texts;

        public Texts(CharSequence[] texts) {
            this.texts = texts;
        }
    }

    public static class Named {
        final String name;
        final Object[] items;

        public Named(String name, Object[] items) {
            this.name = name;
            this.items = items;
        }
    }

    @Test
    void anArrayOfASubtypeFillsAnArrayParameter() {
        String[] strings = {"a", "b"};
        for (int call = 1; call <= 3; call++) {       // the first call matches; the others replay its plan
            Holder holder = (Holder) ClassUtilities.newInstance(Holder.class, Collections.singletonList(strings));
            assertSame(strings, holder.items, "call " + call);

            Texts texts = (Texts) ClassUtilities.newInstance(Texts.class, Collections.singletonList(strings));
            assertSame(strings, texts.texts, "call " + call);

            Named named = (Named) ClassUtilities.newInstance(Named.class, Arrays.asList(strings, "n"));
            assertEquals("n", named.name, "call " + call);
            assertSame(strings, named.items, "call " + call);
        }
    }

    @Test
    void arrayDistances() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(String[].class, Object[].class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(String[][].class, Object[][].class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(String[][].class, Object[].class),
                "String[][] -> Object[][] -> Object[] (JLS 4.10.3)");
        assertEquals(1, ClassUtilities.computeInheritanceDistance(ArrayList[].class, List[].class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Integer[].class, Number[].class));
        assertEquals(1, ClassUtilities.computeInheritanceDistance(int[][].class, Object[].class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer[].class, Long[].class),
                "an Integer[] is not a Long[], though an int widens to a long");
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Integer[].class, int[].class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int[].class, Object[].class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(int[].class, long[].class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object[].class, String[].class));
    }

    interface Deep {
    }

    interface Middle extends Deep {
    }

    interface Root {
    }

    /** Object is reached through Root (no superinterfaces) in 2 steps, not above Deep in 3. */
    interface TwoBranches extends Middle, Root {
    }

    @Test
    void anInterfaceReachesObjectThroughItsNearestRootInterface() {
        assertEquals(1, ClassUtilities.computeInheritanceDistance(Runnable.class, Object.class));
        int toObject = ClassUtilities.computeInheritanceDistance(List.class, Object.class);
        assertEquals(ClassUtilities.computeInheritanceDistance(List.class, Iterable.class) + 1, toObject);
        assertEquals(2, ClassUtilities.computeInheritanceDistance(TwoBranches.class, Object.class));
        assertEquals(2, ClassUtilities.computeInheritanceDistance(TwoBranches.class, Deep.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(List.class, String.class));
        assertEquals(-1, ClassUtilities.computeInheritanceDistance(Object.class, List.class));
    }

    /**
     * Java's own rule is the oracle: a distance exists exactly when the type is assignable, and is 0 only to itself.
     */
    @Test
    void aDistanceExistsExactlyWhenJavaWouldAssign() {
        Class<?>[] types = {Object.class, String.class, CharSequence.class, Comparable.class, Serializable.class,
                Cloneable.class, Iterable.class, Collection.class, List.class, ArrayList.class, Runnable.class,
                Thread.class, Integer.class, Number.class, Object[].class, String[].class, CharSequence[].class,
                Comparable[].class, Serializable[].class, Object[][].class, String[][].class, List[].class,
                ArrayList[].class, Integer[].class, Number[].class, int[].class, long[].class, int[][].class};
        for (Class<?> source : types) {
            for (Class<?> destination : types) {
                int distance = ClassUtilities.computeInheritanceDistance(source, destination);
                String pair = source.getSimpleName() + " -> " + destination.getSimpleName();
                assertEquals(destination.isAssignableFrom(source), distance >= 0, pair + " (" + distance + ")");
                assertEquals(source == destination, distance == 0, pair);
            }
        }
    }
}
