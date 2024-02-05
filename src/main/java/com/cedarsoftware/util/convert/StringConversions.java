package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.DateUtilities;
import com.cedarsoftware.util.StringUtilities;

import static com.cedarsoftware.util.ArrayUtilities.EMPTY_BYTE_ARRAY;
import static com.cedarsoftware.util.ArrayUtilities.EMPTY_CHAR_ARRAY;

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
public final class StringConversions {
    private static final BigDecimal bigDecimalMinByte = BigDecimal.valueOf(Byte.MIN_VALUE);
    private static final BigDecimal bigDecimalMaxByte = BigDecimal.valueOf(Byte.MAX_VALUE);
    private static final BigDecimal bigDecimalMinShort = BigDecimal.valueOf(Short.MIN_VALUE);
    private static final BigDecimal bigDecimalMaxShort = BigDecimal.valueOf(Short.MAX_VALUE);
    private static final BigDecimal bigDecimalMinInteger = BigDecimal.valueOf(Integer.MIN_VALUE);
    private static final BigDecimal bigDecimalMaxInteger = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigDecimal bigDecimalMaxLong = BigDecimal.valueOf(Long.MAX_VALUE);
    private static final BigDecimal bigDecimalMinLong = BigDecimal.valueOf(Long.MIN_VALUE);
    private static final Pattern MM_DD = Pattern.compile("^(\\d{1,2}).(\\d{1,2})$");

    private StringConversions() {}

    static String asString(Object from) {
        return from == null ? null : from.toString();
    }

    static Byte toByte(Object from, Converter converter, ConverterOptions options) {
        return toByte(asString(from));
    }

    private static Byte toByte(String s) {
        if (s.isEmpty()) {
            return CommonValues.BYTE_ZERO;
        }
        try {
            return Byte.valueOf(s);
        } catch (NumberFormatException e) {
            Long value = toLong(s, bigDecimalMinByte, bigDecimalMaxByte);
            if (value == null) {
                throw new IllegalArgumentException("Value '" + s + "' not parseable as a byte value or outside " + Byte.MIN_VALUE + " to " + Byte.MAX_VALUE);
            }
            return value.byteValue();
        }
    }

    static Short toShort(Object from, Converter converter, ConverterOptions options) {
        return toShort(from);
    }

    private static Short toShort(Object o) {
        String str = StringUtilities.trimToEmpty((String)o);
        if (str.isEmpty()) {
            return CommonValues.SHORT_ZERO;
        }
        try {
            return Short.valueOf(str);
        } catch (NumberFormatException e) {
            Long value = toLong(str, bigDecimalMinShort, bigDecimalMaxShort);
            if (value == null) {
                throw new IllegalArgumentException("Value '" + o + "' not parseable as a short value or outside " + Short.MIN_VALUE + " to " + Short.MAX_VALUE);
            }
            return value.shortValue();
        }
    }

    static Integer toInt(Object from, Converter converter, ConverterOptions options) {
        return toInt(from);
    }

    private static Integer toInt(Object from) {
        String str = StringUtilities.trimToEmpty(asString(from));
        if (str.isEmpty()) {
            return CommonValues.INTEGER_ZERO;
        }
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            Long value = toLong(str, bigDecimalMinInteger, bigDecimalMaxInteger);
            if (value == null) {
                throw new IllegalArgumentException("Value '" + from + "' not parseable as an int value or outside " + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE);
            }
            return value.intValue();
        }
    }

    static Long toLong(Object from, Converter converter, ConverterOptions options) {
        return toLong(from);
    }

    private static Long toLong(Object from) {
        String str = StringUtilities.trimToEmpty(asString(from));
        if (str.isEmpty()) {
            return CommonValues.LONG_ZERO;
        }

        try {
            return Long.valueOf(str);
        } catch (NumberFormatException e) {
            Long value = toLong(str, bigDecimalMinLong, bigDecimalMaxLong);
            if (value == null) {
                throw new IllegalArgumentException("Value '" + from + "' not parseable as a long value or outside " + Long.MIN_VALUE + " to " + Long.MAX_VALUE);
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
        String str = StringUtilities.trimToEmpty(asString(from));
        if (str.isEmpty()) {
            return CommonValues.FLOAT_ZERO;
        }
        try {
            return Float.valueOf(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a float value");
        }
    }

    static Double toDouble(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToEmpty(asString(from));
        if (str.isEmpty()) {
            return CommonValues.DOUBLE_ZERO;
        }
        try {
            return Double.valueOf(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a double value");
        }
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter, ConverterOptions options) {
        return new AtomicBoolean(toBoolean(asString(from)));
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter, ConverterOptions options) {
        return new AtomicInteger(toInt(from));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter, ConverterOptions options) {
        return new AtomicLong(toLong(from));
    }

    private static Boolean toBoolean(String from) {
        String str = StringUtilities.trimToEmpty(from);
        if (str.isEmpty()) {
            return false;
        }
        // faster equals check "true" and "false"
        if ("true".equals(str)) {
            return true;
        } else if ("false".equals(str)) {
            return false;
        }
        return "true".equalsIgnoreCase(str) || "t".equalsIgnoreCase(str) || "1".equals(str) || "y".equalsIgnoreCase(str);
    }

    static Boolean toBoolean(Object from, Converter converter, ConverterOptions options) {
        return toBoolean(asString(from));
    }

    static char toCharacter(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToNull(asString(from));
        if (str == null) {
            return CommonValues.CHARACTER_ZERO;
        }
        if (str.length() == 1) {
            return str.charAt(0);
        }
        // Treat as a String number, like "65" = 'A'
        return (char) Integer.parseInt(str.trim());
    }

    static BigInteger toBigInteger(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToNull(asString(from));
        if (str == null) {
            return BigInteger.ZERO;
        }
        try {
            BigDecimal bigDec = new BigDecimal(str);
            return bigDec.toBigInteger();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a BigInteger value.");
        }
    }

    static BigDecimal toBigDecimal(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToEmpty(asString(from));
        if (str.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a BigDecimal value.");
        }
    }

    static String enumToString(Object from, Converter converter, ConverterOptions options) {
        return ((Enum<?>) from).name();
    }

    static UUID toUUID(Object from, Converter converter, ConverterOptions options) {
        return UUID.fromString(((String) from).trim());
    }

    static Duration toDuration(Object from, Converter converter, ConverterOptions options) {
        return Duration.parse((String) from);
    }

    static Class<?> toClass(Object from, Converter converter, ConverterOptions options) {
        String str = ((String) from).trim();
        Class<?> clazz = ClassUtilities.forName(str, options.getClassLoader());
        if (clazz != null) {
            return clazz;
        }
        throw new IllegalArgumentException("Cannot convert String '" + str + "' to class.  Class not found.");
    }

    static MonthDay toMonthDay(Object from, Converter converter, ConverterOptions options) {
        String monthDay = (String) from;
        try {
            return MonthDay.parse(monthDay);
        }
        catch (DateTimeParseException e) {
            Matcher matcher = MM_DD.matcher(monthDay);
            if (matcher.find()) {
                String mm = matcher.group(1);
                String dd = matcher.group(2);
                return MonthDay.of(Integer.parseInt(mm), Integer.parseInt(dd));
            }
            else {
                try {
                    ZonedDateTime zdt = DateUtilities.parseDate(monthDay, options.getZoneId(), true);
                    return MonthDay.of(zdt.getMonthValue(), zdt.getDayOfMonth());
                }
                catch (Exception ex) {
                    throw new IllegalArgumentException("Unable to extract Month-Day from string: " + monthDay);
                }
            }
        }
    }

    static YearMonth toYearMonth(Object from, Converter converter, ConverterOptions options) {
        String yearMonth = (String) from;
        try {
            return YearMonth.parse(yearMonth);
        }
        catch (DateTimeParseException e) {
            try {
                ZonedDateTime zdt = DateUtilities.parseDate(yearMonth, options.getZoneId(), true);
                return YearMonth.of(zdt.getYear(), zdt.getMonthValue());
            }
            catch (Exception ex) {
                throw new IllegalArgumentException("Unable to extract Year-Month from string: " + yearMonth);
            }
        }
    }

    static Period toPeriod(Object from, Converter converter, ConverterOptions options) {
        String period = (String) from;
        try {
            return Period.parse(period);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse '" + period + "' as a Period.");
        }
    }

    static Date toDate(Object from, Converter converter, ConverterOptions options) {
        Instant instant = getInstant((String) from, options);
        if (instant == null) {
            return null;
        }
        // Bring the zonedDateTime to a user-specifiable timezone
        return Date.from(instant);
    }

    static java.sql.Date toSqlDate(Object from, Converter converter, ConverterOptions options) {
        Instant instant = getInstant((String) from, options);
        if (instant == null) {
            return null;
        }
        return new java.sql.Date(instant.toEpochMilli());
    }

    static Timestamp toTimestamp(Object from, Converter converter, ConverterOptions options) {
        Instant instant = getInstant((String) from, options);
        if (instant == null) {
            return null;
        }
        return Timestamp.from(instant);
    }

    static Calendar toCalendar(Object from, Converter converter, ConverterOptions options) {
        ZonedDateTime time = toZonedDateTime(from, options);
        return time == null ? null : GregorianCalendar.from(time);
    }

    static LocalDate toLocalDate(Object from, Converter converter, ConverterOptions options) {
        ZonedDateTime time = toZonedDateTime(from, options);
        return time == null ? null : time.toLocalDate();
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter, ConverterOptions options) {
        ZonedDateTime time = toZonedDateTime(from, options);
        return time == null ? null : time.toLocalDateTime();
    }

    static LocalTime toLocalTime(Object from, Converter converter, ConverterOptions options) {
        String str = StringUtilities.trimToNull(asString(from));
        if (str == null) {
            return null;
        }

        try {
            return LocalTime.parse(str);
        } catch (Exception e) {
            ZonedDateTime zdt = DateUtilities.parseDate(str, options.getSourceZoneIdForLocalDates(), true);
            return zdt.toLocalTime();
        }
    }

    static ZonedDateTime toZonedDateTime(Object from, ConverterOptions options) {
        Instant instant = getInstant((String) from, options);
        if (instant == null) {
            return null;
        }
        return instant.atZone(options.getZoneId());
    }


    static ZonedDateTime toZonedDateTime(Object from, Converter converter, ConverterOptions options) {
        return toZonedDateTime(from, options);
    }

    static ZoneId toZoneId(Object from, Converter converter, ConverterOptions options) {
        String s = StringUtilities.trimToNull(asString(from));
        if (s == null) {
            return null;
        }
        try {
            return ZoneId.of(s);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unknown time-zone ID: '" + s + "'");
        }
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter, ConverterOptions options) {
        String s = StringUtilities.trimToNull(asString(from));
        if (s == null) {
            return null;
        }

        try {
            return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception e) {
            return toZonedDateTime(from, options).toOffsetDateTime();
        }
    }

    static OffsetTime toOffsetTime(Object from, Converter converter, ConverterOptions options) {
        String s = StringUtilities.trimToNull(asString(from));
        if (s == null) {
            return null;
        }

        try {
            return OffsetTime.parse(s, DateTimeFormatter.ISO_OFFSET_TIME);
        } catch (Exception e) {
            return toZonedDateTime(from, options).toOffsetDateTime().toOffsetTime();
        }
    }

    static Instant toInstant(Object from, Converter converter, ConverterOptions options) {
        String s = StringUtilities.trimToNull(asString(from));
        if (s == null) {
            return null;
        }

        try {
            return Instant.parse(s);
        } catch (Exception e) {
            return getInstant(s, options);
        }
    }

    private static Instant getInstant(String from, ConverterOptions options) {
        String str = StringUtilities.trimToNull(from);
        if (str == null) {
            return null;
        }
        ZonedDateTime dateTime = DateUtilities.parseDate(str, options.getSourceZoneIdForLocalDates(), true);
        return dateTime.toInstant();
    }

    static char[] toCharArray(Object from, Converter converter, ConverterOptions options) {
        String s = asString(from);

        if (s == null || s.isEmpty()) {
            return EMPTY_CHAR_ARRAY;
        }

        return s.toCharArray();
    }

    static CharBuffer toCharBuffer(Object from, Converter converter, ConverterOptions options) {
        return CharBuffer.wrap(asString(from));
    }

    static byte[] toByteArray(Object from, ConverterOptions options) {
        String s = asString(from);

        if (s == null || s.isEmpty()) {
            return EMPTY_BYTE_ARRAY;
        }

        return s.getBytes(options.getCharset());
    }

    static byte[] toByteArray(Object from, Converter converter, ConverterOptions options) {
        return toByteArray(from, options);
    }

    static ByteBuffer toByteBuffer(Object from, Converter converter, ConverterOptions options) {
        return ByteBuffer.wrap(toByteArray(from, options));
    }

    static String toString(Object from, Converter converter, ConverterOptions options) {
        return from == null ? null : from.toString();
    }

    static StringBuffer toStringBuffer(Object from, Converter converter, ConverterOptions options) {
        return from == null ? null : new StringBuffer(from.toString());
    }

    static StringBuilder toStringBuilder(Object from, Converter converter, ConverterOptions options) {
        return from == null ? null : new StringBuilder(from.toString());
    }

    static Year toYear(Object from, Converter converter, ConverterOptions options) {
        String s = StringUtilities.trimToNull(asString(from));
        if (s == null) {
            return null;
        }

        try {
            return Year.of(Integer.parseInt(s));
        }
        catch (NumberFormatException e) {
            try {
                ZonedDateTime zdt = DateUtilities.parseDate(s, options.getZoneId(), true);
                return Year.of(zdt.getYear());
            }
            catch (Exception ex) {
                throw new IllegalArgumentException("Unable to parse 4-digit year from '" + s + "'");
            }
        }
    }
}
