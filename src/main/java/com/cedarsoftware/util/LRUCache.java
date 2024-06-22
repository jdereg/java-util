package com.cedarsoftware.util;

import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class provides a thread-safe Least Recently Used (LRU) cache API that will evict the least recently used items,
 * once a threshold is met.  It implements the Map interface for convenience. It is thread-safe via usage of
 * ConcurrentHashMap for internal storage.  The .get(), .remove(), and .put() APIs operate in O(1) without any
 * blocking.  A background thread monitors and cleans up the internal Map if it exceeds capacity.  In addition, if
 * .put() causes the background thread to be triggered to start immediately.  This will keep the size of the LRUCache
 * close to capacity even with bursty loads without reducing insertion (put) performance.
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
    private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private static final long DELAY = 10; // 1 second delay
    private final int capacity;
    private final ConcurrentHashMap<K, Node<K, V>> cache;
    private volatile boolean cleanupScheduled = false;

    private static class Node<K, V> {
        final K key;
        volatile V value;
        volatile long timestamp;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.timestamp = System.nanoTime();
        }

        void updateTimestamp() {
            this.timestamp = System.nanoTime();
        }
    }

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>(capacity);
    }

    private void dynamicCleanup() {
        int size = cache.size();
        if (size > capacity) {
            List<Node<K, V>> nodes = new ArrayList<>(cache.values());
            nodes.sort(Comparator.comparingLong(node -> node.timestamp));
            int nodesToRemove = size - capacity;
            for (int i = 0; i < nodesToRemove; i++) {
                Node<K, V> node = nodes.get(i);
                cache.remove(node.key, node);
            }
        }
        cleanupScheduled = false; // Reset the flag after cleanup
        // Check if another cleanup is needed after the current one
        if (cache.size() > capacity) {
            scheduleCleanup();
        }
    }

    @Override
    public V get(Object key) {
        Node<K, V> node = cache.get(key);
        if (node != null) {
            node.updateTimestamp();
            return node.value;
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        Node<K, V> newNode = new Node<>(key, value);
        Node<K, V> oldNode = cache.put(key, newNode);
        if (oldNode != null) {
            newNode.updateTimestamp();
            return oldNode.value;
        } else {
            scheduleCleanup();
            return null;
        }
    }

    @Override
    public V remove(Object key) {
        Node<K, V> node = cache.remove(key);
        if (node != null) {
            scheduleCleanup();
            return node.value;
        }
        return null;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        for (Node<K, V> node : cache.values()) {
            if (node.value.equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet = Collections.newSetFromMap(new ConcurrentHashMap<>());
        for (Node<K, V> node : cache.values()) {
            entrySet.add(new AbstractMap.SimpleEntry<>(node.key, node.value));
        }
        return entrySet;
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    @Override
    public Collection<V> values() {
        Collection<V> values = new ArrayList<>();
        for (Node<K, V> node : cache.values()) {
            values.add(node.value);
        }
        return Collections.unmodifiableCollection(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Map)) return false;
        Map<?, ?> other = (Map<?, ?>) o;
        return this.entrySet().equals(other.entrySet());
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (Node<K, V> node : cache.values()) {
            hashCode = 31 * hashCode + (node.key == null ? 0 : node.key.hashCode());
            hashCode = 31 * hashCode + (node.value == null ? 0 : node.value.hashCode());
        }
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Node<K, V> node : cache.values()) {
            sb.append(node.key).append("=").append(node.value).append(", ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2); // Remove trailing comma and space
        }
        sb.append("}");
        return sb.toString();
    }

    // Schedule a delayed cleanup
    private synchronized void scheduleCleanup() {
        if (cache.size() > capacity && !cleanupScheduled) {
            cleanupScheduled = true;
            executorService.schedule(this::dynamicCleanup, DELAY, TimeUnit.MILLISECONDS);
        }
    }
}