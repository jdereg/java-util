package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handy conversion utilities.  Convert from primitive to other primitives, plus support for Date, TimeStamp SQL Date,
 * and the Atomic's.
 *
 * `Converter.convert2*()` methods: If `null` passed in, primitive 'logical zero' is returned.
 *      Example: `Converter.convert(null, boolean.class)` returns `false`.
 *
 * `Converter.convertTo*()` methods: if `null` passed in, `null` is returned.  Allows "tri-state" Boolean.
 *      Example: `Converter.convert(null, Boolean.class)` returns `null`.
 *
 * `Converter.convert()` converts using `convertTo*()` methods for primitive wrappers, and
 *      `convert2*()` methods for primitives.
 *      
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public final class Converter
{
    public static final Byte BYTE_ZERO = (byte)0;
    public static final Byte BYTE_ONE = (byte)1;
    public static final Short SHORT_ZERO = (short)0;
    public static final Short SHORT_ONE = (short)1;
    public static final Integer INTEGER_ZERO = 0;
    public static final Integer INTEGER_ONE = 1;
    public static final Long LONG_ZERO = 0L;
    public static final Long LONG_ONE = 1L;
    public static final Float FLOAT_ZERO = 0.0f;
    public static final Float FLOAT_ONE = 1.0f;
    public static final Double DOUBLE_ZERO = 0.0d;
    public static final Double DOUBLE_ONE = 1.0d;
    public static final BigDecimal BIG_DECIMAL_ZERO = BigDecimal.ZERO;
    public static final BigInteger BIG_INTEGER_ZERO = BigInteger.ZERO;
    private static final Map<Class<?>, Work> conversion = new HashMap<>();
    private static final Map<Class<?>, Work> conversionToString = new HashMap<>();
    
    private interface Work<T>
    {
        Object convert(T fromInstance);
    }
    
    static
    {
        conversion.put(String.class, Converter::convertToString);
        conversion.put(long.class, Converter::convert2long);
        conversion.put(Long.class, Converter::convertToLong);
        conversion.put(int.class, Converter::convert2int);
        conversion.put(Integer.class, Converter::convertToInteger);
        conversion.put(short.class, Converter::convert2short);
        conversion.put(Short.class, Converter::convertToShort);
        conversion.put(byte.class, Converter::convert2byte);
        conversion.put(Byte.class, Converter::convertToByte);
        conversion.put(char.class, Converter::convert2char);
        conversion.put(boolean.class, Converter::convert2boolean);
        conversion.put(Boolean.class, Converter::convertToBoolean);
        conversion.put(double.class, Converter::convert2double);
        conversion.put(Double.class, Converter::convertToDouble);
        conversion.put(float.class, Converter::convert2float);
        conversion.put(Float.class, Converter::convertToFloat);
        conversion.put(Character.class, Converter::convertToCharacter);
        conversion.put(Calendar.class, Converter::convertToCalendar);
        conversion.put(Date.class, Converter::convertToDate);
        conversion.put(LocalDate.class, Converter::convertToLocalDate);
        conversion.put(LocalDateTime.class, Converter::convertToLocalDateTime);
        conversion.put(ZonedDateTime.class, Converter::convertToZonedDateTime);
        conversion.put(BigDecimal.class, Converter::convertToBigDecimal);
        conversion.put(BigInteger.class, Converter::convertToBigInteger);
        conversion.put(java.sql.Date.class, Converter::convertToSqlDate);
        conversion.put(Timestamp.class, Converter::convertToTimestamp);
        conversion.put(AtomicInteger.class, Converter::convertToAtomicInteger);
        conversion.put(AtomicLong.class, Converter::convertToAtomicLong);
        conversion.put(AtomicBoolean.class, Converter::convertToAtomicBoolean);

        conversionToString.put(String.class, fromInstance -> fromInstance);
        conversionToString.put(BigDecimal.class, fromInstance -> {
            BigDecimal bd = convertToBigDecimal(fromInstance);
            return bd.stripTrailingZeros().toPlainString();
        });
        conversionToString.put(BigInteger.class, fromInstance -> {
            BigInteger bi = convertToBigInteger(fromInstance);
            return bi.toString();
        });
        Work<?> toString = Object::toString;
        conversionToString.put(Boolean.class, toString);
        conversionToString.put(AtomicBoolean.class, toString);
        conversionToString.put(Byte.class, toString);
        conversionToString.put(Short.class, toString);
        conversionToString.put(Integer.class, toString);
        conversionToString.put(AtomicInteger.class, toString);
        conversionToString.put(Long.class, toString);
        conversionToString.put(AtomicLong.class, toString);

        // Should eliminate possibility of 'e' (exponential) notation
        Work<?> toNoExpString = Object::toString;
        conversionToString.put(Double.class, toNoExpString);
        conversionToString.put(Float.class, toNoExpString);

        conversionToString.put(Date.class, fromInstance -> SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(fromInstance));
        conversionToString.put(Character.class, fromInstance -> "" + fromInstance);
    }

    /**
     * Static utility class.
     */
    private Converter() { }

    /**
     * Turn the passed in value to the class indicated.  This will allow, for
     * example, a String value to be passed in and have it coerced to a Long.
     * <pre>
     *     Examples:
     *     Long x = convert("35", Long.class);
     *     Date d = convert("2015/01/01", Date.class)
     *     int y = convert(45.0, int.class)
     *     String date = convert(date, String.class)
     *     String date = convert(calendar, String.class)
     *     Short t = convert(true, short.class);     // returns (short) 1 or  (short) 0
     *     Long date = convert(calendar, long.class); // get calendar's time into long
     * </pre>
     * @param fromInstance A value used to create the targetType, even though it may
     * not (most likely will not) be the same data type as the targetType
     * @param toType Class which indicates the targeted (final) data type.
     * Please note that in addition to the 8 Java primitives, the targeted class
     * can also be Date.class, String.class, BigInteger.class, BigDecimal.class, and
     * the Atomic classes.  The primitive class can be either primitive class or primitive
     * wrapper class, however, the returned value will always [obviously] be a primitive
     * wrapper.
     * @return An instanceof targetType class, based upon the value passed in.
     */
    public static <T> T convert(Object fromInstance, Class<T> toType)
    {
        if (toType == null)
        {
            throw new IllegalArgumentException("Type cannot be null in Converter.convert(value, type)");
        }

        Work work = conversion.get(toType);
        if (work != null)
        {
            return (T) work.convert(fromInstance);
        }
        throw new IllegalArgumentException("Unsupported type '" + toType.getName() + "' for conversion");
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
        return convertToString(fromInstance);
    }

    /**
     * Convert from the passed in instance to a String.  If null is passed in, this method will return null.
     * Possible inputs are any primitive/primitive wrapper, Date (returns ISO-DATE format: 2020-04-10T12:15:47),
     * Calendar (returns ISO-DATE format: 2020-04-10T12:15:47), any Enum (returns Enum's name()), BigDecimal,
     * BigInteger, AtomicBoolean, AtomicInteger, AtomicLong, and Character.
     */
    public static String convertToString(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return null;
        }
        Class<?> clazz = fromInstance.getClass();
        Work work = conversionToString.get(clazz);
        if (work != null)
        {
            return (String) work.convert(fromInstance);
        }
        else if (fromInstance instanceof Calendar)
        {   // Done this way (as opposed to putting a closure in conversionToString) because Calendar.class is not == to GregorianCalendar.class
            return SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(((Calendar)fromInstance).getTime());
        }
        else if (fromInstance instanceof Enum)
        {
            return ((Enum)fromInstance).name();
        }
        return nope(fromInstance, "String");
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
            return BIG_DECIMAL_ZERO;
        }
        return convertToBigDecimal(fromInstance);
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
        try
        {
            if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return BigDecimal.ZERO;
                }
                return new BigDecimal(((String) fromInstance).trim());
            }
            else if (fromInstance instanceof BigDecimal)
            {
                return (BigDecimal)fromInstance;
            }
            else if (fromInstance instanceof BigInteger)
            {
                return new BigDecimal((BigInteger) fromInstance);
            }
            else if (fromInstance instanceof Long)
            {
                return new BigDecimal((Long)fromInstance);
            }
            else if (fromInstance instanceof AtomicLong)
            {
                return new BigDecimal(((AtomicLong) fromInstance).get());
            }
            else if (fromInstance instanceof Number)
            {
                return new BigDecimal(String.valueOf(fromInstance));
            }
            else if (fromInstance instanceof Boolean)
            {
                return (Boolean) fromInstance ? BigDecimal.ONE : BigDecimal.ZERO;
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean) fromInstance).get() ? BigDecimal.ONE : BigDecimal.ZERO;
            }
            else if (fromInstance instanceof Date)
            {
                return new BigDecimal(((Date)fromInstance).getTime());
            }
            else if (fromInstance instanceof LocalDate)
            {
                return new BigDecimal(localDateToMillis((LocalDate)fromInstance));
            }
            else if (fromInstance instanceof LocalDateTime)
            {
                return new BigDecimal(localDateTimeToMillis((LocalDateTime)fromInstance));
            }
            else if (fromInstance instanceof ZonedDateTime)
            {
                return new BigDecimal(zonedDateTimeToMillis((ZonedDateTime)fromInstance));
            }
            else if (fromInstance instanceof Calendar)
            {
                return new BigDecimal(((Calendar)fromInstance).getTime().getTime());
            }
            else if (fromInstance instanceof Character)
            {
                return new BigDecimal(((Character)fromInstance));
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'BigDecimal'", e);
        }
        nope(fromInstance, "BigDecimal");
        return null;
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
            return BIG_INTEGER_ZERO;
        }
        return convertToBigInteger(fromInstance);
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
        try
        {
            if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return BigInteger.ZERO;
                }
                return new BigInteger(((String) fromInstance).trim());
            }
            else if (fromInstance instanceof BigInteger)
            {
                return (BigInteger) fromInstance;
            }
            else if (fromInstance instanceof BigDecimal)
            {
                return ((BigDecimal) fromInstance).toBigInteger();
            }
            else if (fromInstance instanceof Number)
            {
                return new BigInteger(Long.toString(((Number) fromInstance).longValue()));
            }
            else if (fromInstance instanceof Boolean)
            {
                return (Boolean) fromInstance ? BigInteger.ONE : BigInteger.ZERO;
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean) fromInstance).get() ? BigInteger.ONE : BigInteger.ZERO;
            }
            else if (fromInstance instanceof Date)
            {
                return new BigInteger(Long.toString(((Date) fromInstance).getTime()));
            }
            else if (fromInstance instanceof LocalDate)
            {
                return BigInteger.valueOf(localDateToMillis((LocalDate)fromInstance));
            }
            else if (fromInstance instanceof LocalDateTime)
            {
                return BigInteger.valueOf(localDateTimeToMillis((LocalDateTime)fromInstance));
            }
            else if (fromInstance instanceof ZonedDateTime)
            {
                return BigInteger.valueOf(zonedDateTimeToMillis((ZonedDateTime) fromInstance));
            }
            else if (fromInstance instanceof Calendar)
            {
                return new BigInteger(Long.toString(((Calendar) fromInstance).getTime().getTime()));
            }
            else if (fromInstance instanceof Character)
            {
                return new BigInteger(Long.toString(((Character)fromInstance)));
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'BigInteger'", e);
        }
        nope(fromInstance, "BigInteger");
        return null;
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
        try
        {
            if (fromInstance instanceof java.sql.Date)
            {   // Return a clone of the current date time because java.sql.Date is mutable.
                return new java.sql.Date(((java.sql.Date)fromInstance).getTime());
            }
            else if (fromInstance instanceof Timestamp)
            {
                Timestamp timestamp = (Timestamp) fromInstance;
                return new java.sql.Date(timestamp.getTime());
            }
            else if (fromInstance instanceof Date)
            {   // convert from java.util.Date to java.sql.Date
                return new java.sql.Date(((Date)fromInstance).getTime());
            }
            else if (fromInstance instanceof String)
            {
                Date date = DateUtilities.parseDate(((String) fromInstance).trim());
                if (date == null)
                {
                    return null;
                }
                return new java.sql.Date(date.getTime());
            }
            else if (fromInstance instanceof LocalDate)
            {
                return new java.sql.Date(localDateToMillis((LocalDate)fromInstance));
            }
            else if (fromInstance instanceof LocalDateTime)
            {
                return new java.sql.Date(localDateTimeToMillis((LocalDateTime)fromInstance));
            }
            else if (fromInstance instanceof ZonedDateTime)
            {
                return new java.sql.Date(zonedDateTimeToMillis((ZonedDateTime)fromInstance));
            }
            else if (fromInstance instanceof Calendar)
            {
                return new java.sql.Date(((Calendar) fromInstance).getTime().getTime());
            }
            else if (fromInstance instanceof Long)
            {
                return new java.sql.Date((Long) fromInstance);
            }
            else if (fromInstance instanceof BigInteger)
            {
                return new java.sql.Date(((BigInteger)fromInstance).longValue());
            }
            else if (fromInstance instanceof BigDecimal)
            {
                return new java.sql.Date(((BigDecimal)fromInstance).longValue());
            }
            else if (fromInstance instanceof AtomicLong)
            {
                return new java.sql.Date(((AtomicLong) fromInstance).get());
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'java.sql.Date'", e);
        }
        nope(fromInstance, "java.sql.Date");
        return null;
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
        try
        {
            if (fromInstance instanceof java.sql.Date)
            {   // convert from java.sql.Date to java.util.Date
                return new Timestamp(((java.sql.Date)fromInstance).getTime());
            }
            else if (fromInstance instanceof Timestamp)
            {   // return a clone of the Timestamp because it is mutable
                return new Timestamp(((Timestamp)fromInstance).getTime());
            }
            else if (fromInstance instanceof Date)
            {
                return new Timestamp(((Date) fromInstance).getTime());
            }
            else if (fromInstance instanceof LocalDate)
            {
                return new Timestamp(localDateToMillis((LocalDate)fromInstance));
            }
            else if (fromInstance instanceof LocalDateTime)
            {
                return new Timestamp(localDateTimeToMillis((LocalDateTime)fromInstance));
            }
            else if (fromInstance instanceof ZonedDateTime)
            {
                return new Timestamp(zonedDateTimeToMillis((ZonedDateTime)fromInstance));
            }
            else if (fromInstance instanceof String)
            {
                Date date = DateUtilities.parseDate(((String) fromInstance).trim());
                if (date == null)
                {
                    return null;
                }
                return new Timestamp(date.getTime());
            }
            else if (fromInstance instanceof Calendar)
            {
                return new Timestamp(((Calendar) fromInstance).getTime().getTime());
            }
            else if (fromInstance instanceof Long)
            {
                return new Timestamp((Long) fromInstance);
            }
            else if (fromInstance instanceof BigInteger)
            {
                return new Timestamp(((BigInteger) fromInstance).longValue());
            }
            else if (fromInstance instanceof BigDecimal)
            {
                return new Timestamp(((BigDecimal) fromInstance).longValue());
            }
            else if (fromInstance instanceof AtomicLong)
            {
                return new Timestamp(((AtomicLong) fromInstance).get());
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Timestamp'", e);
        }
        nope(fromInstance, "Timestamp");
        return null;
    }

    /**
     * Convert from the passed in instance to a Date.  If null is passed in, this method will return null.
     * Possible inputs are java.sql.Date, Timestamp, Calendar, Date (will return a copy), String (which will be parsed
     * by DateUtilities and returned as a new Date instance), Long, BigInteger, BigDecimal, and AtomicLong (all of
     * which the Date will be created directly from [number of milliseconds since Jan 1, 1970]).
     */
    public static Date convertToDate(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                return DateUtilities.parseDate(((String) fromInstance).trim());
            }
            else if (fromInstance instanceof java.sql.Date)
            {   // convert from java.sql.Date to java.util.Date
                return new Date(((java.sql.Date)fromInstance).getTime());
            }
            else if (fromInstance instanceof Timestamp)
            {
                Timestamp timestamp = (Timestamp) fromInstance;
                return new Date(timestamp.getTime());
            }
            else if (fromInstance instanceof Date)
            {   // Return a clone, not the same instance because Dates are not immutable
                return new Date(((Date)fromInstance).getTime());
            }
            else if (fromInstance instanceof LocalDate)
            {
                return new Date(localDateToMillis((LocalDate)fromInstance));
            }
            else if (fromInstance instanceof LocalDateTime)
            {
                return new Date(localDateTimeToMillis((LocalDateTime)fromInstance));
            }
            else if (fromInstance instanceof ZonedDateTime)
            {
                return new Date(zonedDateTimeToMillis((ZonedDateTime)fromInstance));
            }
            else if (fromInstance instanceof Calendar)
            {
                return ((Calendar) fromInstance).getTime();
            }
            else if (fromInstance instanceof Long)
            {
                return new Date((Long) fromInstance);
            }
            else if (fromInstance instanceof BigInteger)
            {
                return new Date(((BigInteger)fromInstance).longValue());
            }
            else if (fromInstance instanceof BigDecimal)
            {
                return new Date(((BigDecimal)fromInstance).longValue());
            }
            else if (fromInstance instanceof AtomicLong)
            {
                return new Date(((AtomicLong) fromInstance).get());
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Date'", e);
        }
        nope(fromInstance, "Date");
        return null;
    }

    public static LocalDate convertToLocalDate(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                Date date = DateUtilities.parseDate(((String) fromInstance).trim());
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            else if (fromInstance instanceof LocalDate)
            {   // return passed in instance (no need to copy, LocalDate is immutable)
                return (LocalDate) fromInstance;
            }
            else if (fromInstance instanceof LocalDateTime)
            {
                return ((LocalDateTime) fromInstance).toLocalDate();
            }
            else if (fromInstance instanceof ZonedDateTime)
            {
                return ((ZonedDateTime) fromInstance).toLocalDate();
            }
            else if (fromInstance instanceof java.sql.Date)
            {   
                return ((java.sql.Date) fromInstance).toLocalDate();
            }
            else if (fromInstance instanceof Timestamp)
            {
                return ((Timestamp) fromInstance).toLocalDateTime().toLocalDate();
            }
            else if (fromInstance instanceof Date)
            {   
                return ((Date) fromInstance).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            else if (fromInstance instanceof Calendar)
            {
                return ((Calendar) fromInstance).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            else if (fromInstance instanceof Long)
            {
                Long dateInMillis = (Long) fromInstance;
                return Instant.ofEpochMilli(dateInMillis).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            else if (fromInstance instanceof BigInteger)
            {
                BigInteger big = (BigInteger) fromInstance;
                return Instant.ofEpochMilli(big.longValue()).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            else if (fromInstance instanceof BigDecimal)
            {
                BigDecimal big = (BigDecimal) fromInstance;
                return Instant.ofEpochMilli(big.longValue()).atZone(ZoneId.systemDefault()).toLocalDate();
            }
            else if (fromInstance instanceof AtomicLong)
            {
                AtomicLong atomicLong = (AtomicLong) fromInstance;
                return Instant.ofEpochMilli(atomicLong.longValue()).atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'LocalDate'", e);
        }
        nope(fromInstance, "LocalDate");
        return null;
    }

    public static LocalDateTime convertToLocalDateTime(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                Date date = DateUtilities.parseDate(((String) fromInstance).trim());
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            else if (fromInstance instanceof LocalDate)
            {
                return ((LocalDate) fromInstance).atStartOfDay();
            }
            else if (fromInstance instanceof LocalDateTime)
            {   // return passed in instance (no need to copy, LocalDateTime is immutable)
                return ((LocalDateTime) fromInstance);
            }
            else if (fromInstance instanceof ZonedDateTime)
            {
                return ((ZonedDateTime) fromInstance).toLocalDateTime();
            }
            else if (fromInstance instanceof java.sql.Date)
            {
                return ((java.sql.Date) fromInstance).toLocalDate().atStartOfDay();
            }
            else if (fromInstance instanceof Timestamp)
            {
                return ((Timestamp) fromInstance).toLocalDateTime();
            }
            else if (fromInstance instanceof Date)
            {
                return ((Date) fromInstance).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            else if (fromInstance instanceof Calendar)
            {
                return ((Calendar) fromInstance).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            else if (fromInstance instanceof Long)
            {
                Long dateInMillis = (Long) fromInstance;
                return Instant.ofEpochMilli(dateInMillis).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            else if (fromInstance instanceof BigInteger)
            {
                BigInteger big = (BigInteger) fromInstance;
                return Instant.ofEpochMilli(big.longValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            else if (fromInstance instanceof BigDecimal)
            {
                BigDecimal big = (BigDecimal) fromInstance;
                return Instant.ofEpochMilli(big.longValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            else if (fromInstance instanceof AtomicLong)
            {
                AtomicLong atomicLong = (AtomicLong) fromInstance;
                return Instant.ofEpochMilli(atomicLong.longValue()).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'LocalDateTime'", e);
        }
        nope(fromInstance, "LocalDateTime");
        return null;
    }

    public static ZonedDateTime convertToZonedDateTime(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                Date date = DateUtilities.parseDate(((String) fromInstance).trim());
                return date.toInstant().atZone(ZoneId.systemDefault());
            }
            else if (fromInstance instanceof LocalDate)
            {
                return ((LocalDate)fromInstance).atStartOfDay(ZoneId.systemDefault());
            }
            else if (fromInstance instanceof LocalDateTime)
            {   // return passed in instance (no need to copy, LocalDateTime is immutable)
                return ((LocalDateTime) fromInstance).atZone(ZoneId.systemDefault());
            }
            else if (fromInstance instanceof ZonedDateTime)
            {   // return passed in instance (no need to copy, ZonedDateTime is immutable)
                return ((ZonedDateTime) fromInstance);
            }
            else if (fromInstance instanceof java.sql.Date)
            {
                return ((java.sql.Date) fromInstance).toInstant().atZone(ZoneId.systemDefault());
            }
            else if (fromInstance instanceof Timestamp)
            {
                return ((Timestamp) fromInstance).toInstant().atZone(ZoneId.systemDefault());
            }
            else if (fromInstance instanceof Date)
            {
                return ((Date) fromInstance).toInstant().atZone(ZoneId.systemDefault());
            }
            else if (fromInstance instanceof Calendar)
            {
                return ((Calendar) fromInstance).toInstant().atZone(ZoneId.systemDefault());
            }
            else if (fromInstance instanceof Long)
            {
                Long dateInMillis = (Long) fromInstance;
                return Instant.ofEpochMilli(dateInMillis).atZone(ZoneId.systemDefault());
            }
            else if (fromInstance instanceof BigInteger)
            {
                BigInteger big = (BigInteger) fromInstance;
                return Instant.ofEpochMilli(big.longValue()).atZone(ZoneId.systemDefault());
            }
            else if (fromInstance instanceof BigDecimal)
            {
                BigDecimal big = (BigDecimal) fromInstance;
                return Instant.ofEpochMilli(big.longValue()).atZone(ZoneId.systemDefault());
            }
            else if (fromInstance instanceof AtomicLong)
            {
                AtomicLong atomicLong = (AtomicLong) fromInstance;
                return Instant.ofEpochMilli(atomicLong.longValue()).atZone(ZoneId.systemDefault());
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'LocalDateTime'", e);
        }
        nope(fromInstance, "LocalDateTime");
        return null;
    }

    /**
     * Convert from the passed in instance to a Calendar.  If null is passed in, this method will return null.
     * Possible inputs are java.sql.Date, Timestamp, Date, Calendar (will return a copy), String (which will be parsed
     * by DateUtilities and returned as a new Date instance), Long, BigInteger, BigDecimal, and AtomicLong (all of
     * which the Date will be created directly from [number of milliseconds since Jan 1, 1970]).
     */
    public static Calendar convertToCalendar(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(convertToDate(fromInstance));
        return calendar;
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
        return convertToCharacter(fromInstance);
    }

    /**
     * Convert from the passed in instance to a Character.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Character convertToCharacter(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                if ("".equals(fromInstance))
                {
                    return 0;
                }
                return (char)Integer.parseInt(((String) fromInstance).trim());
            }
            else if (fromInstance instanceof Number)
            {
                return (char)((Number)fromInstance).shortValue();
            }
            else if (fromInstance instanceof Boolean)
            {
                return (boolean)fromInstance ? '1' : '0';
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean) fromInstance).get() ? '1' : '0';
            }
            else if (fromInstance instanceof Character)
            {
                return (Character)fromInstance;
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Character'", e);
        }
        nope(fromInstance, "Character");
        return null;
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
        return convertToByte(fromInstance);
    }

    /**
     * Convert from the passed in instance to a Byte.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Byte convertToByte(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return BYTE_ZERO;
                }
                try
                {
                    return Byte.valueOf(((String) fromInstance).trim());
                }
                catch (NumberFormatException e)
                {
                    long value = convertToBigDecimal(fromInstance).longValue();
                    if (value < -128 || value > 127)
                    {
                        throw new NumberFormatException("Value: " + fromInstance + " outside -128 to 127");
                    }
                    return (byte)value;
                }
            }
            else if (fromInstance instanceof Byte)
            {
                return (Byte)fromInstance;
            }
            else if (fromInstance instanceof Number)
            {
                return ((Number)fromInstance).byteValue();
            }
            else if (fromInstance instanceof Boolean)
            {
                return (Boolean) fromInstance ? BYTE_ONE : BYTE_ZERO;
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean)fromInstance).get() ? BYTE_ONE : BYTE_ZERO;
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Byte'", e);
        }
        nope(fromInstance, "Byte");
        return null;
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
        return convertToShort(fromInstance);
    }

    /**
     * Convert from the passed in instance to a Short.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Short convertToShort(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return SHORT_ZERO;
                }
                try
                {
                    return Short.valueOf(((String) fromInstance).trim());
                }
                catch (NumberFormatException e)
                {
                    long value = convertToBigDecimal(fromInstance).longValue();
                    if (value < -32768 || value > 32767)
                    {
                        throw new NumberFormatException("Value: " + fromInstance + " outside -32768 to 32767");
                    }
                    return (short) value;
                }
            }
            else if (fromInstance instanceof Short)
            {
                return (Short)fromInstance;
            }
            else if (fromInstance instanceof Number)
            {
                return ((Number)fromInstance).shortValue();
            }
            else if (fromInstance instanceof Boolean)
            {
                return (Boolean) fromInstance ? SHORT_ONE : SHORT_ZERO;
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean) fromInstance).get() ? SHORT_ONE : SHORT_ZERO;
            }
            else if (fromInstance instanceof Character)
            {
                return (short)((char)fromInstance);
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Short'", e);
        }
        nope(fromInstance, "Short");
        return null;
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
        return convertToInteger(fromInstance);
    }

    /**
     * Convert from the passed in instance to an Integer.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Integer convertToInteger(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof Integer)
            {
                return (Integer)fromInstance;
            }
            else if (fromInstance instanceof Number)
            {
                return ((Number)fromInstance).intValue();
            }
            else if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return INTEGER_ZERO;
                }
                try
                {
                    return Integer.valueOf(((String) fromInstance).trim());
                }
                catch (NumberFormatException e)
                {
                    long value = convertToBigDecimal(fromInstance).longValue();
                    if (value < -2147483648 || value > 2147483647)
                    {
                        throw new NumberFormatException("Value: " + fromInstance + " outside -2147483648 to 2147483647");
                    }
                    return (int) value;
                }
            }
            else if (fromInstance instanceof Boolean)
            {
                return (Boolean) fromInstance ? INTEGER_ONE : INTEGER_ZERO;
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean) fromInstance).get() ? INTEGER_ONE : INTEGER_ZERO;
            }
            else if (fromInstance instanceof Character)
            {
                return (int)((char)fromInstance);
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to an 'Integer'", e);
        }
        nope(fromInstance, "Integer");
        return null;
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
            return LONG_ZERO;
        }
        return convertToLong(fromInstance);
    }

    /**
     * Convert from the passed in instance to a Long.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.  In
     * addition, Date, LocalDate, LocalDateTime, ZonedDateTime, java.sql.Date, Timestamp, and Calendar can be passed in,
     * in which case the long returned is the number of milliseconds since Jan 1, 1970.
     */
    public static Long convertToLong(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof Long)
            {
                return (Long) fromInstance;
            }
            else if (fromInstance instanceof String)
            {
                if ("".equals(fromInstance))
                {
                    return LONG_ZERO;
                }
                try
                {
                    return Long.valueOf(((String) fromInstance).trim());
                }
                catch (NumberFormatException e)
                {
                    return convertToBigDecimal(fromInstance).longValue();
                }
            }
            else if (fromInstance instanceof Number)
            {
                return ((Number)fromInstance).longValue();
            }
            else if (fromInstance instanceof Boolean)
            {
                return (Boolean) fromInstance ? LONG_ONE : LONG_ZERO;
            }
            else if (fromInstance instanceof Date)
            {
                return ((Date)fromInstance).getTime();
            }
            else if (fromInstance instanceof LocalDate)
            {
                return localDateToMillis((LocalDate)fromInstance);
            }
            else if (fromInstance instanceof LocalDateTime)
            {
                return localDateTimeToMillis((LocalDateTime)fromInstance);
            }
            else if (fromInstance instanceof ZonedDateTime)
            {
                return zonedDateTimeToMillis((ZonedDateTime)fromInstance);
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean) fromInstance).get() ? LONG_ONE : LONG_ZERO;
            }
            else if (fromInstance instanceof Calendar)
            {
                return ((Calendar)fromInstance).getTime().getTime();
            }
            else if (fromInstance instanceof Character)
            {
                return (long)((char)fromInstance);
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Long'", e);
        }
        nope(fromInstance, "Long");
        return null;
    }

    /**
     * Convert from the passed in instance to a float.  If null is passed in, 0.0f is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static float convert2float(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return FLOAT_ZERO;
        }
        return convertToFloat(fromInstance);
    }

    /**
     * Convert from the passed in instance to a Float.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.  
     */
    public static Float convertToFloat(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return FLOAT_ZERO;
                }
                return Float.valueOf(((String) fromInstance).trim());
            }
            else if (fromInstance instanceof Float)
            {
                return (Float)fromInstance;
            }
            else if (fromInstance instanceof Number)
            {
                return ((Number)fromInstance).floatValue();
            }
            else if (fromInstance instanceof Boolean)
            {
                return (Boolean) fromInstance ? FLOAT_ONE : FLOAT_ZERO;
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean) fromInstance).get() ? FLOAT_ONE : FLOAT_ZERO;
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Float'", e);
        }
        nope(fromInstance, "Float");
        return null;
    }

    /**
     * Convert from the passed in instance to a double.  If null is passed in, 0.0d is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static double convert2double(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return DOUBLE_ZERO;
        }
        return convertToDouble(fromInstance);
    }

    /**
     * Convert from the passed in instance to a Double.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Double convertToDouble(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return DOUBLE_ZERO;
                }
                return Double.valueOf(((String) fromInstance).trim());
            }
            else if (fromInstance instanceof Double)
            {
                return (Double)fromInstance;
            }
            else if (fromInstance instanceof Number)
            {
                return ((Number)fromInstance).doubleValue();
            }
            else if (fromInstance instanceof Boolean)
            {
                return (Boolean) fromInstance ? DOUBLE_ONE : DOUBLE_ZERO;
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean) fromInstance).get() ? DOUBLE_ONE : DOUBLE_ZERO;
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Double'", e);
        }
        nope(fromInstance, "Double");
        return null;
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
        return convertToBoolean(fromInstance);
    }

    /**
     * Convert from the passed in instance to a Boolean.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static Boolean convertToBoolean(Object fromInstance)
    {
        if (fromInstance instanceof Boolean)
        {
            return (Boolean)fromInstance;
        }
        else if (fromInstance instanceof String)
        {
            // faster equals check "true" and "false"
            if ("true".equals(fromInstance))
            {
                return true;
            }
            else if ("false".equals(fromInstance))
            {
                return false;
            }

            return "true".equalsIgnoreCase((String)fromInstance);
        }
        else if (fromInstance instanceof Number)
        {
            return ((Number)fromInstance).longValue() != 0;
        }
        else if (fromInstance instanceof AtomicBoolean)
        {
            return ((AtomicBoolean) fromInstance).get();
        }
        nope(fromInstance, "Boolean");
        return null;
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
        return convertToAtomicInteger(fromInstance);
    }

    /**
     * Convert from the passed in instance to an AtomicInteger.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static AtomicInteger convertToAtomicInteger(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof AtomicInteger)
            {   // return a new instance because AtomicInteger is mutable
                return new AtomicInteger(((AtomicInteger)fromInstance).get());
            }
            else if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return new AtomicInteger(0);
                }
                return new AtomicInteger(Integer.parseInt(((String) fromInstance).trim()));
            }
            else if (fromInstance instanceof Number)
            {
                return new AtomicInteger(((Number)fromInstance).intValue());
            }
            else if (fromInstance instanceof Boolean)
            {
                return (Boolean) fromInstance ? new AtomicInteger(1) : new AtomicInteger(0);
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean) fromInstance).get() ? new AtomicInteger(1) : new AtomicInteger(0);
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to an 'AtomicInteger'", e);
        }
        nope(fromInstance, "AtomicInteger");
        return null;
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
        return convertToAtomicLong(fromInstance);
    }

    /**
     * Convert from the passed in instance to an AtomicLong.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.  In
     * addition, Date, LocalDate, LocalDateTime, ZonedDateTime, java.sql.Date, Timestamp, and Calendar can be passed in,
     * in which case the AtomicLong returned is the number of milliseconds since Jan 1, 1970.
     */
    public static AtomicLong convertToAtomicLong(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return new AtomicLong(0);
                }
                return new AtomicLong(Long.parseLong(((String) fromInstance).trim()));
            }
            else if (fromInstance instanceof AtomicLong)
            {   // return a clone of the AtomicLong because it is mutable
                return new AtomicLong(((AtomicLong)fromInstance).get());
            }
            else if (fromInstance instanceof Number)
            {
                return new AtomicLong(((Number)fromInstance).longValue());
            }
            else if (fromInstance instanceof Date)
            {
                return new AtomicLong(((Date)fromInstance).getTime());
            }
            else if (fromInstance instanceof LocalDate)
            {
                return new AtomicLong(localDateToMillis((LocalDate)fromInstance));
            }
            else if (fromInstance instanceof LocalDateTime)
            {
                return new AtomicLong(localDateTimeToMillis((LocalDateTime)fromInstance));
            }
            else if (fromInstance instanceof ZonedDateTime)
            {
                return new AtomicLong(zonedDateTimeToMillis((ZonedDateTime)fromInstance));
            }
            else if (fromInstance instanceof Boolean)
            {
                return (Boolean) fromInstance ? new AtomicLong(1L) : new AtomicLong(0L);
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean) fromInstance).get() ? new AtomicLong(1L) : new AtomicLong(0L);
            }
            else if (fromInstance instanceof Calendar)
            {
                return new AtomicLong(((Calendar)fromInstance).getTime().getTime());
            }
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to an 'AtomicLong'", e);
        }
        nope(fromInstance, "AtomicLong");
        return null;
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
        return convertToAtomicBoolean(fromInstance);
    }

    /**
     * Convert from the passed in instance to an AtomicBoolean.  If null is passed in, null is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.
     */
    public static AtomicBoolean convertToAtomicBoolean(Object fromInstance)
    {
        if (fromInstance instanceof String)
        {
            if (StringUtilities.isEmpty((String)fromInstance))
            {
                return new AtomicBoolean(false);
            }
            String value = (String)  fromInstance;
            return new AtomicBoolean("true".equalsIgnoreCase(value));
        }
        else if (fromInstance instanceof AtomicBoolean)
        {   // return a clone of the AtomicBoolean because it is mutable
            return new AtomicBoolean(((AtomicBoolean)fromInstance).get());
        }
        else if (fromInstance instanceof Boolean)
        {
            return new AtomicBoolean((Boolean) fromInstance);
        }
        else if (fromInstance instanceof Number)
        {
            return new AtomicBoolean(((Number)fromInstance).longValue() != 0);
        }
        nope(fromInstance, "AtomicBoolean");
        return null;
    }

    private static String nope(Object fromInstance, String targetType)
    {
        if (fromInstance == null)
        {
            return null;
        }
        throw new IllegalArgumentException("Unsupported value type [" + name(fromInstance) + "] attempting to convert to '" + targetType + "'");
    }

    private static String name(Object fromInstance)
    {
        return fromInstance.getClass().getName() + " (" + fromInstance.toString() + ")";
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
