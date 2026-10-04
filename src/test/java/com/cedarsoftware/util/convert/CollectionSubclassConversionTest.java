package com.cedarsoftware.util.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Converting to a concrete Collection class of the caller's own returns an instance of that class. It returned the
 * closest known type instead -- converting to {@code MyList extends ArrayList} gave an {@code ArrayList}, which is not
 * a {@code MyList} -- while converting to a Map subclass already returned the subclass. Interfaces, abstract types, the
 * types with a factory of their own, and classes without a no-argument constructor are converted as before.
 */
class CollectionSubclassConversionTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    public static class MyList extends ArrayList<Object> {
    }

    public static class MySet extends HashSet<Object> {
    }

    public static class MyLinkedList extends LinkedList<Object> {
    }

    /** No no-argument constructor: nothing to make one with, so the closest known type, as before. */
    public static class NeedsArgs extends ArrayList<Object> {
        public NeedsArgs(String required) {
            super();
        }
    }

    @Test
    void aConcreteSubclassIsWhatComesBack() {
        List<Integer> source = new ArrayList<>(Arrays.asList(1, 2, 3));
        assertSame(MyList.class, converter.convert(source, MyList.class).getClass());
        assertEquals(source, converter.convert(source, MyList.class));
        assertSame(MySet.class, converter.convert(source, MySet.class).getClass());
        assertEquals(new HashSet<>(source), converter.convert(source, MySet.class));
        assertSame(MyLinkedList.class, converter.convert(source, MyLinkedList.class).getClass());
        assertEquals(source, converter.convert(source, MyLinkedList.class));
    }

    @Test
    void anArrayConvertsToAConcreteSubclassToo() {
        MyList list = converter.convert(new Object[]{"a", "b"}, MyList.class);
        assertSame(MyList.class, list.getClass());
        assertEquals(Arrays.asList("a", "b"), list);
        MySet set = converter.convert(new int[]{4, 5}, MySet.class);
        assertSame(MySet.class, set.getClass());
        assertEquals(new HashSet<>(Arrays.asList(4, 5)), set);
    }

    @Test
    void everythingElseConvertsAsBefore() {
        List<Integer> source = new ArrayList<>(Arrays.asList(1, 2));
        assertSame(ArrayList.class, converter.convert(source, List.class).getClass());
        assertSame(ArrayList.class, converter.convert(source, Collection.class).getClass());
        assertSame(LinkedHashSet.class, converter.convert(source, Set.class).getClass());
        assertSame(ArrayList.class, converter.convert(source, ArrayList.class).getClass());
        assertSame(HashSet.class, converter.convert(source, HashSet.class).getClass());
        assertSame(LinkedList.class, converter.convert(source, LinkedList.class).getClass());
        Object needsArgs = converter.convert(source, NeedsArgs.class);
        assertTrue(needsArgs instanceof ArrayList, String.valueOf(needsArgs.getClass()));
        assertEquals(source, needsArgs);
    }
}
