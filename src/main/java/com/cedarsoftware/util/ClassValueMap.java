package com.cedarsoftware.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
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
 *   <li>Faster than ConcurrentHashMap for concurrent reads — <b>but only for a small number of
 *       instances.</b> The advantage inverts once many ClassValue-based collections compete on the
 *       same classes; read <a href="#constraints">Critical usage constraints</a> before adopting</li>
 *   <li>Most significant when a <b>few</b> instances are read repeatedly</li>
 * </ul>
 *
 * <p><b>Not a general-purpose "faster {@code Map<Class,V>}".</b> This is a specialist for a narrow
 * niche (few instances, read-mostly). Outside that niche a plain
 * {@link java.util.concurrent.ConcurrentHashMap} is as fast or faster — see
 * <a href="#constraints">Critical usage constraints</a>.
 *
 * <h2>Typed fast path: {@link #getByClass(Class)}</h2>
 * <p>
 * The standard {@link #get(Object)} method must accept an {@code Object} and perform a runtime
 * {@code instanceof Class} guard before routing to the {@link ClassValue} cache (keys that are
 * not {@code Class} instances return {@code null} immediately — only {@code Class} keys can ever
 * be stored). When the
 * caller already knows the key is a {@code Class}, {@link #getByClass(Class)} skips that guard
 * entirely and compiles to a near-direct {@link ClassValue#get(Class)} call — a JIT-intrinsified,
 * identity-based per-{@code Class} load.
 * <p>
 * For performance-critical call sites, prefer {@code getByClass(Class)}. To take advantage of it,
 * hold the field as {@code ClassValueMap<V>} (not {@code Map<Class<?>, V>}), so the compiler
 * resolves the typed lookup statically.
 *
 * <h2>How It Works</h2>
 * <p>
 * Each class's value is kept in a small slot stored in the class itself, through a {@link ClassValue}; the
 * classes that have a mapping are also listed, weakly, for iteration and {@code size()}. So the map never keeps a
 * class, or its ClassLoader, alive: as in a {@link java.util.WeakHashMap}, an entry goes away when its class is
 * unloaded, even when the value refers back to the class. Java's {@link ClassValue} mechanism is specially optimized
 * in the JVM through:
 * <ul>
 *   <li>Thread-local caching for reduced contention</li>
 *   <li>Identity-based lookups (faster than equality checks)</li>
 *   <li>Special VM support that connects directly to Class metadata structures</li>
 *   <li>Optimized memory layout that can reduce cache misses</li>
 * </ul>
 * <p>
 * The value, for its part, lives as long as its class. A value stored for a class that outlives the value's own
 * class loader -- a library's object stored for a JDK class such as {@code String}, by a map the library keeps in a
 * static field -- keeps that loader reachable for the life of the JVM. It matters only when that loader is meant to
 * be discarded (a web application's {@code WEB-INF/lib}, on redeploy). This is how {@link ClassValue} works; any
 * per-class cache built on it does the same.
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
 *   <li>A slot (a small object) is kept in every class the map is asked about, found or not</li>
 *   <li>Iteration, {@code size()} and {@code containsValue()} walk the weakly-listed keys and read each class's
 *       slot: slower than a {@code ConcurrentHashMap}'s</li>
 *   <li>Only Class keys benefit from the optimized lookups</li>
 * </ul>
 *
 * <h2 id="constraints">Critical usage constraints (read before adopting)</h2>
 * <p>
 * Use {@code ClassValueMap} only when <b>all</b> of the following hold; otherwise prefer a plain
 * {@link java.util.concurrent.ConcurrentHashMap}:
 * <ul>
 *   <li><b>Few instances — the cost is non-local.</b> ClassValue's speed comes from a per-{@code
 *       Class} fast cache, but the JVM gives every {@code Class} <em>one</em> small, fixed-capacity
 *       cache array <b>shared across all {@link ClassValue} instances</b> (open-addressed, probe
 *       limit of 3). Every {@code ClassValueMap} is a distinct {@code ClassValue}, so many of them
 *       keyed on the same classes evict one another from that array and each lookup falls through to
 *       a slower backup path (a {@code WeakHashMap} re-promote). This map's performance therefore
 *       depends on how many <em>other</em> ClassValue-based collections exist in the JVM — unlike any
 *       ordinary {@code Map}. The "faster than {@code ConcurrentHashMap}" advantage holds for a
 *       handful and <b>inverts</b> once ~10+ compete on the same classes.</li>
 *   <li><b>Values are kept in the key's class.</b> A value lives as long as its class (or until it is
 *       removed): a value of a short-lived class loader's type, stored for a long-lived class (a JDK class,
 *       say), keeps that loader alive while it is mapped.</li>
 * </ul>
 * <p>
 * (Earlier versions kept every mapping in a backing {@code ConcurrentHashMap} as well, which kept each key class and
 * its ClassLoader alive, and made {@code put} invalidate the cached lookup of every class, so filling a map lazily
 * cost ~O(N&sup2;). Neither applies now: {@code put} writes one class's slot.)
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
 * // Fast lookup in a performance-critical context — getByClass skips the
 * // instanceof-Class guard that get(Object) must perform.
 * public void process(Object obj) {
 *     Handler handler = handlerRegistry.getByClass(obj.getClass());
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
 * if immutability is required. Note that {@code unmodifiableView()} returns a
 * {@code Map<Class<?>, V>}, which does not expose {@link #getByClass(Class)} — callers that need
 * the typed fast path should hold the view as a reference to the raw {@code ClassValueMap}.
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

    // In a class's slot: no mapping. (A mapping to null is stored as null.)
    private static final Object NO_VALUE = new Object();

    // Sentinel for "no mapping exists for null key"
    private static final Object NO_NULL_KEY_MAPPING = new Object();

    // Sentinel for "null key maps to null value" (distinct from "no mapping")
    private static final Object NULL_FOR_NULL_KEY = new Object();

    // Single atomic field for null key storage - eliminates race conditions between
    // separate flag and value fields. Uses sentinels to distinguish:
    // - NO_NULL_KEY_MAPPING: no mapping for null key
    // - NULL_FOR_NULL_KEY: null key explicitly maps to null value
    // - any other value: null key maps to that value
    private final AtomicReference<Object> nullKeyStore = new AtomicReference<>(NO_NULL_KEY_MAPPING);

    // Cached view objects (following JDK AbstractMap/HashMap pattern).
    // Marked volatile to close the DCL race: without volatile, a thread that reads a
    // non-null cached view reference has no happens-before guarantee that the view's
    // internal fields are fully visible. Views are instantiated at most once per map
    // lifetime, so the volatile write is paid once; the volatile reads on the
    // entrySet()/values()/unmodifiableView() access paths are off the hot lookup path.
    private transient volatile Set<Entry<Class<?>, V>> cachedEntrySet;
    private transient volatile Collection<V> cachedValues;
    private transient volatile Map<Class<?>, V> cachedUnmodifiableView;

    // The mappings for non-null keys; clear() replaces them all at once.
    private volatile Mappings mappings = new Mappings();

    /**
     * Each class's value is kept in a slot stored in the class itself (a {@link ClassValue}), so the map never keeps
     * a class -- or its ClassLoader -- alive: when the class is unloaded, its slot and value go with it. A value that
     * refers back to its class (a Constructor, a hierarchy) is no obstacle, because class and value become garbage
     * together. A put() or remove() writes one class's slot, under that slot's lock; it never invalidates the cached
     * lookups of other classes (removing or replacing a ClassValue's value does, for every class at once).
     * <p>
     * The slot is a JDK type (an AtomicReference): a ClassValue's value lives inside its Class, and a java-util type
     * stored in a JDK class would keep java-util's own class loader alive for the life of the JVM.
     * <p>
     * The classes that have a mapping are also listed, weakly, for iteration and size().
     * <p>
     * This object is the ClassValue itself, which saves a load on every lookup. The JDK holds a ClassValue only
     * weakly from the classes it has values in, so it pins nothing.
     */
    private static final class Mappings extends ClassValue<AtomicReference<Object>> {
        final Set<ClassKey> keys = ConcurrentHashMap.newKeySet();
        final ReferenceQueue<Class<?>> unloaded = new ReferenceQueue<>();

        @Override
        protected AtomicReference<Object> computeValue(Class<?> type) {
            return new AtomicReference<>(NO_VALUE);
        }

        AtomicReference<Object> slot(Class<?> key) {
            return get(key);
        }

        /** Called holding the key's slot lock, when the key goes from unmapped to mapped. */
        void added(Class<?> key) {
            expunge();
            keys.add(new ClassKey(key, unloaded));
        }

        /** Called holding the key's slot lock, when the key goes from mapped to unmapped. */
        void removed(Class<?> key) {
            keys.remove(new ClassKey(key, null));
        }

        /** Forgets the keys whose classes have been unloaded. */
        void expunge() {
            Reference<? extends Class<?>> ref;
            while ((ref = unloaded.poll()) != null) {
                keys.remove(ref);
            }
        }
    }

    /** A weak reference to a class: equal to another for the same class, and only to itself once cleared. */
    static final class ClassKey extends WeakReference<Class<?>> {   // also ClassValueSet's
        private final int hash;

        ClassKey(Class<?> key, ReferenceQueue<Class<?>> queue) {
            super(key, queue);
            hash = System.identityHashCode(key);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ClassKey)) {
                return false;
            }
            Class<?> key = get();
            return key != null && key == ((ClassKey) o).get();
        }
    }

    // Helper to check if null key has a mapping
    private boolean hasNullKeyMapping() {
        return nullKeyStore.get() != NO_NULL_KEY_MAPPING;
    }

    // Helper to get the value for null key (returns null if no mapping)
    @SuppressWarnings("unchecked")
    private V getNullKeyValue() {
        Object stored = nullKeyStore.get();
        if (stored == NO_NULL_KEY_MAPPING) {
            return null;
        }
        return stored == NULL_FOR_NULL_KEY ? null : (V) stored;
    }

    // Helper to mask value for null key storage
    private Object maskNullKeyValue(V value) {
        return value == null ? NULL_FOR_NULL_KEY : value;
    }

    // Helper to unmask value from null key storage
    @SuppressWarnings("unchecked")
    private V unmaskNullKeyValue(Object stored) {
        if (stored == NO_NULL_KEY_MAPPING) {
            return null;
        }
        return stored == NULL_FOR_NULL_KEY ? null : (V) stored;
    }

    // A slot's value as the map's: null for "no mapping"
    @SuppressWarnings("unchecked")
    private static <V> V valueOf(Object slotValue) {
        return slotValue == NO_VALUE ? null : (V) slotValue;
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
        for (Map.Entry<? extends Class<?>, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V get(Object key) {
        if (key == null) {
            return getNullKeyValue();
        }
        // Fast path: identity check is faster than instanceof hierarchy check
        // Class.class is loaded by bootstrap classloader, so identity comparison is safe
        if (key.getClass() == Class.class) {
            return valueOf(mappings.slot((Class<?>) key).get());
        }
        return null;
    }

    /**
     * Typed-access companion to {@link #get(Object)} for callers that already hold a
     * {@code Class<?>} reference. Bypasses the {@code key.getClass() == Class.class}
     * type guard that {@link #get(Object)} must perform because its {@code Map}
     * contract takes {@code Object}. Results are identical to {@link #get(Object)};
     * the only difference is fewer instructions on the hot path.
     * <p>
     * Null-safe: a null {@code key} returns the current null-key mapping (or
     * {@code null} if none), matching the semantics of {@link #get(Object)} with
     * a {@code null} argument.
     * <p>
     * Use this method anywhere the caller has a {@code Class<?>} in hand —
     * internal caches, type registries, annotation metadata lookups, conversion
     * dispatch tables, etc. The saved work is small per call (a {@code GETFIELD}
     * + {@code CMP} + branch + a cast the JIT doesn't always elide), but adds up
     * across the millions of calls per second that a library like this sees
     * through its hot paths.
     *
     * @param key the class key, or {@code null}
     * @return the mapped value, or {@code null} if no mapping exists (or if the
     *         null key has no mapping, for a {@code null} argument)
     * @see #get(Object)
     */
    public V getByClass(Class<?> key) {
        if (key == null) {
            return getNullKeyValue();
        }
        return valueOf(mappings.slot(key).get());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden because the {@link ConcurrentMap} default implementation assumes the map
     * cannot contain null values (it treats a null return from {@code get()} as "absent").
     * This map explicitly supports null values, so the default would incorrectly return
     * {@code defaultValue} for a key that is present but mapped to null. This override
     * distinguishes "mapped to null" (returns null) from "no mapping" (returns
     * {@code defaultValue}) in a single slot read.
     */
    @Override
    @SuppressWarnings("unchecked")
    public V getOrDefault(Object key, V defaultValue) {
        if (key == null) {
            Object stored = nullKeyStore.get();
            if (stored == NO_NULL_KEY_MAPPING) {
                return defaultValue;
            }
            return stored == NULL_FOR_NULL_KEY ? null : (V) stored;
        }
        // Only Class keys can ever be stored; identity check is faster than instanceof
        if (key.getClass() != Class.class) {
            return defaultValue;
        }
        Object value = mappings.slot((Class<?>) key).get();
        return value == NO_VALUE ? defaultValue : (V) value;
    }

    @Override
    public V put(Class<?> key, V value) {
        if (key == null) {
            Object old = nullKeyStore.getAndSet(maskNullKeyValue(value));
            return unmaskNullKeyValue(old);
        }
        Mappings m = mappings;
        AtomicReference<Object> slot = m.slot(key);
        synchronized (slot) {
            Object old = slot.getAndSet(value);
            if (old == NO_VALUE) {
                m.added(key);
            }
            return valueOf(old);
        }
    }

    @Override
    public V remove(Object key) {
        if (key == null) {
            Object old = nullKeyStore.getAndSet(NO_NULL_KEY_MAPPING);
            return unmaskNullKeyValue(old);
        }
        // Fast path: identity check is faster than instanceof
        if (key.getClass() != Class.class) {
            return null;
        }
        Class<?> clazz = (Class<?>) key;
        Mappings m = mappings;
        AtomicReference<Object> slot = m.slot(clazz);
        if (slot.get() == NO_VALUE) {
            return null;
        }
        synchronized (slot) {
            Object old = slot.getAndSet(NO_VALUE);
            if (old != NO_VALUE) {
                m.removed(clazz);
            }
            return valueOf(old);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        if (key == null) {
            return hasNullKeyMapping();
        }
        // Fast path: identity check is faster than instanceof
        if (key.getClass() != Class.class) {
            return false;
        }
        return mappings.slot((Class<?>) key).get() != NO_VALUE;
    }

    @Override
    public void clear() {
        Mappings old = mappings;
        mappings = new Mappings();
        nullKeyStore.set(NO_NULL_KEY_MAPPING);
        // Let go of the values now: the old slots stay in their classes until the JVM drops them
        for (ClassKey ref : old.keys) {
            Class<?> key = ref.get();
            if (key != null) {
                old.slot(key).set(NO_VALUE);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>As with a {@link java.util.WeakHashMap}, the count can include, briefly, an entry whose class has just been
     * unloaded.</p>
     */
    @Override
    public int size() {
        Mappings m = mappings;
        m.expunge();
        return m.keys.size() + (hasNullKeyMapping() ? 1 : 0);
    }

    /**
     * {@inheritDoc}
     * <p>A cycle back to a map that is already being rendered prints {@code (cycle)} rather than overflowing the
     * stack; see {@link MapUtilities#mapToString(Map)}.</p>
     */
    @Override
    public String toString() {
        return MapUtilities.mapToString(this);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsValue(Object value) {
        Object nullKeyStored = nullKeyStore.get();
        if (nullKeyStored != NO_NULL_KEY_MAPPING) {
            if (Objects.equals(unmaskNullKeyValue(nullKeyStored), value)) {
                return true;
            }
        }
        for (MappingIterator<Object> it = new MappingIterator<Object>(false) {
            @Override
            Object make(Class<?> key, V mapped) {
                return mapped;
            }
        }; it.hasNext(); ) {
            Object mapped = it.next();
            if (mapped == value || (value != null && value.equals(mapped))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void forEach(java.util.function.BiConsumer<? super Class<?>, ? super V> action) {
        Objects.requireNonNull(action);
        for (Entry<Class<?>, V> entry : entrySet()) {
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Walks the null-key mapping (if any), then the mapped classes that are still loaded, reading each one's slot.
     * Weakly consistent, like a ConcurrentHashMap's iterators; a mapping removed while iterating is skipped.
     */
    private abstract class MappingIterator<T> implements Iterator<T> {
        private final Mappings m = mappings;
        private final Iterator<ClassKey> keys = m.keys.iterator();
        private boolean nullEntryPending;
        private final V nullEntryValue;
        private boolean hasNextMapping;
        private Class<?> nextKey;
        private V nextValue;
        private boolean removeAllowed;
        private boolean lastWasNullEntry;
        private Class<?> lastKey;

        MappingIterator(boolean includeNullKey) {
            Object stored = nullKeyStore.get();
            nullEntryPending = includeNullKey && stored != NO_NULL_KEY_MAPPING;
            nullEntryValue = nullEntryPending ? unmaskNullKeyValue(stored) : null;
            advance();
        }

        private void advance() {
            while (keys.hasNext()) {
                Class<?> key = keys.next().get();
                if (key == null) {
                    continue;                   // unloaded
                }
                Object value = m.slot(key).get();
                if (value == NO_VALUE) {
                    continue;                   // removed meanwhile
                }
                hasNextMapping = true;
                nextKey = key;
                nextValue = valueOf(value);
                return;
            }
            hasNextMapping = false;
            nextKey = null;
            nextValue = null;
        }

        abstract T make(Class<?> key, V value);

        @Override
        public boolean hasNext() {
            return nullEntryPending || hasNextMapping;
        }

        @Override
        public T next() {
            if (nullEntryPending) {
                nullEntryPending = false;
                removeAllowed = true;
                lastWasNullEntry = true;
                lastKey = null;
                return make(null, nullEntryValue);
            }
            if (!hasNextMapping) {
                throw new NoSuchElementException();
            }
            Class<?> key = nextKey;
            V value = nextValue;
            advance();
            removeAllowed = true;
            lastWasNullEntry = false;
            lastKey = key;
            return make(key, value);
        }

        @Override
        public void remove() {
            if (!removeAllowed) {
                throw new IllegalStateException("next() must be called before remove()");
            }
            removeAllowed = false;
            if (lastWasNullEntry) {
                nullKeyStore.set(NO_NULL_KEY_MAPPING);
            } else {
                ClassValueMap.this.remove(lastKey);
            }
        }
    }

    @Override
    public Set<Entry<Class<?>, V>> entrySet() {
        Set<Entry<Class<?>, V>> es = cachedEntrySet;
        if (es != null) {
            return es;
        }
        es = new AbstractSet<Entry<Class<?>, V>>() {
            @Override
            public Iterator<Entry<Class<?>, V>> iterator() {
                return new MappingIterator<Entry<Class<?>, V>>(true) {
                    @Override
                    Entry<Class<?>, V> make(Class<?> key, V value) {
                        return new SimpleImmutableEntry<>(key, value);
                    }
                };
            }

            @Override
            public int size() {
                return ClassValueMap.this.size();
            }
        };
        cachedEntrySet = es;
        return es;
    }

    @Override
    @SuppressWarnings("unchecked")
    public V putIfAbsent(Class<?> key, V value) {
        if (key == null) {
            Object masked = maskNullKeyValue(value);
            while (true) {
                Object current = nullKeyStore.get();
                // Per Map spec, null-mapped key should be treated as absent.
                if (current != NO_NULL_KEY_MAPPING && current != NULL_FOR_NULL_KEY) {
                    return unmaskNullKeyValue(current);
                }
                if (nullKeyStore.compareAndSet(current, masked)) {
                    return null;
                }
            }
        }
        Mappings m = mappings;
        AtomicReference<Object> slot = m.slot(key);
        Object current = slot.get();
        if (current != NO_VALUE && current != null) {
            return (V) current;
        }
        synchronized (slot) {
            current = slot.get();
            // Per Map spec, null-mapped key should be treated as absent.
            if (current != NO_VALUE && current != null) {
                return (V) current;
            }
            slot.set(value);
            if (current == NO_VALUE) {
                m.added(key);
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to correctly handle null-value mappings. The default ConcurrentMap implementation
     * cannot distinguish between "key absent" and "key maps to null" (both return null from get()),
     * causing the computed value to be returned without actually being stored when a null-value
     * mapping exists. The mapping function runs holding the key's lock: it must not update this map.
     */
    @Override
    @SuppressWarnings("unchecked")
    public V computeIfAbsent(Class<?> key, java.util.function.Function<? super Class<?>, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        if (key == null) {
            Object current = nullKeyStore.get();
            if (current != NO_NULL_KEY_MAPPING && current != NULL_FOR_NULL_KEY) {
                return unmaskNullKeyValue(current);  // Non-null value exists
            }
            // Absent or null-mapped — per Map spec, treat both as "compute"
            V newValue = mappingFunction.apply(null);
            if (newValue == null) {
                return null;
            }
            // CAS to install computed value (only if still absent or null-mapped)
            while (true) {
                current = nullKeyStore.get();
                if (current != NO_NULL_KEY_MAPPING && current != NULL_FOR_NULL_KEY) {
                    return unmaskNullKeyValue(current);  // Another thread installed non-null
                }
                if (nullKeyStore.compareAndSet(current, maskNullKeyValue(newValue))) {
                    return newValue;
                }
            }
        }
        // Hit path: one slot read, no lock. After warmup, computeIfAbsent-backed registries hit it ~100% of the time.
        Mappings m = mappings;
        AtomicReference<Object> slot = m.slot(key);
        Object current = slot.get();
        if (current != NO_VALUE && current != null) {
            return (V) current;
        }
        synchronized (slot) {
            current = slot.get();
            // Per Map.computeIfAbsent, a key mapped to null is computed as if absent
            if (current != NO_VALUE && current != null) {
                return (V) current;
            }
            V newValue = mappingFunction.apply(key);
            if (newValue == null) {
                return null;                    // no change
            }
            slot.set(newValue);
            if (current == NO_VALUE) {
                m.added(key);
            }
            return newValue;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to correctly handle null-value mappings. The default ConcurrentMap implementation
     * uses {@code putIfAbsent()} when {@code get()} returns null, but {@code putIfAbsent()} returns
     * null for both "inserted successfully" and "existing value is null," causing incorrect behavior.
     * The remapping function runs holding the key's lock: it must not update this map.
     */
    @Override
    public V compute(Class<?> key, java.util.function.BiFunction<? super Class<?>, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        if (key == null) {
            while (true) {
                Object current = nullKeyStore.get();
                V oldValue = (current == NO_NULL_KEY_MAPPING) ? null : unmaskNullKeyValue(current);
                V newValue = remappingFunction.apply(null, oldValue);
                Object replacement = (newValue == null) ? NO_NULL_KEY_MAPPING : maskNullKeyValue(newValue);
                if (nullKeyStore.compareAndSet(current, replacement)) {
                    return newValue;
                }
                // CAS failed, retry with current state
            }
        }
        Mappings m = mappings;
        AtomicReference<Object> slot = m.slot(key);
        synchronized (slot) {
            Object current = slot.get();
            V newValue = remappingFunction.apply(key, valueOf(current));
            if (newValue == null) {
                if (current != NO_VALUE) {      // Remove mapping
                    slot.set(NO_VALUE);
                    m.removed(key);
                }
                return null;
            }
            slot.set(newValue);
            if (current == NO_VALUE) {
                m.added(key);
            }
            return newValue;
        }
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (key == null) {
            // CAS loop with equals() comparison (compareAndSet uses ==, which is wrong for value matching)
            while (true) {
                Object current = nullKeyStore.get();
                if (current == NO_NULL_KEY_MAPPING) {
                    return false;
                }
                if (!Objects.equals(unmaskNullKeyValue(current), value)) {
                    return false;
                }
                if (nullKeyStore.compareAndSet(current, NO_NULL_KEY_MAPPING)) {
                    return true;
                }
                // CAS failed (concurrent modification), retry
            }
        }
        // Fast path: identity check is faster than instanceof
        if (key.getClass() != Class.class) {
            return false;
        }
        Class<?> clazz = (Class<?>) key;
        Mappings m = mappings;
        AtomicReference<Object> slot = m.slot(clazz);
        synchronized (slot) {
            Object current = slot.get();
            if (current == NO_VALUE || !matches(value, current)) {
                return false;
            }
            slot.set(NO_VALUE);
            m.removed(clazz);
            return true;
        }
    }

    // As ConcurrentHashMap compares: the given value's equals(), applied to what is mapped
    private static boolean matches(Object given, Object mapped) {
        return given == mapped || (given != null && given.equals(mapped));
    }

    @Override
    public boolean replace(Class<?> key, V oldValue, V newValue) {
        if (key == null) {
            // CAS loop with equals() comparison (compareAndSet uses ==, which is wrong for value matching)
            Object replacement = maskNullKeyValue(newValue);
            while (true) {
                Object current = nullKeyStore.get();
                if (current == NO_NULL_KEY_MAPPING) {
                    return false;
                }
                if (!Objects.equals(unmaskNullKeyValue(current), oldValue)) {
                    return false;
                }
                if (nullKeyStore.compareAndSet(current, replacement)) {
                    return true;
                }
                // CAS failed (concurrent modification), retry
            }
        }
        AtomicReference<Object> slot = mappings.slot(key);
        synchronized (slot) {
            Object current = slot.get();
            if (current == NO_VALUE || !matches(oldValue, current)) {
                return false;
            }
            slot.set(newValue);
            return true;
        }
    }

    @Override
    public V replace(Class<?> key, V value) {
        if (key == null) {
            // Atomic replace only if mapping exists - uses CAS loop on single field
            Object masked = maskNullKeyValue(value);
            while (true) {
                Object current = nullKeyStore.get();
                if (current == NO_NULL_KEY_MAPPING) {
                    return null;  // No mapping exists, can't replace
                }
                if (nullKeyStore.compareAndSet(current, masked)) {
                    return unmaskNullKeyValue(current);
                }
                // CAS failed, retry
            }
        }
        AtomicReference<Object> slot = mappings.slot(key);
        synchronized (slot) {
            Object current = slot.get();
            if (current == NO_VALUE) {
                return null;                    // No mapping exists, can't replace
            }
            slot.set(value);
            return valueOf(current);
        }
    }

    @Override
    public Collection<V> values() {
        Collection<V> vs = cachedValues;
        if (vs != null) {
            return vs;
        }
        vs = new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                return new MappingIterator<V>(true) {
                    @Override
                    V make(Class<?> key, V value) {
                        return value;
                    }
                };
            }

            @Override
            public int size() {
                return ClassValueMap.this.size();
            }

            @Override
            public boolean contains(Object o) {
                return containsValue(o);
            }
        };
        cachedValues = vs;
        return vs;
    }

    /**
     * Returns an unmodifiable view of this map that preserves ClassValue performance benefits.
     * Unlike Collections.unmodifiableMap(), this method returns a view that maintains
     * the fast lookup performance for Class keys.
     *
     * @return an unmodifiable view of this map with preserved performance characteristics
     */
    public Map<Class<?>, V> unmodifiableView() {
        Map<Class<?>, V> view = cachedUnmodifiableView;
        if (view != null) {
            return view;
        }
        final ClassValueMap<V> thisMap = this;

        view = new AbstractMap<Class<?>, V>() {
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
            public boolean containsValue(Object value) {
                return thisMap.containsValue(value); // Avoids AbstractMap's slow entrySet scan
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
        cachedUnmodifiableView = view;
        return view;
    }
}
