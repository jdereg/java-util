package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class ReflectionUtilsCacheKeyEqualsTest {

    static class FieldSample {
        public int value;
        transient int skip;
        static int ignored;
    }

    @Test
    void testDeprecatedGetDeclaredFields() {
        Collection<Field> fields = new ArrayList<>();
        ReflectionUtils.getDeclaredFields(FieldSample.class, fields);
        assertEquals(1, fields.size());
        assertEquals("value", fields.iterator().next().getName());
    }

    @Test
    void testClassAnnotationCacheKeyEquals() throws Exception {
        Class<?> cls = Class.forName("com.cedarsoftware.util.ReflectionUtils$ClassAnnotationCacheKey");
        Constructor<?> ctor = cls.getDeclaredConstructor(Class.class, Class.class);
        ctor.setAccessible(true);
        Object a = ctor.newInstance(String.class, Deprecated.class);
        Object b = ctor.newInstance(String.class, Deprecated.class);
        Object c = ctor.newInstance(Integer.class, Deprecated.class);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void testMethodAnnotationCacheKeyEquals() throws Exception {
        Class<?> cls = Class.forName("com.cedarsoftware.util.ReflectionUtils$MethodAnnotationCacheKey");
        Constructor<?> ctor = cls.getDeclaredConstructor(Method.class, Class.class);
        ctor.setAccessible(true);
        Method m1 = String.class.getMethod("length");
        Object a = ctor.newInstance(m1, Deprecated.class);
        Object b = ctor.newInstance(m1, Deprecated.class);
        Method m2 = Object.class.getMethod("toString");
        Object c = ctor.newInstance(m2, Deprecated.class);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }

    @Test
    void testFieldNameCacheKeyEquals() throws Exception {
        Class<?> cls = Class.forName("com.cedarsoftware.util.ReflectionUtils$FieldNameCacheKey");
        Constructor<?> ctor = cls.getDeclaredConstructor(Class.class, String.class);
        ctor.setAccessible(true);
        Object a = ctor.newInstance(String.class, "value");
        Object b = ctor.newInstance(String.class, "value");
        Object c = ctor.newInstance(String.class, "hash");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
    }
}
