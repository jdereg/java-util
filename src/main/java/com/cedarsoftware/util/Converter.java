package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
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
    
    private interface Work
    {
        Object convert(Object fromInstance);
    }
    
    static
    {
        conversion.put(String.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToString(fromInstance);
            }
        });

        conversion.put(long.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convert2long(fromInstance);
            }
        });

        conversion.put(Long.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToLong(fromInstance);
            }
        });

        conversion.put(int.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convert2int(fromInstance);
            }
        });

        conversion.put(Integer.class, new Work()
        {
            public Object convert(Object fromInstance) { return convertToInteger(fromInstance); }
        });

        conversion.put(short.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convert2short(fromInstance);
            }
        });

        conversion.put(Short.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToShort(fromInstance);
            }
        });

        conversion.put(byte.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convert2byte(fromInstance);
            }
        });

        conversion.put(Byte.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToByte(fromInstance);
            }
        });

        conversion.put(char.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convert2char(fromInstance);
            }
        });

        conversion.put(boolean.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convert2boolean(fromInstance);
            }
        });

        conversion.put(Boolean.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToBoolean(fromInstance);
            }
        });

        conversion.put(double.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convert2double(fromInstance);
            }
        });

        conversion.put(Double.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToDouble(fromInstance);
            }
        });

        conversion.put(float.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convert2float(fromInstance);
            }
        });

        conversion.put(Float.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToFloat(fromInstance);
            }
        });

        conversion.put(Character.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToCharacter(fromInstance);
            }
        });

        conversion.put(Calendar.class, new Work()
        {
            public Object convert(Object fromInstance) { return convertToCalendar(fromInstance); }
        });
        
        conversion.put(Date.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToDate(fromInstance);
            }
        });

        conversion.put(BigDecimal.class, new Work()
        {
            public Object convert(Object fromInstance)
            { return convertToBigDecimal(fromInstance);
            }
        });

        conversion.put(BigInteger.class, new Work()
        {
            public Object convert(Object fromInstance)
            { return convertToBigInteger(fromInstance);
            }
        });

        conversion.put(java.sql.Date.class, new Work()
        {
            public Object convert(Object fromInstance)
            { return convertToSqlDate(fromInstance);
            }
        });

        conversion.put(Timestamp.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToTimestamp(fromInstance);
            }
        });

        conversion.put(AtomicInteger.class, new Work()
        {
            public Object convert(Object fromInstance)
            { return convertToAtomicInteger(fromInstance);
            }
        });

        conversion.put(AtomicLong.class, new Work()
        {
            public Object convert(Object fromInstance)
            { return convertToAtomicLong(fromInstance);
            }
        });

        conversion.put(AtomicBoolean.class, new Work()
        {
            public Object convert(Object fromInstance)
            { return convertToAtomicBoolean(fromInstance);
            }
        });

        conversionToString.put(String.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return fromInstance;
            }
        });

        conversionToString.put(BigDecimal.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                BigDecimal bd = convertToBigDecimal(fromInstance);
                return bd.stripTrailingZeros().toPlainString();
            }
        });

        conversionToString.put(BigInteger.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                BigInteger bi = convertToBigInteger(fromInstance);
                return bi.toString();
            }
        });

        Work toString = new Work()
        {
            public Object convert(Object fromInstance)
            {
                return fromInstance.toString();
            }
        };

        conversionToString.put(Boolean.class, toString);
        conversionToString.put(AtomicBoolean.class, toString);
        conversionToString.put(Byte.class, toString);
        conversionToString.put(Short.class, toString);
        conversionToString.put(Integer.class, toString);
        conversionToString.put(AtomicInteger.class, toString);
        conversionToString.put(Long.class, toString);
        conversionToString.put(AtomicLong.class, toString);

        // Should eliminate possibility of 'e' (exponential) notation
        Work toNoExpString = new Work()
        {
            public Object convert(Object fromInstance)
            {
                return fromInstance.toString();
            }
        };

        conversionToString.put(Double.class, toNoExpString);
        conversionToString.put(Float.class, toNoExpString);

        conversionToString.put(Date.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(fromInstance);
            }
        });

        conversionToString.put(Character.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return "" + fromInstance;
            }
        });
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
        Class clazz = fromInstance.getClass();
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
     * BigDecimal of 0 or 1), Date/Calendar (returns BigDecimal with the value of number of milliseconds since Jan 1, 1970),
     * and Character (returns integer value of character).
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
            else if (fromInstance instanceof Number)
            {
                return new BigDecimal(((Number) fromInstance).doubleValue());
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
     * BigInteger of 0 or 1), Date/Calendar (returns BigInteger with the value of number of milliseconds since Jan 1, 1970),
     * and Character (returns integer value of character).
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
     * Possible inputs are TimeStamp, Date, Calendar, java.sql.Date (will return a copy), String (which will be parsed
     * by DateUtilities into a Date and a java.sql.Date will created from that), Long, BigInteger, BigDecimal, and
     * AtomicLong (all of which the java.sql.Date will be created directly from [number of milliseconds since Jan 1, 1970]).
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
     * Possible inputs are java.sql.Date, Date, Calendar, TimeStamp (will return a copy), String (which will be parsed
     * by DateUtilities into a Date and a Timestamp will created from that), Long, BigInteger, BigDecimal, and
     * AtomicLong (all of which the Timestamp will be created directly from [number of milliseconds since Jan 1, 1970]).
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
                return Byte.parseByte(((String) fromInstance).trim());
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
                return Short.parseShort(((String) fromInstance).trim());
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
                return Integer.valueOf(((String) fromInstance).trim());
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
     * addition, Date, java.sql.Date, Timestamp, and Calendar can be passed in, in which case the long returned is
     * the number of milliseconds since Jan 1, 1970.
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
     * Convert from the passed in instance to a Long.  If null is passed in, (Long) 0 is returned. Possible inputs
     * are String, all primitive/primitive wrappers, boolean, AtomicBoolean, (false=0, true=1), and all Atomic*s.  In
     * addition, Date, java.sql.Date, Timestamp, and Calendar can be passed in, in which case the long returned is
     * the number of milliseconds since Jan 1, 1970.
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
                return Long.valueOf(((String) fromInstance).trim());
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
     * all Atomic*s.  In addition, Date, java.sql.Date, Timestamp, and Calendar can be passed in, in which case the
     * AtomicLong returned is the number of milliseconds since Jan 1, 1970.
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
     * addition, Date, java.sql.Date, Timestamp, and Calendar can be passed in, in which case the AtomicLong returned
     * is the number of milliseconds since Jan 1, 1970.
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
}
