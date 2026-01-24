package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.geom.Color;
import com.cedarsoftware.util.geom.Dimension;
import com.cedarsoftware.util.geom.Insets;
import com.cedarsoftware.util.geom.Point;
import com.cedarsoftware.util.geom.Rectangle;
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
// HashSet import removed - using IdentitySet for Class objects
import com.cedarsoftware.util.IdentitySet;
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
import java.util.concurrent.ConcurrentHashMap;
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
 * <strong>JDK Module Requirements:</strong>
 * <ul>
 *     <li><b>java.sql</b> (REQUIRED): Core date/time conversions use {@code java.sql.Timestamp} and {@code java.sql.Date}.
 *         This adds ~500KB to your runtime footprint but does NOT require database connectivity or JDBC drivers.
 *         For JPMS modules, this dependency is automatically transitive when you {@code requires com.cedarsoftware.util}.</li>
 *     <li><b>java.compiler</b> (OPTIONAL): Only needed for {@code CompactMap} runtime code generation.
 *         Most users don't need this. Available at compile time in JDK, may not be present in JRE-only environments.</li>
 *     <li><b>java.xml</b> (OPTIONAL): Only needed for XML-specific methods in {@code IOUtilities}.
 *         Marked as a static (optional) dependency in the JPMS module descriptor.</li>
 * </ul>
 * See the README for more details on module requirements and deployment considerations.
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
    private static final Map<Class<?>, SortedSet<ClassLevel>> cacheCompleteHierarchy = new ClassValueMap<>();
    private static final MultiKeyMap<InheritancePair[]> cacheInheritancePairs = MultiKeyMap.<InheritancePair[]>builder()
            .flattenDimensions(true)
            .collectionKeyMode(MultiKeyMap.CollectionKeyMode.COLLECTIONS_NOT_EXPANDED)
            .build();
    private static final Map<ConversionPair, Convert<?>> CONVERSION_DB = new ConcurrentHashMap<>(4096, 0.8f);
    private final Map<ConversionPair, Convert<?>> USER_DB = new ConcurrentHashMap<>(16, 0.8f);
    private static final Map<ConversionPair, Convert<?>> FULL_CONVERSION_CACHE = new ConcurrentHashMap<>(1024, 0.75f);
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
        CONVERSION_DB.put(pair(Duration.class, Number.class), DurationConversions::toNumber);

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
        CONVERSION_DB.put(pair(BigInteger.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(BigDecimal.class, Short.class), NumberConversions::toShort);
        CONVERSION_DB.put(pair(Map.class, Short.class), MapConversions::toShort);
        CONVERSION_DB.put(pair(String.class, Short.class), StringConversions::toShort);
        CONVERSION_DB.put(pair(Year.class, Short.class), YearConversions::toShort);

        // toInteger
        CONVERSION_DB.put(pair(Void.class, int.class), NumberConversions::toIntZero);
        CONVERSION_DB.put(pair(AtomicInteger.class, int.class), UniversalConversions::atomicIntegerToInt);
        CONVERSION_DB.put(pair(Void.class, Integer.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Short.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Integer.class, Integer.class), Converter::identity);
        CONVERSION_DB.put(pair(Long.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Float.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Double.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Boolean.class, Integer.class), BooleanConversions::toInt);
        CONVERSION_DB.put(pair(Character.class, Integer.class), CharacterConversions::toInt);
        CONVERSION_DB.put(pair(AtomicInteger.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(BigInteger.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(BigDecimal.class, Integer.class), NumberConversions::toInt);
        CONVERSION_DB.put(pair(Map.class, Integer.class), MapConversions::toInt);
        CONVERSION_DB.put(pair(String.class, Integer.class), StringConversions::toInt);
        CONVERSION_DB.put(pair(Color.class, Integer.class), ColorConversions::toInteger);
        CONVERSION_DB.put(pair(Year.class, Integer.class), YearConversions::toInt);

        // toLong
        CONVERSION_DB.put(pair(Void.class, long.class), NumberConversions::toLongZero);
        CONVERSION_DB.put(pair(AtomicLong.class, long.class), UniversalConversions::atomicLongToLong);
        CONVERSION_DB.put(pair(Void.class, Long.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Short.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Integer.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Long.class, Long.class), Converter::identity);
        CONVERSION_DB.put(pair(Float.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Double.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Boolean.class, Long.class), BooleanConversions::toLong);
        CONVERSION_DB.put(pair(Character.class, Long.class), CharacterConversions::toLong);
        CONVERSION_DB.put(pair(BigInteger.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(BigDecimal.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(AtomicLong.class, Long.class), NumberConversions::toLong);
        CONVERSION_DB.put(pair(Date.class, Long.class), DateConversions::toLong);
        CONVERSION_DB.put(pair(java.sql.Date.class, Long.class), SqlDateConversions::toLong);
        CONVERSION_DB.put(pair(Timestamp.class, Long.class), TimestampConversions::toLong);
        CONVERSION_DB.put(pair(Instant.class, Long.class), InstantConversions::toLong);
        CONVERSION_DB.put(pair(Duration.class, Long.class), DurationConversions::toLong);
        CONVERSION_DB.put(pair(LocalDate.class, Long.class), LocalDateConversions::toLong);
        CONVERSION_DB.put(pair(LocalTime.class, Long.class), LocalTimeConversions::toLong);
        CONVERSION_DB.put(pair(LocalDateTime.class, Long.class), LocalDateTimeConversions::toLong);
        CONVERSION_DB.put(pair(OffsetTime.class, Long.class), OffsetTimeConversions::toLong);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Long.class), OffsetDateTimeConversions::toLong);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Long.class), ZonedDateTimeConversions::toLong);
        CONVERSION_DB.put(pair(Map.class, Long.class), MapConversions::toLong);
        CONVERSION_DB.put(pair(String.class, Long.class), StringConversions::toLong);
        CONVERSION_DB.put(pair(Color.class, Long.class), ColorConversions::toLong);
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
        CONVERSION_DB.put(pair(BigInteger.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(BigDecimal.class, Float.class), NumberConversions::toFloat);
        CONVERSION_DB.put(pair(Map.class, Float.class), MapConversions::toFloat);
        CONVERSION_DB.put(pair(String.class, Float.class), StringConversions::toFloat);
        CONVERSION_DB.put(pair(Year.class, Float.class), YearConversions::toFloat);

        // toDouble
        CONVERSION_DB.put(pair(Void.class, double.class), NumberConversions::toDoubleZero);
        CONVERSION_DB.put(pair(Year.class, double.class), YearConversions::toDouble);
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
        CONVERSION_DB.put(pair(OffsetTime.class, Double.class), OffsetTimeConversions::toDouble);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Double.class), OffsetDateTimeConversions::toDouble);
        CONVERSION_DB.put(pair(Date.class, Double.class), DateConversions::toDouble);
        CONVERSION_DB.put(pair(java.sql.Date.class, Double.class), SqlDateConversions::toDouble);
        CONVERSION_DB.put(pair(Timestamp.class, Double.class), TimestampConversions::toDouble);
        CONVERSION_DB.put(pair(BigInteger.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(BigDecimal.class, Double.class), NumberConversions::toDouble);
        CONVERSION_DB.put(pair(Map.class, Double.class), MapConversions::toDouble);
        CONVERSION_DB.put(pair(String.class, Double.class), StringConversions::toDouble);
        CONVERSION_DB.put(pair(Year.class, Double.class), YearConversions::toDouble);

        // Boolean/boolean conversions supported
        CONVERSION_DB.put(pair(Void.class, boolean.class), VoidConversions::toBoolean);
        CONVERSION_DB.put(pair(AtomicBoolean.class, boolean.class), UniversalConversions::atomicBooleanToBoolean);
        CONVERSION_DB.put(pair(Duration.class, boolean.class), DurationConversions::toBoolean);
        CONVERSION_DB.put(pair(AtomicBoolean.class, Boolean.class), AtomicBooleanConversions::toBoolean);
        CONVERSION_DB.put(pair(Duration.class, Boolean.class), DurationConversions::toBooleanWrapper);
        CONVERSION_DB.put(pair(Void.class, Boolean.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(pair(Short.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(pair(Integer.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(pair(Long.class, Boolean.class), NumberConversions::isIntTypeNotZero);
        CONVERSION_DB.put(pair(Float.class, Boolean.class), NumberConversions::isFloatTypeNotZero);
        CONVERSION_DB.put(pair(Double.class, Boolean.class), NumberConversions::isFloatTypeNotZero);
        CONVERSION_DB.put(pair(Boolean.class, Boolean.class), Converter::identity);
        CONVERSION_DB.put(pair(Character.class, Boolean.class), CharacterConversions::toBoolean);
        CONVERSION_DB.put(pair(BigInteger.class, Boolean.class), NumberConversions::isBigIntegerNotZero);
        CONVERSION_DB.put(pair(BigDecimal.class, Boolean.class), NumberConversions::isBigDecimalNotZero);
        CONVERSION_DB.put(pair(Map.class, Boolean.class), MapConversions::toBoolean);
        CONVERSION_DB.put(pair(String.class, Boolean.class), StringConversions::toBoolean);
        CONVERSION_DB.put(pair(Dimension.class, Boolean.class), DimensionConversions::toBoolean);
        CONVERSION_DB.put(pair(Point.class, Boolean.class), PointConversions::toBoolean);
        CONVERSION_DB.put(pair(Rectangle.class, Boolean.class), RectangleConversions::toBoolean);
        CONVERSION_DB.put(pair(Insets.class, Boolean.class), InsetsConversions::toBoolean);
        CONVERSION_DB.put(pair(UUID.class, Boolean.class), UUIDConversions::toBoolean);

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
        CONVERSION_DB.put(pair(BigInteger.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(BigDecimal.class, Character.class), NumberConversions::toCharacter);
        CONVERSION_DB.put(pair(Map.class, Character.class), MapConversions::toCharacter);
        CONVERSION_DB.put(pair(String.class, Character.class), StringConversions::toCharacter);

        // BigInteger versions supported
        CONVERSION_DB.put(pair(Void.class, BigInteger.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, BigInteger.class), NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(Short.class, BigInteger.class), NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(Integer.class, BigInteger.class), NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(Long.class, BigInteger.class), NumberConversions::integerTypeToBigInteger);
        CONVERSION_DB.put(pair(Float.class, BigInteger.class), NumberConversions::floatingPointToBigInteger);
        CONVERSION_DB.put(pair(Double.class, BigInteger.class), NumberConversions::floatingPointToBigInteger);
        CONVERSION_DB.put(pair(Boolean.class, BigInteger.class), BooleanConversions::toBigInteger);
        CONVERSION_DB.put(pair(Character.class, BigInteger.class), CharacterConversions::toBigInteger);
        CONVERSION_DB.put(pair(BigInteger.class, BigInteger.class), Converter::identity);
        CONVERSION_DB.put(pair(BigDecimal.class, BigInteger.class), BigDecimalConversions::toBigInteger);
        CONVERSION_DB.put(pair(Date.class, BigInteger.class), DateConversions::toBigInteger);
        CONVERSION_DB.put(pair(java.sql.Date.class, BigInteger.class), SqlDateConversions::toBigInteger);
        CONVERSION_DB.put(pair(Timestamp.class, BigInteger.class), TimestampConversions::toBigInteger);
        CONVERSION_DB.put(pair(Duration.class, BigInteger.class), DurationConversions::toBigInteger);
        CONVERSION_DB.put(pair(Instant.class, BigInteger.class), InstantConversions::toBigInteger);
        CONVERSION_DB.put(pair(LocalTime.class, BigInteger.class), LocalTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(LocalDate.class, BigInteger.class), LocalDateConversions::toBigInteger);
        CONVERSION_DB.put(pair(LocalDateTime.class, BigInteger.class), LocalDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(ZonedDateTime.class, BigInteger.class), ZonedDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(OffsetTime.class, BigInteger.class), OffsetTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(OffsetDateTime.class, BigInteger.class), OffsetDateTimeConversions::toBigInteger);
        CONVERSION_DB.put(pair(UUID.class, BigInteger.class), UUIDConversions::toBigInteger);
        CONVERSION_DB.put(pair(Calendar.class, BigInteger.class), CalendarConversions::toBigInteger);  // Restored - bridge has precision difference (millis vs. nanos)
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
        CONVERSION_DB.put(pair(Date.class, BigDecimal.class), DateConversions::toBigDecimal);
        CONVERSION_DB.put(pair(java.sql.Date.class, BigDecimal.class), SqlDateConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Timestamp.class, BigDecimal.class), TimestampConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Instant.class, BigDecimal.class), InstantConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Duration.class, BigDecimal.class), DurationConversions::toBigDecimal);
        CONVERSION_DB.put(pair(LocalTime.class, BigDecimal.class), LocalTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(LocalDate.class, BigDecimal.class), LocalDateConversions::toBigDecimal);
        CONVERSION_DB.put(pair(LocalDateTime.class, BigDecimal.class), LocalDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(ZonedDateTime.class, BigDecimal.class), ZonedDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(OffsetTime.class, BigDecimal.class), OffsetTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(OffsetDateTime.class, BigDecimal.class), OffsetDateTimeConversions::toBigDecimal);
        CONVERSION_DB.put(pair(UUID.class, BigDecimal.class), UUIDConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Color.class, BigDecimal.class), ColorConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Calendar.class, BigDecimal.class), CalendarConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Map.class, BigDecimal.class), MapConversions::toBigDecimal);
        CONVERSION_DB.put(pair(String.class, BigDecimal.class), StringConversions::toBigDecimal);
        CONVERSION_DB.put(pair(Year.class, BigDecimal.class), YearConversions::toBigDecimal);

        // AtomicBoolean conversions supported
        // Most X → AtomicBoolean handled by surrogate system via X → Boolean → AtomicBoolean
        CONVERSION_DB.put(pair(Void.class, AtomicBoolean.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Boolean.class, AtomicBoolean.class), BooleanConversions::toAtomicBoolean);  // Bridge
        CONVERSION_DB.put(pair(AtomicBoolean.class, AtomicBoolean.class), AtomicBooleanConversions::toAtomicBoolean);
        CONVERSION_DB.put(pair(Year.class, AtomicBoolean.class), YearConversions::toAtomicBoolean);  // No Year → Boolean
        CONVERSION_DB.put(pair(Map.class, AtomicBoolean.class), MapConversions::toAtomicBoolean);  // Better error messages

        // AtomicInteger conversions supported
        // Most X → AtomicInteger handled by surrogate system via X → Integer → AtomicInteger
        CONVERSION_DB.put(pair(Void.class, AtomicInteger.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Integer.class, AtomicInteger.class), NumberConversions::toAtomicInteger);  // Bridge
        CONVERSION_DB.put(pair(AtomicInteger.class, AtomicInteger.class), AtomicIntegerConversions::toAtomicInteger);
        CONVERSION_DB.put(pair(Map.class, AtomicInteger.class), MapConversions::toAtomicInteger);  // Better error messages

        // AtomicLong conversions supported
        // Most X → AtomicLong handled by surrogate system via X → Long → AtomicLong
        CONVERSION_DB.put(pair(Void.class, AtomicLong.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, AtomicLong.class), NumberConversions::toAtomicLong);  // Bridge
        CONVERSION_DB.put(pair(AtomicLong.class, AtomicLong.class), AtomicLongConversions::toAtomicLong);
        CONVERSION_DB.put(pair(Map.class, AtomicLong.class), MapConversions::toAtomicLong);  // Better error messages

        // Date conversions supported
        CONVERSION_DB.put(pair(Void.class, Date.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, Date.class), NumberConversions::toDate);
        CONVERSION_DB.put(pair(Double.class, Date.class), DoubleConversions::toDate);
        CONVERSION_DB.put(pair(BigInteger.class, Date.class), BigIntegerConversions::toDate);
        CONVERSION_DB.put(pair(BigDecimal.class, Date.class), BigDecimalConversions::toDate);
        CONVERSION_DB.put(pair(Date.class, Date.class), DateConversions::toDate);
        CONVERSION_DB.put(pair(java.sql.Date.class, Date.class), SqlDateConversions::toDate);
        CONVERSION_DB.put(pair(Timestamp.class, Date.class), TimestampConversions::toDate);
        CONVERSION_DB.put(pair(Instant.class, Date.class), InstantConversions::toDate);
        CONVERSION_DB.put(pair(LocalDate.class, Date.class), LocalDateConversions::toDate);
        CONVERSION_DB.put(pair(LocalDateTime.class, Date.class), LocalDateTimeConversions::toDate);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Date.class), ZonedDateTimeConversions::toDate);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Date.class), OffsetDateTimeConversions::toDate);
        CONVERSION_DB.put(pair(Duration.class, Date.class), DurationConversions::toDate);
        CONVERSION_DB.put(pair(Map.class, Date.class), MapConversions::toDate);
        CONVERSION_DB.put(pair(String.class, Date.class), StringConversions::toDate);

        // java.sql.Date conversion supported
        CONVERSION_DB.put(pair(Void.class, java.sql.Date.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, java.sql.Date.class), NumberConversions::toSqlDate);
        CONVERSION_DB.put(pair(Double.class, java.sql.Date.class), DoubleConversions::toSqlDate);
        CONVERSION_DB.put(pair(BigInteger.class, java.sql.Date.class), BigIntegerConversions::toSqlDate);
        CONVERSION_DB.put(pair(BigDecimal.class, java.sql.Date.class), BigDecimalConversions::toSqlDate);
        CONVERSION_DB.put(pair(java.sql.Date.class, java.sql.Date.class), SqlDateConversions::toSqlDate);
        CONVERSION_DB.put(pair(Date.class, java.sql.Date.class), DateConversions::toSqlDate);
        CONVERSION_DB.put(pair(Timestamp.class, java.sql.Date.class), TimestampConversions::toSqlDate);
        CONVERSION_DB.put(pair(Duration.class, java.sql.Date.class), DurationConversions::toSqlDate);
        CONVERSION_DB.put(pair(Instant.class, java.sql.Date.class), InstantConversions::toSqlDate);
        CONVERSION_DB.put(pair(LocalDate.class, java.sql.Date.class), LocalDateConversions::toSqlDate);
        CONVERSION_DB.put(pair(LocalDateTime.class, java.sql.Date.class), LocalDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(pair(ZonedDateTime.class, java.sql.Date.class), ZonedDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(pair(OffsetDateTime.class, java.sql.Date.class), OffsetDateTimeConversions::toSqlDate);
        CONVERSION_DB.put(pair(Map.class, java.sql.Date.class), MapConversions::toSqlDate);
        CONVERSION_DB.put(pair(String.class, java.sql.Date.class), StringConversions::toSqlDate);

        // Timestamp conversions supported
        CONVERSION_DB.put(pair(Void.class, Timestamp.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, Timestamp.class), NumberConversions::toTimestamp);
        CONVERSION_DB.put(pair(Double.class, Timestamp.class), DoubleConversions::toTimestamp);
        CONVERSION_DB.put(pair(BigInteger.class, Timestamp.class), BigIntegerConversions::toTimestamp);
        CONVERSION_DB.put(pair(BigDecimal.class, Timestamp.class), BigDecimalConversions::toTimestamp);
        CONVERSION_DB.put(pair(Timestamp.class, Timestamp.class), DateConversions::toTimestamp);
        CONVERSION_DB.put(pair(java.sql.Date.class, Timestamp.class), SqlDateConversions::toTimestamp);
        CONVERSION_DB.put(pair(Date.class, Timestamp.class), DateConversions::toTimestamp);
        CONVERSION_DB.put(pair(Duration.class, Timestamp.class), DurationConversions::toTimestamp);
        CONVERSION_DB.put(pair(Instant.class, Timestamp.class), InstantConversions::toTimestamp);
        CONVERSION_DB.put(pair(LocalDate.class, Timestamp.class), LocalDateConversions::toTimestamp);
        CONVERSION_DB.put(pair(LocalDateTime.class, Timestamp.class), LocalDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Timestamp.class), ZonedDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Timestamp.class), OffsetDateTimeConversions::toTimestamp);
        CONVERSION_DB.put(pair(Map.class, Timestamp.class), MapConversions::toTimestamp);
        CONVERSION_DB.put(pair(String.class, Timestamp.class), StringConversions::toTimestamp);

        // Calendar conversions supported
        CONVERSION_DB.put(pair(Void.class, Calendar.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, Calendar.class), NumberConversions::toCalendar);
        CONVERSION_DB.put(pair(Double.class, Calendar.class), DoubleConversions::toCalendar);
        CONVERSION_DB.put(pair(BigInteger.class, Calendar.class), BigIntegerConversions::toCalendar);
        CONVERSION_DB.put(pair(BigDecimal.class, Calendar.class), BigDecimalConversions::toCalendar);
        CONVERSION_DB.put(pair(Date.class, Calendar.class), DateConversions::toCalendar);
        CONVERSION_DB.put(pair(java.sql.Date.class, Calendar.class), SqlDateConversions::toCalendar);
        CONVERSION_DB.put(pair(Timestamp.class, Calendar.class), TimestampConversions::toCalendar);
        CONVERSION_DB.put(pair(Instant.class, Calendar.class), InstantConversions::toCalendar);
        CONVERSION_DB.put(pair(LocalTime.class, Calendar.class), LocalTimeConversions::toCalendar);
        CONVERSION_DB.put(pair(LocalDate.class, Calendar.class), LocalDateConversions::toCalendar);
        CONVERSION_DB.put(pair(LocalDateTime.class, Calendar.class), LocalDateTimeConversions::toCalendar);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Calendar.class), ZonedDateTimeConversions::toCalendar);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Calendar.class), OffsetDateTimeConversions::toCalendar);
        CONVERSION_DB.put(pair(Duration.class, Calendar.class), DurationConversions::toCalendar);
        CONVERSION_DB.put(pair(Calendar.class, Calendar.class), CalendarConversions::clone);
        CONVERSION_DB.put(pair(Map.class, Calendar.class), MapConversions::toCalendar);
        CONVERSION_DB.put(pair(String.class, Calendar.class), StringConversions::toCalendar);

        // LocalDate conversions supported
        CONVERSION_DB.put(pair(Void.class, LocalDate.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, LocalDate.class), NumberConversions::toLocalDate);
        CONVERSION_DB.put(pair(Double.class, LocalDate.class), DoubleConversions::toLocalDate);
        CONVERSION_DB.put(pair(BigInteger.class, LocalDate.class), BigIntegerConversions::toLocalDate);
        CONVERSION_DB.put(pair(BigDecimal.class, LocalDate.class), BigDecimalConversions::toLocalDate);
        CONVERSION_DB.put(pair(java.sql.Date.class, LocalDate.class), SqlDateConversions::toLocalDate);
        CONVERSION_DB.put(pair(Timestamp.class, LocalDate.class), DateConversions::toLocalDate);
        CONVERSION_DB.put(pair(Date.class, LocalDate.class), DateConversions::toLocalDate);
        CONVERSION_DB.put(pair(Instant.class, LocalDate.class), InstantConversions::toLocalDate);
        CONVERSION_DB.put(pair(Calendar.class, LocalDate.class), CalendarConversions::toLocalDate);
        CONVERSION_DB.put(pair(LocalDate.class, LocalDate.class), Converter::identity);
        CONVERSION_DB.put(pair(LocalDateTime.class, LocalDate.class), LocalDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(pair(ZonedDateTime.class, LocalDate.class), ZonedDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(pair(OffsetDateTime.class, LocalDate.class), OffsetDateTimeConversions::toLocalDate);
        CONVERSION_DB.put(pair(Duration.class, LocalDate.class), DurationConversions::toLocalDate);
        CONVERSION_DB.put(pair(Map.class, LocalDate.class), MapConversions::toLocalDate);
        CONVERSION_DB.put(pair(String.class, LocalDate.class), StringConversions::toLocalDate);

        // LocalDateTime conversions supported
        CONVERSION_DB.put(pair(Void.class, LocalDateTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, LocalDateTime.class), NumberConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Double.class, LocalDateTime.class), DoubleConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(BigInteger.class, LocalDateTime.class), BigIntegerConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(BigDecimal.class, LocalDateTime.class), BigDecimalConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(java.sql.Date.class, LocalDateTime.class), SqlDateConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Timestamp.class, LocalDateTime.class), TimestampConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Date.class, LocalDateTime.class), DateConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Instant.class, LocalDateTime.class), InstantConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(LocalDateTime.class, LocalDateTime.class), LocalDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(LocalDate.class, LocalDateTime.class), LocalDateConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Calendar.class, LocalDateTime.class), CalendarConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(ZonedDateTime.class, LocalDateTime.class), ZonedDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(OffsetDateTime.class, LocalDateTime.class), OffsetDateTimeConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Duration.class, LocalDateTime.class), DurationConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(Map.class, LocalDateTime.class), MapConversions::toLocalDateTime);
        CONVERSION_DB.put(pair(String.class, LocalDateTime.class), StringConversions::toLocalDateTime);

        // LocalTime conversions supported
        CONVERSION_DB.put(pair(Void.class, LocalTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, LocalTime.class), NumberConversions::longNanosToLocalTime);
        CONVERSION_DB.put(pair(Double.class, LocalTime.class), DoubleConversions::toLocalTime);
        CONVERSION_DB.put(pair(BigInteger.class, LocalTime.class), BigIntegerConversions::toLocalTime);
        CONVERSION_DB.put(pair(BigDecimal.class, LocalTime.class), BigDecimalConversions::toLocalTime);
        CONVERSION_DB.put(pair(Timestamp.class, LocalTime.class), DateConversions::toLocalTime);
        CONVERSION_DB.put(pair(Date.class, LocalTime.class), DateConversions::toLocalTime);
        CONVERSION_DB.put(pair(Instant.class, LocalTime.class), InstantConversions::toLocalTime);
        CONVERSION_DB.put(pair(LocalDateTime.class, LocalTime.class), LocalDateTimeConversions::toLocalTime);
        CONVERSION_DB.put(pair(LocalTime.class, LocalTime.class), Converter::identity);
        CONVERSION_DB.put(pair(ZonedDateTime.class, LocalTime.class), ZonedDateTimeConversions::toLocalTime);
        CONVERSION_DB.put(pair(OffsetDateTime.class, LocalTime.class), OffsetDateTimeConversions::toLocalTime);
        CONVERSION_DB.put(pair(Duration.class, LocalTime.class), DurationConversions::toLocalTime);
        CONVERSION_DB.put(pair(Map.class, LocalTime.class), MapConversions::toLocalTime);
        CONVERSION_DB.put(pair(String.class, LocalTime.class), StringConversions::toLocalTime);

        // ZonedDateTime conversions supported
        CONVERSION_DB.put(pair(Void.class, ZonedDateTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, ZonedDateTime.class), NumberConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Double.class, ZonedDateTime.class), DoubleConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(BigInteger.class, ZonedDateTime.class), BigIntegerConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(BigDecimal.class, ZonedDateTime.class), BigDecimalConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(java.sql.Date.class, ZonedDateTime.class), SqlDateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Timestamp.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Date.class, ZonedDateTime.class), DateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Instant.class, ZonedDateTime.class), InstantConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(LocalDate.class, ZonedDateTime.class), LocalDateConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(LocalDateTime.class, ZonedDateTime.class), LocalDateTimeConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(ZonedDateTime.class, ZonedDateTime.class), Converter::identity);
        CONVERSION_DB.put(pair(OffsetDateTime.class, ZonedDateTime.class), OffsetDateTimeConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Calendar.class, ZonedDateTime.class), CalendarConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Duration.class, ZonedDateTime.class), DurationConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(Map.class, ZonedDateTime.class), MapConversions::toZonedDateTime);
        CONVERSION_DB.put(pair(String.class, ZonedDateTime.class), StringConversions::toZonedDateTime);

        // toOffsetDateTime
        CONVERSION_DB.put(pair(Void.class, OffsetDateTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(OffsetDateTime.class, OffsetDateTime.class), Converter::identity);
        CONVERSION_DB.put(pair(Map.class, OffsetDateTime.class), MapConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(String.class, OffsetDateTime.class), StringConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Long.class, OffsetDateTime.class), NumberConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Double.class, OffsetDateTime.class), DoubleConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(BigInteger.class, OffsetDateTime.class), BigIntegerConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(BigDecimal.class, OffsetDateTime.class), BigDecimalConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(java.sql.Date.class, OffsetDateTime.class), SqlDateConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Date.class, OffsetDateTime.class), DateConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Timestamp.class, OffsetDateTime.class), TimestampConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(LocalDate.class, OffsetDateTime.class), LocalDateConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Instant.class, OffsetDateTime.class), InstantConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(ZonedDateTime.class, OffsetDateTime.class), ZonedDateTimeConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(LocalDateTime.class, OffsetDateTime.class), LocalDateTimeConversions::toOffsetDateTime);
        CONVERSION_DB.put(pair(Duration.class, OffsetDateTime.class), DurationConversions::toOffsetDateTime);

        // toOffsetTime
        CONVERSION_DB.put(pair(Void.class, OffsetTime.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Long.class, OffsetTime.class), NumberConversions::toOffsetTime);
        CONVERSION_DB.put(pair(Double.class, OffsetTime.class), DoubleConversions::toOffsetTime);
        CONVERSION_DB.put(pair(BigInteger.class, OffsetTime.class), BigIntegerConversions::toOffsetTime);
        CONVERSION_DB.put(pair(BigDecimal.class, OffsetTime.class), BigDecimalConversions::toOffsetTime);
        CONVERSION_DB.put(pair(OffsetTime.class, OffsetTime.class), Converter::identity);
        CONVERSION_DB.put(pair(OffsetDateTime.class, OffsetTime.class), OffsetDateTimeConversions::toOffsetTime);
        CONVERSION_DB.put(pair(Map.class, OffsetTime.class), MapConversions::toOffsetTime);
        CONVERSION_DB.put(pair(String.class, OffsetTime.class), StringConversions::toOffsetTime);

        // UUID conversions supported
        CONVERSION_DB.put(pair(Void.class, UUID.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(UUID.class, UUID.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, UUID.class), StringConversions::toUUID);
        CONVERSION_DB.put(pair(Boolean.class, UUID.class), BooleanConversions::toUUID);
        CONVERSION_DB.put(pair(BigInteger.class, UUID.class), BigIntegerConversions::toUUID);
        CONVERSION_DB.put(pair(BigDecimal.class, UUID.class), BigDecimalConversions::toUUID);
        CONVERSION_DB.put(pair(Map.class, UUID.class), MapConversions::toUUID);

        // Class conversions supported
        CONVERSION_DB.put(pair(Void.class, Class.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Class.class, Class.class), Converter::identity);
        CONVERSION_DB.put(pair(Map.class, Class.class), MapConversions::toClass);
        CONVERSION_DB.put(pair(String.class, Class.class), StringConversions::toClass);

        // Color conversions supported
        CONVERSION_DB.put(pair(Void.class, Color.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Color.class, Color.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, Color.class), StringConversions::toColor);
        CONVERSION_DB.put(pair(Map.class, Color.class), MapConversions::toColor);
        CONVERSION_DB.put(pair(int[].class, Color.class), ArrayConversions::toColor);

        // Dimension conversions supported
        CONVERSION_DB.put(pair(Void.class, Dimension.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Dimension.class, Dimension.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, Dimension.class), StringConversions::toDimension);
        CONVERSION_DB.put(pair(Map.class, Dimension.class), MapConversions::toDimension);
        CONVERSION_DB.put(pair(int[].class, Dimension.class), ArrayConversions::toDimension);

        // Point conversions supported
        CONVERSION_DB.put(pair(Void.class, Point.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Point.class, Point.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, Point.class), StringConversions::toPoint);
        CONVERSION_DB.put(pair(Map.class, Point.class), MapConversions::toPoint);
        CONVERSION_DB.put(pair(int[].class, Point.class), ArrayConversions::toPoint);
        CONVERSION_DB.put(pair(Dimension.class, Point.class), DimensionConversions::toPoint);

        // Rectangle conversions supported
        CONVERSION_DB.put(pair(Void.class, Rectangle.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Rectangle.class, Rectangle.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, Rectangle.class), StringConversions::toRectangle);
        CONVERSION_DB.put(pair(Map.class, Rectangle.class), MapConversions::toRectangle);
        CONVERSION_DB.put(pair(int[].class, Rectangle.class), ArrayConversions::toRectangle);
        CONVERSION_DB.put(pair(Dimension.class, Rectangle.class), DimensionConversions::toRectangle);

        // Insets conversions supported
        CONVERSION_DB.put(pair(Void.class, Insets.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Insets.class, Insets.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, Insets.class), StringConversions::toInsets);
        CONVERSION_DB.put(pair(Map.class, Insets.class), MapConversions::toInsets);
        CONVERSION_DB.put(pair(int[].class, Insets.class), ArrayConversions::toInsets);
        CONVERSION_DB.put(pair(Dimension.class, Insets.class), DimensionConversions::toInsets);

        // toFile
        CONVERSION_DB.put(pair(Void.class, File.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(File.class, File.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, File.class), StringConversions::toFile);
        CONVERSION_DB.put(pair(Map.class, File.class), MapConversions::toFile);
        CONVERSION_DB.put(pair(URI.class, File.class), UriConversions::toFile);
        CONVERSION_DB.put(pair(Path.class, File.class), PathConversions::toFile);
        CONVERSION_DB.put(pair(char[].class, File.class), ArrayConversions::charArrayToFile);
        CONVERSION_DB.put(pair(byte[].class, File.class), ArrayConversions::byteArrayToFile);

        // toPath
        CONVERSION_DB.put(pair(Void.class, Path.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Path.class, Path.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, Path.class), StringConversions::toPath);
        CONVERSION_DB.put(pair(Map.class, Path.class), MapConversions::toPath);
        CONVERSION_DB.put(pair(URI.class, Path.class), UriConversions::toPath);
        CONVERSION_DB.put(pair(File.class, Path.class), FileConversions::toPath);
        CONVERSION_DB.put(pair(char[].class, Path.class), ArrayConversions::charArrayToPath);
        CONVERSION_DB.put(pair(byte[].class, Path.class), ArrayConversions::byteArrayToPath);

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
        CONVERSION_DB.put(pair(Boolean.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(Character.class, String.class), CharacterConversions::toString);
        CONVERSION_DB.put(pair(BigInteger.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(BigDecimal.class, String.class), BigDecimalConversions::toString);
        CONVERSION_DB.put(pair(byte[].class, String.class), ByteArrayConversions::toString);
        CONVERSION_DB.put(pair(char[].class, String.class), CharArrayConversions::toString);
        CONVERSION_DB.put(pair(Character[].class, String.class), CharacterArrayConversions::toString);
        CONVERSION_DB.put(pair(ByteBuffer.class, String.class), ByteBufferConversions::toString);
        CONVERSION_DB.put(pair(CharBuffer.class, String.class), CharBufferConversions::toString);
        CONVERSION_DB.put(pair(Class.class, String.class), ClassConversions::toString);
        CONVERSION_DB.put(pair(Date.class, String.class), DateConversions::toString);
        CONVERSION_DB.put(pair(Calendar.class, String.class), CalendarConversions::toString);
        CONVERSION_DB.put(pair(java.sql.Date.class, String.class), SqlDateConversions::toString);
        CONVERSION_DB.put(pair(Timestamp.class, String.class), TimestampConversions::toString);
        CONVERSION_DB.put(pair(LocalDate.class, String.class), LocalDateConversions::toString);
        CONVERSION_DB.put(pair(LocalTime.class, String.class), LocalTimeConversions::toString);
        CONVERSION_DB.put(pair(LocalDateTime.class, String.class), LocalDateTimeConversions::toString);
        CONVERSION_DB.put(pair(ZonedDateTime.class, String.class), ZonedDateTimeConversions::toString);
        CONVERSION_DB.put(pair(UUID.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(Color.class, String.class), ColorConversions::toString);
        CONVERSION_DB.put(pair(Dimension.class, String.class), DimensionConversions::toString);
        CONVERSION_DB.put(pair(Point.class, String.class), PointConversions::toString);
        CONVERSION_DB.put(pair(Rectangle.class, String.class), RectangleConversions::toString);
        CONVERSION_DB.put(pair(Insets.class, String.class), InsetsConversions::toString);
        CONVERSION_DB.put(pair(Map.class, String.class), MapConversions::toString);
        CONVERSION_DB.put(pair(Enum.class, String.class), StringConversions::enumToString);
        CONVERSION_DB.put(pair(Enum.class, Integer.class), EnumConversions::enumToOrdinal);
        CONVERSION_DB.put(pair(String.class, Enum.class), (ConvertWithTarget<Enum<?>>) EnumConversions::stringToEnum);
        CONVERSION_DB.put(pair(int.class, Enum.class), (ConvertWithTarget<Enum<?>>) EnumConversions::intToEnum);
        CONVERSION_DB.put(pair(Integer.class, Enum.class), (ConvertWithTarget<Enum<?>>) EnumConversions::intToEnum);
        CONVERSION_DB.put(pair(Number.class, Enum.class), (ConvertWithTarget<Enum<?>>) EnumConversions::numberToEnum);
        CONVERSION_DB.put(pair(String.class, String.class), Converter::identity);
        CONVERSION_DB.put(pair(Duration.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(Instant.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(MonthDay.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(YearMonth.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(Period.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(ZoneId.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(ZoneOffset.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(OffsetTime.class, String.class), OffsetTimeConversions::toString);
        CONVERSION_DB.put(pair(OffsetDateTime.class, String.class), OffsetDateTimeConversions::toString);
        CONVERSION_DB.put(pair(Year.class, String.class), YearConversions::toString);
        CONVERSION_DB.put(pair(Locale.class, String.class), LocaleConversions::toString);
        CONVERSION_DB.put(pair(URI.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(URL.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(File.class, String.class), FileConversions::toString);
        CONVERSION_DB.put(pair(Path.class, String.class), PathConversions::toString);
        CONVERSION_DB.put(pair(TimeZone.class, String.class), TimeZoneConversions::toString);
        CONVERSION_DB.put(pair(Pattern.class, String.class), PatternConversions::toString);
        CONVERSION_DB.put(pair(Currency.class, String.class), CurrencyConversions::toString);
        CONVERSION_DB.put(pair(StringBuilder.class, String.class), UniversalConversions::toString);
        CONVERSION_DB.put(pair(StringBuffer.class, String.class), UniversalConversions::toString);

        // Currency conversions
        CONVERSION_DB.put(pair(Void.class, Currency.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Currency.class, Currency.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, Currency.class), StringConversions::toCurrency);
        CONVERSION_DB.put(pair(Map.class, Currency.class), MapConversions::toCurrency);

        // Pattern conversions
        CONVERSION_DB.put(pair(Void.class, Pattern.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Pattern.class, Pattern.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, Pattern.class), StringConversions::toPattern);
        CONVERSION_DB.put(pair(Map.class, Pattern.class), MapConversions::toPattern);

        // URL conversions
        CONVERSION_DB.put(pair(Void.class, URL.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(URL.class, URL.class), Converter::identity);
        CONVERSION_DB.put(pair(URI.class, URL.class), UriConversions::toURL);
        CONVERSION_DB.put(pair(String.class, URL.class), StringConversions::toURL);
        CONVERSION_DB.put(pair(Map.class, URL.class), MapConversions::toURL);
        CONVERSION_DB.put(pair(File.class, URL.class), FileConversions::toURL);
        CONVERSION_DB.put(pair(Path.class, URL.class), PathConversions::toURL);

        // URI Conversions
        CONVERSION_DB.put(pair(Void.class, URI.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(URI.class, URI.class), Converter::identity);
        CONVERSION_DB.put(pair(URL.class, URI.class), UrlConversions::toURI);
        CONVERSION_DB.put(pair(String.class, URI.class), StringConversions::toURI);
        CONVERSION_DB.put(pair(Map.class, URI.class), MapConversions::toURI);
        CONVERSION_DB.put(pair(File.class, URI.class), FileConversions::toURI);
        CONVERSION_DB.put(pair(Path.class, URI.class), PathConversions::toURI);

        // TimeZone Conversions
        CONVERSION_DB.put(pair(Void.class, TimeZone.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(TimeZone.class, TimeZone.class), Converter::identity);
        CONVERSION_DB.put(pair(String.class, TimeZone.class), StringConversions::toTimeZone);
        CONVERSION_DB.put(pair(Map.class, TimeZone.class), MapConversions::toTimeZone);
        CONVERSION_DB.put(pair(ZoneId.class, TimeZone.class), ZoneIdConversions::toTimeZone);
        CONVERSION_DB.put(pair(ZoneOffset.class, TimeZone.class), ZoneOffsetConversions::toTimeZone);

        // Duration conversions supported
        CONVERSION_DB.put(pair(Void.class, Duration.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Duration.class, Duration.class), Converter::identity);
        CONVERSION_DB.put(pair(Long.class, Duration.class), NumberConversions::longNanosToDuration);
        CONVERSION_DB.put(pair(Double.class, Duration.class), DoubleConversions::toDuration);
        CONVERSION_DB.put(pair(BigInteger.class, Duration.class), BigIntegerConversions::toDuration);
        CONVERSION_DB.put(pair(BigDecimal.class, Duration.class), BigDecimalConversions::toDuration);
        CONVERSION_DB.put(pair(Timestamp.class, Duration.class), TimestampConversions::toDuration);
        CONVERSION_DB.put(pair(String.class, Duration.class), StringConversions::toDuration);
        CONVERSION_DB.put(pair(Map.class, Duration.class), MapConversions::toDuration);

        // Instant conversions supported
        CONVERSION_DB.put(pair(Void.class, Instant.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Instant.class, Instant.class), Converter::identity);
        CONVERSION_DB.put(pair(Long.class, Instant.class), NumberConversions::longNanosToInstant);
        CONVERSION_DB.put(pair(Double.class, Instant.class), DoubleConversions::toInstant);
        CONVERSION_DB.put(pair(BigInteger.class, Instant.class), BigIntegerConversions::toInstant);
        CONVERSION_DB.put(pair(BigDecimal.class, Instant.class), BigDecimalConversions::toInstant);
        CONVERSION_DB.put(pair(java.sql.Date.class, Instant.class), SqlDateConversions::toInstant);
        CONVERSION_DB.put(pair(Timestamp.class, Instant.class), DateConversions::toInstant);
        CONVERSION_DB.put(pair(Date.class, Instant.class), DateConversions::toInstant);
        CONVERSION_DB.put(pair(LocalDate.class, Instant.class), LocalDateConversions::toInstant);
        CONVERSION_DB.put(pair(LocalDateTime.class, Instant.class), LocalDateTimeConversions::toInstant);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Instant.class), ZonedDateTimeConversions::toInstant);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Instant.class), OffsetDateTimeConversions::toInstant);
        CONVERSION_DB.put(pair(Duration.class, Instant.class), DurationConversions::toInstant);

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
        CONVERSION_DB.put(pair(ZoneId.class, ZoneOffset.class), ZoneIdConversions::toZoneOffset);
        CONVERSION_DB.put(pair(TimeZone.class, ZoneOffset.class), TimeZoneConversions::toZoneOffset);

        // MonthDay conversions supported
        CONVERSION_DB.put(pair(Void.class, MonthDay.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(MonthDay.class, MonthDay.class), Converter::identity);
        CONVERSION_DB.put(pair(java.sql.Date.class, MonthDay.class), SqlDateConversions::toMonthDay);
        CONVERSION_DB.put(pair(Date.class, MonthDay.class), DateConversions::toMonthDay);
        CONVERSION_DB.put(pair(Timestamp.class, MonthDay.class), TimestampConversions::toMonthDay);
        CONVERSION_DB.put(pair(LocalDate.class, MonthDay.class), LocalDateConversions::toMonthDay);
        CONVERSION_DB.put(pair(LocalDateTime.class, MonthDay.class), LocalDateTimeConversions::toMonthDay);
        CONVERSION_DB.put(pair(ZonedDateTime.class, MonthDay.class), ZonedDateTimeConversions::toMonthDay);
        CONVERSION_DB.put(pair(OffsetDateTime.class, MonthDay.class), OffsetDateTimeConversions::toMonthDay);
        CONVERSION_DB.put(pair(String.class, MonthDay.class), StringConversions::toMonthDay);
        CONVERSION_DB.put(pair(Map.class, MonthDay.class), MapConversions::toMonthDay);
        CONVERSION_DB.put(pair(Short.class, MonthDay.class), NumberConversions::toMonthDay);
        CONVERSION_DB.put(pair(Integer.class, MonthDay.class), NumberConversions::toMonthDay);
        CONVERSION_DB.put(pair(Long.class, MonthDay.class), NumberConversions::toMonthDay);
        CONVERSION_DB.put(pair(Float.class, MonthDay.class), NumberConversions::toMonthDay);
        CONVERSION_DB.put(pair(Double.class, MonthDay.class), NumberConversions::toMonthDay);
        // AtomicInteger/AtomicLong → MonthDay handled by surrogate system via Integer/Long
        CONVERSION_DB.put(pair(BigInteger.class, MonthDay.class), NumberConversions::toMonthDay);

        // YearMonth conversions supported
        CONVERSION_DB.put(pair(Void.class, YearMonth.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(YearMonth.class, YearMonth.class), Converter::identity);
        CONVERSION_DB.put(pair(java.sql.Date.class, YearMonth.class), SqlDateConversions::toYearMonth);
        CONVERSION_DB.put(pair(Date.class, YearMonth.class), DateConversions::toYearMonth);
        CONVERSION_DB.put(pair(Timestamp.class, YearMonth.class), TimestampConversions::toYearMonth);
        CONVERSION_DB.put(pair(LocalDate.class, YearMonth.class), LocalDateConversions::toYearMonth);
        CONVERSION_DB.put(pair(LocalDateTime.class, YearMonth.class), LocalDateTimeConversions::toYearMonth);
        CONVERSION_DB.put(pair(ZonedDateTime.class, YearMonth.class), ZonedDateTimeConversions::toYearMonth);
        CONVERSION_DB.put(pair(OffsetDateTime.class, YearMonth.class), OffsetDateTimeConversions::toYearMonth);
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

        // toStringBuilder - Bridge through String
        CONVERSION_DB.put(pair(Void.class, StringBuilder.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, StringBuilder.class), StringConversions::toStringBuilder);

        // toByteArray
        CONVERSION_DB.put(pair(Void.class, byte[].class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, byte[].class), StringConversions::toByteArray);
        CONVERSION_DB.put(pair(ByteBuffer.class, byte[].class), ByteBufferConversions::toByteArray);
        CONVERSION_DB.put(pair(CharBuffer.class, byte[].class), CharBufferConversions::toByteArray);
        CONVERSION_DB.put(pair(char[].class, byte[].class), (from, converter) -> ArrayConversions.arrayToArray(from, byte[].class, converter));
        CONVERSION_DB.put(pair(byte[].class, byte[].class), Converter::identity);
        CONVERSION_DB.put(pair(File.class, byte[].class), FileConversions::toByteArray);
        CONVERSION_DB.put(pair(Path.class, byte[].class), PathConversions::toByteArray);

        // toCharArray
        CONVERSION_DB.put(pair(Void.class, char[].class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, char[].class), StringConversions::toCharArray);
        CONVERSION_DB.put(pair(ByteBuffer.class, char[].class), ByteBufferConversions::toCharArray);
        CONVERSION_DB.put(pair(CharBuffer.class, char[].class), CharBufferConversions::toCharArray);
        CONVERSION_DB.put(pair(char[].class, char[].class), CharArrayConversions::toCharArray);
        CONVERSION_DB.put(pair(byte[].class, char[].class), (from, converter) -> ArrayConversions.arrayToArray(from, char[].class, converter));
        CONVERSION_DB.put(pair(File.class, char[].class), FileConversions::toCharArray);
        CONVERSION_DB.put(pair(Path.class, char[].class), PathConversions::toCharArray);

        // toCharacterArray
        CONVERSION_DB.put(pair(Void.class, Character[].class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, Character[].class), StringConversions::toCharacterArray);

        // toCharBuffer
        CONVERSION_DB.put(pair(Void.class, CharBuffer.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, CharBuffer.class), StringConversions::toCharBuffer);
        CONVERSION_DB.put(pair(ByteBuffer.class, CharBuffer.class), ByteBufferConversions::toCharBuffer);
        CONVERSION_DB.put(pair(CharBuffer.class, CharBuffer.class), CharBufferConversions::toCharBuffer);
        CONVERSION_DB.put(pair(char[].class, CharBuffer.class), CharArrayConversions::toCharBuffer);
        CONVERSION_DB.put(pair(byte[].class, CharBuffer.class), ByteArrayConversions::toCharBuffer);
        CONVERSION_DB.put(pair(Map.class, CharBuffer.class), MapConversions::toCharBuffer);

        // toByteBuffer
        CONVERSION_DB.put(pair(Void.class, ByteBuffer.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(String.class, ByteBuffer.class), StringConversions::toByteBuffer);
        CONVERSION_DB.put(pair(ByteBuffer.class, ByteBuffer.class), ByteBufferConversions::toByteBuffer);
        CONVERSION_DB.put(pair(CharBuffer.class, ByteBuffer.class), CharBufferConversions::toByteBuffer);
        CONVERSION_DB.put(pair(char[].class, ByteBuffer.class), CharArrayConversions::toByteBuffer);
        CONVERSION_DB.put(pair(byte[].class, ByteBuffer.class), ByteArrayConversions::toByteBuffer);
        CONVERSION_DB.put(pair(Map.class, ByteBuffer.class), MapConversions::toByteBuffer);

        // toYear
        CONVERSION_DB.put(pair(Void.class, Year.class), NumberConversions::nullToYear);
        CONVERSION_DB.put(pair(Year.class, Year.class), Converter::identity);
        CONVERSION_DB.put(pair(Short.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(Integer.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(Long.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(Float.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(Double.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(BigInteger.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(BigDecimal.class, Year.class), NumberConversions::toYear);
        CONVERSION_DB.put(pair(java.sql.Date.class, Year.class), SqlDateConversions::toYear);
        CONVERSION_DB.put(pair(Date.class, Year.class), DateConversions::toYear);
        CONVERSION_DB.put(pair(Timestamp.class, Year.class), TimestampConversions::toYear);
        CONVERSION_DB.put(pair(LocalDate.class, Year.class), LocalDateConversions::toYear);
        CONVERSION_DB.put(pair(LocalDateTime.class, Year.class), LocalDateTimeConversions::toYear);
        CONVERSION_DB.put(pair(ZonedDateTime.class, Year.class), ZonedDateTimeConversions::toYear);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Year.class), OffsetDateTimeConversions::toYear);
        CONVERSION_DB.put(pair(String.class, Year.class), StringConversions::toYear);
        CONVERSION_DB.put(pair(Map.class, Year.class), MapConversions::toYear);

        // Throwable conversions supported
        CONVERSION_DB.put(pair(Void.class, Throwable.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Map.class, Throwable.class), (ConvertWithTarget<Throwable>) MapConversions::toThrowable);

        // Map conversions supported
        CONVERSION_DB.put(pair(Void.class, Map.class), VoidConversions::toNull);
        CONVERSION_DB.put(pair(Byte.class, Map.class), UniversalConversions::toMap);
        CONVERSION_DB.put(pair(Short.class, Map.class), UniversalConversions::toMap);
        CONVERSION_DB.put(pair(Integer.class, Map.class), UniversalConversions::toMap);
        CONVERSION_DB.put(pair(Long.class, Map.class), UniversalConversions::toMap);
        CONVERSION_DB.put(pair(Float.class, Map.class), UniversalConversions::toMap);
        CONVERSION_DB.put(pair(Double.class, Map.class), UniversalConversions::toMap);
        CONVERSION_DB.put(pair(Boolean.class, Map.class), UniversalConversions::toMap);
        CONVERSION_DB.put(pair(Character.class, Map.class), UniversalConversions::toMap);
        CONVERSION_DB.put(pair(BigInteger.class, Map.class), UniversalConversions::bigIntegerToMap);
        CONVERSION_DB.put(pair(BigDecimal.class, Map.class), UniversalConversions::bigDecimalToMap);
        // AtomicBoolean/AtomicInteger/AtomicLong → Map handled by surrogate system via Boolean/Integer/Long
        CONVERSION_DB.put(pair(Date.class, Map.class), DateConversions::toMap);
        CONVERSION_DB.put(pair(java.sql.Date.class, Map.class), SqlDateConversions::toMap);
        CONVERSION_DB.put(pair(Timestamp.class, Map.class), TimestampConversions::toMap);
        CONVERSION_DB.put(pair(Calendar.class, Map.class), CalendarConversions::toMap);  // Restored - bridge produces different map key (zonedDateTime vs. calendar)
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
        CONVERSION_DB.put(pair(Class.class, Map.class), UniversalConversions::toMap);
        CONVERSION_DB.put(pair(UUID.class, Map.class), UUIDConversions::toMap);
        CONVERSION_DB.put(pair(Color.class, Map.class), ColorConversions::toMap);
        CONVERSION_DB.put(pair(Dimension.class, Map.class), DimensionConversions::toMap);
        CONVERSION_DB.put(pair(Point.class, Map.class), PointConversions::toMap);
        CONVERSION_DB.put(pair(Rectangle.class, Map.class), RectangleConversions::toMap);
        CONVERSION_DB.put(pair(Insets.class, Map.class), InsetsConversions::toMap);
        CONVERSION_DB.put(pair(String.class, Map.class), StringConversions::toMap);
        CONVERSION_DB.put(pair(Enum.class, Map.class), EnumConversions::toMap);
        CONVERSION_DB.put(pair(OffsetDateTime.class, Map.class), OffsetDateTimeConversions::toMap);
        CONVERSION_DB.put(pair(OffsetTime.class, Map.class), OffsetTimeConversions::toMap);
        CONVERSION_DB.put(pair(Year.class, Map.class), YearConversions::toMap);
        CONVERSION_DB.put(pair(Locale.class, Map.class), LocaleConversions::toMap);
        CONVERSION_DB.put(pair(URI.class, Map.class), UriConversions::toMap);
        CONVERSION_DB.put(pair(URL.class, Map.class), UrlConversions::toMap);
        CONVERSION_DB.put(pair(Throwable.class, Map.class), ThrowableConversions::toMap);
        CONVERSION_DB.put(pair(Pattern.class, Map.class), PatternConversions::toMap);
        CONVERSION_DB.put(pair(Currency.class, Map.class), CurrencyConversions::toMap);
        CONVERSION_DB.put(pair(ByteBuffer.class, Map.class), ByteBufferConversions::toMap);
        CONVERSION_DB.put(pair(CharBuffer.class, Map.class), CharBufferConversions::toMap);
        CONVERSION_DB.put(pair(File.class, Map.class), FileConversions::toMap);
        CONVERSION_DB.put(pair(Path.class, Map.class), PathConversions::toMap);

        // toIntArray
        CONVERSION_DB.put(pair(Color.class, int[].class), ColorConversions::toIntArray);
        CONVERSION_DB.put(pair(Dimension.class, int[].class), DimensionConversions::toIntArray);
        CONVERSION_DB.put(pair(Point.class, int[].class), PointConversions::toIntArray);
        CONVERSION_DB.put(pair(Rectangle.class, int[].class), RectangleConversions::toIntArray);
        CONVERSION_DB.put(pair(Insets.class, int[].class), InsetsConversions::toIntArray);

        // Array-like type bridges for universal array system access
        // ========================================
        // Atomic Array Bridges
        // ========================================

        // AtomicIntegerArray ↔ int[] bridges
        CONVERSION_DB.put(pair(AtomicIntegerArray.class, int[].class), UniversalConversions::atomicIntegerArrayToIntArray);
        CONVERSION_DB.put(pair(int[].class, AtomicIntegerArray.class), UniversalConversions::intArrayToAtomicIntegerArray);

        // AtomicLongArray ↔ long[] bridges  
        CONVERSION_DB.put(pair(AtomicLongArray.class, long[].class), UniversalConversions::atomicLongArrayToLongArray);
        CONVERSION_DB.put(pair(long[].class, AtomicLongArray.class), UniversalConversions::longArrayToAtomicLongArray);

        // AtomicReferenceArray ↔ Object[] bridges
        CONVERSION_DB.put(pair(AtomicReferenceArray.class, Object[].class), UniversalConversions::atomicReferenceArrayToObjectArray);
        CONVERSION_DB.put(pair(Object[].class, AtomicReferenceArray.class), UniversalConversions::objectArrayToAtomicReferenceArray);

        // AtomicReferenceArray ↔ String[] bridges
        CONVERSION_DB.put(pair(AtomicReferenceArray.class, String[].class), UniversalConversions::atomicReferenceArrayToStringArray);
        CONVERSION_DB.put(pair(String[].class, AtomicReferenceArray.class), UniversalConversions::stringArrayToAtomicReferenceArray);

        // ========================================
        // NIO Buffer Bridges
        // ========================================

        // IntBuffer ↔ int[] bridges
        CONVERSION_DB.put(pair(IntBuffer.class, int[].class), UniversalConversions::intBufferToIntArray);
        CONVERSION_DB.put(pair(int[].class, IntBuffer.class), UniversalConversions::intArrayToIntBuffer);

        // LongBuffer ↔ long[] bridges
        CONVERSION_DB.put(pair(LongBuffer.class, long[].class), UniversalConversions::longBufferToLongArray);
        CONVERSION_DB.put(pair(long[].class, LongBuffer.class), UniversalConversions::longArrayToLongBuffer);

        // FloatBuffer ↔ float[] bridges
        CONVERSION_DB.put(pair(FloatBuffer.class, float[].class), UniversalConversions::floatBufferToFloatArray);
        CONVERSION_DB.put(pair(float[].class, FloatBuffer.class), UniversalConversions::floatArrayToFloatBuffer);

        // DoubleBuffer ↔ double[] bridges
        CONVERSION_DB.put(pair(DoubleBuffer.class, double[].class), UniversalConversions::doubleBufferToDoubleArray);
        CONVERSION_DB.put(pair(double[].class, DoubleBuffer.class), UniversalConversions::doubleArrayToDoubleBuffer);

        // ShortBuffer ↔ short[] bridges
        CONVERSION_DB.put(pair(ShortBuffer.class, short[].class), UniversalConversions::shortBufferToShortArray);
        CONVERSION_DB.put(pair(short[].class, ShortBuffer.class), UniversalConversions::shortArrayToShortBuffer);

        // ========================================
        // BitSet Bridges
        // ========================================

        // BitSet ↔ boolean[] bridges
        CONVERSION_DB.put(pair(BitSet.class, boolean[].class), UniversalConversions::bitSetToBooleanArray);
        CONVERSION_DB.put(pair(boolean[].class, BitSet.class), UniversalConversions::booleanArrayToBitSet);

        // BitSet ↔ int[] bridges (set bit indices)
        CONVERSION_DB.put(pair(BitSet.class, int[].class), UniversalConversions::bitSetToIntArray);
        CONVERSION_DB.put(pair(int[].class, BitSet.class), UniversalConversions::intArrayToBitSet);

        // BitSet ↔ byte[] bridges
        CONVERSION_DB.put(pair(BitSet.class, byte[].class), UniversalConversions::bitSetToByteArray);
        CONVERSION_DB.put(pair(byte[].class, BitSet.class), UniversalConversions::byteArrayToBitSet);

        // BitSet ↔ long bridges (lower 64 bits)
        CONVERSION_DB.put(pair(BitSet.class, Long.class), UniversalConversions::bitSetToLong);
        CONVERSION_DB.put(pair(Long.class, BitSet.class), UniversalConversions::longToBitSet);
        CONVERSION_DB.put(pair(BitSet.class, long.class), UniversalConversions::bitSetToLong);
        CONVERSION_DB.put(pair(long.class, BitSet.class), UniversalConversions::longToBitSet);

        // BitSet ↔ BigInteger bridges (arbitrary size)
        CONVERSION_DB.put(pair(BitSet.class, BigInteger.class), UniversalConversions::bitSetToBigInteger);
        CONVERSION_DB.put(pair(BigInteger.class, BitSet.class), UniversalConversions::bigIntegerToBitSet);

        // BitSet ↔ int bridges (lower 32 bits)
        CONVERSION_DB.put(pair(BitSet.class, Integer.class), UniversalConversions::bitSetToInt);
        CONVERSION_DB.put(pair(Integer.class, BitSet.class), UniversalConversions::intToBitSet);
        CONVERSION_DB.put(pair(BitSet.class, int.class), UniversalConversions::bitSetToInt);
        CONVERSION_DB.put(pair(int.class, BitSet.class), UniversalConversions::intToBitSet);

        // BitSet ↔ short bridges (lower 16 bits)
        CONVERSION_DB.put(pair(BitSet.class, Short.class), UniversalConversions::bitSetToShort);
        CONVERSION_DB.put(pair(Short.class, BitSet.class), UniversalConversions::shortToBitSet);
        CONVERSION_DB.put(pair(BitSet.class, short.class), UniversalConversions::bitSetToShort);
        CONVERSION_DB.put(pair(short.class, BitSet.class), UniversalConversions::shortToBitSet);

        // BitSet ↔ byte bridges (lower 8 bits)
        CONVERSION_DB.put(pair(BitSet.class, Byte.class), UniversalConversions::bitSetToByte);
        CONVERSION_DB.put(pair(Byte.class, BitSet.class), UniversalConversions::byteToBitSet);
        CONVERSION_DB.put(pair(BitSet.class, byte.class), UniversalConversions::bitSetToByte);
        CONVERSION_DB.put(pair(byte.class, BitSet.class), UniversalConversions::byteToBitSet);

        // BitSet ↔ AtomicInteger handled by surrogate system via Integer

        // BitSet ↔ BigDecimal bridges (arbitrary size)
        CONVERSION_DB.put(pair(BitSet.class, BigDecimal.class), UniversalConversions::bitSetToBigDecimal);
        CONVERSION_DB.put(pair(BigDecimal.class, BitSet.class), UniversalConversions::bigDecimalToBitSet);

        // BitSet ↔ Boolean bridges
        CONVERSION_DB.put(pair(BitSet.class, Boolean.class), UniversalConversions::bitSetToBoolean);
        CONVERSION_DB.put(pair(Boolean.class, BitSet.class), UniversalConversions::booleanToBitSet);

        // BitSet ↔ AtomicBoolean handled by surrogate system via Boolean

        // BitSet ↔ String bridges (binary string format)
        CONVERSION_DB.put(pair(BitSet.class, String.class), UniversalConversions::bitSetToString);
        CONVERSION_DB.put(pair(String.class, BitSet.class), UniversalConversions::stringToBitSet);

        // ========================================
        // Stream Bridges
        // ========================================

        // Array → Stream bridges (Stream → Array removed due to single-use limitation)
        CONVERSION_DB.put(pair(int[].class, IntStream.class), UniversalConversions::intArrayToIntStream);
        CONVERSION_DB.put(pair(long[].class, LongStream.class), UniversalConversions::longArrayToLongStream);
        CONVERSION_DB.put(pair(double[].class, DoubleStream.class), UniversalConversions::doubleArrayToDoubleStream);

        // Register Record.class -> Map.class conversion if Records are supported
        try {
            Class<?> recordClass = Class.forName("java.lang.Record");
            CONVERSION_DB.put(pair(recordClass, Map.class, 0L), MapConversions::recordToMap);
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
                    
                    // Date & Time
                    new SurrogatePrimaryPair(Calendar.class, ZonedDateTime.class,
                            CalendarConversions::toZonedDateTime, null)
            );
        }
        return SURROGATE_TO_PRIMARY_PAIRS;
    }

    /**
     * List 2: PRIMARY → SURROGATE (everythingThatCanReachPrimaryCanAlsoReachSurrogate)
     * These pairs let callers land on the surrogate instead of the primary when they
     * are traveling into the ecosystem. They do not guarantee the reverse trip is
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
        List<Map.Entry<ConversionPair, Convert<?>>> existingEntries = new ArrayList<>(CONVERSION_DB.size());
        for (Map.Entry<ConversionPair, Convert<?>> entry : CONVERSION_DB.entrySet()) {
            // All entries are ConversionPair instances with (Class, Class, long) pattern
            existingEntries.add(entry);
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
                for (Map.Entry<ConversionPair, Convert<?>> entry : existingEntries) {
                    ConversionPair key = entry.getKey();
                    Class<?> sourceClass = key.getSource();
                    Class<?> targetClass = key.getTarget();
                    long instanceId = key.getInstanceId();
                    if (sourceClass.equals(primaryClass)) {
                        // Only add if not already defined and not converting to itself
                        if (CONVERSION_DB.get(pair(surrogateClass, targetClass, 0L)) == null && !targetClass.equals(surrogateClass)) {
                            // Create composite conversion: Surrogate → primary → target
                            Convert<?> originalConversion = CONVERSION_DB.get(pair(sourceClass, targetClass, instanceId));
                            Convert<?> bridgeConversion = createSurrogateToPrimaryBridgeConversion(config, originalConversion);
                            CONVERSION_DB.put(pair(surrogateClass, targetClass, 0L), bridgeConversion);
                        }
                    }
                }
            } else {
                // REVERSE BRIDGES: Source → Primary → Surrogate
                // Example: String.class → Integer.class → int.class
                Class<?> primaryClass = config.surrogateClass;  // Note: in List 2, surrogate is the source
                Class<?> surrogateClass = config.primaryClass;  // and primary is the target

                // Find all sources that can convert to the primary class
                for (Map.Entry<ConversionPair, Convert<?>> entry : existingEntries) {
                    ConversionPair key = entry.getKey();
                    Class<?> sourceClass = key.getSource();
                    Class<?> targetClass = key.getTarget();
                    long instanceId = key.getInstanceId();
                    if (targetClass.equals(primaryClass)) {
                        // Only add if not already defined and not converting from itself
                        if (CONVERSION_DB.get(pair(sourceClass, surrogateClass, 0L)) == null && !sourceClass.equals(surrogateClass)) {
                            // Create composite conversion: Source → primary → surrogate
                            Convert<?> originalConversion = CONVERSION_DB.get(pair(sourceClass, targetClass, instanceId));
                            Convert<?> bridgeConversion = createPrimaryToSurrogateBridgeConversion(config, originalConversion);
                            CONVERSION_DB.put(pair(sourceClass, surrogateClass, 0L), bridgeConversion);
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

        // Return ConvertWithTarget to pass target type through (works for both Convert and ConvertWithTarget)
        return (ConvertWithTarget<?>) (from, converter, target) -> {
            // First: Convert surrogate to primary (e.g., int → Integer, AtomicInteger → Integer)
            Object primaryValue = surrogateToPrimaryConversion.convert(from, converter);
            // Second: Convert primary to target using existing conversion, passing target type
            return primaryToTargetConversion.convert(primaryValue, converter, target);
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

        // Return ConvertWithTarget to pass target type through (works for both Convert and ConvertWithTarget)
        return (ConvertWithTarget<?>) (from, converter, target) -> {
            // First: Convert a source to primary using existing conversion
            Object primaryValue = sourceToPrimaryConversion.convert(from, converter, target);
            // Second: Convert primary to surrogate (e.g., Integer → int, Integer → AtomicInteger)
            return primaryToSurrogateConversion.convert(primaryValue, converter, target);
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
            USER_DB.put(pair(pair.getSource(), pair.getTarget(), pair.getInstanceId()), entry.getValue());
            
            // Add identity conversions for non-standard types to enable O(1) hasConverterOverrideFor lookup
            addIdentityConversionIfNeeded(pair.getSource(), pair.getInstanceId());
            addIdentityConversionIfNeeded(pair.getTarget(), pair.getInstanceId());
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
        Convert<?> conversionMethod = USER_DB.get(pair(sourceType, toType, this.instanceId));
        if (isValidConversion(conversionMethod)) {
            cacheConverter(sourceType, toType, conversionMethod);
            return (T) conversionMethod.convert(from, this, toType);
        }

        // Then check the factory conversion database.
        conversionMethod = CONVERSION_DB.get(pair(sourceType, toType, 0L));
        if (isValidConversion(conversionMethod)) {
            // Cache built-in conversions with instance ID 0 to keep them shared across instances
            FULL_CONVERSION_CACHE.put(pair(sourceType, toType, 0L), conversionMethod);
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
        // Check instance-specific cache first, then fall back to shared conversions.
        // ConversionPair allocation is very cheap (~4ns) - benchmarking showed it's faster than caching pairs.
        Convert<?> converter = FULL_CONVERSION_CACHE.get(pair(source, target, this.instanceId));
        if (converter != null) {
            return converter;
        }

        // Fall back to shared conversions (instance ID 0)
        return FULL_CONVERSION_CACHE.get(pair(source, target, 0L));
    }

    private void cacheConverter(Class<?> source, Class<?> target, Convert<?> converter) {
        // Use put with pair() to create conversion key
        FULL_CONVERSION_CACHE.put(pair(source, target, this.instanceId), converter);
    }

    @SuppressWarnings("unchecked")
    private <T> T attemptContainerConversion(Object from, Class<?> sourceType, Class<T> toType) {
        // Validate source type is a container type (Array, Collection, or Map)
        if (!(from.getClass().isArray() || from instanceof Collection || from instanceof Map)) {
            return null;
        }

        // If source is already an EnumSet and target is a compatible type (superclass/interface),
        // return as-is - no conversion needed. This preserves EnumSet when asked for Collection/Set.
        if (from instanceof EnumSet && toType.isAssignableFrom(sourceType)) {
            return (T) from;
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
    private Convert<?> getInheritedConverter(Class<?> sourceType, Class<?> toType, long instanceId) {
        // Get cached sorted inheritance pairs (builds and caches on first call for this source/target combination)
        InheritancePair[] pairs = getSortedInheritancePairs(sourceType, toType);

        // Iterate over sorted pairs and check the converter databases.
        for (InheritancePair inheritancePair : pairs) {
            final Class<?> source = inheritancePair.source;
            final Class<?> target = inheritancePair.target;

            Convert<?> tempConverter = USER_DB.get(pair(source, target, instanceId));
            if (tempConverter != null) {
                return tempConverter;
            }
            tempConverter = CONVERSION_DB.get(pair(source, target, 0L));
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
     * Gets the complete type hierarchy for a class, including the class itself at level 0.
     * <p>
     * This method returns a cached, immutable sorted set containing:
     * <ul>
     *   <li>The class itself at level 0</li>
     *   <li>All superclasses and interfaces with their inheritance distances</li>
     * </ul>
     * The result is cached per class, so no copying is needed on subsequent calls.
     * </p>
     *
     * @param clazz The class to analyze
     * @return Sorted set of ClassLevel objects representing the complete type hierarchy
     */
    private static SortedSet<ClassLevel> getCompleteTypeHierarchy(Class<?> clazz) {
        return cacheCompleteHierarchy.computeIfAbsent(clazz, key -> {
            SortedSet<ClassLevel> hierarchy = new TreeSet<>(getSuperClassesAndInterfaces(key));
            hierarchy.add(new ClassLevel(key, 0));
            return hierarchy;
        });
    }

    /**
     * Gets the sorted list of inheritance pairs for a given source/target type combination.
     * <p>
     * The pairs are sorted by:
     * <ol>
     *   <li>Exact target matches first</li>
     *   <li>More specific target types (assignability)</li>
     *   <li>Combined inheritance distance (lower is better)</li>
     *   <li>Concrete classes before interfaces</li>
     * </ol>
     * The result is cached per (sourceType, toType) combination.
     * </p>
     *
     * @param sourceType The source type
     * @param toType The target type
     * @return Cached, sorted array of InheritancePair objects
     */
    private static InheritancePair[] getSortedInheritancePairs(Class<?> sourceType, Class<?> toType) {
        Object[] key = {sourceType, toType};
        InheritancePair[] cached = cacheInheritancePairs.get(key);
        if (cached != null) {
            return cached;
        }

        // Build pairs from complete hierarchies (already cached per type)
        SortedSet<ClassLevel> sourceTypes = getCompleteTypeHierarchy(sourceType);
        SortedSet<ClassLevel> targetTypes = getCompleteTypeHierarchy(toType);

        List<InheritancePair> pairs = new ArrayList<>(sourceTypes.size() * targetTypes.size());
        for (ClassLevel source : sourceTypes) {
            for (ClassLevel target : targetTypes) {
                pairs.add(new InheritancePair(source.clazz, target.clazz, source.level, target.level));
            }
        }

        // Sort the pairs by a composite of rules
        final Class<?> finalToType = toType;
        pairs.sort((p1, p2) -> {
            // Exact target matches first
            boolean p1ExactTarget = p1.target == finalToType;
            boolean p2ExactTarget = p2.target == finalToType;
            if (p1ExactTarget != p2ExactTarget) {
                return p1ExactTarget ? -1 : 1;
            }
            // More specific target types (by assignability)
            if (p1.target != p2.target) {
                boolean p1AssignableToP2 = p2.target.isAssignableFrom(p1.target);
                boolean p2AssignableToP1 = p1.target.isAssignableFrom(p2.target);
                if (p1AssignableToP2 != p2AssignableToP1) {
                    return p1AssignableToP2 ? -1 : 1;
                }
            }
            // Combined inheritance distance
            int dist1 = p1.sourceLevel + p1.targetLevel;
            int dist2 = p2.sourceLevel + p2.targetLevel;
            if (dist1 != dist2) {
                return dist1 - dist2;
            }
            // Prefer concrete classes over interfaces (source)
            boolean p1FromInterface = p1.source.isInterface();
            boolean p2FromInterface = p2.source.isInterface();
            if (p1FromInterface != p2FromInterface) {
                return p1FromInterface ? 1 : -1;
            }
            // Prefer concrete classes over interfaces (target)
            boolean p1ToInterface = p1.target.isInterface();
            boolean p2ToInterface = p2.target.isInterface();
            if (p1ToInterface != p2ToInterface) {
                return p1ToInterface ? 1 : -1;
            }
            return 0;
        });

        InheritancePair[] pairsArray = pairs.toArray(new InheritancePair[0]);
        cacheInheritancePairs.putMultiKey(pairsArray, sourceType, toType);
        return pairsArray;
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
     * Represents a source/target class pair with their inheritance levels for converter lookup.
     * <p>
     * This class is used to cache the sorted list of inheritance pairs for a given source/target
     * type combination, avoiding repeated computation and sorting.
     * </p>
     */
    static final class InheritancePair {
        final Class<?> source;
        final Class<?> target;
        final int sourceLevel;
        final int targetLevel;

        InheritancePair(Class<?> source, Class<?> target, int sourceLevel, int targetLevel) {
            this.source = source;
            this.target = target;
            this.sourceLevel = sourceLevel;
            this.targetLevel = targetLevel;
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
                Convert<?> converter = USER_DB.get(pair(sourceType, targetType, instanceId));
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
                Class<?> srcComp = source.getComponentType();
                Class<?> tgtComp = target.getComponentType();
                // If either component is Object, be optimistic - we can't know actual element types at compile time
                // If both are specific types, recursively check if component conversion is supported
                return srcComp == Object.class || tgtComp == Object.class ||
                        isConversionSupportedFor(srcComp, tgtComp);
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
    private Convert<?> getConversionFromDBs(Class<?> source, Class<?> target) {
        source = ClassUtilities.toPrimitiveWrapperClass(source);
        target = ClassUtilities.toPrimitiveWrapperClass(target);
        Convert<?> method = USER_DB.get(pair(source, target, 0L));
        if (isValidConversion(method)) {
            return method;
        }
        method = CONVERSION_DB.get(pair(source, target, 0L));
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
     * <p>
     * This includes both simple type conversions from the conversion database and dynamic
     * array/collection conversions which are handled at runtime.
     * </p>
     *
     * @return A {@code Map<Class<?>, Set<Class<?>>>} representing all supported (built-in) conversions.
     */
    public static Map<Class<?>, Set<Class<?>>> allSupportedConversions() {
        Map<Class<?>, Set<Class<?>>> toFrom = new TreeMap<>(Comparator.comparing(Class::getName));
        addSupportedConversion(CONVERSION_DB, toFrom);
        addArrayCollectionConversions(toFrom);
        return toFrom;
    }

    /**
     * Retrieves a map of all supported conversions with class names instead of class objects.
     * <p>
     * The returned map's keys are source class names, and each key maps to a {@code Set} of target class names
     * that the source can be converted to.
     * </p>
     * <p>
     * This includes both simple type conversions from the conversion database and dynamic
     * array/collection conversions which are handled at runtime.
     * </p>
     *
     * @return A {@code Map<String, Set<String>>} representing all supported (built-int) conversions by class names.
     */
    public static Map<String, Set<String>> getSupportedConversions() {
        Map<String, Set<String>> toFrom = new TreeMap<>(String::compareTo);
        addSupportedConversionName(CONVERSION_DB, toFrom);
        addArrayCollectionConversionNames(toFrom);
        return toFrom;
    }

    /**
     * Adds the dynamic array/collection conversion entries to the supported conversions map.
     * These conversions are handled at runtime by ArrayConversions and CollectionConversions
     * and support any component types where the component conversion is supported.
     */
    private static void addArrayCollectionConversions(Map<Class<?>, Set<Class<?>>> toFrom) {
        Comparator<Class<?>> classComparator = Comparator.comparing(Class::getName);

        // Array to Array (component types converted dynamically)
        toFrom.computeIfAbsent(Object[].class, k -> new TreeSet<>(classComparator)).add(Object[].class);

        // Array to Collection
        toFrom.computeIfAbsent(Object[].class, k -> new TreeSet<>(classComparator)).add(Collection.class);

        // Array to Enum (creates EnumSet)
        toFrom.computeIfAbsent(Object[].class, k -> new TreeSet<>(classComparator)).add(Enum.class);

        // Collection to Array
        toFrom.computeIfAbsent(Collection.class, k -> new TreeSet<>(classComparator)).add(Object[].class);

        // Collection to Collection
        toFrom.computeIfAbsent(Collection.class, k -> new TreeSet<>(classComparator)).add(Collection.class);

        // Collection to Enum (creates EnumSet)
        toFrom.computeIfAbsent(Collection.class, k -> new TreeSet<>(classComparator)).add(Enum.class);

        // Map to Enum (creates EnumSet from keySet)
        toFrom.computeIfAbsent(Map.class, k -> new TreeSet<>(classComparator)).add(Enum.class);

        // EnumSet to Array
        toFrom.computeIfAbsent(EnumSet.class, k -> new TreeSet<>(classComparator)).add(Object[].class);

        // EnumSet to Collection
        toFrom.computeIfAbsent(EnumSet.class, k -> new TreeSet<>(classComparator)).add(Collection.class);
    }

    /**
     * Adds the dynamic array/collection conversion entry names to the supported conversions map.
     */
    private static void addArrayCollectionConversionNames(Map<String, Set<String>> toFrom) {
        // Array to Array (component types converted dynamically)
        toFrom.computeIfAbsent("Object[]", k -> new TreeSet<>()).add("Object[]");

        // Array to Collection
        toFrom.computeIfAbsent("Object[]", k -> new TreeSet<>()).add("Collection");

        // Array to Enum (creates EnumSet)
        toFrom.computeIfAbsent("Object[]", k -> new TreeSet<>()).add("Enum");

        // Collection to Array
        toFrom.computeIfAbsent("Collection", k -> new TreeSet<>()).add("Object[]");

        // Collection to Collection
        toFrom.computeIfAbsent("Collection", k -> new TreeSet<>()).add("Collection");

        // Collection to Enum (creates EnumSet)
        toFrom.computeIfAbsent("Collection", k -> new TreeSet<>()).add("Enum");

        // Map to Enum (creates EnumSet from keySet)
        toFrom.computeIfAbsent("Map", k -> new TreeSet<>()).add("Enum");

        // EnumSet to Array
        toFrom.computeIfAbsent("EnumSet", k -> new TreeSet<>()).add("Object[]");

        // EnumSet to Collection
        toFrom.computeIfAbsent("EnumSet", k -> new TreeSet<>()).add("Collection");
    }

    /**
     * Populates the provided map with supported conversions from the specified conversion database.
     *
     * @param db     The conversion database containing conversion mappings.
     * @param toFrom The map to populate with supported conversions.
     */
    private static void addSupportedConversion(Map<ConversionPair, Convert<?>> db, Map<Class<?>, Set<Class<?>>> toFrom) {
        for (Map.Entry<ConversionPair, Convert<?>> entry : db.entrySet()) {
            if (entry.getValue() != UNSUPPORTED) {
                ConversionPair key = entry.getKey();
                Class<?> source = key.getSource();
                Class<?> target = key.getTarget();
                toFrom.computeIfAbsent(source, k -> new TreeSet<>(Comparator.comparing((Class<?> c) -> c.getName()))).add(target);
            }
        }
    }

    /**
     * Populates the provided map with supported conversions from the specified conversion database, using class names.
     *
     * @param db     The conversion database containing conversion mappings.
     * @param toFrom The map to populate with supported conversions by class names.
     */
    private static void addSupportedConversionName(Map<ConversionPair, Convert<?>> db, Map<String, Set<String>> toFrom) {
        for (Map.Entry<ConversionPair, Convert<?>> entry : db.entrySet()) {
            if (entry.getValue() != UNSUPPORTED) {
                ConversionPair key = entry.getKey();
                Class<?> source = key.getSource();
                Class<?> target = key.getTarget();
                toFrom.computeIfAbsent(getShortName(source), k -> new TreeSet<>(String::compareTo)).add(getShortName(target));
            }
        }
    }

    /**
     * @param conversionMethod A method that converts an instance of the source type to an instance of the target type.
     * @return The previous conversion method associated with the source and target types, or {@code null} if no conversion existed.
     * @deprecated Use {@link #addConversion(Convert, Class, Class)} instead. This method will be removed in a future version as it is less safe and does not handle all type variations correctly.
     */
    @Deprecated
    public Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionMethod) {
        return addConversion(conversionMethod, source, target);
    }

    /**
     * Adds a new conversion function for converting from one type to another for this specific Converter instance.
     * <p>When {@code convert(source, target)} is called on this instance, the conversion function is located by:
     * <ol>
     *   <li>Checking instance-specific conversions first (added via this method)</li>
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
        Convert<?> previous = USER_DB.get(pair(wrapperSource, wrapperTarget, this.instanceId));
        USER_DB.put(pair(wrapperSource, wrapperTarget, this.instanceId), conversionMethod);

        // Add all type combinations to USER_DB with this instance ID
        for (Class<?> srcType : sourceTypes) {
            for (Class<?> tgtType : targetTypes) {
                USER_DB.put(pair(srcType, tgtType, this.instanceId), conversionMethod);
            }
        }

        // Add identity conversions for non-standard types to enable O(1) hasConverterOverrideFor lookup
        addIdentityConversionIfNeeded(source, this.instanceId);
        addIdentityConversionIfNeeded(target, this.instanceId);
        return previous;
    }

    /**
     * Helper method to get all type variations (primitive and wrapper) for a given class.
     */
    private static Set<Class<?>> getTypeVariations(Class<?> clazz) {
        Set<Class<?>> types = new IdentitySet<>();
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
     * Adds an identity conversion (T -> T) for a non-standard type to enable O(1) lookup
     * in hasConverterOverrideFor. This serves as a marker that the type is involved in
     * custom conversions while also providing useful identity conversion functionality.
     * 
     * @param type the type to add identity conversion for
     * @param instanceId the instance ID to use for the conversion
     */
    private void addIdentityConversionIfNeeded(Class<?> type, long instanceId) {
        if (type != null && USER_DB.get(pair(type, type, instanceId)) == null) {
            USER_DB.put(pair(type, type, instanceId), IDENTITY_CONVERTER);
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
        java.util.List<ConversionPair> keysToRemove = new java.util.ArrayList<>(16);
        for (Map.Entry<ConversionPair, Convert<?>> entry : FULL_CONVERSION_CACHE.entrySet()) {
            ConversionPair key = entry.getKey();
            Class<?> sourceClass = key.getSource();
            Class<?> targetClass = key.getTarget();
            if ((sourceClass == source && targetClass == target) ||
                // Also clear inheritance-based entries
                isInheritanceRelated(sourceClass, targetClass, source, target)) {
                keysToRemove.add(key);
            }
        }

        // Remove the collected keys
        for (ConversionPair key : keysToRemove) {
            FULL_CONVERSION_CACHE.remove(key);
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
