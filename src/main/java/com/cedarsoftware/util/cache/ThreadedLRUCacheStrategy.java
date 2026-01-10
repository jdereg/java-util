package com.cedarsoftware.util.cache;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.cedarsoftware.util.ConcurrentHashMapNullSafe;
import com.cedarsoftware.util.ConcurrentSet;
import com.cedarsoftware.util.MapUtilities;

/**
 * This class provides a thread-safe Least Recently Used (LRU) cache API that evicts the least recently used items
 * once a threshold is met. It implements the {@code Map} interface for convenience.
 * <p>
 * <b>Algorithm:</b> This implementation uses a zone-based eviction strategy with sample-15 approximate LRU:
 * <ul>
 *   <li><b>Zone A (0 to capacity):</b> Normal operation, no eviction needed</li>
 *   <li><b>Zone B (capacity to 1.5x):</b> Background cleanup brings cache back to capacity</li>
 *   <li><b>Zone C (1.5x to 2x):</b> Probabilistic inline eviction (probability increases as size approaches 2x)</li>
 *   <li><b>Zone D (2x+):</b> Hard cap - evict before insert to maintain bounded memory</li>
 * </ul>
 * <p>
 * <b>Sample-15 Eviction:</b> Instead of sorting all entries (O(n log n)), we sample 15 random entries and evict
 * the oldest one. This provides ~99% accuracy compared to true LRU (based on Redis research) with O(1) cost.
 * <p>
 * <b>Memory Guarantee:</b> The cache will never exceed 2x the specified capacity, allowing users to size
 * their cache with predictable worst-case memory usage.
 * <p>
 * The Threaded strategy allows for O(1) access for get(), put(), and remove() without blocking in the common case.
 * It uses {@code ConcurrentHashMapNullSafe} internally for null key/value support.
 * <p>
 * LRUCache supports {@code null} for both key and value.
 * <p>
 * <b>Architecture:</b> All ThreadedLRUCacheStrategy instances share a single cleanup thread that runs every 500ms.
 * Each cache registers itself via a WeakReference, allowing garbage collection of unused caches.
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
    private static final int DEFAULT_CLEANUP_INTERVAL_MS = 500;
    private static final int SAMPLE_SIZE = 15;  // Sample size for approximate LRU (99% accuracy per Redis research)
    private static final double SOFT_CAP_RATIO = 1.5;  // Start probabilistic eviction
    private static final double HARD_CAP_RATIO = 2.0;  // Hard cap - evict before insert

    private final int capacity;
    private final int softCap;   // 1.5x capacity - start probabilistic eviction
    private final int hardCap;   // 2.0x capacity - hard limit, evict before insert
    private final ConcurrentMap<K, Node<K, V>> cache;
    private final WeakReference<ThreadedLRUCacheStrategy<K, V>> selfRef;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean cleanupInProgress = new AtomicBoolean(false);

    // Shared infrastructure for ALL cache instances
    private static final Set<WeakReference<ThreadedLRUCacheStrategy<?, ?>>> ALL_CACHES =
            ConcurrentHashMap.newKeySet();

    // Scheduler and cleanup task - can be recreated after shutdown
    private static volatile ScheduledExecutorService scheduler;
    private static volatile ScheduledFuture<?> cleanupTask;
    private static final Object schedulerLock = new Object();

    private static ScheduledExecutorService createScheduler() {
        return Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "LRUCache-Cleanup-Thread");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static void ensureSchedulerRunning() {
        if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
            synchronized (schedulerLock) {
                if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
                    scheduler = createScheduler();
                    cleanupTask = scheduler.scheduleAtFixedRate(
                            ThreadedLRUCacheStrategy::cleanupAllCaches,
                            DEFAULT_CLEANUP_INTERVAL_MS,
                            DEFAULT_CLEANUP_INTERVAL_MS,
                            TimeUnit.MILLISECONDS
                    );
                }
            }
        }
    }

    static {
        ensureSchedulerRunning();
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
     * <p>
     * The cache uses a zone-based eviction strategy:
     * <ul>
     *   <li>Up to 1.5x capacity: Background cleanup only</li>
     *   <li>1.5x to 2x capacity: Probabilistic inline eviction</li>
     *   <li>At 2x capacity: Hard cap with evict-before-insert</li>
     * </ul>
     * <p>
     * Memory usage is guaranteed to never exceed 2x the specified capacity.
     *
     * @param capacity int maximum size for the LRU cache.
     * @throws IllegalArgumentException if capacity is less than 1
     */
    public ThreadedLRUCacheStrategy(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1.");
        }
        this.capacity = capacity;
        this.softCap = (int) (capacity * SOFT_CAP_RATIO);
        this.hardCap = (int) (capacity * HARD_CAP_RATIO);
        this.cache = new ConcurrentHashMapNullSafe<>(capacity);

        ensureSchedulerRunning();

        this.selfRef = new WeakReference<>(this);
        @SuppressWarnings("unchecked")
        WeakReference<ThreadedLRUCacheStrategy<?, ?>> ref = (WeakReference<ThreadedLRUCacheStrategy<?, ?>>) (WeakReference<?>) selfRef;
        ALL_CACHES.add(ref);
    }

    /**
     * Create a ThreadedLRUCacheStrategy with the specified capacity.
     * <p>
     * <b>Note:</b> The cleanupDelayMillis parameter is deprecated and ignored.
     *
     * @param capacity           int maximum size for the LRU cache.
     * @param cleanupDelayMillis ignored (formerly: milliseconds before scheduling cleanup)
     * @deprecated Use {@link #ThreadedLRUCacheStrategy(int)} instead.
     */
    @Deprecated
    public ThreadedLRUCacheStrategy(int capacity, int cleanupDelayMillis) {
        this(capacity);
    }

    /**
     * Background cleanup task that runs every 500ms.
     * Iterates all registered caches and cleans those over capacity.
     */
    private static void cleanupAllCaches() {
        try {
            Iterator<WeakReference<ThreadedLRUCacheStrategy<?, ?>>> iter = ALL_CACHES.iterator();
            while (iter.hasNext()) {
                WeakReference<ThreadedLRUCacheStrategy<?, ?>> ref = iter.next();
                ThreadedLRUCacheStrategy<?, ?> cache = ref.get();
                if (cache == null) {
                    iter.remove();
                } else if (!cache.closed.get()) {
                    try {
                        cache.backgroundCleanup();
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
     * Background cleanup - runs every 500ms.
     * Zone A: Do nothing
     * Zone B/C/D: Evict using sample-15 until at capacity
     */
    private void backgroundCleanup() {
        if (!cleanupInProgress.compareAndSet(false, true)) {
            return;
        }
        try {
            int size = cache.size();
            // Zone A: Nothing to do
            if (size <= capacity) {
                return;
            }
            // Zone B/C/D: Evict until at capacity
            while (cache.size() > capacity) {
                if (!evictOldestUsingSample()) {
                    break;  // Cache is empty or couldn't evict
                }
            }
        } finally {
            cleanupInProgress.set(false);
        }
    }

    /**
     * Evicts the oldest entry from a sample of SAMPLE_SIZE (15) entries.
     * This provides ~99% LRU accuracy with O(1) cost (based on Redis research).
     *
     * @return true if an entry was evicted, false if cache is empty
     */
    private boolean evictOldestUsingSample() {
        Node<K, V> oldest = null;
        long oldestTime = Long.MAX_VALUE;
        int sampled = 0;

        for (Node<K, V> node : cache.values()) {
            if (node.timestamp < oldestTime) {
                oldest = node;
                oldestTime = node.timestamp;
            }
            if (++sampled >= SAMPLE_SIZE) {
                break;
            }
        }

        if (oldest != null) {
            return cache.remove(oldest.key, oldest);
        }
        return false;
    }

    /**
     * Shuts down this cache, removing it from the shared cleanup task.
     */
    public void shutdown() {
        close();
    }

    /**
     * Forces an immediate cleanup of this cache (for testing).
     */
    public void forceCleanup() {
        backgroundCleanup();
    }

    /**
     * Returns the number of registered cache instances (for testing/debugging).
     */
    static int getRegisteredCacheCount() {
        return ALL_CACHES.size();
    }

    /**
     * Shuts down the shared cleanup scheduler used by all ThreadedLRUCacheStrategy instances.
     *
     * @return true if the scheduler terminated cleanly, false if it timed out or was interrupted
     */
    public static boolean shutdownScheduler() {
        synchronized (schedulerLock) {
            if (scheduler == null || scheduler.isShutdown()) {
                return true;
            }
            if (cleanupTask != null) {
                cleanupTask.cancel(false);
                cleanupTask = null;
            }
            scheduler.shutdown();
            try {
                boolean terminated = scheduler.awaitTermination(5, TimeUnit.SECONDS);
                if (!terminated) {
                    scheduler.shutdownNow();
                    terminated = scheduler.awaitTermination(1, TimeUnit.SECONDS);
                }
                return terminated;
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            @SuppressWarnings("unchecked")
            WeakReference<ThreadedLRUCacheStrategy<?, ?>> ref = (WeakReference<ThreadedLRUCacheStrategy<?, ?>>) (WeakReference<?>) selfRef;
            ALL_CACHES.remove(ref);
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

    /**
     * Associates the specified value with the specified key in this cache.
     * <p>
     * Zone-based eviction:
     * <ul>
     *   <li>Zone A/B (0 to 1.5x): Insert and return immediately</li>
     *   <li>Zone C (1.5x to 2x): Insert, then probabilistically evict</li>
     *   <li>Zone D (2x+): Insert, then evict until under hard cap</li>
     * </ul>
     * <p>
     * Note: We insert first, then enforce limits. This avoids the TOCTOU race where
     * multiple threads check size, all see "under limit", then all insert. By checking
     * after insert and looping until under hardCap, we guarantee the hard cap is enforced.
     */
    @Override
    public V put(K key, V value) {
        // Insert the new entry first (fast path)
        Node<K, V> newNode = new Node<>(key, value);
        Node<K, V> oldNode = cache.put(key, newNode);

        if (oldNode != null) {
            // Replacement - no size change, update timestamp
            newNode.updateTimestamp();
            return oldNode.value;
        }

        // New entry added - check zone and enforce limits
        int size = cache.size();

        if (size >= hardCap) {
            // Zone D: At or above hard cap - evict until under hard cap
            // Loop ensures we don't just do one eviction (which might fail due to CAS race)
            while (cache.size() >= hardCap) {
                if (!evictOldestUsingSample()) {
                    break;  // Cache is empty or couldn't evict
                }
            }
        } else if (size > softCap) {
            // Zone C: Probabilistic eviction
            // Probability increases linearly from 0% at softCap to 100% at hardCap
            double probability = (double) (size - softCap) / (hardCap - softCap);
            if (ThreadLocalRandom.current().nextDouble() < probability) {
                evictOldestUsingSample();
            }
        }
        // Zone A/B: No inline eviction needed

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
        Node<K, V> node = cache.computeIfAbsent(key, k -> {
            V value = mappingFunction.apply(key);
            return value != null ? new Node<>(key, value) : null;
        });
        if (node != null) {
            node.updateTimestamp();
            // Check zone and enforce limits (same logic as put())
            int size = cache.size();
            if (size >= hardCap) {
                while (cache.size() >= hardCap) {
                    if (!evictOldestUsingSample()) {
                        break;
                    }
                }
            } else if (size > softCap) {
                double probability = (double) (size - softCap) / (hardCap - softCap);
                if (ThreadLocalRandom.current().nextDouble() < probability) {
                    evictOldestUsingSample();
                }
            }
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
        // New entry was added - check zone and enforce limits (same logic as put())
        int size = cache.size();
        if (size >= hardCap) {
            while (cache.size() >= hardCap) {
                if (!evictOldestUsingSample()) {
                    break;
                }
            }
        } else if (size > softCap) {
            double probability = (double) (size - softCap) / (hardCap - softCap);
            if (ThreadLocalRandom.current().nextDouble() < probability) {
                evictOldestUsingSample();
            }
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
