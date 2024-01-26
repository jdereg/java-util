package com.cedarsoftware.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.cedarsoftware.util.convert.CommonValues;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.internal.util.StringUtil;

import javax.swing.text.Segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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
    void testConstructorIsPrivate() throws Exception {
        Class<StringUtilities> c = StringUtilities.class;
        assertEquals(Modifier.FINAL, c.getModifiers() & Modifier.FINAL);

        Constructor<StringUtilities> con = c.getDeclaredConstructor();
        assertEquals(Modifier.PRIVATE, con.getModifiers() & Modifier.PRIVATE);
        con.setAccessible(true);

        assertNotNull(con.newInstance());
    }

    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void testIsEmpty_whenStringHasOnlyWhitespace_returnsTrue(String s)
    {
        assertTrue(StringUtilities.isEmpty(s));
    }

    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void testIsEmpty_whenStringHasContent_returnsFalse(String s)
    {
        assertFalse(StringUtilities.isEmpty(s));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testIsEmpty_whenNullOrEmpty_returnsTrue(String s)
    {
        assertTrue(StringUtilities.isEmpty(s));
    }

    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void testIsNotEmpty_whenStringHasOnlyWhitespace_returnsFalse(String s)
    {
        assertFalse(StringUtilities.isNotEmpty(s));
    }

    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void testIsNotEmpty_whenStringHasContent_returnsTrue(String s)
    {
        assertTrue(StringUtilities.isNotEmpty(s));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testIsNotEmpty_whenNullOrEmpty_returnsFalse(String s)
    {
        assertFalse(StringUtilities.isNotEmpty(s));
    }

    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void testIsWhiteSpace_whenStringHasWhitespace_returnsTrue(String s)
    {
        assertTrue(StringUtilities.isWhitespace(s));
    }

    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void testIsWhiteSpace_whenStringHasContent_returnsFalse(String s)
    {
        assertFalse(StringUtilities.isWhitespace(s));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testIsWhiteSpace_whenNullOrEmpty_returnsTrue(String s)
    {
        assertTrue(StringUtilities.isWhitespace(s));
    }


    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void testHasContent_whenStringHasWhitespace_returnsFalse(String s)
    {
        assertFalse(StringUtilities.hasContent(s));
    }

    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void testHasContent_whenStringHasContent_returnsTrue(String s)
    {
        assertTrue(StringUtilities.hasContent(s));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testHasContent_whenNullOrEmpty_returnsFalse(String s)
    {
        assertFalse(StringUtilities.hasContent(s));
    }

    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void testIsNotWhitespace_whenStringHasWhitespace_returnsFalse(String s)
    {
        assertFalse(StringUtilities.isNotWhitespace(s));
    }

    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void testIsNotWhitespace_whenStringHasContent_returnsTrue(String s)
    {
        assertTrue(StringUtilities.isNotWhitespace(s));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testIsNotWhitespace_whenNullOrEmpty_returnsFalse(String s)
    {
        assertFalse(StringUtilities.isNotWhitespace(s));
    }
    @Test
    public void testIsEmpty()
    {
        assertTrue(StringUtilities.isEmpty(null));
        assertTrue(StringUtilities.isEmpty(""));
        assertFalse(StringUtilities.isEmpty("foo"));
    }

    @Test
    void testHasContent() {
        assertFalse(StringUtilities.hasContent(null));
        assertFalse(StringUtilities.hasContent(""));
        assertTrue(StringUtilities.hasContent("foo"));
    }

    @Test
    void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(0, StringUtilities.trimLength(""));
        assertEquals(3, StringUtilities.trimLength("  abc "));

        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("abc", " Abc "));
        assertTrue(StringUtilities.equalsWithTrim("abc", " abc "));
        assertEquals(2, StringUtilities.count("abcabc", 'a'));
    }

    @Test
    void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("abc", " abc "));
        assertTrue(StringUtilities.equalsWithTrim(" abc ", "abc"));
        assertFalse(StringUtilities.equalsWithTrim("abc", " AbC "));
        assertFalse(StringUtilities.equalsWithTrim(" AbC ", "abc"));
        assertFalse(StringUtilities.equalsWithTrim(null, ""));
        assertFalse(StringUtilities.equalsWithTrim("", null));
        assertTrue(StringUtilities.equalsWithTrim("", "\t\n\r"));
    }

    @Test
    void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("abc", " abc "));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(" abc ", "abc"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("abc", " AbC "));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(" AbC ", "abc"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(null, ""));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("", null));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("", "\t\n\r"));
    }

    @Test
    void testCount() {
        assertEquals(2, StringUtilities.count("abcabc", 'a'));
        assertEquals(0, StringUtilities.count("foo", 'a'));
        assertEquals(0, StringUtilities.count(null, 'a'));
        assertEquals(0, StringUtilities.count("", 'a'));
    }

    @Test
    void testString()
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
    void testEncode() {
        assertEquals("1A", StringUtilities.encode(new byte[]{0x1A}));
        assertEquals("", StringUtilities.encode(new byte[]{}));
    }

    void testEncodeWithNull()
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
    void testDecode() {
        assertArrayEquals(new byte[]{0x1A}, StringUtilities.decode("1A"));
        assertArrayEquals(new byte[]{}, StringUtilities.decode(""));
        assertNull(StringUtilities.decode("1AB"));
    }

    void testDecodeWithNull()
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


    private static Stream<Arguments> charSequenceEquals_caseSensitive() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of("foo", "foo"),
                Arguments.of(new StringBuffer("foo"), "foo"),
                Arguments.of(new StringBuilder("foo"), "foo"),
                Arguments.of(new Segment("foobar".toCharArray(), 0, 3), "foo")
        );
    }



    @ParameterizedTest
    @MethodSource("charSequenceEquals_caseSensitive")
    void testEquals_whenStringsAreEqualCaseSensitive_returnsTrue(CharSequence one, CharSequence two)
    {
        assertThat(StringUtilities.equals(one, two)).isTrue();
    }

    private static Stream<Arguments> charSequenceNotEqual_caseSensitive() {
        return Stream.of(
                Arguments.of(null, ""),
                Arguments.of("", null),
                Arguments.of("foo", "bar"),
                Arguments.of(" foo", "bar"),
                Arguments.of("foO", "foo"),
                Arguments.of("foo", "food"),
                Arguments.of(new StringBuffer("foo"), "bar"),
                Arguments.of(new StringBuffer("foo"), " foo"),
                Arguments.of(new StringBuffer("foO"), "foo"),
                Arguments.of(new StringBuilder("foo"), "bar"),
                Arguments.of(new StringBuilder("foo"), " foo "),
                Arguments.of(new StringBuilder("foO"), "foo"),
                Arguments.of(new Segment("foobar".toCharArray(), 0, 3), "bar"),
                Arguments.of(new Segment(" foo ".toCharArray(), 0, 5), "bar"),
                Arguments.of(new Segment("FOOBAR".toCharArray(), 0, 3), "foo")
        );
    }
    @ParameterizedTest
    @MethodSource("charSequenceNotEqual_caseSensitive")
    void testEquals_whenStringsAreNotEqualCaseSensitive_returnsFalse(CharSequence one, CharSequence two)
    {
        assertThat(StringUtilities.equals(one, two)).isFalse();
    }

    private static Stream<Arguments> charSequenceEquals_ignoringCase() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of("foo", "foo"),
                Arguments.of("FOO", "foo"),
                Arguments.of(new StringBuffer("foo"), "foo"),
                Arguments.of(new StringBuffer("FOO"), "foo"),
                Arguments.of(new StringBuilder("foo"), "foo"),
                Arguments.of(new StringBuilder("FOO"), "foo"),
                Arguments.of(new Segment("foobar".toCharArray(), 0, 3), "foo"),
                Arguments.of(new Segment("FOOBAR".toCharArray(), 0, 3), "foo")
        );
    }

    @ParameterizedTest
    @MethodSource("charSequenceEquals_ignoringCase")
    void testEqualsIgnoreCase_whenStringsAreEqualIgnoringCase_returnsTrue(CharSequence one, CharSequence two)
    {
        assertThat(StringUtilities.equalsIgnoreCase(one, two)).isTrue();
    }

    private static Stream<Arguments> charSequenceNotEqual_ignoringCase() {
        return Stream.of(
                Arguments.of(null, ""),
                Arguments.of("", null),
                Arguments.of("foo", "bar"),
                Arguments.of(" foo ", "foo"),
                Arguments.of(" foo ", "food"),
                Arguments.of(" foo ", "foo"),
                Arguments.of(new StringBuffer("foo"), "bar"),
                Arguments.of(new StringBuffer("foo "), "foo"),
                Arguments.of(new StringBuilder("foo"), "bar"),
                Arguments.of(new StringBuilder("foo "), "foo"),
                Arguments.of(new Segment("foobar".toCharArray(), 0, 3), "bar"),
                Arguments.of(new Segment("foo bar".toCharArray(), 0, 4), "foo")
        );
    }

    @ParameterizedTest
    @MethodSource("charSequenceNotEqual_ignoringCase")
    void testEqualsIgnoreCase_whenStringsAreNotEqualIgnoringCase_returnsFalse(CharSequence one, CharSequence two)
    {
        assertThat(StringUtilities.equalsIgnoreCase(one, two)).isFalse();
    }

    private static Stream<Arguments> charSequenceEquals_afterTrimCaseSensitive() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of("foo", "foo"),
                Arguments.of(" foo", "foo"),
                Arguments.of("foo\r\n", "foo"),
                Arguments.of("foo ", "\tfoo ")
        );
    }

    @ParameterizedTest
    @MethodSource("charSequenceEquals_afterTrimCaseSensitive")
    void testEqualsWithTrim_whenStringsAreEqual_afterTrimCaseSensitive_returnsTrue(String one, String two)
    {
        assertThat(StringUtilities.equalsWithTrim(one, two)).isTrue();
    }

    private static Stream<Arguments> charSequenceNotEqual_afterTrimCaseSensitive() {
        return Stream.of(
                Arguments.of(null, ""),
                Arguments.of("", null),
                Arguments.of("foo", "bar"),
                Arguments.of("F00", "foo"),
                Arguments.of("food", "foo"),
                Arguments.of("foo", "food")

        );
    }

    @ParameterizedTest
    @MethodSource("charSequenceNotEqual_afterTrimCaseSensitive")
    void testEqualsWithTrim_whenStringsAreNotEqual_returnsFalse(String one, String two)
    {
        assertThat(StringUtilities.equalsWithTrim(one, two)).isFalse();
    }

    private static Stream<Arguments> charSequenceEquals_afterTrimAndIgnoringCase() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of("foo", "foo"),
                Arguments.of(" foo", "foo"),
                Arguments.of("foo\r\n", "foo"),
                Arguments.of("foo ", "\tfoo "),
                Arguments.of("FOO", "foo")
        );
    }

    @ParameterizedTest
    @MethodSource("charSequenceEquals_afterTrimAndIgnoringCase")
    void testEqualsIgnoreCaseWithTrim_whenStringsAreEqual_caseSensitive_returnsTrue(String one, String two)
    {
        assertThat(StringUtilities.equalsIgnoreCaseWithTrim(one, two)).isTrue();
    }

    private static Stream<Arguments> charSequenceNotEqual_afterTrimIgnoringCase() {
        return Stream.of(
                Arguments.of(null, ""),
                Arguments.of("", null),
                Arguments.of("foo", "bar"),
                Arguments.of("foo", "food")

        );
    }

    @ParameterizedTest
    @MethodSource("charSequenceNotEqual_afterTrimIgnoringCase")
    void testEqualsIgnoreCaseWithTrim_whenStringsAreNotEqualIgnoringCase_returnsFalse(String one, String two)
    {
        assertThat(StringUtilities.equalsIgnoreCaseWithTrim(one, two)).isFalse();
    }

    @Test
    void testLastIndexOf()
    {
        assertEquals(-1, StringUtilities.lastIndexOf(null, 'a'));
        assertEquals(-1, StringUtilities.lastIndexOf("foo", 'a'));
        assertEquals(1, StringUtilities.lastIndexOf("bar", 'a'));
    }

    @Test
    void testLength()
    {
        assertEquals(0, StringUtilities.length(""));
        assertEquals(0, StringUtilities.length(null));
        assertEquals(3, StringUtilities.length("abc"));
    }

    @Test
    void testLevenshtein()
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
    void testDamerauLevenshtein() throws Exception
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
    void testRandomString()
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

    void testGetBytesWithInvalidEncoding() {
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
    void testGetBytes()
    {
        assertArrayEquals(new byte[]{102, 111, 111}, StringUtilities.getBytes("foo", "UTF-8"));
    }

    @Test
    void testGetUTF8Bytes()
    {
        assertArrayEquals(new byte[]{102, 111, 111}, StringUtilities.getUTF8Bytes("foo"));
    }

    @Test
    void testGetBytesWithNull()
    {
        assert StringUtilities.getBytes(null, "UTF-8") == null;
    }

    @Test
    void testGetBytesWithEmptyString()
    {
        assert DeepEquals.deepEquals(new byte[]{}, StringUtilities.getBytes("", "UTF-8"));
    }

    @Test
    void testWildcard()
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
    void testCreateString()
    {
        assertEquals("foo", StringUtilities.createString(new byte[]{102, 111, 111}, "UTF-8"));
    }

    @Test
    void testCreateUTF8String()
    {
        assertEquals("foo", StringUtilities.createUTF8String(new byte[]{102, 111, 111}));
    }

    @Test
    void testCreateStringWithNull()
    {
        assertNull(null, StringUtilities.createString(null, "UTF-8"));
    }

    @Test
    void testCreateStringWithEmptyArray()
    {
        assertEquals("", StringUtilities.createString(new byte[]{}, "UTF-8"));
    }

    @Test
    void testCreateUTF8StringWithEmptyArray()
    {
        assertEquals("", StringUtilities.createUTF8String(new byte[]{}));
    }

    @Test
    void testCreateStringWithInvalidEncoding()
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
    void testCreateUtf8String()
    {
        assertEquals("foo", StringUtilities.createUtf8String(new byte[] {102, 111, 111}));
    }

    @Test
    void testCreateUtf8StringWithNull()
    {
        assertNull(null, StringUtilities.createUtf8String(null));
    }

    @Test
    void testCreateUtf8StringWithEmptyArray()
    {
        assertEquals("", StringUtilities.createUtf8String(new byte[]{}));
    }

    @Test
    void testHashCodeIgnoreCase()
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
    void testGetBytes_withInvalidEncoding_throwsException() {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> StringUtilities.getBytes("Some text", "foo-bar"))
                .withMessageContaining("Encoding (foo-bar) is not supported");
    }

    @Test
    void testCount2()
    {
        assert 0 == StringUtilities.count("alphabet", null);
        assert 0 == StringUtilities.count(null, "al");
        assert 0 == StringUtilities.count("alphabet", "");
        assert 0 == StringUtilities.count("", "al");
        assert 1 == StringUtilities.count("alphabet", "al");
        assert 2 == StringUtilities.count("halal", "al");
    }

    private static Stream<Arguments> stringsWithAllWhitespace() {
        return Stream.of(
                Arguments.of("       "),
                Arguments.of(" \t "),
                Arguments.of("\r\n ")
        );
    }

    private static Stream<Arguments> stringsWithContentOtherThanWhitespace() {
        return Stream.of(
                Arguments.of("jfk"),
                Arguments.of("  jfk\r\n"),
                Arguments.of("\tjfk  "),
                Arguments.of("    jfk  ")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testTrimToEmpty_whenNullOrEmpty_returnsEmptyString(String value) {
        assertThat(StringUtilities.trimToEmpty(value)).isEqualTo(StringUtilities.EMPTY);
    }

    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void testTrimToEmpty_whenStringIsAllWhitespace_returnsEmptyString(String value) {
        assertThat(StringUtilities.trimToEmpty(value)).isEqualTo(StringUtilities.EMPTY);
    }

    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void testTrimToEmpty_whenStringHasContent_returnsTrimmedString(String value) {
        assertThat(StringUtilities.trimToEmpty(value)).isEqualTo(value.trim());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testTrimToNull_whenNullOrEmpty_returnsNull(String value) {
        assertThat(StringUtilities.trimToNull(value)).isNull();
    }

    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void testTrimToNull_whenStringIsAllWhitespace_returnsNull(String value) {
        assertThat(StringUtilities.trimToNull(value)).isNull();
    }

    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void testTrimToNull_whenStringHasContent_returnsTrimmedString(String value) {
        assertThat(StringUtilities.trimToNull(value)).isEqualTo(value.trim());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testTrimToDefault_whenNullOrEmpty_returnsDefault(String value) {
        assertThat(StringUtilities.trimEmptyToDefault(value, "foo")).isEqualTo("foo");
    }

    @ParameterizedTest
    @MethodSource("stringsWithAllWhitespace")
    void testTrimToDefault_whenStringIsAllWhitespace_returnsDefault(String value) {
        assertThat(StringUtilities.trimEmptyToDefault(value, "foo")).isEqualTo("foo");
    }

    @ParameterizedTest
    @MethodSource("stringsWithContentOtherThanWhitespace")
    void testTrimToDefault_whenStringHasContent_returnsTrimmedString(String value) {
        assertThat(StringUtilities.trimEmptyToDefault(value, "foo")).isEqualTo(value.trim());
    }


    private static Stream<Arguments> regionMatches_returnsTrue() {
        return Stream.of(
                Arguments.of("a",   true,      0,     "abc", 0,     0),
                Arguments.of("a",   true,      0,     "abc", 0,     1),
                Arguments.of("Abc", true,      0,     "abc", 0,     3),
                Arguments.of("Abc", true,      1,     "abc", 1,     2),
                Arguments.of("Abc", false,     1,     "abc", 1,     2),
                Arguments.of("Abcd", true,      1,     "abcD", 1,     2),
                Arguments.of("Abcd", false,     1,     "abcD", 1,     2),
                Arguments.of(new StringBuilder("a"),   true,      0,     new StringBuffer("abc"), 0,     0),
                Arguments.of(new StringBuilder("a"),   true,      0,     new StringBuffer("abc"), 0,     1),
                Arguments.of(new StringBuilder("Abc"), true,      0,     new StringBuffer("abc"), 0,     3),
                Arguments.of(new StringBuilder("Abc"), true,      1,     new StringBuffer("abc"), 1,     2),
                Arguments.of(new StringBuilder("Abc"), false,     1,     new StringBuffer("abc"), 1,     2),
                Arguments.of(new StringBuilder("Abcd"), true,      1,     new StringBuffer("abcD"), 1,     2),
                Arguments.of(new StringBuilder("Abcd"), false,     1,     new StringBuffer("abcD"), 1,     2)

        );
    }
    @ParameterizedTest
    @MethodSource("regionMatches_returnsTrue")
    void testRegionMatches_returnsTrue(CharSequence s, boolean ignoreCase, int start, CharSequence substring, int subStart, int length) {
        boolean matches = StringUtilities.regionMatches(s, ignoreCase, start, substring, subStart, length);
        assertThat(matches).isTrue();
    }

    private static Stream<Arguments> regionMatches_returnsFalse() {
        return Stream.of(
                Arguments.of("",    true,      -1,    "",    -1,    -1),
                Arguments.of("",    true,      0,     "",    0,     1),
                Arguments.of("Abc", false,     0,     "abc", 0,     3),
                Arguments.of(new StringBuilder(""),   true,      -1,     new StringBuffer(""), -1,     -1),
                Arguments.of(new StringBuilder(""),   true,      0,     new StringBuffer(""), 0,     1),
                Arguments.of(new StringBuilder("Abc"), false,      0,     new StringBuffer("abc"), 0,     3)
        );
    }

    @ParameterizedTest
    @MethodSource("regionMatches_returnsFalse")
    void testRegionMatches_returnsFalse(CharSequence s, boolean ignoreCase, int start, CharSequence substring, int subStart, int length) {
        boolean matches = StringUtilities.regionMatches(s, ignoreCase, start, substring, subStart, length);
        assertThat(matches).isFalse();
    }


    private static Stream<Arguments> regionMatches_throwsNullPointerException() {
        return Stream.of(
                Arguments.of("a",   true,      0,     null,  0,     0, "substring cannot be null"),
                Arguments.of(null,  true,      0,     null,  0,     0, "cs to be processed cannot be null"),
                Arguments.of(null,  true,      0,     "",    0,     0, "cs to be processed cannot be null")
        );
    }

    @ParameterizedTest
    @MethodSource("regionMatches_throwsNullPointerException")
    void testRegionMatches_withStrings_throwsIllegalArgumentException(CharSequence s, boolean ignoreCase, int start, CharSequence substring, int subStart, int length, String exText) {
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> StringUtilities.regionMatches(s, ignoreCase, start, substring, subStart, length))
                .withMessageContaining(exText);
    }

}
