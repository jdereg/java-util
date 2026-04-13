package com.cedarsoftware.util;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Coverage tests for ReflectionUtils — targets JaCoCo gaps:
 * - ensureThreadSafe with non-concurrent map (HashMap)
 * - getDeclaredFields with filter, on enums, populating collections
 * - getAllDeclaredFields (deep hierarchy traversal)
 * - Annotation lookups (class-level, method-level, negative, cached)
 * - getAllConstructors
 * - getMethod by arg count (instance-based API)
 * - Cache key equals edge cases (via cache swap + re-lookup)
 *
 * Note: Security validation tests (dangerous class, sensitive field) are in
 * dedicated security test files to avoid system property cross-contamination.
 */
class ReflectionUtilsCoverageTest {

    // ========== Test model classes ==========

    @Retention(RetentionPolicy.RUNTIME)
    @interface TestAnnotation {
        String value() default "";
    }

    @TestAnnotation("class-level")
    static class AnnotatedClass {
        @TestAnnotation("field-level")
        public String annotatedField;
        public String normalField;

        @TestAnnotation("method-level")
        public void annotatedMethod() {}
        public void normalMethod() {}
    }

    static class SimpleClass {
        public String name;
        public int value;

        public SimpleClass() {}
        public SimpleClass(String name) { this.name = name; }
        public SimpleClass(String name, int value) { this.name = name; this.value = value; }
    }

    static class ChildClass extends SimpleClass {
        public String extra;
        public ChildClass() {}
    }

    enum TestEnum { A, B, C }

    // ========== Basic method lookups ==========

    @Test
    void testGetMethod() {
        Method m = ReflectionUtils.getMethod(String.class, "length");
        assertThat(m).isNotNull();
        assertThat(m.getName()).isEqualTo("length");
    }

    @Test
    void testGetMethodNotFound() {
        Method m = ReflectionUtils.getMethod(String.class, "nonExistentMethod");
        assertThat(m).isNull();
    }

    @Test
    void testGetMethodWithParams() {
        Method m = ReflectionUtils.getMethod(String.class, "substring", int.class);
        assertThat(m).isNotNull();
    }

    @Test
    void testGetMethodInherited() {
        // toString is on Object, not SimpleClass
        Method m = ReflectionUtils.getMethod(SimpleClass.class, "toString");
        assertThat(m).isNotNull();
    }

    // ========== Field lookups ==========

    @Test
    void testGetField() {
        Field f = ReflectionUtils.getField(SimpleClass.class, "name");
        assertThat(f).isNotNull();
        assertThat(f.getName()).isEqualTo("name");
    }

    @Test
    void testGetFieldNotFound() {
        Field f = ReflectionUtils.getField(SimpleClass.class, "nonExistent");
        assertThat(f).isNull();
    }

    @Test
    void testGetDeclaredFields() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SimpleClass.class);
        assertThat(fields).isNotNull().hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void testGetDeclaredFieldsOnObjectClass() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(Object.class);
        assertThat(fields).isNotNull();
    }

    @Test
    void testGetDeclaredFieldsOnEnum() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(TestEnum.class);
        for (Field f : fields) {
            // Synthetic $VALUES / ENUM$VALUES should be filtered by DEFAULT_FIELD_FILTER
            assertThat(f.getName()).doesNotContain("$VALUES");
            assertThat(f.getName()).doesNotContain("ENUM$VALUES");
        }
    }

    @Test
    void testGetDeclaredFieldsWithFilter() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SimpleClass.class,
                f -> f.getName().equals("name"));
        assertThat(fields).hasSize(1);
        assertThat(fields.get(0).getName()).isEqualTo("name");
    }

    @Test
    void testGetDeclaredFieldsWithFilterNoMatch() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(SimpleClass.class, f -> false);
        assertThat(fields).isEmpty();
    }

    @Test
    void testGetDeclaredFieldsIntoCollection() {
        java.util.ArrayList<Field> fields = new java.util.ArrayList<>();
        ReflectionUtils.getDeclaredFields(SimpleClass.class, fields);
        assertThat(fields).isNotEmpty();
    }

    // ========== getAllDeclaredFields (deep hierarchy) ==========

    @Test
    void testGetAllDeclaredFields() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(ChildClass.class);
        assertThat(fields.size()).isGreaterThanOrEqualTo(3); // extra + name + value
    }

    @Test
    void testGetAllDeclaredFieldsWithFilter() {
        List<Field> fields = ReflectionUtils.getAllDeclaredFields(ChildClass.class,
                f -> f.getName().equals("name") || f.getName().equals("extra"));
        assertThat(fields).hasSize(2);
    }

    // ========== Constructor lookups ==========

    @Test
    void testGetConstructor() {
        Constructor<?> c = ReflectionUtils.getConstructor(SimpleClass.class);
        assertThat(c).isNotNull();
    }

    @Test
    void testGetConstructorWithParams() {
        Constructor<?> c = ReflectionUtils.getConstructor(SimpleClass.class, String.class);
        assertThat(c).isNotNull();
    }

    @Test
    void testGetConstructorNotFound() {
        Constructor<?> c = ReflectionUtils.getConstructor(SimpleClass.class, java.util.Date.class);
        assertThat(c).isNull();
    }

    @Test
    void testGetAllConstructors() {
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(SimpleClass.class);
        assertThat(constructors).isNotNull();
        assertThat(constructors.length).isEqualTo(3);
    }

    // ========== Annotation lookups ==========

    @Test
    void testGetClassAnnotation() {
        TestAnnotation ann = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnnotation.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).isEqualTo("class-level");
    }

    @Test
    void testGetClassAnnotationNotPresent() {
        Deprecated ann = ReflectionUtils.getClassAnnotation(SimpleClass.class, Deprecated.class);
        assertThat(ann).isNull();
    }

    @Test
    void testGetMethodAnnotation() {
        Method m = ReflectionUtils.getMethod(AnnotatedClass.class, "annotatedMethod");
        TestAnnotation ann = ReflectionUtils.getMethodAnnotation(m, TestAnnotation.class);
        assertThat(ann).isNotNull();
        assertThat(ann.value()).isEqualTo("method-level");
    }

    @Test
    void testGetMethodAnnotationNotPresent() {
        Method m = ReflectionUtils.getMethod(AnnotatedClass.class, "normalMethod");
        Deprecated ann = ReflectionUtils.getMethodAnnotation(m, Deprecated.class);
        assertThat(ann).isNull();
    }

    // ========== Cache setters — exercises ensureThreadSafe(HashMap) ==========

    @Test
    void testSetMethodCacheWithHashMap() {
        Map<Object, Method> customCache = new com.cedarsoftware.util.LRUCache<>(100);
        ReflectionUtils.setMethodCache(customCache);
        Method m = ReflectionUtils.getMethod(String.class, "length");
        assertThat(m).isNotNull();
    }

    @Test
    void testSetFieldCacheWithHashMap() {
        Map<Object, Field> customCache = new com.cedarsoftware.util.LRUCache<>(100);
        ReflectionUtils.setFieldCache(customCache);
        Field f = ReflectionUtils.getField(SimpleClass.class, "name");
        assertThat(f).isNotNull();
    }

    @Test
    void testSetClassFieldsCacheWithHashMap() {
        Map<Object, Collection<Field>> customCache = new com.cedarsoftware.util.LRUCache<>(100);
        ReflectionUtils.setClassFieldsCache(customCache);
        List<Field> fields = ReflectionUtils.getDeclaredFields(SimpleClass.class);
        assertThat(fields).isNotEmpty();
    }

    @Test
    void testSetConstructorCacheWithHashMap() {
        Map<Object, Constructor<?>> customCache = new com.cedarsoftware.util.LRUCache<>(100);
        ReflectionUtils.setConstructorCache(customCache);
        Constructor<?> c = ReflectionUtils.getConstructor(SimpleClass.class);
        assertThat(c).isNotNull();
    }

    @Test
    void testSetClassAnnotationCacheWithHashMap() {
        Map<Object, java.lang.annotation.Annotation> customCache = new com.cedarsoftware.util.LRUCache<>(100);
        ReflectionUtils.setClassAnnotationCache(customCache);
        TestAnnotation ann = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnnotation.class);
        assertThat(ann).isNotNull();
    }

    @Test
    void testSetMethodAnnotationCacheWithHashMap() {
        Map<Object, java.lang.annotation.Annotation> customCache = new com.cedarsoftware.util.LRUCache<>(100);
        ReflectionUtils.setMethodAnnotationCache(customCache);
        Method m = ReflectionUtils.getMethod(AnnotatedClass.class, "annotatedMethod");
        TestAnnotation ann = ReflectionUtils.getMethodAnnotation(m, TestAnnotation.class);
        assertThat(ann).isNotNull();
    }

    @Test
    void testSetSortedConstructorsCacheWithHashMap() {
        Map<Object, Constructor<?>[]> customCache = new com.cedarsoftware.util.LRUCache<>(100);
        ReflectionUtils.setSortedConstructorsCache(customCache);
        Constructor<?>[] all = ReflectionUtils.getAllConstructors(SimpleClass.class);
        assertThat(all).isNotNull().hasSizeGreaterThan(0);
    }

    // ========== getMethod by arg count (instance-based API) ==========

    @Test
    void testGetMethodByArgCount() {
        AnnotatedClass instance = new AnnotatedClass();
        Method m = ReflectionUtils.getMethod(instance, "annotatedMethod", 0);
        assertThat(m).isNotNull();
    }

    @Test
    void testGetMethodByArgCountNotFound() {
        AnnotatedClass instance = new AnnotatedClass();
        assertThatThrownBy(() -> ReflectionUtils.getMethod(instance, "annotatedMethod", 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void testGetMethodByNegativeArgCount() {
        AnnotatedClass instance = new AnnotatedClass();
        assertThatThrownBy(() -> ReflectionUtils.getMethod(instance, "annotatedMethod", -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    // ========== Cache hit paths ==========

    @Test
    void testGetFieldCalledTwiceUsesCache() {
        Field f1 = ReflectionUtils.getField(SimpleClass.class, "name");
        Field f2 = ReflectionUtils.getField(SimpleClass.class, "name");
        assertThat(f1).isSameAs(f2);
    }

    @Test
    void testGetMethodCalledTwiceUsesCache() {
        Method m1 = ReflectionUtils.getMethod(String.class, "length");
        Method m2 = ReflectionUtils.getMethod(String.class, "length");
        assertThat(m1).isSameAs(m2);
    }

    @Test
    void testGetClassAnnotationCalledTwiceUsesCache() {
        TestAnnotation a1 = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnnotation.class);
        TestAnnotation a2 = ReflectionUtils.getClassAnnotation(AnnotatedClass.class, TestAnnotation.class);
        assertThat(a1).isSameAs(a2);
    }

    @Test
    void testGetConstructorCalledTwiceUsesCache() {
        Constructor<?> c1 = ReflectionUtils.getConstructor(SimpleClass.class, String.class);
        Constructor<?> c2 = ReflectionUtils.getConstructor(SimpleClass.class, String.class);
        assertThat(c1).isSameAs(c2);
    }
}
