package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * newInstance(Converter, Class, Collection) was removed. A caller holding a Collection now binds to
 * newInstance(Converter, Class, Object), which treats a Collection as the list of constructor arguments,
 * matched by type -- exactly what the removed overload did, as it only passed the Collection on to this one.
 */
class ClassUtilitiesCollectionArgumentTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    public static class Pair {
        final String which;
        final Object first;
        final Object second;

        public Pair() {
            which = "none";
            first = null;
            second = null;
        }

        public Pair(String a) {
            which = "String";
            first = a;
            second = null;
        }

        public Pair(String a, Integer b) {
            which = "String,Integer";
            first = a;
            second = b;
        }
    }

    @Test
    void aCollectionIsTheConstructorArguments() {
        Collection<?> args = Arrays.asList("x", 7);
        Pair pair = (Pair) ClassUtilities.newInstance(converter, Pair.class, args);
        assertEquals("String,Integer", pair.which);
        assertEquals("x", pair.first);
        assertEquals(7, pair.second);
    }

    @Test
    void aCollectionIsNotASingleArgument() {
        // The two-element list is two arguments, not one: the (String) constructor is not given the list
        Object args = new ArrayList<>(Arrays.asList("x", 7));
        Pair pair = (Pair) ClassUtilities.newInstance(converter, Pair.class, args);
        assertEquals("String,Integer", pair.which);
        assertEquals("x", pair.first);
        assertEquals(7, pair.second);
    }

    @Test
    void oneElementCollectionPicksTheOneArgumentConstructor() {
        Collection<?> args = Collections.singletonList("only");
        Pair pair = (Pair) ClassUtilities.newInstance(converter, Pair.class, args);
        assertEquals("String", pair.which);
        assertEquals("only", pair.first);
    }

    @Test
    void anEmptyCollectionPicksTheNoArgumentConstructor() {
        Collection<?> args = Collections.emptyList();
        Pair pair = (Pair) ClassUtilities.newInstance(converter, Pair.class, args);
        assertEquals("none", pair.which);
    }
}
