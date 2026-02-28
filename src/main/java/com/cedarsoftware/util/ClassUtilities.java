package com.cedarsoftware.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.logging.Level;
import java.util.logging.Logger;
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
 *     <li>Finding common supertypes and ancestors between classes ({@link #findLowestCommonSupertypes}).</li>
 *     <li>Instantiating objects with varargs constructor support ({@link #newInstance}).</li>
 * </ul>
 *
 * <h2>Inheritance Distance</h2>
 * <p>
 * The {@link #computeInheritanceDistance(Class, Class)} method calculates the number of inheritance steps
 * between two classes or interfaces. If there is no relationship, it returns {@code -1}. This method also
 * supports primitive widening conversions as defined in JLS 5.1.2, treating widening paths like
 * byte→short→int→long→float→double as inheritance relationships.
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

    private static final Logger LOG = Logger.getLogger(ClassUtilities.class.getName());
    static {
        LoggingConfig.init();
    }
    
    /**
     * Custom WeakReference that remembers its key name for cleanup via ReferenceQueue
     */
    private static final class NamedWeakRef extends WeakReference<Class<?>> {
        final String name;
        
        NamedWeakRef(String name, Class<?> referent, ReferenceQueue<Class<?>> q) {
            super(referent, q);
            this.name = name;
        }
    }
    
    /**
     * Holder for per-ClassLoader cache and its associated ReferenceQueue
     */
    private static final class LoaderCache {
        final ConcurrentMap<String, WeakReference<Class<?>>> cache = new ConcurrentHashMap<>(2048);
        final ReferenceQueue<Class<?>> queue = new ReferenceQueue<>();
        final AtomicInteger negativeEntryCount = new AtomicInteger();
    }

    private ClassUtilities() {
    }
    
    // Helper methods for ClassLoader-scoped caching
    
    // Consistently resolve the ClassLoader to use as cache key
    private static ClassLoader resolveLoader(ClassLoader cl) {
        return (cl != null) ? cl : getClassLoader(ClassUtilities.class);
    }
    
    // Private sentinel class to indicate "class not found" in the cache.
    // Using a dedicated inner class avoids collisions with any real class (including java.lang.Void).
    private static final class ClassNotFoundSentinel { }
    private static final Class<?> CLASS_NOT_FOUND_SENTINEL = ClassNotFoundSentinel.class;
    private static final int MAX_NEGATIVE_CLASS_CACHE_ENTRIES = 4096;

    private static Class<?> fromCache(String name, ClassLoader cl) {
        // Check global aliases first (primitive types and user-defined aliases)
        Class<?> globalAlias = GLOBAL_ALIASES.get(name);
        if (globalAlias != null) {
            return globalAlias;
        }

        // Then check classloader-specific cache using consistent resolution
        final ClassLoader key = resolveLoader(cl);
        LoaderCache holder = NAME_CACHE.get(key);
        if (holder == null) {
            return null;
        }

        // Opportunistically drain dead references (lock-free, uses CAS-based removal)
        drainQueue(holder);

        // Lock-free cache access using ConcurrentHashMap
        WeakReference<Class<?>> ref = holder.cache.get(name);
        Class<?> cls = (ref == null) ? null : ref.get();
        if (ref != null && cls == null) {
            // Use remove(key, value) to only remove if the exact same WeakReference is still present.
            // This prevents race conditions where another thread added a new entry with the same name.
            holder.cache.remove(name, ref);
        }
        return cls;
    }

    private static void toCacheNotFound(String name, ClassLoader cl) {
        final ClassLoader key = resolveLoader(cl);
        final LoaderCache holder = getLoaderCacheHolder(key);

        // Opportunistically drain dead references (lock-free, uses CAS-based removal)
        drainQueue(holder);

        // Bound negative-cache growth to prevent memory pressure with many unique missing class names.
        if (holder.negativeEntryCount.get() >= MAX_NEGATIVE_CLASS_CACHE_ENTRIES) {
            return;
        }

        // Cache the sentinel to indicate "class not found" - sentinel class is strongly reachable.
        WeakReference<Class<?>> prior = holder.cache.putIfAbsent(name, new WeakReference<>(CLASS_NOT_FOUND_SENTINEL));
        if (prior == null) {
            holder.negativeEntryCount.incrementAndGet();
        }
    }
    
    // Helper to get or create loader cache holder with proper synchronization
    private static LoaderCache getLoaderCacheHolder(ClassLoader key) {
        synchronized (NAME_CACHE) {
            LoaderCache holder = NAME_CACHE.get(key);
            if (holder == null) {
                holder = new LoaderCache();
                NAME_CACHE.put(key, holder);
            }
            return holder;
        }
    }
    
    private static void toCache(String name, ClassLoader cl, Class<?> c) {
        final ClassLoader key = resolveLoader(cl);
        final LoaderCache holder = getLoaderCacheHolder(key);

        // Opportunistically drain dead references (lock-free, uses CAS-based removal)
        drainQueue(holder);

        // Lock-free cache update using ConcurrentHashMap
        WeakReference<Class<?>> previous = holder.cache.put(name, new NamedWeakRef(name, c, holder.queue));
        if (isNotFoundEntry(previous)) {
            decrementNegativeCount(holder);
        }
    }
    
    /**
     * Drains the ReferenceQueue, removing dead entries from the cache.
     * Uses CAS-based removal to safely handle concurrent modifications.
     */
    private static void drainQueue(LoaderCache holder) {
        Reference<? extends Class<?>> ref;
        while ((ref = holder.queue.poll()) != null) {
            if (ref instanceof NamedWeakRef) {
                NamedWeakRef namedRef = (NamedWeakRef) ref;
                // Use remove(key, value) to only remove if the exact same WeakReference is still present.
                // This prevents race conditions where another thread added a new entry with the same name.
                holder.cache.remove(namedRef.name, namedRef);
            }
        }
    }

    private static boolean isNotFoundEntry(WeakReference<Class<?>> ref) {
        return ref != null && ref.get() == CLASS_NOT_FOUND_SENTINEL;
    }

    private static void decrementNegativeCount(LoaderCache holder) {
        int count;
        do {
            count = holder.negativeEntryCount.get();
            if (count <= 0) {
                return;
            }
        } while (!holder.negativeEntryCount.compareAndSet(count, count - 1));
    }

    private static void removeCacheEntry(LoaderCache holder, String name) {
        WeakReference<Class<?>> removed = holder.cache.remove(name);
        if (isNotFoundEntry(removed)) {
            decrementNegativeCount(holder);
        }
    }

    // ClassLoader-scoped cache with weak references to prevent classloader leaks
    // and ensure correctness in multi-classloader environments (OSGi, app servers, etc.)
    private static final Map<ClassLoader, LoaderCache> NAME_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());
    
    // Global aliases for primitive types and common names (not classloader-specific)
    private static final Map<String, Class<?>> GLOBAL_ALIASES = new ConcurrentHashMap<>();
    // Separate built-in aliases from user-added aliases to preserve user aliases during clearCaches()
    private static final Map<String, Class<?>> BUILTIN_ALIASES = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> USER_ALIASES = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> wrapperMap;
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new ClassValueMap<>();
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = new ClassValueMap<>();
    
    // Primitive widening conversion distances (JLS 5.1.2)
    // Maps from source primitive to Map<destination primitive, distance>
    private static final Map<Class<?>, Map<Class<?>, Integer>> PRIMITIVE_WIDENING_DISTANCES;

    // Cache for OSGi ClassLoader to avoid repeated reflection calls
    private static final Map<Class<?>, ClassLoader> osgiClassLoaders = new ClassValueMap<>();
    private static final ClassLoader SYSTEM_LOADER = ClassLoader.getSystemClassLoader();
    // Counter-based ThreadLocal for reentrant unsafe mode support.
    // Each setUseUnsafe(true) increments, each setUseUnsafe(false) decrements.
    // Unsafe mode is active when count > 0. This supports nested enable/disable calls.
    private static final ThreadLocal<Integer> unsafeDepth = ThreadLocal.withInitial(() -> 0);
    private static volatile Unsafe unsafe;

    // Configurable Security Controls
    // Note: Core class blocking security is ALWAYS enabled for safety
    private static final int DEFAULT_MAX_CLASS_LOAD_DEPTH = 100;
    private static final int DEFAULT_MAX_CONSTRUCTOR_ARGS = 50;
    private static final int DEFAULT_MAX_RESOURCE_NAME_LENGTH = 1000;

    // Thread-local depth tracking for enhanced security
    private static final ThreadLocal<Integer> CLASS_LOAD_DEPTH = ThreadLocal.withInitial(() -> 0);

    // Performance: Cache system properties at class initialization to avoid repeated System.getProperty() calls
    // These are checked frequently during class loading, so caching eliminates synchronized overhead
    // Note: Not final to allow test reinitialization via reinitializeSecuritySettings()
    private static boolean ENHANCED_SECURITY_ENABLED;
    private static int MAX_CLASS_LOAD_DEPTH;
    private static int MAX_CONSTRUCTOR_ARGS;
    private static int MAX_RESOURCE_NAME_LENGTH;

    static {
        // Initialize cached security settings at class load time
        reinitializeSecuritySettings();
    }

    /**
     * Reinitializes cached security settings from system properties.
     * Package-private to allow tests to update settings after changing system properties.
     * In production, security settings are cached once at class initialization for performance.
     */
    static void reinitializeSecuritySettings() {
        ENHANCED_SECURITY_ENABLED = "true".equalsIgnoreCase(
                System.getProperty("classutilities.enhanced.security.enabled"));

        if (ENHANCED_SECURITY_ENABLED) {
            MAX_CLASS_LOAD_DEPTH = getIntProperty("classutilities.max.class.load.depth", DEFAULT_MAX_CLASS_LOAD_DEPTH, 0);
            MAX_CONSTRUCTOR_ARGS = getIntProperty("classutilities.max.constructor.args", DEFAULT_MAX_CONSTRUCTOR_ARGS, 0);
            MAX_RESOURCE_NAME_LENGTH = getIntProperty("classutilities.max.resource.name.length", DEFAULT_MAX_RESOURCE_NAME_LENGTH, 100);
        } else {
            MAX_CLASS_LOAD_DEPTH = 0;
            MAX_CONSTRUCTOR_ARGS = 0;
            MAX_RESOURCE_NAME_LENGTH = DEFAULT_MAX_RESOURCE_NAME_LENGTH;
        }
    }

    private static int getIntProperty(String key, int defaultValue, int minValue) {
        String prop = System.getProperty(key);
        if (prop != null) {
            try {
                return Math.max(minValue, Integer.parseInt(prop));
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return defaultValue;
    }
    private static final Map<Class<?>, Supplier<Object>> DIRECT_CLASS_MAPPING = new ClassValueMap<>();
    private static final Map<Class<?>, Supplier<Object>> ASSIGNABLE_CLASS_MAPPING = new LinkedHashMap<>();
    // Cache for assignable type lookups to avoid repeated O(n) scans of ASSIGNABLE_CLASS_MAPPING.
    // Uses Optional to distinguish "no match" (Optional.empty()) from "not cached" (null).
    private static final Map<Class<?>, Optional<Supplier<Object>>> ASSIGNABLE_TYPE_CACHE = new ClassValueMap<>();
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
     * Cache for constructor selections.
     * - Optional.of(constructor) = this constructor works
     * - Optional.empty() = no constructor works, go directly to unsafe or fail
     * - null (not in map) = not yet determined
     */
    private static final Map<Class<?>, Optional<Constructor<?>>> SUCCESSFUL_CONSTRUCTOR_CACHE = new ClassValueMap<>();
    private static final Map<Class<?>, Map<ArgumentShapeKey, Optional<ConstructorPlan>>> CONSTRUCTOR_PLAN_CACHE = new ClassValueMap<>();
    private static final Map<Class<?>, Boolean> NAMED_PARAMETER_MATCHING_VIABLE_CACHE = new ClassValueMap<>();

    private static final class NullArgType { }
    private static final Class<?> NULL_ARG_TYPE = NullArgType.class;

    private static final class ArgumentShapeKey {
        private final Class<?>[] argTypes;
        private final int hash;

        private ArgumentShapeKey(Class<?>[] argTypes) {
            this.argTypes = argTypes;
            this.hash = Arrays.hashCode(argTypes);
        }

        static ArgumentShapeKey from(Object[] suppliedArgs) {
            if (suppliedArgs.length == 0) {
                return new ArgumentShapeKey(ArrayUtilities.EMPTY_CLASS_ARRAY);
            }
            Class<?>[] types = new Class<?>[suppliedArgs.length];
            for (int i = 0; i < suppliedArgs.length; i++) {
                Object arg = suppliedArgs[i];
                types[i] = (arg == null) ? NULL_ARG_TYPE : arg.getClass();
            }
            return new ArgumentShapeKey(types);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ArgumentShapeKey)) {
                return false;
            }
            ArgumentShapeKey other = (ArgumentShapeKey) o;
            return Arrays.equals(argTypes, other.argTypes);
        }
    }

    private static final class ConstructorPlan {
        private final Constructor<?> constructor;
        private final boolean allowNullsFirst;

        private ConstructorPlan(Constructor<?> constructor, boolean allowNullsFirst) {
            this.constructor = constructor;
            this.allowNullsFirst = allowNullsFirst;
        }
    }

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
        DIRECT_CLASS_MAPPING.put(TimeZone.class, () -> (TimeZone) TimeZone.getDefault().clone());
        // Use epoch (0) for SQL date/time types instead of current time
        DIRECT_CLASS_MAPPING.put(Timestamp.class, () -> new Timestamp(0));
        DIRECT_CLASS_MAPPING.put(java.sql.Date.class, () -> new java.sql.Date(0));
        // Use epoch dates instead of now() for predictable, stable defaults
        DIRECT_CLASS_MAPPING.put(LocalDate.class, () -> LocalDate.of(1970, 1, 1));  // 1970-01-01
        DIRECT_CLASS_MAPPING.put(LocalDateTime.class, () -> LocalDateTime.of(1970, 1, 1, 0, 0, 0));
        DIRECT_CLASS_MAPPING.put(OffsetDateTime.class, () -> OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        DIRECT_CLASS_MAPPING.put(ZonedDateTime.class, () -> ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        DIRECT_CLASS_MAPPING.put(ZoneId.class, ZoneId::systemDefault);
        DIRECT_CLASS_MAPPING.put(AtomicBoolean.class, AtomicBoolean::new);
        DIRECT_CLASS_MAPPING.put(AtomicInteger.class, AtomicInteger::new);
        DIRECT_CLASS_MAPPING.put(AtomicLong.class, AtomicLong::new);
        // URL and URI: Return null instead of potentially connectable URLs
        // Let the second pass handle these if needed
        // DIRECT_CLASS_MAPPING.put(URL.class, () -> null);
        // DIRECT_CLASS_MAPPING.put(URI.class, () -> null);
        DIRECT_CLASS_MAPPING.put(Object.class, Object::new);
        DIRECT_CLASS_MAPPING.put(String.class, () -> "");
        DIRECT_CLASS_MAPPING.put(BigInteger.class, () -> BigInteger.ZERO);
        DIRECT_CLASS_MAPPING.put(BigDecimal.class, () -> BigDecimal.ZERO);
        // Note: Class.class has no sensible default - returns null
        // Use a calendar set to epoch instead of current time
        DIRECT_CLASS_MAPPING.put(Calendar.class, () -> {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(0);
            return cal;
        });
        DIRECT_CLASS_MAPPING.put(Instant.class, () -> Instant.EPOCH);  // 1970-01-01T00:00:00Z
        DIRECT_CLASS_MAPPING.put(Duration.class, () -> Duration.ZERO);
        DIRECT_CLASS_MAPPING.put(Period.class, () -> Period.ofDays(0));
        // Use epoch year (1970) instead of current year
        DIRECT_CLASS_MAPPING.put(Year.class, () -> Year.of(1970));
        DIRECT_CLASS_MAPPING.put(YearMonth.class, () -> YearMonth.of(1970, 1));
        DIRECT_CLASS_MAPPING.put(MonthDay.class, () -> MonthDay.of(1, 1));
        DIRECT_CLASS_MAPPING.put(ZoneOffset.class, () -> ZoneOffset.UTC);
        DIRECT_CLASS_MAPPING.put(OffsetTime.class, () -> OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC));
        DIRECT_CLASS_MAPPING.put(LocalTime.class, () -> LocalTime.MIDNIGHT);
        // Return fresh instances to prevent mutation issues
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
        // EnumMap removed - requires explicit key enum type, cannot have a sensible default

        // Utility classes
        // Use a fixed nil UUID instead of random for predictability
        DIRECT_CLASS_MAPPING.put(UUID.class, () -> new UUID(0L, 0L));  // Nil UUID
        DIRECT_CLASS_MAPPING.put(Currency.class, () -> {
            try {
                return Currency.getInstance(Locale.getDefault());
            } catch (Exception e) {
                // Fall back to USD for locales that don't have a currency (e.g., Locale.ROOT)
                return Currency.getInstance(Locale.US);
            }
        });
        // Use empty pattern instead of match-all pattern
        DIRECT_CLASS_MAPPING.put(Pattern.class, () -> Pattern.compile(""));
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
        // Note: EnumSet cannot be instantiated without knowing the element type, so it's not included

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
        // Remove Comparable mapping - let it return null and be handled in second pass
        // This avoids surprising empty string for a generic interface
        ASSIGNABLE_CLASS_MAPPING.put(Cloneable.class, ArrayList::new);  // ArrayList implements Cloneable
        ASSIGNABLE_CLASS_MAPPING.put(AutoCloseable.class, () -> new ByteArrayInputStream(new byte[0]));

        // Most general
        ASSIGNABLE_CLASS_MAPPING.put(Iterable.class, ArrayList::new);

        // Initialize built-in aliases
        BUILTIN_ALIASES.put("boolean", Boolean.TYPE);
        BUILTIN_ALIASES.put("char", Character.TYPE);
        BUILTIN_ALIASES.put("byte", Byte.TYPE);
        BUILTIN_ALIASES.put("short", Short.TYPE);
        BUILTIN_ALIASES.put("int", Integer.TYPE);
        BUILTIN_ALIASES.put("long", Long.TYPE);
        BUILTIN_ALIASES.put("float", Float.TYPE);
        BUILTIN_ALIASES.put("double", Double.TYPE);
        BUILTIN_ALIASES.put("void", Void.TYPE);
        BUILTIN_ALIASES.put("string", String.class);
        BUILTIN_ALIASES.put("date", Date.class);
        BUILTIN_ALIASES.put("class", Class.class);
        
        // Populate GLOBAL_ALIASES with built-in aliases
        GLOBAL_ALIASES.putAll(BUILTIN_ALIASES);

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
        
        // Initialize primitive widening conversion distances (JLS 5.1.2)
        // byte → short → int → long → float → double
        // char → int → long → float → double
        
        // Create a temporary map to build the widening distances
        Map<Class<?>, Map<Class<?>, Integer>> tempPrimitiveWidening = new HashMap<>();
        
        // byte can widen to...
        Map<Class<?>, Integer> byteWidening = new HashMap<>();
        byteWidening.put(short.class, 1);
        byteWidening.put(int.class, 2);
        byteWidening.put(long.class, 3);
        byteWidening.put(float.class, 4);
        byteWidening.put(double.class, 5);
        tempPrimitiveWidening.put(byte.class, Collections.unmodifiableMap(byteWidening));
        
        // short can widen to...
        Map<Class<?>, Integer> shortWidening = new HashMap<>();
        shortWidening.put(int.class, 1);
        shortWidening.put(long.class, 2);
        shortWidening.put(float.class, 3);
        shortWidening.put(double.class, 4);
        tempPrimitiveWidening.put(short.class, Collections.unmodifiableMap(shortWidening));
        
        // char can widen to...
        Map<Class<?>, Integer> charWidening = new HashMap<>();
        charWidening.put(int.class, 1);
        charWidening.put(long.class, 2);
        charWidening.put(float.class, 3);
        charWidening.put(double.class, 4);
        tempPrimitiveWidening.put(char.class, Collections.unmodifiableMap(charWidening));
        
        // int can widen to...
        Map<Class<?>, Integer> intWidening = new HashMap<>();
        intWidening.put(long.class, 1);
        intWidening.put(float.class, 2);
        intWidening.put(double.class, 3);
        tempPrimitiveWidening.put(int.class, Collections.unmodifiableMap(intWidening));
        
        // long can widen to...
        Map<Class<?>, Integer> longWidening = new HashMap<>();
        longWidening.put(float.class, 1);
        longWidening.put(double.class, 2);
        tempPrimitiveWidening.put(long.class, Collections.unmodifiableMap(longWidening));
        
        // float can widen to...
        Map<Class<?>, Integer> floatWidening = new HashMap<>();
        floatWidening.put(double.class, 1);
        tempPrimitiveWidening.put(float.class, Collections.unmodifiableMap(floatWidening));
        
        // Note: boolean and double don't widen to anything
        
        // Make the outer map unmodifiable too
        PRIMITIVE_WIDENING_DISTANCES = Collections.unmodifiableMap(tempPrimitiveWidening);

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
        if (toType == null) {
            throw new IllegalArgumentException("toType cannot be null");
        }
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
        
        ClassHierarchyInfo(Set<Class<?>> supertypes, Map<Class<?>, Integer> distances) {
            this.allSupertypes = Collections.unmodifiableSet(supertypes);
            this.distanceMap = Collections.unmodifiableMap(distances);

            // Calculate depth as max BFS distance (works for both classes and interfaces)
            int max = 0;
            for (int d : distances.values()) {
                if (d > max) max = d;
            }
            this.depth = max;
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
     * @throws SecurityException if the class is blocked by SecurityChecker
     */
    public static void addPermanentClassAlias(Class<?> clazz, String alias) {
        SecurityChecker.verifyClass(clazz);
        USER_ALIASES.put(alias, clazz);
        GLOBAL_ALIASES.put(alias, clazz);
        // prevent stale per-loader mappings for this alias
        synchronized (NAME_CACHE) {
            for (LoaderCache holder : NAME_CACHE.values()) {
                removeCacheEntry(holder, alias);
            }
        }
    }

    /**
     * Removes a previously registered class alias.
     *
     * @param alias the alias name to remove
     */
    public static void removePermanentClassAlias(String alias) {
        USER_ALIASES.remove(alias);
        // If removing a user alias, check if there's a built-in alias to restore
        if (BUILTIN_ALIASES.containsKey(alias)) {
            GLOBAL_ALIASES.put(alias, BUILTIN_ALIASES.get(alias));
        } else {
            GLOBAL_ALIASES.remove(alias);
        }
        synchronized (NAME_CACHE) {
            for (LoaderCache holder : NAME_CACHE.values()) {
                removeCacheEntry(holder, alias);
            }
        }
    }

    /**
     * Computes the inheritance distance between two classes/interfaces/primitive types.
     * For reference types, distances are cached via ClassHierarchyInfo. For primitive types,
     * widening conversions are pre-computed in static maps.
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

        // Handle primitives specially - now with widening support
        boolean sp = isPrimitive(source);
        boolean dp = isPrimitive(destination);
        if (sp && dp) {
            // Get the actual primitive types (unwrap if needed)
            Class<?> sourcePrim = source.isPrimitive() ? source : WRAPPER_TO_PRIMITIVE.get(source);
            Class<?> destPrim = destination.isPrimitive() ? destination : WRAPPER_TO_PRIMITIVE.get(destination);
            
            if (sourcePrim != null && destPrim != null) {
                // Calculate widening distance (includes same type check)
                return getPrimitiveWideningDistance(sourcePrim, destPrim);
            }
            return -1; // Shouldn't happen if isPrimitive() is correct
        }
        
        // Special case: primitive/wrapper to reference type (e.g., int/Integer to Number)
        // This allows both int → Number and Integer → Number to work correctly
        if (sp && !dp) {
            // Source is primitive/wrapper, destination is reference type
            // Box the primitive if needed, then check hierarchy distance
            Class<?> src = source.isPrimitive() ? PRIMITIVE_TO_WRAPPER.get(source) : source;
            if (src != null) {
                return getClassHierarchyInfo(src).getDistance(destination);
            }
            return -1;
        }

        // Use the cached hierarchy info for non-primitive cases
        return getClassHierarchyInfo(source).getDistance(destination);
    }

    /**
     * Calculates the widening distance between two primitive types.
     * Returns 0 if they are the same type, positive distance for valid widening,
     * or -1 if no widening conversion exists.
     * 
     * @param sourcePrimitive The source primitive type (must be primitive)
     * @param destPrimitive The destination primitive type (must be primitive)
     * @return The widening distance, or -1 if no widening path exists
     */
    private static int getPrimitiveWideningDistance(Class<?> sourcePrimitive, Class<?> destPrimitive) {
        // Same type = distance 0
        if (sourcePrimitive.equals(destPrimitive)) {
            return 0;
        }
        
        // Check if there's a widening path
        Map<Class<?>, Integer> wideningMap = PRIMITIVE_WIDENING_DISTANCES.get(sourcePrimitive);
        if (wideningMap != null) {
            Integer distance = wideningMap.get(destPrimitive);
            if (distance != null) {
                return distance;
            }
        }
        
        // No widening path exists
        return -1;
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
            // Re-throw SecurityException directly for security tests
            throw e;
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
        Class<?> c = fromCache(name, classLoader);
        if (c != null) {
            // Check if this is a cached "class not found" sentinel
            if (c == CLASS_NOT_FOUND_SENTINEL) {
                throw new ClassNotFoundException("Class not found (cached): " + name);
            }
            // Performance: Skip re-verification - classes are verified before being cached
            // Cached classes are immutable, so if verified once, they remain verified
            return c;
        }

        // Check name before loading (quick rejection)
        if (SecurityChecker.isSecurityBlockedName(name)) {
            throw new SecurityException("For security reasons, cannot load: " + name);
        }

        // Performance: Only track depth if enhanced security is enabled
        // Avoids 3 ThreadLocal operations (get, set, set in finally) when disabled
        if (ENHANCED_SECURITY_ENABLED) {
            int currentDepth = CLASS_LOAD_DEPTH.get();
            int nextDepth = currentDepth + 1;
            // Performance: Direct constant check instead of method call + System.getProperty()
            if (nextDepth > MAX_CLASS_LOAD_DEPTH && MAX_CLASS_LOAD_DEPTH > 0) {
                throw new SecurityException("Class loading depth exceeded limit: " + nextDepth + " > " + MAX_CLASS_LOAD_DEPTH);
            }

            try {
                CLASS_LOAD_DEPTH.set(nextDepth);
                c = loadClass(name, classLoader);
            } catch (ClassNotFoundException e) {
                // Cache the negative result to avoid repeated failed lookups
                toCacheNotFound(name, classLoader);
                throw e;
            } finally {
                CLASS_LOAD_DEPTH.set(currentDepth);
            }
        } else {
            // Enhanced security disabled - skip ThreadLocal overhead entirely
            try {
                c = loadClass(name, classLoader);
            } catch (ClassNotFoundException e) {
                // Cache the negative result to avoid repeated failed lookups
                toCacheNotFound(name, classLoader);
                throw e;
            }
        }

        // Perform full security check on loaded class/component type.
        verifyClassAndArrayComponent(c);

        toCache(name, classLoader, c);
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
        // Support Java-style array names like "int[][]" or "java.lang.String[]"
        if (name.endsWith("]")) {
            int dims = 0;
            String base = name;
            while (base.endsWith("[]")) {
                dims++;
                base = base.substring(0, base.length() - 2);
            }
            Class<?> element;
            // primitives by simple name
            switch (base) {
                case "boolean": element = boolean.class; break;
                case "byte":    element = byte.class;    break;
                case "short":   element = short.class;   break;
                case "int":     element = int.class;     break;
                case "long":    element = long.class;    break;
                case "char":    element = char.class;    break;
                case "float":   element = float.class;   break;
                case "double":  element = double.class;  break;
                default:
                    if (SecurityChecker.isSecurityBlockedName(base)) {
                        throw new SecurityException("Class loading denied for security reasons: " + base);
                    }
                    if (classLoader != null) {
                        element = classLoader.loadClass(base);
                    } else {
                        element = Class.forName(base, false, getClassLoader(ClassUtilities.class));
                    }
            }
            Class<?> arrayClass = element;
            for (int i = 0; i < dims; i++) {
                arrayClass = Array.newInstance(arrayClass, 0).getClass();
            }
            return arrayClass;
        }

        // Optimized JVM descriptor handling - count brackets once to avoid re-string-bashing
        if (name.startsWith("[")) {
            int dims = 0;
            while (dims < name.length() && name.charAt(dims) == '[') {
                dims++;
            }
            
            if (dims >= name.length()) {
                throw new ClassNotFoundException("Bad descriptor: " + name);
            }
            
            Class<?> element;
            char typeChar = name.charAt(dims);
            
            // Java 8 compatible switch - handle primitive types
            switch (typeChar) {
                case 'B': element = byte.class; break;
                case 'S': element = short.class; break;
                case 'I': element = int.class; break;
                case 'J': element = long.class; break;
                case 'F': element = float.class; break;
                case 'D': element = double.class; break;
                case 'Z': element = boolean.class; break;
                case 'C': element = char.class; break;
                case 'L':
                    // Object type: extract class name from Lcom/example/Class;
                    if (!name.endsWith(";") || name.length() <= dims + 2) {
                        throw new ClassNotFoundException("Bad descriptor: " + name);
                    }
                    // Convert JVM descriptor format (java/lang/String) to Java format (java.lang.String)
                    String className = name.substring(dims + 1, name.length() - 1).replace('/', '.');
                    if (SecurityChecker.isSecurityBlockedName(className)) {
                        throw new SecurityException("Class loading denied for security reasons: " + className);
                    }
                    if (classLoader != null) {
                        element = classLoader.loadClass(className);
                    } else {
                        // Use the standard classloader resolution which handles OSGi/JPMS properly
                        ClassLoader cl = getClassLoader(ClassUtilities.class);
                        element = Class.forName(className, false, cl);
                    }
                    break;
                default:
                    throw new ClassNotFoundException("Bad descriptor: " + name);
            }
            
            // Build array class with the right number of dimensions
            Class<?> arrayClass = element;
            for (int i = 0; i < dims; i++) {
                arrayClass = Array.newInstance(arrayClass, 0).getClass();
            }
            return arrayClass;
        }
        
        // Regular class name (not an array)
        if (classLoader != null) {
            return classLoader.loadClass(name);
        } else {
            // Use the standard classloader resolution which handles OSGi/JPMS properly
            ClassLoader cl = getClassLoader(ClassUtilities.class);
            if (SecurityChecker.isSecurityBlockedName(name)) {
                throw new SecurityException("Class loading denied for security reasons: " + name);
            }
            return Class.forName(name, false, cl);
        }
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
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(c);
        
        // If no constructors declared, Java provides implicit public no-arg constructor
        if (constructors.length == 0) {
            return false;
        }

        for (Constructor<?> constructor : constructors) {
            if ((constructor.getModifiers() & Modifier.PRIVATE) == 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Converts primitive class to its corresponding wrapper class.
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
     * @throws IllegalArgumentException if the input class is null or not a recognized primitive type
     */
    public static Class<?> toPrimitiveWrapperClass(Class<?> primitiveClass) {
        if (primitiveClass == null) {
            throw new IllegalArgumentException("primitiveClass cannot be null");
        }
        
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
     * Converts a wrapper class to its corresponding primitive class.
     * If the passed in class is not a wrapper class, it returns the same class.
     * 
     * <p><strong>Examples:</strong></p>
     * <pre>{@code
     * Class<?> intPrimitive = ClassUtilities.toPrimitiveClass(Integer.class);   // Returns int.class
     * Class<?> boolPrimitive = ClassUtilities.toPrimitiveClass(Boolean.class);  // Returns boolean.class
     * Class<?> sameClass = ClassUtilities.toPrimitiveClass(String.class);       // Returns String.class
     * }</pre>
     *
     * @param wrapperClass the wrapper class to convert
     * @return the corresponding primitive class, or the same class if not a wrapper
     * @throws IllegalArgumentException if the passed in class is null
     */
    public static Class<?> toPrimitiveClass(Class<?> wrapperClass) {
        if (wrapperClass == null) {
            throw new IllegalArgumentException("Passed in class cannot be null");
        }

        Class<?> primitive = WRAPPER_TO_PRIMITIVE.get(wrapperClass);
        return primitive != null ? primitive : wrapperClass;
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
     * @return true if one class is the wrapper of the other, false otherwise.
     *         If either argument is {@code null}, this method returns {@code false}.
     */
    public static boolean doesOneWrapTheOther(Class<?> x, Class<?> y) {
        if (x == null || y == null) return false;
        return wrapperMap.get(x) == y || wrapperMap.get(y) == x;
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

        // Try context class loader first (may have OSGi classes in some containers)
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl != null) {
            return cl;
        }

        // Try anchor class loader
        cl = anchorClass.getClassLoader();
        if (cl != null) {
            return cl;
        }

        // Try OSGi if available
        cl = getOSGiClassLoader(anchorClass);
        if (cl != null) {
            return cl;
        }

        // Last resort
        return SYSTEM_LOADER;
    }

    /**
     * Checks if the current security manager allows class loader access.
     * <p>
     * This uses {@link SecurityManager}, which is deprecated in recent JDKs.
     * When no security manager is present, this method performs no checks.
     * </p>
     */
    @SuppressWarnings("removal")
    private static void checkSecurityAccess() {
        // SecurityManager is deprecated in Java 17+ and removed in Java 21+
        try {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new RuntimePermission("getClassLoader"));
            }
        } catch (UnsupportedOperationException e) {
            // Java 21+ - SecurityManager not available
            // In modern Java, rely on module system and other security mechanisms
            // No additional security check needed here
        }
    }

    /**
     * Attempts to retrieve the OSGi Bundle's ClassLoader.
     *
     * @param classFromBundle the class from which to get the bundle
     * @return the OSGi Bundle's ClassLoader if in an OSGi environment; otherwise, null
     */
    private static ClassLoader getOSGiClassLoader(final Class<?> classFromBundle) {
        ClassLoader cl = osgiClassLoaders.get(classFromBundle);
        if (cl != null) {
            return cl;
        }
        ClassLoader computed = getOSGiClassLoader0(classFromBundle);
        if (computed != null) {
            osgiClassLoaders.put(classFromBundle, computed);
        }
        return computed;
    }

    /**
     * Internal method to retrieve the OSGi Bundle's ClassLoader using reflection.
     *
     * @param classFromBundle the class from which to get the bundle
     * @return the OSGi Bundle's ClassLoader if in an OSGi environment; otherwise, null
     */
    private static ClassLoader getOSGiClassLoader0(final Class<?> classFromBundle) {
        try {
            // Use ClassUtilities' own classloader for consistent linkage
            // This ensures OSGi framework classes are loaded from the same source
            ClassLoader baseLoader = ClassUtilities.class.getClassLoader();
            if (baseLoader == null) {
                // Bootstrap classloader - use system classloader instead
                baseLoader = ClassLoader.getSystemClassLoader();
            }
            
            // Load the FrameworkUtil class from OSGi using explicit classloader
            Class<?> frameworkUtilClass = Class.forName("org.osgi.framework.FrameworkUtil", false, baseLoader);

            // Get the getBundle(Class<?>) method
            Method getBundleMethod = frameworkUtilClass.getMethod("getBundle", Class.class);

            // Invoke FrameworkUtil.getBundle(classFromBundle) to get the Bundle instance
            Object bundle = getBundleMethod.invoke(null, classFromBundle);

            if (bundle != null) {
                // Get BundleWiring class using the same classloader for consistency
                Class<?> bundleWiringClass = Class.forName("org.osgi.framework.wiring.BundleWiring", false, baseLoader);

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
     * @param defaultValue Default value to return if no suitable match is found
     * @return The value associated with the closest matching class, or defaultValue if no match found
     * @throws IllegalArgumentException if {@code clazz} or {@code candidateClasses} is null
     *
     * @see ClassUtilities#computeInheritanceDistance(Class, Class)
     */
    public static <T> T findClosest(Class<?> clazz, Map<Class<?>, T> candidateClasses, T defaultValue) {
        Convention.throwIfNull(clazz, "Source class cannot be null");
        Convention.throwIfNull(candidateClasses, "Candidate classes Map cannot be null");

        // First try exact match
        T exactMatch = candidateClasses.get(clazz);
        if (exactMatch != null) {
            return exactMatch;
        }

        // If no exact match, then look for closest inheritance match
        // Pull the distance map once to avoid repeated lookups
        Map<Class<?>, Integer> distanceMap = getClassHierarchyInfo(clazz).getDistanceMap();
        T closest = defaultValue;
        int minDistance = Integer.MAX_VALUE;
        Class<?> closestClass = null;

        for (Map.Entry<Class<?>, T> entry : candidateClasses.entrySet()) {
            Class<?> candidateClass = entry.getKey();
            Integer distance = distanceMap.get(candidateClass);
            if (distance != null && (distance < minDistance ||
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
        // Prefer the more specific class: newClass should be a subtype of currentClass
        return currentClass.isAssignableFrom(newClass);
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
        
        // Security: Validate and normalize resource path to prevent path traversal attacks
        resourceName = validateAndNormalizeResourcePath(resourceName);

        InputStream inputStream = null;
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader fallbackLoader = ClassUtilities.class.getClassLoader();
        if (fallbackLoader == null) {
            fallbackLoader = SYSTEM_LOADER;
        }

        if (contextLoader != null) {
            inputStream = contextLoader.getResourceAsStream(resourceName);
        }
        if (inputStream == null && fallbackLoader != null && fallbackLoader != contextLoader) {
            inputStream = fallbackLoader.getResourceAsStream(resourceName);
        }
        
        // ClassLoader.getResourceAsStream() doesn't handle leading slashes,
        // but Class.getResourceAsStream() does. Try without leading slash.
        if (inputStream == null && resourceName.startsWith("/")) {
            String noSlash = resourceName.substring(1);
            if (contextLoader != null) {
                inputStream = contextLoader.getResourceAsStream(noSlash);
            }
            if (inputStream == null && fallbackLoader != null && fallbackLoader != contextLoader) {
                inputStream = fallbackLoader.getResourceAsStream(noSlash);
            }
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
        // ByteArrayOutputStream.flush() is a no-op, removed unnecessary call
        return buffer.toByteArray();
    }

    private static Object getArgForType(com.cedarsoftware.util.convert.Converter converter, Class<?> argType) {
        // Only provide default values for actual primitives, not wrapper types
        // This avoids masking bugs where null wrapper values are silently converted to 0/false
        if (argType.isPrimitive()) {
            return converter.convert(null, argType);  // Get the defaults (false, 0, 0.0d, etc.)
        }

        Supplier<Object> directClassMapping = DIRECT_CLASS_MAPPING.get(argType);

        if (directClassMapping != null) {
            return directClassMapping.get();
        }

        // Check cache first to avoid repeated O(n) scans of ASSIGNABLE_CLASS_MAPPING
        Optional<Supplier<Object>> cached = ASSIGNABLE_TYPE_CACHE.get(argType);
        if (cached != null) {
            // Cache hit - return cached result (may be empty for "no match")
            return cached.map(Supplier::get).orElse(null);
        }

        // Cache miss - search ASSIGNABLE_CLASS_MAPPING and cache the result
        for (Map.Entry<Class<?>, Supplier<Object>> entry : ASSIGNABLE_CLASS_MAPPING.entrySet()) {
            if (entry.getKey().isAssignableFrom(argType)) {
                ASSIGNABLE_TYPE_CACHE.put(argType, Optional.of(entry.getValue()));
                return entry.getValue().get();
            }
        }

        // No match found - cache the negative result
        ASSIGNABLE_TYPE_CACHE.put(argType, Optional.empty());

        if (argType.isArray()) {
            return Array.newInstance(argType.getComponentType(), 0);
        }

        return null;
    }

    /**
     * Optimally match arguments to constructor parameters with minimal collection creation.
     *
     * @param converter Converter to use for type conversions
     * @param valueArray Collection of potential arguments
     * @param parameters Array of parameter types to match against
     * @param allowNulls Whether to allow null values for non-primitive parameters
     * @return Array of values matched to the parameters in the correct order
     */
    private static Object[] matchArgumentsToParameters(Converter converter, Object[] valueArray,
                                                       Parameter[] parameters, boolean allowNulls) {
        return matchArgumentsToParameters(converter, valueArray, parameters, 0, parameters.length, allowNulls);
    }

    private static Object[] matchArgumentsToParameters(Converter converter, Object[] valueArray,
                                                       Parameter[] parameters, int paramOffset,
                                                       int paramCount, boolean allowNulls) {
        if (parameters == null || paramCount <= 0) {
            return ArrayUtilities.EMPTY_OBJECT_ARRAY; // Reuse a static empty array
        }

        // Check if the last parameter is varargs and handle specially
        boolean isVarargs = parameters[paramOffset + paramCount - 1].isVarArgs();
        if (isVarargs) {
            return matchArgumentsWithVarargs(converter, valueArray, parameters, paramOffset, paramCount, allowNulls);
        }

        // Create result array and tracking arrays
        Object[] result = new Object[paramCount];
        boolean[] parameterMatched = new boolean[paramCount];

        // For tracking available values (more efficient than repeated removal from list)
        boolean[] valueUsed = new boolean[valueArray.length];

        // PHASE 1: Find exact type matches - highest priority
        findExactMatches(valueArray, valueUsed, parameters, paramOffset, paramCount, parameterMatched, result, 0, valueArray.length);

        // PHASE 2: Find assignable type matches with inheritance (includes primitive/wrapper via ClassHierarchyInfo)
        findInheritanceMatches(valueArray, valueUsed, parameters, paramOffset, paramCount, parameterMatched, result, 0, valueArray.length);

        // PHASE 3: Find convertible type matches
        findConvertibleMatches(converter, valueArray, valueUsed, parameters, paramOffset, paramCount, parameterMatched, result, 0, valueArray.length);

        // PHASE 4: Fill remaining unmatched parameters with defaults or nulls
        fillRemainingParameters(converter, parameters, paramOffset, paramCount, parameterMatched, result, allowNulls);

        return result;
    }
    
    /**
     * Special handling for varargs parameters. Matches fixed parameters first,
     * then packs remaining arguments into the varargs array.
     */
    private static Object[] matchArgumentsWithVarargs(Converter converter, Object[] valueArray,
                                                      Parameter[] parameters, int paramOffset,
                                                      int paramCount, boolean allowNulls) {
        int fixedParamCount = paramCount - 1;
        Object[] result = new Object[paramCount];

        // Get the varargs component type
        Class<?> varargsType = parameters[paramOffset + fixedParamCount].getType();
        Class<?> componentType = varargsType.getComponentType();

        // Special case: if we have exactly the right number of arguments and the last one
        // is already an array of the correct type, use it directly as the varargs array
        if (valueArray.length == paramCount) {
            Object lastArg = valueArray[valueArray.length - 1];
            if (lastArg != null && varargsType.isInstance(lastArg)) {
                // The last argument is already the right array type — match fixed parameters first
                if (fixedParamCount > 0) {
                    matchFixedParameters(converter, valueArray, 0, fixedParamCount,
                            parameters, paramOffset, fixedParamCount, result, allowNulls);
                }
                result[fixedParamCount] = lastArg;
                return result;
            }
        }

        // Determine which values are available for varargs after fixed parameter matching
        Object[] varargsSource;
        if (fixedParamCount > 0) {
            boolean[] valueUsed = new boolean[valueArray.length];
            matchFixedParameters(converter, valueArray, 0, valueArray.length, valueUsed,
                    parameters, paramOffset, fixedParamCount, result, allowNulls);

            // Collect unused values for varargs using a pre-sized array to avoid intermediate list allocations
            int unusedCount = 0;
            for (int i = 0; i < valueArray.length; i++) {
                if (!valueUsed[i]) {
                    unusedCount++;
                }
            }
            Object[] unused = new Object[unusedCount];
            int out = 0;
            for (int i = 0; i < valueArray.length; i++) {
                if (!valueUsed[i]) {
                    unused[out++] = valueArray[i];
                }
            }
            varargsSource = unused;
        } else {
            varargsSource = valueArray;
        }

        // Pack remaining values into the varargs array
        if (varargsSource.length == 1 && varargsType.isInstance(varargsSource[0])) {
            result[fixedParamCount] = varargsSource[0];
        } else {
            result[fixedParamCount] = packVarargsArray(converter, varargsSource, componentType);
        }

        return result;
    }

    /**
     * Match fixed (non-varargs) parameters using the standard matching pipeline.
     * Variant that does not expose the valueUsed array (for the pre-matched array case).
     */
    private static void matchFixedParameters(Converter converter, Object[] values,
                                             int valueStartInclusive, int valueEndExclusive,
                                             Parameter[] parameters, int paramOffset, int fixedParamCount,
                                             Object[] result, boolean allowNulls) {
        boolean[] valueUsed = new boolean[values.length];
        boolean[] parameterMatched = new boolean[fixedParamCount];

        findExactMatches(values, valueUsed, parameters, paramOffset, fixedParamCount, parameterMatched, result, valueStartInclusive, valueEndExclusive);
        findInheritanceMatches(values, valueUsed, parameters, paramOffset, fixedParamCount, parameterMatched, result, valueStartInclusive, valueEndExclusive);
        findConvertibleMatches(converter, values, valueUsed, parameters, paramOffset, fixedParamCount, parameterMatched, result, valueStartInclusive, valueEndExclusive);
        fillRemainingParameters(converter, parameters, paramOffset, fixedParamCount, parameterMatched, result, allowNulls);
    }

    /**
     * Match fixed (non-varargs) parameters using the standard matching pipeline.
     * Variant that populates the caller's valueUsed array so unused values can be collected.
     */
    private static void matchFixedParameters(Converter converter, Object[] values,
                                             int valueStartInclusive, int valueEndExclusive, boolean[] valueUsed,
                                             Parameter[] parameters, int paramOffset, int fixedParamCount,
                                             Object[] result, boolean allowNulls) {
        boolean[] parameterMatched = new boolean[fixedParamCount];

        findExactMatches(values, valueUsed, parameters, paramOffset, fixedParamCount, parameterMatched, result, valueStartInclusive, valueEndExclusive);
        findInheritanceMatches(values, valueUsed, parameters, paramOffset, fixedParamCount, parameterMatched, result, valueStartInclusive, valueEndExclusive);
        findConvertibleMatches(converter, values, valueUsed, parameters, paramOffset, fixedParamCount, parameterMatched, result, valueStartInclusive, valueEndExclusive);
        fillRemainingParameters(converter, parameters, paramOffset, fixedParamCount, parameterMatched, result, allowNulls);
    }

    /**
     * Convert and pack values into a typed array for varargs parameters.
     * Handles type conversion, primitive defaults, and ArrayStoreException guards.
     */
    private static Object packVarargsArray(Converter converter, Object[] values, Class<?> componentType) {
        Object array = Array.newInstance(componentType, values.length);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            // Try to convert if not already the right type
            if (value != null && !componentType.isInstance(value)) {
                try {
                    value = converter.convert(value, componentType);
                } catch (Exception e) {
                    // Conversion failed, keep original value
                }
            }
            // Guard against ArrayStoreException for still-incompatible values.
            // Note: isInstance() always returns false for primitive componentTypes (e.g., int.class),
            // so we must re-attempt conversion for primitives even if the first conversion succeeded.
            if (value != null && !componentType.isInstance(value)) {
                if (componentType.isPrimitive()) {
                    try {
                        value = converter.convert(value, componentType);
                    } catch (Exception e) {
                        value = getArgForType(converter, componentType);
                    }
                } else {
                    try {
                        value = converter.convert(value, componentType);
                    } catch (Exception e) {
                        value = null;
                    }
                }
            }
            try {
                ArrayUtilities.setElement(array, i, value);
            } catch (IllegalArgumentException ex) {
                ArrayUtilities.setElement(array, i, getArgForType(converter, componentType));
            }
        }
        return array;
    }

    /**
     * Find exact type matches between values and parameters
     */
    private static void findExactMatches(Object[] values, boolean[] valueUsed,
                                         Parameter[] parameters, int paramOffset, int paramCount,
                                         boolean[] parameterMatched, Object[] result,
                                         int valueStartInclusive, int valueEndExclusive) {
        for (int i = 0; i < paramCount; i++) {
            if (parameterMatched[i]) continue;

            Class<?> paramType = parameters[paramOffset + i].getType();

            for (int j = valueStartInclusive; j < valueEndExclusive; j++) {
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
                                               Parameter[] parameters, int paramOffset, int paramCount,
                                               boolean[] parameterMatched, Object[] result,
                                               int valueStartInclusive, int valueEndExclusive) {
        // Cache ClassHierarchyInfo lookups when multiple candidate values are available.
        // For single-value cases, direct lookup is cheaper than allocating a map.
        Map<Class<?>, ClassHierarchyInfo> valueClassCache = null;
        if (valueEndExclusive - valueStartInclusive > 1) {
            valueClassCache = new HashMap<>();
            // Pre-cache hierarchy info for all non-null, unused values
            for (int j = valueStartInclusive; j < valueEndExclusive; j++) {
                if (!valueUsed[j] && values[j] != null) {
                    Class<?> valueClass = values[j].getClass();
                    valueClassCache.computeIfAbsent(valueClass, ClassUtilities::getClassHierarchyInfo);
                }
            }
        }
        
        // For each unmatched parameter, find the best inheritance match
        for (int i = 0; i < paramCount; i++) {
            if (parameterMatched[i]) continue;

            Class<?> paramType = parameters[paramOffset + i].getType();
            int bestDistance = Integer.MAX_VALUE;
            int bestValueIndex = -1;

            for (int j = valueStartInclusive; j < valueEndExclusive; j++) {
                if (valueUsed[j]) continue;

                Object value = values[j];
                if (value == null) continue;

                Class<?> valueClass = value.getClass();
                ClassHierarchyInfo hierarchyInfo;
                if (valueClassCache != null) {
                    hierarchyInfo = valueClassCache.get(valueClass);
                    if (hierarchyInfo == null) {
                        hierarchyInfo = getClassHierarchyInfo(valueClass);
                        valueClassCache.put(valueClass, hierarchyInfo);
                    }
                } else {
                    hierarchyInfo = getClassHierarchyInfo(valueClass);
                }
                int distance = hierarchyInfo.getDistance(paramType);

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
     * Find matches that require type conversion
     */
    private static void findConvertibleMatches(Converter converter, Object[] values, boolean[] valueUsed,
                                               Parameter[] parameters, int paramOffset, int paramCount,
                                               boolean[] parameterMatched, Object[] result,
                                               int valueStartInclusive, int valueEndExclusive) {
        for (int i = 0; i < paramCount; i++) {
            if (parameterMatched[i]) continue;

            Class<?> paramType = parameters[paramOffset + i].getType();

            for (int j = valueStartInclusive; j < valueEndExclusive; j++) {
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
                                                int paramOffset, int paramCount,
                                                boolean[] parameterMatched, Object[] result,
                                                boolean allowNulls) {
        for (int i = 0; i < paramCount; i++) {
            if (parameterMatched[i]) continue;

            Parameter parameter = parameters[paramOffset + i];
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
     * @param c Class to instantiate
     * @param arguments Can be:
     *                  - null or empty (no-arg constructor)
     *                  - {@code Map<String, Object>} to match by parameter name (when available) or type
     *                    Note: When named parameter matching fails, falls back to positional matching.
     *                    For deterministic behavior, values are ordered by:
     *                    • LinkedHashMap/SortedMap: preserves existing order
     *                    • HashMap: sorts keys alphabetically
     *                  - {@code Collection<?>} of values to match by type
     *                  - Object[] of values to match by type
     *                  - Single value for single-argument constructors
     * @return A new instance of the specified class
     * @throws IllegalArgumentException if the class cannot be instantiated or arguments are invalid
     */
    public static Object newInstance(Class<?> c, Object arguments) {
        // Use the legacy Converter's getInstance() which provides a shared instance
        // of the new Converter with default options. This is fine since ClassUtilities
        // only needs basic conversions that don't require special options.
        return newInstance(com.cedarsoftware.util.Converter.getInstance(), c, arguments);
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
     * @param arguments Can be:
     *                  - null or empty (no-arg constructor)
     *                  - {@code Map<String, Object>} to match by parameter name (when available) or type
     *                    Note: When named parameter matching fails, falls back to positional matching.
     *                    For deterministic behavior, values are ordered by:
     *                    • LinkedHashMap/SortedMap: preserves existing order
     *                    • HashMap: sorts keys alphabetically
     *                  - {@code Collection<?>} of values to match by type
     *                  - Object[] of values to match by type
     *                  - Single value for single-argument constructors
     * @return A new instance of the specified class
     * @throws IllegalArgumentException if the class cannot be instantiated or arguments are invalid
     */
    @SuppressWarnings("unchecked")
    public static Object newInstance(Converter converter, Class<?> c, Object arguments) {
        Convention.throwIfNull(c, "Class cannot be null");
        Convention.throwIfNull(converter, "Converter cannot be null");

        // Normalize arguments to Collection format for existing code
        Collection<?> normalizedArgs = null;
        Map<String, Object> namedParameters = null;
        boolean hasNamedParameters = false;
        Map<String, Object> mapArguments = null;

        if (arguments == null) {
            normalizedArgs = Collections.emptyList();
        } else if (arguments instanceof Collection) {
            normalizedArgs = (Collection<?>) arguments;
        } else if (arguments instanceof Map) {
            mapArguments = (Map<String, Object>) arguments;

            // Check once if we have generated keys
            boolean generatedKeys = hasGeneratedKeys(mapArguments);

            if (!generatedKeys) {
                hasNamedParameters = true;
                namedParameters = mapArguments;
            }
        } else if (arguments.getClass().isArray()) {
            normalizedArgs = converter.convert(arguments, Collection.class);
        } else {
            // Single value - wrap in collection
            normalizedArgs = Collections.singletonList(arguments);
        }

        // Try parameter name matching first if we have named parameters
        if (hasNamedParameters && namedParameters != null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Attempting parameter name matching for class: {0}", c.getName());
            }
            if (LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, "Provided parameter names: {0}", namedParameters.keySet());
            }

            try {
                Object result = newInstanceWithNamedParameters(converter, c, namedParameters);
                if (result != null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Successfully created instance of {0} using parameter names", c.getName());
                    }
                    return result;
                }
            } catch (Exception e) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Parameter name matching failed for {0}: {1}", new Object[]{c.getName(), e.getMessage()});
                }
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Falling back to positional argument matching");
                }
            }
        }

        // Lazy normalization for Map fallback path only.
        if (normalizedArgs == null && mapArguments != null) {
            normalizedArgs = normalizeMapValuesForPositionalFallback(mapArguments);
        }

        // Call existing implementation
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Using positional argument matching for {0}", c.getName());
        }
        Set<Class<?>> visited = new IdentitySet<>();

        try {
            return newInstance(converter, c, normalizedArgs, visited);
        } catch (Exception e) {
            // If we were trying with map values and it failed, try with null (no-arg constructor)
            if (arguments instanceof Map && normalizedArgs != null && !normalizedArgs.isEmpty()) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Positional matching with map values failed for {0}, trying no-arg constructor", c.getName());
                }
                return newInstance(converter, c, null, visited);
            }
            throw e;
        }
    }

    private static Collection<?> normalizeMapValuesForPositionalFallback(Map<String, Object> map) {
        boolean generatedKeys = hasGeneratedKeys(map);

        // Convert map values to collection for fallback
        if (generatedKeys) {
            // Fast path for dense generated keys: arg0..argN with no gaps.
            // This avoids allocating/sorting Map.Entry objects.
            int size = map.size();
            Object[] indexed = new Object[size];
            boolean[] seen = new boolean[size];
            int seenCount = 0;

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                int index;
                try {
                    index = Integer.parseInt(entry.getKey().substring(3));
                } catch (RuntimeException ignored) {
                    // Unexpected malformed/overflow key - use deterministic fallback below.
                    seenCount = -1;
                    break;
                }

                if (index < 0 || index >= size || seen[index]) {
                    // Gap, out-of-range key (for this map size), or duplicate index.
                    // Use deterministic sort fallback to preserve prior behavior.
                    seenCount = -1;
                    break;
                }

                indexed[index] = entry.getValue();
                seen[index] = true;
                seenCount++;
            }

            if (seenCount == size) {
                return Arrays.asList(indexed);
            }

            // Fallback: preserve deterministic ordering with gaps (e.g., arg0,arg2).
            List<Map.Entry<String, Object>> entries = new ArrayList<>(map.entrySet());
            entries.sort((e1, e2) -> {
                try {
                    int num1 = Integer.parseInt(e1.getKey().substring(3));
                    int num2 = Integer.parseInt(e2.getKey().substring(3));
                    return Integer.compare(num1, num2);
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                    return e1.getKey().compareTo(e2.getKey());
                }
            });

            List<Object> orderedValues = new ArrayList<>(entries.size());
            for (Map.Entry<String, Object> entry : entries) {
                orderedValues.add(entry.getValue());
            }
            return orderedValues;
        }

        // For non-generated keys, we need deterministic ordering for positional fallback
        // Sort by key name alphabetically to ensure consistent behavior across JVM runs
        // This is important when HashMap is used (which has non-deterministic iteration order)
        if (map instanceof LinkedHashMap || map instanceof SortedMap) {
            // Already has deterministic order (insertion order or sorted)
            return map.values();
        }

        // Sort keys alphabetically for deterministic order
        List<String> sortedKeys = new ArrayList<>(map.keySet());
        Collections.sort(sortedKeys);
        List<Object> orderedValues = new ArrayList<>(sortedKeys.size());
        for (String key : sortedKeys) {
            orderedValues.add(map.get(key));
        }
        return orderedValues;
    }

    private static Object newInstanceWithNamedParameters(Converter converter, Class<?> c, Map<String, Object> namedParams) {
        // Get all constructors using ReflectionUtils for caching
        Constructor<?>[] sortedConstructors = ReflectionUtils.getAllConstructors(c);

        boolean isFinal = Modifier.isFinal(c.getModifiers());
        boolean isException = Throwable.class.isAssignableFrom(c);

        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Class {0} is {1}{2}",
                    new Object[]{c.getName(),
                            isFinal ? "final" : "non-final",
                            isException ? " (Exception type)" : ""});

            LOG.log(Level.FINER, "Trying {0} constructors for {1}",
                    new Object[]{sortedConstructors.length, c.getName()});
        }

        Boolean namedMatchingViable = NAMED_PARAMETER_MATCHING_VIABLE_CACHE.get(c);
        if (namedMatchingViable == null) {
            namedMatchingViable = isNamedParameterMatchingViable(sortedConstructors);
            NAMED_PARAMETER_MATCHING_VIABLE_CACHE.put(c, namedMatchingViable);
        }

        // If no constructors have real parameter names (and class has parameterized ctors), bail out early.
        if (!namedMatchingViable) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "No constructors for {0} have real parameter names - cannot use parameter matching", c.getName());
            }
            return null; // This will trigger fallback to positional matching
        }

        for (Constructor<?> constructor : sortedConstructors) {
            try {
                trySetAccessible(constructor);
            } catch (SecurityException se) {
                // Can't make this constructor accessible under JPMS; try the next one
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Cannot access constructor {0} due to security restrictions: {1}", 
                            new Object[]{constructor, se.getMessage()});
                }
                continue;
            }
            if (LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, "Trying constructor: {0}", constructor);
            }

            Parameter[] parameters = constructor.getParameters();
            boolean hasRealNames = true;

            for (int i = 0; i < parameters.length; i++) {
                String paramName = parameters[i].getName();

                if (LOG.isLoggable(Level.FINEST)) {
                    LOG.log(Level.FINEST, "  Parameter {0}: name=''{1}'', type={2}",
                            new Object[]{i, paramName, parameters[i].getType().getSimpleName()});
                }

                // Check if we have real parameter names or just arg0, arg1, etc.
                if (isSyntheticArgName(paramName)) {
                    hasRealNames = false;
                }
            }

            if (!hasRealNames && parameters.length > 0) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "  Skipping constructor - parameter names not available");
                }
                continue; // Skip this constructor for parameter matching
            }

            // Try to match all parameters
            Object[] args = new Object[parameters.length];
            boolean allMatched = true;

            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isVarArgs()) {
                    // Handle varargs parameter specially
                    Class<?> arrayType = parameters[i].getType();
                    Class<?> componentType = arrayType.getComponentType();
                    String paramName = parameters[i].getName();
                    boolean hasVarargsValue = namedParams.containsKey(paramName);
                    Object v = hasVarargsValue ? namedParams.get(paramName) : null;
                    Object array;

                    if (!hasVarargsValue) {
                        array = Array.newInstance(componentType, 0);
                    } else if (v != null && arrayType.isInstance(v)) {
                        // Already the right array type
                        array = v;
                    } else {
                        // Convert single value or collection to array
                        Collection<?> src = (v instanceof Collection) ? (Collection<?>) v : Collections.singletonList(v);
                        array = Array.newInstance(componentType, src.size());
                        int k = 0;
                        for (Object item : src) {
                            try {
                                ArrayUtilities.setElement(array, k++, converter.convert(item, componentType));
                            } catch (Exception e) {
                                // Use default value if conversion fails
                                ArrayUtilities.setElement(array, k++, getArgForType(converter, componentType));
                            }
                        }
                    }
                    args[i] = array;
                    
                    if (LOG.isLoggable(Level.FINEST)) {
                        LOG.log(Level.FINEST, "  Matched varargs parameter ''{0}'' with array of length: {1}",
                                new Object[]{paramName, ArrayUtilities.getLength(array)});
                    }
                    continue;
                }
                
                String paramName = parameters[i].getName();
                if (namedParams.containsKey(paramName)) {
                    Object value = namedParams.get(paramName);

                    try {
                        // Handle null values - don't convert null for non-primitive types
                        if (value == null) {
                            // If it's a primitive type, we can't use null
                            if (parameters[i].getType().isPrimitive()) {
                                // Let converter handle conversion to primitive default values
                                args[i] = converter.convert(value, parameters[i].getType());
                            } else {
                                // For object types, just use null directly
                                args[i] = null;
                            }
                        } else if (parameters[i].getType().isInstance(value)) {
                            // Value is already the right type
                            args[i] = value;
                        } else {
                            // Convert if necessary
                            args[i] = converter.convert(value, parameters[i].getType());
                        }

                        if (LOG.isLoggable(Level.FINEST)) {
                            LOG.log(Level.FINEST, "  Matched parameter ''{0}'' with value: {1}",
                                    new Object[]{paramName, value});
                        }
                    } catch (Exception conversionException) {
                        allMatched = false;
                        break;
                    }
                } else {
                    if (LOG.isLoggable(Level.FINER)) {
                        LOG.log(Level.FINER, "  Missing parameter: {0}", paramName);
                    }
                    allMatched = false;
                    break;
                }
            }

            if (allMatched) {
                try {
                    Object instance = constructor.newInstance(args);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "  Successfully created instance of {0}", c.getName());
                    }
                    return instance;
                } catch (Exception e) {
                    if (LOG.isLoggable(Level.FINER)) {
                        LOG.log(Level.FINER, "  Failed to invoke constructor: {0}", e.getMessage());
                    }
                }
            }
        }

        return null; // Indicate failure to create with named parameters
    }

    /**
     * Named parameter matching is viable when either:
     * 1) There are no parameterized constructors, or
     * 2) At least one parameterized constructor exposes real (non-synthetic) parameter names.
     */
    private static boolean isNamedParameterMatchingViable(Constructor<?>[] constructors) {
        boolean hasParameterizedConstructor = false;
        for (Constructor<?> constructor : constructors) {
            Parameter[] parameters = constructor.getParameters();
            if (parameters.length == 0) {
                continue;
            }

            hasParameterizedConstructor = true;
            boolean allRealNames = true;
            for (Parameter param : parameters) {
                if (isSyntheticArgName(param.getName())) {
                    allRealNames = false;
                    break;
                }
            }
            if (allRealNames) {
                return true;
            }
        }
        return !hasParameterizedConstructor;
    }
    
    /**
     * Checks if a parameter name is a synthetic name like "arg0", "arg1", etc.
     * This is more efficient than using ARG_PATTERN.matcher().matches() as it
     * avoids creating Matcher objects.
     */
    private static boolean isSyntheticArgName(String name) {
        if (name == null || name.length() < 4 || !name.startsWith("arg")) {
            return false;
        }
        for (int i = 3; i < name.length(); i++) {
            if (!Character.isDigit(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if the map has generated keys (arg0, arg1, etc.)
     */
    private static boolean hasGeneratedKeys(Map<String, Object> map) {
        if (map.isEmpty()) {
            return false;
        }
        // Check if all keys match the pattern arg0, arg1, etc.
        for (String key : map.keySet()) {
            if (!isSyntheticArgName(key)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @deprecated Use {@link #newInstance(Converter, Class, Object)} instead.
     * @param converter Converter instance
     * @param c Class to instantiate
     * @param argumentValues Collection of constructor arguments
     * @return A new instance of the specified class
     * @see #newInstance(Converter, Class, Object)
     */
    @Deprecated
    public static Object newInstance(Converter converter, Class<?> c, Collection<?> argumentValues) {
        return newInstance(converter, c, (Object) argumentValues);
    }

    private static Object newInstance(Converter converter, Class<?> c, Collection<?> argumentValues,
                                      Set<Class<?>> visitedClasses) {
        Convention.throwIfNull(c, "Class cannot be null");

        // Do security check FIRST
        SecurityChecker.verifyClass(c);

        // Enhanced security: Validate constructor argument count
        if (argumentValues != null) {
            validateEnhancedSecurity("Constructor argument", argumentValues.size(), getMaxConstructorArgs());
        }

        if (visitedClasses.contains(c)) {
            throw new IllegalStateException("Circular reference detected for " + c.getName());
        }

        // Then do other validations
        if (c.isInterface()) {
            throw new IllegalArgumentException("Cannot instantiate interface: " + c.getName());
        }
        if (Modifier.isAbstract(c.getModifiers())) {
            throw new IllegalArgumentException("Cannot instantiate abstract class: " + c.getName());
        }
        
        // Prepare arguments
        List<Object> normalizedArgs = argumentValues == null ? new ArrayList<>() : new ArrayList<>(argumentValues);
        Object[] suppliedArgs = normalizedArgs.isEmpty() ? ArrayUtilities.EMPTY_OBJECT_ARRAY : normalizedArgs.toArray();
        ArgumentShapeKey argumentShapeKey = ArgumentShapeKey.from(suppliedArgs);
        Map<ArgumentShapeKey, Optional<ConstructorPlan>> classPlanCache = getConstructorPlanCache(c);

        Optional<ConstructorPlan> cachedPlanResult = classPlanCache.get(argumentShapeKey);
        if (cachedPlanResult != null) {
            if (!cachedPlanResult.isPresent()) {
                Object instance = tryUnsafeInstantiation(c);
                if (instance != null) {
                    return instance;
                }
                throw new IllegalArgumentException("Unable to instantiate (cached): " + c.getName());
            }

            try {
                return invokeConstructorWithPlan(converter, suppliedArgs, cachedPlanResult.get());
            } catch (Exception ignored) {
                // Evict stale plan and continue with normal constructor discovery.
                classPlanCache.remove(argumentShapeKey);
            }
        }
        
        // Fast-path: zero-arg constructor - common case that avoids the whole matching pipeline
        if (normalizedArgs.isEmpty()) {
            try {
                Constructor<?> noArg = c.getDeclaredConstructor();
                trySetAccessible(noArg);
                Object instance = noArg.newInstance();
                SUCCESSFUL_CONSTRUCTOR_CACHE.put(c, Optional.of(noArg));
                classPlanCache.put(argumentShapeKey, Optional.of(new ConstructorPlan(noArg, false)));
                return instance;
            } catch (NoSuchMethodException ignored) {
                // No no-arg constructor, fall through to normal logic
            } catch (SecurityException se) {
                // Can't access no-arg constructor under JPMS, fall through
            } catch (Exception e) {
                // No-arg constructor failed, fall through to try other constructors
            }
        }

        // Check if we have a previously cached result for this class
        Optional<Constructor<?>> cachedResult = suppliedArgs.length == 0 ? SUCCESSFUL_CONSTRUCTOR_CACHE.get(c) : null;

        if (cachedResult != null) {
            if (!cachedResult.isPresent()) {
                // Cached negative result - no constructor works, go directly to unsafe
                Object instance = tryUnsafeInstantiation(c);
                if (instance != null) {
                    return instance;
                }
                throw new IllegalArgumentException("Unable to instantiate (cached): " + c.getName());
            }

            // Cached successful constructor
            Constructor<?> cachedConstructor = cachedResult.get();
            try {
                Parameter[] parameters = cachedConstructor.getParameters();

                // Try both approaches with the cached constructor
                try {
                    Object[] argsNonNull = matchArgumentsToParameters(converter, suppliedArgs, parameters, false);
                    Object instance = cachedConstructor.newInstance(argsNonNull);
                    classPlanCache.put(argumentShapeKey, Optional.of(new ConstructorPlan(cachedConstructor, false)));
                    return instance;
                } catch (Exception e) {
                    Object[] argsNull = matchArgumentsToParameters(converter, suppliedArgs, parameters, true);
                    Object instance = cachedConstructor.newInstance(argsNull);
                    classPlanCache.put(argumentShapeKey, Optional.of(new ConstructorPlan(cachedConstructor, true)));
                    return instance;
                }
            } catch (Exception ignored) {
                // If cached constructor fails, continue with regular instantiation
                // and potentially update the cache
            }
        }

        // Handle inner classes - with circular reference protection
        if (c.getEnclosingClass() != null && !Modifier.isStatic(c.getModifiers())) {
            visitedClasses.add(c);

            try {
                // For inner classes, try to get the enclosing instance
                Class<?> enclosingClass = c.getEnclosingClass();
                if (!visitedClasses.contains(enclosingClass)) {
                    // Try to create enclosing instance with proper constructor initialization
                    Object enclosingInstance;
                    try {
                        // First try default constructor if available
                        Constructor<?> defaultCtor = enclosingClass.getDeclaredConstructor();
                        trySetAccessible(defaultCtor);
                        enclosingInstance = defaultCtor.newInstance();
                    } catch (Exception e) {
                        // Fall back to creating with empty args (may use Unsafe)
                        enclosingInstance = newInstance(converter, enclosingClass, Collections.emptyList(), visitedClasses);
                    }
                    
                    // Try all constructors where the first parameter is the enclosing class
                    Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(c);
                    for (Constructor<?> constructor : constructors) {
                        Parameter[] params = constructor.getParameters();
                        if (params.length > 0 && params[0].getType().equals(enclosingClass)) {
                            try {
                                trySetAccessible(constructor);

                                if (params.length == 1) {
                                    // Simple case: only takes enclosing instance
                                    Object instance = constructor.newInstance(enclosingInstance);
                                    SUCCESSFUL_CONSTRUCTOR_CACHE.put(c, Optional.of(constructor));
                                    classPlanCache.put(argumentShapeKey, Optional.of(new ConstructorPlan(constructor, false)));
                                    return instance;
                                } else {
                                    // Complex case: takes enclosing instance plus more arguments
                                    // Create arguments array with enclosing instance first
                                    Object[] restArgs = matchArgumentsToParameters(converter, suppliedArgs, params, 1, params.length - 1, false);
                                    Object[] allArgs = new Object[params.length];
                                    allArgs[0] = enclosingInstance;
                                    System.arraycopy(restArgs, 0, allArgs, 1, restArgs.length);

                                    Object instance = constructor.newInstance(allArgs);
                                    SUCCESSFUL_CONSTRUCTOR_CACHE.put(c, Optional.of(constructor));
                                    classPlanCache.put(argumentShapeKey, Optional.of(new ConstructorPlan(constructor, false)));
                                    return instance;
                                }
                            } catch (Exception e) {
                                // Try next constructor
                            }
                        }
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
            try {
                trySetAccessible(constructor);
            } catch (SecurityException se) {
                // Can't make this constructor accessible under JPMS; try the next one
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "Cannot access constructor {0} due to security restrictions: {1}", 
                            new Object[]{constructor, se.getMessage()});
                }
                continue;
            }
            Parameter[] parameters = constructor.getParameters();

            // Attempt instantiation with this constructor
            try {
                // Try with non-null arguments first (more precise matching)
                Object[] argsNonNull = matchArgumentsToParameters(converter, suppliedArgs, parameters, false);
                Object instance = constructor.newInstance(argsNonNull);

                // Cache this successful constructor for future use
                SUCCESSFUL_CONSTRUCTOR_CACHE.put(c, Optional.of(constructor));
                classPlanCache.put(argumentShapeKey, Optional.of(new ConstructorPlan(constructor, false)));
                return instance;
            } catch (Exception e1) {
                exceptions.add(e1);

                // If that fails, try with nulls allowed for unmatched parameters
                try {
                    Object[] argsNull = matchArgumentsToParameters(converter, suppliedArgs, parameters, true);
                    Object instance = constructor.newInstance(argsNull);

                    // Cache this successful constructor for future use
                    SUCCESSFUL_CONSTRUCTOR_CACHE.put(c, Optional.of(constructor));
                    classPlanCache.put(argumentShapeKey, Optional.of(new ConstructorPlan(constructor, true)));
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
            classPlanCache.put(argumentShapeKey, Optional.empty());
            return instance;
        }

        classPlanCache.put(argumentShapeKey, Optional.empty());

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

    private static Map<ArgumentShapeKey, Optional<ConstructorPlan>> getConstructorPlanCache(Class<?> c) {
        return CONSTRUCTOR_PLAN_CACHE.computeIfAbsent(c, key -> new ConcurrentHashMap<>());
    }

    private static Object invokeConstructorWithPlan(Converter converter, Object[] suppliedArgs, ConstructorPlan plan) throws Exception {
        Parameter[] parameters = plan.constructor.getParameters();
        if (plan.allowNullsFirst) {
            try {
                Object[] argsNull = matchArgumentsToParameters(converter, suppliedArgs, parameters, true);
                return plan.constructor.newInstance(argsNull);
            } catch (Exception ignored) {
                Object[] argsNonNull = matchArgumentsToParameters(converter, suppliedArgs, parameters, false);
                return plan.constructor.newInstance(argsNonNull);
            }
        }
        try {
            Object[] argsNonNull = matchArgumentsToParameters(converter, suppliedArgs, parameters, false);
            return plan.constructor.newInstance(argsNonNull);
        } catch (Exception ignored) {
            Object[] argsNull = matchArgumentsToParameters(converter, suppliedArgs, parameters, true);
            return plan.constructor.newInstance(argsNull);
        }
    }

    // Cache for tracking which AccessibleObjects we've already tried to make accessible
    // Uses WeakHashMap to allow GC of classes/methods when no longer referenced
    // Uses Collections.synchronizedMap wrapper for thread-safety
    private static final Map<AccessibleObject, Boolean> accessibilityCache = 
        Collections.synchronizedMap(new WeakHashMap<>());
    
    static void trySetAccessible(AccessibleObject object) {
        // Check cache for known failures only. We only cache FALSE (failures) to avoid
        // expensive repeated exception throwing on JPMS-sealed modules. We do NOT cache
        // TRUE because WeakHashMap uses equals()-based lookup, and Field.equals() matches
        // by declaring class + name + type. Different Field instances for the same logical
        // field (from separate getDeclaredFields() calls) would incorrectly share a TRUE
        // cache entry, causing the second instance to never get setAccessible(true) called.
        // Calling setAccessible(true) on an already-accessible field is a cheap no-op.
        if (Boolean.FALSE.equals(accessibilityCache.get(object))) {
            return;
        }

        try {
            object.setAccessible(true);
        } catch (SecurityException e) {
            accessibilityCache.put(object, Boolean.FALSE);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Unable to set accessible: " + object + " - " + e.getMessage());
            }
            throw e; // Don't suppress security exceptions - they indicate important access control violations
        } catch (Throwable t) {
            // Only ignore non-security exceptions (like InaccessibleObjectException in Java 9+)
            accessibilityCache.put(object, Boolean.FALSE);
            safelyIgnoreException(t);
        }
    }

    // Try instantiation via ReflectionFactory serialization constructor (if turned on).
    // It is off by default.  Use ClassUtilities.setUseUnsafe(true) to enable it.
    // This uses the same mechanism as ObjectInputStream — creates a synthetic constructor
    // that runs Object.<init>() instead of the class's own constructors.
    private static Object tryUnsafeInstantiation(Class<?> c) {
        if (unsafeDepth.get() > 0) {
            try {
                SecurityChecker.verifyClass(c);
                return unsafe.allocateInstance(c);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * Turn on (or off) the 'unsafe' option of Class construction for the current thread only.
     * When enabled, allows constructor-bypassing instantiation as a last resort when no
     * suitable constructor can be found.
     * <p>
     * This uses {@code ReflectionFactory.newConstructorForSerialization()} — the same mechanism
     * used by {@code ObjectInputStream} for deserialization. The synthetic constructor runs
     * {@code Object.<init>()} and fields get Java default values (null/0/false).
     * <p>
     * This setting is thread-local and does not affect other threads.
     *
     * @param state boolean true = on, false = off (for the current thread only)
     */
    public static void setUseUnsafe(boolean state) {
        if (state) {
            unsafeDepth.set(unsafeDepth.get() + 1);
            // Initialize singleton on first enable
            if (unsafe == null) {
                synchronized (ClassUtilities.class) {
                    if (unsafe == null) {
                        try {
                            unsafe = new Unsafe();
                        } catch (Exception e) {
                            // Failed to initialize - revert the increment
                            unsafeDepth.set(unsafeDepth.get() - 1);
                            if (LOG.isLoggable(Level.FINE)) {
                                LOG.log(Level.FINE, "Failed to initialize ReflectionFactory instantiation: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        } else {
            int depth = unsafeDepth.get();
            if (depth > 0) {
                unsafeDepth.set(depth - 1);
            }
        }
    }

    /**
     * Cached reference to InaccessibleObjectException class (Java 9+), or null if not available
     */
    private static final Class<?> INACCESSIBLE_OBJECT_EXCEPTION_CLASS;

    static {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("java.lang.reflect.InaccessibleObjectException");
        } catch (ClassNotFoundException e) {
            // Java 8 or earlier - this exception doesn't exist
        }
        INACCESSIBLE_OBJECT_EXCEPTION_CLASS = clazz;
    }

    /**
     * Logs reflection access issues in a concise, readable format without stack traces.
     * Useful for expected access failures due to module restrictions or private access.
     *
     * @param accessible The field, method, or constructor that couldn't be accessed
     * @param e The exception that was thrown
     * @param operation Description of what was being attempted (e.g., "read field", "invoke method")
     */
    public static void logAccessIssue(AccessibleObject accessible, Exception e, String operation) {
        if (!LOG.isLoggable(Level.FINEST)) {
            return;
        }

        String elementType;
        String elementName;
        String declaringClass;
        String modifiers;

        if (accessible instanceof Field) {
            Field field = (Field) accessible;
            elementType = "field";
            elementName = field.getName();
            declaringClass = field.getDeclaringClass().getName();
            modifiers = Modifier.toString(field.getModifiers());
        } else if (accessible instanceof Method) {
            Method method = (Method) accessible;
            elementType = "method";
            elementName = method.getName() + "()";
            declaringClass = method.getDeclaringClass().getName();
            modifiers = Modifier.toString(method.getModifiers());
        } else if (accessible instanceof Constructor) {
            Constructor<?> constructor = (Constructor<?>) accessible;
            elementType = "constructor";
            elementName = constructor.getDeclaringClass().getSimpleName() + "()";
            declaringClass = constructor.getDeclaringClass().getName();
            modifiers = Modifier.toString(constructor.getModifiers());
        } else {
            elementType = "member";
            elementName = accessible.toString();
            declaringClass = "unknown";
            modifiers = "";
        }

        // Determine the reason for the access failure
        String reason = null;
        if (e instanceof IllegalAccessException) {
            String msg = e.getMessage();
            if (msg != null) {
                if (msg.contains("module")) {
                    reason = "Java module system restriction";
                } else if (msg.contains("private")) {
                    reason = "private access";
                } else if (msg.contains("protected")) {
                    reason = "protected access";
                } else if (msg.contains("package")) {
                    reason = "package-private access";
                }
            }
        } else if (INACCESSIBLE_OBJECT_EXCEPTION_CLASS != null &&
                INACCESSIBLE_OBJECT_EXCEPTION_CLASS.isInstance(e)) {
            reason = "Java module system restriction (InaccessibleObjectException)";
        } else if (e instanceof SecurityException) {
            reason = "Security manager restriction";
        }

        if (reason == null) {
            reason = e.getClass().getSimpleName();
        }

        // Log the concise message
        if (LOG.isLoggable(Level.FINEST)) {
            if (operation != null && !operation.isEmpty()) {
                LOG.log(Level.FINEST, "Cannot {0} {1} {2} ''{3}'' on {4} ({5})",
                        new Object[]{operation, modifiers, elementType, elementName, declaringClass, reason});
            } else {
                LOG.log(Level.FINEST, "Cannot access {0} {1} ''{2}'' on {3} ({4})",
                        new Object[]{modifiers, elementType, elementName, declaringClass, reason});
            }
        }
    }
    
    /**
     * Security: Validate and normalize resource path to prevent path traversal attacks.
     * 
     * @param resourceName The resource name to validate
     * @return The normalized resource path (with backslashes converted to forward slashes)
     * @throws SecurityException if the resource path is potentially dangerous
     */
    private static String validateAndNormalizeResourcePath(String resourceName) {
        if (StringUtilities.isEmpty(resourceName)) {
            throw new SecurityException("Resource name cannot be null or empty");
        }
        
        // Security: Block null bytes which can truncate paths
        if (resourceName.indexOf('\0') >= 0) {
            throw new SecurityException("Invalid resource path contains null byte: " + resourceName);
        }
        
        // Security: Block percent-encoded traversal sequences before normalization
        // Check for %2e%2e (percent-encoded ..) and %2e%2E and other case variations
        String lowerPath = resourceName.toLowerCase(Locale.ROOT);
        if (lowerPath.contains("%2e%2e") || lowerPath.contains("%252e") || 
            lowerPath.contains("%2e.") || lowerPath.contains(".%2e")) {
            throw new SecurityException("Invalid resource path contains encoded traversal sequence: " + resourceName);
        }
        
        // Normalize backslashes to forward slashes for Windows developers
        // This is safe because JAR resources always use forward slashes
        String normalizedPath = resourceName.replace('\\', '/');
        
        // Security: Block absolute Windows drive paths (e.g., "C:/...", "D:/...")
        // and UNC paths (e.g., "//server/share/...")
        // These should never appear in classpath resource lookups
        final int pathLength = normalizedPath.length();
        
        // Check for Windows absolute path (e.g., "C:/...")
        if (pathLength >= 3 && Character.isLetter(normalizedPath.charAt(0)) 
                && normalizedPath.charAt(1) == ':' && normalizedPath.charAt(2) == '/') {
            throw new SecurityException("Absolute/UNC paths not allowed: " + resourceName);
        }
        
        // Check for UNC path (e.g., "//server/share/...")
        if (pathLength >= 2 && normalizedPath.charAt(0) == '/' && normalizedPath.charAt(1) == '/') {
            throw new SecurityException("Absolute/UNC paths not allowed: " + resourceName);
        }
        
        // Security: Block ".." path segments (not just substring) to prevent traversal.
        // Single-pass scan avoids split() allocation on a hot path.
        int segmentStart = 0;
        for (int i = 0; i <= pathLength; i++) {
            if (i == pathLength || normalizedPath.charAt(i) == '/') {
                if (i - segmentStart == 2 &&
                        normalizedPath.charAt(segmentStart) == '.' &&
                        normalizedPath.charAt(segmentStart + 1) == '.') {
                    throw new SecurityException("Invalid resource path contains directory traversal: " + resourceName);
                }
                segmentStart = i + 1;
            }
        }
        
        // Security: Limit resource name length to prevent DoS
        // Check the normalized path length to ensure validation happens after normalization
        int maxLength = getMaxResourceNameLength();
        if (normalizedPath.length() > maxLength) {
            throw new SecurityException("Resource name too long (max " + maxLength + "): " + normalizedPath.length());
        }
        
        return normalizedPath;
    }

    private static void verifyClassAndArrayComponent(Class<?> clazz) {
        Class<?> type = clazz;
        while (type.isArray()) {
            type = type.getComponentType();
        }
        if (!type.isPrimitive()) {
            SecurityChecker.verifyClass(type);
        }
    }

    /**
     * Convenience method for field access issues
     */
    public static void logFieldAccessIssue(Field field, Exception e) {
        logAccessIssue(field, e, "read");
    }

    /**
     * Convenience method for method invocation issues
     */
    public static void logMethodAccessIssue(Method method, Exception e) {
        logAccessIssue(method, e, "invoke");
    }

    /**
     * Convenience method for constructor access issues
     */
    public static void logConstructorAccessIssue(Constructor<?> constructor, Exception e) {
        logAccessIssue(constructor, e, "invoke");
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
        excluded = (excluded == null) ? Collections.emptySet() : excluded;
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

        // 2) Iterate the smaller set for better performance
        Set<Class<?>> smaller = allA.size() <= allB.size() ? allA : allB;
        Set<Class<?>> larger = allA.size() <= allB.size() ? allB : allA;
        
        // 3) Create a modifiable copy of the intersection, filtering excluded items
        Set<Class<?>> common = new LinkedHashSet<>();
        for (Class<?> type : smaller) {
            if (larger.contains(type) && !excluded.contains(type)) {
                common.add(type);
            }
        }

        if (common.isEmpty()) {
            return Collections.emptySet();
        }

        // 3) Sort by sum of distances from both input classes
        // The most specific common type minimizes the total distance
        List<Class<?>> candidates = new ArrayList<>(common);
        ClassHierarchyInfo infoA = getClassHierarchyInfo(classA);
        ClassHierarchyInfo infoB = getClassHierarchyInfo(classB);
        candidates.sort((x, y) -> {
            int dx = infoA.getDistance(x) + infoB.getDistance(x);
            int dy = infoA.getDistance(y) + infoB.getDistance(y);
            return Integer.compare(dx, dy); // lowest sum first
        });

        // 4) Identify "lowest" types
        Set<Class<?>> lowest = new LinkedHashSet<>();
        // Use IdentitySet for Class objects - identity-based comparison is faster
        Set<Class<?>> unionOfAncestors = new IdentitySet<>();

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

            // Add primitive/wrapper counterpart at distance 1 so that inheritance matching
            // handles boxing/unboxing (e.g., Integer value → int parameter) without a separate phase.
            Class<?> counterpart = PRIMITIVE_TO_WRAPPER.get(key);
            if (counterpart == null) {
                counterpart = WRAPPER_TO_PRIMITIVE.get(key);
            }
            if (counterpart != null && !distanceMap.containsKey(counterpart)) {
                distanceMap.put(counterpart, 1);
                allSupertypes.add(counterpart);
            }

            return new ClassHierarchyInfo(Collections.unmodifiableSet(allSupertypes),
                    Collections.unmodifiableMap(distanceMap));
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
        BLOCKED_CLASSES.addAll(SecurityChecker.SECURITY_BLOCKED_CLASSES.toSet());
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

    /**
     * Clears internal caches. For tests and hot-reload scenarios only.
     * <p>
     * This method should only be used in testing scenarios or when hot-reloading classes.
     * It clears various internal caches that may hold references to classes and constructors.
     * Note that ClassValue-backed caches cannot be fully cleared and rely on GC for unused keys.
     * </p>
     */
    public static void clearCaches() {
        NAME_CACHE.clear();
        // Preserve user-added aliases while clearing and re-adding built-in aliases
        GLOBAL_ALIASES.clear();
        GLOBAL_ALIASES.putAll(BUILTIN_ALIASES);
        GLOBAL_ALIASES.putAll(USER_ALIASES);
        SUCCESSFUL_CONSTRUCTOR_CACHE.clear();
        CONSTRUCTOR_PLAN_CACHE.clear();
        NAMED_PARAMETER_MATCHING_VIABLE_CACHE.clear();
        CLASS_HIERARCHY_CACHE.clear();
        accessibilityCache.clear();
        osgiClassLoaders.clear();
        // ClassValue-backed caches cannot be fully cleared; rely on GC for unused keys.
    }

    public static class SecurityChecker {
        // Combine all security-sensitive classes in one place
        static final ClassValueSet SECURITY_BLOCKED_CLASSES = ClassValueSet.of(
                ClassLoader.class,
                ProcessBuilder.class,
                Process.class,
                Constructor.class,
                Method.class,
                Field.class,
                Runtime.class,
                System.class
        );

        // Add specific class names that might be loaded dynamically
        static final Set<String> SECURITY_BLOCKED_CLASS_NAMES = new HashSet<>(CollectionUtilities.listOf(
                "java.lang.ProcessImpl",
                "java.lang.Runtime", 
                "java.lang.ProcessBuilder",
                "java.lang.System",
                "javax.script.ScriptEngineManager",
                "javax.script.ScriptEngine",
                "java.lang.invoke.MethodHandles$Lookup"  // Can open modules reflectively
                // Add any other specific class names as needed
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
         * Checks if a class name is directly in the blocked list or belongs to a blocked package.
         * Used before class loading.
         *
         * @param className The class name to check
         * @return true if the class name is blocked, false otherwise
         */
        public static boolean isSecurityBlockedName(String className) {
            // Check exact class name match
            if (BLOCKED_CLASS_NAMES_SET.contains(className)) {
                return true;
            }
            // Check package-level blocking
            if (className.startsWith("javax.script.") ||  // Script engines
                className.startsWith("jdk.nashorn.")) {     // Nashorn JavaScript engine
                return true;
            }
            return false;
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
                        "For security reasons, access to this class is not allowed: " + clazz.getName());
            }
        }
    }

    // Configurable Security Feature Methods
    // Note: These provide enhanced security features beyond the always-on core security
    // Performance: These now use cached constants instead of repeated System.getProperty() calls

    private static boolean isEnhancedSecurityEnabled() {
        return ENHANCED_SECURITY_ENABLED;
    }

    private static int getMaxClassLoadDepth() {
        return MAX_CLASS_LOAD_DEPTH;
    }

    private static int getMaxConstructorArgs() {
        return MAX_CONSTRUCTOR_ARGS;
    }

    private static int getMaxResourceNameLength() {
        return MAX_RESOURCE_NAME_LENGTH;
    }

    private static void validateEnhancedSecurity(String operation, int currentCount, int maxAllowed) {
        // Performance: Use cached constant instead of method call
        if (!ENHANCED_SECURITY_ENABLED || maxAllowed <= 0) {
            return; // Security disabled - early exit
        }
        if (currentCount > maxAllowed) {
            throw new SecurityException(operation + " count exceeded limit: " + currentCount + " > " + maxAllowed);
        }
    }
}
