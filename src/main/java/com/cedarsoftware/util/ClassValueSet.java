package com.cedarsoftware.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Set implementation for Class objects that leverages a ClassValue cache for extremely
 * fast membership tests. This specialized collection is designed for scenarios where you
 * frequently need to check if a Class is a member of a set.
 *
 * <h2>Performance Advantages</h2>
 * <p>
 * ClassValueSet provides significantly faster {@code contains()} operations compared to standard
 * Set implementations:
 * <ul>
 *   <li>2-10x faster than HashSet for membership checks</li>
 *   <li>Faster than {@code ConcurrentHashMap.keySet()} for concurrent reads — <b>but only for a
 *       small number of instances.</b> The advantage inverts once many ClassValue-based collections
 *       compete on the same classes; read <a href="#constraints">Critical usage constraints</a>
 *       before adopting</li>
 *   <li>Most significant when a <b>few</b> instances are checked repeatedly</li>
 * </ul>
 *
 * <p><b>Not a general-purpose "faster {@code Set<Class>}".</b> This is a specialist for a narrow
 * niche (few instances, read-mostly). Outside it a plain
 * {@link java.util.concurrent.ConcurrentHashMap#newKeySet() ConcurrentHashMap.newKeySet()} is as
 * fast or faster — see <a href="#constraints">Critical usage constraints</a>.
 *
 * <h2>Typed fast path: {@link #containsClass(Class)}</h2>
 * <p>
 * The standard {@link #contains(Object)} method must accept an {@code Object} and perform both
 * a null check and a runtime {@code o.getClass() != Class.class} guard before routing to the
 * {@link ClassValue} cache (non-{@code Class} inputs return {@code false}). When the caller
 * already knows the input is a {@code Class}, {@link #containsClass(Class)} skips both guards
 * and compiles to a near-direct {@link ClassValue#get(Class)} call — a JIT-intrinsified,
 * identity-based per-{@code Class} load.
 * <p>
 * For performance-critical call sites, prefer {@code containsClass(Class)}. To take advantage of
 * it, hold the field as {@code ClassValueSet} (not {@code Set<Class<?>>}), so the compiler
 * resolves the typed lookup statically.
 *
 * <h2>How It Works</h2>
 * <p>
 * Each class's membership is a small flag stored in the class itself, through a {@link ClassValue}; the member
 * classes are also listed, weakly, for iteration and {@code size()}. So the set never keeps a class, or its
 * ClassLoader, alive: a member goes away when its class is unloaded. Java's {@link ClassValue} mechanism is specially
 * optimized in the JVM through:
 * <ul>
 *   <li>Thread-local caching for reduced contention</li>
 *   <li>Identity-based lookups (faster than equality checks)</li>
 *   <li>Special VM support that connects directly to Class metadata structures</li>
 *   <li>Optimized memory layout that can reduce cache misses</li>
 * </ul>
 *
 * <h2>Ideal Use Cases</h2>
 * <p>
 * ClassValueSet is ideal for:
 * <ul>
 *   <li>High read-to-write ratio scenarios (read-mostly workloads)</li>
 *   <li>Relatively static sets of classes that are checked frequently</li>
 *   <li>Performance-critical operations in hot code paths</li>
 *   <li>Security blocklists (checking if a class is forbidden)</li>
 *   <li>Feature flags or capability testing based on class membership</li>
 *   <li>Type handling in serialization/deserialization frameworks</li>
 * </ul>
 *
 * <h2>Trade-offs</h2>
 * <p>
 * The performance benefits come with some trade-offs:
 * <ul>
 *   <li>A flag (a small object) is kept in every class the set is asked about, member or not</li>
 *   <li>Iteration, {@code size()}, {@code equals()} and {@code hashCode()} walk the weakly-listed members and read
 *       each class's flag: slower than a {@code ConcurrentHashMap.newKeySet()}'s</li>
 *   <li>Only Class objects benefit from the optimized lookups</li>
 * </ul>
 *
 * <h2 id="constraints">Critical usage constraints (read before adopting)</h2>
 * <p>
 * Use {@code ClassValueSet} only when the following holds; otherwise prefer a plain
 * {@link java.util.concurrent.ConcurrentHashMap#newKeySet() ConcurrentHashMap.newKeySet()}:
 * <ul>
 *   <li><b>Few instances — the cost is non-local.</b> ClassValue's speed comes from a per-{@code
 *       Class} fast cache, but the JVM gives every {@code Class} <em>one</em> small, fixed-capacity
 *       cache array <b>shared across all {@link ClassValue} instances</b> (open-addressed, probe
 *       limit of 3). Every {@code ClassValueSet} is a distinct {@code ClassValue}, so many of them
 *       keyed on the same classes evict one another from that array and each membership test falls
 *       through to a slower backup path. This set's performance therefore depends on how many
 *       <em>other</em> ClassValue-based collections exist in the JVM — unlike any ordinary {@code
 *       Set}. The advantage holds for a handful and <b>inverts</b> once ~10+ compete on the same
 *       classes.</li>
 * </ul>
 * <p>
 * (Earlier versions kept every member in a backing {@code Set} as well, which kept each member class and its
 * ClassLoader alive, and made {@code add} and {@code remove} invalidate the cached membership of every class, so
 * building a set lazily cost ~O(N&sup2;). Neither applies now: {@code add} and {@code remove} write one class's flag.)
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This implementation is thread-safe for all operations.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a set of blocked classes for security checks
 * ClassValueSet blockedClasses = ClassValueSet.of(
 *     ClassLoader.class,
 *     Runtime.class,
 *     ProcessBuilder.class
 * );
 *
 * // Fast membership check in a security-sensitive context — containsClass
 * // skips the null check + instanceof-Class guard that contains(Object) must perform.
 * public void verifyClass(Class<?> clazz) {
 *     if (blockedClasses.containsClass(clazz)) {
 *         throw new SecurityException("Access to " + clazz.getName() + " is not allowed");
 *     }
 * }
 * }</pre>
 *
 * <h2>Important Performance Warning</h2>
 * <p>
 * Wrapping this class with standard collection wrappers like {@code Collections.unmodifiableSet()}
 * will destroy the {@code ClassValue} performance benefits. Always use the raw {@code ClassValueSet} directly
 * or use the provided {@code unmodifiableView()} method if immutability is required. Note that
 * {@code unmodifiableView()} returns a {@code Set<Class<?>>}, which does not expose
 * {@link #containsClass(Class)} — callers that need the typed fast path should hold the view as
 * a reference to the raw {@code ClassValueSet}.
 *
 * @see ClassValue
 * @see Set
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
public class ClassValueSet extends AbstractSet<Class<?>> {

    // Flag for null element
    private final AtomicBoolean containsNull = new AtomicBoolean(false);

    // The members (other than null); clear() replaces them all at once.
    private volatile Members members = new Members();
    // Cached unmodifiable view to avoid repeated wrapper allocations.
    private volatile Set<Class<?>> unmodifiableView;

    /**
     * Each class's membership is a flag stored in the class itself (a {@link ClassValue}), so the set never keeps a
     * class -- or its ClassLoader -- alive: when the class is unloaded, its flag goes with it. An add() or remove()
     * writes one class's flag, under that flag's lock; it never invalidates the cached lookups of other classes
     * (removing a ClassValue's value does, for every class at once).
     * <p>
     * The flag is a JDK type (an AtomicBoolean): a ClassValue's value lives inside its Class, and a java-util type
     * stored in a JDK class would keep java-util's own class loader alive for the life of the JVM.
     * <p>
     * The member classes are also listed, weakly, for iteration and size().
     * <p>
     * This object is the ClassValue itself, which saves a load on every lookup. The JDK holds a ClassValue only
     * weakly from the classes it has values in, so it pins nothing.
     */
    private static final class Members extends ClassValue<AtomicBoolean> {
        final Set<ClassValueMap.ClassKey> keys = ConcurrentHashMap.newKeySet();
        final ReferenceQueue<Class<?>> unloaded = new ReferenceQueue<>();

        @Override
        protected AtomicBoolean computeValue(Class<?> type) {
            return new AtomicBoolean();
        }

        AtomicBoolean flag(Class<?> cls) {
            return get(cls);
        }

        /** Whether {@code cls} is a member, without leaving a flag in it (for probing another set in bulk). */
        boolean listed(Class<?> cls) {
            return keys.contains(new ClassValueMap.ClassKey(cls, null));
        }

        /** Called holding the class's flag lock, when it becomes a member. */
        void added(Class<?> cls) {
            expunge();
            keys.add(new ClassValueMap.ClassKey(cls, unloaded));
        }

        /** Called holding the class's flag lock, when it stops being one. */
        void removed(Class<?> cls) {
            keys.remove(new ClassValueMap.ClassKey(cls, null));
        }

        /** Forgets the members whose classes have been unloaded. */
        void expunge() {
            Reference<? extends Class<?>> ref;
            while ((ref = unloaded.poll()) != null) {
                keys.remove(ref);
            }
        }
    }

    /**
     * Creates an empty ClassValueSet.
     */
    public ClassValueSet() {
    }

    /**
     * Creates a ClassValueSet containing the elements of the specified collection.
     *
     * @param c the collection whose elements are to be placed into this set
     */
    public ClassValueSet(Collection<? extends Class<?>> c) {
        addAll(c);
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return containsNull.get();
        }
        // Fast path: identity check is faster than instanceof hierarchy check
        // Class.class is loaded by bootstrap classloader, so identity comparison is safe
        if (o.getClass() != Class.class) {
            return false;
        }
        return members.flag((Class<?>) o).get();
    }

    /**
     * Typed fast-path membership test for {@link Class} keys. Unlike {@link #contains(Object)},
     * this skips the null check and the {@code o.getClass() != Class.class} guard that
     * {@link #contains(Object)} must perform to reject non-{@code Class} keys — compiling
     * to a near-direct {@link ClassValue#get(Class)} call, which the JVM intrinsifies as a
     * per-{@code Class} identity load.
     *
     * @param clazz the class to test for membership; {@code null} returns the null-element flag
     * @return {@code true} if this set contains the given class (or {@code null} is mapped when
     *         {@code clazz} is {@code null})
     */
    public boolean containsClass(Class<?> clazz) {
        if (clazz == null) {
            return containsNull.get();
        }
        return members.flag(clazz).get();
    }

    @Override
    public boolean add(Class<?> cls) {
        if (cls == null) {
            return !containsNull.getAndSet(true);
        }
        Members m = members;
        AtomicBoolean flag = m.flag(cls);
        if (flag.get()) {
            return false;
        }
        synchronized (flag) {
            if (flag.get()) {
                return false;
            }
            flag.set(true);
            m.added(cls);
            return true;
        }
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) {
            return containsNull.getAndSet(false);
        }
        // Fast path: identity check is faster than instanceof
        if (o.getClass() != Class.class) {
            return false;
        }
        return removeClass((Class<?>) o);
    }

    private boolean removeClass(Class<?> cls) {
        Members m = members;
        AtomicBoolean flag = m.flag(cls);
        if (!flag.get()) {
            return false;
        }
        synchronized (flag) {
            if (!flag.get()) {
                return false;
            }
            flag.set(false);
            m.removed(cls);
            return true;
        }
    }

    /**
     * Removes all classes from this set.
     */
    @Override
    public void clear() {
        members = new Members();
        containsNull.set(false);
    }

    /**
     * {@inheritDoc}
     * <p>As with a {@link java.util.WeakHashMap}, the count can include, briefly, a member whose class has just been
     * unloaded.</p>
     */
    @Override
    public int size() {
        Members m = members;
        m.expunge();
        return m.keys.size() + (containsNull.get() ? 1 : 0);
    }

    @Override
    public boolean isEmpty() {
        Members m = members;
        m.expunge();
        return m.keys.isEmpty() && !containsNull.get();
    }

    /**
     * Returns true if this set equals another object.
     * For sets, equality means they contain the same elements.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Set)) {
            return false;
        }
        Set<?> other = (Set<?>) o;
        if (other.size() != size()) {
            return false;
        }
        try {
            // Size equality + A ⊆ B implies A = B, so one direction suffices.
            if (containsNull.get() && !other.contains(null)) {
                return false;
            }
            MemberIterator members = new MemberIterator();
            while (members.hasNext()) {
                if (!other.contains(members.next())) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Returns the hash code value for this set.
     * The hash code of a set is the sum of the hash codes of its elements.
     * Null's hash code is 0 by convention, so it doesn't contribute to the sum.
     */
    @Override
    public int hashCode() {
        int h = 0;
        MemberIterator members = new MemberIterator();
        while (members.hasNext()) {
            h += members.next().hashCode();
        }
        return h;
    }

    /**
     * Whether {@code c} contains {@code cls}. Another ClassValueSet is probed through its member list: its contains()
     * would leave a flag in every class it is asked about, a side effect of this bulk read.
     */
    private static boolean holds(Collection<?> c, Class<?> cls) {
        return c instanceof ClassValueSet ? ((ClassValueSet) c).members.listed(cls) : c.contains(cls);
    }

    /**
     * Retains only the elements in this set that are contained in the specified collection.
     *
     * @param c collection containing elements to be retained in this set
     * @return true if this set changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c, "Collection cannot be null");

        if (c == this) {
            return false;
        }
        if (c.isEmpty()) {
            if (isEmpty()) {
                return false;
            }
            clear();
            return true;
        }

        boolean modified = false;

        // Handle null element specially
        if (containsNull.get() && !c.contains(null)) {
            containsNull.set(false);
            modified = true;
        }

        MemberIterator members = new MemberIterator();
        while (members.hasNext()) {
            Class<?> cls = members.next();
            if (!holds(c, cls) && removeClass(cls)) {
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c, "Collection cannot be null");

        if (c.isEmpty()) {
            return false;
        }
        if (c == this) {
            if (isEmpty()) {
                return false;
            }
            clear();
            return true;
        }

        boolean modified = false;

        if (containsNull.get() && c.contains(null)) {
            containsNull.set(false);
            modified = true;
        }

        if (!(c instanceof ClassValueSet) && c.size() < size()) {
            for (Object o : c) {
                if (o != null && o.getClass() == Class.class && removeClass((Class<?>) o)) {
                    modified = true;
                }
            }
            return modified;
        }

        MemberIterator members = new MemberIterator();
        while (members.hasNext()) {
            Class<?> cls = members.next();
            if (holds(c, cls) && removeClass(cls)) {
                modified = true;
            }
        }
        return modified;
    }

    /**
     * Walks the member classes that are still loaded (not null). The list changes under each class's flag lock with
     * its flag, so it is the members; weakly consistent, like a ConcurrentHashMap's iterators (a class removed while
     * iterating may still be returned).
     */
    private final class MemberIterator implements Iterator<Class<?>> {
        private final Members m = members;
        private final Iterator<ClassValueMap.ClassKey> keys = m.keys.iterator();
        private Class<?> next;

        MemberIterator() {
            advance();
        }

        private void advance() {
            while (keys.hasNext()) {
                Class<?> cls = keys.next().get();
                if (cls != null) {                          // still loaded
                    next = cls;
                    return;
                }
            }
            next = null;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Class<?> next() {
            Class<?> cls = next;
            if (cls == null) {
                throw new NoSuchElementException();
            }
            advance();
            return cls;
        }
    }

    @Override
    public Iterator<Class<?>> iterator() {
        final boolean hasNull = containsNull.get();
        final MemberIterator memberIterator = new MemberIterator();

        return new Iterator<Class<?>>() {
            private boolean nullReturned = !hasNull;
            private Class<?> lastReturned = null;
            private boolean canRemove = false;

            @Override
            public boolean hasNext() {
                return !nullReturned || memberIterator.hasNext();
            }

            @Override
            public Class<?> next() {
                if (!nullReturned) {
                    nullReturned = true;
                    lastReturned = null;
                    canRemove = true;
                    return null;
                }

                lastReturned = memberIterator.next();
                canRemove = true;
                return lastReturned;
            }

            @Override
            public void remove() {
                if (!canRemove) {
                    throw new IllegalStateException("next() has not been called, or remove() has already been called after the last call to next()");
                }

                canRemove = false;

                if (lastReturned == null) {
                    // Removing the null element
                    containsNull.set(false);
                } else {
                    removeClass(lastReturned);
                }
            }
        };
    }

    /**
     * Returns a new set containing all elements from this set
     *
     * @return a new set containing the same elements
     */
    public Set<Class<?>> toSet() {
        // Use HashSet here since it needs to support null elements
        Set<Class<?>> result = new HashSet<>();
        if (containsNull.get()) {
            result.add(null);
        }
        MemberIterator members = new MemberIterator();
        while (members.hasNext()) {
            result.add(members.next());
        }
        return result;
    }

    /**
     * Factory method to create a ClassValueSet from an existing Collection
     *
     * @param collection the source collection
     * @return a new ClassValueSet containing the same elements
     */
    public static ClassValueSet from(Collection<? extends Class<?>> collection) {
        return new ClassValueSet(collection);
    }

    /**
     * Factory method that creates a set using the provided classes
     *
     * @param classes the classes to include in the set
     * @return a new ClassValueSet containing the provided classes
     */
    public static ClassValueSet of(Class<?>... classes) {
        ClassValueSet set = new ClassValueSet();
        if (classes != null) {
            Collections.addAll(set, classes);
        }
        return set;
    }

    /**
     * Returns an unmodifiable view of this set that preserves ClassValue performance benefits.
     * Unlike Collections.unmodifiableSet(), this method returns a view that maintains
     * the fast membership-testing performance for Class elements.
     *
     * @return an unmodifiable view of this set with preserved performance characteristics
     */
    public Set<Class<?>> unmodifiableView() {
        Set<Class<?>> cached = unmodifiableView;
        if (cached != null) {
            return cached;
        }

        final ClassValueSet thisSet = this;
        Set<Class<?>> view = new AbstractSet<Class<?>>() {
            @Override
            public Iterator<Class<?>> iterator() {
                final Iterator<Class<?>> originalIterator = thisSet.iterator();

                return new Iterator<Class<?>>() {
                    @Override
                    public boolean hasNext() {
                        return originalIterator.hasNext();
                    }

                    @Override
                    public Class<?> next() {
                        return originalIterator.next();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Cannot modify an unmodifiable set");
                    }
                };
            }

            @Override
            public int size() {
                return thisSet.size();
            }

            @Override
            public boolean contains(Object o) {
                return thisSet.contains(o); // Preserves ClassValue optimization
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return thisSet.containsAll(c);
            }

            @Override
            public boolean isEmpty() {
                return thisSet.isEmpty();
            }

            @Override
            public Object[] toArray() {
                return thisSet.toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return thisSet.toArray(a);
            }

            // All mutator methods throw UnsupportedOperationException
            @Override
            public boolean add(Class<?> e) {
                throw new UnsupportedOperationException("Cannot modify an unmodifiable set");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("Cannot modify an unmodifiable set");
            }

            @Override
            public boolean addAll(Collection<? extends Class<?>> c) {
                throw new UnsupportedOperationException("Cannot modify an unmodifiable set");
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException("Cannot modify an unmodifiable set");
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException("Cannot modify an unmodifiable set");
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("Cannot modify an unmodifiable set");
            }

            @Override
            public String toString() {
                return thisSet.toString();
            }

            @Override
            public int hashCode() {
                return thisSet.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return this == obj || thisSet.equals(obj);
            }
        };

        unmodifiableView = view;
        return view;
    }
}
