package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

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
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Create a case-insensitive set
 * CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
 * set.add("Hello");
 * set.add("HELLO"); // No effect, as "Hello" already exists
 * System.out.println(set); // Outputs: [Hello]
 *
 * // Mixed types in the set
 * CaseInsensitiveSet<Object> mixedSet = new CaseInsensitiveSet<>();
 * mixedSet.add("Apple");
 * mixedSet.add(123);
 * mixedSet.add("apple"); // No effect, as "Apple" already exists
 * System.out.println(mixedSet); // Outputs: [Apple, 123]
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
 * <h2>Deprecated Methods</h2>
 * <p>
 * The following methods are deprecated and retained for backward compatibility:
 * </p>
 * <ul>
 *   <li>{@code plus()}: Use {@link #addAll(Collection)} instead.</li>
 *   <li>{@code minus()}: Use {@link #removeAll(Collection)} instead.</li>
 * </ul>
 *
 * <h2>Additional Notes</h2>
 * <ul>
 *   <li>String comparisons are case-insensitive but preserve original case for iteration and output.</li>
 *   <li>Thread safety depends on the thread safety of the chosen backing map.</li>
 *   <li>Set operations like {@code contains()}, {@code add()}, and {@code remove()} rely on the
 *       behavior of the underlying {@link CaseInsensitiveMap}.</li>
 * </ul>
 *
 * @param <E> the type of elements maintained by this set
 * @see java.util.Set
 * @see CaseInsensitiveMap
 * @see java.util.LinkedHashMap
 * @see java.util.TreeMap
 * @see java.util.concurrent.ConcurrentSkipListSet
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
public class CaseInsensitiveSet<E> implements Set<E> {
    private final Map<E, Object> map;
    private static final Object PRESENT = new Object();

    /**
     * Constructs an empty {@code CaseInsensitiveSet} backed by a {@link CaseInsensitiveMap} with a default
     * {@link java.util.LinkedHashMap} implementation.
     * <p>
     * This constructor is useful for creating a case-insensitive set with predictable iteration order
     * and default configuration.
     * </p>
     */
    public CaseInsensitiveSet() {
        map = new CaseInsensitiveMap<>();
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
        if (collection instanceof ConcurrentNavigableSetNullSafe) {
            map = new CaseInsensitiveMap<>(new ConcurrentNavigableMapNullSafe<>());
        } else if (collection instanceof ConcurrentSkipListSet) {
            map = new CaseInsensitiveMap<>(new ConcurrentSkipListMap<>());
        } else if (collection instanceof ConcurrentSet) {
            map = new CaseInsensitiveMap<>(new ConcurrentHashMapNullSafe<>());
        } else if (collection instanceof SortedSet) {
            map = new CaseInsensitiveMap<>(new TreeMap<>());  // covers SortedSet or NavigableSet
        } else {
            map = new CaseInsensitiveMap<>(collection.size());
        }
        addAll(collection);
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
        map = backingMap;
        addAll(source);
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
        map = new CaseInsensitiveMap<>(initialCapacity);
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
        map = new CaseInsensitiveMap<>(initialCapacity, loadFactor);
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
        return map.keySet().hashCode();
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
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Set)) {
            return false;
        }

        Set<E> that = (Set<E>) other;
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
        return map.size();
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
        return map.isEmpty();
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
        return map.containsKey(o);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns an iterator over the elements in this set. For {@link String} elements, the iterator
     * preserves the original case of the strings, even though the set performs case-insensitive
     * comparisons.
     * </p>
     *
     * @return an iterator over the elements in this set
     */
    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
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
        return map.keySet().toArray();
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
        return map.keySet().toArray(a);
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
        return map.putIfAbsent(e, PRESENT) == null;
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
        return map.remove(o) != null;
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
        for (Object o : c) {
            if (!map.containsKey(o)) {
                return false;
            }
        }
        return true;
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
        boolean modified = false;
        for (E elem : c) {
            if (add(elem)) {  // Reuse the efficient add() method
                modified = true;
            }
        }
        return modified;
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
        Map<E, Object> other = new CaseInsensitiveMap<>();
        for (Object o : c) {
            @SuppressWarnings("unchecked")
            E element = (E) o; // Safe cast because Map allows adding any type
            other.put(element, PRESENT);
        }

        Iterator<E> iterator = map.keySet().iterator();
        boolean modified = false;
        while (iterator.hasNext()) {
            E elem = iterator.next();
            if (!other.containsKey(elem)) {
                iterator.remove();
                modified = true;
            }
        }
        return modified;
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
        boolean modified = false;
        for (Object elem : c) {
            @SuppressWarnings("unchecked")
            E element = (E) elem; // Cast to E since map keys match the generic type
            if (map.remove(element) != null) {
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
        map.clear();
    }

    @Deprecated
    public Set<E> minus(Iterable<E> removeMe) {
        for (Object me : removeMe) {
            remove(me);
        }
        return this;
    }

    @Deprecated
    public Set<E> minus(E removeMe) {
        remove(removeMe);
        return this;
    }

    @Deprecated
    public Set<E> plus(Iterable<E> right) {
        for (E item : right) {
            add(item);
        }
        return this;
    }

    @Deprecated
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
        return map.keySet().toString();
    }
}
