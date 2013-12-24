package com.cedarsoftware.util;

/**
 * Useful String utilities for common tasks
 *
 * @author John DeRegnaucourt (jdereg@gmail.com) & Ken Partlow
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class StringUtilities
{
    private static final char[] _hex = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    public static final String FOLDER_SEPARATOR = "/";

    /**
     * <p>Constructor is declared private since all methods are static.</p>
     */
    private StringUtilities()
    {
        super();
    }

    public static boolean equals(final String str1, final String str2)
    {
        if (str1 == null || str2 == null)
        {
            return str1 == str2;
        }
        return str1.equals(str2);
    }

    public static boolean equalsIgnoreCase(final String s1, final String s2)
    {
        if (s1 == null || s2 == null)
        {
            return s1 == s2;
        }
        return s1.equalsIgnoreCase(s2);
    }

    public static boolean equalsWithTrim(final String s1, final String s2)
    {
        if (s1 == null || s2 == null)
        {
            return s1 == s2;
        }
        return s1.trim().equals(s2.trim());
    }

    public static boolean equalsIgnoreCaseWithTrim(final String s1, final String s2)
    {
        if (s1 == null || s2 == null)
        {
            return s1 == s2;
        }
        return s1.trim().equalsIgnoreCase(s2.trim());
    }

    public static boolean isEmpty(final String s)
    {
        return trimLength(s) == 0;
    }

    public static boolean hasContent(final String s)
    {
        return !(trimLength(s) == 0);    // faster than returning !isEmpty()
    }

    /**
     * Use this method when you don't want a length check to
     * throw a NullPointerException when
     *
     * @param s string to return length of
     * @return 0 if string is null, otherwise the length of string.
     */
    public static int length(final String s)
    {
        return s == null ? 0 : s.length();
    }

    /**
     * Returns the length of the trimmed string.  If the length is
     * null then it returns 0.
     */
    public static int trimLength(final String s)
    {
        return (s == null) ? 0 : s.trim().length();
    }

    public static int lastIndexOf(String path, char ch)
    {
        if (path == null)
        {
            return -1;
        }
        return path.lastIndexOf(ch);
    }

    // Turn hex String into byte[]
    // If string is not even length, return null.

    public static byte[] decode(String s)
    {
        int len = s.length();
        if (len % 2 != 0)
        {
            return null;
        }

        byte[] bytes = new byte[len / 2];
        int pos = 0;

        for (int i = 0; i < len; i += 2)
        {
            byte hi = (byte) Character.digit(s.charAt(i), 16);
            byte lo = (byte) Character.digit(s.charAt(i + 1), 16);
            bytes[pos++] = (byte) (hi * 16 + lo);
        }

        return bytes;
    }

    /**
     * Convert a byte array into a printable format containing a
     * String of hex digit characters (two per byte).
     *
     * @param bytes array representation
     */
    public static String encode(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length << 1);
        int len = bytes.length;
        for (byte aByte : bytes)
        {
            sb.append(convertDigit(aByte >> 4));
            sb.append(convertDigit(aByte & 0x0f));
        }
        return sb.toString();
    }

    /**
     * Convert the specified value (0 .. 15) to the corresponding hex digit.
     *
     * @param value to be converted
     * @return '0'..'F' in char format.
     */
    private static char convertDigit(int value)
    {
        return _hex[(value & 0x0f)];
    }

    public static int count(String s, char c)
    {
        if (isEmpty(s))
        {
            return 0;
        }

        int count = 0;
        int len = s.length();
        for (int i=0; i < len; i++)
        {
            if (s.charAt(i) == c)
            {
                count++;
            }
        }

        return count;
    }
}
