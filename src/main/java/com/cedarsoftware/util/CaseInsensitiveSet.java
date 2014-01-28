package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Implements a java.util.Set that will not utilize 'case' when comparing Strings
 * contained within the Set.  The set can be homogeneous or heterogeneous.
 * If the CaseInsensitiveSet is iterated, when Strings are encountered, the original
 * Strings are returned (retains case).
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class CaseInsensitiveSet<E> implements Set<E>
{
    private final CaseInsensitiveMap<E, Object> map;

    public CaseInsensitiveSet() { map = new CaseInsensitiveMap<E, Object>(); }

    public int hashCode()
    {
        int hash = 0;
        for (Object item : map.keySet())
        {
            if (item != null)
            {
                if (item instanceof String)
                {
                    hash += ((String)item).toLowerCase().hashCode();
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

    public CaseInsensitiveSet(Collection<? extends E> collection)
    {
        map = new CaseInsensitiveMap<E, Object>(collection.size());
        for (E item : collection)
        {
            map.put(item, null);
        }
    }

    public CaseInsensitiveSet(int initialCapacity)
    {
        map = new CaseInsensitiveMap<E, Object>(initialCapacity);
    }

    public CaseInsensitiveSet(int initialCapacity, float loadFactor)
    {
        map = new CaseInsensitiveMap<E, Object>(initialCapacity, loadFactor);
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
        boolean exists = map.containsKey(e);
        map.put(e, null);
        return !exists;
    }

    public boolean remove(Object o)
    {
        boolean exists = map.containsKey(o);
        map.remove(o);
        return exists;
    }

    public boolean containsAll(Collection<?> c)
    {
        boolean fail = false;
        for (Object o : c)
        {
            if (!map.containsKey(o))
            {
                 fail = true;
                break;
            }
        }
        return !fail;
    }

    public boolean addAll(Collection<? extends E> c)
    {
        int origSize = map.size();

        for (E elem : c)
        {
            map.put(elem, null);
        }
        return map.size() != origSize;
    }

    public boolean retainAll(Collection<?> c)
    {
        Map other = new CaseInsensitiveMap();
        for (Object o : c)
        {
            other.put(o, null);
        }

        int origSize = map.size();

        for (Object elem : map.keySet())
        {
            if (!other.containsKey(elem))
            {
                map.remove(elem);
            }
        }
        return map.size() != origSize;
    }

    public boolean removeAll(Collection<?> c)
    {
        int origSize = map.size();

        for (Object elem : c)
        {
            map.remove(elem);
        }
        return map.size() != origSize;
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
