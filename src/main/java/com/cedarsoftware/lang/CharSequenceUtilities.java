/*
 * Copyright (c) Cedar Software, LLC
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
 * <p>Useful utilities on {@link java.lang.CharSequence} that are
 * {@code null} safe.</p>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com) 
 * @author Ken Partlow (kpartlow@gmail.com)
 * 
 */
public final class CharSequenceUtilities {

    /**
     * <p>{@code CharSequenceUtilities} instances should NOT be constructed in
     * standard programming. </p>
     *
     */
    private CharSequenceUtilities() {
        super();
    }

    
	// Empty checks
	// -----------------------------------------------------------------------
	/**
	 * <p>
	 * Checks if a CharSequence is empty ("") or null.
	 * </p>
	 * 
	 * <pre>
	 * CharSequenceUtilities.isEmpty(null)      = true
	 * CharSequenceUtilities.isEmpty("")        = true
	 * CharSequenceUtilities.isEmpty(" ")       = false
	 * CharSequenceUtilities.isEmpty("foo")     = false
	 * CharSequenceUtilities.isEmpty("  foo  ") = false
	 * </pre>
	 * 
	 * @param cs
	 *            the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is empty or null
	 */
	public static boolean isEmpty(final CharSequence cs)
	{
		return cs == null || cs.length() == 0;
	}

	/**
	 * <p>
	 * Checks if a CharSequence is not empty ("") and not null.
	 * </p>
	 * 
	 * <pre>
	 * CharSequenceUtilities.isNotEmpty(null)      = false
	 * CharSequenceUtilities.isNotEmpty("")        = false
	 * CharSequenceUtilities.isNotEmpty(" ")       = true
	 * CharSequenceUtilities.isNotEmpty("foo")     = true
	 * CharSequenceUtilities.isNotEmpty("  foo  ") = true
	 * </pre>
	 * 
	 * @param cs
	 *            the CharSequence to check, may be null
	 * @return {@code true} if the CharSequence is not empty and not null
	 */
	public static boolean isNotEmpty(final CharSequence cs)
	{
		return !isEmpty(cs);
	}

	/**
	 * <p>
	 * Checks if a CharSequence is whitespace, empty ("") or null.
	 * </p>
	 * 
	 * <pre>
	 * CharSequenceUtilities.isBlank(null)      = true
	 * CharSequenceUtilities.isBlank("")        = true
	 * CharSequenceUtilities.isBlank(" ")       = true
	 * CharSequenceUtilities.isBlank("foo")     = false
	 * CharSequenceUtilities.isBlank("  foo  ") = false
	 * </pre>
	 * 
	 * @param cs
	 *            the CharSequence to check, may be null
     *
	 * @return {@code true} if the CharSequence is null, empty or whitespace
	 */
	public static boolean isBlank(final CharSequence cs)
	{
		int len;
		if (cs == null || (len = cs.length()) == 0)
		{
			return true;
		}
		for (int i = 0; i < len; i++)
		{
			if (!Character.isWhitespace(cs.charAt(i)))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * <p>
	 * Checks if a CharSequence is not empty (""), not null and not whitespace
	 * only.
	 * </p>
	 * 
	 * <pre>
	 * CharSequenceUtilities.isNotBlank(null)      = false
	 * CharSequenceUtilities.isNotBlank("")        = false
	 * CharSequenceUtilities.isNotBlank(" ")       = false
	 * CharSequenceUtilities.isNotBlank("foo")     = true
	 * CharSequenceUtilities.isNotBlank("  foo  ") = true
	 * </pre>
	 * 
	 * @param cs
	 *            the CharSequence to check, may be null
     *
	 * @return {@code true} if the CharSequence is not empty and not null and
	 *         not whitespace
	 */
	public static boolean isNotBlank(final CharSequence cs)
	{
		return !isBlank(cs);
	}

	/**
	 * <p>
	 * Equivalent to isNotBlank(CharSequence)
	 * </p>
	 * 
	 * <pre>
	 * CharSequenceUtilities.isNotEmpty(null)      = false
	 * CharSequenceUtilities.isNotEmpty("")        = false
	 * CharSequenceUtilities.isNotEmpty(" ")       = false
	 * CharSequenceUtilities.isNotEmpty("foo")     = true
	 * CharSequenceUtilities.isNotEmpty("  foo  ") = true
	 * </pre>
	 * 
	 * @param cs
	 *            the CharSequence to check, may be null
     *
	 * @return {@code true} if the CharSequence is not empty and not null
	 */
	public static boolean hasContent(final CharSequence cs)
	{
		return !isBlank(cs);
	}
    
	/**
	 * <p>
	 * Checks the length of a CharSequence. return 0 length for null
	 * CharSequence.
	 * </p>
	 * 
	 * <pre>
	 * CharSequenceUtilities.length(null)      = 0
	 * CharSequenceUtilities.length("")        = 0
	 * CharSequenceUtilities.length(" ")       = 1
	 * CharSequenceUtilities.length("foo")     = 3
	 * CharSequenceUtilities.length("  foo  ") = 7
	 * </pre>
	 * 
	 * @param cs the CharSequence to return length of
	 * @return 0 if string is null, otherwise the length of string.
	 */
	public static int length(final CharSequence cs)
	{
		return cs == null ? 0 : cs.length();
	}

    /**
     * <p>
     * Returns the length of the trimmed String. If the String is null then it
     * returns 0.
     * </p>
     *
     * <pre>
     * CharSequenceUtilities.lengthAfterTrim(null)      = 0
     * CharSequenceUtilities.lengthAfterTrim("")        = 0
     * CharSequenceUtilities.lengthAfterTrim(" ")       = 0
     * CharSequenceUtilities.lengthAfterTrim("foo")     = 3
     * CharSequenceUtilities.lengthAfterTrim("  foo  ") = 3
     * </pre>
     *
     * @param s
     *            the string to return the length of, may be null.
     * @return 0 if string is null, otherwise the trimmed length of the String.
     */
    public static int lengthAfterTrim(final CharSequence s)
    {
        int len = length(s);

        if (len == 0) {
            return 0;
        }

        int end = len;
        int start = 0;

        while ((start < end) && (s.charAt(start) <= ' ')) {
            start++;
        }
        while ((start < end) && (s.charAt(end - 1) <= ' ')) {
            end--;
        }
        return ((start > 0) || (end < len)) ? end - start : len;
    }

    /**
     * <p>
     * Returns the length of the trimmed String. If the String is null then it
     * returns 0.
     * </p>
     *
     * <pre>
     * CharSequenceUtilities.lengthAfterStrip(null)      = 0
     * CharSequenceUtilities.lengthAfterStrip("")        = 0
     * CharSequenceUtilities.lengthAfterStrip(" ")       = 0
     * CharSequenceUtilities.lengthAfterStrip("foo")     = 3
     * CharSequenceUtilities.lengthAfterStrip("  foo  ") = 3
     * </pre>
     *
     * @param s
     *            the string to return the length of, may be null.
     * @return 0 if string is null, otherwise the trimmed length of the String.
     */
    public static int lengthAfterStrip(final CharSequence s)
    {
        int len = CharSequenceUtilities.length(s);

        if (len == 0) {
            return 0;
        }

        int end = len;
        int start = 0;

        while ((start < end) && (Character.isWhitespace(s.charAt(start)))) {
            start++;
        }
        while ((start < end) && (Character.isWhitespace(s.charAt(end - 1)))) {
            end--;
        }
        return ((start > 0) || (end < len)) ? end - start : len;
    }


    //-----------------------------------------------------------------------
    /**
     * <p>Returns a new {@code CharSequence} that is a subsequence of this
     * sequence starting with the {@code char} value at the specified index.</p>
     *
     * <p>This provides the {@code CharSequence} equivalent to {@link String#substring(int)}.
     * The length (in {@code char}) of the returned sequence is {@code length() - start},
     * so if {@code start == end} then an empty sequence is returned.</p>
     *
     * @param cs  the specified subsequence, null returns null
     * @param start  the start index, inclusive, valid
     * @return a new subsequence, may be null
     * @throws IndexOutOfBoundsException if {@code start} is negative or if 
     *  {@code start} is greater than {@code length()}
     */
    public static CharSequence subSequence(final CharSequence cs, int start) {
        return cs == null ? null : cs.subSequence(start, cs.length());
    }


    /**
     * <p>Returns the instance count of a {@code char} in a {@code CharSequence}.
     *
	 * <pre>
	 * CharSequenceUtilities.count(null, 'c')      = 0
	 * CharSequenceUtilities.count("", 'c')        = 0
	 * CharSequenceUtilities.count("abc", 'd')     = 0
	 * CharSequenceUtilities.count("abc", 'c')     = 1
	 * CharSequenceUtilities.count("ccc", 'c') 	   = 3
	 * CharSequenceUtilities.count(" abc ", ' ')   = 2
	 * </pre>
     *
     * @param cs the specified sequence
     * @param c the char to count
     * @return 0 if null, else the number of times the char occurs
     * in the CharSequence.
     */
	public static int count(final CharSequence cs, final char c)
	{
        int len = length(cs);

        if (len == 0) {
            return 0;
        }

		int count = 0;
		for (int i = 0; i < len; i++)
		{
			if (cs.charAt(i) == c)
			{
				count++;
			}
		}

		return count;
	}

    /**
     * <p>
     * Compares two CharSequences, returning {@code true} if they would be
     * equal after being trimmed.  This method is about an order of
     * magnitude faster than trimming two strings and then calling
     * equals when the strings have to be trimmed.
     * </p>
     *
     * <p>
     * {@code null}s are handled without exceptions. Two {@code null} references
     * are considered equal. Comparison is case sensitive.
     * </p>
     *
     * <pre>
     * StringUtilities.equalsAfterTrim(null, null)   = true
     * StringUtilities.equalsAfterTrim(null, "abc")  = false
     * StringUtilities.equalsAfterTrim("abc", null)  = false
     * StringUtilities.equalsAfterTrim("abc", "abc") = true
     * StringUtilities.equalsAfterTrim("abc", "ABC") = true
     * StringUtilities.equalsAfterTrim(" abc", "ABC ") = false
     * StringUtilities.equalsAfterTrim("abc ", " abc") = true
     * </pre>
     *
     * @param s1
     *            the first String, may be null
     * @param s2
     *            the second String, may be null\
     *
     * @return {@code true} if the String are equal, case insensitive, or both
     *         {@code null}
     */
    public static boolean equalsAfterTrim(final CharSequence s1, final CharSequence s2)
    {
        //return s1 == null ? s2 == null : s1.trim().equals(trim(s2));
        if (s1 == null || s2 == null) {
            return s1 == s2;
        }


        int end1 = s1.length();
        int start1 = 0;

        while ((start1 < end1) && (s1.charAt(start1) <= ' ')) {
            start1++;
        }

        while ((start1 < end1) && (s1.charAt(end1 - 1) <= ' ')) {
            end1--;
        }

        int end2 = s2.length();
        int start2 = 0;

        while ((start2 < end2) && (s2.charAt(start2) <= ' ')) {
            start2++;
        }

        while ((start2 < end2) && (s2.charAt(end2 - 1) <= ' ')) {
            end2--;
        }

        int len1 = end1 - start1;

        if (len1 != (end2 - start2))
        {
            return false;
        }

        return regionMatches(s1, start1, s2, start2, len1);
    }

    /**
     * <p>
     * Compares two CharSequences, returning {@code true} if they would be
     * equal after being trimmed.  This method is about an order of
     * magnitude faster than trimming two strings and then calling
     * equals when the strings have to be trimmed.
     * </p>
     *
     * <p>
     * {@code null}s are handled without exceptions. Two {@code null} references
     * are considered equal. Comparison is case insensitive.
     * </p>
     *
     * <pre>
     * StringUtilities.equalsIgnoreCaseAfterTrim(null, null)   = true
     * StringUtilities.equalsIgnoreCaseAfterTrim(null, "abc")  = false
     * StringUtilities.equalsIgnoreCaseAfterTrim("abc", null)  = false
     * StringUtilities.equalsIgnoreCaseAfterTrim("abc", "abc") = true
     * StringUtilities.equalsIgnoreCaseAfterTrim("abc", "ABC") = true
     * StringUtilities.equalsIgnoreCaseAfterTrim(" abc", "ABC ") = true
     * StringUtilities.equalsIgnoreCaseAfterTrim("abc ", " abc") = true
     * </pre>
     *
     * @param s1
     *            the first String, may be null
     * @param s2
     *            the second String, may be null
     * @return {@code true} if the String are equal, case insensitive, or both
     *         {@code null}
     */
    public static boolean equalsIgnoreCaseAfterTrim(final CharSequence s1, final CharSequence s2)
    {
        if (s1 == null || s2 == null) {
            return s1 == s2;
        }

        int end1 = s1.length();
        int start1 = 0;

        while ((start1 < end1) && (s1.charAt(start1) <= ' ')) {
            start1++;
        }

        while ((start1 < end1) && (s1.charAt(end1 - 1) <= ' ')) {
            end1--;
        }

        int end2 = s2.length();
        int start2 = 0;

        while ((start2 < end2) && (s2.charAt(start2) <= ' ')) {
            start2++;
        }

        while ((start2 < end2) && (s2.charAt(end2 - 1) <= ' ')) {
            end2--;
        }

        int len1 = end1 - start1;

        if (len1 != (end2 - start2))
        {
            return false;
        }

        return regionMatchesIgnoreCase(s1, start1, s2, start2, len1);
    }


    /**
     * <p>
     * Compares two regions of a CharSequence for equality.  This method
     * assumes that null checks have been done on the two CharSequences,
     * but does a length check.
     * </p>
     *
     * @param s1
     *            the first CharSequence, cannot be {@code null}
     * @param s2
     *            the second CharSequence, cannot be {@code null}
     * @return {@code true} if the CharSequences are equal

     * @throws NullPointerException if either s1 or s2 are null.
     */
    public static boolean regionMatches(CharSequence s1, int s1Offset, CharSequence s2, int s2Offset,
                                        int len) {
        // Note: s1Offset, s2Offset, or len might be near -1>>>1.
        if ((s2Offset < 0) || (s1Offset < 0)
                || (s1Offset > (long)s1.length() - len)
                || (s2Offset > (long)s2.length() - len)) {
            return false;
        }
        while (len-- > 0) {
            if (s1.charAt(s1Offset++) != s2.charAt(s2Offset++)) {
                return false;
            }
        }
        return true;
    }

    /**
     * <p>
     * Compares two regions of a CharSequence for equality ignoring case.
     * This method assumes that null checks have been done on the two
     * CharSequences, but does a length check.
     * </p>
     *
     * @param s1
     *            the first CharSequence, cannot be {@code null}
     * @param s2
     *            the second CharSequence, cannot be {@code null}
     * @return {@code true} if the CharSequences are equal

     * @throws NullPointerException if either s1 or s2 are null.
     */
    public static boolean regionMatchesIgnoreCase(CharSequence s1, int s1Offset, CharSequence s2, int s2Offset, int len) {
        // Note: s1Offset, s2Offset, or len might be near -1>>>1.
        if ((s2Offset < 0) || (s1Offset < 0)
                || (s1Offset > (long)s1.length() - len)
                || (s2Offset > (long)s2.length() - len)) {
            return false;
        }
        while (len-- > 0) {
            char c1 = s1.charAt(s1Offset++);
            char c2 = s2.charAt(s2Offset++);
            if (c1 == c2) {
                continue;
            }
            // If characters don't match but case may be ignored,
            // try converting both characters to uppercase.
            // If the results match, then the comparison scan should
            // continue.
            char u1 = Character.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if (u1 == u2) {
                continue;
            }
            // Unfortunately, conversion to uppercase does not work properly
            // for the Georgian alphabet, which has strange rules about case
            // conversion.  So we need to make one last check before
            // exiting.
            if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                continue;
            }
            return false;
        }
        return true;
    }


}
