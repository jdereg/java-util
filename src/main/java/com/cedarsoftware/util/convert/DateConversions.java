package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kenny Partlow (kpartlow@gmail.com)
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
final class DateConversions {

    private DateConversions() {}
    
    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        Date date = (Date) from;
        return Instant.ofEpochMilli(date.getTime()).atZone(converter.getOptions().getZoneId());
    }

    static long toLong(Object from, Converter converter) {
        return ((Date) from).getTime();
    }

    static double toDouble(Object from, Converter converter) {
        Date date = (Date) from;
        return date.getTime() / 1000.0;
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return new java.sql.Date(toLong(from, converter));
    }

    static Date toDate(Object from, Converter converter) {
        return new Date(toLong(from, converter));
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return new Timestamp(toLong(from, converter));
    }

    static Calendar toCalendar(Object from, Converter converter) {
        return CalendarConversions.create(toLong(from, converter), converter);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Date date = (Date) from;
        long epochMillis = date.getTime();

        // Truncate decimal portion
        return new BigDecimal(epochMillis).divide(BigDecimalConversions.GRAND, 9, RoundingMode.DOWN);
    }

    static Instant toInstant(Object from, Converter converter) {
        Date date = (Date) from;
        if (date instanceof java.sql.Date) {
            return new java.util.Date(date.getTime()).toInstant();
        } else {
            return date.toInstant();
        }
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        return toInstant(from, converter).atZone(converter.getOptions().getZoneId()).toOffsetDateTime();
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDateTime();
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDate();
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);

        // Convert Instant to LocalDateTime
        LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, converter.getOptions().getZoneId());

        // Extract the LocalTime from LocalDateTime
        return localDateTime.toLocalTime();
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        return InstantConversions.toBigInteger(instant, converter);
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }

    static String sqlDateToString(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return toString(new Date(sqlDate.getTime()), converter);
    }

    static String toString(Object from, Converter converter) {
        Date date = (Date) from;

        // Convert Date to ZonedDateTime
        ZonedDateTime zonedDateTime = date.toInstant().atZone(converter.getOptions().getZoneId());

        // Build a formatter with optional milliseconds and always show timezone offset
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z") // Timezone offset
                .toFormatter();

        return zonedDateTime.format(formatter);
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        Date date = (Date) from;
        String formatted;
        Map<String, Object> map = new LinkedHashMap<>();

        if (date instanceof java.sql.Date) {
            // Convert millis to Instant then LocalDate in UTC
            LocalDate localDate = Instant.ofEpochMilli(date.getTime())
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();

            // Place that LocalDate at midnight in UTC, then format
            ZonedDateTime zdt = localDate.atStartOfDay(ZoneOffset.UTC);
            formatted = zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

            map.put(MapConversions.SQL_DATE, formatted);
        } else {
            // Regular util.Date - format with time
            ZonedDateTime zdt = date.toInstant().atZone(ZoneOffset.UTC);
            int ms = zdt.getNano() / 1_000_000;  // Convert nanos to millis
            if (ms == 0) {
                // No fractional seconds
                formatted = zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
            } else {
                // Millisecond precision
                formatted = zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                        + String.format(".%03dZ", ms);
            }
            map.put(MapConversions.DATE, formatted);
        }

        return map;
    }
}