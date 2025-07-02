package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
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
    static final DateTimeFormatter MILLIS_FMT = new DateTimeFormatterBuilder()
            .appendInstant(3)  // Force exactly 3 decimal places
            .toFormatter();

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
        return java.sql.Date.valueOf(
                ((Date) from).toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate()
        );
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
        Date date = (Date) from;
        return BigInteger.valueOf(date.getTime());
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }

    static Year toYear(Object from, Converter converter) {
        return Year.from(
                ((Date) from).toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate()
        );
    }

    static YearMonth toYearMonth(Object from, Converter converter) {
        return YearMonth.from(
                ((Date) from).toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate()
        );
    }

    static MonthDay toMonthDay(Object from, Converter converter) {
        return MonthDay.from(
                ((Date) from).toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate()
        );
    }
    
    static String toString(Object from, Converter converter) {
        Date date = (Date) from;
        Instant instant = date.toInstant();   // Convert legacy Date to Instant
        return MILLIS_FMT.format(instant);
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        Map<String, Object> map = new LinkedHashMap<>();
        // Regular util.Date - format with time
        map.put(MapConversions.DATE, toString(from, converter));
        return map;
    }
}