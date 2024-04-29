package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ConcurrentList provides a List and List wrapper that is thread-safe, usable in highly concurrent
 * environments. It provides a no-arg constructor that will directly return a ConcurrentList that is
 * thread-safe.  It has a constructor that takes a List argument, which will wrap that List and make it
 * thread-safe (no elements are duplicated).<br>
 * <br>
 * The iterator(), listIterator() return read-only views copied from the list.  The listIterator(index)
 * is not implemented, as the inbound index could already be outside the lists position due to concurrent
 * edits.  Similarly, subList(from, to) is not implemented because the boundaries may exceed the lists
 * size due to concurrent edits.
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
public class ConcurrentList<E> implements List<E> {
    private final List<E> list;
    private final ReadWriteLock lock;

    /**
     * Use this no-arg constructor to create a ConcurrentList.
     */
    public ConcurrentList() {
        lock = new ReentrantReadWriteLock();
        this.list = new ArrayList<>();
    }
    
    /**
     * Use this constructor to wrap a List (any kind of List) and make it a ConcurrentList.
     * No duplicate of the List is created and the original list is operated on directly.
     * @param list List instance to protect.
     */
    public ConcurrentList(List<E> list) {
        if (list == null) {
            throw new IllegalArgumentException("list cannot be null");
        }
        lock = new ReentrantReadWriteLock();
        this.list = list;
    }

    public int size() {
        lock.readLock().lock();
        try {
            return list.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return list.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean equals(Object obj) {
        lock.readLock().lock();
        try {
            return list.equals(obj);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int hashCode() {
        lock.readLock().lock();
        try {
            return list.hashCode();
        } finally {
            lock.readLock().unlock();
        }
    }

    public String toString() {
        lock.readLock().lock();
        try {
            return list.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean contains(Object o) {
        lock.readLock().lock();
        try {
            return list.contains(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Iterator<E> iterator() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(list).iterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Object[] toArray() {
        lock.readLock().lock();
        try {
            return list.toArray();
        } finally {
            lock.readLock().unlock();
        }
    }

    public <T> T[] toArray(T[] a) {
        lock.readLock().lock();
        try {
            return list.toArray(a);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean add(E e) {
        lock.writeLock().lock();
        try {
            return list.add(e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean remove(Object o) {
        lock.writeLock().lock();
        try {
            return list.remove(o);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean containsAll(Collection<?> c) {
        lock.readLock().lock();
        try {
            return new HashSet<>(list).containsAll(c);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean addAll(Collection<? extends E> c) {
        lock.writeLock().lock();
        try {
            return list.addAll(c);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        lock.writeLock().lock();
        try {
            return list.addAll(index, c);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean removeAll(Collection<?> c) {
        lock.writeLock().lock();
        try {
            return list.removeAll(c);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean retainAll(Collection<?> c) {
        lock.writeLock().lock();
        try {
            return list.retainAll(c);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            list.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public E get(int index) {
        lock.readLock().lock();
        try {
            return list.get(index);
        } finally {
            lock.readLock().unlock();
        }
    }

    public E set(int index, E element) {
        lock.writeLock().lock();
        try {
            return list.set(index, element);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void add(int index, E element) {
        lock.writeLock().lock();
        try {
            list.add(index, element);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public E remove(int index) {
        lock.writeLock().lock();
        try {
            return list.remove(index);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int indexOf(Object o) {
        lock.readLock().lock();
        try {
            return list.indexOf(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    public int lastIndexOf(Object o) {
        lock.readLock().lock();
        try {
            return list.lastIndexOf(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("subList not implemented for ConcurrentList");
    }

    public ListIterator<E> listIterator() {
        lock.readLock().lock();
        try {
            return new ArrayList<E>(list).listIterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    public ListIterator<E> listIterator(int index) {
        throw new UnsupportedOperationException("listIterator(index) not implemented for ConcurrentList");
    }
}