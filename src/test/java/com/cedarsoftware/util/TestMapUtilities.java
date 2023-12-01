package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
public class TestMapUtilities
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
}
