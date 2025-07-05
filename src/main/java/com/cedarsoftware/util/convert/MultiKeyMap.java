package com.cedarsoftware.util.convert;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 *   <li><b>High Performance:</b> Optimized hash computation and separate chaining for excellent performance</li>
 *   <li><b>Thread-Safe:</b> Lock-free reads with synchronized writes for maximum concurrency</li>
 *   <li><b>Map Interface Compatible:</b> Supports single-key operations via "coconut wrapper" mechanism</li>
 *   <li><b>Flexible API:</b> Varargs methods for convenient multi-key operations</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Create a multi-key map
 * MultiKeyMap<String> map = new MultiKeyMap<>(1024);
 * 
 * // Store values with different key dimensions
 * map.put("single-key", "value1");                         // 1D key
 * map.put("key1", "key2", "value2");                       // 2D key
 * map.put("key1", "key2", "key3", "value3");               // 3D key
 * map.put(new Object[]{"k1", "k2", "k3", "k4"}, "value4"); // 4D key via array
 * map.put("k1", "k2", "k3", "k4", "k5", "value5");         // 5D key via array
 * // and so on...unlimited
 *
 * // Retrieve values
 * String val1 = map.get("single-key");
 * String val2 = map.get("key1", "key2");
 * String val3 = map.get("key1", "key2", "key3");
 * String val4 = map.get(new Object[]{"k1", "k2", "k3", "k4"});
 * String val5 = map.get("k1", "k2", "k3", "k4", "k5);
 * // and so on...unlimited
 * }</pre>
 * 
 * <h3>Performance Characteristics:</h3>
 * <ul>
 *   <li><b>Time Complexity:</b> O(1) average case for get/put/remove operations</li>
 *   <li><b>Space Complexity:</b> O(n) where n is the number of stored key-value pairs</li>
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
    
    private volatile Object[] buckets;  // Array of MultiKey<V>[] (or null), Collection, String[] (typed array)
    private final Object writeLock = new Object();
    private volatile int size = 0;
    private volatile int maxChainLength = 0;
    private final float loadFactor;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f; // Same as HashMap default

    /**
     * Sentinel value for null keys, similar to ConcurrentHashMapNullSafe approach.
     * This allows us to store and retrieve null keys safely.
     */
    private static final Object NULL_SENTINEL = new Object();
    
    /**
     * Pre-allocated array for null key lookups to avoid heap allocation on get(null).
     * This provides zero-heap-allocation performance for the common null key case.
     */
    private static final Object[] NULL_KEY_ARRAY = {new SingleKeyWrapper(null)};
    
    /**
     * "Coconut wrapper" for single non-Object[] keys to achieve Map interface compliance.
     * When a single key (that's not already an Object[]) is stored, we wrap it in this
     * shell. When retrieving, we detect this wrapper and unwrap to return the original key.
     * Uses sentinel value for null keys like ConcurrentHashMapNullSafe.
     */
    private static final class SingleKeyWrapper {
        final Object innerKey;
        
        SingleKeyWrapper(Object key) {
            // Use sentinel for null keys to avoid null pointer issues
            this.innerKey = (key == null) ? NULL_SENTINEL : key;
        }
        
        /**
         * Get the original key, translating sentinel back to null.
         */
        Object getOriginalKey() {
            return (innerKey == NULL_SENTINEL) ? null : innerKey;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof MultiKeyMap.SingleKeyWrapper)) return false;
            SingleKeyWrapper other = (SingleKeyWrapper) obj;
            return innerKey == other.innerKey || innerKey.equals(other.innerKey);
        }
        
        @Override
        public int hashCode() {
            return innerKey.hashCode();
        }
        
        @Override
        public String toString() {
            return "CoconutWrapper[" + getOriginalKey() + "]";
        }
    }

    /**
     * Represents an N-dimensional key-value mapping.
     */
    private static final class MultiKey<V> {
        final Object[] keys;
        final int hash;
        final V value;
        
        MultiKey(Object[] keys, V value) {
            this.keys = keys != null ? keys.clone() : new Object[0]; // Defensive copy with null check
            this.hash = computeHash(this.keys);
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
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Load factor must be positive: " + loadFactor);
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must be non-negative: " + capacity);
        }
        
        this.buckets = new Object[capacity];
        this.loadFactor = loadFactor;
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
        if (keys == null || keys.length == 0) {
            return 0;
        }
        
        // Start with a non-zero seed for better distribution
        int hash = 1;
        
        for (Object key : keys) {
            int keyHash;
            if (key == null) {
                keyHash = 0;
            } else if (key instanceof Class) {
                // Use identity hash for Classes (faster than equals-based hash)
                keyHash = System.identityHashCode(key);
            } else {
                // Use standard hash code for other objects
                keyHash = key.hashCode();
            }
            
            hash = hash * 31 + keyHash;
        }
        
        // Aggressive bit mixing to break up patterns - MurmurHash3 inspired
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);
        
        return hash;
    }
    
    /**
     * Computes hash directly from Collection elements to avoid array allocation.
     * Uses same algorithm as computeHash for consistency.
     */
    private static int computeHashFromCollection(Collection<?> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        
        // Start with a non-zero seed for better distribution
        int hash = 1;
        
        for (Object key : keys) {
            int keyHash;
            if (key == null) {
                keyHash = 0;
            } else if (key instanceof Class) {
                // Use identity hash for Classes (faster than equals-based hash)
                keyHash = System.identityHashCode(key);
            } else {
                // Use standard hash code for other objects
                keyHash = key.hashCode();
            }
            
            hash = hash * 31 + keyHash;
        }
        
        // Aggressive bit mixing to break up patterns - MurmurHash3 inspired
        hash ^= (hash >>> 16);
        hash *= 0x85ebca6b;
        hash ^= (hash >>> 13);
        hash *= 0xc2b2ae35;
        hash ^= (hash >>> 16);
        
        return hash;
    }
    
    /**
     * Computes hash directly from typed array elements to avoid conversion.
     * Uses same algorithm as computeHash for consistency.
     */
    private static int computeHashFromTypedArray(Object typedArray) {
        if (typedArray == null) {
            return 0;
        }
        
        int length = Array.getLength(typedArray);
        if (length == 0) {
            return 0;
        }
        
        // Start with a non-zero seed for better distribution
        int hash = 1;
        
        for (int i = 0; i < length; i++) {
            Object key = Array.get(typedArray, i);
            int keyHash;
            if (key == null) {
                keyHash = 0;
            } else if (key instanceof Class) {
                // Use identity hash for Classes (faster than equals-based hash)
                keyHash = System.identityHashCode(key);
            } else {
                // Use standard hash code for other objects
                keyHash = key.hashCode();
            }
            
            hash = hash * 31 + keyHash;
        }
        
        // Aggressive bit mixing to break up patterns - MurmurHash3 inspired
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
        // Use pre-allocated array for zero heap allocation
        if (keys == null) {
            return getInternal(NULL_KEY_ARRAY);
        }
        return getInternal(keys);
    }
    
    /**
     * Map interface compatible get method with "coconut wrapper" unwrapping.
     * Supports both single keys and N-dimensional keys via Object[] detection.
     * 
     * @param key either a single key or an Object[] containing multiple keys
     * @return the value associated with the key, or null if not found
     */
    public V get(Object key) {
        if (key != null && key.getClass().isArray()) {
            if (key instanceof Object[]) {
                // Fast path: Object[] array - lookup directly
                return getInternal((Object[]) key);
            } else {
                // Typed array path: String[], int[], etc. - use reflection for zero conversion
                return getInternalFromTypedArray(key);
            }
        } else if (key == null) {
            // Zero heap allocation for null key
            return getInternal(NULL_KEY_ARRAY);
        } else {
            // Single key case: wrap in coconut shell and lookup
            SingleKeyWrapper wrappedKey = new SingleKeyWrapper(key);
            return getInternal(new Object[]{wrappedKey});
        }
    }
    
    /**
     * Gets the value for the given Collection-based multi-dimensional key.
     * This method provides zero-heap allocation by computing hash directly from Collection
     * and matching without array conversion. Collection elements are treated as key dimensions.
     * 
     * @param keys Collection containing the key components (elements become key dimensions)
     * @return the value associated with the key, or null if not found
     */
    public V get(Collection<?> keys) {
        if (keys == null) {
            // Delegate to single-key method for null handling
            return get((Object) null);
        }
        if (keys.isEmpty()) {
            return null;
        }
        
        // Compute hash directly from Collection to avoid array allocation
        int hash = computeHashFromCollection(keys);
        
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
            if (entry.hash == hash && keysEqualCollection(entry.keys, keys)) {
                return entry.value;
            }
        }
        
        return null;
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
            if (entry.hash == hash && keysEqualTypedArray(entry.keys, typedArray)) {
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
            if (entry.hash == hash && keysEqual(entry.keys, keys)) {
                return entry.value;
            }
        }
        
        return null;
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
            Object k1 = keys1[i];
            Object k2 = keys2[i];
            
            if (k1 == k2) {
                continue; // Same reference (includes null == null)
            }
            
            if (k1 == null || k2 == null) {
                return false; // One null, one not null
            }
            
            // For Classes, use identity comparison for performance
            if (k1 instanceof Class && k2 instanceof Class) {
                return false; // Already checked == above, so they're different Classes
            }
            
            // For other objects, use equals
            if (!k1.equals(k2)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Compares stored Object[] keys with Collection keys for equality.
     * Uses same comparison logic as keysEqual for consistency.
     */
    private static boolean keysEqualCollection(Object[] storedKeys, Collection<?> collectionKeys) {
        if (storedKeys == null || collectionKeys == null) {
            return false;
        }
        
        if (storedKeys.length != collectionKeys.size()) {
            return false;
        }
        
        int i = 0;
        for (Object collectionKey : collectionKeys) {
            Object storedKey = storedKeys[i++];
            
            if (storedKey == collectionKey) {
                continue; // Same reference (includes null == null)
            }
            
            if (storedKey == null || collectionKey == null) {
                return false; // One null, one not null
            }
            
            // For Classes, use identity comparison for performance
            if (storedKey instanceof Class && collectionKey instanceof Class) {
                return false; // Already checked == above, so they're different Classes
            }
            
            // For other objects, use equals
            if (!storedKey.equals(collectionKey)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Compares stored Object[] keys with typed array keys for equality.
     * Uses same comparison logic as keysEqual for consistency.
     */
    private static boolean keysEqualTypedArray(Object[] storedKeys, Object typedArray) {
        if (storedKeys == null || typedArray == null) {
            return false;
        }
        
        int arrayLength = Array.getLength(typedArray);
        if (storedKeys.length != arrayLength) {
            return false;
        }
        
        for (int i = 0; i < arrayLength; i++) {
            Object storedKey = storedKeys[i];
            Object arrayKey = Array.get(typedArray, i);
            
            if (storedKey == arrayKey) {
                continue; // Same reference (includes null == null)
            }
            
            if (storedKey == null || arrayKey == null) {
                return false; // One null, one not null
            }
            
            // For Classes, use identity comparison for performance
            if (storedKey instanceof Class && arrayKey instanceof Class) {
                return false; // Already checked == above, so they're different Classes
            }
            
            // For other objects, use equals
            if (!storedKey.equals(arrayKey)) {
                return false;
            }
        }
        
        return true;
    }
    
    
    /**
     * Stores a conversion function for the given N-dimensional key.
     * If the key already exists, updates the conversion function.
     * 
     * @param keys the key components as an array
     * @param value the value to store
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public V put(Object[] keys, V value) {
        // Special case: when put(null, value) is called, Java resolves to this method
        // We need to handle single null key using coconut wrapper
        if (keys == null) {
            SingleKeyWrapper wrappedKey = new SingleKeyWrapper(null);
            return putInternal(new Object[]{wrappedKey}, value);
        }
        return putInternal(keys, value);
    }
    
    // Overloaded put methods for clean caller code (following JDK pattern)
    // Note: Single-key put(Object, V) already exists for Map interface compatibility
    
    public V put(Object key1, Object key2, V value) {
        return putInternal(new Object[]{key1, key2}, value);
    }
    
    public V put(Object key1, Object key2, Object key3, V value) {
        return putInternal(new Object[]{key1, key2, key3}, value);
    }
    
    public V put(Object key1, Object key2, Object key3, Object key4, V value) {
        return putInternal(new Object[]{key1, key2, key3, key4}, value);
    }
    
    public V put(Object key1, Object key2, Object key3, Object key4, Object key5, V value) {
        return putInternal(new Object[]{key1, key2, key3, key4, key5}, value);
    }
    
    public V put(Object key1, Object key2, Object key3, Object key4, Object key5, Object key6, V value) {
        return putInternal(new Object[]{key1, key2, key3, key4, key5, key6}, value);
    }
    
    public V put(Object key1, Object key2, Object key3, Object key4, Object key5, Object key6, Object key7, V value) {
        return putInternal(new Object[]{key1, key2, key3, key4, key5, key6, key7}, value);
    }
    
    public V put(Object key1, Object key2, Object key3, Object key4, Object key5, Object key6, Object key7, Object key8, V value) {
        return putInternal(new Object[]{key1, key2, key3, key4, key5, key6, key7, key8}, value);
    }
    
    public V put(Object key1, Object key2, Object key3, Object key4, Object key5, Object key6, Object key7, Object key8, Object key9, V value) {
        return putInternal(new Object[]{key1, key2, key3, key4, key5, key6, key7, key8, key9}, value);
    }
    
    public V put(Object key1, Object key2, Object key3, Object key4, Object key5, Object key6, Object key7, Object key8, Object key9, Object key10, V value) {
        return putInternal(new Object[]{key1, key2, key3, key4, key5, key6, key7, key8, key9, key10}, value);
    }
    
    /**
     * Map interface compatible put method with "coconut wrapper" for single keys.
     * Supports both single keys and N-dimensional keys via Object[] detection.
     * 
     * @param key either a single key or an Object[] containing multiple keys
     * @param value the value to store
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public V put(Object key, V value) {
        if (key != null && key.getClass().isArray()) {
            if (key instanceof Object[]) {
                // N-Key case: key is Object[] - store directly
                return putInternal((Object[]) key, value);
            } else {
                // N-Key case: key is typed array - convert to Object[] first
                return putInternalFromTypedArray(key, value);
            }
        } else {
            // Single key case: wrap in coconut shell and store
            SingleKeyWrapper wrappedKey = new SingleKeyWrapper(key);
            return putInternal(new Object[]{wrappedKey}, (V) value);
        }
    }
    
    /**
     * Internal put implementation that works with Object[] keys.
     */
    private V putInternal(Object[] keys, V value) {
        int hash = computeHash(keys);
        MultiKey<V> newKey = new MultiKey<>(keys, value);
        
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
                    if (existing.hash == hash && keysEqual(existing.keys, keys)) {
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
     * Creates a new single-element chain array.
     */
    @SuppressWarnings("unchecked")
    private MultiKey<V>[] createChain(MultiKey<V> key) {
        MultiKey<V>[] chain = new MultiKey[1];
        chain[0] = key;
        return chain;
    }
    
    /**
     * Grows an existing chain by one element.
     * Creates a new array with the new key appended.
     */
    @SuppressWarnings("unchecked")
    private MultiKey<V>[] growChain(MultiKey<V>[] oldChain, MultiKey<V> newKey) {
        int oldLength = oldChain.length;
        MultiKey<V>[] newChain = new MultiKey[oldLength + 1];
        
        // Copy existing entries
        System.arraycopy(oldChain, 0, newChain, 0, oldLength);
        
        // Add new entry at the end
        newChain[oldLength] = newKey;
        
        return newChain;
    }
    
    /**
     * Resizes the hash table to double its current capacity and rehashes all entries.
     * This method must be called from within a synchronized block.
     */
    private void resize() {
        int oldCapacity = buckets.length;
        int newCapacity = oldCapacity * 2;
        
        
        // Save old buckets
        Object[] oldBuckets = buckets;
        
        // Create new buckets array
        buckets = new Object[newCapacity];
        int oldSize = size;
        size = 0;
        int oldMaxChainLength = maxChainLength;
        maxChainLength = 0;
        
        // Rehash all entries from old buckets
        for (Object bucket : oldBuckets) {
            if (bucket != null) {
                @SuppressWarnings("unchecked")
                MultiKey<V>[] chain = (MultiKey<V>[]) bucket;
                
                // Rehash each entry in the chain
                for (MultiKey<V> key : chain) {
                    // Recompute bucket index for new capacity
                    int newBucketIndex = key.hash & (buckets.length - 1);
                    
                    @SuppressWarnings("unchecked")
                    MultiKey<V>[] newChain = (MultiKey<V>[]) buckets[newBucketIndex];
                    
                    if (newChain == null) {
                        // Create new single-element chain
                        newChain = createChain(key);
                        buckets[newBucketIndex] = newChain;
                    } else {
                        // Add to existing chain
                        newChain = growChain(newChain, key);
                        buckets[newBucketIndex] = newChain;
                    }
                    
                    // Update max chain length
                    if (newChain.length > maxChainLength) {
                        maxChainLength = newChain.length;
                    }
                    
                    size++;
                }
            }
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

        MultiKeyEntry(Object[] keys, V value) {
            this.keys = keys.clone(); // Defensive copy
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
                throw new java.util.NoSuchElementException();
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
        // We need to handle single null key using coconut wrapper
        if (keys == null) {
            SingleKeyWrapper wrappedKey = new SingleKeyWrapper(null);
            return removeInternal(new Object[]{wrappedKey});
        }
        return removeInternal(keys);
    }
    
    /**
     * Map interface compatible remove method.
     * Supports both single keys and N-dimensional keys via Object[] detection.
     * 
     * @param key either a single key or an Object[] containing multiple keys
     * @return the previous value associated with the key, or null if there was no mapping
     */
    public V remove(Object key) {
        if (key != null && key.getClass().isArray()) {
            // N-Key case: key is Object[] - remove directly
            return removeInternal((Object[]) key);
        } else {
            // Single key case: wrap in coconut shell and remove
            SingleKeyWrapper wrappedKey = new SingleKeyWrapper(key);
            return removeInternal(new Object[]{wrappedKey});
        }
    }

    @Override
    public void putAll(Map<? extends Object, ? extends V> m) {
        for (Map.Entry<? extends Object, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
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
                if (existing.hash == hash && keysEqual(existing.keys, keys)) {
                    
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
        // We need to handle single null key using coconut wrapper
        if (keys == null) {
            SingleKeyWrapper wrappedKey = new SingleKeyWrapper(null);
            return getInternal(new Object[]{wrappedKey}) != null;
        }
        return getInternal(keys) != null;
    }
    
    /**
     * Map interface compatible containsKey method.
     * Supports both single keys and N-dimensional keys via Object[] detection.
     * 
     * @param key either a single key or an Object[] containing multiple keys
     * @return true if a mapping exists for the key
     */
    public boolean containsKey(Object key) {
        if (key != null && key.getClass().isArray()) {
            if (key instanceof Object[]) {
                // N-Key case: key is Object[] - check directly
                return containsKeyInternal((Object[]) key);
            } else {
                // N-Key case: key is typed array - convert to Object[] first
                return containsKeyFromTypedArray(key);
            }
        } else {
            // Single key case: wrap in coconut shell and check
            SingleKeyWrapper wrappedKey = new SingleKeyWrapper(key);
            return containsKeyInternal(new Object[]{wrappedKey});
        }
    }
    
    /**
     * Returns true if this map contains a mapping for the specified Collection-based key.
     * 
     * @param keys the key components as a Collection
     * @return true if a mapping exists for the key
     */
    public boolean containsKey(Collection<?> keys) {
        if (keys == null) {
            return false;
        }
        
        // Convert Collection to Object[] and check
        Object[] keyArray = keys.toArray();
        return containsKeyInternal(keyArray);
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
            if (existing.hash == hash && keysEqual(existing.keys, keys)) {
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
            // For single-key entries (coconut wrapped), unwrap the key
            if (entry.keys.length == 1 && entry.keys[0] instanceof MultiKeyMap.SingleKeyWrapper) {
                SingleKeyWrapper wrapper = (SingleKeyWrapper) entry.keys[0];
                Object originalKey = (wrapper.innerKey == NULL_SENTINEL) ? null : wrapper.innerKey;
                keys.add(originalKey);
            } else {
                // For multi-key entries, add the Object[] array as the key
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
            // For single-key entries (coconut wrapped), unwrap the key
            if (multiEntry.keys.length == 1 && multiEntry.keys[0] instanceof MultiKeyMap.SingleKeyWrapper) {
                SingleKeyWrapper wrapper = (SingleKeyWrapper) multiEntry.keys[0];
                key = (wrapper.innerKey == NULL_SENTINEL) ? null : wrapper.innerKey;
            } else {
                // For multi-key entries, use the Object[] array as the key
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
            // For single-key entries (coconut wrapped), unwrap the key
            if (entry.keys.length == 1 && entry.keys[0] instanceof MultiKeyMap.SingleKeyWrapper) {
                SingleKeyWrapper wrapper = (SingleKeyWrapper) entry.keys[0];
                key = (wrapper.innerKey == NULL_SENTINEL) ? null : wrapper.innerKey;
            } else {
                // For multi-key entries, use the Object[] array
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
                // For single-key entries (coconut wrapped), unwrap the key
                if (entry.keys.length == 1 && entry.keys[0] instanceof MultiKeyMap.SingleKeyWrapper) {
                    SingleKeyWrapper wrapper = (SingleKeyWrapper) entry.keys[0];
                    key = (wrapper.innerKey == NULL_SENTINEL) ? null : wrapper.innerKey;
                } else {
                    // For multi-key entries, use the Object[] array
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
            
            // Format the key
            if (entry.keys.length == 1 && entry.keys[0] instanceof MultiKeyMap.SingleKeyWrapper) {
                // Single-key entry (coconut wrapped)
                SingleKeyWrapper wrapper = (SingleKeyWrapper) entry.keys[0];
                Object originalKey = (wrapper.innerKey == NULL_SENTINEL) ? null : wrapper.innerKey;
                sb.append(originalKey);
            } else {
                // Multi-key entry - show as array
                sb.append(Arrays.toString(entry.keys));
            }
            
            sb.append('=').append(entry.value);
        }
        
        return sb.append('}').toString();
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