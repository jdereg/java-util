package com.cedarsoftware.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * On JDK 21+, ConcurrentList.reversed() works however the list is held. JDK 21 gave List and Deque each a reversed();
 * ConcurrentList is both, and java-util is compiled for JDK 8, so through SequencedCollection the JVM found two and
 * threw AbstractMethodError. The packaged jar now carries a JDK 21 version of ConcurrentList's superclass
 * (META-INF/versions/21) that settles it. Run by failsafe against the jar, as a multi-release version applies only to
 * classes loaded from a jar; through reflection, as the tests compile for JDK 8.
 */
class ConcurrentListSequencedIT {

    private static Object reversed(Object list, String throughInterface) throws Exception {
        Method method = Class.forName(throughInterface).getMethod("reversed");
        try {
            return method.invoke(list);
        } catch (InvocationTargetException e) {
            throw new AssertionError("reversed() through " + throughInterface + ": " + e.getCause(), e.getCause());
        }
    }

    private static boolean jdk21() {
        try {
            Class.forName("java.util.SequencedCollection");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Test
    void reversedWorksThroughSequencedCollectionListAndDeque() throws Exception {
        if (!jdk21()) {
            return;
        }
        ConcurrentList<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        for (String through : new String[] {"java.util.SequencedCollection", "java.util.List", "java.util.Deque"}) {
            assertEquals("[3, 2, 1]", String.valueOf(reversed(list, through)), through);
        }
        // a live view, as the JDK's own: a change to the list shows in it
        @SuppressWarnings("unchecked")
        List<Integer> view = (List<Integer>) reversed(list, "java.util.SequencedCollection");
        list.add(4);
        assertEquals(Arrays.asList(4, 3, 2, 1), view);
        @SuppressWarnings("unchecked")
        Deque<Integer> deque = (Deque<Integer>) reversed(list, "java.util.Deque");
        assertEquals(4, (int) deque.peekFirst());
    }

    /** Below JDK 21 the superclass adds nothing: no reversed() is declared on it. */
    @Test
    void belowJdk21TheSuperclassIsEmpty() {
        if (jdk21()) {
            return;
        }
        long declared = 0;
        for (Method method : ConcurrentList.class.getSuperclass().getDeclaredMethods()) {
            if (!method.isSynthetic()) {        // a coverage agent adds its own
                declared++;
            }
        }
        assertEquals(0, declared);
    }
}
