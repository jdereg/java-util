package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrentSet provides a Set that is thread-safe and usable in highly concurrent environments.
 * It supports adding and handling null elements by using a sentinel (NULL_ITEM).
 * <br>
 * @author John DeRegnaucourt
 *         <br>
 *         Copyright Cedar Software LLC
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
    private static final Object NULL_ITEM = new Object();
    private final Set<Object> set;

    /**
     * Create a new empty ConcurrentSet.
     */
    public ConcurrentSet() {
        set = ConcurrentHashMap.newKeySet();
    }

    /**
     * Create a new ConcurrentSet instance with data from the passed-in Collection.
     * This data is populated into the internal set with nulls replaced by NULL_ITEM.
     * @param col Collection to supply initial elements.
     */
    public ConcurrentSet(Collection<T> col) {
        set = ConcurrentHashMap.newKeySet(col.size());
        this.addAll(col);
    }

    /**
     * Create a new ConcurrentSet instance by wrapping an existing Set.
     * Nulls in the existing set are replaced by NULL_ITEM.
     * @param set Existing Set to wrap.
     */
    public ConcurrentSet(Set<T> set) {
        this.set = ConcurrentHashMap.newKeySet(set.size());
        this.addAll(set);
    }

    /**
     * Wraps an element, replacing null with NULL_ITEM.
     * @param item The element to wrap.
     * @return The wrapped element.
     */
    private Object wrap(T item) {
        return item == null ? NULL_ITEM : item;
    }

    /**
     * Unwraps an element, replacing NULL_ITEM with null.
     * @param item The element to unwrap.
     * @return The unwrapped element.
     */
    @SuppressWarnings("unchecked")
    private T unwrap(Object item) {
        return item == NULL_ITEM ? null : (T) item;
    }

    // --- Immutable APIs ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Set)) return false;
        Set<?> other = (Set<?>) o;
        if (other.size() != this.size()) return false;
        try {
            for (T item : this) { // Iterates over unwrapped items
                if (!other.contains(item)) { // Compares unwrapped items
                    return false;
                }
            }
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (T item : this) { // Iterates over unwrapped items
            h += (item == null ? 0 : item.hashCode());
        }
        return h;
    }

    @Override
    public String toString() {
        Iterator<T> it = iterator();
        if (!it.hasNext()) return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (;;) {
            T e = it.next();
            sb.append(e == this ? "(this Set)" : e);
            if (!it.hasNext()) return sb.append('}').toString();
            sb.append(',').append(' ');
        }
    }

    @Override
    public int size() { return set.size(); }

    @Override
    public boolean isEmpty() { return set.isEmpty(); }

    @Override
    public boolean contains(Object o) {
        return set.contains(wrap((T) o));
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<Object> iterator = set.iterator();
        return new Iterator<T>() {
            public boolean hasNext() { return iterator.hasNext(); }
            public T next() {
                Object item = iterator.next();
                return unwrap(item);
            }
            public void remove() {
                iterator.remove();
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] array = set.toArray();
        for (int i = 0; i < array.length; i++) {
            if (array[i] == NULL_ITEM) {
                array[i] = null;
            }
        }
        return array;
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        Object[] internalArray = set.toArray();
        int size = internalArray.length;
        if (a.length < size) {
            a = (T1[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        }
        for (int i = 0; i < size; i++) {
            if (internalArray[i] == NULL_ITEM) {
                a[i] = null;
            } else {
                a[i] = (T1) internalArray[i];
            }
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    @Override
    public boolean containsAll(Collection<?> col) {
        for (Object o : col) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    // --- Mutable APIs ---

    @Override
    public boolean add(T e) {
        return set.add(wrap(e));
    }

    @Override
    public boolean remove(Object o) {
        return set.remove(wrap((T) o));
    }

    @Override
    public boolean addAll(Collection<? extends T> col) {
        boolean modified = false;
        for (T item : col) {
            if (this.add(item)) { // Reuse add() which handles wrapping
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(Collection<?> col) {
        boolean modified = false;
        for (Object o : col) {
            if (this.remove(o)) { // Reuse remove() which handles wrapping
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> col) {
        Set<Object> wrappedCol = ConcurrentHashMap.newKeySet();
        for (Object o : col) {
            wrappedCol.add(wrap((T) o));
        }
        return set.retainAll(wrappedCol);
    }

    @Override
    public void clear() { set.clear(); }
}
