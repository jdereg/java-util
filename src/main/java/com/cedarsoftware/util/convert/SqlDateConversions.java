package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

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
public class SqlDateConversions {

    static long toLong(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return sqlDate.toLocalDate()
                .atStartOfDay(converter.getOptions().getZoneId())
                .toInstant()
                .toEpochMilli();
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return new AtomicLong(sqlDate.toLocalDate()
                .atStartOfDay(converter.getOptions().getZoneId())
                .toInstant()
                .toEpochMilli());
    }

    static double toDouble(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return sqlDate.toLocalDate()
                .atStartOfDay(converter.getOptions().getZoneId())
                .toInstant()
                .toEpochMilli() / 1000.0;
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return BigInteger.valueOf(sqlDate.toLocalDate()
                        .atStartOfDay(converter.getOptions().getZoneId())
                        .toInstant()
                        .toEpochMilli())
                .multiply(BigIntegerConversions.MILLION);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return new BigDecimal(sqlDate.toLocalDate()
                .atStartOfDay(converter.getOptions().getZoneId())
                .toInstant()
                .toEpochMilli())
                .divide(BigDecimal.valueOf(1000), 9, RoundingMode.DOWN);
    }

    static Instant toInstant(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return sqlDate.toLocalDate()
                .atStartOfDay(converter.getOptions().getZoneId())
                .toInstant();
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return sqlDate.toLocalDate()
                .atStartOfDay(converter.getOptions().getZoneId())
                .toLocalDateTime();
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return sqlDate.toLocalDate()
                .atStartOfDay(converter.getOptions().getZoneId())
                .toOffsetDateTime();
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return sqlDate.toLocalDate()
                .atStartOfDay(converter.getOptions().getZoneId());
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return sqlDate.toLocalDate();
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return java.sql.Date.valueOf(sqlDate.toLocalDate());
    }

    static Date toDate(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return Date.from(sqlDate.toLocalDate()
                .atStartOfDay(converter.getOptions().getZoneId())
                .toInstant());
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        return Timestamp.from(sqlDate.toLocalDate()
                .atStartOfDay(converter.getOptions().getZoneId())
                .toInstant());
    }

    static Calendar toCalendar(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        ZonedDateTime zdt = sqlDate.toLocalDate()
                .atStartOfDay(converter.getOptions().getZoneId());
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(converter.getOptions().getZoneId()));
        cal.setTimeInMillis(zdt.toInstant().toEpochMilli());
        return cal;
    }

    static YearMonth toYearMonth(Object from, Converter converter) {
        return YearMonth.from(((java.sql.Date) from).toLocalDate());
    }

    static Year toYear(Object from, Converter converter) {
        return Year.from(((java.sql.Date) from).toLocalDate());
    }

    static MonthDay toMonthDay(Object from, Converter converter) {
        return MonthDay.from(((java.sql.Date) from).toLocalDate());
    }

    static String toString(Object from, Converter converter) {
        java.sql.Date sqlDate = (java.sql.Date) from;
        // java.sql.Date.toString() returns the date in "yyyy-MM-dd" format.
        return sqlDate.toString();
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        java.sql.Date date = (java.sql.Date) from;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(MapConversions.SQL_DATE, toString(date, converter));
        return map;
    }
}
