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
    private static final MultiKeyMap<Convert<?>> CONVERSION_DB = new MultiKeyMap<>(4096, 0.70f);
    private static final MultiKeyMap<Convert<?>> USER_DB = new MultiKeyMap<>(256, 0.70f);
    private static final MultiKeyMap<Convert<?>> FULL_CONVERSION_CACHE = new MultiKeyMap<>(1024, 0.75f);
    private static final Map<Class<?>, String> CUSTOM_ARRAY_NAMES = new ClassValueMap<>();
    private static final ClassValueMap<Boolean> SIMPLE_TYPE_CACHE = new ClassValueMap<>();
    private static final ClassValueMap<Boolean> SELF_CONVERSION_CACHE = new ClassValueMap<>();
    private static final AtomicLong INSTANCE_ID_GENERATOR = new AtomicLong(1);
    
    // Compatibility layer: ConversionPair → MultiKeyMap bridge
    private static Convert<?> getFromDB(MultiKeyMap<Convert<?>> db, ConversionPair pair) {
        return db.get(pair.getSource(), pair.getTarget(), pair.getInstanceId());
    }
    
    private static Convert<?> putToDB(MultiKeyMap<Convert<?>> db, Class<?> source, Class<?> target, long instanceId, Convert<?> value) {
        // MultiKeyMap doesn't return previous value, but compatibility layer needs it
        Convert<?> previous = db.get(source, target, instanceId);
        db.put(source, target, instanceId, value);
        return previous;
    }
    
    private static Convert<?> putToDB(MultiKeyMap<Convert<?>> db, ConversionPair pair, Convert<?> value) {
        return putToDB(db, pair.getSource(), pair.getTarget(), pair.getInstanceId(), value);
    }
    
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
        addConversionDB(pair(Byte.class, Number.class), Converter::identity);
        addConversionDB(pair(Short.class, Number.class), Converter::identity);
        addConversionDB(pair(Integer.class, Number.class), Converter::identity);
        addConversionDB(pair(Long.class, Number.class), Converter::identity);
        addConversionDB(pair(Float.class, Number.class), Converter::identity);
        addConversionDB(pair(Double.class, Number.class), Converter::identity);
        addConversionDB(pair(AtomicInteger.class, Number.class), Converter::identity);
        addConversionDB(pair(AtomicLong.class, Number.class), Converter::identity);
        addConversionDB(pair(BigInteger.class, Number.class), Converter::identity);
        addConversionDB(pair(BigDecimal.class, Number.class), Converter::identity);

        // toByte
        addConversionDB(pair(Void.class, byte.class), NumberConversions::toByteZero);
        addConversionDB(pair(Void.class, Byte.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, Byte.class), Converter::identity);
        addConversionDB(pair(Short.class, Byte.class), NumberConversions::toByte);
        addConversionDB(pair(Integer.class, Byte.class), NumberConversions::toByte);
        addConversionDB(pair(Long.class, Byte.class), NumberConversions::toByte);
        addConversionDB(pair(Float.class, Byte.class), NumberConversions::toByte);
        addConversionDB(pair(Double.class, Byte.class), NumberConversions::toByte);
        addConversionDB(pair(Boolean.class, Byte.class), BooleanConversions::toByte);
        addConversionDB(pair(Character.class, Byte.class), CharacterConversions::toByte);
        addConversionDB(pair(BigInteger.class, Byte.class), NumberConversions::toByte);
        addConversionDB(pair(BigDecimal.class, Byte.class), NumberConversions::toByte);
        addConversionDB(pair(Map.class, Byte.class), MapConversions::toByte);
        addConversionDB(pair(String.class, Byte.class), StringConversions::toByte);

        // toShort
        addConversionDB(pair(Void.class, short.class), NumberConversions::toShortZero);
        addConversionDB(pair(Void.class, Short.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, Short.class), NumberConversions::toShort);
        addConversionDB(pair(Short.class, Short.class), Converter::identity);
        addConversionDB(pair(Integer.class, Short.class), NumberConversions::toShort);
        addConversionDB(pair(Long.class, Short.class), NumberConversions::toShort);
        addConversionDB(pair(Float.class, Short.class), NumberConversions::toShort);
        addConversionDB(pair(Double.class, Short.class), NumberConversions::toShort);
        addConversionDB(pair(Boolean.class, Short.class), BooleanConversions::toShort);
        addConversionDB(pair(Character.class, Short.class), CharacterConversions::toShort);
        addConversionDB(pair(BigInteger.class, Short.class), NumberConversions::toShort);
        addConversionDB(pair(BigDecimal.class, Short.class), NumberConversions::toShort);
        addConversionDB(pair(Map.class, Short.class), MapConversions::toShort);
        addConversionDB(pair(String.class, Short.class), StringConversions::toShort);
        addConversionDB(pair(Year.class, Short.class), YearConversions::toShort);

        // toInteger
        addConversionDB(pair(Void.class, int.class), NumberConversions::toIntZero);
        addConversionDB(pair(AtomicInteger.class, int.class), UniversalConversions::atomicIntegerToInt);
        addConversionDB(pair(Void.class, Integer.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, Integer.class), NumberConversions::toInt);
        addConversionDB(pair(Short.class, Integer.class), NumberConversions::toInt);
        addConversionDB(pair(Integer.class, Integer.class), Converter::identity);
        addConversionDB(pair(Long.class, Integer.class), NumberConversions::toInt);
        addConversionDB(pair(Float.class, Integer.class), NumberConversions::toInt);
        addConversionDB(pair(Double.class, Integer.class), NumberConversions::toInt);
        addConversionDB(pair(Boolean.class, Integer.class), BooleanConversions::toInt);
        addConversionDB(pair(Character.class, Integer.class), CharacterConversions::toInt);
        addConversionDB(pair(AtomicInteger.class, Integer.class), NumberConversions::toInt);
        addConversionDB(pair(BigInteger.class, Integer.class), NumberConversions::toInt);
        addConversionDB(pair(BigDecimal.class, Integer.class), NumberConversions::toInt);
        addConversionDB(pair(Map.class, Integer.class), MapConversions::toInt);
        addConversionDB(pair(String.class, Integer.class), StringConversions::toInt);
        addConversionDB(pair(Color.class, Integer.class), ColorConversions::toInteger);
        addConversionDB(pair(Dimension.class, Integer.class), DimensionConversions::toInteger);
        addConversionDB(pair(Point.class, Integer.class), PointConversions::toInteger);
        addConversionDB(pair(Rectangle.class, Integer.class), RectangleConversions::toInteger);
        addConversionDB(pair(Insets.class, Integer.class), InsetsConversions::toInteger);
        addConversionDB(pair(OffsetTime.class, Integer.class), OffsetTimeConversions::toInteger);
        addConversionDB(pair(Year.class, Integer.class), YearConversions::toInt);

        // toLong
        addConversionDB(pair(Void.class, long.class), NumberConversions::toLongZero);
        addConversionDB(pair(AtomicLong.class, long.class), UniversalConversions::atomicLongToLong);
        addConversionDB(pair(Void.class, Long.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, Long.class), NumberConversions::toLong);
        addConversionDB(pair(Short.class, Long.class), NumberConversions::toLong);
        addConversionDB(pair(Integer.class, Long.class), NumberConversions::toLong);
        addConversionDB(pair(Long.class, Long.class), Converter::identity);
        addConversionDB(pair(Float.class, Long.class), NumberConversions::toLong);
        addConversionDB(pair(Double.class, Long.class), NumberConversions::toLong);
        addConversionDB(pair(Boolean.class, Long.class), BooleanConversions::toLong);
        addConversionDB(pair(Character.class, Long.class), CharacterConversions::toLong);
        addConversionDB(pair(BigInteger.class, Long.class), NumberConversions::toLong);
        addConversionDB(pair(BigDecimal.class, Long.class), NumberConversions::toLong);
        addConversionDB(pair(AtomicLong.class, Long.class), NumberConversions::toLong);
        addConversionDB(pair(Date.class, Long.class), DateConversions::toLong);
        addConversionDB(pair(java.sql.Date.class, Long.class), SqlDateConversions::toLong);
        addConversionDB(pair(Timestamp.class, Long.class), TimestampConversions::toLong);
        addConversionDB(pair(Instant.class, Long.class), InstantConversions::toLong);
        addConversionDB(pair(Duration.class, Long.class), DurationConversions::toLong);
        addConversionDB(pair(LocalDate.class, Long.class), LocalDateConversions::toLong);
        addConversionDB(pair(LocalTime.class, Long.class), LocalTimeConversions::toLong);
        addConversionDB(pair(LocalDateTime.class, Long.class), LocalDateTimeConversions::toLong);
        addConversionDB(pair(OffsetTime.class, Long.class), OffsetTimeConversions::toLong);
        addConversionDB(pair(OffsetDateTime.class, Long.class), OffsetDateTimeConversions::toLong);
        addConversionDB(pair(ZonedDateTime.class, Long.class), ZonedDateTimeConversions::toLong);
        addConversionDB(pair(Map.class, Long.class), MapConversions::toLong);
        addConversionDB(pair(String.class, Long.class), StringConversions::toLong);
        addConversionDB(pair(Color.class, Long.class), ColorConversions::toLong);
        addConversionDB(pair(Dimension.class, Long.class), DimensionConversions::toLong);
        addConversionDB(pair(Point.class, Long.class), PointConversions::toLong);
        addConversionDB(pair(Rectangle.class, Long.class), RectangleConversions::toLong);
        addConversionDB(pair(Insets.class, Long.class), InsetsConversions::toLong);
        addConversionDB(pair(Year.class, Long.class), YearConversions::toLong);

        // toFloat
        addConversionDB(pair(Void.class, float.class), NumberConversions::toFloatZero);
        addConversionDB(pair(Void.class, Float.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, Float.class), NumberConversions::toFloat);
        addConversionDB(pair(Short.class, Float.class), NumberConversions::toFloat);
        addConversionDB(pair(Integer.class, Float.class), NumberConversions::toFloat);
        addConversionDB(pair(Long.class, Float.class), NumberConversions::toFloat);
        addConversionDB(pair(Float.class, Float.class), Converter::identity);
        addConversionDB(pair(Double.class, Float.class), NumberConversions::toFloat);
        addConversionDB(pair(Boolean.class, Float.class), BooleanConversions::toFloat);
        addConversionDB(pair(Character.class, Float.class), CharacterConversions::toFloat);
        addConversionDB(pair(BigInteger.class, Float.class), NumberConversions::toFloat);
        addConversionDB(pair(BigDecimal.class, Float.class), NumberConversions::toFloat);
        addConversionDB(pair(Map.class, Float.class), MapConversions::toFloat);
        addConversionDB(pair(String.class, Float.class), StringConversions::toFloat);

        // toDouble
        addConversionDB(pair(Void.class, double.class), NumberConversions::toDoubleZero);
        addConversionDB(pair(Void.class, Double.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, Double.class), NumberConversions::toDouble);
        addConversionDB(pair(Short.class, Double.class), NumberConversions::toDouble);
        addConversionDB(pair(Integer.class, Double.class), NumberConversions::toDouble);
        addConversionDB(pair(Long.class, Double.class), NumberConversions::toDouble);
        addConversionDB(pair(Float.class, Double.class), NumberConversions::toDouble);
        addConversionDB(pair(Double.class, Double.class), Converter::identity);
        addConversionDB(pair(Boolean.class, Double.class), BooleanConversions::toDouble);
        addConversionDB(pair(Character.class, Double.class), CharacterConversions::toDouble);
        addConversionDB(pair(Duration.class, Double.class), DurationConversions::toDouble);
        addConversionDB(pair(Instant.class, Double.class), InstantConversions::toDouble);
        addConversionDB(pair(LocalTime.class, Double.class), LocalTimeConversions::toDouble);
        addConversionDB(pair(LocalDate.class, Double.class), LocalDateConversions::toDouble);
        addConversionDB(pair(LocalDateTime.class, Double.class), LocalDateTimeConversions::toDouble);
        addConversionDB(pair(ZonedDateTime.class, Double.class), ZonedDateTimeConversions::toDouble);
        addConversionDB(pair(OffsetTime.class, Double.class), OffsetTimeConversions::toDouble);
        addConversionDB(pair(OffsetDateTime.class, Double.class), OffsetDateTimeConversions::toDouble);
        addConversionDB(pair(Date.class, Double.class), DateConversions::toDouble);
        addConversionDB(pair(java.sql.Date.class, Double.class), SqlDateConversions::toDouble);
        addConversionDB(pair(Timestamp.class, Double.class), TimestampConversions::toDouble);
        addConversionDB(pair(BigInteger.class, Double.class), NumberConversions::toDouble);
        addConversionDB(pair(BigDecimal.class, Double.class), NumberConversions::toDouble);
        addConversionDB(pair(Map.class, Double.class), MapConversions::toDouble);
        addConversionDB(pair(String.class, Double.class), StringConversions::toDouble);

        // Boolean/boolean conversions supported
        addConversionDB(pair(Void.class, boolean.class), VoidConversions::toBoolean);
        addConversionDB(pair(AtomicBoolean.class, boolean.class), UniversalConversions::atomicBooleanToBoolean);
        addConversionDB(pair(AtomicBoolean.class, Boolean.class), AtomicBooleanConversions::toBoolean);
        addConversionDB(pair(Void.class, Boolean.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        addConversionDB(pair(Short.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        addConversionDB(pair(Integer.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        addConversionDB(pair(Long.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        addConversionDB(pair(Float.class, Boolean.class), NumberConversions::isFloatTypeNotZero);
        addConversionDB(pair(Double.class, Boolean.class), NumberConversions::isFloatTypeNotZero);
        addConversionDB(pair(Boolean.class, Boolean.class), Converter::identity);
        addConversionDB(pair(Character.class, Boolean.class), CharacterConversions::toBoolean);
        addConversionDB(pair(BigInteger.class, Boolean.class), NumberConversions::isBigIntegerNotZero);
        addConversionDB(pair(BigDecimal.class, Boolean.class), NumberConversions::isBigDecimalNotZero);
        addConversionDB(pair(Map.class, Boolean.class), MapConversions::toBoolean);
        addConversionDB(pair(String.class, Boolean.class), StringConversions::toBoolean);
        addConversionDB(pair(Dimension.class, Boolean.class), DimensionConversions::toBoolean);
        addConversionDB(pair(Point.class, Boolean.class), PointConversions::toBoolean);
        addConversionDB(pair(Rectangle.class, Boolean.class), RectangleConversions::toBoolean);
        addConversionDB(pair(Insets.class, Boolean.class), InsetsConversions::toBoolean);
        addConversionDB(pair(UUID.class, Boolean.class), UUIDConversions::toBoolean);

        // Character/char conversions supported
        addConversionDB(pair(Void.class, char.class), VoidConversions::toCharacter);
        addConversionDB(pair(Void.class, Character.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, Character.class), ByteConversions::toCharacter);
        addConversionDB(pair(Short.class, Character.class), NumberConversions::toCharacter);
        addConversionDB(pair(Integer.class, Character.class), NumberConversions::toCharacter);
        addConversionDB(pair(Long.class, Character.class), NumberConversions::toCharacter);
        addConversionDB(pair(Float.class, Character.class), NumberConversions::toCharacter);
        addConversionDB(pair(Double.class, Character.class), NumberConversions::toCharacter);
        addConversionDB(pair(Boolean.class, Character.class), BooleanConversions::toCharacter);
        addConversionDB(pair(Character.class, Character.class), Converter::identity);
        addConversionDB(pair(BigInteger.class, Character.class), NumberConversions::toCharacter);
        addConversionDB(pair(BigDecimal.class, Character.class), NumberConversions::toCharacter);
        addConversionDB(pair(Map.class, Character.class), MapConversions::toCharacter);
        addConversionDB(pair(String.class, Character.class), StringConversions::toCharacter);

        // BigInteger versions supported
        addConversionDB(pair(Void.class, BigInteger.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, BigInteger.class), NumberConversions::integerTypeToBigInteger);
        addConversionDB(pair(Short.class, BigInteger.class), NumberConversions::integerTypeToBigInteger);
        addConversionDB(pair(Integer.class, BigInteger.class), NumberConversions::integerTypeToBigInteger);
        addConversionDB(pair(Long.class, BigInteger.class), NumberConversions::integerTypeToBigInteger);
        addConversionDB(pair(Float.class, BigInteger.class), NumberConversions::floatingPointToBigInteger);
        addConversionDB(pair(Double.class, BigInteger.class), NumberConversions::floatingPointToBigInteger);
        addConversionDB(pair(Boolean.class, BigInteger.class), BooleanConversions::toBigInteger);
        addConversionDB(pair(Character.class, BigInteger.class), CharacterConversions::toBigInteger);
        addConversionDB(pair(BigInteger.class, BigInteger.class), Converter::identity);
        addConversionDB(pair(BigDecimal.class, BigInteger.class), BigDecimalConversions::toBigInteger);
        addConversionDB(pair(Date.class, BigInteger.class), DateConversions::toBigInteger);
        addConversionDB(pair(java.sql.Date.class, BigInteger.class), SqlDateConversions::toBigInteger);
        addConversionDB(pair(Timestamp.class, BigInteger.class), TimestampConversions::toBigInteger);
        addConversionDB(pair(Duration.class, BigInteger.class), DurationConversions::toBigInteger);
        addConversionDB(pair(Instant.class, BigInteger.class), InstantConversions::toBigInteger);
        addConversionDB(pair(LocalTime.class, BigInteger.class), LocalTimeConversions::toBigInteger);
        addConversionDB(pair(LocalDate.class, BigInteger.class), LocalDateConversions::toBigInteger);
        addConversionDB(pair(LocalDateTime.class, BigInteger.class), LocalDateTimeConversions::toBigInteger);
        addConversionDB(pair(ZonedDateTime.class, BigInteger.class), ZonedDateTimeConversions::toBigInteger);
        addConversionDB(pair(OffsetTime.class, BigInteger.class), OffsetTimeConversions::toBigInteger);
        addConversionDB(pair(OffsetDateTime.class, BigInteger.class), OffsetDateTimeConversions::toBigInteger);
        addConversionDB(pair(UUID.class, BigInteger.class), UUIDConversions::toBigInteger);
        addConversionDB(pair(Color.class, BigInteger.class), ColorConversions::toBigInteger);
        addConversionDB(pair(Dimension.class, BigInteger.class), DimensionConversions::toBigInteger);
        addConversionDB(pair(Point.class, BigInteger.class), PointConversions::toBigInteger);
        addConversionDB(pair(Rectangle.class, BigInteger.class), RectangleConversions::toBigInteger);
        addConversionDB(pair(Insets.class, BigInteger.class), InsetsConversions::toBigInteger);
        addConversionDB(pair(Calendar.class, BigInteger.class), CalendarConversions::toBigInteger);  // Restored - bridge has precision difference (millis vs nanos)
        addConversionDB(pair(Map.class, BigInteger.class), MapConversions::toBigInteger);
        addConversionDB(pair(String.class, BigInteger.class), StringConversions::toBigInteger);
        addConversionDB(pair(Year.class, BigInteger.class), YearConversions::toBigInteger);

        // BigDecimal conversions supported
        addConversionDB(pair(Void.class, BigDecimal.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        addConversionDB(pair(Short.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        addConversionDB(pair(Integer.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        addConversionDB(pair(Long.class, BigDecimal.class), NumberConversions::integerTypeToBigDecimal);
        addConversionDB(pair(Float.class, BigDecimal.class), NumberConversions::floatingPointToBigDecimal);
        addConversionDB(pair(Double.class, BigDecimal.class), NumberConversions::floatingPointToBigDecimal);
        addConversionDB(pair(Boolean.class, BigDecimal.class), BooleanConversions::toBigDecimal);
        addConversionDB(pair(Character.class, BigDecimal.class), CharacterConversions::toBigDecimal);
        addConversionDB(pair(BigDecimal.class, BigDecimal.class), Converter::identity);
        addConversionDB(pair(BigInteger.class, BigDecimal.class), BigIntegerConversions::toBigDecimal);
        addConversionDB(pair(Date.class, BigDecimal.class), DateConversions::toBigDecimal);
        addConversionDB(pair(java.sql.Date.class, BigDecimal.class), SqlDateConversions::toBigDecimal);
        addConversionDB(pair(Timestamp.class, BigDecimal.class), TimestampConversions::toBigDecimal);
        addConversionDB(pair(Instant.class, BigDecimal.class), InstantConversions::toBigDecimal);
        addConversionDB(pair(Duration.class, BigDecimal.class), DurationConversions::toBigDecimal);
        addConversionDB(pair(LocalTime.class, BigDecimal.class), LocalTimeConversions::toBigDecimal);
        addConversionDB(pair(LocalDate.class, BigDecimal.class), LocalDateConversions::toBigDecimal);
        addConversionDB(pair(LocalDateTime.class, BigDecimal.class), LocalDateTimeConversions::toBigDecimal);
        addConversionDB(pair(ZonedDateTime.class, BigDecimal.class), ZonedDateTimeConversions::toBigDecimal);
        addConversionDB(pair(OffsetTime.class, BigDecimal.class), OffsetTimeConversions::toBigDecimal);
        addConversionDB(pair(OffsetDateTime.class, BigDecimal.class), OffsetDateTimeConversions::toBigDecimal);
        addConversionDB(pair(UUID.class, BigDecimal.class), UUIDConversions::toBigDecimal);
        addConversionDB(pair(Color.class, BigDecimal.class), ColorConversions::toBigDecimal);
        addConversionDB(pair(Dimension.class, BigDecimal.class), DimensionConversions::toBigDecimal);
        addConversionDB(pair(Insets.class, BigDecimal.class), InsetsConversions::toBigDecimal);
        addConversionDB(pair(Point.class, BigDecimal.class), PointConversions::toBigDecimal);
        addConversionDB(pair(Rectangle.class, BigDecimal.class), RectangleConversions::toBigDecimal);
        addConversionDB(pair(Calendar.class, BigDecimal.class), CalendarConversions::toBigDecimal);
        addConversionDB(pair(Map.class, BigDecimal.class), MapConversions::toBigDecimal);
        addConversionDB(pair(String.class, BigDecimal.class), StringConversions::toBigDecimal);

        // AtomicBoolean conversions supported
        addConversionDB(pair(Void.class, AtomicBoolean.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        addConversionDB(pair(Short.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        addConversionDB(pair(Integer.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        addConversionDB(pair(Long.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        addConversionDB(pair(Float.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        addConversionDB(pair(Double.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        addConversionDB(pair(Boolean.class, AtomicBoolean.class), BooleanConversions::toAtomicBoolean);
        addConversionDB(pair(Character.class, AtomicBoolean.class), CharacterConversions::toAtomicBoolean);
        addConversionDB(pair(BigInteger.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        addConversionDB(pair(BigDecimal.class, AtomicBoolean.class), NumberConversions::toAtomicBoolean);
        addConversionDB(pair(AtomicBoolean.class, AtomicBoolean.class), AtomicBooleanConversions::toAtomicBoolean);
        addConversionDB(pair(Map.class, AtomicBoolean.class), MapConversions::toAtomicBoolean);
        addConversionDB(pair(String.class, AtomicBoolean.class), StringConversions::toAtomicBoolean);

        // AtomicInteger conversions supported
        addConversionDB(pair(Void.class, AtomicInteger.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        addConversionDB(pair(Short.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        addConversionDB(pair(Integer.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        addConversionDB(pair(Long.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        addConversionDB(pair(Float.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        addConversionDB(pair(Double.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        addConversionDB(pair(Boolean.class, AtomicInteger.class), BooleanConversions::toAtomicInteger);
        addConversionDB(pair(Character.class, AtomicInteger.class), CharacterConversions::toAtomicInteger);
        addConversionDB(pair(BigInteger.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        addConversionDB(pair(BigDecimal.class, AtomicInteger.class), NumberConversions::toAtomicInteger);
        addConversionDB(pair(AtomicInteger.class, AtomicInteger.class), AtomicIntegerConversions::toAtomicInteger);
        addConversionDB(pair(OffsetTime.class, AtomicInteger.class), OffsetTimeConversions::toAtomicInteger);
        addConversionDB(pair(Map.class, AtomicInteger.class), MapConversions::toAtomicInteger);
        addConversionDB(pair(String.class, AtomicInteger.class), StringConversions::toAtomicInteger);

        // AtomicLong conversions supported
        addConversionDB(pair(Void.class, AtomicLong.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, AtomicLong.class), NumberConversions::toAtomicLong);
        addConversionDB(pair(Short.class, AtomicLong.class), NumberConversions::toAtomicLong);
        addConversionDB(pair(Integer.class, AtomicLong.class), NumberConversions::toAtomicLong);
        addConversionDB(pair(Long.class, AtomicLong.class), NumberConversions::toAtomicLong);
        addConversionDB(pair(Float.class, AtomicLong.class), NumberConversions::toAtomicLong);
        addConversionDB(pair(Double.class, AtomicLong.class), NumberConversions::toAtomicLong);
        addConversionDB(pair(Boolean.class, AtomicLong.class), BooleanConversions::toAtomicLong);
        addConversionDB(pair(Character.class, AtomicLong.class), CharacterConversions::toAtomicLong);
        addConversionDB(pair(BigInteger.class, AtomicLong.class), NumberConversions::toAtomicLong);
        addConversionDB(pair(BigDecimal.class, AtomicLong.class), NumberConversions::toAtomicLong);
        addConversionDB(pair(AtomicLong.class, AtomicLong.class), AtomicLongConversions::toAtomicLong);
        addConversionDB(pair(Date.class, AtomicLong.class), DateConversions::toAtomicLong);
        addConversionDB(pair(java.sql.Date.class, AtomicLong.class), SqlDateConversions::toAtomicLong);
        addConversionDB(pair(Timestamp.class, AtomicLong.class), DateConversions::toAtomicLong);
        addConversionDB(pair(Instant.class, AtomicLong.class), InstantConversions::toAtomicLong);
        addConversionDB(pair(Duration.class, AtomicLong.class), DurationConversions::toAtomicLong);
        addConversionDB(pair(LocalDate.class, AtomicLong.class), LocalDateConversions::toAtomicLong);
        addConversionDB(pair(LocalTime.class, AtomicLong.class), LocalTimeConversions::toAtomicLong);
        addConversionDB(pair(LocalDateTime.class, AtomicLong.class), LocalDateTimeConversions::toAtomicLong);
        addConversionDB(pair(ZonedDateTime.class, AtomicLong.class), ZonedDateTimeConversions::toAtomicLong);
        addConversionDB(pair(OffsetTime.class, AtomicLong.class), OffsetTimeConversions::toAtomicLong);
        addConversionDB(pair(OffsetDateTime.class, AtomicLong.class), OffsetDateTimeConversions::toAtomicLong);
        addConversionDB(pair(Map.class, AtomicLong.class), MapConversions::toAtomicLong);
        addConversionDB(pair(String.class, AtomicLong.class), StringConversions::toAtomicLong);

        // Date conversions supported
        addConversionDB(pair(Void.class, Date.class), VoidConversions::toNull);
        addConversionDB(pair(Long.class, Date.class), NumberConversions::toDate);
        addConversionDB(pair(Double.class, Date.class), DoubleConversions::toDate);
        addConversionDB(pair(BigInteger.class, Date.class), BigIntegerConversions::toDate);
        addConversionDB(pair(BigDecimal.class, Date.class), BigDecimalConversions::toDate);
        addConversionDB(pair(Date.class, Date.class), DateConversions::toDate);
        addConversionDB(pair(java.sql.Date.class, Date.class), SqlDateConversions::toDate);
        addConversionDB(pair(Timestamp.class, Date.class), TimestampConversions::toDate);
        addConversionDB(pair(Instant.class, Date.class), InstantConversions::toDate);
        addConversionDB(pair(LocalDate.class, Date.class), LocalDateConversions::toDate);
        addConversionDB(pair(LocalDateTime.class, Date.class), LocalDateTimeConversions::toDate);
        addConversionDB(pair(ZonedDateTime.class, Date.class), ZonedDateTimeConversions::toDate);
        addConversionDB(pair(OffsetDateTime.class, Date.class), OffsetDateTimeConversions::toDate);
        addConversionDB(pair(Map.class, Date.class), MapConversions::toDate);
        addConversionDB(pair(String.class, Date.class), StringConversions::toDate);

        // java.sql.Date conversion supported
        addConversionDB(pair(Void.class, java.sql.Date.class), VoidConversions::toNull);
        addConversionDB(pair(Long.class, java.sql.Date.class), NumberConversions::toSqlDate);
        addConversionDB(pair(Double.class, java.sql.Date.class), DoubleConversions::toSqlDate);
        addConversionDB(pair(BigInteger.class, java.sql.Date.class), BigIntegerConversions::toSqlDate);
        addConversionDB(pair(BigDecimal.class, java.sql.Date.class), BigDecimalConversions::toSqlDate);
        addConversionDB(pair(java.sql.Date.class, java.sql.Date.class), SqlDateConversions::toSqlDate);
        addConversionDB(pair(Date.class, java.sql.Date.class), DateConversions::toSqlDate);
        addConversionDB(pair(Timestamp.class, java.sql.Date.class), TimestampConversions::toSqlDate);
        addConversionDB(pair(Duration.class, java.sql.Date.class), DurationConversions::toSqlDate);
        addConversionDB(pair(Instant.class, java.sql.Date.class), InstantConversions::toSqlDate);
        addConversionDB(pair(LocalDate.class, java.sql.Date.class), LocalDateConversions::toSqlDate);
        addConversionDB(pair(LocalDateTime.class, java.sql.Date.class), LocalDateTimeConversions::toSqlDate);
        addConversionDB(pair(ZonedDateTime.class, java.sql.Date.class), ZonedDateTimeConversions::toSqlDate);
        addConversionDB(pair(OffsetDateTime.class, java.sql.Date.class), OffsetDateTimeConversions::toSqlDate);
        addConversionDB(pair(Map.class, java.sql.Date.class), MapConversions::toSqlDate);
        addConversionDB(pair(String.class, java.sql.Date.class), StringConversions::toSqlDate);

        // Timestamp conversions supported
        addConversionDB(pair(Void.class, Timestamp.class), VoidConversions::toNull);
        addConversionDB(pair(Long.class, Timestamp.class), NumberConversions::toTimestamp);
        addConversionDB(pair(Double.class, Timestamp.class), DoubleConversions::toTimestamp);
        addConversionDB(pair(BigInteger.class, Timestamp.class), BigIntegerConversions::toTimestamp);
        addConversionDB(pair(BigDecimal.class, Timestamp.class), BigDecimalConversions::toTimestamp);
        addConversionDB(pair(Timestamp.class, Timestamp.class), DateConversions::toTimestamp);
        addConversionDB(pair(java.sql.Date.class, Timestamp.class), SqlDateConversions::toTimestamp);
        addConversionDB(pair(Date.class, Timestamp.class), DateConversions::toTimestamp);
        addConversionDB(pair(Duration.class, Timestamp.class), DurationConversions::toTimestamp);
        addConversionDB(pair(Instant.class, Timestamp.class), InstantConversions::toTimestamp);
        addConversionDB(pair(LocalDate.class, Timestamp.class), LocalDateConversions::toTimestamp);
        addConversionDB(pair(LocalDateTime.class, Timestamp.class), LocalDateTimeConversions::toTimestamp);
        addConversionDB(pair(ZonedDateTime.class, Timestamp.class), ZonedDateTimeConversions::toTimestamp);
        addConversionDB(pair(OffsetDateTime.class, Timestamp.class), OffsetDateTimeConversions::toTimestamp);
        addConversionDB(pair(Map.class, Timestamp.class), MapConversions::toTimestamp);
        addConversionDB(pair(String.class, Timestamp.class), StringConversions::toTimestamp);

        // Calendar conversions supported
        addConversionDB(pair(Void.class, Calendar.class), VoidConversions::toNull);
        addConversionDB(pair(Long.class, Calendar.class), NumberConversions::toCalendar);
        addConversionDB(pair(Double.class, Calendar.class), DoubleConversions::toCalendar);
        addConversionDB(pair(BigInteger.class, Calendar.class), BigIntegerConversions::toCalendar);
        addConversionDB(pair(BigDecimal.class, Calendar.class), BigDecimalConversions::toCalendar);
        addConversionDB(pair(Date.class, Calendar.class), DateConversions::toCalendar);
        addConversionDB(pair(java.sql.Date.class, Calendar.class), SqlDateConversions::toCalendar);
        addConversionDB(pair(Timestamp.class, Calendar.class), TimestampConversions::toCalendar);
        addConversionDB(pair(Instant.class, Calendar.class), InstantConversions::toCalendar);
        addConversionDB(pair(LocalTime.class, Calendar.class), LocalTimeConversions::toCalendar);
        addConversionDB(pair(LocalDate.class, Calendar.class), LocalDateConversions::toCalendar);
        addConversionDB(pair(LocalDateTime.class, Calendar.class), LocalDateTimeConversions::toCalendar);
        addConversionDB(pair(ZonedDateTime.class, Calendar.class), ZonedDateTimeConversions::toCalendar);
        addConversionDB(pair(OffsetDateTime.class, Calendar.class), OffsetDateTimeConversions::toCalendar);
        addConversionDB(pair(Calendar.class, Calendar.class), CalendarConversions::clone);
        addConversionDB(pair(Map.class, Calendar.class), MapConversions::toCalendar);
        addConversionDB(pair(String.class, Calendar.class), StringConversions::toCalendar);

        // LocalDate conversions supported
        addConversionDB(pair(Void.class, LocalDate.class), VoidConversions::toNull);
        addConversionDB(pair(Long.class, LocalDate.class), NumberConversions::toLocalDate);
        addConversionDB(pair(Double.class, LocalDate.class), DoubleConversions::toLocalDate);
        addConversionDB(pair(BigInteger.class, LocalDate.class), BigIntegerConversions::toLocalDate);
        addConversionDB(pair(BigDecimal.class, LocalDate.class), BigDecimalConversions::toLocalDate);
        addConversionDB(pair(java.sql.Date.class, LocalDate.class), SqlDateConversions::toLocalDate);
        addConversionDB(pair(Timestamp.class, LocalDate.class), DateConversions::toLocalDate);
        addConversionDB(pair(Date.class, LocalDate.class), DateConversions::toLocalDate);
        addConversionDB(pair(Instant.class, LocalDate.class), InstantConversions::toLocalDate);
        addConversionDB(pair(Calendar.class, LocalDate.class), CalendarConversions::toLocalDate);
        addConversionDB(pair(LocalDate.class, LocalDate.class), Converter::identity);
        addConversionDB(pair(LocalDateTime.class, LocalDate.class), LocalDateTimeConversions::toLocalDate);
        addConversionDB(pair(ZonedDateTime.class, LocalDate.class), ZonedDateTimeConversions::toLocalDate);
        addConversionDB(pair(OffsetDateTime.class, LocalDate.class), OffsetDateTimeConversions::toLocalDate);
        addConversionDB(pair(Map.class, LocalDate.class), MapConversions::toLocalDate);
        addConversionDB(pair(String.class, LocalDate.class), StringConversions::toLocalDate);

        // LocalDateTime conversions supported
        addConversionDB(pair(Void.class, LocalDateTime.class), VoidConversions::toNull);
        addConversionDB(pair(Long.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        addConversionDB(pair(Double.class, LocalDateTime.class), DoubleConversions::toLocalDateTime);
        addConversionDB(pair(BigInteger.class, LocalDateTime.class), BigIntegerConversions::toLocalDateTime);
        addConversionDB(pair(BigDecimal.class, LocalDateTime.class), BigDecimalConversions::toLocalDateTime);
        addConversionDB(pair(java.sql.Date.class, LocalDateTime.class), SqlDateConversions::toLocalDateTime);
        addConversionDB(pair(Timestamp.class, LocalDateTime.class), TimestampConversions::toLocalDateTime);
        addConversionDB(pair(Date.class, LocalDateTime.class), DateConversions::toLocalDateTime);
        addConversionDB(pair(Instant.class, LocalDateTime.class), InstantConversions::toLocalDateTime);
        addConversionDB(pair(LocalDateTime.class, LocalDateTime.class), LocalDateTimeConversions::toLocalDateTime);
        addConversionDB(pair(LocalDate.class, LocalDateTime.class), LocalDateConversions::toLocalDateTime);
        addConversionDB(pair(Calendar.class, LocalDateTime.class), CalendarConversions::toLocalDateTime);
        addConversionDB(pair(ZonedDateTime.class, LocalDateTime.class), ZonedDateTimeConversions::toLocalDateTime);
        addConversionDB(pair(OffsetDateTime.class, LocalDateTime.class), OffsetDateTimeConversions::toLocalDateTime);
        addConversionDB(pair(Map.class, LocalDateTime.class), MapConversions::toLocalDateTime);
        addConversionDB(pair(String.class, LocalDateTime.class), StringConversions::toLocalDateTime);

        // LocalTime conversions supported
        addConversionDB(pair(Void.class, LocalTime.class), VoidConversions::toNull);
        addConversionDB(pair(Long.class, LocalTime.class), NumberConversions::longNanosToLocalTime);
        addConversionDB(pair(Double.class, LocalTime.class), DoubleConversions::toLocalTime);
        addConversionDB(pair(BigInteger.class, LocalTime.class), BigIntegerConversions::toLocalTime);
        addConversionDB(pair(BigDecimal.class, LocalTime.class), BigDecimalConversions::toLocalTime);
        addConversionDB(pair(Timestamp.class, LocalTime.class), DateConversions::toLocalTime);
        addConversionDB(pair(Date.class, LocalTime.class), DateConversions::toLocalTime);
        addConversionDB(pair(Instant.class, LocalTime.class), InstantConversions::toLocalTime);
        addConversionDB(pair(LocalDateTime.class, LocalTime.class), LocalDateTimeConversions::toLocalTime);
        addConversionDB(pair(LocalTime.class, LocalTime.class), Converter::identity);
        addConversionDB(pair(ZonedDateTime.class, LocalTime.class), ZonedDateTimeConversions::toLocalTime);
        addConversionDB(pair(OffsetDateTime.class, LocalTime.class), OffsetDateTimeConversions::toLocalTime);
        addConversionDB(pair(Map.class, LocalTime.class), MapConversions::toLocalTime);
        addConversionDB(pair(String.class, LocalTime.class), StringConversions::toLocalTime);

        // ZonedDateTime conversions supported
        addConversionDB(pair(Void.class, ZonedDateTime.class), VoidConversions::toNull);
        addConversionDB(pair(Long.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        addConversionDB(pair(Double.class, ZonedDateTime.class), DoubleConversions::toZonedDateTime);
        addConversionDB(pair(BigInteger.class, ZonedDateTime.class), BigIntegerConversions::toZonedDateTime);
        addConversionDB(pair(BigDecimal.class, ZonedDateTime.class), BigDecimalConversions::toZonedDateTime);
        addConversionDB(pair(java.sql.Date.class, ZonedDateTime.class), SqlDateConversions::toZonedDateTime);
        addConversionDB(pair(Timestamp.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        addConversionDB(pair(Date.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        addConversionDB(pair(Instant.class, ZonedDateTime.class), InstantConversions::toZonedDateTime);
        addConversionDB(pair(LocalDate.class, ZonedDateTime.class), LocalDateConversions::toZonedDateTime);
        addConversionDB(pair(LocalDateTime.class, ZonedDateTime.class), LocalDateTimeConversions::toZonedDateTime);
        addConversionDB(pair(ZonedDateTime.class, ZonedDateTime.class), Converter::identity);
        addConversionDB(pair(OffsetDateTime.class, ZonedDateTime.class), OffsetDateTimeConversions::toZonedDateTime);
        addConversionDB(pair(Calendar.class, ZonedDateTime.class), CalendarConversions::toZonedDateTime);
        addConversionDB(pair(Map.class, ZonedDateTime.class), MapConversions::toZonedDateTime);
        addConversionDB(pair(String.class, ZonedDateTime.class), StringConversions::toZonedDateTime);

        // toOffsetDateTime
        addConversionDB(pair(Void.class, OffsetDateTime.class), VoidConversions::toNull);
        addConversionDB(pair(OffsetDateTime.class, OffsetDateTime.class), Converter::identity);
        addConversionDB(pair(Map.class, OffsetDateTime.class), MapConversions::toOffsetDateTime);
        addConversionDB(pair(String.class, OffsetDateTime.class), StringConversions::toOffsetDateTime);
        addConversionDB(pair(Long.class, OffsetDateTime.class), NumberConversions::toOffsetDateTime);
        addConversionDB(pair(Double.class, OffsetDateTime.class), DoubleConversions::toOffsetDateTime);
        addConversionDB(pair(BigInteger.class, OffsetDateTime.class), BigIntegerConversions::toOffsetDateTime);
        addConversionDB(pair(BigDecimal.class, OffsetDateTime.class), BigDecimalConversions::toOffsetDateTime);
        addConversionDB(pair(java.sql.Date.class, OffsetDateTime.class), SqlDateConversions::toOffsetDateTime);
        addConversionDB(pair(Date.class, OffsetDateTime.class), DateConversions::toOffsetDateTime);
        addConversionDB(pair(Timestamp.class, OffsetDateTime.class), TimestampConversions::toOffsetDateTime);
        addConversionDB(pair(LocalDate.class, OffsetDateTime.class), LocalDateConversions::toOffsetDateTime);
        addConversionDB(pair(Instant.class, OffsetDateTime.class), InstantConversions::toOffsetDateTime);
        addConversionDB(pair(ZonedDateTime.class, OffsetDateTime.class), ZonedDateTimeConversions::toOffsetDateTime);
        addConversionDB(pair(LocalDateTime.class, OffsetDateTime.class), LocalDateTimeConversions::toOffsetDateTime);

        // toOffsetTime
        addConversionDB(pair(Void.class, OffsetTime.class), VoidConversions::toNull);
        addConversionDB(pair(Integer.class, OffsetTime.class), NumberConversions::toOffsetTime);
        addConversionDB(pair(Long.class, OffsetTime.class), NumberConversions::toOffsetTime);
        addConversionDB(pair(Double.class, OffsetTime.class), DoubleConversions::toOffsetTime);
        addConversionDB(pair(BigInteger.class, OffsetTime.class), BigIntegerConversions::toOffsetTime);
        addConversionDB(pair(BigDecimal.class, OffsetTime.class), BigDecimalConversions::toOffsetTime);
        addConversionDB(pair(OffsetTime.class, OffsetTime.class), Converter::identity);
        addConversionDB(pair(OffsetDateTime.class, OffsetTime.class), OffsetDateTimeConversions::toOffsetTime);
        addConversionDB(pair(Map.class, OffsetTime.class), MapConversions::toOffsetTime);
        addConversionDB(pair(String.class, OffsetTime.class), StringConversions::toOffsetTime);

        // UUID conversions supported
        addConversionDB(pair(Void.class, UUID.class), VoidConversions::toNull);
        addConversionDB(pair(UUID.class, UUID.class), Converter::identity);
        addConversionDB(pair(String.class, UUID.class), StringConversions::toUUID);
        addConversionDB(pair(Boolean.class, UUID.class), BooleanConversions::toUUID);
        addConversionDB(pair(BigInteger.class, UUID.class), BigIntegerConversions::toUUID);
        addConversionDB(pair(BigDecimal.class, UUID.class), BigDecimalConversions::toUUID);
        addConversionDB(pair(Map.class, UUID.class), MapConversions::toUUID);

        // Class conversions supported
        addConversionDB(pair(Void.class, Class.class), VoidConversions::toNull);
        addConversionDB(pair(Class.class, Class.class), Converter::identity);
        addConversionDB(pair(Map.class, Class.class), MapConversions::toClass);
        addConversionDB(pair(String.class, Class.class), StringConversions::toClass);

        // Color conversions supported
        addConversionDB(pair(Void.class, Color.class), VoidConversions::toNull);
        addConversionDB(pair(Color.class, Color.class), Converter::identity);
        addConversionDB(pair(String.class, Color.class), StringConversions::toColor);
        addConversionDB(pair(Map.class, Color.class), MapConversions::toColor);
        addConversionDB(pair(Integer.class, Color.class), NumberConversions::toColor);
        addConversionDB(pair(Long.class, Color.class), NumberConversions::toColor);
        addConversionDB(pair(int[].class, Color.class), ArrayConversions::toColor);

        // Dimension conversions supported
        addConversionDB(pair(Void.class, Dimension.class), VoidConversions::toNull);
        addConversionDB(pair(Dimension.class, Dimension.class), Converter::identity);
        addConversionDB(pair(String.class, Dimension.class), StringConversions::toDimension);
        addConversionDB(pair(Map.class, Dimension.class), MapConversions::toDimension);
        addConversionDB(pair(Integer.class, Dimension.class), NumberConversions::toDimension);
        addConversionDB(pair(Long.class, Dimension.class), NumberConversions::toDimension);
        addConversionDB(pair(BigInteger.class, Dimension.class), NumberConversions::toDimension);
        addConversionDB(pair(BigDecimal.class, Dimension.class), NumberConversions::bigDecimalToDimension);
        addConversionDB(pair(Boolean.class, Dimension.class), NumberConversions::booleanToDimension);
        addConversionDB(pair(int[].class, Dimension.class), ArrayConversions::toDimension);
        addConversionDB(pair(Rectangle.class, Dimension.class), RectangleConversions::toDimension);
        addConversionDB(pair(Insets.class, Dimension.class), InsetsConversions::toDimension);
        addConversionDB(pair(Point.class, Dimension.class), PointConversions::toDimension);

        // Point conversions supported
        addConversionDB(pair(Void.class, Point.class), VoidConversions::toNull);
        addConversionDB(pair(Point.class, Point.class), Converter::identity);
        addConversionDB(pair(String.class, Point.class), StringConversions::toPoint);
        addConversionDB(pair(Map.class, Point.class), MapConversions::toPoint);
        addConversionDB(pair(Integer.class, Point.class), NumberConversions::toPoint);
        addConversionDB(pair(Long.class, Point.class), NumberConversions::toPoint);
        addConversionDB(pair(BigInteger.class, Point.class), NumberConversions::toPoint);
        addConversionDB(pair(BigDecimal.class, Point.class), NumberConversions::bigDecimalToPoint);
        addConversionDB(pair(Boolean.class, Point.class), NumberConversions::booleanToPoint);
        addConversionDB(pair(int[].class, Point.class), ArrayConversions::toPoint);
        addConversionDB(pair(Dimension.class, Point.class), DimensionConversions::toPoint);
        addConversionDB(pair(Rectangle.class, Point.class), RectangleConversions::toPoint);
        addConversionDB(pair(Insets.class, Point.class), InsetsConversions::toPoint);

        // Rectangle conversions supported
        addConversionDB(pair(Void.class, Rectangle.class), VoidConversions::toNull);
        addConversionDB(pair(Rectangle.class, Rectangle.class), Converter::identity);
        addConversionDB(pair(String.class, Rectangle.class), StringConversions::toRectangle);
        addConversionDB(pair(Map.class, Rectangle.class), MapConversions::toRectangle);
        addConversionDB(pair(Integer.class, Rectangle.class), NumberConversions::integerToRectangle);
        addConversionDB(pair(Long.class, Rectangle.class), NumberConversions::longToRectangle);
        addConversionDB(pair(BigInteger.class, Rectangle.class), NumberConversions::bigIntegerToRectangle);
        addConversionDB(pair(BigDecimal.class, Rectangle.class), NumberConversions::bigDecimalToRectangle);
        addConversionDB(pair(Boolean.class, Rectangle.class), NumberConversions::booleanToRectangle);
        addConversionDB(pair(int[].class, Rectangle.class), ArrayConversions::toRectangle);
        addConversionDB(pair(Point.class, Rectangle.class), PointConversions::toRectangle);
        addConversionDB(pair(Dimension.class, Rectangle.class), DimensionConversions::toRectangle);
        addConversionDB(pair(Insets.class, Rectangle.class), InsetsConversions::toRectangle);

        // Insets conversions supported
        addConversionDB(pair(Void.class, Insets.class), VoidConversions::toNull);
        addConversionDB(pair(Insets.class, Insets.class), Converter::identity);
        addConversionDB(pair(String.class, Insets.class), StringConversions::toInsets);
        addConversionDB(pair(Map.class, Insets.class), MapConversions::toInsets);
        addConversionDB(pair(Integer.class, Insets.class), NumberConversions::integerToInsets);
        addConversionDB(pair(Long.class, Insets.class), NumberConversions::longToInsets);
        addConversionDB(pair(BigInteger.class, Insets.class), NumberConversions::bigIntegerToInsets);
        addConversionDB(pair(BigDecimal.class, Insets.class), NumberConversions::bigDecimalToInsets);
        addConversionDB(pair(Boolean.class, Insets.class), NumberConversions::booleanToInsets);
        addConversionDB(pair(int[].class, Insets.class), ArrayConversions::toInsets);
        addConversionDB(pair(Point.class, Insets.class), PointConversions::toInsets);
        addConversionDB(pair(Dimension.class, Insets.class), DimensionConversions::toInsets);
        addConversionDB(pair(Rectangle.class, Insets.class), RectangleConversions::toInsets);

        // toFile
        addConversionDB(pair(Void.class, File.class), VoidConversions::toNull);
        addConversionDB(pair(File.class, File.class), Converter::identity);
        addConversionDB(pair(String.class, File.class), StringConversions::toFile);
        addConversionDB(pair(Map.class, File.class), MapConversions::toFile);
        addConversionDB(pair(URI.class, File.class), UriConversions::toFile);
        addConversionDB(pair(Path.class, File.class), PathConversions::toFile);
        addConversionDB(pair(char[].class, File.class), ArrayConversions::charArrayToFile);
        addConversionDB(pair(byte[].class, File.class), ArrayConversions::byteArrayToFile);

        // toPath
        addConversionDB(pair(Void.class, Path.class), VoidConversions::toNull);
        addConversionDB(pair(Path.class, Path.class), Converter::identity);
        addConversionDB(pair(String.class, Path.class), StringConversions::toPath);
        addConversionDB(pair(Map.class, Path.class), MapConversions::toPath);
        addConversionDB(pair(URI.class, Path.class), UriConversions::toPath);
        addConversionDB(pair(File.class, Path.class), FileConversions::toPath);
        addConversionDB(pair(char[].class, Path.class), ArrayConversions::charArrayToPath);
        addConversionDB(pair(byte[].class, Path.class), ArrayConversions::byteArrayToPath);

        // Locale conversions supported
        addConversionDB(pair(Void.class, Locale.class), VoidConversions::toNull);
        addConversionDB(pair(Locale.class, Locale.class), Converter::identity);
        addConversionDB(pair(String.class, Locale.class), StringConversions::toLocale);
        addConversionDB(pair(Map.class, Locale.class), MapConversions::toLocale);

        // String conversions supported
        addConversionDB(pair(Void.class, String.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, String.class), StringConversions::toString);
        addConversionDB(pair(Short.class, String.class), StringConversions::toString);
        addConversionDB(pair(Integer.class, String.class), StringConversions::toString);
        addConversionDB(pair(Long.class, String.class), StringConversions::toString);
        addConversionDB(pair(Float.class, String.class), NumberConversions::floatToString);
        addConversionDB(pair(Double.class, String.class), NumberConversions::doubleToString);
        addConversionDB(pair(Boolean.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(Character.class, String.class), CharacterConversions::toString);
        addConversionDB(pair(BigInteger.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(BigDecimal.class, String.class), BigDecimalConversions::toString);
        addConversionDB(pair(byte[].class, String.class), ByteArrayConversions::toString);
        addConversionDB(pair(char[].class, String.class), CharArrayConversions::toString);
        addConversionDB(pair(Character[].class, String.class), CharacterArrayConversions::toString);
        addConversionDB(pair(ByteBuffer.class, String.class), ByteBufferConversions::toString);
        addConversionDB(pair(CharBuffer.class, String.class), CharBufferConversions::toString);
        addConversionDB(pair(Class.class, String.class), ClassConversions::toString);
        addConversionDB(pair(Date.class, String.class), DateConversions::toString);
        addConversionDB(pair(Calendar.class, String.class), CalendarConversions::toString);
        addConversionDB(pair(java.sql.Date.class, String.class), SqlDateConversions::toString);
        addConversionDB(pair(Timestamp.class, String.class), TimestampConversions::toString);
        addConversionDB(pair(LocalDate.class, String.class), LocalDateConversions::toString);
        addConversionDB(pair(LocalTime.class, String.class), LocalTimeConversions::toString);
        addConversionDB(pair(LocalDateTime.class, String.class), LocalDateTimeConversions::toString);
        addConversionDB(pair(ZonedDateTime.class, String.class), ZonedDateTimeConversions::toString);
        addConversionDB(pair(UUID.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(Color.class, String.class), ColorConversions::toString);
        addConversionDB(pair(Dimension.class, String.class), DimensionConversions::toString);
        addConversionDB(pair(Point.class, String.class), PointConversions::toString);
        addConversionDB(pair(Rectangle.class, String.class), RectangleConversions::toString);
        addConversionDB(pair(Insets.class, String.class), InsetsConversions::toString);
        addConversionDB(pair(Map.class, String.class), MapConversions::toString);
        addConversionDB(pair(Enum.class, String.class), StringConversions::enumToString);
        addConversionDB(pair(String.class, String.class), Converter::identity);
        addConversionDB(pair(Duration.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(Instant.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(MonthDay.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(YearMonth.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(Period.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(ZoneId.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(ZoneOffset.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(OffsetTime.class, String.class), OffsetTimeConversions::toString);
        addConversionDB(pair(OffsetDateTime.class, String.class), OffsetDateTimeConversions::toString);
        addConversionDB(pair(Year.class, String.class), YearConversions::toString);
        addConversionDB(pair(Locale.class, String.class), LocaleConversions::toString);
        addConversionDB(pair(URI.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(URL.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(File.class, String.class), FileConversions::toString);
        addConversionDB(pair(Path.class, String.class), PathConversions::toString);
        addConversionDB(pair(TimeZone.class, String.class), TimeZoneConversions::toString);
        addConversionDB(pair(Pattern.class, String.class), PatternConversions::toString);
        addConversionDB(pair(Currency.class, String.class), CurrencyConversions::toString);
        addConversionDB(pair(StringBuilder.class, String.class), UniversalConversions::toString);
        addConversionDB(pair(StringBuffer.class, String.class), UniversalConversions::toString);

        // Currency conversions
        addConversionDB(pair(Void.class, Currency.class), VoidConversions::toNull);
        addConversionDB(pair(Currency.class, Currency.class), Converter::identity);
        addConversionDB(pair(String.class, Currency.class), StringConversions::toCurrency);
        addConversionDB(pair(Map.class, Currency.class), MapConversions::toCurrency);

        // Pattern conversions
        addConversionDB(pair(Void.class, Pattern.class), VoidConversions::toNull);
        addConversionDB(pair(Pattern.class, Pattern.class), Converter::identity);
        addConversionDB(pair(String.class, Pattern.class), StringConversions::toPattern);
        addConversionDB(pair(Map.class, Pattern.class), MapConversions::toPattern);

        // URL conversions
        addConversionDB(pair(Void.class, URL.class), VoidConversions::toNull);
        addConversionDB(pair(URL.class, URL.class), Converter::identity);
        addConversionDB(pair(URI.class, URL.class), UriConversions::toURL);
        addConversionDB(pair(String.class, URL.class), StringConversions::toURL);
        addConversionDB(pair(Map.class, URL.class), MapConversions::toURL);
        addConversionDB(pair(File.class, URL.class), FileConversions::toURL);
        addConversionDB(pair(Path.class, URL.class), PathConversions::toURL);

        // URI Conversions
        addConversionDB(pair(Void.class, URI.class), VoidConversions::toNull);
        addConversionDB(pair(URI.class, URI.class), Converter::identity);
        addConversionDB(pair(URL.class, URI.class), UrlConversions::toURI);
        addConversionDB(pair(String.class, URI.class), StringConversions::toURI);
        addConversionDB(pair(Map.class, URI.class), MapConversions::toURI);
        addConversionDB(pair(File.class, URI.class), FileConversions::toURI);
        addConversionDB(pair(Path.class, URI.class), PathConversions::toURI);

        // TimeZone Conversions
        addConversionDB(pair(Void.class, TimeZone.class), VoidConversions::toNull);
        addConversionDB(pair(TimeZone.class, TimeZone.class), Converter::identity);
        addConversionDB(pair(String.class, TimeZone.class), StringConversions::toTimeZone);
        addConversionDB(pair(Map.class, TimeZone.class), MapConversions::toTimeZone);
        addConversionDB(pair(ZoneId.class, TimeZone.class), ZoneIdConversions::toTimeZone);
        addConversionDB(pair(ZoneOffset.class, TimeZone.class), ZoneOffsetConversions::toTimeZone);

        // Duration conversions supported
        addConversionDB(pair(Void.class, Duration.class), VoidConversions::toNull);
        addConversionDB(pair(Duration.class, Duration.class), Converter::identity);
        addConversionDB(pair(Long.class, Duration.class), NumberConversions::longNanosToDuration);
        addConversionDB(pair(Double.class, Duration.class), DoubleConversions::toDuration);
        addConversionDB(pair(BigInteger.class, Duration.class), BigIntegerConversions::toDuration);
        addConversionDB(pair(BigDecimal.class, Duration.class), BigDecimalConversions::toDuration);
        addConversionDB(pair(Timestamp.class, Duration.class), TimestampConversions::toDuration);
        addConversionDB(pair(String.class, Duration.class), StringConversions::toDuration);
        addConversionDB(pair(Map.class, Duration.class), MapConversions::toDuration);

        // Instant conversions supported
        addConversionDB(pair(Void.class, Instant.class), VoidConversions::toNull);
        addConversionDB(pair(Instant.class, Instant.class), Converter::identity);
        addConversionDB(pair(Long.class, Instant.class), NumberConversions::longNanosToInstant);
        addConversionDB(pair(Double.class, Instant.class), DoubleConversions::toInstant);
        addConversionDB(pair(BigInteger.class, Instant.class), BigIntegerConversions::toInstant);
        addConversionDB(pair(BigDecimal.class, Instant.class), BigDecimalConversions::toInstant);
        addConversionDB(pair(java.sql.Date.class, Instant.class), SqlDateConversions::toInstant);
        addConversionDB(pair(Timestamp.class, Instant.class), DateConversions::toInstant);
        addConversionDB(pair(Date.class, Instant.class), DateConversions::toInstant);
        addConversionDB(pair(LocalDate.class, Instant.class), LocalDateConversions::toInstant);
        addConversionDB(pair(LocalDateTime.class, Instant.class), LocalDateTimeConversions::toInstant);
        addConversionDB(pair(ZonedDateTime.class, Instant.class), ZonedDateTimeConversions::toInstant);
        addConversionDB(pair(OffsetDateTime.class, Instant.class), OffsetDateTimeConversions::toInstant);

        addConversionDB(pair(String.class, Instant.class), StringConversions::toInstant);
        addConversionDB(pair(Map.class, Instant.class), MapConversions::toInstant);

        // ZoneId conversions supported
        addConversionDB(pair(Void.class, ZoneId.class), VoidConversions::toNull);
        addConversionDB(pair(ZoneId.class, ZoneId.class), Converter::identity);
        addConversionDB(pair(String.class, ZoneId.class), StringConversions::toZoneId);
        addConversionDB(pair(Map.class, ZoneId.class), MapConversions::toZoneId);
        addConversionDB(pair(TimeZone.class, ZoneId.class), TimeZoneConversions::toZoneId);
        addConversionDB(pair(ZoneOffset.class, ZoneId.class), ZoneOffsetConversions::toZoneId);

        // ZoneOffset conversions supported
        addConversionDB(pair(Void.class, ZoneOffset.class), VoidConversions::toNull);
        addConversionDB(pair(ZoneOffset.class, ZoneOffset.class), Converter::identity);
        addConversionDB(pair(String.class, ZoneOffset.class), StringConversions::toZoneOffset);
        addConversionDB(pair(Map.class, ZoneOffset.class), MapConversions::toZoneOffset);
        addConversionDB(pair(ZoneId.class, ZoneOffset.class), ZoneIdConversions::toZoneOffset);
        addConversionDB(pair(TimeZone.class, ZoneOffset.class), TimeZoneConversions::toZoneOffset);

        // MonthDay conversions supported
        addConversionDB(pair(Void.class, MonthDay.class), VoidConversions::toNull);
        addConversionDB(pair(MonthDay.class, MonthDay.class), Converter::identity);
        addConversionDB(pair(java.sql.Date.class, MonthDay.class), SqlDateConversions::toMonthDay);
        addConversionDB(pair(Date.class, MonthDay.class), DateConversions::toMonthDay);
        addConversionDB(pair(Timestamp.class, MonthDay.class), TimestampConversions::toMonthDay);
        addConversionDB(pair(LocalDate.class, MonthDay.class), LocalDateConversions::toMonthDay);
        addConversionDB(pair(LocalDateTime.class, MonthDay.class), LocalDateTimeConversions::toMonthDay);
        addConversionDB(pair(ZonedDateTime.class, MonthDay.class), ZonedDateTimeConversions::toMonthDay);
        addConversionDB(pair(OffsetDateTime.class, MonthDay.class), OffsetDateTimeConversions::toMonthDay);
        addConversionDB(pair(String.class, MonthDay.class), StringConversions::toMonthDay);
        addConversionDB(pair(Map.class, MonthDay.class), MapConversions::toMonthDay);

        // YearMonth conversions supported
        addConversionDB(pair(Void.class, YearMonth.class), VoidConversions::toNull);
        addConversionDB(pair(YearMonth.class, YearMonth.class), Converter::identity);
        addConversionDB(pair(java.sql.Date.class, YearMonth.class), SqlDateConversions::toYearMonth);
        addConversionDB(pair(Date.class, YearMonth.class), DateConversions::toYearMonth);
        addConversionDB(pair(Timestamp.class, YearMonth.class), TimestampConversions::toYearMonth);
        addConversionDB(pair(LocalDate.class, YearMonth.class), LocalDateConversions::toYearMonth);
        addConversionDB(pair(LocalDateTime.class, YearMonth.class), LocalDateTimeConversions::toYearMonth);
        addConversionDB(pair(ZonedDateTime.class, YearMonth.class), ZonedDateTimeConversions::toYearMonth);
        addConversionDB(pair(OffsetDateTime.class, YearMonth.class), OffsetDateTimeConversions::toYearMonth);
        addConversionDB(pair(String.class, YearMonth.class), StringConversions::toYearMonth);
        addConversionDB(pair(Map.class, YearMonth.class), MapConversions::toYearMonth);

        // Period conversions supported
        addConversionDB(pair(Void.class, Period.class), VoidConversions::toNull);
        addConversionDB(pair(Period.class, Period.class), Converter::identity);
        addConversionDB(pair(String.class, Period.class), StringConversions::toPeriod);
        addConversionDB(pair(Map.class, Period.class), MapConversions::toPeriod);

        // toStringBuffer
        addConversionDB(pair(Void.class, StringBuffer.class), VoidConversions::toNull);
        addConversionDB(pair(String.class, StringBuffer.class), StringConversions::toStringBuffer);

        // toStringBuilder - Bridge through String
        addConversionDB(pair(Void.class, StringBuilder.class), VoidConversions::toNull);
        addConversionDB(pair(String.class, StringBuilder.class), StringConversions::toStringBuilder);

        // toByteArray
        addConversionDB(pair(Void.class, byte[].class), VoidConversions::toNull);
        addConversionDB(pair(String.class, byte[].class), StringConversions::toByteArray);
        addConversionDB(pair(ByteBuffer.class, byte[].class), ByteBufferConversions::toByteArray);
        addConversionDB(pair(CharBuffer.class, byte[].class), CharBufferConversions::toByteArray);
        addConversionDB(pair(char[].class, byte[].class), VoidConversions::toNull); // advertising convertion, implemented generically in ArrayConversions.
        addConversionDB(pair(byte[].class, byte[].class), Converter::identity);
        addConversionDB(pair(File.class, byte[].class), FileConversions::toByteArray);
        addConversionDB(pair(Path.class, byte[].class), PathConversions::toByteArray);

        // toCharArray
        addConversionDB(pair(Void.class, char[].class), VoidConversions::toNull);
        addConversionDB(pair(String.class, char[].class), StringConversions::toCharArray);
        addConversionDB(pair(ByteBuffer.class, char[].class), ByteBufferConversions::toCharArray);
        addConversionDB(pair(CharBuffer.class, char[].class), CharBufferConversions::toCharArray);
        addConversionDB(pair(char[].class, char[].class), CharArrayConversions::toCharArray);
        addConversionDB(pair(byte[].class, char[].class), VoidConversions::toNull);   // Used for advertising capability, implemented generically in ArrayConversions.
        addConversionDB(pair(File.class, char[].class), FileConversions::toCharArray);
        addConversionDB(pair(Path.class, char[].class), PathConversions::toCharArray);

        // toCharacterArray
        addConversionDB(pair(Void.class, Character[].class), VoidConversions::toNull);
        addConversionDB(pair(String.class, Character[].class), StringConversions::toCharacterArray);

        // toCharBuffer
        addConversionDB(pair(Void.class, CharBuffer.class), VoidConversions::toNull);
        addConversionDB(pair(String.class, CharBuffer.class), StringConversions::toCharBuffer);
        addConversionDB(pair(ByteBuffer.class, CharBuffer.class), ByteBufferConversions::toCharBuffer);
        addConversionDB(pair(CharBuffer.class, CharBuffer.class), CharBufferConversions::toCharBuffer);
        addConversionDB(pair(char[].class, CharBuffer.class), CharArrayConversions::toCharBuffer);
        addConversionDB(pair(byte[].class, CharBuffer.class), ByteArrayConversions::toCharBuffer);
        addConversionDB(pair(Map.class, CharBuffer.class), MapConversions::toCharBuffer);

        // toByteBuffer
        addConversionDB(pair(Void.class, ByteBuffer.class), VoidConversions::toNull);
        addConversionDB(pair(String.class, ByteBuffer.class), StringConversions::toByteBuffer);
        addConversionDB(pair(ByteBuffer.class, ByteBuffer.class), ByteBufferConversions::toByteBuffer);
        addConversionDB(pair(CharBuffer.class, ByteBuffer.class), CharBufferConversions::toByteBuffer);
        addConversionDB(pair(char[].class, ByteBuffer.class), CharArrayConversions::toByteBuffer);
        addConversionDB(pair(byte[].class, ByteBuffer.class), ByteArrayConversions::toByteBuffer);
        addConversionDB(pair(Map.class, ByteBuffer.class), MapConversions::toByteBuffer);

        // toYear
        addConversionDB(pair(Void.class, Year.class), VoidConversions::toNull);
        addConversionDB(pair(Year.class, Year.class), Converter::identity);
        addConversionDB(pair(Short.class, Year.class), NumberConversions::toYear);
        addConversionDB(pair(Integer.class, Year.class), NumberConversions::toYear);
        addConversionDB(pair(Long.class, Year.class), NumberConversions::toYear);
        addConversionDB(pair(Float.class, Year.class), NumberConversions::toYear);
        addConversionDB(pair(Double.class, Year.class), NumberConversions::toYear);
        addConversionDB(pair(BigInteger.class, Year.class), NumberConversions::toYear);
        addConversionDB(pair(BigDecimal.class, Year.class), NumberConversions::toYear);
        addConversionDB(pair(java.sql.Date.class, Year.class), SqlDateConversions::toYear);
        addConversionDB(pair(Date.class, Year.class), DateConversions::toYear);
        addConversionDB(pair(Timestamp.class, Year.class), TimestampConversions::toYear);
        addConversionDB(pair(LocalDate.class, Year.class), LocalDateConversions::toYear);
        addConversionDB(pair(LocalDateTime.class, Year.class), LocalDateTimeConversions::toYear);
        addConversionDB(pair(ZonedDateTime.class, Year.class), ZonedDateTimeConversions::toYear);
        addConversionDB(pair(OffsetDateTime.class, Year.class), OffsetDateTimeConversions::toYear);
        addConversionDB(pair(String.class, Year.class), StringConversions::toYear);
        addConversionDB(pair(Map.class, Year.class), MapConversions::toYear);

        // Throwable conversions supported
        addConversionDB(pair(Void.class, Throwable.class), VoidConversions::toNull);
        addConversionDB(pair(Map.class, Throwable.class), (ConvertWithTarget<Throwable>) MapConversions::toThrowable);

        // Map conversions supported
        addConversionDB(pair(Void.class, Map.class), VoidConversions::toNull);
        addConversionDB(pair(Byte.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(Short.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(Integer.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(Long.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(Float.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(Double.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(Boolean.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(Character.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(BigInteger.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(BigDecimal.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(AtomicBoolean.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(AtomicInteger.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(AtomicLong.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(Date.class, Map.class), DateConversions::toMap);
        addConversionDB(pair(java.sql.Date.class, Map.class), SqlDateConversions::toMap);
        addConversionDB(pair(Timestamp.class, Map.class), TimestampConversions::toMap);
        addConversionDB(pair(Calendar.class, Map.class), CalendarConversions::toMap);  // Restored - bridge produces different map key (zonedDateTime vs calendar)
        addConversionDB(pair(LocalDate.class, Map.class), LocalDateConversions::toMap);
        addConversionDB(pair(LocalDateTime.class, Map.class), LocalDateTimeConversions::toMap);
        addConversionDB(pair(ZonedDateTime.class, Map.class), ZonedDateTimeConversions::toMap);
        addConversionDB(pair(Duration.class, Map.class), DurationConversions::toMap);
        addConversionDB(pair(Instant.class, Map.class), InstantConversions::toMap);
        addConversionDB(pair(LocalTime.class, Map.class), LocalTimeConversions::toMap);
        addConversionDB(pair(MonthDay.class, Map.class), MonthDayConversions::toMap);
        addConversionDB(pair(YearMonth.class, Map.class), YearMonthConversions::toMap);
        addConversionDB(pair(Period.class, Map.class), PeriodConversions::toMap);
        addConversionDB(pair(TimeZone.class, Map.class), TimeZoneConversions::toMap);
        addConversionDB(pair(ZoneId.class, Map.class), ZoneIdConversions::toMap);
        addConversionDB(pair(ZoneOffset.class, Map.class), ZoneOffsetConversions::toMap);
        addConversionDB(pair(Class.class, Map.class), UniversalConversions::toMap);
        addConversionDB(pair(UUID.class, Map.class), UUIDConversions::toMap);
        addConversionDB(pair(Color.class, Map.class), ColorConversions::toMap);
        addConversionDB(pair(Dimension.class, Map.class), DimensionConversions::toMap);
        addConversionDB(pair(Point.class, Map.class), PointConversions::toMap);
        addConversionDB(pair(Rectangle.class, Map.class), RectangleConversions::toMap);
        addConversionDB(pair(Insets.class, Map.class), InsetsConversions::toMap);
        addConversionDB(pair(String.class, Map.class), StringConversions::toMap);
        addConversionDB(pair(Enum.class, Map.class), EnumConversions::toMap);
        addConversionDB(pair(OffsetDateTime.class, Map.class), OffsetDateTimeConversions::toMap);
        addConversionDB(pair(OffsetTime.class, Map.class), OffsetTimeConversions::toMap);
        addConversionDB(pair(Year.class, Map.class), YearConversions::toMap);
        addConversionDB(pair(Locale.class, Map.class), LocaleConversions::toMap);
        addConversionDB(pair(URI.class, Map.class), UriConversions::toMap);
        addConversionDB(pair(URL.class, Map.class), UrlConversions::toMap);
        addConversionDB(pair(Throwable.class, Map.class), ThrowableConversions::toMap);
        addConversionDB(pair(Pattern.class, Map.class), PatternConversions::toMap);
        addConversionDB(pair(Currency.class, Map.class), CurrencyConversions::toMap);
        addConversionDB(pair(ByteBuffer.class, Map.class), ByteBufferConversions::toMap);
        addConversionDB(pair(CharBuffer.class, Map.class), CharBufferConversions::toMap);
        addConversionDB(pair(File.class, Map.class), FileConversions::toMap);
        addConversionDB(pair(Path.class, Map.class), PathConversions::toMap);

        // toIntArray
        addConversionDB(pair(Color.class, int[].class), ColorConversions::toIntArray);
        addConversionDB(pair(Dimension.class, int[].class), DimensionConversions::toIntArray);
        addConversionDB(pair(Point.class, int[].class), PointConversions::toIntArray);
        addConversionDB(pair(Rectangle.class, int[].class), RectangleConversions::toIntArray);
        addConversionDB(pair(Insets.class, int[].class), InsetsConversions::toIntArray);

        // Array-like type bridges for universal array system access
        // ========================================
        // Atomic Array Bridges
        // ========================================

        // AtomicIntegerArray ↔ int[] bridges
        addConversionDB(pair(AtomicIntegerArray.class, int[].class), UniversalConversions::atomicIntegerArrayToIntArray);
        addConversionDB(pair(int[].class, AtomicIntegerArray.class), UniversalConversions::intArrayToAtomicIntegerArray);

        // AtomicLongArray ↔ long[] bridges  
        addConversionDB(pair(AtomicLongArray.class, long[].class), UniversalConversions::atomicLongArrayToLongArray);
        addConversionDB(pair(long[].class, AtomicLongArray.class), UniversalConversions::longArrayToAtomicLongArray);

        // AtomicReferenceArray ↔ Object[] bridges
        addConversionDB(pair(AtomicReferenceArray.class, Object[].class), UniversalConversions::atomicReferenceArrayToObjectArray);
        addConversionDB(pair(Object[].class, AtomicReferenceArray.class), UniversalConversions::objectArrayToAtomicReferenceArray);

        // AtomicReferenceArray ↔ String[] bridges
        addConversionDB(pair(AtomicReferenceArray.class, String[].class), UniversalConversions::atomicReferenceArrayToStringArray);
        addConversionDB(pair(String[].class, AtomicReferenceArray.class), UniversalConversions::stringArrayToAtomicReferenceArray);

        // ========================================
        // NIO Buffer Bridges
        // ========================================

        // IntBuffer ↔ int[] bridges
        addConversionDB(pair(IntBuffer.class, int[].class), UniversalConversions::intBufferToIntArray);
        addConversionDB(pair(int[].class, IntBuffer.class), UniversalConversions::intArrayToIntBuffer);

        // LongBuffer ↔ long[] bridges
        addConversionDB(pair(LongBuffer.class, long[].class), UniversalConversions::longBufferToLongArray);
        addConversionDB(pair(long[].class, LongBuffer.class), UniversalConversions::longArrayToLongBuffer);

        // FloatBuffer ↔ float[] bridges
        addConversionDB(pair(FloatBuffer.class, float[].class), UniversalConversions::floatBufferToFloatArray);
        addConversionDB(pair(float[].class, FloatBuffer.class), UniversalConversions::floatArrayToFloatBuffer);

        // DoubleBuffer ↔ double[] bridges
        addConversionDB(pair(DoubleBuffer.class, double[].class), UniversalConversions::doubleBufferToDoubleArray);
        addConversionDB(pair(double[].class, DoubleBuffer.class), UniversalConversions::doubleArrayToDoubleBuffer);

        // ShortBuffer ↔ short[] bridges
        addConversionDB(pair(ShortBuffer.class, short[].class), UniversalConversions::shortBufferToShortArray);
        addConversionDB(pair(short[].class, ShortBuffer.class), UniversalConversions::shortArrayToShortBuffer);

        // ========================================
        // BitSet Bridges
        // ========================================

        // BitSet ↔ boolean[] bridges
        addConversionDB(pair(BitSet.class, boolean[].class), UniversalConversions::bitSetToBooleanArray);
        addConversionDB(pair(boolean[].class, BitSet.class), UniversalConversions::booleanArrayToBitSet);

        // BitSet ↔ int[] bridges (set bit indices)
        addConversionDB(pair(BitSet.class, int[].class), UniversalConversions::bitSetToIntArray);
        addConversionDB(pair(int[].class, BitSet.class), UniversalConversions::intArrayToBitSet);

        // BitSet ↔ byte[] bridges
        addConversionDB(pair(BitSet.class, byte[].class), UniversalConversions::bitSetToByteArray);
        addConversionDB(pair(byte[].class, BitSet.class), UniversalConversions::byteArrayToBitSet);

        // ========================================
        // Stream Bridges
        // ========================================

        // IntStream ↔ int[] bridges
        addConversionDB(pair(IntStream.class, int[].class), UniversalConversions::intStreamToIntArray);
        addConversionDB(pair(int[].class, IntStream.class), UniversalConversions::intArrayToIntStream);

        // LongStream ↔ long[] bridges
        addConversionDB(pair(LongStream.class, long[].class), UniversalConversions::longStreamToLongArray);
        addConversionDB(pair(long[].class, LongStream.class), UniversalConversions::longArrayToLongStream);

        // DoubleStream ↔ double[] bridges
        addConversionDB(pair(DoubleStream.class, double[].class), UniversalConversions::doubleStreamToDoubleArray);
        addConversionDB(pair(double[].class, DoubleStream.class), UniversalConversions::doubleArrayToDoubleStream);

        // Register Record.class -> Map.class conversion if Records are supported
        try {
            Class<?> recordClass = Class.forName("java.lang.Record");
            addConversionDB(pair(recordClass, Map.class), MapConversions::recordToMap);
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
                    existingPairs.add(pair((Class<?>) source, (Class<?>) target, (Long) entry.keys[2]));
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
                        ConversionPair surrogateConversionPair = pair(surrogateClass, targetClass);

                        // Only add if not already defined and not converting to itself
                        if (getFromDB(CONVERSION_DB, surrogateConversionPair) == null && !targetClass.equals(surrogateClass)) {
                            // Create composite conversion: Surrogate → primary → target
                            Convert<?> originalConversion = getFromDB(CONVERSION_DB, pair);
                            Convert<?> bridgeConversion = createSurrogateToPrimaryBridgeConversion(config, originalConversion);
                            putToDB(CONVERSION_DB, surrogateConversionPair, bridgeConversion);
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
                        ConversionPair sourceToSurrogateConversionPair = pair(sourceClass, surrogateClass);

                        // Only add if not already defined and not converting from itself
                        if (getFromDB(CONVERSION_DB, sourceToSurrogateConversionPair) == null && !sourceClass.equals(surrogateClass)) {
                            // Create composite conversion: Source → primary → surrogate
                            Convert<?> originalConversion = getFromDB(CONVERSION_DB, pair);
                            Convert<?> bridgeConversion = createPrimaryToSurrogateBridgeConversion(config, originalConversion);
                            putToDB(CONVERSION_DB, sourceToSurrogateConversionPair, bridgeConversion);
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
            putToDB(USER_DB, entry.getKey(), entry.getValue());
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
            Convert<?> tempConverter = getFromDB(USER_DB, pairWithLevel.pair);
            if (tempConverter != null) {
                return tempConverter;
            }
            tempConverter = getFromDB(CONVERSION_DB, pairWithLevel.pair);
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
        Convert<?> previous = putToDB(USER_DB, wrapperSource, wrapperTarget, 0L, conversionMethod);

        // Add all type combinations to USER_DB
        for (Class<?> srcType : sourceTypes) {
            for (Class<?> tgtType : targetTypes) {
                putToDB(USER_DB, srcType, tgtType, 0L, conversionMethod);
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
                CONVERSION_DB.put(srcType, tgtType, 0, conversionMethod);
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
