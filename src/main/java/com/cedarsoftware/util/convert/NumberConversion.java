package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Kenny Partlow (kpartlow@gmail.com)
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
public class NumberConversion {

    static byte toByte(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).byteValue();
    }

    static Byte toByteZero(Object from, Converter converter, ConverterOptions options) {
        return CommonValues.BYTE_ZERO;
    }


    static short toShort(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).shortValue();
    }

    static Short toShortZero(Object from, Converter converter, ConverterOptions options) {
        return CommonValues.SHORT_ZERO;
    }

    static int toInt(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).intValue();
    }

    static Integer toIntZero(Object from, Converter converter, ConverterOptions options) {
        return CommonValues.INTEGER_ZERO;
    }


    static long toLong(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).longValue();
    }

    static Long toLongZero(Object from, Converter converter, ConverterOptions options) {
        return CommonValues.LONG_ZERO;
    }


    static float toFloat(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).floatValue();
    }

    static Float toFloatZero(Object from, Converter converter, ConverterOptions options) {
        return CommonValues.FLOAT_ZERO;
    }


    static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).doubleValue();
    }

    static Double toDoubleZero(Object from, Converter converter, ConverterOptions options) {
        return CommonValues.DOUBLE_ZERO;
    }

    static BigDecimal integerTypeToBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(((Number) from).longValue());
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        Number n = (Number)from;
        return new AtomicLong(n.longValue());
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter, ConverterOptions options) {
        Number n = (Number)from;
        return new AtomicInteger(n.intValue());
    }

    static BigDecimal bigIntegerToBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return new BigDecimal((BigInteger)from);
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return new AtomicBoolean(number.longValue() != 0);
    }

    static BigDecimal floatingPointToBigDecimal(Object from, Converter converter, ConverterOptions options) {
        Number n = (Number)from;
        return BigDecimal.valueOf(n.doubleValue());
    }

    static boolean isIntTypeNotZero(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).longValue() != 0;
    }

    static boolean isFloatTypeNotZero(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).doubleValue() != 0;
    }

    static boolean isBigIntegerNotZero(Object from, Converter converter, ConverterOptions options) {
        BigInteger bi = (BigInteger) from;
        return bi.compareTo(BigInteger.ZERO) != 0;
    }

    static boolean isBigDecimalNotZero(Object from, Converter converter, ConverterOptions options) {
        BigDecimal bd = (BigDecimal) from;
        return bd.compareTo(BigDecimal.ZERO) != 0;
    }

    /**
     * @param number Number instance to convert to char.
     * @return char that best represents the Number.  The result will always be a value between
     * 0 and Character.MAX_VALUE.
     * @throws IllegalArgumentException if the value exceeds the range of a char.
     */
    static char toCharacter(Number number) {
        long value = number.longValue();
        if (value >= 0 && value <= Character.MAX_VALUE) {
            return (char) value;
        }
        throw new IllegalArgumentException("Value: " + value + " out of range to be converted to character.");
    }

    /**
     * @param from      - object that is a number to be converted to char
     * @param converter - instance of converter mappings to use.
     * @param options   - optional conversion options, not used here.
     * @return char that best represents the Number.  The result will always be a value between
     * 0 and Character.MAX_VALUE.
     * @throws IllegalArgumentException if the value exceeds the range of a char.
     */
    static char toCharacter(Object from, Converter converter, ConverterOptions options) {
        return toCharacter((Number) from);
    }

    static Date toDate(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return new Date(number.longValue());
    }

    static java.sql.Date toSqlDate(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return new java.sql.Date(number.longValue());
    }

    static Timestamp toTimestamp(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return new Timestamp(number.longValue());
    }

    static Calendar toCalendar(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return Converter.initCal(number.longValue());
    }

    static LocalDate toLocalDate(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return LocalDate.ofEpochDay(number.longValue());
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return Instant.ofEpochMilli(number.longValue()).atZone(options.getSourceZoneId()).toLocalDateTime();
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return Instant.ofEpochMilli(number.longValue()).atZone(options.getSourceZoneId());
    }
}
