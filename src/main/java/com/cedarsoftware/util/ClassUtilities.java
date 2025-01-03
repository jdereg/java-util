package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class providing various methods for working with Java {@link Class} objects and related operations.
 * <p>
 * {@code ClassUtilities} includes functionalities such as:
 * </p>
 * <ul>
 *     <li>Determining inheritance distance between two classes or interfaces ({@link #computeInheritanceDistance}).</li>
 *     <li>Checking if a class is primitive or a primitive wrapper ({@link #isPrimitive}).</li>
 *     <li>Converting between primitive types and their wrapper classes ({@link #toPrimitiveWrapperClass}).</li>
 *     <li>Loading resources from the classpath as strings or byte arrays ({@link #loadResourceAsString} and {@link #loadResourceAsBytes}).</li>
 *     <li>Providing custom mappings for class aliases ({@link #addPermanentClassAlias} and {@link #removePermanentClassAlias}).</li>
 *     <li>Identifying whether all constructors in a class are private ({@link #areAllConstructorsPrivate}).</li>
 *     <li>Finding the most specific matching class in an inheritance hierarchy ({@link #findClosest}).</li>
 * </ul>
 *
 * <h2>Inheritance Distance</h2>
 * <p>
 * The {@link #computeInheritanceDistance(Class, Class)} method calculates the number of inheritance steps
 * between two classes or interfaces. If there is no relationship, it returns {@code -1}.
 * </p>
 *
 * <h2>Primitive and Wrapper Handling</h2>
 * <ul>
 *     <li>Supports identification of primitive types and their wrappers.</li>
 *     <li>Handles conversions between primitive types and their wrapper classes.</li>
 *     <li>Considers primitive types and their wrappers interchangeable for certain operations.</li>
 * </ul>
 *
 * <h2>Resource Loading</h2>
 * <p>
 * Includes methods for loading resources from the classpath as strings or byte arrays, throwing appropriate
 * exceptions if the resource cannot be found or read.
 * </p>
 *
 * <h2>OSGi and JPMS ClassLoader Support</h2>
 * <p>
 * Detects and supports environments such as OSGi or JPMS for proper class loading. Uses caching
 * for efficient retrieval of class loaders in these environments.
 * </p>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *     <li>This class is designed to be a static utility class and should not be instantiated.</li>
 *     <li>It uses internal caching for operations like class aliasing and OSGi class loading to optimize performance.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Compute inheritance distance
 * int distance = ClassUtilities.computeInheritanceDistance(ArrayList.class, List.class); // Outputs 1
 *
 * // Check if a class is primitive
 * boolean isPrimitive = ClassUtilities.isPrimitive(int.class); // Outputs true
 *
 * // Load a resource as a string
 * String resourceContent = ClassUtilities.loadResourceAsString("example.txt");
 * }</pre>
 *
 * @see Class
 * @see ClassLoader
 * @see Modifier
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
public class ClassUtilities
{
    private static final Set<Class<?>> prims = new HashSet<>();
    private static final Map<Class<?>, Class<?>> primitiveToWrapper = new HashMap<>(20, .8f);
    private static final Map<String, Class<?>> nameToClass = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> wrapperMap = new HashMap<>();
    // Cache for OSGi ClassLoader to avoid repeated reflection calls
    private static final ClassLoader SYSTEM_LOADER = ClassLoader.getSystemClassLoader();
    private static final Map<Class<?>, ClassLoader> osgiClassLoaders = new ConcurrentHashMap<>();
    private static final Set<Class<?>> osgiChecked = Collections.newSetFromMap(new ConcurrentHashMap<>());


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
     * Registers a permanent alias name for a class to support Class.forName() lookups.
     *
     * @param clazz the class to alias
     * @param alias the alternative name for the class
     */
    public static void addPermanentClassAlias(Class<?> clazz, String alias) {
        nameToClass.put(alias, clazz);
    }

    /**
     * Removes a previously registered class alias.
     *
     * @param alias the alias name to remove
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
                // Not equal because source.equals(destination) already checked.
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

        // TODO: This should be in newInstance() call?
        if (ClassLoader.class.isAssignableFrom(c) ||
                ProcessBuilder.class.isAssignableFrom(c) ||
                Process.class.isAssignableFrom(c) ||
                Constructor.class.isAssignableFrom(c) ||
                Method.class.isAssignableFrom(c) ||
                Field.class.isAssignableFrom(c)) {
            throw new SecurityException("For security reasons, cannot instantiate: " + c.getName());
        }

        nameToClass.put(name, c);
        return c;
    }

    /**
     * loadClass() provided by: Thomas Margreiter
     * <p>
     * Loads a class using the specified ClassLoader, with recursive handling for array types
     * and primitive arrays.
     *
     * @param name the fully qualified class name or array type descriptor
     * @param classLoader the ClassLoader to use
     * @return the loaded Class object
     * @throws ClassNotFoundException if the class cannot be found
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
            switch (className) {
                case "[B":
                    primitiveArray = byte[].class;
                    break;
                case "[S":
                    primitiveArray = short[].class;
                    break;
                case "[I":
                    primitiveArray = int[].class;
                    break;
                case "[J":
                    primitiveArray = long[].class;
                    break;
                case "[F":
                    primitiveArray = float[].class;
                    break;
                case "[D":
                    primitiveArray = double[].class;
                    break;
                case "[Z":
                    primitiveArray = boolean[].class;
                    break;
                case "[C":
                    primitiveArray = char[].class;
                    break;
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
        return getClassLoader(ClassUtilities.class);
    }

    /**
     * Obtains the appropriate ClassLoader depending on whether the environment is OSGi, JPMS, or neither.
     *
     * @param anchorClass the class to use as reference for loading
     * @return the appropriate ClassLoader
     */
    public static ClassLoader getClassLoader(final Class<?> anchorClass) {
        if (anchorClass == null) {
            throw new IllegalArgumentException("Anchor class cannot be null");
        }

        checkSecurityAccess();

        // Try OSGi first
        ClassLoader cl = getOSGiClassLoader(anchorClass);
        if (cl != null) {
            return cl;
        }

        // Try context class loader
        cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return cl;
        }

        // Try anchor class loader
        cl = anchorClass.getClassLoader();
        if (cl != null) {
            return cl;
        }

        // Last resort
        return SYSTEM_LOADER;
    }

    /**
     * Checks if the current security manager allows class loader access.
     */
    private static void checkSecurityAccess() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(new RuntimePermission("getClassLoader"));
        }
    }

    /**
     * Attempts to retrieve the OSGi Bundle's ClassLoader.
     *
     * @param classFromBundle the class from which to get the bundle
     * @return the OSGi Bundle's ClassLoader if in an OSGi environment; otherwise, null
     */
    private static ClassLoader getOSGiClassLoader(final Class<?> classFromBundle) {
        if (osgiChecked.contains(classFromBundle)) {
            return osgiClassLoaders.get(classFromBundle);
        }

        synchronized (ClassUtilities.class) {
            if (osgiChecked.contains(classFromBundle)) {
                return osgiClassLoaders.get(classFromBundle);
            }

            ClassLoader loader = getOSGiClassLoader0(classFromBundle);
            if (loader != null) {
                osgiClassLoaders.put(classFromBundle, loader);
            }
            osgiChecked.add(classFromBundle);
            return loader;
        }
    }

    /**
     * Internal method to retrieve the OSGi Bundle's ClassLoader using reflection.
     *
     * @param classFromBundle the class from which to get the bundle
     * @return the OSGi Bundle's ClassLoader if in an OSGi environment; otherwise, null
     */
    private static ClassLoader getOSGiClassLoader0(final Class<?> classFromBundle) {
        try {
            // Load the FrameworkUtil class from OSGi
            Class<?> frameworkUtilClass = Class.forName("org.osgi.framework.FrameworkUtil");

            // Get the getBundle(Class<?>) method
            Method getBundleMethod = frameworkUtilClass.getMethod("getBundle", Class.class);

            // Invoke FrameworkUtil.getBundle(thisClass) to get the Bundle instance
            Object bundle = getBundleMethod.invoke(null, classFromBundle);

            if (bundle != null) {
                // Get BundleWiring class
                Class<?> bundleWiringClass = Class.forName("org.osgi.framework.wiring.BundleWiring");

                // Get the adapt(Class) method
                Method adaptMethod = bundle.getClass().getMethod("adapt", Class.class);

                // Invoke bundle.adapt(BundleWiring.class) to get the BundleWiring instance
                Object bundleWiring = adaptMethod.invoke(bundle, bundleWiringClass);

                if (bundleWiring != null) {
                    // Get the getClassLoader() method from BundleWiring
                    Method getClassLoaderMethod = bundleWiringClass.getMethod("getClassLoader");

                    // Invoke getClassLoader() to obtain the ClassLoader
                    Object classLoader = getClassLoaderMethod.invoke(bundleWiring);

                    if (classLoader instanceof ClassLoader) {
                        return (ClassLoader) classLoader;
                    }
                }
            }
        } catch (Exception e) {
            // OSGi environment not detected or error occurred
            // Silently ignore as this is expected in non-OSGi environments
        }

        return null;
    }
    
    /**
     * Finds the closest matching class in an inheritance hierarchy from a map of candidate classes.
     * <p>
     * This method searches through a map of candidate classes to find the one that is most closely
     * related to the input class in terms of inheritance distance. The search prioritizes:
     * <ul>
     *     <li>Exact class match (returns immediately)</li>
     *     <li>Closest superclass/interface in the inheritance hierarchy</li>
     * </ul>
     * <p>
     * This method is typically used for cache misses when looking up class-specific handlers
     * or processors.
     *
     * @param <T> The type of value stored in the candidateClasses map
     * @param clazz The class to find a match for (must not be null)
     * @param candidateClasses Map of candidate classes and their associated values (must not be null)
     * @param defaultClass Default value to return if no suitable match is found
     * @return The value associated with the closest matching class, or defaultClass if no match found
     * @throws NullPointerException if clazz or candidateClasses is null
     *
     * @see ClassUtilities#computeInheritanceDistance(Class, Class)
     */
    public static <T> T findClosest(Class<?> clazz, Map<Class<?>, T> candidateClasses, T defaultClass) {
        Objects.requireNonNull(clazz, "Class cannot be null");
        Objects.requireNonNull(candidateClasses, "CandidateClasses classes map cannot be null");

        T closest = defaultClass;
        int minDistance = Integer.MAX_VALUE;
        Class<?> closestClass = null;  // Track the actual class for tie-breaking

        for (Map.Entry<Class<?>, T> entry : candidateClasses.entrySet()) {
            Class<?> candidateClass = entry.getKey();
            // Direct match - return immediately
            if (candidateClass == clazz) {
                return entry.getValue();
            }

            int distance = ClassUtilities.computeInheritanceDistance(clazz, candidateClass);
            if (distance != -1 && (distance < minDistance ||
                    (distance == minDistance && shouldPreferNewCandidate(candidateClass, closestClass)))) {
                minDistance = distance;
                closest = entry.getValue();
                closestClass = candidateClass;
            }
        }
        return closest;
    }

    /**
     * Determines if a new candidate class should be preferred over the current closest class when
     * they have equal inheritance distances.
     * <p>
     * The selection logic follows these rules in order:
     * <ol>
     *     <li>If there is no current class (null), the new candidate is preferred</li>
     *     <li>Classes are preferred over interfaces</li>
     *     <li>When both are classes or both are interfaces, the more specific type is preferred</li>
     * </ol>
     *
     * @param newClass the candidate class being evaluated (must not be null)
     * @param currentClass the current closest matching class (may be null)
     * @return true if newClass should be preferred over currentClass, false otherwise
     */
    private static boolean shouldPreferNewCandidate(Class<?> newClass, Class<?> currentClass) {
        if (currentClass == null) return true;
        // Prefer classes to interfaces
        if (newClass.isInterface() != currentClass.isInterface()) {
            return !newClass.isInterface();
        }
        // If both are classes or both are interfaces, prefer the more specific one
        return newClass.isAssignableFrom(currentClass);
    }

    /**
     * Loads resource content as a String.
     * @param resourceName Name of the resource file.
     * @return Content of the resource file as a String.
     */
    public static String loadResourceAsString(String resourceName) {
        byte[] resourceBytes = loadResourceAsBytes(resourceName);
        return new String(resourceBytes, StandardCharsets.UTF_8);
    }

    /**
     * Loads resource content as a byte[].
     * @param resourceName Name of the resource file.
     * @return Content of the resource file as a byte[].
     * @throws IllegalArgumentException if the resource cannot be found
     * @throws UncheckedIOException if there is an error reading the resource
     * @throws NullPointerException if resourceName is null
     */
    public static byte[] loadResourceAsBytes(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName cannot be null");
        try (InputStream inputStream = ClassUtilities.getClassLoader(ClassUtilities.class).getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourceName);
            }
            return readInputStreamFully(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading resource: " + resourceName, e);
        }
    }

    private static final int BUFFER_SIZE = 8192;

    /**
     * Reads an InputStream fully and returns its content as a byte array.
     *
     * @param inputStream InputStream to read.
     * @return Content of the InputStream as byte array.
     * @throws IOException if an I/O error occurs.
     */
    private static byte[] readInputStreamFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(8192);
        byte[] data = new byte[BUFFER_SIZE];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
