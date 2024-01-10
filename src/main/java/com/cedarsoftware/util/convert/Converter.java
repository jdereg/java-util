package com.cedarsoftware.util.convert;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.CollectionUtilities;
import com.cedarsoftware.util.DateUtilities;

/**
 * Instance conversion utility.  Convert from primitive to other primitives, plus support for Number, Date,
 * TimeStamp, SQL Date, LocalDate, LocalDateTime, ZonedDateTime, Calendar, Big*, Atomic*, Class, UUID,
 * String, ...<br/>
 * <br/>
 * Converter.convert(value, class) if null passed in, null is returned for most types, which allows "tri-state"
 * Boolean, for example, however, for primitive types, it chooses zero for the numeric ones, `false` for boolean,
 * and 0 for char.<br/>
 * <br/>
 * A Map can be converted to almost all data types.  For some, like UUID, it is expected for the Map to have
 * certain keys ("mostSigBits", "leastSigBits").  For the older Java Date/Time related classes, it expects
 * "time" or "nanos", and for all others, a Map as the source, the "value" key will be used to source the value
 * for the conversion.<br/>
 * <br/>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
 * <br>
 * Copyright (c) Cedar Software LLC
 * <br><br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <br><br>
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 * <br><br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public final class Converter {
    public static final String NOPE = "~nope!";
    public static final String VALUE = "_v";
    private static final String VALUE2 = "value";

    private final Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> factory;

    private final ConverterOptions options;

    private static final Map<Class<?>, Set<Class<?>>> cacheParentTypes = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> primitiveToWrapper = new HashMap<>(20, .8f);

    private static final Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> DEFAULT_FACTORY = new HashMap<>(500, .8f);

    private static Map.Entry<Class<?>, Class<?>> pair(Class<?> source, Class<?> target) {
        return new AbstractMap.SimpleImmutableEntry<>(source, target);
    }

    static {
        buildPrimitiveWrappers();
        buildFactoryConversions();
    }

    private static void buildPrimitiveWrappers() {
        primitiveToWrapper.put(int.class, Integer.class);
        primitiveToWrapper.put(long.class, Long.class);
        primitiveToWrapper.put(double.class, Double.class);
        primitiveToWrapper.put(float.class, Float.class);
        primitiveToWrapper.put(boolean.class, Boolean.class);
        primitiveToWrapper.put(char.class, Character.class);
        primitiveToWrapper.put(byte.class, Byte.class);
        primitiveToWrapper.put(short.class, Short.class);
        primitiveToWrapper.put(void.class, Void.class);
    }

    private static void buildFactoryConversions() {
        // Byte/byte Conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, byte.class), (fromInstance, converter, options) -> (byte) 0);
        DEFAULT_FACTORY.put(pair(Void.class, Byte.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Byte.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Short.class, Byte.class), NumberConversion::toByte);
        DEFAULT_FACTORY.put(pair(Integer.class, Byte.class), NumberConversion::toByte);
        DEFAULT_FACTORY.put(pair(Long.class, Byte.class), NumberConversion::toByte);
        DEFAULT_FACTORY.put(pair(Float.class, Byte.class), NumberConversion::toByte);
        DEFAULT_FACTORY.put(pair(Double.class, Byte.class), NumberConversion::toByte);
        DEFAULT_FACTORY.put(pair(Boolean.class, Byte.class), BooleanConversion::toByte);
        DEFAULT_FACTORY.put(pair(Character.class, Byte.class), (fromInstance, converter, options) -> (byte) ((Character) fromInstance).charValue());
        DEFAULT_FACTORY.put(pair(Calendar.class, Byte.class), NumberConversion::toByte);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Byte.class), BooleanConversion::atomicToByte);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Byte.class), NumberConversion::toByte);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Byte.class), NumberConversion::toByte);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Byte.class), NumberConversion::toByte);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Byte.class), NumberConversion::toByte);
        DEFAULT_FACTORY.put(pair(Number.class, Byte.class), NumberConversion::toByte);
        DEFAULT_FACTORY.put(pair(Map.class, Byte.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, byte.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, Byte.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return CommonValues.BYTE_ZERO;
            }
            try {
                return Byte.valueOf(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value: " + fromInstance + " not parseable as a byte value or outside " + Byte.MIN_VALUE + " to " + Byte.MAX_VALUE);
            }
        });

        // Short/short conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, short.class), (fromInstance, converter, options) -> (short) 0);
        DEFAULT_FACTORY.put(pair(Void.class, Short.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Short.class), NumberConversion::toShort);
        DEFAULT_FACTORY.put(pair(Short.class, Short.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Integer.class, Short.class), NumberConversion::toShort);
        DEFAULT_FACTORY.put(pair(Long.class, Short.class), NumberConversion::toShort);
        DEFAULT_FACTORY.put(pair(Float.class, Short.class), NumberConversion::toShort);
        DEFAULT_FACTORY.put(pair(Double.class, Short.class), NumberConversion::toShort);
        DEFAULT_FACTORY.put(pair(Boolean.class, Short.class), BooleanConversion::toShort);
        DEFAULT_FACTORY.put(pair(Character.class, Short.class), (fromInstance, converter, options) -> (short) ((Character) fromInstance).charValue());
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Short.class), BooleanConversion::atomicToShort);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Short.class), NumberConversion::toShort);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Short.class), NumberConversion::toShort);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Short.class), NumberConversion::toShort);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Short.class), NumberConversion::toShort);
        DEFAULT_FACTORY.put(pair(LocalDate.class, Short.class), (fromInstance, converter, options) -> ((LocalDate) fromInstance).toEpochDay());
        DEFAULT_FACTORY.put(pair(Number.class, Short.class), NumberConversion::toShort);
        DEFAULT_FACTORY.put(pair(Map.class, Short.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, short.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, Short.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return CommonValues.SHORT_ZERO;
            }
            try {
                return Short.valueOf(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value: " + fromInstance + " not parseable as a short value or outside " + Short.MIN_VALUE + " to " + Short.MAX_VALUE);
            }
        });

        // Integer/int conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, int.class), (fromInstance, converter, options) -> 0);
        DEFAULT_FACTORY.put(pair(Void.class, Integer.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Integer.class), NumberConversion::toInt);
        DEFAULT_FACTORY.put(pair(Short.class, Integer.class), NumberConversion::toInt);
        DEFAULT_FACTORY.put(pair(Integer.class, Integer.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Long.class, Integer.class), NumberConversion::toInt);
        DEFAULT_FACTORY.put(pair(Float.class, Integer.class), NumberConversion::toInt);
        DEFAULT_FACTORY.put(pair(Double.class, Integer.class), NumberConversion::toInt);
        DEFAULT_FACTORY.put(pair(Boolean.class, Integer.class), BooleanConversion::toInteger);
        DEFAULT_FACTORY.put(pair(Character.class, Integer.class), (fromInstance, converter, options) -> (int) (Character) fromInstance);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Integer.class), BooleanConversion::atomicToInteger);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Integer.class), NumberConversion::toInt);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Integer.class), NumberConversion::toInt);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Integer.class), NumberConversion::toInt);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Integer.class), NumberConversion::toInt);
        DEFAULT_FACTORY.put(pair(LocalDate.class, Integer.class), (fromInstance, converter, options) -> (int) ((LocalDate) fromInstance).toEpochDay());
        DEFAULT_FACTORY.put(pair(Number.class, Integer.class), NumberConversion::toInt);
        DEFAULT_FACTORY.put(pair(Map.class, Integer.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, int.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, Integer.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return CommonValues.INTEGER_ZERO;
            }
            try {
                return Integer.valueOf(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value: " + fromInstance + " not parseable as an integer value or outside " + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE);
            }
        });

        // Long/long conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, long.class), (fromInstance, converter, options) -> 0L);
        DEFAULT_FACTORY.put(pair(Void.class, Long.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Long.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(Short.class, Long.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(Integer.class, Long.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(Long.class, Long.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Float.class, Long.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(Double.class, Long.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(Boolean.class, Long.class), BooleanConversion::toLong);
        DEFAULT_FACTORY.put(pair(Character.class, Long.class), (fromInstance, converter, options) -> (long) ((char) fromInstance));
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Long.class), BooleanConversion::atomicToLong);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Long.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Long.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Long.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Long.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(Date.class, Long.class), (fromInstance, converter, options) -> ((Date) fromInstance).getTime());
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Long.class), (fromInstance, converter, options) -> ((Date) fromInstance).getTime());
        DEFAULT_FACTORY.put(pair(Timestamp.class, Long.class), (fromInstance, converter, options) -> ((Date) fromInstance).getTime());
        DEFAULT_FACTORY.put(pair(LocalDate.class, Long.class), (fromInstance, converter, options) -> ((LocalDate) fromInstance).toEpochDay());
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Long.class), (fromInstance, converter, options) -> localDateTimeToMillis((LocalDateTime) fromInstance, options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Long.class), (fromInstance, converter, options) -> zonedDateTimeToMillis((ZonedDateTime) fromInstance));
        DEFAULT_FACTORY.put(pair(Calendar.class, Long.class), (fromInstance, converter, options) -> ((Calendar) fromInstance).getTime().getTime());
        DEFAULT_FACTORY.put(pair(Number.class, Long.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(Map.class, Long.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, long.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, Long.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return CommonValues.LONG_ZERO;
            }
            try {
                return Long.valueOf(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value: " + fromInstance + " not parseable as a long value or outside " + Long.MIN_VALUE + " to " + Long.MAX_VALUE);
            }
        });

        // Float/float conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, float.class), (fromInstance, converter, options) -> 0.0f);
        DEFAULT_FACTORY.put(pair(Void.class, Float.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Float.class), NumberConversion::toFloat);
        DEFAULT_FACTORY.put(pair(Short.class, Float.class), NumberConversion::toFloat);
        DEFAULT_FACTORY.put(pair(Integer.class, Float.class), NumberConversion::toFloat);
        DEFAULT_FACTORY.put(pair(Long.class, Float.class), NumberConversion::toFloat);
        DEFAULT_FACTORY.put(pair(Float.class, Float.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Double.class, Float.class), NumberConversion::toFloat);
        DEFAULT_FACTORY.put(pair(Boolean.class, Float.class), BooleanConversion::toFloat);
        DEFAULT_FACTORY.put(pair(Character.class, Float.class), (fromInstance, converter, options) -> (float) ((char) fromInstance));
        DEFAULT_FACTORY.put(pair(LocalDate.class, Float.class), (fromInstance, converter, options) -> ((LocalDate) fromInstance).toEpochDay());
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Float.class), BooleanConversion::atomicToFloat);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Float.class), NumberConversion::toFloat);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Float.class), NumberConversion::toFloat);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Float.class), NumberConversion::toFloat);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Float.class), NumberConversion::toFloat);
        DEFAULT_FACTORY.put(pair(Number.class, Float.class), NumberConversion::toFloat);
        DEFAULT_FACTORY.put(pair(Map.class, Float.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, float.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, Float.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return CommonValues.FLOAT_ZERO;
            }
            try {
                return Float.valueOf(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value: " + fromInstance + " not parseable as a float value");
            }
        });

        // Double/double conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, double.class), NumberConversion::toDoubleZero);
        DEFAULT_FACTORY.put(pair(Void.class, Double.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Double.class), NumberConversion::toDouble);
        DEFAULT_FACTORY.put(pair(Short.class, Double.class), NumberConversion::toDouble);
        DEFAULT_FACTORY.put(pair(Integer.class, Double.class), NumberConversion::toDouble);
        DEFAULT_FACTORY.put(pair(Long.class, Double.class), NumberConversion::toDouble);
        DEFAULT_FACTORY.put(pair(Float.class, Double.class), NumberConversion::toDouble);
        DEFAULT_FACTORY.put(pair(Double.class, Double.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Boolean.class, Double.class), BooleanConversion::toDouble);
        DEFAULT_FACTORY.put(pair(Character.class, Double.class), (fromInstance, converter, options) -> (double) ((char) fromInstance));
        DEFAULT_FACTORY.put(pair(LocalDate.class, Double.class), (fromInstance, converter, options) -> (double) ((LocalDate) fromInstance).toEpochDay());
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Double.class), (fromInstance, converter, options) -> (double) localDateTimeToMillis((LocalDateTime) fromInstance, options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Double.class), (fromInstance, converter, options) -> (double) zonedDateTimeToMillis((ZonedDateTime) fromInstance));
        DEFAULT_FACTORY.put(pair(Date.class, Double.class), (fromInstance, converter, options) -> (double) ((Date) fromInstance).getTime());
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Double.class), (fromInstance, converter, options) -> (double) ((Date) fromInstance).getTime());
        DEFAULT_FACTORY.put(pair(Timestamp.class, Double.class), (fromInstance, converter, options) -> (double) ((Date) fromInstance).getTime());
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Double.class), BooleanConversion::atomicToDouble);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Double.class), NumberConversion::toDouble);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Double.class), NumberConversion::toDouble);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Double.class), NumberConversion::toDouble);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Double.class), NumberConversion::toDouble);
        DEFAULT_FACTORY.put(pair(Calendar.class, Double.class), (fromInstance, converter, options) -> (double) ((Calendar) fromInstance).getTime().getTime());
        DEFAULT_FACTORY.put(pair(Number.class, Double.class), NumberConversion::toDouble);
        DEFAULT_FACTORY.put(pair(Map.class, Double.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, double.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, Double.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return CommonValues.DOUBLE_ZERO;
            }
            try {
                return Double.valueOf(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value: " + fromInstance + " not parseable as a double value");
            }
        });

        // Boolean/boolean conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, boolean.class), (fromInstance, converter, options) -> false);
        DEFAULT_FACTORY.put(pair(Void.class, Boolean.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Boolean.class), NumberConversion::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(Short.class, Boolean.class), NumberConversion::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(Integer.class, Boolean.class), NumberConversion::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(Long.class, Boolean.class), NumberConversion::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(Float.class, Boolean.class), NumberConversion::isFloatTypeNotZero);
        DEFAULT_FACTORY.put(pair(Double.class, Boolean.class), NumberConversion::isFloatTypeNotZero);
        DEFAULT_FACTORY.put(pair(Boolean.class, Boolean.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Character.class, Boolean.class), (fromInstance, converter, options) -> ((char) fromInstance) > 0);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Boolean.class), (fromInstance, converter, options) -> ((AtomicBoolean) fromInstance).get());
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Boolean.class), NumberConversion::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Boolean.class), NumberConversion::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Boolean.class), NumberConversion::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Boolean.class), NumberConversion::isFloatTypeNotZero);
        DEFAULT_FACTORY.put(pair(Number.class, Boolean.class), NumberConversion::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(Map.class, Boolean.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, boolean.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, Boolean.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return false;
            }
            // faster equals check "true" and "false"
            if ("true".equals(str)) {
                return true;
            } else if ("false".equals(str)) {
                return false;
            }
            return "true".equalsIgnoreCase(str);
        });

        // Character/chat conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, char.class), (fromInstance, converter, options) -> (char) 0);
        DEFAULT_FACTORY.put(pair(Void.class, Character.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Character.class), NumberConversion::numberToCharacter);
        DEFAULT_FACTORY.put(pair(Short.class, Character.class), NumberConversion::numberToCharacter);
        DEFAULT_FACTORY.put(pair(Integer.class, Character.class), NumberConversion::numberToCharacter);
        DEFAULT_FACTORY.put(pair(Long.class, Character.class), NumberConversion::numberToCharacter);
        DEFAULT_FACTORY.put(pair(Float.class, Character.class), NumberConversion::numberToCharacter);
        DEFAULT_FACTORY.put(pair(Double.class, Character.class), NumberConversion::numberToCharacter);
        DEFAULT_FACTORY.put(pair(Boolean.class, Character.class), (fromInstance, converter, options) -> ((Boolean) fromInstance) ? '1' : '0');
        DEFAULT_FACTORY.put(pair(Character.class, Character.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Character.class), (fromInstance, converter, options) -> ((AtomicBoolean) fromInstance).get() ? '1' : '0');
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Character.class), NumberConversion::numberToCharacter);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Character.class), NumberConversion::numberToCharacter);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Character.class), NumberConversion::numberToCharacter);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Character.class), NumberConversion::numberToCharacter);
        DEFAULT_FACTORY.put(pair(Number.class, Character.class), NumberConversion::numberToCharacter);
        DEFAULT_FACTORY.put(pair(Map.class, Character.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, char.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, Character.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance);
            if (str.isEmpty()) {
                return (char) 0;
            }
            if (str.length() == 1) {
                return str.charAt(0);
            }
            // Treat as a String number, like "65" = 'A'
            return (char) Integer.parseInt(str.trim());
        });

        // BigInteger versions supported
        DEFAULT_FACTORY.put(pair(Void.class, BigInteger.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf((byte) fromInstance));
        DEFAULT_FACTORY.put(pair(Short.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf((short) fromInstance));
        DEFAULT_FACTORY.put(pair(Integer.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf((int) fromInstance));
        DEFAULT_FACTORY.put(pair(Long.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf((long) fromInstance));
        DEFAULT_FACTORY.put(pair(Float.class, BigInteger.class), (fromInstance, converter, options) -> new BigInteger(String.format("%.0f", (float) fromInstance)));
        DEFAULT_FACTORY.put(pair(Double.class, BigInteger.class), (fromInstance, converter, options) -> new BigInteger(String.format("%.0f", (double) fromInstance)));
        DEFAULT_FACTORY.put(pair(Boolean.class, BigInteger.class), (fromInstance, converter, options) -> (Boolean) fromInstance ? BigInteger.ONE : BigInteger.ZERO);
        DEFAULT_FACTORY.put(pair(Character.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf(((char) fromInstance)));
        DEFAULT_FACTORY.put(pair(BigInteger.class, BigInteger.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, BigInteger.class), (fromInstance, converter, options) -> ((BigDecimal) fromInstance).toBigInteger());
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, BigInteger.class), (fromInstance, converter, options) -> ((AtomicBoolean) fromInstance).get() ? BigInteger.ONE : BigInteger.ZERO);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf(((Number) fromInstance).intValue()));
        DEFAULT_FACTORY.put(pair(AtomicLong.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Date.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(Timestamp.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(LocalDate.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf(((LocalDate) fromInstance).toEpochDay()));
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf(localDateTimeToMillis((LocalDateTime) fromInstance, options.getSourceZoneId())));
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf(zonedDateTimeToMillis((ZonedDateTime) fromInstance)));
        DEFAULT_FACTORY.put(pair(UUID.class, BigInteger.class), (fromInstance, converter, options) -> {
            UUID uuid = (UUID) fromInstance;
            BigInteger mostSignificant = BigInteger.valueOf(uuid.getMostSignificantBits());
            BigInteger leastSignificant = BigInteger.valueOf(uuid.getLeastSignificantBits());
            // Shift the most significant bits to the left and add the least significant bits
            return mostSignificant.shiftLeft(64).add(leastSignificant);
        });
        DEFAULT_FACTORY.put(pair(Calendar.class, BigInteger.class), (fromInstance, converter, options) -> BigInteger.valueOf(((Calendar) fromInstance).getTime().getTime()));
        DEFAULT_FACTORY.put(pair(Number.class, BigInteger.class), (fromInstance, converter, options) -> new BigInteger(fromInstance.toString()));
        DEFAULT_FACTORY.put(pair(Map.class, BigInteger.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, BigInteger.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, BigInteger.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return BigInteger.ZERO;
            }
            try {
                return new BigInteger(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value: " + fromInstance + " not parseable as a BigInteger value.");
            }
        });

        // BigDecimal conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, BigDecimal.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, BigDecimal.class), NumberConversion::numberToBigDecimal);
        DEFAULT_FACTORY.put(pair(Short.class, BigDecimal.class), NumberConversion::numberToBigDecimal);
        DEFAULT_FACTORY.put(pair(Integer.class, BigDecimal.class), NumberConversion::numberToBigDecimal);
        DEFAULT_FACTORY.put(pair(Long.class, BigDecimal.class), NumberConversion::numberToBigDecimal);
        DEFAULT_FACTORY.put(pair(Float.class, BigDecimal.class), (fromInstance, converter, options) -> BigDecimal.valueOf((Float) fromInstance));
        DEFAULT_FACTORY.put(pair(Double.class, BigDecimal.class), (fromInstance, converter, options) -> BigDecimal.valueOf((Double) fromInstance));
        DEFAULT_FACTORY.put(pair(Boolean.class, BigDecimal.class), (fromInstance, converter, options) -> (Boolean) fromInstance ? BigDecimal.ONE : BigDecimal.ZERO);
        DEFAULT_FACTORY.put(pair(Character.class, BigDecimal.class), (fromInstance, converter, options) -> BigDecimal.valueOf(((char) fromInstance)));
        DEFAULT_FACTORY.put(pair(BigDecimal.class, BigDecimal.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(BigInteger.class, BigDecimal.class), (fromInstance, converter, options) -> new BigDecimal((BigInteger) fromInstance));
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, BigDecimal.class), (fromInstance, converter, options) -> ((AtomicBoolean) fromInstance).get() ? BigDecimal.ONE : BigDecimal.ZERO);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, BigDecimal.class), NumberConversion::numberToBigDecimal);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, BigDecimal.class), NumberConversion::numberToBigDecimal);
        DEFAULT_FACTORY.put(pair(Date.class, BigDecimal.class), (fromInstance, converter, options) -> BigDecimal.valueOf(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, BigDecimal.class), (fromInstance, converter, options) -> BigDecimal.valueOf(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(Timestamp.class, BigDecimal.class), (fromInstance, converter, options) -> BigDecimal.valueOf(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(LocalDate.class, BigDecimal.class), (fromInstance, converter, options) -> BigDecimal.valueOf(((LocalDate) fromInstance).toEpochDay()));
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, BigDecimal.class), (fromInstance, converter, options) -> BigDecimal.valueOf(localDateTimeToMillis((LocalDateTime) fromInstance, options.getSourceZoneId())));
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, BigDecimal.class), (fromInstance, converter, options) -> BigDecimal.valueOf(zonedDateTimeToMillis((ZonedDateTime) fromInstance)));
        DEFAULT_FACTORY.put(pair(UUID.class, BigDecimal.class), (fromInstance, converter, options) -> {
            UUID uuid = (UUID) fromInstance;
            BigInteger mostSignificant = BigInteger.valueOf(uuid.getMostSignificantBits());
            BigInteger leastSignificant = BigInteger.valueOf(uuid.getLeastSignificantBits());
            // Shift the most significant bits to the left and add the least significant bits
            return new BigDecimal(mostSignificant.shiftLeft(64).add(leastSignificant));
        });
        DEFAULT_FACTORY.put(pair(Calendar.class, BigDecimal.class), (fromInstance, converter, options) -> BigDecimal.valueOf(((Calendar) fromInstance).getTime().getTime()));
        DEFAULT_FACTORY.put(pair(Number.class, BigDecimal.class), (fromInstance, converter, options) -> new BigDecimal(fromInstance.toString()));
        DEFAULT_FACTORY.put(pair(Map.class, BigDecimal.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, BigDecimal.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, BigDecimal.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return BigDecimal.ZERO;
            }
            try {
                return new BigDecimal(str);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value: " + fromInstance + " not parseable as a BigDecimal value.");
            }
        });

        // AtomicBoolean conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, AtomicBoolean.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((Number) fromInstance).longValue() != 0));
        DEFAULT_FACTORY.put(pair(Short.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((Number) fromInstance).longValue() != 0));
        DEFAULT_FACTORY.put(pair(Integer.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((Number) fromInstance).longValue() != 0));
        DEFAULT_FACTORY.put(pair(Long.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((Number) fromInstance).longValue() != 0));
        DEFAULT_FACTORY.put(pair(Float.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((Number) fromInstance).longValue() != 0));
        DEFAULT_FACTORY.put(pair(Double.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((Number) fromInstance).longValue() != 0));
        DEFAULT_FACTORY.put(pair(Boolean.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean((Boolean) fromInstance));
        DEFAULT_FACTORY.put(pair(Character.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean((char) fromInstance > 0));
        DEFAULT_FACTORY.put(pair(BigInteger.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((Number) fromInstance).longValue() != 0));
        DEFAULT_FACTORY.put(pair(BigDecimal.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((Number) fromInstance).longValue() != 0));
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((AtomicBoolean) fromInstance).get()));  // mutable, so dupe
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((Number) fromInstance).intValue() != 0));
        DEFAULT_FACTORY.put(pair(AtomicLong.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((Number) fromInstance).longValue() != 0));
        DEFAULT_FACTORY.put(pair(Number.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicBoolean(((Number) fromInstance).longValue() != 0));
        DEFAULT_FACTORY.put(pair(Map.class, AtomicBoolean.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, AtomicBoolean.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, AtomicBoolean.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return new AtomicBoolean(false);
            }
            return new AtomicBoolean("true".equalsIgnoreCase(str));
        });

        // AtomicInteger conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, AtomicInteger.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger(((Number) fromInstance).intValue()));
        DEFAULT_FACTORY.put(pair(Short.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger(((Number) fromInstance).intValue()));
        DEFAULT_FACTORY.put(pair(Integer.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger(((Number) fromInstance).intValue()));
        DEFAULT_FACTORY.put(pair(Long.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger(((Number) fromInstance).intValue()));
        DEFAULT_FACTORY.put(pair(Float.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger(((Number) fromInstance).intValue()));
        DEFAULT_FACTORY.put(pair(Double.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger(((Number) fromInstance).intValue()));
        DEFAULT_FACTORY.put(pair(Boolean.class, AtomicInteger.class), (fromInstance, converter, options) -> ((Boolean) fromInstance) ? new AtomicInteger(1) : new AtomicInteger(0));
        DEFAULT_FACTORY.put(pair(Character.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger(((char) fromInstance)));
        DEFAULT_FACTORY.put(pair(BigInteger.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger(((Number) fromInstance).intValue()));
        DEFAULT_FACTORY.put(pair(BigDecimal.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger(((Number) fromInstance).intValue()));
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger(((Number) fromInstance).intValue())); // mutable, so dupe
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, AtomicInteger.class), (fromInstance, converter, options) -> ((AtomicBoolean) fromInstance).get() ? new AtomicInteger(1) : new AtomicInteger(0));
        DEFAULT_FACTORY.put(pair(AtomicLong.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger(((Number) fromInstance).intValue()));
        DEFAULT_FACTORY.put(pair(LocalDate.class, AtomicInteger.class), (fromInstance, converter, options) -> new AtomicInteger((int) ((LocalDate) fromInstance).toEpochDay()));
        DEFAULT_FACTORY.put(pair(Number.class, AtomicBoolean.class), (fromInstance, converter, options) -> new AtomicInteger(((Number) fromInstance).intValue()));
        DEFAULT_FACTORY.put(pair(Map.class, AtomicInteger.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, AtomicInteger.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, AtomicInteger.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return new AtomicInteger(0);
            }
            try {
                return new AtomicInteger(Integer.parseInt(str));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value: " + fromInstance + " not parseable as an AtomicInteger value or outside " + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE);
            }
        });

        // AtomicLong conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, AtomicLong.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Short.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Integer.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Long.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Float.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Double.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Boolean.class, AtomicLong.class), (fromInstance, converter, options) -> ((Boolean) fromInstance) ? new AtomicLong(1) : new AtomicLong(0));
        DEFAULT_FACTORY.put(pair(Character.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((char) fromInstance)));
        DEFAULT_FACTORY.put(pair(BigInteger.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(BigDecimal.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, AtomicLong.class), (fromInstance, converter, options) -> ((AtomicBoolean) fromInstance).get() ? new AtomicLong(1) : new AtomicLong(0));
        DEFAULT_FACTORY.put(pair(AtomicLong.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Number) fromInstance).longValue()));   // mutable, so dupe
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Date.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(Timestamp.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(LocalDate.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((LocalDate) fromInstance).toEpochDay()));
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(localDateTimeToMillis((LocalDateTime) fromInstance, options.getSourceZoneId())));
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(zonedDateTimeToMillis((ZonedDateTime) fromInstance)));
        DEFAULT_FACTORY.put(pair(Calendar.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Calendar) fromInstance).getTime().getTime()));
        DEFAULT_FACTORY.put(pair(Number.class, AtomicLong.class), (fromInstance, converter, options) -> new AtomicLong(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Map.class, AtomicLong.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, AtomicLong.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, AtomicLong.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            if (str.isEmpty()) {
                return new AtomicLong(0L);
            }
            try {
                return new AtomicLong(Long.parseLong(str));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Value: " + fromInstance + " not parseable as an AtomicLong value or outside " + Long.MIN_VALUE + " to " + Long.MAX_VALUE);
            }
        });

        // Date conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Date.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, Date.class), (fromInstance, converter, options) -> new Date((long) fromInstance));
        DEFAULT_FACTORY.put(pair(Double.class, Date.class), (fromInstance, converter, options) -> new Date(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(BigInteger.class, Date.class), (fromInstance, converter, options) -> new Date(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Date.class), (fromInstance, converter, options) -> new Date(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Date.class), (fromInstance, converter, options) -> new Date(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Date.class, Date.class), (fromInstance, converter, options) -> new Date(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Date.class), (fromInstance, converter, options) -> new Date(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(Timestamp.class, Date.class), (fromInstance, converter, options) -> new Date(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(LocalDate.class, Date.class), (fromInstance, converter, options) -> new Date(localDateToMillis((LocalDate) fromInstance, options.getSourceZoneId())));
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Date.class), (fromInstance, converter, options) -> new Date(localDateTimeToMillis((LocalDateTime) fromInstance, options.getSourceZoneId())));
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Date.class), (fromInstance, converter, options) -> new Date(zonedDateTimeToMillis((ZonedDateTime) fromInstance)));
        DEFAULT_FACTORY.put(pair(Calendar.class, Date.class), (fromInstance, converter, options) -> ((Calendar) fromInstance).getTime());
        DEFAULT_FACTORY.put(pair(Number.class, Date.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(Map.class, Date.class), (fromInstance, converter, options) -> {
            Map<?, ?> map = (Map<?, ?>) fromInstance;
            if (map.containsKey("time")) {
                return converter.convert(map.get("time"), Date.class, options);
            } else {
                return converter.fromValueMap(map, Date.class, CollectionUtilities.setOf("time"), options);
            }
        });
        DEFAULT_FACTORY.put(pair(String.class, Date.class), (fromInstance, converter, options) -> DateUtilities.parseDate(((String) fromInstance).trim()));

        // java.sql.Date conversion supported
        DEFAULT_FACTORY.put(pair(Void.class, java.sql.Date.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date((long) fromInstance));
        DEFAULT_FACTORY.put(pair(Double.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(BigInteger.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(BigDecimal.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(AtomicLong.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date(((java.sql.Date) fromInstance).getTime()));  // java.sql.Date is mutable
        DEFAULT_FACTORY.put(pair(Date.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(Timestamp.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(LocalDate.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date(localDateToMillis((LocalDate) fromInstance, options.getSourceZoneId())));
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date(localDateTimeToMillis((LocalDateTime) fromInstance, options.getSourceZoneId())));
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date(zonedDateTimeToMillis((ZonedDateTime) fromInstance)));
        DEFAULT_FACTORY.put(pair(Calendar.class, java.sql.Date.class), (fromInstance, converter, options) -> new java.sql.Date(((Calendar) fromInstance).getTime().getTime()));
        DEFAULT_FACTORY.put(pair(Number.class, java.sql.Date.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(Map.class, java.sql.Date.class), (fromInstance, converter, options) -> {
            Map<?, ?> map = (Map<?, ?>) fromInstance;
            if (map.containsKey("time")) {
                return converter.convert(map.get("time"), java.sql.Date.class, options);
            } else {
                return converter.fromValueMap((Map<?, ?>) fromInstance, java.sql.Date.class, CollectionUtilities.setOf("time"), options);
            }
        });
        DEFAULT_FACTORY.put(pair(String.class, java.sql.Date.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            Date date = DateUtilities.parseDate(str);
            if (date == null) {
                return null;
            }
            return new java.sql.Date(date.getTime());
        });

        // Timestamp conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Timestamp.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp((long) fromInstance));
        DEFAULT_FACTORY.put(pair(Double.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(BigInteger.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Timestamp.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp(((Timestamp) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(Date.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(LocalDate.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp(localDateToMillis((LocalDate) fromInstance, options.getSourceZoneId())));
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp(localDateTimeToMillis((LocalDateTime) fromInstance, options.getSourceZoneId())));
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp(zonedDateTimeToMillis((ZonedDateTime) fromInstance)));
        DEFAULT_FACTORY.put(pair(Calendar.class, Timestamp.class), (fromInstance, converter, options) -> new Timestamp(((Calendar) fromInstance).getTime().getTime()));
        DEFAULT_FACTORY.put(pair(Number.class, Timestamp.class), NumberConversion::toLong);
        DEFAULT_FACTORY.put(pair(Map.class, Timestamp.class), (fromInstance, converter, options) -> {
            Map<?, ?> map = (Map<?, ?>) fromInstance;
            if (map.containsKey("time")) {
                long time = converter.convert(map.get("time"), long.class, options);
                int ns = converter.convert(map.get("nanos"), int.class, options);
                Timestamp timeStamp = new Timestamp(time);
                timeStamp.setNanos(ns);
                return timeStamp;
            } else {
                return converter.fromValueMap(map, Timestamp.class, CollectionUtilities.setOf("time", "nanos"), options);
            }
        });
        DEFAULT_FACTORY.put(pair(String.class, Timestamp.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            Date date = DateUtilities.parseDate(str);
            if (date == null) {
                return null;
            }
            return new Timestamp(date.getTime());
        });

        // Calendar conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Calendar.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, Calendar.class), (fromInstance, converter, options) -> initCal((Long) fromInstance));
        DEFAULT_FACTORY.put(pair(Double.class, Calendar.class), (fromInstance, converter, options) -> initCal(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(BigInteger.class, Calendar.class), (fromInstance, converter, options) -> initCal(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Calendar.class), (fromInstance, converter, options) -> initCal(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Calendar.class), (fromInstance, converter, options) -> initCal(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Date.class, Calendar.class), (fromInstance, converter, options) -> initCal(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Calendar.class), (fromInstance, converter, options) -> initCal(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(Timestamp.class, Calendar.class), (fromInstance, converter, options) -> initCal(((Date) fromInstance).getTime()));
        DEFAULT_FACTORY.put(pair(LocalDate.class, Calendar.class), (fromInstance, converter, options) -> initCal(localDateToMillis((LocalDate) fromInstance, options.getSourceZoneId())));
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Calendar.class), (fromInstance, converter, options) -> initCal(localDateTimeToMillis((LocalDateTime) fromInstance, options.getSourceZoneId())));
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Calendar.class), (fromInstance, converter, options) -> initCal(zonedDateTimeToMillis((ZonedDateTime) fromInstance)));
        DEFAULT_FACTORY.put(pair(Calendar.class, Calendar.class), (fromInstance, converter, options) -> ((Calendar) fromInstance).clone());
        DEFAULT_FACTORY.put(pair(Number.class, Calendar.class), (fromInstance, converter, options) -> initCal(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Map.class, Calendar.class), (fromInstance, converter, options) -> {
            Map<?, ?> map = (Map<?, ?>) fromInstance;
            if (map.containsKey("time")) {
                Object zoneRaw = map.get("zone");
                TimeZone tz;
                if (zoneRaw instanceof String) {
                    String zone = (String) zoneRaw;
                    tz = TimeZone.getTimeZone(zone);
                } else {
                    tz = TimeZone.getTimeZone(options.getTargetZoneId());
                }
                Calendar cal = Calendar.getInstance();
                cal.setTimeZone(tz);
                Date epochInMillis = converter.convert(map.get("time"), Date.class, options);
                cal.setTimeInMillis(epochInMillis.getTime());
                return cal;
            } else {
                return converter.fromValueMap(map, Calendar.class, CollectionUtilities.setOf("time", "zone"), options);
            }
        });
        DEFAULT_FACTORY.put(pair(String.class, Calendar.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            Date date = DateUtilities.parseDate(str);
            if (date == null) {
                return null;
            }
            return initCal(date.getTime());
        });

        // LocalTime conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, LocalTime.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(LocalTime.class, LocalTime.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(String.class, LocalTime.class), (fromInstance, converter, options) -> {
            String strTime = (String) fromInstance;
            try {
                return LocalTime.parse(strTime);
            } catch (Exception e) {
                return DateUtilities.parseDate(strTime).toInstant().atZone(ZoneId.systemDefault()).toLocalTime();
            }
        });
        DEFAULT_FACTORY.put(pair(Map.class, LocalTime.class), (fromInstance, converter, options) -> {
            Map<?, ?> map = (Map<?, ?>) fromInstance;
            if (map.containsKey("hour") && map.containsKey("minute")) {
                int hour = converter.convert(map.get("hour"), int.class, options);
                int minute = converter.convert(map.get("minute"), int.class, options);
                int second = converter.convert(map.get("second"), int.class, options);
                int nano = converter.convert(map.get("nano"), int.class, options);
                return LocalTime.of(hour, minute, second, nano);
            } else {
                return converter.fromValueMap(map, LocalTime.class, CollectionUtilities.setOf("hour", "minute", "second", "nano"), options);
            }
        });

        // LocalDate conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, LocalDate.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Short.class, LocalDate.class), (fromInstance, converter, options) -> LocalDate.ofEpochDay(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Integer.class, LocalDate.class), (fromInstance, converter, options) -> LocalDate.ofEpochDay(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Long.class, LocalDate.class), (fromInstance, converter, options) -> LocalDate.ofEpochDay(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Float.class, LocalDate.class), (fromInstance, converter, options) -> LocalDate.ofEpochDay(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Double.class, LocalDate.class), (fromInstance, converter, options) -> LocalDate.ofEpochDay(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(BigInteger.class, LocalDate.class), (fromInstance, converter, options) -> LocalDate.ofEpochDay(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(BigDecimal.class, LocalDate.class), (fromInstance, converter, options) -> LocalDate.ofEpochDay(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, LocalDate.class), (fromInstance, converter, options) -> LocalDate.ofEpochDay(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(AtomicLong.class, LocalDate.class), (fromInstance, converter, options) -> LocalDate.ofEpochDay(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, LocalDate.class), (fromInstance, converter, options) -> ((java.sql.Date) fromInstance).toLocalDate());
        DEFAULT_FACTORY.put(pair(Timestamp.class, LocalDate.class), (fromInstance, converter, options) -> ((Timestamp) fromInstance).toLocalDateTime().toLocalDate());
        DEFAULT_FACTORY.put(pair(Date.class, LocalDate.class), (fromInstance, converter, options) -> ((Date) fromInstance).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        DEFAULT_FACTORY.put(pair(LocalDate.class, LocalDate.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, LocalDate.class), (fromInstance, converter, options) -> ((LocalDateTime) fromInstance).toLocalDate());
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, LocalDate.class), (fromInstance, converter, options) -> ((ZonedDateTime) fromInstance).toLocalDate());
        DEFAULT_FACTORY.put(pair(Calendar.class, LocalDate.class), (fromInstance, converter, options) -> ((Calendar) fromInstance).toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        DEFAULT_FACTORY.put(pair(Number.class, LocalDate.class), (fromInstance, converter, options) -> LocalDate.ofEpochDay(((Number) fromInstance).longValue()));
        DEFAULT_FACTORY.put(pair(Map.class, LocalDate.class), (fromInstance, converter, options) -> {
            Map<?, ?> map = (Map<?, ?>) fromInstance;
            if (map.containsKey("month") && map.containsKey("day") && map.containsKey("year")) {
                int month = converter.convert(map.get("month"), int.class, options);
                int day = converter.convert(map.get("day"), int.class, options);
                int year = converter.convert(map.get("year"), int.class, options);
                return LocalDate.of(year, month, day);
            } else {
                return converter.fromValueMap(map, LocalDate.class, CollectionUtilities.setOf("year", "month", "day"), options);
            }
        });
        DEFAULT_FACTORY.put(pair(String.class, LocalDate.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            Date date = DateUtilities.parseDate(str);
            if (date == null) {
                return null;
            }
            return date.toInstant().atZone(options.getTargetZoneId()).toLocalDate();
        });

        // LocalDateTime conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, LocalDateTime.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, LocalDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli((Long) fromInstance).atZone(options.getSourceZoneId()).toLocalDateTime());
        DEFAULT_FACTORY.put(pair(Double.class, LocalDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli(((Number) fromInstance).longValue()).atZone(options.getSourceZoneId()).toLocalDateTime());
        DEFAULT_FACTORY.put(pair(BigInteger.class, LocalDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli(((Number) fromInstance).longValue()).atZone(options.getSourceZoneId()).toLocalDateTime());
        DEFAULT_FACTORY.put(pair(BigDecimal.class, LocalDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli(((Number) fromInstance).longValue()).atZone(options.getSourceZoneId()).toLocalDateTime());
        DEFAULT_FACTORY.put(pair(AtomicLong.class, LocalDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli(((Number) fromInstance).longValue()).atZone(options.getSourceZoneId()).toLocalDateTime());
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, LocalDateTime.class), (fromInstance, converter, options) -> ((java.sql.Date) fromInstance).toLocalDate().atStartOfDay());
        DEFAULT_FACTORY.put(pair(Timestamp.class, LocalDateTime.class), (fromInstance, converter, options) -> ((Timestamp) fromInstance).toLocalDateTime());
        DEFAULT_FACTORY.put(pair(Date.class, LocalDateTime.class), (fromInstance, converter, options) -> ((Date) fromInstance).toInstant().atZone(options.getSourceZoneId()).toLocalDateTime());
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, LocalDateTime.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(LocalDate.class, LocalDateTime.class), (fromInstance, converter, options) -> ((LocalDate) fromInstance).atStartOfDay());
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, LocalDateTime.class), (fromInstance, converter, options) -> ((ZonedDateTime) fromInstance).toLocalDateTime());
        DEFAULT_FACTORY.put(pair(Calendar.class, LocalDateTime.class), (fromInstance, converter, options) -> ((Calendar) fromInstance).toInstant().atZone(options.getSourceZoneId()).toLocalDateTime());
        DEFAULT_FACTORY.put(pair(Number.class, LocalDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli(((Number) fromInstance).longValue()).atZone(options.getSourceZoneId()).toLocalDateTime());
        DEFAULT_FACTORY.put(pair(Map.class, LocalDateTime.class), (fromInstance, converter, options) -> {
            Map<?, ?> map = (Map<?, ?>) fromInstance;
            return converter.fromValueMap(map, LocalDateTime.class, null, options);
        });
        DEFAULT_FACTORY.put(pair(String.class, LocalDateTime.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            Date date = DateUtilities.parseDate(str);
            if (date == null) {
                return null;
            }
            return date.toInstant().atZone(options.getSourceZoneId()).toLocalDateTime();
        });

        // ZonedDateTime conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, ZonedDateTime.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, ZonedDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli((Long) fromInstance).atZone(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(Double.class, ZonedDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli(((Number) fromInstance).longValue()).atZone(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(BigInteger.class, ZonedDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli(((Number) fromInstance).longValue()).atZone(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(BigDecimal.class, ZonedDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli(((Number) fromInstance).longValue()).atZone(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(AtomicLong.class, ZonedDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli(((Number) fromInstance).longValue()).atZone(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, ZonedDateTime.class), (fromInstance, converter, options) -> ((java.sql.Date) fromInstance).toLocalDate().atStartOfDay(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(Timestamp.class, ZonedDateTime.class), (fromInstance, converter, options) -> ((Timestamp) fromInstance).toInstant().atZone(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(Date.class, ZonedDateTime.class), (fromInstance, converter, options) -> ((Date) fromInstance).toInstant().atZone(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(LocalDate.class, ZonedDateTime.class), (fromInstance, converter, options) -> ((LocalDate) fromInstance).atStartOfDay(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, ZonedDateTime.class), (fromInstance, converter, options) -> ((LocalDateTime) fromInstance).atZone(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, ZonedDateTime.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Calendar.class, ZonedDateTime.class), (fromInstance, converter, options) -> ((Calendar) fromInstance).toInstant().atZone(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(Number.class, ZonedDateTime.class), (fromInstance, converter, options) -> Instant.ofEpochMilli(((Number) fromInstance).longValue()).atZone(options.getSourceZoneId()));
        DEFAULT_FACTORY.put(pair(Map.class, ZonedDateTime.class), (fromInstance, converter, options) -> {
            Map<?, ?> map = (Map<?, ?>) fromInstance;
            return converter.fromValueMap(map, ZonedDateTime.class, null, options);
        });
        DEFAULT_FACTORY.put(pair(String.class, ZonedDateTime.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            Date date = DateUtilities.parseDate(str);
            if (date == null) {
                return null;
            }
            return date.toInstant().atZone(options.getSourceZoneId());
        });

        // UUID conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, UUID.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(UUID.class, UUID.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(String.class, UUID.class), (fromInstance, converter, options) -> UUID.fromString(((String) fromInstance).trim()));
        DEFAULT_FACTORY.put(pair(BigInteger.class, UUID.class), (fromInstance, converter, options) -> {
            BigInteger bigInteger = (BigInteger) fromInstance;
            BigInteger mask = BigInteger.valueOf(Long.MAX_VALUE);
            long mostSignificantBits = bigInteger.shiftRight(64).and(mask).longValue();
            long leastSignificantBits = bigInteger.and(mask).longValue();
            return new UUID(mostSignificantBits, leastSignificantBits);
        });
        DEFAULT_FACTORY.put(pair(BigDecimal.class, UUID.class), (fromInstance, converter, options) -> {
            BigInteger bigInt = ((BigDecimal) fromInstance).toBigInteger();
            long mostSigBits = bigInt.shiftRight(64).longValue();
            long leastSigBits = bigInt.and(new BigInteger("FFFFFFFFFFFFFFFF", 16)).longValue();
            return new UUID(mostSigBits, leastSigBits);
        });
        DEFAULT_FACTORY.put(pair(Map.class, UUID.class), (fromInstance, converter, options) -> {
            Map<?, ?> map = (Map<?, ?>) fromInstance;
            Object ret = converter.fromMap(map, "mostSigBits", long.class, options);
            if (ret != NOPE) {
                Object ret2 = converter.fromMap(map, "leastSigBits", long.class, options);
                if (ret2 != NOPE) {
                    return new UUID((Long) ret, (Long) ret2);
                }
            }
            throw new IllegalArgumentException("To convert Map to UUID, the Map must contain both 'mostSigBits' and 'leastSigBits' keys");
        });

        // Class conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Class.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Class.class, Class.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Map.class, Class.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, AtomicLong.class, null, options));
        DEFAULT_FACTORY.put(pair(String.class, Class.class), (fromInstance, converter, options) -> {
            String str = ((String) fromInstance).trim();
            Class<?> clazz = ClassUtilities.forName(str, options.getClassLoader());
            if (clazz != null) {
                return clazz;
            }
            throw new IllegalArgumentException("Cannot convert String '" + str + "' to class.  Class not found.");
        });

        // String conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, String.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(Short.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(Integer.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(Long.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(Float.class, String.class), (fromInstance, converter, options) -> new DecimalFormat("#.####################").format((float) fromInstance));
        DEFAULT_FACTORY.put(pair(Double.class, String.class), (fromInstance, converter, options) -> new DecimalFormat("#.####################").format((double) fromInstance));
        DEFAULT_FACTORY.put(pair(Boolean.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(Character.class, String.class), (fromInstance, converter, options) -> "" + fromInstance);
        DEFAULT_FACTORY.put(pair(BigInteger.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, String.class), (fromInstance, converter, options) -> ((BigDecimal) fromInstance).stripTrailingZeros().toPlainString());
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(Class.class, String.class), (fromInstance, converter, options) -> ((Class<?>) fromInstance).getName());
        DEFAULT_FACTORY.put(pair(Date.class, String.class), (fromInstance, converter, options) -> {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return simpleDateFormat.format(((Date) fromInstance));
        });
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, String.class), (fromInstance, converter, options) -> {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return simpleDateFormat.format(((Date) fromInstance));
        });
        DEFAULT_FACTORY.put(pair(Timestamp.class, String.class), (fromInstance, converter, options) -> {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return simpleDateFormat.format(((Date) fromInstance));
        });
        DEFAULT_FACTORY.put(pair(LocalDate.class, String.class), (fromInstance, converter, options) -> {
            LocalDate localDate = (LocalDate) fromInstance;
            return String.format("%04d-%02d-%02d", localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth());
        });
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, String.class), (fromInstance, converter, options) -> {
            LocalDateTime localDateTime = (LocalDateTime) fromInstance;
            return String.format("%04d-%02d-%02dT%02d:%02d:%02d", localDateTime.getYear(), localDateTime.getMonthValue(), localDateTime.getDayOfMonth(), localDateTime.getHour(), localDateTime.getMinute(), localDateTime.getSecond());
        });
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, String.class), (fromInstance, converter, options) -> {
            ZonedDateTime zonedDateTime = (ZonedDateTime) fromInstance;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
            return zonedDateTime.format(formatter);
        });
        DEFAULT_FACTORY.put(pair(UUID.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(Calendar.class, String.class), (fromInstance, converter, options) -> {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return simpleDateFormat.format(((Calendar) fromInstance).getTime());
        });
        DEFAULT_FACTORY.put(pair(Number.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(Map.class, String.class), (fromInstance, converter, options) -> converter.fromValueMap((Map<?, ?>) fromInstance, String.class, null, options));
        DEFAULT_FACTORY.put(pair(Enum.class, String.class), (fromInstance, converter, options) -> ((Enum<?>) fromInstance).name());
        DEFAULT_FACTORY.put(pair(String.class, String.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Duration.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(Instant.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(LocalTime.class, String.class), Converter::toString);
        DEFAULT_FACTORY.put(pair(MonthDay.class, String.class), Converter::toString);

        // Duration conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Duration.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Duration.class, Duration.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(String.class, Duration.class), (fromInstance, converter, options) -> Duration.parse((String) fromInstance));
        DEFAULT_FACTORY.put(pair(Map.class, Duration.class), (fromInstance, converter, options) -> {
            Map<String, Object> map = (Map<String, Object>) fromInstance;
            if (map.containsKey("seconds")) {
                long sec = converter.convert(map.get("seconds"), long.class, options);
                long nanos = converter.convert(map.get("nanos"), long.class, options);
                return Duration.ofSeconds(sec, nanos);
            } else {
                return converter.fromValueMap(map, Duration.class, CollectionUtilities.setOf("seconds", "nanos"), options);
            }
        });

        // Instant conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Instant.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Instant.class, Instant.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(String.class, Instant.class), (fromInstance, converter, options) -> {
            try {
                return Instant.parse((String) fromInstance);
            } catch (Exception e) {
                return DateUtilities.parseDate((String) fromInstance).toInstant();
            }
        });
        DEFAULT_FACTORY.put(pair(Map.class, Instant.class), (fromInstance, converter, options) -> {
            Map<String, Object> map = (Map<String, Object>) fromInstance;
            if (map.containsKey("seconds")) {
                long sec = converter.convert(map.get("seconds"), long.class, options);
                long nanos = converter.convert(map.get("nanos"), long.class, options);
                return Instant.ofEpochSecond(sec, nanos);
            } else {
                return converter.fromValueMap(map, Instant.class, CollectionUtilities.setOf("seconds", "nanos"), options);
            }
        });

//        java.time.OffsetDateTime = com.cedarsoftware.util.io.DEFAULT_FACTORY.OffsetDateTimeFactory
//        java.time.OffsetTime = com.cedarsoftware.util.io.DEFAULT_FACTORY.OffsetTimeFactory
//        java.time.Period = com.cedarsoftware.util.io.DEFAULT_FACTORY.PeriodFactory
//        java.time.Year = com.cedarsoftware.util.io.DEFAULT_FACTORY.YearFactory
//        java.time.YearMonth = com.cedarsoftware.util.io.DEFAULT_FACTORY.YearMonthFactory
//        java.time.ZoneId = com.cedarsoftware.util.io.DEFAULT_FACTORY.ZoneIdFactory
//        java.time.ZoneOffset = com.cedarsoftware.util.io.DEFAULT_FACTORY.ZoneOffsetFactory
//        java.time.ZoneRegion = com.cedarsoftware.util.io.DEFAULT_FACTORY.ZoneIdFactory

        // MonthDay conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, MonthDay.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(MonthDay.class, MonthDay.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(String.class, MonthDay.class), (fromInstance, converter, options) -> {
            String monthDay = (String) fromInstance;
            return MonthDay.parse(monthDay);
        });
        DEFAULT_FACTORY.put(pair(Map.class, MonthDay.class), (fromInstance, converter, options) -> {
            Map<String, Object> map = (Map<String, Object>) fromInstance;
            if (map.containsKey("month")) {
                int month = converter.convert(map.get("month"), int.class, options);
                int day = converter.convert(map.get("day"), int.class, options);
                return MonthDay.of(month, day);
            } else {
                return converter.fromValueMap(map, MonthDay.class, CollectionUtilities.setOf("month", "day"), options);
            }
        });

        // Map conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Map.class), VoidConversion::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Short.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Integer.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Long.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Float.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Double.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Boolean.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Character.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(BigInteger.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Date.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Timestamp.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(LocalDate.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Duration.class, Map.class), (fromInstance, converter, options) -> {
            long sec = ((Duration) fromInstance).getSeconds();
            long nanos = ((Duration) fromInstance).getNano();
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("seconds", sec);
            target.put("nanos", nanos);
            return target;
        });
        DEFAULT_FACTORY.put(pair(Instant.class, Map.class), (fromInstance, converter, options) -> {
            long sec = ((Instant) fromInstance).getEpochSecond();
            long nanos = ((Instant) fromInstance).getNano();
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("seconds", sec);
            target.put("nanos", nanos);
            return target;
        });
        DEFAULT_FACTORY.put(pair(LocalTime.class, Map.class), (fromInstance, converter, options) -> {
            LocalTime localTime = (LocalTime) fromInstance;
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("hour", localTime.getHour());
            target.put("minute", localTime.getMinute());
            if (localTime.getNano() != 0) {  // Only output 'nano' when not 0 (and then 'second' is required).
                target.put("nano", localTime.getNano());
                target.put("second", localTime.getSecond());
            } else {    // 0 nano, 'second' is optional if 0
                if (localTime.getSecond() != 0) {
                    target.put("second", localTime.getSecond());
                }
            }
            return target;
        });
        DEFAULT_FACTORY.put(pair(MonthDay.class, Map.class), (fromInstance, converter, options) -> {
            MonthDay monthDay = (MonthDay) fromInstance;
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("day", monthDay.getDayOfMonth());
            target.put("month", monthDay.getMonthValue());
            return target;
        });
        DEFAULT_FACTORY.put(pair(Class.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(UUID.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Calendar.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Number.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
        DEFAULT_FACTORY.put(pair(Map.class, Map.class), (fromInstance, converter, options) -> {
            Map<?, ?> source = (Map<?, ?>) fromInstance;
            Map<?, ?> copy = new LinkedHashMap<>(source);
            return copy;
        });
        DEFAULT_FACTORY.put(pair(Enum.class, Map.class), (fromInstance, converter, options) -> initMap(fromInstance));
    }

    public Converter(ConverterOptions options) {
        this.options = options;
        this.factory = new ConcurrentHashMap<>(DEFAULT_FACTORY);
    }

    /**
     * Turn the passed in value to the class indicated.  This will allow, for
     * example, a String to be passed in and be converted to a Long.
     * <pre>
     *     Examples:
     *     Long x = convert("35", Long.class);
     *     Date d = convert("2015/01/01", Date.class)
     *     int y = convert(45.0, int.class)
     *     String date = convert(date, String.class)
     *     String date = convert(calendar, String.class)
     *     Short t = convert(true, short.class);     // returns (short) 1 or  (short) 0
     *     Long date = convert(calendar, long.class); // get calendar's time into long
     *     Map containing ["_v": "75.0"]
     *     convert(map, double.class)   // Converter will extract the value associated to the "_v" (or "value") key and convert it.
     * </pre>
     *
     * @param fromInstance A value used to create the targetType, even though it may
     *                     not (most likely will not) be the same data type as the targetType
     * @param toType       Class which indicates the targeted (final) data type.
     *                     Please note that in addition to the 8 Java primitives, the targeted class
     *                     can also be Date.class, String.class, BigInteger.class, BigDecimal.class, and
     *                     many other JDK classes, including Map.  For Map, often it will seek a 'value'
     *                     field, however, for some complex objects, like UUID, it will look for specific
     *                     fields within the Map to perform the conversion.
     * @return An instanceof targetType class, based upon the value passed in.
     */
    @SuppressWarnings("unchecked")
    public <T> T convert(Object fromInstance, Class<T> toType) {
        return this.convert(fromInstance, toType, options);
    }

    /**
     * Turn the passed in value to the class indicated.  This will allow, for
     * example, a String to be passed in and be converted to a Long.
     * <pre>
     *     Examples:
     *     Long x = convert("35", Long.class);
     *     Date d = convert("2015/01/01", Date.class)
     *     int y = convert(45.0, int.class)
     *     String date = convert(date, String.class)
     *     String date = convert(calendar, String.class)
     *     Short t = convert(true, short.class);     // returns (short) 1 or  (short) 0
     *     Long date = convert(calendar, long.class); // get calendar's time into long
     *     Map containing ["_v": "75.0"]
     *     convert(map, double.class)   // Converter will extract the value associated to the "_v" (or "value") key and convert it.
     * </pre>
     *
     * @param fromInstance A value used to create the targetType, even though it may
     *                     not (most likely will not) be the same data type as the targetType
     * @param toType       Class which indicates the targeted (final) data type.
     *                     Please note that in addition to the 8 Java primitives, the targeted class
     *                     can also be Date.class, String.class, BigInteger.class, BigDecimal.class, and
     *                     many other JDK classes, including Map.  For Map, often it will seek a 'value'
     *                     field, however, for some complex objects, like UUID, it will look for specific
     *                     fields within the Map to perform the conversion.
     * @param options      ConverterOptions - allows you to specify locale, ZoneId, etc to support conversion
     *                     operations.
     * @return An instanceof targetType class, based upon the value passed in.
     */
    @SuppressWarnings("unchecked")
    public <T> T convert(Object fromInstance, Class<T> toType, ConverterOptions options) {
        if (toType == null) {
            throw new IllegalArgumentException("toType cannot be null");
        }
        Class<?> sourceType;
        if (fromInstance == null) {
            // Do not promote primitive to primitive wrapper - allows for different 'from NULL' type for each.
            sourceType = Void.class;
        } else {
            // Promote primitive to primitive wrapper so we don't have to define so many duplicates in the factory map.
            sourceType = fromInstance.getClass();
            if (toType.isPrimitive()) {
                toType = (Class<T>) toPrimitiveWrapperClass(toType);
            }
        }

        // Direct Mapping
        Convert<?> converter = factory.get(pair(sourceType, toType));
        if (converter != null) {
            return (T) converter.convert(fromInstance, this, options);
        }

        // Try inheritance
        converter = getInheritedConverter(sourceType, toType);
        if (converter != null) {
            return (T) converter.convert(fromInstance, this, options);
        }

        throw new IllegalArgumentException("Unsupported conversion, source type [" + name(fromInstance) + "] target type '" + getShortName(toType) + "'");
    }

    /**
     * Expected that source and target classes, if primitive, have already been shifted to primitive wrapper classes.
     */
    private <T> Convert<?> getInheritedConverter(Class<?> sourceType, Class<T> toType) {
        Set<Class<?>> sourceTypes = new TreeSet<>(getClassComparator());
        Set<Class<?>> targetTypes = new TreeSet<>(getClassComparator());

        sourceTypes.addAll(getSuperClassesAndInterfaces(sourceType));
        sourceTypes.add(sourceType);
        targetTypes.addAll(getSuperClassesAndInterfaces(toType));
        targetTypes.add(toType);

        Class<?> sourceClass = sourceType;
        Class<?> targetClass = toType;

        for (Class<?> toClass : targetTypes) {
            sourceClass = null;
            targetClass = null;

            for (Class<?> fromClass : sourceTypes) {
                if (factory.containsKey(pair(fromClass, toClass))) {
                    sourceClass = fromClass;
                    targetClass = toClass;
                    break;
                }
            }

            if (sourceClass != null && targetClass != null) {
                break;
            }
        }

        Convert<?> converter = factory.get(pair(sourceClass, targetClass));
        return converter;
    }

    private static Comparator<Class<?>> getClassComparator() {
        return (c1, c2) -> {
            if (c1.isInterface() == c2.isInterface()) {
                // By name
                return c1.getName().compareToIgnoreCase(c2.getName());
            }
            return c1.isInterface() ? 1 : -1;
        };
    }

    private static Set<Class<?>> getSuperClassesAndInterfaces(Class<?> clazz) {

        Set<Class<?>> parentTypes = cacheParentTypes.get(clazz);
        if (parentTypes != null) {
            return parentTypes;
        }
        parentTypes = new ConcurrentSkipListSet<>(getClassComparator());
        addSuperClassesAndInterfaces(clazz, parentTypes);
        cacheParentTypes.put(clazz, parentTypes);
        return parentTypes;
    }

    private static void addSuperClassesAndInterfaces(Class<?> clazz, Set<Class<?>> result) {
        // Add all superinterfaces
        for (Class<?> iface : clazz.getInterfaces()) {
            result.add(iface);
            addSuperClassesAndInterfaces(iface, result);
        }

        // Add superclass
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            result.add(superClass);
            addSuperClassesAndInterfaces(superClass, result);
        }
    }

    private static String getShortName(Class<?> type) {
        return java.sql.Date.class.equals(type) ? type.getName() : type.getSimpleName();
    }

    private String name(Object fromInstance) {
        if (fromInstance == null) {
            return "null";
        }
        return getShortName(fromInstance.getClass()) + " (" + fromInstance + ")";
    }

    private static Calendar initCal(long ms) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.setTimeInMillis(ms);
        return cal;
    }

    private static Map<String, ?> initMap(Object fromInstance) {
        Map<String, Object> map = new HashMap<>();
        map.put(VALUE, fromInstance);
        return map;
    }

    private Object fromValueMap(Map<?, ?> map, Class<?> type, Set<String> set, ConverterOptions options) {
        Object ret = fromMap(map, VALUE, type, this.options);
        if (ret != NOPE) {
            return ret;
        }

        ret = fromMap(map, VALUE2, type, this.options);
        if (ret == NOPE) {
            if (set == null || set.isEmpty()) {
                throw new IllegalArgumentException("To convert from Map to " + getShortName(type) + ", the map must include keys: '_v' or 'value' an associated value to convert from.");
            } else {
                throw new IllegalArgumentException("To convert from Map to " + getShortName(type) + ", the map must include keys: " + set + ", or '_v' or 'value' an associated value to convert from.");
            }
        }
        return ret;
    }

    private Object fromMap(Map<?, ?> map, String key, Class<?> type, ConverterOptions options) {
        if (map.containsKey(key)) {
            return convert(map.get(key), type, options);
        }
        return NOPE;
    }

    /**
     * Check to see if a direct-conversion from type to another type is supported.
     *
     * @param source Class of source type.
     * @param target Class of target type.
     * @return boolean true if the Converter converts from the source type to the destination type, false otherwise.
     */
    public boolean isDirectConversionSupportedFor(Class<?> source, Class<?> target) {
        source = toPrimitiveWrapperClass(source);
        target = toPrimitiveWrapperClass(target);
        return factory.containsKey(pair(source, target));
    }

    /**
     * Check to see if a conversion from type to another type is supported (may use inheritance via super classes/interfaces).
     *
     * @param source Class of source type.
     * @param target Class of target type.
     * @return boolean true if the Converter converts from the source type to the destination type, false otherwise.
     */
    public boolean isConversionSupportedFor(Class<?> source, Class<?> target) {
        source = toPrimitiveWrapperClass(source);
        target = toPrimitiveWrapperClass(target);
        if (factory.containsKey(pair(source, target))) {
            return true;
        }
        return getInheritedConverter(source, target) != null;
    }

    /**
     * @return Map<Class, Set < Class>> which contains all supported conversions. The key of the Map is a source class,
     * and the Set contains all the target types (classes) that the source can be converted to.
     */
    public Map<Class<?>, Set<Class<?>>> allSupportedConversions() {
        Map<Class<?>, Set<Class<?>>> toFrom = new TreeMap<>((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

        for (Map.Entry<Class<?>, Class<?>> pairs : factory.keySet()) {
            toFrom.computeIfAbsent(pairs.getKey(), k -> new TreeSet<>((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))).add(pairs.getValue());
        }
        return toFrom;
    }

    /**
     * @return Map<String, Set < String>> which contains all supported conversions. The key of the Map is a source class
     * name, and the Set contains all the target class names that the source can be converted to.
     */
    public Map<String, Set<String>> getSupportedConversions() {
        Map<String, Set<String>> toFrom = new TreeMap<>(String::compareToIgnoreCase);

        for (Map.Entry<Class<?>, Class<?>> pairs : factory.keySet()) {
            toFrom.computeIfAbsent(getShortName(pairs.getKey()), k -> new TreeSet<>(String::compareToIgnoreCase)).add(getShortName(pairs.getValue()));
        }
        return toFrom;
    }

    /**
     * Add a new conversion.
     *
     * @param source             Class to convert from.
     * @param target             Class to convert to.
     * @param conversionFunction Convert function that converts from the source type to the destination type.
     * @return prior conversion function if one existed.
     */
    public Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionFunction) {
        source = toPrimitiveWrapperClass(source);
        target = toPrimitiveWrapperClass(target);
        return factory.put(pair(source, target), conversionFunction);
    }

    public static long localDateToMillis(LocalDate localDate, ZoneId zoneId) {
        return localDate.atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    public static long localDateTimeToMillis(LocalDateTime localDateTime, ZoneId zoneId) {
        return localDateTime.atZone(zoneId).toInstant().toEpochMilli();
    }

    public static long zonedDateTimeToMillis(ZonedDateTime zonedDateTime) {
        return zonedDateTime.toInstant().toEpochMilli();
    }

    /**
     * Given a primitive class, return the Wrapper class equivalent.
     */
    private static Class<?> toPrimitiveWrapperClass(Class<?> primitiveClass) {
        if (!primitiveClass.isPrimitive()) {
            return primitiveClass;
        }

        Class<?> c = primitiveToWrapper.get(primitiveClass);

        if (c == null) {
            throw new IllegalArgumentException("Passed in class: " + primitiveClass + " is not a primitive class");
        }

        return c;
    }

    private static <T> T identity(T one, Converter converter, ConverterOptions options) {
        return one;
    }

    private static String toString(Object one, Converter converter, ConverterOptions options) {
        return one.toString();
    }


}
