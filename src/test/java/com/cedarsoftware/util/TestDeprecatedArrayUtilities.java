package com.cedarsoftware.util;

import com.cedarsoftware.test.Asserter;
import org.junit.Assert;
import org.junit.Test;

public class TestDeprecatedArrayUtilities
{

    @Test
    public void testConstructor() throws IllegalAccessException, InstantiationException
    {
        Asserter.assertClassOnlyHasAPrivateDefaultConstructor(ArrayUtilities.class);
    }

    @Test
    public void testIsEmpty()
    {
        Assert.assertTrue(ArrayUtilities.isEmpty(null));
        Assert.assertTrue(ArrayUtilities.isEmpty(new int[]{}));
        Assert.assertTrue(ArrayUtilities.isEmpty(new String[]{}));
        Assert.assertFalse(ArrayUtilities.isEmpty(new String[]{"foo"}));
        Assert.assertFalse(ArrayUtilities.isEmpty(new int[]{3}));
    }

    @Test
    public void testSize()
    {
        Assert.assertEquals(0, ArrayUtilities.size(null));
        Assert.assertEquals(0, ArrayUtilities.size(new int[] { }));
        Assert.assertEquals(0, ArrayUtilities.size(new String[] { }));
        Assert.assertEquals(1, ArrayUtilities.size(new String[] { "foo" }));
        Assert.assertEquals(1, ArrayUtilities.size(new int[]{1}));
        Assert.assertEquals(3, ArrayUtilities.size(new int[]{1, 2, 3}));
    }

    @Test
    public void testShallowCopy()
    {
        Object[] array1 = new Object[] { null, Integer.MAX_VALUE, Integer.MIN_VALUE, new Integer(3) };
        Object[] array2 = ArrayUtilities.shallowCopy(array1);

        Assert.assertNotSame(array1, array2);
        Assert.assertEquals(array1.length, array2.length);
        Assert.assertSame(array1[0], array2[0]);
        Assert.assertSame(array1[1], array2[1]);
        Assert.assertSame(array1[2], array2[2]);
        Assert.assertSame(array1[3], array2[3]);

        Assert.assertNull(ArrayUtilities.shallowCopy(null));
    }

    @Test
    public void testAddAll()
    {
        Integer[] array = new Integer[] { Integer.MAX_VALUE, null, Integer.MIN_VALUE, new Integer(0) };

        Assert.assertNull(ArrayUtilities.addAll(null, (Object[])null));
        Assert.assertArrayEquals(array, ArrayUtilities.addAll(null, array));
        Assert.assertArrayEquals(array, ArrayUtilities.addAll(array, (Integer[])null));
        Assert.assertArrayEquals(new Object[] { null, null, null }, ArrayUtilities.addAll(new Object[] { null }, new Object[] { null, null }));
        Assert.assertArrayEquals(new String[] { "a", "b", "c", "1", "2", "3" }, ArrayUtilities.addAll(new String[] { "a", "b", "c" }, new String[] { "1", "2", "3" }));
    }

    @Test
    public void testRemove()
    {
        Object[] array = new Integer[] { Integer.MAX_VALUE, null, Integer.MIN_VALUE, new Integer(0) };
        Object[] array1 = ArrayUtilities.removeItem(array, 3);
        Assert.assertArrayEquals(new Object[] { Integer.MAX_VALUE, null, Integer.MIN_VALUE }, array1);;
        Object[] array2 = ArrayUtilities.removeItem(array, 0);
        Assert.assertArrayEquals(new Object[] { null, Integer.MIN_VALUE, new Integer(0) }, array2);;
        Object[] array3 = ArrayUtilities.removeItem(array1, 1);
        Assert.assertArrayEquals(new Object[] { Integer.MAX_VALUE, Integer.MIN_VALUE }, array3);;
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testNegativeRemoveParameter()
    {
        Object[] array = new Object[] { Integer.MAX_VALUE, null, Integer.MIN_VALUE, new Integer(0) };
        ArrayUtilities.removeItem(array, -1);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testGreaterThanLengthRemoveParameter()
    {
        Object[] array = new Object[] { Integer.MAX_VALUE, null, Integer.MIN_VALUE, new Integer(0) };
        ArrayUtilities.removeItem(array, 4);
    }

    @Test
    public void testSubArray()
    {
        Integer[] array1 = new Integer[] { null, Integer.MAX_VALUE, Integer.MIN_VALUE, new Integer(3) };

        Assert.assertArrayEquals((Integer[])null, ArrayUtilities.getArraySubset(null, 1, 3));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.getArraySubset(array1, 3, 1));
        Assert.assertArrayEquals(new Integer[] { null }, ArrayUtilities.getArraySubset(array1, 0, 1));
        Assert.assertArrayEquals(new Integer[] { null, Integer.MAX_VALUE, Integer.MIN_VALUE }, ArrayUtilities.getArraySubset(array1, 0, 3));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.getArraySubset(new Integer[]{}, 0, 0));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.getArraySubset(array1, -1, 0));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.getArraySubset(array1, 0, -1));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.getArraySubset(array1, 4, 5));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.getArraySubset(array1, 4, 9));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.getArraySubset(array1, 9, 4));
    }

    @Test
    public void testGetArraySubset()
    {
        Assert.assertArrayEquals(null, ArrayUtilities.getArraySubset(null, 1, 2));
        Assert.assertArrayEquals(new String[] { "cde" }, ArrayUtilities.getArraySubset(new String[]{"abc", "cde", "efg"}, 1, 2));
    }

    @Test
    public void testGetSubset()
    {
        Assert.assertArrayEquals(null, ArrayUtilities.getArraySubset(null, 1, 2));
        Assert.assertArrayEquals(new String[] { "cde" }, ArrayUtilities.getArraySubset(new String[]{"abc", "cde", "efg"}, 1, 2));
    }

}
