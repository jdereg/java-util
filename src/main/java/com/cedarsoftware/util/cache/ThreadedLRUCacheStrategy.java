package com.cedarsoftware.util.cache;

import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.cedarsoftware.util.ConcurrentHashMapNullSafe;
import com.cedarsoftware.util.ConcurrentSet;
import com.cedarsoftware.util.EncryptionUtilities;
import com.cedarsoftware.util.MapUtilities;

/**
 * This class provides a thread-safe Least Recently Used (LRU) cache API that evicts the least recently used items
 * once a threshold is met. It implements the <code>Map</code> interface for convenience.
 * <p>
 * The Threaded strategy allows for O(1) access for get(), put(), and remove() without blocking. It uses a <code>ConcurrentHashMapNullSafe</code>
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
    private final long cleanupDelayMillis;
    private final int capacity;
    private final ConcurrentMap<K, Node<K, V>> cache;
    private final AtomicBoolean cleanupScheduled = new AtomicBoolean(false);

    // Shared ScheduledExecutorService for all cache instances
    // set thread to daemon so application can shut down properly.
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "LRUCache-Purge-Thread");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Inner class representing a cache node with a key, value, and timestamp for LRU tracking.
     */
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
        this.cache = new ConcurrentHashMapNullSafe<>(capacity);
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
            Node<K, V>[] nodes = cache.values().toArray(new Node[0]);
            Arrays.sort(nodes, Comparator.comparingLong(node -> node.timestamp));
            for (int i = 0; i < nodesToRemove; i++) {
                Node<K, V> node = nodes[i];
                cache.remove(node.key, node);
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

    /**
     * @return the maximum number of entries in the cache.
     */
    public int getCapacity() {
        return capacity;
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
        Node<K, V> node = cache.remove(key);
        if (node != null) {
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
            if (Objects.equals(node.value, value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> entrySet = new ConcurrentSet<>();
        for (Node<K, V> node : cache.values()) {
            entrySet.add(new AbstractMap.SimpleEntry<>(node.key, node.value));
        }
        return Collections.unmodifiableSet(entrySet);
    }

    @Override
    public Set<K> keySet() {
        Set<K> keySet = new ConcurrentSet<>();
        for (Node<K, V> node : cache.values()) {
            keySet.add(node.key);
        }
        return Collections.unmodifiableSet(keySet);
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
        return entrySet().equals(other.entrySet());
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (Node<K, V> node : cache.values()) {
            Object key = node.key;
            Object value = node.value;
            hashCode = 31 * hashCode + (key == null ? 0 : key.hashCode());
            hashCode = 31 * hashCode + (value == null ? 0 : value.hashCode());
        }
        return EncryptionUtilities.finalizeHash(hashCode);
    }

    @Override
    public String toString() {
        return MapUtilities.mapToString(this);
    }
}
