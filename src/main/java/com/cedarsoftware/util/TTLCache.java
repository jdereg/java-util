package com.cedarsoftware.util;

import java.lang.ref.WeakReference;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A cache that holds items for a specified Time-To-Live (TTL) duration.
 * Optionally, it supports Least Recently Used (LRU) eviction when a maximum size is specified.
 * This implementation uses sentinel values to support null keys and values in a ConcurrentHashMap.
 * It utilizes a single background thread to manage purging of expired entries for all cache instances.
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
public class TTLCache<K, V> implements Map<K, V> {

    private final long ttlMillis;
    private final int maxSize;
    private final ConcurrentHashMap<Object, CacheEntry> cacheMap;
    private final ReentrantLock lock = new ReentrantLock();
    private final Node head;
    private final Node tail;

    // Sentinel value for null keys and values
    private static final Object NULL_ITEM = new Object();

    // Static ScheduledExecutorService with a single thread
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    /**
     * Constructs a TTLCache with the specified TTL.
     * When constructed this way, there is no LRU size limitation, and the default cleanup interval is 60 seconds.
     *
     * @param ttlMillis the time-to-live in milliseconds for each cache entry
     */
    public TTLCache(long ttlMillis) {
        this(ttlMillis, -1, 60000);
    }

    /**
     * Constructs a TTLCache with the specified TTL and maximum size.
     * When constructed this way, the default cleanup interval is 60 seconds.
     *
     * @param ttlMillis the time-to-live in milliseconds for each cache entry
     * @param maxSize   the maximum number of entries in the cache (-1 for unlimited)
     */
    public TTLCache(long ttlMillis, int maxSize) {
        this(ttlMillis, maxSize, 60000);
    }

    /**
     * Constructs a TTLCache with the specified TTL, maximum size, and cleanup interval.
     *
     * @param ttlMillis             the time-to-live in milliseconds for each cache entry
     * @param maxSize               the maximum number of entries in the cache (-1 for unlimited)
     * @param cleanupIntervalMillis the cleanup interval in milliseconds for purging expired entries
     */
    public TTLCache(long ttlMillis, int maxSize, long cleanupIntervalMillis) {
        if (ttlMillis < 1) {
            throw new IllegalArgumentException("TTL must be at least 1 millisecond.");
        }
        if (cleanupIntervalMillis < 10) {
            throw new IllegalArgumentException("cleanupIntervalMillis must be at least 10 milliseconds.");
        }
        this.ttlMillis = ttlMillis;
        this.maxSize = maxSize;
        this.cacheMap = new ConcurrentHashMap<>();

        // Initialize the doubly-linked list for LRU tracking
        this.head = new Node(null, null);
        this.tail = new Node(null, null);
        head.next = tail;
        tail.prev = head;

        // Schedule the purging task for this cache
        schedulePurgeTask(cleanupIntervalMillis);
    }

    /**
     * Schedules the purging task for this cache.
     *
     * @param cleanupIntervalMillis the cleanup interval in milliseconds
     */
    private void schedulePurgeTask(long cleanupIntervalMillis) {
        WeakReference<TTLCache<?, ?>> cacheRef = new WeakReference<>(this);
        PurgeTask purgeTask = new PurgeTask(cacheRef);
        scheduler.scheduleAtFixedRate(purgeTask, cleanupIntervalMillis, cleanupIntervalMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Inner class for the purging task.
     */
    private static class PurgeTask implements Runnable {
        private final WeakReference<TTLCache<?, ?>> cacheRef;
        private volatile boolean canceled = false;

        PurgeTask(WeakReference<TTLCache<?, ?>> cacheRef) {
            this.cacheRef = cacheRef;
        }

        @Override
        public void run() {
            TTLCache<?, ?> cache = cacheRef.get();
            if (cache == null) {
                // Cache has been garbage collected; cancel the task
                cancel();
            } else {
                cache.purgeExpiredEntries();
            }
        }

        private void cancel() {
            if (!canceled) {
                canceled = true;
                // Remove this task from the scheduler
                // Since we cannot remove the task directly, we rely on the scheduler to not keep strong references to canceled tasks
            }
        }
    }

    // Inner class representing a node in the doubly-linked list.
    private static class Node {
        final Object key;
        Object value;
        Node prev;
        Node next;

        Node(Object key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    // Inner class representing a cache entry with a value and expiration time.
    private static class CacheEntry {
        final Node node;
        final long expiryTime;

        CacheEntry(Node node, long expiryTime) {
            this.node = node;
            this.expiryTime = expiryTime;
        }
    }

    /**
     * Converts a user-provided key or value to a cache item, handling nulls.
     */
    private Object toCacheItem(Object item) {
        return item == null ? NULL_ITEM : item;
    }

    /**
     * Converts a cache item back to the user-provided key or value, handling nulls.
     */
    @SuppressWarnings("unchecked")
    private <T> T fromCacheItem(Object cacheItem) {
        return cacheItem == NULL_ITEM ? null : (T) cacheItem;
    }

    /**
     * Purges expired entries from this cache.
     */
    private void purgeExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        for (Iterator<Map.Entry<Object, CacheEntry>> it = cacheMap.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Object, CacheEntry> entry = it.next();
            if (entry.getValue().expiryTime < currentTime) {
                it.remove();
                lock.lock();
                try {
                    unlink(entry.getValue().node);
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * Removes an entry from the cache.
     *
     * @param cacheKey the cache key to remove
     */
    private void removeEntry(Object cacheKey) {
        CacheEntry entry = cacheMap.remove(cacheKey);
        if (entry != null) {
            Node node = entry.node;
            lock.lock();
            try {
                unlink(node);
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Unlinks a node from the doubly-linked list.
     *
     * @param node the node to unlink
     */
    private void unlink(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
        node.value = null;
    }

    /**
     * Moves a node to the tail of the list (most recently used position).
     *
     * @param node the node to move
     */
    private void moveToTail(Node node) {
        // Unlink the node
        node.prev.next = node.next;
        node.next.prev = node.prev;

        // Insert at the tail
        node.prev = tail.prev;
        node.next = tail;
        tail.prev.next = node;
        tail.prev = node;
    }

    /**
     * Inserts a node at the tail of the list.
     *
     * @param node the node to insert
     */
    private void insertAtTail(Node node) {
        node.prev = tail.prev;
        node.next = tail;
        tail.prev.next = node;
        tail.prev = node;
    }

    // Implementations of Map interface methods

    @Override
    public V put(K key, V value) {
        Object cacheKey = toCacheItem(key);
        Object cacheValue = toCacheItem(value);
        long expiryTime = System.currentTimeMillis() + ttlMillis;
        Node node = new Node(cacheKey, cacheValue);
        CacheEntry newEntry = new CacheEntry(node, expiryTime);
        CacheEntry oldEntry = cacheMap.put(cacheKey, newEntry);

        boolean acquired = lock.tryLock();
        try {
            if (acquired) {
                insertAtTail(node);

                if (maxSize > -1 && cacheMap.size() > maxSize) {
                    // Evict the least recently used entry
                    Node lruNode = head.next;
                    if (lruNode != tail) {
                        removeEntry(lruNode.key);
                    }
                }
            }
            // If lock not acquired, skip LRU update for performance
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }

        return oldEntry != null ? fromCacheItem(oldEntry.node.value) : null;
    }

    @Override
    public V get(Object key) {
        Object cacheKey = toCacheItem(key);
        CacheEntry entry = cacheMap.get(cacheKey);
        if (entry == null) {
            return null;
        }

        long currentTime = System.currentTimeMillis();
        if (entry.expiryTime < currentTime) {
            removeEntry(cacheKey);
            return null;
        }

        V value = fromCacheItem(entry.node.value);

        boolean acquired = lock.tryLock();
        try {
            if (acquired) {
                moveToTail(entry.node);
            }
            // If lock not acquired, skip LRU update for performance
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }

        return value;
    }

    @Override
    public V remove(Object key) {
        Object cacheKey = toCacheItem(key);
        CacheEntry entry = cacheMap.remove(cacheKey);
        if (entry != null) {
            V value = fromCacheItem(entry.node.value);
            lock.lock();
            try {
                unlink(entry.node);
            } finally {
                lock.unlock();
            }
            return value;
        }
        return null;
    }

    @Override
    public void clear() {
        cacheMap.clear();
        lock.lock();
        try {
            // Reset the linked list
            head.next = tail;
            tail.prev = head;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        return cacheMap.size();
    }

    @Override
    public boolean isEmpty() {
        return cacheMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        Object cacheKey = toCacheItem(key);
        CacheEntry entry = cacheMap.get(cacheKey);
        if (entry == null) {
            return false;
        }
        if (entry.expiryTime < System.currentTimeMillis()) {
            removeEntry(cacheKey);
            return false;
        }
        return true;
    }

    @Override
    public boolean containsValue(Object value) {
        Object cacheValue = toCacheItem(value);
        for (CacheEntry entry : cacheMap.values()) {
            Object entryValue = entry.node.value;
            if (Objects.equals(entryValue, cacheValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        for (CacheEntry entry : cacheMap.values()) {
            K key = fromCacheItem(entry.node.key);
            keys.add(key);
        }
        return keys;
    }

    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (CacheEntry entry : cacheMap.values()) {
            V value = fromCacheItem(entry.node.value);
            values.add(value);
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    /**
     * Custom EntrySet implementation that allows iterator removal.
     */
    private class EntrySet extends AbstractSet<Entry<K, V>> {
        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return TTLCache.this.size();
        }

        @Override
        public void clear() {
            TTLCache.this.clear();
        }
    }

    /**
     * Custom Iterator for the EntrySet.
     */
    private class EntryIterator implements Iterator<Entry<K, V>> {
        private final Iterator<Entry<Object, CacheEntry>> iterator;
        private Entry<Object, CacheEntry> current;

        EntryIterator() {
            this.iterator = cacheMap.entrySet().iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Entry<K, V> next() {
            current = iterator.next();
            K key = fromCacheItem(current.getValue().node.key);
            V value = fromCacheItem(current.getValue().node.value);
            return new AbstractMap.SimpleEntry<>(key, value);
        }

        @Override
        public void remove() {
            if (current == null) {
                throw new IllegalStateException();
            }
            Object cacheKey = current.getKey();
            removeEntry(cacheKey);
            current = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Map)) return false;   // covers null check too

        Map<?, ?> other = (Map<?, ?>) o;
        lock.lock();

        try {
            return entrySet().equals(other.entrySet());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int hashCode() {
        lock.lock();
        try {
            int hashCode = 1;
            for (Node node = head.next; node != tail; node = node.next) {
                Object key = fromCacheItem(node.key);
                Object value = fromCacheItem(node.value);
                hashCode = 31 * hashCode + (key == null ? 0 : key.hashCode());
                hashCode = 31 * hashCode + (value == null ? 0 : value.hashCode());
            }
            return hashCode;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        lock.lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            Iterator<Entry<K, V>> it = entrySet().iterator();
            while (it.hasNext()) {
                Entry<K, V> entry = it.next();
                sb.append(entry.getKey()).append('=').append(entry.getValue());
                if (it.hasNext()) {
                    sb.append(", ");
                }
            }
            sb.append('}');
            return sb.toString();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Shuts down the shared scheduler. Call this method when your application is terminating.
     */
    public static void shutdown() {
        scheduler.shutdown();
    }
}