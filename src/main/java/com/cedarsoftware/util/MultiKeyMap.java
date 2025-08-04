package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-performance N-dimensional key-value Map implementation - the definitive solution for multi-dimensional lookups.
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
 *   <li><b>N-Dimensional Array Expansion:</b> Nested arrays of any depth are automatically flattened recursively into multi-keys, with configurable structure preservation.</li>
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
 *   <li><b>MurmurHash3 Finalization:</b> Enhanced hash distribution reduces collisions</li>
 *   <li><b>Polymorphic Storage:</b> Efficient memory usage adapts storage format based on key complexity</li>
 * </ul>
 *
 * <h3>API Overview:</h3>
 * <p>MultiKeyMap provides two complementary APIs:</p>
 * <ul>
 *   <li><b>Map Interface:</b> Use as {@code Map<Object, V>} for compatibility with existing code and single-key operations</li>
 *   <li><b>MultiKeyMap API:</b> Declare as {@code MultiKeyMap<V>} to access powerful var-args methods for multi-dimensional operations</li>
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
 * MultiKeyMap<String> structured = new MultiKeyMap<>(false); // Structure-preserving (default)
 * MultiKeyMap<String> flattened = new MultiKeyMap<>(true);   // Dimension-flattening
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
         * rather than being expanded into multi-dimensional keys.
         */
        COLLECTIONS_NOT_EXPANDED
    }

    private volatile Object[] buckets;
    private final AtomicInteger atomicSize = new AtomicInteger(0);
    private final AtomicInteger maxChainLength = new AtomicInteger(0);
    private final int capacity;
    private final float loadFactor;
    private final CollectionKeyMode collectionKeyMode;
    private final boolean flattenDimensions;
    private final boolean defensiveCopies;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private static final int STRIPE_COUNT = calculateOptimalStripeCount();
    private static final int STRIPE_MASK = STRIPE_COUNT - 1;
    private final ReentrantLock[] stripeLocks = new ReentrantLock[STRIPE_COUNT];

    private static final class MultiKey<V> {
        final Object keys;  // Polymorphic: Object (single), Object[] (flat multi), Collection<?> (nested multi)
        final int hash;
        final V value;

        // Unified constructor that accepts pre-normalized keys and pre-computed hash
        MultiKey(Object normalizedKeys, int hash, V value) {
            this.keys = normalizedKeys;
            this.hash = hash;
            this.value = value;
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
        this.buckets = new Object[actualCapacity];
        this.capacity = builder.capacity;
        this.loadFactor = builder.loadFactor;
        this.collectionKeyMode = builder.collectionKeyMode;
        this.flattenDimensions = builder.flattenDimensions;
        this.defensiveCopies = builder.defensiveCopies;

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
        
        // Deep-copy entries (respect defensiveCopies)
        source.withAllStripeLocks(() -> {  // Lock for consistent snapshot
            for (Object b : source.buckets) {
                if (b != null) {
                    @SuppressWarnings("unchecked")
                    MultiKey<? extends V>[] chain = (MultiKey<? extends V>[]) b;
                    for (MultiKey<? extends V> entry : chain) {
                        if (entry != null) {
                            // Re-create MultiKey with potentially copied keys
                            Object copiedKeys = copyKeysIfNeeded(entry.keys, this.defensiveCopies);
                            V value = entry.value;
                            // Create new MultiKey and use internal put to avoid double-copying
                            MultiKey<V> newKey = new MultiKey<>(copiedKeys, entry.hash, value);
                            putInternal(newKey);
                        }
                    }
                }
            }
        });
    }

    // Optimized copyKeysIfNeeded with cross-type support
    private static Object copyKeysIfNeeded(Object keys, boolean defensive) {
        if (!defensive) return keys;

        if (keys instanceof Object[]) {
            // Deep copy array
            Object[] arr = (Object[]) keys;
            Object[] copy = new Object[arr.length];
            System.arraycopy(arr, 0, copy, 0, arr.length);
            // Recurse for nested
            for (int i = 0; i < copy.length; i++) {
                copy[i] = copyKeysIfNeeded(copy[i], true);
            }
            return copy;
        } else if (keys instanceof Collection<?>) {
            // Deep copy collection - use fast path for ArrayList
            Collection<?> coll = (Collection<?>) keys;
            if (coll instanceof ArrayList) {
                ArrayList<?> src = (ArrayList<?>) coll;
                ArrayList<Object> copy = new ArrayList<>(src.size());
                for (int i = 0; i < src.size(); i++) {
                    copy.add(copyKeysIfNeeded(src.get(i), true));
                }
                return copy;
            }
            // Fallback for other collections
            ArrayList<Object> copy = new ArrayList<>(coll.size());
            for (Object item : coll) {
                copy.add(copyKeysIfNeeded(item, true));
            }
            return copy;
        }
        return keys;  // No copy needed
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
    
    // Constructor for common test usage patterns
    public MultiKeyMap(CollectionKeyMode collectionKeyMode, boolean flattenDimensions) {
        this(MultiKeyMap.<V>builder()
                .collectionKeyMode(collectionKeyMode)
                .flattenDimensions(flattenDimensions));
    }
    
    public MultiKeyMap(boolean flattenDimensions) {
        this(MultiKeyMap.<V>builder().flattenDimensions(flattenDimensions));
    }

    // Builder class
    public static class Builder<V> {
        private int capacity = 16;
        private float loadFactor = DEFAULT_LOAD_FACTOR;
        private CollectionKeyMode collectionKeyMode = CollectionKeyMode.COLLECTIONS_EXPANDED;
        private boolean flattenDimensions = false;
        private boolean defensiveCopies = true;

        // Private constructor - instantiate via MultiKeyMap.builder()
        private Builder() {}

        public Builder<V> capacity(int capacity) {
            if (capacity < 0) {
                throw new IllegalArgumentException("Capacity must be non-negative");
            }
            this.capacity = capacity;
            return this;
        }

        public Builder<V> loadFactor(float loadFactor) {
            if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
                throw new IllegalArgumentException("Load factor must be positive");
            }
            this.loadFactor = loadFactor;
            return this;
        }

        public Builder<V> collectionKeyMode(CollectionKeyMode mode) {
            this.collectionKeyMode = Objects.requireNonNull(mode);
            return this;
        }

        public Builder<V> flattenDimensions(boolean flatten) {
            this.flattenDimensions = flatten;
            return this;
        }

        public Builder<V> defensiveCopies(boolean defensive) {
            this.defensiveCopies = defensive;
            return this;
        }

        // Copy config from existing map
        public Builder<V> from(MultiKeyMap<?> source) {
            this.capacity = source.capacity;
            this.loadFactor = source.loadFactor;
            this.collectionKeyMode = source.collectionKeyMode;
            this.flattenDimensions = source.flattenDimensions;
            this.defensiveCopies = source.defensiveCopies;
            return this;
        }

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
    
    private static int computeElementHash(Object key) {
        if (key == null) return 0;
        if (key instanceof Class || key instanceof java.lang.reflect.AccessibleObject || key instanceof ClassLoader || key instanceof java.lang.ref.Reference || key instanceof Thread) {
            return System.identityHashCode(key);
        }
        return key.hashCode();
    }

    private ReentrantLock getStripeLock(int hash) {
        return stripeLocks[hash & STRIPE_MASK];
    }

    private void lockAllStripes() {
        int contended = 0;
        for (ReentrantLock lock : stripeLocks) {
            if (lock.hasQueuedThreads()) contended++;
            lock.lock();
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
     * Retrieves the value associated with the specified multi-dimensional key using var-args syntax.
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
     * <p>This method supports both single keys and multi-dimensional keys. Arrays and Collections
     * are automatically expanded into multi-keys based on the map's configuration settings.</p>
     * 
     * @param key the key whose associated value is to be returned. Can be a single object,
     *            array, or Collection that will be normalized according to the map's settings
     * @return the value to which the specified key is mapped, or {@code null} if no mapping exists
     */
    public V get(Object key) {
        MultiKey<V> entry = findEntry(key);
        return entry != null ? entry.value : null;
    }

    /**
     * Associates the specified value with the specified multi-dimensional key using var-args syntax.
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
     * <p>This method supports both single keys and multi-dimensional keys. Arrays and Collections
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
        int[] hashPass = new int[1];
        Object normalizedKey = normalizeLookup(key, hashPass, true); // true = make defensive copy for storage
        return new MultiKey<>(normalizedKey, hashPass[0], value);
    }

    // Method for when only the hash is needed, not the normalized key
    private void computeKeyHash(Object key, int[] hashPass) {
        normalizeLookup(key, hashPass, false); // Ignore return value, only need hash side effect
    }
    
    // Update maxChainLength to the maximum of current value and newValue
    // Uses getAndAccumulate for better performance under contention
    private void updateMaxChainLength(int newValue) {
        maxChainLength.getAndAccumulate(newValue, Math::max);
    }

    private Object normalizeLookup(Object key, int[] hashPass, boolean requestDefensiveCopy) {
        // Only make defensive copy if both requested AND enabled
        boolean makeDefensiveCopy = requestDefensiveCopy && this.defensiveCopies;
        // Handle EMOJI_EMPTY case
        if (key == null) {
            hashPass[0] = 0;
            return NULL_SENTINEL;
        }

        Class<?> clazz = key.getClass();
        
        // Simple object case - not array, not collection
        if (!clazz.isArray() && !(key instanceof Collection)) {
            hashPass[0] = finalizeHash(computeElementHash(key));
            return key;
        }

        // Handle collections that should not be expanded
        if (collectionKeyMode == CollectionKeyMode.COLLECTIONS_NOT_EXPANDED && key instanceof Collection) {
            Collection<?> coll = (Collection<?>) key;
            // In NOT_EXPANDED mode, treat collections like regular Map keys - no special processing
            hashPass[0] = finalizeHash(coll.hashCode());
            // Make defensive copy only when storing (put operations), not for lookups
            return makeDefensiveCopy ? new ArrayList<>(coll) : coll;
        }

        // Handle arrays - must check specific type to route correctly
        if (clazz.isArray()) {
            // Object[] arrays need special handling
            if (clazz == Object[].class) {
                Object[] arr = (Object[]) key;
                return process1DObjectArray(arr, hashPass);
            }
            // All other array types (int[], String[], etc.) go to typed array processing
            return process1DTypedArray(key, hashPass);
        }
        
        // Handle Collections
        Collection<?> coll = (Collection<?>) key;
        // If flattening dimensions, always go through expansion
        if (flattenDimensions) {
            return expandWithHash(coll, hashPass);
        }
        return process1DCollection(coll, hashPass, requestDefensiveCopy);
    }
    
    private Object process1DObjectArray(Object[] array, int[] hashPass) {
        if (array.length == 0) {
            hashPass[0] = 0;
            return array;
        }
        
        // Check if truly 1D while computing hash
        int h = 1;
        boolean is1D = true;
        
        for (Object e : array) {
            h = h * 31 + computeElementHash(e);
            if (e != null && (e.getClass().isArray() || e instanceof Collection)) {
                is1D = false;
                break;
            }
        }
        
        if (is1D) {
            // Single element optimization - always collapse single element arrays
            if (array.length == 1) {
                Object element = array[0];
                if (element == null) {
                    // Return NULL_SENTINEL with matching hash for consistency
                    hashPass[0] = 0; // Match top-level null normalization
                    return NULL_SENTINEL;
                } else {
                    // Recompute hash for the single element to match a simple object case
                    hashPass[0] = finalizeHash(computeElementHash(element));
                    return element;
                }
            }
            hashPass[0] = finalizeHash(h);
            return array;
        }
        
        // It's 2D+ - need to expand with hash computation
        return expandWithHash(array, hashPass);
    }
    
    private Object process1DCollection(Collection<?> coll, int[] hashPass, boolean requestDefensiveCopy) {
        // Only make defensive copy if both requested AND enabled
        boolean makeDefensiveCopy = requestDefensiveCopy && this.defensiveCopies;
        if (coll.isEmpty()) {
            hashPass[0] = 0;
            return coll;
        }
        
        // Check if truly 1D while computing hash
        int h = 1;
        boolean is1D = true;
        
        // Use type-specific fast paths for optimal performance
        if (coll instanceof ArrayList) {
            ArrayList<?> list = (ArrayList<?>) coll;
            final int size = list.size();
            for (int i = 0; i < size; i++) {
                Object e = list.get(i);
                h = h * 31 + computeElementHash(e);
                if (e != null && (e.getClass().isArray() || e instanceof Collection)) {
                    is1D = false;
                    break;
                }
            }
        } else {
            // Fallback to iterator for other collection types
            for (Object e : coll) {
                h = h * 31 + computeElementHash(e);
                if (e != null && (e.getClass().isArray() || e instanceof Collection)) {
                    is1D = false;
                    break;
                }
            }
        }
        
        if (is1D) {
            // Single element optimization - always collapse single element collections
            if (coll.size() == 1) {
                Object single = coll.iterator().next();
                if (single == null) {
                    // Return NULL_SENTINEL with matching hash for consistency
                    hashPass[0] = 0; // Match top-level null normalization
                    return NULL_SENTINEL;
                } else {
                    // Recompute hash for the single element to match a simple object case
                    hashPass[0] = finalizeHash(computeElementHash(single));
                    return single;
                }
            }
            hashPass[0] = finalizeHash(h);
            // Make defensive copy only when storing (put operations), not for lookups
            return makeDefensiveCopy ? new ArrayList<>(coll) : coll;
        }
        
        // It's 2D+ - need to expand with hash computation
        return expandWithHash(coll, hashPass);
    }
    
    private Object process1DTypedArray(Object arr, int[] hashPass) {
        Class<?> clazz = arr.getClass();
        
        // Use type-specific fast paths for optimal performance
        if (clazz == String[].class) {
            return process1DStringArray((String[]) arr, hashPass);
        }
        if (clazz == int[].class) {
            return process1DIntArray((int[]) arr, hashPass);
        }
        if (clazz == long[].class) {
            return process1DLongArray((long[]) arr, hashPass);
        }
        if (clazz == double[].class) {
            return process1DDoubleArray((double[]) arr, hashPass);
        }
        if (clazz == boolean[].class) {
            return process1DBooleanArray((boolean[]) arr, hashPass);
        }
        
        // Fallback to reflection for uncommon array types
        return process1DGenericArray(arr, hashPass);
    }
    
    private Object process1DStringArray(String[] array, int[] hashPass) {
        if (array.length == 0) {
            hashPass[0] = 0;
            return array;
        }
        
        // String arrays are always 1D (String elements can't be arrays or collections)
        int h = 1;
        for (String e : array) {
            h = h * 31 + computeElementHash(e);
        }
        
        // Single element optimization - always collapse single element arrays
        if (array.length == 1) {
            String element = array[0];
            if (element == null) {
                // Return NULL_SENTINEL with matching hash for consistency
                hashPass[0] = 0; // Match top-level null normalization
                return NULL_SENTINEL;
            } else {
                // Recompute hash for the single element to match a simple object case
                hashPass[0] = finalizeHash(computeElementHash(element));
                return element;
            }
        }
        
        hashPass[0] = finalizeHash(h);
        return array;
    }
    
    private Object process1DIntArray(int[] array, int[] hashPass) {
        if (array.length == 0) {
            hashPass[0] = 0;
            return array;
        }
        
        // int arrays are always 1D (primitives can't contain collections/arrays)
        int h = 1;
        for (int e : array) {
            h = h * 31 + Integer.hashCode(e);
        }
        
        // Single element optimization - always collapse single element arrays
        if (array.length == 1) {
            // Recompute hash for the single element to match a simple object case
            hashPass[0] = finalizeHash(Integer.hashCode(array[0]));
            return array[0]; // Return the primitive value
        }
        
        hashPass[0] = finalizeHash(h);
        return array;
    }
    
    private Object process1DLongArray(long[] array, int[] hashPass) {
        if (array.length == 0) {
            hashPass[0] = 0;
            return array;
        }
        
        // long arrays are always 1D (primitives can't contain collections/arrays)
        int h = 1;
        for (long e : array) {
            h = h * 31 + Long.hashCode(e);
        }
        
        // Single element optimization - always collapse single element arrays
        if (array.length == 1) {
            // Recompute hash for the single element to match a simple object case
            hashPass[0] = finalizeHash(Long.hashCode(array[0]));
            return array[0]; // Return the primitive value
        }
        
        hashPass[0] = finalizeHash(h);
        return array;
    }
    
    private Object process1DDoubleArray(double[] array, int[] hashPass) {
        if (array.length == 0) {
            hashPass[0] = 0;
            return array;
        }
        
        // double arrays are always 1D (primitives can't contain collections/arrays)
        int h = 1;
        for (double e : array) {
            h = h * 31 + Double.hashCode(e);
        }
        
        // Single element optimization - always collapse single element arrays
        if (array.length == 1) {
            // Recompute hash for the single element to match a simple object case
            hashPass[0] = finalizeHash(Double.hashCode(array[0]));
            return array[0]; // Return the primitive value
        }
        
        hashPass[0] = finalizeHash(h);
        return array;
    }
    
    private Object process1DBooleanArray(boolean[] array, int[] hashPass) {
        if (array.length == 0) {
            hashPass[0] = 0;
            return array;
        }
        
        // boolean arrays are always 1D (primitives can't contain collections/arrays)
        int h = 1;
        for (boolean e : array) {
            h = h * 31 + Boolean.hashCode(e);
        }
        
        // Single element optimization - always collapse single element arrays
        if (array.length == 1) {
            // Recompute hash for the single element to match a simple object case
            hashPass[0] = finalizeHash(Boolean.hashCode(array[0]));
            return array[0]; // Return the primitive value
        }
        
        hashPass[0] = finalizeHash(h);
        return array;
    }
    
    private Object process1DGenericArray(Object arr, int[] hashPass) {
        // Fallback method using reflection for uncommon array types
        int len = Array.getLength(arr);
        if (len == 0) {
            hashPass[0] = 0;
            return arr;
        }
        
        // Check if truly 1D while computing hash (same as process1DObjectArray)
        int h = 1;
        boolean is1D = true;
        
        for (int i = 0; i < len; i++) {
            Object e = Array.get(arr, i);
            h = h * 31 + computeElementHash(e);
            if (e != null && (e.getClass().isArray() || e instanceof Collection)) {
                is1D = false;
                break;
            }
        }
        
        if (is1D) {
            // Single element optimization - always collapse single element arrays
            if (len == 1) {
                Object single = Array.get(arr, 0);
                if (single == null) {
                    // Return NULL_SENTINEL with matching hash for consistency
                    hashPass[0] = 0; // Match top-level null normalization
                    return NULL_SENTINEL;
                } else {
                    // Recompute hash for the single element to match a simple object case
                    hashPass[0] = finalizeHash(computeElementHash(single));
                    return single;
                }
            }
            hashPass[0] = finalizeHash(h);
            return arr;
        }
        
        // It's 2D+ - need to expand with hash computation
        return expandWithHash(arr, hashPass);
    }
    
    private Object expandWithHash(Object key, int[] hashPass) {
        List<Object> expanded = new ArrayList<>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        int[] runningHash = new int[]{1};
        
        expandAndHash(key, expanded, visited, runningHash, flattenDimensions);
        
        hashPass[0] = finalizeHash(runningHash[0]);
        
        // Single element optimization - always collapse single elements after expansion
        if (expanded.size() == 1) {
            Object result = expanded.get(0);
            // IMPORTANT: Handle NULL_SENTINEL specially to match top-level null hash
            if (result == NULL_SENTINEL) {
                hashPass[0] = 0; // Match top-level null normalization
                return NULL_SENTINEL;
            }
            // Recompute hash for the single element to match a simple object case
            hashPass[0] = finalizeHash(computeElementHash(result));
            return result;
        }
        
        return expanded;
    }
    
    private static void expandAndHash(Object current, List<Object> result, IdentityHashMap<Object, Boolean> visited, 
                                      int[] runningHash, boolean useFlatten) {
        if (current == null) {
            result.add(NULL_SENTINEL);
            runningHash[0] = runningHash[0] * 31 + NULL_SENTINEL.hashCode();
            return;
        }

        if (visited.containsKey(current)) {
            Object cycle = EMOJI_CYCLE + System.identityHashCode(current);
            result.add(cycle);
            runningHash[0] = runningHash[0] * 31 + cycle.hashCode();
            return;
        }

        if (current.getClass().isArray()) {
            visited.put(current, true);
            try {
                if (!useFlatten) {
                    result.add(OPEN);
                    runningHash[0] = runningHash[0] * 31 + OPEN.hashCode();
                }
                int len = Array.getLength(current);
                for (int i = 0; i < len; i++) {
                    expandAndHash(Array.get(current, i), result, visited, runningHash, useFlatten);
                }
                if (!useFlatten) {
                    result.add(CLOSE);
                    runningHash[0] = runningHash[0] * 31 + CLOSE.hashCode();
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
                    runningHash[0] = runningHash[0] * 31 + OPEN.hashCode();
                }
                for (Object e : coll) {
                    expandAndHash(e, result, visited, runningHash, useFlatten);
                }
                if (!useFlatten) {
                    result.add(CLOSE);
                    runningHash[0] = runningHash[0] * 31 + CLOSE.hashCode();
                }
            } finally {
                visited.remove(current);
            }
        } else {
            result.add(current);
            runningHash[0] = runningHash[0] * 31 + computeElementHash(current);
        }
    }
    
    private MultiKey<V> findEntry(Object lookupKey) {
        // Direct normalization without creating any objects
        int[] hashPass = new int[1];
        Object normalized = normalizeLookup(lookupKey, hashPass, false);
        int hash = hashPass[0];
        Object[] currentBuckets = buckets;
        int index = hash & (currentBuckets.length - 1);
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) currentBuckets[index];
        if (chain == null) return null;
        for (MultiKey<V> entry : chain) {
            if (entry.hash == hash && keysMatch(entry.keys, normalized)) return entry;
        }
        return null;
    }

    private static boolean keysMatch(Object stored, Object lookup) {
        if (stored == lookup) return true;
        if (stored == null || lookup == null) return false;

        Class<?> storedClazz = stored.getClass();
        Class<?> lookupClazz = lookup.getClass();

        boolean storedSingle = !storedClazz.isArray() && !(stored instanceof Collection);
        boolean lookupSingle = !lookupClazz.isArray() && !(lookup instanceof Collection);

        if (storedSingle != lookupSingle) return false;

        if (storedSingle) return Objects.equals(stored, lookup);

        int storedLen = stored instanceof Collection ? ((Collection<?>) stored).size() : Array.getLength(stored);
        int lookupLen = lookup instanceof Collection ? ((Collection<?>) lookup).size() : Array.getLength(lookup);

        if (storedLen != lookupLen) return false;

        // Branch 1: Same type
        if (storedClazz == lookupClazz) {
            if (storedClazz == Object[].class) return Arrays.equals((Object[]) stored, (Object[]) lookup);
            if (storedClazz == String[].class) return Arrays.equals((String[]) stored, (String[]) lookup);
            if (storedClazz.getComponentType() != null && storedClazz.getComponentType().isPrimitive()) {
                if (storedClazz == int[].class) return Arrays.equals((int[]) stored, (int[]) lookup);
                if (storedClazz == long[].class) return Arrays.equals((long[]) stored, (long[]) lookup);
                if (storedClazz == double[].class) return Arrays.equals((double[]) stored, (double[]) lookup);
                if (storedClazz == boolean[].class) return Arrays.equals((boolean[]) stored, (boolean[]) lookup);
                if (storedClazz == byte[].class) return Arrays.equals((byte[]) stored, (byte[]) lookup);
                if (storedClazz == char[].class) return Arrays.equals((char[]) stored, (char[]) lookup);
                if (storedClazz == float[].class) return Arrays.equals((float[]) stored, (float[]) lookup);
                if (storedClazz == short[].class) return Arrays.equals((short[]) stored, (short[]) lookup);
            }
            if (storedClazz == ArrayList.class) {
                List<?> s = (List<?>) stored;
                List<?> l = (List<?>) lookup;
                for (int i = 0; i < storedLen; i++) if (!Objects.equals(s.get(i), l.get(i))) return false;
                return true;
            }
            if (storedClazz == CopyOnWriteArrayList.class) {  // As before
                List<?> s = (List<?>) stored;
                List<?> l = (List<?>) lookup;
                for (int i = 0; i < storedLen; i++) if (!Objects.equals(s.get(i), l.get(i))) return false;
                return true;
            }
            if (storedClazz == LinkedList.class) {  // New: Iterator for O(n) total
                Iterator<?> sIter = ((LinkedList<?>) stored).iterator();
                Iterator<?> lIter = ((LinkedList<?>) lookup).iterator();
                while (sIter.hasNext() && lIter.hasNext()) {
                    if (!Objects.equals(sIter.next(), lIter.next())) return false;
                }
                return true;
            }
        }

        // Branch 2: Cross-type
        boolean storedIsArray = storedClazz.isArray();
        boolean lookupIsArray = lookupClazz.isArray();

        if (storedIsArray && !lookupIsArray) {
            if (stored instanceof Object[] && lookup instanceof ArrayList) {
                Object[] sArr = (Object[]) stored;
                ArrayList<?> lList = (ArrayList<?>) lookup;
                for (int i = 0; i < storedLen; i++) if (!Objects.equals(sArr[i], lList.get(i))) return false;
                return true;
            } else if (stored instanceof String[] && lookup instanceof List) {
                String[] sArr = (String[]) stored;
                List<?> lList = (List<?>) lookup;
                for (int i = 0; i < storedLen; i++) if (!Objects.equals(sArr[i], lList.get(i))) return false;
                return true;
            } else if (stored instanceof int[] && lookup instanceof List) {
                int[] sArr = (int[]) stored;
                List<?> lList = (List<?>) lookup;
                for (int i = 0; i < storedLen; i++) if (sArr[i] != (Integer) lList.get(i)) return false;
                return true;
            } else if (stored instanceof long[] && lookup instanceof List) {
                long[] sArr = (long[]) stored;
                List<?> lList = (List<?>) lookup;
                for (int i = 0; i < storedLen; i++) if (sArr[i] != (Long) lList.get(i)) return false;
                return true;
            } else if (stored instanceof double[] && lookup instanceof List) {
                double[] sArr = (double[]) stored;
                List<?> lList = (List<?>) lookup;
                for (int i = 0; i < storedLen; i++) if (sArr[i] != (Double) lList.get(i)) return false;
                return true;
            } else if (stored instanceof boolean[] && lookup instanceof List) {
                boolean[] sArr = (boolean[]) stored;
                List<?> lList = (List<?>) lookup;
                for (int i = 0; i < storedLen; i++) if (sArr[i] != (Boolean) lList.get(i)) return false;
                return true;
            } else if (stored instanceof Object[] && lookup instanceof List) {  // General fallback
                Object[] sArr = (Object[]) stored;
                List<?> lList = (List<?>) lookup;
                for (int i = 0; i < storedLen; i++) if (!Objects.equals(sArr[i], lList.get(i))) return false;
                return true;
            }
        } else if (!storedIsArray && lookupIsArray) {
            if (stored instanceof ArrayList && lookup instanceof Object[]) {
                ArrayList<?> sList = (ArrayList<?>) stored;
                Object[] lArr = (Object[]) lookup;
                for (int i = 0; i < storedLen; i++) if (!Objects.equals(sList.get(i), lArr[i])) return false;
                return true;
            } else if (stored instanceof List && lookup instanceof String[]) {
                List<?> sList = (List<?>) stored;
                String[] lArr = (String[]) lookup;
                for (int i = 0; i < storedLen; i++) if (!Objects.equals(sList.get(i), lArr[i])) return false;
                return true;
            } else if (stored instanceof List && lookup instanceof int[]) {
                List<?> sList = (List<?>) stored;
                int[] lArr = (int[]) lookup;
                for (int i = 0; i < storedLen; i++) if ((Integer) sList.get(i) != lArr[i]) return false;
                return true;
            } else if (stored instanceof List && lookup instanceof long[]) {
                List<?> sList = (List<?>) stored;
                long[] lArr = (long[]) lookup;
                for (int i = 0; i < storedLen; i++) if ((Long) sList.get(i) != lArr[i]) return false;
                return true;
            } else if (stored instanceof List && lookup instanceof double[]) {
                List<?> sList = (List<?>) stored;
                double[] lArr = (double[]) lookup;
                for (int i = 0; i < storedLen; i++) if ((Double) sList.get(i) != lArr[i]) return false;
                return true;
            } else if (stored instanceof List && lookup instanceof boolean[]) {
                List<?> sList = (List<?>) stored;
                boolean[] lArr = (boolean[]) lookup;
                for (int i = 0; i < storedLen; i++) if ((Boolean) sList.get(i) != lArr[i]) return false;
                return true;
            } else if (stored instanceof List && lookup instanceof Object[]) {  // General fallback
                List<?> sList = (List<?>) stored;
                Object[] lArr = (Object[]) lookup;
                for (int i = 0; i < storedLen; i++) if (!Objects.equals(sList.get(i), lArr[i])) return false;
                return true;
            }
        }

        // Fallback to iterator
        Iterator<?> storedIter = iteratorFor(stored);
        Iterator<?> lookupIter = iteratorFor(lookup);
        while (storedIter.hasNext() && lookupIter.hasNext()) {
            if (!Objects.equals(storedIter.next(), lookupIter.next())) return false;
        }
        return true;
    }
    
    private static Iterator<?> iteratorFor(Object obj) {
        if (obj instanceof Collection) return ((Collection<?>) obj).iterator();
        if (obj.getClass().isArray()) return new ArrayIterator(obj);
        return Collections.singletonList(obj).iterator();
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
        boolean contended = lock.hasQueuedThreads();
        V old;
        boolean resize;

        lock.lock();
        try {
            totalLockAcquisitions.incrementAndGet();
            stripeLockAcquisitions[stripe].incrementAndGet();
            if (contended) {
                contentionCount.incrementAndGet();
                stripeLockContention[stripe].incrementAndGet();
            }

            old = putNoLock(newKey);
            resize = atomicSize.get() > buckets.length * loadFactor;
        } finally {
            lock.unlock();
        }

        if (resize && resizeInProgress.compareAndSet(false, true)) {
            try {
                resizeInternal();
            } finally {
                resizeInProgress.set(false);
            }
        }

        return old;
    }

    private V putNoLock(MultiKey<V> newKey) {
        int hash = newKey.hash;
        int index = hash & (buckets.length - 1);
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) buckets[index];

        if (chain == null) {
            buckets[index] = new MultiKey[]{newKey};
            atomicSize.incrementAndGet();
            updateMaxChainLength(1);
            return null;
        }

        for (int i = 0; i < chain.length; i++) {
            MultiKey<V> e = chain[i];
            if (e.hash == hash && keysMatch(e.keys, newKey.keys)) {
                V old = e.value;
                chain[i] = newKey;
                return old;
            }
        }

        MultiKey<V>[] newChain = Arrays.copyOf(chain, chain.length + 1);
        newChain[chain.length] = newKey;
        buckets[index] = newChain;
        atomicSize.incrementAndGet();
        updateMaxChainLength(newChain.length);
        return null;
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified multi-dimensional key
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
     * <p>This method supports both single keys and multi-dimensional keys. Arrays and Collections
     * are automatically expanded into multi-keys based on the map's configuration settings.</p>
     * 
     * @param key the key whose presence in this map is to be tested. Can be a single object,
     *            array, or Collection that will be normalized according to the map's settings
     * @return {@code true} if this map contains a mapping for the specified key
     */
    public boolean containsKey(Object key) {
        return findEntry(key) != null;
    }

    /**
     * Removes the mapping for the specified multi-dimensional key using var-args syntax.
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
     * <p>This method supports both single keys and multi-dimensional keys. Arrays and Collections
     * are automatically expanded into multi-keys based on the map's configuration settings.</p>
     * 
     * @param key the key whose mapping is to be removed from the map. Can be a single object,
     *            array, or Collection that will be normalized according to the map's settings
     * @return the previous value associated with the key, or {@code null} if there was
     *         no mapping for the key
     */
    public V remove(Object key) {
        MultiKey<V> removeKey = createMultiKey(key, null);
        return removeInternal(removeKey);
    }

    private V removeInternal(MultiKey<V> removeKey) {
        int hash = removeKey.hash;
        ReentrantLock lock = getStripeLock(hash);
        int stripe = hash & STRIPE_MASK;
        boolean contended = lock.hasQueuedThreads();
        V old;

        lock.lock();
        try {
            totalLockAcquisitions.incrementAndGet();
            stripeLockAcquisitions[stripe].incrementAndGet();
            if (contended) {
                contentionCount.incrementAndGet();
                stripeLockContention[stripe].incrementAndGet();
            }

            old = removeNoLock(removeKey);
        } finally {
            lock.unlock();
        }

        return old;
    }

    private V removeNoLock(MultiKey<V> removeKey) {
        int hash = removeKey.hash;
        int index = hash & (buckets.length - 1);
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) buckets[index];

        if (chain == null) return null;

        for (int i = 0; i < chain.length; i++) {
            MultiKey<V> e = chain[i];
            if (e.hash == hash && keysMatch(e.keys, removeKey.keys)) {
                V old = e.value;
                if (chain.length == 1) {
                    buckets[index] = null;
                } else {
                    // Shift in-place to avoid full copy
                    System.arraycopy(chain, i + 1, chain, i, chain.length - i - 1);
                    buckets[index] = Arrays.copyOf(chain, chain.length - 1);  // Shrink
                }
                atomicSize.decrementAndGet();
                return old;
            }
        }
        return null;
    }

    private void resizeInternal() {
        withAllStripeLocks(() -> {
            double lf = (double) atomicSize.get() / buckets.length;
            if (lf <= loadFactor) return;

            Object[] old = buckets;
            Object[] newBuckets = new Object[old.length * 2];
            int newMax = 0;
            atomicSize.set(0);

            for (Object b : old) {
                if (b != null) {
                    @SuppressWarnings("unchecked")
                    MultiKey<V>[] chain = (MultiKey<V>[]) b;
                    for (MultiKey<V> e : chain) {
                        int len = rehashEntry(e, newBuckets);
                        atomicSize.incrementAndGet();
                        newMax = Math.max(newMax, len);
                    }
                }
            }
            maxChainLength.set(newMax);
            // Only replace buckets after all entries are rehashed
            buckets = newBuckets;
        });
    }

    private int rehashEntry(MultiKey<V> entry, Object[] target) {
        int index = entry.hash & (target.length - 1);
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) target[index];
        if (chain == null) {
            target[index] = new MultiKey[]{entry};
            return 1;
        } else {
            MultiKey<V>[] newChain = Arrays.copyOf(chain, chain.length + 1);
            newChain[chain.length] = entry;
            target[index] = newChain;
            return newChain.length;
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
            Arrays.fill(buckets, null);
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
        for (Object b : buckets) {
            if (b != null) {
                @SuppressWarnings("unchecked")
                MultiKey<V>[] chain = (MultiKey<V>[]) b;
                for (MultiKey<V> e : chain) if (Objects.equals(e.value, value)) return true;
            }
        }
        return false;
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * <p>Multi-dimensional keys are represented as Object arrays, while single keys
     * are returned as their original objects. Changes to the returned set are not
     * reflected in the map.</p>
     * 
     * @return a set view of the keys contained in this map
     */
    public Set<Object> keySet() {
        Set<Object> set = new HashSet<>();
        for (MultiKeyEntry<V> e : entries()) {
            set.add(e.keys.length == 1 ? (e.keys[0] == NULL_SENTINEL ? null : e.keys[0]) : e.keys);
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
     * <p>Multi-dimensional keys are represented as Object arrays, while single keys
     * are returned as their original objects. Changes to the returned set are not
     * reflected in the map.</p>
     * 
     * @return a set view of the mappings contained in this map
     */
    public Set<Map.Entry<Object, V>> entrySet() {
        Set<Map.Entry<Object, V>> set = new HashSet<>();
        for (MultiKeyEntry<V> e : entries()) {
            Object k = e.keys.length == 1 ? (e.keys[0] == NULL_SENTINEL ? null : e.keys[0]) : e.keys;
            set.add(new AbstractMap.SimpleEntry<>(k, e.value));
        }
        return set;
    }

    /**
     * Copies all of the mappings from the specified map to this map.
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
        MultiKey<V> lookupKey = createMultiKey(key, value);
        int hash = lookupKey.hash;
        ReentrantLock lock = getStripeLock(hash);
        lock.lock();
        try {
            existing = get(key);
            if (existing == null) return put(key, value);
            return existing;
        } finally {
            lock.unlock();
        }
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
        int[] hashPass = new int[1];
        computeKeyHash(key, hashPass);
        int hash = hashPass[0];
        ReentrantLock lock = getStripeLock(hash);
        lock.lock();
        try {
            v = get(key);
            if (v == null) {
                v = mappingFunction.apply(key);
                if (v != null) put(key, v);
            }
            return v;
        } finally {                  
            lock.unlock();
        }
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
        int[] hashPass = new int[1];
        computeKeyHash(key, hashPass);
        int hash = hashPass[0];
        ReentrantLock lock = getStripeLock(hash);
        lock.lock();
        try {
            old = get(key);
            if (old == null) return null;
            V newV = remappingFunction.apply(key, old);
            if (newV != null) {
                put(key, newV);
                return newV;
            } else {
                remove(key);
                return null;
            }
        } finally {
            lock.unlock();
        }
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
        int[] hashPass = new int[1];
        computeKeyHash(key, hashPass);
        int hash = hashPass[0];
        ReentrantLock lock = getStripeLock(hash);
        lock.lock();
        try {
            V old = get(key);
            V newV = remappingFunction.apply(key, old);
            if (newV == null) {
                if (old != null || containsKey(key)) remove(key);
                return null;
            }
            put(key, newV);
            return newV;
        } finally {
            lock.unlock();
        }
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
        int[] hashPass = new int[1];
        computeKeyHash(key, hashPass);
        int hash = hashPass[0];
        ReentrantLock lock = getStripeLock(hash);
        lock.lock();
        try {
            V old = get(key);
            V newV = old == null ? value : remappingFunction.apply(old, value);
            if (newV == null) {
                remove(key);
            } else {
                put(key, newV);
            }
            return newV;
        } finally {
            lock.unlock();
        }
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
        int[] hashPass = new int[1];
        computeKeyHash(key, hashPass);
        int hash = hashPass[0];
        ReentrantLock lock = getStripeLock(hash);
        lock.lock();
        try {
            V current = get(key);
            if (!Objects.equals(current, value)) return false;
            remove(key);
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
        int[] hashPass = new int[1];
        computeKeyHash(key, hashPass);
        int hash = hashPass[0];
        ReentrantLock lock = getStripeLock(hash);
        lock.lock();
        try {
            if (!containsKey(key)) return null;
            return put(key, value);
        } finally {
            lock.unlock();
        }
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
        int[] hashPass = new int[1];
        computeKeyHash(key, hashPass);
        int hash = hashPass[0];
        ReentrantLock lock = getStripeLock(hash);
        lock.lock();
        try {
            V current = get(key);
            if (!Objects.equals(current, oldValue)) return false;
            put(key, newValue);
            return true;
        } finally {
            lock.unlock();
        }
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
            Object k = e.keys.length == 1 ? (e.keys[0] == NULL_SENTINEL ? null : e.keys[0]) : Arrays.hashCode(e.keys);
            h += (k == null ? 0 : k.hashCode()) ^ (e.value == null ? 0 : e.value.hashCode());
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
            Object k = e.keys.length == 1 ? (e.keys[0] == NULL_SENTINEL ? null : e.keys[0]) : e.keys;
            if (e.value == null) {
                if (!(m.get(k) == null && m.containsKey(k))) return false;
            } else if (!e.value.equals(m.get(k))) return false;
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
     * and the associated value. This provides access to the full multi-dimensional key structure
     * that may not be available through the standard {@link #entrySet()} method.</p>
     * <p>The returned iterable provides a consistent view of the map at the time of creation,
     * but changes to the map during iteration may or may not be reflected in the iteration results.</p>
     * 
     * @return an iterable of {@code MultiKeyEntry} objects containing all mappings in this map
     * @see MultiKeyEntry
     * @see #entrySet()
     */
    public Iterable<MultiKeyEntry<V>> entries() {
        return EntryIterator::new;
    }

    public static class MultiKeyEntry<V> {
        public final Object[] keys;
        public final V value;

        MultiKeyEntry(Object k, V v) {
            keys = (k != null && k.getClass() == Object[].class) ? (Object[]) k : new Object[]{k};
            value = v;
        }
    }

    private class EntryIterator implements Iterator<MultiKeyEntry<V>> {
        private final Object[] snapshot = buckets;
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
            while (bucketIdx < snapshot.length) {
                @SuppressWarnings("unchecked")
                MultiKey<V>[] chain = (MultiKey<V>[]) snapshot[bucketIdx];
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

    private static int finalizeHash(int h) {
        return EncryptionUtilities.finalizeHash(h);
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
        int[] dummyHash = new int[]{1};  // We don't need the hash for debug output
        expandAndHash(key, expanded, visited, dummyHash, false);  // For debug, always preserve structure (false for flatten)

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