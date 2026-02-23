package com.cedarsoftware.util;

import java.util.Arrays;

/**
 * A utility class providing static methods for operations on byte arrays and hexadecimal representations.
 * <p>
 * {@code ByteUtilities} simplifies common tasks such as encoding byte arrays to hexadecimal strings,
 * decoding hexadecimal strings back to byte arrays, and identifying if a byte array represents GZIP-compressed data.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>Convert hexadecimal strings to byte arrays ({@link #decode(String)}).</li>
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
 * // Decode a hexadecimal string back to a byte array
 * byte[] decoded = ByteUtilities.decode("1F8B3C"); // {0x1f, 0x8b, 0x3c}
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
 *   <li>The {@code decode} method returns {@code null} for invalid inputs (e.g., strings with an odd number of characters).</li>
 * </ul>
 *
 * <h2>Performance Considerations</h2>
 * <p>
 * The methods in this class are optimized for performance:
 * <ul>
 *   <li>{@link #encode(byte[])} avoids excessive memory allocations by pre-sizing the {@link StringBuilder}.</li>
 *   <li>{@link #decode(String)} uses minimal overhead to parse hexadecimal strings into bytes.</li>
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

    private static boolean isSecurityEnabled() {
        return getSecurityConfig().securityEnabled;
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
     * Converts a hexadecimal string into a byte array.
     * 
     * @param s the hexadecimal string to decode
     * @return the decoded byte array, or null if input is null, has odd length, or contains non-hex characters
     */
    public static byte[] decode(final String s) {
        return decodeInternal(s, true);
    }

    /**
     * Converts a hexadecimal CharSequence into a byte array.
     * 
     * @param s the hexadecimal CharSequence to decode
     * @return the decoded byte array, or null if input is null, has odd length, or contains non-hex characters
     */
    public static byte[] decode(final CharSequence s) {
        return decodeInternal(s, true);
    }

    static byte[] decodeTrusted(final String s) {
        return decodeInternal(s, false);
    }

    private static byte[] decodeInternal(final CharSequence s, boolean enforceSecurityLimit) {
        if (s == null) {
            return null;
        }
        final int len = s.length();
        
        if (enforceSecurityLimit) {
            // Security check: validate hex string length
            int maxHexLength = getMaxHexStringLength();
            if (maxHexLength > 0 && len > maxHexLength) {
                throw new SecurityException("Hex string length exceeds maximum allowed: " + maxHexLength);
            }
        }
        
        // Must be even length
        if ((len & 1) != 0) {
            return null;
        }
        byte[] bytes = new byte[len >> 1];
        for (int i = 0, j = 0; i < len; i += 2) {
            char c1 = s.charAt(i);
            char c2 = s.charAt(i + 1);
            // Check if the characters are within ASCII range
            if (c1 >= HEX_LOOKUP.length || c2 >= HEX_LOOKUP.length) {
                return null;
            }
            int hi = HEX_LOOKUP[c1];
            int lo = HEX_LOOKUP[c2];
            if (hi == -1 || lo == -1) {
                return null;
            }
            bytes[j++] = (byte) ((hi << 4) | lo);
        }
        return bytes;
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
        int maxArraySize = getMaxArraySize();
        if (maxArraySize > 0 && bytes.length > maxArraySize) {
            throw new SecurityException("Byte array size exceeds maximum allowed: " + maxArraySize);
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
