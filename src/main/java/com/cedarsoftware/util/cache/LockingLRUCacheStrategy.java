package com.cedarsoftware.util.cache;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
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

    public LockingLRUCacheStrategy(int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMapNullSafe<>(capacity);
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

    private void moveToHead(Node<K, V> node) {
        if (node.prev == null || node.next == null) {
            // Node has been evicted; skip reordering
            return;
        }
        removeNode(node);
        addToHead(node);
    }

    private void addToHead(Node<K, V> node) {
        node.next = head.next;
        node.next.prev = node;
        head.next = node;
        node.prev = head;
    }

    private void removeNode(Node<K, V> node) {
        if (node.prev != null && node.next != null) {
            node.prev.next = node.next;
            node.next.prev = node.prev;
        }
    }
    
    private Node<K, V> removeTail() {
        Node<K, V> node = tail.prev;
        if (node != head) {
            removeNode(node);
            node.prev = null; // Null out links to avoid GC nepotism
            node.next = null; // Null out links to avoid GC nepotism
        }
        return node;
    }

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

    @Override
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
                    Node<K, V> tail = removeTail();
                    cache.remove(tail.key);
                }
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
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

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }
    
    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        lock.lock();
        try {
            for (Node<K, V> node = head.next; node != tail; node = node.next) {
                if (node.value.equals(value)) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Map)) return false;
        Map<?, ?> other = (Map<?, ?>) o;
        return entrySet().equals(other.entrySet());
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            for (Node<K, V> node = head.next; node != tail; node = node.next) {
                sb.append(node.key).append("=").append(node.value).append(", ");
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