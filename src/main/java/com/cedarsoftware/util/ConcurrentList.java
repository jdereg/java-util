package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * A thread-safe implementation of the {@link List} interface, designed for use in highly concurrent environments.
 * <p>
 * The {@code ConcurrentList} can be used either as a standalone thread-safe list or as a wrapper to make an existing
 * list thread-safe. It ensures thread safety without duplicating elements, making it suitable for applications
 * requiring synchronized access to list data.
 * </p>
 *
 * <h2>Features</h2>
 * <ul>
 *   <li><b>Standalone Mode:</b> Use the no-argument constructor to create a new thread-safe {@code ConcurrentList}.</li>
 *   <li><b>Wrapper Mode:</b> Pass an existing {@link List} to the constructor to wrap it with thread-safe behavior.</li>
 *   <li><b>Read-Only Iterators:</b> The {@link #iterator()} and {@link #listIterator()} methods return a read-only
 *       snapshot of the list at the time of the call, ensuring safe iteration in concurrent environments.</li>
 *   <li><b>Unsupported Operations:</b> Due to the dynamic nature of concurrent edits, the following operations are
 *       not implemented:
 *       <ul>
 *         <li>{@link #listIterator(int)}: The starting index may no longer be valid due to concurrent modifications.</li>
 *         <li>{@link #subList(int, int)}: The range may exceed the current list size in a concurrent context.</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * All public methods of {@code ConcurrentList} are thread-safe, ensuring that modifications and access
 * operations can safely occur concurrently. However, thread safety depends on the correctness of the provided
 * backing list in wrapper mode.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Standalone thread-safe list
 * ConcurrentList<String> standaloneList = new ConcurrentList<>();
 * standaloneList.add("Hello");
 * standaloneList.add("World");
 *
 * // Wrapping an existing list
 * List<String> existingList = new ArrayList<>();
 * existingList.add("Java");
 * existingList.add("Concurrency");
 * ConcurrentList<String> wrappedList = new ConcurrentList<>(existingList);
 * }</pre>
 *
 * <h2>Performance Considerations</h2>
 * <p>
 * The {@link #iterator()} and {@link #listIterator()} methods return read-only views created by copying
 * the list contents, which ensures thread safety but may incur a performance cost for very large lists.
 * Modifications to the list during iteration will not be reflected in the iterators.
 * </p>
 *
 * <h2>Additional Notes</h2>
 * <ul>
 *   <li>{@code ConcurrentList} supports {@code null} elements if the underlying list does.</li>
 *   <li>{@link #listIterator(int)} and {@link #subList(int, int)} throw {@link UnsupportedOperationException}.</li>
 * </ul>
 *
 * @param <E> The type of elements in this list
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
 * @see List
 */
public class ConcurrentList<E> implements List<E> {
    private final List<E> list;
    private final transient ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * No-arg constructor to create an empty ConcurrentList, wrapping an ArrayList.
     */
    public ConcurrentList() {
        this.list = new ArrayList<>();
    }

    /**
     * Initial capacity support
     */
    public ConcurrentList(int size) {
        this.list = new ArrayList<>(size);
    }

    /**
     * Use this constructor to wrap a List (any kind of List) and make it a ConcurrentList.
     * No duplicate of the List is created and the original list is operated on directly.
     * @param list List instance to protect.
     */
    public ConcurrentList(List<E> list) {
        if (list == null) {
            throw new IllegalArgumentException("List cannot be null");
        }
        this.list = list;
    }

    // Immutable APIs
    public boolean equals(Object other) { return readOperation(() -> list.equals(other)); }
    public int hashCode() { return readOperation(list::hashCode); }
    public String toString() { return readOperation(list::toString); }
    public int size() { return readOperation(list::size); }
    public boolean isEmpty() { return readOperation(list::isEmpty); }
    public boolean contains(Object o) { return readOperation(() -> list.contains(o)); }
    public boolean containsAll(Collection<?> c) { return readOperation(() -> new HashSet<>(list).containsAll(c)); }
    public E get(int index) { return readOperation(() -> list.get(index)); }
    public int indexOf(Object o) { return readOperation(() -> list.indexOf(o)); }
    public int lastIndexOf(Object o) { return readOperation(() -> list.lastIndexOf(o)); }
    public Iterator<E> iterator() { return readOperation(() -> new ArrayList<>(list).iterator()); }
    public Object[] toArray() { return readOperation(list::toArray); }
    public <T> T[] toArray(T[] a) { return readOperation(() -> list.toArray(a)); }

    // Mutable APIs
    public boolean add(E e) { return writeOperation(() -> list.add(e)); }
    public boolean addAll(Collection<? extends E> c) { return writeOperation(() -> list.addAll(c)); }
    public boolean addAll(int index, Collection<? extends E> c) { return writeOperation(() -> list.addAll(index, c)); }
    public void add(int index, E element) {
        writeOperation(() -> {
            list.add(index, element);
            return null;
        });
    }
    public E set(int index, E element) { return writeOperation(() -> list.set(index, element)); }
    public E remove(int index) { return writeOperation(() -> list.remove(index)); }
    public boolean remove(Object o) { return writeOperation(() -> list.remove(o)); }
    public boolean removeAll(Collection<?> c) { return writeOperation(() -> list.removeAll(c)); }
    public boolean retainAll(Collection<?> c) { return writeOperation(() -> list.retainAll(c)); }
    public void clear() {
        writeOperation(() -> {
            list.clear();
            return null; // To comply with the Supplier<T> return type
        });
    }
    public ListIterator<E> listIterator() { return readOperation(() -> new ArrayList<>(list).listIterator()); }

    // Unsupported operations
    public ListIterator<E> listIterator(int index) { throw new UnsupportedOperationException("listIterator(index) not implemented for ConcurrentList"); }
    public List<E> subList(int fromIndex, int toIndex) { throw new UnsupportedOperationException("subList not implemented for ConcurrentList"); }

    private <T> T readOperation(Supplier<T> operation) {
        lock.readLock().lock();
        try {
            return operation.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    private <T> T writeOperation(Supplier<T> operation) {
        lock.writeLock().lock();
        try {
            return operation.get();
        } finally {
            lock.writeLock().unlock();
        }
    }
}