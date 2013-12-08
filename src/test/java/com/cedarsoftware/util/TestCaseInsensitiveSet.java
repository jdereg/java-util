package com.cedarsoftware.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author John DeRegnaucourt
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

    private Set get123()
    {
        Set set = new CaseInsensitiveSet();
        set.add("One");
        set.add("Two");
        set.add("Three");
        return set;
    }
}
