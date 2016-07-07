package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handy conversion utilities
 *
 * @author John DeRegnaucourt (john@cedarsoftware.com)
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
    private static final Byte BYTE_ZERO = (byte)0;
    private static final Byte BYTE_ONE = (byte)1;
    private static final Short SHORT_ZERO = (short)0;
    private static final Short SHORT_ONE = (short)1;
    private static final Integer INTEGER_ZERO = 0;
    private static final Integer INTEGER_ONE = 1;
    private static final Long LONG_ZERO = 0L;
    private static final Long LONG_ONE = 1L;
    private static final Float FLOAT_ZERO = 0.0f;
    private static final Float FLOAT_ONE = 1.0f;
    private static final Double DOUBLE_ZERO = 0.0d;
    private static final Double DOUBLE_ONE = 1.0d;

    /**
     * Static utility class.
     */
    private Converter() {
    }

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
     * can also be Date.class, String.class, BigInteger.class, and BigDecimal.class.
     * The primitive class can be either primitive class or primitive wrapper class,
     * however, the returned value will always [obviously] be a primitive wrapper.
     * @return An instanceof targetType class, based upon the value passed in.
     */
    public static Object convert(Object fromInstance, Class toType)
    {
        if (toType == null)
        {
            throw new IllegalArgumentException("Type cannot be null in Converter.convert(value, type)");
        }

        if (toType == String.class)
        {
            if (fromInstance == null || fromInstance instanceof String)
            {
                return fromInstance;
            }
            else if (fromInstance instanceof BigDecimal)
            {
                return ((BigDecimal) fromInstance).stripTrailingZeros().toPlainString();
            }
            else if (fromInstance instanceof Number || fromInstance instanceof Boolean || fromInstance instanceof AtomicBoolean)
            {
                return fromInstance.toString();
            }
            else if (fromInstance instanceof Date)
            {
                return SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(fromInstance);
            }
            else if (fromInstance instanceof Calendar)
            {
                return SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(((Calendar)fromInstance).getTime());
            }
            else if (fromInstance instanceof Character)
            {
                return "" + fromInstance;
            }
            nope(fromInstance, "String");
        }
        else if (toType == long.class)
        {
            return fromInstance == null ? 0L : convertLong(fromInstance);
        }
        else if (toType == Long.class)
        {
            return fromInstance == null ? null : convertLong(fromInstance);
        }
        else if (toType == int.class)
        {
            return fromInstance == null ? 0 : convertInteger(fromInstance);
        }
        else if (toType == Integer.class)
        {
            return fromInstance == null ? null : convertInteger(fromInstance);
        }
        else if (toType == Date.class)
        {
            if (fromInstance == null)
            {
                return null;
            }
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
                else if (fromInstance instanceof AtomicLong)
                {
                    return new Date(((AtomicLong) fromInstance).get());
                }
            }
            catch(Exception e)
            {
                throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Date'", e);
            }
            nope(fromInstance, "Date");
        }
        else if (toType == BigDecimal.class)
        {
            if (fromInstance == null)
            {
                return null;
            }

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
                    return fromInstance;
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
            }
            catch(Exception e)
            {
                throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'BigDecimal'", e);
            }
            nope(fromInstance, "BigDecimal");
        }
        else if (toType == BigInteger.class)
        {
            if (fromInstance == null)
            {
                return null;
            }
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
                    return fromInstance;
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
            }
            catch(Exception e)
            {
                throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'BigInteger'", e);
            }
            nope(fromInstance, "BigInteger");
        }
        else if (toType == java.sql.Date.class)
        {
            if (fromInstance == null)
            {
                return null;
            }
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
                else if (fromInstance instanceof AtomicLong)
                {
                    return new java.sql.Date(((AtomicLong) fromInstance).get());
                }
            }
            catch(Exception e)
            {
                throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'java.sql.Date'", e);
            }
            nope(fromInstance, "java.sql.Date");
        }
        else if (toType == Timestamp.class)
        {
            if (fromInstance == null)
            {
                return null;
            }
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
                else if (fromInstance instanceof AtomicLong)
                {
                    return new Timestamp(((AtomicLong) fromInstance).get());
                }
            }
            catch(Exception e)
            {
                throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Timestamp'", e);
            }
            nope(fromInstance, "Timestamp");
        }
        else if (toType == AtomicInteger.class)
        {
            if (fromInstance == null)
            {
                return null;
            }
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
                    return new AtomicInteger(Integer.valueOf(((String) fromInstance).trim()));
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
            catch(Exception e)
            {
                throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to an 'AtomicInteger'", e);
            }
            nope(fromInstance, "AtomicInteger");
        }
        else if (toType == AtomicLong.class)
        {
            if (fromInstance == null)
            {
                return null;
            }
            try
            {
                if (fromInstance instanceof String)
                {
                    if (StringUtilities.isEmpty((String)fromInstance))
                    {
                        return new AtomicLong(0);
                    }
                    return new AtomicLong(Long.valueOf(((String) fromInstance).trim()));
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
            catch(Exception e)
            {
                throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to an 'AtomicLong'", e);
            }
            nope(fromInstance, "AtomicLong");
        }
        else if (toType == AtomicBoolean.class)
        {
            if (fromInstance == null)
            {
                return null;
            }
            else if (fromInstance instanceof String)
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
        }
        else if (toType == boolean.class)
        {
            return fromInstance == null ? Boolean.FALSE : convertBoolean(fromInstance);
        }
        else if (toType == Boolean.class)
        {
            return fromInstance == null ? null : convertBoolean(fromInstance);
        }
        else if (toType == double.class)
        {
            return fromInstance == null ? DOUBLE_ZERO : convertDouble(fromInstance);
        }
        else if (toType == Double.class)
        {
            return fromInstance == null ? null : convertDouble(fromInstance);
        }
        else if (toType == byte.class)
        {
            return fromInstance == null ? BYTE_ZERO : convertByte(fromInstance);
        }
        else if (toType == Byte.class)
        {
            return fromInstance == null ? null : convertByte(fromInstance);
        }
        else if (toType == float.class)
        {
            return fromInstance == null ? FLOAT_ZERO : convertFloat(fromInstance);
        }
        else if (toType == Float.class)
        {
            return fromInstance == null ? null : convertFloat(fromInstance);
        }
        else if (toType == short.class)
        {
            return fromInstance == null ? SHORT_ZERO : convertShort(fromInstance);
        }
        else if (toType == Short.class)
        {
            return fromInstance == null ? null : convertShort(fromInstance);
        }
        throw new IllegalArgumentException("Unsupported type '" + toType.getName() + "' for conversion");
    }

    private static Object convertByte(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return BYTE_ZERO;
                }
                return Byte.valueOf(((String) fromInstance).trim());
            }
            else if (fromInstance instanceof Byte)
            {
                return fromInstance;
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
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Byte'", e);
        }
        return nope(fromInstance, "Byte");
    }

    private static Object convertShort(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return SHORT_ZERO;
                }
                return Short.valueOf(((String) fromInstance).trim());
            }
            else if (fromInstance instanceof Short)
            {
                return fromInstance;
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
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Short'", e);
        }
        return nope(fromInstance, "Short");
    }

    private static Object convertInteger(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof Integer)
            {
                return fromInstance;
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
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to an 'Integer'", e);
        }
        return nope(fromInstance, "Integer");
    }

    private static Object convertLong(Object fromInstance)
    {
        try
        {
            if (fromInstance instanceof Long)
            {
                return fromInstance;
            }
            else if (fromInstance instanceof Number)
            {
                return ((Number)fromInstance).longValue();
            }
            else if (fromInstance instanceof String)
            {
                if (StringUtilities.isEmpty((String)fromInstance))
                {
                    return LONG_ZERO;
                }
                return Long.valueOf(((String) fromInstance).trim());
            }
            else if (fromInstance instanceof Date)
            {
                return ((Date)fromInstance).getTime();
            }
            else if (fromInstance instanceof Boolean)
            {
                return (Boolean) fromInstance ? LONG_ONE : LONG_ZERO;
            }
            else if (fromInstance instanceof AtomicBoolean)
            {
                return ((AtomicBoolean) fromInstance).get() ? LONG_ONE : LONG_ZERO;
            }
            else if (fromInstance instanceof Calendar)
            {
                return ((Calendar)fromInstance).getTime().getTime();
            }
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Long'", e);
        }
        return nope(fromInstance, "Long");
    }

    private static Object convertFloat(Object fromInstance)
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
                return fromInstance;
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
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Float'", e);
        }
        return nope(fromInstance, "Float");
    }

    private static Object convertDouble(Object fromInstance)
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
                return fromInstance;
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
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Double'", e);
        }
        return nope(fromInstance, "Double");
    }

    private static Object convertBoolean(Object fromInstance)
    {
        if (fromInstance instanceof Boolean)
        {
            return fromInstance;
        }
        else if (fromInstance instanceof Number)
        {
            return ((Number)fromInstance).longValue() != 0;
        }
        else if (fromInstance instanceof String)
        {
            if (StringUtilities.isEmpty((String)fromInstance))
            {
                return Boolean.FALSE;
            }
            String value = (String)  fromInstance;
            return "true".equalsIgnoreCase(value) ? Boolean.TRUE : Boolean.FALSE;
        }
        else if (fromInstance instanceof AtomicBoolean)
        {
            return ((AtomicBoolean) fromInstance).get();
        }
        return nope(fromInstance, "Boolean");
    }

    private static String nope(Object fromInstance, String targetType)
    {
        throw new IllegalArgumentException("Unsupported value type [" + name(fromInstance) + "] attempting to convert to '" + targetType + "'");
    }

    private static String name(Object fromInstance)
    {
        return fromInstance.getClass().getName() + " (" + fromInstance.toString() + ")";
    }
}
