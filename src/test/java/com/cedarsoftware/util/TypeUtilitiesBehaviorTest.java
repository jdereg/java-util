package com.cedarsoftware.util;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    static class Parent<T> {
        T field;
    }

    static class Mid<X> extends Parent<List<X>> {
    }

    static class Child extends Mid<Integer> {
    }

    /**
     * A binding that is itself built from another type variable is resolved in turn: Mid<X> extends
     * Parent<List<X>> and Child extends Mid<Integer> make Parent's T, seen from Child, List<Integer>. Only a bare
     * variable was resolved further, so the answer was List<X>.
     */
    @Test
    void aBindingBuiltFromAnotherVariableIsResolvedInTurn() throws Exception {
        Type parentT = fieldType(Parent.class, "field");
        String listOfInteger = "java.util.List<java.lang.Integer>";
        assertEquals(listOfInteger, TypeUtilities.resolveType(Child.class, parentT).getTypeName());
        assertEquals(listOfInteger, TypeUtilities.resolveTypeUsingInstance(new Child(), parentT).getTypeName());
    }

    static class MapGen<T> {
        Map<String, T> map;
        List<? extends T> wild;
    }

    static class ConcreteMap extends MapGen<Integer> {
    }

    static class JdkTypes {
        Map<String, Integer> map;
        List<? extends Integer> wild;
    }

    /**
     * TypeUtilities' own ParameterizedType and WildcardType hash as the JDK's do, so equal types hash alike and a
     * hash set of the JDK's finds ours. Equality already held both ways, but the hashes differed.
     */
    @Test
    void ownTypesHashAsTheJdksDo() throws Exception {
        Type ours = TypeUtilities.resolveType(ConcreteMap.class, fieldType(MapGen.class, "map"));
        Type jdk = fieldType(JdkTypes.class, "map");
        assertNotSame(jdk.getClass(), ours.getClass());
        assertEquals(jdk, ours);
        assertEquals(ours, jdk);
        assertEquals(jdk.hashCode(), ours.hashCode());
        assertTrue(new HashSet<>(Collections.singleton(jdk)).contains(ours));

        Type ourList = TypeUtilities.resolveType(ConcreteMap.class, fieldType(MapGen.class, "wild"));
        Type ourWild = ((ParameterizedType) ourList).getActualTypeArguments()[0];
        Type jdkWild = ((ParameterizedType) fieldType(JdkTypes.class, "wild")).getActualTypeArguments()[0];
        assertNotSame(jdkWild.getClass(), ourWild.getClass());
        assertEquals(jdkWild, ourWild);
        assertEquals(jdkWild.hashCode(), ourWild.hashCode());
    }

    /**
     * getActualTypeArguments() hands out a copy, as the JDK's does. TypeUtilities' own ParameterizedType returned its
     * internal array, and the instance is shared through the cache: a caller writing into it changed the answer
     * every later caller got (Map<String, Integer> became Map<String, Thread>).
     */
    @Test
    void typeArgumentsAreACopy() throws Exception {
        ParameterizedType first = (ParameterizedType) TypeUtilities.resolveType(ConcreteMap.class,
                fieldType(MapGen.class, "map"));
        first.getActualTypeArguments()[1] = Thread.class;          // a caller's scratch edit
        ParameterizedType again = (ParameterizedType) TypeUtilities.resolveType(ConcreteMap.class,
                fieldType(MapGen.class, "map"));
        assertEquals(Integer.class, again.getActualTypeArguments()[1]);
        assertEquals(Integer.class, first.getActualTypeArguments()[1]);
    }

    /** A Type built fresh for each call, with identity equality (it overrides neither equals nor hashCode). */
    static class FreshListType implements ParameterizedType {
        @Override
        public Type[] getActualTypeArguments() {
            return new Type[]{String.class};
        }

        @Override
        public Type getRawType() {
            return List.class;
        }

        @Override
        public Type getOwnerType() {
            return null;
        }
    }

    /**
     * A Type of a class TypeUtilities does not know (neither the JDK's nor its own) is resolved but not cached: its
     * equality is unknown, and with identity equality each fresh object added a cache entry that was never removed
     * (about 290 bytes a call, without bound).
     */
    @Test
    @SuppressWarnings("unchecked")
    void typeObjectsOfUnknownClassesAreNotCached() throws Exception {
        Field cache = TypeUtilities.class.getDeclaredField("RESOLVED");
        cache.setAccessible(true);
        Map<Type, Map<Type, Type>> onConcrete = ((ClassValue<Map<Type, Map<Type, Type>>>) cache.get(null))
                .get(Concrete.class);
        TypeUtilities.resolveType(Concrete.class, fieldType(Gen.class, "value"));     // the root's slot exists
        int before = onConcrete.get(Concrete.class).size();
        for (int i = 0; i < 1000; i++) {
            assertEquals("java.util.List<java.lang.String>",
                    TypeUtilities.resolveType(Concrete.class, new FreshListType()).getTypeName());
        }
        assertEquals(before, onConcrete.get(Concrete.class).size(), "cache entries added for fresh Type objects");
    }
}
