package com.cedarsoftware.util.convert;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
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
import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
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

import com.cedarsoftware.util.ClassUtilities;

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
    public static final Convert<?> UNSUPPORTED = Converter::unsupported;
    static final String VALUE = "_v";

    private final Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> factory;
    private final ConverterOptions options;

    private static final Map<Class<?>, Set<ClassLevel>> cacheParentTypes = new ConcurrentHashMap<>();
    private static final Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> CONVERSION_DB = new ConcurrentHashMap<>(500, .8f);

    // Create a Map.Entry (pair) of source class to target class.
    static Map.Entry<Class<?>, Class<?>> pair(Class<?> source, Class<?> target) {
        return new AbstractMap.SimpleImmutableEntry<>(source, target);
    }

    static {
        buildFactoryConversions();
    }

    public ConverterOptions getOptions() {
        return options;
    }

    private static void buildFactoryConversions() {
        // toByte
        CONVERSION_DB.put(pair(Void.class, byte.class), NumberConversions::toByteZero);
        CONVERSION_DB.put(pair(Void.class, Byte.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Byte.class), Converter::identity);
        CONVERSION_DB.put(pair(Short.class, Byte.class), NumberConversions::toByte);
        CONVERSION_DB.put(pair(Integer.class, Byte.class), NumberConversions::toByte);
        CONVERSION_DB.put(pair(Long.class, Byte.class), NumberConversions::toByte);
        CONVERSION_DB.put(pair(Float.class, Byte.class), NumberConversions::toByte);
        CONVERSION_DB.put(pair(Double.class, Byte.class), NumberConversions::toByte);
        CONVERSION_DB.put(pair(Boolean.class, Byte.class), BooleanConversions::toByte);
        CONVERSION_DB.put(pair(Character.class, Byte.class), CharacterConversions::toByte);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Byte.class), AtomicBooleanConversions::toByte);
        CONVERSION_DB.put(pair(AtomicInteger.class, Byte.class), NumberConversions::toByte);
        CONVERSION_DB.put(pair(AtomicLong.class, Byte.class), NumberConversions::toByte);
        CONVERSION_DB.put(pair(BigInteger.class, Byte.class), NumberConversions::toByte);
        CONVERSION_DB.put(pair(BigDecimal.class, Byte.class), NumberConversions::toByte);
        CONVERSION_DB.put(pair(Number.class, Byte.class), NumberConversions::toByte);
        CONVERSION_DB.put(pair(Map.class, Byte.class), MapConversions::toByte);
        CONVERSION_DB.put(pair(String.class, Byte.class), StringConversions::toByte);

        // toShort
        CONVERSION_DB.put(pair(Void.class, short.class), NumberConversions::toShortZero);
        CONVERSION_DB.put(pair(Void.class, Short.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(Short.class, Short.class), Converter::identity);
        CONVERSION_DB.put(pair(Integer.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(Long.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(Float.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(Double.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(Boolean.class, Short.class), BooleanConversions::toShort);
        CONVERSION_DB.put(pair(Character.class, Short.class), CharacterConversions::toShort);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Short.class), AtomicBooleanConversions::toShort);
        CONVERSION_DB.put(pair(AtomicInteger.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(AtomicLong.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(BigInteger.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(BigDecimal.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(Number.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(Map.class, Short.class), MapConversions::toShort);
        CONVERSION_DB.put(pair(String.class, Short.class), StringConversions::toShort);
        CONVERSION_DB.put(pair(Year.class, Short.class), YearConversions::toShort);

        // toInteger
        CONVERSION_DB.put(pair(Void.class, int.class), NumberConversions::toIntZero);
        CONVERSION_DB.put(pair(Void.class, Integer.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Short.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Integer.class, Integer.class), Converter::identity);
        CONVERSION_DB.put(pair(Long.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Float.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Double.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Boolean.class, Integer.class), BooleanConversions::toInteger);
        CONVERSION_DB.put(pair(Character.class, Integer.class), CharacterConversions::toInt);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Integer.class), AtomicBooleanConversions::toInteger);
        CONVERSION_DB.put(pair(AtomicInteger.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(AtomicLong.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(BigInteger.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(BigDecimal.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Number.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Map.class, Integer.class), MapConversions::toInt);
        CONVERSION_DB.put(pair(String.class, Integer.class), StringConversions::toInt);
        CONVERSION_DB.put(pair(Year.class, Integer.class), YearConversions::toInt);

        // toLong
        CONVERSION_DB.put(pair(Void.class, long.class), NumberConversions::toLongZero);
        CONVERSION_DB.put(pair(Void.class, Long.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Short.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Integer.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Long.class, Long.class), Converter::identity);
        CONVERSION_DB.put(pair(Float.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Double.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Boolean.class, Long.class), BooleanConversions::toLong);
        CONVERSION_DB.put(pair(Character.class, Long.class), CharacterConversions::toLong);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Long.class), AtomicBooleanConversions::toLong);
        CONVERSION_DB.put(pair(AtomicInteger.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(AtomicLong.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(BigInteger.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(BigDecimal.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Date.class, Long.class), DateConversions::toLong);
        CONVERSION_DB.put(pair(java.sql.Date.class, Long.class), DateConversions::toLong);
        CONVERSION_DB.put(pair(Timestamp.class, Long.class), DateConversions::toLong);
        CONVERSION_DB.put(pair(Instant.class, Long.class), InstantConversions::toLong);
        CONVERSION_DB.put(pair(LocalDate.class, Long.class), LocalDateConversions::toLong);
        CONVERSION_DB.put(pair(LocalDateTime.class, Long.class), LocalDateTimeConversions::toLong);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Long.class), ZonedDateTimeConversions::toLong);
        CONVERSION_DB.put(pair(Calendar.class, Long.class), CalendarConversions::toLong);
        CONVERSION_DB.put(pair(Number.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Map.class, Long.class), MapConversions::toLong);
        CONVERSION_DB.put(pair(String.class, Long.class), StringConversions::toLong);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Long.class), OffsetDateTimeConversions::toLong);
        CONVERSION_DB.put(pair(Year.class, Long.class), YearConversions::toLong);

        // toFloat
        CONVERSION_DB.put(pair(Void.class, float.class), NumberConversions::toFloatZero);
        CONVERSION_DB.put(pair(Void.class, Float.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(Short.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(Integer.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(Long.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(Float.class, Float.class), Converter::identity);
        CONVERSION_DB.put(pair(Double.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(Boolean.class, Float.class), BooleanConversions::toFloat);
        CONVERSION_DB.put(pair(Character.class, Float.class), CharacterConversions::toFloat);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Float.class), AtomicBooleanConversions::toFloat);
        CONVERSION_DB.put(pair(AtomicInteger.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(AtomicLong.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(BigInteger.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(BigDecimal.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(Number.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(Map.class, Float.class), MapConversions::toFloat);
        CONVERSION_DB.put(pair(String.class, Float.class), StringConversions::toFloat);
        CONVERSION_DB.put(pair(Year.class, Float.class), YearConversions::toFloat);

        // toDouble
        CONVERSION_DB.put(pair(Void.class, double.class), NumberConversions::toDoubleZero);
        CONVERSION_DB.put(pair(Void.class, Double.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(Short.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(Integer.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(Long.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(Float.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(Double.class, Double.class), Converter::identity);
        CONVERSION_DB.put(pair(Boolean.class, Double.class), BooleanConversions::toDouble);
        CONVERSION_DB.put(pair(Character.class, Double.class), CharacterConversions::toDouble);
        CONVERSION_DB.put(pair(Instant.class, Double.class), InstantConversions::toDouble);
        CONVERSION_DB.put(pair(LocalDate.class, Double.class), LocalDateConversions::toDouble);
        CONVERSION_DB.put(pair(LocalDateTime.class, Double.class), LocalDateTimeConversions::toDouble);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Double.class), ZonedDateTimeConversions::toDouble);
        CONVERSION_DB.put(pair(Date.class, Double.class), DateConversions::toDouble);
        CONVERSION_DB.put(pair(java.sql.Date.class, Double.class), DateConversions::toDouble);
        CONVERSION_DB.put(pair(Timestamp.class, Double.class), DateConversions::toDouble);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Double.class), AtomicBooleanConversions::toDouble);
        CONVERSION_DB.put(pair(AtomicInteger.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(AtomicLong.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(BigInteger.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(BigDecimal.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(Calendar.class, Double.class), CalendarConversions::toDouble);
        CONVERSION_DB.put(pair(Number.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(Map.class, Double.class), MapConversions::toDouble);
        CONVERSION_DB.put(pair(String.class, Double.class), StringConversions::toDouble);
        CONVERSION_DB.put(pair(Year.class, Double.class), YearConversions::toDouble);

        // Boolean/boolean conversions supported
        CONVERSION_DB.put(pair(Void.class, boolean.class), VoidConversions::toBoolean);
        CONVERSION_DB.put(pair(Void.class, Boolean.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(pair(Short.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(pair(Integer.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(pair(Long.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(pair(Float.class, Boolean.class), NumberConversions::isFloatTypeNotZero);
        CONVERSION_DB.put(pair(Double.class, Boolean.class), NumberConversions::isFloatTypeNotZero);
        CONVERSION_DB.put(pair(Boolean.class, Boolean.class), Converter::identity);
        CONVERSION_DB.put(pair(Character.class, Boolean.class), CharacterConversions::toBoolean);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Boolean.class), AtomicBooleanConversions::toBoolean);
        CONVERSION_DB.put(pair(AtomicInteger.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(pair(AtomicLong.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(pair(BigInteger.class, Boolean.class), NumberConversions::isBigIntegerNotZero);
        CONVERSION_DB.put(pair(BigDecimal.class, Boolean.class), NumberConversions::isBigDecimalNotZero);
        CONVERSION_DB.put(pair(Number.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(pair(Map.class, Boolean.class), MapConversions::toBoolean);
        CONVERSION_DB.put(pair(String.class, Boolean.class), StringConversions::toBoolean);
        CONVERSION_DB.put(pair(Year.class, Boolean.class), YearConversions::toBoolean);

        // Character/char conversions supported
        CONVERSION_DB.put(pair(Void.class, char.class), VoidConversions::toChar);
        CONVERSION_DB.put(pair(Void.class, Character.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(Short.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(Integer.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(Long.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(Float.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(Double.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(Boolean.class, Character.class), BooleanConversions::toCharacter);
        CONVERSION_DB.put(pair(Character.class, Character.class), Converter::identity);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Character.class), AtomicBooleanConversions::toCharacter);
        CONVERSION_DB.put(pair(AtomicInteger.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(AtomicLong.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(BigInteger.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(BigDecimal.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(Number.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(Map.class, Character.class), MapConversions::toCharacter);
        CONVERSION_DB.put(pair(String.class, Character.class), StringConversions::toCharacter);

        // BigInteger versions supported
        CONVERSION_DB.put(pair(Void.class, BigInteger.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(Short.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(Integer.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(Long.class, BigInteger.class), NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(Float.class, BigInteger.class), NumberConversions::floatingPointToBigInteger);
        CONVERSION_DB.put(pair(Double.class, BigInteger.class), NumberConversions::floatingPointToBigInteger);
        CONVERSION_DB.put(pair(Boolean.class, BigInteger.class), BooleanConversions::toBigInteger);
        CONVERSION_DB.put(pair(Character.class, BigInteger.class), CharacterConversions::toBigInteger);
        CONVERSION_DB.put(pair(BigInteger.class, BigInteger.class), Converter::identity);
        CONVERSION_DB.put(pair(BigDecimal.class, BigInteger.class), NumberConversions::bigDecimalToBigInteger);
        CONVERSION_DB.put(pair(AtomicBoolean.class, BigInteger.class), AtomicBooleanConversions::toBigInteger);
        CONVERSION_DB.put(pair(AtomicInteger.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(AtomicLong.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(Date.class, BigInteger.class), DateConversions::toBigInteger);
        CONVERSION_DB.put(pair(java.sql.Date.class, BigInteger.class), DateConversions::toBigInteger);
        CONVERSION_DB.put(pair(Timestamp.class, BigInteger.class), DateConversions::toBigInteger);
        CONVERSION_DB.put(pair(Instant.class, BigInteger.class), InstantConversions::toBigInteger);
        CONVERSION_DB.put(pair(LocalDate.class, BigInteger.class), LocalDateConversions::toBigInteger);
        CONVERSION_DB.put(pair(LocalDateTime.class, BigInteger.class), LocalDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(ZonedDateTime.class, BigInteger.class), ZonedDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(UUID.class, BigInteger.class), UUIDConversions::toBigInteger);
        CONVERSION_DB.put(pair(Calendar.class, BigInteger.class), CalendarConversions::toBigInteger);
        CONVERSION_DB.put(pair(Number.class, BigInteger.class), NumberConversions::toBigInteger);
        CONVERSION_DB.put(pair(Map.class, BigInteger.class), MapConversions::toBigInteger);
        CONVERSION_DB.put(pair(String.class, BigInteger.class), StringConversions::toBigInteger);
        CONVERSION_DB.put(pair(OffsetDateTime.class, BigInteger.class), OffsetDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(Year.class, BigInteger.class), YearConversions::toBigInteger);

        // BigDecimal conversions supported
        CONVERSION_DB.put(pair(Void.class, BigDecimal.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(pair(Short.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(pair(Integer.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(pair(Long.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(pair(Float.class, BigDecimal.class), NumberConversions::floatingPointToBigDecimal);
        CONVERSION_DB.put(pair(Double.class, BigDecimal.class), NumberConversions::floatingPointToBigDecimal);
        CONVERSION_DB.put(pair(Boolean.class, BigDecimal.class), BooleanConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Character.class, BigDecimal.class), CharacterConversions::toBigDecimal);
        CONVERSION_DB.put(pair(BigDecimal.class, BigDecimal.class), Converter::identity);
        CONVERSION_DB.put(pair(BigInteger.class, BigDecimal.class), NumberConversions::bigIntegerToBigDecimal);
        CONVERSION_DB.put(pair(AtomicBoolean.class, BigDecimal.class), AtomicBooleanConversions::toBigDecimal);
        CONVERSION_DB.put(pair(AtomicInteger.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(pair(AtomicLong.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(pair(Date.class, BigDecimal.class), DateConversions::toBigDecimal);
        CONVERSION_DB.put(pair(java.sql.Date.class, BigDecimal.class), DateConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Timestamp.class, BigDecimal.class), DateConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Instant.class, BigDecimal.class), InstantConversions::toBigDecimal);
        CONVERSION_DB.put(pair(LocalDate.class, BigDecimal.class), LocalDateConversions::toBigDecimal);
        CONVERSION_DB.put(pair(LocalDateTime.class, BigDecimal.class), LocalDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(ZonedDateTime.class, BigDecimal.class), ZonedDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(UUID.class, BigDecimal.class), UUIDConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Calendar.class, BigDecimal.class), CalendarConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Number.class, BigDecimal.class), NumberConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Map.class, BigDecimal.class), MapConversions::toBigDecimal);
        CONVERSION_DB.put(pair(String.class, BigDecimal.class), StringConversions::toBigDecimal);
        CONVERSION_DB.put(pair(OffsetDateTime.class, BigDecimal.class), OffsetDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Year.class, BigDecimal.class), YearConversions::toBigDecimal);

        // AtomicBoolean conversions supported
        CONVERSION_DB.put(pair(Void.class, AtomicBoolean.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(Short.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(Integer.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(Long.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(Float.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(Double.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(Boolean.class, AtomicBoolean.class), BooleanConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(Character.class, AtomicBoolean.class), CharacterConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(BigInteger.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(BigDecimal.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(AtomicBoolean.class, AtomicBoolean.class), AtomicBooleanConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(AtomicInteger.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(AtomicLong.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(Number.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(Map.class, AtomicBoolean.class), MapConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(String.class, AtomicBoolean.class), StringConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(Year.class, AtomicBoolean.class), YearConversions::toAtomicBoolean);

        // AtomicInteger conversions supported
        CONVERSION_DB.put(pair(Void.class, AtomicInteger.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(Short.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(Integer.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(Long.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(Float.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(Double.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(Boolean.class, AtomicInteger.class), BooleanConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(Character.class, AtomicInteger.class), CharacterConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(BigInteger.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(BigDecimal.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(AtomicInteger.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(AtomicBoolean.class, AtomicInteger.class), AtomicBooleanConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(AtomicLong.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(LocalDate.class, AtomicInteger.class), LocalDateConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Number.class, AtomicBoolean.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(Map.class, AtomicInteger.class), MapConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(String.class, AtomicInteger.class), StringConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(Year.class, AtomicInteger.class), YearConversions::toAtomicInteger);

        // AtomicLong conversions supported
        CONVERSION_DB.put(pair(Void.class, AtomicLong.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, AtomicLong.class), NumberConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Short.class, AtomicLong.class), NumberConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Integer.class, AtomicLong.class), NumberConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Long.class, AtomicLong.class), NumberConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Float.class, AtomicLong.class), NumberConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Double.class, AtomicLong.class), NumberConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Boolean.class, AtomicLong.class), BooleanConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Character.class, AtomicLong.class), CharacterConversions::toAtomicLong);
        CONVERSION_DB.put(pair(BigInteger.class, AtomicLong.class), NumberConversions::toAtomicLong);
        CONVERSION_DB.put(pair(BigDecimal.class, AtomicLong.class), NumberConversions::toAtomicLong);
        CONVERSION_DB.put(pair(AtomicBoolean.class, AtomicLong.class), AtomicBooleanConversions::toAtomicLong);
        CONVERSION_DB.put(pair(AtomicLong.class, AtomicLong.class), Converter::identity);
        CONVERSION_DB.put(pair(AtomicInteger.class, AtomicLong.class), NumberConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Date.class, AtomicLong.class), DateConversions::toAtomicLong);
        CONVERSION_DB.put(pair(java.sql.Date.class, AtomicLong.class), DateConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Timestamp.class, AtomicLong.class), DateConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Instant.class, AtomicLong.class), InstantConversions::toAtomicLong);
        CONVERSION_DB.put(pair(LocalDate.class, AtomicLong.class), LocalDateConversions::toAtomicLong);
        CONVERSION_DB.put(pair(LocalDateTime.class, AtomicLong.class), LocalDateTimeConversions::toAtomicLong);
        CONVERSION_DB.put(pair(ZonedDateTime.class, AtomicLong.class), ZonedDateTimeConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Calendar.class, AtomicLong.class), CalendarConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Number.class, AtomicLong.class), NumberConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Map.class, AtomicLong.class), MapConversions::toAtomicLong);
        CONVERSION_DB.put(pair(String.class, AtomicLong.class), StringConversions::toAtomicLong);
        CONVERSION_DB.put(pair(OffsetDateTime.class, AtomicLong.class), OffsetDateTimeConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Year.class, AtomicLong.class), YearConversions::toAtomicLong);

        // Date conversions supported
        CONVERSION_DB.put(pair(Void.class, Date.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Date.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Short.class, Date.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Integer.class, Date.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Long.class, Date.class), NumberConversions::toDate);
        CONVERSION_DB.put(pair(Double.class, Date.class), NumberConversions::toDate);
        CONVERSION_DB.put(pair(BigInteger.class, Date.class), NumberConversions::toDate);
        CONVERSION_DB.put(pair(BigDecimal.class, Date.class), NumberConversions::toDate);
        CONVERSION_DB.put(pair(AtomicInteger.class, Date.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(AtomicLong.class, Date.class), NumberConversions::toDate);
        CONVERSION_DB.put(pair(Date.class, Date.class), DateConversions::toDate);
        CONVERSION_DB.put(pair(java.sql.Date.class, Date.class), DateConversions::toDate);
        CONVERSION_DB.put(pair(Timestamp.class, Date.class), DateConversions::toDate);
        CONVERSION_DB.put(pair(Instant.class, Date.class), InstantConversions::toDate);
        CONVERSION_DB.put(pair(LocalDate.class, Date.class), LocalDateConversions::toDate);
        CONVERSION_DB.put(pair(LocalDateTime.class, Date.class), LocalDateTimeConversions::toDate);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Date.class), ZonedDateTimeConversions::toDate);
        CONVERSION_DB.put(pair(Calendar.class, Date.class), CalendarConversions::toDate);
        CONVERSION_DB.put(pair(Number.class, Date.class), NumberConversions::toDate);
        CONVERSION_DB.put(pair(Map.class, Date.class), MapConversions::toDate);
        CONVERSION_DB.put(pair(String.class, Date.class), StringConversions::toDate);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Date.class), OffsetDateTimeConversions::toDate);

        // java.sql.Date conversion supported
        CONVERSION_DB.put(pair(Void.class, java.sql.Date.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, java.sql.Date.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Short.class, java.sql.Date.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Integer.class, java.sql.Date.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Long.class, java.sql.Date.class), NumberConversions::toSqlDate);
        CONVERSION_DB.put(pair(Double.class, java.sql.Date.class), NumberConversions::toSqlDate);
        CONVERSION_DB.put(pair(BigInteger.class, java.sql.Date.class), NumberConversions::toSqlDate);
        CONVERSION_DB.put(pair(BigDecimal.class, java.sql.Date.class), NumberConversions::toSqlDate);
        CONVERSION_DB.put(pair(AtomicInteger.class, java.sql.Date.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(AtomicLong.class, java.sql.Date.class), NumberConversions::toSqlDate);
        CONVERSION_DB.put(pair(java.sql.Date.class, java.sql.Date.class), DateConversions::toSqlDate);
        CONVERSION_DB.put(pair(Date.class, java.sql.Date.class), DateConversions::toSqlDate);
        CONVERSION_DB.put(pair(Timestamp.class, java.sql.Date.class), DateConversions::toSqlDate);
        CONVERSION_DB.put(pair(Instant.class, java.sql.Date.class), InstantConversions::toSqlDate);
        CONVERSION_DB.put(pair(LocalDate.class, java.sql.Date.class), LocalDateConversions::toSqlDate);
        CONVERSION_DB.put(pair(LocalDateTime.class, java.sql.Date.class), LocalDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(pair(ZonedDateTime.class, java.sql.Date.class), ZonedDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(pair(Calendar.class, java.sql.Date.class), CalendarConversions::toSqlDate);
        CONVERSION_DB.put(pair(Number.class, java.sql.Date.class), NumberConversions::toSqlDate);
        CONVERSION_DB.put(pair(Map.class, java.sql.Date.class), MapConversions::toSqlDate);
        CONVERSION_DB.put(pair(String.class, java.sql.Date.class), StringConversions::toSqlDate);
        CONVERSION_DB.put(pair(OffsetDateTime.class, java.sql.Date.class), OffsetDateTimeConversions::toSqlDate);

        // Timestamp conversions supported
        CONVERSION_DB.put(pair(Void.class, Timestamp.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Timestamp.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Short.class, Timestamp.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Integer.class, Timestamp.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Long.class, Timestamp.class), NumberConversions::toTimestamp);
        CONVERSION_DB.put(pair(Double.class, Timestamp.class), NumberConversions::toTimestamp);
        CONVERSION_DB.put(pair(BigInteger.class, Timestamp.class), NumberConversions::toTimestamp);
        CONVERSION_DB.put(pair(BigDecimal.class, Timestamp.class), NumberConversions::toTimestamp);
        CONVERSION_DB.put(pair(AtomicInteger.class, Timestamp.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(AtomicLong.class, Timestamp.class), NumberConversions::toTimestamp);
        CONVERSION_DB.put(pair(Timestamp.class, Timestamp.class), DateConversions::toTimestamp);
        CONVERSION_DB.put(pair(java.sql.Date.class, Timestamp.class), DateConversions::toTimestamp);
        CONVERSION_DB.put(pair(Date.class, Timestamp.class), DateConversions::toTimestamp);
        CONVERSION_DB.put(pair(Instant.class,Timestamp.class), InstantConversions::toTimestamp);
        CONVERSION_DB.put(pair(LocalDate.class, Timestamp.class), LocalDateConversions::toTimestamp);
        CONVERSION_DB.put(pair(LocalDateTime.class, Timestamp.class), LocalDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Timestamp.class), ZonedDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(pair(Calendar.class, Timestamp.class), CalendarConversions::toTimestamp);
        CONVERSION_DB.put(pair(Number.class, Timestamp.class), NumberConversions::toTimestamp);
        CONVERSION_DB.put(pair(Map.class, Timestamp.class), MapConversions::toTimestamp);
        CONVERSION_DB.put(pair(String.class, Timestamp.class), StringConversions::toTimestamp);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Timestamp.class), OffsetDateTimeConversions::toTimestamp);

        // Calendar conversions supported
        CONVERSION_DB.put(pair(Void.class, Calendar.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Calendar.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Short.class, Calendar.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Integer.class, Calendar.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Long.class, Calendar.class), NumberConversions::toCalendar);
        CONVERSION_DB.put(pair(Double.class, Calendar.class), NumberConversions::toCalendar);
        CONVERSION_DB.put(pair(BigInteger.class, Calendar.class), NumberConversions::toCalendar);
        CONVERSION_DB.put(pair(BigDecimal.class, Calendar.class), NumberConversions::toCalendar);
        CONVERSION_DB.put(pair(AtomicInteger.class, Calendar.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(AtomicLong.class, Calendar.class), NumberConversions::toCalendar);
        CONVERSION_DB.put(pair(Date.class, Calendar.class), DateConversions::toCalendar);
        CONVERSION_DB.put(pair(java.sql.Date.class, Calendar.class), DateConversions::toCalendar);
        CONVERSION_DB.put(pair(Timestamp.class, Calendar.class), DateConversions::toCalendar);
        CONVERSION_DB.put(pair(Instant.class, Calendar.class), InstantConversions::toCalendar);
        CONVERSION_DB.put(pair(LocalDate.class, Calendar.class), LocalDateConversions::toCalendar);
        CONVERSION_DB.put(pair(LocalDateTime.class, Calendar.class), LocalDateTimeConversions::toCalendar);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Calendar.class), ZonedDateTimeConversions::toCalendar);
        CONVERSION_DB.put(pair(Calendar.class, Calendar.class), CalendarConversions::clone);
        CONVERSION_DB.put(pair(Number.class, Calendar.class), NumberConversions::toCalendar);
        CONVERSION_DB.put(pair(Map.class, Calendar.class), MapConversions::toCalendar);
        CONVERSION_DB.put(pair(String.class, Calendar.class), StringConversions::toCalendar);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Calendar.class), OffsetDateTimeConversions::toCalendar);

        // LocalDate conversions supported
        CONVERSION_DB.put(pair(Void.class, LocalDate.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, LocalDate.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Short.class, LocalDate.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Integer.class, LocalDate.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Long.class, LocalDate.class), NumberConversions::toLocalDate);
        CONVERSION_DB.put(pair(Double.class, LocalDate.class), NumberConversions::toLocalDate);
        CONVERSION_DB.put(pair(BigInteger.class, LocalDate.class), NumberConversions::toLocalDate);
        CONVERSION_DB.put(pair(BigDecimal.class, LocalDate.class), NumberConversions::toLocalDate);
        CONVERSION_DB.put(pair(AtomicInteger.class, LocalDate.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(AtomicLong.class, LocalDate.class), NumberConversions::toLocalDate);
        CONVERSION_DB.put(pair(java.sql.Date.class, LocalDate.class), DateConversions::toLocalDate);
        CONVERSION_DB.put(pair(Timestamp.class, LocalDate.class), DateConversions::toLocalDate);
        CONVERSION_DB.put(pair(Date.class, LocalDate.class), DateConversions::toLocalDate);
        CONVERSION_DB.put(pair(Instant.class, LocalDate.class), InstantConversions::toLocalDate);
        CONVERSION_DB.put(pair(LocalDate.class, LocalDate.class), LocalDateConversions::toLocalDate);
        CONVERSION_DB.put(pair(LocalDateTime.class, LocalDate.class), LocalDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(pair(ZonedDateTime.class, LocalDate.class), ZonedDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(pair(Calendar.class, LocalDate.class), CalendarConversions::toLocalDate);
        CONVERSION_DB.put(pair(Number.class, LocalDate.class), NumberConversions::toLocalDate);
        CONVERSION_DB.put(pair(Map.class, LocalDate.class), MapConversions::toLocalDate);
        CONVERSION_DB.put(pair(String.class, LocalDate.class), StringConversions::toLocalDate);
        CONVERSION_DB.put(pair(OffsetDateTime.class, LocalDate.class), OffsetDateTimeConversions::toLocalDate);

        // LocalDateTime conversions supported
        CONVERSION_DB.put(pair(Void.class, LocalDateTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, LocalDateTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Short.class, LocalDateTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Integer.class, LocalDateTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Long.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Double.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(BigInteger.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(BigDecimal.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(AtomicInteger.class, LocalDateTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(AtomicLong.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(java.sql.Date.class, LocalDateTime.class), DateConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Timestamp.class, LocalDateTime.class), DateConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Date.class, LocalDateTime.class), DateConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Instant.class, LocalDateTime.class), InstantConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(LocalDateTime.class, LocalDateTime.class), LocalDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(LocalDate.class, LocalDateTime.class), LocalDateConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(ZonedDateTime.class, LocalDateTime.class), ZonedDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Calendar.class, LocalDateTime.class), CalendarConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Number.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Map.class, LocalDateTime.class), MapConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(String.class, LocalDateTime.class), StringConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(OffsetDateTime.class, LocalDateTime.class), OffsetDateTimeConversions::toLocalDateTime);

        // LocalTime conversions supported
        CONVERSION_DB.put(pair(Void.class, LocalTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, LocalTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Short.class, LocalTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Integer.class, LocalTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Long.class, LocalTime.class), NumberConversions::toLocalTime);
        CONVERSION_DB.put(pair(Double.class, LocalTime.class), NumberConversions::toLocalTime);
        CONVERSION_DB.put(pair(BigInteger.class, LocalTime.class), NumberConversions::toLocalTime);
        CONVERSION_DB.put(pair(BigDecimal.class, LocalTime.class), NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(AtomicInteger.class, LocalTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(AtomicLong.class, LocalTime.class), NumberConversions::toLocalTime);
        CONVERSION_DB.put(pair(java.sql.Date.class, LocalTime.class), DateConversions::toLocalTime);
        CONVERSION_DB.put(pair(Timestamp.class, LocalTime.class), DateConversions::toLocalTime);
        CONVERSION_DB.put(pair(Date.class, LocalTime.class), DateConversions::toLocalTime);
        CONVERSION_DB.put(pair(Instant.class, LocalTime.class), InstantConversions::toLocalTime);
        CONVERSION_DB.put(pair(LocalDateTime.class, LocalTime.class), LocalDateTimeConversions::toLocalTime);
        CONVERSION_DB.put(pair(LocalDate.class, LocalTime.class), LocalDateConversions::toLocalTime);
        CONVERSION_DB.put(pair(LocalTime.class, LocalTime.class), Converter::identity);
        CONVERSION_DB.put(pair(ZonedDateTime.class, LocalTime.class), ZonedDateTimeConversions::toLocalTime);
        CONVERSION_DB.put(pair(Calendar.class, LocalTime.class), CalendarConversions::toLocalTime);
        CONVERSION_DB.put(pair(Number.class, LocalTime.class), NumberConversions::toLocalTime);
        CONVERSION_DB.put(pair(Map.class, LocalTime.class), MapConversions::toLocalTime);
        CONVERSION_DB.put(pair(String.class, LocalTime.class), StringConversions::toLocalTime);
        CONVERSION_DB.put(pair(OffsetDateTime.class, LocalTime.class), OffsetDateTimeConversions::toLocalTime);

        // ZonedDateTime conversions supported
        CONVERSION_DB.put(pair(Void.class, ZonedDateTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, ZonedDateTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Short.class, ZonedDateTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Integer.class, ZonedDateTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Long.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Double.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(BigInteger.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(BigDecimal.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(AtomicInteger.class, ZonedDateTime.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(AtomicLong.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(java.sql.Date.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Timestamp.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Date.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Instant.class, ZonedDateTime.class), InstantConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(LocalDate.class, ZonedDateTime.class), LocalDateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(LocalDateTime.class, ZonedDateTime.class), LocalDateTimeConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(ZonedDateTime.class, ZonedDateTime.class), Converter::identity);
        CONVERSION_DB.put(pair(Calendar.class, ZonedDateTime.class), CalendarConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Number.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Map.class, ZonedDateTime.class), MapConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(String.class, ZonedDateTime.class), StringConversions::toZonedDateTime);

        // toOffsetDateTime
        CONVERSION_DB.put(pair(Void.class, OffsetDateTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(OffsetDateTime.class, OffsetDateTime.class), Converter::identity);
        CONVERSION_DB.put(pair(Map.class, OffsetDateTime.class), MapConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(String.class, OffsetDateTime.class), StringConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Long.class, OffsetDateTime.class), NumberConversions::toOffsetDateTime);

        // toOffsetTime
        CONVERSION_DB.put(pair(Void.class, OffsetTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(OffsetTime.class, OffsetTime.class), Converter::identity);
        CONVERSION_DB.put(pair(OffsetDateTime.class, OffsetTime.class), OffsetDateTimeConversions::toOffsetTime);
        CONVERSION_DB.put(pair(Map.class, OffsetTime.class), MapConversions::toOffsetTime);
        CONVERSION_DB.put(pair(String.class, OffsetTime.class), StringConversions::toOffsetTime);

        // UUID conversions supported
        CONVERSION_DB.put(pair(Void.class, UUID.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(UUID.class, UUID.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, UUID.class), StringConversions::toUUID);
        CONVERSION_DB.put(pair(BigInteger.class, UUID.class), NumberConversions::bigIntegerToUUID);
        CONVERSION_DB.put(pair(BigDecimal.class, UUID.class), NumberConversions::bigDecimalToUUID);
        CONVERSION_DB.put(pair(Map.class, UUID.class), MapConversions::toUUID);

        // Class conversions supported
        CONVERSION_DB.put(pair(Void.class, Class.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Class.class, Class.class), Converter::identity);
        CONVERSION_DB.put(pair(Map.class, Class.class), MapConversions::toClass);
        CONVERSION_DB.put(pair(String.class, Class.class), StringConversions::toClass);

        // Class conversions supported
        CONVERSION_DB.put(pair(Void.class, Locale.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Locale.class, Locale.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, Locale.class), StringConversions::toLocale);
        CONVERSION_DB.put(pair(Map.class, Locale.class), MapConversions::toLocale);

        // String conversions supported
        CONVERSION_DB.put(pair(Void.class, String.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(Short.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(Integer.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(Long.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(Float.class, String.class), NumberConversions::floatToString);
        CONVERSION_DB.put(pair(Double.class, String.class), NumberConversions::doubleToString);
        CONVERSION_DB.put(pair(Boolean.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(Character.class, String.class), CharacterConversions::toString);
        CONVERSION_DB.put(pair(BigInteger.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(BigDecimal.class, String.class), NumberConversions::bigDecimalToString);
        CONVERSION_DB.put(pair(AtomicBoolean.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(AtomicInteger.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(AtomicLong.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(byte[].class, String.class), ByteArrayConversions::toString);
        CONVERSION_DB.put(pair(char[].class, String.class), CharArrayConversions::toString);
        CONVERSION_DB.put(pair(Character[].class, String.class), CharacterArrayConversions::toString);
        CONVERSION_DB.put(pair(ByteBuffer.class, String.class), ByteBufferConversions::toString);
        CONVERSION_DB.put(pair(CharBuffer.class, String.class), CharBufferConversions::toString);
        CONVERSION_DB.put(pair(Class.class, String.class), ClassConversions::toString);
        CONVERSION_DB.put(pair(Date.class, String.class), DateConversions::dateToString);
        CONVERSION_DB.put(pair(java.sql.Date.class, String.class), DateConversions::sqlDateToString);
        CONVERSION_DB.put(pair(Timestamp.class, String.class), DateConversions::timestampToString);
        CONVERSION_DB.put(pair(LocalDate.class, String.class), LocalDateConversions::toString);
        CONVERSION_DB.put(pair(LocalTime.class, String.class), LocalTimeConversions::toString);
        CONVERSION_DB.put(pair(LocalDateTime.class, String.class), LocalDateTimeConversions::toString);
        CONVERSION_DB.put(pair(ZonedDateTime.class, String.class), ZonedDateTimeConversions::toString);
        CONVERSION_DB.put(pair(UUID.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(Calendar.class, String.class), CalendarConversions::toString);
        CONVERSION_DB.put(pair(Number.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(Map.class, String.class), MapConversions::toString);
        CONVERSION_DB.put(pair(Enum.class, String.class), StringConversions::enumToString);
        CONVERSION_DB.put(pair(String.class, String.class), Converter::identity);
        CONVERSION_DB.put(pair(Duration.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(Instant.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(LocalTime.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(MonthDay.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(YearMonth.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(Period.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(ZoneId.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(ZoneOffset.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(OffsetTime.class, String.class), OffsetTimeConversions::toString);
        CONVERSION_DB.put(pair(OffsetDateTime.class, String.class), OffsetDateTimeConversions::toString);
        CONVERSION_DB.put(pair(Year.class, String.class), YearConversions::toString);
        CONVERSION_DB.put(pair(Locale.class, String.class), LocaleConversions::toString);
        CONVERSION_DB.put(pair(URL.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(URI.class, String.class), StringConversions::toString);
        
        // Duration conversions supported
        CONVERSION_DB.put(pair(Void.class, Duration.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Duration.class, Duration.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, Duration.class), StringConversions::toDuration);
        CONVERSION_DB.put(pair(Map.class, Duration.class), MapConversions::toDuration);

        // Instant conversions supported
        CONVERSION_DB.put(pair(Void.class, Instant.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Instant.class, Instant.class), Converter::identity);
        CONVERSION_DB.put(pair(Byte.class, Instant.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Short.class, Instant.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Integer.class, Instant.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Long.class, Instant.class), NumberConversions::toInstant);
        CONVERSION_DB.put(pair(Double.class, Instant.class), NumberConversions::toInstant);
        CONVERSION_DB.put(pair(BigInteger.class, Instant.class), NumberConversions::toInstant);
        CONVERSION_DB.put(pair(BigDecimal.class, Instant.class), NumberConversions::toInstant);
        CONVERSION_DB.put(pair(AtomicInteger.class, Instant.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(AtomicLong.class, Instant.class), NumberConversions::toInstant);
        CONVERSION_DB.put(pair(java.sql.Date.class, Instant.class), DateConversions::toInstant);
        CONVERSION_DB.put(pair(Timestamp.class, Instant.class), DateConversions::toInstant);
        CONVERSION_DB.put(pair(Date.class, Instant.class), DateConversions::toInstant);
        CONVERSION_DB.put(pair(LocalDate.class, Instant.class), LocalDateConversions::toInstant);
        CONVERSION_DB.put(pair(LocalDateTime.class, Instant.class), LocalDateTimeConversions::toInstant);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Instant.class), ZonedDateTimeConversions::toInstant);
        CONVERSION_DB.put(pair(Calendar.class, Instant.class), CalendarConversions::toInstant);
        CONVERSION_DB.put(pair(Number.class, Instant.class), NumberConversions::toInstant);
        CONVERSION_DB.put(pair(String.class, Instant.class), StringConversions::toInstant);
        CONVERSION_DB.put(pair(Map.class, Instant.class), MapConversions::toInstant);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Instant.class), OffsetDateTimeConversions::toInstant);

        // ZoneId conversions supported
        CONVERSION_DB.put(pair(Void.class, ZoneId.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(ZoneId.class, ZoneId.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, ZoneId.class), StringConversions::toZoneId);
        CONVERSION_DB.put(pair(Map.class, ZoneId.class), MapConversions::toZoneId);
        
        // ZoneOffset conversions supported
        CONVERSION_DB.put(pair(Void.class, ZoneOffset.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(ZoneOffset.class, ZoneOffset.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, ZoneOffset.class), StringConversions::toZoneOffset);
        CONVERSION_DB.put(pair(Map.class, ZoneOffset.class), MapConversions::toZoneOffset);
        
        // MonthDay conversions supported
        CONVERSION_DB.put(pair(Void.class, MonthDay.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(MonthDay.class, MonthDay.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, MonthDay.class), StringConversions::toMonthDay);
        CONVERSION_DB.put(pair(Map.class, MonthDay.class), MapConversions::toMonthDay);

        // YearMonth conversions supported
        CONVERSION_DB.put(pair(Void.class, YearMonth.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(YearMonth.class, YearMonth.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, YearMonth.class), StringConversions::toYearMonth);
        CONVERSION_DB.put(pair(Map.class, YearMonth.class), MapConversions::toYearMonth);

        // Period conversions supported
        CONVERSION_DB.put(pair(Void.class, Period.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Period.class, Period.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, Period.class), StringConversions::toPeriod);
        CONVERSION_DB.put(pair(Map.class, Period.class), MapConversions::toPeriod);

        // toStringBuffer
        CONVERSION_DB.put(pair(Void.class, StringBuffer.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, StringBuffer.class), StringConversions::toStringBuffer);
        CONVERSION_DB.put(pair(StringBuilder.class, StringBuffer.class), StringConversions::toStringBuffer);
        CONVERSION_DB.put(pair(StringBuffer.class, StringBuffer.class), StringConversions::toStringBuffer);
        CONVERSION_DB.put(pair(ByteBuffer.class, StringBuffer.class), ByteBufferConversions::toStringBuffer);
        CONVERSION_DB.put(pair(CharBuffer.class, StringBuffer.class), CharBufferConversions::toStringBuffer);
        CONVERSION_DB.put(pair(Character[].class, StringBuffer.class), CharacterArrayConversions::toStringBuffer);
        CONVERSION_DB.put(pair(char[].class, StringBuffer.class), CharArrayConversions::toStringBuffer);
        CONVERSION_DB.put(pair(byte[].class, StringBuffer.class), ByteArrayConversions::toStringBuffer);

        // toStringBuilder
        CONVERSION_DB.put(pair(Void.class, StringBuilder.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, StringBuilder.class), StringConversions::toStringBuilder);
        CONVERSION_DB.put(pair(StringBuilder.class, StringBuilder.class), StringConversions::toStringBuilder);
        CONVERSION_DB.put(pair(StringBuffer.class, StringBuilder.class), StringConversions::toStringBuilder);
        CONVERSION_DB.put(pair(ByteBuffer.class, StringBuilder.class), ByteBufferConversions::toStringBuilder);
        CONVERSION_DB.put(pair(CharBuffer.class, StringBuilder.class), CharBufferConversions::toStringBuilder);
        CONVERSION_DB.put(pair(Character[].class, StringBuilder.class), CharacterArrayConversions::toStringBuilder);
        CONVERSION_DB.put(pair(char[].class, StringBuilder.class), CharArrayConversions::toStringBuilder);
        CONVERSION_DB.put(pair(byte[].class, StringBuilder.class), ByteArrayConversions::toStringBuilder);

        // toByteArray
        CONVERSION_DB.put(pair(Void.class, byte[].class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, byte[].class), StringConversions::toByteArray);
        CONVERSION_DB.put(pair(StringBuilder.class, byte[].class), StringConversions::toByteArray);
        CONVERSION_DB.put(pair(StringBuffer.class, byte[].class), StringConversions::toByteArray);
        CONVERSION_DB.put(pair(ByteBuffer.class, byte[].class), ByteBufferConversions::toByteArray);
        CONVERSION_DB.put(pair(CharBuffer.class, byte[].class), CharBufferConversions::toByteArray);
        CONVERSION_DB.put(pair(char[].class, byte[].class), CharArrayConversions::toByteArray);
        CONVERSION_DB.put(pair(byte[].class, byte[].class), Converter::identity);

        // toCharArray
        CONVERSION_DB.put(pair(Void.class, char[].class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Void.class, Character[].class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, char[].class), StringConversions::toCharArray);
        CONVERSION_DB.put(pair(StringBuilder.class, char[].class), StringConversions::toCharArray);
        CONVERSION_DB.put(pair(StringBuffer.class, char[].class), StringConversions::toCharArray);
        CONVERSION_DB.put(pair(ByteBuffer.class, char[].class), ByteBufferConversions::toCharArray);
        CONVERSION_DB.put(pair(CharBuffer.class, char[].class), CharBufferConversions::toCharArray);
        CONVERSION_DB.put(pair(char[].class, char[].class), CharArrayConversions::toCharArray);
        CONVERSION_DB.put(pair(byte[].class, char[].class), ByteArrayConversions::toCharArray);

        // toCharBuffer
        CONVERSION_DB.put(pair(Void.class, CharBuffer.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, CharBuffer.class), StringConversions::toCharBuffer);
        CONVERSION_DB.put(pair(StringBuilder.class, CharBuffer.class), StringConversions::toCharBuffer);
        CONVERSION_DB.put(pair(StringBuffer.class, CharBuffer.class), StringConversions::toCharBuffer);
        CONVERSION_DB.put(pair(ByteBuffer.class, CharBuffer.class), ByteBufferConversions::toCharBuffer);
        CONVERSION_DB.put(pair(CharBuffer.class, CharBuffer.class), CharBufferConversions::toCharBuffer);
        CONVERSION_DB.put(pair(char[].class, CharBuffer.class), CharArrayConversions::toCharBuffer);
        CONVERSION_DB.put(pair(byte[].class, CharBuffer.class), ByteArrayConversions::toCharBuffer);

        // toByteBuffer
        CONVERSION_DB.put(pair(Void.class, ByteBuffer.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, ByteBuffer.class), StringConversions::toByteBuffer);
        CONVERSION_DB.put(pair(StringBuilder.class, ByteBuffer.class), StringConversions::toByteBuffer);
        CONVERSION_DB.put(pair(StringBuffer.class, ByteBuffer.class), StringConversions::toByteBuffer);
        CONVERSION_DB.put(pair(ByteBuffer.class, ByteBuffer.class), ByteBufferConversions::toByteBuffer);
        CONVERSION_DB.put(pair(CharBuffer.class, ByteBuffer.class), CharBufferConversions::toByteBuffer);
        CONVERSION_DB.put(pair(char[].class, ByteBuffer.class), CharArrayConversions::toByteBuffer);
        CONVERSION_DB.put(pair(byte[].class, ByteBuffer.class), ByteArrayConversions::toByteBuffer);

        // toYear
        CONVERSION_DB.put(pair(Void.class, Year.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Year.class, Year.class), Converter::identity);
        CONVERSION_DB.put(pair(Byte.class, Year.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Number.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(String.class, Year.class), StringConversions::toYear);
        CONVERSION_DB.put(pair(Map.class, Year.class), MapConversions::toYear);

        // Map conversions supported
        CONVERSION_DB.put(pair(Void.class, Map.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Short.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Integer.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Long.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Float.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Double.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Boolean.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Character.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(BigInteger.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(BigDecimal.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(AtomicInteger.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(AtomicLong.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Date.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(java.sql.Date.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Timestamp.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(LocalDate.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(LocalDateTime.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Duration.class, Map.class), DurationConversions::toMap);
        CONVERSION_DB.put(pair(Instant.class, Map.class), InstantConversions::toMap);
        CONVERSION_DB.put(pair(LocalTime.class, Map.class), LocalTimeConversions::toMap);
        CONVERSION_DB.put(pair(MonthDay.class, Map.class), MonthDayConversions::toMap);
        CONVERSION_DB.put(pair(YearMonth.class, Map.class), YearMonthConversions::toMap);
        CONVERSION_DB.put(pair(Period.class, Map.class), PeriodConversions::toMap);
        CONVERSION_DB.put(pair(ZoneId.class, Map.class), ZoneIdConversions::toMap);
        CONVERSION_DB.put(pair(ZoneOffset.class, Map.class), ZoneOffsetConversions::toMap);
        CONVERSION_DB.put(pair(Class.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(UUID.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Calendar.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Number.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(Map.class, Map.class), MapConversions::toMap);
        CONVERSION_DB.put(pair(Enum.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Map.class), OffsetDateTimeConversions::toMap);
    }

    public Converter(ConverterOptions options) {
        this.options = options;
        this.factory = new ConcurrentHashMap<>(CONVERSION_DB);
        this.factory.putAll(this.options.getConverterOverrides());
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
     * @see #getSupportedConversions()
     * @return An instanceof targetType class, based upon the value passed in.
     */
    @SuppressWarnings("unchecked")
    public <T> T convert(Object from, Class<T> toType) {
        if (toType == null) {
            throw new IllegalArgumentException("toType cannot be null");
        }
        Class<?> sourceType;
        if (from == null) {
            // Do not promote primitive to primitive wrapper - allows for different 'from NULL' type for each.
            sourceType = Void.class;
        } else {
            // Promote primitive to primitive wrapper, so we don't have to define so many duplicates in the factory map.
            sourceType = from.getClass();
            if (toType.isPrimitive()) {
                toType = (Class<T>)  ClassUtilities.toPrimitiveWrapperClass(toType);
            }
        }

        // Direct Mapping
        Convert<?> converter = factory.get(pair(sourceType, toType));
        if (converter != null && converter != UNSUPPORTED) {
            return (T) converter.convert(from, this);
        }

        // Try inheritance
        converter = getInheritedConverter(sourceType, toType);
        if (converter != null && converter != UNSUPPORTED) {
            // Fast lookup next time.
            if (!isDirectConversionSupportedFor(sourceType, toType)) {
                addConversion(sourceType, toType, converter);
            }
            return (T) converter.convert(from, this);
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
        source =  ClassUtilities.toPrimitiveWrapperClass(source);
        target =  ClassUtilities.toPrimitiveWrapperClass(target);
        Convert<?> method = factory.get(pair(source, target));
        return method != null && method != UNSUPPORTED;
    }

    /**
     * Check to see if a conversion from type to another type is supported (may use inheritance via super classes/interfaces).
     *
     * @param source Class of source type.
     * @param target Class of target type.
     * @return boolean true if the Converter converts from the source type to the destination type, false otherwise.
     */
    public boolean isConversionSupportedFor(Class<?> source, Class<?> target) {
        source =  ClassUtilities.toPrimitiveWrapperClass(source);
        target =  ClassUtilities.toPrimitiveWrapperClass(target);
        Convert<?> method = factory.get(pair(source, target));
        if (method != null && method != UNSUPPORTED) {
            return true;
        }

        method = getInheritedConverter(source, target);
        return method != null && method != UNSUPPORTED;
    }

    /**
     * @return Map<Class, Set < Class>> which contains all supported conversions. The key of the Map is a source class,
     * and the Set contains all the target types (classes) that the source can be converted to.
     */
    public Map<Class<?>, Set<Class<?>>> allSupportedConversions() {
        Map<Class<?>, Set<Class<?>>> toFrom = new TreeMap<>((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()));

        for (Map.Entry<Map.Entry<Class<?>, Class<?>>, Convert<?>> entry : factory.entrySet()) {
            if (entry.getValue() != UNSUPPORTED) {
                Map.Entry<Class<?>, Class<?>> pair = entry.getKey();
                toFrom.computeIfAbsent(pair.getKey(), k -> new TreeSet<>((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))).add(pair.getValue());
            }
        }
        return toFrom;
    }

    /**
     * @return Map<String, Set < String>> which contains all supported conversions. The key of the Map is a source class
     * name, and the Set contains all the target class names that the source can be converted to.
     */
    public Map<String, Set<String>> getSupportedConversions() {
        Map<String, Set<String>> toFrom = new TreeMap<>(String::compareToIgnoreCase);

        for (Map.Entry<Map.Entry<Class<?>, Class<?>>, Convert<?>> entry : factory.entrySet()) {
            if (entry.getValue() != UNSUPPORTED) {
                Map.Entry<Class<?>, Class<?>> pair = entry.getKey();
                toFrom.computeIfAbsent(getShortName(pair.getKey()), k -> new TreeSet<>(String::compareToIgnoreCase)).add(getShortName(pair.getValue()));
            }
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
        source = ClassUtilities.toPrimitiveWrapperClass(source);
        target = ClassUtilities.toPrimitiveWrapperClass(target);
        return factory.put(pair(source, target), conversionFunction);
    }

    /**
     * Given a primitive class, return the Wrapper class equivalent.
     */
    private static <T> T identity(T from, Converter converter) {
        return from;
    }

    private static <T> T unsupported(T from, Converter converter) {
        return null;
    }
}
