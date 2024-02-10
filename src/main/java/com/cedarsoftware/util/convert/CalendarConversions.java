package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
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
final class CalendarConversions {

    private CalendarConversions() {}

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        Calendar calendar = (Calendar)from;
        return calendar.toInstant().atZone(calendar.getTimeZone().toZoneId());
    }

    static Long toLong(Object from, Converter converter) {
        return ((Calendar) from).getTime().getTime();
    }

    static double toDouble(Object from, Converter converter) {
        return (double)toLong(from, converter);
    }
    
    static Date toDate(Object from, Converter converter) {
        return ((Calendar) from).getTime();
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return new java.sql.Date(((Calendar) from).getTime().getTime());
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return new Timestamp(((Calendar) from).getTime().getTime());
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(((Calendar) from).getTime().getTime());
    }

    static Instant toInstant(Object from, Converter converter) {
        Calendar calendar = (Calendar) from;
        return calendar.toInstant();
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

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        return BigDecimal.valueOf(((Calendar) from).getTime().getTime());
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return BigInteger.valueOf(((Calendar) from).getTime().getTime());
    }

    static Calendar clone(Object from, Converter converter) {
        Calendar calendar = (Calendar)from;
        // mutable class, so clone it.
        return (Calendar)calendar.clone();
    }

    static Calendar create(long epochMilli, Converter converter) {
        Calendar cal = Calendar.getInstance(converter.getOptions().getTimeZone());
        cal.clear();
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    static String toString(Object from, Converter converter) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        simpleDateFormat.setTimeZone(converter.getOptions().getTimeZone());
        return simpleDateFormat.format(((Calendar) from).getTime());
    }
}
