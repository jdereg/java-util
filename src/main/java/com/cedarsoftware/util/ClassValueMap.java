package com.cedarsoftware.util;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Map implementation keyed on Class objects that leverages a ClassValue cache for extremely
 * fast lookups. This specialized collection is designed for scenarios where you frequently
 * need to retrieve values associated with Class keys.
 *
 * <h2>Performance Advantages</h2>
 * <p>
 * ClassValueMap provides significantly faster {@code get()} operations compared to standard
 * Map implementations:
 * <ul>
 *   <li>2-10x faster than HashMap for key lookups</li>
 *   <li>3-15x faster than ConcurrentHashMap for concurrent access patterns</li>
 *   <li>The performance advantage increases with contention (multiple threads)</li>
 *   <li>Most significant when looking up the same class keys repeatedly</li>
 * </ul>
 *
 * <h2>How It Works</h2>
 * <p>
 * The implementation utilizes Java's {@link ClassValue} mechanism, which is specially optimized
 * in the JVM through:
 * <ul>
 *   <li>Thread-local caching for reduced contention</li>
 *   <li>Identity-based lookups (faster than equality checks)</li>
 *   <li>Special VM support that connects directly to Class metadata structures</li>
 *   <li>Optimized memory layout that can reduce cache misses</li>
 * </ul>
 *
 * <h2>Drop-in Replacement</h2>
 * <p>
 * ClassValueMap is designed as a drop-in replacement for existing maps with Class keys:
 * <ul>
 *   <li>Fully implements the {@link java.util.Map} and {@link java.util.concurrent.ConcurrentMap} interfaces</li>
 *   <li>Supports all standard map operations (put, remove, clear, etc.)</li>
 *   <li>Handles null keys and null values just like standard map implementations</li>
 *   <li>Thread-safe for all operations</li>
 * </ul>
 *
 * <h2>Ideal Use Cases</h2>
 * <p>
 * ClassValueMap is ideal for:
 * <ul>
 *   <li>High read-to-write ratio scenarios (read-mostly workloads)</li>
 *   <li>Caches for class-specific handlers, factories, or metadata</li>
 *   <li>Performance-critical operations in hot code paths</li>
 *   <li>Type registries in frameworks (serializers, converters, validators)</li>
 *   <li>Class capability or feature mappings</li>
 *   <li>Any system that frequently maps from Class objects to associated data</li>
 * </ul>
 *
 * <h2>Trade-offs</h2>
 * <p>
 * The performance benefits come with some trade-offs:
 * <ul>
 *   <li>Higher memory usage (maintains both a backing map and ClassValue cache)</li>
 *   <li>Write operations (put/remove) aren't faster and may be slightly slower</li>
 *   <li>Only Class keys benefit from the optimized lookups</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This implementation is thread-safe for all operations and implements ConcurrentMap.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a registry of class handlers
 * ClassValueMap<Handler> handlerRegistry = new ClassValueMap<>();
 * handlerRegistry.put(String.class, new StringHandler());
 * handlerRegistry.put(Integer.class, new IntegerHandler());
 * handlerRegistry.put(List.class, new ListHandler());
 *
 * // Fast lookup in a performance-critical context
 * public void process(Object obj) {
 *     Handler handler = handlerRegistry.get(obj.getClass());
 *     if (handler != null) {
 *         handler.handle(obj);
 *     } else {
 *         // Default handling
 *     }
 * }
 * }</pre>
 *
 * <h2>Important Performance Warning</h2>
 * <p>
 * Wrapping this class with standard collection wrappers like {@code Collections.unmodifiableMap()}
 * or {@code Collections.newSetFromMap()} will destroy the {@code ClassValue} performance benefits.
 * Always use the raw {@code ClassValueMap} directly or use the provided {@code unmodifiableView()} method
 * if immutability is required.
 * </p>
 * @see ClassValue
 * @see Map
 * @see ConcurrentMap
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

    // Sentinel used to represent null values in the backing ConcurrentHashMap
    // (since ConcurrentHashMap doesn't allow null values)
    private static final Object NULL_VALUE = new Object();

    // Backing map - using raw ConcurrentHashMap since null keys are handled separately.
    // This avoids the overhead of ConcurrentHashMapNullSafe's null-masking wrapper.
    // Note: We use Object as value type to allow storing NULL_VALUE sentinel.
    private final ConcurrentHashMap<Class<?>, Object> backingMap = new ConcurrentHashMap<>();

    // Storage for the null key (since ClassValue cannot handle null keys)
    private final AtomicReference<V> nullKeyValue = new AtomicReference<>();

    // Track whether a null value has been explicitly stored for null key
    private volatile boolean hasNullKeyMapping = false;

    // A ClassValue cache for extremely fast lookups on non-null Class keys.
    // When a key is missing from backingMap, we return NO_VALUE.
    private final ClassValue<Object> cache = new ClassValue<Object>() {
        @Override
        protected Object computeValue(Class<?> key) {
            // Single lookup - ConcurrentHashMap.get() returns null only if key is not present
            Object result = backingMap.get(key);
            if (result == null) {
                return NO_VALUE;
            }
            // Unmask null sentinel
            return result == NULL_VALUE ? null : result;
        }
    };

    // Helper to mask null values for storage in ConcurrentHashMap
    private Object maskNull(V value) {
        return value == null ? NULL_VALUE : value;
    }

    // Helper to unmask null values from storage
    @SuppressWarnings("unchecked")
    private V unmaskNull(Object value) {
        return value == NULL_VALUE ? null : (V) value;
    }

    /**
     * Creates a ClassValueMap
     */
    public ClassValueMap() {
    }

    /**
     * Creates a ClassValueMap containing the mappings from the specified map.
     *
     * @param map the map whose mappings are to be placed in this map
     * @throws NullPointerException if the specified map is null
     */
    public ClassValueMap(Map<? extends Class<?>, ? extends V> map) {
        if (map == null) {
            throw new NullPointerException("Map cannot be null");
        }
        // Batch initialization without per-entry cache invalidation.
        // The cache is empty at construction time, so no invalidation is needed.
        for (Map.Entry<? extends Class<?>, ? extends V> entry : map.entrySet()) {
            Class<?> key = entry.getKey();
            if (key == null) {
                nullKeyValue.set(entry.getValue());
                hasNullKeyMapping = true;
            } else {
                backingMap.put(key, maskNull(entry.getValue()));
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        if (key == null) {
            return nullKeyValue.get();
        }
        // Fast path: identity check is faster than instanceof hierarchy check
        // Class.class is loaded by bootstrap classloader, so identity comparison is safe
        if (key.getClass() == Class.class) {
            Object value = cache.get((Class<?>) key);
            return (value == NO_VALUE) ? null : (V) value;
        }
        return null;
    }

    @Override
    public V put(Class<?> key, V value) {
        if (key == null) {
            V old = nullKeyValue.getAndSet(value);
            hasNullKeyMapping = true;
            return old;
        }
        Object old = backingMap.put(key, maskNull(value));
        cache.remove(key); // Invalidate cached value for this key.
        return unmaskNull(old);
    }

    @Override
    public V remove(Object key) {
        if (key == null) {
            V old = nullKeyValue.getAndSet(null);
            hasNullKeyMapping = false;
            return old;
        }
        // Fast path: identity check is faster than instanceof
        if (key.getClass() != Class.class) {
            return null;
        }
        Class<?> clazz = (Class<?>) key;
        Object old = backingMap.remove(clazz);
        if (old != null) {
            cache.remove(clazz);
        }
        return unmaskNull(old);
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return hasNullKeyMapping;
        }
        // Fast path: identity check is faster than instanceof
        if (key.getClass() != Class.class) {
            return false;
        }
        return cache.get((Class<?>) key) != NO_VALUE;
    }

    @Override
    public void clear() {
        // Iterate and invalidate cache entries before clearing the backing map.
        // We iterate directly over keySet (no copy needed) and invalidate each entry.
        // This is O(n) but necessary for correctness - cached entries would otherwise
        // return stale values after clear().
        for (Class<?> key : backingMap.keySet()) {
            cache.remove(key);
        }
        backingMap.clear();
        nullKeyValue.set(null);
        hasNullKeyMapping = false;
    }

    @Override
    public int size() {
        // Size is the backingMap size plus 1 if a null-key mapping exists.
        return backingMap.size() + (hasNullKeyMapping ? 1 : 0);
    }

    @Override
    public Set<Entry<Class<?>, V>> entrySet() {
        // Combine the null-key entry (if present) with the backingMap entries.
        return new AbstractSet<Entry<Class<?>, V>>() {
            @Override
            public Iterator<Entry<Class<?>, V>> iterator() {
                // First, create an iterator over the backing map entries.
                final Iterator<Entry<Class<?>, Object>> backingIterator = backingMap.entrySet().iterator();
                // Check if null-key mapping exists using the flag (avoids volatile read in loop)
                final boolean hasNullEntry = hasNullKeyMapping;
                final V nullValue = hasNullEntry ? nullKeyValue.get() : null;

                return new Iterator<Entry<Class<?>, V>>() {
                    private boolean nullEntryReturned = !hasNullEntry;

                    @Override
                    public boolean hasNext() {
                        return !nullEntryReturned || backingIterator.hasNext();
                    }

                    @Override
                    public Entry<Class<?>, V> next() {
                        if (!nullEntryReturned) {
                            nullEntryReturned = true;
                            return new SimpleImmutableEntry<>(null, nullValue);
                        }
                        Entry<Class<?>, Object> entry = backingIterator.next();
                        return new SimpleImmutableEntry<>(entry.getKey(), unmaskNull(entry.getValue()));
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
            if (hasNullKeyMapping) {
                return nullKeyValue.get();
            }
            if (nullKeyValue.compareAndSet(null, value)) {
                hasNullKeyMapping = true;
                return null;
            }
            return nullKeyValue.get();
        }
        Object prev = backingMap.putIfAbsent(key, maskNull(value));
        if (prev == null) {
            // Only invalidate cache if insertion actually occurred
            cache.remove(key);
        }
        return unmaskNull(prev);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean remove(Object key, Object value) {
        if (key == null) {
            if (nullKeyValue.compareAndSet((V) value, null)) {
                hasNullKeyMapping = false;
                return true;
            }
            return false;
        }
        // Fast path: identity check is faster than instanceof
        if (key.getClass() != Class.class) {
            return false;
        }
        // Mask the value for comparison since backingMap stores masked values
        boolean removed = backingMap.remove(key, maskNull((V) value));
        if (removed) {
            // Only invalidate cache if removal actually occurred
            cache.remove((Class<?>) key);
        }
        return removed;
    }

    @Override
    public boolean replace(Class<?> key, V oldValue, V newValue) {
        if (key == null) {
            return nullKeyValue.compareAndSet(oldValue, newValue);
        }
        // Mask both values for comparison/storage since backingMap stores masked values
        boolean replaced = backingMap.replace(key, maskNull(oldValue), maskNull(newValue));
        if (replaced) {
            // Only invalidate cache if replacement actually occurred
            cache.remove(key);
        }
        return replaced;
    }

    @Override
    public V replace(Class<?> key, V value) {
        if (key == null) {
            if (!hasNullKeyMapping) {
                return null;
            }
            return nullKeyValue.getAndSet(value);
        }
        Object replaced = backingMap.replace(key, maskNull(value));
        if (replaced != null) {
            // Only invalidate cache if replacement actually occurred
            cache.remove(key);
        }
        return unmaskNull(replaced);
    }

    @Override
    public Collection<V> values() {
        // Return a live view that combines values from backingMap with null-key value.
        // This avoids the O(n) allocation and copy of the previous implementation.
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                final Iterator<Object> backingIterator = backingMap.values().iterator();
                final boolean hasNullEntry = hasNullKeyMapping;
                final V nullValue = hasNullEntry ? nullKeyValue.get() : null;

                return new Iterator<V>() {
                    private boolean nullReturned = !hasNullEntry;

                    @Override
                    public boolean hasNext() {
                        return !nullReturned || backingIterator.hasNext();
                    }

                    @Override
                    public V next() {
                        if (!nullReturned) {
                            nullReturned = true;
                            return nullValue;
                        }
                        return unmaskNull(backingIterator.next());
                    }
                };
            }

            @Override
            public int size() {
                return ClassValueMap.this.size();
            }

            @Override
            @SuppressWarnings("unchecked")
            public boolean contains(Object o) {
                if (hasNullKeyMapping && java.util.Objects.equals(nullKeyValue.get(), o)) {
                    return true;
                }
                // Need to mask the value for comparison since backingMap stores masked values
                return backingMap.containsValue(o == null ? NULL_VALUE : o);
            }
        };
    }

    /**
     * Returns an unmodifiable view of this map that preserves ClassValue performance benefits.
     * Unlike Collections.unmodifiableMap(), this method returns a view that maintains
     * the fast lookup performance for Class keys.
     *
     * @return an unmodifiable view of this map with preserved performance characteristics
     */
    public Map<Class<?>, V> unmodifiableView() {
        final ClassValueMap<V> thisMap = this;

        return new AbstractMap<Class<?>, V>() {
            @Override
            public Set<Entry<Class<?>, V>> entrySet() {
                return Collections.unmodifiableSet(thisMap.entrySet());
            }

            @Override
            public V get(Object key) {
                return thisMap.get(key); // Preserves ClassValue optimization
            }

            @Override
            public boolean containsKey(Object key) {
                return thisMap.containsKey(key); // Preserves ClassValue optimization
            }

            @Override
            public Set<Class<?>> keySet() {
                return Collections.unmodifiableSet(thisMap.keySet());
            }

            @Override
            public Collection<V> values() {
                return Collections.unmodifiableCollection(thisMap.values());
            }

            @Override
            public int size() {
                return thisMap.size();
            }

            // All mutator methods throw UnsupportedOperationException
            @Override
            public V put(Class<?> key, V value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public V remove(Object key) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void putAll(Map<? extends Class<?>, ? extends V> m) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException();
            }
        };
    }
}