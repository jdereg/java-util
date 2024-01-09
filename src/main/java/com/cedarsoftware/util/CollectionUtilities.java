package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CollectionUtilities {

    private static final Set<?> unmodifiableEmptySet = Collections.unmodifiableSet(new HashSet<>());

    private static final List<?> unmodifiableEmptyList = Collections.unmodifiableList(new ArrayList<>());

    /**
     * For JDK1.8 support.  Remove this and change to List.of() for JDK11+
     */
    @SafeVarargs
    public static <T> List<T> listOf(T... items)
    {
        if (items == null || items.length ==0)
        {
            return (List<T>)unmodifiableEmptyList;
        }
        List<T> list = new ArrayList<>();
        Collections.addAll(list, items);
        return Collections.unmodifiableList(list);
    }

    /**
     * For JDK1.8 support.  Remove this and change to Set.of() for JDK11+
     */
    @SafeVarargs
    public static <T> Set<T> setOf(T... items)
    {
        if (items == null || items.length ==0)
        {
            return (Set<T>) unmodifiableEmptySet;
        }
        Set<T> set = new LinkedHashSet<>();
        Collections.addAll(set, items);
        return Collections.unmodifiableSet(set);
    }
}
