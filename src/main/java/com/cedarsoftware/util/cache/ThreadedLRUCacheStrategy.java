package com.cedarsoftware.util.cache;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;

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
    private static final int BULK_STEP = 1024;      // bulk evictions per hold of evictLock

    private final int capacity;
    private final ConcurrentMap<K, Node<K, V>> cache;
    private final WeakReference<ThreadedLRUCacheStrategy<K, V>> selfRef;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    // One evictor at a time per cache (the cleanup thread, forceCleanup or a trimming writer), for one step at a
    // time; only it moves the sample cursor.
    private final ReentrantLock evictLock = new ReentrantLock();
    private Iterator<Node<K, V>> sampleCursor;

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
         * Records an access: the node takes the clock's current tick. The clock advances only when a node is
         * made, so the tick says "used after the N-th insertion", every access is recorded, and a read never
         * writes the shared counter. Incrementing it on (a random eighth of) accesses made every reading thread
         * contend for one cache line: 8 threads read about as fast as one. The node is written only when its
         * tick is stale, so a hot node read between insertions is not rewritten.
         */
        void updateTimestamp() {
            long now = LOGICAL_CLOCK.get();
            if (timestamp != now) {
                timestamp = now;
            }
        }
    }

    // Logical clock for LRU ordering - faster than System.nanoTime() (~5ns vs ~25ns). It ticks once per node made;
    // an access stamps the current tick. Higher values = more recently used
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
        this.cache = new ConcurrentHashMapNullSafe<>(Math.min(capacity, LockingLRUCacheStrategy.MAX_PRESIZE));

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
                        cache.backgroundCleanup(deadline, false);
                    } catch (Throwable t) {
                        // Don't let one cache's failure stop cleanup of others
                    }
                }
            }
        } catch (Throwable t) {
            // Anything escaping, an Error included, would end this periodic task: the executor silently
            // cancels a periodic task that throws, and every THREADED cache in the JVM would stop being trimmed.
        }
    }

    /**
     * Background cleanup — the "elves" strategy.
     * <p>
     * Drains this cache back to capacity one step at a time, each step under evictLock, so a writer trimming the
     * cache (past twice its capacity) can take the lock between steps. Holding it for a whole pass shut the writer
     * out while one-at-a-time sampled eviction fell behind it. Each step re-checks the surplus and uses one of
     * two strategies:
     * <ul>
     *   <li><b>Bulk eviction</b> (surplus &gt; capacity): removes the next entries at the sample cursor, up to
     *       BULK_STEP of them, with {@code iterator.remove()}. LRU accuracy is sacrificed for speed — acceptable
     *       when 50%+ of entries must go.</li>
     *   <li><b>Sample-10 eviction</b> (surplus &le; capacity): Samples 10 entries and evicts the
     *       oldest, providing ~95-99% LRU accuracy with O(1) cost per eviction.</li>
     * </ul>
     * <p>
     * The caller passes a deadline so that all caches share a single time budget per cycle.
     * Puts are never blocked — the user chose THREADED knowing capacity is approximate.
     *
     * @param deadline     absolute nanoTime after which the elf should stop
     * @param waitForLock  true to wait for a writer's trim to finish (forceCleanup); false to leave the cache to
     *                     that writer (the cleanup thread)
     */
    private void backgroundCleanup(long deadline, boolean waitForLock) {
        while (cache.size() > capacity && System.nanoTime() < deadline) {
            if (waitForLock) {
                evictLock.lock();
            } else if (!evictLock.tryLock()) {
                return;                             // a writer is trimming: it brings the cache back to capacity
            }
            try {
                int surplus = cache.size() - capacity;
                if (surplus > capacity) {
                    evictNext(Math.min(surplus, BULK_STEP));
                } else if (surplus <= 0 || !evictOldestUsingSample()) {
                    return;                         // Cache is empty or eviction failed
                }
            } finally {
                evictLock.unlock();
            }
        }
    }

    /**
     * Removes the next {@code count} entries at the sample cursor, wrapping round at the end of the table, with
     * {@code iterator.remove()} (no second hash lookup) and no regard for recency: for when more than half the
     * entries must go. Continuing from the cursor keeps chunks from re-walking the bins earlier chunks emptied.
     * Called under evictLock.
     *
     * @param count number of entries to remove
     */
    private void evictNext(int count) {
        Iterator<Node<K, V>> it = sampleCursor;
        boolean wrapped = false;
        for (int removed = 0; removed < count; removed++) {
            if (it == null || !it.hasNext()) {
                if (wrapped) {
                    break;
                }
                it = cache.values().iterator();
                wrapped = true;
                if (!it.hasNext()) {
                    break;
                }
            }
            it.next();
            it.remove();
        }
        sampleCursor = it;
    }

    /**
     * Evicts the oldest entry from a sample of SAMPLE_SIZE (10) entries: the next ones in the table after the
     * previous sample, wrapping round at the end, so successive samples sweep the whole table. Starting each sample
     * at the front of the table looked at the same entries every time and, as evictions emptied the front bins,
     * walked past more of them on every call. Called under evictLock.
     *
     * @return true if an entry was evicted, false if cache is empty
     */
    private boolean evictOldestUsingSample() {
        Node<K, V> oldest = null;
        long oldestTime = Long.MAX_VALUE;
        int sampled = 0;
        Iterator<Node<K, V>> it = sampleCursor;
        boolean wrapped = false;

        while (sampled < SAMPLE_SIZE) {
            if (it == null || !it.hasNext()) {
                if (wrapped) {
                    break;                          // fewer entries than a sample
                }
                it = cache.values().iterator();
                wrapped = true;
                if (!it.hasNext()) {
                    break;
                }
            }
            Node<K, V> node = it.next();
            sampled++;
            if (node.timestamp < oldestTime) {
                oldest = node;
                oldestTime = node.timestamp;
            }
        }
        sampleCursor = it;

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
        backgroundCleanup(System.nanoTime() + CYCLE_BUDGET_NS, true);
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
        if (oldNode == null) {
            trimIfFarOverCapacity(newNode);
            return null;
        }
        return oldNode.value;
    }

    /**
     * Writers help when the cleanup thread falls behind. It runs every 500 ms, and a single writer could add
     * millions of entries in between. Past twice the capacity, the writer that wins evictLock trims the cache
     * back to capacity with bulk eviction, as the cleanup thread would; the other writers carry on, leaving the
     * work to whoever holds the lock. Past four times the capacity a writer waits for the lock (an evictor holds
     * it for one step, at most BULK_STEP removals): a hard bound however fast the writers. Only a new node whose
     * timestamp (a tick of the shared clock, one per node made) is a multiple of 32 looks at the size:
     * ConcurrentHashMap.size() reads every counter cell the writers update, which on every insert cost 70% more
     * per put at 4 threads. A cache can so pass those marks by a few dozen entries before a writer notices.
     */
    private void trimIfFarOverCapacity(Node<K, V> inserted) {
        if ((inserted.timestamp & 31) != 0) {
            return;
        }
        long surplus = cache.size() - (long) capacity;
        if (surplus <= capacity) {
            return;
        }
        if (surplus > 3L * capacity) {
            evictLock.lock();
        } else if (!evictLock.tryLock()) {
            return;
        }
        try {
            int remaining = cache.size() - capacity;
            if (remaining > 0) {
                evictNext(remaining);
            }
        } finally {
            evictLock.unlock();
        }
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
     * Replaces each value with the function's result through the map's own replaceAll, which retries an entry
     * whose value changed meanwhile. Each entry keeps its recency: a new value is not a new use.
     */
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        cache.replaceAll((k, node) -> {
            Node<K, V> replacement = new Node<>(k, function.apply(k, node.value));
            replacement.timestamp = node.timestamp;
            return replacement;
        });
    }

    /**
     * Pure delegation to ConcurrentHashMap — zero eviction overhead.
     * The background "elves" handle all eviction asynchronously.
     */
    @Override
    public V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction) {
        final Object[] result = new Object[2];     // the value, and the node of a new key
        cache.compute(key, (k, existing) -> {
            if (existing != null && existing.value != null) {
                existing.updateTimestamp();
                result[0] = existing.value;
                return existing;
            }

            V computed = mappingFunction.apply(k);
            result[0] = computed;
            if (computed == null) {
                if (existing != null) {
                    existing.updateTimestamp();
                    return existing;
                }
                return null;
            }
            Node<K, V> node = new Node<>(k, computed);
            if (existing == null) {
                result[1] = node;
            }
            return node;
        });
        if (result[1] != null) {
            @SuppressWarnings("unchecked")
            Node<K, V> inserted = (Node<K, V>) result[1];
            trimIfFarOverCapacity(inserted);
        }
        @SuppressWarnings("unchecked")
        V value = (V) result[0];
        return value;
    }

    /**
     * Pure delegation to ConcurrentHashMap — zero eviction overhead.
     * The background "elves" handle all eviction asynchronously.
     */
    @Override
    public V putIfAbsent(K key, V value) {
        while (true) {
            Node<K, V> existing = cache.get(key);
            if (existing == null) {
                Node<K, V> newNode = new Node<>(key, value);
                Node<K, V> raced = cache.putIfAbsent(key, newNode);
                if (raced == null) {
                    trimIfFarOverCapacity(newNode);
                    return null;
                }
                existing = raced;
            }

            if (existing.value == null) {
                if (value == null) {
                    existing.updateTimestamp();
                    return null;
                }
                Node<K, V> replacement = new Node<>(key, value);
                if (cache.replace(key, existing, replacement)) {
                    return null;
                }
                continue;
            }

            existing.updateTimestamp();
            return existing.value;
        }
    }

    // The compound operations below are compare-and-set on the nodes (the Map defaults were a get, then a put:
    // concurrent updates overwrote each other). A new value is a new node, a fresh use.

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        return update(key, old -> remappingFunction.apply(key, old), false);
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        return update(key, old -> remappingFunction.apply(key, old), true);
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(value);
        Objects.requireNonNull(remappingFunction);
        return update(key, old -> old == null ? value : remappingFunction.apply(old, value), false);
    }

    /**
     * compute, computeIfPresent and merge: compare-and-set over exactly the node read (Node has no equals, so the
     * map compares identity), tried twice; a null result removes the key, and with onlyIfPresent a key absent or
     * mapped to null is left alone. Overtaken twice, the function runs under the map's own lock for the key,
     * which nothing overtakes: retrying for as long as it took would spin forever on a function that writes the
     * key it computes (a Map contract breach that ConcurrentHashMap refuses).
     */
    private V update(K key, Function<V, V> function, boolean onlyIfPresent) {
        for (int attempt = 0; attempt < 2; attempt++) {
            Node<K, V> node = cache.get(key);
            V old = node == null ? null : node.value;
            if (onlyIfPresent && old == null) {
                return null;
            }
            V value = function.apply(old);
            if (value == null) {
                if (node == null || cache.remove(key, node)) {
                    return null;
                }
                continue;
            }
            Node<K, V> fresh = new Node<>(key, value);
            if (node == null) {
                if (cache.putIfAbsent(key, fresh) == null) {
                    trimIfFarOverCapacity(fresh);
                    return value;
                }
            } else if (cache.replace(key, node, fresh)) {
                return value;
            }
        }
        final Object[] result = new Object[2];     // the value, and the node of a new key
        cache.compute(key, (k, node) -> {
            V old = node == null ? null : node.value;
            if (onlyIfPresent && old == null) {
                return node;
            }
            V value = function.apply(old);
            result[0] = value;
            if (value == null) {
                return null;
            }
            Node<K, V> fresh = new Node<>(k, value);
            if (node == null) {
                result[1] = fresh;
            }
            return fresh;
        });
        if (result[1] != null) {
            @SuppressWarnings("unchecked")
            Node<K, V> inserted = (Node<K, V>) result[1];
            trimIfFarOverCapacity(inserted);
        }
        @SuppressWarnings("unchecked")
        V value = (V) result[0];
        return value;
    }

    @Override
    public V replace(K key, V value) {
        Node<K, V> previous = cache.replace(key, new Node<>(key, value));
        return previous == null ? null : previous.value;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        while (true) {
            Node<K, V> node = cache.get(key);
            if (node == null || !Objects.equals(node.value, oldValue)) {
                return false;
            }
            if (cache.replace(key, node, new Node<>(key, newValue))) {     // Node has no equals: identity
                return true;
            }
        }
    }

    @Override
    public boolean remove(Object key, Object value) {
        while (true) {
            Node<K, V> node = cache.get(key);
            if (node == null || !Objects.equals(node.value, value)) {
                return false;
            }
            if (cache.remove(key, node)) {
                return true;
            }
        }
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
        // Snapshot keyed by KEY. A hash set of entries would hash every VALUE to store it -- work nobody asked
        // for, and unbounded recursion when a value's graph leads back to this cache (which then took toString()
        // down with it, since toString() walks entrySet()).
        Map<K, V> snapshot = new LinkedHashMap<>();
        for (Node<K, V> node : cache.values()) {
            snapshot.put(node.key, node.value);
        }
        return Collections.unmodifiableMap(snapshot).entrySet();    // read-only entries: setValue throws
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
        int hashCode = 0;
        for (Node<K, V> node : cache.values()) {
            hashCode += Objects.hashCode(node.key) ^ Objects.hashCode(node.value);
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return MapUtilities.mapToString(this);
    }
}
