package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;

/**
 * SealableNavigableMap provides a NavigableMap or NavigableMap wrapper that can be 'sealed' and 'unsealed.'
 * When sealed, the Map is mutable, when unsealed it is immutable (read-only). The view methods keySet(), entrySet(),
 * values(), navigableKeySet(), descendingMap(), descendingKeySet(), subMap(), headMap(), and tailMap() return a view
 * that honors the Supplier's sealed state.  The sealed state can be changed as often as needed.
 * <br><br>
 * NOTE: Please do not reformat this code as the current format makes it easy to see the overall structure.
 * <br><br>
 * @author John DeRegnaucourt
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
public class SealableNavigableMap<K, V> implements NavigableMap<K, V> {
    private final NavigableMap<K, V> map;
    private final Supplier<Boolean> sealedSupplier;

    /**
     * Create a SealableNavigableMap. Since a Map is not supplied, this will use a ConcurrentSkipListMap internally.
     * If you want a TreeMap to be used internally, use the SealableMap constructor that takes a NavigableMap and pass
     * it the instance you want it to wrap.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableMap(Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        this.map = new ConcurrentSkipListMap<>();
    }

    /**
     * Create a NavigableSealableMap. Since NavigableMap is not supplied, the elements from the passed in SortedMap
     * will be copied to an internal ConcurrentSkipListMap. If you want to use a TreeMap for example, use the
     * SealableNavigableMap constructor that takes a NavigableMap and pass it the instance you want it to wrap.
     * @param map SortedMap to supply initial elements.  These are copied to an internal ConcurrentSkipListMap.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableMap(SortedMap<K, V> map, Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        this.map = new ConcurrentSkipListMap<>(map);
    }

    /**
     * Use this constructor to wrap a NavigableMap (any kind of NavigableMap) and make it a SealableNavigableMap.
     * No duplicate of the Map is created and the original map is operated on directly if unsealed, or protected
     * from changes if sealed.
     * @param map NavigableMap instance to protect.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableMap(NavigableMap<K, V> map, Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        this.map = map;
    }

    private void throwIfSealed() {
        if (sealedSupplier.get()) {
            throw new UnsupportedOperationException("This map has been sealed and is now immutable");
        }
    }

    // Immutable APIs
    public boolean equals(Object o) { return map.equals(o); }
    public int hashCode() { return map.hashCode(); }
    public boolean isEmpty() { return map.isEmpty(); }
    public boolean containsKey(Object key) { return map.containsKey(key); }
    public boolean containsValue(Object value) { return map.containsValue(value); }
    public int size() { return map.size(); }
    public V get(Object key) { return map.get(key); }
    public Comparator<? super K> comparator() { return map.comparator(); }
    public K firstKey() { return map.firstKey(); }
    public K lastKey() { return map.lastKey(); }
    public Set<K> keySet() { return new SealableSet<>(map.keySet(), sealedSupplier); }
    public Collection<V> values() { return new SealableList<>(new ArrayList<>(map.values()), sealedSupplier); }
    public Set<Entry<K, V>> entrySet() { return new SealableSet<>(map.entrySet(), sealedSupplier); }
    public Map.Entry<K, V> lowerEntry(K key) { return map.lowerEntry(key); }
    public K lowerKey(K key) { return map.lowerKey(key); }
    public Map.Entry<K, V> floorEntry(K key) { return map.floorEntry(key); }
    public K floorKey(K key) { return map.floorKey(key); }
    public Map.Entry<K, V> ceilingEntry(K key) { return map.ceilingEntry(key); }
    public K ceilingKey(K key) { return map.ceilingKey(key); }
    public Map.Entry<K, V> higherEntry(K key) { return map.higherEntry(key); }
    public K higherKey(K key) { return map.higherKey(key); }
    public Map.Entry<K, V> firstEntry() { return map.firstEntry(); }
    public Map.Entry<K, V> lastEntry() { return map.lastEntry(); }
    public NavigableMap<K, V> descendingMap() { return new SealableNavigableMap<>(map.descendingMap(), sealedSupplier); }
    public NavigableSet<K> navigableKeySet() { return new SealableNavigableSet<>(map.navigableKeySet(), sealedSupplier); }
    public NavigableSet<K> descendingKeySet() { return new SealableNavigableSet<>(map.descendingKeySet(), sealedSupplier); }
    public SortedMap<K, V> subMap(K fromKey, K toKey) { return subMap(fromKey, true, toKey, false); }
    public NavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        return new SealableNavigableMap<>(map.subMap(fromKey, fromInclusive, toKey, toInclusive), sealedSupplier);
    }
    public SortedMap<K, V> headMap(K toKey) { return headMap(toKey, false); }
    public NavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        return new SealableNavigableMap<>(map.headMap(toKey, inclusive), sealedSupplier);
    }
    public SortedMap<K, V> tailMap(K fromKey) { return tailMap(fromKey, true); }
    public NavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        return new SealableNavigableMap<>(map.tailMap(fromKey, inclusive), sealedSupplier);
    }

    // Mutable APIs
    public Map.Entry<K, V> pollFirstEntry() { throwIfSealed(); return map.pollFirstEntry(); }
    public Map.Entry<K, V> pollLastEntry() { throwIfSealed(); return map.pollLastEntry(); }
    public V put(K key, V value) { throwIfSealed(); return map.put(key, value); }
    public V remove(Object key) { throwIfSealed(); return map.remove(key); }
    public void putAll(Map<? extends K, ? extends V> m) { throwIfSealed(); map.putAll(m); }
    public void clear() { throwIfSealed(); map.clear(); }
}