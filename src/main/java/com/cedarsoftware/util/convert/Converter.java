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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.ClassUtilities;


/**
 * Instance conversion utility for converting objects between various types.
 * <p>
 * Supports conversion from primitive types to their corresponding wrapper classes, Number classes,
 * Date and Time classes (e.g., {@link Date}, {@link Timestamp}, {@link LocalDate}, {@link LocalDateTime},
 * {@link ZonedDateTime}, {@link Calendar}), {@link BigInteger}, {@link BigDecimal}, Atomic classes
 * (e.g., {@link AtomicBoolean}, {@link AtomicInteger}, {@link AtomicLong}), {@link Class}, {@link UUID},
 * {@link String}, Collection classes (e.g., {@link List}, {@link Set}, {@link Map}), ByteBuffer, CharBuffer,
 * and other related classes.
 * </p>
 * <p>
 * The Converter includes thousands of built-in conversions. Use the {@link #getSupportedConversions()}
 * API to view all source-to-target conversion mappings.
 * </p>
 * <p>
 * The primary API is {@link #convert(Object, Class)}. For example:
 * <pre>{@code
 *     Long x = convert("35", Long.class);
 *     Date d = convert("2015/01/01", Date.class);
 *     int y = convert(45.0, int.class);
 *     String dateStr = convert(date, String.class);
 *     String dateStr = convert(calendar, String.class);
 *     Short t = convert(true, short.class);     // returns (short) 1 or 0
 *     Long time = convert(calendar, long.class); // retrieves calendar's time as long
 *     Map<String, Object> map = Map.of("_v", "75.0");
 *     Double value = convert(map, double.class); // Extracts "_v" key and converts it
 * }</pre>
 * </p>
 * <p>
 * <strong>Null Handling:</strong> If a null value is passed as the source, the Converter returns:
 * <ul>
 *     <li>null for object types</li>
 *     <li>0 for numeric primitive types</li>
 *     <li>false for boolean primitives</li>
 *     <li>'\u0000' for char primitives</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Map Conversions:</strong> A {@code Map} can be converted to almost all supported JDK data classes.
 * For example, {@link UUID} can be converted to/from a {@code Map} with keys like "mostSigBits" and "leastSigBits".
 * Date/Time classes expect specific keys such as "time" or "nanos". For other classes, the Converter typically
 * looks for a "value" key to source the conversion.
 * </p>
 * <p>
 * <strong>Extensibility:</strong> Additional conversions can be added by specifying the source class, target class,
 * and a conversion function (e.g., a lambda). Use the {@link #addConversion(Class, Class, Convert)} method to register
 * custom converters. This allows for the inclusion of new Collection types and other custom types as needed.
 * </p>
 *
 * <p>
 * <strong>Supported Collection Conversions:</strong>
 * The Converter supports conversions involving various Collection types, including but not limited to:
 * <ul>
 *     <li>{@link List}</li>
 *     <li>{@link Set}</li>
 *     <li>{@link Map}</li>
 *     <li>{@link Collection}</li>
 *     <li>Arrays (e.g., {@code byte[]}, {@code char[]}, {@code ByteBuffer}, {@code CharBuffer})</li>
 * </ul>
 * These conversions facilitate seamless transformation between different Collection types and other supported classes.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>{@code
 *     ConverterOptions options = new ConverterOptions();
 *     Converter converter = new Converter(options);
 *
 *     // Convert String to Integer
 *     Integer number = converter.convert("123", Integer.class);
 *
 *     // Convert Enum to String
 *     Day day = Day.MONDAY;
 *     String dayStr = converter.convert(day, String.class);
 *
 *     // Convert Object[], String[], Collection, and primitive Arrays to EnumSet
 *     Object[] array = {Day.MONDAY, Day.WEDNESDAY, "FRIDAY", 4};
 *     EnumSet<Day> daySet = (EnumSet<Day>)(Object)converter.convert(array, Day.class);
 *
 *     Enum, String, and Number value in the source collection/array is properly converted
 *     to the correct Enum type and added to the returned EnumSet. Null values inside the
 *     source (Object[], Collection) are skipped.
 *
 *     When converting arrays or collections to EnumSet, you must use a double cast due to Java's
 *     type system and generic type erasure. The cast is safe as the converter guarantees return of
 *     an EnumSet when converting arrays/collections to enum types.
 *
 *     // Add a custom conversion from String to CustomType
 *     converter.addConversion(String.class, CustomType.class, (from, conv) -> new CustomType(from));
 *
 *     // Convert using the custom converter
 *     CustomType custom = converter.convert("customValue", CustomType.class);
 * }</pre>
 * </p>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
    private static final Convert<?> UNSUPPORTED = Converter::unsupported;
    static final String VALUE = "_v";
    private static final Map<Class<?>, SortedSet<ClassLevel>> cacheParentTypes = new ConcurrentHashMap<>();
    private static final Map<ConversionPair, Convert<?>> CONVERSION_DB = new HashMap<>(860, 0.8f);
    private final Map<ConversionPair, Convert<?>> USER_DB = new ConcurrentHashMap<>();
    private final ConverterOptions options;
    private static final Map<Class<?>, String> CUSTOM_ARRAY_NAMES = new HashMap<>();
    
    // Thread-local cache for frequently used conversion keys
    private static final ThreadLocal<Map<Long, ConversionPair>> KEY_CACHE = ThreadLocal.withInitial(
            () -> new HashMap<>(32)
    );

    // Efficient key that combines two Class instances for fast creation and lookup
    public static final class ConversionPair {
        private final Class<?> source;
        private final Class<?> target;
        private final int hash;

        public ConversionPair(Class<?> source, Class<?> target) {
            this.source = source;
            this.target = target;
            this.hash = 31 * source.hashCode() + target.hashCode();
        }

        public Class<?> getSource() {  // Added getter
            return source;
        }

        public Class<?> getTarget() {  // Added getter
            return target;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ConversionPair)) return false;
            ConversionPair other = (ConversionPair) obj;
            return source == other.source && target == other.target;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    // Helper method to get or create a cached key
    private static ConversionPair pair(Class<?> source, Class<?> target) {
        // Combine source and target class identities into a single long for cache lookup
        long cacheKey = ((long)System.identityHashCode(source) << 32) | System.identityHashCode(target);
        Map<Long, ConversionPair> cache = KEY_CACHE.get();
        ConversionPair key = cache.get(cacheKey);
        if (key == null) {
            key = new ConversionPair(source, target);
            cache.put(cacheKey, key);
        }
        return key;
    }
    
    static {
        CUSTOM_ARRAY_NAMES.put(java.sql.Date[].class, "java.sql.Date[]");
        buildFactoryConversions();
    }

    /**
     * Retrieves the converter options associated with this Converter instance.
     *
     * @return The {@link ConverterOptions} used by this Converter.
     */
    public ConverterOptions getOptions() {
        return options;
    }

    /**
     * Initializes the built-in conversion mappings within the Converter.
     * <p>
     * This method populates the {@link #CONVERSION_DB} with a comprehensive set of predefined conversion functions
     * that handle a wide range of type transformations, including primitives, wrappers, numbers, dates, times,
     * collections, and more.
     * </p>
     * <p>
     * These conversions serve as the foundational capabilities of the Converter, enabling it to perform most
     * common type transformations out-of-the-box. Users can extend or override these conversions using the
     * {@link #addConversion(Class, Class, Convert)} method as needed.
     * </p>
     */
    @SuppressWarnings("unchecked")
    private static void buildFactoryConversions() {
        // toNumber
        CONVERSION_DB.put(pair(Byte.class, Number.class), Converter::identity);
        CONVERSION_DB.put(pair(Short.class, Number.class), Converter::identity);
        CONVERSION_DB.put(pair(Integer.class, Number.class), Converter::identity);
        CONVERSION_DB.put(pair(Long.class, Number.class), Converter::identity);
        CONVERSION_DB.put(pair(Float.class, Number.class), Converter::identity);
        CONVERSION_DB.put(pair(Double.class, Number.class), Converter::identity);
        CONVERSION_DB.put(pair(AtomicInteger.class, Number.class), Converter::identity);
        CONVERSION_DB.put(pair(AtomicLong.class, Number.class), Converter::identity);
        CONVERSION_DB.put(pair(BigInteger.class, Number.class), Converter::identity);
        CONVERSION_DB.put(pair(BigDecimal.class, Number.class), Converter::identity);

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
        CONVERSION_DB.put(pair(Boolean.class, Integer.class), BooleanConversions::toInt);
        CONVERSION_DB.put(pair(Character.class, Integer.class), CharacterConversions::toInt);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Integer.class), AtomicBooleanConversions::toInt);
        CONVERSION_DB.put(pair(AtomicInteger.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(AtomicLong.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(BigInteger.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(BigDecimal.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Map.class, Integer.class), MapConversions::toInt);
        CONVERSION_DB.put(pair(String.class, Integer.class), StringConversions::toInt);
        CONVERSION_DB.put(pair(LocalTime.class, Integer.class), LocalTimeConversions::toInteger);
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
        CONVERSION_DB.put(pair(Duration.class, Long.class), DurationConversions::toLong);
        CONVERSION_DB.put(pair(LocalDate.class, Long.class), LocalDateConversions::toLong);
        CONVERSION_DB.put(pair(LocalTime.class, Long.class), LocalTimeConversions::toLong);
        CONVERSION_DB.put(pair(LocalDateTime.class, Long.class), LocalDateTimeConversions::toLong);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Long.class), OffsetDateTimeConversions::toLong);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Long.class), ZonedDateTimeConversions::toLong);
        CONVERSION_DB.put(pair(Calendar.class, Long.class), CalendarConversions::toLong);
        CONVERSION_DB.put(pair(Map.class, Long.class), MapConversions::toLong);
        CONVERSION_DB.put(pair(String.class, Long.class), StringConversions::toLong);
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
        CONVERSION_DB.put(pair(Duration.class, Double.class), DurationConversions::toDouble);
        CONVERSION_DB.put(pair(Instant.class, Double.class), InstantConversions::toDouble);
        CONVERSION_DB.put(pair(LocalTime.class, Double.class), LocalTimeConversions::toDouble);
        CONVERSION_DB.put(pair(LocalDate.class, Double.class), LocalDateConversions::toDouble);
        CONVERSION_DB.put(pair(LocalDateTime.class, Double.class), LocalDateTimeConversions::toDouble);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Double.class), ZonedDateTimeConversions::toDouble);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Double.class), OffsetDateTimeConversions::toDouble);
        CONVERSION_DB.put(pair(Date.class, Double.class), DateConversions::toDouble);
        CONVERSION_DB.put(pair(java.sql.Date.class, Double.class), DateConversions::toDouble);
        CONVERSION_DB.put(pair(Timestamp.class, Double.class), TimestampConversions::toDouble);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Double.class), AtomicBooleanConversions::toDouble);
        CONVERSION_DB.put(pair(AtomicInteger.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(AtomicLong.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(BigInteger.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(BigDecimal.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(Calendar.class, Double.class), CalendarConversions::toDouble);
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
        CONVERSION_DB.put(pair(Map.class, Boolean.class), MapConversions::toBoolean);
        CONVERSION_DB.put(pair(String.class, Boolean.class), StringConversions::toBoolean);

        // Character/char conversions supported
        CONVERSION_DB.put(pair(Void.class, char.class), VoidConversions::toCharacter);
        CONVERSION_DB.put(pair(Void.class, Character.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Character.class), ByteConversions::toCharacter);
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
        CONVERSION_DB.put(pair(BigDecimal.class, BigInteger.class), BigDecimalConversions::toBigInteger);
        CONVERSION_DB.put(pair(AtomicBoolean.class, BigInteger.class), AtomicBooleanConversions::toBigInteger);
        CONVERSION_DB.put(pair(AtomicInteger.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(AtomicLong.class, BigInteger.class),  NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(Date.class, BigInteger.class), DateConversions::toBigInteger);
        CONVERSION_DB.put(pair(java.sql.Date.class, BigInteger.class), DateConversions::toBigInteger);
        CONVERSION_DB.put(pair(Timestamp.class, BigInteger.class), TimestampConversions::toBigInteger);
        CONVERSION_DB.put(pair(Duration.class, BigInteger.class), DurationConversions::toBigInteger);
        CONVERSION_DB.put(pair(Instant.class, BigInteger.class), InstantConversions::toBigInteger);
        CONVERSION_DB.put(pair(LocalTime.class, BigInteger.class), LocalTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(LocalDate.class, BigInteger.class), LocalDateConversions::toBigInteger);
        CONVERSION_DB.put(pair(LocalDateTime.class, BigInteger.class), LocalDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(ZonedDateTime.class, BigInteger.class), ZonedDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(OffsetDateTime.class, BigInteger.class), OffsetDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(UUID.class, BigInteger.class), UUIDConversions::toBigInteger);
        CONVERSION_DB.put(pair(Calendar.class, BigInteger.class), CalendarConversions::toBigInteger);
        CONVERSION_DB.put(pair(Map.class, BigInteger.class), MapConversions::toBigInteger);
        CONVERSION_DB.put(pair(String.class, BigInteger.class), StringConversions::toBigInteger);
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
        CONVERSION_DB.put(pair(BigInteger.class, BigDecimal.class), BigIntegerConversions::toBigDecimal);
        CONVERSION_DB.put(pair(AtomicBoolean.class, BigDecimal.class), AtomicBooleanConversions::toBigDecimal);
        CONVERSION_DB.put(pair(AtomicInteger.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(pair(AtomicLong.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(pair(Date.class, BigDecimal.class), DateConversions::toBigDecimal);
        CONVERSION_DB.put(pair(java.sql.Date.class, BigDecimal.class), DateConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Timestamp.class, BigDecimal.class), TimestampConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Instant.class, BigDecimal.class), InstantConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Duration.class, BigDecimal.class), DurationConversions::toBigDecimal);
        CONVERSION_DB.put(pair(LocalTime.class, BigDecimal.class), LocalTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(LocalDate.class, BigDecimal.class), LocalDateConversions::toBigDecimal);
        CONVERSION_DB.put(pair(LocalDateTime.class, BigDecimal.class), LocalDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(ZonedDateTime.class, BigDecimal.class), ZonedDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(OffsetDateTime.class, BigDecimal.class), OffsetDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(UUID.class, BigDecimal.class), UUIDConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Calendar.class, BigDecimal.class), CalendarConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Map.class, BigDecimal.class), MapConversions::toBigDecimal);
        CONVERSION_DB.put(pair(String.class, BigDecimal.class), StringConversions::toBigDecimal);
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
        CONVERSION_DB.put(pair(AtomicInteger.class, AtomicInteger.class), AtomicIntegerConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(AtomicBoolean.class, AtomicInteger.class), AtomicBooleanConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(AtomicLong.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(LocalTime.class, AtomicInteger.class), LocalTimeConversions::toAtomicInteger);
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
        CONVERSION_DB.put(pair(AtomicInteger.class, AtomicLong.class), NumberConversions::toAtomicLong);
        CONVERSION_DB.put(pair(AtomicLong.class, AtomicLong.class), AtomicLongConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Date.class, AtomicLong.class), DateConversions::toAtomicLong);
        CONVERSION_DB.put(pair(java.sql.Date.class, AtomicLong.class), DateConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Timestamp.class, AtomicLong.class), DateConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Instant.class, AtomicLong.class), InstantConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Duration.class, AtomicLong.class), DurationConversions::toAtomicLong);
        CONVERSION_DB.put(pair(LocalDate.class, AtomicLong.class), LocalDateConversions::toAtomicLong);
        CONVERSION_DB.put(pair(LocalTime.class, AtomicLong.class), LocalTimeConversions::toAtomicLong);
        CONVERSION_DB.put(pair(LocalDateTime.class, AtomicLong.class), LocalDateTimeConversions::toAtomicLong);
        CONVERSION_DB.put(pair(ZonedDateTime.class, AtomicLong.class), ZonedDateTimeConversions::toAtomicLong);
        CONVERSION_DB.put(pair(OffsetDateTime.class, AtomicLong.class), OffsetDateTimeConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Calendar.class, AtomicLong.class), CalendarConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Map.class, AtomicLong.class), MapConversions::toAtomicLong);
        CONVERSION_DB.put(pair(String.class, AtomicLong.class), StringConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Year.class, AtomicLong.class), YearConversions::toAtomicLong);

        // Date conversions supported
        CONVERSION_DB.put(pair(Void.class, Date.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, Date.class), NumberConversions::toDate);
        CONVERSION_DB.put(pair(Double.class, Date.class), DoubleConversions::toDate);
        CONVERSION_DB.put(pair(BigInteger.class, Date.class), BigIntegerConversions::toDate);
        CONVERSION_DB.put(pair(BigDecimal.class, Date.class), BigDecimalConversions::toDate);
        CONVERSION_DB.put(pair(AtomicLong.class, Date.class), NumberConversions::toDate);
        CONVERSION_DB.put(pair(Date.class, Date.class), DateConversions::toDate);
        CONVERSION_DB.put(pair(java.sql.Date.class, Date.class), DateConversions::toDate);
        CONVERSION_DB.put(pair(Timestamp.class, Date.class), DateConversions::toDate);
        CONVERSION_DB.put(pair(Instant.class, Date.class), InstantConversions::toDate);
        CONVERSION_DB.put(pair(LocalDate.class, Date.class), LocalDateConversions::toDate);
        CONVERSION_DB.put(pair(LocalDateTime.class, Date.class), LocalDateTimeConversions::toDate);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Date.class), ZonedDateTimeConversions::toDate);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Date.class), OffsetDateTimeConversions::toDate);
        CONVERSION_DB.put(pair(Calendar.class, Date.class), CalendarConversions::toDate);
        CONVERSION_DB.put(pair(Map.class, Date.class), MapConversions::toDate);
        CONVERSION_DB.put(pair(String.class, Date.class), StringConversions::toDate);

        // java.sql.Date conversion supported
        CONVERSION_DB.put(pair(Void.class, java.sql.Date.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, java.sql.Date.class), NumberConversions::toSqlDate);
        CONVERSION_DB.put(pair(Double.class, java.sql.Date.class), DoubleConversions::toSqlDate);
        CONVERSION_DB.put(pair(BigInteger.class, java.sql.Date.class), BigIntegerConversions::toSqlDate);
        CONVERSION_DB.put(pair(BigDecimal.class, java.sql.Date.class), BigDecimalConversions::toSqlDate);
        CONVERSION_DB.put(pair(AtomicLong.class, java.sql.Date.class), NumberConversions::toSqlDate);
        CONVERSION_DB.put(pair(java.sql.Date.class, java.sql.Date.class), DateConversions::toSqlDate);
        CONVERSION_DB.put(pair(Date.class, java.sql.Date.class), DateConversions::toSqlDate);
        CONVERSION_DB.put(pair(Timestamp.class, java.sql.Date.class), DateConversions::toSqlDate);
        CONVERSION_DB.put(pair(Instant.class, java.sql.Date.class), InstantConversions::toSqlDate);
        CONVERSION_DB.put(pair(LocalDate.class, java.sql.Date.class), LocalDateConversions::toSqlDate);
        CONVERSION_DB.put(pair(LocalDateTime.class, java.sql.Date.class), LocalDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(pair(ZonedDateTime.class, java.sql.Date.class), ZonedDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(pair(OffsetDateTime.class, java.sql.Date.class), OffsetDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(pair(Calendar.class, java.sql.Date.class), CalendarConversions::toSqlDate);
        CONVERSION_DB.put(pair(Map.class, java.sql.Date.class), MapConversions::toSqlDate);
        CONVERSION_DB.put(pair(String.class, java.sql.Date.class), StringConversions::toSqlDate);

        // Timestamp conversions supported
        CONVERSION_DB.put(pair(Void.class, Timestamp.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, Timestamp.class), NumberConversions::toTimestamp);
        CONVERSION_DB.put(pair(Double.class, Timestamp.class), DoubleConversions::toTimestamp);
        CONVERSION_DB.put(pair(BigInteger.class, Timestamp.class), BigIntegerConversions::toTimestamp);
        CONVERSION_DB.put(pair(BigDecimal.class, Timestamp.class), BigDecimalConversions::toTimestamp);
        CONVERSION_DB.put(pair(AtomicLong.class, Timestamp.class), NumberConversions::toTimestamp);
        CONVERSION_DB.put(pair(Timestamp.class, Timestamp.class), DateConversions::toTimestamp);
        CONVERSION_DB.put(pair(java.sql.Date.class, Timestamp.class), DateConversions::toTimestamp);
        CONVERSION_DB.put(pair(Date.class, Timestamp.class), DateConversions::toTimestamp);
        CONVERSION_DB.put(pair(Duration.class, Timestamp.class), DurationConversions::toTimestamp);
        CONVERSION_DB.put(pair(Instant.class,Timestamp.class), InstantConversions::toTimestamp);
        CONVERSION_DB.put(pair(LocalDate.class, Timestamp.class), LocalDateConversions::toTimestamp);
        CONVERSION_DB.put(pair(LocalDateTime.class, Timestamp.class), LocalDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Timestamp.class), ZonedDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Timestamp.class), OffsetDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(pair(Calendar.class, Timestamp.class), CalendarConversions::toTimestamp);
        CONVERSION_DB.put(pair(Map.class, Timestamp.class), MapConversions::toTimestamp);
        CONVERSION_DB.put(pair(String.class, Timestamp.class), StringConversions::toTimestamp);

        // Calendar conversions supported
        CONVERSION_DB.put(pair(Void.class, Calendar.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, Calendar.class), NumberConversions::toCalendar);
        CONVERSION_DB.put(pair(Double.class, Calendar.class), DoubleConversions::toCalendar);
        CONVERSION_DB.put(pair(BigInteger.class, Calendar.class), BigIntegerConversions::toCalendar);
        CONVERSION_DB.put(pair(BigDecimal.class, Calendar.class), BigDecimalConversions::toCalendar);
        CONVERSION_DB.put(pair(AtomicLong.class, Calendar.class), NumberConversions::toCalendar);
        CONVERSION_DB.put(pair(Date.class, Calendar.class), DateConversions::toCalendar);
        CONVERSION_DB.put(pair(java.sql.Date.class, Calendar.class), DateConversions::toCalendar);
        CONVERSION_DB.put(pair(Timestamp.class, Calendar.class), TimestampConversions::toCalendar);
        CONVERSION_DB.put(pair(Instant.class, Calendar.class), InstantConversions::toCalendar);
        CONVERSION_DB.put(pair(LocalTime.class, Calendar.class), LocalTimeConversions::toCalendar);
        CONVERSION_DB.put(pair(LocalDate.class, Calendar.class), LocalDateConversions::toCalendar);
        CONVERSION_DB.put(pair(LocalDateTime.class, Calendar.class), LocalDateTimeConversions::toCalendar);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Calendar.class), ZonedDateTimeConversions::toCalendar);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Calendar.class), OffsetDateTimeConversions::toCalendar);
        CONVERSION_DB.put(pair(Calendar.class, Calendar.class), CalendarConversions::clone);
        CONVERSION_DB.put(pair(Map.class, Calendar.class), MapConversions::toCalendar);
        CONVERSION_DB.put(pair(String.class, Calendar.class), StringConversions::toCalendar);

        // LocalDate conversions supported
        CONVERSION_DB.put(pair(Void.class, LocalDate.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, LocalDate.class), NumberConversions::toLocalDate);
        CONVERSION_DB.put(pair(Double.class, LocalDate.class), DoubleConversions::toLocalDate);
        CONVERSION_DB.put(pair(BigInteger.class, LocalDate.class), BigIntegerConversions::toLocalDate);
        CONVERSION_DB.put(pair(BigDecimal.class, LocalDate.class), BigDecimalConversions::toLocalDate);
        CONVERSION_DB.put(pair(AtomicLong.class, LocalDate.class), NumberConversions::toLocalDate);
        CONVERSION_DB.put(pair(java.sql.Date.class, LocalDate.class), DateConversions::toLocalDate);
        CONVERSION_DB.put(pair(Timestamp.class, LocalDate.class), DateConversions::toLocalDate);
        CONVERSION_DB.put(pair(Date.class, LocalDate.class), DateConversions::toLocalDate);
        CONVERSION_DB.put(pair(Instant.class, LocalDate.class), InstantConversions::toLocalDate);
        CONVERSION_DB.put(pair(LocalDate.class, LocalDate.class), Converter::identity);
        CONVERSION_DB.put(pair(LocalDateTime.class, LocalDate.class), LocalDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(pair(ZonedDateTime.class, LocalDate.class), ZonedDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(pair(OffsetDateTime.class, LocalDate.class), OffsetDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(pair(Calendar.class, LocalDate.class), CalendarConversions::toLocalDate);
        CONVERSION_DB.put(pair(Map.class, LocalDate.class), MapConversions::toLocalDate);
        CONVERSION_DB.put(pair(String.class, LocalDate.class), StringConversions::toLocalDate);

        // LocalDateTime conversions supported
        CONVERSION_DB.put(pair(Void.class, LocalDateTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Double.class, LocalDateTime.class), DoubleConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(BigInteger.class, LocalDateTime.class), BigIntegerConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(BigDecimal.class, LocalDateTime.class), BigDecimalConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(AtomicLong.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(java.sql.Date.class, LocalDateTime.class), DateConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Timestamp.class, LocalDateTime.class), TimestampConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Date.class, LocalDateTime.class), DateConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Instant.class, LocalDateTime.class), InstantConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(LocalDateTime.class, LocalDateTime.class), LocalDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(LocalDate.class, LocalDateTime.class), LocalDateConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(ZonedDateTime.class, LocalDateTime.class), ZonedDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(OffsetDateTime.class, LocalDateTime.class), OffsetDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Calendar.class, LocalDateTime.class), CalendarConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Map.class, LocalDateTime.class), MapConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(String.class, LocalDateTime.class), StringConversions::toLocalDateTime);

        // LocalTime conversions supported
        CONVERSION_DB.put(pair(Void.class, LocalTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Integer.class, LocalTime.class), IntegerConversions::toLocalTime);
        CONVERSION_DB.put(pair(Long.class, LocalTime.class), LongConversions::toLocalTime);
        CONVERSION_DB.put(pair(Double.class, LocalTime.class), DoubleConversions::toLocalTime);
        CONVERSION_DB.put(pair(BigInteger.class, LocalTime.class), BigIntegerConversions::toLocalTime);
        CONVERSION_DB.put(pair(BigDecimal.class, LocalTime.class), BigDecimalConversions::toLocalTime);
        CONVERSION_DB.put(pair(AtomicInteger.class, LocalTime.class), AtomicIntegerConversions::toLocalTime);
        CONVERSION_DB.put(pair(AtomicLong.class, LocalTime.class), AtomicLongConversions::toLocalTime);
        CONVERSION_DB.put(pair(java.sql.Date.class, LocalTime.class), DateConversions::toLocalTime);
        CONVERSION_DB.put(pair(Timestamp.class, LocalTime.class), DateConversions::toLocalTime);
        CONVERSION_DB.put(pair(Date.class, LocalTime.class), DateConversions::toLocalTime);
        CONVERSION_DB.put(pair(Instant.class, LocalTime.class), InstantConversions::toLocalTime);
        CONVERSION_DB.put(pair(LocalDateTime.class, LocalTime.class), LocalDateTimeConversions::toLocalTime);
        CONVERSION_DB.put(pair(LocalTime.class, LocalTime.class), Converter::identity);
        CONVERSION_DB.put(pair(ZonedDateTime.class, LocalTime.class), ZonedDateTimeConversions::toLocalTime);
        CONVERSION_DB.put(pair(OffsetDateTime.class, LocalTime.class), OffsetDateTimeConversions::toLocalTime);
        CONVERSION_DB.put(pair(Calendar.class, LocalTime.class), CalendarConversions::toLocalTime);
        CONVERSION_DB.put(pair(Map.class, LocalTime.class), MapConversions::toLocalTime);
        CONVERSION_DB.put(pair(String.class, LocalTime.class), StringConversions::toLocalTime);

        // ZonedDateTime conversions supported
        CONVERSION_DB.put(pair(Void.class, ZonedDateTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Double.class, ZonedDateTime.class), DoubleConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(BigInteger.class, ZonedDateTime.class), BigIntegerConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(BigDecimal.class, ZonedDateTime.class), BigDecimalConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(AtomicLong.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(java.sql.Date.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Timestamp.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Date.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Instant.class, ZonedDateTime.class), InstantConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(LocalDate.class, ZonedDateTime.class), LocalDateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(LocalDateTime.class, ZonedDateTime.class), LocalDateTimeConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(ZonedDateTime.class, ZonedDateTime.class), Converter::identity);
        CONVERSION_DB.put(pair(OffsetDateTime.class, ZonedDateTime.class), OffsetDateTimeConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Calendar.class, ZonedDateTime.class), CalendarConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Map.class, ZonedDateTime.class), MapConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(String.class, ZonedDateTime.class), StringConversions::toZonedDateTime);

        // toOffsetDateTime
        CONVERSION_DB.put(pair(Void.class, OffsetDateTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(OffsetDateTime.class, OffsetDateTime.class), Converter::identity);
        CONVERSION_DB.put(pair(Map.class, OffsetDateTime.class), MapConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(String.class, OffsetDateTime.class), StringConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Long.class, OffsetDateTime.class), NumberConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(AtomicLong.class, OffsetDateTime.class), NumberConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Double.class, OffsetDateTime.class), DoubleConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(BigInteger.class, OffsetDateTime.class), BigIntegerConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(BigDecimal.class, OffsetDateTime.class), BigDecimalConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(java.sql.Date.class, OffsetDateTime.class), DateConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Date.class, OffsetDateTime.class), DateConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Calendar.class, OffsetDateTime.class), CalendarConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Timestamp.class, OffsetDateTime.class), TimestampConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(LocalDate.class, OffsetDateTime.class), LocalDateConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Instant.class, OffsetDateTime.class), InstantConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(ZonedDateTime.class, OffsetDateTime.class), ZonedDateTimeConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(LocalDateTime.class, OffsetDateTime.class), LocalDateTimeConversions::toOffsetDateTime);

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
        CONVERSION_DB.put(pair(BigInteger.class, UUID.class), BigIntegerConversions::toUUID);
        CONVERSION_DB.put(pair(BigDecimal.class, UUID.class), BigDecimalConversions::toUUID);
        CONVERSION_DB.put(pair(Map.class, UUID.class), MapConversions::toUUID);

        // Class conversions supported
        CONVERSION_DB.put(pair(Void.class, Class.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Class.class, Class.class), Converter::identity);
        CONVERSION_DB.put(pair(Map.class, Class.class), MapConversions::toClass);
        CONVERSION_DB.put(pair(String.class, Class.class), StringConversions::toClass);

        // Locale conversions supported
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
        CONVERSION_DB.put(pair(BigDecimal.class, String.class), BigDecimalConversions::toString);
        CONVERSION_DB.put(pair(AtomicBoolean.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(AtomicInteger.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(AtomicLong.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(byte[].class, String.class), ByteArrayConversions::toString);
        CONVERSION_DB.put(pair(char[].class, String.class), CharArrayConversions::toString);
        CONVERSION_DB.put(pair(Character[].class, String.class), CharacterArrayConversions::toString);
        CONVERSION_DB.put(pair(ByteBuffer.class, String.class), ByteBufferConversions::toString);
        CONVERSION_DB.put(pair(CharBuffer.class, String.class), CharBufferConversions::toString);
        CONVERSION_DB.put(pair(Class.class, String.class), ClassConversions::toString);
        CONVERSION_DB.put(pair(Date.class, String.class), DateConversions::toString);
        CONVERSION_DB.put(pair(java.sql.Date.class, String.class), DateConversions::sqlDateToString);
        CONVERSION_DB.put(pair(Timestamp.class, String.class), DateConversions::toString);
        CONVERSION_DB.put(pair(LocalDate.class, String.class), LocalDateConversions::toString);
        CONVERSION_DB.put(pair(LocalTime.class, String.class), LocalTimeConversions::toString);
        CONVERSION_DB.put(pair(LocalDateTime.class, String.class), LocalDateTimeConversions::toString);
        CONVERSION_DB.put(pair(ZonedDateTime.class, String.class), ZonedDateTimeConversions::toString);
        CONVERSION_DB.put(pair(UUID.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(Calendar.class, String.class), CalendarConversions::toString);
        CONVERSION_DB.put(pair(Map.class, String.class), MapConversions::toString);
        CONVERSION_DB.put(pair(Enum.class, String.class), StringConversions::enumToString);
        CONVERSION_DB.put(pair(String.class, String.class), Converter::identity);
        CONVERSION_DB.put(pair(Duration.class, String.class), StringConversions::toString);
        CONVERSION_DB.put(pair(Instant.class, String.class), StringConversions::toString);
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
        CONVERSION_DB.put(pair(TimeZone.class, String.class), TimeZoneConversions::toString);
        CONVERSION_DB.put(pair(StringBuilder.class, String.class), StringBuilderConversions::toString);
        CONVERSION_DB.put(pair(StringBuffer.class, String.class), StringBufferConversions::toString);

        // URL conversions
        CONVERSION_DB.put(pair(Void.class, URL.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(URL.class, URL.class), Converter::identity);
        CONVERSION_DB.put(pair(URI.class, URL.class), UriConversions::toURL);
        CONVERSION_DB.put(pair(String.class, URL.class), StringConversions::toURL);
        CONVERSION_DB.put(pair(Map.class, URL.class), MapConversions::toURL);

        // URI Conversions
        CONVERSION_DB.put(pair(Void.class, URI.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(URI.class, URI.class), Converter::identity);
        CONVERSION_DB.put(pair(URL.class, URI.class), UrlConversions::toURI);
        CONVERSION_DB.put(pair(String.class, URI.class), StringConversions::toURI);
        CONVERSION_DB.put(pair(Map.class, URI.class), MapConversions::toURI);

        // TimeZone Conversions
        CONVERSION_DB.put(pair(Void.class, TimeZone.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(TimeZone.class, TimeZone.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, TimeZone.class), StringConversions::toTimeZone);
        CONVERSION_DB.put(pair(Map.class, TimeZone.class), MapConversions::toTimeZone);
        CONVERSION_DB.put(pair(ZoneId.class, TimeZone.class), ZoneIdConversions::toTimeZone);
        CONVERSION_DB.put(pair(ZoneOffset.class, TimeZone.class), UNSUPPORTED);

        // Duration conversions supported
        CONVERSION_DB.put(pair(Void.class, Duration.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Duration.class, Duration.class), Converter::identity);
        CONVERSION_DB.put(pair(Long.class, Duration.class), NumberConversions::toDuration);
        CONVERSION_DB.put(pair(Double.class, Duration.class), DoubleConversions::toDuration);
        CONVERSION_DB.put(pair(AtomicLong.class, Duration.class), NumberConversions::toDuration);
        CONVERSION_DB.put(pair(BigInteger.class, Duration.class), BigIntegerConversions::toDuration);
        CONVERSION_DB.put(pair(BigDecimal.class, Duration.class), BigDecimalConversions::toDuration);
        CONVERSION_DB.put(pair(Timestamp.class, Duration.class), TimestampConversions::toDuration);
        CONVERSION_DB.put(pair(String.class, Duration.class), StringConversions::toDuration);
        CONVERSION_DB.put(pair(Map.class, Duration.class), MapConversions::toDuration);

        // Instant conversions supported
        CONVERSION_DB.put(pair(Void.class, Instant.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Instant.class, Instant.class), Converter::identity);
        CONVERSION_DB.put(pair(Long.class, Instant.class), NumberConversions::toInstant);
        CONVERSION_DB.put(pair(Double.class, Instant.class), DoubleConversions::toInstant);
        CONVERSION_DB.put(pair(BigInteger.class, Instant.class), BigIntegerConversions::toInstant);
        CONVERSION_DB.put(pair(BigDecimal.class, Instant.class), BigDecimalConversions::toInstant);
        CONVERSION_DB.put(pair(AtomicLong.class, Instant.class), NumberConversions::toInstant);
        CONVERSION_DB.put(pair(java.sql.Date.class, Instant.class), DateConversions::toInstant);
        CONVERSION_DB.put(pair(Timestamp.class, Instant.class), DateConversions::toInstant);
        CONVERSION_DB.put(pair(Date.class, Instant.class), DateConversions::toInstant);
        CONVERSION_DB.put(pair(LocalDate.class, Instant.class), LocalDateConversions::toInstant);
        CONVERSION_DB.put(pair(LocalDateTime.class, Instant.class), LocalDateTimeConversions::toInstant);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Instant.class), ZonedDateTimeConversions::toInstant);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Instant.class), OffsetDateTimeConversions::toInstant);
        CONVERSION_DB.put(pair(Calendar.class, Instant.class), CalendarConversions::toInstant);
        CONVERSION_DB.put(pair(String.class, Instant.class), StringConversions::toInstant);
        CONVERSION_DB.put(pair(Map.class, Instant.class), MapConversions::toInstant);

        // ZoneId conversions supported
        CONVERSION_DB.put(pair(Void.class, ZoneId.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(ZoneId.class, ZoneId.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, ZoneId.class), StringConversions::toZoneId);
        CONVERSION_DB.put(pair(Map.class, ZoneId.class), MapConversions::toZoneId);
        CONVERSION_DB.put(pair(TimeZone.class, ZoneId.class), TimeZoneConversions::toZoneId);
        CONVERSION_DB.put(pair(ZoneOffset.class, ZoneId.class), ZoneOffsetConversions::toZoneId);

        // ZoneOffset conversions supported
        CONVERSION_DB.put(pair(Void.class, ZoneOffset.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(ZoneOffset.class, ZoneOffset.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, ZoneOffset.class), StringConversions::toZoneOffset);
        CONVERSION_DB.put(pair(Map.class, ZoneOffset.class), MapConversions::toZoneOffset);
        CONVERSION_DB.put(pair(ZoneId.class, ZoneOffset.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(TimeZone.class, ZoneOffset.class), UNSUPPORTED);

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
        CONVERSION_DB.put(pair(Map.class, StringBuffer.class), MapConversions::toStringBuffer);

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
        CONVERSION_DB.put(pair(Map.class, StringBuilder.class), MapConversions::toStringBuilder);

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
        CONVERSION_DB.put(pair(String.class, char[].class), StringConversions::toCharArray);
        CONVERSION_DB.put(pair(StringBuilder.class, char[].class), StringConversions::toCharArray);
        CONVERSION_DB.put(pair(StringBuffer.class, char[].class), StringConversions::toCharArray);
        CONVERSION_DB.put(pair(ByteBuffer.class, char[].class), ByteBufferConversions::toCharArray);
        CONVERSION_DB.put(pair(CharBuffer.class, char[].class), CharBufferConversions::toCharArray);
        CONVERSION_DB.put(pair(char[].class, char[].class), CharArrayConversions::toCharArray);
        CONVERSION_DB.put(pair(byte[].class, char[].class), ByteArrayConversions::toCharArray);

        // toCharacterArray
        CONVERSION_DB.put(pair(Void.class, Character[].class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, Character[].class), StringConversions::toCharacterArray);
        CONVERSION_DB.put(pair(StringBuffer.class, Character[].class), StringConversions::toCharacterArray);
        CONVERSION_DB.put(pair(StringBuilder.class, Character[].class), StringConversions::toCharacterArray);

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
        CONVERSION_DB.put(pair(Short.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(Integer.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(Long.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(Float.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(Double.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(AtomicInteger.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(AtomicLong.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(BigInteger.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(BigDecimal.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(String.class, Year.class), StringConversions::toYear);
        CONVERSION_DB.put(pair(Map.class, Year.class), MapConversions::toYear);

        // Throwable conversions supported
        CONVERSION_DB.put(pair(Void.class, Throwable.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Map.class, Throwable.class), (ConvertWithTarget<Throwable>) MapConversions::toThrowable);

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
        CONVERSION_DB.put(pair(Date.class, Map.class), DateConversions::toMap);
        CONVERSION_DB.put(pair(java.sql.Date.class, Map.class), DateConversions::toMap);
        CONVERSION_DB.put(pair(Timestamp.class, Map.class), TimestampConversions::toMap);
        CONVERSION_DB.put(pair(LocalDate.class, Map.class), LocalDateConversions::toMap);
        CONVERSION_DB.put(pair(LocalDateTime.class, Map.class), LocalDateTimeConversions::toMap);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Map.class), ZonedDateTimeConversions::toMap);
        CONVERSION_DB.put(pair(Duration.class, Map.class), DurationConversions::toMap);
        CONVERSION_DB.put(pair(Instant.class, Map.class), InstantConversions::toMap);
        CONVERSION_DB.put(pair(LocalTime.class, Map.class), LocalTimeConversions::toMap);
        CONVERSION_DB.put(pair(MonthDay.class, Map.class), MonthDayConversions::toMap);
        CONVERSION_DB.put(pair(YearMonth.class, Map.class), YearMonthConversions::toMap);
        CONVERSION_DB.put(pair(Period.class, Map.class), PeriodConversions::toMap);
        CONVERSION_DB.put(pair(TimeZone.class, Map.class), TimeZoneConversions::toMap);
        CONVERSION_DB.put(pair(ZoneId.class, Map.class), ZoneIdConversions::toMap);
        CONVERSION_DB.put(pair(ZoneOffset.class, Map.class), ZoneOffsetConversions::toMap);
        CONVERSION_DB.put(pair(Class.class, Map.class), MapConversions::initMap);
        CONVERSION_DB.put(pair(UUID.class, Map.class), UUIDConversions::toMap);
        CONVERSION_DB.put(pair(Calendar.class, Map.class), CalendarConversions::toMap);
        CONVERSION_DB.put(pair(Map.class, Map.class), UNSUPPORTED);
        CONVERSION_DB.put(pair(Enum.class, Map.class), EnumConversions::toMap);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Map.class), OffsetDateTimeConversions::toMap);
        CONVERSION_DB.put(pair(OffsetTime.class, Map.class), OffsetTimeConversions::toMap);
        CONVERSION_DB.put(pair(Year.class, Map.class), YearConversions::toMap);
        CONVERSION_DB.put(pair(Locale.class, Map.class), LocaleConversions::toMap);
        CONVERSION_DB.put(pair(URI.class, Map.class), UriConversions::toMap);
        CONVERSION_DB.put(pair(URL.class, Map.class), UrlConversions::toMap);
        CONVERSION_DB.put(pair(Throwable.class, Map.class), ThrowableConversions::toMap);
    }

    /**
     * Constructs a new Converter instance with the specified options.
     * <p>
     * The Converter initializes its internal conversion databases by merging the predefined
     * {@link #CONVERSION_DB} with any user-specified overrides provided in {@code options}.
     * </p>
     *
     * @param options The {@link ConverterOptions} that configure this Converter's behavior and conversions.
     * @throws NullPointerException if {@code options} is {@code null}.
     */
    public Converter(ConverterOptions options) {
        this.options = options;
        USER_DB.putAll(this.options.getConverterOverrides());
    }

    /**
     * Converts the given source object to the specified target type.
     * <p>
     * The {@code convert} method serves as the primary API for transforming objects between various types.
     * It supports a wide range of conversions, including primitive types, wrapper classes, numeric types,
     * date and time classes, collections, and custom objects. Additionally, it allows for extensibility
     * by enabling the registration of custom converters.
     * </p>
     * <p>
     * <strong>Key Features:</strong>
     * <ul>
     *     <li><b>Wide Range of Supported Types:</b> Supports conversion between Java primitives, their corresponding
     *         wrapper classes, {@link Number} subclasses, date and time classes (e.g., {@link Date}, {@link LocalDateTime}),
     *         collections (e.g., {@link List}, {@link Set}, {@link Map}), {@link UUID}, and more.</li>
     *     <li><b>Null Handling:</b> Gracefully handles {@code null} inputs by returning {@code null} for object types,
     *         default primitive values (e.g., 0 for numeric types, {@code false} for boolean), and default characters.</li>
     *     <li><b>Inheritance-Based Conversions:</b> Automatically considers superclass and interface hierarchies
     *         to find the most suitable converter when a direct conversion is not available.</li>
     *     <li><b>Custom Converters:</b> Allows users to register custom conversion logic for specific source-target type pairs
     *         using the {@link #addConversion(Class, Class, Convert)} method.</li>
     *     <li><b>Thread-Safe:</b> Designed to be thread-safe, allowing concurrent conversions without compromising data integrity.</li>
     * </ul>
     * </p>
     *
     * <h3>Usage Examples:</h3>
     * <pre>{@code
     *     ConverterOptions options = new ConverterOptions();
     *     Converter converter = new Converter(options);
     *
     *     // Example 1: Convert String to Integer
     *     String numberStr = "123";
     *     Integer number = converter.convert(numberStr, Integer.class);
     *     System.out.println("Converted Integer: " + number); // Output: Converted Integer: 123
     *
     *     // Example 2: Convert String to Date
     *     String dateStr = "2024-04-27";
     *     LocalDate date = converter.convert(dateStr, LocalDate.class);
     *     System.out.println("Converted Date: " + date); // Output: Converted Date: 2024-04-27
     *
     *     // Example 3: Convert Enum to String
     *     Day day = Day.MONDAY;
     *     String dayStr = converter.convert(day, String.class);
     *     System.out.println("Converted Day: " + dayStr); // Output: Converted Day: MONDAY
     *
     *     // Example 4: Convert Array to List
     *     String[] stringArray = {"apple", "banana", "cherry"};
     *     List<String> stringList = converter.convert(stringArray, List.class);
     *     System.out.println("Converted List: " + stringList); // Output: Converted List: [apple, banana, cherry]
     *
     *     // Example 5: Convert Map to UUID
     *     Map<String, Object> uuidMap = Map.of("mostSigBits", 123456789L, "leastSigBits", 987654321L);
     *     UUID uuid = converter.convert(uuidMap, UUID.class);
     *     System.out.println("Converted UUID: " + uuid); // Output: Converted UUID: 00000000-075b-cd15-0000-0000003ade68
     *
     *     // Example 6: Convert Object[], String[], Collection, and primitive Arrays to EnumSet
     *     Object[] array = {Day.MONDAY, Day.WEDNESDAY, "FRIDAY", 4};
     *     EnumSet<Day> daySet = (EnumSet<Day>)(Object)converter.convert(array, Day.class);
     *
     *     Enum, String, and Number value in the source collection/array is properly converted
     *     to the correct Enum type and added to the returned EnumSet. Null values inside the
     *     source (Object[], Collection) are skipped.
     *
     *     When converting arrays or collections to EnumSet, you must use a double cast due to Java's
     *     type system and generic type erasure. The cast is safe as the converter guarantees return of
     *     an EnumSet when converting arrays/collections to enum types.
     *
     *     // Example 7: Register and Use a Custom Converter
     *     // Custom converter to convert String to CustomType
     *     converter.addConversion(String.class, CustomType.class, (from, conv) -> new CustomType(from));
     *
     *     String customStr = "customValue";
     *     CustomType custom = converter.convert(customStr, CustomType.class);
     *     System.out.println("Converted CustomType: " + custom); // Output: Converted CustomType: CustomType{value='customValue'}
     * }
     * </pre>
     *
     * <h3>Parameter Descriptions:</h3>
     * <ul>
     *     <li><b>from:</b> The source object to be converted. This can be any object, including {@code null}.
     *         The actual type of {@code from} does not need to match the target type; the Converter will attempt to
     *         perform the necessary transformation.</li>
     *     <li><b>toType:</b> The target class to which the source object should be converted. This parameter
     *         specifies the desired output type. It can be a primitive type (e.g., {@code int.class}), a wrapper class
     *         (e.g., {@link Integer}.class), or any other supported class.</li>
     * </ul>
     *
     * <h3>Return Value:</h3>
     * <p>
     * Returns an instance of the specified target type {@code toType}, representing the converted value of the source object {@code from}.
     * If {@code from} is {@code null}, the method returns:
     * <ul>
     *     <li>{@code null} for non-primitive target types.</li>
     *     <li>Default primitive values for primitive target types (e.g., 0 for numeric types, {@code false} for {@code boolean}, '\u0000' for {@code char}).</li>
     * </ul>
     * </p>
     *
     * <h3>Exceptions:</h3>
     * <ul>
     *     <li><b>IllegalArgumentException:</b> Thrown if the conversion from the source type to the target type is not supported,
     *         or if the target type {@code toType} is {@code null}.</li>
     *     <li><b>RuntimeException:</b> Any underlying exception thrown during the conversion process is propagated as a {@code RuntimeException}.</li>
     * </ul>
     *
     * <h3>Supported Conversions:</h3>
     * <p>
     * The Converter supports a vast array of conversions, including but not limited to:
     * <ul>
     *     <li><b>Primitives and Wrappers:</b> Convert between Java primitive types (e.g., {@code int}, {@code boolean}) and their corresponding wrapper classes (e.g., {@link Integer}, {@link Boolean}).</li>
     *     <li><b>Numbers:</b> Convert between different numeric types (e.g., {@link Integer} to {@link Double}, {@link BigInteger} to {@link BigDecimal}).</li>
     *     <li><b>Date and Time:</b> Convert between various date and time classes (e.g., {@link String} to {@link LocalDate}, {@link Date} to {@link Instant}, {@link Calendar} to {@link ZonedDateTime}).</li>
     *     <li><b>Collections:</b> Convert between different collection types (e.g., arrays to {@link List}, {@link Set} to {@link Map}, {@link StringBuilder} to {@link String}).</li>
     *     <li><b>Custom Objects:</b> Convert between complex objects (e.g., {@link UUID} to {@link Map}, {@link Class} to {@link String}, custom types via user-defined converters).</li>
     *     <li><b>Buffer Types:</b> Convert between buffer types (e.g., {@link ByteBuffer} to {@link String}, {@link CharBuffer} to {@link byte}[]).</li>
     * </ul>
     * </p>
     *
     * <h3>Extensibility:</h3>
     * <p>
     * Users can extend the Converter's capabilities by registering custom converters for specific type pairs.
     * This is accomplished using the {@link #addConversion(Class, Class, Convert)} method, which accepts the source type,
     * target type, and a {@link Convert} functional interface implementation that defines the conversion logic.
     * </p>
     *
     * <h3>Performance Considerations:</h3>
     * <p>
     * The Converter utilizes caching mechanisms to store and retrieve converters, ensuring efficient performance
     * even with a large number of conversion operations. However, registering an excessive number of custom converters
     * may impact memory usage. It is recommended to register only necessary converters to maintain optimal performance.
     * </p>
     *
     * @param from   The source object to be converted. Can be any object, including {@code null}.
     * @param toType The target class to which the source object should be converted. Must not be {@code null}.
     * @param <T>    The type of the target object.
     * @return An instance of {@code toType} representing the converted value of {@code from}.
     * @throws IllegalArgumentException if {@code toType} is {@code null} or if the conversion is not supported.
     * @see #getSupportedConversions()
     * @see #addConversion(Class, Class, Convert)
     */
    @SuppressWarnings("unchecked")
    public <T> T convert(Object from, Class<T> toType) {
        if (toType == null) {
            throw new IllegalArgumentException("toType cannot be null");
        }
        Class<?> sourceType;
        if (from == null) {
            // Allow for primitives to support convert(null, int.class) return 0, or convert(null, boolean.class) return false
            sourceType = Void.class;
        } else {
            sourceType = from.getClass();
            if (toType.isPrimitive()) {
                // Eliminates need to define the primitives in the CONVERSION_DB table (would add hundreds of entries)
                toType = (Class<T>) ClassUtilities.toPrimitiveWrapperClass(toType);
            }

            // Try collection conversion first
            T result = attemptCollectionConversion(from, sourceType, toType);
            if (result != null) {
                return result;
            }
        }

        // Check user added conversions (allows overriding factory conversions)
        ConversionPair key = pair(sourceType, toType);
        Convert<?> converter = USER_DB.get(key);
        if (converter != null && converter != UNSUPPORTED) {
            return (T) converter.convert(from, this, toType);
        }

        // Check factory conversion database
        converter = CONVERSION_DB.get(key);
        if (converter != null && converter != UNSUPPORTED) {
            return (T) converter.convert(from, this, toType);
        }

        if (EnumSet.class.isAssignableFrom(toType)) {
            throw new IllegalArgumentException("To convert to EnumSet, specify the Enum class to convert to. See convert() Javadoc for example.");
        }

        // Always attempt inheritance-based conversion
        converter = getInheritedConverter(sourceType, toType);
        if (converter != null && converter != UNSUPPORTED) {
            // Fast lookup next time.
            if (!isDirectConversionSupported(sourceType, toType)) {
                addConversion(sourceType, toType, converter);
            }
            return (T) converter.convert(from, this, toType);
        }

        throw new IllegalArgumentException("Unsupported conversion, source type [" + name(from) + "] target type '" + getShortName(toType) + "'");
    }

    @SuppressWarnings("unchecked")
    private <T> T attemptCollectionConversion(Object from, Class<?> sourceType, Class<T> toType) {
        // First validate source type is actually a collection/array type
        if (!(from == null || from.getClass().isArray() || from instanceof Collection)) {
            return null;
        }

        // Check for EnumSet target first
        if (EnumSet.class.isAssignableFrom(toType)) {
            throw new IllegalArgumentException("To convert to EnumSet, specify the Enum class to convert to as the 'toType.' Example: EnumSet<Day> daySet = (EnumSet<Day>)(Object)converter.convert(array, Day.class);");
        }

        // Special handling for Collection/Array/EnumSet conversions
        if (toType.isEnum()) {
            // When target is something like Day.class, we're actually creating an EnumSet<Day>
            if (sourceType.isArray() || Collection.class.isAssignableFrom(sourceType)) {
                return (T) EnumConversions.toEnumSet(from, this, toType);
            }
        } else if (EnumSet.class.isAssignableFrom(sourceType)) {
            if (Collection.class.isAssignableFrom(toType)) {
                Collection<Object> target = (Collection<Object>) CollectionHandling.createCollection(from, toType);
                target.addAll((Collection<?>) from);
                return (T) target;
            }
            if (toType.isArray()) {
                return (T) ArrayConversions.enumSetToArray((EnumSet<?>) from, toType);
            }
        } else if (Collection.class.isAssignableFrom(sourceType)) {
            if (toType.isArray()) {
                return (T) ArrayConversions.collectionToArray((Collection<?>) from, toType, this);
            } else if (Collection.class.isAssignableFrom(toType)) {
                return (T) CollectionConversions.collectionToCollection((Collection<?>) from, toType);
            }
        } else if (sourceType.isArray()) {
            if (Collection.class.isAssignableFrom(toType)) {
                return (T) CollectionConversions.arrayToCollection(from, toType);
            } else if (toType.isArray() && !sourceType.getComponentType().equals(toType.getComponentType())) {
                // Handle array-to-array conversion when component types differ
                return (T) ArrayConversions.arrayToArray(from, toType, this);
            }
        }

        return null;
    }

    /**
     * Retrieves the most suitable converter for converting from the specified source type to the desired target type.
     * This method searches through the class hierarchies of both source and target types to find the best matching
     * conversion, prioritizing matches in the following order:
     *
     * <ol>
     *   <li>Exact match to requested target type</li>
     *   <li>Most specific target type when considering inheritance (e.g., java.sql.Date over java.util.Date)</li>
     *   <li>Shortest combined inheritance distance from source and target types</li>
     *   <li>Concrete classes over interfaces at the same inheritance level</li>
     * </ol>
     *
     * <p>The method first checks user-defined conversions ({@code USER_DB}) before falling back to built-in
     * conversions ({@code CONVERSION_DB}). Class hierarchies are cached to improve performance of repeated lookups.</p>
     *
     * <p>For example, when converting to java.sql.Date, a converter to java.sql.Date will be chosen over a converter
     * to its parent class java.util.Date, even if the java.util.Date converter is closer in the source type's hierarchy.</p>
     *
     * @param sourceType The source type to convert from
     * @param toType The target type to convert to
     * @return A {@link Convert} instance for the most appropriate conversion, or {@code null} if no suitable converter is found
     */
    private Convert<?> getInheritedConverter(Class<?> sourceType, Class<?> toType) {
        Set<ClassLevel> sourceTypes = new TreeSet<>(getSuperClassesAndInterfaces(sourceType));
        sourceTypes.add(new ClassLevel(sourceType, 0));
        Set<ClassLevel> targetTypes = new TreeSet<>(getSuperClassesAndInterfaces(toType));
        targetTypes.add(new ClassLevel(toType, 0));

        // Create pairs of source/target types with their levels
        final class ConversionPairWithLevel {
            private final ConversionPair pair;
            private final int sourceLevel;
            private final int targetLevel;

            private ConversionPairWithLevel(Class<?> source, Class<?> target, int sourceLevel, int targetLevel) {
                this.pair = new ConversionPair(source, target);
                this.sourceLevel = sourceLevel;
                this.targetLevel = targetLevel;
            }
        }

        List<ConversionPairWithLevel> pairs = new ArrayList<>();
        for (ClassLevel source : sourceTypes) {
            for (ClassLevel target : targetTypes) {
                pairs.add(new ConversionPairWithLevel(source.clazz, target.clazz, source.level, target.level));
            }
        }

        // Sort pairs by combined inheritance distance with type safety priority
        pairs.sort((p1, p2) -> {
            // First prioritize exact target type matches
            boolean p1ExactTarget = p1.pair.getTarget() == toType;
            boolean p2ExactTarget = p2.pair.getTarget() == toType;
            if (p1ExactTarget != p2ExactTarget) {
                return p1ExactTarget ? -1 : 1;
            }

            // Then check assignability to target type if different
            if (p1.pair.getTarget() != p2.pair.getTarget()) {
                boolean p1AssignableToP2 = p2.pair.getTarget().isAssignableFrom(p1.pair.getTarget());
                boolean p2AssignableToP1 = p1.pair.getTarget().isAssignableFrom(p2.pair.getTarget());
                if (p1AssignableToP2 != p2AssignableToP1) {
                    return p1AssignableToP2 ? -1 : 1;
                }
            }

            // Then consider inheritance distance
            int dist1 = p1.sourceLevel + p1.targetLevel;
            int dist2 = p2.sourceLevel + p2.targetLevel;
            if (dist1 != dist2) {
                return dist1 - dist2;
            }

            // Finally prefer concrete classes over interfaces
            boolean p1FromInterface = p1.pair.getSource().isInterface();
            boolean p2FromInterface = p2.pair.getSource().isInterface();
            if (p1FromInterface != p2FromInterface) {
                return p1FromInterface ? 1 : -1;
            }
            boolean p1ToInterface = p1.pair.getTarget().isInterface();
            boolean p2ToInterface = p2.pair.getTarget().isInterface();
            if (p1ToInterface != p2ToInterface) {
                return p1ToInterface ? 1 : -1;
            }
            return 0;
        });

        // Check pairs in sorted order
        for (ConversionPairWithLevel pairWithLevel : pairs) {
            // Check USER_DB first
            Convert<?> tempConverter = USER_DB.get(pairWithLevel.pair);
            if (tempConverter != null) {
                return tempConverter;
            }

            tempConverter = CONVERSION_DB.get(pairWithLevel.pair);
            if (tempConverter != null) {
                return tempConverter;
            }
        }

        return null;
    }
    
    /**
     * Retrieves all superclasses and interfaces of the specified class, excluding general marker interfaces.
     * <p>
     * This method utilizes caching to improve performance by storing previously computed class hierarchies.
     * </p>
     *
     * @param clazz The class for which to retrieve superclasses and interfaces.
     * @return A {@link Set} of {@link ClassLevel} instances representing the superclasses and interfaces of the specified class.
     */
    private static Set<ClassLevel> getSuperClassesAndInterfaces(Class<?> clazz) {
        SortedSet<ClassLevel> parentTypes = cacheParentTypes.get(clazz);
        if (parentTypes != null) {
            return parentTypes;
        }
        parentTypes = new TreeSet<>();
        addSuperClassesAndInterfaces(clazz, parentTypes, 1);
        cacheParentTypes.put(clazz, parentTypes);
        return parentTypes;
    }

    /**
     * Represents a class along with its hierarchy level for ordering purposes.
     * <p>
     * This class is used internally to manage and compare classes based on their position within the class hierarchy.
     * </p>
     */
    static class ClassLevel implements Comparable<ClassLevel> {
        private final Class<?> clazz;
        private final int level;
        private final boolean isInterface;

        ClassLevel(Class<?> c, int level) {
            clazz = c;
            this.level = level;
            isInterface = c.isInterface();
        }

        @Override
        public int compareTo(ClassLevel other) {
            // Primary sort key: level (ascending)
            int levelComparison = Integer.compare(this.level, other.level);
            if (levelComparison != 0) {
                return levelComparison;
            }

            // Secondary sort key: concrete class before interface
            if (isInterface && !other.isInterface) {
                return 1;
            }
            if (!isInterface && other.isInterface) {
                return -1;
            }

            // Tertiary sort key: alphabetical order (for determinism)
            return this.clazz.getName().compareTo(other.clazz.getName());
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ClassLevel)) return false;
            ClassLevel other = (ClassLevel) obj;
            return this.clazz.equals(other.clazz) && this.level == other.level;
        }

        @Override
        public int hashCode() {
            return clazz.hashCode() * 31 + level;
        }
    }

    /**
     * Recursively adds all superclasses and interfaces of the specified class to the result set.
     * <p>
     * This method excludes general marker interfaces such as {@link Serializable}, {@link Cloneable}, and {@link Comparable}
     * to prevent unnecessary or irrelevant conversions.
     * </p>
     *
     * @param clazz  The class whose superclasses and interfaces are to be added.
     * @param result The set where the superclasses and interfaces are collected.
     * @param level  The current hierarchy level, used for ordering purposes.
     */
    private static void addSuperClassesAndInterfaces(Class<?> clazz, SortedSet<ClassLevel> result, int level) {
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

    /**
     * Returns a short name for the given class.
     * <ul>
     * <li>For specific array types, returns the custom name</li>
     * <li>For other array types, returns the component's simple name + "[]"</li>
     * <li>For java.sql.Date, returns the fully qualified name</li>
     * <li>For all other classes, returns the simple name</li>
     * </ul>
     *
     * @param type  The class to get the short name for
     * @return      The short name of the class
     */
    static String getShortName(Class<?> type) {
        if (type.isArray()) {
            // Check if the array type has a custom short name
            String customName = CUSTOM_ARRAY_NAMES.get(type);
            if (customName != null) {
                return customName;
            }
            // For other arrays, use component's simple name + "[]"
            Class<?> componentType = type.getComponentType();
            return componentType.getSimpleName() + "[]";
        }
        // Special handling for java.sql.Date
        if (java.sql.Date.class.equals(type)) {
            return type.getName();
        }
        // Default: use simple name
        return type.getSimpleName();
    }

    /**
     * Generates a descriptive name for the given object.
     * <p>
     * If the object is {@code null}, returns "null". Otherwise, returns a string combining the short name
     * of the object's class and its {@code toString()} representation.
     * </p>
     *
     * @param from The object for which to generate a name.
     * @return A descriptive name of the object.
     */
    static private String name(Object from) {
        if (from == null) {
            return "null";
        }
        return getShortName(from.getClass()) + " (" + from + ")";
    }

    /**
     * Determines if a collection-based conversion is supported between the specified source and target types.
     * This method checks for valid conversions between arrays, collections, and EnumSets without actually
     * performing the conversion.
     *
     * <p>Supported conversions include:
     * <ul>
     *   <li>Array to Collection</li>
     *   <li>Collection to Array</li>
     *   <li>Array to Array (when component types differ)</li>
     *   <li>Array or Collection to EnumSet (when target is an Enum type)</li>
     *   <li>EnumSet to Array or Collection</li>
     * </ul>
     * </p>
     *
     * @param sourceType The source type to convert from
     * @param target The target type to convert to
     * @return true if a collection-based conversion is supported between the types, false otherwise
     * @throws IllegalArgumentException if target is EnumSet.class (caller should specify specific Enum type instead)
     */
    public boolean isCollectionConversionSupported(Class<?> sourceType, Class<?> target) {
        // Quick check: If the source is not an array, a Collection, or an EnumSet, no conversion is supported here.
        if (!(sourceType.isArray() || Collection.class.isAssignableFrom(sourceType) || EnumSet.class.isAssignableFrom(sourceType))) {
            return false;
        }

        // Target is EnumSet: We cannot directly determine the target Enum type here.
        // The caller should specify the Enum type (e.g. "Day.class") instead of EnumSet.
        if (EnumSet.class.isAssignableFrom(target)) {
            throw new IllegalArgumentException(
                    "To convert to EnumSet, specify the Enum class to convert to as the 'toType.' " +
                            "Example: EnumSet<Day> daySet = (EnumSet<Day>)(Object)converter.convert(array, Day.class);"
            );
        }

        // If the target type is an Enum, then we're essentially looking to create an EnumSet<EnumType>.
        // For that, the source must be either an array or a collection from which we can build the EnumSet.
        if (target.isEnum()) {
            return (sourceType.isArray() || Collection.class.isAssignableFrom(sourceType));
        }

        // If the source is an EnumSet, it can be converted to either an array or another collection.
        if (EnumSet.class.isAssignableFrom(sourceType)) {
            return target.isArray() || Collection.class.isAssignableFrom(target);
        }

        // If the source is a generic Collection, we only support converting it to an array type.
        if (Collection.class.isAssignableFrom(sourceType)) {
            return target.isArray();
        }

        // If the source is an array:
        // 1. If the target is a Collection, we can always convert.
        // 2. If the target is another array, we must verify that component types differ,
        //    otherwise it's just a no-op (the caller might be expecting a conversion).
        if (sourceType.isArray()) {
            if (Collection.class.isAssignableFrom(target)) {
                return true;
            } else {
                return target.isArray() && !sourceType.getComponentType().equals(target.getComponentType());
            }
        }

        // Fallback: Shouldn't reach here given the initial conditions.
        return false;
    }

    /**
     * Determines whether a conversion from the specified source type to the target type is supported,
     * excluding any conversions involving arrays or collections.
     *
     * <p>The method is particularly useful when you need to verify that a conversion is possible
     * between simple types without considering array or collection conversions. This can be helpful
     * in scenarios where you need to validate component type conversions separately from their
     * container types.</p>
     *
     * <p><strong>Example usage:</strong></p>
     * <pre>{@code
     * Converter converter = new Converter(options);
     *
     * // Check if String can be converted to Integer
     * boolean canConvert = converter.isNonCollectionConversionSupportedFor(
     *     String.class, Integer.class);  // returns true
     *
     * // Check array conversion (always returns false)
     * boolean arrayConvert = converter.isNonCollectionConversionSupportedFor(
     *     String[].class, Integer[].class);  // returns false
     *
     * // Check collection conversion (always returns false)
     * boolean listConvert = converter.isNonCollectionConversionSupportedFor(
     *     List.class, Set.class);  // returns false
     * }</pre>
     *
     * @param source The source class type to check
     * @param target The target class type to check
     * @return {@code true} if a non-collection conversion exists between the types,
     *         {@code false} if either type is an array/collection or no conversion exists
     * @see #isConversionSupportedFor(Class, Class)
     * @see #isDirectConversionSupported(Class, Class)
     */
    public boolean isSimpleTypeConversionSupported(Class<?> source, Class<?> target) {
        // Check both source and target for array/collection types
        if (source.isArray() || Collection.class.isAssignableFrom(source) ||
                target.isArray() || Collection.class.isAssignableFrom(target)) {
            return false;
        }

        // Direct conversion check first (fastest)
        if (isConversionInMap(source, target)) {
            return true;
        }

        // Check inheritance-based conversions
        Convert<?> method = getInheritedConverter(source, target);
        return method != null && method != UNSUPPORTED;
    }
    
    /**
     * Determines whether a direct conversion from the specified source type to the target type is supported,
     * without considering inheritance hierarchies. For array-to-array conversions, verifies that both array
     * conversion and component type conversions are directly supported.
     *
     * <p>The method checks:</p>
     * <ol>
     *   <li>User-defined and built-in direct conversions</li>
     *   <li>Collection/Array/EnumSet conversions - for array-to-array conversions, also verifies
     *       that component type conversions are directly supported</li>
     * </ol>
     *
     * <p>For array conversions, performs a deep check to ensure both the array types and their
     * component types can be converted directly. For example, when checking if a String[] can be
     * converted to Integer[], verifies both:</p>
     * <ul>
     *   <li>That array-to-array conversion is supported</li>
     *   <li>That String-to-Integer conversion is directly supported</li>
     * </ul>
     *
     * @param source The source class type
     * @param target The target class type
     * @return {@code true} if a direct conversion exists (including component type conversions for arrays),
     *         {@code false} otherwise
     */
    public boolean isDirectConversionSupported(Class<?> source, Class<?> target) {
        // First check if there's a direct conversion defined in the maps
        if (isConversionInMap(source, target)) {
            return true;
        }
        // If not found in the maps, check if collection/array/enum set conversions are possible
        if (isCollectionConversionSupported(source, target)) {
            // For array-to-array conversions, verify we can convert the component types
            if (source.isArray() && target.isArray()) {
                return isDirectConversionSupported(source.getComponentType(), target.getComponentType());
            }
            return true;
        }
        return false;
    }
    
    /**
     * Determines whether a conversion from the specified source type to the target type is supported.
     * For array-to-array conversions, this method verifies that both array conversion and component type
     * conversions are supported.
     *
     * <p>The method checks three paths for conversion support:</p>
     * <ol>
     *   <li>Direct conversions as defined in the conversion maps</li>
     *   <li>Collection/Array/EnumSet conversions - for array-to-array conversions, also verifies
     *       that component type conversions are supported</li>
     *   <li>Inherited conversions (via superclasses and implemented interfaces)</li>
     * </ol>
     *
     * <p>For array conversions, this method performs a deep check to ensure both the array types
     * and their component types can be converted. For example, when checking if a String[] can be
     * converted to Integer[], it verifies both:</p>
     * <ul>
     *   <li>That array-to-array conversion is supported</li>
     *   <li>That String-to-Integer conversion is supported for the components</li>
     * </ul>
     *
     * @param source The source class type
     * @param target The target class type
     * @return true if the conversion is fully supported (including component type conversions for arrays),
     *         false otherwise
     */
    public boolean isConversionSupportedFor(Class<?> source, Class<?> target) {
        // Try simple type conversion first
        if (isConversionInMap(source, target)) {
            return true;
        }

        // Handle collection/array conversions
        if (isCollectionConversionSupported(source, target)) {
            // Only need special handling for array-to-array conversions
            if (source.isArray() && target.isArray()) {
                return target.getComponentType() == Object.class ||
                        isConversionSupportedFor(source.getComponentType(), target.getComponentType());
            }
            return true;  // All other collection conversions are supported
        }

        // Try inheritance-based conversion as last resort
        Convert<?> method = getInheritedConverter(source, target);
        return method != null && method != UNSUPPORTED;
    }
    
    /**
     * Private helper method to check if a conversion exists directly in USER_DB or CONVERSION_DB.
     *
     * @param source Class of source type.
     * @param target Class of target type.
     * @return boolean true if a direct conversion exists, false otherwise.
     */
    private boolean isConversionInMap(Class<?> source, Class<?> target) {
        source = ClassUtilities.toPrimitiveWrapperClass(source);
        target = ClassUtilities.toPrimitiveWrapperClass(target);
        ConversionPair key = pair(source, target);
        Convert<?> method = USER_DB.get(key);
        if (method != null && method != UNSUPPORTED) {
            return true;
        }
        method = CONVERSION_DB.get(key);
        return method != null && method != UNSUPPORTED;
    }

    /**
     * Retrieves a map of all supported conversions, categorized by source and target classes.
     * <p>
     * The returned map's keys are source classes, and each key maps to a {@code Set} of target classes
     * that the source can be converted to.
     * </p>
     *
     * @return A {@code Map<Class<?>, Set<Class<?>>>} representing all supported conversions.
     */
    public Map<Class<?>, Set<Class<?>>> allSupportedConversions() {
        Map<Class<?>, Set<Class<?>>> toFrom = new TreeMap<>(Comparator.comparing(Class::getName));
        addSupportedConversion(CONVERSION_DB, toFrom);
        addSupportedConversion(USER_DB, toFrom);
        return toFrom;
    }

    /**
     * Retrieves a map of all supported conversions with class names instead of class objects.
     * <p>
     * The returned map's keys are source class names, and each key maps to a {@code Set} of target class names
     * that the source can be converted to.
     * </p>
     *
     * @return A {@code Map<String, Set<String>>} representing all supported conversions by class names.
     */
    public Map<String, Set<String>> getSupportedConversions() {
        Map<String, Set<String>> toFrom = new TreeMap<>(String::compareTo);
        addSupportedConversionName(CONVERSION_DB, toFrom);
        addSupportedConversionName(USER_DB, toFrom);
        return toFrom;
    }

    /**
     * Populates the provided map with supported conversions from the specified conversion database.
     *
     * @param db      The conversion database containing conversion mappings.
     * @param toFrom  The map to populate with supported conversions.
     */
    private static void addSupportedConversion(Map<ConversionPair, Convert<?>> db, Map<Class<?>, Set<Class<?>>> toFrom) {
        for (Map.Entry<ConversionPair, Convert<?>> entry : db.entrySet()) {
            if (entry.getValue() != UNSUPPORTED) {
                ConversionPair pair = entry.getKey();
                toFrom.computeIfAbsent(pair.getSource(), k -> new TreeSet<>(Comparator.comparing((Class<?> c) -> c.getName()))).add(pair.getTarget());
            }
        }
    }

    /**
     * Populates the provided map with supported conversions from the specified conversion database, using class names.
     *
     * @param db      The conversion database containing conversion mappings.
     * @param toFrom  The map to populate with supported conversions by class names.
     */
    private static void addSupportedConversionName(Map<ConversionPair, Convert<?>> db, Map<String, Set<String>> toFrom) {
        for (Map.Entry<ConversionPair, Convert<?>> entry : db.entrySet()) {
            if (entry.getValue() != UNSUPPORTED) {
                ConversionPair pair = entry.getKey();
                toFrom.computeIfAbsent(getShortName(pair.getSource()), k -> new TreeSet<>(String::compareTo)).add(getShortName(pair.getTarget()));
            }
        }
    }

    /**
     * Adds a new conversion function for converting from one type to another. If a conversion already exists
     * for the specified source and target types, the existing conversion will be overwritten.
     *
     * <p>When {@code convert(source, target)} is called, the conversion function is located by matching the class
     * of the source instance and the target class. If an exact match is found, that conversion function is used.
     * If no exact match is found, the method attempts to find the most appropriate conversion by traversing
     * the class hierarchy of the source and target types (including interfaces), excluding common marker
     * interfaces such as {@link java.io.Serializable}, {@link java.lang.Comparable}, and {@link java.lang.Cloneable}.
     * The nearest match based on class inheritance and interface implementation is used.
     *
     * <p>This method allows you to explicitly define custom conversions between types. It also supports the automatic
     * handling of primitive types by converting them to their corresponding wrapper types (e.g., {@code int} to {@code Integer}).
     *
     * <p><strong>Note:</strong> This method utilizes the {@link ClassUtilities#toPrimitiveWrapperClass(Class)} utility
     * to ensure that primitive types are mapped to their respective wrapper classes before attempting to locate
     * or store the conversion.
     *
     * @param source             The source class (type) to convert from.
     * @param target             The target class (type) to convert to.
     * @param conversionFunction A function that converts an instance of the source type to an instance of the target type.
     * @return The previous conversion function associated with the source and target types, or {@code null} if no conversion existed.
     */
    public Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionFunction) {
        source = ClassUtilities.toPrimitiveWrapperClass(source);
        target = ClassUtilities.toPrimitiveWrapperClass(target);
        return USER_DB.put(pair(source, target), conversionFunction);
    }
    
    /**
     * Performs an identity conversion, returning the source object as-is.
     *
     * @param from      The source object.
     * @param converter The Converter instance performing the conversion.
     * @param <T>       The type of the source and target object.
     * @return The source object unchanged.
     */
    public static <T> T identity(T from, Converter converter) {
        return from;
    }

    /**
     * Handles unsupported conversions by returning {@code null}.
     *
     * @param from      The source object.
     * @param converter The Converter instance performing the conversion.
     * @param <T>       The type of the source and target object.
     * @return {@code null} indicating the conversion is unsupported.
     */
    private static <T> T unsupported(T from, Converter converter) {
        return null;
    }
}