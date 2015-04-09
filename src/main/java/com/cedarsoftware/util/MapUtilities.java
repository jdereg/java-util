package com.cedarsoftware.util;

import java.util.Map;

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
 *         http://www.apache.org/licenses/LICENSE-2.0
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
     * @exception ClassCastException if the item found is not
     * a the expected type.
     */
    public static <T> T get(Map map, String key, T def) {
        Object val = map.get(key);
        return val == null ? def : (T)val;
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
}
