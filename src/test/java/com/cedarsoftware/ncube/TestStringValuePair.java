package com.cedarsoftware.ncube;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by kpartlow on 10/24/2014.
 */
public class TestStringValuePair
{
    @Test
    public void testStringValuePair() {
        StringValuePair one = new StringValuePair("foo", "bar");
        StringValuePair two = new StringValuePair("boo", "far");

        assertEquals("foo", one.getKey());
        assertEquals("bar", one.getValue());

        assertEquals("boo", two.getKey());
        assertEquals("far", two.getValue());

        assertEquals("foo:bar", one.toString());

        assertNotEquals(one, two);
        two.setValue("bar");
        assertNotEquals(one, two);
        two.setKey("foo");
        assertEquals(one, two);
        assertNotEquals(one, new Integer(3));

        // keys only need to be the same.
        two.setValue("far");
        assertEquals(one, two);

        assertEquals(two.getKey().hashCode(), two.hashCode());

        two.setKey(null);
        assertEquals(0xbabe, two.hashCode());
    }
}
