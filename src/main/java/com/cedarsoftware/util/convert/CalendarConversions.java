package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import com.cedarsoftware.util.DateUtilities;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
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

    private static final DateTimeFormatter OFFSET_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendPattern("XXX")
            .toFormatter();

    private static final DateTimeFormatter ZONE_FMT = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .appendPattern("XXX'['VV']'")
            .toFormatter();

    private CalendarConversions() {}

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        long epochMillis = ((Calendar) from).getTimeInMillis();
        return new BigDecimal(epochMillis).divide(BigDecimal.valueOf(1000));
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return BigInteger.valueOf(((Calendar) from).getTimeInMillis());
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return java.sql.Date.valueOf(toZonedDateTime(from, converter).toLocalDate());
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        Calendar calendar = (Calendar)from;
        return calendar.toInstant().atZone(calendar.getTimeZone().toZoneId());
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDateTime();
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDate();
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

    static Year toYear(Object from, Converter converter) {
        return Year.from(toZonedDateTime(from, converter).toLocalDate());
    }

    static YearMonth toYearMonth(Object from, Converter converter) {
        return YearMonth.from(toZonedDateTime(from, converter).toLocalDate());
    }

    static MonthDay toMonthDay(Object from, Converter converter) {
        return MonthDay.from(toZonedDateTime(from, converter).toLocalDate());
    }

    static String toString(Object from, Converter converter) {
        ZonedDateTime zdt = toZonedDateTime(from, converter);
        String zoneId = zdt.getZone().getId();

        // If the zoneId does NOT contain "/", assume it's an abbreviation.
        if (!zoneId.contains("/")) {
            String fullZone = DateUtilities.ABBREVIATION_TO_TIMEZONE.get(zoneId);
            if (fullZone != null) {
                // Adjust the ZonedDateTime to use the full zone name.
                zdt = zdt.withZoneSameInstant(ZoneId.of(fullZone));
            }
        }

        if (zdt.getZone() instanceof ZoneOffset) {
            return OFFSET_FMT.format(zdt);
        } else {
            return ZONE_FMT.format(zdt);
        }
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(MapConversions.CALENDAR, toString(from, converter));
        return target;
    }
}