package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.CompactLinkedMap;

import static com.cedarsoftware.util.convert.Converter.VALUE;

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
final class LocalDateConversions {

    private LocalDateConversions() {}

    static Instant toInstant(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toInstant();
    }

    static long toLong(Object from, Converter converter) {
        return toInstant(from, converter).toEpochMilli();
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDateTime();
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        ZoneId zoneId = converter.getOptions().getZoneId();
        return ((LocalDate) from).atStartOfDay(zoneId);
    }
    
    static double toDouble(Object from, Converter converter) {
        return toInstant(from, converter).toEpochMilli() / 1000d;
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        LocalDate localDate = (LocalDate) from;
        return new Timestamp(localDate.toEpochDay() * 86400 * 1000);
    }

    static Calendar toCalendar(Object from, Converter converter) {
        ZonedDateTime time = toZonedDateTime(from, converter);
        Calendar calendar = Calendar.getInstance(converter.getOptions().getTimeZone());
        calendar.setTimeInMillis(time.toInstant().toEpochMilli());
        return calendar;
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return new java.sql.Date(toLong(from, converter));
    }

    static Date toDate(Object from, Converter converter) {
        return new Date(toLong(from, converter));
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        return InstantConversions.toBigInteger(instant, converter);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        return InstantConversions.toBigDecimal(instant, converter);
    }

    static String toString(Object from, Converter converter) {
        LocalDate localDate = (LocalDate) from;
        return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        LocalDate localDate = (LocalDate) from;
        Map<String, Object> target = new CompactLinkedMap<>();
        target.put(VALUE, localDate.toString());
        return target;
    }
}
