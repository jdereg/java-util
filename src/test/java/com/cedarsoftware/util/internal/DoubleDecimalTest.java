package com.cedarsoftware.util.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DoubleDecimal gives the decimal a double prints on JDK 19 and later, on every JDK. Its oracle here is the JDK 19
 * specification itself, checked with BigDecimal, so it holds on JDK 8 too: the decimal reads back as the double; no
 * decimal of fewer digits does (unless it is two digits long); of the decimals of its length that read back, it is
 * the closest to the double, the even one on a tie. On JDK 19+ the text is also compared with Double.toString.
 * Billions of doubles were compared once with Double.toString on JDK 21 (random bits, every exponent, decimal-like
 * values and their neighbors, whole doubles above 2^53), with no difference in the text, the BigDecimal (value and
 * scale) or the long.
 */
class DoubleDecimalTest {

    private static final int JDK = javaMajorVersion();

    private static int javaMajorVersion() {
        String v = System.getProperty("java.specification.version");
        return v.startsWith("1.") ? Integer.parseInt(v.substring(2)) : Integer.parseInt(v);
    }

    /** Check v against the JDK 19 specification of Double.toString, and printedDecimal / printedLong against it. */
    private static void check(double v) {
        String text = DoubleDecimal.toString(v);
        char[] buf = new char[DoubleDecimal.MAX_CHARS + 3];
        int end = DoubleDecimal.toChars(v, buf, 3);
        assertEquals(text, new String(buf, 3, end - 3), "toChars at an offset");
        if (JDK >= 19) {
            assertEquals(Double.toString(v), text);
        }
        if (!Double.isFinite(v)) {
            return;
        }
        assertEquals(Double.doubleToRawLongBits(v), Double.doubleToRawLongBits(Double.parseDouble(text)),
                text + " does not read back as the double");
        BigDecimal printed = DoubleDecimal.printedDecimal(v);
        BigDecimal parsed = new BigDecimal(text);
        assertEquals(parsed.unscaledValue(), printed.unscaledValue(), "printedDecimal of " + text);
        assertEquals(parsed.scale(), printed.scale(), "printedDecimal scale of " + text);
        if (v > -0x1p63 && v < 0x1p63) {
            assertEquals(parsed.longValue(), DoubleDecimal.printedLong(v), "printedLong of " + text);
        }
        if (v == 0) {
            return;
        }
        BigDecimal x = new BigDecimal(Math.abs(v));                 // the double's exact value
        BigDecimal d = parsed.abs().stripTrailingZeros();
        int n = d.precision();
        if (n >= 3) {
            assertFalse(readsBack(x.round(new MathContext(n - 1, RoundingMode.FLOOR)), v), text + " is not shortest");
            assertFalse(readsBack(x.round(new MathContext(n - 1, RoundingMode.CEILING)), v), text + " is not shortest");
            assertClosest(x, d, n, v, text);
        } else {                                                    // one or two digits: the closest of either length
            assertClosest(x, d, 1, v, text);
            assertClosest(x, d, 2, v, text);
        }
    }

    /** Of the two decimals of n digits around x, the one that reads back and is closer must not beat d. */
    private static void assertClosest(BigDecimal x, BigDecimal d, int n, double v, String text) {
        BigDecimal distance = d.subtract(x).abs();
        for (RoundingMode mode : new RoundingMode[] {RoundingMode.FLOOR, RoundingMode.CEILING}) {
            BigDecimal other = x.round(new MathContext(n, mode));
            if (other.compareTo(d) == 0 || !readsBack(other, v)) {
                continue;
            }
            int cmp = other.subtract(x).abs().compareTo(distance);
            assertTrue(cmp > 0 || cmp == 0 && n == d.precision() && evenLastDigit(d),
                    text + " is not the closest: " + other);
        }
    }

    private static boolean readsBack(BigDecimal candidate, double v) {
        return candidate.signum() != 0 && Double.parseDouble(candidate.toString()) == Math.abs(v);
    }

    private static boolean evenLastDigit(BigDecimal d) {
        return !d.unscaledValue().testBit(0);
    }

    @Test
    void randomBitPatterns() {
        SplittableRandom r = new SplittableRandom(19);
        for (int i = 0; i < 100_000; i++) {
            check(Double.longBitsToDouble(r.nextLong()));
        }
    }

    @Test
    void decimalLikeValuesAndTheirNeighbors() {
        SplittableRandom r = new SplittableRandom(23);
        for (int i = 0; i < 30_000; i++) {
            int digits = 1 + r.nextInt(17);
            long m = r.nextLong(1, PrintedDecimal.POW10[digits]);
            double d = Double.parseDouble(m + "E" + (r.nextInt(650) - 340));
            check(d);
            check(Math.nextUp(d));
            check(Math.nextDown(d));
            check(-d);
        }
    }

    /** Subnormals (a digit more below 3 ulps), powers of two (a narrower interval below), whole numbers. */
    @Test
    void edgeFamilies() {
        for (long bits = 1; bits < 2048; bits++) {
            check(Double.longBitsToDouble(bits));                   // the smallest subnormals
        }
        for (int e = -1074; e <= 1023; e++) {
            double p = Math.scalb(1.0, e);
            check(p);
            check(Math.nextUp(p));
            check(Math.nextDown(p));
            check(-p);
        }
        for (int k = -323; k <= 308; k++) {
            double p = Double.parseDouble("1E" + k);
            check(p);
            check(Math.nextUp(p));
            check(Math.nextDown(p));
        }
        SplittableRandom r = new SplittableRandom(29);
        for (int i = 0; i < 20_000; i++) {
            check((double) r.nextLong(1L << 53));                   // whole, below 2^53: the integer itself
            check((double) r.nextLong());                           // whole, mostly above 2^53
        }
        double[] values = {0.0, -0.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.MIN_VALUE, Double.MIN_NORMAL, Double.MAX_VALUE, 0.1, 0.2, 0.3, 1.0E-3, 9.999999999999998E-4,
                1.0E7, 9999999.999999998, 123.0, 1234567.0, 12345678.0, 0x1p53, 0x1p63, -0x1p63, 0x1p64};
        for (double v : values) {
            check(v);
            check(-v);
        }
    }

    /** Doubles JDK 8 prints differently (a longer or a different decimal): the JDK 19 decimal on every JDK. */
    @Test
    void theDoublesOlderJdksPrintDifferently() {
        String[][] expected = {
                {"44b52d02c7e14af6", "1.0E23"},                     // JDK 8: 9.999999999999999E22
                {"44c52d02c7e14af6", "2.0E23"},                     // JDK 8: 1.9999999999999998E23
                {"447c7e83209e90b2", "8.41E21"},                    // JDK 8: 8.409999999999999E21
                {"43b0000000000000", "1.152921504606847E18"},       // 2^60; JDK 8: 1.15292150460684698E18
                {"43d0edfc638cf012", "4.879634314335111E18"},       // JDK 8: 4.8796343143351112E18
                {"44061cecb24ea3bc", "5.09890758E19"},              // JDK 8: 5.0989075800000004E19
                {"449a420db02bd7d6", "3.1E22"},                     // JDK 8: 3.1000000000000002E22
                {"43a1cae0c517841a", "6.4104209052402E17"},         // JDK 8: 6.4104209052401997E17
                {"2", "9.9E-324"},                                  // JDK 8: 1.0E-323
                {"14", "9.9E-323"},                                 // JDK 8: 1.0E-322
                {"80", "6.3E-322"}};                                // JDK 8: 6.32E-322
        for (String[] e : expected) {
            double v = Double.longBitsToDouble(Long.parseUnsignedLong(e[0], 16));
            assertEquals(e[1], DoubleDecimal.toString(v));
            assertEquals("-" + e[1], DoubleDecimal.toString(-v));
            assertEquals(new BigDecimal(e[1]), DoubleDecimal.printedDecimal(v));
            check(v);
        }
        assertEquals(1152921504606847000L, DoubleDecimal.printedLong(0x1p60));
        assertEquals(-1152921504606847000L, DoubleDecimal.printedLong(-0x1p60));
    }

    @Test
    void layout() {
        String[][] expected = {
                {"0.0", "0.0"}, {"-0.0", "-0.0"}, {"1", "1.0"}, {"100", "100.0"}, {"1234567", "1234567.0"},
                {"12345678", "1.2345678E7"}, {"1E7", "1.0E7"}, {"0.001", "0.001"}, {"0.00123", "0.00123"},
                {"0.000999", "9.99E-4"}, {"12.34", "12.34"}, {"-12.5", "-12.5"}, {"1E-5", "1.0E-5"},
                {"1.5E300", "1.5E300"}, {"4.9E-324", "4.9E-324"}, {"1.7976931348623157E308", "1.7976931348623157E308"},
                {"NaN", "NaN"}, {"Infinity", "Infinity"}, {"-Infinity", "-Infinity"}};
        for (String[] e : expected) {
            assertEquals(e[1], DoubleDecimal.toString(Double.parseDouble(e[0])), e[0]);
        }
    }
}
