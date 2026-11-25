package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MathUtilities#wholeDigits(BigDecimal)} and {@link MathUtilities#toBigInteger(BigDecimal, int)}: a
 * BigDecimal's whole part without work in proportion to its exponent. The count was written out by hand, as
 * precision minus scale, in six places (the Converter's BigDecimal and String conversions to whole numbers and to
 * dates, IntervalSet), and that count is wrong for zero written with an exponent: 0E+30 came out as 31 digits, so
 * zero was refused as outside a long. The JDK is the oracle wherever it answers cheaply.
 * <p>
 * The class timeout is what fails if a whole part is ever built from an exponent again: 1E+600000000 takes five
 * minutes on JDK 25.
 */
@Timeout(value = 30, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class MathUtilitiesWholePartTest {

    @Test
    void wholeDigitsCountsTheDigitsBeforeThePoint() {
        Object[][] cases = {
                {"1", 1L}, {"9.99", 1L}, {"10", 2L}, {"12.5", 2L}, {"-12.5", 2L}, {"1E+3", 4L}, {"1.5E+3", 4L},
                {"-1E+3", 4L}, {"99999999999999999999", 20L}, {"100.000", 3L}, {"1E+600000000", 600000001L},
                {"1.5E+999999999", 1000000000L},
                // a whole part of 0: no digits
                {"0.5", 0L}, {"-0.999", 0L}, {"0.05", 0L}, {"1E-999999999", 0L}, {"123456789E-600000000", 0L},
                // zero, however it is written
                {"0", 0L}, {"0.000", 0L}, {"0E-2000", 0L}, {"0E+1", 0L}, {"0E+30", 0L}, {"-0E+30", 0L},
                {"0E+600000000", 0L},
        };
        for (Object[] each : cases) {
            assertEquals(each[1], MathUtilities.wholeDigits(new BigDecimal((String) each[0])), (String) each[0]);
        }
    }

    /** For 20,000 values the JDK converts cheaply, the whole part and its digit count are the JDK's. */
    @Test
    void theWholePartIsTheJdksWhereTheJdkAnswersCheaply() {
        Random random = new Random(20261125L);
        for (int i = 0; i < 20_000; i++) {
            BigInteger unscaled = new BigInteger(random.nextInt(260), random);       // up to 78 digits
            if (random.nextInt(4) == 0) {
                unscaled = unscaled.negate();
            }
            int scale = random.nextInt(8) == 0 ? random.nextInt(1800) - 900 : random.nextInt(120) - 60;
            BigDecimal value = new BigDecimal(unscaled, scale);
            BigInteger expected = value.toBigInteger();
            assertEquals(expected.signum() == 0 ? 0 : expected.abs().toString().length(),
                    MathUtilities.wholeDigits(value), value.toString());
            assertEquals(expected, MathUtilities.toBigInteger(value, 1000), value.toString());
            assertEquals(digits(value.toPlainString()), MathUtilities.plainDigits(value), value.toString());
        }
    }

    private static long digits(String plain) {
        long count = 0;
        for (int i = 0; i < plain.length(); i++) {
            if (Character.isDigit(plain.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    @Test
    void aWholePartOfZeroIsZeroAtOnce() {
        for (String zero : new String[]{"1E-600000000", "-1E-30000000", "123456789E-600000000", "1E-999999999",
                "0.5", "-0.999", "0", "0.000", "0E-2000", "0E+30", "-0E+30", "0E+2000", "0E+600000000"}) {
            assertEquals(BigInteger.ZERO, MathUtilities.toBigInteger(new BigDecimal(zero), 1000), zero);
            assertEquals(BigInteger.ZERO, MathUtilities.toBigInteger(new BigDecimal(zero), 0), zero);
        }
    }

    @Test
    void aWholePartAnExponentExpandsPastTheLimitIsRefusedNotBuilt() {
        for (String huge : new String[]{"1E+1000", "-1E+1000", "1.5E+30000000", "1E+600000000", "-1E+600000000",
                "1E+999999999", "123456789E+992"}) {                                  // 123456789E+992: 1,001 digits
            ArithmeticException e = assertThrows(ArithmeticException.class,
                    () -> MathUtilities.toBigInteger(new BigDecimal(huge), 1000), huge);
            assertTrue(e.getMessage().contains("more than 1000"), e.getMessage());
        }
        assertEquals("The whole part of 1E+1000 would take 1001 digits, more than 1000",
                assertThrows(ArithmeticException.class,
                        () -> MathUtilities.toBigInteger(new BigDecimal("1E+1000"), 1000)).getMessage());

        // the limit is the caller's
        assertEquals(BigInteger.valueOf(10_000), MathUtilities.toBigInteger(new BigDecimal("1E+4"), 5));
        assertThrows(ArithmeticException.class, () -> MathUtilities.toBigInteger(new BigDecimal("1E+5"), 5));
        assertThrows(ArithmeticException.class, () -> MathUtilities.toBigInteger(new BigDecimal("1.5E+7"), 5));
        assertThrows(ArithmeticException.class, () -> MathUtilities.toBigInteger(new BigDecimal("1234567E+2"), 5));
        assertEquals(BigInteger.valueOf(123_456_700), MathUtilities.toBigInteger(new BigDecimal("1234567E+2"), 9));

        // at the limit, and under it
        assertEquals(BigInteger.TEN.pow(999), MathUtilities.toBigInteger(new BigDecimal("1E+999"), 1000));
        assertEquals(BigInteger.TEN.pow(999).negate(), MathUtilities.toBigInteger(new BigDecimal("-1E+999"), 1000));
        assertEquals(BigInteger.valueOf(2500), MathUtilities.toBigInteger(new BigDecimal("2.5E+3"), 1000));
    }

    /** A value's own digits are never refused: only what an exponent adds is limited. */
    @Test
    void aNumbersOwnDigitsAreItsWholePartAtAnyLimit() {
        BigInteger written = BigInteger.TEN.pow(1500).add(BigInteger.valueOf(7));    // 1,501 digits
        assertEquals(written, MathUtilities.toBigInteger(new BigDecimal(written), 1000));
        assertEquals(written, MathUtilities.toBigInteger(new BigDecimal(written), 0));
        assertEquals(written, MathUtilities.toBigInteger(new BigDecimal(written + ".25"), 1000));
        assertEquals(written.negate(), MathUtilities.toBigInteger(new BigDecimal("-" + written + ".999"), 5));
        assertEquals(BigInteger.valueOf(123_456_789), MathUtilities.toBigInteger(new BigDecimal("123456789"), 5));
        assertEquals(BigInteger.valueOf(123_456_789), MathUtilities.toBigInteger(new BigDecimal("123456789.5"), 5));
        assertEquals(BigInteger.valueOf(2), MathUtilities.toBigInteger(new BigDecimal("2.9"), 1000));
        assertEquals(BigInteger.valueOf(-2), MathUtilities.toBigInteger(new BigDecimal("-2.9"), 1000));   // toward zero
    }

    /**
     * Zero written with an exponent has a plain form of one digit: toPlainString() writes "0". plainDigits() said
     * 31 for 0E+30 and 2,001 for 0E+2000, so past the limit zero was written as "0E+2000".
     */
    @Test
    void plainDigitsOfZeroWithAnExponentIsOne() {
        for (String zero : new String[]{"0E+1", "0E+30", "-0E+30", "0E+2000", "0E+600000000"}) {
            BigDecimal value = new BigDecimal(zero);
            assertEquals("0", value.toPlainString(), zero);
            assertEquals(1L, MathUtilities.plainDigits(value), zero);
            assertTrue(MathUtilities.fitsPlainDigits(value, 1000), zero);
            assertEquals("0", MathUtilities.toPlainString(value, 1000), zero);
        }
        // unchanged: a zero with decimal places is written with them, and counted so
        assertEquals(4L, MathUtilities.plainDigits(new BigDecimal("0.000")));
        assertEquals(2001L, MathUtilities.plainDigits(new BigDecimal("0E-2000")));
        assertFalse(MathUtilities.fitsPlainDigits(new BigDecimal("0E-2000"), 1000));
        assertEquals("0E-2000", MathUtilities.toPlainString(new BigDecimal("0E-2000"), 1000));
        assertEquals(4L, MathUtilities.plainDigits(new BigDecimal("1E+3")));
        assertEquals(3L, MathUtilities.plainDigits(new BigDecimal("12.5")));
        assertEquals(3L, MathUtilities.plainDigits(new BigDecimal("0.05")));
        assertEquals("1E+1001", MathUtilities.toPlainString(new BigDecimal("1E+1001"), 1000));
    }
}
