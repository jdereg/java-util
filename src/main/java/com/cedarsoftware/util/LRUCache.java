package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class provides a Least Recently Used (LRU) cache API that will evict the least recently used items,
 * once a threshold is met.  It implements the Map interface for convenience.
 * <p>
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
    private final Map<K, V> cache;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public LRUCache(int capacity) {
        this.cache = Collections.synchronizedMap(new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        });
    }

    // Implement Map interface
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return cache.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean containsKey(Object key) {
        lock.readLock().lock();
        try {
            return cache.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean containsValue(Object value) {
        lock.readLock().lock();
        try {
            return cache.containsValue(value);
        } finally {
            lock.readLock().unlock();
        }
    }

    public V get(Object key) {
        lock.readLock().lock();
        try {
            return cache.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public V put(K key, V value) {
        lock.writeLock().lock();
        try {
            return cache.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public V remove(Object key) {
        lock.writeLock().lock();
        try {
            return cache.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        lock.writeLock().lock();
        try {
            cache.putAll(m);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Set<K> keySet() {
        lock.readLock().lock();
        try {
            return cache.keySet();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Collection<V> values() {
        lock.readLock().lock();
        try {
            return cache.values();
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<Map.Entry<K, V>> entrySet() {
        lock.readLock().lock();
        try {
            return cache.entrySet();
        } finally {
            lock.readLock().unlock();
        }
    }

    public V putIfAbsent(K key, V value) {
        lock.writeLock().lock();
        try {
            V existingValue = cache.get(key);
            if (existingValue == null) {
                cache.put(key, value);
                return null;
            }
            return existingValue;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
