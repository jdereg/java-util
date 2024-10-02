package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * SealableSet provides a Set or Set wrapper that can be 'sealed' and 'unsealed.'  When sealed, the
 * Set is mutable, when unsealed it is immutable (read-only). The iterator() returns a view that
 * honors the Supplier's sealed state.  The sealed state can be changed as often as needed.
 * <br><br>
 * NOTE: Please do not reformat this code as the current format makes it easy to see the overall structure.
 * <br><br>
 * @author John DeRegnaucourt
 *         <br>
 *         Copyright Cedar Software LLC
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
public class SealableSet<T> implements Set<T> {
    private final Set<T> set;
    private final transient Supplier<Boolean> sealedSupplier;

    /**
     * Create a SealableSet. Since a Set is not supplied, this will use a ConcurrentSet internally.
     * If you want a HashSet to be used internally, use SealableSet constructor that takes a Set and pass it the
     * instance you want it to wrap.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableSet(Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        this.set = new ConcurrentSet<>();
    }
    
    /**
     * Create a SealableSet. Since a Set is not supplied, the elements from the passed in Collection will be
     * copied to an internal ConcurrentHashMap.newKeySet.  If you want to use a HashSet for example, use SealableSet
     * constructor that takes a Set and pass it the instance you want it to wrap.
     * @param col Collection to supply initial elements.  These are copied to an internal ConcurrentHashMap.newKeySet.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableSet(Collection<T> col, Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        this.set = new ConcurrentSet<>(col);
    }

    /**
     * Use this constructor to wrap a Set (any kind of Set) and make it a SealableSet. No duplicate of the Set is
     * created and the original set is operated on directly if unsealed, or protected from changes if sealed.
     * @param set Set instance to protect.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableSet(Set<T> set, Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        this.set = set;
    }

    private void throwIfSealed() {
        if (sealedSupplier.get()) {
            throw new UnsupportedOperationException("This set has been sealed and is now immutable");
        }
    }

    // Immutable APIs
    public boolean equals(Object o) { return set.equals(o); }
    public int hashCode() { return set.hashCode(); }
    public String toString() { return set.toString(); }
    public int size() { return set.size(); }
    public boolean isEmpty() { return set.isEmpty(); }
    public boolean contains(Object o) { return set.contains(o); }
    public Object[] toArray() { return set.toArray(); }
    public <T1> T1[] toArray(T1[] a) { return set.toArray(a); }
    public boolean containsAll(Collection<?> col) { return set.containsAll(col); }

    // Mutable APIs
    public boolean add(T t) { throwIfSealed(); return set.add(t); }
    public boolean remove(Object o) { throwIfSealed(); return set.remove(o); }
    public boolean addAll(Collection<? extends T> col) { throwIfSealed(); return set.addAll(col); }
    public boolean removeAll(Collection<?> col) { throwIfSealed(); return set.removeAll(col); }
    public boolean retainAll(Collection<?> col) { throwIfSealed(); return set.retainAll(col); }
    public void clear() { throwIfSealed(); set.clear(); }
    public Iterator<T> iterator() {
        Iterator<T> iterator = set.iterator();
        return new Iterator<T>() {
            public boolean hasNext() { return iterator.hasNext(); }
            public T next() {
                T item = iterator.next();
                if (item instanceof Map.Entry) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) item;
                    return (T) new SealAwareEntry<>(entry, sealedSupplier);
                }
                return item;
            }
            public void remove() { throwIfSealed(); iterator.remove(); }
        };
    }
    
    // Must enforce immutability after the Map.Entry was "handed out" because
    // it could have been handed out when the map was unsealed or sealed.
    static class SealAwareEntry<K, V> implements Map.Entry<K, V> {
        private final Map.Entry<K, V> entry;
        private final Supplier<Boolean> sealedSupplier;

        SealAwareEntry(Map.Entry<K, V> entry, Supplier<Boolean> sealedSupplier) {
            this.entry = entry;
            this.sealedSupplier = sealedSupplier;
        }

        public K getKey() { return entry.getKey(); }
        public V getValue() { return entry.getValue(); }
        public V setValue(V value) {
            if (sealedSupplier.get()) {
                throw new UnsupportedOperationException("Cannot modify, set is sealed");
            }
            return entry.setValue(value);
        }

        public boolean equals(Object o) { return entry.equals(o); }
        public int hashCode() { return entry.hashCode(); }
    }
}