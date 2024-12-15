package com.cedarsoftware.util.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Provides cached access to common wrapper collection types (unmodifiable, synchronized, empty, checked).
 * All wrapper instances are pre-initialized in a static block and stored in a cache for reuse to improve
 * memory efficiency.
 *
 * <p>All collections are created empty and stored in a static cache. Wrapper collections are immutable
 * and safe for concurrent access across threads.</p>
 *
 * <p>Provides wrapper types for:</p>
 * <ul>
 *   <li>Unmodifiable collections (Collection, List, Set, SortedSet, NavigableSet)</li>
 *   <li>Synchronized collections (Collection, List, Set, SortedSet, NavigableSet)</li>
 *   <li>Empty collections (Collection, List, Set, SortedSet, NavigableSet)</li>
 *   <li>Checked collections (Collection, List, Set, SortedSet, NavigableSet)</li>
 * </ul>
 * 
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         Kenny Partlow (kpartlow@gmail.com)
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
public final class CollectionsWrappers {

    private static final Map<CollectionType, Class<?>> CACHE = new HashMap<>();

    private CollectionsWrappers() {}

    /**
     * Collection wrapper types available in the cache
     */
    private enum CollectionType {
        UNMODIFIABLE_COLLECTION,
        UNMODIFIABLE_LIST,
        UNMODIFIABLE_SET,
        UNMODIFIABLE_SORTED_SET,
        UNMODIFIABLE_NAVIGABLE_SET,
        SYNCHRONIZED_COLLECTION,
        SYNCHRONIZED_LIST,
        SYNCHRONIZED_SET,
        SYNCHRONIZED_SORTED_SET,
        SYNCHRONIZED_NAVIGABLE_SET,
        EMPTY_COLLECTION,
        EMPTY_LIST,
        EMPTY_SET,
        EMPTY_SORTED_SET,
        EMPTY_NAVIGABLE_SET,
        CHECKED_COLLECTION,
        CHECKED_LIST,
        CHECKED_SET,
        CHECKED_SORTED_SET,
        CHECKED_NAVIGABLE_SET
    }

    static {
        // Initialize unmodifiable collections
        CACHE.put(CollectionType.UNMODIFIABLE_COLLECTION, Collections.unmodifiableCollection(new ArrayList<>()).getClass());
        CACHE.put(CollectionType.UNMODIFIABLE_LIST, Collections.unmodifiableList(new ArrayList<>()).getClass());
        CACHE.put(CollectionType.UNMODIFIABLE_SET, Collections.unmodifiableSet(new HashSet<>()).getClass());
        CACHE.put(CollectionType.UNMODIFIABLE_SORTED_SET, Collections.unmodifiableSortedSet(new TreeSet<>()).getClass());
        CACHE.put(CollectionType.UNMODIFIABLE_NAVIGABLE_SET, Collections.unmodifiableNavigableSet(new TreeSet<>()).getClass());

        // Initialize synchronized collections
        CACHE.put(CollectionType.SYNCHRONIZED_COLLECTION, Collections.synchronizedCollection(new ArrayList<>()).getClass());
        CACHE.put(CollectionType.SYNCHRONIZED_LIST, Collections.synchronizedList(new ArrayList<>()).getClass());
        CACHE.put(CollectionType.SYNCHRONIZED_SET, Collections.synchronizedSet(new HashSet<>()).getClass());
        CACHE.put(CollectionType.SYNCHRONIZED_SORTED_SET, Collections.synchronizedSortedSet(new TreeSet<>()).getClass());
        CACHE.put(CollectionType.SYNCHRONIZED_NAVIGABLE_SET, Collections.synchronizedNavigableSet(new TreeSet<>()).getClass());

        // Initialize empty collections
        CACHE.put(CollectionType.EMPTY_COLLECTION, Collections.emptyList().getClass());
        CACHE.put(CollectionType.EMPTY_LIST, Collections.emptyList().getClass());
        CACHE.put(CollectionType.EMPTY_SET, Collections.emptySet().getClass());
        CACHE.put(CollectionType.EMPTY_SORTED_SET, Collections.emptySortedSet().getClass());
        CACHE.put(CollectionType.EMPTY_NAVIGABLE_SET, Collections.emptyNavigableSet().getClass());

        // Initialize checked collections
        CACHE.put(CollectionType.CHECKED_COLLECTION, Collections.checkedCollection(new ArrayList<>(), Object.class).getClass());
        CACHE.put(CollectionType.CHECKED_LIST, Collections.checkedList(new ArrayList<>(), Object.class).getClass());
        CACHE.put(CollectionType.CHECKED_SET, Collections.checkedSet(new HashSet<>(), Object.class).getClass());
        CACHE.put(CollectionType.CHECKED_SORTED_SET, Collections.checkedSortedSet(new TreeSet<>(), Object.class).getClass());
        CACHE.put(CollectionType.CHECKED_NAVIGABLE_SET, Collections.checkedNavigableSet(new TreeSet<>(), Object.class).getClass());
    }

    // Unmodifiable collection getters
    @SuppressWarnings("unchecked")
    public static <E> Class<Collection<E>> getUnmodifiableCollectionClass() {
        return (Class<Collection<E>>) CACHE.get(CollectionType.UNMODIFIABLE_COLLECTION);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<List<E>> getUnmodifiableListClass() {
        return (Class<List<E>>) CACHE.get(CollectionType.UNMODIFIABLE_LIST);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<Set<E>> getUnmodifiableSetClass() {
        return (Class<Set<E>>) CACHE.get(CollectionType.UNMODIFIABLE_SET);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<SortedSet<E>> getUnmodifiableSortedSetClass() {
        return (Class<SortedSet<E>>) CACHE.get(CollectionType.UNMODIFIABLE_SORTED_SET);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<NavigableSet<E>> getUnmodifiableNavigableSetClass() {
        return (Class<NavigableSet<E>>) CACHE.get(CollectionType.UNMODIFIABLE_NAVIGABLE_SET);
    }

    // Synchronized collection getters
    @SuppressWarnings("unchecked")
    public static <E> Class<Collection<E>> getSynchronizedCollectionClass() {
        return (Class<Collection<E>>) CACHE.get(CollectionType.SYNCHRONIZED_COLLECTION);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<List<E>> getSynchronizedListClass() {
        return (Class<List<E>>) CACHE.get(CollectionType.SYNCHRONIZED_LIST);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<Set<E>> getSynchronizedSetClass() {
        return (Class<Set<E>>) CACHE.get(CollectionType.SYNCHRONIZED_SET);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<SortedSet<E>> getSynchronizedSortedSetClass() {
        return (Class<SortedSet<E>>) CACHE.get(CollectionType.SYNCHRONIZED_SORTED_SET);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<NavigableSet<E>> getSynchronizedNavigableSetClass() {
        return (Class<NavigableSet<E>>) CACHE.get(CollectionType.SYNCHRONIZED_NAVIGABLE_SET);
    }

    // Empty collection getters
    @SuppressWarnings("unchecked")
    public static <E> Class<Collection<E>> getEmptyCollectionClass() {
        return (Class<Collection<E>>) CACHE.get(CollectionType.EMPTY_COLLECTION);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<List<E>> getEmptyListClass() {
        return (Class<List<E>>) CACHE.get(CollectionType.EMPTY_LIST);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<Set<E>> getEmptySetClass() {
        return (Class<Set<E>>) CACHE.get(CollectionType.EMPTY_SET);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<SortedSet<E>> getEmptySortedSetClass() {
        return (Class<SortedSet<E>>) CACHE.get(CollectionType.EMPTY_SORTED_SET);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<NavigableSet<E>> getEmptyNavigableSetClass() {
        return (Class<NavigableSet<E>>) CACHE.get(CollectionType.EMPTY_NAVIGABLE_SET);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<Collection<E>> getCheckedCollectionClass() {
        return (Class<Collection<E>>) CACHE.get(CollectionType.CHECKED_COLLECTION);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<List<E>> getCheckedListClass() {
        return (Class<List<E>>) CACHE.get(CollectionType.CHECKED_LIST);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<Set<E>> getCheckedSetClass() {
        return (Class<Set<E>>) CACHE.get(CollectionType.CHECKED_SET);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<SortedSet<E>> getCheckedSortedSetClass() {
        return (Class<SortedSet<E>>) CACHE.get(CollectionType.CHECKED_SORTED_SET);
    }

    @SuppressWarnings("unchecked")
    public static <E> Class<NavigableSet<E>> getCheckedNavigableSetClass() {
        return (Class<NavigableSet<E>>) CACHE.get(CollectionType.CHECKED_NAVIGABLE_SET);
    }
}