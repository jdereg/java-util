package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Many developers do not realize than they may have thousands or hundreds of thousands of Maps in memory, often
 * representing small JSON objects.  These maps (often HashMaps) usually have a table of 16/32/64... elements in them,
 * with many empty elements.  HashMap doubles it's internal storage each time it expands, so often these Maps have
 * fewer than 50% of these arrays filled.<p></p>
 *
 * CompactMap is a Map that strives to reduce memory at all costs while retaining speed that is close to HashMap's speed.
 * It does this by using only one (1) member variable (of type Object) and changing it as the Map grows.  It goes from
 * single value, to a single MapEntry, to an Object[], and finally it uses a Map (user defined).  CompactMap is
 * especially small when 0 and 1 entries are stored in it.  When size() is from `2` to compactSize(), then entries
 * are stored internally in single Object[].  If the size() is {@literal >} compactSize() then the entries are stored in a
 * regular `Map`.<pre>
 *
 *     Methods you may want to override:
 *
 *     // If this key is used and only 1 element then only the value is stored
 *     protected K getSingleValueKey() { return "someKey"; }
 *
 *     // Map you would like it to use when size() {@literal >} compactSize().  HashMap is default
 *     protected abstract Map{@literal <}K, V{@literal >} getNewMap();
 *
 *     // If you want case insensitivity, return true and return new CaseInsensitiveMap or TreeMap(String.CASE_INSENSITIVE_PRDER) from getNewMap()
 *     protected boolean isCaseInsensitive() { return false; }
 *
 *     // When size() {@literal >} than this amount, the Map returned from getNewMap() is used to store elements.
 *     protected int compactSize() { return 80; }
 *
 * </pre>
 * **Empty**
 * This class only has one (1) member variable of type `Object`.  If there are no entries in it, then the value of that
 * member variable takes on a pointer (points to sentinel value.)<p></p>
 *
 * **One entry**
 * If the entry has a key that matches the value returned from `getSingleValueKey()` then there is no key stored
 * and the internal single member points to the value only.<p></p>
 *
 * If the single entry's key does not match the value returned from `getSingleValueKey()` then the internal field points
 * to an internal `Class` `CompactMapEntry` which contains the key and the value (nothing else).  Again, all APIs still operate
 * the same.<p></p>
 *
 * **Two thru compactSize() entries**
 * In this case, the single member variable points to a single Object[] that contains all the keys and values.  The
 * keys are in the even positions, the values are in the odd positions (1 up from the key).  [0] = key, [1] = value,
 * [2] = next key, [3] = next value, and so on.  The Object[] is dynamically expanded until size() {@literal >} compactSize(). In
 * addition, it is dynamically shrunk until the size becomes 1, and then it switches to a single Map Entry or a single
 * value.<p></p>
 *
 * **size() greater than compactSize()**
 * In this case, the single member variable points to a `Map` instance (supplied by `getNewMap()` API that user supplied.)
 * This allows `CompactMap` to work with nearly all `Map` types.<p></p>
 *
 * This Map supports null for the key and values, as long as the Map returned by getNewMap() supports null keys-values.
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
@SuppressWarnings("unchecked")
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
        if (val instanceof Object[])
        {   // 2 to compactSize
            return ((Object[])val).length >> 1;
        }
        else if (val instanceof Map)
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
        if (val instanceof Object[])
        {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            final int len = entries.length;
            for (int i=0; i < len; i += 2)
            {
                if (compareKeys(key, entries[i]))
                {
                    return true;
                }
            }
            return false;
        }
        else if (val instanceof Map)
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
        if (val instanceof Object[])
        {   // 2 to Compactsize
            Object[] entries = (Object[]) val;
            final int len = entries.length;
            for (int i=0; i < len; i += 2)
            {
                Object aValue = entries[i + 1];
                if (Objects.equals(value, aValue))
                {
                    return true;
                }
            }
            return false;
        }
        else if (val instanceof Map)
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
        if (val instanceof Object[])
        {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            final int len = entries.length;
            for (int i=0; i < len; i += 2)
            {
                Object aKey = entries[i];
                if (compareKeys(key, aKey))
                {
                    return (V) entries[i + 1];
                }
            }
            return null;
        }
        else if (val instanceof Map)
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
        if (val instanceof Object[])
        {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            final int len = entries.length;
            for (int i=0; i < len; i += 2)
            {
                Object aKey = entries[i];
                Object aValue = entries[i + 1];
                if (compareKeys(key, aKey))
                {   // Overwrite case
                    entries[i + 1] = value;
                    return (V) aValue;
                }
            }

            // Not present in Object[]
            if (size() < compactSize())
            {   // Grow array
                Object[] expand = new Object[len + 2];
                System.arraycopy(entries, 0, expand, 0, len);
                // Place new entry at end
                expand[expand.length - 2] = key;
                expand[expand.length - 1] = value;
                val = expand;
            }
            else
            {   // Switch to Map - copy entries
                Map<K, V> map = getNewMap(size() + 1);
                entries = (Object[]) val;
                final int len2 = entries.length;
                for (int i=0; i < len2; i += 2)
                {
                    Object aKey = entries[i];
                    Object aValue = entries[i + 1];
                    map.put((K) aKey, (V) aValue);
                }
                // Place new entry
                map.put(key, value);
                val = map;
            }
            return null;
        }
        else if (val instanceof Map)
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
            V save = getLogicalSingleValue();
            if (compareKeys(key, getSingleValueKey()) && !(value instanceof Map || value instanceof Object[]))
            {
                val = value;
            }
            else
            {
                val = new CompactMapEntry(key, value);
            }
            return save;
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
        if (val instanceof Object[])
        {   // 2 to compactSize
            Object[] entries = (Object[]) val;
            if (size() == 2)
            {   // When at 2 entries, we must drop back to CompactMapEntry or val (use clear() and put() to get us there).
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
            }
            else
            {
                final int len = entries.length;
                for (int i = 0; i < len; i += 2)
                {
                    Object aKey = entries[i];
                    if (compareKeys(key, aKey))
                    {   // Found, must shrink
                        Object prior = entries[i + 1];
                        Object[] shrink = new Object[len - 2];
                        System.arraycopy(entries, 0, shrink, 0, i);
                        System.arraycopy(entries, i + 2, shrink, i, shrink.length - i);
                        val = shrink;
                        return (V) prior;
                    }
                }
            }
            return null;    // not found
        }
        else if (val instanceof Map)
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
            V save = getLogicalSingleValue();
            val = EMPTY_MAP;
            return save;
        }
        else
        {   // not found
            return null;
        }
    }

    public void putAll(Map<? extends K, ? extends V> map)
    {
        if (map == null)
        {
            return;
        }
        int mSize = map.size();
        if (val instanceof Map || mSize > compactSize())
        {
            if (val == EMPTY_MAP)
            {
                val = getNewMap(mSize);
            }
            ((Map<K, V>) val).putAll(map);
        }
        else
        {
            for (Entry<? extends K, ? extends V> entry : map.entrySet())
            {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void clear()
    {
        val = EMPTY_MAP;
    }

    public int hashCode()
    {
        if (val instanceof Object[])
        {
            int h = 0;
            Object[] entries = (Object[]) val;
            final int len = entries.length;
            for (int i=0; i < len; i += 2)
            {
                Object aKey = entries[i];
                Object aValue = entries[i + 1];
                h += computeKeyHashCode(aKey) ^ computeValueHashCode(aValue);
            }
            return h;
        }
        else if (val instanceof Map)
        {
            return val.hashCode();
        }
        else if (val == EMPTY_MAP)
        {
            return 0;
        }
        
        // size == 1
        return computeKeyHashCode(getLogicalSingleKey()) ^ computeValueHashCode(getLogicalSingleValue());
    }

    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (!(obj instanceof Map)) return false;
        Map<?, ?> other = (Map<?, ?>) obj;
        if (size() != other.size()) return false;

        if (val instanceof Object[])
        {   // 2 to compactSize
            for (Entry<?, ?> entry : other.entrySet())
            {
                final Object thatKey = entry.getKey();
                if (!containsKey(thatKey))
                {
                    return false;
                }

                Object thatValue = entry.getValue();
                Object thisValue = get(thatKey);

                if (thatValue == null || thisValue == null)
                {   // Perform null checks
                    if (thatValue != thisValue)
                    {
                        return false;
                    }
                }
                else if (!thisValue.equals(thatValue))
                {
                    return false;
                }
            }
        }
        else if (val instanceof Map)
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

    public String toString()
    {
        Iterator<Entry<K,V>> i = entrySet().iterator();
        if (!i.hasNext())
        {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (;;)
        {
            Entry<K,V> e = i.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key == this ? "(this Map)" : key);
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value);
            if (!i.hasNext())
            {
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
        }
    }

    public Set<K> keySet()
    {
        return new AbstractSet<K>()
        {
            public Iterator<K> iterator()
            {
                if (useCopyIterator())
                {
                    return new CopyKeyIterator();
                }
                else
                {
                    return new CompactKeyIterator();
                }
            }

            public int size() { return CompactMap.this.size(); }
            public void clear() { CompactMap.this.clear(); }
            public boolean contains(Object o) { return CompactMap.this.containsKey(o); }    // faster than inherited method
            public boolean remove(Object o)
            {
                final int size = size();
                CompactMap.this.remove(o);
                return size() != size;
            }

            public boolean removeAll(Collection c)
            {
                int size = size();
                for (Object o : c)
                {
                    CompactMap.this.remove(o);
                }
                return size() != size;
            }

            public boolean retainAll(Collection c)
            {
                // Create fast-access O(1) to all elements within passed in Collection
                Map<K, V> other = new CompactMap<K, V>()
                {   // Match outer
                    protected boolean isCaseInsensitive() { return CompactMap.this.isCaseInsensitive(); }
                    protected int compactSize() { return CompactMap.this.compactSize(); }
                    protected Map<K, V> getNewMap() { return CompactMap.this.getNewMap(c.size()); }
                };
                for (Object o : c)
                {
                    other.put((K)o, null);
                }

                final int size = size();
                keySet().removeIf(key -> !other.containsKey(key));

                return size() != size;
            }
        };
    }

    public Collection<V> values()
    {
        return new AbstractCollection<V>()
        {
            public Iterator<V> iterator()
            {
                if (useCopyIterator())
                {
                    return new CopyValueIterator();
                }
                else
                {
                    return new CompactValueIterator();
                }
            }

            public int size() { return CompactMap.this.size(); }
            public void clear() { CompactMap.this.clear(); }
        };
    }

    public Set<Entry<K, V>> entrySet()
    {
        return new AbstractSet()
        {
            public Iterator<Entry<K, V>> iterator()
            {
                if (useCopyIterator())
                {
                    return new CopyEntryIterator();
                }
                else
                {
                    return new CompactEntryIterator();
                }
            }
            
            public int size() { return CompactMap.this.size(); }
            public void clear() { CompactMap.this.clear(); }
            public boolean contains(Object o)
            {   // faster than inherited method
                if (o instanceof Entry)
                {
                    Entry<K, V> entry = (Entry<K, V>)o;
                    K entryKey = entry.getKey();

                    Object value = CompactMap.this.get(entryKey);
                    if (value != null)
                    {   // Found non-null value with key, return true if values are equals()
                        return Objects.equals(value, entry.getValue());
                    }
                    else if (CompactMap.this.containsKey(entryKey))
                    {
                        value = CompactMap.this.get(entryKey);
                        return Objects.equals(value, entry.getValue());
                    }
                }
                return false;
            }

            public boolean remove(Object o)
            {
                if (!(o instanceof Entry)) { return  false; }
                final int size = size();
                Entry<K, V> that = (Entry<K, V>) o;
                CompactMap.this.remove(that.getKey());
                return size() != size;
            }

            /**
             * This method is required.  JDK method is broken, as it relies
             * on iterator solution.  This method is fast because contains()
             * and remove() are both hashed O(1) look ups.
             */
            public boolean removeAll(Collection c)
            {
                final int size = size();
                for (Object o : c)
                {
                    remove(o);
                }
                return size() != size;
            }

            public boolean retainAll(Collection c)
            {
                // Create fast-access O(1) to all elements within passed in Collection
                Map<K, V> other = new CompactMap<K, V>()
                {   // Match outer
                    protected boolean isCaseInsensitive() { return CompactMap.this.isCaseInsensitive(); }
                    protected int compactSize() { return CompactMap.this.compactSize(); }
                    protected Map<K, V> getNewMap() { return CompactMap.this.getNewMap(c.size()); }
                };
                for (Object o : c)
                {
                    if (o instanceof Entry)
                    {
                        other.put(((Entry<K, V>)o).getKey(), ((Entry<K, V>) o).getValue());
                    }
                }

                int origSize = size();

                // Drop all items that are not in the passed in Collection
                Iterator<Entry<K,V>> i = entrySet().iterator();
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
        };
    }

    private Map<K, V> getCopy()
    {
        Map<K, V> copy = getNewMap(size());   // Use their Map (TreeMap, HashMap, LinkedHashMap, etc.)
        if (val instanceof Object[])
        {   // 2 to compactSize - copy Object[] into Map
            Object[] entries = (Object[]) CompactMap.this.val;
            final int len = entries.length;
            for (int i=0; i < len; i += 2)
            {
                copy.put((K)entries[i], (V)entries[i + 1]);
            }
        }
        else if (val instanceof Map)
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

    private void iteratorRemove(Entry<K, V> currentEntry, Iterator<Entry<K, V>> i)
    {
        if (currentEntry == null)
        {   // remove() called on iterator prematurely
            throw new IllegalStateException("remove() called on an Iterator before calling next()");
        }

        remove(currentEntry.getKey());
    }

    public Map<K, V> minus(Object removeMe)
    {
        throw new UnsupportedOperationException("Unsupported operation [minus] or [-] between Maps.  Use removeAll() or retainAll() instead.");
    }

    public Map<K, V> plus(Object right)
    {
        throw new UnsupportedOperationException("Unsupported operation [plus] or [+] between Maps.  Use putAll() instead.");
    }

    protected enum LogicalValueType
    {
        EMPTY, OBJECT, ENTRY, MAP, ARRAY
    }

    protected LogicalValueType getLogicalValueType()
    {
        if (val instanceof Object[])
        {   // 2 to compactSize
            return LogicalValueType.ARRAY;
        }
        else if (val instanceof Map)
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
     * This method transmits the setValue() changes to the outer CompactMap instance.
     */
    public class CompactMapEntry extends AbstractMap.SimpleEntry<K, V>
    {
        public CompactMapEntry(K key, V value)
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

        public boolean equals(Object o)
        {
            if (!(o instanceof Map.Entry)) { return false; }
            if (o == this) { return true; }

            Map.Entry<?,?> e = (Map.Entry<?,?>)o;
            return compareKeys(getKey(), e.getKey()) && Objects.equals(getValue(), e.getValue());
        }

        public int hashCode()
        {
            return computeKeyHashCode(getKey()) ^ computeValueHashCode(getValue());
        }
    }

    protected int computeKeyHashCode(Object key)
    {
        if (key instanceof String)
        {
            if (isCaseInsensitive())
            {
                return StringUtilities.hashCodeIgnoreCase((String)key);
            }
            else
            {   // k can't be null here (null is not instanceof String)
                return key.hashCode();
            }
        }
        else
        {
            int keyHash;
            if (key == null)
            {
                return 0;
            }
            else
            {
                keyHash = key == CompactMap.this ? 37: key.hashCode();
            }
            return keyHash;
        }
    }

    protected int computeValueHashCode(Object value)
    {
        if (value == CompactMap.this)
        {
            return 17;
        }
        else
        {
            return value == null ? 0 : value.hashCode();
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
     * @return new empty Map instance to use when size() becomes {@literal >} compactSize().
     */
    protected Map<K, V> getNewMap() { return new HashMap<>(compactSize() + 1); }
    protected Map<K, V> getNewMap(int size)
    {
        Map<K, V> map = getNewMap();
        try
        {
            Constructor<?> constructor = ReflectionUtils.getConstructor(map.getClass(), Integer.TYPE);
            return (Map<K, V>) constructor.newInstance(size);
        }
        catch (Exception e)
        {
            return map;
        }
    }
    protected boolean isCaseInsensitive() { return false; }
    protected int compactSize() { return 80; }

    protected boolean useCopyIterator() {
        Map<K, V> newMap = getNewMap();
        if (newMap instanceof CaseInsensitiveMap) {
            newMap = ((CaseInsensitiveMap<K, V>) newMap).getWrappedMap();
        }
        return newMap instanceof SortedMap;
    }

    /* ------------------------------------------------------------ */
    // iterators

    abstract class CompactIterator {
        Iterator<Map.Entry<K,V>> mapIterator;
        Object current; // Map.Entry if > compactsize, key <= compactsize
        int expectedSize;  // for fast-fail
        int index;             // current slot

        CompactIterator() {
            expectedSize = size();
            current = EMPTY_MAP;
            index = -1;
            if (val instanceof Map) {
                mapIterator = ((Map<K, V>)val).entrySet().iterator();
            }
        }

        public final boolean hasNext() {
            if (mapIterator!=null) {
                return mapIterator.hasNext();
            }
            else {
                return (index+1) < size();
            }
        }

        final void advance() {
            if (expectedSize != size()) {
                throw new ConcurrentModificationException();
            }
            if (++index>=size()) {
                throw new NoSuchElementException();
            }
            if (mapIterator!=null) {
                current = mapIterator.next();
            }
            else if (expectedSize==1) {
                current = getLogicalSingleKey();
            }
            else {
                current = ((Object [])val)[index*2];
            }
        }

        public final void remove() {
            if (current==EMPTY_MAP) {
                throw new IllegalStateException();
            }
            if (size() != expectedSize)
                throw new ConcurrentModificationException();
            int newSize = expectedSize-1;

            // account for the change in size
            if (mapIterator!=null && newSize==compactSize()) {
                current = ((Map.Entry<K,V>)current).getKey();
                mapIterator = null;
            }

            // perform the remove
            if (mapIterator==null) {
                CompactMap.this.remove(current);
            } else {
                mapIterator.remove();
            }

            index--;
            current = EMPTY_MAP;
            expectedSize--;
        }
    }

    final class CompactKeyIterator extends CompactMap<K, V>.CompactIterator implements Iterator<K>
    {
        public final K next() {
            advance();
            if (mapIterator!=null) {
                return ((Map.Entry<K,V>)current).getKey();
            } else {
                return (K) current;
            }
        }
    }

    final class CompactValueIterator extends CompactMap<K, V>.CompactIterator implements Iterator<V>
    {
        public final V next() {
            advance();
            if (mapIterator != null) {
                return ((Map.Entry<K, V>) current).getValue();
            } else if (expectedSize == 1) {
                return getLogicalSingleValue();
            } else {
                return (V) ((Object[]) val)[(index*2) + 1];
            }
        }
    }

    final class CompactEntryIterator extends CompactMap<K, V>.CompactIterator implements Iterator<Map.Entry<K,V>>
    {
        public final Map.Entry<K,V> next() {
            advance();
            if (mapIterator != null) {
                return (Map.Entry<K, V>) current;
            } else if (expectedSize == 1) {
                if (val instanceof CompactMap.CompactMapEntry) {
                    return (CompactMapEntry) val;
                }
                else {
                    return new CompactMapEntry(getLogicalSingleKey(), getLogicalSingleValue());
                }
            } else {
                Object [] objs = (Object []) val;
                return new CompactMapEntry((K)objs[(index*2)],(V)objs[(index*2) + 1]);
            }
        }
    }

    abstract class CopyIterator {
        Iterator<Entry<K, V>> iter;
        Entry<K, V> currentEntry = null;

        public CopyIterator() {
            iter = getCopy().entrySet().iterator();
        }

        public final boolean hasNext() {
            return iter.hasNext();
        }

        public final Entry<K,V> nextEntry() {
            currentEntry = iter.next();
            return currentEntry;
        }

        public final void remove() {
            iteratorRemove(currentEntry, iter);
            currentEntry = null;
        }
    }

    final class CopyKeyIterator extends CopyIterator
            implements Iterator<K> {
        public K next() { return nextEntry().getKey(); }
    }

    final class CopyValueIterator extends CopyIterator
            implements Iterator<V> {
        public V next() { return nextEntry().getValue(); }
    }

    final class CopyEntryIterator extends CompactMap<K, V>.CopyIterator implements Iterator<Map.Entry<K,V>>
    {
        public Map.Entry<K,V> next() { return nextEntry(); }
    }
}
