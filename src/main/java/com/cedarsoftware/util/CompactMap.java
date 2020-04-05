package com.cedarsoftware.util;

import java.util.*;

/**
 * Map that uses very little memory when only one entry is inside.
 * CompactMap has one member variable, that either holds the single
 * value, or is a pointer to a Map that holds all the keys and values
 * when there are two (2) or more entries in the Map.  When only one (1)
 * entry is in the Map, the member variable points to that.  When no
 * entries are in the Map, it uses an intern sentinel value to indicate
 * that the map is empty.  In order to support the "key" when there is
 * only one entry, the method singleValueKey() must be overloaded to return
 * a String key name.  This Map supports null values, but the keys must
 * not be null.  Subclasses can overwrite the newMap() API to return their
 * specific Map type (HashMap, LinkedHashMap, etc.) for the Map instance
 * that is used when there is more than one entry in the Map.
 *
 * @author John DeRegnaucourt (john@cedarsoftware.com)
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
public abstract class CompactMap<K, V> implements Map<K, V>
{
    private static final String EMPTY_MAP = "_︿_ψ_☼";
    private Object val = EMPTY_MAP;

    public int size()
    {
        if (val == EMPTY_MAP)
        {
            return 0;
        }
        else if (val instanceof CompactMapEntry || !(val instanceof Map))
        {
            return 1;
        }
        else
        {
            Map<K, V> map = (Map<K, V>) val;
            return map.size();
        }
    }

    public boolean isEmpty()
    {
        return val == EMPTY_MAP;
    }

    public boolean containsKey(Object key)
    {
        if (size() == 1)
        {
            return getLogicalSingleKey().equals(key);
        }
        else if (isEmpty())
        {
            return false;
        }

        Map<K, V> map = (Map<K, V>) val;
        return map.containsKey(key);
    }

    public boolean containsValue(Object value)
    {
        if (size() == 1)
        {
            return getLogicalSingleValue() == value;
        }
        else if (isEmpty())
        {
            return false;
        }
        Map<K, V> map = (Map<K, V>) val;
        return map.containsValue(value);
    }

    public V get(Object key)
    {
        if (size() == 1)
        {
            return getLogicalSingleKey().equals(key) ? getLogicalSingleValue() : null;
        }
        else if (isEmpty())
        {
            return null;
        }
        Map<K, V> map = (Map<K, V>) val;
        return map.get(key);
    }

    public V put(K key, V value)
    {
        if (size() == 1)
        {
            if (getLogicalSingleKey().equals(key))
            {   // Overwrite
                Object save = getLogicalSingleValue();
                if (getSingleValueKey().equals(key) && !(value instanceof Map))
                {
                    val = value;
                }
                else
                {
                    val = new CompactMapEntry<>(key, value);
                }
                return (V) save;
            }
            else
            {   // Add
                Map<K, V> map = getNewMap();
                map.put(getLogicalSingleKey(), getLogicalSingleValue());
                map.put(key, value);
                val = map;
                return null;
            }
        }
        else if (isEmpty())
        {
            if (getSingleValueKey().equals(key) && !(value instanceof Map))
            {
                val = value;
            }
            else
            {
                val = new CompactMapEntry<>(key, value);
            }
            return null;
        }
        Map<K, V> map = (Map<K, V>) val;
        return map.put(key, value);
    }

    public V remove(Object key)
    {
        if (size() == 1)
        {
            if (getLogicalSingleKey().equals(key))
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
        else if (isEmpty())
        {
            return null;
        }

        // Handle from 2+ entries.
        Map<K, V> map = (Map<K, V>) val;
        V save = map.remove(key);
        
        if (map.size() == 1)
        {   // Down to 1 entry, need to set 'val' to value or CompactMapEntry containing key/value
            Entry<K, V> entry = map.entrySet().iterator().next();
            clear();
            put(entry.getKey(), entry.getValue());  // .put() will figure out how to place this entry
        }
        return save;
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

    public Set<K> keySet()
    {
        return new AbstractSet<K>()
        {
            Iterator<K> iter;

            public Iterator<K> iterator()
            {
                if (CompactMap.this.size() == 1)
                {
                    Map<K, V> map = new HashMap<>();
                    map.put(getLogicalSingleKey(), (V)getLogicalSingleValue());
                    iter = map.keySet().iterator();
                    return new Iterator<K>()
                    {
                        public boolean hasNext() { return iter.hasNext(); }
                        public K next() { return iter.next(); }
                        public void remove() { CompactMap.this.clear(); }
                    };
                }
                else if (CompactMap.this.isEmpty())
                {
                    return new Iterator<K>()
                    {
                        public boolean hasNext() { return false; }
                        public K next() { throw new NoSuchElementException(".next() called on an empty CompactMap's keySet()"); }
                        public void remove() { throw new IllegalStateException(".remove() called on an empty CompactMap's keySet()"); }
                    };
                }

                // 2 or more elements in the CompactMap case.
                Map<K, V> map = (Map<K, V>)CompactMap.this.val;
                iter = map.keySet().iterator();

                return new Iterator<K>()
                {
                    public boolean hasNext() { return iter.hasNext(); }
                    public K next() { return iter.next(); }
                    public void remove() { removeIteratorItem(iter, "keySet"); }
                };
            }

            public int size() { return CompactMap.this.size(); }
            public boolean contains(Object o) { return CompactMap.this.containsKey(o); }
            public void clear() { CompactMap.this.clear(); }
        };
    }

    public Collection<V> values()
    {
        return new AbstractCollection<V>()
        {
            Iterator<V> iter;
            public Iterator<V> iterator()
            {
                if (CompactMap.this.size() == 1)
                {
                    Map<K, V> map = new HashMap<>();
                    map.put(getLogicalSingleKey(), (V)getLogicalSingleValue());
                    iter = map.values().iterator();
                    return new Iterator<V>()
                    {
                        public boolean hasNext() { return iter.hasNext(); }
                        public V next() { return iter.next(); }
                        public void remove() { CompactMap.this.clear(); }
                    };
                }
                else if (CompactMap.this.isEmpty())
                {
                    return new Iterator<V>()
                    {
                        public boolean hasNext() { return false; }
                        public V next() { throw new NoSuchElementException(".next() called on an empty CompactMap's values()"); }
                        public void remove() { throw new IllegalStateException(".remove() called on an empty CompactMap's values()"); }
                    };
                }

                // 2 or more elements in the CompactMap case.
                Map<K, V> map = (Map<K, V>)CompactMap.this.val;
                iter = map.values().iterator();

                return new Iterator<V>()
                {
                    public boolean hasNext() { return iter.hasNext(); }
                    public V next() { return iter.next(); }
                    public void remove() { removeIteratorItem(iter, "values"); }
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

            public int size() { return CompactMap.this.size(); }

            public Iterator<Entry<K, V>> iterator()
            {
                if (CompactMap.this.size() == 1)
                {
                    Map<K, V> map = new HashMap<>();
                    map.put(getLogicalSingleKey(), getLogicalSingleValue());
                    iter = map.entrySet().iterator();
                    return new Iterator<Entry<K, V>>()
                    {
                        public boolean hasNext() { return iter.hasNext(); }
                        public Entry<K, V> next() { return iter.next(); }
                        public void remove() { CompactMap.this.clear(); }
                    };
                }
                else if (CompactMap.this.isEmpty())
                {
                    return new Iterator<Entry<K, V>>()
                    {
                        public boolean hasNext() { return false; }
                        public Entry<K, V> next() { throw new NoSuchElementException(".next() called on an empty CompactMap's entrySet()"); }
                        public void remove() { throw new IllegalStateException(".remove() called on an empty CompactMap's entrySet()"); }
                    };
                }
                // 2 or more elements in the CompactMap case.
                Map<K, V> map = (Map<K, V>)CompactMap.this.val;
                iter = map.entrySet().iterator();

                return new Iterator<Entry<K, V>>()
                {
                    public boolean hasNext() { return iter.hasNext(); }
                    public Entry<K, V> next() { return iter.next(); }
                    public void remove() { removeIteratorItem(iter, "entrySet"); }
                };
            }
            public void clear() { CompactMap.this.clear(); }
        };
    }

    private void removeIteratorItem(Iterator iter, String methodName)
    {
        if (CompactMap.this.size() == 1)
        {
            CompactMap.this.clear();
        }
        else if (CompactMap.this.isEmpty())
        {
            throw new IllegalStateException(".remove() called on an empty CompactMap's " + methodName + " iterator");
        }
        else
        {
            if (this.size() == 2)
            {
                Iterator<Entry<K, V>> entryIterator = ((Map<K, V>) this.val).entrySet().iterator();
                Entry<K, V> firstEntry = entryIterator.next();
                Entry<K, V> secondEntry = entryIterator.next();
                this.clear();

                if (iter.hasNext())
                {   // .remove() called on 2nd element in 2 element list
                    this.put(secondEntry.getKey(), secondEntry.getValue());
                }
                else
                {   // .remove() called on 1st element in 1 element list
                    this.put(firstEntry.getKey(), firstEntry.getValue());
                }
            }
            else
            {
                iter.remove();
            }
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
        EMPTY, OBJECT, ENTRY, MAP
    }

    protected LogicalValueType getLogicalValueType()
    {
        if (size() == 1)
        {
            if (val instanceof CompactMapEntry)
            {
                return LogicalValueType.ENTRY;
            }
            else
            {
                return LogicalValueType.OBJECT;
            }
        }
        else if (isEmpty())
        {
            return LogicalValueType.EMPTY;
        }
        else
        {
            return LogicalValueType.MAP;
        }
    }

    /**
     * Marker Class to hold key and value when the key is not the same as the getSingleValueKey().
     */
    public class CompactMapEntry<K, V> implements Entry
    {
        K key;
        V value;

        public CompactMapEntry(K key, V value)
        {
            this.key = key;
            this.value = value;
        }

        public Object getKey() { return key; }
        public Object getValue() { return value; }
        public Object setValue(Object value1)
        {
            if (CompactMap.this.size() == 1)
            {
                CompactMap.this.clear();
            }
            return null;
        }
    }

    private K getLogicalSingleKey()
    {
        if (val instanceof CompactMapEntry)
        {
            CompactMapEntry<K, V> entry = (CompactMapEntry<K, V>) val;
            return (K) entry.getKey();
        }
        return getSingleValueKey();
    }

    private V getLogicalSingleValue()
    {
        if (val instanceof CompactMapEntry)
        {
            CompactMapEntry<K, V> entry = (CompactMapEntry<K, V>) val;
            return (V)entry.getValue();
        }
        return (V) val;
    }

    /**
     * @return String key name when there is only one entry in the Map.
     */
    protected abstract K getSingleValueKey();

    /**
     * @return new empty Map instance to use when there is more than one entry.
     */
    protected abstract Map<K, V> getNewMap();
}
