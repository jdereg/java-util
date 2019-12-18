package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.fail;

/**
 * @author Kenneth Partlow
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
public class TestMapUtilities
{
    @Test
    public void testMapUtilitiesConstructor() throws Exception
    {
        Constructor<MapUtilities> con = MapUtilities.class.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
    }

    @Test(expected = ClassCastException.class)
    public void testGetWithWrongType() {
        Map map = new TreeMap();
        map.put("foo", Boolean.TRUE);
        String s = (String) MapUtilities.get(map, "foo", null);
    }

    @Test
    public void testGet() {
        Map map = new HashMap();
        Assert.assertEquals("bar", MapUtilities.get(map, "baz", "bar"));
        Assert.assertEquals(7, (long) MapUtilities.get(map, "baz", 7));
        Assert.assertEquals(new Long(7), MapUtilities.get(map, "baz", 7L));

        // auto boxing tests
        Assert.assertEquals(Boolean.TRUE, (Boolean)MapUtilities.get(map, "baz", true));
        Assert.assertEquals(true, MapUtilities.get(map, "baz", Boolean.TRUE));

        map.put("foo", "bar");
        Assert.assertEquals("bar", MapUtilities.get(map, "foo", null));

        map.put("foo", 5);
        Assert.assertEquals(5, (long)MapUtilities.get(map, "foo", 9));

        map.put("foo", 9L);
        Assert.assertEquals(new Long(9), MapUtilities.get(map, "foo", null));

    }

    @Test
    public void testIsEmpty()
    {
        Assert.assertTrue(MapUtilities.isEmpty(null));

        Map map = new HashMap();
        Assert.assertTrue(MapUtilities.isEmpty(new HashMap()));

        map.put("foo", "bar");
        Assert.assertFalse(MapUtilities.isEmpty(map));
    }

    @Test
    public void testGetOrThrow()
    {
        Map map = new TreeMap();
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
