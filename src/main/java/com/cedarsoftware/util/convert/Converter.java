package com.cedarsoftware.util.convert;

import java.io.Serializable;
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
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Instance conversion utility.  Convert from primitive to other primitives, plus support for Number, Date,
 * TimeStamp, SQL Date, LocalDate, LocalDateTime, ZonedDateTime, Calendar, Big*, Atomic*, Class, UUID,
 * String, ... Additional conversions can be added by specifying source class, destination class, and
 * a lambda function that performs the conversion.<br>
 * <br>
 * Currently, there are nearly 500 built-in conversions.  Use the getSupportedConversions() API to see all
 * source to target conversions.<br>
 * <br>
 * The main API is convert(value, class). if null passed in, null is returned for most types, which allows "tri-state"
 * Boolean, for example, however, for primitive types, it chooses zero for the numeric ones, `false` for boolean,
 * and 0 for char.<br>
 * <br>
 * A Map can be converted to almost all JDL "data" classes.  For example, UUID can be converted to/from a Map.
 * It is expected for the Map to have certain keys ("mostSigBits", "leastSigBits").  For the older Java Date/Time
 * related classes, it expects "time" or "nanos", and for all others, a Map as the source, the "value" key will be
 * used to source the value for the conversion.<br>
 * <br>
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

public final class Converter {
    static final String VALUE = "_v";

    private final Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> factory;
    private final ConverterOptions options;

    private static final Map<Class<?>, Set<ClassLevel>> cacheParentTypes = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Class<?>> primitiveToWrapper = new HashMap<>(20, .8f);
    private static final Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> DEFAULT_FACTORY = new ConcurrentHashMap<>(500, .8f);

    // Create a Map.Entry (pair) of source class to target class.
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
        DEFAULT_FACTORY.put(pair(Void.class, byte.class), NumberConversions::toByteZero);
        DEFAULT_FACTORY.put(pair(Void.class, Byte.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Byte.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Short.class, Byte.class), NumberConversions::toByte);
        DEFAULT_FACTORY.put(pair(Integer.class, Byte.class), NumberConversions::toByte);
        DEFAULT_FACTORY.put(pair(Long.class, Byte.class), NumberConversions::toByte);
        DEFAULT_FACTORY.put(pair(Float.class, Byte.class), NumberConversions::toByte);
        DEFAULT_FACTORY.put(pair(Double.class, Byte.class), NumberConversions::toByte);
        DEFAULT_FACTORY.put(pair(Boolean.class, Byte.class), BooleanConversions::toByte);
        DEFAULT_FACTORY.put(pair(Character.class, Byte.class), CharacterConversions::toByte);
        DEFAULT_FACTORY.put(pair(Calendar.class, Byte.class), NumberConversions::toByte);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Byte.class), AtomicBooleanConversions::toByte);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Byte.class), NumberConversions::toByte);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Byte.class), NumberConversions::toByte);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Byte.class), NumberConversions::toByte);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Byte.class), NumberConversions::toByte);
        DEFAULT_FACTORY.put(pair(Number.class, Byte.class), NumberConversions::toByte);
        DEFAULT_FACTORY.put(pair(Map.class, Byte.class), MapConversions::toByte);
        DEFAULT_FACTORY.put(pair(String.class, Byte.class), StringConversions::toByte);

        // Short/short conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, short.class), NumberConversions::toShortZero);
        DEFAULT_FACTORY.put(pair(Void.class, Short.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Short.class), NumberConversions::toShort);
        DEFAULT_FACTORY.put(pair(Short.class, Short.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Integer.class, Short.class), NumberConversions::toShort);
        DEFAULT_FACTORY.put(pair(Long.class, Short.class), NumberConversions::toShort);
        DEFAULT_FACTORY.put(pair(Float.class, Short.class), NumberConversions::toShort);
        DEFAULT_FACTORY.put(pair(Double.class, Short.class), NumberConversions::toShort);
        DEFAULT_FACTORY.put(pair(Boolean.class, Short.class), BooleanConversions::toShort);
        DEFAULT_FACTORY.put(pair(Character.class, Short.class), CharacterConversions::toShort);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Short.class), AtomicBooleanConversions::toShort);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Short.class), NumberConversions::toShort);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Short.class), NumberConversions::toShort);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Short.class), NumberConversions::toShort);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Short.class), NumberConversions::toShort);
        DEFAULT_FACTORY.put(pair(Number.class, Short.class), NumberConversions::toShort);
        DEFAULT_FACTORY.put(pair(Map.class, Short.class), MapConversions::toShort);
        DEFAULT_FACTORY.put(pair(String.class, Short.class), StringConversions::toShort);

        // Integer/int conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, int.class), NumberConversions::toIntZero);
        DEFAULT_FACTORY.put(pair(Void.class, Integer.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Integer.class), NumberConversions::toInt);
        DEFAULT_FACTORY.put(pair(Short.class, Integer.class), NumberConversions::toInt);
        DEFAULT_FACTORY.put(pair(Integer.class, Integer.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Long.class, Integer.class), NumberConversions::toInt);
        DEFAULT_FACTORY.put(pair(Float.class, Integer.class), NumberConversions::toInt);
        DEFAULT_FACTORY.put(pair(Double.class, Integer.class), NumberConversions::toInt);
        DEFAULT_FACTORY.put(pair(Boolean.class, Integer.class), BooleanConversions::toInteger);
        DEFAULT_FACTORY.put(pair(Character.class, Integer.class), CharacterConversions::toInt);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Integer.class), AtomicBooleanConversions::toInteger);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Integer.class), NumberConversions::toInt);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Integer.class), NumberConversions::toInt);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Integer.class), NumberConversions::toInt);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Integer.class), NumberConversions::toInt);
        DEFAULT_FACTORY.put(pair(Number.class, Integer.class), NumberConversions::toInt);
        DEFAULT_FACTORY.put(pair(Map.class, Integer.class), MapConversions::toInt);
        DEFAULT_FACTORY.put(pair(String.class, Integer.class), StringConversions::toInt);

        // Long/long conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, long.class), NumberConversions::toLongZero);
        DEFAULT_FACTORY.put(pair(Void.class, Long.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Long.class), NumberConversions::toLong);
        DEFAULT_FACTORY.put(pair(Short.class, Long.class), NumberConversions::toLong);
        DEFAULT_FACTORY.put(pair(Integer.class, Long.class), NumberConversions::toLong);
        DEFAULT_FACTORY.put(pair(Long.class, Long.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Float.class, Long.class), NumberConversions::toLong);
        DEFAULT_FACTORY.put(pair(Double.class, Long.class), NumberConversions::toLong);
        DEFAULT_FACTORY.put(pair(Boolean.class, Long.class), BooleanConversions::toLong);
        DEFAULT_FACTORY.put(pair(Character.class, Long.class), CharacterConversions::toLong);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Long.class), AtomicBooleanConversions::toLong);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Long.class), NumberConversions::toLong);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Long.class), NumberConversions::toLong);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Long.class), NumberConversions::toLong);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Long.class), NumberConversions::toLong);
        DEFAULT_FACTORY.put(pair(Date.class, Long.class), DateConversions::toLong);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Long.class), DateConversions::toLong);
        DEFAULT_FACTORY.put(pair(Timestamp.class, Long.class), DateConversions::toLong);
        DEFAULT_FACTORY.put(pair(Instant.class, Long.class), InstantConversions::toLong);
        DEFAULT_FACTORY.put(pair(LocalDate.class, Long.class), LocalDateConversions::toLong);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Long.class), LocalDateTimeConversions::toLong);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Long.class), ZonedDateTimeConversions::toLong);
        DEFAULT_FACTORY.put(pair(Calendar.class, Long.class), CalendarConversions::toLong);
        DEFAULT_FACTORY.put(pair(Number.class, Long.class), NumberConversions::toLong);
        DEFAULT_FACTORY.put(pair(Map.class, Long.class), MapConversions::toLong);
        DEFAULT_FACTORY.put(pair(String.class, Long.class), StringConversions::toLong);

        // Float/float conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, float.class), NumberConversions::toFloatZero);
        DEFAULT_FACTORY.put(pair(Void.class, Float.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Float.class), NumberConversions::toFloat);
        DEFAULT_FACTORY.put(pair(Short.class, Float.class), NumberConversions::toFloat);
        DEFAULT_FACTORY.put(pair(Integer.class, Float.class), NumberConversions::toFloat);
        DEFAULT_FACTORY.put(pair(Long.class, Float.class), NumberConversions::toFloat);
        DEFAULT_FACTORY.put(pair(Float.class, Float.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Double.class, Float.class), NumberConversions::toFloat);
        DEFAULT_FACTORY.put(pair(Boolean.class, Float.class), BooleanConversions::toFloat);
        DEFAULT_FACTORY.put(pair(Character.class, Float.class), CharacterConversions::toFloat);
        DEFAULT_FACTORY.put(pair(Instant.class, Float.class), InstantConversions::toFloat);
        DEFAULT_FACTORY.put(pair(LocalDate.class, Float.class), LocalDateConversions::toFloat);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Float.class), AtomicBooleanConversions::toFloat);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Float.class), NumberConversions::toFloat);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Float.class), NumberConversions::toFloat);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Float.class), NumberConversions::toFloat);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Float.class), NumberConversions::toFloat);
        DEFAULT_FACTORY.put(pair(Number.class, Float.class), NumberConversions::toFloat);
        DEFAULT_FACTORY.put(pair(Map.class, Float.class), MapConversions::toFloat);
        DEFAULT_FACTORY.put(pair(String.class, Float.class), StringConversions::toFloat);

        // Double/double conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, double.class), NumberConversions::toDoubleZero);
        DEFAULT_FACTORY.put(pair(Void.class, Double.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Double.class), NumberConversions::toDouble);
        DEFAULT_FACTORY.put(pair(Short.class, Double.class), NumberConversions::toDouble);
        DEFAULT_FACTORY.put(pair(Integer.class, Double.class), NumberConversions::toDouble);
        DEFAULT_FACTORY.put(pair(Long.class, Double.class), NumberConversions::toDouble);
        DEFAULT_FACTORY.put(pair(Float.class, Double.class), NumberConversions::toDouble);
        DEFAULT_FACTORY.put(pair(Double.class, Double.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Boolean.class, Double.class), BooleanConversions::toDouble);
        DEFAULT_FACTORY.put(pair(Character.class, Double.class), CharacterConversions::toDouble);
        DEFAULT_FACTORY.put(pair(Instant.class, Double.class), InstantConversions::toDouble);
        DEFAULT_FACTORY.put(pair(LocalDate.class, Double.class), LocalDateConversions::toDouble);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Double.class), LocalDateTimeConversions::toLong);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Double.class), ZonedDateTimeConversions::toLong);
        DEFAULT_FACTORY.put(pair(Date.class, Double.class), DateConversions::toLong);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Double.class), DateConversions::toLong);
        DEFAULT_FACTORY.put(pair(Timestamp.class, Double.class), DateConversions::toLong);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Double.class), AtomicBooleanConversions::toDouble);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Double.class), NumberConversions::toDouble);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Double.class), NumberConversions::toDouble);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Double.class), NumberConversions::toDouble);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Double.class), NumberConversions::toDouble);
        DEFAULT_FACTORY.put(pair(Calendar.class, Double.class), CalendarConversions::toDouble);
        DEFAULT_FACTORY.put(pair(Number.class, Double.class), NumberConversions::toDouble);
        DEFAULT_FACTORY.put(pair(Map.class, Double.class), MapConversions::toDouble);
        DEFAULT_FACTORY.put(pair(String.class, Double.class), StringConversions::toDouble);

        // Boolean/boolean conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, boolean.class), VoidConversions::toBoolean);
        DEFAULT_FACTORY.put(pair(Void.class, Boolean.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(Short.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(Integer.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(Long.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(Float.class, Boolean.class), NumberConversions::isFloatTypeNotZero);
        DEFAULT_FACTORY.put(pair(Double.class, Boolean.class), NumberConversions::isFloatTypeNotZero);
        DEFAULT_FACTORY.put(pair(Boolean.class, Boolean.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Character.class, Boolean.class), CharacterConversions::toBoolean);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Boolean.class), AtomicBooleanConversions::toBoolean);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Boolean.class), NumberConversions::isBigIntegerNotZero);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Boolean.class), NumberConversions::isBigDecimalNotZero);
        DEFAULT_FACTORY.put(pair(Number.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        DEFAULT_FACTORY.put(pair(Map.class, Boolean.class), MapConversions::toBoolean);
        DEFAULT_FACTORY.put(pair(String.class, Boolean.class), StringConversions::toBoolean);

        // Character/chat conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, char.class), VoidConversions::toChar);
        DEFAULT_FACTORY.put(pair(Void.class, Character.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Character.class), NumberConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(Short.class, Character.class), NumberConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(Integer.class, Character.class), NumberConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(Long.class, Character.class), NumberConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(Float.class, Character.class), NumberConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(Double.class, Character.class), NumberConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(Boolean.class, Character.class), BooleanConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(Character.class, Character.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Character.class), AtomicBooleanConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Character.class), NumberConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Character.class), NumberConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Character.class), NumberConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Character.class), NumberConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(Number.class, Character.class), NumberConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(Map.class, Character.class), MapConversions::toCharacter);
        DEFAULT_FACTORY.put(pair(String.class, Character.class), StringConversions::toCharacter);

        // BigInteger versions supported
        DEFAULT_FACTORY.put(pair(Void.class, BigInteger.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        DEFAULT_FACTORY.put(pair(Short.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        DEFAULT_FACTORY.put(pair(Integer.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        DEFAULT_FACTORY.put(pair(Long.class, BigInteger.class), NumberConversions::integerTypeToBigInteger);
        DEFAULT_FACTORY.put(pair(Float.class, BigInteger.class), NumberConversions::floatingPointToBigInteger);
        DEFAULT_FACTORY.put(pair(Double.class, BigInteger.class), NumberConversions::floatingPointToBigInteger);
        DEFAULT_FACTORY.put(pair(Boolean.class, BigInteger.class), BooleanConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(Character.class, BigInteger.class), CharacterConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(BigInteger.class, BigInteger.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, BigInteger.class), NumberConversions::bigDecimalToBigInteger);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, BigInteger.class), AtomicBooleanConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        DEFAULT_FACTORY.put(pair(Date.class, BigInteger.class), DateConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, BigInteger.class), DateConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(Timestamp.class, BigInteger.class), DateConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(Instant.class, BigInteger.class), InstantConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(LocalDate.class, BigInteger.class), LocalDateConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, BigInteger.class), LocalDateTimeConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, BigInteger.class), ZonedDateTimeConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(UUID.class, BigInteger.class), UUIDConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(Calendar.class, BigInteger.class), CalendarConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(Number.class, BigInteger.class), NumberConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(Map.class, BigInteger.class), MapConversions::toBigInteger);
        DEFAULT_FACTORY.put(pair(String.class, BigInteger.class), StringConversions::toBigInteger);

        // BigDecimal conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, BigDecimal.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        DEFAULT_FACTORY.put(pair(Short.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        DEFAULT_FACTORY.put(pair(Integer.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        DEFAULT_FACTORY.put(pair(Long.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        DEFAULT_FACTORY.put(pair(Float.class, BigDecimal.class), NumberConversions::floatingPointToBigDecimal);
        DEFAULT_FACTORY.put(pair(Double.class, BigDecimal.class), NumberConversions::floatingPointToBigDecimal);
        DEFAULT_FACTORY.put(pair(Boolean.class, BigDecimal.class), BooleanConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(Character.class, BigDecimal.class), CharacterConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, BigDecimal.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(BigInteger.class, BigDecimal.class), NumberConversions::bigIntegerToBigDecimal);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, BigDecimal.class), AtomicBooleanConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        DEFAULT_FACTORY.put(pair(Date.class, BigDecimal.class), DateConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, BigDecimal.class), DateConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(Timestamp.class, BigDecimal.class), DateConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(Instant.class, BigDecimal.class), InstantConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(LocalDate.class, BigDecimal.class), LocalDateConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, BigDecimal.class), LocalDateTimeConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, BigDecimal.class), ZonedDateTimeConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(UUID.class, BigDecimal.class), UUIDConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(Calendar.class, BigDecimal.class), CalendarConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(Number.class, BigDecimal.class), NumberConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(Map.class, BigDecimal.class), MapConversions::toBigDecimal);
        DEFAULT_FACTORY.put(pair(String.class, BigDecimal.class), StringConversions::toBigDecimal);

        // AtomicBoolean conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, AtomicBoolean.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(Short.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(Integer.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(Long.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(Float.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(Double.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(Boolean.class, AtomicBoolean.class), BooleanConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(Character.class, AtomicBoolean.class), CharacterConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(BigInteger.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, AtomicBoolean.class), AtomicBooleanConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(Number.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(Map.class, AtomicBoolean.class), MapConversions::toAtomicBoolean);
        DEFAULT_FACTORY.put(pair(String.class, AtomicBoolean.class), StringConversions::toAtomicBoolean);

        // AtomicInteger conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, AtomicInteger.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(Short.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(Integer.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(Long.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(Float.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(Double.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(Boolean.class, AtomicInteger.class), BooleanConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(Character.class, AtomicInteger.class), CharacterConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(BigInteger.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, AtomicInteger.class), AtomicBooleanConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(LocalDate.class, AtomicInteger.class), LocalDateConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Number.class, AtomicBoolean.class), NumberConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(Map.class, AtomicInteger.class), MapConversions::toAtomicInteger);
        DEFAULT_FACTORY.put(pair(String.class, AtomicInteger.class), StringConversions::toAtomicInteger);

        // AtomicLong conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, AtomicLong.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, AtomicLong.class), NumberConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Short.class, AtomicLong.class), NumberConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Integer.class, AtomicLong.class), NumberConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Long.class, AtomicLong.class), NumberConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Float.class, AtomicLong.class), NumberConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Double.class, AtomicLong.class), NumberConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Boolean.class, AtomicLong.class), BooleanConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Character.class, AtomicLong.class), CharacterConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(BigInteger.class, AtomicLong.class), NumberConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, AtomicLong.class), NumberConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, AtomicLong.class), AtomicBooleanConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, AtomicLong.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, AtomicLong.class), NumberConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Date.class, AtomicLong.class), DateConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, AtomicLong.class), DateConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Timestamp.class, AtomicLong.class), DateConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Instant.class, AtomicLong.class), InstantConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(LocalDate.class, AtomicLong.class), LocalDateConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, AtomicLong.class), LocalDateTimeConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, AtomicLong.class), ZonedDateTimeConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Calendar.class, AtomicLong.class), CalendarConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Number.class, AtomicLong.class), NumberConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(Map.class, AtomicLong.class), MapConversions::toAtomicLong);
        DEFAULT_FACTORY.put(pair(String.class, AtomicLong.class), StringConversions::toAtomicLong);

        // Date conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Date.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, Date.class), NumberConversions::toDate);
        DEFAULT_FACTORY.put(pair(Double.class, Date.class), NumberConversions::toDate);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Date.class), NumberConversions::toDate);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Date.class), NumberConversions::toDate);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Date.class), NumberConversions::toDate);
        DEFAULT_FACTORY.put(pair(Date.class, Date.class), DateConversions::toDate);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Date.class), DateConversions::toDate);
        DEFAULT_FACTORY.put(pair(Timestamp.class, Date.class), DateConversions::toDate);
        DEFAULT_FACTORY.put(pair(Instant.class, Date.class), InstantConversions::toDate);
        DEFAULT_FACTORY.put(pair(LocalDate.class, Date.class), LocalDateConversions::toDate);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Date.class), LocalDateTimeConversions::toDate);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Date.class), ZonedDateTimeConversions::toDate);
        DEFAULT_FACTORY.put(pair(Calendar.class, Date.class), CalendarConversions::toDate);
        DEFAULT_FACTORY.put(pair(Number.class, Date.class), NumberConversions::toDate);
        DEFAULT_FACTORY.put(pair(Map.class, Date.class), MapConversions::toDate);
        DEFAULT_FACTORY.put(pair(String.class, Date.class), StringConversions::toDate);

        // java.sql.Date conversion supported
        DEFAULT_FACTORY.put(pair(Void.class, java.sql.Date.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, java.sql.Date.class), NumberConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(Double.class, java.sql.Date.class), NumberConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(BigInteger.class, java.sql.Date.class), NumberConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, java.sql.Date.class), NumberConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, java.sql.Date.class), NumberConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, java.sql.Date.class), DateConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(Date.class, java.sql.Date.class), DateConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(Timestamp.class, java.sql.Date.class), DateConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(Instant.class, java.sql.Date.class), InstantConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(LocalDate.class, java.sql.Date.class), LocalDateConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, java.sql.Date.class), LocalDateTimeConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, java.sql.Date.class), ZonedDateTimeConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(Calendar.class, java.sql.Date.class), CalendarConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(Number.class, java.sql.Date.class), NumberConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(Map.class, java.sql.Date.class), MapConversions::toSqlDate);
        DEFAULT_FACTORY.put(pair(String.class, java.sql.Date.class), StringConversions::toSqlDate);

        // Timestamp conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Timestamp.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, Timestamp.class), NumberConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(Double.class, Timestamp.class), NumberConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Timestamp.class), NumberConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Timestamp.class), NumberConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Timestamp.class), NumberConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(Timestamp.class, Timestamp.class), DateConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Timestamp.class), DateConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(Date.class, Timestamp.class), DateConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(Instant.class,Timestamp.class), InstantConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(LocalDate.class, Timestamp.class), LocalDateConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Timestamp.class), LocalDateTimeConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Timestamp.class), ZonedDateTimeConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(Calendar.class, Timestamp.class), CalendarConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(Number.class, Timestamp.class), NumberConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(Map.class, Timestamp.class), MapConversions::toTimestamp);
        DEFAULT_FACTORY.put(pair(String.class, Timestamp.class), StringConversions::toTimestamp);

        // Calendar conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Calendar.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, Calendar.class), NumberConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(Double.class, Calendar.class), NumberConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Calendar.class), NumberConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Calendar.class), NumberConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Calendar.class), NumberConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(Date.class, Calendar.class), DateConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Calendar.class), DateConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(Timestamp.class, Calendar.class), DateConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(Instant.class, Calendar.class), InstantConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(LocalDate.class, Calendar.class), LocalDateConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Calendar.class), LocalDateTimeConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Calendar.class), ZonedDateTimeConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(Calendar.class, Calendar.class), CalendarConversions::clone);
        DEFAULT_FACTORY.put(pair(Number.class, Calendar.class), NumberConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(Map.class, Calendar.class), MapConversions::toCalendar);
        DEFAULT_FACTORY.put(pair(String.class, Calendar.class), StringConversions::toCalendar);

        // LocalDate conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, LocalDate.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, LocalDate.class), NumberConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(Double.class, LocalDate.class), NumberConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(BigInteger.class, LocalDate.class), NumberConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, LocalDate.class), NumberConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, LocalDate.class), NumberConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, LocalDate.class), DateConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(Timestamp.class, LocalDate.class), DateConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(Date.class, LocalDate.class), DateConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(Instant.class, LocalDate.class), InstantConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(LocalDate.class, LocalDate.class), LocalDateConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, LocalDate.class), LocalDateTimeConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, LocalDate.class), ZonedDateTimeConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(Calendar.class, LocalDate.class), CalendarConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(Number.class, LocalDate.class), NumberConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(Map.class, LocalDate.class), MapConversions::toLocalDate);
        DEFAULT_FACTORY.put(pair(String.class, LocalDate.class), StringConversions::toLocalDate);

        // LocalDateTime conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, LocalDateTime.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(Double.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(BigInteger.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, LocalDateTime.class), DateConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(Timestamp.class, LocalDateTime.class), DateConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(Date.class, LocalDateTime.class), DateConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(Instant.class, LocalDateTime.class), InstantConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, LocalDateTime.class), LocalDateTimeConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(LocalDate.class, LocalDateTime.class), LocalDateConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, LocalDateTime.class), ZonedDateTimeConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(Calendar.class, LocalDateTime.class), CalendarConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(Number.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(Map.class, LocalDateTime.class), MapConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(String.class, LocalDateTime.class), StringConversions::toLocalDateTime);

        // LocalTime conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, LocalTime.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, LocalTime.class), NumberConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(Double.class, LocalTime.class), NumberConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(BigInteger.class, LocalTime.class), NumberConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, LocalTime.class), NumberConversions::toLocalDateTime);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, LocalTime.class), NumberConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, LocalTime.class), DateConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(Timestamp.class, LocalTime.class), DateConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(Date.class, LocalTime.class), DateConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(Instant.class, LocalTime.class), InstantConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, LocalTime.class), LocalDateTimeConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(LocalDate.class, LocalTime.class), LocalDateConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(LocalTime.class, LocalTime.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, LocalTime.class), ZonedDateTimeConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(Calendar.class, LocalTime.class), CalendarConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(Number.class, LocalTime.class), NumberConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(Map.class, LocalTime.class), MapConversions::toLocalTime);
        DEFAULT_FACTORY.put(pair(String.class, LocalTime.class), StringConversions::toLocalTime);
        
        // ZonedDateTime conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, ZonedDateTime.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Long.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(Double.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(BigInteger.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(Timestamp.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(Date.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(Instant.class, ZonedDateTime.class), InstantConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(LocalDate.class, ZonedDateTime.class), LocalDateConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, ZonedDateTime.class), LocalDateTimeConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, ZonedDateTime.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Calendar.class, ZonedDateTime.class), CalendarConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(Number.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(Map.class, ZonedDateTime.class), MapConversions::toZonedDateTime);
        DEFAULT_FACTORY.put(pair(String.class, ZonedDateTime.class), StringConversions::toZonedDateTime);

        // UUID conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, UUID.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(UUID.class, UUID.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(String.class, UUID.class), StringConversions::toUUID);
        DEFAULT_FACTORY.put(pair(BigInteger.class, UUID.class), NumberConversions::bigIntegerToUUID);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, UUID.class), NumberConversions::bigDecimalToUUID);
        DEFAULT_FACTORY.put(pair(Map.class, UUID.class), MapConversions::toUUID);

        // Class conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Class.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Class.class, Class.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Map.class, Class.class), MapConversions::toClass);
        DEFAULT_FACTORY.put(pair(String.class, Class.class), StringConversions::toClass);

        // String conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, String.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(Short.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(Integer.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(Long.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(Float.class, String.class), NumberConversions::floatToString);
        DEFAULT_FACTORY.put(pair(Double.class, String.class), NumberConversions::doubleToString);
        DEFAULT_FACTORY.put(pair(Boolean.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(Character.class, String.class), CharacterConversions::toString);
        DEFAULT_FACTORY.put(pair(BigInteger.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, String.class), NumberConversions::bigDecimalToString);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(Class.class, String.class), StringConversions::classToString);
        DEFAULT_FACTORY.put(pair(Date.class, String.class), DateConversions::dateToString);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, String.class), DateConversions::sqlDateToString);
        DEFAULT_FACTORY.put(pair(Timestamp.class, String.class), DateConversions::timestampToString);
        DEFAULT_FACTORY.put(pair(LocalDate.class, String.class), DateConversions::localDateToString);
        DEFAULT_FACTORY.put(pair(LocalTime.class, String.class), DateConversions::localTimeToString);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, String.class), DateConversions::localDateTimeToString);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, String.class), DateConversions::zonedDateTimeToString);
        DEFAULT_FACTORY.put(pair(UUID.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(Calendar.class, String.class), DateConversions::calendarToString);
        DEFAULT_FACTORY.put(pair(Number.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(Map.class, String.class), MapConversions::toString);
        DEFAULT_FACTORY.put(pair(Enum.class, String.class), StringConversions::enumToString);
        DEFAULT_FACTORY.put(pair(String.class, String.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Duration.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(Instant.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(LocalTime.class, String.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(MonthDay.class, String.class), StringConversions::toString);

        // Duration conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Duration.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Duration.class, Duration.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(String.class, Duration.class), StringConversions::toString);
        DEFAULT_FACTORY.put(pair(Map.class, Duration.class), MapConversions::toDuration);

        // Instant conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Instant.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Instant.class, Instant.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(Long.class, Instant.class), NumberConversions::toInstant);
        DEFAULT_FACTORY.put(pair(Double.class, Instant.class), NumberConversions::toInstant);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Instant.class), NumberConversions::toInstant);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Instant.class), NumberConversions::toInstant);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Instant.class), NumberConversions::toInstant);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Instant.class), DateConversions::toInstant);
        DEFAULT_FACTORY.put(pair(Timestamp.class, Instant.class), DateConversions::toInstant);
        DEFAULT_FACTORY.put(pair(Date.class, Instant.class), DateConversions::toInstant);
        DEFAULT_FACTORY.put(pair(LocalDate.class, Instant.class), LocalDateConversions::toInstant);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Instant.class), LocalDateTimeConversions::toInstant);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Instant.class), ZonedDateTimeConversions::toInstant);
        DEFAULT_FACTORY.put(pair(Calendar.class, Instant.class), CalendarConversions::toInstant);
        DEFAULT_FACTORY.put(pair(Number.class, Instant.class), NumberConversions::toInstant);
        DEFAULT_FACTORY.put(pair(String.class, Instant.class), StringConversions::toInstant);
        DEFAULT_FACTORY.put(pair(Map.class, Instant.class), MapConversions::toInstant);

//        java.time.OffsetDateTime = com.cedarsoftware.util.io.DEFAULT_FACTORY.OffsetDateTimeFactory
//        java.time.OffsetTime = com.cedarsoftware.util.io.DEFAULT_FACTORY.OffsetTimeFactory
//        java.time.Period = com.cedarsoftware.util.io.DEFAULT_FACTORY.PeriodFactory
//        java.time.Year = com.cedarsoftware.util.io.DEFAULT_FACTORY.YearFactory
//        java.time.YearMonth = com.cedarsoftware.util.io.DEFAULT_FACTORY.YearMonthFactory
//        java.time.ZoneId = com.cedarsoftware.util.io.DEFAULT_FACTORY.ZoneIdFactory
//        java.time.ZoneOffset = com.cedarsoftware.util.io.DEFAULT_FACTORY.ZoneOffsetFactory
//        java.time.ZoneRegion = com.cedarsoftware.util.io.DEFAULT_FACTORY.ZoneIdFactory

        // MonthDay conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, MonthDay.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(MonthDay.class, MonthDay.class), Converter::identity);
        DEFAULT_FACTORY.put(pair(String.class, MonthDay.class), StringConversions::toMonthDay);
        DEFAULT_FACTORY.put(pair(Map.class, MonthDay.class), MapConversions::toMonthDay);

        // Map conversions supported
        DEFAULT_FACTORY.put(pair(Void.class, Map.class), VoidConversions::toNull);
        DEFAULT_FACTORY.put(pair(Byte.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Short.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Integer.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Long.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Float.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Double.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Boolean.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Character.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(BigInteger.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(BigDecimal.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(AtomicBoolean.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(AtomicInteger.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(AtomicLong.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Date.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(java.sql.Date.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Timestamp.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(LocalDate.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(LocalDateTime.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(ZonedDateTime.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Duration.class, Map.class), (from, converter, options) -> {
            long sec = ((Duration) from).getSeconds();
            long nanos = ((Duration) from).getNano();
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("seconds", sec);
            target.put("nanos", nanos);
            return target;
        });
        DEFAULT_FACTORY.put(pair(Instant.class, Map.class), (from, converter, options) -> {
            long sec = ((Instant) from).getEpochSecond();
            long nanos = ((Instant) from).getNano();
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("seconds", sec);
            target.put("nanos", nanos);
            return target;
        });
        DEFAULT_FACTORY.put(pair(LocalTime.class, Map.class), (from, converter, options) -> {
            LocalTime localTime = (LocalTime) from;
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
        DEFAULT_FACTORY.put(pair(MonthDay.class, Map.class), (from, converter, options) -> {
            MonthDay monthDay = (MonthDay) from;
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("day", monthDay.getDayOfMonth());
            target.put("month", monthDay.getMonthValue());
            return target;
        });
        DEFAULT_FACTORY.put(pair(Class.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(UUID.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Calendar.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Number.class, Map.class), MapConversions::initMap);
        DEFAULT_FACTORY.put(pair(Map.class, Map.class), (from, converter, options) -> {
            Map<?, ?> source = (Map<?, ?>) from;
            Map<?, ?> copy = new LinkedHashMap<>(source);
            return copy;
        });
        DEFAULT_FACTORY.put(pair(Enum.class, Map.class), MapConversions::initMap);
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
     * @param from A value used to create the targetType, even though it may
     *                     not (most likely will not) be the same data type as the targetType
     * @param toType       Class which indicates the targeted (final) data type.
     *                     Please note that in addition to the 8 Java primitives, the targeted class
     *                     can also be Date.class, String.class, BigInteger.class, BigDecimal.class, and
     *                     many other JDK classes, including Map.  For Map, often it will seek a 'value'
     *                     field, however, for some complex objects, like UUID, it will look for specific
     *                     fields within the Map to perform the conversion.
     * @return An instanceof targetType class, based upon the value passed in.
     */
    public <T> T convert(Object from, Class<T> toType) {
        return this.convert(from, toType, options);
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
     * @param from A value used to create the targetType, even though it may
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
    public <T> T convert(Object from, Class<T> toType, ConverterOptions options) {
        if (toType == null) {
            throw new IllegalArgumentException("toType cannot be null");
        }
        Class<?> sourceType;
        if (from == null) {
            // Do not promote primitive to primitive wrapper - allows for different 'from NULL' type for each.
            sourceType = Void.class;
        } else {
            // Promote primitive to primitive wrapper so we don't have to define so many duplicates in the factory map.
            sourceType = from.getClass();
            if (toType.isPrimitive()) {
                toType = (Class<T>) toPrimitiveWrapperClass(toType);
            }
        }

        // Direct Mapping
        Convert<?> converter = factory.get(pair(sourceType, toType));
        if (converter != null) {
            return (T) converter.convert(from, this, options);
        }

        // Try inheritance
        converter = getInheritedConverter(sourceType, toType);
        if (converter != null) {
            // Fast lookup next time.
            if (!isDirectConversionSupportedFor(sourceType, toType)) {
                addConversion(sourceType, toType, converter);
            }
            return (T) converter.convert(from, this, options);
        }

        throw new IllegalArgumentException("Unsupported conversion, source type [" + name(from) + "] target type '" + getShortName(toType) + "'");
    }

    /**
     * Expected that source and target classes, if primitive, have already been shifted to primitive wrapper classes.
     */
    private <T> Convert<?> getInheritedConverter(Class<?> sourceType, Class<T> toType) {
        Set<ClassLevel> sourceTypes = new TreeSet<>(getSuperClassesAndInterfaces(sourceType));
        sourceTypes.add(new ClassLevel(sourceType, 0));
        Set<ClassLevel> targetTypes = new TreeSet<>(getSuperClassesAndInterfaces(toType));
        targetTypes.add(new ClassLevel(toType, 0));

        Class<?> sourceClass = sourceType;
        Class<?> targetClass = toType;

        for (ClassLevel toClassLevel : targetTypes) {
            sourceClass = null;
            targetClass = null;

            for (ClassLevel fromClassLevel : sourceTypes) {
                if (factory.containsKey(pair(fromClassLevel.clazz, toClassLevel.clazz))) {
                    sourceClass = fromClassLevel.clazz;
                    targetClass = toClassLevel.clazz;
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

    private static Set<ClassLevel> getSuperClassesAndInterfaces(Class<?> clazz) {
        Set<ClassLevel> parentTypes = cacheParentTypes.get(clazz);
        if (parentTypes != null) {
            return parentTypes;
        }
        parentTypes = new ConcurrentSkipListSet<>();
        addSuperClassesAndInterfaces(clazz, parentTypes, 1);
        cacheParentTypes.put(clazz, parentTypes);
        return parentTypes;
    }

    static class ClassLevel implements Comparable {
        private final Class<?> clazz;
        private final int level;

        ClassLevel(Class<?> c, int level) {
            clazz = c;
            this.level = level;
        }

        public int compareTo(Object o) {
            if (!(o instanceof ClassLevel)) {
                throw new IllegalArgumentException("Object must be of type ClassLevel");
            }
            ClassLevel other = (ClassLevel) o;

            // Primary sort key: level
            int levelComparison = Integer.compare(this.level, other.level);
            if (levelComparison != 0) {
                return levelComparison;
            }

            // Secondary sort key: clazz type (class vs interface)
            boolean thisIsInterface = this.clazz.isInterface();
            boolean otherIsInterface = other.clazz.isInterface();
            if (thisIsInterface != otherIsInterface) {
                return thisIsInterface ? 1 : -1;
            }

            // Tertiary sort key: class name
            return this.clazz.getName().compareTo(other.clazz.getName());
        }
    }

    private static void addSuperClassesAndInterfaces(Class<?> clazz, Set<ClassLevel> result, int level) {
        // Add all superinterfaces
        for (Class<?> iface : clazz.getInterfaces()) {
            // Performance speed up, skip interfaces that are too general
            if (iface != Serializable.class && iface != Cloneable.class && iface != Comparable.class) {
                result.add(new ClassLevel(iface, level));
                addSuperClassesAndInterfaces(iface, result, level + 1);
            }
        }

        // Add superclass
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            result.add(new ClassLevel(superClass, level));
            addSuperClassesAndInterfaces(superClass, result, level + 1);
        }
    }

    static String getShortName(Class<?> type) {
        return java.sql.Date.class.equals(type) ? type.getName() : type.getSimpleName();
    }

    static private String name(Object from) {
        if (from == null) {
            return "null";
        }
        return getShortName(from.getClass()) + " (" + from + ")";
    }
    
    /**
     * Check to see if a direct-conversion from type to another type is supported.
     *
     * @param source Class of source type.
     * @param target Class of target type.
     * @return boolean true if the Converter converts from the source type to the destination type, false otherwise.
     */
    boolean isDirectConversionSupportedFor(Class<?> source, Class<?> target) {
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
}