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
import java.time.Year;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
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
public final class MapConversions {

    private static final String V = "_v";
    private static final String VALUE = "value";
    private static final String TIME = "time";
    private static final String ZONE = "zone";
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY = "day";
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final String SECOND = "second";
    private static final String SECONDS = "seconds";
    private static final String NANO = "nano";
    private static final String NANOS = "nanos";
    private static final String OFFSET_HOUR = "offsetHour";
    private static final String OFFSET_MINUTE = "offsetMinute";
    private static final String MOST_SIG_BITS = "mostSigBits";
    private static final String LEAST_SIG_BITS = "leastSigBits";

    private MapConversions() {}
    
    public static final String KEY_VALUE_ERROR_MESSAGE = "To convert from Map to %s the map must include one of the following: %s[_v], or [value] with associated values.";

    private static String[] UUID_PARAMS = new String[] { MOST_SIG_BITS, LEAST_SIG_BITS };
    static Object toUUID(Object from, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) from;

        if (map.containsKey(MOST_SIG_BITS) && map.containsKey(LEAST_SIG_BITS)) {
            long most = converter.convert(map.get(MOST_SIG_BITS), long.class, options);
            long least = converter.convert(map.get(LEAST_SIG_BITS), long.class, options);
            return new UUID(most, least);
        }

        return fromValueForMultiKey(from, converter, options, UUID.class, UUID_PARAMS);
    }

    static Byte toByte(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, Byte.class);
    }

    static Short toShort(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, Short.class);
    }

    static Integer toInt(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, Integer.class);
    }

    static Long toLong(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, Long.class);
    }

    static Float toFloat(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, Float.class);
    }

    static Double toDouble(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, Double.class);
    }

    static Boolean toBoolean(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, Boolean.class);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, BigDecimal.class);
    }

    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, BigInteger.class);
    }

    static String toString(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, String.class);
    }

    static Character toCharacter(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, char.class);
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, AtomicInteger.class);
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, AtomicLong.class);
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, AtomicBoolean.class);
    }

    static java.sql.Date toSqlDate(Object from, Converter converter, ConverterOptions options) {
        return fromSingleKey(from, converter, options, TIME, java.sql.Date.class);
    }

    static Date toDate(Object from, Converter converter, ConverterOptions options) {
        return fromSingleKey(from, converter, options, TIME, Date.class);
    }

    private static final String[] TIMESTAMP_PARAMS = new String[] { TIME, NANOS };
    static Timestamp toTimestamp(Object from, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(TIME)) {
            long time = converter.convert(map.get(TIME), long.class, options);
            int ns = converter.convert(map.get(NANOS), int.class, options);
            Timestamp timeStamp = new Timestamp(time);
            timeStamp.setNanos(ns);
            return timeStamp;
        }

        return fromValueForMultiKey(map, converter, options, Timestamp.class, TIMESTAMP_PARAMS);
    }

    private static final String[] CALENDAR_PARAMS = new String[] { TIME, ZONE };
    static Calendar toCalendar(Object from, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(TIME)) {
            Object zoneRaw = map.get(ZONE);
            TimeZone tz;
            if (zoneRaw instanceof String) {
                String zone = (String) zoneRaw;
                tz = TimeZone.getTimeZone(zone);
            } else {
                tz = TimeZone.getTimeZone(options.getZoneId());
            }
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(tz);
            Date epochInMillis = converter.convert(map.get(TIME), Date.class, options);
            cal.setTimeInMillis(epochInMillis.getTime());
            return cal;
        } else {
            return fromValueForMultiKey(map, converter, options, Calendar.class, CALENDAR_PARAMS);
        }
    }

    private static final String[] LOCAL_DATE_PARAMS = new String[] { YEAR, MONTH, DAY };
    static LocalDate toLocalDate(Object from, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(MONTH) && map.containsKey(DAY) && map.containsKey(YEAR)) {
            int month = converter.convert(map.get(MONTH), int.class, options);
            int day = converter.convert(map.get(DAY), int.class, options);
            int year = converter.convert(map.get(YEAR), int.class, options);
            return LocalDate.of(year, month, day);
        } else {
            return fromValueForMultiKey(map, converter, options, LocalDate.class, LOCAL_DATE_PARAMS);
        }
    }

    private static final String[] LOCAL_TIME_PARAMS = new String[] { HOUR, MINUTE, SECOND, NANO };
    static LocalTime toLocalTime(Object from, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(HOUR) && map.containsKey(MINUTE)) {
            int hour = converter.convert(map.get(HOUR), int.class, options);
            int minute = converter.convert(map.get(MINUTE), int.class, options);
            int second = converter.convert(map.get(SECOND), int.class, options);
            int nano = converter.convert(map.get(NANO), int.class, options);
            return LocalTime.of(hour, minute, second, nano);
        } else {
            return fromValueForMultiKey(map, converter, options, LocalTime.class, LOCAL_TIME_PARAMS);
        }
    }

    private static final String[] OFFSET_TIME_PARAMS = new String[] { HOUR, MINUTE, SECOND, NANO, OFFSET_HOUR, OFFSET_MINUTE };
    static OffsetTime toOffsetTime(Object from, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(HOUR) && map.containsKey(MINUTE)) {
            int hour = converter.convert(map.get(HOUR), int.class, options);
            int minute = converter.convert(map.get(MINUTE), int.class, options);
            int second = converter.convert(map.get(SECOND), int.class, options);
            int nano = converter.convert(map.get(NANO), int.class, options);
            int offsetHour = converter.convert(map.get(OFFSET_HOUR), int.class, options);
            int offsetMinute = converter.convert(map.get(OFFSET_MINUTE), int.class, options);
            ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(offsetHour, offsetMinute);
            return OffsetTime.of(hour, minute, second, nano, zoneOffset);
        } else {
            return fromValueForMultiKey(map, converter, options, OffsetTime.class, OFFSET_TIME_PARAMS);
        }
    }

    private static final String[] OFFSET_DATE_TIME_PARAMS = new String[] { YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, NANO, OFFSET_HOUR, OFFSET_MINUTE };
    static OffsetDateTime toOffsetDateTime(Object from, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(YEAR) && map.containsKey(OFFSET_HOUR)) {
            int year = converter.convert(map.get(YEAR), int.class, options);
            int month = converter.convert(map.get(MONTH), int.class, options);
            int day = converter.convert(map.get(DAY), int.class, options);
            int hour = converter.convert(map.get(HOUR), int.class, options);
            int minute = converter.convert(map.get(MINUTE), int.class, options);
            int second = converter.convert(map.get(SECOND), int.class, options);
            int nano = converter.convert(map.get(NANO), int.class, options);
            int offsetHour = converter.convert(map.get(OFFSET_HOUR), int.class, options);
            int offsetMinute = converter.convert(map.get(OFFSET_MINUTE), int.class, options);
            ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(offsetHour, offsetMinute);
            return OffsetDateTime.of(year, month, day, hour, minute, second, nano, zoneOffset);
        } else {
            return fromValueForMultiKey(map, converter, options, OffsetDateTime.class, OFFSET_DATE_TIME_PARAMS);
        }
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, LocalDateTime.class);
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, ZonedDateTime.class);
    }

    static Class<?> toClass(Object from, Converter converter, ConverterOptions options) {
        return fromValue(from, converter, options, Class.class);
    }

    private static final String[] DURATION_PARAMS = new String[] { SECONDS, NANOS };
    static Duration toDuration(Object from, Converter converter, ConverterOptions options) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(SECONDS)) {
            long sec = converter.convert(map.get(SECONDS), long.class, options);
            long nanos = converter.convert(map.get(NANOS), long.class, options);
            return Duration.ofSeconds(sec, nanos);
        } else {
            return fromValueForMultiKey(from, converter, options, Duration.class, DURATION_PARAMS);
        }
    }

    private static final String[] INSTANT_PARAMS = new String[] { SECONDS, NANOS };
    static Instant toInstant(Object from, Converter converter, ConverterOptions options) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(SECONDS)) {
            long sec = converter.convert(map.get(SECONDS), long.class, options);
            long nanos = converter.convert(map.get(NANOS), long.class, options);
            return Instant.ofEpochSecond(sec, nanos);
        } else {
            return fromValueForMultiKey(from, converter, options, Instant.class, INSTANT_PARAMS);
        }
    }

    private static final String[] MONTH_DAY_PARAMS = new String[] { MONTH, DAY };
    static MonthDay toMonthDay(Object from, Converter converter, ConverterOptions options) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(MONTH)) {
            int month = converter.convert(map.get(MONTH), int.class, options);
            int day = converter.convert(map.get(DAY), int.class, options);
            return MonthDay.of(month, day);
        } else {
            return fromValueForMultiKey(from, converter, options, MonthDay.class, MONTH_DAY_PARAMS);
        }
    }

    static Year toYear(Object from, Converter converter, ConverterOptions options) {
        return fromSingleKey(from, converter, options, YEAR, Year.class);
    }

    static Map<String, ?> initMap(Object from, Converter converter, ConverterOptions options) {
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
    private static <T> T fromSingleKey(final Object from, final Converter converter, final ConverterOptions options, final String key, final Class<T> type) {
        validateParams(converter, options, type);

        Map<?, ?> map = asMap(from);

        if (map.containsKey(key)) {
            return converter.convert(key, type, options);
        }

        return extractValue(map, converter, options, type, key);
    }

    private static <T> T fromValueForMultiKey(Object from, Converter converter, ConverterOptions options, Class<T> type, String[] keys) {
        validateParams(converter, options, type);

        return extractValue(asMap(from), converter, options, type, keys);
    }

    private static <T> T fromValue(Object from, Converter converter, ConverterOptions options, Class<T> type) {
        validateParams(converter, options, type);

        return extractValue(asMap(from), converter, options, type);
    }

    private static <T> T extractValue(Map<?, ?> map, Converter converter, ConverterOptions options, Class<T> type, String...keys) {
        if (map.containsKey(V)) {
            return converter.convert(map.get(V), type, options);
        }

        if (map.containsKey(VALUE)) {
            return converter.convert(map.get(VALUE), type, options);
        }

        String keyText = ArrayUtilities.isEmpty(keys) ? "" : "[" + String.join(", ", keys) + "], ";
        throw new IllegalArgumentException(String.format(KEY_VALUE_ERROR_MESSAGE, Converter.getShortName(type), keyText));
    }

    private static <T> void validateParams(Converter converter, ConverterOptions options, Class<T> type) {
        Convention.throwIfNull(type, "type cannot be null");
        Convention.throwIfNull(converter, "converter cannot be null");
        Convention.throwIfNull(options, "options cannot be null");
    }

    private static Map<?, ?> asMap(Object o) {
        Convention.throwIfFalse(o instanceof Map, "from must be an instance of map");
        return (Map<?, ?>)o;
    }

    static Map<?, ?> toMap(Object from, Converter converter, ConverterOptions options) {
        Map<?, ?> source = (Map<?, ?>) from;
        Map<?, ?> copy = new LinkedHashMap<>(source);
        return copy;
    }
}
