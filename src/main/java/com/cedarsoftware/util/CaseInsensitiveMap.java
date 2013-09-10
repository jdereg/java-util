package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Useful Map that does not care about the case-sensitivity of keys
 * when the key value is a String.  Other key types can be used.
 * String keys will be treated case insensitively, yet key case will 
 * be retained.  Non-string keys will work as they normally would.
 *
 * The internal CaseInsentitiveString is never exposed externally
 * from this class. When requesting the keys or entries of this map,
 * or calling containsKey() or get() for example, use a String as you
 * normally would.  The returned Set of keys for the keySet() and
 * entrySet() APIs return the original Strings, not the internally
 * wrapped CaseInsensitiveString.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) John DeRegnaucourt
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
public class CaseInsensitiveMap<K, V> implements Map<K, V>
{
    private LinkedHashMap<K, V> map;

    public CaseInsensitiveMap()
    {
        map = new LinkedHashMap<K, V>();
    }

    public CaseInsensitiveMap(int initialCapacity)
    {
        map = new LinkedHashMap<K, V>(initialCapacity);
    }

    public CaseInsensitiveMap(Map<? extends K, ? extends V> map)
    {
        this(map.size());
        putAll(map);
    }

    public V get(Object key)
    {
        if (key instanceof String)
        {
            String keyString = (String) key;
            return map.get(new CaseInsensitiveString(keyString));
        }
        return map.get(key);
    }

    public V put(K key, V value)
    {
        if (key instanceof String)
        {	// Must remove entry because the key case can change
            CaseInsensitiveString newKey = new CaseInsensitiveString((String) key);
            if (map.containsKey(newKey))
            {
                map.remove(newKey);
            }
            return map.put((K)newKey, value);
        }
        return map.put(key, value);
    }

    public boolean containsKey(Object key)
    {
        if (key instanceof String)
        {
            String keyString = (String) key;
            return map.containsKey(new CaseInsensitiveString(keyString));
        }
        return map.containsKey(key);
    }

    public void putAll(Map<? extends K, ? extends V> m)
    {
        if (m == null)
        {
            return;
        }

        for (Map.Entry entry : m.entrySet())
        {
            put((K)entry.getKey(), (V)entry.getValue());
        }
    }

    public V remove(Object key)
    {
        if (key instanceof String)
        {
            String keyString = (String) key;
            return map.remove(new CaseInsensitiveString(keyString));
        }
        return map.remove(key);
    }

    /**
     * @return Set of Keys from the Map.  This API is implemented to allow
     * this class to unwrap the internal structure placed around String
     * keys, returning them as the original String keys, retaining their case.
     */
    public Set<K> keySet()
    {
        Set returnedKeySet = new LinkedHashSet(map.keySet().size());

        for (Object key : map.keySet())
        {
            returnedKeySet.add(key instanceof CaseInsensitiveString ? key.toString() : key);
        }

        return returnedKeySet;
    }

    /**
     * @return Set of Map.Entry for each entry in the Map.  This API is
     * implemented to allow this class to unwrap the internal structure placed
     * around String keys, returning them as the original String keys, retaining
     * their case.
     */
    public Set<Map.Entry<K, V>> entrySet()
    {
        Set<Map.Entry<K, V>> insensitiveEntrySet = map.entrySet();
        Set<Map.Entry<K, V>> returnEntrySet = new LinkedHashSet<Map.Entry<K,V>>();

        for (Map.Entry entry : insensitiveEntrySet)
        {
            if (entry.getKey() instanceof CaseInsensitiveString)
            {
                CaseInsensitiveString key = (CaseInsensitiveString) entry.getKey();
                entry = new AbstractMap.SimpleImmutableEntry<K, V>((K)key.toString(), (V)entry.getValue());
            }
            returnEntrySet.add(entry);
        }

        return returnEntrySet;
    }

    /**
     * Internal class used to wrap String keys.  This class ignores the
     * case of Strings when they are compared.
     */
    private static class CaseInsensitiveString
    {
        private final String caseInsensitiveString;

        private CaseInsensitiveString(String string)
        {
            caseInsensitiveString = string;
        }

        public String toString()
        {
            return caseInsensitiveString;
        }

        public int hashCode()
        {
            if (caseInsensitiveString == null)
            {
                return 0;
            }
            return caseInsensitiveString.toLowerCase().hashCode();
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof CaseInsensitiveString))
            {
                return false;
            }

            if (this == obj)
            {
                return true;
            }

            CaseInsensitiveString other = (CaseInsensitiveString) obj;
            if (caseInsensitiveString == null)
            {
                return other.caseInsensitiveString == null;
            }
            else
            {
                return caseInsensitiveString.equalsIgnoreCase(other.caseInsensitiveString);
            }
        }
    }

    // delegates
    public int size()
    {
        return map.size();
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public boolean equals(Object o)
    {
        return map.equals(o);
    }

    public int hashCode()
    {
        return map.hashCode();
    }

    public String toString()
    {
        return map.toString();
    }

    public void clear()
    {
        map.clear();
    }

    public boolean containsValue(Object value)
    {
        return map.containsValue(value);
    }

    public Collection<V> values()
    {
        return map.values();
    }
}
