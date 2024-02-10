package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
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
final class OffsetDateTimeConversions {
    private OffsetDateTimeConversions() {}

    static OffsetDateTime toDifferentZone(Object from, Converter converter) {
        OffsetDateTime offsetDateTime = (OffsetDateTime) from;
        return offsetDateTime.toInstant().atZone(converter.getOptions().getZoneId()).toOffsetDateTime();
    }

    static Instant toInstant(Object from, Converter converter) {
        return ((OffsetDateTime)from).toInstant();
    }

    static long toLong(Object from, Converter converter) {
        return toInstant(from, converter).toEpochMilli();
    }
    
    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toDifferentZone(from, converter).toLocalDateTime();
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return toDifferentZone(from, converter).toLocalDate();
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        return toDifferentZone(from, converter).toLocalTime();
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return new Timestamp(toLong(from, converter));
    }

    static Calendar toCalendar(Object from, Converter converter) {
        Calendar calendar = Calendar.getInstance(converter.getOptions().getTimeZone());
        calendar.setTimeInMillis(toLong(from, converter));
        return calendar;
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return new java.sql.Date(toLong(from, converter));
    }

    static Date toDate(Object from, Converter converter) {
        return new Date(toLong(from, converter));
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return BigInteger.valueOf(toLong(from, converter));
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        return BigDecimal.valueOf(toLong(from, converter));
    }

    static OffsetTime toOffsetTime(Object from, Converter converter) {
        OffsetDateTime dateTime = (OffsetDateTime) from;
        return dateTime.toOffsetTime();
    }

    static String toString(Object from, Converter converter) {
        OffsetDateTime offsetDateTime = (OffsetDateTime) from;
        return offsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
