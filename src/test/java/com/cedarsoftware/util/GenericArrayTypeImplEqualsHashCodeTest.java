package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests equality and hash code for GenericArrayTypeImpl in TypeUtilities.
 */
public class GenericArrayTypeImplEqualsHashCodeTest {

    public static class TestGeneric<T> {
        public T[] arrayField;
    }

    public static class TestInteger extends TestGeneric<Integer> { }
    public static class TestString extends TestGeneric<String> { }

    @Test
    public void testEqualsAndHashCode() throws Exception {
        Field field = TestGeneric.class.getField("arrayField");
        Type arrayType = field.getGenericType();

        Type resolved1 = TypeUtilities.resolveType(TestInteger.class.getGenericSuperclass(), arrayType);
        Type resolved2 = TypeUtilities.resolveType(TestInteger.class.getGenericSuperclass(), arrayType);
        Type resolvedDiff = TypeUtilities.resolveType(TestString.class.getGenericSuperclass(), arrayType);

        assertTrue(resolved1 instanceof GenericArrayType);
        assertTrue(resolved2 instanceof GenericArrayType);
        assertTrue(resolvedDiff instanceof GenericArrayType);

        GenericArrayType gat1 = (GenericArrayType) resolved1;
        GenericArrayType gat2 = (GenericArrayType) resolved2;
        GenericArrayType gatDiff = (GenericArrayType) resolvedDiff;

        assertEquals(gat1, gat2);
        assertEquals(gat1.hashCode(), gat2.hashCode());

        assertNotEquals(gat1, gatDiff);
        assertNotEquals(gat1.hashCode(), gatDiff.hashCode());
    }
}
