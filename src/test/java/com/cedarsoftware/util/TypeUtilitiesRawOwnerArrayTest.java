package com.cedarsoftware.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Three answers TypeUtilities.resolveType gave wrong (review item TU-8).
 * <ol>
 *   <li>With a raw root (no type arguments known), a bare variable stayed unresolved while the same variable inside a
 *       type argument resolved to its bound: {@code T} gave {@code T}, {@code List<T>} gave {@code List<Number>}.
 *       Both are now the bound, which is what a raw type means in Java.</li>
 *   <li>A variable of the enclosing class, used in an inner class, ignored the owner type: in
 *       {@code OC<String>.Inner}, OC's {@code T} gave {@code Object}, not {@code String}. json-io then stored a Long
 *       where the code expects a String, and a ClassCastException followed wherever it was read.</li>
 *   <li>{@code T[]} resolved to a concrete class came back as a GenericArrayType that prints {@code Integer[]} but
 *       is not {@code Integer[].class}. It is now the array class, as reflection gives a non-generic array; a
 *       GenericArrayType remains only while the component is itself generic ({@code List<String>[]}).</li>
 * </ol>
 */
class TypeUtilitiesRawOwnerArrayTest {

    static class Box<T extends Number> {
        public T value;
        public List<T> list;
        public T[] array;
    }

    static class Plain<T> {
        public T value;
        public List<T> list;
    }

    static class IntBox extends Box<Integer> {
    }

    static class OC<T> {
        class Inner {
            public T value;
            public List<T> list;
            public T[] array;
        }
    }

    static class G<T> {
        public T[] array;
        public T[][] matrix;
        public List<T>[] lists;
    }

    static class Holders {
        public OC<String>.Inner inner;
        public G<Integer> g;
        public G<String> gs;
    }

    private static Type field(Class<?> c, String name) throws Exception {
        return c.getField(name).getGenericType();
    }

    private static Type holder(String name) throws Exception {
        return field(Holders.class, name);
    }

    @Test
    void aRawRootResolvesABareVariableToItsBoundAsInsideATypeArgument() throws Exception {
        assertEquals(Number.class, TypeUtilities.resolveType(Box.class, field(Box.class, "value")));
        ParameterizedType list = (ParameterizedType) TypeUtilities.resolveType(Box.class, field(Box.class, "list"));
        assertEquals(Number.class, list.getActualTypeArguments()[0]);

        assertEquals(Object.class, TypeUtilities.resolveType(Plain.class, field(Plain.class, "value")));
        ParameterizedType plainList = (ParameterizedType) TypeUtilities.resolveType(Plain.class,
                field(Plain.class, "list"));
        assertEquals(Object.class, plainList.getActualTypeArguments()[0]);

        // a root that binds the variable still gives the binding
        assertEquals(Integer.class, TypeUtilities.resolveType(IntBox.class, field(Box.class, "value")));
        assertEquals(Number.class, TypeUtilities.resolveTypeUsingInstance(new Box<Integer>(),
                field(Box.class, "value")));
    }

    @Test
    void anInnerClassTakesItsOuterClassVariableFromTheOwnerType() throws Exception {
        Type inner = holder("inner");      // OC<String>.Inner
        assertEquals(String.class, TypeUtilities.resolveType(inner, field(OC.Inner.class, "value")));
        ParameterizedType list = (ParameterizedType) TypeUtilities.resolveType(inner,
                field(OC.Inner.class, "list"));
        assertEquals(String.class, list.getActualTypeArguments()[0]);
        assertEquals(String[].class, TypeUtilities.resolveType(inner, field(OC.Inner.class, "array")));

        // a raw inner class knows no owner: the bound, as any raw root
        assertEquals(Object.class, TypeUtilities.resolveType(OC.Inner.class, field(OC.Inner.class, "value")));
    }

    @Test
    void anArrayOfAResolvedClassIsTheArrayClass() throws Exception {
        assertEquals(Integer[].class, TypeUtilities.resolveType(holder("g"), field(G.class, "array")));
        assertEquals(Integer[][].class, TypeUtilities.resolveType(holder("g"), field(G.class, "matrix")));
        assertEquals(String[].class, TypeUtilities.resolveType(holder("gs"), field(G.class, "array")));
        assertEquals(Number[].class, TypeUtilities.resolveType(Box.class, field(Box.class, "array")));
        assertEquals(Integer[].class, TypeUtilities.resolveType(IntBox.class, field(Box.class, "array")));

        // still generic while the component is: List<String>[]
        Type lists = TypeUtilities.resolveType(holder("gs"), field(G.class, "lists"));
        assertTrue(lists instanceof GenericArrayType, String.valueOf(lists));
        ParameterizedType component = (ParameterizedType) ((GenericArrayType) lists).getGenericComponentType();
        assertEquals(String.class, component.getActualTypeArguments()[0]);
        assertEquals(List[].class, TypeUtilities.getRawClass(lists));
    }
}
