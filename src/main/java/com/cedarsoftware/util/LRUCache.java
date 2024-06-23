package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

import com.cedarsoftware.util.cache.LockingLRUCacheStrategy;
import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;

public class LRUCache<K, V> implements Map<K, V> {
    private final Map<K, V> strategy;

    public enum StrategyType {
        THREADED,
        LOCKING
    }

    /**
     * Create a "locking-based" LRUCache with the passed in capacity.
     * @param capacity int maximum number of entries in the cache.
     * @see com.cedarsoftware.util.cache.LockingLRUCacheStrategy
     */
    public LRUCache(int capacity) {
        strategy = new LockingLRUCacheStrategy<>(capacity);
    }

    /**
     * Create a "locking-based" OR a "thread-based" LRUCache with the passed in capacity.
     * <p>
     * Note: There is a "shutdown" method on LRUCache to ensure that the default scheduler that was created for you
     * is cleaned up, which is useful in a container environment.
     * @param capacity int maximum number of entries in the cache.
     * @param strategyType StrategyType.LOCKING or Strategy.THREADED indicating the underlying LRUCache implementation.
     * @see com.cedarsoftware.util.cache.LockingLRUCacheStrategy
     * @see com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy
     */
    public LRUCache(int capacity, StrategyType strategyType) {
        if (strategyType == StrategyType.THREADED) {
            strategy = new ThreadedLRUCacheStrategy<>(capacity, 10, null, null);
        } else if (strategyType == StrategyType.LOCKING) {
            strategy = new LockingLRUCacheStrategy<>(capacity);
        } else {
            throw new IllegalArgumentException("Unsupported strategy type: " + strategyType);
        }
    }

    /**
     * Create a "thread-based" LRUCache with the passed in capacity.
     * <p>
     * Note: There is a "shutdown" method on LRUCache to ensure that the default scheduler that was created for you
     * is cleaned up, which is useful in a container environment.  If you supplied your own scheduler and cleanupPool,
     * then it is up to you to manage there termination.  The shutdown() method will not manipulate them in anyway.
     * @param capacity int maximum number of entries in the cache.
     * @param cleanupDelayMillis int number of milliseconds after a put() call when a scheduled task should run to
     *                           trim the cache to no more than capacity.  The default is 10ms.
     * @param scheduler ScheduledExecutorService which can be null, in which case one will be created for you, or you
     *                  can supply your own.
     * @param cleanupPool ForkJoinPool can be null, in which case one will be created for you, you can supply your own.
     * @see com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy
     */
    public LRUCache(int capacity, int cleanupDelayMillis, ScheduledExecutorService scheduler, ForkJoinPool cleanupPool) {
        strategy = new ThreadedLRUCacheStrategy<>(capacity, cleanupDelayMillis, scheduler, cleanupPool);
    }

    @Override
    public V get(Object key) {
        return strategy.get(key);
    }

    @Override
    public V put(K key, V value) {
        return strategy.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        strategy.putAll(m);
    }

    @Override
    public V remove(Object key) {
        return strategy.remove(key);
    }

    @Override
    public void clear() {
        strategy.clear();
    }

    @Override
    public int size() {
        return strategy.size();
    }

    @Override
    public boolean isEmpty() {
        return strategy.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return strategy.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return strategy.containsValue(value);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return strategy.entrySet();
    }

    @Override
    public Set<K> keySet() {
        return strategy.keySet();
    }

    @Override
    public Collection<V> values() {
        return strategy.values();
    }

    @Override
    public String toString() {
        return strategy.toString();
    }

    @Override
    public int hashCode() {
        return strategy.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        LRUCache<?, ?> other = (LRUCache<?, ?>) obj;
        return strategy.equals(other.strategy);
    }

    public void shutdown() {
        if (strategy instanceof ThreadedLRUCacheStrategy) {
            ((ThreadedLRUCacheStrategy<K, V>) strategy).shutdown();
        }
    }
}
