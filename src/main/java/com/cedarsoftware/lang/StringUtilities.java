/*
 *         Copyright (c) Cedar Software LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License.  You may 
 * obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cedarsoftware.lang;

/**
 * Useful String utilities for common tasks.  Also, see
 * {@code CharSequenceUtilities} for string methods that
 * can also apply to StringBuffer, StringBuilder, and other
 * classes that implement {@code CharSequence}.
 * 
 * @author John DeRegnaucourt (jdereg@gmail.com) 
 * @author Ken Partlow (kpartlow@gmail.com)
 * 
 */
public final class StringUtilities
{
	// no longer used?
	//public static final String FOLDER_SEPARATOR = "/";

	/**
	 * The empty String {@code ""}.
	 */
	public static final String EMPTY = "";

	/**
	 * Represents a failed index search.
	 */
	public static final int INDEX_NOT_FOUND = -1;

	/**
	 * <p>
	 * {@code StringUtilities} instances should NOT be constructed in standard
	 * programming. Instead, the class should be used statically as
	 * {@code StringUtilities.trim();}.
	 * </p>
	 * 
	 */
	private StringUtilities()
	{
		super();
	}

	// Trim
	// -----------------------------------------------------------------------
	/**
	 * <p>
	 * Removes control characters (char &lt;= 32) from both ends of this String,
	 * handling {@code null} by returning {@code null}.
	 * </p>
	 * 
	 * <p>
	 * The String is trimmed using {@link String#trim()}. Trim removes start and
	 * end characters &lt;= 32. To strip whitespace use {@link #strip(String)}.
	 * </p>
	 * 
	 * <p>
	 * To trim your choice of characters, use the {@link #strip(String, String)}
	 * methods.
	 * </p>
	 * 
	 * <pre>
	 * StringUtilities.trim(null)          = null
	 * StringUtilities.trim("")            = ""
	 * StringUtilities.trim("     ")       = ""
	 * StringUtilities.trim("abc")         = "abc"
	 * StringUtilities.trim("    abc    ") = "abc"
	 * </pre>
	 * 
	 * @param s
	 *            the String to be trimmed, may be null
	 * @return the trimmed string, {@code null} if null String input
	 */
	public static String trim(final String s)
	{
		return s == null ? null : s.trim();
	}

    /**
	 * <p>
	 * Removes control characters (char &lt;= 32) from both ends of this String
	 * returning {@code null} if the String is empty ("") after the trim or if
	 * it is {@code null}.
	 * 
	 * <p>
	 * The String is trimmed using {@link String#trim()}. Trim removes start and
	 * end characters &lt;= 32. To strip whitespace use
	 * {@link #stripToNull(String)}.
	 * </p>
	 * 
	 * <pre>
	 * StringUtilities.trimToNull(null)          = null
	 * StringUtilities.trimToNull("")            = null
	 * StringUtilities.trimToNull("     ")       = null
	 * StringUtilities.trimToNull("abc")         = "abc"
	 * StringUtilities.trimToNull("    abc    ") = "abc"
	 * </pre>
	 * 
	 * @param str
	 *            the String to be trimmed, may be null
	 * @return the trimmed String, {@code null} if only chars &lt;= 32, empty or
	 *         null String input
	 */
	public static String trimToNull(final String str)
	{
		String ts = trim(str);
		return CharSequenceUtilities.isEmpty(ts) ? null : ts;
	}

	/**
	 * <p>
	 * Removes control characters (char &lt;= 32) from both ends of this String
	 * returning an empty String ("") if the String is empty ("") after the trim
	 * or if it is {@code null}.
	 * 
	 * <p>
	 * The String is trimmed using {@link String#trim()}. Trim removes start and
	 * end characters &lt;= 32. To strip whitespace use
	 * {@link #stripToEmpty(String)}.
	 * </p>
	 * 
	 * <pre>
	 * StringUtilities.trimToEmpty(null)          = ""
	 * StringUtilities.trimToEmpty("")            = ""
	 * StringUtilities.trimToEmpty("     ")       = ""
	 * StringUtilities.trimToEmpty("abc")         = "abc"
	 * StringUtilities.trimToEmpty("    abc    ") = "abc"
	 * </pre>
	 * 
	 * @param str
	 *            the String to be trimmed, may be null
	 * @return the trimmed String, or an empty String if {@code null} input
	 */
	public static String trimToEmpty(final String str)
	{
		return str == null ? EMPTY : str.trim();
	}

    // Stripping
    //-----------------------------------------------------------------------
    /**
     * <p>Strips whitespace from the start and end of a String.</p>
     *
     * <p>This is similar to {@link #trim(String)} but removes whitespace.
     * Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <p>A {@code null} input String returns {@code null}.</p>
     *
     * <pre>
     * StringUtilities.strip(null)     = null
     * StringUtilities.strip("")       = ""
     * StringUtilities.strip("   ")    = ""
     * StringUtilities.strip("abc")    = "abc"
     * StringUtilities.strip("  abc")  = "abc"
     * StringUtilities.strip("abc  ")  = "abc"
     * StringUtilities.strip(" abc ")  = "abc"
     * StringUtilities.strip(" ab c ") = "ab c"
     * </pre>
     *
     * @param s  the String to remove whitespace from, may be null
     * @return the stripped String, {@code null} if null String input
     */
    public static String strip(String s) {
        int len = CharSequenceUtilities.length(s);
        if (len == 0) {
            return s;
        }

        int start = 0;
        int end = len;

        while ((start < end) && (Character.isWhitespace(s.charAt(start)))) {
            start++;
        }

        while ((start < end) && (Character.isWhitespace(s.charAt(end-1)))) {
            end--;
        }
        return ((start > 0) || (end < len)) ? s.substring(start, end) : s;
    }

    /**
     * <p>Strips whitespace from the start and end of a String  returning
     * {@code null} if the String is empty ("") after the strip.</p>
     *
     * <p>This is similar to {@link #trimToNull(String)} but removes whitespace.
     * Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtilities.stripToNull(null)     = null
     * StringUtilities.stripToNull("")       = null
     * StringUtilities.stripToNull("   ")    = null
     * StringUtilities.stripToNull("abc")    = "abc"
     * StringUtilities.stripToNull("  abc")  = "abc"
     * StringUtilities.stripToNull("abc  ")  = "abc"
     * StringUtilities.stripToNull(" abc ")  = "abc"
     * StringUtilities.stripToNull(" ab c ") = "ab c"
     * </pre>
     *
     * @param str  the String to be stripped, may be null
     * @return the stripped String,
     *  {@code null} if whitespace, empty or null String input
     */
    public static String stripToNull(String str) {
        if (str == null) {
            return null;
        }
        str = strip(str);
        return str.length() == 0 ? null : str;
    }

    /**
     * <p>Strips whitespace from the start and end of a String  returning
     * an empty String if {@code null} input.</p>
     *
     * <p>This is similar to {@link #trimToEmpty(String)} but removes whitespace.
     * Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtilities.stripToEmpty(null)     = ""
     * StringUtilities.stripToEmpty("")       = ""
     * StringUtilities.stripToEmpty("   ")    = ""
     * StringUtilities.stripToEmpty("abc")    = "abc"
     * StringUtilities.stripToEmpty("  abc")  = "abc"
     * StringUtilities.stripToEmpty("abc  ")  = "abc"
     * StringUtilities.stripToEmpty(" abc ")  = "abc"
     * StringUtilities.stripToEmpty(" ab c ") = "ab c"
     * </pre>
     *
     * @param s  the String to be stripped, may be null
     * @return the stripped String, or an empty String if {@code null} input
     */
    public static String stripToEmpty(String s) {
        return s == null ? EMPTY : strip(s);
    }

    /**
     * <p>Strips any of a set of characters from the start and end of a String.
     * This is similar to {@link String#trim()} but allows the characters
     * to be stripped to be controlled.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * An empty string ("") input returns the empty string.</p>
     *
     * <p>If the stripChars String is {@code null}, whitespace is
     * stripped as defined by {@link Character#isWhitespace(char)}.
     * Alternatively use {@link #strip(String)}.</p>
     *
     * <pre>
     * StringUtilities.strip(null, *)          = null
     * StringUtilities.strip("", *)            = ""
     * StringUtilities.strip("abc", null)      = "abc"
     * StringUtilities.strip("  abc", null)    = "abc"
     * StringUtilities.strip("abc  ", null)    = "abc"
     * StringUtilities.strip(" abc ", null)    = "abc"
     * StringUtilities.strip("  abcyx", "xyz") = "  abc"
     * </pre>
     *
     * @param s  the String to remove characters from, may be null
     * @param stripChars  the characters to remove, null treated as whitespace
     * @return the stripped String, {@code null} if null String input
     */
    public static String strip(String s, String stripChars) {
        int len = CharSequenceUtilities.length(s);
        if (len == 0 || CharSequenceUtilities.isEmpty(stripChars)) {
            return s;
        }

        int start = 0;
        while (start < len && stripChars.indexOf(s.charAt(start)) != INDEX_NOT_FOUND) {
            start++;
        }

        int end = len;
        while (end != start && stripChars.indexOf(s.charAt(end - 1)) != INDEX_NOT_FOUND) {
            end--;
        }

        return ((start > 0) || (end < len)) ? s.substring(start, end) : s;
    }

    /**
     * <p>Strips all whitespace from a string.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * An empty string ("") input returns the empty string.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtilities.stripStart(null, *)          = null
     * StringUtilities.stripStart("", *)            = ""
     * StringUtilities.stripStart("abc", "")        = "abc"
     * StringUtilities.stripStart("abc", null)      = "abc"
     * StringUtilities.stripStart("  abc", null)    = "abc"
     * StringUtilities.stripStart("abc  ", null)    = "abc  "
     * StringUtilities.stripStart(" abc ", null)    = "abc "
     * StringUtilities.stripStart("yxabc  ", "xyz") = "abc  "
     * </pre>
     *
     * @param s  the String to remove characters from, may be null
     * @return the stripped String, {@code null} if null String input
     */
    public static String stripStart(String s) {
        int len = CharSequenceUtilities.length(s);
        if (len == 0) {
            return s;
        }
        int start = 0;
        while (start != len && Character.isWhitespace(s.charAt(start))) {
            start++;
        }
        return s.substring(start);
    }

    /**
     * <p>Strips any of a set of characters from the start of a String.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * An empty string ("") input returns the empty string.</p>
     *
     * <p>If the stripChars String is {@code null}, whitespace is
     * stripped as defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtilities.stripStart(null, *)          = null
     * StringUtilities.stripStart("", *)            = ""
     * StringUtilities.stripStart("abc", "")        = "abc"
     * StringUtilities.stripStart("abc", null)      = "abc"
     * StringUtilities.stripStart("  abc", null)    = "abc"
     * StringUtilities.stripStart("abc  ", null)    = "abc  "
     * StringUtilities.stripStart(" abc ", null)    = "abc "
     * StringUtilities.stripStart("yxabc  ", "xyz") = "abc  "
     * </pre>
     *
     * @param s  the String to remove characters from, may be null
     * @param stripChars  the characters to remove, null treated as whitespace
     * @return the stripped String, {@code null} if null String input
     */
    public static String stripStart(String s, String stripChars) {
        int len = CharSequenceUtilities.length(s);
        if (len == 0 || CharSequenceUtilities.isEmpty(stripChars)) {
            return s;
        }
        int start = 0;
        while (start != len && stripChars.indexOf(s.charAt(start)) != INDEX_NOT_FOUND) {
            start++;
        }
        return (start > 0) ? s.substring(start) : s;
    }
    /**
     * <p>Strips whitespace from the end of a String.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * An empty string ("") input returns the empty string.</p>
     *
     * <p>Whitespace is defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtilities.stripEnd(null)          = null
     * StringUtilities.stripEnd("")            = ""
     * StringUtilities.stripEnd("abc")        = "abc"
     * StringUtilities.stripEnd("  abc")    = "  abc"
     * StringUtilities.stripEnd("abc  ")    = "abc"
     * StringUtilities.stripEnd(" abc ")    = " abc"
     * </pre>
     *
     * @param s  the String to remove characters from, may be null
     * @return the stripped String, {@code null} if null String input
     */
    public static String stripEnd(String s) {
        int len = CharSequenceUtilities.length(s);
        if (len == 0) {
            return s;
        }

        int end = len;
        while (end != 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }

        return (end < len) ? s.substring(0, end) : s;
    }

    /**
     * <p>Strips any of a set of characters from the end of a String.</p>
     *
     * <p>A {@code null} input String returns {@code null}.
     * An empty string ("") input returns the empty string.</p>
     *
     * <p>If the stripChars String is {@code null}, whitespace is
     * stripped as defined by {@link Character#isWhitespace(char)}.</p>
     *
     * <pre>
     * StringUtilities.stripEnd(null, *)          = null
     * StringUtilities.stripEnd("", *)            = ""
     * StringUtilities.stripEnd("abc", "")        = "abc"
     * StringUtilities.stripEnd("abc", null)      = "abc"
     * StringUtilities.stripEnd("  abc", null)    = "  abc"
     * StringUtilities.stripEnd("abc  ", null)    = "abc"
     * StringUtilities.stripEnd(" abc ", null)    = " abc"
     * StringUtilities.stripEnd("  abcyx", "xyz") = "  abc"
     * StringUtilities.stripEnd("120.00", ".0")   = "12"
     * </pre>
     *
     * @param s  the String to remove characters from, may be null
     * @param stripChars  the set of characters to remove, null treated as whitespace
     * @return the stripped String, {@code null} if null String input
     */
    public static String stripEnd(String s, String stripChars) {
        int len = CharSequenceUtilities.length(s);
        if (len == 0 || CharSequenceUtilities.isEmpty(stripChars)) {
            return s;
        }

        int end = len;
        while (end != 0 && stripChars.indexOf(s.charAt(end - 1)) != INDEX_NOT_FOUND) {
            end--;
        }
        return (end < len) ? s.substring(0, end) : s;
    }

    // Equals
	// -----------------------------------------------------------------------
	/**
	 * <p>
	 * Compares two Strings, returning {@code true} if they are equal.
	 * </p>
	 * 
	 * <p>
	 * {@code null}s are handled without exceptions. Two {@code null} references
	 * are considered to be equal. The comparison is case sensitive.
	 * </p>
	 * 
	 * <pre>
	 * StringUtilities.equals(null, null)   = true
	 * StringUtilities.equals(null, "abc")  = false
	 * StringUtilities.equals("abc", null)  = false
	 * StringUtilities.equals("abc", "abc") = true
	 * StringUtilities.equals("abc", "ABC") = false
	 * StringUtilities.equals(" abc", "ABC ") = false
	 * StringUtilities.equals("abc ", " abc") = false
	 * </pre>
	 * 
	 * @see java.lang.String#equals(Object)
     *
	 * @param s1
	 *            the first String, may be null
	 * @param s2
	 *            the second String, may be null
     *
	 * @return {@code true} if the Strings are equal, case sensitive, or
	 *         both {@code null}
	 */
	public static boolean equals(final String s1, final String s2)
	{
		return s1 == null ? s2 == null : s1.equals(s2);
	}

	/**
	 * <p>
	 * Compares two CharSequences, returning {@code true} if they are equal
	 * ignoring the case.
	 * </p>
	 * 
	 * <p>
	 * {@code null}s are handled without exceptions. Two {@code null} references
	 * are considered equal. Comparison is case insensitive.
	 * </p>
	 * 
	 * <pre>
	 * StringUtilities.equalsIgnoreCase(null, null)   = true
	 * StringUtilities.equalsIgnoreCase(null, "abc")  = false
	 * StringUtilities.equalsIgnoreCase("abc", null)  = false
	 * StringUtilities.equalsIgnoreCase("abc", "abc") = true
	 * StringUtilities.equalsIgnoreCase("abc", "ABC") = true
	 * StringUtilities.equalsIgnoreCase(" abc", "ABC ") = false
	 * StringUtilities.equalsIgnoreCase("abc ", " abc") = false
	 * </pre>
	 * 
	 * @param s1
	 *            the first String, may be null
	 * @param s2
	 *            the second String, may be null
     *
	 * @return {@code true} if the Strings are equal, case insensitive, or
	 *         both {@code null}
	 */
	public static boolean equalsIgnoreCase(final String s1, final String s2)
	{
		return s1 == null ? s2 == null : s1.equalsIgnoreCase(s2);
	}


    /**
	 * <p>
	 * Checks the length of a CharSequence. return 0 length for null
	 * CharSequence.
	 * </p>
	 * 
	 * <pre>
	 * StringUtilities.length(null)      = 0
	 * StringUtilities.length("")        = 0
	 * StringUtilities.length(" ")       = 1
	 * StringUtilities.length("foo")     = 3
	 * StringUtilities.length("  foo  ") = 7
	 * </pre>
	 * 
	 * @deprecated use CharSequenceUtilities.length()
	 * @param cs
	 *            the CharSequence to return length of
	 * @return 0 if string is null, otherwise the length of string.
	 */
	public static int length(final CharSequence cs)
	{
		return CharSequenceUtilities.length(cs);
	}


	// IndexOf
	// -----------------------------------------------------------------------
	/**
	 * <p>
	 * Finds the first index within a CharSequence, handling {@code null}. This
	 * method uses {@link String#indexOf(int, int)} if possible.
	 * </p>
	 * 
	 * <p>
	 * A {@code null} or empty ("") CharSequence will return
	 * {@code INDEX_NOT_FOUND (-1)}.
	 * </p>
	 * 
	 * <pre>
	 * StringUtilities.indexOf(null, *)         = -1
	 * StringUtilities.indexOf("", *)           = -1
	 * StringUtilities.indexOf("aabaabaa", 'a') = 0
	 * StringUtilities.indexOf("aabaabaa", 'b') = 2
	 * </pre>
	 * 
	 * @param s
	 *            the String to check, may be null
	 * @param ch
	 *            the character to find
	 * @return the first index of the search character, -1 if no match or
	 *         {@code null} string input
	 */
	public static int indexOf(String s, int ch)
	{
		return null == s ? INDEX_NOT_FOUND : s.indexOf(ch);
	}

	/**
	 * <p>
	 * Finds the first index within a String from a start position,
	 * handling {@code null}. This method uses {@link String#indexOf(int, int)}
	 * if possible.
	 * </p>
	 * 
	 * <p>
	 * A {@code null} or empty ("") CharSequence will return
	 * {@code (INDEX_NOT_FOUND) -1}. A negative start position is treated as
	 * zero. A start position greater than the string length returns {@code -1}.
	 * </p>
	 * 
	 * <pre>
	 * StringUtilities.indexOf(null, *, *)          = -1
	 * StringUtilities.indexOf("", *, *)            = -1
	 * StringUtilities.indexOf("aabaabaa", 'b', 0)  = 2
	 * StringUtilities.indexOf("aabaabaa", 'b', 3)  = 5
	 * StringUtilities.indexOf("aabaabaa", 'b', 9)  = -1
	 * StringUtilities.indexOf("aabaabaa", 'b', -1) = 2
	 * </pre>
	 * 
	 * @param s
	 *            the CharSequence to check, may be null
	 * @param ch
	 *            the character to find
	 * @param start
	 *            the start position, negative treated as zero
	 * @return the first index of the search character, -1 if no match or
	 *         {@code null} string input
	 */
	public static int indexOf(String s, int ch, int start)
	{
		return null == s ? INDEX_NOT_FOUND : s.indexOf(ch, start);
	}

	/**
	 * <p>
	 * Finds the first index within a String, handling {@code null}. This
	 * method uses {@link String#indexOf(String, int)} if possible.
	 * </p>
	 * 
	 * <p>
	 * A {@code null} CharSequence will return {@code -1}.
	 * </p>
	 * 
	 * <pre>
	 * StringUtilities.indexOf(null, *)          = -1
	 * StringUtilities.indexOf(*, null)          = -1
	 * StringUtilities.indexOf("", "")           = 0
	 * StringUtilities.indexOf("", *)            = -1 (except when * = "")
	 * StringUtilities.indexOf("aabaabaa", "a")  = 0
	 * StringUtilities.indexOf("aabaabaa", "b")  = 2
	 * StringUtilities.indexOf("aabaabaa", "ab") = 1
	 * StringUtilities.indexOf("aabaabaa", "")   = 0
	 * </pre>
	 * 
	 * @param s
	 *            the CharSequence to check, may be null
	 * @param searchStr
	 *            the CharSequence to find, may be null
	 * @return the first index of the search CharSequence, -1 if no match or
	 *         {@code null} string input
	 */
	public static int indexOf(String s, String searchStr)
	{
		return (s == null || searchStr == null) ? INDEX_NOT_FOUND : s.indexOf(searchStr);
	}

	/**
	 * <p>
	 * Finds the first index within a CharSequence, handling {@code null}. This
	 * method uses {@link String#indexOf(String, int)} if possible.
	 * </p>
	 * 
	 * <p>
	 * A {@code null} CharSequence will return {@code -1}. A negative start
	 * position is treated as zero. An empty ("") search CharSequence always
	 * matches. A start position greater than the string length only matches an
	 * empty search CharSequence.
	 * </p>
	 * 
	 * <pre>
	 * StringUtilities.indexOf(null, *, *)          = -1
	 * StringUtilities.indexOf(*, null, *)          = -1
	 * StringUtilities.indexOf("", "", 0)           = 0
	 * StringUtilities.indexOf("", *, 0)            = -1 (except when * = "")
	 * StringUtilities.indexOf("aabaabaa", "a", 0)  = 0
	 * StringUtilities.indexOf("aabaabaa", "b", 0)  = 2
	 * StringUtilities.indexOf("aabaabaa", "ab", 0) = 1
	 * StringUtilities.indexOf("aabaabaa", "b", 3)  = 5
	 * StringUtilities.indexOf("aabaabaa", "b", 9)  = -1
	 * StringUtilities.indexOf("aabaabaa", "b", -1) = 2
	 * StringUtilities.indexOf("aabaabaa", "", 2)   = 2
	 * StringUtilities.indexOf("abc", "", 9)        = 3
	 * </pre>
	 * 
	 * @param s
	 *            the String to check, may be null
	 * @param search
	 *            the String to find, may be null
	 * @param start
	 *            the start position, negative treated as zero
	 * @return the first index of the search String, -1 if no match or
	 *         {@code null} string input
	 */
	public static int indexOf(String s, String search, int start)
	{
		return (s == null || search == null) ? INDEX_NOT_FOUND : s.indexOf(search, start); 
	}

    // LastIndexOf
    //-----------------------------------------------------------------------
    /**
     * <p>Finds the last index within a CharSequence, handling {@code null}.
     * This method uses {@link String#lastIndexOf(int)} if possible.</p>
     *
     * <p>A {@code null} or empty ("") CharSequence will return {@code -1}.</p>
     *
     * <pre>
     * StringUtilities.lastIndexOf(null, *)         = -1
     * StringUtilities.lastIndexOf("", *)           = -1
     * StringUtilities.lastIndexOf("aabaabaa", 'a') = 7
     * StringUtilities.lastIndexOf("aabaabaa", 'b') = 5
     * </pre>
     *
     * @param s  the CharSequence to check, may be null
     * @param ch  the character to find
     * @return the last index of the search character,
     *  -1 if no match or {@code null} string input
     */
    public static int lastIndexOf(String s, int ch) {
    	return null == s ? INDEX_NOT_FOUND : s.lastIndexOf(ch);
    }

    /**
     * <p>Finds the last index within a CharSequence from a start position,
     * handling {@code null}.
     * This method uses {@link String#lastIndexOf(int, int)} if possible.</p>
     *
     * <p>A {@code null} or empty ("") CharSequence will return {@code -1}.
     * A negative start position returns {@code -1}.
     * A start position greater than the string length searches the whole string.</p>
     *
     * <pre>
     * StringUtilities.lastIndexOf(null, *, *)          = -1
     * StringUtilities.lastIndexOf("", *,  *)           = -1
     * StringUtilities.lastIndexOf("aabaabaa", 'b', 8)  = 5
     * StringUtilities.lastIndexOf("aabaabaa", 'b', 4)  = 2
     * StringUtilities.lastIndexOf("aabaabaa", 'b', 0)  = -1
     * StringUtilities.lastIndexOf("aabaabaa", 'b', 9)  = 5
     * StringUtilities.lastIndexOf("aabaabaa", 'b', -1) = -1
     * StringUtilities.lastIndexOf("aabaabaa", 'a', 0)  = 0
     * </pre>
     *
     * @param s  the CharSequence to check, may be null
     * @param searchStr  the character to find
     * @param from  the start position
     * @return the last index of the search character,
     *  -1 if no match or {@code null} string input
     */
    public static int lastIndexOf(String s, int searchStr, int from) {
    	return CharSequenceUtilities.isEmpty(s) ? INDEX_NOT_FOUND : s.lastIndexOf(searchStr, from);
    }

    /**
     * <p>Finds the last index within a CharSequence, handling {@code null}.
     * This method uses {@link String#lastIndexOf(String)} if possible.</p>
     *
     * <p>A {@code null} CharSequence will return {@code -1}.</p>
     *
     * <pre>
     * StringUtilities.lastIndexOf(null, *)          = -1
     * StringUtilities.lastIndexOf(*, null)          = -1
     * StringUtilities.lastIndexOf("", "")           = 0
     * StringUtilities.lastIndexOf("aabaabaa", "a")  = 7
     * StringUtilities.lastIndexOf("aabaabaa", "b")  = 5
     * StringUtilities.lastIndexOf("aabaabaa", "ab") = 4
     * StringUtilities.lastIndexOf("aabaabaa", "")   = 8
     * </pre>
     *
     * @param s  the String to check, may be null
     * @param search  the String to find, may be null
     * @return the last index of the search String,
     *  -1 if no match or {@code null} string input
     */
    public static int lastIndexOf(String s, String search) {
    	return (s == null || search == null) ? INDEX_NOT_FOUND : s.lastIndexOf(search);
    }

    /**
     * <p>Finds the first index within a CharSequence, handling {@code null}.
     * This method uses {@link String#lastIndexOf(String, int)} if possible.</p>
     *
     * <p>A {@code null} CharSequence will return {@code -1}.
     * A negative start position returns {@code -1}.
     * An empty ("") search CharSequence always matches unless the start position is negative.
     * A start position greater than the string length searches the whole string.</p>
     *
     * <pre>
     * StringUtilities.lastIndexOf(null, *, *)          = -1
     * StringUtilities.lastIndexOf(*, null, *)          = -1
     * StringUtilities.lastIndexOf("aabaabaa", "a", 8)  = 7
     * StringUtilities.lastIndexOf("aabaabaa", "b", 8)  = 5
     * StringUtilities.lastIndexOf("aabaabaa", "ab", 8) = 4
     * StringUtilities.lastIndexOf("aabaabaa", "b", 9)  = 5
     * StringUtilities.lastIndexOf("aabaabaa", "b", -1) = -1
     * StringUtilities.lastIndexOf("aabaabaa", "a", 0)  = 0
     * StringUtilities.lastIndexOf("aabaabaa", "b", 0)  = -1
     * </pre>
     *
     * @param s  the String to check, may be null
     * @param search  the String to find, may be null
     * @param start  the start position, negative treated as zero
     * @return the first index of the search CharSequence,
     *  -1 if no match or {@code null} string input
     */
    public static int lastIndexOf(String s, String search, int start) {
    	return (s == null || search == null) ? INDEX_NOT_FOUND : s.lastIndexOf(search, start);
    }
    

    // Contains
    //-----------------------------------------------------------------------
    /**
     * <p>Checks if String contains a search character, handling {@code null}.
     * This method uses {@link String#indexOf(int)} if possible.</p>
     *
     * <p>A {@code null} or empty ("") CharSequence will return {@code false}.</p>
     *
     * <pre>
     * StringUtilities.contains(null, *)    = false
     * StringUtilities.contains("", *)      = false
     * StringUtilities.contains("abc", 'a') = true
     * StringUtilities.contains("abc", 'z') = false
     * </pre>
     *
     * @param s  the String to check, may be null
     * @param ch  the character to find
     * @return true if the CharSequence contains the search character,
     *  false if not or {@code null} string input
     */
    public static boolean contains(String s, int ch) {
    	return indexOf(s, ch) > -1;
    }
    

    
}
