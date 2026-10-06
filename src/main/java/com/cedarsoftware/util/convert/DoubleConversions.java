package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
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

import com.cedarsoftware.util.internal.DoubleDecimal;

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
final class DoubleConversions {
    private DoubleConversions() { }

    static Instant toInstant(Object from, Converter converter) {
        return toInstant(from, "Instant");
    }

    /**
     * {@code from} seconds as an Instant, for a conversion to {@code type}; NaN, infinity or out of range refused. The
     * seconds are the decimal the double prints, to the nearest nanosecond: 1700000000.123 is .123, where its binary
     * fraction gave .122999907.
     */
    private static Instant toInstant(Object from, String type) {
        double d = (Double) from;
        long seconds = units(d, 0, from, type);
        try {
            return Instant.ofEpochSecond(seconds, DoubleDecimal.printedNanos(d));
        } catch (DateTimeException | ArithmeticException e) {
            throw NumberConversions.outsideTimeRange(from, type);
        }
    }

    /**
     * {@code seconds} in whole seconds (scale 0) or milliseconds (scale 3), truncated toward zero, taken from the
     * decimal the double prints (1.001 seconds is 1001 milliseconds; the double times 1000 is 1000.9999999999999,
     * and 741 of the first 100,000 millisecond values lost one that way); NaN, infinity or beyond a long refused.
     */
    private static long units(double seconds, int scale, Object from, String type) {
        double scaled = scale == 0 ? seconds : seconds * 1000.0;
        long binary = NumberConversions.timeUnits(scaled, from, type);    // NaN, infinity, beyond a long: refused
        if (scaled > -0x1p62 && scaled < 0x1p62) {
            return DoubleDecimal.printedLong(seconds, scale);
        }
        // Near a long's ends: the decimal, unless only the double's own value fits -- Date(Long.MIN_VALUE) as seconds
        // is -9.223372036854776E15, whose decimal in milliseconds is just past a long; it comes back, as -2^63 does
        // to Long
        BigDecimal decimal = DoubleDecimal.printedDecimal(seconds).movePointRight(scale);
        long wholeDigits = (long) decimal.precision() - decimal.scale();
        return wholeDigits <= 19 && decimal.toBigInteger().bitLength() <= 63 ? decimal.longValue() : binary;
    }

    static Date toDate(Object from, Converter converter) {
        return new Date(units((Double) from, 3, from, "Date"));
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        double seconds = (Double) from;
        long whole = units(seconds, 0, from, "java.sql.Date");          // whole seconds, as before
        Instant instant;
        try {
            instant = Instant.ofEpochSecond(whole);
        } catch (DateTimeException e) {
            throw NumberConversions.outsideTimeRange(from, "java.sql.Date");
        }
        return java.sql.Date.valueOf(instant.atZone(converter.getOptions().getZoneId()).toLocalDate());
    }

    static Calendar toCalendar(Object from, Converter converter) {
        long epochMillis = units((Double) from, 3, from, "Calendar");
        Calendar calendar = Calendar.getInstance(converter.getOptions().getTimeZone());
        calendar.clear();
        calendar.setTimeInMillis(epochMillis);
        return calendar;
    }
    
    static LocalTime toLocalTime(Object from, Converter converter) {
        double seconds = (double) from;
        if (Double.isNaN(seconds)) {      // Math.round(NaN) is 0: midnight
            NumberConversions.timeUnits(seconds, from, "LocalTime");
        }
        // the decimal it prints, to the nearest nanosecond, as toInstant does (truncating made 1.001 s
        // 00:00:01.000999999); a value within half a nanosecond of midnight is the day's last nanosecond, not 24:00
        long nanos = seconds >= 0 && seconds < 86_400.0
                ? DoubleDecimal.printedLong(seconds) * 1_000_000_000L + DoubleDecimal.printedNanos(seconds)
                : Math.round(seconds * 1_000_000_000.0);                // out of range: refused below
        if (nanos == 86_400_000_000_000L && seconds < 86_400.0) {
            nanos--;
        }
        try {
            return LocalTime.ofNanoOfDay(nanos);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Input value [" + seconds + "] for conversion to LocalTime must be >= 0 && <= 86399.999999999", e);
        }
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDate();
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDateTime();
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return toInstant(from, "ZonedDateTime").atZone(converter.getOptions().getZoneId());
    }

    static OffsetTime toOffsetTime(Object from, Converter converter) {
        double seconds = (double) from;
        long wholeSecs = units(seconds, 0, from, "OffsetTime");        // whole seconds of the decimal it prints
        long nanos = DoubleDecimal.printedNanos(seconds);               // its fraction, to the nearest nanosecond

        try {
            Instant instant = Instant.ofEpochSecond(wholeSecs, nanos);
            return OffsetTime.ofInstant(instant, converter.getOptions().getZoneId());
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Input value [" + seconds + "] for conversion to LocalTime must be >= 0 && <= 86399.999999999", e);
        }
    }
    
    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        return toInstant(from, converter).atZone(converter.getOptions().getZoneId()).toOffsetDateTime();
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return Timestamp.from(toInstant(from, converter));
    }
    
    static Duration toDuration(Object from, Converter converter) {
        double d = (Double) from;
        // whole seconds and nanoseconds of the decimal it prints, rounded to the nanosecond as toInstant does
        long seconds = units(d, 0, from, "Duration");
        long nanoAdjustment = DoubleDecimal.printedNanos(d);
        try {
            return Duration.ofSeconds(seconds, nanoAdjustment);
        } catch (ArithmeticException e) {
            throw NumberConversions.outsideTimeRange(from, "Duration");
        }
    }
}
