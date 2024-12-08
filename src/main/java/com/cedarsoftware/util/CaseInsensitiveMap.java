package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A Map implementation that provides case-insensitive key comparison when the keys are Strings,
 * while preserving the original case of the keys. Non-String keys behave as they would in a normal Map.
 *
 * <p>This class attempts to preserve the behavior of the source map implementation when constructed
 * from another map. For example, if constructed from a TreeMap, the internal map will be a TreeMap.</p>
 *
 * <p>All String keys are internally stored as {@link CaseInsensitiveString}, which provides
 * case-insensitive equals/hashCode. Retrieval and access methods convert to/from this form,
 * ensuring that String keys are matched case-insensitively.</p>
 *
 * <p>This class also provides overrides for Java 8+ map methods such as {@code computeIfAbsent()},
 * {@code computeIfPresent()}, {@code merge()}, etc., ensuring that keys are treated
 * case-insensitively for these operations as well.</p>
 *
 * @param <K> the type of keys maintained by this map (usually String for case-insensitive behavior)
 * @param <V> the type of mapped values
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
public class CaseInsensitiveMap<K extends Object, V> extends AbstractMap<K, V> {
    private final Map<K, V> map;
    private final static LRUCache<String, CaseInsensitiveString> ciStringCache = new LRUCache<>(1000);
    
    /**
     * Registry of known source map types to their corresponding factory functions.
     * Uses CopyOnWriteArrayList to maintain thread safety and preserve insertion order.
     * More specific types should be registered before more general ones.
     */
    private static volatile List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> mapRegistry;

    static {
        // Initialize the registry with default map types
        List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> tempList = new ArrayList<>();
        tempList.add(new AbstractMap.SimpleEntry<>(Hashtable.class, size -> new Hashtable<>()));
        tempList.add(new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>()));
        tempList.add(new AbstractMap.SimpleEntry<>(ConcurrentSkipListMap.class, size -> new ConcurrentSkipListMap<>()));
        tempList.add(new AbstractMap.SimpleEntry<>(WeakHashMap.class, size -> new WeakHashMap<>(size)));
        tempList.add(new AbstractMap.SimpleEntry<>(LinkedHashMap.class, size -> new LinkedHashMap<>(size)));
        tempList.add(new AbstractMap.SimpleEntry<>(HashMap.class, size -> new HashMap<>(size)));
        tempList.add(new AbstractMap.SimpleEntry<>(ConcurrentNavigableMapNullSafe.class, size -> new ConcurrentNavigableMapNullSafe<>()));
        tempList.add(new AbstractMap.SimpleEntry<>(ConcurrentHashMapNullSafe.class, size -> new ConcurrentHashMapNullSafe<>(size)));
        tempList.add(new AbstractMap.SimpleEntry<>(ConcurrentNavigableMap.class, size -> new ConcurrentSkipListMap<>()));
        tempList.add(new AbstractMap.SimpleEntry<>(ConcurrentMap.class, size -> new ConcurrentHashMap<>(size)));
        tempList.add(new AbstractMap.SimpleEntry<>(NavigableMap.class, size -> new TreeMap<>()));
        tempList.add(new AbstractMap.SimpleEntry<>(SortedMap.class, size -> new TreeMap<>()));

        validateMappings(tempList);
        // Convert to unmodifiable list to prevent accidental modifications
        mapRegistry = Collections.unmodifiableList(tempList);
    }

    /**
     * Validates that collection type mappings are ordered correctly (most specific to most general)
     * and ensures that unsupported map types like IdentityHashMap are not included.
     * Throws IllegalStateException if mappings are incorrectly ordered or contain unsupported types.
     *
     * @param registry the registry list to validate
     */
    private static void validateMappings(List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry) {
        for (int i = 0; i < registry.size(); i++) {
            Class<?> current = registry.get(i).getKey();

            // Check for unsupported map types
            if (current.equals(IdentityHashMap.class)) {
                throw new IllegalStateException("IdentityHashMap is not supported and cannot be added to the registry.");
            }

            for (int j = i + 1; j < registry.size(); j++) {
                Class<?> next = registry.get(j).getKey();
                if (current.isAssignableFrom(next)) {
                    throw new IllegalStateException("Mapping order error: " + next.getName() + " should come before " + current.getName());
                }
            }
        }
    }

    /**
     * Allows users to replace the entire registry with a new list of map type entries.
     * This should typically be done at startup before any CaseInsensitiveMap instances are created.
     *
     * @param newRegistry the new list of map type entries
     * @throws NullPointerException     if newRegistry is null or contains null elements
     * @throws IllegalArgumentException if newRegistry contains duplicate Class types or is incorrectly ordered
     */
    public static void replaceRegistry(List<Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> newRegistry) {
        Objects.requireNonNull(newRegistry, "New registry list cannot be null");
        for (Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>> entry : newRegistry) {
            Objects.requireNonNull(entry, "Registry entries cannot be null");
            Objects.requireNonNull(entry.getKey(), "Registry entry key (Class) cannot be null");
            Objects.requireNonNull(entry.getValue(), "Registry entry value (Function) cannot be null");
        }

        // Check for duplicate Class types
        Set<Class<?>> seen = new HashSet<>();
        for (Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>> entry : newRegistry) {
            if (!seen.add(entry.getKey())) {
                throw new IllegalArgumentException("Duplicate map type in registry: " + entry.getKey());
            }
        }

        // Validate mapping order
        validateMappings(newRegistry);

        // Replace the registry atomically with an unmodifiable copy
        mapRegistry = Collections.unmodifiableList(new ArrayList<>(newRegistry));
    }

    /**
     * Determines the appropriate backing map based on the source map's type.
     *
     * @param source the source map to copy from
     * @return a new Map instance with entries copied from the source
     * @throws IllegalArgumentException if the source map is an IdentityHashMap
     */
    protected Map<K, V> determineBackingMap(Map<K, V> source) {
        if (source instanceof IdentityHashMap) {
            throw new IllegalArgumentException(
                    "Cannot create a CaseInsensitiveMap from an IdentityHashMap. " +
                            "IdentityHashMap compares keys by reference (==) which is incompatible.");
        }

        int size = source.size();

        // Iterate through the registry and pick the first matching type
        for (Map.Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>> entry : mapRegistry) {
            if (entry.getKey().isInstance(source)) {
                @SuppressWarnings("unchecked")
                Function<Integer, Map<K, V>> factory = (Function<Integer, Map<K, V>>) entry.getValue();
                return copy(source, factory.apply(size));
            }
        }

        // If no match found, default to LinkedHashMap
        return copy(source, new LinkedHashMap<>(size));
    }

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

    /**
     * Creates a case-insensitive map initialized with the entries from the specified source map.
     * The created map preserves the characteristics of the source map by using a similar implementation type.
     *
     * <p>Concrete or known map types are matched to their corresponding internal maps (e.g. TreeMap to TreeMap).
     * If no specific match is found, a LinkedHashMap is used by default.</p>
     *
     * @param source the map whose mappings are to be placed in this map. Must not be null.
     * @throws NullPointerException if the source map is null
     */
    public CaseInsensitiveMap(Map<K, V> source) {
        Objects.requireNonNull(source, "Source map cannot be null");
        map = determineBackingMap(source);
    }

    /**
     * Copies all entries from the source map to the destination map, wrapping String keys as needed.
     *
     * @param source the map whose entries are being copied
     * @param dest   the destination map
     * @return the populated destination map
     */
    @SuppressWarnings("unchecked")
    protected Map<K, V> copy(Map<K, V> source, Map<K, V> dest) {
        for (Entry<K, V> entry : source.entrySet()) {
            K key = entry.getKey();
            if (isCaseInsensitiveEntry(entry)) {
                key = ((CaseInsensitiveEntry) entry).getOriginalKey();
            } else if (key instanceof String) {
                key = (K) newCIString((String) key);
            }
            dest.put(key, entry.getValue());
        }
        return dest;
    }

    /**
     * Checks if the given object is a CaseInsensitiveEntry.
     *
     * @param o the object to test
     * @return true if o is a CaseInsensitiveEntry, false otherwise
     */
    private boolean isCaseInsensitiveEntry(Object o) {
        return CaseInsensitiveEntry.class.isInstance(o);
    }

    /**
     * {@inheritDoc}
     * <p>String keys are handled case-insensitively.</p>
     */
    @Override
    public V get(Object key) {
        if (key instanceof String) {
            return map.get(newCIString((String) key));
        }
        return map.get(key);
    }

    /**
     * {@inheritDoc}
     * <p>String keys are handled case-insensitively.</p>
     */
    @Override
    public boolean containsKey(Object key) {
        if (key instanceof String) {
            return map.containsKey(newCIString((String) key));
        }
        return map.containsKey(key);
    }

    /**
     * {@inheritDoc}
     * <p>String keys are stored case-insensitively.</p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public V put(K key, V value) {
        if (key instanceof String) {
            return map.put((K) newCIString((String) key), value);
        }
        return map.put(key, value);
    }
    
    /**
     * {@inheritDoc}
     * <p>String keys are handled case-insensitively.</p>
     */
    @Override
    public V remove(Object key) {
        if (key instanceof String) {
            return map.remove(newCIString((String) key));
        }
        return map.remove(key);
    }

    /**
     * {@inheritDoc}
     * <p>Equality is based on case-insensitive comparison for String keys.</p>
     */
    @Override
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
            Object thatKey = entry.getKey();
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
     * Returns the underlying wrapped map instance. This map contains the keys in their
     * case-insensitive form (i.e., {@link CaseInsensitiveString} for String keys).
     *
     * @return the wrapped map
     */
    public Map<K, V> getWrappedMap() {
        return map;
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map. The set is backed by the
     * map, so changes to the map are reflected in the set, and vice-versa. For String keys,
     * the set contains the original Strings rather than their case-insensitive representations.
     *
     * @return a set view of the keys contained in this map
     */
    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            /**
             * Returns an iterator over the keys in this set. For String keys, the iterator
             * returns the original Strings rather than their case-insensitive representations.
             *
             * @return an iterator over the keys in this set
             */
            @Override
            public Iterator<K> iterator() {
                return new Iterator<K>() {
                    private final Iterator<K> iter = map.keySet().iterator();

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    /**
                     * Returns the next key in the iteration. For String keys, returns the
                     * original String rather than its case-insensitive representation.
                     *
                     * @return the next key in the iteration
                     * @throws java.util.NoSuchElementException if the iteration has no more elements
                     */
                    @Override
                    @SuppressWarnings("unchecked")
                    public K next() {
                        K next = iter.next();
                        return (K) (next instanceof CaseInsensitiveString ? next.toString() : next);
                    }

                    /**
                     * {@inheritDoc}
                     */
                    @Override
                    public void remove() {
                        iter.remove();
                    }
                };
            }

            /**
             * Computes a hash code for this set. The hash code of a set is defined as the
             * sum of the hash codes of its elements. For null elements, no value is added
             * to the sum. The hash code computation is case-insensitive, as it relies on
             * the case-insensitive hash code implementation of the underlying keys.
             *
             * @return the hash code value for this set
             */
            @Override
            public int hashCode() {
                int h = 0;
                for (Object key : map.keySet()) {
                    if (key != null) {
                        h += key.hashCode();  // CaseInsensitiveString's hashCode() is already case-insensitive
                    }
                }
                return h;
            }

            /**
             * Returns the number of elements in this set (its cardinality).
             * This method delegates to the size of the underlying map.
             *
             * @return the number of elements in this set
             */
            @Override
            public int size() {
                return map.size();
            }

            /**
             * Returns true if this set contains the specified element.
             * This operation is equivalent to checking if the specified object
             * exists as a key in the map, using case-insensitive comparison.
             *
             * @param o element whose presence in this set is to be tested
             * @return true if this set contains the specified element
             */
            @Override
            public boolean contains(Object o) {
                return containsKey(o);
            }

            /**
             * Removes the specified element from this set if it is present.
             * This operation removes the corresponding entry from the underlying map.
             * The item to be removed is located case-insensitively if the element is a String.
             * The method returns true if the set contained the specified element
             * (or equivalently, if the map was modified as a result of the call).
             *
             * @param o object to be removed from this set, if present
             * @return true if the set contained the specified element
             */
            @Override
            public boolean remove(Object o) {
                int size = map.size();
                CaseInsensitiveMap.this.remove(o);
                return map.size() != size;
            }

            /**
             * Returns an array containing all the keys in this set; the runtime type of the returned
             * array is that of the specified array. If the set fits in the specified array, it is
             * returned therein. Otherwise, a new array is allocated with the runtime type of the
             * specified array and the size of this set.
             *
             * <p>If the set fits in the specified array with room to spare (i.e., the array has more
             * elements than the set), the element in the array immediately following the end of the set
             * is set to null. This is useful in determining the length of the set only if the caller
             * knows that the set does not contain any null elements.
             *
             * <p>String keys are returned in their original form rather than their case-insensitive
             * representation used internally by the map.
             *
             * <p>This method could be remove and the parent class method would work, however, it's more efficient:
             * It works directly with the backing map's keyset instead of using an iterator.
             *
             * @param a the array into which the elements of this set are to be stored,
             *          if it is big enough; otherwise, a new array of the same runtime
             *          type is allocated for this purpose
             * @return an array containing the elements of this set
             * @throws ArrayStoreException if the runtime type of the specified array
             *         is not a supertype of the runtime type of every element in this set
             * @throws NullPointerException if the specified array is null
             */
            @Override
            @SuppressWarnings("unchecked")
            public <T> T[] toArray(T[] a) {
                int size = size();
                T[] result = a.length >= size ? a :
                        (T[]) Array.newInstance(a.getClass().getComponentType(), size);

                int i = 0;
                for (K key : map.keySet()) {
                    result[i++] = (T) (key instanceof CaseInsensitiveString ? key.toString() : key);
                }

                if (result.length > size) {
                    result[size] = null;
                }
                return result;
            }

            /**
             * <p>Retains only the elements in this set that are contained in the specified collection.
             * In other words, removes from this set all of its elements that are not contained
             * in the specified collection. The comparison is case-insensitive.
             *
             * <p>This operation creates a temporary CaseInsensitiveMap to perform case-insensitive
             * comparison of elements, then removes all keys from the underlying map that are not
             * present in the specified collection.
             *
             * @param c collection containing elements to be retained in this set
             * @return true if this set changed as a result of the call
             * @throws ClassCastException if the types of one or more elements in this set
             *         are incompatible with the specified collection
             * @SuppressWarnings("unchecked") suppresses unchecked cast warnings as elements
             *         are assumed to be of type K
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
        };
    }
    
    /**
     * {@inheritDoc}
     * <p>Returns a Set view of the entries contained in this map. Each entry returns its key in the
     * original String form (if it was a String). Operations on this set affect the underlying map.</p>
     */
    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            /**
             * {@inheritDoc}
             * <p>Returns the number of entries in the underlying map.</p>
             */
            @Override
            public int size() {
                return map.size();
            }

            /**
             * {@inheritDoc}
             * <p>Determines if the specified object is an entry present in the map. String keys are
             * matched case-insensitively.</p>
             */
            @Override
            @SuppressWarnings("unchecked")
            public boolean contains(Object o) {
                if (!(o instanceof Entry)) {
                    return false;
                }
                Entry<K, V> that = (Entry<K, V>) o;
                Object value = get(that.getKey());
                return value != null ? value.equals(that.getValue())
                        : that.getValue() == null && containsKey(that.getKey());
            }

            /**
             * {@inheritDoc}
             * <p>Returns an array containing all the entries in this set. Each entry returns its key in the
             * original String form if it was originally a String.</p>
             */
            @Override
            public Object[] toArray() {
                Object[] result = new Object[size()];
                int i = 0;
                for (Entry<K, V> entry : map.entrySet()) {
                    result[i++] = new CaseInsensitiveEntry(entry);
                }
                return result;
            }

            /**
             * {@inheritDoc}
             * <p>Returns an array containing all the entries in this set. The runtime type of the returned
             * array is that of the specified array.</p>
             */
            @Override
            @SuppressWarnings("unchecked")
            public <T> T[] toArray(T[] a) {
                int size = size();
                T[] result = a.length >= size ? a :
                        (T[]) Array.newInstance(a.getClass().getComponentType(), size);

                Iterator<Entry<K, V>> it = map.entrySet().iterator();
                for (int i = 0; i < size; i++) {
                    result[i] = (T) new CaseInsensitiveEntry(it.next());
                }

                if (result.length > size) {
                    result[size] = null;
                }

                return result;
            }

            /**
             * {@inheritDoc}
             * <p>Removes the specified entry from the underlying map if present.</p>
             */
            @Override
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
             * {@inheritDoc}
             * <p>Removes all entries in the specified collection from the underlying map, if present.</p>
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
                            // Ignore entries that cannot be cast
                        }
                    }
                }
                return map.size() != size;
            }

            /**
             * {@inheritDoc}
             * <p>Retains only the entries in this set that are contained in the specified collection.</p>
             */
            @Override
            @SuppressWarnings("unchecked")
            public boolean retainAll(Collection<?> c) {
                if (c.isEmpty()) {
                    int oldSize = size();
                    clear();
                    return oldSize > 0;
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

            /**
             * {@inheritDoc}
             * <p>Returns an iterator over the entries in the map. Each returned entry will provide
             * the key in its original form if it was originally a String.</p>
             */
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<Entry<K, V>>() {
                    private final Iterator<Entry<K, V>> iter = map.entrySet().iterator();

                    /**
                     * {@inheritDoc}
                     * <p>Returns true if there are more entries to iterate over.</p>
                     */
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    /**
                     * {@inheritDoc}
                     * <p>Returns the next entry. The key will be returned in its original case if it was a String.</p>
                     */
                    @Override
                    public Entry<K, V> next() {
                        return new CaseInsensitiveEntry(iter.next());
                    }

                    /**
                     * {@inheritDoc}
                     * <p>Removes the last returned entry from the underlying map.</p>
                     */
                    @Override
                    public void remove() {
                        iter.remove();
                    }
                };
            }
        };
    }
    
    /**
     * Entry implementation that returns a String key rather than a CaseInsensitiveString
     * when {@link #getKey()} is called.
     */
    public class CaseInsensitiveEntry extends AbstractMap.SimpleEntry<K, V> {
        /**
         * Constructs a CaseInsensitiveEntry from the specified entry.
         *
         * @param entry the entry to wrap
         */
        public CaseInsensitiveEntry(Entry<K, V> entry) {
            super(entry);
        }

        /**
         * {@inheritDoc}
         * <p>Returns the key in its original String form if it was originally stored as a String,
         * otherwise returns the key as is.</p>
         */
        @Override
        @SuppressWarnings("unchecked")
        public K getKey() {
            K superKey = super.getKey();
            if (superKey instanceof CaseInsensitiveString) {
                return (K) ((CaseInsensitiveString) superKey).original;
            }
            return superKey;
        }

        /**
         * Returns the original key object used internally by the map. This may be a CaseInsensitiveString
         * if the key was originally a String.
         *
         * @return the original key object
         */
        public K getOriginalKey() {
            return super.getKey();
        }

        /**
         * {@inheritDoc}
         * <p>Sets the value associated with this entry's key in the underlying map.</p>
         */
        @Override
        public V setValue(V value) {
            return put(getOriginalKey(), value);
        }

        /**
         * {@inheritDoc}
         * <p>
         * For String keys, equality is based on the original String value rather than
         * the case-insensitive representation. This ensures that entries with the same
         * case-insensitive key but different original strings are considered distinct.
         *
         * @param o object to be compared for equality with this map entry
         * @return true if the specified object is equal to this map entry
         * @see Map.Entry#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Entry)) return false;
            Entry<?, ?> e = (Entry<?, ?>) o;
            return Objects.equals(getOriginalKey(), e.getKey()) &&
                    Objects.equals(getValue(), e.getValue());
        }

        /**
         * {@inheritDoc}
         * <p>
         * For String keys, the hash code is computed using the original String value
         * rather than the case-insensitive representation.
         *
         * @return the hash code value for this map entry
         * @see Map.Entry#hashCode()
         */
        @Override
        public int hashCode() {
            return Objects.hashCode(getOriginalKey()) ^ Objects.hashCode(getValue());
        }

        /**
         * {@inheritDoc}
         * <p>
         * Returns a string representation of this map entry. The string representation
         * consists of this entry's key followed by the equals character ("=") followed
         * by this entry's value. For String keys, the original string value is used.
         *
         * @return a string representation of this map entry
         */
        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    /**
     * Wrapper class for String keys to enforce case-insensitive comparison.
     * Note: Do not use this class directly, as it will eventually be made private.
     */
    public static final class CaseInsensitiveString implements Comparable<Object> {
        private final String original;
        private final int hash;

        /**
         * Constructs a CaseInsensitiveString from the given String.
         *
         * @param string the original String
         */
        public CaseInsensitiveString(String string) {
            original = string;
            hash = StringUtilities.hashCodeIgnoreCase(string);
        }

        /**
         * Returns the original String.
         *
         * @return the original String
         */
        @Override
        public String toString() {
            return original;
        }

        /**
         * Returns the hash code for this object, computed in a case-insensitive manner.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            return hash;
        }

        /**
         * Compares this object to another for equality in a case-insensitive manner.
         *
         * @param other the object to compare to
         * @return true if they are equal ignoring case, false otherwise
         */
        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (other instanceof CaseInsensitiveString) {
                CaseInsensitiveString cis = (CaseInsensitiveString) other;
                return hash == cis.hash && original.equalsIgnoreCase(cis.original);
            }
            if (other instanceof String) {
                return original.equalsIgnoreCase((String) other);
            }
            return false;
        }

        /**
         * Compares this CaseInsensitiveString to another object. If the object is a String or CaseInsensitiveString,
         * comparison is case-insensitive. Otherwise, Strings are considered "less" than non-Strings.
         *
         * @param o the object to compare to
         * @return a negative integer, zero, or a positive integer depending on ordering
         */
        @Override
        public int compareTo(Object o) {
            if (o instanceof CaseInsensitiveString) {
                CaseInsensitiveString other = (CaseInsensitiveString) o;
                return original.compareToIgnoreCase(other.original);
            }
            if (o instanceof String) {
                return original.compareToIgnoreCase((String) o);
            }
            // Strings are considered less than non-Strings
            return -1;
        }
    }

    /**
     * Normalizes the key for insertion or lookup in the underlying map.
     * If the key is a String, it is converted to a CaseInsensitiveString.
     * Otherwise, it is returned as is.
     *
     * @param key the key to normalize
     * @return the normalized key
     */
    @SuppressWarnings("unchecked")
    private K normalizeKey(K key) {
        if (key instanceof String) {
            return (K) newCIString((String) key);
        }
        return key;
    }

    /**
     * Wraps a Function to maintain the map's case-insensitive transparency. When the wrapped
     * Function is called, if the key is internally stored as a CaseInsensitiveString, this wrapper
     * ensures the original String value is passed to the function instead of the wrapper object.
     * Non-String keys are passed through unchanged.
     *
     * <p>This wrapper ensures users' Function implementations receive the same key type they originally
     * put into the map, maintaining the map's encapsulation of its case-insensitive implementation.</p>
     *
     * @param func the original function to be wrapped
     * @param <R>  the type of result returned by the Function
     * @return a wrapped Function that provides the original key value to the wrapped function
     */
    private <R> Function<? super K, ? extends R> wrapFunctionForKey(Function<? super K, ? extends R> func) {
        return k -> {
            // If key is a CaseInsensitiveString, extract its original String.
            // Otherwise, use the key as is.
            K originalKey = (k instanceof CaseInsensitiveString)
                    ? (K) ((CaseInsensitiveString) k).original
                    : k;
            return func.apply(originalKey);
        };
    }
    
    /**
     * Wraps a BiFunction to maintain the map's case-insensitive transparency. When the wrapped
     * BiFunction is called, if the key is internally stored as a CaseInsensitiveString, this wrapper
     * ensures the original String value is passed to the function instead of the wrapper object.
     * Non-String keys are passed through unchanged.
     *
     * <p>This wrapper ensures users' BiFunction implementations receive the same key type they originally
     * put into the map, maintaining the map's encapsulation of its case-insensitive implementation.</p>
     *
     * @param func the original bi-function to be wrapped
     * @param <R>  the type of result returned by the BiFunction
     * @return a wrapped BiFunction that provides the original key value to the wrapped function
     */
    private <R> BiFunction<? super K, ? super V, ? extends R> wrapBiFunctionForKey(BiFunction<? super K, ? super V, ? extends R> func) {
        return (k, v) -> {
            // If key is a CaseInsensitiveString, extract its original String.
            // Otherwise, use the key as is.
            K originalKey = (k instanceof CaseInsensitiveString)
                    ? (K) ((CaseInsensitiveString) k).original
                    : k;
            return func.apply(originalKey, v);
        };
    }

    /**
     * {@inheritDoc}
     * <p>
     * For String keys, the mapping is performed in a case-insensitive manner. If the mapping
     * function receives a String key, it will be passed the original String rather than the
     * internal case-insensitive representation.
     *
     * @see Map#computeIfAbsent(Object, Function)
     */
    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        K actualKey = normalizeKey(key);
        // mappingFunction gets wrapped so it sees the original String if k is a CaseInsensitiveString
        return map.computeIfAbsent(actualKey, wrapFunctionForKey(mappingFunction));
    }

    /**
     * {@inheritDoc}
     * <p>
     * For String keys, the mapping is performed in a case-insensitive manner. If the remapping
     * function receives a String key, it will be passed the original String rather than the
     * internal case-insensitive representation.
     *
     * @see Map#computeIfPresent(Object, BiFunction)
     */
    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        // Normalize input key to ensure case-insensitive lookup for Strings
        K actualKey = normalizeKey(key);
        // remappingFunction gets wrapped so it sees the original String if k is a CaseInsensitiveString
        return map.computeIfPresent(actualKey, wrapBiFunctionForKey(remappingFunction));
    }

    /**
     * {@inheritDoc}
     * <p>
     * For String keys, the computation is performed in a case-insensitive manner. If the remapping
     * function receives a String key, it will be passed the original String rather than the
     * internal case-insensitive representation.
     *
     * @see Map#compute(Object, BiFunction)
     */
    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        K actualKey = normalizeKey(key);
        // Wrapped so that the BiFunction receives original String key if applicable
        return map.compute(actualKey, wrapBiFunctionForKey(remappingFunction));
    }

    /**
     * {@inheritDoc}
     * <p>
     * For String keys, the merge is performed in a case-insensitive manner. The remapping
     * function operates only on values and is not affected by case sensitivity.
     *
     * @see Map#merge(Object, Object, BiFunction)
     */
    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        K actualKey = normalizeKey(key);
        // merge doesn't provide the key to the BiFunction, only values. No wrapping of keys needed.
        // The remapping function only deals with values, so we do not need wrapBiFunctionForKey here.
        return map.merge(actualKey, value, remappingFunction);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For String keys, the operation is performed in a case-insensitive manner.
     *
     * @see Map#putIfAbsent(Object, Object)
     */
    @Override
    public V putIfAbsent(K key, V value) {
        K actualKey = normalizeKey(key);
        return map.putIfAbsent(actualKey, value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For String keys, the removal is performed in a case-insensitive manner.
     *
     * @see Map#remove(Object, Object)
     */
    @Override
    public boolean remove(Object key, Object value) {
        if (key instanceof String) {
            return map.remove(newCIString((String) key), value);
        }
        return map.remove(key, value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For String keys, the replacement is performed in a case-insensitive manner.
     *
     * @see Map#replace(Object, Object, Object)
     */
    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        K actualKey = normalizeKey(key);
        return map.replace(actualKey, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For String keys, the replacement is performed in a case-insensitive manner.
     *
     * @see Map#replace(Object, Object)
     */
    @Override
    public V replace(K key, V value) {
        K actualKey = normalizeKey(key);
        return map.replace(actualKey, value);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For String keys, the action receives the original String key rather than the
     * internal case-insensitive representation.
     *
     * @see Map#forEach(BiConsumer)
     */
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        // Unwrap keys before calling action
        map.forEach((k, v) -> {
            K originalKey = (k instanceof CaseInsensitiveString) ? (K) ((CaseInsensitiveString) k).original : k;
            action.accept(originalKey, v);
        });
    }

    /**
     * {@inheritDoc}
     * <p>
     * For String keys, the function receives the original String key rather than the
     * internal case-insensitive representation. The replacement is performed in a
     * case-insensitive manner.
     *
     * @see Map#replaceAll(BiFunction)
     */
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        // Unwrap keys before applying the function to values
        map.replaceAll((k, v) -> {
            K originalKey = (k instanceof CaseInsensitiveString) ? (K) ((CaseInsensitiveString) k).original : k;
            return function.apply(originalKey, v);
        });
    }

    /**
     * Creates a new CaseInsensitiveString instance. If the input string's length is greater than 100,
     * a new instance is always created. Otherwise, the method checks the cache:
     * - If a cached instance exists, it returns the cached instance.
     * - If not, it creates a new instance, caches it, and then returns it.
     *
     * @param string the original string to wrap
     * @return a CaseInsensitiveString instance corresponding to the input string
     * @throws NullPointerException if the input string is null
     */
    private CaseInsensitiveString newCIString(String string) {
        Objects.requireNonNull(string, "Input string cannot be null");

        if (string.length() > 100) {
            // For long strings, always create a new instance to save cache space
            return new CaseInsensitiveString(string);
        } else {
            // Attempt to retrieve from cache
            CaseInsensitiveString cachedCIString = ciStringCache.get(string);
            if (cachedCIString != null) {
                return cachedCIString;
            } else {
                // Create a new instance, cache it, and return
                CaseInsensitiveString newCIString = new CaseInsensitiveString(string);
                ciStringCache.put(string, newCIString);
                return newCIString;
            }
        }
    }
}
