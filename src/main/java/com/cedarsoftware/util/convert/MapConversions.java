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
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.CollectionUtilities;
import com.cedarsoftware.util.Convention;

import static com.cedarsoftware.util.convert.Converter.NOPE;
import static com.cedarsoftware.util.convert.Converter.VALUE2;

public class MapConversions {

    private static final String V = "_v";
    private static final String VALUE = "value";
    private static final String TIME = "time";
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
        return fromValueMap(converter, (Map<?, ?>) fromInstance, char.class, null, options);
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
        if (map.containsKey("time")) {
            long time = converter.convert(map.get("time"), long.class, options);
            int ns = converter.convert(map.get("nanos"), int.class, options);
            Timestamp timeStamp = new Timestamp(time);
            timeStamp.setNanos(ns);
            return timeStamp;
        }

        return fromValueForMultiKey(map, converter, options, Timestamp.class, TIMESTAMP_PARAMS);
    }

    static Calendar toCalendar(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;
        if (map.containsKey("time")) {
            Object zoneRaw = map.get("zone");
            TimeZone tz;
            if (zoneRaw instanceof String) {
                String zone = (String) zoneRaw;
                tz = TimeZone.getTimeZone(zone);
            } else {
                tz = TimeZone.getTimeZone(options.getZoneId());
            }
            Calendar cal = Calendar.getInstance();
            cal.setTimeZone(tz);
            Date epochInMillis = converter.convert(map.get("time"), Date.class, options);
            cal.setTimeInMillis(epochInMillis.getTime());
            return cal;
        } else {
            return fromValueMap(converter, map, Calendar.class, CollectionUtilities.setOf("time", "zone"), options);
        }
    }

    static LocalDate toLocalDate(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;
        if (map.containsKey("month") && map.containsKey("day") && map.containsKey("year")) {
            int month = converter.convert(map.get("month"), int.class, options);
            int day = converter.convert(map.get("day"), int.class, options);
            int year = converter.convert(map.get("year"), int.class, options);
            return LocalDate.of(year, month, day);
        } else {
            return fromValueMap(converter, map, LocalDate.class, CollectionUtilities.setOf("year", "month", "day"), options);
        }
    }

    static LocalTime toLocalTime(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;
        if (map.containsKey("hour") && map.containsKey("minute")) {
            int hour = converter.convert(map.get("hour"), int.class, options);
            int minute = converter.convert(map.get("minute"), int.class, options);
            int second = converter.convert(map.get("second"), int.class, options);
            int nano = converter.convert(map.get("nano"), int.class, options);
            return LocalTime.of(hour, minute, second, nano);
        } else {
            return fromValueMap(converter, map, LocalTime.class, CollectionUtilities.setOf("hour", "minute", "second", "nano"), options);
        }
    }

    static LocalDateTime toLocalDateTime(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;
        return fromValueMap(converter, map, LocalDateTime.class, null, options);
    }

    static ZonedDateTime toZonedDateTime(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;
        return fromValueMap(converter, map, ZonedDateTime.class, null, options);
    }

    static Class<?> toClass(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;
        return fromValueMap(converter, map, Class.class, null, options);
    }

    static Duration toDuration(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<String, Object> map = (Map<String, Object>) fromInstance;
        if (map.containsKey("seconds")) {
            long sec = converter.convert(map.get("seconds"), long.class, options);
            long nanos = converter.convert(map.get("nanos"), long.class, options);
            return Duration.ofSeconds(sec, nanos);
        } else {
            return fromValueMap(converter, map, Duration.class, CollectionUtilities.setOf("seconds", "nanos"), options);
        }
    }

    static Instant toInstant(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<String, Object> map = (Map<String, Object>) fromInstance;
        if (map.containsKey("seconds")) {
            long sec = converter.convert(map.get("seconds"), long.class, options);
            long nanos = converter.convert(map.get("nanos"), long.class, options);
            return Instant.ofEpochSecond(sec, nanos);
        } else {
            return fromValueMap(converter, map, Instant.class, CollectionUtilities.setOf("seconds", "nanos"), options);
        }
    }

    static MonthDay toMonthDay(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<String, Object> map = (Map<String, Object>) fromInstance;
        if (map.containsKey("month")) {
            int month = converter.convert(map.get("month"), int.class, options);
            int day = converter.convert(map.get("day"), int.class, options);
            return MonthDay.of(month, day);
        } else {
            return fromValueMap(converter, map, MonthDay.class, CollectionUtilities.setOf("month", "day"), options);
        }
    }

    /**
     * Allows you to check for a single named key and convert that to a type of it exists, otherwise falls back
     * onto the value type V or VALUE.
     *
     * @param <T> type of object to convert the value.
     * @return type if it exists, else returns what is in V or VALUE
     */
    static <T> T fromSingleKey(final Object fromInstance, final Converter converter, final ConverterOptions options, final String key, final Class<T> type) {
        validateParams(converter, options, type);

        Map<?, ?> map = asMap(fromInstance);

        if (map.containsKey(key)) {
            return converter.convert(key, type, options);
        }

        return extractValue(map, converter, options, type, key);
    }

    static <T> T fromValueForMultiKey(Object from, Converter converter, ConverterOptions options, Class<T> type, String[] keys) {
        validateParams(converter, options, type);

        return extractValue(asMap(from), converter, options, type, keys);
    }

    static <T> T fromValue(Object from, Converter converter, ConverterOptions options, Class<T> type) {
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

        String keyText = ArrayUtilities.isEmpty(keys) ? "" : "[" + String.join(",", keys) + "], ";
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

    private static <T> T fromValueMap(Converter converter, Map<?, ?> map, Class<T> type, Set<String> set, ConverterOptions options) {
        T ret = fromMap(converter, map, VALUE, type, options);
        if (ret != NOPE) {
            return ret;
        }

        ret = fromMap(converter, map, VALUE2, type, options);
        if (ret == NOPE) {
            if (set == null || set.isEmpty()) {
                throw new IllegalArgumentException("To convert from Map to " + getShortName(type) + ", the map must include keys: '_v' or 'value' an associated value to convert from.");
            } else {
                throw new IllegalArgumentException("To convert from Map to " + getShortName(type) + ", the map must include keys: " + set + ", or '_v' or 'value' an associated value to convert from.");
            }
        }
        return ret;
    }

    private static <T> T fromMap(Converter converter, Map<?, ?> map, String key, Class<T> type, ConverterOptions options) {
        if (map.containsKey(key)) {
            return converter.convert(map.get(key), type, options);
        }
        return (T) NOPE;
    }
}
