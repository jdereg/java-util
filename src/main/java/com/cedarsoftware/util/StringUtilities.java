package com.cedarsoftware.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cedarsoftware.util.ByteUtilities.HEX_ARRAY;

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
 * <h2>Security Configuration</h2>
 * <p>StringUtilities provides configurable security controls to prevent various attack vectors.
 * All security features are <strong>disabled by default</strong> for backward compatibility.</p>
 *
 * <p>Security controls can be enabled via system properties:</p>
 * <ul>
 *   <li><code>stringutilities.security.enabled=false</code> &mdash; Master switch for all security features</li>
 *   <li><code>stringutilities.max.hex.decode.size=0</code> &mdash; Max hex string size for decode() (0=disabled)</li>
 *   <li><code>stringutilities.max.wildcard.length=0</code> &mdash; Max wildcard pattern length (0=disabled)</li>
 *   <li><code>stringutilities.max.wildcard.count=0</code> &mdash; Max wildcard characters in pattern (0=disabled)</li>
 *   <li><code>stringutilities.max.levenshtein.string.length=0</code> &mdash; Max string length for Levenshtein distance (0=disabled)</li>
 *   <li><code>stringutilities.max.damerau.levenshtein.string.length=0</code> &mdash; Max string length for Damerau-Levenshtein distance (0=disabled)</li>
 *   <li><code>stringutilities.max.repeat.count=0</code> &mdash; Max repeat count for repeat() method (0=disabled)</li>
 *   <li><code>stringutilities.max.repeat.total.size=0</code> &mdash; Max total size for repeat() result (0=disabled)</li>
 * </ul>
 *
 * <h3>Security Features</h3>
 * <ul>
 *   <li><b>Memory Exhaustion Protection:</b> Limits string sizes to prevent out-of-memory attacks</li>
 *   <li><b>ReDoS Prevention:</b> Limits wildcard pattern complexity to prevent regular expression denial of service</li>
 *   <li><b>Integer Overflow Protection:</b> Prevents arithmetic overflow in size calculations</li>
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
    public static final String FOLDER_SEPARATOR = File.separator;

    public static final String EMPTY = "";
    
    // Security configuration - all disabled by default for backward compatibility
    // These are checked dynamically to allow runtime configuration changes for testing
    private static boolean isSecurityEnabled() {
        return Boolean.parseBoolean(System.getProperty("stringutilities.security.enabled", "false"));
    }
    
    private static int getMaxHexDecodeSize() {
        try {
            return Converter.convert(System.getProperty("stringutilities.max.hex.decode.size", "0"), int.class);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getMaxWildcardLength() {
        try {
            return Converter.convert(System.getProperty("stringutilities.max.wildcard.length", "0"), int.class);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getMaxWildcardCount() {
        try {
            return Converter.convert(System.getProperty("stringutilities.max.wildcard.count", "0"), int.class);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getMaxLevenshteinStringLength() {
        try {
            return Converter.convert(System.getProperty("stringutilities.max.levenshtein.string.length", "0"), int.class);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getMaxDamerauLevenshteinStringLength() {
        try {
            return Converter.convert(System.getProperty("stringutilities.max.damerau.levenshtein.string.length", "0"), int.class);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getMaxRepeatCount() {
        try {
            return Converter.convert(System.getProperty("stringutilities.max.repeat.count", "0"), int.class);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int getMaxRepeatTotalSize() {
        try {
            return Converter.convert(System.getProperty("stringutilities.max.repeat.total.size", "0"), int.class);
        } catch (Exception e) {
            return 0;
        }
    }

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
        return equals(s1, (CharSequence) s2);
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
        if (cs1 == cs2) return true;
        if (cs1 == null || cs2 == null) return false;
        final int n = cs1.length();
        if (n != cs2.length()) return false;

        // Let HotSpot's heavily optimized code handle String/String
        if (cs1 instanceof String && cs2 instanceof String) {
            return ((String) cs1).equalsIgnoreCase((String) cs2);
        }
        return regionEqualsIgnoreCase(cs1, 0, cs2, 0, n);
    }

    /**
     * @see StringUtilities#equalsIgnoreCase(CharSequence, CharSequence)
     */
    public static boolean equalsIgnoreCase(String s1, String s2) {
        return equalsIgnoreCase(s1, (CharSequence) s2);
    }

    /**
     * Checks if the first string contains the second string, ignoring case considerations.
     * <p>
     * This method uses {@link String#regionMatches(boolean, int, String, int, int)} for optimal performance,
     * avoiding the creation of temporary lowercase strings that would be required with 
     * {@code s1.toLowerCase().contains(s2.toLowerCase())}.
     * </p>
     *
     * @param s1 the string to search within, may be {@code null}
     * @param s2 the substring to search for, may be {@code null}
     * @return {@code true} if s1 contains s2 (case-insensitive), {@code false} otherwise.
     *         Returns {@code false} if either parameter is {@code null}.
     */
    public static boolean containsIgnoreCase(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return false;
        }
        if (s2.isEmpty()) {
            return true;
        }
        if (s1.length() < s2.length()) {
            return false;
        }
        
        int searchLen = s2.length();
        int maxIndex = s1.length() - searchLen;
        
        for (int i = 0; i <= maxIndex; i++) {
            if (s1.regionMatches(true, i, s2, 0, searchLen)) {
                return true;
            }
        }
        return false;
    }

    /** Fast, allocation-free case-insensitive region compare. */
    static boolean regionMatches(CharSequence cs, boolean ignoreCase, int thisStart,
                                 CharSequence substring, int start, int length) {
        Convention.throwIfNull(cs, "cs to be processed cannot be null");
        Convention.throwIfNull(substring, "substring cannot be null");

        // Delegate to JDK for String/String — it knows about compact strings, etc.
        if (cs instanceof String && substring instanceof String) {
            return ((String) cs).regionMatches(ignoreCase, thisStart, (String) substring, start, length);
        }

        // Bounds and trivial cases first
        if (thisStart < 0 || start < 0 || length < 0) return false;
        if (cs.length() - thisStart < length || substring.length() - start < length) return false;
        if (length == 0) return true;

        if (!ignoreCase) {
            for (int i = 0; i < length; i++) {
                if (cs.charAt(thisStart + i) != substring.charAt(start + i)) return false;
            }
            return true;
        }
        return regionEqualsIgnoreCase(cs, thisStart, substring, start, length);
    }

    /** ASCII-first path with Unicode fallback; matches String.regionMatches(ignoreCase=true). */
    private static boolean regionEqualsIgnoreCase(CharSequence a, int aOff,
                                                  CharSequence b, int bOff,
                                                  int len) {
        int i = 0;
        // Fast path: ASCII only (no Character.* calls)
        while (i < len) {
            char c1 = a.charAt(aOff + i);
            char c2 = b.charAt(bOff + i);
            if (c1 == c2) { i++; continue; }

            // If either is non-ASCII, fall back once
            if ( (c1 | c2) >= 128 ) {
                return regionEqualsIgnoreCaseSlow(a, aOff + i, b, bOff + i, len - i);
            }

            // Fold ASCII A..Z → a..z via arithmetic (fast and branch-friendly)
            if ((c1 - 'A') <= 25) c1 += 32;
            if ((c2 - 'A') <= 25) c2 += 32;
            if (c1 != c2) return false;
            i++;
        }
        return true;
    }

    /** Slow path: exact String.regionMatches(ignoreCase=true) semantics (char-based). */
    private static boolean regionEqualsIgnoreCaseSlow(CharSequence a, int aOff,
                                                      CharSequence b, int bOff,
                                                      int len) {
        for (int i = 0; i < len; i++) {
            char c1 = a.charAt(aOff + i);
            char c2 = b.charAt(bOff + i);
            if (c1 == c2) continue;

            char u1 = Character.toUpperCase(c1);
            char u2 = Character.toUpperCase(c2);
            if (u1 != u2 && Character.toLowerCase(u1) != Character.toLowerCase(u2)) {
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

    /**
     * Convert a hexadecimal {@link String} into a byte array.
     *
     * <p>If the input length is odd or contains non-hex characters the method
     * returns {@code null}.</p>
     *
     * @param s the hexadecimal string to decode, may not be {@code null}
     * @return the decoded bytes or {@code null} if the input is malformed
     */
    public static byte[] decode(String s) {
        if (s == null) {
            return null;
        }
        
        // Security: Limit input size to prevent memory exhaustion (configurable)
        if (isSecurityEnabled()) {
            int maxSize = getMaxHexDecodeSize();
            if (maxSize > 0 && s.length() > maxSize) {
                throw new IllegalArgumentException("Input string too long for hex decoding (max " + maxSize + "): " + s.length());
            }
        }
        
        int len = s.length();
        if (len % 2 != 0) {
            return null;
        }

        byte[] bytes = new byte[len / 2];
        int pos = 0;

        for (int i = 0; i < len; i += 2) {
            int hi = Character.digit(s.charAt(i), 16);
            int lo = Character.digit(s.charAt(i + 1), 16);
            if (hi == -1 || lo == -1) {
                return null;
            }
            bytes[pos++] = (byte) ((hi << 4) + lo);
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
        return HEX_ARRAY[value & 0x0f];
    }

    public static int count(String s, char c) {
        if (s == null) {
            return 0;
        }

        int answer = 0;
        int len = s.length();
        for (int i = 0; i < len; i++) {
            if (s.charAt(i) == c) {
                answer++;
            }
        }
        return answer;
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

        int contentLen = content.length();
        int tokenLen = token.length();

        if (contentLen == 0 || tokenLen == 0) {
            return 0;
        }

        int answer = 0;
        int idx = 0;

        // Use CharSequence comparison instead of converting to String
        while (idx <= contentLen - tokenLen) {
            boolean match = true;
            for (int i = 0; i < tokenLen; i++) {
                if (content.charAt(idx + i) != token.charAt(i)) {
                    match = false;
                    break;
                }
            }
            if (match) {
                answer++;
                idx += tokenLen;
            } else {
                idx++;
            }
        }

        return answer;
    }

    /**
     * Convert strings containing DOS-style '*' or '?' to a regex String.
     */
    public static String wildcardToRegexString(String wildcard) {
        if (wildcard == null) {
            throw new IllegalArgumentException("Wildcard pattern cannot be null");
        }
        
        // Security: Prevent ReDoS attacks by limiting pattern length and complexity (configurable)
        if (isSecurityEnabled()) {
            int maxLength = getMaxWildcardLength();
            if (maxLength > 0 && wildcard.length() > maxLength) {
                throw new IllegalArgumentException("Wildcard pattern too long (max " + maxLength + " characters): " + wildcard.length());
            }
            
            // Security: Count wildcards to prevent patterns with excessive complexity (configurable)
            int maxCount = getMaxWildcardCount();
            if (maxCount > 0) {
                int wildcardCount = 0;
                for (int i = 0; i < wildcard.length(); i++) {
                    if (wildcard.charAt(i) == '*' || wildcard.charAt(i) == '?') {
                        wildcardCount++;
                        if (wildcardCount > maxCount) {
                            throw new IllegalArgumentException("Too many wildcards in pattern (max " + maxCount + "): " + wildcardCount);
                        }
                    }
                }
            }
        }
        
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
        // Security: Prevent memory exhaustion attacks with very long strings (configurable)
        if (isSecurityEnabled()) {
            int maxLength = getMaxLevenshteinStringLength();
            if (maxLength > 0) {
                if (s != null && s.length() > maxLength) {
                    throw new IllegalArgumentException("First string too long for distance calculation (max " + maxLength + "): " + s.length());
                }
                if (t != null && t.length() > maxLength) {
                    throw new IllegalArgumentException("Second string too long for distance calculation (max " + maxLength + "): " + t.length());
                }
            }
        }
        
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
        // Security: Prevent memory exhaustion attacks with very long strings (configurable)
        if (isSecurityEnabled()) {
            int maxLength = getMaxDamerauLevenshteinStringLength();
            if (maxLength > 0) {
                if (source != null && source.length() > maxLength) {
                    throw new IllegalArgumentException("Source string too long for Damerau-Levenshtein calculation (max " + maxLength + "): " + source.length());
                }
                if (target != null && target.length() > maxLength) {
                    throw new IllegalArgumentException("Target string too long for Damerau-Levenshtein calculation (max " + maxLength + "): " + target.length());
                }
            }
        }
        
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
     * Generate a random proper‑case string.
     *
     * @param random Random instance, must not be {@code null}
     * @param minLen minimum number of characters (inclusive)
     * @param maxLen maximum number of characters (inclusive)
     * @return alphabetic string with the first character uppercase
     * @throws NullPointerException     if {@code random} is {@code null}
     * @throws IllegalArgumentException if length parameters are invalid
     */
    public static String getRandomString(Random random, int minLen, int maxLen) {
        if (random == null) {
            throw new NullPointerException("random cannot be null");
        }
        if (minLen < 0 || maxLen < minLen) {
            throw new IllegalArgumentException("minLen must be >= 0 and <= maxLen");
        }

        StringBuilder s = new StringBuilder();
        int len = minLen + random.nextInt(maxLen - minLen + 1);

        for (int i = 0; i < len; i++) {
            s.append(getRandomChar(random, i == 0));
        }
        return s.toString();
    }

    public static char getRandomChar(Random random, boolean upper) {
        int r = random.nextInt(26);
        return upper ? (char) ('A' + r) : (char) ('a' + r);
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
     * Computes a case-insensitive hash code for a CharSequence.
     * This method produces the same hash as cs.toString().toLowerCase().hashCode()
     * for compatibility with existing code.
     *
     * @param cs the CharSequence to hash (can be String, StringBuilder, etc.)
     * @return the case-insensitive hash code, or 0 if cs is null
     */
    public static int hashCodeIgnoreCase(CharSequence cs) {
        if (cs == null) return 0;
        
        // For String, delegate to the optimized String-specific version
        if (cs instanceof String) {
            return hashCodeIgnoreCase((String) cs);
        }

        // Check if CharSequence is pure ASCII for fast path
        boolean isPureAscii = true;
        final int n = cs.length();
        for (int i = 0; i < n; i++) {
            if (cs.charAt(i) >= 128) {
                isPureAscii = false;
                break;
            }
        }
        
        if (isPureAscii) {
            // Fast path for pure ASCII: compute hash directly without allocation
            int h = 0;
            for (int i = 0; i < n; i++) {
                char c = cs.charAt(i);
                // Convert A-Z to a-z
                if (c >= 'A' && c <= 'Z') {
                    c = (char) (c + 32);
                }
                h = 31 * h + c;
            }
            return h;
        } else {
            // For non-ASCII, we must use toLowerCase() to maintain compatibility
            return cs.toString().toLowerCase().hashCode();
        }
    }

    /**
     * Get the hashCode of a String, insensitive to case, without any new Strings
     * being created on the heap.
     * <p>
     * This implementation uses a fast ASCII shift approach for compatible locales,
     * and falls back to the more correct but slower Locale-aware approach for locales
     * where simple ASCII case conversion does not work properly.
     */
    public static int hashCodeIgnoreCase(String s) {
        if (s == null) return 0;

        // To maintain compatibility with existing code that relies on specific hash collisions,
        // we need to produce the same hash as s.toLowerCase().hashCode()
        // The optimized version below computes hash differently and breaks some tests
        
        // Check if string is pure ASCII for fast path
        boolean isPureAscii = true;
        final int n = s.length();
        for (int i = 0; i < n; i++) {
            if (s.charAt(i) >= 128) {
                isPureAscii = false;
                break;
            }
        }
        
        if (isPureAscii) {
            // Fast path for pure ASCII: compute hash directly without allocation
            int h = 0;
            for (int i = 0; i < n; i++) {
                char c = s.charAt(i);
                // Convert A-Z to a-z
                if (c >= 'A' && c <= 'Z') {
                    c = (char) (c + 32);
                }
                h = 31 * h + c;
            }
            return h;
        } else {
            // For non-ASCII, we must use toLowerCase() to maintain compatibility
            // This ensures we get the exact same hash codes as before
            return s.toLowerCase().hashCode();
        }
    }

    /**
     * Add when we support Java 18+
     */
//    static {
//        // This ensures our optimization remains valid even if the default locale changes
//        Locale.addLocaleChangeListener(locale -> {
//            isAsciiCompatibleLocale = checkAsciiCompatibleLocale();
//        });
//    }

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
     * @return a mutable {@link Set} containing the trimmed, unique, non-empty substrings from the input string.
     *         Returns an empty {@link LinkedHashSet} if the input is {@code null}, empty, or contains only whitespace.
     *
     * @throws IllegalArgumentException if the method is modified to disallow {@code null} inputs in the future
     *
     * @see String#split(String)
     * @see Collectors#toSet()
     */
    public static Set<String> commaSeparatedStringToSet(String commaSeparatedString) {
        if (commaSeparatedString == null || commaSeparatedString.trim().isEmpty()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(commaSeparatedString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * Convert a {@code snake_case} string to {@code camelCase}.
     *
     * @param snake the snake case string, may be {@code null}
     * @return the camelCase representation or {@code null} if {@code snake} is {@code null}
     */
    public static String snakeToCamel(String snake) {
        if (snake == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        boolean upper = false;
        for (char c : snake.toCharArray()) {
            if (c == '_') {
                upper = true;
                continue;
            }
            result.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return result.toString();
    }

    /**
     * Convert a {@code camelCase} or {@code PascalCase} string to {@code snake_case}.
     *
     * @param camel the camel case string, may be {@code null}
     * @return the snake_case representation or {@code null} if {@code camel} is {@code null}
     */
    public static String camelToSnake(String camel) {
        if (camel == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                result.append('_');
            }
            result.append(Character.toLowerCase(c));
        }
        return result.toString();
    }

    /**
     * Determine if the supplied string contains only numeric digits.
     *
     * @param s the string to test, may be {@code null}
     * @return {@code true} if {@code s} is non-empty and consists solely of digits
     */
    public static boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Repeat a string {@code count} times.
     *
     * @param s     the string to repeat, may be {@code null}
     * @param count the number of times to repeat, must be non-negative
     * @return the repeated string or {@code null} if {@code s} is {@code null}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public static String repeat(String s, int count) {
        if (s == null) {
            return null;
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be >= 0");
        }
        if (count == 0) {
            return EMPTY;
        }
        
        // Security: Prevent memory exhaustion and integer overflow attacks (configurable)
        if (isSecurityEnabled()) {
            int maxCount = getMaxRepeatCount();
            if (maxCount > 0 && count > maxCount) {
                throw new IllegalArgumentException("count too large (max " + maxCount + "): " + count);
            }
            
            // Security: Check for integer overflow in total length calculation
            long totalLength = (long) s.length() * count;
            if (totalLength > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Result would be too large: " + totalLength + " characters");
            }
            
            // Security: Limit total memory allocation to reasonable size
            int maxTotalSize = getMaxRepeatTotalSize();
            if (maxTotalSize > 0 && totalLength > maxTotalSize) {
                throw new IllegalArgumentException("Result too large (max " + maxTotalSize + "): " + totalLength + " characters");
            }
        }
        
        StringBuilder result = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            result.append(s);
        }
        return result.toString();
    }

    /**
     * Reverse the characters of a string.
     *
     * @param s the string to reverse, may be {@code null}
     * @return the reversed string or {@code null} if {@code s} is {@code null}
     */
    public static String reverse(String s) {
        return s == null ? null : new StringBuilder(s).reverse().toString();
    }

    /**
     * Pad the supplied string on the left with spaces until it reaches the specified length.
     * If the string is already longer than {@code length}, the original string is returned.
     *
     * @param s      the string to pad, may be {@code null}
     * @param length desired final length
     * @return the padded string or {@code null} if {@code s} is {@code null}
     */
    public static String padLeft(String s, int length) {
        if (s == null) {
            return null;
        }
        if (length <= s.length()) {
            return s;
        }
        StringBuilder result = new StringBuilder(length);
        for (int i = s.length(); i < length; i++) {
            result.append(' ');
        }
        return result.append(s).toString();
    }

    /**
     * Pad the supplied string on the right with spaces until it reaches the specified length.
     * If the string is already longer than {@code length}, the original string is returned.
     *
     * @param s      the string to pad, may be {@code null}
     * @param length desired final length
     * @return the padded string or {@code null} if {@code s} is {@code null}
     */
    public static String padRight(String s, int length) {
        if (s == null) {
            return null;
        }
        if (length <= s.length()) {
            return s;
        }
        StringBuilder result = new StringBuilder(length);
        result.append(s);
        for (int i = s.length(); i < length; i++) {
            result.append(' ');
        }
        return result.toString();
    }
}
