package com.cedarsoftware.util;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class providing static methods for operations on byte arrays and hexadecimal representations.
 * <p>
 * {@code ByteUtilities} simplifies common tasks such as encoding byte arrays to hexadecimal strings,
 * reading byte arrays back from text, and identifying if a byte array represents GZIP-compressed data.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Read a byte array written in any common notation -- hex, hex dumps, MAC addresses, {@code \x} escapes,
 *       number lists, Base64, data URIs ({@link #decode(String)}).</li>
 *   <li>Read correctly padded Base64 ({@link #decodeBase64(String)}).</li>
 *   <li>Convert byte arrays to hexadecimal strings ({@link #encode(byte[])}).</li>
 *   <li>Check if a byte array is GZIP-compressed ({@link #isGzipped(byte[])}).</li>
 *   <li>Find byte patterns within byte arrays ({@link #indexOf(byte[], byte[], int)}).</li>
 *   <li>Internally optimized for performance with reusable utilities like {@link #toHexChar(int)}.</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Encode a byte array to a hexadecimal string
 * byte[] data = {0x1f, 0x8b, 0x3c};
 * String hex = ByteUtilities.encode(data); // "1F8B3C"
 *
 * // Read it back, in that notation or any other common one
 * byte[] decoded = ByteUtilities.decode("1F8B3C");          // {0x1f, 0x8b, 0x3c}
 * decoded = ByteUtilities.decode("1f:8b:3c");               // the same
 * decoded = ByteUtilities.decode("[31, -117, 60]");         // the same
 *
 * // Check if a byte array is GZIP-compressed
 * boolean isGzip = ByteUtilities.isGzipped(data); // true
 * }</pre>
 *
 * <h2>Security Configuration</h2>
 * <p>ByteUtilities provides configurable security options through system properties.
 * All security features are <strong>disabled by default</strong> for backward compatibility:</p>
 * <ul>
 *   <li><code>byteutilities.security.enabled=false</code> &mdash; Master switch to enable all security features</li>
 *   <li><code>byteutilities.max.hex.string.length=0</code> &mdash; Hex string length limit for decode operations (0=disabled)</li>
 *   <li><code>byteutilities.max.array.size=0</code> &mdash; Byte array size limit for encode operations (0=disabled)</li>
 * </ul>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * // Enable security with default limits
 * System.setProperty("byteutilities.security.enabled", "true");
 *
 * // Or enable with custom limits
 * System.setProperty("byteutilities.security.enabled", "true");
 * System.setProperty("byteutilities.max.hex.string.length", "10000");
 * System.setProperty("byteutilities.max.array.size", "1000000");
 * }</pre>
 *
 * <h2>Design Notes</h2>
 * <ul>
 *   <li>The class is designed as a utility class, and its constructor is private to prevent instantiation.</li>
 *   <li>All methods are static and thread-safe, making them suitable for use in concurrent environments.</li>
 *   <li>The {@code decode} method returns {@code null} for text in none of its notations (e.g., {@code "456"}, an odd
 *       number of hex digits).</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <p>
 * The methods in this class are optimized for performance:
 * <ul>
 *   <li>{@link #encode(byte[])} avoids excessive memory allocations by pre-sizing the {@link StringBuilder}.</li>
 *   <li>{@link #decode(String)} reads plain hex first, with minimal overhead; only other text goes on to the other
 *       notations.</li>
 * </ul>
 * </p>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         Ken Partlow (kpartlow@gmail.com)
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
public final class ByteUtilities {
    // Security Configuration - cached for performance with property change detection
    // Default limits used when security is enabled but no custom limits specified
    private static final int DEFAULT_MAX_HEX_STRING_LENGTH = 1000000;  // 1MB hex string
    private static final int DEFAULT_MAX_ARRAY_SIZE = 10000000;        // 10MB byte array

    // Property names
    private static final String PROP_SECURITY_ENABLED = "byteutilities.security.enabled";
    private static final String PROP_MAX_HEX_LENGTH = "byteutilities.max.hex.string.length";
    private static final String PROP_MAX_ARRAY_SIZE = "byteutilities.max.array.size";

    private static volatile SecurityConfig securityConfigCache;

    // Maximum array size that can be safely doubled without overflow
    private static final int MAX_SAFE_ARRAY_SIZE = Integer.MAX_VALUE / 2;

    private static final class SecurityConfig {
        private final String securityEnabledSource;
        private final String maxHexLengthSource;
        private final String maxArraySizeSource;
        private final boolean securityEnabled;
        private final int maxHexStringLength;
        private final int maxArraySize;

        private SecurityConfig(String securityEnabledSource, String maxHexLengthSource, String maxArraySizeSource) {
            this.securityEnabledSource = securityEnabledSource;
            this.maxHexLengthSource = maxHexLengthSource;
            this.maxArraySizeSource = maxArraySizeSource;
            securityEnabled = Boolean.parseBoolean(securityEnabledSource);
            maxHexStringLength = parseLimit(maxHexLengthSource, DEFAULT_MAX_HEX_STRING_LENGTH);
            maxArraySize = parseLimit(maxArraySizeSource, DEFAULT_MAX_ARRAY_SIZE);
        }

        private boolean matches(String securityEnabledSource, String maxHexLengthSource, String maxArraySizeSource) {
            return equalsNullable(this.securityEnabledSource, securityEnabledSource) &&
                    equalsNullable(this.maxHexLengthSource, maxHexLengthSource) &&
                    equalsNullable(this.maxArraySizeSource, maxArraySizeSource);
        }
    }

    private static boolean equalsNullable(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static int parseLimit(String source, int defaultLimit) {
        if (source == null) {
            return defaultLimit;
        }
        try {
            int limit = Integer.parseInt(source);
            return limit <= 0 ? 0 : limit;
        } catch (NumberFormatException ignored) {
            return defaultLimit;
        }
    }

    private static SecurityConfig getSecurityConfig() {
        String securityEnabledSource = System.getProperty(PROP_SECURITY_ENABLED, "false");
        String maxHexLengthSource = System.getProperty(PROP_MAX_HEX_LENGTH);
        String maxArraySizeSource = System.getProperty(PROP_MAX_ARRAY_SIZE);
        SecurityConfig config = securityConfigCache;
        if (config != null && config.matches(securityEnabledSource, maxHexLengthSource, maxArraySizeSource)) {
            return config;
        }

        synchronized (ByteUtilities.class) {
            securityEnabledSource = System.getProperty(PROP_SECURITY_ENABLED, "false");
            maxHexLengthSource = System.getProperty(PROP_MAX_HEX_LENGTH);
            maxArraySizeSource = System.getProperty(PROP_MAX_ARRAY_SIZE);
            config = securityConfigCache;
            if (config == null || !config.matches(securityEnabledSource, maxHexLengthSource, maxArraySizeSource)) {
                config = new SecurityConfig(securityEnabledSource, maxHexLengthSource, maxArraySizeSource);
                securityConfigCache = config;
            }
            return config;
        }
    }

    /**
     * Fast path for the default (security disabled) case: one property read instead
     * of the full three-property config snapshot. Still reads per call so runtime
     * toggling of the master switch is honored immediately.
     */
    private static boolean isSecurityDisabled() {
        return !Boolean.parseBoolean(System.getProperty(PROP_SECURITY_ENABLED, "false"));
    }

    private static int getMaxHexStringLength() {
        SecurityConfig config = getSecurityConfig();
        return config.securityEnabled ? config.maxHexStringLength : 0;
    }

    private static int getMaxArraySize() {
        SecurityConfig config = getSecurityConfig();
        return config.securityEnabled ? config.maxArraySize : 0;
    }

    // For encode: Array of hex digits (private - use toHexChar() for public access)
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    // For decode: Precomputed lookup table for hex digits.
    // Maps ASCII codes (0–127) to their hex value or -1 if invalid.
    private static final int[] HEX_LOOKUP = new int[128];
    static {
        Arrays.fill(HEX_LOOKUP, -1);
        for (char c = '0'; c <= '9'; c++) {
            HEX_LOOKUP[c] = c - '0';
        }
        for (char c = 'A'; c <= 'F'; c++) {
            HEX_LOOKUP[c] = 10 + (c - 'A');
        }
        for (char c = 'a'; c <= 'f'; c++) {
            HEX_LOOKUP[c] = 10 + (c - 'a');
        }
    }

    /**
     * Magic number identifying a gzip byte stream.
     */
    private static final byte[] GZIP_MAGIC = {(byte) 0x1f, (byte) 0x8b};

    private ByteUtilities() { }

    /**
     * Convert the specified value (0 .. 15) to the corresponding hex digit.
     *
     * @param value to be converted
     * @return '0'...'F' in char format.
     */
    public static char toHexChar(final int value) {
        return HEX_ARRAY[value & 0x0f];
    }

    /**
     * Reads a byte array written in any common notation, or returns {@code null}. Surrounding whitespace is ignored,
     * and blank text is the empty array. Whoever calls this knows they hold bytes; the question is only how they were
     * written:
     * <ul>
     *   <li><b>Hex digits</b>: {@code "1F8B3C"}, {@code "1f8b3c"}; with a {@code 0x} prefix, {@code "0x1F8B3C"}, where
     *       an odd count takes a leading zero ({@code "0xF"} is {@code {0x0F}}). Without the prefix the count must be
     *       even: {@code "456"} is {@code null}.</li>
     *   <li><b>Hex in groups</b> separated by whitespace (hex dumps, line breaks included), by {@code ':'} (MAC
     *       addresses, certificate fingerprints, OpenSSL's listings with a line break after a {@code ':'}) or by
     *       {@code '-'} (Windows MAC addresses), one kind throughout: {@code "AF DE 92 9F"}, {@code "af:de:92:9f"},
     *       {@code "AF-DE-92-9F"}, {@code "6A3B 0D4C"} (groups of four, as GPG prints them), {@code "0xAF 0xDE"}. A
     *       group of one digit is one byte ({@code "0:1c:42:b"}, as macOS prints a MAC address).</li>
     *   <li><b>{@code \x} escapes</b>: {@code "\xAF\xDE\x92"} (C, Python), or {@code "\xAFDE92"} (PostgreSQL
     *       {@code bytea}); and the SQL literal {@code "X'AFDE92'"}.</li>
     *   <li><b>A list of numbers</b> in brackets, braces or parentheses, separated by commas or whitespace, or
     *       separated by commas with no brackets: {@code "[31, -117, 60]"} ({@link Arrays#toString(byte[])}, a JSON
     *       array), {@code "{0x1F, (byte) 0x8B, 0x3C}"} and {@code "new byte[] {31, -117, 60}"} (Java and C
     *       source), {@code "[ 31 139  60]"} (numpy), {@code "31, 139, 60"}. Each number is decimal from -128 to 255
     *       -- a signed or an unsigned byte, so -1 and 255 are both {@code 0xFF} -- or {@code 0x00} to {@code 0xFF}.
     *       Numbers in a list are decimal: {@code "[10, 20]"} is {@code {10, 20}}, while {@code "10 20"} is hex
     *       groups, {@code {0x10, 0x20}}. A trailing comma is allowed, as in Java source.</li>
     *   <li><b>Base64</b>, standard or URL-safe, padded correctly ({@link #decodeBase64(String)}), with the line
     *       breaks of MIME Base64 ignored: {@code "AQID"}, {@code "-_8="}. {@code "AF"} and {@code "OK"} are not
     *       Base64.</li>
     *   <li><b>A data URI</b> (RFC 2397): {@code "data:image/png;base64,iVBORw0KGgo="} (Base64), or
     *       {@code "data:,Hello%2C%20World"} (percent-encoded).</li>
     * </ul>
     * Text that fits two notations is read as hex: {@code "CAFEBABE"} is the four bytes of hex, though it is also
     * six bytes of Base64, and {@code "AB-CD-EF"} is three bytes of hex, not six of URL-safe Base64. When the text is
     * known to be Base64, read it with
     * {@link #decodeBase64(String)}.
     *
     * @param s the text to read
     * @return the bytes it writes, or {@code null} if {@code s} is null or in none of these notations
     * @throws SecurityException if security is enabled and {@code s} is longer than
     *         {@code byteutilities.max.hex.string.length}
     */
    public static byte[] decode(final String s) {
        return decodeInternal(s, true);
    }

    /**
     * Reads a byte array written in any common notation, as {@link #decode(String)} does.
     *
     * @param s the text to read
     * @return the bytes it writes, or {@code null} if {@code s} is null or in none of the notations
     */
    public static byte[] decode(final CharSequence s) {
        return decodeInternal(s, true);
    }

    /**
     * Decodes Base64 -- the standard alphabet or the URL-safe one -- that is padded correctly, or returns
     * {@code null}. Padded correctly means exactly what every encoder writes: the length is a multiple of 4,
     * {@code =} filling out the last group, and the bits that padding leaves over are zero. So {@code "AQ=="} is
     * {@code {1}} and {@code "AQL9"} (three bytes, no padding needed) is {@code {1, 2, -3}}, while {@code "AQ"}
     * (no padding), {@code "AQ="} (too little) and {@code "AF=="} (no encoder writes it) are not Base64. A short
     * word such as {@code "AF"} or {@code "OK"} is therefore never read as bytes; a four-letter one such as
     * {@code "DATA"} is valid Base64 and is. Whitespace is not skipped ({@link #decode(String)} skips the line
     * breaks of MIME Base64).
     *
     * @param s the text to decode
     * @return the decoded bytes, or {@code null} if {@code s} is null or not correctly padded Base64
     */
    public static byte[] decodeBase64(final String s) {
        if (s == null || (s.length() & 3) != 0 || !padBitsAreZero(s)) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException notStandard) {
            try {
                return Base64.getUrlDecoder().decode(s);
            } catch (IllegalArgumentException notBase64) {
                return null;
            }
        }
    }

    // The bits a '=' leaves over in the last group are zero, as every encoder writes them. (The JDK's decoders
    // ignore them, so "AF==" and "AA==" would both be {0}.)
    private static boolean padBitsAreZero(final String s) {
        final int len = s.length();
        if (len == 0 || s.charAt(len - 1) != '=') {
            return true;
        }
        if (s.charAt(len - 2) == '=') {     // "xY==" is one byte: Y's low 4 bits are left over
            return (base64Value(s.charAt(len - 3)) & 0x0F) == 0;
        }
        return (base64Value(s.charAt(len - 2)) & 0x03) == 0;   // "xyZ=" is two bytes: Z's low 2 bits are left over
    }

    // The 6-bit value of a Base64 digit in either alphabet, or -1
    private static int base64Value(final char c) {
        if (c >= 'A' && c <= 'Z') {
            return c - 'A';
        }
        if (c >= 'a' && c <= 'z') {
            return c - 'a' + 26;
        }
        if (c >= '0' && c <= '9') {
            return c - '0' + 52;
        }
        if (c == '+' || c == '-') {
            return 62;
        }
        return c == '/' || c == '_' ? 63 : -1;
    }

    static byte[] decodeTrusted(final String s) {
        return decodeInternal(s, false);
    }

    private static byte[] decodeInternal(final CharSequence s, boolean enforceSecurityLimit) {
        if (s == null) {
            return null;
        }
        final int len = s.length();
        
        if (enforceSecurityLimit && !isSecurityDisabled()) {
            // Security check: validate hex string length
            int maxHexLength = getMaxHexStringLength();
            if (maxHexLength > 0 && len > maxHexLength) {
                throw new SecurityException("Hex string length exceeds maximum allowed: " + maxHexLength);
            }
        }
        byte[] bytes = decodeHex(s, 0, len);        // the common case: hex digits, nothing else
        return bytes != null ? bytes : decodeAnyNotation(s.toString().trim());
    }

    // Hex digits from 'from' to 'to', an even count of them, or null
    private static byte[] decodeHex(final CharSequence s, final int from, final int to) {
        final int len = to - from;
        if ((len & 1) != 0) {
            return null;
        }
        byte[] bytes = new byte[len >> 1];
        for (int i = from, j = 0; i < to; i += 2) {
            int hi = hexValue(s.charAt(i));
            int lo = hexValue(s.charAt(i + 1));
            if (hi == -1 || lo == -1) {
                return null;
            }
            bytes[j++] = (byte) ((hi << 4) | lo);
        }
        return bytes;
    }

    private static int hexValue(final char c) {
        return c < HEX_LOOKUP.length ? HEX_LOOKUP[c] : -1;
    }

    // The notations after plain hex, in the order decode(String) documents; s is trimmed
    private static byte[] decodeAnyNotation(final String s) {
        if (s.isEmpty()) {
            return new byte[0];
        }
        if (s.regionMatches(true, 0, "data:", 0, 5)) {
            return decodeDataUri(s);
        }
        byte[] bytes = decodeNumberList(s);
        if (bytes == null) {
            bytes = decodeHexGroups(s);
        }
        if (bytes == null) {
            bytes = decodeEscapedHex(s);
        }
        if (bytes == null && s.length() >= 3 && (s.charAt(0) | 0x20) == 'x' && s.charAt(1) == '\''
                && s.charAt(s.length() - 1) == '\'') {
            bytes = decodeHex(s, 2, s.length() - 1);       // X'AFDE92', SQL
        }
        return bytes != null ? bytes : decodeBase64(withoutWhitespace(s));
    }

    // "AF DE 92", "af:de:92", "AF-DE-92", "0:1c:42:b", "6A3B 0D4C", "0xAF 0xDE", "0x1F8B3C" -- groups of hex digits
    // (one digit, or an even count; any count after 0x) between one kind of separator: whitespace, or ':' or '-'
    // with whitespace allowed around it.
    private static byte[] decodeHexGroups(final String s) {
        final int len = s.length();
        byte[] out = new byte[(len + 1) >> 1];
        int n = 0;
        int i = 0;
        int groups = 0;
        boolean firstIsOneDigit = false;
        char separator = 0;
        while (true) {
            boolean prefixed = i + 1 < len && s.charAt(i) == '0' && (s.charAt(i + 1) | 0x20) == 'x';
            int start = prefixed ? i + 2 : i;
            int end = start;
            while (end < len && hexValue(s.charAt(end)) >= 0) {
                end++;
            }
            int digits = end - start;
            if (digits == 0 || (!prefixed && digits > 1 && (digits & 1) != 0)) {
                return null;
            }
            if (groups++ == 0) {
                firstIsOneDigit = !prefixed && digits == 1;
            }
            if ((digits & 1) != 0) {
                out[n++] = (byte) hexValue(s.charAt(start++));
            }
            for (; start < end; start += 2) {
                out[n++] = (byte) ((hexValue(s.charAt(start)) << 4) | hexValue(s.charAt(start + 1)));
            }
            if (end == len) {
                break;
            }
            int k = end;
            while (k < len && s.charAt(k) <= ' ') {
                k++;
            }
            char found = k > end ? ' ' : 0;
            if (k < len && (s.charAt(k) == ':' || s.charAt(k) == '-')) {
                found = s.charAt(k++);
                while (k < len && s.charAt(k) <= ' ') {
                    k++;
                }
            }
            if (separator != 0 && found != separator) {
                return null;        // two kinds of separator
            }
            // Any other character, or a separator at the end, leaves the next group with no digits: null
            separator = found;
            i = k;
        }
        if (groups == 1 && firstIsOneDigit) {
            return null;            // a lone digit, like any odd count without 0x
        }
        return n == out.length ? out : Arrays.copyOf(out, n);
    }

    // "\xAF\xDE\x92" (C, Python) or "\xAFDE92" (PostgreSQL bytea)
    private static byte[] decodeEscapedHex(final String s) {
        final int len = s.length();
        if (len < 4 || s.charAt(0) != '\\' || (s.charAt(1) | 0x20) != 'x') {
            return null;
        }
        if (s.indexOf('\\', 2) < 0) {
            return decodeHex(s, 2, len);
        }
        if ((len & 3) != 0) {
            return null;
        }
        byte[] out = new byte[len >> 2];
        for (int i = 0, j = 0; i < len; i += 4) {
            if (s.charAt(i) != '\\' || (s.charAt(i + 1) | 0x20) != 'x') {
                return null;
            }
            int hi = hexValue(s.charAt(i + 2));
            int lo = hexValue(s.charAt(i + 3));
            if (hi == -1 || lo == -1) {
                return null;
            }
            out[j++] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    private static final Pattern JAVA_ARRAY_PREFIX = Pattern.compile("(?:new\\s+)?byte\\s*\\[\\s*]\\s*(?=\\{)");
    private static final int NOT_A_BYTE = Integer.MIN_VALUE;

    // "[31, -117, 60]", "{0x1F, (byte) 0x8B}", "new byte[] {31, 139}", "(1 2 3)", "[ 31 139  60]", "31, 139, 60"
    private static byte[] decodeNumberList(final String s) {
        int from = 0;
        int to = s.length();
        Matcher prefix = JAVA_ARRAY_PREFIX.matcher(s);
        if (prefix.lookingAt()) {
            from = prefix.end();
        }
        char open = s.charAt(from);
        char close = s.charAt(to - 1);
        if (to - from >= 2 && ((open == '[' && close == ']') || (open == '{' && close == '}')
                || (open == '(' && close == ')'))) {
            from++;
            to--;
        } else if (from > 0 || s.indexOf(',') < 0) {
            return null;
        }
        byte[] out = new byte[(to - from + 1) >> 1];
        int n = 0;
        int i = skipWhitespace(s, from, to);
        while (i < to) {
            if (s.startsWith("(byte)", i)) {
                i = skipWhitespace(s, i + 6, to);
            }
            int end = i;
            while (end < to && isNumberChar(s.charAt(end))) {
                end++;
            }
            int value = byteValue(s, i, end);
            if (value == NOT_A_BYTE) {
                return null;
            }
            out[n++] = (byte) value;
            int next = skipWhitespace(s, end, to);
            if (next < to && s.charAt(next) == ',') {
                next = skipWhitespace(s, next + 1, to);
            } else if (next == end && next < to) {
                return null;        // a number runs into something else
            }
            i = next;
        }
        return n == out.length ? out : Arrays.copyOf(out, n);
    }

    private static int skipWhitespace(final String s, int i, final int to) {
        while (i < to && s.charAt(i) <= ' ') {
            i++;
        }
        return i;
    }

    private static boolean isNumberChar(final char c) {
        return hexValue(c) >= 0 || c == 'x' || c == 'X' || c == '-' || c == '+';
    }

    // A decimal from -128 to 255, or 0x and one or two hex digits; NOT_A_BYTE otherwise
    private static int byteValue(final String s, final int from, final int to) {
        if (to - from >= 3 && to - from <= 4 && s.charAt(from) == '0' && (s.charAt(from + 1) | 0x20) == 'x') {
            int value = 0;
            for (int i = from + 2; i < to; i++) {
                int digit = hexValue(s.charAt(i));
                if (digit == -1) {
                    return NOT_A_BYTE;
                }
                value = (value << 4) | digit;
            }
            return value;
        }
        int i = from;
        boolean negative = i < to && s.charAt(i) == '-';
        if (i < to && (negative || s.charAt(i) == '+')) {
            i++;
        }
        if (i == to) {
            return NOT_A_BYTE;
        }
        int value = 0;
        for (; i < to; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return NOT_A_BYTE;
            }
            value = value * 10 + (c - '0');
            if (value > 255) {
                return NOT_A_BYTE;
            }
        }
        return !negative ? value : value <= 128 ? -value : NOT_A_BYTE;
    }

    // data:[<media type>][;base64],<data> (RFC 2397): Base64, or percent-encoded
    private static byte[] decodeDataUri(final String s) {
        int comma = s.indexOf(',');
        if (comma < 0) {
            return null;
        }
        String data = s.substring(comma + 1);
        if (comma >= 12 && s.regionMatches(true, comma - 7, ";base64", 0, 7)) {
            return decodeBase64(withoutWhitespace(data));
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(data.length());
        final int len = data.length();
        int i = 0;
        while (i < len) {
            if (data.charAt(i) == '%') {
                if (i + 2 >= len) {
                    return null;
                }
                int hi = hexValue(data.charAt(i + 1));
                int lo = hexValue(data.charAt(i + 2));
                if (hi == -1 || lo == -1) {
                    return null;
                }
                out.write((hi << 4) | lo);
                i += 3;
            } else {
                int end = data.indexOf('%', i);
                if (end < 0) {
                    end = len;
                }
                byte[] text = data.substring(i, end).getBytes(StandardCharsets.UTF_8);
                out.write(text, 0, text.length);
                i = end;
            }
        }
        return out.toByteArray();
    }

    private static String withoutWhitespace(final String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) > ' ') {
            i++;
        }
        if (i == s.length()) {
            return s;
        }
        StringBuilder kept = new StringBuilder(s.length()).append(s, 0, i);
        for (; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c > ' ') {
                kept.append(c);
            }
        }
        return kept.toString();
    }

    /**
     * Converts a byte array into a string of hex digits.
     * 
     * @param bytes the byte array to encode
     * @return the hexadecimal string representation, or null if input is null
     */
    public static String encode(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        
        // Security check: validate byte array size
        if (!isSecurityDisabled()) {
            int maxArraySize = getMaxArraySize();
            if (maxArraySize > 0 && bytes.length > maxArraySize) {
                throw new SecurityException("Byte array size exceeds maximum allowed: " + maxArraySize);
            }
        }

        // Check for integer overflow: bytes.length * 2 must not overflow
        if (bytes.length > MAX_SAFE_ARRAY_SIZE) {
            throw new IllegalArgumentException("Byte array too large to encode: length " + bytes.length +
                    " exceeds maximum safe size " + MAX_SAFE_ARRAY_SIZE);
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[j++] = HEX_ARRAY[v >>> 4];
            hexChars[j++] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Checks if the byte array represents gzip-compressed data.
     */
    public static boolean isGzipped(byte[] bytes) {
        return isGzipped(bytes, 0);
    }

    /**
     * Checks if the byte array represents gzip-compressed data starting at the given offset.
     *
     * @param bytes  the byte array to inspect
     * @param offset the starting offset within the array
     * @return true if the bytes appear to be GZIP compressed, false if bytes is null, offset is invalid, or not enough bytes
     */
    public static boolean isGzipped(byte[] bytes, int offset) {
        if (bytes == null || offset < 0 || offset >= bytes.length) {
            return false;
        }
        return bytes.length - offset >= 2 &&
                bytes[offset] == GZIP_MAGIC[0] && bytes[offset + 1] == GZIP_MAGIC[1];
    }

    /**
     * Finds the first occurrence of a byte pattern within a byte array, starting from the specified index.
     * <p>
     * This method performs a simple linear search for the pattern within the data array.
     * It is useful for locating byte sequences such as markers, headers, or placeholders
     * within binary data.
     * </p>
     *
     * <h3>Example Usage</h3>
     * <pre>{@code
     * byte[] data = {0x00, 0x01, 0x02, 0x03, 0x04, 0x02, 0x03};
     * byte[] pattern = {0x02, 0x03};
     * int index = ByteUtilities.indexOf(data, pattern, 0);  // Returns 2
     * int next = ByteUtilities.indexOf(data, pattern, 3);   // Returns 5
     * }</pre>
     *
     * @param data    the byte array to search within
     * @param pattern the byte pattern to find
     * @param start   the index to start searching from (inclusive)
     * @return the index of the first occurrence of the pattern, or -1 if not found
     *         or if any parameter is invalid (null arrays, negative start, etc.)
     */
    public static int indexOf(byte[] data, byte[] pattern, int start) {
        if (data == null || pattern == null || start < 0 || pattern.length == 0) {
            return -1;
        }
        final int dataLen = data.length;
        final int patternLen = pattern.length;
        if (patternLen > dataLen || start > dataLen - patternLen) {
            return -1;
        }

        // Fast path for single-byte patterns
        if (patternLen == 1) {
            byte target = pattern[0];
            for (int i = start; i < dataLen; i++) {
                if (data[i] == target) {
                    return i;
                }
            }
            return -1;
        }

        final byte first = pattern[0];
        final byte last = pattern[patternLen - 1];
        final int limit = dataLen - patternLen;
        outer:
        for (int i = start; i <= limit; i++) {
            if (data[i] != first || data[i + patternLen - 1] != last) {
                continue;
            }
            for (int j = 1; j < patternLen - 1; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * Finds the last occurrence of a byte pattern within a byte array, searching backwards from the specified index.
     * <p>
     * This method searches backwards from the start position to find the last occurrence
     * of the pattern. It is useful for locating byte sequences when you need the rightmost match.
     * </p>
     *
     * <h3>Example Usage</h3>
     * <pre>{@code
     * byte[] data = {0x02, 0x03, 0x00, 0x02, 0x03};
     * byte[] pattern = {0x02, 0x03};
     * int index = ByteUtilities.lastIndexOf(data, pattern, data.length - 1);  // Returns 3
     * int prev = ByteUtilities.lastIndexOf(data, pattern, 2);                  // Returns 0
     * }</pre>
     *
     * @param data    the byte array to search within
     * @param pattern the byte pattern to find
     * @param start   the index to start searching backwards from (inclusive)
     * @return the index of the last occurrence of the pattern, or -1 if not found
     *         or if any parameter is invalid (null arrays, negative start, etc.)
     */
    public static int lastIndexOf(byte[] data, byte[] pattern, int start) {
        if (data == null || pattern == null || start < 0 || pattern.length == 0) {
            return -1;
        }
        final int dataLen = data.length;
        final int patternLen = pattern.length;
        if (patternLen > dataLen) {
            return -1;
        }

        // Adjust start to the last valid position where pattern could fit
        int effectiveStart = Math.min(start, dataLen - patternLen);
        if (effectiveStart < 0) {
            return -1;
        }

        // Fast path for single-byte patterns
        if (patternLen == 1) {
            byte target = pattern[0];
            for (int i = effectiveStart; i >= 0; i--) {
                if (data[i] == target) {
                    return i;
                }
            }
            return -1;
        }

        final byte first = pattern[0];
        final byte last = pattern[patternLen - 1];
        outer:
        for (int i = effectiveStart; i >= 0; i--) {
            if (data[i] != first || data[i + patternLen - 1] != last) {
                continue;
            }
            for (int j = 1; j < patternLen - 1; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * Finds the last occurrence of a byte pattern within a byte array.
     * <p>
     * This is a convenience method that searches from the end of the data array.
     * </p>
     *
     * @param data    the byte array to search within
     * @param pattern the byte pattern to find
     * @return the index of the last occurrence of the pattern, or -1 if not found
     *         or if any parameter is invalid
     * @see #lastIndexOf(byte[], byte[], int)
     */
    public static int lastIndexOf(byte[] data, byte[] pattern) {
        if (data == null) {
            return -1;
        }
        return lastIndexOf(data, pattern, data.length - 1);
    }

    /**
     * Checks if a byte array contains the specified byte pattern.
     * <p>
     * This is a convenience method equivalent to {@code indexOf(data, pattern, 0) >= 0}.
     * </p>
     *
     * <h3>Example Usage</h3>
     * <pre>{@code
     * byte[] data = {0x00, 0x01, 0x02, 0x03};
     * byte[] pattern = {0x01, 0x02};
     * boolean found = ByteUtilities.contains(data, pattern);  // Returns true
     * }</pre>
     *
     * @param data    the byte array to search within
     * @param pattern the byte pattern to find
     * @return true if the pattern is found within data, false otherwise
     */
    public static boolean contains(byte[] data, byte[] pattern) {
        return indexOf(data, pattern, 0) >= 0;
    }
}
