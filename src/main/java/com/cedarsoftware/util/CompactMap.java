package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
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
import java.util.TreeMap;

/**
 * A memory-efficient {@code Map} implementation that adapts its internal storage structure
 * to minimize memory usage while maintaining acceptable performance. {@code CompactMap}
 * uses only one instance variable ({@code val}) to store all its entries in different
 * forms depending on the current size.
 *
 * <h2>Motivation</h2>
 * Traditional {@code Map} implementations (like {@link java.util.HashMap}) allocate internal
 * structures upfront, even for empty or very small maps. {@code CompactMap} aims to reduce
 * memory overhead by starting in a minimalistic representation and evolving into more
 * complex internal structures only as the map grows.
 *
 * <h2>Internal States</h2>
 * As the map size changes, the internal {@code val} field transitions through distinct states:
 *
 * <table border="1" cellpadding="5" summary="Internal States">
 *   <tr>
 *     <th>State</th>
 *     <th>Condition</th>
 *     <th>Representation</th>
 *     <th>Size Range</th>
 *   </tr>
 *   <tr>
 *     <td>Empty</td>
 *     <td>{@code val == EMPTY_MAP}</td>
 *     <td>A sentinel empty value, indicating no entries are present.</td>
 *     <td>0</td>
 *   </tr>
 *   <tr>
 *     <td>Single Entry (Key = getSingleValueKey())</td>
 *     <td>{@code val} is a direct reference to the single value.</td>
 *     <td>When the inserted key matches {@link #getSingleValueKey()}, the map stores
 *         only the value directly (no {@code Map.Entry} overhead).</td>
 *     <td>1</td>
 *   </tr>
 *   <tr>
 *     <td>Single Entry (Key != getSingleValueKey())</td>
 *     <td>{@code val} is a {@link CompactMapEntry}</td>
 *     <td>For a single entry whose key does not match {@code getSingleValueKey()}, the map holds
 *         a single {@link java.util.Map.Entry} containing both key and value.</td>
 *     <td>1</td>
 *   </tr>
 *   <tr>
 *     <td>Compact Array</td>
 *     <td>{@code val} is an {@code Object[]}</td>
 *     <td>For maps with multiple entries (from 2 up to {@code compactSize()}),
 *         keys and values are stored in a single {@code Object[]} array, with keys in even
 *         indices and corresponding values in odd indices. When sorting is requested (e.g.,
 *         {@code ORDERING = SORTED} or {@code REVERSE}), the keys are sorted according to
 *         the chosen comparator or the default logic.</td>
 *     <td>2 to {@code compactSize()}</td>
 *   </tr>
 *   <tr>
 *     <td>Backing Map</td>
 *     <td>{@code val} is a standard {@code Map}</td>
 *     <td>Once the map grows beyond {@code compactSize()}, it delegates storage to a standard
 *         {@code Map} implementation (e.g., {@link java.util.HashMap} by default).
 *         This ensures good performance for larger data sets.</td>
 *     <td>> {@code compactSize()}</td>
 *   </tr>
 * </table>
 *
 * <h2>Case Sensitivity and Sorting</h2>
 * {@code CompactMap} allows you to specify whether string key comparisons are case-sensitive or not,
 * controlled by the {@link #isCaseInsensitive()} method. By default, string key equality checks are
 * case-sensitive. If you configure the map to be case-insensitive (e.g., by passing an option to
 * {@code newMap(...)}), then:
 * <ul>
 *   <li>Key equality checks will ignore case for {@code String} keys.</li>
 *   <li>If sorting is requested (when in the {@code Object[]} compact state and no custom comparator
 *       is provided), string keys will be sorted using a case-insensitive order. Non-string keys
 *       will use natural ordering if possible.</li>
 * </ul>
 *
 * If a custom comparator is provided, that comparator takes precedence over case-insensitivity settings.
 *
 * <h2>Behavior and Configuration</h2>
 * {@code CompactMap} allows customization of:
 * <ul>
 *   <li>The compact size threshold (override {@link #compactSize()}).</li>
 *   <li>Case sensitivity for string keys (override {@link #isCaseInsensitive()} or specify via factory options).</li>
 *   <li>The special single-value key optimization (override {@link #getSingleValueKey()}).</li>
 *   <li>The backing map type, comparator, and ordering via provided factory methods.</li>
 * </ul>
 *
 * While subclassing {@code CompactMap} is possible, it is generally not necessary. Use the static
 * factory methods and configuration options to change behavior. This design ensures the core
 * {@code CompactMap} remains minimal with only one member variable.
 *
 * <h2>Factory Methods and Configuration Options</h2>
 * Instead of subclassing, you can configure a {@code CompactMap} through the static factory methods
 * like {@link #newMap(Map)}, which accept a configuration options map. For example, to enable
 * case-insensitivity:
 *
 * <pre>{@code
 * Map<String, Object> options = new HashMap<>();
 * options.put(CompactMap.CASE_SENSITIVE, false); // case-insensitive
 * CompactMap<String, Integer> caseInsensitiveMap = CompactMap.newMap(options);
 * }</pre>
 *
 * If you then request sorted or reverse ordering without providing a custom comparator, string keys
 * will be sorted case-insensitively.
 *
 * <h3>Additional Examples</h3>
 * <pre>{@code
 * // Default CompactMap:
 * CompactMap<String, Integer> defaultMap = CompactMap.newMap();
 *
 * // Case-insensitive and sorted using natural case-insensitive order:
 * Map<String, Object> sortedOptions = new HashMap<>();
 * sortedOptions.put(CompactMap.ORDERING, CompactMap.SORTED);
 * sortedOptions.put(CompactMap.CASE_SENSITIVE, false);
 * sortedOptions.put(CompactMap.MAP_TYPE, TreeMap.class);
 * CompactMap<String, Integer> ciSortedMap = CompactMap.newMap(sortedOptions);
 *
 * // Use a custom comparator to override case-insensitive checks:
 * sortedOptions.put(CompactMap.COMPARATOR, String.CASE_INSENSITIVE_ORDER);
 * // Now sorting uses the provided comparator.
 * CompactMap<String, Integer> customSortedMap = CompactMap.newMap(sortedOptions);
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * Thread safety depends on the chosen backing map implementation. If you require thread safety,
 * consider using a concurrent map type or external synchronization.
 *
 * <h2>Conclusion</h2>
 * {@code CompactMap} is a flexible, memory-efficient map suitable for scenarios where map sizes vary.
 * Its flexible configuration and factory methods allow you to tailor its behavior—such as case sensitivity
 * and ordering—without subclassing.
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
    public static final String ORDERING = "ordering";
    public static final String COMPARATOR = "comparator";

    // Constants for ordering options
    public static final String UNORDERED = "unordered";
    public static final String SORTED = "sorted";
    public static final String INSERTION = "insertion";
    public static final String REVERSE = "reverse";

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
     * @return {@code true} if this map contains no key-value mappings; {@code false} otherwise
     */
    public boolean isEmpty() {
        return val == EMPTY_MAP;
    }

    /**
     * Determines whether two keys are equal, considering case sensitivity for String keys.
     *
     * @param key  the first key to compare
     * @param aKey the second key to compare
     * @return {@code true} if the keys are equal based on the comparison rules; {@code false} otherwise
     */
    private boolean areKeysEqual(Object key, Object aKey) {
        if (key instanceof String && aKey instanceof String) {
            return isCaseInsensitive()
                    ? ((String) key).equalsIgnoreCase((String) aKey)
                    : key.equals(aKey);
        }
        return Objects.equals(key, aKey);
    }

    /**
     * Compares two keys for ordering based on the map's ordering and case sensitivity settings.
     *
     * <p>
     * The comparison follows these rules:
     * <ul>
     *   <li>If both keys are equal (as determined by {@link #areKeysEqual}), returns {@code 0}.</li>
     *   <li>If both keys are instances of {@link String}:
     *     <ul>
     *       <li>Uses a case-insensitive comparator if {@link #isCaseInsensitive()} is {@code true}; otherwise, uses case-sensitive comparison.</li>
     *       <li>Reverses the comparator if the map's ordering is set to {@code REVERSE}.</li>
     *       <li>If one keys is String and other is not, compares class names lexicographically to establish a consistent order (honoring {@code REVERSE} if needed).</li>
     *     </ul>
     *   </li>
     *   <li>If both keys implement {@link Comparable} and are of the exact same class:
     *     <ul>
     *       <li>Compares them using their natural ordering.</li>
     *       <li>Reverses the result if the map's ordering is set to {@code REVERSE}.</li>
     *     </ul>
     *   </li>
     *   <li>If keys are of different classes or do not implement {@link Comparable}:
     *     <ul>
     *       <li>Handles {@code null} values: {@code null} is considered less than any non-null key.</li>
     *       <li>Compares class names lexicographically to establish a consistent order (honoring {@code REVERSE} if needed)</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     *
     * <p><b>Note:</b> This method ensures a durable and consistent ordering, even for keys of differing types or non-comparable keys, by falling back to class name comparison.</p>
     *
     * @param key1 the first key to compare
     * @param key2 the second key to compare
     * @return a negative integer, zero, or a positive integer as {@code key1} is less than, equal to,
     *         or greater than {@code key2}
     */
    private int compareKeysForOrder(Object key1, Object key2) {
        // Handle nulls explicitly
        if (key1 == null && key2 == null) {
            return 0;
        }
        if (key1 == null) {
            return 1;  // Nulls last when sorting
        }
        if (key2 == null) {
            return -1; // Nulls last when sorting
        }

        // Early exit if keys are equal based on case sensitivity
        if (areKeysEqual(key1, key2)) {
            return 0;
        }

        // Get custom comparator - only call getComparator() once
        Comparator<? super K> customComparator = getComparator();
        if (customComparator != null) {
            return customComparator.compare((K)key1, (K)key2);
        }

        // String comparison - most common case
        if (key1 instanceof String) {
            if (key2 instanceof String) {
                // Both are strings - handle case sensitivity
                return isCaseInsensitive()
                        ? String.CASE_INSENSITIVE_ORDER.compare((String)key1, (String)key2)
                        : ((String)key1).compareTo((String)key2);
            }
            // key1 is String, key2 is not - use class name comparison
            int cmp = key1.getClass().getName().compareTo(key2.getClass().getName());
            return REVERSE.equals(getOrdering()) ? -cmp : cmp;
        }

        // Try Comparable if same type
        if (key1.getClass() == key2.getClass() && key1 instanceof Comparable) {
            Comparable<Object> comp1 = (Comparable<Object>) key1;
            return comp1.compareTo(key2);
        }

        // Fallback to class name comparison for different types
        int cmp = key1.getClass().getName().compareTo(key2.getClass().getName());
        return REVERSE.equals(getOrdering()) ? -cmp : cmp;
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
                if (areKeysEqual(key, entries[i])) {
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
        return areKeysEqual(key, getLogicalSingleKey());
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
            int len = entries.length;
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
        return Objects.equals(getLogicalSingleValue(), value);
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
            int len = entries.length;
            for (int i = 0; i < len; i += 2) {
                if (areKeysEqual(key, entries[i])) {
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
        if (areKeysEqual(key, getLogicalSingleKey())) {
            return getLogicalSingleValue();
        }
        return null;
    }

    /**
     * Associates the specified value with the specified key in this map.
     * If the map previously contained a mapping for the key, the old value is replaced.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with key, or {@code null} if there was no mapping for key.
     * @throws NullPointerException if the specified key is null and this map does not permit null keys
     * @throws ClassCastException   if the key is of an inappropriate type for this map
     */
    @Override
    public V put(K key, V value) {
        if (val == EMPTY_MAP) {   // Empty map
            if (areKeysEqual(key, getSingleValueKey()) && !(value instanceof Map || value instanceof Object[])) {
                // Store the value directly for optimized single-entry storage
                // (can't allow Map or Object[] because that would throw off the 'state')
                val = value;
            } else {
                // Create a CompactMapEntry for the first entry
                val = new CompactMapEntry(key, value);
            }
            return null;
        } else if (val instanceof Object[]) {   // Compact array storage (2 to compactSize)
            Object[] entries = (Object[]) val;
            return putInCompactArray(entries, key, value);
        } else if (val instanceof Map) {   // Backing map storage (&gt; compactSize)
            Map<K, V> map = (Map<K, V>) val;
            return map.put(key, value);
        }

        // Single entry state, handle overwrite, or insertion which transitions the Map to Object[4]
        return handleSingleEntryPut(key, value);
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     */
    @Override
    public V remove(Object key) {
        if (val instanceof Object[]) {   // 2 to compactSize
            return removeFromCompactArray(key);
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
            if (areKeysEqual(key, aKey)) {
                int i1 = i + 1;
                V oldValue = (V) entries[i1];
                entries[i1] = value;
                return oldValue;
            }
        }

        if (size() < compactSize()) {
            Object[] expand = new Object[len + 2];
            System.arraycopy(entries, 0, expand, 0, len);
            expand[len] = key;
            expand[len + 1] = value;
            sortCompactArray(expand);  // Make sure sorting happens
            val = expand;
        } else {
            switchToMap(entries, key, value);
        }
        return null;
    }

    /**
     * Removes a key-value pair from the compact array while preserving order.
     */
    private V removeFromCompactArray(Object key) {
        Object[] entries = (Object[]) val;
        if (size() == 2) {   // Transition back to single entry
            return handleTransitionToSingleEntry(entries, key);
        }

        int len = entries.length;
        for (int i = 0; i < len; i += 2) {
            if (areKeysEqual(key, entries[i])) {
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

    /**
     * Sorts the internal array maintaining key-value pairs in the correct relative positions.
     * This method is optimized for CompactMap's specific use case where the array is always
     * sorted except for the last key-value pair added.
     *
     * <p>The implementation uses a modified insertion sort to place the newly added pair into
     * its correct position. This approach was chosen because:
     * <ul>
     *   <li>The array is already sorted except for the last pair added</li>
     *   <li>Only needs to find the insertion point and shift pairs to make room</li>
     *   <li>Performs O(1) comparisons in best case (new pair belongs at end)</li>
     *   <li>Performs O(n) comparisons in worst case (new pair belongs at start)</li>
     *   <li>Makes minimal memory allocations (just temporary storage for inserted pair)</li>
     * </ul>
     * </p>
     *
     * <p>The method maintains the key-value pair relationship by always moving pairs of array
     * elements together (keys at even indices, values at odd indices). No sorting is performed
     * for unordered or insertion-ordered maps.</p>
     *
     * @param array The array containing key-value pairs to sort
     */
    private void sortCompactArray(Object[] array) {
        String ordering = getOrdering();

        if (ordering.equals(UNORDERED) || ordering.equals(INSERTION)) {
            return;
        }

        int pairCount = array.length / 2;
        if (pairCount <= 1) {
            return;
        }

        // Get last key-value pair position
        int insertPairIndex = pairCount - 1;
        K keyToInsert = (K) array[insertPairIndex * 2];
        Object valueToInsert = array[insertPairIndex * 2 + 1];

        // Find insertion point and shift
        int j = insertPairIndex - 1;
        while (j >= 0 && compareKeysForOrder(array[j * 2], keyToInsert) > 0) {
            // Shift pair right
            int j2 = j * 2;                  // cache re-used math
            int j1_2 = (j + 1) * 2;          // cache re-used math
            array[j1_2] = array[j2];         // Shift key
            array[j1_2 + 1] = array[j2 + 1]; // Shift value
            j--;
        }

        // Insert pair at correct position
        array[(j + 1) * 2] = keyToInsert;
        array[(j + 1) * 2 + 1] = valueToInsert;
    }

    private void switchToMap(Object[] entries, K key, V value) {
        // Get the correct map type with initial capacity
        Map<K, V> map = getNewMap();  // This respects subclass overrides

        // Copy existing entries preserving order
        int len = entries.length;
        for (int i = 0; i < len; i += 2) {
            map.put((K) entries[i], (V) entries[i + 1]);
        }
        map.put(key, value);
        val = map;
    }
    
    /**
     * Handles the case where the array is reduced to a single entry during removal.
     */
    private V handleTransitionToSingleEntry(Object[] entries, Object key) {
        if (areKeysEqual(key, entries[0])) {
            Object prevValue = entries[1];
            clear();
            put((K) entries[2], (V) entries[3]);
            return (V) prevValue;
        } else if (areKeysEqual(key, entries[2])) {
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
        if (areKeysEqual(key, getLogicalSingleKey())) {   // Overwrite
            V save = getLogicalSingleValue();
            if (areKeysEqual(key, getSingleValueKey()) && !(value instanceof Map || value instanceof Object[])) {
                val = value;
            } else {
                val = new CompactMapEntry(key, value);
            }
            return save;
        } else {   // CompactMapEntry to []
            Object[] entries = new Object[4];
            K existingKey = getLogicalSingleKey();
            V existingValue = getLogicalSingleValue();

            // Determine order based on comparison
            if (SORTED.equals(getOrdering()) || REVERSE.equals(getOrdering())) {
                int comparison = compareKeysForOrder(existingKey, key);
                if (comparison <= 0) {
                    entries[0] = existingKey;
                    entries[1] = existingValue;
                    entries[2] = key;
                    entries[3] = value;
                } else {
                    entries[0] = key;
                    entries[1] = value;
                    entries[2] = existingKey;
                    entries[3] = existingValue;
                }
            } else {
                // For INSERTION or UNORDERED, maintain insertion order
                entries[0] = existingKey;
                entries[1] = existingValue;
                entries[2] = key;
                entries[3] = value;
            }

            val = entries;
            return null;
        }
    }
    
    /**
     * Handles a remove operation when the map has a single entry.
     */
    private V handleSingleEntryRemove(Object key) {
        if (areKeysEqual(key, getLogicalSingleKey())) {   // Found
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
                Map<K, V> other = getNewMap(c.size());
                
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
            return areKeysEqual(getKey(), e.getKey()) && Objects.equals(getValue(), e.getValue());
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
     * @return new empty Map instance to use when {@code size() > compactSize()}.
     */
    protected Map<K, V> getNewMap() {
        return new HashMap<>(compactSize() + 1);
    }

    protected Map<K, V> getNewMap(int size) {
        Map<K, V> map = getNewMap();
        try {
            Constructor<?> constructor = ReflectionUtils.getConstructor(map.getClass(), Integer.TYPE);
            return (Map<K, V>) constructor.newInstance(size);
        } catch (Exception ignored) {
            return map;
        }
    }

    /**
     * Returns the initial capacity to use when creating a new backing map.
     * This defaults to 16 unless overridden.
     *
     * @return the initial capacity for the backing map
     */
    protected int capacity() {
        return DEFAULT_CAPACITY;
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
        return orderingComparator; // Return the provided comparator
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
     * Creates a new {@code CompactMap} with advanced configuration options.
     * <p>
     * This method provides fine-grained control over various aspects of the resulting {@code CompactMap},
     * including size thresholds, ordering strategies, case sensitivity, comparator usage, backing map type,
     * and source initialization. All options are validated and finalized via {@link #validateAndFinalizeOptions(Map)}
     * before the map is constructed.
     * </p>
     *
     * <h3>Available Options</h3>
     * <table border="1" cellpadding="5" summary="Configuration Options">
     *   <tr>
     *     <th>Key</th>
     *     <th>Type</th>
     *     <th>Description</th>
     *     <th>Default Value</th>
     *   </tr>
     *   <tr>
     *     <td>{@link #COMPACT_SIZE}</td>
     *     <td>Integer</td>
     *     <td>Specifies the threshold at which the map transitions from compact array-based storage
     *         to a standard {@link Map} implementation. A value of N means that once the map size
     *         exceeds N, it uses a backing map. Conversely, if the map size shrinks to N or below,
     *         it transitions back to compact storage.</td>
     *     <td>{@code 80}</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #CAPACITY}</td>
     *     <td>Integer</td>
     *     <td>Defines the initial capacity of the backing map when size exceeds {@code compactSize()}.
     *         Adjusted automatically if a {@link #SOURCE_MAP} is provided.</td>
     *     <td>{@code 16}</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #CASE_SENSITIVE}</td>
     *     <td>Boolean</td>
     *     <td>Determines whether {@code String} keys are compared in a case-sensitive manner.
     *         If {@code false}, string keys are treated case-insensitively for equality checks and,
     *         if sorting is enabled (and no custom comparator is provided), they are sorted
     *         case-insensitively in the {@code Object[]} compact state.</td>
     *     <td>{@code true}</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #MAP_TYPE}</td>
     *     <td>Class&lt;? extends Map&gt;</td>
     *     <td>The type of map to use once the size exceeds {@code compactSize()}. For example,
     *         {@link java.util.HashMap}, {@link java.util.LinkedHashMap}, or a {@link java.util.SortedMap}
     *         implementation like {@link java.util.TreeMap}. Certain orderings require specific map types
     *         (e.g., {@code SORTED} requires a {@code SortedMap}).</td>
     *     <td>{@code HashMap.class}</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #USE_COPY_ITERATOR}</td>
     *     <td>Boolean</td>
     *     <td>If {@code true}, iterators returned by this map operate on a copy of its entries,
     *         allowing safe iteration during modifications. Otherwise, iteration may throw
     *         {@link java.util.ConcurrentModificationException} if the map is modified during iteration.</td>
     *     <td>{@code false}</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #SINGLE_KEY}</td>
     *     <td>K</td>
     *     <td>Specifies a special key that, if present as the sole entry in the map, allows the map
     *         to store just the value without a {@code Map.Entry}, saving memory for single-entry maps.</td>
     *     <td>{@code "key"}</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #SOURCE_MAP}</td>
     *     <td>Map&lt;K,V&gt;</td>
     *     <td>If provided, the new map is initialized with all entries from this source. The capacity
     *         may be adjusted accordingly for efficiency.</td>
     *     <td>{@code null}</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #ORDERING}</td>
     *     <td>String</td>
     *     <td>Determines the ordering of entries. Valid values:
     *         <ul>
     *           <li>{@link #UNORDERED}</li>
     *           <li>{@link #SORTED}</li>
     *           <li>{@link #REVERSE}</li>
     *           <li>{@link #INSERTION}</li>
     *         </ul>
     *         If {@code SORTED} or {@code REVERSE} is chosen and no custom comparator is provided,
     *         sorting relies on either natural ordering or case-insensitive ordering for strings if
     *         {@code CASE_SENSITIVE=false}.</td>
     *     <td>{@code UNORDERED}</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #COMPARATOR}</td>
     *     <td>Comparator&lt;? super K&gt;</td>
     *     <td>A custom comparator to determine key order when {@code ORDERING = SORTED} or
     *         {@code ORDERING = REVERSE}. If {@code CASE_SENSITIVE=false} and no comparator is provided,
     *         string keys are sorted case-insensitively by default. If a comparator is provided,
     *         it overrides any case-insensitive logic.</td>
     *     <td>{@code null}</td>
     *   </tr>
     * </table>
     *
     * <h3>Behavior and Validation</h3>
     * <ul>
     *   <li>{@link #validateAndFinalizeOptions(Map)} is called first to verify and adjust the options.</li>
     *   <li>If {@code CASE_SENSITIVE} is {@code false} and no comparator is provided, string keys are
     *       handled case-insensitively during equality checks and sorting in the compact array state.</li>
     *   <li>If constraints are violated (e.g., {@code SORTED} ordering with a non-{@code SortedMap} type),
     *       an {@link IllegalArgumentException} is thrown.</li>
     *   <li>Providing a {@code SOURCE_MAP} initializes this map with its entries.</li>
     * </ul>
     *
     * @param <K> the type of keys maintained by the resulting map
     * @param <V> the type of values associated with the keys
     * @param options a map of configuration options (see table above)
     * @return a new {@code CompactMap} instance configured according to the provided options
     * @throws IllegalArgumentException if the provided options are invalid or incompatible
     * @see #validateAndFinalizeOptions(Map)
     */
    public static <K, V> CompactMap<K, V> newMap(Map<String, Object> options) {
        validateAndFinalizeOptions(options);

        int compactSize = (int) options.getOrDefault(COMPACT_SIZE, DEFAULT_COMPACT_SIZE);
        boolean caseSensitive = (boolean) options.getOrDefault(CASE_SENSITIVE, DEFAULT_CASE_SENSITIVE);
        boolean useCopyIterator = (boolean) options.getOrDefault(USE_COPY_ITERATOR, DEFAULT_USE_COPY_ITERATOR);
        Class<? extends Map<K, V>> mapType = (Class<? extends Map<K, V>>) options.getOrDefault(MAP_TYPE, DEFAULT_MAP_TYPE);
        Comparator<? super K> comparator = (Comparator<? super K>) options.get(COMPARATOR);
        K singleKey = (K) options.get(SINGLE_KEY);
        Map<K, V> source = (Map<K, V>) options.get(SOURCE_MAP);
        String ordering = (String) options.getOrDefault(ORDERING, UNORDERED);
        int capacity = (int) options.getOrDefault(CAPACITY, DEFAULT_CAPACITY);

        CompactMap<K, V> map = new CompactMap<K, V>() {
            @Override
            protected Map<K, V> getNewMap() {
                try {
                    if (!caseSensitive) {
                        // For case-insensitive maps, create the appropriate inner map first
                        Class<? extends Map<K, V>> innerMapType =
                                (Class<? extends Map<K, V>>) options.get("INNER_MAP_TYPE");
                        Map<K, V> innerMap;

                        if (comparator != null && SortedMap.class.isAssignableFrom(innerMapType)) {
                            innerMap = innerMapType.getConstructor(Comparator.class).newInstance(comparator);
                        } else {
                            Constructor<? extends Map<K, V>> constructor =
                                    innerMapType.getConstructor(int.class);
                            innerMap = constructor.newInstance(capacity);
                        }
                        // Wrap in CaseInsensitiveMap
                        return new CaseInsensitiveMap<>(Collections.emptyMap(), innerMap);
                    } else {
                        // Case-sensitive map creation
                        if (comparator != null && SortedMap.class.isAssignableFrom(mapType)) {
                            return mapType.getConstructor(Comparator.class).newInstance(comparator);
                        }
                        Constructor<? extends Map<K, V>> constructor = mapType.getConstructor(int.class);
                        return constructor.newInstance(capacity);
                    }
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
            protected int capacity() {
                return capacity;
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

            @Override
            protected Comparator<? super K> getComparator() {
                return comparator;
            }
        };

        // Populate the map with entries from the source map, if provided
        if (source != null) {
            map.putAll(source);
        }

        return map;
    }
    
    /**
     * Creates a new CompactMap with base configuration:
     * - compactSize = 80
     * - caseSensitive = true
     * - capacity = 16
     * - ordering = UNORDERED
     * - useCopyIterator = false
     * - singleKey = "key"
     * - sourceMap = null
     * - mapType = HashMap.class
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @return a new CompactMap instance
     */
    public static <K, V> CompactMap<K, V> newMap() {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, DEFAULT_COMPACT_SIZE);
        options.put(CASE_SENSITIVE, DEFAULT_CASE_SENSITIVE);
        options.put(CAPACITY, DEFAULT_CAPACITY);
        options.put(ORDERING, UNORDERED);
        options.put(USE_COPY_ITERATOR, DEFAULT_USE_COPY_ITERATOR);
        options.put(SINGLE_KEY, "key");
        options.put(SOURCE_MAP, null);
        options.put(MAP_TYPE, HashMap.class);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with specified compact size and default values for other options.
     */
    public static <K, V> CompactMap<K, V> newMap(int compactSize) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with specified compact size and case sensitivity.
     */
    public static <K, V> CompactMap<K, V> newMap(int compactSize, boolean caseSensitive) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CASE_SENSITIVE, caseSensitive);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with specified compact size, case sensitivity, and capacity.
     */
    public static <K, V> CompactMap<K, V> newMap(int compactSize, boolean caseSensitive, int capacity) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CASE_SENSITIVE, caseSensitive);
        options.put(CAPACITY, capacity);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with specified compact size, case sensitivity, capacity, and ordering.
     */
    public static <K, V> CompactMap<K, V> newMap(int compactSize, boolean caseSensitive, int capacity,
                                                 String ordering) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CASE_SENSITIVE, caseSensitive);
        options.put(CAPACITY, capacity);
        options.put(ORDERING, ordering);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with specified compact size, case sensitivity, capacity,
     * ordering, and copy iterator setting.
     */
    public static <K, V> CompactMap<K, V> newMap(int compactSize, boolean caseSensitive, int capacity,
                                                 String ordering, boolean useCopyIterator) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CASE_SENSITIVE, caseSensitive);
        options.put(CAPACITY, capacity);
        options.put(ORDERING, ordering);
        options.put(USE_COPY_ITERATOR, useCopyIterator);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with specified compact size, case sensitivity, capacity,
     * ordering, copy iterator setting, and single key value.
     */
    public static <K, V> CompactMap<K, V> newMap(int compactSize, boolean caseSensitive, int capacity,
                                                 String ordering, boolean useCopyIterator, String singleKey) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CASE_SENSITIVE, caseSensitive);
        options.put(CAPACITY, capacity);
        options.put(ORDERING, ordering);
        options.put(USE_COPY_ITERATOR, useCopyIterator);
        options.put(SINGLE_KEY, singleKey);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with all configuration options and a source map.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @param compactSize the compact size threshold
     * @param caseSensitive whether the map is case-sensitive
     * @param capacity the initial capacity of the map
     * @param ordering the ordering strategy (UNORDERED, SORTED, REVERSE, or INSERTION)
     * @param useCopyIterator whether to use copy iterator
     * @param singleKey the key to use for single-entry optimization
     * @param sourceMap the source map to initialize entries from
     * @return a new CompactMap instance
     */
    public static <K, V> CompactMap<K, V> newMap(int compactSize, boolean caseSensitive, int capacity,
                                                 String ordering, boolean useCopyIterator, String singleKey,
                                                 Map<K, V> sourceMap) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CASE_SENSITIVE, caseSensitive);
        options.put(CAPACITY, capacity);
        options.put(ORDERING, ordering);
        options.put(USE_COPY_ITERATOR, useCopyIterator);
        options.put(SINGLE_KEY, singleKey);
        options.put(SOURCE_MAP, sourceMap);
        options.put(MAP_TYPE, HashMap.class);
        return newMap(options);
    }

    /**
     * Creates a new CompactMap with full configuration options.
     *
     * @param <K> the type of keys maintained by this map
     * @param <V> the type of mapped values
     * @param compactSize the compact size threshold
     * @param caseSensitive whether the map is case-sensitive
     * @param capacity the initial capacity of the map
     * @param ordering the ordering strategy (UNORDERED, SORTED, REVERSE, or INSERTION)
     * @param useCopyIterator whether to use copy iterator
     * @param singleKey the key to use for single-entry optimization
     * @param sourceMap the source map to initialize entries from
     * @param mapType the type of map to use for backing storage
     * @return a new CompactMap instance
     */
    public static <K, V> CompactMap<K, V> newMap(int compactSize, boolean caseSensitive, int capacity,
                                                 String ordering, boolean useCopyIterator, String singleKey,
                                                 Map<K, V> sourceMap, Class<? extends Map> mapType) {
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, compactSize);
        options.put(CASE_SENSITIVE, caseSensitive);
        options.put(CAPACITY, capacity);
        options.put(ORDERING, ordering);
        options.put(USE_COPY_ITERATOR, useCopyIterator);
        options.put(SINGLE_KEY, singleKey);
        options.put(SOURCE_MAP, sourceMap);
        options.put(MAP_TYPE, mapType);
        return newMap(options);
    }
    
    /**
     * Validates the provided configuration options and resolves conflicts.
     * Throws an {@link IllegalArgumentException} if the configuration is invalid.
     *
     * @param options a map of user-provided options
     */
    private static void validateAndFinalizeOptions(Map<String, Object> options) {
        // First check raw map type before any defaults are applied
        Class<? extends Map> rawMapType = (Class<? extends Map>) options.get(MAP_TYPE);
        String ordering = (String) options.getOrDefault(ORDERING, UNORDERED);

        // Determine map type
        Class<? extends Map> mapType;
        if (rawMapType == null) {
            // No map type specified - choose based on ordering
            if (ordering.equals(INSERTION)) {
                mapType = LinkedHashMap.class;
            } else if (ordering.equals(SORTED) || ordering.equals(REVERSE)) {
                mapType = TreeMap.class;
            } else {
                mapType = DEFAULT_MAP_TYPE;
            }
            options.put(MAP_TYPE, mapType);
        } else {
            mapType = rawMapType;
            // Validate user's explicit map type choice against ordering
            if (ordering.equals(INSERTION) && !LinkedHashMap.class.isAssignableFrom(mapType)) {
                throw new IllegalArgumentException("Ordering 'insertion' requires a LinkedHashMap type.");
            }
            if ((ordering.equals(SORTED) || ordering.equals(REVERSE)) && !SortedMap.class.isAssignableFrom(mapType)) {
                throw new IllegalArgumentException("Ordering 'sorted' or 'reverse' requires a SortedMap type.");
            }
        }

        // Get remaining options
        Comparator<?> comparator = (Comparator<?>) options.get(COMPARATOR);
        boolean caseSensitive = (boolean) options.getOrDefault(CASE_SENSITIVE, DEFAULT_CASE_SENSITIVE);

        // If case-insensitive, store inner map type and set outer type to CaseInsensitiveMap
        if (!caseSensitive) {
            // Don't wrap CaseInsensitiveMap in another CaseInsensitiveMap
            if (mapType != CaseInsensitiveMap.class) {
                options.put("INNER_MAP_TYPE", mapType);
                options.put(MAP_TYPE, CaseInsensitiveMap.class);
            }
        }

        // Add this code here to wrap the comparator if one exists
        if (comparator != null) {
            Comparator<?> originalComparator = comparator;
            comparator = (a, b) -> {
                Object key1 = (a instanceof CaseInsensitiveMap.CaseInsensitiveString) ?
                        a.toString() : a;
                Object key2 = (b instanceof CaseInsensitiveMap.CaseInsensitiveString) ?
                        b.toString() : b;
                return ((Comparator<Object>)originalComparator).compare(key1, key2);
            };
            options.put(COMPARATOR, comparator);
        }
        
        // Validate ordering and mapType compatibility for the actual backing map
        Class<?> effectiveMapType = !caseSensitive ? (Class<?>) options.get("INNER_MAP_TYPE") : mapType;
        
        // Handle case sensitivity for sorted maps when no comparator is provided
        if ((ordering.equals(SORTED) || ordering.equals(REVERSE)) &&
                !caseSensitive &&
                comparator == null) {
            // Create a wrapped case-insensitive comparator that handles CaseInsensitiveString
            comparator = (o1, o2) -> {
                String s1 = (o1 instanceof CaseInsensitiveMap.CaseInsensitiveString) ?
                        o1.toString() : (String)o1;
                String s2 = (o2 instanceof CaseInsensitiveMap.CaseInsensitiveString) ?
                        o2.toString() : (String)o2;
                return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
            };
            options.put(COMPARATOR, comparator);
        }

        // Handle reverse ordering with or without comparator
        if (ordering.equals(REVERSE)) {
            if (comparator == null && !caseSensitive) {
                // For case-insensitive reverse ordering
                comparator = (o1, o2) -> {
                    String s1 = (o1 instanceof CaseInsensitiveMap.CaseInsensitiveString) ?
                            o1.toString() : (String)o1;
                    String s2 = (o2 instanceof CaseInsensitiveMap.CaseInsensitiveString) ?
                            o2.toString() : (String)o2;
                    return String.CASE_INSENSITIVE_ORDER.compare(s2, s1);
                };
            } else if (comparator == null) {
                // For case-sensitive reverse ordering
                comparator = (o1, o2) -> {
                    String s1 = (o1 instanceof CaseInsensitiveMap.CaseInsensitiveString) ?
                            o1.toString() : (String)o1;
                    String s2 = (o2 instanceof CaseInsensitiveMap.CaseInsensitiveString) ?
                            o2.toString() : (String)o2;
                    return s2.compareTo(s1);
                };
            } else {
                // Reverse an existing comparator
                Comparator<Object> existing = (Comparator<Object>)comparator;
                comparator = (o1, o2) -> {
                    Object k1 = (o1 instanceof CaseInsensitiveMap.CaseInsensitiveString) ?
                            o1.toString() : o1;
                    Object k2 = (o2 instanceof CaseInsensitiveMap.CaseInsensitiveString) ?
                            o2.toString() : o2;
                    return existing.compare(k2, k1);
                };
            }
            options.put(COMPARATOR, comparator);
        }

        // Ensure the comparator is compatible with the map type
        if (comparator != null && !SortedMap.class.isAssignableFrom(effectiveMapType)) {
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
    }
}