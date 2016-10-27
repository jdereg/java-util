package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static com.cedarsoftware.util.StringUtilities.hashCodeIgnoreCase;

/**
 * Implements a java.util.Set that will not utilize 'case' when comparing Strings
 * contained within the Set.  The set can be homogeneous or heterogeneous.
 * If the CaseInsensitiveSet is iterated, when Strings are encountered, the original
 * Strings are returned (retains case).
 *
 * @author John DeRegnaucourt (john@cedarsoftware.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class CaseInsensitiveSet<E> implements Set<E>
{
    private final Map<E, Object> map;

    public CaseInsensitiveSet() { map = new CaseInsensitiveMap<>(); }

    public CaseInsensitiveSet(Collection<? extends E> collection)
    {
        if (collection instanceof ConcurrentSkipListSet)
        {
            map = new CaseInsensitiveMap<>(new ConcurrentSkipListMap());
        }
        else if (collection instanceof SortedSet)
        {
            map = new CaseInsensitiveMap<>(new TreeMap());
        }
        else
        {
            map = new CaseInsensitiveMap<>(collection.size());
        }
        addAll(collection);
    }

    public CaseInsensitiveSet(int initialCapacity)
    {
        map = new CaseInsensitiveMap<>(initialCapacity);
    }

    public CaseInsensitiveSet(int initialCapacity, float loadFactor)
    {
        map = new CaseInsensitiveMap<>(initialCapacity, loadFactor);
    }

    public int hashCode()
    {
        int hash = 0;
        for (Object item : map.keySet())
        {
            if (item != null)
            {
                if (item instanceof String)
                {
                    hash += hashCodeIgnoreCase((String)item);
                }
                else
                {
                    hash += item.hashCode();
                }
            }
        }
        return hash;
    }

    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (!(other instanceof Set)) return false;

        Set that = (Set) other;
        return that.size()==size() && containsAll(that);
    }

    public int size()
    {
        return map.size();
    }

    public boolean isEmpty()
    {
        return map.isEmpty();
    }

    public boolean contains(Object o)
    {
        return map.containsKey(o);
    }

    public Iterator<E> iterator()
    {
        return map.keySet().iterator();
    }

    public Object[] toArray()
    {
        return map.keySet().toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        return map.keySet().toArray(a);
    }

    public boolean add(E e)
    {
        int size = map.size();
        map.put(e, e);
        return map.size() != size;
    }

    public boolean remove(Object o)
    {
        int size = map.size();
        map.remove(o);
        return map.size() != size;
    }

    public boolean containsAll(Collection<?> c)
    {
        for (Object o : c)
        {
            if (!map.containsKey(o))
            {
                return false;
            }
        }
        return true;
    }

    public boolean addAll(Collection<? extends E> c)
    {
        int size = map.size();
        for (E elem : c)
        {
            map.put(elem, elem);
        }
        return map.size() != size;
    }

    public boolean retainAll(Collection<?> c)
    {
        Map other = new CaseInsensitiveMap();
        for (Object o : c)
        {
            other.put(o, null);
        }

        Iterator i = map.keySet().iterator();
        int size = map.size();
        while (i.hasNext())
        {
            Object elem = i.next();
            if (!other.containsKey(elem))
            {
                i.remove();
            }
        }
        return map.size() != size;
    }

    public boolean removeAll(Collection<?> c)
    {
        int size = map.size();
        for (Object elem : c)
        {
            map.remove(elem);
        }
        return map.size() != size;
    }

    public void clear()
    {
        map.clear();
    }

    public String toString()
    {
        return map.keySet().toString();
    }
}
