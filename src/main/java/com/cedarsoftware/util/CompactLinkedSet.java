package com.cedarsoftware.util;

import java.util.Collection;

/**
 * A case-insensitive Set implementation that uses a compact internal representation
 * for small sets.  This Set exists to simplify JSON serialization. No custom reader nor
 * writer is needed to serialize this set.  It is a drop-in replacement for LinkedHashSet.
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
public class CompactLinkedSet<E> extends CompactSet<E> {

    /**
     * Constructs an empty {@code CompactCIHashSet} with case-insensitive configuration.
     * <p>
     * Specifically, it sets the set to be case-insensitive.
     * </p>
     *
     * @throws IllegalArgumentException if {@link #compactSize()} returns a value less than 2
     */
    public CompactLinkedSet() {
        super(CompactMap.<E, Object>builder()
                .caseSensitive(true)
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
    public CompactLinkedSet(Collection<E> other) {
        this();
        // Add all elements from the provided collection
        addAll(other);
    }

    /**
     * Indicates that this set is case-sensitive.
     *
     * @return {@code false} to denote case-sensitive behavior
     */
    @Override
    protected boolean isCaseInsensitive() {
        return false;
    }

}