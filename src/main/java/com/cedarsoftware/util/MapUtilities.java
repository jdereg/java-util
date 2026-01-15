package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;

/**
 * Useful utilities for Maps
 *
 * @author Ken Partlow (kpartlow@gmail.com)
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
public class MapUtilities {
    private static final int MAX_ENTRIES = 10;

    private MapUtilities() {
    }

    /**
     * Retrieves a value from a map by key
     *
     * @param map Map to retrieve item from
     * @param key the key whose associated value is to be returned
     * @param def value to return if item was not found.
     * @return Returns a string value that was found at the location key.
     * If the item is null then the def value is sent back.
     * If the item is not the expected type, an exception is thrown.
     */
    public static <T> T get(Map<?, T> map, Object key, T def) {
        T val = map.get(key);
        return val == null ? def : val;
    }

    /**
     * Retrieves a value from a map by key, if value is not found by the given key throws a 'Throwable.'
     * This version allows the value associated to the key to be null, and it still works.  In other words,
     * if the passed in key is within the map, this method will return whatever is associated to the key, including
     * null.
     *
     * @param map       Map to retrieve item from
     * @param key       the key whose associated value is to be returned
     * @param <T>       Throwable passed in to be thrown *if* the passed in key is not within the passed in map.
     * @return the value associated to the passed in key from the passed in map, otherwise throw the passed in exception.
     */
    public static <T extends Throwable> Object getOrThrow(Map<?, ?> map, Object key, T throwable) throws T {
        if (map == null) {
            throw new NullPointerException("Map parameter cannot be null");
        }

        if (throwable == null) {
            throw new NullPointerException("Throwable object cannot be null");
        }

        if (map.containsKey(key)) {
            return map.get(key);
        }
        throw throwable;
    }

    /**
     * Returns null safe isEmpty check for Map
     *
     * @param map Map to check, can be null
     * @return Returns true if map is empty or null
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * Duplicate a map of Set to Class, possibly as unmodifiable
     *
     * @param other        map to duplicate
     * @param unmodifiable will the result be unmodifiable
     * @return duplicated map
     */
    public static <T> Map<Class<?>, Set<T>> dupe(Map<Class<?>, Set<T>> other, boolean unmodifiable) {
        final Map<Class<?>, Set<T>> newItemsAssocToClass = new LinkedHashMap<>();
        for (Map.Entry<Class<?>, Set<T>> entry : other.entrySet()) {
            final Set<T> itemsAssocToClass = new LinkedHashSet<>(entry.getValue());
            if (unmodifiable) {
                newItemsAssocToClass.computeIfAbsent(entry.getKey(), k -> Collections.unmodifiableSet(itemsAssocToClass));
            } else {
                newItemsAssocToClass.computeIfAbsent(entry.getKey(), k -> itemsAssocToClass);
            }
        }
        if (unmodifiable) {
            return Collections.unmodifiableMap(newItemsAssocToClass);
        } else {
            return newItemsAssocToClass;
        }
    }

    //  Keeping next two methods in case we need to make certain sets unmodifiable still.
    /**
     * Deep clone a map whose values are {@link Set Sets}.
     *
     * @param original   map to clone
     * @param immutable  if {@code true}, return unmodifiable sets and map
     * @param <T>        key type
     * @param <V>        set element type
     * @return cloned map of sets, optionally immutable
     */
    public static <T, V> Map<T, Set<V>> cloneMapOfSets(final Map<T, Set<V>> original, final boolean immutable) {
        final Map<T, Set<V>> result = new HashMap<>();

        for (Map.Entry<T, Set<V>> entry : original.entrySet()) {
            final T key = entry.getKey();
            final Set<V> value = entry.getValue();

            final Set<V> clonedSet = immutable
                    ? Collections.unmodifiableSet(value)
                    : new HashSet<>(value);

            result.put(key, clonedSet);
        }

        return immutable ? Collections.unmodifiableMap(result) : result;
    }

    /**
     * Deep clone a map whose values are themselves maps.
     *
     * @param original  map to clone
     * @param immutable if {@code true}, return unmodifiable maps
     * @param <T>       outer key type
     * @param <U>       inner key type
     * @param <V>       inner value type
     * @return cloned map of maps, optionally immutable
     */
    public static <T, U, V> Map<T, Map<U, V>> cloneMapOfMaps(final Map<T, Map<U, V>> original, final boolean immutable) {
        final Map<T, Map<U, V>> result = new LinkedHashMap<>();

        for (Map.Entry<T, Map<U, V>> entry : original.entrySet()) {
            final T key = entry.getKey();
            final Map<U, V> value = entry.getValue();

            final Map<U, V> clonedMap = immutable
                    ? Collections.unmodifiableMap(value)
                    : new LinkedHashMap<>(value);

            result.put(key, clonedMap);
        }

        return immutable ? Collections.unmodifiableMap(result) : result;
    }

    /**
     * Returns a string representation of the provided map.
     * <p>
     * The string representation consists of a list of key-value mappings in the order returned by the map's
     * {@code entrySet} iterator, enclosed in braces ({@code "{}"}). Adjacent mappings are separated by the characters
     * {@code ", "} (comma and space). Each key-value mapping is rendered as the key followed by an equals sign
     * ({@code "="}) followed by the associated value.
     * </p>
     *
     * @param map the map to represent as a string
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @return a string representation of the provided map
     */
    public static <K, V> String mapToString(Map<K, V> map) {
        Iterator<Map.Entry<K, V>> i = map.entrySet().iterator();
        if (!i.hasNext()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (; ; ) {
            Map.Entry<K, V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key == map ? "(this Map)" : key);
            sb.append('=');
            sb.append(value == map ? "(this Map)" : value);
            if (!i.hasNext()) {
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
        }
    }

    /**
     * For JDK1.8 support.  Remove this and change to Map.of() for JDK11+
     */
    /**
     * Creates an immutable map with the specified key-value pairs, limited to 10 entries.
     * <p>
     * If more than 10 key-value pairs are provided, an {@link IllegalArgumentException} is thrown.
     * </p>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param keyValues an even number of key-value pairs
     * @return an immutable map containing the specified key-value pairs
     * @throws IllegalArgumentException if the number of arguments is odd or exceeds 10 entries
     * @throws NullPointerException if any key or value in the map is {@code null}
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mapOf(Object... keyValues) {
        if (keyValues == null || keyValues.length == 0) {
            return Collections.unmodifiableMap(new LinkedHashMap<>());
        }

        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of arguments; keys and values must be paired.");
        }

        if (keyValues.length / 2 > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many entries; maximum is " + MAX_ENTRIES);
        }

        Map<K, V> map = new LinkedHashMap<>(keyValues.length / 2);
        for (int i = 0; i < keyValues.length; i += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) keyValues[i];
            @SuppressWarnings("unchecked")
            V value = (V) keyValues[i + 1];
            
            map.put(key, value);
        }

        return Collections.unmodifiableMap(map);
    }

    /**
     * Creates an immutable map from a series of {@link Map.Entry} objects.
     * <p>
     * This method is intended for use with larger maps where more than 10 entries are needed.
     * </p>
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param entries the entries to be included in the map
     * @return an immutable map containing the specified entries
     * @throws NullPointerException if any entry, key, or value is {@code null}
     */
    @SafeVarargs
    public static <K, V> Map<K, V> mapOfEntries(Map.Entry<K, V>... entries) {
        if (entries == null || entries.length == 0) {
            return Collections.unmodifiableMap(new LinkedHashMap<>());
        }

        Map<K, V> map = new LinkedHashMap<>(entries.length);
        for (Map.Entry<K, V> entry : entries) {
            if (entry == null) {
                throw new NullPointerException("Entries must not be null.");
            }
            map.put(entry.getKey(), entry.getValue());
        }

        return Collections.unmodifiableMap(map);
    }

    /**
     * Gets the underlying map instance, traversing through any wrapper maps.
     * <p>
     * This method unwraps common map wrappers from both the JDK and java-util to find
     * the innermost backing map. It properly handles nested wrappers and detects cycles.
     * </p>
     *
     * @param map The map to unwrap
     * @return The innermost backing map, or the original map if not wrapped
     * @throws IllegalArgumentException if a cycle is detected in the map structure
     */
    private static Map<?, ?> getUnderlyingMap(Map<?, ?> map) {
        if (map == null) {
            return null;
        }

        // Use identity semantics to avoid false cycle detection when wrapper
        // maps implement equals() by delegating to their wrapped map.
        Set<Map<?, ?>> seen = new IdentitySet<>();
        Map<?, ?> current = map;
        List<String> path = new ArrayList<>();
        path.add(current.getClass().getSimpleName());

        while (true) {
            if (!seen.add(current)) {
                throw new IllegalArgumentException(
                        "Circular map structure detected: " + String.join(" -> ", path));
            }

            if (current instanceof CompactMap) {
                CompactMap<?, ?> cMap = (CompactMap<?, ?>) current;
                if (cMap.getLogicalValueType() == CompactMap.LogicalValueType.MAP) {
                    current = (Map<?, ?>) cMap.val;  // val is package-private, accessible from MapUtilities
                    path.add(current.getClass().getSimpleName());
                    continue;
                }
                return current;
            }

            if (current instanceof CaseInsensitiveMap) {
                current = ((CaseInsensitiveMap<?, ?>) current).getWrappedMap();
                path.add(current.getClass().getSimpleName());
                continue;
            }

            if (current instanceof TrackingMap) {
                current = ((TrackingMap<?, ?>) current).getWrappedMap();
                path.add(current.getClass().getSimpleName());
                continue;
            }

            return current;
        }
    }

    /**
     * Gets a string representation of a map's structure, showing all wrapper layers.
     * <p>
     * This method is useful for debugging and understanding complex map structures.
     * It shows the chain of map wrappers and their configurations, including:
     * <ul>
     *   <li>CompactMap state (empty, array, single entry) and ordering</li>
     *   <li>CaseInsensitiveMap wrappers</li>
     *   <li>TrackingMap wrappers</li>
     *   <li>NavigableMap implementations</li>
     *   <li>Circular references in the structure</li>
     * </ul>
     * </p>
     *
     * @param map The map to analyze
     * @return A string showing the map's complete structure
     */
    static String getMapStructureString(Map<?, ?> map) {
        if (map == null) return "null";

        List<String> structure = new ArrayList<>();
        // Use identity semantics so wrapper maps that compare equal to their
        // wrapped map do not trigger false cycles.
        Set<Map<?, ?>> seen = new IdentitySet<>();
        Map<?, ?> current = map;

        while (true) {
            if (!seen.add(current)) {
                structure.add("CYCLE -> " + current.getClass().getSimpleName());
                break;
            }

            if (current instanceof CompactMap) {
                CompactMap<?, ?> cMap = (CompactMap<?, ?>) current;
                structure.add("CompactMap(" + cMap.getOrdering() + ")");

                CompactMap.LogicalValueType valueType = cMap.getLogicalValueType();
                if (valueType == CompactMap.LogicalValueType.MAP) {
                    current = (Map<?, ?>) cMap.val;
                    continue;
                }

                structure.add("[" + valueType.name() + "]");
                break;
            }

            if (current instanceof CaseInsensitiveMap) {
                structure.add("CaseInsensitiveMap");
                current = ((CaseInsensitiveMap<?, ?>) current).getWrappedMap();
                continue;
            }

            if (current instanceof TrackingMap) {
                structure.add("TrackingMap");
                current = ((TrackingMap<?, ?>) current).getWrappedMap();
                continue;
            }

            structure.add(current.getClass().getSimpleName() +
                    (current instanceof NavigableMap ? "(NavigableMap)" : ""));
            break;
        }

        return String.join(" -> ", structure);
    }

    /**
     * Analyzes a map to determine its logical ordering behavior.
     * <p>
     * This method examines both the map type and its wrapper structure to determine
     * the actual ordering behavior. It properly handles:
     * <ul>
     *   <li>CompactMap with various ordering settings</li>
     *   <li>CaseInsensitiveMap with different backing maps</li>
     *   <li>TrackingMap wrappers</li>
     *   <li>Standard JDK maps (LinkedHashMap, TreeMap, etc.)</li>
     *   <li>Navigable and Concurrent maps</li>
     * </ul>
     * </p>
     *
     * @param map The map to analyze
     * @return The detected ordering type (one of CompactMap.UNORDERED, INSERTION, SORTED, or REVERSE)
     * @throws IllegalArgumentException if the map structure contains cycles
     */
    static String detectMapOrdering(Map<?, ?> map) {
        if (map == null) return CompactMap.UNORDERED;

        try {
            if (map instanceof CompactMap) {
                return ((CompactMap<?, ?>)map).getOrdering();
            }

            Map<?, ?> underlyingMap = getUnderlyingMap(map);

            if (underlyingMap instanceof CompactMap) {
                return ((CompactMap<?, ?>)underlyingMap).getOrdering();
            }

            if (underlyingMap instanceof SortedMap) {
                return CompactMap.SORTED;
            }

            if (underlyingMap instanceof LinkedHashMap || underlyingMap instanceof EnumMap) {
                return CompactMap.INSERTION;
            }

            return CompactMap.UNORDERED;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Cannot determine map ordering: " + e.getMessage());
        }
    }
}
