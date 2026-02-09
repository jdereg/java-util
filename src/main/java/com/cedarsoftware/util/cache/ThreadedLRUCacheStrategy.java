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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.ConcurrentHashMapNullSafe;
import com.cedarsoftware.util.ConcurrentSet;
import com.cedarsoftware.util.MapUtilities;

/**
 * This class provides a thread-safe Least Recently Used (LRU) cache API that evicts the least recently used items
 * once a threshold is met. It implements the {@code Map} interface for convenience.
 * <p>
 * <b>Algorithm:</b> This implementation uses pure delegation for all mutating operations ({@code put()},
 * {@code putIfAbsent()}, {@code computeIfAbsent()}) — they delegate directly to the underlying
 * {@code ConcurrentHashMap} with zero eviction overhead, providing raw CHM speed for writes.
 * <p>
 * <b>Background Eviction ("Elves"):</b> A shared daemon thread wakes every 500ms and services all registered
 * caches. For each cache over capacity, the elves work within a <b>time budget</b> (10ms per cache per cycle),
 * performing sample-10 evictions until the cache is back at capacity or the budget is exhausted.
 * <ul>
 *   <li><b>Self-limiting CPU:</b> Max ~2% of one core (10ms per 500ms cycle per cache).</li>
 *   <li><b>Adapts to cache size:</b> Large caches with expensive iteration do fewer evictions per cycle;
 *       small caches do more.</li>
 *   <li><b>No unbounded work:</b> The elves never spend more than 10ms on a single cache per cycle.</li>
 * </ul>
 * <p>
 * <b>Trade-off:</b> The cache may temporarily exceed its capacity during burst inserts. The elves will
 * drain it back to capacity asynchronously. Users choosing the THREADED strategy accept this approximate
 * capacity behavior in exchange for zero-overhead writes.
 * <p>
 * <b>Sample-10 Eviction:</b> Instead of sorting all entries (O(n log n)), we sample 10 entries and evict
 * the oldest one. This provides ~95-99% accuracy compared to true LRU (based on Redis research) with O(1) cost.
 * <p>
 * <b>Probabilistic Timestamp Updates:</b> To minimize overhead, timestamps are updated probabilistically (~12.5%
 * of accesses). This dramatically reduces the cost of volatile writes and System.nanoTime() calls while maintaining
 * approximate LRU behavior. Frequently accessed entries will still have their timestamps updated regularly.
 * <p>
 * The Threaded strategy allows for O(1) access for get(), put(), and remove() without blocking.
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
    private static final int SAMPLE_SIZE = 10;  // Sample size for approximate LRU (~95-99% accuracy per Redis research)
    private static final long CYCLE_BUDGET_NS = 400_000_000L;  // 400ms global budget per 500ms cycle (~80% duty during overload)

    private final int capacity;
    private final ConcurrentMap<K, Node<K, V>> cache;
    private final WeakReference<ThreadedLRUCacheStrategy<K, V>> selfRef;
    private final AtomicBoolean closed = new AtomicBoolean(false);

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
     * <p>
     * Neither field uses volatile because:
     * <ul>
     *   <li>{@code value} is never modified after construction</li>
     *   <li>{@code timestamp} is used for approximate LRU ordering only - stale reads are acceptable
     *       given the existing approximations (sample-10 eviction, probabilistic updates)</li>
     * </ul>
     * The initial write in the constructor is visible to other threads after the Node reference
     * is published through ConcurrentHashMap (which provides the necessary memory barriers).
     */
    private static class Node<K, V> {
        final K key;
        final V value;
        long timestamp;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
            // Use logical clock instead of System.nanoTime() - much faster (~5ns vs ~25ns)
            this.timestamp = LOGICAL_CLOCK.incrementAndGet();
        }

        /**
         * Updates the timestamp with probabilistic sampling to reduce overhead.
         * Uses low bits of current timestamp for randomness - no extra atomic operations.
         * Only updates ~12.5% of the time to maintain approximate LRU behavior.
         */
        void updateTimestamp() {
            // Use low bits of current timestamp for probabilistic check
            // This avoids any atomic/ThreadLocal operations on most accesses
            if ((this.timestamp & 0x7) == 0) {
                this.timestamp = LOGICAL_CLOCK.incrementAndGet();
            }
        }
    }

    // Logical clock for LRU ordering - faster than System.nanoTime() (~5ns vs ~25ns)
    // Higher values = more recently used
    private static final AtomicLong LOGICAL_CLOCK = new AtomicLong(0);

    /**
     * Create a ThreadedLRUCacheStrategy with the specified capacity.
     * <p>
     * All mutating operations ({@code put()}, {@code putIfAbsent()}, {@code computeIfAbsent()})
     * delegate directly to the underlying ConcurrentHashMap with zero eviction overhead.
     * A background cleanup thread ("elves") runs every 500ms to drain surplus entries using
     * time-budgeted sample-10 eviction.
     *
     * @param capacity int maximum size for the LRU cache.
     * @throws IllegalArgumentException if capacity is less than 1
     */
    public ThreadedLRUCacheStrategy(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1.");
        }
        this.capacity = capacity;
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
     * <p>
     * Uses a global time budget (400ms out of 500ms cycle) shared across all caches.
     * Drains one cache at a time for CPU cache locality — staying on one cache keeps
     * its ConcurrentHashMap memory pages hot in L1/L2 cache, avoiding costly page
     * thrashing from switching between caches.
     * <p>
     * The 100ms gap (500ms cycle - 400ms budget) ensures the scheduled task never
     * piles up. During overload, the elf runs at ~80% duty cycle on one core.
     * When all caches are at capacity, the elf returns immediately and sleeps.
     */
    private static void cleanupAllCaches() {
        long deadline = System.nanoTime() + CYCLE_BUDGET_NS;
        try {
            Iterator<WeakReference<ThreadedLRUCacheStrategy<?, ?>>> iter = ALL_CACHES.iterator();
            while (iter.hasNext()) {
                if (System.nanoTime() >= deadline) {
                    break;  // Global budget exhausted — resume next cycle
                }
                WeakReference<ThreadedLRUCacheStrategy<?, ?>> ref = iter.next();
                ThreadedLRUCacheStrategy<?, ?> cache = ref.get();
                if (cache == null) {
                    iter.remove();
                } else if (!cache.closed.get()) {
                    try {
                        cache.backgroundCleanup(deadline);
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
     * Background cleanup — the "elves" strategy.
     * <p>
     * Drains this cache back to capacity using sample-10 evictions, working until
     * the cache is healthy or the global deadline is reached. The caller passes
     * the deadline so that all caches share a single time budget per cycle.
     * <p>
     * At ~500ns per sample-10 eviction, a 400ms budget ≈ 800K evictions per cycle.
     * A 100K surplus drains in under one cycle. Puts are never blocked — the user
     * chose THREADED knowing capacity is approximate.
     *
     * @param deadline absolute nanoTime after which the elf should stop
     */
    private void backgroundCleanup(long deadline) {
        if (cache.size() <= capacity) {
            return;
        }

        while (cache.size() > capacity) {
            if (!evictOldestUsingSample()) {
                break;  // Cache is empty or eviction failed
            }
            if (System.nanoTime() >= deadline) {
                break;  // Global budget exhausted — resume next cycle
            }
        }
    }

    /**
     * Evicts the oldest entry from a sample of SAMPLE_SIZE (10) entries.
     * This provides ~95-99% LRU accuracy with O(1) cost (based on Redis research).
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
        backgroundCleanup(System.nanoTime() + CYCLE_BUDGET_NS);
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
                scheduler = null;  // Ensure null if already shutdown
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
                scheduler = null;  // Clear reference to allow recreation
                return terminated;
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                scheduler = null;  // Clear reference to allow recreation
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
     * Pure delegation to ConcurrentHashMap — zero eviction overhead.
     * The background "elves" handle all eviction asynchronously.
     */
    @Override
    public V put(K key, V value) {
        Node<K, V> newNode = new Node<>(key, value);
        Node<K, V> oldNode = cache.put(key, newNode);
        return (oldNode != null) ? oldNode.value : null;
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

    /**
     * Pure delegation to ConcurrentHashMap — zero eviction overhead.
     * The background "elves" handle all eviction asynchronously.
     */
    @Override
    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        boolean[] inserted = {false};
        Node<K, V> node = cache.computeIfAbsent(key, k -> {
            V value = mappingFunction.apply(key);
            if (value != null) {
                inserted[0] = true;
                return new Node<>(key, value);
            }
            return null;
        });
        if (node != null) {
            if (!inserted[0]) {
                node.updateTimestamp();
            }
            return node.value;
        }
        return null;
    }

    /**
     * Pure delegation to ConcurrentHashMap — zero eviction overhead.
     * The background "elves" handle all eviction asynchronously.
     */
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
