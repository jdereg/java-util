package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.Convention;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
}
