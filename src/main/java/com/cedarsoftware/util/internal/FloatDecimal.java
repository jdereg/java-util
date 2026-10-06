package com.cedarsoftware.util.internal;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.cedarsoftware.util.fastdoubleparser.FastDoubleMath;

/**
 * The decimal a float prints, as a double, without printing it: the shortest decimal that reads back as the float
 * (the one closest to it when several are that short, and never fewer than two digits when a two-digit one is
 * closer), which is what {@link Float#toString(float)} prints from JDK 19 on. Older JDKs print a longer decimal for
 * a few floats (19 positive ones, powers of two and subnormals: 1.0E-43 where JDK 19 prints 9.9E-44); this class
 * gives the JDK 19 decimal on every JDK.
 * <p>
 * It implements the Schubfach algorithm of Raffaello Giulietti ("The Schubfach way to render doubles", 2020), which
 * the JDK's Float.toString uses from JDK 19 on, for floats only. Its table of powers of ten is derived at class load
 * with BigInteger. Every one of the 1,249,902,592 positive floats with a fraction was checked against
 * {@code Double.parseDouble(Float.toString(f))} on JDK 21: the same double for each, and for its negation.
 * <p>
 * The decimal becomes a double through fastdoubleparser's FastDoubleMath (Clinger's fast path, else
 * Eisel-Lemire), correctly rounded with no text: every one of the 645,922,816 positive floats whose decimal is
 * beyond Clinger's reach (exponent below -22) was checked against Double.parseDouble of the decimal on JDK 21 and
 * JDK 8, with no difference, for it and its negation.
 * <p>
 * It also gives that decimal as text (on JDK 19+ from Float.toString, which prints the same), as a BigDecimal and
 * as a long. MultiKeyMap uses it for a float's value in value-based equality (0.1f is the 0.1 it prints), Converter
 * for Float to every number type and to String, and json-io to write floats. Internal: not part of java-util's API
 * (the package is exported to json-io only). Printing with Float.toString and parsing that cost 35-43 ns per float
 * on JDK 21 (52-54 on JDK 8); this takes about 7-8, and 10 for a float below about 1e-14.
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
public final class FloatDecimal {
    private static final int P = 24;                        // a float's precision, the hidden bit included
    private static final int Q_MIN = -149;                  // the least binary exponent of a float's last bit
    private static final int C_MIN = 1 << (P - 1);          // the least normal significand
    private static final int C_TINY = 8;                    // below this, a subnormal significand gets a digit more
    private static final int K_MIN = -45;                   // the decimal exponents the floats need
    private static final int K_MAX = 31;
    private static final long MASK_32 = (1L << 32) - 1;

    /** G1[k - K_MIN]: the top 63 bits of floor(beta) + 1, where 10^-k = beta 2^r and 2^125 <= beta < 2^126. */
    private static final long[] G1 = new long[K_MAX - K_MIN + 1];
    /** FLOG2_POW10[k - K_MIN]: floor(log2(10^-k)), that is r + 125. */
    private static final int[] FLOG2_POW10 = new int[K_MAX - K_MIN + 1];

    static {
        BigInteger two125 = BigInteger.ONE.shiftLeft(125);
        BigInteger two126 = BigInteger.ONE.shiftLeft(126);
        for (int k = K_MIN; k <= K_MAX; k++) {
            BigInteger floorBeta;
            int r;
            if (k <= 0) {
                BigInteger n = BigInteger.TEN.pow(-k);
                r = n.bitLength() - 126;
                floorBeta = r >= 0 ? n.shiftRight(r) : n.shiftLeft(-r);
            } else {
                BigInteger d = BigInteger.TEN.pow(k);
                int e = 125 + d.bitLength() - 1;
                BigInteger q = BigInteger.ONE.shiftLeft(e).divide(d);
                while (q.compareTo(two125) < 0) {
                    e++;
                    q = BigInteger.ONE.shiftLeft(e).divide(d);
                }
                while (q.compareTo(two126) >= 0) {
                    e--;
                    q = BigInteger.ONE.shiftLeft(e).divide(d);
                }
                floorBeta = q;
                r = -e;
            }
            if (floorBeta.compareTo(two125) < 0 || floorBeta.compareTo(two126) >= 0) {
                throw new IllegalStateException("10^" + (-k) + " scaled out of range");
            }
            G1[k - K_MIN] = floorBeta.add(BigInteger.ONE).shiftRight(63).longValue();
            FLOG2_POW10[k - K_MIN] = r + 125;
        }
    }

    private FloatDecimal() {
    }

    /**
     * The double nearest the decimal a float prints (what Float.toString prints from JDK 19 on), for every float:
     * 0.1f is 0.1, 3.7f is 3.7 and Float.MAX_VALUE is 3.4028235E38. Zeros, infinities and NaN are themselves.
     */
    public static double printedValue(float f) {
        double w = f;
        if (w == 0 || !Double.isFinite(w)) {
            return w;
        }
        return valueOf(f);
    }

    /** What Float.toString prints from JDK 19 on, on every JDK: 1.0E-43 printed on JDK 8 is "9.9E-44". */
    public static String toString(float f) {
        if (PrintedDecimal.JDK_PRINTS_IT) {
            return Float.toString(f);
        }
        char[] buf = PrintedDecimal.buffer();
        return new String(buf, 0, toChars(f, buf, 0));
    }

    /**
     * Write what Float.toString prints from JDK 19 on from {@code buf[off]}, which needs room for
     * {@link DoubleDecimal#MAX_CHARS}. Returns the index after the last character.
     */
    public static int toChars(float f, char[] buf, int off) {
        int bits = Float.floatToRawIntBits(f);
        int magnitude = bits & 0x7FFFFFFF;
        if (magnitude >= 0x7F800000) {                              // infinite or NaN
            String special = magnitude > 0x7F800000 ? "NaN" : bits < 0 ? "-Infinity" : "Infinity";
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
        long decimal = decimal(f);
        return PrintedDecimal.toChars(bits < 0, decimal >>> 8, (int) (decimal & 0xFF) - 128, buf, off);
    }

    /**
     * The decimal a float prints (what Float.toString prints from JDK 19 on), exactly, on every JDK: 3.4e38f is
     * 3.4E+38 and 1.5e10f is 1.5E+10, with the scale {@code new BigDecimal(Float.toString(f))} has there. Zero is
     * 0.0. For a finite float.
     */
    public static BigDecimal printedDecimal(float f) {
        if (f == 0) {
            return BigDecimal.valueOf(0, 1);                        // "0.0"
        }
        long decimal = decimal(f);
        return PrintedDecimal.toBigDecimal(f < 0, decimal >>> 8, (int) (decimal & 0xFF) - 128);
    }

    /**
     * The decimal a float prints, truncated toward zero, as a long, without a BigDecimal: printedDecimal(f) as a long.
     * For a finite float of magnitude below 2^63, whose printed decimal is then below 2^63 too.
     */
    public static long printedLong(float f) {
        if (f == 0) {
            return 0L;
        }
        long decimal = decimal(f);
        return PrintedDecimal.toLong(f < 0, decimal >>> 8, (int) (decimal & 0xFF) - 128);
    }

    /**
     * The double nearest the decimal {@code f} prints, for a finite float other than zero. The float's sign is kept.
     */
    public static double valueOf(float f) {
        long decimal = decimal(f);
        long significand = decimal >>> 8;
        int exponent = (int) (decimal & 0xFF) - 128;
        double value = FastDoubleMath.tryDecToDoubleWithFastAlgorithm(f < 0, significand, exponent);
        if (Double.isNaN(value)) {                                  // the fast algorithm could not decide
            value = Double.parseDouble(significand + "E" + exponent);
            return f < 0 ? -value : value;
        }
        return value;
    }

    /**
     * The decimal a positive float of that magnitude prints, as significand * 10^exponent, packed into
     * (significand << 8) | (exponent + 128).
     */
    static long decimal(float f) {
        int bits = Float.floatToRawIntBits(f) & 0x7FFFFFFF;
        int t = bits & (C_MIN - 1);
        int bq = bits >>> (P - 1);
        if (bq != 0) {
            return decimal(bq + Q_MIN - 1, C_MIN | t, 0);           // normal
        }
        return t < C_TINY ? decimal(Q_MIN, 10 * t, -1) : decimal(Q_MIN, t, 0);     // subnormal
    }

    /** Schubfach for the float c 2^q: the shortest decimal in its rounding interval, the closest when two are. */
    private static long decimal(int q, int c, int dk) {
        int out = c & 1;
        long cb = (long) c << 2;
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
        int h = q + FLOG2_POW10[k - K_MIN] + 33;
        long g = G1[k - K_MIN] + 1;
        int vb = rop(g, cb << h);
        int vbl = rop(g, cbl << h);
        int vbr = rop(g, cbr << h);
        int s = vb >> 2;
        if (s >= 100) {
            int sp10 = 10 * (int) (s * 1_717_986_919L >>> 34);     // s rounded down to a multiple of 10
            int tp10 = sp10 + 10;
            boolean upin = vbl + out <= sp10 << 2;
            boolean wpin = (tp10 << 2) + out <= vbr;
            if (upin != wpin) {
                return pack(upin ? sp10 : tp10, k);
            }
        }
        int t = s + 1;
        boolean uin = vbl + out <= s << 2;
        boolean win = (t << 2) + out <= vbr;
        if (uin != win) {
            return pack(uin ? s : t, k + dk);
        }
        int cmp = vb - ((s + t) << 1);
        return pack(cmp < 0 || cmp == 0 && (s & 1) == 0 ? s : t, k + dk);
    }

    private static long pack(int significand, int exponent) {
        return ((long) significand << 8) | (exponent + 128);
    }

    /** Round to odd of g cp 2^-95. */
    private static int rop(long g, long cp) {
        long x1 = PrintedDecimal.multiplyHigh(g, cp);
        long vbp = x1 >>> 31;
        return (int) (vbp | ((x1 & MASK_32) + MASK_32) >>> 32);
    }
}
