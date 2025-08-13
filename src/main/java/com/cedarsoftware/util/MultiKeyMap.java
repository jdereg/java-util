package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
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
 *   <li><b>High Performance:</b> Zero-allocation polymorphic storage, MurmurHash3 finalization, and optimized hash computation — no GC/heap pressure for gets in flat cases.</li>
 *   <li><b>Thread-Safe:</b> Lock-free reads with auto-tuned stripe locking that scales with your server cores, similar to ConcurrentHashMap.</li>
 *   <li><b>Map Interface Compatible:</b> Supports single-key operations via the standard Map interface (get()/put() automatically unpack Collections/Arrays into multi-keys).</li>
 *   <li><b>Flexible API:</b> Var-args methods for convenient multi-key operations (getMultiKey()/putMultiKey() with many keys).</li>
 *   <li><b>Smart Collection Handling:</b> Configurable behavior for Collections via {@link CollectionKeyMode} — change the default automatic unpacking capability as needed.</li>
 *   <li><b>N-Dimensional Array Expansion:</b> Nested arrays of any depth are automatically flattened recursively into multi-keys.</li>
 *   <li><b>Cross-Container Equivalence:</b> Arrays and Collections with equivalent structure are treated as identical keys, regardless of container type.</li>
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
 * }</pre>
 *
 * <p>For comprehensive examples and advanced usage patterns, see the user guide documentation.</p>
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
    private static final Object NULL_SENTINEL = new Object() {
        @Override public String toString() { return "∅"; }
        @Override public int hashCode() { return "∅".hashCode(); }
    };

    // Common strings
    private static final String THIS_MAP = "(this Map ♻️)"; // Recycle for cycles

    // Emojis for debug output (professional yet intuitive)
    private static final String EMOJI_OPEN = "[";   // Opening bracket for stepping into dimension  
    private static final String EMOJI_CLOSE = "]"; // Closing bracket for stepping back out of dimension
    private static final String EMOJI_CYCLE = "♻️"; // Recycle for cycles
    private static final String EMOJI_EMPTY = "∅";  // Empty set for null/empty
    private static final String EMOJI_KEY = "🆔 ";   // ID for keys (with space)
    private static final String EMOJI_VALUE = "🟣 "; // Purple circle for values (with space)

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
    private final AtomicInteger atomicSize = new AtomicInteger(0);
    // Diagnostic metric: tracks the maximum chain length seen since map creation (never decreases on remove)
    private final AtomicInteger maxChainLength = new AtomicInteger(0);
    private final int capacity;
    private final float loadFactor;
    private final CollectionKeyMode collectionKeyMode;
    private final boolean flattenDimensions;
    private final boolean simpleKeysMode;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private static final int STRIPE_COUNT = calculateOptimalStripeCount();
    private static final int STRIPE_MASK = STRIPE_COUNT - 1;
    private final ReentrantLock[] stripeLocks = new ReentrantLock[STRIPE_COUNT];

    private static final class MultiKey<V> {
        // Kind constants for fast type-based switching
        static final byte KIND_SINGLE = 0;    // Single object
        static final byte KIND_OBJECT_ARRAY = 1;  // Object[] array
        static final byte KIND_COLLECTION = 2;    // Collection (List, etc.)
        static final byte KIND_PRIMITIVE_ARRAY = 3; // Primitive arrays (int[], etc.)
        
        final Object keys;  // Polymorphic: Object (single), Object[] (flat multi), Collection<?> (nested multi)
        final int hash;
        final V value;
        final int arity;    // Number of keys (1 for single, array.length for arrays, collection.size() for collections)
        final byte kind;    // Type of keys structure (0=single, 1=obj[], 2=collection, 3=prim[])

        // Unified constructor that accepts pre-normalized keys and pre-computed hash
        MultiKey(Object normalizedKeys, int hash, V value) {
            this.keys = normalizedKeys;
            this.hash = hash;
            this.value = value;
            
            // Compute and cache arity and kind for fast operations
            if (normalizedKeys == null) {
                this.arity = 1;
                this.kind = KIND_SINGLE;
            } else {
                Class<?> keyClass = normalizedKeys.getClass();
                if (keyClass.isArray()) {
                    this.arity = Array.getLength(normalizedKeys);
                    // Check if it's a primitive array
                    Class<?> componentType = keyClass.getComponentType();
                    this.kind = (componentType != null && componentType.isPrimitive()) 
                        ? KIND_PRIMITIVE_ARRAY 
                        : KIND_OBJECT_ARRAY;
                } else if (normalizedKeys instanceof Collection) {
                    this.arity = ((Collection<?>) normalizedKeys).size();
                    this.kind = KIND_COLLECTION;
                } else {
                    this.arity = 1;
                    this.kind = KIND_SINGLE;
                }
            }
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

        // Ensure capacity is a power of 2, following HashMap's behavior
        int actualCapacity = tableSizeFor(builder.capacity);
        this.buckets = new AtomicReferenceArray<>(actualCapacity);
        // Store the ACTUAL capacity, not the requested one, to avoid confusion
        this.capacity = actualCapacity;
        this.loadFactor = builder.loadFactor;
        this.collectionKeyMode = builder.collectionKeyMode;
        this.flattenDimensions = builder.flattenDimensions;
        this.simpleKeysMode = builder.simpleKeysMode;

        for (int i = 0; i < STRIPE_COUNT; i++) {
            stripeLocks[i] = new ReentrantLock();
            stripeLockContention[i] = new AtomicInteger(0);
            stripeLockAcquisitions[i] = new AtomicInteger(0);
        }

        if (STRIPE_CONFIG_LOGGED.compareAndSet(false, true) && LOG.isLoggable(Level.INFO)) {
            LOG.info(String.format("MultiKeyMap stripe configuration: %d locks for %d cores",
                    STRIPE_COUNT, Runtime.getRuntime().availableProcessors()));
        }
    }

    // Copy constructor
    public MultiKeyMap(MultiKeyMap<? extends V> source) {
        this(MultiKeyMap.<V>builder().from(source));
        
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
     * </ul>
     */
    public static class Builder<V> {
        private int capacity = 16;
        private float loadFactor = DEFAULT_LOAD_FACTOR;
        private CollectionKeyMode collectionKeyMode = CollectionKeyMode.COLLECTIONS_EXPANDED;
        private boolean flattenDimensions = false;
        private boolean simpleKeysMode = false;

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
    
    private static int computeElementHash(Object key) {
        if (key == null) return 0;
        
        // Use standard hashCode for all types to maintain Map contract
        // Equal objects must have equal hash codes
        return key.hashCode();
    }

    private ReentrantLock getStripeLock(int hash) {
        return stripeLocks[hash & STRIPE_MASK];
    }

    private void lockAllStripes() {
        int contended = 0;
        for (ReentrantLock lock : stripeLocks) {
            // Use tryLock() to accurately detect contention
            if (!lock.tryLock()) {
                contended++;
                lock.lock(); // Now wait for the lock
            }
        }
        globalLockAcquisitions.incrementAndGet();
        if (contended > 0) globalLockContentions.incrementAndGet();
    }

    private void unlockAllStripes() {
        for (int i = stripeLocks.length - 1; i >= 0; i--) {
            stripeLocks[i].unlock();
        }
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
        // Use the unified normalization method
        NormalizedKey normalizedKey = flattenKey(key);
        MultiKey<V> entry = findEntryWithPrecomputedHash(normalizedKey.key, normalizedKey.hash);
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
     * @param key the key to normalize
     * @param value the value (can be null for remove operations)
     * @return a MultiKey object with a normalized key and computed hash
     */
    private MultiKey<V> createMultiKey(Object key, V value) {
        final NormalizedKey normalizedKey = flattenKey(key);
        return new MultiKey<>(normalizedKey.key, normalizedKey.hash, value);
    }

    // Method for when only the hash is needed, not the normalized key
    // Update maxChainLength to the maximum of current value and newValue
    // Uses getAndAccumulate for better performance under contention
    private void updateMaxChainLength(int newValue) {
        maxChainLength.getAndAccumulate(newValue, Math::max);
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
        // Optimized check order for better performance
        // 1. null check first (fastest)
        // 2. instanceof Collection (faster than isArray)
        // 3. isArray check last (requires getClass() call)
        return o instanceof Collection || (o != null && o.getClass().isArray());
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
    private NormalizedKey flattenKey(Object key) {
        
        // Handle null case
        if (key == null) {
            return new NormalizedKey(NULL_SENTINEL, 0);
        }

        Class<?> keyClass = key.getClass();
        boolean isKeyArray = keyClass.isArray();
        
        // === FAST PATH: Simple objects (not arrays or collections) ===
        if (!isKeyArray && !(key instanceof Collection)) {
            return new NormalizedKey(key, computeElementHash(key));
        }
        
        // === FAST PATH: Object[] arrays with length-based optimization ===
        if (keyClass == Object[].class) {
            Object[] array = (Object[]) key;
            
            // In simpleKeysMode, route ALL sizes through optimized methods
            if (simpleKeysMode) {
                switch (array.length) {
                    case 0:
                        return new NormalizedKey(array, 0);
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
                        return new NormalizedKey(array, 0);
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
            int length = Array.getLength(key);
            if (length == 0) {
                return new NormalizedKey(key, 0);
            }
            
            // Each primitive type handled separately with inline loops for maximum performance
            // These return the primitive array directly as the key (no boxing)
            int h = 1;
            
            if (keyClass == int[].class) {
                int[] array = (int[]) key;
                for (int i = 0; i < array.length; i++) {
                    h = h * 31 + Integer.hashCode(array[i]);
                }
                return new NormalizedKey(array, h);
            }
            
            if (keyClass == long[].class) {
                long[] array = (long[]) key;
                for (int i = 0; i < array.length; i++) {
                    h = h * 31 + Long.hashCode(array[i]);
                }
                return new NormalizedKey(array, h);
            }
            
            if (keyClass == double[].class) {
                double[] array = (double[]) key;
                for (int i = 0; i < array.length; i++) {
                    h = h * 31 + Double.hashCode(array[i]);
                }
                return new NormalizedKey(array, h);
            }
            
            if (keyClass == float[].class) {
                float[] array = (float[]) key;
                for (int i = 0; i < array.length; i++) {
                    h = h * 31 + Float.hashCode(array[i]);
                }
                return new NormalizedKey(array, h);
            }
            
            if (keyClass == boolean[].class) {
                boolean[] array = (boolean[]) key;
                for (int i = 0; i < array.length; i++) {
                    h = h * 31 + Boolean.hashCode(array[i]);
                }
                return new NormalizedKey(array, h);
            }
            
            if (keyClass == byte[].class) {
                byte[] array = (byte[]) key;
                for (int i = 0; i < array.length; i++) {
                    h = h * 31 + Byte.hashCode(array[i]);
                }
                return new NormalizedKey(array, h);
            }
            
            if (keyClass == short[].class) {
                short[] array = (short[]) key;
                for (int i = 0; i < array.length; i++) {
                    h = h * 31 + Short.hashCode(array[i]);
                }
                return new NormalizedKey(array, h);
            }
            
            if (keyClass == char[].class) {
                char[] array = (char[]) key;
                for (int i = 0; i < array.length; i++) {
                    h = h * 31 + Character.hashCode(array[i]);
                }
                return new NormalizedKey(array, h);
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
        
        // Handle collections that should not be expanded
        if (collectionKeyMode == CollectionKeyMode.COLLECTIONS_NOT_EXPANDED) {
            return new NormalizedKey(coll, coll.hashCode());
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
                    return new NormalizedKey(ArrayUtilities.EMPTY_OBJECT_ARRAY, 0);
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
                    return new NormalizedKey(ArrayUtilities.EMPTY_OBJECT_ARRAY, 0);
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
    
    private NormalizedKey flattenObjectArray1(Object[] array) {
        Object elem = array[0];
        
        // Simple element - fast path
        if (!isArrayOrCollection(elem)) {
            int hash = 31 + computeElementHash(elem);
            return new NormalizedKey(array, hash);
        }
        
        // Complex element - check flattenDimensions
        if (flattenDimensions) {
            return expandWithHash(array);
        }
        
        // Not flattening - delegate to process1DObjectArray
        return process1DObjectArray(array);
    }
    
    private NormalizedKey flattenObjectArray2(Object[] array) {
        // Optimized unrolled version for size 2
        Object elem0 = array[0];
        Object elem1 = array[1];
        
        if (isArrayOrCollection(elem0) || isArrayOrCollection(elem1)) {
            if (flattenDimensions) return expandWithHash(array);
            return process1DObjectArray(array);
        }
        
        int h = 31 + computeElementHash(elem0);
        h = h * 31 + computeElementHash(elem1);
        return new NormalizedKey(array, h);
    }
    
    private NormalizedKey flattenObjectArray3(Object[] array) {
        // Optimized unrolled version for size 3
        Object elem0 = array[0];
        Object elem1 = array[1];
        Object elem2 = array[2];
        
        if (isArrayOrCollection(elem0) || isArrayOrCollection(elem1) || isArrayOrCollection(elem2)) {
            if (flattenDimensions) return expandWithHash(array);
            return process1DObjectArray(array);
        }
        
        int h = 31 + computeElementHash(elem0);
        h = h * 31 + computeElementHash(elem1);
        h = h * 31 + computeElementHash(elem2);
        return new NormalizedKey(array, h);
    }
    
    private NormalizedKey flattenCollection1(Collection<?> coll) {
        Iterator<?> iter = coll.iterator();
        Object elem = iter.next();
        
        // Simple element - fast path
        if (!isArrayOrCollection(elem)) {
            int hash = 31 + computeElementHash(elem);
            
            // Convert non-RandomAccess to array for consistent lookup
            if (!(coll instanceof RandomAccess)) {
                return new NormalizedKey(new Object[]{elem}, hash);
            }
            return new NormalizedKey(coll, hash);
        }
        
        // Complex element - check flattenDimensions
        if (flattenDimensions) {
            return expandWithHash(coll);
        }
        
        // Not flattening - delegate to process1DCollection
        return process1DCollection(coll);
    }
    
    private NormalizedKey flattenCollection2(Collection<?> coll) {
        // Optimized unrolled version for size 2
        if (coll instanceof RandomAccess) {
            List<?> list = (List<?>) coll;
            Object elem0 = list.get(0);
            Object elem1 = list.get(1);
            
            if (isArrayOrCollection(elem0) || isArrayOrCollection(elem1)) {
                if (flattenDimensions) return expandWithHash(coll);
                return process1DCollection(coll);
            }
            
            int h = 31 + computeElementHash(elem0);
            h = h * 31 + computeElementHash(elem1);
            return new NormalizedKey(coll, h);
        }
        
        // Non-RandomAccess path
        Object[] elements = new Object[2];
        Iterator<?> iter = coll.iterator();
        elements[0] = iter.next();
        elements[1] = iter.next();
        
        if (isArrayOrCollection(elements[0]) || isArrayOrCollection(elements[1])) {
            if (flattenDimensions) return expandWithHash(coll);
            return process1DCollection(coll);
        }
        
        int h = 31 + computeElementHash(elements[0]);
        h = h * 31 + computeElementHash(elements[1]);
        return new NormalizedKey(elements, h);
    }
    
    private NormalizedKey flattenCollection3(Collection<?> coll) {
        // Optimized unrolled version for size 3
        if (coll instanceof RandomAccess) {
            List<?> list = (List<?>) coll;
            Object elem0 = list.get(0);
            Object elem1 = list.get(1);
            Object elem2 = list.get(2);
            
            if (isArrayOrCollection(elem0) || isArrayOrCollection(elem1) || isArrayOrCollection(elem2)) {
                if (flattenDimensions) return expandWithHash(coll);
                return process1DCollection(coll);
            }
            
            int h = 31 + computeElementHash(elem0);
            h = h * 31 + computeElementHash(elem1);
            h = h * 31 + computeElementHash(elem2);
            return new NormalizedKey(coll, h);
        }
        
        // Non-RandomAccess path
        Object[] elements = new Object[3];
        Iterator<?> iter = coll.iterator();
        elements[0] = iter.next();
        elements[1] = iter.next();
        elements[2] = iter.next();
        
        if (isArrayOrCollection(elements[0]) || isArrayOrCollection(elements[1]) || 
            isArrayOrCollection(elements[2])) {
            if (flattenDimensions) return expandWithHash(coll);
            return process1DCollection(coll);
        }
        
        int h = 31 + computeElementHash(elements[0]);
        h = h * 31 + computeElementHash(elements[1]);
        h = h * 31 + computeElementHash(elements[2]);
        return new NormalizedKey(elements, h);
    }

    /**
     * Parameterized version of Object[] flattening for sizes 6-10.
     * Uses loops instead of unrolling to handle any size efficiently.
     */
    private NormalizedKey flattenObjectArrayN(Object[] array, int size) {
        // Single pass: check complexity AND compute hash
        int h = 1;

        if (simpleKeysMode) {
            for (int i = 0; i < size; i++) {
                h = h * 31 + computeElementHash(array[i]);
            }
        } else {
            for (int i = 0; i < size; i++) {
                Object elem = array[i];
                boolean isArrayOrCollection = elem instanceof Collection || (elem != null && elem.getClass().isArray());
                if (isArrayOrCollection) {
                    // Found complex element - bail out
                    if (flattenDimensions) return expandWithHash(array);
                    return process1DObjectArray(array);
                }
                h = h * 31 + computeElementHash(elem);
            }
        }
        
        // All simple - return with computed hash
        return new NormalizedKey(array, h);
    }
    
    /**
     * Parameterized version of collection flattening for sizes 6-10.
     * This version uses loops instead of unrolling to handle any size.
     */
    private NormalizedKey flattenCollectionN(Collection<?> coll, int size) {
        // RandomAccess path - NO HEAP ALLOCATION
        final boolean flattenLocal = flattenDimensions;
        if (coll instanceof RandomAccess) {
            List<?> list = (List<?>) coll;
            
            // Single pass: check complexity AND compute hash
            int h = 1;

            if (simpleKeysMode) {
                for (int i = 0; i < size; i++) {
                    h = h * 31 + computeElementHash(list.get(i));
                }
            } else {
                for (int i = 0; i < size; i++) {
                    Object elem = list.get(i);
                    boolean isArrayOrCollection = elem instanceof Collection || (elem != null && elem.getClass().isArray());
                    if (isArrayOrCollection) {
                        // Found complex element - bail out
                        if (flattenLocal) return expandWithHash(coll);
                        return process1DCollection(coll);
                    }
                    h = h * 31 + computeElementHash(elem);
                }
            }

            // All simple - return with computed hash
            return new NormalizedKey(coll, h);
        }
        
        // Non-RandomAccess path - check complexity first, create array only if needed
        Iterator<?> iter = coll.iterator();
        Object[] elements = new Object[size];
        int h = 1;

        // First pass: check for complex elements without creating array
        for (int i = 0; i < size; i++) {
            Object elem = iter.next();
            boolean isArrayOrCollection = elem instanceof Collection || (elem != null && elem.getClass().isArray());
            if (isArrayOrCollection) {
                // Found complex element - handle immediately without array creation
                if (flattenLocal) return expandWithHash(coll);
                return process1DCollection(coll);
            }
            elements[i] = elem;
            h = h * 31 + computeElementHash(elem);
        }
        
        // All simple - return with computed hash
        return new NormalizedKey(elements, h);
    }

    private NormalizedKey process1DObjectArray(final Object[] array) {
        final int len = array.length;

        if (len == 0) {
            return new NormalizedKey(array, 0);
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
                h = h * 31 + (eClass == String.class || eClass == Integer.class || eClass == Long.class || eClass == Double.class || eClass == Boolean.class ? e.hashCode() : computeElementHash(e));
            }
        }
        
        if (is1D) {
            // No collapse - arrays stay as arrays
            return new NormalizedKey(array, h);
        }
        
        // It's 2D+ - need to expand with hash computation
        return expandWithHash(array);
    }
    
    private NormalizedKey process1DCollection(final Collection<?> coll) {
        if (coll.isEmpty()) {
            // Normalize empty collections to empty array for cross-container equivalence
            return new NormalizedKey(ArrayUtilities.EMPTY_OBJECT_ARRAY, 0);
        }
        
        // Check if truly 1D while computing hash
        int h = 1;
        boolean is1D = true;
        
        // Use type-specific fast paths for optimal performance
        if (coll instanceof RandomAccess && coll instanceof List) {
            List<?> list = (List<?>) coll;
            final int size = list.size();
            // Compute full hash for all elements
            for (int i = 0; i < size; i++) {
                Object e = list.get(i);
                h = h * 31 + computeElementHash(e);
                if (e instanceof Collection || (e != null && e.getClass().isArray())) {
                    is1D = false;
                    break;
                }
            }
        } else {
            // Fallback to explicit iterator for other collection types
            Iterator<?> iter = coll.iterator();
            while (iter.hasNext()) {
                Object e = iter.next();
                // Compute hash for all elements
                h = h * 31 + computeElementHash(e);
                if (e instanceof Collection || (e != null && e.getClass().isArray())) {
                    is1D = false;
                    break;
                }
            }
        }
        
        if (is1D) {
            // No collapse - collections stay as collections
            
            // For non-random-access collections, convert to Object[] for fast indexed access
            // This ensures consistent O(1) element access in keysMatch comparisons
            if (!(coll instanceof RandomAccess)) {
                Object[] array = coll.toArray();
                return new NormalizedKey(array, h);
            }
            
            return new NormalizedKey(coll, h);
        }
        
        // It's 2D+ - need to expand with hash computation
        return expandWithHash(coll);
    }
    
    private NormalizedKey process1DTypedArray(Object arr) {
        Class<?> clazz = arr.getClass();
        
        // Primitive arrays are already handled in flattenKey() and never reach here
        // Only handle common non-primitive array types for optimization
        
        // Handle common Object[] subtypes efficiently (these can't contain nested arrays/collections)
        if (clazz == String[].class || clazz == Integer[].class || clazz == Long[].class || 
            clazz == Double[].class || clazz == Date[].class || clazz == Boolean[].class || 
            clazz == Float[].class || clazz == Short[].class || clazz == Byte[].class || 
            clazz == Character[].class) {
            
            Object[] objArray = (Object[]) arr;
            final int len = objArray.length;
            if (len == 0) {
                return new NormalizedKey(objArray, 0);
            }

            // These array types are always 1D (their elements can't be arrays or collections)
            // Optimized: Direct array access without checks
            int h = 1;
            for (int i = 0; i < len; i++) {
                final Object o = objArray[i];
                h = h * 31 + (o == null ? 0 : o.hashCode());
            }

            // No collapse - arrays stay as arrays
            return new NormalizedKey(objArray, h);
        }
        
        // Fallback to reflection for other array types
        return process1DGenericArray(arr);
    }

    private NormalizedKey process1DGenericArray(Object arr) {
        // Fallback method using reflection for uncommon array types
        final int len = Array.getLength(arr);
        if (len == 0) {
            return new NormalizedKey(arr, 0);
        }
        
        // Check if truly 1D while computing full hash (same as process1DObjectArray)
        int h = 1;
        boolean is1D = true;
        
        // Compute full hash for all elements
        for (int i = 0; i < len; i++) {
            Object e = Array.get(arr, i);
            h = h * 31 + computeElementHash(e);
            if (e instanceof Collection || (e != null && e.getClass().isArray())) {
                is1D = false;
                break;
            }
        }
        
        if (is1D) {
            // No collapse - arrays stay as arrays
            return new NormalizedKey(arr, h);
        }
        
        // It's 2D+ - need to expand with hash computation
        return expandWithHash(arr);
    }
    
    private NormalizedKey expandWithHash(Object key) {
        // Pre-size the expanded list based on heuristic:
        // - Arrays/Collections typically expand to their size + potential nesting markers
        // - Default to 8 for unknown types (better than ArrayList's default 10 for small keys)
        int estimatedSize = 8;
        if (key != null) {
            if (key.getClass().isArray()) {
                int len = Array.getLength(key);
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
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        
        int hash = expandAndHash(key, expanded, visited, 1, flattenDimensions);
        
        // NO COLLAPSE - expanded results stay as lists
        // Even single-element expanded results remain as lists to maintain consistency
        // [x] should never become x
        
        return new NormalizedKey(expanded, hash);
    }
    
    private static int expandAndHash(Object current, List<Object> result, IdentityHashMap<Object, Boolean> visited, 
                                      int runningHash, boolean useFlatten) {
        if (current == null) {
            result.add(NULL_SENTINEL);
            return runningHash * 31 + NULL_SENTINEL.hashCode();
        }

        if (visited.containsKey(current)) {
            Object cycle = EMOJI_CYCLE + System.identityHashCode(current);
            result.add(cycle);
            return runningHash * 31 + cycle.hashCode();
        }

        if (current.getClass().isArray()) {
            visited.put(current, true);
            try {
                if (!useFlatten) {
                    result.add(OPEN);
                    runningHash = runningHash * 31 + OPEN.hashCode();
                }
                int len = Array.getLength(current);
                for (int i = 0; i < len; i++) {
                    runningHash = expandAndHash(Array.get(current, i), result, visited, runningHash, useFlatten);
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
            visited.put(current, true);
            try {
                if (!useFlatten) {
                    result.add(OPEN);
                    runningHash = runningHash * 31 + OPEN.hashCode();
                }
                for (Object e : coll) {
                    runningHash = expandAndHash(e, result, visited, runningHash, useFlatten);
                }
                if (!useFlatten) {
                    result.add(CLOSE);
                    runningHash = runningHash * 31 + CLOSE.hashCode();
                }
            } finally {
                visited.remove(current);
            }
        } else {
            result.add(current);
            runningHash = runningHash * 31 + computeElementHash(current);
        }
        return runningHash;
    }
    
    /**
     * Optimized findEntry that skips the flattenKey() call when we already have
     * the normalized key and precomputed hash. This is the core of informed handoff optimization.
     */
    private MultiKey<V> findEntryWithPrecomputedHash(final Object normalizedKey, final int hash) {
        final AtomicReferenceArray<MultiKey<V>[]> table = buckets;  // Pin table reference
        final int index = hash & (table.length() - 1);
        final MultiKey<V>[] chain = table.get(index);
        if (chain == null) return null;
        final int chLen = chain.length;
        for (int i = 0; i < chLen; i++) {
            MultiKey<V> entry = chain[i];
            if (entry.hash == hash && keysMatch(entry, normalizedKey)) return entry;
        }
        return null;
    }

    /**
     * Optimized keysMatch that leverages MultiKey's precomputed arity and kind.
     * This is used when we have access to the stored MultiKey object.
     */
    private static <V> boolean keysMatch(MultiKey<V> stored, Object lookup) {
        // Fast identity check
        if (stored.keys == lookup) return true;
        if (stored.keys == null || lookup == null) return false;

        // Multi-key case - use precomputed kind for fast switching
        final Class<?> lookupClass = lookup.getClass();

        // Early arity rejection - if stored has precomputed arity, check it first
        if (stored.kind == MultiKey.KIND_SINGLE) {
            // Single key optimization
            if (lookupClass.isArray() || lookup instanceof Collection) {
                return false; // Collection/array not single element
            }
            return Objects.equals(stored.keys == NULL_SENTINEL ? null : stored.keys, 
                                 lookup == NULL_SENTINEL ? null : lookup);
        }

        // Check arity match first (early rejection)
        final int lookupArity;
        final byte lookupKind;
        
        if (lookupClass.isArray()) {
            lookupArity = Array.getLength(lookup);
            Class<?> componentType = lookupClass.getComponentType();
            lookupKind = (componentType != null && componentType.isPrimitive()) 
                ? MultiKey.KIND_PRIMITIVE_ARRAY 
                : MultiKey.KIND_OBJECT_ARRAY;
        } else if (lookup instanceof Collection) {
            lookupArity = ((Collection<?>) lookup).size();
            lookupKind = MultiKey.KIND_COLLECTION;
        } else {
            // Lookup is single but stored is multi
            return false;
        }
        
        // Early rejection on arity mismatch
        if (stored.arity != lookupArity) return false;
        final Class<?> storeKeysClass = stored.keys.getClass();
        
        // Now use kind-based fast paths
        switch (stored.kind) {
            case MultiKey.KIND_OBJECT_ARRAY:
                if (lookupKind == MultiKey.KIND_OBJECT_ARRAY) {
                    return Arrays.equals((Object[]) stored.keys, (Object[]) lookup);
                }
                // Fall through to cross-type comparison
                break;
                
            case MultiKey.KIND_COLLECTION:
                if (lookupKind == MultiKey.KIND_COLLECTION && storeKeysClass == lookupClass) {
                    // Same collection type - use built-in equals() method
                    return stored.keys.equals(lookup);
                }
                // Fall through to cross-type comparison
                break;
                
            case MultiKey.KIND_PRIMITIVE_ARRAY:
                if (lookupKind == MultiKey.KIND_PRIMITIVE_ARRAY && storeKeysClass == lookupClass) {
                    // Same primitive array type - use specialized equals
                    if (storeKeysClass == int[].class) return Arrays.equals((int[]) stored.keys, (int[]) lookup);
                    if (storeKeysClass == long[].class) return Arrays.equals((long[]) stored.keys, (long[]) lookup);
                    if (storeKeysClass == double[].class) return Arrays.equals((double[]) stored.keys, (double[]) lookup);
                    if (storeKeysClass == boolean[].class) return Arrays.equals((boolean[]) stored.keys, (boolean[]) lookup);
                    if (storeKeysClass == byte[].class) return Arrays.equals((byte[]) stored.keys, (byte[]) lookup);
                    if (storeKeysClass == char[].class) return Arrays.equals((char[]) stored.keys, (char[]) lookup);
                    if (storeKeysClass == float[].class) return Arrays.equals((float[]) stored.keys, (float[]) lookup);
                    if (storeKeysClass == short[].class) return Arrays.equals((short[]) stored.keys, (short[]) lookup);
                }
                // Fall through to cross-type comparison
                break;
        }
        
        // Cross-type comparison or fallback to element-wise
        return keysMatchCrossType(stored.keys, lookup, stored.arity, stored.kind, lookupKind);
    }
    
    /**
     * Helper for cross-type comparisons when stored and lookup have different container types.
     * Uses precomputed kind to avoid instanceof checks.
     */
    private static boolean keysMatchCrossType(Object stored, Object lookup, int arity, byte storedKind, byte lookupKind) {
        // Convert to iterators for uniform comparison - no instanceof needed!
        final Iterator<?> storedIter = (storedKind == MultiKey.KIND_COLLECTION)
            ? ((Collection<?>) stored).iterator()
            : new ArrayIterator(stored);
        final Iterator<?> lookupIter = (lookupKind == MultiKey.KIND_COLLECTION)
            ? ((Collection<?>) lookup).iterator()
            : new ArrayIterator(lookup);
            
        for (int i = 0; i < arity; i++) {
            if (!Objects.equals(storedIter.next(), lookupIter.next())) {
                return false;
            }
        }
        return true;
    }
    
    private static class ArrayIterator implements Iterator<Object> {
        private final Object array;
        private final int len;
        private int index = 0;

        ArrayIterator(Object array) {
            this.array = array;
            this.len = Array.getLength(array);
        }

        @Override
        public boolean hasNext() {
            return index < len;
        }

        @Override
        public Object next() {
            return Array.get(array, index++);
        }
    }

    private V putInternal(MultiKey<V> newKey) {
        int hash = newKey.hash;
        ReentrantLock lock = getStripeLock(hash);
        int stripe = hash & STRIPE_MASK;
        V old;
        boolean resize;

        // Use tryLock() to accurately detect contention
        boolean contended = !lock.tryLock();
        if (contended) {
            // Failed to acquire immediately - this is true contention
            lock.lock(); // Now wait for the lock
            contentionCount.incrementAndGet();
            stripeLockContention[stripe].incrementAndGet();
        }
        
        try {
            totalLockAcquisitions.incrementAndGet();
            stripeLockAcquisitions[stripe].incrementAndGet();

            old = putNoLock(newKey);
            resize = atomicSize.get() > buckets.length() * loadFactor;
        } finally {
            lock.unlock();
        }

        resizeRequest(resize);

        return old;
    }

    private V getNoLock(MultiKey<V> lookupKey) {
        int hash = lookupKey.hash;
        final AtomicReferenceArray<MultiKey<V>[]> table = buckets;  // Pin table reference
        int index = hash & (table.length() - 1);
        MultiKey<V>[] chain = table.get(index);
        
        if (chain == null) return null;
        
        for (MultiKey<V> e : chain) {
            if (e.hash == hash && keysMatch(e, lookupKey.keys)) {
                return e.value;
            }
        }
        return null;
    }
    
    private V putNoLock(MultiKey<V> newKey) {
        int hash = newKey.hash;
        final AtomicReferenceArray<MultiKey<V>[]> table = buckets;  // Pin table reference
        int index = hash & (table.length() - 1);
        MultiKey<V>[] chain = table.get(index);

        if (chain == null) {
            buckets.set(index, new MultiKey[]{newKey});
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
                buckets.set(index, newChain);
                return old;
            }
        }

        MultiKey<V>[] newChain = Arrays.copyOf(chain, chain.length + 1);
        newChain[chain.length] = newKey;
        buckets.set(index, newChain);
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
     * Returns {@code true} if this map contains a mapping for the specified key.
     * <p>This method supports both single keys and multidimensional keys. Arrays and Collections
     * are automatically expanded into multi-keys based on the map's configuration settings.</p>
     * 
     * @param key the key whose presence in this map is to be tested. Can be a single object,
     *            array, or Collection that will be normalized according to the map's settings
     * @return {@code true} if this map contains a mapping for the specified key
     */
    public boolean containsKey(Object key) {
        // Use the unified normalization method
        NormalizedKey normalizedKey = flattenKey(key);
        MultiKey<V> entry = findEntryWithPrecomputedHash(normalizedKey.key, normalizedKey.hash);
        return entry != null;
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
        int hash = removeKey.hash;
        ReentrantLock lock = getStripeLock(hash);
        int stripe = hash & STRIPE_MASK;
        V old;

        // Use tryLock() to accurately detect contention
        boolean contended = !lock.tryLock();
        if (contended) {
            // Failed to acquire immediately - this is true contention
            lock.lock(); // Now wait for the lock
            contentionCount.incrementAndGet();
            stripeLockContention[stripe].incrementAndGet();
        }
        
        try {
            totalLockAcquisitions.incrementAndGet();
            stripeLockAcquisitions[stripe].incrementAndGet();

            old = removeNoLock(removeKey);
        } finally {
            lock.unlock();
        }

        return old;
    }

    private V removeNoLock(MultiKey<V> removeKey) {
        int hash = removeKey.hash;
        final AtomicReferenceArray<MultiKey<V>[]> table = buckets;  // Pin table reference
        int index = hash & (table.length() - 1);
        MultiKey<V>[] chain = table.get(index);

        if (chain == null) return null;

        for (int i = 0; i < chain.length; i++) {
            MultiKey<V> e = chain[i];
            if (e.hash == hash && keysMatch(e, removeKey.keys)) {
                V old = e.value;
                if (chain.length == 1) {
                    buckets.set(index, null);
                } else {
                    // Create new array without the removed element - never mutate published array
                    MultiKey<V>[] newChain = new MultiKey[chain.length - 1];
                    // Copy elements before the removed one
                    System.arraycopy(chain, 0, newChain, 0, i);
                    // Copy elements after the removed one
                    System.arraycopy(chain, i + 1, newChain, i, chain.length - i - 1);
                    buckets.set(index, newChain);
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
        });
    }

    private int rehashEntry(MultiKey<V> entry, AtomicReferenceArray<MultiKey<V>[]> target) {
        int index = entry.hash & (target.length() - 1);
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
     * 
     * @param resize whether to perform resize
     */
    private void resizeRequest(boolean resize) {
        if (resize && resizeInProgress.compareAndSet(false, true)) {
            try { 
                resizeInternal(); 
            } finally { 
                resizeInProgress.set(false); 
            }
        }
    }

    /**
     * Returns the number of key-value mappings in this map.
     * 
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return atomicSize.get();
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     * 
     * @return {@code true} if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Removes all the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
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
     * Helper method to create an immutable view of multi-key arrays.
     * This ensures external code cannot mutate our internal key arrays.
     */
    private static List<Object> keyView(Object[] keys) {
        return Collections.unmodifiableList(Arrays.asList(keys));
    }
    
    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * <p>Multidimensional keys are represented as immutable List<Object>, while single keys
     * are returned as their original objects. Changes to the returned set are not
     * reflected in the map.</p>
     * 
     * @return a set view of the keys contained in this map
     */
    public Set<Object> keySet() {
        Set<Object> set = new HashSet<>();
        for (MultiKeyEntry<V> e : entries()) {
            if (e.keys.length == 1) {
                // Single key case
                set.add(e.keys[0] == NULL_SENTINEL ? null : e.keys[0]);
            } else {
                // Multi-key case: expose as immutable List for proper equals/hashCode behavior
                // and to prevent mutation of internal arrays
                set.add(keyView(e.keys));
            }
        }
        return set;
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * <p>Changes to the returned collection are not reflected in the map.</p>
     * 
     * @return a collection view of the values contained in this map
     */
    public Collection<V> values() {
        List<V> vals = new ArrayList<>();
        for (MultiKeyEntry<V> e : entries()) vals.add(e.value);
        return vals;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * <p>Multidimensional keys are represented as immutable List<Object>, while single keys
     * are returned as their original objects. Changes to the returned set are not
     * reflected in the map.</p>
     * 
     * @return a set view of the mappings contained in this map
     */
    public Set<Map.Entry<Object, V>> entrySet() {
        Set<Map.Entry<Object, V>> set = new HashSet<>();
        for (MultiKeyEntry<V> e : entries()) {
            Object k = e.keys.length == 1 ? (e.keys[0] == NULL_SENTINEL ? null : e.keys[0]) : keyView(e.keys);
            set.add(new AbstractMap.SimpleEntry<>(k, e.value));
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
        NormalizedKey norm = flattenKey(key);
        Object normalizedKey = norm.key;
        int hash = norm.hash;
        ReentrantLock lock = getStripeLock(hash);
        boolean resize = false;
        
        lock.lock();
        try {
            // Check again inside the lock
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            existing = getNoLock(lookupKey);
            if (existing == null) {
                // Use putNoLock directly to avoid double locking
                MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, value);
                putNoLock(newKey);
                resize = atomicSize.get() > buckets.length() * loadFactor;
            }
        } finally {
            lock.unlock();
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
        V v = get(key);
        if (v != null) return v;
        
        NormalizedKey norm = flattenKey(key);
        Object normalizedKey = norm.key;
        int hash = norm.hash;
        ReentrantLock lock = getStripeLock(hash);
        boolean resize = false;
        
        lock.lock();
        try {
            // Create lookup key for checking existence
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            v = getNoLock(lookupKey);
            if (v == null) {
                v = mappingFunction.apply(key);
                if (v != null) {
                    // Create new key with value and use putNoLock
                    MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, v);
                    putNoLock(newKey);
                    resize = atomicSize.get() > buckets.length() * loadFactor;
                }
            }
        } finally {                  
            lock.unlock();
        }
        // Handle resize outside the lock
        resizeRequest(resize);
        return v;
    }

    /**
     * If the value for the specified key is present, attempts to compute a new mapping
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
        V old = get(key);
        if (old == null) return null;
        
        NormalizedKey norm = flattenKey(key);
        Object normalizedKey = norm.key;
        int hash = norm.hash;
        ReentrantLock lock = getStripeLock(hash);
        boolean resize = false;
        
        V result = null;
        lock.lock();
        try {
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            old = getNoLock(lookupKey);
            if (old != null) {
                V newV = remappingFunction.apply(key, old);
                if (newV != null) {
                    // Replace with new value using putNoLock
                    MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, newV);
                    putNoLock(newKey);
                    resize = atomicSize.get() > buckets.length() * loadFactor;
                    result = newV;
                } else {
                    // Remove using removeNoLock
                    MultiKey<V> removeKey = new MultiKey<>(normalizedKey, hash, old);
                    removeNoLock(removeKey);
                }
            }
        } finally {
            lock.unlock();
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
        
        NormalizedKey norm = flattenKey(key);
        Object normalizedKey = norm.key;
        int hash = norm.hash;
        ReentrantLock lock = getStripeLock(hash);
        boolean resize = false;
        
        V result;
        lock.lock();
        try {
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            V old = getNoLock(lookupKey);
            V newV = remappingFunction.apply(key, old);
            
            if (newV == null) {
                // Check if key existed (even with null value) and remove if so
                if (old != null || findEntryWithPrecomputedHash(normalizedKey, hash) != null) {
                    MultiKey<V> removeKey = new MultiKey<>(normalizedKey, hash, old);
                    removeNoLock(removeKey);
                }
                result = null;
            } else {
                // Put new value using putNoLock
                MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, newV);
                putNoLock(newKey);
                resize = atomicSize.get() > buckets.length() * loadFactor;
                result = newV;
            }
        } finally {
            lock.unlock();
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
        
        NormalizedKey norm = flattenKey(key);
        Object normalizedKey = norm.key;
        int hash = norm.hash;
        ReentrantLock lock = getStripeLock(hash);
        boolean resize = false;
        
        V result;
        lock.lock();
        try {
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
                resize = atomicSize.get() > buckets.length() * loadFactor;
            }
            result = newV;
        } finally {
            lock.unlock();
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
     * @param key the key with which the specified value is associated
     * @param value the value expected to be associated with the specified key
     * @return {@code true} if the value was removed
     */
    public boolean remove(Object key, Object value) {
        NormalizedKey norm = flattenKey(key);
        Object normalizedKey = norm.key;
        int hash = norm.hash;
        ReentrantLock lock = getStripeLock(hash);
        
        lock.lock();
        try {
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            V current = getNoLock(lookupKey);
            if (!Objects.equals(current, value)) return false;
            
            // Remove using removeNoLock
            MultiKey<V> removeKey = new MultiKey<>(normalizedKey, hash, current);
            removeNoLock(removeKey);
            return true;
        } finally {
            lock.unlock();
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
     * @param key the key with which the specified value is associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null}
     *         if there was no mapping for the key
     */
    public V replace(Object key, V value) {
        NormalizedKey norm = flattenKey(key);
        Object normalizedKey = norm.key;
        int hash = norm.hash;
        ReentrantLock lock = getStripeLock(hash);
        boolean resize = false;
        
        V result;
        lock.lock();
        try {
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            V old = getNoLock(lookupKey);
            if (old == null && findEntryWithPrecomputedHash(normalizedKey, hash) == null) {
                result = null; // Key doesn't exist
            } else {
                // Replace with new value using putNoLock
                MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, value);
                result = putNoLock(newKey);
                resize = atomicSize.get() > buckets.length() * loadFactor;
            }
        } finally {
            lock.unlock();
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
     * @param key the key with which the specified value is associated
     * @param oldValue the value expected to be associated with the specified key
     * @param newValue the value to be associated with the specified key
     * @return {@code true} if the value was replaced
     */
    public boolean replace(Object key, V oldValue, V newValue) {
        NormalizedKey norm = flattenKey(key);
        Object normalizedKey = norm.key;
        int hash = norm.hash;
        ReentrantLock lock = getStripeLock(hash);
        boolean resize = false;
        
        boolean result = false;
        lock.lock();
        try {
            MultiKey<V> lookupKey = new MultiKey<>(normalizedKey, hash, null);
            V current = getNoLock(lookupKey);
            if (Objects.equals(current, oldValue)) {
                // Replace with new value using putNoLock
                MultiKey<V> newKey = new MultiKey<>(normalizedKey, hash, newValue);
                putNoLock(newKey);
                resize = atomicSize.get() > buckets.length() * loadFactor;
                result = true;
            }
        } finally {
            lock.unlock();
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
        int h = 0;
        for (MultiKeyEntry<V> e : entries()) {
            Object k = e.keys.length == 1 ? (e.keys[0] == NULL_SENTINEL ? null : e.keys[0]) : keyView(e.keys);
            h += Objects.hashCode(k) ^ Objects.hashCode(e.value);
        }
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
        for (MultiKeyEntry<V> e : entries()) {
            Object k = e.keys.length == 1 ? (e.keys[0] == NULL_SENTINEL ? null : e.keys[0]) : keyView(e.keys);
            V v = e.value;
            Object mv = m.get(k);
            if (!Objects.equals(v, mv) || (v == null && !m.containsKey(k))) return false;
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
        for (MultiKeyEntry<V> e : entries()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("  ");  // Two-space indentation
            String keyStr = dumpExpandedKeyStatic(e.keys, true, this);
            // Remove trailing comma and space if present
            if (keyStr.endsWith(", ")) {
                keyStr = keyStr.substring(0, keyStr.length() - 2);
            }
            sb.append(keyStr).append(" → ");
            sb.append(EMOJI_VALUE);
            sb.append(formatValueForToString(e.value, this));
        }
        return sb.append("\n}").toString();
    }

    /**
     * Returns an {@link Iterable} of {@link MultiKeyEntry} objects representing all key-value
     * mappings in this map.
     * <p>Each {@code MultiKeyEntry} contains the complete key information as an Object array
     * and the associated value. This provides access to the full multidimensional key structure
     * that may not be available through the standard {@link #entrySet()} method.</p>
     * <p>The returned iterable provides a <b>weakly consistent</b> view - it captures the buckets
     * reference at creation time and walks live bucket elements. Concurrent modifications may or may
     * not be reflected during iteration, and the iterator will never throw ConcurrentModificationException.</p>
     * 
     * @return an iterable of {@code MultiKeyEntry} objects containing all mappings in this map
     * @see MultiKeyEntry
     * @see #entrySet()
     */
    public Iterable<MultiKeyEntry<V>> entries() {
        return EntryIterator::new;
    }

    /**
     * Normalized key with hash - eliminates int[] allocation overhead.
     * This small record is often scalar-replaced by C2 compiler, avoiding heap allocation.
     */
    static final class NormalizedKey {
        final Object key;
        final int hash;
        
        NormalizedKey(Object key, int hash) {
            this.key = key;
            this.hash = hash;
        }
    }

    public static class MultiKeyEntry<V> {
        public final Object[] keys;
        public final V value;

        MultiKeyEntry(Object k, V v) {
            // Canonicalize to Object[] for consistent external presentation
            if (k instanceof Object[]) {
                keys = (Object[]) k;
            } else if (k instanceof Collection) {
                // Convert internal List representation back to Object[] for API consistency
                keys = ((Collection<?>) k).toArray();
            } else {
                keys = new Object[]{k};
            }
            value = v;
        }
    }

    private class EntryIterator implements Iterator<MultiKeyEntry<V>> {
        private final AtomicReferenceArray<MultiKey<V>[]> snapshot = buckets;
        private int bucketIdx = 0;
        private int chainIdx = 0;
        private MultiKeyEntry<V> next;

        EntryIterator() {
            advance();
        }

        public boolean hasNext() {
            return next != null;
        }

        public MultiKeyEntry<V> next() {
            if (next == null) throw new NoSuchElementException();
            MultiKeyEntry<V> current = next;
            advance();
            return current;
        }

        private void advance() {
            while (bucketIdx < snapshot.length()) {
                MultiKey<V>[] chain = snapshot.get(bucketIdx);
                if (chain != null && chainIdx < chain.length) {
                    MultiKey<V> e = chain[chainIdx++];
                    next = new MultiKeyEntry<>(e.keys, e.value);
                    return;
                }
                bucketIdx++;
                chainIdx = 0;
            }
            next = null;
        }
    }

    private static int calculateOptimalStripeCount() {
        int cores = Runtime.getRuntime().availableProcessors();
        int stripes = Math.max(8, cores / 2);
        stripes = Math.min(32, stripes);
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
        lockAllStripes();
        try {
            action.run();
        } finally {
            unlockAllStripes();
        }
    }
    
    private static void processNestedStructure(StringBuilder sb, List<Object> list, int[] index, MultiKeyMap<?> selfMap) {
        if (index[0] >= list.size()) return;
        
        Object element = list.get(index[0]++);
        
        if (element == OPEN) {
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
        if (key.getClass().isArray() && Array.getLength(key) == 1) {
            Object element = Array.get(key, 0);
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
                int len = Array.getLength(key);
                
                // Check if this array is already-flattened (starts with OPEN sentinel)
                boolean isAlreadyFlattenedArray = false;
                if (len > 0) {
                    Object first = Array.get(key, 0);
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
                        arrayList.add(Array.get(key, i));
                    }
                    int[] index = {0};
                    // The flattened structure should start with OPEN, so process it directly
                    processNestedStructure(sb, arrayList, index, selfMap);
                    return sb.toString();
                }
                
                if (len == 1) {
                    Object element = Array.get(key, 0);
                    if (element == NULL_SENTINEL) return EMOJI_KEY + EMOJI_EMPTY;
                    if (selfMap != null && element == selfMap) return EMOJI_KEY + THIS_MAP;
                    if (element == OPEN) {
                        return EMOJI_KEY + EMOJI_OPEN;
                    } else if (element == CLOSE) {
                        return EMOJI_KEY + EMOJI_CLOSE;
                    } else {
                        return EMOJI_KEY + (element != null ? element.toString() : EMOJI_EMPTY);
                    }
                } else {
                    // Multi-element array - use bracket notation
                    StringBuilder sb = new StringBuilder();
                    sb.append(EMOJI_KEY).append("[");
                    boolean needsComma = false;
                    for (int i = 0; i < len; i++) {
                        Object element = Array.get(key, i);
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
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        // We don't need the hash for debug output, but the method returns it
        expandAndHash(key, expanded, visited, 1, false);  // For debug, always preserve structure (false for flatten)

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
        int len = Array.getLength(array);
        if (len == 0) return "[]";
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append(", ");
            Object element = Array.get(array, i);
            sb.append(formatValueForToString(element, selfMap));
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