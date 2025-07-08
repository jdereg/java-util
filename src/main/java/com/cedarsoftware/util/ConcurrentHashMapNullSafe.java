package com.cedarsoftware.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe implementation of {@link java.util.concurrent.ConcurrentMap} that supports
 * {@code null} keys and {@code null} values by using internal sentinel objects.
 * <p>
 * {@code ConcurrentHashMapNullSafe} extends {@link AbstractConcurrentNullSafeMap} and uses a
 * {@link ConcurrentHashMap} as its backing implementation. This class retains all the advantages
 * of {@code ConcurrentHashMap} (e.g., high concurrency, thread safety, and performance) while
 * enabling safe handling of {@code null} keys and values.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Thread-safe and highly concurrent.</li>
 *   <li>Supports {@code null} keys and {@code null} values through internal sentinel objects.</li>
 *   <li>Adheres to the {@link java.util.Map} and {@link java.util.concurrent.ConcurrentMap} contracts.</li>
 *   <li>Provides constructors to control initial capacity, load factor,
 *       concurrency level, and to populate from another map.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create an empty ConcurrentHashMapNullSafe
 * ConcurrentHashMapNullSafe<String, String> map = new ConcurrentHashMapNullSafe<>();
 * map.put(null, "nullKey");
 * map.put("key", null);
 *
 * // Populate from another map
 * Map<String, String> existingMap = Map.of("a", "b", "c", "d");
 * ConcurrentHashMapNullSafe<String, String> populatedMap = new ConcurrentHashMapNullSafe<>(existingMap);
 *
 * LOG.info(map.get(null));  // Outputs: nullKey
 * LOG.info(map.get("key")); // Outputs: null
 * LOG.info(populatedMap);  // Outputs: {a=b, c=d}
 * }</pre>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 *
 * @author John DeRegnaucourt
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
 * @see ConcurrentHashMap
 * @see AbstractConcurrentNullSafeMap
 */
public final class ConcurrentHashMapNullSafe<K, V> extends AbstractConcurrentNullSafeMap<K, V> {
    /**
     * Constructs a new, empty {@code ConcurrentHashMapNullSafe} with the default initial capacity (16)
     * and load factor (0.75).
     * <p>
     * This constructor creates a thread-safe map suitable for general-purpose use, retaining the
     * concurrency properties of {@link ConcurrentHashMap} while supporting {@code null} keys and values.
     * </p>
     */
    public ConcurrentHashMapNullSafe() {
        super(new ConcurrentHashMap<>());
    }

    /**
     * Constructs a new, empty {@code ConcurrentHashMapNullSafe} with the specified initial capacity
     * and default load factor (0.75).
     *
     * @param initialCapacity the initial capacity. The implementation performs internal sizing
     *                        to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public ConcurrentHashMapNullSafe(int initialCapacity) {
        super(new ConcurrentHashMap<>(initialCapacity));
    }

    /**
     * Constructs a new, empty {@code ConcurrentHashMapNullSafe} with the specified initial capacity
     * and load factor.
     *
     * @param initialCapacity the initial capacity. The implementation performs internal sizing
     *                        to accommodate this many elements.
     * @param loadFactor      the load factor threshold, used to control resizing. Resizing may be
     *                        performed when the average number of elements per bin exceeds this threshold.
     * @throws IllegalArgumentException if the initial capacity is negative or the load factor is nonpositive
     */
    public ConcurrentHashMapNullSafe(int initialCapacity, float loadFactor) {
        super(new ConcurrentHashMap<>(initialCapacity, loadFactor));
    }

    /**
     * Constructs a new, empty {@code ConcurrentHashMapNullSafe} with the specified
     * initial capacity, load factor, and concurrency level.
     *
     * @param initialCapacity  the initial capacity of the map
     * @param loadFactor       the load factor threshold
     * @param concurrencyLevel the estimated number of concurrently updating threads
     * @throws IllegalArgumentException if the initial capacity is negative,
     *                                  or the load factor or concurrency level are nonpositive
     */
    public ConcurrentHashMapNullSafe(int initialCapacity, float loadFactor, int concurrencyLevel) {
        super(new ConcurrentHashMap<>(initialCapacity, loadFactor, concurrencyLevel));
    }

    /**
     * Constructs a new {@code ConcurrentHashMapNullSafe} with the same mappings as the specified map.
     * <p>
     * This constructor copies all mappings from the given map into the new {@code ConcurrentHashMapNullSafe}.
     * The mappings are inserted in the order returned by the source map's {@code entrySet} iterator.
     * </p>
     *
     * @param m the map whose mappings are to be placed in this map
     * @throws NullPointerException if the specified map is {@code null}
     */
    public ConcurrentHashMapNullSafe(Map<? extends K, ? extends V> m) {
        super(new ConcurrentHashMap<>(Math.max(16, (int) (m.size() / 0.75f) + 1)));
        putAll(m);
    }

    // No need to override any methods from AbstractConcurrentNullSafeMap
    // as all required functionalities are already inherited.
}
