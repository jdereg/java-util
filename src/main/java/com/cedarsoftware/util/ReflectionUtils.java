package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * <p><strong>ReflectionUtils</strong> is a comprehensive utility class designed to simplify and optimize
 * reflective operations in Java. By providing a suite of methods for accessing class fields, methods,
 * constructors, and annotations, this utility ensures both ease of use and enhanced performance
 * through intelligent caching mechanisms.</p>
 *
 * <p>The primary features of {@code ReflectionUtils} include:</p>
 * <ul>
 *     <li><strong>Field Retrieval:</strong>
 *         <ul>
 *             <li><code>getDeclaredFields(Class<?> c)</code>: Retrieves all non-static, non-transient
 *             declared fields of a class, excluding special fields like <code>this$</code> and
 *             <code>metaClass</code>.</li>
 *             <li><code>getDeepDeclaredFields(Class<?> c)</code>: Retrieves all non-static,
 *             non-transient fields of a class and its superclasses, facilitating deep introspection.</li>
 *             <li><code>getDeclaredFields(Class<?> c, Collection&lt;Field&gt; fields)</code>: Adds
 *             all declared fields of a class to a provided collection.</li>
 *             <li><code>getDeepDeclaredFieldMap(Class<?> c)</code>: Provides a map of all fields,
 *             including inherited ones, keyed by field name. In cases of name collisions, the keys are
 *             qualified with the declaring class name.</li>
 *         </ul>
 *     </li>
 *     <li><strong>Method Retrieval:</strong>
 *         <ul>
 *             <li><code>getMethod(Class<?> c, String methodName, Class<?>... types)</code>: Fetches
 *             a public method by name and parameter types, utilizing caching to expedite subsequent
 *             retrievals.</li>
 *             <li><code>getNonOverloadedMethod(Class<?> clazz, String methodName)</code>: Retrieves
 *             a method by name, ensuring that it is not overloaded. Throws an exception if multiple
 *             methods with the same name exist.</li>
 *             <li><code>getMethod(Object bean, String methodName, int argCount)</code>: Fetches a
 *             method based on name and argument count, suitable for scenarios where parameter types
 *             are not distinct.</li>
 *         </ul>
 *     </li>
 *     <li><strong>Constructor Retrieval:</strong>
 *         <ul>
 *             <li><code>getConstructor(Class<?> clazz, Class<?>... parameterTypes)</code>: Obtains
 *             a public constructor based on parameter types, with caching to enhance performance.</li>
 *         </ul>
 *     </li>
 *     <li><strong>Annotation Retrieval:</strong>
 *         <ul>
 *             <li><code>getClassAnnotation(Class<?> classToCheck, Class&lt;T&gt; annoClass)</code>:
 *             Determines if a class or any of its superclasses/interfaces is annotated with a
 *             specific annotation.</li>
 *             <li><code>getMethodAnnotation(Method method, Class&lt;T&gt; annoClass)</code>: Checks
 *             whether a method or its counterparts in the inheritance hierarchy possess a particular
 *             annotation.</li>
 *         </ul>
 *     </li>
 *     <li><strong>Method Invocation:</strong>
 *         <ul>
 *             <li><code>call(Object instance, Method method, Object... args)</code>: Facilitates
 *             reflective method invocation without necessitating explicit exception handling for
 *             <code>IllegalAccessException</code> and <code>InvocationTargetException</code>.</li>
 *             <li><code>call(Object instance, String methodName, Object... args)</code>: Enables
 *             one-step reflective method invocation by method name and arguments, leveraging caching
 *             based on method name and argument count.</li>
 *         </ul>
 *     </li>
 *     <li><strong>Class Name Extraction:</strong>
 *         <ul>
 *             <li><code>getClassName(Object o)</code>: Retrieves the fully qualified class name of
 *             an object, returning "null" if the object is <code>null</code>.</li>
 *             <li><code>getClassNameFromByteCode(byte[] byteCode)</code>: Extracts the class name
 *             from a byte array representing compiled Java bytecode.</li>
 *         </ul>
 *     </li>
 * </ul>
 *
 * <p><strong>Key Features and Benefits:</strong></p>
 * <ul>
 *     <li><strong>Performance Optimization:</strong>
 *         Extensive use of caching via thread-safe <code>ConcurrentHashMap</code> ensures that reflective
 *         operations are performed efficiently, minimizing the overhead typically associated with
 *         reflection.</li>
 *     <li><strong>Thread Safety:</strong>
 *         All caching mechanisms are designed to be thread-safe, allowing concurrent access without
 *         compromising data integrity.</li>
 *     <li><strong>Ease of Use:</strong>
 *         Simplifies complex reflective operations through intuitive method signatures and
 *         comprehensive utility methods, reducing boilerplate code for developers.</li>
 *     <li><strong>Comprehensive Coverage:</strong>
 *         Provides a wide range of reflective utilities, covering fields, methods, constructors,
 *         and annotations, catering to diverse introspection needs.</li>
 *     <li><strong>Robust Error Handling:</strong>
 *         Incorporates informative exception messages and handles potential reflection-related
 *         issues gracefully, enhancing reliability and debuggability.</li>
 *     <li><strong>Extensibility:</strong>
 *         Designed with modularity in mind, facilitating easy extension or integration with other
 *         utilities and frameworks.</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Retrieve all declared fields of a class
 * List<Field> fields = ReflectionUtils.getDeclaredFields(MyClass.class);
 *
 * // Retrieve all fields including inherited ones
 * Collection<Field> allFields = ReflectionUtils.getDeepDeclaredFields(MyClass.class);
 *
 * // Invoke a method reflectively without handling exceptions
 * Method method = ReflectionUtils.getMethod(MyClass.class, "compute", int.class, int.class);
 * Object result = ReflectionUtils.call(myClassInstance, method, 5, 10);
 *
 * // Fetch a class-level annotation
 * Deprecated deprecated = ReflectionUtils.getClassAnnotation(MyClass.class, Deprecated.class);
 * if (deprecated != null) {
 *     // Handle deprecated class
 * }
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong>
 * {@code ReflectionUtils} employs thread-safe caching mechanisms, ensuring that all utility methods
 * can be safely used in concurrent environments without additional synchronization.</p>
 *
 * <p><strong>Dependencies:</strong>
 * This utility relies on standard Java libraries and does not require external dependencies,
 * ensuring ease of integration into diverse projects.</p>
 *
 * <p><strong>Limitations:</strong></p>
 * <ul>
 *     <li>Some methods assume that class and method names provided are accurate and may not handle
 *     all edge cases of class loader hierarchies or dynamically generated classes.</li>
 *     <li>While caching significantly improves performance, it may increase memory usage for applications
 *     that introspect a large number of unique classes or methods.</li>
 * </ul>
 *
 * <p><strong>Best Practices:</strong></p>
 * <ul>
 *     <li>Prefer using method signatures that include parameter types to avoid ambiguity with overloaded methods.</li>
 *     <li>Utilize caching-aware methods to leverage performance benefits, especially in performance-critical applications.</li>
 *     <li>Handle returned collections appropriately, considering their immutability or thread safety as documented.</li>
 * </ul>
 *
 * <p><strong>License:</strong>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <br><br>
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">http://www.apache.org/licenses/LICENSE-2.0</a>
 * <br><br>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 * </p>
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
    /**
     * Keyed by MethodCacheKey (class, methodName, paramTypes), so that distinct
     * ClassLoaders or param-type arrays produce different cache entries.
     */
    private static volatile Map<MethodCacheKey, Method> METHOD_CACHE = new LRUCache<>(CACHE_SIZE);
    private static volatile Map<String, Constructor<?>> CONSTRUCTOR_CACHE = new LRUCache<>(CACHE_SIZE);

    // Cache for class-level annotation lookups
    private static volatile Map<ClassAnnotationKey, Object> CLASS_ANNOTATION_CACHE = new LRUCache<>(CACHE_SIZE);

    // Cache for method-level annotation lookups
    private static volatile Map<MethodAnnotationKey, Object> METHOD_ANNOTATION_CACHE = new LRUCache<>(CACHE_SIZE);

    // Unified Fields Cache: Keyed by (Class, isDeep)
    private static volatile Map<ClassFieldsCacheKey, Collection<Field>> FIELDS_CACHE = new LRUCache<>(CACHE_SIZE);

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
    public static void setConstructorCache(Map<String, Constructor<?>> cache) {
        CONSTRUCTOR_CACHE = cache;
    }

    /**
     * Sets a custom cache implementation for class-level annotation lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing class annotation lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setClassAnnotationCache(Map<Object, Method> cache) {
        CLASS_ANNOTATION_CACHE = (Map) cache;
    }

    /**
     * Sets a custom cache implementation for method-level annotation lookups.
     * <p>
     * This method allows switching out the default LRUCache implementation with a custom
     * cache implementation. The provided cache must be thread-safe and should implement
     * the Map interface. This method is typically called once during application initialization.
     * </p>
     *
     * @param cache The custom cache implementation to use for storing method annotation lookups.
     *             Must be thread-safe and implement Map interface.
     */
    public static void setMethodAnnotationCache(Map<Object, Method> cache) {
        METHOD_ANNOTATION_CACHE = (Map) cache;
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
    
    // Prevent instantiation
    private ReflectionUtils() {
        // private constructor to prevent instantiation
    }

    /**
     * MethodCacheKey uniquely identifies a method by its class, name, and parameter types.
     */
    private static class MethodCacheKey {
        private final Class<?> clazz;
        private final String methodName;
        private final Class<?>[] paramTypes;

        MethodCacheKey(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.paramTypes = (paramTypes == null) ? new Class<?>[0] : paramTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodCacheKey)) return false;
            MethodCacheKey other = (MethodCacheKey) o;
            return (clazz == other.clazz)
                    && Objects.equals(methodName, other.methodName)
                    && Arrays.equals(paramTypes, other.paramTypes);
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(clazz);
            result = 31 * result + Objects.hashCode(methodName);
            result = 31 * result + Arrays.hashCode(paramTypes);
            return result;
        }
    }

    /**
     * ClassAnnotationKey uniquely identifies a class-annotation pair.
     */
    private static final class ClassAnnotationKey {
        private final Class<?> clazz;
        private final Class<? extends Annotation> annoClass;

        private ClassAnnotationKey(Class<?> clazz, Class<? extends Annotation> annoClass) {
            this.clazz = clazz;
            this.annoClass = annoClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClassAnnotationKey)) return false;
            ClassAnnotationKey other = (ClassAnnotationKey) o;
            return (clazz == other.clazz) && (annoClass == other.annoClass);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(clazz) * 31 + System.identityHashCode(annoClass);
        }
    }

    /**
     * MethodAnnotationKey uniquely identifies a method-annotation pair.
     */
    private static final class MethodAnnotationKey {
        private final Method method;
        private final Class<? extends Annotation> annoClass;

        private MethodAnnotationKey(Method method, Class<? extends Annotation> annoClass) {
            this.method = method;
            this.annoClass = annoClass;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MethodAnnotationKey)) return false;
            MethodAnnotationKey other = (MethodAnnotationKey) o;
            return this.method.equals(other.method) && (this.annoClass == other.annoClass);
        }

        @Override
        public int hashCode() {
            return method.hashCode() * 31 + System.identityHashCode(annoClass);
        }
    }

    /**
     * FieldsCacheKey uniquely identifies a field retrieval request by class and depth.
     */
    private static final class ClassFieldsCacheKey {
        private final Class<?> clazz;
        private final boolean deep;

        ClassFieldsCacheKey(Class<?> clazz, boolean deep) {
            this.clazz = clazz;
            this.deep = deep;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ClassFieldsCacheKey)) return false;
            ClassFieldsCacheKey other = (ClassFieldsCacheKey) o;
            return (clazz == other.clazz) && (deep == other.deep);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(clazz) * 31 + (deep ? 1 : 0);
        }
    }

    /**
     * Unified internal method to retrieve declared fields, with caching.
     *
     * @param c    The class to retrieve fields from.
     * @param deep If true, include fields from superclasses; otherwise, only declared fields.
     * @return A collection of Fields as per the 'deep' parameter.
     */
    private static Collection<Field> getAllDeclaredFieldsInternal(Class<?> c, boolean deep) {
        ClassFieldsCacheKey key = new ClassFieldsCacheKey(c, deep);
        Collection<Field> cached = FIELDS_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        Collection<Field> fields = new ArrayList<>();
        if (deep) {
            Class<?> current = c;
            while (current != null) {
                gatherDeclaredFields(current, fields);
                current = current.getSuperclass();
            }
        } else {
            gatherDeclaredFields(c, fields);
        }

        // Optionally, make the collection unmodifiable to prevent external modifications
        Collection<Field> unmodifiableFields = Collections.unmodifiableCollection(fields);
        FIELDS_CACHE.put(key, unmodifiableFields);
        return unmodifiableFields;
    }

    /**
     * Helper method used by getAllDeclaredFieldsInternal(...) to gather declared fields from a single class.
     *
     * @param c      The class to gather fields from.
     * @param fields The collection to add the fields to.
     */
    private static void gatherDeclaredFields(Class<?> c, Collection<Field> fields) {
        try {
            Field[] local = c.getDeclaredFields();
            for (Field field : local) {
                int modifiers = field.getModifiers();
                if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
                    // skip static and transient fields
                    continue;
                }
                String fieldName = field.getName();
                if ("metaClass".equals(fieldName) && "groovy.lang.MetaClass".equals(field.getType().getName())) {
                    // skip Groovy metaClass field if present
                    continue;
                }
                if (fieldName.startsWith("this$")) {
                    // Skip field in nested class pointing to enclosing outer class instance
                    continue;
                }
                if (!Modifier.isPublic(modifiers)) {
                    try {
                        field.setAccessible(true);
                    } catch (Exception ignore) {
                        // ignore
                    }
                }
                fields.add(field);
            }
        } catch (Throwable e) {
            ExceptionUtilities.safelyIgnoreException(e);
        }
    }

    /**
     * Determine if the passed in class (classToCheck) has the annotation (annoClass) on itself,
     * any of its super classes, any of its interfaces, or any of its super interfaces.
     * This is an exhaustive check throughout the complete inheritance hierarchy.
     * <p>
     * <strong>Note:</strong> The result of this lookup is cached. Repeated calls for the same
     * {@code (classToCheck, annoClass)} will skip the hierarchy search.
     *
     * @param classToCheck The class on which to search for the annotation.
     * @param annoClass    The specific annotation type to locate.
     * @param <T>          The type of the annotation.
     * @return The annotation instance if found, or null if it is not present.
     */
    public static <T extends Annotation> T getClassAnnotation(final Class<?> classToCheck, final Class<T> annoClass) {
        // First, see if we already have an answer cached (including “no annotation found”)
        ClassAnnotationKey key = new ClassAnnotationKey(classToCheck, annoClass);
        Object cached = CLASS_ANNOTATION_CACHE.get(key);
        if (cached != null) {
            return annoClass.cast(cached);
        }

        // Otherwise, perform the hierarchical search
        final Set<Class<?>> visited = new HashSet<>();
        final LinkedList<Class<?>> stack = new LinkedList<>();
        stack.add(classToCheck);

        T found = null;
        while (!stack.isEmpty()) {
            Class<?> classToChk = stack.pop();
            if (classToChk == null || !visited.add(classToChk)) {
                continue;
            }
            T a = classToChk.getAnnotation(annoClass);
            if (a != null) {
                found = a;
                break;
            }
            stack.push(classToChk.getSuperclass());
            addInterfaces(classToChk, stack);
        }

        // Store the found annotation or sentinel in the cache
        CLASS_ANNOTATION_CACHE.put(key, found);
        return found;
    }

    private static void addInterfaces(final Class<?> classToCheck, final LinkedList<Class<?>> stack) {
        for (Class<?> interFace : classToCheck.getInterfaces()) {
            stack.push(interFace);
        }
    }

    /**
     * Determine if the specified method, or the same method signature on its superclasses/interfaces,
     * has the annotation (annoClass). This is an exhaustive check throughout the complete inheritance
     * hierarchy, searching for the method by name and parameter types.
     * <p>
     * <strong>Note:</strong> The result is cached. Repeated calls for the same {@code (method, annoClass)}
     * will skip the hierarchy walk.
     *
     * @param method    The Method object whose annotation is to be checked.
     * @param annoClass The specific annotation type to locate.
     * @param <T>       The type of the annotation.
     * @return The annotation instance if found, or null if it is not present.
     */
    public static <T extends Annotation> T getMethodAnnotation(final Method method, final Class<T> annoClass) {
        // Check the cache first
        MethodAnnotationKey key = new MethodAnnotationKey(method, annoClass);
        Object cached = METHOD_ANNOTATION_CACHE.get(key);
        if (cached != null) {
            return annoClass.cast(cached);
        }

        // Perform the existing hierarchical search
        final Set<Class<?>> visited = new HashSet<>();
        final LinkedList<Class<?>> stack = new LinkedList<>();
        stack.add(method.getDeclaringClass());

        T found = null;
        while (!stack.isEmpty()) {
            Class<?> classToChk = stack.pop();
            if (classToChk == null || !visited.add(classToChk)) {
                continue;
            }

            // Attempt to find the same method signature on classToChk
            Method m = getMethod(classToChk, method.getName(), method.getParameterTypes());
            if (m != null) {
                T a = m.getAnnotation(annoClass);
                if (a != null) {
                    found = a;
                    break;
                }
            }

            // Move upward in the hierarchy
            stack.push(classToChk.getSuperclass());
            addInterfaces(classToChk, stack);
        }

        // Cache the result
        METHOD_ANNOTATION_CACHE.put(key, found);
        return found;
    }

    /**
     * Fetch a public method reflectively by name with argument types. This method caches the lookup, so that
     * subsequent calls are significantly faster. The method can be on an inherited class of the passed-in [starting]
     * Class.
     *
     * @param c          Class on which method is to be found.
     * @param methodName String name of method to find.
     * @param types      Argument types for the method (null is used for no-argument methods).
     * @return Method located, or null if not found.
     */
    public static Method getMethod(Class<?> c, String methodName, Class<?>... types) {
        try {
            MethodCacheKey key = new MethodCacheKey(c, methodName, types);
            Method method = METHOD_CACHE.computeIfAbsent(key, k -> {
                try {
                    return c.getMethod(methodName, types);
                } catch (NoSuchMethodException | SecurityException e) {
                    return null;
                }
            });
            return method;
        } catch (Exception nse) {
            // Includes NoSuchMethodException, SecurityException, etc.
            return null;
        }
    }

    /**
     * Retrieve the declared fields on a Class, cached for performance. This does not
     * fetch the fields on the Class's superclass, for example. If you need that
     * behavior, use {@code getDeepDeclaredFields()}
     * <p>
     * This method is thread-safe and returns an immutable list of fields.
     *
     * @param c The class whose declared fields are to be retrieved.
     * @return An immutable list of declared fields.
     */
    public static List<Field> getDeclaredFields(final Class<?> c) {
        // Utilize the unified cached utility
        Collection<Field> fields = getAllDeclaredFieldsInternal(c, false);
        // Return as a List for compatibility
        return new ArrayList<>(fields);
    }

    /**
     * Get all non-static, non-transient, fields of the passed-in class and its superclasses, including
     * private fields. Note, the special this$ field is also not returned.
     *
     * <pre>
     * {@code
     * Collection<Field> fields = ReflectionUtils.getDeepDeclaredFields(MyClass.class);
     * for (Field field : fields) {
     *     System.out.println(field.getName());
     * }
     * }
     * </pre>
     *
     * @param c Class instance
     * @return Collection of fields in the passed-in class and its superclasses
     * that would need further processing (reference fields).
     */
    public static Collection<Field> getDeepDeclaredFields(Class<?> c) {
        // Utilize the unified cached utility with deep=true
        return getAllDeclaredFieldsInternal(c, true);
    }

    /**
     * Get all non-static, non-transient, fields of the passed-in class, including
     * private fields. Note, the special this$ field is also not returned.
     *
     * @param c      Class instance
     * @param fields A collection to which discovered declared fields are added
     */
    public static void getDeclaredFields(Class<?> c, Collection<Field> fields) {
        // Utilize the unified cached utility with deep=false and add to provided collection
        Collection<Field> fromCache = getAllDeclaredFieldsInternal(c, false);
        fields.addAll(fromCache);
    }

    /**
     * Return all Fields from a class (including inherited), mapped by
     * String field name to java.lang.reflect.Field.
     *
     * @param c Class whose fields are being fetched.
     * @return Map of all fields on the Class, keyed by String field
     * name to java.lang.reflect.Field. If there are name collisions, the key is
     * qualified with the declaring class name.
     */
    public static Map<String, Field> getDeepDeclaredFieldMap(Class<?> c) {
        // Utilize the unified cached utility with deep=true
        Collection<Field> fields = getAllDeclaredFieldsInternal(c, true);
        Map<String, Field> fieldMap = new HashMap<>();
        for (Field field : fields) {
            String fieldName = field.getName();
            // If there is a name collision, store it with a fully qualified key
            if (fieldMap.containsKey(fieldName)) {
                fieldMap.put(field.getDeclaringClass().getName() + '.' + fieldName, field);
            } else {
                fieldMap.put(fieldName, field);
            }
        }
        return fieldMap;
    }

    /**
     * Make reflective method calls without having to handle two checked exceptions
     * (IllegalAccessException and InvocationTargetException).
     *
     * @param instance Object on which to call method.
     * @param method   Method instance from target object.
     * @param args     Arguments to pass to method.
     * @return Object  Value from reflectively called method.
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
     * Make a reflective method call in one step, caching the method based on name + argCount.
     * <p>
     * <strong>Note:</strong> This approach does not handle overloaded methods that have the same
     * argCount but different types. For fully robust usage, use {@link #call(Object, Method, Object...)}
     * with an explicitly obtained Method.
     *
     * @param instance   Object instance on which to call method.
     * @param methodName String name of method to call.
     * @param args       Arguments to pass.
     * @return Object value returned from the reflectively invoked method.
     * @throws IllegalArgumentException if the method cannot be found or is inaccessible.
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
     * Fetch the named method from the passed-in Object instance, caching by (methodName + argCount).
     * This does NOT handle overloaded methods that differ only by parameter types but share argCount.
     *
     * @param bean       Object on which the named method will be found.
     * @param methodName String name of method to be located.
     * @param argCount   int number of arguments.
     * @throws IllegalArgumentException if the method is not found, or if bean/methodName is null.
     */
    public static Method getMethod(Object bean, String methodName, int argCount) {
        if (bean == null) {
            throw new IllegalArgumentException("Attempted to call getMethod() [" + methodName + "()] on a null instance.");
        }
        if (methodName == null) {
            throw new IllegalArgumentException("Attempted to call getMethod() with a null method name on an instance of: " + bean.getClass().getName());
        }
        Class<?> beanClass = bean.getClass();
        Method method = getMethodWithArgs(beanClass, methodName, argCount);
        if (method == null) {
            throw new IllegalArgumentException("Method: " + methodName + "() is not found on class: " + beanClass.getName()
                    + ". Perhaps the method is protected, private, or misspelled?");
        }

        // Now that we've found the actual param types, store it in the same cache so next time is fast.
        MethodCacheKey key = new MethodCacheKey(beanClass, methodName, method.getParameterTypes());
        Method existing = METHOD_CACHE.putIfAbsent(key, method);
        if (existing != null) {
            method = existing;
        }
        return method;
    }

    /**
     * Reflectively find the requested method on the requested class, only matching on argument count.
     */
    private static Method getMethodWithArgs(Class<?> c, String methodName, int argc) {
        Method[] methods = c.getMethods();
        for (Method m : methods) {
            if (methodName.equals(m.getName()) && m.getParameterTypes().length == argc) {
                return m;
            }
        }
        return null;
    }

    /**
     * Fetch a public constructor reflectively by parameter types. This method caches the lookup, so that
     * subsequent calls are significantly faster. Constructors are uniquely identified by their class and parameter types.
     *
     * @param clazz          Class on which constructor is to be found.
     * @param parameterTypes Argument types for the constructor (null is used for no-argument constructors).
     * @return Constructor located.
     * @throws IllegalArgumentException if the constructor is not found.
     */
    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            // We still store constructors by a string key (unchanged).
            StringBuilder sb = new StringBuilder("CT>");
            sb.append(getClassLoaderName(clazz)).append('.');
            sb.append(clazz.getName());
            sb.append(makeParamKey(parameterTypes));

            String key = sb.toString();
            Constructor<?> constructor = CONSTRUCTOR_CACHE.get(key);
            if (constructor == null) {
                constructor = clazz.getConstructor(parameterTypes);
                Constructor<?> existing = CONSTRUCTOR_CACHE.putIfAbsent(key, constructor);
                if (existing != null) {
                    constructor = existing;
                }
            }
            return constructor;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Attempted to get Constructor that did not exist.", e);
        }
    }

    private static String makeParamKey(Class<?>... parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(":");
        for (int i = 0; i < parameterTypes.length; i++) {
            builder.append(parameterTypes[i].getName());
            if (i < parameterTypes.length - 1) {
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
        Method method = getMethodNoArgs(clazz, methodName);
        if (method == null) {
            throw new IllegalArgumentException("Method: " + methodName + "() is not found on class: " + clazz.getName()
                    + ". Perhaps the method is protected, private, or misspelled?");
        }

        // The found method's actual param types are used for the key (usually zero-length).
        MethodCacheKey key = new MethodCacheKey(clazz, methodName, method.getParameterTypes());
        Method existing = METHOD_CACHE.putIfAbsent(key, method);
        if (existing != null) {
            method = existing;
        }
        return method;
    }

    /**
     * Reflectively find the requested method on the requested class that has no arguments,
     * also ensuring it is not overloaded.
     */
    private static Method getMethodNoArgs(Class<?> c, String methodName) {
        Method[] methods = c.getMethods();
        Method foundMethod = null;
        for (Method m : methods) {
            if (methodName.equals(m.getName())) {
                if (foundMethod != null) {
                    // We’ve already found another method with the same name => overloaded.
                    throw new IllegalArgumentException("Method: " + methodName + "() called on a class with overloaded methods "
                            + "- ambiguous as to which one to return. Use getMethod() with argument types or argument count.");
                }
                foundMethod = m;
            }
        }
        return foundMethod;
    }

    /**
     * Return the name of the class on the object, or "null" if the object is null.
     *
     * @param o The object whose class name is to be retrieved.
     * @return The class name as a String, or "null" if the object is null.
     */
    public static String getClassName(Object o) {
        return (o == null) ? "null" : o.getClass().getName();
    }

    /**
     * Given a byte[] of a Java .class file (compiled Java), this code will retrieve the class name from those bytes.
     *
     * @param byteCode byte[] of compiled byte code.
     * @return String name of class
     * @throws Exception potential IO exceptions can happen
     */
    public static String getClassNameFromByteCode(byte[] byteCode) throws Exception {
        try (InputStream is = new ByteArrayInputStream(byteCode);
             DataInputStream dis = new DataInputStream(is)) {
            dis.readInt();   // magic number
            dis.readShort(); // minor version
            dis.readShort(); // major version
            int cpcnt = (dis.readShort() & 0xffff) - 1;
            int[] classes = new int[cpcnt];
            String[] strings = new String[cpcnt];
            int t;
            for (int i = 0; i < cpcnt; i++) {
                t = dis.read(); // tag - 1 byte
                if (t == 1) // CONSTANT_Utf8
                {
                    strings[i] = dis.readUTF();
                } else if (t == 3 || t == 4) // CONSTANT_Integer || CONSTANT_Float
                {
                    dis.readInt(); // bytes
                } else if (t == 5 || t == 6) // CONSTANT_Long || CONSTANT_Double
                {
                    dis.readInt(); // high_bytes
                    dis.readInt(); // low_bytes
                    i++; // 8-byte constants take up two entries
                } else if (t == 7) // CONSTANT_Class
                {
                    classes[i] = dis.readShort() & 0xffff;
                } else if (t == 8) // CONSTANT_String
                {
                    dis.readShort(); // string_index
                } else if (t == 9 || t == 10 || t == 11)  // CONSTANT_Fieldref || CONSTANT_Methodref || CONSTANT_InterfaceMethodref
                {
                    dis.readShort(); // class_index
                    dis.readShort(); // name_and_type_index
                } else if (t == 12) // CONSTANT_NameAndType
                {
                    dis.readShort(); // name_index
                    dis.readShort(); // descriptor_index
                } else if (t == 15) // CONSTANT_MethodHandle
                {
                    dis.readByte();  // reference_kind
                    dis.readShort(); // reference_index
                } else if (t == 16) // CONSTANT_MethodType
                {
                    dis.readShort(); // descriptor_index
                } else if (t == 17 || t == 18) // CONSTANT_Dynamic || CONSTANT_InvokeDynamic
                {
                    dis.readShort(); // bootstrap_method_attr_index
                    dis.readShort(); // name_and_type_index
                } else if (t == 19 || t == 20) // CONSTANT_Module || CONSTANT_Package
                {
                    dis.readShort(); // name_index
                } else {
                    throw new IllegalStateException("Byte code format exceeds JDK 17 format.");
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
     * Uses ClassUtilities.getClassLoader(c) to be OSGi-friendly.
     *
     * @param c The class whose class loader is to be identified.
     * @return A String representing the class loader.
     */
    static String getClassLoaderName(Class<?> c) {
        ClassLoader loader = ClassUtilities.getClassLoader(c);
        if (loader == null) {
            return "bootstrap";
        }
        // Add a unique suffix to differentiate distinct loader instances
        return loader.toString() + '@' + System.identityHashCode(loader);
    }

    /**
     * Retrieves a method of any access level (public, protected, private, or package-private)
     * from the specified class or its superclass hierarchy, including default methods on interfaces.
     * The result is cached for subsequent lookups.
     * <p>
     * The search order is:
     * 1. Declared methods on the specified class (any access level)
     * 2. Default methods from interfaces implemented by the class
     * 3. Methods from superclass hierarchy (recursively applying steps 1-2)
     *
     * @param clazz          The class to search for the method
     * @param methodName     The name of the method to find
     * @param inherited      Consider inherited (defaults true)
     * @param parameterTypes The parameter types of the method (empty array for no parameters)
     * @return The requested Method object, or null if not found
     * @throws SecurityException if the caller does not have permission to access the method
     */
    /**
     * Retrieves a method of any access level (public, protected, private, or package-private).
     * <p>
     * When inherited=false, only returns methods declared directly on the specified class.
     * When inherited=true, searches the entire class hierarchy including superclasses and interfaces.
     *
     * @param clazz          The class to search for the method
     * @param methodName     The name of the method to find
     * @param inherited      If true, search superclasses and interfaces; if false, only return methods declared on the specified class
     * @param parameterTypes The parameter types of the method (empty array for no parameters)
     * @return The requested Method object, or null if not found
     * @throws SecurityException if the caller does not have permission to access the method
     */
    public static Method getMethodAnyAccess(Class<?> clazz, String methodName, boolean inherited, Class<?>... parameterTypes) {
        if (clazz == null || methodName == null) {
            return null;
        }

        // Check cache first
        MethodCacheKey key = new MethodCacheKey(clazz, methodName, parameterTypes);
        Method method = METHOD_CACHE.get(key);

        if (method != null) {
            // For non-inherited case, verify method is declared on the specified class
            if (!inherited && method.getDeclaringClass() != clazz) {
                method = null;
            }
            return method;
        }

        // First check declared methods on the specified class
        try {
            method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            METHOD_CACHE.put(key, method);
            return method;
        } catch (NoSuchMethodException ignored) {
            // If not inherited, stop here
            if (!inherited) {
                return null;
            }
        }

        // Continue with inherited search if needed
        for (Class<?> iface : clazz.getInterfaces()) {
            try {
                method = iface.getMethod(methodName, parameterTypes);
                if (method.isDefault()) {
                    METHOD_CACHE.put(key, method);
                    return method;
                }
            } catch (NoSuchMethodException ignored) {
                // Continue searching
            }
        }

        // Search superclass hierarchy
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            method = getMethodAnyAccess(superClass, methodName, true, parameterTypes);
            if (method != null) {
                METHOD_CACHE.put(key, method);
                return method;
            }
        }

        // Search implemented interfaces recursively for default methods
        for (Class<?> iface : clazz.getInterfaces()) {
            method = searchInterfaceHierarchy(iface, methodName, parameterTypes);
            if (method != null) {
                METHOD_CACHE.put(key, method);
                return method;
            }
        }

        return null;
    }
    
    /**
     * Helper method to recursively search interface hierarchies for default methods.
     */
    private static Method searchInterfaceHierarchy(Class<?> iface, String methodName, Class<?>... parameterTypes) {
        // Check methods in this interface
        try {
            Method method = iface.getMethod(methodName, parameterTypes);
            if (method.isDefault()) {
                return method;
            }
        } catch (NoSuchMethodException ignored) {
            // Continue searching
        }

        // Search extended interfaces
        for (Class<?> superIface : iface.getInterfaces()) {
            Method method = searchInterfaceHierarchy(superIface, methodName, parameterTypes);
            if (method != null) {
                return method;
            }
        }

        return null;
    }
}