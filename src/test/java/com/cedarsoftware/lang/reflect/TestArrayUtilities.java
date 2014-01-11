package com.cedarsoftware.lang.reflect;

import com.cedarsoftware.test.Asserter;
import org.junit.Assert;
import org.junit.Test;

public class TestArrayUtilities
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
        Assert.assertTrue(ArrayUtilities.isEmpty(new int[] { }));
        Assert.assertTrue(ArrayUtilities.isEmpty(new String[] { }));
        Assert.assertFalse(ArrayUtilities.isEmpty(new String[] { "foo" }));
        Assert.assertFalse(ArrayUtilities.isEmpty(new int[] { 3 }));
    }

    @Test
    public void testHasContent()
    {
        Assert.assertFalse(ArrayUtilities.hasContent(null));
        Assert.assertFalse(ArrayUtilities.hasContent(new int[] { }));
        Assert.assertFalse(ArrayUtilities.hasContent(new String[] { }));
        Assert.assertTrue(ArrayUtilities.hasContent(new String[] { "foo" }));
        Assert.assertTrue(ArrayUtilities.hasContent(new int[] { 3 }));
    }

    @Test
    public void testSize()
    {
        Assert.assertEquals(0, ArrayUtilities.size(null));
        Assert.assertEquals(0, ArrayUtilities.size(new int[] { }));
        Assert.assertEquals(0, ArrayUtilities.size(new String[] { }));
        Assert.assertEquals(1, ArrayUtilities.size(new String[] { "foo" }));
        Assert.assertEquals(1, ArrayUtilities.size(new int[] { 1 }));
        Assert.assertEquals(3, ArrayUtilities.size(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testToArray()
    {
        Assert.assertArrayEquals(new Integer[] { 1, 2, 3 }, ArrayUtilities.toArray(1, 2, 3));
        Assert.assertArrayEquals((Object[])null, ArrayUtilities.toArray((Object[])null));
    }

    @Test
    public void testShallowCopy()
    {
        Integer[] array1 = new Integer[] { null, Integer.MAX_VALUE, Integer.MIN_VALUE, new Integer(3) };
        Integer[] array2 = ArrayUtilities.shallowCopy(array1);

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
        Integer[] array = new Integer[] { Integer.MAX_VALUE, null, Integer.MIN_VALUE, 0 };
        Integer[] array1 = ArrayUtilities.remove(array, 3);
        Assert.assertArrayEquals(new Integer[] { Integer.MAX_VALUE, null, Integer.MIN_VALUE }, array1);;
        Integer[] array2 = ArrayUtilities.remove(array, 0);
        Assert.assertArrayEquals(new Integer[] { null, Integer.MIN_VALUE, 0 }, array2);;
        Integer[] array3 = ArrayUtilities.remove(array1, 1);
        Assert.assertArrayEquals(new Integer[] { Integer.MAX_VALUE, Integer.MIN_VALUE }, array3);;
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testNegativeRemoveParameter()
    {
        Integer[] array = new Integer[] { Integer.MAX_VALUE, null, Integer.MIN_VALUE, 0 };
        ArrayUtilities.remove(array, -1);
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void testGreaterThanLengthRemoveParameter()
    {
        Integer[] array = new Integer[] { Integer.MAX_VALUE, null, Integer.MIN_VALUE, 0};
        ArrayUtilities.remove(array, 4);
    }

    @Test
    public void testSubArray()
    {
        Integer[] array1 = new Integer[] { null, Integer.MAX_VALUE, Integer.MIN_VALUE, 3 };

        Assert.assertArrayEquals(null, ArrayUtilities.createSubarray(null, 1, 3));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.createSubarray(array1, 3, 1));
        Assert.assertArrayEquals(new Integer[] { null }, ArrayUtilities.createSubarray(array1, 0, 1));
        Assert.assertArrayEquals(new Integer[] { null, Integer.MAX_VALUE, Integer.MIN_VALUE }, ArrayUtilities.createSubarray(array1, 0, 3));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.createSubarray(new Integer[] { }, 0, 0));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.createSubarray(array1, -1, 0));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.createSubarray(array1, 0, -1));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.createSubarray(array1, 4, 5));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.createSubarray(array1, 4, 9));
        Assert.assertArrayEquals(new Integer[] { }, ArrayUtilities.createSubarray(array1, 9, 4));
    }

    @Test
    public void testGetArraySubset()
    {
        Assert.assertArrayEquals(null, ArrayUtilities.createSubarray(null, 1, 2));
        Assert.assertArrayEquals(new String[] { "cde" }, ArrayUtilities.createSubarray(new String[] { "abc", "cde", "efg" }, 1, 2));
    }

    @Test
    public void testGetSubset()
    {
        Assert.assertArrayEquals(null, ArrayUtilities.createSubarray(null, 1, 2));
        Assert.assertArrayEquals(new String[] { "cde" }, ArrayUtilities.createSubarray(new String[] { "abc", "cde", "efg" }, 1, 2));
    }

}
