package com.cedarsoftware.util.cache;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.cedarsoftware.util.ConcurrentHashMapNullSafe;
import com.cedarsoftware.util.ConcurrentSet;
import com.cedarsoftware.util.MapUtilities;

/**
 * This class provides a thread-safe Least Recently Used (LRU) cache API that evicts the least recently used items
 * once a threshold is met. It implements the <code>Map</code> interface for convenience.
 * <p>
 * The Threaded strategy allows for O(1) access for get(), put(), and remove() without blocking. It uses a <code>ConcurrentHashMapNullSafe</code>
 * internally. A single shared background task periodically checks all cache instances and removes the least recently
 * used items from any cache that exceeds its capacity.
 * <p>
 * LRUCache supports <code>null</code> for both key and value.
 * <p>
 * <b>Architecture:</b> All ThreadedLRUCacheStrategy instances share a single cleanup thread that runs periodically.
 * Each cache registers itself via a WeakReference, allowing garbage collection of unused caches. The shared task
 * automatically removes dead references during iteration. This design prevents task accumulation under heavy load
 * and ensures efficient resource usage regardless of how many cache instances are created.
 * <p>
 * <b>Lifecycle:</b> Caches are automatically managed via WeakReferences. Calling {@link #close()} or {@link #shutdown()}
 * explicitly removes the cache from the shared cleanup task. If not explicitly closed, the cache will be cleaned up
 * when garbage collected.
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
public class ThreadedLRUCacheStrategy<K, V> implements Map<K, V>, Closeable {
    private static final int DEFAULT_CLEANUP_INTERVAL_MS = 500;  // Check every 500ms to reduce contention
    private static final int MAX_CLEANUP_BATCH = 10000;  // Limit items removed per cleanup cycle

    private final int capacity;
    private final ConcurrentMap<K, Node<K, V>> cache;
    private final WeakReference<ThreadedLRUCacheStrategy<K, V>> selfRef;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);

    // Shared infrastructure for ALL cache instances
    private static final Set<WeakReference<ThreadedLRUCacheStrategy<?, ?>>> ALL_CACHES =
            ConcurrentHashMap.newKeySet();

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "LRUCache-Cleanup-Thread");
        thread.setDaemon(true);
        return thread;
    });

    // Single shared cleanup task for all cache instances
    private static final ScheduledFuture<?> SHARED_CLEANUP_TASK;

    static {
        // Start the single shared cleanup task
        SHARED_CLEANUP_TASK = scheduler.scheduleAtFixedRate(
                ThreadedLRUCacheStrategy::cleanupAllCaches,
                DEFAULT_CLEANUP_INTERVAL_MS,
                DEFAULT_CLEANUP_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

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
     * Create a ThreadedLRUCacheStrategy with the specified capacity.
     * A shared background task monitors all cache instances and performs cleanup as needed.
     *
     * @param capacity int maximum size for the LRU cache.
     */
    public ThreadedLRUCacheStrategy(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1.");
        }
        this.capacity = capacity;
        this.cache = new ConcurrentHashMapNullSafe<>(capacity);

        // Create weak reference and register with shared cleanup task
        this.selfRef = new WeakReference<>(this);
        @SuppressWarnings("unchecked")
        WeakReference<ThreadedLRUCacheStrategy<?, ?>> ref = (WeakReference<ThreadedLRUCacheStrategy<?, ?>>) (WeakReference<?>) selfRef;
        ALL_CACHES.add(ref);
    }

    /**
     * Create a ThreadedLRUCacheStrategy with the specified capacity.
     * <p>
     * <b>Note:</b> The cleanupDelayMillis parameter is deprecated and ignored.
     * All cache instances now share a single cleanup task with a fixed interval.
     *
     * @param capacity           int maximum size for the LRU cache.
     * @param cleanupDelayMillis ignored (formerly: milliseconds before scheduling cleanup)
     * @deprecated Use {@link #ThreadedLRUCacheStrategy(int)} instead. The cleanupDelayMillis parameter is ignored.
     */
    @Deprecated
    public ThreadedLRUCacheStrategy(int capacity, int cleanupDelayMillis) {
        this(capacity);
    }

    /**
     * Shared cleanup task that iterates all registered caches and cleans those over capacity.
     * Also removes dead WeakReferences (where the cache has been garbage collected).
     */
    private static void cleanupAllCaches() {
        try {
            Iterator<WeakReference<ThreadedLRUCacheStrategy<?, ?>>> iter = ALL_CACHES.iterator();
            while (iter.hasNext()) {
                WeakReference<ThreadedLRUCacheStrategy<?, ?>> ref = iter.next();
                ThreadedLRUCacheStrategy<?, ?> cache = ref.get();
                if (cache == null) {
                    // Cache was garbage collected - remove dead reference
                    iter.remove();
                } else if (!cache.closed.get()) {
                    // Cache is alive and not closed - check if cleanup needed
                    try {
                        cache.cleanup();
                    } catch (Exception e) {
                        // Don't let one cache's failure stop cleanup of others
                    }
                }
            }
        } catch (Exception e) {
            // Catch any exception to prevent the scheduled task from dying
        }
    }

    /**
     * Shuts down this cache, removing it from the shared cleanup task.
     * After calling this method, the cache will no longer perform automatic cleanup.
     * Equivalent to calling {@link #close()}.
     */
    public void shutdown() {
        close();
    }

    /**
     * Forces an immediate cleanup of this cache.
     * This is primarily for testing purposes.
     */
    public void forceCleanup() {
        cleanup();
    }

    /**
     * Returns the number of registered cache instances (for testing/debugging).
     */
    static int getRegisteredCacheCount() {
        return ALL_CACHES.size();
    }

    /**
     * Closes this cache, removing it from the shared cleanup task.
     * This method is idempotent - calling it multiple times has no additional effect.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            @SuppressWarnings("unchecked")
            WeakReference<ThreadedLRUCacheStrategy<?, ?>> ref = (WeakReference<ThreadedLRUCacheStrategy<?, ?>>) (WeakReference<?>) selfRef;
            ALL_CACHES.remove(ref);
        }
    }

    /**
     * Cleanup method that removes least recently used entries to maintain the capacity.
     * Uses LRU eviction by sorting entries by timestamp and removing the oldest ones.
     */
    private void cleanup() {
        // Skip if cleanup is already running (prevents pile-up under heavy load)
        if (!cleanupInProgress.compareAndSet(false, true)) {
            return;
        }
        try {
            int size = cache.size();
            int excess = size - capacity;
            if (excess <= 0) {
                return;
            }

            // Take a snapshot of nodes
            @SuppressWarnings("unchecked")
            Node<K, V>[] nodes = cache.values().toArray(new Node[0]);
            int numNodes = nodes.length;

            // Capture timestamps at snapshot time to avoid sort instability
            // (timestamps can change during sort as other threads access nodes)
            long[] timestamps = new long[numNodes];
            for (int i = 0; i < numNodes; i++) {
                timestamps[i] = nodes[i].timestamp;
            }

            // Create index array and sort by captured timestamps
            Integer[] indices = new Integer[numNodes];
            for (int i = 0; i < numNodes; i++) {
                indices[i] = i;
            }
            Arrays.sort(indices, Comparator.comparingLong(i -> timestamps[i]));

            // Calculate how many to remove:
            // - Normal case: remove all excess items up to MAX_CLEANUP_BATCH
            // - Massively over capacity (>10x): remove all excess to catch up quickly
            int nodesToRemove;
            if (excess > capacity * 10) {
                // Massively over capacity - remove all excess to catch up
                nodesToRemove = excess;
            } else {
                nodesToRemove = Math.min(excess, MAX_CLEANUP_BATCH);
            }

            // Remove the oldest nodes (by captured timestamp order)
            for (int i = 0; i < nodesToRemove && i < numNodes; i++) {
                Node<K, V> node = nodes[indices[i]];
                cache.remove(node.key, node);
            }
        } finally {
            cleanupInProgress.set(false);
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
        }
        // Note: cleanup is handled by the shared cleanup task, not on every put
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
    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        // Use cache's computeIfAbsent for atomicity, but wrap in Node
        Node<K, V> node = cache.computeIfAbsent(key, k -> {
            V value = mappingFunction.apply(k);
            return value != null ? new Node<>(k, value) : null;
        });
        if (node != null) {
            node.updateTimestamp();
            return node.value;
        }
        return null;
    }

    @Override
    public V putIfAbsent(K key, V value) {
        Node<K, V> newNode = new Node<>(key, value);
        Node<K, V> oldNode = cache.putIfAbsent(key, newNode);
        if (oldNode != null) {
            oldNode.updateTimestamp();
            return oldNode.value;
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
        return hashCode;
    }

    @Override
    public String toString() {
        return MapUtilities.mapToString(this);
    }
}
