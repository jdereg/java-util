package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * High-performance N-dimensional key-value Map implementation using separate chaining.
 * 
 * <p>MultiKeyMap allows storing and retrieving values using keys composed of multiple dimensions.
 * Unlike traditional maps that use a single key, this map can handle keys with any number of 
 * components, making it ideal for complex lookup scenarios.</p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><b>N-Dimensional Keys:</b> Support for keys with any number of components (1, 2, 3, ... N)</li>
 *   <li><b>High Performance:</b> Zero-allocation polymorphic storage, optimized hash computation</li>
 *   <li><b>Thread-Safe:</b> Lock-free reads with synchronized writes for maximum concurrency</li>
 *   <li><b>Map Interface Compatible:</b> Supports single-key operations via standard Map interface</li>
 *   <li><b>Flexible API:</b> Varargs methods for convenient multi-key operations</li>
 *   <li><b>Smart Collection Handling:</b> Configurable behavior for Collections and Arrays</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Create a multi-key map
 * MultiKeyMap<String> map = new MultiKeyMap<>(1024);
 * 
 * // Store values with different key dimensions using Map interface
 * map.put("single-key", "value1");                         // 1D key
 * map.put(new Object[]{"k1", "k2"}, "value2");             // 2D key via array
 * map.put(Arrays.asList("k1", "k2", "k3"), "value3");      // 3D key via Collection
 * 
 * // OR use convenient varargs methods (requires MultiKeyMap variable type)
 * MultiKeyMap<String> mkMap = new MultiKeyMap<>();
 * mkMap.put("value1", "single-key");                       // 1D key
 * mkMap.put("value2", "key1", "key2");                     // 2D key  
 * mkMap.put("value3", "key1", "key2", "key3");             // 3D key
 * mkMap.put("value4", "k1", "k2", "k3", "k4");             // 4D key
 * // ... unlimited dimensions
 *
 * // Retrieve values using matching signatures
 * String val1 = map.get("single-key");
 * String val2 = map.get(new Object[]{"k1", "k2"});
 * String val3 = mkMap.get("key1", "key2");
 * String val4 = mkMap.get("k1", "k2", "k3", "k4");
 * }</pre>
 * 
 * <h3>Collection and Array Handling:</h3>
 * <p>MultiKeyMap provides flexible handling of Collections and Arrays through the 
 * {@link CollectionKeyMode} enum:</p>
 * <ul>
 *   <li><b>MULTI_KEY_ONLY:</b> Collections/Arrays are always unpacked into multi-key lookups</li>
 *   <li><b>MULTI_KEY_FIRST:</b> Try unpacking first, fallback to treating as single key</li>
 *   <li><b>COLLECTION_KEY_FIRST:</b> Try as single key first, fallback to unpacking</li>
 * </ul>
 * 
 * <pre>{@code
 * // Configure collection handling behavior
 * MultiKeyMap<String> map = new MultiKeyMap<>(1024, CollectionKeyMode.COLLECTION_KEY_FIRST);
 * 
 * String[] arrayKey = {"config", "database", "url"};
 * map.put(arrayKey, "jdbc:mysql://localhost:3306/db");     // Array treated as single key
 * String url = map.get(arrayKey);                          // Retrieved as single key
 * }</pre>
 * 
 * <h3>Performance Characteristics:</h3>
 * <ul>
 *   <li><b>Time Complexity:</b> O(1) average case for get/put/remove operations</li>
 *   <li><b>Space Complexity:</b> O(n) where n is the number of stored key-value pairs</li>
 *   <li><b>Memory Efficiency:</b> Polymorphic storage (Object vs Object[]) eliminates wrappers</li>
 *   <li><b>Concurrency:</b> Lock-free reads, synchronized writes</li>
 *   <li><b>Load Factor:</b> Configurable, defaults to 0.75 for optimal performance</li>
 * </ul>
 * 
 * <h3>Thread Safety:</h3>
 * <p>This implementation is thread-safe. Read operations (get, containsKey, etc.) are lock-free 
 * for maximum performance, while write operations (put, remove, etc.) are synchronized to ensure 
 * data consistency. The class uses volatile fields and careful memory ordering to ensure 
 * visibility across threads.</p>
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
public final class MultiKeyMap<V> implements Map<Object, V> {
    
    /**
     * Enum to control how keys are stored in put() operations.
     * Used as optional last parameter in varargs put() method.
     */
    public enum KeyMode {
        /**
         * Store the preceding keys as a single key without auto-unpacking.
         * Use this when you want Collections or Arrays to be treated as single keys
         * rather than being auto-unpacked into multiple key dimensions.
         */
        SINGLE_KEY
    }
    
    /**
     * Enum to control how Collections and Arrays are handled in get/remove/containsKey operations.
     */
    public enum CollectionKeyMode {
        /**
         * Default behavior: Collections and Arrays are always unpacked into multi-key lookups.
         * No fallback to treating them as single keys.
         */
        MULTI_KEY_ONLY,
        
        /**
         * Try multi-key lookup first, then fallback to collection-as-key lookup if not found.
         * Prioritizes the traditional multi-key behavior.
         */
        MULTI_KEY_FIRST,
        
        /**
         * Try collection-as-key lookup first, then fallback to multi-key lookup if not found.
         * Prioritizes treating Collections/Arrays as single keys.
         */
        COLLECTION_KEY_FIRST
    }
    
    private volatile Object[] buckets;  // Array of MultiKey<V>[] (or null), Collection, String[] (typed array)
    private final Object writeLock = new Object();
    private volatile int size = 0;
    private volatile int maxChainLength = 0;
    private final float loadFactor;
    private final CollectionKeyMode collectionKeyMode;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f; // Same as HashMap default

    /**
     * Sentinel value for null keys, similar to ConcurrentHashMapNullSafe approach.
     * This allows us to store and retrieve null keys safely.
     */
    private static final Object NULL_SENTINEL = new Object();
    
    /**
     * Sentinel value to distinguish between "key not found" vs "key found with null value".
     * This allows efficient single-lookup double-try logic without containsKey() calls.
     */
    private static final Object NOT_FOUND_SENTINEL = new Object();
    
    /**
     * Represents a key-value mapping that can store either single keys or N-dimensional keys.
     * Uses polymorphic storage: Object (single key) or Object[] (multi-key).
     */
    private static final class MultiKey<V> {
        final Object keys;    // Object (single key) OR Object[] (multi-key) - no wrapper needed!
        final int hash;
        final V value;
        
        // Constructor for single keys (including null → NULL_SENTINEL)
        MultiKey(Object singleKey, V value) {
            this.keys = (singleKey == null) ? NULL_SENTINEL : singleKey;
            this.hash = computeSingleKeyHash(this.keys);
            this.value = value;
        }
        
        // Constructor for multi keys
        MultiKey(Object[] multiKeys, V value) {
            this.keys = multiKeys != null ? multiKeys.clone() : new Object[0]; // Defensive copy
            this.hash = computeHash(multiKeys);
            this.value = value;
        }
    }
    
    /**
     * Creates a new MultiKeyMap with the specified capacity and load factor.
     * 
     * @param capacity the initial capacity
     * @param loadFactor the load factor threshold for resizing
     */
    public MultiKeyMap(int capacity, float loadFactor) {
        this(capacity, loadFactor, CollectionKeyMode.MULTI_KEY_ONLY);
    }
    
    /**
     * Creates a new MultiKeyMap with specified capacity, load factor, and collection key behavior.
     * 
     * @param capacity the initial capacity
     * @param loadFactor the load factor threshold for resizing
     * @param collectionKeyMode how to handle Collections/Arrays in get/remove/containsKey operations
     */
    public MultiKeyMap(int capacity, float loadFactor, CollectionKeyMode collectionKeyMode) {
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Load factor must be positive: " + loadFactor);
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must be non-negative: " + capacity);
        }
        
        this.buckets = new Object[capacity];
        this.loadFactor = loadFactor;
        this.collectionKeyMode = (collectionKeyMode != null) ? collectionKeyMode : CollectionKeyMode.MULTI_KEY_ONLY;
    }
    
    /**
     * Creates a new MultiKeyMap with default capacity (16) and default load factor (0.75).
     */
    public MultiKeyMap() {
        this(16); // Default initial capacity
    }
    
    /**
     * Creates a new MultiKeyMap with default capacity (16) and specified collection key behavior.
     * 
     * @param collectionKeyMode how to handle Collections/Arrays in get/remove/containsKey operations
     */
    public MultiKeyMap(CollectionKeyMode collectionKeyMode) {
        this(16, DEFAULT_LOAD_FACTOR, collectionKeyMode);
    }
    
    /**
     * Creates a new MultiKeyMap with the specified capacity and default load factor.
     * 
     * @param capacity the initial capacity
     */
    public MultiKeyMap(int capacity) {
        this(capacity, DEFAULT_LOAD_FACTOR);
    }
    
    /**
     * Computes optimized hash for N-dimensional key using appropriate hash codes.
     * Uses identity hash codes for Classes and standard hash codes for other objects.
     */
    private static int computeHash(Object... keys) {
        return computeHashFromArray(keys);
    }
    
    /**
     * Computes hash for single keys (including NULL_SENTINEL).
     * Uses same hashing approach as multi-key hash for consistency.
     */
    private static int computeSingleKeyHash(Object key) {
        return computeHashFromSingle(key);
    }
    
    private static int computeHashFromArray(Object[] keys) {
        if (keys == null || keys.length == 0) {
            return 0;
        }
        return computeHashInternal(keys, keys.length);
    }
    
    private static int computeHashFromCollection(Collection<?> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        return computeHashInternal(keys, keys.size());
    }
    
    private static int computeHashFromTypedArray(Object typedArray) {
        if (typedArray == null) {
            return 0;
        }
        int length = Array.getLength(typedArray);
        if (length == 0) {
            return 0;
        }
        return computeHashInternal(typedArray, length);
    }
    
    private static int computeHashFromSingle(Object key) {
        if (key == null || key == NULL_SENTINEL) {
            return 0;
        }
        int keyHash = getKeyHash(key);
        return finalizeHash(keyHash);
    }
    
    private static int computeHashInternal(Object keys, int size) {
        if (size == 0) return 0;
        
        int hash = 1;
        if (keys instanceof Object[]) {
            Object[] array = (Object[]) keys;
            for (Object key : array) {
                hash = hash * 31 + getKeyHash(key);
            }
        } else if (keys instanceof Collection) {
            for (Object key : (Collection<?>) keys) {
                hash = hash * 31 + getKeyHash(key);
            }
        } else {
            // Typed array
            for (int i = 0; i < size; i++) {
                hash = hash * 31 + getKeyHash(Array.get(keys, i));
            }
        }
        
        return finalizeHash(hash);
    }
    
    private static int getKeyHash(Object key) {
        if (key == null) return 0;
        return (key instanceof Class) ? 
            System.identityHashCode(key) : key.hashCode();
    }
    
    private static int finalizeHash(int hash) {
        // MurmurHash3 finalization
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);
        return hash;
    }
    
    /**
     * Gets the conversion function for the given N-dimensional key, or null if not found.
     * This method is lock-free for maximum read performance.
     * 
     * @param keys the key components (can be varargs or Object[])
     * @return the value associated with the key, or null if not found
     */
    public V get(Object... keys) {
        // Special case: when get(null) is called on varargs method, Java passes keys=null
        // Use direct single-key lookup for zero heap allocation
        if (keys == null) {
            return getInternalDirect(null);
        }
        return getInternal(keys);
    }
    
    /**
     * Map interface compatible get method with zero-allocation direct storage.
     * Supports both single keys and N-dimensional keys via Object[] detection.
     * 
     * @param key either a single key or an Object[] containing multiple keys
     * @return the value associated with the key, or null if not found
     */
    public V get(Object key) {
        // Fast path for normal objects (most common case) - zero heap allocation
        if (key == null) {
            return getInternalDirect(null);
        }
        
        Class<?> keyClass = key.getClass();
        if (keyClass.isArray()) {
            return getFromArray(key);
        } else if (key instanceof Collection) {
            return getFromCollection((Collection<?>) key);
        } else {
            // Normal object
            return getInternalDirect(key);
        }
    }
    
    /**
     * Handle Collection keys with mode-based logic.
     */
    private V getFromCollection(Collection<?> collection) {
        switch (collectionKeyMode) {
            case MULTI_KEY_ONLY:
                return getFromCollectionMultiKeyOnly(collection);
            case MULTI_KEY_FIRST:
                return getFromCollectionMultiKeyFirst(collection);
            case COLLECTION_KEY_FIRST:
                return getFromCollectionKeyFirst(collection);
            default:
                throw new IllegalStateException("Unknown CollectionKeyMode: " + collectionKeyMode);
        }
    }
    
    /**
     * Handle Array keys with mode-based logic.
     */
    private V getFromArray(Object array) {
        switch (collectionKeyMode) {
            case MULTI_KEY_ONLY:
                return getFromArrayMultiKeyOnly(array);
            case MULTI_KEY_FIRST:
                return getFromArrayMultiKeyFirst(array);
            case COLLECTION_KEY_FIRST:
                return getFromArrayKeyFirst(array);
            default:
                throw new IllegalStateException("Unknown CollectionKeyMode: " + collectionKeyMode);
        }
    }
    
    /**
     * Collection with MULTI_KEY_ONLY mode - only try multi-key lookup.
     */
    private V getFromCollectionMultiKeyOnly(Collection<?> collection) {
        Object rawResult = getInternalFromCollectionRaw(collection);
        return rawResult == NOT_FOUND_SENTINEL ? null : (V) rawResult;
    }
    
    /**
     * Collection with MULTI_KEY_FIRST mode - try multi-key first, then collection-as-key.
     */
    private V getFromCollectionMultiKeyFirst(Collection<?> collection) {
        // Try multi-key first
        Object rawResult = getInternalFromCollectionRaw(collection);
        if (rawResult != NOT_FOUND_SENTINEL) {
            return (V) rawResult;  // Found via multi-key (could be null value)
        }
        
        // Multi-key not found, try collection-as-key with zero allocations!
        return getInternalDirect(collection);
    }
    
    /**
     * Collection with COLLECTION_KEY_FIRST mode - try collection-as-key first, then multi-key.
     */
    private V getFromCollectionKeyFirst(Collection<?> collection) {
        // Try collection-as-key first with zero allocations!
        V result = getInternalDirect(collection);
        if (result != null) {
            return result;  // Found via collection-as-key
        }
        
        // Collection-as-key not found, try multi-key
        Object rawResult = getInternalFromCollectionRaw(collection);
        return rawResult == NOT_FOUND_SENTINEL ? null : (V) rawResult;
    }
    
    /**
     * Array with MULTI_KEY_ONLY mode - only try multi-key lookup.
     */
    private V getFromArrayMultiKeyOnly(Object array) {
        if (array instanceof Object[]) {
            return getInternal((Object[]) array);
        } else {
            return getInternalFromTypedArray(array);
        }
    }
    
    /**
     * Array with MULTI_KEY_FIRST mode - try multi-key first, then array-as-key.
     */
    private V getFromArrayMultiKeyFirst(Object array) {
        // Try multi-key first
        Object rawResult;
        if (array instanceof Object[]) {
            rawResult = getInternalRaw((Object[]) array);
        } else {
            // For typed arrays, we need to convert to check - less efficient but necessary
            V result = getInternalFromTypedArray(array);
            if (result != null) {
                return result;
            }
            rawResult = NOT_FOUND_SENTINEL;  // Typed array lookup returned null
        }
        
        if (rawResult != NOT_FOUND_SENTINEL) {
            return (V) rawResult;  // Found via multi-key (could be null value)
        }
        
        // Multi-key not found, try array-as-key with zero allocations!
        return getInternalDirect(array);
    }
    
    /**
     * Array with COLLECTION_KEY_FIRST mode - try array-as-key first, then multi-key.
     */
    private V getFromArrayKeyFirst(Object array) {
        // Try array-as-key first with zero allocations!
        V result = getInternalDirect(array);
        if (result != null) {
            return result;  // Found via array-as-key
        }
        
        // Array-as-key not found, try multi-key
        if (array instanceof Object[]) {
            return getInternal((Object[]) array);
        } else {
            return getInternalFromTypedArray(array);
        }
    }
    
    
    /**
     * Gets the value for the given typed array-based multi-dimensional key.
     * This method provides zero-conversion access by working directly with typed arrays
     * like String[], int[], Class<?>[], etc. using reflection for element access.
     * 
     * @param typedArray typed array containing the key components (String[], int[], etc.)
     * @return the value associated with the key, or null if not found
     */
    private V getInternalFromTypedArray(Object typedArray) {
        if (typedArray == null) {
            return get((Object) null);
        }
        
        int length = Array.getLength(typedArray);
        if (length == 0) {
            return null;
        }
        
        // Compute hash directly from typed array to avoid conversion
        int hash = computeHashFromTypedArray(typedArray);
        
        // Capture buckets reference to avoid race condition during resize
        Object[] currentBuckets = buckets;
        int bucketIndex = hash & (currentBuckets.length - 1);
        
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) currentBuckets[bucketIndex];
        
        if (chain == null) {
            return null;
        }
        
        // Scan the chain for exact match
        for (MultiKey<V> entry : chain) {
            if (entry.hash == hash && keysMatch(entry.keys, typedArray)) {
                return entry.value;
            }
        }
        
        return null;
    }
    
    /**
     * Internal get implementation that works with Object[] keys.
     */
    private V getInternal(Object[] keys) {
        int hash = computeHash(keys);
        
        // Capture buckets reference to avoid race condition during resize
        Object[] currentBuckets = buckets;
        int bucketIndex = hash & (currentBuckets.length - 1);
        
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) currentBuckets[bucketIndex];
        
        if (chain == null) {
            return null;
        }
        
        // Scan the chain for exact match - direct array access for maximum speed
        for (MultiKey<V> entry : chain) {
            if (entry.hash == hash && keysMatch(entry.keys, keys)) {
                return entry.value;
            }
        }
        
        return null;
    }
    
    /**
     * Internal get implementation that returns raw result with sentinels.
     * Returns NOT_FOUND_SENTINEL if key doesn't exist, actual value (including null) if found.
     */
    private Object getInternalRaw(Object[] keys) {
        int hash = computeHash(keys);
        
        // Capture buckets reference to avoid race condition during resize
        Object[] currentBuckets = buckets;
        int bucketIndex = hash & (currentBuckets.length - 1);
        
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) currentBuckets[bucketIndex];
        
        if (chain == null) {
            return NOT_FOUND_SENTINEL;
        }
        
        // Scan the chain for exact match
        for (MultiKey<V> entry : chain) {
            if (entry.hash == hash && keysMatch(entry.keys, keys)) {
                return entry.value;  // Return actual value (could be null)
            }
        }
        
        return NOT_FOUND_SENTINEL;
    }
    
    /**
     * Internal get() implementation for Collections using sentinel return.
     */
    private Object getInternalFromCollectionRaw(Collection<?> collection) {
        if (collection.isEmpty()) {
            return NOT_FOUND_SENTINEL;
        }
        
        int hash = computeHashFromCollection(collection);
        
        // Capture buckets reference to avoid race condition during resize
        Object[] currentBuckets = buckets;
        int bucketIndex = hash & (currentBuckets.length - 1);
        
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) currentBuckets[bucketIndex];
        
        if (chain == null) {
            return NOT_FOUND_SENTINEL;
        }
        
        // Scan the chain for exact match
        for (MultiKey<V> entry : chain) {
            if (entry.hash == hash && keysMatch(entry.keys, collection)) {
                return entry.value;  // Return actual value (could be null)
            }
        }
        
        return NOT_FOUND_SENTINEL;
    }
    
    /**
     * Direct single-key lookup with zero heap allocations.
     * Uses polymorphic storage - no wrapper needed!
     */
    private V getInternalDirect(Object key) {
        // Handle null key using NULL_SENTINEL
        Object lookupKey = (key == null) ? NULL_SENTINEL : key;
        int hash = computeSingleKeyHash(lookupKey);
        
        // Capture buckets reference to avoid race condition during resize
        Object[] currentBuckets = buckets;
        int bucketIndex = hash & (currentBuckets.length - 1);
        
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) currentBuckets[bucketIndex];
        
        if (chain == null) {
            return null;
        }
        
        // Scan the chain for exact match
        for (MultiKey<V> entry : chain) {
            if (entry.hash == hash && isSingleKeyMatch(entry.keys, lookupKey)) {
                return entry.value;
            }
        }
        
        return null;
    }
    
    /**
     * Check if stored keys match the single lookup key.
     * Optimized for single-key lookups.
     */
    private boolean isSingleKeyMatch(Object storedKeys, Object singleKey) {
        return keysMatch(storedKeys, singleKey);
    }
    
    /**
     * Direct single key removal - zero allocation for single key operations.
     * Similar to getInternalDirect but removes the entry.
     */
    private V removeInternalDirect(Object key) {
        // Handle null key using NULL_SENTINEL
        Object lookupKey = (key == null) ? NULL_SENTINEL : key;
        int hash = computeSingleKeyHash(lookupKey);
        
        // Capture buckets reference to avoid race condition during resize
        Object[] currentBuckets = buckets;
        int bucketIndex = hash & (currentBuckets.length - 1);
        
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) currentBuckets[bucketIndex];
        
        if (chain == null) {
            return null;
        }
        
        // Scan the chain for exact match and remove
        for (int i = 0; i < chain.length; i++) {
            MultiKey<V> entry = chain[i];
            if (entry.hash == hash && isSingleKeyMatch(entry.keys, lookupKey)) {
                // Found it - remove from chain
                @SuppressWarnings("unchecked")
                MultiKey<V>[] newChain = new MultiKey[chain.length - 1];
                System.arraycopy(chain, 0, newChain, 0, i);
                System.arraycopy(chain, i + 1, newChain, i, chain.length - i - 1);
                
                // Update bucket atomically
                currentBuckets[bucketIndex] = (newChain.length == 0) ? null : newChain;
                size--;
                
                return entry.value;
            }
        }
        
        return null;
    }
    
    /**
     * Direct single key containment check - zero allocation for single key operations.
     * Similar to getInternalDirect but only checks existence.
     */
    private boolean containsKeyInternalDirect(Object key) {
        // Handle null key using NULL_SENTINEL
        Object lookupKey = (key == null) ? NULL_SENTINEL : key;
        int hash = computeSingleKeyHash(lookupKey);
        
        // Capture buckets reference to avoid race condition during resize
        Object[] currentBuckets = buckets;
        int bucketIndex = hash & (currentBuckets.length - 1);
        
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) currentBuckets[bucketIndex];
        
        if (chain == null) {
            return false;
        }
        
        // Scan the chain for exact match
        for (MultiKey<V> entry : chain) {
            if (entry.hash == hash && isSingleKeyMatch(entry.keys, lookupKey)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if stored keys match the lookup key (single or multi).
     * Handles all combinations: Object vs Object, Object[] vs Object[], but NOT Object vs Object[]
     */
    private static boolean keysMatch(Object storedKeys, Object lookupKeys) {
        if (storedKeys == lookupKeys) return true;
        if (storedKeys == null || lookupKeys == null) return false;
        
        boolean storedIsArray = storedKeys instanceof Object[];
        boolean lookupIsArray = lookupKeys instanceof Object[] || 
                               (lookupKeys instanceof Collection) ||
                               (lookupKeys != null && lookupKeys.getClass().isArray());
        
        // Type mismatch: single vs multi-key
        if (storedIsArray != lookupIsArray) return false;
        
        if (storedIsArray) {
            return matchMultiKeys((Object[]) storedKeys, lookupKeys);
        } else {
            return Objects.equals(storedKeys, lookupKeys);
        }
    }
    
    private static boolean matchMultiKeys(Object[] stored, Object lookup) {
        if (lookup instanceof Object[]) {
            return keysEqual(stored, (Object[]) lookup);
        } else if (lookup instanceof Collection) {
            return keysEqualCollection(stored, (Collection<?>) lookup);
        } else if (lookup.getClass().isArray()) {
            return keysEqualTypedArray(stored, lookup);
        }
        return false;
    }
    
    /**
     * Optimized key equality check that handles Classes with identity comparison.
     */
    private static boolean keysEqual(Object[] keys1, Object[] keys2) {
        if (keys1 == keys2) return true;  // Same reference including both null
        if (keys1 == null || keys2 == null) return false;  // One null, one not
        if (keys1.length != keys2.length) {
            return false;
        }
        
        for (int i = 0; i < keys1.length; i++) {
            if (!keyEquals(keys1[i], keys2[i])) {
                return false;
            }
        }
        
        return true;
    }
    
    private static boolean keysEqualCollection(Object[] stored, Collection<?> lookup) {
        if (stored.length != lookup.size()) {
            return false;
        }
        
        int i = 0;
        for (Object lookupKey : lookup) {
            if (!keyEquals(stored[i], lookupKey)) {
                return false;
            }
            i++;
        }
        return true;
    }
    
    private static boolean keysEqualTypedArray(Object[] stored, Object typedArray) {
        int length = Array.getLength(typedArray);
        if (stored.length != length) {
            return false;
        }
        
        for (int i = 0; i < length; i++) {
            if (!keyEquals(stored[i], Array.get(typedArray, i))) {
                return false;
            }
        }
        return true;
    }
    
    private static boolean keyEquals(Object k1, Object k2) {
        if (k1 == k2) {
            return true; // Same reference (includes null == null)
        }
        
        if (k1 == null || k2 == null) {
            return false; // One null, one not null
        }
        
        // For Classes, use identity comparison for performance
        if (k1 instanceof Class && k2 instanceof Class) {
            return false; // Already checked == above, so they're different Classes
        }
        
        // For other objects, use equals
        return k1.equals(k2);
    }
    
    /**
     * Compares stored keys (Object or Object[]) with Collection keys for equality.
     * Uses same comparison logic as keysEqual for consistency.
     */
    
    
    
    /**
     * Premium varargs API - Store a value with unlimited multiple keys.
     * This is the recommended API for MultiKeyMap users as it provides the best
     * developer experience with unlimited keys and zero array allocations for
     * inline arguments.
     * 
     * <p>Examples:</p>
     * <pre>{@code
     * MultiKeyMap<Employee> map = new MultiKeyMap<>();
     * 
     * // Zero allocation - no arrays created
     * map.put(employee, "dept", "engineering", "senior");
     * map.put(person, "location", "building1", "floor2", "room101");
     * 
     * // Works with existing arrays too
     * String[] keyArray = {"dept", "marketing", "director"};
     * map.put(manager, keyArray);  // Passes array directly to varargs
     * }</pre>
     * 
     * @param value the value to store
     * @param keys the key components (unlimited number)
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public V put(V value, Object... keys) {
        // Handle null keys array (empty varargs call)
        if (keys == null || keys.length == 0) {
            return putInternalSingle(null, value);
        }
        
        // Prevent KeyMode enum from being stored as actual key
        for (Object key : keys) {
            if (key == KeyMode.SINGLE_KEY) {
                // Only allow at the end as a flag, not as a regular key
                if (key != keys[keys.length - 1]) {
                    throw new IllegalArgumentException("KeyMode.SINGLE_KEY can only be used as the last parameter");
                }
            }
        }
        
        // Check if last parameter is KeyMode.SINGLE_KEY (force single-key storage)
        if (keys.length >= 2 && keys[keys.length - 1] == KeyMode.SINGLE_KEY) {
            // Remove the KeyMode flag and treat remaining keys as single key
            Object[] actualKeys = new Object[keys.length - 1];
            System.arraycopy(keys, 0, actualKeys, 0, keys.length - 1);
            
            // Force single-key storage using new direct approach
            if (actualKeys.length == 1) {
                // Single key - store directly
                return putInternalSingle(actualKeys[0], value);
            } else {
                // Multiple keys but user wants them as single composite key
                return putInternalSingle(actualKeys, value);
            }
        }
        
        return putInternal(keys, value);
    }
    
    /**
     * Map interface compatible put method with auto-unpacking for arrays.
     * This provides a great experience for Map users by automatically detecting
     * and unpacking arrays into multi-key calls.
     * 
     * <p><strong>Auto-unpacking behavior:</strong></p>
     * <ul>
     *   <li>If key is an array → automatically unpacked into multiple keys</li>
     *   <li>If key is a Collection → automatically unpacked into multiple keys</li>
     *   <li>Otherwise → treated as single key</li>
     * </ul>
     * 
     * <p>Examples:</p>
     * <pre>{@code
     * Map<Object, Employee> map = new MultiKeyMap<>();
     * 
     * // Auto-unpacking: array becomes multi-key
     * String[] keys = {"dept", "engineering", "senior"};
     * map.put(keys, employee);  // Stored as 3-key entry
     * 
     * // Auto-unpacking: Collection becomes multi-key
     * List<String> keyList = Arrays.asList("dept", "sales", "junior");
     * map.put(keyList, employee);  // Stored as 3-key entry
     * 
     * // Single key: other objects stored normally  
     * map.put("manager", boss);  // Stored as single-key entry
     * 
     * // Typed arrays also auto-unpack
     * int[] intKeys = {1, 2, 3};
     * map.put(intKeys, data);  // Stored as 3-key entry
     * }</pre>
     * 
     * @param key single key, or array/Collection that will be auto-unpacked into multiple keys
     * @param value the value to store
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public V put(Object key, V value) {
        if (key != null && key.getClass().isArray()) {
            if (key instanceof Object[]) {
                // Auto-unpack Object[] into multi-key call
                return put(value, (Object[]) key);
            } else {
                // Auto-unpack typed array into multi-key call  
                return putInternalFromTypedArray(key, value);
            }
        } else if (key instanceof Collection) {
            // Auto-unpack Collection into multi-key call
            Collection<?> collection = (Collection<?>) key;
            return put(value, collection.toArray());
        } else {
            // Single key case
            return putInternalSingle(key, value);
        }
    }
    
    
    /**
     * Internal put implementation that works with Object[] keys.
     */
    private V putInternal(Object[] keys, V value) {
        MultiKey<V> newKey = new MultiKey<>(keys, value); // Uses multi-key constructor
        return putInternalCommon(newKey);
    }
    
    /**
     * Internal put implementation for single keys using polymorphic storage.
     * No wrapper needed - stores the key directly!
     */
    private V putInternalSingle(Object key, V value) {
        MultiKey<V> newKey = new MultiKey<>(key, value); // Uses single-key constructor
        return putInternalCommon(newKey);
    }
    
    /**
     * Common put logic for both single and multi-key entries.
     */
    private V putInternalCommon(MultiKey<V> newKey) {
        int hash = newKey.hash;
        
        synchronized (writeLock) {
            // Bound hash to our table size inside the synchronized block
            int bucketIndex = hash & (buckets.length - 1);

            @SuppressWarnings("unchecked")
            MultiKey<V>[] chain = (MultiKey<V>[]) buckets[bucketIndex];
            
            if (chain == null) {
                // Create a new single-element chain
                chain = createChain(newKey);
                buckets[bucketIndex] = chain;
                size++;
                
                // Update max chain length monitoring
                if (1 > maxChainLength) {
                    maxChainLength = 1;
                }
            } else {
                // Check for an existing key (update case)
                for (int i = 0; i < chain.length; i++) {
                    MultiKey<V> existing = chain[i];
                    if (existing.hash == hash && keysMatch(existing.keys, newKey.keys)) {
                        // Update existing entry in place - return old value
                        V oldValue = existing.value;
                        chain[i] = newKey;
                        return oldValue;
                    }
                }
                
                // Add new entry by growing the chain
                chain = growChain(chain, newKey);
                buckets[bucketIndex] = chain;
                size++;
                
                // Update max chain length monitoring
                if (chain.length > maxChainLength) {
                    maxChainLength = chain.length;
                }
            }
            
            // Check if resize is needed after adding new entry
            if (size > buckets.length * loadFactor) {
                resize();
            }
            
            return null; // No previous value for new entry
        }
    }
    
    /**
     * Internal put implementation that works with typed arrays.
     * Converts typed array to Object[] and delegates to putInternal.
     */
    private V putInternalFromTypedArray(Object typedArray, V value) {
        int length = Array.getLength(typedArray);
        Object[] keys = new Object[length];
        
        for (int i = 0; i < length; i++) {
            keys[i] = Array.get(typedArray, i);
        }
        
        return putInternal(keys, value);
    }
    
    /**
     * Helper method to handle typed arrays (String[], int[], etc.) in remove operations.
     * Converts typed arrays to Object[] arrays using reflection to avoid ClassCastException.
     */
    private V removeInternalFromTypedArray(Object typedArray) {
        int length = Array.getLength(typedArray);
        Object[] keys = new Object[length];
        
        for (int i = 0; i < length; i++) {
            keys[i] = Array.get(typedArray, i);
        }
        
        return removeInternal(keys);
    }
    
    
    /**
     * Creates a new single-element chain array.
     */
    @SuppressWarnings("unchecked")
    private MultiKey<V>[] createChain(MultiKey<V> key) {
        return new MultiKey[]{key};
    }
    
    /**
     * Grows an existing chain by one element.
     * Creates a new array with the new key appended.
     */
    private MultiKey<V>[] growChain(MultiKey<V>[] oldChain, MultiKey<V> newKey) {
        MultiKey<V>[] newChain = Arrays.copyOf(oldChain, oldChain.length + 1);
        newChain[oldChain.length] = newKey;
        return newChain;
    }

    /**
     * Resizes the hash table to double its current capacity and rehashes all entries.
     * This method must be called from within a synchronized block.
     */
    private void resize() {
        Object[] oldBuckets = buckets;
        Object[] newBuckets = new Object[oldBuckets.length * 2];

        int newSize = 0;
        int newMaxChainLength = 0;

        // Rehash all entries into the newBuckets array
        for (Object bucket : oldBuckets) {
            if (bucket != null) {
                @SuppressWarnings("unchecked")
                MultiKey<V>[] chain = (MultiKey<V>[]) bucket;
                for (MultiKey<V> entry : chain) {
                    int len = rehashEntry(entry, newBuckets);
                    newSize++;
                    if (len > newMaxChainLength) {
                        newMaxChainLength = len;
                    }
                }
            }
        }

        buckets = newBuckets;
        size = newSize;
        maxChainLength = newMaxChainLength;
    }

    private int rehashEntry(MultiKey<V> entry, Object[] targetBuckets) {
        int bucketIndex = entry.hash & (targetBuckets.length - 1);

        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) targetBuckets[bucketIndex];

        if (chain == null) {
            targetBuckets[bucketIndex] = createChain(entry);
            return 1;
        } else {
            chain = growChain(chain, entry);
            targetBuckets[bucketIndex] = chain;
            return chain.length;
        }
    }
    
    /**
     * Returns the current number of entries in the map.
     */
    public int size() {
        return size;
    }
    
    /**
     * Returns the maximum chain length encountered so far.
     */
    public int getMaxChainLength() {
        return maxChainLength;
    }
    
    /**
     * Returns the current load factor.
     */
    public double getLoadFactor() {
        return (double) size / buckets.length;
    }
    
    
    /**
     * Returns an iterator over all entries in the map.
     * The iterator captures a snapshot of the current state and is thread-safe for reads.
     * Concurrent modifications during iteration may not be reflected in the iteration.
     */
    public Iterable<MultiKeyEntry<V>> entries() {
        return new EntryIterable();
    }
    
    /**
     * Represents a single entry in the MultiKeyMap.
     * Provides access to both the N-dimensional key and its associated value.
     * For backward compatibility, provides legacy triple-key access methods.
     */
    public static class MultiKeyEntry<V> {
        public final Object[] keys;
        public final V value;

        // Universal constructor - handles both single keys and Object[] keys
        MultiKeyEntry(Object keys, V value) {
            if (keys instanceof Object[]) {
                // Multi-key case
                this.keys = ((Object[]) keys).clone();
            } else {
                // Single key case - wrap in Object[]
                this.keys = new Object[]{keys};
            }
            this.value = value;
        }
        
    }

    /**
     * Iterable implementation for MultiKeyMap entries.
     */
    private class EntryIterable implements Iterable<MultiKeyEntry<V>> {
        @Override
        public java.util.Iterator<MultiKeyEntry<V>> iterator() {
            return new EntryIterator();
        }
    }

    /**
     * Iterator implementation for MultiKeyMap entries.
     * Thread-safe - captures a snapshot of the buckets array at creation time.
     */
    private class EntryIterator implements java.util.Iterator<MultiKeyEntry<V>> {
        private final Object[] bucketSnapshot;  // Snapshot of buckets at iterator creation
        private int currentBucket = 0;
        private int currentChainIndex = 0;
        private MultiKeyEntry<V> nextEntry = null;

        EntryIterator() {
            // Capture snapshot of current buckets array for thread safety
            this.bucketSnapshot = buckets;
            advance();
        }

        @Override
        public boolean hasNext() {
            return nextEntry != null;
        }

        @Override
        public MultiKeyEntry<V> next() {
            if (nextEntry == null) {
                throw new NoSuchElementException();
            }
            
            MultiKeyEntry<V> result = nextEntry;
            advance();
            return result;
        }

        private void advance() {
            while (currentBucket < bucketSnapshot.length) {
                @SuppressWarnings("unchecked")
                MultiKey<V>[] chain = (MultiKey<V>[]) bucketSnapshot[currentBucket];
                
                if (chain != null && currentChainIndex < chain.length) {
                    MultiKey<V> key = chain[currentChainIndex];
                    nextEntry = new MultiKeyEntry<>(key.keys, key.value);
                    currentChainIndex++;
                    return;
                }
                
                // Move to next bucket
                currentBucket++;
                currentChainIndex = 0;
            }
            nextEntry = null;
        }
    }
    
    // ===== ADDITIONAL MAP-LIKE APIS (without key complications) =====
    
    /**
     * Returns true if this map contains no key-value mappings.
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * Returns true if this map maps one or more keys to the specified value.
     */
    public boolean containsValue(Object value) {
        for (MultiKeyEntry<V> entry : entries()) {
            if (Objects.equals(entry.value, value)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Removes the mapping for the specified N-dimensional key from this map if it is present.
     * 
     * @param keys the key components (can be varargs)
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public V remove(Object... keys) {
        // Special case: when remove(null) is called on varargs method, Java passes keys=null
        // We need to handle single null key directly
        if (keys == null) {
            return removeInternalDirect(null);
        }
        return removeInternal(keys);
    }
    
    /**
     * Map interface compatible remove method with auto-unpacking for arrays and collections.
     * This provides a great experience for Map users by automatically detecting
     * and unpacking arrays/collections into multi-key calls.
     * 
     * <p><strong>Auto-unpacking behavior:</strong></p>
     * <ul>
     *   <li>If key is an array → automatically unpacked into multiple keys</li>
     *   <li>If key is a Collection → automatically unpacked into multiple keys</li>
     *   <li>Otherwise → treated as single key</li>
     * </ul>
     * 
     * <p>Examples:</p>
     * <pre>{@code
     * Map<Object, Employee> map = new MultiKeyMap<>();
     * 
     * // Auto-unpacking: array becomes multi-key
     * String[] keys = {"dept", "engineering", "senior"};
     * Employee removed = map.remove(keys);  // Removes 3-key entry
     * 
     * // Auto-unpacking: Collection becomes multi-key
     * List<String> keyList = Arrays.asList("dept", "sales", "junior");
     * Employee removed2 = map.remove(keyList);  // Removes 3-key entry
     * 
     * // Single key: other objects removed normally  
     * Employee manager = map.remove("manager");  // Removes single-key entry
     * 
     * // Typed arrays also auto-unpack
     * int[] intKeys = {1, 2, 3};
     * Data removed3 = map.remove(intKeys);  // Removes 3-key entry
     * }</pre>
     * 
     * @param key single key, or array/Collection that will be auto-unpacked into multiple keys
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public V remove(Object key) {
        // Fast path for normal objects (most common case) - zero heap allocation
        if (key == null) {
            return removeInternalDirect(null);
        }
        
        Class<?> keyClass = key.getClass();
        if (keyClass.isArray()) {
            return removeFromArray(key);
        } else if (key instanceof Collection) {
            return removeFromCollection((Collection<?>) key);
        } else {
            // Normal object - most common case, optimized for zero heap allocation
            return removeInternalSingleKeyDirect(key);
        }
    }
    
    /**
     * Handle Collection keys with mode-based logic for remove operations.
     */
    private V removeFromCollection(Collection<?> collection) {
        switch (collectionKeyMode) {
            case MULTI_KEY_ONLY:
                return removeFromCollectionMultiKeyOnly(collection);
            case MULTI_KEY_FIRST:
                return removeFromCollectionMultiKeyFirst(collection);
            case COLLECTION_KEY_FIRST:
                return removeFromCollectionKeyFirst(collection);
            default:
                throw new IllegalStateException("Unknown CollectionKeyMode: " + collectionKeyMode);
        }
    }
    
    /**
     * Handle Array keys with mode-based logic for remove operations.
     */
    private V removeFromArray(Object array) {
        switch (collectionKeyMode) {
            case MULTI_KEY_ONLY:
                return removeFromArrayMultiKeyOnly(array);
            case MULTI_KEY_FIRST:
                return removeFromArrayMultiKeyFirst(array);
            case COLLECTION_KEY_FIRST:
                return removeFromArrayKeyFirst(array);
            default:
                throw new IllegalStateException("Unknown CollectionKeyMode: " + collectionKeyMode);
        }
    }
    
    // Collection remove mode implementations
    private V removeFromCollectionMultiKeyOnly(Collection<?> collection) {
        // Only try multi-key removal
        return removeInternalFromCollection(collection);
    }
    
    private V removeFromCollectionMultiKeyFirst(Collection<?> collection) {
        // Try multi-key first, then collection-as-key
        V result = removeInternalFromCollection(collection);
        if (result == null) {
            result = removeInternalDirect(collection);
        }
        return result;
    }
    
    private V removeFromCollectionKeyFirst(Collection<?> collection) {
        // Try collection-as-key first, then multi-key
        V result = removeInternalDirect(collection);
        if (result == null) {
            result = removeInternalFromCollection(collection);
        }
        return result;
    }
    
    // Array remove mode implementations
    private V removeFromArrayMultiKeyOnly(Object array) {
        if (array instanceof Object[]) {
            return removeInternal((Object[]) array);
        } else {
            return removeInternalFromTypedArray(array);
        }
    }
    
    private V removeFromArrayMultiKeyFirst(Object array) {
        // Try multi-key first, then array-as-key
        V result;
        if (array instanceof Object[]) {
            result = removeInternal((Object[]) array);
        } else {
            result = removeInternalFromTypedArray(array);
        }
        if (result == null) {
            result = removeInternalDirect(array);
        }
        return result;
    }
    
    private V removeFromArrayKeyFirst(Object array) {
        // Try array-as-key first, then multi-key
        V result = removeInternalDirect(array);
        if (result == null) {
            if (array instanceof Object[]) {
                result = removeInternal((Object[]) array);
            } else {
                result = removeInternalFromTypedArray(array);
            }
        }
        return result;
    }
    
    /**
     * Efficient Collection removal without array conversion.
     */
    private V removeInternalFromCollection(Collection<?> collection) {
        if (collection.isEmpty()) {
            return null;
        }
        
        // TODO: Implement efficient Collection-based removal similar to get
        // For now, convert to array (will optimize later)
        return removeInternal(collection.toArray());
    }
    
    /**
     * Direct single-key removal without heap allocation.
     */
    private V removeInternalSingleKeyDirect(Object key) {
        // Direct single key removal - zero allocation
        return removeInternalDirect(key);
    }
    
    

    @Override
    public void putAll(Map<? extends Object, ? extends V> m) {
        for (Map.Entry<? extends Object, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Uses a double-check locking pattern to avoid unnecessary synchronization
     * when a value is already present. If the key is absent or currently mapped
     * to {@code null}, the provided value is stored.
     *
     * @see Map#putIfAbsent(Object, Object)
     */
    @Override
    public V putIfAbsent(Object key, V value) {
        V existing = get(key);
        if (existing != null) {
            return existing;
        }

        synchronized (writeLock) {
            existing = get(key);
            if (existing == null) {
                put(key, value);
                return null;
            }
            return existing;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Performs a double-check locking pattern to avoid unnecessary
     * synchronization when the value already exists. If the value is absent
     * or {@code null}, the mapping function is invoked and the result stored
     * if non-null.
     *
     * @see Map#computeIfAbsent(Object, Function)
     */
    @Override
    public V computeIfAbsent(Object key, Function<? super Object, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction, "mappingFunction must not be null");

        V value = get(key);
        if (value != null) {
            return value;
        }

        synchronized (writeLock) {
            value = get(key);
            if (value == null) {
                V newValue = mappingFunction.apply(key);
                if (newValue != null) {
                    put(key, newValue);
                    return newValue;
                }
            }
            return value; // may be null or value from second read
        }
    }

    /**
     * Internal remove implementation that works with Object[] keys.
     */
    private V removeInternal(Object[] keys) {
        int hash = computeHash(keys);
        
        synchronized (writeLock) {
            int bucketIndex = hash & (buckets.length - 1);
            
            @SuppressWarnings("unchecked")
            MultiKey<V>[] chain = (MultiKey<V>[]) buckets[bucketIndex];
            
            if (chain == null) {
                return null;
            }
            
            // Find and remove the entry
            for (int i = 0; i < chain.length; i++) {
                MultiKey<V> existing = chain[i];
                if (existing.hash == hash && keysMatch(existing.keys, keys)) {
                    
                    V oldValue = existing.value;
                    
                    // Remove this entry by creating a new array without it
                    if (chain.length == 1) {
                        // Last entry in chain - remove the entire chain
                        buckets[bucketIndex] = null;
                    } else {
                        // Create a new chain without this entry
                        @SuppressWarnings("unchecked")
                        MultiKey<V>[] newChain = new MultiKey[chain.length - 1];
                        
                        // Copy entries before the removed one
                        System.arraycopy(chain, 0, newChain, 0, i);
                        
                        // Copy entries after the removed one
                        if (i < chain.length - 1) {
                            System.arraycopy(chain, i + 1, newChain, i, chain.length - i - 1);
                        }
                        
                        buckets[bucketIndex] = newChain;
                    }
                    
                    size--;
                    return oldValue;
                }
            }
        }
        
        return null;
    }
    
    
    /**
     * Returns true if this map contains a mapping for the specified N-dimensional key.
     * 
     * @param keys the key components (can be varargs)
     * @return true if a mapping exists for the key
     */
    public boolean containsKey(Object... keys) {
        // Special case: when containsKey(null) is called on varargs method, Java passes keys=null
        // We need to handle single null key directly
        if (keys == null) {
            return containsKeyInternalDirect(null);
        }
        return getInternal(keys) != null;
    }
    
    /**
     * Map interface compatible containsKey method.
     * Supports both single keys and N-dimensional keys via Object[] detection.
     * Uses efficient decision tree pattern: Normal objects first, then Arrays, then Collections.
     * 
     * @param key either a single key or an Object[] containing multiple keys
     * @return true if a mapping exists for the key
     */
    public boolean containsKey(Object key) {
        // Fast path for normal objects (most common case) - zero heap allocation
        if (key == null) {
            return containsKeyInternalDirect(null);
        }
        
        Class<?> keyClass = key.getClass();
        if (keyClass.isArray()) {
            return containsKeyFromArray(key);
        } else if (key instanceof Collection) {
            return containsKeyFromCollection((Collection<?>) key);
        } else {
            return containsKeyInternalDirect(key);
        }
    }
    
    
    /**
     * Internal containsKey implementation that works with Object[] keys.
     * Actually checks for key existence, not just non-null values.
     */
    private boolean containsKeyInternal(Object[] keys) {
        int hash = computeHash(keys);
        int bucketIndex = hash & (buckets.length - 1);
        
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) buckets[bucketIndex];
        
        if (chain == null) {
            return false;
        }
        
        for (MultiKey<V> existing : chain) {
            if (existing.hash == hash && keysMatch(existing.keys, keys)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Internal containsKey implementation for typed arrays.
     */
    private boolean containsKeyFromTypedArray(Object typedArray) {
        int length = Array.getLength(typedArray);
        Object[] keys = new Object[length];
        
        for (int i = 0; i < length; i++) {
            keys[i] = Array.get(typedArray, i);
        }
        
        return containsKeyInternal(keys);
    }
    
    /**
     * Handles containsKey for array keys with mode-based logic.
     */
    private boolean containsKeyFromArray(Object key) {
        if (key instanceof Object[]) {
            return containsKeyFromObjectArray((Object[]) key);
        } else {
            return containsKeyFromTypedArray(key);
        }
    }
    
    /**
     * Handles containsKey for Object[] keys with mode-based logic.
     */
    private boolean containsKeyFromObjectArray(Object[] key) {
        if (collectionKeyMode == CollectionKeyMode.MULTI_KEY_ONLY) {
            return containsKeyInternal(key);
        } else if (collectionKeyMode == CollectionKeyMode.MULTI_KEY_FIRST) {
            return containsKeyMultiFirst(key);
        } else { // COLLECTION_KEY_FIRST
            return containsKeyCollectionFirst(key);
        }
    }
    
    /**
     * Handles containsKey for Collection keys with mode-based logic.
     */
    private boolean containsKeyFromCollection(Collection<?> collection) {
        if (collection.isEmpty()) {
            return false;
        }
        
        if (collectionKeyMode == CollectionKeyMode.MULTI_KEY_ONLY) {
            return containsKeyFromCollectionAsMultiKey(collection);
        } else if (collectionKeyMode == CollectionKeyMode.MULTI_KEY_FIRST) {
            return containsKeyFromCollectionMultiFirst(collection);
        } else { // COLLECTION_KEY_FIRST
            return containsKeyFromCollectionCollectionFirst(collection);
        }
    }
    
    /**
     * Try multi-key first, then single-key for Object[] keys.
     */
    private boolean containsKeyMultiFirst(Object[] key) {
        // First try: multi-key lookup
        boolean found = containsKeyInternal(key);
        if (found) {
            return true;
        }
        
        // Second try: single-key lookup (direct, zero allocation)
        return containsKeyInternalDirect(key);
    }
    
    /**
     * Try single-key first, then multi-key for Object[] keys.
     */
    private boolean containsKeyCollectionFirst(Object[] key) {
        // First try: single-key lookup (direct, zero allocation)
        boolean found = containsKeyInternalDirect(key);
        if (found) {
            return true;
        }
        
        // Second try: multi-key lookup
        return containsKeyInternal(key);
    }
    
    /**
     * Multi-key only lookup for Collections.
     */
    private boolean containsKeyFromCollectionAsMultiKey(Collection<?> collection) {
        // Compute hash directly from Collection to avoid array allocation
        int hash = computeHashFromCollection(collection);
        int bucketIndex = hash & (buckets.length - 1);
        
        @SuppressWarnings("unchecked")
        MultiKey<V>[] chain = (MultiKey<V>[]) buckets[bucketIndex];
        
        if (chain == null) {
            return false;
        }
        
        // Scan the chain for exact match
        for (MultiKey<V> entry : chain) {
            if (entry.hash == hash && keysMatch(entry.keys, collection)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Try multi-key first, then single-key for Collections.
     */
    private boolean containsKeyFromCollectionMultiFirst(Collection<?> collection) {
        // First try: multi-key lookup
        boolean found = containsKeyFromCollectionAsMultiKey(collection);
        if (found) {
            return true;
        }
        
        // Second try: single-key lookup (direct, zero allocation)
        return containsKeyInternalDirect(collection);
    }
    
    /**
     * Try single-key first, then multi-key for Collections.
     */
    private boolean containsKeyFromCollectionCollectionFirst(Collection<?> collection) {
        // First try: single-key lookup (direct, zero allocation)
        boolean found = containsKeyInternalDirect(collection);
        if (found) {
            return true;
        }
        
        // Second try: multi-key lookup
        return containsKeyFromCollectionAsMultiKey(collection);
    }
    
    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        synchronized (writeLock) {
            Arrays.fill(buckets, null);
            size = 0;
            maxChainLength = 0;
        }
    }
    
    /**
     * Returns a Collection view of the values contained in this map.
     * The collection is backed by the map's current state snapshot.
     */
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (MultiKeyEntry<V> entry : entries()) {
            values.add(entry.value);
        }
        return values;
    }
    
    /**
     * Returns a Set view of the keys contained in this map.
     * For MultiKeyMap, keys can be single objects or Object[] arrays.
     * The set is backed by the map's current state snapshot.
     */
    @Override
    public Set<Object> keySet() {
        Set<Object> keys = new HashSet<>();
        for (MultiKeyEntry<V> entry : entries()) {
            // For Map interface compliance: treat length-1 arrays as single keys
            if (entry.keys.length == 1) {
                // Single key case - unwrap from array and NULL_SENTINEL if needed
                Object singleKey = entry.keys[0];
                Object originalKey = (singleKey == NULL_SENTINEL) ? null : singleKey;
                keys.add(originalKey);
            } else {
                // Multi-key case - add the Object[] array as the key
                keys.add(entry.keys);
            }
        }
        return keys;
    }
    
    /**
     * Returns a Set view of the mappings contained in this map.
     * Each entry represents a key-value mapping where the key can be
     * a single object or an Object[] array for multi-dimensional keys.
     */
    @Override
    public Set<Map.Entry<Object, V>> entrySet() {
        Set<Map.Entry<Object, V>> entrySet = new HashSet<>();
        for (MultiKeyEntry<V> multiEntry : entries()) {
            Object key;
            // For Map interface compliance: treat length-1 arrays as single keys
            if (multiEntry.keys.length == 1) {
                // Single key case - unwrap from array and NULL_SENTINEL if needed
                Object singleKey = multiEntry.keys[0];
                key = (singleKey == NULL_SENTINEL) ? null : singleKey;
            } else {
                // Multi-key case - use the Object[] array as the key
                key = multiEntry.keys;
            }
            
            entrySet.add(new SimpleEntry<>(key, multiEntry.value));
        }
        return entrySet;
    }
    
    /**
     * Returns the hash code value for this map.
     * The hash code is computed based on all key-value pairs.
     */
    @Override
    public int hashCode() {
        int hash = 0;
        for (MultiKeyEntry<V> entry : entries()) {
            Object key;
            // For Map interface compliance: treat length-1 arrays as single keys
            if (entry.keys.length == 1) {
                // Single key case - unwrap from array and NULL_SENTINEL if needed
                Object singleKey = entry.keys[0];
                key = (singleKey == NULL_SENTINEL) ? null : singleKey;
            } else {
                // Multi-key case - use Arrays.hashCode for the Object[] array
                key = Arrays.hashCode(entry.keys);
            }
            
            int keyHash = (key == null) ? 0 : key.hashCode();
            int valueHash = (entry.value == null) ? 0 : entry.value.hashCode();
            hash += keyHash ^ valueHash;
        }
        return hash;
    }
    
    /**
     * Compares the specified object with this map for equality.
     * Returns true if the given object is also a map and the two maps
     * represent the same mappings.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Map)) return false;
        
        Map<?, ?> other = (Map<?, ?>) obj;
        if (size() != other.size()) return false;
        
        try {
            for (MultiKeyEntry<V> entry : entries()) {
                Object key;
                // For Map interface compliance: treat length-1 arrays as single keys
                if (entry.keys.length == 1) {
                    // Single key case - unwrap from array and NULL_SENTINEL if needed
                    Object singleKey = entry.keys[0];
                    key = (singleKey == NULL_SENTINEL) ? null : singleKey;
                } else {
                    // Multi-key case - use the Object[] array as the key
                    key = entry.keys;
                }
                
                V value = entry.value;
                if (value == null) {
                    if (!(other.get(key) == null && other.containsKey(key))) {
                        return false;
                    }
                } else {
                    if (!value.equals(other.get(key))) {
                        return false;
                    }
                }
            }
        } catch (ClassCastException | NullPointerException e) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Returns a string representation of this map.
     * Shows the key-value mappings in the format {key1=value1, key2=value2}.
     * Handles self-references to prevent infinite recursion.
     */
    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        
        for (MultiKeyEntry<V> entry : entries()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            
            // Format the key for Map interface compliance
            if (entry.keys.length == 1) {
                // Single key case - unwrap from array and NULL_SENTINEL if needed
                Object singleKey = entry.keys[0];
                Object originalKey = (singleKey == NULL_SENTINEL) ? null : singleKey;
                appendSafeKey(sb, originalKey);
            } else {
                // Multi-key case - show as array with self-reference detection
                appendSafeMultiKey(sb, entry.keys);
            }
            
            sb.append('=');
            appendSafeValue(sb, entry.value);
        }
        
        return sb.append('}').toString();
    }
    
    private void appendSafeKey(StringBuilder sb, Object key) {
        if (key == this) {
            sb.append("(this Map)");
        } else {
            sb.append(key);
        }
    }
    
    private void appendSafeMultiKey(StringBuilder sb, Object[] keys) {
        sb.append('[');
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) sb.append(", ");
            if (keys[i] == this) {
                sb.append("(this Map)");
            } else {
                sb.append(keys[i]);
            }
        }
        sb.append(']');
    }
    
    private void appendSafeValue(StringBuilder sb, Object value) {
        if (value == this) {
            sb.append("(this Map)");
        } else {
            sb.append(value);
        }
    }
    
    /**
     * Simple implementation of Map.Entry for the entrySet() method.
     */
    private static class SimpleEntry<K, V> implements Map.Entry<K, V> {
        private final K key;
        private V value;
        
        public SimpleEntry(K key, V value) {
            this.key = key;
            this.value = value;
        }
        
        @Override
        public K getKey() {
            return key;
        }
        
        @Override
        public V getValue() {
            return value;
        }
        
        @Override
        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Map.Entry)) return false;
            Map.Entry<?, ?> other = (Map.Entry<?, ?>) obj;
            return Objects.equals(key, other.getKey()) && Objects.equals(value, other.getValue());
        }
        
        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value);
        }
    }
}