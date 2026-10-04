package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A number converts to a narrower type only if it fits: a value beyond the target's range, NaN or infinity is an
 * IllegalArgumentException, whatever the source. A String was already refused that way; a Long or BigInteger wrapped
 * (2^32 + 1 as an int was 1), a double clamped (1e10 as an int was 2147483647, and as a short -1) and NaN became 0, the
 * epoch, or an enum's first constant. A fraction is still truncated toward zero, as a cast does.
 */
class ConverterNarrowingTest {

    enum Color { RED, GREEN, BLUE }

    private final Converter converter = new Converter(new DefaultConverterOptions());

    private static final BigInteger TWO_64_PLUS_5 = BigInteger.ONE.shiftLeft(64).add(BigInteger.valueOf(5));

    private void refused(Object source, Class<?> target, List<String> wrong) {
        try {
            Object result = converter.convert(source, target);
            wrong.add(describe(source) + " -> " + target.getSimpleName() + " gave " + result);
        } catch (IllegalArgumentException expected) {
            // refused
        } catch (RuntimeException other) {
            wrong.add(describe(source) + " -> " + target.getSimpleName() + " threw " + other);
        }
    }

    private static String describe(Object source) {
        return source == null ? "null" : source.getClass().getSimpleName() + " " + source;
    }

    @Test
    void integerTypesRefuseWhatDoesNotFit() {
        List<String> wrong = new ArrayList<>();
        Object[] tooBigForInt = {(1L << 32) + 1, Long.MAX_VALUE, 1e10, -1e10, 1e10f, TWO_64_PLUS_5,
                new BigDecimal("1e20"), new AtomicLong(1L << 40), 2147483648.0};
        for (Object source : tooBigForInt) {
            for (Class<?> target : new Class<?>[] {int.class, Integer.class, AtomicInteger.class, short.class,
                    byte.class, char.class}) {
                refused(source, target, wrong);
            }
        }
        for (Object source : new Object[] {TWO_64_PLUS_5, 1e19, -1e19, new BigDecimal("1e19"), 9.3e18f}) {
            refused(source, long.class, wrong);
            refused(source, AtomicLong.class, wrong);
        }
        for (Object source : new Object[] {32768, -32769L, 40000.0, new BigInteger("32768")}) {
            refused(source, short.class, wrong);
        }
        for (Object source : new Object[] {128, -129L, 200.5, new BigDecimal("300")}) {
            refused(source, byte.class, wrong);
        }
        for (Object source : new Object[] {-1, 65536L, -0.5 - 1, TWO_64_PLUS_5}) {
            refused(source, char.class, wrong);
        }
        assertTrue(wrong.isEmpty(), wrong.size() + " not refused: " + wrong);
    }

    @Test
    void notANumberAndInfinityAreRefused() {
        List<String> wrong = new ArrayList<>();
        Object[] nonFinite = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Float.NaN,
                Float.POSITIVE_INFINITY};
        Class<?>[] targets = {int.class, long.class, short.class, byte.class, char.class, Integer.class, Long.class,
                AtomicInteger.class, AtomicLong.class, Color.class, Date.class, Instant.class, Duration.class,
                LocalTime.class, LocalDate.class, Calendar.class, Timestamp.class, java.sql.Date.class};
        for (Object source : nonFinite) {
            for (Class<?> target : targets) {
                refused(source, target, wrong);
            }
        }
        assertTrue(wrong.isEmpty(), wrong.size() + " not refused: " + wrong);
    }

    /** A finite value that would be infinite as a float or double is refused; NaN and infinity stay as they are. */
    @Test
    void floatingTypesRefuseFiniteOverflow() {
        List<String> wrong = new ArrayList<>();
        for (Object source : new Object[] {1e300, -1e300, new BigDecimal("1e39"), BigInteger.ONE.shiftLeft(200),
                "1e300", "-3.5e38"}) {
            refused(source, float.class, wrong);
            refused(source, Float.class, wrong);
        }
        for (Object source : new Object[] {new BigDecimal("1e400"), BigInteger.ONE.shiftLeft(2000), "1e400"}) {
            refused(source, double.class, wrong);
        }
        assertTrue(wrong.isEmpty(), wrong.size() + " not refused: " + wrong);

        assertEquals(Float.POSITIVE_INFINITY, converter.convert(Double.POSITIVE_INFINITY, float.class));
        assertTrue(Float.isNaN(converter.convert(Double.NaN, float.class)));
        assertEquals(Float.POSITIVE_INFINITY, converter.convert("Infinity", float.class));
        assertEquals(Double.NEGATIVE_INFINITY, converter.convert("-Infinity", double.class));
        assertEquals(3.1415927f, converter.convert(3.14159265358979, float.class));
        assertEquals(0.0f, converter.convert(1e-50, float.class));
        assertEquals(Float.MAX_VALUE, converter.convert((double) Float.MAX_VALUE, float.class));
    }

    /** Dates and times from a number out of their range: refused, not wrapped to 1970 or clamped to year 292278994. */
    @Test
    void datesAndTimesRefuseWhatDoesNotFit() {
        List<String> wrong = new ArrayList<>();
        // a BigInteger counts milliseconds to Date and Calendar, nanoseconds to Instant, Timestamp and Duration
        BigInteger bigMillis = BigInteger.ONE.shiftLeft(64).add(BigInteger.valueOf(1000));
        BigInteger bigNanos = bigMillis.multiply(BigInteger.valueOf(1_000_000_000L));
        for (Class<?> target : new Class<?>[] {Date.class, java.sql.Date.class, Calendar.class}) {
            refused(bigMillis, target, wrong);
        }
        for (Class<?> target : new Class<?>[] {Date.class, java.sql.Date.class, Timestamp.class, Calendar.class,
                Instant.class, Duration.class, LocalTime.class}) {
            refused(bigNanos, target, wrong);
            refused(1e300, target, wrong);
            refused(-1e300, target, wrong);
            refused(new BigDecimal("1e20"), target, wrong);
        }
        refused(TWO_64_PLUS_5, LocalTime.class, wrong);
        refused("99999999999999999999", Duration.class, wrong);
        assertTrue(wrong.isEmpty(), wrong.size() + " not refused: " + wrong);
    }

    /** char, Year, YearMonth and MonthDay narrow by the same rule. */
    @Test
    void charactersAndCalendarFieldsRefuseWhatDoesNotFit() {
        List<String> wrong = new ArrayList<>();
        refused('é', byte.class, wrong);
        refused('￿', short.class, wrong);
        refused((byte) -1, char.class, wrong);
        refused((short) -1, char.class, wrong);
        refused(Year.of(40000), short.class, wrong);
        refused(YearMonth.of(2024, 1), short.class, wrong);
        refused(YearMonth.of(30_000_000, 1), int.class, wrong);
        assertTrue(wrong.isEmpty(), wrong.size() + " not refused: " + wrong);

        assertEquals((byte) 65, converter.convert('A', byte.class));
        assertEquals('A', converter.convert((byte) 65, char.class));
        assertEquals((short) 2024, converter.convert(Year.of(2024), short.class));
        assertEquals((short) 1225, converter.convert(MonthDay.of(12, 25), short.class));
        assertEquals(202401, converter.convert(YearMonth.of(2024, 1), int.class));
    }

    /** An enum by ordinal, alone or in a collection: an ordinal that wraps into range is not taken. */
    @Test
    void enumOrdinalsRefuseWhatDoesNotFit() {
        List<String> wrong = new ArrayList<>();
        refused((1L << 32) + 1, Color.class, wrong);
        refused(Arrays.asList((1L << 32) + 1), Color.class, wrong);
        refused(new Object[] {TWO_64_PLUS_5}, Color.class, wrong);
        assertTrue(wrong.isEmpty(), wrong.size() + " not refused: " + wrong);
        assertEquals(Color.GREEN, converter.convert(1L, Color.class));
    }

    /** A fraction is truncated toward zero, from every source, as before. */
    @Test
    void fractionsAreStillTruncated() {
        for (Object source : new Object[] {3.7, 3.7f, new BigDecimal("3.7"), "3.7"}) {
            assertEquals(3, converter.convert(source, int.class), describe(source));
            assertEquals(3L, converter.convert(source, long.class), describe(source));
            assertEquals((short) 3, converter.convert(source, short.class), describe(source));
            assertEquals((byte) 3, converter.convert(source, byte.class), describe(source));
        }
        assertEquals(-3, converter.convert(-3.7, int.class));
        assertEquals(Integer.MAX_VALUE, converter.convert(2147483647.9, int.class));
        assertEquals(Integer.MIN_VALUE, converter.convert(-2147483648.9, int.class));
        assertEquals(Long.MIN_VALUE, converter.convert(-0x1p63, long.class));
        assertEquals(0, converter.convert(new BigDecimal("1e-999999999"), int.class));
    }

    /** The message names the value and the target's range; a huge BigDecimal is refused at once. */
    @Test
    void theMessageSaysWhyAndHugeValuesAreRefusedAtOnce() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> converter.convert((1L << 32) + 1, int.class));
        assertTrue(e.getMessage().contains("4294967297") && e.getMessage().contains("-2147483648")
                && e.getMessage().contains("2147483647"), e.getMessage());
        IllegalArgumentException nan = assertThrows(IllegalArgumentException.class,
                () -> converter.convert(Double.NaN, long.class));
        assertTrue(nan.getMessage().contains("NaN"), nan.getMessage());

        assertTimeout(java.time.Duration.ofMillis(500), () -> {
            assertThrows(IllegalArgumentException.class,
                    () -> converter.convert(new BigDecimal("1e999999999"), int.class));
            String message = assertThrows(IllegalArgumentException.class,
                    () -> converter.convert(BigInteger.ONE.shiftLeft(5_000_000), long.class)).getMessage();
            assertTrue(message.length() < 200, message.length() + " characters");
        });
    }
}
