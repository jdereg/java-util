package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Set;

/**
 * @deprecated As of release 2.19.0, replaced by {@link CompactSet} with builder configurations.
 *             This class is no longer recommended for use and may be removed in future releases.
 *             <p>
 *             Similar to {@link CompactSet}, but it is configured to be case-insensitive.
 *             Instead of using this subclass, please utilize {@link CompactSet} with the builder
 *             to configure case insensitivity, sequence order, and other desired behaviors.
 *             </p>
 *             <p>
 *             Example migration:
 *             </p>
 *             <pre>{@code
 * // Deprecated usage:
 * CompactCILinkedSet<String> ciLinkedSet = new CompactCILinkedSet<>();
 * ciLinkedSet.add("Apple");
 * assert ciLinkedSet.contains("APPLE"); // true
 *
 * // Recommended replacement:
 * CompactSet<String> compactSet = CompactSet.<String>builder()
 *     .caseSensitive(false)
 *     .insertionOrder()
 *     .build();
 * compactSet.add("Apple");
 * assert compactSet.contains("APPLE"); // true
 * }</pre>
 *
 * <p>
 * This approach reduces the need for multiple specialized subclasses and leverages the
 * flexible builder pattern to achieve the desired configurations.
 * </p>
 *
 * @param <E> the type of elements maintained by this set
 *
 * @author
 *         John DeRegnaucourt (jdereg@gmail.com)
 *
 * @see CompactSet
 * @see CompactSet.Builder
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
@Deprecated
public class CompactCILinkedSet<E> extends CompactSet<E> {

    /**
     * Constructs an empty {@code CompactCIHashSet} with case-insensitive configuration.
     * <p>
     * Specifically, it sets the set to be case-insensitive.
     * </p>
     *
     * @throws IllegalArgumentException if {@link #compactSize()} returns a value less than 2
     */
    public CompactCILinkedSet() {
        // Initialize the superclass with a pre-configured CompactMap using the builder
        super(CompactMap.<E, Object>builder()
                .caseSensitive(false) // case-insensitive
                .insertionOrder()
                .build());
    }

    /**
     * Constructs a {@code CompactCIHashSet} containing the elements of the specified collection.
     * <p>
     * The set will be case-insensitive.
     * </p>
     *
     * @param other the collection whose elements are to be placed into this set
     * @throws NullPointerException if the specified collection is null
     * @throws IllegalArgumentException if {@link #compactSize()} returns a value less than 2
     */
    public CompactCILinkedSet(Collection<E> other) {
        // Initialize the superclass with a pre-configured CompactMap using the builder
        super(CompactMap.<E, Object>builder()
                .caseSensitive(false) // case-insensitive
                .insertionOrder()
                .build());
        // Add all elements from the provided collection
        addAll(other);
    }

    /**
     * Indicates that this set is case-insensitive.
     *
     * @return {@code true} to denote case-insensitive behavior
     */
    @Override
    protected boolean isCaseInsensitive() {
        return true;
    }

    /**
     * @deprecated This method is no longer used and has been removed.
     *             It is retained here only to maintain backward compatibility with existing subclasses.
     *             New implementations should use the builder pattern to configure {@link CompactSet}.
     *
     * @return {@code null} as this method is deprecated and no longer functional
     */
    @Deprecated
    @Override
    protected Set<E> getNewSet() {
        // Deprecated method; no longer used in the new CompactSet implementation.
        // Returning null to indicate it has no effect.
        return null;
    }
}
