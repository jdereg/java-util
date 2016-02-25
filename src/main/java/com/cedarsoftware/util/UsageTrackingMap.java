package com.cedarsoftware.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class UsageTrackingMap<K, V> implements Map<K, V> {
    private final Map<K, V> internalMap;
    private Set<Object> readKeys;

    public UsageTrackingMap(Map<K, V> map) {
        internalMap = map; //TODO What to do when input map is null?
        readKeys = new HashSet<>();
    }

    public V get(Object key) {
        V value = internalMap.get(key);
        if (value != null) {
            readKeys.add(key);
        }
        return value;
    }

    public V put(K key, V value) {
        return internalMap.put(key, value);
    }

    public boolean containsKey(Object key) {
        boolean containsKey = internalMap.containsKey(key);
        if (containsKey) {
            readKeys.add(key);
        }
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
        return other instanceof UsageTrackingMap && internalMap.equals(((UsageTrackingMap) other).internalMap);
    }

    @Override
    public int hashCode() {
        int result = internalMap != null ? internalMap.hashCode() : 0;
        result = 31 * result + (readKeys != null ? readKeys.hashCode() : 0);
        return result;
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

    public void expungeUnused() {
        internalMap.keySet().retainAll(readKeys);
    }

    public void informAdditionalUsage(Set<?> additional) {
        readKeys.addAll(additional);
    }

    public void informAdditionalUsage(UsageTrackingMap<K, V> additional) {
        readKeys.addAll(additional.readKeys);
    }
}
