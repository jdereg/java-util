package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.DateUtilities;
import com.cedarsoftware.util.StringUtilities;

/**
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
public class StringConversions {
    private static final BigDecimal bigDecimalMinByte = BigDecimal.valueOf(Byte.MIN_VALUE);
    private static final BigDecimal bigDecimalMaxByte = BigDecimal.valueOf(Byte.MAX_VALUE);
    private static final BigDecimal bigDecimalMinShort = BigDecimal.valueOf(Short.MIN_VALUE);
    private static final BigDecimal bigDecimalMaxShort = BigDecimal.valueOf(Short.MAX_VALUE);
    private static final BigDecimal bigDecimalMinInteger = BigDecimal.valueOf(Integer.MIN_VALUE);
    private static final BigDecimal bigDecimalMaxInteger = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigDecimal bigDecimalMaxLong = BigDecimal.valueOf(Long.MAX_VALUE);
    private static final BigDecimal bigDecimalMinLong = BigDecimal.valueOf(Long.MIN_VALUE);

    static Byte toByte(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return CommonValues.BYTE_ZERO;
        }
        try {
            return Byte.valueOf(str);
        } catch (NumberFormatException e) {
            Byte value = toByte(str);
            if (value == null) {
                throw new IllegalArgumentException("Value: " + from + " not parseable as a byte value or outside " + Byte.MIN_VALUE + " to " + Byte.MAX_VALUE);
            }
            return value;
        }
    }

    private static Byte toByte(String s) {
        Long value = toLong(s, bigDecimalMinByte, bigDecimalMaxByte);
        if (value == null) {
            return null;
        }
        return value.byteValue();
    }

    static Short toShort(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return CommonValues.SHORT_ZERO;
        }
        try {
            return Short.valueOf(str);
        } catch (NumberFormatException e) {
            Short value = toShort(str);
            if (value == null) {
                throw new IllegalArgumentException("Value: " + from + " not parseable as a short value or outside " + Short.MIN_VALUE + " to " + Short.MAX_VALUE);
            }
            return value;
        }
    }

    private static Short toShort(String s) {
        Long value = toLong(s, bigDecimalMinShort, bigDecimalMaxShort);
        if (value == null) {
            return null;
        }
        return value.shortValue();
    }

    static Integer toInt(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return CommonValues.INTEGER_ZERO;
        }
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            Integer value = toInt(str);
            if (value == null) {
                throw new IllegalArgumentException("Value: " + from + " not parseable as an int value or outside " + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE);
            }
            return value;
        }
    }

    private static Integer toInt(String s) {
        Long value = toLong(s, bigDecimalMinInteger, bigDecimalMaxInteger);
        if (value == null) {
            return null;
        }
        return value.intValue();
    }

    static Long toLong(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return CommonValues.LONG_ZERO;
        }
        try {
            return Long.valueOf(str);
        } catch (NumberFormatException e) {
            Long value = toLong(str, bigDecimalMinLong, bigDecimalMaxLong);
            if (value == null) {
                throw new IllegalArgumentException("Value: " + from + " not parseable as a long value or outside " + Long.MIN_VALUE + " to " + Long.MAX_VALUE);
            }
            return value;
        }
    }

    private static Long toLong(String s, BigDecimal low, BigDecimal high) {
        try {
            BigDecimal big = new BigDecimal(s);
            big = big.setScale(0, RoundingMode.DOWN);
            if (big.compareTo(low) == -1 || big.compareTo(high) == 1) {
                return null;
            }
            return big.longValue();
        } catch (Exception e) {
            return null;
        }
    }

    static Float toFloat(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return CommonValues.FLOAT_ZERO;
        }
        try {
            return Float.valueOf(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value: " + from + " not parseable as a float value");
        }
    }

    static Double toDouble(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return CommonValues.DOUBLE_ZERO;
        }
        try {
            return Double.valueOf(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value: " + from + " not parseable as a double value");
        }
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return new AtomicBoolean(false);
        }
        return new AtomicBoolean("true".equalsIgnoreCase(str) || "t".equalsIgnoreCase(str));
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return new AtomicInteger(0);
        }

        Integer integer = toInt(str);
        if (integer == null) {
            throw new IllegalArgumentException("Value: " + from + " not parseable as an AtomicInteger value or outside " + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE);
        }
        return new AtomicInteger(integer);
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToEmpty((String)from);
        if (str.isEmpty()) {
            return new AtomicLong(0L);
        }
        Long value = toLong(str, bigDecimalMinLong, bigDecimalMaxLong);
        if (value == null) {
            throw new IllegalArgumentException("Value: " + from + " not parseable as an AtomicLong value or outside " + Long.MIN_VALUE + " to " + Long.MAX_VALUE);
        }
        return new AtomicLong(value);
    }

    static Boolean toBoolean(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToEmpty((String)from);
        if (str.isEmpty()) {
            return false;
        }
        // faster equals check "true" and "false"
        if ("true".equals(str)) {
            return true;
        } else if ("false".equals(str)) {
            return false;
        }
        return "true".equalsIgnoreCase(str) || "t".equalsIgnoreCase(str) || "1".equalsIgnoreCase(str) || "y".equalsIgnoreCase(str);
    }

    static char toCharacter(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToEmpty((String)from);
        if (str.isEmpty()) {
            return CommonValues.CHARACTER_ZERO;
        }
        if (str.length() == 1) {
            return str.charAt(0);
        }
        // Treat as a String number, like "65" = 'A'
        return (char) Integer.parseInt(str.trim());
    }

    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToEmpty((String)from);
        if (str.isEmpty()) {
            return BigInteger.ZERO;
        }
        try {
            BigDecimal bigDec = new BigDecimal(str);
            return bigDec.toBigInteger();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value: " + from + " not parseable as a BigInteger value.");
        }
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToEmpty((String)from);
        if (str.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value: " + from + " not parseable as a BigDecimal value.");
        }
    }

    static java.sql.Date toSqlDate(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToNull((String)from);
        Date date = DateUtilities.parseDate(str);
        return date == null ? null : new java.sql.Date(date.getTime());
    }

    static Timestamp toTimestamp(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToNull((String)from);
        Date date = DateUtilities.parseDate(str);
        return date == null ? null : new Timestamp(date.getTime());
    }

    static Date toDate(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToNull((String)from);
        Date date = DateUtilities.parseDate(str);
        return date;
    }

    static Instant toInstant(Object from, Converter converter, ConverterOptions options) {
        String s = StringUtilities.trimToEmpty((String)from);
        if (s.isEmpty()) {
            return null;
        }

        try {
            return Instant.parse(s);
        } catch (Exception e) {
            Date date = DateUtilities.parseDate(s);
            return date == null ? null : date.toInstant();
        }
    }

    static String toString(Object from, Converter converter, ConverterOptions options) {
        return from.toString();
    }
}
