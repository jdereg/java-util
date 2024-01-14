package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
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

    public static byte toByte(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).byteValue();
    }

    public static short toShort(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).shortValue();
    }

    public static int toInt(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).intValue();
    }

    public static long toLong(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).longValue();
    }

    public static float toFloat(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).floatValue();
    }

    public static double toDouble(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).doubleValue();
    }

    public static BigDecimal longToBigDecimal(Object from, Converter converter, ConverterOptions options) {
        return BigDecimal.valueOf(((Number) from).longValue());
    }

    public static boolean isIntTypeNotZero(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).longValue() != 0;
    }

    public static boolean isFloatTypeNotZero(Object from, Converter converter, ConverterOptions options) {
        return ((Number) from).doubleValue() != 0;
    }

    /**
     * @param number Number instance to convert to char.
     * @return char that best represents the Number.  The result will always be a value between
     * 0 and Character.MAX_VALUE.
     * @throws IllegalArgumentException if the value exceeds the range of a char.
     */
    public static char numberToCharacter(Number number) {
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
    public static char numberToCharacter(Object from, Converter converter, ConverterOptions options) {
        return numberToCharacter((Number) from);
    }

    public static AtomicInteger numberToAtomicInteger(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return new AtomicInteger(number.intValue());
    }

    public static AtomicLong numberToAtomicLong(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return new AtomicLong(number.longValue());
    }

    public static Date numberToDate(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return new Date(number.longValue());
    }

    public static java.sql.Date numberToSqlDate(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return new java.sql.Date(number.longValue());
    }

    public static Timestamp numberToTimestamp(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return new Timestamp(number.longValue());
    }

    public static Calendar numberToCalendar(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return Converter.initCal(number.longValue());
    }

    public static LocalDate numberToLocalDate(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return LocalDate.ofEpochDay(number.longValue());
    }

    public static LocalDateTime numberToLocalDateTime(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return Instant.ofEpochMilli(number.longValue()).atZone(options.getSourceZoneId()).toLocalDateTime();
    }

    public static ZonedDateTime numberToZonedDateTime(Object from, Converter converter, ConverterOptions options) {
        Number number = (Number) from;
        return Instant.ofEpochMilli(number.longValue()).atZone(options.getSourceZoneId());
    }
}