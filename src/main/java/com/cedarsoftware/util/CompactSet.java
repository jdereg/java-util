package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * A memory-efficient Set implementation that internally uses {@link CompactMap}.
 * <p>
 * This implementation provides the same memory benefits as CompactMap while
 * maintaining proper Set semantics. It can be configured for:
 * <ul>
 *     <li>Case sensitivity for String elements</li>
 *     <li>Element ordering (sorted, reverse, insertion)</li>
 *     <li>Custom compact size threshold</li>
 * </ul>
 * </p>
 *
 * <h2>Creating a CompactSet</h2>
 * <pre>{@code
 * // Create a case-insensitive, sorted CompactSet
 * CompactSet<String> set = CompactSet.<String>builder()
 *     .caseSensitive(false)
 *     .sortedOrder()
 *     .compactSize(80)
 *     .build();
 *
 * // Create a CompactSet with insertion ordering
 * CompactSet<String> ordered = CompactSet.<String>builder()
 *     .insertionOrder()
 *     .build();
 * }</pre>
 *
 * @param <E> the type of elements maintained by this set
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
public class CompactSet<E> implements Set<E> {

    /**
     * A special marker object stored in the map for each key.
     * Using a single static instance to avoid per-entry overhead.
     */
    private static final Object PRESENT = new Object();

    /**
     * The one and only data structure: a CompactMap whose keys represent the set elements.
     */
    private final CompactMap<E, Object> map;

    /**
     * Constructs an empty CompactSet with the default configuration (i.e., default CompactMap).
     * <p>
     * This uses the no-arg CompactMap constructor, which typically yields:
     * <ul>
     *   <li>caseSensitive = true</li>
     *   <li>compactSize = 70</li>
     *   <li>unordered</li>
     * </ul>
     * <p>
     * If you want custom config, use the {@link Builder} instead.
     *
     * @throws IllegalStateException if {@link #compactSize()} returns a value less than 2
     */
    public CompactSet() {
        // Utilize the overridden compactSize() from subclasses
        CompactMap<E, Object> defaultMap = CompactMap.<E, Object>builder()
                .compactSize(this.compactSize())
                .caseSensitive(!isCaseInsensitive())
                .build();

        if (defaultMap.compactSize() < 2) {
            throw new IllegalStateException("compactSize() must be >= 2");
        }

        this.map = defaultMap;
    }

    /**
     * Constructs a CompactSet with a pre-existing CompactMap (usually from a builder).
     *
     * @param map the underlying CompactMap to store elements
     */
    protected CompactSet(CompactMap<E, Object> map) {
        if (map.compactSize() < 2) {
            throw new IllegalStateException("compactSize() must be >= 2");
        }
        this.map = map;
    }

    /**
     * Constructs a CompactSet containing the elements of the specified collection,
     * using the default CompactMap configuration.
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     */
    public CompactSet(Collection<? extends E> c) {
        this();
        addAll(c);
    }

    /* ----------------------------------------------------------------- */
    /*                Implementation of Set<E> methods                   */
    /* ----------------------------------------------------------------- */

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(E e) {
        // If map.put(e, PRESENT) returns null, the key was not in the map
        // => we effectively added a new element => return true
        // else we replaced an existing key => return false (no change)
        return map.put(e, PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        // If map.remove(o) != null, the key existed => return true
        // else the key wasn't there => return false
        return map.remove(o) != null;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        // We can just leverage map.keySet().containsAll(...)
        return map.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        // Again, rely on keySet() to do the heavy lifting
        return map.keySet().retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return map.keySet().removeAll(c);
    }

    @Override
    public Iterator<E> iterator() {
        // We can simply return map.keySet().iterator()
        return map.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T> T[] toArray(T[] a) {
        return map.keySet().toArray(a);
    }

    /* ----------------------------------------------------------------- */
    /*              Object overrides (equals, hashCode, etc.)            */
    /* ----------------------------------------------------------------- */

    @Override
    public boolean equals(Object o) {
        // Let keySet() handle equality checks for us
        return map.keySet().equals(o);
    }

    @Override
    public int hashCode() {
        return map.keySet().hashCode();
    }

    @Override
    public String toString() {
        return map.keySet().toString();
    }

    /**
     * Returns a builder for creating customized CompactSet instances.
     *
     * @param <E> the type of elements in the set
     * @return a new Builder instance
     */
    public static <E> Builder<E> builder() {
        return new Builder<>();
    }

    /**
     * Builder for creating CompactSet instances with custom configurations.
     * <p>
     * Internally, the builder configures a {@link CompactMap} (with &lt;E, Object&gt;).
     */
    public static final class Builder<E> {
        private final CompactMap.Builder<E, Object> mapBuilder;

        private Builder() {
            // Build a map for our set
            this.mapBuilder = CompactMap.builder();
        }

        /**
         * Sets whether String elements should be compared case-sensitively.
         * @param caseSensitive if false, do case-insensitive compares
         */
        public Builder<E> caseSensitive(boolean caseSensitive) {
            mapBuilder.caseSensitive(caseSensitive);
            return this;
        }

        /**
         * Sets the maximum size for compact array storage.
         */
        public Builder<E> compactSize(int size) {
            mapBuilder.compactSize(size);
            return this;
        }

        /**
         * Configures the set to maintain elements in natural sorted order.
         * <p>Requires elements to be {@link Comparable}</p>
         */
        public Builder<E> sortedOrder() {
            mapBuilder.sortedOrder();
            return this;
        }

        /**
         * Configures the set to maintain elements in reverse sorted order.
         * <p>Requires elements to be {@link Comparable}</p>
         */
        public Builder<E> reverseOrder() {
            mapBuilder.reverseOrder();
            return this;
        }

        /**
         * Configures the set to maintain elements in insertion order.
         */
        public Builder<E> insertionOrder() {
            mapBuilder.insertionOrder();
            return this;
        }

        /**
         * Configures the set to maintain elements in no specific order, like a HashSet.
         */
        public Builder<E> noOrder() {
            mapBuilder.noOrder();
            return this;
        }

        /**
         * Creates a new CompactSet with the configured options.
         */
        public CompactSet<E> build() {
            // Build the underlying map, then wrap it in a new CompactSet
            CompactMap<E, Object> builtMap = mapBuilder.build();
            return new CompactSet<>(builtMap);
        }
    }

    /**
     * @deprecated Use {@link Builder#compactSize(int)} instead.
     * Maintained for backward compatibility with existing subclasses.
     */
    @Deprecated
    protected int compactSize() {
        // Typically 70 is the default. You can override as needed.
        return 70;
    }

    /**
     * @deprecated Use {@link Builder#caseSensitive(boolean)} instead.
     * Maintained for backward compatibility with existing subclasses.
     */
    @Deprecated
    protected boolean isCaseInsensitive() {
        return false;  // default to case-sensitive, for legacy
    }

    /**
     * @deprecated Legacy method. Subclasses should configure CompactSet using the builder pattern instead.
     * Maintained for backward compatibility with existing subclasses.
     */
    @Deprecated
    protected Set<E> getNewSet() {
        return new LinkedHashSet<>(2);
    }

    /**
     * Returns the configuration settings of this CompactSet.
     * <p>
     * The returned map contains the following keys:
     * <ul>
     *   <li>{@link CompactMap#COMPACT_SIZE} - Maximum size before switching to backing map</li>
     *   <li>{@link CompactMap#CASE_SENSITIVE} - Whether string elements are case-sensitive</li>
     *   <li>{@link CompactMap#ORDERING} - Element ordering strategy</li>
     * </ul>
     * </p>
     *
     * @return an unmodifiable map containing the configuration settings
     */
    public Map<String, Object> getConfig() {
        // Get the underlying map's config but filter out map-specific details
        Map<String, Object> mapConfig = map.getConfig();

        // Create a new map with only the Set-relevant configuration
        Map<String, Object> setConfig = new LinkedHashMap<>();
        setConfig.put(CompactMap.COMPACT_SIZE, mapConfig.get(CompactMap.COMPACT_SIZE));
        setConfig.put(CompactMap.CASE_SENSITIVE, mapConfig.get(CompactMap.CASE_SENSITIVE));
        setConfig.put(CompactMap.ORDERING, mapConfig.get(CompactMap.ORDERING));

        return Collections.unmodifiableMap(setConfig);
    }

    public CompactSet<E> withConfig(Map<String, Object> config) {
        Convention.throwIfNull(config, "config cannot be null");

        // Start with a builder
        Builder<E> builder = CompactSet.<E>builder();

        // Get current configuration from the underlying map
        Map<String, Object> currentConfig = map.getConfig();

        // Handle compactSize with proper priority
        Integer configCompactSize = (Integer) config.get(CompactMap.COMPACT_SIZE);
        Integer currentCompactSize = (Integer) currentConfig.get(CompactMap.COMPACT_SIZE);
        int compactSizeToUse = (configCompactSize != null) ? configCompactSize : currentCompactSize;
        builder.compactSize(compactSizeToUse);

        // Handle caseSensitive with proper priority
        Boolean configCaseSensitive = (Boolean) config.get(CompactMap.CASE_SENSITIVE);
        Boolean currentCaseSensitive = (Boolean) currentConfig.get(CompactMap.CASE_SENSITIVE);
        boolean caseSensitiveToUse = (configCaseSensitive != null) ? configCaseSensitive : currentCaseSensitive;
        builder.caseSensitive(caseSensitiveToUse);

        // Handle ordering with proper priority
        String configOrdering = (String) config.get(CompactMap.ORDERING);
        String currentOrdering = (String) currentConfig.get(CompactMap.ORDERING);
        String orderingToUse = (configOrdering != null) ? configOrdering : currentOrdering;

        // Apply the determined ordering
        applyOrdering(builder, orderingToUse);

        // Build and populate the new set
        CompactSet<E> newSet = builder.build();
        newSet.addAll(this);
        return newSet;
    }

    private void applyOrdering(Builder<E> builder, String ordering) {
        if (ordering == null) {
            builder.noOrder(); // Default to no order if somehow null
            return;
        }

        switch (ordering) {
            case CompactMap.SORTED:
                builder.sortedOrder();
                break;
            case CompactMap.REVERSE:
                builder.reverseOrder();
                break;
            case CompactMap.INSERTION:
                builder.insertionOrder();
                break;
            default:
                builder.noOrder();
        }
    }
}