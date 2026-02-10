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
        if (Float.floatToRawIntBits(x) == 0) {
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
        if (Double.doubleToRawLongBits(x) == 0L) {
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
    
    static BigDecimal floatingPointToBigDecimal(Object from, Converter converter) {
        return BigDecimal.valueOf(toDouble(from, converter));
    }

    static BigInteger floatingPointToBigInteger(Object from, Converter converter) {
        double d = toDouble(from, converter);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new IllegalArgumentException("Cannot convert " + d + " to BigInteger");
        }
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