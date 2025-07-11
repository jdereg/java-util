package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A wrapper around a {@link Map} that tracks which keys have been accessed via {@code get} or {@code containsKey} methods.
 * This is useful for scenarios where it's necessary to monitor usage patterns of keys in a map,
 * such as identifying unused entries or optimizing memory usage by expunging rarely accessed keys.
 * 
 * <p>When the backing map is a {@link MultiKeyMap}, this map also supports multi-key operations
 * and automatic array/collection expansion with proper access tracking.</p>
 *
 * <p>
 * <b>Usage Example:</b>
 * </p>
 * <pre>{@code
 * Map<String, Integer> originalMap = new HashMap<>();
 * originalMap.put("apple", 1);
 * originalMap.put("banana", 2);
 * originalMap.put("cherry", 3);
 *
 * TrackingMap<String, Integer> trackingMap = new TrackingMap<>(originalMap);
 *
 * // Access some keys
 * trackingMap.get("apple");
 * trackingMap.containsKey("banana");
 *
 * // Expunge unused keys
 * trackingMap.expungeUnused();
 *
 * // Now, "cherry" has been removed as it was not accessed
 * LOG.info(trackingMap.keySet()); // Outputs: [apple, banana]
 * }</pre>
 *
 * <p>
 * <b>Thread Safety:</b> This class is thread-safe when wrapping concurrent map implementations 
 * ({@link ConcurrentMap}, {@link ConcurrentNavigableMap}). The thread safety is provided by using
 * a concurrent tracking set and delegating all operations to the underlying concurrent map. 
 * When wrapping non-concurrent maps, external synchronization is required.
 * </p>
 * 
 * <p>
 * <b>Concurrent Operations:</b> When wrapping a {@link ConcurrentMap} or {@link ConcurrentNavigableMap},
 * this class provides additional methods that leverage the concurrent semantics of the backing map:
 * {@code putIfAbsent()}, {@code replace()}, {@code compute*()}, {@code merge()}, and navigation methods.
 * These operations maintain both the concurrent guarantees and the access tracking functionality.
 * </p>
 *
 * <p>
 * <b>Note:</b> The {@link #expungeUnused()} method removes all entries that have not been accessed via
 * {@link #get(Object)} or {@link #containsKey(Object)} since the map was created or since the last call to
 * {@code expungeUnused()}.
 * </p>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author
 *         Sean Kellner - original version
 *         John DeRegnaucourt - correct ConcurrentMap and ConcurrentNavigableMap support when wrapped.
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
public class TrackingMap<K, V> implements Map<K, V> {
    private final Map<K, V> internalMap;
    /**
     * Tracks all keys that were read via {@link #get(Object)} or
     * {@link #containsKey(Object)}. Stored as {@code Object} to avoid
     * {@link ClassCastException} if callers supply a key of a different type.
     */
    private final Set<Object> readKeys;

    /**
     * Wraps the provided {@code Map} with a {@code TrackingMap}.
     *
     * @param map the {@code Map} to be wrapped and tracked
     * @throws IllegalArgumentException if the provided {@code map} is {@code null}
     */
    public TrackingMap(Map<K, V> map) {
        if (map == null) {
            throw new IllegalArgumentException("Cannot construct a TrackingMap() with null");
        }
        internalMap = map;
        // Use concurrent tracking set if wrapping a concurrent map for thread safety
        readKeys = (map instanceof ConcurrentMap) ? ConcurrentHashMap.newKeySet() : new HashSet<>();
    }

    /**
     * Retrieves the value associated with the specified key and marks the key as accessed.
     * <p>When backing map is MultiKeyMap, Collections and Arrays are automatically expanded to multi-key operations.</p>
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key, or {@code null} if no mapping exists
     */
    public V get(Object key) {
        // Try array/collection handling first if MultiKeyMap
        V result = handleArrayCollectionKey(key, processedKey -> {
            @SuppressWarnings("unchecked")
            MultiKeyMap<V> multiKeyMap = (MultiKeyMap<V>) internalMap;
            return multiKeyMap.get(processedKey);
        });
        
        if (result != null || (internalMap instanceof MultiKeyMap && (key != null && key.getClass().isArray() || key instanceof Collection))) {
            trackKeyAccess(key);
            return result;
        }
        
        V value = internalMap.get(key);
        readKeys.add(key);
        return value;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * <p>When backing map is MultiKeyMap, Collections and Arrays are automatically expanded to multi-key operations.</p>
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping
     */
    public V put(K key, V value)
    {
        if (internalMap instanceof MultiKeyMap) {
            @SuppressWarnings("unchecked")
            MultiKeyMap<V> multiKeyMap = (MultiKeyMap<V>) internalMap;
            
            // Handle array/collection auto-expansion
            if (key != null && key.getClass().isArray()) {
                if (key instanceof Object[]) {
                    // Use varargs method for Object arrays
                    Object[] objArray = (Object[]) key;
                    return multiKeyMap.put(value, objArray);
                } else if (key instanceof String[]) {
                    // Use varargs method for String arrays
                    String[] strArray = (String[]) key;
                    Object[] objArray = new Object[strArray.length];
                    System.arraycopy(strArray, 0, objArray, 0, strArray.length);
                    return multiKeyMap.put(value, objArray);
                } else {
                    // For other typed arrays (int[], double[], etc.), use Map interface method to get array unpacking
                    return multiKeyMap.put(key, value);
                }
            } else if (key instanceof Collection) {
                // Always unpack collections into multi-key call
                Collection<?> collection = (Collection<?>) key;
                return putMultiKey(value, collection.toArray());
            }
        }
        return internalMap.put(key, value);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     * Marks the key as accessed.
     * <p>When backing map is MultiKeyMap, Collections and Arrays are automatically expanded to multi-key operations.</p>
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key
     */
    public boolean containsKey(Object key) {
        // Try array/collection handling first if MultiKeyMap
        Boolean result = handleArrayCollectionKey(key, processedKey -> {
            @SuppressWarnings("unchecked")
            MultiKeyMap<V> multiKeyMap = (MultiKeyMap<V>) internalMap;
            return multiKeyMap.containsKey(processedKey);
        });
        
        if (result != null) {
            trackKeyAccess(key);
            return result;
        }
        
        boolean containsKey = internalMap.containsKey(key);
        readKeys.add(key);
        return containsKey;
    }

    /**
     * Copies all the mappings from the specified map to this map.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if the specified map is {@code null}
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        internalMap.putAll(m);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     * Also removes the key from the set of accessed keys.
     * <p>When backing map is MultiKeyMap, Collections and Arrays are automatically expanded to multi-key operations.</p>
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping
     */
    public V remove(Object key) {
        // Try array/collection handling first if MultiKeyMap
        V result = handleArrayCollectionKey(key, processedKey -> {
            @SuppressWarnings("unchecked")
            MultiKeyMap<V> multiKeyMap = (MultiKeyMap<V>) internalMap;
            return multiKeyMap.remove(processedKey);
        });
        
        if (result != null || (internalMap instanceof MultiKeyMap && (key != null && key.getClass().isArray() || key instanceof Collection))) {
            trackKeyRemoval(key);
            return result;
        }
        
        readKeys.remove(key);
        return internalMap.remove(key);
    }

    /**
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return internalMap.size();
    }

    /**
     * @return {@code true} if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    /**
     * Compares the specified object with this map for equality.
     *
     * @param other object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    public boolean equals(Object other) {
        return other instanceof Map && internalMap.equals(other);
    }

    /**
     * @return the hash code value for this map
     */
    public int hashCode() {
        return internalMap.hashCode();
    }

    /**
     * @return a string representation of this map
     */
    public String toString() {
        return internalMap.toString();
    }

    /**
     * Removes all the mappings from this map. The map will be empty after this call returns.
     * Also clears the set of accessed keys.
     */
    public void clear() {
        readKeys.clear();
        internalMap.clear();
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     *
     * @param value value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the specified value
     */
    public boolean containsValue(Object value) {
        return internalMap.containsValue(value);
    }

    /**
     * @return a {@link Collection} view of the values contained in this map
     */
    public Collection<V> values() {
        return internalMap.values();
    }

    /**
     * @return a {@link Set} view of the keys contained in this map
     */
    public Set<K> keySet() {
        return internalMap.keySet();
    }

    /**
     * @return a {@link Set} view of the mappings contained in this map
     */
    public Set<Entry<K, V>> entrySet() {
        return internalMap.entrySet();
    }

    /**
     * Remove the entries from the Map that have not been accessed by .get() or .containsKey().
     */
    public void expungeUnused() {
        if (internalMap instanceof MultiKeyMap) {
            // Special handling for MultiKeyMap
            @SuppressWarnings("unchecked")
            MultiKeyMap<V> multiKeyMap = (MultiKeyMap<V>) internalMap;
            
            // Collect keys to remove first
            List<Object> keysToRemove = new ArrayList<>();
            for (Object mapKey : multiKeyMap.keySet()) {
                if (mapKey instanceof Object[]) {
                    Object[] keyArray = (Object[]) mapKey;
                    // Check if all components of this key were tracked
                    boolean shouldRemove = false;
                    for (Object component : keyArray) {
                        if (component != null && !readKeys.contains(component)) {
                            shouldRemove = true;
                            break;
                        }
                    }
                    if (shouldRemove) {
                        keysToRemove.add(mapKey);
                    }
                } else {
                    // Single key - check if it was tracked
                    if (!readKeys.contains(mapKey)) {
                        keysToRemove.add(mapKey);
                    }
                }
            }
            
            // Remove the collected keys
            for (Object keyToRemove : keysToRemove) {
                multiKeyMap.remove(keyToRemove);
            }
            
            // Clean up readKeys to only contain components that are still in use
            Set<Object> stillUsedComponents = new HashSet<>();
            for (Object mapKey : multiKeyMap.keySet()) {
                if (mapKey instanceof Object[]) {
                    Object[] keyArray = (Object[]) mapKey;
                    for (Object component : keyArray) {
                        if (component != null) {
                            stillUsedComponents.add(component);
                        }
                    }
                } else {
                    stillUsedComponents.add(mapKey);
                }
            }
            readKeys.retainAll(stillUsedComponents);
        } else {
            // Original logic for regular maps
            internalMap.keySet().retainAll(readKeys);
            // remove tracked keys that no longer exist in the map to avoid
            // unbounded growth when many misses occur
            readKeys.retainAll(internalMap.keySet());
        }
    }

    /**
     * Adds the accessed keys from another {@code TrackingMap} to this map's set of accessed keys.
     * This can be useful when merging usage information from multiple tracking maps.
     *
     * @param additional another {@code TrackingMap} whose accessed keys are to be added
     */
    public void informAdditionalUsage(Collection<K> additional) {
        readKeys.addAll(additional);
    }

    /**
     * Add the used keys from the passed in TrackingMap to this TrackingMap's keysUsed.  This can
     * cause the readKeys to include entries that are not in wrapped Maps keys.
     * @param additional TrackingMap whose used keys are to be added to this map's used keys.
     */
    public void informAdditionalUsage(TrackingMap<K, V> additional) {
        readKeys.addAll(additional.readKeys);
    }

    /**
     * Returns an unmodifiable view of the keys that have been accessed via
     * {@code get()} or {@code containsKey()}.
     * <p>
     * The returned set may contain objects that are not of type {@code K} if
     * callers queried the map using keys of a different type.
     *
     * @return unmodifiable set of accessed keys
     */
    public Set<Object> keysUsed() { return Collections.unmodifiableSet(readKeys); }

    /**
     * Returns the underlying {@link Map} that this {@code TrackingMap} wraps.
     *
     * @return the wrapped {@link Map}
     */
    public Map<K, V> getWrappedMap() { return internalMap; }

    /**
     * Replace all contents of the wrapped map with those from the provided map.
     * The underlying map instance remains the same.
     *
     * @param map map providing new contents; must not be {@code null}
     */
    public void replaceContents(Map<K, V> map) {
        Convention.throwIfNull(map, "Cannot replace contents with null");
        clear();
        putAll(map);
    }

    /**
     * @deprecated Use {@link #replaceContents(Map)} instead. This method
     * merely replaces the contents of the wrapped map and does not change the
     * underlying instance.
     */
    @Deprecated
    public void setWrappedMap(Map<K, V> map) {
        replaceContents(map);
    }

    // ===== MULTI-KEY APIs =====

    /**
     * Stores a value with multiple keys. This method is only supported when the backing map is a MultiKeyMap.
     * Note: putMultiKey does not mark keys as accessed - use getMultiKey or containsMultiKey to access.
     *
     * @param value the value to store
     * @param keys the key components (unlimited number)
     * @return the previous value associated with the key, or null if there was no mapping
     * @throws IllegalStateException if the backing map is not a MultiKeyMap instance
     */
    public V putMultiKey(V value, Object... keys) {
        if (!(internalMap instanceof MultiKeyMap)) {
            throw new IllegalStateException("Multi-key operations require the backing map to be a MultiKeyMap instance");
        }
        @SuppressWarnings("unchecked")
        MultiKeyMap<V> multiKeyMap = (MultiKeyMap<V>) internalMap;
        
        return multiKeyMap.put(value, keys);
    }

    /**
     * Retrieves the value associated with the specified multi-dimensional key.
     * This method is only supported when the backing map is a MultiKeyMap.
     * Marks all key components as accessed only if the key exists.
     *
     * @param keys the key components
     * @return the value associated with the key, or null if not found
     * @throws IllegalStateException if the backing map is not a MultiKeyMap instance
     */
    public V getMultiKey(Object... keys) {
        if (!(internalMap instanceof MultiKeyMap)) {
            throw new IllegalStateException("Multi-key operations require the backing map to be a MultiKeyMap instance");
        }
        @SuppressWarnings("unchecked")
        MultiKeyMap<V> multiKeyMap = (MultiKeyMap<V>) internalMap;
        
        V result = multiKeyMap.get(keys);
        
        // Only track if the key actually exists (result is not null)
        if (result != null) {
            // Track individual components (for user visibility and expungeUnused)
            for (Object key : keys) {
                if (key != null) {
                    readKeys.add(key);
                }
            }
        }
        
        return result;
    }

    /**
     * Removes the mapping for the specified multi-dimensional key.
     * This method is only supported when the backing map is a MultiKeyMap.
     * Removes key components from tracked keys only if no other multi-keys use them.
     *
     * @param keys the key components
     * @return the previous value associated with the key, or null if there was no mapping
     * @throws IllegalStateException if the backing map is not a MultiKeyMap instance
     */
    public V removeMultiKey(Object... keys) {
        if (!(internalMap instanceof MultiKeyMap)) {
            throw new IllegalStateException("Multi-key operations require the backing map to be a MultiKeyMap instance");
        }
        @SuppressWarnings("unchecked")
        MultiKeyMap<V> multiKeyMap = (MultiKeyMap<V>) internalMap;
        
        V result = multiKeyMap.remove(keys);
        
        // Only remove individual components if this was the only multi-key using them
        // We need to check if any remaining keys in the map use these components
        if (result != null) {
            for (Object component : keys) {
                if (component != null) {
                    boolean componentStillUsed = false;
                    // Check all remaining keys in the map to see if they use this component
                    for (Object mapKey : multiKeyMap.keySet()) {
                        if (mapKey instanceof Object[]) {
                            Object[] keyArray = (Object[]) mapKey;
                            for (Object keyComponent : keyArray) {
                                if (Objects.equals(component, keyComponent)) {
                                    componentStillUsed = true;
                                    break;
                                }
                            }
                        }
                        if (componentStillUsed) break;
                    }
                    // Only remove the component if it's not used by any remaining keys
                    if (!componentStillUsed) {
                        readKeys.remove(component);
                    }
                }
            }
            // Remove the composite key array that was tracking this multi-key
            // Find and remove matching array
            readKeys.removeIf(trackedKey -> {
                if (trackedKey instanceof Object[]) {
                    Object[] trackedArray = (Object[]) trackedKey;
                    return Arrays.equals(trackedArray, keys);
                }
                return false;
            });
        }
        
        return result;
    }

    /**
     * Returns true if this map contains a mapping for the specified multi-dimensional key.
     * This method is only supported when the backing map is a MultiKeyMap.
     * Marks all key components as accessed only if the key exists.
     *
     * @param keys the key components
     * @return true if a mapping exists for the key
     * @throws IllegalStateException if the backing map is not a MultiKeyMap instance
     */
    public boolean containsMultiKey(Object... keys) {
        if (!(internalMap instanceof MultiKeyMap)) {
            throw new IllegalStateException("Multi-key operations require the backing map to be a MultiKeyMap instance");
        }
        @SuppressWarnings("unchecked")
        MultiKeyMap<V> multiKeyMap = (MultiKeyMap<V>) internalMap;
        
        boolean exists = multiKeyMap.containsKey(keys);
        
        // Only track if the key actually exists
        if (exists) {
            // Track individual components (for user visibility and expungeUnused)
            for (Object key : keys) {
                if (key != null) {
                    readKeys.add(key);
                }
            }
        }
        
        return exists;
    }

    // ===== PRIVATE HELPER METHODS =====

    /**
     * Handles array and collection keys for MultiKeyMap operations.
     * 
     * @param key the key to process (can be array, collection, or single object)
     * @param operation a function that takes the processed key and returns the result
     * @return the result of the operation, or null if not a MultiKeyMap or not an array/collection
     */
    private <T> T handleArrayCollectionKey(Object key, Function<Object, T> operation) {
        if (!(internalMap instanceof MultiKeyMap)) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        MultiKeyMap<V> multiKeyMap = (MultiKeyMap<V>) internalMap;
        
        if (key != null && key.getClass().isArray()) {
            if (key instanceof Object[]) {
                // Pass Object array directly
                return operation.apply(key);
            } else if (key instanceof String[]) {
                // Convert String[] to Object[] to maintain compatibility
                String[] strArray = (String[]) key;
                Object[] objArray = new Object[strArray.length];
                System.arraycopy(strArray, 0, objArray, 0, strArray.length);
                return operation.apply(objArray);
            } else {
                // For other typed arrays (int[], double[], etc.), pass through directly
                // since they cannot contain complex keys that need processing
                return operation.apply(key);
            }
        } else if (key instanceof Collection) {
            Collection<?> collection = (Collection<?>) key;
            // Convert collection to array and recursively handle
            return handleArrayCollectionKey(collection.toArray(), operation);
        }
        
        return null; // Not an array or collection
    }

    /**
     * Tracks key access for arrays and collections. When backed by MultiKeyMap, 
     * tracks individual key components only if the composite key exists.
     */
    private void trackKeyAccess(Object key) {
        if (internalMap instanceof MultiKeyMap) {
            @SuppressWarnings("unchecked")
            MultiKeyMap<V> multiKeyMap = (MultiKeyMap<V>) internalMap;
            
            if (key != null && key.getClass().isArray()) {
                if (key instanceof Object[]) {
                    Object[] objArray = (Object[]) key;
                    // Only track components if the composite key exists
                    if (multiKeyMap.containsKey(objArray)) {
                        for (Object component : objArray) {
                            if (component != null) {
                                readKeys.add(component);
                            }
                        }
                    }
                } else if (key instanceof String[]) {
                    String[] strArray = (String[]) key;
                    Object[] objArray = new Object[strArray.length];
                    System.arraycopy(strArray, 0, objArray, 0, strArray.length);
                    // Only track components if the composite key exists
                    if (multiKeyMap.containsKey(objArray)) {
                        for (String component : strArray) {
                            if (component != null) {
                                readKeys.add(component);
                            }
                        }
                    }
                } else {
                    // For typed arrays, track the array itself only if it exists
                    if (multiKeyMap.containsKey(key)) {
                        readKeys.add(key);
                    }
                }
            } else if (key instanceof Collection) {
                Collection<?> collection = (Collection<?>) key;
                Object[] objArray = collection.toArray();
                // Only track components if the composite key exists
                if (multiKeyMap.containsKey(objArray)) {
                    for (Object component : collection) {
                        if (component != null) {
                            readKeys.add(component);
                        }
                    }
                }
            } else {
                // For single keys, track only if they exist
                if (multiKeyMap.containsKey(key)) {
                    readKeys.add(key);
                }
            }
        } else {
            // For regular maps, always track the key as-is (original behavior)
            readKeys.add(key);
        }
    }

    /**
     * Tracks key removal for arrays and collections, cleaning up component tracking
     * when they are no longer in use by any remaining keys.
     */
    private void trackKeyRemoval(Object key) {
        if (internalMap instanceof MultiKeyMap) {
            if (key != null && key.getClass().isArray()) {
                if (key instanceof Object[]) {
                    Object[] objArray = (Object[]) key;
                    // Immediately untrack all components - consistent with regular TrackingMap behavior
                    for (Object component : objArray) {
                        if (component != null) {
                            readKeys.remove(component);
                        }
                    }
                } else if (key instanceof String[]) {
                    String[] strArray = (String[]) key;
                    // Immediately untrack all components - consistent with regular TrackingMap behavior
                    for (String component : strArray) {
                        if (component != null) {
                            readKeys.remove(component);
                        }
                    }
                } else {
                    // For typed arrays, remove the array itself
                    readKeys.remove(key);
                }
            } else if (key instanceof Collection) {
                Collection<?> collection = (Collection<?>) key;
                // Immediately untrack all components - consistent with regular TrackingMap behavior
                for (Object component : collection) {
                    if (component != null) {
                        readKeys.remove(component);
                    }
                }
            } else {
                readKeys.remove(key);
            }
        } else {
            // For regular maps, remove the key as-is
            readKeys.remove(key);
        }
    }

    // ===== ConcurrentMap methods (available when backing map supports them) =====
    
    /**
     * If the specified key is not already associated with a value,
     * associate it with the given value.
     * <p>
     * Does not mark the key as accessed since this is a write operation.
     * Available when the backing map is a {@link ConcurrentMap}.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null}
     *         if there was no mapping for the key
     * @throws UnsupportedOperationException if the wrapped map doesn't support ConcurrentMap operations
     */
    public V putIfAbsent(K key, V value) {
        if (internalMap instanceof ConcurrentMap) {
            return internalMap.putIfAbsent(key, value);
        }
        // Fallback for non-concurrent maps with synchronization
        synchronized (this) {
            V existing = internalMap.get(key);
            if (existing == null) {
                return internalMap.put(key, value);
            }
            return existing;
        }
    }

    /**
     * Removes the entry for a key only if currently mapped to a given value.
     * Also removes the key from the set of accessed keys if removal succeeds.
     * Available when the backing map is a {@link ConcurrentMap}.
     *
     * @param key key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     * @return {@code true} if the value was removed
     * @throws UnsupportedOperationException if the wrapped map doesn't support ConcurrentMap operations
     */
    public boolean remove(Object key, Object value) {
        boolean removed;
        if (internalMap instanceof ConcurrentMap) {
            removed = internalMap.remove(key, value);
        } else {
            // Fallback for non-concurrent maps with synchronization
            synchronized (this) {
                Object curValue = internalMap.get(key);
                if (Objects.equals(curValue, value)) {
                    internalMap.remove(key);
                    removed = true;
                } else {
                    removed = false;
                }
            }
        }
        if (removed) {
            readKeys.remove(key);
        }
        return removed;
    }

    /**
     * Replaces the entry for a key only if currently mapped to a given value.
     * Available when the backing map is a {@link ConcurrentMap}.
     *
     * @param key key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     * @return {@code true} if the value was replaced
     * @throws UnsupportedOperationException if the wrapped map doesn't support ConcurrentMap operations
     */
    public boolean replace(K key, V oldValue, V newValue) {
        if (internalMap instanceof ConcurrentMap) {
            return internalMap.replace(key, oldValue, newValue);
        }
        // Fallback for non-concurrent maps with synchronization
        synchronized (this) {
            Object curValue = internalMap.get(key);
            if (Objects.equals(curValue, oldValue)) {
                internalMap.put(key, newValue);
                return true;
            }
            return false;
        }
    }

    /**
     * Replaces the entry for a key only if currently mapped to some value.
     * Available when the backing map is a {@link ConcurrentMap}.
     *
     * @param key key with which the specified value is associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with the specified key, or {@code null}
     *         if there was no mapping for the key
     * @throws UnsupportedOperationException if the wrapped map doesn't support ConcurrentMap operations
     */
    public V replace(K key, V value) {
        if (internalMap instanceof ConcurrentMap) {
            return internalMap.replace(key, value);
        }
        // Fallback for non-concurrent maps with synchronization
        synchronized (this) {
            if (internalMap.containsKey(key)) {
                return internalMap.put(key, value);
            }
            return null;
        }
    }

    // ===== Java 8+ Map methods with tracking =====
    
    /**
     * If the specified key is not already associated with a value,
     * attempts to compute its value using the given mapping function
     * and enters it into this map unless null.
     * Marks the key as accessed since this involves reading the current value.
     *
     * @param key key with which the specified value is to be associated
     * @param mappingFunction the function to compute a value
     * @return the current (existing or computed) value associated with
     *         the specified key, or null if the computed value is null
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V result = internalMap.computeIfAbsent(key, mappingFunction);
        readKeys.add(key);
        return result;
    }

    /**
     * If the value for the specified key is present and non-null, attempts to
     * compute a new mapping given the key and its current mapped value.
     * Marks the key as accessed since this involves reading the current value.
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     */
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V result = internalMap.computeIfPresent(key, remappingFunction);
        readKeys.add(key);
        return result;
    }

    /**
     * Attempts to compute a mapping for the specified key and its current
     * mapped value (or null if there is no current mapping).
     * Marks the key as accessed since this involves reading the current value.
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     */
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V result = internalMap.compute(key, remappingFunction);
        readKeys.add(key);
        return result;
    }

    /**
     * If the specified key is not already associated with a value or is
     * associated with null, associates it with the given non-null value.
     * Otherwise, replaces the associated value with the results of the given
     * remapping function, or removes if the result is null.
     * Marks the key as accessed since this involves reading the current value.
     *
     * @param key key with which the resulting value is to be associated
     * @param value the non-null value to be merged with the existing value
     * @param remappingFunction the function to recompute a value if present
     * @return the new value associated with the specified key, or null if no
     *         value is associated with the key
     */
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        V result = internalMap.merge(key, value, remappingFunction);
        readKeys.add(key);
        return result;
    }

    /**
     * Returns the value to which the specified key is mapped, or
     * {@code defaultValue} if this map contains no mapping for the key.
     * Marks the key as accessed.
     *
     * @param key the key whose associated value is to be returned
     * @param defaultValue the default mapping of the key
     * @return the value to which the specified key is mapped, or
     *         {@code defaultValue} if this map contains no mapping for the key
     */
    public V getOrDefault(Object key, V defaultValue) {
        V result = internalMap.getOrDefault(key, defaultValue);
        readKeys.add(key);
        return result;
    }

    /**
     * Performs the given action for each entry in this map until all entries
     * have been processed or the action throws an exception.
     *
     * @param action The action to be performed for each entry
     */
    public void forEach(BiConsumer<? super K, ? super V> action) {
        internalMap.forEach(action);
    }

    /**
     * Replaces each entry's value with the result of invoking the given
     * function on that entry until all entries have been processed or the
     * function throws an exception.
     *
     * @param function the function to apply to each entry
     */
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        internalMap.replaceAll(function);
    }

    // ===== NavigableMap methods (available when backing map supports them) =====
    
    /**
     * Returns a key-value mapping associated with the greatest key strictly less
     * than the given key, or null if there is no such key.
     * Marks the returned key as accessed if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param key the key
     * @return an entry with the greatest key less than key, or null if no such key
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public Map.Entry<K, V> lowerEntry(K key) {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = ((NavigableMap<K, V>) internalMap).lowerEntry(key);
        if (entry != null) {
            readKeys.add(entry.getKey());
        }
        return entry;
    }

    /**
     * Returns the greatest key strictly less than the given key, or null if no such key.
     * Marks the returned key as accessed if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param key the key
     * @return the greatest key less than key, or null if no such key
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public K lowerKey(K key) {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        K result = ((NavigableMap<K, V>) internalMap).lowerKey(key);
        if (result != null) {
            readKeys.add(result);
        }
        return result;
    }

    /**
     * Returns a key-value mapping associated with the greatest key less than or
     * equal to the given key, or null if there is no such key.
     * Marks the returned key as accessed if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param key the key
     * @return an entry with the greatest key less than or equal to key, or null if no such key
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public Map.Entry<K, V> floorEntry(K key) {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = ((NavigableMap<K, V>) internalMap).floorEntry(key);
        if (entry != null) {
            readKeys.add(entry.getKey());
        }
        return entry;
    }

    /**
     * Returns the greatest key less than or equal to the given key, or null if no such key.
     * Marks the returned key as accessed if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param key the key
     * @return the greatest key less than or equal to key, or null if no such key
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public K floorKey(K key) {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        K result = ((NavigableMap<K, V>) internalMap).floorKey(key);
        if (result != null) {
            readKeys.add(result);
        }
        return result;
    }

    /**
     * Returns a key-value mapping associated with the least key greater than or
     * equal to the given key, or null if there is no such key.
     * Marks the returned key as accessed if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param key the key
     * @return an entry with the least key greater than or equal to key, or null if no such key
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public Map.Entry<K, V> ceilingEntry(K key) {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = ((NavigableMap<K, V>) internalMap).ceilingEntry(key);
        if (entry != null) {
            readKeys.add(entry.getKey());
        }
        return entry;
    }

    /**
     * Returns the least key greater than or equal to the given key, or null if no such key.
     * Marks the returned key as accessed if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param key the key
     * @return the least key greater than or equal to key, or null if no such key
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public K ceilingKey(K key) {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        K result = ((NavigableMap<K, V>) internalMap).ceilingKey(key);
        if (result != null) {
            readKeys.add(result);
        }
        return result;
    }

    /**
     * Returns a key-value mapping associated with the least key strictly greater
     * than the given key, or null if there is no such key.
     * Marks the returned key as accessed if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param key the key
     * @return an entry with the least key greater than key, or null if no such key
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public Map.Entry<K, V> higherEntry(K key) {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = ((NavigableMap<K, V>) internalMap).higherEntry(key);
        if (entry != null) {
            readKeys.add(entry.getKey());
        }
        return entry;
    }

    /**
     * Returns the least key strictly greater than the given key, or null if no such key.
     * Marks the returned key as accessed if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param key the key
     * @return the least key greater than key, or null if no such key
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public K higherKey(K key) {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        K result = ((NavigableMap<K, V>) internalMap).higherKey(key);
        if (result != null) {
            readKeys.add(result);
        }
        return result;
    }

    /**
     * Returns a key-value mapping associated with the least key in this map,
     * or null if the map is empty.
     * Marks the returned key as accessed if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @return an entry with the least key, or null if this map is empty
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public Map.Entry<K, V> firstEntry() {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = ((NavigableMap<K, V>) internalMap).firstEntry();
        if (entry != null) {
            readKeys.add(entry.getKey());
        }
        return entry;
    }

    /**
     * Returns a key-value mapping associated with the greatest key in this map,
     * or null if the map is empty.
     * Marks the returned key as accessed if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @return an entry with the greatest key, or null if this map is empty
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public Map.Entry<K, V> lastEntry() {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = ((NavigableMap<K, V>) internalMap).lastEntry();
        if (entry != null) {
            readKeys.add(entry.getKey());
        }
        return entry;
    }

    /**
     * Removes and returns a key-value mapping associated with the least key
     * in this map, or null if the map is empty.
     * Removes the key from tracked keys if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @return the removed first entry of this map, or null if this map is empty
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public Map.Entry<K, V> pollFirstEntry() {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = ((NavigableMap<K, V>) internalMap).pollFirstEntry();
        if (entry != null) {
            readKeys.remove(entry.getKey());
        }
        return entry;
    }

    /**
     * Removes and returns a key-value mapping associated with the greatest key
     * in this map, or null if the map is empty.
     * Removes the key from tracked keys if present.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @return the removed last entry of this map, or null if this map is empty
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public Map.Entry<K, V> pollLastEntry() {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = ((NavigableMap<K, V>) internalMap).pollLastEntry();
        if (entry != null) {
            readKeys.remove(entry.getKey());
        }
        return entry;
    }

    /**
     * Returns a NavigableSet view of the keys contained in this map.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @return a navigable set view of the keys in this map
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public NavigableSet<K> navigableKeySet() {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        return ((NavigableMap<K, V>) internalMap).navigableKeySet();
    }

    /**
     * Returns a reverse order NavigableSet view of the keys contained in this map.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @return a reverse order navigable set view of the keys in this map
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public NavigableSet<K> descendingKeySet() {
        if (!(internalMap instanceof NavigableMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        return ((NavigableMap<K, V>) internalMap).descendingKeySet();
    }

    /**
     * Returns a view of the portion of this map whose keys range from fromKey to toKey.
     * Available when the backing map is a {@link ConcurrentNavigableMap}.
     *
     * @param fromKey low endpoint of the keys in the returned map
     * @param fromInclusive true if the low endpoint is to be included in the returned view
     * @param toKey high endpoint of the keys in the returned map
     * @param toInclusive true if the high endpoint is to be included in the returned view
     * @return a view of the portion of this map whose keys range from fromKey to toKey
     * @throws UnsupportedOperationException if the wrapped map doesn't support ConcurrentNavigableMap operations
     */
    public TrackingMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        if (internalMap instanceof ConcurrentNavigableMap) {
            NavigableMap<K, V> subMap = ((ConcurrentNavigableMap<K, V>) internalMap).subMap(fromKey, fromInclusive, toKey, toInclusive);
            return new TrackingMap<>(subMap);
        } else if (internalMap instanceof NavigableMap) {
            NavigableMap<K, V> subMap = ((NavigableMap<K, V>) internalMap).subMap(fromKey, fromInclusive, toKey, toInclusive);
            return new TrackingMap<>(subMap);
        } else {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
    }

    /**
     * Returns a view of the portion of this map whose keys are less than (or
     * equal to, if inclusive is true) toKey.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param toKey high endpoint of the keys in the returned map
     * @param inclusive true if the high endpoint is to be included in the returned view
     * @return a view of the portion of this map whose keys are less than toKey
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public TrackingMap<K, V> headMap(K toKey, boolean inclusive) {
        if (internalMap instanceof ConcurrentNavigableMap) {
            NavigableMap<K, V> headMap = ((ConcurrentNavigableMap<K, V>) internalMap).headMap(toKey, inclusive);
            return new TrackingMap<>(headMap);
        } else if (internalMap instanceof NavigableMap) {
            NavigableMap<K, V> headMap = ((NavigableMap<K, V>) internalMap).headMap(toKey, inclusive);
            return new TrackingMap<>(headMap);
        } else {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
    }

    /**
     * Returns a view of the portion of this map whose keys are greater than (or
     * equal to, if inclusive is true) fromKey.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param fromKey low endpoint of the keys in the returned map
     * @param inclusive true if the low endpoint is to be included in the returned view
     * @return a view of the portion of this map whose keys are greater than fromKey
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public TrackingMap<K, V> tailMap(K fromKey, boolean inclusive) {
        if (internalMap instanceof ConcurrentNavigableMap) {
            NavigableMap<K, V> tailMap = ((ConcurrentNavigableMap<K, V>) internalMap).tailMap(fromKey, inclusive);
            return new TrackingMap<>(tailMap);
        } else if (internalMap instanceof NavigableMap) {
            NavigableMap<K, V> tailMap = ((NavigableMap<K, V>) internalMap).tailMap(fromKey, inclusive);
            return new TrackingMap<>(tailMap);
        } else {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
    }

    // ===== SortedMap methods =====
    
    /**
     * Returns the comparator used to order the keys in this map, or null
     * if this map uses the natural ordering of its keys.
     * Available when the backing map is a {@link SortedMap}.
     *
     * @return the comparator used to order the keys in this map, or null
     * @throws UnsupportedOperationException if the wrapped map doesn't support SortedMap operations
     */
    public java.util.Comparator<? super K> comparator() {
        if (!(internalMap instanceof SortedMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support SortedMap operations");
        }
        return ((SortedMap<K, V>) internalMap).comparator();
    }

    /**
     * Returns a view of the portion of this map whose keys range from fromKey,
     * inclusive, to toKey, exclusive.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param fromKey low endpoint (inclusive) of the keys in the returned map
     * @param toKey high endpoint (exclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys range from fromKey to toKey
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public TrackingMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    /**
     * Returns a view of the portion of this map whose keys are strictly less than toKey.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param toKey high endpoint (exclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys are less than toKey
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public TrackingMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    /**
     * Returns a view of the portion of this map whose keys are greater than or equal to fromKey.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @param fromKey low endpoint (inclusive) of the keys in the returned map
     * @return a view of the portion of this map whose keys are greater than or equal to fromKey
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public TrackingMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    /**
     * Returns the first (lowest) key currently in this map.
     * Marks the returned key as accessed.
     * Available when the backing map is a {@link SortedMap}.
     *
     * @return the first (lowest) key currently in this map
     * @throws UnsupportedOperationException if the wrapped map doesn't support SortedMap operations
     */
    public K firstKey() {
        if (!(internalMap instanceof SortedMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support SortedMap operations");
        }
        K result = ((SortedMap<K, V>) internalMap).firstKey();
        if (result != null) {
            readKeys.add(result);
        }
        return result;
    }

    /**
     * Returns the last (highest) key currently in this map.
     * Marks the returned key as accessed.
     * Available when the backing map is a {@link SortedMap}.
     *
     * @return the last (highest) key currently in this map
     * @throws UnsupportedOperationException if the wrapped map doesn't support SortedMap operations
     */
    public K lastKey() {
        if (!(internalMap instanceof SortedMap)) {
            throw new UnsupportedOperationException("Wrapped map does not support SortedMap operations");
        }
        K result = ((SortedMap<K, V>) internalMap).lastKey();
        if (result != null) {
            readKeys.add(result);
        }
        return result;
    }
}
