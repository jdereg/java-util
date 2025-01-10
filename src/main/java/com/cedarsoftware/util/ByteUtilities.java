package com.cedarsoftware.util;

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
 *   <li>Internally optimized for performance with reusable utilities like {@link #convertDigit(int)}.</li>
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
    private static final char[] _hex =
            {
                    '0', '1', '2', '3', '4', '5', '6', '7',
                    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
            };

    /**
     * <p>
     * {@code StringUtilities} instances should NOT be constructed in standard
     * programming. Instead, the class should be used statically as
     * {@code StringUtilities.trim();}.
     * </p>
     */
    private ByteUtilities() {
        super();
    }

    // Turn hex String into byte[]
    // If string is not even length, return null.

    public static byte[] decode(final String s) {
        final int len = s.length();
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
     * Convert a byte array into a printable format containing a String of hex
     * digit characters (two per byte).
     *
     * @param bytes array representation
     * @return String hex digits
     */
    public static String encode(final byte[] bytes) {
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
     * @return '0'...'F' in char format.
     */
    private static char convertDigit(final int value) {
        return _hex[value & 0x0f];
    }

    /**
     * @param bytes byte[] of bytes to test
     * @return true if bytes are gzip compressed, false otherwise.
     */
    public static boolean isGzipped(byte[] bytes) {
        if (ArrayUtilities.size(bytes) < 18) {  // minimum valid GZIP size
            return false;
        }
        return bytes[0] == (byte) 0x1f && bytes[1] == (byte) 0x8b;
    }
}
