package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.StringUtilities;

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
final class NumberConversions {
    private NumberConversions() {}

    static byte toByte(Object from, Converter converter) {
        return ((Number)from).byteValue();
    }

    static Byte toByteZero(Object from, Converter converter) {
        return CommonValues.BYTE_ZERO;
    }

    static short toShort(Object from, Converter converter) {
        return ((Number)from).shortValue();
    }

    static Short toShortZero(Object from, Converter converter) {
        return CommonValues.SHORT_ZERO;
    }

    static int toInt(Object from, Converter converter) {
        return ((Number)from).intValue();
    }

    static Integer toIntZero(Object from, Converter converter) {
        return CommonValues.INTEGER_ZERO;
    }

    static long toLong(Object from, Converter converter) {
        return ((Number) from).longValue();
    }
    
    static Long toLongZero(Object from, Converter converter) {
        return CommonValues.LONG_ZERO;
    }

    static float toFloat(Object from, Converter converter) {
        return ((Number) from).floatValue();
    }

    static Float toFloatZero(Object from, Converter converter) {
        return CommonValues.FLOAT_ZERO;
    }
    
    static String floatToString(Object from, Converter converter) {
        float x = (float) from;
        if (x == 0f) {
            return "0";
        }
        return from.toString();
    }

    static double toDouble(Object from, Converter converter) {
        return ((Number) from).doubleValue();
    }

    static Double toDoubleZero(Object from, Converter converter) {
        return CommonValues.DOUBLE_ZERO;
    }

    static String doubleToString(Object from, Converter converter) {
        double x = (double) from;
        if (x == 0d) {
            return "0";
        }
        return from.toString();
    }

    static BigDecimal integerTypeToBigDecimal(Object from, Converter converter) {
        return BigDecimal.valueOf(toLong(from, converter));
    }
    
    static BigInteger integerTypeToBigInteger(Object from, Converter converter) {
        return BigInteger.valueOf(toLong(from, converter));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter) {
        return new AtomicInteger(toInt(from, converter));
    }

    static BigDecimal bigIntegerToBigDecimal(Object from, Converter converter) {
        return new BigDecimal((BigInteger)from);
    }

    static BigInteger bigDecimalToBigInteger(Object from, Converter converter) {
        return ((BigDecimal)from).toBigInteger();
    }

    static String bigDecimalToString(Object from, Converter converter) {
        return ((BigDecimal) from).stripTrailingZeros().toPlainString();
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        return new BigDecimal(StringUtilities.trimToEmpty(from.toString()));
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return new AtomicBoolean(toLong(from, converter) != 0);
    }

    static BigDecimal floatingPointToBigDecimal(Object from, Converter converter) {
        return BigDecimal.valueOf(toDouble(from, converter));
    }

    static BigInteger floatingPointToBigInteger(Object from, Converter converter) {
        double d = toDouble(from, converter);
        String s = String.format("%.0f", (d > 0.0) ? Math.floor(d) : Math.ceil(d));
        return new BigInteger(s);
    }

    static boolean isIntTypeNotZero(Object from, Converter converter) {
        return toLong(from, converter) != 0;
    }

    static boolean isFloatTypeNotZero(Object from, Converter converter) {
        return toDouble(from, converter) != 0;
    }

    static boolean isBigIntegerNotZero(Object from, Converter converter) {
        return ((BigInteger)from).compareTo(BigInteger.ZERO) != 0;
    }

    static boolean isBigDecimalNotZero(Object from, Converter converter) {
        return ((BigDecimal)from).compareTo(BigDecimal.ZERO) != 0;
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return new BigInteger(StringUtilities.trimToEmpty(from.toString()));
    }

    static UUID bigIntegerToUUID(Object from, Converter converter) {
        BigInteger bigInteger = (BigInteger) from;
        BigInteger mask = BigInteger.valueOf(Long.MAX_VALUE);
        long mostSignificantBits = bigInteger.shiftRight(64).and(mask).longValue();
        long leastSignificantBits = bigInteger.and(mask).longValue();
        return new UUID(mostSignificantBits, leastSignificantBits);
    }

    static UUID bigDecimalToUUID(Object from, Converter converter) {
        BigInteger bigInt = ((BigDecimal) from).toBigInteger();
        long mostSigBits = bigInt.shiftRight(64).longValue();
        long leastSigBits = bigInt.and(new BigInteger("FFFFFFFFFFFFFFFF", 16)).longValue();
        return new UUID(mostSigBits, leastSigBits);
    }

    /**
     * @param from      - object that is a number to be converted to char
     * @param converter - instance of converter mappings to use.
     * @return char that best represents the Number.  The result will always be a value between
     * 0 and Character.MAX_VALUE.
     * @throws IllegalArgumentException if the value exceeds the range of a char.
     */
    static char toCharacter(Object from, Converter converter) {
        long value = toLong(from, converter);
        if (value >= 0 && value <= Character.MAX_VALUE) {
            return (char) value;
        }
        throw new IllegalArgumentException("Value '" + value + "' out of range to be converted to character.");
    }

    static Date toDate(Object from, Converter converter) {
        return new Date(toLong(from, converter));
    }
    
    static Instant toInstant(Object from, Converter converter) {
        return Instant.ofEpochMilli(toLong(from, converter));
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return new java.sql.Date(toLong(from, converter));
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return new Timestamp(toLong(from, converter));
    }

    static Calendar toCalendar(Object from, Converter converter) {
        return CalendarConversions.create(toLong(from, converter), converter);
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDate();
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDateTime();
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalTime();
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return toInstant(from, converter).atZone(converter.getOptions().getZoneId());
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toOffsetDateTime();
    }

    static Year toYear(Object from, Converter converter) {
        if (from instanceof Byte) {
            throw new IllegalArgumentException("Cannot convert Byte to Year, not enough precision.");
        }
        Number number = (Number) from;
        return Year.of(number.shortValue());
    }
}
