package com.cedarsoftware.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * Thread-safe list implementation backed by chunked {@link AtomicReferenceArray} buckets.
 * Each bucket holds {@value #BUCKET_SIZE} elements and never moves once allocated.
 * Appends and removals at either end use atomic counters for constant-time performance.
 * Rare middle insertions or removals rebuild the structure under a write lock.
 *
 * @param <E> element type
 */
public final class ConcurrentList<E> implements List<E>, Deque<E>, RandomAccess, Serializable {
    private static final long serialVersionUID = 1L;

    private static final int BUCKET_SIZE = 1024;

    private final ConcurrentMap<Integer, AtomicReferenceArray<Object>> buckets = new ConcurrentHashMap<>();
    private final AtomicLong head = new AtomicLong(0);
    private final AtomicLong tail = new AtomicLong(0);

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** Creates an empty list. */
    public ConcurrentList() {
    }

    /**
     * Creates an empty list with the provided initial capacity hint.
     *
     * @param initialCapacity ignored but kept for API compatibility
     */
    public ConcurrentList(int initialCapacity) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity cannot be negative: " + initialCapacity);
        }
    }

    /**
     * Creates a list containing the elements of the provided collection.
     *
     * @param collection elements to copy
     */
    public ConcurrentList(Collection<? extends E> collection) {
        Objects.requireNonNull(collection, "collection cannot be null");
        addAll(collection);
    }

    private static int bucketIndex(long pos) {
        return (int) Math.floorDiv(pos, BUCKET_SIZE);
    }

    private static int bucketOffset(long pos) {
        return (int) Math.floorMod(pos, BUCKET_SIZE);
    }

    private AtomicReferenceArray<Object> ensureBucket(int index) {
        AtomicReferenceArray<Object> bucket = buckets.get(index);
        if (bucket == null) {
            bucket = new AtomicReferenceArray<>(BUCKET_SIZE);
            AtomicReferenceArray<Object> existing = buckets.putIfAbsent(index, bucket);
            if (existing != null) {
                bucket = existing;
            }
        }
        return bucket;
    }

    private AtomicReferenceArray<Object> getBucket(int index) {
        AtomicReferenceArray<Object> bucket = buckets.get(index);
        if (bucket == null) {
            return ensureBucket(index);
        }
        return bucket;
    }

    @Override
    public int size() {
        long diff = tail.get() - head.get();
        return diff > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) diff;
    }

    @Override
    public boolean isEmpty() {
        return tail.get() == head.get();
    }

    @Override
    public boolean contains(Object o) {
        for (Object element : this) {
            if (Objects.equals(o, element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        Object[] snapshot = toArray();
        List<E> list = new ArrayList<>(snapshot.length);
        for (Object obj : snapshot) {
            @SuppressWarnings("unchecked")
            E e = (E) obj;
            list.add(e);
        }
        return list.iterator();
    }

    @Override
    public Object[] toArray() {
        lock.readLock().lock();
        try {
            int sz = size();
            Object[] array = new Object[sz];
            for (int i = 0; i < sz; i++) {
                array[i] = get(i);
            }
            return array;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        lock.readLock().lock();
        try {
            int sz = size();
            if (a.length < sz) {
                a = (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), sz);
            }
            for (int i = 0; i < sz; i++) {
                a[i] = (T) get(i);
            }
            if (a.length > sz) {
                a[sz] = null;
            }
            return a;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    @Override
    public boolean remove(Object o) {
        lock.writeLock().lock();
        try {
            int sz = size();
            for (int i = 0; i < sz; i++) {
                E element = get(i);
                if (Objects.equals(o, element)) {
                    remove(i);
                    return true;
                }
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object e : c) {
            if (!contains(e)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            addLast(e);
            modified = true;
        }
        return modified;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        lock.writeLock().lock();
        try {
            int i = index;
            for (E e : c) {
                add(i++, e);
            }
            return !c.isEmpty();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        lock.writeLock().lock();
        try {
            boolean modified = false;
            Iterator<?> it = c.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                while (remove(o)) {
                    modified = true;
                }
            }
            return modified;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        lock.writeLock().lock();
        try {
            boolean modified = false;
            int sz = size();
            for (int i = sz - 1; i >= 0; i--) {
                E element = get(i);
                if (!c.contains(element)) {
                    remove(i);
                    modified = true;
                }
            }
            return modified;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            buckets.clear();
            head.set(0);
            tail.set(0);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public E get(int index) {
        long h = head.get();
        long t = tail.get();
        long pos = h + index;
        if (index < 0 || pos >= t) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
        AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(pos));
        @SuppressWarnings("unchecked")
        E e = (E) bucket.get(bucketOffset(pos));
        return e;
    }

    @Override
    public E set(int index, E element) {
        long h = head.get();
        long t = tail.get();
        long pos = h + index;
        if (index < 0 || pos >= t) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
        AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(pos));
        @SuppressWarnings("unchecked")
        E old = (E) bucket.getAndSet(bucketOffset(pos), element);
        return old;
    }

    @Override
    public void add(int index, E element) {
        if (index == 0) {
            addFirst(element);
            return;
        }
        if (index == size()) {
            addLast(element);
            return;
        }
        lock.writeLock().lock();
        try {
            List<E> list = new ArrayList<>(this);
            list.add(index, element);
            rebuild(list);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public E remove(int index) {
        if (index == 0) {
            return removeFirst();
        }
        if (index == size() - 1) {
            return removeLast();
        }
        lock.writeLock().lock();
        try {
            List<E> list = new ArrayList<>(this);
            E removed = list.remove(index);
            rebuild(list);
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int indexOf(Object o) {
        int idx = 0;
        for (E element : this) {
            if (Objects.equals(o, element)) {
                return idx;
            }
            idx++;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int idx = size() - 1;
        ListIterator<E> it = listIterator(size());
        while (it.hasPrevious()) {
            E element = it.previous();
            if (Objects.equals(o, element)) {
                return idx;
            }
            idx--;
        }
        return -1;
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        Object[] snapshot = toArray();
        List<E> list = new ArrayList<>(snapshot.length);
        for (Object obj : snapshot) {
            @SuppressWarnings("unchecked")
            E e = (E) obj;
            list.add(e);
        }
        return list.listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("subList not implemented for ConcurrentList");
    }

    // -------- Deque --------

    @Override
    public void addFirst(E e) {
        lock.readLock().lock();
        try {
            long pos = head.decrementAndGet();
            AtomicReferenceArray<Object> bucket = ensureBucket(bucketIndex(pos));
            bucket.lazySet(bucketOffset(pos), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void addLast(E e) {
        lock.readLock().lock();
        try {
            long pos = tail.getAndIncrement();
            AtomicReferenceArray<Object> bucket = ensureBucket(bucketIndex(pos));
            bucket.lazySet(bucketOffset(pos), e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean offerFirst(E e) {
        addFirst(e);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        addLast(e);
        return true;
    }

    @Override
    public E removeFirst() {
        E e = pollFirst();
        if (e == null) {
            throw new NoSuchElementException("List is empty");
        }
        return e;
    }

    @Override
    public E removeLast() {
        E e = pollLast();
        if (e == null) {
            throw new NoSuchElementException("List is empty");
        }
        return e;
    }

    @Override
    public E pollFirst() {
        lock.readLock().lock();
        try {
            while (true) {
                long h = head.get();
                long t = tail.get();
                if (h >= t) {
                    return null;
                }
                if (head.compareAndSet(h, h + 1)) {
                    AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(h));
                    @SuppressWarnings("unchecked")
                    E val = (E) bucket.getAndSet(bucketOffset(h), null);
                    return val;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public E pollLast() {
        lock.readLock().lock();
        try {
            while (true) {
                long t = tail.get();
                long h = head.get();
                if (t <= h) {
                    return null;
                }
                long newTail = t - 1;
                if (tail.compareAndSet(t, newTail)) {
                    AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(newTail));
                    @SuppressWarnings("unchecked")
                    E val = (E) bucket.getAndSet(bucketOffset(newTail), null);
                    return val;
                }
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public E getFirst() {
        E e = peekFirst();
        if (e == null) {
            throw new NoSuchElementException("List is empty");
        }
        return e;
    }

    @Override
    public E getLast() {
        E e = peekLast();
        if (e == null) {
            throw new NoSuchElementException("List is empty");
        }
        return e;
    }

    @Override
    public E peekFirst() {
        long h = head.get();
        long t = tail.get();
        if (h >= t) {
            return null;
        }
        AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(h));
        @SuppressWarnings("unchecked")
        E val = (E) bucket.get(bucketOffset(h));
        return val;
    }

    @Override
    public E peekLast() {
        long t = tail.get();
        long h = head.get();
        if (t <= h) {
            return null;
        }
        long pos = t - 1;
        AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(pos));
        @SuppressWarnings("unchecked")
        E val = (E) bucket.get(bucketOffset(pos));
        return val;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        lock.writeLock().lock();
        try {
            for (int i = size() - 1; i >= 0; i--) {
                E element = get(i);
                if (Objects.equals(o, element)) {
                    remove(i);
                    return true;
                }
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean offer(E e) {
        return offerLast(e);
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public E poll() {
        return pollFirst();
    }

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public E peek() {
        return peekFirst();
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public Iterator<E> descendingIterator() {
        Object[] snapshot = toArray();
        return new Iterator<E>() {
            private int index = snapshot.length - 1;

            @Override
            public boolean hasNext() {
                return index >= 0;
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                if (index < 0) {
                    throw new NoSuchElementException();
                }
                return (E) snapshot[index--];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove not supported");
            }
        };
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        for (E e : this) {
            action.accept(e);
        }
    }

    private void rebuild(List<E> elements) {
        buckets.clear();
        head.set(0);
        tail.set(0);
        for (E e : elements) {
            long pos = tail.getAndIncrement();
            AtomicReferenceArray<Object> bucket = ensureBucket(bucketIndex(pos));
            bucket.lazySet(bucketOffset(pos), e);
        }
    }
}

