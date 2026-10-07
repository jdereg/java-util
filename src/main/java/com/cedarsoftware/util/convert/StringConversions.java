package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.DateTimeException;
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
import java.util.Base64;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.DateUtilities;
import com.cedarsoftware.util.MathUtilities;
import com.cedarsoftware.util.RegexUtilities;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.geom.Color;
import com.cedarsoftware.util.geom.Dimension;
import com.cedarsoftware.util.geom.Insets;
import com.cedarsoftware.util.geom.Point;
import com.cedarsoftware.util.geom.Rectangle;

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
    private static final Pattern MM_DD = Pattern.compile("^(\\d{1,2})[-/. ](\\d{1,2})$");
    private static final Pattern allDigits = Pattern.compile("^\\d+$");

    private StringConversions() {}

    static String asString(Object from) {
        return from == null ? null : from.toString();
    }

    // NUMERIC WHITESPACE: surrounding whitespace is not part of a number, so every String -> number
    // conversion below trims before parsing. The JDK is split on this -- Double.parseDouble ignores
    // leading/trailing whitespace by contract while Integer.parseInt does not -- which left the same
    // padded value converting cleanly to Double and THROWING for Integer, Long, Short, Byte, BigInteger
    // and BigDecimal. One abstraction, one answer: " 42 " is 42 for all of them. This can only turn a
    // throw into a value: a string with no surrounding whitespace is unchanged by trim(), and a blank
    // one already returned zero through the isEmpty() guard (which is blank-aware, not just length==0).
    // Whitespace INSIDE the number is untouched and still fails, as it must -- "-  5" is not a number.
    // Deliberately NOT applied to Character, where a single space is a legitimate value that trimming
    // would turn into NUL, nor to Boolean, where the input does not throw today (see toBoolean).
    static Byte toByte(Object from, Converter converter) {
        String str = ((String) from).trim();   // NUMERIC WHITESPACE (see note above toByte)
        if (StringUtilities.isEmpty(str)) {
            return (byte)0;
        }
        long whole = parseWhole(str, Byte.MIN_VALUE, Byte.MAX_VALUE);
        if (whole != NOT_WHOLE) {
            return (byte) whole;
        }
        // anything else: decimal text, out of range, or not a number
        Long value = toLong(str, bigDecimalMinByte, bigDecimalMaxByte);
        if (value == null) {
            throw new IllegalArgumentException("Value '" + str + "' not parseable as a byte value or outside "
                    + Byte.MIN_VALUE + " to " + Byte.MAX_VALUE);
        }
        return value.byteValue();
    }

    static Short toShort(Object from, Converter converter) {
        String str = ((String) from).trim();   // NUMERIC WHITESPACE (see note above toByte)
        if (StringUtilities.isEmpty(str)) {
            return (short)0;
        }
        long whole = parseWhole(str, Short.MIN_VALUE, Short.MAX_VALUE);
        if (whole != NOT_WHOLE) {
            return (short) whole;
        }
        // anything else: decimal text, out of range, or not a number
        Long value = toLong(str, bigDecimalMinShort, bigDecimalMaxShort);
        if (value == null) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a short value or outside "
                    + Short.MIN_VALUE + " to " + Short.MAX_VALUE);
        }
        return value.shortValue();
    }

    static Integer toInt(Object from, Converter converter) {
        String str = ((String) from).trim();   // NUMERIC WHITESPACE (see note above toByte)
        if (StringUtilities.isEmpty(str)) {
            return 0;
        }
        long whole = parseWhole(str, Integer.MIN_VALUE, Integer.MAX_VALUE);
        if (whole != NOT_WHOLE) {
            return (int) whole;
        }
        // anything else: decimal text, out of range, or not a number
        Long value = toLong(str, bigDecimalMinInteger, bigDecimalMaxInteger);
        if (value == null) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as an int value or outside "
                    + Integer.MIN_VALUE + " to " + Integer.MAX_VALUE);
        }
        return value.intValue();
    }

    static Long toLong(Object from, Converter converter) {
        String str = ((String) from).trim();   // NUMERIC WHITESPACE (see note above toByte)
        if (StringUtilities.isEmpty(str)) {
            return 0L;
        }

        long whole = parseWhole(str, Long.MIN_VALUE + 1, Long.MAX_VALUE);
        if (whole != NOT_WHOLE) {
            return whole;
        }
        // anything else: decimal text, out of range, or not a number
        Long value = toLong(str, bigDecimalMinLong, bigDecimalMaxLong);
        if (value == null) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a long value or outside "
                    + Long.MIN_VALUE + " to " + Long.MAX_VALUE);
        }
        return value;
    }

    private static final long NOT_WHOLE = Long.MIN_VALUE;

    /**
     * Number text as the Number it says: whole text -- an optional sign and decimal digits -- is a Long within a
     * long's range ("42", "-007") and a BigInteger beyond it ("99999999999999999999"); any other text BigDecimal reads
     * (a fraction "5.5", an exponent "1e3", ".5") is a BigDecimal. Surrounding whitespace is ignored and blank is 0,
     * as for the other number types; the digits of another script read as their ASCII digits. Text that is no number
     * ("abc", "0x1F", "NaN") is refused. There was no String to Number conversion: a field declared Number, or a
     * type variable bounded by it, could not take number text.
     */
    static Number toNumber(Object from, Converter converter) {
        String str = asciiDigits(((String) from).trim());
        if (str.isEmpty()) {
            return 0L;
        }
        long whole = parseWhole(str, Long.MIN_VALUE + 1, Long.MAX_VALUE);
        if (whole != NOT_WHOLE) {
            return whole;
        }
        if (isWholeText(str)) {
            BigInteger big = new BigInteger(str);
            return big.bitLength() <= 63 ? (Number) big.longValue() : big;    // Long.MIN_VALUE is a Long
        }
        try {
            return MathUtilities.parseBigDecimal(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value '" + NumberConversions.shown(from) + "' is not a number", e);
        }
    }

    /** An optional sign and one or more ASCII decimal digits, nothing else. */
    private static boolean isWholeText(String s) {
        int length = s.length();
        int i = s.charAt(0) == '-' || s.charAt(0) == '+' ? 1 : 0;
        if (i == length) {
            return false;
        }
        for (; i < length; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * {@code s} as Long.parseLong reads it (an optional sign, then decimal digits, Unicode digits included) if it is
     * within [min, max], else NOT_WHOLE. One pass and no exception: valueOf() threw for any other text, and building
     * that exception was nearly all the cost of "12345.0" -> Integer (281 ns). {@code min} must exceed
     * Long.MIN_VALUE (which then takes the decimal path).
     */
    private static long parseWhole(String s, long min, long max) {
        int length = s.length();
        int i = 0;
        boolean negative = false;
        char first = s.charAt(0);
        if (first == '-' || first == '+') {
            if (length == 1) {
                return NOT_WHOLE;
            }
            negative = first == '-';
            i = 1;
        }
        long limit = negative ? -min : max;         // the magnitude allowed
        long multmax = limit / 10;
        long result = 0;
        for (; i < length; i++) {
            int digit = Character.digit(s.charAt(i), 10);
            if (digit < 0 || result > multmax) {
                return NOT_WHOLE;
            }
            result *= 10;
            if (result > limit - digit) {
                return NOT_WHOLE;
            }
            result += digit;
        }
        return negative ? -result : result;
    }

    private static Long toLong(String s, BigDecimal low, BigDecimal high) {
        try {
            BigDecimal big = parseBigDecimal(s);
            // Settle out of range, or below 1, from the digit count before setScale: truncating builds the whole
            // number, so "1e999999999" (or "1e-999999999") held a thread for minutes
            long wholeDigits = (long) big.precision() - big.scale();
            if (wholeDigits > 19) {
                return null;                // beyond any long
            }
            if (wholeDigits <= 0) {
                return 0L;                  // below 1 in magnitude: truncates to 0, within every range
            }
            big = big.setScale(0, RoundingMode.DOWN);
            if (big.compareTo(low) < 0 || big.compareTo(high) > 0) {
                return null;
            }
            return big.longValue();
        } catch (Exception e) {
            return null;
        }
    }

    static Float toFloat(Object from, Converter converter) {
        String str = ((String) from).trim();   // NUMERIC WHITESPACE (see note above toByte)
        if (StringUtilities.isEmpty(str)) {
            return 0f;
        }
        float value;
        try {
            value = MathUtilities.parseFloat(str);
        } catch (NumberFormatException e) {
            String ascii = asciiDigits(str);
            if (ascii == str) {
                throw new IllegalArgumentException("Value '" + from + "' not parseable as a float value", e);
            }
            return toFloat(ascii, converter);
        } catch (Exception e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a float value", e);
        }
        if (Float.isInfinite(value) && !namesInfinity(str)) {
            // a finite number beyond float's range: "1e300" parsed as Infinity
            throw new IllegalArgumentException("Unable to convert " + NumberConversions.shown(str) + " to float: it is"
                    + " outside " + -Float.MAX_VALUE + " to " + Float.MAX_VALUE);
        }
        return value;
    }

    static Double toDouble(Object from, Converter converter) {
        String str = ((String) from).trim();   // NUMERIC WHITESPACE (see note above toByte)
        if (StringUtilities.isEmpty(str)) {
            return 0.0;
        }
        double value;
        try {
            value = MathUtilities.parseDouble(str);
        } catch (NumberFormatException e) {
            String ascii = asciiDigits(str);
            if (ascii == str) {
                throw new IllegalArgumentException("Value '" + from + "' not parseable as a double value", e);
            }
            return toDouble(ascii, converter);
        } catch (Exception e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a double value", e);
        }
        if (Double.isInfinite(value) && !namesInfinity(str)) {
            // a finite number beyond double's range: "1e400" parsed as Infinity
            throw new IllegalArgumentException("Unable to convert " + NumberConversions.shown(str) + " to double: it"
                    + " is outside " + -Double.MAX_VALUE + " to " + Double.MAX_VALUE);
        }
        return value;
    }

    /** Text that says infinity itself ("Infinity", "-Infinity", "+inf"), rather than a number too large to hold. */
    private static boolean namesInfinity(String str) {
        return str.toLowerCase(Locale.ROOT).contains("inf") || str.indexOf('\u221E') >= 0;
    }

    static Boolean toBoolean(Object from, Converter converter) {
        String str = (String) from;
        // faster equals check "true" and "false"
        if ("true".equals(str)) {
            return true;
        } else if ("false".equals(str)) {
            return false;
        }
        // Surrounding whitespace is not part of the word -- " true " says true and converted to FALSE,
        // the same silent inversion as the spellings below rather than a rejection, because anything
        // unrecognised returns false. Trimmed only AFTER the fast path above has missed, so the common
        // unpadded case pays nothing for it. Whitespace INSIDE the word is not touched: "tr ue" is not
        // a boolean and stays false.
        str = str.trim();
        // "yes" belongs here for the same reason "y" already did. Without it the abbreviation was true and the
        // word was false -- so "y" converted to true while "Yes" converted to FALSE, silently inverting any data
        // written the long way. Text answers in the wild are written both ways and mean the same thing.
        // "on" belongs here for the same reason "yes" did, and it is the last common spelling missing:
        // it says true and converted to FALSE, because anything unrecognised returns false rather than
        // throwing. It is not an exotic form -- it is what a checkbox, a feature flag and a properties
        // file write; PostgreSQL's own boolean input accepts it alongside true/yes/t/y/1; and n-cube's
        // decision-table equality already treats on/off as a boolean pair. "off" needs no entry: it is
        // false already, though only by the catch-all, exactly as "no" is.
        return "true".equalsIgnoreCase(str) || "t".equalsIgnoreCase(str) || "1".equals(str)
                || "y".equalsIgnoreCase(str) || "yes".equalsIgnoreCase(str)
                || "on".equalsIgnoreCase(str) || "\"true\"".equalsIgnoreCase(str);
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
                int value = Integer.parseInt(str.trim());
                if (value < Character.MIN_VALUE || value > Character.MAX_VALUE) {
                    throw new IllegalArgumentException("Value '" + from + "' out of range for char (0-65535).");
                }
                return (char) value;
            } catch (IllegalArgumentException e) {
                throw e;
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
        String str = ((String) from).trim();   // NUMERIC WHITESPACE (see note above toByte)
        if (StringUtilities.isEmpty(str)) {
            return BigInteger.ZERO;
        }
        BigDecimal bigDec;
        try {
            bigDec = parseBigDecimal(str);
        } catch (Exception e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a BigInteger value.", e);
        }
        return BigDecimalConversions.wholePart(bigDec, converter);    // refuses "1e999999999" from its digit count
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        String str = ((String) from).trim();   // NUMERIC WHITESPACE (see note above toByte)
        if (StringUtilities.isEmpty(str)) {
            return BigDecimal.ZERO;
        }
        try {
            return parseBigDecimal(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value '" + from + "' not parseable as a BigDecimal value.", e);
        }
    }

    /**
     * {@code s} as a BigDecimal, reading the decimal digits of any script as their ASCII digits (see
     * {@link #asciiDigits(String)}); those are looked for only when the text does not parse as it is.
     */
    private static BigDecimal parseBigDecimal(String s) {
        try {
            return MathUtilities.parseBigDecimal(s);
        } catch (NumberFormatException e) {
            String ascii = asciiDigits(s);
            if (ascii == s) {
                throw e;
            }
            return MathUtilities.parseBigDecimal(ascii);
        }
    }

    /**
     * {@code s} with each decimal digit of another script -- Arabic-Indic, Devanagari, fullwidth -- replaced by its
     * ASCII digit, as the JDK's Integer.parseInt and new BigDecimal(String) read them; {@code s} itself when it has
     * none. Other characters, another script's decimal separator included, are left as they are. Arabic-Indic 42
     * converted to Integer, Long, Short and Year, which read such digits, but not to Double, Float, BigInteger or
     * BigDecimal, whose parser reads ASCII only.
     */
    static String asciiDigits(String s) {
        int length = s.length();
        int i = 0;
        while (i < length && s.charAt(i) < 128) {
            i++;
        }
        if (i == length) {
            return s;
        }
        char[] chars = s.toCharArray();
        boolean changed = false;
        for (; i < length; i++) {
            char c = chars[i];
            if (c >= 128) {
                int digit = Character.digit(c, 10);
                if (digit >= 0) {
                    chars[i] = (char) ('0' + digit);
                    changed = true;
                }
            }
        }
        return changed ? new String(chars) : s;
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

    static java.net.InetAddress toInetAddress(Object from, Converter converter) {
        return InetAddressConversions.fromText((String) from);
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
        if (StringUtilities.isEmpty(s)) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
        }
        try {
            return UUID.fromString(s.trim());       // surrounding whitespace is no part of a UUID, as for numbers
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to convert '" + s + "' to UUID", e);
        }
    }

    /**
     * Text that is a number, converted to the date or time type {@code type} as that number would be: integer text
     * ("1500") as a Long -- milliseconds, or nanoseconds under the precision setting -- and decimal text ("1.5") as a
     * BigDecimal, seconds. Null for any other text, which the caller parses as a date, a time or a duration. With
     * {@code dateRule}, eight digits are left to the parser, which reads "20240115" as that date. "1500" was 25
     * minutes as a Duration (seconds) but 1.5 s as an Instant (milliseconds), and as a LocalTime the time of day of an
     * instant in the Converter's zone; decimal text did not convert to an Instant, a Date or a LocalTime.
     */
    private static <T> T fromNumberText(Object from, Class<T> type, boolean dateRule, Converter converter) {
        String str = (String) from;
        int kind = numberTextKind(str);
        if (kind == NOT_A_NUMBER) {
            String ascii = asciiDigits(str);        // the digits of another script are number text too
            if (ascii != str) {
                str = ascii;
                kind = numberTextKind(str);
            }
        }
        if (kind == NOT_A_NUMBER || (kind == EIGHT_DIGITS && dateRule)) {
            return null;                // a date, a time, a duration; or eight digits that make a date (DateUtilities)
        }
        return convertNumberText(str.trim(), kind == DECIMAL, type, converter);
    }

    private static final int NOT_A_NUMBER = 0;
    private static final int INTEGER = 1;
    private static final int EIGHT_DIGITS = 2;
    private static final int DECIMAL = 3;

    /**
     * What number text {@code str} is -- {@link #INTEGER}, {@link #EIGHT_DIGITS} (unsigned), {@link #DECIMAL} -- or
     * {@link #NOT_A_NUMBER}, with surrounding whitespace ignored. Small, so the parse of a date (which fails at its
     * first separator, "2024-") pays next to nothing for it.
     */
    private static int numberTextKind(String str) {
        // one pass, reading each character once: a date stops it at its first separator ("2024-")
        int len = str.length();
        int i = 0;
        char c = 0;
        while (i < len && (c = str.charAt(i)) <= ' ') {
            i++;
        }
        if (i == len) {
            return NOT_A_NUMBER;
        }
        boolean signed = c == '-' || c == '+';
        if (signed) {
            if (++i == len) {
                return NOT_A_NUMBER;
            }
            c = str.charAt(i);
        }
        int digits = 0;
        boolean point = false;
        while (true) {
            if (c >= '0' && c <= '9') {
                digits++;
            } else if (c == '.' && !point) {
                point = true;
            } else if (c <= ' ') {
                break;                  // trailing whitespace; nothing else may follow
            } else {
                return NOT_A_NUMBER;
            }
            if (++i == len) {
                break;
            }
            c = str.charAt(i);
        }
        for (; i < len; i++) {
            if (str.charAt(i) > ' ') {
                return NOT_A_NUMBER;
            }
        }
        if (digits == 0) {
            return NOT_A_NUMBER;
        }
        if (point) {
            return DECIMAL;
        }
        return digits == 8 && !signed ? EIGHT_DIGITS : INTEGER;
    }

    private static <T> T convertNumberText(String str, boolean decimal, Class<T> type, Converter converter) {
        if (decimal) {
            return converter.convert(new BigDecimal(str), type);
        }
        long number;
        try {
            number = Long.parseLong(str);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to convert '" + NumberConversions.shown(str) + "' to "
                    + type.getSimpleName() + ": it is outside " + Long.MIN_VALUE + " to " + Long.MAX_VALUE);
        }
        return converter.convert(number, type);
    }

    static Duration toDuration(Object from, Converter converter) {
        if (!(from instanceof String)) {
            throw new IllegalArgumentException("Expected a String, but got: " + from);
        }
        Duration number = fromNumberText(from, Duration.class, false, converter);
        if (number != null) {
            return number;
        }
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
        }
        try {
            // Otherwise, try ISO-8601 parsing.
            return parseIsoDuration(str);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Unable to parse '" + str + "' as a Duration. Expected either:\n" +
                            "  - Decimal seconds (e.g., '123.456')\n" +
                            "  - ISO-8601 duration (e.g., 'PT1H2M3.456S')", e);
        }
    }

    // A seconds component strictly between -1 and 0 ("-0.5S", "-0,001s"), at the end of the duration.
    private static final Pattern NEGATIVE_SUBSECOND_COMPONENT =
            Pattern.compile("(?<=[TtHhMm])-0[.,](\\d{1,9})[Ss]$");

    /**
     * {@link Duration#parse} with JDK 8's sign bug worked around. JDK 8 drops the sign of a seconds
     * component between -1 and 0 (JDK-8054978, fixed in 9): {@code "PT-0.001S"} parses as +1 ms, so JDK 8
     * cannot even read back its own {@code Duration.ofMillis(-1).toString()}. That component is parsed
     * here -- the rest of the text still goes through {@code Duration.parse()} -- which gives JDK 9+'s
     * result on every JDK.
     */
    static Duration parseIsoDuration(String text) {
        Matcher m = NEGATIVE_SUBSECOND_COMPONENT.matcher(text);
        if (!m.find()) {
            return Duration.parse(text);
        }
        boolean negateAll = text.startsWith("-");
        String body = (negateAll || text.startsWith("+")) ? text.substring(1) : text;
        m = NEGATIVE_SUBSECOND_COMPONENT.matcher(body);
        if (!m.find()) {
            return Duration.parse(text);
        }
        String digits = m.group(1);
        long fractionNanos = Long.parseLong(digits) * POWERS_OF_TEN[9 - digits.length()];
        Duration result = Duration.parse(body.substring(0, m.start()) + "0S").minusNanos(fractionNanos);
        return negateAll ? result.negated() : result;
    }

    private static final long[] POWERS_OF_TEN = {1L, 10L, 100L, 1_000L, 10_000L, 100_000L, 1_000_000L,
            10_000_000L, 100_000_000L, 1_000_000_000L};

    static Class<?> toClass(Object from, Converter converter) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
        }
        Class<?> clazz = ClassUtilities.forName(str, converter.getOptions().getClassLoader());
        if (clazz != null) {
            return clazz;
        }
        throw new IllegalArgumentException("Cannot convert String '" + str + "' to class.  Class not found.");
    }

    /** True if {@code s} is minLength to maxLength ASCII digits and nothing else. */
    private static boolean isDigits(String s, int minLength, int maxLength) {
        int length = s.length();
        if (length < minLength || length > maxLength) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    static MonthDay toMonthDay(Object from, Converter converter) {
        String monthDay = (String) from;
        if (isDigits(monthDay, 3, 4)) {
            // MMDD, as the same number converts ("1225" is --12-25, not month 12 day 5)
            return NumberConversions.toMonthDay(Integer.parseInt(monthDay), converter);
        }
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
        if (isDigits(yearMonth, 5, 6)) {
            // YYYYMM, as the same number converts ("202401" is 2024-01; the date parser read it as epoch millis)
            return NumberConversions.toYearMonth(Integer.parseInt(yearMonth), converter);
        }
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
        if (StringUtilities.isEmpty(period)) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
        }
        try {
            return Period.parse(period);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse '" + period + "' as a Period.", e);
        }
    }

    static Date toDate(Object from, Converter converter) {
        Date number = fromNumberText(from, Date.class, true, converter);
        if (number != null) {
            return number;
        }
        ZonedDateTime zdt = parseZoned(from, converter);
        if (zdt == null) {
            return null;
        }
        return Date.from(zdt.toInstant());
    }

    private static final Pattern SIMPLE_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    /**
     * A time of day -- "13:14", "13:14:15", "13:14:15.123" -- or any date-time DateUtilities reads, of which the time
     * of day (in the string's own zone) is kept, as toSqlDate keeps the date.
     */
    static java.sql.Time toSqlTime(Object from, Converter converter) {
        String str = ((String) from).trim();
        if (str.isEmpty()) {
            return null;
        }
        java.sql.Time number = fromNumberText(str, java.sql.Time.class, false, converter);
        if (number != null) {
            return number;
        }
        try {
            return LocalTimeConversions.toSqlTime(LocalTime.parse(str), converter);
        } catch (DateTimeParseException e) {
            ZonedDateTime zdt = DateUtilities.parseDate(str, converter.getOptions().getZoneId(), true);
            return zdt == null ? null : LocalTimeConversions.toSqlTime(zdt.toLocalTime(), converter);
        }
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        String dateStr = ((String) from).trim();
        java.sql.Date number = fromNumberText(dateStr, java.sql.Date.class, true, converter);
        if (number != null) {
            return number;
        }

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
        Timestamp number = fromNumberText(from, Timestamp.class, true, converter);
        if (number != null) {
            return number;
        }
        ZonedDateTime zdt = parseZoned(from, converter);
        if (zdt == null) {
            return null;
        }
        Instant instant = zdt.toInstant();

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

        TimeZone zone = timeZoneOf(str);
        if (zone == null) {
            String other = zoneIdInOtherCase(str);
            zone = other == null ? null : timeZoneOf(other);
            if (zone == null) {
                throw unknownZone(str);
            }
        }
        return zone;
    }

    /** The TimeZone an ID names exactly, as TimeZone or ZoneId reads it, or null if neither knows it. */
    private static TimeZone timeZoneOf(String id) {
        TimeZone zone = TimeZone.getTimeZone(id);
        if (!"GMT".equals(zone.getID()) || "GMT".equals(id)) {
            return zone;
        }
        // getTimeZone answers GMT for an ID it does not know, as its Javadoc says, and so it read "+05:00" or "Z"
        // as GMT, and a misspelled zone too
        try {
            return ZoneIdConversions.timeZone(ZoneId.of(id));
        } catch (DateTimeException e) {
            return null;
        }
    }

    /** The ZoneId an ID names exactly, as ZoneId or TimeZone reads it, or null if neither knows it. */
    private static ZoneId zoneIdOf(String id) {
        try {
            return ZoneId.of(id);
        } catch (DateTimeException e) {
            TimeZone zone = TimeZone.getTimeZone(id);
            return "GMT".equals(zone.getID()) ? null : zone.toZoneId();
        }
    }

    /**
     * A zone ID given in another case, "america/new_york" or "utc+5", spelled as the JDK knows it: the zone ID that
     * differs from it only in case (no two IDs the JDK knows do), else the ID in upper case, for the prefixes and
     * the offset "z". Null if the ID is not ASCII, as no zone ID is: a Kelvin sign folds to "k" in lower case.
     */
    private static String zoneIdInOtherCase(String id) {
        for (int i = 0; i < id.length(); i++) {
            if (id.charAt(i) > 0x7F) {
                return null;
            }
        }
        String known = ZoneIdsByLowerCase.IDS.get(id.toLowerCase(Locale.ROOT));
        if (known != null) {
            return known;
        }
        String upper = id.toUpperCase(Locale.ROOT);
        return upper.equals(id) ? null : upper;
    }

    /** The zone IDs TimeZone and ZoneId know, by their lower case; built when an ID is first given in another case. */
    private static final class ZoneIdsByLowerCase {
        static final Map<String, String> IDS = new HashMap<>();

        static {
            for (String id : TimeZone.getAvailableIDs()) {
                IDS.put(id.toLowerCase(Locale.ROOT), id);
            }
            for (String id : ZoneId.getAvailableZoneIds()) {
                IDS.put(id.toLowerCase(Locale.ROOT), id);
            }
        }
    }

    private static IllegalArgumentException unknownZone(String id) {
        return new IllegalArgumentException("Unknown time-zone ID: '" + NumberConversions.shown(id) + "'");
    }

    static Calendar toCalendar(Object from, Converter converter) {
        Calendar number = fromNumberText(from, Calendar.class, true, converter);
        if (number != null) {
            return number;
        }
        ZonedDateTime zdt = parseZoned(from, converter);
        if (zdt == null) {
            return null;
        }

        if (zdt.getYear() < 2) {
            throw new IllegalArgumentException(
                    "Cannot convert to Calendar: date " + zdt + " has year less than 2. " +
                            "Due to Calendar implementation limitations, years 0 and 1 cannot be reliably represented.");
        }

        // the calendar system of the default FORMAT locale, as Calendar.getInstance and every other conversion use
        Calendar calendar = Calendar.getInstance(ZoneIdConversions.timeZone(zdt.getZone()));
        calendar.setTimeInMillis(zdt.toInstant().toEpochMilli());

        return calendar;
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        LocalDate number = fromNumberText(from, LocalDate.class, true, converter);
        if (number != null) {
            return number;
        }
        ZonedDateTime zdt = parseZoned(from, converter);
        if (zdt == null) {
            return null;
        }
        return zdt.toLocalDate();
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        LocalDateTime number = fromNumberText(from, LocalDateTime.class, true, converter);
        if (number != null) {
            return number;
        }
        ZonedDateTime zdt = parseZoned(from, converter);
        if (zdt == null) {
            return null;
        }
        return zdt.toLocalDateTime();
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        String str = (String) from;
        LocalTime number = fromNumberText(str, LocalTime.class, false, converter);
        if (number != null) {
            return number;
        }
        try {
            return LocalTime.parse(str);
        } catch (Exception e) {
            ZonedDateTime zdt = parseZoned(str, converter);
            if (zdt == null) {
                return null;
            }
            return zdt.toLocalTime();
        }
    }

    static Charset toCharset(Object from, Converter converter) {
        String name = ((String) from).trim();
        if (name.isEmpty()) {
            return null;
        }
        try {
            return Charset.forName(name);
        } catch (IllegalArgumentException e) {   // IllegalCharsetNameException, UnsupportedCharsetException
            throw new IllegalArgumentException("Unknown or unsupported Charset: '" + name + "'", e);
        }
    }

    // In StringConversion.toLocale():
    static Locale toLocale(Object from, Converter converter) {
        String str = (String)from;
        if (StringUtilities.isEmpty(str)) {
            return null;
        }
        // A language tag; Locale.toString()'s "_" between the parts read as BCP 47's "-" ("en_US" gave "und")
        return Locale.forLanguageTag(str.replace('_', '-'));
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        ZonedDateTime number = fromNumberText(from, ZonedDateTime.class, true, converter);
        if (number != null) {
            return number;
        }
        return parseZoned(from, converter);
    }

    /** The date-time text names, read by DateUtilities, for a caller that has already ruled out number text. */
    private static ZonedDateTime parseZoned(Object from, Converter converter) {
        return DateUtilities.parseDate((String) from, converter.getOptions().getZoneId(), true);
    }

    static ZoneId toZoneId(Object from, Converter converter) {
        String str = (String) from;
        if (StringUtilities.isEmpty(str)) {
            return null;
        }
        try {
            return ZoneId.of(str);
        } catch (DateTimeException e) {
            TimeZone zone = TimeZone.getTimeZone(str);
            return "GMT".equals(zone.getID()) ? zoneIdInAnyCase(str) : zone.toZoneId();
        }
    }

    /** A zone ID in another case, "america/new_york", as the ZoneId of that zone; refused if it names none. */
    private static ZoneId zoneIdInAnyCase(String id) {
        String other = zoneIdInOtherCase(id);
        ZoneId zone = other == null ? null : zoneIdOf(other);
        if (zone == null) {
            throw unknownZone(id);
        }
        return zone;
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
        OffsetDateTime number = fromNumberText(from, OffsetDateTime.class, true, converter);
        if (number != null) {
            return number;
        }
        ZonedDateTime zdt = parseZoned(from, converter);
        if (zdt == null) {
            return null;
        }
        return zdt.toOffsetDateTime();
    }

    static OffsetTime toOffsetTime(Object from, Converter converter) {
        String str = (String) from;
        OffsetTime number = fromNumberText(str, OffsetTime.class, false, converter);
        if (number != null) {
            return number;
        }
        try {
            return OffsetTime.parse(str, DateTimeFormatter.ISO_OFFSET_TIME);
        } catch (Exception e) {
            try {
                ZonedDateTime dateTime = parseZoned(from, converter);
                if (dateTime == null) {
                    return null;
                }
                return dateTime.toOffsetDateTime().toOffsetTime();
            } catch (Exception ex) {
                throw new IllegalArgumentException("Unable to parse '" + str + "' as an OffsetTime", e);
            }
        }
    }

    static Instant toInstant(Object from, Converter converter) {
        Instant number = fromNumberText(from, Instant.class, true, converter);
        if (number != null) {
            return number;
        }
        ZonedDateTime zdt = parseZoned(from, converter);
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

    /**
     * Convert a String to a {@code byte[]}: the String's bytes in the Converter's charset, the reverse of
     * {@code byte[]} to {@code String}. Nothing is guessed from what the String looks like: hex, Base64 or a list of
     * numbers stays text. (From 4.103.0 to 4.112.0 such text was decoded, and bytes that spelled it did not survive a
     * round trip.) Base64 is read from the Map form, {@code {"value": "<base64>"}}, which is how json-io writes a
     * {@code byte[]}; {@link com.cedarsoftware.util.ByteUtilities#decode(String)} reads hex.
     */
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
        str = StringUtilities.trimToNull(str);
        if (str == null) {
            return null;            // blank text is no year (it was year 0): null, as for the dates
        }
        try {
            return Year.of(converter.convert(str, int.class));
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

    // not trimmed: whitespace is part of a regex
    static Pattern toPattern(Object from, Converter converter) {
        String patternString = (String) from;
        Pattern pattern = RegexUtilities.getCachedPattern(patternString);
        if (pattern == null) {
            throw new IllegalArgumentException("Invalid regex pattern: " + patternString);
        }
        return pattern;
    }

    static Currency toCurrency(Object from, Converter converter) {
        String code = ((String) from).trim();
        if (code.isEmpty()) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
        }
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
        String str = ((String) from).trim().toLowerCase(Locale.ROOT);    // Turkish lowers WHITE's I to a dotless i
        
        if (StringUtilities.isEmpty(str)) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
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
        String lower = original.toLowerCase(Locale.ROOT);
        if (lower.startsWith("rgb(") && lower.endsWith(")")) {
            String values = original.substring(4, original.length() - 1);
            String[] components = values.split(",");
            if (components.length == 3) {
                try {
                    int r = converter.convert(components[0].trim(), int.class);
                    int g = converter.convert(components[1].trim(), int.class);
                    int b = converter.convert(components[2].trim(), int.class);
                    return new Color(r, g, b);
                } catch (NumberFormatException e) {
                    // Fall through to error
                }
            }
        }

        // Try rgba(r,g,b,a) format
        if (lower.startsWith("rgba(") && lower.endsWith(")")) {
            String values = original.substring(5, original.length() - 1);
            String[] components = values.split(",");
            if (components.length == 4) {
                try {
                    int r = converter.convert(components[0].trim(), int.class);
                    int g = converter.convert(components[1].trim(), int.class);
                    int b = converter.convert(components[2].trim(), int.class);
                    int a = converter.convert(components[3].trim(), int.class);
                    return new Color(r, g, b, a);
                } catch (NumberFormatException e) {
                    // Fall through to error
                }
            }
        }

        throw new IllegalArgumentException("Unable to parse color from string: " + from);
    }

    /**
     * Convert String to Dimension. Supports formats like:
     * - "800x600" (width x height)
     * - "800,600" (width,height)
     * - "800 600" (width height with space)
     *
     * @param from String representation of a dimension
     * @param converter Converter instance
     * @return Dimension instance
     * @throws IllegalArgumentException if the string cannot be parsed as a dimension
     */
    static Dimension toDimension(Object from, Converter converter) {
        String str = ((String) from).trim();
        
        if (StringUtilities.isEmpty(str)) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
        }

        // Try "Dimension[width=100,height=200]" or "java.awt.Dimension[width=100,height=200]" format (toString format) - check this FIRST
        if ((str.startsWith("Dimension[") || str.startsWith("java.awt.Dimension[")) && str.endsWith("]")) {
            int startIndex = str.indexOf('[') + 1;
            String content = str.substring(startIndex, str.length() - 1); // Remove prefix and "]"
            String[] parts = content.split(",");
            if (parts.length == 2) {
                String widthPart = parts[0].trim();
                String heightPart = parts[1].trim();
                
                // Extract width value: "width=100" -> "100"
                if (widthPart.startsWith("width=")) {
                    String widthValue = widthPart.substring(6).trim();
                    int width = converter.convert(widthValue, int.class);
                    
                    // Extract height value: "height=200" -> "200"
                    if (heightPart.startsWith("height=")) {
                        String heightValue = heightPart.substring(7).trim();
                        int height = converter.convert(heightValue, int.class);
                        return new Dimension(width, height);
                    }
                }
            }
        }

        // Remove parentheses if present: "(100,200)" -> "100,200"
        if (str.startsWith("(") && str.endsWith(")")) {
            str = str.substring(1, str.length() - 1).trim();
        }

        // Try "800x600" format (most common)
        if (str.contains("x")) {
            String[] components = str.split("x");
            if (components.length == 2) {
                try {
                    int width = converter.convert(components[0].trim(), int.class);
                    int height = converter.convert(components[1].trim(), int.class);
                    return new Dimension(width, height);
                } catch (NumberFormatException e) {
                    // Fall through to try other formats
                }
            }
        }
        
        // Try "800,600" format (comma-separated)
        if (str.contains(",")) {
            String[] components = str.split(",");
            if (components.length == 2) {
                try {
                    int width = converter.convert(components[0].trim(), int.class);
                    int height = converter.convert(components[1].trim(), int.class);
                    return new Dimension(width, height);
                } catch (NumberFormatException e) {
                    // Fall through to try other formats
                }
            }
        }
        
        // Try "800 600" format (space-separated)
        if (str.contains(" ")) {
            String[] components = str.split("\\s+");
            if (components.length == 2) {
                try {
                    int width = converter.convert(components[0].trim(), int.class);
                    int height = converter.convert(components[1].trim(), int.class);
                    return new Dimension(width, height);
                } catch (NumberFormatException e) {
                    // Fall through to error
                }
            }
        }

        throw new IllegalArgumentException("Unable to parse dimension from string: " + from);
    }

    /**
     * Convert String to Point. Supports formats like:
     * - "(100,200)" (parentheses with comma)
     * - "100,200" (comma-separated)
     * - "100 200" (space-separated)
     *
     * @param from String representation of a point
     * @param converter Converter instance
     * @return Point instance
     * @throws IllegalArgumentException if the string cannot be parsed as a point
     */
    static Point toPoint(Object from, Converter converter) {
        String str = ((String) from).trim();
        
        if (StringUtilities.isEmpty(str)) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
        }

        // Try "Point[x=10,y=20]" or "java.awt.Point[x=10,y=20]" format (toString format) - check this FIRST
        if ((str.startsWith("Point[") || str.startsWith("java.awt.Point[")) && str.endsWith("]")) {
            int startIndex = str.indexOf('[') + 1;
            String content = str.substring(startIndex, str.length() - 1); // Remove prefix and "]"
            String[] parts = content.split(",");
            if (parts.length == 2) {
                String xPart = parts[0].trim();
                String yPart = parts[1].trim();
                
                // Extract x value: "x=10" -> "10"
                if (xPart.startsWith("x=")) {
                    String xValue = xPart.substring(2).trim();
                    int x = converter.convert(xValue, int.class);
                    
                    // Extract y value: "y=20" -> "20"
                    if (yPart.startsWith("y=")) {
                        String yValue = yPart.substring(2).trim();
                        int y = converter.convert(yValue, int.class);
                        return new Point(x, y);
                    }
                }
            }
        }

        // Remove parentheses if present: "(100,200)" -> "100,200"
        if (str.startsWith("(") && str.endsWith(")")) {
            str = str.substring(1, str.length() - 1).trim();
        }

        // Try "100,200" format (comma-separated)
        if (str.contains(",")) {
            String[] components = str.split(",");
            if (components.length == 2) {
                int x = converter.convert(components[0].trim(), int.class);
                int y = converter.convert(components[1].trim(), int.class);
                return new Point(x, y);
            }
        }
        
        // Try "100 200" format (space-separated)
        if (str.contains(" ")) {
            String[] components = str.split("\\s+");
            if (components.length == 2) {
                int x = converter.convert(components[0].trim(), int.class);
                int y = converter.convert(components[1].trim(), int.class);
                return new Point(x, y);
            }
        }

        throw new IllegalArgumentException("Unable to parse point from string: " + from);
    }

    /**
     * Convert String to Rectangle. Supports formats like:
     * - "java.awt.Rectangle[x=10,y=20,width=100,height=200]" (toString format)
     * - "(10,20,100,50)" (parentheses with commas)
     * - "10,20,100,50" (comma-separated)
     * - "10 20 100 50" (space-separated)
     *
     * @param from String representation of a rectangle
     * @param converter Converter instance
     * @return Rectangle instance
     * @throws IllegalArgumentException if the string cannot be parsed as a rectangle
     */
    static Rectangle toRectangle(Object from, Converter converter) {
        String str = ((String) from).trim();
        
        if (StringUtilities.isEmpty(str)) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
        }

        // Try "Rectangle[x=10,y=20,width=100,height=200]" or "java.awt.Rectangle[x=10,y=20,width=100,height=200]" format (toString format) - check this FIRST
        if ((str.startsWith("Rectangle[") || str.startsWith("java.awt.Rectangle[")) && str.endsWith("]")) {
            int startIndex = str.indexOf('[') + 1;
            String content = str.substring(startIndex, str.length() - 1); // Remove prefix and "]"
            String[] parts = content.split(",");
            if (parts.length == 4) {
                try {
                    String xPart = parts[0].trim();
                    String yPart = parts[1].trim();
                    String widthPart = parts[2].trim();
                    String heightPart = parts[3].trim();
                    
                    // Extract x value: "x=10" -> "10"
                    if (xPart.startsWith("x=")) {
                        String xValue = xPart.substring(2).trim();
                        int x = converter.convert(xValue, int.class);
                        
                        // Extract y value: "y=20" -> "20"
                        if (yPart.startsWith("y=")) {
                            String yValue = yPart.substring(2).trim();
                            int y = converter.convert(yValue, int.class);
                            
                            // Extract width value: "width=100" -> "100"
                            if (widthPart.startsWith("width=")) {
                                String widthValue = widthPart.substring(6).trim();
                                int width = converter.convert(widthValue, int.class);
                                
                                // Extract height value: "height=200" -> "200"
                                if (heightPart.startsWith("height=")) {
                                    String heightValue = heightPart.substring(7).trim();
                                    int height = converter.convert(heightValue, int.class);
                                    return new Rectangle(x, y, width, height);
                                }
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Fall through to try other formats
                }
            }
        }

        // Remove parentheses if present: "(10,20,100,50)" -> "10,20,100,50"
        if (str.startsWith("(") && str.endsWith(")")) {
            str = str.substring(1, str.length() - 1).trim();
        }

        // Try "10,20,100,50" format (comma-separated)
        if (str.contains(",")) {
            String[] components = str.split(",");
            if (components.length == 4) {
                try {
                    int x = converter.convert(components[0].trim(), int.class);
                    int y = converter.convert(components[1].trim(), int.class);
                    int width = converter.convert(components[2].trim(), int.class);
                    int height = converter.convert(components[3].trim(), int.class);
                    return new Rectangle(x, y, width, height);
                } catch (NumberFormatException e) {
                    // Fall through to try other formats
                }
            }
        }
        
        // Try "10 20 100 50" format (space-separated)
        if (str.contains(" ")) {
            String[] components = str.split("\\s+");
            if (components.length == 4) {
                try {
                    int x = converter.convert(components[0].trim(), int.class);
                    int y = converter.convert(components[1].trim(), int.class);
                    int width = converter.convert(components[2].trim(), int.class);
                    int height = converter.convert(components[3].trim(), int.class);
                    return new Rectangle(x, y, width, height);
                } catch (NumberFormatException e) {
                    // Fall through to error
                }
            }
        }

        throw new IllegalArgumentException("Unable to parse rectangle from string: " + from);
    }

    /**
     * Convert String to Insets. Supports formats like:
     * - "java.awt.Insets[top=5,left=10,bottom=15,right=20]" (toString format)
     * - "(5,10,5,10)" (parentheses with commas)
     * - "5,10,5,10" (comma-separated)
     * - "5 10 5 10" (space-separated)
     *
     * @param from String representation of insets
     * @param converter Converter instance
     * @return Insets instance
     * @throws IllegalArgumentException if the string cannot be parsed as insets
     */
    static Insets toInsets(Object from, Converter converter) {
        String str = ((String) from).trim();
        
        if (StringUtilities.isEmpty(str)) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
        }

        // Try "Insets[top=5,left=10,bottom=15,right=20]" or "java.awt.Insets[top=5,left=10,bottom=15,right=20]" format (toString format) - check this FIRST
        if ((str.startsWith("Insets[") || str.startsWith("java.awt.Insets[")) && str.endsWith("]")) {
            int startIndex = str.indexOf('[') + 1;
            String content = str.substring(startIndex, str.length() - 1); // Remove prefix and "]"
            String[] parts = content.split(",");
            if (parts.length == 4) {
                try {
                    String topPart = parts[0].trim();
                    String leftPart = parts[1].trim();
                    String bottomPart = parts[2].trim();
                    String rightPart = parts[3].trim();
                    
                    // Extract top value: "top=5" -> "5"
                    if (topPart.startsWith("top=")) {
                        String topValue = topPart.substring(4).trim();
                        int top = converter.convert(topValue, int.class);
                        
                        // Extract left value: "left=10" -> "10"
                        if (leftPart.startsWith("left=")) {
                            String leftValue = leftPart.substring(5).trim();
                            int left = converter.convert(leftValue, int.class);
                            
                            // Extract bottom value: "bottom=15" -> "15"
                            if (bottomPart.startsWith("bottom=")) {
                                String bottomValue = bottomPart.substring(7).trim();
                                int bottom = converter.convert(bottomValue, int.class);
                                
                                // Extract right value: "right=20" -> "20"
                                if (rightPart.startsWith("right=")) {
                                    String rightValue = rightPart.substring(6).trim();
                                    int right = converter.convert(rightValue, int.class);
                                    return new Insets(top, left, bottom, right);
                                }
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unable to parse insets from string: " + from, e);
                }
            }
        }

        // Remove parentheses if present: "(5,10,5,10)" -> "5,10,5,10"
        if (str.startsWith("(") && str.endsWith(")")) {
            str = str.substring(1, str.length() - 1).trim();
        }

        // Try "5,10,5,10" format (comma-separated)
        if (str.contains(",")) {
            String[] components = str.split(",");
            if (components.length == 4) {
                try {
                    int top = converter.convert(components[0].trim(), int.class);
                    int left = converter.convert(components[1].trim(), int.class);
                    int bottom = converter.convert(components[2].trim(), int.class);
                    int right = converter.convert(components[3].trim(), int.class);
                    return new Insets(top, left, bottom, right);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unable to parse insets from string: " + from, e);
                }
            }
        }
        
        // Try "5 10 5 10" format (space-separated)
        if (str.contains(" ")) {
            String[] components = str.split("\\s+");
            if (components.length == 4) {
                try {
                    int top = converter.convert(components[0].trim(), int.class);
                    int left = converter.convert(components[1].trim(), int.class);
                    int bottom = converter.convert(components[2].trim(), int.class);
                    int right = converter.convert(components[3].trim(), int.class);
                    return new Insets(top, left, bottom, right);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Unable to parse insets from string: " + from, e);
                }
            }
        }

        throw new IllegalArgumentException("Unable to parse insets from string: " + from);
    }

    /**
     * Convert String to File.
     *
     * @param from String path to convert
     * @param converter Converter instance
     * @return File instance
     * @throws IllegalArgumentException if the string cannot be converted to File
     */
    static java.io.File toFile(Object from, Converter converter) {
        String str = ((String) from).trim();
        
        if (StringUtilities.isEmpty(str)) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
        }

        return new java.io.File(str);
    }

    /**
     * Convert String to Path.
     *
     * @param from String path to convert
     * @param converter Converter instance
     * @return Path instance
     * @throws IllegalArgumentException if the string cannot be converted to Path
     */
    static java.nio.file.Path toPath(Object from, Converter converter) {
        String str = ((String) from).trim();
        
        if (StringUtilities.isEmpty(str)) {
            return null;            // blank text is no value of a type without a zero: null, as for the dates
        }

        return java.nio.file.Paths.get(str);
    }
}