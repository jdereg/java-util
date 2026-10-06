package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A float or double is the decimal it prints, and an integer type takes that decimal truncated toward zero, as
 * BigDecimal and String already take the decimal: 1.5e10f is 15000000000 as a Long, a BigInteger and a BigDecimal, and
 * 2^60 as a double is 1152921504606847000 (it prints 1.152921504606847E18). Float to BigInteger widened the float to a
 * double and took the double's digits (3.4e38f gave 339999995214436420000000000000000000000), and the integer types
 * took the exact binary value (1.5e10f gave 15000000512, 2^60 gave 1152921504606846976). The decimal is the one JDK
 * 19+ prints, on every JDK (FloatDecimal, DoubleDecimal): JDK 8 printed 1.0E23 as 9.999999999999999E22. Below 2^24 for
 * a float, 2^53 for a double, every whole value is exact and nothing changes. The double -2^63 stays Long.MIN_VALUE.
 */
class ConverterFloatingWholeNumberTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    @Test
    void largeFloatsAreTheDecimalTheyPrint() {
        assertEquals(15_000_000_000L, (long) converter.convert(1.5e10f, Long.class));
        assertEquals(BigInteger.valueOf(15_000_000_000L), converter.convert(1.5e10f, BigInteger.class));
        assertEquals(15_000_000_000L, converter.convert(1.5e10f, AtomicLong.class).get());
        assertEquals(1_000_000_000_000_000_000L, (long) converter.convert(1e18f, Long.class));
        assertEquals(new BigInteger("340000000000000000000000000000000000000"),
                converter.convert(3.4e38f, BigInteger.class));
        assertEquals(1_234_568_000, (int) converter.convert(1.23456794E9f, Integer.class));   // prints 1.234568E9
        assertEquals(9_223_371_500_000_000_000L, (long) converter.convert(9.2233715E18f, Long.class));
    }

    /** Below 2^24 a whole float is exact, and a fraction still truncates toward zero. */
    @Test
    void smallerValuesAreUnchanged() {
        assertEquals(16_777_216L, (long) converter.convert(16_777_216f, Long.class));
        assertEquals(3L, (long) converter.convert(3.7f, Long.class));
        assertEquals(-3, (int) converter.convert(-3.7f, Integer.class));
        assertEquals(BigInteger.valueOf(-12), converter.convert(-12.9f, BigInteger.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(Float.NaN, Long.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(Float.POSITIVE_INFINITY,
                BigInteger.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(9.223372E18f, Long.class));
    }

    /** JDK 8 printed 19 floats with a longer decimal than JDK 19+: BigDecimal takes the JDK 19 one on every JDK. */
    @Test
    void theDecimalIsTheSameOnEveryJdk() {
        float f = 9.9E-44f;                         // JDK 8: Float.toString gives "1.0E-43"
        assertEquals(new BigDecimal("9.9E-44"), converter.convert(f, BigDecimal.class));
    }

    @Test
    void largeDoublesAreTheDecimalTheyPrint() {
        assertEquals(1_152_921_504_606_847_000L, (long) converter.convert(0x1p60, Long.class));
        assertEquals(BigInteger.valueOf(1_152_921_504_606_847_000L), converter.convert(0x1p60, BigInteger.class));
        assertEquals(new BigDecimal("1.152921504606847E+18"), converter.convert(0x1p60, BigDecimal.class));
        assertEquals(-1_152_921_504_606_847_000L, converter.convert(-0x1p60, AtomicLong.class).get());
        assertEquals(4_879_634_314_335_111_000L, (long) converter.convert(4.879634314335111E18, Long.class));
        assertEquals(9_223_372_036_854_775_000L, (long) converter.convert(Math.nextDown(0x1p63), Long.class));
        assertEquals(new BigInteger("100000000000000000000000"), converter.convert(1e23, BigInteger.class));
    }

    /** Below 2^53 a whole double is exact, and a fraction still truncates toward zero. */
    @Test
    void smallerDoublesAreUnchanged() {
        assertEquals(9_007_199_254_740_991L, (long) converter.convert(9_007_199_254_740_991d, Long.class));
        assertEquals(-9_007_199_254_740_991L, (long) converter.convert(-9_007_199_254_740_991d, Long.class));
        assertEquals(3L, (long) converter.convert(3.7, Long.class));
        assertEquals(-3, (int) converter.convert(-3.7, Integer.class));
        assertEquals(BigInteger.valueOf(-12), converter.convert(-12.9, BigInteger.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(Double.NaN, Long.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert(0x1p63, Long.class));
        // -2^63, the double Long.MIN_VALUE becomes, comes back as Long.MIN_VALUE, although it prints
        // -9.223372036854776E18, just past a long: the one double whose decimal is outside a long and its value not
        assertEquals(Long.MIN_VALUE, (long) converter.convert((double) Long.MIN_VALUE, Long.class));
    }

    /** JDK 8 printed many doubles (and 19 floats) differently from JDK 19+: every JDK now gives the JDK 19 decimal. */
    @Test
    void theDoubleDecimalIsTheSameOnEveryJdk() {
        assertEquals("1.0E23", converter.convert(1e23, String.class));                 // JDK 8: 9.999999999999999E22
        assertEquals(new BigDecimal("1.0E+23"), converter.convert(1e23, BigDecimal.class));
        assertEquals(new BigDecimal("2.0E+23"), converter.convert(2e23, BigDecimal.class));    // JDK 8: 1.9999...8E23
        assertEquals("8.41E21", converter.convert(8.41E21, String.class));             // JDK 8: 8.409999999999999E21
        assertEquals(new BigInteger("8410000000000000000000"), converter.convert(8.41E21, BigInteger.class));
        assertEquals("1.152921504606847E18", converter.convert(0x1p60, String.class)); // JDK 8: ...698E18
        assertEquals(new BigDecimal("6.3E-322"), converter.convert(6.3E-322, BigDecimal.class));  // JDK 8: 6.32E-322
        assertEquals("9.9E-44", converter.convert(9.9E-44f, String.class));            // JDK 8: 1.0E-43
        assertEquals("0", converter.convert(0.0, String.class));
        assertEquals("-0.0", converter.convert(-0.0, String.class));
    }

    /**
     * The rule's promise: for any float or double, Long, BigInteger and BigDecimal-then-Long are one number (where it
     * fits a long). Random values of every magnitude.
     */
    @Test
    void longBigIntegerAndBigDecimalAgree() {
        Random random = new Random(19);
        List<String> wrong = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) {
            double d = (random.nextDouble() * 2 - 1) * Math.pow(10, random.nextInt(19));
            float f = (float) d;
            for (Object value : new Object[] {f, d}) {
                long viaLong = converter.convert(value, Long.class);
                BigInteger viaBigInteger = converter.convert(value, BigInteger.class);
                long viaBigDecimal = converter.convert(converter.convert(value, BigDecimal.class), Long.class);
                if (viaLong != viaBigInteger.longValue() || viaLong != viaBigDecimal) {
                    wrong.add(value.getClass().getSimpleName() + " " + value + ": Long " + viaLong + ", BigInteger "
                            + viaBigInteger + ", via BigDecimal " + viaBigDecimal);
                }
            }
        }
        assertTrue(wrong.isEmpty(), wrong.size() + " disagree, e.g. " + wrong.subList(0, Math.min(3, wrong.size())));
    }
}
