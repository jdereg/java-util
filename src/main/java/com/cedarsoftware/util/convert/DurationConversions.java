package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.DURATION;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
final class DurationConversions {
    
    // Feature option constants for Duration precision control
    public static final String DURATION_LONG_PRECISION = "duration.long.precision";

    private DurationConversions() {}

    static Map toMap(Object from, Converter converter) {
        Duration duration = (Duration) from;
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(DURATION, duration.toString());
        return target;
    }

    static long toLong(Object from, Converter converter) {
        Duration duration = (Duration) from;
        
        // Check for precision override (system property takes precedence)
        String systemPrecision = System.getProperty("cedarsoftware.converter." + DURATION_LONG_PRECISION);
        String precision = systemPrecision;
        
        // Fall back to converter options if no system property
        if (precision == null) {
            precision = converter.getOptions().getCustomOption(DURATION_LONG_PRECISION);
        }
        
        // Default to milliseconds if no override specified
        if (Converter.PRECISION_NANOS.equals(precision)) {
            return duration.toNanos();
        } else {
            return duration.toMillis(); // Default: milliseconds
        }
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }
    
    static BigInteger toBigInteger(Object from, Converter converter) {
        Duration duration = (Duration) from;
        BigInteger epochSeconds = BigInteger.valueOf(duration.getSeconds());
        BigInteger nanos = BigInteger.valueOf(duration.getNano());

        // Convert seconds to nanoseconds and add the nanosecond part
        return epochSeconds.multiply(BigIntegerConversions.BILLION).add(nanos);
    }

    static double toDouble(Object from, Converter converter) {
        Duration duration = (Duration) from;
        return BigDecimalConversions.secondsAndNanosToDouble(duration.getSeconds(), duration.getNano()).doubleValue();
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Duration duration = (Duration) from;
        return BigDecimalConversions.secondsAndNanosToDouble(duration.getSeconds(), duration.getNano());
    }
    
    static Timestamp toTimestamp(Object from, Converter converter) {
        Duration duration = (Duration) from;
        Instant epoch = Instant.EPOCH;
        Instant timeAfterDuration = epoch.plus(duration);
        return Timestamp.from(timeAfterDuration);
    }
    
    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return java.sql.Date.valueOf(toLocalDate(from, converter));
    }
    
    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toOffsetDateTime();
    }

    static boolean toBoolean(Object from, Converter converter) {
        Duration duration = (Duration) from;
        return !duration.isZero();
    }

    static Boolean toBooleanWrapper(Object from, Converter converter) {
        return toBoolean(from, converter);
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return new AtomicBoolean(toBoolean(from, converter));
    }

    static Calendar toCalendar(Object from, Converter converter) {
        Duration duration = (Duration) from;
        // Add duration to epoch to get the target instant
        Instant epoch = Instant.EPOCH;
        Instant timeAfterDuration = epoch.plus(duration);
        
        Calendar calendar = Calendar.getInstance(converter.getOptions().getTimeZone());
        calendar.setTimeInMillis(timeAfterDuration.toEpochMilli());
        return calendar;
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        Duration duration = (Duration) from;
        // Add duration to epoch and convert to LocalDate in system timezone
        Instant epoch = Instant.EPOCH;
        Instant timeAfterDuration = epoch.plus(duration);
        return timeAfterDuration.atZone(converter.getOptions().getZoneId()).toLocalDate();
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        Duration duration = (Duration) from;
        // Convert duration to time within a day (modulo 24 hours)
        long totalSeconds = duration.getSeconds();
        int nanos = duration.getNano();
        
        // Handle negative durations by getting the equivalent positive time within a day
        long secondsInDay = 24 * 60 * 60; // 86400 seconds in a day
        long adjustedSeconds = ((totalSeconds % secondsInDay) + secondsInDay) % secondsInDay;
        
        return LocalTime.ofSecondOfDay(adjustedSeconds).withNano(nanos);
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        Duration duration = (Duration) from;
        // Add duration to epoch and convert to LocalDateTime in system timezone
        Instant epoch = Instant.EPOCH;
        Instant timeAfterDuration = epoch.plus(duration);
        return timeAfterDuration.atZone(converter.getOptions().getZoneId()).toLocalDateTime();
    }


    static Date toDate(Object from, Converter converter) {
        Duration duration = (Duration) from;
        // Add duration to epoch to get the target instant
        Instant epoch = Instant.EPOCH;
        Instant timeAfterDuration = epoch.plus(duration);
        return Date.from(timeAfterDuration);
    }

    static Instant toInstant(Object from, Converter converter) {
        Duration duration = (Duration) from;
        // Add duration to epoch to get the target instant
        Instant epoch = Instant.EPOCH;
        return epoch.plus(duration);
    }

    static Number toNumber(Object from, Converter converter) {
        Duration duration = (Duration) from;
        // Return duration as milliseconds (Long is a Number)
        return duration.toMillis();
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        Duration duration = (Duration) from;
        // Add duration to epoch and convert to ZonedDateTime in system timezone
        Instant epoch = Instant.EPOCH;
        Instant timeAfterDuration = epoch.plus(duration);
        return timeAfterDuration.atZone(converter.getOptions().getZoneId());
    }
}
