package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * Created by Kenny on 4/14/2014.
 */
public class TestArrayUtilities
{
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Class c = ArrayUtilities.class;
        Assert.assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<ArrayUtilities> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
    }

    @Test
    public void testIsEmpty() {
        Assert.assertTrue(ArrayUtilities.isEmpty(new byte[] {}));
        Assert.assertTrue(ArrayUtilities.isEmpty(null));
        Assert.assertFalse(ArrayUtilities.isEmpty(new byte[] {5}));
    }

    @Test
    public void testSize() {
        Assert.assertEquals(0, ArrayUtilities.size(new byte[] {}));
        Assert.assertEquals(0, ArrayUtilities.size(null));
        Assert.assertEquals(1, ArrayUtilities.size(new byte[] {5}));
    }

    @Test
    public void testAddAll() {
        Assert.assertEquals(0, ArrayUtilities.size(new byte[] {}));

        //  Test One
        Long[] one = new Long[] { new Long(1), new Long(2) };
        Object[] resultOne = ArrayUtilities.addAll(null, one);
        Assert.assertNotSame(one, resultOne);
        for (int i=0; i<one.length; i++) {
            Assert.assertSame(one[i], resultOne[i]);
        }

        //  Test Two
        Long[] two = new Long[] { new Long(3), new Long(4) };
        Object[] resultTwo = ArrayUtilities.addAll(two, null);
        Assert.assertNotSame(two, resultTwo);
        for (int i=0; i<two.length; i++) {
            Assert.assertSame(two[i], resultTwo[i]);
        }

        // Test Three
        Object[] resultThree = ArrayUtilities.addAll(one, two);
        Assert.assertNotSame(one, resultThree);
        Assert.assertNotSame(two, resultThree);
        for (int i=0; i<one.length; i++) {
            Assert.assertSame(one[i], resultThree[i]);
        }
        for (int i=0; i<two.length; i++) {
            Assert.assertSame(two[i], resultThree[i+one.length]);
        }
    }

    @Test(expected=ArrayStoreException.class)
    public void InvalidClassDuringAddAll() {
        Long[] one = new Long[] { new Long(1), new Long(2)};
        String[] two = new String[] {"foo", "bar"};

        ArrayUtilities.addAll(one, two);
    }
}
