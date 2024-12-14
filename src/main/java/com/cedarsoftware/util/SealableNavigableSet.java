package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.SortedSet;
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
 *         
 * @deprecated This class is no longer supported.
 */
@Deprecated
public class SealableNavigableSet<E> implements NavigableSet<E> {
    private final NavigableSet<E> navSet;
    private final transient Supplier<Boolean> sealedSupplier;

    /**
     * Create a NavigableSealableSet. Since a NavigableSet is not supplied, this will use a ConcurrentSkipListSet
     * internally.  If you want to use a TreeSet for example, use the SealableNavigableSet constructor that takes
     * a NavigableSet and pass it the instance you want it to wrap.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableSet(Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        navSet = new ConcurrentNavigableSetNullSafe<>();
    }

    /**
     * Create a NavigableSealableSet. Since a NavigableSet is not supplied, this will use a ConcurrentSkipListSet
     * internally. If you want to use a TreeSet for example, use the SealableNavigableSet constructor that takes
     * a NavigableSet and pass it the instance you want it to wrap.
     * @param comparator {@code Comparator} A comparison function, which imposes a <i>total ordering</i> on some
     * collection of objects.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableSet(Comparator<? super E> comparator, Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        navSet = new ConcurrentNavigableSetNullSafe<>(comparator);
    }

    /**
     * Create a NavigableSealableSet. Since NavigableSet is not supplied, the elements from the passed in Collection
     * will be copied to an internal ConcurrentSkipListSet. If you want to use a TreeSet for example, use the
     * SealableNavigableSet constructor that takes a NavigableSet and pass it the instance you want it to wrap.
     * @param col Collection to supply initial elements.  These are copied to an internal ConcurrentSkipListSet.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableSet(Collection<? extends E> col, Supplier<Boolean> sealedSupplier) {
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
    public SealableNavigableSet(SortedSet<E> set, Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        navSet = new ConcurrentNavigableSetNullSafe<>(set);
    }

    /**
     * Use this constructor to wrap a NavigableSet (any kind of NavigableSet) and make it a SealableNavigableSet.
     * No duplicate of the Set is created, the original set is operated on directly if unsealed, or protected
     * from changes if sealed.
     * @param set NavigableSet instance to protect.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableNavigableSet(NavigableSet<E> set, Supplier<Boolean> sealedSupplier) {
        this.sealedSupplier = sealedSupplier;
        navSet = set;
    }

    private void throwIfSealed() {
        if (sealedSupplier.get()) {
            throw new UnsupportedOperationException("This set has been sealed and is now immutable");
        }
    }

    // Immutable APIs
    public boolean equals(Object o) { return o == this || navSet.equals(o); }
    public int hashCode() { return navSet.hashCode(); }
    public String toString() { return navSet.toString(); }
    public int size() { return navSet.size(); }
    public boolean isEmpty() { return navSet.isEmpty(); }
    public boolean contains(Object o) { return navSet.contains(o); }
    public boolean containsAll(Collection<?> col) { return navSet.containsAll(col);}
    public Comparator<? super E> comparator() { return navSet.comparator(); }
    public E first() { return navSet.first(); }
    public E last() { return navSet.last(); }
    public Object[] toArray() { return navSet.toArray(); }
    public <T1> T1[] toArray(T1[] a) { return navSet.toArray(a); }
    public E lower(E e) { return navSet.lower(e); }
    public E floor(E e) { return navSet.floor(e); }
    public E ceiling(E e) { return navSet.ceiling(e); }
    public E higher(E e) { return navSet.higher(e); }
    public Iterator<E> iterator() {
        return createSealHonoringIterator(navSet.iterator());
    }
    public Iterator<E> descendingIterator() {
        return createSealHonoringIterator(navSet.descendingIterator());
    }
    public NavigableSet<E> descendingSet() {
        return new SealableNavigableSet<>(navSet.descendingSet(), sealedSupplier);
    }
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return new SealableNavigableSet<>(navSet.subSet(fromElement, fromInclusive, toElement, toInclusive), sealedSupplier);
    }
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new SealableNavigableSet<>(navSet.headSet(toElement, inclusive), sealedSupplier);
    }
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, false);
    }
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new SealableNavigableSet<>(navSet.tailSet(fromElement, inclusive), sealedSupplier);
    }

    // Mutable APIs
    public boolean add(E e) { throwIfSealed(); return navSet.add(e); }
    public boolean addAll(Collection<? extends E> col) { throwIfSealed(); return navSet.addAll(col); }
    public void clear() { throwIfSealed(); navSet.clear(); }
    public boolean remove(Object o) { throwIfSealed(); return navSet.remove(o); }
    public boolean removeAll(Collection<?> col) { throwIfSealed(); return navSet.removeAll(col); }
    public boolean retainAll(Collection<?> col) { throwIfSealed(); return navSet.retainAll(col); }
    public E pollFirst() { throwIfSealed(); return navSet.pollFirst(); }
    public E pollLast() { throwIfSealed(); return navSet.pollLast(); }

    private Iterator<E> createSealHonoringIterator(Iterator<E> iterator) {
        return new Iterator<E>() {
            public boolean hasNext() { return iterator.hasNext(); }
            public E next() {
                E item = iterator.next();
                if (item instanceof Map.Entry) {
                    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) item;
                    return (E) new SealableSet.SealAwareEntry<>(entry, sealedSupplier);
                }
                return item;
            }
            public void remove() { throwIfSealed(); iterator.remove();}
        };
    }
}