package com.cedarsoftware.util;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A lightweight Set implementation that uses object identity (==) instead of equals()
 * for element comparison. Uses open addressing with linear probing for minimal overhead.
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Implements full {@link java.util.Set} interface</li>
 *   <li>Uses object identity (==) not equals() for comparison</li>
 *   <li>No Entry objects - single Object[] array</li>
 *   <li>Single identityHashCode call per operation</li>
 *   <li>Excellent cache locality</li>
 * </ul>
 *
 * <p>This class is a high-performance, drop-in replacement for:</p>
 * <pre>{@code
 * Set<Object> set = Collections.newSetFromMap(new IdentityHashMap<>());
 * }</pre>
 *
 * <p>Performance benefits over IdentityHashMap-backed Set:</p>
 * <ul>
 *   <li>No wrapper layer indirection</li>
 *   <li>No Entry object allocations</li>
 *   <li>No Boolean.TRUE value storage</li>
 *   <li>Better CPU cache utilization (contiguous array)</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is <i>not</i> thread-safe. If multiple threads
 * access an IdentitySet concurrently, external synchronization is required.</p>
 *
 * @param <T> the type of elements maintained by this set
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
public class IdentitySet<T> extends AbstractSet<T> {
    private static final int DEFAULT_CAPACITY = 16;
    private static final int MAX_CAPACITY = 1 << 30;  // Largest power-of-2 for int
    private static final float LOAD_FACTOR = 0.5f;  // Keep load low for fast probing

    // Sentinel for deleted slots to maintain probe chains
    private static final Object DELETED = new Object();

    private Object[] elements;
    private int size;
    private int threshold;
    private int mask;

    /**
     * Creates a new IdentitySet with default initial capacity (16).
     */
    public IdentitySet() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Creates a new IdentitySet with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity (will be rounded up to power of 2)
     */
    public IdentitySet(int initialCapacity) {
        // Round up to power of 2, capping at MAX_CAPACITY to prevent int overflow
        int capacity = 1;
        int target = Math.min(Math.max(initialCapacity, 1), MAX_CAPACITY);
        while (capacity < target) {
            capacity <<= 1;
        }
        elements = new Object[capacity];
        mask = capacity - 1;
        threshold = (int) (capacity * LOAD_FACTOR);
    }

    /**
     * Creates a new IdentitySet containing the elements of the specified collection.
     *
     * @param c the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null or contains null elements
     */
    public IdentitySet(Collection<? extends T> c) {
        this(Math.max((int) (c.size() / LOAD_FACTOR) + 1, DEFAULT_CAPACITY));
        addAll(c);
    }

    /**
     * Adds an element to this set using identity comparison.
     *
     * @param element the element to add (must not be null)
     * @return true if the element was added (was not already present), false otherwise
     * @throws NullPointerException if the element is null
     */
    @Override
    public boolean add(T element) {
        if (element == null) {
            throw new NullPointerException("IdentitySet does not support null elements");
        }
        if (size >= threshold) {
            resize();
        }
        return addInternal(element);
    }

    private boolean addInternal(Object element) {
        final int hash = System.identityHashCode(element);
        int index = hash & mask;
        final Object[] e = elements;
        int firstDeleted = -1;

        // Linear probe — must scan past DELETED slots to check for existing duplicates
        while (true) {
            Object existing = e[index];
            if (existing == null) {
                // Element not in set — insert at first DELETED slot if one was seen, else here
                int insertIndex = firstDeleted >= 0 ? firstDeleted : index;
                e[insertIndex] = element;
                size++;
                return true;
            }
            if (existing == DELETED) {
                if (firstDeleted < 0) {
                    firstDeleted = index;
                }
            } else if (existing == element) {  // Identity comparison - already present
                return false;
            }
            index = (index + 1) & mask;
        }
    }

    /**
     * Checks if this set contains the specified element using identity comparison.
     *
     * @param element the element to check for
     * @return true if the element is present, false otherwise
     */
    @Override
    public boolean contains(Object element) {
        if (element == null) {
            return false;
        }
        final int hash = System.identityHashCode(element);
        int index = hash & mask;
        final Object[] e = elements;

        while (true) {
            Object existing = e[index];
            if (existing == null) {
                return false;
            }
            if (existing == element) {  // Identity comparison
                return true;
            }
            // Continue probing (DELETED slots don't stop the search)
            index = (index + 1) & mask;
        }
    }

    /**
     * Removes the specified element from this set using identity comparison.
     *
     * @param element the element to remove
     * @return true if the element was removed (was present), false otherwise
     */
    @Override
    public boolean remove(Object element) {
        if (element == null) {
            return false;
        }
        final int hash = System.identityHashCode(element);
        int index = hash & mask;
        final Object[] e = elements;

        while (true) {
            Object existing = e[index];
            if (existing == null) {
                return false;
            }
            if (existing == element) {  // Identity comparison - found it
                e[index] = DELETED;
                size--;
                return true;
            }
            index = (index + 1) & mask;
        }
    }

    private void resize() {
        final Object[] oldElements = elements;
        final int oldCapacity = oldElements.length;

        final int newCapacity = oldCapacity << 1;
        elements = new Object[newCapacity];
        mask = newCapacity - 1;
        threshold = (int) (newCapacity * LOAD_FACTOR);
        size = 0;

        for (int i = 0; i < oldCapacity; i++) {
            Object element = oldElements[i];
            if (element != null && element != DELETED) {
                addInternal(element);
            }
        }
    }

    /**
     * Removes all elements from this set.
     */
    @Override
    public void clear() {
        final Object[] e = elements;
        for (int i = 0; i < e.length; i++) {
            e[i] = null;
        }
        size = 0;
    }

    /**
     * Returns the number of elements in this set.
     *
     * @return the number of elements
     */
    @Override
    public int size() {
        return size;
    }

    /**
     * Returns true if this set contains no elements.
     *
     * @return true if empty, false otherwise
     */
    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns an iterator over the elements in this set. The elements are returned
     * in no particular order.
     *
     * @return an iterator over the elements in this set
     */
    @Override
    public Iterator<T> iterator() {
        return new IdentitySetIterator();
    }

    /**
     * Iterator implementation that skips null and DELETED slots.
     */
    private class IdentitySetIterator implements Iterator<T> {
        private int index = 0;
        private int remaining = size;
        private int lastReturnedIndex = -1;

        @Override
        public boolean hasNext() {
            return remaining > 0;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            if (remaining <= 0) {
                throw new NoSuchElementException();
            }
            final Object[] e = elements;
            while (index < e.length) {
                Object element = e[index];
                if (element != null && element != DELETED) {
                    lastReturnedIndex = index;
                    index++;
                    remaining--;
                    return (T) element;
                }
                index++;
            }
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            if (lastReturnedIndex < 0) {
                throw new IllegalStateException();
            }
            elements[lastReturnedIndex] = DELETED;
            size--;
            lastReturnedIndex = -1;
        }
    }
}
