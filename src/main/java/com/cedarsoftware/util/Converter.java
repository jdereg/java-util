package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.ConverterOptions;
import com.cedarsoftware.util.convert.DefaultConverterOptions;

/**
 * Handy conversion utilities.  Convert from primitive to other primitives, plus support for Date, TimeStamp SQL Date,
 * and the Atomic's.
 * <p>
 * `Converter.convert2*()` methods: If `null` passed in, primitive 'logical zero' is returned.
 *      Example: `Converter.convert(null, boolean.class)` returns `false`.
 * <p>
 * `Converter.convertTo*()` methods: if `null` passed in, `null` is returned.  Allows "tri-state" Boolean.
 *      Example: `Converter.convert(null, Boolean.class)` returns `null`.
 * <p>
 * `Converter.convert()` converts using `convertTo*()` methods for primitive wrappers, and
 *      `convert2*()` methods for primitives.
 * <p>
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


    @SuppressWarnings("unchecked")
    /**
     * Uses the default configuration options for you system.
     */
    public static <T> T convert(Object fromInstance, Class<T> toType) {
        return instance.convert(fromInstance, toType);
    }

    /**
     * Allows you to specify (each call) a different conversion options.  Useful so you don't have
     * to recreate the instance of Converter that is out there for every configuration option.  Just
     * provide a different set of CovnerterOptions on the call itself.
     */
    public static <T> T convert(Object fromInstance, Class<T> toType, ConverterOptions options) {
        return instance.convert(fromInstance, toType, options);
    }

    /**
     * Convert from the passed in instance to a String.  If null is passed in, this method will return "".
     * Possible inputs are any primitive or primitive wrapper, Date (returns ISO-DATE format: 2020-04-10T12:15:47),
     * Calendar (returns ISO-DATE format: 2020-04-10T12:15:47), any Enum (returns Enum's name()), BigDecimal,
     * BigInteger, AtomicBoolean, AtomicInteger, AtomicLong, and Character.
     */
    public static String convert2String(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return "";
        }
        return instance.convert(fromInstance, String.class);
    }

    /**
     * Convert from the passed in instance to a String.  If null is passed in, this method will return null.
     * Possible inputs are any primitive/primitive wrapper, Date (returns ISO-DATE format: 2020-04-10T12:15:47),
     * Calendar (returns ISO-DATE format: 2020-04-10T12:15:47), any Enum (returns Enum's name()), BigDecimal,
     * BigInteger, AtomicBoolean, AtomicInteger, AtomicLong, and Character.
     */
    @SuppressWarnings("unchecked")
    public static String convertToString(Object fromInstance)
    {
        return instance.convert(fromInstance, String.class);
    }

    /**
     * Convert from the passed in instance to a BigDecimal.  If null or "" is passed in, this method will return a
     * BigDecimal with the value of 0.  Possible inputs are String (base10 numeric values in string), BigInteger,
     * any primitive/primitive wrapper, Boolean/AtomicBoolean (returns BigDecimal of 0 or 1), Date/Calendar
     * (returns BigDecimal with the value of number of milliseconds since Jan 1, 1970), and Character (returns integer
     * value of character).
     */
    public static BigDecimal convert2BigDecimal(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return BigDecimal.ZERO;
        }
        return instance.convert(fromInstance, BigDecimal.class);
    }

    /**
     * Convert from the passed in instance to a BigDecimal.  If null is passed in, this method will return null.  If ""
     * is passed in, this method will return a BigDecimal with the value of 0.  Possible inputs are String (base10
     * numeric values in string), BigInteger, any primitive/primitive wrapper, Boolean/AtomicBoolean (returns
     * BigDecimal of 0 or 1), Date, Calendar, LocalDate, LocalDateTime, ZonedDateTime (returns BigDecimal with the
     * value of number of milliseconds since Jan 1, 1970), and Character (returns integer value of character).
     */
    public static BigDecimal convertToBigDecimal(Object fromInstance)
    {
        return instance.convert(fromInstance, BigDecimal.class);
    }

    /**
     * Convert from the passed in instance to a BigInteger.  If null or "" is passed in, this method will return a
     * BigInteger with the value of 0.  Possible inputs are String (base10 numeric values in string), BigDecimal,
     * any primitive/primitive wrapper, Boolean/AtomicBoolean (returns BigDecimal of 0 or 1), Date/Calendar
     * (returns BigDecimal with the value of number of milliseconds since Jan 1, 1970), and Character (returns integer
     * value of character).
     */
    public static BigInteger convert2BigInteger(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return BigInteger.ZERO;
        }
        return instance.convert(fromInstance, BigInteger.class);
    }

    /**
     * Convert from the passed in instance to a BigInteger.  If null is passed in, this method will return null.  If ""
     * is passed in, this method will return a BigInteger with the value of 0.  Possible inputs are String (base10
     * numeric values in string), BigDecimal, any primitive/primitive wrapper, Boolean/AtomicBoolean (returns
     * BigInteger of 0 or 1), Date, Calendar, LocalDate, LocalDateTime, ZonedDateTime (returns BigInteger with the value
     * of number of milliseconds since Jan 1, 1970), and Character (returns integer value of character).
     */
    public static BigInteger convertToBigInteger(Object fromInstance)
    {
        return instance.convert(fromInstance, BigInteger.class);
    }

    /**
     * Convert from the passed in instance to a java.sql.Date.  If null is passed in, this method will return null.
     * Possible inputs are TimeStamp, Date, Calendar, java.sql.Date (will return a copy), LocalDate, LocalDateTime,
     * ZonedDateTime, String (which will be parsed by DateUtilities into a Date and a java.sql.Date will created from that),
     * Long, BigInteger, BigDecimal, and AtomicLong (all of which the java.sql.Date will be created directly from
     * [number of milliseconds since Jan 1, 1970]).
     */
    public static java.sql.Date convertToSqlDate(Object fromInstance)
    {
        return instance.convert(fromInstance, java.sql.Date.class);
    }

    /**
     * Convert from the passed in instance to a Timestamp.  If null is passed in, this method will return null.
     * Possible inputs are java.sql.Date, Date, Calendar, LocalDate, LocalDateTime, ZonedDateTime, TimeStamp
     * (will return a copy), String (which will be parsed by DateUtilities into a Date and a Timestamp will created
     * from that), Long, BigInteger, BigDecimal, and AtomicLong (all of which the Timestamp will be created directly
     * from [number of milliseconds since Jan 1, 1970]).
     */
    public static Timestamp convertToTimestamp(Object fromInstance)
    {
        return instance.convert(fromInstance, Timestamp.class);
    }

    /**
     * Convert from the passed in instance to a Date.  If null is passed in, this method will return null.
     * Possible inputs are java.sql.Date, Timestamp, Calendar, Date (will return a copy), String (which will be parsed
     * by DateUtilities and returned as a new Date instance), Long, BigInteger, BigDecimal, and AtomicLong (all of
     * which the Date will be created directly from [number of milliseconds since Jan 1, 1970]).
     */
    public static Date convertToDate(Object fromInstance)
    {
        return instance.convert(fromInstance, Date.class);
    }

    public static LocalDate convertToLocalDate(Object fromInstance)
    {
        return instance.convert(fromInstance, LocalDate.class);
    }

    public static LocalDateTime convertToLocalDateTime(Object fromInstance)
    {
        return instance.convert(fromInstance, LocalDateTime.class);
    }

    public static ZonedDateTime convertToZonedDateTime(Object fromInstance)
    {
        return instance.convert(fromInstance, ZonedDateTime.class);
    }

    /**
     * Convert from the passed in instance to a Calendar.  If null is passed in, this method will return null.
     * Possible inputs are java.sql.Date, Timestamp, Date, Calendar (will return a copy), String (which will be parsed
     * by DateUtilities and returned as a new Date instance), Long, BigInteger, BigDecimal, and AtomicLong (all of
     * which the Date will be created directly from [number of milliseconds since Jan 1, 1970]).
     */
    public static Calendar convertToCalendar(Object fromInstance)
    {
        return convert(fromInstance, Calendar.class);
    }

    /**
     * Convert from the passed in instance to a char.  If null is passed in, (char) 0 is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static char convert2char(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return 0;
        }
        return instance.convert(fromInstance, char.class);
    }

    /**
     * Convert from the passed in instance to a Character.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Character convertToCharacter(Object fromInstance)
    {
        return instance.convert(fromInstance, Character.class);
    }

    /**
     * Convert from the passed in instance to a byte.  If null is passed in, (byte) 0 is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static byte convert2byte(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return 0;
        }
        return instance.convert(fromInstance, byte.class);
    }

    /**
     * Convert from the passed in instance to a Byte.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Byte convertToByte(Object fromInstance)
    {
        return instance.convert(fromInstance, Byte.class);
    }

    /**
     * Convert from the passed in instance to a short.  If null is passed in, (short) 0 is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static short convert2short(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return 0;
        }
        return instance.convert(fromInstance, short.class);
    }

    /**
     * Convert from the passed in instance to a Short.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Short convertToShort(Object fromInstance)
    {
        return instance.convert(fromInstance, Short.class);
    }

    /**
     * Convert from the passed in instance to an int.  If null is passed in, (int) 0 is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static int convert2int(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return 0;
        }
        return instance.convert(fromInstance, int.class);
    }

    /**
     * Convert from the passed in instance to an Integer.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Integer convertToInteger(Object fromInstance)
    {
        return instance.convert(fromInstance, Integer.class);
    }

    /**
     * Convert from the passed in instance to an long.  If null is passed in, (long) 0 is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.  In
     * addition, Date, LocalDate, LocalDateTime, ZonedDateTime, java.sql.Date, Timestamp, and Calendar can be passed in,
     * in which case the long returned is the number of milliseconds since Jan 1, 1970.
     */
    public static long convert2long(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return CommonValues.LONG_ZERO;
        }
        return instance.convert(fromInstance, long.class);
    }

    /**
     * Convert from the passed in instance to a Long.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.  In
     * addition, Date, LocalDate, LocalDateTime, ZonedDateTime, java.sql.Date, Timestamp, and Calendar can be passed in,
     * in which case the long returned is the number of milliseconds since Jan 1, 1970.
     */
    public static Long convertToLong(Object fromInstance)
    {
        return instance.convert(fromInstance, Long.class);
    }

    /**
     * Convert from the passed in instance to a float.  If null is passed in, 0.0f is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static float convert2float(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return CommonValues.FLOAT_ZERO;
        }
        return instance.convert(fromInstance, float.class);
    }

    /**
     * Convert from the passed in instance to a Float.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.  
     */
    public static Float convertToFloat(Object fromInstance)
    {
        return instance.convert(fromInstance, Float.class);
    }

    /**
     * Convert from the passed in instance to a double.  If null is passed in, 0.0d is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static double convert2double(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return CommonValues.DOUBLE_ZERO;
        }
        return instance.convert(fromInstance, double.class);
    }

    /**
     * Convert from the passed in instance to a Double.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Double convertToDouble(Object fromInstance)
    {
        return instance.convert(fromInstance, Double.class);
    }

    /**
     * Convert from the passed in instance to a boolean.  If null is passed in, false is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static boolean convert2boolean(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return false;
        }
        return instance.convert(fromInstance, boolean.class);
    }

    /**
     * Convert from the passed in instance to a Boolean.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Boolean convertToBoolean(Object fromInstance)
    {
        return instance.convert(fromInstance, Boolean.class);
    }

    /**
     * Convert from the passed in instance to an AtomicInteger.  If null is passed in, a new AtomicInteger(0) is
     * returned. Possible inputs are String, all primitive/primitive wrappers, boolean, AtomicBoolean,
     * (false=0, true=1), and all Atomic*s.
     */
    public static AtomicInteger convert2AtomicInteger(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return new AtomicInteger(0);
        }
        return instance.convert(fromInstance, AtomicInteger.class);
    }

    /**
     * Convert from the passed in instance to an AtomicInteger.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static AtomicInteger convertToAtomicInteger(Object fromInstance)
    {
        return instance.convert(fromInstance, AtomicInteger.class);
    }

    /**
     * Convert from the passed in instance to an AtomicLong.  If null is passed in, new AtomicLong(0L) is returned.
     * Possible inputs are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and
     * all Atomic*s.  In addition, Date, LocalDate, LocalDateTime, ZonedDateTime, java.sql.Date, Timestamp, and Calendar
     * can be passed in, in which case the AtomicLong returned is the number of milliseconds since Jan 1, 1970.
     */
    public static AtomicLong convert2AtomicLong(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return new AtomicLong(0);
        }
        return instance.convert(fromInstance, AtomicLong.class);
    }

    /**
     * Convert from the passed in instance to an AtomicLong.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.  In
     * addition, Date, LocalDate, LocalDateTime, ZonedDateTime, java.sql.Date, Timestamp, and Calendar can be passed in,
     * in which case the AtomicLong returned is the number of milliseconds since Jan 1, 1970.
     */
    public static AtomicLong convertToAtomicLong(Object fromInstance)
    {
        return instance.convert(fromInstance, AtomicLong.class);
    }

    /**
     * Convert from the passed in instance to an AtomicBoolean.  If null is passed in, new AtomicBoolean(false) is
     * returned. Possible inputs are String, all primitive/primitive wrappers, boolean, AtomicBoolean,
     * (false=0, true=1), and all Atomic*s.
     */
    public static AtomicBoolean convert2AtomicBoolean(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return new AtomicBoolean(false);
        }
        return instance.convert(fromInstance, AtomicBoolean.class);
    }

    /**
     * Convert from the passed in instance to an AtomicBoolean.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static AtomicBoolean convertToAtomicBoolean(Object fromInstance)
    {
        return instance.convert(fromInstance, AtomicBoolean.class);
    }
    
    /**
     * @param localDate A Java LocalDate
     * @return a long representing the localDate as the number of milliseconds since the
     * number of milliseconds since Jan 1, 1970
     */
    public static long localDateToMillis(LocalDate localDate)
    {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * @param localDateTime A Java LocalDateTime
     * @return a long representing the localDateTime as the number of milliseconds since the
     * number of milliseconds since Jan 1, 1970
     */
    public static long localDateTimeToMillis(LocalDateTime localDateTime)
    {
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * @param zonedDateTime A Java ZonedDateTime
     * @return a long representing the zonedDateTime as the number of milliseconds since the
     * number of milliseconds since Jan 1, 1970
     */
    public static long zonedDateTimeToMillis(ZonedDateTime zonedDateTime)
    {
        return zonedDateTime.toInstant().toEpochMilli();
    }
}
