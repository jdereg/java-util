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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
            if (key.equals("One")) foundOne = true;
            if (key.equals("thREe")) foundThree = true;
            if (key.equals("Five")) foundFive = true;
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
            if (key.equals("One") && value.equals("Two")) foundOne = true;
            if (key.equals("thREe") && value.equals("four")) foundThree = true;
            if (key.equals("Five") && value.equals("Six")) foundFive = true;
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
        assertTrue(a.hashCode() == b.hashCode());

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

        assertTrue(map1.hashCode() != map2.hashCode());    // By design: case sensitive maps will [rightly] compute has of ABC and abc differently
        assertTrue(map1.hashCode() != map3.hashCode());    // By design: case sensitive maps will [rightly] compute has of ABC and abc differently
        assertTrue(map2.hashCode() == map3.hashCode());

        assertTrue(map1.equals(map2));
        assertTrue(map1.equals(map3));
        assertTrue(map3.equals(map1));
        assertTrue(map2.equals(map3));
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

}
