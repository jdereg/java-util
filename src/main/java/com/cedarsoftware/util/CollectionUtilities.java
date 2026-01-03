package com.cedarsoftware.util;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.cedarsoftware.util.convert.CollectionsWrappers;

/**
 * A utility class providing enhanced operations for working with Java collections.
 * <p>
 * {@code CollectionUtilities} simplifies tasks such as null-safe checks, retrieving collection sizes,
 * creating immutable collections, and wrapping collections in checked, synchronized, or unmodifiable views.
 * It includes functionality compatible with JDK 8, providing alternatives to methods introduced in later
 * versions of Java, such as {@link java.util.List#of(Object...)} and {@link java.util.Set#of(Object...)}.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Null-Safe Checks:</b>
 *       <ul>
 *         <li>{@link #isEmpty(Collection)}: Checks if a collection is null or empty.</li>
 *         <li>{@link #hasContent(Collection)}: Checks if a collection is not null and contains at least one element.</li>
 *         <li>{@link #size(Collection)}: Safely retrieves the size of a collection, returning {@code 0} if it is null.</li>
 *       </ul>
 *   </li>
 *   <li><b>Immutable Collection Creation:</b>
 *       <ul>
 *         <li>{@link #listOf(Object...)}: Creates an immutable list of specified elements, compatible with JDK 8.</li>
 *         <li>{@link #setOf(Object...)}: Creates an immutable set of specified elements, compatible with JDK 8.</li>
 *       </ul>
 *   </li>
 *   <li><b>Collection Wrappers:</b>
 *       <ul>
 *         <li>{@link #getUnmodifiableCollection(Collection)}: Wraps a collection in the most specific
 *             unmodifiable view based on its type (e.g., {@link NavigableSet}, {@link SortedSet}, {@link List}).</li>
 *         <li>{@link #getCheckedCollection(Collection, Class)}: Wraps a collection in the most specific
 *             type-safe checked view based on its type (e.g., {@link NavigableSet}, {@link SortedSet}, {@link List}).</li>
 *         <li>{@link #getSynchronizedCollection(Collection)}: Wraps a collection in the most specific
 *             thread-safe synchronized view based on its type (e.g., {@link NavigableSet}, {@link SortedSet}, {@link List}).</li>
 *         <li>{@link #getEmptyCollection(Collection)}: Returns an empty collection of the same type as the input
 *             collection (e.g., {@link NavigableSet}, {@link SortedSet}, {@link List}).</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Null-safe checks
 * boolean isEmpty = CollectionUtilities.isEmpty(myCollection);
 * boolean hasContent = CollectionUtilities.hasContent(myCollection);
 * int size = CollectionUtilities.size(myCollection);
 *
 * // Immutable collections
 * List<String> list = CollectionUtilities.listOf("A", "B", "C");
 * Set<String> set = CollectionUtilities.setOf("X", "Y", "Z");
 *
 * // Collection wrappers
 * Collection<?> unmodifiable = CollectionUtilities.getUnmodifiableCollection(myCollection);
 * Collection<?> checked = CollectionUtilities.getCheckedCollection(myCollection, String.class);
 * Collection<?> synchronizedCollection = CollectionUtilities.getSynchronizedCollection(myCollection);
 * Collection<?> empty = CollectionUtilities.getEmptyCollection(myCollection);
 * }</pre>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>This class is designed as a static utility class and should not be instantiated.</li>
 *   <li>It uses unmodifiable empty collections as constants to optimize memory usage and prevent unnecessary object creation.</li>
 *   <li>The collection wrappers apply type-specific operations based on the runtime type of the provided collection.</li>
 * </ul>
 *
 * @see java.util.Collection
 * @see java.util.List
 * @see java.util.Set
 * @see Collections
 * @see Collections#unmodifiableCollection(Collection)
 * @see Collections#checkedCollection(Collection, Class)
 * @see Collections#synchronizedCollection(Collection)
 * @see Collections#emptyList()
 * @see Collections#emptySet()
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
public class CollectionUtilities {

    private static final Set<?> unmodifiableEmptySet = Collections.emptySet();
    private static final List<?> unmodifiableEmptyList = Collections.emptyList();
    private static final Class<?> unmodifiableCollectionClass = CollectionsWrappers.getUnmodifiableCollectionClass();
    private static final Class<?> synchronizedCollectionClass = CollectionsWrappers.getSynchronizedCollectionClass();

    private CollectionUtilities() { }

    /**
     * This is a null-safe isEmpty check.
     *
     * @param col the collection to check, may be {@code null}
     * @return {@code true} if the collection is {@code null} or empty; {@code false} otherwise
     */
    public static boolean isEmpty(Collection<?> col) {
        return col == null || col.isEmpty();
    }

    /**
     * Checks if the specified collection is not {@code null} and contains at least one element.
     * <p>
     * This method provides a null-safe way to verify that a collection has content, returning {@code false}
     * if the collection is {@code null} or empty.
     * </p>
     *
     * @param col the collection to check, may be {@code null}
     * @return {@code true} if the collection is not {@code null} and contains at least one element;
     *         {@code false} otherwise
     */
    public static boolean hasContent(Collection<?> col) {
        return col != null && !col.isEmpty();
    }

    /**
     * Returns the size of the specified collection in a null-safe manner.
     * <p>
     * If the collection is {@code null}, this method returns {@code 0}. Otherwise, it returns the
     * number of elements in the collection.
     * </p>
     *
     * @param col the collection to check, may be {@code null}
     * @return the size of the collection, or {@code 0} if the collection is {@code null}
     */
    public static int size(Collection<?> col) {
        return col == null ? 0 : col.size();
    }

    /**
     * Creates an unmodifiable list containing the specified elements.
     * <p>
     * This method provides functionality similar to {@link java.util.List#of(Object...)} introduced in JDK 9,
     * but is compatible with JDK 8. If the input array is {@code null} or empty, this method returns
     * an unmodifiable empty list.
     * </p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * List<String> list = listOf("A", "B", "C"); // Returns an unmodifiable list containing "A", "B", "C"
     * List<String> emptyList = listOf();         // Returns an unmodifiable empty list
     * }</pre>
     *
     * @param <T> the type of elements in the list
     * @param items the elements to be included in the list; may be {@code null}
     * @return an unmodifiable list containing the specified elements, or an unmodifiable empty list if the input is {@code null} or empty
     * @throws NullPointerException if any of the elements in the input array are {@code null}
     * @see Collections#unmodifiableList(List)
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> List<T> listOf(T... items) {
        if (items == null || items.length == 0) {
            return (List<T>) unmodifiableEmptyList;
        }
        // Pre-size the ArrayList to avoid resizing and avoid Collections.addAll() overhead
        List<T> list = new ArrayList<>(items.length);
        for (T item : items) {
            list.add(item); // This will throw NPE if item is null, as documented
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * Creates an unmodifiable set containing the specified elements.
     * <p>
     * This method provides functionality similar to {@link java.util.Set#of(Object...)} introduced in JDK 9,
     * but is compatible with JDK 8. If the input array is {@code null} or empty, this method returns
     * an unmodifiable empty set.
     * </p>
     *
     * <h2>Usage Example</h2>
     * <pre>{@code
     * Set<String> set = setOf("A", "B", "C"); // Returns an unmodifiable set containing "A", "B", "C"
     * Set<String> emptySet = setOf();         // Returns an unmodifiable empty set
     * }</pre>
     *
     * @param <T> the type of elements in the set
     * @param items the elements to be included in the set; may be {@code null}
     * @return an unmodifiable set containing the specified elements, or an unmodifiable empty set if the input is {@code null} or empty
     * @throws NullPointerException if any of the elements in the input array are {@code null}
     * @see Collections#unmodifiableSet(Set)
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public static <T> Set<T> setOf(T... items) {
        if (items == null || items.length == 0) {
            return (Set<T>) unmodifiableEmptySet;
        }
        // Pre-size the LinkedHashSet to avoid resizing and avoid Collections.addAll() overhead
        Set<T> set = new LinkedHashSet<>(items.length);
        for (T item : items) {
            set.add(item); // This will throw NPE if item is null, as documented
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * Determines whether the specified class represents an unmodifiable collection type.
     * <p>
     * This method checks if the provided {@code targetType} is assignable to the class of
     * unmodifiable collections. It is commonly used to identify whether a given class type
     * indicates a collection that cannot be modified (e.g., collections wrapped with
     * {@link Collections#unmodifiableCollection(Collection)} or its specialized variants).
     * </p>
     *
     * <p><strong>Null Handling:</strong> If {@code targetType} is {@code null}, this method
     * will throw a {@link NullPointerException} with a clear error message.</p>
     *
     * @param targetType the {@link Class} to check, must not be {@code null}
     * @return {@code true} if the specified {@code targetType} indicates an unmodifiable collection;
     *         {@code false} otherwise
     * @throws NullPointerException if {@code targetType} is {@code null}
     * @see Collections#unmodifiableCollection(Collection)
     * @see Collections#unmodifiableList(List)
     * @see Collections#unmodifiableSet(Set)
     */
    public static boolean isUnmodifiable(Class<?> targetType) {
        Objects.requireNonNull(targetType, "targetType (Class) cannot be null");
        return unmodifiableCollectionClass.isAssignableFrom(targetType);
    }

    /**
     * Determines whether the specified class represents a synchronized collection type.
     * <p>
     * This method checks if the provided {@code targetType} is assignable to the class of
     * synchronized collections. It is commonly used to identify whether a given class type
     * indicates a collection that supports concurrent access (e.g., collections wrapped with
     * {@link Collections#synchronizedCollection(Collection)} or its specialized variants).
     * </p>
     *
     * <p><strong>Null Handling:</strong> If {@code targetType} is {@code null}, this method
     * will throw a {@link NullPointerException} with a clear error message.</p>
     *
     * @param targetType the {@link Class} to check, must not be {@code null}
     * @return {@code true} if the specified {@code targetType} indicates a synchronized collection;
     *         {@code false} otherwise
     * @throws NullPointerException if {@code targetType} is {@code null}
     * @see Collections#synchronizedCollection(Collection)
     * @see Collections#synchronizedList(List)
     * @see Collections#synchronizedSet(Set)
     */
    public static boolean isSynchronized(Class<?> targetType) {
        Objects.requireNonNull(targetType, "targetType (Class) cannot be null");
        return synchronizedCollectionClass.isAssignableFrom(targetType);
    }

    /**
     * Wraps the provided collection in an unmodifiable wrapper appropriate to its runtime type.
     * <p>
     * This method ensures that the collection cannot be modified by any client code and applies the
     * most specific unmodifiable wrapper based on the runtime type of the provided collection:
     * </p>
     * <ul>
     *     <li>If the collection is a {@link NavigableSet}, it is wrapped using
     *     {@link Collections#unmodifiableNavigableSet(NavigableSet)}.</li>
     *     <li>If the collection is a {@link SortedSet}, it is wrapped using
     *     {@link Collections#unmodifiableSortedSet(SortedSet)}.</li>
     *     <li>If the collection is a {@link Set}, it is wrapped using
     *     {@link Collections#unmodifiableSet(Set)}.</li>
     *     <li>If the collection is a {@link List}, it is wrapped using
     *     {@link Collections#unmodifiableList(List)}.</li>
     *     <li>Otherwise, it is wrapped using {@link Collections#unmodifiableCollection(Collection)}.</li>
     * </ul>
     *
     * <p>
     * Attempting to modify the returned collection will result in an
     * {@link UnsupportedOperationException} at runtime. For example:
     * </p>
     * <pre>{@code
     * NavigableSet<String> set = new TreeSet<>(Set.of("A", "B", "C"));
     * NavigableSet<String> unmodifiableSet = (NavigableSet<String>) getUnmodifiableCollection(set);
     * unmodifiableSet.add("D"); // Throws UnsupportedOperationException
     * }</pre>
     *
     * <h2>Null Handling</h2>
     * <p>
     * If the input collection is {@code null}, this method will throw a {@link NullPointerException}
     * with a descriptive error message.
     * </p>
     *
     * @param <T> the type of elements in the collection
     * @param collection the collection to be wrapped in an unmodifiable wrapper
     * @return an unmodifiable view of the provided collection, preserving its runtime type
     * @throws NullPointerException if the provided collection is {@code null}
     * @see Collections#unmodifiableNavigableSet(NavigableSet)
     * @see Collections#unmodifiableSortedSet(SortedSet)
     * @see Collections#unmodifiableSet(Set)
     * @see Collections#unmodifiableList(List)
     * @see Collections#unmodifiableCollection(Collection)
     */
    public static <T> Collection<T> getUnmodifiableCollection(Collection<T> collection) {
        Objects.requireNonNull(collection, "Collection must not be null");

        if (collection instanceof NavigableSet) {
            return Collections.unmodifiableNavigableSet((NavigableSet<T>) collection);
        } else if (collection instanceof SortedSet) {
            return Collections.unmodifiableSortedSet((SortedSet<T>) collection);
        } else if (collection instanceof Set) {
            return Collections.unmodifiableSet((Set<T>) collection);
        } else if (collection instanceof List) {
            return Collections.unmodifiableList((List<T>) collection);
        } else {
            return Collections.unmodifiableCollection(collection);
        }
    }

    /**
     * Returns an empty collection of the same type as the provided collection.
     * <p>
     * This method determines the runtime type of the input collection and returns an
     * appropriate empty collection instance:
     * </p>
     * <ul>
     *     <li>If the collection is a {@link NavigableSet}, it returns {@link Collections#emptyNavigableSet()}.</li>
     *     <li>If the collection is a {@link SortedSet}, it returns {@link Collections#emptySortedSet()}.</li>
     *     <li>If the collection is a {@link Set}, it returns {@link Collections#emptySet()}.</li>
     *     <li>If the collection is a {@link List}, it returns {@link Collections#emptyList()}.</li>
     *     <li>For all other collection types, it defaults to returning {@link Collections#emptySet()}.</li>
     * </ul>
     *
     * <p>
     * The returned collection is immutable and will throw an {@link UnsupportedOperationException}
     * if any modification is attempted. For example:
     * </p>
     * <pre>{@code
     * List<String> list = new ArrayList<>();
     * Collection<String> emptyList = getEmptyCollection(list);
     *
     * emptyList.add("one"); // Throws UnsupportedOperationException
     * }</pre>
     *
     * <h2>Null Handling</h2>
     * <p>
     * If the input collection is {@code null}, this method will throw a {@link NullPointerException}
     * with a descriptive error message.
     * </p>
     *
     * <h2>Usage Notes</h2>
     * <ul>
     *     <li>The returned collection is type-specific based on the input collection, ensuring
     *     compatibility with type-specific operations such as iteration or ordering.</li>
     *     <li>The method provides an empty collection that is appropriate for APIs requiring
     *     non-null collections as inputs or defaults.</li>
     * </ul>
     *
     * @param <T> the type of elements in the collection
     * @param collection the collection whose type determines the type of the returned empty collection
     * @return an empty, immutable collection of the same type as the input collection
     * @throws NullPointerException if the provided collection is {@code null}
     * @see Collections#emptyNavigableSet()
     * @see Collections#emptySortedSet()
     * @see Collections#emptySet()
     * @see Collections#emptyList()
     */
    public static <T> Collection<T> getEmptyCollection(Collection<T> collection) {
        Objects.requireNonNull(collection, "Collection must not be null");

        if (collection instanceof NavigableSet) {
            return Collections.emptyNavigableSet();
        } else if (collection instanceof SortedSet) {
            return Collections.emptySortedSet();
        } else if (collection instanceof Set) {
            return Collections.emptySet();
        } else if (collection instanceof List) {
            return Collections.emptyList();
        } else {
            return Collections.emptySet(); // More neutral default than emptyList() for unknown collection types
        }
    }

    /**
     * Wraps the provided collection in a checked wrapper that enforces type safety.
     * <p>
     * This method applies the most specific checked wrapper based on the runtime type of the collection:
     * </p>
     * <ul>
     *     <li>If the collection is a {@link NavigableSet}, it is wrapped using
     *     {@link Collections#checkedNavigableSet(NavigableSet, Class)}.</li>
     *     <li>If the collection is a {@link SortedSet}, it is wrapped using
     *     {@link Collections#checkedSortedSet(SortedSet, Class)}.</li>
     *     <li>If the collection is a {@link Set}, it is wrapped using
     *     {@link Collections#checkedSet(Set, Class)}.</li>
     *     <li>If the collection is a {@link List}, it is wrapped using
     *     {@link Collections#checkedList(List, Class)}.</li>
     *     <li>Otherwise, it is wrapped using {@link Collections#checkedCollection(Collection, Class)}.</li>
     * </ul>
     *
     * <p>
     * Attempting to add an element to the returned collection that is not of the specified type
     * will result in a {@link ClassCastException} at runtime. For example:
     * </p>
     * <pre>{@code
     * List<Object> list = new ArrayList<>(Arrays.asList("one", "two"));
     * Collection<String> checkedCollection = getCheckedCollection(list, String.class);
     *
     * // Adding a String is allowed
     * checkedCollection.add("three");
     *
     * // Adding an Integer will throw a ClassCastException
     * checkedCollection.add(42); // Throws ClassCastException
     * }</pre>
     *
     * <h2>Null Handling</h2>
     * <p>
     * If the input collection or the type class is {@code null}, this method will throw a
     * {@link NullPointerException} with a descriptive error message.
     * </p>
     *
     * <h2>Usage Notes</h2>
     * <ul>
     *     <li>The method enforces runtime type safety by validating all elements added to the collection.</li>
     *     <li>The returned collection retains the original type-specific behavior of the input collection
     *     (e.g., sorting for {@link SortedSet} or ordering for {@link List}).</li>
     *     <li>Use this method when you need to ensure that a collection only contains elements of a specific type.</li>
     * </ul>
     *
     * @param <T> the type of the input collection
     * @param <E> the type of elements in the collection
     * @param collection the collection to be wrapped, must not be {@code null}
     * @param type the class of elements that the collection is permitted to hold, must not be {@code null}
     * @return a checked view of the provided collection
     * @throws NullPointerException if the provided collection or type is {@code null}
     * @see Collections#checkedNavigableSet(NavigableSet, Class)
     * @see Collections#checkedSortedSet(SortedSet, Class)
     * @see Collections#checkedSet(Set, Class)
     * @see Collections#checkedList(List, Class)
     * @see Collections#checkedCollection(Collection, Class)
     */
    @SuppressWarnings("unchecked")
    public static <T extends Collection<?>, E> Collection<E> getCheckedCollection(T collection, Class<E> type) {
        Objects.requireNonNull(collection, "Collection must not be null");
        Objects.requireNonNull(type, "Type (Class) must not be null");

        if (collection instanceof NavigableSet) {
            return Collections.checkedNavigableSet((NavigableSet<E>) collection, type);
        } else if (collection instanceof SortedSet) {
            return Collections.checkedSortedSet((SortedSet<E>) collection, type);
        } else if (collection instanceof Set) {
            return Collections.checkedSet((Set<E>) collection, type);
        } else if (collection instanceof List) {
            return Collections.checkedList((List<E>) collection, type);
        } else {
            return Collections.checkedCollection((Collection<E>) collection, type);
        }
    }

    /**
     * Wraps the provided collection in a thread-safe synchronized wrapper.
     * <p>
     * This method applies the most specific synchronized wrapper based on the runtime type of the collection:
     * </p>
     * <ul>
     *     <li>If the collection is a {@link NavigableSet}, it is wrapped using
     *     {@link Collections#synchronizedNavigableSet(NavigableSet)}.</li>
     *     <li>If the collection is a {@link SortedSet}, it is wrapped using
     *     {@link Collections#synchronizedSortedSet(SortedSet)}.</li>
     *     <li>If the collection is a {@link Set}, it is wrapped using
     *     {@link Collections#synchronizedSet(Set)}.</li>
     *     <li>If the collection is a {@link List}, it is wrapped using
     *     {@link Collections#synchronizedList(List)}.</li>
     *     <li>Otherwise, it is wrapped using {@link Collections#synchronizedCollection(Collection)}.</li>
     * </ul>
     *
     * <p>
     * The returned collection is thread-safe. However, iteration over the collection must be manually synchronized:
     * </p>
     * <pre>{@code
     * List<String> list = new ArrayList<>(Arrays.asList("one", "two", "three"));
     * Collection<String> synchronizedList = getSynchronizedCollection(list);
     *
     * synchronized (synchronizedList) {
     *     for (String item : synchronizedList) {
     *         LOG.info(item);
     *     }
     * }
     * }</pre>
     *
     * <h2>Null Handling</h2>
     * <p>
     * If the input collection is {@code null}, this method will throw a {@link NullPointerException}
     * with a descriptive error message.
     * </p>
     *
     * <h2>Usage Notes</h2>
     * <ul>
     *     <li>The method returns a synchronized wrapper that delegates all operations to the original collection.</li>
     *     <li>Any structural modifications (e.g., {@code add}, {@code remove}) must occur within a synchronized block
     *     to ensure thread safety during concurrent access.</li>
     * </ul>
     *
     * @param <T> the type of elements in the collection
     * @param collection the collection to be wrapped in a synchronized wrapper
     * @return a synchronized view of the provided collection, preserving its runtime type
     * @throws NullPointerException if the provided collection is {@code null}
     * @see Collections#synchronizedNavigableSet(NavigableSet)
     * @see Collections#synchronizedSortedSet(SortedSet)
     * @see Collections#synchronizedSet(Set)
     * @see Collections#synchronizedList(List)
     * @see Collections#synchronizedCollection(Collection)
     */
    public static <T> Collection<T> getSynchronizedCollection(Collection<T> collection) {
        Objects.requireNonNull(collection, "Collection must not be null");

        if (collection instanceof NavigableSet) {
            return Collections.synchronizedNavigableSet((NavigableSet<T>) collection);
        } else if (collection instanceof SortedSet) {
            return Collections.synchronizedSortedSet((SortedSet<T>) collection);
        } else if (collection instanceof Set) {
            return Collections.synchronizedSet((Set<T>) collection);
        } else if (collection instanceof List) {
            return Collections.synchronizedList((List<T>) collection);
        } else {
            return Collections.synchronizedCollection(collection);
        }
    }

    /**
     * Creates a deep copy of all container structures (arrays and collections) while preserving
     * references to non-container objects. This method deep copies all arrays and collections
     * to any depth (iterative traversal), but keeps the same references for all other objects (the "berries").
     * 
     * <p>Maps are treated as berries (non-containers) and are not deep copied.</p>
     * 
     * <p>This method handles:
     * <ul>
     *   <li>Arrays of any type (primitive and object arrays)</li>
     *   <li>Collections (Lists, Sets, Queues, etc.)</li>
     *   <li>Nested combinations of arrays and collections to any depth</li>
     *   <li>Circular references (maintains the circular structure in the copy)</li>
     * </ul>
     * </p>
     * 
     * <p>Collection type preservation:
     * <ul>
     *   <li>EnumSet → EnumSet (preserves enum type)</li>
     *   <li>Deque → LinkedList (preserves deque operations, supports nulls)</li>
     *   <li>PriorityQueue → PriorityQueue (preserves comparator and heap semantics)</li>
     *   <li>SortedSet → TreeSet (preserves comparator and sorting)</li>
     *   <li>Set → LinkedHashSet (preserves insertion order)</li>
     *   <li>List → ArrayList (optimized for random access)</li>
     *   <li>Other Queue types → LinkedList (preserves queue operations)</li>
     *   <li>Other Collections → ArrayList (fallback)</li>
     * </ul>
     * </p>
     * 
     * <p><strong>⚠️ Important Notes:</strong>
     * <ul>
     *   <li><strong>Maps containers are NOT copied:</strong> Maps are treated as leaf objects (berries) and the same
     *       reference is maintained in the copy.</li>
     *   <li><strong>Implementation classes may change:</strong> For example, ArrayDeque becomes LinkedList
     *       (to support nulls), HashSet becomes LinkedHashSet (to preserve order). The semantic behavior
     *       is preserved where possible.</li>
     *   <li><strong>Concurrent/blocking queues:</strong> Special queue types (concurrent, blocking) become
     *       LinkedList, losing their concurrency or blocking semantics but preserving queue operations.</li>
     *   <li><strong>Thread Safety:</strong> This method is NOT thread-safe. The copy operation is not safe 
     *       under concurrent mutation of the source containers during traversal. If the source containers
     *       are being modified by other threads during the copy operation, the behavior is undefined and
     *       may result in {@code ConcurrentModificationException}, incomplete copies, or other issues.
     *       Ensure exclusive access to the source containers during the copy operation.</li>
     * </ul>
     * </p>
     * 
     * <p>Example:
     * <pre>{@code
     * Object[] array = {
     *     Arrays.asList("a", "b"),           // Will be copied to new ArrayList
     *     new String[]{"x", "y"},            // Will be copied to new String[]
     *     new HashMap<>(),                   // Will NOT be copied (Map is a berry)
     *     "standalone"                       // Will NOT be copied (String is a berry)
     * };
     * Object[] copy = deepCopyContainers(array);
     * // array != copy (new array)
     * // array[0] != copy[0] (new ArrayList)
     * // array[1] != copy[1] (new String array)
     * // array[2] == copy[2] (same HashMap reference)
     * // array[3] == copy[3] (same String reference)
     * }</pre>
     * </p>
     * 
     * <p>Queue/Deque Example:
     * <pre>{@code
     * ArrayDeque<String> deque = new ArrayDeque<>();
     * deque.addFirst("first");
     * deque.addLast("last");
     * 
     * Deque<String> copy = deepCopyContainers(deque);
     * // copy is a LinkedList that preserves deque operations!
     * copy.removeFirst();  // Works! Returns "first"
     * copy.removeLast();   // Works! Returns "last"
     * 
     * PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.reverseOrder());
     * pq.addAll(Arrays.asList(3, 1, 2));
     * 
     * PriorityQueue<Integer> pqCopy = deepCopyContainers(pq);
     * // Priority semantics preserved with comparator
     * pqCopy.poll();  // Returns 3 (largest first due to reverse order)
     * }</pre>
     * </p>
     * 
     * @param <T> the type of the input object
     * @param source the object to deep copy (can be array, collection, or any other object)
     * @return a deep copy of all containers with same references to non-containers,
     *         or the same reference if source is not a container
     * 
     * @apiNote This method uses generics for type safety. When type inference is problematic,
     *          explicitly specify the return type or cast the parameter:
     *          <ul>
     *          <li>Type-safe: {@code String[][] copy = deepCopyContainers(stringArray);}</li>
     *          <li>Explicit type: {@code Object copy = CollectionUtilities.<Object>deepCopyContainers(source);}</li>
     *          <li>With cast: {@code Object copy = deepCopyContainers((Object) source);}</li>
     *          </ul>
     *          Note: For callers who prefer to avoid type inference issues, simply declare the
     *          result as Object and cast as needed.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopyContainers(T source) {
        if (!isContainer(source)) {
            return source; // berry (includes Map) or null
        }
        
        // Track visited objects to handle cycles
        // Pre-size to avoid rehash thrash - we'll typically track every container
        Map<Object, Object> visited = new IdentityHashMap<>(64);
        
        // Queue for iterative processing - only containers go here
        Deque<ContainerPair> workQueue = new ArrayDeque<>();
        
        // Create the root copy and add to visited immediately
        Object rootCopy = createContainerCopy(source);
        visited.put(source, rootCopy);
        
        // Only queue the root if it needs processing
        // Primitive arrays are already fully copied by createContainerCopy
        Class<?> sourceClass = source.getClass();
        boolean rootIsPrimitiveArray =
            sourceClass.isArray() && sourceClass.getComponentType().isPrimitive();
        if (!rootIsPrimitiveArray) {
            workQueue.add(new ContainerPair(source, rootCopy));
        }
        
        // Process work queue
        while (!workQueue.isEmpty()) {
            ContainerPair pair = workQueue.poll();
            
            // Process this container's contents directly (no per-element allocations)
            Class<?> pairSourceClass = pair.source.getClass();
            if (pairSourceClass.isArray()) {
                // Handle array contents
                // Skip primitive arrays - already copied by System.arraycopy
                if (!pairSourceClass.getComponentType().isPrimitive()) {
                    // Use direct array access for object arrays (avoids reflection overhead)
                    // Casting once per container is safe for any reference array
                    Object[] srcArr = (Object[]) pair.source;
                    Object[] dstArr = (Object[]) pair.target;
                    int length = srcArr.length;
                    
                    for (int i = 0; i < length; i++) {
                        Object element = srcArr[i];
                        
                        if (isContainer(element)) {
                            // Check if we've already processed this container
                            Object existingCopy = visited.get(element);
                            if (existingCopy != null) {
                                // Use existing copy (handles cycles)
                                dstArr[i] = existingCopy;
                            } else {
                                // Special case: primitive arrays are fully copied immediately
                                Class<?> elementClass = element.getClass();
                                if (elementClass.isArray() && elementClass.getComponentType().isPrimitive()) {
                                    // Create and fully copy the primitive array
                                    int elemLength = ArrayUtilities.getLength(element);
                                    Class<?> componentType = elementClass.getComponentType();
                                    Object elementCopy = Array.newInstance(componentType, elemLength);
                                    System.arraycopy(element, 0, elementCopy, 0, elemLength);
                                    visited.put(element, elementCopy);
                                    dstArr[i] = elementCopy;
                                    // DO NOT enqueue - it's already fully copied
                                } else {
                                    // Create new container copy
                                    Object elementCopy = createContainerCopy(element);
                                    visited.put(element, elementCopy);
                                    dstArr[i] = elementCopy;
                                    // Queue the new container for processing
                                    workQueue.add(new ContainerPair(element, elementCopy));
                                }
                            }
                        } else {
                            // Berry - use same reference
                            dstArr[i] = element;
                        }
                    }
                }
            } else if (pair.source instanceof Collection) {
                // Handle collection contents
                Collection<?> sourceCollection = (Collection<?>) pair.source;
                Collection<Object> targetCollection = (Collection<Object>) pair.target;
                
                for (Object element : sourceCollection) {
                    if (isContainer(element)) {
                        // Check if we've already processed this container
                        Object existingCopy = visited.get(element);
                        if (existingCopy != null) {
                            // Use existing copy (handles cycles)
                            targetCollection.add(existingCopy);
                        } else {
                            // Special case: primitive arrays are fully copied immediately
                            Class<?> elementClass = element.getClass();
                            if (elementClass.isArray() && elementClass.getComponentType().isPrimitive()) {
                                // Create and fully copy the primitive array
                                int elemLength = ArrayUtilities.getLength(element);
                                Class<?> componentType = elementClass.getComponentType();
                                Object elementCopy = Array.newInstance(componentType, elemLength);
                                System.arraycopy(element, 0, elementCopy, 0, elemLength);
                                visited.put(element, elementCopy);
                                targetCollection.add(elementCopy);
                                // DO NOT enqueue - it's already fully copied
                            } else {
                                // Create new container copy
                                Object elementCopy = createContainerCopy(element);
                                visited.put(element, elementCopy);
                                targetCollection.add(elementCopy);
                                // Queue the new container for processing
                                workQueue.add(new ContainerPair(element, elementCopy));
                            }
                        }
                    } else {
                        // Berry - use same reference
                        targetCollection.add(element);
                    }
                }
            }
        }
        
        return (T) rootCopy;
    }
    
    /**
     * Determines if an object is a container (array or Collection).
     * Maps are NOT considered containers.
     */
    private static boolean isContainer(Object obj) {
        return obj != null && (obj.getClass().isArray() || obj instanceof Collection);
    }
    
    /**
     * Creates an empty copy of a container with the same type characteristics.
     * For primitive arrays, immediately copies the data since primitives can't be containers.
     * Collections are pre-sized to avoid resize overhead during population.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object createContainerCopy(Object source) {
        Class<?> sourceClass = source.getClass();
        if (sourceClass.isArray()) {
            int length = ArrayUtilities.getLength(source);
            Class<?> componentType = sourceClass.getComponentType();
            Object newArray = Array.newInstance(componentType, length);
            
            // For primitive arrays, copy immediately - no need to queue work
            if (componentType.isPrimitive()) {
                System.arraycopy(source, 0, newArray, 0, length);
            }
            return newArray;
        } else if (source instanceof EnumSet) {
            // EnumSet requires special handling
            EnumSet<?> src = (EnumSet<?>) source;
            if (src.isEmpty()) {
                // Use clone().clear() to preserve enum type for empty sets
                // This is bulletproof - works even when we can't access elements
                EnumSet<?> peer = src.clone();
                peer.clear();
                return peer;  // empty EnumSet of same enum type
            } else {
                // For non-empty sets, get enum type from first element
                return EnumSet.noneOf((Class) src.iterator().next().getDeclaringClass());
            }
        } else if (source instanceof Deque) {
            // Preserve deque behavior and tolerate nulls (LinkedList allows nulls, ArrayDeque doesn't)
            return new LinkedList<>();
        } else if (source instanceof PriorityQueue) {
            // Preserve priority queue with comparator and heap semantics
            PriorityQueue<?> pq = (PriorityQueue<?>) source;
            Comparator<?> cmp = pq.comparator();
            // Use source size for reasonable initial capacity
            return new PriorityQueue<>(Math.max(1, pq.size()), (Comparator) cmp);
        } else if (source instanceof SortedSet) {
            Comparator<?> cmp = ((SortedSet<?>) source).comparator();
            // TreeSet doesn't have a capacity constructor, but that's ok as it's a tree structure
            return cmp == null ? new TreeSet<>() : new TreeSet<>((Comparator) cmp);
        } else if (source instanceof Set) {
            Set<?> srcSet = (Set<?>) source;
            // Pre-size with load factor consideration to avoid rehashing
            int capacity = (int)(srcSet.size() / 0.75f) + 1;
            return new LinkedHashSet<>(capacity);
        } else if (source instanceof List) {
            List<?> srcList = (List<?>) source;
            // Pre-size to exact size to avoid resizing
            return new ArrayList<>(srcList.size());
        } else if (source instanceof Queue) {
            // Catch-all for other Queue implementations (concurrent/blocking queues)
            // Use LinkedList to preserve queue semantics and tolerate nulls
            return new LinkedList<>();
        } else if (source instanceof Collection) {
            // Fallback for any other collection types
            Collection<?> srcCollection = (Collection<?>) source;
            return new ArrayList<>(srcCollection.size());
        }
        throw new IllegalArgumentException("Unknown container type: " + source.getClass());
    }
    
    /**
     * Pair of source and target containers for processing.
     */
    private static class ContainerPair {
        final Object source;
        final Object target;
        
        ContainerPair(Object source, Object target) {
            this.source = source;
            this.target = target;
        }
    }

}
