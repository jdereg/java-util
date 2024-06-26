package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrentSet provides a Set that is thread-safe, usable in highly concurrent environments. It provides
 * a no-arg constructor that will directly return a ConcurrentSet that is thread-safe.  It has a constructor
 * that takes a Collection argument and populates its internal Concurrent Set delegate implementation.
 * <br>
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
public class ConcurrentSet<T> implements Set<T> {
    private final Set<T> set;

    /**
     * Create a new empty ConcurrentSet.
     */
    public ConcurrentSet() {
        set = ConcurrentHashMap.newKeySet();
    }

    /**
     * Create a new ConcurrentSet instance with data from the passed in Collection.  This data is populated into the
     * internal set. 
     * @param col
     */
    public ConcurrentSet(Collection<T> col) {
        set = ConcurrentHashMap.newKeySet(col.size());
        set.addAll(col);
    }
    
    // Immutable APIs
    public boolean equals(Object other) { return set.equals(other); }
    public int hashCode() { return set.hashCode(); }
    public String toString() { return set.toString(); }
    public boolean isEmpty() { return set.isEmpty(); }
    public int size() { return set.size(); }
    public boolean contains(Object o) { return set.contains(o); }
    public boolean containsAll(Collection<?> c) { return set.containsAll(c); }
    public Iterator<T> iterator() { return set.iterator(); }
    public Object[] toArray() { return set.toArray(); }
    public <T1> T1[] toArray(T1[] a) { return set.toArray(a); }

    // Mutable APIs
    public boolean add(T e) {return set.add(e);}
    public boolean addAll(Collection<? extends T> c) { return set.addAll(c); }
    public boolean remove(Object o) { return set.remove(o); }
    public boolean removeAll(Collection<?> c) { return set.removeAll(c); }
    public boolean retainAll(Collection<?> c) { return set.retainAll(c); }
    public void clear() { set.clear(); }
}
