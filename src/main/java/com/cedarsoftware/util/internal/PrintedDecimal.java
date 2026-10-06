package com.cedarsoftware.util.internal;

import java.math.BigDecimal;

import com.cedarsoftware.util.SystemUtilities;

/**
 * What FloatDecimal and DoubleDecimal share: a decimal {@code digits 10^exponent} (digits above zero, trailing zeros
 * allowed) laid out as Float.toString and Double.toString print it from JDK 19 on, made a BigDecimal of the scale
 * {@code new BigDecimal(Double.toString(d))} has, or truncated to a long; and the 128-bit multiply Java 8 lacks.
 * <p>
 * The layout: plain notation from 10^-3 up to 10^7 ("123.0", "0.00123", "1234567.0"), computerized scientific
 * notation otherwise ("1.0E7", "1.234E-5"), at least one digit after the point either way.
 *
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
final class PrintedDecimal {
    /** The most characters a double prints: "-2.2250738585072014E-308". */
    static final int MAX_CHARS = 24;

    private static final long MASK_32 = (1L << 32) - 1;

    static final long[] POW10 = {1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L, 10_000_000L,
            100_000_000L, 1_000_000_000L, 10_000_000_000L, 100_000_000_000L, 1_000_000_000_000L,
            10_000_000_000_000L, 100_000_000_000_000L, 1_000_000_000_000_000L, 10_000_000_000_000_000L,
            100_000_000_000_000_000L, 1_000_000_000_000_000_000L};

    /**
     * Double.toString and Float.toString print this text from JDK 19 on, and faster there than laying it out here
     * and copying it into a String (3-9 ns on JDK 21): toString hands it to them.
     */
    static final boolean JDK_PRINTS_IT = SystemUtilities.currentJdkMajorVersion() >= 19;

    /**
     * A buffer per thread for toString before JDK 19: writing into a new array and copying it into a String at once
     * cost 25-30 ns more than either step alone (JDK 8 and 21), so the text is laid out in a reused one.
     */
    private static final ThreadLocal<char[]> BUFFER = ThreadLocal.withInitial(() -> new char[MAX_CHARS]);

    private PrintedDecimal() {
    }

    /** This thread's buffer for laying out a decimal: {@link #MAX_CHARS} long, overwritten by the next call. */
    static char[] buffer() {
        return BUFFER.get();
    }

    /**
     * Lay out {@code digits 10^exponent} as Double.toString does from JDK 19 on, from {@code buf[off]}, which needs
     * room for {@link #MAX_CHARS}. Returns the index after the last character.
     */
    static int toChars(boolean negative, long digits, int exponent, char[] buf, int off) {
        if (negative) {
            buf[off++] = '-';
        }
        int len = length(digits);
        int point = exponent + len;                                 // the value is 0.digits 10^point
        if (point > 0 && point <= 7) {                              // plain, with a whole part
            writeDigits(digits, buf, off, len);
            if (len <= point) {                                     // "1200.0"
                for (int i = off + len; i < off + point; i++) {
                    buf[i] = '0';
                }
                buf[off + point] = '.';
                buf[off + point + 1] = '0';
                return off + point + 2;
            }
            System.arraycopy(buf, off + point, buf, off + point + 1, len - point);    // "12.34"
            buf[off + point] = '.';
            return trimZeros(buf, off + len + 1, off + point + 2);
        }
        if (point > -3 && point <= 0) {                             // plain, below 1: "0.00123"
            buf[off] = '0';
            buf[off + 1] = '.';
            int start = off + 2;
            for (int i = 0; i < -point; i++) {
                buf[start++] = '0';
            }
            writeDigits(digits, buf, start, len);
            return trimZeros(buf, start + len, start + 1);
        }
        writeDigits(digits, buf, off + 1, len);                     // scientific: "1.234E-5"
        buf[off] = buf[off + 1];
        buf[off + 1] = '.';
        int end;
        if (len == 1) {
            buf[off + 2] = '0';
            end = off + 3;
        } else {
            end = trimZeros(buf, off + len + 1, off + 3);
        }
        buf[end++] = 'E';
        int e = point - 1;
        if (e < 0) {
            buf[end++] = '-';
            e = -e;
        }
        if (e >= 100) {
            buf[end++] = (char) ('0' + e / 100);
            e %= 100;
            buf[end++] = (char) ('0' + e / 10);
        } else if (e >= 10) {
            buf[end++] = (char) ('0' + e / 10);
        }
        buf[end++] = (char) ('0' + e % 10);
        return end;
    }

    /** The end after dropping trailing zeros from buf[..end), keeping at least up to {@code keep}. */
    private static int trimZeros(char[] buf, int end, int keep) {
        while (end > keep && buf[end - 1] == '0') {
            end--;
        }
        return end;
    }

    /** Write the {@code len} digits of {@code digits} at buf[off..off+len). */
    private static void writeDigits(long digits, char[] buf, int off, int len) {
        int i = off + len;
        while (digits >= 100_000_000L) {                            // eight at a time in int arithmetic
            long q = digits / 100_000_000L;
            int r = (int) (digits - q * 100_000_000L);
            for (int j = 0; j < 8; j++) {
                int rq = r / 10;
                buf[--i] = (char) ('0' + r - rq * 10);
                r = rq;
            }
            digits = q;
        }
        int d = (int) digits;
        while (i > off) {
            int dq = d / 10;
            buf[--i] = (char) ('0' + d - dq * 10);
            d = dq;
        }
    }

    /** The number of decimal digits of a positive long. */
    static int length(long digits) {
        int len = flog10pow2(64 - Long.numberOfLeadingZeros(digits));
        return digits >= POW10[len] ? len + 1 : len;
    }

    /**
     * {@code digits 10^exponent} as a BigDecimal of the scale {@code new BigDecimal(Double.toString(d))} gives it:
     * plain notation (10^-3 up to 10^7) shows at least one fraction digit ("3.0" is 30 with scale 1), scientific
     * notation at least two significant digits ("1.0E10" is 10 with scale -9).
     */
    static BigDecimal toBigDecimal(boolean negative, long digits, int exponent) {
        while (true) {                                              // drop trailing zeros
            long q = digits / 10;
            if (q * 10 != digits) {
                break;
            }
            digits = q;
            exponent++;
        }
        int len = length(digits);
        int leading = exponent + len - 1;                           // the decimal exponent of the leading digit
        long unscaled = digits;
        int scale = -exponent;
        if (leading >= -3 && leading < 7) {
            if (scale < 1) {
                unscaled = digits * POW10[1 - scale];               // below 10^8
                scale = 1;
            }
        } else if (len < 2) {
            unscaled = digits * 10;
            scale++;
        }
        return BigDecimal.valueOf(negative ? -unscaled : unscaled, scale);
    }

    /** {@code digits 10^exponent} truncated toward zero, for a decimal below 2^63 in magnitude. */
    static long toLong(boolean negative, long digits, int exponent) {
        long magnitude;
        if (exponent >= 0) {
            magnitude = digits * POW10[exponent];
        } else {
            magnitude = -exponent < POW10.length ? digits / POW10[-exponent] : 0L;
        }
        return negative ? -magnitude : magnitude;
    }

    /** floor(e log10(2)) */
    static int flog10pow2(int e) {
        return (int) (e * 661_971_961_083L >> 41);
    }

    /** floor(e log10(2) + log10(3/4)) */
    static int flog10threeQuartersPow2(int e) {
        return (int) (e * 661_971_961_083L - 274_743_187_321L >> 41);
    }

    /** The high 64 bits of the 128-bit product x * y (Math.multiplyHigh, which Java 8 lacks). */
    static long multiplyHigh(long x, long y) {
        long x1 = x >> 32;
        long x2 = x & MASK_32;
        long y1 = y >> 32;
        long y2 = y & MASK_32;
        long z2 = x2 * y2;
        long t = x1 * y2 + (z2 >>> 32);
        long z1 = t & MASK_32;
        long z0 = t >> 32;
        z1 += x2 * y1;
        return x1 * y1 + z0 + (z1 >> 32);
    }
}
