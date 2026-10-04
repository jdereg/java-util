package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import com.cedarsoftware.util.MathUtilities;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
final class BigDecimalConversions {
    static final BigDecimal BILLION = BigDecimal.valueOf(1_000_000_000);
    static final BigDecimal GRAND = BigDecimal.valueOf(1000);

    private BigDecimalConversions() { }

    static Calendar toCalendar(Object from, Converter converter) {
        BigDecimal seconds = (BigDecimal) from;
        BigDecimal millis = seconds.multiply(GRAND);
        Calendar calendar = Calendar.getInstance(converter.getOptions().getTimeZone());
        calendar.setTimeInMillis(NumberConversions.timeUnits(millis, from, "Calendar"));
        return calendar;
    }

    static Instant toInstant(Object from, Converter converter) {
        BigDecimal seconds = (BigDecimal) from;
        long whole = NumberConversions.timeUnits(seconds, from, "Instant");    // longValue() wrapped 1e20
        try {
            return Instant.ofEpochSecond(whole, fractionNanos(seconds, whole));
        } catch (DateTimeException | ArithmeticException e) {
            throw NumberConversions.outsideTimeRange(from, "Instant");
        }
    }

    static Duration toDuration(Object from, Converter converter) {
        BigDecimal seconds = (BigDecimal) from;
        long whole = NumberConversions.timeUnits(seconds, from, "Duration");
        try {
            return Duration.ofSeconds(whole, fractionNanos(seconds, whole));
        } catch (ArithmeticException e) {
            throw NumberConversions.outsideTimeRange(from, "Duration");
        }
    }

    /**
     * The nanoseconds in {@code seconds} beyond its {@code whole} seconds, truncated, as remainder(1) gave them;
     * counted by digits, so a tiny fraction (1e-999999999) is 0 at once rather than a division by 10^999999999.
     */
    private static long fractionNanos(BigDecimal seconds, long whole) {
        return NumberConversions.timeUnits(seconds.subtract(BigDecimal.valueOf(whole)).movePointRight(9), seconds,
                "nanoseconds");
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        BigDecimal seconds = (BigDecimal) from;
        BigDecimal nanos = seconds.multiply(BILLION);
        try {
            return LocalTime.ofNanoOfDay(NumberConversions.timeUnits(nanos, from, "LocalTime"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Input value [" + plain(seconds, converter) + "] for conversion to LocalTime must be >= 0 && <= 86399.999999999", e);
        }
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDate();
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDateTime();
    }

    static OffsetTime toOffsetTime(Object from, Converter converter) {
        BigDecimal seconds = (BigDecimal) from;
        try {
            long wholeSecs = NumberConversions.timeUnits(seconds, from, "OffsetTime"); // the integer part
            BigDecimal frac = seconds.subtract(BigDecimal.valueOf(wholeSecs)); // gets just the fractional part
            long nanos = NumberConversions.timeUnits(frac.movePointRight(9), from, "OffsetTime"); // fraction to nanos

            Instant instant = Instant.ofEpochSecond(wholeSecs, nanos);
            return OffsetTime.ofInstant(instant, converter.getOptions().getZoneId());
        } catch (Exception e) {
            throw new IllegalArgumentException("Input value [" + plain(seconds, converter) + "] for conversion to LocalTime must be >= 0 && <= 86399.999999999", e);
        }
    }
    
    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toOffsetDateTime();
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return toInstant(from, converter).atZone(converter.getOptions().getZoneId());
    }

    static Date toDate(Object from, Converter converter) {
        return Date.from(toInstant(from, converter));
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        // Convert the Instant to a LocalDate using the converter's zoneId.
        LocalDate ld = instant.atZone(converter.getOptions().getZoneId()).toLocalDate();
        // Return a java.sql.Date that represents that LocalDate (normalized to midnight).
        return java.sql.Date.valueOf(ld);
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return Timestamp.from(toInstant(from, converter));
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return wholePart((BigDecimal) from, converter);
    }

    /**
     * {@code value}'s whole part, as toBigInteger() gives it; 0 at once below 1 in magnitude, where toBigInteger()
     * divides by 10 to the power of the scale (minutes for 1e-999999999). A whole part longer than
     * {@link ConverterOptions#getMaxPlainDigits()} allows (and than the value's own digits) is refused from its digit
     * count, without building it: 1e999999999 is about 415 MB as a BigInteger.
     */
    static BigInteger wholePart(BigDecimal value, Converter converter) {
        long wholeDigits = (long) value.precision() - value.scale();
        if (wholeDigits <= 0) {
            return BigInteger.ZERO;
        }
        int maxDigits = converter.getOptions().getMaxPlainDigits();
        if (wholeDigits > Math.max(maxDigits, value.precision())) {
            throw new IllegalArgumentException("Unable to convert " + value + " to a whole number: it would take "
                    + wholeDigits + " digits, more than " + maxDigits + " (ConverterOptions.getMaxPlainDigits())");
        }
        return value.toBigInteger();
    }

    /** {@code value} for text: plain digits, or its exponent form past the Converter's plain-digit limit. */
    static String plain(BigDecimal value, Converter converter) {
        return MathUtilities.toPlainString(value, converter.getOptions().getMaxPlainDigits());
    }

    /**
     * The BigDecimal's own plain-string form, scale intact.
     * <p>
     * This used to call {@code stripTrailingZeros()} first, which normalized 1.50 to "1.5". Scale is information on
     * a BigDecimal -- it is what separates a money amount written to the cent from the same quantity written to the
     * tenth -- and discarding it broke round-tripping: BigDecimal -> String -> BigDecimal returned a value that was
     * not {@code equals()} to the original, because BigDecimal equality includes scale.
     * <p>
     * {@code toPlainString()} rather than {@code toString()} so a large or small scale does not come back in
     * scientific notation, which is the property the previous implementation was really relying on -- up to
     * {@link ConverterOptions#getMaxPlainDigits()} digits (1,000 by default; money is far inside it). Past that the
     * exponent form is written: {@code 1E+999999999} written out is a billion digits.
     */
    static String toString(Object from, Converter converter) {
        BigDecimal value = (BigDecimal) from;
        int scale = value.scale();
        if (scale >= 0 && scale < converter.getOptions().getMaxPlainDigits()) {
            return value.toPlainString();       // the common case, called here directly: JDK 8 inlines it at this depth
        }
        return plain(value, converter);
    }

    static UUID toUUID(Object from, Converter converter) {
        BigInteger bigInt = wholePart((BigDecimal) from, converter);
        return BigIntegerConversions.toUUID(bigInt, converter);
    }

    static BigDecimal secondsAndNanosToDouble(long seconds, long nanos) {
        return BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9));
    }
}
