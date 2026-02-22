package com.cedarsoftware.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * ConcurrentNavigableSetNullSafe is a thread-safe implementation of NavigableSet
 * that allows null elements by using a unique sentinel value internally.
 *
 * @param <E> The type of elements maintained by this set
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
public class ConcurrentNavigableSetNullSafe<E> extends AbstractSet<E> implements NavigableSet<E> {

    private final NavigableSet<Object> internalSet;
    private final Comparator<? super E> originalComparator;
    private final Comparator<? super E> viewComparator;
    private volatile boolean comparatorRejectsNulls;
    // Use a unique Object instance as sentinel - cannot collide with user data
    // and identity comparison (==) is used for detection
    private static final Object NULL_ELEMENT_SENTINEL = new Object();

    /**
     * Constructs a new, empty ConcurrentNavigableSetNullSafe with natural ordering of its elements.
     * All elements inserted must implement the Comparable interface.
     */
    public ConcurrentNavigableSetNullSafe() {
        // Use natural ordering
        this.originalComparator = null;
        this.viewComparator = null;
        Comparator<Object> comp = wrapComparator(null);
        this.internalSet = new ConcurrentSkipListSet<>(comp);
    }

    /**
     * Constructs a new, empty ConcurrentNavigableSetNullSafe with the specified comparator.
     *
     * @param comparator the comparator that will be used to order this set. If null, the natural
     *                   ordering of the elements will be used.
     */
    public ConcurrentNavigableSetNullSafe(Comparator<? super E> comparator) {
        this.originalComparator = comparator;
        this.viewComparator = comparator;
        Comparator<Object> comp = wrapComparator(comparator);
        this.internalSet = new ConcurrentSkipListSet<>(comp);
    }

    /**
     * Constructs a new ConcurrentNavigableSetNullSafe containing the elements in the specified collection.
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     */
    public ConcurrentNavigableSetNullSafe(Collection<? extends E> c) {
        // Use natural ordering
        this.originalComparator = null;
        this.viewComparator = null;
        Comparator<Object> comp = wrapComparator(null);
        this.internalSet = new ConcurrentSkipListSet<>(comp);
        this.addAll(c); // Ensure masking of null elements
    }

    /**
     * Constructs a new ConcurrentNavigableSetNullSafe containing the elements in the specified collection,
     * ordered according to the provided comparator.
     *
     * @param c          the collection whose elements are to be placed into this set
     * @param comparator the comparator that will be used to order this set. If null, the natural
     *                   ordering of the elements will be used.
     * @throws NullPointerException if the specified collection is null
     */
    public ConcurrentNavigableSetNullSafe(Collection<? extends E> c, Comparator<? super E> comparator) {
        this.originalComparator = comparator;
        this.viewComparator = comparator;
        Comparator<Object> comp = wrapComparator(comparator);
        this.internalSet = new ConcurrentSkipListSet<>(comp);
        this.addAll(c); // Ensure masking of null elements
    }

    private ConcurrentNavigableSetNullSafe(NavigableSet<Object> internalSet, Comparator<? super E> comparator, Comparator<? super E> viewComparator) {
        this.internalSet = internalSet;
        this.originalComparator = comparator;
        this.viewComparator = viewComparator;
    }
    
    /**
     * Masks null elements with a sentinel value.
     *
     * @param element the element to mask
     * @return the masked element
     */
    private Object maskNull(E element) {
        return element == null ? NULL_ELEMENT_SENTINEL : element;
    }

    /**
     * Unmasks elements, converting the sentinel value back to null.
     * Uses identity comparison (==) since sentinel is a unique Object instance.
     *
     * @param maskedElement the masked element
     * @return the unmasked element
     */
    @SuppressWarnings("unchecked")
    private E unmaskNull(Object maskedElement) {
        return maskedElement == NULL_ELEMENT_SENTINEL ? null : (E) maskedElement;
    }

    /**
     * Wraps the user-provided comparator to handle the sentinel value and ensure proper ordering of null elements.
     * User comparators that handle nulls (via Comparator.nullsFirst/nullsLast or custom null handling) will
     * have their null ordering respected. User comparators that don't handle nulls will gracefully fall back
     * to default null ordering (nulls greater than non-nulls).
     *
     * @param comparator the user-provided comparator
     * @return a comparator that handles the sentinel value
     */
    @SuppressWarnings("unchecked")
    private Comparator<Object> wrapComparator(Comparator<? super E> comparator) {
        return (o1, o2) -> {
            // Handle the sentinel values - use identity comparison since sentinel is a unique object
            boolean o1IsNullSentinel = o1 == NULL_ELEMENT_SENTINEL;
            boolean o2IsNullSentinel = o2 == NULL_ELEMENT_SENTINEL;

            // Unmask the sentinels back to null
            E e1 = o1IsNullSentinel ? null : (E) o1;
            E e2 = o2IsNullSentinel ? null : (E) o2;

            // If user provided a comparator, try it first - many comparators handle nulls correctly
            // via Comparator.nullsFirst/nullsLast or custom null handling
            if (comparator != null) {
                if (!o1IsNullSentinel && !o2IsNullSentinel) {
                    // Both non-null - safe to use comparator directly
                    return comparator.compare(e1, e2);
                }
                // At least one null - try comparator once unless known to reject nulls.
                if (!comparatorRejectsNulls) {
                    try {
                        return comparator.compare(e1, e2);
                    } catch (NullPointerException npe) {
                        comparatorRejectsNulls = true;
                    }
                }
            }

            // Default null handling (for natural ordering, or if user comparator threw NPE on nulls)
            if (e1 == null && e2 == null) {
                return 0;
            }
            if (e1 == null) {
                return 1; // Nulls are considered greater in natural ordering
            }
            if (e2 == null) {
                return -1;
            }

            // Both elements are non-null - use natural ordering
            return ((Comparable<E>) e1).compareTo(e2);
        };
    }

    @Override
    public Comparator<? super E> comparator() {
        return viewComparator;
    }

    // Implement NavigableSet methods

    @Override
    public E lower(E e) {
        Object masked = internalSet.lower(maskNull(e));
        return unmaskNull(masked);
    }

    @Override
    public E floor(E e) {
        Object masked = internalSet.floor(maskNull(e));
        return unmaskNull(masked);
    }

    @Override
    public E ceiling(E e) {
        Object masked = internalSet.ceiling(maskNull(e));
        return unmaskNull(masked);
    }

    @Override
    public E higher(E e) {
        Object masked = internalSet.higher(maskNull(e));
        return unmaskNull(masked);
    }

    @Override
    public E pollFirst() {
        Object masked = internalSet.pollFirst();
        return unmaskNull(masked);
    }

    @Override
    public E pollLast() {
        Object masked = internalSet.pollLast();
        return unmaskNull(masked);
    }

    @Override
    public Iterator<E> iterator() {
        Iterator<Object> it = internalSet.iterator();
        return new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                return unmaskNull(it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    @Override
    public NavigableSet<E> descendingSet() {
        NavigableSet<Object> descendingInternalSet = internalSet.descendingSet();
        return new ConcurrentNavigableSetNullSafe<>(descendingInternalSet, originalComparator, createViewComparator(descendingInternalSet));
    }

    @Override
    public Iterator<E> descendingIterator() {
        Iterator<Object> it = internalSet.descendingIterator();
        return new Iterator<E>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public E next() {
                return unmaskNull(it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        };
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        Object maskedFrom = maskNull(fromElement);
        Object maskedTo = maskNull(toElement);

        NavigableSet<Object> subInternal = internalSet.subSet(maskedFrom, fromInclusive, maskedTo, toInclusive);
        return new ConcurrentNavigableSetNullSafe<>(subInternal, originalComparator, viewComparator);
    }


    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        NavigableSet<Object> headInternal = internalSet.headSet(maskNull(toElement), inclusive);
        return new ConcurrentNavigableSetNullSafe<>(headInternal, originalComparator, viewComparator);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        NavigableSet<Object> tailInternal = internalSet.tailSet(maskNull(fromElement), inclusive);
        return new ConcurrentNavigableSetNullSafe<>(tailInternal, originalComparator, viewComparator);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        Object masked = internalSet.first();
        return unmaskNull(masked);
    }

    @Override
    public E last() {
        Object masked = internalSet.last();
        return unmaskNull(masked);
    }

    // Implement Set methods

    @Override
    public int size() {
        return internalSet.size();
    }

    @Override
    public boolean isEmpty() {
        return internalSet.isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return internalSet.contains(maskNull((E) o));
    }

    @Override
    public boolean add(E e) {
        return internalSet.add(maskNull(e));
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean remove(Object o) {
        return internalSet.remove(maskNull((E) o));
    }

    @Override
    public void clear() {
        internalSet.clear();
    }

    @SuppressWarnings("unchecked")
    private Comparator<? super E> createViewComparator(NavigableSet<Object> viewSet) {
        Comparator<Object> comparator = (Comparator<Object>) viewSet.comparator();
        if (comparator == null) {
            return null;
        }
        return (a, b) -> comparator.compare(maskNull(a), maskNull(b));
    }
}
