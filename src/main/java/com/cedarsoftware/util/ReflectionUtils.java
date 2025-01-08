package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cedarsoftware.util.ExceptionUtilities.safelyIgnoreException;

/**
 * Utilities to simplify writing reflective code as well as improve performance of reflective operations like
 * method and annotation lookups.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class ReflectionUtils {
    private static final int CACHE_SIZE = 1000;

    private static volatile Map<ConstructorCacheKey, Constructor<?>> CONSTRUCTOR_CACHE = new LRUCache<>(CACHE_SIZE);
    private static volatile Map<MethodCacheKey, Method> METHOD_CACHE = new LRUCache<>(CACHE_SIZE);
    private static volatile Map<FieldsCacheKey, Collection<Field>> FIELDS_CACHE = new LRUCache<>(CACHE_SIZE);
    private static volatile Map<FieldNameCacheKey, Field> FIELD_NAME_CACHE = new LRUCache<>(CACHE_SIZE * 10);
    private static volatile Map<ClassAnnotationCacheKey, Annotation> CLASS_ANNOTATION_CACHE = new LRUCache<>(CACHE_SIZE);
    private static volatile Map<MethodAnnotationCacheKey, Annotation> METHOD_ANNOTATION_CACHE = new LRUCache<>(CACHE_SIZE);

    /**
     * Sets a custom cache implementation for method lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing method lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setMethodCache(Map<Object, Method> cache) {
        METHOD_CACHE = (Map) cache;
    }

    /**
     * Sets a custom cache implementation for field lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing field lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setClassFieldsCache(Map<Object, Collection<Field>> cache) {
        FIELDS_CACHE = (Map) cache;
    }
    
    /**
     * Sets a custom cache implementation for field lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing field lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setFieldCache(Map<Object, Field> cache) {
        FIELD_NAME_CACHE = (Map) cache;
    }

    /**
     * Sets a custom cache implementation for class annotation lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing class annotation lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setClassAnnotationCache(Map<Object, Annotation> cache) {
        CLASS_ANNOTATION_CACHE = (Map) cache;
    }

    /**
     * Sets a custom cache implementation for method annotation lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing method annotation lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setMethodAnnotationCache(Map<Object, Annotation> cache) {
        METHOD_ANNOTATION_CACHE = (Map) cache;
    }

    /**
     * Sets a custom cache implementation for constructor lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing constructor lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setConstructorCache(Map<Object, Constructor<?>> cache) {
        CONSTRUCTOR_CACHE = (Map) cache;
    }
    
    private ReflectionUtils() { }

    private static final class ClassAnnotationCacheKey {
        private final String classLoaderName;
        private final String className;
        private final String annotationClassName;
        private final int hash;

        ClassAnnotationCacheKey(Class<?> clazz, Class<? extends Annotation> annotationClass) {
            this.classLoaderName = getClassLoaderName(clazz);
            this.className = clazz.getName();
            this.annotationClassName = annotationClass.getName();
            this.hash = Objects.hash(classLoaderName, className, annotationClassName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClassAnnotationCacheKey)) return false;
            ClassAnnotationCacheKey that = (ClassAnnotationCacheKey) o;
            return Objects.equals(classLoaderName, that.classLoaderName) &&
                    Objects.equals(className, that.className) &&
                    Objects.equals(annotationClassName, that.annotationClassName);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class MethodAnnotationCacheKey {
        private final String classLoaderName;
        private final String className;
        private final String methodName;
        private final String parameterTypes;
        private final String annotationClassName;
        private final int hash;

        MethodAnnotationCacheKey(Method method, Class<? extends Annotation> annotationClass) {
            Class<?> declaringClass = method.getDeclaringClass();
            this.classLoaderName = getClassLoaderName(declaringClass);
            this.className = declaringClass.getName();
            this.methodName = method.getName();
            this.parameterTypes = makeParamKey(method.getParameterTypes());
            this.annotationClassName = annotationClass.getName();
            this.hash = Objects.hash(classLoaderName, className, methodName, parameterTypes, annotationClassName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodAnnotationCacheKey)) return false;
            MethodAnnotationCacheKey that = (MethodAnnotationCacheKey) o;
            return Objects.equals(classLoaderName, that.classLoaderName) &&
                    Objects.equals(className, that.className) &&
                    Objects.equals(methodName, that.methodName) &&
                    Objects.equals(parameterTypes, that.parameterTypes) &&
                    Objects.equals(annotationClassName, that.annotationClassName);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class ConstructorCacheKey {
        private final String classLoaderName;
        private final String className;
        private final String parameterTypes;
        private final int hash;

        ConstructorCacheKey(Class<?> clazz, Class<?>... types) {
            this.classLoaderName = getClassLoaderName(clazz);
            this.className = clazz.getName();
            this.parameterTypes = makeParamKey(types);
            this.hash = Objects.hash(classLoaderName, className, parameterTypes);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConstructorCacheKey)) return false;
            ConstructorCacheKey that = (ConstructorCacheKey) o;
            return Objects.equals(classLoaderName, that.classLoaderName) &&
                    Objects.equals(className, that.className) &&
                    Objects.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class FieldNameCacheKey {
        private final String classLoaderName;
        private final String className;
        private final String fieldName;
        private final int hash;

        FieldNameCacheKey(Class<?> clazz, String fieldName) {
            this.classLoaderName = getClassLoaderName(clazz);
            this.className = clazz.getName();
            this.fieldName = fieldName;
            this.hash = Objects.hash(classLoaderName, className, fieldName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FieldNameCacheKey)) return false;
            FieldNameCacheKey that = (FieldNameCacheKey) o;
            return Objects.equals(classLoaderName, that.classLoaderName) &&
                    Objects.equals(className, that.className) &&
                    Objects.equals(fieldName, that.fieldName);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    private static final class FieldsCacheKey {
        private final String classLoaderName;
        private final String className;
        private final boolean deep;
        private final int hash;

        FieldsCacheKey(Class<?> clazz, boolean deep) {
            this.classLoaderName = getClassLoaderName(clazz);
            this.className = clazz.getName();
            this.deep = deep;
            this.hash = Objects.hash(classLoaderName, className, deep);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FieldsCacheKey)) return false;
            FieldsCacheKey other = (FieldsCacheKey) o;
            return deep == other.deep &&
                    Objects.equals(classLoaderName, other.classLoaderName) &&
                    Objects.equals(className, other.className);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static class MethodCacheKey {
        private final String classLoaderName;
        private final String className;
        private final String methodName;
        private final String parameterTypes;
        private final int hash;

        public MethodCacheKey(Class<?> clazz, String methodName, Class<?>... types) {
            this.classLoaderName = getClassLoaderName(clazz);
            this.className = clazz.getName();
            this.methodName = methodName;
            this.parameterTypes = makeParamKey(types);

            // Pre-compute hash code
            this.hash = Objects.hash(classLoaderName, className, methodName, parameterTypes);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodCacheKey)) return false;
            MethodCacheKey that = (MethodCacheKey) o;
            return Objects.equals(classLoaderName, that.classLoaderName) &&
                    Objects.equals(className, that.className) &&
                    Objects.equals(methodName, that.methodName) &&
                    Objects.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
    
    /**
     * Searches for a specific annotation on a class, examining the entire inheritance hierarchy.
     * Results (including misses) are cached for performance.
     * <p>
     * This method performs an exhaustive search through:
     * <ul>
     *     <li>The class itself</li>
     *     <li>All superclasses</li>
     *     <li>All implemented interfaces</li>
     *     <li>All super-interfaces</li>
     * </ul>
     * <p>
     * Key behaviors:
     * <ul>
     *     <li>Caches both found annotations and misses (nulls)</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Uses depth-first search through the inheritance hierarchy</li>
     *     <li>Prevents circular reference issues</li>
     *     <li>Returns the first matching annotation found</li>
     *     <li>Thread-safe implementation</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * JsonObject anno = ReflectionUtils.getClassAnnotation(MyClass.class, JsonObject.class);
     * if (anno != null) {
     *     // Process annotation...
     * }
     * </pre>
     *
     * @param classToCheck The class to search for the annotation
     * @param annoClass The annotation class to search for
     * @param <T> The type of the annotation
     * @return The annotation if found, null otherwise
     * @throws IllegalArgumentException if either classToCheck or annoClass is null
     */
    public static <T extends Annotation> T getClassAnnotation(final Class<?> classToCheck, final Class<T> annoClass) {
        if (classToCheck == null) {
            return null; // legacy behavior, not changing now.
        }
        Convention.throwIfNull(annoClass, "annotation class cannot be null");

        ClassAnnotationCacheKey key = new ClassAnnotationCacheKey(classToCheck, annoClass);

        // Check cache first
        Annotation cached = CLASS_ANNOTATION_CACHE.get(key);
        if (cached != null || CLASS_ANNOTATION_CACHE.containsKey(key)) {
            return (T) cached;
        }

        // Not in cache, do the lookup
        T found = findClassAnnotation(classToCheck, annoClass);

        // Cache the result (even if null)
        CLASS_ANNOTATION_CACHE.put(key, found);
        return found;
    }

    private static <T extends Annotation> T findClassAnnotation(Class<?> classToCheck, Class<T> annoClass) {
        final Set<Class<?>> visited = new HashSet<>();
        final LinkedList<Class<?>> stack = new LinkedList<>();
        stack.add(classToCheck);

        while (!stack.isEmpty()) {
            Class<?> classToChk = stack.pop();
            if (classToChk == null || visited.contains(classToChk)) {
                continue;
            }
            visited.add(classToChk);
            T a = classToChk.getAnnotation(annoClass);
            if (a != null) {
                return a;
            }
            stack.push(classToChk.getSuperclass());
            addInterfaces(classToChk, stack);
        }
        return null;
    }
    
    private static void addInterfaces(final Class<?> classToCheck, final LinkedList<Class<?>> stack) {
        for (Class<?> interFace : classToCheck.getInterfaces()) {
            stack.push(interFace);
        }
    }

    /**
     * Searches for a specific annotation on a method, examining the entire inheritance hierarchy.
     * Results (including misses) are cached for performance.
     * <p>
     * This method performs an exhaustive search through:
     * <ul>
     *     <li>The method in the declaring class</li>
     *     <li>Matching methods in all superclasses</li>
     *     <li>Matching methods in all implemented interfaces</li>
     *     <li>Matching methods in all super-interfaces</li>
     * </ul>
     * <p>
     * Key behaviors:
     * <ul>
     *     <li>Caches both found annotations and misses (nulls)</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Uses depth-first search through the inheritance hierarchy</li>
     *     <li>Matches methods by name and parameter types</li>
     *     <li>Prevents circular reference issues</li>
     *     <li>Returns the first matching annotation found</li>
     *     <li>Thread-safe implementation</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * Method method = obj.getClass().getMethod("processData", String.class);
     * JsonProperty anno = ReflectionUtils.getMethodAnnotation(method, JsonProperty.class);
     * if (anno != null) {
     *     // Process annotation...
     * }
     * </pre>
     *
     * @param method The method to search for the annotation
     * @param annoClass The annotation class to search for
     * @param <T> The type of the annotation
     * @return The annotation if found, null otherwise
     * @throws IllegalArgumentException if either method or annoClass is null
     */
    public static <T extends Annotation> T getMethodAnnotation(final Method method, final Class<T> annoClass) {
        Convention.throwIfNull(method, "method cannot be null");
        Convention.throwIfNull(annoClass, "annotation class cannot be null");

        MethodAnnotationCacheKey key = new MethodAnnotationCacheKey(method, annoClass);

        // Check cache first
        Annotation cached = METHOD_ANNOTATION_CACHE.get(key);
        if (cached != null || METHOD_ANNOTATION_CACHE.containsKey(key)) {
            return (T) cached;
        }

        // Search through class hierarchy
        Class<?> currentClass = method.getDeclaringClass();
        while (currentClass != null) {
            try {
                Method currentMethod = currentClass.getDeclaredMethod(
                        method.getName(),
                        method.getParameterTypes()
                );

                T annotation = currentMethod.getAnnotation(annoClass);
                if (annotation != null) {
                    METHOD_ANNOTATION_CACHE.put(key, annotation);
                    return annotation;
                }
            } catch (NoSuchMethodException ignored) {
                // Method not found in current class, continue up hierarchy
            }
            currentClass = currentClass.getSuperclass();
        }

        // Also check interfaces
        for (Class<?> iface : method.getDeclaringClass().getInterfaces()) {
            try {
                Method ifaceMethod = iface.getMethod(
                        method.getName(),
                        method.getParameterTypes()
                );
                T annotation = ifaceMethod.getAnnotation(annoClass);
                if (annotation != null) {
                    METHOD_ANNOTATION_CACHE.put(key, annotation);
                    return annotation;
                }
            } catch (NoSuchMethodException ignored) {
                // Method not found in interface
            }
        }

        // Cache the miss
        METHOD_ANNOTATION_CACHE.put(key, null);
        return null;
    }
    
    /**
     * Retrieves a specific field from a class by name, searching through the entire class hierarchy
     * (including superclasses). Results are cached for performance.
     * <p>
     * This method:
     * <ul>
     *     <li>Searches through all fields (public, protected, package, private)</li>
     *     <li>Includes fields from superclasses</li>
     *     <li>Excludes static fields</li>
     *     <li>Makes non-public fields accessible</li>
     *     <li>Caches results (including misses) for performance</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * Field nameField = ReflectionUtils.getField(Employee.class, "name");
     * if (nameField != null) {
     *     nameField.set(employee, "John");
     * }
     * </pre>
     *
     * @param c The class to search for the field
     * @param fieldName The name of the field to find
     * @return The Field object if found, null if the field doesn't exist
     * @throws IllegalArgumentException if either the class or fieldName is null
     */
    public static Field getField(Class<?> c, String fieldName) {
        Convention.throwIfNull(c, "class cannot be null");
        Convention.throwIfNull(fieldName, "fieldName cannot be null");

        FieldNameCacheKey key = new FieldNameCacheKey(c, fieldName);

        // Check if we already cached this field lookup
        Field cachedField = FIELD_NAME_CACHE.get(key);
        if (cachedField != null || FIELD_NAME_CACHE.containsKey(key)) {  // Handle null field case (caches misses)
            return cachedField;
        }

        // Not in cache, do the linear search
        Collection<Field> fields = getAllDeclaredFields(c);
        Field found = null;
        for (Field field : fields) {
            if (fieldName.equals(field.getName())) {
                found = field;
                break;
            }
        }

        // Cache the result (even if null)
        FIELD_NAME_CACHE.put(key, found);
        return found;
    }

    /**
     * Retrieves the declared fields of a class, with sophisticated filtering and caching.
     * This method provides direct field access with careful handling of special cases.
     * <p>
     * Field filtering:
     * <ul>
     *     <li>Returns only fields declared directly on the specified class (not from superclasses)</li>
     *     <li>Excludes static fields</li>
     *     <li>Excludes internal enum fields ("internal" and "ENUM$VALUES")</li>
     *     <li>Excludes enum base class fields ("hash" and "ordinal")</li>
     *     <li>Excludes Groovy's metaClass field</li>
     * </ul>
     * <p>
     * Included fields:
     * <ul>
     *     <li>All instance fields (public, protected, package, private)</li>
     *     <li>Transient fields</li>
     *     <li>Synthetic fields for inner classes (e.g., "$this" reference to enclosing class)</li>
     *     <li>Compiler-generated fields for anonymous classes and lambdas</li>
     * </ul>
     * <p>
     * Key behaviors:
     * <ul>
     *     <li>Attempts to make non-public fields accessible</li>
     *     <li>Caches both successful lookups and misses</li>
     *     <li>Returns an unmodifiable List to prevent modification</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Thread-safe implementation</li>
     *     <li>Maintains consistent order of fields</li>
     * </ul>
     * <p>
     * Note: For fields from the entire class hierarchy, use {@link #getAllDeclaredFields(Class)} instead.
     * <p>
     * Example usage:
     * <pre>
     * List<Field> fields = ReflectionUtils.getDeclaredFields(MyClass.class);
     * for (Field field : fields) {
     *     // Process each field...
     * }
     * </pre>
     *
     * @param c The class whose declared fields are to be retrieved
     * @return An unmodifiable list of the class's declared fields
     * @throws IllegalArgumentException if the class is null
     * @see #getAllDeclaredFields(Class) For retrieving fields from the entire class hierarchy
     */
    public static List<Field> getDeclaredFields(final Class<?> c) {
        Convention.throwIfNull(c, "class cannot be null");
        FieldsCacheKey key = new FieldsCacheKey(c, false);
        Collection<Field> cached = FIELDS_CACHE.get(key);
        if (cached != null || FIELDS_CACHE.containsKey(key)) {  // Cache misses so we don't retry over and over
            return (List<Field>) cached;
        }

        Field[] fields = c.getDeclaredFields();
        List<Field> list = new ArrayList<>(fields.length);  // do not change from being List

        for (Field field : fields) {
            String fieldName = field.getName();
            if (Modifier.isStatic(field.getModifiers()) ||
                    (field.getDeclaringClass().isEnum() && ("internal".equals(fieldName) || "ENUM$VALUES".equals(fieldName))) ||
                    ("metaClass".equals(fieldName) && "groovy.lang.MetaClass".equals(field.getType().getName())) ||
                    (field.getDeclaringClass().isAssignableFrom(Enum.class) && ("hash".equals(fieldName) || "ordinal".equals(fieldName)))) {
                continue;
            }

            if (!Modifier.isPublic(field.getModifiers())) {
                try {
                    field.setAccessible(true);
                } catch(Exception ignored) { }
            }

            list.add(field);
        }

        List<Field> unmodifiableFields = Collections.unmodifiableList(list);
        FIELDS_CACHE.put(key, unmodifiableFields);
        return unmodifiableFields;
    }

    /**
     * Retrieves all fields from a class and its complete inheritance hierarchy, with
     * sophisticated filtering and caching. This method provides comprehensive field access
     * across the entire class hierarchy up to Object.
     * <p>
     * Field filtering:
     * <ul>
     *     <li>Includes fields from the specified class and all superclasses</li>
     *     <li>Excludes static fields</li>
     *     <li>Excludes internal enum fields ("internal" and "ENUM$VALUES")</li>
     *     <li>Excludes enum base class fields ("hash" and "ordinal")</li>
     *     <li>Excludes Groovy's metaClass field</li>
     * </ul>
     * <p>
     * Included fields:
     * <ul>
     *     <li>All instance fields (public, protected, package, private)</li>
     *     <li>Fields from all superclasses up to Object</li>
     *     <li>Transient fields</li>
     *     <li>Synthetic fields for inner classes (e.g., "$this" reference to enclosing class)</li>
     *     <li>Compiler-generated fields for anonymous classes and lambdas</li>
     * </ul>
     * <p>
     * Key behaviors:
     * <ul>
     *     <li>Attempts to make non-public fields accessible</li>
     *     <li>Caches both successful lookups and misses</li>
     *     <li>Returns an unmodifiable List to prevent modification</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Thread-safe implementation</li>
     *     <li>Maintains consistent order of fields (subclass fields before superclass fields)</li>
     *     <li>Uses recursive caching strategy for optimal performance</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * List<Field> allFields = ReflectionUtils.getAllDeclaredFields(MyClass.class);
     * for (Field field : allFields) {
     *     // Process each field, including inherited fields...
     * }
     * </pre>
     *
     * @param c The class whose complete field hierarchy is to be retrieved
     * @return An unmodifiable list of all fields in the class hierarchy
     * @throws IllegalArgumentException if the class is null
     * @see #getDeclaredFields(Class) For retrieving fields from just the specified class
     */
    public static List<Field> getAllDeclaredFields(final Class<?> c) {
        Convention.throwIfNull(c, "class cannot be null");
        FieldsCacheKey key = new FieldsCacheKey(c, true);
        Collection<Field> cached = FIELDS_CACHE.get(key);
        if (cached != null || FIELDS_CACHE.containsKey(key)) {  // Cache misses so we do not retry over and over
            return (List<Field>) cached;
        }

        List<Field> allFields = new ArrayList<>();
        Class<?> current = c;
        while (current != null) {
            allFields.addAll(getDeclaredFields(current));
            current = current.getSuperclass();
        }

        List<Field> unmodifiableFields = Collections.unmodifiableList(allFields);
        FIELDS_CACHE.put(key, unmodifiableFields);
        return unmodifiableFields;
    }

    /**
     * Return all Fields from a class (including inherited), mapped by String field name
     * to java.lang.reflect.Field.  This method uses getDeclaredFields(Class) to obtain
     * the methods from a class, therefore it will have the same field inclusion rules
     * as getAllDeclaredFields().
     * <p>
     * Additional Field mapping rules for String key names:
     * <ul>
     *     <li>Simple field names (e.g., "name") are used when no collision exists</li>
     *     <li>Qualified names (e.g., "com.example.Parent.name") are used to resolve collisions</li>
     *     <li>Child class fields take precedence for simple name mapping</li>
     *     <li>Parent class fields use fully qualified names when shadowed</li>
     * </ul>
     * <p>
     *
     * @param c Class whose fields are being fetched
     * @return Map of filtered fields on the Class, keyed by String field name to Field
     * @throws IllegalArgumentException if the class is null
     */
    public static Map<String, Field> getAllDeclaredFieldsMap(Class<?> c) {
        Convention.throwIfNull(c, "class cannot be null");

        Map<String, Field> fieldMap = new LinkedHashMap<>();
        Collection<Field> fields = getAllDeclaredFields(c);  // Uses FIELDS_CACHE internally

        for (Field field : fields) {
            String fieldName = field.getName();
            if (fieldMap.containsKey(fieldName)) {   // Can happen when parent and child class both have private field with same name
                fieldMap.put(field.getDeclaringClass().getName() + '.' + fieldName, field);
            } else {
                fieldMap.put(fieldName, field);
            }
        }

        return fieldMap;
    }

    /**
     * @deprecated As of 2.x.x, replaced by {@link #getAllDeclaredFields(Class)}.
     * Note that getAllDeclaredFields() includes transient fields and synthetic fields
     * (like "$this"). If you need the old behavior, filter the additional fields:
     * <pre>
     * List<Field> fields = getAllDeclaredFields(clazz).stream()
     *     .filter(f -> !Modifier.isTransient(f.getModifiers()))
     *     .filter(f -> !f.getName().startsWith("this$"))
     *     .collect(Collectors.toList());
     * </pre>
     * This method will be removed in 3.0.0.
     */
    @Deprecated
    public static Collection<Field> getDeepDeclaredFields(Class<?> c) {
        Convention.throwIfNull(c, "Class cannot be null");

        FieldsCacheKey key = new FieldsCacheKey(c, true);
        Collection<Field> cached = FIELDS_CACHE.get(key);

        if (cached != null || FIELDS_CACHE.containsKey(key)) {   // Cache null too
            // Filter the cached fields according to the old behavior
            return cached.stream()
                    .filter(f -> !Modifier.isTransient(f.getModifiers()))
                    .filter(f -> !f.getName().startsWith("this$"))
                    .collect(Collectors.toList());
        }

        // If not in cache, getAllDeclaredFields will do the work and cache it
        Collection<Field> allFields = getAllDeclaredFields(c);

        // Filter and return according to old behavior
        return Collections.unmodifiableCollection(allFields.stream()
                .filter(f -> !Modifier.isTransient(f.getModifiers()))
                .filter(f -> !f.getName().startsWith("this$"))
                .collect(Collectors.toList()));
    }

    /**
     * Return all Fields from a class (including inherited), mapped by String field name
     * to java.lang.reflect.Field, excluding synthetic "$this" fields and transient fields.
     * <p>
     * Field mapping rules:
     * <ul>
     *     <li>Simple field names (e.g., "name") are used when no collision exists</li>
     *     <li>Qualified names (e.g., "com.example.Parent.name") are used to resolve collisions</li>
     *     <li>Child class fields take precedence for simple name mapping</li>
     *     <li>Parent class fields use fully qualified names when shadowed</li>
     * </ul>
     * <p>
     * Excluded fields:
     * <ul>
     *     <li>Transient fields</li>
     *     <li>Synthetic "$this" fields for inner classes</li>
     *     <li>Other synthetic fields</li>
     * </ul>
     *
     * @deprecated As of 2.x.x, replaced by {@link #getAllDeclaredFieldsMap(Class)}.
     * If you need a map of fields excluding transient and synthetic fields:
     * <pre>
     * Map<String, Field> fieldMap = getAllDeclaredFields(clazz).stream()
     *     .filter(f -> !Modifier.isTransient(f.getModifiers()))
     *     .filter(f -> !f.getName().startsWith("this$"))
     *     .collect(Collectors.toMap(
     *         field -> {
     *             String name = field.getName();
     *             return seen.add(name) ? name :
     *                 field.getDeclaringClass().getName() + "." + name;
     *         },
     *         field -> field,
     *         (existing, replacement) -> replacement,
     *         LinkedHashMap::new
     *     ));
     * </pre>
     * This method will be removed in 3.0.0 or later.
     *
     * @param c Class whose fields are being fetched
     * @return Map of filtered fields on the Class, keyed by String field name to Field
     * @throws IllegalArgumentException if the class is null
     */
    @Deprecated
    public static Map<String, Field> getDeepDeclaredFieldMap(Class<?> c) {
        Convention.throwIfNull(c, "class cannot be null");

        Map<String, Field> fieldMap = new LinkedHashMap<>();
        Collection<Field> fields = getAllDeclaredFields(c);  // Uses FIELDS_CACHE internally

        for (Field field : fields) {
            // Skip transient and synthetic fields
            if (Modifier.isTransient(field.getModifiers()) ||
                    field.getName().startsWith("this$")) {
                continue;
            }

            String fieldName = field.getName();
            if (fieldMap.containsKey(fieldName)) {   // Can happen when parent and child class both have private field with same name
                fieldMap.put(field.getDeclaringClass().getName() + '.' + fieldName, field);
            } else {
                fieldMap.put(fieldName, field);
            }
        }

        return fieldMap;
    }

    /**
     * @deprecated As of 2.x.x, replaced by {@link #getAllDeclaredFields(Class)}.
     * Note that getAllDeclaredFields() includes transient fields and synthetic fields
     * (like "$this"). If you need the old behavior, filter the additional fields:
     * <pre>
     * List<Field> fields = getAllDeclaredFields(clazz).stream()
     *     .filter(f -> !Modifier.isTransient(f.getModifiers()))
     *     .filter(f -> !f.getName().startsWith("this$"))
     *     .collect(Collectors.toList());
     * </pre>
     * This method will be removed in 3.0.0 or soon after.
     */
    @Deprecated
    public static void getDeclaredFields(Class<?> c, Collection<Field> fields) {
        try {
            Field[] local = c.getDeclaredFields();
            for (Field field : local) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    continue;
                }
                String fieldName = field.getName();
                if ("metaClass".equals(fieldName) && "groovy.lang.MetaClass".equals(field.getType().getName())) {
                    continue;
                }
                if (fieldName.startsWith("this$")) {
                    continue;
                }
                if (Modifier.isPublic(modifiers)) {
                    fields.add(field);
                } else {
                    try {
                        field.setAccessible(true);
                    } catch(Exception ignored) { }
                    fields.add(field);
                }
            }
        } catch (Throwable t) {
            safelyIgnoreException(t);
        }
    }

    /**
     * Simplifies reflective method invocation by wrapping checked exceptions into runtime exceptions.
     * This method provides a cleaner API for reflection-based method calls.
     * <p>
     * Key features:
     * <ul>
     *     <li>Converts checked exceptions to runtime exceptions</li>
     *     <li>Preserves the original exception cause</li>
     *     <li>Provides clear error messages</li>
     *     <li>Handles null checking for both method and instance</li>
     * </ul>
     * <p>
     * Exception handling:
     * <ul>
     *     <li>IllegalAccessException → RuntimeException</li>
     *     <li>InvocationTargetException → RuntimeException (with target exception)</li>
     *     <li>Null method → IllegalArgumentException</li>
     *     <li>Null instance → IllegalArgumentException</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * Method method = ReflectionUtils.getMethod(obj.getClass(), "processData", String.class);
     * Object result = ReflectionUtils.call(obj, method, "input data");
     *
     * // No need for try-catch blocks for checked exceptions
     * // Just handle RuntimeException if needed
     * </pre>
     *
     * @param instance The object instance on which to call the method
     * @param method The Method object representing the method to call
     * @param args The arguments to pass to the method (may be empty)
     * @return The result of the method invocation, or null for void methods
     * @throws IllegalArgumentException if either method or instance is null
     * @throws RuntimeException if the method is inaccessible or throws an exception
     * @see Method#invoke(Object, Object...) For the underlying reflection mechanism
     */
    public static Object call(Object instance, Method method, Object... args) {
        if (method == null) {
            String className = (instance == null) ? "null instance" : instance.getClass().getName();
            throw new IllegalArgumentException("null Method passed to ReflectionUtils.call() on instance of type: " + className);
        }
        if (instance == null) {
            throw new IllegalArgumentException("Cannot call [" + method.getName() + "()] on a null object.");
        }
        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("IllegalAccessException occurred attempting to reflectively call method: " + method.getName() + "()", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Exception thrown inside reflectively called method: " + method.getName() + "()", e.getTargetException());
        }
    }

    /**
     * Provides a simplified, cached reflection API for method invocation using method name.
     * This method combines method lookup and invocation in one step, with results cached
     * for performance.
     * <p>
     * Key features:
     * <ul>
     *     <li>Caches method lookups for improved performance</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Converts checked exceptions to runtime exceptions</li>
     *     <li>Caches both successful lookups and misses</li>
     *     <li>Thread-safe implementation</li>
     * </ul>
     * <p>
     * Limitations:
     * <ul>
     *     <li>Does not distinguish between overloaded methods with same parameter count</li>
     *     <li>Only matches by method name and parameter count</li>
     *     <li>Always selects the first matching method found</li>
     *     <li>Only finds public methods</li>
     * </ul>
     * <p>
     * Exception handling:
     * <ul>
     *     <li>Method not found → IllegalArgumentException</li>
     *     <li>IllegalAccessException → RuntimeException</li>
     *     <li>InvocationTargetException → RuntimeException (with target exception)</li>
     *     <li>Null instance/methodName → IllegalArgumentException</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * // Simple case - no method overloading
     * Object result = ReflectionUtils.call(myObject, "processData", "input");
     *
     * // For overloaded methods, use the more specific call() method:
     * Method specific = ReflectionUtils.getMethod(myObject.getClass(), "processData", String.class);
     * Object result = ReflectionUtils.call(myObject, specific, "input");
     * </pre>
     *
     * @param instance The object instance on which to call the method
     * @param methodName The name of the method to call
     * @param args The arguments to pass to the method (may be empty)
     * @return The result of the method invocation, or null for void methods
     * @throws IllegalArgumentException if the method cannot be found, or if instance/methodName is null
     * @throws RuntimeException if the method is inaccessible or throws an exception
     * @see #call(Object, Method, Object...) For handling overloaded methods
     * @see #getMethod(Class, String, Class...) For explicit method lookup with parameter types
     */
    public static Object call(Object instance, String methodName, Object... args) {
        Method method = getMethod(instance, methodName, args.length);
        try {
            return method.invoke(instance, args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("IllegalAccessException occurred attempting to reflectively call method: " + method.getName() + "()", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Exception thrown inside reflectively called method: " + method.getName() + "()", e.getTargetException());
        }
    }

    /**
     * Retrieves a method of any access level by name and parameter types, with sophisticated
     * caching for optimal performance. This method searches through the class hierarchy and
     * attempts to make non-public methods accessible.
     * <p>
     * Key features:
     * <ul>
     *     <li>Finds methods of any access level (public, protected, package, private)</li>
     *     <li>Includes bridge methods (compiler-generated for generic type erasure)</li>
     *     <li>Includes synthetic methods (compiler-generated for lambdas, inner classes)</li>
     *     <li>Attempts to make non-public methods accessible</li>
     *     <li>Caches both successful lookups and misses</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Thread-safe implementation</li>
     *     <li>Searches entire inheritance hierarchy</li>
     * </ul>
     *
     * @param c The class to search for the method
     * @param methodName The name of the method to find
     * @param types The parameter types for the method (empty array for no-arg methods)
     * @return The Method object if found and made accessible, null if not found
     * @throws IllegalArgumentException if class or methodName is null
     */
    public static Method getMethod(Class<?> c, String methodName, Class<?>... types) {
        Convention.throwIfNull(c, "class cannot be null");
        Convention.throwIfNull(methodName, "methodName cannot be null");

        MethodCacheKey key = new MethodCacheKey(c, methodName, types);

        // Check cache first
        Method cached = METHOD_CACHE.get(key);
        if (cached != null || METHOD_CACHE.containsKey(key)) {
            return cached;
        }

        // Search for method
        Method found = null;
        Class<?> current = c;

        while (current != null && found == null) {
            try {
                found = current.getDeclaredMethod(methodName, types);

                // Attempt to make the method accessible
                if (!found.isAccessible()) {
                    try {
                        found.setAccessible(true);
                    } catch (Exception ignored) {
                        // Return the method even if we can't make it accessible
                    }
                }
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }

        // Cache the result (even if null)
        METHOD_CACHE.put(key, found);
        return found;
    }
    
    /**
     * Retrieves a method by name and argument count from an object instance, using a
     * deterministic selection strategy when multiple matching methods exist.
     * <p>
     * Key features:
     * <ul>
     *     <li>Finds methods of any access level (public, protected, package, private)</li>
     *     <li>Uses deterministic method selection strategy</li>
     *     <li>Attempts to make non-public methods accessible</li>
     *     <li>Caches both successful lookups and misses</li>
     *     <li>Handles different classloaders correctly</li>
     *     <li>Thread-safe implementation</li>
     *     <li>Searches entire inheritance hierarchy</li>
     * </ul>
     * <p>
     * Method selection priority (when multiple methods match):
     * <ul>
     *     <li>1. Non-synthetic/non-bridge methods preferred</li>
     *     <li>2. Higher accessibility preferred (public > protected > package > private)</li>
     *     <li>3. Most specific declaring class in hierarchy preferred</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>
     * // Will select most accessible, non-synthetic method with two parameters
     * Method method = ReflectionUtils.getMethod(myObject, "processData", 2);
     *
     * // For exact method selection, use getMethod with specific types:
     * Method specific = ReflectionUtils.getMethod(
     *     myObject.getClass(),
     *     "processData",
     *     String.class, Integer.class
     * );
     * </pre>
     *
     * @param instance The object instance on which to find the method
     * @param methodName The name of the method to find
     * @param argCount The number of parameters the method should have
     * @return The Method object, made accessible if necessary
     * @throws IllegalArgumentException if the method is not found or if bean/methodName is null
     * @see #getMethod(Class, String, Class...) For finding methods with specific parameter types
     */
    public static Method getMethod(Object instance, String methodName, int argCount) {
        Convention.throwIfNull(instance, "Object instance cannot be null");
        Convention.throwIfNull(methodName, "Method name cannot be null");
        if (argCount < 0) {
            throw new IllegalArgumentException("Argument count cannot be negative");
        }

        Class<?> beanClass = instance.getClass();

        Class<?>[] types = new Class<?>[argCount];
        Arrays.fill(types, Object.class);
        MethodCacheKey key = new MethodCacheKey(beanClass, methodName, types);
        
        // Check cache first
        Method cached = METHOD_CACHE.get(key);
        if (cached != null || METHOD_CACHE.containsKey(key)) {
            return cached;
        }

        // Collect all matching methods
        List<Method> candidates = new ArrayList<>();
        Class<?> current = beanClass;

        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == argCount) {
                    candidates.add(method);
                }
            }
            current = current.getSuperclass();
        }

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Method '%s' with %d parameters not found in %s or its superclasses",
                            methodName, argCount, beanClass.getName())
            );
        }

        // Select the best matching method using our composite strategy
        Method selected = selectMethod(candidates);

        // Attempt to make the method accessible
        if (!selected.isAccessible()) {
            try {
                selected.setAccessible(true);
            } catch (SecurityException ignored) {
                // Return the method even if we can't make it accessible
            }
        }

        // Cache the result
        METHOD_CACHE.put(key, selected);
        return selected;
    }

    /**
     * Selects the most appropriate method using a composite selection strategy.
     * Selection criteria are applied in order of priority.
     */
    private static Method selectMethod(List<Method> candidates) {
        return candidates.stream()
                .min((m1, m2) -> {
                    // First, prefer non-synthetic/non-bridge methods
                    if (m1.isSynthetic() != m2.isSynthetic()) {
                        return m1.isSynthetic() ? 1 : -1;
                    }
                    if (m1.isBridge() != m2.isBridge()) {
                        return m1.isBridge() ? 1 : -1;
                    }

                    // Then, prefer more accessible methods
                    int accessDiff = getAccessibilityScore(m2.getModifiers()) -
                            getAccessibilityScore(m1.getModifiers());
                    if (accessDiff != 0) return accessDiff;

                    // Finally, prefer methods declared in most specific class
                    if (m1.getDeclaringClass().isAssignableFrom(m2.getDeclaringClass())) return 1;
                    if (m2.getDeclaringClass().isAssignableFrom(m1.getDeclaringClass())) return -1;

                    return 0;
                })
                .orElse(candidates.get(0));
    }

    /**
     * Returns an accessibility score for method modifiers.
     * Higher scores indicate greater accessibility.
     */
    private static int getAccessibilityScore(int modifiers) {
        if (Modifier.isPublic(modifiers)) return 4;
        if (Modifier.isProtected(modifiers)) return 3;
        if (Modifier.isPrivate(modifiers)) return 1;
        return 2; // package-private
    }
    
    /**
     * Gets a constructor for the specified class with the given parameter types,
     * regardless of access level (public, protected, private, or package).
     * Both successful lookups and misses are cached for performance.
     * <p>
     * This method:
     * <ul>
     *     <li>Searches for constructors of any access level</li>
     *     <li>Attempts to make non-public constructors accessible</li>
     *     <li>Returns the constructor even if it cannot be made accessible</li>
     *     <li>Caches both found constructors and misses</li>
     *     <li>Handles different classloaders correctly</li>
     * </ul>
     * <p>
     * Note: Finding a constructor does not guarantee that the caller has the necessary
     * permissions to invoke it. Security managers or module restrictions may prevent
     * access even if the constructor is found and marked accessible.
     *
     * @param clazz The class whose constructor is to be retrieved
     * @param parameterTypes The parameter types for the constructor
     * @return The constructor matching the specified parameters, or null if not found
     * @throws IllegalArgumentException if the class is null
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        Convention.throwIfNull(clazz, "class cannot be null");

        ConstructorCacheKey key = new ConstructorCacheKey(clazz, parameterTypes);

        // Check if we already cached this constructor lookup (hit or miss)
        Constructor<?> cached = CONSTRUCTOR_CACHE.get(key);
        if (cached != null || CONSTRUCTOR_CACHE.containsKey(key)) {
            return cached;
        }

        // Not in cache, attempt to find the constructor
        Constructor<?> found = null;
        try {
            found = clazz.getDeclaredConstructor(parameterTypes);

            // Attempt to make it accessible if it's not public
            if (!Modifier.isPublic(found.getModifiers())) {
                try {
                    found.setAccessible(true);
                } catch (SecurityException ignored) {
                    // Return the constructor even if we can't make it accessible
                }
            }
        } catch (NoSuchMethodException ignored) {
            // Constructor not found - will cache null
        }

        // Cache the result (even if null)
        CONSTRUCTOR_CACHE.put(key, found);
        return found;
    }
    
    private static String makeParamKey(Class<?>... parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return "";
        }

        StringBuilder builder = new StringBuilder(":");
        Iterator<Class<?>> i = Arrays.stream(parameterTypes).iterator();
        while (i.hasNext()) {
            Class<?> param = i.next();
            builder.append(param.getName());
            if (i.hasNext()) {
                builder.append('|');
            }
        }
        return builder.toString();
    }

    /**
     * Fetches a no-argument method from the specified class, caching the result for subsequent lookups.
     * This is intended for methods that are not overloaded and require no arguments
     * (e.g., simple getter methods).
     * <p>
     * If the class contains multiple methods with the same name, an
     * {@code IllegalArgumentException} is thrown.
     *
     * @param clazz      the class that contains the desired method
     * @param methodName the name of the no-argument method to locate
     * @return the {@code Method} instance found on the given class
     * @throws IllegalArgumentException if the method is not found or if multiple
     *                                  methods with the same name exist
     */
    public static Method getNonOverloadedMethod(Class<?> clazz, String methodName) {
        if (clazz == null) {
            throw new IllegalArgumentException("Attempted to call getMethod() [" + methodName + "()] on a null class.");
        }
        if (methodName == null) {
            throw new IllegalArgumentException("Attempted to call getMethod() with a null method name on class: " + clazz.getName());
        }

        // Create a cache key for a method with no parameters
        MethodCacheKey key = new MethodCacheKey(clazz, methodName);

        return METHOD_CACHE.computeIfAbsent(key, k -> {
            Method foundMethod = null;
            for (Method m : clazz.getMethods()) {
                if (methodName.equals(m.getName())) {
                    if (foundMethod != null) {
                        throw new IllegalArgumentException("Method: " + methodName + "() called on a class with overloaded methods "
                                + "- ambiguous as to which one to return. Use getMethod() with argument types or argument count.");
                    }
                    foundMethod = m;
                }
            }

            if (foundMethod == null) {
                throw new IllegalArgumentException("Method: " + methodName + "() is not found on class: " + clazz.getName()
                        + ". Perhaps the method is protected, private, or misspelled?");
            }

            return foundMethod;
        });
    }
    
    /**
     * Return the name of the class on the object, or "null" if the object is null.
     * @param o Object to get the class name.
     * @return String name of the class or "null"
     */
    public static String getClassName(Object o) {
        return o == null ? "null" : o.getClass().getName();
    }

    // Constant pool tags
    private final static int CONSTANT_UTF8 = 1;
    private final static int CONSTANT_INTEGER = 3;
    private final static int CONSTANT_FLOAT = 4;
    private final static int CONSTANT_LONG = 5;
    private final static int CONSTANT_DOUBLE = 6;
    private final static int CONSTANT_CLASS = 7;
    private final static int CONSTANT_STRING = 8;
    private final static int CONSTANT_FIELDREF = 9;
    private final static int CONSTANT_METHODREF = 10;
    private final static int CONSTANT_INTERFACEMETHODREF = 11;
    private final static int CONSTANT_NAMEANDTYPE = 12;
    private final static int CONSTANT_METHODHANDLE = 15;
    private final static int CONSTANT_METHODTYPE = 16;
    private final static int CONSTANT_DYNAMIC = 17;
    private final static int CONSTANT_INVOKEDYNAMIC = 18;
    private final static int CONSTANT_MODULE = 19;
    private final static int CONSTANT_PACKAGE = 20;

    /**
     * Given a byte[] of a Java .class file (compiled Java), this code will retrieve the class name from those bytes.
     * This method supports class files up to the latest JDK version.
     *
     * @param byteCode byte[] of compiled byte code
     * @return String fully qualified class name
     * @throws IOException if there are problems reading the byte code
     * @throws IllegalStateException if the class file format is not recognized
     */
    public static String getClassNameFromByteCode(byte[] byteCode) throws IOException {
        try (InputStream is = new ByteArrayInputStream(byteCode);
             DataInputStream dis = new DataInputStream(is)) {

            dis.readInt(); // magic number
            dis.readShort(); // minor version
            dis.readShort(); // major version
            int cpcnt = (dis.readShort() & 0xffff) - 1;
            int[] classes = new int[cpcnt];
            String[] strings = new String[cpcnt];
            int t;

            for (int i = 0; i < cpcnt; i++) {
                t = dis.read(); // tag - 1 byte

                switch (t) {
                    case CONSTANT_UTF8:
                        strings[i] = dis.readUTF();
                        break;

                    case CONSTANT_INTEGER:
                    case CONSTANT_FLOAT:
                        dis.readInt(); // bytes
                        break;

                    case CONSTANT_LONG:
                    case CONSTANT_DOUBLE:
                        dis.readInt(); // high_bytes
                        dis.readInt(); // low_bytes
                        i++; // All 8-byte constants take up two entries
                        break;

                    case CONSTANT_CLASS:
                        classes[i] = dis.readShort() & 0xffff;
                        break;

                    case CONSTANT_STRING:
                        dis.readShort(); // string_index
                        break;

                    case CONSTANT_FIELDREF:
                    case CONSTANT_METHODREF:
                    case CONSTANT_INTERFACEMETHODREF:
                        dis.readShort(); // class_index
                        dis.readShort(); // name_and_type_index
                        break;

                    case CONSTANT_NAMEANDTYPE:
                        dis.readShort(); // name_index
                        dis.readShort(); // descriptor_index
                        break;

                    case CONSTANT_METHODHANDLE:
                        dis.readByte(); // reference_kind
                        dis.readShort(); // reference_index
                        break;

                    case CONSTANT_METHODTYPE:
                        dis.readShort(); // descriptor_index
                        break;

                    case CONSTANT_DYNAMIC:
                    case CONSTANT_INVOKEDYNAMIC:
                        dis.readShort(); // bootstrap_method_attr_index
                        dis.readShort(); // name_and_type_index
                        break;

                    case CONSTANT_MODULE:
                    case CONSTANT_PACKAGE:
                        dis.readShort(); // name_index
                        break;

                    default:
                        throw new IllegalStateException("Unrecognized constant pool tag: " + t);
                }
            }

            dis.readShort(); // access flags
            int thisClassIndex = dis.readShort() & 0xffff; // this_class
            int stringIndex = classes[thisClassIndex - 1];
            String className = strings[stringIndex - 1];
            return className.replace('/', '.');
        }
    }
    
    /**
     * Return a String representation of the class loader, or "bootstrap" if null.
     *
     * @param c The class whose class loader is to be identified.
     * @return A String representing the class loader.
     */
    static String getClassLoaderName(Class<?> c) {
        ClassLoader loader = c.getClassLoader();  // Actual ClassLoader that loaded this specific class
        return loader == null ? "bootstrap" : loader.toString();
    }
}