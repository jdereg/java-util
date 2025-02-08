package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

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
final class TimestampConversions {
    private TimestampConversions() {}

    static double toDouble(Object from, Converter converter) {
        Duration d = toDuration(from, converter);
        return BigDecimalConversions.secondsAndNanosToDouble(d.getSeconds(), d.getNano()).doubleValue();
    }
    
    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Instant instant = timestamp.toInstant();
        return InstantConversions.toBigDecimal(instant, converter);
    }
    
    static BigInteger toBigInteger(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Instant instant = timestamp.toInstant();
        return InstantConversions.toBigInteger(instant, converter);
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        return timestamp.toInstant().atZone(converter.getOptions().getZoneId()).toLocalDateTime();
    }
    
    static Duration toDuration(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Instant timestampInstant = timestamp.toInstant();
        return Duration.between(Instant.EPOCH, timestampInstant);
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        ZonedDateTime zdt = ZonedDateTime.ofInstant(timestamp.toInstant(), converter.getOptions().getZoneId());
        return zdt.toOffsetDateTime();
    }

    static Calendar toCalendar(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Calendar cal = Calendar.getInstance(converter.getOptions().getTimeZone());
        cal.setTimeInMillis(timestamp.getTime());
        return cal;
    }

    static Date toDate(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        Instant instant = timestamp.toInstant();
        return Date.from(instant);
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return java.sql.Date.valueOf(
                ((Timestamp) from).toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate()
        );
    }

    static long toLong(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        return timestamp.getTime();
    }

    static Year toYear(Object from, Converter converter) {
        return Year.from(
                ((Timestamp) from).toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate()
        );
    }

    static YearMonth toYearMonth(Object from, Converter converter) {
        return YearMonth.from(
                ((Timestamp) from).toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate()
        );
    }

    static MonthDay toMonthDay(Object from, Converter converter) {
        return MonthDay.from(
                ((Timestamp) from).toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate()
        );
    }
    
    static String toString(Object from, Converter converter) {
        Timestamp timestamp = (Timestamp) from;
        int nanos = timestamp.getNanos();

        // Decide whether we need 3 decimals or 9 decimals
        final String pattern;
        if (nanos % 1_000_000 == 0) {
            // Exactly millisecond precision
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        } else {
            // Nanosecond precision
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'";
        }

        // Format the Timestamp in UTC using the chosen pattern
        return timestamp
                .toInstant()
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(pattern));
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        String formatted = toString(from, converter);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.TIMESTAMP, formatted);
        return map;
    }
}