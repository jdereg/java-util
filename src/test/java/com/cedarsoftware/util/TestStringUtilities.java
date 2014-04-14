package com.cedarsoftware.util;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

public class TestStringUtilities
{
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Class c = StringUtilities.class;
        Assert.assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<StringUtilities> con = c.getDeclaredConstructor();
        Assert.assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        Assert.assertNotNull(con.newInstance());
    }

    @Test
    public void testIsEmpty()
    {
        Assert.assertTrue(StringUtilities.isEmpty(null));
        Assert.assertTrue(StringUtilities.isEmpty(""));
        Assert.assertFalse(StringUtilities.isEmpty("foo"));
    }

    @Test
    public void testHasContent() {
        Assert.assertFalse(StringUtilities.hasContent(null));
        Assert.assertFalse(StringUtilities.hasContent(""));
        Assert.assertTrue(StringUtilities.hasContent("foo"));
    }

    @Test
    public void testTrimLength() {
        Assert.assertEquals(0, StringUtilities.trimLength(null));
        Assert.assertEquals(0, StringUtilities.trimLength(""));
        Assert.assertEquals(3, StringUtilities.trimLength("  abc "));

        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("abc", " Abc "));
        Assert.assertTrue(StringUtilities.equalsWithTrim("abc", " abc "));
        Assert.assertEquals(2, StringUtilities.count("abcabc", 'a'));
    }

    @Test
    public void testEqualsWithTrim() {
        Assert.assertTrue(StringUtilities.equalsWithTrim("abc", " abc "));
        Assert.assertTrue(StringUtilities.equalsWithTrim(" abc ", "abc"));
        Assert.assertFalse(StringUtilities.equalsWithTrim("abc", " AbC "));
        Assert.assertFalse(StringUtilities.equalsWithTrim(" AbC ", "abc"));
        Assert.assertFalse(StringUtilities.equalsWithTrim(null, ""));
        Assert.assertFalse(StringUtilities.equalsWithTrim("", null));
        Assert.assertTrue(StringUtilities.equalsWithTrim("", "\t\n\r"));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("abc", " abc "));
        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(" abc ", "abc"));
        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("abc", " AbC "));
        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(" AbC ", "abc"));
        Assert.assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(null, ""));
        Assert.assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("", null));
        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("", "\t\n\r"));
    }

    @Test
    public void testCount() {
        Assert.assertEquals(2, StringUtilities.count("abcabc", 'a'));
        Assert.assertEquals(0, StringUtilities.count("foo", 'a'));
        Assert.assertEquals(0, StringUtilities.count(null, 'a'));
        Assert.assertEquals(0, StringUtilities.count("", 'a'));
    }

    @Test
    public void testEncode() {
        Assert.assertEquals("1A", StringUtilities.encode(new byte[]{0x1A}));
        Assert.assertEquals("", StringUtilities.encode(new byte[]{}));
    }

    @Test(expected=NullPointerException.class)
    public void testEncodeWithNull()
    {
        StringUtilities.encode(null);
    }

    @Test
    public void testDecode() {
        Assert.assertArrayEquals(new byte[]{0x1A}, StringUtilities.decode("1A"));
        Assert.assertArrayEquals(new byte[]{}, StringUtilities.decode(""));
        Assert.assertNull(StringUtilities.decode("1AB"));
    }

    @Test(expected=NullPointerException.class)
    public void testDecodeWithNull()
    {
        StringUtilities.decode(null);
    }

    @Test
    public void testEquals()
    {
        Assert.assertTrue(StringUtilities.equals(null, null));
        Assert.assertFalse(StringUtilities.equals(null, ""));
        Assert.assertFalse(StringUtilities.equals("", null));
        Assert.assertFalse(StringUtilities.equals("foo", "bar"));
        Assert.assertFalse(StringUtilities.equals("Foo", "foo"));
        Assert.assertTrue(StringUtilities.equals("foo", "foo"));
    }

    @Test
    public void testEqualsIgnoreCase()
    {
        Assert.assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        Assert.assertFalse(StringUtilities.equalsIgnoreCase(null, ""));
        Assert.assertFalse(StringUtilities.equalsIgnoreCase("", null));
        Assert.assertFalse(StringUtilities.equalsIgnoreCase("foo", "bar"));
        Assert.assertTrue(StringUtilities.equalsIgnoreCase("Foo", "foo"));
        Assert.assertTrue(StringUtilities.equalsIgnoreCase("foo", "foo"));
    }


    @Test
    public void testLastIndexOf()
    {
        Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, 'a'));
        Assert.assertEquals(-1, StringUtilities.lastIndexOf("foo", 'a'));
        Assert.assertEquals(1, StringUtilities.lastIndexOf("bar", 'a'));
    }

    @Test
    public void testLength() {
        Assert.assertEquals(0, StringUtilities.length(""));
        Assert.assertEquals(0, StringUtilities.length(null));
        Assert.assertEquals(3, StringUtilities.length("abc"));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testGetBytesWithInvalidEncoding() {
        StringUtilities.getBytes("foo", "foo");
    }

    @Test
    public void testGetBytes() {
        Assert.assertArrayEquals(new byte[] {102, 111, 111}, StringUtilities.getBytes("foo", "UTF-8"));
    }

}
