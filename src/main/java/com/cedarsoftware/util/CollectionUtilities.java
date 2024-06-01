package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
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

    /**
     * This is a null-safe isEmpty check.
     *
     * @param col Collection to check
     * @return true if empty or null
     */
    public static boolean isEmpty(Collection col) {
        return col == null || col.isEmpty();
    }

    /**
     * This is a null-safe isEmpty check.
     *
     * @param col Collection to check
     * @return true if empty or null
     */
    public static boolean hasContent(Collection col) {
        return col != null && !col.isEmpty();
    }

    /**
     * This is a null-safe size check.
     *
     * @param col Collection to check
     * @return true if empty or null
     */
    public static int size(Collection col) {
        return col == null ? 0 : col.size();
    }

    /**
     * For JDK1.8 support.  Remove this and change to List.of() for JDK11+
     */
    @SafeVarargs
    public static <T> List<T> listOf(T... items) {
        if (items == null || items.length == 0) {
            return (List<T>) unmodifiableEmptyList;
        }
        List<T> list = new ArrayList<>();
        Collections.addAll(list, items);
        return Collections.unmodifiableList(list);
    }

    /**
     * For JDK1.8 support.  Remove this and change to Set.of() for JDK11+
     */
    @SafeVarargs
    public static <T> Set<T> setOf(T... items) {
        if (items == null || items.length == 0) {
            return (Set<T>) unmodifiableEmptySet;
        }
        Set<T> set = new LinkedHashSet<>();
        Collections.addAll(set, items);
        return Collections.unmodifiableSet(set);
    }
}
