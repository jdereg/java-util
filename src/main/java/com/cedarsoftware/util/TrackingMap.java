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
    private final KeyTracker<K> keyTracker;

    public TrackingMap(Map<K, V> map) {
        if (map == null) {
            throw new IllegalArgumentException("Cannot construct a TrackingMap() with null");
        }
        internalMap = map;
        keyTracker = new KeyTracker<>();
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        V value = internalMap.get(key);
        keyTracker.add((K) key);
        return value;
    }

    public V put(K key, V value) {
        return internalMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        boolean containsKey = internalMap.containsKey(key);
        keyTracker.add((K) key);
        return containsKey;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        internalMap.putAll(m);
    }

    public V remove(Object key) {
        keyTracker.remove(key);
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
        keyTracker.clear();
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

    public void expungeUnused() {
        internalMap.keySet().retainAll(keyTracker.getKeys());
    }

    public void informAdditionalUsage(Collection<K> additional) {
        keyTracker.addAll(additional);
    }

    public void informAdditionalUsage(TrackingMap<K, V> additional) {
        keyTracker.addAll(additional.getKeyTracker().getKeys());
    }

    public Set<K> keysUsed() {
        return keyTracker.getKeys();
    }

    public Map<K, V> getWrappedMap() {
        return internalMap;
    }

    public KeyTracker<K> getKeyTracker() {
        return keyTracker;
    }

}
class KeyTracker<K> {
    private final Set<K> keys;

    KeyTracker() {
        keys = new HashSet<>();
    }

    void add(K key) {
        keys.add(key);
    }

    void addAll(Collection<K> keys) {
        this.keys.addAll(keys);
    }

    void remove(Object key) {
        keys.remove(key);
    }

    Set<K> getKeys() {
        return keys;
    }

    void clear() {
        keys.clear();
    }
}
