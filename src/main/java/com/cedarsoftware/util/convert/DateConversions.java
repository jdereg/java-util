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
public class DateConversions {

    static long toLong(Object from) {
        return ((Date) from).getTime();
    }

    static Instant toInstant(Object from) {
        return Instant.ofEpochMilli(toLong(from));
    }

    static ZonedDateTime toZonedDateTime(Object from, ConverterOptions options) {
        return toInstant(from).atZone(options.getZoneId());
    }

    static long toLong(Object from, Converter converter, ConverterOptions options) {
        return toLong(from);
    }

    static java.sql.Date toSqlDate(Object from, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(from));
    }

    static Date toDate(Object from, Converter converter, ConverterOptions options) {
        return new Date(toLong(from));
    }

    static Timestamp toTimestamp(Object from, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(from));
    }

    static Calendar toCalendar(Object from, Converter converter, ConverterOptions options) {
        return CalendarConversions.create(toLong(from), options);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(from));
    }

    static Instant toInstant(Object from, Converter converter, ConverterOptions options) {
        return toInstant(from);
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options);
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

    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(from));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(from));
    }

    static String dateToString(Object from, Converter converter, ConverterOptions options) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return simpleDateFormat.format(((Date) from));
    }

    static String sqlDateToString(Object from, Converter converter, ConverterOptions options) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return simpleDateFormat.format(((Date) from));
    }

    static String timestampToString(Object from, Converter converter, ConverterOptions options) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return simpleDateFormat.format(((Date) from));
    }

    static String calendarToString(Object from, Converter converter, ConverterOptions options) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        return simpleDateFormat.format(((Calendar) from).getTime());
    }

    static String localDateToString(Object from, Converter converter, ConverterOptions options) {
        LocalDate localDate = (LocalDate) from;
        return localDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    static String localTimeToString(Object from, Converter converter, ConverterOptions options) {
        LocalTime localTime = (LocalTime) from;
        return localTime.format(DateTimeFormatter.ISO_LOCAL_TIME);
    }

    static String localDateTimeToString(Object from, Converter converter, ConverterOptions options) {
        LocalDateTime localDateTime = (LocalDateTime) from;
        return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    static String zonedDateTimeToString(Object from, Converter converter, ConverterOptions options) {
        ZonedDateTime zonedDateTime = (ZonedDateTime) from;
        return zonedDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }
}
