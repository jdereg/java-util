package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
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
import java.util.concurrent.TimeUnit;
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

    private static final Map<String, Class<?>> nameToClass = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> wrapperMap;
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new ClassValueMap<>();
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = new ClassValueMap<>();

    // Cache for OSGi ClassLoader to avoid repeated reflection calls
    private static final Map<Class<?>, ClassLoader> osgiClassLoaders = new ClassValueMap<>();
    private static final ClassLoader SYSTEM_LOADER = ClassLoader.getSystemClassLoader();
    private static volatile boolean useUnsafe = false;
    private static volatile Unsafe unsafe;
    private static final Map<Class<?>, Supplier<Object>> DIRECT_CLASS_MAPPING = new ClassValueMap<>();
    private static final Map<Class<?>, Supplier<Object>> ASSIGNABLE_CLASS_MAPPING = new ClassValueMap<>();
    /**
     * A cache that maps a Class<?> to its associated enum type (if any).
     */
    private static final ClassValue<Class<?>> ENUM_CLASS_CACHE = new ClassValue<Class<?>>() {
        @Override
        protected Class<?> computeValue(Class<?> type) {
            return computeEnum(type);
        }
    };

    /**
     * Add a cache for successful constructor selections
     */
    private static final Map<Class<?>, Constructor<?>> SUCCESSFUL_CONSTRUCTOR_CACHE = new ClassValueMap<>();

    /**
     * Cache for class hierarchy information
     */
    private static final Map<Class<?>, ClassHierarchyInfo> CLASS_HIERARCHY_CACHE = new ClassValueMap<>();

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
        DIRECT_CLASS_MAPPING.put(EnumMap.class, () -> new EnumMap<>(TimeUnit.class));

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
        DIRECT_CLASS_MAPPING.put(Object[].class, () -> ArrayUtilities.EMPTY_OBJECT_ARRAY);

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

        PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
        PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
        PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
        PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
        PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
        PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
        PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
        PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
        PRIMITIVE_TO_WRAPPER.put(void.class, Void.class);

        // Initialize wrapper mappings
        WRAPPER_TO_PRIMITIVE.put(Boolean.class, boolean.class);
        WRAPPER_TO_PRIMITIVE.put(Byte.class, byte.class);
        WRAPPER_TO_PRIMITIVE.put(Character.class, char.class);
        WRAPPER_TO_PRIMITIVE.put(Short.class, short.class);
        WRAPPER_TO_PRIMITIVE.put(Integer.class, int.class);
        WRAPPER_TO_PRIMITIVE.put(Long.class, long.class);
        WRAPPER_TO_PRIMITIVE.put(Float.class, float.class);
        WRAPPER_TO_PRIMITIVE.put(Double.class, double.class);
        WRAPPER_TO_PRIMITIVE.put(Void.class, void.class);

        Map<Class<?>, Class<?>> map = new ClassValueMap<>();
        map.putAll(PRIMITIVE_TO_WRAPPER);
        map.putAll(WRAPPER_TO_PRIMITIVE);
        wrapperMap = Collections.unmodifiableMap(map);
    }

    /**
     * Converts a wrapper class to its corresponding primitive type.
     *
     * @param toType The wrapper class to convert to its primitive equivalent.
     *               Must be one of the standard Java wrapper classes (e.g., Integer.class, Boolean.class).
     * @return The primitive class corresponding to the provided wrapper class or null if toType is not a primitive wrapper.
     * @throws IllegalArgumentException If toType is null
     */
    public static Class<?> getPrimitiveFromWrapper(Class<?> toType) {
        Convention.throwIfNull(toType, "toType cannot be null");
        return WRAPPER_TO_PRIMITIVE.get(toType);
    }

    /**
     * Container for class hierarchy information to avoid redundant calculations
     * Not considered API.  Do not use this class in your code.
     */
    public static class ClassHierarchyInfo {
        private final Set<Class<?>> allSupertypes;
        private final Map<Class<?>, Integer> distanceMap;
        private final int depth; // Store depth as a field
        
        ClassHierarchyInfo(Set<Class<?>> supertypes, Map<Class<?>, Integer> distances, Class<?> sourceClass) {
            this.allSupertypes = Collections.unmodifiableSet(supertypes);
            this.distanceMap = Collections.unmodifiableMap(distances);

            // Calculate the depth during construction
            int maxDepth = 0;
            Class<?> current = sourceClass;
            while (current != null) {
                current = current.getSuperclass();
                maxDepth++;
            }
            this.depth = maxDepth - 1; // -1 because we counted steps, not classes
        }

        public Map<Class<?>, Integer> getDistanceMap() {
            return distanceMap;
        }

        Set<Class<?>> getAllSupertypes() {
            return allSupertypes;
        }
        
        int getDistance(Class<?> type) {
            return distanceMap.getOrDefault(type, -1);
        }

        public int getDepth() {
            return depth;
        }
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

        // Handle primitives specially
        if (source.isPrimitive() || isPrimitive(source)) {
            if (destination.isPrimitive() || isPrimitive(destination)) {
                return areSamePrimitiveType(source, destination) ? 0 : -1;
            }
        }

        // Use the cached hierarchy info for non-primitive cases
        return getClassHierarchyInfo(source).getDistance(destination);
    }

    /**
     * Determines if two primitive or wrapper types represent the same primitive type.
     *
     * @param source The source type to compare
     * @param destination The destination type to compare
     * @return true if both types represent the same primitive type, false otherwise
     */
    private static boolean areSamePrimitiveType(Class<?> source, Class<?> destination) {
        // If both are primitive, they must be exactly the same type
        if (source.isPrimitive() && destination.isPrimitive()) {
            return source.equals(destination);
        }

        // Get normalized primitive types (if they are wrappers, get the primitive equivalent)
        Class<?> sourcePrimitive = source.isPrimitive() ? source : WRAPPER_TO_PRIMITIVE.get(source);
        Class<?> destPrimitive = destination.isPrimitive() ? destination : WRAPPER_TO_PRIMITIVE.get(destination);

        // If either conversion failed, they're not compatible
        if (sourcePrimitive == null || destPrimitive == null) {
            return false;
        }

        // Check if they represent the same primitive type (e.g., int.class and Integer.class)
        return sourcePrimitive.equals(destPrimitive);
    }
    
    /**
     * @param c Class to test
     * @return boolean true if the passed in class is a Java primitive, false otherwise.  The Wrapper classes
     * Integer, Long, Boolean, etc. are considered primitives by this method.
     */
    public static boolean isPrimitive(Class<?> c) {
        return c.isPrimitive() || WRAPPER_TO_PRIMITIVE.containsKey(c);
    }

    /**
     * Given the passed in String class name, return the named JVM class.
     *
     * @param name        String name of a JVM class.
     * @param classLoader ClassLoader to use when searching for JVM classes.
     * @return Class instance of the named JVM class or null if not found.
     */
    public static Class<?> forName(String name, ClassLoader classLoader) {
        if (StringUtilities.isEmpty(name)) {
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

        // Check name before loading (quick rejection)
        if (SecurityChecker.isSecurityBlockedName(name)) {
            throw new SecurityException("For security reasons, cannot load: " + name);
        }

        c = loadClass(name, classLoader);

        // Perform full security check on loaded class
        SecurityChecker.verifyClass(c);

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

        Class<?> c = PRIMITIVE_TO_WRAPPER.get(primitiveClass);

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

            // Invoke FrameworkUtil.getBundle(classFromBundle) to get the Bundle instance
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
            // OSGi environment not detected or an error occurred
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
     * Loads resource content as a {@link String}.
     * <p>
     * This method delegates to {@link #loadResourceAsBytes(String)} which first
     * attempts to resolve the resource using the current thread's context
     * {@link ClassLoader} and then falls back to the {@code ClassUtilities}
     * class loader.
     * </p>
     *
     * @param resourceName Name of the resource file.
     * @return Content of the resource file as a String.
     */
    public static String loadResourceAsString(String resourceName) {
        byte[] resourceBytes = loadResourceAsBytes(resourceName);
        return new String(resourceBytes, StandardCharsets.UTF_8);
    }

    /**
     * Loads resource content as a byte[] using the following lookup order:
     * <ol>
     *     <li>The current thread's context {@link ClassLoader}</li>
     *     <li>The {@code ClassUtilities} class loader</li>
     * </ol>
     *
     * @param resourceName Name of the resource file.
     * @return Content of the resource file as a byte[].
     * @throws IllegalArgumentException if the resource cannot be found
     * @throws UncheckedIOException if there is an error reading the resource
     * @throws NullPointerException if resourceName is null
     */
    public static byte[] loadResourceAsBytes(String resourceName) {
        Objects.requireNonNull(resourceName, "resourceName cannot be null");

        InputStream inputStream = null;
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            inputStream = cl.getResourceAsStream(resourceName);
        }
        if (inputStream == null) {
            cl = ClassUtilities.getClassLoader(ClassUtilities.class);
            inputStream = cl.getResourceAsStream(resourceName);
        }

        if (inputStream == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceName);
        }

        try (InputStream in = inputStream) {
            return readInputStreamFully(in);
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
     * Optimally match arguments to constructor parameters with minimal collection creation.
     *
     * @param converter Converter to use for type conversions
     * @param values Collection of potential arguments
     * @param parameters Array of parameter types to match against
     * @param allowNulls Whether to allow null values for non-primitive parameters
     * @return Array of values matched to the parameters in the correct order
     */
    private static Object[] matchArgumentsToParameters(Converter converter, Collection<?> values,
                                                       Parameter[] parameters, boolean allowNulls) {
        if (parameters == null || parameters.length == 0) {
            return ArrayUtilities.EMPTY_OBJECT_ARRAY; // Reuse a static empty array
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

        return result;
    }

    /**
     * Find exact type matches between values and parameters
     */
    private static void findExactMatches(Object[] values, boolean[] valueUsed,
                                         Parameter[] parameters, boolean[] parameterMatched,
                                         Object[] result) {
        int valLen = values.length;
        int paramLen = parameters.length;
        for (int i = 0; i < paramLen; i++) {
            if (parameterMatched[i]) continue;

            Class<?> paramType = parameters[i].getType();

            for (int j = 0; j < valLen; j++) {
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
        // For each unmatched parameter, find the best inheritance match
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
     * Returns the related enum class for the provided class, if one exists.
     *
     * @param c the class to check; may be null
     * @return the related enum class, or null if none is found
     */
    public static Class<?> getClassIfEnum(Class<?> c) {
        if (c == null) {
            return null;
        }
        return ENUM_CLASS_CACHE.get(c);
    }

    /**
     * Computes the enum type for a given class by first checking if the class itself is an enum,
     * then traversing its superclass hierarchy, and finally its enclosing classes.
     *
     * @param c the class to check; not null
     * @return the related enum class if found, or null otherwise
     */
    private static Class<?> computeEnum(Class<?> c) {
        // Fast path: if the class itself is an enum (and not java.lang.Enum), return it immediately.
        if (c.isEnum() && c != Enum.class) {
            return c;
        }

        // Traverse the superclass chain.
        Class<?> current = c;
        while ((current = current.getSuperclass()) != null) {
            if (current.isEnum() && current != Enum.class) {
                return current;
            }
        }

        // Traverse the enclosing class chain.
        current = c.getEnclosingClass();
        while (current != null) {
            if (current.isEnum() && current != Enum.class) {
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
        Convention.throwIfNull(c, "Class cannot be null");

        // Do security check FIRST
        SecurityChecker.verifyClass(c);

        // Then do other validation
        if (c.isInterface()) {
            throw new IllegalArgumentException("Cannot instantiate interface: " + c.getName());
        }
        if (Modifier.isAbstract(c.getModifiers())) {
            throw new IllegalArgumentException("Cannot instantiate abstract class: " + c.getName());
        }
        
        // First attempt: Check if we have a previously successful constructor for this class
        List<Object> normalizedArgs = argumentValues == null ? new ArrayList<>() : new ArrayList<>(argumentValues);
        Constructor<?> cachedConstructor = SUCCESSFUL_CONSTRUCTOR_CACHE.get(c);

        if (cachedConstructor != null) {
            try {
                Parameter[] parameters = cachedConstructor.getParameters();

                // Try both approaches with the cached constructor
                try {
                    Object[] argsNonNull = matchArgumentsToParameters(converter, normalizedArgs, parameters, false);
                    return cachedConstructor.newInstance(argsNonNull);
                } catch (Exception e) {
                    Object[] argsNull = matchArgumentsToParameters(converter, normalizedArgs, parameters, true);
                    return cachedConstructor.newInstance(argsNull);
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
                Object[] argsNonNull = matchArgumentsToParameters(converter, normalizedArgs, parameters, false);
                Object instance = constructor.newInstance(argsNonNull);

                // Cache this successful constructor for future use
                SUCCESSFUL_CONSTRUCTOR_CACHE.put(c, constructor);
                return instance;
            } catch (Exception e1) {
                exceptions.add(e1);

                // If that fails, try with nulls allowed for unmatched parameters
                try {
                    Object[] argsNull = matchArgumentsToParameters(converter, normalizedArgs, parameters, true);
                    Object instance = constructor.newInstance(argsNull);

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
                return unsafe.allocateInstance(c);
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
     *
     * @param classA   the first class, may be null
     * @param classB   the second class, may be null
     * @param excluded a set of classes or interfaces to exclude from the final result
     * @return a {@code Set} of the most specific common supertypes, excluding any in excluded set
     */
    public static Set<Class<?>> findLowestCommonSupertypesExcluding(
            Class<?> classA, Class<?> classB,
            Set<Class<?>> excluded)
    {
        if (classA == null || classB == null) {
            return Collections.emptySet();
        }
        if (classA.equals(classB)) {
            // If it's in the excluded list, return empty; otherwise return singleton
            return excluded.contains(classA) ? Collections.emptySet()
                    : Collections.singleton(classA);
        }

        // 1) Get unmodifiable views for better performance
        Set<Class<?>> allA = getClassHierarchyInfo(classA).getAllSupertypes();
        Set<Class<?>> allB = getClassHierarchyInfo(classB).getAllSupertypes();

        // 2) Create a modifiable copy of the intersection, filtering excluded items
        Set<Class<?>> common = new LinkedHashSet<>();
        for (Class<?> type : allA) {
            if (allB.contains(type) && !excluded.contains(type)) {
                common.add(type);
            }
        }

        if (common.isEmpty()) {
            return Collections.emptySet();
        }

        // 3) Sort by descending depth
        List<Class<?>> candidates = new ArrayList<>(common);
        candidates.sort((x, y) -> {
            int dx = getClassHierarchyInfo(x).getDepth();
            int dy = getClassHierarchyInfo(y).getDepth();
            return Integer.compare(dy, dx); // descending
        });

        // 4) Identify "lowest" types
        Set<Class<?>> lowest = new LinkedHashSet<>();
        Set<Class<?>> unionOfAncestors = new HashSet<>();

        for (Class<?> type : candidates) {
            if (unionOfAncestors.contains(type)) {
                // type is an ancestor of something already in 'lowest'
                continue;
            }
            // type is indeed a "lowest" so far
            lowest.add(type);

            // Add all type's supertypes to the union set
            unionOfAncestors.addAll(getClassHierarchyInfo(type).getAllSupertypes());
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
     * Gets the complete hierarchy information for a class, including all supertypes
     * and their inheritance distances from the source class.
     *
     * @param clazz The class to analyze
     * @return ClassHierarchyInfo containing all supertypes and distances
     */
    public static ClassHierarchyInfo getClassHierarchyInfo(Class<?> clazz) {
        return CLASS_HIERARCHY_CACHE.computeIfAbsent(clazz, key -> {
            // Compute all supertypes and their distances in one pass
            Set<Class<?>> allSupertypes = new LinkedHashSet<>();
            Map<Class<?>, Integer> distanceMap = new HashMap<>();

            // BFS to find all supertypes and compute distances in one pass
            Queue<Class<?>> queue = new ArrayDeque<>();
            queue.add(key);
            distanceMap.put(key, 0); // Distance to self is 0

            while (!queue.isEmpty()) {
                Class<?> current = queue.poll();
                int currentDistance = distanceMap.get(current);

                if (current != null && allSupertypes.add(current)) {
                    // Add superclass with distance+1
                    Class<?> superclass = current.getSuperclass();
                    if (superclass != null && !distanceMap.containsKey(superclass)) {
                        distanceMap.put(superclass, currentDistance + 1);
                        queue.add(superclass);
                    }

                    // Add interfaces with distance+1
                    for (Class<?> iface : current.getInterfaces()) {
                        if (!distanceMap.containsKey(iface)) {
                            distanceMap.put(iface, currentDistance + 1);
                            queue.add(iface);
                        }
                    }
                }
            }

            return new ClassHierarchyInfo(Collections.unmodifiableSet(allSupertypes),
                    Collections.unmodifiableMap(distanceMap), key);
        });
    }
    
    // Convenience boolean method
    public static boolean haveCommonAncestor(Class<?> a, Class<?> b) {
        return !findLowestCommonSupertypes(a, b).isEmpty();
    }

    // Static fields for the SecurityChecker class
    private static final ClassValueSet BLOCKED_CLASSES = new ClassValueSet();
    private static final Set<String> BLOCKED_CLASS_NAMES_SET = new HashSet<>(SecurityChecker.SECURITY_BLOCKED_CLASS_NAMES);

    // Cache for classes that have been checked and found to be inheriting from blocked classes
    private static final ClassValueSet INHERITS_FROM_BLOCKED = new ClassValueSet();
    // Cache for classes that have been checked and found to be safe
    private static final ClassValueSet VERIFIED_SAFE_CLASSES = new ClassValueSet();

    static {
        // Pre-populate with all blocked classes
        for (Class<?> blockedClass : SecurityChecker.SECURITY_BLOCKED_CLASSES.toSet()) {
            BLOCKED_CLASSES.add(blockedClass);
        }
    }

    private static final ClassValue<Boolean> SECURITY_CHECK_CACHE = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            // Direct blocked class check (ultra-fast with ClassValueSet)
            if (BLOCKED_CLASSES.contains(type)) {
                return Boolean.TRUE;
            }

            // Fast name-based check
            if (BLOCKED_CLASS_NAMES_SET.contains(type.getName())) {
                return Boolean.TRUE;
            }

            // Check if already verified as inheriting from blocked
            if (INHERITS_FROM_BLOCKED.contains(type)) {
                return Boolean.TRUE;
            }

            // Check if already verified as safe
            if (VERIFIED_SAFE_CLASSES.contains(type)) {
                return Boolean.FALSE;
            }

            // Need to check inheritance - use ClassHierarchyInfo
            for (Class<?> superType : getClassHierarchyInfo(type).getAllSupertypes()) {
                if (BLOCKED_CLASSES.contains(superType)) {
                    // Cache for future checks
                    INHERITS_FROM_BLOCKED.add(type);
                    return Boolean.TRUE;
                }
            }

            // Class is safe
            VERIFIED_SAFE_CLASSES.add(type);
            return Boolean.FALSE;
        }
    };

    public static class SecurityChecker {
        // Combine all security-sensitive classes in one place
        static final ClassValueSet SECURITY_BLOCKED_CLASSES = new ClassValueSet(Arrays.asList(
                ClassLoader.class,
                ProcessBuilder.class,
                Process.class,
                Constructor.class,
                Method.class,
                Field.class,
                Runtime.class,
                System.class
        ));

        // Add specific class names that might be loaded dynamically
        static final Set<String> SECURITY_BLOCKED_CLASS_NAMES = new HashSet<>(Collections.singletonList(
                "java.lang.ProcessImpl"
                // Add any other specific class names
        ));

        /**
         * Checks if a class is blocked for security reasons.
         *
         * @param clazz The class to check
         * @return true if the class is blocked, false otherwise
         */
        public static boolean isSecurityBlocked(Class<?> clazz) {
            return SECURITY_CHECK_CACHE.get(clazz);
        }

        /**
         * Checks if a class name is directly in the blocked list.
         * Used before class loading.
         *
         * @param className The class name to check
         * @return true if the class name is blocked, false otherwise
         */
        public static boolean isSecurityBlockedName(String className) {
            return BLOCKED_CLASS_NAMES_SET.contains(className);
        }

        /**
         * Throws an exception if the class is blocked for security reasons.
         *
         * @param clazz The class to verify
         * @throws SecurityException if the class is blocked
         */
        public static void verifyClass(Class<?> clazz) {
            if (isSecurityBlocked(clazz)) {
                throw new SecurityException(
                        "For security reasons, json-io does not allow instantiation of: " + clazz.getName());
            }
        }
    }
}
