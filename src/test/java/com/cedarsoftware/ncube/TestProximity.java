package com.cedarsoftware.ncube;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigInteger;

/**
 * Created by kpartlow on 4/9/2014.
 */
public class TestProximity
{
    @Test(expected=IllegalArgumentException.class)
    public void testInvalidArgument() {
        Proximity.distance(new BigInteger("9"), new BigInteger("11"));
    }

    @Test
    public void testConstructorIsPrivate() throws Exception {
        Class c = Proximity.class;
        Assert.assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<Proximity> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
    }
}
