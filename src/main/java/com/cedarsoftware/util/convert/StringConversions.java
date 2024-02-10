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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;
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
final class StringConversions {
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

    static Byte toByte(Object from, Converter converter) {
        String s = asString(from);
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

    static Short toShort(Object from, Converter converter) {
        String str = StringUtilities.trimToEmpty((String) from);
        if (str.isEmpty()) {
            return CommonValues.SHORT_ZERO;
        }
        try {
            return Short.valueOf(str);
        } catch (NumberFormatException e) {
            Long value = toLong(str, bigDecimalMinShort, bigDecimalMaxShort);
            if (value == null) {
                throw new IllegalArgumentException("Value '" + from + "' not parseable as a short value or outside " + Short.MIN_VALUE + " to " + Short.MAX_VALUE);
            }
            return value.shortValue();
        }
    }

    static Integer toInt(Object from, Converter converter) {
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

    static Long toLong(Object from, Converter converter) {
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

    static Float toFloat(Object from, Converter converter) {
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

    static Double toDouble(Object from, Converter converter) {
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

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return new AtomicBoolean(toBoolean(asString(from), converter));
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter) {
        return new AtomicInteger(toInt(from, converter));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }
    
    static Boolean toBoolean(Object from, Converter converter) {
        String from1 = asString(from);
        String str = StringUtilities.trimToEmpty(from1);
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

    static char toCharacter(Object from, Converter converter) {
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

    static BigInteger toBigInteger(Object from, Converter converter) {
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

    static BigDecimal toBigDecimal(Object from, Converter converter) {
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

    static String enumToString(Object from, Converter converter) {
        return ((Enum<?>) from).name();
    }

    static UUID toUUID(Object from, Converter converter) {
        return UUID.fromString(((String) from).trim());
    }

    static Duration toDuration(Object from, Converter converter) {
        return Duration.parse((String) from);
    }

    static Class<?> toClass(Object from, Converter converter) {
        String str = ((String) from).trim();
        Class<?> clazz = ClassUtilities.forName(str, converter.getOptions().getClassLoader());
        if (clazz != null) {
            return clazz;
        }
        throw new IllegalArgumentException("Cannot convert String '" + str + "' to class.  Class not found.");
    }

    static MonthDay toMonthDay(Object from, Converter converter) {
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
                    ZonedDateTime zdt = DateUtilities.parseDate(monthDay, converter.getOptions().getZoneId(), true);
                    return MonthDay.of(zdt.getMonthValue(), zdt.getDayOfMonth());
                }
                catch (Exception ex) {
                    throw new IllegalArgumentException("Unable to extract Month-Day from string: " + monthDay);
                }
            }
        }
    }

    static YearMonth toYearMonth(Object from, Converter converter) {
        String yearMonth = (String) from;
        try {
            return YearMonth.parse(yearMonth);
        }
        catch (DateTimeParseException e) {
            try {
                ZonedDateTime zdt = DateUtilities.parseDate(yearMonth, converter.getOptions().getZoneId(), true);
                return YearMonth.of(zdt.getYear(), zdt.getMonthValue());
            }
            catch (Exception ex) {
                throw new IllegalArgumentException("Unable to extract Year-Month from string: " + yearMonth);
            }
        }
    }

    static Period toPeriod(Object from, Converter converter) {
        String period = (String) from;
        try {
            return Period.parse(period);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse '" + period + "' as a Period.");
        }
    }

    static Date toDate(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        return instant == null ? null : Date.from(instant);
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        return instant == null ? null :  new java.sql.Date(instant.toEpochMilli());
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        return instant == null ? null : new Timestamp(instant.toEpochMilli());
    }

    static Calendar toCalendar(Object from, Converter converter) {
        return parseDate(from, converter).map(GregorianCalendar::from).orElse(null);
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return parseDate(from, converter).map(ZonedDateTime::toLocalDate).orElse(null);
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return parseDate(from, converter).map(ZonedDateTime::toLocalDateTime).orElse(null);
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        String str = StringUtilities.trimToNull(asString(from));
        if (str == null) {
            return null;
        }

        try {
            return LocalTime.parse(str);
        } catch (Exception e) {
            return parseDate(str, converter).map(ZonedDateTime::toLocalTime).orElse(null);
        }
    }

    private static Optional<ZonedDateTime> parseDate(Object from, Converter converter) {
        String str = StringUtilities.trimToNull(asString(from));

        if (str == null) {
            return Optional.empty();
        }

        ZonedDateTime zonedDateTime = DateUtilities.parseDate(str, converter.getOptions().getZoneId(), true);

        if (zonedDateTime == null) {
            return Optional.empty();
        }

        return Optional.of(zonedDateTime);
    }


    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return parseDate(from, converter).orElse(null);
    }

    static ZoneId toZoneId(Object from, Converter converter) {
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

    static ZoneOffset toZoneOffset(Object from, Converter converter) {
        String s = StringUtilities.trimToNull(asString(from));
        if (s == null) {
            return null;
        }
        try {
            return ZoneOffset.of(s);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unknown time-zone offset: '" + s + "'");
        }
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        return parseDate(from, converter).map(ZonedDateTime::toOffsetDateTime).orElse(null);
    }

    static OffsetTime toOffsetTime(Object from, Converter converter) {
        String s = StringUtilities.trimToNull(asString(from));
        if (s == null) {
            return null;
        }

        try {
            return OffsetTime.parse(s, DateTimeFormatter.ISO_OFFSET_TIME);
        } catch (Exception e) {
            OffsetDateTime dateTime = toOffsetDateTime(from, converter);
            if (dateTime == null) {
                return null;
            }
            return dateTime.toOffsetTime();
        }
    }

    static Instant toInstant(Object from, Converter converter) {
        return parseDate(from, converter).map(ZonedDateTime::toInstant).orElse(null);
    }

    static char[] toCharArray(Object from, Converter converter) {
        String s = asString(from);

        if (s == null || s.isEmpty()) {
            return EMPTY_CHAR_ARRAY;
        }

        return s.toCharArray();
    }

    static CharBuffer toCharBuffer(Object from, Converter converter) {
        return CharBuffer.wrap(asString(from));
    }

    static byte[] toByteArray(Object from, Converter converter) {
        String s = asString(from);

        if (s == null || s.isEmpty()) {
            return EMPTY_BYTE_ARRAY;
        }

        return s.getBytes(converter.getOptions().getCharset());
    }
    
    static ByteBuffer toByteBuffer(Object from, Converter converter) {
        return ByteBuffer.wrap(toByteArray(from, converter));
    }

    static String toString(Object from, Converter converter) {
        return from == null ? null : from.toString();
    }

    static StringBuffer toStringBuffer(Object from, Converter converter) {
        return from == null ? null : new StringBuffer(from.toString());
    }

    static StringBuilder toStringBuilder(Object from, Converter converter) {
        return from == null ? null : new StringBuilder(from.toString());
    }

    static Year toYear(Object from, Converter converter) {
        String s = StringUtilities.trimToNull(asString(from));
        if (s == null) {
            return null;
        }

        try {
            return Year.of(Integer.parseInt(s));
        }
        catch (NumberFormatException e) {
            try {
                ZonedDateTime zdt = DateUtilities.parseDate(s, converter.getOptions().getZoneId(), true);
                return Year.of(zdt.getYear());
            }
            catch (Exception ex) {
                throw new IllegalArgumentException("Unable to parse 4-digit year from '" + s + "'");
            }
        }
    }
}
