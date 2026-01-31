package com.cedarsoftware.util.convert;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.IdentityHashMap;

import com.cedarsoftware.util.ArrayUtilities;

import static com.cedarsoftware.util.CollectionUtilities.getSynchronizedCollection;
import static com.cedarsoftware.util.CollectionUtilities.getUnmodifiableCollection;
import static com.cedarsoftware.util.CollectionUtilities.isSynchronized;
import static com.cedarsoftware.util.CollectionUtilities.isUnmodifiable;
import static com.cedarsoftware.util.convert.CollectionHandling.createCollection;

/**
 * Converts between arrays and collections while preserving collection characteristics.
 * Handles conversion from arrays to various collection types including:
 * <ul>
 *     <li>JDK collections (ArrayList, HashSet, etc.)</li>
 *     <li>Concurrent collections (ConcurrentSet, etc.)</li>
 *     <li>Special collections (Unmodifiable, Synchronized, etc.)</li>
 *     <li>Cedar Software collections (CaseInsensitiveSet, CompactSet, etc.)</li>
 * </ul>
 * The most specific matching collection type is used when converting, and collection
 * characteristics are preserved. For example, converting to a Set from a source that
 * maintains order will result in an ordered Set implementation.
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
public final class CollectionConversions {

    private CollectionConversions() { }


    /**
     * Converts an array to a collection, supporting special collection types
     * and nested arrays. Uses iterative processing to handle deeply nested
     * structures without stack overflow. Preserves circular references.
     *
     * @param array      The source array to convert
     * @param targetType The target collection type
     * @param <T>        The collection class to return
     * @return A collection of the specified target type
     */
    @SuppressWarnings("unchecked")
    public static <T extends Collection<?>> T arrayToCollection(Object array, Class<T> targetType) {
        // Track visited arrays to handle circular references
        IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();

        // Determine if the target type requires unmodifiable behavior
        boolean requiresUnmodifiable = isUnmodifiable(targetType);
        boolean requiresSynchronized = isSynchronized(targetType);

        // Create the appropriate collection using CollectionHandling
        Collection<Object> rootCollection = (Collection<Object>) createCollection(array, targetType);

        // If the target represents an empty collection, return it immediately
        if (isEmptyCollection(targetType)) {
            return (T) rootCollection;
        }

        // Track source array → target collection mapping
        visited.put(array, rootCollection);

        // Work queue for iterative processing
        Deque<ArrayToCollectionWorkItem> queue = new ArrayDeque<>();
        queue.add(new ArrayToCollectionWorkItem(array, rootCollection, targetType));

        while (!queue.isEmpty()) {
            ArrayToCollectionWorkItem work = queue.poll();
            int workLength = ArrayUtilities.getLength(work.sourceArray);

            for (int i = 0; i < workLength; i++) {
                Object element = ArrayUtilities.getElement(work.sourceArray, i);

                if (element != null && element.getClass().isArray()) {
                    // Check if we've already converted this array (circular reference)
                    if (visited.containsKey(element)) {
                        // Reuse existing collection - preserves cycles
                        work.targetCollection.add(visited.get(element));
                    } else {
                        // Create new collection for this nested array
                        Collection<Object> nestedCollection = (Collection<Object>) createCollection(element, work.targetType);
                        visited.put(element, nestedCollection);
                        work.targetCollection.add(nestedCollection);

                        // Queue for processing
                        queue.add(new ArrayToCollectionWorkItem(element, nestedCollection, work.targetType));
                    }
                } else {
                    // Simple element - add directly
                    work.targetCollection.add(element);
                }
            }
        }

        // If the created collection already matches the target type, return it as is
        if (targetType.isAssignableFrom(rootCollection.getClass())) {
            return (T) rootCollection;
        }

        // If wrapping is required, return the wrapped version
        if (requiresUnmodifiable) {
            return (T) getUnmodifiableCollection(rootCollection);
        }
        if (requiresSynchronized) {
            return (T) getSynchronizedCollection(rootCollection);
        }
        return (T) rootCollection;
    }

    /**
     * Converts a collection to another collection type, preserving characteristics.
     * Uses iterative processing to handle deeply nested collections without stack overflow.
     * Preserves circular references.
     *
     * @param source     The source collection to convert
     * @param targetType The target collection type
     * @return A collection of the specified target type
     */
    @SuppressWarnings("unchecked")
    public static Object collectionToCollection(Collection<?> source, Class<?> targetType) {
        // Track visited collections to handle circular references
        IdentityHashMap<Object, Object> visited = new IdentityHashMap<>();

        // Determine if the target type requires unmodifiable behavior
        boolean requiresUnmodifiable = isUnmodifiable(targetType);
        boolean requiresSynchronized = isSynchronized(targetType);

        // Create a modifiable collection of the specified target type
        Collection<Object> rootCollection = (Collection<Object>) createCollection(source, targetType);

        // If the target represents an empty collection, return it without population
        if (isEmptyCollection(targetType)) {
            return rootCollection;
        }

        // Track source collection → target collection mapping
        visited.put(source, rootCollection);

        // Work queue for iterative processing
        Deque<CollectionToCollectionWorkItem> queue = new ArrayDeque<>();
        queue.add(new CollectionToCollectionWorkItem(source, rootCollection, targetType, requiresUnmodifiable, requiresSynchronized));

        while (!queue.isEmpty()) {
            CollectionToCollectionWorkItem work = queue.poll();

            for (Object element : work.sourceCollection) {
                if (element instanceof Collection) {
                    // Check if we've already converted this collection (circular reference)
                    if (visited.containsKey(element)) {
                        // Reuse existing collection - preserves cycles
                        work.targetCollection.add(visited.get(element));
                    } else {
                        // Create new modifiable collection for this nested collection
                        Collection<Object> nestedModifiable = (Collection<Object>) createCollection(element, work.targetType);

                        // Wrap it before adding to parent if needed (wrapping is a view, so we can still populate it)
                        Object nestedToAdd;
                        if (work.requiresUnmodifiable) {
                            nestedToAdd = getUnmodifiableCollection(nestedModifiable);
                        } else if (work.requiresSynchronized) {
                            nestedToAdd = getSynchronizedCollection(nestedModifiable);
                        } else {
                            nestedToAdd = nestedModifiable;
                        }

                        // Track the wrapped version for cycle detection
                        visited.put(element, nestedToAdd);

                        // Add wrapped version to parent
                        work.targetCollection.add(nestedToAdd);

                        // Queue the MODIFIABLE version for processing (so we can populate it)
                        queue.add(new CollectionToCollectionWorkItem(
                            (Collection<?>) element,
                            nestedModifiable,  // Process the modifiable version
                            work.targetType,
                            work.requiresUnmodifiable,
                            work.requiresSynchronized
                        ));
                    }
                } else {
                    // Simple element - add directly
                    work.targetCollection.add(element);
                }
            }
        }

        // If the created collection already matches the target type, return it as is
        if (targetType.isAssignableFrom(rootCollection.getClass())) {
            return rootCollection;
        }

        // If wrapping is required, return the wrapped version
        if (requiresUnmodifiable) {
            return getUnmodifiableCollection(rootCollection);
        }
        if (requiresSynchronized) {
            return getSynchronizedCollection(rootCollection);
        }
        return rootCollection;
    }

    /**
     * Determines if the specified target type represents one of the empty
     * collection wrapper classes.
     */
    private static boolean isEmptyCollection(Class<?> targetType) {
        return CollectionsWrappers.getEmptyCollectionClass().isAssignableFrom(targetType)
                || CollectionsWrappers.getEmptyListClass().isAssignableFrom(targetType)
                || CollectionsWrappers.getEmptySetClass().isAssignableFrom(targetType)
                || CollectionsWrappers.getEmptySortedSetClass().isAssignableFrom(targetType)
                || CollectionsWrappers.getEmptyNavigableSetClass().isAssignableFrom(targetType);
    }

    /**
     * Work item for iterative array-to-collection conversion.
     * Holds the state needed to process one array during the conversion.
     */
    private static class ArrayToCollectionWorkItem {
        final Object sourceArray;
        final Collection<Object> targetCollection;
        final Class<?> targetType;

        ArrayToCollectionWorkItem(Object sourceArray, Collection<Object> targetCollection, Class<?> targetType) {
            this.sourceArray = sourceArray;
            this.targetCollection = targetCollection;
            this.targetType = targetType;
        }
    }

    /**
     * Work item for iterative collection-to-collection conversion.
     * Holds the state needed to process one collection during the conversion.
     */
    private static class CollectionToCollectionWorkItem {
        final Collection<?> sourceCollection;
        final Collection<Object> targetCollection;
        final Class<?> targetType;
        final boolean requiresUnmodifiable;
        final boolean requiresSynchronized;

        CollectionToCollectionWorkItem(Collection<?> sourceCollection, Collection<Object> targetCollection, Class<?> targetType, boolean requiresUnmodifiable, boolean requiresSynchronized) {
            this.sourceCollection = sourceCollection;
            this.targetCollection = targetCollection;
            this.targetType = targetType;
            this.requiresUnmodifiable = requiresUnmodifiable;
            this.requiresSynchronized = requiresSynchronized;
        }
    }
}
