package com.cedarsoftware.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
 * System.out.println(trackingMap.keySet()); // Outputs: [apple, banana]
 * }</pre>
 *
 * <p>
 * <b>Thread Safety:</b> This class is <i>not</i> thread-safe. If multiple threads access a {@code TrackingMap}
 * concurrently and at least one of the threads modifies the map structurally, it must be synchronized externally.
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
 *         Sean Kellner
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
    private final Set<K> readKeys;

    /**
     * Wraps the provided {@code Map} with a {@code TrackingMap}.
     *
     * @param map the {@code Map} to be wrapped and tracked
     * @throws IllegalArgumentException if the provided {@code map} is {@code null}
     */
    public TrackingMap(Map<K, V> map) {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot construct a TrackingMap() with null");
        }
        internalMap = map;
        readKeys = new HashSet<>();
    }

    /**
     * Retrieves the value associated with the specified key and marks the key as accessed.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key, or {@code null} if no mapping exists
     */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        V value = internalMap.get(key);
        readKeys.add((K) key);
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
    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        boolean containsKey = internalMap.containsKey(key);
        readKeys.add((K)key);
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
     * Returns a {@link Set} of keys that have been accessed via {@code get} or {@code containsKey}.
     *
     * @return a {@link Set} of accessed keys
     */
    public Set<K> keysUsed() { return readKeys; }

    /**
     * Returns the underlying {@link Map} that this {@code TrackingMap} wraps.
     *
     * @return the wrapped {@link Map}
     */
    public Map<K, V> getWrappedMap() { return internalMap; }

    public void setWrappedMap(Map<K, V> map) {
        Convention.throwIfNull(map, "Cannot set a TrackingMap() with null");
        clear();
        putAll(map);
    }
}
