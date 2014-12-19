package com.cedarsoftware.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
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
public class TestCaseInsensitiveSet
{
    @Test
    public void testSize()
    {
        CaseInsensitiveSet set = new CaseInsensitiveSet();
        set.add(16);
        set.add("Hi");
        assertEquals(2, set.size());
        set.remove(16);
        assertEquals(1, set.size());
        set.remove("hi");   // different case
        assertEquals(0, set.size());
    }

    @Test
    public void testIsEmpty()
    {
        CaseInsensitiveSet set = new CaseInsensitiveSet();
        assertTrue(set.isEmpty());
        set.add("Seven");
        assertFalse(set.isEmpty());
        set.remove("SEVEN");
        assertTrue(set.isEmpty());
    }

    @Test
    public void testContains()
    {
        Set set = get123();
        set.add(9);
        assertTrue(set.contains("One"));
        assertTrue(set.contains("one"));
        assertTrue(set.contains("onE"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("TWO"));
        assertTrue(set.contains("Two"));
        assertTrue(set.contains("three"));
        assertTrue(set.contains("THREE"));
        assertTrue(set.contains("Three"));
        assertTrue(set.contains(9));
        assertFalse(set.contains("joe"));
        set.remove("one");
        assertFalse(set.contains("one"));
    }

    @Test
    public void testIterator()
    {
        Set set = get123();

        int count = 0;
        Iterator i = set.iterator();
        while (i.hasNext())
        {
            i.next();
            count++;
        }

        assertEquals(3, count);

        i = set.iterator();
        while (i.hasNext())
        {
            Object elem = i.next();
            if (elem.equals("One"))
            {
                i.remove();
            }
        }

        assertEquals(2, set.size());
        assertFalse(set.contains("one"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("three"));
    }

    @Test
    public void testToArray()
    {
        Set set = get123();
        Object[] items = set.toArray();
        assertEquals(3, items.length);
        assertEquals(items[0], "One");
        assertEquals(items[1], "Two");
        assertEquals(items[2], "Three");
    }

    @Test
    public void testToArrayWithArgs()
    {
        Set set = get123();
        String[] items = (String[]) set.toArray(new String[]{});
        assertEquals(3, items.length);
        assertEquals(items[0], "One");
        assertEquals(items[1], "Two");
        assertEquals(items[2], "Three");
    }

    @Test
    public void testAdd()
    {
        Set set = get123();
        set.add("Four");
        assertEquals(set.size(), 4);
        assertTrue(set.contains("FOUR"));
    }

    @Test
    public void testRemove()
    {
        Set set = get123();
        assertEquals(3, set.size());
        set.remove("one");
        assertEquals(2, set.size());
        set.remove("TWO");
        assertEquals(1, set.size());
        set.remove("ThreE");
        assertEquals(0, set.size());
        set.add(45);
        assertEquals(1, set.size());
    }

    @Test
    public void testContainsAll()
    {
        List list = new ArrayList();
        list.add("one");
        list.add("two");
        list.add("three");
        Set set = get123();
        assertTrue(set.containsAll(list));
        assertTrue(set.containsAll(new ArrayList()));
        list.clear();
        list.add("one");
        list.add("four");
        assertFalse(set.containsAll(list));
    }

    @Test
    public void testAddAll()
    {
        Set set = get123();
        List list = new ArrayList();
        list.add("one");
        list.add("TWO");
        list.add("four");
        set.addAll(list);
        assertTrue(set.size() == 4);
        assertTrue(set.contains("FOUR"));
    }

    @Test
    public void testRetainAll()
    {
        Set set = get123();
        List list = new ArrayList();
        list.add("TWO");
        list.add("four");
        set.retainAll(list);
        assertTrue(set.size() == 1);
        assertTrue(set.contains("tWo"));
    }

    @Test
    public void testRemoveAll()
    {
        Set set = get123();
        Set set2 = new HashSet();
        set2.add("one");
        set2.add("three");
        set.removeAll(set2);
        assertEquals(1, set.size());
        assertTrue(set.contains("TWO"));
    }

    @Test
    public void testClearAll()
    {
        Set set = get123();
        assertEquals(3, set.size());
        set.clear();
        assertEquals(0, set.size());
        set.add("happy");
        assertEquals(1, set.size());
    }

    @Test
    public void testConstructors()
    {
        Set hashSet = new HashSet();
        hashSet.add("BTC");
        hashSet.add("LTC");

        Set set1 = new CaseInsensitiveSet(hashSet);
        assertTrue(set1.size() == 2);
        assertTrue(set1.contains("btc"));
        assertTrue(set1.contains("ltc"));

        Set set2 = new CaseInsensitiveSet(10);
        set2.add("BTC");
        set2.add("LTC");
        assertTrue(set2.size() == 2);
        assertTrue(set2.contains("btc"));
        assertTrue(set2.contains("ltc"));

        Set set3 = new CaseInsensitiveSet(10, 0.75f);
        set3.add("BTC");
        set3.add("LTC");
        assertTrue(set3.size() == 2);
        assertTrue(set3.contains("btc"));
        assertTrue(set3.contains("ltc"));
    }

    @Test
    public void testHashCodeAndEquals()
    {
        Set set1 = new HashSet();
        set1.add("Bitcoin");
        set1.add("Litecoin");
        set1.add(16);
        set1.add(null);

        Set set2 = new CaseInsensitiveSet();
        set2.add("Bitcoin");
        set2.add("Litecoin");
        set2.add(16);
        set2.add(null);

        Set set3 = new CaseInsensitiveSet();
        set3.add("BITCOIN");
        set3.add("LITECOIN");
        set3.add(16);
        set3.add(null);

        assertTrue(set1.hashCode() != set2.hashCode());
        assertTrue(set1.hashCode() != set3.hashCode());
        assertTrue(set2.hashCode() == set3.hashCode());

        assertTrue(set1.equals(set2));
        assertFalse(set1.equals(set3));
        assertTrue(set3.equals(set1));
        assertTrue(set2.equals(set3));
    }

    @Test
    public void testToString()
    {
        Set set = get123();
        String s = set.toString();
        assertTrue(s.contains("One"));
        assertTrue(s.contains("Two"));
        assertTrue(s.contains("Three"));
    }

    @Test
    public void testKeySet()
    {
        Set s = get123();
        assertTrue(s.contains("oNe"));
        assertTrue(s.contains("tWo"));
        assertTrue(s.contains("tHree"));

        s = get123();
        Iterator i = s.iterator();
        i.next();
        i.remove();
        assertEquals(2, s.size());

        i.next();
        i.remove();
        assertEquals(1, s.size());

        i.next();
        i.remove();
        assertEquals(0, s.size());
    }

    @Test
    public void testRetainAll2()
    {
        Set<String> oldSet = new CaseInsensitiveSet<>();
        Set<String> newSet = new CaseInsensitiveSet<>();

        oldSet.add("foo");
        oldSet.add("bar");
        newSet.add("foo");
        newSet.add("bar");
        newSet.add("qux");
        assertTrue(newSet.retainAll(oldSet));
    }

    @Test
    public void testAddAll2()
    {
        Set<String> oldSet = new CaseInsensitiveSet<>();
        Set<String> newSet = new CaseInsensitiveSet<>();

        oldSet.add("foo");
        oldSet.add("bar");
        newSet.add("foo");
        newSet.add("bar");
        newSet.add("qux");
        assertFalse(newSet.addAll(oldSet));
    }

    @Test
    public void testRemoveAll2()
    {
        Set<String> oldSet = new CaseInsensitiveSet<>();
        Set<String> newSet = new CaseInsensitiveSet<>();

        oldSet.add("bart");
        oldSet.add("qux");
        newSet.add("foo");
        newSet.add("bar");
        newSet.add("qux");
        boolean ret = newSet.removeAll(oldSet);
        assertTrue(ret);
    }

    @Test
    public void testAgainstUnmodifiableSet()
    {
        Set<String> oldKeys = new CaseInsensitiveSet<>();
        oldKeys.add("foo");
        oldKeys = Collections.unmodifiableSet(oldKeys);
        Set<String> newKeys = new CaseInsensitiveSet<>();
        newKeys.add("foo");
        newKeys.add("bar");
        newKeys = Collections.unmodifiableSet(newKeys);

        Set<String> sameKeys = new CaseInsensitiveSet<>(newKeys);
        sameKeys.retainAll(oldKeys);

        Set<String> addedKeys  = new CaseInsensitiveSet<>(newKeys);
        addedKeys.removeAll(sameKeys);
        assertEquals(1, addedKeys.size());
        assertTrue(addedKeys.contains("BAR"));
    }

    private Set get123()
    {
        Set set = new CaseInsensitiveSet();
        set.add("One");
        set.add("Two");
        set.add("Three");
        return set;
    }
}
