package com.cedarsoftware.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;

/**
 * SealableList provides a List or List wrapper that can be 'sealed' and 'unsealed.'  When
 * sealed, the List is immutable, when unsealed it is mutable. The iterator(),
 * listIterator(), and subList() return views that honor the Supplier's sealed state.
 * The sealed state can be changed as often as needed.
 * <br><br>
 * NOTE: Please do not reformat this code as the current format makes it easy to see the overall structure.
 * <br><br>
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
 *
 * @deprecated This class is no longer supported.
 */
@Deprecated
public class SealableList<T> implements List<T> {
    private final List<T> list;
    private final transient Supplier<Boolean> sealedSupplier;

    /**
     * Create a SealableList. Since no List is being supplied, this will use an ConcurrentList internally. If you
     * want to use an ArrayList for example, use SealableList constructor that takes a List and pass it the instance
     * you want it to wrap.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableList(Supplier<Boolean> sealedSupplier) {
        this.list = new ConcurrentList<>();
        this.sealedSupplier = sealedSupplier;
    }

    /**
     * Create a SealableList. Since a List is not supplied, the elements from the passed in Collection will be
     * copied to an internal ConcurrentList.  If you want to use an ArrayList for example, use SealableList
     * constructor that takes a List and pass it the instance you want it to wrap.
     * @param col Collection to supply initial elements.  These are copied to an internal ConcurrentList.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableList(Collection<T> col, Supplier<Boolean> sealedSupplier) {
        this.list = new ConcurrentList<>();
        this.list.addAll(col);
        this.sealedSupplier = sealedSupplier;
    }

    /**
     * Use this constructor to wrap a List (any kind of List) and make it a SealableList.
     * No duplicate of the List is created and the original list is operated on directly if unsealed, or protected
     * from changes if sealed.
     * @param list List instance to protect.
     * @param sealedSupplier {@code Supplier<Boolean>} that returns 'true' to indicate sealed, 'false' for mutable.
     */
    public SealableList(List<T> list, Supplier<Boolean> sealedSupplier) {
        this.list = list;
        this.sealedSupplier = sealedSupplier;
    }

    private void throwIfSealed() {
        if (sealedSupplier.get()) {
            throw new UnsupportedOperationException("This list has been sealed and is now immutable");
        }
    }

    // Immutable APIs
    public boolean equals(Object other) { return list.equals(other); }
    public int hashCode() { return list.hashCode(); }
    public String toString() { return list.toString(); }
    public int size() { return list.size(); }
    public boolean isEmpty() { return list.isEmpty(); }
    public boolean contains(Object o) { return list.contains(o); }
    public boolean containsAll(Collection<?> col) { return new HashSet<>(list).containsAll(col); }
    public int indexOf(Object o) { return list.indexOf(o); }
    public int lastIndexOf(Object o) { return list.lastIndexOf(o); }
    public T get(int index) { return list.get(index); }
    public Object[] toArray() { return list.toArray(); }
    public <T1> T1[] toArray(T1[] a) { return list.toArray(a);}
    public Iterator<T> iterator() { return createSealHonoringIterator(list.iterator()); }
    public ListIterator<T> listIterator() { return createSealHonoringListIterator(list.listIterator()); }
    public ListIterator<T> listIterator(final int index) { return createSealHonoringListIterator(list.listIterator(index)); }
    public List<T> subList(int fromIndex, int toIndex) { return new SealableList<>(list.subList(fromIndex, toIndex), sealedSupplier); }
    
    // Mutable APIs
    public boolean add(T t) { throwIfSealed(); return list.add(t); }
    public boolean remove(Object o) { throwIfSealed(); return list.remove(o); }
    public boolean addAll(Collection<? extends T> col) { throwIfSealed(); return list.addAll(col); }
    public boolean addAll(int index, Collection<? extends T> col) { throwIfSealed(); return list.addAll(index, col); }
    public boolean removeAll(Collection<?> col) { throwIfSealed(); return list.removeAll(col); }
    public boolean retainAll(Collection<?> col) { throwIfSealed(); return list.retainAll(col); }
    public void clear() { throwIfSealed(); list.clear(); }
    public T set(int index, T element) { throwIfSealed(); return list.set(index, element); }
    public void add(int index, T element) { throwIfSealed(); list.add(index, element); }
    public T remove(int index) { throwIfSealed(); return list.remove(index); }

    private Iterator<T> createSealHonoringIterator(Iterator<T> iterator) {
        return new Iterator<T>() {
            public boolean hasNext() { return iterator.hasNext(); }
            public T next() { return iterator.next(); }
            public void remove() { throwIfSealed(); iterator.remove(); }
        };
    }

    private ListIterator<T> createSealHonoringListIterator(ListIterator<T> iterator) {
        return new ListIterator<T>() {
            public boolean hasNext() { return iterator.hasNext();}
            public T next() { return iterator.next(); }
            public boolean hasPrevious() { return iterator.hasPrevious(); }
            public T previous() { return iterator.previous(); }
            public int nextIndex() { return iterator.nextIndex(); }
            public int previousIndex() { return iterator.previousIndex(); }
            public void remove() { throwIfSealed(); iterator.remove(); }
            public void set(T e) { throwIfSealed(); iterator.set(e); }
            public void add(T e) { throwIfSealed(); iterator.add(e);}
        };
    }
}