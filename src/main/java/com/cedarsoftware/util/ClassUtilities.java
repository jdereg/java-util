package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Useful utilities for Class work. For example, call computeInheritanceDistance(source, destination)
 * to get the inheritance distance (number of super class steps to make it from source to destination.
 * It will return the distance as an integer.  If there is no inheritance relationship between the two,
 * then -1 is returned.  The primitives and primitive wrappers return 0 distance as if they are the
 * same class.
 * <p>
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
public class ClassUtilities
{
    private static final Set<Class<?>> prims = new HashSet<>();

    private static final Map<Class<?>, Class<?>> primitiveToWrapper = new HashMap<>(20, .8f);
    private static final Map<String, Class<?>> nameToClass = new HashMap<>();

    static
    {
        prims.add(Byte.class);
        prims.add(Short.class);
        prims.add(Integer.class);
        prims.add(Long.class);
        prims.add(Float.class);
        prims.add(Double.class);
        prims.add(Character.class);
        prims.add(Boolean.class);

        nameToClass.put("boolean", Boolean.TYPE);
        nameToClass.put("char", Character.TYPE);
        nameToClass.put("byte", Byte.TYPE);
        nameToClass.put("short", Short.TYPE);
        nameToClass.put("int", Integer.TYPE);
        nameToClass.put("long", Long.TYPE);
        nameToClass.put("float", Float.TYPE);
        nameToClass.put("double", Double.TYPE);
        nameToClass.put("string", String.class);
        nameToClass.put("date", Date.class);
        nameToClass.put("class", Class.class);

        primitiveToWrapper.put(int.class, Integer.class);
        primitiveToWrapper.put(long.class, Long.class);
        primitiveToWrapper.put(double.class, Double.class);
        primitiveToWrapper.put(float.class, Float.class);
        primitiveToWrapper.put(boolean.class, Boolean.class);
        primitiveToWrapper.put(char.class, Character.class);
        primitiveToWrapper.put(byte.class, Byte.class);
        primitiveToWrapper.put(short.class, Short.class);
        primitiveToWrapper.put(void.class, Void.class);

    }

    /**
     * Computes the inheritance distance between two classes/interfaces/primitive types.
     * @param source      The source class, interface, or primitive type.
     * @param destination The destination class, interface, or primitive type.
     * @return The number of steps from the source to the destination, or -1 if no path exists.
     */
    public static int computeInheritanceDistance(Class<?> source, Class<?> destination) {
        if (source == null || destination == null) {
            return -1;
        }
        if (source.equals(destination)) {
            return 0;
        }

        // Check for primitive types
        if (source.isPrimitive()) {
            if (destination.isPrimitive()) {
                // Not equal because source.equals(destination) already chceked.
                return -1;
            }
            if (!isPrimitive(destination)) {
                return -1;
            }
            return comparePrimitiveToWrapper(destination, source);
        }

        if (destination.isPrimitive()) {
            if (!isPrimitive(source)) {
                return -1;
            }
            return comparePrimitiveToWrapper(source, destination);
        }

        Queue<Class<?>> queue = new LinkedList<>();
        Set<Class<?>> visited = new HashSet<>();
        queue.add(source);
        visited.add(source);

        int distance = 0;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            distance++;

            for (int i = 0; i < levelSize; i++) {
                Class<?> current = queue.poll();

                // Check superclass
                if (current.getSuperclass() != null) {
                    if (current.getSuperclass().equals(destination)) {
                        return distance;
                    }
                    if (!visited.contains(current.getSuperclass())) {
                        queue.add(current.getSuperclass());
                        visited.add(current.getSuperclass());
                    }
                }

                // Check interfaces
                for (Class<?> interfaceClass : current.getInterfaces()) {
                    if (interfaceClass.equals(destination)) {
                        return distance;
                    }
                    if (!visited.contains(interfaceClass)) {
                        queue.add(interfaceClass);
                        visited.add(interfaceClass);
                    }
                }
            }
        }

        return -1; // No path found
    }

    /**
     * @param c Class to test
     * @return boolean true if the passed in class is a Java primitive, false otherwise.  The Wrapper classes
     * Integer, Long, Boolean, etc. are considered primitives by this method.
     */
    public static boolean isPrimitive(Class<?> c)
    {
        return c.isPrimitive() || prims.contains(c);
    }

    /**
     * Compare two primitives.
     * @return 0 if they are the same, -1 if not.  Primitive wrapper classes are consider the same as primitive classes.
     */
    private static int comparePrimitiveToWrapper(Class<?> source, Class<?> destination)
    {
        try
        {
            return source.getField("TYPE").get(null).equals(destination) ? 0 : -1;
        }
        catch (Exception e)
        {
            return -1;
        }
    }


    /**
     * Given the passed in String class name, return the named JVM class.
     * @param name String name of a JVM class.
     * @param classLoader ClassLoader to use when searching for JVM classes.
     * @return Class instance of the named JVM class or null if not found.
     */
    public static Class<?> forName(String name, ClassLoader classLoader)
    {
        if (name == null || name.isEmpty()) {
            return null;
        }

        try {
            return internalClassForName(name, classLoader);
        } catch(SecurityException e) {
            throw new IllegalArgumentException("Security exception, classForName() call on: " + name, e);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Used internally to load a class by name, and takes care of caching name mappings for speed.
     *
     * @param name        String name of a JVM class.
     * @param classLoader ClassLoader to use when searching for JVM classes.
     * @return Class instance of the named JVM class
     */
    private static Class<?> internalClassForName(String name, ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> c = nameToClass.get(name);
        if (c != null) {
            return c;
        }
        c = loadClass(name, classLoader);

        SecurityChecker.checkSecurityConstraints(c);

        nameToClass.put(name, c);
        return c;
    }

    /**
     * loadClass() provided by: Thomas Margreiter
     */
    private static Class<?> loadClass(String name, ClassLoader classLoader) throws ClassNotFoundException
    {
        String className = name;
        boolean arrayType = false;
        Class<?> primitiveArray = null;

        while (className.startsWith("["))
        {
            arrayType = true;
            if (className.endsWith(";"))
            {
                className = className.substring(0, className.length() - 1);
            }
            if (className.equals("[B"))
            {
                primitiveArray = byte[].class;
            }
            else if (className.equals("[S"))
            {
                primitiveArray = short[].class;
            }
            else if (className.equals("[I"))
            {
                primitiveArray = int[].class;
            }
            else if (className.equals("[J"))
            {
                primitiveArray = long[].class;
            }
            else if (className.equals("[F"))
            {
                primitiveArray = float[].class;
            }
            else if (className.equals("[D"))
            {
                primitiveArray = double[].class;
            }
            else if (className.equals("[Z"))
            {
                primitiveArray = boolean[].class;
            }
            else if (className.equals("[C"))
            {
                primitiveArray = char[].class;
            }
            int startpos = className.startsWith("[L") ? 2 : 1;
            className = className.substring(startpos);
        }
        Class<?> currentClass = null;
        if (null == primitiveArray)
        {
            try
            {
                currentClass = classLoader.loadClass(className);
            }
            catch (ClassNotFoundException e)
            {
                currentClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            }
        }

        if (arrayType)
        {
            currentClass = (null != primitiveArray) ? primitiveArray : Array.newInstance(currentClass, 0).getClass();
            while (name.startsWith("[["))
            {
                currentClass = Array.newInstance(currentClass, 0).getClass();
                name = name.substring(1);
            }
        }
        return currentClass;
    }

    public static boolean isClassFinal(Class<?> c) {
        return (c.getModifiers() & Modifier.FINAL) != 0;
    }

    public static boolean areAllConstructorsPrivate(Class<?> c) {
        Constructor<?>[] constructors = c.getDeclaredConstructors();

        for (Constructor<?> constructor : constructors) {
            if ((constructor.getModifiers() & Modifier.PRIVATE) == 0) {
                return false;
            }
        }

        return true;
    }

    public static Class<?> toPrimitiveWrapperClass(Class<?> primitiveClass) {
        if (!primitiveClass.isPrimitive()) {
            return primitiveClass;
        }

        Class<?> c = primitiveToWrapper.get(primitiveClass);

        if (c == null) {
            throw new IllegalArgumentException("Passed in class: " + primitiveClass + " is not a primitive class");
        }

        return c;
    }

}
class SecurityChecker {
    public static void checkSecurityConstraints(Class<?> clazz) {
        if (isRestrictedClass(clazz)) {
            throw new SecurityException("For security reasons, cannot instantiate: " + clazz.getName() + " when loading JSON.");
        }
    }

    private static boolean isRestrictedClass(Class<?> clazz) {
        return ClassLoader.class.isAssignableFrom(clazz) ||
                ProcessBuilder.class.isAssignableFrom(clazz) ||
                Process.class.isAssignableFrom(clazz) ||
                Constructor.class.isAssignableFrom(clazz) ||
                Method.class.isAssignableFrom(clazz) ||
                Field.class.isAssignableFrom(clazz);
    }
}
