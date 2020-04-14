package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
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
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class ReflectionUtils
{
    private static final ConcurrentMap<Class<?>, Collection<Field>> FIELD_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Method> METHOD_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Method> METHOD_MAP2 = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Method> METHOD_MAP3 = new ConcurrentHashMap<>();

    private ReflectionUtils()
    {
        super();
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
     * Fetch a public method reflectively by name with argument types.  This method caches the lookup, so that
     * subsequent calls are significantly faster.  The method can be on an inherited class of the passed in [starting]
     * Class.
     * @param c Class on which method is to be found.
     * @param methodName String name of method to find.
     * @param types Argument types for the method (null is used for no argument methods).
     * @return Method located, or null if not found.
     */
    public static Method getMethod(Class<?> c, String methodName, Class<?>...types)
    {
        try
        {
            StringBuilder builder = new StringBuilder(c.getName());
            builder.append('.');
            builder.append(methodName);
            for (Class clz : types)
            {
                builder.append('|');
                builder.append(clz.getName());
            }

            // methodKey is in form ClassName.methodName|arg1.class|arg2.class|...
            String methodKey = builder.toString();
            Method method = METHOD_MAP.get(methodKey);
            if (method == null)
            {
                method = c.getMethod(methodName, types);
                Method other = METHOD_MAP.putIfAbsent(methodKey, method);
                if (other != null)
                {
                    method = other;
                }
            }
            return method;
        }
        catch (Exception nse)
        {
            return null;
        }
    }

    /**
     * Get all non static, non transient, fields of the passed in class, including
     * private fields. Note, the special this$ field is also not returned.  The result
     * is cached in a static ConcurrentHashMap to benefit execution performance.
     * @param c Class instance
     * @return Collection of only the fields in the passed in class
     * that would need further processing (reference fields).  This
     * makes field traversal on a class faster as it does not need to
     * continually process known fields like primitives.
     */
    public static Collection<Field> getDeepDeclaredFields(Class<?> c)
    {
        if (FIELD_MAP.containsKey(c))
        {
            return FIELD_MAP.get(c);
        }
        Collection<Field> fields = new ArrayList<>();
        Class<?> curr = c;

        while (curr != null)
        {
            getDeclaredFields(curr, fields);
            curr = curr.getSuperclass();
        }
        FIELD_MAP.put(c, fields);
        return fields;
    }

    /**
     * Get all non static, non transient, fields of the passed in class, including
     * private fields. Note, the special this$ field is also not returned.  The
     * resulting fields are stored in a Collection.
     * @param c Class instance
     * that would need further processing (reference fields).  This
     * makes field traversal on a class faster as it does not need to
     * continually process known fields like primitives.
     */
    public static void getDeclaredFields(Class<?> c, Collection<Field> fields) {
        try
        {
            Field[] local = c.getDeclaredFields();

            for (Field field : local)
            {
                try
                {
                    field.setAccessible(true);
                }
                catch (Exception ignored) { }

                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) &&
                        !field.getName().startsWith("this$") &&
                        !Modifier.isTransient(modifiers))
                {   // speed up: do not count static fields, do not go back up to enclosing object in nested case, do not consider transients
                    fields.add(field);
                }
            }
        }
        catch (Throwable ignored)
        {
            ExceptionUtilities.safelyIgnoreException(ignored);
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
    public static Object call(Object bean, Method method, Object... args)
    {
        if (method == null)
        {
            String className = bean == null ? "null bean" : bean.getClass().getName();
            throw new IllegalArgumentException("null Method passed to ReflectionUtils.call() on bean of type: " + className);
        }
        if (bean == null)
        {
            throw new IllegalArgumentException("Cannot call [" + method.getName() + "()] on a null object.");
        }
        try
        {
            return method.invoke(bean, args);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("IllegalAccessException occurred attempting to reflectively call method: " + method.getName() + "()", e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException("Exception thrown inside reflectively called method: " + method.getName() + "()", e.getTargetException());
        }
    }

    /**
     * Make a reflective method call in one step.  This approach does not support calling two different methods with
     * the same argument count, since it caches methods internally by "className.methodName|argCount".  For example,
     * if you had a class with two methods, foo(int, String) and foo(String, String), you cannot use this method.
     * However, this method would support calling foo(int), foo(int, String), foo(int, String, Object), etc.
     * Internally, it is caching the reflective method lookups as mentioned earlier for speed, using argument count
     * as part of the key (not all argument types).
     *
     * Ideally, use the call(Object, Method, Object...args) method when possible, as it will support any method, and
     * also provides caching.  There are times, however, when all that is passed in (REST APIs) is argument values,
     * and if some of those are null, you may have an ambiguous targeted method.  With this approach, you can still
     * call these methods, assuming the methods are not overloaded with the same number of arguments and differing
     * types.
     * 
     * @param bean Object instance on which to call method.
     * @param methodName String name of method to call.
     * @param args Arguments to pass.
     * @return Object value returned from the reflectively invoked method.
     */
    public static Object call(Object bean, String methodName, Object... args)
    {
        Method method = getMethod(bean, methodName, args.length);
        try
        {
            return method.invoke(bean, args);
        }
        catch (IllegalAccessException e)
        {
            throw new RuntimeException("IllegalAccessException occurred attempting to reflectively call method: " + method.getName() + "()", e);
        }
        catch (InvocationTargetException e)
        {
            throw new RuntimeException("Exception thrown inside reflectively called method: " + method.getName() + "()", e.getTargetException());
        }
    }

    /**
     * Fetch the named method from the passed in Object instance. This method caches found methods, so it should be used
     * instead of reflectively searching for the method every time.  Ideally, use the other getMethod() API that
     * takes an additional argument, Class[] of argument types (most desirable). This is to better support overloaded
     * methods.  Sometimes, you only have the argument values, and if they can be null, you cannot call the getMethod()
     * API that take argument Class types.
     * @param bean Object on which the named method will be found.
     * @param methodName String name of method to be located on the controller.
     * @param argCount int number of arguments.  This is used as part of the cache key to allow for
     * duplicate method names as long as the argument list length is different.
     * @throws IllegalArgumentException
     */
    public static Method getMethod(Object bean, String methodName, int argCount)
    {
        if (bean == null)
        {
            throw new IllegalArgumentException("Attempted to call getMethod() [" + methodName + "()] on a null instance.");
        }
        if (methodName == null)
        {
            throw new IllegalArgumentException("Attempted to call getMethod() with a null method name on an instance of: " + bean.getClass().getName());
        }
        Class beanClass = bean.getClass();
        StringBuilder builder = new StringBuilder(beanClass.getName());
        builder.append('.');
        builder.append(methodName);
        builder.append('|');
        builder.append(argCount);
        String methodKey = builder.toString();
        Method method = METHOD_MAP2.get(methodKey);
        if (method == null)
        {
            method = getMethodWithArgs(beanClass, methodName, argCount);
            if (method == null)
            {
                throw new IllegalArgumentException("Method: " + methodName + "() is not found on class: " + beanClass.getName() + ". Perhaps the method is protected, private, or misspelled?");
            }
            Method other = METHOD_MAP2.putIfAbsent(methodKey, method);
            if (other != null)
            {
                method = other;
            }
        }
        return method;
    }

    /**
     * Reflectively find the requested method on the requested class, only matching on argument count.
     */
    private static Method getMethodWithArgs(Class c, String methodName, int argc)
    {
        Method[] methods = c.getMethods();
        for (Method method : methods)
        {
            if (methodName.equals(method.getName()) && method.getParameterTypes().length == argc)
            {
                return method;
            }
        }
        return null;
    }

    /**
     * Fetch the named method from the passed in Class. This method caches found methods, so it should be used
     * instead of reflectively searching for the method every time.  This method expects the desired method name to
     * not be overloaded.
     * @param clazz Class that containst the desired method.
     * @param methodName String name of method to be located on the controller.
     * @return Method instance found on the passed in class, or an IllegalArgumentException is thrown.
     * @throws IllegalArgumentException
     */
    public static Method getNonOverloadedMethod(Class clazz, String methodName)
    {
        if (clazz == null)
        {
            throw new IllegalArgumentException("Attempted to call getMethod() [" + methodName + "()] on a null class.");
        }
        if (methodName == null)
        {
            throw new IllegalArgumentException("Attempted to call getMethod() with a null method name on class: " + clazz.getName());
        }
        StringBuilder builder = new StringBuilder(clazz.getName());
        builder.append('.');
        builder.append(methodName);
        String methodKey = builder.toString();
        Method method = METHOD_MAP3.get(methodKey);
        if (method == null)
        {
            method = getMethodNoArgs(clazz, methodName);
            if (method == null)
            {
                throw new IllegalArgumentException("Method: " + methodName + "() is not found on class: " + clazz.getName() + ". Perhaps the method is protected, private, or misspelled?");
            }
            Method other = METHOD_MAP3.putIfAbsent(methodKey, method);
            if (other != null)
            {
                method = other;
            }
        }
        return method;
    }

    /**
     * Reflectively find the requested method on the requested class, only matching on argument count.
     */
    private static Method getMethodNoArgs(Class<?> c, String methodName)
    {
        Method[] methods = c.getMethods();
        Method foundMethod = null;
        for (Method method : methods)
        {
            if (methodName.equals(method.getName()))
            {
                if (foundMethod != null)
                {
                    throw new IllegalArgumentException("Method: " + methodName + "() called on a class with overloaded methods - ambiguous as to which one to return.  Use getMethod() that takes argument types or argument count.");
                }
                foundMethod = method;
            }
        }
        return foundMethod;
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
        dis.readLong(); // skip header and class version
        int cpcnt = (dis.readShort() & 0xffff) - 1;
        int[] classes = new int[cpcnt];
        String[] strings = new String[cpcnt];
        for (int i=0; i < cpcnt; i++)
        {
            int t = dis.read();
            if (t == 7)
            {
                classes[i] = dis.readShort() & 0xffff;
            }
            else if (t == 1)
            {
                strings[i] = dis.readUTF();
            }
            else if (t == 5 || t == 6)
            {
                dis.readLong();
                i++;
            }
            else if (t == 8)
            {
                dis.readShort();
            }
            else
            {
                dis.readInt();
            }
        }
        dis.readShort(); // skip access flags
        return strings[classes[(dis.readShort() & 0xffff) - 1] - 1].replace('/', '.');
    }
}
