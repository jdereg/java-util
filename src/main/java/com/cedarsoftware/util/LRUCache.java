package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * This class provides a thread-safe Least Recently Used (LRU) cache API that will evict the least recently used items,
 * once a threshold is met.  It implements the Map interface for convenience. It is thread-safe via usage of
 * ReentrantReadWriteLock() around read and write APIs, including delegating to keySet(), entrySet(), and
 * values() and each of their iterators.
 * <p>
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
public class LRUCache<K, V> implements Map<K, V> {
    private final Map<K, V> cache;
    private final transient ReadWriteLock lock = new ReentrantReadWriteLock();
    private final static Object NO_ENTRY = new Object();

    public LRUCache(int capacity) {
        cache = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    // Immutable APIs
    public boolean equals(Object obj) { return readOperation(() -> cache.equals(obj)); }
    public int hashCode() { return readOperation(cache::hashCode); }
    public String toString() { return readOperation(cache::toString); }
    public int size() { return readOperation(cache::size); }
    public boolean isEmpty() { return readOperation(cache::isEmpty); }
    public boolean containsKey(Object key) { return readOperation(() -> cache.containsKey(key)); }
    public boolean containsValue(Object value) { return readOperation(() -> cache.containsValue(value)); }
    public V get(Object key) { return readOperation(() -> cache.get(key)); }

    // Mutable APIs
    public V put(K key, V value) { return writeOperation(() -> cache.put(key, value)); }
    public void putAll(Map<? extends K, ? extends V> m) { writeOperation(() -> { cache.putAll(m); return null; }); }
    public V putIfAbsent(K key, V value) { return writeOperation(() -> cache.putIfAbsent(key, value)); }
    public V remove(Object key) { return writeOperation(() -> cache.remove(key)); }
    public void clear() { writeOperation(() -> { cache.clear(); return null; }); }

    public Set<K> keySet() {
        return readOperation(() -> new Set<K>() {
            public int size() { return readOperation(cache::size); }
            public boolean isEmpty() { return readOperation(cache::isEmpty); }
            public boolean contains(Object o) { return readOperation(() -> cache.containsKey(o)); }
            public Iterator<K> iterator() {
                return new Iterator<K>() {
                    private final Iterator<K> it = cache.keySet().iterator();
                    private K current = (K)NO_ENTRY;

                    public boolean hasNext() { return readOperation(it::hasNext); }
                    public K next() { return readOperation(() -> { current = it.next(); return current; }); }
                    public void remove() {
                        writeOperation(() -> {
                            if (current == NO_ENTRY) {
                                throw new IllegalStateException("Next not called or already removed");
                            }
                            it.remove(); // Remove from the underlying map
                            current = (K)NO_ENTRY;
                            return null;
                        });
                    }
                };
            }
            public Object[] toArray() { return readOperation(() -> cache.keySet().toArray()); }
            public <T> T[] toArray(T[] a) { return readOperation(() -> cache.keySet().toArray(a)); }
            public boolean add(K k) { throw new UnsupportedOperationException("add() not supported on .keySet() of a Map"); }
            public boolean remove(Object o) { return writeOperation(() -> cache.remove(o) != null); }
            public boolean containsAll(Collection<?> c) { return readOperation(() -> cache.keySet().containsAll(c)); }
            public boolean addAll(Collection<? extends K> c) { throw new UnsupportedOperationException("addAll() not supported on .keySet() of a Map"); }
            public boolean retainAll(Collection<?> c) { return writeOperation(() -> cache.keySet().retainAll(c)); }
            public boolean removeAll(Collection<?> c) { return writeOperation(() -> cache.keySet().removeAll(c)); }
            public void clear() { writeOperation(() -> { cache.clear(); return null; }); }
        });
    }

    public Collection<V> values() {
        return readOperation(() -> new Collection<V>() {
            public int size() { return readOperation(cache::size); }
            public boolean isEmpty() { return readOperation(cache::isEmpty); }
            public boolean contains(Object o) { return readOperation(() -> cache.containsValue(o)); }
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    private final Iterator<V> it = cache.values().iterator();
                    private V current = (V)NO_ENTRY;

                    public boolean hasNext() { return readOperation(it::hasNext); }
                    public V next() { return readOperation(() -> { current = it.next(); return current; }); }
                    public void remove() {
                        writeOperation(() -> {
                            if (current == NO_ENTRY) {
                                throw new IllegalStateException("Next not called or already removed");
                            }
                            it.remove(); // Remove from the underlying map
                            current = (V)NO_ENTRY;
                            return null;
                        });
                    }
                };
            }
            public Object[] toArray() { return readOperation(() -> cache.values().toArray()); }
            public <T> T[] toArray(T[] a) { return readOperation(() -> cache.values().toArray(a)); }
            public boolean add(V value) { throw new UnsupportedOperationException("add() not supported on values() of a Map"); }
            public boolean remove(Object o) { return writeOperation(() -> cache.values().remove(o)); }
            public boolean containsAll(Collection<?> c) { return readOperation(() -> cache.values().containsAll(c)); }
            public boolean addAll(Collection<? extends V> c) { throw new UnsupportedOperationException("addAll() not supported on values() of a Map"); }
            public boolean removeAll(Collection<?> c) { return writeOperation(() -> cache.values().removeAll(c)); }
            public boolean retainAll(Collection<?> c) { return writeOperation(() -> cache.values().retainAll(c)); }
            public void clear() { writeOperation(() -> { cache.clear(); return null; }); }
        });
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return readOperation(() -> new Set<Entry<K, V>>() {
            public int size() { return readOperation(cache::size); }
            public boolean isEmpty() { return readOperation(cache::isEmpty); }
            public boolean contains(Object o) { return readOperation(() -> cache.entrySet().contains(o)); }
            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<Entry<K, V>>() {
                    private final Iterator<Entry<K, V>> it = cache.entrySet().iterator();
                    private Entry<K, V> current = (Entry<K, V>) NO_ENTRY;

                    public boolean hasNext() { return readOperation(it::hasNext); }
                    public Entry<K, V> next() { return readOperation(() -> { current = it.next(); return current; }); }
                    public void remove() {
                        writeOperation(() -> {
                            if (current == NO_ENTRY) {
                                throw new IllegalStateException("Next not called or already removed");
                            }
                            it.remove();
                            current = (Entry<K, V>) NO_ENTRY;
                            return null;
                        });
                    }
                };
            }

            public Object[] toArray() { return readOperation(() -> cache.entrySet().toArray()); }
            public <T> T[] toArray(T[] a) { return readOperation(() -> cache.entrySet().toArray(a)); }
            public boolean add(Entry<K, V> kvEntry) { throw new UnsupportedOperationException("add() not supported on entrySet() of a Map"); }
            public boolean remove(Object o) { return writeOperation(() -> cache.entrySet().remove(o)); }
            public boolean containsAll(Collection<?> c) { return readOperation(() -> cache.entrySet().containsAll(c)); }
            public boolean addAll(Collection<? extends Entry<K, V>> c) { throw new UnsupportedOperationException("addAll() not supported on entrySet() of a Map"); }
            public boolean retainAll(Collection<?> c) { return writeOperation(() -> cache.entrySet().retainAll(c)); }
            public boolean removeAll(Collection<?> c) { return writeOperation(() -> cache.entrySet().removeAll(c)); }
            public void clear() { writeOperation(() -> { cache.clear(); return null; }); }
        });
    }
    
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
