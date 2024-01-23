package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.Convention;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class MapConversion {

    private static final String V = "_v";
    private static final String VALUE = "value";

    static Object toUUID(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;

        if (map.containsKey("mostSigBits") && map.containsKey("leastSigBits")) {
            long most = converter.convert(map.get("mostSigBits"), long.class, options);
            long least = converter.convert(map.get("leastSigBits"), long.class, options);

            return new UUID(most, least);
        }

        throw new IllegalArgumentException("To convert Map to UUID, the Map must contain both 'mostSigBits' and 'leastSigBits' keys");
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

    private static final String TIME = "time";

    static java.sql.Date toSqlDate(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromKeyOrValue(fromInstance, TIME, java.sql.Date.class, converter, options);
    }

    /**
     * Allows you to check for a single named key and convert that to a type of it exists, otherwise falls back
     * onto the value type V or VALUE.
     * @return type if it exists, else returns what is in V or VALUE
     * @param <T> type of object to convert the value.
     */
    static <T> T fromKeyOrValue(final Object fromInstance, final String key, final Class<T> type, final Converter converter, final ConverterOptions options) {
        Convention.throwIfFalse(fromInstance instanceof Map, "fromInstance must be an instance of map");
        Convention.throwIfNullOrEmpty(key, "key cannot be null or empty");
        Convention.throwIfNull(type, "type cannot be null");
        Convention.throwIfNull(converter, "converter cannot be null");
        Convention.throwIfNull(options, "options cannot be null");

        Map<?, ?> map = (Map<?, ?>) fromInstance;

        if (map.containsKey(key)) {
            return converter.convert(key, type, options);
        }

        if (map.containsKey(V)) {
            return converter.convert(map.get(V), type, options);
        }

        if (map.containsKey(VALUE)) {
            return converter.convert(map.get(VALUE), type, options);
        }

        throw new IllegalArgumentException(String.format("To convert from Map to %s the map must include keys: %s, '_v' or 'value' an associated value to convert from.", getShortName(type), key));
    }

    static <T> T fromValue(Object fromInstance, Converter converter, ConverterOptions options, Class<T> type) {
        Convention.throwIfFalse(fromInstance instanceof Map, "fromInstance must be an instance of map");
        Convention.throwIfNull(type, "type cannot be null");
        Convention.throwIfNull(converter, "converter cannot be null");
        Convention.throwIfNull(options, "options cannot be null");

        Map<?, ?> map = (Map<?, ?>) fromInstance;

        if (map.containsKey(V)) {
            return converter.convert(map.get(V), type, options);
        }

        if (map.containsKey(VALUE)) {
            return converter.convert(map.get(VALUE), type, options);
        }

        throw new IllegalArgumentException(String.format("To convert from Map to %s the map must include keys: '_v' or 'value' an associated value to convert from.", getShortName(type)));
    }

    static <T> Optional<T> convert(Map<?, ?> map, String key, Class<T> type, Converter converter, ConverterOptions options) {
        return map.containsKey(key) ? Optional.of(converter.convert(map.get(key), type, options)) : Optional.empty();
    }

    private static <T> T getConvertedValue(Map<?, ?> map, String key, Class<T> type, Converter converter, ConverterOptions options) {
        // NOPE STUFF?
        return converter.convert(map.get(key), type, options);
    }

    private static String getShortName(Class<?> type) {
        return java.sql.Date.class.equals(type) ? type.getName() : type.getSimpleName();
    }
}
