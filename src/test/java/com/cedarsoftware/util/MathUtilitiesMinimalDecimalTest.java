package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Random;

import org.junit.jupiter.api.Test;

import static com.cedarsoftware.util.MathUtilities.parseToMinimalNumericType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MathUtilities#parseToMinimalNumericType(String)} on text with a decimal point or an exponent: a Double when
 * the double reads back as the number the text names, otherwise a BigDecimal holding that number. Up to 4.117.0 the
 * choice was made by counting digits (any digits) and the exponent, and it got three things wrong: finite text became
 * Infinity, 16 digits came back as a different number, and doubles came back as BigDecimals (the 17 digits
 * {@link Double#toString(double)} writes, and doubles near the ends of the range).
 */
class MathUtilitiesMinimalDecimalTest
{
    /** Whether this JDK's Double.toString writes the shortest form (JDK 19+); JDK 8-18 add an 18th digit to a few. */
    private static final boolean TO_STRING_IS_SHORTEST = BigDecimal.valueOf(1.90369215284329024E17).precision() <= 17;

    @Test
    void finiteTextNeverBecomesInfinity()
    {
        String[] finite = {"99e307", "-99e307", "1000000e305", "123456789012345.6e300", "1.7976931348623159e308"};
        for (String s : finite)
        {
            assertKeptAsBigDecimal(s);
        }
    }

    @Test
    void aSubnormalThatWouldLoseDigitsIsABigDecimal()
    {
        // Near the bottom of the range a double keeps 3 or 4 digits. (1e-322 is JDK-dependent: its double is written
        // 9.9E-323 from JDK 19, 1.0E-322 before, so it is a BigDecimal on one and a Double on the other.)
        assertKeptAsBigDecimal("1.23456e-320");                 // the nearest double reads back as 1.2347E-320
        assertKeptAsBigDecimal("5e-324");                       // the nearest double reads back as 4.9E-324
    }

    @Test
    void aSubnormalThatKeepsItsDigitsIsADouble()
    {
        assertDouble("4.9E-324", Double.MIN_VALUE);
        assertDouble("1.0E-310", 1.0E-310);
        assertDouble("-1.0E-310", -1.0E-310);
    }

    @Test
    void sixteenDigitsThatADoubleWouldChangeAreABigDecimal()
    {
        assertKeptAsBigDecimal("8.000000000000001");            // the nearest double reads back as 8.000000000000002
        assertKeptAsBigDecimal("-8.000000000000001");
        assertDouble("1.000000000000001", 1.000000000000001);   // this one reads back
    }

    @Test
    void whatDoubleToStringWritesComesBackAsADouble()
    {
        double[] doubles = {0.1 + 0.2, Math.PI, Math.E, 1.0 / 3, 2.0 / 3, 1e23, 9.9e307, 1e308, 1e-308,
                Double.MAX_VALUE, -Double.MAX_VALUE, Double.MIN_NORMAL, Double.MIN_VALUE, 0.1, -123.456};
        for (double d : doubles)
        {
            assertDouble(Double.toString(d), d);
        }
    }

    @Test
    void significantDigitsAreCountedNotAllDigits()
    {
        assertDouble("1.50000000000000000000", 1.5);                 // trailing zeros are not digits a double must keep
        assertDouble("0.0000000000000000000012", 1.2e-21);           // nor are leading zeros
        assertDouble("000000000000000000123.25", 123.25);
        assertKeptAsBigDecimal("1.234567890123456789");               // 19 significant digits: no double holds them
        assertKeptAsBigDecimal("12345678901234567890.12345678901234567890");
    }

    @Test
    void zeroIsADoubleWhateverItsExponent()
    {
        assertDouble("0.0", 0.0);
        assertDouble("0e400", 0.0);
        assertDouble("0.000e-999", 0.0);
        Number negativeZero = parseToMinimalNumericType("-0.0");
        assertInstanceOf(Double.class, negativeZero);
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits((Double) negativeZero));
    }

    @Test
    void textOnlyTheDoubleParserReadsIsADouble()
    {
        // no BigDecimal reading to compare with: the double parser's is the reading (4.117.0 threw at 17 digits)
        assertDouble("1.000000000000001d", 1.000000000000001);
        assertDouble(" 3.141592653589793 ", Math.PI);
        assertDouble("0x1.8p1", 3.0);
        assertDouble("0.30000000000000004d", 0.1 + 0.2);
        assertDouble(" 0.30000000000000004 ", 0.1 + 0.2);
    }

    @Test
    void nonzeroTextTooSmallForAnyDoubleIsABigDecimal()
    {
        assertKeptAsBigDecimal("1e-400");
        assertKeptAsBigDecimal("-2.5e-330");
        assertKeptAsBigDecimal("0.000001e-320");
    }

    /**
     * The rule over many numbers, stated without the method's digit counting: a Double reads back as the number the
     * text names (in the shortest form, or as this JDK writes it); a BigDecimal holds that number, and where
     * Double.toString writes the shortest form, it appears only when no double reads back as that number.
     */
    @Test
    void theRuleOverManyNumbers()
    {
        Random random = new Random(20261023);
        for (int i = 0; i < 20000; i++)
        {
            String text = i % 2 == 0 ? randomDecimal(random) : Double.toString(randomDouble(random));
            BigDecimal exact = new BigDecimal(text);
            double nearest = Double.parseDouble(text);
            boolean aDoubleReadsBack = !Double.isInfinite(nearest) && (sameNumber(shortest(nearest), exact)
                    || sameNumber(BigDecimal.valueOf(nearest), exact));
            Number n = parseToMinimalNumericType(text);
            if (n instanceof Double)
            {
                assertTrue(aDoubleReadsBack, text + " became the Double " + n + ", which reads back as another number");
                assertEquals(nearest, n.doubleValue(), text);
            }
            else
            {
                assertInstanceOf(BigDecimal.class, n, text);
                assertTrue(sameNumber((BigDecimal) n, exact), text + " became " + n);
                if (TO_STRING_IS_SHORTEST)
                {
                    assertFalse(aDoubleReadsBack, text + " became a BigDecimal, but " + nearest + " reads back");
                }
            }
        }
    }

    private static void assertDouble(String text, double expected)
    {
        Number n = parseToMinimalNumericType(text);
        assertInstanceOf(Double.class, n, text);
        assertEquals(expected, n.doubleValue(), text);
    }

    private static void assertKeptAsBigDecimal(String text)
    {
        Number n = parseToMinimalNumericType(text);
        assertInstanceOf(BigDecimal.class, n, text);
        assertTrue(sameNumber((BigDecimal) n, new BigDecimal(text)), text + " became " + n);
    }

    private static boolean sameNumber(BigDecimal a, BigDecimal b)
    {
        return a.compareTo(b) == 0;
    }

    /** The shortest decimal that reads as {@code d}, the closest to it among those (what JDK 19+ writes). */
    private static BigDecimal shortest(double d)
    {
        if (d == 0.0)
        {
            return BigDecimal.ZERO;
        }
        BigDecimal exact = new BigDecimal(d);
        for (int digits = 1; digits <= 17; digits++)
        {
            BigDecimal candidate = exact.round(new MathContext(digits, RoundingMode.HALF_EVEN));
            if (Double.parseDouble(candidate.toString()) == d)
            {
                return candidate;
            }
        }
        throw new AssertionError("no decimal of 17 digits or fewer reads as " + d);
    }

    /** Any finite double, subnormals and both ends of the range included. */
    private static double randomDouble(Random random)
    {
        double d;
        do
        {
            d = Double.longBitsToDouble(random.nextLong());
        }
        while (Double.isNaN(d) || Double.isInfinite(d));
        return d;
    }

    /** 1 to 20 significant digits, sometimes with zeros around them, at a decimal exponent from -330 to +330. */
    private static String randomDecimal(Random random)
    {
        StringBuilder sb = new StringBuilder();
        if (random.nextBoolean())
        {
            sb.append('-');
        }
        int digits = 1 + random.nextInt(20);
        sb.append((char) ('1' + random.nextInt(9)));
        for (int i = 1; i < digits; i++)
        {
            sb.append((char) ('0' + random.nextInt(10)));
        }
        for (int i = random.nextInt(3); i > 0; i--)
        {
            sb.append('0');
        }
        int point = 1 + random.nextInt(sb.length() - (sb.charAt(0) == '-' ? 1 : 0));
        sb.insert(sb.charAt(0) == '-' ? point + 1 : point, '.');
        int exponent = random.nextInt(4) == 0 ? random.nextInt(20) - 10 : random.nextInt(661) - 330;
        return sb.append('e').append(exponent).toString();
    }
}
