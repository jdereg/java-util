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
import com.cedarsoftware.util.MultiKeyMap;

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
    
    // Identity converter for marking non-standard types and handling identity conversions
    private static final Convert<?> IDENTITY_CONVERTER = (source, converter) -> source;
    
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
        CONVERSION_DB.put(Converter::identity, Byte.class, Number.class, 0L);
        CONVERSION_DB.put(Converter::identity, Short.class, Number.class, 0L);
        CONVERSION_DB.put(Converter::identity, Integer.class, Number.class, 0L);
        CONVERSION_DB.put(Converter::identity, Long.class, Number.class, 0L);
        CONVERSION_DB.put(Converter::identity, Float.class, Number.class, 0L);
        CONVERSION_DB.put(Converter::identity, Double.class, Number.class, 0L);
        CONVERSION_DB.put(Converter::identity, AtomicInteger.class, Number.class, 0L);
        CONVERSION_DB.put(Converter::identity, AtomicLong.class, Number.class, 0L);
        CONVERSION_DB.put(Converter::identity, BigInteger.class, Number.class, 0L);
        CONVERSION_DB.put(Converter::identity, BigDecimal.class, Number.class, 0L);
        CONVERSION_DB.put(DurationConversions::toNumber, Duration.class, Number.class, 0L);

        // toByte
        CONVERSION_DB.put(NumberConversions::toByteZero, Void.class, byte.class, 0L);
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Byte.class, 0L);
        CONVERSION_DB.put(Converter::identity, Byte.class, Byte.class, 0L);
        CONVERSION_DB.put(NumberConversions::toByte, Short.class, Byte.class, 0L);
        CONVERSION_DB.put(NumberConversions::toByte, Integer.class, Byte.class, 0L);
        CONVERSION_DB.put(NumberConversions::toByte, Long.class, Byte.class, 0L);
        CONVERSION_DB.put(NumberConversions::toByte, Float.class, Byte.class, 0L);
        CONVERSION_DB.put(NumberConversions::toByte, Double.class, Byte.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toByte, Boolean.class, Byte.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toByte, Character.class, Byte.class, 0L);
        CONVERSION_DB.put(NumberConversions::toByte, BigInteger.class, Byte.class, 0L);
        CONVERSION_DB.put(NumberConversions::toByte, BigDecimal.class, Byte.class, 0L);
        CONVERSION_DB.put(MapConversions::toByte, Map.class, Byte.class, 0L);
        CONVERSION_DB.put(StringConversions::toByte, String.class, Byte.class, 0L);

        // toShort
        CONVERSION_DB.put(NumberConversions::toShortZero, Void.class, short.class, 0L);
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Short.class, 0L);
        CONVERSION_DB.put(NumberConversions::toShort, Byte.class, Short.class, 0L);
        CONVERSION_DB.put(Converter::identity, Short.class, Short.class, 0L);
        CONVERSION_DB.put(NumberConversions::toShort, Integer.class, Short.class, 0L);
        CONVERSION_DB.put(NumberConversions::toShort, Long.class, Short.class, 0L);
        CONVERSION_DB.put(NumberConversions::toShort, Float.class, Short.class, 0L);
        CONVERSION_DB.put(NumberConversions::toShort, Double.class, Short.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toShort, Boolean.class, Short.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toShort, Character.class, Short.class, 0L);
        CONVERSION_DB.put(NumberConversions::toShort, BigInteger.class, Short.class, 0L);
        CONVERSION_DB.put(NumberConversions::toShort, BigDecimal.class, Short.class, 0L);
        CONVERSION_DB.put(MapConversions::toShort, Map.class, Short.class, 0L);
        CONVERSION_DB.put(StringConversions::toShort, String.class, Short.class, 0L);
        CONVERSION_DB.put(YearConversions::toShort, Year.class, Short.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toShort, MonthDay.class, Short.class, 0L);

        // toInteger
        CONVERSION_DB.put(NumberConversions::toIntZero, Void.class, int.class, 0L);
        CONVERSION_DB.put(UniversalConversions::atomicIntegerToInt, AtomicInteger.class, int.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toInt, MonthDay.class, int.class, 0L);
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Integer.class, 0L);
        CONVERSION_DB.put(NumberConversions::toInt, Byte.class, Integer.class, 0L);
        CONVERSION_DB.put(NumberConversions::toInt, Short.class, Integer.class, 0L);
        CONVERSION_DB.put(Converter::identity, Integer.class, Integer.class, 0L);
        CONVERSION_DB.put(NumberConversions::toInt, Long.class, Integer.class, 0L);
        CONVERSION_DB.put(NumberConversions::toInt, Float.class, Integer.class, 0L);
        CONVERSION_DB.put(NumberConversions::toInt, Double.class, Integer.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toInt, Boolean.class, Integer.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toInt, Character.class, Integer.class, 0L);
        CONVERSION_DB.put(NumberConversions::toInt, AtomicInteger.class, Integer.class, 0L);
        CONVERSION_DB.put(NumberConversions::toInt, BigInteger.class, Integer.class, 0L);
        CONVERSION_DB.put(NumberConversions::toInt, BigDecimal.class, Integer.class, 0L);
        CONVERSION_DB.put(MapConversions::toInt, Map.class, Integer.class, 0L);
        CONVERSION_DB.put(StringConversions::toInt, String.class, Integer.class, 0L);
        CONVERSION_DB.put(ColorConversions::toInteger, Color.class, Integer.class, 0L);
        CONVERSION_DB.put(DimensionConversions::toInteger, Dimension.class, Integer.class, 0L);
        CONVERSION_DB.put(PointConversions::toInteger, Point.class, Integer.class, 0L);
        CONVERSION_DB.put(RectangleConversions::toInteger, Rectangle.class, Integer.class, 0L);
        CONVERSION_DB.put(InsetsConversions::toInteger, Insets.class, Integer.class, 0L);
        CONVERSION_DB.put(OffsetTimeConversions::toInteger, OffsetTime.class, Integer.class, 0L);
        CONVERSION_DB.put(YearConversions::toInt, Year.class, Integer.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toInteger, MonthDay.class, Integer.class, 0L);

        // toLong
        CONVERSION_DB.put(NumberConversions::toLongZero, Void.class, long.class, 0L);
        CONVERSION_DB.put(UniversalConversions::atomicLongToLong, AtomicLong.class, long.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toLong, MonthDay.class, long.class, 0L);
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Long.class, 0L);
        CONVERSION_DB.put(NumberConversions::toLong, Byte.class, Long.class, 0L);
        CONVERSION_DB.put(NumberConversions::toLong, Short.class, Long.class, 0L);
        CONVERSION_DB.put(NumberConversions::toLong, Integer.class, Long.class, 0L);
        CONVERSION_DB.put(Converter::identity, Long.class, Long.class, 0L);
        CONVERSION_DB.put(NumberConversions::toLong, Float.class, Long.class, 0L);
        CONVERSION_DB.put(NumberConversions::toLong, Double.class, Long.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toLong, Boolean.class, Long.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toLong, Character.class, Long.class, 0L);
        CONVERSION_DB.put(NumberConversions::toLong, BigInteger.class, Long.class, 0L);
        CONVERSION_DB.put(NumberConversions::toLong, BigDecimal.class, Long.class, 0L);
        CONVERSION_DB.put(NumberConversions::toLong, AtomicLong.class, Long.class, 0L);
        CONVERSION_DB.put(DateConversions::toLong, Date.class, Long.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toLong, java.sql.Date.class, Long.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toLong, Timestamp.class, Long.class, 0L);
        CONVERSION_DB.put(InstantConversions::toLong, Instant.class, Long.class, 0L);
        CONVERSION_DB.put(DurationConversions::toLong, Duration.class, Long.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toLong, LocalDate.class, Long.class, 0L);
        CONVERSION_DB.put(LocalTimeConversions::toLong, LocalTime.class, Long.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toLong, LocalDateTime.class, Long.class, 0L);
        CONVERSION_DB.put(OffsetTimeConversions::toLong, OffsetTime.class, Long.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toLong, OffsetDateTime.class, Long.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toLong, ZonedDateTime.class, Long.class, 0L);
        CONVERSION_DB.put(MapConversions::toLong, Map.class, Long.class, 0L);
        CONVERSION_DB.put(StringConversions::toLong, String.class, Long.class, 0L);
        CONVERSION_DB.put(ColorConversions::toLong, Color.class, Long.class, 0L);
        CONVERSION_DB.put(DimensionConversions::toLong, Dimension.class, Long.class, 0L);
        CONVERSION_DB.put(PointConversions::toLong, Point.class, Long.class, 0L);
        CONVERSION_DB.put(RectangleConversions::toLong, Rectangle.class, Long.class, 0L);
        CONVERSION_DB.put(InsetsConversions::toLong, Insets.class, Long.class, 0L);
        CONVERSION_DB.put(YearConversions::toLong, Year.class, Long.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toLong, MonthDay.class, Long.class, 0L);

        // toFloat
        CONVERSION_DB.put(NumberConversions::toFloatZero, Void.class, float.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toFloat, MonthDay.class, float.class, 0L);
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Float.class, 0L);
        CONVERSION_DB.put(NumberConversions::toFloat, Byte.class, Float.class, 0L);
        CONVERSION_DB.put(NumberConversions::toFloat, Short.class, Float.class, 0L);
        CONVERSION_DB.put(NumberConversions::toFloat, Integer.class, Float.class, 0L);
        CONVERSION_DB.put(NumberConversions::toFloat, Long.class, Float.class, 0L);
        CONVERSION_DB.put(Converter::identity, Float.class, Float.class, 0L);
        CONVERSION_DB.put(NumberConversions::toFloat, Double.class, Float.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toFloat, Boolean.class, Float.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toFloat, Character.class, Float.class, 0L);
        CONVERSION_DB.put(NumberConversions::toFloat, BigInteger.class, Float.class, 0L);
        CONVERSION_DB.put(NumberConversions::toFloat, BigDecimal.class, Float.class, 0L);
        CONVERSION_DB.put(MapConversions::toFloat, Map.class, Float.class, 0L);
        CONVERSION_DB.put(StringConversions::toFloat, String.class, Float.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toFloat, MonthDay.class, Float.class, 0L);
        CONVERSION_DB.put(YearConversions::toFloat, Year.class, Float.class, 0L);

        // toDouble
        CONVERSION_DB.put(NumberConversions::toDoubleZero, Void.class, double.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toDouble, MonthDay.class, double.class, 0L);
        CONVERSION_DB.put(YearConversions::toDouble, Year.class, double.class, 0L);
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Double.class, 0L);
        CONVERSION_DB.put(NumberConversions::toDouble, Byte.class, Double.class, 0L);
        CONVERSION_DB.put(NumberConversions::toDouble, Short.class, Double.class, 0L);
        CONVERSION_DB.put(NumberConversions::toDouble, Integer.class, Double.class, 0L);
        CONVERSION_DB.put(NumberConversions::toDouble, Long.class, Double.class, 0L);
        CONVERSION_DB.put(NumberConversions::toDouble, Float.class, Double.class, 0L);
        CONVERSION_DB.put(Converter::identity, Double.class, Double.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toDouble, Boolean.class, Double.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toDouble, Character.class, Double.class, 0L);
        CONVERSION_DB.put(DurationConversions::toDouble, Duration.class, Double.class, 0L);
        CONVERSION_DB.put(InstantConversions::toDouble, Instant.class, Double.class, 0L);
        CONVERSION_DB.put(LocalTimeConversions::toDouble, LocalTime.class, Double.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toDouble, LocalDate.class, Double.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toDouble, LocalDateTime.class, Double.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toDouble, ZonedDateTime.class, Double.class, 0L);
        CONVERSION_DB.put(OffsetTimeConversions::toDouble, OffsetTime.class, Double.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toDouble, OffsetDateTime.class, Double.class, 0L);
        CONVERSION_DB.put(DateConversions::toDouble, Date.class, Double.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toDouble, java.sql.Date.class, Double.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toDouble, Timestamp.class, Double.class, 0L);
        CONVERSION_DB.put(NumberConversions::toDouble, BigInteger.class, Double.class, 0L);
        CONVERSION_DB.put(NumberConversions::toDouble, BigDecimal.class, Double.class, 0L);
        CONVERSION_DB.put(MapConversions::toDouble, Map.class, Double.class, 0L);
        CONVERSION_DB.put(StringConversions::toDouble, String.class, Double.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toDouble, MonthDay.class, Double.class, 0L);
        CONVERSION_DB.put(YearConversions::toDouble, Year.class, Double.class, 0L);

        // Boolean/boolean conversions supported
        CONVERSION_DB.put(VoidConversions::toBoolean, Void.class, boolean.class, 0L);
        CONVERSION_DB.put(UniversalConversions::atomicBooleanToBoolean, AtomicBoolean.class, boolean.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toBoolean, MonthDay.class, boolean.class, 0L);
        CONVERSION_DB.put(DurationConversions::toBoolean, Duration.class, boolean.class, 0L);
        CONVERSION_DB.put(AtomicBooleanConversions::toBoolean, AtomicBoolean.class, Boolean.class, 0L);
        CONVERSION_DB.put(DurationConversions::toBooleanWrapper, Duration.class, Boolean.class, 0L);
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Boolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::isIntTypeNotZero, Byte.class, Boolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::isIntTypeNotZero, Short.class, Boolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::isIntTypeNotZero, Integer.class, Boolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::isIntTypeNotZero, Long.class, Boolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::isFloatTypeNotZero, Float.class, Boolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::isFloatTypeNotZero, Double.class, Boolean.class, 0L);
        CONVERSION_DB.put(Converter::identity, Boolean.class, Boolean.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toBoolean, Character.class, Boolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::isBigIntegerNotZero, BigInteger.class, Boolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::isBigDecimalNotZero, BigDecimal.class, Boolean.class, 0L);
        CONVERSION_DB.put(MapConversions::toBoolean, Map.class, Boolean.class, 0L);
        CONVERSION_DB.put(StringConversions::toBoolean, String.class, Boolean.class, 0L);
        CONVERSION_DB.put(DimensionConversions::toBoolean, Dimension.class, Boolean.class, 0L);
        CONVERSION_DB.put(PointConversions::toBoolean, Point.class, Boolean.class, 0L);
        CONVERSION_DB.put(RectangleConversions::toBoolean, Rectangle.class, Boolean.class, 0L);
        CONVERSION_DB.put(InsetsConversions::toBoolean, Insets.class, Boolean.class, 0L);
        CONVERSION_DB.put(UUIDConversions::toBoolean, UUID.class, Boolean.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toBoolean, MonthDay.class, Boolean.class, 0L);

        // Character/char conversions supported
        CONVERSION_DB.put(VoidConversions::toCharacter, Void.class, char.class, 0L);
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Character.class, 0L);
        CONVERSION_DB.put(ByteConversions::toCharacter, Byte.class, Character.class, 0L);
        CONVERSION_DB.put(NumberConversions::toCharacter, Short.class, Character.class, 0L);
        CONVERSION_DB.put(NumberConversions::toCharacter, Integer.class, Character.class, 0L);
        CONVERSION_DB.put(NumberConversions::toCharacter, Long.class, Character.class, 0L);
        CONVERSION_DB.put(NumberConversions::toCharacter, Float.class, Character.class, 0L);
        CONVERSION_DB.put(NumberConversions::toCharacter, Double.class, Character.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toCharacter, Boolean.class, Character.class, 0L);
        CONVERSION_DB.put(Converter::identity, Character.class, Character.class, 0L);
        CONVERSION_DB.put(NumberConversions::toCharacter, BigInteger.class, Character.class, 0L);
        CONVERSION_DB.put(NumberConversions::toCharacter, BigDecimal.class, Character.class, 0L);
        CONVERSION_DB.put(MapConversions::toCharacter, Map.class, Character.class, 0L);
        CONVERSION_DB.put(StringConversions::toCharacter, String.class, Character.class, 0L);

        // BigInteger versions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, BigInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::integerTypeToBigInteger, Byte.class, BigInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::integerTypeToBigInteger, Short.class, BigInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::integerTypeToBigInteger, Integer.class, BigInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::integerTypeToBigInteger, Long.class, BigInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::floatingPointToBigInteger, Float.class, BigInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::floatingPointToBigInteger, Double.class, BigInteger.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toBigInteger, Boolean.class, BigInteger.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toBigInteger, Character.class, BigInteger.class, 0L);
        CONVERSION_DB.put(Converter::identity, BigInteger.class, BigInteger.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toBigInteger, BigDecimal.class, BigInteger.class, 0L);
        CONVERSION_DB.put(DateConversions::toBigInteger, Date.class, BigInteger.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toBigInteger, java.sql.Date.class, BigInteger.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toBigInteger, Timestamp.class, BigInteger.class, 0L);
        CONVERSION_DB.put(DurationConversions::toBigInteger, Duration.class, BigInteger.class, 0L);
        CONVERSION_DB.put(InstantConversions::toBigInteger, Instant.class, BigInteger.class, 0L);
        CONVERSION_DB.put(LocalTimeConversions::toBigInteger, LocalTime.class, BigInteger.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toBigInteger, LocalDate.class, BigInteger.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toBigInteger, LocalDateTime.class, BigInteger.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toBigInteger, ZonedDateTime.class, BigInteger.class, 0L);
        CONVERSION_DB.put(OffsetTimeConversions::toBigInteger, OffsetTime.class, BigInteger.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toBigInteger, OffsetDateTime.class, BigInteger.class, 0L);
        CONVERSION_DB.put(UUIDConversions::toBigInteger, UUID.class, BigInteger.class, 0L);
        CONVERSION_DB.put(ColorConversions::toBigInteger, Color.class, BigInteger.class, 0L);
        CONVERSION_DB.put(DimensionConversions::toBigInteger, Dimension.class, BigInteger.class, 0L);
        CONVERSION_DB.put(PointConversions::toBigInteger, Point.class, BigInteger.class, 0L);
        CONVERSION_DB.put(RectangleConversions::toBigInteger, Rectangle.class, BigInteger.class, 0L);
        CONVERSION_DB.put(InsetsConversions::toBigInteger, Insets.class, BigInteger.class, 0L);
        CONVERSION_DB.put(CalendarConversions::toBigInteger, Calendar.class, BigInteger.class, 0L);  // Restored - bridge has precision difference (millis vs. nanos)
        CONVERSION_DB.put(MapConversions::toBigInteger, Map.class, BigInteger.class, 0L);
        CONVERSION_DB.put(StringConversions::toBigInteger, String.class, BigInteger.class, 0L);
        CONVERSION_DB.put(YearConversions::toBigInteger, Year.class, BigInteger.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toBigInteger, MonthDay.class, BigInteger.class, 0L);

        // BigDecimal conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(NumberConversions::integerTypeToBigDecimal, Byte.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(NumberConversions::integerTypeToBigDecimal, Short.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(NumberConversions::integerTypeToBigDecimal, Integer.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(NumberConversions::integerTypeToBigDecimal, Long.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(NumberConversions::floatingPointToBigDecimal, Float.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(NumberConversions::floatingPointToBigDecimal, Double.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toBigDecimal, Boolean.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toBigDecimal, Character.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(Converter::identity, BigDecimal.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toBigDecimal, BigInteger.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(DateConversions::toBigDecimal, Date.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toBigDecimal, java.sql.Date.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toBigDecimal, Timestamp.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(InstantConversions::toBigDecimal, Instant.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(DurationConversions::toBigDecimal, Duration.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(LocalTimeConversions::toBigDecimal, LocalTime.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toBigDecimal, LocalDate.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toBigDecimal, LocalDateTime.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toBigDecimal, ZonedDateTime.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(OffsetTimeConversions::toBigDecimal, OffsetTime.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toBigDecimal, OffsetDateTime.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(UUIDConversions::toBigDecimal, UUID.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(ColorConversions::toBigDecimal, Color.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(DimensionConversions::toBigDecimal, Dimension.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(InsetsConversions::toBigDecimal, Insets.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(PointConversions::toBigDecimal, Point.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(RectangleConversions::toBigDecimal, Rectangle.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(CalendarConversions::toBigDecimal, Calendar.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(MapConversions::toBigDecimal, Map.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(StringConversions::toBigDecimal, String.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(YearConversions::toBigDecimal, Year.class, BigDecimal.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toBigDecimal, MonthDay.class, BigDecimal.class, 0L);

        // AtomicBoolean conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(DurationConversions::toAtomicBoolean, Duration.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicBoolean, Byte.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicBoolean, Short.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicBoolean, Integer.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicBoolean, Long.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicBoolean, Float.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicBoolean, Double.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toAtomicBoolean, Boolean.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toAtomicBoolean, Character.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicBoolean, BigInteger.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicBoolean, BigDecimal.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(AtomicBooleanConversions::toAtomicBoolean, AtomicBoolean.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(MapConversions::toAtomicBoolean, Map.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(StringConversions::toAtomicBoolean, String.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(YearConversions::toAtomicBoolean, Year.class, AtomicBoolean.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toAtomicBoolean, MonthDay.class, AtomicBoolean.class, 0L);

        // AtomicInteger conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicInteger, Byte.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicInteger, Short.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicInteger, Integer.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicInteger, Long.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicInteger, Float.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicInteger, Double.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toAtomicInteger, Boolean.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toAtomicInteger, Character.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicInteger, BigInteger.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicInteger, BigDecimal.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(AtomicIntegerConversions::toAtomicInteger, AtomicInteger.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(OffsetTimeConversions::toAtomicInteger, OffsetTime.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(MapConversions::toAtomicInteger, Map.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(StringConversions::toAtomicInteger, String.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(YearConversions::toAtomicInteger, Year.class, AtomicInteger.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toAtomicInteger, MonthDay.class, AtomicInteger.class, 0L);

        // AtomicLong conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicLong, Byte.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicLong, Short.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicLong, Integer.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicLong, Long.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicLong, Float.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicLong, Double.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toAtomicLong, Boolean.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toAtomicLong, Character.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicLong, BigInteger.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(NumberConversions::toAtomicLong, BigDecimal.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(AtomicLongConversions::toAtomicLong, AtomicLong.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(DateConversions::toAtomicLong, Date.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toAtomicLong, java.sql.Date.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(DateConversions::toAtomicLong, Timestamp.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(InstantConversions::toAtomicLong, Instant.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(DurationConversions::toAtomicLong, Duration.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toAtomicLong, LocalDate.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(LocalTimeConversions::toAtomicLong, LocalTime.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toAtomicLong, LocalDateTime.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toAtomicLong, ZonedDateTime.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(OffsetTimeConversions::toAtomicLong, OffsetTime.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(OffsetTimeConversions::toAtomicLong, OffsetTime.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toAtomicLong, OffsetDateTime.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(MapConversions::toAtomicLong, Map.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(StringConversions::toAtomicLong, String.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(YearConversions::toAtomicLong, Year.class, AtomicLong.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toAtomicLong, MonthDay.class, AtomicLong.class, 0L);

        // Date conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Date.class, 0L);
        CONVERSION_DB.put(NumberConversions::toDate, Long.class, Date.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toDate, Double.class, Date.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toDate, BigInteger.class, Date.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toDate, BigDecimal.class, Date.class, 0L);
        CONVERSION_DB.put(DateConversions::toDate, Date.class, Date.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toDate, java.sql.Date.class, Date.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toDate, Timestamp.class, Date.class, 0L);
        CONVERSION_DB.put(InstantConversions::toDate, Instant.class, Date.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toDate, LocalDate.class, Date.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toDate, LocalDateTime.class, Date.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toDate, ZonedDateTime.class, Date.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toDate, OffsetDateTime.class, Date.class, 0L);
        CONVERSION_DB.put(DurationConversions::toDate, Duration.class, Date.class, 0L);
        CONVERSION_DB.put(MapConversions::toDate, Map.class, Date.class, 0L);
        CONVERSION_DB.put(StringConversions::toDate, String.class, Date.class, 0L);

        // java.sql.Date conversion supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(NumberConversions::toSqlDate, Long.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toSqlDate, Double.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toSqlDate, BigInteger.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toSqlDate, BigDecimal.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toSqlDate, java.sql.Date.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(DateConversions::toSqlDate, Date.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toSqlDate, Timestamp.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(DurationConversions::toSqlDate, Duration.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(InstantConversions::toSqlDate, Instant.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toSqlDate, LocalDate.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toSqlDate, LocalDateTime.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toSqlDate, ZonedDateTime.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toSqlDate, OffsetDateTime.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(MapConversions::toSqlDate, Map.class, java.sql.Date.class, 0L);
        CONVERSION_DB.put(StringConversions::toSqlDate, String.class, java.sql.Date.class, 0L);

        // Timestamp conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Timestamp.class, 0L);
        CONVERSION_DB.put(NumberConversions::toTimestamp, Long.class, Timestamp.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toTimestamp, Double.class, Timestamp.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toTimestamp, BigInteger.class, Timestamp.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toTimestamp, BigDecimal.class, Timestamp.class, 0L);
        CONVERSION_DB.put(DateConversions::toTimestamp, Timestamp.class, Timestamp.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toTimestamp, java.sql.Date.class, Timestamp.class, 0L);
        CONVERSION_DB.put(DateConversions::toTimestamp, Date.class, Timestamp.class, 0L);
        CONVERSION_DB.put(DurationConversions::toTimestamp, Duration.class, Timestamp.class, 0L);
        CONVERSION_DB.put(InstantConversions::toTimestamp, Instant.class, Timestamp.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toTimestamp, LocalDate.class, Timestamp.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toTimestamp, LocalDateTime.class, Timestamp.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toTimestamp, ZonedDateTime.class, Timestamp.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toTimestamp, OffsetDateTime.class, Timestamp.class, 0L);
        CONVERSION_DB.put(MapConversions::toTimestamp, Map.class, Timestamp.class, 0L);
        CONVERSION_DB.put(StringConversions::toTimestamp, String.class, Timestamp.class, 0L);

        // Calendar conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Calendar.class, 0L);
        CONVERSION_DB.put(NumberConversions::toCalendar, Long.class, Calendar.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toCalendar, Double.class, Calendar.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toCalendar, BigInteger.class, Calendar.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toCalendar, BigDecimal.class, Calendar.class, 0L);
        CONVERSION_DB.put(DateConversions::toCalendar, Date.class, Calendar.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toCalendar, java.sql.Date.class, Calendar.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toCalendar, Timestamp.class, Calendar.class, 0L);
        CONVERSION_DB.put(InstantConversions::toCalendar, Instant.class, Calendar.class, 0L);
        CONVERSION_DB.put(LocalTimeConversions::toCalendar, LocalTime.class, Calendar.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toCalendar, LocalDate.class, Calendar.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toCalendar, LocalDateTime.class, Calendar.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toCalendar, ZonedDateTime.class, Calendar.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toCalendar, OffsetDateTime.class, Calendar.class, 0L);
        CONVERSION_DB.put(DurationConversions::toCalendar, Duration.class, Calendar.class, 0L);
        CONVERSION_DB.put(CalendarConversions::clone, Calendar.class, Calendar.class, 0L);
        CONVERSION_DB.put(MapConversions::toCalendar, Map.class, Calendar.class, 0L);
        CONVERSION_DB.put(StringConversions::toCalendar, String.class, Calendar.class, 0L);

        // LocalDate conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, LocalDate.class, 0L);
        CONVERSION_DB.put(NumberConversions::toLocalDate, Long.class, LocalDate.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toLocalDate, Double.class, LocalDate.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toLocalDate, BigInteger.class, LocalDate.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toLocalDate, BigDecimal.class, LocalDate.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toLocalDate, java.sql.Date.class, LocalDate.class, 0L);
        CONVERSION_DB.put(DateConversions::toLocalDate, Timestamp.class, LocalDate.class, 0L);
        CONVERSION_DB.put(DateConversions::toLocalDate, Date.class, LocalDate.class, 0L);
        CONVERSION_DB.put(InstantConversions::toLocalDate, Instant.class, LocalDate.class, 0L);
        CONVERSION_DB.put(CalendarConversions::toLocalDate, Calendar.class, LocalDate.class, 0L);
        CONVERSION_DB.put(Converter::identity, LocalDate.class, LocalDate.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toLocalDate, LocalDateTime.class, LocalDate.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toLocalDate, ZonedDateTime.class, LocalDate.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toLocalDate, OffsetDateTime.class, LocalDate.class, 0L);
        CONVERSION_DB.put(DurationConversions::toLocalDate, Duration.class, LocalDate.class, 0L);
        CONVERSION_DB.put(MapConversions::toLocalDate, Map.class, LocalDate.class, 0L);
        CONVERSION_DB.put(StringConversions::toLocalDate, String.class, LocalDate.class, 0L);

        // LocalDateTime conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(NumberConversions::toLocalDateTime, Long.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toLocalDateTime, Double.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toLocalDateTime, BigInteger.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toLocalDateTime, BigDecimal.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toLocalDateTime, java.sql.Date.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toLocalDateTime, Timestamp.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(DateConversions::toLocalDateTime, Date.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(InstantConversions::toLocalDateTime, Instant.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toLocalDateTime, LocalDateTime.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toLocalDateTime, LocalDate.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(CalendarConversions::toLocalDateTime, Calendar.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toLocalDateTime, ZonedDateTime.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toLocalDateTime, OffsetDateTime.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(DurationConversions::toLocalDateTime, Duration.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(MapConversions::toLocalDateTime, Map.class, LocalDateTime.class, 0L);
        CONVERSION_DB.put(StringConversions::toLocalDateTime, String.class, LocalDateTime.class, 0L);

        // LocalTime conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, LocalTime.class, 0L);
        CONVERSION_DB.put(NumberConversions::longNanosToLocalTime, Long.class, LocalTime.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toLocalTime, Double.class, LocalTime.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toLocalTime, BigInteger.class, LocalTime.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toLocalTime, BigDecimal.class, LocalTime.class, 0L);
        CONVERSION_DB.put(DateConversions::toLocalTime, Timestamp.class, LocalTime.class, 0L);
        CONVERSION_DB.put(DateConversions::toLocalTime, Date.class, LocalTime.class, 0L);
        CONVERSION_DB.put(InstantConversions::toLocalTime, Instant.class, LocalTime.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toLocalTime, LocalDateTime.class, LocalTime.class, 0L);
        CONVERSION_DB.put(Converter::identity, LocalTime.class, LocalTime.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toLocalTime, ZonedDateTime.class, LocalTime.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toLocalTime, OffsetDateTime.class, LocalTime.class, 0L);
        CONVERSION_DB.put(DurationConversions::toLocalTime, Duration.class, LocalTime.class, 0L);
        CONVERSION_DB.put(MapConversions::toLocalTime, Map.class, LocalTime.class, 0L);
        CONVERSION_DB.put(StringConversions::toLocalTime, String.class, LocalTime.class, 0L);

        // ZonedDateTime conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(NumberConversions::toZonedDateTime, Long.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toZonedDateTime, Double.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toZonedDateTime, BigInteger.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toZonedDateTime, BigDecimal.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toZonedDateTime, java.sql.Date.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(DateConversions::toZonedDateTime, Timestamp.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(DateConversions::toZonedDateTime, Date.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(InstantConversions::toZonedDateTime, Instant.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toZonedDateTime, LocalDate.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toZonedDateTime, LocalDateTime.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(Converter::identity, ZonedDateTime.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toZonedDateTime, OffsetDateTime.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(CalendarConversions::toZonedDateTime, Calendar.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(DurationConversions::toZonedDateTime, Duration.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(MapConversions::toZonedDateTime, Map.class, ZonedDateTime.class, 0L);
        CONVERSION_DB.put(StringConversions::toZonedDateTime, String.class, ZonedDateTime.class, 0L);

        // toOffsetDateTime
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(Converter::identity, OffsetDateTime.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(MapConversions::toOffsetDateTime, Map.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(StringConversions::toOffsetDateTime, String.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(NumberConversions::toOffsetDateTime, Long.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toOffsetDateTime, Double.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toOffsetDateTime, BigInteger.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toOffsetDateTime, BigDecimal.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toOffsetDateTime, java.sql.Date.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(DateConversions::toOffsetDateTime, Date.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toOffsetDateTime, Timestamp.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toOffsetDateTime, LocalDate.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(InstantConversions::toOffsetDateTime, Instant.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toOffsetDateTime, ZonedDateTime.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toOffsetDateTime, LocalDateTime.class, OffsetDateTime.class, 0L);
        CONVERSION_DB.put(DurationConversions::toOffsetDateTime, Duration.class, OffsetDateTime.class, 0L);

        // toOffsetTime
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, OffsetTime.class, 0L);
        CONVERSION_DB.put(NumberConversions::toOffsetTime, Integer.class, OffsetTime.class, 0L);
        CONVERSION_DB.put(NumberConversions::toOffsetTime, Long.class, OffsetTime.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toOffsetTime, Double.class, OffsetTime.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toOffsetTime, BigInteger.class, OffsetTime.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toOffsetTime, BigDecimal.class, OffsetTime.class, 0L);
        CONVERSION_DB.put(Converter::identity, OffsetTime.class, OffsetTime.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toOffsetTime, OffsetDateTime.class, OffsetTime.class, 0L);
        CONVERSION_DB.put(MapConversions::toOffsetTime, Map.class, OffsetTime.class, 0L);
        CONVERSION_DB.put(StringConversions::toOffsetTime, String.class, OffsetTime.class, 0L);

        // UUID conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, UUID.class, 0L);
        CONVERSION_DB.put(Converter::identity, UUID.class, UUID.class, 0L);
        CONVERSION_DB.put(StringConversions::toUUID, String.class, UUID.class, 0L);
        CONVERSION_DB.put(BooleanConversions::toUUID, Boolean.class, UUID.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toUUID, BigInteger.class, UUID.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toUUID, BigDecimal.class, UUID.class, 0L);
        CONVERSION_DB.put(MapConversions::toUUID, Map.class, UUID.class, 0L);

        // Class conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Class.class, 0L);
        CONVERSION_DB.put(Converter::identity, Class.class, Class.class, 0L);
        CONVERSION_DB.put(MapConversions::toClass, Map.class, Class.class, 0L);
        CONVERSION_DB.put(StringConversions::toClass, String.class, Class.class, 0L);

        // Color conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Color.class, 0L);
        CONVERSION_DB.put(Converter::identity, Color.class, Color.class, 0L);
        CONVERSION_DB.put(StringConversions::toColor, String.class, Color.class, 0L);
        CONVERSION_DB.put(MapConversions::toColor, Map.class, Color.class, 0L);
        CONVERSION_DB.put(NumberConversions::toColor, Integer.class, Color.class, 0L);
        CONVERSION_DB.put(NumberConversions::toColor, Long.class, Color.class, 0L);
        CONVERSION_DB.put(ArrayConversions::toColor, int[].class, Color.class, 0L);

        // Dimension conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Dimension.class, 0L);
        CONVERSION_DB.put(Converter::identity, Dimension.class, Dimension.class, 0L);
        CONVERSION_DB.put(StringConversions::toDimension, String.class, Dimension.class, 0L);
        CONVERSION_DB.put(MapConversions::toDimension, Map.class, Dimension.class, 0L);
        CONVERSION_DB.put(NumberConversions::toDimension, Integer.class, Dimension.class, 0L);
        CONVERSION_DB.put(NumberConversions::toDimension, Long.class, Dimension.class, 0L);
        CONVERSION_DB.put(NumberConversions::toDimension, BigInteger.class, Dimension.class, 0L);
        CONVERSION_DB.put(NumberConversions::bigDecimalToDimension, BigDecimal.class, Dimension.class, 0L);
        CONVERSION_DB.put(NumberConversions::booleanToDimension, Boolean.class, Dimension.class, 0L);
        CONVERSION_DB.put(ArrayConversions::toDimension, int[].class, Dimension.class, 0L);
        CONVERSION_DB.put(RectangleConversions::toDimension, Rectangle.class, Dimension.class, 0L);
        CONVERSION_DB.put(InsetsConversions::toDimension, Insets.class, Dimension.class, 0L);
        CONVERSION_DB.put(PointConversions::toDimension, Point.class, Dimension.class, 0L);

        // Point conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Point.class, 0L);
        CONVERSION_DB.put(Converter::identity, Point.class, Point.class, 0L);
        CONVERSION_DB.put(StringConversions::toPoint, String.class, Point.class, 0L);
        CONVERSION_DB.put(MapConversions::toPoint, Map.class, Point.class, 0L);
        CONVERSION_DB.put(NumberConversions::toPoint, Integer.class, Point.class, 0L);
        CONVERSION_DB.put(NumberConversions::toPoint, Long.class, Point.class, 0L);
        CONVERSION_DB.put(NumberConversions::toPoint, BigInteger.class, Point.class, 0L);
        CONVERSION_DB.put(NumberConversions::bigDecimalToPoint, BigDecimal.class, Point.class, 0L);
        CONVERSION_DB.put(NumberConversions::booleanToPoint, Boolean.class, Point.class, 0L);
        CONVERSION_DB.put(ArrayConversions::toPoint, int[].class, Point.class, 0L);
        CONVERSION_DB.put(DimensionConversions::toPoint, Dimension.class, Point.class, 0L);
        CONVERSION_DB.put(RectangleConversions::toPoint, Rectangle.class, Point.class, 0L);
        CONVERSION_DB.put(InsetsConversions::toPoint, Insets.class, Point.class, 0L);

        // Rectangle conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Rectangle.class, 0L);
        CONVERSION_DB.put(Converter::identity, Rectangle.class, Rectangle.class, 0L);
        CONVERSION_DB.put(StringConversions::toRectangle, String.class, Rectangle.class, 0L);
        CONVERSION_DB.put(MapConversions::toRectangle, Map.class, Rectangle.class, 0L);
        CONVERSION_DB.put(NumberConversions::integerToRectangle, Integer.class, Rectangle.class, 0L);
        CONVERSION_DB.put(NumberConversions::longToRectangle, Long.class, Rectangle.class, 0L);
        CONVERSION_DB.put(NumberConversions::bigIntegerToRectangle, BigInteger.class, Rectangle.class, 0L);
        CONVERSION_DB.put(NumberConversions::bigDecimalToRectangle, BigDecimal.class, Rectangle.class, 0L);
        CONVERSION_DB.put(NumberConversions::booleanToRectangle, Boolean.class, Rectangle.class, 0L);
        CONVERSION_DB.put(ArrayConversions::toRectangle, int[].class, Rectangle.class, 0L);
        CONVERSION_DB.put(PointConversions::toRectangle, Point.class, Rectangle.class, 0L);
        CONVERSION_DB.put(DimensionConversions::toRectangle, Dimension.class, Rectangle.class, 0L);
        CONVERSION_DB.put(InsetsConversions::toRectangle, Insets.class, Rectangle.class, 0L);

        // Insets conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Insets.class, 0L);
        CONVERSION_DB.put(Converter::identity, Insets.class, Insets.class, 0L);
        CONVERSION_DB.put(StringConversions::toInsets, String.class, Insets.class, 0L);
        CONVERSION_DB.put(MapConversions::toInsets, Map.class, Insets.class, 0L);
        CONVERSION_DB.put(NumberConversions::integerToInsets, Integer.class, Insets.class, 0L);
        CONVERSION_DB.put(NumberConversions::longToInsets, Long.class, Insets.class, 0L);
        CONVERSION_DB.put(NumberConversions::bigIntegerToInsets, BigInteger.class, Insets.class, 0L);
        CONVERSION_DB.put(NumberConversions::bigDecimalToInsets, BigDecimal.class, Insets.class, 0L);
        CONVERSION_DB.put(NumberConversions::booleanToInsets, Boolean.class, Insets.class, 0L);
        CONVERSION_DB.put(ArrayConversions::toInsets, int[].class, Insets.class, 0L);
        CONVERSION_DB.put(PointConversions::toInsets, Point.class, Insets.class, 0L);
        CONVERSION_DB.put(DimensionConversions::toInsets, Dimension.class, Insets.class, 0L);
        CONVERSION_DB.put(RectangleConversions::toInsets, Rectangle.class, Insets.class, 0L);

        // toFile
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, File.class, 0L);
        CONVERSION_DB.put(Converter::identity, File.class, File.class, 0L);
        CONVERSION_DB.put(StringConversions::toFile, String.class, File.class, 0L);
        CONVERSION_DB.put(MapConversions::toFile, Map.class, File.class, 0L);
        CONVERSION_DB.put(UriConversions::toFile, URI.class, File.class, 0L);
        CONVERSION_DB.put(PathConversions::toFile, Path.class, File.class, 0L);
        CONVERSION_DB.put(ArrayConversions::charArrayToFile, char[].class, File.class, 0L);
        CONVERSION_DB.put(ArrayConversions::byteArrayToFile, byte[].class, File.class, 0L);

        // toPath
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Path.class, 0L);
        CONVERSION_DB.put(Converter::identity, Path.class, Path.class, 0L);
        CONVERSION_DB.put(StringConversions::toPath, String.class, Path.class, 0L);
        CONVERSION_DB.put(MapConversions::toPath, Map.class, Path.class, 0L);
        CONVERSION_DB.put(UriConversions::toPath, URI.class, Path.class, 0L);
        CONVERSION_DB.put(FileConversions::toPath, File.class, Path.class, 0L);
        CONVERSION_DB.put(ArrayConversions::charArrayToPath, char[].class, Path.class, 0L);
        CONVERSION_DB.put(ArrayConversions::byteArrayToPath, byte[].class, Path.class, 0L);

        // Locale conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Locale.class, 0L);
        CONVERSION_DB.put(Converter::identity, Locale.class, Locale.class, 0L);
        CONVERSION_DB.put(StringConversions::toLocale, String.class, Locale.class, 0L);
        CONVERSION_DB.put(MapConversions::toLocale, Map.class, Locale.class, 0L);

        // String conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, String.class, 0L);
        CONVERSION_DB.put(StringConversions::toString, Byte.class, String.class, 0L);
        CONVERSION_DB.put(StringConversions::toString, Short.class, String.class, 0L);
        CONVERSION_DB.put(StringConversions::toString, Integer.class, String.class, 0L);
        CONVERSION_DB.put(StringConversions::toString, Long.class, String.class, 0L);
        CONVERSION_DB.put(NumberConversions::floatToString, Float.class, String.class, 0L);
        CONVERSION_DB.put(NumberConversions::doubleToString, Double.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, Boolean.class, String.class, 0L);
        CONVERSION_DB.put(CharacterConversions::toString, Character.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, BigInteger.class, String.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toString, BigDecimal.class, String.class, 0L);
        CONVERSION_DB.put(ByteArrayConversions::toString, byte[].class, String.class, 0L);
        CONVERSION_DB.put(CharArrayConversions::toString, char[].class, String.class, 0L);
        CONVERSION_DB.put(CharacterArrayConversions::toString, Character[].class, String.class, 0L);
        CONVERSION_DB.put(ByteBufferConversions::toString, ByteBuffer.class, String.class, 0L);
        CONVERSION_DB.put(CharBufferConversions::toString, CharBuffer.class, String.class, 0L);
        CONVERSION_DB.put(ClassConversions::toString, Class.class, String.class, 0L);
        CONVERSION_DB.put(DateConversions::toString, Date.class, String.class, 0L);
        CONVERSION_DB.put(CalendarConversions::toString, Calendar.class, String.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toString, java.sql.Date.class, String.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toString, Timestamp.class, String.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toString, LocalDate.class, String.class, 0L);
        CONVERSION_DB.put(LocalTimeConversions::toString, LocalTime.class, String.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toString, LocalDateTime.class, String.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toString, ZonedDateTime.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, UUID.class, String.class, 0L);
        CONVERSION_DB.put(ColorConversions::toString, Color.class, String.class, 0L);
        CONVERSION_DB.put(DimensionConversions::toString, Dimension.class, String.class, 0L);
        CONVERSION_DB.put(PointConversions::toString, Point.class, String.class, 0L);
        CONVERSION_DB.put(RectangleConversions::toString, Rectangle.class, String.class, 0L);
        CONVERSION_DB.put(InsetsConversions::toString, Insets.class, String.class, 0L);
        CONVERSION_DB.put(MapConversions::toString, Map.class, String.class, 0L);
        CONVERSION_DB.put(StringConversions::enumToString, Enum.class, String.class, 0L);
        CONVERSION_DB.put(Converter::identity, String.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, Duration.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, Instant.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, MonthDay.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, YearMonth.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, Period.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, ZoneId.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, ZoneOffset.class, String.class, 0L);
        CONVERSION_DB.put(OffsetTimeConversions::toString, OffsetTime.class, String.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toString, OffsetDateTime.class, String.class, 0L);
        CONVERSION_DB.put(YearConversions::toString, Year.class, String.class, 0L);
        CONVERSION_DB.put(LocaleConversions::toString, Locale.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, URI.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, URL.class, String.class, 0L);
        CONVERSION_DB.put(FileConversions::toString, File.class, String.class, 0L);
        CONVERSION_DB.put(PathConversions::toString, Path.class, String.class, 0L);
        CONVERSION_DB.put(TimeZoneConversions::toString, TimeZone.class, String.class, 0L);
        CONVERSION_DB.put(PatternConversions::toString, Pattern.class, String.class, 0L);
        CONVERSION_DB.put(CurrencyConversions::toString, Currency.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, StringBuilder.class, String.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toString, StringBuffer.class, String.class, 0L);

        // Currency conversions
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Currency.class, 0L);
        CONVERSION_DB.put(Converter::identity, Currency.class, Currency.class, 0L);
        CONVERSION_DB.put(StringConversions::toCurrency, String.class, Currency.class, 0L);
        CONVERSION_DB.put(MapConversions::toCurrency, Map.class, Currency.class, 0L);

        // Pattern conversions
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Pattern.class, 0L);
        CONVERSION_DB.put(Converter::identity, Pattern.class, Pattern.class, 0L);
        CONVERSION_DB.put(StringConversions::toPattern, String.class, Pattern.class, 0L);
        CONVERSION_DB.put(MapConversions::toPattern, Map.class, Pattern.class, 0L);

        // URL conversions
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, URL.class, 0L);
        CONVERSION_DB.put(Converter::identity, URL.class, URL.class, 0L);
        CONVERSION_DB.put(UriConversions::toURL, URI.class, URL.class, 0L);
        CONVERSION_DB.put(StringConversions::toURL, String.class, URL.class, 0L);
        CONVERSION_DB.put(MapConversions::toURL, Map.class, URL.class, 0L);
        CONVERSION_DB.put(FileConversions::toURL, File.class, URL.class, 0L);
        CONVERSION_DB.put(PathConversions::toURL, Path.class, URL.class, 0L);

        // URI Conversions
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, URI.class, 0L);
        CONVERSION_DB.put(Converter::identity, URI.class, URI.class, 0L);
        CONVERSION_DB.put(UrlConversions::toURI, URL.class, URI.class, 0L);
        CONVERSION_DB.put(StringConversions::toURI, String.class, URI.class, 0L);
        CONVERSION_DB.put(MapConversions::toURI, Map.class, URI.class, 0L);
        CONVERSION_DB.put(FileConversions::toURI, File.class, URI.class, 0L);
        CONVERSION_DB.put(PathConversions::toURI, Path.class, URI.class, 0L);

        // TimeZone Conversions
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, TimeZone.class, 0L);
        CONVERSION_DB.put(Converter::identity, TimeZone.class, TimeZone.class, 0L);
        CONVERSION_DB.put(StringConversions::toTimeZone, String.class, TimeZone.class, 0L);
        CONVERSION_DB.put(MapConversions::toTimeZone, Map.class, TimeZone.class, 0L);
        CONVERSION_DB.put(ZoneIdConversions::toTimeZone, ZoneId.class, TimeZone.class, 0L);
        CONVERSION_DB.put(ZoneOffsetConversions::toTimeZone, ZoneOffset.class, TimeZone.class, 0L);

        // Duration conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Duration.class, 0L);
        CONVERSION_DB.put(Converter::identity, Duration.class, Duration.class, 0L);
        CONVERSION_DB.put(NumberConversions::longNanosToDuration, Long.class, Duration.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toDuration, Double.class, Duration.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toDuration, BigInteger.class, Duration.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toDuration, BigDecimal.class, Duration.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toDuration, Timestamp.class, Duration.class, 0L);
        CONVERSION_DB.put(StringConversions::toDuration, String.class, Duration.class, 0L);
        CONVERSION_DB.put(MapConversions::toDuration, Map.class, Duration.class, 0L);

        // Instant conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Instant.class, 0L);
        CONVERSION_DB.put(Converter::identity, Instant.class, Instant.class, 0L);
        CONVERSION_DB.put(NumberConversions::longNanosToInstant, Long.class, Instant.class, 0L);
        CONVERSION_DB.put(DoubleConversions::toInstant, Double.class, Instant.class, 0L);
        CONVERSION_DB.put(BigIntegerConversions::toInstant, BigInteger.class, Instant.class, 0L);
        CONVERSION_DB.put(BigDecimalConversions::toInstant, BigDecimal.class, Instant.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toInstant, java.sql.Date.class, Instant.class, 0L);
        CONVERSION_DB.put(DateConversions::toInstant, Timestamp.class, Instant.class, 0L);
        CONVERSION_DB.put(DateConversions::toInstant, Date.class, Instant.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toInstant, LocalDate.class, Instant.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toInstant, LocalDateTime.class, Instant.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toInstant, ZonedDateTime.class, Instant.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toInstant, OffsetDateTime.class, Instant.class, 0L);
        CONVERSION_DB.put(DurationConversions::toInstant, Duration.class, Instant.class, 0L);

        CONVERSION_DB.put(StringConversions::toInstant, String.class, Instant.class, 0L);
        CONVERSION_DB.put(MapConversions::toInstant, Map.class, Instant.class, 0L);

        // ZoneId conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, ZoneId.class, 0L);
        CONVERSION_DB.put(Converter::identity, ZoneId.class, ZoneId.class, 0L);
        CONVERSION_DB.put(StringConversions::toZoneId, String.class, ZoneId.class, 0L);
        CONVERSION_DB.put(MapConversions::toZoneId, Map.class, ZoneId.class, 0L);
        CONVERSION_DB.put(TimeZoneConversions::toZoneId, TimeZone.class, ZoneId.class, 0L);
        CONVERSION_DB.put(ZoneOffsetConversions::toZoneId, ZoneOffset.class, ZoneId.class, 0L);

        // ZoneOffset conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, ZoneOffset.class, 0L);
        CONVERSION_DB.put(Converter::identity, ZoneOffset.class, ZoneOffset.class, 0L);
        CONVERSION_DB.put(StringConversions::toZoneOffset, String.class, ZoneOffset.class, 0L);
        CONVERSION_DB.put(MapConversions::toZoneOffset, Map.class, ZoneOffset.class, 0L);
        CONVERSION_DB.put(ZoneIdConversions::toZoneOffset, ZoneId.class, ZoneOffset.class, 0L);
        CONVERSION_DB.put(TimeZoneConversions::toZoneOffset, TimeZone.class, ZoneOffset.class, 0L);

        // MonthDay conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, MonthDay.class, 0L);
        CONVERSION_DB.put(Converter::identity, MonthDay.class, MonthDay.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toMonthDay, java.sql.Date.class, MonthDay.class, 0L);
        CONVERSION_DB.put(DateConversions::toMonthDay, Date.class, MonthDay.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toMonthDay, Timestamp.class, MonthDay.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toMonthDay, LocalDate.class, MonthDay.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toMonthDay, LocalDateTime.class, MonthDay.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toMonthDay, ZonedDateTime.class, MonthDay.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toMonthDay, OffsetDateTime.class, MonthDay.class, 0L);
        CONVERSION_DB.put(StringConversions::toMonthDay, String.class, MonthDay.class, 0L);
        CONVERSION_DB.put(MapConversions::toMonthDay, Map.class, MonthDay.class, 0L);
        CONVERSION_DB.put(NumberConversions::toMonthDay, Short.class, MonthDay.class, 0L);
        CONVERSION_DB.put(NumberConversions::toMonthDay, Integer.class, MonthDay.class, 0L);
        CONVERSION_DB.put(NumberConversions::toMonthDay, Long.class, MonthDay.class, 0L);
        CONVERSION_DB.put(NumberConversions::toMonthDay, Float.class, MonthDay.class, 0L);
        CONVERSION_DB.put(NumberConversions::toMonthDay, Double.class, MonthDay.class, 0L);
        CONVERSION_DB.put(NumberConversions::toMonthDay, AtomicInteger.class, MonthDay.class, 0L);
        CONVERSION_DB.put(NumberConversions::toMonthDay, AtomicLong.class, MonthDay.class, 0L);
        CONVERSION_DB.put(NumberConversions::toMonthDay, BigInteger.class, MonthDay.class, 0L);
        CONVERSION_DB.put(NumberConversions::toMonthDay, BigDecimal.class, MonthDay.class, 0L);

        // YearMonth conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, YearMonth.class, 0L);
        CONVERSION_DB.put(Converter::identity, YearMonth.class, YearMonth.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toYearMonth, java.sql.Date.class, YearMonth.class, 0L);
        CONVERSION_DB.put(DateConversions::toYearMonth, Date.class, YearMonth.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toYearMonth, Timestamp.class, YearMonth.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toYearMonth, LocalDate.class, YearMonth.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toYearMonth, LocalDateTime.class, YearMonth.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toYearMonth, ZonedDateTime.class, YearMonth.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toYearMonth, OffsetDateTime.class, YearMonth.class, 0L);
        CONVERSION_DB.put(StringConversions::toYearMonth, String.class, YearMonth.class, 0L);
        CONVERSION_DB.put(MapConversions::toYearMonth, Map.class, YearMonth.class, 0L);

        // Period conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Period.class, 0L);
        CONVERSION_DB.put(Converter::identity, Period.class, Period.class, 0L);
        CONVERSION_DB.put(StringConversions::toPeriod, String.class, Period.class, 0L);
        CONVERSION_DB.put(MapConversions::toPeriod, Map.class, Period.class, 0L);

        // toStringBuffer
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, StringBuffer.class, 0L);
        CONVERSION_DB.put(StringConversions::toStringBuffer, String.class, StringBuffer.class, 0L);

        // toStringBuilder - Bridge through String
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, StringBuilder.class, 0L);
        CONVERSION_DB.put(StringConversions::toStringBuilder, String.class, StringBuilder.class, 0L);

        // toByteArray
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, byte[].class, 0L);
        CONVERSION_DB.put(StringConversions::toByteArray, String.class, byte[].class, 0L);
        CONVERSION_DB.put(ByteBufferConversions::toByteArray, ByteBuffer.class, byte[].class, 0L);
        CONVERSION_DB.put(CharBufferConversions::toByteArray, CharBuffer.class, byte[].class, 0L);
        CONVERSION_DB.put(VoidConversions::toNull, char[].class, byte[].class, 0L); // advertising convertion, implemented generically in ArrayConversions.
        CONVERSION_DB.put(Converter::identity, byte[].class, byte[].class, 0L);
        CONVERSION_DB.put(FileConversions::toByteArray, File.class, byte[].class, 0L);
        CONVERSION_DB.put(PathConversions::toByteArray, Path.class, byte[].class, 0L);

        // toCharArray
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, char[].class, 0L);
        CONVERSION_DB.put(StringConversions::toCharArray, String.class, char[].class, 0L);
        CONVERSION_DB.put(ByteBufferConversions::toCharArray, ByteBuffer.class, char[].class, 0L);
        CONVERSION_DB.put(CharBufferConversions::toCharArray, CharBuffer.class, char[].class, 0L);
        CONVERSION_DB.put(CharArrayConversions::toCharArray, char[].class, char[].class, 0L);
        CONVERSION_DB.put(VoidConversions::toNull, byte[].class, char[].class, 0L);   // Used for advertising capability, implemented generically in ArrayConversions.
        CONVERSION_DB.put(FileConversions::toCharArray, File.class, char[].class, 0L);
        CONVERSION_DB.put(PathConversions::toCharArray, Path.class, char[].class, 0L);

        // toCharacterArray
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Character[].class, 0L);
        CONVERSION_DB.put(StringConversions::toCharacterArray, String.class, Character[].class, 0L);

        // toCharBuffer
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, CharBuffer.class, 0L);
        CONVERSION_DB.put(StringConversions::toCharBuffer, String.class, CharBuffer.class, 0L);
        CONVERSION_DB.put(ByteBufferConversions::toCharBuffer, ByteBuffer.class, CharBuffer.class, 0L);
        CONVERSION_DB.put(CharBufferConversions::toCharBuffer, CharBuffer.class, CharBuffer.class, 0L);
        CONVERSION_DB.put(CharArrayConversions::toCharBuffer, char[].class, CharBuffer.class, 0L);
        CONVERSION_DB.put(ByteArrayConversions::toCharBuffer, byte[].class, CharBuffer.class, 0L);
        CONVERSION_DB.put(MapConversions::toCharBuffer, Map.class, CharBuffer.class, 0L);

        // toByteBuffer
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, ByteBuffer.class, 0L);
        CONVERSION_DB.put(StringConversions::toByteBuffer, String.class, ByteBuffer.class, 0L);
        CONVERSION_DB.put(ByteBufferConversions::toByteBuffer, ByteBuffer.class, ByteBuffer.class, 0L);
        CONVERSION_DB.put(CharBufferConversions::toByteBuffer, CharBuffer.class, ByteBuffer.class, 0L);
        CONVERSION_DB.put(CharArrayConversions::toByteBuffer, char[].class, ByteBuffer.class, 0L);
        CONVERSION_DB.put(ByteArrayConversions::toByteBuffer, byte[].class, ByteBuffer.class, 0L);
        CONVERSION_DB.put(MapConversions::toByteBuffer, Map.class, ByteBuffer.class, 0L);

        // toYear
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Year.class, 0L);
        CONVERSION_DB.put(Converter::identity, Year.class, Year.class, 0L);
        CONVERSION_DB.put(NumberConversions::toYear, Short.class, Year.class, 0L);
        CONVERSION_DB.put(NumberConversions::toYear, Integer.class, Year.class, 0L);
        CONVERSION_DB.put(NumberConversions::toYear, Long.class, Year.class, 0L);
        CONVERSION_DB.put(NumberConversions::toYear, Float.class, Year.class, 0L);
        CONVERSION_DB.put(NumberConversions::toYear, Double.class, Year.class, 0L);
        CONVERSION_DB.put(NumberConversions::toYear, BigInteger.class, Year.class, 0L);
        CONVERSION_DB.put(NumberConversions::toYear, BigDecimal.class, Year.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toYear, java.sql.Date.class, Year.class, 0L);
        CONVERSION_DB.put(DateConversions::toYear, Date.class, Year.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toYear, Timestamp.class, Year.class, 0L);
        CONVERSION_DB.put(LocalDateConversions::toYear, LocalDate.class, Year.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toYear, LocalDateTime.class, Year.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toYear, ZonedDateTime.class, Year.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toYear, OffsetDateTime.class, Year.class, 0L);
        CONVERSION_DB.put(StringConversions::toYear, String.class, Year.class, 0L);
        CONVERSION_DB.put(MapConversions::toYear, Map.class, Year.class, 0L);

        // Throwable conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Throwable.class, 0L);
        CONVERSION_DB.put((ConvertWithTarget<Throwable>) MapConversions::toThrowable, Map.class, Throwable.class, 0L);

        // Map conversions supported
        CONVERSION_DB.put(VoidConversions::toNull, Void.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, Byte.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, Short.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, Integer.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, Long.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, Float.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, Double.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, Boolean.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, Character.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, BigInteger.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, BigDecimal.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, AtomicBoolean.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, AtomicInteger.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, AtomicLong.class, Map.class, 0L);
        CONVERSION_DB.put(DateConversions::toMap, Date.class, Map.class, 0L);
        CONVERSION_DB.put(SqlDateConversions::toMap, java.sql.Date.class, Map.class, 0L);
        CONVERSION_DB.put(TimestampConversions::toMap, Timestamp.class, Map.class, 0L);
        CONVERSION_DB.put(CalendarConversions::toMap, Calendar.class, Map.class, 0L);  // Restored - bridge produces different map key (zonedDateTime vs. calendar)
        CONVERSION_DB.put(LocalDateConversions::toMap, LocalDate.class, Map.class, 0L);
        CONVERSION_DB.put(LocalDateTimeConversions::toMap, LocalDateTime.class, Map.class, 0L);
        CONVERSION_DB.put(ZonedDateTimeConversions::toMap, ZonedDateTime.class, Map.class, 0L);
        CONVERSION_DB.put(DurationConversions::toMap, Duration.class, Map.class, 0L);
        CONVERSION_DB.put(InstantConversions::toMap, Instant.class, Map.class, 0L);
        CONVERSION_DB.put(LocalTimeConversions::toMap, LocalTime.class, Map.class, 0L);
        CONVERSION_DB.put(MonthDayConversions::toMap, MonthDay.class, Map.class, 0L);
        CONVERSION_DB.put(YearMonthConversions::toMap, YearMonth.class, Map.class, 0L);
        CONVERSION_DB.put(PeriodConversions::toMap, Period.class, Map.class, 0L);
        CONVERSION_DB.put(TimeZoneConversions::toMap, TimeZone.class, Map.class, 0L);
        CONVERSION_DB.put(ZoneIdConversions::toMap, ZoneId.class, Map.class, 0L);
        CONVERSION_DB.put(ZoneOffsetConversions::toMap, ZoneOffset.class, Map.class, 0L);
        CONVERSION_DB.put(UniversalConversions::toMap, Class.class, Map.class, 0L);
        CONVERSION_DB.put(UUIDConversions::toMap, UUID.class, Map.class, 0L);
        CONVERSION_DB.put(ColorConversions::toMap, Color.class, Map.class, 0L);
        CONVERSION_DB.put(DimensionConversions::toMap, Dimension.class, Map.class, 0L);
        CONVERSION_DB.put(PointConversions::toMap, Point.class, Map.class, 0L);
        CONVERSION_DB.put(RectangleConversions::toMap, Rectangle.class, Map.class, 0L);
        CONVERSION_DB.put(InsetsConversions::toMap, Insets.class, Map.class, 0L);
        CONVERSION_DB.put(StringConversions::toMap, String.class, Map.class, 0L);
        CONVERSION_DB.put(EnumConversions::toMap, Enum.class, Map.class, 0L);
        CONVERSION_DB.put(OffsetDateTimeConversions::toMap, OffsetDateTime.class, Map.class, 0L);
        CONVERSION_DB.put(OffsetTimeConversions::toMap, OffsetTime.class, Map.class, 0L);
        CONVERSION_DB.put(YearConversions::toMap, Year.class, Map.class, 0L);
        CONVERSION_DB.put(LocaleConversions::toMap, Locale.class, Map.class, 0L);
        CONVERSION_DB.put(UriConversions::toMap, URI.class, Map.class, 0L);
        CONVERSION_DB.put(UrlConversions::toMap, URL.class, Map.class, 0L);
        CONVERSION_DB.put(ThrowableConversions::toMap, Throwable.class, Map.class, 0L);
        CONVERSION_DB.put(PatternConversions::toMap, Pattern.class, Map.class, 0L);
        CONVERSION_DB.put(CurrencyConversions::toMap, Currency.class, Map.class, 0L);
        CONVERSION_DB.put(ByteBufferConversions::toMap, ByteBuffer.class, Map.class, 0L);
        CONVERSION_DB.put(CharBufferConversions::toMap, CharBuffer.class, Map.class, 0L);
        CONVERSION_DB.put(FileConversions::toMap, File.class, Map.class, 0L);
        CONVERSION_DB.put(PathConversions::toMap, Path.class, Map.class, 0L);

        // toIntArray
        CONVERSION_DB.put(ColorConversions::toIntArray, Color.class, int[].class, 0L);
        CONVERSION_DB.put(DimensionConversions::toIntArray, Dimension.class, int[].class, 0L);
        CONVERSION_DB.put(PointConversions::toIntArray, Point.class, int[].class, 0L);
        CONVERSION_DB.put(RectangleConversions::toIntArray, Rectangle.class, int[].class, 0L);
        CONVERSION_DB.put(InsetsConversions::toIntArray, Insets.class, int[].class, 0L);

        // Array-like type bridges for universal array system access
        // ========================================
        // Atomic Array Bridges
        // ========================================

        // AtomicIntegerArray ↔ int[] bridges
        CONVERSION_DB.put(UniversalConversions::atomicIntegerArrayToIntArray, AtomicIntegerArray.class, int[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::intArrayToAtomicIntegerArray, int[].class, AtomicIntegerArray.class, 0L);

        // AtomicLongArray ↔ long[] bridges  
        CONVERSION_DB.put(UniversalConversions::atomicLongArrayToLongArray, AtomicLongArray.class, long[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::longArrayToAtomicLongArray, long[].class, AtomicLongArray.class, 0L);

        // AtomicReferenceArray ↔ Object[] bridges
        CONVERSION_DB.put(UniversalConversions::atomicReferenceArrayToObjectArray, AtomicReferenceArray.class, Object[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::objectArrayToAtomicReferenceArray, Object[].class, AtomicReferenceArray.class, 0L);

        // AtomicReferenceArray ↔ String[] bridges
        CONVERSION_DB.put(UniversalConversions::atomicReferenceArrayToStringArray, AtomicReferenceArray.class, String[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::stringArrayToAtomicReferenceArray, String[].class, AtomicReferenceArray.class, 0L);

        // ========================================
        // NIO Buffer Bridges
        // ========================================

        // IntBuffer ↔ int[] bridges
        CONVERSION_DB.put(UniversalConversions::intBufferToIntArray, IntBuffer.class, int[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::intArrayToIntBuffer, int[].class, IntBuffer.class, 0L);

        // LongBuffer ↔ long[] bridges
        CONVERSION_DB.put(UniversalConversions::longBufferToLongArray, LongBuffer.class, long[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::longArrayToLongBuffer, long[].class, LongBuffer.class, 0L);

        // FloatBuffer ↔ float[] bridges
        CONVERSION_DB.put(UniversalConversions::floatBufferToFloatArray, FloatBuffer.class, float[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::floatArrayToFloatBuffer, float[].class, FloatBuffer.class, 0L);

        // DoubleBuffer ↔ double[] bridges
        CONVERSION_DB.put(UniversalConversions::doubleBufferToDoubleArray, DoubleBuffer.class, double[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::doubleArrayToDoubleBuffer, double[].class, DoubleBuffer.class, 0L);

        // ShortBuffer ↔ short[] bridges
        CONVERSION_DB.put(UniversalConversions::shortBufferToShortArray, ShortBuffer.class, short[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::shortArrayToShortBuffer, short[].class, ShortBuffer.class, 0L);

        // ========================================
        // BitSet Bridges
        // ========================================

        // BitSet ↔ boolean[] bridges
        CONVERSION_DB.put(UniversalConversions::bitSetToBooleanArray, BitSet.class, boolean[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::booleanArrayToBitSet, boolean[].class, BitSet.class, 0L);

        // BitSet ↔ int[] bridges (set bit indices)
        CONVERSION_DB.put(UniversalConversions::bitSetToIntArray, BitSet.class, int[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::intArrayToBitSet, int[].class, BitSet.class, 0L);

        // BitSet ↔ byte[] bridges
        CONVERSION_DB.put(UniversalConversions::bitSetToByteArray, BitSet.class, byte[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::byteArrayToBitSet, byte[].class, BitSet.class, 0L);

        // ========================================
        // Stream Bridges
        // ========================================

        // IntStream ↔ int[] bridges
        CONVERSION_DB.put(UniversalConversions::intStreamToIntArray, IntStream.class, int[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::intArrayToIntStream, int[].class, IntStream.class, 0L);

        // LongStream ↔ long[] bridges
        CONVERSION_DB.put(UniversalConversions::longStreamToLongArray, LongStream.class, long[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::longArrayToLongStream, long[].class, LongStream.class, 0L);

        // DoubleStream ↔ double[] bridges
        CONVERSION_DB.put(UniversalConversions::doubleStreamToDoubleArray, DoubleStream.class, double[].class, 0L);
        CONVERSION_DB.put(UniversalConversions::doubleArrayToDoubleStream, double[].class, DoubleStream.class, 0L);

        // Register Record.class -> Map.class conversion if Records are supported
        try {
            Class<?> recordClass = Class.forName("java.lang.Record");
            CONVERSION_DB.put(MapConversions::recordToMap, recordClass, Map.class, 0L);
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
     * Every "surrogate" on the left can be losslessly collapsed to the "primary" on the
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

//                    // Year → Long (maximum reach for data pipelines)
//                    new SurrogatePrimaryPair(Year.class, Long.class,
//                            YearConversions::toLong, null),
//
//                    // YearMonth → String (maximum reach for temporal formatting)
//                    new SurrogatePrimaryPair(YearMonth.class, String.class,
//                            UniversalConversions::toString, null),
//
//                    // Duration → Long (numeric reach for time calculations)
//                    new SurrogatePrimaryPair(Duration.class, Long.class,
//                            DurationConversions::toLong, null),
//
//                    // OffsetTime → String (maximum reach preserving offset info)
//                    new SurrogatePrimaryPair(OffsetTime.class, String.class,
//                            OffsetTimeConversions::toString, null),

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
     * Represents a surrogate-primary class pair with bidirectional bridge conversion functions.
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
        // Expand all configured surrogate bridges in both directions
        expandSurrogateBridges(BridgeDirection.SURROGATE_TO_PRIMARY);
        expandSurrogateBridges(BridgeDirection.PRIMARY_TO_SURROGATE);
    }

    /**
     * Consolidated method for expanding surrogate bridges in both directions.
     * Creates composite conversion functions that bridge through intermediate types.
     * 
     * @param direction The direction of bridge expansion (SURROGATE_TO_PRIMARY or PRIMARY_TO_SURROGATE)
     */
    private static void expandSurrogateBridges(BridgeDirection direction) {
        // Create a snapshot of existing entries to avoid ConcurrentModificationException
        List<MultiKeyMap.MultiKeyEntry<Convert<?>>> existingEntries = new ArrayList<>();
        for (MultiKeyMap.MultiKeyEntry<Convert<?>> entry : CONVERSION_DB.entries()) {
            // Skip entries that don't follow the classic (Class, Class, long) pattern
            // This includes coconut-wrapped single-key entries and other N-Key entries
            if (entry.keys.length >= 3) {
                Object source = entry.keys[0];
                Object target = entry.keys[1];
                if (source instanceof Class && target instanceof Class) {
                    existingEntries.add(entry);
                }
            }
        }

        // Get the appropriate configuration list based on a direction
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
                for (MultiKeyMap.MultiKeyEntry<Convert<?>> entry : existingEntries) {
                    Class<?> sourceClass = (Class<?>) entry.keys[0];
                    Class<?> targetClass = (Class<?>) entry.keys[1];
                    if (sourceClass.equals(primaryClass)) {
                        // Only add if not already defined and not converting to itself
                        if (CONVERSION_DB.get(surrogateClass, targetClass, 0L) == null && !targetClass.equals(surrogateClass)) {
                            // Create composite conversion: Surrogate → primary → target
                            Object instanceIdObj = entry.keys[2];
                            long instanceId = (instanceIdObj instanceof Integer) ? ((Integer) instanceIdObj).longValue() : (Long) instanceIdObj;
                            Convert<?> originalConversion = CONVERSION_DB.get(sourceClass, targetClass, instanceId);
                            Convert<?> bridgeConversion = createSurrogateToPrimaryBridgeConversion(config, originalConversion);
                            CONVERSION_DB.put(bridgeConversion, surrogateClass, targetClass, 0L);
                        }
                    }
                }
            } else {
                // REVERSE BRIDGES: Source → Primary → Surrogate
                // Example: String.class → Integer.class → int.class
                Class<?> primaryClass = config.surrogateClass;  // Note: in List 2, surrogate is the source
                Class<?> surrogateClass = config.primaryClass;  // and primary is the target

                // Find all sources that can convert to the primary class
                for (MultiKeyMap.MultiKeyEntry<Convert<?>> entry : existingEntries) {
                    Class<?> sourceClass = (Class<?>) entry.keys[0];
                    Class<?> targetClass = (Class<?>) entry.keys[1];
                    if (targetClass.equals(primaryClass)) {
                        // Only add if not already defined and not converting from itself
                        if (CONVERSION_DB.get(sourceClass, surrogateClass, 0L) == null && !sourceClass.equals(surrogateClass)) {
                            // Create composite conversion: Source → primary → surrogate
                            Object instanceIdObj = entry.keys[2];
                            long instanceId = (instanceIdObj instanceof Integer) ? ((Integer) instanceIdObj).longValue() : (Long) instanceIdObj;
                            Convert<?> originalConversion = CONVERSION_DB.get(sourceClass, targetClass, instanceId);
                            Convert<?> bridgeConversion = createPrimaryToSurrogateBridgeConversion(config, originalConversion);
                            CONVERSION_DB.put(bridgeConversion, sourceClass, surrogateClass, 0L);
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
            // First: Convert a source to primary using existing conversion
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
            USER_DB.put(entry.getValue(), pair.getSource(), pair.getTarget(), pair.getInstanceId());
            
            // Add identity conversions for non-standard types to enable O(1) hasConverterOverrideFor lookup
            if (!isStandardType(pair.getSource())) {
                addIdentityConversionIfNeeded(pair.getSource(), pair.getInstanceId());
            }
            if (!isStandardType(pair.getTarget())) {
                addIdentityConversionIfNeeded(pair.getTarget(), pair.getInstanceId());
            }
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
     *     LOG.info("Converted Integer: " + number); // Output: Converted Integer: 123
     *
     *     // Example 2: Convert String to Date
     *     String dateStr = "2024-04-27";
     *     LocalDate date = converter.convert(dateStr, LocalDate.class);
     *     LOG.info("Converted Date: " + date); // Output: Converted Date: 2024-04-27
     *
     *     // Example 3: Convert Enum to String
     *     Day day = Day.MONDAY;
     *     String dayStr = converter.convert(day, String.class);
     *     LOG.info("Converted Day: " + dayStr); // Output: Converted Day: MONDAY
     *
     *     // Example 4: Convert Array to List
     *     String[] stringArray = {"apple", "banana", "cherry"};
     *     List<String> stringList = converter.convert(stringArray, List.class);
     *     LOG.info("Converted List: " + stringList); // Output: Converted List: [apple, banana, cherry]
     *
     *     // Example 5: Convert Map to UUID
     *     Map<String, Object> uuidMap = Map.of("mostSigBits", 123456789L, "leastSigBits", 987654321L);
     *     UUID uuid = converter.convert(uuidMap, UUID.class);
     *     LOG.info("Converted UUID: " + uuid); // Output: Converted UUID: 00000000-075b-cd15-0000-0000003ade68
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
     *     LOG.info("Converted CustomType: " + custom); // Output: Converted CustomType: CustomType{value='customValue'}
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
     * may impact memory usage. It is recommended to register only the necessary converters to maintain optimal performance.
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
                return (T) cached.convert(null, this, toType);
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

        // Check user-added conversions in this context (either instanceId 0L for static, or specific instanceId for instance)
        Convert<?> conversionMethod = USER_DB.get(sourceType, toType, this.instanceId);
        if (isValidConversion(conversionMethod)) {
            cacheConverter(sourceType, toType, conversionMethod);
            return (T) conversionMethod.convert(from, this, toType);
        }

        // Then check the factory conversion database.
        conversionMethod = CONVERSION_DB.get(sourceType, toType, 0L);
        if (isValidConversion(conversionMethod)) {
            // Cache built-in conversions with instance ID 0 to keep them shared across instances
            FULL_CONVERSION_CACHE.put(conversionMethod, sourceType, toType, 0L);
            // Also cache with current instance ID for faster future lookup
            cacheConverter(sourceType, toType, conversionMethod);
            return (T) conversionMethod.convert(from, this, toType);
        }

        // Attempt inheritance-based conversion.
        conversionMethod = getInheritedConverter(sourceType, toType, this.instanceId);
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
        FULL_CONVERSION_CACHE.put(converter, source, target, this.instanceId);
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
    private static Convert<?> getInheritedConverter(Class<?> sourceType, Class<?> toType, long instanceId) {
        // Build the complete set of source types (including sourceType itself) with levels.
        Set<ClassLevel> sourceTypes = new TreeSet<>(getSuperClassesAndInterfaces(sourceType));
        sourceTypes.add(new ClassLevel(sourceType, 0));
        // Build the complete set of target types (including toType itself) with levels.
        Set<ClassLevel> targetTypes = new TreeSet<>(getSuperClassesAndInterfaces(toType));
        targetTypes.add(new ClassLevel(toType, 0));

        // Create pairs of source/target types with their associated levels.
        class ConversionPairWithLevel {
            private final Class<?> source;
            private final Class<?> target;
            private final long instanceId;
            private final int sourceLevel;
            private final int targetLevel;

            private ConversionPairWithLevel(Class<?> source, Class<?> target, long instanceId, int sourceLevel, int targetLevel) {
                this.source = source;
                this.target = target;
                this.instanceId = instanceId;
                this.sourceLevel = sourceLevel;
                this.targetLevel = targetLevel;
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj) return true;
                if (!(obj instanceof ConversionPairWithLevel)) return false;
                ConversionPairWithLevel other = (ConversionPairWithLevel) obj;
                return source == other.source && 
                       target == other.target && 
                       instanceId == other.instanceId &&
                       sourceLevel == other.sourceLevel &&
                       targetLevel == other.targetLevel;
            }

            @Override
            public int hashCode() {
                return Objects.hash(source, target, instanceId, sourceLevel, targetLevel);
            }
        }

        List<ConversionPairWithLevel> pairs = new ArrayList<>();
        for (ClassLevel source : sourceTypes) {
            for (ClassLevel target : targetTypes) {
                pairs.add(new ConversionPairWithLevel(source.clazz, target.clazz, instanceId, source.level, target.level));
            }
        }

        // Sort the pairs by a composite of rules:
        // - Exact target matches first.
        // - Then by assignability of the target types.
        // - Then by combined inheritance distance.
        // - Finally, prefer concrete classes over interfaces.
        pairs.sort((p1, p2) -> {
            boolean p1ExactTarget = p1.target == toType;
            boolean p2ExactTarget = p2.target == toType;
            if (p1ExactTarget != p2ExactTarget) {
                return p1ExactTarget ? -1 : 1;
            }
            if (p1.target != p2.target) {
                boolean p1AssignableToP2 = p2.target.isAssignableFrom(p1.target);
                boolean p2AssignableToP1 = p1.target.isAssignableFrom(p2.target);
                if (p1AssignableToP2 != p2AssignableToP1) {
                    return p1AssignableToP2 ? -1 : 1;
                }
            }
            int dist1 = p1.sourceLevel + p1.targetLevel;
            int dist2 = p2.sourceLevel + p2.targetLevel;
            if (dist1 != dist2) {
                return dist1 - dist2;
            }
            boolean p1FromInterface = p1.source.isInterface();
            boolean p2FromInterface = p2.source.isInterface();
            if (p1FromInterface != p2FromInterface) {
                return p1FromInterface ? 1 : -1;
            }
            boolean p1ToInterface = p1.target.isInterface();
            boolean p2ToInterface = p2.target.isInterface();
            if (p1ToInterface != p2ToInterface) {
                return p1ToInterface ? 1 : -1;
            }
            return 0;
        });

        // Iterate over sorted pairs and check the converter databases.
        for (ConversionPairWithLevel pairWithLevel : pairs) {
            Convert<?> tempConverter = USER_DB.get(pairWithLevel.source, pairWithLevel.target, pairWithLevel.instanceId);
            if (tempConverter != null) {
                return tempConverter;
            }
            tempConverter = CONVERSION_DB.get(pairWithLevel.source, pairWithLevel.target, 0L);
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
        // If user has registered custom converter overrides for this conversion pair, it's not simple anymore
        if (hasConverterOverrideFor(source, target)) {
            return false;
        }
        
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

        // Special case: When a source is Number, delegate using Long.
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
        method = getInheritedConverter(source, target, 0L);
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
     * Results are cached for fast subsequent lookups when no custom overrides exist.
     * 
     * <p>If custom converter overrides exist for the specified type, this method returns false,
     * regardless of inheritance-based conversion support. This ensures that user-defined custom
     * converters take precedence over automatic simple type conversions.</p>
     *
     * @param type the class to check
     * @return {@code true} if a simple type conversion exists for the class and no custom overrides are registered
     */
    public boolean isSimpleTypeConversionSupported(Class<?> type) {
        // If user has registered custom converter overrides targeting this type, it's not simple anymore
        if (hasConverterOverrideFor(type)) {
            return false;
        }
        
        // Use cached result for types without custom overrides
        return SIMPLE_TYPE_CACHE.computeIfAbsent(type, t -> isSimpleTypeConversionSupported(t, t));
    }

    /**
     * Checks if custom converter overrides exist for the specified source-target conversion pair.
     * Uses efficient hash lookup in USER_DB instead of linear search.
     *
     * @param sourceType the source type to check for custom overrides
     * @param targetType the target type to check for custom overrides  
     * @return {@code true} if custom converter overrides exist for the conversion pair, {@code false} otherwise
     */
    private boolean hasConverterOverrideFor(Class<?> sourceType, Class<?> targetType) {
        // Check if there are custom overrides for this conversion pair
        if (options != null) {
            Map<ConversionPair, Convert<?>> converterOverrides = options.getConverterOverrides();
            if (converterOverrides != null && !converterOverrides.isEmpty()) {
                // Get instance ID from first conversion pair (all pairs for this instance use same ID)
                ConversionPair firstPair = converterOverrides.keySet().iterator().next();
                long instanceId = firstPair.getInstanceId();
                
                // Direct hash lookup in USER_DB using this instance's ID
                Convert<?> converter = USER_DB.get(sourceType, targetType, instanceId);
                return converter != null && converter != UNSUPPORTED;
            }
        }
        return false;
    }

    /**
     * Checks if custom converter overrides exist for the specified target type.
     * Uses the brilliant optimization of checking for identity conversion (T -> T) which is 
     * automatically added for all non-standard types involved in custom conversions.
     * This provides O(1) performance instead of O(n) linear search.
     * 
     * @param targetType the target type to check for custom overrides
     * @return {@code true} if custom converter overrides exist for the target type, {@code false} otherwise
     */
    private boolean hasConverterOverrideFor(Class<?> targetType) {
        // Optimization: Just check for identity conversion (T -> T)
        // Non-standard types involved in custom conversions automatically get identity conversions
        // This turns an O(n) linear search into an O(1) hash lookup
        return hasConverterOverrideFor(targetType, targetType);
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
        method = getInheritedConverter(source, target, 0L);
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
        USER_DB.put(conversionMethod, wrapperSource, wrapperTarget, 0L);

        // Add all type combinations to USER_DB
        for (Class<?> srcType : sourceTypes) {
            for (Class<?> tgtType : targetTypes) {
                USER_DB.put(conversionMethod, srcType, tgtType, 0L);
            }
        }

        // Add identity conversions for non-standard types to enable O(1) hasConverterOverrideFor lookup
        if (!isStandardType(source)) {
            addIdentityConversionIfNeeded(source, 0L);
        }
        if (!isStandardType(target)) {
            addIdentityConversionIfNeeded(target, 0L);
        }

        return previous;
    }

    /**
     * Adds a new conversion function for converting from one type to another for this specific Converter instance.
     * Unlike the static {@link #addConversion(Class, Class, Convert)} method, this instance method stores the 
     * conversion only for this converter instance, avoiding global pollution of the conversion database.
     *
     * <p>This is the recommended approach for adding custom conversions as it prevents interference between
     * different applications, libraries, or test cases that might be using java-util simultaneously.
     * Each Converter instance maintains its own set of custom conversions in addition to accessing
     * the shared factory conversions and any static user conversions.</p>
     *
     * <p>When {@code convert(source, target)} is called on this instance, the conversion function is located by:
     * <ol>
     *   <li>Checking instance-specific conversions first (added via this method)</li>
     *   <li>Checking static user conversions (added via static {@link #addConversion} method)</li>
     *   <li>Checking factory conversions (built-in conversions)</li>
     *   <li>Attempting inheritance-based conversion lookup</li>
     * </ol></p>
     *
     * <p>This method automatically handles primitive types by converting them to their corresponding wrapper types
     * and stores conversions for all primitive/wrapper combinations, just like the static version.</p>
     *
     * @param conversionMethod A method that converts an instance of the source type to an instance of the target type.
     * @param source           The source class (type) to convert from.
     * @param target           The target class (type) to convert to.
     * @return The previous conversion method associated with the source and target types for this instance, or {@code null} if no conversion existed.
     */
    public Convert<?> addConversion(Convert<?> conversionMethod, Class<?> source, Class<?> target) {
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
        Convert<?> previous = USER_DB.get(wrapperSource, wrapperTarget, this.instanceId);
        USER_DB.put(conversionMethod, wrapperSource, wrapperTarget, this.instanceId);

        // Add all type combinations to USER_DB with this instance ID
        for (Class<?> srcType : sourceTypes) {
            for (Class<?> tgtType : targetTypes) {
                USER_DB.put(conversionMethod, srcType, tgtType, this.instanceId);
            }
        }

        // Add identity conversions for non-standard types to enable O(1) hasConverterOverrideFor lookup
        if (!isStandardType(source)) {
            addIdentityConversionIfNeeded(source, this.instanceId);
        }
        if (!isStandardType(target)) {
            addIdentityConversionIfNeeded(target, this.instanceId);
        }

        return previous;
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
     * Determines if a class is a "standard" type that has built-in conversion support.
     * Standard types include primitives, wrappers, common JDK types, and types with
     * existing conversions in the factory conversion database.
     * 
     * @param clazz the class to check
     * @return true if the class is a standard type, false if it's a custom/non-standard type
     */
    private static boolean isStandardType(Class<?> clazz) {
        if (clazz == null) {
            return true; // null is considered standard
        }
        
        // Primitive types and their wrappers are standard
        if (clazz.isPrimitive() || ClassUtilities.toPrimitiveClass(clazz) != clazz) {
            return true;
        }
        
        // Common JDK types that are typically standard
        if (clazz.getName().startsWith("java.") || clazz.getName().startsWith("javax.")) {
            return true;
        }
        
        // Check if this type has any factory conversions (built-in support)
        // Look for any entries in CONVERSION_DB where this type is source or target
        for (MultiKeyMap.MultiKeyEntry<Convert<?>> entry : CONVERSION_DB.entries()) {
            if (entry.keys.length >= 2) {
                Object source = entry.keys[0];
                Object target = entry.keys[1];
                if (source instanceof Class && target instanceof Class) {
                    if (source == clazz || target == clazz) {
                        return true; // Found built-in conversion support
                    }
                }
            }
        }
        
        return false; // No built-in support found, consider it non-standard
    }

    /**
     * Adds an identity conversion (T -> T) for a non-standard type to enable O(1) lookup
     * in hasConverterOverrideFor. This serves as a marker that the type is involved in
     * custom conversions while also providing useful identity conversion functionality.
     * 
     * @param type the type to add identity conversion for
     * @param instanceId the instance ID to use for the conversion
     */
    private static void addIdentityConversionIfNeeded(Class<?> type, long instanceId) {
        if (type != null && USER_DB.get(type, type, instanceId) == null) {
            USER_DB.put(IDENTITY_CONVERTER, type, type, instanceId);
        }
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
                FULL_CONVERSION_CACHE.remove(keys[0], keys[1], keys[2]);
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
