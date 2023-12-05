package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
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
public class TestCaseInsensitiveSet
{
    @Test
    public void testSize()
    {
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
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
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        assertTrue(set.isEmpty());
        set.add("Seven");
        assertFalse(set.isEmpty());
        set.remove("SEVEN");
        assertTrue(set.isEmpty());
    }

    @Test
    public void testContains()
    {
        Set<Object> set = get123();
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
        Set<Object> set = get123();

        int count = 0;
        Iterator<Object> i = set.iterator();
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
        Set<Object> set = get123();
        Object[] items = set.toArray();
        assertEquals(3, items.length);
        assertEquals(items[0], "One");
        assertEquals(items[1], "Two");
        assertEquals(items[2], "Three");
    }

    @Test
    public void testToArrayWithArgs()
    {
        Set<Object> set = get123();
        String[] empty = new String[]{};
        String[] items = set.toArray(empty);
        assertEquals(3, items.length);
        assertEquals(items[0], "One");
        assertEquals(items[1], "Two");
        assertEquals(items[2], "Three");
    }

    @Test
    public void testAdd()
    {
        Set<Object> set = get123();
        set.add("Four");
        assertEquals(set.size(), 4);
        assertTrue(set.contains("FOUR"));
    }

    @Test
    public void testRemove()
    {
        Set<Object> set = get123();
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
        List<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        list.add("three");
        Set<Object> set = get123();
        assertTrue(set.containsAll(list));
        assertTrue(set.containsAll(new ArrayList<>()));
        list.clear();
        list.add("one");
        list.add("four");
        assertFalse(set.containsAll(list));
    }

    @Test
    public void testAddAll()
    {
        Set<Object> set = get123();
        List<Object> list = new ArrayList<>();
        list.add("one");
        list.add("TWO");
        list.add("four");
        set.addAll(list);
        assertEquals(4, set.size());
        assertTrue(set.contains("FOUR"));
    }

    @Test
    public void testRetainAll()
    {
        Set<Object> set = get123();
        List<Object> list = new ArrayList<>();
        list.add("TWO");
        list.add("four");
        assert set.retainAll(list);
        assertEquals(1, set.size());
        assertTrue(set.contains("tWo"));
    }

    @Test
    public void testRetainAll3()
    {
        Set<Object> set = get123();
        Set<Object> set2 = get123();
        assert !set.retainAll(set2);
        assert set2.size() == set.size();
    }

    @Test
    public void testRemoveAll()
    {
        Set<Object> set = get123();
        Set<Object> set2 = new HashSet<>();
        set2.add("one");
        set2.add("three");
        set.removeAll(set2);
        assertEquals(1, set.size());
        assertTrue(set.contains("TWO"));
    }

    @Test
    public void testRemoveAll3()
    {
        Set<Object> set = get123();
        Set<Object> set2 = new HashSet<>();
        set2.add("a");
        set2.add("b");
        set2.add("c");
        assert !set.removeAll(set2);
        assert set.size() == get123().size();
        set2.add("one");
        assert set.removeAll(set2);
        assert set.size() == get123().size() - 1;
    }

    @Test
    public void testClearAll()
    {
        Set<Object> set = get123();
        assertEquals(3, set.size());
        set.clear();
        assertEquals(0, set.size());
        set.add("happy");
        assertEquals(1, set.size());
    }

    @Test
    public void testConstructors()
    {
        Set<Object> hashSet = new HashSet<>();
        hashSet.add("BTC");
        hashSet.add("LTC");

        Set<Object> set1 = new CaseInsensitiveSet<>(hashSet);
        assertEquals(2, set1.size());
        assertTrue(set1.contains("btc"));
        assertTrue(set1.contains("ltc"));

        Set<Object> set2 = new CaseInsensitiveSet<>(10);
        set2.add("BTC");
        set2.add("LTC");
        assertEquals(2, set2.size());
        assertTrue(set2.contains("btc"));
        assertTrue(set2.contains("ltc"));

        Set<Object> set3 = new CaseInsensitiveSet<Object>(10, 0.75f);
        set3.add("BTC");
        set3.add("LTC");
        assertEquals(2, set3.size());
        assertTrue(set3.contains("btc"));
        assertTrue(set3.contains("ltc"));
    }

    @Test
    public void testHashCodeAndEquals()
    {
        Set<Object> set1 = new HashSet<>();
        set1.add("Bitcoin");
        set1.add("Litecoin");
        set1.add(16);
        set1.add(null);

        Set<Object> set2 = new CaseInsensitiveSet<>();
        set2.add("Bitcoin");
        set2.add("Litecoin");
        set2.add(16);
        set2.add(null);

        Set<Object> set3 = new CaseInsensitiveSet<>();
        set3.add("BITCOIN");
        set3.add("LITECOIN");
        set3.add(16);
        set3.add(null);

        assertTrue(set1.hashCode() != set2.hashCode());
        assertTrue(set1.hashCode() != set3.hashCode());
        assertEquals(set2.hashCode(), set3.hashCode());

        assertEquals(set1, set2);
        assertNotEquals(set1, set3);
        assertEquals(set3, set1);
        assertEquals(set2, set3);
    }

    @Test
    public void testToString()
    {
        Set<Object> set = get123();
        String s = set.toString();
        assertTrue(s.contains("One"));
        assertTrue(s.contains("Two"));
        assertTrue(s.contains("Three"));
    }

    @Test
    public void testKeySet()
    {
        Set<Object> s = get123();
        assertTrue(s.contains("oNe"));
        assertTrue(s.contains("tWo"));
        assertTrue(s.contains("tHree"));

        s = get123();
        Iterator<Object> i = s.iterator();
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
        sameKeys.retainAll(oldKeys);        // allow modifiability
    }

    @Test
    public void testTreeSet()
    {
        Collection<String> set = new CaseInsensitiveSet<>(new TreeSet<>());
        set.add("zuLU");
        set.add("KIlo");
        set.add("charLIE");
        assert set.size() == 3;
        assert set.contains("charlie");
        assert set.contains("kilo");
        assert set.contains("zulu");
        Object[] array = set.toArray();
        assert array[0].equals("charLIE");
        assert array[1].equals("KIlo");
        assert array[2].equals("zuLU");
    }

    @Test
    public void testTreeSetNoNull()
    {
        try
        {
            Collection<String> set = new CaseInsensitiveSet<>(new TreeSet<>());
            set.add(null);
            fail("should not make it here");
        }
        catch (NullPointerException ignored)
        { }
    }

    @Test
    public void testConcurrentSet()
    {
        Collection<String> set = new CaseInsensitiveSet<>(new ConcurrentSkipListSet<>());
        set.add("zuLU");
        set.add("KIlo");
        set.add("charLIE");
        assert set.size() == 3;
        assert set.contains("charlie");
        assert set.contains("kilo");
        assert set.contains("zulu");
    }

    @Test
    public void testConcurrentSetNoNull()
    {
        try
        {
            Collection<String> set = new CaseInsensitiveSet<String>(new ConcurrentSkipListSet<String>());
            set.add(null);
        }
        catch (NullPointerException ignored)
        { }
    }

    @Test
    public void testHashSet()
    {
        Collection<String> set = new CaseInsensitiveSet<>(new HashSet<>());
        set.add("zuLU");
        set.add("KIlo");
        set.add("charLIE");
        assert set.size() == 3;
        assert set.contains("charlie");
        assert set.contains("kilo");
        assert set.contains("zulu");
    }

    @Test
    public void testHashSetNoNull()
    {
        Collection<String> set = new CaseInsensitiveSet<>(new HashSet<>());
        set.add(null);
        set.add("alpha");
        assert set.size() == 2;
    }

    @Test
    public void testUnmodifiableSet()
    {
        Set<String> junkSet = new HashSet<>();
        junkSet.add("z");
        junkSet.add("J");
        junkSet.add("a");
        Set<String> set = new CaseInsensitiveSet<>(Collections.unmodifiableSet(junkSet));
        assert set.size() == 3;
        assert set.contains("A");
        assert set.contains("j");
        assert set.contains("Z");
        set.add("h");
    }

    @Test
    public void testMinus()
    {
        CaseInsensitiveSet<Object> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("aaa");
        ciSet.add("bbb");
        ciSet.add("ccc");
        ciSet.add('d'); // Character

        Set<Object> things = new HashSet<>();
        things.add(1L);
        things.add("aAa");
        things.add('c');
        ciSet.minus(things);
        assert ciSet.size() == 3;
        assert ciSet.contains("BbB");
        assert ciSet.contains("cCC");

        ciSet.minus(7);
        assert ciSet.size() == 3;

        ciSet.minus('d');
        assert ciSet.size() == 2;

        Set<Object> theRest = new HashSet<>();
        theRest.add("BBb");
        theRest.add("CCc");
        ciSet.minus(theRest);
        assert ciSet.isEmpty();
    }

    @Test
    public void testPlus()
    {
        CaseInsensitiveSet<Object> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("aaa");
        ciSet.add("bbb");
        ciSet.add("ccc");
        ciSet.add('d'); // Character

        Set<Object> things = new HashSet<>();
        things.add(1L);
        things.add("aAa");  // no duplicate added
        things.add('c');
        ciSet.plus(things);
        assert ciSet.size() == 6;
        assert ciSet.contains(1L);
        assert ciSet.contains('c');

        ciSet.plus(7);
        assert ciSet.size() == 7;
        assert ciSet.contains(7);
    }

    @Test
    public void testHashMapBacked()
    {
        String[] strings = new String[] { "foo", "bar", "baz", "qux", "quux", "garpley"};
        Set<String> set = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new HashMap<>()));
        Set<String> ordered = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(Collections.emptyMap(), new LinkedHashMap<>()));

        set.addAll(Arrays.asList(strings));
        ordered.addAll(Arrays.asList(strings));

        assert ordered.equals(set);

        Iterator<String> i = set.iterator();
        Iterator<String> j = ordered.iterator();

        boolean orderDiffered = false;

        while (i.hasNext())
        {
            String x = i.next();
            String y = j.next();
            
            if (!Objects.equals(x, y))
            {
                orderDiffered = true;
            }
        }

        assert orderDiffered;
    }

    @Test
    public void testEquals()
    {
        Set<Object> set = new CaseInsensitiveSet<>(get123());
        assert !set.equals("cat");
        Set<Object> other = new CaseInsensitiveSet<>(get123());
        assert set.equals(other);

        other.remove("Two");
        assert !set.equals(other);
        other.add("too");
        assert !set.equals(other);
    }

    private static Set<Object> get123()
    {
        Set<Object> set = new CaseInsensitiveSet<>();
        set.add("One");
        set.add("Two");
        set.add("Three");
        return set;
    }
}
