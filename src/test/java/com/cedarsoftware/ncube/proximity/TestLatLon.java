package com.cedarsoftware.ncube.proximity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by kpartlow on 4/10/2014.
 */
public class TestLatLon
{
    @Test
    public void testEquals() {
        LatLon l = new LatLon(1.0, 2.0);
        assertNotEquals(l, new Long(5));
    }

    @Test
    public void testCompareTo() {
        LatLon l = new LatLon(10.0, 10.0);
        assertTrue(l.compareTo(new LatLon(1, 10)) > 0);
        assertTrue(l.compareTo(new LatLon(10, 0)) > 0);
        assertTrue(l.compareTo(new LatLon(20, 10)) < 0);
        assertTrue(l.compareTo(new LatLon(10, 20)) < 0);
        assertTrue(l.compareTo(new LatLon(10, 10)) == 0);

        assertEquals("10.0, 10.0", l.toString());
    }
}
