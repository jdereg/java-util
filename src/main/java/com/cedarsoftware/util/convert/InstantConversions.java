package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.CompactLinkedMap;

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
public class InstantConversions {
    
    static long toLong(Object from) {
        return ((Instant)from).toEpochMilli();
    }

    static Map toMap(Object from, Converter converter, ConverterOptions options) {
        long sec = ((Instant) from).getEpochSecond();
        long nanos = ((Instant) from).getNano();
        Map<String, Object> target = new CompactLinkedMap<>();
        target.put("seconds", sec);
        target.put("nanos", nanos);
        return target;
    }
    static ZonedDateTime toZonedDateTime(Object from, ConverterOptions options) {
        return ((Instant)from).atZone(options.getZoneId());
    }

    static long toLong(Object from, Converter converter, ConverterOptions options) {
        return toLong(from);
    }

    static float toFloat(Object from, Converter converter, ConverterOptions options) {
        return toLong(from);
    }

    static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return toLong(from);
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(from));
    }

    static Timestamp toTimestamp(Object from, Converter converter, ConverterOptions options) {
        return new Timestamp(toLong(from));
    }
    
    static java.sql.Date toSqlDate(Object from, Converter converter, ConverterOptions options) {
        return new java.sql.Date(toLong(from));
    }

    static Date toDate(Object from, Converter converter, ConverterOptions options) {
        return new Date(toLong(from));
    }

    static Calendar toCalendar(Object from, Converter converter, ConverterOptions options) {
        return CalendarConversions.create(toLong(from), options);
    }
    
    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        return BigInteger.valueOf(toLong(from));
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(toLong(from));
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
}
