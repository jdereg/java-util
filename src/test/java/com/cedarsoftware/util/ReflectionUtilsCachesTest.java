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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    static class MethodHolder {
        public String methodWithOneArg(int value) {
            return String.valueOf(value);
        }

        public String methodWith0Args() {
            return "zero";
        }

        public String methodWith0Args(int value) {
            return String.valueOf(value);
        }
    }

    @Test
    void testGetAllConstructorsSorting() {
        Constructor<?>[] ctors = ReflectionUtils.getAllConstructors(ConstructorTarget.class);
        assertEquals(5, ctors.length);

        // First: public with 1 parameter (more specific)
        assertEquals(1, ctors[0].getParameterCount());
        assertTrue(Modifier.isPublic(ctors[0].getModifiers()));

        // Second: public with 0 parameters
        assertEquals(0, ctors[1].getParameterCount());
        assertTrue(Modifier.isPublic(ctors[1].getModifiers()));

        // Third: protected with 1 parameter
        assertEquals(1, ctors[2].getParameterCount());
        assertTrue(Modifier.isProtected(ctors[2].getModifiers()));

        // Fourth: package-private with 1 parameter
        assertEquals(1, ctors[3].getParameterCount());
        assertFalse(Modifier.isPublic(ctors[3].getModifiers()));
        assertFalse(Modifier.isProtected(ctors[3].getModifiers()));
        assertFalse(Modifier.isPrivate(ctors[3].getModifiers()));

        // Fifth: private with 1 parameter
        assertEquals(1, ctors[4].getParameterCount());
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
            assertFalse(custom.isEmpty());
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
            assertFalse(custom.isEmpty());
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

    @Test
    void testExactMethodMissIsCached() throws Exception {
        Map<Object, Method> original = getCache("METHOD_CACHE");
        Map<Object, Method> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);
        try {
            assertNull(ReflectionUtils.getMethod(MethodHolder.class, "noSuchMethod"));
            assertFalse(custom.isEmpty());
        } finally {
            ReflectionUtils.setMethodCache(original);
        }
    }

    @Test
    void testArgCountLookupDoesNotPoisonExactSignatureLookup() throws Exception {
        Map<Object, Method> original = getCache("METHOD_CACHE");
        Map<Object, Method> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);
        try {
            Method byCount = ReflectionUtils.getMethod(new MethodHolder(), "methodWithOneArg", 1);
            assertNotNull(byCount);

            Method exactMissing = ReflectionUtils.getMethod(MethodHolder.class, "methodWithOneArg", Object.class);
            assertNull(exactMissing);
        } finally {
            ReflectionUtils.setMethodCache(original);
        }
    }

    @Test
    void testNonOverloadedLookupDoesNotPoisonExactNoArgLookup() throws Exception {
        Map<Object, Method> original = getCache("METHOD_CACHE");
        Map<Object, Method> custom = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(custom);
        try {
            assertThrows(IllegalArgumentException.class,
                    () -> ReflectionUtils.getNonOverloadedMethod(MethodHolder.class, "methodWith0Args"));

            Method exact = ReflectionUtils.getMethod(MethodHolder.class, "methodWith0Args");
            assertNotNull(exact);
            assertEquals("methodWith0Args", exact.getName());
            assertEquals(0, exact.getParameterCount());
        } finally {
            ReflectionUtils.setMethodCache(original);
        }
    }
}
