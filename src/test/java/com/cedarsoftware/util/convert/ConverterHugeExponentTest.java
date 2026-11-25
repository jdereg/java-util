package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * A String with a huge or tiny exponent converts to Byte, Short, Integer or Long at once.
 */
class ConverterHugeExponentTest {

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private void outOfRangeOrZero(String huge, String tiny) {
        for (Class<?> type : new Class<?>[]{Byte.class, Short.class, Integer.class, Long.class}) {
            assertThrows(IllegalArgumentException.class, () -> converter.convert(huge, type), huge + " " + type);
            assertEquals(0L, ((Number) converter.convert(tiny, type)).longValue(), tiny + " " + type);
        }
    }

    /**
     * Text such as "1e9999999" was parsed to a BigDecimal and truncated to a whole number before the range check,
     * which built all of its 10 million digits: 1.5 s for this one, ten times longer for each digit added to the
     * exponent, so an 11-character string held a thread for minutes. Out of range, or below 1, is now settled from
     * the digit count first.
     */
    @Test
    void hugeAndTinyExponentsAnswerAtOnce() {
        assertTimeout(Duration.ofMillis(500), () -> outOfRangeOrZero("1e9999999", "1e-9999999"));
        assertTimeout(Duration.ofMillis(500), () -> outOfRangeOrZero("-1e999999999", "-1e-999999999"));
        // ordinary values are unchanged
        assertEquals(100_000, (int) converter.convert("1e5", Integer.class));
        assertEquals(12, (int) converter.convert("12.9", Integer.class));
        assertEquals(-12L, (long) converter.convert("-12.9", Long.class));
        assertEquals(0, (int) converter.convert("0.000", Integer.class));
        assertEquals(Long.MAX_VALUE, (long) converter.convert("9223372036854775807.5", Long.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert("9223372036854775808", Long.class));
        assertThrows(IllegalArgumentException.class, () -> converter.convert("1e10", Integer.class));
    }

    /**
     * Zero written with an exponent is zero. The whole part's digits were counted as precision minus scale, which
     * for 0E+30 is 31, so zero was refused as outside a long ("Unable to convert 0E+30 to long: it is outside
     * -9223372036854775808 to 9223372036854775807"), outside an int and outside the range of Instant, and 0E+2000 was
     * refused as a whole number of 2,001 digits. MathUtilities.wholeDigits() counts none for zero.
     */
    @Test
    void aZeroWrittenWithAnExponentIsZero() {
        for (String zero : new String[]{"0E+30", "0E+2000", "0e600000000", "-0E+30", "0E+1", "0E-2000", "0.000", "0"}) {
            BigDecimal decimal = new BigDecimal(zero);
            for (Object source : new Object[]{decimal, zero}) {
                String what = source.getClass().getSimpleName() + " " + zero;
                for (Class<?> type : new Class<?>[]{Byte.class, Short.class, Integer.class, Long.class,
                        byte.class, short.class, int.class, long.class}) {
                    assertEquals(0L, ((Number) converter.convert(source, type)).longValue(), what + " to " + type);
                }
                assertEquals(BigInteger.ZERO, converter.convert(source, BigInteger.class), what);
                assertEquals(0L, converter.convert(source, AtomicLong.class).get(), what);
            }
            assertEquals(new Date(0), converter.convert(decimal, Date.class), zero);
            assertEquals(Instant.EPOCH, converter.convert(decimal, Instant.class), zero);
            assertEquals(Duration.ZERO, converter.convert(decimal, Duration.class), zero);
            assertEquals(new UUID(0, 0), converter.convert(decimal, UUID.class), zero);
        }
        // zero has a plain form of one digit whatever its exponent, so it is written out, as 0E+30 always was
        assertEquals("0", converter.convert(new BigDecimal("0E+2000"), String.class));
        assertEquals("0", converter.convert(new BigDecimal("0E+30"), String.class));
    }
}
