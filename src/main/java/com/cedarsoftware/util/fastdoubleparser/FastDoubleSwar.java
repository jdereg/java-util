/*
 * @(#)FastDoubleSwar.java
 * Copyright © 2024 Werner Randelshofer, Switzerland. MIT License.
 */
package com.cedarsoftware.util.fastdoubleparser;


/**
 * This class provides methods for parsing multiple characters at once using
 * the "SIMD with a register" (SWAR) technique.
 * <p>
 * References:
 * <dl>
 *     <dt>Leslie Lamport, Multiple Byte Processing with Full-Word Instructions</dt>
 *     <dd><a href="https://lamport.azurewebsites.net/pubs/multiple-byte.pdf">azurewebsites.net</a></dd>
 *
 *     <dt>Daniel Lemire, fast_float number parsing library: 4x faster than strtod.
 *     <a href="https://github.com/fastfloat/fast_float/blob/cc1e01e9eee74128e48d51488a6b1df4a767a810/LICENSE-MIT">MIT License</a>.</dt>
 *     <dd><a href="https://github.com/fastfloat/fast_float">github.com</a></dd>
 *
 *     <dt>Daniel Lemire, Number Parsing at a Gigabyte per Second,
 *     Software: Practice and Experience 51 (8), 2021.
 *     arXiv.2101.11408v3 [cs.DS] 24 Feb 2021</dt>
 *     <dd><a href="https://arxiv.org/pdf/2101.11408.pdf">arxiv.org</a></dd>
 * </dl>
 * </p>
 */
final class FastDoubleSwar {

    /**
     * Checks if '0' <= c && c <= '9'.
     *
     * @param c a character
     * @return true if c is a digit
     */
    protected static boolean isDigit(char c) {
        // We take advantage of the fact that char is an unsigned numeric type:
        // subtracted values wrap around.
        return (char) (c - '0') < 10;
    }

    /**
     * Checks if '0' <= c && c <= '9'.
     *
     * @param c a character
     * @return true if c is a digit
     */
    protected static boolean isDigit(byte c) {
        // We check if '0' <= c && c <= '9'.
        // We take advantage of the fact that char is an unsigned numeric type:
        // subtracted values wrap around.
        return (char) (c - '0') < 10;
    }

    public static boolean isEightDigits(byte[] a, int offset) {
        return isEightDigitsUtf8((long) readLongLE(a, offset));
    }

    /**
     * Checks if the string contains eight digits at the specified
     * offset.
     *
     * @param a      a string
     * @param offset offset into string
     * @return true if eight digits
     * @throws IndexOutOfBoundsException if offset is larger than 2^29.
     */
    public static boolean isEightDigits(char[] a, int offset) {
        long first = a[offset]
                | (long) a[offset + 1] << 16
                | (long) a[offset + 2] << 32
                | (long) a[offset + 3] << 48;
        long second = a[offset + 4]
                | (long) a[offset + 5] << 16
                | (long) a[offset + 6] << 32
                | (long) a[offset + 7] << 48;
        return isEightDigitsUtf16(first, second);
    }

    public static boolean isEightDigits(CharSequence a, int offset) {
        boolean success = true;
        for (int i = 0; i < 8; i++) {
            char ch = a.charAt(i + offset);
            success &= isDigit(ch);
        }
        return success;
    }

    public static boolean isEightDigitsUtf16(long first, long second) {
        long fval = first - 0x0030_0030_0030_0030L;
        long sval = second - 0x0030_0030_0030_0030L;

        // Create a predicate for all bytes which are smaller than '0' (0x0030)
        // or greater than '9' (0x0039).
        // We have 0x007f - 0x0039 = 0x0046.
        // The predicate is true if the hsb of a byte is set: (predicate & 0xff80) != 0.
        long fpre = first + 0x0046_0046_0046_0046L | fval;
        long spre = second + 0x0046_0046_0046_0046L | sval;
        return ((fpre | spre) & 0xff80_ff80_ff80_ff80L) == 0L;
    }

    public static boolean isEightDigitsUtf8(long chunk) {
        long val = chunk - 0x3030303030303030L;
        long predicate = ((chunk + 0x4646464646464646L) | val) & 0x8080808080808080L;
        return predicate == 0L;
    }

    public static boolean isEightZeroes(byte[] a, int offset) {
        return isEightZeroesUtf8(readLongLE(a, offset));
    }

    public static boolean isEightZeroes(CharSequence a, int offset) {
        boolean success = true;
        for (int i = 0; i < 8; i++) {
            success &= '0' == a.charAt(i + offset);
        }
        return success;
    }

    /**
     * Checks if the string contains eight zeroes at the specified
     * offset.
     *
     * @param a      a string
     * @param offset offset into string
     * @return true if eight digits
     * @throws IndexOutOfBoundsException if offset is larger than 2^29.
     */
    public static boolean isEightZeroes(char[] a, int offset) {
        long first = a[offset]
                | (long) a[offset + 1] << 16
                | (long) a[offset + 2] << 32
                | (long) a[offset + 3] << 48;
        long second = a[offset + 4]
                | (long) a[offset + 5] << 16
                | (long) a[offset + 6] << 32
                | (long) a[offset + 7] << 48;
        return isEightZeroesUtf16(first, second);
    }

    public static boolean isEightZeroesUtf16(long first, long second) {
        return first == 0x0030_0030_0030_0030L
                && second == 0x0030_0030_0030_0030L;
    }

    public static boolean isEightZeroesUtf8(long chunk) {
        return chunk == 0x3030303030303030L;
    }

    public static int readIntBE(byte[] a, int offset) {
        return ((a[offset] & 0xff) << 24)
                | ((a[offset + 1] & 0xff) << 16)
                | ((a[offset + 2] & 0xff) << 8)
                | (a[offset + 3] & 0xff);
    }

    public static int readIntLE(byte[] a, int offset) {
        return ((a[offset + 3] & 0xff) << 24)
                | ((a[offset + 2] & 0xff) << 16)
                | ((a[offset + 1] & 0xff) << 8)
                | (a[offset] & 0xff);
    }

    public static long readLongBE(byte[] a, int offset) {
        return ((a[offset] & 0xffL) << 56)
                | ((a[offset + 1] & 0xffL) << 48)
                | ((a[offset + 2] & 0xffL) << 40)
                | ((a[offset + 3] & 0xffL) << 32)
                | ((a[offset + 4] & 0xffL) << 24)
                | ((a[offset + 5] & 0xffL) << 16)
                | ((a[offset + 6] & 0xffL) << 8)
                | (a[offset + 7] & 0xffL);
    }

    public static long readLongLE(byte[] a, int offset) {
        return ((a[offset + 7] & 0xffL) << 56)
                | ((a[offset + 6] & 0xffL) << 48)
                | ((a[offset + 5] & 0xffL) << 40)
                | ((a[offset + 4] & 0xffL) << 32)
                | ((a[offset + 3] & 0xffL) << 24)
                | ((a[offset + 2] & 0xffL) << 16)
                | ((a[offset + 1] & 0xffL) << 8)
                | (a[offset] & 0xffL);
    }

    /**
     * Tries to parse eight decimal digits from a char array using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param a      contains 8 utf-16 characters starting at offset
     * @param offset the offset into the array
     * @return the parsed number,
     * returns a negative value if {@code value} does not contain 8 hex digits
     * @throws IndexOutOfBoundsException if offset is larger than 2^
     */

    public static int tryToParseEightDigits(char[] a, int offset) {
        long first = a[offset]
                | (long) a[offset + 1] << 16
                | (long) a[offset + 2] << 32
                | (long) a[offset + 3] << 48;
        long second = a[offset + 4]
                | (long) a[offset + 5] << 16
                | (long) a[offset + 6] << 32
                | (long) a[offset + 7] << 48;
        return FastDoubleSwar.tryToParseEightDigitsUtf16(first, second);
    }

    public static int tryToParseEightDigits(byte[] a, int offset) {
        return FastDoubleSwar.tryToParseEightDigitsUtf8(readLongLE(a, offset));
    }


    /**
     * Tries to parse eight digits at once using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param str    a character sequence
     * @param offset the index of the first character in the character sequence
     * @return the parsed digits or -1
     */
    public static int tryToParseEightDigits(CharSequence str, int offset) {
        long first = str.charAt(offset)
                | (long) str.charAt(offset + 1) << 16
                | (long) str.charAt(offset + 2) << 32
                | (long) str.charAt(offset + 3) << 48;
        long second = str.charAt(offset + 4)
                | (long) str.charAt(offset + 5) << 16
                | (long) str.charAt(offset + 6) << 32
                | (long) str.charAt(offset + 7) << 48;
        return FastDoubleSwar.tryToParseEightDigitsUtf16(first, second);
    }

    /**
     * Tries to parse eight decimal digits at once using the
     * 'SIMD within a register technique' (SWAR).
     *
     * <pre>{@literal
     * char[] chars = ...;
     * long first  = chars[0]|(chars[1]<<16)|(chars[2]<<32)|(chars[3]<<48);
     * long second = chars[4]|(chars[5]<<16)|(chars[6]<<32)|(chars[7]<<48);
     * }</pre>
     *
     * @param first  the first four characters in big endian order
     * @param second the second four characters in big endian order
     * @return the parsed digits or -1
     */
    public static int tryToParseEightDigitsUtf16(long first, long second) {
        long fval = first - 0x0030_0030_0030_0030L;
        long sval = second - 0x0030_0030_0030_0030L;

        // Create a predicate for all bytes which are smaller than '0' (0x0030)
        // or greater than '9' (0x0039).
        // We have 0x007f - 0x0039 = 0x0046.
        // The predicate is true if the hsb of a byte is set: (predicate & 0xff80) != 0.
        long fpre = first + 0x0046_0046_0046_0046L | fval;
        long spre = second + 0x0046_0046_0046_0046L | sval;
        if (((fpre | spre) & 0xff80_ff80_ff80_ff80L) != 0L) {
            return -1;
        }

        return (int) (sval * 0x03e8_0064_000a_0001L >>> 48)
                + (int) (fval * 0x03e8_0064_000a_0001L >>> 48) * 10000;
    }

    /**
     * Tries to parse eight decimal digits from a byte array using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param a      contains 8 ascii characters
     * @param offset the offset of the first character in {@code a}
     * @return the parsed number,
     * returns a negative value if {@code value} does not contain 8 digits
     */
    public static int tryToParseEightDigitsUtf8(byte[] a, int offset) {
        return tryToParseEightDigitsUtf8((long) readLongLE(a, offset));
    }

    /**
     * Tries to parse eight digits from a long using the
     * 'SIMD within a register technique' (SWAR).
     *
     * <pre>{@literal
     * byte[] bytes = ...;
     * long value  = ((bytes[7]&0xffL)<<56)
     *             | ((bytes[6]&0xffL)<<48)
     *             | ((bytes[5]&0xffL)<<40)
     *             | ((bytes[4]&0xffL)<<32)
     *             | ((bytes[3]&0xffL)<<24)
     *             | ((bytes[2]&0xffL)<<16)
     *             | ((bytes[1]&0xffL)<< 8)
     *             |  (bytes[0]&0xffL);
     * }</pre>
     *
     * @param chunk contains 8 ascii characters in little endian order
     * @return the parsed number, or a value &lt; 0 if not all characters are
     * digits.
     */
    public static int tryToParseEightDigitsUtf8(long chunk) {
        // Subtract the character '0' from all characters.
        long val = chunk - 0x3030303030303030L;

        // Create a predicate for all bytes which are greater than '0' (0x30).
        // The predicate is true if the hsb of a byte is set: (predicate & 0x80) != 0.
        long predicate = ((chunk + 0x4646464646464646L) | val) & 0x8080808080808080L;
        if (predicate != 0L) {
            return -1;
        }

        // The last 2 multiplications are independent of each other.
        long mask = 0xff_000000ffL;
        long mul1 = 100 + (100_0000L << 32);
        long mul2 = 1 + (1_0000L << 32);
        val = val * 10 + (val >>> 8);// same as: val = val * (1 + (10 << 8)) >>> 8;
        val = (val & mask) * mul1 + (val >>> 16 & mask) * mul2 >>> 32;
        return (int) val;
    }

    /**
     * Tries to parse eight digits at once using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param str    a character sequence
     * @param offset the index of the first character in the character sequence
     * @return the parsed digits or -1
     */
    public static long tryToParseEightHexDigits(CharSequence str, int offset) {
        long first = (long) str.charAt(offset) << 48
                | (long) str.charAt(offset + 1) << 32
                | (long) str.charAt(offset + 2) << 16
                | (long) str.charAt(offset + 3);

        long second = (long) str.charAt(offset + 4) << 48
                | (long) str.charAt(offset + 5) << 32
                | (long) str.charAt(offset + 6) << 16
                | (long) str.charAt(offset + 7);

        return FastDoubleSwar.tryToParseEightHexDigitsUtf16(first, second);
    }

    /**
     * Tries to parse eight hex digits from a char array using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param chars  contains 8 utf-16 characters starting at offset
     * @param offset the offset into the array
     * @return the parsed number,
     * returns a negative value if {@code value} does not contain 8 hex digits
     */
    public static long tryToParseEightHexDigits(char[] chars, int offset) {
        // Performance: We extract the chars in two steps so that we
        //              can benefit from out of order execution in the CPU.
        long first = (long) chars[offset] << 48
                | (long) chars[offset + 1] << 32
                | (long) chars[offset + 2] << 16
                | (long) chars[offset + 3];

        long second = (long) chars[offset + 4] << 48
                | (long) chars[offset + 5] << 32
                | (long) chars[offset + 6] << 16
                | (long) chars[offset + 7];

        return FastDoubleSwar.tryToParseEightHexDigitsUtf16(first, second);
    }

    /**
     * Tries to parse eight hex digits from a byte array using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param a      contains 8 ascii characters
     * @param offset the offset of the first character in {@code a}
     *               returns a negative value if {@code value} does not contain 8 digits
     */
    public static long tryToParseEightHexDigits(byte[] a, int offset) {
        return tryToParseEightHexDigitsUtf8((long) readLongBE(a, offset));
    }

    /**
     * Tries to parse eight hex digits from two longs using the
     * 'SIMD within a register technique' (SWAR).
     *
     * <pre>{@code
     * char[] chars = ...;
     * long first  = (long) chars[0] << 48
     *             | (long) chars[1] << 32
     *             | (long) chars[2] << 16
     *             | (long) chars[3];
     *
     * long second = (long) chars[4] << 48
     *             | (long) chars[5] << 32
     *             | (long) chars[6] << 16
     *             | (long) chars[7];
     * }</pre>
     *
     * @param first  contains 4 utf-16 characters in big endian order
     * @param second contains 4 utf-16 characters in big endian order
     * @return the parsed number,
     * returns a negative value if the two longs do not contain 8 hex digits
     */
    public static long tryToParseEightHexDigitsUtf16(long first, long second) {
        if (((first | second) & 0xff00_ff00_ff00_ff00L) != 0) {
            return -1;
        }
        long f = first * 0x0000_0000_0001_0100L;
        long s = second * 0x0000_0000_0001_0100L;
        long utf8Bytes = (f & 0xffff_0000_0000_0000L)
                | ((f & 0xffff_0000L) << 16)
                | ((s & 0xffff_0000_0000_0000L) >>> 32)
                | ((s & 0xffff_0000L) >>> 16);
        return tryToParseEightHexDigitsUtf8(utf8Bytes);
    }

    /**
     * Tries to parse eight digits from a long using the
     * 'SIMD within a register technique' (SWAR).
     *
     * @param chunk contains 8 ascii characters in big endian order
     * @return the parsed number,
     * returns a negative value if {@code value} does not contain 8 digits
     */
    public static long tryToParseEightHexDigitsUtf8(long chunk) {
        // The following code is based on the technique presented in the paper
        // by Leslie Lamport.

        // The predicates are true if the hsb of a byte is set.

        // Create a predicate for all bytes which are less than '0'
        long lt_0 = chunk - 0x30_30_30_30_30_30_30_30L;
        lt_0 &= 0x80_80_80_80_80_80_80_80L;

        // Create a predicate for all bytes which are greater than '9'
        long gt_9 = chunk + (0x39_39_39_39_39_39_39_39L ^ 0x7f_7f_7f_7f_7f_7f_7f_7fL);
        gt_9 &= 0x80_80_80_80_80_80_80_80L;

        // We can convert upper case characters to lower case by setting the 0x20 bit.
        // (This does not have an impact on decimal digits, which is very handy!).
        // Subtract character '0' (0x30) from each of the eight characters
        long vec = (chunk | 0x20_20_20_20_20_20_20_20L) - 0x30_30_30_30_30_30_30_30L;

        // Create a predicate for all bytes which are greater or equal than 'a'-'0' (0x30).
        long ge_a = vec + (0x30_30_30_30_30_30_30_30L ^ 0x7f_7f_7f_7f_7f_7f_7f_7fL);
        ge_a &= 0x80_80_80_80_80_80_80_80L;

        // Create a predicate for all bytes which are less or equal than 'f'-'0' (0x37).
        long le_f = vec - 0x37_37_37_37_37_37_37_37L;
        // we don't need to 'and' with 0x80…L here, because we 'and' this with ge_a anyway.
        //le_f &= 0x80_80_80_80_80_80_80_80L;

        // If a character is less than '0' or greater than '9' then it must be greater or equal than 'a' and less or equal then 'f'.
        if (((lt_0 | gt_9) != (ge_a & le_f))) {
            return -1;
        }

        // Expand the predicate to a byte mask
        long gt_9mask = (gt_9 >>> 7) * 0xffL;

        // Subtract 'a'-'0'+10 (0x27) from all bytes that are greater than 0x09.
        long v = vec & ~gt_9mask | vec - (0x27272727_27272727L & gt_9mask);

        // Compact all nibbles
        //return Long.compress(v, 0x0f0f0f0f_0f0f0f0fL);// since Java 19, Long.comporess is faster on Intel x64 but slower on Apple Silicon
        long v2 = v | v >>> 4;
        long v3 = v2 & 0x00ff00ff_00ff00ffL;
        long v4 = v3 | v3 >>> 8;
        long v5 = ((v4 >>> 16) & 0xffff_0000L) | v4 & 0xffffL;
        return v5;
    }

    public static int tryToParseFourDigits(char[] a, int offset) {
        long first = a[offset]
                | (long) a[offset + 1] << 16
                | (long) a[offset + 2] << 32
                | (long) a[offset + 3] << 48;
        return FastDoubleSwar.tryToParseFourDigitsUtf16(first);
    }

    public static int tryToParseFourDigits(CharSequence str, int offset) {
        long first = str.charAt(offset)
                | (long) str.charAt(offset + 1) << 16
                | (long) str.charAt(offset + 2) << 32
                | (long) str.charAt(offset + 3) << 48;

        return FastDoubleSwar.tryToParseFourDigitsUtf16(first);
    }

    public static int tryToParseFourDigits(byte[] a, int offset) {
        return tryToParseFourDigitsUtf8((int) readIntLE(a, offset));
    }

    public static int tryToParseFourDigitsUtf16(long first) {
        long fval = first - 0x0030_0030_0030_0030L;

        // Create a predicate for all bytes which are smaller than '0' (0x0030)
        // or greater than '9' (0x0039).
        // We have 0x007f - 0x0039 = 0x0046.
        // The predicate is true if the hsb of a byte is set: (predicate & 0xff80) != 0.
        long fpre = first + 0x0046_0046_0046_0046L | fval;
        if ((fpre & 0xff80_ff80_ff80_ff80L) != 0L) {
            return -1;
        }

        return (int) (fval * 0x03e8_0064_000a_0001L >>> 48);
    }

    public static int tryToParseFourDigitsUtf8(int chunk) {
        // Create a predicate for all bytes which are greater than '0' (0x30).
        // The predicate is true if the hsb of a byte is set: (predicate & 0x80) != 0.
        int val = chunk - 0x30303030;
        int predicate = ((chunk + 0x46464646) | val) & 0x80808080;
        if (predicate != 0L) {
            return -1;//~(Integer.numberOfTrailingZeros(predicate)>>3);
        }

        // The last 2 multiplications are independent of each other.
        val = val * (1 + (10 << 8)) >>> 8;
        val = (val & 0xff) * 100 + ((val & 0xff0000) >> 16);
        return val;
    }

    public static int tryToParseUpTo7Digits(byte[] str, int from, int to) {
        int result = 0;
        boolean success = true;
        for (; from < to; from++) {
            byte ch = str[from];
            int digit = (char) (ch - '0');
            success &= digit < 10;
            result = 10 * (result) + digit;
        }
        return success ? result : -1;
    }

    public static int tryToParseUpTo7Digits(char[] str, int from, int to) {
        int result = 0;
        boolean success = true;
        for (; from < to; from++) {
            char ch = str[from];
            int digit = (char) (ch - '0');
            success &= digit < 10;
            result = 10 * (result) + digit;
        }
        return success ? result : -1;
    }

    public static int tryToParseUpTo7Digits(CharSequence str, int from, int to) {
        int result = 0;
        boolean success = true;
        for (; from < to; from++) {
            char ch = str.charAt(from);
            int digit = (char) (ch - '0');
            success &= digit < 10;
            result = 10 * (result) + digit;
        }
        return success ? result : -1;
    }

    public static void writeIntBE(byte[] a, int offset, int v) {
        a[offset] = (byte) (v >>> 24);
        a[offset + 1] = (byte) (v >>> 16);
        a[offset + 2] = (byte) (v >>> 8);
        a[offset + 3] = (byte) v;
    }

    public static void writeLongBE(byte[] a, int offset, long v) {
        a[offset] = (byte) (v >>> 56);
        a[offset + 1] = (byte) (v >>> 48);
        a[offset + 2] = (byte) (v >>> 40);
        a[offset + 3] = (byte) (v >>> 32);
        a[offset + 4] = (byte) (v >>> 24);
        a[offset + 5] = (byte) (v >>> 16);
        a[offset + 6] = (byte) (v >>> 8);
        a[offset + 7] = (byte) v;
    }

    public static double fma(double a, double b, double c) {
        return a * b + c;
    }
}