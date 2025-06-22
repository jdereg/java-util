package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;

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
 * @author
 *         <br>
 *         John DeRegnaucourt (jdereg@gmail.com)
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
public final class Converter
{
    private static final com.cedarsoftware.util.convert.Converter instance =
            new com.cedarsoftware.util.convert.Converter(new DefaultConverterOptions());

    /**
     * Static utility class.
     */
    private Converter() { }

    /**
     * Provides access to the default {@link com.cedarsoftware.util.convert.Converter}
     * instance used by this class.
     * <p>
     * The returned instance is created with {@link DefaultConverterOptions} and is
     * the same one used by all static conversion APIs. It is immutable and
     * thread-safe.
     * </p>
     *
     * @return the default {@code Converter} instance
     */
    public static com.cedarsoftware.util.convert.Converter getInstance() {
        return instance;
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
     * This is accomplished using the {@link #addConversion(Class, Class, Convert)} method, which accepts the source type,
     * target type, and a {@link Convert} functional interface implementation that defines the conversion logic.
     * </p>
     *
     * <h3>Performance Considerations:</h3>
     * <p>
     * The Converter uses caching mechanisms to store and retrieve converters, ensuring efficient performance
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
    public static <T> T convert(Object from, Class<T> toType) {
        return instance.convert(from, toType);
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
    public static boolean isConversionSupportedFor(Class<?> source, Class<?> target) {
        return instance.isConversionSupportedFor(source, target);
    }

    /**
     * Overload of {@link #isConversionSupportedFor(Class, Class)} that checks a single
     * class for conversion support using cached results.
     *
     * @param type the class to query
     * @return {@code true} if the converter supports this class
     */
    public static boolean isConversionSupportedFor(Class<?> type) {
        return instance.isConversionSupportedFor(type);
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
     * // Check if String can be converted to Integer
     * boolean canConvert = Converter.isSimpleTypeConversionSupported(
     *     String.class, Integer.class);  // returns true
     *
     * // Check array conversion (always returns false)
     * boolean arrayConvert = Converter.isSimpleTypeConversionSupported(
     *     String[].class, Integer[].class);  // returns false
     *
     * // Intentionally repeat source type (class) - will find identity conversion
     * // Let's us know that it is a "simple" type (String, Date, Class, UUID, URL, Temporal type, etc.)
     * boolean isSimpleType = Converter.isSimpleTypeConversionSupported(
     *     ZonedDateTime.class, ZonedDateTime.class);
     *
     * // Check collection conversion (always returns false)
     * boolean listConvert = Converter.isSimpleTypeConversionSupported(
     *     List.class, Set.class);  // returns false
     * }</pre>
     *
     * @param source The source class type to check
     * @param target The target class type to check
     * @return {@code true} if a non-collection conversion exists between the types,
     *         {@code false} if either type is an array/collection or no conversion exists
     * @see #isConversionSupportedFor(Class, Class)
     */
    public static boolean isSimpleTypeConversionSupported(Class<?> source, Class<?> target) {
        return instance.isSimpleTypeConversionSupported(source, target);
    }

    /**
     * Overload of {@link #isSimpleTypeConversionSupported(Class, Class)} for querying
     * if a single class is treated as a simple type. Results are cached.
     *
     * @param type the class to check
     * @return {@code true} if the class is a simple convertible type
     */
    public static boolean isSimpleTypeConversionSupported(Class<?> type) {
        return instance.isSimpleTypeConversionSupported(type);
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
        return instance.allSupportedConversions();
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
        return instance.getSupportedConversions();
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
    public static Convert<?> addConversion(Class<?> source, Class<?> target, Convert<?> conversionFunction) {
        return instance.addConversion(source, target, conversionFunction);
    }

    /**
     * Convert from the passed in instance to a String.  If null is passed in, this method will return "".
     * Call 'getSupportedConversions()' to see all conversion options for all Classes (all sources to all destinations).
     */
    public static String convert2String(Object fromInstance)
    {
        if (fromInstance == null) {
            return "";
        }
        return instance.convert(fromInstance, String.class);
    }

    /**
     * Convert from the passed in instance to a String.  If null is passed in, this method will return null.
     */
    public static String convertToString(Object fromInstance)
    {
        return instance.convert(fromInstance, String.class);
    }

    /**
     * Convert from the passed in instance to a BigDecimal.  If null or "" is passed in, this method will return a
     * BigDecimal with the value of 0.
     */
    public static BigDecimal convert2BigDecimal(Object fromInstance)
    {
        if (fromInstance == null) {
            return BigDecimal.ZERO;
        }
        return instance.convert(fromInstance, BigDecimal.class);
    }

    /**
     * Convert from the passed in instance to a BigDecimal.  If null is passed in, this method will return null.  If ""
     * is passed in, this method will return a BigDecimal with the value of 0. 
     */
    public static BigDecimal convertToBigDecimal(Object fromInstance)
    {
        return instance.convert(fromInstance, BigDecimal.class);
    }

    /**
     * Convert from the passed in instance to a BigInteger.  If null or "" is passed in, this method will return a
     * BigInteger with the value of 0.
     */
    public static BigInteger convert2BigInteger(Object fromInstance)
    {
        if (fromInstance == null) {
            return BigInteger.ZERO;
        }
        return instance.convert(fromInstance, BigInteger.class);
    }

    /**
     * Convert from the passed in instance to a BigInteger.  If null is passed in, this method will return null.  If ""
     * is passed in, this method will return a BigInteger with the value of 0.
     */
    public static BigInteger convertToBigInteger(Object fromInstance)
    {
        return instance.convert(fromInstance, BigInteger.class);
    }

    /**
     * Convert from the passed in instance to a java.sql.Date.  If null is passed in, this method will return null.
     */
    public static java.sql.Date convertToSqlDate(Object fromInstance)
    {
        return instance.convert(fromInstance, java.sql.Date.class);
    }

    /**
     * Convert from the passed in instance to a Timestamp.  If null is passed in, this method will return null.
     */
    public static Timestamp convertToTimestamp(Object fromInstance)
    {
        return instance.convert(fromInstance, Timestamp.class);
    }

    /**
     * Convert from the passed in instance to a Date.  If null is passed in, this method will return null.
     */
    public static Date convertToDate(Object fromInstance)
    {
        return instance.convert(fromInstance, Date.class);
    }

    /**
     * Convert from the passed in instance to a LocalDate.  If null is passed in, this method will return null.
     */
    public static LocalDate convertToLocalDate(Object fromInstance)
    {
        return instance.convert(fromInstance, LocalDate.class);
    }

    /**
     * Convert from the passed in instance to a LocalDateTime.  If null is passed in, this method will return null.
     */
    public static LocalDateTime convertToLocalDateTime(Object fromInstance)
    {
        return instance.convert(fromInstance, LocalDateTime.class);
    }

    /**
     * Convert from the passed in instance to a Date.  If null is passed in, this method will return null.
     */
    public static ZonedDateTime convertToZonedDateTime(Object fromInstance)
    {
        return instance.convert(fromInstance, ZonedDateTime.class);
    }

    /**
     * Convert from the passed in instance to a Calendar.  If null is passed in, this method will return null.
     */
    public static Calendar convertToCalendar(Object fromInstance)
    {
        return convert(fromInstance, Calendar.class);
    }

    /**
     * Convert from the passed in instance to a char.  If null is passed in, (char) 0 is returned.
     */
    public static char convert2char(Object fromInstance)
    {
        if (fromInstance == null) {
            return 0;
        }
        return instance.convert(fromInstance, char.class);
    }

    /**
     * Convert from the passed in instance to a Character.  If null is passed in, null is returned.
     */
    public static Character convertToCharacter(Object fromInstance)
    {
        return instance.convert(fromInstance, Character.class);
    }

    /**
     * Convert from the passed in instance to a byte.  If null is passed in, (byte) 0 is returned.
     */
    public static byte convert2byte(Object fromInstance)
    {
        if (fromInstance == null) {
            return 0;
        }
        return instance.convert(fromInstance, byte.class);
    }

    /**
     * Convert from the passed in instance to a Byte.  If null is passed in, null is returned.
     */
    public static Byte convertToByte(Object fromInstance)
    {
        return instance.convert(fromInstance, Byte.class);
    }

    /**
     * Convert from the passed in instance to a short.  If null is passed in, (short) 0 is returned.
     */
    public static short convert2short(Object fromInstance)
    {
        if (fromInstance == null) {
            return 0;
        }
        return instance.convert(fromInstance, short.class);
    }

    /**
     * Convert from the passed in instance to a Short.  If null is passed in, null is returned.
     */
    public static Short convertToShort(Object fromInstance)
    {
        return instance.convert(fromInstance, Short.class);
    }

    /**
     * Convert from the passed in instance to an int.  If null is passed in, (int) 0 is returned.
     */
    public static int convert2int(Object fromInstance)
    {
        if (fromInstance == null) {
            return 0;
        }
        return instance.convert(fromInstance, int.class);
    }

    /**
     * Convert from the passed in instance to an Integer.  If null is passed in, null is returned.
     */
    public static Integer convertToInteger(Object fromInstance)
    {
        return instance.convert(fromInstance, Integer.class);
    }

    /**
     * Convert from the passed in instance to an long.  If null is passed in, (long) 0 is returned.
     */
    public static long convert2long(Object fromInstance)
    {
        if (fromInstance == null) {
            return CommonValues.LONG_ZERO;
        }
        return instance.convert(fromInstance, long.class);
    }

    /**
     * Convert from the passed in instance to a Long.  If null is passed in, null is returned.
     */
    public static Long convertToLong(Object fromInstance)
    {
        return instance.convert(fromInstance, Long.class);
    }

    /**
     * Convert from the passed in instance to a float.  If null is passed in, 0.0f is returned.
     */
    public static float convert2float(Object fromInstance)
    {
        if (fromInstance == null) {
            return CommonValues.FLOAT_ZERO;
        }
        return instance.convert(fromInstance, float.class);
    }

    /**
     * Convert from the passed in instance to a Float.  If null is passed in, null is returned.
     */
    public static Float convertToFloat(Object fromInstance)
    {
        return instance.convert(fromInstance, Float.class);
    }

    /**
     * Convert from the passed in instance to a double.  If null is passed in, 0.0d is returned.
     */
    public static double convert2double(Object fromInstance)
    {
        if (fromInstance == null) {
            return CommonValues.DOUBLE_ZERO;
        }
        return instance.convert(fromInstance, double.class);
    }

    /**
     * Convert from the passed in instance to a Double.  If null is passed in, null is returned.
     */
    public static Double convertToDouble(Object fromInstance)
    {
        return instance.convert(fromInstance, Double.class);
    }

    /**
     * Convert from the passed in instance to a boolean.  If null is passed in, false is returned.
     */
    public static boolean convert2boolean(Object fromInstance)
    {
        if (fromInstance == null) {
            return false;
        }
        return instance.convert(fromInstance, boolean.class);
    }

    /**
     * Convert from the passed in instance to a Boolean.  If null is passed in, null is returned.
     */
    public static Boolean convertToBoolean(Object fromInstance)
    {
        return instance.convert(fromInstance, Boolean.class);
    }

    /**
     * Convert from the passed in instance to an AtomicInteger.  If null is passed in, a new AtomicInteger(0) is
     * returned.
     */
    public static AtomicInteger convert2AtomicInteger(Object fromInstance)
    {
        if (fromInstance == null) {
            return new AtomicInteger(0);
        }
        return instance.convert(fromInstance, AtomicInteger.class);
    }

    /**
     * Convert from the passed in instance to an AtomicInteger.  If null is passed in, null is returned.
     */
    public static AtomicInteger convertToAtomicInteger(Object fromInstance)
    {
        return instance.convert(fromInstance, AtomicInteger.class);
    }

    /**
     * Convert from the passed in instance to an AtomicLong.  If null is passed in, new AtomicLong(0L) is returned.
     */
    public static AtomicLong convert2AtomicLong(Object fromInstance)
    {
        if (fromInstance == null) {
            return new AtomicLong(0);
        }
        return instance.convert(fromInstance, AtomicLong.class);
    }

    /**
     * Convert from the passed in instance to an AtomicLong.  If null is passed in, null is returned.
     */
    public static AtomicLong convertToAtomicLong(Object fromInstance)
    {
        return instance.convert(fromInstance, AtomicLong.class);
    }

    /**
     * Convert from the passed in instance to an AtomicBoolean.  If null is passed in, new AtomicBoolean(false) is
     * returned.
     */
    public static AtomicBoolean convert2AtomicBoolean(Object fromInstance)
    {
        if (fromInstance == null) {
            return new AtomicBoolean(false);
        }
        return instance.convert(fromInstance, AtomicBoolean.class);
    }

    /**
     * Convert from the passed in instance to an AtomicBoolean.  If null is passed in, null is returned.
     */
    public static AtomicBoolean convertToAtomicBoolean(Object fromInstance)
    {
        return instance.convert(fromInstance, AtomicBoolean.class);
    }
    
    /**
     * No longer needed - use convert(localDate, long.class)
     * @param localDate A Java LocalDate
     * @return a long representing the localDate as epoch milliseconds (since 1970 Jan 1 at midnight)
     * @deprecated  replaced by convert(localDate, long.class)
     */
    @Deprecated
    public static long localDateToMillis(LocalDate localDate)
    {
        return instance.convert(localDate, long.class);
    }

    /**
     * No longer needed - use convert(localDateTime, long.class)
     * @param localDateTime A Java LocalDateTime
     * @return a long representing the localDateTime as epoch milliseconds (since 1970 Jan 1 at midnight)
     * @deprecated replaced by convert(localDateTime, long.class)
     */
    @Deprecated
    public static long localDateTimeToMillis(LocalDateTime localDateTime)
    {
        return instance.convert(localDateTime, long.class);
    }

    /**
     * No longer needed - use convert(ZonedDateTime, long.class)
     * @param zonedDateTime A Java ZonedDateTime
     * @return a long representing the ZonedDateTime as epoch milliseconds (since 1970 Jan 1 at midnight)
     * @deprecated replaced by convert(ZonedDateTime, long.class)
     */
    @Deprecated
    public static long zonedDateTimeToMillis(ZonedDateTime zonedDateTime)
    {
        return instance.convert(zonedDateTime, long.class);
    }
}
