package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Queue;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Stack;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.cedarsoftware.util.convert.Converter;

import static com.cedarsoftware.util.ExceptionUtilities.safelyIgnoreException;

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
public class ClassUtilities {

    private ClassUtilities() {
    }

    private static final Set<Class<?>> prims = new HashSet<>();
    private static final Map<Class<?>, Class<?>> primitiveToWrapper = new HashMap<>(20, .8f);
    private static final Map<String, Class<?>> nameToClass = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> wrapperMap = new HashMap<>();
    // Cache for OSGi ClassLoader to avoid repeated reflection calls
    private static final ConcurrentHashMapNullSafe<Class<?>, ClassLoader> osgiClassLoaders = new ConcurrentHashMapNullSafe<>();
    private static final ClassLoader SYSTEM_LOADER = ClassLoader.getSystemClassLoader();
    private static volatile boolean useUnsafe = false;
    private static volatile Unsafe unsafe;
    private static final Map<Class<?>, Supplier<Object>> DIRECT_CLASS_MAPPING = new HashMap<>();
    private static final Map<Class<?>, Supplier<Object>> ASSIGNABLE_CLASS_MAPPING = new LinkedHashMap<>();
    private static volatile Map<Class<?>, Set<Class<?>>> SUPER_TYPES_CACHE = new LRUCache<>(500);
    private static volatile Map<Map.Entry<Class<?>, Class<?>>, Integer> CLASS_DISTANCE_CACHE = new LRUCache<>(2000);
    private static final Set<Class<?>> SECURITY_BLOCKED_CLASSES = CollectionUtilities.setOf(
            ProcessBuilder.class, Process.class, ClassLoader.class,
            Constructor.class, Method.class, Field.class, MethodHandle.class);

    // Add a cache for successful constructor selections
    private static final Map<Class<?>, Constructor<?>> SUCCESSFUL_CONSTRUCTOR_CACHE = new LRUCache<>(500);

    static {
        // DIRECT_CLASS_MAPPING for concrete types
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
        DIRECT_CLASS_MAPPING.put(Duration.class, () -> Duration.ofSeconds(10));
        DIRECT_CLASS_MAPPING.put(Period.class, () -> Period.ofDays(0));
        DIRECT_CLASS_MAPPING.put(Year.class, Year::now);
        DIRECT_CLASS_MAPPING.put(YearMonth.class, YearMonth::now);
        DIRECT_CLASS_MAPPING.put(MonthDay.class, MonthDay::now);
        DIRECT_CLASS_MAPPING.put(ZoneOffset.class, () -> ZoneOffset.UTC);
        DIRECT_CLASS_MAPPING.put(OffsetTime.class, OffsetTime::now);
        DIRECT_CLASS_MAPPING.put(LocalTime.class, LocalTime::now);
        DIRECT_CLASS_MAPPING.put(ByteBuffer.class, () -> ByteBuffer.allocate(0));
        DIRECT_CLASS_MAPPING.put(CharBuffer.class, () -> CharBuffer.allocate(0));

        // Collection classes
        DIRECT_CLASS_MAPPING.put(HashSet.class, HashSet::new);
        DIRECT_CLASS_MAPPING.put(TreeSet.class, TreeSet::new);
        DIRECT_CLASS_MAPPING.put(HashMap.class, HashMap::new);
        DIRECT_CLASS_MAPPING.put(TreeMap.class, TreeMap::new);
        DIRECT_CLASS_MAPPING.put(Hashtable.class, Hashtable::new);
        DIRECT_CLASS_MAPPING.put(ArrayList.class, ArrayList::new);
        DIRECT_CLASS_MAPPING.put(LinkedList.class, LinkedList::new);
        DIRECT_CLASS_MAPPING.put(Vector.class, Vector::new);
        DIRECT_CLASS_MAPPING.put(Stack.class, Stack::new);
        DIRECT_CLASS_MAPPING.put(Properties.class, Properties::new);
        DIRECT_CLASS_MAPPING.put(ConcurrentHashMap.class, ConcurrentHashMap::new);
        DIRECT_CLASS_MAPPING.put(LinkedHashMap.class, LinkedHashMap::new);
        DIRECT_CLASS_MAPPING.put(LinkedHashSet.class, LinkedHashSet::new);
        DIRECT_CLASS_MAPPING.put(ArrayDeque.class, ArrayDeque::new);
        DIRECT_CLASS_MAPPING.put(PriorityQueue.class, PriorityQueue::new);

        // Concurrent collections
        DIRECT_CLASS_MAPPING.put(CopyOnWriteArrayList.class, CopyOnWriteArrayList::new);
        DIRECT_CLASS_MAPPING.put(CopyOnWriteArraySet.class, CopyOnWriteArraySet::new);
        DIRECT_CLASS_MAPPING.put(LinkedBlockingQueue.class, LinkedBlockingQueue::new);
        DIRECT_CLASS_MAPPING.put(LinkedBlockingDeque.class, LinkedBlockingDeque::new);
        DIRECT_CLASS_MAPPING.put(ConcurrentSkipListMap.class, ConcurrentSkipListMap::new);
        DIRECT_CLASS_MAPPING.put(ConcurrentSkipListSet.class, ConcurrentSkipListSet::new);

        // Additional Map implementations
        DIRECT_CLASS_MAPPING.put(WeakHashMap.class, WeakHashMap::new);
        DIRECT_CLASS_MAPPING.put(IdentityHashMap.class, IdentityHashMap::new);
        DIRECT_CLASS_MAPPING.put(EnumMap.class, () -> new EnumMap(Object.class));

        // Utility classes
        DIRECT_CLASS_MAPPING.put(UUID.class, UUID::randomUUID);
        DIRECT_CLASS_MAPPING.put(Currency.class, () -> Currency.getInstance(Locale.getDefault()));
        DIRECT_CLASS_MAPPING.put(Pattern.class, () -> Pattern.compile(".*"));
        DIRECT_CLASS_MAPPING.put(BitSet.class, BitSet::new);
        DIRECT_CLASS_MAPPING.put(StringJoiner.class, () -> new StringJoiner(","));

        // Optional types
        DIRECT_CLASS_MAPPING.put(Optional.class, Optional::empty);
        DIRECT_CLASS_MAPPING.put(OptionalInt.class, OptionalInt::empty);
        DIRECT_CLASS_MAPPING.put(OptionalLong.class, OptionalLong::empty);
        DIRECT_CLASS_MAPPING.put(OptionalDouble.class, OptionalDouble::empty);

        // Stream types
        DIRECT_CLASS_MAPPING.put(Stream.class, Stream::empty);
        DIRECT_CLASS_MAPPING.put(IntStream.class, IntStream::empty);
        DIRECT_CLASS_MAPPING.put(LongStream.class, LongStream::empty);
        DIRECT_CLASS_MAPPING.put(DoubleStream.class, DoubleStream::empty);

        // Primitive arrays
        DIRECT_CLASS_MAPPING.put(boolean[].class, () -> new boolean[0]);
        DIRECT_CLASS_MAPPING.put(byte[].class, () -> new byte[0]);
        DIRECT_CLASS_MAPPING.put(short[].class, () -> new short[0]);
        DIRECT_CLASS_MAPPING.put(int[].class, () -> new int[0]);
        DIRECT_CLASS_MAPPING.put(long[].class, () -> new long[0]);
        DIRECT_CLASS_MAPPING.put(float[].class, () -> new float[0]);
        DIRECT_CLASS_MAPPING.put(double[].class, () -> new double[0]);
        DIRECT_CLASS_MAPPING.put(char[].class, () -> new char[0]);
        DIRECT_CLASS_MAPPING.put(Object[].class, () -> new Object[0]);

        // Boxed primitive arrays
        DIRECT_CLASS_MAPPING.put(Boolean[].class, () -> new Boolean[0]);
        DIRECT_CLASS_MAPPING.put(Byte[].class, () -> new Byte[0]);
        DIRECT_CLASS_MAPPING.put(Short[].class, () -> new Short[0]);
        DIRECT_CLASS_MAPPING.put(Integer[].class, () -> new Integer[0]);
        DIRECT_CLASS_MAPPING.put(Long[].class, () -> new Long[0]);
        DIRECT_CLASS_MAPPING.put(Float[].class, () -> new Float[0]);
        DIRECT_CLASS_MAPPING.put(Double[].class, () -> new Double[0]);
        DIRECT_CLASS_MAPPING.put(Character[].class, () -> new Character[0]);

        // ASSIGNABLE_CLASS_MAPPING for interfaces and abstract classes
        // Order from most specific to most general
        ASSIGNABLE_CLASS_MAPPING.put(EnumSet.class, () -> null);

        // Specific collection types
        ASSIGNABLE_CLASS_MAPPING.put(BlockingDeque.class, LinkedBlockingDeque::new);
        ASSIGNABLE_CLASS_MAPPING.put(Deque.class, ArrayDeque::new);
        ASSIGNABLE_CLASS_MAPPING.put(BlockingQueue.class, LinkedBlockingQueue::new);
        ASSIGNABLE_CLASS_MAPPING.put(Queue.class, LinkedList::new);

        // Specific set types
        ASSIGNABLE_CLASS_MAPPING.put(NavigableSet.class, TreeSet::new);
        ASSIGNABLE_CLASS_MAPPING.put(SortedSet.class, TreeSet::new);
        ASSIGNABLE_CLASS_MAPPING.put(Set.class, LinkedHashSet::new);

        // Specific map types
        ASSIGNABLE_CLASS_MAPPING.put(ConcurrentMap.class, ConcurrentHashMap::new);
        ASSIGNABLE_CLASS_MAPPING.put(NavigableMap.class, TreeMap::new);
        ASSIGNABLE_CLASS_MAPPING.put(SortedMap.class, TreeMap::new);
        ASSIGNABLE_CLASS_MAPPING.put(Map.class, LinkedHashMap::new);

        // List and more general collection types
        ASSIGNABLE_CLASS_MAPPING.put(List.class, ArrayList::new);
        ASSIGNABLE_CLASS_MAPPING.put(Collection.class, ArrayList::new);

        // Iterators and enumerations
        ASSIGNABLE_CLASS_MAPPING.put(ListIterator.class, () -> new ArrayList<>().listIterator());
        ASSIGNABLE_CLASS_MAPPING.put(Iterator.class, Collections::emptyIterator);
        ASSIGNABLE_CLASS_MAPPING.put(Enumeration.class, Collections::emptyEnumeration);

        // Other interfaces
        ASSIGNABLE_CLASS_MAPPING.put(RandomAccess.class, ArrayList::new);
        ASSIGNABLE_CLASS_MAPPING.put(CharSequence.class, StringBuilder::new);
        ASSIGNABLE_CLASS_MAPPING.put(Comparable.class, () -> "");  // String implements Comparable
        ASSIGNABLE_CLASS_MAPPING.put(Cloneable.class, ArrayList::new);  // ArrayList implements Cloneable
        ASSIGNABLE_CLASS_MAPPING.put(AutoCloseable.class, () -> new ByteArrayInputStream(new byte[0]));

        // Most general
        ASSIGNABLE_CLASS_MAPPING.put(Iterable.class, ArrayList::new);

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
     * Sets a custom cache implementation for holding results of getAllSuperTypes(). The Set implementation must be
     * thread-safe, like ConcurrentSet, ConcurrentSkipListSet, etc.
     * @param cache The custom cache implementation to use for storing results of getAllSuperTypes().
     *             Must be thread-safe and implement Map interface.
     */
    public static void setSuperTypesCache(Map<Class<?>, Set<Class<?>>> cache) {
        SUPER_TYPES_CACHE = cache;
    }

    /**
     * Sets a custom cache implementation for holding results of getAllSuperTypes(). The Map implementation must be
     * thread-safe, like ConcurrentHashMap, LRUCache, ConcurrentSkipListMap, etc.
     * @param cache The custom cache implementation to use for storing results of getAllSuperTypes().
     *             Must be thread-safe and implement Map interface.
     */
    public static void setClassDistanceCache(Map<Map.Entry<Class<?>, Class<?>>, Integer> cache) {
        CLASS_DISTANCE_CACHE = cache;
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
     * Results are cached for performance.
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

        // Use an immutable Map.Entry as the key
        Map.Entry<Class<?>, Class<?>> key = new AbstractMap.SimpleImmutableEntry<>(source, destination);

        // Retrieve from cache or compute if absent
        return CLASS_DISTANCE_CACHE.computeIfAbsent(key, k ->
                computeInheritanceDistanceInternal(source, destination));
    }

    /**
     * Internal implementation of inheritance distance calculation.
     *
     * @param source      The source class, interface, or primitive type.
     * @param destination The destination class, interface, or primitive type.
     * @return The number of steps from the source to the destination, or -1 if no path exists.
     */
    private static int computeInheritanceDistanceInternal(Class<?> source, Class<?> destination) {
        // Handle primitives first.
        if (source.isPrimitive()) {
            if (destination.isPrimitive()) {
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

        // Use a BFS approach to determine the inheritance distance.
        Queue<Class<?>> queue = new LinkedList<>();
        Map<Class<?>, Boolean> visited = new IdentityHashMap<>();
        queue.add(source);
        visited.put(source, Boolean.TRUE);
        int distance = 0;

        while (!queue.isEmpty()) {
            int levelSize = queue.size();
            distance++;

            for (int i = 0; i < levelSize; i++) {
                Class<?> current = queue.poll();

                // Check the superclass
                Class<?> sup = current.getSuperclass();
                if (sup != null) {
                    if (sup.equals(destination)) {
                        return distance;
                    }
                    if (!visited.containsKey(sup)) {
                        queue.add(sup);
                        visited.put(sup, Boolean.TRUE);
                    }
                }

                // Check all interfaces
                for (Class<?> iface : current.getInterfaces()) {
                    if (iface.equals(destination)) {
                        return distance;
                    }
                    if (!visited.containsKey(iface)) {
                        queue.add(iface);
                        visited.put(iface, Boolean.TRUE);
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
        return osgiClassLoaders.computeIfAbsent(classFromBundle, ClassUtilities::getOSGiClassLoader0);
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
        Convention.throwIfNull(clazz, "Source class cannot be null");
        Convention.throwIfNull(candidateClasses, "Candidate classes Map cannot be null");

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

    private static final int BUFFER_SIZE = 65536;

    /**
     * Reads an InputStream fully and returns its content as a byte array.
     *
     * @param inputStream InputStream to read.
     * @return Content of the InputStream as byte array.
     * @throws IOException if an I/O error occurs.
     */
    private static byte[] readInputStreamFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(BUFFER_SIZE);
        byte[] data = new byte[BUFFER_SIZE];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
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
     * Optimally match arguments to constructor parameters.
     * This implementation uses a more efficient scoring algorithm and avoids redundant operations.
     *
     * @param converter Converter to use for type conversions
     * @param values Collection of potential arguments
     * @param parameters Array of parameter types to match against
     * @param allowNulls Whether to allow null values for non-primitive parameters
     * @return List of values matched to the parameters in the correct order
     */
    private static List<Object> matchArgumentsToParameters(Converter converter, Collection<Object> values,
                                                           Parameter[] parameters, boolean allowNulls) {
        if (parameters == null || parameters.length == 0) {
            return new ArrayList<>();
        }

        // Create result array and tracking arrays
        Object[] result = new Object[parameters.length];
        boolean[] parameterMatched = new boolean[parameters.length];

        // For tracking available values (more efficient than repeated removal from list)
        Object[] valueArray = values.toArray();
        boolean[] valueUsed = new boolean[valueArray.length];

        // PHASE 1: Find exact type matches - highest priority
        findExactMatches(valueArray, valueUsed, parameters, parameterMatched, result);

        // PHASE 2: Find assignable type matches with inheritance
        findInheritanceMatches(valueArray, valueUsed, parameters, parameterMatched, result);

        // PHASE 3: Find primitive/wrapper matches
        findPrimitiveWrapperMatches(valueArray, valueUsed, parameters, parameterMatched, result);

        // PHASE 4: Find convertible type matches
        findConvertibleMatches(converter, valueArray, valueUsed, parameters, parameterMatched, result);

        // PHASE 5: Fill remaining unmatched parameters with defaults or nulls
        fillRemainingParameters(converter, parameters, parameterMatched, result, allowNulls);

        // Convert result to List
        return Arrays.asList(result);
    }

    /**
     * Find exact type matches between values and parameters
     */
    private static void findExactMatches(Object[] values, boolean[] valueUsed,
                                         Parameter[] parameters, boolean[] parameterMatched,
                                         Object[] result) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameterMatched[i]) continue;

            Class<?> paramType = parameters[i].getType();

            for (int j = 0; j < values.length; j++) {
                if (valueUsed[j]) continue;

                Object value = values[j];
                if (value != null && value.getClass() == paramType) {
                    result[i] = value;
                    parameterMatched[i] = true;
                    valueUsed[j] = true;
                    break;
                }
            }
        }
    }

    /**
     * Find matches based on inheritance relationships
     */
    private static void findInheritanceMatches(Object[] values, boolean[] valueUsed,
                                               Parameter[] parameters, boolean[] parameterMatched,
                                               Object[] result) {
        // For each unmatched parameter, find best inheritance match
        for (int i = 0; i < parameters.length; i++) {
            if (parameterMatched[i]) continue;

            Class<?> paramType = parameters[i].getType();
            int bestDistance = Integer.MAX_VALUE;
            int bestValueIndex = -1;

            for (int j = 0; j < values.length; j++) {
                if (valueUsed[j]) continue;

                Object value = values[j];
                if (value == null) continue;

                Class<?> valueClass = value.getClass();
                int distance = ClassUtilities.computeInheritanceDistance(valueClass, paramType);

                if (distance >= 0 && distance < bestDistance) {
                    bestDistance = distance;
                    bestValueIndex = j;
                }
            }

            if (bestValueIndex >= 0) {
                result[i] = values[bestValueIndex];
                parameterMatched[i] = true;
                valueUsed[bestValueIndex] = true;
            }
        }
    }

    /**
     * Find matches between primitives and their wrapper types
     */
    private static void findPrimitiveWrapperMatches(Object[] values, boolean[] valueUsed,
                                                    Parameter[] parameters, boolean[] parameterMatched,
                                                    Object[] result) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameterMatched[i]) continue;

            Class<?> paramType = parameters[i].getType();

            for (int j = 0; j < values.length; j++) {
                if (valueUsed[j]) continue;

                Object value = values[j];
                if (value == null) continue;

                Class<?> valueClass = value.getClass();

                if (doesOneWrapTheOther(paramType, valueClass)) {
                    result[i] = value;
                    parameterMatched[i] = true;
                    valueUsed[j] = true;
                    break;
                }
            }
        }
    }

    /**
     * Find matches that require type conversion
     */
    private static void findConvertibleMatches(Converter converter, Object[] values, boolean[] valueUsed,
                                               Parameter[] parameters, boolean[] parameterMatched,
                                               Object[] result) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameterMatched[i]) continue;

            Class<?> paramType = parameters[i].getType();

            for (int j = 0; j < values.length; j++) {
                if (valueUsed[j]) continue;

                Object value = values[j];
                if (value == null) continue;

                Class<?> valueClass = value.getClass();

                if (converter.isSimpleTypeConversionSupported(paramType, valueClass)) {
                    try {
                        Object converted = converter.convert(value, paramType);
                        result[i] = converted;
                        parameterMatched[i] = true;
                        valueUsed[j] = true;
                        break;
                    } catch (Exception ignored) {
                        // Conversion failed, continue
                    }
                }
            }
        }
    }

    /**
     * Fill any remaining unmatched parameters with default values or nulls
     */
    private static void fillRemainingParameters(Converter converter, Parameter[] parameters,
                                                boolean[] parameterMatched, Object[] result,
                                                boolean allowNulls) {
        for (int i = 0; i < parameters.length; i++) {
            if (parameterMatched[i]) continue;

            Parameter parameter = parameters[i];
            Class<?> paramType = parameter.getType();

            if (allowNulls && !paramType.isPrimitive()) {
                result[i] = null;
            } else {
                // Get default value for the type
                Object defaultValue = getArgForType(converter, paramType);

                // If no default and primitive, convert null
                if (defaultValue == null && paramType.isPrimitive()) {
                    defaultValue = converter.convert(null, paramType);
                }

                result[i] = defaultValue;
            }
        }
    }

    /**
     * Returns the index of the smallest value in an array.
     * @param array The array to search.
     * @return The index of the smallest value, or -1 if the array is empty.
     * @deprecated 
     */
    @Deprecated
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
     *     <li>Using cached successful constructor from previous instantiations</li>
     *     <li>Using constructors in optimal order (public, protected, package, private)</li>
     *     <li>Within each accessibility level, trying constructors with more parameters first</li>
     *     <li>For each constructor, trying with exact matches first, then allowing null values</li>
     *     <li>Using unsafe instantiation (if enabled)</li>
     * </ol>
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
     */
    public static Object newInstance(Converter converter, Class<?> c, Collection<?> argumentValues) {
        if (c == null) { throw new IllegalArgumentException("Class cannot be null"); }
        if (c.isInterface()) { throw new IllegalArgumentException("Cannot instantiate interface: " + c.getName()); }
        if (Modifier.isAbstract(c.getModifiers())) { throw new IllegalArgumentException("Cannot instantiate abstract class: " + c.getName()); }

        // Security checks - now using static final field
        for (Class<?> check : SECURITY_BLOCKED_CLASSES) {
            if (check.isAssignableFrom(c)) {
                throw new IllegalArgumentException("For security reasons, json-io does not allow instantiation of: " + check.getName());
            }
        }

        // Additional security check for ProcessImpl
        if ("java.lang.ProcessImpl".equals(c.getName())) {
            throw new IllegalArgumentException("For security reasons, json-io does not allow instantiation of: java.lang.ProcessImpl");
        }

        // First attempt: Check if we have a previously successful constructor for this class
        List<Object> normalizedArgs = argumentValues == null ? new ArrayList<>() : new ArrayList<>(argumentValues);
        Constructor<?> cachedConstructor = SUCCESSFUL_CONSTRUCTOR_CACHE.get(c);

        if (cachedConstructor != null) {
            try {
                Parameter[] parameters = cachedConstructor.getParameters();

                // Try both approaches with the cached constructor
                try {
                    List<Object> argsNonNull = matchArgumentsToParameters(converter, new ArrayList<>(normalizedArgs), parameters, false);
                    return cachedConstructor.newInstance(argsNonNull.toArray());
                } catch (Exception e) {
                    List<Object> argsNull = matchArgumentsToParameters(converter, new ArrayList<>(normalizedArgs), parameters, true);
                    return cachedConstructor.newInstance(argsNull.toArray());
                }
            } catch (Exception ignored) {
                // If cached constructor fails, continue with regular instantiation
                // and potentially update the cache
            }
        }

        // Handle inner classes - with circular reference protection
        if (c.getEnclosingClass() != null && !Modifier.isStatic(c.getModifiers())) {
            // Track already visited classes to prevent circular references
            Set<Class<?>> visitedClasses = Collections.newSetFromMap(new IdentityHashMap<>());
            visitedClasses.add(c);

            try {
                // For inner classes, try to get the enclosing instance
                Class<?> enclosingClass = c.getEnclosingClass();
                if (!visitedClasses.contains(enclosingClass)) {
                    Object enclosingInstance = newInstance(converter, enclosingClass, Collections.emptyList());
                    Constructor<?> constructor = ReflectionUtils.getConstructor(c, enclosingClass);
                    if (constructor != null) {
                        // Cache this successful constructor
                        SUCCESSFUL_CONSTRUCTOR_CACHE.put(c, constructor);
                        return constructor.newInstance(enclosingInstance);
                    }
                }
            } catch (Exception ignored) {
                // Fall through to regular instantiation if this fails
            }
        }

        // Get constructors - already sorted in optimal order by ReflectionUtils.getAllConstructors
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(c);
        List<Exception> exceptions = new ArrayList<>();  // Collect all exceptions for better diagnostics

        // Try each constructor in order
        for (Constructor<?> constructor : constructors) {
            Parameter[] parameters = constructor.getParameters();

            // Attempt instantiation with this constructor
            try {
                // Try with non-null arguments first (more precise matching)
                List<Object> argsNonNull = matchArgumentsToParameters(converter, new ArrayList<>(normalizedArgs), parameters, false);
                Object instance = constructor.newInstance(argsNonNull.toArray());

                // Cache this successful constructor for future use
                SUCCESSFUL_CONSTRUCTOR_CACHE.put(c, constructor);
                return instance;
            } catch (Exception e1) {
                exceptions.add(e1);

                // If that fails, try with nulls allowed for unmatched parameters
                try {
                    List<Object> argsNull = matchArgumentsToParameters(converter, new ArrayList<>(normalizedArgs), parameters, true);
                    Object instance = constructor.newInstance(argsNull.toArray());

                    // Cache this successful constructor for future use
                    SUCCESSFUL_CONSTRUCTOR_CACHE.put(c, constructor);
                    return instance;
                } catch (Exception e2) {
                    exceptions.add(e2);
                    // Continue to next constructor
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
        if (!exceptions.isEmpty()) {
            // Include the most relevant exception message
            Exception lastException = exceptions.get(exceptions.size() - 1);
            msg += " - Most recent error: " + lastException.getMessage();

            // Optionally include all exception messages for detailed troubleshooting
            if (exceptions.size() > 1) {
                StringBuilder errorDetails = new StringBuilder("\nAll constructor errors:\n");
                for (int i = 0; i < exceptions.size(); i++) {
                    Exception e = exceptions.get(i);
                    errorDetails.append("  ").append(i + 1).append(") ")
                            .append(e.getClass().getSimpleName()).append(": ")
                            .append(e.getMessage()).append("\n");
                }
                msg += errorDetails.toString();
            }
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
     * {@code classA} and {@code classB}, excluding any types specified in {@code excludeSet}.
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
     * or other "marker" interfaces (e.g. {@code Serializable}, {@code Cloneable}, {@code Externalizable}
     * {@code Comparable}). If you do not want these in the final result, add them to
     * {@code excludeSet}.</p>
     *
     * <p>The returned set may contain multiple types if they are "equally specific"
     * and do not extend or implement one another. If the resulting set is empty,
     * then there is no common supertype of {@code classA} and {@code classB} outside
     * of the excluded types.</p>
     *
     * <p>Example (excluding {@code Object.class, Serializable.class, Externalizable.class, Cloneable.class}):
     * <pre>{@code
     * Set<Class<?>> excludedSet = Collections.singleton(Object.class, Serializable.class, Externalizable.class, Cloneable.class);
     * Set<Class<?>> supertypes = findLowestCommonSupertypesExcluding(TreeSet.class, HashSet.class, excludeSet);
     * // supertypes might contain only [Set], since Set is the lowest interface
     * // they both implement, and we excluded Object.
     * }</pre>
     *
     * @param classA   the first class, may be null
     * @param classB   the second class, may be null
     * @param excluded a set of classes or interfaces to exclude from the final result
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
            Set<Class<?>> excluded)
    {
        if (classA == null || classB == null) {
            return Collections.emptySet();
        }
        if (classA.equals(classB)) {
            // If it's in the skip list, return empty; otherwise return singleton
            return excluded.contains(classA) ? Collections.emptySet()
                    : Collections.singleton(classA);
        }

        // 1) Gather all supertypes of A and B
        Set<Class<?>> allA = getAllSupertypes(classA);
        Set<Class<?>> allB = getAllSupertypes(classB);

        // 2) Intersect
        allA.retainAll(allB);

        // 3) Remove all excluded (Object, Serializable, etc.)
        allA.removeAll(excluded);

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
     * {@code Object, Serializable, Externalizable, Cloneable}.
     * <p>
     * This method is a convenience wrapper around
     * {@link #findLowestCommonSupertypesExcluding(Class, Class, Set)} using a skip list
     * that includes {@code Object, Serializable, Externalizable, Cloneable}. In other words, if the only common
     * ancestor is {@code Object.class}, this method returns an empty set.
     * </p>
     *
     * <p>Example:
     * <pre>{@code
     * Set<Class<?>> supertypes = findLowestCommonSupertypes(Integer.class, Double.class);
     * // Potentially returns [Number, Comparable] because those are
     * // equally specific and not ancestors of one another, ignoring Object.class.
     * }</pre>
     *
     * @param classA the first class, may be null
     * @param classB the second class, may be null
     * @return a {@code Set} of all equally "lowest" common supertypes, excluding
     *         {@code Object, Serializable, Externalizable, Cloneable}; or an empty
     *         set if none are found beyond {@code Object} (or if either input is null)
     * @see #findLowestCommonSupertypesExcluding(Class, Class, Set)
     * @see #getAllSupertypes(Class)
     */
    public static Set<Class<?>> findLowestCommonSupertypes(Class<?> classA, Class<?> classB) {
        return findLowestCommonSupertypesExcluding(classA, classB,
                CollectionUtilities.setOf(Object.class, Serializable.class, Externalizable.class, Cloneable.class));
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
        Set<Class<?>> cached = SUPER_TYPES_CACHE.computeIfAbsent(clazz, key -> {
            Set<Class<?>> results = new LinkedHashSet<>();
            Queue<Class<?>> queue = new ArrayDeque<>();
            queue.add(key);
            while (!queue.isEmpty()) {
                Class<?> current = queue.poll();
                if (current != null && results.add(current)) {
                    // Add its superclass
                    Class<?> sup = current.getSuperclass();
                    if (sup != null) {
                        queue.add(sup);
                    }
                    // Add all interfaces
                    queue.addAll(Arrays.asList(current.getInterfaces()));
                }
            }
            return results;
        });
        return new LinkedHashSet<>(cached);
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
