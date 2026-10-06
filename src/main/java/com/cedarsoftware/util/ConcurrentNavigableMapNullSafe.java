package com.cedarsoftware.util;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ConcurrentNavigableMapNullSafe is a thread-safe implementation of {@link ConcurrentNavigableMap}
 * that allows {@code null} keys and values. A dedicated sentinel object is used internally to
 * represent {@code null} keys, ensuring no accidental key collisions.
 * From an ordering perspective, null keys are considered last.  This is honored with the
 * ascending and descending views, where ascending view places them last, and descending view
 * place a null key first.
 *
 * @param <K> The type of keys maintained by this map
 * @param <V> The type of mapped values
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
public class ConcurrentNavigableMapNullSafe<K, V> extends AbstractConcurrentNullSafeMap<K, V>
        implements ConcurrentNavigableMap<K, V>, Serializable {

    private static final long serialVersionUID = 1L;
    private final Comparator<? super K> originalComparator;
    private final ConcurrentNavigableMap<Object, Object> navigableMap;
    private final boolean descendingView;
    private transient NavigableSet<K> cachedKeySet;
    /**
     * Sentinel object used to represent {@code null} keys internally. Using a
     * dedicated object avoids any chance of key collision and eliminates the
     * overhead of generating a random value.
     */
    private static final Object NULL_KEY_SENTINEL = new Object();

    /**
     * Tie-breakers for distinct same-class non-Comparable keys whose hash codes
     * collide. Without this, colliding distinct keys compare equal and silently
     * overwrite each other. Keys are held weakly so map keys stay GC-able, and the
     * registry is only populated when a collision actually occurs (vanishingly rare),
     * so the common comparison paths never touch it.
     */
    private static final Map<Object, Long> TIE_BREAKERS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final AtomicLong TIE_BREAKER_SEQ = new AtomicLong();

    private static long tieBreakerFor(Object o) {
        return TIE_BREAKERS.computeIfAbsent(o, k -> TIE_BREAKER_SEQ.getAndIncrement());
    }

    /**
     * Constructs a new, empty ConcurrentNavigableMapNullSafe with natural ordering of its keys.
     * All keys inserted must implement the Comparable interface.
     */
    public ConcurrentNavigableMapNullSafe() {
        this(null);
    }

    /**
     * Constructs a new, empty ConcurrentNavigableMapNullSafe with the specified comparator.
     *
     * @param comparator the comparator that will be used to order this map. If null, the natural
     *                   ordering of the keys will be used.
     */
    public ConcurrentNavigableMapNullSafe(Comparator<? super K> comparator) {
        this(new ConcurrentSkipListMap<>(wrapComparator(comparator)), comparator, false);
    }

    /**
     * Private constructor that accepts an internal map and the original comparator.
     *
     * @param internalMap       the internal map to wrap
     * @param originalComparator the original comparator provided by the user
     * @param descendingView true when this map is a descending-order view
     */
    private ConcurrentNavigableMapNullSafe(ConcurrentNavigableMap<Object, Object> internalMap,
                                           Comparator<? super K> originalComparator,
                                           boolean descendingView) {
        super(internalMap);
        this.navigableMap = internalMap;
        this.originalComparator = originalComparator;
        this.descendingView = descendingView;
    }

    /**
     * Static method to wrap the user-provided comparator to handle sentinel keys and mixed key types.
     * The common comparison (two non-null keys: the user comparator, or two of one Comparable class) stays small
     * enough for the JIT to inline into the skip list; nulls and mixed classes are handled out of line.
     *
     * @param comparator the user-provided comparator
     * @return a comparator that handles sentinel keys and mixed key types
     */
    @SuppressWarnings("unchecked")
    private static <K> Comparator<Object> wrapComparator(Comparator<? super K> comparator) {
        // Set once the user comparator throws NullPointerException for a null: nulls then go last.
        final boolean[] comparatorRejectsNulls = new boolean[1];
        return (o1, o2) -> {
            if (o1 != NULL_KEY_SENTINEL && o2 != NULL_KEY_SENTINEL && o1 != null && o2 != null) {
                if (comparator != null) {
                    return comparator.compare((K) o1, (K) o2);
                }
                if (o1.getClass() == o2.getClass() && o1 instanceof Comparable) {
                    return ((Comparable<Object>) o1).compareTo(o2);
                }
                return compareMixed(o1, o2);
            }
            return compareWithNull(o1, o2, comparator, comparatorRejectsNulls);
        };
    }

    /**
     * Ordering when either key is the null key. A user comparator that places null (Comparator.nullsFirst, say)
     * decides where it goes, as comparator() reports it (as ConcurrentNavigableSetNullSafe does); otherwise, or
     * once that comparator has thrown NullPointerException for a null, null keys are greater than any other.
     */
    @SuppressWarnings("unchecked")
    private static <K> int compareWithNull(Object o1, Object o2, Comparator<? super K> comparator,
                                           boolean[] comparatorRejectsNulls) {
        boolean o1IsNull = o1 == NULL_KEY_SENTINEL || o1 == null;
        boolean o2IsNull = o2 == NULL_KEY_SENTINEL || o2 == null;
        if (comparator != null && !comparatorRejectsNulls[0]) {
            try {
                return comparator.compare(o1IsNull ? null : (K) o1, o2IsNull ? null : (K) o2);
            } catch (NullPointerException npe) {
                comparatorRejectsNulls[0] = true;
            }
        }
        if (o1IsNull && o2IsNull) {
            return 0;
        }
        return o1IsNull ? 1 : -1;
    }

    /**
     * Natural ordering of two non-null keys that are not two of one Comparable class: related classes by
     * compareTo, keys of one class that is not Comparable (held as HashedKey) by hash code then equals, unrelated
     * classes by class name.
     */
    @SuppressWarnings("unchecked")
    private static int compareMixed(Object o1, Object o2) {
        Object k1 = o1 instanceof HashedKey ? ((HashedKey) o1).key : o1;
        Object k2 = o2 instanceof HashedKey ? ((HashedKey) o2).key : o2;
        Class<?> class1 = k1.getClass();
        Class<?> class2 = k2.getClass();

        // Related classes are one natural order too: an enum's constants (one with a body has its own class)
        // and a Comparable class with its subclasses. Ordering those by class name split them apart.
        if (class1 != class2 && k1 instanceof Comparable && related(k1, class1, k2, class2)) {
            return ((Comparable<Object>) k1).compareTo(k2);
        }

        // Same-class keys that are not Comparable: equal keys (by equals, as a Map's keys are) are one key, and
        // distinct keys need a strict ordering so they never overwrite each other. Hash codes first (cached in
        // the HashedKey, so a comparison is an int compare), equals only when they tie. Identity hashes, as
        // before, made two equal keys two keys.
        if (class1 == class2) {
            if (k1 == k2) {
                return 0;
            }
            int hashCmp = Integer.compare(hashOf(o1, k1), hashOf(o2, k2));
            if (hashCmp != 0) {
                return hashCmp;
            }
            if (k1.equals(k2)) {
                return 0;
            }
            // Distinct keys with colliding hash codes: assign stable sequence numbers so they remain distinct
            // keys instead of one silently overwriting the other. (The registry is keyed by equals, so an equal
            // key shares the entry; equal keys never get here.)
            return Long.compare(tieBreakerFor(k1), tieBreakerFor(k2));
        }

        // Compare class names to provide ordering between different types
        String className1 = class1.getName();
        String className2 = class2.getName();
        int classComparison = className1.compareTo(className2);

        if (classComparison != 0) {
            return classComparison;
        }

        // If class names are the same but classes are different (rare), compare class loader information
        ClassLoader cl1 = class1.getClassLoader();
        ClassLoader cl2 = class2.getClassLoader();
        String loader1 = cl1 == null ? "" : cl1.getClass().getName();
        String loader2 = cl2 == null ? "" : cl2.getClass().getName();
        int loaderCompare = loader1.compareTo(loader2);
        if (loaderCompare != 0) {
            return loaderCompare;
        }

        // Final tie-breaker using identity hash of the class loaders
        return Integer.compare(System.identityHashCode(cl1), System.identityHashCode(cl2));
    }

    private static int hashOf(Object masked, Object key) {
        return masked instanceof HashedKey ? ((HashedKey) masked).hash : key.hashCode();
    }

    /**
     * A key that is not Comparable, as a natural-order map holds it: with its hash code computed once, when the key
     * goes in or is looked up, since such keys are ordered by hash code and every comparison would otherwise
     * compute both. Only maskNullKey makes one and unmaskNullKey takes it apart.
     */
    private static final class HashedKey {
        final Object key;
        final int hash;

        HashedKey(Object key) {
            this.key = key;
            this.hash = key.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof HashedKey && key.equals(((HashedKey) o).key);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return String.valueOf(key);
        }
    }

    /**
     * Whether two keys of different classes share a natural order: constants of one enum, or instances of a
     * Comparable class and a subclass of it (the common class itself Comparable, so compareTo accepts both).
     */
    private static boolean related(Object o1, Class<?> class1, Object o2, Class<?> class2) {
        if (o1 instanceof Enum && o2 instanceof Enum) {
            return ((Enum<?>) o1).getDeclaringClass() == ((Enum<?>) o2).getDeclaringClass();
        }
        Class<?> common = class1.isAssignableFrom(class2) ? class1 : class2.isAssignableFrom(class1) ? class2 : null;
        return common != null && Comparable.class.isAssignableFrom(common);
    }

    @Override
    protected Object maskNullKey(Object key) {
        if (key == null) {
            return NULL_KEY_SENTINEL;
        }
        if (originalComparator == null && !(key instanceof Comparable)) {
            return new HashedKey(key);              // natural order sorts such keys by hash code: compute it once
        }
        return key;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected K unmaskNullKey(Object maskedKey) {
        if (maskedKey == NULL_KEY_SENTINEL) {
            return null;
        }
        if (maskedKey instanceof HashedKey) {
            return (K) ((HashedKey) maskedKey).key;
        }
        return (K) maskedKey;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Comparator<? super K> comparator() {
        if (!descendingView) {
            return originalComparator;
        }
        if (originalComparator == null) {
            return (Comparator) Collections.reverseOrder();
        }
        return Collections.reverseOrder(originalComparator);
    }

    // Implement navigational methods

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        ConcurrentNavigableMap<Object, Object> subInternal = navigableMap.subMap(
                maskNullKey(fromKey), fromInclusive,
                maskNullKey(toKey), toInclusive
        );
        return new ConcurrentNavigableMapNullSafe<>(subInternal, this.originalComparator, this.descendingView);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        ConcurrentNavigableMap<Object, Object> headInternal = navigableMap.headMap(
                maskNullKey(toKey), inclusive
        );
        return new ConcurrentNavigableMapNullSafe<>(headInternal, this.originalComparator, this.descendingView);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        ConcurrentNavigableMap<Object, Object> tailInternal = navigableMap.tailMap(
                maskNullKey(fromKey), inclusive
        );
        return new ConcurrentNavigableMapNullSafe<>(tailInternal, this.originalComparator, this.descendingView);
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        Entry<Object, Object> entry = navigableMap.lowerEntry(maskNullKey(key));
        return wrapEntry(entry);
    }

    @Override
    public K lowerKey(K key) {
        return unmaskNullKey(navigableMap.lowerKey(maskNullKey(key)));
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        Entry<Object, Object> entry = navigableMap.floorEntry(maskNullKey(key));
        return wrapEntry(entry);
    }

    @Override
    public K floorKey(K key) {
        return unmaskNullKey(navigableMap.floorKey(maskNullKey(key)));
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        Entry<Object, Object> entry = navigableMap.ceilingEntry(maskNullKey(key));
        return wrapEntry(entry);
    }

    @Override
    public K ceilingKey(K key) {
        return unmaskNullKey(navigableMap.ceilingKey(maskNullKey(key)));
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        Entry<Object, Object> entry = navigableMap.higherEntry(maskNullKey(key));
        return wrapEntry(entry);
    }

    @Override
    public K higherKey(K key) {
        return unmaskNullKey(navigableMap.higherKey(maskNullKey(key)));
    }

    @Override
    public Entry<K, V> firstEntry() {
        Entry<Object, Object> entry = navigableMap.firstEntry();
        return wrapEntry(entry);
    }

    @Override
    public Entry<K, V> lastEntry() {
        Entry<Object, Object> entry = navigableMap.lastEntry();
        return wrapEntry(entry);
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        Entry<Object, Object> entry = navigableMap.pollFirstEntry();
        if (entry == null) {
            return null;
        }
        K key = unmaskNullKey(entry.getKey());
        V value = unmaskNullValue(entry.getValue());
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        Entry<Object, Object> entry = navigableMap.pollLastEntry();
        if (entry == null) {
            return null;
        }
        K key = unmaskNullKey(entry.getKey());
        V value = unmaskNullValue(entry.getValue());
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    @Override
    public K firstKey() {
        return unmaskNullKey(navigableMap.firstKey());
    }

    @Override
    public K lastKey() {
        return unmaskNullKey(navigableMap.lastKey());
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return keySet();
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    public ConcurrentNavigableMap<K, V> descendingMap() {
        ConcurrentNavigableMap<Object, Object> descInternal = navigableMap.descendingMap();
        return new ConcurrentNavigableMapNullSafe<>(descInternal, this.originalComparator, !this.descendingView);
    }

    @Override
    public NavigableSet<K> keySet() {
        if (cachedKeySet == null) {
            cachedKeySet = new KeyNavigableSet<>(this, navigableMap.keySet());
        }
        return cachedKeySet;
    }

    /**
     * Inner class implementing NavigableSet<K> for the keySet().
     */
    private static class KeyNavigableSet<K, V> extends AbstractSet<K> implements NavigableSet<K> {
        private final ConcurrentNavigableMapNullSafe<K, V> owner;
        private final Set<Object> internalKeys;

        KeyNavigableSet(ConcurrentNavigableMapNullSafe<K, V> owner, Set<Object> internalKeys) {
            this.owner = owner;
            this.internalKeys = internalKeys;
        }

        @Override
        public Iterator<K> iterator() {
            Iterator<Object> it = internalKeys.iterator();
            return new Iterator<K>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public K next() {
                    return owner.unmaskNullKey(it.next());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public int size() {
            return internalKeys.size();
        }

        @Override
        public Spliterator<K> spliterator() {
            return new UnmaskingSpliterator<>(internalKeys.spliterator(), owner::unmaskNullKey, comparator());
        }

        @Override
        public boolean contains(Object o) {
            return owner.internalMap.containsKey(owner.maskNullKey(o));
        }

        @Override
        public boolean remove(Object o) {
            return owner.internalMap.remove(owner.maskNullKey(o)) != null;
        }

        @Override
        public void clear() {
            owner.internalMap.clear();
        }

        @Override
        public K lower(K k) {
            return owner.unmaskNullKey(owner.navigableMap.lowerKey(owner.maskNullKey(k)));
        }

        @Override
        public K floor(K k) {
            return owner.unmaskNullKey(owner.navigableMap.floorKey(owner.maskNullKey(k)));
        }

        @Override
        public K ceiling(K k) {
            return owner.unmaskNullKey(owner.navigableMap.ceilingKey(owner.maskNullKey(k)));
        }

        @Override
        public K higher(K k) {
            return owner.unmaskNullKey(owner.navigableMap.higherKey(owner.maskNullKey(k)));
        }

        @Override
        public K pollFirst() {
            Entry<Object, Object> entry = owner.navigableMap.pollFirstEntry();
            return (entry == null) ? null : owner.unmaskNullKey(entry.getKey());
        }

        @Override
        public K pollLast() {
            Entry<Object, Object> entry = owner.navigableMap.pollLastEntry();
            return (entry == null) ? null : owner.unmaskNullKey(entry.getKey());
        }

        @Override
        public Comparator<? super K> comparator() {
            return owner.comparator();
        }

        @Override
        public K first() {
            return owner.unmaskNullKey(owner.navigableMap.firstKey());
        }

        @Override
        public K last() {
            return owner.unmaskNullKey(owner.navigableMap.lastKey());
        }

        @Override
        public NavigableSet<K> descendingSet() {
            return owner.descendingKeySet();
        }

        @Override
        public Iterator<K> descendingIterator() {
            Iterator<Object> it = owner.navigableMap.descendingKeySet().iterator();
            return new Iterator<K>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public K next() {
                    return owner.unmaskNullKey(it.next());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive) {
            ConcurrentNavigableMap<K, V> subMap = owner.subMap(fromElement, fromInclusive, toElement, toInclusive);
            return subMap.navigableKeySet();
        }

        @Override
        public NavigableSet<K> headSet(K toElement, boolean inclusive) {
            ConcurrentNavigableMap<K, V> headMap = owner.headMap(toElement, inclusive);
            return headMap.navigableKeySet();
        }

        @Override
        public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
            ConcurrentNavigableMap<K, V> tailMap = owner.tailMap(fromElement, inclusive);
            return tailMap.navigableKeySet();
        }

        @Override
        public SortedSet<K> subSet(K fromElement, K toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public SortedSet<K> headSet(K toElement) {
            return headSet(toElement, false);
        }

        @Override
        public SortedSet<K> tailSet(K fromElement) {
            return tailSet(fromElement, true);
        }
    }

    /**
     * Wraps an internal entry to expose it as an Entry<K, V> with unmasked keys and values.
     *
     * @param internalEntry the internal map entry
     * @return the wrapped entry, or null if the internal entry is null
     */
    private Entry<K, V> wrapEntry(Entry<Object, Object> internalEntry) {
        if (internalEntry == null) {
            return null;
        }
        K key = unmaskNullKey(internalEntry.getKey());
        V value = unmaskNullValue(internalEntry.getValue());
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    /**
     * Serializable as ConcurrentSkipListMap is: written as its entries, in order, with real nulls, and its comparator
     * (which must be Serializable, as for TreeMap); read back through the public constructor. A descending view comes
     * back as a descending view of a copy, a range view as a copy of the entries it holds (without its bounds).
     */
    private Object writeReplace() {
        return new SerializedForm(originalComparator, descendingView, this);
    }

    // Never read directly: the stream holds a SerializedForm (a crafted stream naming this class is refused).
    private void readObject(java.io.ObjectInputStream in) throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Written as its SerializedForm");
    }

    private static final class SerializedForm implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Comparator<?> comparator;
        private final boolean descending;
        private final Object[] keys;
        private final Object[] values;

        SerializedForm(Comparator<?> comparator, boolean descending, Map<?, ?> map) {
            this.comparator = comparator;
            this.descending = descending;
            List<Object> k = new ArrayList<>();
            List<Object> v = new ArrayList<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                k.add(entry.getKey());
                v.add(entry.getValue());
            }
            keys = k.toArray();
            values = v.toArray();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Object readResolve() {
            ConcurrentNavigableMapNullSafe<Object, Object> map =
                    new ConcurrentNavigableMapNullSafe<>((Comparator) comparator);
            for (int i = 0; i < keys.length; i++) {
                map.put(keys[i], values[i]);
            }
            return descending ? map.descendingMap() : map;
        }
    }
}
