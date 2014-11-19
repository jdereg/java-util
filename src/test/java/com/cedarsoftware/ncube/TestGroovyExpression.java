package com.cedarsoftware.ncube;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Created by kpartlow on 11/18/2014.
 */
public class TestGroovyExpression
{
    @Test
    public void testDefaultConstructorIsPrivateForSerialization() throws Exception {
        Class c = GroovyExpression.class;
        Constructor<GroovyExpression> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);
        Assert.assertNotNull(con.newInstance());
    }

}
