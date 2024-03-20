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
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
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
final class InstantConversions {

    private InstantConversions() {}
    
    static Map toMap(Object from, Converter converter) {
        long sec = ((Instant) from).getEpochSecond();
        int nanos = ((Instant) from).getNano();
        Map<String, Object> target = new CompactLinkedMap<>();
        target.put("seconds", sec);
        target.put("nanos", nanos);
        return target;
    }
    
    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return ((Instant)from).atZone(converter.getOptions().getZoneId());
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        Instant instant = (Instant) from;
        TimeZone timeZone = converter.getOptions().getTimeZone();
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(timeZone.getOffset(System.currentTimeMillis()) / 1000);
        return instant.atOffset(zoneOffset);
    }

    static long toLong(Object from, Converter converter) {
        return ((Instant) from).toEpochMilli();
    }
    
    /**
     * @return double number of seconds.  The fractional part represents sub-second precision, with
     * nanosecond level support.
     */
    static double toDouble(Object from, Converter converter) {
        Instant instant = (Instant) from;
        return BigDecimalConversions.secondsAndNanosToDouble(instant.getEpochSecond(), instant.getNano()).doubleValue();
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return Timestamp.from((Instant) from);
    }
    
    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return new java.sql.Date(toLong(from, converter));
    }

    static Date toDate(Object from, Converter converter) {
        return new Date(toLong(from, converter));
    }

    static Calendar toCalendar(Object from, Converter converter) {
        return CalendarConversions.create(toLong(from, converter), converter);
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        Instant instant = (Instant) from;
        // Get seconds and nanoseconds from the Instant
        long seconds = instant.getEpochSecond();
        int nanoseconds = instant.getNano();

        // Convert the entire time to nanoseconds
        return BigInteger.valueOf(seconds).multiply(BigIntegerConversions.BILLION).add(BigInteger.valueOf(nanoseconds));
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Instant instant = (Instant) from;
        return BigDecimalConversions.secondsAndNanosToDouble(instant.getEpochSecond(), instant.getNano());
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
}
