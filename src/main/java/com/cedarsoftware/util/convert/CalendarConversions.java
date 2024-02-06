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
public final class CalendarConversions {

    private CalendarConversions() {}

    static Date toDate(Object from) {
        return ((Calendar)from).getTime();
    }

    static long toLong(Object from) {
        return toDate(from).getTime();
    }

    static Instant toInstant(Object from) {
        return ((Calendar)from).toInstant();
    }

    static ZonedDateTime toZonedDateTime(Object from, ConverterOptions options) {
        return toInstant(from).atZone(options.getZoneId());
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options);
    }

    static Long toLong(Object from, Converter converter, ConverterOptions options) {
        return toLong(from);
    }

    static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return (double)toLong(from);
    }


    static Date toDate(Object from, Converter converter, ConverterOptions options) {
        return toDate(from);
    }

    static java.sql.Date toSqlDate(Object from, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(from));
    }

    static Timestamp toTimestamp(Object from, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(from));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(from));
    }

    static Instant toInstant(Object from, Converter converter, ConverterOptions options) {
        return toInstant(from);
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options).toLocalDateTime();
    }

    static LocalDate toLocalDate(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options).toLocalDate();
    }

    static LocalTime toLocalTime(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options).toLocalTime();
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(from));
    }

    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(from));
    }

    static Calendar clone(Object from, Converter converter, ConverterOptions options) {
        Calendar calendar = (Calendar)from;
        // mutable class, so clone it.
        return (Calendar)calendar.clone();
    }

    static Calendar create(long epochMilli, ConverterOptions options) {
        Calendar cal = Calendar.getInstance(options.getTimeZone());
        cal.clear();
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    static String toString(Object from, Converter converter, ConverterOptions options) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        simpleDateFormat.setTimeZone(options.getTimeZone());
        return simpleDateFormat.format(((Calendar) from).getTime());
    }
}
