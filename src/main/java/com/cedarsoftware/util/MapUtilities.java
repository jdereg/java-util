package com.cedarsoftware.util;

import java.util.Map;

/**
 * Useful String utilities for common tasks
 *
 * @author John DeRegnaucourt (jdereg@gmail.com) & Ken Partlow
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class MapUtilities {

    /**
     * <p>Constructor is declared private since all methods are static.</p>
     */
    private MapUtilities()
    {
    }

    /**
     * Retrieves a String from a map by key
     *
     * @param map Map to retrieve item from
     * @param key
     * @param def value to return if item was not found.
     * @return Returns a string value that was found at the location key.
     * If the item is null then the def value is sent back.
     * If the item is not a String, an exception is thrown.
     * @exception ClassCastException if the item found is not
     * a String.
     */
    public static String getString(Map map, Object key, String def)
    {
        Object val = map.get(key);

        if (val == null)
        {
            return def;
        }

        if (val instanceof String)
        {
            return (String)val;
        }

        throw new ClassCastException(String.format("Expected String for key '%s' but instead found: %s", key, ReflectionUtils.getClassName(val)));
    }

    /**
     * Retrieves a String from a map by key
     *
     * @param map Map to retrieve item from
     * @param key
     * @return Returns a string value that was found at the location key.
     * If the item is null then the null is sent back.
     * If the item is not a String, an exception is thrown.
     * @exception ClassCastException if the item found is not
     * a String.
     */
    public static String get(Map map, Object key)
    {
        return getString(map, key, null);
    }

    /**
     * Retrieves a Long from a map by key
     *
     * @param map Map to retrieve item from
     * @param key
     * @param def value to return if item was not found.
     * @return Returns a string value that was found at the location key.
     * If the item is null then the def value is sent back.
     * If the item is not a String, an exception is thrown.
     * @exception ClassCastException if the item found is not
     * a String.
     */
    public static Long getLong(Map map, String key, Long def)
    {
        Object val = map.get(key);

        if (val == null)
        {
            return def;
        }

        if (val instanceof Long)
        {
            return (Long)val;
        }

        throw new ClassCastException("Expected 'Long' for key '" + key + "' but instead found: " + ReflectionUtils.getClassName(val));
    }

    /**
     * Retrieves a Long from a map by key
     *
     * @param map Map to retrieve item from
     * @param key
     * @return Returns a string value that was found at the location key.
     * If the item is null then the null is sent back.
     * If the item is not a String, an exception is thrown.
     * @exception ClassCastException if the item found is not
     * a String.
     */
    public static Long getLong(Map map, String key)
    {
        return getLong(map, key, null);
    }
/*
    public static Boolean getBoolean(Map map, String key)
    {
        Object val = map.get(key);
        if (val instanceof Boolean)
        {
            return (Boolean) val;
        }
        if (val == null)
        {
            return Boolean.FALSE;
        }
        String clazz = val.getClass().getName();
        throw new IllegalArgumentException("Expected 'Boolean' for key '" + key + "' but instead found: " + clazz);
    }
    */
}
