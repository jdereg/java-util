package com.cedarsoftware.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrentHashMapNullSafe is a thread-safe implementation of ConcurrentMap
 * that allows null keys and null values by using sentinel objects internally.
 * <br>
 * @param <K> The type of keys maintained by this map
 * @param <V> The type of mapped values
 * <br>
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
public class ConcurrentHashMapNullSafe<K, V> extends AbstractConcurrentNullSafeMap<K, V> {
    /**
     * Constructs a new, empty ConcurrentHashMapNullSafe with default initial capacity (16) and load factor (0.75).
     */
    public ConcurrentHashMapNullSafe() {
        super(new ConcurrentHashMap<>());
    }

    /**
     * Constructs a new, empty ConcurrentHashMapNullSafe with the specified initial capacity and default load factor (0.75).
     *
     * @param initialCapacity the initial capacity. The implementation performs internal sizing
     *                        to accommodate this many elements.
     * @throws IllegalArgumentException if the initial capacity is negative.
     */
    public ConcurrentHashMapNullSafe(int initialCapacity) {
        super(new ConcurrentHashMap<>(initialCapacity));
    }

    /**
     * Constructs a new, empty ConcurrentHashMapNullSafe with the specified initial capacity and load factor.
     *
     * @param initialCapacity the initial capacity. The implementation
     *                        performs internal sizing to accommodate this many elements.
     * @param loadFactor      the load factor threshold, used to control resizing.
     *                        Resizing may be performed when the average number of elements per
     *                        bin exceeds this threshold.
     * @throws IllegalArgumentException if the initial capacity is negative or the load factor is nonpositive
     */
    public ConcurrentHashMapNullSafe(int initialCapacity, float loadFactor) {
        super(new ConcurrentHashMap<>(initialCapacity, loadFactor));
    }

    /**
     * Constructs a new ConcurrentHashMapNullSafe with the same mappings as the specified map.
     *
     * @param m the map whose mappings are to be placed in this map
     * @throws NullPointerException if the specified map is null
     */
    public ConcurrentHashMapNullSafe(Map<? extends K, ? extends V> m) {
        super(new ConcurrentHashMap<>());
        putAll(m);
    }

    // No need to override any methods from AbstractConcurrentNullSafeMap
    // as all required functionalities are already inherited.
}
