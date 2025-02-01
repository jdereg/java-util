package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
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
        Calendar calendar = (Calendar) from;
        long epochMillis = calendar.getTime().getTime();
        return epochMillis / 1000.0;
    }
    
    static Date toDate(Object from, Converter converter) {
        return ((Calendar) from).getTime();
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return new java.sql.Date(((Calendar) from).getTime().getTime());
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return new Timestamp(((Calendar) from).getTimeInMillis());
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
        Calendar cal = (Calendar) from;
        long epochMillis = cal.getTime().getTime();
        return new BigDecimal(epochMillis).divide(BigDecimalConversions.GRAND);
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return BigInteger.valueOf(((Calendar) from).getTime().getTime() * 1_000_000L);
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
        Calendar cal = (Calendar) from;
        ZonedDateTime zdt = cal.toInstant().atZone(cal.getTimeZone().toZoneId());
        TimeZone tz = cal.getTimeZone();

        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";

        // Only use named zones for IANA timezone IDs
        String id = tz.getID();
        if (id.contains("/")) {  // IANA timezones contain "/"
            pattern += "['['VV']']";
        } else if ("GMT".equals(id) || "UTC".equals(id)) {
            pattern += "X";  // Z for UTC/GMT
        } else {
            pattern += "xxx";  // Offsets for everything else (EST, GMT+02:00, etc)
        }

        return zdt.format(DateTimeFormatter.ofPattern(pattern));
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        Calendar cal = (Calendar) from;
        OffsetDateTime offsetDateTime = cal.toInstant().atOffset(ZoneOffset.ofTotalSeconds(cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 1000));
        return offsetDateTime;
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.CALENDAR, toString(from, converter));
        return target;
    }
}
