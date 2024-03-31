package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Useful Map that does not care about the case-sensitivity of keys
 * when the key value is a String.  Other key types can be used.
 * String keys will be treated case insensitively, yet key case will
 * be retained.  Non-string keys will work as they normally would.
 * <p>
 * The internal CaseInsensitiveString is never exposed externally
 * from this class. When requesting the keys or entries of this map,
 * or calling containsKey() or get() for example, use a String as you
 * normally would.  The returned Set of keys for the keySet() and
 * entrySet() APIs return the original Strings, not the internally
 * wrapped CaseInsensitiveString.
 *
 * As an added benefit, .keySet() returns a case-insenstive
 * Set, however, again, the contents of the entries are actual Strings.
 * Similarly, .entrySet() returns a case-insensitive entry set, such that
 * .getKey() on the entry is case insensitive when compared, but the
 * returned key is a String.
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
public class CaseInsensitiveMap<K, V> implements Map<K, V>
{
    private final Map<K, V> map;

    public CaseInsensitiveMap()
    {
        map = new LinkedHashMap<>();
    }

    /**
     * Use the constructor that takes two (2) Maps.  The first Map may/may not contain any items to add,
     * and the second Map is an empty Map configured the way you want it to be (load factor, capacity)
     * and the type of Map you want.  This Map is used by CaseInsenstiveMap internally to store entries.
     */
    public CaseInsensitiveMap(int initialCapacity)
    {
        map = new LinkedHashMap<>(initialCapacity);
    }

    /**
     * Use the constructor that takes two (2) Maps.  The first Map may/may not contain any items to add,
     * and the second Map is an empty Map configured the way you want it to be (load factor, capacity)
     * and the type of Map you want.  This Map is used by CaseInsenstiveMap internally to store entries.
     */
    public CaseInsensitiveMap(int initialCapacity, float loadFactor)
    {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    /**
     * Wrap the passed in Map with a CaseInsensitiveMap, allowing other Map types like
     * TreeMap, ConcurrentHashMap, etc. to be case insensitive.  The caller supplies
     * the actual Map instance that will back the CaseInsensitiveMap.;
     * @param source existing Map to supply the entries.
     * @param mapInstance empty new Map to use.  This lets you decide what Map to use to back the CaseInsensitiveMap.
     */
    public CaseInsensitiveMap(Map<K, V> source, Map<K, V> mapInstance)
    {
        map = copy(source, mapInstance);
    }

    /**
     * Wrap the passed in Map with a CaseInsensitiveMap, allowing other Map types like
     * TreeMap, ConcurrentHashMap, etc. to be case insensitive.
     * @param m Map to wrap.
     */
    public CaseInsensitiveMap(Map<K, V> m)
    {
        if (m instanceof TreeMap)
        {
            map = copy(m, new TreeMap<>());
        }
        else if (m instanceof LinkedHashMap)
        {
            map = copy(m, new LinkedHashMap<>(m.size()));
        }
        else if (m instanceof ConcurrentSkipListMap)
        {
            map = copy(m, new ConcurrentSkipListMap<>());
        }
        else if (m instanceof ConcurrentMap)
        {
            map = copy(m, new ConcurrentHashMap<>(m.size()));
        }
        else if (m instanceof WeakHashMap)
        {
            map = copy(m, new WeakHashMap<>(m.size()));
        }
        else if (m instanceof HashMap)
        {
            map = copy(m, new HashMap<>(m.size()));
        }
        else
        {
            map = copy(m, new LinkedHashMap<>(m.size()));
        }
    }

    @SuppressWarnings("unchecked")
    protected Map<K, V> copy(Map<K, V> source, Map<K, V> dest)
    {
        for (Entry<K, V> entry : source.entrySet())
        {
            // Get key from Entry, leaving it in it's original state (in case the key is a CaseInsensitiveString)
            Object key;
            if (isCaseInsenstiveEntry(entry))
            {
                key = ((CaseInsensitiveEntry)entry).getOriginalKey();
            }
            else
            {
                key = entry.getKey();
            }

            // Wrap any String keys with a CaseInsensitiveString.  Keys that were already CaseInsensitiveStrings will
            // remain as such.
            K altKey;
            if (key instanceof String)
            {
                altKey = (K) new CaseInsensitiveString((String)key);
            }
            else
            {
                altKey = (K)key;
            }

            dest.put(altKey, entry.getValue());
        }
        return dest;
    }

    private boolean isCaseInsenstiveEntry(Object o)
    {
        return CaseInsensitiveEntry.class.isInstance(o);
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

    public boolean containsKey(Object key)
    {
        if (key instanceof String)
        {
            String keyString = (String) key;
            return map.containsKey(new CaseInsensitiveString(keyString));
        }
        return map.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    public V put(K key, V value)
    {
        if (key instanceof String)
        {
            final CaseInsensitiveString newKey = new CaseInsensitiveString((String) key);
            return map.put((K) newKey, value);
        }
        return map.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public Object putObject(Object key, Object value)
    {   // not calling put() to save a little speed.
        if (key instanceof String)
        {   
            final CaseInsensitiveString newKey = new CaseInsensitiveString((String) key);
            return map.put((K) newKey, (V)value);
        }
        return map.put((K)key, (V)value);
    }

    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends K, ? extends V> m)
    {
        if (MapUtilities.isEmpty(m))
        {
            return;
        }

        for (Entry<? extends K, ? extends V> entry : m.entrySet())
        {
            if (isCaseInsenstiveEntry(entry))
            {
                CaseInsensitiveEntry ciEntry = (CaseInsensitiveEntry) entry;
                put(ciEntry.getOriginalKey(), entry.getValue());
            }
            else
            {
                put(entry.getKey(), entry.getValue());
            }
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

    // delegates
    public int size()
    {
        return map.size();
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (!(other instanceof Map)) return false;

        Map<?, ?> that = (Map<?, ?>) other;
        if (that.size() != size())
        {
            return false;
        }

        for (Entry<?, ?> entry : that.entrySet())
        {
            final Object thatKey = entry.getKey();
            if (!containsKey(thatKey))
            {
                return false;
            }

            Object thatValue = entry.getValue();
            Object thisValue = get(thatKey);
            if (!Objects.equals(thisValue, thatValue))
            {
                return false;
            }
        }
        return true;
    }

    public int hashCode()
    {
        int h = 0;
        for (Entry<K, V> entry : map.entrySet())
        {
            Object key = entry.getKey();
            int hKey = key == null ? 0 : key.hashCode();
            Object value = entry.getValue();
            int hValue = value == null ? 0 : value.hashCode();
            h += hKey ^ hValue;
        }
        return h;
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

    public Map minus(Object removeMe)
    {
        throw new UnsupportedOperationException("Unsupported operation [minus] or [-] between Maps.  Use removeAll() or retainAll() instead.");
    }

    public Map plus(Object right)
    {
        throw new UnsupportedOperationException("Unsupported operation [plus] or [+] between Maps.  Use putAll() instead.");
    }

    public Map<K, V> getWrappedMap()
    {
        return map;
    }

    /**
     * Returns a Set view of the keys contained in this map.
     * The set is backed by the map, so changes to the map are
     * reflected in the set, and vice-versa.  If the map is modified
     * while an iteration over the set is in progress (except through
     * the iterator's own <b>remove</b> operation), the results of
     * the iteration are undefined.  The set supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <b>Iterator.remove</b>, <b>Set.remove</b>,
     * <b>removeAll</b>, <b>retainAll</b>, and <b>clear</b>
     * operations.  It does not support the <b>add</b> or <b>addAll</b>
     * operations.
     */
    public Set<K> keySet()
    {
        return new AbstractSet<K>()
        {
            Iterator iter;

            public boolean contains(Object o) { return CaseInsensitiveMap.this.containsKey(o); }

            public boolean remove(Object o)
            {
                final int size = map.size();
                CaseInsensitiveMap.this.remove(o);
                return map.size() != size;
            }

            public boolean removeAll(Collection c)
            {
                int size = map.size();
                for (Object o : c)
                {
                    CaseInsensitiveMap.this.remove(o);
                }
                return map.size() != size;
            }

            @SuppressWarnings("unchecked")
            public boolean retainAll(Collection<?> c)
            {
                Map<K, V> other = new CaseInsensitiveMap<>();
                for (Object o : c)
                {
                    other.put((K)o, null);
                }

                final int size = map.size();
                Iterator<K> i = map.keySet().iterator();
                while (i.hasNext())
                {
                    K key = i.next();
                    if (!other.containsKey(key))
                    {
                        i.remove();
                    }
                }

                return map.size() != size;
            }
            
            public Object[] toArray()
            {
                Object[] items = new Object[size()];
                int i = 0;
                for (Object key : map.keySet())
                {
                    items[i++] = key instanceof CaseInsensitiveString ? key.toString() : key;
                }
                return items;
            }

            public int size() { return map.size(); }
            public void clear() { map.clear(); }

            public int hashCode()
            {
                int h = 0;

                // Use map.keySet() so that we walk through the CaseInsensitiveStrings generating a hashCode
                // that is based on the lowerCase() value of the Strings (hashCode() on the CaseInsensitiveStrings
                // with map.keySet() will return the hashCode of .toLowerCase() of those strings).
                for (Object key : map.keySet())
                {
                    if (key != null)
                    {
                        h += key.hashCode();
                    }
                }
                return h;
            }

            @SuppressWarnings("unchecked")
            public Iterator<K> iterator()
            {
                iter = map.keySet().iterator();
                return new Iterator<K>()
                {
                    public void remove() { iter.remove(); }
                    public boolean hasNext() { return iter.hasNext(); }
                    public K next()
                    {
                        Object next = iter.next();
                        if (next instanceof CaseInsensitiveString)
                        {
                            next = next.toString();
                        }
                        return (K) next;
                    }
                };
            }
        };
    }

    public Set<Entry<K, V>> entrySet()
    {
        return new AbstractSet<Entry<K, V>>()
        {
            Iterator<Entry<K, V>> iter;

            public int size() { return map.size(); }
            public boolean isEmpty() { return map.isEmpty(); }
            public void clear() { map.clear(); }

            @SuppressWarnings("unchecked")
            public boolean contains(Object o)
            {
                if (!(o instanceof Entry))
                {
                    return false;
                }

                Entry<K, V> that = (Entry<K, V>) o;
                if (CaseInsensitiveMap.this.containsKey(that.getKey()))
                {
                    Object value = CaseInsensitiveMap.this.get(that.getKey());
                    return Objects.equals(value, that.getValue());
                }
                return false;
            }

            @SuppressWarnings("unchecked")
            public boolean remove(Object o)
            {
                if (!(o instanceof Entry))
                {
                    return false;
                }
                final int size = map.size();
                Entry<K, V> that = (Entry<K, V>) o;
                CaseInsensitiveMap.this.remove(that.getKey());
                return map.size() != size;
            }

            /**
             * This method is required.  JDK method is broken, as it relies
             * on iterator solution.  This method is fast because contains()
             * and remove() are both hashed O(1) look ups.
             */
            @SuppressWarnings("unchecked")
            public boolean removeAll(Collection c)
            {
                final int size = map.size();
                for (Object o : c)
                {
                    if (o instanceof Entry)
                    {
                        Entry<K, V> that = (Entry<K, V>) o;
                        CaseInsensitiveMap.this.remove(that.getKey());
                    }
                }
                return map.size() != size;
            }

            @SuppressWarnings("unchecked")
            public boolean retainAll(Collection c)
            {
                // Create fast-access O(1) to all elements within passed in Collection
                Map<K, V> other = new CaseInsensitiveMap<>();
                for (Object o : c)
                {
                    if (o instanceof Entry)
                    {
                        other.put(((Entry<K, V>)o).getKey(), ((Entry<K, V>) o).getValue());
                    }
                }

                int origSize = size();

                // Drop all items that are not in the passed in Collection
                Iterator<Entry<K,V>> i = map.entrySet().iterator();
                while (i.hasNext())
                {
                    Entry<K, V> entry = i.next();
                    K key = entry.getKey();
                    V value = entry.getValue();
                    if (!other.containsKey(key))
                    {   // Key not even present, nuke the entry
                        i.remove();
                    }
                    else
                    {   // Key present, now check value match
                        Object v = other.get(key);
                        if (!Objects.equals(v, value))
                        {
                            i.remove();
                        }
                    }
                }

                return size() != origSize;
            }
            
            public Iterator<Entry<K, V>> iterator()
            {
                iter = map.entrySet().iterator();
                return new Iterator<Entry<K, V>>()
                {
                    public boolean hasNext() { return iter.hasNext(); }
                    public Entry<K, V> next() { return new CaseInsensitiveEntry(iter.next()); }
                    public void remove() { iter.remove(); }
                };
            }
        };
    }

    /**
     * Entry implementation that will give back a String instead of a CaseInsensitiveString
     * when .getKey() is called.
     *
     * Also, when the setValue() API is called on the Entry, it will 'write thru' to the
     * underlying Map's value.
     */
    public class CaseInsensitiveEntry extends AbstractMap.SimpleEntry<K, V>
    {
        public CaseInsensitiveEntry(Entry<K, V> entry)
        {
            super(entry);
        }

        @SuppressWarnings("unchecked")
        public K getKey()
        {
            K superKey = super.getKey();
            if (superKey instanceof CaseInsensitiveString)
            {
                return (K)((CaseInsensitiveString)superKey).original;
            }
            return superKey;
        }

        public K getOriginalKey()
        {
            return super.getKey();
        }

        public V setValue(V value)
        {
            return map.put(super.getKey(), value);
        }
    }

    /**
     * Class used to wrap String keys.  This class ignores the
     * case of Strings when they are compared.  Based on known usage,
     * null checks, proper instance, etc. are dropped.
     */
    public static final class CaseInsensitiveString implements Comparable
    {
        private final String original;
        private final int hash;

        public CaseInsensitiveString(String string)
        {
            original = string;
            hash = StringUtilities.hashCodeIgnoreCase(string);  // no new String created unlike .toLowerCase()
        }

        public String toString()
        {
            return original;
        }

        public int hashCode()
        {
            return hash;
        }

        public boolean equals(Object other)
        {
            if (other == this)
            {
                return true;
            }
            if (other instanceof CaseInsensitiveString)
            {
                return hash == ((CaseInsensitiveString)other).hash &&
                        original.equalsIgnoreCase(((CaseInsensitiveString)other).original);
            }
            if (other instanceof String)
            {
                return original.equalsIgnoreCase((String)other);
            }
            return false;
        }

        public int compareTo(Object o)
        {
            if (o instanceof CaseInsensitiveString)
            {
                CaseInsensitiveString other = (CaseInsensitiveString) o;
                return original.compareToIgnoreCase(other.original);
            }
            if (o instanceof String)
            {
                String other = (String)o;
                return original.compareToIgnoreCase(other);
            }
            // Strings are less than non-Strings (come before)
            return -1;
        }
    }
}
