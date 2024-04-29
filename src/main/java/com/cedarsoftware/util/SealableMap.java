package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * SealableMap provides a Map or Map wrapper that can be 'sealed' and 'unsealed.'  When sealed, the
 * Map is mutable, when unsealed it is immutable (read-only). The view methods iterator(), keySet(),
 * values(), and entrySet() return a view that honors the Supplier's sealed state.  The sealed state
 * can be changed as often as needed.
 * <br><br>
 * NOTE: Please do not reformat this code as the current format makes it easy to see the overall structure.
 * <br><br>
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
public class SealableMap<K, V> implements Map<K, V> {
    private final Map<K, V> map;
    private final Supplier<Boolean> sealedSupplier;

    /**
     * Create a SealableMap. Since a Map is not supplied, this will use a ConcurrentHashMap internally. If you
     * want a HashMap to be used internally, use the SealableMap constructor that takes a Map and pass it the
     * instance you want it to wrap.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableMap(Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        this.map = new ConcurrentHashMap<>();
    }

    /**
     * Use this constructor to wrap a Map (any kind of Map) and make it a SealableMap. No duplicate of the Map is
     * created and the original map is operated on directly if unsealed, or protected from changes if sealed.
     * @param map Map instance to protect.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableMap(Map<K, V> map, Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        this.map = map;
    }
    
    private void throwIfSealed() {
        if (sealedSupplier.get()) {
            throw new UnsupportedOperationException("This map has been sealed and is now immutable");
        }
    }

    // Immutable
    public boolean equals(Object obj) { return map.equals(obj); }
    public int hashCode() { return map.hashCode(); }
    public String toString() { return map.toString(); }
    public int size() { return map.size(); }
    public boolean isEmpty() { return map.isEmpty(); }
    public boolean containsKey(Object key) { return map.containsKey(key); }
    public boolean containsValue(Object value) { return map.containsValue(value); }
    public V get(Object key) { return map.get(key); }
    public Set<K> keySet() { return new SealableSet<>(map.keySet(), sealedSupplier); }
    public Collection<V> values() { return new SealableList<>(new ArrayList<>(map.values()), sealedSupplier); }
    public Set<Map.Entry<K, V>> entrySet() { return new SealableSet<>(map.entrySet(), sealedSupplier); }

    // Mutable
    public V put(K key, V value) { throwIfSealed(); return map.put(key, value); }
    public V remove(Object key) { throwIfSealed(); return map.remove(key); }
    public void putAll(Map<? extends K, ? extends V> m) { throwIfSealed(); map.putAll(m); }
    public void clear() { throwIfSealed(); map.clear(); }
}