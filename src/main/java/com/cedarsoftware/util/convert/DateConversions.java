package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

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

    static BigInteger toBigInteger(Object from, Converter converter) {
        Date date = (Date) from;
        return BigInteger.valueOf(date.getTime());
    }

    static String toString(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        return MILLIS_FMT.format(instant);
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        Map<String, Object> map = new LinkedHashMap<>();
        // Regular util.Date - format with time
        map.put(MapConversions.DATE, toString(from, converter));
        return map;
    }
}
