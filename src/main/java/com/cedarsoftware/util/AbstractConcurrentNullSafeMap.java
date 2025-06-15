package com.cedarsoftware.util;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;

/**
 * An abstract thread-safe implementation of the {@link ConcurrentMap} interface that allows {@code null} keys
 * and {@code null} values. Internally, {@code AbstractConcurrentNullSafeMap} uses sentinel objects to
 * represent {@code null} keys and values, enabling safe handling of {@code null} while maintaining
 * compatibility with {@link ConcurrentMap} behavior.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Thread-Safe:</b> Implements {@link ConcurrentMap} with thread-safe operations.</li>
 *   <li><b>Null Handling:</b> Supports {@code null} keys and {@code null} values using sentinel objects
 *       ({@link NullSentinel}).</li>
 *   <li><b>Customizable:</b> Allows customization of the underlying {@link ConcurrentMap} through its
 *       constructor.</li>
 *   <li><b>Standard Map Behavior:</b> Adheres to the {@link Map} and {@link ConcurrentMap} contract,
 *       supporting operations like {@link #putIfAbsent}, {@link #computeIfAbsent}, {@link #merge}, and more.</li>
 * </ul>
 *
 * <h2>Null Key and Value Handling</h2>
 * <p>
 * The {@code AbstractConcurrentNullSafeMap} uses internal sentinel objects ({@link NullSentinel}) to distinguish
 * {@code null} keys and values from actual entries. This ensures that {@code null} keys and values can coexist
 * with regular entries without ambiguity.
 * </p>
 *
 * <h2>Customization</h2>
 * <p>
 * This abstract class requires a concrete implementation of the backing {@link ConcurrentMap}.
 * To customize the behavior, subclasses can provide a specific implementation of the internal map.
 * </p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Example subclass using ConcurrentHashMap as the backing map
 * public class MyConcurrentNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {
 *     public MyConcurrentNullSafeMap() {
 *         super(new ConcurrentHashMap<>());
 *     }
 * }
 *
 * // Using the map
 * MyConcurrentNullSafeMap<String, String> map = new MyConcurrentNullSafeMap<>();
 * map.put(null, "nullKey");
 * map.put("key", null);
 * System.out.println(map.get(null));  // Outputs: nullKey
 * System.out.println(map.get("key")); // Outputs: null
 * }</pre>
 *
 * <h2>Additional Notes</h2>
 * <ul>
 *   <li><b>Equality and HashCode:</b> Ensures consistent behavior for equality and hash code computation
 *       in compliance with the {@link Map} contract.</li>
 *   <li><b>Thread Safety:</b> The thread safety of this class is determined by the thread safety of the
 *       underlying {@link ConcurrentMap} implementation.</li>
 *   <li><b>Sentinel Objects:</b> The {@link NullSentinel#NULL_KEY} and {@link NullSentinel#NULL_VALUE} are used
 *       internally to mask {@code null} keys and values.</li>
 * </ul>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
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
 * @see ConcurrentMap
 * @see java.util.concurrent.ConcurrentHashMap
 */
public abstract class AbstractConcurrentNullSafeMap<K, V> implements ConcurrentMap<K, V> {
    // Sentinel objects to represent null keys and values
    protected enum NullSentinel {
        NULL_KEY, NULL_VALUE
    }

    // Internal ConcurrentMap storing Objects
    protected final ConcurrentMap<Object, Object> internalMap;

    /**
     * Constructs a new AbstractConcurrentNullSafeMap with the provided internal map.
     *
     * @param internalMap the internal ConcurrentMap to use
     */
    protected AbstractConcurrentNullSafeMap(ConcurrentMap<Object, Object> internalMap) {
        this.internalMap = internalMap;
    }

    // Helper methods to handle nulls
    protected Object maskNullKey(K key) {
        return key == null ? NullSentinel.NULL_KEY : key;
    }

    @SuppressWarnings("unchecked")
    protected K unmaskNullKey(Object key) {
        return key == NullSentinel.NULL_KEY ? null : (K) key;
    }

    protected Object maskNullValue(V value) {
        return value == null ? NullSentinel.NULL_VALUE : value;
    }

    @SuppressWarnings("unchecked")
    protected V unmaskNullValue(Object value) {
        return value == NullSentinel.NULL_VALUE ? null : (V) value;
    }

    // Implement shared ConcurrentMap and Map methods

    @Override
    public int size() {
        return internalMap.size();
    }

    @Override
    public boolean isEmpty() {
        return internalMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return internalMap.containsKey(maskNullKey((K) key));
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return internalMap.containsValue(NullSentinel.NULL_VALUE);
        }
        return internalMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        Object val = internalMap.get(maskNullKey((K) key));
        return unmaskNullValue(val);
    }

    @Override
    public V put(K key, V value) {
        Object prev = internalMap.put(maskNullKey(key), maskNullValue(value));
        return unmaskNullValue(prev);
    }

    @Override
    public V remove(Object key) {
        Object prev = internalMap.remove(maskNullKey((K) key));
        return unmaskNullValue(prev);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            internalMap.put(maskNullKey(entry.getKey()), maskNullValue(entry.getValue()));
        }
    }

    @Override
    public void clear() {
        internalMap.clear();
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Object val = internalMap.get(maskNullKey((K) key));
        return (val != null) ? unmaskNullValue(val) : defaultValue;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        Object prev = internalMap.putIfAbsent(maskNullKey(key), maskNullValue(value));
        return unmaskNullValue(prev);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return internalMap.remove(maskNullKey((K) key), maskNullValue((V) value));
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return internalMap.replace(maskNullKey(key), maskNullValue(oldValue), maskNullValue(newValue));
    }

    @Override
    public V replace(K key, V value) {
        Object prev = internalMap.replace(maskNullKey(key), maskNullValue(value));
        return unmaskNullValue(prev);
    }

    @Override
    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        Object maskedKey = maskNullKey(key);

        Object result = internalMap.compute(maskedKey, (k, v) -> {
            if (v != null && v != NullSentinel.NULL_VALUE) {
                // Existing non-null value remains untouched
                return v;
            }

            V newValue = mappingFunction.apply(unmaskNullKey(k));
            return (newValue == null) ? null : maskNullValue(newValue);
        });

        return unmaskNullValue(result);
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Object maskedKey = maskNullKey(key);
        Object result = internalMap.compute(maskedKey, (k, v) -> {
            V oldValue = unmaskNullValue(v);
            V newValue = remappingFunction.apply(unmaskNullKey(k), oldValue);
            return (newValue == null) ? null : maskNullValue(newValue);
        });

        return unmaskNullValue(result);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value); // Adjust based on whether you want to allow nulls
        Object maskedKey = maskNullKey(key);
        Object result = internalMap.merge(maskedKey, maskNullValue(value), (v1, v2) -> {
            V unmaskV1 = unmaskNullValue(v1);
            V unmaskV2 = unmaskNullValue(v2);
            V newValue = remappingFunction.apply(unmaskV1, unmaskV2);
            return (newValue == null) ? null : maskNullValue(newValue);
        });

        return unmaskNullValue(result);
    }

    // Implement shared values() and entrySet() methods

    @Override
    public Collection<V> values() {
        Collection<Object> internalValues = internalMap.values();
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                Iterator<Object> it = internalValues.iterator();
                return new Iterator<V>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public V next() {
                        return unmaskNullValue(it.next());
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }

            @Override
            public int size() {
                return internalValues.size();
            }

            @Override
            public boolean contains(Object o) {
                return internalMap.containsValue(maskNullValue((V) o));
            }

            @Override
            public void clear() {
                internalMap.clear();
            }
        };
    }

    @Override
    public Set<K> keySet() {
        Set<Object> internalKeys = internalMap.keySet();
        return new AbstractSet<K>() {
            @Override
            public Iterator<K> iterator() {
                Iterator<Object> it = internalKeys.iterator();
                return new Iterator<K>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public K next() {
                        return unmaskNullKey(it.next());
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }

            @Override
            public int size() {
                return internalKeys.size();
            }

            @Override
            public boolean contains(Object o) {
                return internalMap.containsKey(maskNullKey((K) o));
            }

            @Override
            public boolean remove(Object o) {
                return internalMap.remove(maskNullKey((K) o)) != null;
            }

            @Override
            public void clear() {
                internalMap.clear();
            }
        };
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<Object, Object>> internalEntries = internalMap.entrySet();
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                Iterator<Entry<Object, Object>> it = internalEntries.iterator();
                return new Iterator<Entry<K, V>>() {
                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Entry<K, V> next() {
                        Entry<Object, Object> internalEntry = it.next();
                        return new Entry<K, V>() {
                            @Override
                            public K getKey() {
                                return unmaskNullKey(internalEntry.getKey());
                            }

                            @Override
                            public V getValue() {
                                return unmaskNullValue(internalEntry.getValue());
                            }

                            @Override
                            public V setValue(V value) {
                                Object oldValue = internalEntry.setValue(maskNullValue(value));
                                return unmaskNullValue(oldValue);
                            }

                            @Override
                            public boolean equals(Object o) {
                                if (!(o instanceof Entry)) return false;
                                Entry<?, ?> e = (Entry<?, ?>) o;
                                return Objects.equals(getKey(), e.getKey()) &&
                                        Objects.equals(getValue(), e.getValue());
                            }

                            @Override
                            public int hashCode() {
                                return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
                            }

                            @Override
                            public String toString() {
                                return getKey() + "=" + getValue();
                            }
                        };
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }

            @Override
            public int size() {
                return internalEntries.size();
            }

            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Entry)) return false;
                Entry<?, ?> e = (Entry<?, ?>) o;
                Object val = internalMap.get(maskNullKey((K) e.getKey()));
                return maskNullValue((V) e.getValue()).equals(val);
            }

            @Override
            public boolean remove(Object o) {
                if (!(o instanceof Entry)) return false;
                Entry<?, ?> e = (Entry<?, ?>) o;
                return internalMap.remove(maskNullKey((K) e.getKey()), maskNullValue((V) e.getValue()));
            }

            @Override
            public void clear() {
                internalMap.clear();
            }
        };
    }

    /**
     * Overrides the equals method to ensure proper comparison between two maps.
     * Two maps are considered equal if they contain the same key-value mappings.
     *
     * @param o the object to be compared for equality with this map
     * @return true if the specified object is equal to this map
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Map)) return false;
        Map<?, ?> other = (Map<?, ?>) o;
        if (this.size() != other.size()) return false;
        for (Entry<K, V> entry : this.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            if (!other.containsKey(key)) return false;
            Object otherValue = other.get(key);
            if (!Objects.equals(value, otherValue)) return false;
        }
        return true;
    }

    /**
     * Overrides the hashCode method to ensure consistency with equals.
     * The hash code of a map is defined to be the sum of the hash codes of each entry in the map.
     *
     * @return the hash code value for this map
     */
    @Override
    public int hashCode() {
        int h = 0;
        for (Entry<K, V> entry : this.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            int keyHash = (key == null) ? 0 : key.hashCode();
            int valueHash = (value == null) ? 0 : value.hashCode();
            h += keyHash ^ valueHash;
        }
        return h;
    }

    /**
     * Overrides the toString method to provide a string representation of the map.
     * The string representation consists of a list of key-value mappings in the order returned by the map's entrySet view's iterator,
     * enclosed in braces ("{}"). Adjacent mappings are separated by the characters ", " (comma and space).
     *
     * @return a string representation of this map
     */
    @Override
    public String toString() {
        return MapUtilities.mapToString(this);
    }
}
