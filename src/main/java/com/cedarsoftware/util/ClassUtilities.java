package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
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
    private static final Map<Class<?>, Class<?>> wrapperMap = new HashMap<>();
    // Cache for OSGi ClassLoader to avoid repeated reflection calls
    private static volatile ClassLoader osgiClassLoader;
    private static volatile boolean osgiChecked = false;

    static {
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

        wrapperMap.put(int.class, Integer.class);
        wrapperMap.put(Integer.class, int.class);
        wrapperMap.put(char.class, Character.class);
        wrapperMap.put(Character.class, char.class);
        wrapperMap.put(byte.class, Byte.class);
        wrapperMap.put(Byte.class, byte.class);
        wrapperMap.put(short.class, Short.class);
        wrapperMap.put(Short.class, short.class);
        wrapperMap.put(long.class, Long.class);
        wrapperMap.put(Long.class, long.class);
        wrapperMap.put(float.class, Float.class);
        wrapperMap.put(Float.class, float.class);
        wrapperMap.put(double.class, Double.class);
        wrapperMap.put(Double.class, double.class);
        wrapperMap.put(boolean.class, Boolean.class);
        wrapperMap.put(Boolean.class, boolean.class);
    }

    /**
     * Add alias names for classes to allow .forName() to bring the class (.class) back with the alias name.
     * Because the alias to class name mappings are static, it is expected that these are set up during initialization
     * and not changed later.
     *
     * @param clazz Class to add an alias for
     * @param alias String alias name
     */
    public static void addPermanentClassAlias(Class<?> clazz, String alias) {
        nameToClass.put(alias, clazz);
    }

    /**
     * Remove alias name for classes to prevent .forName() from fetching the class with the alias name.
     * Because the alias to class name mappings are static, it is expected that these are set up during initialization
     * and not changed later.
     *
     * @param alias String alias name
     */
    public static void removePermanentClassAlias(String alias) {
        nameToClass.remove(alias);
    }

    /**
     * Computes the inheritance distance between two classes/interfaces/primitive types.
     *
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
        Map<Class<?>, String> visited = new IdentityHashMap<>();
        queue.add(source);
        visited.put(source, null);

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
                    if (!visited.containsKey(current.getSuperclass())) {
                        queue.add(current.getSuperclass());
                        visited.put(current.getSuperclass(), null);
                    }
                }

                // Check interfaces
                for (Class<?> interfaceClass : current.getInterfaces()) {
                    if (interfaceClass.equals(destination)) {
                        return distance;
                    }
                    if (!visited.containsKey(interfaceClass)) {
                        queue.add(interfaceClass);
                        visited.put(interfaceClass, null);
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
    public static boolean isPrimitive(Class<?> c) {
        return c.isPrimitive() || prims.contains(c);
    }

    /**
     * Compare two primitives.
     *
     * @return 0 if they are the same, -1 if not.  Primitive wrapper classes are consider the same as primitive classes.
     */
    private static int comparePrimitiveToWrapper(Class<?> source, Class<?> destination) {
        try {
            return source.getField("TYPE").get(null).equals(destination) ? 0 : -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Given the passed in String class name, return the named JVM class.
     *
     * @param name        String name of a JVM class.
     * @param classLoader ClassLoader to use when searching for JVM classes.
     * @return Class instance of the named JVM class or null if not found.
     */
    public static Class<?> forName(String name, ClassLoader classLoader) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        try {
            return internalClassForName(name, classLoader);
        } catch (SecurityException e) {
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

        if (ClassLoader.class.isAssignableFrom(c) ||
                ProcessBuilder.class.isAssignableFrom(c) ||
                Process.class.isAssignableFrom(c) ||
                Constructor.class.isAssignableFrom(c) ||
                Method.class.isAssignableFrom(c) ||
                Field.class.isAssignableFrom(c)) {
            throw new SecurityException("For security reasons, cannot instantiate: " + c.getName() + " when loading JSON.");
        }

        nameToClass.put(name, c);
        return c;
    }

    /**
     * loadClass() provided by: Thomas Margreiter
     */
    private static Class<?> loadClass(String name, ClassLoader classLoader) throws ClassNotFoundException {
        String className = name;
        boolean arrayType = false;
        Class<?> primitiveArray = null;

        while (className.startsWith("[")) {
            arrayType = true;
            if (className.endsWith(";")) {
                className = className.substring(0, className.length() - 1);
            }
            if (className.equals("[B")) {
                primitiveArray = byte[].class;
            } else if (className.equals("[S")) {
                primitiveArray = short[].class;
            } else if (className.equals("[I")) {
                primitiveArray = int[].class;
            } else if (className.equals("[J")) {
                primitiveArray = long[].class;
            } else if (className.equals("[F")) {
                primitiveArray = float[].class;
            } else if (className.equals("[D")) {
                primitiveArray = double[].class;
            } else if (className.equals("[Z")) {
                primitiveArray = boolean[].class;
            } else if (className.equals("[C")) {
                primitiveArray = char[].class;
            }
            int startpos = className.startsWith("[L") ? 2 : 1;
            className = className.substring(startpos);
        }
        Class<?> currentClass = null;
        if (null == primitiveArray) {
            try {
                currentClass = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                currentClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            }
        }

        if (arrayType) {
            currentClass = (null != primitiveArray) ? primitiveArray : Array.newInstance(currentClass, 0).getClass();
            while (name.startsWith("[[")) {
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

    public static boolean doesOneWrapTheOther(Class<?> x, Class<?> y) {
        return wrapperMap.get(x) == y;
    }

    /**
     * Obtains the appropriate ClassLoader depending on whether the environment is OSGi, JPMS, or neither.
     *
     * @return the appropriate ClassLoader
     */
    public static ClassLoader getClassLoader() {
        // Attempt to detect and handle OSGi environment
        ClassLoader cl = getOSGiClassLoader();
        if (cl != null) {
            return cl;
        }

        // Use the thread's context ClassLoader if available
        cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return cl;
        }

        // Fallback to the ClassLoader that loaded this utility class
        cl = ClassUtilities.class.getClassLoader();
        if (cl != null) {
            return cl;
        }

        // As a last resort, use the system ClassLoader
        return ClassLoader.getSystemClassLoader();
    }

    /**
     * Attempts to retrieve the OSGi Bundle's ClassLoader using FrameworkUtil.
     *
     * @return the OSGi Bundle's ClassLoader if in an OSGi environment; otherwise, null
     */
    private static ClassLoader getOSGiClassLoader() {
        if (osgiChecked) {
            return osgiClassLoader;
        }

        synchronized (ClassUtilities.class) {
            if (osgiChecked) {
                return osgiClassLoader;
            }

            try {
                // Load the FrameworkUtil class from OSGi
                Class<?> frameworkUtilClass = Class.forName("org.osgi.framework.FrameworkUtil");

                // Get the getBundle(Class<?>) method
                Method getBundleMethod = frameworkUtilClass.getMethod("getBundle", Class.class);

                // Invoke FrameworkUtil.getBundle(thisClass) to get the Bundle instance
                Object bundle = getBundleMethod.invoke(null, ClassUtilities.class);

                if (bundle != null) {
                    // Get BundleWiring class
                    Class<?> bundleWiringClass = Class.forName("org.osgi.framework.wiring.BundleWiring");

                    // Get the adapt(Class) method
                    Method adaptMethod = bundle.getClass().getMethod("adapt", Class.class);

                    // method is inside not a public class, so we need to make it accessible
                    adaptMethod.setAccessible(true);

                    // Invoke bundle.adapt(BundleWiring.class) to get the BundleWiring instance
                    Object bundleWiring = adaptMethod.invoke(bundle, bundleWiringClass);

                    if (bundleWiring != null) {
                        // Get the getClassLoader() method from BundleWiring
                        Method getClassLoaderMethod = bundleWiringClass.getMethod("getClassLoader");

                        // Invoke getClassLoader() to obtain the ClassLoader
                        Object classLoader = getClassLoaderMethod.invoke(bundleWiring);

                        if (classLoader instanceof ClassLoader) {
                            osgiClassLoader = (ClassLoader) classLoader;
                        }
                    }
                }
            } catch (Exception e) {
                // OSGi FrameworkUtil is not present; not in an OSGi environment
            } finally {
                osgiChecked = true;
            }
        }

        return osgiClassLoader;
    }
}
