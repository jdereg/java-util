package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CollectionUtilities;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MapConversion {

    private static final String V = "_v";
    private static final String VALUE = "value";

    public static Object toUUID(Object fromInstance, Converter converter, ConverterOptions options) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;

        if (map.containsKey("mostSigBits") && map.containsKey("leastSigBits")) {
            long most = converter.convert(map.get("mostSigBits"), long.class, options);
            long least = converter.convert(map.get("leastSigBits"), long.class, options);

            return new UUID(most, least);
        }

        throw new IllegalArgumentException("To convert Map to UUID, the Map must contain both 'mostSigBits' and 'leastSigBits' keys");
    }

    public static Byte toByte(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, Byte.class);
    }

    public static Short toShort(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, Short.class);
    }

    public static Integer toInt(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, Integer.class);
    }

    public static Long toLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, Long.class);
    }

    public static Float toFloat(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, Float.class);
    }

    public static Double toDouble(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, Double.class);
    }

    public static Boolean toBoolean(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, Boolean.class);
    }

    public static BigDecimal toBigDecimal(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, BigDecimal.class);
    }

    public static BigInteger toBigInteger(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, BigInteger.class);
    }

    public static String toString(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, String.class);
    }


    public static AtomicInteger toAtomicInteger(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, AtomicInteger.class);
    }

    public static AtomicLong toAtomicLong(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, AtomicLong.class);
    }

    public static AtomicBoolean toAtomicBoolean(Object fromInstance, Converter converter, ConverterOptions options) {
        return fromMapValue(fromInstance, converter, options, AtomicBoolean.class);
    }

    public static <T> T fromMapValue(Object fromInstance, Converter converter, ConverterOptions options, Class<T> type) {
        Map<?, ?> map = (Map<?, ?>) fromInstance;

        if (map.containsKey(V)) {
            return converter.convert(map.get(V), type);
        }

        if (map.containsKey(VALUE)) {
            return converter.convert(map.get(VALUE), type);
        }

        throw new IllegalArgumentException("To convert from Map to " + getShortName(type) + ", the map must include keys: '_v' or 'value' an associated value to convert from.");
    }


    private static <T> T getConvertedValue(Map<?, ?> map, String key, Class<T> type, Converter converter, ConverterOptions options) {
        // NOPE STUFF?
        return converter.convert(map.get(key), type, options);
    }

    private static String getShortName(Class<?> type) {
        return java.sql.Date.class.equals(type) ? type.getName() : type.getSimpleName();
    }
}
