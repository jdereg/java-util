package com.cedarsoftware.util;

import java.util.*;

/**
 * `CompactMap` introduced.  This `Map` is especially small when 0 and 1 entries are stored in it.  When `>=2` entries
 * are in the `Map` it acts as regular `Map`.
 * 
 *     You must override two methods in order to instantiate:
 *     
 *     protected abstract K getSingleValueKey();
 *     protected abstract Map<K, V> getNewMap();
 *
 * **Empty**
 * This class only has one (1) member variable of type `Object`.  If there are no entries in it, then the value of that
 * member variable takes on a pointer (points to sentinel value.)
 *
 * **One entry**
 * If the entry has a key that matches the value returned from `getSingleValueKey()` then there is no key stored
 * and the internal single member points to the value only.
 *
 * If the single entry's key does not match the value returned from `getSingleValueKey()` then the internal field points
 * to an internal `Class` `CompactMapEntry` which contains the key and the value (nothing else).  Again, all APIs still operate
 * the same.
 *
 * **Two or more entries**
 * In this case, the single member variable points to a `Map` instance (supplied by `getNewMap()` API that user supplied.)
 * This allows `CompactMap` to work with nearly all `Map` types.
 *
 * This Map supports null for the key and values.  If the Map returned by getNewMap() does not support this, then this
 * Map will not.
 *
 * A future version *may* support an additional option to allow it to maintain entries 2-n in an internal
 * array (pointed to by the single member variable).  This small array would be 'scanned' in linear time.  Given
 * a small *`n`*  entries, the resultant `Map` would be significantly smaller than the equivalent `HashMap`, for instance.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
public class CompactMap<K, V> implements Map<K, V>
{
    private static final String EMPTY_MAP = "_︿_ψ_☼";
    private Object val = EMPTY_MAP;

    public CompactMap()
    {
        if (compactSize() < 2)
        {
            throw new IllegalStateException("compactSize() must be >= 2");
        }
    }

    public CompactMap(Map<K, V> other)
    {
        this();
        putAll(other);
    }
    
    public int size()
    {
        if (Object[].class.isInstance(val))
        {   // 2 to compactSize
            return ((Object[])val).length >> 1;
        }
        else if (Map.class.isInstance(val))
        {   // > compactSize
            return ((Map<K, V>)val).size();
        }
        else if (val == EMPTY_MAP)
        {   // empty
            return 0;
        }

        // size == 1
        return 1;
    }

    public boolean isEmpty()
    {
        return val == EMPTY_MAP;
    }

    private boolean compareKeys(Object key, Object aKey)
    {
        if (key instanceof String)
        {
            if (aKey instanceof String)
            {
                if (isCaseInsensitive())
                {
                    return ((String)aKey).equalsIgnoreCase((String) key);
                }
                else
                {
                    return aKey.equals(key);
                }
            }
            return false;
        }

        return Objects.equals(key, aKey);
    }

    public boolean containsKey(Object key)
    {
        if (Object[].class.isInstance(val))
        {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            for (int i=0; i < entries.length; i += 2)
            {
                if (compareKeys(key, entries[i]))
                {
                    return true;
                }
            }
            return false;
        }
        else if (Map.class.isInstance(val))
        {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            return map.containsKey(key);
        }
        else if (val == EMPTY_MAP)
        {   // empty
            return false;
        }

        // size == 1
        return compareKeys(key, getLogicalSingleKey());
    }

    public boolean containsValue(Object value)
    {
        if (Object[].class.isInstance(val))
        {   // 2 to Compactsize
            Object[] entries = (Object[]) val;
            for (int i=0; i < entries.length; i += 2)
            {
                Object aValue = entries[i + 1];
                if (Objects.equals(value, aValue))
                {
                    return true;
                }
            }
            return false;
        }
        else if (Map.class.isInstance(val))
        {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            return map.containsValue(value);
        }
        else if (val == EMPTY_MAP)
        {   // empty
            return false;
        }

        // size == 1
        return getLogicalSingleValue() == value;
    }

    public V get(Object key)
    {
        if (Object[].class.isInstance(val))
        {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            for (int i=0; i < entries.length; i += 2)
            {
                Object aKey = entries[i];
                if (compareKeys(key, aKey))
                {
                    return (V) entries[i + 1];
                }
            }
            return null;
        }
        else if (Map.class.isInstance(val))
        {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            return map.get(key);
        }
        else if (val == EMPTY_MAP)
        {   // empty
            return null;
        }

        // size == 1
        return compareKeys(key, getLogicalSingleKey()) ? getLogicalSingleValue() : null;
    }

    public V put(K key, V value)
    {
        if (Object[].class.isInstance(val))
        {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            for (int i=0; i < entries.length; i += 2)
            {
                Object aKey = entries[i];
                Object aValue = entries[i + 1];
                if (Objects.equals(key, aKey))
                {   // Overwrite case
                    entries[i + 1] = value;
                    return (V) aValue;
                }
            }

            // Not present in Object[]
            if (size() < compactSize())
            {   // Grow array
                Object[] expand = new Object[entries.length + 2];
                System.arraycopy(entries, 0, expand, 0, entries.length);
                // Place new entry
                expand[expand.length - 2] = key;
                expand[expand.length - 1] = value;
                val = expand;
                return null;
            }
            else
            {   // Switch to Map - copy entries
                Map<K, V> map = getNewMap();
                entries = (Object[]) val;
                for (int i=0; i < entries.length; i += 2)
                {
                    Object aKey = entries[i];
                    Object aValue = entries[i + 1];
                    map.put((K) aKey, (V) aValue);
                }
                // Place new entry
                map.put(key, value);
                val = map;
                return null;
            }
        }
        else if (Map.class.isInstance(val))
        {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            return map.put(key, value);
        }
        else if (val == EMPTY_MAP)
        {   // empty
            if (compareKeys(key, getLogicalSingleKey()) && !(value instanceof Map || value instanceof Object[]))
            {
                val = value;
            }
            else
            {
                val = new CompactMapEntry(key, value);
            }
            return null;
        }

        // size == 1
        if (compareKeys(key, getLogicalSingleKey()))
        {   // Overwrite
            Object save = getLogicalSingleValue();
            if (Objects.equals(getSingleValueKey(), key) && !(value instanceof Map || value instanceof Object[]))
            {
                val = value;
            }
            else
            {
                val = new CompactMapEntry(key, value);
            }
            return (V) save;
        }
        else
        {   // CompactMapEntry to []
            Object[] entries = new Object[4];
            entries[0] = getLogicalSingleKey();
            entries[1] = getLogicalSingleValue();
            entries[2] = key;
            entries[3] = value;
            val = entries;
            return null;
        }
    }

    public V remove(Object key)
    {
        if (Object[].class.isInstance(val))
        {   // 2 to compactSize
            if (size() == 2)
            {   // When at 2 entries, we must drop back to CompactMapEntry or val (use clear() and put() to get us there).
                Object[] entries = (Object[]) val;
                if (compareKeys(key, entries[0]))
                {
                    Object prevValue = entries[1];
                    clear();
                    put((K)entries[2], (V)entries[3]);
                    return (V) prevValue;
                }
                else if (compareKeys(key, entries[2]))
                {
                    Object prevValue = entries[3];
                    clear();
                    put((K)entries[0], (V)entries[1]);
                    return (V) prevValue;
                }

                return null;    // not found
            }
            else
            {
                Object[] entries = (Object[]) val;
                for (int i = 0; i < entries.length; i += 2)
                {
                    Object aKey = entries[i];
                    if (compareKeys(key, aKey))
                    {   // Found, must shrink
                        Object prior = entries[i + 1];
                        Object[] shrink = new Object[entries.length - 2];
                        System.arraycopy(entries, 0, shrink, 0, i);
                        System.arraycopy(entries, i + 2, shrink, i, shrink.length - i);
                        val = shrink;
                        return (V) prior;
                    }
                }

                return null;    // not found
            }
        }
        else if (Map.class.isInstance(val))
        {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            if (!map.containsKey(key))
            {
                return null;
            }
            V save = map.remove(key);

            if (map.size() == compactSize())
            {   // Down to compactSize, need to switch to Object[]
                Object[] entries = new Object[compactSize() * 2];
                Iterator<Entry<K, V>> i = map.entrySet().iterator();
                int idx = 0;
                while (i.hasNext())
                {
                    Entry<K, V> entry = i.next();
                    entries[idx] = entry.getKey();
                    entries[idx + 1] = entry.getValue();
                    idx += 2;
                }
                val = entries;
            }
            return save;
        }
        else if (val == EMPTY_MAP)
        {   // empty
            return null;
        }

        // size == 1
        if (compareKeys(key, getLogicalSingleKey()))
        {   // found
            Object save = getLogicalSingleValue();
            val = EMPTY_MAP;
            return (V) save;
        }
        else
        {   // not found
            return null;
        }
    }

    public void putAll(Map<? extends K, ? extends V> m)
    {
        for (Entry<? extends K, ? extends V> entry : m.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear()
    {
        val = EMPTY_MAP;
    }

    public int hashCode()
    {
        int h = 0;
        Iterator<Entry<K,V>> i = entrySet().iterator();
        while (i.hasNext())
        {
            h += i.next().hashCode();
        }
        return h;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Map))
        {   // null or a non-Map passed in.
            return false;
        }
        Map other = (Map) obj;
        if (size() != other.size())
        {   // sizes are not the same
            return false;
        }

        if (Object[].class.isInstance(val))
        {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            for (int i=0; i < entries.length; i += 2)
            {
                Object aKey = entries[i];
                Object aValue = entries[i + 1];
                if (!other.containsKey(aKey))
                {
                    return false;
                }
                Object otherVal = other.get(aKey);
                if (!Objects.equals(aValue, otherVal))
                {
                    return false;
                }
            }
            return true;
        }
        else if (Map.class.isInstance(val))
        {   // > compactSize
            Map<K, V> map = (Map<K, V>) val;
            return map.equals(other);
        }
        else if (val == EMPTY_MAP)
        {   // empty
            return other.isEmpty();
        }

        // size == 1
        return entrySet().equals(other.entrySet());
    }

    public Set<K> keySet()
    {
        return new AbstractSet<K>()
        {
            Iterator<Entry<K,V>> iter;
            Entry<K, V> currentEntry;

            public Iterator<K> iterator()
            {
                Map<K, V> copy = getCopy();
                iter = copy.entrySet().iterator();

                return new Iterator<K>()
                {
                    public boolean hasNext() { return iter.hasNext(); }
                    public K next()
                    {
                        currentEntry = iter.next();
                        return currentEntry.getKey();
                    }
                    public void remove() { iteratorRemove(currentEntry); }
                };
            }

            public int size() { return CompactMap.this.size(); }
            public void clear() { CompactMap.this.clear(); }
            public boolean contains(Object o) { return CompactMap.this.containsKey(o); }    // faster than inherited method
        };
    }

    public Collection<V> values()
    {
        return new AbstractCollection<V>()
        {
            Iterator<Entry<K, V>> iter;
            Entry<K, V> currentEntry;

            public Iterator<V> iterator()
            {
                Map<K, V> copy = getCopy();
                iter = copy.entrySet().iterator();

                return new Iterator<V>()
                {
                    public boolean hasNext() { return iter.hasNext(); }
                    public V next()
                    {
                        currentEntry = iter.next();
                        return currentEntry.getValue();
                    }
                    public void remove() { iteratorRemove(currentEntry); }
                };
            }

            public int size() { return CompactMap.this.size(); }
            public void clear() { CompactMap.this.clear(); }
        };
    }

    public Set<Entry<K, V>> entrySet()
    {
        return new AbstractSet<Entry<K,V>>()
        {
            Iterator<Entry<K, V>> iter;
            Entry<K, V> currentEntry;

            public Iterator<Entry<K, V>> iterator()
            {
                Map<K, V> copy = getCopy();
                iter = copy.entrySet().iterator();
                
                return new Iterator<Entry<K, V>>()
                {
                    public boolean hasNext() { return iter.hasNext(); }
                    public Entry<K, V> next()
                    {
                        currentEntry = iter.next();
                        return new CompactMapEntry(currentEntry.getKey(), currentEntry.getValue());
                    }
                    public void remove() { iteratorRemove(currentEntry); }
                };
            }
            
            public int size() { return CompactMap.this.size(); }
            public void clear() { CompactMap.this.clear(); }
            public boolean contains(Object o)
            {   // faster than inherited method
                if (o instanceof Entry)
                {
                    Entry<K, V> entry = (Entry<K, V>)o;
                    if (CompactMap.this.containsKey(entry.getKey()))
                    {
                        V value = CompactMap.this.get(entry.getKey());
                        Entry<K, V> candidate = new AbstractMap.SimpleEntry<>(entry.getKey(), value);
                        return Objects.equals(entry, candidate);
                    }
                }
                return false;
            }
        };
    }

    private Map<K, V> getCopy()
    {
        Map<K, V> copy = getNewMap();   // Use their Map (TreeMap, HashMap, LinkedHashMap, etc.)
        if (Object[].class.isInstance(val))
        {   // 2 to compactSize - copy Object[] into Map
            Object[] entries = (Object[]) CompactMap.this.val;
            int len = entries.length;
            for (int i=0; i < len; i += 2)
            {
                copy.put((K)entries[i], (V)entries[i + 1]);
            }
        }
        else if (Map.class.isInstance(val))
        {   // > compactSize - putAll to copy
            copy.putAll((Map<K, V>)CompactMap.this.val);
        }
        else if (val == EMPTY_MAP)
        {   // empty - nothing to copy
        }
        else
        {   // size == 1
            copy.put(getLogicalSingleKey(), getLogicalSingleValue());
        }
        return copy;
    }

    private void iteratorRemove(Entry<K, V> currentEntry)
    {
        if (currentEntry == null)
        {   // remove() called on iterator
            throw new IllegalStateException("remove() called on an Iterator before calling next()");
        }

        K key = currentEntry.getKey();
        if (containsKey(key))
        {
            remove(key);
        }
        else
        {
            throw new IllegalStateException("Cannot remove from iterator when it is passed the last item.");
        }
    }

    public Map minus(Object removeMe)
    {
        throw new UnsupportedOperationException("Unsupported operation [minus] or [-] between Maps.  Use removeAll() or retainAll() instead.");
    }

    public Map plus(Object right)
    {
        throw new UnsupportedOperationException("Unsupported operation [plus] or [+] between Maps.  Use putAll() instead.");
    }

    protected enum LogicalValueType
    {
        EMPTY, OBJECT, ENTRY, MAP, ARRAY
    }

    protected LogicalValueType getLogicalValueType()
    {
        if (Object[].class.isInstance(val))
        {   // 2 to compactSize
            return LogicalValueType.ARRAY;
        }
        else if (Map.class.isInstance(val))
        {   // > compactSize
            return LogicalValueType.MAP;
        }
        else if (val == EMPTY_MAP)
        {   // empty
            return LogicalValueType.EMPTY;
        }
        else
        {   // size == 1
            if (CompactMapEntry.class.isInstance(val))
            {
                return LogicalValueType.ENTRY;
            }
            else
            {
                return LogicalValueType.OBJECT;
            }
        }
    }

    /**
     * Marker Class to hold key and value when the key is not the same as the getSingleValueKey().
     * This method transmits the setValue() to changes on the outer CompactMap instance.
     */
    protected class CompactMapEntry extends AbstractMap.SimpleEntry<K, V>
    {
        protected CompactMapEntry(K key, V value)
        {
            super(key, value);
        }

        public V setValue(V value)
        {
            V save = this.getValue();
            super.setValue(value);
            CompactMap.this.put(getKey(), value);    // "Transmit" (write-thru) to underlying Map.
            return save;
        }
    }

    private K getLogicalSingleKey()
    {
        if (CompactMapEntry.class.isInstance(val))
        {
            CompactMapEntry entry = (CompactMapEntry) val;
            return entry.getKey();
        }
        return getSingleValueKey();
    }

    private V getLogicalSingleValue()
    {
        if (CompactMapEntry.class.isInstance(val))
        {
            CompactMapEntry entry = (CompactMapEntry) val;
            return entry.getValue();
        }
        return (V) val;
    }

    /**
     * @return String key name when there is only one entry in the Map.
     */
    protected K getSingleValueKey() { return (K) "key"; };

    /**
     * @return new empty Map instance to use when there is more than one entry.
     */
    protected Map<K, V> getNewMap() { return new HashMap<>(); }
    protected boolean isCaseInsensitive() { return false; }
    protected int compactSize() { return 100; }
}
