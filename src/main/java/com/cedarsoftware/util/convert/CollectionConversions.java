package com.cedarsoftware.util.convert;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;

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
final class CollectionConversions {
    private static final Class<?> unmodifiableCollectionClass = WrappedCollections.getUnmodifiableCollection();

    private CollectionConversions() {
        // Private constructor to prevent instantiation
    }

    /**
     * Converts an array to a collection, supporting special collection types
     * and nested arrays.
     *
     * @param array      The source array to convert
     * @param targetType The target collection type
     * @return A collection of the specified target type
     */
    @SuppressWarnings("unchecked")
    static Object arrayToCollection(Object array, Class<?> targetType) {
        int length = Array.getLength(array);

        // Create the appropriate collection using CollectionHandling
        Collection<Object> collection = (Collection<Object>) CollectionHandling.createCollection(array, targetType);

        // Populate the collection with array elements
        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);

            if (element != null && element.getClass().isArray()) {
                // Recursively handle nested arrays
                element = arrayToCollection(element, targetType);
            }

            collection.add(element);
        }

        return collection;
    }

    /**
     * Converts a collection to another collection type, preserving characteristics.
     *
     * @param source     The source collection to convert
     * @param targetType The target collection type
     * @return A collection of the specified target type
     */
    @SuppressWarnings("unchecked")
    static Object collectionToCollection(Collection<?> source, Class<?> targetType) {
        // Determine if the target type requires unmodifiable behavior
        boolean requiresUnmodifiable = isUnmodifiable(targetType);

        // Create a modifiable or pre-wrapped collection
        Collection<Object> targetCollection = (Collection<Object>) CollectionHandling.createCollection(source, targetType);

        targetCollection.addAll(source);

        // If wrapping is required, return the wrapped version
        if (requiresUnmodifiable) {
            if (targetCollection instanceof NavigableSet) {
                return Collections.unmodifiableNavigableSet((NavigableSet<?>)targetCollection);
            } else if (targetCollection instanceof SortedSet) {
                return Collections.unmodifiableSortedSet((SortedSet<?>) targetCollection);
            } else if (targetCollection instanceof Set) {
                return Collections.unmodifiableSet((Set<?>) targetCollection);
            } else if (targetCollection instanceof List) {
                return Collections.unmodifiableList((List<?>) targetCollection);
            } else {
                return Collections.unmodifiableCollection(targetCollection);
            }
        }

        return targetCollection;
    }

    /**
     * Checks if the target type indicates an unmodifiable collection.
     *
     * @param targetType The target type to check.
     * @return True if the target type indicates unmodifiable, false otherwise.
     */
    private static boolean isUnmodifiable(Class<?> targetType) {
        return unmodifiableCollectionClass.isAssignableFrom(targetType);
    }
}