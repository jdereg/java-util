
package com.cedarsoftware.lang;

import com.cedarsoftware.test.Asserter;
import org.junit.Assert;
import org.junit.Test;


public class TestCharSequenceUtilities {

    
	@Test
	public void testConstructor()
	{
        Asserter.assertClassOnlyHasAPrivateDefaultConstructor(CharSequenceUtilities.class);
	}
	
    //-----------------------------------------------------------------------
	@Test
	public void testSubSequence() {
        //
        // null input
        //
        Assert.assertEquals(null, CharSequenceUtilities.subSequence(null, -1));
        Assert.assertEquals(null, CharSequenceUtilities.subSequence(null, 0));
        Assert.assertEquals(null, CharSequenceUtilities.subSequence(null, 1));
        //
        // non-null input
        //
        Assert.assertEquals(StringUtilities.EMPTY, CharSequenceUtilities.subSequence(StringUtilities.EMPTY, 0));
        Assert.assertEquals("012", CharSequenceUtilities.subSequence("012", 0));
        Assert.assertEquals("12", CharSequenceUtilities.subSequence("012", 1));
        Assert.assertEquals("2", CharSequenceUtilities.subSequence("012", 2));
        Assert.assertEquals(StringUtilities.EMPTY, CharSequenceUtilities.subSequence("012", 3));
        try {
            Assert.assertEquals(null, CharSequenceUtilities.subSequence(StringUtilities.EMPTY, 1));
            Assert.fail("Expected " + IndexOutOfBoundsException.class.getName());
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
        try {
            Assert.assertEquals(null, CharSequenceUtilities.subSequence(StringUtilities.EMPTY, -1));
            Assert.fail("Expected " + IndexOutOfBoundsException.class.getName());
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }
    }


	@Test
	public void testIsEmpty()
	{
		Assert.assertEquals(true, CharSequenceUtilities.isEmpty(null));
		Assert.assertEquals(true, CharSequenceUtilities.isEmpty(""));
		Assert.assertEquals(false, CharSequenceUtilities.isEmpty(" "));
		Assert.assertEquals(false, CharSequenceUtilities.isEmpty("\t\n\r\f"));
		Assert.assertEquals(false, CharSequenceUtilities.isEmpty("\u0009\u000B\u000C\u001D\u001E\u001F"));
		Assert.assertEquals(false, CharSequenceUtilities.isEmpty("foo"));
		Assert.assertEquals(false, CharSequenceUtilities.isEmpty(" foo "));
	}

    @Test
    public void testIsNotEmpty()
    {
        Assert.assertEquals(false, CharSequenceUtilities.isNotEmpty(null));
        Assert.assertEquals(false, CharSequenceUtilities.isNotEmpty(""));
        Assert.assertEquals(true, CharSequenceUtilities.isNotEmpty(" "));
        Assert.assertEquals(true, CharSequenceUtilities.isNotEmpty("\t\n\r\f"));
        Assert.assertEquals(true, CharSequenceUtilities.isNotEmpty("\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertEquals(true, CharSequenceUtilities.isNotEmpty("foo"));
        Assert.assertEquals(true, CharSequenceUtilities.isNotEmpty(" foo "));
    }

    @Test
    public void testIsBlank()
    {
        Assert.assertEquals(true, CharSequenceUtilities.isBlank(null));
        Assert.assertEquals(true, CharSequenceUtilities.isBlank(""));
        Assert.assertEquals(true, CharSequenceUtilities.isBlank(" "));
        Assert.assertEquals(true, CharSequenceUtilities.isBlank("\t\n\r\f"));
        Assert.assertEquals(true, CharSequenceUtilities.isBlank("\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertEquals(false, CharSequenceUtilities.isBlank("foo"));
        Assert.assertEquals(false, CharSequenceUtilities.isBlank(" foo "));
    }


	@Test
	public void testIsNotBlankAndHasContent()
	{
		Assert.assertEquals(false, CharSequenceUtilities.isNotBlank(null));
		Assert.assertEquals(false, CharSequenceUtilities.isNotBlank(""));
		Assert.assertEquals(false, CharSequenceUtilities.isNotBlank(" "));
		Assert.assertEquals(false, CharSequenceUtilities.isNotBlank("\t\n\r\f"));
		Assert.assertEquals(false, CharSequenceUtilities.isNotBlank("\u0009\u000B\u000C\u001D\u001E\u001F"));
		Assert.assertEquals(true, CharSequenceUtilities.isNotBlank("foo"));
		Assert.assertEquals(true, CharSequenceUtilities.isNotBlank(" foo "));

		Assert.assertEquals(false, CharSequenceUtilities.hasContent(null));
		Assert.assertEquals(false, CharSequenceUtilities.hasContent(""));
		Assert.assertEquals(false, CharSequenceUtilities.hasContent(" "));
		Assert.assertEquals(false, CharSequenceUtilities.hasContent("\t\n\r\f"));
		Assert.assertEquals(false, CharSequenceUtilities.hasContent("\u0009\u000B\u000C\u001D\u001E\u001F"));
		Assert.assertEquals(true, CharSequenceUtilities.hasContent("foo"));
		Assert.assertEquals(true, CharSequenceUtilities.hasContent(" foo "));
	}

    @Test
	public void testCount()
	{
		Assert.assertEquals(0, CharSequenceUtilities.count(null, 'a'));
		Assert.assertEquals(0, CharSequenceUtilities.count(StringUtilities.EMPTY, 'a'));
		Assert.assertEquals(0, CharSequenceUtilities.count(new StringBuffer(), 'a'));
		Assert.assertEquals(0, CharSequenceUtilities.count(new StringBuilder(), 'a'));
		Assert.assertEquals(1, CharSequenceUtilities.count("abc", 'a'));
		Assert.assertEquals(2, CharSequenceUtilities.count(" abc ", ' '));
		Assert.assertEquals(2, CharSequenceUtilities.count("abca", 'a'));
	}

    @Test
    public void testLength()
    {
        Assert.assertEquals(0, CharSequenceUtilities.length(null));
        Assert.assertEquals(0, CharSequenceUtilities.length(""));
        Assert.assertEquals(1, CharSequenceUtilities.length(" "));
        Assert.assertEquals(4, CharSequenceUtilities.length("\t\n\r\f"));
        Assert.assertEquals(6, CharSequenceUtilities.length("\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertEquals(3, CharSequenceUtilities.length("foo"));
        Assert.assertEquals(5, CharSequenceUtilities.length("\tfoo "));
    }


    @Test
    public void testLengthAfterTrim()
    {
        Assert.assertEquals(0, CharSequenceUtilities.lengthAfterTrim(null));
        Assert.assertEquals(0, CharSequenceUtilities.lengthAfterTrim(""));
        Assert.assertEquals(0, CharSequenceUtilities.lengthAfterTrim(" "));
        Assert.assertEquals(0, CharSequenceUtilities.lengthAfterTrim("\t\n\r\f"));
        Assert.assertEquals(0, CharSequenceUtilities.lengthAfterTrim("\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertEquals(3, CharSequenceUtilities.lengthAfterTrim("foo"));
        Assert.assertEquals(3, CharSequenceUtilities.lengthAfterTrim("\tfoo "));
    }

    @Test
    public void testLengthAfterStrip()
    {
        Assert.assertEquals(0, CharSequenceUtilities.lengthAfterStrip(null));
        Assert.assertEquals(0, CharSequenceUtilities.lengthAfterStrip(""));
        Assert.assertEquals(0, CharSequenceUtilities.lengthAfterStrip(" "));
        Assert.assertEquals(0, CharSequenceUtilities.lengthAfterStrip("\t\n\r\f"));
        Assert.assertEquals(0, CharSequenceUtilities.lengthAfterStrip("\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertEquals(3, CharSequenceUtilities.lengthAfterStrip("foo"));
        Assert.assertEquals(3, CharSequenceUtilities.lengthAfterStrip("\tfoo "));
    }

    @Test
    public void testEqualsAfterTrim()
    {
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim(null, null));
        Assert.assertFalse(CharSequenceUtilities.equalsAfterTrim(null, ""));
        Assert.assertFalse(CharSequenceUtilities.equalsAfterTrim("", null));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("", " "));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("", ""));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("\t\n\r\f", "\t\n\r\f"));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("\t\n\r\f", ""));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("\u0009\u000B\u000C\u001D\u001E\u001F", "\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("\u0009\u000B\u000C\u001D\u001E\u001F", ""));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("foo", "foo"));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim(" foo", "foo"));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("foo ", "foo"));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("foo", " foo"));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("foo", "foo "));
        Assert.assertFalse(CharSequenceUtilities.equalsAfterTrim("foo", "FOO"));
        Assert.assertFalse(CharSequenceUtilities.equalsAfterTrim(" foo", "FOO"));
        Assert.assertFalse(CharSequenceUtilities.equalsAfterTrim("foo ", "FOO"));
        Assert.assertFalse(CharSequenceUtilities.equalsAfterTrim("foo", " FOO"));
        Assert.assertFalse(CharSequenceUtilities.equalsAfterTrim("foo", "FOO "));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("ab cd", "ab cd"));
        Assert.assertTrue(CharSequenceUtilities.equalsAfterTrim("\tfoo", "foo "));
        Assert.assertFalse(CharSequenceUtilities.equalsAfterTrim("foo\r\n", " Foo "));
        Assert.assertFalse(CharSequenceUtilities.equalsAfterTrim(" abc", "abc d "));
    }

    @Test
    public void testEqualsIgnoreCaseAfterTrim()
    {
        Assert.assertTrue(CharSequenceUtilities.equalsIgnoreCaseAfterTrim(null, null));
        Assert.assertFalse(CharSequenceUtilities.equalsIgnoreCaseAfterTrim(null, ""));
        Assert.assertFalse(CharSequenceUtilities.equalsIgnoreCaseAfterTrim("", null));
        Assert.assertTrue(CharSequenceUtilities.equalsIgnoreCaseAfterTrim("", " "));
        Assert.assertTrue(CharSequenceUtilities.equalsIgnoreCaseAfterTrim("", ""));
        Assert.assertTrue(CharSequenceUtilities.equalsIgnoreCaseAfterTrim("\t\n\r\f", "\t\n\r\f"));
        Assert.assertTrue(CharSequenceUtilities.equalsIgnoreCaseAfterTrim("\t\n\r\f", ""));
        Assert.assertTrue(CharSequenceUtilities.equalsIgnoreCaseAfterTrim("\u0009\u000B\u000C\u001D\u001E\u001F", "\u0009\u000B\u000C\u001D\u001E\u001F"));
        Assert.assertTrue(CharSequenceUtilities.equalsIgnoreCaseAfterTrim("\u0009\u000B\u000C\u001D\u001E\u001F", ""));
        Assert.assertTrue(CharSequenceUtilities.equalsIgnoreCaseAfterTrim("foo", "foo"));
        Assert.assertTrue(CharSequenceUtilities.equalsIgnoreCaseAfterTrim("foo", "Foo"));
        Assert.assertTrue(CharSequenceUtilities.equalsIgnoreCaseAfterTrim("\tfoo", "foo "));
        Assert.assertTrue(CharSequenceUtilities.equalsIgnoreCaseAfterTrim("foo\r\n", " Foo "));
        Assert.assertFalse(CharSequenceUtilities.equalsIgnoreCaseAfterTrim(" abc", "abc D "));
    }

    @Test(expected=NullPointerException.class)
    public void testRegionMatchesFirstParamNull()
    {
        Assert.assertTrue(CharSequenceUtilities.regionMatches(null, 0, "foo", 0, 2));
    }

    @Test(expected=NullPointerException.class)
    public void testRegionMatchesThirdParamNull()
    {
        Assert.assertTrue(CharSequenceUtilities.regionMatches("foo", 0, null, 0, 2));
    }

    @Test
    public void testRegionMatches() {
        Assert.assertTrue(CharSequenceUtilities.regionMatches(" foo", 1, "foo", 0, 3));
        Assert.assertTrue(CharSequenceUtilities.regionMatches("foo", 0, " foo", 1, 3));
        Assert.assertTrue(CharSequenceUtilities.regionMatches("afooabc", 1, "fooabcd", 0, 3));
        Assert.assertTrue(CharSequenceUtilities.regionMatches("fooabcd", 0, "afoobcd", 1, 3));
        Assert.assertFalse(CharSequenceUtilities.regionMatches("foo", 0, "foo", 1, 3));
        Assert.assertFalse(CharSequenceUtilities.regionMatches("foo", 1, "foo", 0, 3));
        Assert.assertFalse(CharSequenceUtilities.regionMatches(" foo", 0, "foo", 0, 4));
        Assert.assertFalse(CharSequenceUtilities.regionMatches("foo", 1, " foo", 0, 4));
    }

    @Test
    public void testRegionMatchesIgnoreCase() {
        Assert.assertFalse(CharSequenceUtilities.regionMatchesIgnoreCase("abc", 0, "def", 0, 3));
        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase(" Foo", 1, "foo", 0, 3));
        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase("Foo", 0, " foo", 1, 3));
        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase("aFooabc", 1, "fooabcd", 0, 3));
        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase("Fooabcd", 0, "afoobcd", 1, 3));
        Assert.assertFalse(CharSequenceUtilities.regionMatchesIgnoreCase("Foo", 0, "foo", 1, 3));
        Assert.assertFalse(CharSequenceUtilities.regionMatchesIgnoreCase("Foo", 1, "foo", 0, 3));
        Assert.assertFalse(CharSequenceUtilities.regionMatchesIgnoreCase(" Foo", 0, "foo", 0, 4));
        Assert.assertFalse(CharSequenceUtilities.regionMatchesIgnoreCase("Foo", 1, " foo", 0, 4));

        StringBuffer b1 = new StringBuffer("\u10a0\u10a1\u10a2\u10a3\u10a4\u10a5\u10a6\u10a7\u10a8\u10a9\u10aa\u10ab\u10ac\u10ad\u10ae\u10af\u10b0\u10b1\u10b2\u10b3\u10b4\u10b5\u10b6\u10b7\u10b8\u10b9\u10ba\u10bb\u10bc\u10bd\u10be\u10bf\u10c0\u10c1\u10c2\u10c3\u10c4\u10c5");
        StringBuffer b2 = new StringBuffer("\u2d00\u2d01\u2d02\u2d03\u2d04\u2d05\u2d06\u2d07\u2d08\u2d09\u2d0a\u2d0b\u2d0c\u2d0d\u2d0e\u2d0f\u2d10\u2d11\u2d12\u2d13\u2d14\u2d15\u2d16\u2d17\u2d18\u2d19\u2d1a\u2d1b\u2d1c\u2d1d\u2d1e\u2d1f\u2d20\u2d21\u2d22\u2d23\u2d24\u2d25");
        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase(b1, 0, b2, 0, 38));
        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase(b2, 0, b1, 0, 38));


        // Test Rare Characters that should be equal with ignore case
        StringBuffer b5 = new StringBuffer("\u0131\u03f4\u2126");
        StringBuffer b6 = new StringBuffer("\u0130\u03d1\u03c9");

        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase(b5, 0, b6, 0, 3));
        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase(b6, 0, b5, 0, 3));

        StringBuffer b3 = new StringBuffer("\u10a0\u10a1\u10a2\u10a3\u10a4\u10a5\u10a6\u10a7\u10a8\u10a9\u10aa\u10ab\u10ac\u10ad\u10ae\u10af\u10b0\u10b1\u10b2\u10b3\u10b4\u10b5\u10b6\u10b7\u10b8\u10b9\u10ba\u10bb\u10bc\u10bd\u10be\u10bf\u10c0\u10c1\u10c2\u10c3\u10c4\u10c5\u2d00\u2d01\u2d02\u2d03\u2d04\u2d05\u2d06\u2d07\u2d08\u2d09\u2d0a\u2d0b\u2d0c\u2d0d\u2d0e\u2d0f\u2d10\u2d11\u2d12\u2d13\u2d14\u2d15\u2d16\u2d17\u2d18\u2d19\u2d1a\u2d1b\u2d1c\u2d1d\u2d1e\u2d1f\u2d20\u2d21\u2d22\u2d23\u2d24\u2d25");
        StringBuffer b4 = new StringBuffer("\u2d00\u2d01\u2d02\u2d03\u2d04\u2d05\u2d06\u2d07\u2d08\u2d09\u2d0a\u2d0b\u2d0c\u2d0d\u2d0e\u2d0f\u2d10\u2d11\u2d12\u2d13\u2d14\u2d15\u2d16\u2d17\u2d18\u2d19\u2d1a\u2d1b\u2d1c\u2d1d\u2d1e\u2d1f\u2d20\u2d21\u2d22\u2d23\u2d24\u2d25\u10a0\u10a1\u10a2\u10a3\u10a4\u10a5\u10a6\u10a7\u10a8\u10a9\u10aa\u10ab\u10ac\u10ad\u10ae\u10af\u10b0\u10b1\u10b2\u10b3\u10b4\u10b5\u10b6\u10b7\u10b8\u10b9\u10ba\u10bb\u10bc\u10bd\u10be\u10bf\u10c0\u10c1\u10c2\u10c3\u10c4\u10c5");

        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase(b3, 0, b4, 0, 38));
        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase(b4, 0, b3, 0, 38));
    }

    @Test(expected=NullPointerException.class)
    public void testRegionMatchesIgnoreCaseFirstParamNull()
    {
        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase(null, 0, "foo", 0, 2));
    }

    @Test(expected=NullPointerException.class)
    public void testRegionMatchesIgnoreCaseThirdParamNull()
    {
        Assert.assertTrue(CharSequenceUtilities.regionMatchesIgnoreCase("foo", 0, null, 0, 2));
    }

}


