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
package com.cedarsoftware.util;

import com.cedarsoftware.lang.ByteUtilities;
import com.cedarsoftware.lang.CharSequenceUtilities;

/**
 * Useful String utilities for common tasks.  This class is now deprecated between the
 * following three classes:
 *
 * @see com.cedarsoftware.lang.CharSequenceUtilities
 * @see com.cedarsoftware.lang.StringUtilities;
 * @see com.cedarsoftware.lang.ByteUtilities ;
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * @author Ken Partlow (kpartlow@gmail.com)
 *
 */
@Deprecated
public final class StringUtilities
{
    public static final String FOLDER_SEPARATOR = "/";

    /**
     * <p>Constructor is declared private since all methods are static.</p>
     */
    private StringUtilities()
    {
        super();
    }

    // Equals
    // -----------------------------------------------------------------------
    /**
     * @deprecated use CharSequenceUtilities.equals()
     */
    public static boolean equals(final String str1, final String str2)
    {
        return com.cedarsoftware.lang.StringUtilities.equals(str1, str2);
    }

    /**
     * @deprecated use com.cedarsoftware.lang.StringUtilities.equalsIgnoreCase()
     */
    public static boolean equalsIgnoreCase(final String s1, final String s2)
    {
        return com.cedarsoftware.lang.StringUtilities.equalsIgnoreCase(s1, s2);
    }

    /**
     * @deprecated use CharSequenceUtilities.equalsAfterTrim()
     */
    public static boolean equalsWithTrim(final CharSequence s1, final CharSequence s2)
    {
        return CharSequenceUtilities.equalsAfterTrim(s1, s2);
    }

    /**
     * @deprecated use CharSequenceUtilities.equalsIgnoreCaseAfterTrim()
     */
    public static boolean equalsIgnoreCaseWithTrim(final CharSequence s1, final CharSequence s2)
    {
        return CharSequenceUtilities.equalsIgnoreCaseAfterTrim(s1, s2);
    }

    /**
     * @deprecated use CharSequenceUtilities.isBlank()
     */
    public static boolean isEmpty(final CharSequence s)
    {
        return CharSequenceUtilities.isBlank(s);
    }

    /**
     * @deprecated use com.cedarsoftware.lang.CharSequenceUtilities.hasContent()
     */
    public static boolean hasContent(final CharSequence s)
    {
        return CharSequenceUtilities.hasContent(s);
    }

    /**
     * @deprecated use com.cedarsoftware.lang.CharSequenceUtilities.length()
     */
    public static int length(final CharSequence s)
    {
        return CharSequenceUtilities.length(s);
    }

    /**
     * @deprecated use com.cedarsoftware.lang.StringUtilities.lengthAfterTrim()
     */
    public static int trimLength(final String s)
    {
        return CharSequenceUtilities.lengthAfterTrim(s);
    }

    /**
    * @deprecated use com.cedarsoftware.lang.StringUtilities.trim()
    */
    public static String trim(final String s)
    {
        return com.cedarsoftware.lang.StringUtilities.trim(s);
    }

    /**
     * @deprecated use com.cedarsoftware.lang.StringUtilities.lastIndexOf()
     */
    public static int lastIndexOf(String path, char ch)
    {
        return com.cedarsoftware.lang.StringUtilities.lastIndexOf(path, ch);
    }

    /**
     * @deprecated use ByteUtilities.decode()
     */
    public static byte[] decode(String s)
    {
        return ByteUtilities.decode(s);
    }

    /**
     * @deprecated use ByteUtilities.encode()
     */
    public static String encode(byte[] bytes)
    {
        return ByteUtilities.encode(bytes);
    }

    /**
     * @deprecated use CharSequenceUtilities.count()
     */
    public static int count(String s, char c)
    {
        return CharSequenceUtilities.count(s, c);
    }
}
