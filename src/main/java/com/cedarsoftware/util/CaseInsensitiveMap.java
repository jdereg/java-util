package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * A Map implementation that provides case-insensitive key comparison when the keys are Strings,
 * while preserving the original case of the keys. Non-String keys behave as they would in a normal Map.
 *
 * <p>This implementation is serializable and cloneable. It provides thread-safety guarantees
 * equivalent to the underlying Map implementation used.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * Map<String, String> map = new CaseInsensitiveMap<>();
 * map.put("Hello", "World");
 * assert map.get("hello").equals("World");
 * assert map.get("HELLO").equals("World");
 * </pre>
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * <br>
 * Copyright (c) Cedar Software LLC
 * <br><br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <br><br>
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 * <br><br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class CaseInsensitiveMap<K extends Object, V> implements Map<K, V> {
    private final Map<K, V> map;

    /**
     * Constructs an empty CaseInsensitiveMap with a LinkedHashMap as the underlying
     * implementation, providing predictable iteration order.
     */
    public CaseInsensitiveMap() {
        map = new LinkedHashMap<>();
    }

    /**
     * Constructs an empty CaseInsensitiveMap with the specified initial capacity
     * and a LinkedHashMap as the underlying implementation.
     *
     * @param initialCapacity the initial capacity
     * @throws IllegalArgumentException if the initial capacity is negative
     */
    public CaseInsensitiveMap(int initialCapacity) {
        map = new LinkedHashMap<>(initialCapacity);
    }

    /**
     * Constructs an empty CaseInsensitiveMap with the specified initial capacity
     * and load factor, using a LinkedHashMap as the underlying implementation.
     *
     * @param initialCapacity the initial capacity
     * @param loadFactor      the load factor
     * @throws IllegalArgumentException if the initial capacity is negative or the load factor is negative
     */
    public CaseInsensitiveMap(int initialCapacity, float loadFactor) {
        map = new LinkedHashMap<>(initialCapacity, loadFactor);
    }

    /**
     * Creates a CaseInsensitiveMap by copying entries from the specified source map into
     * the specified destination map implementation.
     *
     * @param source      the map containing entries to be copied
     * @param mapInstance the empty map instance to use as the underlying implementation
     * @throws NullPointerException     if either map is null
     * @throws IllegalArgumentException if mapInstance is not empty
     */
    public CaseInsensitiveMap(Map<K, V> source, Map<K, V> mapInstance) {
        map = copy(source, mapInstance);
    }

    public CaseInsensitiveMap(Map<K, V> source) {
        Objects.requireNonNull(source, "Source map cannot be null");
        int size = source.size();

        // Concrete implementations (most specific to most general)
        if (source instanceof Hashtable && !(source instanceof Properties)) {
            map = copy(source, new Hashtable<>(size));
        } else if (source instanceof TreeMap) {
            map = copy(source, new TreeMap<>());
        } else if (source instanceof ConcurrentSkipListMap) {
            map = copy(source, new ConcurrentSkipListMap<>());
        } else if (source instanceof IdentityHashMap) {
            map = copy(source, new IdentityHashMap<>(size));
        } else if (source instanceof WeakHashMap) {
            map = copy(source, new WeakHashMap<>(size));
        } else if (source instanceof LinkedHashMap) {
            map = copy(source, new LinkedHashMap<>(size));
        } else if (source instanceof HashMap) {
            map = copy(source, new HashMap<>(size));
        }

        // Custom implementations
        else if (source instanceof ConcurrentNavigableMapNullSafe) {
            map = copy(source, new ConcurrentNavigableMapNullSafe<>());
        } else if (source instanceof ConcurrentHashMapNullSafe) {
            map = copy(source, new ConcurrentHashMapNullSafe<>(size));
        }

        // Interface implementations (most specific to most general)
        else if (source instanceof ConcurrentNavigableMap) {
            map = copy(source, new ConcurrentSkipListMap<>());
        } else if (source instanceof ConcurrentMap) {
            map = copy(source, new ConcurrentHashMap<>(size));
        } else if (source instanceof NavigableMap) {
            map = copy(source, new TreeMap<>());
        } else if (source instanceof SortedMap) {
            map = copy(source, new TreeMap<>());
        }

        // Default case
        else {
            map = copy(source, new LinkedHashMap<>(size));
        }
    }

    /**
     * Creates a case-insensitive map initialized with the entries from the specified source map.
     * The created map preserves the characteristics of the source map by using a similar implementation type.
     * For example, if the source map is a TreeMap, the internal map will be created as a TreeMap to maintain
     * ordering and performance characteristics.
     *
     * <p>The constructor intelligently selects the appropriate map implementation based on the source map's type,
     * following this matching strategy:</p>
     *
     * <ul>
     *   <li>Concrete implementations are matched exactly (e.g., Hashtable → Hashtable, TreeMap → TreeMap)</li>
     *   <li>Custom implementations are preserved (e.g., ConcurrentNavigableMapNullSafe)</li>
     *   <li>Interface implementations are matched to their most appropriate concrete type
     *       (e.g., ConcurrentMap → ConcurrentHashMap)</li>
     *   <li>Defaults to LinkedHashMap if no specific match is found</li>
     * </ul>
     *
     * <p>All String keys in the source map are copied to the new map and will be handled case-insensitively
     * for subsequent operations. Non-String keys maintain their original case sensitivity.</p>
     *
     * @param source the map whose mappings are to be placed in this map. Must not be null.
     *               The source map is not modified by this operation.
     * @throws NullPointerException if the source map is null
     * @see Hashtable
     * @see TreeMap
     * @see ConcurrentSkipListMap
     * @see IdentityHashMap
     * @see WeakHashMap
     * @see LinkedHashMap
     * @see HashMap
     * @see ConcurrentHashMap
     */
    @SuppressWarnings("unchecked")
    protected Map<K, V> copy(Map<K, V> source, Map<K, V> dest) {
        for (Entry<K, V> entry : source.entrySet()) {
            // Get key from Entry, preserving CaseInsensitiveString instances
            K key = entry.getKey();
            if (isCaseInsensitiveEntry(entry)) {
                key = ((CaseInsensitiveEntry) entry).getOriginalKey();
            } else if (key instanceof String) {
                key = (K) new CaseInsensitiveString((String) key);
            }

            dest.put(key, entry.getValue());
        }
        return dest;
    }

    /**
     * Tests if an object is an instance of CaseInsensitiveEntry.
     *
     * @param o the object to test
     * @return true if the object is an instance of CaseInsensitiveEntry, false otherwise
     * @see CaseInsensitiveEntry
     */
    private boolean isCaseInsensitiveEntry(Object o) {
        return CaseInsensitiveEntry.class.isInstance(o);
    }

    /**
     * Returns the value to which the specified key is mapped,
     * or null if this map contains no mapping for the key.
     * String keys are handled case-insensitively.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     * null if this map contains no mapping for the key
     */
    public V get(Object key) {
        if (key instanceof String) {
            String keyString = (String) key;
            return map.get(new CaseInsensitiveString(keyString));
        }
        return map.get(key);
    }

    /**
     * Returns true if this map contains a mapping for the specified key.
     * String keys are handled case-insensitively.
     *
     * @param key key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key,
     * false otherwise
     */
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            String keyString = (String) key;
            return map.containsKey(new CaseInsensitiveString(keyString));
        }
        return map.containsKey(key);
    }

    /**
     * Returns true if this map contains a mapping for the specified key.
     * String keys are handled case-insensitively.
     *
     * @param key key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key,
     * false otherwise
     */
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        if (key instanceof String) {
            final CaseInsensitiveString newKey = new CaseInsensitiveString((String) key);
            return map.put((K) newKey, value);
        }
        return map.put(key, value);
    }

    /**
     * Copies all the mappings from the specified map to this map.
     * String keys will be stored and accessed case-insensitively.
     * If the specified map is a CaseInsensitiveMap, the original case
     * of its keys is preserved.
     *
     * @param m mappings to be stored in this map
     * @throws NullPointerException if any of the entries' keys or values is null
     *                              and this map does not permit null keys or values
     */
    @SuppressWarnings("unchecked")
    public void putAll(Map<? extends K, ? extends V> m) {
        if (MapUtilities.isEmpty(m)) {
            return;
        }

        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            if (isCaseInsensitiveEntry(entry)) {
                CaseInsensitiveEntry ciEntry = (CaseInsensitiveEntry) entry;
                put(ciEntry.getOriginalKey(), entry.getValue());
            } else {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Removes the mapping for the specified key from this map if present.
     * String keys are handled case-insensitively.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with key, or null if there
     * was no mapping for key
     */
    public V remove(Object key) {
        if (key instanceof String) {
            String keyString = (String) key;
            return map.remove(new CaseInsensitiveString(keyString));
        }
        return map.remove(key);
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the number of key-value mappings in this map
     */
    public int size() {
        return map.size();
    }

    /**
     * Returns true if this map contains no key-value mappings.
     *
     * @return true if this map contains no key-value mappings
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Compares the specified object with this map for equality.
     * Returns true if the given object is also a map and the two maps
     * represent the same mappings, with case-insensitive comparison
     * of String keys.
     *
     * <p>Two maps represent the same mappings if they contain the same
     * mappings as each other. String keys are compared case-insensitively,
     * while other key types are compared normally.</p>
     *
     * @param other object to be compared for equality with this map
     * @return true if the specified object is equal to this map,
     * false otherwise
     */
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Map)) {
            return false;
        }

        Map<?, ?> that = (Map<?, ?>) other;
        if (that.size() != size()) {
            return false;
        }

        for (Entry<?, ?> entry : that.entrySet()) {
            final Object thatKey = entry.getKey();
            if (!containsKey(thatKey)) {
                return false;
            }

            Object thatValue = entry.getValue();
            Object thisValue = get(thatKey);
            if (!Objects.equals(thisValue, thatValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the hash code value for this map. The hash code is computed
     * such that two CaseInsensitiveMap instances that are equal according
     * to equals() will have the same hash code.
     *
     * <p>The hash code for String keys is computed case-insensitively,
     * while other key types use their natural hash codes.</p>
     *
     * @return the hash code value for this map
     * @see #equals(Object)
     */
    public int hashCode() {
        int h = 0;
        for (Entry<K, V> entry : map.entrySet()) {
            Object key = entry.getKey();
            int hKey = key == null ? 0 : key.hashCode();
            Object value = entry.getValue();
            int hValue = value == null ? 0 : value.hashCode();
            h += hKey ^ hValue;
        }
        return h;
    }

    /**
     * Returns a string representation of this map. The string representation
     * consists of a list of key-value mappings in the order returned by the
     * map's entrySet view's iterator, enclosed in braces ("{}"). Adjacent
     * mappings are separated by the characters ", " (comma and space).
     *
     * @return a string representation of this map
     */
    public String toString() {
        return map.toString();
    }

    /**
     * Removes all the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        map.clear();
    }

    /**
     * Returns true if this map maps one or more keys to the specified value.
     * This operation will require time linear in the map size.
     *
     * @param value value whose presence in this map is to be tested
     * @return true if this map maps one or more keys to the specified value,
     * false otherwise
     */
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    /**
     * Returns a Collection view of the values contained in this map.
     * The collection is backed by the map, so changes to the map are
     * reflected in the collection, and vice versa.
     *
     * @return a collection view of the values contained in this map
     */
    public Collection<V> values() {
        return map.values();
    }

    /**
     * Returns the underlying map that this CaseInsensitiveMap wraps.
     * The returned map contains the internal CaseInsensitiveString
     * representations for String keys.
     *
     * @return the wrapped map containing the actual key-value mappings
     */
    public Map<K, V> getWrappedMap() {
        return map;
    }

    /**
     * Returns a Set view of the keys contained in this map.
     * The set is backed by the map, so changes to either will be reflected in both.
     * For String keys, they are returned in their original form rather than their
     * case-insensitive representation.
     *
     * <p>The returned set supports the following operations that modify the underlying map:
     * <ul>
     *   <li>remove(Object)</li>
     *   <li>removeAll(Collection)</li>
     *   <li>retainAll(Collection)</li>
     *   <li>clear()</li>
     * </ul>
     *
     * <p>The iteration behavior, including whether changes to the map during iteration
     * result in ConcurrentModificationException, depends on the underlying map implementation.
     *
     * @return a Set view of the keys contained in this map
     */
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            /**
             * Tests if the specified object is a key in this set.
             * String keys are compared case-insensitively.
             *
             * @param o object to be checked for containment in this set
             * @return true if this set contains the specified object
             */
            @Override
            public boolean contains(Object o) {
                return CaseInsensitiveMap.this.containsKey(o);
            }

            /**
             * Removes the specified object from this set if it is present.
             * Returns true if the set contained the specified element.
             *
             * @param o object to be removed from this set, if present
             * @return true if the set contained the specified element
             */
            @Override
            public boolean remove(Object o) {
                final int size = map.size();
                CaseInsensitiveMap.this.remove(o);
                return map.size() != size;
            }

            /**
             * Removes from this set all of its elements that are contained in the
             * specified collection. String comparisons are case-insensitive.
             *
             * @param c collection containing elements to be removed from this set
             * @return true if this set changed as a result of the call
             * @throws NullPointerException if the specified collection is null
             */
            @Override
            public boolean removeAll(Collection c) {
                int size = map.size();
                for (Object o : c) {
                    CaseInsensitiveMap.this.remove(o);
                }
                return map.size() != size;
            }

            /**
             * Retains only the elements in this set that are contained in the
             * specified collection. String comparisons are case-insensitive.
             *
             * @param c collection containing elements to be retained in this set
             * @return true if this set changed as a result of the call
             * @throws NullPointerException if the specified collection is null
             */
            @Override
            @SuppressWarnings("unchecked")
            public boolean retainAll(Collection<?> c) {
                Map<K, V> other = new CaseInsensitiveMap<>();
                for (Object o : c) {
                    other.put((K) o, null);
                }

                final int size = map.size();
                map.keySet().removeIf(key -> !other.containsKey(key));

                return map.size() != size;
            }

            /**
             * Returns an array containing all the elements in this set.
             * String keys are returned in their original form.
             *
             * @return an array containing all the elements in this set
             */
            @Override
            @SuppressWarnings("unchecked")
            public <T> T[] toArray(T[] a) {
                int size = size();
                T[] result = a.length >= size ? a :
                        (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);

                int i = 0;
                for (Object key : map.keySet()) {
                    result[i++] = (T) (key instanceof CaseInsensitiveString ? key.toString() : key);
                }

                if (result.length > size) {
                    result[size] = null;
                }
                return result;
            }

            /**
             * Returns the number of keys in this set (equal to the size of the map).
             *
             * @return the number of elements in this set
             */
            @Override
            public int size() {
                return map.size();
            }

            /**
             * Removes all elements from this set (clears the map).
             */
            @Override
            public void clear() {
                map.clear();
            }

            /**
             * Returns a hash code value for this set, based on the case-insensitive
             * hash codes of its elements.
             *
             * @return the hash code value for this set
             */
            @Override
            public int hashCode() {
                int h = 0;

                // Use map.keySet() so that we walk through the CaseInsensitiveStrings generating a hashCode
                // that is based on the lowerCase() value of the Strings (hashCode() on the CaseInsensitiveStrings
                // with map.keySet() will return the hashCode of .toLowerCase() of those strings).
                for (Object key : map.keySet()) {
                    if (key != null) {
                        h += key.hashCode();
                    }
                }
                return h;
            }

            /**
             * Returns an iterator over the keys in this set. The iterator returns
             * String keys in their original form rather than their case-insensitive
             * representation.
             *
             * @return an iterator over the elements in this set
             */
            @Override
            @SuppressWarnings("unchecked")
            public Iterator<K> iterator() {
                return new Iterator<K>() {
                    private final Iterator<?> iter = map.keySet().iterator();

                    /**
                     * Removes from the underlying map the last element returned
                     * by this iterator.
                     *
                     * @throws IllegalStateException if the next method has not yet been called,
                     *         or the remove method has already been called after the last call
                     *         to the next method
                     */
                    @Override
                    public void remove() {
                        iter.remove();
                    }

                    /**
                     * Returns true if the iteration has more elements.
                     *
                     * @return true if the iteration has more elements
                     */
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    /**
                     * Returns the next element in the iteration. String keys
                     * are returned in their original form rather than their
                     * case-insensitive representation.
                     *
                     * @return the next element in the iteration
                     */
                    @Override
                    public K next() {
                        Object next = iter.next();
                        return (K) (next instanceof CaseInsensitiveString ? next.toString() : next);
                    }
                };
            }
        };
    }

    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public int size() {
                return map.size();
            }

            @Override
            public boolean isEmpty() {
                return map.isEmpty();
            }

            @Override
            public void clear() {
                map.clear();
            }

            @SuppressWarnings("unchecked")
            public boolean contains(Object o) {
                if (!(o instanceof Entry)) {
                    return false;
                }
                Entry<K, V> that = (Entry<K, V>) o;
                Object value = CaseInsensitiveMap.this.get(that.getKey());
                return value != null ? value.equals(that.getValue())
                        : that.getValue() == null && CaseInsensitiveMap.this.containsKey(that.getKey());
            }

            public Object[] toArray() {
                Object[] result = new Object[size()];
                int i = 0;
                for (Entry<K, V> entry : map.entrySet()) {
                    result[i++] = new CaseInsensitiveEntry(entry);
                }
                return result;
            }
            
            @SuppressWarnings("unchecked")
            public <T> T[] toArray(T[] a) {
                // Create array of the appropriate size
                int size = size();
                T[] result = a.length >= size ? a :
                        (T[]) Array.newInstance(a.getClass().getComponentType(), size);

                // Fill the array
                Iterator<Entry<K,V>> it = map.entrySet().iterator();
                for (int i = 0; i < size; i++) {
                    result[i] = (T) new CaseInsensitiveEntry(it.next());
                }

                // Null out remaining elements if array was larger
                if (result.length > size) {
                    result[size] = null;
                }

                return result;
            }
            
            @SuppressWarnings("unchecked")
            public boolean remove(Object o) {
                if (!(o instanceof Entry)) {
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
             * and remove() are both hashed O(1) look-ups.
             */
            @Override
            @SuppressWarnings("unchecked")
            public boolean removeAll(Collection<?> c) {
                final int size = map.size();
                for (Object o : c) {
                    if (o instanceof Entry) {
                        try {
                            Entry<K, V> that = (Entry<K, V>) o;
                            CaseInsensitiveMap.this.remove(that.getKey());
                        } catch (ClassCastException ignored) {
                            // Skip entries that can't be cast to Entry<K,V>
                        }
                    }
                }
                return map.size() != size;
            }
            
            @SuppressWarnings("unchecked")
            public boolean retainAll(Collection c) {
                if (c.isEmpty()) {  // special case for performance.
                    int size = size();
                    clear();
                    return size > 0;
                }
                Map<K, V> other = new CaseInsensitiveMap<>();
                for (Object o : c) {
                    if (o instanceof Entry) {
                        Entry<K, V> entry = (Entry<K, V>) o;
                        other.put(entry.getKey(), entry.getValue());
                    }
                }

                int originalSize = size();
                map.entrySet().removeIf(entry ->
                        !other.containsKey(entry.getKey()) ||
                                !Objects.equals(other.get(entry.getKey()), entry.getValue())
                );
                return size() != originalSize;
            }

            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<Entry<K, V>>() {
                    private final Iterator<Entry<K, V>> iter = map.entrySet().iterator();
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
     * <p></p>
     * Also, when the setValue() API is called on the Entry, it will 'write thru' to the
     * underlying Map's value.
     */
    public class CaseInsensitiveEntry extends AbstractMap.SimpleEntry<K, V> {
        public CaseInsensitiveEntry(Entry<K, V> entry) {
            super(entry);
        }

        @SuppressWarnings("unchecked")
        public K getKey() {
            K superKey = super.getKey();
            if (superKey instanceof CaseInsensitiveString) {
                return (K) ((CaseInsensitiveString) superKey).original;
            }
            return superKey;
        }

        public K getOriginalKey() {
            return super.getKey();
        }

        public V setValue(V value) {
            return map.put(super.getKey(), value);
        }
    }

    /**
     * Class used to wrap String keys.  This class ignores the
     * case of Strings when they are compared.  Based on known usage,
     * null checks, proper instance, etc. are dropped.
     */
    public static final class CaseInsensitiveString implements Comparable {
        private final String original;
        private final int hash;

        public CaseInsensitiveString(String string) {
            original = string;
            hash = StringUtilities.hashCodeIgnoreCase(string);  // no new String created unlike .toLowerCase()
        }

        public String toString() {
            return original;
        }

        public int hashCode() {
            return hash;
        }

        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (other instanceof CaseInsensitiveString) {
                return hash == ((CaseInsensitiveString) other).hash &&
                        original.equalsIgnoreCase(((CaseInsensitiveString) other).original);
            }
            if (other instanceof String) {
                return original.equalsIgnoreCase((String) other);
            }
            return false;
        }

        public int compareTo(Object o) {
            if (o instanceof CaseInsensitiveString) {
                CaseInsensitiveString other = (CaseInsensitiveString) o;
                return original.compareToIgnoreCase(other.original);
            }
            if (o instanceof String) {
                String other = (String) o;
                return original.compareToIgnoreCase(other);
            }
            // Strings are less than non-Strings (come before)
            return -1;
        }
    }
}
