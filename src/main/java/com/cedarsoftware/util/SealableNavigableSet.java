package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Supplier;

/**
 * SealableNavigableSet provides a NavigableSet or NavigableSet wrapper that can be 'sealed' and
 * 'unsealed.'  When sealed, the NavigableSet is mutable, when unsealed it is immutable (read-only).
 * The view methods iterator(), descendingIterator(), descendingSet(), subSet(), headSet(), and
 * tailSet(), return a view that honors the Supplier's sealed state.  The sealed state can be
 * changed as often as needed.
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
public class SealableNavigableSet<T> implements NavigableSet<T> {
    private final NavigableSet<T> navigableSet;
    private final Supplier<Boolean> sealedSupplier;

    /**
     * Create a NavigableSealableSet. Since a NavigableSet is not supplied, this will use a ConcurrentSkipListSet
     * internally.  If you want to use a TreeSet for example, use the SealableNavigableSet constructor that takes
     * a NavigableSet and pass it the instance you want it to wrap.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableSet(Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        navigableSet = new ConcurrentSkipListSet<>();
    }

    /**
     * Create a NavigableSealableSet. Since a NavigableSet is not supplied, this will use a ConcurrentSkipListSet
     * internally. If you want to use a TreeSet for example, use the SealableNavigableSet constructor that takes
     * a NavigableSet and pass it the instance you want it to wrap.
     * @param comparator {@code Comparator} A comparison function, which imposes a <i>total ordering</i> on some
     * collection of objects.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableSet(Comparator<? super T> comparator, Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        navigableSet = new ConcurrentSkipListSet<>(comparator);
    }

    /**
     * Create a NavigableSealableSet. Since NavigableSet is not supplied, the elements from the passed in Collection
     * will be copied to an internal ConcurrentSkipListSet. If you want to use a TreeSet for example, use the
     * SealableNavigableSet constructor that takes a NavigableSet and pass it the instance you want it to wrap.
     * @param col Collection to supply initial elements.  These are copied to an internal ConcurrentSkipListSet.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableSet(Collection<? extends T> col, Supplier<Boolean> sealedSupplier) {
        this(sealedSupplier);
        addAll(col);
    }

    /**
     * Create a NavigableSealableSet. Since NavigableSet is not supplied, the elements from the passed in SortedSet
     * will be copied to an internal ConcurrentSkipListSet. If you want to use a TreeSet for example, use the
     * SealableNavigableSet constructor that takes a NavigableSet and pass it the instance you want it to wrap.
     * @param set SortedSet to supply initial elements.  These are copied to an internal ConcurrentSkipListSet.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableSet(SortedSet<T> set, Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        navigableSet = new ConcurrentSkipListSet<>(set);
    }

    /**
     * Use this constructor to wrap a NavigableSet (any kind of NavigableSet) and make it a SealableNavigableSet.
     * No duplicate of the Set is created, the original set is operated on directly if unsealed, or protected
     * from changes if sealed.
     * @param set NavigableSet instance to protect.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableSet(NavigableSet<T> set, Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        navigableSet = set;
    }

    private void throwIfSealed() {
        if (sealedSupplier.get()) {
            throw new UnsupportedOperationException("This set has been sealed and is now immutable");
        }
    }

    // Immutable APIs
    public boolean equals(Object o) { return o == this || navigableSet.equals(o); }
    public int hashCode() { return navigableSet.hashCode(); }
    public int size() { return navigableSet.size(); }
    public boolean isEmpty() { return navigableSet.isEmpty(); }
    public boolean contains(Object o) { return navigableSet.contains(o); }
    public boolean containsAll(Collection<?> col) { return navigableSet.containsAll(col);}
    public Comparator<? super T> comparator() { return navigableSet.comparator(); }
    public T first() { return navigableSet.first(); }
    public T last() { return navigableSet.last(); }
    public Object[] toArray() { return navigableSet.toArray(); }
    public <T> T[] toArray(T[] a) { return navigableSet.toArray(a); }
    public T lower(T e) { return navigableSet.lower(e); }
    public T floor(T e) { return navigableSet.floor(e); }
    public T ceiling(T e) { return navigableSet.ceiling(e); }
    public T higher(T e) { return navigableSet.higher(e); }
    public Iterator<T> iterator() {
        return createSealHonoringIterator(navigableSet.iterator());
    }
    public Iterator<T> descendingIterator() {
        return createSealHonoringIterator(navigableSet.descendingIterator());
    }
    public NavigableSet<T> descendingSet() {
        return new SealableNavigableSet<>(navigableSet.descendingSet(), sealedSupplier);
    }
    public SortedSet<T> subSet(T fromElement, T toElement) {
        return subSet(fromElement, true, toElement, false);
    }
    public NavigableSet<T> subSet(T fromElement, boolean fromInclusive, T toElement, boolean toInclusive) {
        return new SealableNavigableSet<>(navigableSet.subSet(fromElement, fromInclusive, toElement, toInclusive), sealedSupplier);
    }
    public SortedSet<T> headSet(T toElement) {
        return headSet(toElement, false);
    }
    public NavigableSet<T> headSet(T toElement, boolean inclusive) {
        return new SealableNavigableSet<>(navigableSet.headSet(toElement, inclusive), sealedSupplier);
    }
    public SortedSet<T> tailSet(T fromElement) {
        return tailSet(fromElement, false);
    }
    public NavigableSet<T> tailSet(T fromElement, boolean inclusive) {
        return new SealableNavigableSet<>(navigableSet.tailSet(fromElement, inclusive), sealedSupplier);
    }

    // Mutable APIs
    public boolean add(T e) { throwIfSealed(); return navigableSet.add(e); }
    public boolean addAll(Collection<? extends T> col) { throwIfSealed(); return navigableSet.addAll(col); }
    public void clear() { throwIfSealed(); navigableSet.clear(); }
    public boolean remove(Object o) { throwIfSealed(); return navigableSet.remove(o); }
    public boolean removeAll(Collection<?> col) { throwIfSealed(); return navigableSet.removeAll(col); }
    public boolean retainAll(Collection<?> col) { throwIfSealed(); return navigableSet.retainAll(col); }
    public T pollFirst() { throwIfSealed(); return navigableSet.pollFirst(); }
    public T pollLast() { throwIfSealed(); return navigableSet.pollLast(); }

    private Iterator<T> createSealHonoringIterator(Iterator<T> iterator) {
        return new Iterator<T>() {
            public boolean hasNext() { return iterator.hasNext(); }
            public T next() {
                T item = iterator.next();
                if (item instanceof Map.Entry) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) item;
                    return (T) new SealableSet.SealAwareEntry<>(entry, sealedSupplier);
                }
                return item;
            }
            public void remove() { throwIfSealed(); iterator.remove();}
        };
    }
}