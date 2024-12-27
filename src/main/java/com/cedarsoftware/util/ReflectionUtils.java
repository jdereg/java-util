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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    private static final ConcurrentMap<String, Collection<Field>> FIELD_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Constructor<?>> CONSTRUCTORS = new ConcurrentHashMap<>();
    // Cache for method-level annotation lookups
    private static volatile Map<MethodCacheKey, Method> METHOD_CACHE = new LRUCache<>(CACHE_SIZE);
    // Unified Fields Cache: Keyed by (Class, isDeep)
    private static volatile Map<FieldsCacheKey, Collection<Field>> FIELDS_CACHE = new LRUCache<>(CACHE_SIZE);

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
    
    private ReflectionUtils() { }

    /**
     * MethodCacheKey uniquely identifies a method by its class, name, and parameter types.
     */
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
     * FieldsCacheKey uniquely identifies a field retrieval request by classloader, class and depth.
     */
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
    
    /**
     * Determine if the passed in class (classToCheck) has the annotation (annoClass) on itself,
     * any of its super classes, any of it's interfaces, or any of it's super interfaces.
     * This is a exhaustive check throughout the complete inheritance hierarchy.
     * @return the Annotation if found, null otherwise.
     */
    public static <T extends Annotation> T getClassAnnotation(final Class<?> classToCheck, final Class<T> annoClass)
    {
        final Set<Class<?>> visited = new HashSet<>();
        final LinkedList<Class<?>> stack = new LinkedList<>();
        stack.add(classToCheck);

        while (!stack.isEmpty())
        {
            Class<?> classToChk = stack.pop();
            if (classToChk == null || visited.contains(classToChk))
            {
                continue;
            }
            visited.add(classToChk);
            T a = (T) classToChk.getAnnotation(annoClass);
            if (a != null)
            {
                return a;
            }
            stack.push(classToChk.getSuperclass());
            addInterfaces(classToChk, stack);
        }
        return null;
    }

    private static void addInterfaces(final Class<?> classToCheck, final LinkedList<Class<?>> stack)
    {
        for (Class<?> interFace : classToCheck.getInterfaces())
        {
            stack.push(interFace);
        }
    }

    public static <T extends Annotation> T getMethodAnnotation(final Method method, final Class<T> annoClass)
    {
        final Set<Class<?>> visited = new HashSet<>();
        final LinkedList<Class<?>> stack = new LinkedList<>();
        stack.add(method.getDeclaringClass());

        while (!stack.isEmpty())
        {
            Class<?> classToChk = stack.pop();
            if (classToChk == null || visited.contains(classToChk))
            {
                continue;
            }
            visited.add(classToChk);
            Method m = getMethod(classToChk, method.getName(), method.getParameterTypes());
            if (m == null)
            {
                continue;
            }
            T a = m.getAnnotation(annoClass);
            if (a != null)
            {
                return a;
            }
            stack.push(classToChk.getSuperclass());
            addInterfaces(method.getDeclaringClass(), stack);
        }
        return null;
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
     * Retrieves the declared fields of a class, cached for performance. This method:
     * <ul>
     * <li>Returns only fields declared directly on the specified class (not from superclasses)</li>
     * <li>Excludes static fields</li>
     * <li>Excludes internal enum fields ("internal" and "ENUM$VALUES")</li>
     * <li>Excludes enum base class fields ("hash" and "ordinal")</li>
     * <li>Excludes Groovy's metaClass field</li>
     * </ul>
     * Note that the returned fields will include:
     * <ul>
     * <li>Transient fields</li>
     * <li>The synthetic "$this" field for non-static inner classes (reference to enclosing class)</li>
     * <li>Synthetic fields created by the compiler for anonymous classes and lambdas (capturing local
     *     variables, method parameters, etc.)</li>
     * </ul>
     * For fields from the entire class hierarchy, use {@code getDeepDeclaredFields()}.
     * <p>
     * This method is thread-safe and returns an unmodifiable list of fields. Results are
     * cached for performance.
     *
     * @param c the class whose declared fields are to be retrieved
     * @return an unmodifiable list of the class's declared fields
     * @throws IllegalArgumentException if the class is null
     */
    public static List<Field> getDeclaredFields(final Class<?> c) {
        Convention.throwIfNull(c, "class cannot be null");
        FieldsCacheKey key = new FieldsCacheKey(c, false);
        Collection<Field> cached = FIELDS_CACHE.get(key);
        if (cached != null) {
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
     * Returns all fields from a class and its entire inheritance hierarchy (up to Object).
     * This method applies the same field filtering as {@link #getDeclaredFields(Class)},
     * excluding:
     * <ul>
     * <li>Static fields</li>
     * <li>Internal enum fields ("internal" and "ENUM$VALUES")</li>
     * <li>Enum base class fields ("hash" and "ordinal")</li>
     * <li>Groovy's metaClass field</li>
     * </ul>
     * Note that the returned fields will include:
     * <ul>
     * <li>Transient fields</li>
     * <li>The synthetic "$this" field for non-static inner classes (reference to enclosing class)</li>
     * <li>Synthetic fields created by the compiler for anonymous classes and lambdas (capturing local
     *     variables, method parameters, etc.)</li>
     * </ul>
     * <p>
     * This method is thread-safe and returns an unmodifiable list of fields. Results are
     * cached for performance.
     *
     * @param c the class whose field hierarchy is to be retrieved
     * @return an unmodifiable list of all fields in the class hierarchy
     * @throws IllegalArgumentException if the class is null
     */
    public static List<Field> getAllDeclaredFields(final Class<?> c) {
        Convention.throwIfNull(c, "class cannot be null");
        FieldsCacheKey key = new FieldsCacheKey(c, true);
        Collection<Field> cached = FIELDS_CACHE.get(key);
        if (cached != null) {
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
        String key = getClassLoaderName(c) + '.' + c.getName();
        Collection<Field> fields = FIELD_MAP.get(key);
        if (fields != null) {
            return fields;
        }
        fields = new ArrayList<>();
        Class<?> curr = c;
        while (curr != null) {
            getDeclaredFields(curr, fields);
            curr = curr.getSuperclass();
        }
        FIELD_MAP.put(key, fields);
        return fields;
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
                    } catch(Exception e) { }
                    fields.add(field);
                }
            }
        } catch (Throwable ignore) {
            ExceptionUtilities.safelyIgnoreException(ignore);
        }
    }
    
    /**
     * Return all Fields from a class (including inherited), mapped by
     * String field name to java.lang.reflect.Field.
     * @param c Class whose fields are being fetched.
     * @return Map of all fields on the Class, keyed by String field
     * name to java.lang.reflect.Field.
     */
    public static Map<String, Field> getDeepDeclaredFieldMap(Class<?> c)
    {
        Map<String, Field> fieldMap = new HashMap<>();
        Collection<Field> fields = getDeepDeclaredFields(c);
        for (Field field : fields)
        {
            String fieldName = field.getName();
            if (fieldMap.containsKey(fieldName))
            {   // Can happen when parent and child class both have private field with same name
                fieldMap.put(field.getDeclaringClass().getName() + '.' + fieldName, field);
            }
            else
            {
                fieldMap.put(fieldName, field);
            }
        }

        return fieldMap;
    }

    /**
     * Make reflective method calls without having to handle two checked exceptions (IllegalAccessException and
     * InvocationTargetException).  These exceptions are caught and rethrown as RuntimeExceptions, with the original
     * exception passed (nested) on.
     * @param bean Object (instance) on which to call method.
     * @param method Method instance from target object [easily obtained by calling ReflectionUtils.getMethod()].
     * @param args Arguments to pass to method.
     * @return Object Value from reflectively called method.
     */
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

    public static Constructor<?> getConstructor(Class<?> clazz, Class<?>... parameterTypes)
    {
        try
        {
            String key = clazz.getName() + makeParamKey(parameterTypes);
            Constructor<?> constructor = CONSTRUCTORS.get(key);
            if (constructor == null)
            {
                constructor = clazz.getConstructor(parameterTypes);
                Constructor<?> constructorRef = CONSTRUCTORS.putIfAbsent(key, constructor);
                if (constructorRef != null)
                {
                    constructor = constructorRef;
                }
            }
            return constructor;
        }
        catch (NoSuchMethodException e)
        {
            throw new IllegalArgumentException("Attempted to get Constructor that did not exist.", e);
        }
    }

    private static String makeParamKey(Class<?>... parameterTypes)
    {
        if (parameterTypes == null || parameterTypes.length == 0)
        {
            return "";
        }

        StringBuilder builder = new StringBuilder(":");
        Iterator<Class<?>> i = Arrays.stream(parameterTypes).iterator();
        while (i.hasNext())
        {
            Class<?> param = i.next();
            builder.append(param.getName());
            if (i.hasNext())
            {
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
    public static String getClassName(Object o)
    {
        return o == null ? "null" : o.getClass().getName();
    }

    /**
     * Given a byte[] of a Java .class file (compiled Java), this code will retrieve the class name from those bytes.
     * @param byteCode byte[] of compiled byte code.
     * @return String name of class
     * @throws Exception potential io exceptions can happen
     */
    public static String getClassNameFromByteCode(byte[] byteCode) throws Exception
    {
        InputStream is = new ByteArrayInputStream(byteCode);
        DataInputStream dis = new DataInputStream(is);
        dis.readInt(); // magic number
        dis.readShort(); // minor version
        dis.readShort(); // major version
        int cpcnt = (dis.readShort() & 0xffff) - 1;
        int[] classes = new int[cpcnt];
        String[] strings = new String[cpcnt];
        int t;
        
        for (int i=0; i < cpcnt; i++)
        {
            t = dis.read(); // tag - 1 byte

            if (t == 1) // CONSTANT_Utf8
            {
                strings[i] = dis.readUTF();
            }
            else if (t == 3 || t == 4) // CONSTANT_Integer || CONSTANT_Float
            {
                dis.readInt(); // bytes
            }
            else if (t == 5 || t == 6) // CONSTANT_Long || CONSTANT_Double
            {
                dis.readInt(); // high_bytes
                dis.readInt(); // low_bytes
                i++; // All 8-byte constants take up two entries in the constant_pool table of the class file.
            }
            else if (t == 7) // CONSTANT_Class
            {
                classes[i] = dis.readShort() & 0xffff;
            }
            else if (t == 8) // CONSTANT_String
            {
                dis.readShort(); // string_index
            }
            else if (t == 9 || t == 10 || t == 11)  // CONSTANT_Fieldref || CONSTANT_Methodref || CONSTANT_InterfaceMethodref
            {
                dis.readShort(); // class_index
                dis.readShort(); // name_and_type_index
            }
            else if (t == 12) // CONSTANT_NameAndType
            {
                dis.readShort(); // name_index
                dis.readShort(); // descriptor_index
            }
            else if (t == 15) // CONSTANT_MethodHandle
            {
                dis.readByte(); // reference_kind
                dis.readShort(); // reference_index
            }
            else if (t == 16) // CONSTANT_MethodType
            {
                dis.readShort(); // descriptor_index
            }
            else if (t == 17 || t == 18) // CONSTANT_Dynamic || CONSTANT_InvokeDynamic
            {
                dis.readShort(); // bootstrap_method_attr_index
                dis.readShort(); // name_and_type_index
            }
            else if (t == 19 || t == 20) // CONSTANT_Module || CONSTANT_Package
            {
                dis.readShort(); // name_index
            }
            else
            {
                throw new IllegalStateException("Byte code format exceeds JDK 17 format.");
            }
        }

        dis.readShort(); // access flags
        int thisClassIndex = dis.readShort() & 0xffff; // this_class
        int stringIndex = classes[thisClassIndex - 1];
        String className = strings[stringIndex - 1];
        return className.replace('/', '.');
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