package com.cedarsoftware.util;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Character.toLowerCase;

/**
 * Comprehensive utility class for string operations providing enhanced manipulation, comparison,
 * and conversion capabilities with null-safe implementations.
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>String Comparison:</b>
 *     <ul>
 *       <li>Case-sensitive and case-insensitive equality</li>
 *       <li>Comparison with automatic trimming</li>
 *       <li>Null-safe operations</li>
 *       <li>CharSequence support</li>
 *     </ul>
 *   </li>
 *   <li><b>Content Analysis:</b>
 *     <ul>
 *       <li>Empty and whitespace checking</li>
 *       <li>String length calculations</li>
 *       <li>Character/substring counting</li>
 *       <li>Pattern matching with wildcards</li>
 *     </ul>
 *   </li>
 *   <li><b>String Manipulation:</b>
 *     <ul>
 *       <li>Advanced trimming operations</li>
 *       <li>Quote handling</li>
 *       <li>Encoding conversions</li>
 *       <li>Random string generation</li>
 *     </ul>
 *   </li>
 *   <li><b>Distance Metrics:</b>
 *     <ul>
 *       <li>Levenshtein distance calculation</li>
 *       <li>Damerau-Levenshtein distance calculation</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>String Comparison:</h3>
 * <pre>{@code
 * // Case-sensitive and insensitive comparison
 * boolean equals = StringUtilities.equals("text", "text");           // true
 * boolean equals = StringUtilities.equalsIgnoreCase("Text", "text"); // true
 *
 * // Comparison with trimming
 * boolean equals = StringUtilities.equalsWithTrim(" text ", "text"); // true
 * }</pre>
 *
 * <h3>Content Checking:</h3>
 * <pre>{@code
 * // Empty and whitespace checking
 * boolean empty = StringUtilities.isEmpty("   ");      // true
 * boolean empty = StringUtilities.isEmpty(null);       // true
 * boolean hasContent = StringUtilities.hasContent(" text "); // true
 *
 * // Length calculations
 * int len = StringUtilities.length(null);             // 0
 * int len = StringUtilities.trimLength(" text ");     // 4
 * }</pre>
 *
 * <h3>String Manipulation:</h3>
 * <pre>{@code
 * // Trimming operations
 * String result = StringUtilities.trimToEmpty(null);    // ""
 * String result = StringUtilities.trimToNull("  ");     // null
 * String result = StringUtilities.trimEmptyToDefault("  ", "default");  // "default"
 *
 * // Quote handling
 * String result = StringUtilities.removeLeadingAndTrailingQuotes("\"text\"");  // text
 *
 * // Set conversion
 * Set<String> set = StringUtilities.commaSeparatedStringToSet("a,b,c");  // [a, b, c]
 * }</pre>
 *
 * <h3>Distance Calculations:</h3>
 * <pre>{@code
 * // Edit distance metrics
 * int distance = StringUtilities.levenshteinDistance("kitten", "sitting");        // 3
 * int distance = StringUtilities.damerauLevenshteinDistance("book", "back");      // 2
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All methods in this class are stateless and thread-safe.</p>
 *
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
public final class StringUtilities {
    private static final char[] _hex = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    public static String FOLDER_SEPARATOR = "/";

    public static String EMPTY = "";

    /**
     * <p>Constructor is declared private since all methods are static.</p>
     */
    private StringUtilities() {
    }

    /**
     * Compares two CharSequences, returning {@code true} if they represent
     * equal sequences of characters.
     *
     * <p>{@code null}s are handled without exceptions. Two {@code null}
     * references are considered to be equal. The comparison is <strong>case-sensitive</strong>.</p>
     *
     * @param cs1 the first CharSequence, may be {@code null}
     * @param cs2 the second CharSequence, may be {@code null}
     * @return {@code true} if the CharSequences are equal (case-sensitive), or both {@code null}
     * @see #equalsIgnoreCase(CharSequence, CharSequence)
     */
    public static boolean equals(CharSequence cs1, CharSequence cs2) {
        if (cs1 == cs2) {
            return true;
        }
        if (cs1 == null || cs2 == null) {
            return false;
        }
        if (cs1.length() != cs2.length()) {
            return false;
        }
        if (cs1 instanceof String && cs2 instanceof String) {
            return cs1.equals(cs2);
        }
        // Step-wise comparison
        int length = cs1.length();
        for (int i = 0; i < length; i++) {
            if (cs1.charAt(i) != cs2.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see StringUtilities#equals(CharSequence, CharSequence)
     */
    public static boolean equals(String s1, String s2) {
        return equals((CharSequence) s1, (CharSequence) s2);
    }

    /**
     * Compares two CharSequences, returning {@code true} if they represent
     * equal sequences of characters, ignoring case.
     *
     * <p>{@code null}s are handled without exceptions. Two {@code null}
     * references are considered equal. The comparison is <strong>case insensitive</strong>.</p>
     *
     * @param cs1 the first CharSequence, may be {@code null}
     * @param cs2 the second CharSequence, may be {@code null}
     * @return {@code true} if the CharSequences are equal (case-insensitive), or both {@code null}
     * @see #equals(CharSequence, CharSequence)
     */
    public static boolean equalsIgnoreCase(CharSequence cs1, CharSequence cs2) {
        if (cs1 == cs2) {
            return true;
        }
        if (cs1 == null || cs2 == null) {
            return false;
        }
        if (cs1.length() != cs2.length()) {
            return false;
        }
        return regionMatches(cs1, true, 0, cs2, 0, cs1.length());
    }

    /**
     * @see StringUtilities#equalsIgnoreCase(CharSequence, CharSequence)
     */
    public static boolean equalsIgnoreCase(String s1, String s2) {
        return equalsIgnoreCase((CharSequence) s1, (CharSequence) s2);
    }

    /**
     * Green implementation of regionMatches.
     *
     * @param cs         the {@link CharSequence} to be processed
     * @param ignoreCase whether to be case-insensitive
     * @param thisStart  the index to start on the {@code cs} CharSequence
     * @param substring  the {@link CharSequence} to be looked for
     * @param start      the index to start on the {@code substring} CharSequence
     * @param length     character length of the region
     * @return whether the region matched
     */
    static boolean regionMatches(CharSequence cs, boolean ignoreCase, int thisStart,
                                 CharSequence substring, int start, int length) {
        Convention.throwIfNull(cs, "cs to be processed cannot be null");
        Convention.throwIfNull(substring, "substring cannot be null");

        if (cs instanceof String && substring instanceof String) {
            return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
        }
        int index1 = thisStart;
        int index2 = start;
        int tmpLen = length;

        // Extract these first so we detect NPEs the same as the java.lang.String version
        int srcLen = cs.length() - thisStart;
        int otherLen = substring.length() - start;

        // Check for invalid parameters
        if (thisStart < 0 || start < 0 || length < 0) {
            return false;
        }

        // Check that the regions are long enough
        if (srcLen < length || otherLen < length) {
            return false;
        }

        while (tmpLen-- > 0) {
            char c1 = cs.charAt(index1++);
            char c2 = substring.charAt(index2++);

            if (c1 == c2) {
                continue;
            }

            if (!ignoreCase) {
                return false;
            }

            // The real same check as in String.regionMatches():
            char u1 = Character.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if (u1 != u2 && toLowerCase(u1) != toLowerCase(u2)) {
                return false;
            }
        }

        return true;
    }

    public static boolean equalsWithTrim(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return s1 == s2;
        }
        return s1.trim().equals(s2.trim());
    }

    public static boolean equalsIgnoreCaseWithTrim(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return s1 == s2;
        }
        return s1.trim().equalsIgnoreCase(s2.trim());
    }

    /**
     * Checks if a CharSequence is empty (""), null, or only whitespace.
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is empty or null
     */
    public static boolean isEmpty(CharSequence cs) {
        return isWhitespace(cs);
    }

    /**
     * @see StringUtilities#isEmpty(CharSequence)
     */
    public static boolean isEmpty(String s) {
        return isWhitespace(s);
    }

    /**
     * Checks if a CharSequence is empty (""), null or whitespace only.
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is null, empty or whitespace only
     */
    public static boolean isWhitespace(CharSequence cs) {
        int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a String is not empty (""), not null and not whitespace only.
     *
     * @param s the CharSequence to check, may be null
     * @return {@code true} if the CharSequence is
     * not empty and not null and not whitespace only
     */
    public static boolean hasContent(String s) {
        return !isWhitespace(s);
    }

    /**
     * Gets a CharSequence length or {@code 0} if the CharSequence is {@code null}.
     *
     * @param cs a CharSequence or {@code null}
     * @return CharSequence length or {@code 0} if the CharSequence is {@code null}.
     */
    public static int length(CharSequence cs) {
        return cs == null ? 0 : cs.length();
    }

    /**
     * @see StringUtilities#length(CharSequence)
     */
    public static int length(String s) {
        return s == null ? 0 : s.length();
    }

    /**
     * Returns the length of the trimmed string.  If the length is
     * null then it returns 0.
     *
     * @param s the string to get the trimmed length of
     * @return the length of the trimmed string, or 0 if the input is null
     */
    public static int trimLength(String s) {
        return trimToEmpty(s).length();
    }


    public static int lastIndexOf(String path, char ch) {
        if (path == null) {
            return -1;
        }
        return path.lastIndexOf(ch);
    }

    // Turn hex String into byte[]
    // If string is not even length, return null.

    public static byte[] decode(String s) {
        int len = s.length();
        if (len % 2 != 0) {
            return null;
        }

        byte[] bytes = new byte[len / 2];
        int pos = 0;

        for (int i = 0; i < len; i += 2) {
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
    public static String encode(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length << 1);
        for (byte aByte : bytes) {
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
    private static char convertDigit(int value) {
        return _hex[value & 0x0f];
    }

    public static int count(String s, char c) {
        return count(s, EMPTY + c);
    }

    /**
     * Count the number of times that 'token' occurs within 'content'.
     *
     * @return int count (0 if it never occurs, null is the source string, or null is the token).
     */
    public static int count(CharSequence content, CharSequence token) {
        if (content == null || token == null) {
            return 0;
        }

        String source = content.toString();
        if (source.isEmpty()) {
            return 0;
        }
        String sub = token.toString();
        if (sub.isEmpty()) {
            return 0;
        }

        int answer = 0;
        int idx = 0;

        while (true) {
            idx = source.indexOf(sub, idx);
            if (idx < answer) {
                return answer;
            }
            ++answer;
            ++idx;
        }
    }

    /**
     * Convert strings containing DOS-style '*' or '?' to a regex String.
     */
    public static String wildcardToRegexString(String wildcard) {
        int len = wildcard.length();
        StringBuilder s = new StringBuilder(len);
        s.append('^');
        for (int i = 0; i < len; i++) {
            char c = wildcard.charAt(i);
            switch (c) {
                case '*':
                    s.append(".*");
                    break;

                case '?':
                    s.append('.');
                    break;

                // escape special regexp-characters
                case '(':
                case ')':
                case '[':
                case ']':
                case '$':
                case '^':
                case '.':
                case '{':
                case '}':
                case '|':
                case '\\':
                    s.append('\\');
                    s.append(c);
                    break;

                default:
                    s.append(c);
                    break;
            }
        }
        s.append('$');
        return s.toString();
    }

    /**
     * The Levenshtein distance is a string metric for measuring the difference between two sequences.
     * Informally, the Levenshtein distance between two words is the minimum number of single-character edits
     * (i.e. insertions, deletions or substitutions) required to change one word into the other. The phrase
     * 'edit distance' is often used to refer specifically to Levenshtein distance.
     *
     * @param s String one
     * @param t String two
     * @return the 'edit distance' (Levenshtein distance) between the two strings.
     */
    public static int levenshteinDistance(CharSequence s, CharSequence t) {
        // degenerate cases
        if (s == null || EMPTY.contentEquals(s)) {
            return t == null || EMPTY.contentEquals(t) ? 0 : t.length();
        } else if (t == null || EMPTY.contentEquals(t)) {
            return s.length();
        }

        // create two work vectors of integer distances
        int[] v0 = new int[t.length() + 1];
        int[] v1 = new int[t.length() + 1];

        // initialize v0 (the previous row of distances)
        // this row is A[0][i]: edit distance for an empty s
        // the distance is just the number of characters to delete from t
        for (int i = 0; i < v0.length; i++) {
            v0[i] = i;
        }

        int sLen = s.length();
        int tLen = t.length();
        for (int i = 0; i < sLen; i++) {
            // calculate v1 (current row distances) from the previous row v0

            // first element of v1 is A[i+1][0]
            //   edit distance is delete (i+1) chars from s to match empty t
            v1[0] = i + 1;

            // use formula to fill in the rest of the row
            for (int j = 0; j < tLen; j++) {
                int cost = (s.charAt(i) == t.charAt(j)) ? 0 : 1;
                v1[j + 1] = (int) MathUtilities.minimum(v1[j] + 1, v0[j + 1] + 1, v0[j] + cost);
            }

            // copy v1 (current row) to v0 (previous row) for next iteration
            System.arraycopy(v1, 0, v0, 0, v0.length);
        }

        return v1[t.length()];
    }

    /**
     * Calculate the Damerau-Levenshtein Distance between two strings.  The basic difference
     * between this algorithm and the general Levenshtein algorithm is that damerau-Levenshtein
     * counts a swap of two characters next to each other as 1 instead of 2.  This breaks the
     * 'triangular equality', which makes it unusable for Metric trees.  See Wikipedia pages on
     * both Levenshtein and Damerau-Levenshtein and then make your decision as to which algorithm
     * is appropriate for your situation.
     *
     * @param source Source input string
     * @param target Target input string
     * @return The number of substitutions it would take
     * to make the source string identical to the target
     * string
     */
    public static int damerauLevenshteinDistance(CharSequence source, CharSequence target) {
        if (source == null || EMPTY.contentEquals(source)) {
            return target == null || EMPTY.contentEquals(target) ? 0 : target.length();
        } else if (target == null || EMPTY.contentEquals(target)) {
            return source.length();
        }

        int srcLen = source.length();
        int targetLen = target.length();
        int[][] distanceMatrix = new int[srcLen + 1][targetLen + 1];

        // We need indexers from 0 to the length of the source string.
        // This sequential set of numbers will be the row "headers"
        // in the matrix.
        for (int srcIndex = 0; srcIndex <= srcLen; srcIndex++) {
            distanceMatrix[srcIndex][0] = srcIndex;
        }

        // We need indexers from 0 to the length of the target string.
        // This sequential set of numbers will be the
        // column "headers" in the matrix.
        for (int targetIndex = 0; targetIndex <= targetLen; targetIndex++) {
            // Set the value of the first cell in the column
            // equivalent to the current value of the iterator
            distanceMatrix[0][targetIndex] = targetIndex;
        }

        for (int srcIndex = 1; srcIndex <= srcLen; srcIndex++) {
            for (int targetIndex = 1; targetIndex <= targetLen; targetIndex++) {
                // If the current characters in both strings are equal
                int cost = source.charAt(srcIndex - 1) == target.charAt(targetIndex - 1) ? 0 : 1;

                // Find the current distance by determining the shortest path to a
                // match (hence the 'minimum' calculation on distances).
                distanceMatrix[srcIndex][targetIndex] = (int) MathUtilities.minimum(
                        // Character match between current character in
                        // source string and next character in target
                        distanceMatrix[srcIndex - 1][targetIndex] + 1,
                        // Character match between next character in
                        // source string and current character in target
                        distanceMatrix[srcIndex][targetIndex - 1] + 1,
                        // No match, at current, add cumulative penalty
                        distanceMatrix[srcIndex - 1][targetIndex - 1] + cost);

                // We don't want to do the next series of calculations on
                // the first pass because we would get an index out of bounds
                // exception.
                if (srcIndex == 1 || targetIndex == 1) {
                    continue;
                }

                // transposition check (if the current and previous
                // character are switched around (e.g.: t[se]t and t[es]t)...
                if (source.charAt(srcIndex - 1) == target.charAt(targetIndex - 2) && source.charAt(srcIndex - 2) == target.charAt(targetIndex - 1)) {
                    // What's the minimum cost between the current distance
                    // and a transposition.
                    distanceMatrix[srcIndex][targetIndex] = (int) MathUtilities.minimum(
                            // Current cost
                            distanceMatrix[srcIndex][targetIndex],
                            // Transposition
                            distanceMatrix[srcIndex - 2][targetIndex - 2] + cost);
                }
            }
        }

        return distanceMatrix[srcLen][targetLen];
    }

    /**
     * @param random Random instance
     * @param minLen minimum number of characters
     * @param maxLen maximum number of characters
     * @return String of alphabetical characters, with the first character uppercase (Proper case strings).
     */
    public static String getRandomString(Random random, int minLen, int maxLen) {
        StringBuilder s = new StringBuilder();
        int len = minLen + random.nextInt(maxLen - minLen + 1);

        for (int i = 0; i < len; i++) {
            s.append(getRandomChar(random, i == 0));
        }
        return s.toString();
    }

    public static String getRandomChar(Random random, boolean upper) {
        int r = random.nextInt(26);
        return upper ? EMPTY + (char) ('A' + r) : EMPTY + (char) ('a' + r);
    }

    /**
     * Convert a String into a byte[] with a particular encoding.
     * Preferable used when the encoding is one of the guaranteed Java types
     * and you don't want to have to catch the UnsupportedEncodingException
     * required by Java
     *
     * @param s        string to encode into bytes
     * @param encoding encoding to use
     */
    public static byte[] getBytes(String s, String encoding) {
        try {
            return s == null ? null : s.getBytes(encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(String.format("Encoding (%s) is not supported by your JVM", encoding), e);
        }
    }


    //  TODO: The following two methods are exactly the same other than the case of the method.
    //  TODO: deprecate one and remove next major version.
    /**
     * Convert a byte[] into a UTF-8 String.  Preferable used when the encoding
     * is one of the guaranteed Java types and you don't want to have to catch
     * the UnsupportedEncodingException required by Java
     *
     * @param bytes bytes to encode into a string
     */
    public static String createUtf8String(byte[] bytes) {
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Convert a byte[] into a UTF-8 encoded String.
     *
     * @param bytes bytes to encode into a string
     */
    public static String createUTF8String(byte[] bytes) {
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Convert a String into a byte[] encoded by UTF-8.
     *
     * @param s string to encode into bytes
     */
    public static byte[] getUTF8Bytes(String s) {
        return s == null ? null : s.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Convert a byte[] into a String with a particular encoding.
     * Preferable used when the encoding is one of the guaranteed Java types
     * and you don't want to have to catch the UnsupportedEncodingException
     * required by Java
     *
     * @param bytes    bytes to encode into a string
     * @param encoding encoding to use
     */
    public static String createString(byte[] bytes, String encoding) {
        try {
            return bytes == null ? null : new String(bytes, encoding);
        }
        catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(String.format("Encoding (%s) is not supported by your JVM", encoding), e);
        }
    }

    /**
     * Get the hashCode of a String, insensitive to case, without any new Strings
     * being created on the heap.
     *
     * @param s String input
     * @return int hashCode of input String insensitive to case
     */
    public static int hashCodeIgnoreCase(String s) {
        if (s == null) {
            return 0;
        }
        final int len = s.length();
        int hash = 0;
        for (int i = 0; i < len; i++) {
            hash = 31 * hash + toLowerCase(s.charAt(i));
        }
        return hash;
    }

    /**
     * Removes control characters (char &lt;= 32) from both
     * ends of this String, handling {@code null} by returning
     * {@code null}.
     *
     * <p>The String is trimmed using {@link String#trim()}.
     * Trim removes start and end characters &lt;= 32.
     *
     * @param str the String to be trimmed, may be null
     * @return the trimmed string, {@code null} if null String input
     */
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    /**
     * Trims a string, its null safe and null will return empty string here..
     *
     * @param value string input
     * @return String trimmed string, if value was null this will be empty
     */
    public static String trimToEmpty(String value) {
        return value == null ? EMPTY : value.trim();
    }

    /**
     * Trims a string, If the string trims to empty then we return null.
     *
     * @param value string input
     * @return String, trimmed from value.  If the value was empty we return null.
     */
    public static String trimToNull(String value) {
        String ts = trim(value);
        return isEmpty(ts) ? null : ts;
    }

    /**
     * Trims a string, If the string trims to empty then we return the default.
     *
     * @param value        string input
     * @param defaultValue value to return on empty or null
     * @return trimmed string, or defaultValue when null or empty
     */
    public static String trimEmptyToDefault(String value, String defaultValue) {
        return Optional.ofNullable(value).map(StringUtilities::trimToNull).orElse(defaultValue);
    }

    /**
     * Removes all leading and trailing double quotes from a String. Multiple consecutive quotes
     * at the beginning or end of the string will all be removed.
     * <p>
     * Examples:
     * <ul>
     *     <li>"text" → text</li>
     *     <li>""text"" → text</li>
     *     <li>"""text""" → text</li>
     *     <li>"text with "quotes" inside" → text with "quotes" inside</li>
     * </ul>
     *
     * @param input the String from which to remove quotes (may be null)
     * @return the String with all leading and trailing quotes removed, or null if input was null
     */
    public static String removeLeadingAndTrailingQuotes(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        int start = 0;
        int end = input.length();

        while (start < end && input.charAt(start) == '"') {
            start++;
        }
        while (end > start && input.charAt(end - 1) == '"') {
            end--;
        }

        return input.substring(start, end);
    }

    /**
     * Converts a comma-separated string into a {@link Set} of trimmed, non-empty strings.
     *
     * <p>
     * This method splits the provided string by commas, trims whitespace from each resulting substring,
     * filters out any empty strings, and collects the unique strings into a {@link Set}. If the input string
     * is {@code null} or empty after trimming, the method returns an empty set.
     * </p>
     *
     * <p>
     * <b>Usage Example:</b>
     * </p>
     * <pre>{@code
     * String csv = "apple, banana, cherry, apple,  ";
     * Set<String> fruitSet = commaSeparatedStringToSet(csv);
     * // fruitSet contains ["apple", "banana", "cherry"]
     * }</pre>
     *
     * <p>
     * <b>Note:</b> The resulting {@code Set} does not maintain the insertion order. If order preservation is required,
     * consider using a {@link LinkedHashSet}.
     * </p>
     *
     * @param commaSeparatedString the comma-separated string to convert
     * @return a {@link Set} containing the trimmed, unique, non-empty substrings from the input string.
     *         Returns an empty set if the input is {@code null}, empty, or contains only whitespace.
     *
     * @throws IllegalArgumentException if the method is modified to disallow {@code null} inputs in the future
     *
     * @see String#split(String)
     * @see Collectors#toSet()
     */
    public static Set<String> commaSeparatedStringToSet(String commaSeparatedString) {
        if (commaSeparatedString == null || commaSeparatedString.trim().isEmpty()) {
            return Collections.emptySet();
        }
        return Arrays.stream(commaSeparatedString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }
}
