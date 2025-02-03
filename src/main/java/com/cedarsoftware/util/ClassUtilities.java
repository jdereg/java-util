package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import com.cedarsoftware.util.convert.Converter;

import static com.cedarsoftware.util.ExceptionUtilities.safelyIgnoreException;
import static java.lang.reflect.Modifier.isProtected;
import static java.lang.reflect.Modifier.isPublic;

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
    private static volatile boolean useUnsafe = false;
    private static volatile Unsafe unsafe;
    private static final Map<Class<?>, Supplier<Object>> DIRECT_CLASS_MAPPING = new HashMap<>();
    private static final Map<Class<?>, Supplier<Object>> ASSIGNABLE_CLASS_MAPPING = new LinkedHashMap<>();

    static {
        DIRECT_CLASS_MAPPING.put(Date.class, Date::new);
        DIRECT_CLASS_MAPPING.put(StringBuilder.class, StringBuilder::new);
        DIRECT_CLASS_MAPPING.put(StringBuffer.class, StringBuffer::new);
        DIRECT_CLASS_MAPPING.put(Locale.class, Locale::getDefault);
        DIRECT_CLASS_MAPPING.put(TimeZone.class, TimeZone::getDefault);
        DIRECT_CLASS_MAPPING.put(Timestamp.class, () -> new Timestamp(System.currentTimeMillis()));
        DIRECT_CLASS_MAPPING.put(java.sql.Date.class, () -> new java.sql.Date(System.currentTimeMillis()));
        DIRECT_CLASS_MAPPING.put(LocalDate.class, LocalDate::now);
        DIRECT_CLASS_MAPPING.put(LocalDateTime.class, LocalDateTime::now);
        DIRECT_CLASS_MAPPING.put(OffsetDateTime.class, OffsetDateTime::now);
        DIRECT_CLASS_MAPPING.put(ZonedDateTime.class, ZonedDateTime::now);
        DIRECT_CLASS_MAPPING.put(ZoneId.class, ZoneId::systemDefault);
        DIRECT_CLASS_MAPPING.put(AtomicBoolean.class, AtomicBoolean::new);
        DIRECT_CLASS_MAPPING.put(AtomicInteger.class, AtomicInteger::new);
        DIRECT_CLASS_MAPPING.put(AtomicLong.class, AtomicLong::new);
        DIRECT_CLASS_MAPPING.put(URL.class, () -> ExceptionUtilities.safelyIgnoreException(() -> new URL("http://localhost"), null));
        DIRECT_CLASS_MAPPING.put(URI.class, () -> ExceptionUtilities.safelyIgnoreException(() -> new URI("http://localhost"), null));
        DIRECT_CLASS_MAPPING.put(Object.class, Object::new);
        DIRECT_CLASS_MAPPING.put(String.class, () -> "");
        DIRECT_CLASS_MAPPING.put(BigInteger.class, () -> BigInteger.ZERO);
        DIRECT_CLASS_MAPPING.put(BigDecimal.class, () -> BigDecimal.ZERO);
        DIRECT_CLASS_MAPPING.put(Class.class, () -> String.class);
        DIRECT_CLASS_MAPPING.put(Calendar.class, Calendar::getInstance);
        DIRECT_CLASS_MAPPING.put(Instant.class, Instant::now);

        // order is important
        ASSIGNABLE_CLASS_MAPPING.put(EnumSet.class, () -> null);
        ASSIGNABLE_CLASS_MAPPING.put(List.class, ArrayList::new);
        ASSIGNABLE_CLASS_MAPPING.put(NavigableSet.class, TreeSet::new);
        ASSIGNABLE_CLASS_MAPPING.put(SortedSet.class, TreeSet::new);
        ASSIGNABLE_CLASS_MAPPING.put(Set.class, LinkedHashSet::new);
        ASSIGNABLE_CLASS_MAPPING.put(NavigableMap.class, TreeMap::new);
        ASSIGNABLE_CLASS_MAPPING.put(SortedMap.class, TreeMap::new);
        ASSIGNABLE_CLASS_MAPPING.put(Map.class, LinkedHashMap::new);
        ASSIGNABLE_CLASS_MAPPING.put(Collection.class, ArrayList::new);
        ASSIGNABLE_CLASS_MAPPING.put(Calendar.class, Calendar::getInstance);
        ASSIGNABLE_CLASS_MAPPING.put(LinkedHashSet.class, LinkedHashSet::new);

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

    /**
     * Determines if a class is declared as final.
     * <p>
     * Checks if the class has the {@code final} modifier, indicating that it cannot be subclassed.
     * </p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * boolean isFinal = ClassUtilities.isClassFinal(String.class);  // Returns true
     * boolean notFinal = ClassUtilities.isClassFinal(ArrayList.class);  // Returns false
     * }</pre>
     *
     * @param c the class to check, must not be null
     * @return true if the class is final, false otherwise
     * @throws NullPointerException if the input class is null
     */
    public static boolean isClassFinal(Class<?> c) {
        return (c.getModifiers() & Modifier.FINAL) != 0;
    }

    /**
     * Determines if all constructors in a class are declared as private.
     * <p>
     * This method is useful for identifying classes that enforce singleton patterns
     * or utility classes that should not be instantiated.
     * </p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>{@code
     * // Utility class with private constructor
     * public final class Utils {
     *     private Utils() {}
     * }
     *
     * boolean isPrivate = ClassUtilities.areAllConstructorsPrivate(Utils.class);  // Returns true
     * boolean notPrivate = ClassUtilities.areAllConstructorsPrivate(String.class);  // Returns false
     * }</pre>
     *
     * @param c the class to check, must not be null
     * @return true if all constructors in the class are private, false if any constructor is non-private
     * @throws NullPointerException if the input class is null
     */
    public static boolean areAllConstructorsPrivate(Class<?> c) {
        Constructor<?>[] constructors = c.getDeclaredConstructors();

        for (Constructor<?> constructor : constructors) {
            if ((constructor.getModifiers() & Modifier.PRIVATE) == 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Converts a primitive class to its corresponding wrapper class.
     * <p>
     * If the input class is already a non-primitive type, it is returned unchanged.
     * For primitive types, returns the corresponding wrapper class (e.g., {@code int.class} → {@code Integer.class}).
     * </p>
     *
     * <p><strong>Examples:</strong></p>
     * <pre>{@code
     * Class<?> intWrapper = ClassUtilities.toPrimitiveWrapperClass(int.class);     // Returns Integer.class
     * Class<?> boolWrapper = ClassUtilities.toPrimitiveWrapperClass(boolean.class); // Returns Boolean.class
     * Class<?> sameClass = ClassUtilities.toPrimitiveWrapperClass(String.class);    // Returns String.class
     * }</pre>
     *
     * <p><strong>Supported Primitive Types:</strong></p>
     * <ul>
     *   <li>{@code boolean.class} → {@code Boolean.class}</li>
     *   <li>{@code byte.class} → {@code Byte.class}</li>
     *   <li>{@code char.class} → {@code Character.class}</li>
     *   <li>{@code double.class} → {@code Double.class}</li>
     *   <li>{@code float.class} → {@code Float.class}</li>
     *   <li>{@code int.class} → {@code Integer.class}</li>
     *   <li>{@code long.class} → {@code Long.class}</li>
     *   <li>{@code short.class} → {@code Short.class}</li>
     *   <li>{@code void.class} → {@code Void.class}</li>
     * </ul>
     *
     * @param primitiveClass the class to convert, must not be null
     * @return the wrapper class if the input is primitive, otherwise the input class itself
     * @throws NullPointerException if the input class is null
     * @throws IllegalArgumentException if the input class is not a recognized primitive type
     */
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

    /**
     * Determines if one class is the wrapper type of the other.
     * <p>
     * This method checks if there is a primitive-wrapper relationship between two classes.
     * For example, {@code Integer.class} wraps {@code int.class} and vice versa.
     * </p>
     *
     * <p><strong>Examples:</strong></p>
     * <pre>{@code
     * boolean wraps = ClassUtilities.doesOneWrapTheOther(Integer.class, int.class);    // Returns true
     * boolean wraps2 = ClassUtilities.doesOneWrapTheOther(int.class, Integer.class);   // Returns true
     * boolean noWrap = ClassUtilities.doesOneWrapTheOther(Integer.class, long.class);  // Returns false
     * }</pre>
     *
     * <p><strong>Supported Wrapper Pairs:</strong></p>
     * <ul>
     *   <li>{@code Boolean.class} ↔ {@code boolean.class}</li>
     *   <li>{@code Byte.class} ↔ {@code byte.class}</li>
     *   <li>{@code Character.class} ↔ {@code char.class}</li>
     *   <li>{@code Double.class} ↔ {@code double.class}</li>
     *   <li>{@code Float.class} ↔ {@code float.class}</li>
     *   <li>{@code Integer.class} ↔ {@code int.class}</li>
     *   <li>{@code Long.class} ↔ {@code long.class}</li>
     *   <li>{@code Short.class} ↔ {@code short.class}</li>
     * </ul>
     *
     * @param x first class to check
     * @param y second class to check
     * @return true if one class is the wrapper of the other, false otherwise
     * @throws NullPointerException if either input class is null
     */
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

        // First try exact match
        T exactMatch = candidateClasses.get(clazz);
        if (exactMatch != null) {
            return exactMatch;
        }

        // If no exact match, then look for closest inheritance match
        T closest = defaultClass;
        int minDistance = Integer.MAX_VALUE;
        Class<?> closestClass = null;

        for (Map.Entry<Class<?>, T> entry : candidateClasses.entrySet()) {
            Class<?> candidateClass = entry.getKey();
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


    private static void throwIfSecurityConcern(Class<?> securityConcern, Class<?> c) {
        if (securityConcern.isAssignableFrom(c)) {
            throw new IllegalArgumentException("For security reasons, json-io does not allow instantiation of: " + securityConcern.getName());
        }
    }

    private static Object getArgForType(com.cedarsoftware.util.convert.Converter converter, Class<?> argType) {
        if (isPrimitive(argType)) {
            return converter.convert(null, argType);  // Get the defaults (false, 0, 0.0d, etc.)
        }

        Supplier<Object> directClassMapping = DIRECT_CLASS_MAPPING.get(argType);

        if (directClassMapping != null) {
            return directClassMapping.get();
        }

        for (Map.Entry<Class<?>, Supplier<Object>> entry : ASSIGNABLE_CLASS_MAPPING.entrySet()) {
            if (entry.getKey().isAssignableFrom(argType)) {
                return entry.getValue().get();
            }
        }

        if (argType.isArray()) {
            return Array.newInstance(argType.getComponentType(), 0);
        }

        return null;
    }

    /**
     * Build a List the same size of parameterTypes, where the objects in the list are ordered
     * to best match the parameters.  Values from the passed in list are used only once or never.
     * @param values A list of potential arguments.  This list can be smaller than parameterTypes
     *               or larger.
     * @param parameterTypes A list of classes that the values will be matched against.
     * @return List of values that are best ordered to match the passed in parameter types.  This
     * list will be the same length as the passed in parameterTypes list.
     */
    private static List<Object> matchArgumentsToParameters(Converter converter, Collection<Object> values, Parameter[] parameterTypes, boolean useNull) {
        List<Object> answer = new ArrayList<>();
        if (parameterTypes == null || parameterTypes.length == 0) {
            return answer;
        }

        // First pass: Try exact matches and close inheritance matches
        List<Object> copyValues = new ArrayList<>(values);
        boolean[] parameterMatched = new boolean[parameterTypes.length];

        // First try exact matches
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterMatched[i]) {
                continue;
            }

            Class<?> paramType = parameterTypes[i].getType();
            Iterator<Object> valueIter = copyValues.iterator();
            while (valueIter.hasNext()) {
                Object value = valueIter.next();
                if (value != null && value.getClass() == paramType) {
                    answer.add(value);
                    valueIter.remove();
                    parameterMatched[i] = true;
                    break;
                }
            }
        }

        // Second pass: Try inheritance and conversion matches for unmatched parameters
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterMatched[i]) {
                continue;
            }

            Parameter parameter = parameterTypes[i];
            Class<?> paramType = parameter.getType();

            // Try to find best match from remaining values
            Object value = pickBestValue(paramType, copyValues);

            if (value == null) {
                // No matching value found, handle according to useNull flag
                if (useNull) {
                    // For primitives, convert null to default value
                    value = paramType.isPrimitive() ? converter.convert(null, paramType) : null;
                } else {
                    // Try to get a suitable default value
                    value = getArgForType(converter, paramType);

                    // If still null and primitive, convert null
                    if (value == null && paramType.isPrimitive()) {
                        value = converter.convert(null, paramType);
                    }
                }
            } else if (value != null && !paramType.isAssignableFrom(value.getClass())) {
                // Value needs conversion
                try {
                    value = converter.convert(value, paramType);
                } catch (Exception e) {
                    // Conversion failed, fall back to default
                    value = useNull ? null : getArgForType(converter, paramType);
                }
            }

            answer.add(value);
        }

        return answer;
    }

    /**
     * Pick the best value from the list that has the least 'distance' from the passed in Class 'param.'
     * Note: this method has a side effect - it will remove the value that was chosen from the list.
     * Note: If none of the instances in the 'values' list are instances of the 'param' class,
     * then the values list is not modified.
     * @param param Class driving the choice.
     * @param values List of potential argument values to pick from, that would best match the param (class).
     * @return a value from the 'values' list that best matched the 'param,' or null if none of the values
     * were assignable to the 'param'.
     */
    private static Object pickBestValue(Class<?> param, List<Object> values) {
        int[] scores = new int[values.size()];
        int i = 0;

        for (Object value : values) {
            if (value == null) {
                scores[i] = param.isPrimitive() ? Integer.MAX_VALUE : 1000; // Null is okay for objects, bad for primitives
            } else {
                Class<?> valueClass = value.getClass();
                int inheritanceDistance = ClassUtilities.computeInheritanceDistance(valueClass, param);

                if (inheritanceDistance >= 0) {
                    // Direct match or inheritance relationship
                    scores[i] = inheritanceDistance;
                } else if (doesOneWrapTheOther(param, valueClass)) {
                    // Primitive to wrapper match (like int -> Integer)
                    scores[i] = 1;
                } else if (com.cedarsoftware.util.Converter.isSimpleTypeConversionSupported(param, valueClass)) {
                    // Convertible types (like String -> Integer)
                    scores[i] = 100;
                } else {
                    // No match
                    scores[i] = Integer.MAX_VALUE;
                }
            }
            i++;
        }

        int bestIndex = -1;
        int bestScore = Integer.MAX_VALUE;

        for (i = 0; i < scores.length; i++) {
            if (scores[i] < bestScore) {
                bestScore = scores[i];
                bestIndex = i;
            }
        }

        if (bestIndex >= 0 && bestScore < Integer.MAX_VALUE) {
            Object bestValue = values.get(bestIndex);
            values.remove(bestIndex);
            return bestValue;
        }

        return null;
    }

    /**
     * Returns the index of the smallest value in an array.
     * @param array The array to search.
     * @return The index of the smallest value, or -1 if the array is empty.
     */
    public static int indexOfSmallestValue(int[] array) {
        if (array == null || array.length == 0) {
            return -1; // Return -1 for null or empty array.
        }

        int minValue = Integer.MAX_VALUE;
        int minIndex = -1;

        for (int i = 0; i < array.length; i++) {
            if (array[i] < minValue && array[i] > -1) {
                minValue = array[i];
                minIndex = i;
            }
        }

        return minIndex;
    }

    /**
     * Ideal class to hold all constructors for a Class, so that they are sorted in the most
     * appeasing construction order, in terms of public vs protected vs private.  That could be
     * the same, so then it looks at values passed into the arguments, non-null being more
     * valuable than null, as well as number of argument types - more is better than fewer.
     */
    private static class ConstructorWithValues implements Comparable<ConstructorWithValues> {
        final Constructor<?> constructor;
        final Object[] argsNull;
        final Object[] argsNonNull;

        ConstructorWithValues(Constructor<?> constructor, Object[] argsNull, Object[] argsNonNull) {
            this.constructor = constructor;
            this.argsNull = argsNull;
            this.argsNonNull = argsNonNull;
        }

        public int compareTo(ConstructorWithValues other) {
            final int mods = constructor.getModifiers();
            final int otherMods = other.constructor.getModifiers();

            // Rule 1: Visibility: favor public over non-public
            if (!isPublic(mods) && isPublic(otherMods)) {
                return 1;
            } else if (isPublic(mods) && !isPublic(otherMods)) {
                return -1;
            }

            // Rule 2: Visibility: favor protected over private
            if (!isProtected(mods) && isProtected(otherMods)) {
                return 1;
            } else if (isProtected(mods) && !isProtected(otherMods)) {
                return -1;
            }

            // Rule 3: Sort by score of the argsNull list
            long score1 = scoreArgumentValues(argsNull);
            long score2 = scoreArgumentValues(other.argsNull);
            if (score1 < score2) {
                return 1;
            } else if (score1 > score2) {
                return -1;
            }

            // Rule 4: Sort by score of the argsNonNull list
            score1 = scoreArgumentValues(argsNonNull);
            score2 = scoreArgumentValues(other.argsNonNull);
            if (score1 < score2) {
                return 1;
            } else if (score1 > score2) {
                return -1;
            }

            // Rule 5: Favor by Class of parameter type alphabetically.  Mainly, distinguish so that no constructors
            // are dropped from the Set.  Although an "arbitrary" rule, it is consistent.
            String params1 = buildParameterTypeString(constructor);
            String params2 = buildParameterTypeString(other.constructor);
            return params1.compareTo(params2);
        }

        /**
         * The more non-null arguments you have, the higher your score. 100 points for each non-null argument.
         * 50 points for each parameter.  So non-null values are twice as high (100 points versus 50 points) as
         * parameter "slots."
         */
        private long scoreArgumentValues(Object[] args) {
            if (args.length == 0) {
                return 0L;
            }

            int nonNull = 0;

            for (Object arg : args) {
                if (arg != null) {
                    nonNull++;
                }
            }

            return nonNull * 100L + args.length * 50L;
        }

        private String buildParameterTypeString(Constructor<?> constructor) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            StringBuilder s = new StringBuilder();

            for (Class<?> paramType : paramTypes) {
                s.append(paramType.getName()).append(".");
            }
            return s.toString();
        }
    }

    /**
     * Determines if a class is an enum or is related to an enum through inheritance or enclosure.
     * <p>
     * This method searches for an enum class in two ways:
     * <ol>
     *     <li>Checks if the input class or any of its superclasses is an enum</li>
     *     <li>If no enum is found in the inheritance hierarchy, checks if any enclosing (outer) classes are enums</li>
     * </ol>
     * Note: This method specifically excludes java.lang.Enum itself from the results.
     *
     * @param c The class to check (may be null)
     * @return The related enum class if found, null otherwise
     *
     * @see Class#isEnum()
     * @see Class#getEnclosingClass()
     */
    public static Class<?> getClassIfEnum(Class<?> c) {
        if (c == null) {
            return null;
        }

        // Step 1: Traverse up the class hierarchy
        Class<?> current = c;
        while (current != null && current != Object.class) {
            if (current.isEnum() && !Enum.class.equals(current)) {
                return current;
            }
            current = current.getSuperclass();
        }

        // Step 2: Traverse the enclosing classes
        current = c.getEnclosingClass();
        while (current != null) {
            if (current.isEnum() && !Enum.class.equals(current)) {
                return current;
            }
            current = current.getEnclosingClass();
        }

        return null;
    }
    
    /**
     * Create a new instance of the specified class, optionally using provided constructor arguments.
     * <p>
     * This method attempts to instantiate a class using the following strategies in order:
     * <ol>
     *     <li>Using cached constructor information from previous successful instantiations</li>
     *     <li>Matching constructor parameters with provided argument values</li>
     *     <li>Using default values for unmatched parameters</li>
     *     <li>Using unsafe instantiation (if enabled)</li>
     * </ol>
     *
     * <p>Constructor selection prioritizes:
     * <ul>
     *     <li>Public over non-public constructors</li>
     *     <li>Protected over private constructors</li>
     *     <li>Constructors with more non-null argument matches</li>
     *     <li>Constructors with more parameters</li>
     * </ul>
     *
     * @param converter Converter instance used to convert null values to appropriate defaults for primitive types
     * @param c Class to instantiate
     * @param argumentValues Optional collection of values to match to constructor parameters. Can be null or empty.
     * @return A new instance of the specified class
     * @throws IllegalArgumentException if:
     *         <ul>
     *             <li>The class cannot be instantiated</li>
     *             <li>The class is a security-sensitive class (Process, ClassLoader, etc.)</li>
     *             <li>The class is an unknown interface</li>
     *         </ul>
     * @throws IllegalStateException if constructor invocation fails
     *
     * <p><b>Security Note:</b> For security reasons, this method prevents instantiation of:
     * <ul>
     *     <li>ProcessBuilder</li>
     *     <li>Process</li>
     *     <li>ClassLoader</li>
     *     <li>Constructor</li>
     *     <li>Method</li>
     *     <li>Field</li>
     * </ul>
     *
     * <p><b>Usage Example:</b>
     * <pre>{@code
     * // Create instance with no arguments
     * MyClass obj1 = (MyClass) newInstance(converter, MyClass.class, null);
     *
     * // Create instance with constructor arguments
     * List<Object> args = Arrays.asList("arg1", 42);
     * MyClass obj2 = (MyClass) newInstance(converter, MyClass.class, args);
     * }</pre>
     */
    public static Object newInstance(Converter converter, Class<?> c, Collection<?> argumentValues) {
        if (c == null) { throw new IllegalArgumentException("Class cannot be null"); }
        if (c.isInterface()) { throw new IllegalArgumentException("Cannot instantiate interface: " + c.getName()); }
        if (Modifier.isAbstract(c.getModifiers())) { throw new IllegalArgumentException("Cannot instantiate abstract class: " + c.getName()); }

        // Security checks
        Set<Class<?>> securityChecks = CollectionUtilities.setOf(
                ProcessBuilder.class, Process.class, ClassLoader.class,
                Constructor.class, Method.class, Field.class, MethodHandle.class);

        for (Class<?> check : securityChecks) {
            if (check.isAssignableFrom(c)) {
                throw new IllegalArgumentException("For security reasons, json-io does not allow instantiation of: " + check.getName());
            }
        }

        // Additional security check for ProcessImpl
        if ("java.lang.ProcessImpl".equals(c.getName())) {
            throw new IllegalArgumentException("For security reasons, json-io does not allow instantiation of: java.lang.ProcessImpl");
        }

        // Handle inner classes
        if (c.getEnclosingClass() != null && !Modifier.isStatic(c.getModifiers())) {
            try {
                // For inner classes, try to get the enclosing instance
                Object enclosingInstance = newInstance(converter, c.getEnclosingClass(), Collections.emptyList());
                Constructor<?> constructor = ReflectionUtils.getConstructor(c, c.getEnclosingClass());
                if (constructor != null) {
                    trySetAccessible(constructor);
                    return constructor.newInstance(enclosingInstance);
                }
            } catch (Exception ignored) {
                // Fall through to regular instantiation if this fails
            }
        }

        // Normalize arguments
        List<Object> normalizedArgs = argumentValues == null ? new ArrayList<>() : new ArrayList<>(argumentValues);

        // Try constructors in order of parameter count match
        Constructor<?>[] declaredConstructors = ReflectionUtils.getAllConstructors(c);
        Set<ConstructorWithValues> constructorOrder = new TreeSet<>();

        // Prepare all constructors with their argument matches
        for (Constructor<?> constructor : declaredConstructors) {
            Parameter[] parameters = constructor.getParameters();
            List<Object> argsNonNull = matchArgumentsToParameters(converter, new ArrayList<>(normalizedArgs), parameters, false);
            List<Object> argsNull = matchArgumentsToParameters(converter, new ArrayList<>(normalizedArgs), parameters, true);
            constructorOrder.add(new ConstructorWithValues(constructor, argsNull.toArray(), argsNonNull.toArray()));
        }

        // Try constructors in order (based on ConstructorWithValues comparison logic)
        Exception lastException = null;
        for (ConstructorWithValues constructorWithValues : constructorOrder) {
            Constructor<?> constructor = constructorWithValues.constructor;

            // Try with non-null arguments first (prioritize actual values)
            try {
                trySetAccessible(constructor);
                return constructor.newInstance(constructorWithValues.argsNonNull);
            } catch (Exception e1) {
                // If non-null arguments fail, try with null arguments
                try {
                    trySetAccessible(constructor);
                    return constructor.newInstance(constructorWithValues.argsNull);
                } catch (Exception e2) {
                    lastException = e2;
                    // Both attempts failed for this constructor, continue to next constructor
                }
            }
        }

        // Last resort: try unsafe instantiation
        Object instance = tryUnsafeInstantiation(c);
        if (instance != null) {
            return instance;
        }

        // If we get here, we couldn't create the instance
        String msg = "Unable to instantiate: " + c.getName();
        if (lastException != null) {
            msg += " - " + lastException.getMessage();
        }
        throw new IllegalArgumentException(msg);
    }

    static void trySetAccessible(AccessibleObject object) {
        safelyIgnoreException(() -> object.setAccessible(true));
    }

    // Try instantiation via unsafe (if turned on).  It is off by default.  Use
    // ClassUtilities.setUseUnsafe(true) to enable it. This may result in heap-dumps
    // for e.g. ConcurrentHashMap or can cause problems when the class is not initialized,
    // that's why we try ordinary constructors first.
    private static Object tryUnsafeInstantiation(Class<?> c) {
        if (useUnsafe) {
            try {
                Object o = unsafe.allocateInstance(c);
                return o;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Globally turn on (or off) the 'unsafe' option of Class construction.  The unsafe option
     * is used when all constructors have been tried and the Java class could not be instantiated.
     * @param state boolean true = on, false = off.
     */
    public static void setUseUnsafe(boolean state) {
        useUnsafe = state;
        if (state) {
            try {
                unsafe = new Unsafe();
            } catch (InvocationTargetException e) {
                useUnsafe = false;
            }
        }
    }

    /**
     * Returns all equally "lowest" common supertypes (classes or interfaces) shared by both
     * {@code classA} and {@code classB}, excluding any types specified in {@code skipList}.
     * <p>
     * A "lowest" common supertype is defined as any type {@code T} such that:
     * <ul>
     *   <li>{@code T} is a supertype of both {@code classA} and {@code classB} (either
     *       via inheritance or interface implementation), and</li>
     *   <li>{@code T} is not a superclass (or superinterface) of any other common supertype
     *       in the result. In other words, no other returned type is a subtype of {@code T}.</li>
     * </ul>
     *
     * <p>Typically, this method is used to discover the most specific shared classes or
     * interfaces without including certain unwanted types—such as {@code Object.class}
     * or other "marker" interfaces (e.g. {@code Serializable}, {@code Cloneable},
     * {@code Comparable}). If you do not want these in the final result, add them to
     * {@code skipList}.</p>
     *
     * <p>The returned set may contain multiple types if they are "equally specific"
     * and do not extend or implement one another. If the resulting set is empty,
     * then there is no common supertype of {@code classA} and {@code classB} outside
     * of the skipped types.</p>
     *
     * <p>Example (skipping {@code Object.class}):
     * <pre>{@code
     * Set<Class<?>> skip = Collections.singleton(Object.class);
     * Set<Class<?>> supertypes = findLowestCommonSupertypesWithSkip(TreeSet.class, HashSet.class, skip);
     * // supertypes might contain only [Set], since Set is the lowest interface
     * // they both implement, and we skipped Object.
     * }</pre>
     *
     * @param classA   the first class, may be null
     * @param classB   the second class, may be null
     * @param skipList a set of classes or interfaces to exclude from the final result
     *                 (e.g. {@code Object.class}, {@code Serializable.class}, etc.).
     *                 May be empty but not null.
     * @return a {@code Set} of the most specific common supertypes of {@code classA}
     *         and {@code classB}, excluding any in {@code skipList}; if either class
     *         is {@code null} or the entire set is excluded, an empty set is returned
     * @see #findLowestCommonSupertypes(Class, Class)
     * @see #getAllSupertypes(Class)
     */
    public static Set<Class<?>> findLowestCommonSupertypesExcluding(
            Class<?> classA, Class<?> classB,
            Set<Class<?>> skipList)
    {
        if (classA == null || classB == null) {
            return Collections.emptySet();
        }
        if (classA.equals(classB)) {
            // If it's in the skip list, return empty; otherwise return singleton
            return skipList.contains(classA) ? Collections.emptySet()
                    : Collections.singleton(classA);
        }

        // 1) Gather all supertypes of A and B
        Set<Class<?>> allA = getAllSupertypes(classA);
        Set<Class<?>> allB = getAllSupertypes(classB);

        // 2) Intersect
        allA.retainAll(allB);

        // 3) Remove anything in the skip list, including Object if you like
        allA.removeAll(skipList);

        if (allA.isEmpty()) {
            return Collections.emptySet();
        }

        // 4) Sort by descending depth
        List<Class<?>> candidates = new ArrayList<>(allA);
        candidates.sort((x, y) -> {
            int dx = getDepth(x);
            int dy = getDepth(y);
            return Integer.compare(dy, dx); // descending
        });

        // 5) Identify "lowest"
        Set<Class<?>> lowest = new LinkedHashSet<>();
        Set<Class<?>> unionOfAncestors = new HashSet<>();

        for (Class<?> T : candidates) {
            if (unionOfAncestors.contains(T)) {
                // T is an ancestor of something in 'lowest'
                continue;
            }
            // T is indeed a "lowest" so far
            lowest.add(T);

            // Add all T's supertypes to the union set
            Set<Class<?>> ancestorsOfT = getAllSupertypes(T);
            unionOfAncestors.addAll(ancestorsOfT);
        }

        return lowest;
    }

    /**
     * Returns all equally "lowest" common supertypes (classes or interfaces) that
     * both {@code classA} and {@code classB} share, automatically excluding
     * {@code Object.class}.
     * <p>
     * This method is a convenience wrapper around
     * {@link #findLowestCommonSupertypesExcluding(Class, Class, Set)} using a skip list
     * that includes only {@code Object.class}. In other words, if the only common
     * ancestor is {@code Object.class}, this method returns an empty set.
     * </p>
     *
     * <p>Example:
     * <pre>{@code
     * Set<Class<?>> supertypes = findLowestCommonSupertypes(Integer.class, Double.class);
     * // Potentially returns [Number, Comparable, Serializable] because those are
     * // equally specific and not ancestors of one another, ignoring Object.class.
     * }</pre>
     *
     * @param classA the first class, may be null
     * @param classB the second class, may be null
     * @return a {@code Set} of all equally "lowest" common supertypes, excluding
     *         {@code Object.class}; or an empty set if none are found beyond {@code Object}
     *         (or if either input is null)
     * @see #findLowestCommonSupertypesExcluding(Class, Class, Set)
     * @see #getAllSupertypes(Class)
     */
    public static Set<Class<?>> findLowestCommonSupertypes(Class<?> classA, Class<?> classB) {
        return findLowestCommonSupertypesExcluding(classA, classB,
                CollectionUtilities.setOf(Object.class));
    }

    /**
     * Returns the *single* most specific type from findLowestCommonSupertypes(...).
     * If there's more than one, returns any one (or null if none).
     */
    public static Class<?> findLowestCommonSupertype(Class<?> classA, Class<?> classB) {
        Set<Class<?>> all = findLowestCommonSupertypes(classA, classB);
        return all.isEmpty() ? null : all.iterator().next();
    }

    /**
     * Gather all superclasses and all interfaces (recursively) of 'clazz',
     * including clazz itself.
     * <p>
     * BFS or DFS is fine.  Here is a simple BFS approach:
     */
    public static Set<Class<?>> getAllSupertypes(Class<?> clazz) {
        Set<Class<?>> results = new HashSet<>();
        Queue<Class<?>> queue = new ArrayDeque<>();
        queue.add(clazz);
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            if (current != null && results.add(current)) {
                // Add its superclass
                Class<?> sup = current.getSuperclass();
                if (sup != null) {
                    queue.add(sup);
                }
                // Add all interfaces
                for (Class<?> ifc : current.getInterfaces()) {
                    queue.add(ifc);
                }
            }
        }
        return results;
    }

    /**
     * Returns distance of 'clazz' from Object.class in its *class* hierarchy
     * (not counting interfaces).  This is a convenience for sorting by depth.
     */
    private static int getDepth(Class<?> clazz) {
        int depth = 0;
        while (clazz != null) {
            clazz = clazz.getSuperclass();
            depth++;
        }
        return depth;
    }

    // Convenience boolean method
    public static boolean haveCommonAncestor(Class<?> a, Class<?> b) {
        return !findLowestCommonSupertypes(a, b).isEmpty();
    }
}