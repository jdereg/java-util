package com.cedarsoftware.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
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
    private static final Map<Class, Work> conversion = new LinkedHashMap<>();
    private static final Map<Class, Work> conversionToString = new LinkedHashMap<>();

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
                return fromInstance == null ? 0L : convertToLong(fromInstance);
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
                return fromInstance == null ? 0 : convertToInteger(fromInstance);
            }
        });

        conversion.put(Integer.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToInteger(fromInstance);
            }
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
            {
                return convertToBigDecimal(fromInstance);
            }
        });

        conversion.put(BigInteger.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToBigInteger(fromInstance);
            }
        });

        conversion.put(java.sql.Date.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToSqlDate(fromInstance);
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
            {
                return convertToAtomicInteger(fromInstance);
            }
        });

        conversion.put(AtomicLong.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToAtomicLong(fromInstance);
            }
        });

        conversion.put(AtomicBoolean.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToAtomicBoolean(fromInstance);
            }
        });

        conversion.put(boolean.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return fromInstance == null ? Boolean.FALSE : convertToBoolean(fromInstance);
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
                return fromInstance == null ? DOUBLE_ZERO : convertToDouble(fromInstance);
            }
        });

        conversion.put(Double.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToDouble(fromInstance);
            }
        });

        conversion.put(byte.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return fromInstance == null ? BYTE_ZERO : convertToByte(fromInstance);
            }
        });

        conversion.put(Byte.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToByte(fromInstance);
            }
        });

        conversion.put(float.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return fromInstance == null ? FLOAT_ZERO : convertToFloat(fromInstance);
            }
        });

        conversion.put(Float.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToFloat(fromInstance);
            }
        });

        conversion.put(short.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return fromInstance == null ? SHORT_ZERO : convertToShort(fromInstance);
            }
        });

        conversion.put(Short.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                return convertToShort(fromInstance);
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
                return ((BigDecimal) fromInstance).stripTrailingZeros().toPlainString();
            }
        });

        conversionToString.put(BigInteger.class, new Work()
        {
            public Object convert(Object fromInstance)
            {
                BigInteger bi = (BigInteger)fromInstance;
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

        Work toNoExpString = new Work()
        {
            public Object convert(Object fromInstance)
            {   // Should eliminate possibility of 'e' (exponential) notation
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

        Work work = conversion.get(toType);
        if (work != null)
        {
            return work.convert(fromInstance);
        }
        throw new IllegalArgumentException("Unsupported type '" + toType.getName() + "' for conversion");
    }

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
        {
            return SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(((Calendar)fromInstance).getTime());
        }
        else if (fromInstance instanceof Enum)
        {
            return ((Enum)fromInstance).name();
        }
        return nope(fromInstance, "String");
    }

    public static BigDecimal convertToBigDecimal(Object fromInstance)
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
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'BigDecimal'", e);
        }
        nope(fromInstance, "BigDecimal");
        return null;
    }

    public static BigInteger convertToBigInteger(Object fromInstance)
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
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'BigInteger'", e);
        }
        nope(fromInstance, "BigInteger");
        return null;
    }

    public static java.sql.Date convertToSqlDate(Object fromInstance)
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
        return null;
    }

    public static Timestamp convertToTimestamp(Object fromInstance)
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
        return null;
    }

    public static Date convertToDate(Object fromInstance)
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
        return null;
    }

    public static Byte convertToByte(Object fromInstance)
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
                    return BYTE_ZERO;
                }
                return Byte.valueOf(((String) fromInstance).trim());
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
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Byte'", e);
        }
        nope(fromInstance, "Byte");
        return null;
    }

    public static Short convertToShort(Object fromInstance)
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
                    return SHORT_ZERO;
                }
                return Short.valueOf(((String) fromInstance).trim());
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
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Short'", e);
        }
        nope(fromInstance, "Short");
        return null;
    }

    public static Integer convertToInteger(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return null;
        }
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
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to an 'Integer'", e);
        }
        nope(fromInstance, "Integer");
        return null;
    }

    public static Long convertToLong(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return null;
        }
        try
        {
            if (fromInstance instanceof Long)
            {
                return (Long) fromInstance;
            }
            else if (fromInstance instanceof String)
            {
                String trimmed = ((String) fromInstance).trim();
                if ("".equals(trimmed))
                {
                    return LONG_ZERO;
                }
                return Long.valueOf(trimmed);
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
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Long'", e);
        }
        nope(fromInstance, "Long");
        return null;
    }

    public static Float convertToFloat(Object fromInstance)
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
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Float'", e);
        }
        nope(fromInstance, "Float");
        return null;
    }

    public static Double convertToDouble(Object fromInstance)
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
        catch(Exception e)
        {
            throw new IllegalArgumentException("value [" + name(fromInstance) + "] could not be converted to a 'Double'", e);
        }
        nope(fromInstance, "Double");
        return null;
    }

    public static Boolean convertToBoolean(Object fromInstance)
    {
        if (fromInstance == null)
        {
            return null;
        }
        if (fromInstance instanceof Boolean)
        {
            return (Boolean)fromInstance;
        }
        else if (fromInstance instanceof String)
        {
            // faster equals check "true" and "false"
            if ("true".equals(fromInstance))
            {
                return Boolean.TRUE;
            }
            else if ("false".equals(fromInstance))
            {
                return Boolean.FALSE;
            }

            return "true".equalsIgnoreCase((String)fromInstance) ? Boolean.TRUE : Boolean.FALSE;
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

    public static AtomicInteger convertToAtomicInteger(Object fromInstance)
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
        return null;
    }

    public static AtomicLong convertToAtomicLong(Object fromInstance)
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
        return null;
    }

    public static AtomicBoolean convertToAtomicBoolean(Object fromInstance)
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
        return null;
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
