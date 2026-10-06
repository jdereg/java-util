package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Double and Float to BigInteger: the whole part of the value as Java prints it, whatever the default locale. The
 * decimal is the one JDK 19+ prints, on every JDK (DoubleDecimal): the across-the-range comparison with the JDK's own
 * printing runs on JDK 19+, where that printing is the oracle; JDK 8 printed many doubles differently (1.0E23 as
 * 9.999999999999999E22), and DoubleDecimalTest checks the decimal there against the specification itself.
 */
class ConverterDoubleToBigIntegerTest {

    private static final int JDK = javaMajorVersion();

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private static int javaMajorVersion() {
        String v = System.getProperty("java.specification.version");
        return v.startsWith("1.") ? Integer.parseInt(v.substring(2)) : Integer.parseInt(v);
    }

    /** The previous algorithm, with the locale pinned: the expected answer, where the JDK prints the JDK 19 decimal. */
    private static BigInteger reference(double d) {
        return new BigInteger(String.format(Locale.US, "%.0f", d > 0.0 ? Math.floor(d) : Math.ceil(d)));
    }

    /**
     * The conversion works under any default locale. It printed the value with String.format("%.0f"), which uses the
     * default locale's digits: under ar-SA (JDK 9+) that is ١٢٣٤٥, and parsing it threw NumberFormatException.
     */
    @Test
    void worksUnderALocaleWithOtherDigits() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-SA"));
            assertEquals(BigInteger.valueOf(12345), converter.convert(12345.6d, BigInteger.class));
            assertEquals(BigInteger.valueOf(-12345), converter.convert(-12345.6f, BigInteger.class));
        } finally {
            Locale.setDefault(saved);
        }
    }

    /** The same answers as before for every double tried: random bit patterns, magnitudes and fractions. */
    @Test
    void answersAsBeforeAcrossTheDoubleRange() {
        assumeTrue(JDK >= 19, "the JDK's own printing is the oracle from JDK 19 on");
        List<Double> special = Arrays.asList(0.0, -0.0, 0.5, -0.5, 1.5, -1.5, 0.999999999999, 1e15 + 0.5,
                4503599627370495.5, 9007199254740993.0, 1e16, 1e22, 1e23, 1.2345678901234567E30, Double.MAX_VALUE,
                -Double.MAX_VALUE, Double.MIN_VALUE, 123456789.987654321, (double) Long.MAX_VALUE, 2.5e-300);
        for (double d : special) {
            assertEquals(reference(d), converter.convert(d, BigInteger.class), String.valueOf(d));
        }
        Random random = new Random(42);
        for (int i = 0; i < 200_000; i++) {
            double d;
            switch (i % 4) {
                case 0:
                    d = Double.longBitsToDouble(random.nextLong());
                    break;
                case 1:
                    d = (random.nextDouble() - 0.5) * Math.pow(10, random.nextInt(40));
                    break;
                case 2:
                    d = random.nextInt() + random.nextDouble();
                    break;
                default:
                    d = (float) ((random.nextDouble() - 0.5) * Math.pow(10, random.nextInt(38)));
            }
            if (!Double.isNaN(d) && !Double.isInfinite(d)) {
                assertEquals(reference(d), converter.convert(d, BigInteger.class), String.valueOf(d));
            }
        }
    }
}
