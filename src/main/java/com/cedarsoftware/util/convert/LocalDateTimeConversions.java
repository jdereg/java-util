package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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
final class LocalDateTimeConversions {

    private LocalDateTimeConversions() {}

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return ((LocalDateTime)from).atZone(converter.getOptions().getZoneId());
    }

    static Instant toInstant(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toInstant();
    }

    static long toLong(Object from, Converter converter) {
        return toInstant(from, converter).toEpochMilli();
    }

    static double toDouble(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        return InstantConversions.toDouble(instant, converter);
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDateTime();
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDate();
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalTime();
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return new Timestamp(toLong(from, converter));
    }

    static Calendar toCalendar(Object from, Converter converter) {
        ZonedDateTime time = toZonedDateTime(from, converter);
        GregorianCalendar calendar = new GregorianCalendar(converter.getOptions().getTimeZone());
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
        LocalDateTime localDateTime = (LocalDateTime) from;
        return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
