package com.cedarsoftware.util;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * A memory-efficient {@code Map} implementation that adapts its internal storage structure
 * to minimize memory usage while maintaining excellent performance.
 *
 * <h2>Creating a CompactMap</h2>
 * There are two primary ways to create a CompactMap:
 *
 * <h3>1. Using the Builder Pattern (Recommended)</h3>
 * <pre>{@code
 * // Create a case-insensitive, sorted CompactMap
 * CompactMap<String, Object> map = CompactMap.<String, Object>builder()
 *     .caseSensitive(false)
 *     .sortedOrder()
 *     .compactSize(80)
 *     .build();
 *
 * // Create a CompactMap with insertion ordering
 * CompactMap<String, Object> ordered = CompactMap.<String, Object>builder()
 *     .insertionOrder()
 *     .mapType(LinkedHashMap.class)
 *     .build();
 * }</pre>
 *
 * <h3>Type Inference and Builder Usage</h3>
 * Note the type witness ({@code <String, Object>}) in the example above.  When using the builder pattern
 * with method chaining, you may need to provide a type witness to help Java's type inference:
 *
 * <pre>{@code
 * // Alternative approach without type witness
 * Builder<String, Object> builder = CompactMap.builder();
 * CompactMap<String, Object> map2 = builder
 *     .caseSensitive(false)
 *     .sortedOrder()
 *     .build();
 * }</pre>
 *
 * The type witness ({@code <String, Object>}) is required due to Java's type inference
 * limitations when method chaining directly from the builder() method. If you find the
 * type witness syntax cumbersome, you can split the builder creation and configuration
 * into separate statements as shown in the second example above.
 *
 * <h3>2. Using Constructor</h3>
 * <pre>{@code
 * // Creates a default CompactMap that scales based on size
 * CompactMap<String, Object> map = new CompactMap<>();
 *
 * // Creates a CompactMap initialized with entries from another map
 * CompactMap<String, Object> copy = new CompactMap<>(existingMap);
 * }</pre>
 *
 * In the examples above, the behavior of the CompactMap will be that of a HashMap,
 * while using the minimal amount of memory possible to hold the contents. The CompactMap
 * has only one instance variable.
 *
 * <h2>Configuration Options</h2>
 * When using the Builder pattern, the following options are available:
 * <table border="1" cellpadding="5" summary="Builder Options">
 *   <tr><th>Method</th><th>Description</th><th>Default</th></tr>
 *   <tr>
 *     <td>{@code caseSensitive(boolean)}</td>
 *     <td>Controls case sensitivity for string keys</td>
 *     <td>true</td>
 *   </tr>
 *   <tr>
 *     <td>{@code compactSize(int)}</td>
 *     <td>Maximum size before switching to backing map</td>
 *     <td>50</td>
 *   </tr>
 *   <tr>
 *     <td>{@code mapType(Class)}</td>
 *     <td>Type of backing map when size exceeds compact size (must originate
 *         from {@code java.util.*}, {@code java.util.concurrent.*}, or
 *         {@code com.cedarsoftware.util.*})</td>
 *     <td>HashMap.class</td>
 *   </tr>
 *   <tr>
 *     <td>{@code singleValueKey(K)}</td>
 *     <td>Special key that enables optimized storage when map contains only one entry with this key</td>
 *     <td>"id"</td>
 *   </tr>
 *   <tr>
 *     <td>{@code sourceMap(Map)}</td>
 *     <td>Initializes the CompactMap with entries from the provided map</td>
 *     <td>null</td>
 *   </tr>
 *   <tr>
 *     <td>{@code sortedOrder()}</td>
 *     <td>Maintains keys in sorted order</td>
 *     <td>unordered</td>
 *   </tr>
 *   <tr>
 *     <td>{@code reverseOrder()}</td>
 *     <td>Maintains keys in reverse order</td>
 *     <td>unordered</td>
 *   </tr>
 *   <tr>
 *     <td>{@code insertionOrder()}</td>
 *     <td>Maintains keys in insertion order</td>
 *     <td>unordered</td>
 *   </tr>
 * </table>
 *
 * <h3>Example with Additional Properties</h3>
 * <pre>{@code
 * CompactMap<String, Object> map = CompactMap.builder()
 *     .caseSensitive(false)
 *     .sortedOrder()
 *     .compactSize(80)
 *     .singleValueKey("uuid")    // Optimize storage for single entry with key "uuid"
 *     .sourceMap(existingMap)    // Initialize with existing entries
 *     .build();
 * }</pre>
 *
 * <h2>Internal Storage States</h2>
 * As elements are added to or removed from the map, it transitions through different internal states
 * to optimize memory usage:
 *
 * <table border="1" cellpadding="5" summary="Internal States">
 *   <tr>
 *     <th>State</th>
 *     <th>Condition</th>
 *     <th>Storage</th>
 *     <th>Size Range</th>
 *   </tr>
 *   <tr>
 *     <td>Empty</td>
 *     <td>{@code val == EMPTY_MAP}</td>
 *     <td>Sentinel value</td>
 *     <td>0</td>
 *   </tr>
 *   <tr>
 *     <td>Single Entry</td>
 *     <td>Direct value or Entry</td>
 *     <td>Optimized single value storage</td>
 *     <td>1</td>
 *   </tr>
 *   <tr>
 *     <td>Compact Array</td>
 *     <td>{@code val} is Object[]</td>
 *     <td>Array with alternating keys/values</td>
 *     <td>2 to compactSize</td>
 *   </tr>
 *   <tr>
 *     <td>Backing Map</td>
 *     <td>{@code val} is Map</td>
 *     <td>Standard Map implementation</td>
 *     <td>> compactSize</td>
 *   </tr>
 * </table>
 *
 * <h2>Implementation Note</h2>
 * <p>This class uses runtime optimization techniques to create specialized implementations
 * based on the configuration options. When a CompactMap is first created with a specific
 * combination of options (case sensitivity, ordering, map type, etc.), a custom class
 * is dynamically generated and cached to provide optimal performance for that configuration.
 * This is an implementation detail that is transparent to users of the class.</p>
 *
 * <p>The generated class names encode the configuration settings. For example:</p>
 * <ul>
 *   <li>{@code CompactMap$HashMap_CS_S50_id_Unord} - A case-sensitive, unordered map
 *       with HashMap backing, compact size of 50, and "id" as single value key</li>
 *   <li>{@code CompactMap$TreeMap_CI_S100_UUID_Sort} - A case-insensitive, sorted map
 *       with TreeMap backing, compact size of 100, and "UUID" as single value key</li>
 *   <li>{@code CompactMap$LinkedHashMap_CS_S50_Key_Ins} - A case-sensitive map with
 *       insertion ordering, LinkedHashMap backing, compact size of 50, and "Key" as
 *       single value key</li>
 * </ul>
 *
 * <p>For developers interested in the internal mechanics, the source code contains
 * detailed documentation of the template generation and compilation process.</p>
 * <p>Note: As elements are removed, the map will transition back through these states
 * in reverse order to maintain optimal memory usage.</p>
 *
 * <p>While subclassing CompactMap is still supported for backward compatibility,
 * it is recommended to use the Builder pattern for new implementations.</p>
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
    public static final String CASE_SENSITIVE = "caseSensitive";
    public static final String MAP_TYPE = "mapType";
    public static final String SINGLE_KEY = "singleKey";
    public static final String SOURCE_MAP = "source";
    public static final String ORDERING = "ordering";

    // Constants for ordering options
    public static final String UNORDERED = "unordered";
    public static final String SORTED = "sorted";
    public static final String INSERTION = "insertion";
    public static final String REVERSE = "reverse";

    // Default values
    /**
     * Default threshold for switching from the internal compact array
     * representation to the backing {@code Map}.  Empirical testing shows
     * a value of 50 offers good performance with strong memory savings.
     */
    public static final int DEFAULT_COMPACT_SIZE = 50;
    public static final boolean DEFAULT_CASE_SENSITIVE = true;
    public static final Class<? extends Map> DEFAULT_MAP_TYPE = HashMap.class;
    public static final String DEFAULT_SINGLE_KEY = "id";
    /**
     * Packages allowed when specifying a custom backing map type.
     */
    private static final Set<String> ALLOWED_MAP_PACKAGES = new HashSet<>(Arrays.asList(
            "java.util",
            "java.util.concurrent",
            "com.cedarsoftware.util"));
    private static final String INNER_MAP_TYPE = "innerMapType";
    private static final TemplateClassLoader templateClassLoader = new TemplateClassLoader(ClassUtilities.getClassLoader(CompactMap.class));
    private static final Map<String, ReentrantLock> CLASS_LOCKS = new ConcurrentHashMap<>();

    private static boolean isAllowedMapType(Class<?> mapType) {
        String name = mapType.getName();
        for (String prefix : ALLOWED_MAP_PACKAGES) {
            if (name.startsWith(prefix + ".") || name.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    // The only "state" and why this is a compactMap - one-member variable
    protected Object val = EMPTY_MAP;

    /**
     * Constructs an empty CompactMap with the default configuration.
     * <p>
     * This constructor ensures that the `compactSize()` method returns a value greater than or equal to 2.
     * </p>
     *
     * @throws IllegalStateException if {@link #compactSize()} returns a value less than 2
     */
    public CompactMap() {
        if (compactSize() < 2) {
            throw new IllegalArgumentException("compactSize() must be >= 2");
        }

        // Only check direct subclasses, not our generated classes
        if (getClass() != CompactMap.class && isLegacyConstructed()) {
            Map<K,V> map = getNewMap();
            if (map instanceof SortedMap) {
                SortedMap<?,?> sortedMap = (SortedMap<?,?>)map;
                Comparator<?> comparator = sortedMap.comparator();

                // Check case sensitivity consistency
                if (comparator == String.CASE_INSENSITIVE_ORDER && !isCaseInsensitive()) {
                    throw new IllegalStateException(
                            "Inconsistent configuration: Map uses case-insensitive comparison but isCaseInsensitive() returns false");
                }
            }
        }
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

    public boolean isDefaultCompactMap() {
        // 1. Check that compactSize() is the library default (50)
        if (compactSize() != DEFAULT_COMPACT_SIZE) {
            return false;
        }

        // 2. Check that the map is case-sensitive, meaning isCaseInsensitive() should be false.
        if (isCaseInsensitive()) {
            return false;
        }

        // 3. Check that the ordering is "unordered"
        if (!"unordered".equals(getOrdering())) {
            return false;
        }

        // 4. Check that the single key is "id"
        if (!DEFAULT_SINGLE_KEY.equals(getSingleValueKey())) {
            return false;
        }

        // 5. Check that the backing map is a HashMap.
        return HashMap.class.equals(getNewMap().getClass());
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
     * Determines if this CompactMap instance was created using legacy construction (direct subclassing)
     * rather than the template-based generation system.
     * <p>
     * Legacy construction refers to instances where CompactMap is directly subclassed by user code,
     * rather than using the builder pattern or template generation system. This method helps
     * differentiate between these two creation patterns to maintain backward compatibility.
     * </p>
     * <p>
     * The method works by checking if the class name starts with the template prefix
     * "com.cedarsoftware.util.CompactMap$". Template-generated classes will always have this
     * prefix, while legacy subclasses will not.
     * </p>
     *
     * @return {@code true} if this instance was created through legacy subclassing,
     *         {@code false} if it was created through the template generation system
     */
    private boolean isLegacyConstructed() {
        return !getClass().getName().startsWith("com.cedarsoftware.util.CompactMap$");
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
            String ordering = getOrdering();
            if (SORTED.equals(ordering) || REVERSE.equals(ordering)) {
                Comparator<Object> comp = new CompactMapComparator(isCaseInsensitive(), REVERSE.equals(ordering));
                return pairBinarySearch(entries, key, comp) >= 0;
            }
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
        if (val instanceof Object[]) {   // 2 to CompactSize
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
            String ordering = getOrdering();
            if (SORTED.equals(ordering) || REVERSE.equals(ordering)) {
                Comparator<Object> comp = new CompactMapComparator(isCaseInsensitive(), REVERSE.equals(ordering));
                int pairIdx = pairBinarySearch(entries, key, comp);
                return pairIdx >= 0 ? (V) entries[pairIdx * 2 + 1] : null;
            }
            int len = entries.length;
            for (int i = 0; i < len; i += 2) {
                if (areKeysEqual(key, entries[i])) {
                    return (V) entries[i + 1];
                }
            }
            return null;
        } else if (val instanceof Map) {   // > compactSize
            return ((Map<K, V>) val).get(key);
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
        if (val instanceof Object[]) {   // Compact array storage (2 to compactSize)
            return putInCompactArray((Object[]) val, key, value);
        } else if (val instanceof Map) {   // Backing map storage (&gt; compactSize)
            return ((Map<K, V>) val).put(key, value);
        } else if (val == EMPTY_MAP) {   // Empty map
            if (areKeysEqual(key, getSingleValueKey()) && !(value instanceof Map || value instanceof Object[])) {
                // Store the value directly for optimized single-entry storage
                // (can't allow Map or Object[] because that would throw off the 'state')
                val = value;
            } else {
                // Create a CompactMapEntry for the first entry
                val = new CompactMapEntry(key, value);
            }
            return null;
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

    /**
     * Performs a binary search on an array storing key-value pairs.
     * <p>
     * The array alternates keys and values where keys occupy the even
     * indices. This method searches only the keys using the supplied
     * comparator and returns the pair index using the same semantics as
     * {@link java.util.Arrays#binarySearch(Object[], Object, Comparator)}.
     * </p>
     *
     * @param arr   array containing alternating keys and values
     * @param key   the key to search for
     * @param comp  comparator used for key comparison
     * @return index of the key if found (pair index), otherwise
     *         {@code -(insertionPoint + 1)} where {@code insertionPoint}
     *         is the pair index at which the key should be inserted
     */
    private int pairBinarySearch(Object[] arr, Object key, Comparator<Object> comp) {
        int low = 0;
        int high = (arr.length / 2) - 1;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            Object midKey = arr[mid * 2];
            int cmp = comp.compare(key, midKey);
            if (cmp > 0) {
                low = mid + 1;
            } else if (cmp < 0) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -(low + 1);
    }

    /**
     * Adds or updates an entry in the compact array storage.
     * <p>
     * If the key exists, updates its value. If the key is new and there's room to stay as an array (< compactSize),
     * appends the new entry by growing the Object[]. If adding would exceed compactSize(), transitions to map storage.
     * </p>
     *
     * @param entries the current array storage containing alternating keys and values
     * @param key the key to add or update
     * @param value the value to associate with the key
     * @return the previous value associated with the key, or null if the key was not present
     */
    private V putInCompactArray(final Object[] entries, K key, V value) {
        final int len = entries.length;
        String ordering = getOrdering();
        boolean binary = SORTED.equals(ordering) || REVERSE.equals(ordering);
        Comparator<Object> comp = null;
        int pairIndex = -1;

        if (binary) {
            comp = new CompactMapComparator(isCaseInsensitive(), REVERSE.equals(ordering));
            pairIndex = pairBinarySearch(entries, key, comp);
            if (pairIndex >= 0) {
                int vIdx = pairIndex * 2 + 1;
                V oldValue = (V) entries[vIdx];
                entries[vIdx] = value;
                return oldValue;
            }
            pairIndex = -(pairIndex + 1);
        } else {
            for (int i = 0; i < len; i += 2) {
                if (areKeysEqual(key, entries[i])) {
                    int vIdx = i + 1;
                    V oldValue = (V) entries[vIdx];
                    entries[vIdx] = value;
                    return oldValue;
                }
            }
        }

        if (size() < compactSize()) {
            Object[] expand = new Object[len + 2];
            if (binary) {
                int insert = pairIndex * 2;
                System.arraycopy(entries, 0, expand, 0, insert);
                expand[insert] = key;
                expand[insert + 1] = value;
                System.arraycopy(entries, insert, expand, insert + 2, len - insert);
            } else {
                System.arraycopy(entries, 0, expand, 0, len);
                expand[len] = key;
                expand[len + 1] = value;
            }
            val = expand;
        } else {
            switchToMap(entries, key, value);
        }
        return null;
    }

    /**
     * Removes an entry from the compact array storage.
     * <p>
     * If size will become 1 after removal, transitions back to single entry storage.
     * Otherwise, creates a new smaller array excluding the removed entry.
     * </p>
     *
     * @param key the key whose entry should be removed
     * @return the value associated with the key, or null if the key was not found
     */
    private V removeFromCompactArray(Object key) {
        Object[] entries = (Object[]) val;
        int pairCount = size(); // Number of key-value pairs

        if (pairCount == 2) {   // Transition back to single entry
            return handleTransitionToSingleEntry(entries, key);
        }

        int len = entries.length;
        String ordering = getOrdering();
        boolean binary = SORTED.equals(ordering) || REVERSE.equals(ordering);
        int idx = -1;

        if (binary) {
            Comparator<Object> comp = new CompactMapComparator(isCaseInsensitive(), REVERSE.equals(ordering));
            int pairIdx = pairBinarySearch(entries, key, comp);
            if (pairIdx < 0) {
                return null;
            }
            idx = pairIdx * 2;
        } else {
            for (int i = 0; i < len; i += 2) {
                if (areKeysEqual(key, entries[i])) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                return null;
            }
        }

        V oldValue = (V) entries[idx + 1];
        Object[] shrink = new Object[len - 2];
        if (idx > 0) {
            System.arraycopy(entries, 0, shrink, 0, idx);
        }
        if (idx + 2 < len) {
            System.arraycopy(entries, idx + 2, shrink, idx, len - idx - 2);
        }
        val = shrink;
        return oldValue;
    }

    /**
     * Sorts the compact array while maintaining key-value pair relationships.
     * <p>
     * For legacy constructed maps, sorts only if backing map is a SortedMap.
     * For template maps, sorts based on the specified ordering (sorted/reverse).
     * Keys at even indices, values at odd indices are kept together during sort.
     * </p>
     *
     * @param array the array of alternating keys and values to sort
     */
    private void sortCompactArray(final Object[] array) {
        int pairCount = array.length / 2;
        if (pairCount <= 1) {
            return;
        }

        if (isLegacyConstructed()) {
            Map<K,V> mapInstance = getNewMap();  // Called only once before iteration

            // Only sort if it's a SortedMap
            if (mapInstance instanceof SortedMap) {
                boolean reverse = REVERSE.equals(getOrdering());

                // Fall back to detecting a reverse comparator when legacy
                // subclasses did not override getOrdering().  Older
                // implementations simply returned a TreeMap constructed with
                // Collections.reverseOrder(), so check the comparator's class
                // name for "reverse" to maintain backward compatibility.
                if (!reverse) {
                    Comparator<?> legacyComp = ((SortedMap<?, ?>) mapInstance).comparator();
                    if (legacyComp != null) {
                        String name = legacyComp.getClass().getName().toLowerCase();
                        reverse = name.contains("reverse");
                    }
                }

                Comparator<Object> comparator = new CompactMapComparator(isCaseInsensitive(), reverse);
                quickSort(array, 0, pairCount - 1, comparator);
            }
            return;
        }

        // Non-legacy mode logic
        String ordering = getOrdering();
        if (ordering.equals(UNORDERED) || ordering.equals(INSERTION)) {
            return;
        }

        Comparator<Object> comparator = new CompactMapComparator(isCaseInsensitive(),
                REVERSE.equals(ordering));
        quickSort(array, 0, pairCount - 1, comparator);
    }

    /**
     * Implements QuickSort for the compact array, maintaining key-value pair relationships.
     * <p>
     * Indices represent pair positions (i.e., lowPair=1 refers to array indices 2,3).
     * Uses recursion to sort subarrays around pivot points.
     * </p>
     *
     * @param array the array of alternating keys and values to sort
     * @param lowPair starting pair index of the subarray
     * @param highPair ending pair index of the subarray
     * @param comparator the comparator to use for key comparison
     */
    private void quickSort(Object[] array, int lowPair, int highPair, Comparator<Object> comparator) {
        if (lowPair < highPair) {
            int pivotPair = partition(array, lowPair, highPair, comparator);
            quickSort(array, lowPair, pivotPair - 1, comparator);
            quickSort(array, pivotPair + 1, highPair, comparator);
        }
    }

    /**
     * Partitions array segment around a pivot while maintaining key-value pairs.
     * <p>
     * Uses median-of-three pivot selection and adjusts indices to handle paired elements.
     * All comparisons are performed on keys (even indices) only.
     * </p>
     *
     * @param array the array of alternating keys and values to partition
     * @param lowPair starting pair index of the partition segment
     * @param highPair ending pair index of the partition segment
     * @param comparator the comparator to use for key comparison
     * @return the final position (pair index) of the pivot
     */
    private int partition(Object[] array, int lowPair, int highPair, Comparator<Object> comparator) {
        int low = lowPair * 2;
        int high = highPair * 2;
        int mid = low + ((high - low) / 4) * 2;

        Object pivot = selectPivot(array, low, mid, high, comparator);

        int i = low - 2;

        for (int j = low; j < high; j += 2) {
            if (comparator.compare(array[j], pivot) <= 0) {
                i += 2;
                swapPairs(array, i, j);
            }
        }

        i += 2;
        swapPairs(array, i, high);
        return i / 2;
    }

    /**
     * Selects and positions the median-of-three pivot for partitioning.
     * <p>
     * Compares first, middle, and last elements to find the median value.
     * Moves the selected pivot to the high position while maintaining pair relationships.
     * </p>
     *
     * @param array the array of alternating keys and values
     * @param low index of the first key in the segment
     * @param mid index of the middle key in the segment
     * @param high index of the last key in the segment
     * @param comparator the comparator to use for key comparison
     * @return the selected pivot value
     */
    private Object selectPivot(Object[] array, int low, int mid, int high,
                               Comparator<Object> comparator) {
        Object first = array[low];
        Object middle = array[mid];
        Object last = array[high];

        if (comparator.compare(first, middle) <= 0) {
            if (comparator.compare(middle, last) <= 0) {
                swapPairs(array, mid, high);  // median is middle
                return middle;
            } else if (comparator.compare(first, last) <= 0) {
                // median is last, already in position
                return last;
            } else {
                swapPairs(array, low, high);  // median is first
                return first;
            }
        } else {
            if (comparator.compare(first, last) <= 0) {
                swapPairs(array, low, high);  // median is first
                return first;
            } else if (comparator.compare(middle, last) <= 0) {
                swapPairs(array, mid, high);  // median is middle
                return middle;
            } else {
                // median is last, already in position
                return last;
            }
        }
    }

    /**
     * Swaps two key-value pairs in the array.
     * <p>
     * Exchanges elements at indices i,i+1 with j,j+1, maintaining
     * the relationship between keys and their values.
     * </p>
     *
     * @param array the array of alternating keys and values
     * @param i the index of the first key to swap
     * @param j the index of the second key to swap
     */
    private void swapPairs(Object[] array, int i, int j) {
        Object tempKey = array[i];
        Object tempValue = array[i + 1];
        array[i] = array[j];
        array[i + 1] = array[j + 1];
        array[j] = tempKey;
        array[j + 1] = tempValue;
    }

    /**
     * Transitions storage from compact array to backing map implementation.
     * <p>
     * Creates new map instance, copies existing entries from array,
     * adds the new key-value pair, and updates internal storage reference.
     * Called when size would exceed compactSize.
     * </p>
     *
     * @param entries the current array of alternating keys and values
     * @param key the new key triggering the transition
     * @param value the value associated with the new key
     */
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
     * Transitions from two entries to single entry storage when removing a key.
     * <p>
     * If the specified key matches either entry, removes it and retains the other entry,
     * transitioning back to single entry storage mode.
     * </p>
     *
     * @param entries array containing exactly two key-value pairs
     * @param key the key to remove
     * @return the previous value associated with the removed key, or null if key not found
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
     * Handles put operation when map contains exactly one entry.
     * <p>
     * If key matches existing entry, updates value. Otherwise, transitions
     * to array storage with both the existing and new entries.
     * Optimizes storage when key matches singleValueKey.
     * </p>
     *
     * @param key the key to add or update
     * @param value the value to associate with the key
     * @return the previous value if key existed, null otherwise
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
        } else {   // Transition to Object[]
            // Create an array with the existing entry and then insert the
            // new entry using the standard compact array logic. This ensures
            // that ordering is properly maintained for sorted or reverse
            // ordered maps and that duplicates are detected correctly.

            Object[] entries = new Object[2];
            entries[0] = getLogicalSingleKey();
            entries[1] = getLogicalSingleValue();

            // Set the internal storage to the two element array so that
            // size() and other methods behave correctly when
            // putInCompactArray() is invoked.
            val = entries;

            // Delegate insertion of the second entry to putInCompactArray()
            // which will handle ordering (including binary search) and
            // growth of the array as needed.
            putInCompactArray(entries, key, value);
            return null;
        }
    }

    /**
     * Handles remove operation when map contains exactly one entry.
     * <p>
     * If key matches the single entry, removes it and transitions to empty state.
     * Otherwise, returns null as key was not found.
     * </p>
     *
     * @param key the key to remove
     * @return the value associated with the removed key, or null if key not found
     */
    private V handleSingleEntryRemove(Object key) {
        if (areKeysEqual(key, getLogicalSingleKey())) {   // Found
            V save = getLogicalSingleValue();
            clear();
            return save;
        }
        return null;   // Not found
    }

    /**
     * Removes entry from map storage and handles transition to array if needed.
     * <p>
     * If size after removal equals compactSize, transitions back to array storage.
     * Otherwise, maintains map storage with entry removed.
     * </p>
     *
     * @param map the current map storage
     * @param key the key to remove
     * @return the value associated with the removed key, or null if key not found
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
     * Copies all mappings from the specified map into this map.
     * <p>
     * Entries are inserted one by one using {@link #put(Object, Object)},
     * allowing the map to transition naturally through its storage modes
     * as elements are added.
     * </p>
     *
     * @param map mappings to be stored in this map
     * @throws NullPointerException if the specified map is null
     */
    public void putAll(Map<? extends K, ? extends V> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        for (Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes all mappings from this map.
     * <p>
     * Resets internal storage to empty state, allowing garbage collection
     * of any existing storage structures.
     * </p>
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
     * Returns a Set view of the keys in this map.
     * <p>
     * The set is backed by the map, so changes to the map are reflected in the set.
     * Set supports element removal but not addition. Iterator supports concurrent
     * modification detection.
     * </p>
     *
     * @return a set view of the keys contained in this map
     */
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            public Iterator<K> iterator() {
                return new CompactKeyIterator();
            }

            public int size() {
                return CompactMap.this.size();
            }

            @Override
            public void clear() {
                CompactMap.this.clear();
            }

            @Override
            public boolean contains(Object o) {
                return CompactMap.this.containsKey(o);
            }    // faster than inherited method

            @Override
            public boolean remove(Object o) {
                final int size = size();
                CompactMap.this.remove(o);
                return size() != size;
            }

            @Override
            public boolean removeAll(Collection c) {
                int size = size();
                for (Object o : c) {
                    CompactMap.this.remove(o);
                }
                return size() != size;
            }

            @Override
            public boolean retainAll(Collection c) {
                // Create fast-access O(1) to all elements within passed in Collection
                Map<K, V> other = getNewMap();

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
     * If the map is modified while an iteration over the collection is in progress (except through the iterators
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
                return new CompactValueIterator();
            }

            public int size() {
                return CompactMap.this.size();
            }

            @Override
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
     * (except through the iterators own {@code remove} operation, or through the {@code setValue} operation on a map
     * entry returned by the iterator), the results of the iteration are undefined. The set supports element removal,
     * which removes the corresponding mapping from the map. It does not support the {@code add} or {@code addAll}
     * operations.
     * </p>
     *
     * @return a set view of the mappings contained in this map
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            public Iterator<Entry<K, V>> iterator() {
                return new CompactEntryIterator();
            }

            public int size() {
                return CompactMap.this.size();
            }

            @Override
            public void clear() {
                CompactMap.this.clear();
            }

            @Override
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

            @Override
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
            @Override
            public boolean removeAll(Collection c) {
                final int size = size();
                for (Object o : c) {
                    remove(o);
                }
                return size() != size;
            }

            @Override
            public boolean retainAll(Collection c) {
                // Create fast-access O(1) to all elements within passed in Collection
                Map<K, V> other = new CompactMap<K, V>() {   // Match outer
                    @Override
                    protected boolean isCaseInsensitive() {
                        return CompactMap.this.isCaseInsensitive();
                    }

                    @Override
                    protected int compactSize() {
                        return CompactMap.this.compactSize();
                    }

                    @Override
                    protected Map<K, V> getNewMap() {
                        return CompactMap.this.getNewMap();
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

    @Deprecated
    public Map<K, V> minus(Object removeMe) {
        throw new UnsupportedOperationException("Unsupported operation [minus] or [-] between Maps.  Use removeAll() or retainAll() instead.");
    }

    @Deprecated
    public Map<K, V> plus(Object right) {
        throw new UnsupportedOperationException("Unsupported operation [plus] or [+] between Maps.  Use putAll() instead.");
    }

    public enum LogicalValueType {
        EMPTY, OBJECT, ENTRY, MAP, ARRAY
    }

    /**
     * Returns the current storage state of this map.
     * <p>
     * Possible states are: EMPTY (no entries), OBJECT (single value), ENTRY (single entry),
     * MAP (backing map), or ARRAY (compact array storage).
     * Used internally to determine appropriate operations for current state.
     * </p>
     *
     * @return the LogicalValueType enum representing current storage state
     */
    public LogicalValueType getLogicalValueType() {
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
     * A specialized Map.Entry implementation for single-entry storage in CompactMap.
     * <p>
     * Extends SimpleEntry to provide:
     * <ul>
     *   <li>Write-through behavior to parent CompactMap on setValue</li>
     *   <li>Case-sensitive/insensitive key comparison based on parent's configuration</li>
     *   <li>Consistent hashCode computation with parent's key comparison logic</li>
     * </ul>
     * </p>
     */
    public class CompactMapEntry extends AbstractMap.SimpleEntry<K, V> {
        public CompactMapEntry(K key, V value) {
            super(key, value);
        }

        @Override
        public V setValue(V value) {
            V save = this.getValue();
            super.setValue(value);
            CompactMap.this.put(getKey(), value);    // "Transmit" (write-thru) to underlying Map.
            return save;
        }

        @Override
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

        @Override
        public int hashCode() {
            return computeKeyHashCode(getKey()) ^ computeValueHashCode(getValue());
        }
    }

    /**
     * Computes hash code for map keys, handling special cases.
     * <p>
     * For String keys, respects case sensitivity setting.
     * Handles null keys, self-referential keys, and standard objects.
     * Used for both map operations and entry hash codes.
     * </p>
     *
     * @param key the key to compute hash code for
     * @return the computed hash code for the key
     */
    protected int computeKeyHashCode(Object key) {
        if (key instanceof String) {
            if (isCaseInsensitive()) {
                return StringUtilities.hashCodeIgnoreCase((String) key);
            } else {   
                return key.hashCode();
            }
        } else {
            if (key == null) {
                return 0;
            } else {
                return key == CompactMap.this ? 37 : key.hashCode();
            }
        }
    }

    /**
     * Computes hash code for map values, handling special cases.
     * <p>
     * Handles null values and self-referential values (where value is this map).
     * Used for both map operations and entry hash codes.
     * </p>
     *
     * @param value the value to compute hash code for
     * @return the computed hash code for the value
     */
    protected int computeValueHashCode(Object value) {
        if (value == CompactMap.this) {
            return 17;
        } else {
            return value == null ? 0 : value.hashCode();
        }
    }

    /**
     * Returns the key when map contains exactly one entry.
     * <p>
     * For CompactMapEntry storage, returns the entry's key.
     * For optimized single value storage, returns the singleValueKey.
     * </p>
     *
     * @return the key of the single entry in this map
     */
    private K getLogicalSingleKey() {
        if (CompactMapEntry.class.isInstance(val)) {
            CompactMapEntry entry = (CompactMapEntry) val;
            return entry.getKey();
        }
        return getSingleValueKey();
    }

    /**
     * Returns the value when map contains exactly one entry.
     * <p>
     * For CompactMapEntry storage, returns the entry's value.
     * For optimized single value storage, returns the direct value.
     * </p>
     *
     * @return the value of the single entry in this map
     */
    private V getLogicalSingleValue() {
        if (CompactMapEntry.class.isInstance(val)) {
            CompactMapEntry entry = (CompactMapEntry) val;
            return entry.getValue();
        }
        return (V) val;
    }

    /**
     * Returns the designated key for optimized single-value storage.
     * <p>
     * When map contains one entry with this key, value is stored directly.
     * Default implementation returns "id". Override to customize.
     * </p>
     *
     * @return the key to use for optimized single-value storage
     */
    protected K getSingleValueKey() {
        return (K) DEFAULT_SINGLE_KEY;
    }

    /**
     * Creates the backing map instance when size exceeds compactSize.
     * <p>
     * Default implementation returns HashMap. Override to provide different
     * map implementation (e.g., TreeMap for sorted maps, LinkedHashMap for
     * insertion ordered maps).
     * </p>
     *
     * @return new empty map instance for backing storage
     */
    protected Map<K, V> getNewMap() {
        return new HashMap<>();
    }
    
    /**
     * Determines if String keys are compared case-insensitively.
     * <p>
     * Default implementation returns false (case-sensitive). Override to change
     * String key comparison behavior. Affects key equality and sorting.
     * </p>
     *
     * @return true if String keys should be compared ignoring case, false otherwise
     */
    protected boolean isCaseInsensitive() {
        return !DEFAULT_CASE_SENSITIVE;
    }

    /**
     * Returns the threshold size for compact array storage.
     * <p>
     * When size exceeds this value, switches to map storage.
     * When size reduces to this value, returns to array storage.
     * Default implementation returns 50.
     * </p>
     *
     * @return the maximum number of entries for compact array storage
     */
    protected int compactSize() {
        return DEFAULT_COMPACT_SIZE;
    }

    /**
     * Returns the ordering strategy for this map.
     * <p>
     * Valid values include:
     * <ul>
     *   <li>{@link #INSERTION}: Maintains insertion order.</li>
     *   <li>{@link #SORTED}: Maintains sorted order.</li>
     *   <li>{@link #REVERSE}: Maintains reverse order.</li>
     *   <li>{@link #UNORDERED}: Default unordered behavior.</li>
     * </ul>
     * </p>
     *
     * @return the ordering strategy for this map
     */
    protected String getOrdering() {
        return UNORDERED;
    }
    
    /**
     * Returns the configuration settings of this CompactMap.
     * <p>
     * The returned map contains the following keys:
     * <ul>
     *   <li>{@link #COMPACT_SIZE} - Maximum size before switching to backing map</li>
     *   <li>{@link #CASE_SENSITIVE} - Whether string keys are case-sensitive</li>
     *   <li>{@link #ORDERING} - Key ordering strategy</li>
     *   <li>{@link #SINGLE_KEY} - Key for optimized single-entry storage</li>
     *   <li>{@link #MAP_TYPE} - Class of backing map implementation</li>
     * </ul>
     * </p>
     *
     * @return an unmodifiable map containing the configuration settings
     */
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put(COMPACT_SIZE, compactSize());
        config.put(CASE_SENSITIVE, !isCaseInsensitive());
        config.put(ORDERING, getOrdering());
        config.put(SINGLE_KEY, getSingleValueKey());
        Map<?, ?> map = getNewMap();
        if (map instanceof CaseInsensitiveMap) {
            map = ((CaseInsensitiveMap<?, ?>) map).getWrappedMap();
        }
        config.put(MAP_TYPE, map.getClass());
        return Collections.unmodifiableMap(config);
    }

    /**
     * Creates a new CompactMap with the same entries but different configuration.
     * <p>
     * This is useful for creating a new CompactMap with the same configuration
     * as another compactMap.
     *
     * <p><b>JDK Requirement:</b> this method ultimately calls
     * {@link Builder#build()} which generates a specialized subclass using the
     * JDK compiler. It will throw an {@link IllegalStateException} when executed
     * in a runtime that lacks these compiler tools (such as a JRE-only
     * container).
     *
     * @param config a map containing configuration options to change
     * @return a new CompactMap with the specified configuration and the same entries
     */
    public CompactMap<K, V> withConfig(Map<String, Object> config) {
        Convention.throwIfNull(config, "config cannot be null");

        // Start with a builder
        Builder<K, V> builder = CompactMap.builder();

        // Handle compactSize with proper priority
        Integer configCompactSize = (Integer) config.get(COMPACT_SIZE);
        int compactSizeToUse = (configCompactSize != null) ? configCompactSize : compactSize();
        builder.compactSize(compactSizeToUse);

        // Handle caseSensitive with proper priority
        Boolean configCaseSensitive = (Boolean) config.get(CASE_SENSITIVE);
        boolean caseSensitiveToUse = (configCaseSensitive != null) ? configCaseSensitive : !isCaseInsensitive();
        builder.caseSensitive(caseSensitiveToUse);

        // Handle ordering with proper priority
        String configOrdering = (String) config.get(ORDERING);
        String orderingToUse = (configOrdering != null) ? configOrdering : getOrdering();

        // Apply the determined ordering
        switch (orderingToUse) {
            case SORTED:
                builder.sortedOrder();
                break;
            case REVERSE:
                builder.reverseOrder();
                break;
            case INSERTION:
                builder.insertionOrder();
                break;
            default:
                builder.noOrder();
        }

        // Handle singleValueKey (this part looks good as fixed)
        String thisSingleKeyValue = (String) getSingleValueKey();
        String configSingleKeyValue = (String) config.get(SINGLE_KEY);

        String priorityKey;
        if (configSingleKeyValue != null) {
            priorityKey = configSingleKeyValue;
        } else if (thisSingleKeyValue != null) {
            priorityKey = thisSingleKeyValue;
        } else {
            priorityKey = DEFAULT_SINGLE_KEY;
        }
        builder.singleValueKey((K) priorityKey);

        // ISSUE 2: MAP_TYPE has same getOrDefault issue
        Class<? extends Map<String, Object>> configMapType = (Class<? extends Map<String, Object>>) config.get(MAP_TYPE);
        Map<?, ?> thisMap = getNewMap();
        Class<? extends Map> thisMapType = thisMap.getClass();

        // Handle CaseInsensitiveMap special case
        if (thisMapType == CaseInsensitiveMap.class && thisMap instanceof CaseInsensitiveMap) {
            thisMapType = ((CaseInsensitiveMap<?, ?>) thisMap).getWrappedMap().getClass();
        }

        Class<? extends Map> mapTypeToUse;
        if (configMapType != null) {
            mapTypeToUse = configMapType;
        } else {
            mapTypeToUse = thisMapType;
        }
        builder.mapType(mapTypeToUse);

        // Build and populate the new map
        CompactMap<K, V> newMap = builder.build();
        newMap.putAll(this);
        return newMap;
    }
    
    /* ------------------------------------------------------------ */
    // iterators

    /**
     * Base iterator implementation for CompactMap's collection views.
     * <p>
     * Handles iteration across all storage states (empty, single entry,
     * array, and map). Provides concurrent modification detection and
     * supports element removal. Extended by key, value, and entry iterators.
     * </p>
     */
    abstract class CompactIterator {
        Iterator<Map.Entry<K, V>> mapIterator;
        Object current;
        int expectedSize;
        int index;

        CompactIterator() {
            expectedSize = size();
            current = EMPTY_MAP;
            index = -1;

            if (val instanceof Object[]) {   // State 3: 2 to compactSize
                sortCompactArray((Object[]) val);
            } else if (val instanceof Map) {   // State 4: > compactSize
                mapIterator = ((Map<K, V>) val).entrySet().iterator();
            } else if (val == EMPTY_MAP) {   // State 1: empty
                // Already handled by initialization of current and index
            } else {   // State 2: size == 1
                // Single value or CompactMapEntry handled in next() methods
            }
        }

        public final boolean hasNext() {
            if (val instanceof Object[]) {   // State 3: 2 to compactSize
                return (index + 1) < size();
            } else if (val instanceof Map) {   // State 4: > compactSize
                return mapIterator.hasNext();
            } else if (val == EMPTY_MAP) {   // State 1: empty
                return false;
            } else {   // State 2: size == 1
                return index < 0;  // Only allow one iteration
            }
        }

        final void advance() {
            if (expectedSize != size()) {
                throw new ConcurrentModificationException();
            }
            if (++index >= size()) {
                throw new NoSuchElementException();
            }
            if (val instanceof Object[]) {  // State 3: 2 to compactSize
                current = ((Object[]) val)[index * 2];  // For keys - values adjust in subclasses
            } else if (val instanceof Map) {  // State 4: > compactSize
                current = mapIterator.next();
            } else if (val == EMPTY_MAP) {  // State 1: empty
                throw new NoSuchElementException();
            } else {  // State 2: size == 1
                current = getLogicalSingleKey();
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

            if (mapIterator != null && newSize == compactSize()) {
                current = ((Map.Entry<K, V>) current).getKey();
                mapIterator = null;
            }

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

    /**
     * Iterator over the map's keys, maintaining storage-appropriate iteration.
     * <p>
     * Provides key-specific iteration behavior while inheriting storage state
     * management and concurrent modification detection from CompactIterator.
     * </p>
     */
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

    /**
     * Iterator over the map's values, maintaining storage-appropriate iteration.
     * <p>
     * Provides value-specific iteration behavior while inheriting storage state
     * management and concurrent modification detection from CompactIterator.
     * </p>
     */
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

    /**
     * Iterator over the map's entries, maintaining storage-appropriate iteration.
     * <p>
     * Provides entry-specific iteration behavior, creating appropriate entry objects
     * for each storage state while inheriting concurrent modification detection
     * from CompactIterator.
     * </p>
     */
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

    /**
     * Creates a new CompactMap instance with specified configuration options.
     * <p>
     * Validates options, generates appropriate template class, and instantiates
     * the map. Template class is cached for reuse with identical configurations.
     * If source map provided in options, initializes with its entries.
     * </p>
     *
     * <table border="1" summary="Configuration Options">
     *   <caption>Available Configuration Options</caption>
     *   <tr>
     *     <th>Option Key</th>
     *     <th>Type</th>
     *     <th>Description</th>
     *     <th>Default</th>
     *   </tr>
     *   <tr>
     *     <td>{@link #COMPACT_SIZE}</td>
     *     <td>Integer</td>
     *     <td>Maximum size before switching to backing map</td>
     *     <td>50</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #CASE_SENSITIVE}</td>
     *     <td>Boolean</td>
     *     <td>Whether String keys are case-sensitive</td>
     *     <td>true</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #MAP_TYPE}</td>
     *     <td>Class&lt;? extends Map&gt;</td>
     *     <td>Type of backing map to use</td>
     *     <td>HashMap.class</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #SINGLE_KEY}</td>
     *     <td>K</td>
     *     <td>Key for optimized single-value storage</td>
     *     <td>"id"</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #SOURCE_MAP}</td>
     *     <td>Map&lt;K,V&gt;</td>
     *     <td>Initial entries for the map</td>
     *     <td>null</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #ORDERING}</td>
     *     <td>String</td>
     *     <td>One of: {@link #UNORDERED}, {@link #SORTED}, {@link #REVERSE}, {@link #INSERTION}</td>
     *     <td>UNORDERED</td>
     *   </tr>
     * </table>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of values maintained by the map
     * @param options configuration options for the map
     * @return a new CompactMap instance configured according to options
     * @throws IllegalArgumentException if options are invalid or incompatible
     * @throws IllegalStateException if template generation or instantiation fails
     *         and the Java compiler tools are not present (for example when only
     *         a JRE is available)
     *
     * <p><b>JDK Requirement:</b> this method generates specialized subclasses at
     * runtime using the JDK compiler. Running in an environment without
     * {@code javax.tools.JavaCompiler} will result in an
     * {@link IllegalStateException}.</p>
     */
    static <K, V> CompactMap<K, V> newMap(Map<String, Object> options) {
        // Ensure JDK Java Compiler is available before proceeding
        if (!ReflectionUtils.isJavaCompilerAvailable()) {
            throw new IllegalStateException(
                    "CompactMap dynamic subclassing requires the Java Compiler (JDK). " +
                            "You are running on a JRE or in an environment where javax.tools.JavaCompiler is not available. " +
                            "Use CompactMap as-is, one of the pre-built subclasses, or provide your own subclass instead."
            );
        }
        
        // Validate and finalize options first (existing code)
        validateAndFinalizeOptions(options);

        try {
            // Get template class for these options
            Class<?> templateClass = TemplateGenerator.getOrCreateTemplateClass(options);

            // Create new instance
            CompactMap<K, V> map = (CompactMap<K, V>) templateClass.getDeclaredConstructor().newInstance();

            // Initialize with source map if provided
            Map<K, V> source = (Map<K, V>) options.get(SOURCE_MAP);
            if (source != null) {
                map.putAll(source);
            }

            return map;
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Failed to create CompactMap instance", e);
        }
    }

    /**
     * Validates and finalizes the configuration options for creating a CompactMap.
     * <p>
     * This method performs several important tasks:
     * <ul>
     *   <li>Validates the compactSize is >= 2</li>
     *   <li>Determines and validates the appropriate map type based on ordering requirements</li>
     *   <li>Ensures compatibility between 'ordering' property and map type</li>
     *   <li>Handles case sensitivity settings</li>
     *   <li>Validates source map compatibility if provided</li>
     * </ul>
     * </p>
     * <p>
     * The method may modify the options map to:
     * <ul>
     *   <li>Set default values for missing options</li>
     *   <li>Adjust the map type based on requirements (e.g., wrapping in CaseInsensitiveMap)</li>
     *   <li>Store the original map type as INNER_MAP_TYPE when wrapping is needed</li>
     * </ul>
     * </p>
     *
     * @param options the map of configuration options to validate and finalize. The map may be modified
     *               by this method.
     * @throws IllegalArgumentException if:
     *         <ul>
     *           <li>compactSize is less than 2</li>
     *           <li>map type is incompatible with specified ordering</li>
     *           <li>source map's ordering conflicts with requested ordering</li>
     *           <li>IdentityHashMap or WeakHashMap is specified as map type</li>
     *           <li>specified map type is not a Map class</li>
     *           <li>map type comes from a disallowed package</li>
     *         </ul>
     * @see #COMPACT_SIZE
     * @see #CASE_SENSITIVE
     * @see #MAP_TYPE
     * @see #ORDERING
     * @see #SOURCE_MAP
     */
    static void validateAndFinalizeOptions(Map<String, Object> options) {
        String ordering = (String) options.getOrDefault(ORDERING, UNORDERED);

        // Validate compactSize
        int compactSize = (int) options.getOrDefault(COMPACT_SIZE, DEFAULT_COMPACT_SIZE);
        if (compactSize < 2) {
            throw new IllegalArgumentException("compactSize must be >= 2");
        }

        Class<? extends Map> mapType = determineMapType(options, ordering);
        if (!isAllowedMapType(mapType)) {
            throw new IllegalArgumentException("Map type " + mapType.getName() +
                    " is not from an allowed package");
        }
        boolean caseSensitive = (boolean) options.getOrDefault(CASE_SENSITIVE, DEFAULT_CASE_SENSITIVE);

        // Store the validated mapType
        options.put(MAP_TYPE, mapType);

        // Get remaining options
        Map<?, ?> sourceMap = (Map<?, ?>) options.get(SOURCE_MAP);

        // Check source map ordering compatibility
        if (sourceMap != null) {
            String sourceOrdering = MapUtilities.detectMapOrdering(sourceMap);
            if (!UNORDERED.equals(ordering) && !UNORDERED.equals(sourceOrdering) &&
                    !ordering.equals(sourceOrdering)) {
                throw new IllegalArgumentException(
                        "Requested ordering '" + ordering +
                                "' conflicts with source map's ordering '" + sourceOrdering +
                                "'. Map structure: " + MapUtilities.getMapStructureString(sourceMap));
            }
        }

        // Handle case sensitivity
        if (!caseSensitive && (!SORTED.equals(ordering) && !REVERSE.equals(ordering) && (mapType != CaseInsensitiveMap.class))) {
            options.put(INNER_MAP_TYPE, mapType);
            options.put(MAP_TYPE, CaseInsensitiveMap.class);
        }

        // Final default resolution
        options.putIfAbsent(COMPACT_SIZE, DEFAULT_COMPACT_SIZE);
        options.putIfAbsent(CASE_SENSITIVE, DEFAULT_CASE_SENSITIVE);
    }

    /**
     * Determines the appropriate Map implementation based on configuration options and ordering requirements.
     * <p>
     * This method performs several tasks:
     * <ul>
     *     <li>Validates that unsupported map types (IdentityHashMap, WeakHashMap) are not used</li>
     *     <li>Determines the appropriate map type based on ordering if none specified</li>
     *     <li>Infers ordering from map type if ordering not specified</li>
     *     <li>Validates compatibility between specified map type and ordering</li>
     * </ul>
     *
     * @param options the configuration options map containing:
     *               <ul>
     *                   <li>{@link #MAP_TYPE} - optional, the requested map implementation</li>
     *                   <li>{@link #ORDERING} - optional, the requested ordering strategy</li>
     *               </ul>
     * @param ordering the current ordering strategy (UNORDERED, SORTED, REVERSE, or INSERTION)
     *
     * @return the determined map implementation class to use
     *
     * @throws IllegalArgumentException if:
     *         <ul>
     *             <li>IdentityHashMap or WeakHashMap is specified</li>
     *             <li>specified map type is not compatible with requested ordering</li>
     *             <li>specified map type is not a Map class</li>
     *         </ul>
     *
     * @see #UNORDERED
     * @see #SORTED
     * @see #REVERSE
     * @see #INSERTION
     */
    private static Class<? extends Map> determineMapType(Map<String, Object> options, String ordering) {
        Class<? extends Map> rawMapType = (Class<? extends Map>) options.get(MAP_TYPE);

        // Handle special map types first
        if (rawMapType != null) {
            if (IdentityHashMap.class.isAssignableFrom(rawMapType)) {
                throw new IllegalArgumentException(
                        "IdentityHashMap is not supported as it compares keys by reference identity");
            }
            if (WeakHashMap.class.isAssignableFrom(rawMapType)) {
                throw new IllegalArgumentException(
                        "WeakHashMap is not supported as it can unpredictably remove entries");
            }
        }

        // Determine map type and ordering together
        if (rawMapType == null) {
            // No map type specified, determine based on ordering
            if (ordering.equals(INSERTION)) {
                rawMapType = LinkedHashMap.class;
            } else if (ordering.equals(SORTED) || ordering.equals(REVERSE)) {
                rawMapType = TreeMap.class;
            } else {
                rawMapType = DEFAULT_MAP_TYPE;
            }
        } else if (options.get(ORDERING) == null) {
            // Map type specified but no ordering, determine ordering from map type
            if (LinkedHashMap.class.isAssignableFrom(rawMapType) ||
                    EnumMap.class.isAssignableFrom(rawMapType)) {
                ordering = INSERTION;
            } else if (SortedMap.class.isAssignableFrom(rawMapType)) {
                ordering = rawMapType.getName().toLowerCase().contains(REVERSE) ||
                        rawMapType.getName().toLowerCase().contains("descending")
                        ? REVERSE : SORTED;
            } else {
                ordering = UNORDERED;
            }
            options.put(ORDERING, ordering);
        }

        // Validate compatibility
        if (!(rawMapType == CompactMap.class ||
                rawMapType == CaseInsensitiveMap.class ||
                rawMapType == TrackingMap.class)) {

            boolean isValidForOrdering;
            if (ordering.equals(INSERTION)) {
                isValidForOrdering = LinkedHashMap.class.isAssignableFrom(rawMapType) ||
                        EnumMap.class.isAssignableFrom(rawMapType);
            } else if (ordering.equals(SORTED) || ordering.equals(REVERSE)) {
                isValidForOrdering = SortedMap.class.isAssignableFrom(rawMapType);
            } else {
                isValidForOrdering = true; // Any map can be unordered
            }

            if (!isValidForOrdering) {
                throw new IllegalArgumentException("Map type " + rawMapType.getSimpleName() +
                        " is not compatible with ordering '" + ordering + "'");
            }
        }
        
        // Validate mapType is actually a Map
        options.put(MAP_TYPE, rawMapType);
        if (rawMapType != null && !Map.class.isAssignableFrom(rawMapType)) {
            throw new IllegalArgumentException("mapType must be a Map class");
        }
        
        return rawMapType;
    }

    /**
     * Returns a builder for creating customized CompactMap instances.
     * <p>
     * For detailed configuration options and examples, see {@link Builder}.
     * <p>
     * Note: When method chaining directly from builder(), you may need to provide
     * a type witness to help type inference:
     * <pre>{@code
     * // Type witness needed:
     * CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
     *         .sortedOrder()
     *         .build();
     *
     * // Alternative without type witness:
     * Builder<String, Integer> builder = CompactMap.builder();
     * CompactMap<String, Integer> map = builder.sortedOrder().build();
     * }</pre>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     * @return a new CompactMapBuilder instance
     *
     * @see Builder
     */
    public static <K, V> Builder<K, V> builder() {
        return new Builder<>();
    }

    /**
     * Builder class for creating customized CompactMap instances.
     * <p>
     * Simple example with common options:
     * <pre>{@code
     * CompactMap<String, Object> map = CompactMap.<String, Object>builder()
     *         .caseSensitive(false)
     *         .sortedOrder()
     *         .build();
     * }</pre>
     * <p>
     * Note the type witness ({@code <String, Object>}) in the example above. This explicit type
     * information is required when method chaining directly from builder() due to Java's type
     * inference limitations. Alternatively, you can avoid the type witness by splitting the
     * builder creation and configuration:
     * <pre>{@code
     * // Using type witness
     * CompactMap<String, Object> map1 = CompactMap.<String, Object>builder()
     *         .sortedOrder()
     *         .build();
     *
     * // Without type witness
     * Builder<String, Object> builder = CompactMap.builder();
     * CompactMap<String, Object> map2 = builder
     *         .sortedOrder()
     *         .build();
     * }</pre>
     * <p>
     * Comprehensive example with all options:
     * <pre>{@code
     * CompactMap<String, Object> map = CompactMap.<String, Object>builder()
     *         .caseSensitive(false)           // Enable case-insensitive key comparison
     *         .compactSize(80)                // Set threshold for switching to backing map
     *         .mapType(LinkedHashMap.class)   // Specify backing map implementation
     *         .singleValueKey("uuid")         // Optimize storage for single entry with this key
     *         .sourceMap(existingMap)         // Initialize with entries from another map
     *         .insertionOrder()               // Or: .reverseOrder(), .sortedOrder(), .noOrder()
     *         .build();
     * }</pre>
     * 
     * <table border="1" cellpadding="5" summary="Builder Options">
     *   <caption>Available Builder Options</caption>
     *   <tr>
     *     <th>Method</th>
     *     <th>Description</th>
     *     <th>Default</th>
     *   </tr>
     *   <tr>
     *     <td>{@link #caseSensitive(boolean)}</td>
     *     <td>Controls case sensitivity for string keys</td>
     *     <td>true</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #compactSize(int)}</td>
     *     <td>Maximum size before switching to backing map</td>
     *     <td>50</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #mapType(Class)}</td>
     *     <td>Type of backing map when size exceeds compact size</td>
     *     <td>HashMap.class</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #singleValueKey(Object)}</td>
     *     <td>Special key that enables optimized storage when map contains only one entry with this key</td>
     *     <td>"id"</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #sourceMap(Map)}</td>
     *     <td>Initializes the CompactMap with entries from the provided map</td>
     *     <td>null</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #sortedOrder()}</td>
     *     <td>Maintains keys in sorted order</td>
     *     <td>unordered</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #reverseOrder()}</td>
     *     <td>Maintains keys in reverse order</td>
     *     <td>unordered</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #insertionOrder()}</td>
     *     <td>Maintains keys in insertion order</td>
     *     <td>unordered</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #noOrder()}</td>
     *     <td>Explicitly sets unordered behavior</td>
     *     <td>unordered</td>
     *   </tr>
     * </table>
     *
     * @param <K> the type of keys maintained by the map
     * @param <V> the type of mapped values
     *
     * @see CompactMap
     */
    public static final class Builder<K, V> {
        private final Map<String, Object> options;

        private Builder() {
            options = new HashMap<>();
        }

        /**
         * Sets whether String keys should be compared case-sensitively.
         * <p>
         * When set to false, String keys will be compared ignoring case. For example,
         * "Key", "key", and "KEY" would all be considered equal. This setting only
         * affects String keys; other key types are compared normally. Maps can
         * contain heterogeneous content (Strings, Numbers, null, as keys).
         *
         * @param caseSensitive true for case-sensitive comparison (default),
         *                      false for case-insensitive comparison
         * @return this builder instance for method chaining
         */
        public Builder<K, V> caseSensitive(boolean caseSensitive) {
            options.put(CASE_SENSITIVE, caseSensitive);
            return this;
        }

        /**
         * Sets the type of Map to use when size exceeds compact storage threshold.
         * <p>
         * Common map types include:
         * <ul>
         *     <li>{@link HashMap} - Default, unordered storage</li>
         *     <li>{@link TreeMap} - Sorted key order</li>
         *     <li>{@link LinkedHashMap} - Insertion order</li>
         * </ul>
         * Note: {@link IdentityHashMap} and {@link WeakHashMap} are not supported.
         * The map type must come from an allowed package
         * ({@code java.util.*}, {@code java.util.concurrent.*}, or
         * {@code com.cedarsoftware.util.*}).
         *
         * @param mapType the Class object representing the desired Map implementation
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if mapType is not a Map class or is
         *         from a disallowed package
         */
        public Builder<K, V> mapType(Class<? extends Map> mapType) {
            if (!Map.class.isAssignableFrom(mapType)) {
                throw new IllegalArgumentException("mapType must be a Map class");
            }
            if (!isAllowedMapType(mapType)) {
                throw new IllegalArgumentException("Map type " + mapType.getName() +
                        " is not from an allowed package");
            }
            options.put(MAP_TYPE, mapType);
            return this;
        }

        /**
         * Sets a special key for optimized single-entry storage.
         * <p>
         * When the map contains exactly one entry with this key, the value is stored
         * directly without wrapper objects, reducing memory overhead. The default
         * single value key is "id".
         *
         * @param key the key to use for optimized single-entry storage
         * @return this builder instance for method chaining
         */
        public Builder<K, V> singleValueKey(K key) {
            options.put(SINGLE_KEY, key);
            return this;
        }

        /**
         * Sets the maximum size for compact array storage.
         * <p>
         * When the map size is between 2 and this value, entries are stored in a
         * compact array format. Above this size, entries are moved to a backing map.
         * Must be greater than or equal to 2.
         *
         * @param size the maximum number of entries to store in compact format
         * @return this builder instance for method chaining
         */
        public Builder<K, V> compactSize(int size) {
            options.put(COMPACT_SIZE, size);
            return this;
        }

        /**
         * Configures the map to maintain keys in natural sorted order.
         * <p>
         * Keys must be {@link Comparable} or a {@link ClassCastException} will be
         * thrown when incomparable keys are inserted. For String keys, the ordering
         * respects the case sensitivity setting.
         *
         * @return this builder instance for method chaining
         */
        public Builder<K, V> sortedOrder() {
            options.put(ORDERING, CompactMap.SORTED);
            return this;
        }

        /**
         * Configures the map to maintain keys in reverse sorted order.
         * <p>
         * Keys must be {@link Comparable} or a {@link ClassCastException} will be
         * thrown when incomparable keys are inserted. For String keys, the ordering
         * respects the case sensitivity setting.
         *
         * @return this builder instance for method chaining
         */
        public Builder<K, V> reverseOrder() {
            options.put(ORDERING, CompactMap.REVERSE);
            return this;
        }

        /**
         * Configures the map to maintain keys in insertion order.
         * <p>
         * The iteration order will match the order in which entries were added
         * to the map. This ordering is preserved even when entries are updated.
         *
         * @return this builder instance for method chaining
         */
        public Builder<K, V> insertionOrder() {
            options.put(ORDERING, CompactMap.INSERTION);
            return this;
        }

        /**
         * Explicitly configures the map to not maintain any specific ordering.
         * <p>
         * This is the default behavior if no ordering is specified. The iteration
         * order may change as entries are added or removed.
         *
         * @return this builder instance for method chaining
         */
        public Builder<K, V> noOrder() {
            options.put(ORDERING, CompactMap.UNORDERED);
            return this;
        }

        /**
         * Initializes the map with entries from the specified source map.
         * <p>
         *
         * @param source the map whose entries are to be copied
         * @return this builder instance for method chaining
         * @throws IllegalArgumentException if source map's ordering conflicts with
         *         configured ordering
         */
        public Builder<K, V> sourceMap(Map<K, V> source) {
            options.put(SOURCE_MAP, source);
            return this;
        }

        /**
         * Creates a new CompactMap instance with the configured options.
         * <p>
         * This method validates all options and creates a specialized implementation
         * based on the configuration. The resulting map is optimized for the
         * specified combination of options.
         *
         * <p><b>JDK Requirement:</b> this method generates a specialized subclass
         * at runtime using {@code javax.tools.JavaCompiler}. It will throw an
         * {@link IllegalStateException} when the compiler tools are not present
         * (for example in a JRE-only environment).
         *
         * @return a new CompactMap instance
         * @throws IllegalStateException if JavaCompiler is unavailable at runtime (JRE detected)
         */
        public CompactMap<K, V> build() {
            if (!ReflectionUtils.isJavaCompilerAvailable()) {
                throw new IllegalStateException(
                        "CompactMap builder pattern requires the Java Compiler (JDK). " +
                                "You are running on a JRE or in an environment where javax.tools.JavaCompiler is not available. " +
                                "Use CompactMap as-is, one of the pre-built subclasses, or provide your own subclass instead."
                );
            }
            return CompactMap.newMap(options);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------
    
    /**
     * Internal class that handles dynamic generation of specialized CompactMap implementations.
     * <p>
     * This class generates and compiles optimized CompactMap subclasses at runtime based on
     * configuration options. Generated classes are cached for reuse. Class names encode their
     * configuration, for example: "CompactMap$HashMap_CS_S50_id_Unord" represents a
     * case-sensitive, unordered map with HashMap backing, compact size of 50, and "id" as
     * the single value key.
     * <p>
     * This is an implementation detail and not part of the public API.
     */
    private static final class TemplateGenerator {
        private static final String TEMPLATE_CLASS_PREFIX = "com.cedarsoftware.util.CompactMap$";

        /**
         * Returns an existing or creates a new template class for the specified configuration options.
         * <p>
         * First attempts to load an existing template class matching the options. If not found,
         * generates, compiles, and loads a new template class. Generated classes are cached
         * for future reuse.
         *
         * @param options configuration map containing case sensitivity, ordering, map type, etc.
         * @return the template Class object matching the specified options
         * @throws IllegalStateException if template generation or compilation fails
         */
        private static Class<?> getOrCreateTemplateClass(Map<String, Object> options) {
            String className = generateClassName(options);
            try {
                return templateClassLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                return generateTemplateClass(options);
            }
        }
        /**
         * Generates a unique class name encoding the configuration options.
         * <p>
         * Format: "CompactMap$[MapType]_[CS/CI]_S[Size]_[SingleKey]_[Order]"
         * Example: "CompactMap$HashMap_CS_S50_id_Unord" represents:
         * <ul>
         *     <li>HashMap backing</li>
         *     <li>Case Sensitive (CS)</li>
         *     <li>Size 50</li>
         *     <li>Single key "id"</li>
         *     <li>Unordered</li>
         * </ul>
         *
         * @param options configuration map containing case sensitivity, ordering, map type, etc.
         * @return the generated class name
         */
        private static String generateClassName(Map<String, Object> options) {
            StringBuilder keyBuilder = new StringBuilder(TEMPLATE_CLASS_PREFIX);

            // Add map type's simple name
            Object mapTypeObj = options.get(MAP_TYPE);
            if (mapTypeObj instanceof Class) {
                keyBuilder.append(((Class<?>) mapTypeObj).getSimpleName());
            } else {
                keyBuilder.append((String) mapTypeObj);
            }

            // Add case sensitivity
            keyBuilder.append('_')
                    .append((boolean)options.getOrDefault(CASE_SENSITIVE, DEFAULT_CASE_SENSITIVE) ? "CS" : "CI");

            // Add size
            keyBuilder.append("_S")
                    .append(options.getOrDefault(COMPACT_SIZE, DEFAULT_COMPACT_SIZE));

            // Add single key value (convert to title case and remove non-alphanumeric)
            String singleKey = (String) options.getOrDefault(SINGLE_KEY, DEFAULT_SINGLE_KEY);
            singleKey = singleKey.substring(0, 1).toUpperCase() + singleKey.substring(1);
            singleKey = singleKey.replaceAll("[^a-zA-Z0-9]", "");
            keyBuilder.append('_').append(singleKey);

            // Add ordering
            String ordering = (String) options.getOrDefault(ORDERING, UNORDERED);
            keyBuilder.append('_');
            switch (ordering) {
                case SORTED:
                    keyBuilder.append("Sort");
                    break;
                case REVERSE:
                    keyBuilder.append("Rev");
                    break;
                case INSERTION:
                    keyBuilder.append("Ins");
                    break;
                default:
                    keyBuilder.append("Unord");
            }

            return keyBuilder.toString();
        }

        /**
         * Creates a new template class for the specified configuration options.
         * <p>
         * This method effectively synchronizes on the class name to ensure that only one thread can be
         * compiling a particular class, but multiple threads can compile different classes concurrently.
         * <ul>
         *     <li>Double-checks if class was created while waiting for lock</li>
         *     <li>Generates source code for the template class</li>
         *     <li>Compiles the source code</li>
         *     <li>Loads and returns the compiled class</li>
         * </ul>
         *
         * @param options configuration map containing case sensitivity, ordering, map type, etc.
         * @return the newly generated and compiled template Class
         * @throws IllegalStateException if compilation fails or class cannot be loaded
         */
        private static Class<?> generateTemplateClass(Map<String, Object> options) {
            // Determine the target class name
            String className = generateClassName(options);

            // Acquire (or create) a lock dedicated to this className
            ReentrantLock lock = CLASS_LOCKS.computeIfAbsent(className, k -> new ReentrantLock());

            lock.lock();
            try {
                // --- Double-check if class was created while waiting for lock ---
                try {
                    return ClassUtilities.getClassLoader(CompactMap.class).loadClass(className);
                } catch (ClassNotFoundException ignored) {
                    // Not found, proceed with generation
                }

                // --- Generate source code ---
                String sourceCode = generateSourceCode(className, options);

                // --- Compile the source code using JavaCompiler ---
                Class<?> templateClass = compileClass(className, sourceCode);
                return templateClass;
            }
            finally {
                lock.unlock();
            }
        }
        
        /**
         * Generates Java source code for a CompactMap template class.
         * <p>
         * Creates a class that extends CompactMap and overrides:
         * <ul>
         *     <li>isCaseInsensitive()</li>
         *     <li>compactSize()</li>
         *     <li>getSingleValueKey()</li>
         *     <li>getOrdering()</li>
         *     <li>getNewMap()</li>
         * </ul>
         * The generated class implements the behavior specified by the configuration options.
         *
         * @param className fully qualified name for the generated class
         * @param options configuration map containing case sensitivity, ordering, map type, etc.
         * @return Java source code as a String
         */
        private static String generateSourceCode(String className, Map<String, Object> options) {
            String simpleClassName = className.substring(className.lastIndexOf('.') + 1);
            StringBuilder sb = new StringBuilder();

            // Package declaration
            sb.append("package com.cedarsoftware.util;\n\n");

            // Basic imports
            sb.append("import java.util.*;\n");
            sb.append("import java.util.concurrent.*;\n");

            // Add import for test classes if needed
            Class<?> mapType = (Class<?>) options.get(MAP_TYPE);
            if (mapType != null) {
                String mapClassName = getMapClassName(mapType);
                if (!mapClassName.startsWith("java.util.") &&
                        !mapClassName.startsWith("java.util.concurrent.") &&
                        !mapClassName.startsWith("com.cedarsoftware.util.")) {
                    sb.append("import ").append(mapClassName).append(";\n");
                }
            }
            
            sb.append("\n");

            // Class declaration
            sb.append("public class ").append(simpleClassName)
                    .append(" extends CompactMap {\n");

            // Override isCaseInsensitive
            boolean caseSensitive = (boolean)options.getOrDefault(CASE_SENSITIVE, DEFAULT_CASE_SENSITIVE);
            sb.append("    @Override\n")
                    .append("    protected boolean isCaseInsensitive() {\n")
                    .append("        return ").append(!caseSensitive).append(";\n")
                    .append("    }\n\n");

            // Override compactSize
            sb.append("    @Override\n")
                    .append("    protected int compactSize() {\n")
                    .append("        return ").append(options.getOrDefault(COMPACT_SIZE, DEFAULT_COMPACT_SIZE)).append(";\n")
                    .append("    }\n\n");

            // Override getSingleValueKey
            sb.append("    @Override\n")
                    .append("    protected Object getSingleValueKey() {\n")
                    .append("        return \"").append(options.getOrDefault(SINGLE_KEY, DEFAULT_SINGLE_KEY)).append("\";\n")
                    .append("    }\n\n");

            // Override getOrdering
            String ordering = (String)options.getOrDefault(ORDERING, UNORDERED);
            sb.append("    @Override\n")
                    .append("    protected String getOrdering() {\n")
                    .append("        return \"").append(ordering).append("\";\n")
                    .append("    }\n\n");

            // Add getNewMap override
            appendGetNewMapOverride(sb, options);

            // Close class
            sb.append("}\n");
            return sb.toString();
        }

        /**
         * Generates the getNewMap() method override for the template class.
         * <p>
         * Creates code that instantiates the appropriate map type with:
         * <ul>
         *     <li>Correct constructor (default, capacity, or comparator)</li>
         *     <li>Error handling for constructor failures</li>
         *     <li>Type validation checks</li>
         *     <li>Support for wrapper maps (CaseInsensitive, etc.)</li>
         * </ul>
         *
         * @param sb StringBuilder to append the generated code to
         * @param options configuration map containing map type and related options
         */
        private static void appendGetNewMapOverride(StringBuilder sb, Map<String, Object> options) {
            // Main method template
            String methodTemplate =
                    "    @Override\n" +
                            "    protected Map getNewMap() {\n" +
                            "        Map map;\n" +
                            "        try {\n" +
                            "%s" +  // Indented map creation code will be inserted here
                            "        } catch (Exception e) {\n" +
                            "            throw new IllegalStateException(\"Failed to create map instance\", e);\n" +
                            "        }\n" +
                            "        if (!(map instanceof Map)) {\n" +
                            "            throw new IllegalStateException(\"mapType must be a Map class\");\n" +
                            "        }\n" +
                            "        return map;\n" +
                            "    }\n";

            // Get the appropriate map creation code and indent it
            String mapCreationCode = getMapCreationCode(options);
            String indentedCreationCode = indentCode(mapCreationCode, 12); // 3 levels of indent * 4 spaces

            // Combine it all
            sb.append(String.format(methodTemplate, indentedCreationCode));
        }

        /**
         * Generates code to create a sorted map instance (TreeMap or similar).
         */
        private static String getSortedMapCreationCode(Class<?> mapType, boolean caseSensitive,
                                                       String ordering, Map<String, Object> options) {
            // Template for comparator-based constructor
            String comparatorTemplate =
                    "map = new %s(new CompactMapComparator(%b, %b));";

            // Check if capacity constructor exists using ReflectionUtils
            boolean hasCapacityConstructor = ReflectionUtils.getConstructor(mapType, int.class) != null;

            // Template based on available constructors
            String capacityTemplate = hasCapacityConstructor ?
                    "map = new %s();\n" +
                            "map = new %s(%d);" :
                    "map = new %s();";

            if (hasComparatorConstructor(mapType)) {
                return String.format(comparatorTemplate,
                        getMapClassName(mapType),
                        !caseSensitive,
                        REVERSE.equals(ordering));
            } else {
                int compactSize = (Integer) options.getOrDefault(COMPACT_SIZE, DEFAULT_COMPACT_SIZE);
                if (hasCapacityConstructor) {
                    return String.format(capacityTemplate,
                            getMapClassName(mapType),
                            getMapClassName(mapType),
                            compactSize + 1);  // Use compactSize + 1 as capacity
                } else {
                    return String.format(capacityTemplate, getMapClassName(mapType));
                }
            }
        }

        /**
         * Generates code to create a standard (non-sorted) map instance.
         */
        private static String getStandardMapCreationCode(Class<?> mapType, Map<String, Object> options) {
            // Check if capacity constructor exists using ReflectionUtils
            boolean hasCapacityConstructor = ReflectionUtils.getConstructor(mapType, int.class) != null;

            // Template based on available constructors
            String template = hasCapacityConstructor ?
                    "map = new %s();\n" +
                            "map = new %s(%d);" :
                    "map = new %s();";

            String mapClassName = getMapClassName(mapType);
            int compactSize = (Integer) options.getOrDefault(COMPACT_SIZE, DEFAULT_COMPACT_SIZE);

            if (hasCapacityConstructor) {
                return String.format(template,
                        mapClassName,
                        mapClassName,
                        compactSize + 1);  // Use compactSize + 1 as initial capacity
            } else {
                return String.format(template, mapClassName);
            }
        }

        /**
         * Checks if the map class has a constructor that accepts a Comparator.
         * <p>
         * Used to determine if a sorted map can be created with a custom
         * comparator (e.g., case-insensitive or reverse order).
         *
         * @param mapType the Class object for the map implementation
         * @return true if the class has a Comparator constructor, false otherwise
         */
        private static boolean hasComparatorConstructor(Class<?> mapType) {
            return ReflectionUtils.getConstructor(mapType, Comparator.class) != null;
        }

        /**
         * Returns the appropriate class name for use in generated code.
         * <p>
         * Handles special cases:
         * <ul>
         *     <li>Inner classes (converts '$' to '.')</li>
         *     <li>Test classes (uses simple name)</li>
         *     <li>java-util classes (uses enclosing class prefix)</li>
         * </ul>
         *
         * @param mapType the Class object for the map implementation
         * @return fully qualified or simple class name appropriate for generated code
         */
        private static String getMapClassName(Class<?> mapType) {
            if (mapType.getEnclosingClass() != null) {
                if (mapType.getName().contains("Test")) {
                    return mapType.getSimpleName();
                } else if (mapType.getPackage().getName().equals("com.cedarsoftware.util")) {
                    return mapType.getEnclosingClass().getSimpleName() + "." + mapType.getSimpleName();
                }
                return mapType.getName().replace('$', '.');
            }
            return mapType.getName();
        }

        /**
         * Indents each line of the provided code by the specified number of spaces.
         * <p>
         * Splits input on newlines, adds leading spaces to each line, and
         * rejoins with newlines. Used to format generated source code with
         * proper indentation.
         *
         * @param code the source code to indent
         * @param spaces number of spaces to add at start of each line
         * @return the indented source code
         */
        private static String indentCode(String code, int spaces) {
            String indent = String.format("%" + spaces + "s", "");
            return Arrays.stream(code.split("\n"))
                    .map(line -> indent + line)
                    .collect(Collectors.joining("\n"));
        }

        /**
         * Generates code to instantiate the appropriate map implementation.
         * <p>
         * Handles multiple scenarios:
         * <ul>
         *     <li>CaseInsensitiveMap with specified inner map type</li>
         *     <li>Sorted maps (TreeMap) with comparator</li>
         *     <li>Standard maps with capacity constructor</li>
         *     <li>Wrapper maps (maintaining inner map characteristics)</li>
         * </ul>
         * Generated code includes proper error handling and constructor fallbacks.
         *
         * @param options configuration map containing MAP_TYPE, INNER_MAP_TYPE, ordering, etc.
         * @return String containing Java code to create the configured map instance
         */
        private static String getMapCreationCode(Map<String, Object> options) {
            String ordering = (String)options.getOrDefault(ORDERING, UNORDERED);
            boolean caseSensitive = (boolean)options.getOrDefault(CASE_SENSITIVE, DEFAULT_CASE_SENSITIVE);
            Class<?> mapType = (Class<?>)options.getOrDefault(MAP_TYPE, DEFAULT_MAP_TYPE);

            // Handle CaseInsensitiveMap with inner map type
            if (mapType == CaseInsensitiveMap.class) {
                Class<?> innerMapType = (Class<?>) options.get(INNER_MAP_TYPE);
                if (innerMapType != null) {
                    if (SORTED.equals(ordering) || REVERSE.equals(ordering)) {
                        return String.format(
                                "map = new CaseInsensitiveMap(new %s(new CompactMapComparator(%b, %b)));",
                                getMapClassName(innerMapType),
                                !caseSensitive,
                                REVERSE.equals(ordering));
                    } else {
                        String template =
                                "Map innerMap = new %s();\n" +
                                        "try {\n" +
                                        "    innerMap = new %s(%d);\n" +
                                        "} catch (Exception e) {\n" +
                                        "    // Fallback to default constructor already done\n" +
                                        "}\n" +
                                        "map = new CaseInsensitiveMap(innerMap);";

                        return String.format(template,
                                getMapClassName(innerMapType),
                                getMapClassName(innerMapType),
                                (Integer)options.getOrDefault(COMPACT_SIZE, DEFAULT_COMPACT_SIZE) + 1);
                    }
                }
            }

            // Handle regular sorted/ordered maps
            if (SORTED.equals(ordering) || REVERSE.equals(ordering)) {
                return getSortedMapCreationCode(mapType, caseSensitive, ordering, options);
            } else {
                return getStandardMapCreationCode(mapType, options);
            }
        }

        /**
         * Compiles Java source code into a Class object at runtime.
         * <p>
         * Process:
         * <ul>
         *     <li>Uses JavaCompiler from JDK tools</li>
         *     <li>Compiles in memory (no file system access needed)</li>
         *     <li>Captures compilation diagnostics for error reporting</li>
         *     <li>Loads compiled bytecode via custom ClassLoader</li>
         * </ul>
         *
         * @param className fully qualified name for the class to create
         * @param sourceCode Java source code to compile
         * @return the compiled Class object
         * @throws IllegalStateException if compilation fails or JDK compiler unavailable
         */
        private static Class<?> compileClass(String className, String sourceCode) {
            JavaCompiler compiler;
            try {
                compiler = ToolProvider.getSystemJavaCompiler();
            } catch (Throwable t) {
                throw new IllegalStateException("No JavaCompiler found (JDK required, not just JRE).", t);
            }
            if (compiler == null) {
                throw new IllegalStateException("No JavaCompiler found. Ensure JDK (not just JRE) is being used.");
            }

            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(diagnostics, null, null);

            // Create in-memory source file
            SimpleJavaFileObject sourceFile = new SimpleJavaFileObject(
                    URI.create("string:///" + className.replace('.', '/') + ".java"),
                    JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return sourceCode;
                }
            };

            // Create in-memory output for class file
            Map<String, ByteArrayOutputStream> classOutputs = new HashMap<>();
            JavaFileManager fileManager = new ForwardingJavaFileManager(stdFileManager) {
                @Override
                public JavaFileObject getJavaFileForOutput(Location location,
                                                           String className,
                                                           JavaFileObject.Kind kind,
                                                           FileObject sibling) throws IOException {
                    if (kind == JavaFileObject.Kind.CLASS) {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        classOutputs.put(className, outputStream);
                        return new SimpleJavaFileObject(
                                URI.create("byte:///" + className.replace('.', '/') + ".class"),
                                JavaFileObject.Kind.CLASS) {
                            @Override
                            public OutputStream openOutputStream() {
                                return outputStream;
                            }
                        };
                    }
                    return super.getJavaFileForOutput(location, className, kind, sibling);
                }
            };

            // Compile the source
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,                // Writer for compiler messages
                    fileManager,         // Custom file manager
                    diagnostics,         // DiagnosticListener
                    Collections.singletonList("-proc:none"), // Compiler options - disable annotation processing
                    null,                // Classes for annotation processing
                    Collections.singletonList(sourceFile)  // Source files to compile
            );

            boolean success = task.call();
            if (!success) {
                StringBuilder error = new StringBuilder("Compilation failed:\n");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    error.append(diagnostic.toString()).append('\n');
                }
                throw new IllegalStateException(error.toString());
            }

            // Get the class bytes
            ByteArrayOutputStream classOutput = classOutputs.get(className);
            if (classOutput == null) {
                throw new IllegalStateException("No class file generated for " + className);
            }

            // Define the class
            byte[] classBytes = classOutput.toByteArray();
            return defineClass(className, classBytes);
        }

        /**
         * Defines a Class object from compiled bytecode using a custom ClassLoader.
         * <p>
         * Uses TemplateClassLoader to:
         * <ul>
         *     <li>Define new template classes</li>
         *     <li>Handle class loading hierarchy properly</li>
         *     <li>Support test class loading via thread context</li>
         * </ul>
         *
         * @param className fully qualified name of the class to define
         * @param classBytes compiled bytecode for the class
         * @return the defined Class object
         * @throws LinkageError if class definition fails
         */
        private static Class<?> defineClass(String className, byte[] classBytes) {
            return templateClassLoader.defineTemplateClass(className, classBytes);
        }
    }

    /**
     * Custom ClassLoader for dynamically generated CompactMap template classes.
     * <p>
     * Provides class loading that:
     * <ul>
     *     <li>Defines new template classes from byte code</li>
     *     <li>Delegates non-template class loading to parent</li>
     *     <li>Caches template classes for reuse</li>
     *     <li>Uses thread context ClassLoader for test classes</li>
     * </ul>
     * Internal implementation detail of the template generation system.
     */
    private static final class TemplateClassLoader extends ClassLoader {
        private final Map<String, Class<?>> definedClasses = new ConcurrentHashMap<>();
        private final Map<String, ReentrantLock> classLoadLocks = new ConcurrentHashMap<>();

        private TemplateClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            // 1. Check if we already loaded it
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    // 2. Parent-first
                    c = getParent().loadClass(name);
                }
                catch (ClassNotFoundException e) {
                    // 3. If the parent can't find it, attempt local
                    c = findClass(name);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
        
        /**
         * Defines or retrieves a template class in this ClassLoader.
         * <p>
         * First attempts to find an existing template class. If not found,
         * defines a new class from the provided bytecode. This method
         * ensures template classes are only defined once.
         *
         * @param name fully qualified class name for the template
         * @param bytes bytecode for the template class
         * @return the template Class object
         * @throws LinkageError if class definition fails
         */
        private Class<?> defineTemplateClass(String name, byte[] bytes) {
            ReentrantLock lock = classLoadLocks.computeIfAbsent(name, k -> new ReentrantLock());
            lock.lock();
            try {
                // Check if already defined
                Class<?> cached = definedClasses.get(name);
                if (cached != null) {
                    return cached;
                }

                // Define new class
                Class<?> definedClass = defineClass(name, bytes, 0, bytes.length);
                definedClasses.put(name, definedClass);
                return definedClass;
            }
            finally {
                lock.unlock();
            }
        }
        
        /**
         * Finds the specified class using appropriate ClassLoader.
         * <p>
         * For non-template classes (not starting with "com.cedarsoftware.util.CompactMap$"):
         * <ul>
         *     <li>First tries thread context ClassLoader</li>
         *     <li>Falls back to parent ClassLoader</li>
         * </ul>
         * Template classes must be defined explicitly via defineTemplateClass().
         *
         * @param name fully qualified class name to find
         * @return the Class object for the specified class
         * @throws ClassNotFoundException if the class cannot be found
         */
        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            // For your "template" classes:
            if (name.startsWith("com.cedarsoftware.util.CompactMap$")) {
                // Check if we have it cached
                Class<?> cached = definedClasses.get(name);
                if (cached != null) {
                    return cached;
                }
                // If we don't, we can throw ClassNotFoundException or
                // your code might dynamically generate the class at this point.
                // Typically, you'd have a method to define it:
                //   return defineTemplateClassDynamically(name);

                throw new ClassNotFoundException("Not found: " + name);
            }

            // Fallback: if it's not a template, let the system handle it
            // (i.e. you can call super, or also do TCCL checks if you want).
            return super.findClass(name);
        }
    }

    /**
     * Comparator implementation for CompactMap key ordering.
     * <p>
     * Provides comparison logic that:
     * <ul>
     *     <li>Handles case sensitivity for String keys</li>
     *     <li>Supports natural or reverse ordering</li>
     *     <li>Maintains consistent ordering for different key types</li>
     *     <li>Properly handles null keys (always last)</li>
     * </ul>
     * Used by sorted CompactMaps and during compact array sorting.
     */
    public static class CompactMapComparator implements Comparator<Object> {
        private final boolean caseInsensitive;
        private final boolean reverse;

        public CompactMapComparator(boolean caseInsensitive, boolean reverse) {
            this.caseInsensitive = caseInsensitive;
            this.reverse = reverse;
        }

        @Override
        public int compare(Object key1, Object key2) {
            // 1. Handle nulls explicitly (nulls always last, regardless of reverse)
            if (key1 == null) {
                return (key2 == null) ? 0 : 1;
            }
            if (key2 == null) {
                return -1;
            }

            int result;
            Class<?> key1Class = key1.getClass();
            Class<?> key2Class = key2.getClass();

            // 2. Handle String comparisons with case sensitivity
            if (key1Class == String.class) {
                if (key2Class == String.class) {
                    // For strings, apply case sensitivity first
                    result = caseInsensitive
                            ? String.CASE_INSENSITIVE_ORDER.compare((String) key1, (String) key2)
                            : ((String) key1).compareTo((String) key2);
                } else {
                    // String vs non-String: use class name comparison
                    result = key1Class.getName().compareTo(key2Class.getName());
                }
            }
            // 3. Handle Comparable objects of the same type
            else if (key1Class == key2Class && key1 instanceof Comparable) {
                result = ((Comparable<Object>) key1).compareTo(key2);
            }
            // 4. Fallback to class name comparison
            else {
                result = key1Class.getName().compareTo(key2Class.getName());
            }

            // Apply reverse at the end, after all other comparisons
            return reverse ? -result : result;
        }

        @Override
        public String toString() {
            return "CompactMapComparator{caseInsensitive=" + caseInsensitive + ", reverse=" + reverse + "}";
        }
    }
}
