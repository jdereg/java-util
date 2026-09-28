package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * newInstance() chooses among overloaded constructors as Java does (JLS 15.12.2) when one takes the supplied values as
 * they are: as many parameters as values, each value an instance of its parameter's type (a wrapper for its
 * primitive); no unboxing when a constructor needs none; then the most specific. It took the first constructor that
 * worked in its own order (public first, more parameters first, String parameters first), converting or filling in
 * where it had to: N(String) for 42L, C(String) for a Date, P(int, int, int) for [1, 2], S(String) for a
 * StringBuilder and for an Integer, and (String, Double) for [HashMap, ArrayList] where (Collection, Map) was there.
 * When no constructor takes the values as they are, the constructors are tried in that order, as before.
 */
class ClassUtilitiesOverloadChoiceTest {

    interface Chosen {
        String chosen();
    }

    public static class N implements Chosen {
        final String chosen;

        public N(String s) {
            chosen = "String";
        }

        public N(long l) {
            chosen = "long";
        }

        public String chosen() {
            return chosen;
        }
    }

    public static class C implements Chosen {
        final String chosen;

        public C(String s) {
            chosen = "String";
        }

        public C(Date d) {
            chosen = "Date";
        }

        public String chosen() {
            return chosen;
        }
    }

    public static class P implements Chosen {
        final String chosen;

        public P(int x, int y) {
            chosen = "int,int";
        }

        public P(int x, int y, int z) {
            chosen = "int,int,int";
        }

        public String chosen() {
            return chosen;
        }
    }

    public static class S implements Chosen {
        final String chosen;

        public S(Object o) {
            chosen = "Object";
        }

        public S(CharSequence c) {
            chosen = "CharSequence";
        }

        public S(String s) {
            chosen = "String";
        }

        public String chosen() {
            return chosen;
        }
    }

    public static class B implements Chosen {
        final String chosen;

        public B(long l) {
            chosen = "long";
        }

        public B(Long l) {
            chosen = "Long";
        }

        public String chosen() {
            return chosen;
        }
    }

    public static class Pairs implements Chosen {
        final String chosen;

        public Pairs(String s, Double d) {
            chosen = "String,Double";
        }

        public Pairs(Collection<?> c, Map<?, ?> m) {
            chosen = "Collection,Map";
        }

        public Pairs(Object a, Object b) {
            chosen = "Object,Object";
        }

        public String chosen() {
            return chosen;
        }
    }

    private static String chosen(Class<?> c, Object... values) {
        return ((Chosen) ClassUtilities.newInstance(c, Arrays.asList(values))).chosen();
    }

    @Test
    void theConstructorThatTakesTheValuesAsTheyAreIsChosen() {
        assertEquals("long", chosen(N.class, 42L));
        assertEquals("String", chosen(N.class, "x"));
        assertEquals("Date", chosen(C.class, new Date(1000)));
        assertEquals("int,int", chosen(P.class, 1, 2));
        assertEquals("int,int,int", chosen(P.class, 1, 2, 3));
    }

    @Test
    void theMostSpecificIsChosen() {
        assertEquals("CharSequence", chosen(S.class, new StringBuilder("sb")));
        assertEquals("String", chosen(S.class, "s"));
        assertEquals("Object", chosen(S.class, 5));
        assertEquals("Collection,Map", chosen(Pairs.class, new HashMap<>(), new ArrayList<>()));
        assertEquals("Collection,Map", chosen(Pairs.class, new ArrayList<>(), new HashMap<>()));
        assertEquals("Object,Object", chosen(Pairs.class, 5, new Date(0)));
    }

    @Test
    void noUnboxingWhenAConstructorNeedsNone() {
        assertEquals("Long", chosen(B.class, 7L));
    }

    @Test
    void withoutAnExactFitTheConstructorsAreTriedInOrderAsBefore() {
        // No constructor takes a Double as it is: the String one converts it, as it did
        assertEquals("String", chosen(N.class, 2.5d));
        // One value for two parameters: the constructors are tried in their order, more parameters first
        assertEquals("int,int,int", chosen(P.class, 1));
    }

    @Test
    void theChoiceIsReplayedOnLaterCalls() {
        List<String> seen = new ArrayList<>();
        for (int call = 0; call < 3; call++) {
            seen.add(chosen(N.class, 42L) + "/" + chosen(S.class, new StringBuilder("sb")));
        }
        assertEquals(Collections.nCopies(3, "long/CharSequence"), seen);
    }
}
