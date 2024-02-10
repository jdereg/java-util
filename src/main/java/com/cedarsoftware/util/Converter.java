package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.Convert;
import com.cedarsoftware.util.convert.DefaultConverterOptions;

/**
 * Useful conversion utilities.  Convert from primitive to other primitives, primitives to java.time and the older
 * java.util.Date, TimeStamp SQL Date, and Calendar classes.  Support is there for the Atomics, BigInteger, BigDecimal,
 * String, Map, all the Java temporal (java.time) classes, and Object[] to Collection types. In addition, you can add
 * your own source/target pairings, and supply the lambda that performs the conversion.<br>
 * <br>
 * Use the 'getSupportedConversions()' API to see all conversion supported - from all sources
 * to all destinations per each source.  Close to 500 "out-of-the-box" conversions ship with the library.<br>
 * <br>
 * The Converter can be used as statically or as an instance.  See the public static methods on this Converter class
 * to use statically.  Any added conversions will added to a singleton instance maintained inside this class.
 * Alternatively, you can instantiate the Converter class to get an instance, and the conversions you add, remove, or
 * change will be scoped to just that instance. <br>
 * <br>
 * On this static Convert class:<br>
 * `Converter.convert2*()` methods: If `null` passed in, primitive 'logical zero' is returned.
 *      Example: `Converter.convert(null, boolean.class)` returns `false`.<br>
 * <br>
 * `Converter.convertTo*()` methods: if `null` passed in, `null` is returned.  Allows "tri-state" Boolean, for example.
 *      Example: `Converter.convert(null, Boolean.class)` returns `null`.<br>
 * <br>
 * `Converter.convert()` converts using `convertTo*()` methods for primitive wrappers, and
 *      `convert2*()` methods for primitives. <br>
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
public final class Converter
{
    public static com.cedarsoftware.util.convert.Converter instance =
            new com.cedarsoftware.util.convert.Converter(new DefaultConverterOptions());

    /**
     * Static utility class.
     */
    private Converter() { }

    /**
     * Uses the default configuration options for your system.
     */
    public static <T> T convert(Object fromInstance, Class<T> toType) {
        return instance.convert(fromInstance, toType);
    }
    
    /**
     * Check to see if a conversion from type to another type is supported (may use inheritance via super classes/interfaces).
     *
     * @param source Class of source type.
     * @param target Class of target type.
     * @return boolean true if the Converter converts from the source type to the destination type, false otherwise.
     */
    public static boolean isConversionSupportedFor(Class<?> source, Class<?> target) {
        return instance.isConversionSupportedFor(source, target);
    }

    /**
     * @return Map<Class, Set < Class>> which contains all supported conversions. The key of the Map is a source class,
     * and the Set contains all the target types (classes) that the source can be converted to.
     */
    public static Map<Class<?>, Set<Class<?>>> allSupportedConversions() {
        return instance.allSupportedConversions();
    }

    /**
     * @return Map<String, Set < String>> which contains all supported conversions. The key of the Map is a source class
     * name, and the Set contains all the target class names that the source can be converted to.
     */
    public static Map<String, Set<String>> getSupportedConversions() {
        return instance.getSupportedConversions();
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
     */
    @Deprecated
    public static long zonedDateTimeToMillis(ZonedDateTime zonedDateTime)
    {
        return instance.convert(zonedDateTime, long.class);
    }
}
