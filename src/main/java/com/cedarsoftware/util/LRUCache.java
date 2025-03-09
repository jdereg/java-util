package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.cedarsoftware.util.cache.LockingLRUCacheStrategy;
import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;

/**
 * This class provides a thread-safe Least Recently Used (LRU) cache API that evicts the least recently used items once
 * a threshold is met. It implements the <code>Map</code> interface for convenience.
 * <p>
 * This class offers two implementation strategies: a locking approach and a threaded approach.
 * <ul>
 *   <li>The Locking strategy can be selected by using the constructor that takes only an int for capacity, or by using
 *       the constructor that takes an int and a StrategyType enum (StrategyType.LOCKING).</li>
 *   <li>The Threaded strategy can be selected by using the constructor that takes an int and a StrategyType enum
 *       (StrategyType.THREADED). Additionally, there is a constructor that takes a capacity, a cleanup delay time,
 *       and a ScheduledExecutorService.</li>
 * </ul>
 * <p>
 * The Locking strategy allows for O(1) access for get(), put(), and remove(). For put(), remove(), and many other
 * methods, a write-lock is obtained. For get(), it attempts to lock but does not lock unless it can obtain it right away.
 * This 'try-lock' approach ensures that the get() API is never blocking, but it also means that the LRU order is not
 * perfectly maintained under heavy load.
 * <p>
 * The Threaded strategy allows for O(1) access for get(), put(), and remove() without blocking. It uses a <code>ConcurrentHashMapNullSafe</code>
 * internally. To ensure that the capacity is honored, whenever put() is called, a thread (from a thread pool) is tasked
 * with cleaning up items above the capacity threshold. This means that the cache may temporarily exceed its capacity, but
 * it will soon be trimmed back to the capacity limit by the scheduled thread.
 * <p>
 * LRUCache supports <code>null</code> for both <b>key</b> and <b>value</b>.
 * <p>
 * <b>Special Thanks:</b> This implementation was inspired by insights and suggestions from Ben Manes.
 * @see LockingLRUCacheStrategy
 * @see ThreadedLRUCacheStrategy
 * @see LRUCache.StrategyType
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
            strategy = new ThreadedLRUCacheStrategy<>(capacity, 10);
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
     * then it is up to you to manage their termination.  The shutdown() method will not manipulate them in any way.
     * @param capacity int maximum number of entries in the cache.
     * @param cleanupDelayMillis int number of milliseconds after a put() call when a scheduled task should run to
     *                           trim the cache to no more than capacity.  The default is 10ms.
     * @see com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy
     */
    public LRUCache(int capacity, int cleanupDelayMillis) {
        strategy = new ThreadedLRUCacheStrategy<>(capacity, cleanupDelayMillis);
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
        if (!(obj instanceof Map)) {    // covers null check too
            return false;
        }
        Map<?, ?> other = (Map<?, ?>) obj;
        return strategy.equals(other);
    }

    /**
     * This method is no longer needed as the ThreadedLRUCacheStrategy will automatically end because it uses a
     * daemon thread.
     * @deprecated 
     */
    @Deprecated
    public void shutdown() {
    }
}