package com.cedarsoftware.util;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * TypeUtilities behavior found by review.
 */
class TypeUtilitiesBehaviorTest {

    static class Base {
    }

    static class Holder<T> extends Base {
        T value;
    }

    static class Box<T> implements Serializable {
        T value;
    }

    static class Node<T> implements Comparable<Node<T>> {
        T value;

        @Override
        public int compareTo(Node<T> o) {
            return 0;
        }
    }

    static class Gen<T> {
        T value;
    }

    static class Unrelated extends ArrayList<String> {
    }

    static class Concrete extends Gen<Integer> {
    }

    static class MoreConcrete extends Concrete {
    }

    interface Source<T> {
        T get();
    }

    static class StringSource implements Source<String>, Serializable {
        @Override
        public String get() {
            return "";
        }
    }

    private static Type fieldType(Class<?> c, String name) throws Exception {
        return c.getDeclaredField(name).getGenericType();
    }

    private static Type sourceT() {
        return Source.class.getTypeParameters()[0];
    }

    /**
     * A type variable that nothing binds stays unresolved; it is not replaced by the class's own superclass or
     * interface. Climbing the hierarchy resolved each supertype and returned it as the variable's value:
     * Holder<T> extends Base gave T = Base, Box<T> implements Serializable gave Serializable, and a class
     * unrelated to the variable's own gave its superclass. json-io reading {"value":{...}} as a Holder then
     * made the value an empty Base, and as a Box failed to instantiate Serializable.
     */
    @Test
    void aTypeVariableIsNotResolvedToTheClassesOwnSupertype() throws Exception {
        Type holderT = fieldType(Holder.class, "value");
        assertInstanceOf(TypeVariable.class, TypeUtilities.resolveType(Holder.class, holderT));
        assertEquals("T", TypeUtilities.resolveType(Holder.class, holderT).getTypeName());
        assertEquals(Object.class, TypeUtilities.resolveTypeUsingInstance(new Holder<>(), holderT));
        assertEquals(Object.class, TypeUtilities.resolveTypeUsingInstance(new Box<>(), fieldType(Box.class, "value")));
        assertEquals(Object.class,
                TypeUtilities.resolveTypeUsingInstance(new Node<>(), fieldType(Node.class, "value")));
        Type genT = fieldType(Gen.class, "value");
        assertInstanceOf(TypeVariable.class, TypeUtilities.resolveType(Unrelated.class, genT));

        // what the climb is for: a binding made by a superclass or an interface, at any depth
        assertEquals(Integer.class, TypeUtilities.resolveType(Concrete.class, genT));
        assertEquals(Integer.class, TypeUtilities.resolveType(MoreConcrete.class, genT));
        assertEquals(Integer.class, TypeUtilities.resolveTypeUsingInstance(new MoreConcrete(), genT));
        assertEquals(String.class, TypeUtilities.resolveType(StringSource.class, sourceT()));
    }
}
