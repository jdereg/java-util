package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
public class StringConversion {
    private static final BigDecimal bigDecimalMinByte = BigDecimal.valueOf(Byte.MIN_VALUE);
    private static final BigDecimal bigDecimalMaxByte = BigDecimal.valueOf(Byte.MAX_VALUE);
    private static final BigDecimal bigDecimalMinShort = BigDecimal.valueOf(Short.MIN_VALUE);
    private static final BigDecimal bigDecimalMaxShort = BigDecimal.valueOf(Short.MAX_VALUE);
    private static final BigDecimal bigDecimalMinInteger = BigDecimal.valueOf(Integer.MIN_VALUE);
    private static final BigDecimal bigDecimalMaxInteger = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigDecimal bigDecimalMaxLong = BigDecimal.valueOf(Long.MAX_VALUE);
    private static final BigDecimal bigDecimalMinLong = BigDecimal.valueOf(Long.MIN_VALUE);

    static Byte stringToByte(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return CommonValues.BYTE_ZERO;
        }
        try {
            return Byte.valueOf(str);
        } catch (NumberFormatException e) {
            Byte value = stringToByte(str);
            if (value == null) {
                throw new IllegalArgumentException("Value: " + from + " not parseable as a byte value or outside " + Byte.MIN_VALUE + " to " + Byte.MAX_VALUE);
            }
            return value;
        }
    }

    private static Byte stringToByte(String s) {
        Long value = stringToLong(s, bigDecimalMinByte, bigDecimalMaxByte);
        if (value == null) {
            return null;
        }
        return value.byteValue();
    }

    static Short stringToShort(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return CommonValues.SHORT_ZERO;
        }
        try {
            return Short.valueOf(str);
        } catch (NumberFormatException e) {
            Short value = stringToShort(str);
            if (value == null) {
                throw new IllegalArgumentException("Value: " + from + " not parseable as a short value or outside " + Short.MIN_VALUE + " to " + Short.MAX_VALUE);
            }
            return value;
        }
    }

    private static Short stringToShort(String s) {
        Long value = stringToLong(s, bigDecimalMinShort, bigDecimalMaxShort);
        if (value == null) {
            return null;
        }
        return value.shortValue();
    }

    static Integer stringToInteger(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return CommonValues.INTEGER_ZERO;
        }
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            Integer value = stringToInteger(str);
            if (value == null) {
                throw new IllegalArgumentException("Value: " + from + " not parseable as an int value or outside " + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE);
            }
            return value;
        }
    }

    private static Integer stringToInteger(String s) {
        Long value = stringToLong(s, bigDecimalMinInteger, bigDecimalMaxInteger);
        if (value == null) {
            return null;
        }
        return value.intValue();
    }

    static Long stringToLong(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return CommonValues.LONG_ZERO;
        }
        try {
            return Long.valueOf(str);
        } catch (NumberFormatException e) {
            Long value = stringToLong(str, bigDecimalMinLong, bigDecimalMaxLong);
            if (value == null) {
                throw new IllegalArgumentException("Value: " + from + " not parseable as a long value or outside " + Long.MIN_VALUE + " to " + Long.MAX_VALUE);
            }
            return value;
        }
    }

    private static Long stringToLong(String s, BigDecimal low, BigDecimal high) {
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

    static Float stringToFloat(Object from, Converter converter, ConverterOptions options) {
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

    static Double stringToDouble(Object from, Converter converter, ConverterOptions options) {
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

    static AtomicInteger stringToAtomicInteger(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return new AtomicInteger(0);
        }

        Integer integer = stringToInteger(str);
        if (integer == null) {
            throw new IllegalArgumentException("Value: " + from + " not parseable as an AtomicInteger value or outside " + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE);
        }
        return new AtomicInteger(integer);
    }

    static AtomicLong stringToAtomicLong(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return new AtomicLong(0L);
        }
        Long value = stringToLong(str, bigDecimalMinLong, bigDecimalMaxLong);
        if (value == null) {
            throw new IllegalArgumentException("Value: " + from + " not parseable as an AtomicLong value or outside " + Long.MIN_VALUE + " to " + Long.MAX_VALUE);
        }
        return new AtomicLong(value);
    }
}
