package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by kpartlow on 3/20/2014.
 */

public class TestMapUtilities
{
    @Test
    public void testMapUtilitiesConstructor() throws Exception
    {
        Class c = MapUtilities.class;
        Assert.assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<MapUtilities> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
    }

    @Test(expected = ClassCastException.class)
    public void testGetStringOnMapThatDoesNotSupportNullKeys() {
        Map map = new TreeMap();
        map.put("foo", Boolean.TRUE);

        MapUtilities.getString(map, "foo");
    }

    @Test
    public void testGetString() {
        Map map = new HashMap();
        Assert.assertNull(MapUtilities.getString(map, "foo"));
        Assert.assertEquals("bar", MapUtilities.getString(map, "foo", "bar"));

        map.put("foo", "bar");

        Assert.assertEquals("bar", MapUtilities.getString(map, "foo"));
    }

    @Test(expected = ClassCastException.class)
    public void testGetLongOnMapThatDoesNotSupportNullKeys() {
        Map map = new TreeMap();
        map.put("foo", Boolean.TRUE);

        MapUtilities.getLong(map, "foo");
    }

    @Test
    public void testGetLong() {
        Map map = new HashMap();
        Assert.assertNull(MapUtilities.getLong(map, "foo"));
        Assert.assertEquals((Object)Long.MIN_VALUE, MapUtilities.getLong(map, "foo", Long.MIN_VALUE));

        map.put("foo", Long.MAX_VALUE);

        Assert.assertEquals((Object)Long.MAX_VALUE, MapUtilities.getLong(map, "foo"));
    }
}
