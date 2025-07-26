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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * High-performance N-dimensional key-value Map implementation using separate chaining.
 *
 * <p>MultiKeyMap allows storing and retrieving values using multiple keys. Unlike traditional maps that
 * use a single key, this map can handle keys with any number of components, making it ideal for complex
 * lookup scenarios.</p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><b>N-Dimensional Keys:</b> Support for keys with any number of components (1, 2, 3, ... N).</li>
 *   <li><b>High Performance:</b> Zero-allocation polymorphic storage and optimized hash computation — no GC/heap used for "gets" in flat cases.</li>
 *   <li><b>Thread-Safe:</b> Lock-free reads with auto-tuned stripe locking that scales with your server, similar to ConcurrentHashMap.</li>
 *   <li><b>Map Interface Compatible:</b> Supports single-key operations via the standard Map interface (get()/put() automatically unpack Collections/Arrays into multi-keys).</li>
 *   <li><b>Flexible API:</b> Var-args methods for convenient multi-key operations (getMultiKey()/putMultiKey() with many keys).</li>
 *   <li><b>Smart Collection Handling:</b> Configurable behavior for Collections — change the default automatic unpacking capability as needed.</li>
 *   <li><b>N-Dimensional Array Expansion:</b> Nested arrays of any depth are automatically flattened recursively into multi-keys, with optional structure preservation.</li>
 * </ul>
 *
 * <p>For detailed usage examples, see the class documentation in the source code.</p>
 *
 * @param <V> the type of values stored in the map
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * <br>
 * Copyright (c) Cedar Software LLC
 * <br><br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <br><br>
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 * <br><br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public final class MultiKeyMap<V> implements ConcurrentMap<Object, V> {

    private static final Logger LOG = Logger.getLogger(MultiKeyMap.class.getName());

    static {
        // Assuming LoggingConfig is defined elsewhere
        // LoggingConfig.init();
    }

    // Sentinels as private Objects for safety and zero collision risk
    private static final Object OPEN = new Object();
    private static final Object CLOSE = new Object();
    private static final Object NULL_SENTINEL = new Object();

    // Emojis for debug output (professional yet intuitive)
    private static final String EMOJI_OPEN = "📂";  // Open folder for nesting start
    private static final String EMOJI_CLOSE = "📁"; // Closed folder for nesting end
    private static final String EMOJI_CYCLE = "♻️"; // Recycle for cycles
    private static final String EMOJI_EMPTY = "∅";  // Empty set for null/empty
    private static final String EMOJI_KEY = "🔑";   // Key for single keys
    private static final String EMOJI_ARRAY = "[]"; // Brackets for arrays
    private static final String EMOJI_COLLECTION = "()"; // Braces for collections

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

    public enum CollectionKeyMode {
        COLLECTIONS_EXPANDED,
        COLLECTIONS_NOT_EXPANDED
    }

    private volatile Object[] buckets;
    private final AtomicInteger atomicSize = new AtomicInteger(0);
    private volatile int maxChainLength = 0;
    private final float loadFactor;
    private final CollectionKeyMode collectionKeyMode;
    private final boolean flattenDimensions;
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

    public MultiKeyMap(int capacity, float loadFactor, CollectionKeyMode collectionKeyMode, boolean flattenDimensions) {
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Load factor must be positive: " + loadFactor);
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must be non-negative: " + capacity);
        }

        this.buckets = new Object[capacity];
        this.loadFactor = loadFactor;
        this.collectionKeyMode = collectionKeyMode != null ? collectionKeyMode : CollectionKeyMode.COLLECTIONS_EXPANDED;
        this.flattenDimensions = flattenDimensions;

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

    // Convenience constructors (as in original)
    public MultiKeyMap(int capacity, float loadFactor) {
        this(capacity, loadFactor, CollectionKeyMode.COLLECTIONS_EXPANDED, false);
    }

    public MultiKeyMap(int capacity, float loadFactor, CollectionKeyMode collectionKeyMode) {
        this(capacity, loadFactor, collectionKeyMode, false);
    }

    public MultiKeyMap() {
        this(16);
    }

    public MultiKeyMap(int capacity) {
        this(capacity, DEFAULT_LOAD_FACTOR);
    }

    public MultiKeyMap(CollectionKeyMode collectionKeyMode, boolean flattenDimensions) {
        this(16, DEFAULT_LOAD_FACTOR, collectionKeyMode, flattenDimensions);
    }

    public MultiKeyMap(boolean flattenDimensions) {
        this(16, DEFAULT_LOAD_FACTOR, CollectionKeyMode.COLLECTIONS_EXPANDED, flattenDimensions);
    }

    public CollectionKeyMode getCollectionKeyMode() {
        return collectionKeyMode;
    }

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

    public V getMultiKey(Object... keys) {
        if (keys == null || keys.length == 0) return get(null);
        if (keys.length == 1) return get(keys[0]);
        return get(keys);  // Let get()'s normalizeLookup() handle everything!
    }

    public V get(Object key) {
        MultiKey<V> entry = findEntry(key);
        return entry != null ? entry.value : null;
    }

    public V putMultiKey(V value, Object... keys) {
        if (keys == null || keys.length == 0) return put(null, value);
        if (keys.length == 1) return put(keys[0], value);
        return put(keys, value);  // Let put()'s normalization handle everything!
    }

    public V put(Object key, V value) {
        MultiKey<V> newKey = createMultiKey(key, value);
        return putInternal(newKey);
    }
    
    /**
     * Creates a MultiKey from a key, normalizing it first.
     * Used by put() and remove() operations that need MultiKey objects.
     * @param key the key to normalize
     * @param value the value (can be null for remove operations)
     * @return a MultiKey object with normalized key and computed hash
     */
    private MultiKey<V> createMultiKey(Object key, V value) {
        int[] hashPass = new int[1];
        Object normalizedKey = normalizeLookup(key, hashPass, true); // true = make defensive copy for storage
        return new MultiKey<>(normalizedKey, hashPass[0], value);
    }

    private Object normalizeLookup(Object key, int[] hashPass) {
        return normalizeLookup(key, hashPass, false); // false = no defensive copy needed for lookup
    }

    private Object normalizeLookup(Object key, int[] hashPass, boolean makeDefensiveCopy) {
        // Handle null case
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
        return process1DCollection(coll, hashPass, makeDefensiveCopy);
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
                    // Recompute hash for the single element to match simple object case
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
    
    private Object process1DCollection(Collection<?> coll, int[] hashPass, boolean makeDefensiveCopy) {
        if (coll.isEmpty()) {
            hashPass[0] = 0;
            return coll;
        }
        
        // Check if truly 1D while computing hash
        int h = 1;
        boolean is1D = true;
        
        for (Object e : coll) {
            h = h * 31 + computeElementHash(e);
            if (e != null && (e.getClass().isArray() || e instanceof Collection)) {
                is1D = false;
                break;
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
                    // Recompute hash for the single element to match simple object case
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
                    // Recompute hash for the single element to match simple object case
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
            // Recompute hash for the single element to match simple object case
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

        if (storedSingle) return Objects.equals(stored == NULL_SENTINEL ? null : stored, lookup == NULL_SENTINEL ? null : lookup);

        // Multi-key match
        int storedLen = stored instanceof Collection ? ((Collection<?>) stored).size() : Array.getLength(stored);
        int lookupLen = lookup instanceof Collection ? ((Collection<?>) lookup).size() : Array.getLength(lookup);

        if (storedLen != lookupLen) return false;

        // Type-specific fast paths - use exact class matching to avoid instanceof hierarchy issues
        Class<?> storedClass = stored.getClass();
        Class<?> lookupClass = lookup.getClass();

        if (storedClass == Object[].class && lookupClass == Object[].class) {
            Object[] s = (Object[]) stored;
            Object[] l = (Object[]) lookup;
            for (int i = 0; i < storedLen; i++) if (!Objects.equals(s[i], l[i])) return false;
            return true;
        }
        
        if (storedClass == String[].class && lookupClass == String[].class) {
            String[] s = (String[]) stored;
            String[] l = (String[]) lookup;
            for (int i = 0; i < storedLen; i++) if (!Objects.equals(s[i], l[i])) return false;
            return true;
        }

        if (storedClass == int[].class && lookupClass == int[].class) {
            int[] s = (int[]) stored;
            int[] l = (int[]) lookup;
            for (int i = 0; i < storedLen; i++) if (s[i] != l[i]) return false;
            return true;
        }

        if (storedClass == long[].class && lookupClass == long[].class) {
            long[] s = (long[]) stored;
            long[] l = (long[]) lookup;
            for (int i = 0; i < storedLen; i++) if (s[i] != l[i]) return false;
            return true;
        }

        if (storedClass == double[].class && lookupClass == double[].class) {
            double[] s = (double[]) stored;
            double[] l = (double[]) lookup;
            for (int i = 0; i < storedLen; i++) if (s[i] != l[i]) return false;
            return true;
        }

        if (storedClass == boolean[].class && lookupClass == boolean[].class) {
            boolean[] s = (boolean[]) stored;
            boolean[] l = (boolean[]) lookup;
            for (int i = 0; i < storedLen; i++) if (s[i] != l[i]) return false;
            return true;
        }

        // Cross-type or general
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
        V old = null;
        boolean resize = false;

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
            maxChainLength = Math.max(maxChainLength, 1);
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
        maxChainLength = Math.max(maxChainLength, newChain.length);
        return null;
    }

    public boolean containsMultiKey(Object... keys) {
        if (keys == null || keys.length == 0) return containsKey(null);
        if (keys.length == 1) return containsKey(keys[0]);
        return containsKey(keys);  // Let containsKey()'s normalization handle everything!
    }

    public boolean containsKey(Object key) {
        return findEntry(key) != null;
    }

    public V removeMultiKey(Object... keys) {
        if (keys == null || keys.length == 0) return remove(null);
        if (keys.length == 1) return remove(keys[0]);
        return remove(keys);  // Let remove()'s normalization handle everything!
    }

    public V remove(Object key) {
        MultiKey<V> removeKey = createMultiKey(key, null);
        return removeInternal(removeKey);
    }

    private V removeInternal(MultiKey<V> removeKey) {
        int hash = removeKey.hash;
        ReentrantLock lock = getStripeLock(hash);
        int stripe = hash & STRIPE_MASK;
        boolean contended = lock.hasQueuedThreads();
        V old = null;

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
            maxChainLength = newMax;
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

    public int size() {
        return atomicSize.get();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void clear() {
        withAllStripeLocks(() -> {
            Arrays.fill(buckets, null);
            atomicSize.set(0);
            maxChainLength = 0;
        });
    }

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

    public Set<Object> keySet() {
        Set<Object> set = new HashSet<>();
        for (MultiKeyEntry<V> e : entries()) {
            set.add(e.keys.length == 1 ? (e.keys[0] == NULL_SENTINEL ? null : e.keys[0]) : e.keys);
        }
        return set;
    }

    public Collection<V> values() {
        List<V> vals = new ArrayList<>();
        for (MultiKeyEntry<V> e : entries()) vals.add(e.value);
        return vals;
    }

    public Set<Map.Entry<Object, V>> entrySet() {
        Set<Map.Entry<Object, V>> set = new HashSet<>();
        for (MultiKeyEntry<V> e : entries()) {
            Object k = e.keys.length == 1 ? (e.keys[0] == NULL_SENTINEL ? null : e.keys[0]) : e.keys;
            set.add(new AbstractMap.SimpleEntry<>(k, e.value));
        }
        return set;
    }

    public void putAll(Map<?, ? extends V> m) {
        for (Map.Entry<?, ? extends V> e : m.entrySet()) put(e.getKey(), e.getValue());
    }

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

    public V computeIfAbsent(Object key, Function<? super Object, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v = get(key);
        if (v != null) return v;
        int[] hashPass = new int[1];
        normalizeLookup(key, hashPass);
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

    public V computeIfPresent(Object key, BiFunction<? super Object, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V old = get(key);
        if (old == null) return null;
        int[] hashPass = new int[1];
        normalizeLookup(key, hashPass);
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

    public V compute(Object key, BiFunction<? super Object, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        int[] hashPass = new int[1];
        normalizeLookup(key, hashPass);
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

    public V merge(Object key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(remappingFunction);
        int[] hashPass = new int[1];
        normalizeLookup(key, hashPass);
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

    public boolean remove(Object key, Object value) {
        int[] hashPass = new int[1];
        normalizeLookup(key, hashPass);
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

    public V replace(Object key, V value) {
        int[] hashPass = new int[1];
        normalizeLookup(key, hashPass);
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

    public boolean replace(Object key, V oldValue, V newValue) {
        int[] hashPass = new int[1];
        normalizeLookup(key, hashPass);
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

    public int hashCode() {
        int h = 0;
        for (MultiKeyEntry<V> e : entries()) {
            Object k = e.keys.length == 1 ? (e.keys[0] == NULL_SENTINEL ? null : e.keys[0]) : Arrays.hashCode(e.keys);
            h += (k == null ? 0 : k.hashCode()) ^ (e.value == null ? 0 : e.value.hashCode());
        }
        return h;
    }

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

    public String toString() {
        if (isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{\n");
        boolean first = true;
        for (MultiKeyEntry<V> e : entries()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("  ");  // Two-space indentation
            String keyStr = dumpExpandedKey(e.keys, true, this);
            // Remove trailing comma and space if present
            if (keyStr.endsWith(", ")) {
                keyStr = keyStr.substring(0, keyStr.length() - 2);
            }
            sb.append(keyStr).append(" → ");
            // Handle self-reference in values
            if (e.value == this) {
                sb.append("(this Map)");
            } else {
                sb.append(e.value);
            }
        }
        return sb.append("\n}").toString();
    }

    public String dumpExpandedKey(Object key) {
        return dumpExpandedKey(key, false, null);
    }

    private String dumpExpandedKey(Object key, boolean forToString, MultiKeyMap<?> selfMap) {
        if (key == null) return EMOJI_EMPTY;
        if (key == NULL_SENTINEL) return EMOJI_EMPTY;
        if (!(key.getClass().isArray() || key instanceof Collection)) {
            // Handle self-reference in single keys
            if (selfMap != null && key == selfMap) return EMOJI_KEY + "(this Map)";
            return EMOJI_KEY + key.toString();
        }

        // Special case: single-element arrays should be treated as single keys
        if (key.getClass().isArray()) {
            int len = Array.getLength(key);
            if (len == 1) {
                Object element = Array.get(key, 0);
                if (element == NULL_SENTINEL) return EMOJI_KEY + EMOJI_EMPTY;
                if (selfMap != null && element == selfMap) return EMOJI_KEY + "(this Map)";
                return EMOJI_KEY + (element != null ? element.toString() : "null");
            }
        } else if (key instanceof Collection) {
            Collection<?> coll = (Collection<?>) key;
            if (coll.size() == 1) {
                Object element = coll.iterator().next();
                if (element == NULL_SENTINEL) return EMOJI_KEY + EMOJI_EMPTY;
                if (selfMap != null && element == selfMap) return EMOJI_KEY + "(this Map)";
                return EMOJI_KEY + (element != null ? element.toString() : "null");
            }
        }

        // For multi-key arrays/collections, use format: 🔑[key1, key2, key3]
        StringBuilder sb = new StringBuilder();
        sb.append(EMOJI_KEY).append("[");
        
        if (key.getClass().isArray()) {
            int len = Array.getLength(key);
            for (int i = 0; i < len; i++) {
                if (i > 0) sb.append(", ");
                Object element = Array.get(key, i);
                if (element == NULL_SENTINEL) sb.append(EMOJI_EMPTY);
                else if (selfMap != null && element == selfMap) sb.append("(this Map)");
                else sb.append(element != null ? element.toString() : "null");
            }
        } else if (key instanceof Collection) {
            Collection<?> coll = (Collection<?>) key;
            int i = 0;
            for (Object element : coll) {
                if (i > 0) sb.append(", ");
                if (element == NULL_SENTINEL) sb.append(EMOJI_EMPTY);
                else if (selfMap != null && element == selfMap) sb.append("(this Map)");
                else sb.append(element != null ? element.toString() : "null");
                i++;
            }
        }
        
        sb.append("]");
        return sb.toString();
    }

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

    private static String dumpExpandedKeyStatic(Object key, boolean forToString, MultiKeyMap<?> selfMap) {
        if (key == null) return EMOJI_EMPTY;
        if (key == NULL_SENTINEL) return EMOJI_EMPTY;
        if (!(key.getClass().isArray() || key instanceof Collection)) {
            // Handle self-reference in single keys
            if (selfMap != null && key == selfMap) return EMOJI_KEY + "(this Map)";
            return EMOJI_KEY + key.toString();
        }

        List<Object> expanded = new ArrayList<>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        int[] dummyHash = new int[]{1};  // We don't need the hash for debug output
        expandAndHash(key, expanded, visited, dummyHash, false);  // For debug, always preserve structure (false for flatten)

        StringBuilder sb = new StringBuilder();
        for (Object e : expanded) {
            if (e == OPEN) sb.append(EMOJI_OPEN);
            else if (e == CLOSE) sb.append(EMOJI_CLOSE);
            else if (visited.containsKey(e)) sb.append(EMOJI_CYCLE);
            else if (e == NULL_SENTINEL) sb.append(EMOJI_EMPTY).append(forToString ? ", " : " ");
            else if (selfMap != null && e == selfMap) sb.append("(this Map)").append(forToString ? ", " : " ");
            else sb.append(e).append(forToString ? ", " : " ");
        }
        return sb.toString().trim();
    }
}