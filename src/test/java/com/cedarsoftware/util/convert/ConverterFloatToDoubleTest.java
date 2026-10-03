package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Float to Double through the decimal the float prints (3.7f is 3.7), the JDK 19 decimal on every JDK.
 */
class ConverterFloatToDoubleTest {

    private static final Converter CONVERTER = new Converter(new DefaultConverterOptions());

    private static double convert(float f) {
        return CONVERTER.convert(f, Double.class);
    }

    /**
     * The floats JDK 8 through 18 print with a longer decimal than JDK 19 does. Printing and parsing gave a
     * different double for them on those JDKs; every JDK now gives the JDK 19 decimal, as MultiKeyMap does.
     */
    @Test
    void theFloatsOlderJdksPrintDifferentlyGiveTheJdk19Decimal() {
        String[][] expected = {
                {"10", "2.2E-44"}, {"10000", "9.1835E-41"}, {"10000000", "2.524355E-29"}, {"10800000", "5.04871E-29"},
                {"11000000", "1.009742E-28"}, {"11800000", "2.019484E-28"}, {"12000000", "4.038968E-28"},
                {"12800000", "8.077936E-28"}, {"15800000", "5.169879E-26"}, {"16000000", "1.0339758E-25"},
                {"200", "7.17E-43"}, {"2000", "1.148E-41"}, {"2c800000", "3.637979E-12"},
                {"2d000000", "7.275958E-12"}, {"32000000", "7.450581E-9"}, {"47", "9.9E-44"}, {"80", "1.8E-43"},
                {"800000", "1.1754944E-38"}, {"f800000", "1.2621775E-29"}};
        for (String[] e : expected) {
            float f = Float.intBitsToFloat(Integer.parseUnsignedInt(e[0], 16));
            assertEquals(Double.parseDouble(e[1]), convert(f), e[1]);
            assertEquals(-Double.parseDouble(e[1]), convert(-f), "-" + e[1]);
        }
    }

    /** Fractions are their printed decimal; whole numbers, infinities, NaN and signed zeros their exact value. */
    @Test
    void fractionsAreTheirDecimalAndEverythingElseItsExactValue() {
        assertEquals(3.7d, convert(3.7f));
        assertEquals(0.1d, convert(0.1f));
        assertEquals(-123.456d, convert(-123.456f));
        assertEquals(1.0E-20d, convert(1.0E-20f));                 // a decimal past Clinger's fast path
        assertEquals(16777216d, convert(16777216f));
        assertEquals(3.0d, convert(3.0f));
        assertEquals(Double.POSITIVE_INFINITY, convert(Float.POSITIVE_INFINITY));
        assertEquals(Double.NEGATIVE_INFINITY, convert(Float.NEGATIVE_INFINITY));
        assertEquals(Double.doubleToRawLongBits(Double.NaN), Double.doubleToRawLongBits(convert(Float.NaN)));
        assertEquals(Double.doubleToRawLongBits(-0.0d), Double.doubleToRawLongBits(convert(-0.0f)));
        assertEquals(Double.doubleToRawLongBits(0.0d), Double.doubleToRawLongBits(convert(0.0f)));
    }
}
