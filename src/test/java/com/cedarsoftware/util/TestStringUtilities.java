package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Ken Partlow
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class TestStringUtilities
{
    @Test
    public void testConstructorIsPrivate() throws Exception {
        Class<StringUtilities> c = StringUtilities.class;
        assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<StringUtilities> con = c.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    @Test
    public void testIsEmpty()
    {
        assertTrue(StringUtilities.isEmpty(null));
        assertTrue(StringUtilities.isEmpty(""));
        assertFalse(StringUtilities.isEmpty("foo"));
    }

    @Test
    public void testHasContent() {
        assertFalse(StringUtilities.hasContent(null));
        assertFalse(StringUtilities.hasContent(""));
        assertTrue(StringUtilities.hasContent("foo"));
    }

    @Test
    public void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(0, StringUtilities.trimLength(""));
        assertEquals(3, StringUtilities.trimLength("  abc "));

        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("abc", " Abc "));
        assertTrue(StringUtilities.equalsWithTrim("abc", " abc "));
        assertEquals(2, StringUtilities.count("abcabc", 'a'));
    }

    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("abc", " abc "));
        assertTrue(StringUtilities.equalsWithTrim(" abc ", "abc"));
        assertFalse(StringUtilities.equalsWithTrim("abc", " AbC "));
        assertFalse(StringUtilities.equalsWithTrim(" AbC ", "abc"));
        assertFalse(StringUtilities.equalsWithTrim(null, ""));
        assertFalse(StringUtilities.equalsWithTrim("", null));
        assertTrue(StringUtilities.equalsWithTrim("", "\t\n\r"));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("abc", " abc "));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(" abc ", "abc"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("abc", " AbC "));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(" AbC ", "abc"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(null, ""));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("", null));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("", "\t\n\r"));
    }

    @Test
    public void testCount() {
        assertEquals(2, StringUtilities.count("abcabc", 'a'));
        assertEquals(0, StringUtilities.count("foo", 'a'));
        assertEquals(0, StringUtilities.count(null, 'a'));
        assertEquals(0, StringUtilities.count("", 'a'));
    }

    @Test
    public void testString()
    {
        assertTrue(StringUtilities.isEmpty(null));
        assertFalse(StringUtilities.hasContent(null));
        assertEquals(0, StringUtilities.trimLength(null));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("abc", " Abc "));
        assertTrue(StringUtilities.equalsWithTrim("abc", " abc "));
        assertEquals("1A", StringUtilities.encode(new byte[]{0x1A}));
        assertArrayEquals(new byte[]{0x1A}, StringUtilities.decode("1A"));
        assertEquals(2, StringUtilities.count("abcabc", 'a'));
    }

    @Test
    public void testEncode() {
        assertEquals("1A", StringUtilities.encode(new byte[]{0x1A}));
        assertEquals("", StringUtilities.encode(new byte[]{}));
    }

    public void testEncodeWithNull()
    {
        try
        {
            StringUtilities.encode(null);
            fail("should not make it here");
        }
        catch (NullPointerException e)
        {
        }
    }

    @Test
    public void testDecode() {
        assertArrayEquals(new byte[]{0x1A}, StringUtilities.decode("1A"));
        assertArrayEquals(new byte[]{}, StringUtilities.decode(""));
        assertNull(StringUtilities.decode("1AB"));
    }

    public void testDecodeWithNull()
    {
        try
        {
            StringUtilities.decode(null);
            fail("should not make it here");
        }
        catch (NullPointerException e)
        {
        }
    }

    @Test
    public void testEquals()
    {
        assertTrue(StringUtilities.equals(null, null));
        assertFalse(StringUtilities.equals(null, ""));
        assertFalse(StringUtilities.equals("", null));
        assertFalse(StringUtilities.equals("foo", "bar"));
        assertFalse(StringUtilities.equals("Foo", "foo"));
        assertTrue(StringUtilities.equals("foo", "foo"));
    }

    @Test
    public void testEqualsIgnoreCase()
    {
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        assertFalse(StringUtilities.equalsIgnoreCase(null, ""));
        assertFalse(StringUtilities.equalsIgnoreCase("", null));
        assertFalse(StringUtilities.equalsIgnoreCase("foo", "bar"));
        assertTrue(StringUtilities.equalsIgnoreCase("Foo", "foo"));
        assertTrue(StringUtilities.equalsIgnoreCase("foo", "foo"));
    }


    @Test
    public void testLastIndexOf()
    {
        assertEquals(-1, StringUtilities.lastIndexOf(null, 'a'));
        assertEquals(-1, StringUtilities.lastIndexOf("foo", 'a'));
        assertEquals(1, StringUtilities.lastIndexOf("bar", 'a'));
    }

    @Test
    public void testLength()
    {
        assertEquals(0, StringUtilities.length(""));
        assertEquals(0, StringUtilities.length(null));
        assertEquals(3, StringUtilities.length("abc"));
    }

    @Test
    public void testLevenshtein()
    {
        assertEquals(3, StringUtilities.levenshteinDistance("example", "samples"));
        assertEquals(6, StringUtilities.levenshteinDistance("sturgeon", "urgently"));
        assertEquals(6, StringUtilities.levenshteinDistance("levenshtein", "frankenstein"));
        assertEquals(5, StringUtilities.levenshteinDistance("distance", "difference"));
        assertEquals(7, StringUtilities.levenshteinDistance("java was neat", "scala is great"));
        assertEquals(0, StringUtilities.levenshteinDistance(null, ""));
        assertEquals(0, StringUtilities.levenshteinDistance("", null));
        assertEquals(0, StringUtilities.levenshteinDistance(null, null));
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(1, StringUtilities.levenshteinDistance(null, "1"));
        assertEquals(1, StringUtilities.levenshteinDistance("1", null));
        assertEquals(1, StringUtilities.levenshteinDistance("", "1"));
        assertEquals(1, StringUtilities.levenshteinDistance("1", ""));
        assertEquals(3, StringUtilities.levenshteinDistance("schill", "thrill"));
        assertEquals(2, StringUtilities.levenshteinDistance("abcdef", "bcdefa"));
    }

    @Test
    public void testDamerauLevenshtein() throws Exception
    {
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("example", "samples"));
        assertEquals(6, StringUtilities.damerauLevenshteinDistance("sturgeon", "urgently"));
        assertEquals(6, StringUtilities.damerauLevenshteinDistance("levenshtein", "frankenstein"));
        assertEquals(5, StringUtilities.damerauLevenshteinDistance("distance", "difference"));
        assertEquals(9, StringUtilities.damerauLevenshteinDistance("java was neat", "groovy is great"));
        assertEquals(0, StringUtilities.damerauLevenshteinDistance(null, ""));
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", null));
        assertEquals(0, StringUtilities.damerauLevenshteinDistance(null, null));
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance(null, "1"));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("1", null));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("", "1"));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("1", ""));
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("schill", "thrill"));
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("abcdef", "bcdefa"));

        int d1 = StringUtilities.levenshteinDistance("neat", "naet");
        int d2 = StringUtilities.damerauLevenshteinDistance("neat", "naet");
        assertEquals(d1, 2);
        assertEquals(d2, 1);
    }

    @Test
    public void testRandomString()
    {
        Random random = new Random(42);
        Set<String> strings = new TreeSet<String>();
        for (int i=0; i < 100000; i++)
        {
            String s = StringUtilities.getRandomString(random, 3, 9);
            strings.add(s);
        }

        for (String s : strings)
        {
            assertTrue(s.length() >= 3 && s.length() <= 9);
        }
    }

    public void testGetBytesWithInvalidEncoding() {
        try
        {
            StringUtilities.getBytes("foo", "foo");
            fail("should not make it here");
        }
        catch (IllegalArgumentException e)
        {
        }
    }

    @Test
    public void testGetBytes()
    {
        assertArrayEquals(new byte[]{102, 111, 111}, StringUtilities.getBytes("foo", "UTF-8"));
    }

    @Test
    public void testGetUTF8Bytes()
    {
        assertArrayEquals(new byte[]{102, 111, 111}, StringUtilities.getUTF8Bytes("foo"));
    }

    @Test
    public void testGetBytesWithNull()
    {
        assert StringUtilities.getBytes(null, "UTF-8") == null;
    }

    @Test
    public void testGetBytesWithEmptyString()
    {
        assert DeepEquals.deepEquals(new byte[]{}, StringUtilities.getBytes("", "UTF-8"));
    }

    @Test
    public void testWildcard()
    {
        String name = "George Washington";
        assertTrue(name.matches(StringUtilities.wildcardToRegexString("*")));
        assertTrue(name.matches(StringUtilities.wildcardToRegexString("G*")));
        assertTrue(name.matches(StringUtilities.wildcardToRegexString("*on")));
        assertFalse(name.matches(StringUtilities.wildcardToRegexString("g*")));

        name = "com.acme.util.string";
        assertTrue(name.matches(StringUtilities.wildcardToRegexString("com.*")));
        assertTrue(name.matches(StringUtilities.wildcardToRegexString("com.*.util.string")));

        name = "com.acme.util.string";
        assertTrue(name.matches(StringUtilities.wildcardToRegexString("com.????.util.string")));
        assertFalse(name.matches(StringUtilities.wildcardToRegexString("com.??.util.string")));
    }

    @Test
    public void testCreateString()
    {
        assertEquals("foo", StringUtilities.createString(new byte[]{102, 111, 111}, "UTF-8"));
    }

    @Test
    public void testCreateUTF8String()
    {
        assertEquals("foo", StringUtilities.createUTF8String(new byte[]{102, 111, 111}));
    }

    @Test
    public void testCreateStringWithNull()
    {
        assertNull(null, StringUtilities.createString(null, "UTF-8"));
    }

    @Test
    public void testCreateStringWithEmptyArray()
    {
        assertEquals("", StringUtilities.createString(new byte[]{}, "UTF-8"));
    }

    @Test
    public void testCreateUTF8StringWithEmptyArray()
    {
        assertEquals("", StringUtilities.createUTF8String(new byte[]{}));
    }

    @Test
    public void testCreateStringWithInvalidEncoding()
    {
        try
        {
            StringUtilities.createString(new byte[] {102, 111, 111}, "baz");
            fail("Should not make it here");
        }
        catch(IllegalArgumentException e)
        { }
    }

    @Test
    public void testCreateUtf8String()
    {
        assertEquals("foo", StringUtilities.createUtf8String(new byte[] {102, 111, 111}));
    }

    @Test
    public void testCreateUtf8StringWithNull()
    {
        assertNull(null, StringUtilities.createUtf8String(null));
    }

    @Test
    public void testCreateUtf8StringWithEmptyArray()
    {
        assertEquals("", StringUtilities.createUtf8String(new byte[]{}));
    }

    @Test
    public void testHashCodeIgnoreCase()
    {
        String s = "Hello";
        String t = "HELLO";
        assert StringUtilities.hashCodeIgnoreCase(s) == StringUtilities.hashCodeIgnoreCase(t);

        s = "Hell0";
        assert StringUtilities.hashCodeIgnoreCase(s) != StringUtilities.hashCodeIgnoreCase(t);

        assert StringUtilities.hashCodeIgnoreCase(null) == 0;
        assert StringUtilities.hashCodeIgnoreCase("") == 0;
    }

    @Test
    public void testCount2()
    {
        assert 0 == StringUtilities.count("alphabet", null);
        assert 0 == StringUtilities.count(null, "al");
        assert 0 == StringUtilities.count("alphabet", "");
        assert 0 == StringUtilities.count("", "al");
        assert 1 == StringUtilities.count("alphabet", "al");
        assert 2 == StringUtilities.count("halal", "al");
    }
}
