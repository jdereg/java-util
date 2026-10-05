package com.cedarsoftware.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A thread-safe implementation of the {@link List}, {@link Deque}, and {@link RandomAccess} interfaces,
 * designed for concurrent environments.
 *
 * <p>This implementation uses a bucket-based architecture with chunked {@link AtomicReferenceArray}
 * storage and atomic head/tail counters, guarded by a single {@link ReentrantReadWriteLock}: reads
 * take the shared read lock (parallel among readers), and all mutations take the exclusive write
 * lock. Head/tail mutations are O(1) under the brief write lock; only middle insertion/removal
 * pays an O(n) rebuild.</p>
 *
 * <h2>Architecture Overview</h2>
 * <p>The list is structured as a series of fixed-size buckets (1024 elements each), managed through a
 * {@link ConcurrentHashMap}. Each bucket is an {@link AtomicReferenceArray} that never moves once allocated,
 * ensuring stable memory layout and eliminating costly array copying operations.</p>
 *
 * <h2>Performance Characteristics</h2>
 * <table border="1">
 * <caption>Operation Performance Comparison</caption>
 * <tr><th>Operation</th><th>ArrayList + External Sync</th><th>CopyOnWriteArrayList</th><th>Vector</th><th>This Implementation</th></tr>
 * <tr><td>{@code get(index)}</td><td>🔴 O(1) but serialized</td><td>🟢 O(1) no locks</td><td>🔴 O(1) but synchronized</td><td>🟢 O(1) shared read lock</td></tr>
 * <tr><td>{@code set(index, val)}</td><td>🔴 O(1) but serialized</td><td>🔴 O(n) copy array</td><td>🔴 O(1) but synchronized</td><td>🟢 O(1) shared read lock</td></tr>
 * <tr><td>{@code add(element)}</td><td>🔴 O(1)* but serialized</td><td>🔴 O(n) copy array</td><td>🔴 O(1)* but synchronized</td><td>🟡 O(1) write lock</td></tr>
 * <tr><td>{@code addFirst(element)}</td><td>🔴 O(n) + serialized</td><td>🔴 O(n) copy array</td><td>🔴 O(n) + synchronized</td><td>🟡 O(1) write lock</td></tr>
 * <tr><td>{@code addLast(element)}</td><td>🔴 O(1)* but serialized</td><td>🔴 O(n) copy array</td><td>🔴 O(1)* but synchronized</td><td>🟡 O(1) write lock</td></tr>
 * <tr><td>{@code removeFirst()}</td><td>🔴 O(n) + serialized</td><td>🔴 O(n) copy array</td><td>🔴 O(n) + synchronized</td><td>🟡 O(1) write lock</td></tr>
 * <tr><td>{@code removeLast()}</td><td>🔴 O(1) but serialized</td><td>🔴 O(n) copy array</td><td>🔴 O(1) but synchronized</td><td>🟡 O(1) write lock</td></tr>
 * <tr><td>{@code add(middle, element)}</td><td>🔴 O(n) + serialized</td><td>🔴 O(n) copy array</td><td>🔴 O(n) + synchronized</td><td>🟡 O(n) + write lock</td></tr>
 * <tr><td>{@code remove(middle)}</td><td>🔴 O(n) + serialized</td><td>🔴 O(n) copy array</td><td>🔴 O(n) + synchronized</td><td>🟡 O(n) + write lock</td></tr>
 * <tr><td>Concurrent reads</td><td>❌ Serialized</td><td>🟢 Fully parallel</td><td>❌ Serialized</td><td>🟢 Parallel (shared read lock)</td></tr>
 * <tr><td>Concurrent writes</td><td>❌ Serialized</td><td>❌ Serialized (copy)</td><td>❌ Serialized</td><td>🔴 Serialized (write lock)</td></tr>
 * <tr><td>Memory efficiency</td><td>🟡 Resizing overhead</td><td>🔴 Constant copying</td><td>🟡 Resizing overhead</td><td>🟢 Granular allocation</td></tr>
 * </table>
 * <p><i>* O(1) amortized, may trigger O(n) array resize</i></p>
 *
 * <h2>Key Advantages</h2>
 * <ul>
 *   <li><strong>O(1) deque operations:</strong> {@code addFirst}, {@code addLast}, {@code removeFirst},
 *       {@code removeLast} update atomic head/tail counters under a brief write lock — no array shifting or copying</li>
 *   <li><strong>Parallel reads:</strong> {@code get()}, {@code set()}, {@code contains()}, iteration, and
 *       {@code toArray()} share the read lock and run concurrently with each other</li>
 *   <li><strong>No copy-on-write:</strong> Unlike {@link java.util.concurrent.CopyOnWriteArrayList}, mutations
 *       never copy the whole backing storage (except middle insertion/removal, which rebuilds)</li>
 *   <li><strong>Stable memory layout:</strong> Buckets never move, reducing GC pressure</li>
 *   <li><strong>Optimal memory usage:</strong> No wasted capacity from exponential growth strategies</li>
 * </ul>
 *
 * <h2>Use Case Recommendations</h2>
 * <ul>
 *   <li><strong>🟢 Excellent for:</strong> Queue/stack patterns, append-heavy workloads, read-heavy access,
 *       producer-consumer scenarios</li>
 *   <li><strong>🟢 Very good for:</strong> Random access patterns, bulk operations, frequent size queries</li>
 *   <li><strong>🟡 Acceptable for:</strong> Moderate middle insertion/deletion (rebuilds structure)</li>
 *   <li><strong>❌ Consider alternatives for:</strong> Frequent middle insertion/deletion, or extreme
 *       write contention where a lock-free queue (e.g. {@link java.util.concurrent.ConcurrentLinkedDeque}) fits the access pattern</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li><strong>Reads:</strong> {@code get}, {@code set}, {@code size}, iteration, and scans take the shared
 *       read lock ({@code size()}/{@code isEmpty()} are lock-free via a dedicated counter)</li>
 *   <li><strong>Writes:</strong> All structural mutations take the exclusive write lock; head/tail operations
 *       hold it only for an O(1) update</li>
 *   <li><strong>Consistent iteration:</strong> Iterators operate over a point-in-time snapshot and never throw
 *       {@link java.util.ConcurrentModificationException}</li>
 * </ul>
 *
 * <h2>Implementation Details</h2>
 * <ul>
 *   <li><strong>Bucket size:</strong> 1024 elements per bucket for optimal cache line usage</li>
 *   <li><strong>Storage:</strong> {@link ConcurrentHashMap} of {@link AtomicReferenceArray} buckets</li>
 *   <li><strong>Indexing:</strong> Atomic head/tail counters with negative indexing support</li>
 *   <li><strong>Memory management:</strong> Lazy bucket allocation, automatic cleanup of vacated buckets</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Concurrent queue
 * ConcurrentList<Task> taskQueue = new ConcurrentList<>();
 *
 * // Producer threads
 * taskQueue.addLast(new Task());     // O(1)
 *
 * // Consumer threads
 * Task task = taskQueue.pollFirst(); // O(1)
 *
 * // Stack operations
 * ConcurrentList<String> stack = new ConcurrentList<>();
 * stack.addFirst("item");            // O(1) push
 * String item = stack.removeFirst(); // O(1) pop
 *
 * // Random access
 * String value = stack.get(index);   // O(1), parallel with other reads
 * stack.set(index, "new value");     // O(1), parallel with other reads
 * }</pre>
 *
 * @param <E> the type of elements held in this list
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
public final class ConcurrentList<E> extends ConcurrentListBase<E> implements List<E>, Deque<E>, RandomAccess, Serializable {
    private static final long serialVersionUID = 1L;

    private static final int BUCKET_SHIFT = 10;
    private static final int BUCKET_SIZE = 1 << BUCKET_SHIFT;
    private static final int BUCKET_MASK = BUCKET_SIZE - 1;

    private final ConcurrentMap<Integer, AtomicReferenceArray<Object>> buckets = new ConcurrentHashMap<>();
    private final AtomicLong head = new AtomicLong(0);
    private final AtomicLong tail = new AtomicLong(0);
    private final AtomicLong sizeCounter = new AtomicLong(0);  // Dedicated size counter for O(1) lock-free size()

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** Creates an empty list. */
    public ConcurrentList() {
    }

    /**
     * Writes the list under the read lock, so the stream holds a list it really held. Default serialization of
     * the buckets, head, tail and size while another thread changed them wrote nulls never added, elements out
     * of order, or a list that failed to read back. The serial form is unchanged.
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        lock.readLock().lock();
        try {
            out.defaultWriteObject();
        } finally {
            lock.readLock().unlock();
        }
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
        return (int) (pos >> BUCKET_SHIFT);     // floor(pos / BUCKET_SIZE), negative positions included
    }

    private static int bucketOffset(long pos) {
        return (int) (pos & BUCKET_MASK);       // the mathematical mod, never negative
    }

    /**
     * Under a lock: copies the elements at indexes from .. from + count - 1 into dest from destPos, looking each
     * bucket up once rather than once per element (a map lookup by a boxed Integer, which allocates past bucket
     * 127). A typed dest checks each store, as an array store does.
     */
    private void copyRange(int from, int count, Object[] dest, int destPos) {
        long pos = head.get() + from;
        int done = 0;
        while (done < count) {
            AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(pos));
            int offset = bucketOffset(pos);
            int n = Math.min(BUCKET_SIZE - offset, count - done);
            for (int k = 0; k < n; k++) {
                dest[destPos + done + k] = bucket.get(offset + k);
            }
            done += n;
            pos += n;
        }
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
        // For valid indices (within head/tail range), bucket should always exist
        // because add operations create buckets before updating head/tail.
        // If bucket is null, it indicates a bug in the calling code.
        return buckets.get(index);
    }

    @Override
    public int size() {
        // Lock-free O(1) operation using dedicated size counter
        long size = sizeCounter.get();
        return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
    }

    @Override
    public boolean isEmpty() {
        // Lock-free O(1) operation using dedicated size counter
        return sizeCounter.get() == 0;
    }

    @Override
    public boolean contains(Object o) {
        lock.readLock().lock();
        try {
            return indexOfNoLock(o) >= 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * A spliterator over a snapshot taken now, as {@link #iterator()} iterates one: a stream sees the list as it
     * was when the stream was made. The List default (on JDK 9+) read the live list by index, so a list changing
     * mid-stream skipped elements or threw IndexOutOfBoundsException.
     */
    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator(toArray(), Spliterator.ORDERED);
    }

    @Override
    public Iterator<E> iterator() {
        Object[] snapshot = toArray();
        return new Iterator<E>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < snapshot.length;
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                if (index >= snapshot.length) {
                    throw new NoSuchElementException();
                }
                return (E) snapshot[index++];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove not supported");
            }
        };
    }

    @Override
    public Object[] toArray() {
        lock.readLock().lock();
        try {
            int sz = size();
            if (sz == 0) {
                return new Object[0];
            }
            // Build array directly - no intermediate ArrayList needed
            // Read lock ensures size is stable during iteration
            Object[] result = new Object[sz];
            copyRange(0, sz, result, 0);
            return result;
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
            if (sz == 0) {
                if (a.length > 0) {
                    a[0] = null;
                }
                return a;
            }
            // Build array directly - no intermediate ArrayList needed
            // Read lock ensures size is stable during iteration
            T[] result = a.length >= sz ? a :
                (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), sz);

            copyRange(0, sz, result, 0);

            if (result.length > sz) {
                result[sz] = null;
            }
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean add(E e) {
        addLast(e);
        return true;
    }

    /**
     * Removes the first occurrence. o's equals runs outside this list's locks (it may lock another list: running
     * it under the write lock deadlocked two lists removing from each other). Elements are copied out under the
     * read lock in doubling chunks and compared with no lock held, so finding index i copies about i elements.
     * The removal is made under the write lock if indexes 0 .. i still hold the elements compared; a miss is
     * reported once the whole list is seen to hold exactly the elements compared. Otherwise the search runs again.
     */
    @Override
    public boolean remove(Object o) {
        while (true) {
            Object[] seen = new Object[16];
            int count = 0;
            int match = -1;
            while (match < 0) {
                int from = count;
                lock.readLock().lock();
                try {
                    int n = Math.min(size() - from, Math.max(16, from));
                    if (n <= 0) {
                        break;
                    }
                    if (from + n > seen.length) {
                        seen = Arrays.copyOf(seen, Math.max(seen.length * 2, from + n));
                    }
                    copyRange(from, n, seen, from);
                    count = from + n;
                } finally {
                    lock.readLock().unlock();
                }
                for (int i = from; i < count; i++) {
                    if (Objects.equals(o, seen[i])) {
                        match = i;
                        break;
                    }
                }
            }
            if (match < 0) {
                lock.readLock().lock();
                try {
                    if (holdsAt(seen, count, 0, count)) {
                        return false;
                    }
                } finally {
                    lock.readLock().unlock();
                }
                continue;
            }
            lock.writeLock().lock();
            try {
                if (holdsAt(seen, -1, 0, match + 1)) {     // the prefix only: the list may go on past the copy
                    remove(match);
                    return true;
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /** {@link #holdsAt(Object[], int, int, int)} for a snapshot that fills its array. */
    private boolean holdsAt(Object[] snapshot, int from, int to) {
        return holdsAt(snapshot, snapshot.length, from, to);
    }

    /**
     * Under a lock: whether indexes from .. to - 1 hold exactly (by identity) the snapshot's elements at those
     * indexes, and the list is length long when the range reaches length (-1: whatever its length).
     */
    private boolean holdsAt(Object[] snapshot, int length, int from, int to) {
        int sz = size();
        if (sz < to || (to == length && sz != length)) {
            return false;
        }
        long pos = head.get() + from;
        int i = from;
        while (i < to) {
            AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(pos));
            int offset = bucketOffset(pos);
            int n = Math.min(BUCKET_SIZE - offset, to - i);
            for (int k = 0; k < n; k++) {
                if (bucket.get(offset + k) != snapshot[i + k]) {
                    return false;
                }
            }
            i += n;
            pos += n;
        }
        return true;
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
        Objects.requireNonNull(c, "collection cannot be null");
        Object[] additions = c.toArray();   // c's code runs before this list's lock is held (c may lock this list)
        if (additions.length == 0) {
            return false;
        }
        lock.writeLock().lock();
        try {
            appendNoLock(additions);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean addAll(int index, Collection<? extends E> c) {
        Objects.requireNonNull(c, "collection cannot be null");
        Object[] additions = c.toArray();   // c's code runs before this list's lock is held (c may lock this list)
        lock.writeLock().lock();
        try {
            int currentSize = size();
            if (index < 0 || index > currentSize) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + currentSize);
            }
            if (additions.length == 0) {
                return false;
            }
            if (index == currentSize) {
                appendNoLock(additions);
                return true;
            }

            List<E> elements = snapshotToListNoLock(currentSize);
            elements.addAll(index, (List<E>) Arrays.asList(additions));
            rebuild(elements);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c, "collection cannot be null");
        if (c.isEmpty()) {
            return false;
        }
        return removeMatching(c, true);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c, "collection cannot be null");
        if (c.isEmpty()) {
            lock.writeLock().lock();
            try {
                if (size() == 0) {
                    return false;
                }
                clear();
                return true;
            } finally {
                lock.writeLock().unlock();
            }
        }
        return removeMatching(c, false);
    }

    /**
     * removeAll and retainAll. Which elements go is decided on a snapshot, asking c outside this list's locks
     * (c's code may lock another list: running it under the write lock deadlocked two lists working on each
     * other), and the decision is applied under the write lock if the list still holds exactly the snapshot's
     * elements; otherwise it is decided again.
     */
    @SuppressWarnings("unchecked")
    private boolean removeMatching(Collection<?> c, boolean removeContained) {
        Collection<?> lookup = c instanceof Set ? c : new HashSet<>(c);
        while (true) {
            Object[] snapshot = toArray();
            List<E> retained = new ArrayList<>(snapshot.length);
            for (Object element : snapshot) {
                if (lookup.contains(element) != removeContained) {
                    retained.add((E) element);
                }
            }
            if (retained.size() == snapshot.length) {
                return false;
            }
            if (rebuildIfUnchanged(snapshot, retained)) {
                return true;
            }
        }
    }

    /**
     * Removes the elements the filter accepts, deciding on a snapshot with no lock held (the filter may lock
     * other lists), as removeAll does. The List default worked through the snapshot iterator, whose remove
     * throws.
     */
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        return removeWhere(filter);
    }

    /** Replaces each element with the operator's result, computed on a snapshot with no lock held. */
    @Override
    @SuppressWarnings("unchecked")
    public void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        while (true) {
            Object[] snapshot = toArray();
            Object[] replaced = new Object[snapshot.length];
            for (int i = 0; i < snapshot.length; i++) {
                replaced[i] = operator.apply((E) snapshot[i]);
            }
            if (writeOverIfUnchanged(snapshot, replaced)) {
                return;
            }
        }
    }

    /** Sorts the list: a snapshot is sorted with no lock held (the comparator may lock other lists). */
    @Override
    @SuppressWarnings("unchecked")
    public void sort(Comparator<? super E> c) {
        while (true) {
            Object[] snapshot = toArray();
            Object[] sorted = snapshot.clone();
            Arrays.sort(sorted, (Comparator<Object>) c);
            if (writeOverIfUnchanged(snapshot, sorted)) {
                return;
            }
        }
    }

    /**
     * Under the write lock: writes values into the list's slots if the list still holds exactly the snapshot's
     * elements; reports whether it did.
     */
    private boolean writeOverIfUnchanged(Object[] snapshot, Object[] values) {
        lock.writeLock().lock();
        try {
            if (!holdsAt(snapshot, 0, snapshot.length)) {
                return false;
            }
            long pos = head.get();
            int done = 0;
            while (done < values.length) {
                AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(pos));
                int offset = bucketOffset(pos);
                int n = Math.min(BUCKET_SIZE - offset, values.length - done);
                for (int k = 0; k < n; k++) {
                    bucket.set(offset + k, values[done + k]);
                }
                done += n;
                pos += n;
            }
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private boolean removeWhere(Predicate<? super E> drop) {
        while (true) {
            Object[] snapshot = toArray();
            List<E> retained = new ArrayList<>(snapshot.length);
            for (Object element : snapshot) {
                if (!drop.test((E) element)) {
                    retained.add((E) element);
                }
            }
            if (retained.size() == snapshot.length) {
                return false;
            }
            if (rebuildIfUnchanged(snapshot, retained)) {
                return true;
            }
        }
    }

    /** Under the write lock: rebuilds the list from retained if it still holds exactly the snapshot's elements. */
    private boolean rebuildIfUnchanged(Object[] snapshot, List<E> retained) {
        lock.writeLock().lock();
        try {
            if (!holdsAt(snapshot, 0, snapshot.length)) {
                return false;
            }
            rebuild(retained);
            return true;
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
            sizeCounter.set(0);  // Reset size counter
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public E get(int index) {
        lock.readLock().lock();
        try {
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
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public E set(int index, E element) {
        lock.readLock().lock();
        try {
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
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void add(int index, E element) {
        lock.writeLock().lock();
        try {
            int sz = size();
            if (index < 0 || index > sz) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + sz);
            }
            if (index == 0) {
                addFirst(element);
                return;
            }
            if (index == sz) {
                addLast(element);
                return;
            }
            List<E> list = new ArrayList<>(this);
            list.add(index, element);
            rebuild(list);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public E remove(int index) {
        lock.writeLock().lock();
        try {
            int sz = size();
            // Bounds-check up front: List contract requires IndexOutOfBoundsException
            // (remove(0) on an empty list previously surfaced as NoSuchElementException)
            if (index < 0 || index >= sz) {
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + sz);
            }
            if (index == 0) {
                return removeFirst();
            }
            if (index == sz - 1) {
                return removeLast();
            }
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
        lock.readLock().lock();
        try {
            return indexOfNoLock(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    /** Under a lock: the index of the first element equal to o, or -1; one bucket lookup per bucket. */
    private int indexOfNoLock(Object o) {
        int sz = size();
        long pos = head.get();
        int i = 0;
        while (i < sz) {
            AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(pos));
            int offset = bucketOffset(pos);
            int n = Math.min(BUCKET_SIZE - offset, sz - i);
            for (int k = 0; k < n; k++) {
                if (Objects.equals(o, bucket.get(offset + k))) {
                    return i + k;
                }
            }
            i += n;
            pos += n;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        lock.readLock().lock();
        try {
            long h = head.get();
            int i = size() - 1;
            while (i >= 0) {
                long pos = h + i;
                AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(pos));
                int offset = bucketOffset(pos);
                for (int k = offset; k >= 0 && i >= 0; k--, i--) {
                    if (Objects.equals(o, bucket.get(k))) {
                        return i;
                    }
                }
            }
            return -1;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        Object[] snapshot = toArray();
        if (index < 0 || index > snapshot.length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + snapshot.length);
        }
        return new ListIterator<E>() {
            private int cursor = index;

            @Override
            public boolean hasNext() {
                return cursor < snapshot.length;
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                if (cursor >= snapshot.length) {
                    throw new NoSuchElementException();
                }
                return (E) snapshot[cursor++];
            }

            @Override
            public boolean hasPrevious() {
                return cursor > 0;
            }

            @Override
            @SuppressWarnings("unchecked")
            public E previous() {
                if (cursor <= 0) {
                    throw new NoSuchElementException();
                }
                return (E) snapshot[--cursor];
            }

            @Override
            public int nextIndex() {
                return cursor;
            }

            @Override
            public int previousIndex() {
                return cursor - 1;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove not supported");
            }

            @Override
            public void set(E e) {
                throw new UnsupportedOperationException("set not supported");
            }

            @Override
            public void add(E e) {
                throw new UnsupportedOperationException("add not supported");
            }
        };
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("subList not implemented for ConcurrentList");
    }

    // -------- Deque --------

    @Override
    public void addFirst(E e) {
        // Use write lock to ensure atomic update of head counter + value
        // This prevents readers from seeing updated head before value is written
        lock.writeLock().lock();
        try {
            long pos = head.decrementAndGet();
            AtomicReferenceArray<Object> bucket = ensureBucket(bucketIndex(pos));
            bucket.set(bucketOffset(pos), e);
            sizeCounter.incrementAndGet();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addLast(E e) {
        // Use write lock to ensure atomic update of tail counter + value
        // This prevents readers from seeing updated tail before value is written
        lock.writeLock().lock();
        try {
            long pos = tail.getAndIncrement();
            AtomicReferenceArray<Object> bucket = ensureBucket(bucketIndex(pos));
            bucket.set(bucketOffset(pos), e);
            sizeCounter.incrementAndGet();
        } finally {
            lock.writeLock().unlock();
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
        lock.writeLock().lock();
        try {
            if (isEmpty()) {
                throw new NoSuchElementException("List is empty");
            }
            return pollFirstInternal();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public E removeLast() {
        lock.writeLock().lock();
        try {
            if (isEmpty()) {
                throw new NoSuchElementException("List is empty");
            }
            return pollLastInternal();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public E pollFirst() {
        lock.writeLock().lock();
        try {
            if (isEmpty()) {
                return null;
            }
            return pollFirstInternal();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Internal pollFirst without lock acquisition - caller must hold write lock.
     * Assumes list is not empty.
     */
    private E pollFirstInternal() {
        long h = head.getAndIncrement();
        int oldBucketIdx = bucketIndex(h);
        AtomicReferenceArray<Object> bucket = getBucket(oldBucketIdx);
        @SuppressWarnings("unchecked")
        E val = (E) bucket.getAndSet(bucketOffset(h), null);
        sizeCounter.decrementAndGet();

        // Cleanup: remove bucket if it's now completely outside the valid range
        long t = tail.get();
        if (h + 1 >= t) {
            // List is now empty, clean up all buckets
            buckets.clear();
        } else {
            // Check if head crossed into a new bucket
            int newBucketIdx = bucketIndex(h + 1);
            if (oldBucketIdx != newBucketIdx) {
                buckets.remove(oldBucketIdx);
            }
        }

        return val;
    }

    @Override
    public E pollLast() {
        lock.writeLock().lock();
        try {
            if (isEmpty()) {
                return null;
            }
            return pollLastInternal();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Internal pollLast without lock acquisition - caller must hold write lock.
     * Assumes list is not empty.
     */
    private E pollLastInternal() {
        long newTail = tail.decrementAndGet();
        int removedBucketIdx = bucketIndex(newTail);
        AtomicReferenceArray<Object> bucket = getBucket(removedBucketIdx);
        @SuppressWarnings("unchecked")
        E val = (E) bucket.getAndSet(bucketOffset(newTail), null);
        sizeCounter.decrementAndGet();

        // Cleanup: remove bucket if it's now completely outside the valid range
        // This happens when the last valid position (newTail - 1) is in a different bucket
        // Special case: if list is now empty, newTail == head, and we should clean up
        long h = head.get();
        if (newTail <= h) {
            // List is empty, clean up all buckets
            buckets.clear();
        } else {
            // Check if removed position's bucket is now outside valid range
            int prevValidBucketIdx = bucketIndex(newTail - 1);
            if (removedBucketIdx != prevValidBucketIdx) {
                buckets.remove(removedBucketIdx);
            }
        }

        return val;
    }

    @Override
    public E getFirst() {
        lock.readLock().lock();
        try {
            if (isEmpty()) {
                throw new NoSuchElementException("List is empty");
            }
            return peekFirstInternal();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public E getLast() {
        lock.readLock().lock();
        try {
            if (isEmpty()) {
                throw new NoSuchElementException("List is empty");
            }
            return peekLastInternal();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public E peekFirst() {
        lock.readLock().lock();
        try {
            if (isEmpty()) {
                return null;
            }
            return peekFirstInternal();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Internal peekFirst without lock acquisition - caller must hold read or write lock.
     * Assumes list is not empty.
     */
    private E peekFirstInternal() {
        long h = head.get();
        AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(h));
        @SuppressWarnings("unchecked")
        E val = (E) bucket.get(bucketOffset(h));
        return val;
    }

    @Override
    public E peekLast() {
        lock.readLock().lock();
        try {
            if (isEmpty()) {
                return null;
            }
            return peekLastInternal();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Internal peekLast without lock acquisition - caller must hold read or write lock.
     * Assumes list is not empty.
     */
    private E peekLastInternal() {
        long pos = tail.get() - 1;
        AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(pos));
        @SuppressWarnings("unchecked")
        E val = (E) bucket.get(bucketOffset(pos));
        return val;
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        return remove(o);
    }

    /** Removes the last occurrence: searched on a snapshot as {@link #remove(Object)} searches. */
    @Override
    public boolean removeLastOccurrence(Object o) {
        while (true) {
            Object[] snapshot = toArray();
            int i = snapshot.length - 1;
            while (i >= 0 && !Objects.equals(o, snapshot[i])) {
                i--;
            }
            if (i < 0) {
                return false;
            }
            lock.writeLock().lock();
            try {
                if (holdsAt(snapshot, i, snapshot.length)) {
                    remove(i);
                    return true;
                }
            } finally {
                lock.writeLock().unlock();
            }
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
    @SuppressWarnings("unchecked")
    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        Object[] snapshot = toArray();
        for (Object item : snapshot) {
            action.accept((E) item);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof List)) {
            return false;
        }
        List<?> other = (List<?>) obj;
        // Snapshot this list, then compare WITHOUT holding our lock. Iterating a foreign
        // collection (its iterator/equals are arbitrary code) while holding our read lock
        // can deadlock: two ConcurrentLists comparing against each other while writers are
        // queued on both blocks each reader behind the other's queued writer, forming a cycle.
        Object[] mine = toArray();
        Iterator<?> it2 = other.iterator();
        for (Object e1 : mine) {
            if (!it2.hasNext()) {
                return false;
            }
            if (!Objects.equals(e1, it2.next())) {
                return false;
            }
        }
        return !it2.hasNext();
    }

    @Override
    public int hashCode() {
        // Per List.hashCode() contract: hash = 1; for each e: hash = 31*hash + (e==null ? 0 : e.hashCode())
        // Iterate directly under read lock to avoid O(n) snapshot allocation.
        lock.readLock().lock();
        try {
            int hash = 1;
            int sz = size();
            long pos = head.get();
            int i = 0;
            while (i < sz) {
                AtomicReferenceArray<Object> bucket = getBucket(bucketIndex(pos));
                int offset = bucketOffset(pos);
                int n = Math.min(BUCKET_SIZE - offset, sz - i);
                for (int k = 0; k < n; k++) {
                    Object e = bucket.get(offset + k);
                    hash = 31 * hash + (e == null ? 0 : e.hashCode());
                }
                i += n;
                pos += n;
            }
            return hash;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        // Iterate directly under read lock to avoid O(n) snapshot allocation.
        lock.readLock().lock();
        try {
            return CycleSafeToString.render(this, path -> {
                int sz = size();
                long h = head.get();
                StringBuilder sb = new StringBuilder();
                sb.append('[');
                for (int i = 0; i < sz; i++) {
                    if (i > 0) {
                        sb.append(',').append(' ');
                    }
                    long pos = h + i;
                    Object e = getBucket(bucketIndex(pos)).get(bucketOffset(pos));
                    if (e == this) {
                        sb.append(CycleSafeToString.THIS_COLLECTION);
                    } else {
                        CycleSafeToString.append(sb, e, path);
                    }
                }
                sb.append(']');
                return sb.toString();
            });
        } finally {
            lock.readLock().unlock();
        }
    }

    private void rebuild(List<E> elements) {
        buckets.clear();
        head.set(0);
        tail.set(0);
        sizeCounter.set(0);
        appendNoLock(elements.toArray());
    }

    /**
     * Under the write lock: appends the elements, one bucket lookup per bucket; tail and size move once, after
     * the elements are stored (size() and isEmpty() read them without the lock).
     */
    private void appendNoLock(Object[] elements) {
        long pos = tail.get();
        int done = 0;
        while (done < elements.length) {
            AtomicReferenceArray<Object> bucket = ensureBucket(bucketIndex(pos));
            int offset = bucketOffset(pos);
            int n = Math.min(BUCKET_SIZE - offset, elements.length - done);
            for (int k = 0; k < n; k++) {
                bucket.set(offset + k, elements[done + k]);   // set() for immediate visibility
            }
            done += n;
            pos += n;
        }
        tail.set(pos);
        sizeCounter.addAndGet(elements.length);
    }

    @SuppressWarnings("unchecked")
    private List<E> snapshotToListNoLock(int size) {
        Object[] elements = new Object[size];
        copyRange(0, size, elements, 0);
        return new ArrayList<>((List<E>) Arrays.asList(elements));
    }
}
