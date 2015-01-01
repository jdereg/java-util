package com.cedarsoftware.ncube

import org.junit.Test

import java.lang.reflect.Constructor
import java.lang.reflect.Modifier

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull

/**
 * Created by kpartlow on 4/9/2014.
 */
public class TestProximity
{
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidArgument()
    {
        Proximity.distance new BigInteger('9'), new BigInteger('11')
    }

    @Test
    public void testConstructorIsPrivate() throws Exception
    {
        Class c = Proximity.class;
        assertEquals Modifier.FINAL, c.modifiers & Modifier.FINAL

        Constructor<Proximity> con = c.getDeclaredConstructor();
        assertEquals Modifier.PRIVATE, con.modifiers & Modifier.PRIVATE
        con.accessible = true;

        assertNotNull con.newInstance()
    }
}
