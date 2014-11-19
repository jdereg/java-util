package com.cedarsoftware.ncube;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Created by kpartlow on 11/18/2014.
 */
public class TestStringUrlCmd
{
    @Test
    public void testDefaultConstructorIsPrivateForSerialization() throws Exception {
        Class c = StringUrlCmd.class;
        Constructor<StringUrlCmd> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);
        Assert.assertNotNull(con.newInstance());
    }

}
