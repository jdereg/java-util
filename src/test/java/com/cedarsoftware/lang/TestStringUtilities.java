package com.cedarsoftware.lang;

import com.cedarsoftware.test.Asserter;
import org.junit.Assert;
import org.junit.Test;

public class TestStringUtilities
{

	@Test
	public void testConstructor() throws IllegalAccessException, InstantiationException
	{
        Asserter.assertClassOnlyHasAPrivateDefaultConstructor(StringUtilities.class);
	}

    private String getTrimmableString() {
        StringBuilder b = new StringBuilder();
        for (int i=0; i<33; i++) {
            b.appendCodePoint(i);
        }
        return b.toString();
    }

    private String getStrippedCharactersUnder32(boolean stripStart, boolean stripEnd) {
        StringBuilder b = new StringBuilder();
        boolean start = stripStart;
        StringBuilder ws = new StringBuilder();
        for (int i=0; i<33; i++) {
            if (start) {
                if (!Character.isWhitespace(i)) {
                    ws.appendCodePoint(i);
                    start = false;
                }
            } else if (Character.isWhitespace(i)) {
                ws.appendCodePoint(i);
            } else {
                b.append(ws.toString());
                ws.setLength(0);
                b.appendCodePoint(i);
            }
        }
        if (!stripEnd) {
            b.append(ws.toString());
        }
        return b.toString();
    }


    public String getWhiteSpaceString() {
        StringBuilder b = new StringBuilder();
        for (int i=0; i<33; i++) {
            b.appendCodePoint(i);
        }
        return b.toString();
    }

	@Test
	public void testTrim()
	{
		Assert.assertEquals(null, StringUtilities.trim(null));
		Assert.assertEquals("", StringUtilities.trim(""));
		Assert.assertEquals("", StringUtilities.trim(" "));
		Assert.assertEquals("", StringUtilities.trim("\t\n\r\f"));
        Assert.assertEquals("", StringUtilities.trim(getTrimmableString()));
		Assert.assertEquals("", StringUtilities.trim("\u0009\u000B\u000C\u001D\u001E\u001F"));
		Assert.assertEquals("foo", StringUtilities.trim("foo"));
		Assert.assertEquals("foo", StringUtilities.trim(" foo "));
	}

	@Test
	public void testTrimToNull()
	{
		Assert.assertEquals(null, StringUtilities.trimToNull(null));
		Assert.assertEquals(null, StringUtilities.trimToNull(""));
		Assert.assertEquals(null, StringUtilities.trimToNull(" "));
		Assert.assertEquals(null, StringUtilities.trimToNull("\t\n\r\f"));
		Assert.assertEquals(null, StringUtilities.trimToNull("\u0009\u000B\u000C\u001D\u001E\u001F"));
		Assert.assertEquals("foo", StringUtilities.trimToNull("foo"));
		Assert.assertEquals("foo", StringUtilities.trimToNull(" foo "));
	}

	@Test
	public void testTrimToEmpty()
	{
		Assert.assertEquals("", StringUtilities.trimToEmpty(null));
		Assert.assertEquals("", StringUtilities.trimToEmpty(""));
		Assert.assertEquals("", StringUtilities.trimToEmpty(" "));
		Assert.assertEquals("", StringUtilities.trimToEmpty("\t\n\r\f"));
		Assert.assertEquals("", StringUtilities.trimToEmpty("\u0009\u000B\u000C\u001D\u001E\u001F"));
		Assert.assertEquals("foo", StringUtilities.trimToEmpty("foo"));
		Assert.assertEquals("foo", StringUtilities.trimToEmpty(" foo "));
	}

	@Test
	public void testEquals()
	{
		Assert.assertTrue(StringUtilities.equals(null, null));
		Assert.assertFalse(StringUtilities.equals(null, ""));
		Assert.assertFalse(StringUtilities.equals("", null));
		Assert.assertFalse(StringUtilities.equals("", " "));
		Assert.assertTrue(StringUtilities.equals("", ""));
		Assert.assertTrue(StringUtilities.equals("\t\n\r\f", "\t\n\r\f"));
		Assert.assertFalse(StringUtilities.equals("\t\n\r\f", ""));
		Assert.assertTrue(StringUtilities.equals("\u0009\u000B\u000C\u001D\u001E\u001F", "\u0009\u000B\u000C\u001D\u001E\u001F"));
		Assert.assertFalse(StringUtilities.equals("\u0009\u000B\u000C\u001D\u001E\u001F", ""));
		Assert.assertTrue(StringUtilities.equals("foo", "foo"));
		Assert.assertFalse(StringUtilities.equals("foo", "Foo"));
		Assert.assertFalse(StringUtilities.equals("\tfoo", "foo "));
		Assert.assertFalse(StringUtilities.equals("foo\r\n", " Foo "));
	}

	@Test
	public void testEqualsIgnoreCase()
	{
		Assert.assertTrue(StringUtilities.equalsIgnoreCase(null, null));
		Assert.assertFalse(StringUtilities.equalsIgnoreCase(null, ""));
		Assert.assertFalse(StringUtilities.equalsIgnoreCase("", null));
		Assert.assertFalse(StringUtilities.equalsIgnoreCase("", " "));
		Assert.assertTrue(StringUtilities.equalsIgnoreCase("", ""));
		Assert.assertTrue(StringUtilities.equalsIgnoreCase("\t\n\r\f", "\t\n\r\f"));
		Assert.assertFalse(StringUtilities.equalsIgnoreCase("\t\n\r\f", ""));
		Assert.assertTrue(StringUtilities.equalsIgnoreCase("\u0009\u000B\u000C\u001D\u001E\u001F", "\u0009\u000B\u000C\u001D\u001E\u001F"));
		Assert.assertFalse(StringUtilities.equalsIgnoreCase("\u0009\u000B\u000C\u001D\u001E\u001F", ""));
		Assert.assertTrue(StringUtilities.equalsIgnoreCase("foo", "foo"));
		Assert.assertTrue(StringUtilities.equalsIgnoreCase("foo", "Foo"));
		Assert.assertFalse(StringUtilities.equalsIgnoreCase("\tfoo", "foo "));
		Assert.assertFalse(StringUtilities.equalsIgnoreCase("foo\r\n", " Foo "));
	}

    @Test
	public void testIndexOf()
	{
		Assert.assertEquals(-1, StringUtilities.indexOf(null, 'a'));
		Assert.assertEquals(-1, StringUtilities.indexOf("", 'a'));
		Assert.assertEquals(0, StringUtilities.indexOf("abcabcabc", 'a'));
		Assert.assertEquals(1, StringUtilities.indexOf("abcabcabc", 'b'));
		Assert.assertEquals(2, StringUtilities.indexOf("abcabcabc", 'c'));

		Assert.assertEquals(-1, StringUtilities.indexOf(null, 'a', 0));
		Assert.assertEquals(-1, StringUtilities.indexOf(null, 'a', 1));
		Assert.assertEquals(-1, StringUtilities.indexOf("", 'a', 0));
		Assert.assertEquals(-1, StringUtilities.indexOf("abcabcabc", 'd', 0));
		Assert.assertEquals(-1, StringUtilities.indexOf("abcabcabc", 'b', "abcabcabc".length() + 1));
		Assert.assertEquals(5, StringUtilities.indexOf("abcabcabc", 'c', 3));
		Assert.assertEquals(3, StringUtilities.indexOf("abcabcabc", 'a', 2));
		Assert.assertEquals(5, StringUtilities.indexOf("abcabcabc", 'c', 3));
		Assert.assertEquals(1, StringUtilities.indexOf("abcabcabc", 'b', 0));
		Assert.assertEquals(0, StringUtilities.indexOf("abcabcabc", 'a', 0));
		Assert.assertEquals(1, StringUtilities.indexOf("abcabcabc", 'b', -1));

		Assert.assertEquals(-1, StringUtilities.indexOf(null, "a"));
		Assert.assertEquals(-1, StringUtilities.indexOf(null, ""));
		Assert.assertEquals(-1, StringUtilities.indexOf("abcabcabc", null));
		Assert.assertEquals(-1, StringUtilities.indexOf("", "a"));
		Assert.assertEquals(-1, StringUtilities.indexOf("abcabcabc", "d"));
		Assert.assertEquals(-1, StringUtilities.indexOf("abcabcabc", "bcd"));
		Assert.assertEquals(0, StringUtilities.indexOf("abcabcabc", ""));
		Assert.assertEquals(2, StringUtilities.indexOf("abcabcabc", "cab"));
		Assert.assertEquals(0, StringUtilities.indexOf("abcabcabc", "abc"));
		Assert.assertEquals(2, StringUtilities.indexOf("abcabcabc", "c"));

		Assert.assertEquals(-1, StringUtilities.indexOf(null, "a", 0));
		Assert.assertEquals(-1, StringUtilities.indexOf(null, "", 1));
		Assert.assertEquals(-1, StringUtilities.indexOf("abcabcabc", null, 1));
		Assert.assertEquals(-1, StringUtilities.indexOf("", "a", 0));
		Assert.assertEquals(-1, StringUtilities.indexOf("abcabcabc", "d", 0));
		Assert.assertEquals(-1, StringUtilities.indexOf("abcabcabc", "bcd", 0));
		Assert.assertEquals(-1, StringUtilities.indexOf("abcabcabc", "b", "abcabcabc".length() + 1));
		Assert.assertEquals(-1, StringUtilities.indexOf("abcabcabc", "abc", "abcabcabc".length() + 1));
		Assert.assertEquals(2, StringUtilities.indexOf("abcabcabc", "", 2));
		Assert.assertEquals(5, StringUtilities.indexOf("abcabcabc", "cab", 3));
		Assert.assertEquals(3, StringUtilities.indexOf("abcabcabc", "abc", 2));
		Assert.assertEquals(5, StringUtilities.indexOf("abcabcabc", "c", 3));
		Assert.assertEquals(0, StringUtilities.indexOf("abcabcabc", "a", 0));
		Assert.assertEquals(2, StringUtilities.indexOf("abcabcabc", "cab", -1));
	}

	@Test
	public void testLastIndexOf()
	{
		Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, 'a'));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("", 'a'));
		Assert.assertEquals(6, StringUtilities.lastIndexOf("abcabcabc", 'a'));
		Assert.assertEquals(7, StringUtilities.lastIndexOf("abcabcabc", 'b'));
		Assert.assertEquals(8, StringUtilities.lastIndexOf("abcabcabc", 'c'));

		Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, 'a', 0));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, 'a', 1));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("", 'a', 0));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("abcabcabc", 'd', 0));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("abcabcabc", 'a', -1));
		Assert.assertEquals(2, StringUtilities.lastIndexOf("abcabcabc", 'c', 3));
		Assert.assertEquals(0, StringUtilities.lastIndexOf("abcabcabc", 'a', 2));
		Assert.assertEquals(2, StringUtilities.lastIndexOf("abcabcabc", 'c', 3));
		Assert.assertEquals(7, StringUtilities.lastIndexOf("abcabcabc", 'b', 9));
		Assert.assertEquals(3, StringUtilities.lastIndexOf("abcabcabc", 'a', 5));
		Assert.assertEquals(6, StringUtilities.lastIndexOf("abcabcabc", 'a', "abcabcabc".length() + 1));

		Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, "a"));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, ""));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("abcabcabc", null));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("", "a"));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("abcabcabc", "d"));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("abcabcabc", "bcd"));
		Assert.assertEquals(9, StringUtilities.lastIndexOf("abcabcabc", ""));
		Assert.assertEquals(5, StringUtilities.lastIndexOf("abcabcabc", "cab"));
		Assert.assertEquals(6, StringUtilities.lastIndexOf("abcabcabc", "abc"));
		Assert.assertEquals(8, StringUtilities.lastIndexOf("abcabcabc", "c"));

		Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, "a", 0));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, "", 1));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("abcabcabc", null, 1));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("", "a", 0));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("abcabcabc", "ca", -1));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("abcabcabc", "d", 0));
		Assert.assertEquals(-1, StringUtilities.lastIndexOf("abcabcabc", "bcd", 0));
		Assert.assertEquals(2, StringUtilities.lastIndexOf("abcabcabc", "", 2));
		Assert.assertEquals(2, StringUtilities.lastIndexOf("abcabcabc", "cab", 3));
		Assert.assertEquals(0, StringUtilities.lastIndexOf("abcabcabc", "abc", 2));
		Assert.assertEquals(2, StringUtilities.lastIndexOf("abcabcabc", "c", 3));
		Assert.assertEquals(0, StringUtilities.lastIndexOf("abcabcabc", "a", 0));
		Assert.assertEquals(7, StringUtilities.lastIndexOf("abcabcabc", "bc", "abcabcabc".length() + 1));
	}

	@Test
	public void testContains()
	{
		Assert.assertEquals(false, StringUtilities.contains(null, 'a'));
		Assert.assertEquals(false, StringUtilities.contains("", 'a'));
		Assert.assertEquals(true, StringUtilities.contains("abc", 'a'));
		Assert.assertEquals(false, StringUtilities.contains("abc", 'z'));
	}

    @Test
    public void testStrip() {
        Assert.assertEquals(null, StringUtilities.strip(null));
        Assert.assertEquals("", StringUtilities.strip(""));
        Assert.assertEquals("", StringUtilities.strip(" "));
        Assert.assertEquals("", StringUtilities.strip("\t\n\r\f"));
        Assert.assertEquals("", StringUtilities.strip("\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertEquals("foo", StringUtilities.strip("foo"));
        Assert.assertEquals("foo", StringUtilities.strip(" foo "));

        //  Only 4 characters are truly considered whitespace
        Assert.assertEquals(getStrippedCharactersUnder32(true, true), StringUtilities.strip(getTrimmableString()));
    }

    @Test
    public void testStripToNull() {
        Assert.assertNull(StringUtilities.stripToNull(null));
        Assert.assertNull(StringUtilities.stripToNull(""));
        Assert.assertNull(StringUtilities.stripToNull(" "));
        Assert.assertNull(StringUtilities.stripToNull("\t\n\r\f"));
        Assert.assertNull(StringUtilities.stripToNull("\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertEquals("foo", StringUtilities.stripToNull("foo"));
        Assert.assertEquals("foo", StringUtilities.stripToNull(" foo "));

        //  Only 4 characters are truly considered whitespace
        Assert.assertEquals(getStrippedCharactersUnder32(true, true), StringUtilities.stripToNull(getTrimmableString()));
    }

    @Test
    public void testStripToEmpty() {
        Assert.assertEquals("", StringUtilities.stripToEmpty(null));
        Assert.assertEquals("", StringUtilities.stripToEmpty(""));
        Assert.assertEquals("", StringUtilities.stripToEmpty(" "));
        Assert.assertEquals("", StringUtilities.stripToEmpty("\t\n\r\f"));
        Assert.assertEquals("", StringUtilities.stripToEmpty("\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertEquals("foo", StringUtilities.stripToEmpty("foo"));
        Assert.assertEquals("foo", StringUtilities.stripToEmpty(" foo "));

        //  Only 4 characters are truly considered whitespace
        Assert.assertEquals(getStrippedCharactersUnder32(true, true), StringUtilities.stripToEmpty(getTrimmableString()));
    }

    @Test
    public void testStripStart() {
        Assert.assertEquals(null, StringUtilities.stripStart(null));
        Assert.assertEquals("", StringUtilities.stripStart(""));
        Assert.assertEquals("", StringUtilities.stripStart(" "));
        Assert.assertEquals("", StringUtilities.stripStart("\t\n\r\f"));
        Assert.assertEquals("", StringUtilities.stripStart("\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertEquals("foo", StringUtilities.stripStart("foo"));
        Assert.assertEquals("foo ", StringUtilities.stripStart(" foo "));

        Assert.assertEquals(getStrippedCharactersUnder32(true, false), StringUtilities.stripStart(getTrimmableString()));
    }

    @Test
    public void testStripEnd() {
        Assert.assertEquals(null, StringUtilities.stripEnd(null));
        Assert.assertEquals("", StringUtilities.stripEnd(""));
        Assert.assertEquals("", StringUtilities.stripEnd(" "));
        Assert.assertEquals("", StringUtilities.stripEnd("\t\n\r\f"));
        Assert.assertEquals("", StringUtilities.stripEnd("\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertEquals("foo", StringUtilities.stripEnd("foo"));
        Assert.assertEquals(" foo", StringUtilities.stripEnd(" foo "));

        Assert.assertEquals(getStrippedCharactersUnder32(false, true), StringUtilities.stripEnd(getTrimmableString()));
    }

    @Test
    public void testStripWithChars() {
        Assert.assertEquals(null, StringUtilities.strip(null, "abc"));
        Assert.assertEquals(null, StringUtilities.strip(null, " abc "));
        Assert.assertEquals(null, StringUtilities.strip(null, null));
        Assert.assertEquals(null, StringUtilities.strip(null, ""));
        Assert.assertEquals("", StringUtilities.strip("", "abc"));

        Assert.assertEquals("", StringUtilities.strip("abc", "abc"));
        Assert.assertEquals("a", StringUtilities.strip("abc", "bc"));
        Assert.assertEquals("c", StringUtilities.strip("abc", "ab"));
        Assert.assertEquals("abc", StringUtilities.strip("abc", "b"));
        Assert.assertEquals(" abc ", StringUtilities.strip(" abc ", "abc"));
        Assert.assertEquals("\nabc\n", StringUtilities.strip(" \nabc\n ", " "));

        Assert.assertEquals("", StringUtilities.strip(""));
        Assert.assertEquals(" ", StringUtilities.strip(" ", "abc"));
        Assert.assertEquals("\ffoo\t\u000B\f\u001D\u001E\u001F", StringUtilities.strip("\t\n\r\ffoo\t\u000B\f\u001D\u001E\u001F", "\t\n\r"));
        Assert.assertEquals("foo", StringUtilities.strip("foo", "abc"));
        Assert.assertEquals(" foo ", StringUtilities.strip(" foo ", "abc"));

        //  Only 4 characters are truly considered whitespace
        Assert.assertEquals(getStrippedCharactersUnder32(false, false), StringUtilities.strip(getTrimmableString(), "abc"));
    }

    @Test
    public void testStripStartWithChars() {
        Assert.assertEquals(null, StringUtilities.stripStart(null, "abc"));
        Assert.assertEquals(null, StringUtilities.stripStart(null, " abc "));
        Assert.assertEquals(null, StringUtilities.stripStart(null, null));
        Assert.assertEquals(null, StringUtilities.stripStart(null, ""));
        Assert.assertEquals("", StringUtilities.stripStart("", "abc"));

        Assert.assertEquals("", StringUtilities.stripStart("abc", "abc"));
        Assert.assertEquals("abc", StringUtilities.stripStart("abc", "bc"));
        Assert.assertEquals("c", StringUtilities.stripStart("abc", "ab"));
        Assert.assertEquals("abc", StringUtilities.stripStart("abc", "b"));
        Assert.assertEquals(" abc ", StringUtilities.stripStart(" abc ", "abc"));

        Assert.assertEquals("", StringUtilities.stripStart(""));
        Assert.assertEquals(" ", StringUtilities.stripStart(" ", "abc"));
        Assert.assertEquals("\ffoo\t\u000B\f\u001D\u001E\u001F", StringUtilities.stripStart("\t\n\r\ffoo\t\u000B\f\u001D\u001E\u001F", "\t\n\r"));
        Assert.assertEquals("foo", StringUtilities.stripStart("foo", "abc"));
        Assert.assertEquals(" foo ", StringUtilities.stripStart(" foo ", "abc"));

        //  Only 4 characters are truly considered whitespace
        Assert.assertEquals(getStrippedCharactersUnder32(false, false), StringUtilities.strip(getTrimmableString(), "abc"));
    }

    @Test
    public void testStripEndWithChars() {
        Assert.assertEquals(null, StringUtilities.stripEnd(null, "abc"));
        Assert.assertEquals(null, StringUtilities.stripEnd(null, " abc "));
        Assert.assertEquals(null, StringUtilities.stripEnd(null, null));
        Assert.assertEquals(null, StringUtilities.stripEnd(null, ""));
        Assert.assertEquals("", StringUtilities.stripEnd("", "abc"));

        Assert.assertEquals("", StringUtilities.stripEnd("abc", "abc"));
        Assert.assertEquals("a", StringUtilities.stripEnd("abc", "bc"));
        Assert.assertEquals("abc", StringUtilities.stripEnd("abc", "ab"));
        Assert.assertEquals("abba", StringUtilities.stripEnd("abba", "b"));
        Assert.assertEquals("abc", StringUtilities.stripEnd("abc", "b"));
        Assert.assertEquals(" abc ", StringUtilities.stripEnd(" abc ", "abc"));

        Assert.assertEquals("", StringUtilities.stripEnd(""));
        Assert.assertEquals(" ", StringUtilities.stripEnd(" ", "abc"));
        Assert.assertEquals("\t\n\r\ffoo\t\u000B\f\u001D\u001E", StringUtilities.stripEnd("\t\n\r\ffoo\t\u000B\f\u001D\u001E\u001F", "\u001F"));
        Assert.assertEquals("foo", StringUtilities.stripEnd("foo", "abc"));
        Assert.assertEquals(" foo ", StringUtilities.stripEnd(" foo ", "abc"));

        //  Only 4 characters are truly considered whitespace
        Assert.assertEquals(getTrimmableString(), StringUtilities.stripEnd(getTrimmableString(), "abc"));
    }

    @Test
    public void testLength() {
        Assert.assertEquals(0, StringUtilities.length(""));
        Assert.assertEquals(0, StringUtilities.length(null));
        Assert.assertEquals(3, StringUtilities.length("abc"));
    }


}
