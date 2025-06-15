package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReflectionUtilsCachesTest {

    static class ConstructorTarget {
        public ConstructorTarget() {}
        public ConstructorTarget(int a) {}
        protected ConstructorTarget(String s) {}
        ConstructorTarget(boolean b) {}
        private ConstructorTarget(double d) {}
    }

    static class FieldHolder {
        public int a;
        private int b;
        static int c;
    }

    static class ParentFields {
        private int parentField;
        transient int transientField;
    }

    static class ChildFields extends ParentFields {
        public String childField;
    }

    @Test
    void testGetAllConstructorsSorting() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorTarget.class);
        assertEquals(5, ctors.length);
        assertEquals(1, ctors[0].getParameterCount());
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));
        assertEquals(0, ctors[1].getParameterCount());
        assertTrue(Modifier.isPublic(ctors[1].getModifiers()));
        assertTrue(Modifier.isProtected(ctors[2].getModifiers()));
        assertFalse(Modifier.isPrivate(ctors[3].getModifiers()));
        assertTrue(Modifier.isPrivate(ctors[4].getModifiers()));
    }

    @Test
    void testGetConstructorCaching() {
        Constructor<?> c1 = ReflectionUtils.getConstructor(ConstructorTarget.class, String.class);
        Constructor<?> c2 = ReflectionUtils.getConstructor(ConstructorTarget.class, String.class);
        assertSame(c1, c2);
        assertNull(ReflectionUtils.getConstructor(ConstructorTarget.class, Float.class));
    }

    @Test
    void testGetDeclaredFieldsWithFilter() {
        Predicate<Field> filter = f -> !Modifier.isStatic(f.getModifiers());
        List<Field> fields = ReflectionUtils.getDeclaredFields(FieldHolder.class, filter);
        assertEquals(2, fields.size());
        List<Field> again = ReflectionUtils.getDeclaredFields(FieldHolder.class, filter);
        assertSame(fields, again);
    }

    @Test
    void testGetDeepDeclaredFields() {
        Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(ChildFields.class);
        assertEquals(2, fields.size());
    }

    @Test
    void testGetDeepDeclaredFieldMap() {
        Map<String, Field> map = ReflectionUtils.getDeepDeclaredFieldMap(ChildFields.class);
        assertTrue(map.containsKey("parentField"));
        assertTrue(map.containsKey("childField"));
        assertFalse(map.containsKey("transientField"));
    }

    @Test
    void testIsJavaCompilerAvailable() {
        System.setProperty("java.util.force.jre", "true");
        assertFalse(ReflectionUtils.isJavaCompilerAvailable());
        System.clearProperty("java.util.force.jre");
        assertTrue(ReflectionUtils.isJavaCompilerAvailable());
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<Object, T> getCache(String field) throws Exception {
        Field f = ReflectionUtils.class.getDeclaredField(field);
        f.setAccessible(true);
        AtomicReference<Map<Object, T>> ref = (AtomicReference<Map<Object, T>>) f.get(null);
        return ref.get();
    }

    @Test
    void testSetMethodCache() throws Exception {
        Map<Object, Method> original = getCache("METHOD_CACHE");
        Map<Object, Method> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);
        try {
            ReflectionUtils.getMethod(FieldHolder.class, "toString");
            assertFalse(custom.isEmpty());
        } finally {
            ReflectionUtils.setMethodCache(original);
        }
    }

    @Test
    void testSetFieldCache() throws Exception {
        Map<Object, Field> original = getCache("FIELD_NAME_CACHE");
        Map<Object, Field> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setFieldCache(custom);
        try {
            ReflectionUtils.getField(FieldHolder.class, "a");
            assertFalse(custom.isEmpty());
        } finally {
            ReflectionUtils.setFieldCache(original);
        }
    }

    @Test
    void testSetClassFieldsCache() throws Exception {
        Map<Object, Collection<Field>> original = getCache("FIELDS_CACHE");
        Map<Object, Collection<Field>> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setClassFieldsCache(custom);
        try {
            ReflectionUtils.getDeclaredFields(FieldHolder.class);
            assertFalse(custom.isEmpty());
        } finally {
            ReflectionUtils.setClassFieldsCache(original);
        }
    }

    @Test
    void testSetClassAnnotationCache() throws Exception {
        Map<Object, Annotation> original = getCache("CLASS_ANNOTATION_CACHE");
        Map<Object, Annotation> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setClassAnnotationCache(custom);
        try {
            ReflectionUtils.getClassAnnotation(FieldHolder.class, Deprecated.class);
            assertTrue(custom.isEmpty());
        } finally {
            ReflectionUtils.setClassAnnotationCache(original);
        }
    }

    @Test
    void testSetMethodAnnotationCache() throws Exception {
        Map<Object, Annotation> original = getCache("METHOD_ANNOTATION_CACHE");
        Map<Object, Annotation> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodAnnotationCache(custom);
        try {
            Method m = Object.class.getDeclaredMethod("toString");
            ReflectionUtils.getMethodAnnotation(m, Deprecated.class);
            assertTrue(custom.isEmpty());
        } finally {
            ReflectionUtils.setMethodAnnotationCache(original);
        }
    }

    @Test
    void testSetConstructorCache() throws Exception {
        Map<Object, Constructor<?>> original = getCache("CONSTRUCTOR_CACHE");
        Map<Object, Constructor<?>> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setConstructorCache(custom);
        try {
            ReflectionUtils.getConstructor(ConstructorTarget.class, String.class);
            assertFalse(custom.isEmpty());
        } finally {
            ReflectionUtils.setConstructorCache(original);
        }
    }

    @Test
    void testSetSortedConstructorsCache() throws Exception {
        Map<Object, Constructor<?>[]> original = getCache("SORTED_CONSTRUCTORS_CACHE");
        Map<Object, Constructor<?>[]> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setSortedConstructorsCache(custom);
        try {
            ReflectionUtils.getAllConstructors(ConstructorTarget.class);
            assertFalse(custom.isEmpty());
        } finally {
            ReflectionUtils.setSortedConstructorsCache(original);
        }
    }
}
