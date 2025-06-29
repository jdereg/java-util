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
 *   <li><code>bytes.max.hex.string.length=0</code> &mdash; Hex string length limit for decode operations (0=disabled)</li>
 *   <li><code>bytes.max.array.size=0</code> &mdash; Byte array size limit for encode operations (0=disabled)</li>
 * </ul>
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
    // Security limits to prevent resource exhaustion attacks
    // 0 or negative values = disabled, positive values = enabled with limit
    private static final int MAX_HEX_STRING_LENGTH = Integer.parseInt(
            System.getProperty("bytes.max.hex.string.length", "0"));
    private static final int MAX_ARRAY_SIZE = Integer.parseInt(
            System.getProperty("bytes.max.array.size", "0"));

    // For encode: Array of hex digits.
    static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

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
        return decode((CharSequence) s);
    }

    /**
     * Converts a hexadecimal CharSequence into a byte array.
     * 
     * @param s the hexadecimal CharSequence to decode
     * @return the decoded byte array, or null if input is null, has odd length, or contains non-hex characters
     */
    public static byte[] decode(final CharSequence s) {
        if (s == null) {
            return null;
        }
        final int len = s.length();
        
        // Security check: validate hex string length
        if (MAX_HEX_STRING_LENGTH > 0 && len > MAX_HEX_STRING_LENGTH) {
            throw new SecurityException("Hex string length exceeds maximum allowed: " + MAX_HEX_STRING_LENGTH);
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
        if (MAX_ARRAY_SIZE > 0 && bytes.length > MAX_ARRAY_SIZE) {
            throw new SecurityException("Byte array size exceeds maximum allowed: " + MAX_ARRAY_SIZE);
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
}