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

    public LRUCache(int capacity, StrategyType strategyType) {
        this(capacity, strategyType, 10, null, null);
    }

    public LRUCache(int capacity, StrategyType strategyType, int cleanupDelayMillis, ScheduledExecutorService scheduler, ForkJoinPool cleanupPool) {
        switch (strategyType) {
            case THREADED:
                this.strategy = new ThreadedLRUCacheStrategy<>(capacity, cleanupDelayMillis, scheduler, cleanupPool);
                break;
            case LOCKING:
                this.strategy = new LockingLRUCacheStrategy<>(capacity);
                break;
            default:
                throw new IllegalArgumentException("Unknown strategy type");
        }
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
        return strategy.remove((K)key);
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
        return strategy.containsKey((K)key);
    }

    @Override
    public boolean containsValue(Object value) {
        return strategy.containsValue((V)value);
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
        return strategy.equals(obj);
    }

    public void shutdown() {
        if (strategy instanceof ThreadedLRUCacheStrategy) {
            ((ThreadedLRUCacheStrategy<K, V>) strategy).shutdown();
        }
    }
}
