package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
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
    private final List<E> list = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

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
            return new ArrayList<>(list).iterator();  // Create a snapshot for iterator
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
            return list.containsAll(c);
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

    public ListIterator<E> listIterator() {
        throw new UnsupportedOperationException("ListIterator is not supported with thread-safe iteration.");
    }

    public ListIterator<E> listIterator(int index) {
        throw new UnsupportedOperationException("ListIterator is not supported with thread-safe iteration.");
    }

    public List<E> subList(int fromIndex, int toIndex) {
        lock.readLock().lock();
        try {
            return new ArrayList<>(list.subList(fromIndex, toIndex));  // Return a snapshot of the sublist
        } finally {
            lock.readLock().unlock();
        }
    }
}
