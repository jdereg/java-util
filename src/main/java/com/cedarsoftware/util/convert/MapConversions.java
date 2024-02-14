package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.CompactLinkedMap;
import com.cedarsoftware.util.Convention;

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
final class MapConversions {
    private static final String V = "_v";
    private static final String VALUE = "value";
    private static final String DATE = "date";
    private static final String TIME = "time";
    private static final String ZONE = "zone";
    private static final String YEAR = "year";
    private static final String YEARS = "years";
    private static final String MONTH = "month";
    private static final String MONTHS = "months";
    private static final String DAY = "day";
    private static final String DAYS = "days";
    private static final String HOUR = "hour";
    private static final String HOURS = "hours";
    private static final String MINUTE = "minute";
    private static final String MINUTES = "minutes";
    private static final String SECOND = "second";
    private static final String SECONDS = "seconds";
    private static final String NANO = "nano";
    private static final String NANOS = "nanos";
    private static final String OFFSET_HOUR = "offsetHour";
    private static final String OFFSET_MINUTE = "offsetMinute";
    private static final String MOST_SIG_BITS = "mostSigBits";
    private static final String LEAST_SIG_BITS = "leastSigBits";

    static final String OFFSET = "offset";

    private static final String TOTAL_SECONDS = "totalSeconds";

    static final String DATE_TIME = "dateTime";

    private static final String ID = "id";
    public static final String LANGUAGE = "language";
    public static final String VARIANT = "variant";
    private static String COUNTRY = "country";

    private MapConversions() {}
    
    public static final String KEY_VALUE_ERROR_MESSAGE = "To convert from Map to %s the map must include one of the following: %s[_v], or [value] with associated values.";

    private static String[] UUID_PARAMS = new String[] { MOST_SIG_BITS, LEAST_SIG_BITS };
    static Object toUUID(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;

        ConverterOptions options = converter.getOptions();
        if (map.containsKey(MOST_SIG_BITS) && map.containsKey(LEAST_SIG_BITS)) {
            long most = converter.convert(map.get(MOST_SIG_BITS), long.class);
            long least = converter.convert(map.get(LEAST_SIG_BITS), long.class);
            return new UUID(most, least);
        }

        return fromValueForMultiKey(from, converter, UUID.class, UUID_PARAMS);
    }

    static Byte toByte(Object from, Converter converter) {
        return fromValue(from, converter, Byte.class);
    }

    static Short toShort(Object from, Converter converter) {
        return fromValue(from, converter, Short.class);
    }

    static Integer toInt(Object from, Converter converter) {
        return fromValue(from, converter, Integer.class);
    }

    static Long toLong(Object from, Converter converter) {
        return fromValue(from, converter, Long.class);
    }

    static Float toFloat(Object from, Converter converter) {
        return fromValue(from, converter, Float.class);
    }

    static Double toDouble(Object from, Converter converter) {
        return fromValue(from, converter, Double.class);
    }

    static Boolean toBoolean(Object from, Converter converter) {
        return fromValue(from, converter, Boolean.class);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        return fromValue(from, converter, BigDecimal.class);
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return fromValue(from, converter, BigInteger.class);
    }

    static String toString(Object from, Converter converter) {
        return fromValue(from, converter, String.class);
    }

    static Character toCharacter(Object from, Converter converter) {
        return fromValue(from, converter, char.class);
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter) {
        return fromValue(from, converter, AtomicInteger.class);
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return fromValue(from, converter, AtomicLong.class);
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return fromValue(from, converter, AtomicBoolean.class);
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return fromSingleKey(from, converter, TIME, java.sql.Date.class);
    }

    static Date toDate(Object from, Converter converter) {
        return fromSingleKey(from, converter, TIME, Date.class);
    }

    private static final String[] TIMESTAMP_PARAMS = new String[] { TIME, NANOS };
    static Timestamp toTimestamp(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        ConverterOptions options = converter.getOptions();
        if (map.containsKey(TIME)) {
            long time = converter.convert(map.get(TIME), long.class);
            int ns = converter.convert(map.get(NANOS), int.class);
            Timestamp timeStamp = new Timestamp(time);
            timeStamp.setNanos(ns);
            return timeStamp;
        }

        return fromValueForMultiKey(map, converter, Timestamp.class, TIMESTAMP_PARAMS);
    }

    private static final String[] CALENDAR_PARAMS = new String[] { TIME, ZONE };
    static Calendar toCalendar(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(TIME)) {
            Object zoneRaw = map.get(ZONE);
            TimeZone tz;
            ConverterOptions options = converter.getOptions();

            if (zoneRaw instanceof String) {
                String zone = (String) zoneRaw;
                tz = TimeZone.getTimeZone(zone);
            } else {
                tz = TimeZone.getTimeZone(options.getZoneId());
            }
            
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(tz);
            Date epochInMillis = converter.convert(map.get(TIME), Date.class);
            cal.setTimeInMillis(epochInMillis.getTime());
            return cal;
        } else {
            return fromValueForMultiKey(map, converter, Calendar.class, CALENDAR_PARAMS);
        }
    }

    private static final String[] LOCALE_PARAMS = new String[] { LANGUAGE };
    static Locale toLocale(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;

        if (map.containsKey(VALUE) || map.containsKey(V)) {
            return fromValueForMultiKey(map, converter, Locale.class, LOCALE_PARAMS);
        }

        String language = converter.convert(map.get(LANGUAGE), String.class);
        if (language == null) {
            throw new IllegalArgumentException("java.util.Locale must specify 'language' field");
        }
        String country = converter.convert(map.get(COUNTRY), String.class);
        String variant = converter.convert(map.get(VARIANT), String.class);

        if (country == null) {
            return new Locale(language);
        }
        if (variant == null) {
            return new Locale(language, country);
        }

        return new Locale(language, country, variant);
    }


    private static final String[] LOCAL_DATE_PARAMS = new String[] { YEAR, MONTH, DAY };
    static LocalDate toLocalDate(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(MONTH) && map.containsKey(DAY) && map.containsKey(YEAR)) {
            ConverterOptions options = converter.getOptions();
            int month = converter.convert(map.get(MONTH), int.class);
            int day = converter.convert(map.get(DAY), int.class);
            int year = converter.convert(map.get(YEAR), int.class);
            return LocalDate.of(year, month, day);
        } else {
            return fromValueForMultiKey(map, converter, LocalDate.class, LOCAL_DATE_PARAMS);
        }
    }

    private static final String[] LOCAL_TIME_PARAMS = new String[] { HOUR, MINUTE, SECOND, NANO };
    static LocalTime toLocalTime(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(HOUR) && map.containsKey(MINUTE)) {
            ConverterOptions options = converter.getOptions();
            int hour = converter.convert(map.get(HOUR), int.class);
            int minute = converter.convert(map.get(MINUTE), int.class);
            int second = converter.convert(map.get(SECOND), int.class);
            int nano = converter.convert(map.get(NANO), int.class);
            return LocalTime.of(hour, minute, second, nano);
        } else {
            return fromValueForMultiKey(map, converter, LocalTime.class, LOCAL_TIME_PARAMS);
        }
    }

    private static final String[] OFFSET_TIME_PARAMS = new String[] { HOUR, MINUTE, SECOND, NANO, OFFSET_HOUR, OFFSET_MINUTE };
    static OffsetTime toOffsetTime(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(HOUR) && map.containsKey(MINUTE)) {
            ConverterOptions options = converter.getOptions();
            int hour = converter.convert(map.get(HOUR), int.class);
            int minute = converter.convert(map.get(MINUTE), int.class);
            int second = converter.convert(map.get(SECOND), int.class);
            int nano = converter.convert(map.get(NANO), int.class);
            int offsetHour = converter.convert(map.get(OFFSET_HOUR), int.class);
            int offsetMinute = converter.convert(map.get(OFFSET_MINUTE), int.class);
            ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(offsetHour, offsetMinute);
            return OffsetTime.of(hour, minute, second, nano, zoneOffset);
        } else {
            return fromValueForMultiKey(map, converter, OffsetTime.class, OFFSET_TIME_PARAMS);
        }
    }

    private static final String[] OFFSET_DATE_TIME_PARAMS = new String[] { YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, NANO, OFFSET_HOUR, OFFSET_MINUTE };
    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        ConverterOptions options = converter.getOptions();
        if (map.containsKey(DATE_TIME) && map.containsKey(OFFSET)) {
            LocalDateTime dateTime = converter.convert(map.get(DATE_TIME), LocalDateTime.class);
            ZoneOffset zoneOffset = converter.convert(map.get(OFFSET), ZoneOffset.class);
            return OffsetDateTime.of(dateTime, zoneOffset);
        } else if (map.containsKey(YEAR) && map.containsKey(OFFSET_HOUR)) {
            int year = converter.convert(map.get(YEAR), int.class);
            int month = converter.convert(map.get(MONTH), int.class);
            int day = converter.convert(map.get(DAY), int.class);
            int hour = converter.convert(map.get(HOUR), int.class);
            int minute = converter.convert(map.get(MINUTE), int.class);
            int second = converter.convert(map.get(SECOND), int.class);
            int nano = converter.convert(map.get(NANO), int.class);
            int offsetHour = converter.convert(map.get(OFFSET_HOUR), int.class);
            int offsetMinute = converter.convert(map.get(OFFSET_MINUTE), int.class);
            ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(offsetHour, offsetMinute);
            return OffsetDateTime.of(year, month, day, hour, minute, second, nano, zoneOffset);
        } else {
            return fromValueForMultiKey(map, converter, OffsetDateTime.class, OFFSET_DATE_TIME_PARAMS);
        }
    }

    private static final String[] LOCAL_DATE_TIME_PARAMS = new String[] { DATE, TIME };

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(DATE)) {
            LocalDate localDate = converter.convert(map.get(DATE), LocalDate.class);
            LocalTime localTime = map.containsKey(TIME) ? converter.convert(map.get(TIME), LocalTime.class) : LocalTime.MIDNIGHT;
            // validate date isn't null?
            return LocalDateTime.of(localDate, localTime);
        } else {
            return fromValueForMultiKey(from, converter, LocalDateTime.class, LOCAL_DATE_TIME_PARAMS);
        }
    }

    private static final String[] ZONED_DATE_TIME_PARAMS = new String[] { ZONE, DATE_TIME };
    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(ZONE) && map.containsKey(DATE_TIME)) {
            ZoneId zoneId = converter.convert(map.get(ZONE), ZoneId.class);
            LocalDateTime localDateTime = converter.convert(map.get(DATE_TIME), LocalDateTime.class);
            return ZonedDateTime.of(localDateTime, zoneId);
        } else {
            return fromValueForMultiKey(from, converter, ZonedDateTime.class, ZONED_DATE_TIME_PARAMS);
        }
    }

    static Class<?> toClass(Object from, Converter converter) {
        return fromValue(from, converter, Class.class);
    }

    private static final String[] DURATION_PARAMS = new String[] { SECONDS, NANOS };
    static Duration toDuration(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(SECONDS)) {
            ConverterOptions options = converter.getOptions();
            long sec = converter.convert(map.get(SECONDS), long.class);
            long nanos = converter.convert(map.get(NANOS), long.class);
            return Duration.ofSeconds(sec, nanos);
        } else {
            return fromValueForMultiKey(from, converter, Duration.class, DURATION_PARAMS);
        }
    }

    private static final String[] INSTANT_PARAMS = new String[] { SECONDS, NANOS };
    static Instant toInstant(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(SECONDS)) {
            ConverterOptions options = converter.getOptions();
            long sec = converter.convert(map.get(SECONDS), long.class);
            long nanos = converter.convert(map.get(NANOS), long.class);
            return Instant.ofEpochSecond(sec, nanos);
        } else {
            return fromValueForMultiKey(from, converter, Instant.class, INSTANT_PARAMS);
        }
    }

    private static final String[] MONTH_DAY_PARAMS = new String[] { MONTH, DAY };
    static MonthDay toMonthDay(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(MONTH) && map.containsKey(DAY)) {
            ConverterOptions options = converter.getOptions();
            int month = converter.convert(map.get(MONTH), int.class);
            int day = converter.convert(map.get(DAY), int.class);
            return MonthDay.of(month, day);
        } else {
            return fromValueForMultiKey(from, converter, MonthDay.class, MONTH_DAY_PARAMS);
        }
    }

    private static final String[] YEAR_MONTH_PARAMS = new String[] { YEAR, MONTH };
    static YearMonth toYearMonth(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(YEAR) && map.containsKey(MONTH)) {
            ConverterOptions options = converter.getOptions();
            int year = converter.convert(map.get(YEAR), int.class);
            int month = converter.convert(map.get(MONTH), int.class);
            return YearMonth.of(year, month);
        } else {
            return fromValueForMultiKey(from, converter, YearMonth.class, YEAR_MONTH_PARAMS);
        }
    }

    private static final String[] PERIOD_PARAMS = new String[] { YEARS, MONTHS, DAYS };
    static Period toPeriod(Object from, Converter converter) {

        Map<String, Object> map = (Map<String, Object>) from;

        if (map.containsKey(VALUE) || map.containsKey(V)) {
            return fromValueForMultiKey(from, converter, Period.class, PERIOD_PARAMS);
        }

        Number years = converter.convert(map.getOrDefault(YEARS, 0), int.class);
        Number months = converter.convert(map.getOrDefault(MONTHS, 0), int.class);
        Number days = converter.convert(map.getOrDefault(DAYS, 0), int.class);

        return Period.of(years.intValue(), months.intValue(), days.intValue());
    }

    static ZoneId toZoneId(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(ZONE)) {
            ConverterOptions options = converter.getOptions();
            ZoneId zoneId = converter.convert(map.get(ZONE), ZoneId.class);
            return zoneId;
        } else if (map.containsKey(ID)) {
            ConverterOptions options = converter.getOptions();
            ZoneId zoneId = converter.convert(map.get(ID), ZoneId.class);
            return zoneId;
        } else {
            return fromSingleKey(from, converter, ZONE, ZoneId.class);
        }
    }

    private static final String[] ZONE_OFFSET_PARAMS = new String[] { HOURS, MINUTES, SECONDS };
    static ZoneOffset toZoneOffset(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(HOURS)) {
            int hours = converter.convert(map.get(HOURS), int.class);
            int minutes = converter.convert(map.getOrDefault(MINUTES, 0), int.class);  // optional
            int seconds = converter.convert(map.getOrDefault(SECONDS, 0), int.class);  // optional
            return ZoneOffset.ofHoursMinutesSeconds(hours, minutes, seconds);
        } else {
            return fromValueForMultiKey(from, converter, ZoneOffset.class, ZONE_OFFSET_PARAMS);
        }
    }

    static Year toYear(Object from, Converter converter) {
        return fromSingleKey(from, converter, YEAR, Year.class);
    }

    static Map<String, ?> initMap(Object from, Converter converter) {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put(V, from);
        return map;
    }

    /**
     * Allows you to check for a single named key and convert that to a type of it exists, otherwise falls back
     * onto the value type V or VALUE.
     *
     * @param <T> type of object to convert the value.
     * @return type if it exists, else returns what is in V or VALUE
     */
    private static <T> T fromSingleKey(final Object from, final Converter converter, final String key, final Class<T> type) {
        validateParams(converter, type);

        Map<?, ?> map = asMap(from);

        if (map.containsKey(key)) {
            return converter.convert(map.get(key), type);
        }

        return extractValue(map, converter, type, key);
    }

    private static <T> T fromValueForMultiKey(Object from, Converter converter, Class<T> type, String[] keys) {
        validateParams(converter, type);

        return extractValue(asMap(from), converter, type, keys);
    }

    private static <T> T fromValue(Object from, Converter converter, Class<T> type) {
        validateParams(converter, type);

        return extractValue(asMap(from), converter, type);
    }

    private static <T> T extractValue(Map<?, ?> map, Converter converter, Class<T> type, String...keys) {
        if (map.containsKey(V)) {
            return converter.convert(map.get(V), type);
        }

        if (map.containsKey(VALUE)) {
            return converter.convert(map.get(VALUE), type);
        }

        String keyText = ArrayUtilities.isEmpty(keys) ? "" : "[" + String.join(", ", keys) + "], ";
        throw new IllegalArgumentException(String.format(KEY_VALUE_ERROR_MESSAGE, Converter.getShortName(type), keyText));
    }

    private static <T> void validateParams(Converter converter, Class<T> type) {
        Convention.throwIfNull(type, "type cannot be null");
        Convention.throwIfNull(converter, "converter cannot be null");
    }

    private static Map<?, ?> asMap(Object o) {
        Convention.throwIfFalse(o instanceof Map, "from must be an instance of map");
        return (Map<?, ?>)o;
    }

    static Map<?, ?> toMap(Object from, Converter converter) {
        Map<?, ?> source = (Map<?, ?>) from;
        Map<?, ?> copy = new LinkedHashMap<>(source);
        return copy;
    }
}
