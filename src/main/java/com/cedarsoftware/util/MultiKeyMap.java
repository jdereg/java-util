package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
// ReentrantLock replaced with synchronized monitors for lower uncontended latency
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-performance N-dimensional key-value Map implementation - the definitive solution for multidimensional lookups.
 *
 * <p>MultiKeyMap allows storing and retrieving values using multiple keys. Unlike traditional maps that
 * use a single key, this map can handle keys with any number of components, making it ideal for complex
 * lookup scenarios like user permissions, configuration trees, and caching systems.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><b>N-Dimensional Keys:</b> Support for keys with any number of components (1, 2, 3, ... N).</li>
 *   <li><b>High Performance:</b> Zero-allocation polymorphic storage, polynomial rolling hash, and optimized hash computation — no GC/heap pressure for gets in flat cases.</li>
 *   <li><b>Thread-Safe:</b> Lock-free reads with auto-tuned stripe locking that scales with your server cores, similar to ConcurrentHashMap.</li>
 *   <li><b>Map Interface Compatible:</b> Supports single-key operations via the standard Map interface (get()/put() automatically unpack Collections/Arrays into multi-keys).</li>
 *   <li><b>Flexible API:</b> Var-args methods for convenient multi-key operations (getMultiKey()/putMultiKey() with many keys).</li>
 *   <li><b>Smart Collection Handling:</b> Configurable behavior for Collections via {@link CollectionKeyMode} — change the default automatic unpacking capability as needed.</li>
 *   <li><b>N-Dimensional Array Expansion:</b> Nested arrays of any depth are automatically flattened recursively into multi-keys.</li>
 *   <li><b>Cross-Container Equivalence:</b> Arrays and ordered Collections (Lists) with equivalent structure are treated as identical keys.</li>
 *   <li><b>Set Support:</b> Sets are treated as order-agnostic containers. Sets only match other Sets (not Lists/Arrays), and matching is independent of element order.</li>
 * </ul>
 *
 * <h3>Dimensional Behavior Control:</h3>
 * <p>MultiKeyMap provides revolutionary control over how dimensions are handled through the {@code flattenDimensions} parameter:</p>
 * <ul>
 *   <li><b>Structure-Preserving Mode (default, flattenDimensions = false):</b> Different structural depths remain distinct keys. 
 *       Arrays/Collections with different nesting levels create separate entries.</li>
 *   <li><b>Dimension-Flattening Mode (flattenDimensions = true):</b> All equivalent flat representations are treated as identical keys, 
 *       regardless of original container structure.</li>
 * </ul>
 *
 * <h3>Performance Characteristics:</h3>
 * <ul>
 *   <li><b>Lock-Free Reads:</b> Get operations require no locking for optimal concurrent performance</li>
 *   <li><b>Auto-Tuned Stripe Locking:</b> Write operations use stripe locking that adapts to your server's core count</li>
 *   <li><b>Zero-Allocation Gets:</b> No temporary objects created during retrieval operations</li>
 *   <li><b>Polymorphic Storage:</b> Efficient memory usage adapts storage format based on key complexity</li>
 *   <li><b>Simple Keys Mode:</b> Optional performance optimization that skips nested structure checks when keys are known to be flat</li>
 * </ul>
 *
 * <h3>Capacity and Size Limits:</h3>
 * <p>MultiKeyMap uses {@code AtomicLong} internally for size tracking, allowing it to scale well beyond
 * the traditional {@code Integer.MAX_VALUE} (2<sup>31</sup>-1) limitation:</p>
 * <ul>
 *   <li><b>Actual Capacity:</b> The map can hold many more than 2<sup>31</sup>-1 entries, limited only by heap memory.</li>
 *   <li><b>size() Method:</b> Due to the {@link Map#size()} interface contract requiring {@code int},
 *       the {@link #size()} method returns {@link Integer#MAX_VALUE} if the actual size exceeds this limit.</li>
 *   <li><b>longSize() Method:</b> Use {@link #longSize()} to get the exact count for very large maps.
 *       This method returns the true size as a {@code long} without the 2<sup>31</sup>-1 cap.</li>
 *   <li><b>Memory Requirements:</b> Storing 2<sup>31</sup> entries requires approximately 200-300 GB of heap memory
 *       (assuming ~100-150 bytes per entry including bucket overhead).</li>
 *   <li><b>Practical Limit:</b> On modern servers with 512GB-2TB RAM, maps with billions of entries are feasible.</li>
 * </ul>
 *
 * <h3>Performance Complexity:</h3>
 * <p>MultiKeyMap provides constant-time performance for most operations:</p>
 * <ul>
 *   <li><b>get/put/remove/containsKey:</b> O(k) average, O(k + c) worst case
 *       <br>where k = key components, c = chain length (typically 1-3)</li>
 *   <li><b>size/longSize/isEmpty:</b> O(1) - atomic counter access</li>
 *   <li><b>clear:</b> O(capacity) - must clear all buckets</li>
 *   <li><b>keySet/values/entrySet:</b> O(n) - creates snapshot of n entries</li>
 * </ul>
 * <p><b>Key Processing Complexity:</b></p>
 * <ul>
 *   <li>Simple keys (String, Integer): O(1) hash and comparison</li>
 *   <li>Multi-dimensional keys: O(k) where k = number of components</li>
 *   <li>Nested collections: O(k × m) where k = depth, m = average size per level</li>
 *   <li>Set keys: ~3-4x slower than List keys (order-agnostic hashing)</li>
 * </ul>
 *
 * <h3>Value-Based vs Type-Based Equality:</h3>
 * <p>MultiKeyMap provides two equality modes for key comparison, controlled via the {@code valueBasedEquality} parameter:</p>
 * <ul>
 *   <li><b>Value-Based Equality (default, valueBasedEquality = true):</b> Cross-type numeric comparisons work naturally.
 *       Integer 1 equals Long 1L equals Double 1.0. This mode is ideal for configuration lookups and user-friendly APIs.</li>
 *   <li><b>Type-Based Equality (valueBasedEquality = false):</b> Strict type checking - Integer 1 ≠ Long 1L.
 *       This mode provides traditional Java Map semantics and maximum performance.</li>
 * </ul>
 *
 * <h4>Value-Based Equality Edge Cases:</h4>
 * <ul>
 *   <li><b>NaN Behavior:</b> In value-based mode, {@code NaN == NaN} returns true (unlike Java's default).
 *       This ensures consistent key lookups with floating-point values.</li>
 *   <li><b>Zero Handling:</b> {@code +0.0 == -0.0} returns true in both modes (standard Java behavior).</li>
 *   <li><b>BigDecimal Precision:</b> Doubles are converted via {@code new BigDecimal(number.toString())}.
 *       This means {@code 0.1d} equals {@code BigDecimal("0.1")} but NOT {@code BigDecimal(0.1)} 
 *       (the latter has binary rounding errors).</li>
 *   <li><b>Infinity Handling:</b> Comparing {@code Double.POSITIVE_INFINITY} or {@code NEGATIVE_INFINITY} 
 *       to BigDecimal returns false (BigDecimal cannot represent infinity).</li>
 *   <li><b>Atomic Types:</b> In type-based mode, only identical atomic types match (AtomicInteger ≠ Integer).
 *       In value-based mode, atomic types participate in numeric families (AtomicInteger(1) == Integer(1)).</li>
 * </ul>
 *
 * <h3>Case Sensitivity for CharSequences:</h3>
 * <p>MultiKeyMap provides configurable case sensitivity for CharSequence keys (String, StringBuilder, etc.),
 * controlled via the {@code caseSensitive} parameter:</p>
 * <ul>
 *   <li><b>Case-Sensitive Mode (default, caseSensitive = true):</b> CharSequences are compared using their
 *       standard equals() methods. "Hello" and "hello" are different keys.</li>
 *   <li><b>Case-Insensitive Mode (caseSensitive = false):</b> All CharSequence instances are compared
 *       case-insensitively. "Hello", "HELLO", and "hello" are treated as the same key.</li>
 * </ul>
 *
 * <h3>API Overview:</h3>
 * <p>MultiKeyMap provides two complementary APIs:</p>
 * <ul>
 *   <li><b>Map Interface:</b> Use as {@code Map<Object, V>} for compatibility with existing code and single-key operations</li>
 *   <li><b>MultiKeyMap API:</b> Declare as {@code MultiKeyMap<V>} to access powerful var-args methods for multidimensional operations</li>
 * </ul>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Basic multi-dimensional usage
 * MultiKeyMap<String> map = new MultiKeyMap<>();
 * map.putMultiKey("user-config", "user123", "settings", "theme");
 * String theme = map.getMultiKey("user123", "settings", "theme");
 * 
 * // Cross-container equivalence
 * map.put(new String[]{"key1", "key2"}, "value1");           // Array key
 * String value = map.get(Arrays.asList("key1", "key2"));     // Collection lookup - same key!
 * 
 * // Structure-preserving vs flattening modes
 * MultiKeyMap<String> structured = MultiKeyMap.<String>builder().flattenDimensions(false).build(); // Structure-preserving (default)
 * MultiKeyMap<String> flattened = MultiKeyMap.<String>builder().flattenDimensions(true).build();   // Dimension-flattening
 * 
 * // Performance optimization for flat keys (no nested arrays/collections)
 * MultiKeyMap<String> fast = MultiKeyMap.<String>builder()
 *     .simpleKeysMode(true)  // Skip nested structure checks for maximum performance
 *     .capacity(50000)       // Pre-size for known data volume
 *     .build();
 * 
 * // Value-based vs Type-based equality
 * MultiKeyMap<String> valueMap = MultiKeyMap.<String>builder().valueBasedEquality(true).build();  // Default
 * valueMap.putMultiKey("found", 1, 2L, 3.0);        // Mixed numeric types
 * String result = valueMap.getMultiKey(1L, 2, 3);   // Found! Cross-type numeric matching
 * 
 * MultiKeyMap<String> typeMap = MultiKeyMap.<String>builder().valueBasedEquality(false).build();
 * typeMap.putMultiKey("int-key", 1, 2, 3);
 * String missing = typeMap.getMultiKey(1L, 2L, 3L); // null - different types don't match
 * 
 * // Case-insensitive string keys
 * MultiKeyMap<String> caseInsensitive = MultiKeyMap.<String>builder().caseSensitive(false).build();
 * caseInsensitive.putMultiKey("value", "USER", "Settings", "THEME");
 * String found = caseInsensitive.getMultiKey("user", "settings", "theme"); // Found! Case doesn't matter
 *
 * // Set support - order-agnostic matching
 * MultiKeyMap<String> map = new MultiKeyMap<>();
 * Set<String> coordinates = new HashSet<>(Arrays.asList("x", "y", "z"));
 * map.put(coordinates, "value");
 *
 * // Sets match other Sets regardless of order or Set type
 * Set<String> lookup1 = new LinkedHashSet<>(Arrays.asList("z", "x", "y"));  // Different order
 * assertEquals("value", map.get(lookup1));  // Found! Order doesn't matter
 *
 * Set<String> lookup2 = new TreeSet<>(Arrays.asList("y", "z", "x"));  // Different Set type
 * assertEquals("value", map.get(lookup2));  // Found! Set type doesn't matter
 *
 * // Sets don't match Lists/Arrays (semantic distinction)
 * List<String> listLookup = Arrays.asList("x", "y", "z");
 * assertNull(map.get(listLookup));  // Not found - Lists don't match Sets
 * }</pre>
 *
 * <p>For comprehensive examples and advanced usage patterns, see the user guide documentation.</p>
 *
 * <h3>Set Semantics:</h3>
 * <p>Sets are treated as order-agnostic containers with the following behavior:</p>
 * <ul>
 *   <li><b>Order-Agnostic Matching:</b> Sets match other Sets regardless of element order. {@code Set.of(1,2,3)} equals {@code Set.of(3,2,1)}.</li>
 *   <li><b>Type Independence:</b> All Set types (HashSet, TreeSet, LinkedHashSet) are treated equivalently based on their elements.</li>
 *   <li><b>Semantic Distinction:</b> Sets only match other Sets - they do not match Lists or Arrays even with identical elements.</li>
 *   <li><b>Nested Sets:</b> Sets within arrays, lists, or other sets are properly handled with order-agnostic semantics.</li>
 *   <li><b>Performance:</b> Set operations are approximately 3-4x slower than List operations due to order-agnostic hash computation.</li>
 *   <li><b>Empty Sets:</b> Empty Sets are distinct from empty Lists/Arrays.</li>
 * </ul>
 *
 * <h3>Complex Key Examples with Sets:</h3>
 * <p>Sets can be combined with other key types in multi-dimensional keys:</p>
 * <pre>{@code
 * MultiKeyMap<String> map = new MultiKeyMap<>();
 *
 * // Example 1: Set combined with Object[] as a multi-key
 * Set<String> permissions = new HashSet<>(Arrays.asList("read", "write", "execute"));
 * Object[] userKey = new Object[]{"user123", permissions, "config"};
 * map.put(userKey, "user-permissions-config");
 *
 * // Lookup works with different Set order
 * Set<String> samePermsDiffOrder = new LinkedHashSet<>(Arrays.asList("execute", "read", "write"));
 * Object[] lookupKey = new Object[]{"user123", samePermsDiffOrder, "config"};
 * assertEquals("user-permissions-config", map.get(lookupKey));  // Found!
 *
 * // Example 2: Set combined with List as a multi-key
 * Set<Integer> tags = Set.of(100, 200, 300);  // Order doesn't matter
 * List<String> path = Arrays.asList("api", "v1", "users");  // Order DOES matter
 * map.putMultiKey("endpoint-handler", tags, path);
 *
 * // Lookup with different Set order but same List order
 * Set<Integer> sameTags = Set.of(300, 100, 200);  // Different order - still matches
 * List<String> samePath = Arrays.asList("api", "v1", "users");  // Same order required
 * assertEquals("endpoint-handler", map.getMultiKey(sameTags, samePath));  // Found!
 *
 * // Different List order will NOT match
 * List<String> differentPath = Arrays.asList("v1", "api", "users");  // Different order
 * assertNull(map.getMultiKey(sameTags, differentPath));  // Not found!
 * }</pre>
 *
 * @param <V> the type of values stored in the map
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
public final class MultiKeyMap<V> implements ConcurrentMap<Object, V> {

    private static final Logger LOG = Logger.getLogger(MultiKeyMap.class.getName());

    static {
        LoggingConfig.init();
    }

    // Sentinels as custom objects - identity-based equality prevents user key collisions
    private static final Object OPEN = new Object() {
        @Override public String toString() { return "["; }
        @Override public int hashCode() { return "[".hashCode(); }
    };
    private static final Object CLOSE = new Object() {
        @Override public String toString() { return "]"; }
        @Override public int hashCode() { return "]".hashCode(); }
    };
    private static final Object SET_OPEN = new Object() {
        @Override public String toString() { return "{"; }
        @Override public int hashCode() { return "{".hashCode(); }
    };
    private static final Object SET_CLOSE = new Object() {
        @Override public String toString() { return "}"; }
        @Override public int hashCode() { return "}".hashCode(); }
    };
    private static final Object NULL_SENTINEL = new Object() {
        @Override public String toString() { return "∅"; }
        @Override public int hashCode() { return "∅".hashCode(); }
    };
    
    // Pre-created MultiKey for null to avoid allocation on every null key operation
    @SuppressWarnings("rawtypes")
    private static final MultiKey NULL_NORMALIZED_KEY = new MultiKey<>(NULL_SENTINEL, 0, null);

    // ThreadLocal arrays for zero-allocation multi-key lookups (getMultiKey/containsMultiKey)
    // Each size has its own ThreadLocal to provide exact-sized arrays without allocation
    private static final ThreadLocal<Object[]> LOOKUP_KEY_2 = ThreadLocal.withInitial(() -> new Object[2]);
    private static final ThreadLocal<Object[]> LOOKUP_KEY_3 = ThreadLocal.withInitial(() -> new Object[3]);
    private static final ThreadLocal<Object[]> LOOKUP_KEY_4 = ThreadLocal.withInitial(() -> new Object[4]);
    private static final ThreadLocal<Object[]> LOOKUP_KEY_5 = ThreadLocal.withInitial(() -> new Object[5]);

    // ThreadLocal holder for normalized key and hash - avoids allocation on GET operations
    private static final class Norm {
        Object key;
        int hash;
    }

    // Common strings
    private static final String THIS_MAP = "(this Map ♻️)"; // Recycle for cycles

    // Emojis for debug output (professional yet intuitive)
    private static final String EMOJI_OPEN = "[";   // Opening bracket for Lists
    private static final String EMOJI_CLOSE = "]"; // Closing bracket for Lists
    private static final String EMOJI_SET_OPEN = "{";   // Opening brace for Sets
    private static final String EMOJI_SET_CLOSE = "}"; // Closing brace for Sets
    private static final String EMOJI_CYCLE = "♻️"; // Recycle for cycles
    private static final String EMOJI_EMPTY = "∅";  // Empty set for null/empty
    private static final String EMOJI_KEY = "🆔 ";   // ID for keys (with space)
    private static final String EMOJI_VALUE = "🟣 "; // Purple circle for values (with space)

    // JDK DTO array types that are guaranteed to be 1D (elements can't be arrays/collections)
    // Using ConcurrentHashMap-backed Set for thread-safe, high-performance lookups
    private static final Set<Class<?>> SIMPLE_ARRAY_TYPES = new ClassValueSet();
    static {
        // Wrapper types
        SIMPLE_ARRAY_TYPES.add(String[].class);
        SIMPLE_ARRAY_TYPES.add(Integer[].class);
        SIMPLE_ARRAY_TYPES.add(Long[].class);
        SIMPLE_ARRAY_TYPES.add(Double[].class);
        SIMPLE_ARRAY_TYPES.add(Float[].class);
        SIMPLE_ARRAY_TYPES.add(Boolean[].class);
        SIMPLE_ARRAY_TYPES.add(Character[].class);
        SIMPLE_ARRAY_TYPES.add(Byte[].class);
        SIMPLE_ARRAY_TYPES.add(Short[].class);
        
        // Date/Time types
        SIMPLE_ARRAY_TYPES.add(Date[].class);
        SIMPLE_ARRAY_TYPES.add(java.sql.Date[].class);
        SIMPLE_ARRAY_TYPES.add(java.sql.Time[].class);
        SIMPLE_ARRAY_TYPES.add(java.sql.Timestamp[].class);
        
        // java.time types (Java 8+)
        SIMPLE_ARRAY_TYPES.add(java.time.LocalDate[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.LocalTime[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.LocalDateTime[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.ZonedDateTime[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.OffsetDateTime[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.OffsetTime[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.Instant[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.Duration[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.Period[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.Year[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.YearMonth[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.MonthDay[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.ZoneId[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.ZoneOffset[].class);
        
        // Math/Precision types
        SIMPLE_ARRAY_TYPES.add(BigInteger[].class);
        SIMPLE_ARRAY_TYPES.add(BigDecimal[].class);
        
        // Network/IO types
        SIMPLE_ARRAY_TYPES.add(java.net.URL[].class);
        SIMPLE_ARRAY_TYPES.add(java.net.URI[].class);
        SIMPLE_ARRAY_TYPES.add(java.net.InetAddress[].class);
        SIMPLE_ARRAY_TYPES.add(java.net.Inet4Address[].class);
        SIMPLE_ARRAY_TYPES.add(java.net.Inet6Address[].class);
        SIMPLE_ARRAY_TYPES.add(java.io.File[].class);
        SIMPLE_ARRAY_TYPES.add(java.nio.file.Path[].class);
        
        // Utility types
        SIMPLE_ARRAY_TYPES.add(java.util.UUID[].class);
        SIMPLE_ARRAY_TYPES.add(java.util.Locale[].class);
        SIMPLE_ARRAY_TYPES.add(java.util.Currency[].class);
        SIMPLE_ARRAY_TYPES.add(java.util.TimeZone[].class);
        SIMPLE_ARRAY_TYPES.add(java.util.regex.Pattern[].class);

        // Enum arrays are also simple (enums can't contain collections/arrays)
        SIMPLE_ARRAY_TYPES.add(java.time.DayOfWeek[].class);
        SIMPLE_ARRAY_TYPES.add(java.time.Month[].class);
        SIMPLE_ARRAY_TYPES.add(java.nio.file.StandardOpenOption[].class);
        SIMPLE_ARRAY_TYPES.add(java.nio.file.LinkOption[].class);
    }

    // Static flag to log stripe configuration only once per JVM
    private static final AtomicBoolean STRIPE_CONFIG_LOGGED = new AtomicBoolean(false);
    
    // Contention monitoring fields (retained from original)
    private final AtomicInteger totalLockAcquisitions = new AtomicInteger(0);
    private final AtomicInteger contentionCount = new AtomicInteger(0);
    private final AtomicInteger[] stripeLockContention = new AtomicInteger[STRIPE_COUNT];
    private final AtomicInteger[] stripeLockAcquisitions = new AtomicInteger[STRIPE_COUNT];
    private final AtomicInteger globalLockAcquisitions = new AtomicInteger(0);
    private final AtomicInteger globalLockContentions = new AtomicInteger(0);

    // Prevent concurrent resize operations to avoid deadlock
    private final AtomicBoolean resizeInProgress = new AtomicBoolean(false);

    /**
     * Controls how Collections are treated when used as keys in MultiKeyMap.
     * <p>Note: Arrays are ALWAYS expanded regardless of this setting, as they cannot
     * override equals/hashCode and would only compare by identity (==).</p>
     * 
     * @since 3.6.0
     */
    public enum CollectionKeyMode {
        /**
         * Collections are automatically unpacked into multi-key entries (default behavior).
         * A List.of("a", "b", "c") becomes a 3-dimensional key equivalent to calling
         * getMultiKey("a", "b", "c").
         */
        COLLECTIONS_EXPANDED,
        
        /**
         * Collections are treated as single key objects and not unpacked.
         * A List.of("a", "b", "c") remains as a single Collection key.
         * Use this mode when you want Collections to be compared by their equals() method
         * rather than being expanded into multidimensional keys.
         */
        COLLECTIONS_NOT_EXPANDED
    }

    private volatile AtomicReferenceArray<MultiKey<V>[]> buckets;
    private final AtomicLong atomicSize = new AtomicLong(0);
    // Diagnostic metric: tracks the maximum chain length seen since map creation (never decreases on remove)
    private final AtomicInteger maxChainLength = new AtomicInteger(0);
    private final int capacity;
    private final float loadFactor;
    private final CollectionKeyMode collectionKeyMode;
    private final boolean flattenDimensions;
    private final boolean simpleKeysMode;
    private final boolean valueBasedEquality;
    private final boolean caseSensitive;
    private final boolean trackContentionMetrics;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    // Cached hashCode for performance (invalidated on mutations)
    // Volatile ensures visibility when mutations set to null; no lock needed since computation is idempotent
    private transient volatile Integer cachedHashCode;

    // Pre-computed resize threshold to avoid per-put multiplication
    private volatile long resizeThreshold;

    // Contention detection threshold for timing-based metrics (1 microsecond)
    private static final long CONTENTION_THRESHOLD_NS = 1000;

    private static final int STRIPE_COUNT = calculateOptimalStripeCount();
    private static final int STRIPE_MASK = STRIPE_COUNT - 1;
    private final Object[] stripeMonitors = new Object[STRIPE_COUNT];

    private static final class MultiKey<V> {
        // Kind constants for fast type-based switching
        static final byte KIND_SINGLE = 0;    // Single object
        static final byte KIND_OBJECT_ARRAY = 1;  // Object[] array
        static final byte KIND_COLLECTION = 2;    // Collection (List, etc.)
        static final byte KIND_PRIMITIVE_ARRAY = 3; // Primitive arrays (int[], etc.)
        
        final Object keys;  // Polymorphic: Object (single), Object[] (flat multi), Collection<?> (nested multi)
        final int hash;
        final V value;
        final int size;    // Number of keys (1 for single, array.length for arrays, collection.size() for collections)
        final byte kind;    // Type of keys structure (0=single, 1=obj[], 2=collection, 3=prim[])

        // Unified constructor that accepts pre-normalized keys and pre-computed hash
        MultiKey(Object normalizedKeys, int hash, V value) {
            this.keys = normalizedKeys;
            this.hash = hash;
            this.value = value;
            
            // Compute and cache size and kind for fast operations
            if (normalizedKeys == null) {
                this.size = 1;
                this.kind = KIND_SINGLE;
            } else {
                Class<?> keyClass = normalizedKeys.getClass();
                if (keyClass.isArray()) {
                    this.size = ArrayUtilities.getLength(normalizedKeys);
                    // Check if it's a primitive array
                    Class<?> componentType = keyClass.getComponentType();
                    this.kind = (componentType != null && componentType.isPrimitive()) 
                        ? KIND_PRIMITIVE_ARRAY 
                        : KIND_OBJECT_ARRAY;
                } else if (normalizedKeys instanceof Collection) {
                    this.size = ((Collection<?>) normalizedKeys).size();
                    this.kind = KIND_COLLECTION;
                } else {
                    this.size = 1;
                    this.kind = KIND_SINGLE;
                }
            }
        }

        // Fast constructor when caller already knows size and kind (avoids reflection)
        MultiKey(Object normalizedKeys, int hash, V value, int size, byte kind) {
            this.keys = normalizedKeys;
            this.hash = hash;
            this.value = value;
            this.size = size;
            this.kind = kind;
        }

        @Override
        public String toString() {
            return dumpExpandedKeyStatic(keys, true, null);  // Use emoji rendering
        }
    }

    /**
     * Returns a power of 2 size for the given target capacity.
     * This method implements the same logic as HashMap's tableSizeFor method,
     * ensuring optimal hash table performance through power-of-2 sizing.
     * 
     * @param cap the target capacity
     * @return the smallest power of 2 greater than or equal to cap, or 1 if cap <= 0
     */
    private static int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= (1 << 30)) ? (1 << 30) : n + 1;
    }

    // Private constructor called by Builder
    private MultiKeyMap(Builder<V> builder) {
        if (builder.loadFactor <= 0 || Float.isNaN(builder.loadFactor)) {
            throw new IllegalArgumentException("Load factor must be positive: " + builder.loadFactor);
        }
        if (builder.capacity < 0) {
            throw new IllegalArgumentException("Illegal initial capacity: " + builder.capacity);
        }

        // Ensure capacity is a power of 2 AND at least STRIPE_COUNT.
        // The minimum guarantees that table mask >= STRIPE_MASK, so entries
        // in the same bucket always map to the same stripe lock.
        int actualCapacity = Math.max(tableSizeFor(builder.capacity), STRIPE_COUNT);
        this.buckets = new AtomicReferenceArray<>(actualCapacity);
        // Store the ACTUAL capacity, not the requested one, to avoid confusion
        this.capacity = actualCapacity;
        this.loadFactor = builder.loadFactor;
        this.collectionKeyMode = builder.collectionKeyMode;
        this.flattenDimensions = builder.flattenDimensions;
        this.simpleKeysMode = builder.simpleKeysMode;
        this.valueBasedEquality = builder.valueBasedEquality;
        this.caseSensitive = builder.caseSensitive;
        this.trackContentionMetrics = builder.trackContentionMetrics;

        for (int i = 0; i < STRIPE_COUNT; i++) {
            stripeMonitors[i] = new Object();
            stripeLockContention[i] = new AtomicInteger(0);
            stripeLockAcquisitions[i] = new AtomicInteger(0);
        }

        resizeThreshold = (long)(actualCapacity * loadFactor);

        if (STRIPE_CONFIG_LOGGED.compareAndSet(false, true) && LOG.isLoggable(Level.INFO)) {
            LOG.info(String.format("MultiKeyMap stripe configuration: %d monitors for %d cores",
                    STRIPE_COUNT, Runtime.getRuntime().availableProcessors()));
        }
    }

    // Copy constructor
    public MultiKeyMap(MultiKeyMap<? extends V> source) {
        this(MultiKeyMap.<V>builder().from(source).capacity(source.buckets.length()));
        
        source.withAllStripeLocks(() -> {  // Lock for consistent snapshot
            final AtomicReferenceArray<? extends MultiKey<? extends V>[]> sourceTable = source.buckets;  // Pin source table reference
            final int len = sourceTable.length();
            for (int i = 0; i < len; i++) {
                MultiKey<? extends V>[] chain = sourceTable.get(i);
                if (chain != null) {
                    for (MultiKey<? extends V> entry : chain) {
                        if (entry != null) {
                            // Re-use keys directly - no copying  
                            V value = entry.value;
                            MultiKey<V> newKey = new MultiKey<>(entry.keys, entry.hash, value);
                            putInternal(newKey);
                        }
                    }
                }
            }
        });
    }


    // Keep the most commonly used convenience constructors
    public MultiKeyMap() {
        this(MultiKeyMap.builder());
    }

    public MultiKeyMap(int capacity) {
        this(MultiKeyMap.<V>builder().capacity(capacity));
    }

    public MultiKeyMap(int capacity, float loadFactor) {
        this(MultiKeyMap.<V>builder().capacity(capacity).loadFactor(loadFactor));
    }

    // Builder class
    /**
     * Builder for creating configured MultiKeyMap instances.
     * <p>The builder provides a fluent API for configuring various aspects of the map's behavior:</p>
     * <ul>
     *   <li>{@code capacity} - Initial capacity (will be rounded up to power of 2)</li>
     *   <li>{@code loadFactor} - Load factor for resizing (default 0.75)</li>
     *   <li>{@code collectionKeyMode} - How Collections are treated as keys</li>
     *   <li>{@code flattenDimensions} - Whether to flatten nested structures</li>
     *   <li>{@code simpleKeysMode} - Performance optimization for non-nested keys</li>
     *   <li>{@code valueBasedEquality} - Enable cross-type numeric matching (default true)</li>
     *   <li>{@code caseSensitive} - Whether CharSequence comparisons are case-sensitive (default true)</li>
     * </ul>
     */
    public static class Builder<V> {
        private int capacity = 16;
        private float loadFactor = DEFAULT_LOAD_FACTOR;
        private CollectionKeyMode collectionKeyMode = CollectionKeyMode.COLLECTIONS_EXPANDED;
        private boolean flattenDimensions = false;
        private boolean simpleKeysMode = false;
        private boolean valueBasedEquality = true;  // Default: cross-type numeric matching
        private boolean caseSensitive = true;  // Default: case-sensitive string comparison
        private boolean trackContentionMetrics = false;  // Default: off for maximum write throughput

        // Private constructor - instantiate via MultiKeyMap.builder()
        private Builder() {}

        /**
         * Sets the initial capacity of the map.
         * <p>The actual capacity will be rounded up to the nearest power of 2 for optimal performance.</p>
         * 
         * @param capacity the initial capacity (must be non-negative)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if capacity is negative
         */
        public Builder<V> capacity(int capacity) {
            if (capacity < 0) {
                throw new IllegalArgumentException("Capacity must be non-negative");
            }
            this.capacity = capacity;
            return this;
        }

        /**
         * Sets the load factor for the map.
         * <p>The load factor determines when the map will resize. A value of 0.75 means
         * the map will resize when it's 75% full.</p>
         * 
         * @param loadFactor the load factor (must be positive)
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if loadFactor is not positive or is NaN
         */
        public Builder<V> loadFactor(float loadFactor) {
            if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
                throw new IllegalArgumentException("Load factor must be positive");
            }
            this.loadFactor = loadFactor;
            return this;
        }

        /**
         * Sets the collection key mode for the map.
         * <p>This determines how Collections are treated when used as keys:</p>
         * <ul>
         *   <li>{@code COLLECTIONS_EXPANDED} (default) - Collections are unpacked into multi-dimensional keys</li>
         *   <li>{@code COLLECTIONS_NOT_EXPANDED} - Collections are treated as single key objects</li>
         * </ul>
         * 
         * @param mode the collection key mode (must not be null)
         * @return this builder instance for method chaining
         * @throws NullPointerException if mode is null
         */
        public Builder<V> collectionKeyMode(CollectionKeyMode mode) {
            this.collectionKeyMode = Objects.requireNonNull(mode);
            return this;
        }

        /**
         * Sets whether to flatten nested dimensions.
         * <p>When enabled, nested arrays and collections are recursively flattened so that
         * all equivalent flat representations are treated as the same key.</p>
         * <p>When disabled (default), structure is preserved and different nesting levels
         * create distinct keys.</p>
         * 
         * @param flatten {@code true} to flatten nested structures, {@code false} to preserve structure
         * @return this builder instance for method chaining
         */
        public Builder<V> flattenDimensions(boolean flatten) {
            this.flattenDimensions = flatten;
            return this;
        }

        /**
         * Enables simple keys mode for maximum performance.
         * <p>When enabled, the map assumes keys do not contain nested arrays or collections,
         * allowing it to skip expensive nested structure checks. This provides significant
         * performance improvements when you know your keys are "flat" (no nested containers).</p>
         * <p><b>Warning:</b> If you enable this mode but use keys with nested arrays/collections,
         * they will not be expanded and may not match as expected.</p>
         * 
         * @param simple {@code true} to enable simple keys optimization, {@code false} for normal operation
         * @return this builder instance for method chaining
         */
        public Builder<V> simpleKeysMode(boolean simple) {
            this.simpleKeysMode = simple;
            return this;
        }
        
        /**
         * Enables value-based equality for numeric keys.
         * <p>When enabled, numeric keys are compared by value rather than type:</p>
         * <ul>
         *   <li>Integral types (byte, short, int, long) compare as longs</li>
         *   <li>Floating point types (float, double) compare as doubles</li>
         *   <li>Float/double can equal integers only when they represent whole numbers</li>
         *   <li>Booleans only equal other booleans</li>
         *   <li>Characters only equal other characters</li>
         * </ul>
         * <p>Default is {@code true} (value-based equality with cross-type numeric matching).</p>
         * 
         * @param valueBasedEquality {@code true} to enable value-based equality, {@code false} for type-based
         * @return this builder instance for method chaining
         */
        public Builder<V> valueBasedEquality(boolean valueBasedEquality) {
            this.valueBasedEquality = valueBasedEquality;
            return this;
        }

        /**
         * Sets whether CharSequence comparisons should be case-sensitive.
         * <p>When disabled (false), all CharSequence instances (String, StringBuilder, etc.) 
         * are compared case-insensitively for both equality and hashing.</p>
         * <p>Default is {@code true} (case-sensitive comparison).</p>
         * 
         * @param caseSensitive {@code true} for case-sensitive comparison, {@code false} for case-insensitive
         * @return this builder instance for method chaining
         * @since 3.6.0
         */
        public Builder<V> caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }

        /**
         * Enables or disables lock contention metrics tracking.
         * <p>When enabled, the map tracks per-stripe lock acquisition counts and contention
         * events using atomic counters. This adds measurable overhead (multiple {@code AtomicInteger}
         * increments per write operation) but provides diagnostic data accessible via
         * {@link MultiKeyMap#printContentionStatistics()}.</p>
         * <p>Default is {@code false} (disabled) for maximum write throughput.</p>
         *
         * @param track {@code true} to enable contention tracking, {@code false} to disable
         * @return this builder instance for method chaining
         */
        public Builder<V> trackContentionMetrics(boolean track) {
            this.trackContentionMetrics = track;
            return this;
        }

        /**
         * Copies configuration from an existing MultiKeyMap.
         * <p>This copies all configuration settings including capacity, load factor,
         * collection key mode, and dimension flattening settings.</p>
         * 
         * @param source the MultiKeyMap to copy configuration from
         * @return this builder instance for method chaining
         */
        public Builder<V> from(MultiKeyMap<?> source) {
            this.capacity = source.capacity;
            this.loadFactor = source.loadFactor;
            this.collectionKeyMode = source.collectionKeyMode;
            this.flattenDimensions = source.flattenDimensions;
            this.simpleKeysMode = source.simpleKeysMode;
            this.valueBasedEquality = source.valueBasedEquality;
            this.caseSensitive = source.caseSensitive;
            this.trackContentionMetrics = source.trackContentionMetrics;
            return this;
        }

        /**
         * Builds and returns a new MultiKeyMap with the configured settings.
         * 
         * @return a new MultiKeyMap instance with the specified configuration
         */
        public MultiKeyMap<V> build() {
            return new MultiKeyMap<>(this);
        }
    }

    // Static factory for builder
    public static <V> Builder<V> builder() {
        return new Builder<>();
    }

    /**
     * Returns the current collection key mode setting.
     * <p>This mode determines how Collections are treated when used as keys in this map.</p>
     * 
     * @return the current {@link CollectionKeyMode} - either COLLECTIONS_EXPANDED (default) 
     *         where Collections are automatically unpacked into multi-key entries, or 
     *         COLLECTIONS_NOT_EXPANDED where Collections are treated as single key objects
     * @see CollectionKeyMode
     */
    public CollectionKeyMode getCollectionKeyMode() {
        return collectionKeyMode;
    }

    /**
     * Returns the current dimension flattening setting.
     * <p>This setting controls how nested arrays and collections are handled when used as keys.</p>
     * 
     * @return {@code true} if dimension flattening is enabled (all equivalent flat representations 
     *         are treated as identical keys regardless of original container structure), 
     *         {@code false} if structure-preserving mode is used (default, where different 
     *         structural depths remain distinct keys)
     */
    public boolean getFlattenDimensions() {
        return flattenDimensions;
    }
    
    /**
     * Returns the current simple keys mode setting.
     * <p>This performance optimization setting indicates whether the map assumes keys do not
     * contain nested arrays or collections.</p>
     * 
     * @return {@code true} if simple keys mode is enabled (nested structure checks are skipped
     *         for maximum performance), {@code false} if normal operation with full nested
     *         structure support
     */
    public boolean getSimpleKeysMode() {
        return simpleKeysMode;
    }
    
    /**
     * Returns the current case sensitivity setting for CharSequence comparisons.
     * <p>This setting controls how CharSequence instances (String, StringBuilder, etc.)
     * are compared within keys.</p>
     *
     * @return {@code true} if case-sensitive comparison is enabled (default),
     *         {@code false} if case-insensitive comparison is used
     * @since 3.6.0
     */
    public boolean getCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Returns the initial capacity setting for this map.
     * <p>This is the capacity value specified when the map was built, which was used
     * to determine the initial internal bucket array size.</p>
     *
     * @return the initial capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns the load factor setting for this map.
     * <p>The load factor determines when the map will resize. A value of 0.75 means
     * the map will resize when it's 75% full.</p>
     *
     * @return the load factor
     */
    public float getLoadFactor() {
        return loadFactor;
    }

    /**
     * Returns the value-based equality setting for numeric keys.
     * <p>When enabled, numeric keys are compared by value rather than type.
     * For example, Integer(42) will match Long(42) when value-based equality is enabled.</p>
     *
     * @return {@code true} if value-based equality is enabled (default),
     *         {@code false} if type-based equality is used
     */
    public boolean getValueBasedEquality() {
        return valueBasedEquality;
    }

    private static int computeElementHash(Object key, boolean caseSensitive) {
        if (key == null) return 0;

        // Fast path: Class identity check for most common types
        // This avoids multiple instanceof checks for the common case
        Class<?> keyClass = key.getClass();

        if (keyClass == String.class) {
            return caseSensitive ? key.hashCode() : StringUtilities.hashCodeIgnoreCase((String) key);
        }
        if (keyClass == Integer.class) return hashLong(((Integer) key).longValue());
        if (keyClass == Long.class) return hashLong((Long) key);
        if (keyClass == Double.class) {
            double d = (Double) key;
            if (d == 0.0d) d = 0.0d; // Canonicalize -0.0
            if (Double.isFinite(d) && d == Math.rint(d) && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
                return hashLong((long) d);
            }
            return hashDouble(d);
        }

        // Less common types - use instanceof checks
        if (key instanceof Number || key instanceof Boolean || key instanceof AtomicBoolean) {
            return valueHashCode(key); // align whole floats with integrals
        }

        // Handle CharSequences with case sensitivity
        if (!caseSensitive && key instanceof CharSequence) {
            return StringUtilities.hashCodeIgnoreCase((CharSequence) key);
        }

        // Non-numeric, non-boolean, non-char types use their natural hashCode
        return key.hashCode();
    }
    
    /**
     * Compute hash code that aligns with value-based equality semantics.
     * Based on the provided reference implementation.
     */
    private static int valueHashCode(Object o) {
        if (o == null) return 0;

        // Fast path: Check most common types first using Class identity (faster than instanceof)
        // Integer, Long, Double are by far the most common numeric types in practice
        Class<?> clazz = o.getClass();

        if (clazz == Integer.class) return hashLong(((Integer) o).longValue());
        if (clazz == Long.class) return hashLong((Long) o);
        if (clazz == Double.class) {
            double d = (Double) o;
            if (d == 0.0d) d = 0.0d; // Canonicalize -0.0 to +0.0
            if (Double.isFinite(d) && d == Math.rint(d) &&
                    d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
                return hashLong((long) d);
            }
            return hashDouble(d);
        }

        // Less common primitive wrappers
        if (clazz == Float.class) {
            double d = ((Float) o).doubleValue();
            if (d == 0.0d) d = 0.0d; // Canonicalize -0.0 to +0.0
            if (Double.isFinite(d) && d == Math.rint(d) &&
                    d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
                return hashLong((long) d);
            }
            return hashDouble(d);
        }
        if (clazz == Short.class) return hashLong(((Short) o).longValue());
        if (clazz == Byte.class) return hashLong(((Byte) o).longValue());

        // Boolean types
        if (clazz == Boolean.class) return Boolean.hashCode((Boolean) o);
        if (clazz == AtomicBoolean.class) return Boolean.hashCode(((AtomicBoolean) o).get());

        // Atomic numeric types
        if (clazz == AtomicInteger.class) return hashLong(((AtomicInteger) o).get());
        if (clazz == AtomicLong.class) return hashLong(((AtomicLong) o).get());

        // BigInteger/BigDecimal: convert to primitive type for consistent hashing
        if (o instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) o;
            try {
                // Check if it can be represented as a long (whole number)
                if (bd.scale() <= 0 || bd.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
                    // It's a whole number - try to convert to long
                    if (bd.compareTo(new BigDecimal(Long.MAX_VALUE)) <= 0 &&
                        bd.compareTo(new BigDecimal(Long.MIN_VALUE)) >= 0) {
                        return hashLong(bd.longValue());
                    }
                }
                // Not a whole number or too large for long - use double representation
                double d = bd.doubleValue();
                if (d == 0.0d) d = 0.0d; // canonicalize -0.0
                if (Double.isFinite(d) && d == Math.rint(d) &&
                        d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
                    return hashLong((long) d);
                }
                return hashDouble(d);
            } catch (Exception e) {
                // Fallback to original hash
                return bd.hashCode();
            }
        }

        if (o instanceof BigInteger) {
            BigInteger bi = (BigInteger) o;
            try {
                // Try to convert to long if it fits
                if (bi.bitLength() < 64) {
                    return hashLong(bi.longValue());
                }
                // Too large for long - use double approximation
                double d = bi.doubleValue();
                if (Double.isFinite(d) && d == Math.rint(d) &&
                        d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
                    return hashLong((long) d);
                }
                return hashDouble(d);
            } catch (Exception e) {
                // Fallback to original hash
                return bi.hashCode();
            }
        }

        // Other Number types: use their hash
        return o.hashCode();
    }
    
    private static int hashLong(long v) {
        return (int) (v ^ (v >>> 32));
    }
    
    private static int hashDouble(double d) {
        // Use the canonicalized IEEE bits (doubleToLongBits collapses all NaNs to one NaN)
        long bits = Double.doubleToLongBits(d);
        return (int) (bits ^ (bits >>> 32));
    }

    /**
     * Spreads higher bits of the hash into lower bits to improve bucket distribution.
     * When the table is small, only the low-order bits select the bucket. Without
     * spreading, hashes that differ only in higher bits collide in the same bucket.
     * This is the same technique used by {@code ConcurrentHashMap}.
     */
    private static int spread(int h) {
        return h ^ (h >>> 16);
    }

    private int getStripeIndex(int hash) {
        // Stripe is determined solely by the hash, independent of table size.
        // This is safe because the constructor enforces capacity >= STRIPE_COUNT,
        // guaranteeing that all entries in the same bucket share the same stripe
        // (same low-order bits up to STRIPE_MASK).  Being table-size-independent
        // also eliminates a race window between computing the stripe and acquiring
        // the lock: a concurrent resize can no longer redirect us to a wrong stripe.
        return spread(hash) & STRIPE_MASK;
    }

    private Object getStripeMonitor(int hash) {
        return stripeMonitors[getStripeIndex(hash)];
    }

    /**
     * Retrieves the value associated with the specified multidimensional key using var-args syntax.
     * <p>This is a convenience method that allows easy multi-key lookups without having to pass
     * arrays or collections. The keys are treated as separate dimensions of a multi-key.</p>
     * 
     * @param keys the key components to look up. Can be null or empty (treated as null key),
     *             single key, or multiple key components
     * @return the value associated with the multi-key, or {@code null} if no mapping exists
     * @see #get(Object)
     */
    public V getMultiKey(Object... keys) {
        if (keys == null || keys.length == 0) return get(null);
        if (keys.length == 1) return get(keys[0]);
        return get(keys);  // Let get()'s normalizeLookup() handle everything!
    }

    /**
     * Optimized 2-key lookup with zero allocation using ThreadLocal array.
     *
     * @param k1 first key component
     * @param k2 second key component
     * @return the value associated with the multi-key, or {@code null} if no mapping exists
     */
    public V getMultiKey(Object k1, Object k2) {
        Object[] key = LOOKUP_KEY_2.get();
        if (key[0] != null || key[1] != null) {
            // Reentrant call detected — allocate to avoid corrupting the outer call's key
            return get(new Object[]{k1, k2});
        }
        key[0] = k1;
        key[1] = k2;
        try {
            return get(key);
        } finally {
            key[0] = null;
            key[1] = null;
        }
    }

    /**
     * Optimized 3-key lookup with zero allocation using ThreadLocal array.
     *
     * @param k1 first key component
     * @param k2 second key component
     * @param k3 third key component
     * @return the value associated with the multi-key, or {@code null} if no mapping exists
     */
    public V getMultiKey(Object k1, Object k2, Object k3) {
        Object[] key = LOOKUP_KEY_3.get();
        if (key[0] != null || key[1] != null || key[2] != null) {
            return get(new Object[]{k1, k2, k3});
        }
        key[0] = k1;
        key[1] = k2;
        key[2] = k3;
        try {
            return get(key);
        } finally {
            key[0] = null;
            key[1] = null;
            key[2] = null;
        }
    }

    /**
     * Optimized 4-key lookup with zero allocation using ThreadLocal array.
     *
     * @param k1 first key component
     * @param k2 second key component
     * @param k3 third key component
     * @param k4 fourth key component
     * @return the value associated with the multi-key, or {@code null} if no mapping exists
     */
    public V getMultiKey(Object k1, Object k2, Object k3, Object k4) {
        Object[] key = LOOKUP_KEY_4.get();
        if (key[0] != null || key[1] != null || key[2] != null || key[3] != null) {
            return get(new Object[]{k1, k2, k3, k4});
        }
        key[0] = k1;
        key[1] = k2;
        key[2] = k3;
        key[3] = k4;
        try {
            return get(key);
        } finally {
            key[0] = null;
            key[1] = null;
            key[2] = null;
            key[3] = null;
        }
    }

    /**
     * Optimized 5-key lookup with zero allocation using ThreadLocal array.
     *
     * @param k1 first key component
     * @param k2 second key component
     * @param k3 third key component
     * @param k4 fourth key component
     * @param k5 fifth key component
     * @return the value associated with the multi-key, or {@code null} if no mapping exists
     */
    public V getMultiKey(Object k1, Object k2, Object k3, Object k4, Object k5) {
        Object[] key = LOOKUP_KEY_5.get();
        if (key[0] != null || key[1] != null || key[2] != null || key[3] != null || key[4] != null) {
            return get(new Object[]{k1, k2, k3, k4, k5});
        }
        key[0] = k1;
        key[1] = k2;
        key[2] = k3;
        key[3] = k4;
        key[4] = k5;
        try {
            return get(key);
        } finally {
            key[0] = null;
            key[1] = null;
            key[2] = null;
            key[3] = null;
            key[4] = null;
        }
    }

    /**
     * Normalizes a key for lookup operations without allocating a MultiKey object.
     * This method uses a ThreadLocal Norm holder to avoid allocations on the hot path.
     * 
     * @param key the key to normalize
     * @return a Norm object containing the normalized key and hash
     */
    private Norm normalizeForLookup(Object key) {
        Norm n = new Norm();  // Simple allocation - JIT escape analysis optimizes this away!
        
        // Fast path: null
        if (key == null) {
            n.key = NULL_SENTINEL;
            n.hash = 0;
            return n;
        }
        
        // Fast path: simple keys (not arrays or collections)
        if (!(key instanceof Collection)) {
            Class<?> keyClass = key.getClass();
            if (!keyClass.isArray()) {
                // Simple object - not a collection or array
                n.key = key;
                n.hash = computeElementHash(key, caseSensitive);
                return n;
            }
            // It's an array - fall through to flattenKey which handles all array types
            // (including rare atomic arrays)
        }
        
        // Complex keys: fall back to flattenKey but extract just the data we need
        MultiKey<?> mk = flattenKey(key);
        n.key = mk.keys;
        n.hash = mk.hash;
        return n;
    }

    /**
     * Finds an entry for the given key using the ultra-fast path for simple keys
     * or the normal path for complex keys. This method is shared by get() and containsKey()
     * to eliminate code duplication while maintaining maximum performance.
     * 
     * @param key the key to find - can be simple (non-collection, non-array) or complex
     * @return the MultiKey entry if found, null otherwise
     */
    private MultiKey<V> findSimpleOrComplexKey(Object key) {
        // Ultra-fast path: Simple single keys (non-collection, non-array)
        // This optimization bypasses normalization entirely for the most common case
        if (key != null && !(key instanceof Collection)) {
            Class<?> keyClass = key.getClass();
            if (!keyClass.isArray()) {
                // Direct bucket access - no normalization needed for simple keys
                int hash = computeElementHash(key, caseSensitive);
                final AtomicReferenceArray<MultiKey<V>[]> table = buckets;
                final int mask = table.length() - 1;
                final int index = spread(hash) & mask;
                final MultiKey<V>[] chain = table.get(index);
                if (chain != null) {
                    // Fast scan for single-key entries only
                    for (MultiKey<V> entry : chain) {
                        if (entry.hash == hash && entry.kind == MultiKey.KIND_SINGLE) {
                            if (elementEquals(entry.keys, key, valueBasedEquality, caseSensitive)) {
                                return entry;
                            }
                        }
                    }
                }
                return null;
            }
            // Fast path: Object[] in simpleKeysMode — compute hash inline, zero allocation
            if (simpleKeysMode && keyClass == Object[].class) {
                Object[] array = (Object[]) key;
                int len = array.length;
                int h = len == 0 ? 0 : 1;
                for (int i = 0; i < len; i++) {
                    h = h * 31 + computeElementHash(array[i], caseSensitive);
                }
                return findEntryWithPrecomputedHash(array, h);
            }
            // It's an array (including rare atomic arrays) - fall through to complex path
        }

        // Complex keys: Use zero-allocation lookup with simple new Norm()
        Norm n = normalizeForLookup(key);
        return findEntryWithPrecomputedHash(n.key, n.hash);
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map
     * contains no mapping for the key.
     * <p>This method supports both single keys and multidimensional keys. Arrays and Collections
     * are automatically expanded into multi-keys based on the map's configuration settings.</p>
     * 
     * @param key the key whose associated value is to be returned. Can be a single object,
     *            array, or Collection that will be normalized according to the map's settings
     * @return the value to which the specified key is mapped, or {@code null} if no mapping exists
     */
    public V get(Object key) {
        MultiKey<V> entry = findSimpleOrComplexKey(key);
        return entry != null ? entry.value : null;
    }

    /**
     * Associates the specified value with the specified multidimensional key using var-args syntax.
     * <p>This is a convenience method that allows easy multi-key storage without having to pass
     * arrays or collections. The keys are treated as separate dimensions of a multi-key.</p>
     * 
     * @param value the value to be associated with the multi-key
     * @param keys the key components for the mapping. Can be null or empty (treated as null key),
     *             single key, or multiple key components
     * @return the previous value associated with the multi-key, or {@code null} if there was
     *         no mapping for the key
     * @see #put(Object, Object)
     */
    public V putMultiKey(V value, Object... keys) {
        if (keys == null || keys.length == 0) return put(null, value);
        if (keys.length == 1) return put(keys[0], value);
        return put(keys, value);  // Let put()'s normalization handle everything!
    }

    /**
     * Associates the specified value with the specified key in this map.
     * <p>This method supports both single keys and multidimensional keys. Arrays and Collections
     * are automatically expanded into multi-keys based on the map's configuration settings.</p>
     * 
     * @param key the key with which the specified value is to be associated. Can be a single object,
     *            array, or Collection that will be normalized according to the map's settings
     * @param value the value to be associated with the specified key
     * @return the previous value associated with the key, or {@code null} if there was
     *         no mapping for the key
     */
    public V put(Object key, V value) {
        MultiKey<V> newKey = createMultiKey(key, value);
        return putInternal(newKey);
    }
    
    /**
     * Creates a MultiKey from a key, normalizing it first.
     * Used by put() and remove() operations that need MultiKey objects.
     * This optimized version avoids the intermediate NormalizedKey allocation.
     * @param key the key to normalize
     * @param value the value (can be null for remove operations)
     * @return a MultiKey object with a normalized key and computed hash
     */
    private MultiKey<V> createMultiKey(Object key, V value) {
        // Direct optimization: create MultiKey without intermediate NormalizedKey
        // This saves one object allocation per put/remove operation
        
        // Handle null case - reuse constant's data
        if (key == null) {
            return new MultiKey<>(NULL_NORMALIZED_KEY.keys, NULL_NORMALIZED_KEY.hash, value);
        }

        // === OPTIMIZATION: Check instanceof Collection first (faster than getClass().isArray()) ===
        // For simple keys (the common case), this avoids the expensive getClass() call when possible.
        if (key instanceof Collection) {
            // It's a Collection - handle based on mode
            if (collectionKeyMode == CollectionKeyMode.COLLECTIONS_NOT_EXPANDED) {
                // Treat Collection as single key - fast return
                return new MultiKey<>(key, computeElementHash(key, caseSensitive), value);
            }
            // Collection needs expansion - fall through to handle below
        } else {
            // Not a Collection - check if it's an array
            Class<?> keyClass = key.getClass();
            boolean isKeyArray = keyClass.isArray();

            if (!isKeyArray) {
                // === FAST PATH: Simple objects (not arrays or collections) ===
                return new MultiKey<>(key, computeElementHash(key, caseSensitive), value, 1, MultiKey.KIND_SINGLE);
            }

            // Fast path: Object[] in simpleKeysMode — single allocation (no temp MultiKey)
            if (simpleKeysMode && keyClass == Object[].class) {
                Object[] array = (Object[]) key;
                int len = array.length;
                int h = len == 0 ? 0 : 1;
                for (int i = 0; i < len; i++) {
                    h = h * 31 + computeElementHash(array[i], caseSensitive);
                }
                return new MultiKey<>(array, h, value, len, MultiKey.KIND_OBJECT_ARRAY);
            }
            // It's an array (including rare atomic arrays) - fall through to flattenKey
        }

        // For complex keys (arrays/collections), use the standard flattenKey path
        final MultiKey<V> normalizedKey = flattenKey(key);
        return new MultiKey<>(normalizedKey.keys, normalizedKey.hash, value);
    }

    // Update maxChainLength to the maximum of current value and newValue
    // Uses CAS loop to avoid method reference allocation overhead
    private void updateMaxChainLength(int newValue) {
        int current;
        while ((current = maxChainLength.get()) < newValue) {
            if (maxChainLength.compareAndSet(current, newValue)) {
                break;
            }
        }
    }
    
    /**
     * Fast check if an object is an array or collection that might contain nested structures.
     * Used by optimized fast paths to determine routing.
     */
    private boolean isArrayOrCollection(Object o) {
        // In simpleKeysMode, immediately return false to avoid all checks
        if (simpleKeysMode) {
            return false;
        }
        if (o == null) {
            return false;
        }
        // Fast path: known simple types are never arrays or collections
        Class<?> c = o.getClass();
        if (c == String.class || c == Integer.class || c == Long.class || c == Double.class) {
            return false;
        }
        // Check for Collection or array
        return o instanceof Collection || c.isArray();
    }
    
    /**
     * CENTRAL NORMALIZATION METHOD - Single source of truth for all key operations.
     * <p>
     * This method is the ONLY place where keys are normalized in the entire MultiKeyMap.
     * ALL operations (get, put, remove, containsKey, compute*, etc.) use this method
     * to ensure consistent key normalization across the entire API.
     * <p>
     * Performance optimizations:
     * - Fast path for simple objects (non-arrays, non-collections)
     * - Specialized handling for 0-5 element arrays/collections (covers 90%+ of use cases)
     * - Type-specific processing for primitive arrays to avoid reflection
     * - Direct computation of hash codes during traversal to avoid redundant passes
     * 
     * @param key the key to normalize (can be null, single object, array, or collection)
     * @return Norm object containing normalized key and precomputed hash
     */
    @SuppressWarnings("unchecked")
    private <T> MultiKey<T> flattenKey(Object key) {

        // Handle null case - use pre-created instance to avoid allocation
        if (key == null) {
            return NULL_NORMALIZED_KEY;
        }

        // === FAST PATH: Known common simple types ===
        // Class identity check (==) is much faster than instanceof hierarchy traversal.
        // These are the most common key types in practice - handle them first.
        Class<?> keyClass = key.getClass();
        if (keyClass == String.class || keyClass == Integer.class ||
            keyClass == Long.class || keyClass == Double.class) {
            return new MultiKey<>(key, computeElementHash(key, caseSensitive), null, 1, MultiKey.KIND_SINGLE);
        }

        // === ATOMIC ARRAY CONVERSION ===
        // Convert atomic arrays to regular arrays for normalization
        // These are transport mechanisms for values, not stored directly
        if (key instanceof AtomicIntegerArray) {
            AtomicIntegerArray atomicArr = (AtomicIntegerArray) key;
            int len = atomicArr.length();
            int[] regularArr = new int[len];
            for (int i = 0; i < len; i++) {
                regularArr[i] = atomicArr.get(i);
            }
            return flattenKey(regularArr);
        }
        
        if (key instanceof AtomicLongArray) {
            AtomicLongArray atomicArr = (AtomicLongArray) key;
            int len = atomicArr.length();
            long[] regularArr = new long[len];
            for (int i = 0; i < len; i++) {
                regularArr[i] = atomicArr.get(i);
            }
            return flattenKey(regularArr);
        }
        
        if (key instanceof AtomicReferenceArray) {
            AtomicReferenceArray<?> atomicArr = (AtomicReferenceArray<?>) key;
            int len = atomicArr.length();
            Object[] regularArr = new Object[len];
            for (int i = 0; i < len; i++) {
                regularArr[i] = atomicArr.get(i);
            }
            return flattenKey(regularArr);
        }

        // === Check for Collection or Array ===
        // keyClass was already computed above for the fast path check
        boolean isKeyArray;

        if (key instanceof Collection) {
            // It's a Collection - handle based on mode
            if (collectionKeyMode == CollectionKeyMode.COLLECTIONS_NOT_EXPANDED) {
                // Treat Collection as single key - fast return
                return new MultiKey<>(key, computeElementHash(key, caseSensitive), null, 1, MultiKey.KIND_SINGLE);
            }
            // Collection needs expansion - set array flags for later processing
            isKeyArray = false;  // Collections are not arrays
        } else {
            // Not a Collection - check if it's an array
            isKeyArray = keyClass.isArray();

            if (!isKeyArray) {
                // Simple objects (not arrays or collections) - return immediately
                // This handles types not covered by the fast path above (Float, Short, etc.)
                return new MultiKey<>(key, computeElementHash(key, caseSensitive), null, 1, MultiKey.KIND_SINGLE);
            }
            // Continue with array processing below
        }

        // At this point, key is either:
        // 1. An array (isKeyArray == true from the else branch above)
        // 2. A Collection that needs expansion (isKeyArray == false from the if branch above)
        
        // === FAST PATH: Object[] arrays with length-based optimization ===
        if (keyClass == Object[].class) {
            Object[] array = (Object[]) key;
            
            // In simpleKeysMode, route ALL sizes through optimized methods
            if (simpleKeysMode) {
                switch (array.length) {
                    case 0:
                        return new MultiKey<>(array, 0, null, 0, MultiKey.KIND_OBJECT_ARRAY);
                    case 1:
                        return flattenObjectArray1(array);  // Unrolled for maximum speed
                    case 2:
                        return flattenObjectArray2(array);  // Unrolled for performance
                    case 3:
                        return flattenObjectArray3(array);  // Unrolled for performance
                    default:
                        // For larger arrays in simpleKeysMode, use parameterized version
                        return flattenObjectArrayN(array, array.length);
                }
            } else {
                // Normal mode: use size-based routing
                switch (array.length) {
                    case 0:
                        return new MultiKey<>(array, 0, null, 0, MultiKey.KIND_OBJECT_ARRAY);
                    case 1:
                        return flattenObjectArray1(array);  // Unrolled for maximum speed
                    case 2:
                        return flattenObjectArray2(array);  // Unrolled for performance  
                    case 3:
                        return flattenObjectArray3(array);  // Unrolled for performance
                    case 4:
                    case 5:
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                        return flattenObjectArrayN(array, array.length);  // Use parameterized version
                    default:
                        return process1DObjectArray(array);
                }
            }
        }
        
        // === FAST PATH: Primitive arrays - handle each type separately to keep them unboxed ===
        if (isKeyArray && keyClass.getComponentType().isPrimitive()) {
            // Handle empty arrays once for all primitive types
            int length = ArrayUtilities.getLength(key);
            if (length == 0) {
                return new MultiKey<>(key, 0, null);
            }
            
            // Each primitive type handled separately with inline loops for maximum performance
            // These return the primitive array directly as the key (no boxing)
            int h = 1;
            
            if (keyClass == int[].class) {
                int[] array = (int[]) key;
                for (int i = 0; i < length; i++) {
                    h = h * 31 + hashLong(array[i]);
                }
                return new MultiKey<>(array, h, null);
            }
            
            if (keyClass == long[].class) {
                long[] array = (long[]) key;
                for (int i = 0; i < length; i++) {
                    h = h * 31 + hashLong(array[i]);
                }
                return new MultiKey<>(array, h, null);
            }
            
            if (keyClass == double[].class) {
                double[] array = (double[]) key;
                for (int i = 0; i < length; i++) {
                    // Use value-based hash for doubles
                    double d = array[i];
                    if (d == 0.0d) d = 0.0d; // canonicalize -0.0
                    if (Double.isFinite(d) && d == Math.rint(d) && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
                        h = h * 31 + hashLong((long) d);
                    } else {
                        h = h * 31 + hashDouble(d);
                    }
                }
                return new MultiKey<>(array, h, null);
            }
            
            if (keyClass == float[].class) {
                float[] array = (float[]) key;
                for (int i = 0; i < length; i++) {
                    // Convert float to double and use value-based hash
                    double d = array[i];
                    if (d == 0.0d) d = 0.0d; // canonicalize -0.0
                    if (Double.isFinite(d) && d == Math.rint(d) && d >= Long.MIN_VALUE && d <= Long.MAX_VALUE) {
                        h = h * 31 + hashLong((long) d);
                    } else {
                        h = h * 31 + hashDouble(d);
                    }
                }
                return new MultiKey<>(array, h, null);
            }
            
            if (keyClass == boolean[].class) {
                boolean[] array = (boolean[]) key;
                for (int i = 0; i < length; i++) {
                    h = h * 31 + Boolean.hashCode(array[i]);
                }
                return new MultiKey<>(array, h, null);
            }
            
            if (keyClass == byte[].class) {
                byte[] array = (byte[]) key;
                for (int i = 0; i < length; i++) {
                    h = h * 31 + hashLong(array[i]);
                }
                return new MultiKey<>(array, h, null);
            }
            
            if (keyClass == short[].class) {
                short[] array = (short[]) key;
                for (int i = 0; i < length; i++) {
                    h = h * 31 + hashLong(array[i]);
                }
                return new MultiKey<>(array, h, null);
            }
            
            if (keyClass == char[].class) {
                char[] array = (char[]) key;
                for (int i = 0; i < length; i++) {
                    h = h * 31 + Character.hashCode(array[i]);
                }
                return new MultiKey<>(array, h, null);
            }
            
            // This shouldn't happen, but handle it with the generic approach as fallback
            throw new IllegalStateException("Unknown primitive key type: " + keyClass.getName());
        }

        // === Other array types (String[], etc.) ===
        if (isKeyArray) {
            return process1DTypedArray(key);
        }
        
        // === FAST PATH: Collections with size-based optimization ===
        Collection<?> coll = (Collection<?>) key;
        
        // Collections that reach this point need expansion (COLLECTIONS_NOT_EXPANDED handled earlier)

        // Sets always need expansion to get SET_OPEN/SET_CLOSE markers and order-agnostic hash
        if (coll instanceof Set) {
            return expandWithHash(coll);
        }

        // If flattening dimensions, always go through expansion
        if (flattenDimensions) {
            return expandWithHash(coll);
        }
        
        // Size-based optimization for collections
        int size = coll.size();
        
        // In simpleKeysMode, route ALL sizes through optimized methods
        if (simpleKeysMode) {
            switch (size) {
                case 0:
                    return new MultiKey<>(ArrayUtilities.EMPTY_OBJECT_ARRAY, 0, null);
                case 1:
                    return flattenCollection1(coll);  // Unrolled for maximum speed
                case 2:
                    return flattenCollection2(coll);  // Unrolled for performance
                case 3:
                    return flattenCollection3(coll);  // Unrolled for performance
                default:
                    // For larger collections in simpleKeysMode, use parameterized version
                    return flattenCollectionN(coll, size);
            }
        } else {
            // Normal mode: use size-based routing
            switch (size) {
                case 0:
                    return new MultiKey<>(ArrayUtilities.EMPTY_OBJECT_ARRAY, 0, null);
                case 1:
                    return flattenCollection1(coll);  // Unrolled for maximum speed
                case 2:
                    return flattenCollection2(coll);  // Unrolled for performance
                case 3:
                    return flattenCollection3(coll);  // Unrolled for performance
                case 4:
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                    return flattenCollectionN(coll, size);  // Use parameterized version
                default:
                    return process1DCollection(coll);
            }
        }
    }
    
    // === Fast path helper methods for flattenKey() ===
    
    private <T> MultiKey<T> flattenObjectArray1(Object[] array) {
        Object elem = array[0];

        // Simple element - fast path
        if (!isArrayOrCollection(elem)) {
            int hash = 31 + computeElementHash(elem, caseSensitive);
            return new MultiKey<>(array, hash, null, 1, MultiKey.KIND_OBJECT_ARRAY);
        }
        
        // Complex element - check flattenDimensions
        if (flattenDimensions) {
            return expandWithHash(array);
        }
        
        // Not flattening - delegate to process1DObjectArray
        return process1DObjectArray(array);
    }
    
    private <T> MultiKey<T> flattenObjectArray2(Object[] array) {
        // Optimized unrolled version for size 2
        Object elem0 = array[0];
        Object elem1 = array[1];
        
        if (isArrayOrCollection(elem0) || isArrayOrCollection(elem1)) {
            if (flattenDimensions) return expandWithHash(array);
            return process1DObjectArray(array);
        }
        
        int h = 31 + computeElementHash(elem0, caseSensitive);
        h = h * 31 + computeElementHash(elem1, caseSensitive);
        return new MultiKey<>(array, h, null, 2, MultiKey.KIND_OBJECT_ARRAY);
    }

    private <T> MultiKey<T> flattenObjectArray3(Object[] array) {
        // Optimized unrolled version for size 3
        Object elem0 = array[0];
        Object elem1 = array[1];
        Object elem2 = array[2];
        
        if (isArrayOrCollection(elem0) || isArrayOrCollection(elem1) || isArrayOrCollection(elem2)) {
            if (flattenDimensions) return expandWithHash(array);
            return process1DObjectArray(array);
        }
        
        int h = 31 + computeElementHash(elem0, caseSensitive);
        h = h * 31 + computeElementHash(elem1, caseSensitive);
        h = h * 31 + computeElementHash(elem2, caseSensitive);
        return new MultiKey<>(array, h, null, 3, MultiKey.KIND_OBJECT_ARRAY);
    }

    /**
     * Parameterized version of Object[] flattening for sizes 4-10.
     * Uses loops instead of unrolling to handle any size efficiently.
     */
    private <T> MultiKey<T> flattenObjectArrayN(Object[] array, int size) {
        // Single pass: check complexity AND compute hash
        int h = 1;

        if (simpleKeysMode) {
            for (int i = 0; i < size; i++) {
                h = h * 31 + computeElementHash(array[i], caseSensitive);
            }
        } else {
            for (int i = 0; i < size; i++) {
                Object elem = array[i];
                // Fast path: skip array/collection check for known simple types
                // Class identity check (==) is faster than instanceof + isArray()
                if (elem != null) {
                    Class<?> ec = elem.getClass();
                    if (ec != String.class && ec != Integer.class &&
                        ec != Long.class && ec != Double.class) {
                        // Not a known simple type - check if it's an array or collection
                        if (elem instanceof Collection || ec.isArray()) {
                            // Found complex element - bail out
                            if (flattenDimensions) return expandWithHash(array);
                            return process1DObjectArray(array);
                        }
                    }
                }
                h = h * 31 + computeElementHash(elem, caseSensitive);
            }
        }

        // All simple - return with computed hash
        return new MultiKey<>(array, h, null, size, MultiKey.KIND_OBJECT_ARRAY);
    }

    private <T> MultiKey<T> flattenCollection1(Collection<?> coll) {
        // Sets always need expansion to get SET_OPEN/SET_CLOSE markers and order-agnostic hash
        if (coll instanceof Set) {
            return expandWithHash(coll);
        }

        Iterator<?> iter = coll.iterator();
        Object elem = iter.next();
        
        // Simple element - fast path
        if (!isArrayOrCollection(elem)) {
            int hash = 31 + computeElementHash(elem, caseSensitive);
            // Always store Collection as-is
            return new MultiKey<>(coll, hash, null);
        }
        
        // Complex element - check flattenDimensions
        if (flattenDimensions) {
            return expandWithHash(coll);
        }
        
        // Not flattening - delegate to process1DCollection
        return process1DCollection(coll);
    }
    
    private <T> MultiKey<T> flattenCollection2(Collection<?> coll) {
        // Sets always need expansion to get SET_OPEN/SET_CLOSE markers and order-agnostic hash
        if (coll instanceof Set) {
            return expandWithHash(coll);
        }

        // Simplified: always store Collections as-is
        Iterator<?> iter = coll.iterator();
        Object elem0 = iter.next();
        Object elem1 = iter.next();
        
        if (isArrayOrCollection(elem0) || isArrayOrCollection(elem1)) {
            if (flattenDimensions) return expandWithHash(coll);
            return process1DCollection(coll);
        }
        
        int h = 31 + computeElementHash(elem0, caseSensitive);
        h = h * 31 + computeElementHash(elem1, caseSensitive);
        // Always store Collection as-is
        return new MultiKey<>(coll, h, null);
    }
    
    private <T> MultiKey<T> flattenCollection3(Collection<?> coll) {
        // Sets always need expansion to get SET_OPEN/SET_CLOSE markers and order-agnostic hash
        if (coll instanceof Set) {
            return expandWithHash(coll);
        }

        // Simplified: always store Collections as-is
        Iterator<?> iter = coll.iterator();
        Object elem0 = iter.next();
        Object elem1 = iter.next();
        Object elem2 = iter.next();
        
        if (isArrayOrCollection(elem0) || isArrayOrCollection(elem1) || isArrayOrCollection(elem2)) {
            if (flattenDimensions) return expandWithHash(coll);
            return process1DCollection(coll);
        }
        
        int h = 31 + computeElementHash(elem0, caseSensitive);
        h = h * 31 + computeElementHash(elem1, caseSensitive);
        h = h * 31 + computeElementHash(elem2, caseSensitive);
        // Always store Collection as-is
        return new MultiKey<>(coll, h, null);
    }

    /**
     * Parameterized version of collection flattening for sizes 6-10.
     * Simplified to always store Collections as-is.
     */
    private <T> MultiKey<T> flattenCollectionN(Collection<?> coll, int size) {
        // Sets always need expansion to get SET_OPEN/SET_CLOSE markers and order-agnostic hash
        if (coll instanceof Set) {
            return expandWithHash(coll);
        }

        // Simplified: always use iterator and store Collection as-is
        Iterator<?> iter = coll.iterator();
        int h = 1;

        // Check for complex elements and compute hash
        if (simpleKeysMode) {
            // In simple keys mode, just compute hash
            for (int i = 0; i < size; i++) {
                h = h * 31 + computeElementHash(iter.next(), caseSensitive);
            }
        } else {
            // Check for nested structures
            final boolean flattenDimLocal = flattenDimensions;
            
            for (int i = 0; i < size; i++) {
                Object elem = iter.next();
                boolean isArrayOrCollection = elem instanceof Collection || (elem != null && elem.getClass().isArray());
                if (isArrayOrCollection) {
                    // Found complex element - bail out
                    if (flattenDimLocal) return expandWithHash(coll);
                    return process1DCollection(coll);
                }
                h = h * 31 + computeElementHash(elem, caseSensitive);
            }
        }

        // All simple - store Collection as-is with computed hash
        return new MultiKey<>(coll, h, null);
    }

    private <T> MultiKey<T> process1DObjectArray(final Object[] array) {
        final int len = array.length;

        if (len == 0) {
            return new MultiKey<>(array, 0, null);
        }
        
        // Check if truly 1D while computing full hash
        int h = 1;
        boolean is1D = true;
        
        // Check all elements and compute full hash
        for (int i = 0; i < len; i++) {
            final Object e = array[i];
            if (e == null) {
                // h = h * 31 + 0; // This is just h * 31, optimize it
                h *= 31;
            } else {
                final Class<?> eClass = e.getClass();
                // Check dimension first (before expensive hash computation if we're going to break)
                if (eClass.isArray() || e instanceof Collection) {
                    // Not 1D - delegate to expandWithHash which will handle everything
                    is1D = false;
                    break;
                }
                // Most common path - regular object, inline the common cases
                // Always use computeElementHash to maintain value-mode hash alignment
                h = h * 31 + computeElementHash(e, caseSensitive);
            }
        }
        
        if (is1D) {
            // No collapse - arrays stay as arrays
            return new MultiKey<>(array, h, null);
        }
        
        // It's 2D+ - need to expand with hash computation
        return expandWithHash(array);
    }
    
    private <T> MultiKey<T> process1DCollection(final Collection<?> coll) {
        // Sets always need expansion to get SET_OPEN/SET_CLOSE markers and order-agnostic hash
        if (coll instanceof Set) {
            return expandWithHash(coll);
        }

        if (coll.isEmpty()) {
            // Normalize empty collections to empty array for cross-container equivalence
            return new MultiKey<>(ArrayUtilities.EMPTY_OBJECT_ARRAY, 0, null);
        }
        
        // Check if truly 1D while computing hash
        int h = 1;
        boolean is1D = true;
        
        // Simplified: always use iterator (no RandomAccess distinction)
        Iterator<?> iter = coll.iterator();
        while (iter.hasNext()) {
            Object e = iter.next();
            // Compute hash for all elements
            h = h * 31 + computeElementHash(e, caseSensitive);
            if (e instanceof Collection || (e != null && e.getClass().isArray())) {
                is1D = false;
                break;
            }
        }
        
        if (is1D) {
            // Store all collections as-is
            // This eliminates conversion overhead and simplifies the code
            return new MultiKey<>(coll, h, null);
        }
        
        // It's 2D+ - need to expand with hash computation
        return expandWithHash(coll);
    }
    
    private <T> MultiKey<T> process1DTypedArray(Object arr) {
        Class<?> clazz = arr.getClass();
        
        // Primitive arrays are already handled in flattenKey() and never reach here
        // Handle JDK DTO array types for optimization (elements guaranteed to be simple)
        
        // Handle simple array types efficiently (these can't contain nested arrays/collections)
        if (SIMPLE_ARRAY_TYPES.contains(clazz)) {
            
            Object[] objArray = (Object[]) arr;
            final int len = objArray.length;
            if (len == 0) {
                return new MultiKey<>(objArray, 0, null);
            }

            // JDK DTO array types are always 1D (their elements can't be arrays or collections)
            // Optimized: Direct array access without nested structure checks
            int h = 1;
            for (int i = 0; i < len; i++) {
                final Object o = objArray[i];
                h = h * 31 + computeElementHash(o, caseSensitive);
            }

            // No collapse - arrays stay as arrays
            return new MultiKey<>(objArray, h, null);
        }
        
        // Fallback to reflection for other array types
        return process1DGenericArray(arr);
    }

    private <T> MultiKey<T> process1DGenericArray(Object arr) {
        // Fallback method using reflection for uncommon array types
        final int len = ArrayUtilities.getLength(arr);
        if (len == 0) {
            return new MultiKey<>(arr, 0, null);
        }
        
        // Check if truly 1D while computing full hash (same as process1DObjectArray)
        int h = 1;
        boolean is1D = true;
        
        // Compute full hash for all elements
        for (int i = 0; i < len; i++) {
            Object e = ArrayUtilities.getElement(arr, i);
            h = h * 31 + computeElementHash(e, caseSensitive);
            if (e instanceof Collection || (e != null && e.getClass().isArray())) {
                is1D = false;
                break;
            }
        }
        
        if (is1D) {
            // No collapse - arrays stay as arrays
            return new MultiKey<>(arr, h, null);
        }
        
        // It's 2D+ - need to expand with hash computation
        return expandWithHash(arr);
    }
    
    private <T> MultiKey<T> expandWithHash(Object key) {
        // Pre-size the expanded list based on heuristic:
        // - Arrays/Collections typically expand to their size + potential nesting markers
        // - Default to 8 for unknown types (better than ArrayList's default 10 for small keys)
        int estimatedSize = 8;
        if (key != null) {
            if (key.getClass().isArray()) {
                int len = ArrayUtilities.getLength(key);
                // For arrays: size + potential OPEN/CLOSE markers + buffer for nested expansion
                estimatedSize = flattenDimensions ? len : len + 2;
                // Add some buffer for potential nested structures
                estimatedSize = Math.min(estimatedSize + (estimatedSize / 2), 64); // Cap at reasonable size
            } else if (key instanceof Collection) {
                int size = ((Collection<?>) key).size();
                // For collections: similar to arrays
                estimatedSize = flattenDimensions ? size : size + 2;
                estimatedSize = Math.min(estimatedSize + (estimatedSize / 2), 64);
            }
        }
        
        List<Object> expanded = new ArrayList<>(estimatedSize);
        IdentitySet<Object> visited = new IdentitySet<>();

        int hash = expandAndHash(key, expanded, visited, 1, flattenDimensions, caseSensitive);
        
        // NO COLLAPSE - expanded results stay as lists
        // Even single-element expanded results remain as lists to maintain consistency
        // [x] should never become x
        
        return new MultiKey<>(expanded, hash, null);
    }
    
    private static int expandAndHash(Object current, List<Object> result, IdentitySet<Object> visited,
                                      int runningHash, boolean useFlatten, boolean caseSensitive) {
        if (current == null) {
            result.add(NULL_SENTINEL);
            return runningHash * 31 + NULL_SENTINEL.hashCode();
        }

        // Fast path: Common leaf types (final classes - safe to use == for class comparison)
        // These are the most frequent key types, so check them first to skip the instanceof chain
        Class<?> clazz = current.getClass();
        if (clazz == String.class || clazz == Integer.class || clazz == Long.class ||
            clazz == Double.class || clazz == Boolean.class || clazz == Short.class ||
            clazz == Byte.class || clazz == Character.class || clazz == Float.class) {
            result.add(current);
            return runningHash * 31 + computeElementHash(current, caseSensitive);
        }

        if (visited.contains(current)) {
            Object cycle = EMOJI_CYCLE + System.identityHashCode(current);
            result.add(cycle);
            return runningHash * 31 + cycle.hashCode();
        }

        // Use getComponentType() != null instead of isArray() - slightly faster
        if (clazz.getComponentType() != null) {
            visited.add(current);
            try {
                if (!useFlatten) {
                    result.add(OPEN);
                    runningHash = runningHash * 31 + OPEN.hashCode();
                }
                int len = ArrayUtilities.getLength(current);
                for (int i = 0; i < len; i++) {
                    runningHash = expandAndHash(ArrayUtilities.getElement(current, i), result, visited, runningHash, useFlatten, caseSensitive);
                }
                if (!useFlatten) {
                    result.add(CLOSE);
                    runningHash = runningHash * 31 + CLOSE.hashCode();
                }
            } finally {
                visited.remove(current);
            }
        } else if (current instanceof Collection) {
            Collection<?> coll = (Collection<?>) current;
            boolean isSet = current instanceof Set;
            visited.add(current);
            try {
                if (isSet) {
                    // Sets always use SET_OPEN/SET_CLOSE markers (even in flatten mode)
                    // to preserve order-agnostic semantics
                    result.add(SET_OPEN);
                    runningHash = runningHash * 31 + SET_OPEN.hashCode();

                    // Order-agnostic hash for Set elements (XOR with rotation for better distribution)
                    // Optimization: Add elements directly to result instead of allocating tempResult per element.
                    // By passing 1 as runningHash, expandAndHash returns just that element's hash.
                    int setHash = 0;
                    for (Object e : coll) {
                        int elemHash = expandAndHash(e, result, visited, 1, useFlatten, caseSensitive);
                        setHash ^= Integer.rotateLeft(elemHash, 1);
                    }
                    runningHash = runningHash * 31 + setHash;

                    result.add(SET_CLOSE);
                    runningHash = runningHash * 31 + SET_CLOSE.hashCode();
                } else {
                    // Non-Set Collections use order-dependent polynomial hash
                    if (!useFlatten) {
                        result.add(OPEN);
                        runningHash = runningHash * 31 + OPEN.hashCode();
                    }
                    for (Object e : coll) {
                        runningHash = expandAndHash(e, result, visited, runningHash, useFlatten, caseSensitive);
                    }
                    if (!useFlatten) {
                        result.add(CLOSE);
                        runningHash = runningHash * 31 + CLOSE.hashCode();
                    }
                }
            } finally {
                visited.remove(current);
            }
        } else {
            result.add(current);
            runningHash = runningHash * 31 + computeElementHash(current, caseSensitive);
        }
        return runningHash;
    }
    
    /**
     * Optimized findEntry that skips the flattenKey() call when we already have
     * the normalized key and precomputed hash. This is the core of informed handoff optimization.
     */
    private MultiKey<V> findEntryWithPrecomputedHash(final Object normalizedKey, final int hash) {
        final AtomicReferenceArray<MultiKey<V>[]> table = buckets;  // Volatile read of buckets
        final int mask = table.length() - 1;  // Array length is immutable
        final int index = spread(hash) & mask;

        final MultiKey<V>[] chain = table.get(index);
        if (chain == null) {
            return null;
        }

        final int chLen = chain.length;
        for (int i = 0; i < chLen; i++) {
            MultiKey<V> entry = chain[i];
            if (entry.hash == hash && keysMatch(entry, normalizedKey)) return entry;
        }
        return null;
    }

    /**
     * Optimized keysMatch that leverages MultiKey's precomputed size and kind.
     * This is used when we have access to the stored MultiKey object.
     */
    private boolean keysMatch(MultiKey<V> stored, Object lookup) {
        // Fast identity check
        if (stored.keys == lookup) return true;
        if (stored.keys == null || lookup == null) return false;

        // Multi-key case - use precomputed kind for fast switching
        final Class<?> lookupClass = lookup.getClass();

        // Early size rejection - if stored has precomputed size, check it first
        if (stored.kind == MultiKey.KIND_SINGLE) {
            // Single key optimization
            // In COLLECTIONS_NOT_EXPANDED mode, a Collection/array can be a single key
            if (lookupClass.isArray() || lookup instanceof Collection) {
                // If stored key is also a Collection/array, compare using elementEquals
                // This handles COLLECTIONS_NOT_EXPANDED mode where collections are single keys
                if (stored.keys instanceof Collection || stored.keys.getClass().isArray()) {
                    // Both stored and lookup are Collection/array - compare using elementEquals
                    // This handles cases where collection types differ (e.g., List vs Set)
                    // but content is the same - can happen after deserialization
                    return elementEquals(stored.keys, lookup, valueBasedEquality, caseSensitive);
                }
                // Stored is not Collection/array but lookup is - no match
                return false;
            }
            // Use elementEquals to respect value-based equality for single keys
            return elementEquals(stored.keys, lookup, valueBasedEquality, caseSensitive);
        }

        // Check size match first (early rejection)
        final int lookupSize;
        final byte lookupKind;
        
        if (lookupClass.isArray()) {
            lookupSize = ArrayUtilities.getLength(lookup);
            Class<?> componentType = lookupClass.getComponentType();
            lookupKind = (componentType != null && componentType.isPrimitive()) 
                ? MultiKey.KIND_PRIMITIVE_ARRAY 
                : MultiKey.KIND_OBJECT_ARRAY;
        } else if (lookup instanceof Collection) {
            lookupSize = ((Collection<?>) lookup).size();
            lookupKind = MultiKey.KIND_COLLECTION;
        } else {
            // Lookup is single but stored is multi
            return false;
        }
        
        // Early rejection on size mismatch
        // IMPORTANT: Skip size check when stored is expanded Collection and lookup is un-expanded Collection
        // In COLLECTIONS_EXPANDED mode, stored.size is the EXPANDED size (includes SET_OPEN/SET_CLOSE markers)
        // but lookupSize is the UN-EXPANDED Collection size. The compareCollections method handles this correctly.
        boolean skipSizeCheck = (collectionKeyMode == CollectionKeyMode.COLLECTIONS_EXPANDED &&
                                  stored.kind == MultiKey.KIND_COLLECTION &&
                                  lookupKind == MultiKey.KIND_COLLECTION);
        if (!skipSizeCheck && stored.size != lookupSize) return false;
        
        // Handle COLLECTIONS_NOT_EXPANDED mode - Collections should use their own equals
        if (collectionKeyMode == CollectionKeyMode.COLLECTIONS_NOT_EXPANDED && stored.kind == MultiKey.KIND_COLLECTION) {
            if (!(lookup instanceof Collection)) return false;
            // Always use the collection's own equals; do NOT require same concrete class
            return stored.keys.equals(lookup);
        }
        
        final Class<?> storeKeysClass = stored.keys.getClass();
        
        // Delegate all container comparisons to unified method
        return compareContainers(stored.keys, lookup, stored.size, stored.kind, lookupKind, storeKeysClass, lookupClass, valueBasedEquality, caseSensitive);
    }
    
    /**
     * Unified container comparison handling all type combinations.
     * Optimized fast paths for same-type comparisons, cross-type handling for others.
     */
    private boolean compareContainers(Object stored, Object lookup, int size, byte storedKind, byte lookupKind,
                                      Class<?> storedClass, Class<?> lookupClass, boolean valueBasedEquality, boolean caseSensitive) {
        // Fast path: same container types
        if (storedKind == lookupKind) {
            switch (storedKind) {
                case MultiKey.KIND_OBJECT_ARRAY:
                    return compareObjectArrays((Object[]) stored, (Object[]) lookup, size);
                    
                case MultiKey.KIND_COLLECTION:
                    return compareCollections((Collection<?>) stored, (Collection<?>) lookup, size, valueBasedEquality, caseSensitive);
                    
                case MultiKey.KIND_PRIMITIVE_ARRAY:
                    // Same primitive array type - use optimized comparison
                    if (storedClass == lookupClass) {
                        return compareSamePrimitiveArrays(stored, lookup, storedClass, valueBasedEquality);
                    }
                    // Different primitive array types - fall through to cross-type
                    break;
            }
        }
        
        // Cross-type comparisons
        // Direct dispatch with argument swapping to eliminate symmetric methods
        
        // Object[] vs Collection (or vice versa)
        if (storedKind == MultiKey.KIND_OBJECT_ARRAY && lookupKind == MultiKey.KIND_COLLECTION) {
            return compareObjectArrayToCollection((Object[]) stored, (Collection<?>) lookup, size, valueBasedEquality, caseSensitive);
        }
        if (storedKind == MultiKey.KIND_COLLECTION && lookupKind == MultiKey.KIND_OBJECT_ARRAY) {
            // Just swap arguments
            return compareObjectArrayToCollection((Object[]) lookup, (Collection<?>) stored, size, valueBasedEquality, caseSensitive);
        }
        
        // Primitive array vs Collection (or vice versa)
        if (storedKind == MultiKey.KIND_PRIMITIVE_ARRAY && lookupKind == MultiKey.KIND_COLLECTION) {
            return comparePrimitiveArrayToCollection(stored, (Collection<?>) lookup, size, valueBasedEquality, caseSensitive);
        }
        if (storedKind == MultiKey.KIND_COLLECTION && lookupKind == MultiKey.KIND_PRIMITIVE_ARRAY) {
            // Just swap arguments
            return comparePrimitiveArrayToCollection(lookup, (Collection<?>) stored, size, valueBasedEquality, caseSensitive);
        }
        
        // Primitive array vs Object array (or vice versa)
        if (storedKind == MultiKey.KIND_PRIMITIVE_ARRAY && lookupKind == MultiKey.KIND_OBJECT_ARRAY) {
            return comparePrimitiveArrayToObjectArray(stored, (Object[]) lookup, size, valueBasedEquality, caseSensitive);
        }
        if (storedKind == MultiKey.KIND_OBJECT_ARRAY && lookupKind == MultiKey.KIND_PRIMITIVE_ARRAY) {
            // Just swap arguments
            return comparePrimitiveArrayToObjectArray(lookup, (Object[]) stored, size, valueBasedEquality, caseSensitive);
        }
        
        // Fallback for any other cases (e.g., different primitive array types)
        // This is the slow path with iterator creation
        final Iterator<?> storedIter = (storedKind == MultiKey.KIND_COLLECTION)
            ? ((Collection<?>) stored).iterator()
            : new ArrayIterator(stored);
        final Iterator<?> lookupIter = (lookupKind == MultiKey.KIND_COLLECTION)
            ? ((Collection<?>) lookup).iterator()
            : new ArrayIterator(lookup);
            
        for (int i = 0; i < size; i++) {
            if (!elementEquals(storedIter.next(), lookupIter.next(), valueBasedEquality, caseSensitive)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Compare two primitive arrays of the same type.
     * Handles special cases for float/double arrays with NaN in valueBasedEquality mode.
     */
    private boolean compareSamePrimitiveArrays(Object array1, Object array2, Class<?> arrayClass, boolean valueBasedEquality) {
        // Special handling for double[] with NaN equality in valueBasedEquality mode
        if (arrayClass == double[].class) {
            double[] a = (double[]) array1;
            double[] b = (double[]) array2;
            if (valueBasedEquality) {
                // Value-based mode: NaN == NaN
                for (int i = 0; i < a.length; i++) {
                    double x = a[i], y = b[i];
                    // Fast path: if equal (including -0.0 == +0.0), continue
                    if (x == y) continue;
                    // Special case: both NaN should be equal
                    if (Double.isNaN(x) && Double.isNaN(y)) continue;
                    return false;
                }
                return true;
            } else {
                // Type-strict mode: use standard Arrays.equals (NaN != NaN)
                return Arrays.equals(a, b);
            }
        }
        
        // Special handling for float[] with NaN equality in valueBasedEquality mode
        if (arrayClass == float[].class) {
            float[] a = (float[]) array1;
            float[] b = (float[]) array2;
            if (valueBasedEquality) {
                // Value-based mode: NaN == NaN
                for (int i = 0; i < a.length; i++) {
                    float x = a[i], y = b[i];
                    // Fast path: if equal (including -0.0f == +0.0f), continue
                    if (x == y) continue;
                    // Special case: both NaN should be equal
                    if (Float.isNaN(x) && Float.isNaN(y)) continue;
                    return false;
                }
                return true;
            } else {
                // Type-strict mode: use standard Arrays.equals (NaN != NaN)
                return Arrays.equals(a, b);
            }
        }
        
        // Other primitive types: Arrays.equals is fine (no NaN issues)
        if (arrayClass == int[].class) return Arrays.equals((int[]) array1, (int[]) array2);
        if (arrayClass == long[].class) return Arrays.equals((long[]) array1, (long[]) array2);
        if (arrayClass == boolean[].class) return Arrays.equals((boolean[]) array1, (boolean[]) array2);
        if (arrayClass == byte[].class) return Arrays.equals((byte[]) array1, (byte[]) array2);
        if (arrayClass == char[].class) return Arrays.equals((char[]) array1, (char[]) array2);
        if (arrayClass == short[].class) return Arrays.equals((short[]) array1, (short[]) array2);
        
        return false;
    }
    
    private static class ArrayIterator implements Iterator<Object> {
        private final Object array;
        private final int len;
        private int index = 0;

        ArrayIterator(Object array) {
            this.array = array;
            this.len = ArrayUtilities.getLength(array);
        }

        @Override
        public boolean hasNext() {
            return index < len;
        }

        @Override
        public Object next() {
            return ArrayUtilities.getElement(array, index++);
        }
    }
    
    // ======================== Optimized Comparison Methods ========================
    // These methods provide zero-allocation paths for common cross-container comparisons
    
    /**
     * Compare two Object[] arrays using configured equality semantics.
     */
    private boolean compareObjectArrays(Object[] array1, Object[] array2, int size) {
        int i = 0;
        while (i < size) {
            Object elem1 = array1[i];
            Object elem2 = array2[i];

            // Check if we're entering a Set portion
            if (elem1 == SET_OPEN && elem2 == SET_OPEN) {
                // Count set elements first (to check size and allocation strategy)
                i++; // Move past SET_OPEN
                int startIdx = i;
                int setSize = 0;

                // Count elements in both sets and verify size match
                while (i < size) {
                    Object next1 = array1[i];
                    Object next2 = array2[i];

                    if (next1 == SET_CLOSE && next2 == SET_CLOSE) {
                        break; // Both sets ended
                    } else if (next1 == SET_CLOSE || next2 == SET_CLOSE) {
                        return false; // One set ended before the other - size mismatch
                    }
                    setSize++;
                    i++;
                }

                // Compare using size-appropriate strategy
                if (setSize == 0) {
                    // Empty sets - equal, nothing to compare
                    // No action needed - continue to next element
                } else if (setSize <= 6) {
                    // Optimization: For small Sets (≤6 elements), nested loop is faster than HashMap
                    // O(n²) comparison (max 36 operations) but avoids HashMap overhead
                    // Track consumed matches to prevent double-matching under valueBasedEquality
                    boolean[] consumed = new boolean[setSize];
                    for (int s1 = startIdx; s1 < startIdx + setSize; s1++) {
                        boolean found = false;
                        for (int s2 = startIdx; s2 < startIdx + setSize; s2++) {
                            if (!consumed[s2 - startIdx] && elementEquals(array1[s1], array2[s2], valueBasedEquality, caseSensitive)) {
                                consumed[s2 - startIdx] = true;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            return false;
                        }
                    }
                } else {
                    // For larger Sets (>6 elements), use hash-bucketing approach inspired by DeepEquals
                    // This is much more efficient than creating two ArrayLists + two HashSets
                    // We only allocate ONE Map<Integer, List<Object>> for array2's elements

                    // Build hash buckets for array2 elements
                    // Pre-size to avoid rehashing: capacity = size * 4/3 to account for 0.75 load factor
                    Map<Integer, List<Object>> hashBuckets = new java.util.HashMap<>(Math.max(16, setSize * 4 / 3));
                    for (int idx = startIdx; idx < startIdx + setSize; idx++) {
                        Object elem = array2[idx];
                        int hash = computeElementHash(elem, caseSensitive);
                        List<Object> bucket = hashBuckets.computeIfAbsent(hash, k -> new ArrayList<>(2));
                        bucket.add(elem);
                    }

                    // Match each array1 element against bucketed array2 elements
                    for (int idx = startIdx; idx < startIdx + setSize; idx++) {
                        Object setElem1 = array1[idx];
                        int hash1 = computeElementHash(setElem1, caseSensitive);
                        List<Object> candidates = hashBuckets.get(hash1);

                        boolean matched = false;
                        if (candidates != null && !candidates.isEmpty()) {
                            // Try to find a match in the same hash bucket
                            for (Iterator<Object> it = candidates.iterator(); it.hasNext();) {
                                Object candidate = it.next();
                                if (elementEquals(setElem1, candidate, valueBasedEquality, caseSensitive)) {
                                    it.remove(); // Remove matched element to prevent double-matching
                                    if (candidates.isEmpty()) {
                                        hashBuckets.remove(hash1); // Clean up empty bucket
                                    }
                                    matched = true;
                                    break;
                                }
                            }
                        }

                        // Slow path: element not found in expected bucket, scan all other buckets
                        // This handles hash collisions and ensures correctness
                        if (!matched) {
                            for (Iterator<Map.Entry<Integer, List<Object>>> bucketIter = hashBuckets.entrySet().iterator();
                                 bucketIter.hasNext();) {
                                Map.Entry<Integer, List<Object>> bucket = bucketIter.next();
                                if (bucket.getKey() == hash1) continue; // Already checked this bucket

                                List<Object> otherCandidates = bucket.getValue();
                                for (Iterator<Object> it = otherCandidates.iterator(); it.hasNext();) {
                                    Object candidate = it.next();
                                    if (elementEquals(elem1, candidate, valueBasedEquality, caseSensitive)) {
                                        it.remove();
                                        if (otherCandidates.isEmpty()) {
                                            bucketIter.remove();
                                        }
                                        matched = true;
                                        break;
                                    }
                                }
                                if (matched) break;
                            }
                        }

                        if (!matched) {
                            return false; // Element from array1 not found in array2
                        }
                    }
                }

                i++; // Move past SET_CLOSE
            } else if (elem1 == SET_OPEN || elem2 == SET_OPEN) {
                // One is SET_OPEN but not the other - mismatch
                return false;
            } else {
                // elementEquals handles identity check, NULL_SENTINEL, valueBasedEquality, and atomic types
                if (!elementEquals(elem1, elem2, valueBasedEquality, caseSensitive)) {
                    return false;
                }
                i++;
            }
        }
        return true;
    }
    
    /**
     * Compare Object[] to non-RandomAccess Collection using iterator.
     */
    private static boolean compareObjectArrayToCollection(Object[] array, Collection<?> coll, int size, boolean valueBasedEquality, boolean caseSensitive) {
        Iterator<?> iter = coll.iterator();
        int i = 0;
        while (i < size) {
            Object elem1 = array[i];
            Object elem2 = iter.next();

            // Check if we're entering a Set portion
            if (elem1 == SET_OPEN && elem2 == SET_OPEN) {
                // Count array set elements and collect iterator elements
                i++; // Move past SET_OPEN
                int arrayStartIdx = i;

                // Count array set elements
                int arraySetSize = 0;
                while (i < size && array[i] != SET_CLOSE) {
                    arraySetSize++;
                    i++;
                }

                // Collect iterator set elements into a temporary list (unavoidable for iterator)
                List<Object> iterElements = new ArrayList<>();
                while (iter.hasNext()) {
                    Object next = iter.next();
                    if (next == SET_CLOSE) break;
                    iterElements.add(next);
                }

                // Size check first (fast rejection)
                int setSize = arraySetSize;
                if (iterElements.size() != setSize) {
                    return false;
                }

                // Compare using size-appropriate strategy
                if (setSize == 0) {
                    // Empty sets - equal, nothing to compare
                    // No action needed - continue to next element
                } else if (setSize <= 6) {
                    // Optimization: For small Sets (≤6 elements), nested loop is faster than allocation
                    // O(n²) comparison (max 36 operations) - we only allocate the iterator list
                    // Track consumed matches to prevent double-matching under valueBasedEquality
                    boolean[] consumed = new boolean[setSize];
                    for (int s1 = arrayStartIdx; s1 < arrayStartIdx + arraySetSize; s1++) {
                        boolean found = false;
                        for (int s2 = 0; s2 < iterElements.size(); s2++) {
                            if (!consumed[s2] && elementEquals(array[s1], iterElements.get(s2), valueBasedEquality, caseSensitive)) {
                                consumed[s2] = true;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            return false;
                        }
                    }
                } else {
                    // For larger Sets (>6 elements), use hash-bucketing approach
                    // Build hash buckets for iterator elements (unavoidable since we already collected them)
                    Map<Integer, List<Object>> hashBuckets = new java.util.HashMap<>(Math.max(16, setSize * 4 / 3));
                    for (Object elem : iterElements) {
                        int hash = computeElementHash(elem, caseSensitive);
                        List<Object> bucket = hashBuckets.computeIfAbsent(hash, k -> new ArrayList<>(2));
                        bucket.add(elem);
                    }

                    // Match each array element against bucketed iterator elements
                    for (int idx = arrayStartIdx; idx < arrayStartIdx + arraySetSize; idx++) {
                        Object arrayElem = array[idx];
                        int hash1 = computeElementHash(arrayElem, caseSensitive);
                        List<Object> candidates = hashBuckets.get(hash1);

                        boolean matched = false;
                        if (candidates != null && !candidates.isEmpty()) {
                            for (Iterator<Object> it = candidates.iterator(); it.hasNext();) {
                                Object candidate = it.next();
                                if (elementEquals(arrayElem, candidate, valueBasedEquality, caseSensitive)) {
                                    it.remove();
                                    if (candidates.isEmpty()) {
                                        hashBuckets.remove(hash1);
                                    }
                                    matched = true;
                                    break;
                                }
                            }
                        }

                        // Slow path: scan all other buckets
                        if (!matched) {
                            for (Iterator<Map.Entry<Integer, List<Object>>> bucketIter = hashBuckets.entrySet().iterator();
                                 bucketIter.hasNext();) {
                                Map.Entry<Integer, List<Object>> bucket = bucketIter.next();
                                if (bucket.getKey() == hash1) continue;

                                List<Object> otherCandidates = bucket.getValue();
                                for (Iterator<Object> it = otherCandidates.iterator(); it.hasNext();) {
                                    Object candidate = it.next();
                                    if (elementEquals(arrayElem, candidate, valueBasedEquality, caseSensitive)) {
                                        it.remove();
                                        if (otherCandidates.isEmpty()) {
                                            bucketIter.remove();
                                        }
                                        matched = true;
                                        break;
                                    }
                                }
                                if (matched) break;
                            }
                        }

                        if (!matched) {
                            return false;
                        }
                    }
                }

                i++; // Move past SET_CLOSE
            } else if (elem1 == SET_OPEN || elem2 == SET_OPEN) {
                // One is SET_OPEN but not the other - mismatch
                return false;
            } else {
                if (!elementEquals(elem1, elem2, valueBasedEquality, caseSensitive)) {
                    return false;
                }
                i++;
            }
        }
        return true;
    }
    
    
    /**
     * Compare two Collections where at least one is non-RandomAccess.
     * Uses iterators for both.
     */
    private static boolean compareCollections(Collection<?> coll1, Collection<?> coll2, int size, boolean valueBasedEquality, boolean caseSensitive) {
        Iterator<?> iter1 = coll1.iterator();
        Iterator<?> iter2 = coll2.iterator();
        int i = 0;
        while (i < size) {
            Object elem1 = iter1.next();
            Object elem2 = iter2.next();

            // Check if we're entering a Set portion
            if (elem1 == SET_OPEN && elem2 == SET_OPEN) {
                i++;  // Count the SET_OPEN element itself

                // Collect elements from both iterators (unavoidable since we're using iterators)
                List<Object> set1Elements = new ArrayList<>();
                List<Object> set2Elements = new ArrayList<>();

                // Extract from first set (track nesting depth for nested Sets!)
                int depth1 = 1;  // We've already consumed the outer SET_OPEN
                while (iter1.hasNext() && depth1 > 0) {
                    Object next1 = iter1.next();
                    i++;
                    if (next1 == SET_OPEN) {
                        depth1++;
                        set1Elements.add(next1);
                    } else if (next1 == SET_CLOSE) {
                        depth1--;
                        if (depth1 > 0) {
                            set1Elements.add(next1);  // Inner SET_CLOSE, add it
                        }
                        // If depth1 == 0, we've found the matching outer SET_CLOSE, stop
                    } else {
                        set1Elements.add(next1);
                    }
                }

                // Extract from second set (track nesting depth for nested Sets!)
                int depth2 = 1;  // We've already consumed the outer SET_OPEN
                while (iter2.hasNext() && depth2 > 0) {
                    Object next2 = iter2.next();
                    if (next2 == SET_OPEN) {
                        depth2++;
                        set2Elements.add(next2);
                    } else if (next2 == SET_CLOSE) {
                        depth2--;
                        if (depth2 > 0) {
                            set2Elements.add(next2);  // Inner SET_CLOSE, add it
                        }
                        // If depth2 == 0, we've found the matching outer SET_CLOSE, stop
                    } else {
                        set2Elements.add(next2);
                    }
                }

                // Size check first (fast rejection)
                int setSize = set1Elements.size();
                if (set2Elements.size() != setSize) {
                    return false;
                }

                // Compare using size-appropriate strategy
                if (setSize == 0) {
                    // Empty sets - equal, nothing to compare
                    // No action needed - continue to next element
                } else if (setSize <= 6) {
                    // Optimization: For small Sets (≤6 elements), nested loop is faster than additional allocation
                    // O(n²) comparison (max 36 operations) - we already have the two lists
                    // Track consumed matches to prevent double-matching under valueBasedEquality
                    boolean[] consumed = new boolean[setSize];
                    for (int s1 = 0; s1 < set1Elements.size(); s1++) {
                        boolean found = false;
                        for (int s2 = 0; s2 < set2Elements.size(); s2++) {
                            if (!consumed[s2] && elementEquals(set1Elements.get(s1), set2Elements.get(s2), valueBasedEquality, caseSensitive)) {
                                consumed[s2] = true;
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            return false;
                        }
                    }
                } else {
                    // For larger Sets (>6 elements), use hash-bucketing approach instead of creating HashSets
                    // Build hash buckets for set2 elements (we already have them in a list)
                    Map<Integer, List<Object>> hashBuckets = new java.util.HashMap<>(Math.max(16, setSize * 4 / 3));
                    for (Object elem : set2Elements) {
                        int hash = computeElementHash(elem, caseSensitive);
                        List<Object> bucket = hashBuckets.computeIfAbsent(hash, k -> new ArrayList<>(2));
                        bucket.add(elem);
                    }

                    // Match each set1 element against bucketed set2 elements
                    for (Object setElem1 : set1Elements) {
                        int hash1 = computeElementHash(setElem1, caseSensitive);
                        List<Object> candidates = hashBuckets.get(hash1);

                        boolean matched = false;
                        if (candidates != null && !candidates.isEmpty()) {
                            for (Iterator<Object> it = candidates.iterator(); it.hasNext();) {
                                Object candidate = it.next();
                                if (elementEquals(setElem1, candidate, valueBasedEquality, caseSensitive)) {
                                    it.remove();
                                    if (candidates.isEmpty()) {
                                        hashBuckets.remove(hash1);
                                    }
                                    matched = true;
                                    break;
                                }
                            }
                        }

                        // Slow path: scan all other buckets
                        if (!matched) {
                            for (Iterator<Map.Entry<Integer, List<Object>>> bucketIter = hashBuckets.entrySet().iterator();
                                 bucketIter.hasNext();) {
                                Map.Entry<Integer, List<Object>> bucket = bucketIter.next();
                                if (bucket.getKey() == hash1) continue;

                                List<Object> otherCandidates = bucket.getValue();
                                for (Iterator<Object> it = otherCandidates.iterator(); it.hasNext();) {
                                    Object candidate = it.next();
                                    if (elementEquals(setElem1, candidate, valueBasedEquality, caseSensitive)) {
                                        it.remove();
                                        if (otherCandidates.isEmpty()) {
                                            bucketIter.remove();
                                        }
                                        matched = true;
                                        break;
                                    }
                                }
                                if (matched) break;
                            }
                        }

                        if (!matched) {
                            return false;
                        }
                    }
                }

                // Note: SET_CLOSE was already counted when extracted (line 2529), no need to count again
                continue;  // Skip the unconditional i++ below; the Set branch already advanced i correctly
            } else if (elem1 == SET_OPEN || elem2 == SET_OPEN) {
                // One is SET_OPEN but not the other - mismatch
                return false;
            } else {
                // Regular element-by-element comparison
                if (!elementEquals(elem1, elem2, valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            i++;
        }
        return true;
    }
    
    /**
     * Functional interface for accessing elements from either a List or Object[].
     * Allows us to unify primitive array comparison logic.
     */
    @FunctionalInterface
    private interface ElementAccessor {
        Object get(int index);
    }
    
    /**
     * Compare primitive array to elements accessed via ElementAccessor.
     * Unified implementation for both List and Object[] comparisons.
     * Optimized for each primitive type to avoid Array.get() overhead.
     */
    private static boolean comparePrimitiveArrayToElements(Object primArray, ElementAccessor accessor, int size, boolean valueBasedEquality, boolean caseSensitive) {
        Class<?> arrayClass = primArray.getClass();
        
        if (arrayClass == int[].class) {
            int[] array = (int[]) primArray;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(array[i], accessor.get(i), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == long[].class) {
            long[] array = (long[]) primArray;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(array[i], accessor.get(i), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == double[].class) {
            double[] array = (double[]) primArray;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(array[i], accessor.get(i), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == float[].class) {
            float[] array = (float[]) primArray;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(array[i], accessor.get(i), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == boolean[].class) {
            boolean[] array = (boolean[]) primArray;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(array[i], accessor.get(i), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == byte[].class) {
            byte[] array = (byte[]) primArray;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(array[i], accessor.get(i), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == char[].class) {
            char[] array = (char[]) primArray;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(array[i], accessor.get(i), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == short[].class) {
            short[] array = (short[]) primArray;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(array[i], accessor.get(i), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        }
        
        // Unknown primitive array type
        return false;
    }
    
    /**
     * Compare primitive array to Object[].
     * Direct access on both sides.
     */
    private static boolean comparePrimitiveArrayToObjectArray(Object primArray, Object[] objArray, int size, boolean valueBasedEquality, boolean caseSensitive) {
        return comparePrimitiveArrayToElements(primArray, i -> objArray[i], size, valueBasedEquality, caseSensitive);
    }
    
    /**
     * Compare primitive array to Collection.
     * For non-RandomAccess Collections, uses iterator.
     */
    private static boolean comparePrimitiveArrayToCollection(Object array, Collection<?> coll, int size, boolean valueBasedEquality, boolean caseSensitive) {
        Iterator<?> iter = coll.iterator();
        Class<?> arrayClass = array.getClass();
        
        if (arrayClass == int[].class) {
            int[] intArray = (int[]) array;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(intArray[i], iter.next(), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == long[].class) {
            long[] longArray = (long[]) array;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(longArray[i], iter.next(), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == double[].class) {
            double[] doubleArray = (double[]) array;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(doubleArray[i], iter.next(), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == float[].class) {
            float[] floatArray = (float[]) array;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(floatArray[i], iter.next(), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == boolean[].class) {
            boolean[] boolArray = (boolean[]) array;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(boolArray[i], iter.next(), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == byte[].class) {
            byte[] byteArray = (byte[]) array;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(byteArray[i], iter.next(), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == char[].class) {
            char[] charArray = (char[]) array;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(charArray[i], iter.next(), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        } else if (arrayClass == short[].class) {
            short[] shortArray = (short[]) array;
            for (int i = 0; i < size; i++) {
                if (!elementEquals(shortArray[i], iter.next(), valueBasedEquality, caseSensitive)) {
                    return false;
                }
            }
            return true;
        }
        
        return false;
    }
    
    // ======================== Value-Based Equality Methods ========================
    // These methods provide "semantic key matching" - focusing on logical value rather than exact type
    
    /**
     * Element equality comparison that respects the valueBasedEquality and caseSensitive configurations.
     */
    private static boolean elementEquals(Object a, Object b, boolean valueBasedEquality, boolean caseSensitive) {
        // Fast identity check - handles same object, both null, and NULL_SENTINEL cases
        if (a == b) return true;

        // Normalize internal null sentinel so comparisons treat stored sentinel and real null as equivalent
        if (a == NULL_SENTINEL) { a = null; }
        if (b == NULL_SENTINEL) { b = null; }

        // Check again after normalization - if one was NULL_SENTINEL and other was null, they're now equal
        if (a == b) return true;
        if (a == null || b == null) return false;  // One is null, the other isn't

        // Handle case-insensitive CharSequence comparison
        if (!caseSensitive && a instanceof CharSequence && b instanceof CharSequence) {
            // OPTIMIZATION: Use StringUtilities for efficient case-insensitive comparison
            // This avoids creating new String objects and works for all CharSequence types
            // StringUtilities.equalsIgnoreCase already does: identity check, null check, length check
            return StringUtilities.equalsIgnoreCase((CharSequence) a, (CharSequence) b);
        }
        
        if (valueBasedEquality) {
            return valueEquals(a, b);
        } else {
            // Type-strict equality: use Objects.equals, except for atomic types which
            // always use value-based comparison for intuitive behavior
            if (isAtomicType(a) && isAtomicType(b)) {
                return atomicValueEquals(a, b);
            }
            return Objects.equals(a, b);
        }
    }
    
    /**
     * Check if an object is an atomic type (AtomicBoolean, AtomicInteger, AtomicLong).
     */
    private static boolean isAtomicType(Object o) {
        return o instanceof AtomicBoolean || o instanceof AtomicInteger || o instanceof AtomicLong;
    }
    
    /**
     * Compare atomic types by their contained values.
     * This provides intuitive value-based equality for atomic types even in type-strict mode.
     * In type-strict mode, only same-type comparisons are allowed.
     */
    private static boolean atomicValueEquals(Object a, Object b) {
        // Fast path
        if (a == b) return true;
        if (a == null || b == null) return false;
        
        // AtomicBoolean comparison - only with other AtomicBoolean
        if (a instanceof AtomicBoolean && b instanceof AtomicBoolean) {
            return ((AtomicBoolean) a).get() == ((AtomicBoolean) b).get();
        }
        
        // AtomicInteger comparison - only with other AtomicInteger
        if (a instanceof AtomicInteger && b instanceof AtomicInteger) {
            return ((AtomicInteger) a).get() == ((AtomicInteger) b).get();
        }
        
        // AtomicLong comparison - only with other AtomicLong
        if (a instanceof AtomicLong && b instanceof AtomicLong) {
            return ((AtomicLong) a).get() == ((AtomicLong) b).get();
        }
        
        // Different atomic types don't match in type-strict mode
        return false;
    }
    
    private static boolean valueEquals(Object a, Object b) {
        // Note: Identity check (a == b) already done in elementEquals() before calling this
        if (a == null || b == null) return false;
        
        // Booleans: only equal to other booleans (including AtomicBoolean)
        if ((a instanceof Boolean || a instanceof AtomicBoolean) && 
            (b instanceof Boolean || b instanceof AtomicBoolean)) {
            boolean valA = (a instanceof Boolean) ? (Boolean) a : ((AtomicBoolean) a).get();
            boolean valB = (b instanceof Boolean) ? (Boolean) b : ((AtomicBoolean) b).get();
            return valA == valB;
        }
        
        // Numeric types: use value-based comparison (including atomic numeric types)
        if (a instanceof Number && b instanceof Number) {
            return compareNumericValues(a, b);
        }
        
        // All other types: use standard equals
        return a.equals(b);
    }
    
    
    /**
     * Compare two numeric values for equality with sensible type promotion rules:
     * 1. byte, short, int, long, AtomicInteger, AtomicLong compare as longs
     * 2. float & double compare by promoting float to double
     * 3. float/double can equal integral types only if they represent whole numbers
     * 4. BigInteger/BigDecimal use BigDecimal comparison
     */
    private static boolean compareNumericValues(Object a, Object b) {
        // Precondition: a and b are Numbers (AtomicInteger/AtomicLong extend Number)
        final Class<?> ca = a.getClass();
        final Class<?> cb = b.getClass();

        // 0) Same-class fast path (monomorphic & cheapest)
        if (ca == cb) {
            if (ca == Integer.class) return ((Integer) a).intValue()  == ((Integer) b).intValue();
            if (ca == Long.class)    return ((Long) a).longValue()     == ((Long) b).longValue();
            if (ca == Short.class)   return ((Short) a).shortValue()   == ((Short) b).shortValue();
            if (ca == Byte.class)    return ((Byte) a).byteValue()     == ((Byte) b).byteValue();
            if (ca == Double.class)  { double x = (Double) a, y = (Double) b; return (x == y) || (Double.isNaN(x) && Double.isNaN(y)); }
            if (ca == Float.class)   { float  x = (Float)  a, y = (Float)  b; return (x == y) || (Float.isNaN(x)  && Float.isNaN(y)); }
            if (ca == BigInteger.class) return ((BigInteger) a).compareTo((BigInteger) b) == 0;
            if (ca == BigDecimal.class) return ((BigDecimal) a).compareTo((BigDecimal) b) == 0;
            if (ca == AtomicInteger.class) return ((AtomicInteger) a).get() == ((AtomicInteger) b).get();
            if (ca == AtomicLong.class)    return ((AtomicLong) a).get()    == ((AtomicLong) b).get();
        }

        // 1) Integral-like ↔ integral-like (byte/short/int/long/atomics) as longs
        final boolean aInt = isIntegralLike(ca);
        final boolean bInt = isIntegralLike(cb);
        if (aInt && bInt) return extractLongFast(a) == extractLongFast(b);

        // 2) Float-like ↔ float-like (float/double): promote to double
        final boolean aFp = (ca == Double.class || ca == Float.class);
        final boolean bFp = (cb == Double.class || cb == Float.class);
        if (aFp && bFp) {
            final double x = ((Number) a).doubleValue();
            final double y = ((Number) b).doubleValue();
            return (x == y) || (Double.isNaN(x) && Double.isNaN(y));
        }

        // 3) Mixed integral ↔ float: equal only if finite and exactly integer (.0)
        if ((aInt && bFp) || (aFp && bInt)) {
            final double d = aFp ? ((Number) a).doubleValue() : ((Number) b).doubleValue();
            if (!Double.isFinite(d)) return false;
            final long li = aInt ? extractLongFast(a) : extractLongFast(b);
            // Quick fail then exactness check
            if ((long) d != li) return false;
            return d == (double) li;
        }

        // 4) BigInteger/BigDecimal involvement → compare via BigDecimal
        //    Guard: non-finite float/double (Infinity, NaN) can never equal BigDecimal/BigInteger
        if (isBig(ca) || isBig(cb)) {
            if (aFp && !Double.isFinite(((Number) a).doubleValue())) return false;
            if (bFp && !Double.isFinite(((Number) b).doubleValue())) return false;
            return toBigDecimal((Number) a).compareTo(toBigDecimal((Number) b)) == 0;
        }

        // 5) Fallback for odd Number subclasses
        return Objects.equals(a, b);
    }
    
    private static boolean isIntegralLike(Class<?> c) {
        return c == Integer.class || c == Long.class || c == Short.class || c == Byte.class
            || c == AtomicInteger.class || c == AtomicLong.class;
    }
    
    private static boolean isBig(Class<?> c) {
        return c == BigInteger.class || c == BigDecimal.class;
    }

    private static long extractLongFast(Object o) {
        if (o instanceof Long) return (Long)o;
        if (o instanceof Integer) return (Integer)o;
        if (o instanceof Short) return (Short)o;
        if (o instanceof Byte) return (Byte)o;
        if (o instanceof AtomicInteger) return ((AtomicInteger)o).get();
        if (o instanceof AtomicLong) return ((AtomicLong)o).get();
        return ((Number) o).longValue();
    }
    
    private static BigDecimal toBigDecimal(Number n) {
        if (n instanceof BigDecimal) return (BigDecimal) n;
        if (n instanceof BigInteger) return new BigDecimal((BigInteger) n);
        if (n instanceof Double || n instanceof Float) return new BigDecimal(n.toString()); // exact
        return BigDecimal.valueOf(n.longValue());
    }

    private V putInternal(MultiKey<V> newKey) {
        // Invalidate hashCode cache on mutation
        cachedHashCode = null;

        int hash = newKey.hash;
        int stripe = getStripeIndex(hash);
        Object monitor = stripeMonitors[stripe];
        V old;
        boolean resize;

        if (trackContentionMetrics) {
            long start = System.nanoTime();
            synchronized (monitor) {
                long waitNs = System.nanoTime() - start;
                if (waitNs > CONTENTION_THRESHOLD_NS) {
                    contentionCount.incrementAndGet();
                    stripeLockContention[stripe].incrementAndGet();
                }
                totalLockAcquisitions.incrementAndGet();
                stripeLockAcquisitions[stripe].incrementAndGet();
                old = putNoLock(newKey);
                resize = atomicSize.get() > resizeThreshold;
            }
        } else {
            synchronized (monitor) {
                old = putNoLock(newKey);
                resize = atomicSize.get() > resizeThreshold;
            }
        }

        resizeRequest(resize);

        return old;
    }

    private V getNoLock(MultiKey<V> lookupKey) {
        int hash = lookupKey.hash;
        final AtomicReferenceArray<MultiKey<V>[]> table = buckets;  // Volatile read of buckets
        final int mask = table.length() - 1;  // Array length is immutable
        int index = spread(hash) & mask;
        MultiKey<V>[] chain = table.get(index);

        if (chain == null) return null;

        for (MultiKey<V> e : chain) {
            if (e.hash == hash && keysMatch(e, lookupKey.keys)) {
                return e.value;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private V putNoLock(MultiKey<V> newKey) {
        int hash = newKey.hash;
        final AtomicReferenceArray<MultiKey<V>[]> table = buckets;  // Volatile read of buckets
        final int mask = table.length() - 1;  // Array length is immutable
        int index = spread(hash) & mask;
        MultiKey<V>[] chain = table.get(index);

        if (chain == null) {
            table.set(index, new MultiKey[]{newKey});
            atomicSize.incrementAndGet();
            updateMaxChainLength(1);
            return null;
        }

        for (int i = 0; i < chain.length; i++) {
            MultiKey<V> e = chain[i];
            if (e.hash == hash && keysMatch(e, newKey.keys)) {
                V old = e.value;
                // Create new array with replaced element - never mutate published array
                MultiKey<V>[] newChain = chain.clone();
                newChain[i] = newKey;
                table.set(index, newChain);
                return old;
            }
        }

        MultiKey<V>[] newChain = Arrays.copyOf(chain, chain.length + 1);
        newChain[chain.length] = newKey;
        table.set(index, newChain);
        atomicSize.incrementAndGet();
        updateMaxChainLength(newChain.length);
        return null;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified multidimensional key
     * using var-args syntax.
     * <p>This is a convenience method that allows easy multi-key existence checks without having
     * to pass arrays or collections. The keys are treated as separate dimensions of a multi-key.</p>
     * 
     * @param keys the key components to check for. Can be null or empty (treated as null key),
     *             single key, or multiple key components
     * @return {@code true} if this map contains a mapping for the specified multi-key
     * @see #containsKey(Object)
     */
    public boolean containsMultiKey(Object... keys) {
        if (keys == null || keys.length == 0) return containsKey(null);
        if (keys.length == 1) return containsKey(keys[0]);
        return containsKey(keys);  // Let containsKey()'s normalization handle everything!
    }

    /**
     * Optimized 2-key containsKey check with zero allocation using ThreadLocal array.
     *
     * @param k1 first key component
     * @param k2 second key component
     * @return {@code true} if this map contains a mapping for the multi-key
     */
    public boolean containsMultiKey(Object k1, Object k2) {
        Object[] key = LOOKUP_KEY_2.get();
        if (key[0] != null || key[1] != null) {
            return containsKey(new Object[]{k1, k2});
        }
        key[0] = k1;
        key[1] = k2;
        try {
            return containsKey(key);
        } finally {
            key[0] = null;
            key[1] = null;
        }
    }

    /**
     * Optimized 3-key containsKey check with zero allocation using ThreadLocal array.
     *
     * @param k1 first key component
     * @param k2 second key component
     * @param k3 third key component
     * @return {@code true} if this map contains a mapping for the multi-key
     */
    public boolean containsMultiKey(Object k1, Object k2, Object k3) {
        Object[] key = LOOKUP_KEY_3.get();
        if (key[0] != null || key[1] != null || key[2] != null) {
            return containsKey(new Object[]{k1, k2, k3});
        }
        key[0] = k1;
        key[1] = k2;
        key[2] = k3;
        try {
            return containsKey(key);
        } finally {
            key[0] = null;
            key[1] = null;
            key[2] = null;
        }
    }

    /**
     * Optimized 4-key containsKey check with zero allocation using ThreadLocal array.
     *
     * @param k1 first key component
     * @param k2 second key component
     * @param k3 third key component
     * @param k4 fourth key component
     * @return {@code true} if this map contains a mapping for the multi-key
     */
    public boolean containsMultiKey(Object k1, Object k2, Object k3, Object k4) {
        Object[] key = LOOKUP_KEY_4.get();
        if (key[0] != null || key[1] != null || key[2] != null || key[3] != null) {
            return containsKey(new Object[]{k1, k2, k3, k4});
        }
        key[0] = k1;
        key[1] = k2;
        key[2] = k3;
        key[3] = k4;
        try {
            return containsKey(key);
        } finally {
            key[0] = null;
            key[1] = null;
            key[2] = null;
            key[3] = null;
        }
    }

    /**
     * Optimized 5-key containsKey check with zero allocation using ThreadLocal array.
     *
     * @param k1 first key component
     * @param k2 second key component
     * @param k3 third key component
     * @param k4 fourth key component
     * @param k5 fifth key component
     * @return {@code true} if this map contains a mapping for the multi-key
     */
    public boolean containsMultiKey(Object k1, Object k2, Object k3, Object k4, Object k5) {
        Object[] key = LOOKUP_KEY_5.get();
        if (key[0] != null || key[1] != null || key[2] != null || key[3] != null || key[4] != null) {
            return containsKey(new Object[]{k1, k2, k3, k4, k5});
        }
        key[0] = k1;
        key[1] = k2;
        key[2] = k3;
        key[3] = k4;
        key[4] = k5;
        try {
            return containsKey(key);
        } finally {
            key[0] = null;
            key[1] = null;
            key[2] = null;
            key[3] = null;
            key[4] = null;
        }
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     * <p>This method supports both single keys and multidimensional keys. Arrays and Collections
     * are automatically expanded into multi-keys based on the map's configuration settings.</p>
     * 
     * @param key the key whose presence in this map is to be tested. Can be a single object,
     *            array, or Collection that will be normalized according to the map's settings
     * @return {@code true} if this map contains a mapping for the specified key
     */
    public boolean containsKey(Object key) {
        return findSimpleOrComplexKey(key) != null;
    }

    /**
     * Removes the mapping for the specified multidimensional key using var-args syntax.
     * <p>This is a convenience method that allows easy multi-key removal without having
     * to pass arrays or collections. The keys are treated as separate dimensions of a multi-key.</p>
     * 
     * @param keys the key components for the mapping to remove. Can be null or empty (treated as null key),
     *             single key, or multiple key components
     * @return the previous value associated with the multi-key, or {@code null} if there was
     *         no mapping for the key
     * @see #remove(Object)
     */
    public V removeMultiKey(Object... keys) {
        if (keys == null || keys.length == 0) return remove(null);
        if (keys.length == 1) return remove(keys[0]);
        return remove(keys);  // Let remove()'s normalization handle everything!
    }

    /**
     * Removes the mapping for the specified key from this map if it is present.
     * <p>This method supports both single keys and multidimensional keys. Arrays and Collections
     * are automatically expanded into multi-keys based on the map's configuration settings.</p>
     * 
     * @param key the key whose mapping is to be removed from the map. Can be a single object,
     *            array, or Collection that will be normalized according to the map's settings
     * @return the previous value associated with the key, or {@code null} if there was
     *         no mapping for the key
     */
    public V remove(Object key) {
        final MultiKey<V> removeKey = createMultiKey(key, null);
        return removeInternal(removeKey);
    }

    private V removeInternal(final MultiKey<V> removeKey) {
        // Invalidate hashCode cache on mutation
        cachedHashCode = null;

        int hash = removeKey.hash;
        int stripe = getStripeIndex(hash);
        Object monitor = stripeMonitors[stripe];
        V old;

        if (trackContentionMetrics) {
            long start = System.nanoTime();
            synchronized (monitor) {
                long waitNs = System.nanoTime() - start;
                if (waitNs > CONTENTION_THRESHOLD_NS) {
                    contentionCount.incrementAndGet();
                    stripeLockContention[stripe].incrementAndGet();
                }
                totalLockAcquisitions.incrementAndGet();
                stripeLockAcquisitions[stripe].incrementAndGet();
                old = removeNoLock(removeKey);
            }
        } else {
            synchronized (monitor) {
                old = removeNoLock(removeKey);
            }
        }

        return old;
    }

    @SuppressWarnings("unchecked")
    private V removeNoLock(MultiKey<V> removeKey) {
        int hash = removeKey.hash;
        final AtomicReferenceArray<MultiKey<V>[]> table = buckets;  // Volatile read of buckets
        final int mask = table.length() - 1;  // Array length is immutable
        int index = spread(hash) & mask;
        MultiKey<V>[] chain = table.get(index);

        if (chain == null) return null;

        for (int i = 0; i < chain.length; i++) {
            MultiKey<V> e = chain[i];
            if (e.hash == hash && keysMatch(e, removeKey.keys)) {
                V old = e.value;
                if (chain.length == 1) {
                    table.set(index, null);
                } else {
                    // Create new array without the removed element - never mutate published array
                    MultiKey<V>[] newChain = new MultiKey[chain.length - 1];
                    // Copy elements before the removed one
                    System.arraycopy(chain, 0, newChain, 0, i);
                    // Copy elements after the removed one
                    System.arraycopy(chain, i + 1, newChain, i, chain.length - i - 1);
                    table.set(index, newChain);
                }
                atomicSize.decrementAndGet();
                return old;
            }
        }
        return null;
    }

    private void resizeInternal() {
        withAllStripeLocks(() -> {
            double lf = (double) atomicSize.get() / buckets.length();
            if (lf <= loadFactor) return;

            AtomicReferenceArray<MultiKey<V>[]> oldBuckets = buckets;
            AtomicReferenceArray<MultiKey<V>[]> newBuckets = new AtomicReferenceArray<>(oldBuckets.length() * 2);
            int newMax = 0;
            atomicSize.set(0);

            for (int i = 0; i < oldBuckets.length(); i++) {
                MultiKey<V>[] chain = oldBuckets.get(i);
                if (chain != null) {
                    for (MultiKey<V> e : chain) {
                        int len = rehashEntry(e, newBuckets);
                        atomicSize.incrementAndGet();
                        newMax = Math.max(newMax, len);
                    }
                }
            }
            maxChainLength.set(newMax);
            // Replace buckets atomically after all entries are rehashed
            buckets = newBuckets;
            resizeThreshold = (long)(newBuckets.length() * loadFactor);
        });
    }

    @SuppressWarnings("unchecked")
    private int rehashEntry(MultiKey<V> entry, AtomicReferenceArray<MultiKey<V>[]> target) {
        int index = spread(entry.hash) & (target.length() - 1);
        MultiKey<V>[] chain = target.get(index);
        if (chain == null) {
            target.set(index, new MultiKey[]{entry});
            return 1;
        } else {
            MultiKey<V>[] newChain = Arrays.copyOf(chain, chain.length + 1);
            newChain[chain.length] = entry;
            target.set(index, newChain);
            return newChain.length;
        }
    }

    /**
     * Helper method to handle resize request.
     * Performs resize if requested and no resize is already in progress.
     * Uses double-checked pattern to avoid unnecessary CAS operations.
     *
     * @param resize whether to perform resize
     */
    private void resizeRequest(boolean resize) {
        // Quick exit if no resize needed or already in progress
        if (!resize || resizeInProgress.get()) {
            return;
        }

        // Double-check load factor before expensive CAS - another thread may have resized
        if (atomicSize.get() <= resizeThreshold) {
            return;
        }

        // Attempt to acquire resize permission via CAS
        if (resizeInProgress.compareAndSet(false, true)) {
            try {
                resizeInternal();
            } finally {
                resizeInProgress.set(false);
            }
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     * <p>Note: Due to the {@link Map#size()} interface contract requiring {@code int},
     * this method returns {@link Integer#MAX_VALUE} if the actual size exceeds 2<sup>31</sup>-1.
     * For maps that may exceed this limit, use {@link #longSize()} to get the accurate count.</p>
     *
     * @return the number of key-value mappings in this map, capped at {@link Integer#MAX_VALUE}
     * @see #longSize()
     */
    public int size() {
        long sz = atomicSize.get();
        return sz > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sz;
    }

    /**
     * Returns the exact number of key-value mappings in this map as a {@code long}.
     * <p>This method provides the true size without the 2<sup>31</sup>-1 limitation
     * imposed by the {@link Map#size()} contract. Use this method when working with
     * very large maps that may contain more than {@link Integer#MAX_VALUE} entries.</p>
     *
     * @return the exact number of key-value mappings in this map
     * @see #size()
     */
    public long longSize() {
        return atomicSize.get();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return atomicSize.get() == 0;
    }

    /**
     * Removes all the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        // Invalidate hashCode cache on mutation
        cachedHashCode = null;

        withAllStripeLocks(() -> {
            final AtomicReferenceArray<MultiKey<V>[]> table = buckets;  // Pin table reference
            for (int i = 0; i < table.length(); i++) {
                table.set(i, null);
            }
            atomicSize.set(0);
            maxChainLength.set(0);
        });
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     * <p>This operation requires time linear in the map size.</p>
     * 
     * @param value the value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the specified value
     */
    public boolean containsValue(Object value) {
        final AtomicReferenceArray<MultiKey<V>[]> table = buckets;  // Pin table reference
        for (int i = 0; i < table.length(); i++) {
            MultiKey<V>[] chain = table.get(i);
            if (chain != null) {
                for (MultiKey<V> e : chain) if (Objects.equals(e.value, value)) return true;
            }
        }
        return false;
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * <p>Multidimensional keys are represented as List (ordered) or Set (unordered),
     * while single keys are returned as their original objects. This provides a consistent
     * mental model: keys are always single items, Lists, or Sets - regardless of nesting depth.</p>
     *
     * <p><b>CONTRACT VIOLATION:</b> This method returns a <b>snapshot</b>, not a live view.
     * Changes to the returned set are <b>NOT</b> reflected in the map, and vice versa.
     * This violates the {@link Map#keySet()} contract which requires a live view.</p>
     *
     * @return a snapshot set view of the keys contained in this map
     */
    public Set<Object> keySet() {
        Set<Object> set = new HashSet<>();
        final AtomicReferenceArray<MultiKey<V>[]> snapshot = buckets;
        final int len = snapshot.length();
        for (int bucketIdx = 0; bucketIdx < len; bucketIdx++) {
            MultiKey<V>[] chain = snapshot.get(bucketIdx);
            if (chain != null) {
                for (MultiKey<V> e : chain) {
                    Object keys = e.keys;
                    Object k;
                    if (keys == null || keys == NULL_SENTINEL) {
                        k = null;
                    } else if (keys instanceof Object[]) {
                        Object[] keysArray = (Object[]) keys;
                        k = keysArray.length == 1 ? (keysArray[0] == NULL_SENTINEL ? null : keysArray[0]) : reconstructKey(keysArray);
                    } else if (keys instanceof Collection) {
                        if (collectionKeyMode == CollectionKeyMode.COLLECTIONS_NOT_EXPANDED) {
                            k = keys;
                        } else {
                            Object[] keysArray = ((Collection<?>) keys).toArray();
                            k = reconstructKey(keysArray);
                        }
                    } else {
                        k = keys;
                    }
                    set.add(k);
                }
            }
        }
        return set;
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     *
     * <p><b>CONTRACT VIOLATION:</b> This method returns a <b>snapshot</b>, not a live view.
     * Changes to the returned collection are <b>NOT</b> reflected in the map, and vice versa.
     * This violates the {@link Map#values()} contract which requires a live view.</p>
     *
     * @return a snapshot collection view of the values contained in this map
     */
    public Collection<V> values() {
        List<V> vals = new ArrayList<>();
        final AtomicReferenceArray<MultiKey<V>[]> snapshot = buckets;
        final int len = snapshot.length();
        for (int bucketIdx = 0; bucketIdx < len; bucketIdx++) {
            MultiKey<V>[] chain = snapshot.get(bucketIdx);
            if (chain != null) {
                for (MultiKey<V> e : chain) {
                    vals.add(e.value);
                }
            }
        }
        return vals;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * <p>Multidimensional keys are represented as List (ordered) or Set (unordered),
     * while single keys are returned as their original objects. This provides a consistent
     * mental model: keys are always single items, Lists, or Sets - regardless of nesting depth.</p>
     *
     * <p><b>CONTRACT VIOLATION:</b> This method returns a <b>snapshot</b>, not a live view.
     * Changes to the returned set are <b>NOT</b> reflected in the map, and vice versa.
     * This violates the {@link Map#entrySet()} contract which requires a live view.</p>
     *
     * <p><b>Rationale:</b> Implementing a true live view would require maintaining bidirectional
     * references between the set and map, adding significant complexity and memory overhead.
     * The snapshot approach provides better performance and thread-safety characteristics
     * for this concurrent data structure, at the cost of contract compliance.</p>
     *
     * @return a snapshot set view of the mappings contained in this map
     */
    public Set<Map.Entry<Object, V>> entrySet() {
        Set<Map.Entry<Object, V>> set = new HashSet<>();
        // Iterate buckets directly instead of using deprecated entries()
        final AtomicReferenceArray<MultiKey<V>[]> snapshot = buckets;
        final int len = snapshot.length();
        for (int bucketIdx = 0; bucketIdx < len; bucketIdx++) {
            MultiKey<V>[] chain = snapshot.get(bucketIdx);
            if (chain != null) {
                for (MultiKey<V> e : chain) {
                    // Reconstruct key for external presentation
                    Object keys = e.keys;
                    Object k;
                    if (keys == null || keys == NULL_SENTINEL) {
                        k = null;
                    } else if (keys instanceof Object[]) {
                        Object[] keysArray = (Object[]) keys;
                        k = keysArray.length == 1 ? (keysArray[0] == NULL_SENTINEL ? null : keysArray[0]) : reconstructKey(keysArray);
                    } else if (keys instanceof Collection) {
                        // In COLLECTIONS_NOT_EXPANDED mode, Collections are stored as single keys without flattening
                        // Return them as-is to preserve the original type and hashCode
                        if (collectionKeyMode == CollectionKeyMode.COLLECTIONS_NOT_EXPANDED) {
                            k = keys;
                        } else {
                            // Collection represents nested structure - need to convert to array for reconstruction
                            Object[] keysArray = ((Collection<?>) keys).toArray();
                            k = reconstructKey(keysArray);
                        }
                    } else {
                        // Single key
                        k = keys;
                    }
                    set.add(new AbstractMap.SimpleEntry<>(k, e.value));
                }
            }
        }
        return set;
    }

    /**
     * Copies all the mappings from the specified map to this map.
     * <p>The effect of this call is equivalent to that of calling {@link #put(Object, Object)}
     * on this map once for each mapping from key {@code k} to value {@code v} in the
     * specified map.</p>
     * 
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     */
    public void putAll(Map<?, ? extends V> m) {
        for (Map.Entry<?, ? extends V> e : m.entrySet()) put(e.getKey(), e.getValue());
    }

    /**
     * If the specified key is not already associated with a value, associates it with the given value.
     * <p>This is equivalent to:
     * <pre> {@code
     * if (!map.containsKey(key))
     *   return map.put(key, value);
     * else
     *   return map.get(key);
     * }</pre>
     * except that the action is performed atomically.</p>
     * 
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null}
     *         if there was no mapping for the key
     */
    public V putIfAbsent(Object key, V value) {
        V existing = get(key);
        if (existing != null) return existing;

        // Normalize the key once, outside the lock
        MultiKey<V> norm = flattenKey(key);
        Object normalizedKey = norm.keys;
        int hash = norm.hash;
        boolean resize = false;

        synchronized (getStripeMonitor(hash)) {
            // Check again inside the lock
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            existing = getNoLock(lookupKey);
            if (existing == null) {
                // Use putNoLock directly to avoid double locking
                MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, value);
                putNoLock(newKey);
                cachedHashCode = null;
                resize = atomicSize.get() > resizeThreshold;
            }
        }
        // Handle resize outside the lock
        resizeRequest(resize);
        return existing;
    }

    /**
     * If the specified key is not already associated with a value, attempts to compute its value
     * using the given mapping function and enters it into this map unless {@code null}.
     * <p>The entire method invocation is performed atomically, so the function is applied
     * at most once per key.</p>
     * 
     * @param key the key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with the specified key,
     *         or {@code null} if the computed value is {@code null}
     * @throws NullPointerException if the specified mappingFunction is null
     */
    public V computeIfAbsent(Object key, Function<? super Object, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);

        // Normalize key once upfront - avoid double normalization that get() + flattenKey() would cause
        MultiKey<V> norm = flattenKey(key);
        Object normalizedKey = norm.keys;
        int hash = norm.hash;

        // Fast path: check if value exists without locking (lock-free read)
        // Per ConcurrentMap contract, computeIfAbsent treats null values as "absent"
        MultiKey<V> existing = findEntryWithPrecomputedHash(normalizedKey, hash);
        if (existing != null && existing.value != null) {
            return existing.value;
        }

        boolean resize = false;
        V v;

        synchronized (getStripeMonitor(hash)) {
            // Double-check under lock
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            v = getNoLock(lookupKey);
            if (v == null) {
                v = mappingFunction.apply(key);
                if (v != null) {
                    // Create new key with value and use putNoLock
                    MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, v);
                    putNoLock(newKey);
                    cachedHashCode = null;
                    resize = atomicSize.get() > resizeThreshold;
                }
            }
        }
        // Handle resize outside the lock
        resizeRequest(resize);
        return v;
    }

    /**
     * If the specified key is not already associated with a value, attempts to compute a new mapping
     * given the key and its current mapped value.
     * <p>The entire method invocation is performed atomically. If the function returns
     * {@code null}, the mapping is removed.</p>
     *
     * @param key the key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or {@code null} if none
     * @throws NullPointerException if the specified remappingFunction is null
     */
    public V computeIfPresent(Object key, BiFunction<? super Object, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);

        // Normalize key once upfront - avoid double normalization that get() + flattenKey() would cause
        MultiKey<V> norm = flattenKey(key);
        Object normalizedKey = norm.keys;
        int hash = norm.hash;

        // Fast path: check if value exists without locking (lock-free read)
        MultiKey<V> existing = findEntryWithPrecomputedHash(normalizedKey, hash);
        if (existing == null) {
            return null;
        }

        boolean resize = false;
        V result = null;

        synchronized (getStripeMonitor(hash)) {
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            V old = getNoLock(lookupKey);
            if (old != null) {
                V newV = remappingFunction.apply(key, old);
                if (newV != null) {
                    // Replace with new value using putNoLock
                    MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, newV);
                    putNoLock(newKey);
                    cachedHashCode = null;
                    resize = atomicSize.get() > resizeThreshold;
                    result = newV;
                } else {
                    // Remove using removeNoLock
                    MultiKey<V> removeKey = new MultiKey<>(normalizedKey, hash, old);
                    removeNoLock(removeKey);
                    cachedHashCode = null;
                }
            }
        }
        // Handle resize outside the lock
        resizeRequest(resize);
        return result;
    }

    /**
     * Attempts to compute a mapping for the specified key and its current mapped value
     * (or {@code null} if there is no current mapping).
     * <p>The entire method invocation is performed atomically. If the function returns
     * {@code null}, the mapping is removed (or remains absent if initially absent).</p>
     *
     * @param key the key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or {@code null} if none
     * @throws NullPointerException if the specified remappingFunction is null
     */
    public V compute(Object key, BiFunction<? super Object, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);

        MultiKey<V> norm = flattenKey(key);
        Object normalizedKey = norm.keys;
        int hash = norm.hash;
        boolean resize = false;

        V result;
        synchronized (getStripeMonitor(hash)) {
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            V old = getNoLock(lookupKey);
            V newV = remappingFunction.apply(key, old);

            if (newV == null) {
                // Check if key existed (even with null value) and remove if so
                if (old != null || findEntryWithPrecomputedHash(normalizedKey, hash) != null) {
                    MultiKey<V> removeKey = new MultiKey<>(normalizedKey, hash, old);
                    removeNoLock(removeKey);
                    cachedHashCode = null;
                }
                result = null;
            } else {
                // Put new value using putNoLock
                MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, newV);
                putNoLock(newKey);
                cachedHashCode = null;
                resize = atomicSize.get() > resizeThreshold;
                result = newV;
            }
        }
        // Handle resize outside the lock
        resizeRequest(resize);
        return result;
    }

    /**
     * If the specified key is not already associated with a value or is associated with null,
     * associates it with the given non-null value. Otherwise, replaces the associated value
     * with the results of the given remapping function, or removes if the result is {@code null}.
     * <p>The entire method invocation is performed atomically.</p>
     * 
     * @param key the key with which the resulting value is to be associated
     * @param value the non-null value to be merged with the existing value
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or {@code null} if no
     *         value is associated with the key
     * @throws NullPointerException if the specified value or remappingFunction is null
     */
    public V merge(Object key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(remappingFunction);

        MultiKey<V> norm = flattenKey(key);
        Object normalizedKey = norm.keys;
        int hash = norm.hash;
        boolean resize = false;

        V result;
        synchronized (getStripeMonitor(hash)) {
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            V old = getNoLock(lookupKey);
            V newV = old == null ? value : remappingFunction.apply(old, value);

            if (newV == null) {
                // Remove using removeNoLock
                MultiKey<V> removeKey = new MultiKey<>(normalizedKey, hash, old);
                removeNoLock(removeKey);
            } else {
                // Put new value using putNoLock
                MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, newV);
                putNoLock(newKey);
                resize = atomicSize.get() > resizeThreshold;
            }
            cachedHashCode = null;
            result = newV;
        }
        // Handle resize outside the lock
        resizeRequest(resize);
        return result;
    }

    /**
     * Removes the entry for a key only if it is currently mapped to the specified value.
     * <p>This is equivalent to:
     * <pre> {@code
     * if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
     *   map.remove(key);
     *   return true;
     * } else
     *   return false;
     * }</pre>
     * except that the action is performed atomically.</p>
     * 
     * @param key the key with which the specified value is to be associated
     * @param value the value expected to be associated with the specified key
     * @return {@code true} if the value was removed
     */
    public boolean remove(Object key, Object value) {
        MultiKey<V> norm = flattenKey(key);
        Object normalizedKey = norm.keys;
        int hash = norm.hash;

        synchronized (getStripeMonitor(hash)) {
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            V current = getNoLock(lookupKey);
            // If current is null, verify the key actually exists before comparing
            if (current == null && findEntryWithPrecomputedHash(normalizedKey, hash) == null) {
                return false;
            }
            if (!Objects.equals(current, value)) return false;

            // Remove using removeNoLock
            MultiKey<V> removeKey = new MultiKey<>(normalizedKey, hash, current);
            removeNoLock(removeKey);
            cachedHashCode = null;
            return true;
        }
    }

    /**
     * Replaces the entry for the specified key only if it is currently mapped to some value.
     * <p>This is equivalent to:
     * <pre> {@code
     * if (map.containsKey(key)) {
     *   return map.put(key, value);
     * } else
     *   return null;
     * }</pre>
     * except that the action is performed atomically.</p>
     * 
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null}
     *         if there was no mapping for the key
     */
    public V replace(Object key, V value) {
        MultiKey<V> norm = flattenKey(key);
        Object normalizedKey = norm.keys;
        int hash = norm.hash;
        boolean resize = false;

        V result;
        synchronized (getStripeMonitor(hash)) {
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            V old = getNoLock(lookupKey);
            if (old == null && findEntryWithPrecomputedHash(normalizedKey, hash) == null) {
                result = null; // Key doesn't exist
            } else {
                // Replace with new value using putNoLock
                MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, value);
                result = putNoLock(newKey);
                cachedHashCode = null;
                resize = atomicSize.get() > resizeThreshold;
            }
        }
        // Handle resize outside the lock
        resizeRequest(resize);
        return result;
    }

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     * <p>This is equivalent to:
     * <pre> {@code
     * if (map.containsKey(key) && Objects.equals(map.get(key), oldValue)) {
     *   map.put(key, newValue);
     *   return true;
     * } else
     *   return false;
     * }</pre>
     * except that the action is performed atomically.</p>
     * 
     * @param key the key with which the specified value is to be associated
     * @param oldValue the value expected to be associated with the specified key
     * @param newValue the value to be associated with the specified key
     * @return {@code true} if the value was replaced
     */
    public boolean replace(Object key, V oldValue, V newValue) {
        MultiKey<V> norm = flattenKey(key);
        Object normalizedKey = norm.keys;
        int hash = norm.hash;
        boolean resize = false;

        boolean result = false;
        synchronized (getStripeMonitor(hash)) {
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            V current = getNoLock(lookupKey);
            // If current is null, verify the key actually exists before comparing
            if (current == null && findEntryWithPrecomputedHash(normalizedKey, hash) == null) {
                result = false;
            } else if (Objects.equals(current, oldValue)) {
                // Replace with new value using putNoLock
                MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, newValue);
                putNoLock(newKey);
                cachedHashCode = null;
                resize = atomicSize.get() > resizeThreshold;
                result = true;
            }
        }
        // Handle resize outside the lock
        resizeRequest(resize);
        return result;
    }

    /**
     * Returns the hash code value for this map.
     * <p>The hash code of a map is defined to be the sum of the hash codes of each entry
     * in the map's {@code entrySet()} view. This ensures that {@code m1.equals(m2)}
     * implies that {@code m1.hashCode()==m2.hashCode()} for any two maps {@code m1} and
     * {@code m2}, as required by the general contract of {@link Object#hashCode}.</p>
     * 
     * @return the hash code value for this map
     */
    public int hashCode() {
        // Fast path: return cached value if available
        Integer cached = cachedHashCode;
        if (cached != null) {
            return cached;
        }

        int h = 0;
        if (caseSensitive) {
            // Case-sensitive: use entrySet() with standard Objects.hashCode()
            for (Map.Entry<Object, V> e : entrySet()) {
                h += Objects.hashCode(e.getKey()) ^ Objects.hashCode(e.getValue());
            }
        } else {
            // Case-insensitive: iterate internal buckets and use MultiKey.hash
            // (pre-computed via computeElementHash with case-insensitive string hashing).
            // This ensures two equal case-insensitive maps with different-case keys
            // produce the same hashCode.
            final AtomicReferenceArray<MultiKey<V>[]> snapshot = buckets;
            final int len = snapshot.length();
            for (int bucketIdx = 0; bucketIdx < len; bucketIdx++) {
                MultiKey<V>[] chain = snapshot.get(bucketIdx);
                if (chain != null) {
                    for (MultiKey<V> mk : chain) {
                        h += mk.hash ^ Objects.hashCode(mk.value);
                    }
                }
            }
        }

        // Cache the result (volatile write ensures visibility to all threads)
        cachedHashCode = h;
        return h;
    }

    /**
     * Compares the specified object with this map for equality.
     * <p>Returns {@code true} if the given object is also a map and the two maps
     * represent the same mappings. Two maps {@code m1} and {@code m2} represent the
     * same mappings if {@code m1.entrySet().equals(m2.entrySet())}.</p>
     * 
     * @param o object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Map)) return false;
        Map<?, ?> m = (Map<?, ?>) o;
        if (m.size() != size()) return false;

        // OPTIMIZATION: Walk the OTHER map and query THIS map
        // This leverages get()'s matching logic and avoids reconstructing OUR keys
        // When other map is also a MultiKeyMap, its entrySet() does the reconstruction
        // and we just use get() which already handles all the matching semantics
        for (Map.Entry<?, ?> entry : m.entrySet()) {
            Object key = entry.getKey();
            Object theirValue = entry.getValue();

            // Use get() which handles all matching logic (Sets, Lists, etc.)
            Object myValue = this.get(key);

            // Check if values match, handling null correctly
            if (!Objects.equals(theirValue, myValue) || (theirValue == null && !this.containsKey(key))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a string representation of this map.
     * <p>The string representation consists of a list of key-value mappings in the order 
     * returned by the map's entries iterator, enclosed in braces ({}).</p>
     * <p>Each key-value mapping is rendered as "key → value", where the key part shows 
     * all key components and the value part shows the mapped value. Adjacent mappings 
     * are separated by commas and newlines.</p>
     * <p>Empty maps are represented as "{}".</p>
     * 
     * @return a string representation of this map, formatted for readability with 
     *         multi-line output and proper indentation
     */
    public String toString() {
        if (isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{\n");
        boolean first = true;
        // Iterate buckets directly to access raw keys for formatting
        final AtomicReferenceArray<MultiKey<V>[]> snapshot = buckets;
        final int len = snapshot.length();
        for (int bucketIdx = 0; bucketIdx < len; bucketIdx++) {
            MultiKey<V>[] chain = snapshot.get(bucketIdx);
            if (chain != null) {
                for (MultiKey<V> e : chain) {
                    if (!first) sb.append(",\n");
                    first = false;
                    sb.append("  ");  // Two-space indentation
                    // Access raw keys for dumpExpandedKeyStatic
                    Object keys = e.keys;
                    Object[] keysArray;
                    if (keys instanceof Object[]) {
                        keysArray = (Object[]) keys;
                    } else if (keys instanceof Collection) {
                        keysArray = ((Collection<?>) keys).toArray();
                    } else {
                        keysArray = new Object[]{keys};
                    }
                    String keyStr = dumpExpandedKeyStatic(keysArray, true, this);
                    // Remove trailing comma and space if present
                    if (keyStr.endsWith(", ")) {
                        keyStr = keyStr.substring(0, keyStr.length() - 2);
                    }
                    sb.append(keyStr).append(" → ");
                    sb.append(EMOJI_VALUE);
                    sb.append(formatValueForToString(e.value, this));
                }
            }
        }
        return sb.append("\n}").toString();
    }



    /**
     * Reconstructs keys as native structures (List, Set, or single items) for serialization.
     * This is the preferred method for entrySet()/keySet() as it returns structures that serialize
     * naturally in JSON without requiring marker parsing.
     * <p>
     * Returns:
     * <ul>
     *   <li>Single item if the key has only one component</li>
     *   <li>List (ArrayList) for ordered sequences (from OPEN/CLOSE markers or unmarked arrays)</li>
     *   <li>Set (LinkedHashSet) for unordered sequences (from SET_OPEN/SET_CLOSE markers)</li>
     * </ul>
     * <p>Nested structures are handled recursively, so Lists can contain Sets/Lists/singles,
     * and Sets can contain Lists/Sets/singles. This ensures proper equals/hashCode behavior
     * at all nesting levels.</p>
     *
     * @param in the flattened internal key array with markers
     * @return the reconstructed key using native List/Set/single-item structures
     */
    public static Object reconstructKey(Object[] in) {
        // Check if there are any markers in the array
        boolean hasMarkers = false;
        boolean hasSetMarker = false;
        for (Object elem : in) {
            if (elem == OPEN || elem == CLOSE) {
                hasMarkers = true;
            } else if (elem == SET_OPEN || elem == SET_CLOSE) {
                hasMarkers = true;
                hasSetMarker = true;
            }
        }

        // If no markers, check if it's a single element or a multi-element sequence
        if (!hasMarkers) {
            if (in.length == 1) {
                // Single element without markers - return as-is (it's a simple key)
                return (in[0] == NULL_SENTINEL) ? null : in[0];
            } else {
                // Multi-element array without markers - return as List (ordered sequence)
                List<Object> elements = new ArrayList<>(in.length);
                for (int i = 0; i < in.length; i++) {
                    elements.add((in[i] == NULL_SENTINEL) ? null : in[i]);
                }
                return Collections.unmodifiableList(elements);
            }
        }

        // If only Set markers and no List markers, wrap entire content as Set
        if (hasSetMarker && !hasNonSetMarkers(in)) {
            List<Object> elements = new ArrayList<>();
            for (Object elem : in) {
                if (elem != SET_OPEN && elem != SET_CLOSE) {
                    elements.add(elem == NULL_SENTINEL ? null : elem);
                }
            }
            return Collections.unmodifiableSet(new LinkedHashSet<>(elements));
        }

        // Has markers - reconstruct composite structure
        List<Object> components = new ArrayList<>();
        int[] index = {0};  // Use array for mutability

        while (index[0] < in.length) {
            Object elem = in[index[0]];

            if (elem == OPEN) {
                index[0]++;  // Skip OPEN
                List<Object> elements = collectElementsToNative(in, index, CLOSE);
                components.add(Collections.unmodifiableList(elements));  // Return as List for ordered sequence
                index[0]++;  // Skip CLOSE
            } else if (elem == SET_OPEN) {
                index[0]++;  // Skip SET_OPEN
                List<Object> elements = collectElementsToNative(in, index, SET_CLOSE);
                components.add(Collections.unmodifiableSet(new LinkedHashSet<>(elements)));  // Return as LinkedHashSet
                index[0]++;  // Skip SET_CLOSE
            } else {
                // Regular element (not inside a collection marker)
                components.add(elem == NULL_SENTINEL ? null : elem);
                index[0]++;
            }
        }

        // Return single element if only one component, otherwise return as List
        if (components.size() == 1) {
            return components.get(0);
        } else {
            return Collections.unmodifiableList(components);
        }
    }

    /**
     * Helper method to collect elements between open and close markers for native reconstruction.
     * Handles nested structures recursively, returning List/Set structures.
     *
     * @param in the flattened key array
     * @param index current position (mutated as elements are collected)
     * @param closeMarker the marker that ends this collection (CLOSE or SET_CLOSE)
     * @return list of collected elements (to be converted to List or Set by caller)
     */
    private static List<Object> collectElementsToNative(Object[] in, int[] index, Object closeMarker) {
        List<Object> elements = new ArrayList<>();

        while (index[0] < in.length && in[index[0]] != closeMarker) {
            Object current = in[index[0]];

            if (current == SET_OPEN || current == OPEN) {
                // Nested structure - find matching close and recurse
                Object matchingClose = (current == SET_OPEN) ? SET_CLOSE : CLOSE;
                int closeIdx = findMatchingClose(in, index[0], current, matchingClose);
                Object[] nested = Arrays.copyOfRange(in, index[0], closeIdx + 1);
                elements.add(reconstructKey(nested));  // Use native reconstruction
                index[0] = closeIdx + 1;
            } else {
                elements.add(current == NULL_SENTINEL ? null : current);
                index[0]++;
            }
        }

        return elements;
    }

    /**
     * Helper method to check if array has non-Set markers (OPEN/CLOSE).
     */
    private static boolean hasNonSetMarkers(Object[] arr) {
        for (Object elem : arr) {
            if (elem == OPEN || elem == CLOSE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper method to find the matching closing marker for an opening marker.
     * Handles nested structures correctly by counting open/close pairs.
     */
    private static int findMatchingClose(Object[] arr, int openIdx, Object openMarker, Object closeMarker) {
        int depth = 1;
        for (int i = openIdx + 1; i < arr.length; i++) {
            if (arr[i] == openMarker) {
                depth++;
            } else if (arr[i] == closeMarker) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        throw new IllegalStateException("No matching close marker found");
    }


    private static int calculateOptimalStripeCount() {
        int cores = Runtime.getRuntime().availableProcessors();
        // Use 2x cores for better concurrency (matches ConcurrentHashMap's DEFAULT_CONCURRENCY_LEVEL approach)
        // Minimum 8 stripes even on low-core systems, maximum 128 for high-core servers
        int stripes = Math.max(8, cores * 2);
        stripes = Math.min(128, stripes);
        // Round up to next power of 2 for efficient bit-masking
        return Integer.highestOneBit(stripes - 1) << 1;
    }

    /**
     * Prints detailed contention statistics for this map's stripe locking system to the logger.
     * <p>This method outputs comprehensive performance monitoring information including:</p>
     * <ul>
     *   <li>Total lock acquisitions and contentions across all operations</li>
     *   <li>Global lock statistics (used during resize operations)</li>
     *   <li>Per-stripe breakdown showing acquisitions, contentions, and contention rates</li>
     *   <li>Analysis of stripe distribution including most/least contended stripes</li>
     *   <li>Count of unused stripes for load balancing assessment</li>
     * </ul>
     * <p>This information is useful for performance tuning and understanding concurrency
     * patterns in high-throughput scenarios. The statistics are logged at INFO level.</p>
     * 
     * @see #STRIPE_COUNT
     */
    public void printContentionStatistics() {
        int totalAcquisitions = totalLockAcquisitions.get();
        int totalContentions = contentionCount.get();
        int globalAcquisitions = globalLockAcquisitions.get();
        int globalContentions = globalLockContentions.get();

        LOG.info("=== MultiKeyMap Contention Statistics ===");
        LOG.info("Total lock acquisitions: " + totalAcquisitions);
        LOG.info("Total contentions: " + totalContentions);

        if (totalAcquisitions > 0) {
            double contentionRate = (double) totalContentions / totalAcquisitions * 100;
            LOG.info(String.format("Overall contention rate: %.2f%%", contentionRate));
        }

        LOG.info("Global lock acquisitions: " + globalAcquisitions);
        LOG.info("Global lock contentions: " + globalContentions);

        LOG.info("Stripe-level statistics:");
        LOG.info("Stripe | Acquisitions | Contentions | Rate");
        LOG.info("-------|-------------|-------------|------");

        for (int i = 0; i < STRIPE_COUNT; i++) {
            int acquisitions = stripeLockAcquisitions[i].get();
            int contentions = stripeLockContention[i].get();
            double rate = acquisitions > 0 ? (double) contentions / acquisitions * 100 : 0.0;

            LOG.info(String.format("%6d | %11d | %11d | %5.2f%%",
                    i, acquisitions, contentions, rate));
        }

        // Find most/least contended stripes
        int maxContentionStripe = 0;
        int minContentionStripe = 0;
        int maxContentions = stripeLockContention[0].get();
        int minContentions = stripeLockContention[0].get();

        for (int i = 1; i < STRIPE_COUNT; i++) {
            int contentions = stripeLockContention[i].get();
            if (contentions > maxContentions) {
                maxContentions = contentions;
                maxContentionStripe = i;
            }
            if (contentions < minContentions) {
                minContentions = contentions;
                minContentionStripe = i;
            }
        }

        LOG.info("Stripe distribution analysis:");
        LOG.info(String.format("Most contended stripe: %d (%d contentions)", maxContentionStripe, maxContentions));
        LOG.info(String.format("Least contended stripe: %d (%d contentions)", minContentionStripe, minContentions));

        // Check for unused stripes
        int unusedStripes = 0;
        for (int i = 0; i < STRIPE_COUNT; i++) {
            if (stripeLockAcquisitions[i].get() == 0) {
                unusedStripes++;
            }
        }
        LOG.info(String.format("Unused stripes: %d out of %d", unusedStripes, STRIPE_COUNT));
        LOG.info("================================================");
    }

    private void withAllStripeLocks(Runnable action) {
        if (trackContentionMetrics) {
            globalLockAcquisitions.incrementAndGet();
        }
        lockStripeRecursive(0, action);
    }

    private void lockStripeRecursive(int index, Runnable action) {
        if (index >= STRIPE_COUNT) {
            action.run();
            return;
        }
        synchronized (stripeMonitors[index]) {
            lockStripeRecursive(index + 1, action);
        }
    }
    
    private static void processNestedStructure(StringBuilder sb, List<Object> list, int[] index, MultiKeyMap<?> selfMap) {
        if (index[0] >= list.size()) return;

        Object element = list.get(index[0]);
        index[0]++;

        if (element == OPEN) {
            // List - use square brackets
            sb.append(EMOJI_OPEN);
            boolean first = true;
            while (index[0] < list.size()) {
                Object next = list.get(index[0]);
                if (next == CLOSE) {
                    index[0]++;
                    sb.append(EMOJI_CLOSE);
                    break;
                }
                if (!first) sb.append(", ");
                first = false;
                processNestedStructure(sb, list, index, selfMap);
            }
        } else if (element == SET_OPEN) {
            // Set - use curly braces
            sb.append(EMOJI_SET_OPEN);
            boolean first = true;
            while (index[0] < list.size()) {
                Object next = list.get(index[0]);
                if (next == SET_CLOSE) {
                    index[0]++;
                    sb.append(EMOJI_SET_CLOSE);
                    break;
                }
                if (!first) sb.append(", ");
                first = false;
                processNestedStructure(sb, list, index, selfMap);
            }
        } else if (element == NULL_SENTINEL) {
            sb.append(EMOJI_EMPTY);
        } else if (selfMap != null && element == selfMap) {
            sb.append(THIS_MAP);
        } else if (element instanceof String && ((String) element).startsWith(EMOJI_CYCLE)) {
            sb.append(element);
        } else {
            sb.append(element);
        }
    }
    
    private static String dumpExpandedKeyStatic(Object key, boolean forToString, MultiKeyMap<?> selfMap) {
        if (key == null) return forToString ? EMOJI_KEY + EMOJI_EMPTY : EMOJI_EMPTY;
        if (key == NULL_SENTINEL) return forToString ? EMOJI_KEY + EMOJI_EMPTY : EMOJI_EMPTY;
        
        // Handle single-element Object[] that contains a Collection (from MultiKeyEntry constructor)
        if (key.getClass().isArray() && ArrayUtilities.getLength(key) == 1) {
            Object element = ArrayUtilities.getElement(key, 0);
            if (element instanceof Collection) {
                return dumpExpandedKeyStatic(element, forToString, selfMap);
            }
        }
        
        if (!(key.getClass().isArray() || key instanceof Collection)) {
            // Handle self-reference in single keys
            if (selfMap != null && key == selfMap) return EMOJI_KEY + THIS_MAP;
            return EMOJI_KEY + key;
        }

        // Special case for toString: use bracket notation for readability
        if (forToString) {
            // Check if this is an already-flattened structure (starts with OPEN sentinel)
            if (key instanceof Collection) {
                Collection<?> coll = (Collection<?>) key;
                // A flattened structure should start with OPEN and end with CLOSE
                boolean isAlreadyFlattened = false;
                if (!coll.isEmpty()) {
                    Object first = coll.iterator().next();
                    if (first == OPEN) {
                        isAlreadyFlattened = true;
                    }
                }
                
                if (isAlreadyFlattened) {
                    // Process already-flattened collection with proper recursive structure
                    StringBuilder sb = new StringBuilder();
                    sb.append(EMOJI_KEY);
                    List<Object> collList = new ArrayList<>(coll);
                    int[] index = {0};
                    // The flattened structure should start with OPEN, so process it directly
                    processNestedStructure(sb, collList, index, selfMap);
                    return sb.toString();
                }
            }
            
            if (key.getClass().isArray()) {
                int len = ArrayUtilities.getLength(key);
                
                // Check if this array is already-flattened (starts with OPEN sentinel)
                boolean isAlreadyFlattenedArray = false;
                if (len > 0) {
                    Object first = ArrayUtilities.getElement(key, 0);
                    if (first == OPEN) {
                        isAlreadyFlattenedArray = true;
                    }
                }
                
                if (isAlreadyFlattenedArray) {
                    // Process already-flattened array with proper recursive structure
                    StringBuilder sb = new StringBuilder();
                    sb.append(EMOJI_KEY);
                    List<Object> arrayList = new ArrayList<>();
                    for (int i = 0; i < len; i++) {
                        arrayList.add(ArrayUtilities.getElement(key, i));
                    }
                    int[] index = {0};
                    // The flattened structure should start with OPEN, so process it directly
                    processNestedStructure(sb, arrayList, index, selfMap);
                    return sb.toString();
                }
                
                if (len == 1) {
                    Object element = ArrayUtilities.getElement(key, 0);
                    if (element == NULL_SENTINEL) return EMOJI_KEY + EMOJI_EMPTY;
                    if (selfMap != null && element == selfMap) return EMOJI_KEY + THIS_MAP;
                    if (element == OPEN) {
                        return EMOJI_KEY + EMOJI_OPEN;
                    } else if (element == CLOSE) {
                        return EMOJI_KEY + EMOJI_CLOSE;
                    } else if (element == SET_OPEN) {
                        return EMOJI_KEY + EMOJI_SET_OPEN;
                    } else if (element == SET_CLOSE) {
                        return EMOJI_KEY + EMOJI_SET_CLOSE;
                    } else {
                        return EMOJI_KEY + (element != null ? element.toString() : EMOJI_EMPTY);
                    }
                } else {
                    // Multi-element array - use bracket notation
                    StringBuilder sb = new StringBuilder();
                    sb.append(EMOJI_KEY).append("[");
                    boolean needsComma = false;
                    for (int i = 0; i < len; i++) {
                        Object element = ArrayUtilities.getElement(key, i);
                        if (element == NULL_SENTINEL) {
                            if (needsComma) sb.append(", ");
                            sb.append(EMOJI_EMPTY);
                            needsComma = true;
                        } else if (element == OPEN) {
                            sb.append(EMOJI_OPEN);
                            needsComma = false;
                        } else if (element == CLOSE) {
                            sb.append(EMOJI_CLOSE);
                            needsComma = true;
                        } else if (element == SET_OPEN) {
                            sb.append(EMOJI_SET_OPEN);
                            needsComma = false;
                        } else if (element == SET_CLOSE) {
                            sb.append(EMOJI_SET_CLOSE);
                            needsComma = true;
                        } else if (selfMap != null && element == selfMap) {
                            if (needsComma) sb.append(", ");
                            sb.append(THIS_MAP);
                            needsComma = true;
                        } else if (element instanceof String && ((String) element).startsWith(EMOJI_CYCLE)) {
                            if (needsComma) sb.append(", ");
                            sb.append(element);
                            needsComma = true;
                        } else {
                            if (needsComma) sb.append(", ");
                            if (element == NULL_SENTINEL) {
                                sb.append(EMOJI_EMPTY);
                            } else if (element == OPEN) {
                                sb.append(EMOJI_OPEN);
                            } else if (element == CLOSE) {
                                sb.append(EMOJI_CLOSE);
                            } else if (element == SET_OPEN) {
                                sb.append(EMOJI_SET_OPEN);
                            } else if (element == SET_CLOSE) {
                                sb.append(EMOJI_SET_CLOSE);
                            } else {
                                sb.append(element != null ? element.toString() : EMOJI_EMPTY);
                            }
                            needsComma = true;
                        }
                    }
                    sb.append("]");
                    return sb.toString();
                }
            } else {
                Collection<?> coll = (Collection<?>) key;
                if (coll.size() == 1) {
                    Object element = coll.iterator().next();
                    if (element == NULL_SENTINEL) {
                        // Use bracket notation for sentinel objects
                        return EMOJI_KEY + "[" + EMOJI_EMPTY + "]";
                    }
                    if (selfMap != null && element == selfMap) return EMOJI_KEY + THIS_MAP;
                    if (element == OPEN) {
                        return EMOJI_KEY + EMOJI_OPEN;
                    } else if (element == CLOSE) {
                        return EMOJI_KEY + EMOJI_CLOSE;
                    } else if (element == SET_OPEN) {
                        return EMOJI_KEY + EMOJI_SET_OPEN;
                    } else if (element == SET_CLOSE) {
                        return EMOJI_KEY + EMOJI_SET_CLOSE;
                    } else {
                        return EMOJI_KEY + (element != null ? element.toString() : EMOJI_EMPTY);
                    }
                } else {
                    // Multi-element collection - use bracket notation
                    StringBuilder sb = new StringBuilder();
                    sb.append(EMOJI_KEY).append("[");
                    boolean needsComma = false;
                    for (Object element : coll) {
                        if (element == NULL_SENTINEL) {
                            if (needsComma) sb.append(", ");
                            sb.append(EMOJI_EMPTY);
                            needsComma = true;
                        } else if (element == OPEN) {
                            sb.append(EMOJI_OPEN);
                            needsComma = false;
                        } else if (element == CLOSE) {
                            sb.append(EMOJI_CLOSE);
                            needsComma = true;
                        } else if (element == SET_OPEN) {
                            sb.append(EMOJI_SET_OPEN);
                            needsComma = false;
                        } else if (element == SET_CLOSE) {
                            sb.append(EMOJI_SET_CLOSE);
                            needsComma = true;
                        } else if (selfMap != null && element == selfMap) {
                            if (needsComma) sb.append(", ");
                            sb.append(THIS_MAP);
                            needsComma = true;
                        } else if (element instanceof String && ((String) element).startsWith(EMOJI_CYCLE)) {
                            if (needsComma) sb.append(", ");
                            sb.append(element);
                            needsComma = true;
                        } else {
                            if (needsComma) sb.append(", ");
                            if (element == NULL_SENTINEL) {
                                sb.append(EMOJI_EMPTY);
                            } else if (element == OPEN) {
                                sb.append(EMOJI_OPEN);
                            } else if (element == CLOSE) {
                                sb.append(EMOJI_CLOSE);
                            } else if (element == SET_OPEN) {
                                sb.append(EMOJI_SET_OPEN);
                            } else if (element == SET_CLOSE) {
                                sb.append(EMOJI_SET_CLOSE);
                            } else {
                                sb.append(element != null ? element.toString() : EMOJI_EMPTY);
                            }
                            needsComma = true;
                        }
                    }
                    sb.append("]");
                    return sb.toString();
                }
            }
        }

        List<Object> expanded = new ArrayList<>();
        IdentitySet<Object> visited = new IdentitySet<>();
        // We don't need the hash for debug output, but the method returns it
        expandAndHash(key, expanded, visited, 1, false, true);  // For debug, always preserve structure (false for flatten, true for caseSensitive)

        StringBuilder sb = new StringBuilder();
        sb.append(EMOJI_KEY);
        int[] index = {0};
        processNestedStructure(sb, expanded, index, selfMap);
        return sb.toString();
    }

    /**
     * Format a value for toString() display, replacing null with ∅ and handling nested structures
     */
    private static String formatValueForToString(Object value, MultiKeyMap<?> selfMap) {
        if (value == null) return EMOJI_EMPTY;
        if (selfMap != null && value == selfMap) return THIS_MAP;
        
        // For collections and arrays, recursively format with ∅ for nulls
        if (value instanceof Collection || value.getClass().isArray()) {
            return formatComplexValueForToString(value, selfMap);
        }
        
        return value.toString();
    }

    /**
     * Format complex values (collections/arrays) with ∅ for nulls while maintaining simple formatting
     */
    private static String formatComplexValueForToString(Object value, MultiKeyMap<?> selfMap) {
        if (value == null) return EMOJI_EMPTY;
        if (selfMap != null && value == selfMap) return THIS_MAP;
        
        if (value.getClass().isArray()) {
            return formatArrayValueForToString(value, selfMap);
        } else if (value instanceof Collection) {
            return formatCollectionValueForToString((Collection<?>) value, selfMap);
        }
        
        return value.toString();
    }
    
    /**
     * Format array values with ∅ for nulls
     */
    private static String formatArrayValueForToString(Object array, MultiKeyMap<?> selfMap) {
        int len = ArrayUtilities.getLength(array);
        if (len == 0) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        
        // Fast path for Object[] - avoid reflection overhead
        if (array instanceof Object[]) {
            Object[] oa = (Object[]) array;
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(", ");
                sb.append(formatValueForToString(oa[i], selfMap));  // Direct array access
            }
        } else {
            // Primitive arrays require reflection for boxing
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(", ");
                Object element = ArrayUtilities.getElement(array, i);  // Uses optimized primitive retrieval
                sb.append(formatValueForToString(element, selfMap));
            }
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * Format collection values with ∅ for nulls  
     */
    private static String formatCollectionValueForToString(Collection<?> collection, MultiKeyMap<?> selfMap) {
        if (collection.isEmpty()) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object element : collection) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(formatValueForToString(element, selfMap));
        }
        sb.append("]");
        return sb.toString();
    }
}
