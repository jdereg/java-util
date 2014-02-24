package com.cedarsoftware.util;

import org.junit.Test;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestStringUtilities
{
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
    public void testEquals()
    {
        assertTrue(StringUtilities.equals(null, null));
    }

    @Test
    public void testEqualsIgnoreCase()
    {
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
    }


    @Test
    public void testLastIndexOf()
    {
        assertEquals(-1, StringUtilities.lastIndexOf(null, 'a'));
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
}
