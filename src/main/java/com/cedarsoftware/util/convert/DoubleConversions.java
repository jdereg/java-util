package com.cedarsoftware.util.convert;

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

    /** {@code from} seconds as an Instant, for a conversion to {@code type}; NaN, infinity or out of range refused. */
    private static Instant toInstant(Object from, String type) {
        double d = (Double) from;
        long seconds = NumberConversions.timeUnits(d, from, type);
        // Calculate nanoseconds by taking the fractional part of the double and multiplying by 1_000_000_000,
        // rounding to the nearest long to maintain precision.
        long nanos = Math.round((d - seconds) * 1_000_000_000);
        try {
            return Instant.ofEpochSecond(seconds, nanos);
        } catch (DateTimeException | ArithmeticException e) {
            throw NumberConversions.outsideTimeRange(from, type);
        }
    }

    static Date toDate(Object from, Converter converter) {
        double d = (Double) from;
        return new Date(NumberConversions.timeUnits(d * 1000, from, "Date"));
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        double seconds = (Double) from;
        long whole = NumberConversions.timeUnits(seconds, from, "java.sql.Date");   // whole seconds, as before
        Instant instant;
        try {
            instant = Instant.ofEpochSecond(whole);
        } catch (DateTimeException e) {
            throw NumberConversions.outsideTimeRange(from, "java.sql.Date");
        }
        return java.sql.Date.valueOf(instant.atZone(converter.getOptions().getZoneId()).toLocalDate());
    }

    static Calendar toCalendar(Object from, Converter converter) {
        double seconds = (double) from;
        long epochMillis = NumberConversions.timeUnits(seconds * 1000, from, "Calendar");
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
        // rounded to the nearest nanosecond, as toInstant does (truncating made 1.001 s 00:00:01.000999999); a value
        // within half a nanosecond of midnight is the day's last nanosecond, not 24:00
        long nanos = Math.round(seconds * 1_000_000_000.0);
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
        long wholeSecs = NumberConversions.timeUnits(seconds, from, "OffsetTime");  // whole number of seconds
        double frac = seconds - wholeSecs;  // gets just the fractional part
        long nanos = Math.round(frac * 1_000_000_000.0);  // fraction to nanos, rounded as toInstant does

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
        // Separate whole seconds and nanoseconds
        long seconds = NumberConversions.timeUnits(d, from, "Duration");
        long nanoAdjustment = Math.round((d - seconds) * 1_000_000_000L);    // rounded, as toInstant does
        try {
            return Duration.ofSeconds(seconds, nanoAdjustment);
        } catch (ArithmeticException e) {
            throw NumberConversions.outsideTimeRange(from, "Duration");
        }
    }
}
