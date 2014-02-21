package com.cedarsoftware.util;

import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
public class TestCaseInsensitiveMap
{
    @Test
    public void testMapStraightUp()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        assertTrue(stringMap.get("one").equals("Two"));
        assertTrue(stringMap.get("One").equals("Two"));
        assertTrue(stringMap.get("oNe").equals("Two"));
        assertTrue(stringMap.get("onE").equals("Two"));
        assertTrue(stringMap.get("ONe").equals("Two"));
        assertTrue(stringMap.get("oNE").equals("Two"));
        assertTrue(stringMap.get("ONE").equals("Two"));

        assertFalse(stringMap.get("one").equals("two"));

        assertTrue(stringMap.get("three").equals("Four"));
        assertTrue(stringMap.get("fIvE").equals("Six"));
    }

    @Test
    public void testOverwrite()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        assertTrue(stringMap.get("three").equals("Four"));

        stringMap.put("thRee", "Thirty");

        assertFalse(stringMap.get("three").equals("Four"));
        assertTrue(stringMap.get("three").equals("Thirty"));
        assertTrue(stringMap.get("THREE").equals("Thirty"));
    }

    @Test
    public void testKeySetWithOverwrite()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        stringMap.put("thREe", "Four");

        Set<String> keySet = stringMap.keySet();
        assertNotNull(keySet);
        assertTrue(!keySet.isEmpty());
        assertTrue(keySet.size() == 3);

        boolean foundOne = false, foundThree = false, foundFive = false;
        for (String key : keySet)
        {
            if (key.equals("One"))
            {
                foundOne = true;
            }
            if (key.equals("thREe"))
            {
                foundThree = true;
            }
            if (key.equals("Five"))
            {
                foundFive = true;
            }
        }
        assertTrue(foundOne);
        assertTrue(foundThree);
        assertTrue(foundFive);
    }

    @Test
    public void testEntrySetWithOverwrite()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        stringMap.put("thREe", "four");

        Set<Map.Entry<String, Object>> entrySet = stringMap.entrySet();
        assertNotNull(entrySet);
        assertTrue(!entrySet.isEmpty());
        assertTrue(entrySet.size() == 3);

        boolean foundOne = false, foundThree = false, foundFive = false;
        for (Map.Entry<String, Object> entry : entrySet)
        {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.equals("One") && value.equals("Two"))
            {
                foundOne = true;
            }
            if (key.equals("thREe") && value.equals("four"))
            {
                foundThree = true;
            }
            if (key.equals("Five") && value.equals("Six"))
            {
                foundFive = true;
            }
        }
        assertTrue(foundOne);
        assertTrue(foundThree);
        assertTrue(foundFive);
    }

    @Test
    public void testPutAll()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();
        CaseInsensitiveMap<String, Object> newMap = new CaseInsensitiveMap<String, Object>(2);
        newMap.put("thREe", "four");
        newMap.put("Seven", "Eight");

        stringMap.putAll(newMap);

        assertTrue(stringMap.size() == 4);
        assertFalse(stringMap.get("one").equals("two"));
        assertTrue(stringMap.get("fIvE").equals("Six"));
        assertTrue(stringMap.get("three").equals("four"));
        assertTrue(stringMap.get("seven").equals("Eight"));

        Map a = createSimpleMap();
        a.putAll(null);     // Ensure NPE not happening
    }

    @Test
    public void testContainsKey()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        assertTrue(stringMap.containsKey("one"));
        assertTrue(stringMap.containsKey("One"));
        assertTrue(stringMap.containsKey("oNe"));
        assertTrue(stringMap.containsKey("onE"));
        assertTrue(stringMap.containsKey("ONe"));
        assertTrue(stringMap.containsKey("oNE"));
        assertTrue(stringMap.containsKey("ONE"));
    }

    @Test
    public void testRemove()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        assertTrue(stringMap.remove("one").equals("Two"));
        assertNull(stringMap.get("one"));
    }

    @Test
    public void testNulls()
    {
        CaseInsensitiveMap<String, Object> stringMap = createSimpleMap();

        stringMap.put(null, "Something");
        assertTrue("Something".equals(stringMap.get(null)));
    }

    @Test
    public void testRemoveIterator()
    {
        Map map = new CaseInsensitiveMap();
        map.put("One", null);
        map.put("Two", null);
        map.put("Three", null);

        int count = 0;
        Iterator i = map.keySet().iterator();
        while (i.hasNext())
        {
            i.next();
            count++;
        }

        assertEquals(3, count);

        i = map.keySet().iterator();
        while (i.hasNext())
        {
            Object elem = i.next();
            if (elem.equals("One"))
            {
                i.remove();
            }
        }

        assertEquals(2, map.size());
        assertFalse(map.containsKey("one"));
        assertTrue(map.containsKey("two"));
        assertTrue(map.containsKey("three"));
    }

    @Test
    public void testEquals()
    {
        Map a = createSimpleMap();
        Map b = createSimpleMap();
        assertTrue(a.equals(b));
        Map c = new HashMap();
        assertFalse(a.equals(c));

        Map other = new LinkedHashMap();
        other.put("one", "Two");
        other.put("THREe", "Four");
        other.put("five", "Six");

        assertTrue(a.equals(other));
        assertTrue(other.equals(a));

        other.clear();
        other.put("one", "Two");
        other.put("Three-x", "Four");
        other.put("five", "Six");
        assertFalse(a.equals(other));

        other.clear();
        other.put("One", "Two");
        other.put("Three", "Four");
        other.put("Five", "six");   // lowercase six
        assertFalse(a.equals(other));

        assertFalse(a.equals("Foo"));

        other.put("FIVE", null);
        assertFalse(a.equals(other));
    }

    @Test
    public void testHashCode()
    {
        Map a = createSimpleMap();
        Map b = new CaseInsensitiveMap(a);
        assertTrue(a.hashCode() == b.hashCode());

        b = new CaseInsensitiveMap();
        b.put("ONE", "Two");
        b.put("THREE", "Four");
        b.put("FIVE", "Six");
        assertEquals(a.hashCode(), b.hashCode());

        b = new CaseInsensitiveMap();
        b.put("One", "Two");
        b.put("THREE", "FOUR");
        b.put("Five", "Six");
        assertFalse(a.hashCode() == b.hashCode());
    }

    @Test
    public void testToString()
    {
        assertNotNull(createSimpleMap().toString());
    }

    @Test
    public void testClear()
    {
        Map a = createSimpleMap();
        a.clear();
        assertEquals(0, a.size());
    }

    @Test
    public void testContainsValue()
    {
        Map a = createSimpleMap();
        assertTrue(a.containsValue("Two"));
        assertFalse(a.containsValue("TWO"));
    }

    @Test
    public void testValues()
    {
        Map a = createSimpleMap();
        Collection col = a.values();
        assertEquals(3, col.size());
        assertTrue(col.contains("Two"));
        assertTrue(col.contains("Four"));
        assertTrue(col.contains("Six"));
        assertFalse(col.contains("TWO"));
    }


    @Test
    public void testNullKey()
    {
        Map a = createSimpleMap();
        a.put(null, "foo");
        String b = (String) a.get(null);
        int x = b.hashCode();
        assertEquals("foo", b);
    }

    @Test
    public void testConstructors()
    {
        Map<String, Object> map = new CaseInsensitiveMap<String, Object>();
        map.put("BTC", "Bitcoin");
        map.put("LTC", "Litecoin");

        assertTrue(map.size() == 2);
        assertEquals("Bitcoin", map.get("btc"));
        assertEquals("Litecoin", map.get("ltc"));

        map = new CaseInsensitiveMap<String, Object>(20);
        map.put("BTC", "Bitcoin");
        map.put("LTC", "Litecoin");

        assertTrue(map.size() == 2);
        assertEquals("Bitcoin", map.get("btc"));
        assertEquals("Litecoin", map.get("ltc"));

        map = new CaseInsensitiveMap<String, Object>(20, 0.85f);
        map.put("BTC", "Bitcoin");
        map.put("LTC", "Litecoin");

        assertTrue(map.size() == 2);
        assertEquals("Bitcoin", map.get("btc"));
        assertEquals("Litecoin", map.get("ltc"));

        Map map1 = new HashMap<String, Object>();
        map1.put("BTC", "Bitcoin");
        map1.put("LTC", "Litecoin");

        map = new CaseInsensitiveMap<String, Object>(map1);
        assertTrue(map.size() == 2);
        assertEquals("Bitcoin", map.get("btc"));
        assertEquals("Litecoin", map.get("ltc"));
    }

    @Test
    public void testEqualsAndHashCode()
    {
        Map map1 = new HashMap();
        map1.put("BTC", "Bitcoin");
        map1.put("LTC", "Litecoin");
        map1.put(16, 16);
        map1.put(null, null);

        Map map2 = new CaseInsensitiveMap();
        map2.put("BTC", "Bitcoin");
        map2.put("LTC", "Litecoin");
        map2.put(16, 16);
        map2.put(null, null);

        Map map3 = new CaseInsensitiveMap();
        map3.put("btc", "Bitcoin");
        map3.put("ltc", "Litecoin");
        map3.put(16, 16);
        map3.put(null, null);

        assertTrue(map1.hashCode() != map2.hashCode());    // By design: case sensitive maps will [rightly] compute hash of ABC and abc differently
        assertTrue(map1.hashCode() != map3.hashCode());    // By design: case sensitive maps will [rightly] compute hash of ABC and abc differently
        assertTrue(map2.hashCode() == map3.hashCode());

        assertTrue(map1.equals(map2));
        assertTrue(map1.equals(map3));
        assertTrue(map3.equals(map1));
        assertTrue(map2.equals(map3));
    }

    // --------- Test returned keySet() operations ---------

    @Test
    public void testKeySetContains()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();
        assertTrue(s.contains("oNe"));
        assertTrue(s.contains("thRee"));
        assertTrue(s.contains("fiVe"));
        assertFalse(s.contains("dog"));
    }

    @Test
    public void testKeySetContainsAll()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();
        Set items = new HashSet();
        items.add("one");
        items.add("five");
        assertTrue(s.containsAll(items));
        items.add("dog");
        assertFalse(s.containsAll(items));
    }

    @Test
    public void testKeySetRemove()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();

        s.remove("Dog");
        assertEquals(3, m.size());
        assertEquals(3, s.size());

        assertTrue(s.remove("oNe"));
        assertTrue(s.remove("thRee"));
        assertTrue(s.remove("fiVe"));
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    public void testKeySetRemoveAll()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();
        Set items = new HashSet();
        items.add("one");
        items.add("five");
        assertTrue(s.removeAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("three"));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add("dog");
        s.removeAll(items);
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("three"));
        assertTrue(m.containsKey("three"));
    }

    @Test
    public void testKeySetRetainAll()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();
        Set items = new HashSet();
        items.add("three");
        assertTrue(s.retainAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("three"));
        assertTrue(m.containsKey("three"));

        m = createSimpleMap();
        s = m.keySet();
        items.clear();
        items.add("dog");
        items.add("one");
        assertTrue(s.retainAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains("one"));
        assertTrue(m.containsKey("one"));
    }

    @Test
    public void testKeySetToObjectArray()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();
        Object[] array = s.toArray();
        assertEquals(array[0], "One");
        assertEquals(array[1], "Three");
        assertEquals(array[2], "Five");
    }

    @Test
    public void testKeySetToTypedArray()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();
        String[] array = (String[]) s.toArray(new String[]{});
        assertEquals(array[0], "One");
        assertEquals(array[1], "Three");
        assertEquals(array[2], "Five");

        array = (String[]) s.toArray(new String[4]);
        assertEquals(array[0], "One");
        assertEquals(array[1], "Three");
        assertEquals(array[2], "Five");
        assertEquals(array[3], null);
        assertEquals(4, array.length);

        array = (String[]) s.toArray(new String[]{"","",""});
        assertEquals(array[0], "One");
        assertEquals(array[1], "Three");
        assertEquals(array[2], "Five");
        assertEquals(3, array.length);
    }

    @Test
    public void testKeySetClear()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();
        s.clear();
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    public void testKeySetHashCode()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();
        int h = s.hashCode();
        Set s2 = new HashSet();
        s2.add("One");
        s2.add("Three");
        s2.add("Five");
        assertNotEquals(h, s2.hashCode());

        s2 = new CaseInsensitiveSet();
        s2.add("One");
        s2.add("Three");
        s2.add("Five");
        assertEquals(h, s2.hashCode());
    }

    @Test
    public void testKeySetIteratorActions()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();
        Iterator i = s.iterator();
        Object o = i.next();
        assertTrue(o instanceof String);
        i.remove();
        assertEquals(2, m.size());
        assertEquals(2, s.size());

        o = i.next();
        assertTrue(o instanceof String);
        i.remove();
        assertEquals(1, m.size());
        assertEquals(1, s.size());

        o = i.next();
        assertTrue(o instanceof String);
        i.remove();
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    public void testKeySetEquals()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();

        Set s2 = new HashSet();
        s2.add("One");
        s2.add("Three");
        s2.add("Five");
        assertTrue(s2.equals(s));
        assertTrue(s.equals(s2));

        Set s3 = new HashSet();
        s3.add("one");
        s3.add("three");
        s3.add("five");
        assertTrue(s3.equals(s));
        assertTrue(s.equals(s3));

        Set s4 = new CaseInsensitiveSet();
        s4.add("one");
        s4.add("three");
        s4.add("five");
        assertTrue(s4.equals(s));
        assertTrue(s.equals(s4));
    }

    @Test
    public void testKeySetAddNotSupported()
    {
        Map m = createSimpleMap();
        Set s = m.keySet();
        try
        {
            s.add("Bitcoin");
            fail("should not make it here");
        }
        catch (UnsupportedOperationException e)
        { }

        Set items = new HashSet();
        items.add("Food");
        items.add("Water");

        try
        {
            s.addAll(items);
            fail("should not make it here");
        }
        catch (UnsupportedOperationException e)
        { }
    }

    // ---------------- returned Entry Set tests ---------

    @Test
    public void testEntrySetContains()
    {
        Map m = createSimpleMap();
        Set s = m.entrySet();
        assertTrue(s.contains(getEntry("one", "Two")));
        assertTrue(s.contains(getEntry("tHree", "Four")));
        assertFalse(s.contains(getEntry("one", "two")));    // Value side is case-sensitive (needs 'Two' not 'two')
    }

    @Test
    public void testEntrySetContainsAll()
    {
        Map m = createSimpleMap();
        Set s = m.entrySet();
        Set items = new HashSet();
        items.add(getEntry("one", "Two"));
        items.add(getEntry("thRee", "Four"));
        assertTrue(s.containsAll(items));

        items = new HashSet();
        items.add(getEntry("one", "two"));
        items.add(getEntry("thRee", "Four"));
        assertFalse(s.containsAll(items));
    }

    @Test
    public void testEntrySetRemove()
    {
        Map m = createSimpleMap();
        Set s = m.entrySet();

        assertFalse(s.remove(getEntry("Cat", "Six")));
        assertEquals(3, m.size());
        assertEquals(3, s.size());

        assertTrue(s.remove(getEntry("oNe", "Two")));
        assertTrue(s.remove(getEntry("thRee", "Four")));

        assertFalse(s.remove(getEntry("Dog", "Two")));
        assertEquals(1, m.size());
        assertEquals(1, s.size());

        assertTrue(s.remove(getEntry("fiVe", "Six")));
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    public void testEntrySetRemoveAll()
    {
        // Pure JDK test that fails
//        LinkedHashMap<String, Object> mm = new LinkedHashMap<String, Object>();
//        mm.put("One", "Two");
//        mm.put("Three", "Four");
//        mm.put("Five", "Six");
//        Set ss = mm.entrySet();
//        Set itemz = new HashSet();
//        itemz.add(getEntry("One", "Two"));
//        itemz.add(getEntry("Five", "Six"));
//        ss.removeAll(itemz);
//
//        itemz.clear();
//        itemz.add(getEntry("dog", "Two"));
//        assertFalse(ss.removeAll(itemz));
//        assertEquals(1, mm.size());
//        assertEquals(1, ss.size());
//        assertTrue(ss.contains(getEntry("Three", "Four")));
//        assertTrue(mm.containsKey("Three"));
//
//        itemz.clear();
//        itemz.add(getEntry("Three", "Four"));
//        assertTrue(ss.removeAll(itemz));  // fails - bug in JDK (Watching to see if this gets fixed)
//        assertEquals(0, mm.size());
//        assertEquals(0, ss.size());

        // Cedar Software code handles removeAll from entrySet perfectly
        Map m = createSimpleMap();
        Set s = m.entrySet();
        Set items = new HashSet();
        items.add(getEntry("one", "Two"));
        items.add(getEntry("five", "Six"));
        assertTrue(s.removeAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains(getEntry("three", "Four")));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add(getEntry("dog", "Two"));
        assertFalse(s.removeAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains(getEntry("three", "Four")));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add(getEntry("three", "Four"));
        assertTrue(s.removeAll(items));
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    public void testEntrySetRetainAll()
    {
        Map m = createSimpleMap();
        Set s = m.entrySet();
        Set items = new HashSet();
        items.add(getEntry("three", "Four"));
        assertTrue(s.retainAll(items));
        assertEquals(1, m.size());
        assertEquals(1, s.size());
        assertTrue(s.contains(getEntry("three", "Four")));
        assertTrue(m.containsKey("three"));

        items.clear();
        items.add("dog");
        assertTrue(s.retainAll(items));
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    public void testEntrySetToObjectArray()
    {
        Map m = createSimpleMap();
        Set s = m.entrySet();
        Object[] array = s.toArray();
        assertEquals(3, array.length);

        Map.Entry entry = (Map.Entry) array[0];
        assertEquals("One", entry.getKey());
        assertEquals("Two", entry.getValue());

        entry = (Map.Entry) array[1];
        assertEquals("Three", entry.getKey());
        assertEquals("Four", entry.getValue());

        entry = (Map.Entry) array[2];
        assertEquals("Five", entry.getKey());
        assertEquals("Six", entry.getValue());
    }

    @Test
    public void testEntrySetToTypedArray()
    {
        Map m = createSimpleMap();
        Set s = m.entrySet();
        Map.Entry[] array = (Map.Entry[]) s.toArray(new Map.Entry[]{});
        assertEquals(array[0], getEntry("One", "Two"));
        assertEquals(array[1], getEntry("Three", "Four"));
        assertEquals(array[2], getEntry("Five", "Six"));

        s = m.entrySet();   // Should not need to do this (JDK has same issue)
        array = (Map.Entry[]) s.toArray(new Map.Entry[4]);
        assertEquals(array[0], getEntry("One", "Two"));
        assertEquals(array[1], getEntry("Three", "Four"));
        assertEquals(array[2], getEntry("Five", "Six"));
        assertEquals(array[3], null);
        assertEquals(4, array.length);

        s = m.entrySet();
        array = (Map.Entry[]) s.toArray(new Map.Entry[]{getEntry(1, 1), getEntry(2, 2), getEntry(3, 3)});
        assertEquals(array[0], getEntry("One", "Two"));
        assertEquals(array[1], getEntry("Three", "Four"));
        assertEquals(array[2], getEntry("Five", "Six"));
        assertEquals(3, array.length);
    }

    @Test
    public void testEntrySetClear()
    {
        Map m = createSimpleMap();
        Set s = m.entrySet();
        s.clear();
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    public void testEntrySetHashCode()
    {
        Map m = createSimpleMap();
        Map m2 = new CaseInsensitiveMap();
        m2.put("one", "Two");
        m2.put("three", "Four");
        m2.put("five", "Six");
        assertEquals(m.hashCode(), m2.hashCode());

        Map m3 = new LinkedHashMap();
        m3.put("One", "Two");
        m3.put("Three", "Four");
        m3.put("Five", "Six");
        assertNotEquals(m.hashCode(), m3.hashCode());
    }

    @Test
    public void testEntrySetIteratorActions()
    {
        Map m = createSimpleMap();
        Set s = m.entrySet();
        Iterator i = s.iterator();
        Object o = i.next();
        assertTrue(o instanceof Map.Entry);
        i.remove();
        assertEquals(2, m.size());
        assertEquals(2, s.size());

        o = i.next();
        assertTrue(o instanceof Map.Entry);
        i.remove();
        assertEquals(1, m.size());
        assertEquals(1, s.size());

        o = i.next();
        assertTrue(o instanceof Map.Entry);
        i.remove();
        assertEquals(0, m.size());
        assertEquals(0, s.size());
    }

    @Test
    public void testEntrySetEquals()
    {
        Map m = createSimpleMap();
        Set s = m.entrySet();

        Set s2 = new HashSet();
        s2.add(getEntry("One", "Two"));
        s2.add(getEntry("Three", "Four"));
        s2.add(getEntry("Five", "Six"));
        assertTrue(s.equals(s2));

        s2.clear();
        s2.add(getEntry("One", "Two"));
        s2.add(getEntry("Three", "Four"));
        s2.add(getEntry("Five", "six"));    // lowercase six
        assertFalse(s.equals(s2));

        s2.clear();
        s2.add(getEntry("One", "Two"));
        s2.add(getEntry("Thre", "Four"));   // missing 'e' on three
        s2.add(getEntry("Five", "Six"));
        assertFalse(s.equals(s2));

        Set s3 = new HashSet();
        s3.add(getEntry("one", "Two"));
        s3.add(getEntry("three", "Four"));
        s3.add(getEntry("five","Six"));
        assertTrue(s.equals(s3));

        Set s4 = new CaseInsensitiveSet();
        s4.add(getEntry("one", "Two"));
        s4.add(getEntry("three", "Four"));
        s4.add(getEntry("five","Six"));
        assertTrue(s.equals(s4));

        CaseInsensitiveMap<String, Object> secondStringMap = createSimpleMap();
        assertFalse(s.equals("one"));

        assertTrue(s.equals(secondStringMap.entrySet()));
        // case-insensitive
        secondStringMap.put("five", "Six");
        assertTrue(s.equals(secondStringMap.entrySet()));
        secondStringMap.put("six", "sixty");
        assertFalse(s.equals(secondStringMap.entrySet()));
        secondStringMap.remove("five");
        assertFalse(s.equals(secondStringMap.entrySet()));
        secondStringMap.put("five", null);
        secondStringMap.remove("six");
        assertFalse(s.equals(secondStringMap.entrySet()));
        m.put("five", null);
        assertTrue(m.entrySet().equals(secondStringMap.entrySet()));

    }

    @Test
    public void testEntrySetAddNotSupport()
    {
        Map m = createSimpleMap();
        Set s = m.entrySet();

        try
        {
            s.add(10);
            fail("should not make it here");
        }
        catch (UnsupportedOperationException e)
        { }

        Set s2 = new HashSet();
        s2.add("food");
        s2.add("water");

        try
        {
            s.addAll(s2);
            fail("should not make it here");
        }
        catch (UnsupportedOperationException e)
        { }
    }

    // ---------------------------------------------------
    private CaseInsensitiveMap<String, Object> createSimpleMap()
    {
        CaseInsensitiveMap<String, Object> stringMap = new CaseInsensitiveMap<String, Object>();
        stringMap.put("One", "Two");
        stringMap.put("Three", "Four");
        stringMap.put("Five", "Six");
        return stringMap;
    }

    private Map.Entry getEntry(final Object key, final Object value)
    {
        return new Map.Entry()
        {
            Object myValue = value;

            public Object getKey()
            {
                return key;
            }

            public Object getValue()
            {
                return value;
            }

            public Object setValue(Object value)
            {
                Object save = myValue;
                myValue = value;
                return save;
            }
        };
    }
}
