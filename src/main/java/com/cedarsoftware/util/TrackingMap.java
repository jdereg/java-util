package com.cedarsoftware.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TrackingMap
 *
 * @author Sean Kellner
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
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
     * Wrap the passed in Map with a TrackingMap.
     * @param map Map to wrap
     */
    public TrackingMap(Map<K, V> map) {
        if (map == null)
        {
            throw new IllegalArgumentException("Cannot construct a TrackingMap() with null");
        }
        internalMap = map;
        readKeys = new HashSet<>();
    }

    public V get(Object key) {
        V value = internalMap.get(key);
        readKeys.add((K) key);
        return value;
    }

    public V put(K key, V value)
    {
        return internalMap.put(key, value);
    }

    public boolean containsKey(Object key) {
        boolean containsKey = internalMap.containsKey(key);
        readKeys.add((K)key);
        return containsKey;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        internalMap.putAll(m);
    }

    public V remove(Object key) {
        readKeys.remove(key);
        return internalMap.remove(key);
    }

    public int size() {
        return internalMap.size();
    }

    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    public boolean equals(Object other) {
        return other instanceof Map && internalMap.equals(other);
    }

    public int hashCode() {
        return internalMap.hashCode();
    }

    public String toString() {
        return internalMap.toString();
    }

    public void clear() {
        readKeys.clear();
        internalMap.clear();
    }

    public boolean containsValue(Object value) {
        return internalMap.containsValue(value);
    }

    public Collection<V> values() {
        return internalMap.values();
    }

    public Set<K> keySet() {
        return internalMap.keySet();
    }

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
     * Add the Collection of keys to the internal list of keys accessed.  If there are keys
     * in the passed in Map that are not included in the contained Map, the readKeys will
     * exceed the keySet() of the wrapped Map.
     * @param additional Collection of keys to add to the list of keys read.
     */
    public void informAdditionalUsage(Collection<K> additional) {
        readKeys.addAll(additional);
    }

    /**
     * Add the used keys from the passed in TrackingMap to this TrackingMap's keysUsed.  This can
     * cause the readKeys to include entries that are not in wrapped Maps keys.
     * @param additional TrackingMap whose used keys are to be added to this maps used keys.
     */
    public void informAdditionalUsage(TrackingMap<K, V> additional) {
        readKeys.addAll(additional.readKeys);
    }

    /**
     * Fetch the Set of keys that have been accessed via .get() or .containsKey() of the contained Map.
     * @return Set of the accessed (read) keys.
     */
    public Set<K> keysUsed() { return readKeys; }

    /**
     * Fetch the Map that this TrackingMap wraps.
     * @return Map the wrapped Map
     */
    public Map getWrappedMap() { return internalMap; }
}
