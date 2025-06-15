package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Kenneth Partlow
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
public class MapUtilitiesTest
{
    @Test
    public void testMapUtilitiesConstructor() throws Exception
    {
        Constructor con = MapUtilities.class.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    @Test
    public void testGetWithWrongType()
    {
        Map<String, Object> map = new TreeMap<>();
        map.put("foo", Boolean.TRUE);
        try
        {
            String s = (String) MapUtilities.get(map, "foo", null);
            fail("should not make it here");
        }
        catch (ClassCastException ignored)
        {
        }
    }

    @Test
    public void testGet() {
        Map<Object, Object> map = new HashMap<>();
        assertEquals("bar", MapUtilities.get(map, "baz", "bar"));
        assertEquals(7, (long) MapUtilities.get(map, "baz", 7L));
        assertEquals(Long.valueOf(7), MapUtilities.get(map, "baz", 7L));

        // auto boxing tests
        assertEquals(Boolean.TRUE, (Boolean)MapUtilities.get(map, "baz", true));
        assertEquals(true, MapUtilities.get(map, "baz", Boolean.TRUE));

        map.put("foo", "bar");
        assertEquals("bar", MapUtilities.get(map, "foo", null));

        map.put("foo", 5L);
        assertEquals(5, (long)MapUtilities.get(map, "foo", 9L));

        map.put("foo", 9L);
        assertEquals(9L, MapUtilities.get(map, "foo", null));

    }

    @Test
    public void testIsEmpty()
    {
        assertTrue(MapUtilities.isEmpty(null));

        Map<String, Object> map = new HashMap<>();
        assertTrue(MapUtilities.isEmpty(new HashMap<>()));

        map.put("foo", "bar");
        assertFalse(MapUtilities.isEmpty(map));
    }

    @Test
    public void testGetOrThrow()
    {
        Map<String, Object> map = new TreeMap<>();
        map.put("foo", Boolean.TRUE);
        map.put("bar", null);
        Object value = MapUtilities.getOrThrow(map, "foo", new RuntimeException("garply"));
        assert (boolean)value;

        value = MapUtilities.getOrThrow(map, "bar", new RuntimeException("garply"));
        assert null == value;

        try
        {
            MapUtilities.getOrThrow(map, "baz", new RuntimeException("garply"));
            fail("Should not make it here");
        }
        catch (RuntimeException e)
        {
            assert e.getMessage().equals("garply");
        }
    }

    @Test
    public void testCloneMapOfSetsMutable()
    {
        Map<String, Set<Integer>> original = new LinkedHashMap<>();
        original.put("a", new LinkedHashSet<>(Arrays.asList(1, 2)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);

        assertEquals(original, clone);
        assertNotSame(original, clone);
        assertNotSame(original.get("a"), clone.get("a"));

        clone.get("a").add(3);
        assertFalse(original.get("a").contains(3));
    }

    @Test
    public void testCloneMapOfSetsImmutable()
    {
        Map<String, Set<Integer>> original = new HashMap<>();
        Set<Integer> set = new HashSet<>(Arrays.asList(5));
        original.put("x", set);

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);

        assertThrows(UnsupportedOperationException.class, () -> clone.put("y", new HashSet<>()));
        assertThrows(UnsupportedOperationException.class, () -> clone.get("x").add(6));

        set.add(7);
        assertTrue(clone.get("x").contains(7));
    }

    @Test
    public void testCloneMapOfMapsMutable()
    {
        Map<String, Map<String, Integer>> original = new LinkedHashMap<>();
        Map<String, Integer> inner = new LinkedHashMap<>();
        inner.put("a", 1);
        original.put("first", inner);

        Map<String, Map<String, Integer>> clone = MapUtilities.cloneMapOfMaps(original, false);

        assertEquals(original, clone);
        assertNotSame(original, clone);
        assertNotSame(inner, clone.get("first"));

        clone.get("first").put("b", 2);
        assertFalse(inner.containsKey("b"));
    }

    @Test
    public void testCloneMapOfMapsImmutable()
    {
        Map<String, Map<String, Integer>> original = new HashMap<>();
        Map<String, Integer> inner = new HashMap<>();
        inner.put("n", 9);
        original.put("num", inner);

        Map<String, Map<String, Integer>> clone = MapUtilities.cloneMapOfMaps(original, true);

        assertThrows(UnsupportedOperationException.class, () -> clone.put("z", new HashMap<>()));
        assertThrows(UnsupportedOperationException.class, () -> clone.get("num").put("m", 10));

        inner.put("p", 11);
        assertTrue(clone.get("num").containsKey("p"));
    }

    @Test
    public void testDupeMutable()
    {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        Set<String> vals = new LinkedHashSet<>(Arrays.asList("A"));
        original.put(String.class, vals);

        Map<Class<?>, Set<String>> clone = MapUtilities.dupe(original, false);

        assertEquals(original, clone);
        assertNotSame(original.get(String.class), clone.get(String.class));

        clone.get(String.class).add("B");
        assertFalse(original.get(String.class).contains("B"));
    }

    @Test
    public void testDupeImmutable()
    {
        Map<Class<?>, Set<String>> original = new HashMap<>();
        Set<String> set = new HashSet<>(Arrays.asList("X"));
        original.put(Integer.class, set);

        Map<Class<?>, Set<String>> clone = MapUtilities.dupe(original, true);

        assertThrows(UnsupportedOperationException.class, () -> clone.put(String.class, new HashSet<>()));
        assertThrows(UnsupportedOperationException.class, () -> clone.get(Integer.class).add("Y"));

        set.add("Z");
        assertFalse(clone.get(Integer.class).contains("Z"));
    }

    @Test
    public void testMapOfEntries()
    {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("a", 1);
        Map.Entry<String, Integer> e2 = new AbstractMap.SimpleEntry<>("b", 2);

        Map<String, Integer> map = MapUtilities.mapOfEntries(e1, e2);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertThrows(UnsupportedOperationException.class, () -> map.put("c", 3));

        assertThrows(NullPointerException.class, () -> MapUtilities.mapOfEntries(e1, null));
    }

    @Test
    public void testMapToString()
    {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 1);
        map.put("y", 2);
        assertEquals("{x=1, y=2}", MapUtilities.mapToString(map));

        Map<String, Object> self = new HashMap<>();
        self.put("self", self);
        assertEquals("{self=(this Map)}", MapUtilities.mapToString(self));

        assertEquals("{}", MapUtilities.mapToString(new HashMap<>()));
    }
}
