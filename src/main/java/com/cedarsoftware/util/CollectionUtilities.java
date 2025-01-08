package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

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

    private static final Set<?> unmodifiableEmptySet = Collections.unmodifiableSet(new HashSet<>());
    private static final List<?> unmodifiableEmptyList = Collections.unmodifiableList(new ArrayList<>());
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
        List<T> list = new ArrayList<>();
        Collections.addAll(list, items);
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
        Set<T> set = new LinkedHashSet<>();
        Collections.addAll(set, items);
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
     * Determines whether the specified class represents an synchronized collection type.
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
     *     <li>For all other collection types, it defaults to returning {@link Collections#emptyList()}.</li>
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
            return Collections.emptyList(); // Default to an empty list for other collection types
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
     * List<Object> list = new ArrayList<>(List.of("one", "two"));
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
     * List<String> list = new ArrayList<>(List.of("one", "two", "three"));
     * Collection<String> synchronizedList = getSynchronizedCollection(list);
     *
     * synchronized (synchronizedList) {
     *     for (String item : synchronizedList) {
     *         System.out.println(item);
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
}
