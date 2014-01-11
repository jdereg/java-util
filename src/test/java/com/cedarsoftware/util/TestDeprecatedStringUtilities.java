package com.cedarsoftware.util;

import com.cedarsoftware.test.Asserter;
import org.junit.Assert;
import org.junit.Test;

public class TestDeprecatedStringUtilities
{

    @Test
    public void testConstructor() throws IllegalAccessException, InstantiationException
    {
        Asserter.assertClassOnlyHasAPrivateDefaultConstructor(StringUtilities.class);
    }

    @Test
    public void testString()
    {
        Assert.assertTrue(StringUtilities.isEmpty(null));
        Assert.assertFalse(StringUtilities.hasContent(null));
        Assert.assertEquals(0, StringUtilities.trimLength(null));
        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("abc", " Abc "));
        Assert.assertTrue(StringUtilities.equalsWithTrim("abc", " abc "));
        Assert.assertEquals("1A", StringUtilities.encode(new byte[] {0x1A}));
        Assert.assertArrayEquals(new byte[] {0x1A}, StringUtilities.decode("1A"));
        Assert.assertEquals(2, StringUtilities.count("abcabc", 'a'));
    }

    @Test
    public void testTrim()
    {
        Assert.assertEquals(null, StringUtilities.trim(null));
        Assert.assertEquals("", StringUtilities.trim(""));
    }

    @Test
    public void testEquals()
    {
        Assert.assertTrue(StringUtilities.equals(null, null));
    }

    @Test
    public void testEqualsIgnoreCase()
    {
        Assert.assertTrue(StringUtilities.equalsIgnoreCase(null, null));
    }


    @Test
    public void testLastIndexOf()
    {
        Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, 'a'));
    }

    @Test
    public void testLength() {
        Assert.assertEquals(0, StringUtilities.length(""));
        Assert.assertEquals(0, StringUtilities.length(null));
        Assert.assertEquals(3, StringUtilities.length("abc"));
    }


}
