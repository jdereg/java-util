package com.cedarsoftware.util.internal;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The decimal a double prints, on every JDK: the shortest decimal that reads back as the double (the one closest to
 * it when several are that short, and never fewer than two digits when a two-digit one is closer), which is what
 * {@link Double#toString(double)} prints from JDK 19 on. JDK 8 through 18 print a longer or a different decimal for
 * many doubles (most whole doubles above 2^53: 4.879634314335111E18 printed 4.8796343143351112E18, 2^60
 * 1.15292150460684698E18; 2.0E-3 printed 0.0020); this class gives the JDK 19 decimal everywhere, as text, as a
 * BigDecimal, or truncated to a long.
 * <p>
 * It implements the Schubfach algorithm of Raffaello Giulietti ("The Schubfach way to render doubles", 2020), which
 * the JDK's Double.toString uses from JDK 19 on, written from the paper as FloatDecimal is for floats. Its table of
 * powers of ten is derived at class load with BigInteger. It was checked against Double.toString on JDK 21 for
 * about 7 billion doubles (random bit patterns, every exponent, decimal-like values and their neighbors, whole
 * doubles above 2^53), with no difference in the text, the BigDecimal (value and scale) or the long. On JDK 19+
 * toString hands the double to Double.toString, which prints the same text faster there.
 * <p>
 * Converter uses it for Double to every number type and to String, MultiKeyMap for a fractional double's decimal,
 * and json-io to write doubles. Internal: not part of java-util's API (the package is exported to json-io only).
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
public final class DoubleDecimal {
    /** The most characters a double prints ("-2.2250738585072014E-308"): the room toChars needs. */
    public static final int MAX_CHARS = PrintedDecimal.MAX_CHARS;

    private static final int P = 53;                        // a double's precision, the hidden bit included
    private static final int Q_MIN = -1074;                 // the least binary exponent of a double's last bit
    private static final long C_MIN = 1L << (P - 1);        // the least normal significand
    private static final long C_TINY = 3;                   // below this, a subnormal significand gets a digit more
    private static final int K_MIN = -324;                  // the decimal exponents the doubles need
    private static final int K_MAX = 292;
    private static final long MASK_63 = Long.MAX_VALUE;

    /**
     * G[2(k - K_MIN)] and G[2(k - K_MIN) + 1]: g1 and g0 of g = floor(beta) + 1 = g1 2^63 + g0, where
     * 10^-k = beta 2^r and 2^125 <= beta < 2^126.
     */
    private static final long[] G = new long[2 * (K_MAX - K_MIN + 1)];
    /** FLOG2_POW10[k - K_MIN]: floor(log2(10^-k)), that is r + 125. */
    private static final int[] FLOG2_POW10 = new int[K_MAX - K_MIN + 1];

    static {
        BigInteger two125 = BigInteger.ONE.shiftLeft(125);
        BigInteger two126 = BigInteger.ONE.shiftLeft(126);
        BigInteger mask63 = BigInteger.valueOf(MASK_63);
        BigInteger pow10 = BigInteger.ONE;                  // 10^j
        for (int j = 0; j <= -K_MIN; j++) {
            if (j > 0) {
                pow10 = pow10.multiply(BigInteger.TEN);
            }
            int b = pow10.bitLength();
            int r = b - 126;                                // k = -j: 10^j = beta 2^r
            BigInteger floorBeta = r >= 0 ? pow10.shiftRight(r) : pow10.shiftLeft(-r);
            set(-j, floorBeta, r, two125, two126, mask63);
            if (j > 0 && j <= K_MAX) {                      // k = j: 10^-j = beta 2^-(125 + b), as 2^(b-1) < 10^j
                floorBeta = BigInteger.ONE.shiftLeft(125 + b).divide(pow10);
                set(j, floorBeta, -(125 + b), two125, two126, mask63);
            }
        }
    }

    private static void set(int k, BigInteger floorBeta, int r, BigInteger two125, BigInteger two126,
                            BigInteger mask63) {
        if (floorBeta.compareTo(two125) < 0 || floorBeta.compareTo(two126) >= 0) {
            throw new IllegalStateException("10^" + (-k) + " scaled out of range");
        }
        BigInteger g = floorBeta.add(BigInteger.ONE);
        long g1 = g.shiftRight(63).longValue();
        if (g1 < 0) {
            throw new IllegalStateException("10^" + (-k) + " scaled to 2^126");
        }
        G[2 * (k - K_MIN)] = g1;
        G[2 * (k - K_MIN) + 1] = g.and(mask63).longValue();
        FLOG2_POW10[k - K_MIN] = r + 125;
    }

    private DoubleDecimal() {
    }

    /** What Double.toString prints from JDK 19 on, on every JDK: 2.0E-3 is "0.002", 1.0E23 is "1.0E23". */
    public static String toString(double v) {
        if (PrintedDecimal.JDK_PRINTS_IT) {
            return Double.toString(v);
        }
        char[] buf = PrintedDecimal.buffer();
        return new String(buf, 0, toChars(v, buf, 0));
    }

    /**
     * Write what Double.toString prints from JDK 19 on from {@code buf[off]}, which needs room for
     * {@link #MAX_CHARS}. Returns the index after the last character.
     */
    public static int toChars(double v, char[] buf, int off) {
        long bits = Double.doubleToRawLongBits(v);
        long magnitude = bits & MASK_63;
        if (magnitude >= 0x7FF0000000000000L) {                     // infinite or NaN
            String special = magnitude > 0x7FF0000000000000L ? "NaN" : bits < 0 ? "-Infinity" : "Infinity";
            special.getChars(0, special.length(), buf, off);
            return off + special.length();
        }
        if (magnitude == 0) {
            if (bits < 0) {
                buf[off++] = '-';
            }
            buf[off] = '0';
            buf[off + 1] = '.';
            buf[off + 2] = '0';
            return off + 3;
        }
        return PrintedDecimal.toChars(bits < 0, digits(magnitude), exponent(magnitude), buf, off);
    }

    /**
     * The decimal a double prints (what Double.toString prints from JDK 19 on), exactly, with the scale
     * {@code new BigDecimal(Double.toString(v))} has there: 1.0E23 is 1.0E+23 and 0.1 is 0.1. Zero is 0.0. For a
     * finite double.
     */
    public static BigDecimal printedDecimal(double v) {
        long bits = Double.doubleToRawLongBits(v);
        long magnitude = bits & MASK_63;
        if (magnitude == 0) {
            return BigDecimal.valueOf(0, 1);                        // "0.0"
        }
        return PrintedDecimal.toBigDecimal(bits < 0, digits(magnitude), exponent(magnitude));
    }

    /**
     * The decimal a double prints, truncated toward zero, as a long, without a BigDecimal: printedDecimal(v) as a
     * long. For a finite double strictly between -2^63 and 2^63 (-2^63 prints -9.223372036854776E18, beyond a long).
     * Below 2^53 in magnitude it is the cast: a whole double there is exact, and a fraction truncates as its decimal
     * does.
     */
    public static long printedLong(double v) {
        if (v > -0x1p53 && v < 0x1p53) {
            return (long) v;
        }
        long bits = Double.doubleToRawLongBits(v);
        long magnitude = bits & MASK_63;
        return PrintedDecimal.toLong(bits < 0, digits(magnitude), exponent(magnitude));
    }

    /**
     * The digits of the decimal a positive, finite double of these bits prints: the decimal is
     * digits 10^exponent(bits). Trailing zeros may remain.
     */
    static long digits(long bits) {
        long t = bits & (C_MIN - 1);
        int bq = (int) (bits >>> (P - 1));
        if (bq == 0) {                                              // subnormal
            return t < C_TINY ? decimal(Q_MIN, 10 * t) : decimal(Q_MIN, t);
        }
        int q = bq + Q_MIN - 1;
        long c = C_MIN | t;
        if (q < 0 && q > -P && (c & ((1L << -q) - 1)) == 0) {
            return c >> -q;                                         // a whole number below 2^53: itself
        }
        return decimal(q, c);
    }

    /** The decimal exponent that goes with {@link #digits(long)}. */
    static int exponent(long bits) {
        long t = bits & (C_MIN - 1);
        int bq = (int) (bits >>> (P - 1));
        if (bq == 0) {
            return t < C_TINY ? PrintedDecimal.flog10pow2(Q_MIN) - 1 : PrintedDecimal.flog10pow2(Q_MIN);
        }
        int q = bq + Q_MIN - 1;
        long c = C_MIN | t;
        if (q < 0 && q > -P && (c & ((1L << -q) - 1)) == 0) {
            return 0;
        }
        return c != C_MIN || q == Q_MIN
                ? PrintedDecimal.flog10pow2(q) : PrintedDecimal.flog10threeQuartersPow2(q);
    }

    /**
     * Schubfach for the double c 2^q: the digits of the shortest decimal in its rounding interval, the closest when
     * two are, whose exponent is floor(log10(2^q)) (floor(log10(3/4 2^q)) below a power of 2).
     */
    private static long decimal(int q, long c) {
        int out = (int) c & 1;
        long cb = c << 2;
        long cbr = cb + 2;
        long cbl;
        int k;
        if (c != C_MIN || q == Q_MIN) {
            cbl = cb - 2;
            k = PrintedDecimal.flog10pow2(q);
        } else {
            cbl = cb - 1;                                           // the interval is narrower below a power of 2
            k = PrintedDecimal.flog10threeQuartersPow2(q);
        }
        int h = q + FLOG2_POW10[k - K_MIN] + 2;
        int i = (k - K_MIN) << 1;
        long g1 = G[i];
        long g0 = G[i + 1];
        long vb = rop(g1, g0, cb << h);
        long vbl = rop(g1, g0, cbl << h);
        long vbr = rop(g1, g0, cbr << h);
        long s = vb >> 2;
        if (s >= 100) {
            long sp10 = 10 * (s / 10);                              // s rounded down to a multiple of 10
            long tp10 = sp10 + 10;
            boolean upin = vbl + out <= sp10 << 2;
            boolean wpin = (tp10 << 2) + out <= vbr;
            if (upin != wpin) {
                return upin ? sp10 : tp10;
            }
        }
        long t = s + 1;
        boolean uin = vbl + out <= s << 2;
        boolean win = (t << 2) + out <= vbr;
        if (uin != win) {
            return uin ? s : t;
        }
        long cmp = vb - ((s + t) << 1);
        return cmp < 0 || cmp == 0 && (s & 1) == 0 ? s : t;
    }

    /** Round to odd of cp g 2^-127, where g = g1 2^63 + g0. */
    private static long rop(long g1, long g0, long cp) {
        long x1 = PrintedDecimal.multiplyHigh(g0, cp);
        long y0 = g1 * cp;
        long y1 = PrintedDecimal.multiplyHigh(g1, cp);
        long z = (y0 >>> 1) + x1;
        long vbp = y1 + (z >>> 63);
        return vbp | ((z & MASK_63) + MASK_63) >>> 63;
    }
}
