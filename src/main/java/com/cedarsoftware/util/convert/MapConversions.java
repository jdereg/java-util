package com.cedarsoftware.util.convert;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.CollectionUtilities;
import com.cedarsoftware.util.ReflectionUtils;
import com.cedarsoftware.util.StringUtilities;

import static com.cedarsoftware.util.convert.Converter.getShortName;

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
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
final class MapConversions {
    static final String V = "_v";
    static final String VALUE = "value";
    static final String DATE = "date";
    static final String SQL_DATE = "sqlDate";
    static final String CALENDAR = "calendar";
    static final String TIMESTAMP = "timestamp";
    static final String DURATION = "duration";
    static final String INSTANT = "instant";
    static final String LOCALE = "locale";
    static final String MONTH_DAY = "monthDay";
    static final String YEAR_MONTH = "yearMonth";
    static final String PERIOD = "period";
    static final String ZONE_OFFSET = "zoneOffset";
    static final String LOCAL_DATE = "localDate";
    static final String LOCAL_TIME = "localTime";
    static final String LOCAL_DATE_TIME = "localDateTime";
    static final String OFFSET_TIME = "offsetTime";
    static final String OFFSET_DATE_TIME = "offsetDateTime";
    static final String ZONED_DATE_TIME = "zonedDateTime";
    static final String ZONE = "zone";
    static final String YEAR = "year";
    static final String EPOCH_MILLIS = "epochMillis";
    static final String MOST_SIG_BITS = "mostSigBits";
    static final String LEAST_SIG_BITS = "leastSigBits";
    static final String ID = "id";
    static final String URI_KEY = "URI";
    static final String URL_KEY = "URL";
    static final String UUID = "UUID";
    static final String CLASS = "class";
    static final String MESSAGE = "message";
    static final String DETAIL_MESSAGE = "detailMessage";
    static final String CAUSE = "cause";
    static final String CAUSE_MESSAGE = "causeMessage";
    private static final Object NO_MATCH = new Object();

    private MapConversions() {}

    private static final String[] VALUE_KEYS = {VALUE, V};

    /**
     * The common dispatch method. It extracts the value (using getValue) from the map
     * and, if found, converts it to the target type. Otherwise, it calls fromMap()
     * to throw an exception.
     */
    private static <T> T dispatch(Object from, Converter converter, Class<T> clazz, String[] keys) {
        Object value = getValue((Map<String, Object>) from, keys);
        if (value != NO_MATCH) {
            return converter.convert(value, clazz);
        }
        return fromMap(clazz, keys);
    }

    static Object toUUID(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;

        Object mostSigBits = map.get(MOST_SIG_BITS);
        Object leastSigBits = map.get(LEAST_SIG_BITS);
        if (mostSigBits != null && leastSigBits != null) {
            long most = converter.convert(mostSigBits, long.class);
            long least = converter.convert(leastSigBits, long.class);
            return new UUID(most, least);
        }

        return dispatch(from, converter, UUID.class, new String[]{UUID, VALUE, V, MOST_SIG_BITS + ", " + LEAST_SIG_BITS});
    }

    static Byte toByte(Object from, Converter converter) {
        return dispatch(from, converter, Byte.class, VALUE_KEYS);
    }

    static Short toShort(Object from, Converter converter) {
        return dispatch(from, converter, Short.class, VALUE_KEYS);
    }

    static Integer toInt(Object from, Converter converter) {
        return dispatch(from, converter, Integer.class, VALUE_KEYS);
    }

    static Long toLong(Object from, Converter converter) {
        return dispatch(from, converter, Long.class, VALUE_KEYS);
    }

    static Float toFloat(Object from, Converter converter) {
        return dispatch(from, converter, Float.class, VALUE_KEYS);
    }

    static Double toDouble(Object from, Converter converter) {
        return dispatch(from, converter, Double.class, VALUE_KEYS);
    }

    static Boolean toBoolean(Object from, Converter converter) {
        return dispatch(from, converter, Boolean.class, VALUE_KEYS);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        return dispatch(from, converter, BigDecimal.class, VALUE_KEYS);
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return dispatch(from, converter, BigInteger.class, VALUE_KEYS);
    }

    static String toString(Object from, Converter converter) {
        return dispatch(from, converter, String.class, VALUE_KEYS);
    }

    static StringBuffer toStringBuffer(Object from, Converter converter) {
        return dispatch(from, converter, StringBuffer.class, VALUE_KEYS);
    }

    static StringBuilder toStringBuilder(Object from, Converter converter) {
        return dispatch(from, converter, StringBuilder.class, VALUE_KEYS);
    }

    static Character toCharacter(Object from, Converter converter) {
        return dispatch(from, converter, char.class, VALUE_KEYS);
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter) {
        return dispatch(from, converter, AtomicInteger.class, VALUE_KEYS);
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return dispatch(from, converter, AtomicLong.class, VALUE_KEYS);
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return dispatch(from, converter, AtomicBoolean.class, VALUE_KEYS);
    }

    static Pattern toPattern(Object from, Converter converter) {
        return dispatch(from, converter, Pattern.class, VALUE_KEYS);
    }

    static Currency toCurrency(Object from, Converter converter) {
        return dispatch(from, converter, Currency.class, VALUE_KEYS);
    }

    private static final String[] SQL_DATE_KEYS = {SQL_DATE, VALUE, V, EPOCH_MILLIS};

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return dispatch(from, converter, java.sql.Date.class, SQL_DATE_KEYS);
    }

    private static final String[] DATE_KEYS = {DATE, VALUE, V, EPOCH_MILLIS};

    static Date toDate(Object from, Converter converter) {
        return dispatch(from, converter, Date.class, DATE_KEYS);
    }

    private static final String[] TIMESTAMP_KEYS = {TIMESTAMP, VALUE, V, EPOCH_MILLIS};

    static Timestamp toTimestamp(Object from, Converter converter) {
        return dispatch(from, converter, Timestamp.class, TIMESTAMP_KEYS);
    }

    // Assuming ZONE_KEYS is defined as follows:
    private static final String[] ZONE_KEYS = {ZONE, ID, VALUE, V};

    static TimeZone toTimeZone(Object from, Converter converter) {
        return dispatch(from, converter, TimeZone.class, ZONE_KEYS);
    }

    private static final String[] CALENDAR_KEYS = {CALENDAR, VALUE, V, EPOCH_MILLIS};

    static Calendar toCalendar(Object from, Converter converter) {
        return dispatch(from, converter, Calendar.class, CALENDAR_KEYS);
    }

    private static final String[] LOCALE_KEYS = {LOCALE, VALUE, V};

    static Locale toLocale(Object from, Converter converter) {
        return dispatch(from, converter, Locale.class, LOCALE_KEYS);
    }

    private static final String[] LOCAL_DATE_KEYS = {LOCAL_DATE, VALUE, V};

    static LocalDate toLocalDate(Object from, Converter converter) {
        return dispatch(from, converter, LocalDate.class, LOCAL_DATE_KEYS);
    }

    private static final String[] LOCAL_TIME_KEYS = {LOCAL_TIME, VALUE, V};

    static LocalTime toLocalTime(Object from, Converter converter) {
        return dispatch(from, converter, LocalTime.class, LOCAL_TIME_KEYS);
    }

    private static final String[] LDT_KEYS = {LOCAL_DATE_TIME, VALUE, V, EPOCH_MILLIS};

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        return dispatch(from, converter, LocalDateTime.class, LDT_KEYS);
    }

    private static final String[] OFFSET_TIME_KEYS = {OFFSET_TIME, VALUE, V};

    static OffsetTime toOffsetTime(Object from, Converter converter) {
        return dispatch(from, converter, OffsetTime.class, OFFSET_TIME_KEYS);
    }

    private static final String[] OFFSET_KEYS = {OFFSET_DATE_TIME, VALUE, V, EPOCH_MILLIS};

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        return dispatch(from, converter, OffsetDateTime.class, OFFSET_KEYS);
    }

    private static final String[] ZDT_KEYS = {ZONED_DATE_TIME, VALUE, V, EPOCH_MILLIS};

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        return dispatch(from, converter, ZonedDateTime.class, ZDT_KEYS);
    }

    private static final String[] CLASS_KEYS = {CLASS, VALUE, V};

    static Class<?> toClass(Object from, Converter converter) {
        return dispatch(from, converter, Class.class, CLASS_KEYS);
    }

    private static final String[] DURATION_KEYS = {DURATION, VALUE, V};

    static Duration toDuration(Object from, Converter converter) {
        return dispatch(from, converter, Duration.class, DURATION_KEYS);
    }

    private static final String[] INSTANT_KEYS = {INSTANT, VALUE, V};

    static Instant toInstant(Object from, Converter converter) {
        return dispatch(from, converter, Instant.class, INSTANT_KEYS);
    }

    private static final String[] MONTH_DAY_KEYS = {MONTH_DAY, VALUE, V};

    static MonthDay toMonthDay(Object from, Converter converter) {
        return dispatch(from, converter, MonthDay.class, MONTH_DAY_KEYS);
    }

    private static final String[] YEAR_MONTH_KEYS = {YEAR_MONTH, VALUE, V};

    static YearMonth toYearMonth(Object from, Converter converter) {
        return dispatch(from, converter, YearMonth.class, YEAR_MONTH_KEYS);
    }

    private static final String[] PERIOD_KEYS = {PERIOD, VALUE, V};

    static Period toPeriod(Object from, Converter converter) {
        return dispatch(from, converter, Period.class, PERIOD_KEYS);
    }

    static ZoneId toZoneId(Object from, Converter converter) {
        return dispatch(from, converter, ZoneId.class, ZONE_KEYS);
    }

    private static final String[] ZONE_OFFSET_KEYS = {ZONE_OFFSET, VALUE, V};

    static ZoneOffset toZoneOffset(Object from, Converter converter) {
        return dispatch(from, converter, ZoneOffset.class, ZONE_OFFSET_KEYS);
    }

    private static final String[] YEAR_KEYS = {YEAR, VALUE, V};

    static Year toYear(Object from, Converter converter) {
        return dispatch(from, converter, Year.class, YEAR_KEYS);
    }

    private static final String[] URL_KEYS = {URL_KEY, VALUE, V};

    static URL toURL(Object from, Converter converter) {
        return dispatch(from, converter, URL.class, URL_KEYS);
    }

    private static final String[] URI_KEYS = {URI_KEY, VALUE, V};

    static URI toURI(Object from, Converter converter) {
        return dispatch(from, converter, URI.class, URI_KEYS);
    }

    /**
     * Converts a Map to a ByteBuffer by decoding a Base64-encoded string value.
     *
     * @param from The Map containing a Base64-encoded string under "value" or "_v" key
     * @param converter The Converter instance for configuration access
     * @return A ByteBuffer containing the decoded bytes
     * @throws IllegalArgumentException If the map is missing required keys or contains invalid data
     * @throws NullPointerException If the map or its required values are null
     */
    static ByteBuffer toByteBuffer(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;

        // Check for the value in preferred order (VALUE first, then V)
        Object valueObj = map.containsKey(VALUE) ? map.get(VALUE) : map.get(V);

        if (valueObj == null) {
            throw new IllegalArgumentException("Unable to convert map to ByteBuffer: Missing or null 'value' or '_v' field");
        }

        if (!(valueObj instanceof String)) {
            throw new IllegalArgumentException("Unable to convert map to ByteBuffer: Value must be a Base64-encoded String, found: "
                    + valueObj.getClass().getName());
        }

        String base64 = (String) valueObj;

        try {
            // Decode the Base64 string into a byte array
            byte[] decoded = Base64.getDecoder().decode(base64);

            // Wrap the byte array with a ByteBuffer (creates a backed array that can be gc'd when no longer referenced)
            return ByteBuffer.wrap(decoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unable to convert map to ByteBuffer: Invalid Base64 encoding", e);
        }
    }

    static CharBuffer toCharBuffer(Object from, Converter converter) {
        return dispatch(from, converter, CharBuffer.class, VALUE_KEYS);
    }

    static Throwable toThrowable(Object from, Converter converter, Class<?> target) {
        Map<String, Object> map = (Map<String, Object>) from;
        try {
            // Determine most derived class between target and class specified in map
            Class<?> classToUse = target;
            String className = (String) map.get(CLASS);
            if (StringUtilities.hasContent(className)) {
                Class<?> mapClass = ClassUtilities.forName(className, ClassUtilities.getClassLoader(MapConversions.class));
                if (mapClass != null) {
                    // Use ClassUtilities to determine which class is more derived
                    if (ClassUtilities.computeInheritanceDistance(mapClass, target) >= 0) {
                        classToUse = mapClass;
                    }
                }
            }

            // First, handle the cause if it exists
            Throwable cause = null;
            String causeClassName = (String) map.get(CAUSE);
            String causeMessage = (String) map.get(CAUSE_MESSAGE);
            if (StringUtilities.hasContent(causeClassName)) {
                Class<?> causeClass = ClassUtilities.forName(causeClassName, ClassUtilities.getClassLoader(MapConversions.class));
                if (causeClass != null) {
                    cause = (Throwable) ClassUtilities.newInstance(converter, causeClass, Arrays.asList(causeMessage));
                }
            }

            // Prepare constructor args - message and cause if available
            List<Object> constructorArgs = new ArrayList<>();
            String message = (String) map.get(MESSAGE);
            if (message != null) {
                constructorArgs.add(message);
            } else {
                if (map.containsKey(DETAIL_MESSAGE)) {
                    constructorArgs.add(map.get(DETAIL_MESSAGE));
                }
            }

            if (cause != null) {
                constructorArgs.add(cause);
            }

            // Create the main exception using the determined class
            Throwable exception = (Throwable) ClassUtilities.newInstance(converter, classToUse, constructorArgs);

            // If cause wasn't handled in constructor, set it explicitly
            if (cause != null && exception.getCause() == null) {
                exception.initCause(cause);
            }

            // Now attempt to populate all remaining fields
            populateFields(exception, map, converter);

            // Clear the stackTrace
            exception.setStackTrace(new StackTraceElement[0]);

            return exception;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to reconstruct exception instance from map: " + map, e);
        }
    }

    private static void populateFields(Throwable exception, Map<String, Object> map, Converter converter) {
        // Skip special fields we've already handled
        Set<String> skipFields = CollectionUtilities.setOf(CAUSE, CAUSE_MESSAGE, MESSAGE, "stackTrace");

        // Get all fields as a Map for O(1) lookup, excluding fields we want to skip
        Map<String, Field> fieldMap = ReflectionUtils.getAllDeclaredFieldsMap(
                exception.getClass(),
                field -> !skipFields.contains(field.getName())
        );

        // Process each map entry
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();
            Field field = fieldMap.get(fieldName);

            if (field != null) {
                try {
                    // Convert value to field type if needed
                    Object convertedValue = value;
                    if (value != null && !field.getType().isAssignableFrom(value.getClass())) {
                        convertedValue = converter.convert(value, field.getType());
                    }
                    field.set(exception, convertedValue);
                } catch (Exception ignored) {
                    // Silently ignore field population errors
                }
            }
        }
    }

    static Map<String, ?> initMap(Object from, Converter converter) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(V, from);
        return map;
    }

    /**
     * Throws an IllegalArgumentException that tells the user which keys are needed.
     *
     * @param type the target type for conversion
     * @param keys one or more arrays of alternative keys (e.g. {"value", "_v"})
     * @param <T> target type (unused because the method always throws)
     * @return nothing—it always throws.
     */
    private static <T> T fromMap(Class<T> type, String[] keys) {
        // Build the message.
        StringBuilder builder = new StringBuilder();
        builder.append("To convert from Map to '")
                .append(getShortName(type))
                .append("' the map must include: ");
        builder.append(formatKeys(keys));
        builder.append(" as key with associated value.");

        throw new IllegalArgumentException(builder.toString());
    }

    /**
     * Formats an array of keys into a natural-language list.
     * <ul>
     *   <li>1 key: [oneKey]</li>
     *   <li>2 keys: [oneKey] or [twoKey]</li>
     *   <li>3+ keys: [oneKey], [twoKey], or [threeKey]</li>
     * </ul>
     *
     * @param keys an array of keys
     * @return a formatted String with each key in square brackets
     */
    private static String formatKeys(String[] keys) {
        if (keys == null || keys.length == 0) {
            return "";
        }
        if (keys.length == 1) {
            return "[" + keys[0] + "]";
        }
        if (keys.length == 2) {
            return "[" + keys[0] + "] or [" + keys[1] + "]";
        }
        // For 3 or more keys:
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                // Before the last element, prepend ", or " (if it is the last) or ", " (if not)
                if (i == keys.length - 1) {
                    sb.append(", or ");
                } else {
                    sb.append(", ");
                }
            }
            sb.append("[").append(keys[i]).append("]");
        }
        return sb.toString();
    }

    private static Object getValue(Map<String, Object> map, String[] keys) {
        String hadKey = null;
        Object value;

        for (String key : keys) {
            value = map.get(key);

            // Pick best value (if a String, it has content, if not a String, non-null)
            if (value != null && (!(value instanceof String) || StringUtilities.hasContent((String) value))) {
                return value;
            }

            // Record if there was an entry for the key
            if (map.containsKey(key)) {
                hadKey = key;
            }
        }

        if (hadKey != null) {
            return map.get(hadKey);
        }
        return NO_MATCH;
    }
}