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
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.geom.Color;
import com.cedarsoftware.util.geom.Dimension;
import com.cedarsoftware.util.geom.Insets;
import com.cedarsoftware.util.geom.Point;
import com.cedarsoftware.util.geom.Rectangle;

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

    static Duration toDuration(Object from, Converter converter) {
        Number num = (Number) from;

        // For whole number types, respect precision configuration
        if (num instanceof Long
                || num instanceof Integer
                || num instanceof BigInteger
                || num instanceof AtomicLong
                || num instanceof AtomicInteger) {
                
            // Check for precision override (system property takes precedence)
            String systemPrecision = System.getProperty("cedarsoftware.converter.duration.long.precision");
            String precision = systemPrecision;
            
            // Fall back to converter options if no system property
            if (precision == null) {
                precision = converter.getOptions().getCustomOption("duration.long.precision");
            }
            
            // Default to milliseconds if no override specified
            if (Converter.PRECISION_NANOS.equals(precision)) {
                return Duration.ofNanos(num.longValue());
            } else {
                return Duration.ofMillis(num.longValue()); // Default: milliseconds
            }
        }
        // For BigDecimal, interpret the value as seconds (with fractional seconds).
        else if (num instanceof BigDecimal) {
            BigDecimal seconds = (BigDecimal) num;
            long wholeSecs = seconds.longValue();
            long nanos = seconds.subtract(BigDecimal.valueOf(wholeSecs))
                    .multiply(BigDecimal.valueOf(1_000_000_000L))
                    .longValue();
            return Duration.ofSeconds(wholeSecs, nanos);
        }
        // For Double and Float, interpret as seconds with fractional seconds.
        else if (num instanceof Double || num instanceof Float) {
            BigDecimal seconds = BigDecimal.valueOf(num.doubleValue());
            long wholeSecs = seconds.longValue();
            long nanos = seconds.subtract(BigDecimal.valueOf(wholeSecs))
                    .multiply(BigDecimal.valueOf(1_000_000_000L))
                    .longValue();
            return Duration.ofSeconds(wholeSecs, nanos);
        }
        // Fallback: use the number's string representation as seconds.
        else {
            BigDecimal seconds = new BigDecimal(num.toString());
            long wholeSecs = seconds.longValue();
            long nanos = seconds.subtract(BigDecimal.valueOf(wholeSecs))
                    .multiply(BigDecimal.valueOf(1_000_000_000L))
                    .longValue();
            return Duration.ofSeconds(wholeSecs, nanos);
        }
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
    
    static Duration atomicLongNanosToDuration(Object from, Converter converter) {
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
    
    static Instant atomicLongNanosToInstant(Object from, Converter converter) {
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

    static LocalTime toLocalTime(Object from, Converter converter) {
        long value = ((Number) from).longValue();
        
        // Check for precision override (system property takes precedence)
        String systemPrecision = System.getProperty("cedarsoftware.converter.localtime.long.precision");
        String precision = systemPrecision;
        
        // Fall back to converter options if no system property
        if (precision == null) {
            precision = converter.getOptions().getCustomOption("localtime.long.precision");
        }
        
        // Default to milliseconds if no override specified
        if (Converter.PRECISION_NANOS.equals(precision)) {
            try {
                return LocalTime.ofNanoOfDay(value);
            } catch (Exception e) {
                throw new IllegalArgumentException("Input value [" + value + "] for conversion to LocalTime must be >= 0 && <= 86399999999999", e);
            }
        } else {
            try {
                return LocalTime.ofNanoOfDay(value * 1_000_000); // Default: milliseconds
            } catch (Exception e) {
                throw new IllegalArgumentException("Input value [" + value + "] for conversion to LocalTime must be >= 0 && <= 86399999", e);
            }
        }
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
        return Year.of(number.shortValue());
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
     * Convert Number to java.awt.Color. Treats the number as a packed RGB or ARGB value.
     * @param from Number (Integer, Long, etc.) representing packed RGB value
     * @param converter Converter instance
     * @return Color instance created from the packed RGB value
     */
    static Color toColor(Object from, Converter converter) {
        Number number = (Number) from;
        int rgb = number.intValue();
        // Check if this might be an ARGB value (has meaningful alpha channel)
        if ((rgb & 0xFF000000) != 0) {
            return new Color(rgb, true); // Include alpha
        } else {
            return new Color(rgb); // RGB only
        }
    }

    /**
     * Convert Number to Dimension. The number is treated as both width and height (square dimension).
     * @param from Number to convert (will be used as both width and height)  
     * @param converter Converter instance
     * @return Dimension instance with width = height = number
     */
    static Dimension toDimension(Object from, Converter converter) {
        Number number = (Number) from;
        int size = number.intValue();
        
        // Validate size (should be non-negative for Dimension)
        if (size < 0) {
            throw new IllegalArgumentException("Dimension size must be non-negative, got: " + size);
        }
        
        return new Dimension(size, size);
    }

    /**
     * Convert Number to Point. The number is treated as both x and y coordinates (square point).
     * @param from Number to convert (will be used as both x and y)  
     * @param converter Converter instance
     * @return Point instance with x = y = number
     */
    static Point toPoint(Object from, Converter converter) {
        Number number = (Number) from;
        int coordinate = number.intValue();
        
        return new Point(coordinate, coordinate);
    }

    // ========================================
    // Atomic Types to Dimension/Point (Recursive Approach)
    // ========================================

    // atomicIntegerToDimension removed - now bridged via AtomicInteger → Integer → Dimension

    // atomicLongToDimension removed - now bridged via AtomicLong → Long → Dimension

    /**
     * Convert AtomicBoolean to Dimension by recursively converting to Boolean first.
     * @param from AtomicBoolean to convert
     * @param converter Converter instance
     * @return Dimension instance
     */
    static Dimension atomicBooleanToDimension(Object from, Converter converter) {
        AtomicBoolean atomic = (AtomicBoolean) from;
        return converter.convert(atomic.get(), Dimension.class);
    }

    // atomicIntegerToPoint removed - now bridged via AtomicInteger → Integer → Point

    // atomicLongToPoint removed - now bridged via AtomicLong → Long → Point

    /**
     * Convert AtomicBoolean to Point by recursively converting to Boolean first.
     * @param from AtomicBoolean to convert
     * @param converter Converter instance
     * @return Point instance
     */
    static Point atomicBooleanToPoint(Object from, Converter converter) {
        AtomicBoolean atomic = (AtomicBoolean) from;
        return converter.convert(atomic.get(), Point.class);
    }

    // ========================================
    // Boolean to Dimension/Point (Direct Approach)
    // ========================================

    /**
     * Convert Boolean to Dimension. false → (0,0), true → (1,1).
     * @param from Boolean to convert
     * @param converter Converter instance
     * @return Dimension instance
     */
    static Dimension booleanToDimension(Object from, Converter converter) {
        Boolean bool = (Boolean) from;
        return bool ? new Dimension(1, 1) : new Dimension(0, 0);
    }

    /**
     * Convert Boolean to Point. false → (0,0), true → (1,1).
     * @param from Boolean to convert
     * @param converter Converter instance
     * @return Point instance
     */
    static Point booleanToPoint(Object from, Converter converter) {
        Boolean bool = (Boolean) from;
        return bool ? new Point(1, 1) : new Point(0, 0);
    }

    /**
     * Convert Boolean to Rectangle. false → (0,0,0,0), true → (1,1,1,1).
     * @param from Boolean to convert
     * @param converter Converter instance
     * @return Rectangle instance
     */
    static Rectangle booleanToRectangle(Object from, Converter converter) {
        Boolean bool = (Boolean) from;
        return bool ? new Rectangle(1, 1, 1, 1) : new Rectangle(0, 0, 0, 0);
    }

    /**
     * Convert Long to Rectangle by treating as area and creating square.
     * @param from Long to convert (will be used as area for square Rectangle)
     * @param converter Converter instance
     * @return Rectangle instance with x=0, y=0, and width=height=sqrt(area)
     */
    static Rectangle longToRectangle(Object from, Converter converter) {
        Long number = (Long) from;
        if (number < 0) {
            throw new IllegalArgumentException("Rectangle area must be non-negative, got: " + number);
        }
        int side = (int) Math.sqrt(number);
        return new Rectangle(0, 0, side, side);
    }

    /**
     * Convert Integer to Rectangle by treating as area and creating square.
     * @param from Integer to convert (will be used as area for square Rectangle)
     * @param converter Converter instance
     * @return Rectangle instance with x=0, y=0, and width=height=sqrt(area)
     */
    static Rectangle integerToRectangle(Object from, Converter converter) {
        Integer number = (Integer) from;
        if (number < 0) {
            throw new IllegalArgumentException("Rectangle area must be non-negative, got: " + number);
        }
        int side = (int) Math.sqrt(number);
        return new Rectangle(0, 0, side, side);
    }

    /**
     * Convert BigInteger to Rectangle by treating as area and creating square.
     * @param from BigInteger to convert
     * @param converter Converter instance
     * @return Rectangle instance
     */
    static Rectangle bigIntegerToRectangle(Object from, Converter converter) {
        BigInteger bigInt = (BigInteger) from;
        if (bigInt.compareTo(BigInteger.ZERO) < 0) {
            throw new IllegalArgumentException("Rectangle area must be non-negative, got: " + bigInt);
        }
        // For very large numbers, cap at Integer.MAX_VALUE
        long longValue = bigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ? 
            Integer.MAX_VALUE : bigInt.longValue();
        int side = (int) Math.sqrt(longValue);
        return new Rectangle(0, 0, side, side);
    }

    // Atomic types (recursive approach)
    // atomicIntegerToRectangle removed - now bridged via AtomicInteger → Integer → Rectangle

    // atomicLongToRectangle removed - now bridged via AtomicLong → Long → Rectangle

    static Rectangle atomicBooleanToRectangle(Object from, Converter converter) {
        AtomicBoolean atomic = (AtomicBoolean) from;
        return converter.convert(atomic.get(), Rectangle.class);
    }

    // ========================================
    // Number to Insets Conversions
    // ========================================

    /**
     * Convert Long to Insets. Creates uniform insets with same value for all sides.
     * @param from Long to convert (will be used for all sides)
     * @param converter Converter instance
     * @return Insets instance with top=left=bottom=right=number
     */
    static Insets longToInsets(Object from, Converter converter) {
        Long number = (Long) from;
        int value = number.intValue();
        return new Insets(value, value, value, value);
    }

    /**
     * Convert Integer to Insets. Creates uniform insets with same value for all sides.
     * @param from Integer to convert (will be used for all sides)
     * @param converter Converter instance
     * @return Insets instance with top=left=bottom=right=number
     */
    static Insets integerToInsets(Object from, Converter converter) {
        Integer number = (Integer) from;
        return new Insets(number, number, number, number);
    }

    /**
     * Convert BigInteger to Insets. Creates uniform insets with same value for all sides.
     * @param from BigInteger to convert
     * @param converter Converter instance
     * @return Insets instance
     */
    static Insets bigIntegerToInsets(Object from, Converter converter) {
        BigInteger bigInt = (BigInteger) from;
        // For very large numbers, cap at Integer.MAX_VALUE
        int value = bigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ? 
            Integer.MAX_VALUE : bigInt.intValue();
        return new Insets(value, value, value, value);
    }

    /**
     * Convert Boolean to Insets. false → (0,0,0,0), true → (1,1,1,1).
     * @param from Boolean to convert
     * @param converter Converter instance
     * @return Insets instance
     */
    static Insets booleanToInsets(Object from, Converter converter) {
        Boolean bool = (Boolean) from;
        return bool ? new Insets(1, 1, 1, 1) : new Insets(0, 0, 0, 0);
    }

    // Atomic types (recursive approach)
    // atomicIntegerToInsets removed - now bridged via AtomicInteger → Integer → Insets

    // atomicLongToInsets removed - now bridged via AtomicLong → Long → Insets

    static Insets atomicBooleanToInsets(Object from, Converter converter) {
        AtomicBoolean atomic = (AtomicBoolean) from;
        return converter.convert(atomic.get(), Insets.class);
    }

    // ========================================
    // BigDecimal to AWT Types (Direct Approach)
    // ========================================

    /**
     * Convert BigDecimal to Point. Creates square point with both coordinates equal to the number.
     * @param from BigDecimal to convert (will be used as both x and y)  
     * @param converter Converter instance
     * @return Point instance with x = y = number
     */
    static Point bigDecimalToPoint(Object from, Converter converter) {
        BigDecimal bigDecimal = (BigDecimal) from;
        int coordinate = bigDecimal.intValue();
        return new Point(coordinate, coordinate);
    }

    /**
     * Convert BigDecimal to Dimension. Creates square dimension with both dimensions equal to the number.
     * @param from BigDecimal to convert (will be used as both width and height)  
     * @param converter Converter instance
     * @return Dimension instance with width = height = number
     */
    static Dimension bigDecimalToDimension(Object from, Converter converter) {
        BigDecimal bigDecimal = (BigDecimal) from;
        int size = bigDecimal.intValue();
        
        // Validate size (should be non-negative for Dimension)
        if (size < 0) {
            throw new IllegalArgumentException("Dimension size must be non-negative, got: " + size);
        }
        
        return new Dimension(size, size);
    }

    /**
     * Convert BigDecimal to Rectangle by treating as area and creating square.
     * @param from BigDecimal to convert (will be used as area for square Rectangle)
     * @param converter Converter instance
     * @return Rectangle instance with x=0, y=0, and width=height=sqrt(area)
     */
    static Rectangle bigDecimalToRectangle(Object from, Converter converter) {
        BigDecimal bigDecimal = (BigDecimal) from;
        if (bigDecimal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Rectangle area must be non-negative, got: " + bigDecimal);
        }
        // Convert to double for sqrt, then back to int
        double doubleValue = bigDecimal.doubleValue();
        int side = (int) Math.sqrt(doubleValue);
        return new Rectangle(0, 0, side, side);
    }

    /**
     * Convert BigDecimal to Insets. Creates uniform insets with same value for all sides.
     * @param from BigDecimal to convert (will be used for all sides)
     * @param converter Converter instance
     * @return Insets instance with top=left=bottom=right=number
     */
    static Insets bigDecimalToInsets(Object from, Converter converter) {
        BigDecimal bigDecimal = (BigDecimal) from;
        int value = bigDecimal.intValue();
        return new Insets(value, value, value, value);
    }

    /**
     * Convert Number to MonthDay. Parses the number as MMDD format.
     * For example, 1225 becomes MonthDay.of(12, 25).
     * @param from Number to convert (int, Integer, short, etc.)
     * @param converter Converter instance
     * @return MonthDay instance
     * @throws IllegalArgumentException if the number is not in valid MMDD format
     */
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