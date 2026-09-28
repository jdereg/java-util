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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
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
import java.util.concurrent.ConcurrentLinkedQueue;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
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
 * The {@link #computeInheritanceDistance(Class, Class)} method calculates the fewest inheritance steps
 * between two types, through direct supertypes as the Java Language Specification defines them (JLS 4.10):
 * a distance exists exactly when Java would assign the one to the other, so an interface reaches
 * {@code Object} and {@code String[]} reaches {@code Object[]}. If there is no relationship, it returns
 * {@code -1}. This method also supports primitive widening conversions as defined in JLS 5.1.2, treating
 * widening paths like byte→short→int→long→float→double as inheritance relationships.
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
        // Default capacity: a table sized for 2,048 names took 16 KB per loader on its first entry, and most loaders
        // resolve a handful (a Groovy class loader per script, say). It grows as names arrive.
        final ConcurrentMap<String, WeakReference<Class<?>>> cache = new ConcurrentHashMap<>();
        final ReferenceQueue<Class<?>> queue = new ReferenceQueue<>();
        /** Names cached as not found, oldest first; its length is kept in notFoundCount. */
        final ConcurrentLinkedQueue<String> notFoundOrder = new ConcurrentLinkedQueue<>();
        final AtomicInteger notFoundCount = new AtomicInteger();
    }

    /**
     * One-entry memo in front of NAME_CACHE: the (loader → LoaderCache) pair most recently used.
     * <p>
     * NAME_CACHE is a synchronized WeakHashMap, so without this memo every {@code forName()} cache
     * hit acquires a single global lock — under concurrent load (e.g., multi-threaded JSON parsing
     * resolving type names) that lock does not just cap scaling, it inverts it (measured: 8 threads
     * ran ~5x slower in aggregate than 1 thread). Nearly all processes resolve classes through one
     * dominant ClassLoader, so a one-entry identity-checked memo removes the lock from the hot path.
     * <p>
     * The memo is held through a {@link WeakReference} so it never pins a ClassLoader: when the
     * loader is otherwise unreachable, the memo entry is weakly reachable and both collapse together
     * (the backing WeakHashMap remains the authority). Threads racing on different loaders simply
     * overwrite the memo — each then falls back to the synchronized map, which stays correct.
     */
    private static final class LoaderCacheMemo {
        final ClassLoader loader;
        final LoaderCache cache;

        LoaderCacheMemo(ClassLoader loader, LoaderCache cache) {
            this.loader = loader;
            this.cache = cache;
        }
    }
    private static volatile WeakReference<LoaderCacheMemo> lastLoaderCache = new WeakReference<>(null);

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
    // One shared entry for every "not found" (the sentinel is strongly reachable, so it never clears), which
    // lets eviction remove a name only while it is still cached as not found: remove(name, NOT_FOUND).
    private static final WeakReference<Class<?>> NOT_FOUND = new WeakReference<>(CLASS_NOT_FOUND_SENTINEL);
    private static final int MAX_NEGATIVE_CLASS_CACHE_ENTRIES = 4096;

    private static Class<?> fromCache(String name, ClassLoader cl) {
        // Check global aliases first (primitive types and user-defined aliases)
        Class<?> globalAlias = GLOBAL_ALIASES.get(name);
        if (globalAlias != null) {
            return globalAlias;
        }

        // Then check classloader-specific cache using consistent resolution
        final ClassLoader key = resolveLoader(cl);
        LoaderCache holder = getLoaderCacheHolder(key);

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

        if (holder.cache.putIfAbsent(name, NOT_FOUND) != null) {
            return;     // already cached, found or not
        }

        // Bounded, and past the bound the OLDEST miss is forgotten -- not the newest refused. Refusing let any
        // 4,096 unique names (junk read from untrusted input, say) fill the cache for good, after which every
        // genuinely missing class cost a full class loader search on every lookup. A forgotten miss is simply
        // looked up once more. Hits write nothing, so this is not LRU: it keeps the hit path contention-free.
        holder.notFoundOrder.offer(name);
        if (holder.notFoundCount.incrementAndGet() > MAX_NEGATIVE_CLASS_CACHE_ENTRIES) {
            String oldest = holder.notFoundOrder.poll();
            if (oldest != null) {
                holder.notFoundCount.decrementAndGet();
                // Only if still "not found": a class loaded under that name since then stays cached
                holder.cache.remove(oldest, NOT_FOUND);
            }
        }
    }
    
    // Helper to get or create loader cache holder. Lock-free for the most recently used loader
    // (the overwhelmingly common case); falls back to the synchronized backing map otherwise.
    private static LoaderCache getLoaderCacheHolder(ClassLoader key) {
        LoaderCacheMemo memo = lastLoaderCache.get();
        if (memo != null && memo.loader == key) {
            return memo.cache;
        }

        LoaderCache holder;
        synchronized (NAME_CACHE) {
            holder = NAME_CACHE.get(key);
            if (holder == null) {
                holder = new LoaderCache();
                NAME_CACHE.put(key, holder);
            }
        }
        lastLoaderCache = new WeakReference<>(new LoaderCacheMemo(key, holder));
        return holder;
    }
    
    private static void toCache(String name, ClassLoader cl, Class<?> c) {
        final ClassLoader key = resolveLoader(cl);
        final LoaderCache holder = getLoaderCacheHolder(key);

        // Opportunistically drain dead references (lock-free, uses CAS-based removal)
        drainQueue(holder);

        // Lock-free cache update using ConcurrentHashMap. A "not found" this replaces leaves its name in
        // notFoundOrder, where it ages out harmlessly: eviction removes only a NOT_FOUND entry.
        holder.cache.put(name, new NamedWeakRef(name, c, holder.queue));
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

    private static void removeCacheEntry(LoaderCache holder, String name) {
        holder.cache.remove(name);
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
    private static final ClassValueMap<Class<?>> PRIMITIVE_TO_WRAPPER = new ClassValueMap<>();
    private static final ClassValueMap<Class<?>> WRAPPER_TO_PRIMITIVE = new ClassValueMap<>();
    
    // Primitive widening conversion distances (JLS 5.1.2)
    // Maps from source primitive to Map<destination primitive, distance>
    private static final Map<Class<?>, Map<Class<?>, Integer>> PRIMITIVE_WIDENING_DISTANCES;

    // Cache for OSGi ClassLoader to avoid repeated reflection calls (set only when one is found)
    private static volatile ClassValue<AtomicReference<ClassLoader>> osgiClassLoaders = perClass(c -> new AtomicReference<>());
    // False once OSGi's FrameworkUtil proved not loadable from this class's loader: every later look failed the same
    // way, at the cost of a ClassNotFoundException each time
    private static volatile boolean osgiPresent = true;
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
    private static final ClassValueMap<Supplier<Object>> DIRECT_CLASS_MAPPING = new ClassValueMap<>();
    private static final Map<Class<?>, Supplier<Object>> ASSIGNABLE_CLASS_MAPPING = new LinkedHashMap<>();
    // Per class, the ASSIGNABLE_CLASS_MAPPING supplier for it (Optional.empty() for none), so the O(n) scan runs once
    private static volatile ClassValue<Optional<Supplier<Object>>> assignableTypes = perClass(ClassUtilities::findAssignableSupplier);
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
    private static volatile ClassValue<AtomicReference<Optional<Constructor<?>>>> successfulConstructors = perClass(c -> new AtomicReference<>());
    private static volatile ClassValue<Map<ArgumentShapeKey, Optional<ConstructorPlan>>> constructorPlans = perClass(c -> new ConcurrentHashMap<>());

    /**
     * A per-class cache: each class's value is computed on first use and kept inside the class itself (a
     * {@link ClassValue}), so it never keeps the class -- or its ClassLoader, and every class that loader loaded --
     * alive: a class from a discarded loader (a redeployed webapp, a plugin, recompiled script) can be unloaded.
     * Computing one class's value never slows another's lookup either. A ClassValueMap is neither: its backing map
     * holds every class it has seen, and each put() invalidates the cached lookup of every class. clearCaches()
     * replaces these, since a ClassValue cannot be emptied.
     */
    private static <V> ClassValue<V> perClass(Function<Class<?>, V> compute) {
        return new ClassValue<V>() {
            @Override
            protected V computeValue(Class<?> type) {
                return compute.apply(type);
            }
        };
    }

    private static void rememberConstructor(Class<?> c, Constructor<?> constructor) {
        successfulConstructors.get(c).set(Optional.of(constructor));
    }
    /**
     * Per class, the constructors named-parameter matching may use, each followed by its parameters --
     * {@code [Constructor, Parameter[], Constructor, Parameter[], ...]} in {@link ReflectionUtils#getAllConstructors}
     * order -- or {@link #NOT_NAMED_MATCHABLE}. Fixed for the class, so computed once rather than per object built.
     * <p>
     * JDK types only, deliberately: a ClassValue's value lives inside its Class, and a java-util type stored in a
     * JDK class would pin java-util's class loader (a webapp's, say) for the life of the JVM. Replaced, not
     * cleared, by clearCaches(): a ClassValue cannot be emptied.
     */
    private static volatile ClassValue<Object[]> namedConstructors = perClass(ClassUtilities::namedConstructorPlan);
    /** Every parameterized constructor has synthetic ({@code argN}) names: named matching cannot apply. */
    private static final Object[] NOT_NAMED_MATCHABLE = new Object[0];

    private static Object[] namedConstructorPlan(Class<?> c) {
        Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(c);
        Parameter[][] parameters = new Parameter[constructors.length][];
        boolean[] realNames = new boolean[constructors.length];
        boolean hasParameterized = false;
        boolean viable = false;
        for (int i = 0; i < constructors.length; i++) {
            parameters[i] = constructors[i].getParameters();
            realNames[i] = hasRealParameterNames(parameters[i]);
            if (parameters[i].length > 0) {
                hasParameterized = true;
                viable |= realNames[i];
            }
        }
        if (hasParameterized && !viable) {
            return NOT_NAMED_MATCHABLE;
        }

        List<Object> plan = new ArrayList<>(constructors.length * 2);
        for (int i = 0; i < constructors.length; i++) {
            // A constructor whose names were not compiled in (argN) cannot be matched by name
            if (realNames[i] && isAccessibleOrMadeSo(constructors[i])) {
                plan.add(constructors[i]);
                plan.add(parameters[i]);
            }
        }
        return plan.toArray();
    }

    private static boolean hasRealParameterNames(Parameter[] parameters) {
        for (Parameter parameter : parameters) {
            if (isSyntheticArgName(parameter.getName())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isAccessibleOrMadeSo(Constructor<?> constructor) {
        try {
            trySetAccessible(constructor);
            return true;
        } catch (SecurityException se) {
            // Can't make this constructor accessible under JPMS; named matching skips it
            if (LOG.isLoggable(Level.FINER)) {
                LOG.log(Level.FINER, "Cannot access constructor {0} due to security restrictions: {1}",
                        new Object[]{constructor, se.getMessage()});
            }
            return false;
        }
    }

    private static final class NullArgType { }
    private static final Class<?> NULL_ARG_TYPE = NullArgType.class;
    private static final Parameter[] EMPTY_PARAMETERS = new Parameter[0];

    private static final class ArgumentShapeKey {
        private final Class<?>[] argTypes;
        private final int hash;

        private ArgumentShapeKey(Class<?>[] argTypes) {
            this.argTypes = argTypes;
            this.hash = Arrays.hashCode(argTypes);
        }

        static ArgumentShapeKey from(Object[] suppliedArgs) {
            if (suppliedArgs.length == 0) {
                return EMPTY_ARGUMENT_SHAPE_KEY;
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
    private static final ArgumentShapeKey EMPTY_ARGUMENT_SHAPE_KEY =
            new ArgumentShapeKey(ArrayUtilities.EMPTY_CLASS_ARRAY);

    private static final class ConstructorPlan {
        private final Constructor<?> constructor;
        private final Parameter[] parameters;
        private final boolean allowNullsFirst;
        /** The matcher's decisions for this argument shape, or null: match on every call. */
        private final ArgumentBinding binding;

        private ConstructorPlan(Constructor<?> constructor, Parameter[] parameters, boolean allowNullsFirst) {
            this(constructor, parameters, allowNullsFirst, null);
        }

        private ConstructorPlan(Constructor<?> constructor, Parameter[] parameters, boolean allowNullsFirst,
                                ArgumentBinding binding) {
            this.constructor = constructor;
            this.parameters = parameters == null ? EMPTY_PARAMETERS : parameters;
            this.allowNullsFirst = allowNullsFirst;
            this.binding = binding == null || !binding.replayable ? null : binding;
        }
    }

    /**
     * What the class-decided phases of matching chose for one argument shape: which supplied value fills each
     * parameter by exact type, and which by inheritance. Those choices depend only on the argument classes,
     * which key the plan cache, so they replay exactly -- and they were most of matching's cost. Conversion
     * (which depends on the values and on each call's Converter) and defaults (a fresh object per call) run on
     * every call, as in matching. So a replay produces exactly what matching would, for any values and any
     * Converter.
     */
    private static final class ArgumentBinding {
        /** Per parameter: the index of the value matched by type or inheritance, or -1. */
        final int[] source;
        /** False for varargs, whose packing is not recorded: such a plan matches in full on every call. */
        boolean replayable = true;
        private boolean[] matched;
        private boolean[] used;
        private boolean complete;
        private boolean identity;

        ArgumentBinding(int parameterCount) {
            source = new int[parameterCount];
            Arrays.fill(source, -1);
        }

        /** Called once matching is done, before the binding is shared through the plan cache. */
        ArgumentBinding seal(int valueCount) {
            matched = new boolean[source.length];
            used = new boolean[valueCount];
            complete = true;
            identity = source.length == valueCount;
            for (int i = 0; i < source.length; i++) {
                int j = source[i];
                matched[i] = j >= 0;
                complete &= j >= 0;
                identity &= j == i;
                if (j >= 0) {
                    used[j] = true;
                }
            }
            return this;
        }

        /**
         * Every parameter is matched by type, whatever the values or the Converter: the arguments do not
         * depend on the null policy, so a retry under the other policy would pass the very same arguments.
         */
        boolean isComplete() {
            return complete;
        }

        /** The arguments matching produces for these values (of the recorded classes). */
        Object[] replay(Converter converter, Object[] values, Parameter[] parameters, boolean allowNulls) {
            if (identity) {
                return values;      // each value in its own position, as is (a fresh array per call)
            }
            int n = source.length;
            Object[] result = new Object[n];
            for (int i = 0; i < n; i++) {
                if (source[i] >= 0) {
                    result[i] = values[source[i]];
                }
            }
            if (!complete) {
                // The rest of matching, as matching does it: conversions, then defaults
                boolean[] parameterMatched = matched.clone();
                findConvertibleMatches(converter, values, used.clone(), parameters, 0, n, parameterMatched, result,
                        0, values.length);
                fillRemainingParameters(converter, parameters, 0, n, parameterMatched, result, allowNulls);
            }
            return result;
        }
    }

    /**
     * Per class, its hierarchy information (see perClass). It was a ClassValueMap, which kept every class ever passed
     * to forName(), newInstance() or a security check -- and its ClassLoader -- alive for the life of the JVM.
     */
    private static volatile ClassValue<ClassHierarchyInfo> classHierarchies = perClass(ClassUtilities::computeClassHierarchyInfo);

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
        return WRAPPER_TO_PRIMITIVE.getByClass(toType);
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
     * <p>
     * For reference types it is the fewest steps from {@code source} up to {@code destination} through direct
     * supertypes as the Java Language Specification defines them (JLS 4.10.2, 4.10.3; see
     * {@link #getClassHierarchyInfo}): a distance exists exactly when {@code destination.isAssignableFrom(source)}.
     * So an interface reaches {@code Object}, and {@code String[]} reaches {@code Object[]} in 1 step and
     * {@code Object} in 2. For primitive types it follows the widening chain (JLS 4.10.1, 5.1.2:
     * byte→short→int→long→float→double, char→int); a wrapper is one step from its primitive, and boxes to
     * reach a reference type.
     * <p>
     * A distance counts steps; it does not rank specificity. A supertype can be fewer steps away than one of its
     * own subtypes ({@code ArrayList} reaches {@code Object} in 2 steps, through {@code RandomAccess}, and
     * {@code Collection} in 3), and distances change with the JDK (JDK 21's {@code SequencedCollection} added a
     * step between {@code List} and {@code Collection}). To choose among candidate types, prefer the most specific
     * (a subtype over its supertype), as {@link #findClosest} does, and use the distance only between unrelated
     * candidates.
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
            Class<?> sourcePrim = source.isPrimitive() ? source : WRAPPER_TO_PRIMITIVE.getByClass(source);
            Class<?> destPrim = destination.isPrimitive() ? destination : WRAPPER_TO_PRIMITIVE.getByClass(destination);
            
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
            Class<?> src = source.isPrimitive() ? PRIMITIVE_TO_WRAPPER.getByClass(source) : source;
            if (src != null) {
                return getClassHierarchyInfo(src).getDistance(destination);
            }
            return -1;
        }

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
     * <p>
     * An array class may be named as the JVM names it ({@code "[I"}, {@code "[Ljava.lang.String;"}), with
     * {@code '/'} separators ({@code "[Ljava/lang/String;"}), or in Java form ({@code "int[]"},
     * {@code "java.lang.String[][]"}), up to the JVM's 255 dimensions. Any other array spelling -- text after
     * a primitive's letter ({@code "[Ixyz"}), the forms mixed ({@code "[I[]"}), or the separators mixed
     * ({@code "[Ljava/lang.String;"}) -- is malformed and returns null.
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
        } catch (LinkageError e) {
            // Malformed or incompatible class bytes (ClassFormatError, UnsupportedClassVersionError,
            // NoClassDefFoundError from a broken dependency, ...). This API is documented to return
            // null when the class cannot be provided — an Error escaping here lets a bad (possibly
            // attacker-supplied) type name take down the calling thread instead.
            return null;
        }
    }

    /**
     * Used internally to load a class by name, and takes care of caching name mappings for speed.
     *
     * @param name        String name of a JVM class.
     * @param classLoader ClassLoader to use when searching for JVM classes.
     * @return Class instance of the named JVM class, or null when the name is cached as not found
     */
    private static Class<?> internalClassForName(String name, ClassLoader classLoader) throws ClassNotFoundException {
        Class<?> c = fromCache(name, classLoader);
        if (c != null) {
            // A cached "class not found": null, not an exception. The only caller turns every exception into
            // null anyway, and building one here (message and stack trace) cost more than the lookup it saved.
            if (c == CLASS_NOT_FOUND_SENTINEL) {
                return null;
            }
            // Verified before it was cached (an alias, when it was registered). Only a runtime rule can change the
            // verdict since: a block added later, or an unblock since cleared. Once there has been one, every hit
            // is checked again -- a blocked hit throws, as the lookup of an uncached blocked name does.
            if (verifyCachedClasses) {
                verifyClassAndArrayComponent(c);
            }
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
            int end = name.length();
            int dims = 0;
            while (end >= 2 && name.charAt(end - 1) == ']' && name.charAt(end - 2) == '[') {
                dims++;
                end -= 2;
            }
            String base = name.substring(0, end);
            // The element must be a plain Java name: not a descriptor ("[Ljava.lang.String;[]", "[I[]"),
            // no stray bracket ("String[]]"), no '/' separators
            if (base.isEmpty() || dims > MAX_ARRAY_DIMENSIONS || base.indexOf('[') >= 0 || base.indexOf(']') >= 0
                    || base.indexOf(';') >= 0 || base.indexOf('/') >= 0) {
                throw new ClassNotFoundException("Bad array name: " + name);
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
                    element = loadArrayElement(base, classLoader);
            }
            return arrayOf(element, dims);
        }

        // JVM descriptors: "[I", "[[Ljava.lang.String;" -- or with '/' separators, the JVM's internal form
        if (name.startsWith("[")) {
            int dims = 0;
            while (dims < name.length() && name.charAt(dims) == '[') {
                dims++;
            }
            if (dims >= name.length() || dims > MAX_ARRAY_DIMENSIONS) {
                throw new ClassNotFoundException("Bad descriptor: " + name);
            }

            Class<?> element;
            char typeChar = name.charAt(dims);
            if (typeChar == 'L') {
                // "L" + a class name + ";", and nothing after it
                int semicolon = name.indexOf(';', dims);
                if (semicolon != name.length() - 1 || semicolon == dims + 1) {
                    throw new ClassNotFoundException("Bad descriptor: " + name);
                }
                String className = name.substring(dims + 1, semicolon);
                // Not a nested descriptor, and one separator style, not a mix ("java/lang.String")
                if (className.indexOf('[') >= 0 || (className.indexOf('.') >= 0 && className.indexOf('/') >= 0)) {
                    throw new ClassNotFoundException("Bad descriptor: " + name);
                }
                // Convert JVM descriptor format (java/lang/String) to Java format (java.lang.String)
                className = className.replace('/', '.');
                if (SecurityChecker.isSecurityBlockedName(className)) {
                    throw new SecurityException("Class loading denied for security reasons: " + className);
                }
                element = loadArrayElement(className, classLoader);
            } else {
                // A primitive is one letter, with nothing after it ("[Ixyz" and "[I;" are not int[])
                if (name.length() != dims + 1) {
                    throw new ClassNotFoundException("Bad descriptor: " + name);
                }
                switch (typeChar) {
                    case 'B': element = byte.class; break;
                    case 'S': element = short.class; break;
                    case 'I': element = int.class; break;
                    case 'J': element = long.class; break;
                    case 'F': element = float.class; break;
                    case 'D': element = double.class; break;
                    case 'Z': element = boolean.class; break;
                    case 'C': element = char.class; break;
                    default:
                        throw new ClassNotFoundException("Bad descriptor: " + name);
                }
            }
            return arrayOf(element, dims);
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

    /** The JVM's limit on array dimensions. */
    private static final int MAX_ARRAY_DIMENSIONS = 255;

    private static Class<?> loadArrayElement(String className, ClassLoader classLoader) throws ClassNotFoundException {
        if (classLoader != null) {
            return classLoader.loadClass(className);
        }
        // Use the standard classloader resolution which handles OSGi/JPMS properly
        return Class.forName(className, false, getClassLoader(ClassUtilities.class));
    }

    private static Class<?> arrayOf(Class<?> element, int dims) {
        Class<?> arrayClass = element;
        for (int i = 0; i < dims; i++) {
            arrayClass = Array.newInstance(arrayClass, 0).getClass();
        }
        return arrayClass;
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

        Class<?> c = PRIMITIVE_TO_WRAPPER.getByClass(primitiveClass);

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

        Class<?> primitive = WRAPPER_TO_PRIMITIVE.getByClass(wrapperClass);
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
        if (!osgiPresent) {
            return null;
        }
        AtomicReference<ClassLoader> slot = osgiClassLoaders.get(classFromBundle);
        ClassLoader cl = slot.get();
        if (cl != null) {
            return cl;
        }
        ClassLoader computed = getOSGiClassLoader0(classFromBundle);
        if (computed != null) {
            slot.set(computed);
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
            Class<?> frameworkUtilClass;
            try {
                frameworkUtilClass = Class.forName("org.osgi.framework.FrameworkUtil", false, baseLoader);
            } catch (ClassNotFoundException | LinkageError notOsgi) {
                osgiPresent = false;
                return null;
            }

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
     * related to the input class. The search prioritizes:
     * <ul>
     *     <li>Exact class match (returns immediately)</li>
     *     <li>The most specific of the candidates the class is assignable to: a candidate that another applicable
     *         candidate is a subtype of is passed over, however few steps away it is, as Java chooses the most
     *         specific overload (JLS 15.12.2.5). For an {@code ArrayList}, {@code Collection} is chosen over
     *         {@code Object}, and {@code List} over {@code Collection}.</li>
     *     <li>Among candidates unrelated to each other, a class over an interface, as a class's method wins over an
     *         interface's default (JLS 8.4.8); then the fewest inheritance steps away
     *         ({@link #computeInheritanceDistance}); then the first in the map's iteration order.</li>
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

        // The candidates clazz is assignable to: its hierarchy holds exactly those (JLS 4.10)
        Map<Class<?>, Integer> distanceMap = getClassHierarchyInfo(clazz).getDistanceMap();
        Class<?>[] applicable = null;
        int[] distances = null;
        int count = 0;
        for (Class<?> candidate : candidateClasses.keySet()) {
            Integer distance = distanceMap.get(candidate);
            if (distance == null) {
                continue;
            }
            if (applicable == null) {
                applicable = new Class<?>[4];
                distances = new int[4];
            } else if (count == applicable.length) {
                applicable = Arrays.copyOf(applicable, count * 2);
                distances = Arrays.copyOf(distances, count * 2);
            }
            applicable[count] = candidate;
            distances[count++] = distance;
        }
        if (count == 0) {
            return defaultValue;
        }

        // The most specific of them. Choosing by distance alone could pass over a subtype for its own supertype:
        // ArrayList reaches Object in 2 steps (through RandomAccess) and Collection in 3, and on a tie a class was
        // preferred, so Object won over Collection. So the answer is the best -- a class over an interface, as a
        // class's method wins over an interface's default (JLS 8.4.8), then fewest steps, then the first -- of the
        // candidates no other applicable candidate is a subtype of. That is checked only for a candidate that would
        // displace the best so far.
        int best = -1;
        for (int i = 0; i < count; i++) {
            if (best >= 0) {
                boolean classOverInterface = applicable[best].isInterface() && !applicable[i].isInterface();
                boolean sameKind = applicable[best].isInterface() == applicable[i].isInterface();
                if (!classOverInterface && !(sameKind && distances[i] < distances[best])) {
                    continue;
                }
            }
            if (count > 1 && hasMoreSpecific(applicable[i], applicable, count)) {
                continue;
            }
            best = i;
        }
        return candidateClasses.get(applicable[best]);
    }

    /** Whether another of the first {@code count} {@code candidates} is a proper subtype of {@code candidate}. */
    private static boolean hasMoreSpecific(Class<?> candidate, Class<?>[] candidates, int count) {
        for (int i = 0; i < count; i++) {
            Class<?> other = candidates[i];
            if (other != candidate && candidate.isAssignableFrom(other)) {
                return true;
            }
        }
        return false;
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

        Supplier<Object> directClassMapping = DIRECT_CLASS_MAPPING.getByClass(argType);

        if (directClassMapping != null) {
            return directClassMapping.get();
        }

        // Arrays: return an empty array of the correct type. This must be decided BEFORE the
        // assignable-interface scan below — every Java array implements Cloneable, so the scan
        // would otherwise match the Cloneable→ArrayList mapping and hand back an ArrayList for a
        // Date[]/String[][]/etc. parameter: a type-incompatible default that forces the
        // constructor invocation to fail and fall through to the allow-nulls pass.
        if (argType.isArray()) {
            return Array.newInstance(argType.getComponentType(), 0);
        }

        return assignableTypes.get(argType).map(Supplier::get).orElse(null);
    }

    /**
     * The first ASSIGNABLE_CLASS_MAPPING supplier whose type {@code argType} is assignable to and whose product is an
     * {@code argType}, if any. A mapping's product is only known to be of the mapped type: an ArrayBlockingQueue
     * parameter was given a LinkedBlockingQueue (the BlockingQueue mapping), a GregorianCalendar or a Path an
     * ArrayList, and a Writer a ByteArrayInputStream -- defaults the constructor could not take.
     */
    private static Optional<Supplier<Object>> findAssignableSupplier(Class<?> argType) {
        for (Map.Entry<Class<?>, Supplier<Object>> entry : ASSIGNABLE_CLASS_MAPPING.entrySet()) {
            if (entry.getKey().isAssignableFrom(argType) && argType.isInstance(entry.getValue().get())) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
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
        return matchArgumentsToParameters(converter, valueArray, parameters, 0, parameters.length, allowNulls, null);
    }

    /** As above, recording its decisions in {@code binding} when that is not null (and not varargs). */
    private static Object[] matchArgumentsToParameters(Converter converter, Object[] valueArray,
                                                       Parameter[] parameters, boolean allowNulls,
                                                       ArgumentBinding binding) {
        return matchArgumentsToParameters(converter, valueArray, parameters, 0, parameters.length, allowNulls, binding);
    }

    private static Object[] matchArgumentsToParameters(Converter converter, Object[] valueArray,
                                                       Parameter[] parameters, int paramOffset,
                                                       int paramCount, boolean allowNulls) {
        return matchArgumentsToParameters(converter, valueArray, parameters, paramOffset, paramCount, allowNulls, null);
    }

    private static Object[] matchArgumentsToParameters(Converter converter, Object[] valueArray,
                                                       Parameter[] parameters, int paramOffset,
                                                       int paramCount, boolean allowNulls,
                                                       ArgumentBinding binding) {
        if (parameters == null || paramCount <= 0) {
            return ArrayUtilities.EMPTY_OBJECT_ARRAY; // Reuse a static empty array
        }

        // Check if the last parameter is varargs and handle specially
        boolean isVarargs = parameters[paramOffset + paramCount - 1].isVarArgs();
        if (isVarargs) {
            if (binding != null) {
                binding.replayable = false;     // varargs packing is not recorded: always match in full
            }
            return matchArgumentsWithVarargs(converter, valueArray, parameters, paramOffset, paramCount, allowNulls);
        }

        // Create result array and tracking arrays
        Object[] result = new Object[paramCount];
        boolean[] parameterMatched = new boolean[paramCount];

        // For tracking available values (more efficient than repeated removal from list)
        boolean[] valueUsed = new boolean[valueArray.length];

        // PHASE 1: Find exact type matches - highest priority
        findExactMatches(valueArray, valueUsed, parameters, paramOffset, paramCount, parameterMatched, result, 0, valueArray.length, binding);

        // PHASE 2: Find assignable type matches with inheritance (includes primitive/wrapper via ClassHierarchyInfo)
        findInheritanceMatches(valueArray, valueUsed, parameters, paramOffset, paramCount, parameterMatched, result, 0, valueArray.length, binding);

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
            if (varargsSource.length == 1) {
                // A lone array or Collection that is not itself an element (Object[]{"a","b"} for String..., or an
                // Integer[] for int...) holds the elements: it was packed as one element, which converted to null.
                Object only = varargsSource[0];
                if (only != null && !componentType.isInstance(only)) {
                    if (only.getClass().isArray()) {
                        varargsSource = elementsOf(only);
                    } else if (only instanceof Collection) {
                        varargsSource = ((Collection<?>) only).toArray();
                    }
                }
            }
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

        findExactMatches(values, valueUsed, parameters, paramOffset, fixedParamCount, parameterMatched, result, valueStartInclusive, valueEndExclusive, null);
        findInheritanceMatches(values, valueUsed, parameters, paramOffset, fixedParamCount, parameterMatched, result, valueStartInclusive, valueEndExclusive, null);
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

        findExactMatches(values, valueUsed, parameters, paramOffset, fixedParamCount, parameterMatched, result, valueStartInclusive, valueEndExclusive, null);
        findInheritanceMatches(values, valueUsed, parameters, paramOffset, fixedParamCount, parameterMatched, result, valueStartInclusive, valueEndExclusive, null);
        findConvertibleMatches(converter, values, valueUsed, parameters, paramOffset, fixedParamCount, parameterMatched, result, valueStartInclusive, valueEndExclusive);
        fillRemainingParameters(converter, parameters, paramOffset, fixedParamCount, parameterMatched, result, allowNulls);
    }

    /** The elements of an array of any component type, boxed. */
    private static Object[] elementsOf(Object array) {
        int length = Array.getLength(array);
        Object[] elements = new Object[length];
        for (int i = 0; i < length; i++) {
            elements[i] = Array.get(array, i);
        }
        return elements;
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
                                         int valueStartInclusive, int valueEndExclusive,
                                         ArgumentBinding binding) {
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
                    if (binding != null) {
                        binding.source[i] = j;
                    }
                    break;
                }
            }
        }
    }

    /**
     * Gives the unmatched parameters the values that are instances of their types (a wrapper counts for its
     * primitive). As many values as any assignment could place are placed: each parameter, in declaration order, takes
     * the first free value it can hold, in the order the values were supplied, and when none is free a value is
     * moved to it from a parameter that can hold another. So a broad parameter never keeps a value that only another
     * parameter can hold -- (Object, CharSequence) given ["x", list] gives the Object the list -- and values keep the
     * order they were supplied in, as far as the types allow. The inheritance distance decided which value a parameter
     * took; it put ["x"] before [5] for (Object, Object) given [5, "x"], a String being fewer steps from Object.
     */
    private static void findInheritanceMatches(Object[] values, boolean[] valueUsed,
                                               Parameter[] parameters, int paramOffset, int paramCount,
                                               boolean[] parameterMatched, Object[] result,
                                               int valueStartInclusive, int valueEndExclusive,
                                               ArgumentBinding binding) {
        int valueCount = valueEndExclusive - valueStartInclusive;
        if (valueCount <= 0) {
            return;
        }
        // fits[i][k]: parameter i can hold value valueStartInclusive + k as it is
        boolean[][] fits = null;
        for (int i = 0; i < paramCount; i++) {
            if (parameterMatched[i]) continue;
            Class<?> paramType = parameters[paramOffset + i].getType();
            for (int k = 0; k < valueCount; k++) {
                int j = valueStartInclusive + k;
                Object value = values[j];
                if (valueUsed[j] || value == null
                        || getClassHierarchyInfo(value.getClass()).getDistance(paramType) < 0) {
                    continue;
                }
                if (fits == null) {
                    fits = new boolean[paramCount][valueCount];
                }
                fits[i][k] = true;
            }
        }
        if (fits == null) {
            return;     // no value is an instance of any unmatched parameter's type
        }

        int[] holder = new int[valueCount];         // the parameter holding each value, or -1
        int[] held = new int[paramCount];           // the value each parameter holds, or -1
        Arrays.fill(holder, -1);
        Arrays.fill(held, -1);
        for (int i = 0; i < paramCount; i++) {
            if (!parameterMatched[i] && !takeFreeValue(i, fits, holder, held)) {
                moveValueTo(i, fits, holder, held, new boolean[valueCount]);
            }
        }

        for (int i = 0; i < paramCount; i++) {
            if (held[i] >= 0) {
                int j = valueStartInclusive + held[i];
                result[i] = values[j];
                parameterMatched[i] = true;
                valueUsed[j] = true;
                if (binding != null) {
                    binding.source[i] = j;
                }
            }
        }
    }

    /** Gives parameter {@code i} the first value it can hold that no parameter holds. */
    private static boolean takeFreeValue(int i, boolean[][] fits, int[] holder, int[] held) {
        for (int k = 0; k < holder.length; k++) {
            if (fits[i][k] && holder[k] < 0) {
                holder[k] = i;
                held[i] = k;
                return true;
            }
        }
        return false;
    }

    /**
     * Gives parameter {@code i} a value another parameter holds, when that parameter can take a free value or, in
     * turn, one moved from yet another (an augmenting path: each move keeps every placed value placed).
     */
    private static boolean moveValueTo(int i, boolean[][] fits, int[] holder, int[] held, boolean[] tried) {
        for (int k = 0; k < holder.length; k++) {
            if (!fits[i][k] || tried[k]) continue;
            tried[k] = true;
            int other = holder[k];
            if (other < 0 || takeFreeValue(other, fits, holder, held)
                    || moveValueTo(other, fits, holder, held, tried)) {
                holder[k] = i;
                held[i] = k;
                return true;
            }
        }
        return false;
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

                // (source, target): converting FROM the value's class TO the parameter type.
                // (These were swapped historically — mostly masked because Converter's simple-type
                // matrix is largely bidirectional, but direction-specific user overrides and any
                // one-way conversions were judged backwards.)
                if (converter.isSimpleTypeConversionSupported(valueClass, paramType)) {
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
     * Returns the enum that instances of {@code c} belong to: {@code c} itself when it is an enum, or the enum whose
     * constant body it is (a constant with a body, {@code PLUS { ... }}, is an anonymous subclass of its enum).
     * <p>
     * A class merely declared inside an enum ({@code enum Status { ...; static class Detail {} }}) is not an enum: its
     * instances are not constants, and this returns null for it. (It returned the enclosing enum, so json-io wrote a
     * {@code Status.Detail} as an enum and failed with a ClassCastException.)
     *
     * @param c the class to check; may be null
     * @return the enum class, or null if instances of {@code c} are not enum constants
     */
    public static Class<?> getClassIfEnum(Class<?> c) {
        if (c == null) {
            return null;
        }
        return ENUM_CLASS_CACHE.get(c);
    }

    /**
     * The enum instances of {@code c} belong to: {@code c} if it is an enum, else the enum among its superclasses (a
     * constant body's class extends its enum). Enclosing classes are not consulted: a class nested in an enum is not
     * one, and on JDK 8 {@code getEnclosingClass()} can throw NoClassDefFoundError, resolving the outer class of other
     * nested classes the enclosing class names.
     *
     * @param c the class to check; not null
     * @return the enum class if found, or null otherwise
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

        // Security check FIRST, at the single public chokepoint, and OUTSIDE every try block below.
        // The named-parameter branch and the positional branch each construct independently, so
        // checking inside either one leaves the other open — and a SecurityException thrown deeper
        // down would be swallowed by the fallback handlers that retry on Exception.
        SecurityChecker.verifyClass(c);

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
            normalizedArgs = normalizeMapValuesForPositionalFallback(c, mapArguments);
        }

        // Call existing implementation
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Using positional argument matching for {0}", c.getName());
        }
        Set<Class<?>> visited = null;

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

    private static Collection<?> normalizeMapValuesForPositionalFallback(Class<?> c, Map<String, Object> map) {
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
        return valuesInKeyOrder(c, map);
    }

    /**
     * Per class, the key order of the last Map sent down the positional path and the sorted order it gave:
     * {@code [String[] keys as iterated, int[] indexes in sorted key order]}. JDK types only (see
     * namedConstructors). Every lookup compares the keys, so a memo from another Map is never misapplied.
     */
    private static final ClassValue<AtomicReference<Object[]>> POSITIONAL_KEY_ORDER = new ClassValue<AtomicReference<Object[]>>() {
        @Override
        protected AtomicReference<Object[]> computeValue(Class<?> type) {
            return new AtomicReference<>();
        }
    };

    /**
     * The map's values in the alphabetical order of their keys. Sorting on every call -- and then a get()
     * per key -- was most of the positional path's cost, for any map not already ordered: json-io's
     * JsonObject is one. The objects of a class nearly always arrive with their keys in the same order, so
     * the sorted order is remembered per class and reused while the keys match.
     */
    private static List<Object> valuesInKeyOrder(Class<?> c, Map<String, Object> map) {
        int size = map.size();
        String[] keys = new String[size];
        Object[] values = new Object[size];
        int n = 0;
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) map).entrySet()) {
            if (n == size) {
                return valuesInKeyOrderSorted(map);     // the map grew while we read it
            }
            Object key = entry.getKey();
            if (!(key instanceof String)) {
                // Named parameters are Strings; ordering by name was a ClassCastException or NullPointerException
                throw new IllegalArgumentException("Cannot order Map arguments by parameter name: "
                        + (key == null ? "a key is null" : "a key is a " + key.getClass().getName()));
            }
            keys[n] = (String) key;
            values[n] = entry.getValue();
            n++;
        }
        if (n != size) {
            return valuesInKeyOrderSorted(map);
        }

        AtomicReference<Object[]> slot = POSITIONAL_KEY_ORDER.get(c);
        Object[] memo = slot.get();
        int[] order;
        if (memo != null && Arrays.equals((String[]) memo[0], keys)) {
            order = (int[]) memo[1];
        } else {
            order = sortedIndexes(keys);        // null when a key repeats
            slot.set(new Object[]{keys, order});
        }
        if (order == null) {
            // A key appears twice (json-io's parser appends fields without looking for duplicates, so a
            // document that repeats a key does this). Take each value as get() gives it, as before.
            return valuesInKeyOrderSorted(map);
        }

        Object[] ordered = new Object[size];
        for (int i = 0; i < size; i++) {
            ordered[i] = values[order[i]];
        }
        return Arrays.asList(ordered);
    }

    /**
     * The indexes of {@code keys} in the order {@code Collections.sort} would put the keys, or null when a key
     * appears more than once.
     */
    private static int[] sortedIndexes(String[] keys) {
        int n = keys.length;
        if (n <= 64) {
            // Binary insertion sort on the indexes: n log n comparisons like a merge sort, but no boxing, for
            // the sizes objects come in. The search keeps every key left of lo smaller than the new one and
            // every key from hi on larger, so it can only end without a comparison of 0 if no key equals it:
            // every repeat is found.
            int[] order = new int[n];
            for (int i = 0; i < n; i++) {
                String key = keys[i];
                int lo = 0;
                int hi = i;
                while (lo < hi) {
                    int mid = (lo + hi) >>> 1;
                    int cmp = keys[order[mid]].compareTo(key);
                    if (cmp == 0) {
                        return null;
                    }
                    if (cmp < 0) {
                        lo = mid + 1;
                    } else {
                        hi = mid;
                    }
                }
                System.arraycopy(order, lo, order, lo + 1, i - lo);
                order[lo] = i;
            }
            return order;
        }
        Integer[] boxed = new Integer[n];
        for (int i = 0; i < n; i++) {
            boxed[i] = i;
        }
        Arrays.sort(boxed, (a, b) -> keys[a].compareTo(keys[b]));
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = boxed[i];
            if (i > 0 && keys[order[i]].equals(keys[order[i - 1]])) {
                return null;
            }
        }
        return order;
    }

    /** The original approach: for a map with a repeated key, or one that changes size while being read. */
    private static List<Object> valuesInKeyOrderSorted(Map<String, Object> map) {
        List<String> sortedKeys = new ArrayList<>(map.keySet());
        Collections.sort(sortedKeys);
        List<Object> orderedValues = new ArrayList<>(sortedKeys.size());
        for (String key : sortedKeys) {
            orderedValues.add(map.get(key));
        }
        return orderedValues;
    }

    private static Object newInstanceWithNamedParameters(Converter converter, Class<?> c, Map<String, Object> namedParams) {
        // Defense in depth. The public entry point already verified c, but this method reflectively
        // invokes constructors with caller-supplied values and must never be reachable without the
        // gate — the ClassValue-backed check is a cached lookup, so re-asserting it is nearly free.
        SecurityChecker.verifyClass(c);

        // Everything about c's constructors that this method needs is fixed for the class: computed
        // once (see namedConstructors), not per object as it was -- the constructor lookup, a copy of
        // every Parameter[] and a scan of their names ran on every instance json-io built.
        Object[] plan = namedConstructors.get(c);
        if (plan == NOT_NAMED_MATCHABLE) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "No constructors for {0} have real parameter names - cannot use parameter matching", c.getName());
            }
            return null; // This will trigger fallback to positional matching
        }
        if (LOG.isLoggable(Level.FINER)) {
            logNamedParameterAttempt(c, plan);
        }

        for (int i = 0; i < plan.length; i += 2) {
            Object[] args = matchNamedArguments(converter, (Parameter[]) plan[i + 1], namedParams);
            if (args == null) {
                continue;   // a parameter is missing from the map, or its value would not convert
            }
            try {
                Object instance = ((Constructor<?>) plan[i]).newInstance(args);
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

        return null; // Indicate failure to create with named parameters
    }

    /**
     * Arguments for one constructor, taken from the map by parameter name, or {@code null} when a
     * parameter is missing from the map or its value will not convert.
     */
    private static Object[] matchNamedArguments(Converter converter, Parameter[] parameters, Map<String, Object> namedParams) {
        Object[] args = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter.isVarArgs()) {
                args[i] = namedVarargsArray(converter, parameter, namedParams);
                continue;
            }

            String paramName = parameter.getName();
            if (!namedParams.containsKey(paramName)) {
                if (LOG.isLoggable(Level.FINER)) {
                    LOG.log(Level.FINER, "  Missing parameter: {0}", paramName);
                }
                return null;
            }
            Object value = namedParams.get(paramName);
            Class<?> type = parameter.getType();
            try {
                if (value == null) {
                    // A primitive cannot take null: let the converter supply its default. For object
                    // types, use null directly.
                    args[i] = type.isPrimitive() ? converter.convert(null, type) : null;
                } else if (type.isInstance(value)) {
                    args[i] = value;
                } else if (!converter.isConversionSupportedFor(value.getClass(), type)) {
                    // Not a conversion this value's type supports at all -- typically a nested object reaching
                    // the constructor as a Map. Asking first costs a cached lookup; converting built and threw
                    // an exception (message and stack trace) on every object.
                    return null;
                } else {
                    args[i] = converter.convert(value, type);
                }
            } catch (Exception conversionException) {
                // A supported conversion that failed for this value ("abc" to a long), or a type the converter
                // refuses to reason about (it throws for an EnumSet target rather than answer)
                return null;
            }
            if (LOG.isLoggable(Level.FINEST)) {
                LOG.log(Level.FINEST, "  Matched parameter ''{0}'' with value: {1}", new Object[]{paramName, value});
            }
        }
        return args;
    }

    /** The array for a varargs parameter matched by name: its value, converted element by element. */
    private static Object namedVarargsArray(Converter converter, Parameter parameter, Map<String, Object> namedParams) {
        Class<?> arrayType = parameter.getType();
        Class<?> componentType = arrayType.getComponentType();
        String paramName = parameter.getName();
        boolean hasVarargsValue = namedParams.containsKey(paramName);
        Object v = hasVarargsValue ? namedParams.get(paramName) : null;
        Object array;

        if (!hasVarargsValue) {
            array = Array.newInstance(componentType, 0);
        } else if (v != null && arrayType.isInstance(v)) {
            // Already the right array type
            array = v;
        } else {
            // A Collection or an array (a JSON array arrives as an Object[]) gives one element per item; anything
            // else, a single element. An array was taken as a single element, and converting it failed.
            Object[] items = v instanceof Collection ? ((Collection<?>) v).toArray()
                    : v != null && v.getClass().isArray() ? elementsOf(v) : new Object[]{v};
            array = Array.newInstance(componentType, items.length);
            for (int k = 0; k < items.length; k++) {
                Object element;
                try {
                    element = converter.convert(items[k], componentType);
                } catch (Exception e) {
                    // Use default value if conversion fails (the index advanced twice here, and the next write
                    // fell off the end of the array)
                    element = getArgForType(converter, componentType);
                }
                ArrayUtilities.setElement(array, k, element);
            }
        }
        if (LOG.isLoggable(Level.FINEST)) {
            LOG.log(Level.FINEST, "  Matched varargs parameter ''{0}'' with array of length: {1}",
                    new Object[]{paramName, ArrayUtilities.getLength(array)});
        }
        return array;
    }

    private static void logNamedParameterAttempt(Class<?> c, Object[] plan) {
        LOG.log(Level.FINER, "Class {0} is {1}{2}",
                new Object[]{c.getName(),
                        Modifier.isFinal(c.getModifiers()) ? "final" : "non-final",
                        Throwable.class.isAssignableFrom(c) ? " (Exception type)" : ""});
        LOG.log(Level.FINER, "Trying {0} constructors with real parameter names for {1}",
                new Object[]{plan.length / 2, c.getName()});
        for (int i = 0; i < plan.length; i += 2) {
            LOG.log(Level.FINER, "Trying constructor: {0}", plan[i]);
        }
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
    private static boolean hasGeneratedKeys(Map<?, ?> map) {
        if (map.isEmpty()) {
            return false;
        }
        // Check if all keys match the pattern arg0, arg1, etc. A key that is not a String (or is null) is not one:
        // it was a ClassCastException here.
        for (Object key : map.keySet()) {
            if (!(key instanceof String) || !isSyntheticArgName((String) key)) {
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
        int argumentCount = argumentValues == null ? 0 : argumentValues.size();
        validateEnhancedSecurity("Constructor argument", argumentCount, getMaxConstructorArgs());

        if (visitedClasses != null && visitedClasses.contains(c)) {
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
        boolean noArgs = argumentCount == 0;
        Object[] suppliedArgs = noArgs ? ArrayUtilities.EMPTY_OBJECT_ARRAY : argumentValues.toArray();
        ArgumentShapeKey argumentShapeKey = ArgumentShapeKey.from(suppliedArgs);
        // A non-static inner class needs its enclosing instance, which a plan does not record: a replayed plan (or a
        // remembered constructor) passed null for it. So nothing is cached for one -- its plans go in a map dropped
        // with this call.
        boolean innerClass = c.getEnclosingClass() != null && !Modifier.isStatic(c.getModifiers());
        Map<ArgumentShapeKey, Optional<ConstructorPlan>> classPlanCache =
                innerClass ? new HashMap<>(4) : getConstructorPlanCache(c);

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
        if (noArgs) {
            try {
                Constructor<?> noArg = c.getDeclaredConstructor();
                trySetAccessible(noArg);
                Object instance = noArg.newInstance(ArrayUtilities.EMPTY_OBJECT_ARRAY);
                if (!innerClass) {
                    rememberConstructor(c, noArg);
                }
                rememberPlan(classPlanCache, argumentShapeKey,
                        Optional.of(new ConstructorPlan(noArg, EMPTY_PARAMETERS, false)));
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
        Optional<Constructor<?>> cachedResult = suppliedArgs.length == 0 && !innerClass ? successfulConstructors.get(c).get() : null;

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
                    rememberPlan(classPlanCache, argumentShapeKey,
                            Optional.of(new ConstructorPlan(cachedConstructor, parameters, false)));
                    return instance;
                } catch (Exception e) {
                    Object[] argsNull = matchArgumentsToParameters(converter, suppliedArgs, parameters, true);
                    Object instance = cachedConstructor.newInstance(argsNull);
                    rememberPlan(classPlanCache, argumentShapeKey,
                            Optional.of(new ConstructorPlan(cachedConstructor, parameters, true)));
                    return instance;
                }
            } catch (Exception ignored) {
                // If cached constructor fails, continue with regular instantiation
                // and potentially update the cache
            }
        }

        // Handle inner classes - with circular reference protection
        if (innerClass) {
            if (visitedClasses == null) {
                visitedClasses = new IdentitySet<>();
            }
            visitedClasses.add(c);

            // For inner classes, try to get the enclosing instance
            Class<?> enclosingClass = c.getEnclosingClass();

            // The enclosing class gets constructed too, so it needs its own verdict — being the outer
            // class of a permitted inner class is not a reason to skip the gate. Verified OUTSIDE the
            // try below, whose catch-all would otherwise downgrade this into a silent fall-through.
            SecurityChecker.verifyClass(enclosingClass);

            try {
                // The enclosing instance: one supplied among the arguments (it was ignored, and a new one made),
                // else one made for it. The other arguments go to the constructor's other parameters.
                Object enclosingInstance = null;
                Object[] otherArgs = suppliedArgs;
                for (int i = 0; i < suppliedArgs.length; i++) {
                    if (enclosingClass.isInstance(suppliedArgs[i])) {
                        enclosingInstance = suppliedArgs[i];
                        otherArgs = new Object[suppliedArgs.length - 1];
                        System.arraycopy(suppliedArgs, 0, otherArgs, 0, i);
                        System.arraycopy(suppliedArgs, i + 1, otherArgs, i, suppliedArgs.length - i - 1);
                        break;
                    }
                }
                if (enclosingInstance == null && !visitedClasses.contains(enclosingClass)) {
                    try {
                        // First try default constructor if available
                        Constructor<?> defaultCtor = enclosingClass.getDeclaredConstructor();
                        trySetAccessible(defaultCtor);
                        enclosingInstance = defaultCtor.newInstance();
                    } catch (Exception e) {
                        // Fall back to creating with empty args (may use Unsafe)
                        enclosingInstance = newInstance(converter, enclosingClass, Collections.emptyList(), visitedClasses);
                    }
                }
                if (enclosingInstance != null) {
                    // Try all constructors where the first parameter is the enclosing class
                    Constructor<?>[] constructors = ReflectionUtils.getAllConstructors(c);
                    for (Constructor<?> constructor : constructors) {
                        Parameter[] params = constructor.getParameters();
                        if (params.length > 0 && params[0].getType().equals(enclosingClass)) {
                            try {
                                trySetAccessible(constructor);

                                if (params.length == 1) {
                                    // Simple case: only takes enclosing instance
                                    return constructor.newInstance(enclosingInstance);
                                } else {
                                    // Complex case: takes enclosing instance plus more arguments
                                    // Create arguments array with enclosing instance first
                                    Object[] restArgs = matchArgumentsToParameters(converter, otherArgs, params, 1, params.length - 1, false);
                                    Object[] allArgs = new Object[params.length];
                                    allArgs[0] = enclosingInstance;
                                    System.arraycopy(restArgs, 0, allArgs, 1, restArgs.length);
                                    return constructor.newInstance(allArgs);
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

        // The plan cache is keyed by the argument TYPES, so it may only record outcomes the types decide.
        // A constructor BODY that throws (InvocationTargetException) has usually rejected a VALUE
        // ("must not be negative"): once that happens, neither the failure nor a fallback constructor
        // chosen because of it may be cached -- the next call, with other values of the same types,
        // must try the constructors again. (Zero supplied arguments carry no values to depend on.)
        boolean valueDependentOutcome = false;

        // Java's choice first (JLS 15.12.2): of the constructors that take the supplied values as they are, the
        // most specific. Only when none does are the constructors tried in order below, converting and filling in --
        // where the first that worked was chosen, so N(String) won over N(long) for 42L, a Long converting to text.
        ExactFit fit = noArgs ? null : findExactFit(constructors, suppliedArgs);
        if (fit != null) {
            try {
                trySetAccessible(fit.constructor);
                Object[] args = new Object[fit.parameters.length];
                for (int i = 0; i < args.length; i++) {
                    args[i] = suppliedArgs[fit.binding.source[i]];
                }
                Object instance = fit.constructor.newInstance(args);
                if (!innerClass) {
                    rememberConstructor(c, fit.constructor);
                }
                rememberPlan(classPlanCache, argumentShapeKey,
                        Optional.of(new ConstructorPlan(fit.constructor, fit.parameters, false,
                                fit.binding.seal(suppliedArgs.length))));
                return instance;
            } catch (Exception e) {
                exceptions.add(constructorFailure(e));
                valueDependentOutcome |= e instanceof InvocationTargetException;
            }
        }

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
            Object[] argsNonNull = null;
            try {
                // Try with non-null arguments first (more precise matching)
                ArgumentBinding binding = new ArgumentBinding(parameters.length);
                argsNonNull = matchArgumentsToParameters(converter, suppliedArgs, parameters, false, binding);
                Object instance = constructor.newInstance(argsNonNull);

                // Cache this successful constructor for future use
                if (noArgs || !valueDependentOutcome) {
                    if (!innerClass) {
                        rememberConstructor(c, constructor);
                    }
                    rememberPlan(classPlanCache, argumentShapeKey,
                            Optional.of(new ConstructorPlan(constructor, parameters, false,
                                    binding.seal(suppliedArgs.length))));
                }
                return instance;
            } catch (Exception e1) {
                exceptions.add(constructorFailure(e1));
                valueDependentOutcome |= e1 instanceof InvocationTargetException;

                // If that fails, try with nulls allowed for unmatched parameters
                try {
                    ArgumentBinding binding = new ArgumentBinding(parameters.length);
                    Object[] argsNull = matchArgumentsToParameters(converter, suppliedArgs, parameters, true, binding);
                    if (argsNonNull != null && Arrays.equals(argsNonNull, argsNull)) {
                        // Every parameter was matched, so the "nulls allowed" retry would pass the very
                        // same arguments -- and fail the same way, at the cost of another exception.
                        continue;
                    }
                    Object instance = constructor.newInstance(argsNull);

                    // Cache this successful constructor for future use
                    if (noArgs || !valueDependentOutcome) {
                        if (!innerClass) {
                            rememberConstructor(c, constructor);
                        }
                        rememberPlan(classPlanCache, argumentShapeKey,
                                Optional.of(new ConstructorPlan(constructor, parameters, true,
                                        binding.seal(suppliedArgs.length))));
                    }
                    return instance;
                } catch (Exception e2) {
                    exceptions.add(constructorFailure(e2));
                    valueDependentOutcome |= e2 instanceof InvocationTargetException;
                    // Continue to next constructor
                }
            }
        }

        boolean cacheFailure = noArgs || !valueDependentOutcome;

        // Last resort: try unsafe instantiation
        Object instance = tryUnsafeInstantiation(c);
        if (instance != null) {
            if (cacheFailure) {
                rememberPlan(classPlanCache, argumentShapeKey, Optional.empty());
            }
            return instance;
        }

        if (cacheFailure) {
            rememberPlan(classPlanCache, argumentShapeKey, Optional.empty());
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

    /** A constructor that takes the supplied values as they are, and which value each parameter gets. */
    private static final class ExactFit {
        final Constructor<?> constructor;
        final Parameter[] parameters;
        final ArgumentBinding binding;
        /** Per supplied value, the type of the parameter it goes to. */
        final Class<?>[] valueTypes;
        final boolean unboxes;

        ExactFit(Constructor<?> constructor, Parameter[] parameters, ArgumentBinding binding) {
            this.constructor = constructor;
            this.parameters = parameters;
            this.binding = binding;
            valueTypes = new Class<?>[parameters.length];
            boolean unboxing = false;
            for (int i = 0; i < parameters.length; i++) {
                Class<?> type = parameters[i].getType();
                valueTypes[binding.source[i]] = type;
                unboxing |= type.isPrimitive();
            }
            unboxes = unboxing;
        }

        /** Every value goes to a parameter type at least as specific as in {@code other}, and one more so. */
        boolean moreSpecificThan(ExactFit other) {
            boolean stricter = false;
            for (int j = 0; j < valueTypes.length; j++) {
                Class<?> mine = boxed(valueTypes[j]);
                Class<?> theirs = boxed(other.valueTypes[j]);
                if (mine == theirs) {
                    continue;
                }
                if (!theirs.isAssignableFrom(mine)) {
                    return false;
                }
                stricter = true;
            }
            return stricter;
        }
    }

    private static Class<?> boxed(Class<?> type) {
        Class<?> wrapper = type.isPrimitive() ? PRIMITIVE_TO_WRAPPER.getByClass(type) : null;
        return wrapper == null ? type : wrapper;
    }

    /**
     * Of the constructors that take the supplied values as Java would pass them -- as many parameters as values, not
     * varargs, every value an instance of its parameter's type (a wrapper for its primitive) -- the one Java would
     * choose (JLS 15.12.2): those needing no unboxing when any does, then the most specific, judged per value (a
     * constructor that gives every value a type at least as specific, and one value a more specific one). Between
     * constructors neither more specific than the other, the first in the constructors' order. Null when none takes
     * the values so, or when a value is null (a null fits many types; the constructors are then tried in order).
     */
    private static ExactFit findExactFit(Constructor<?>[] constructors, Object[] values) {
        for (Object value : values) {
            if (value == null) {
                return null;
            }
        }
        int n = values.length;
        List<ExactFit> fits = null;
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() != n) {
                continue;
            }
            Parameter[] parameters = constructor.getParameters();
            if (parameters[n - 1].isVarArgs()) {
                continue;
            }
            ArgumentBinding binding = new ArgumentBinding(n);
            boolean[] used = new boolean[n];
            boolean[] matched = new boolean[n];
            Object[] result = new Object[n];
            findExactMatches(values, used, parameters, 0, n, matched, result, 0, n, binding);
            findInheritanceMatches(values, used, parameters, 0, n, matched, result, 0, n, binding);
            boolean all = true;
            for (boolean m : matched) {
                all &= m;
            }
            if (all) {
                if (fits == null) {
                    fits = new ArrayList<>(2);
                }
                fits.add(new ExactFit(constructor, parameters, binding));
            }
        }
        if (fits == null) {
            return null;
        }
        boolean anyWithoutUnboxing = false;
        for (ExactFit fit : fits) {
            anyWithoutUnboxing |= !fit.unboxes;
        }
        for (ExactFit fit : fits) {
            if (anyWithoutUnboxing && fit.unboxes) {
                continue;
            }
            boolean beaten = false;
            for (ExactFit other : fits) {
                beaten |= other != fit && !(anyWithoutUnboxing && other.unboxes) && other.moreSpecificThan(fit);
            }
            if (!beaten) {
                return fit;
            }
        }
        return null;        // unreachable: specificity has no cycles
    }

    /**
     * The exception to report for a failed constructor attempt: for an InvocationTargetException, the
     * exception the constructor itself threw -- the wrapper carries no message, so reporting it lost the
     * constructor's own reason ("Most recent error: null").
     */
    private static Exception constructorFailure(Exception e) {
        if (e instanceof InvocationTargetException) {
            Throwable target = ((InvocationTargetException) e).getTargetException();
            if (target instanceof Exception) {
                return (Exception) target;
            }
        }
        return e;
    }

    private static Map<ArgumentShapeKey, Optional<ConstructorPlan>> getConstructorPlanCache(Class<?> c) {
        return constructorPlans.get(c);
    }

    /**
     * Plans kept per class. A class meets a few argument shapes, but the shapes are the caller's: a JSON object's
     * values decide them (null, String, Long, Double, Boolean, Map or array per key is 7^n shapes for n keys), so the
     * map grew without limit -- one class held a plan for every shape it had ever been sent.
     */
    private static final int MAX_PLANS_PER_CLASS = 256;

    /** Past the bound the class's plans start over; the shapes in use are planned again on their next call. */
    private static void rememberPlan(Map<ArgumentShapeKey, Optional<ConstructorPlan>> plans, ArgumentShapeKey key,
                                     Optional<ConstructorPlan> plan) {
        if (plans.size() >= MAX_PLANS_PER_CLASS && !plans.containsKey(key)) {
            plans.clear();
        }
        plans.put(key, plan);
    }

    private static Object invokeConstructorWithPlan(Converter converter, Object[] suppliedArgs, ConstructorPlan plan) throws Exception {
        Parameter[] parameters = plan.parameters;
        if (parameters.length == 0) {
            return plan.constructor.newInstance(ArrayUtilities.EMPTY_OBJECT_ARRAY);
        }
        boolean nullsFirst = plan.allowNullsFirst;
        if (plan.binding != null && plan.binding.isComplete()) {
            // Matched by type throughout: the other null policy would pass the same arguments, so a retry
            // would only repeat the constructor's failure (the discovery loop skips it for the same reason)
            return plan.constructor.newInstance(planArguments(converter, suppliedArgs, plan, nullsFirst));
        }
        try {
            return plan.constructor.newInstance(planArguments(converter, suppliedArgs, plan, nullsFirst));
        } catch (Exception ignored) {
            return plan.constructor.newInstance(planArguments(converter, suppliedArgs, plan, !nullsFirst));
        }
    }

    /** The arguments matching produces for these values: replayed from the plan's binding, or matched. */
    private static Object[] planArguments(Converter converter, Object[] suppliedArgs, ConstructorPlan plan,
                                          boolean allowNulls) {
        if (plan.binding != null) {
            return plan.binding.replay(converter, suppliedArgs, plan.parameters, allowNulls);
        }
        return matchArgumentsToParameters(converter, suppliedArgs, plan.parameters, allowNulls);
    }

    // Per declaring class, its members that could NOT be made accessible (trySetAccessible remembers
    // only failures). This is probed on every instantiation and field injection, so under concurrent
    // deserialization it MUST be lock-free: a Collections.synchronizedMap(WeakHashMap) here serialized
    // every worker thread on a single monitor (the dominant contention point in multi-threaded reads).
    //
    // Per class, so the entries go with their class. It was one ConcurrentHashMap, safe only while
    // every failure was a JDK internal: a named application module that does not open its package
    // (a module layer's loader), or a SecurityManager, refuses too -- and such a member kept its
    // class loader alive for good. Values are JDK types (see perClass).
    private static volatile ClassValue<Set<AccessibleObject>> inaccessibleMembers =
            perClass(c -> ConcurrentHashMap.newKeySet());

    /** The declaring class whose inaccessibleMembers hold {@code object}, or null: remembered nowhere. */
    private static Class<?> declaringClass(AccessibleObject object) {
        return object instanceof Member ? ((Member) object).getDeclaringClass() : null;
    }

    private static void rememberInaccessible(AccessibleObject object) {
        Class<?> declaring = declaringClass(object);
        if (declaring != null) {
            inaccessibleMembers.get(declaring).add(object);
        }
    }
    
    @SuppressWarnings("deprecation")   // isAccessible(): deprecated since 9, not for removal; JDK 8 has no alternative
    static void trySetAccessible(AccessibleObject object) {
        // Already accessible: nothing to do. Calling setAccessible(true) again is NOT a cheap no-op
        // under concurrency: it WRITES the object's override flag, and these Constructor/Field/Method
        // objects are shared by every thread (ReflectionUtils caches them), so each call invalidated
        // that cache line on every other core -- 8 threads constructing the same class ran >10x slower
        // than one. isAccessible() only reads the flag. The check is per INSTANCE, so a second Field
        // object for the same field (see below) still gets its own setAccessible(true).
        if (object.isAccessible()) {
            return;
        }

        // Check cache for known failures only. We only cache failures to avoid expensive
        // repeated exception throwing on JPMS-sealed modules. We do NOT cache successes because
        // the lookup is equals()-based, and Field.equals() matches by declaring class + name +
        // type. Different Field instances for the same logical field (from separate
        // getDeclaredFields() calls) would incorrectly share a success, causing the second
        // instance to never get setAccessible(true) called.
        Class<?> declaring = declaringClass(object);
        if (declaring != null && inaccessibleMembers.get(declaring).contains(object)) {
            return;
        }

        try {
            object.setAccessible(true);
        } catch (SecurityException e) {
            rememberInaccessible(object);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Unable to set accessible: " + object + " - " + e.getMessage());
            }
            throw e; // Don't suppress security exceptions - they indicate important access control violations
        } catch (Throwable t) {
            // Only ignore non-security exceptions (like InaccessibleObjectException in Java 9+)
            rememberInaccessible(object);
            safelyIgnoreException(t);
        }
    }

    // Try instantiation via ReflectionFactory serialization constructor (if turned on).
    // It is off by default.  Use ClassUtilities.setUseUnsafe(true) to enable it.
    // This uses the same mechanism as ObjectInputStream — creates a synthetic constructor
    // that runs Object.<init>() instead of the class's own constructors.
    private static Object tryUnsafeInstantiation(Class<?> c) {
        // Never an enum: its constants are its only instances, and one made here was a fake (name null, ordinal 0,
        // equal to none of them)
        if (unsafeDepth.get() > 0 && !Enum.class.isAssignableFrom(c)) {
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

        // 3) A class first, as in an intersection type (JLS 4.9) -- there is at most one lowest common class, the
        //    superclass chains being single -- then by the sum of the distances from both input classes. Which is
        //    first matters to findLowestCommonSupertype(); the distance only orders interfaces unrelated to each other.
        List<Class<?>> candidates = new ArrayList<>(common);
        ClassHierarchyInfo infoA = getClassHierarchyInfo(classA);
        ClassHierarchyInfo infoB = getClassHierarchyInfo(classB);
        candidates.sort((x, y) -> {
            if (x.isInterface() != y.isInterface()) {
                return x.isInterface() ? 1 : -1;
            }
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
            // type is indeed a "lowest" so far. The sort is by distance, not by inheritance, so one of its own
            // ancestors may have come first (List before AbstractList, for ArrayList and LinkedList): drop it.
            Set<Class<?>> supertypes = getClassHierarchyInfo(type).getAllSupertypes();
            lowest.removeIf(supertypes::contains);
            lowest.add(type);

            // Add all type's supertypes to the union set
            unionOfAncestors.addAll(supertypes);
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
     * <p>
     * The supertypes are those of the Java Language Specification (JLS 4.10.2, 4.10.3), each at the fewest steps
     * through direct supertypes: a class's superclass and superinterfaces; an interface's superinterfaces, or
     * {@code Object} when it has none; for an array of references {@code S[]}, the array {@code T[]} of each
     * direct supertype {@code T} of {@code S}; and {@code Object}, {@code Cloneable} and {@code Serializable} for
     * {@code Object[]} and for an array of primitives. So the supertypes of a reference type are exactly the types
     * it is assignable to. Beyond the specification, a primitive and its wrapper are one step apart.
     *
     * @param clazz The class to analyze
     * @return ClassHierarchyInfo containing all supertypes and distances
     */
    public static ClassHierarchyInfo getClassHierarchyInfo(Class<?> clazz) {
        return classHierarchies.get(clazz);
    }

    /**
     * A type's direct supertypes as the Java Language Specification defines them (JLS 4.10.2, 4.10.3): a class's
     * superclass and superinterfaces; an interface's superinterfaces, or Object when it has none; for an array of
     * references S[], the array T[] of each direct supertype T of S; and Object, Cloneable and Serializable for
     * Object[] and for an array of primitives. A primitive has none here (its widening, JLS 4.10.1, is
     * computeInheritanceDistance's).
     */
    private static List<Class<?>> directSupertypes(Class<?> type) {
        if (type.isArray()) {
            Class<?> component = type.getComponentType();
            if (component.isPrimitive() || component == Object.class) {
                return Arrays.asList(Object.class, Cloneable.class, Serializable.class);
            }
            List<Class<?>> supertypes = directSupertypes(component);
            List<Class<?>> arrays = new ArrayList<>(supertypes.size());
            for (Class<?> supertype : supertypes) {
                arrays.add(Array.newInstance(supertype, 0).getClass());
            }
            return arrays;
        }
        Class<?> superclass = type.getSuperclass();
        Class<?>[] interfaces = type.getInterfaces();
        List<Class<?>> supertypes = new ArrayList<>(interfaces.length + 1);
        if (superclass != null) {
            supertypes.add(superclass);
        } else if (type.isInterface() && interfaces.length == 0) {
            supertypes.add(Object.class);
        }
        Collections.addAll(supertypes, interfaces);
        return supertypes;
    }

    private static ClassHierarchyInfo computeClassHierarchyInfo(Class<?> key) {
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
                for (Class<?> supertype : directSupertypes(current)) {
                    if (!distanceMap.containsKey(supertype)) {
                        distanceMap.put(supertype, currentDistance + 1);
                        queue.add(supertype);
                    }
                }
            }
        }

        // Add primitive/wrapper counterpart at distance 1 so that inheritance matching
        // handles boxing/unboxing (e.g., Integer value → int parameter) without a separate phase.
        Class<?> counterpart = PRIMITIVE_TO_WRAPPER.getByClass(key);
        if (counterpart == null) {
            counterpart = WRAPPER_TO_PRIMITIVE.getByClass(key);
        }
        if (counterpart != null && !distanceMap.containsKey(counterpart)) {
            distanceMap.put(counterpart, 1);
            allSupertypes.add(counterpart);
        }

        return new ClassHierarchyInfo(allSupertypes, distanceMap);     // which wraps them unmodifiable
    }
    
    // Convenience boolean method
    public static boolean haveCommonAncestor(Class<?> a, Class<?> b) {
        return !findLowestCommonSupertypes(a, b).isEmpty();
    }

    // ---------------------------------------------------------------------------------------------
    // SecurityChecker backing state.
    //
    // Rules come from two tiers, deliberately separated for performance:
    //
    //   1. Built-ins plus anything supplied via the classutilities.security.* system properties are
    //      fixed at class-initialization time. A verdict derived only from them is immutable for a
    //      given Class, so it is memoized in SECURITY_CHECK_CACHE — a ClassValue, whose computeValue()
    //      runs at most once per Class lifetime.
    //   2. Rules added programmatically (addBlockedClass/addBlockedPackage/allowClass) are consulted
    //      BEFORE that cache. That ordering is what lets them take effect immediately with no
    //      cache-invalidation machinery at all. The overwhelmingly common case — no programmatic
    //      rules — costs one volatile boolean read on top of the pre-existing ClassValue lookup.
    // ---------------------------------------------------------------------------------------------
    private static final ClassValueSet BLOCKED_CLASSES = new ClassValueSet();
    private static final Set<String> BLOCKED_CLASS_NAMES_SET;
    private static final String[] BLOCKED_PACKAGES;
    private static final Set<String> UNBLOCKED_CLASS_NAMES;

    // Tier 2: programmatic overrides. hasRuntimeSecurityRules gates all three lookups so the
    // default configuration pays a single volatile read rather than three set probes.
    private static volatile boolean hasRuntimeSecurityRules;
    // Set by the first runtime rule and never reset: forName() then checks cached classes and aliases on every hit,
    // for they were verified under the rules of the time -- before a later block, or under an unblock since cleared.
    private static volatile boolean verifyCachedClasses;
    private static final Set<String> runtimeBlockedNames = ConcurrentHashMap.newKeySet();
    private static final Set<String> runtimeBlockedPackages = ConcurrentHashMap.newKeySet();
    private static final Set<String> runtimeUnblockedNames = ConcurrentHashMap.newKeySet();

    static {
        // Pre-populate with all blocked classes
        BLOCKED_CLASSES.addAll(SecurityChecker.SECURITY_BLOCKED_CLASSES.toSet());

        Set<String> names = new HashSet<>(SecurityChecker.SECURITY_BLOCKED_CLASS_NAMES);
        names.addAll(parseCsvProperty("classutilities.security.blocked.classes"));
        BLOCKED_CLASS_NAMES_SET = Collections.unmodifiableSet(names);

        Set<String> packages = new LinkedHashSet<>(SecurityChecker.SECURITY_BLOCKED_PACKAGES);
        packages.addAll(parseCsvProperty("classutilities.security.blocked.packages"));
        BLOCKED_PACKAGES = packages.toArray(new String[0]);

        UNBLOCKED_CLASS_NAMES = Collections.unmodifiableSet(
                new HashSet<>(parseCsvProperty("classutilities.security.unblocked")));
    }

    private static Set<String> parseCsvProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : value.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                tokens.add(trimmed);
            }
        }
        return tokens;
    }

    private static final ClassValue<Boolean> SECURITY_CHECK_CACHE = new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return builtInBlocked(type, Collections.<String>emptySet());
        }
    };

    /**
     * The verdict of the init-time (tier 1) rules for {@code type}.
     *
     * @param alsoUnblocked supertype names whose rules to skip besides the tier-1 unblocks: the supertypes unblocked
     *                      at runtime, which exempt a subclass only from the rules that match through them
     */
    private static Boolean builtInBlocked(Class<?> type, Set<String> alsoUnblocked) {
        // Direct blocked class check (ultra-fast with ClassValueSet). The internal-package rule
        // is applied HERE ONLY, to the class itself -- never to its supertypes: the danger is an
        // internal class's own constructors, while many public JDK classes merely implement an
        // internal interface (java.nio.DirectByteBuffer implements sun.nio.ch.DirectBuffer).
        String typeName = type.getName();
        if (!isUnblockedName(typeName)
                && (BLOCKED_CLASSES.containsClass(type) || isBlockedName(typeName)
                    || isBlockedInternalName(typeName))) {
            return Boolean.TRUE;
        }

        // Need to check inheritance - use ClassHierarchyInfo. Consult the blocked-class set, the
        // blocked-NAME set, AND the blocked package prefixes: entries like
        // javax.script.ScriptEngine[Manager] and every third-party gadget family exist only by
        // name (their Class may not be loadable at init), and without the name check a class
        // merely IMPLEMENTING/extending them would pass verifyClass() even though loading it by
        // name would have been refused. Matching supertypes by name is also what lets a single
        // entry cover a whole family — org.springframework.context.ApplicationContext blocks
        // every *ApplicationContext implementation without java-util depending on Spring.
        //
        // Do NOT memoize the verdict in a static set: this ClassValue IS the per-class cache
        // (computeValue runs at most once per Class lifetime, and a re-loaded class is a new
        // identity that must be re-verified). A strong-reference memo set can never produce a
        // hit this cache wouldn't, but it pins every checked class — and its ClassLoader —
        // for the JVM's lifetime, unreleasable even by clearCaches().
        for (Class<?> superType : getClassHierarchyInfo(type).getAllSupertypes()) {
            String superName = superType.getName();
            if (isUnblockedName(superName) || alsoUnblocked.contains(superName)) {
                // An explicit unblock defeats the rule that would have matched here, so a
                // subclass of an unblocked type is not blocked by virtue of that supertype.
                continue;
            }
            if (BLOCKED_CLASSES.containsClass(superType) || isBlockedName(superName)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /** Exact-name or blocked-package-prefix match against the init-time (tier 1) rules. */
    private static boolean isBlockedName(String className) {
        if (BLOCKED_CLASS_NAMES_SET.contains(className)) {
            return true;
        }
        for (String prefix : BLOCKED_PACKAGES) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * JDK/vendor internal package, unless it is one of the few internal types JSON legitimately names.
     * Matched against the class ITSELF (its name, or the loaded class), not against its supertypes.
     */
    private static boolean isBlockedInternalName(String className) {
        for (String prefix : SecurityChecker.SECURITY_BLOCKED_INTERNAL_PACKAGES) {
            if (className.startsWith(prefix)) {
                if (SecurityChecker.INTERNAL_PACKAGE_EXEMPT_CLASS_NAMES.contains(className)) {
                    return false;
                }
                for (String exempt : SecurityChecker.INTERNAL_PACKAGE_EXEMPT_PREFIXES) {
                    if (className.startsWith(exempt)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static boolean isUnblockedName(String className) {
        return !UNBLOCKED_CLASS_NAMES.isEmpty() && UNBLOCKED_CLASS_NAMES.contains(className);
    }

    /** Exact-name or blocked-package-prefix match against the programmatic (tier 2) rules. */
    private static boolean isRuntimeBlockedName(String className) {
        if (runtimeBlockedNames.contains(className)) {
            return true;
        }
        for (String prefix : runtimeBlockedPackages) {
            if (className.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Applies the programmatic overrides to {@code type} and its supertypes.
     *
     * @return TRUE if a runtime rule blocks it, FALSE if a runtime unblock clears it, or
     *         {@code null} when no runtime rule applies and the tier-1 verdict should stand.
     */
    private static Boolean runtimeVerdict(Class<?> type) {
        String name = type.getName();
        if (runtimeUnblockedNames.contains(name)) {
            return Boolean.FALSE;               // an unblock of the class itself overrides every rule, built-in too
        }
        if (isRuntimeBlockedName(name)) {
            return Boolean.TRUE;
        }
        boolean unblockedSupertype = false;
        for (Class<?> superType : getClassHierarchyInfo(type).getAllSupertypes()) {
            String superName = superType.getName();
            if (runtimeUnblockedNames.contains(superName)) {
                unblockedSupertype = true;      // exempts only from the rules that match through it
            } else if (isRuntimeBlockedName(superName)) {
                return Boolean.TRUE;
            }
        }
        // With no supertype unblocked at runtime, the built-in verdict (cached) stands. Otherwise it is worked out
        // again without the rules that match through those supertypes: the class's own, and its other supertypes',
        // still apply. (The first unblocked supertype returned FALSE, which lifted every built-in rule: unblocking
        // java.io.Flushable unblocked FileOutputStream, and java.lang.Object unblocked ProcessBuilder.)
        return unblockedSupertype ? builtInBlocked(type, runtimeUnblockedNames) : null;
    }

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
        // Drop the loader-cache memo too — it fronts NAME_CACHE, and keeping it would hand out
        // the now-orphaned holder (stale class mappings) after the backing map was cleared.
        lastLoaderCache = new WeakReference<>(null);
        // Preserve user-added aliases while clearing and re-adding built-in aliases
        GLOBAL_ALIASES.clear();
        GLOBAL_ALIASES.putAll(BUILTIN_ALIASES);
        GLOBAL_ALIASES.putAll(USER_ALIASES);
        // The per-class caches are replaced: a ClassValue cannot be emptied (see perClass)
        successfulConstructors = perClass(c -> new AtomicReference<>());
        constructorPlans = perClass(c -> new ConcurrentHashMap<>());
        namedConstructors = perClass(ClassUtilities::namedConstructorPlan);
        classHierarchies = perClass(ClassUtilities::computeClassHierarchyInfo);
        assignableTypes = perClass(ClassUtilities::findAssignableSupplier);
        osgiClassLoaders = perClass(c -> new AtomicReference<>());
        osgiPresent = true;
        inaccessibleMembers = perClass(c -> ConcurrentHashMap.newKeySet());
        // ENUM_CLASS_CACHE, SECURITY_CHECK_CACHE and POSITIONAL_KEY_ORDER are ClassValues too, left as they are.
    }

    /**
     * The instantiation and class-resolution gate for {@link ClassUtilities}.
     * <p>
     * Every path that resolves a class by name or reflectively constructs one passes through
     * {@link #verifyClass(Class)} / {@link #isSecurityBlockedName(String)} first. This matters most
     * for deserialization: a library that reads a type name out of untrusted input (a JSON
     * {@code @type}, an XML attribute) and hands the sibling fields to that type's constructor is
     * only as safe as this gate.
     * <p>
     * A class is blocked when it — or <em>any</em> of its supertypes — matches a blocked class
     * identity, a blocked class name, or a blocked package prefix. Matching supertypes by name is
     * what lets one entry cover a whole family: {@code org.springframework.context.ApplicationContext}
     * blocks every {@code *ApplicationContext} implementation without java-util depending on Spring.
     * <p>
     * Classes in the JDK and platform-vendor <em>internal</em> packages ({@code sun.}, {@code com.sun.},
     * {@code com.apple.}, {@code apple.}) are blocked — the class itself, not classes that merely extend or
     * implement an internal type: on JDK 8 — and on JDK 9-15 under the
     * default {@code --illegal-access=permit} — every constructor in them is reachable, and some take raw
     * native pointers whose finalizers crash the JVM when handed an arbitrary number. The few internal
     * types that ordinary JSON names are exempt from that rule only: {@code sun.util.calendar.ZoneInfo}
     * (a {@code TimeZone}), {@code sun.nio.fs.UnixPath} / {@code WindowsPath} (a {@code Path}), and the
     * {@code sun.nio.cs.} {@code Charset} implementations.
     * <p>
     * <b>Precedence</b>, highest first:
     * <ol>
     *     <li>An explicit unblock ({@link #allowClass(String)} or the
     *         {@code classutilities.security.unblocked} property) matching the class or a supertype.</li>
     *     <li>Any blocking rule — built-in, system property, or programmatic — matching the class or
     *         a supertype.</li>
     *     <li>Otherwise allowed.</li>
     * </ol>
     * <b>Configuration.</b> Rules may be added programmatically via {@link #addBlockedClass(String)}
     * and {@link #addBlockedPackage(String)}, or at JVM startup via comma-separated system properties:
     * <ul>
     *     <li>{@code classutilities.security.blocked.classes} — additional exact class names</li>
     *     <li>{@code classutilities.security.blocked.packages} — additional blocked package prefixes</li>
     *     <li>{@code classutilities.security.unblocked} — class names to exempt from blocking</li>
     * </ul>
     *
     * @see #allowClass(String) for the caveats on unblocking
     */
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

        // Blocked by NAME rather than by Class literal, for two reasons: the class may not be loadable
        // when this list initializes (third-party gadgets are not on java-util's classpath), and the
        // supertype walk in SECURITY_CHECK_CACHE matches these names against every ancestor, so one
        // entry covers an entire implementation family.
        static final Set<String> SECURITY_BLOCKED_CLASS_NAMES = new HashSet<>(CollectionUtilities.listOf(
                // --- Process execution ---
                "java.lang.ProcessImpl",
                "java.lang.Runtime",
                "java.lang.ProcessBuilder",
                "java.lang.System",

                // --- Scripting engines: eval() of attacker-supplied source ---
                "javax.script.ScriptEngineManager",
                "javax.script.ScriptEngine",
                "groovy.lang.GroovyShell",
                "groovy.util.Eval",

                // --- Module / member access. Deliberately Lookup ONLY: it is the object that grants
                // reflective module-opening power, whereas MethodHandle is abstract and MethodHandles
                // has no public constructor, so neither is instantiable. Blocking those two names
                // would only break legitimate resolution of an ordinary MethodHandle-typed field.
                "java.lang.invoke.MethodHandles$Lookup",
                "sun.misc.Unsafe",
                "jdk.internal.misc.Unsafe",

                // --- Indirect loaders: the constructor fetches and INTERPRETS an attacker-named
                // resource, instantiating further classes beyond this gate's reach. Listed as the
                // root interface so every context/factory implementation is caught.
                "org.springframework.context.ApplicationContext",
                "org.springframework.beans.factory.BeanFactory",

                // --- JNDI: lookup() against an attacker URL is the classic remote-codebase gadget ---
                "javax.naming.Context",
                "com.sun.rowset.JdbcRowSetImpl",

                // --- XSLT: Templates carries compiled bytecode that newTransformer() defines and runs ---
                "javax.xml.transform.Templates",

                // --- Java serialization: hands the payload the entire ObjectInputStream gadget surface ---
                "java.io.ObjectInputStream",

                // --- Dynamic proxies: a $Proxy class's constructor takes an InvocationHandler, and every
                // interface call on the instance -- equals(), hashCode(), compareTo() from a HashMap or
                // TreeSet -- is routed to it. The root name covers every proxy class on every JDK
                // (com.sun.proxy.$ProxyN through 15, jdk.proxyN.$ProxyN from 16). EventHandler is the
                // JDK's handler that turns such a call into an arbitrary method call on an arbitrary target.
                "java.lang.reflect.Proxy",
                "java.beans.EventHandler",

                // --- Constructors that perform network I/O: SSRF, port scanning, egress from the victim ---
                "java.net.Socket",
                "java.net.ServerSocket",
                "java.net.DatagramSocket",
                "java.net.URLConnection",
                "java.net.InetSocketAddress",        // (String host, int port) resolves the host name

                // --- Constructors that create or truncate an attacker-named file: integrity / DoS ---
                "java.io.FileOutputStream",
                "java.io.FileWriter",
                "java.io.RandomAccessFile",
                "java.io.PrintStream",               // (String fileName) / (File)
                "java.io.PrintWriter",               // (String fileName) / (File)
                "java.util.Formatter",               // (String fileName) / (File)
                "javax.imageio.stream.FileImageOutputStream",

                // --- Constructors that DELETE an attacker-named file: ZipFile's OPEN_DELETE mode.
                // Covers java.util.jar.JarFile through the supertype walk.
                "java.util.zip.ZipFile",

                // --- Logging: a Handler's constructor opens a log file (FileHandler, plus its .lck
                // file) or a socket (SocketHandler); the root name covers the whole family. The
                // LogManager constructor registers a JVM shutdown hook.
                "java.util.logging.Handler",
                "java.util.logging.LogManager",

                // --- Constructors that start a thread: one payload can start thousands ---
                "java.util.Timer"
        ));

        // Blocked package prefixes. Applied to the class being checked AND to every supertype, on both
        // the class-loading path and the instantiation path.
        static final Set<String> SECURITY_BLOCKED_PACKAGES = new LinkedHashSet<>(CollectionUtilities.listOf(
                "javax.script.",             // Script engines
                "jdk.nashorn.",              // Nashorn JavaScript engine
                "org.mozilla.javascript.",   // Rhino
                "bsh.",                      // BeanShell
                "org.python.",               // Jython
                "javax.naming.",             // JNDI
                "com.sun.jndi.",
                "com.sun.org.apache.xalan.", // TemplatesImpl and friends
                "java.rmi.",
                "javax.management.remote.",
                "jdk.internal.",
                "jdk.jshell.",               // JShell: executes code, launches a debuggee JVM
                "jdk.jfr.",                  // Flight Recorder: a Recording creates its repository on disk
                "jdk.management.jfr.",
                "java.util.prefs."           // writes the preferences store; starts timers, shutdown hooks
        ));

        // JDK and platform-vendor INTERNAL packages. On JDK 8 -- and on JDK 9-15, whose default
        // --illegal-access=permit lets setAccessible() reach them -- every constructor in these
        // packages is callable, and some take raw native pointers or handles that a finalizer later
        // dereferences: handing one an attacker-chosen number crashes the JVM (SIGSEGV), not merely
        // the request. From JDK 16 on they are unreachable anyway. Blocked as a family, minus the
        // few internal types that ordinary JSON legitimately names -- see
        // INTERNAL_PACKAGE_EXEMPT_CLASS_NAMES / _PREFIXES. The exemptions apply ONLY to this rule:
        // an exact-name or ordinary package block (com.sun.jndi., sun.misc.Unsafe, ...) still wins.
        // Unlike the other rules this one matches the class ITSELF, not its supertypes: the danger is
        // the internal class's own constructors, and many public JDK classes merely implement an
        // internal interface (java.nio.DirectByteBuffer implements sun.nio.ch.DirectBuffer).
        static final Set<String> SECURITY_BLOCKED_INTERNAL_PACKAGES = new LinkedHashSet<>(CollectionUtilities.listOf(
                "sun.",
                "com.sun.",
                "com.apple.",
                "apple."
        ));

        // Internal types that JSON legitimately names. json-io's config/aliases.txt resolves all three
        // through forName() while initializing ReadOptionsBuilder -- in every release 4.102.0-4.112.0 --
        // so blocking any of them would stop json-io from starting at all, not merely refuse one type.
        //   ZoneInfo  - the runtime class of TimeZone.getDefault() / TimeZone.getTimeZone(id)
        //   UnixPath / WindowsPath - the runtime class of a java.nio.file.Path
        static final Set<String> INTERNAL_PACKAGE_EXEMPT_CLASS_NAMES = new HashSet<>(CollectionUtilities.listOf(
                "sun.util.calendar.ZoneInfo",
                "sun.nio.fs.UnixPath",
                "sun.nio.fs.WindowsPath"
        ));

        // The JDK's Charset implementations (sun.nio.cs.UTF_8, ...); json-io writes their class name
        // for a Charset held in an Object-typed field. Constructing one has no side effect.
        static final Set<String> INTERNAL_PACKAGE_EXEMPT_PREFIXES = new LinkedHashSet<>(CollectionUtilities.listOf(
                "sun.nio.cs."
        ));

        /**
         * Checks if a class is blocked for security reasons. A class is blocked when it or any of its
         * supertypes matches a blocked identity, name, or package prefix — see the class javadoc for
         * full precedence rules.
         *
         * @param clazz The class to check
         * @return true if the class is blocked, false otherwise
         */
        public static boolean isSecurityBlocked(Class<?> clazz) {
            if (hasRuntimeSecurityRules) {
                Boolean verdict = runtimeVerdict(clazz);
                if (verdict != null) {
                    return verdict;
                }
            }
            return SECURITY_CHECK_CACHE.get(clazz);
        }

        /**
         * Checks if a class name is directly in the blocked list or belongs to a blocked package.
         * Used before class loading, when there is no {@code Class} to walk the supertypes of.
         *
         * @param className The class name to check
         * @return true if the class name is blocked, false otherwise
         */
        public static boolean isSecurityBlockedName(String className) {
            if (hasRuntimeSecurityRules) {
                if (runtimeUnblockedNames.contains(className)) {
                    return false;
                }
                if (isRuntimeBlockedName(className)) {
                    return true;
                }
            }
            if (isUnblockedName(className)) {
                return false;
            }
            return isBlockedName(className) || isBlockedInternalName(className);
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

        /**
         * Blocks an additional class by exact name. Subclasses are blocked too, via the supertype walk.
         * Takes effect immediately, including for classes already checked, cached by {@code forName()}, or named
         * by an alias.
         *
         * @param className fully-qualified class name to block; must not be null or blank
         * @throws IllegalArgumentException if {@code className} is null or blank
         */
        public static void addBlockedClass(String className) {
            runtimeBlockedNames.add(requireConfigValue(className, "className"));
            verifyCachedClasses = true;
            hasRuntimeSecurityRules = true;
        }

        /**
         * Blocks an additional package by prefix. Any class whose name — or the name of any of its
         * supertypes — starts with {@code packagePrefix} is blocked. Takes effect immediately.
         * <p>
         * The value is used as a raw string prefix, so it should normally end with {@code '.'}
         * ({@code "com.acme.unsafe."} rather than {@code "com.acme.unsafe"}, which would also match
         * {@code com.acme.unsafeButFine}).
         *
         * @param packagePrefix package name prefix to block; must not be null or blank
         * @throws IllegalArgumentException if {@code packagePrefix} is null or blank
         */
        public static void addBlockedPackage(String packagePrefix) {
            runtimeBlockedPackages.add(requireConfigValue(packagePrefix, "packagePrefix"));
            verifyCachedClasses = true;
            hasRuntimeSecurityRules = true;
        }

        /**
         * Exempts a class from blocking, overriding every blocking rule — built-in included. Provided
         * for the consumer who legitimately constructs one of the blocked types and accepts the
         * consequences of re-enabling it.
         * <p>
         * <b>Use with care.</b> The built-in list exists because these types let attacker-controlled
         * constructor arguments reach a process, a socket, a file, a class loader, or a script engine.
         * Unblocking one on a code path that instantiates types named in untrusted input reopens
         * exactly the hole the list closes. Unblocking a type also exempts its subclasses from the
         * rule that would have matched through it.
         *
         * @param className fully-qualified class name to exempt; must not be null or blank
         * @throws IllegalArgumentException if {@code className} is null or blank
         */
        public static void allowClass(String className) {
            runtimeUnblockedNames.add(requireConfigValue(className, "className"));
            verifyCachedClasses = true;
            hasRuntimeSecurityRules = true;
        }

        /**
         * Discards every rule added through {@link #addBlockedClass}, {@link #addBlockedPackage}, and
         * {@link #allowClass}, restoring the built-in and system-property configuration. Intended for
         * tests; note that it also discards any unblocks, so it never loosens the default posture: a class that
         * only an unblock admitted is refused again, even when {@code forName()} has cached it.
         */
        public static void clearSecurityOverrides() {
            runtimeBlockedNames.clear();
            runtimeBlockedPackages.clear();
            runtimeUnblockedNames.clear();
            hasRuntimeSecurityRules = false;
        }

        private static String requireConfigValue(String value, String argName) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(argName + " cannot be null or blank");
            }
            return value.trim();
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
