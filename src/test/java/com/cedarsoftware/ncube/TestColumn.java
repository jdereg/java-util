package com.cedarsoftware.ncube;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by kpartlow on 8/6/2014.
 */
public class TestColumn
{
    @Test
    public void testSetValue() {
        Column c = new Column(5, true);
        assertEquals(0, c.getValue());
        c.setValue(5);
        assertEquals(5, c.getValue());
    }

    @Test
    public void testMetaProperties() {
        Column c = new Column(5, true);
        assertNull(c.getMetaProperties().get("foo"));

        c.clearMetaProperties();
        c.setMetaProperty("foo", "bar");
        assertEquals("bar", c.getMetaProperties().get("foo"));

        c.clearMetaProperties();
        assertNull(c.getMetaProperties().get("foo"));

        c.clearMetaProperties();
        Map map = new HashMap();
        map.put("BaZ", "qux");

        c.addMetaProperties(map);
        assertEquals("qux", c.getMetaProperties().get("baz"));
    }



}
