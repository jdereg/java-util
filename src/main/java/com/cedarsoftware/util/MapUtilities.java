package com.cedarsoftware.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Usefule utilities for Maps
 *
 * @author Kenneth Partlow
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
public class MapUtilities
{
    /**
     * <p>Constructor is declared private since all methods are static.</p>
     */
    private MapUtilities()
    {
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
    public static <T> T get(Map<?, T> map, Object key, T def)
    {
        T val = map.get(key);
        return val == null ? def : val;
    }

    /**
     * Retrieves a value from a map by key, if value is not found by the given key throws a 'Throwable.'
     * This version allows the value associated to the key to be null, and it still works.  In other words,
     * if the passed in key is within the map, this method will return whatever is associated to the key, including
     * null.
     * @param map Map to retrieve item from
     * @param key the key whose associated value is to be returned
     * @param throwable
     * @param <T> Throwable passed in to be thrown *if* the passed in key is not within the passed in map.
     * @return the value associated to the passed in key from the passed in map, otherwise throw the passed in exception.
     */
    public static <T extends Throwable> Object getOrThrow(Map<?, ?> map, Object key, T throwable) throws T
    {
        if (map == null)
        {
            throw new NullPointerException("Map parameter cannot be null");
        }

        if (throwable == null)
        {
            throw new NullPointerException("Throwable object cannot be null");
        }

        if (map.containsKey(key))
        {
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
    public static boolean isEmpty(Map map) {
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
     * For JDK1.8 support.  Remove this and change to Map.of() for JDK11+
     */
    public static <K, V> Map<K, V> mapOf()
    {
        return Collections.unmodifiableMap(new LinkedHashMap<>());
    }

    public static <K, V> Map<K, V> mapOf(K k, V v)
    {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k, v);
        return Collections.unmodifiableMap(map);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2)
    {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        return Collections.unmodifiableMap(map);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3)
    {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return Collections.unmodifiableMap(map);
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4)
    {
        Map<K, V> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        map.put(k4, v4);
        return Collections.unmodifiableMap(map);
    }
}
