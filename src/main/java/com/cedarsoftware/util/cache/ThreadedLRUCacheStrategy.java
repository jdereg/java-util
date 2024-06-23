package com.cedarsoftware.util.cache;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class provides a thread-safe Least Recently Used (LRU) cache API that will evict the least recently used items,
 * once a threshold is met. It implements the <code>Map</code> interface for convenience.
 * <p>
 * The Threaded strategy allows for O(1) access for get(), put(), and remove() without blocking. It uses a <code>ConcurrentHashMap</code>
 * internally. To ensure that the capacity is honored, whenever put() is called, a thread (from a thread pool) is tasked
 * with cleaning up items above the capacity threshold. This means that the cache may temporarily exceed its capacity, but
 * it will soon be trimmed back to the capacity limit by the scheduled thread.
 * <p>
 * LRUCache supports <code>null</code> for both key or value.
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
public class ThreadedLRUCacheStrategy<K, V> implements Map<K, V> {
    private static final Object NULL_ITEM = new Object(); // Sentinel value for null keys and values
    private final long cleanupDelayMillis;
    private final int capacity;
    private final ConcurrentMap<Object, Node<K>> cache;
    private final AtomicBoolean cleanupScheduled = new AtomicBoolean(false);
    private final ScheduledExecutorService scheduler;
    private final boolean isDefaultScheduler;

    private static class Node<K> {
        final K key;
        volatile Object value;
        volatile long timestamp;

        Node(K key, Object value) {
            this.key = key;
            this.value = value;
            this.timestamp = System.nanoTime();
        }

        void updateTimestamp() {
            this.timestamp = System.nanoTime();
        }
    }

    /**
     * Create a LRUCache with the maximum capacity of 'capacity.' Note, the LRUCache could temporarily exceed the
     * capacity; however, it will quickly reduce to that amount. This time is configurable via the cleanupDelay
     * parameter and custom scheduler and executor services.
     * 
     * @param capacity           int maximum size for the LRU cache.
     * @param cleanupDelayMillis int milliseconds before scheduling a cleanup (reduction to capacity if the cache currently
     *                           exceeds it).
     * @param scheduler          ScheduledExecutorService for scheduling cleanup tasks. Can be null. If none is supplied,
     *                           a default scheduler is created for you. Calling the .shutdown() method will shutdown
     *                           the schedule only if you passed in null (using default). If you pass one in, it is
     *                           your responsibility to terminate the scheduler.
     */
    public ThreadedLRUCacheStrategy(int capacity, int cleanupDelayMillis, ScheduledExecutorService scheduler) {
        if (scheduler == null) {
            this.scheduler = Executors.newScheduledThreadPool(1);
            isDefaultScheduler = true;
        } else {
            this.scheduler = scheduler;
            isDefaultScheduler = false;
        }
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>(capacity);
        this.cleanupDelayMillis = cleanupDelayMillis;
    }

    @SuppressWarnings("unchecked")
    private void cleanup() {
        int size = cache.size();
        if (size > capacity) {
            Node<K>[] nodes = cache.values().toArray(new Node[0]);
            Arrays.sort(nodes, Comparator.comparingLong(node -> node.timestamp));
            int nodesToRemove = size - capacity;
            for (int i = 0; i < nodesToRemove; i++) {
                Node<K> node = nodes[i];
                cache.remove(toCacheItem(node.key), node);
            }
        }
        cleanupScheduled.set(false); // Reset the flag after cleanup
        
        // Check if another cleanup is needed after the current one
        if (cache.size() > capacity) {
            scheduleCleanup();
        }
    }

    @Override
    public V get(Object key) {
        Object cacheKey = toCacheItem(key);
        Node<K> node = cache.get(cacheKey);
        if (node != null) {
            node.updateTimestamp();
            return fromCacheItem(node.value);
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        Object cacheKey = toCacheItem(key);
        Object cacheValue = toCacheItem(value);
        Node<K> newNode = new Node<>(key, cacheValue);
        Node<K> oldNode = cache.put(cacheKey, newNode);
        if (oldNode != null) {
            newNode.updateTimestamp();
            return fromCacheItem(oldNode.value);
        } else if (size() > capacity) {
            scheduleCleanup();
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @Override
    public V remove(Object key) {
        Object cacheKey = toCacheItem(key);
        Node<K> node = cache.remove(cacheKey);
        if (node != null) {
            return fromCacheItem(node.value);
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
        return cache.containsKey(toCacheItem(key));
    }

    @Override
    public boolean containsValue(Object value) {
        Object cacheValue = toCacheItem(value);
        for (Node<K> node : cache.values()) {
            if (node.value.equals(cacheValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet = Collections.newSetFromMap(new ConcurrentHashMap<>());
        for (Node<K> node : cache.values()) {
            entrySet.add(new AbstractMap.SimpleEntry<>(fromCacheItem(node.key), fromCacheItem(node.value)));
        }
        return entrySet;
    }

    @Override
    public Set<K> keySet() {
        Set<K> keySet = Collections.newSetFromMap(new ConcurrentHashMap<>());
        for (Node<K> node : cache.values()) {
            keySet.add(fromCacheItem(node.key));
        }
        return Collections.unmodifiableSet(keySet);
    }

    @Override
    public Collection<V> values() {
        Collection<V> values = new ArrayList<>();
        for (Node<K> node : cache.values()) {
            values.add(fromCacheItem(node.value));
        }
        return Collections.unmodifiableCollection(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Map)) return false;
        Map<?, ?> other = (Map<?, ?>) o;
        return entrySet().equals(other.entrySet());
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (Node<K> node : cache.values()) {
            Object key = fromCacheItem(node.key);
            Object value = fromCacheItem(node.value);
            hashCode = 31 * hashCode + (key == null ? 0 : key.hashCode());
            hashCode = 31 * hashCode + (value == null ? 0 : value.hashCode());
        }
        return hashCode;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Node<K> node : cache.values()) {
            sb.append((K) fromCacheItem(node.key)).append("=").append((V) fromCacheItem(node.value)).append(", ");
        }
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 2); // Remove trailing comma and space
        }
        sb.append("}");
        return sb.toString();
    }

    // Schedule a delayed cleanup
    private void scheduleCleanup() {
        if (cleanupScheduled.compareAndSet(false, true)) {
            scheduler.schedule(this::cleanup, cleanupDelayMillis, TimeUnit.MILLISECONDS);
        }
    }

    // Converts a key or value to a cache-compatible item
    private Object toCacheItem(Object item) {
        return item == null ? NULL_ITEM : item;
    }

    // Converts a cache-compatible item to the original key or value
    @SuppressWarnings("unchecked")
    private <T> T fromCacheItem(Object cacheItem) {
        return cacheItem == NULL_ITEM ? null : (T) cacheItem;
    }

    /**
     * Shut down the scheduler if it is the default one.
     */
    public void shutdown() {
        if (isDefaultScheduler) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }
}