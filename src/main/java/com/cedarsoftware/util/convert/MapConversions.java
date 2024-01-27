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
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.Convention;

public class MapConversions {

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

    private static final String MOST_SIG_BITS = "mostSigBits";
    private static final String LEAST_SIG_BITS = "leastSigBits";


    public static final String KEY_VALUE_ERROR_MESSAGE = "To convert from Map to %s the map must include one of the following: %s[_v], or [value] with associated values.";
    private static String[] UUID_PARAMS = new String[] { MOST_SIG_BITS, LEAST_SIG_BITS };

    static Object toUUID(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;

        if (map.containsKey(MOST_SIG_BITS) && map.containsKey(LEAST_SIG_BITS)) {
            long most = converter.convert(map.get(MOST_SIG_BITS), long.class, options);
            long least = converter.convert(map.get(LEAST_SIG_BITS), long.class, options);
            return new UUID(most, least);
        }

        return fromValueForMultiKey(fromInstance, converter, options, UUID.class, UUID_PARAMS);
    }

    static Byte toByte(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, Byte.class);
    }

    static Short toShort(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, Short.class);
    }

    static Integer toInt(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, Integer.class);
    }

    static Long toLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, Long.class);
    }

    static Float toFloat(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, Float.class);
    }

    static Double toDouble(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, Double.class);
    }

    static Boolean toBoolean(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, Boolean.class);
    }

    static BigDecimal toBigDecimal(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, BigDecimal.class);
    }

    static BigInteger toBigInteger(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, BigInteger.class);
    }

    static String toString(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, String.class);
    }

    static Character toCharacter(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, char.class);
    }

    static AtomicInteger toAtomicInteger(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, AtomicInteger.class);
    }

    static AtomicLong toAtomicLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, AtomicLong.class);
    }

    static AtomicBoolean toAtomicBoolean(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, AtomicBoolean.class);
    }

    static java.sql.Date toSqlDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromSingleKey(fromInstance, converter, options, TIME, java.sql.Date.class);
    }

    static Date toDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromSingleKey(fromInstance, converter, options, TIME, Date.class);
    }

    private static final String[] TIMESTAMP_PARAMS = new String[] { TIME, NANOS };
    static Timestamp toTimestamp(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;
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
    static Calendar toCalendar(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;
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
    static LocalDate toLocalDate(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;
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
    static LocalTime toLocalTime(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;
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

    static LocalDateTime toLocalDateTime(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, LocalDateTime.class);
    }

    static ZonedDateTime toZonedDateTime(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, ZonedDateTime.class);
    }

    static Class<?> toClass(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromValue(fromInstance, converter, options, Class.class);
    }

    private static final String[] DURATION_PARAMS = new String[] { SECONDS, NANOS };
    static Duration toDuration(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<String, Object> map = (Map<String, Object>) fromInstance;
        if (map.containsKey(SECONDS)) {
            long sec = converter.convert(map.get(SECONDS), long.class, options);
            long nanos = converter.convert(map.get(NANOS), long.class, options);
            return Duration.ofSeconds(sec, nanos);
        } else {
            return fromValueForMultiKey(fromInstance, converter, options, Duration.class, DURATION_PARAMS);
        }
    }

    private static final String[] INSTANT_PARAMS = new String[] { SECONDS, NANOS };
    static Instant toInstant(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<String, Object> map = (Map<String, Object>) fromInstance;
        if (map.containsKey(SECONDS)) {
            long sec = converter.convert(map.get(SECONDS), long.class, options);
            long nanos = converter.convert(map.get(NANOS), long.class, options);
            return Instant.ofEpochSecond(sec, nanos);
        } else {
            return fromValueForMultiKey(fromInstance, converter, options, Instant.class, INSTANT_PARAMS);
        }
    }

    private static final String[] MONTH_DAY_PARAMS = new String[] { MONTH, DAY };
    static MonthDay toMonthDay(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<String, Object> map = (Map<String, Object>) fromInstance;
        if (map.containsKey(MONTH)) {
            int month = converter.convert(map.get(MONTH), int.class, options);
            int day = converter.convert(map.get(DAY), int.class, options);
            return MonthDay.of(month, day);
        } else {
            return fromValueForMultiKey(fromInstance, converter, options, MonthDay.class, MONTH_DAY_PARAMS);
        }
    }

    static Map<String, ?> initMap(Object from, Converter converter, ConverterOptions options) {
        Map<String, Object> map = new HashMap<>();
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
    private static <T> T fromSingleKey(final Object fromInstance, final Converter converter, final ConverterOptions options, final String key, final Class<T> type) {
        validateParams(converter, options, type);

        Map<?, ?> map = asMap(fromInstance);

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
        throw new IllegalArgumentException(String.format(KEY_VALUE_ERROR_MESSAGE, getShortName(type), keyText));
    }

    private static <T> void validateParams(Converter converter, ConverterOptions options, Class<T> type) {
        Convention.throwIfNull(type, "type cannot be null");
        Convention.throwIfNull(converter, "converter cannot be null");
        Convention.throwIfNull(options, "options cannot be null");
    }

    private static String getShortName(Class<?> type) {
        return java.sql.Date.class.equals(type) ? type.getName() : type.getSimpleName();
    }

    private static Map<?, ?> asMap(Object o) {
        Convention.throwIfFalse(o instanceof Map, "fromInstance must be an instance of map");
        return (Map<?, ?>)o;
    }
}
