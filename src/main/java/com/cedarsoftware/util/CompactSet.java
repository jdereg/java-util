package com.cedarsoftware.util;

import java.util.*;

/**
 * Often, memory may be consumed by lots of Maps or Sets (HashSet uses a HashMap to implement it's set).  HashMaps
 * and other similar Maps often have a lot of blank entries in their internal structures.  If you have a lot of Maps
 * in memory, perhaps representing JSON objects, large amounts of memory can be consumed by these empty Map entries.<p></p>
 *
 * CompactSet is a Set that strives to reduce memory at all costs while retaining speed that is close to HashSet's speed.
 * It does this by using only one (1) member variable (of type Object) and changing it as the Set grows.  It goes from
 * an Object[] to a Set when the size() of the Set crosses the threshold defined by the method compactSize() (defaults
 * to 80).  After the Set crosses compactSize() size, then it uses a Set (defined by the user) to hold the items.  This
 * Set is defined by a method that can be overridden, which returns a new empty Set() for use in the {@literal >} compactSize()
 * state.<pre>
 *
 *     Methods you may want to override:
 *
 *     // Map you would like it to use when size() {@literal >} compactSize().  HashSet is default
 *     protected abstract Map{@literal <}K, V{@literal >} getNewMap();
 *
 *     // If you want case insensitivity, return true and return new CaseInsensitiveSet or TreeSet(String.CASE_INSENSITIVE_PRDER) from getNewSet()
 *     protected boolean isCaseInsensitive() { return false; }
 *
 *     // When size() {@literal >} than this amount, the Set returned from getNewSet() is used to store elements.
 *     protected int compactSize() { return 80; }
 * </pre>
 * This Set supports holding a null element.
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
public class CompactSet<E> extends AbstractSet<E>
{
    private static final String EMPTY_SET = "_︿_ψ_☼";
    private static final String NO_ENTRY = EMPTY_SET;
    private Object val = EMPTY_SET;

    public CompactSet()
    {
        if (compactSize() < 2)
        {
            throw new IllegalStateException("compactSize() must be >= 2");
        }
    }

    public CompactSet(Collection<E> other)
    {
        this();
        addAll(other);
    }

    public int size()
    {
        if (val instanceof Object[])
        {   // 1 to compactSize
            return ((Object[])val).length;
        }
        else if (val instanceof Set)
        {   // > compactSize
            return ((Set<E>)val).size();
        }
        // empty
        return 0;
    }

    public boolean isEmpty()
    {
        return val == EMPTY_SET;
    }

    private boolean compareItems(Object item, Object anItem)
    {
        if (item instanceof String)
        {
            if (anItem instanceof String)
            {
                if (isCaseInsensitive())
                {
                    return ((String)anItem).equalsIgnoreCase((String) item);
                }
                else
                {
                    return anItem.equals(item);
                }
            }
            return false;
        }

        return Objects.equals(item, anItem);
    }

    public boolean contains(Object item)
    {
        if (val instanceof Object[])
        {   // 1 to compactSize
            Object[] entries = (Object[]) val;
            int len = entries.length;
            for (int i=0; i < len; i++)
            {
                if (compareItems(item, entries[i]))
                {
                    return true;
                }
            }
            return false;
        }
        else if (val instanceof Set)
        {   // > compactSize
            Set<E> set = (Set<E>) val;
            return set.contains(item);
        }
        // empty
        return false;
    }

    public Iterator<E> iterator()
    {
        return new Iterator<E>()
        {
            Iterator<E> iter = getCopy().iterator();
            E currentEntry = (E) NO_ENTRY;

            public boolean hasNext() { return iter.hasNext(); }
            
            public E next()
            {
                currentEntry = iter.next();
                return currentEntry;
            }

            public void remove()
            {
                if (currentEntry == (E)NO_ENTRY)
                {   // remove() called on iterator
                    throw new IllegalStateException("remove() called on an Iterator before calling next()");
                }
                CompactSet.this.remove(currentEntry);
                currentEntry = (E)NO_ENTRY;
            }
        };
    }

    private Set<E> getCopy()
    {
        Set<E> copy = getNewSet(size());   // Use their Set (TreeSet, HashSet, LinkedHashSet, etc.)
        if (val instanceof Object[])
        {   // 1 to compactSize - copy Object[] into Set
            Object[] entries = (Object[]) CompactSet.this.val;
            for (Object entry : entries)
            {
                copy.add((E) entry);
            }
        }
        else if (val instanceof Set)
        {   // > compactSize - addAll to copy
            copy.addAll((Set<E>)CompactSet.this.val);
        }
//        else
//        {   // empty - nothing to copy
//        }
        return copy;
    }
    
    public boolean add(E item)
    {
        if (val instanceof Object[])
        {   // 1 to compactSize
            if (contains(item))
            {
                return false;
            }

            Object[] entries = (Object[]) val;
            if (size() < compactSize())
            {   // Grow array
                Object[] expand = new Object[entries.length + 1];
                System.arraycopy(entries, 0, expand, 0, entries.length);
                // Place new entry at end
                expand[expand.length - 1] = item;
                val = expand;
            }
            else
            {   // Switch to Map - copy entries
                Set<E> set = getNewSet(size() + 1);
                entries = (Object[]) val;
                for (Object anItem : entries)
                {
                    set.add((E) anItem);
                }
                // Place new entry
                set.add(item);
                val = set;
            }
            return true;
        }
        else if (val instanceof Set)
        {   // > compactSize
            Set<E> set = (Set<E>) val;
            return set.add(item);
        }
        // empty
        val = new Object[] { item };
        return true;
    }

    public boolean remove(Object item)
    {
        if (val instanceof Object[])
        {
            Object[] local = (Object[]) val;
            int len = local.length;

            for (int i=0; i < len; i++)
            {
                if (compareItems(local[i], item))
                {
                    if (len == 1)
                    {
                        val = EMPTY_SET;
                    }
                    else
                    {
                        Object[] newElems = new Object[len - 1];
                        System.arraycopy(local, i + 1, local, i, len - i - 1);
                        System.arraycopy(local, 0, newElems, 0, len - 1);
                        val = newElems;
                    }
                    return true;
                }
            }
            return false;    // not found
        }
        else if (val instanceof Set)
        {   // > compactSize
            Set<E> set = (Set<E>) val;
            if (!set.contains(item))
            {
                return false;
            }
            boolean removed = set.remove(item);

            if (set.size() == compactSize())
            {   // Down to compactSize, need to switch to Object[]
                Object[] entries = new Object[compactSize()];
                Iterator<E> i = set.iterator();
                int idx = 0;
                while (i.hasNext())
                {
                    entries[idx++] = i.next();
                }
                val = entries;
            }
            return removed;
        }
        
        // empty
        return false;
    }

    public void clear()
    {
        val = EMPTY_SET;
    }

    /**
     * @return new empty Set instance to use when size() becomes {@literal >} compactSize().
     */
    protected Set<E> getNewSet() { return new HashSet<>(compactSize() + 1); }
    protected Set<E> getNewSet(int size)
    {
        Set<E> set = getNewSet();
        try
        {   // Extra step here is to get a Map of the same type as above, with the "size" already established
            // which saves the time of growing the internal array dynamically.
            set = set.getClass().getConstructor(Integer.TYPE).newInstance(size);
        }
        catch (Exception ignored) { }
        return set;
    }
    protected boolean isCaseInsensitive() { return false; }
    protected int compactSize() { return 80; }
}
