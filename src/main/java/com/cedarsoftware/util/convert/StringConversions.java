package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URL;
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
import java.util.Currency;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.LinkedHashMap;
import java.awt.Color;

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
    private static final Pattern allDigits = Pattern.compile("^\\d+$");

    private StringConversions() {}

    static String asString(Object from) {
        return from == null ? null : from.toString();
    }

    static Byte toByte(Object from, Converter converter) {
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return (byte)0;
        }
        try {
            return Byte.valueOf(str);
        } catch (NumberFormatException e) {
            Long value = toLong(str, bigDecimalMinByte, bigDecimalMaxByte);
            if (value == null) {
                throw new IllegalArgumentException("Value '" + str + "' not parseable as a byte value or outside " + Byte.MIN_VALUE + " to " + Byte.MAX_VALUE, e);
            }
            return value.byteValue();
        }
    }

    static Short toShort(Object from, Converter converter) {
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return (short)0;
        }
        try {
            return Short.valueOf(str);
        } catch (Exception e) {
            Long value = toLong(str, bigDecimalMinShort, bigDecimalMaxShort);
            if (value == null) {
                throw new IllegalArgumentException("Value '" + from + "' not parseable as a short value or outside " + Short.MIN_VALUE + " to " + Short.MAX_VALUE, e);
            }
            return value.shortValue();
        }
    }

    static Integer toInt(Object from, Converter converter) {
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return 0;
        }
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            Long value = toLong(str, bigDecimalMinInteger, bigDecimalMaxInteger);
            if (value == null) {
                throw new IllegalArgumentException("Value '" + from + "' not parseable as an int value or outside " + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE, e);
            }
            return value.intValue();
        }
    }

    static Long toLong(Object from, Converter converter) {
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return 0L;
        }

        try {
            return Long.valueOf(str);
        } catch (Exception e) {
            Long value = toLong(str, bigDecimalMinLong, bigDecimalMaxLong);
            if (value == null) {
                throw new IllegalArgumentException("Value '" + from + "' not parseable as a long value or outside " + Long.MIN_VALUE + " to " + Long.MAX_VALUE, e);
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
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return 0f;
        }
        try {
            return Float.valueOf(str);
        } catch (Exception e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a float value", e);
        }
    }

    static Double toDouble(Object from, Converter converter) {
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return 0.0;
        }
        try {
            return Double.valueOf(str);
        } catch (Exception e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a double value", e);
        }
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return new AtomicBoolean(toBoolean(from, converter));
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter) {
        return new AtomicInteger(toInt(from, converter));
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return new AtomicLong(toLong(from, converter));
    }
    
    static Boolean toBoolean(Object from, Converter converter) {
        String str = (String) from;
        // faster equals check "true" and "false"
        if ("true".equals(str)) {
            return true;
        } else if ("false".equals(str)) {
            return false;
        }
        return "true".equalsIgnoreCase(str) || "t".equalsIgnoreCase(str) || "1".equals(str) || "y".equalsIgnoreCase(str) || "\"true\"".equalsIgnoreCase(str);
    }

    static char toCharacter(Object from, Converter converter) {
        String str = (String)from;
        if (str.isEmpty()) {
            return (char)0;
        }
        if (str.length() == 1) {
            return str.charAt(0);
        }

        Matcher matcher = allDigits.matcher(str);
        boolean isAllDigits = matcher.matches();
        if (isAllDigits) {
            try {  // Treat as a String number, like "65" = 'A'
                return (char) Integer.parseInt(str.trim());
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to parse '" + from + "' as a Character.", e);
            }
        }

        char result = parseUnicodeEscape(str);
        return result;
    }

    private static char parseUnicodeEscape(String unicodeStr) {
        if (!unicodeStr.startsWith("\\u") || unicodeStr.length() != 6) {
            throw new IllegalArgumentException("Unable to parse '" + unicodeStr + "' as a char/Character. Invalid Unicode escape sequence." + unicodeStr);
        }
        int codePoint = Integer.parseInt(unicodeStr.substring(2), 16);
        return (char) codePoint;
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return BigInteger.ZERO;
        }
        try {
            BigDecimal bigDec = new BigDecimal(str);
            return bigDec.toBigInteger();
        } catch (Exception e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a BigInteger value.", e);
        }
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a BigDecimal value.", e);
        }
    }

    static URL toURL(Object from, Converter converter) {
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return null;
        }
        try {
            URI uri = URI.create(str);
            return uri.toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot convert String '" + str + "' to URL", e);
        }
    }

    static URI toURI(Object from, Converter converter) {
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return null;
        }
        return URI.create((String) from);
    }

    static String enumToString(Object from, Converter converter) {
        return ((Enum<?>) from).name();
    }

    static UUID toUUID(Object from, Converter converter) {
        String s = (String) from;
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to convert '" + s + "' to UUID", e);
        }
    }

    // Precompile the pattern for decimal numbers: optional minus, digits, optional decimal point with digits.
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("-?\\d+(\\.\\d+)?");

    static Duration toDuration(Object from, Converter converter) {
        if (!(from instanceof String)) {
            throw new IllegalArgumentException("Expected a String, but got: " + from);
        }
        String str = ((String) from).trim();
        try {
            // If the string matches a plain decimal number, treat it as seconds.
            if (DECIMAL_PATTERN.matcher(str).matches()) {
                BigDecimal seconds = new BigDecimal(str);
                long wholeSecs = seconds.longValue();
                long nanos = seconds.subtract(BigDecimal.valueOf(wholeSecs))
                        .multiply(BigDecimalConversions.BILLION)
                        .longValue();
                return Duration.ofSeconds(wholeSecs, nanos);
            }
            // Otherwise, try ISO-8601 parsing.
            return Duration.parse(str);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to parse '" + str + "' as a Duration. Expected either:\n" +
                            "  - Decimal seconds (e.g., '123.456')\n" +
                            "  - ISO-8601 duration (e.g., 'PT1H2M3.456S')", e);
        }
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
                    if (zdt == null) {
                        return null;
                    }
                    return MonthDay.of(zdt.getMonthValue(), zdt.getDayOfMonth());
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Unable to extract Month-Day from string: " + monthDay, ex);
                }
            }
        }
    }

    static YearMonth toYearMonth(Object from, Converter converter) {
        String yearMonth = (String) from;
        try {
            return YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            try {
                ZonedDateTime zdt = DateUtilities.parseDate(yearMonth, converter.getOptions().getZoneId(), true);
                if (zdt == null) {
                    return null;
                }
                return YearMonth.of(zdt.getYear(), zdt.getMonthValue());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Unable to extract Year-Month from string: " + yearMonth, ex);
            }
        }
    }

    static Period toPeriod(Object from, Converter converter) {
        String period = (String) from;
        try {
            return Period.parse(period);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse '" + period + "' as a Period.", e);
        }
    }

    static Date toDate(Object from, Converter converter) {
        ZonedDateTime zdt = toZonedDateTime(from, converter);
        if (zdt == null) {
            return null;
        }
        return Date.from(zdt.toInstant());
    }

    private static final Pattern SIMPLE_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        String dateStr = ((String) from).trim();

        // First try simple date format (yyyy-MM-dd)
        if (!dateStr.contains("T") && SIMPLE_DATE.matcher(dateStr).matches()) {
            return java.sql.Date.valueOf(dateStr);
        }

        // Handle ISO 8601 format
        try {
            // Parse ISO date strings while respecting any supplied zone or offset.
            if (dateStr.endsWith("Z")) {
                return java.sql.Date.valueOf(
                        Instant.parse(dateStr).atZone(ZoneOffset.UTC).toLocalDate());
            }

            ZonedDateTime zdt = ZonedDateTime.parse(dateStr);
            return java.sql.Date.valueOf(zdt.toLocalDate());
        } catch (DateTimeParseException e) {
            // If not ISO 8601, try other formats using DateUtilities
            ZonedDateTime zdt = DateUtilities.parseDate(dateStr, converter.getOptions().getZoneId(), true);
            return zdt == null ? null : java.sql.Date.valueOf(zdt.toLocalDate());
        }
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        if (instant == null) {
            return null;
        }

        // Check if the year is before 0001
        if (instant.getEpochSecond() < -62135596800L) {  // 0001-01-01T00:00:00Z
            throw new IllegalArgumentException(
                    "Cannot convert to Timestamp: date " + instant + " has year before 0001. " +
                            "java.sql.Timestamp does not support dates before year 0001.");
        }

        return Timestamp.from(instant);
    }

    static TimeZone toTimeZone(Object from, Converter converter) {
        String str = StringUtilities.trimToNull((String)from);
        if (str == null) {
            return null;
        }

        return TimeZone.getTimeZone(str);
    }

    static Calendar toCalendar(Object from, Converter converter) {
        ZonedDateTime zdt = toZonedDateTime(from, converter);
        if (zdt == null) {
            return null;
        }

        if (zdt.getYear() < 2) {
            throw new IllegalArgumentException(
                    "Cannot convert to Calendar: date " + zdt + " has year less than 2. " +
                            "Due to Calendar implementation limitations, years 0 and 1 cannot be reliably represented.");
        }

        TimeZone timeZone = TimeZone.getTimeZone(zdt.getZone());
        Locale locale = Locale.getDefault();

        // Get the appropriate calendar type for the locale
        Calendar calendar = Calendar.getInstance(timeZone, locale);
        calendar.setTimeInMillis(zdt.toInstant().toEpochMilli());

        return calendar;
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        ZonedDateTime zdt = toZonedDateTime(from, converter);
        if (zdt == null) {
            return null;
        }
        return zdt.toLocalDate();
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        ZonedDateTime zdt = toZonedDateTime(from, converter);
        if (zdt == null) {
            return null;
        }
        return zdt.toLocalDateTime();
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        String str = (String) from;
        try {
            return LocalTime.parse(str);
        } catch (Exception e) {
            ZonedDateTime zdt = toZonedDateTime(str, converter);
            if (zdt == null) {
                return null;
            }
            return zdt.toLocalTime();
        }
    }

    // In StringConversion.toLocale():
    static Locale toLocale(Object from, Converter converter) {
        String str = (String)from;
        if (StringUtilities.isEmpty(str)) {
            return null;
        }
        // Parse the string into components
        return Locale.forLanguageTag(str);
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return DateUtilities.parseDate((String)from, converter.getOptions().getZoneId(), true);
    }

    static ZoneId toZoneId(Object from, Converter converter) {
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return null;
        }
        try {
            return ZoneId.of(str);
        } catch (Exception e) {
            TimeZone tz = TimeZone.getTimeZone(str);
            if ("GMT".equals(tz.getID())) {
                throw new IllegalArgumentException("Unknown time-zone ID: '" + str + "'", e);
            } else {
                return tz.toZoneId();
            }
        }
    }

    static ZoneOffset toZoneOffset(Object from, Converter converter) {
        String str = (String)from;
        if (StringUtilities.isEmpty(str)) {
            return null;
        }
        try {
            return ZoneOffset.of(str);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown time-zone offset: '" + str + "'");
        }
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        ZonedDateTime zdt = toZonedDateTime(from, converter);
        if (zdt == null) {
            return null;
        }
        return zdt.toOffsetDateTime();
    }

    static OffsetTime toOffsetTime(Object from, Converter converter) {
        String str = (String) from;
        try {
            return OffsetTime.parse(str, DateTimeFormatter.ISO_OFFSET_TIME);
        } catch (Exception e) {
            try {
                OffsetDateTime dateTime = toOffsetDateTime(from, converter);
                if (dateTime == null) {
                    return null;
                }
                return dateTime.toOffsetTime();
            } catch (Exception ex) {
                throw new IllegalArgumentException("Unable to parse '" + str + "' as an OffsetTime", e);
            }
        }
    }

    static Instant toInstant(Object from, Converter converter) {
        ZonedDateTime zdt = toZonedDateTime(from, converter);
        if (zdt == null) {
            return null;
        }
        return zdt.toInstant();
    }

    static char[] toCharArray(Object from, Converter converter) {
        String str = from.toString();

        if (StringUtilities.isEmpty(str)) {
            return EMPTY_CHAR_ARRAY;
        }

        return str.toCharArray();
    }

    static Character[] toCharacterArray(Object from, Converter converter) {
        CharSequence s = (CharSequence) from;
        int len = s.length();
        Character[] ca = new Character[len];
        for (int i=0; i < len; i++) {
            ca[i] = s.charAt(i);
        }
        return ca;
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
        String str = (String) from;
        try {
            str = StringUtilities.trimToNull(str);
            return Year.of(Integer.parseInt(str));
        } catch (Exception e) {
            try {
                ZonedDateTime zdt = toZonedDateTime(from, converter);
                if (zdt == null) {
                    return null;
                }
                return Year.of(zdt.getYear());
            } catch (Exception ex) {
                throw new IllegalArgumentException("Unable to parse 4-digit year from '" + str + "'", e);
            }
        }
    }

    static Pattern toPattern(Object from, Converter converter) {
        return Pattern.compile(((String) from).trim());
    }

    static Currency toCurrency(Object from, Converter converter) {
        String code = ((String) from).trim();
        return Currency.getInstance(code);
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        String str = ((String) from).trim();
        
        // Special case: if the string looks like an enum name (all uppercase letters with optional underscores),
        // convert it to a Map with "name" field like enum conversions do
        if (str.matches("^[A-Z][A-Z0-9_]*$")) {
            Map<String, Object> target = new LinkedHashMap<>();
            target.put("name", str);
            return target;
        }
        
        // Otherwise, this is an unsupported conversion
        throw new IllegalArgumentException("Unsupported conversion, source type [String (" + str + ")] target type 'Map'");
    }

    /**
     * Convert String to java.awt.Color. Supports multiple formats:
     * - Hex strings: "#FF8040", "#80FF8040" (with alpha), "FF8040", "80FF8040"
     * - Color names: "red", "green", "blue", "white", "black", etc.
     * - RGB format: "rgb(255, 128, 64)" 
     * - RGBA format: "rgba(255, 128, 64, 128)"
     *
     * @param from String representation of a color
     * @param converter Converter instance
     * @return Color instance
     * @throws IllegalArgumentException if the string cannot be parsed as a color
     */
    static Color toColor(Object from, Converter converter) {
        String str = ((String) from).trim().toLowerCase();
        
        if (StringUtilities.isEmpty(str)) {
            throw new IllegalArgumentException("Cannot convert empty/null string to Color");
        }

        // Handle hex color strings (with or without #)
        if (str.startsWith("#")) {
            str = str.substring(1);
        }
        
        // Check if it's a hex string
        if (str.matches("^[0-9a-f]{6}$")) {
            // RGB format: "ff8040"
            int rgb = Integer.parseInt(str, 16);
            return new Color(rgb);
        } else if (str.matches("^[0-9a-f]{8}$")) {
            // ARGB format: "80ff8040"
            long argb = Long.parseLong(str, 16);
            int alpha = (int) ((argb >> 24) & 0xFF);
            int rgb = (int) (argb & 0xFFFFFF);
            return new Color(rgb | (alpha << 24), true);
        }

        // Handle named colors
        str = str.replace("_", "").replace(" ", "").replace("-", "");
        
        // Standard Color constants
        switch (str) {
            case "black": return Color.BLACK;
            case "blue": return Color.BLUE;
            case "cyan": return Color.CYAN;
            case "darkgray": case "darkgrey": return Color.DARK_GRAY;
            case "gray": case "grey": return Color.GRAY;
            case "green": return Color.GREEN;
            case "lightgray": case "lightgrey": return Color.LIGHT_GRAY;
            case "magenta": return Color.MAGENTA;
            case "orange": return Color.ORANGE;
            case "pink": return Color.PINK;
            case "red": return Color.RED;
            case "white": return Color.WHITE;
            case "yellow": return Color.YELLOW;
        }

        // Try rgb(r,g,b) format
        String original = ((String) from).trim();
        if (original.startsWith("rgb(") && original.endsWith(")")) {
            String values = original.substring(4, original.length() - 1);
            String[] components = values.split(",");
            if (components.length == 3) {
                try {
                    int r = Integer.parseInt(components[0].trim());
                    int g = Integer.parseInt(components[1].trim());
                    int b = Integer.parseInt(components[2].trim());
                    return new Color(r, g, b);
                } catch (NumberFormatException e) {
                    // Fall through to error
                }
            }
        }

        // Try rgba(r,g,b,a) format
        if (original.startsWith("rgba(") && original.endsWith(")")) {
            String values = original.substring(5, original.length() - 1);
            String[] components = values.split(",");
            if (components.length == 4) {
                try {
                    int r = Integer.parseInt(components[0].trim());
                    int g = Integer.parseInt(components[1].trim());
                    int b = Integer.parseInt(components[2].trim());
                    int a = Integer.parseInt(components[3].trim());
                    return new Color(r, g, b, a);
                } catch (NumberFormatException e) {
                    // Fall through to error
                }
            }
        }

        throw new IllegalArgumentException("Unable to parse color from string: " + from);
    }
}