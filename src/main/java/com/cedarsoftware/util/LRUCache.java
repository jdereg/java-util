package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
public class LRUCache<K, V> extends AbstractMap<K, V> implements Map<K, V> {
    private final Map<K, Node> map;
    private final Node head;
    private final Node tail;
    private final int capacity;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private class Node {
        K key;
        V value;
        Node prev;
        Node next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(key, node.key) && Objects.equals(value, node.value);
        }

        public int hashCode() {
            return Objects.hash(key, value);
        }

        public String toString() {
            return "Node{" +
                    "key=" + key +
                    ", value=" + value +
                    '}';
        }
    }

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.map = new ConcurrentHashMap<>(capacity);
        this.head = new Node(null, null);
        this.tail = new Node(null, null);
        head.next = tail;
        tail.prev = head;
    }

    public V get(Object key) {
        lock.readLock().lock();
        try {
            Node node = map.get(key);
            if (node == null) {
                return null;
            }
            moveToHead(node);
            return node.value;
        } finally {
            lock.readLock().unlock();
        }
    }

    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            Node newNode = new Node(key, value);
            Node oldNode = map.put(key, newNode);

            if (oldNode != null) {
                removeNode(oldNode);
            }

            addToHead(newNode);

            if (map.size() > capacity) {
                Node oldestNode = removeTailNode();
                if (oldestNode != null) {
                    map.remove(oldestNode.key);
                }
            }

            return oldNode != null ? oldNode.value : null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public V remove(Object key) {
        lock.writeLock().lock();
        try {
            Node node = map.remove(key);
            if (node != null) {
                removeNode(node);
                return node.value;
            }
            return null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            map.clear();
            head.next = tail;
            tail.prev = head;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        return map.size();
    }

    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    public boolean containsValue(Object value) {
        for (Node node : map.values()) {
            if (Objects.equals(node.value, value)) {
                return true;
            }
        }
        return false;
    }

    public Set<Map.Entry<K, V>> entrySet() {
        Map<K, V> result = new LinkedHashMap<>();
        for (Node node : map.values()) {
            result.put(node.key, node.value);
        }
        return Collections.unmodifiableSet(result.entrySet());
    }

    public Set<K> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    public Collection<V> values() {
        Collection<V> values = new ArrayList<>();
        for (Node node : map.values()) {
            values.add(node.value);
        }
        return Collections.unmodifiableCollection(values);
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Map) {
            Map<?, ?> other = (Map<?, ?>) o;
            if (other.size() != this.size()) {
                return false;
            }
            for (Map.Entry<?, ?> entry : other.entrySet()) {
                V value = this.get(entry.getKey());
                if (!Objects.equals(value, entry.getValue())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        int hashCode = 1;
        for (Map.Entry<K, Node> entry : map.entrySet()) {
            hashCode = 31 * hashCode + (entry.getKey() == null ? 0 : entry.getKey().hashCode());
            hashCode = 31 * hashCode + (entry.getValue().value == null ? 0 : entry.getValue().value.hashCode());
        }
        return hashCode;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<K, Node> entry : map.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue().value).append(", ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }

    private void addToHead(Node node) {
        Node nextNode = head.next;
        node.next = nextNode;
        node.prev = head;
        head.next = node;
        nextNode.prev = node;
    }

    private void removeNode(Node node) {
        Node prevNode = node.prev;
        Node nextNode = node.next;
        prevNode.next = nextNode;
        nextNode.prev = prevNode;
    }

    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }

    private Node removeTailNode() {
        Node oldestNode = tail.prev;
        if (oldestNode == head) {
            return null;
        }
        removeNode(oldestNode);
        return oldestNode;
    }
}