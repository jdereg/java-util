package com.cedarsoftware.util.cache;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.cedarsoftware.util.ConcurrentHashMapNullSafe;

/**
 * This class provides a thread-safe Least Recently Used (LRU) cache API that evicts the least recently used items
 * once a threshold is met. It implements the <code>Map</code> interface for convenience.
 * <p>
 * The Locking strategy allows for O(1) access for get(), put(), and remove(). For put(), remove(), and many other
 * methods, a write-lock is obtained. For get(), it attempts to lock but does not lock unless it can obtain it right away.
 * This 'try-lock' approach ensures that the get() API is never blocking, but it also means that the LRU order is not
 * perfectly maintained under heavy load.
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

    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }

    /**
     * Constructs a new LRU cache with the specified maximum capacity.
     *
     * @param capacity the maximum number of entries the cache can hold
     * @throws IllegalArgumentException if capacity is negative
     */
    public LockingLRUCacheStrategy(int capacity) {
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
        removeNode(node);
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
     * Removes a node from the doubly linked list.
     * This operation must be performed under a lock.
     *
     * @param node the node to be removed
     */
    private void removeNode(Node<K, V> node) {
        if (node.prev != null && node.next != null) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }
    }

    /**
     * Removes and returns the least recently used node from the tail of the list.
     * This operation must be performed under a lock.
     *
     * @return the removed node, or null if the list is empty
     */
    private Node<K, V> removeTail() {
        Node<K, V> node = tail.prev;
        if (node != head) {
            removeNode(node);
            node.prev = null; // Null out links to avoid GC nepotism
            node.next = null; // Null out links to avoid GC nepotism
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
     * Returns the value associated with the specified key in this cache.
     * If the key exists, attempts to move it to the front of the LRU list
     * using a non-blocking try-lock approach.
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

        // Ben Manes suggestion - use exclusive 'try-lock'
        if (lock.tryLock()) {
            try {
                moveToHead(node);
            } finally {
                lock.unlock();
            }
        }
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
                node.value = value;
                moveToHead(node);
                return node.value;
            } else {
                Node<K, V> newNode = new Node<>(key, value);
                cache.put(key, newNode);
                addToHead(newNode);
                if (cache.size() > capacity) {
                    Node<K, V> tailToRemove = removeTail();
                    cache.remove(tailToRemove.key);
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
                removeNode(node);
                return node.value;
            }
            return null;
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
     * The returned set is a <em>snapshot</em> of the cache contents at the time
     * of the call.  Modifying the set or its iterator does not affect the
     * underlying cache.  Iterator removal operates only on the snapshot.
     * The snapshot preserves LRU ordering via a temporary {@link LinkedHashMap}.
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
            return map.entrySet();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a Set view of the keys contained in this cache.
     * <p>
     * Like {@link #entrySet()}, this method returns a snapshot.  The set is
     * independent of the cache and retains the current LRU ordering.  Removing
     * elements from the returned set does not remove them from the cache.
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
            return map.keySet();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns a Collection view of the values contained in this cache.
     * <p>
     * The collection is a snapshot with values ordered from most to least
     * recently used.  Changes to the returned collection or its iterator do not
     * affect the cache.  Iterator removal only updates the snapshot.
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
            return map.values();
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
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            for (Node<K, V> node = head.next; node != tail; node = node.next) {
                sb.append(formatElement(node.key))
                        .append("=")
                        .append(formatElement(node.value))
                        .append(", ");
            }

            if (sb.length() > 1) {
                sb.setLength(sb.length() - 2); // Remove trailing comma and space
            }
            sb.append("}");
            return sb.toString();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Helper method to format an element, replacing self-references with a placeholder.
     *
     * @param element The element to format.
     * @return The string representation of the element, or a placeholder if it's a self-reference.
     */
    private String formatElement(Object element) {
        if (element == this) {
            return "(this Collection)";
        }
        return String.valueOf(element);
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
            int hashCode = 1;
            for (Node<K, V> node = head.next; node != tail; node = node.next) {
                Object key = node.key;
                Object value = node.value;
                hashCode = 31 * hashCode + (key == null ? 0 : key.hashCode());
                hashCode = 31 * hashCode + (value == null ? 0 : value.hashCode());
            }
            return hashCode;
        } finally {
            lock.unlock();
        }
    }
}
