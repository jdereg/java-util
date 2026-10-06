package com.cedarsoftware.util.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

import com.cedarsoftware.util.ConcurrentHashMapNullSafe;
import com.cedarsoftware.util.MapUtilities;
import com.cedarsoftware.util.internal.KeyedLoads;

/**
 * This class provides a thread-safe Least Recently Used (LRU) cache API that evicts the least recently used items
 * once a threshold is met. It implements the <code>Map</code> interface for convenience.
 * <p>
 * The Locking strategy allows for O(1) access for get(), put(), and remove(). For put(), remove(), and many other
 * methods, a lock is obtained. get() takes no lock: it sets the entry's "used" flag. Eviction is CLOCK (second chance):
 * from the oldest entry, a flagged entry has its flag cleared and is passed over once (moved to the front), and the
 * first unflagged entry is evicted. That approximates LRU closely: simulated over skewed, uniform, shifting and
 * scanning request patterns, its hit rate was never lower than exact LRU's, and on skewed traffic its victims were
 * among the least recently used tenth of the cache. Reads no longer contend on the lock, and none is lost under load.
 * <p>
 * LRUCache supports <code>null</code> for both key and value.
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
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
public class LockingLRUCacheStrategy<K, V> implements Map<K, V> {
    private final int capacity;
    private final ConcurrentHashMapNullSafe<Object, Node<K, V>> cache;
    private final Node<K, V> head;
    private final Node<K, V> tail;
    private final Lock lock = new ReentrantLock();
    private final KeyedLoads loads = new KeyedLoads();
    private final KeyedLoads.Load<K, V> loadAndWrite = this::loadAndWrite;      // one, not a lambda per call

    private static class Node<K, V> {
        final K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;
        volatile boolean used;          // set by a read, without the lock; cleared by eviction's second chance

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Constructs a new LRU cache with the specified maximum capacity.
     *
     * @param capacity the maximum number of entries the cache can hold
     * @throws IllegalArgumentException if capacity is less than 1
     */
    public LockingLRUCacheStrategy(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1.");
        }
        this.capacity = capacity;
        this.cache = new ConcurrentHashMapNullSafe<>(capacity);
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

    /**
     * Moves the specified node to the head of the doubly linked list.
     * This operation must be performed under a lock.
     *
     * @param node the node to be moved to the head
     */
    private void moveToHead(Node<K, V> node) {
        if (node.prev == null || node.next == null) {
            // Node has been evicted; skip reordering
            return;
        }
        // Unlink from current position (don't null links since we're re-adding)
        node.prev.next = node.next;
        node.next.prev = node.prev;
        // Re-add at head
        addToHead(node);
    }

    /**
     * Adds a node to the head of the doubly linked list.
     * This operation must be performed under a lock.
     *
     * @param node the node to be added to the head
     */
    private void addToHead(Node<K, V> node) {
        node.next = head.next;
        node.next.prev = node;
        head.next = node;
        node.prev = head;
    }

    /**
     * Removes a node from the doubly linked list and nulls out its links.
     * This operation must be performed under a lock.
     *
     * @param node the node to be removed
     */
    private void removeNode(Node<K, V> node) {
        if (node.prev != null && node.next != null) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
            node.prev = null;  // Null out links to avoid GC nepotism
            node.next = null;
        }
    }

    /**
     * Removes and returns the node to evict, by CLOCK (second chance): from the tail, a node read since it was last
     * passed over has its flag cleared and moves to the head; the first node not read is removed. Ends after at most
     * one pass over every node. This operation must be performed under a lock.
     * Note: The caller should null out node.value after using it to help GC.
     *
     * @return the removed node, or head if the list is empty
     */
    private Node<K, V> removeTail() {
        Node<K, V> node = tail.prev;
        while (node != head && node.used) {
            node.used = false;
            moveToHead(node);           // a second chance
            node = tail.prev;
        }
        if (node != head) {
            removeNode(node);  // This nulls out prev/next links
        }
        return node;
    }

    /**
     * @return the maximum number of entries in the cache.
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Returns the value associated with the specified key in this cache, and marks the entry used, taking no lock:
     * eviction passes over a used entry once (CLOCK).
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key, or null if no mapping exists
     */
    @Override
    public V get(Object key) {
        Node<K, V> node = cache.get(key);
        if (node == null) {
            return null;
        }
        touch(node);
        return node.value;
    }

    /**
     * Associates the specified value with the specified key in this cache.
     * If the cache previously contained a mapping for the key, the old value
     * is replaced and moved to the front of the LRU list. If the cache is at
     * capacity, removes the least recently used item before adding the new item.
     *
     * @param key the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the previous value associated with key, or null if there was no mapping
     */
    public V put(K key, V value) {
        lock.lock();
        try {
            Node<K, V> node = cache.get(key);
            if (node != null) {
                V oldValue = node.value;
                node.value = value;
                moveToHead(node);
                return oldValue;
            } else {
                Node<K, V> newNode = new Node<>(key, value);
                cache.put(key, newNode);
                addToHead(newNode);
                if (cache.size() > capacity) {
                    Node<K, V> tailToRemove = removeTail();
                    cache.remove(tailToRemove.key);
                    tailToRemove.value = null;  // Help GC by releasing value reference
                }
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Copies all mappings from the specified map to this cache.
     * These operations will be performed atomically under a single lock.
     *
     * @param m mappings to be stored in this cache
     * @throws NullPointerException if the specified map is null
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        lock.lock();
        try {
            for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes the mapping for the specified key from this cache if present.
     *
     * @param key key whose mapping is to be removed from the cache
     * @return the previous value associated with key, or null if there was no mapping
     */
    @Override
    public V remove(Object key) {
        lock.lock();
        try {
            Node<K, V> node = cache.remove(key);
            if (node != null) {
                V value = node.value;  // Get value before cleanup
                removeNode(node);
                node.value = null;     // Help GC by releasing value reference
                return value;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Replaces each value with the function's result, under the lock (the function runs under it, as in
     * computeIfAbsent). The recency order is left as it is, as LinkedHashMap's replaceAll leaves it.
     */
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        lock.lock();
        try {
            for (Node<K, V> node = head.next; node != tail; node = node.next) {
                node.value = function.apply(node.key, node.value);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * The key's value, computed by mappingFunction and cached if absent (or null-valued), as Map.computeIfAbsent.
     * <p>
     * Loads single-flight per key, outside the cache's lock: the function runs once for a key however many threads
     * ask for it at once (the others wait for its value, or its exception, which nothing caches); loads of different
     * keys run in parallel; and no other operation on the cache waits for a function. A value written for the key
     * while the function runs is kept and returned. A function that asks for its own key again throws
     * IllegalStateException, as ConcurrentHashMap does, and so do functions on different threads that need each
     * other's keys (k1's needs k2 while k2's needs k1): where ConcurrentHashMap would hang, the loop is detected.
     */
    @Override
    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        Node<K, V> node = cache.get(key);
        if (node != null) {
            V value = node.value;
            if (value != null) {
                touch(node);
                return value;
            }
        }
        // Single-flight per key, outside the cache-wide lock: loads of different keys run in parallel, and nothing
        // else waits for a loader. The lock is taken only to write the result.
        return loads.load(key, mappingFunction, loadAndWrite);
    }

    private V loadAndWrite(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        Node<K, V> node = cache.get(key);
        if (node != null && node.value != null) {
            return node.value;                      // loaded by a load that just ended
        }
        V computed = mappingFunction.apply(key);
        lock.lock();
        try {
            Node<K, V> now = cache.get(key);
            if (now != null && now.value != null) {
                moveToHead(now);
                return now.value;                   // written while the function ran: that value stays
            }
            if (computed != null) {
                writeLocked(key, now, computed);
            } else if (now != null) {
                moveToHead(now);
            }
            return computed;
        } finally {
            lock.unlock();
        }
    }

    /** A read's use: the flag eviction reads, set without the lock (and written only when it changes). */
    private static void touch(Node<?, ?> node) {
        if (!node.used) {
            node.used = true;
        }
    }

    @Override
    public V putIfAbsent(K key, V value) {
        lock.lock();
        try {
            Node<K, V> node = cache.get(key);
            if (node != null) {
                if (node.value == null) {
                    node.value = value;
                    moveToHead(node);
                    return null;
                }
                moveToHead(node);
                return node.value;
            }

            Node<K, V> newNode = new Node<>(key, value);
            cache.put(key, newNode);
            addToHead(newNode);
            if (cache.size() > capacity) {
                Node<K, V> tailToRemove = removeTail();
                cache.remove(tailToRemove.key);
                tailToRemove.value = null;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    // The compound operations below run under the lock, so each is atomic (the Map defaults were a get, then a
    // put: concurrent updates overwrote each other). The functions run under the lock, as in computeIfAbsent.

    /**
     * Under the lock: writes value for key, at the head of the recency order, evicting past capacity. node is the
     * one read before the caller's function ran; a function that wrote the key itself (against the Map contract;
     * the lock is reentrant) may have unlinked it or added another, so the key keeps one node either way.
     */
    private void writeLocked(K key, Node<K, V> node, V value) {
        if (node != null && node.prev == null) {
            node = cache.get(key);                  // unlinked meanwhile: the key's node now, if any
        }
        if (node != null) {
            node.value = value;
            moveToHead(node);
            return;
        }
        Node<K, V> newNode = new Node<>(key, value);
        Node<K, V> displaced = cache.put(key, newNode);
        if (displaced != null) {
            removeNode(displaced);                  // added meanwhile: replaced, not kept as a second node
        }
        addToHead(newNode);
        if (cache.size() > capacity) {
            Node<K, V> tailToRemove = removeTail();
            cache.remove(tailToRemove.key);
            tailToRemove.value = null;
        }
    }

    /** Under the lock: removes key's node, if it is still the key's node. */
    private void removeLocked(Object key, Node<K, V> node) {
        if (cache.remove(key, node)) {              // Node has no equals: identity
            removeNode(node);
            node.value = null;
        }
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        lock.lock();
        try {
            Node<K, V> node = cache.get(key);
            V value = remappingFunction.apply(key, node == null ? null : node.value);
            if (value == null) {
                if (node != null) {
                    removeLocked(key, node);
                }
                return null;
            }
            writeLocked(key, node, value);
            return value;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        lock.lock();
        try {
            Node<K, V> node = cache.get(key);
            if (node == null || node.value == null) {
                return null;
            }
            V value = remappingFunction.apply(key, node.value);
            if (value == null) {
                removeLocked(key, node);
                return null;
            }
            writeLocked(key, node, value);
            return value;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(remappingFunction);
        lock.lock();
        try {
            Node<K, V> node = cache.get(key);
            V old = node == null ? null : node.value;
            V merged = old == null ? value : remappingFunction.apply(old, value);
            if (merged == null) {
                removeLocked(key, node);            // merged is null only when there was a value to merge with
                return null;
            }
            writeLocked(key, node, merged);
            return merged;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public V replace(K key, V value) {
        lock.lock();
        try {
            Node<K, V> node = cache.get(key);
            if (node == null) {
                return null;
            }
            V old = node.value;
            writeLocked(key, node, value);
            return old;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        lock.lock();
        try {
            Node<K, V> node = cache.get(key);
            if (node == null || !Objects.equals(node.value, oldValue)) {
                return false;
            }
            writeLocked(key, node, newValue);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(Object key, Object value) {
        lock.lock();
        try {
            Node<K, V> node = cache.get(key);
            if (node == null || !Objects.equals(node.value, value)) {
                return false;
            }
            removeLocked(key, node);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Removes all mappings from this cache.
     * The cache will be empty after this call returns.
     */
    @Override
    public void clear() {
        lock.lock();
        try {
            // Null out all nodes' links so concurrent get()'s tryLock/moveToHead
            // detects them as evicted via the null-link check.
            Node<K, V> node = head.next;
            while (node != tail) {
                Node<K, V> next = node.next;
                node.prev = null;
                node.next = null;
                node.value = null;
                node = next;
            }
            head.next = tail;
            tail.prev = head;
            cache.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the number of key-value mappings in this cache.
     *
     * @return the number of key-value mappings in this cache
     */
    @Override
    public int size() {
        return cache.size();
    }

    /**
     * Returns true if this cache contains no key-value mappings.
     *
     * @return true if this cache contains no key-value mappings
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns true if this cache contains a mapping for the specified key.
     *
     * @param key key whose presence in this cache is to be tested
     * @return true if this cache contains a mapping for the specified key
     */
    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    /**
     * Returns true if this cache maps one or more keys to the specified value.
     * This operation requires a full traversal of the cache under a lock.
     *
     * @param value value whose presence in this cache is to be tested
     * @return true if this cache maps one or more keys to the specified value
     */
    @Override
    public boolean containsValue(Object value) {
        lock.lock();
        try {
            for (Node<K, V> node = head.next; node != tail; node = node.next) {
                if (Objects.equals(node.value, value)) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a Set view of the mappings contained in this cache.
     * <p>
     * The returned set is an <em>unmodifiable snapshot</em> of the cache contents
     * at the time of the call. The snapshot preserves LRU ordering via a temporary
     * {@link LinkedHashMap}.
     * This operation requires a full traversal under a lock.
     *
     * @return a snapshot set of the mappings contained in this cache
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        lock.lock();
        try {
            Map<K, V> map = new LinkedHashMap<>();
            for (Node<K, V> node = head.next; node != tail; node = node.next) {
                map.put(node.key, node.value);
            }
            return Collections.unmodifiableMap(map).entrySet();     // read-only entries: setValue throws
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a Set view of the keys contained in this cache.
     * <p>
     * Like {@link #entrySet()}, this method returns an unmodifiable snapshot.
     * The set is independent of the cache and retains the current LRU ordering.
     * This operation requires a full traversal under a lock.
     *
     * @return a snapshot set of the keys contained in this cache
     */
    @Override
    public Set<K> keySet() {
        lock.lock();
        try {
            Map<K, V> map = new LinkedHashMap<>();
            for (Node<K, V> node = head.next; node != tail; node = node.next) {
                map.put(node.key, node.value);
            }
            return Collections.unmodifiableSet(map.keySet());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a Collection view of the values contained in this cache.
     * <p>
     * The collection is an unmodifiable snapshot with values ordered from most
     * to least recently used.
     * This operation requires a full traversal under a lock.
     *
     * @return a snapshot collection of the values contained in this cache
     */
    @Override
    public Collection<V> values() {
        lock.lock();
        try {
            Map<K, V> map = new LinkedHashMap<>();
            for (Node<K, V> node = head.next; node != tail; node = node.next) {
                map.put(node.key, node.value);
            }
            return Collections.unmodifiableCollection(map.values());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Compares the specified object with this cache for equality.
     * Returns true if the given object is also a map and the two maps
     * represent the same mappings.
     *
     * @param o object to be compared for equality with this cache
     * @return true if the specified object is equal to this cache
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Map)) return false;
        Map<?, ?> other = (Map<?, ?>) o;
        return entrySet().equals(other.entrySet());
    }

    /**
     * Returns a string representation of this cache.
     * The string representation consists of a list of key-value mappings
     * in LRU order (most recently used first) enclosed in braces ("{}").
     * Adjacent mappings are separated by the characters ", ".
     *
     * @return a string representation of this cache
     */
    @Override
    public String toString() {
        // entrySet() snapshots under the lock, so the values' own toString() runs after the lock is released.
        return MapUtilities.mapToString(this);
    }

    /**
     * Returns the hash code value for this cache.
     * The hash code is computed by iterating over all entries in LRU order
     * and combining their hash codes.
     *
     * @return the hash code value for this cache
     */
    @Override
    public int hashCode() {
        lock.lock();
        try {
            int hashCode = 0;
            for (Node<K, V> node = head.next; node != tail; node = node.next) {
                hashCode += Objects.hashCode(node.key) ^ Objects.hashCode(node.value);
            }
            return hashCode;
        } finally {
            lock.unlock();
        }
    }
}
