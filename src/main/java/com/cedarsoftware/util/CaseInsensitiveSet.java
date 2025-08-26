package com.cedarsoftware.util;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Spliterator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A {@link java.util.Set} implementation that performs case-insensitive comparisons for {@link String} elements,
 * while preserving the original case of the strings. This set can contain both {@link String} and non-String elements,
 * providing support for homogeneous and heterogeneous collections.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Case-Insensitive String Handling:</b> For {@link String} elements, comparisons are performed
 *       in a case-insensitive manner, but the original case is preserved when iterating or retrieving elements.</li>
 *   <li><b>Homogeneous and Heterogeneous Collections:</b> Supports mixed types within the set, treating non-String
 *       elements as in a normal {@link Set}.</li>
 *   <li><b>Customizable Backing Map:</b> Allows specifying the underlying {@link java.util.Map} implementation,
 *       providing flexibility for use cases requiring custom performance or ordering guarantees.</li>
 *   <li><b>Compatibility with Java Collections Framework:</b> Fully implements the {@link Set} interface,
 *       supporting standard operations like {@code add()}, {@code remove()}, and {@code retainAll()}.</li>
 *   <li><b>Thread Safety:</b> Thread safety depends on the backing map implementation. When backed by
 *       concurrent maps (e.g., {@link ConcurrentHashMap}), the set is thread-safe.</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create a case-insensitive set
 * CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
 * set.add("Hello");
 * set.add("HELLO"); // No effect, as "Hello" already exists
 * LOG.info(set); // Outputs: [Hello]
 *
 * // Mixed types in the set
 * CaseInsensitiveSet<Object> mixedSet = new CaseInsensitiveSet<>();
 * mixedSet.add("Apple");
 * mixedSet.add(123);
 * mixedSet.add("apple"); // No effect, as "Apple" already exists
 * LOG.info(mixedSet); // Outputs: [Apple, 123]
 * }</pre>
 *
 * <h2>Backing Map Selection</h2>
 * <p>
 * The backing map for this set can be customized using various constructors:
 * </p>
 * <ul>
 *   <li>The default constructor uses a {@link CaseInsensitiveMap} with a {@link java.util.LinkedHashMap} backing
 *       to preserve insertion order.</li>
 *   <li>Other constructors allow specifying the backing map explicitly or initializing the set from
 *       another collection.</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * Thread safety depends entirely on the thread safety of the chosen backing map:
 * </p>
 * <ul>
 *   <li><b>Thread-Safe:</b> When backed by concurrent maps ({@link ConcurrentHashMap}, {@link ConcurrentSkipListMap},
 *       {@link ConcurrentHashMapNullSafe}, {@link ConcurrentNavigableMapNullSafe}), all operations are thread-safe.</li>
 *   <li><b>Not Thread-Safe:</b> When backed by non-concurrent maps ({@link java.util.LinkedHashMap}, 
 *       {@link java.util.HashMap}, {@link TreeMap}), external synchronization is required for thread safety.</li>
 * </ul>
 *
 * <h2>Implementation Note</h2>
 * <p>
 * This implementation uses {@link Collections#newSetFromMap(Map)} internally to create a Set view over 
 * a {@link CaseInsensitiveMap}. This provides a clean, efficient implementation that leverages the
 * proven JDK Collections framework while maintaining case-insensitive semantics for String elements.
 * </p>
 *
 * <h2>Deprecated Methods</h2>
 * <p>
 * The following methods are deprecated and retained for backward compatibility:
 * </p>
 * <ul>
 *   <li>{@code plus()}: Use {@link #addAll(Collection)} instead.</li>
 *   <li>{@code minus()}: Use {@link #removeAll(Collection)} instead.</li>
 * </ul>
 *
 * @param <E> the type of elements maintained by this set
 * @see java.util.Set
 * @see CaseInsensitiveMap
 * @see Collections#newSetFromMap(Map)
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
public class CaseInsensitiveSet<E> extends AbstractSet<E> implements Set<E>, Serializable {
    private static final long serialVersionUID = 1L;
    private final CaseInsensitiveMap<E, Boolean> backingMap;
    private final Set<E> delegate;

    /**
     * Constructs an empty {@code CaseInsensitiveSet} backed by a {@link CaseInsensitiveMap} with a default
     * {@link java.util.LinkedHashMap} implementation.
     * <p>
     * This constructor is useful for creating a case-insensitive set with predictable iteration order
     * and default configuration.
     * </p>
     */
    public CaseInsensitiveSet() {
        this.backingMap = new CaseInsensitiveMap<>();
        this.delegate = Collections.newSetFromMap(backingMap);
    }

    /**
     * Constructs a {@code CaseInsensitiveSet} containing the elements of the specified collection.
     * <p>
     * The backing map is chosen based on the type of the input collection:
     * <ul>
     *   <li>If the input collection is a {@code ConcurrentNavigableSetNullSafe}, the backing map is a {@code ConcurrentNavigableMapNullSafe}.</li>
     *   <li>If the input collection is a {@code ConcurrentSkipListSet}, the backing map is a {@code ConcurrentSkipListMap}.</li>
     *   <li>If the input collection is a {@code ConcurrentSet}, the backing map is a {@code ConcurrentHashMapNullSafe}.</li>
     *   <li>If the input collection is a {@code SortedSet}, the backing map is a {@code TreeMap}.</li>
     *   <li>For all other collection types, the backing map is a {@code LinkedHashMap} with an initial capacity based on the size of the input collection.</li>
     * </ul>
     * </p>
     *
     * @param collection the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is {@code null}
     */
    public CaseInsensitiveSet(Collection<? extends E> collection) {
        this.backingMap = determineBackingMap(collection);
        this.delegate = Collections.newSetFromMap(backingMap);
        if (collection != null) {
            addAll(collection);
        }
    }

    /**
     * Constructs a {@code CaseInsensitiveSet} containing the elements of the specified collection,
     * using the provided map as the backing implementation.
     * <p>
     * This constructor allows full control over the underlying map implementation, enabling custom behavior
     * for the set.
     * </p>
     *
     * @param source      the collection whose elements are to be placed into this set
     * @param backingMap  the map to be used as the backing implementation
     * @throws NullPointerException if the specified collection or map is {@code null}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public CaseInsensitiveSet(Collection<? extends E> source, Map backingMap) {
        this.backingMap = new CaseInsensitiveMap<>(Collections.emptyMap(), backingMap);
        this.delegate = Collections.newSetFromMap(this.backingMap);
        if (source != null) {
            addAll(source);
        }
    }

    /**
     * Constructs an empty {@code CaseInsensitiveSet} with the specified initial capacity.
     * <p>
     * This constructor is useful for creating a set with a predefined capacity to reduce resizing overhead
     * during population.
     * </p>
     *
     * @param initialCapacity the initial capacity of the backing map
     * @throws IllegalArgumentException if the specified initial capacity is negative
     */
    public CaseInsensitiveSet(int initialCapacity) {
        this.backingMap = new CaseInsensitiveMap<>(initialCapacity);
        this.delegate = Collections.newSetFromMap(backingMap);
    }

    /**
     * Constructs an empty {@code CaseInsensitiveSet} with the specified initial capacity and load factor.
     * <p>
     * This constructor allows fine-grained control over the performance characteristics of the backing map.
     * </p>
     *
     * @param initialCapacity the initial capacity of the backing map
     * @param loadFactor      the load factor of the backing map, which determines when resizing occurs
     * @throws IllegalArgumentException if the specified initial capacity is negative or if the load factor is
     *         non-positive
     */
    public CaseInsensitiveSet(int initialCapacity, float loadFactor) {
        this.backingMap = new CaseInsensitiveMap<>(initialCapacity, loadFactor);
        this.delegate = Collections.newSetFromMap(backingMap);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For {@link String} elements, the hash code computation is case-insensitive, as it relies on the
     * case-insensitive hash codes provided by the underlying {@link CaseInsensitiveMap}.
     * </p>
     */
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    /**
     * {@inheritDoc}
     * <p>
     * For {@link String} elements, equality is determined in a case-insensitive manner, ensuring that
     * two sets containing equivalent strings with different cases (e.g., "Hello" and "hello") are considered equal.
     * </p>
     *
     * @param other the object to be compared for equality with this set
     * @return {@code true} if the specified object is equal to this set
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Set)) {
            return false;
        }
        Set<?> that = (Set<?>) other;
        return that.size() == size() && containsAll(that);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the number of elements in this set. For {@link String} elements, the count is determined
     * in a case-insensitive manner, ensuring that equivalent strings with different cases (e.g., "Hello" and "hello")
     * are counted as a single element.
     * </p>
     *
     * @return the number of elements in this set
     */
    @Override
    public int size() {
        return delegate.size();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} if this set contains no elements. For {@link String} elements, the check
     * is performed in a case-insensitive manner, ensuring that equivalent strings with different cases
     * are treated as a single element.
     * </p>
     *
     * @return {@code true} if this set contains no elements, {@code false} otherwise
     */
    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} if this set contains the specified element. For {@link String} elements,
     * the check is performed in a case-insensitive manner, meaning that strings differing only by case
     * (e.g., "Hello" and "hello") are considered equal.
     * </p>
     *
     * @param o the element whose presence in this set is to be tested
     * @return {@code true} if this set contains the specified element, {@code false} otherwise
     */
    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns an iterator over the elements in this set. For {@link String} elements, the iterator
     * preserves the original case of the strings, even though the set performs case-insensitive
     * comparisons.
     * </p>
     * <p>
     * When the backing map is a ConcurrentHashMap, the returned iterator is weakly consistent and
     * will not throw {@link java.util.ConcurrentModificationException}. The iterator may reflect
     * updates made during traversal, but is not required to do so.
     * </p>
     *
     * @return an iterator over the elements in this set
     */
    @Override
    public Iterator<E> iterator() {
        return delegate.iterator();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns an array containing all the elements in this set. For {@link String} elements, the array
     * preserves the original case of the strings, even though the set performs case-insensitive
     * comparisons.
     * </p>
     *
     * @return an array containing all the elements in this set
     */
    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns an array containing all the elements in this set. The runtime type of the returned array
     * is that of the specified array. For {@link String} elements, the array preserves the original
     * case of the strings, even though the set performs case-insensitive comparisons.
     * </p>
     *
     * @param a the array into which the elements of the set are to be stored, if it is big enough;
     *          otherwise, a new array of the same runtime type is allocated for this purpose
     * @return an array containing all the elements in this set
     * @throws ArrayStoreException if the runtime type of the specified array is not a supertype of the runtime type
     *         of every element in this set
     * @throws NullPointerException if the specified array is {@code null}
     */
    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Adds the specified element to this set if it is not already present. For {@link String} elements,
     * the addition is case-insensitive, meaning that strings differing only by case (e.g., "Hello" and
     * "hello") are considered equal, and only one instance is added to the set.
     * </p>
     *
     * @param e the element to be added to this set
     * @return {@code true} if this set did not already contain the specified element
     */
    @Override
    public boolean add(E e) {
        return delegate.add(e);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes the specified element from this set if it is present. For {@link String} elements, the
     * removal is case-insensitive, meaning that strings differing only by case (e.g., "Hello" and "hello")
     * are treated as equal, and removing any of them will remove the corresponding entry from the set.
     * </p>
     *
     * @param o the object to be removed from this set, if present
     * @return {@code true} if this set contained the specified element
     */
    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} if this set contains all of the elements in the specified collection. For
     * {@link String} elements, the comparison is case-insensitive, meaning that strings differing only by
     * case (e.g., "Hello" and "hello") are treated as equal.
     * </p>
     *
     * @param c the collection to be checked for containment in this set
     * @return {@code true} if this set contains all of the elements in the specified collection
     * @throws NullPointerException if the specified collection is {@code null}
     */
    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Adds all the elements in the specified collection to this set if they're not already present.
     * For {@link String} elements, the addition is case-insensitive, meaning that strings differing
     * only by case (e.g., "Hello" and "hello") are treated as equal, and only one instance is added
     * to the set.
     * </p>
     *
     * @param c the collection containing elements to be added to this set
     * @return {@code true} if this set changed as a result of the call
     * @throws NullPointerException if the specified collection is {@code null} or contains {@code null} elements
     */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        return delegate.addAll(c);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Retains only the elements in this set that are contained in the specified collection.
     * For {@link String} elements, the comparison is case-insensitive, meaning that strings
     * differing only by case (e.g., "Hello" and "hello") are treated as equal.
     * </p>
     *
     * @param c the collection containing elements to be retained in this set
     * @return {@code true} if this set changed as a result of the call
     * @throws NullPointerException if the specified collection is {@code null}
     */
    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Removes from this set all of its elements that are contained in the specified collection.
     * For {@link String} elements, the removal is case-insensitive, meaning that strings differing
     * only by case (e.g., "Hello" and "hello") are treated as equal, and removing any of them will
     * remove the corresponding entry from the set.
     * </p>
     *
     * @param c the collection containing elements to be removed from this set
     * @return {@code true} if this set changed as a result of the call
     * @throws NullPointerException if the specified collection is {@code null}
     */
    @Override
    public boolean removeAll(Collection<?> c) {
        // We need to handle this specially because the default implementation
        // may use c.contains() which would be case-sensitive if c is not a CaseInsensitiveSet
        boolean modified = false;
        for (Object elem : c) {
            if (remove(elem)) {
                modified = true;
            }
        }
        return modified;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Removes all elements from this set. After this call, the set will be empty.
     * For {@link String} elements, the case-insensitive behavior of the set has no impact
     * on the clearing operation.
     * </p>
     */
    @Override
    public void clear() {
        delegate.clear();
    }

    /**
     * Creates a {@link Spliterator} over the elements in this set.
     * <p>
     * The spliterator reports {@link Spliterator#DISTINCT}. The spliterator's comparator
     * is {@code null} if the set's comparator is {@code null}. Otherwise, the spliterator's
     * comparator is the same as or imposes the same total ordering as the set's comparator.
     * </p>
     *
     * @return a {@code Spliterator} over the elements in this set
     * @since 1.8
     */
    @Override
    public Spliterator<E> spliterator() {
        return delegate.spliterator();
    }

    /**
     * Removes all of the elements of this collection that satisfy the given predicate.
     * <p>
     * Errors or runtime exceptions thrown during iteration or by the predicate are relayed
     * to the caller. For {@link String} elements, the removal is case-insensitive.
     * </p>
     *
     * @param filter a predicate which returns {@code true} for elements to be removed
     * @return {@code true} if any elements were removed
     * @throws NullPointerException if the specified filter is null
     * @since 1.8
     */
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return delegate.removeIf(filter);
    }

    /**
     * Performs the given action for each element of the set until all elements have been
     * processed or the action throws an exception.
     * <p>
     * Actions are performed in the order of iteration (if an iteration order is specified).
     * Exceptions thrown by the action are relayed to the caller.
     * </p>
     *
     * @param action The action to be performed for each element
     * @throws NullPointerException if the specified action is null
     * @since 1.8
     */
    @Override
    public void forEach(Consumer<? super E> action) {
        delegate.forEach(action);
    }

    /* ----------------------------------------------------------------- */
    /*           Concurrent Operations (when backed by ConcurrentMap)    */
    /* ----------------------------------------------------------------- */

    /**
     * Returns an estimate of the number of elements in this set when backed by a ConcurrentHashMap.
     * This method provides better performance and handles large sets (size > Integer.MAX_VALUE).
     * <p>
     * When the backing map is not a ConcurrentHashMap, this method delegates to {@link #size()}.
     * The estimate may not reflect recent additions or removals due to concurrent modifications.
     * </p>
     * 
     * @return the estimated number of elements in this set
     * @since 3.6.0
     */
    public long elementCount() {
        return backingMap.mappingCount();
    }

    /**
     * Performs the given action for each element in this set, with operations potentially 
     * performed in parallel when the parallelism threshold is met and the set is backed 
     * by a ConcurrentHashMap.
     * <p>
     * This method provides high-performance parallel iteration over set elements when using 
     * concurrent backing maps. The parallelism threshold determines the minimum set size 
     * required to enable parallel processing.
     * </p>
     * 
     * @param parallelismThreshold the threshold for parallel execution (typically use 1 for parallel, 
     *                           Long.MAX_VALUE for sequential)
     * @param action the action to be performed for each element
     * @throws NullPointerException if the specified action is null
     * @since 3.6.0
     */
    public void forEach(long parallelismThreshold, Consumer<? super E> action) {
        backingMap.forEachKey(parallelismThreshold, action);
    }

    /**
     * Returns a non-null result from applying the given search function to each element 
     * in this set, or null if none are found. The search may be performed in parallel 
     * when the parallelism threshold is met and the set is backed by a ConcurrentHashMap.
     * <p>
     * This method provides high-performance parallel search over set elements when using 
     * concurrent backing maps. The search terminates early upon finding the first non-null result.
     * </p>
     * 
     * @param <U> the type of the search result
     * @param parallelismThreshold the threshold for parallel execution (typically use 1 for parallel,
     *                           Long.MAX_VALUE for sequential)
     * @param searchFunction the function to apply to each element
     * @return a non-null result from applying the search function, or null if none found
     * @throws NullPointerException if the specified search function is null
     * @since 3.6.0
     */
    public <U> U searchElements(long parallelismThreshold, Function<? super E, ? extends U> searchFunction) {
        return backingMap.searchKeys(parallelismThreshold, searchFunction);
    }

    /**
     * Returns the result of accumulating all elements in this set using the given reducer 
     * and transformer functions. The reduction may be performed in parallel when the 
     * parallelism threshold is met and the set is backed by a ConcurrentHashMap.
     * <p>
     * This method provides high-performance parallel reduction over set elements when using 
     * concurrent backing maps. The transformer is applied to each element before reduction.
     * </p>
     * 
     * @param <U> the type of the transformed elements and the result
     * @param parallelismThreshold the threshold for parallel execution (typically use 1 for parallel,
     *                           Long.MAX_VALUE for sequential)
     * @param transformer the function to transform each element before reduction
     * @param reducer the function to combine transformed elements
     * @return the result of the reduction, or null if the set is empty
     * @throws NullPointerException if the specified transformer or reducer is null
     * @since 3.6.0
     */
    public <U> U reduceElements(long parallelismThreshold, 
                               Function<? super E, ? extends U> transformer,
                               BiFunction<? super U, ? super U, ? extends U> reducer) {
        return backingMap.reduceKeys(parallelismThreshold, transformer, reducer);
    }

    /**
     * Returns the underlying map used to implement this set.
     * <p>
     * This method provides access to the backing {@link CaseInsensitiveMap} implementation,
     * allowing advanced operations and inspections. The returned map maintains the same
     * case-insensitive semantics as this set.
     * </p>
     * <p>
     * <strong>Warning:</strong> Modifying the returned map directly may affect this set's state.
     * Use with caution and prefer the set's public methods when possible.
     * </p>
     * 
     * @return the backing map implementation
     * @since 3.6.0
     */
    @SuppressWarnings("unchecked")
    public Map<E, Object> getBackingMap() {
        // Cast is safe because Boolean extends Object
        return (Map<E, Object>) (Map<?, ?>) backingMap;
    }

    /**
     * Determines the appropriate backing map based on the source collection's type.
     * This method creates a CaseInsensitiveMap with the appropriate underlying map implementation
     * to preserve the characteristics of the source collection.
     *
     * @param source the source collection to copy from
     * @return a new CaseInsensitiveMap instance with appropriate backing map
     */
    private CaseInsensitiveMap<E, Boolean> determineBackingMap(Collection<? extends E> source) {
        if (source == null) {
            return new CaseInsensitiveMap<>();
        }
        
        // Create the appropriate backing map based on source type
        if (source instanceof ConcurrentNavigableSetNullSafe) {
            return new CaseInsensitiveMap<>(Collections.emptyMap(), new ConcurrentNavigableMapNullSafe<>());
        } else if (source instanceof ConcurrentSkipListSet) {
            return new CaseInsensitiveMap<>(Collections.emptyMap(), new ConcurrentSkipListMap<>());
        } else if (source instanceof ConcurrentSet) {
            return new CaseInsensitiveMap<>(Collections.emptyMap(), new ConcurrentHashMapNullSafe<>());
        } else if (source instanceof SortedSet) {
            return new CaseInsensitiveMap<>(Collections.emptyMap(), new TreeMap<>());
        } else {
            // For all other collection types, use LinkedHashMap
            int size = source.isEmpty() ? 16 : source.size();
            return new CaseInsensitiveMap<>(size);
        }
    }

    /* ----------------------------------------------------------------- */
    /*                      Deprecated Methods                           */
    /* ----------------------------------------------------------------- */

    /**
     * Removes all elements in the specified collection from this set.
     * <p>
     * This method is deprecated. Use {@link #removeAll(Collection)} instead.
     * </p>
     *
     * @param removeMe the collection of elements to remove
     * @return this set (for method chaining)
     * @deprecated Use {@link #removeAll(Collection)} instead
     */
    @Deprecated
    public Set<E> minus(Iterable<E> removeMe) {
        for (Object me : removeMe) {
            remove(me);
        }
        return this;
    }

    /**
     * Removes the specified element from this set.
     * <p>
     * This method is deprecated. Use {@link #remove(Object)} instead.
     * </p>
     *
     * @param removeMe the element to remove
     * @return this set (for method chaining)
     * @deprecated Use {@link #remove(Object)} instead
     */
    @Deprecated
    public Set<E> minus(E removeMe) {
        remove(removeMe);
        return this;
    }

    /**
     * Adds all elements in the specified collection to this set.
     * <p>
     * This method is deprecated. Use {@link #addAll(Collection)} instead.
     * </p>
     *
     * @param right the collection of elements to add
     * @return this set (for method chaining)
     * @deprecated Use {@link #addAll(Collection)} instead
     */
    @Deprecated
    public Set<E> plus(Iterable<E> right) {
        for (E item : right) {
            add(item);
        }
        return this;
    }

    /**
     * Adds the specified element to this set.
     * <p>
     * This method is deprecated. Use {@link #add(Object)} instead.
     * </p>
     *
     * @param right the element to add
     * @return this set (for method chaining)
     * @deprecated Use {@link #add(Object)} instead
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public Set<E> plus(Object right) {
        add((E) right);
        return this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a string representation of this set. The string representation consists of a list of
     * the set's elements in their original case, enclosed in square brackets ({@code "[]"}). For
     * {@link String} elements, the original case is preserved, even though the set performs
     * case-insensitive comparisons.
     * </p>
     *
     * <p>
     * The order of elements in the string representation matches the iteration order of the backing map.
     * </p>
     *
     * @return a string representation of this set
     */
    @Override
    public String toString() {
        return delegate.toString();
    }
}