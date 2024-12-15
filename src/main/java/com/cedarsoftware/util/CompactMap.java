package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;

/**
 * A memory-efficient Map implementation that optimizes storage based on size.
 * CompactMap uses only one instance variable of type Object and changes its internal
 * representation as the map grows, achieving memory savings while maintaining
 * performance comparable to HashMap.
 *
 * <h2>Storage Strategy</h2>
 * The map uses different internal representations based on size:
 * <ul>
 *   <li><b>Empty (size=0):</b> Single sentinel value</li>
 *   <li><b>Single Entry (size=1):</b>
 *     <ul>
 *       <li>If key matches {@link #getSingleValueKey()}: Stores only the value</li>
 *       <li>Otherwise: Uses a compact CompactMapEntry containing key and value</li>
 *     </ul>
 *   </li>
 *   <li><b>Multiple Entries (2 ≤ size ≤ compactSize()):</b> Single Object[] storing
 *       alternating keys and values at even/odd indices</li>
 *   <li><b>Large Maps (size > compactSize()):</b> Delegates to standard Map implementation</li>
 * </ul>
 *
 * <h2>Customization Points</h2>
 * The following methods can be overridden to customize behavior:
 *
 * <pre>{@code
 * // Key used for optimized single-entry storage
 * protected K getSingleValueKey() { return "someKey"; }
 *
 * // Map implementation for large maps (size > compactSize)
 * protected Map<K,V> getNewMap() { return new HashMap<>(); }
 *
 * // Enable case-insensitive key comparison
 * protected boolean isCaseInsensitive() { return false; }
 *
 * // Threshold at which to switch to standard Map implementation
 * protected int compactSize() { return 80; }
 * }</pre>
 *
 * <h2>Additional Notes</h2>
 * <ul>
 *   <li>Supports null keys and values if the backing Map implementation does</li>
 *   <li>Thread safety depends on the backing Map implementation</li>
 *   <li>Particularly memory efficient for maps of size 0-1</li>
 * </ul>
 *
 * @param <K> The type of keys maintained by this map
 * @param <V> The type of mapped values
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
 * @see HashMap
 */
@SuppressWarnings("unchecked")
public class CompactMap<K, V> implements Map<K, V> {
    private static final String EMPTY_MAP = "_︿_ψ_☼";

    // Constants for option keys
    public static final String COMPACT_SIZE = "compactSize";
    public static final String CAPACITY = "capacity";
    public static final String CASE_SENSITIVE = "caseSensitive";
    public static final String MAP_TYPE = "mapType";
    public static final String USE_COPY_ITERATOR = "useCopyIterator";
    public static final String SINGLE_KEY = "singleKey";
    public static final String SOURCE_MAP = "source";

    // Constants for ordering options
    public static final String ORDERING = "ordering";
    public static final String UNORDERED = "unordered";
    public static final String SORTED = "sorted";
    public static final String INSERTION = "insertion";
    public static final String REVERSE = "reverse";
    public static final String COMPARATOR = "comparator";

    // Default values
    private static final int DEFAULT_COMPACT_SIZE = 80;
    private static final int DEFAULT_CAPACITY = 16;
    private static final boolean DEFAULT_CASE_SENSITIVE = true;
    private static final boolean DEFAULT_USE_COPY_ITERATOR = false;
    private static final Class<? extends Map> DEFAULT_MAP_TYPE = HashMap.class;
    private Object val = EMPTY_MAP;

    // Ordering comparator for maintaining order in compact array
    private final Comparator<K> orderingComparator;

    /**
     * Constructs an empty CompactMap with the default configuration.
     * <p>
     * This constructor ensures that the `compactSize()` method returns a value greater than or equal to 2.
     * </p>
     *
     * @throws IllegalStateException if {@link #compactSize()} returns a value less than 2
     */
    public CompactMap() {
        this((Comparator<K>) null);
    }

    public CompactMap(Comparator<K> comparator) {
        if (compactSize() < 2) {
            throw new IllegalStateException("compactSize() must be >= 2");
        }
        this.orderingComparator = comparator;
    }

    /**
     * Constructs a CompactMap initialized with the entries from the provided map.
     * <p>
     * The entries are copied from the provided map, and the internal representation
     * is determined based on the number of entries and the {@link #compactSize()} threshold.
     * </p>
     *
     * @param other the map whose entries are to be placed in this map
     * @throws NullPointerException if {@code other} is null
     */
    public CompactMap(Map<K, V> other) {
        this();
        putAll(other);
    }

    /**
     * Returns the number of key-value mappings in this map.
     * <p>
     * If the map contains more than {@link Integer#MAX_VALUE} elements, returns {@link Integer#MAX_VALUE}.
     * </p>
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        if (val instanceof Object[]) {   // 2 to compactSize
            return ((Object[]) val).length >> 1;
        } else if (val instanceof Map) {   // > compactSize
            return ((Map<K, V>) val).size();
        } else if (val == EMPTY_MAP) {   // empty
            return 0;
        }

        // size == 1
        return 1;
    }

    /**
     * Returns {@code true} if this map contains no key-value mappings.
     *
     * @return {@code true} if this map contains no key-value mappings; {@code false} otherwise
     */
    public boolean isEmpty() {
        return val == EMPTY_MAP;
    }

    private boolean compareKeys(Object key, Object aKey) {
        if (key instanceof String) {
            if (aKey instanceof String) {
                if (isCaseInsensitive()) {
                    return ((String) aKey).equalsIgnoreCase((String) key);
                } else {
                    return aKey.equals(key);
                }
            }
            return false;
        }

        return Objects.equals(key, aKey);
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     *
     * @param key the key whose presence in this map is to be tested
     * @return {@code true} if this map contains a mapping for the specified key; {@code false} otherwise
     */
    public boolean containsKey(Object key) {
        if (val instanceof Object[]) {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            final int len = entries.length;
            for (int i = 0; i < len; i += 2) {
                if (compareKeys(key, entries[i])) {
                    return true;
                }
            }
            return false;
        } else if (val instanceof Map) {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            return map.containsKey(key);
        } else if (val == EMPTY_MAP) {   // empty
            return false;
        }

        // size == 1
        return compareKeys(key, getLogicalSingleKey());
    }

    /**
     * Returns {@code true} if this map maps one or more keys to the specified value.
     *
     * @param value the value whose presence in this map is to be tested
     * @return {@code true} if this map maps one or more keys to the specified value;
     * {@code false} otherwise
     */
    public boolean containsValue(Object value) {
        if (val instanceof Object[]) {   // 2 to Compactsize
            Object[] entries = (Object[]) val;
            final int len = entries.length;
            for (int i = 0; i < len; i += 2) {
                Object aValue = entries[i + 1];
                if (Objects.equals(value, aValue)) {
                    return true;
                }
            }
            return false;
        } else if (val instanceof Map) {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            return map.containsValue(value);
        } else if (val == EMPTY_MAP) {   // empty
            return false;
        }

        // size == 1
        return getLogicalSingleValue() == value;
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key.
     * <p>
     * A return value of {@code null} does not necessarily indicate that the map contains no mapping for the key; it is also
     * possible that the map explicitly maps the key to {@code null}.
     * </p>
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or {@code null} if this map contains no mapping for the key
     */
    public V get(Object key) {
        if (val instanceof Object[]) {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            for (int i = 0; i < entries.length; i += 2) {
                if (compareKeys(key, entries[i])) {
                    return (V) entries[i + 1];
                }
            }
            return null;
        } else if (val instanceof Map) {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            return map.get(key);
        } else if (val == EMPTY_MAP) {   // empty
            return null;
        }

        // size == 1
        if (compareKeys(key, getLogicalSingleKey())) {
            return getLogicalSingleValue();
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     */
    @Override
    public V put(K key, V value) {
        if (val instanceof Object[]) {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            return putInCompactArray(entries, key, value);
        } else if (val instanceof Map) {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            return map.put(key, value);
        } else if (val == EMPTY_MAP) {   // empty
            if (compareKeys(key, getLogicalSingleKey()) && !(value instanceof Map || value instanceof Object[])) {
                val = value;
            } else {
                val = new CompactMapEntry(key, value);
            }
            return null;
        }

        // size == 1
        return handleSingleEntryPut(key, value);
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     */
    @Override
    public V remove(Object key) {
        if (val instanceof Object[]) {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            return removeFromCompactArray(entries, key);
        } else if (val instanceof Map) {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            return removeFromMap(map, key);
        } else if (val == EMPTY_MAP) {   // empty
            return null;
        }

        // size == 1
        return handleSingleEntryRemove(key);
    }

    private V putInCompactArray(Object[] entries, K key, V value) {
        final int len = entries.length;
        for (int i = 0; i < len; i += 2) {
            Object aKey = entries[i];
            if (compareKeys(key, aKey)) {
                V oldValue = (V) entries[i + 1];
                entries[i + 1] = value;
                return oldValue;
            }
        }

        if (size() < compactSize()) {
            Object[] expand = new Object[len + 2];
            System.arraycopy(entries, 0, expand, 0, len);
            expand[len] = key;
            expand[len + 1] = value;

            sortCompactArray(expand); // Delegate sorting
            val = expand;
        } else {
            switchToMap(entries, key, value);
        }
        return null;
    }

    /**
     * Removes a key-value pair from the compact array while preserving order.
     */
    private V removeFromCompactArray(Object[] entries, Object key) {
        if (size() == 2) {   // Transition back to single entry
            return handleTransitionToSingleEntry(entries, key);
        }

        final int len = entries.length;
        for (int i = 0; i < len; i += 2) {
            if (compareKeys(key, entries[i])) {
                V oldValue = (V) entries[i + 1];
                Object[] shrink = new Object[len - 2];
                System.arraycopy(entries, 0, shrink, 0, i);
                System.arraycopy(entries, i + 2, shrink, i, shrink.length - i);
                sortCompactArray(shrink); // Centralized sorting logic
                val = shrink;
                return oldValue;
            }
        }
        return null;
    }

    private void sortCompactArray(Object[] array) {
        // Determine if sorting is required
        if (getOrdering().equals(UNORDERED)) {
            return; // No sorting needed for unordered maps
        }

        int size = array.length / 2;
        Object[] keys = new Object[size];
        Object[] values = new Object[size];

        for (int i = 0; i < size; i++) {
            keys[i] = array[i * 2];
            values[i] = array[(i * 2) + 1];
        }

        // Fetch the comparator to use
        final Comparator<? super K> comparatorToUse;
        if (getOrdering().equals(REVERSE)) {
            comparatorToUse = getReverseComparator((Comparator<? super K>) getComparator());
        } else {
            comparatorToUse = getComparator();
        }

        // Sort keys using the determined comparator
        Arrays.sort(keys, (o1, o2) -> {
            if (comparatorToUse != null) {
                return comparatorToUse.compare((K) o1, (K) o2);
            }
            return 0;
        });

        for (int i = 0; i < size; i++) {
            array[i * 2] = keys[i];
            array[(i * 2) + 1] = values[i];
        }
    }

    private void switchToMap(Object[] entries, K key, V value) {
        Map<K, V> map = getNewMap(size() + 1);
        for (int i = 0; i < entries.length; i += 2) {
            map.put((K) entries[i], (V) entries[i + 1]);
        }
        map.put(key, value);
        val = map;
    }

    /**
     * Returns a comparator that reverses the order of the given comparator.
     * <p>
     * If the provided comparator is {@code null}, the resulting comparator
     * uses the natural reverse order of the keys.
     * </p>
     *
     * @param <T> the type of elements compared by the comparator
     * @param original the original comparator to be reversed, or {@code null} for natural reverse order
     * @return a comparator that reverses the given comparator, or natural reverse order if {@code original} is {@code null}
     */
    private static <T> Comparator<? super T> getReverseComparator(Comparator<T> original) {
        return (o1, o2) -> {
            if (original != null) {
                return original.compare(o2, o1); // Reverse the order using the provided comparator
            }
            Comparable<T> c1 = (Comparable<T>) o1;
            return c1.compareTo(o2); // Default to reverse natural order
        };
    }
    
    /**
     * Handles the case where the array is reduced to a single entry during removal.
     */
    private V handleTransitionToSingleEntry(Object[] entries, Object key) {
        if (compareKeys(key, entries[0])) {
            Object prevValue = entries[1];
            clear();
            put((K) entries[2], (V) entries[3]);
            return (V) prevValue;
        } else if (compareKeys(key, entries[2])) {
            Object prevValue = entries[3];
            clear();
            put((K) entries[0], (V) entries[1]);
            return (V) prevValue;
        }
        return null;
    }

    /**
     * Handles a put operation when the map has a single entry.
     */
    private V handleSingleEntryPut(K key, V value) {
        if (compareKeys(key, getLogicalSingleKey())) {   // Overwrite
            V save = getLogicalSingleValue();
            if (compareKeys(key, getSingleValueKey()) && !(value instanceof Map || value instanceof Object[])) {
                val = value;
            } else {
                val = new CompactMapEntry(key, value);
            }
            return save;
        } else {   // CompactMapEntry to []
            Object[] entries = new Object[4];
            entries[0] = getLogicalSingleKey();
            entries[1] = getLogicalSingleValue();
            entries[2] = key;
            entries[3] = value;
            val = entries;
            return null;
        }
    }

    /**
     * Handles a remove operation when the map has a single entry.
     */
    private V handleSingleEntryRemove(Object key) {
        if (compareKeys(key, getLogicalSingleKey())) {   // Found
            V save = getLogicalSingleValue();
            val = EMPTY_MAP;
            return save;
        }
        return null;   // Not found
    }

    /**
     * Removes a key-value pair from the map and transitions back to compact storage if needed.
     */
    private V removeFromMap(Map<K, V> map, Object key) {
        if (!map.containsKey(key)) {
            return null;
        }
        V save = map.remove(key);

        if (map.size() == compactSize()) {   // Transition back to Object[]
            Object[] entries = new Object[compactSize() * 2];
            int idx = 0;
            for (Entry<K, V> entry : map.entrySet()) {
                entries[idx] = entry.getKey();
                entries[idx + 1] = entry.getValue();
                idx += 2;
            }
            val = entries;
        }
        return save;
    }

    /**
     * Copies all the mappings from the specified map to this map. The effect of this call is equivalent
     * to calling {@link #put(Object, Object)} on this map once for each mapping in the specified map.
     *
     * @param map mappings to be stored in this map
     */
    public void putAll(Map<? extends K, ? extends V> map) {
        if (map == null) {
            return;
        }
        int mSize = map.size();
        if (val instanceof Map || mSize > compactSize()) {
            if (val == EMPTY_MAP) {
                val = getNewMap(mSize);
            }
            ((Map<K, V>) val).putAll(map);
        } else {
            for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Removes all the mappings from this map. The map will be empty after this call returns.
     */
    public void clear() {
        val = EMPTY_MAP;
    }

    /**
     * Returns the hash code value for this map.
     * <p>
     * The hash code of a map is defined as the sum of the hash codes of each entry in the map's entry set.
     * This implementation ensures consistency with the `equals` method.
     * </p>
     *
     * @return the hash code value for this map
     */
    public int hashCode() {
        if (val instanceof Object[]) {
            int h = 0;
            Object[] entries = (Object[]) val;
            final int len = entries.length;
            for (int i = 0; i < len; i += 2) {
                Object aKey = entries[i];
                Object aValue = entries[i + 1];
                h += computeKeyHashCode(aKey) ^ computeValueHashCode(aValue);
            }
            return h;
        } else if (val instanceof Map) {
            return val.hashCode();
        } else if (val == EMPTY_MAP) {
            return 0;
        }

        // size == 1
        return computeKeyHashCode(getLogicalSingleKey()) ^ computeValueHashCode(getLogicalSingleValue());
    }

    /**
     * Compares the specified object with this map for equality.
     * <p>
     * Returns {@code true} if the given object is also a map and the two maps represent the same mappings.
     * More formally, two maps {@code m1} and {@code m2} are equal if:
     * </p>
     * <pre>{@code
     * m1.entrySet().equals(m2.entrySet())
     * }</pre>
     *
     * @param obj the object to be compared for equality with this map
     * @return {@code true} if the specified object is equal to this map
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Map)) {
            return false;
        }
        Map<?, ?> other = (Map<?, ?>) obj;
        if (size() != other.size()) {
            return false;
        }

        if (val instanceof Object[]) {   // 2 to compactSize
            for (Entry<?, ?> entry : other.entrySet()) {
                final Object thatKey = entry.getKey();
                if (!containsKey(thatKey)) {
                    return false;
                }

                Object thatValue = entry.getValue();
                Object thisValue = get(thatKey);

                if (thatValue == null || thisValue == null) {   // Perform null checks
                    if (thatValue != thisValue) {
                        return false;
                    }
                } else if (!thisValue.equals(thatValue)) {
                    return false;
                }
            }
        } else if (val instanceof Map) {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            return map.equals(other);
        } else if (val == EMPTY_MAP) {   // empty
            return other.isEmpty();
        }

        // size == 1
        return entrySet().equals(other.entrySet());
    }

    /**
     * Returns a string representation of this map.
     * <p>
     * The string representation consists of a list of key-value mappings in the order returned by the map's
     * {@code entrySet} iterator, enclosed in braces ({@code "{}"}). Adjacent mappings are separated by the characters
     * {@code ", "} (comma and space). Each key-value mapping is rendered as the key followed by an equals sign
     * ({@code "="}) followed by the associated value.
     * </p>
     *
     * @return a string representation of this map
     */
    public String toString() {
        return MapUtilities.mapToString(this);
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     * <p>
     * The set is backed by the map, so changes to the map are reflected in the set, and vice versa. If the map
     * is modified while an iteration over the set is in progress (except through the iterator's own
     * {@code remove} operation), the results of the iteration are undefined. The set supports element removal,
     * which removes the corresponding mapping from the map. It does not support the {@code add} or {@code addAll}
     * operations.
     * </p>
     *
     * @return a set view of the keys contained in this map
     */
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            public Iterator<K> iterator() {
                if (useCopyIterator()) {
                    return new CopyKeyIterator();
                } else {
                    return new CompactKeyIterator();
                }
            }

            public int size() {
                return CompactMap.this.size();
            }

            public void clear() {
                CompactMap.this.clear();
            }

            public boolean contains(Object o) {
                return CompactMap.this.containsKey(o);
            }    // faster than inherited method

            public boolean remove(Object o) {
                final int size = size();
                CompactMap.this.remove(o);
                return size() != size;
            }

            public boolean removeAll(Collection c) {
                int size = size();
                for (Object o : c) {
                    CompactMap.this.remove(o);
                }
                return size() != size;
            }

            public boolean retainAll(Collection c) {
                // Create fast-access O(1) to all elements within passed in Collection
                Map<K, V> other = new CompactMap<K, V>() {   // Match outer
                    protected boolean isCaseInsensitive() {
                        return CompactMap.this.isCaseInsensitive();
                    }

                    protected int compactSize() {
                        return CompactMap.this.compactSize();
                    }

                    protected Map<K, V> getNewMap() {
                        return CompactMap.this.getNewMap(c.size());
                    }
                };
                for (Object o : c) {
                    other.put((K) o, null);
                }

                final int size = size();
                keySet().removeIf(key -> !other.containsKey(key));

                return size() != size;
            }
        };
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     * <p>
     * The collection is backed by the map, so changes to the map are reflected in the collection, and vice versa.
     * If the map is modified while an iteration over the collection is in progress (except through the iterator's
     * own {@code remove} operation), the results of the iteration are undefined. The collection supports element
     * removal, which removes the corresponding mapping from the map. It does not support the {@code add} or
     * {@code addAll} operations.
     * </p>
     *
     * @return a collection view of the values contained in this map
     */
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            public Iterator<V> iterator() {
                if (useCopyIterator()) {
                    return new CopyValueIterator();
                } else {
                    return new CompactValueIterator();
                }
            }

            public int size() {
                return CompactMap.this.size();
            }

            public void clear() {
                CompactMap.this.clear();
            }
        };
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     * <p>
     * Each element in the returned set is a {@code Map.Entry}. The set is backed by the map, so changes to the map
     * are reflected in the set, and vice versa. If the map is modified while an iteration over the set is in progress
     * (except through the iterator's own {@code remove} operation, or through the {@code setValue} operation on a map
     * entry returned by the iterator), the results of the iteration are undefined. The set supports element removal,
     * which removes the corresponding mapping from the map. It does not support the {@code add} or {@code addAll}
     * operations.
     * </p>
     *
     * @return a set view of the mappings contained in this map
     */
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            public Iterator<Entry<K, V>> iterator() {
                if (useCopyIterator()) {
                    return new CopyEntryIterator();
                } else {
                    return new CompactEntryIterator();
                }
            }

            public int size() {
                return CompactMap.this.size();
            }

            public void clear() {
                CompactMap.this.clear();
            }

            public boolean contains(Object o) {   // faster than inherited method
                if (o instanceof Entry) {
                    Entry<K, V> entry = (Entry<K, V>) o;
                    K entryKey = entry.getKey();

                    Object value = CompactMap.this.get(entryKey);
                    if (value != null) {   // Found non-null value with key, return true if values are equals()
                        return Objects.equals(value, entry.getValue());
                    } else if (CompactMap.this.containsKey(entryKey)) {
                        value = CompactMap.this.get(entryKey);
                        return Objects.equals(value, entry.getValue());
                    }
                }
                return false;
            }

            public boolean remove(Object o) {
                if (!(o instanceof Entry)) {
                    return false;
                }
                final int size = size();
                Entry<K, V> that = (Entry<K, V>) o;
                CompactMap.this.remove(that.getKey());
                return size() != size;
            }

            /**
             * This method is required.  JDK method is broken, as it relies
             * on iterator solution.  This method is fast because contains()
             * and remove() are both hashed O(1) look-ups.
             */
            public boolean removeAll(Collection c) {
                final int size = size();
                for (Object o : c) {
                    remove(o);
                }
                return size() != size;
            }

            public boolean retainAll(Collection c) {
                // Create fast-access O(1) to all elements within passed in Collection
                Map<K, V> other = new CompactMap<K, V>() {   // Match outer
                    protected boolean isCaseInsensitive() {
                        return CompactMap.this.isCaseInsensitive();
                    }

                    protected int compactSize() {
                        return CompactMap.this.compactSize();
                    }

                    protected Map<K, V> getNewMap() {
                        return CompactMap.this.getNewMap(c.size());
                    }
                };
                for (Object o : c) {
                    if (o instanceof Entry) {
                        other.put(((Entry<K, V>) o).getKey(), ((Entry<K, V>) o).getValue());
                    }
                }

                int origSize = size();

                // Drop all items that are not in the passed in Collection
                Iterator<Entry<K, V>> i = entrySet().iterator();
                while (i.hasNext()) {
                    Entry<K, V> entry = i.next();
                    K key = entry.getKey();
                    V value = entry.getValue();
                    if (!other.containsKey(key)) {   // Key not even present, nuke the entry
                        i.remove();
                    } else {   // Key present, now check value match
                        Object v = other.get(key);
                        if (!Objects.equals(v, value)) {
                            i.remove();
                        }
                    }
                }

                return size() != origSize;
            }
        };
    }

    private Map<K, V> getCopy() {
        Map<K, V> copy = getNewMap(size());   // Use their Map (TreeMap, HashMap, LinkedHashMap, etc.)
        if (val instanceof Object[]) {   // 2 to compactSize - copy Object[] into Map
            Object[] entries = (Object[]) CompactMap.this.val;
            final int len = entries.length;
            for (int i = 0; i < len; i += 2) {
                copy.put((K) entries[i], (V) entries[i + 1]);
            }
        } else if (val instanceof Map) {   // > compactSize - putAll to copy
            copy.putAll((Map<K, V>) CompactMap.this.val);
        } else if (val == EMPTY_MAP) {   // empty - nothing to copy
        } else {   // size == 1
            copy.put(getLogicalSingleKey(), getLogicalSingleValue());
        }
        return copy;
    }

    private void iteratorRemove(Entry<K, V> currentEntry) {
        if (currentEntry == null) {   // remove() called on iterator prematurely
            throw new IllegalStateException("remove() called on an Iterator before calling next()");
        }

        remove(currentEntry.getKey());
    }

    @Deprecated
    public Map<K, V> minus(Object removeMe) {
        throw new UnsupportedOperationException("Unsupported operation [minus] or [-] between Maps.  Use removeAll() or retainAll() instead.");
    }

    @Deprecated
    public Map<K, V> plus(Object right) {
        throw new UnsupportedOperationException("Unsupported operation [plus] or [+] between Maps.  Use putAll() instead.");
    }

    protected enum LogicalValueType {
        EMPTY, OBJECT, ENTRY, MAP, ARRAY
    }

    protected LogicalValueType getLogicalValueType() {
        if (val instanceof Object[]) {   // 2 to compactSize
            return LogicalValueType.ARRAY;
        } else if (val instanceof Map) {   // > compactSize
            return LogicalValueType.MAP;
        } else if (val == EMPTY_MAP) {   // empty
            return LogicalValueType.EMPTY;
        } else {   // size == 1
            if (CompactMapEntry.class.isInstance(val)) {
                return LogicalValueType.ENTRY;
            } else {
                return LogicalValueType.OBJECT;
            }
        }
    }

    /**
     * Marker Class to hold key and value when the key is not the same as the getSingleValueKey().
     * This method transmits the setValue() changes to the outer CompactMap instance.
     */
    public class CompactMapEntry extends AbstractMap.SimpleEntry<K, V> {
        public CompactMapEntry(K key, V value) {
            super(key, value);
        }

        public V setValue(V value) {
            V save = this.getValue();
            super.setValue(value);
            CompactMap.this.put(getKey(), value);    // "Transmit" (write-thru) to underlying Map.
            return save;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            if (o == this) {
                return true;
            }

            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return compareKeys(getKey(), e.getKey()) && Objects.equals(getValue(), e.getValue());
        }

        public int hashCode() {
            return computeKeyHashCode(getKey()) ^ computeValueHashCode(getValue());
        }
    }

    protected int computeKeyHashCode(Object key) {
        if (key instanceof String) {
            if (isCaseInsensitive()) {
                return StringUtilities.hashCodeIgnoreCase((String) key);
            } else {   // k can't be null here (null is not instanceof String)
                return key.hashCode();
            }
        } else {
            int keyHash;
            if (key == null) {
                return 0;
            } else {
                keyHash = key == CompactMap.this ? 37 : key.hashCode();
            }
            return keyHash;
        }
    }

    protected int computeValueHashCode(Object value) {
        if (value == CompactMap.this) {
            return 17;
        } else {
            return value == null ? 0 : value.hashCode();
        }
    }

    private K getLogicalSingleKey() {
        if (CompactMapEntry.class.isInstance(val)) {
            CompactMapEntry entry = (CompactMapEntry) val;
            return entry.getKey();
        }
        return getSingleValueKey();
    }

    private V getLogicalSingleValue() {
        if (CompactMapEntry.class.isInstance(val)) {
            CompactMapEntry entry = (CompactMapEntry) val;
            return entry.getValue();
        }
        return (V) val;
    }

    /**
     * @return String key name when there is only one entry in the Map.
     */
    protected K getSingleValueKey() {
        return (K) "key";
    }
    
    /**
     * @return new empty Map instance to use when size() becomes {@literal >} compactSize().
     */
    protected Map<K, V> getNewMap() {
        Map<K, V> map = new HashMap<>(compactSize() + 1); // Default behavior
        return map;
    }
    
    protected Map<K, V> getNewMap(int size) {
        Map<K, V> map = getNewMap();
        try {
            Constructor<?> constructor = ReflectionUtils.getConstructor(map.getClass(), Integer.TYPE);
            return (Map<K, V>) constructor.newInstance(size);
        } catch (Exception e) {
            return map;
        }
    }

    protected boolean isCaseInsensitive() {
        return false;
    }

    protected int compactSize() {
        return 80;
    }

    protected boolean useCopyIterator() {
        Map<K, V> newMap = getNewMap();
        if (newMap instanceof CaseInsensitiveMap) {
            newMap = ((CaseInsensitiveMap<K, V>) newMap).getWrappedMap();
        }
        return newMap instanceof SortedMap;
    }

    /**
     * Returns the ordering strategy for this map.
     * <p>
     * Valid values include:
     * <ul>
     *   <li>{@link #INSERTION}: Maintains insertion order.</li>
     *   <li>{@link #SORTED}: Maintains sorted order based on the {@link #getComparator()}.</li>
     *   <li>{@link #REVERSE}: Maintains reverse order based on the {@link #getComparator()} or natural reverse order.</li>
     *   <li>{@link #UNORDERED}: Default unordered behavior.</li>
     * </ul>
     * </p>
     *
     * @return the ordering strategy for this map
     */
    protected String getOrdering() {
        return UNORDERED; // Default: unordered
    }

    /**
     * Returns the comparator used for sorting entries in this map.
     * <p>
     * If {@link #getOrdering()} is {@link #SORTED} or {@link #REVERSE}, the returned comparator determines the order.
     * If {@code null}, natural ordering is used.
     * </p>
     *
     * @return the comparator used for sorting, or {@code null} for natural ordering
     */
    protected Comparator<? super K> getComparator() {
        return null; // Default: natural ordering
    }

    /* ------------------------------------------------------------ */
    // iterators

    abstract class CompactIterator {
        Iterator<Map.Entry<K, V>> mapIterator;
        Object current; // Map.Entry if > compactsize, key <= compactsize
        int expectedSize;  // for fast-fail
        int index;             // current slot

        CompactIterator() {
            expectedSize = size();
            current = EMPTY_MAP;
            index = -1;
            if (val instanceof Map) {
                mapIterator = ((Map<K, V>) val).entrySet().iterator();
            }
        }

        public final boolean hasNext() {
            if (mapIterator != null) {
                return mapIterator.hasNext();
            } else {
                return (index + 1) < size();
            }
        }

        final void advance() {
            if (expectedSize != size()) {
                throw new ConcurrentModificationException();
            }
            if (++index >= size()) {
                throw new NoSuchElementException();
            }
            if (mapIterator != null) {
                current = mapIterator.next();
            } else if (expectedSize == 1) {
                current = getLogicalSingleKey();
            } else {
                current = ((Object[]) val)[index * 2];
            }
        }

        public final void remove() {
            if (current == EMPTY_MAP) {
                throw new IllegalStateException();
            }
            if (size() != expectedSize) {
                throw new ConcurrentModificationException();
            }
            int newSize = expectedSize - 1;

            // account for the change in size
            if (mapIterator != null && newSize == compactSize()) {
                current = ((Map.Entry<K, V>) current).getKey();
                mapIterator = null;
            }

            // perform the remove
            if (mapIterator == null) {
                CompactMap.this.remove(current);
            } else {
                mapIterator.remove();
            }

            index--;
            current = EMPTY_MAP;
            expectedSize--;
        }
    }

    final class CompactKeyIterator extends CompactMap<K, V>.CompactIterator implements Iterator<K> {
        public K next() {
            advance();
            if (mapIterator != null) {
                return ((Map.Entry<K, V>) current).getKey();
            } else {
                return (K) current;
            }
        }
    }

    final class CompactValueIterator extends CompactMap<K, V>.CompactIterator implements Iterator<V> {
        public V next() {
            advance();
            if (mapIterator != null) {
                return ((Map.Entry<K, V>) current).getValue();
            } else if (expectedSize == 1) {
                return getLogicalSingleValue();
            } else {
                return (V) ((Object[]) val)[(index * 2) + 1];
            }
        }
    }

    final class CompactEntryIterator extends CompactMap<K, V>.CompactIterator implements Iterator<Map.Entry<K, V>> {
        public Map.Entry<K, V> next() {
            advance();
            if (mapIterator != null) {
                return (Map.Entry<K, V>) current;
            } else if (expectedSize == 1) {
                if (val instanceof CompactMap.CompactMapEntry) {
                    return (CompactMapEntry) val;
                } else {
                    return new CompactMapEntry(getLogicalSingleKey(), getLogicalSingleValue());
                }
            } else {
                Object[] objs = (Object[]) val;
                return new CompactMapEntry((K) objs[(index * 2)], (V) objs[(index * 2) + 1]);
            }
        }
    }

    abstract class CopyIterator {
        Iterator<Entry<K, V>> iter;
        Entry<K, V> currentEntry = null;

        public CopyIterator() {
            iter = getCopy().entrySet().iterator();
        }

        public final boolean hasNext() {
            return iter.hasNext();
        }

        public final Entry<K, V> nextEntry() {
            currentEntry = iter.next();
            return currentEntry;
        }

        public final void remove() {
            iteratorRemove(currentEntry);
            currentEntry = null;
        }
    }

    final class CopyKeyIterator extends CopyIterator implements Iterator<K> {
        public K next() {
            return nextEntry().getKey();
        }
    }

    final class CopyValueIterator extends CopyIterator implements Iterator<V> {
        public V next() {
            return nextEntry().getValue();
        }
    }

    final class CopyEntryIterator extends CompactMap<K, V>.CopyIterator implements Iterator<Map.Entry<K, V>> {
        public Map.Entry<K, V> next() {
            return nextEntry();
        }
    }

    /**
     * Creates a new CompactMap with advanced configuration options.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @param options a map of configuration options
     *                <ul>
     *                    <li>{@link #COMPACT_SIZE}: (Integer) Compact size threshold.</li>
     *                    <li>{@link #CAPACITY}: (Integer) Initial capacity of the map.</li>
     *                    <li>{@link #CASE_SENSITIVE}: (Boolean) Whether the map is case-sensitive.</li>
     *                    <li>{@link #MAP_TYPE}: (Class<? extends Map<K, V>>) Backing map type for large maps.</li>
     *                    <li>{@link #USE_COPY_ITERATOR}: (Boolean) Whether to use a copy-based iterator.</li>
     *                    <li>{@link #SINGLE_KEY}: (K) Key to optimize single-entry storage.</li>
     *                    <li>{@link #SOURCE_MAP}: (Map<K, V>) Source map to initialize entries.</li>
     *                </ul>
     * @return a new CompactMap instance with the specified options
     */
    public static <K, V> CompactMap<K, V> newMap(Map<String, Object> options) {
        options = validateOptions(options); // Validate and resolve conflicts
        int compactSize = (int) options.getOrDefault(COMPACT_SIZE, DEFAULT_COMPACT_SIZE);
        boolean caseSensitive = (boolean) options.getOrDefault(CASE_SENSITIVE, DEFAULT_CASE_SENSITIVE);
        boolean useCopyIterator = (boolean) options.getOrDefault(USE_COPY_ITERATOR, DEFAULT_USE_COPY_ITERATOR);
        Class<? extends Map<K, V>> mapType = (Class<? extends Map<K, V>>) options.getOrDefault(MAP_TYPE, DEFAULT_MAP_TYPE);
        Comparator<? super K> comparator = (Comparator<? super K>) options.get(COMPARATOR);
        K singleKey = (K) options.get(SINGLE_KEY);
        Map<K, V> source = (Map<K, V>) options.get(SOURCE_MAP);
        String ordering = (String) options.getOrDefault(ORDERING, UNORDERED);

        // Dynamically adjust capacity if a source map is provided
        int capacity = (source != null) ? source.size() : (int) options.getOrDefault(CAPACITY, DEFAULT_CAPACITY);

        CompactMap<K, V> map = new CompactMap<K, V>() {
            @Override
            protected Map<K, V> getNewMap() {
                try {
                    if (comparator != null && SortedMap.class.isAssignableFrom(mapType)) {
                        return mapType.getConstructor(Comparator.class).newInstance(comparator);
                    }
                    Constructor<? extends Map<K, V>> constructor = mapType.getConstructor(int.class);
                    return constructor.newInstance(capacity);
                } catch (Exception e) {
                    try {
                        return mapType.getDeclaredConstructor().newInstance();
                    } catch (Exception ex) {
                        throw new IllegalArgumentException("Unable to instantiate Map of type: " + mapType.getName(), ex);
                    }
                }
            }

            @Override
            protected boolean isCaseInsensitive() {
                return !caseSensitive;
            }

            @Override
            protected int compactSize() {
                return compactSize;
            }

            @Override
            protected boolean useCopyIterator() {
                return useCopyIterator;
            }

            @Override
            protected K getSingleValueKey() {
                return singleKey != null ? singleKey : super.getSingleValueKey();
            }

            @Override
            protected String getOrdering() {
                return ordering;
            }
        };

        // Populate the map with entries from the source map, if provided
        if (source != null) {
            map.putAll(source);
        }

        return map;
    }

    /**
     * Creates a new CompactMap with default configuration.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @return a new CompactMap instance
     */
    public static <K, V> CompactMap<K, V> newMap() {
        return newMap(DEFAULT_COMPACT_SIZE, DEFAULT_CASE_SENSITIVE);
    }

    /**
     * Creates a new CompactMap with a specified compact size.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @param compactSize the compact size threshold
     * @return a new CompactMap instance
     */
    public static <K, V> CompactMap<K, V> newMap(int compactSize) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with specified compact size and case sensitivity.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @param compactSize the compact size threshold
     * @param caseSensitive whether the map is case-sensitive
     * @return a new CompactMap instance
     */
    public static <K, V> CompactMap<K, V> newMap(int compactSize, boolean caseSensitive) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CASE_SENSITIVE, caseSensitive);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with specified compact size, initial capacity, and case sensitivity.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @param compactSize the compact size threshold
     * @param capacity the initial capacity of the map
     * @param caseSensitive whether the map is case-sensitive
     * @return a new CompactMap instance
     */
    public static <K, V> CompactMap<K, V> newMap(int compactSize, int capacity, boolean caseSensitive) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CAPACITY, capacity);
        options.put(CASE_SENSITIVE, caseSensitive);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with specified compact size, initial capacity, case sensitivity,
     * and backing map type.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @param compactSize the compact size threshold
     * @param capacity the initial capacity of the map
     * @param caseSensitive whether the map is case-sensitive
     * @param mapType the type of backing map for large sizes
     * @return a new CompactMap instance
     */
    public static <K, V> CompactMap<K, V> newMap(
            int compactSize,
            int capacity,
            boolean caseSensitive,
            Class<? extends Map<K, V>> mapType) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CAPACITY, capacity);
        options.put(CASE_SENSITIVE, caseSensitive);
        options.put(MAP_TYPE, mapType);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with specified compact size, case sensitivity,
     * backing map type, and initialized with the entries from a source map.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @param compactSize the compact size threshold
     * @param caseSensitive whether the map is case-sensitive
     * @param mapType the type of backing map for large sizes
     * @param source the source map to initialize the CompactMap; may be {@code null}
     * @return a new CompactMap instance initialized with the entries from the source map
     */
    public static <K, V> CompactMap<K, V> newMap(
            int compactSize,
            boolean caseSensitive,
            Class<? extends Map<K, V>> mapType,
            Map<K, V> source
    ) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CASE_SENSITIVE, caseSensitive);
        options.put(MAP_TYPE, mapType);
        options.put(SOURCE_MAP, source);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with specified compact size, case sensitivity,
     * and initialized with the entries from a source map.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @param compactSize the compact size threshold
     * @param caseSensitive whether the map is case-sensitive
     * @param source the source map to initialize the CompactMap; may be {@code null}
     * @return a new CompactMap instance initialized with the entries from the source map
     */
    public static <K, V> CompactMap<K, V> newMap(
            int compactSize,
            boolean caseSensitive,
            Map<K, V> source
    ) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CASE_SENSITIVE, caseSensitive);
        options.put(SOURCE_MAP, source);
        return newMap(options);
    }

    /**
     * Validates the provided configuration options and resolves conflicts.
     * Throws an {@link IllegalArgumentException} if the configuration is invalid.
     *
     * @param options a map of user-provided options
     * @return the resolved options map
     */
    private static Map<String, Object> validateOptions(Map<String, Object> options) {
        // Extract and set default values
        String ordering = (String) options.getOrDefault(ORDERING, UNORDERED);
        Class<? extends Map> mapType = (Class<? extends Map>) options.getOrDefault(MAP_TYPE, DEFAULT_MAP_TYPE);
        Comparator<?> comparator = (Comparator<?>) options.get(COMPARATOR);

        // Validate ordering and mapType compatibility
        if (ordering.equals(SORTED) && !SortedMap.class.isAssignableFrom(mapType)) {
            throw new IllegalArgumentException("Ordering 'sorted' requires a SortedMap type.");
        }

        if (ordering.equals(INSERTION) && !LinkedHashMap.class.isAssignableFrom(mapType)) {
            throw new IllegalArgumentException("Ordering 'insertion' requires a LinkedHashMap type.");
        }

        if (ordering.equals(REVERSE) && !SortedMap.class.isAssignableFrom(mapType)) {
            throw new IllegalArgumentException("Ordering 'reverse' requires a SortedMap type.");
        }

        // Handle reverse ordering with or without comparator
        if (ordering.equals(REVERSE)) {
            if (comparator == null) {
                comparator = getReverseComparator(null); // Default to reverse natural ordering
            } else {
                comparator = getReverseComparator((Comparator) comparator); // Reverse user-provided comparator
            }
            options.put(COMPARATOR, comparator);
        }

        // Ensure the comparator is compatible with the map type
        if (comparator != null && !SortedMap.class.isAssignableFrom(mapType)) {
            throw new IllegalArgumentException("Comparator can only be used with a SortedMap type.");
        }

        // Resolve any conflicts or set missing defaults
        if (ordering.equals(UNORDERED)) {
            options.put(COMPARATOR, null); // Unordered maps don't need a comparator
        }

        // Additional validation: Ensure SOURCE_MAP overrides capacity if provided
        Map<?, ?> sourceMap = (Map<?, ?>) options.get(SOURCE_MAP);
        if (sourceMap != null) {
            options.put(CAPACITY, sourceMap.size());
        }

        // Final default resolution
        options.putIfAbsent(COMPACT_SIZE, DEFAULT_COMPACT_SIZE);
        options.putIfAbsent(CAPACITY, DEFAULT_CAPACITY);
        options.putIfAbsent(CASE_SENSITIVE, DEFAULT_CASE_SENSITIVE);
        options.putIfAbsent(MAP_TYPE, DEFAULT_MAP_TYPE);
        options.putIfAbsent(USE_COPY_ITERATOR, DEFAULT_USE_COPY_ITERATOR);
        options.putIfAbsent(ORDERING, UNORDERED);

        return options; // Return the validated and resolved options
    }
}