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
 * A float is the decimal it prints, and an integer type takes that decimal truncated toward zero, as BigDecimal and
 * String already take the decimal: 1.5e10f is 15000000000 as a Long, a BigInteger and a BigDecimal. Float to
 * BigInteger widened the float to a double and took the double's digits (3.4e38f gave
 * 339999995214436420000000000000000000000), and the integer types took the exact binary value (1.5e10f gave
 * 15000000512). The decimal is the one JDK 19+ prints, on every JDK (FloatDecimal). Below 2^24 every whole float is
 * exact and nothing changes.
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

    /**
     * The rule's promise: for any float, Long, BigInteger and BigDecimal-then-Long are one number (where it fits a
     * long). Random values of every magnitude.
     */
    @Test
    void longBigIntegerAndBigDecimalAgree() {
        Random random = new Random(19);
        List<String> wrong = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) {
            float f = (float) ((random.nextDouble() * 2 - 1) * Math.pow(10, random.nextInt(19)));
            for (Object value : new Object[] {f}) {
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
