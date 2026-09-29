package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FloatDecimal gives the decimal a float prints on JDK 19 and later -- the shortest that reads back as the float,
 * the closest when two are, two digits when a two-digit one is closer -- as a double, without printing it. Every one
 * of the 1,249,902,592 positive floats with a fraction was checked once against Float.toString() on JDK 21, with no
 * difference, for it and its negation; this test checks a spread of about four million, plus edge families.
 */
class FloatDecimalTest {

    private static final int JDK = javaMajorVersion();

    private static int javaMajorVersion() {
        String v = System.getProperty("java.specification.version");
        return v.startsWith("1.") ? Integer.parseInt(v.substring(2)) : Integer.parseInt(v);
    }

    private static String decimalOf(float f) {
        long decimal = FloatDecimal.decimal(f);
        return (decimal >>> 8) + "E" + ((int) (decimal & 0xFF) - 128);
    }

    private static void check(float f) {
        double w = f;
        if (!Double.isFinite(w) || w == Math.rint(w)) {
            return;                                             // MultiKeyMap asks only about fractions
        }
        String decimal = decimalOf(f);
        assertEquals(Float.floatToRawIntBits(Math.abs(f)), Float.floatToRawIntBits(Float.parseFloat(decimal)),
                decimal + " does not read back as " + f);
        double value = FloatDecimal.valueOf(f);
        assertEquals(Double.doubleToRawLongBits((f < 0 ? -1 : 1) * Double.parseDouble(decimal)),
                Double.doubleToRawLongBits(value), "value of " + decimal);
        if (JDK >= 19) {
            assertEquals(Double.doubleToRawLongBits(Double.parseDouble(Float.toString(f))),
                    Double.doubleToRawLongBits(value), Float.toString(f));
        }
    }

    @Test
    void agreesWithFloatToStringAcrossAllBitPatterns() {
        for (long bits = 0; bits <= 0xFFFFFFFFL; bits += 1031) {
            check(Float.intBitsToFloat((int) bits));
        }
    }

    /** The floats JDK 8 through 18 print differently (a longer decimal); the JDK 19 decimal on every JDK. */
    @Test
    void theFloatsOlderJdksPrintDifferently() {
        String[][] expected = {
                {"10", "2.2E-44"}, {"10000", "9.1835E-41"}, {"10000000", "2.524355E-29"}, {"10800000", "5.04871E-29"},
                {"11000000", "1.009742E-28"}, {"11800000", "2.019484E-28"}, {"12000000", "4.038968E-28"},
                {"12800000", "8.077936E-28"}, {"15800000", "5.169879E-26"}, {"16000000", "1.0339758E-25"},
                {"200", "7.17E-43"}, {"2000", "1.148E-41"}, {"2c800000", "3.637979E-12"},
                {"2d000000", "7.275958E-12"}, {"32000000", "7.450581E-9"}, {"47", "9.9E-44"}, {"80", "1.8E-43"},
                {"800000", "1.1754944E-38"}, {"f800000", "1.2621775E-29"}};
        for (String[] e : expected) {
            float f = Float.intBitsToFloat(Integer.parseUnsignedInt(e[0], 16));
            assertEquals(Double.parseDouble(e[1]), FloatDecimal.valueOf(f), e[1]);
            assertEquals(-Double.parseDouble(e[1]), FloatDecimal.valueOf(-f), "-" + e[1]);
        }
    }

    /** Subnormals (a digit more below 8 ulps), powers of two (a narrower interval below), and near-integers. */
    @Test
    void edgeFamilies() {
        for (int bits = 1; bits < 4096; bits++) {
            check(Float.intBitsToFloat(bits));                  // the smallest subnormals
        }
        for (int e = -149; e <= 22; e++) {
            float p = (float) Math.pow(2, e);
            check(p);
            check(Math.nextUp(p));
            check(Math.nextDown(p));
            check(-p);
        }
        float[] values = {0.1f, 0.2f, 0.3f, 0.7f, 1.1f, 3.4028235E-5f, 0.99999994f, 1.0000001f, 8388607.5f, 4194303.8f,
                1.0E-3f, 9.999999E-4f, 123.456f, 1.0E-5f, 3.14159265f, 2.7182817f, Float.MIN_VALUE, Float.MIN_NORMAL};
        for (float f : values) {
            check(f);
            check(-f);
            check(Math.nextUp(f));
            check(Math.nextDown(f));
        }
    }
}
