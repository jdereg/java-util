package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ConcurrentList provides a List or List wrapper that is thread-safe, usable in highly concurrent
 * environments. It provides a no-arg constructor that will directly return a ConcurrentList that is
 * thread-safe.  It has a constructor that takes a List argument, which will wrap that List and make it
 * thread-safe (no elements are duplicated).
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
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Use this no-arg constructor to create a ConcurrentList.
     */
    public ConcurrentList() {
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

    public List<E> subList(int fromIndex, int toIndex) { return new ConcurrentList<>(list.subList(fromIndex, toIndex)); }

    public ListIterator<E> listIterator() {
        return createLockHonoringListIterator(list.listIterator());
    }

    public ListIterator<E> listIterator(int index) {
        return createLockHonoringListIterator(list.listIterator(index));
    }

    private ListIterator<E> createLockHonoringListIterator(ListIterator<E> iterator) {
        return new ListIterator<E>() {
            public boolean hasNext() {
                lock.readLock().lock();
                try {
                    return iterator.hasNext();
                } finally {
                    lock.readLock().unlock();
                }
            }
            public E next() {
                lock.readLock().lock();
                try {
                    return iterator.next();
                } finally {
                    lock.readLock().unlock();
                }
            }
            public boolean hasPrevious() {
                lock.readLock().lock();
                try {
                    return iterator.hasPrevious();
                } finally {
                    lock.readLock().unlock();
                }
            }
            public E previous() {
                lock.readLock().lock();
                try {
                    return iterator.previous();
                } finally {
                    lock.readLock().unlock();
                }
            }
            public int nextIndex() {
                lock.readLock().lock();
                try {
                    return iterator.nextIndex();
                } finally {
                    lock.readLock().unlock();
                }
            }
            public int previousIndex() {
                lock.readLock().lock();
                try {
                    return iterator.previousIndex();
                } finally {
                    lock.readLock().unlock();
                }
            }
            public void remove() {
                lock.writeLock().lock();
                try {
                    iterator.remove();
                } finally {
                    lock.writeLock().unlock();
                }
            }
            public void set(E e) {
                lock.writeLock().lock();
                try {
                    iterator.set(e);
                } finally {
                    lock.writeLock().unlock();
                }
            }
            public void add(E e) {
                lock.writeLock().lock();
                try {
                    iterator.add(e);
                } finally {
                    lock.writeLock().unlock();
                }
            }
        };
    }
}
