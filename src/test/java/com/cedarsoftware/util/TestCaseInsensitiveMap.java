package com.cedarsoftware.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.junit.Test;

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

    private CaseInsensitiveMap<String, Object> createSimpleMap()
    {
        CaseInsensitiveMap<String, Object> stringMap = new CaseInsensitiveMap<String, Object>();
        stringMap.put("One", "Two");
        stringMap.put("Three", "Four");
        stringMap.put("Five", "Six");
        return stringMap;
    }

}
