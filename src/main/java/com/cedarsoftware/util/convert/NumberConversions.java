package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

import com.cedarsoftware.util.internal.DoubleDecimal;
import com.cedarsoftware.util.internal.FloatDecimal;

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
        return (byte) fit(from, Byte.MIN_VALUE, Byte.MAX_VALUE, "byte");
    }

    static Byte toByteZero(Object from, Converter converter) {
        return CommonValues.BYTE_ZERO;
    }

    static short toShort(Object from, Converter converter) {
        return (short) fit(from, Short.MIN_VALUE, Short.MAX_VALUE, "short");
    }

    static Short toShortZero(Object from, Converter converter) {
        return CommonValues.SHORT_ZERO;
    }

    static int toInt(Object from, Converter converter) {
        return (int) fit(from, Integer.MIN_VALUE, Integer.MAX_VALUE, "int");
    }

    static Integer toIntZero(Object from, Converter converter) {
        return CommonValues.INTEGER_ZERO;
    }

    static long toLong(Object from, Converter converter) {
        if (from instanceof Long) {
            return (Long) from;
        }
        return fit(from, Long.MIN_VALUE, Long.MAX_VALUE, "long");
    }
    
    static Long toLongZero(Object from, Converter converter) {
        return CommonValues.LONG_ZERO;
    }

    static float toFloat(Object from, Converter converter) {
        float value = ((Number) from).floatValue();
        if (Float.isInfinite(value) && !isInfinite(from)) {
            // a finite value beyond float's range: (float) 1e300 is Infinity
            throw new IllegalArgumentException("Unable to convert " + shown(from) + " to float: it is outside "
                    + -Float.MAX_VALUE + " to " + Float.MAX_VALUE);
        }
        return value;
    }

    static Float toFloatZero(Object from, Converter converter) {
        return CommonValues.FLOAT_ZERO;
    }
    
    /** The text Float.toString prints from JDK 19 on, on every JDK (JDK 8 printed 9.9E-44f as 1.0E-43). */
    static String floatToString(Object from, Converter converter) {
        float x = (float) from;
        if (Float.floatToRawIntBits(x) == 0) {
            return "0";
        }
        return FloatDecimal.toString(x);
    }

    static double toDouble(Object from, Converter converter) {
        double value = ((Number) from).doubleValue();
        if (Double.isInfinite(value) && !isInfinite(from)) {
            // a finite BigDecimal or BigInteger beyond double's range: 1e400 as a double is Infinity
            throw new IllegalArgumentException("Unable to convert " + shown(from) + " to double: it is outside "
                    + -Double.MAX_VALUE + " to " + Double.MAX_VALUE);
        }
        return value;
    }

    // -------------------------------------------------------------------------------------------------------------
    // Narrowing: a number converts to a narrower type only if it fits. A fraction is truncated toward zero, as a cast
    // does; a value beyond the target's range, NaN or infinity is an IllegalArgumentException. A cast wrapped a long
    // or BigInteger (2^32 + 1 as an int is 1), clamped a double (1e10 as an int is 2147483647, and as a short -1), and
    // made NaN 0. A String source was already refused that way.
    // -------------------------------------------------------------------------------------------------------------

    /**
     * {@code from} as a whole number from {@code min} to {@code max}, for a conversion to {@code type}: truncated
     * toward zero, refused if it does not fit.
     */
    static long fit(Object from, long min, long max, String type) {
        long value;
        if (from instanceof Long || from instanceof Integer) {          // the common sources first
            value = ((Number) from).longValue();
        } else if (from instanceof Double) {
            value = wholeLong((Double) from, from, type, min, max);
        } else if (from instanceof Float) {
            value = wholeLong((float) (Float) from, from, type, min, max);
        } else if (from instanceof BigDecimal) {
            value = wholeLong((BigDecimal) from, from, type, min, max);
        } else if (from instanceof BigInteger) {
            value = wholeLong((BigInteger) from, from, type, min, max);
        } else if (from instanceof Short || from instanceof Byte || from instanceof AtomicLong
                || from instanceof AtomicInteger || from instanceof LongAdder) {
            value = ((Number) from).longValue();
        } else if (from instanceof DoubleAdder) {
            value = wholeLong(((Number) from).doubleValue(), from, type, min, max);
        } else {
            value = ((Number) from).longValue();    // another Number: its own longValue()
        }
        if (value < min || value > max) {
            throw outOfRange(from, type, min, max);
        }
        return value;
    }

    /**
     * {@code d} as the decimal it prints, truncated toward zero, if finite and within a long: 2^60 is
     * 1152921504606847000 (it prints 1.152921504606847E18), as BigDecimal and String take it, and on every JDK the
     * decimal JDK 19 prints. Below 2^53 every whole double is exact and the cast is that number. -2^63, the double
     * Long.MIN_VALUE becomes, stays Long.MIN_VALUE: its decimal (-9.223372036854776E18) is just past a long, its value
     * is not, and a cast, Jackson and Python all give it back.
     */
    static long wholeLong(double d, Object from, String type, long min, long max) {
        if (d > -0x1p53 && d < 0x1p53) {
            return (long) d;
        }
        if (Double.isNaN(d)) {
            throw new IllegalArgumentException("Unable to convert " + shown(from) + " to " + type
                    + ": it is not a number");
        }
        if (!(d >= -0x1p63 && d < 0x1p63)) {      // infinite, or beyond a long
            throw outOfRange(from, type, min, max);
        }
        return d == -0x1p63 ? Long.MIN_VALUE : DoubleDecimal.printedLong(d);
    }

    /**
     * {@code f} as the decimal it prints, truncated toward zero, if finite and within a long: 1.5e10f is 15000000000,
     * not 15000000512 (its binary value), as BigDecimal and String take it. Below 2^24 every whole float is exact and
     * the cast is that number.
     */
    static long wholeLong(float f, Object from, String type, long min, long max) {
        if (f > -0x1p24f && f < 0x1p24f) {
            return (long) f;
        }
        double d = f;
        if (Double.isNaN(d)) {
            throw new IllegalArgumentException("Unable to convert " + shown(from) + " to " + type
                    + ": it is not a number");
        }
        if (!(d >= -0x1p63 && d < 0x1p63)) {      // infinite, or beyond a long
            throw outOfRange(from, type, min, max);
        }
        return FloatDecimal.printedLong(f);
    }

    /** {@code b} as a long, if it fits one. */
    static long wholeLong(BigInteger b, Object from, String type, long min, long max) {
        if (b.bitLength() > 63) {
            throw outOfRange(from, type, min, max);
        }
        return b.longValue();
    }

    /**
     * {@code d}'s whole part as a long, if it fits one. A value of more than 19 whole digits is refused by counting
     * them, without computing it: toBigInteger() of 1e999999999 builds a billion digits.
     */
    static long wholeLong(BigDecimal d, Object from, String type, long min, long max) {
        long wholeDigits = (long) d.precision() - d.scale();
        if (wholeDigits <= 0) {
            return 0L;
        }
        if (wholeDigits <= 18) {
            return d.longValue();       // below 10^18 in magnitude: fits, and longValue() truncates toward zero
        }
        if (wholeDigits > 19) {
            throw outOfRange(from, type, min, max);
        }
        return wholeLong(d.toBigInteger(), from, type, min, max);
    }

    static IllegalArgumentException outOfRange(Object from, String type, long min, long max) {
        return new IllegalArgumentException("Unable to convert " + shown(from) + " to " + type + ": it is outside "
                + min + " to " + max);
    }

    /** The value for a message: at most 100 characters, and a huge BigInteger by its size, not its digits. */
    static String shown(Object from) {
        if (from instanceof BigInteger && ((BigInteger) from).bitLength() > 300) {
            return "a " + ((BigInteger) from).bitLength() + "-bit BigInteger";
        }
        String text = String.valueOf(from);
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }

    /**
     * A count of time units as a long, for a conversion of {@code from} to the date or time type {@code type}:
     * truncated toward zero, refused if it is NaN, infinite, or beyond a long. A cast made NaN the epoch and clamped
     * 1e300 seconds to the year 292278994.
     */
    static long timeUnits(double units, Object from, String type) {
        if (Double.isNaN(units)) {
            throw new IllegalArgumentException("Unable to convert " + shown(from) + " to " + type
                    + ": it is not a number");
        }
        if (!(units >= -0x1p63 && units < 0x1p63)) {
            throw outsideTimeRange(from, type);
        }
        return (long) units;
    }

    /** As {@link #timeUnits(double, Object, String)}, for a BigInteger count: longValue() wrapped 2^64 + 1000 to 1000. */
    static long timeUnits(BigInteger units, Object from, String type) {
        if (units.bitLength() > 63) {
            throw outsideTimeRange(from, type);
        }
        return units.longValue();
    }

    /** As {@link #timeUnits(double, Object, String)}, for a BigDecimal count, its whole part, refused by digit count. */
    static long timeUnits(BigDecimal units, Object from, String type) {
        long wholeDigits = (long) units.precision() - units.scale();
        if (wholeDigits <= 0) {
            return 0L;
        }
        if (wholeDigits > 19) {
            throw outsideTimeRange(from, type);
        }
        return timeUnits(units.toBigInteger(), from, type);
    }

    static IllegalArgumentException outsideTimeRange(Object from, String type) {
        return new IllegalArgumentException("Unable to convert " + shown(from) + " to " + type
                + ": it is outside the range of " + type);
    }

    private static boolean isInfinite(Object from) {
        return (from instanceof Double || from instanceof Float || from instanceof DoubleAdder)
                && Double.isInfinite(((Number) from).doubleValue());
    }

    /**
     * Widen a Float to a double through its decimal text rather than its bits.
     * <p>
     * {@code Float.doubleValue()} is binary-exact and therefore decimal-wrong: 3.7f becomes
     * 3.700000047683716, because the float nearest 3.7 is not 3.7 and widening preserves that error instead of the
     * author's intent. The shortest decimal that uniquely identifies the float -- "3.7", what {@code Float.toString()}
     * prints from JDK 19 on -- is the number the author actually wrote. The same reasoning applies to every Float
     * source that keeps the fractional value; only the truncating targets are unaffected. FloatDecimal computes that
     * decimal's double without printing or parsing, and gives the JDK 19 decimal on every JDK.
     */
    static Double floatToDouble(Object from, Converter converter) {
        return FloatDecimal.printedValue((Float) from);
    }

    static Double toDoubleZero(Object from, Converter converter) {
        return CommonValues.DOUBLE_ZERO;
    }

    /** The text Double.toString prints from JDK 19 on, on every JDK (JDK 8 printed 1.0E23 as 9.999999999999999E22). */
    static String doubleToString(Object from, Converter converter) {
        double x = (double) from;
        if (Double.doubleToRawLongBits(x) == 0L) {
            return "0";
        }
        return DoubleDecimal.toString(x);
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
    
    /** A Double as the decimal it prints (0.1 is 0.1), the one JDK 19 prints, on every JDK. */
    static BigDecimal floatingPointToBigDecimal(Object from, Converter converter) {
        double d = toDouble(from, converter);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return BigDecimal.valueOf(d);                   // throws NumberFormatException, as before
        }
        return DoubleDecimal.printedDecimal(d);
    }

    /**
     * A Float to BigDecimal through its decimal text, for the reason given on {@link #floatToDouble}: going via
     * doubleValue() carried the binary error into a type whose whole purpose is exact decimal representation, so
     * 3.7f arrived as 3.700000047683716 rather than 3.7.
     */
    static BigDecimal floatToBigDecimal(Object from, Converter converter) {
        float f = (Float) from;
        if (Float.isNaN(f) || Float.isInfinite(f)) {
            return new BigDecimal(from.toString());     // throws NumberFormatException, as before
        }
        return FloatDecimal.printedDecimal(f);          // the JDK 19 decimal on every JDK (JDK 8 printed 19 longer)
    }

    static BigInteger floatingPointToBigInteger(Object from, Converter converter) {
        if (from instanceof Float) {
            float f = (Float) from;
            if (Float.isNaN(f) || Float.isInfinite(f)) {
                throw new IllegalArgumentException("Cannot convert " + f + " to BigInteger");
            }
            // the decimal the float prints (3.4e38f is 3.4E38): widened to a double first, it took the double's digits
            return FloatDecimal.printedDecimal(f).toBigInteger();
        }
        double d = toDouble(from, converter);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new IllegalArgumentException("Cannot convert " + d + " to BigInteger");
        }
        // The whole part of the decimal it prints, the one JDK 19 prints, on every JDK (as String.format("%.0f") did,
        // without its locale's digits: under ar-SA those are not ASCII, and parsing them threw)
        return DoubleDecimal.printedDecimal(d).toBigInteger();
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

    /**
     * @param from      - object that is a number to be converted to char
     * @param converter - instance of converter mappings to use.
     * @return char that best represents the Number.  The result will always be a value between
     * 0 and Character.MAX_VALUE.
     * @throws IllegalArgumentException if the value exceeds the range of a char.
     */
    static char toCharacter(Object from, Converter converter) {
        return (char) fit(from, Character.MIN_VALUE, Character.MAX_VALUE, "char");
    }

    static Date toDate(Object from, Converter converter) {
        return new Date(toLong(from, converter));
    }

    static Instant toInstant(Object from, Converter converter) {
        long value = toLong(from, converter);
        
        // Check for precision override (system property takes precedence)
        String systemPrecision = System.getProperty("cedarsoftware.converter.modern.time.long.precision");
        String precision = systemPrecision;
        
        // Fall back to converter options if no system property
        if (precision == null) {
            precision = converter.getOptions().getCustomOption("modern.time.long.precision");
        }
        
        // Default to milliseconds if no override specified
        if (Converter.PRECISION_NANOS.equals(precision)) {
            return Instant.ofEpochSecond(value / 1_000_000_000L, value % 1_000_000_000L);
        } else {
            return Instant.ofEpochMilli(value); // Default: milliseconds
        }
    }
    
    static Duration longNanosToDuration(Object from, Converter converter) {
        long value = toLong(from, converter);
        
        // Check for precision override (system property takes precedence)
        String systemPrecision = System.getProperty("cedarsoftware.converter.duration.long.precision");
        String precision = systemPrecision;
        
        // Fall back to converter options if no system property
        if (precision == null) {
            precision = converter.getOptions().getCustomOption("duration.long.precision");
        }
        
        // Handle precision-aware conversion
        if (Converter.PRECISION_NANOS.equals(precision)) {
            // Treat as nanoseconds
            return Duration.ofNanos(value);
        } else {
            // Default: treat as milliseconds
            return Duration.ofMillis(value);
        }
    }
    
    static Instant longNanosToInstant(Object from, Converter converter) {
        long value = toLong(from, converter);
        
        // Check for precision override (system property takes precedence)
        String systemPrecision = System.getProperty("cedarsoftware.converter.modern.time.long.precision");
        String precision = systemPrecision;
        
        // Fall back to converter options if no system property
        if (precision == null) {
            precision = converter.getOptions().getCustomOption("modern.time.long.precision");
        }
        
        // Handle precision-aware conversion
        if (Converter.PRECISION_NANOS.equals(precision)) {
            // Treat as nanoseconds
            return Instant.ofEpochSecond(value / 1_000_000_000L, value % 1_000_000_000L);
        } else {
            // Default: treat as milliseconds
            return Instant.ofEpochMilli(value);
        }
    }
    
    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return java.sql.Date.valueOf(
                Instant.ofEpochMilli(((Number) from).longValue())
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate()
        );
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        return new Timestamp(toLong(from, converter));
    }

    static Calendar toCalendar(Object from, Converter converter) {
        return CalendarConversions.create(toLong(from, converter), converter);
    }

    static LocalTime longNanosToLocalTime(Object from, Converter converter) {
        long value = ((Number) from).longValue();
        
        // Check for precision override (system property takes precedence)
        String systemPrecision = System.getProperty("cedarsoftware.converter.localtime.long.precision");
        String precision = systemPrecision;
        
        // Fall back to converter options if no system property
        if (precision == null) {
            precision = converter.getOptions().getCustomOption("localtime.long.precision");
        }
        
        if (Converter.PRECISION_NANOS.equals(precision)) {
            // Treat as nanoseconds - validate range first
            if (value < 0 || value > 86399999999999L) {
                throw new IllegalArgumentException("Input value [" + value + "] for conversion to LocalTime must be >= 0 && <= 86399999999999");
            }
            try {
                return LocalTime.ofNanoOfDay(value);
            } catch (Exception e) {
                throw new IllegalArgumentException("Input value [" + value + "] for conversion to LocalTime must be >= 0 && <= 86399999999999", e);
            }
        } else {
            // Default: treat as milliseconds - validate range first  
            if (value < 0 || value > 86399999L) {
                throw new IllegalArgumentException("Input value [" + value + "] for conversion to LocalTime must be >= 0 && <= 86399999");
            }
            try {
                long seconds = value / 1000L;
                long millis = value % 1000L;
                return LocalTime.ofSecondOfDay(seconds).plusNanos(millis * 1_000_000L);
            } catch (Exception e) {
                throw new IllegalArgumentException("Input value [" + value + "] for conversion to LocalTime must be >= 0 && <= 86399999", e);
            }
        }
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDate();
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toLocalDateTime();
    }
    
    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return toInstant(from, converter).atZone(converter.getOptions().getZoneId());
    }

    static OffsetTime toOffsetTime(Object from, Converter converter) {
        if (from instanceof Integer || from instanceof Long || from instanceof AtomicLong || from instanceof AtomicInteger) {
            long number = ((Number)from).longValue();
            Instant instant = Instant.ofEpochMilli(number);
            return OffsetTime.ofInstant(instant, converter.getOptions().getZoneId());
        } else if (from instanceof BigDecimal) {
            return BigDecimalConversions.toOffsetTime(from, converter);
        } else if (from instanceof BigInteger) {
            return BigIntegerConversions.toOffsetTime(from, converter);
        }
        
        throw new IllegalArgumentException("Unsupported value: " + from + " requested to be converted to an OffsetTime.");
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        return toZonedDateTime(from, converter).toOffsetDateTime();
    }

    static Year toYear(Object from, Converter converter) {
        Number number = (Number) from;
        return Year.of(number.intValue());
    }

    /**
     * Convert null/void to Year 0 (the "zero" point for Year).
     * @param from null/void value
     * @param converter Converter instance
     * @return Year.of(0)
     */
    static Year nullToYear(Object from, Converter converter) {
        return Year.of(0);
    }

    /**
     * Convert Number to MonthDay. Parses the number as MMDD format.
     * For example, 1225 becomes MonthDay.of(12, 25).
     * @param from Number to convert (int, Integer, short, etc.)
     * @param converter Converter instance
     * @return MonthDay instance
     * @throws IllegalArgumentException if the number is not in valid MMDD format
     */
    static YearMonth toYearMonth(Object from, Converter converter) {
        Number number = (Number) from;
        int value = number.intValue();

        int year = value / 100;
        int month = value % 100;

        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month in YYYYMM format: " + month + " (from " + value + ")");
        }

        try {
            return YearMonth.of(year, month);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid YYYYMM format: " + value + " - " + e.getMessage(), e);
        }
    }

    static MonthDay toMonthDay(Object from, Converter converter) {
        Number number = (Number) from;
        int value = number.intValue();
        
        // Handle negative numbers
        if (value < 0) {
            throw new IllegalArgumentException("Cannot convert negative number to MonthDay: " + value);
        }
        
        // Extract month and day from MMDD format
        int month = value / 100;
        int day = value % 100;
        
        // Validate month and day ranges
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Invalid month in MMDD format: " + month + " (from " + value + ")");
        }
        if (day < 1 || day > 31) {
            throw new IllegalArgumentException("Invalid day in MMDD format: " + day + " (from " + value + ")");
        }
        
        try {
            return MonthDay.of(month, day);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid MMDD format: " + value + " - " + e.getMessage(), e);
        }
    }
}