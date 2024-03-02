package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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
        double d = (Double) from;
        long seconds = (long) d;
        long nanoAdjustment = (long) ((d - seconds) * 1_000_000_000L);
        return Instant.ofEpochSecond(seconds, nanoAdjustment);
    }

    static Date toDate(Object from, Converter converter) {
        double d = (Double) from;
        return new Date((long)(d * 1000));
    }

    static Date toSqlDate(Object from, Converter converter) {
        double d = (Double) from;
        return new java.sql.Date((long)(d * 1000));
    }

    static Calendar toCalendar(Object from, Converter converter) {
        double seconds = (double) from;
        long epochMillis = (long)(seconds * 1000);
        Calendar calendar = GregorianCalendar.getInstance(converter.getOptions().getTimeZone());
        calendar.clear();
        calendar.setTimeInMillis(epochMillis);
        return calendar;
    }
    
    static LocalTime toLocalTime(Object from, Converter converter) {
        double seconds = (double) from;
        double nanos = seconds * 1_000_000_000.0;
        try {
            return LocalTime.ofNanoOfDay((long)nanos);
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
        return toInstant(from, converter).atZone(converter.getOptions().getZoneId());
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
        long seconds = (long) d;
        long nanoAdjustment = (long) ((d - seconds) * 1_000_000_000L);
        return Duration.ofSeconds(seconds, nanoAdjustment);
    }
}
