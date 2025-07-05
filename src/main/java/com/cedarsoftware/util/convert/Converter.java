package com.cedarsoftware.util.convert;

import java.awt.*;
import java.io.Externalizable;
import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.nio.file.Path;
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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.regex.Pattern;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.ClassValueMap;

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
 * <p>
 * <strong>Module Dependencies:</strong>
 * </p>
 * <ul>
 *   <li>
 *     <b>SQL support:</b> Conversions involving {@code java.sql.Date} and {@code java.sql.Timestamp} require
 *     the {@code java.sql} module to be present at runtime. If you're using OSGi, ensure your bundle imports
 *     the {@code java.sql} package or declare it as an optional import if SQL support is not required.
 *   </li>
 *   <li>
 *     <b>XML support:</b> This library does not directly use XML classes, but {@link com.cedarsoftware.util.IOUtilities}
 *     provides XML stream support that requires the {@code java.xml} module. See {@link com.cedarsoftware.util.IOUtilities}
 *     for more details.
 *   </li>
 * </ul>
 *
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
    private static final Convert<?> UNSUPPORTED = Converter::unsupported;
    static final String VALUE = "_v";
    
    // Precision constants for time conversions
    public static final String PRECISION_MILLIS = "millis";
    public static final String PRECISION_NANOS = "nanos";
    private static final Map<Class<?>, SortedSet<ClassLevel>> cacheParentTypes = new ClassValueMap<>();
    private static final MultiKeyMap<Convert<?>> CONVERSION_DB = new MultiKeyMap<>(4096, 0.8f);
    private static final MultiKeyMap<Convert<?>> USER_DB = new MultiKeyMap<>(128, 0.8f);
    private static final MultiKeyMap<Convert<?>> FULL_CONVERSION_CACHE = new MultiKeyMap<>(1024, 0.75f);
    private static final Map<Class<?>, String> CUSTOM_ARRAY_NAMES = new ClassValueMap<>();
    private static final ClassValueMap<Boolean> SIMPLE_TYPE_CACHE = new ClassValueMap<>();
    private static final ClassValueMap<Boolean> SELF_CONVERSION_CACHE = new ClassValueMap<>();
    private static final AtomicLong INSTANCE_ID_GENERATOR = new AtomicLong(1);
    
    // Convenience method to add conversions to CONVERSION_DB using ConversionPair
    private static void addConversionDB(ConversionPair pair, Convert<?> converter) {
        CONVERSION_DB.put(pair.getSource(), pair.getTarget(), pair.getInstanceId(), converter);
    }
    
    private final ConverterOptions options;
    private final long instanceId;

    // Efficient key that combines two Class instances and instance ID for fast creation and lookup
    public static final class ConversionPair {
        private final Class<?> source;
        private final Class<?> target;
        private final long instanceId; // Unique instance identifier
        private final int hash;

        private ConversionPair(Class<?> source, Class<?> target, long instanceId) {
            this.source = source;
            this.target = target;
            this.instanceId = instanceId;
            // Combine class hash codes with instance ID
            this.hash = 31 * (31 * source.hashCode() + target.hashCode()) + Long.hashCode(instanceId);
        }

        public Class<?> getSource() {
            return source;
        }

        public Class<?> getTarget() {
            return target;
        }

        public long getInstanceId() {
            return instanceId;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ConversionPair)) {
                return false;
            }
            ConversionPair other = (ConversionPair) obj;
            return source == other.source && target == other.target && instanceId == other.instanceId;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    // Helper method to create a conversion pair key with instance ID context
    public static ConversionPair pair(Class<?> source, Class<?> target, long instanceId) {
        return new ConversionPair(source, target, instanceId);
    }

    // Helper method for static contexts that don't have instance context (legacy support)
    public static ConversionPair pair(Class<?> source, Class<?> target) {
        return new ConversionPair(source, target, 0); // Use 0 for static/shared conversions
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
    private static void buildFactoryConversions() {
        // toNumber
        CONVERSION_DB.put(Byte.class, Number.class, 0L, Converter::identity);
        CONVERSION_DB.put(Short.class, Number.class, 0L, Converter::identity);
        CONVERSION_DB.put(Integer.class, Number.class, 0L, Converter::identity);
        CONVERSION_DB.put(Long.class, Number.class, 0L, Converter::identity);
        CONVERSION_DB.put(Float.class, Number.class, 0L, Converter::identity);
        CONVERSION_DB.put(Double.class, Number.class, 0L, Converter::identity);
        CONVERSION_DB.put(AtomicInteger.class, Number.class, 0L, Converter::identity);
        CONVERSION_DB.put(AtomicLong.class, Number.class, 0L, Converter::identity);
        CONVERSION_DB.put(BigInteger.class, Number.class, 0L, Converter::identity);
        CONVERSION_DB.put(BigDecimal.class, Number.class, 0L, Converter::identity);

        // toByte
        CONVERSION_DB.put(Void.class, byte.class, 0L, NumberConversions::toByteZero);
        CONVERSION_DB.put(Void.class, Byte.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Byte.class, 0L, Converter::identity);
        CONVERSION_DB.put(Short.class, Byte.class, 0L, NumberConversions::toByte);
        CONVERSION_DB.put(Integer.class, Byte.class, 0L, NumberConversions::toByte);
        CONVERSION_DB.put(Long.class, Byte.class, 0L, NumberConversions::toByte);
        CONVERSION_DB.put(Float.class, Byte.class, 0L, NumberConversions::toByte);
        CONVERSION_DB.put(Double.class, Byte.class, 0L, NumberConversions::toByte);
        CONVERSION_DB.put(Boolean.class, Byte.class, 0L, BooleanConversions::toByte);
        CONVERSION_DB.put(Character.class, Byte.class, 0L, CharacterConversions::toByte);
        CONVERSION_DB.put(BigInteger.class, Byte.class, 0L, NumberConversions::toByte);
        CONVERSION_DB.put(BigDecimal.class, Byte.class, 0L, NumberConversions::toByte);
        CONVERSION_DB.put(Map.class, Byte.class, 0L, MapConversions::toByte);
        CONVERSION_DB.put(String.class, Byte.class, 0L, StringConversions::toByte);

        // toShort
        CONVERSION_DB.put(Void.class, short.class, 0L, NumberConversions::toShortZero);
        CONVERSION_DB.put(Void.class, Short.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Short.class, 0L, NumberConversions::toShort);
        CONVERSION_DB.put(Short.class, Short.class, 0L, Converter::identity);
        CONVERSION_DB.put(Integer.class, Short.class, 0L, NumberConversions::toShort);
        CONVERSION_DB.put(Long.class, Short.class, 0L, NumberConversions::toShort);
        CONVERSION_DB.put(Float.class, Short.class, 0L, NumberConversions::toShort);
        CONVERSION_DB.put(Double.class, Short.class, 0L, NumberConversions::toShort);
        CONVERSION_DB.put(Boolean.class, Short.class, 0L, BooleanConversions::toShort);
        CONVERSION_DB.put(Character.class, Short.class, 0L, CharacterConversions::toShort);
        CONVERSION_DB.put(BigInteger.class, Short.class, 0L, NumberConversions::toShort);
        CONVERSION_DB.put(BigDecimal.class, Short.class, 0L, NumberConversions::toShort);
        CONVERSION_DB.put(Map.class, Short.class, 0L, MapConversions::toShort);
        CONVERSION_DB.put(String.class, Short.class, 0L, StringConversions::toShort);
        CONVERSION_DB.put(Year.class, Short.class, 0L, YearConversions::toShort);

        // toInteger
        CONVERSION_DB.put(Void.class, int.class, 0L, NumberConversions::toIntZero);
        CONVERSION_DB.put(AtomicInteger.class, int.class, 0L, UniversalConversions::atomicIntegerToInt);
        CONVERSION_DB.put(Void.class, Integer.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Integer.class, 0L, NumberConversions::toInt);
        CONVERSION_DB.put(Short.class, Integer.class, 0L, NumberConversions::toInt);
        CONVERSION_DB.put(Integer.class, Integer.class, 0L, Converter::identity);
        CONVERSION_DB.put(Long.class, Integer.class, 0L, NumberConversions::toInt);
        CONVERSION_DB.put(Float.class, Integer.class, 0L, NumberConversions::toInt);
        CONVERSION_DB.put(Double.class, Integer.class, 0L, NumberConversions::toInt);
        CONVERSION_DB.put(Boolean.class, Integer.class, 0L, BooleanConversions::toInt);
        CONVERSION_DB.put(Character.class, Integer.class, 0L, CharacterConversions::toInt);
        CONVERSION_DB.put(AtomicInteger.class, Integer.class, 0L, NumberConversions::toInt);
        CONVERSION_DB.put(BigInteger.class, Integer.class, 0L, NumberConversions::toInt);
        CONVERSION_DB.put(BigDecimal.class, Integer.class, 0L, NumberConversions::toInt);
        CONVERSION_DB.put(Map.class, Integer.class, 0L, MapConversions::toInt);
        CONVERSION_DB.put(String.class, Integer.class, 0L, StringConversions::toInt);
        CONVERSION_DB.put(Color.class, Integer.class, 0L, ColorConversions::toInteger);
        CONVERSION_DB.put(Dimension.class, Integer.class, 0L, DimensionConversions::toInteger);
        CONVERSION_DB.put(Point.class, Integer.class, 0L, PointConversions::toInteger);
        CONVERSION_DB.put(Rectangle.class, Integer.class, 0L, RectangleConversions::toInteger);
        CONVERSION_DB.put(Insets.class, Integer.class, 0L, InsetsConversions::toInteger);
        CONVERSION_DB.put(OffsetTime.class, Integer.class, 0L, OffsetTimeConversions::toInteger);
        CONVERSION_DB.put(Year.class, Integer.class, 0L, YearConversions::toInt);

        // toLong
        CONVERSION_DB.put(Void.class, long.class, 0L, NumberConversions::toLongZero);
        CONVERSION_DB.put(AtomicLong.class, long.class, 0L, UniversalConversions::atomicLongToLong);
        CONVERSION_DB.put(Void.class, Long.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Long.class, 0L, NumberConversions::toLong);
        CONVERSION_DB.put(Short.class, Long.class, 0L, NumberConversions::toLong);
        CONVERSION_DB.put(Integer.class, Long.class, 0L, NumberConversions::toLong);
        CONVERSION_DB.put(Long.class, Long.class, 0L, Converter::identity);
        CONVERSION_DB.put(Float.class, Long.class, 0L, NumberConversions::toLong);
        CONVERSION_DB.put(Double.class, Long.class, 0L, NumberConversions::toLong);
        CONVERSION_DB.put(Boolean.class, Long.class, 0L, BooleanConversions::toLong);
        CONVERSION_DB.put(Character.class, Long.class, 0L, CharacterConversions::toLong);
        CONVERSION_DB.put(BigInteger.class, Long.class, 0L, NumberConversions::toLong);
        CONVERSION_DB.put(BigDecimal.class, Long.class, 0L, NumberConversions::toLong);
        CONVERSION_DB.put(AtomicLong.class, Long.class, 0L, NumberConversions::toLong);
        CONVERSION_DB.put(Date.class, Long.class, 0L, DateConversions::toLong);
        CONVERSION_DB.put(java.sql.Date.class, Long.class, 0L, SqlDateConversions::toLong);
        CONVERSION_DB.put(Timestamp.class, Long.class, 0L, TimestampConversions::toLong);
        CONVERSION_DB.put(Instant.class, Long.class, 0L, InstantConversions::toLong);
        CONVERSION_DB.put(Duration.class, Long.class, 0L, DurationConversions::toLong);
        CONVERSION_DB.put(LocalDate.class, Long.class, 0L, LocalDateConversions::toLong);
        CONVERSION_DB.put(LocalTime.class, Long.class, 0L, LocalTimeConversions::toLong);
        CONVERSION_DB.put(LocalDateTime.class, Long.class, 0L, LocalDateTimeConversions::toLong);
        CONVERSION_DB.put(OffsetTime.class, Long.class, 0L, OffsetTimeConversions::toLong);
        CONVERSION_DB.put(OffsetDateTime.class, Long.class, 0L, OffsetDateTimeConversions::toLong);
        CONVERSION_DB.put(ZonedDateTime.class, Long.class, 0L, ZonedDateTimeConversions::toLong);
        CONVERSION_DB.put(Map.class, Long.class, 0L, MapConversions::toLong);
        CONVERSION_DB.put(String.class, Long.class, 0L, StringConversions::toLong);
        CONVERSION_DB.put(Color.class, Long.class, 0L, ColorConversions::toLong);
        CONVERSION_DB.put(Dimension.class, Long.class, 0L, DimensionConversions::toLong);
        CONVERSION_DB.put(Point.class, Long.class, 0L, PointConversions::toLong);
        CONVERSION_DB.put(Rectangle.class, Long.class, 0L, RectangleConversions::toLong);
        CONVERSION_DB.put(Insets.class, Long.class, 0L, InsetsConversions::toLong);
        CONVERSION_DB.put(Year.class, Long.class, 0L, YearConversions::toLong);

        // toFloat
        CONVERSION_DB.put(Void.class, float.class, 0L, NumberConversions::toFloatZero);
        CONVERSION_DB.put(Void.class, Float.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Float.class, 0L, NumberConversions::toFloat);
        CONVERSION_DB.put(Short.class, Float.class, 0L, NumberConversions::toFloat);
        CONVERSION_DB.put(Integer.class, Float.class, 0L, NumberConversions::toFloat);
        CONVERSION_DB.put(Long.class, Float.class, 0L, NumberConversions::toFloat);
        CONVERSION_DB.put(Float.class, Float.class, 0L, Converter::identity);
        CONVERSION_DB.put(Double.class, Float.class, 0L, NumberConversions::toFloat);
        CONVERSION_DB.put(Boolean.class, Float.class, 0L, BooleanConversions::toFloat);
        CONVERSION_DB.put(Character.class, Float.class, 0L, CharacterConversions::toFloat);
        CONVERSION_DB.put(BigInteger.class, Float.class, 0L, NumberConversions::toFloat);
        CONVERSION_DB.put(BigDecimal.class, Float.class, 0L, NumberConversions::toFloat);
        CONVERSION_DB.put(Map.class, Float.class, 0L, MapConversions::toFloat);
        CONVERSION_DB.put(String.class, Float.class, 0L, StringConversions::toFloat);

        // toDouble
        CONVERSION_DB.put(Void.class, double.class, 0L, NumberConversions::toDoubleZero);
        CONVERSION_DB.put(Void.class, Double.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Double.class, 0L, NumberConversions::toDouble);
        CONVERSION_DB.put(Short.class, Double.class, 0L, NumberConversions::toDouble);
        CONVERSION_DB.put(Integer.class, Double.class, 0L, NumberConversions::toDouble);
        CONVERSION_DB.put(Long.class, Double.class, 0L, NumberConversions::toDouble);
        CONVERSION_DB.put(Float.class, Double.class, 0L, NumberConversions::toDouble);
        CONVERSION_DB.put(Double.class, Double.class, 0L, Converter::identity);
        CONVERSION_DB.put(Boolean.class, Double.class, 0L, BooleanConversions::toDouble);
        CONVERSION_DB.put(Character.class, Double.class, 0L, CharacterConversions::toDouble);
        CONVERSION_DB.put(Duration.class, Double.class, 0L, DurationConversions::toDouble);
        CONVERSION_DB.put(Instant.class, Double.class, 0L, InstantConversions::toDouble);
        CONVERSION_DB.put(LocalTime.class, Double.class, 0L, LocalTimeConversions::toDouble);
        CONVERSION_DB.put(LocalDate.class, Double.class, 0L, LocalDateConversions::toDouble);
        CONVERSION_DB.put(LocalDateTime.class, Double.class, 0L, LocalDateTimeConversions::toDouble);
        CONVERSION_DB.put(ZonedDateTime.class, Double.class, 0L, ZonedDateTimeConversions::toDouble);
        CONVERSION_DB.put(OffsetTime.class, Double.class, 0L, OffsetTimeConversions::toDouble);
        CONVERSION_DB.put(OffsetDateTime.class, Double.class, 0L, OffsetDateTimeConversions::toDouble);
        CONVERSION_DB.put(Date.class, Double.class, 0L, DateConversions::toDouble);
        CONVERSION_DB.put(java.sql.Date.class, Double.class, 0L, SqlDateConversions::toDouble);
        CONVERSION_DB.put(Timestamp.class, Double.class, 0L, TimestampConversions::toDouble);
        CONVERSION_DB.put(BigInteger.class, Double.class, 0L, NumberConversions::toDouble);
        CONVERSION_DB.put(BigDecimal.class, Double.class, 0L, NumberConversions::toDouble);
        CONVERSION_DB.put(Map.class, Double.class, 0L, MapConversions::toDouble);
        CONVERSION_DB.put(String.class, Double.class, 0L, StringConversions::toDouble);

        // Boolean/boolean conversions supported
        CONVERSION_DB.put(Void.class, boolean.class, 0L, VoidConversions::toBoolean);
        CONVERSION_DB.put(AtomicBoolean.class, boolean.class, 0L, UniversalConversions::atomicBooleanToBoolean);
        CONVERSION_DB.put(AtomicBoolean.class, Boolean.class, 0L, AtomicBooleanConversions::toBoolean);
        CONVERSION_DB.put(Void.class, Boolean.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Boolean.class, 0L, NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(Short.class, Boolean.class, 0L, NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(Integer.class, Boolean.class, 0L, NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(Long.class, Boolean.class, 0L, NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(Float.class, Boolean.class, 0L, NumberConversions::isFloatTypeNotZero);
        CONVERSION_DB.put(Double.class, Boolean.class, 0L, NumberConversions::isFloatTypeNotZero);
        CONVERSION_DB.put(Boolean.class, Boolean.class, 0L, Converter::identity);
        CONVERSION_DB.put(Character.class, Boolean.class, 0L, CharacterConversions::toBoolean);
        CONVERSION_DB.put(BigInteger.class, Boolean.class, 0L, NumberConversions::isBigIntegerNotZero);
        CONVERSION_DB.put(BigDecimal.class, Boolean.class, 0L, NumberConversions::isBigDecimalNotZero);
        CONVERSION_DB.put(Map.class, Boolean.class, 0L, MapConversions::toBoolean);
        CONVERSION_DB.put(String.class, Boolean.class, 0L, StringConversions::toBoolean);
        CONVERSION_DB.put(Dimension.class, Boolean.class, 0L, DimensionConversions::toBoolean);
        CONVERSION_DB.put(Point.class, Boolean.class, 0L, PointConversions::toBoolean);
        CONVERSION_DB.put(Rectangle.class, Boolean.class, 0L, RectangleConversions::toBoolean);
        CONVERSION_DB.put(Insets.class, Boolean.class, 0L, InsetsConversions::toBoolean);
        CONVERSION_DB.put(UUID.class, Boolean.class, 0L, UUIDConversions::toBoolean);

        // Character/char conversions supported
        CONVERSION_DB.put(Void.class, char.class, 0L, VoidConversions::toCharacter);
        CONVERSION_DB.put(Void.class, Character.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Character.class, 0L, ByteConversions::toCharacter);
        CONVERSION_DB.put(Short.class, Character.class, 0L, NumberConversions::toCharacter);
        CONVERSION_DB.put(Integer.class, Character.class, 0L, NumberConversions::toCharacter);
        CONVERSION_DB.put(Long.class, Character.class, 0L, NumberConversions::toCharacter);
        CONVERSION_DB.put(Float.class, Character.class, 0L, NumberConversions::toCharacter);
        CONVERSION_DB.put(Double.class, Character.class, 0L, NumberConversions::toCharacter);
        CONVERSION_DB.put(Boolean.class, Character.class, 0L, BooleanConversions::toCharacter);
        CONVERSION_DB.put(Character.class, Character.class, 0L, Converter::identity);
        CONVERSION_DB.put(BigInteger.class, Character.class, 0L, NumberConversions::toCharacter);
        CONVERSION_DB.put(BigDecimal.class, Character.class, 0L, NumberConversions::toCharacter);
        CONVERSION_DB.put(Map.class, Character.class, 0L, MapConversions::toCharacter);
        CONVERSION_DB.put(String.class, Character.class, 0L, StringConversions::toCharacter);

        // BigInteger versions supported
        CONVERSION_DB.put(Void.class, BigInteger.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, BigInteger.class, 0L, NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(Short.class, BigInteger.class, 0L, NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(Integer.class, BigInteger.class, 0L, NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(Long.class, BigInteger.class, 0L, NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(Float.class, BigInteger.class, 0L, NumberConversions::floatingPointToBigInteger);
        CONVERSION_DB.put(Double.class, BigInteger.class, 0L, NumberConversions::floatingPointToBigInteger);
        CONVERSION_DB.put(Boolean.class, BigInteger.class, 0L, BooleanConversions::toBigInteger);
        CONVERSION_DB.put(Character.class, BigInteger.class, 0L, CharacterConversions::toBigInteger);
        CONVERSION_DB.put(BigInteger.class, BigInteger.class, 0L, Converter::identity);
        CONVERSION_DB.put(BigDecimal.class, BigInteger.class, 0L, BigDecimalConversions::toBigInteger);
        CONVERSION_DB.put(Date.class, BigInteger.class, 0L, DateConversions::toBigInteger);
        CONVERSION_DB.put(java.sql.Date.class, BigInteger.class, 0L, SqlDateConversions::toBigInteger);
        CONVERSION_DB.put(Timestamp.class, BigInteger.class, 0L, TimestampConversions::toBigInteger);
        CONVERSION_DB.put(Duration.class, BigInteger.class, 0L, DurationConversions::toBigInteger);
        CONVERSION_DB.put(Instant.class, BigInteger.class, 0L, InstantConversions::toBigInteger);
        CONVERSION_DB.put(LocalTime.class, BigInteger.class, 0L, LocalTimeConversions::toBigInteger);
        CONVERSION_DB.put(LocalDate.class, BigInteger.class, 0L, LocalDateConversions::toBigInteger);
        CONVERSION_DB.put(LocalDateTime.class, BigInteger.class, 0L, LocalDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(ZonedDateTime.class, BigInteger.class, 0L, ZonedDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(OffsetTime.class, BigInteger.class, 0L, OffsetTimeConversions::toBigInteger);
        CONVERSION_DB.put(OffsetDateTime.class, BigInteger.class, 0L, OffsetDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(UUID.class, BigInteger.class, 0L, UUIDConversions::toBigInteger);
        CONVERSION_DB.put(Color.class, BigInteger.class, 0L, ColorConversions::toBigInteger);
        CONVERSION_DB.put(Dimension.class, BigInteger.class, 0L, DimensionConversions::toBigInteger);
        CONVERSION_DB.put(Point.class, BigInteger.class, 0L, PointConversions::toBigInteger);
        CONVERSION_DB.put(Rectangle.class, BigInteger.class, 0L, RectangleConversions::toBigInteger);
        CONVERSION_DB.put(Insets.class, BigInteger.class, 0L, InsetsConversions::toBigInteger);
        CONVERSION_DB.put(Calendar.class, BigInteger.class, 0L, CalendarConversions::toBigInteger);  // Restored - bridge has precision difference (millis vs nanos)
        CONVERSION_DB.put(Map.class, BigInteger.class, 0L, MapConversions::toBigInteger);
        CONVERSION_DB.put(String.class, BigInteger.class, 0L, StringConversions::toBigInteger);
        CONVERSION_DB.put(Year.class, BigInteger.class, 0L, YearConversions::toBigInteger);

        // BigDecimal conversions supported
        CONVERSION_DB.put(Void.class, BigDecimal.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, BigDecimal.class, 0L, NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(Short.class, BigDecimal.class, 0L, NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(Integer.class, BigDecimal.class, 0L, NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(Long.class, BigDecimal.class, 0L, NumberConversions::integerTypeToBigDecimal);
        CONVERSION_DB.put(Float.class, BigDecimal.class, 0L, NumberConversions::floatingPointToBigDecimal);
        CONVERSION_DB.put(Double.class, BigDecimal.class, 0L, NumberConversions::floatingPointToBigDecimal);
        CONVERSION_DB.put(Boolean.class, BigDecimal.class, 0L, BooleanConversions::toBigDecimal);
        CONVERSION_DB.put(Character.class, BigDecimal.class, 0L, CharacterConversions::toBigDecimal);
        CONVERSION_DB.put(BigDecimal.class, BigDecimal.class, 0L, Converter::identity);
        CONVERSION_DB.put(BigInteger.class, BigDecimal.class, 0L, BigIntegerConversions::toBigDecimal);
        CONVERSION_DB.put(Date.class, BigDecimal.class, 0L, DateConversions::toBigDecimal);
        CONVERSION_DB.put(java.sql.Date.class, BigDecimal.class, 0L, SqlDateConversions::toBigDecimal);
        CONVERSION_DB.put(Timestamp.class, BigDecimal.class, 0L, TimestampConversions::toBigDecimal);
        CONVERSION_DB.put(Instant.class, BigDecimal.class, 0L, InstantConversions::toBigDecimal);
        CONVERSION_DB.put(Duration.class, BigDecimal.class, 0L, DurationConversions::toBigDecimal);
        CONVERSION_DB.put(LocalTime.class, BigDecimal.class, 0L, LocalTimeConversions::toBigDecimal);
        CONVERSION_DB.put(LocalDate.class, BigDecimal.class, 0L, LocalDateConversions::toBigDecimal);
        CONVERSION_DB.put(LocalDateTime.class, BigDecimal.class, 0L, LocalDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(ZonedDateTime.class, BigDecimal.class, 0L, ZonedDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(OffsetTime.class, BigDecimal.class, 0L, OffsetTimeConversions::toBigDecimal);
        CONVERSION_DB.put(OffsetDateTime.class, BigDecimal.class, 0L, OffsetDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(UUID.class, BigDecimal.class, 0L, UUIDConversions::toBigDecimal);
        CONVERSION_DB.put(Color.class, BigDecimal.class, 0L, ColorConversions::toBigDecimal);
        CONVERSION_DB.put(Dimension.class, BigDecimal.class, 0L, DimensionConversions::toBigDecimal);
        CONVERSION_DB.put(Insets.class, BigDecimal.class, 0L, InsetsConversions::toBigDecimal);
        CONVERSION_DB.put(Point.class, BigDecimal.class, 0L, PointConversions::toBigDecimal);
        CONVERSION_DB.put(Rectangle.class, BigDecimal.class, 0L, RectangleConversions::toBigDecimal);
        CONVERSION_DB.put(Calendar.class, BigDecimal.class, 0L, CalendarConversions::toBigDecimal);
        CONVERSION_DB.put(Map.class, BigDecimal.class, 0L, MapConversions::toBigDecimal);
        CONVERSION_DB.put(String.class, BigDecimal.class, 0L, StringConversions::toBigDecimal);

        // AtomicBoolean conversions supported
        CONVERSION_DB.put(Void.class, AtomicBoolean.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, AtomicBoolean.class, 0L, NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(Short.class, AtomicBoolean.class, 0L, NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(Integer.class, AtomicBoolean.class, 0L, NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(Long.class, AtomicBoolean.class, 0L, NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(Float.class, AtomicBoolean.class, 0L, NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(Double.class, AtomicBoolean.class, 0L, NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(Boolean.class, AtomicBoolean.class, 0L, BooleanConversions::toAtomicBoolean);
        CONVERSION_DB.put(Character.class, AtomicBoolean.class, 0L, CharacterConversions::toAtomicBoolean);
        CONVERSION_DB.put(BigInteger.class, AtomicBoolean.class, 0L, NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(BigDecimal.class, AtomicBoolean.class, 0L, NumberConversions::toAtomicBoolean);
        CONVERSION_DB.put(AtomicBoolean.class, AtomicBoolean.class, 0L, AtomicBooleanConversions::toAtomicBoolean);
        CONVERSION_DB.put(Map.class, AtomicBoolean.class, 0L, MapConversions::toAtomicBoolean);
        CONVERSION_DB.put(String.class, AtomicBoolean.class, 0L, StringConversions::toAtomicBoolean);

        // AtomicInteger conversions supported
        CONVERSION_DB.put(Void.class, AtomicInteger.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, AtomicInteger.class, 0L, NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(Short.class, AtomicInteger.class, 0L, NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(Integer.class, AtomicInteger.class, 0L, NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(Long.class, AtomicInteger.class, 0L, NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(Float.class, AtomicInteger.class, 0L, NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(Double.class, AtomicInteger.class, 0L, NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(Boolean.class, AtomicInteger.class, 0L, BooleanConversions::toAtomicInteger);
        CONVERSION_DB.put(Character.class, AtomicInteger.class, 0L, CharacterConversions::toAtomicInteger);
        CONVERSION_DB.put(BigInteger.class, AtomicInteger.class, 0L, NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(BigDecimal.class, AtomicInteger.class, 0L, NumberConversions::toAtomicInteger);
        CONVERSION_DB.put(AtomicInteger.class, AtomicInteger.class, 0L, AtomicIntegerConversions::toAtomicInteger);
        CONVERSION_DB.put(OffsetTime.class, AtomicInteger.class, 0L, OffsetTimeConversions::toAtomicInteger);
        CONVERSION_DB.put(Map.class, AtomicInteger.class, 0L, MapConversions::toAtomicInteger);
        CONVERSION_DB.put(String.class, AtomicInteger.class, 0L, StringConversions::toAtomicInteger);

        // AtomicLong conversions supported
        CONVERSION_DB.put(Void.class, AtomicLong.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, AtomicLong.class, 0L, NumberConversions::toAtomicLong);
        CONVERSION_DB.put(Short.class, AtomicLong.class, 0L, NumberConversions::toAtomicLong);
        CONVERSION_DB.put(Integer.class, AtomicLong.class, 0L, NumberConversions::toAtomicLong);
        CONVERSION_DB.put(Long.class, AtomicLong.class, 0L, NumberConversions::toAtomicLong);
        CONVERSION_DB.put(Float.class, AtomicLong.class, 0L, NumberConversions::toAtomicLong);
        CONVERSION_DB.put(Double.class, AtomicLong.class, 0L, NumberConversions::toAtomicLong);
        CONVERSION_DB.put(Boolean.class, AtomicLong.class, 0L, BooleanConversions::toAtomicLong);
        CONVERSION_DB.put(Character.class, AtomicLong.class, 0L, CharacterConversions::toAtomicLong);
        CONVERSION_DB.put(BigInteger.class, AtomicLong.class, 0L, NumberConversions::toAtomicLong);
        CONVERSION_DB.put(BigDecimal.class, AtomicLong.class, 0L, NumberConversions::toAtomicLong);
        CONVERSION_DB.put(AtomicLong.class, AtomicLong.class, 0L, AtomicLongConversions::toAtomicLong);
        CONVERSION_DB.put(Date.class, AtomicLong.class, 0L, DateConversions::toAtomicLong);
        CONVERSION_DB.put(java.sql.Date.class, AtomicLong.class, 0L, SqlDateConversions::toAtomicLong);
        CONVERSION_DB.put(Timestamp.class, AtomicLong.class, 0L, DateConversions::toAtomicLong);
        CONVERSION_DB.put(Instant.class, AtomicLong.class, 0L, InstantConversions::toAtomicLong);
        CONVERSION_DB.put(Duration.class, AtomicLong.class, 0L, DurationConversions::toAtomicLong);
        CONVERSION_DB.put(LocalDate.class, AtomicLong.class, 0L, LocalDateConversions::toAtomicLong);
        CONVERSION_DB.put(LocalTime.class, AtomicLong.class, 0L, LocalTimeConversions::toAtomicLong);
        CONVERSION_DB.put(LocalDateTime.class, AtomicLong.class, 0L, LocalDateTimeConversions::toAtomicLong);
        CONVERSION_DB.put(ZonedDateTime.class, AtomicLong.class, 0L, ZonedDateTimeConversions::toAtomicLong);
        CONVERSION_DB.put(OffsetTime.class, AtomicLong.class, 0L, OffsetTimeConversions::toAtomicLong);
        CONVERSION_DB.put(OffsetDateTime.class, AtomicLong.class, 0L, OffsetDateTimeConversions::toAtomicLong);
        CONVERSION_DB.put(Map.class, AtomicLong.class, 0L, MapConversions::toAtomicLong);
        CONVERSION_DB.put(String.class, AtomicLong.class, 0L, StringConversions::toAtomicLong);

        // Date conversions supported
        CONVERSION_DB.put(Void.class, Date.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, Date.class, 0L, NumberConversions::toDate);
        CONVERSION_DB.put(Double.class, Date.class, 0L, DoubleConversions::toDate);
        CONVERSION_DB.put(BigInteger.class, Date.class, 0L, BigIntegerConversions::toDate);
        CONVERSION_DB.put(BigDecimal.class, Date.class, 0L, BigDecimalConversions::toDate);
        CONVERSION_DB.put(Date.class, Date.class, 0L, DateConversions::toDate);
        CONVERSION_DB.put(java.sql.Date.class, Date.class, 0L, SqlDateConversions::toDate);
        CONVERSION_DB.put(Timestamp.class, Date.class, 0L, TimestampConversions::toDate);
        CONVERSION_DB.put(Instant.class, Date.class, 0L, InstantConversions::toDate);
        CONVERSION_DB.put(LocalDate.class, Date.class, 0L, LocalDateConversions::toDate);
        CONVERSION_DB.put(LocalDateTime.class, Date.class, 0L, LocalDateTimeConversions::toDate);
        CONVERSION_DB.put(ZonedDateTime.class, Date.class, 0L, ZonedDateTimeConversions::toDate);
        CONVERSION_DB.put(OffsetDateTime.class, Date.class, 0L, OffsetDateTimeConversions::toDate);
        CONVERSION_DB.put(Map.class, Date.class, 0L, MapConversions::toDate);
        CONVERSION_DB.put(String.class, Date.class, 0L, StringConversions::toDate);

        // java.sql.Date conversion supported
        CONVERSION_DB.put(Void.class, java.sql.Date.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, java.sql.Date.class, 0L, NumberConversions::toSqlDate);
        CONVERSION_DB.put(Double.class, java.sql.Date.class, 0L, DoubleConversions::toSqlDate);
        CONVERSION_DB.put(BigInteger.class, java.sql.Date.class, 0L, BigIntegerConversions::toSqlDate);
        CONVERSION_DB.put(BigDecimal.class, java.sql.Date.class, 0L, BigDecimalConversions::toSqlDate);
        CONVERSION_DB.put(java.sql.Date.class, java.sql.Date.class, 0L, SqlDateConversions::toSqlDate);
        CONVERSION_DB.put(Date.class, java.sql.Date.class, 0L, DateConversions::toSqlDate);
        CONVERSION_DB.put(Timestamp.class, java.sql.Date.class, 0L, TimestampConversions::toSqlDate);
        CONVERSION_DB.put(Duration.class, java.sql.Date.class, 0L, DurationConversions::toSqlDate);
        CONVERSION_DB.put(Instant.class, java.sql.Date.class, 0L, InstantConversions::toSqlDate);
        CONVERSION_DB.put(LocalDate.class, java.sql.Date.class, 0L, LocalDateConversions::toSqlDate);
        CONVERSION_DB.put(LocalDateTime.class, java.sql.Date.class, 0L, LocalDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(ZonedDateTime.class, java.sql.Date.class, 0L, ZonedDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(OffsetDateTime.class, java.sql.Date.class, 0L, OffsetDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(Map.class, java.sql.Date.class, 0L, MapConversions::toSqlDate);
        CONVERSION_DB.put(String.class, java.sql.Date.class, 0L, StringConversions::toSqlDate);

        // Timestamp conversions supported
        CONVERSION_DB.put(Void.class, Timestamp.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, Timestamp.class, 0L, NumberConversions::toTimestamp);
        CONVERSION_DB.put(Double.class, Timestamp.class, 0L, DoubleConversions::toTimestamp);
        CONVERSION_DB.put(BigInteger.class, Timestamp.class, 0L, BigIntegerConversions::toTimestamp);
        CONVERSION_DB.put(BigDecimal.class, Timestamp.class, 0L, BigDecimalConversions::toTimestamp);
        CONVERSION_DB.put(Timestamp.class, Timestamp.class, 0L, DateConversions::toTimestamp);
        CONVERSION_DB.put(java.sql.Date.class, Timestamp.class, 0L, SqlDateConversions::toTimestamp);
        CONVERSION_DB.put(Date.class, Timestamp.class, 0L, DateConversions::toTimestamp);
        CONVERSION_DB.put(Duration.class, Timestamp.class, 0L, DurationConversions::toTimestamp);
        CONVERSION_DB.put(Instant.class, Timestamp.class, 0L, InstantConversions::toTimestamp);
        CONVERSION_DB.put(LocalDate.class, Timestamp.class, 0L, LocalDateConversions::toTimestamp);
        CONVERSION_DB.put(LocalDateTime.class, Timestamp.class, 0L, LocalDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(ZonedDateTime.class, Timestamp.class, 0L, ZonedDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(OffsetDateTime.class, Timestamp.class, 0L, OffsetDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(Map.class, Timestamp.class, 0L, MapConversions::toTimestamp);
        CONVERSION_DB.put(String.class, Timestamp.class, 0L, StringConversions::toTimestamp);

        // Calendar conversions supported
        CONVERSION_DB.put(Void.class, Calendar.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, Calendar.class, 0L, NumberConversions::toCalendar);
        CONVERSION_DB.put(Double.class, Calendar.class, 0L, DoubleConversions::toCalendar);
        CONVERSION_DB.put(BigInteger.class, Calendar.class, 0L, BigIntegerConversions::toCalendar);
        CONVERSION_DB.put(BigDecimal.class, Calendar.class, 0L, BigDecimalConversions::toCalendar);
        CONVERSION_DB.put(Date.class, Calendar.class, 0L, DateConversions::toCalendar);
        CONVERSION_DB.put(java.sql.Date.class, Calendar.class, 0L, SqlDateConversions::toCalendar);
        CONVERSION_DB.put(Timestamp.class, Calendar.class, 0L, TimestampConversions::toCalendar);
        CONVERSION_DB.put(Instant.class, Calendar.class, 0L, InstantConversions::toCalendar);
        CONVERSION_DB.put(LocalTime.class, Calendar.class, 0L, LocalTimeConversions::toCalendar);
        CONVERSION_DB.put(LocalDate.class, Calendar.class, 0L, LocalDateConversions::toCalendar);
        CONVERSION_DB.put(LocalDateTime.class, Calendar.class, 0L, LocalDateTimeConversions::toCalendar);
        CONVERSION_DB.put(ZonedDateTime.class, Calendar.class, 0L, ZonedDateTimeConversions::toCalendar);
        CONVERSION_DB.put(OffsetDateTime.class, Calendar.class, 0L, OffsetDateTimeConversions::toCalendar);
        CONVERSION_DB.put(Calendar.class, Calendar.class, 0L, CalendarConversions::clone);
        CONVERSION_DB.put(Map.class, Calendar.class, 0L, MapConversions::toCalendar);
        CONVERSION_DB.put(String.class, Calendar.class, 0L, StringConversions::toCalendar);

        // LocalDate conversions supported
        CONVERSION_DB.put(Void.class, LocalDate.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, LocalDate.class, 0L, NumberConversions::toLocalDate);
        CONVERSION_DB.put(Double.class, LocalDate.class, 0L, DoubleConversions::toLocalDate);
        CONVERSION_DB.put(BigInteger.class, LocalDate.class, 0L, BigIntegerConversions::toLocalDate);
        CONVERSION_DB.put(BigDecimal.class, LocalDate.class, 0L, BigDecimalConversions::toLocalDate);
        CONVERSION_DB.put(java.sql.Date.class, LocalDate.class, 0L, SqlDateConversions::toLocalDate);
        CONVERSION_DB.put(Timestamp.class, LocalDate.class, 0L, DateConversions::toLocalDate);
        CONVERSION_DB.put(Date.class, LocalDate.class, 0L, DateConversions::toLocalDate);
        CONVERSION_DB.put(Instant.class, LocalDate.class, 0L, InstantConversions::toLocalDate);
        CONVERSION_DB.put(Calendar.class, LocalDate.class, 0L, CalendarConversions::toLocalDate);
        CONVERSION_DB.put(LocalDate.class, LocalDate.class, 0L, Converter::identity);
        CONVERSION_DB.put(LocalDateTime.class, LocalDate.class, 0L, LocalDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(ZonedDateTime.class, LocalDate.class, 0L, ZonedDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(OffsetDateTime.class, LocalDate.class, 0L, OffsetDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(Map.class, LocalDate.class, 0L, MapConversions::toLocalDate);
        CONVERSION_DB.put(String.class, LocalDate.class, 0L, StringConversions::toLocalDate);

        // LocalDateTime conversions supported
        CONVERSION_DB.put(Void.class, LocalDateTime.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, LocalDateTime.class, 0L, NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(Double.class, LocalDateTime.class, 0L, DoubleConversions::toLocalDateTime);
        CONVERSION_DB.put(BigInteger.class, LocalDateTime.class, 0L, BigIntegerConversions::toLocalDateTime);
        CONVERSION_DB.put(BigDecimal.class, LocalDateTime.class, 0L, BigDecimalConversions::toLocalDateTime);
        CONVERSION_DB.put(java.sql.Date.class, LocalDateTime.class, 0L, SqlDateConversions::toLocalDateTime);
        CONVERSION_DB.put(Timestamp.class, LocalDateTime.class, 0L, TimestampConversions::toLocalDateTime);
        CONVERSION_DB.put(Date.class, LocalDateTime.class, 0L, DateConversions::toLocalDateTime);
        CONVERSION_DB.put(Instant.class, LocalDateTime.class, 0L, InstantConversions::toLocalDateTime);
        CONVERSION_DB.put(LocalDateTime.class, LocalDateTime.class, 0L, LocalDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(LocalDate.class, LocalDateTime.class, 0L, LocalDateConversions::toLocalDateTime);
        CONVERSION_DB.put(Calendar.class, LocalDateTime.class, 0L, CalendarConversions::toLocalDateTime);
        CONVERSION_DB.put(ZonedDateTime.class, LocalDateTime.class, 0L, ZonedDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(OffsetDateTime.class, LocalDateTime.class, 0L, OffsetDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(Map.class, LocalDateTime.class, 0L, MapConversions::toLocalDateTime);
        CONVERSION_DB.put(String.class, LocalDateTime.class, 0L, StringConversions::toLocalDateTime);

        // LocalTime conversions supported
        CONVERSION_DB.put(Void.class, LocalTime.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, LocalTime.class, 0L, NumberConversions::longNanosToLocalTime);
        CONVERSION_DB.put(Double.class, LocalTime.class, 0L, DoubleConversions::toLocalTime);
        CONVERSION_DB.put(BigInteger.class, LocalTime.class, 0L, BigIntegerConversions::toLocalTime);
        CONVERSION_DB.put(BigDecimal.class, LocalTime.class, 0L, BigDecimalConversions::toLocalTime);
        CONVERSION_DB.put(Timestamp.class, LocalTime.class, 0L, DateConversions::toLocalTime);
        CONVERSION_DB.put(Date.class, LocalTime.class, 0L, DateConversions::toLocalTime);
        CONVERSION_DB.put(Instant.class, LocalTime.class, 0L, InstantConversions::toLocalTime);
        CONVERSION_DB.put(LocalDateTime.class, LocalTime.class, 0L, LocalDateTimeConversions::toLocalTime);
        CONVERSION_DB.put(LocalTime.class, LocalTime.class, 0L, Converter::identity);
        CONVERSION_DB.put(ZonedDateTime.class, LocalTime.class, 0L, ZonedDateTimeConversions::toLocalTime);
        CONVERSION_DB.put(OffsetDateTime.class, LocalTime.class, 0L, OffsetDateTimeConversions::toLocalTime);
        CONVERSION_DB.put(Map.class, LocalTime.class, 0L, MapConversions::toLocalTime);
        CONVERSION_DB.put(String.class, LocalTime.class, 0L, StringConversions::toLocalTime);

        // ZonedDateTime conversions supported
        CONVERSION_DB.put(Void.class, ZonedDateTime.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Long.class, ZonedDateTime.class, 0L, NumberConversions::toZonedDateTime);
        CONVERSION_DB.put(Double.class, ZonedDateTime.class, 0L, DoubleConversions::toZonedDateTime);
        CONVERSION_DB.put(BigInteger.class, ZonedDateTime.class, 0L, BigIntegerConversions::toZonedDateTime);
        CONVERSION_DB.put(BigDecimal.class, ZonedDateTime.class, 0L, BigDecimalConversions::toZonedDateTime);
        CONVERSION_DB.put(java.sql.Date.class, ZonedDateTime.class, 0L, SqlDateConversions::toZonedDateTime);
        CONVERSION_DB.put(Timestamp.class, ZonedDateTime.class, 0L, DateConversions::toZonedDateTime);
        CONVERSION_DB.put(Date.class, ZonedDateTime.class, 0L, DateConversions::toZonedDateTime);
        CONVERSION_DB.put(Instant.class, ZonedDateTime.class, 0L, InstantConversions::toZonedDateTime);
        CONVERSION_DB.put(LocalDate.class, ZonedDateTime.class, 0L, LocalDateConversions::toZonedDateTime);
        CONVERSION_DB.put(LocalDateTime.class, ZonedDateTime.class, 0L, LocalDateTimeConversions::toZonedDateTime);
        CONVERSION_DB.put(ZonedDateTime.class, ZonedDateTime.class, 0L, Converter::identity);
        CONVERSION_DB.put(OffsetDateTime.class, ZonedDateTime.class, 0L, OffsetDateTimeConversions::toZonedDateTime);
        CONVERSION_DB.put(Calendar.class, ZonedDateTime.class, 0L, CalendarConversions::toZonedDateTime);
        CONVERSION_DB.put(Map.class, ZonedDateTime.class, 0L, MapConversions::toZonedDateTime);
        CONVERSION_DB.put(String.class, ZonedDateTime.class, 0L, StringConversions::toZonedDateTime);

        // toOffsetDateTime
        CONVERSION_DB.put(Void.class, OffsetDateTime.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(OffsetDateTime.class, OffsetDateTime.class, 0L, Converter::identity);
        CONVERSION_DB.put(Map.class, OffsetDateTime.class, 0L, MapConversions::toOffsetDateTime);
        CONVERSION_DB.put(String.class, OffsetDateTime.class, 0L, StringConversions::toOffsetDateTime);
        CONVERSION_DB.put(Long.class, OffsetDateTime.class, 0L, NumberConversions::toOffsetDateTime);
        CONVERSION_DB.put(Double.class, OffsetDateTime.class, 0L, DoubleConversions::toOffsetDateTime);
        CONVERSION_DB.put(BigInteger.class, OffsetDateTime.class, 0L, BigIntegerConversions::toOffsetDateTime);
        CONVERSION_DB.put(BigDecimal.class, OffsetDateTime.class, 0L, BigDecimalConversions::toOffsetDateTime);
        CONVERSION_DB.put(java.sql.Date.class, OffsetDateTime.class, 0L, SqlDateConversions::toOffsetDateTime);
        CONVERSION_DB.put(Date.class, OffsetDateTime.class, 0L, DateConversions::toOffsetDateTime);
        CONVERSION_DB.put(Timestamp.class, OffsetDateTime.class, 0L, TimestampConversions::toOffsetDateTime);
        CONVERSION_DB.put(LocalDate.class, OffsetDateTime.class, 0L, LocalDateConversions::toOffsetDateTime);
        CONVERSION_DB.put(Instant.class, OffsetDateTime.class, 0L, InstantConversions::toOffsetDateTime);
        CONVERSION_DB.put(ZonedDateTime.class, OffsetDateTime.class, 0L, ZonedDateTimeConversions::toOffsetDateTime);
        CONVERSION_DB.put(LocalDateTime.class, OffsetDateTime.class, 0L, LocalDateTimeConversions::toOffsetDateTime);

        // toOffsetTime
        CONVERSION_DB.put(Void.class, OffsetTime.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Integer.class, OffsetTime.class, 0L, NumberConversions::toOffsetTime);
        CONVERSION_DB.put(Long.class, OffsetTime.class, 0L, NumberConversions::toOffsetTime);
        CONVERSION_DB.put(Double.class, OffsetTime.class, 0L, DoubleConversions::toOffsetTime);
        CONVERSION_DB.put(BigInteger.class, OffsetTime.class, 0L, BigIntegerConversions::toOffsetTime);
        CONVERSION_DB.put(BigDecimal.class, OffsetTime.class, 0L, BigDecimalConversions::toOffsetTime);
        CONVERSION_DB.put(OffsetTime.class, OffsetTime.class, 0L, Converter::identity);
        CONVERSION_DB.put(OffsetDateTime.class, OffsetTime.class, 0L, OffsetDateTimeConversions::toOffsetTime);
        CONVERSION_DB.put(Map.class, OffsetTime.class, 0L, MapConversions::toOffsetTime);
        CONVERSION_DB.put(String.class, OffsetTime.class, 0L, StringConversions::toOffsetTime);

        // UUID conversions supported
        CONVERSION_DB.put(Void.class, UUID.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(UUID.class, UUID.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, UUID.class, 0L, StringConversions::toUUID);
        CONVERSION_DB.put(Boolean.class, UUID.class, 0L, BooleanConversions::toUUID);
        CONVERSION_DB.put(BigInteger.class, UUID.class, 0L, BigIntegerConversions::toUUID);
        CONVERSION_DB.put(BigDecimal.class, UUID.class, 0L, BigDecimalConversions::toUUID);
        CONVERSION_DB.put(Map.class, UUID.class, 0L, MapConversions::toUUID);

        // Class conversions supported
        CONVERSION_DB.put(Void.class, Class.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Class.class, Class.class, 0L, Converter::identity);
        CONVERSION_DB.put(Map.class, Class.class, 0L, MapConversions::toClass);
        CONVERSION_DB.put(String.class, Class.class, 0L, StringConversions::toClass);

        // Color conversions supported
        CONVERSION_DB.put(Void.class, Color.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Color.class, Color.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, Color.class, 0L, StringConversions::toColor);
        CONVERSION_DB.put(Map.class, Color.class, 0L, MapConversions::toColor);
        CONVERSION_DB.put(Integer.class, Color.class, 0L, NumberConversions::toColor);
        CONVERSION_DB.put(Long.class, Color.class, 0L, NumberConversions::toColor);
        CONVERSION_DB.put(int[].class, Color.class, 0L, ArrayConversions::toColor);

        // Dimension conversions supported
        CONVERSION_DB.put(Void.class, Dimension.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Dimension.class, Dimension.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, Dimension.class, 0L, StringConversions::toDimension);
        CONVERSION_DB.put(Map.class, Dimension.class, 0L, MapConversions::toDimension);
        CONVERSION_DB.put(Integer.class, Dimension.class, 0L, NumberConversions::toDimension);
        CONVERSION_DB.put(Long.class, Dimension.class, 0L, NumberConversions::toDimension);
        CONVERSION_DB.put(BigInteger.class, Dimension.class, 0L, NumberConversions::toDimension);
        CONVERSION_DB.put(BigDecimal.class, Dimension.class, 0L, NumberConversions::bigDecimalToDimension);
        CONVERSION_DB.put(Boolean.class, Dimension.class, 0L, NumberConversions::booleanToDimension);
        CONVERSION_DB.put(int[].class, Dimension.class, 0L, ArrayConversions::toDimension);
        CONVERSION_DB.put(Rectangle.class, Dimension.class, 0L, RectangleConversions::toDimension);
        CONVERSION_DB.put(Insets.class, Dimension.class, 0L, InsetsConversions::toDimension);
        CONVERSION_DB.put(Point.class, Dimension.class, 0L, PointConversions::toDimension);

        // Point conversions supported
        CONVERSION_DB.put(Void.class, Point.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Point.class, Point.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, Point.class, 0L, StringConversions::toPoint);
        CONVERSION_DB.put(Map.class, Point.class, 0L, MapConversions::toPoint);
        CONVERSION_DB.put(Integer.class, Point.class, 0L, NumberConversions::toPoint);
        CONVERSION_DB.put(Long.class, Point.class, 0L, NumberConversions::toPoint);
        CONVERSION_DB.put(BigInteger.class, Point.class, 0L, NumberConversions::toPoint);
        CONVERSION_DB.put(BigDecimal.class, Point.class, 0L, NumberConversions::bigDecimalToPoint);
        CONVERSION_DB.put(Boolean.class, Point.class, 0L, NumberConversions::booleanToPoint);
        CONVERSION_DB.put(int[].class, Point.class, 0L, ArrayConversions::toPoint);
        CONVERSION_DB.put(Dimension.class, Point.class, 0L, DimensionConversions::toPoint);
        CONVERSION_DB.put(Rectangle.class, Point.class, 0L, RectangleConversions::toPoint);
        CONVERSION_DB.put(Insets.class, Point.class, 0L, InsetsConversions::toPoint);

        // Rectangle conversions supported
        CONVERSION_DB.put(Void.class, Rectangle.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Rectangle.class, Rectangle.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, Rectangle.class, 0L, StringConversions::toRectangle);
        CONVERSION_DB.put(Map.class, Rectangle.class, 0L, MapConversions::toRectangle);
        CONVERSION_DB.put(Integer.class, Rectangle.class, 0L, NumberConversions::integerToRectangle);
        CONVERSION_DB.put(Long.class, Rectangle.class, 0L, NumberConversions::longToRectangle);
        CONVERSION_DB.put(BigInteger.class, Rectangle.class, 0L, NumberConversions::bigIntegerToRectangle);
        CONVERSION_DB.put(BigDecimal.class, Rectangle.class, 0L, NumberConversions::bigDecimalToRectangle);
        CONVERSION_DB.put(Boolean.class, Rectangle.class, 0L, NumberConversions::booleanToRectangle);
        CONVERSION_DB.put(int[].class, Rectangle.class, 0L, ArrayConversions::toRectangle);
        CONVERSION_DB.put(Point.class, Rectangle.class, 0L, PointConversions::toRectangle);
        CONVERSION_DB.put(Dimension.class, Rectangle.class, 0L, DimensionConversions::toRectangle);
        CONVERSION_DB.put(Insets.class, Rectangle.class, 0L, InsetsConversions::toRectangle);

        // Insets conversions supported
        CONVERSION_DB.put(Void.class, Insets.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Insets.class, Insets.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, Insets.class, 0L, StringConversions::toInsets);
        CONVERSION_DB.put(Map.class, Insets.class, 0L, MapConversions::toInsets);
        CONVERSION_DB.put(Integer.class, Insets.class, 0L, NumberConversions::integerToInsets);
        CONVERSION_DB.put(Long.class, Insets.class, 0L, NumberConversions::longToInsets);
        CONVERSION_DB.put(BigInteger.class, Insets.class, 0L, NumberConversions::bigIntegerToInsets);
        CONVERSION_DB.put(BigDecimal.class, Insets.class, 0L, NumberConversions::bigDecimalToInsets);
        CONVERSION_DB.put(Boolean.class, Insets.class, 0L, NumberConversions::booleanToInsets);
        CONVERSION_DB.put(int[].class, Insets.class, 0L, ArrayConversions::toInsets);
        CONVERSION_DB.put(Point.class, Insets.class, 0L, PointConversions::toInsets);
        CONVERSION_DB.put(Dimension.class, Insets.class, 0L, DimensionConversions::toInsets);
        CONVERSION_DB.put(Rectangle.class, Insets.class, 0L, RectangleConversions::toInsets);

        // toFile
        CONVERSION_DB.put(Void.class, File.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(File.class, File.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, File.class, 0L, StringConversions::toFile);
        CONVERSION_DB.put(Map.class, File.class, 0L, MapConversions::toFile);
        CONVERSION_DB.put(URI.class, File.class, 0L, UriConversions::toFile);
        CONVERSION_DB.put(Path.class, File.class, 0L, PathConversions::toFile);
        CONVERSION_DB.put(char[].class, File.class, 0L, ArrayConversions::charArrayToFile);
        CONVERSION_DB.put(byte[].class, File.class, 0L, ArrayConversions::byteArrayToFile);

        // toPath
        CONVERSION_DB.put(Void.class, Path.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Path.class, Path.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, Path.class, 0L, StringConversions::toPath);
        CONVERSION_DB.put(Map.class, Path.class, 0L, MapConversions::toPath);
        CONVERSION_DB.put(URI.class, Path.class, 0L, UriConversions::toPath);
        CONVERSION_DB.put(File.class, Path.class, 0L, FileConversions::toPath);
        CONVERSION_DB.put(char[].class, Path.class, 0L, ArrayConversions::charArrayToPath);
        CONVERSION_DB.put(byte[].class, Path.class, 0L, ArrayConversions::byteArrayToPath);

        // Locale conversions supported
        CONVERSION_DB.put(Void.class, Locale.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Locale.class, Locale.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, Locale.class, 0L, StringConversions::toLocale);
        CONVERSION_DB.put(Map.class, Locale.class, 0L, MapConversions::toLocale);

        // String conversions supported
        CONVERSION_DB.put(Void.class, String.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, String.class, 0L, StringConversions::toString);
        CONVERSION_DB.put(Short.class, String.class, 0L, StringConversions::toString);
        CONVERSION_DB.put(Integer.class, String.class, 0L, StringConversions::toString);
        CONVERSION_DB.put(Long.class, String.class, 0L, StringConversions::toString);
        CONVERSION_DB.put(Float.class, String.class, 0L, NumberConversions::floatToString);
        CONVERSION_DB.put(Double.class, String.class, 0L, NumberConversions::doubleToString);
        CONVERSION_DB.put(Boolean.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(Character.class, String.class, 0L, CharacterConversions::toString);
        CONVERSION_DB.put(BigInteger.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(BigDecimal.class, String.class, 0L, BigDecimalConversions::toString);
        CONVERSION_DB.put(byte[].class, String.class, 0L, ByteArrayConversions::toString);
        CONVERSION_DB.put(char[].class, String.class, 0L, CharArrayConversions::toString);
        CONVERSION_DB.put(Character[].class, String.class, 0L, CharacterArrayConversions::toString);
        CONVERSION_DB.put(ByteBuffer.class, String.class, 0L, ByteBufferConversions::toString);
        CONVERSION_DB.put(CharBuffer.class, String.class, 0L, CharBufferConversions::toString);
        CONVERSION_DB.put(Class.class, String.class, 0L, ClassConversions::toString);
        CONVERSION_DB.put(Date.class, String.class, 0L, DateConversions::toString);
        CONVERSION_DB.put(Calendar.class, String.class, 0L, CalendarConversions::toString);
        CONVERSION_DB.put(java.sql.Date.class, String.class, 0L, SqlDateConversions::toString);
        CONVERSION_DB.put(Timestamp.class, String.class, 0L, TimestampConversions::toString);
        CONVERSION_DB.put(LocalDate.class, String.class, 0L, LocalDateConversions::toString);
        CONVERSION_DB.put(LocalTime.class, String.class, 0L, LocalTimeConversions::toString);
        CONVERSION_DB.put(LocalDateTime.class, String.class, 0L, LocalDateTimeConversions::toString);
        CONVERSION_DB.put(ZonedDateTime.class, String.class, 0L, ZonedDateTimeConversions::toString);
        CONVERSION_DB.put(UUID.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(Color.class, String.class, 0L, ColorConversions::toString);
        CONVERSION_DB.put(Dimension.class, String.class, 0L, DimensionConversions::toString);
        CONVERSION_DB.put(Point.class, String.class, 0L, PointConversions::toString);
        CONVERSION_DB.put(Rectangle.class, String.class, 0L, RectangleConversions::toString);
        CONVERSION_DB.put(Insets.class, String.class, 0L, InsetsConversions::toString);
        CONVERSION_DB.put(Map.class, String.class, 0L, MapConversions::toString);
        CONVERSION_DB.put(Enum.class, String.class, 0L, StringConversions::enumToString);
        CONVERSION_DB.put(String.class, String.class, 0L, Converter::identity);
        CONVERSION_DB.put(Duration.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(Instant.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(MonthDay.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(YearMonth.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(Period.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(ZoneId.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(ZoneOffset.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(OffsetTime.class, String.class, 0L, OffsetTimeConversions::toString);
        CONVERSION_DB.put(OffsetDateTime.class, String.class, 0L, OffsetDateTimeConversions::toString);
        CONVERSION_DB.put(Year.class, String.class, 0L, YearConversions::toString);
        CONVERSION_DB.put(Locale.class, String.class, 0L, LocaleConversions::toString);
        CONVERSION_DB.put(URI.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(URL.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(File.class, String.class, 0L, FileConversions::toString);
        CONVERSION_DB.put(Path.class, String.class, 0L, PathConversions::toString);
        CONVERSION_DB.put(TimeZone.class, String.class, 0L, TimeZoneConversions::toString);
        CONVERSION_DB.put(Pattern.class, String.class, 0L, PatternConversions::toString);
        CONVERSION_DB.put(Currency.class, String.class, 0L, CurrencyConversions::toString);
        CONVERSION_DB.put(StringBuilder.class, String.class, 0L, UniversalConversions::toString);
        CONVERSION_DB.put(StringBuffer.class, String.class, 0L, UniversalConversions::toString);

        // Currency conversions
        CONVERSION_DB.put(Void.class, Currency.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Currency.class, Currency.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, Currency.class, 0L, StringConversions::toCurrency);
        CONVERSION_DB.put(Map.class, Currency.class, 0L, MapConversions::toCurrency);

        // Pattern conversions
        CONVERSION_DB.put(Void.class, Pattern.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Pattern.class, Pattern.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, Pattern.class, 0L, StringConversions::toPattern);
        CONVERSION_DB.put(Map.class, Pattern.class, 0L, MapConversions::toPattern);

        // URL conversions
        CONVERSION_DB.put(Void.class, URL.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(URL.class, URL.class, 0L, Converter::identity);
        CONVERSION_DB.put(URI.class, URL.class, 0L, UriConversions::toURL);
        CONVERSION_DB.put(String.class, URL.class, 0L, StringConversions::toURL);
        CONVERSION_DB.put(Map.class, URL.class, 0L, MapConversions::toURL);
        CONVERSION_DB.put(File.class, URL.class, 0L, FileConversions::toURL);
        CONVERSION_DB.put(Path.class, URL.class, 0L, PathConversions::toURL);

        // URI Conversions
        CONVERSION_DB.put(Void.class, URI.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(URI.class, URI.class, 0L, Converter::identity);
        CONVERSION_DB.put(URL.class, URI.class, 0L, UrlConversions::toURI);
        CONVERSION_DB.put(String.class, URI.class, 0L, StringConversions::toURI);
        CONVERSION_DB.put(Map.class, URI.class, 0L, MapConversions::toURI);
        CONVERSION_DB.put(File.class, URI.class, 0L, FileConversions::toURI);
        CONVERSION_DB.put(Path.class, URI.class, 0L, PathConversions::toURI);

        // TimeZone Conversions
        CONVERSION_DB.put(Void.class, TimeZone.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(TimeZone.class, TimeZone.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, TimeZone.class, 0L, StringConversions::toTimeZone);
        CONVERSION_DB.put(Map.class, TimeZone.class, 0L, MapConversions::toTimeZone);
        CONVERSION_DB.put(ZoneId.class, TimeZone.class, 0L, ZoneIdConversions::toTimeZone);
        CONVERSION_DB.put(ZoneOffset.class, TimeZone.class, 0L, ZoneOffsetConversions::toTimeZone);

        // Duration conversions supported
        CONVERSION_DB.put(Void.class, Duration.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Duration.class, Duration.class, 0L, Converter::identity);
        CONVERSION_DB.put(Long.class, Duration.class, 0L, NumberConversions::longNanosToDuration);
        CONVERSION_DB.put(Double.class, Duration.class, 0L, DoubleConversions::toDuration);
        CONVERSION_DB.put(BigInteger.class, Duration.class, 0L, BigIntegerConversions::toDuration);
        CONVERSION_DB.put(BigDecimal.class, Duration.class, 0L, BigDecimalConversions::toDuration);
        CONVERSION_DB.put(Timestamp.class, Duration.class, 0L, TimestampConversions::toDuration);
        CONVERSION_DB.put(String.class, Duration.class, 0L, StringConversions::toDuration);
        CONVERSION_DB.put(Map.class, Duration.class, 0L, MapConversions::toDuration);

        // Instant conversions supported
        CONVERSION_DB.put(Void.class, Instant.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Instant.class, Instant.class, 0L, Converter::identity);
        CONVERSION_DB.put(Long.class, Instant.class, 0L, NumberConversions::longNanosToInstant);
        CONVERSION_DB.put(Double.class, Instant.class, 0L, DoubleConversions::toInstant);
        CONVERSION_DB.put(BigInteger.class, Instant.class, 0L, BigIntegerConversions::toInstant);
        CONVERSION_DB.put(BigDecimal.class, Instant.class, 0L, BigDecimalConversions::toInstant);
        CONVERSION_DB.put(java.sql.Date.class, Instant.class, 0L, SqlDateConversions::toInstant);
        CONVERSION_DB.put(Timestamp.class, Instant.class, 0L, DateConversions::toInstant);
        CONVERSION_DB.put(Date.class, Instant.class, 0L, DateConversions::toInstant);
        CONVERSION_DB.put(LocalDate.class, Instant.class, 0L, LocalDateConversions::toInstant);
        CONVERSION_DB.put(LocalDateTime.class, Instant.class, 0L, LocalDateTimeConversions::toInstant);
        CONVERSION_DB.put(ZonedDateTime.class, Instant.class, 0L, ZonedDateTimeConversions::toInstant);
        CONVERSION_DB.put(OffsetDateTime.class, Instant.class, 0L, OffsetDateTimeConversions::toInstant);

        CONVERSION_DB.put(String.class, Instant.class, 0L, StringConversions::toInstant);
        CONVERSION_DB.put(Map.class, Instant.class, 0L, MapConversions::toInstant);

        // ZoneId conversions supported
        CONVERSION_DB.put(Void.class, ZoneId.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(ZoneId.class, ZoneId.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, ZoneId.class, 0L, StringConversions::toZoneId);
        CONVERSION_DB.put(Map.class, ZoneId.class, 0L, MapConversions::toZoneId);
        CONVERSION_DB.put(TimeZone.class, ZoneId.class, 0L, TimeZoneConversions::toZoneId);
        CONVERSION_DB.put(ZoneOffset.class, ZoneId.class, 0L, ZoneOffsetConversions::toZoneId);

        // ZoneOffset conversions supported
        CONVERSION_DB.put(Void.class, ZoneOffset.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(ZoneOffset.class, ZoneOffset.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, ZoneOffset.class, 0L, StringConversions::toZoneOffset);
        CONVERSION_DB.put(Map.class, ZoneOffset.class, 0L, MapConversions::toZoneOffset);
        CONVERSION_DB.put(ZoneId.class, ZoneOffset.class, 0L, ZoneIdConversions::toZoneOffset);
        CONVERSION_DB.put(TimeZone.class, ZoneOffset.class, 0L, TimeZoneConversions::toZoneOffset);

        // MonthDay conversions supported
        CONVERSION_DB.put(Void.class, MonthDay.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(MonthDay.class, MonthDay.class, 0L, Converter::identity);
        CONVERSION_DB.put(java.sql.Date.class, MonthDay.class, 0L, SqlDateConversions::toMonthDay);
        CONVERSION_DB.put(Date.class, MonthDay.class, 0L, DateConversions::toMonthDay);
        CONVERSION_DB.put(Timestamp.class, MonthDay.class, 0L, TimestampConversions::toMonthDay);
        CONVERSION_DB.put(LocalDate.class, MonthDay.class, 0L, LocalDateConversions::toMonthDay);
        CONVERSION_DB.put(LocalDateTime.class, MonthDay.class, 0L, LocalDateTimeConversions::toMonthDay);
        CONVERSION_DB.put(ZonedDateTime.class, MonthDay.class, 0L, ZonedDateTimeConversions::toMonthDay);
        CONVERSION_DB.put(OffsetDateTime.class, MonthDay.class, 0L, OffsetDateTimeConversions::toMonthDay);
        CONVERSION_DB.put(String.class, MonthDay.class, 0L, StringConversions::toMonthDay);
        CONVERSION_DB.put(Map.class, MonthDay.class, 0L, MapConversions::toMonthDay);

        // YearMonth conversions supported
        CONVERSION_DB.put(Void.class, YearMonth.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(YearMonth.class, YearMonth.class, 0L, Converter::identity);
        CONVERSION_DB.put(java.sql.Date.class, YearMonth.class, 0L, SqlDateConversions::toYearMonth);
        CONVERSION_DB.put(Date.class, YearMonth.class, 0L, DateConversions::toYearMonth);
        CONVERSION_DB.put(Timestamp.class, YearMonth.class, 0L, TimestampConversions::toYearMonth);
        CONVERSION_DB.put(LocalDate.class, YearMonth.class, 0L, LocalDateConversions::toYearMonth);
        CONVERSION_DB.put(LocalDateTime.class, YearMonth.class, 0L, LocalDateTimeConversions::toYearMonth);
        CONVERSION_DB.put(ZonedDateTime.class, YearMonth.class, 0L, ZonedDateTimeConversions::toYearMonth);
        CONVERSION_DB.put(OffsetDateTime.class, YearMonth.class, 0L, OffsetDateTimeConversions::toYearMonth);
        CONVERSION_DB.put(String.class, YearMonth.class, 0L, StringConversions::toYearMonth);
        CONVERSION_DB.put(Map.class, YearMonth.class, 0L, MapConversions::toYearMonth);

        // Period conversions supported
        CONVERSION_DB.put(Void.class, Period.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Period.class, Period.class, 0L, Converter::identity);
        CONVERSION_DB.put(String.class, Period.class, 0L, StringConversions::toPeriod);
        CONVERSION_DB.put(Map.class, Period.class, 0L, MapConversions::toPeriod);

        // toStringBuffer
        CONVERSION_DB.put(Void.class, StringBuffer.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, StringBuffer.class, 0L, StringConversions::toStringBuffer);

        // toStringBuilder - Bridge through String
        CONVERSION_DB.put(Void.class, StringBuilder.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, StringBuilder.class, 0L, StringConversions::toStringBuilder);

        // toByteArray
        CONVERSION_DB.put(Void.class, byte[].class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, byte[].class, 0L, StringConversions::toByteArray);
        CONVERSION_DB.put(ByteBuffer.class, byte[].class, 0L, ByteBufferConversions::toByteArray);
        CONVERSION_DB.put(CharBuffer.class, byte[].class, 0L, CharBufferConversions::toByteArray);
        CONVERSION_DB.put(char[].class, byte[].class, 0L, VoidConversions::toNull); // advertising convertion, implemented generically in ArrayConversions.
        CONVERSION_DB.put(byte[].class, byte[].class, 0L, Converter::identity);
        CONVERSION_DB.put(File.class, byte[].class, 0L, FileConversions::toByteArray);
        CONVERSION_DB.put(Path.class, byte[].class, 0L, PathConversions::toByteArray);

        // toCharArray
        CONVERSION_DB.put(Void.class, char[].class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, char[].class, 0L, StringConversions::toCharArray);
        CONVERSION_DB.put(ByteBuffer.class, char[].class, 0L, ByteBufferConversions::toCharArray);
        CONVERSION_DB.put(CharBuffer.class, char[].class, 0L, CharBufferConversions::toCharArray);
        CONVERSION_DB.put(char[].class, char[].class, 0L, CharArrayConversions::toCharArray);
        CONVERSION_DB.put(byte[].class, char[].class, 0L, VoidConversions::toNull);   // Used for advertising capability, implemented generically in ArrayConversions.
        CONVERSION_DB.put(File.class, char[].class, 0L, FileConversions::toCharArray);
        CONVERSION_DB.put(Path.class, char[].class, 0L, PathConversions::toCharArray);

        // toCharacterArray
        CONVERSION_DB.put(Void.class, Character[].class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, Character[].class, 0L, StringConversions::toCharacterArray);

        // toCharBuffer
        CONVERSION_DB.put(Void.class, CharBuffer.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, CharBuffer.class, 0L, StringConversions::toCharBuffer);
        CONVERSION_DB.put(ByteBuffer.class, CharBuffer.class, 0L, ByteBufferConversions::toCharBuffer);
        CONVERSION_DB.put(CharBuffer.class, CharBuffer.class, 0L, CharBufferConversions::toCharBuffer);
        CONVERSION_DB.put(char[].class, CharBuffer.class, 0L, CharArrayConversions::toCharBuffer);
        CONVERSION_DB.put(byte[].class, CharBuffer.class, 0L, ByteArrayConversions::toCharBuffer);
        CONVERSION_DB.put(Map.class, CharBuffer.class, 0L, MapConversions::toCharBuffer);

        // toByteBuffer
        CONVERSION_DB.put(Void.class, ByteBuffer.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(String.class, ByteBuffer.class, 0L, StringConversions::toByteBuffer);
        CONVERSION_DB.put(ByteBuffer.class, ByteBuffer.class, 0L, ByteBufferConversions::toByteBuffer);
        CONVERSION_DB.put(CharBuffer.class, ByteBuffer.class, 0L, CharBufferConversions::toByteBuffer);
        CONVERSION_DB.put(char[].class, ByteBuffer.class, 0L, CharArrayConversions::toByteBuffer);
        CONVERSION_DB.put(byte[].class, ByteBuffer.class, 0L, ByteArrayConversions::toByteBuffer);
        CONVERSION_DB.put(Map.class, ByteBuffer.class, 0L, MapConversions::toByteBuffer);

        // toYear
        CONVERSION_DB.put(Void.class, Year.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Year.class, Year.class, 0L, Converter::identity);
        CONVERSION_DB.put(Short.class, Year.class, 0L, NumberConversions::toYear);
        CONVERSION_DB.put(Integer.class, Year.class, 0L, NumberConversions::toYear);
        CONVERSION_DB.put(Long.class, Year.class, 0L, NumberConversions::toYear);
        CONVERSION_DB.put(Float.class, Year.class, 0L, NumberConversions::toYear);
        CONVERSION_DB.put(Double.class, Year.class, 0L, NumberConversions::toYear);
        CONVERSION_DB.put(BigInteger.class, Year.class, 0L, NumberConversions::toYear);
        CONVERSION_DB.put(BigDecimal.class, Year.class, 0L, NumberConversions::toYear);
        CONVERSION_DB.put(java.sql.Date.class, Year.class, 0L, SqlDateConversions::toYear);
        CONVERSION_DB.put(Date.class, Year.class, 0L, DateConversions::toYear);
        CONVERSION_DB.put(Timestamp.class, Year.class, 0L, TimestampConversions::toYear);
        CONVERSION_DB.put(LocalDate.class, Year.class, 0L, LocalDateConversions::toYear);
        CONVERSION_DB.put(LocalDateTime.class, Year.class, 0L, LocalDateTimeConversions::toYear);
        CONVERSION_DB.put(ZonedDateTime.class, Year.class, 0L, ZonedDateTimeConversions::toYear);
        CONVERSION_DB.put(OffsetDateTime.class, Year.class, 0L, OffsetDateTimeConversions::toYear);
        CONVERSION_DB.put(String.class, Year.class, 0L, StringConversions::toYear);
        CONVERSION_DB.put(Map.class, Year.class, 0L, MapConversions::toYear);

        // Throwable conversions supported
        CONVERSION_DB.put(Void.class, Throwable.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Map.class, Throwable.class, 0L, (ConvertWithTarget<Throwable>) MapConversions::toThrowable);

        // Map conversions supported
        CONVERSION_DB.put(Void.class, Map.class, 0L, VoidConversions::toNull);
        CONVERSION_DB.put(Byte.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(Short.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(Integer.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(Long.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(Float.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(Double.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(Boolean.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(Character.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(BigInteger.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(BigDecimal.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(AtomicBoolean.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(AtomicInteger.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(AtomicLong.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(Date.class, Map.class, 0L, DateConversions::toMap);
        CONVERSION_DB.put(java.sql.Date.class, Map.class, 0L, SqlDateConversions::toMap);
        CONVERSION_DB.put(Timestamp.class, Map.class, 0L, TimestampConversions::toMap);
        CONVERSION_DB.put(Calendar.class, Map.class, 0L, CalendarConversions::toMap);  // Restored - bridge produces different map key (zonedDateTime vs calendar)
        CONVERSION_DB.put(LocalDate.class, Map.class, 0L, LocalDateConversions::toMap);
        CONVERSION_DB.put(LocalDateTime.class, Map.class, 0L, LocalDateTimeConversions::toMap);
        CONVERSION_DB.put(ZonedDateTime.class, Map.class, 0L, ZonedDateTimeConversions::toMap);
        CONVERSION_DB.put(Duration.class, Map.class, 0L, DurationConversions::toMap);
        CONVERSION_DB.put(Instant.class, Map.class, 0L, InstantConversions::toMap);
        CONVERSION_DB.put(LocalTime.class, Map.class, 0L, LocalTimeConversions::toMap);
        CONVERSION_DB.put(MonthDay.class, Map.class, 0L, MonthDayConversions::toMap);
        CONVERSION_DB.put(YearMonth.class, Map.class, 0L, YearMonthConversions::toMap);
        CONVERSION_DB.put(Period.class, Map.class, 0L, PeriodConversions::toMap);
        CONVERSION_DB.put(TimeZone.class, Map.class, 0L, TimeZoneConversions::toMap);
        CONVERSION_DB.put(ZoneId.class, Map.class, 0L, ZoneIdConversions::toMap);
        CONVERSION_DB.put(ZoneOffset.class, Map.class, 0L, ZoneOffsetConversions::toMap);
        CONVERSION_DB.put(Class.class, Map.class, 0L, UniversalConversions::toMap);
        CONVERSION_DB.put(UUID.class, Map.class, 0L, UUIDConversions::toMap);
        CONVERSION_DB.put(Color.class, Map.class, 0L, ColorConversions::toMap);
        CONVERSION_DB.put(Dimension.class, Map.class, 0L, DimensionConversions::toMap);
        CONVERSION_DB.put(Point.class, Map.class, 0L, PointConversions::toMap);
        CONVERSION_DB.put(Rectangle.class, Map.class, 0L, RectangleConversions::toMap);
        CONVERSION_DB.put(Insets.class, Map.class, 0L, InsetsConversions::toMap);
        CONVERSION_DB.put(String.class, Map.class, 0L, StringConversions::toMap);
        CONVERSION_DB.put(Enum.class, Map.class, 0L, EnumConversions::toMap);
        CONVERSION_DB.put(OffsetDateTime.class, Map.class, 0L, OffsetDateTimeConversions::toMap);
        CONVERSION_DB.put(OffsetTime.class, Map.class, 0L, OffsetTimeConversions::toMap);
        CONVERSION_DB.put(Year.class, Map.class, 0L, YearConversions::toMap);
        CONVERSION_DB.put(Locale.class, Map.class, 0L, LocaleConversions::toMap);
        CONVERSION_DB.put(URI.class, Map.class, 0L, UriConversions::toMap);
        CONVERSION_DB.put(URL.class, Map.class, 0L, UrlConversions::toMap);
        CONVERSION_DB.put(Throwable.class, Map.class, 0L, ThrowableConversions::toMap);
        CONVERSION_DB.put(Pattern.class, Map.class, 0L, PatternConversions::toMap);
        CONVERSION_DB.put(Currency.class, Map.class, 0L, CurrencyConversions::toMap);
        CONVERSION_DB.put(ByteBuffer.class, Map.class, 0L, ByteBufferConversions::toMap);
        CONVERSION_DB.put(CharBuffer.class, Map.class, 0L, CharBufferConversions::toMap);
        CONVERSION_DB.put(File.class, Map.class, 0L, FileConversions::toMap);
        CONVERSION_DB.put(Path.class, Map.class, 0L, PathConversions::toMap);

        // toIntArray
        CONVERSION_DB.put(Color.class, int[].class, 0L, ColorConversions::toIntArray);
        CONVERSION_DB.put(Dimension.class, int[].class, 0L, DimensionConversions::toIntArray);
        CONVERSION_DB.put(Point.class, int[].class, 0L, PointConversions::toIntArray);
        CONVERSION_DB.put(Rectangle.class, int[].class, 0L, RectangleConversions::toIntArray);
        CONVERSION_DB.put(Insets.class, int[].class, 0L, InsetsConversions::toIntArray);

        // Array-like type bridges for universal array system access
        // ========================================
        // Atomic Array Bridges
        // ========================================

        // AtomicIntegerArray ↔ int[] bridges
        CONVERSION_DB.put(AtomicIntegerArray.class, int[].class, 0L, UniversalConversions::atomicIntegerArrayToIntArray);
        CONVERSION_DB.put(int[].class, AtomicIntegerArray.class, 0L, UniversalConversions::intArrayToAtomicIntegerArray);

        // AtomicLongArray ↔ long[] bridges  
        CONVERSION_DB.put(AtomicLongArray.class, long[].class, 0L, UniversalConversions::atomicLongArrayToLongArray);
        CONVERSION_DB.put(long[].class, AtomicLongArray.class, 0L, UniversalConversions::longArrayToAtomicLongArray);

        // AtomicReferenceArray ↔ Object[] bridges
        CONVERSION_DB.put(AtomicReferenceArray.class, Object[].class, 0L, UniversalConversions::atomicReferenceArrayToObjectArray);
        CONVERSION_DB.put(Object[].class, AtomicReferenceArray.class, 0L, UniversalConversions::objectArrayToAtomicReferenceArray);

        // AtomicReferenceArray ↔ String[] bridges
        CONVERSION_DB.put(AtomicReferenceArray.class, String[].class, 0L, UniversalConversions::atomicReferenceArrayToStringArray);
        CONVERSION_DB.put(String[].class, AtomicReferenceArray.class, 0L, UniversalConversions::stringArrayToAtomicReferenceArray);

        // ========================================
        // NIO Buffer Bridges
        // ========================================

        // IntBuffer ↔ int[] bridges
        CONVERSION_DB.put(IntBuffer.class, int[].class, 0L, UniversalConversions::intBufferToIntArray);
        CONVERSION_DB.put(int[].class, IntBuffer.class, 0L, UniversalConversions::intArrayToIntBuffer);

        // LongBuffer ↔ long[] bridges
        CONVERSION_DB.put(LongBuffer.class, long[].class, 0L, UniversalConversions::longBufferToLongArray);
        CONVERSION_DB.put(long[].class, LongBuffer.class, 0L, UniversalConversions::longArrayToLongBuffer);

        // FloatBuffer ↔ float[] bridges
        CONVERSION_DB.put(FloatBuffer.class, float[].class, 0L, UniversalConversions::floatBufferToFloatArray);
        CONVERSION_DB.put(float[].class, FloatBuffer.class, 0L, UniversalConversions::floatArrayToFloatBuffer);

        // DoubleBuffer ↔ double[] bridges
        CONVERSION_DB.put(DoubleBuffer.class, double[].class, 0L, UniversalConversions::doubleBufferToDoubleArray);
        CONVERSION_DB.put(double[].class, DoubleBuffer.class, 0L, UniversalConversions::doubleArrayToDoubleBuffer);

        // ShortBuffer ↔ short[] bridges
        CONVERSION_DB.put(ShortBuffer.class, short[].class, 0L, UniversalConversions::shortBufferToShortArray);
        CONVERSION_DB.put(short[].class, ShortBuffer.class, 0L, UniversalConversions::shortArrayToShortBuffer);

        // ========================================
        // BitSet Bridges
        // ========================================

        // BitSet ↔ boolean[] bridges
        CONVERSION_DB.put(BitSet.class, boolean[].class, 0L, UniversalConversions::bitSetToBooleanArray);
        CONVERSION_DB.put(boolean[].class, BitSet.class, 0L, UniversalConversions::booleanArrayToBitSet);

        // BitSet ↔ int[] bridges (set bit indices)
        CONVERSION_DB.put(BitSet.class, int[].class, 0L, UniversalConversions::bitSetToIntArray);
        CONVERSION_DB.put(int[].class, BitSet.class, 0L, UniversalConversions::intArrayToBitSet);

        // BitSet ↔ byte[] bridges
        CONVERSION_DB.put(BitSet.class, byte[].class, 0L, UniversalConversions::bitSetToByteArray);
        CONVERSION_DB.put(byte[].class, BitSet.class, 0L, UniversalConversions::byteArrayToBitSet);

        // ========================================
        // Stream Bridges
        // ========================================

        // IntStream ↔ int[] bridges
        CONVERSION_DB.put(IntStream.class, int[].class, 0L, UniversalConversions::intStreamToIntArray);
        CONVERSION_DB.put(int[].class, IntStream.class, 0L, UniversalConversions::intArrayToIntStream);

        // LongStream ↔ long[] bridges
        CONVERSION_DB.put(LongStream.class, long[].class, 0L, UniversalConversions::longStreamToLongArray);
        CONVERSION_DB.put(long[].class, LongStream.class, 0L, UniversalConversions::longArrayToLongStream);

        // DoubleStream ↔ double[] bridges
        CONVERSION_DB.put(DoubleStream.class, double[].class, 0L, UniversalConversions::doubleStreamToDoubleArray);
        CONVERSION_DB.put(double[].class, DoubleStream.class, 0L, UniversalConversions::doubleArrayToDoubleStream);

        // Register Record.class -> Map.class conversion if Records are supported
        try {
            Class<?> recordClass = Class.forName("java.lang.Record");
            CONVERSION_DB.put(recordClass, Map.class, 0L, MapConversions::recordToMap);
        } catch (ClassNotFoundException e) {
            // Records not available in this JVM (JDK < 14)
        }

        // Expand bridge conversions - discover multi-hop paths and add them to CONVERSION_DB
        expandBridgeConversions();

        // CONVERSION_DB is now ready for use (MultiKeyMap is inherently thread-safe)
    }

    /**
     * Cached list of surrogate → primary pairs for one-way expansion.
     */
    private static List<SurrogatePrimaryPair> SURROGATE_TO_PRIMARY_PAIRS = null;

    /**
     * Cached list of primary → surrogate pairs for reverse expansion.
     */
    private static List<SurrogatePrimaryPair> PRIMARY_TO_SURROGATE_PAIRS = null;

    /**
     * List 1: SURROGATE → PRIMARY (surrogateCanReachEverythingPrimaryCanReach)
     * Every "surrogate" on the left can be loss-lessly collapsed to the "primary" on the
     * right, so it is safe to give the surrogate all the outbound conversions that the
     * primary already owns.
     */
    private static List<SurrogatePrimaryPair> getSurrogateToPrimaryPairs() {
        if (SURROGATE_TO_PRIMARY_PAIRS == null) {
            SURROGATE_TO_PRIMARY_PAIRS = Arrays.asList(
                    // Primitives → Wrappers (lossless)
                    new SurrogatePrimaryPair(byte.class, Byte.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(short.class, Short.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(int.class, Integer.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(long.class, Long.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(float.class, Float.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(double.class, Double.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(char.class, Character.class, UniversalConversions::primitiveToWrapper, null),
                    new SurrogatePrimaryPair(boolean.class, Boolean.class, UniversalConversions::primitiveToWrapper, null),

                    // Atomic types → Wrappers (lossless via .get())
                    new SurrogatePrimaryPair(AtomicBoolean.class, Boolean.class,
                            UniversalConversions::atomicBooleanToBoolean, null),
                    new SurrogatePrimaryPair(AtomicInteger.class, Integer.class,
                            UniversalConversions::atomicIntegerToInt, null),
                    new SurrogatePrimaryPair(AtomicLong.class, Long.class,
                            UniversalConversions::atomicLongToLong, null),

                    // String builders → String (lossless via .toString())
                    new SurrogatePrimaryPair(CharSequence.class, String.class,
                            UniversalConversions::charSequenceToString, null),

                    // Resource identifiers → URI (lossless via URL.toURI())
                    new SurrogatePrimaryPair(URL.class, URI.class,
                            UrlConversions::toURI, null),

                    // Year → Long (maximum reach for data pipelines)
                    new SurrogatePrimaryPair(Year.class, Long.class,
                            YearConversions::toLong, null),

                    // YearMonth → String (maximum reach for temporal formatting)
                    new SurrogatePrimaryPair(YearMonth.class, String.class,
                            UniversalConversions::toString, null),

                    // MonthDay → String (maximum reach for temporal formatting)
                    new SurrogatePrimaryPair(MonthDay.class, String.class,
                            UniversalConversions::toString, null),

                    // Duration → Long (numeric reach for time calculations)
                    new SurrogatePrimaryPair(Duration.class, Long.class,
                            DurationConversions::toLong, null),

                    // OffsetTime → String (maximum reach preserving offset info)
                    new SurrogatePrimaryPair(OffsetTime.class, String.class,
                            OffsetTimeConversions::toString, null),

                    // Date & Time
                    new SurrogatePrimaryPair(Calendar.class, ZonedDateTime.class, 
                            UniversalConversions::calendarToZonedDateTime, null)
            );
        }
        return SURROGATE_TO_PRIMARY_PAIRS;
    }

    /**
     * List 2: PRIMARY → SURROGATE (everythingThatCanReachPrimaryCanAlsoReachSurrogate)
     * These pairs let callers land on the surrogate instead of the primary when they
     * are travelling into the ecosystem. They do not guarantee the reverse trip is
     * perfect, so they only belong in this reverse list.
     */
    private static List<SurrogatePrimaryPair> getPrimaryToSurrogatePairs() {
        if (PRIMARY_TO_SURROGATE_PAIRS == null) {
            PRIMARY_TO_SURROGATE_PAIRS = Arrays.asList(
                    // Wrappers → Primitives (safe conversion via auto-unboxing)
                    new SurrogatePrimaryPair(Byte.class, byte.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Short.class, short.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Integer.class, int.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Long.class, long.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Float.class, float.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Double.class, double.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Character.class, char.class, null, UniversalConversions::wrapperToPrimitive),
                    new SurrogatePrimaryPair(Boolean.class, boolean.class, null, UniversalConversions::wrapperToPrimitive),

                    // Wrappers → Atomic types (create new atomic with same value)
                    new SurrogatePrimaryPair(Boolean.class, AtomicBoolean.class, null,
                            UniversalConversions::booleanToAtomicBoolean),
                    new SurrogatePrimaryPair(Integer.class, AtomicInteger.class, null,
                            UniversalConversions::integerToAtomicInteger),
                    new SurrogatePrimaryPair(Long.class, AtomicLong.class, null,
                            UniversalConversions::longToAtomicLong),

                    // String → String builders (create new mutable builder)
                    new SurrogatePrimaryPair(String.class, StringBuffer.class, null,
                            UniversalConversions::stringToStringBuffer),
                    new SurrogatePrimaryPair(String.class, StringBuilder.class, null,
                            UniversalConversions::stringToStringBuilder),
                    new SurrogatePrimaryPair(String.class, CharSequence.class, null,
                            UniversalConversions::stringToCharSequence),

                    // URI → URL (convert URI to URL for legacy compatibility)
                    new SurrogatePrimaryPair(URI.class, URL.class, null,
                            UriConversions::toURL)
            );
        }
        return PRIMARY_TO_SURROGATE_PAIRS;
    }

    /**
     * Represents a surrogate-primary class pair with bi-directional bridge conversion functions.
     */
    private static class SurrogatePrimaryPair {
        final Class<?> surrogateClass;
        final Class<?> primaryClass;
        final Convert<?> surrogateToPrimaryConversion;
        final Convert<?> primaryToSurrogateConversion;

        SurrogatePrimaryPair(Class<?> surrogateClass, Class<?> primaryClass,
                             Convert<?> surrogateToPrimaryConversion, Convert<?> primaryToSurrogateConversion) {
            this.surrogateClass = surrogateClass;
            this.primaryClass = primaryClass;
            this.surrogateToPrimaryConversion = surrogateToPrimaryConversion;
            this.primaryToSurrogateConversion = primaryToSurrogateConversion;
        }
    }

    /**
     * Direction enumeration for bridge expansion operations.
     */
    private enum BridgeDirection {
        SURROGATE_TO_PRIMARY,
        PRIMARY_TO_SURROGATE
    }

    /**
     * Expands bridge conversions by discovering multi-hop conversion paths and adding them to CONVERSION_DB.
     * This allows for code reduction by eliminating redundant conversion definitions while maintaining
     * the same or greater conversion capabilities.
     * <p>
     * For example, if we have:
     * - AtomicInteger → Integer (bridge)
     * - Integer → String (direct conversion)
     * <p>
     * This method will discover the AtomicInteger → String path and add it to CONVERSION_DB
     * as a composite conversion function.
     */
    private static void expandBridgeConversions() {
        // Track original size for logging
        int originalSize = CONVERSION_DB.size();

        // Expand all configured surrogate bridges in both directions
        expandSurrogateBridges(BridgeDirection.SURROGATE_TO_PRIMARY);
        expandSurrogateBridges(BridgeDirection.PRIMARY_TO_SURROGATE);

        int expandedSize = CONVERSION_DB.size();
        // TODO: Add logging when ready
        // System.out.println("Expanded CONVERSION_DB from " + originalSize + " to " + expandedSize + " entries via bridge discovery");
    }

    /**
     * Consolidated method for expanding surrogate bridges in both directions.
     * Creates composite conversion functions that bridge through intermediate types.
     * 
     * @param direction The direction of bridge expansion (SURROGATE_TO_PRIMARY or PRIMARY_TO_SURROGATE)
     */
    private static void expandSurrogateBridges(BridgeDirection direction) {
        // Create a snapshot of existing pairs to avoid ConcurrentModificationException
        Set<ConversionPair> existingPairs = new HashSet<>();
        for (MultiKeyMap.MultiKeyEntry<Convert<?>> entry : CONVERSION_DB.entries()) {
            // Skip entries that don't follow the classic (Class, Class, long) pattern
            // This includes coconut-wrapped single-key entries and other N-Key entries
            if (entry.keys.length >= 3) {
                Object source = entry.keys[0];
                Object target = entry.keys[1];
                if (source instanceof Class && target instanceof Class) {
                    // Handle both Integer and Long instance IDs 
                    Object instanceIdObj = entry.keys[2];
                    long instanceId = (instanceIdObj instanceof Integer) ? ((Integer) instanceIdObj).longValue() : (Long) instanceIdObj;
                    existingPairs.add(pair((Class<?>) source, (Class<?>) target, instanceId));
                }
            }
        }

        // Get the appropriate configuration list based on direction
        List<SurrogatePrimaryPair> configs = (direction == BridgeDirection.SURROGATE_TO_PRIMARY) ?
            getSurrogateToPrimaryPairs() : getPrimaryToSurrogatePairs();

        // Process each surrogate configuration
        for (SurrogatePrimaryPair config : configs) {
            if (direction == BridgeDirection.SURROGATE_TO_PRIMARY) {
                // FORWARD BRIDGES: Surrogate → Primary → Target
                // Example: int.class → Integer.class → String.class
                Class<?> surrogateClass = config.surrogateClass;
                Class<?> primaryClass = config.primaryClass;

                // Find all targets that the primary class can convert to
                for (ConversionPair pair : existingPairs) {
                    if (pair.source.equals(primaryClass)) {
                        Class<?> targetClass = pair.target;
                        // Only add if not already defined and not converting to itself
                        if (CONVERSION_DB.get(surrogateClass, targetClass, 0L) == null && !targetClass.equals(surrogateClass)) {
                            // Create composite conversion: Surrogate → primary → target
                            Convert<?> originalConversion = CONVERSION_DB.get(pair.getSource(), pair.getTarget(), pair.getInstanceId());
                            Convert<?> bridgeConversion = createSurrogateToPrimaryBridgeConversion(config, originalConversion);
                            CONVERSION_DB.put(surrogateClass, targetClass, 0L, bridgeConversion);
                        }
                    }
                }
            } else {
                // REVERSE BRIDGES: Source → Primary → Surrogate
                // Example: String.class → Integer.class → int.class
                Class<?> primaryClass = config.surrogateClass;  // Note: in List 2, surrogate is the source
                Class<?> surrogateClass = config.primaryClass;  // and primary is the target

                // Find all sources that can convert to the primary class
                for (ConversionPair pair : existingPairs) {
                    if (pair.target.equals(primaryClass)) {
                        Class<?> sourceClass = pair.source;
                        // Only add if not already defined and not converting from itself
                        if (CONVERSION_DB.get(sourceClass, surrogateClass, 0L) == null && !sourceClass.equals(surrogateClass)) {
                            // Create composite conversion: Source → primary → surrogate
                            Convert<?> originalConversion = CONVERSION_DB.get(pair.getSource(), pair.getTarget(), pair.getInstanceId());
                            Convert<?> bridgeConversion = createPrimaryToSurrogateBridgeConversion(config, originalConversion);
                            CONVERSION_DB.put(sourceClass, surrogateClass, 0L, bridgeConversion);
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a composite conversion function that bridges from surrogate type to target via primary.
     * Uses the configured bridge conversion to extract primary value, then applies existing primary conversion.
     */
    private static Convert<?> createSurrogateToPrimaryBridgeConversion(SurrogatePrimaryPair config, Convert<?> primaryToTargetConversion) {
        Convert<?> surrogateToPrimaryConversion = config.surrogateToPrimaryConversion;
        if (surrogateToPrimaryConversion == null) {
            throw new IllegalArgumentException("No surrogate→primary conversion found for: " + config.surrogateClass);
        }

        return (from, converter) -> {
            // First: Convert surrogate to primary (e.g., int → Integer, AtomicInteger → Integer)
            Object primaryValue = surrogateToPrimaryConversion.convert(from, converter);
            // Second: Convert primary to target using existing conversion
            return primaryToTargetConversion.convert(primaryValue, converter);
        };
    }

    /**
     * Creates a composite conversion function that bridges from source type to surrogate via primary.
     * Uses the existing source-to-primary conversion, then applies configured primary-to-surrogate bridge.
     */
    private static Convert<?> createPrimaryToSurrogateBridgeConversion(SurrogatePrimaryPair config, Convert<?> sourceToPrimaryConversion) {
        Convert<?> primaryToSurrogateConversion = config.primaryToSurrogateConversion;
        if (primaryToSurrogateConversion == null) {
            throw new IllegalArgumentException("No primary→surrogate conversion found for: " + config.primaryClass);
        }

        return (from, converter) -> {
            // First: Convert source to primary using existing conversion  
            Object primaryValue = sourceToPrimaryConversion.convert(from, converter);
            // Second: Convert primary to surrogate (e.g., Integer → int, Integer → AtomicInteger)
            return primaryToSurrogateConversion.convert(primaryValue, converter);
        };
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
        this.instanceId = INSTANCE_ID_GENERATOR.getAndIncrement();
        for (Map.Entry<ConversionPair, Convert<?>> entry : this.options.getConverterOverrides().entrySet()) {
            ConversionPair pair = entry.getKey();
            USER_DB.put(pair.getSource(), pair.getTarget(), pair.getInstanceId(), entry.getValue());
        }
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
     *     <li><b>Buffer Types:</b> Convert between buffer types (e.g., {@link ByteBuffer} to {@link String}, {@link CharBuffer} to {@link Byte}[]).</li>
     * </ul>
     * </p>
     *
     * <h3>Extensibility:</h3>
     * <p>
     * Users can extend the Converter's capabilities by registering custom converters for specific type pairs.
     * This is achieved using the {@link #addConversion(Class, Class, Convert)} method, which accepts the source type,
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
            // For null inputs, use Void.class so that e.g. convert(null, int.class) returns 0.
            sourceType = Void.class;
            // Also check the cache for (Void.class, toType) to avoid redundant lookups.
            Convert<?> cached = getCachedConverter(sourceType, toType);
            if (cached != null) {
                return (T) cached.convert(from, this, toType);
            }
        } else {
            sourceType = from.getClass();
            Convert<?> cached = getCachedConverter(sourceType, toType);
            if (cached != null) {
                return (T) cached.convert(from, this, toType);
            }
            // Try container conversion first (Arrays, Collections, Maps).
            T result = attemptContainerConversion(from, sourceType, toType);
            if (result != null) {
                return result;
            }
        }

        // Check user-added conversions first with instance-specific precision.
        Convert<?> conversionMethod = USER_DB.get(sourceType, toType, this.instanceId);
        if (isValidConversion(conversionMethod)) {
            cacheConverter(sourceType, toType, conversionMethod);
            return (T) conversionMethod.convert(from, this, toType);
        }

        // Then check the factory conversion database.
        conversionMethod = CONVERSION_DB.get(sourceType, toType, this.instanceId);
        if (isValidConversion(conversionMethod)) {
            // Cache built-in conversions with instance ID 0 to keep them shared across instances
            FULL_CONVERSION_CACHE.put(sourceType, toType, 0L, conversionMethod);
            // Also cache with current instance ID for faster future lookup
            cacheConverter(sourceType, toType, conversionMethod);
            return (T) conversionMethod.convert(from, this, toType);
        }

        // Attempt inheritance-based conversion.
        conversionMethod = getInheritedConverter(sourceType, toType);
        if (isValidConversion(conversionMethod)) {
            cacheConverter(sourceType, toType, conversionMethod);
            return (T) conversionMethod.convert(from, this, toType);
        }

        // If no specific converter found, check assignment compatibility as fallback [someone is doing convert(linkedMap, Map.class) for example]
        if (from != null && toType.isAssignableFrom(from.getClass())) {
            return (T) from; // Assignment compatible - use as-is
        }

        // Universal Object → Map conversion (only when no specific converter exists)
        if (!(from instanceof Map) && Map.class.isAssignableFrom(toType)) {
            // Skip collections and arrays - they have their own conversion paths
            if (!(from != null && from.getClass().isArray() || from instanceof Collection)) {
                // Create cached converter for Object→Map conversion
                final Class<?> finalToType = toType;
                Convert<?> objectConverter = (fromObj, converter) -> ObjectConversions.objectToMapWithTarget(fromObj, converter, finalToType);

                // Execute and cache successful conversions
                Object result = objectConverter.convert(from, this);
                if (result != null) {
                    cacheConverter(sourceType, toType, objectConverter);
                }
                return (T) result;
            }
        }

        throw new IllegalArgumentException("Unsupported conversion, source type [" + name(from) +
                "] target type '" + getShortName(toType) + "'");
    }

    private Convert<?> getCachedConverter(Class<?> source, Class<?> target) {
        // First check instance-specific cache
        Convert<?> converter = FULL_CONVERSION_CACHE.get(source, target, this.instanceId);
        if (converter != null) {
            return converter;
        }

        // Fall back to shared conversions (instance ID 0)
        return FULL_CONVERSION_CACHE.get(source, target, 0L);
    }

    private void cacheConverter(Class<?> source, Class<?> target, Convert<?> converter) {
        FULL_CONVERSION_CACHE.put(source, target, this.instanceId, converter);
    }

    // Cache JsonObject class to avoid repeated reflection lookups
    private static final Class<?> JSON_OBJECT_CLASS;

    static {
        Class<?> jsonObjectClass;
        try {
            jsonObjectClass = Class.forName("com.cedarsoftware.io.JsonObject");
        } catch (ClassNotFoundException e) {
            // JsonObject not available - use Void.class as a safe fallback that will never match
            jsonObjectClass = Objects.class;
        }
        JSON_OBJECT_CLASS = jsonObjectClass;
    }

    @SuppressWarnings("unchecked")
    private <T> T attemptContainerConversion(Object from, Class<?> sourceType, Class<T> toType) {
        // Validate source type is a container type (Array, Collection, or Map)
        if (!(from.getClass().isArray() || from instanceof Collection || from instanceof Map)) {
            return null;
        }

        // Check for EnumSet target first
        if (EnumSet.class.isAssignableFrom(toType)) {
            throw new IllegalArgumentException("To convert to EnumSet, specify the Enum class to convert to as the 'toType.' Example: EnumSet<Day> daySet = (EnumSet<Day>)(Object)converter.convert(array, Day.class);");
        }

        // Special handling for container → Enum conversions (creates EnumSet)
        if (toType.isEnum()) {
            if (sourceType.isArray() || Collection.class.isAssignableFrom(sourceType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> EnumConversions.toEnumSet(fromObj, toType));
            } else if (Map.class.isAssignableFrom(sourceType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> EnumConversions.toEnumSet(((Map<?, ?>) fromObj).keySet(), toType));
            }
        }
        // EnumSet source conversions
        else if (EnumSet.class.isAssignableFrom(sourceType)) {
            if (Collection.class.isAssignableFrom(toType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> {
                            Collection<Object> target = (Collection<Object>) CollectionHandling.createCollection(fromObj, toType);
                            target.addAll((Collection<?>) fromObj);
                            return target;
                        });
            }
            if (toType.isArray()) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> ArrayConversions.enumSetToArray((EnumSet<?>) fromObj, toType));
            }
        }
        // Collection source conversions
        else if (Collection.class.isAssignableFrom(sourceType)) {
            if (toType.isArray()) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> ArrayConversions.collectionToArray((Collection<?>) fromObj, toType, converter));
            } else if (Collection.class.isAssignableFrom(toType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> CollectionConversions.collectionToCollection((Collection<?>) fromObj, toType));
            }
        }
        // Array source conversions
        else if (sourceType.isArray()) {
            if (Collection.class.isAssignableFrom(toType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> CollectionConversions.arrayToCollection(fromObj, (Class<? extends Collection<?>>) toType));
            } else if (toType.isArray() && !sourceType.getComponentType().equals(toType.getComponentType())) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> ArrayConversions.arrayToArray(fromObj, toType, converter));
            }
        }
        // Map source conversions
        else if (Map.class.isAssignableFrom(sourceType)) {
            if (Map.class.isAssignableFrom(toType)) {
                return executeAndCache(sourceType, toType, from,
                        (fromObj, converter) -> MapConversions.mapToMapWithTarget(fromObj, converter, toType));
            }
        }

        return null;
    }

    /**
     * Helper method to execute a converter and cache it if successful
     */
    @SuppressWarnings("unchecked")
    private <T> T executeAndCache(Class<?> sourceType, Class<?> toType, Object from, Convert<?> converter) {
        Object result = converter.convert(from, this);
        if (result != null) {
            cacheConverter(sourceType, toType, converter);
        }
        return (T) result;
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
     * @param toType     The target type to convert to
     * @return A {@link Convert} instance for the most appropriate conversion, or {@code null} if no suitable converter is found
     */

    private static Convert<?> getInheritedConverter(Class<?> sourceType, Class<?> toType) {
        // Build the complete set of source types (including sourceType itself) with levels.
        Set<ClassLevel> sourceTypes = new TreeSet<>(getSuperClassesAndInterfaces(sourceType));
        sourceTypes.add(new ClassLevel(sourceType, 0));
        // Build the complete set of target types (including toType itself) with levels.
        Set<ClassLevel> targetTypes = new TreeSet<>(getSuperClassesAndInterfaces(toType));
        targetTypes.add(new ClassLevel(toType, 0));

        // Create pairs of source/target types with their associated levels.
        class ConversionPairWithLevel {
            private final ConversionPair pair;
            private final int sourceLevel;
            private final int targetLevel;

            private ConversionPairWithLevel(Class<?> source, Class<?> target, int sourceLevel, int targetLevel) {
                this.pair = Converter.pair(source, target);
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

        // Sort the pairs by a composite of rules:
        // - Exact target matches first.
        // - Then by assignability of the target types.
        // - Then by combined inheritance distance.
        // - Finally, prefer concrete classes over interfaces.
        pairs.sort((p1, p2) -> {
            boolean p1ExactTarget = p1.pair.getTarget() == toType;
            boolean p2ExactTarget = p2.pair.getTarget() == toType;
            if (p1ExactTarget != p2ExactTarget) {
                return p1ExactTarget ? -1 : 1;
            }
            if (p1.pair.getTarget() != p2.pair.getTarget()) {
                boolean p1AssignableToP2 = p2.pair.getTarget().isAssignableFrom(p1.pair.getTarget());
                boolean p2AssignableToP1 = p1.pair.getTarget().isAssignableFrom(p2.pair.getTarget());
                if (p1AssignableToP2 != p2AssignableToP1) {
                    return p1AssignableToP2 ? -1 : 1;
                }
            }
            int dist1 = p1.sourceLevel + p1.targetLevel;
            int dist2 = p2.sourceLevel + p2.targetLevel;
            if (dist1 != dist2) {
                return dist1 - dist2;
            }
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

        // Iterate over sorted pairs and check the converter databases.
        for (ConversionPairWithLevel pairWithLevel : pairs) {
            ConversionPair pair = pairWithLevel.pair;
            Convert<?> tempConverter = USER_DB.get(pair.getSource(), pair.getTarget(), pair.getInstanceId());
            if (tempConverter != null) {
                return tempConverter;
            }
            tempConverter = CONVERSION_DB.get(pair.getSource(), pair.getTarget(), pair.getInstanceId());
            if (tempConverter != null) {
                return tempConverter;
            }
        }
        return null;
    }

    /**
     * Gets a sorted set of all superclasses and interfaces for a class,
     * with their inheritance distances.
     *
     * @param clazz The class to analyze
     * @return Sorted set of ClassLevel objects representing the inheritance hierarchy
     */
    private static SortedSet<ClassLevel> getSuperClassesAndInterfaces(Class<?> clazz) {
        return cacheParentTypes.computeIfAbsent(clazz, key -> {
            SortedSet<ClassLevel> parentTypes = new TreeSet<>();
            ClassUtilities.ClassHierarchyInfo info = ClassUtilities.getClassHierarchyInfo(key);

            for (Map.Entry<Class<?>, Integer> entry : info.getDistanceMap().entrySet()) {
                Class<?> type = entry.getKey();
                int distance = entry.getValue();

                // Skip the class itself and marker interfaces
                if (distance > 0 &&
                        type != Serializable.class &&
                        type != Cloneable.class &&
                        type != Comparable.class &&
                        type != Externalizable.class) {

                    parentTypes.add(new ClassLevel(type, distance));
                }
            }

            return parentTypes;
        });
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
            if (!(obj instanceof ClassLevel)) {
                return false;
            }
            ClassLevel other = (ClassLevel) obj;
            return this.clazz.equals(other.clazz) && this.level == other.level;
        }

        @Override
        public int hashCode() {
            return clazz.hashCode() * 31 + level;
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
     * @param type The class to get the short name for
     * @return The short name of the class
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
     * Determines if a container-based conversion is supported between the specified source and target types.
     * This method checks for valid conversions between arrays, collections, Maps, and EnumSets without actually
     * performing the conversion.
     *
     * <p>Supported conversions include:
     * <ul>
     *   <li>Array to Collection</li>
     *   <li>Collection to Array</li>
     *   <li>Array to Array (when component types differ)</li>
     *   <li>Array, Collection, or Map to EnumSet (when target is an Enum type)</li>
     *   <li>EnumSet to Array or Collection</li>
     * </ul>
     * </p>
     *
     * @param sourceType The source type to convert from
     * @param target     The target type to convert to
     * @return true if a container-based conversion is supported between the types, false otherwise
     * @throws IllegalArgumentException if target is EnumSet.class (caller should specify specific Enum type instead)
     */
    public static boolean isContainerConversionSupported(Class<?> sourceType, Class<?> target) {
        // Quick check: If the source is not an array, a Collection, Map, or an EnumSet, no conversion is supported here.
        if (!(sourceType.isArray() || Collection.class.isAssignableFrom(sourceType) || Map.class.isAssignableFrom(sourceType) || EnumSet.class.isAssignableFrom(sourceType))) {
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
        // For that, the source must be either an array, a collection, or a Map (via keySet) from which we can build the EnumSet.
        if (target.isEnum()) {
            return (sourceType.isArray() || Collection.class.isAssignableFrom(sourceType) || Map.class.isAssignableFrom(sourceType));
        }

        // If the source is an EnumSet, it can be converted to either an array or another collection.
        if (EnumSet.class.isAssignableFrom(sourceType)) {
            return target.isArray() || Collection.class.isAssignableFrom(target);
        }

        // If the source is a generic Collection, we only support converting it to an array or collection
        if (Collection.class.isAssignableFrom(sourceType)) {
            return target.isArray() || Collection.class.isAssignableFrom(target);
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
     * @deprecated Use {@link #isContainerConversionSupported(Class, Class)} instead.
     * This method will be removed in a future version.
     */
    @Deprecated
    public static boolean isCollectionConversionSupported(Class<?> sourceType, Class<?> target) {
        return isContainerConversionSupported(sourceType, target);
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
     * {@code false} if either type is an array/collection or no conversion exists
     * @see #isConversionSupportedFor(Class, Class)
     */
    public boolean isSimpleTypeConversionSupported(Class<?> source, Class<?> target) {
        // First, try to get the converter from the FULL_CONVERSION_CACHE.
        Convert<?> cached = getCachedConverter(source, target);
        if (cached != null) {
            return cached != UNSUPPORTED;
        }

        // If either source or target is a collection/array/map type, this method is not applicable.
        if (source.isArray() || target.isArray() ||
                Collection.class.isAssignableFrom(source) || Collection.class.isAssignableFrom(target) ||
                Map.class.isAssignableFrom(source) || Map.class.isAssignableFrom(target)) {
            return false;
        }

        // Special case: When source is Number, delegate using Long.
        if (source.equals(Number.class)) {
            Convert<?> method = getConversionFromDBs(Long.class, target);
            cacheConverter(source, target, method);
            return isValidConversion(method);
        }

        // Next, check direct conversion support in the primary databases.

        Convert<?> method = getConversionFromDBs(source, target);
        if (isValidConversion(method)) {
            cacheConverter(source, target, method);
            return true;
        }

        // Finally, attempt an inheritance-based lookup.
        method = getInheritedConverter(source, target);
        if (isValidConversion(method)) {
            cacheConverter(source, target, method);
            return true;
        }

        // Cache the failure result so that subsequent lookups are fast.
        cacheConverter(source, target, UNSUPPORTED);
        return false;
    }

    /**
     * Overload of {@link #isSimpleTypeConversionSupported(Class, Class)} that checks
     * if the specified class is considered a simple type.
     * Results are cached for fast subsequent lookups.
     *
     * @param type the class to check
     * @return {@code true} if a simple type conversion exists for the class
     */
    public boolean isSimpleTypeConversionSupported(Class<?> type) {
        return SIMPLE_TYPE_CACHE.computeIfAbsent(type, t -> isSimpleTypeConversionSupported(t, t));
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
     * false otherwise
     */
    public boolean isConversionSupportedFor(Class<?> source, Class<?> target) {
        // First, check the FULL_CONVERSION_CACHE.
        Convert<?> cached = getCachedConverter(source, target);
        if (cached != null) {
            return cached != UNSUPPORTED;
        }

        // Check direct conversion support in the primary databases.
        Convert<?> method = getConversionFromDBs(source, target);
        if (isValidConversion(method)) {
            cacheConverter(source, target, method);
            return true;
        }

        // Handle container conversions (arrays, collections, maps).
        if (isContainerConversionSupported(source, target)) {
            // Special handling for array-to-array conversions:
            if (source.isArray() && target.isArray()) {
                return target.getComponentType() == Object.class ||
                        isConversionSupportedFor(source.getComponentType(), target.getComponentType());
            }
            return true;  // All other collection conversions are supported.
        }

        // Finally, attempt inheritance-based conversion.
        method = getInheritedConverter(source, target);
        if (isValidConversion(method)) {
            cacheConverter(source, target, method);
            return true;
        }
        return false;
    }

    /**
     * Overload of {@link #isConversionSupportedFor(Class, Class)} that checks whether
     * the specified class can be converted to itself.
     * The result is cached for fast repeat access.
     *
     * @param type the class to query
     * @return {@code true} if a conversion exists for the class
     */
    public boolean isConversionSupportedFor(Class<?> type) {
        return SELF_CONVERSION_CACHE.computeIfAbsent(type, t -> isConversionSupportedFor(t, t));
    }

    private static boolean isValidConversion(Convert<?> method) {
        return method != null && method != UNSUPPORTED;
    }

    /**
     * Private helper method to check if a conversion exists directly in USER_DB or CONVERSION_DB.
     *
     * @param source Class of source type.
     * @param target Class of target type.
     * @return Convert instance
     */
    private static Convert<?> getConversionFromDBs(Class<?> source, Class<?> target) {
        source = ClassUtilities.toPrimitiveWrapperClass(source);
        target = ClassUtilities.toPrimitiveWrapperClass(target);
        Convert<?> method = USER_DB.get(source, target, 0L);
        if (isValidConversion(method)) {
            return method;
        }
        method = CONVERSION_DB.get(source, target, 0L);
        if (isValidConversion(method)) {
            return method;
        }
        return UNSUPPORTED;
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
    public static Map<Class<?>, Set<Class<?>>> allSupportedConversions() {
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
    public static Map<String, Set<String>> getSupportedConversions() {
        Map<String, Set<String>> toFrom = new TreeMap<>(String::compareTo);
        addSupportedConversionName(CONVERSION_DB, toFrom);
        addSupportedConversionName(USER_DB, toFrom);
        return toFrom;
    }

    /**
     * Populates the provided map with supported conversions from the specified conversion database.
     *
     * @param db     The conversion database containing conversion mappings.
     * @param toFrom The map to populate with supported conversions.
     */
    private static void addSupportedConversion(MultiKeyMap<Convert<?>> db, Map<Class<?>, Set<Class<?>>> toFrom) {
        for (MultiKeyMap.MultiKeyEntry<Convert<?>> entry : db.entries()) {
            if (entry.value != UNSUPPORTED && entry.keys.length >= 2) {
                Object source = entry.keys[0];
                Object target = entry.keys[1];
                if (source instanceof Class && target instanceof Class) {
                    toFrom.computeIfAbsent((Class<?>) source, k -> new TreeSet<>(Comparator.comparing((Class<?> c) -> c.getName()))).add((Class<?>) target);
                }
            }
        }
    }

    /**
     * Populates the provided map with supported conversions from the specified conversion database, using class names.
     *
     * @param db     The conversion database containing conversion mappings.
     * @param toFrom The map to populate with supported conversions by class names.
     */
    private static void addSupportedConversionName(MultiKeyMap<Convert<?>> db, Map<String, Set<String>> toFrom) {
        for (MultiKeyMap.MultiKeyEntry<Convert<?>> entry : db.entries()) {
            if (entry.value != UNSUPPORTED && entry.keys.length >= 2) {
                Object source = entry.keys[0];
                Object target = entry.keys[1];
                if (source instanceof Class && target instanceof Class) {
                    toFrom.computeIfAbsent(getShortName((Class<?>) source), k -> new TreeSet<>(String::compareTo)).add(getShortName((Class<?>) target));
                }
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
     * @param source           The source class (type) to convert from.
     * @param target           The target class (type) to convert to.
     * @param conversionMethod A method that converts an instance of the source type to an instance of the target type.
     * @return The previous conversion method associated with the source and target types, or {@code null} if no conversion existed.
     */
    public static Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionMethod) {
        // Collect all type variations (primitive and wrapper) for both source and target
        Set<Class<?>> sourceTypes = getTypeVariations(source);
        Set<Class<?>> targetTypes = getTypeVariations(target);

        // Clear caches for all combinations
        for (Class<?> srcType : sourceTypes) {
            for (Class<?> tgtType : targetTypes) {
                clearCachesForType(srcType, tgtType);
            }
        }

        // Store the wrapper version first to capture return value
        Class<?> wrapperSource = ClassUtilities.toPrimitiveWrapperClass(source);
        Class<?> wrapperTarget = ClassUtilities.toPrimitiveWrapperClass(target);
        Convert<?> previous = USER_DB.get(wrapperSource, wrapperTarget, 0L);
        USER_DB.put(wrapperSource, wrapperTarget, 0L, conversionMethod);

        // Add all type combinations to USER_DB
        for (Class<?> srcType : sourceTypes) {
            for (Class<?> tgtType : targetTypes) {
                USER_DB.put(srcType, tgtType, 0L, conversionMethod);
            }
        }

        return previous;
    }

    /**
     * John's suggested addConversionDB method: Like addConversion() but writes to CONVERSION_DB.
     * This handles primitive/wrapper expansion automatically and is used for factory conversions.
     */
    private static void addConversionDB(Class<?> source, Class<?> target, Convert<?> conversionMethod) {
        // Collect all type variations (primitive and wrapper) for both source and target
        Set<Class<?>> sourceTypes = getTypeVariations(source);
        Set<Class<?>> targetTypes = getTypeVariations(target);

        // Add all type combinations to CONVERSION_DB (static factory conversions with ID 0)
        for (Class<?> srcType : sourceTypes) {
            for (Class<?> tgtType : targetTypes) {
                CONVERSION_DB.put(srcType, tgtType, 0L, conversionMethod);
            }
        }
    }

    /**
     * Helper method to get all type variations (primitive and wrapper) for a given class.
     */
    private static Set<Class<?>> getTypeVariations(Class<?> clazz) {
        Set<Class<?>> types = new HashSet<>();
        types.add(clazz);
        
        if (clazz.isPrimitive()) {
            // If it's primitive, add the wrapper
            types.add(ClassUtilities.toPrimitiveWrapperClass(clazz));
        } else {
            // If it's not primitive, check if it's a wrapper and add the primitive
            Class<?> primitive = ClassUtilities.toPrimitiveClass(clazz);
            if (primitive != clazz) {  // toPrimitiveClass returns same class if not a wrapper
                types.add(primitive);
            }
        }
        
        return types;
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

    private static void clearCachesForType(Class<?> source, Class<?> target) {
        // Note: Since cache keys now include instance ID, we need to clear all cache entries
        // that match the source/target classes regardless of instance. This is less efficient
        // but necessary for the static addConversion API.
        
        // Collect keys to remove (can't modify during iteration)
        java.util.List<Object[]> keysToRemove = new java.util.ArrayList<>();
        for (MultiKeyMap.MultiKeyEntry<Convert<?>> entry : FULL_CONVERSION_CACHE.entries()) {
            if (entry.keys.length >= 2) {
                Object keySource = entry.keys[0];
                Object keyTarget = entry.keys[1];
                if (keySource instanceof Class && keyTarget instanceof Class) {
                    Class<?> sourceClass = (Class<?>) keySource;
                    Class<?> targetClass = (Class<?>) keyTarget;
                    if ((sourceClass == source && targetClass == target) ||
                        // Also clear inheritance-based entries
                        isInheritanceRelated(sourceClass, targetClass, source, target)) {
                        keysToRemove.add(entry.keys.clone());
                    }
                }
            }
        }
        
        // Remove the collected keys
        for (Object[] keys : keysToRemove) {
            if (keys.length >= 3) {
                FULL_CONVERSION_CACHE.remove((Class<?>) keys[0], (Class<?>) keys[1], (Long) keys[2]);
            }
        }

        SIMPLE_TYPE_CACHE.remove(source);
        SIMPLE_TYPE_CACHE.remove(target);
        SELF_CONVERSION_CACHE.remove(source);
        SELF_CONVERSION_CACHE.remove(target);
    }

    private static boolean isInheritanceRelated(Class<?> keySource, Class<?> keyTarget, Class<?> source, Class<?> target) {
        // Check if this cache entry might be affected by inheritance-based lookups
        return (keySource != source && (source.isAssignableFrom(keySource) || keySource.isAssignableFrom(source))) ||
                (keyTarget != target && (target.isAssignableFrom(keyTarget) || keyTarget.isAssignableFrom(target)));
    }
}
