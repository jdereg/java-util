package com.cedarsoftware.util;


import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

public class TestExceptionUtilities
{
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Constructor<ExceptionUtilities> con = ExceptionUtilities.class.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
    }

    @Test(expected=ThreadDeath.class)
    public void testThreadDeathThrown() {
        ExceptionUtilities.safelyIgnoreException(new ThreadDeath());
    }

    @Test(expected=OutOfMemoryError.class)
    public void testOutOfMemoryErrorThrown() {
        ExceptionUtilities.safelyIgnoreException(new OutOfMemoryError());
    }

    @Test
    public void testIgnoredExceptions() {
        ExceptionUtilities.safelyIgnoreException(new IllegalArgumentException());
    }
}
