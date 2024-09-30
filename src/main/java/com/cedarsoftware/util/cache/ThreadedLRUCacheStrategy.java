package com.cedarsoftware.util.cache;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.ref.WeakReference;

/**
 * This class provides a thread-safe Least Recently Used (LRU) cache API that evicts the least recently used items
 * once a threshold is met. It implements the <code>Map</code> interface for convenience.
 * <p>
 * The Threaded strategy allows for O(1) access for get(), put(), and remove() without blocking. It uses a <code>ConcurrentHashMap</code>
 * internally. To ensure that the capacity is honored, whenever put() is called, a scheduled cleanup task is triggered
 * to remove the least recently used items if the cache exceeds the capacity.
 * <p>
 * LRUCache supports <code>null</code> for both key and value.
 * <p>
 * <b>Note:</b> This implementation uses a shared scheduler for all cache instances to optimize resource usage.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 *
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

    // Shared ScheduledExecutorService for all cache instances
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Inner class representing a cache node with a key, value, and timestamp for LRU tracking.
     */
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
     * Inner class for the purging task.
     * Uses a WeakReference to avoid preventing garbage collection of cache instances.
     */
    private static class PurgeTask<K, V> implements Runnable {
        private final WeakReference<ThreadedLRUCacheStrategy<K, V>> cacheRef;

        PurgeTask(WeakReference<ThreadedLRUCacheStrategy<K, V>> cacheRef) {
            this.cacheRef = cacheRef;
        }

        @Override
        public void run() {
            ThreadedLRUCacheStrategy<K, V> cache = cacheRef.get();
            if (cache != null) {
                cache.cleanup();
            }
            // If cache is null, it has been garbage collected; no action needed
        }
    }

    /**
     * Create an LRUCache with the maximum capacity of 'capacity.'
     * The cleanup task is scheduled to run after 'cleanupDelayMillis' milliseconds.
     *
     * @param capacity           int maximum size for the LRU cache.
     * @param cleanupDelayMillis int milliseconds before scheduling a cleanup (reduction to capacity if the cache currently
     *                           exceeds it).
     */
    public ThreadedLRUCacheStrategy(int capacity, int cleanupDelayMillis) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1.");
        }
        if (cleanupDelayMillis < 10) {
            throw new IllegalArgumentException("cleanupDelayMillis must be at least 10 milliseconds.");
        }
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>(capacity);
        this.cleanupDelayMillis = cleanupDelayMillis;

        // Schedule the purging task for this cache
        schedulePurgeTask();
    }

    /**
     * Schedules the purging task for this cache using the shared scheduler.
     */
    private void schedulePurgeTask() {
        WeakReference<ThreadedLRUCacheStrategy<K, V>> cacheRef = new WeakReference<>(this);
        PurgeTask<K, V> purgeTask = new PurgeTask<>(cacheRef);
        scheduler.scheduleAtFixedRate(purgeTask, cleanupDelayMillis, cleanupDelayMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Cleanup method that removes least recently used entries to maintain the capacity.
     */
    private void cleanup() {
        int size = cache.size();
        if (size > capacity) {
            int nodesToRemove = size - capacity;
            Node<K>[] nodes = cache.values().toArray(new Node[0]);
            Arrays.sort(nodes, Comparator.comparingLong(node -> node.timestamp));
            for (int i = 0; i < nodesToRemove; i++) {
                Node<K> node = nodes[i];
                cache.remove(toCacheItem(node.key), node);
            }
            cleanupScheduled.set(false); // Reset the flag after cleanup

            // Check if another cleanup is needed after the current one
            if (cache.size() > capacity) {
                scheduleImmediateCleanup();
            }
        }
    }

    /**
     * Schedules an immediate cleanup if not already scheduled.
     */
    private void scheduleImmediateCleanup() {
        if (cleanupScheduled.compareAndSet(false, true)) {
            scheduler.schedule(this::cleanup, cleanupDelayMillis, TimeUnit.MILLISECONDS);
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
            scheduleImmediateCleanup();
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
            if (Objects.equals(node.value, cacheValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet = ConcurrentHashMap.newKeySet();
        for (Node<K> node : cache.values()) {
            entrySet.add(new AbstractMap.SimpleEntry<>(fromCacheItem(node.key), fromCacheItem(node.value)));
        }
        return Collections.unmodifiableSet(entrySet);
    }

    @Override
    public Set<K> keySet() {
        Set<K> keySet = ConcurrentHashMap.newKeySet();
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Iterator<Entry<K, V>> it = entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Converts a user-provided key or value to a cache item, handling nulls.
     *
     * @param item the key or value to convert
     * @return the cache item representation
     */
    private Object toCacheItem(Object item) {
        return item == null ? NULL_ITEM : item;
    }

    /**
     * Converts a cache item back to the user-provided key or value, handling nulls.
     *
     * @param <T>       the type of the returned item
     * @param cacheItem the cache item to convert
     * @return the original key or value
     */
    @SuppressWarnings("unchecked")
    private <T> T fromCacheItem(Object cacheItem) {
        return cacheItem == NULL_ITEM ? null : (T) cacheItem;
    }
    /**
     * Shuts down the shared scheduler. Call this method when your application is terminating.
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}