package com.cedarsoftware.util;

import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

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

    // TODO: Remove iterator NOT working
    // Need to write my own Iterator?  What about on CaseInsensitiveMap?  Add test to ensure
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
        assertFalse(set.contains("two"));
        assertTrue(set.contains("one"));
        assertTrue(set.contains("three"));
    }

    @Test
    public void testToArray()
    {

    }

    @Test
    public void testToArrayWithArgs()
    {

    }

    @Test
    public void testAdd()
    {

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

    }

    @Test
    public void testAddAll()
    {

    }

    @Test
    public void testRetainAll()
    {

    }

    @Test
    public void testRemoveAll()
    {

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

    private Set get123()
    {
        Set set = new CaseInsensitiveSet();
        set.add("One");
        set.add("Two");
        set.add("Three");
        return set;
    }
}
