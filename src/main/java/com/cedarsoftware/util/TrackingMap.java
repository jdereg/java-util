package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

    // Cached interface references to avoid repeated instanceof checks and casts
    private final ConcurrentMap<K, V> asConcurrent;
    private final NavigableMap<K, V> asNavigable;
    private final SortedMap<K, V> asSorted;

    /**
     * Wraps the provided {@code Map} with a {@code TrackingMap}.
     *
     * @param map the {@code Map} to be wrapped and tracked
     * @throws IllegalArgumentException if the provided {@code map} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public TrackingMap(Map<K, V> map) {
        if (map == null) {
            throw new IllegalArgumentException("Cannot construct a TrackingMap() with null");
        }
        internalMap = map;

        // Cache interface references for performance
        asConcurrent = (map instanceof ConcurrentMap) ? (ConcurrentMap<K, V>) map : null;
        asNavigable = (map instanceof NavigableMap) ? (NavigableMap<K, V>) map : null;
        asSorted = (map instanceof SortedMap) ? (SortedMap<K, V>) map : null;

        // Use concurrent tracking set if wrapping a concurrent map for thread safety
        // Pre-size HashSet based on map size to reduce rehashing
        readKeys = (asConcurrent != null)
            ? ConcurrentHashMap.newKeySet()
            : new HashSet<>(Math.max(16, map.size()));
    }

    /**
     * Private constructor for creating sub-maps that share the parent's readKeys.
     */
    @SuppressWarnings("unchecked")
    private TrackingMap(Map<K, V> map, Set<Object> sharedReadKeys) {
        internalMap = map;
        readKeys = sharedReadKeys;
        asConcurrent = (map instanceof ConcurrentMap) ? (ConcurrentMap<K, V>) map : null;
        asNavigable = (map instanceof NavigableMap) ? (NavigableMap<K, V>) map : null;
        asSorted = (map instanceof SortedMap) ? (SortedMap<K, V>) map : null;
    }

    /**
     * Retrieves the value associated with the specified key and marks the key as accessed.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key, or {@code null} if no mapping exists
     */
    public V get(Object key) {
        V value = internalMap.get(key);
        readKeys.add(key);
        return value;
    }

    /**
     * Associates the specified value with the specified key in this map.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping
     */
    public V put(K key, V value)
    {
        return internalMap.put(key, value);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     * Marks the key as accessed.
     *
     * @param key key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key
     */
    public boolean containsKey(Object key) {
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
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping
     */
    public V remove(Object key) {
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
        internalMap.keySet().retainAll(readKeys);
        // remove tracked keys that no longer exist in the map to avoid
        // unbounded growth when many misses occur
        readKeys.retainAll(internalMap.keySet());
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
        if (asConcurrent != null) {
            return asConcurrent.putIfAbsent(key, value);
        }
        // Fallback for non-concurrent maps with synchronization
        synchronized (this) {
            if (!internalMap.containsKey(key)) {
                internalMap.put(key, value);
                return null;
            }
            return internalMap.get(key);
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
        if (asConcurrent != null) {
            removed = asConcurrent.remove(key, value);
        } else {
            // Fallback for non-concurrent maps with synchronization
            synchronized (this) {
                Object curValue = internalMap.get(key);
                if (!Objects.equals(curValue, value) || (curValue == null && !internalMap.containsKey(key))) {
                    removed = false;
                } else {
                    internalMap.remove(key);
                    removed = true;
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
        if (asConcurrent != null) {
            return asConcurrent.replace(key, oldValue, newValue);
        }
        // Fallback for non-concurrent maps with synchronization
        synchronized (this) {
            Object curValue = internalMap.get(key);
            if (!Objects.equals(curValue, oldValue) || (curValue == null && !internalMap.containsKey(key))) {
                return false;
            }
            internalMap.put(key, newValue);
            return true;
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
        if (asConcurrent != null) {
            return asConcurrent.replace(key, value);
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
     * Only marks the key as accessed if it was actually present in the map.
     *
     * @param key key with which the specified value is to be associated
     * @param remappingFunction the function to compute a value
     * @return the new value associated with the specified key, or null if none
     */
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        boolean wasPresent = internalMap.containsKey(key);
        V result = internalMap.computeIfPresent(key, remappingFunction);
        if (wasPresent) {
            readKeys.add(key);
        }
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = asNavigable.lowerEntry(key);
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        K result = asNavigable.lowerKey(key);
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = asNavigable.floorEntry(key);
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        K result = asNavigable.floorKey(key);
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = asNavigable.ceilingEntry(key);
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        K result = asNavigable.ceilingKey(key);
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = asNavigable.higherEntry(key);
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        K result = asNavigable.higherKey(key);
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = asNavigable.firstEntry();
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = asNavigable.lastEntry();
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = asNavigable.pollFirstEntry();
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        Map.Entry<K, V> entry = asNavigable.pollLastEntry();
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        return asNavigable.navigableKeySet();
    }

    /**
     * Returns a reverse order NavigableSet view of the keys contained in this map.
     * Available when the backing map is a {@link NavigableMap}.
     *
     * @return a reverse order navigable set view of the keys in this map
     * @throws UnsupportedOperationException if the wrapped map doesn't support NavigableMap operations
     */
    public NavigableSet<K> descendingKeySet() {
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        return asNavigable.descendingKeySet();
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        NavigableMap<K, V> subMap = asNavigable.subMap(fromKey, fromInclusive, toKey, toInclusive);
        return new TrackingMap<>(subMap, readKeys);
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        NavigableMap<K, V> headMap = asNavigable.headMap(toKey, inclusive);
        return new TrackingMap<>(headMap, readKeys);
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
        if (asNavigable == null) {
            throw new UnsupportedOperationException("Wrapped map does not support NavigableMap operations");
        }
        NavigableMap<K, V> tailMap = asNavigable.tailMap(fromKey, inclusive);
        return new TrackingMap<>(tailMap, readKeys);
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
        if (asSorted == null) {
            throw new UnsupportedOperationException("Wrapped map does not support SortedMap operations");
        }
        return asSorted.comparator();
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
        if (asSorted == null) {
            throw new UnsupportedOperationException("Wrapped map does not support SortedMap operations");
        }
        K result = asSorted.firstKey();
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
        if (asSorted == null) {
            throw new UnsupportedOperationException("Wrapped map does not support SortedMap operations");
        }
        K result = asSorted.lastKey();
        if (result != null) {
            readKeys.add(result);
        }
        return result;
    }
}
