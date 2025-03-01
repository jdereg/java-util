package com.cedarsoftware.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Map implementation keyed on Class objects that leverages a ClassValue cache for extremely
 * fast lookups on non-null keys. Null keys and null values are supported by delegating to a
 * ConcurrentHashMapNullSafe for storage.
 *
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
public class ClassValueMap<V> extends AbstractMap<Class<?>, V> implements ConcurrentMap<Class<?>, V> {

    // Sentinel used by the ClassValue cache to indicate "no value"
    private static final Object NO_VALUE = new Object();

    // Backing map that supports null keys and null values.
    private final ConcurrentMap<Class<?>, V> backingMap = new ConcurrentHashMapNullSafe<>();

    // Storage for the null key (since ClassValue cannot handle null keys)
    private final AtomicReference<V> nullKeyValue = new AtomicReference<>();

    // A ClassValue cache for extremely fast lookups on non-null Class keys.
    // When a key is missing from backingMap, we return NO_VALUE.
    private final ClassValue<Object> cache = new ClassValue<Object>() {
        @Override
        protected Object computeValue(Class<?> key) {
            V value = backingMap.get(key);
            return (value != null || backingMap.containsKey(key)) ? value : NO_VALUE;
        }
    };

    @Override
    public V get(Object key) {
        if (key == null) {
            return nullKeyValue.get();
        }
        if (!(key instanceof Class)) {
            return null;
        }
        Class<?> clazz = (Class<?>) key;
        Object value = cache.get(clazz);
        return (value == NO_VALUE) ? null : (V) value;
    }

    @Override
    public V put(Class<?> key, V value) {
        if (key == null) {
            return nullKeyValue.getAndSet(value);
        }
        V old = backingMap.put(key, value);
        cache.remove(key); // Invalidate cached value for this key.
        return old;
    }

    @Override
    public V remove(Object key) {
        if (key == null) {
            return nullKeyValue.getAndSet(null);
        }
        if (!(key instanceof Class)) {
            return null;
        }
        Class<?> clazz = (Class<?>) key;
        V old = backingMap.remove(clazz);
        cache.remove(clazz);
        return old;
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return nullKeyValue.get() != null;
        }
        if (!(key instanceof Class)) {
            return false;
        }
        Class<?> clazz = (Class<?>) key;
        return cache.get(clazz) != NO_VALUE;
    }

    @Override
    public void clear() {
        backingMap.clear();
        nullKeyValue.set(null);
        // Invalidate cache entries. (Since ClassValue doesn't provide a bulk-clear,
        // we remove entries for the keys in our backingMap.)
        for (Class<?> key : backingMap.keySet()) {
            cache.remove(key);
        }
    }

    @Override
    public int size() {
        // Size is the backingMap size plus 1 if a null-key mapping exists.
        return backingMap.size() + (nullKeyValue.get() != null ? 1 : 0);
    }

    @Override
    public Set<Entry<Class<?>, V>> entrySet() {
        // Combine the null-key entry (if present) with the backingMap entries.
        return new AbstractSet<Entry<Class<?>, V>>() {
            @Override
            public Iterator<Entry<Class<?>, V>> iterator() {
                // First, create an iterator over the backing map entries.
                Iterator<Entry<Class<?>, V>> backingIterator = backingMap.entrySet().iterator();
                // And prepare the null-key entry if one exists.
                final Entry<Class<?>, V> nullEntry =
                        (nullKeyValue.get() != null) ? new SimpleImmutableEntry<>(null, nullKeyValue.get()) : null;
                return new Iterator<Entry<Class<?>, V>>() {
                    private boolean nullEntryReturned = (nullEntry == null);

                    @Override
                    public boolean hasNext() {
                        return !nullEntryReturned || backingIterator.hasNext();
                    }

                    @Override
                    public Entry<Class<?>, V> next() {
                        if (!nullEntryReturned) {
                            nullEntryReturned = true;
                            return nullEntry;
                        }
                        return backingIterator.next();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Removal not supported via iterator.");
                    }
                };
            }

            @Override
            public int size() {
                return ClassValueMap.this.size();
            }
        };
    }

    // The remaining ConcurrentMap methods (putIfAbsent, replace, etc.) can be implemented by
    // delegating to the backingMap and invalidating the cache as needed.

    @Override
    public V putIfAbsent(Class<?> key, V value) {
        if (key == null) {
            return nullKeyValue.compareAndSet(null, value) ? null : nullKeyValue.get();
        }
        V prev = backingMap.putIfAbsent(key, value);
        cache.remove(key);
        return prev;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (key == null) {
            return nullKeyValue.compareAndSet((V) value, null);
        }
        if (!(key instanceof Class)) {
            return false;
        }
        boolean removed = backingMap.remove(key, value);
        cache.remove((Class<?>) key);
        return removed;
    }

    @Override
    public boolean replace(Class<?> key, V oldValue, V newValue) {
        if (key == null) {
            return nullKeyValue.compareAndSet(oldValue, newValue);
        }
        boolean replaced = backingMap.replace(key, oldValue, newValue);
        cache.remove(key);
        return replaced;
    }

    @Override
    public V replace(Class<?> key, V value) {
        if (key == null) {
            V prev = nullKeyValue.get();
            nullKeyValue.set(value);
            return prev;
        }
        V replaced = backingMap.replace(key, value);
        cache.remove(key);
        return replaced;
    }

    @Override
    public Collection<V> values() {
        // Combine values from the backingMap with the null-key value (if present)
        Set<V> vals = new HashSet<>(backingMap.values());
        if (nullKeyValue.get() != null) {
            vals.add(nullKeyValue.get());
        }
        return vals;
    }
}
