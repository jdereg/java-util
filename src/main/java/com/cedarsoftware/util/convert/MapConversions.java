package com.cedarsoftware.util.convert;

import java.lang.reflect.Method;
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
import java.util.Base64;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.ConcurrentHashMapNullSafe;
import com.cedarsoftware.util.ConcurrentNavigableMapNullSafe;
import com.cedarsoftware.util.LoggingConfig;
import com.cedarsoftware.util.ReflectionUtils;
import com.cedarsoftware.util.StringUtilities;
import com.cedarsoftware.util.SystemUtilities;
import com.cedarsoftware.util.geom.Color;
import com.cedarsoftware.util.geom.Dimension;
import com.cedarsoftware.util.geom.Insets;
import com.cedarsoftware.util.geom.Point;
import com.cedarsoftware.util.geom.Rectangle;

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
    private static final Logger LOG = Logger.getLogger(MapConversions.class.getName());
    static { LoggingConfig.init(); }
    
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
    static final String FILE_KEY = "file";
    static final String PATH_KEY = "path";
    static final String UUID = "UUID";
    static final String CLASS = "class";
    static final String MESSAGE = "message";
    static final String DETAIL_MESSAGE = "detailMessage";
    static final String CAUSE = "cause";
    static final String CAUSE_MESSAGE = "causeMessage";
    static final String RED = "red";
    static final String GREEN = "green";
    static final String BLUE = "blue";
    static final String ALPHA = "alpha";
    static final String RGB = "rgb";
    static final String COLOR = "color";
    static final String R = "r";
    static final String G = "g";
    static final String B = "b";
    static final String A = "a";
    static final String WIDTH = "width";
    static final String HEIGHT = "height";
    static final String W = "w";
    static final String H = "h";
    static final String X = "x";
    static final String Y = "y";
    static final String TOP = "top";
    static final String LEFT = "left";
    static final String BOTTOM = "bottom";
    static final String RIGHT = "right";
    static final String FLAGS = "flags";
    private static final Object NO_MATCH = new Object();

    private MapConversions() {}

    private static final String[] VALUE_KEYS = {VALUE, V};

    /**
     * The common dispatch method. It extracts the value (using getValue) from the map
     * and, if found, converts it to the target type. Otherwise, it calls fromMap()
     * to throw an exception.
     */
    @SuppressWarnings("unchecked")
    private static <T> T dispatch(Object from, Converter converter, Class<T> clazz, String[] keys) {
        Object value = getValue((Map<String, Object>) from, keys);
        if (value != NO_MATCH) {
            return converter.convert(value, clazz);
        }
        return fromMap(clazz, keys);
    }

    @SuppressWarnings("unchecked")
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

    static BitSet toBitSet(Object from, Converter converter) {
        return dispatch(from, converter, BitSet.class, VALUE_KEYS);
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

    @SuppressWarnings("unchecked")
    static Pattern toPattern(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object flagsObj = map.get(FLAGS);
        if (flagsObj != null) {
            String pattern = (String) getValue(map, VALUE_KEYS);
            int flags = converter.convert(flagsObj, int.class);
            return Pattern.compile(pattern, flags);
        }
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

    @SuppressWarnings("unchecked")
    static Throwable toThrowable(Object from, Converter converter, Class<?> target) {
        // Handle null input - return null rather than creating an empty exception
        if (from == null) {
            return null;
        }
        Map<String, Object> map = (Map<String, Object>) from;
        // If we get an empty map, it's likely from converter trying to convert null to Exception
        // Return null instead of creating an empty exception
        if (map.isEmpty()) {
            return null;
        }

        try {
            // Make a mutable copy for safety
            Map<String, Object> namedParams = new LinkedHashMap<>(map);

            // Handle a special case where cause is specified as a class name string
            Object causeValue = namedParams.get(CAUSE);
            if (causeValue instanceof String) {
                String causeClassName = (String) causeValue;
                Object causeMessageRaw = namedParams.get(CAUSE_MESSAGE);
                String causeMessage = causeMessageRaw == null ? null : causeMessageRaw.toString();

                if (StringUtilities.hasContent(causeClassName)) {
                    Class<?> causeClass = ClassUtilities.forName(causeClassName, ClassUtilities.getClassLoader(MapConversions.class));
                    if (causeClass != null) {
                        Map<String, Object> causeMap = new LinkedHashMap<>();
                        if (causeMessage != null) {
                            causeMap.put(MESSAGE, causeMessage);
                        }

                        // Recursively create the cause
                        Throwable cause = (Throwable) ClassUtilities.newInstance(converter, causeClass, causeMap);
                        namedParams.put(CAUSE, cause);
                    }
                }
                // Remove the cause message since we've processed it
                namedParams.remove(CAUSE_MESSAGE);
            } else if (causeValue instanceof Map) {
                // If cause is a Map, recursively convert it
                Map<String, Object> causeMap = (Map<String, Object>) causeValue;

                // Determine the actual type of the cause
                Class<?> causeType = Throwable.class;
                String causeClassName = (String) causeMap.get("@type");
                if (causeClassName == null) {
                    causeClassName = (String) causeMap.get(CLASS);
                }

                if (StringUtilities.hasContent(causeClassName)) {
                    Class<?> specifiedClass = ClassUtilities.forName(causeClassName, ClassUtilities.getClassLoader(MapConversions.class));
                    if (specifiedClass != null && Throwable.class.isAssignableFrom(specifiedClass)) {
                        causeType = specifiedClass;
                    }
                }

                Throwable cause = toThrowable(causeMap, converter, causeType);
                namedParams.put(CAUSE, cause);
            }
            // If cause is null, DON'T remove it - we need to pass null to the constructor
            // Just make sure no aliases are created for it

            // Add throwable-specific aliases to improve parameter matching
            addThrowableAliases(namedParams);

            // Remove internal fields that aren't constructor parameters
            namedParams.remove(DETAIL_MESSAGE);
            namedParams.remove("suppressed");
            namedParams.remove("stackTrace");

            // For custom exceptions with additional fields, ensure the message comes first
            // This helps with positional parameter matching when named parameters aren't available
            if (!namedParams.isEmpty() && (namedParams.containsKey("msg") || namedParams.containsKey("message"))) {
                Map<String, Object> orderedParams = new LinkedHashMap<>();

                // Put message first
                Object messageValue = namedParams.get("msg");
                if (messageValue == null) messageValue = namedParams.get("message");
                if (messageValue != null) {
                    orderedParams.put("msg", messageValue);
                    orderedParams.put("message", messageValue);
                }

                // Then add all other parameters in their original order
                for (Map.Entry<String, Object> entry : namedParams.entrySet()) {
                    if (!entry.getKey().equals("msg") && !entry.getKey().equals("message") && !entry.getKey().equals("s")) {
                        orderedParams.put(entry.getKey(), entry.getValue());
                    }
                }

                namedParams = orderedParams;
            }

            // Determine the actual class to instantiate
            Class<?> classToUse = target;
            String className = (String) namedParams.get(CLASS);
            if (StringUtilities.hasContent(className)) {
                Class<?> specifiedClass = ClassUtilities.forName(className, ClassUtilities.getClassLoader(MapConversions.class));
                if (specifiedClass != null && target.isAssignableFrom(specifiedClass)) {
                    classToUse = specifiedClass;
                }
            }

            // Remove metadata that shouldn't be constructor parameters
            namedParams.remove(CLASS);

            // Let ClassUtilities.newInstance handle everything!
            Throwable exception = (Throwable) ClassUtilities.newInstance(converter, classToUse, namedParams);

            // Clear the stack trace (as required by the original)
            exception.setStackTrace(new StackTraceElement[0]);

            return exception;

        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create " + target.getName() + " from map: " + map, e);
        }
    }
    
    private static void addThrowableAliases(Map<String, Object> namedParams) {
        // Convert null messages to empty string to match original behavior
        String[] messageFields = {DETAIL_MESSAGE, MESSAGE, "msg"};
        for (String field : messageFields) {
            if (namedParams.containsKey(field) && namedParams.get(field) == null) {
                namedParams.put(field, "");
            }
        }

        // Map detailMessage/message to msg since many constructors use 'msg' as parameter name
        if (!namedParams.containsKey("msg")) {
            Object messageValue = null;
            if (namedParams.containsKey(DETAIL_MESSAGE)) {
                messageValue = namedParams.get(DETAIL_MESSAGE);
            } else if (namedParams.containsKey(MESSAGE)) {
                messageValue = namedParams.get(MESSAGE);
            } else if (namedParams.containsKey("reason")) {
                messageValue = namedParams.get("reason");
            } else if (namedParams.containsKey("description")) {
                messageValue = namedParams.get("description");
            }

            if (messageValue != null) {
                namedParams.put("msg", messageValue);
            }
        }

        // Also ensure message exists if we have detailMessage or other variants
        if (!namedParams.containsKey(MESSAGE)) {
            Object messageValue = null;
            if (namedParams.containsKey(DETAIL_MESSAGE)) {
                messageValue = namedParams.get(DETAIL_MESSAGE);
            } else if (namedParams.containsKey("msg")) {
                messageValue = namedParams.get("msg");
            }

            if (messageValue != null) {
                namedParams.put(MESSAGE, messageValue);
            }
        }

        // For constructors that use 's' for string message
        if (!namedParams.containsKey("s")) {
            Object messageValue = namedParams.get(MESSAGE);
            if (messageValue == null) messageValue = namedParams.get("msg");
            if (messageValue == null) messageValue = namedParams.get(DETAIL_MESSAGE);

            if (messageValue != null) {
                namedParams.put("s", messageValue);
            }
        }

        // Handle cause aliases - ONLY if cause is not null
        Object causeValue = namedParams.get(CAUSE);

        // Don't create any aliases for null causes
        if (causeValue != null) {
            if (!namedParams.containsKey("rootCause")) {
                namedParams.put("rootCause", causeValue);
            }

            if (!namedParams.containsKey("throwable")) {
                namedParams.put("throwable", causeValue);
            }

            // For constructors that use 't' for throwable
            if (!namedParams.containsKey("t")) {
                namedParams.put("t", causeValue);
            }
        }

        // Handle boolean parameter aliases
        if (namedParams.containsKey("suppressionEnabled") && !namedParams.containsKey("enableSuppression")) {
            namedParams.put("enableSuppression", namedParams.get("suppressionEnabled"));
        }

        if (namedParams.containsKey("stackTraceWritable") && !namedParams.containsKey("writableStackTrace")) {
            namedParams.put("writableStackTrace", namedParams.get("stackTraceWritable"));
        }
    }
    
    /**
     * Converts a Record instance to a Map using its component names as keys.
     * Only available when running on JDK 14+ where Records are supported.
     *
     * @param from The Record instance to convert
     * @param converter The Converter instance for type conversions
     * @return A Map with component names as keys and component values as values
     * @throws IllegalArgumentException if the object is not a Record or Records are not supported
     */
    static Map<String, Object> recordToMap(Object from, Converter converter) {
        // Verify this is actually a Record using reflection (JDK 8 compatible)
        if (!isRecord(from.getClass())) {
            throw new IllegalArgumentException("Expected Record instance, got: " + from.getClass().getName());
        }
        
        Map<String, Object> target = new LinkedHashMap<>();
        
        try {
            // Use reflection to get record components (JDK 8 compatible)
            Object[] components = getRecordComponents(from.getClass());
            
            for (Object component : components) {
                // Get component name and accessor method
                String name = getRecordComponentName(component);
                Object accessor = getRecordComponentAccessor(component);
                
                // Invoke accessor to get the value using ReflectionUtils
                Object value = ReflectionUtils.call(from, (Method) accessor);
                
                target.put(name, value);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to convert Record to Map: " + from.getClass().getName(), e);
        }
        
        return target;
    }
    
    /**
     * JDK 8 compatible check for Record classes using SystemUtilities and ReflectionUtils caching.
     * Package-friendly to allow access from ObjectConversions.
     */
    static boolean isRecord(Class<?> clazz) {
        // Records are only available in JDK 14+
        if (!SystemUtilities.isJavaVersionAtLeast(14, 0)) {
            return false;
        }
        
        try {
            Method isRecord = ReflectionUtils.getMethod(Class.class, "isRecord");
            if (isRecord != null) {
                return (Boolean) ReflectionUtils.call(clazz, isRecord);
            }
            return false;
        } catch (Exception e) {
            return false; // JDK < 14 or method not available
        }
    }
    
    /**
     * JDK 8 compatible method to get record components using ReflectionUtils caching.
     */
    private static Object[] getRecordComponents(Class<?> recordClass) {
        try {
            Method getRecordComponents = ReflectionUtils.getMethod(Class.class, "getRecordComponents");
            if (getRecordComponents != null) {
                return (Object[]) ReflectionUtils.call(recordClass, getRecordComponents);
            }
            throw new IllegalArgumentException("Records not supported in this JVM version");
        } catch (Exception e) {
            throw new IllegalArgumentException("Not a record class or Records not supported: " + recordClass.getName(), e);
        }
    }
    
    /**
     * Gets the name of a record component using ReflectionUtils caching.
     */
    private static String getRecordComponentName(Object component) {
        try {
            Method getName = ReflectionUtils.getMethod(component.getClass(), "getName");
            if (getName != null) {
                return (String) ReflectionUtils.call(component, getName);
            }
            throw new IllegalArgumentException("Cannot get component name");
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to get record component name", e);
        }
    }
    
    /**
     * Gets the accessor method of a record component using ReflectionUtils caching.
     */
    private static Object getRecordComponentAccessor(Object component) {
        try {
            Method getAccessor = ReflectionUtils.getMethod(component.getClass(), "getAccessor");
            if (getAccessor != null) {
                return ReflectionUtils.call(component, getAccessor);
            }
            throw new IllegalArgumentException("Cannot get component accessor");
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to get record component accessor", e);
        }
    }

    static Map<String, ?> initMap(Object from, Converter converter) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(V, from);
        return map;
    }

    /**
     * Universal Map to Map converter that handles all source/target combinations.
     * Analyzes source characteristics and target requirements to route to appropriate conversion logic.
     */
    static Map<?, ?> mapToMapWithTarget(Object from, Converter converter, Class<?> toType) {
        if (from == null) {
            return null;
        }
        
        if (!(from instanceof Map)) {
            throw new IllegalArgumentException("Expected Map instance, got: " + from.getClass().getName());
        }
        
        Map<?, ?> sourceMap = (Map<?, ?>) from;
        
        // 1. ANALYZE SOURCE characteristics
        SourceCharacteristics source = analyzeSource(sourceMap);
        
        // 2. ANALYZE TARGET type requirements  
        TargetCharacteristics target = analyzeTarget(toType);
        
        // 3. ROUTE to appropriate conversion logic
        return routeConversion(sourceMap, source, target, converter);
    }
    
    /**
     * Analyzes source Map to determine its characteristics
     */
    @SuppressWarnings("unchecked")
    private static SourceCharacteristics analyzeSource(Map<?, ?> sourceMap) {
        SourceCharacteristics source = new SourceCharacteristics();
        
        source.size = sourceMap.size();
        source.isSortedMap = sourceMap instanceof java.util.SortedMap;
        
        // Extract comparator if sorted
        if (source.isSortedMap) {
            source.comparator = ((java.util.SortedMap<Object, Object>) sourceMap).comparator();
        }
        
        return source;
    }
    
    /**
     * Analyzes a target type to determine requirements
     */
    private static TargetCharacteristics analyzeTarget(Class<?> toType) {
        TargetCharacteristics target = new TargetCharacteristics();
        target.toType = toType;
        
        if (toType == null) {
            target.isGenericMap = true;
            return target;
        }
        
        String typeName = toType.getName();
        
        // Collections wrapper types (require static factory methods)
        // Use contains + endsWith("Map") to catch sorted/navigable variants
        // e.g., $UnmodifiableSortedMap, $UnmodifiableNavigableMap
        target.isEmptyMap = typeName.contains("$Empty") && typeName.endsWith("Map");
        target.isSingletonMap = typeName.contains("$Singleton") && typeName.endsWith("Map");
        target.isUnmodifiableMap = typeName.contains("$Unmodifiable") && typeName.endsWith("Map");
        target.isSynchronizedMap = typeName.contains("$Synchronized") && typeName.endsWith("Map");
        target.isCheckedMap = typeName.contains("$Checked") && typeName.endsWith("Map");
        
        // Interface types (need concrete implementation selection)
        target.isConcurrentMapInterface = toType.getName().equals("java.util.concurrent.ConcurrentMap");
        target.isConcurrentNavigableMapInterface = toType.getName().equals("java.util.concurrent.ConcurrentNavigableMap");
        target.isGenericMap = toType == Map.class;
        
        // Types requiring constructor arguments or special null handling
        target.isTreeMap = toType == TreeMap.class;
        target.isConcurrentSkipListMap = toType == ConcurrentSkipListMap.class;
        target.isConcurrentHashMap = toType == ConcurrentHashMap.class; // Only for null handling logic
        
        return target;
    }
    
    /**
     * Routes conversion based on source characteristics and target requirements.
     * Only handles types that ClassUtilities.newInstance() cannot create.
     */
    @SuppressWarnings("unchecked")
    private static Map<?, ?> routeConversion(Map<?, ?> sourceMap, SourceCharacteristics source, TargetCharacteristics target, Converter converter) {
        
        // ========== TYPES THAT ClassUtilities.newInstance() CANNOT HANDLE ==========
        
        // Collections wrapper types (static factory methods)
        if (target.isEmptyMap) {
            return Collections.emptyMap();
        }
        
        if (target.isSingletonMap) {
            if (source.size == 1) {
                Map.Entry<?, ?> entry = sourceMap.entrySet().iterator().next();
                return Collections.singletonMap(entry.getKey(), entry.getValue());
            } else {
                throw new IllegalArgumentException("Cannot convert Map with " + source.size + 
                    " entries to SingletonMap (requires exactly 1 entry)");
            }
        }
        
        if (target.isUnmodifiableMap) {
            Map<Object, Object> mutableCopy = new LinkedHashMap<>();
            copyEntries(sourceMap, mutableCopy, false, false);
            return Collections.unmodifiableMap(mutableCopy);
        }
        
        if (target.isSynchronizedMap) {
            Map<Object, Object> mutableCopy = new LinkedHashMap<>();
            copyEntries(sourceMap, mutableCopy, false, false);
            return Collections.synchronizedMap(mutableCopy);
        }
        
        if (target.isCheckedMap) {
            // CheckedMap requires key and value types, but we don't have them at runtime
            // Fall back to creating a regular HashMap and wrapping with Object.class types
            Map<Object, Object> mutableCopy = new LinkedHashMap<>();
            copyEntries(sourceMap, mutableCopy, false, false);
            return Collections.checkedMap(mutableCopy, Object.class, Object.class);
        }
        
        // Interface types that need concrete implementation selection
        if (target.isConcurrentMapInterface) {
            ConcurrentMap<Object, Object> result = new ConcurrentHashMapNullSafe<>();
            copyEntries(sourceMap, result, false, false);
            return result;
        }
        
        if (target.isConcurrentNavigableMapInterface) {
            ConcurrentNavigableMap<Object, Object> result = new ConcurrentNavigableMapNullSafe<>();
            copyEntries(sourceMap, result, false, false);
            return result;
        }
        
        if (target.isGenericMap) {
            Map<Object, Object> result = new LinkedHashMap<>();
            copyEntries(sourceMap, result, false, false);
            return result;
        }
        
        // Types requiring constructor arguments (comparator preservation)
        if (target.isTreeMap && source.isSortedMap && source.comparator != null) {
            Map<Object, Object> result = new TreeMap<>(source.comparator);
            copyEntries(sourceMap, result, true, false); // Skip null keys
            return result;
        }
        
        if (target.isConcurrentSkipListMap && source.isSortedMap && source.comparator != null) {
            ConcurrentNavigableMap<Object, Object> result = new ConcurrentSkipListMap<>(source.comparator);
            copyEntries(sourceMap, result, true, true); // Skip null keys and values
            return result;
        }
        
        // ========== UNIVERSAL APPROACH FOR ALL OTHER TYPES ==========
        
        try {
            Map<Object, Object> result;
            
            // Optimization: Use constructor(int initialCapacity) if available
            if (sourceMap.size() > 0 && hasIntConstructor(target.toType)) {
                result = (Map<Object, Object>) ClassUtilities.newInstance(target.toType, sourceMap.size());
            } else {
                result = (Map<Object, Object>) ClassUtilities.newInstance(target.toType, (Object) null);
            }
            
            // Determine null handling based on target type
            boolean skipNullKeys = target.isConcurrentHashMap || target.isConcurrentSkipListMap || target.isTreeMap;
            boolean skipNullValues = target.isConcurrentHashMap || target.isConcurrentSkipListMap;
            
            copyEntries(sourceMap, result, skipNullKeys, skipNullValues);
            return result;
            
        } catch (Exception e) {
            // Final fallback
            LinkedHashMap<Object, Object> result = new LinkedHashMap<>();
            copyEntries(sourceMap, result, false, false);
            return result;
        }
    }
    
    /**
     * Checks if the target Map class has a constructor that takes a single int parameter.
     * Such constructors are always for initial capacity in Map implementations.
     */
    private static boolean hasIntConstructor(Class<?> targetType) {
        try {
            return ReflectionUtils.getConstructor(targetType, int.class) != null;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Utility method to copy entries with filtering options
     */
    private static void copyEntries(Map<?, ?> source, Map<Object, Object> target, boolean skipNullKeys, boolean skipNullValues) {
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            
            // Skip null keys if requested
            if (skipNullKeys && key == null) {
                continue;
            }
            
            // Skip null values if requested  
            if (skipNullValues && value == null) {
                continue;
            }
            
            try {
                target.put(key, value);
            } catch (ClassCastException | NullPointerException e) {
                // Skip entries incompatible with target map (e.g., uncomparable keys in TreeMap,
                // null keys/values in ConcurrentHashMap)
                continue;
            }
        }
    }
    
    /**
     * Source Map characteristics
     */
    private static class SourceCharacteristics {
        int size;
        boolean isSortedMap;
        java.util.Comparator<Object> comparator;
    }
    
    /**
     * Target Map characteristics  
     */
    private static class TargetCharacteristics {
        Class<?> toType;
        
        // Collections wrapper types (require static factory methods)
        boolean isEmptyMap;
        boolean isSingletonMap;
        boolean isUnmodifiableMap;
        boolean isSynchronizedMap;
        boolean isCheckedMap;
        
        // Interface types (need concrete implementation selection)
        boolean isConcurrentMapInterface;
        boolean isConcurrentNavigableMapInterface;
        boolean isGenericMap;
        
        // Types requiring constructor arguments or special null handling
        boolean isTreeMap;
        boolean isConcurrentSkipListMap;
        boolean isConcurrentHashMap; // Only for null handling logic
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

    private static final String[] COLOR_KEYS = {COLOR, VALUE, V};
    private static final String[] DIMENSION_KEYS = {WIDTH, HEIGHT, VALUE, V};
    private static final String[] POINT_KEYS = {X, Y, VALUE, V};
    private static final String[] RECTANGLE_KEYS = {X, Y, WIDTH, HEIGHT, VALUE, V};
    private static final String[] INSETS_KEYS = {TOP, LEFT, BOTTOM, RIGHT, VALUE, V};
    private static final String[] FILE_KEYS = {FILE_KEY, VALUE, V};
    private static final String[] PATH_KEYS = {PATH_KEY, VALUE, V};

    /**
     * Converts a Map to a java.awt.Color by extracting RGB/RGBA values.
     * Supports multiple map formats:
     * - {"red": r, "green": g, "blue": b} - RGB components (alpha defaults to 255)
     * - {"red": r, "green": g, "blue": b, "alpha": a} - RGBA components
     * - {"r": r, "g": g, "b": b} - Short RGB components (alpha defaults to 255)
     * - {"r": r, "g": g, "b": b, "a": a} - Short RGBA components
     * - {"rgb": packedValue} - Packed RGB integer
     * - {"color": "hexString"} - Hex color string like "#FF8040"
     * - {"value": colorValue} - Fallback to value-based conversion
     *
     * @param from The Map containing color data
     * @param converter The Converter instance for type conversions
     * @return A Color instance
     * @throws IllegalArgumentException if the map cannot be converted to a Color
     */
    static Color toColor(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;

        // Try full RGB components first (most explicit)
        if (map.containsKey(RED) && map.containsKey(GREEN) && map.containsKey(BLUE)) {
            int r = converter.convert(map.get(RED), int.class);
            int g = converter.convert(map.get(GREEN), int.class);
            int b = converter.convert(map.get(BLUE), int.class);
            
            if (map.containsKey(ALPHA)) {
                int a = converter.convert(map.get(ALPHA), int.class);
                return new Color(r, g, b, a);
            } else {
                return new Color(r, g, b);
            }
        }

        // Try short RGB components (r, g, b)
        if (map.containsKey(R) && map.containsKey(G) && map.containsKey(B)) {
            int r = converter.convert(map.get(R), int.class);
            int g = converter.convert(map.get(G), int.class);
            int b = converter.convert(map.get(B), int.class);
            
            if (map.containsKey(A)) {
                int a = converter.convert(map.get(A), int.class);
                return new Color(r, g, b, a);
            } else {
                return new Color(r, g, b);
            }
        }

        // Try packed RGB value
        if (map.containsKey(RGB)) {
            int rgb = converter.convert(map.get(RGB), Integer.class);
            if (map.containsKey(ALPHA)) {
                // Explicit alpha overrides any alpha bits in the packed int
                int a = converter.convert(map.get(ALPHA), int.class);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                return new Color(r, g, b, a);
            }
            return new Color(rgb);
        }

        // Try standard key-based dispatch for hex strings or other formats
        return dispatch(from, converter, Color.class, COLOR_KEYS);
    }

    /**
     * Converts a Map to a java.awt.Dimension by extracting width and height values.
     * Supports multiple map formats:
     * - {"width": w, "height": h} - Width and height components
     * - {"w": w, "h": h} - Short width and height components
     * - {"value": "800x600"} - String format value for dispatch
     *
     * @param from The Map containing dimension data
     * @param converter The Converter instance for type conversions
     * @return A Dimension instance
     * @throws IllegalArgumentException if the map cannot be converted to a Dimension
     */
    static Dimension toDimension(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;

        // Try full width/height components first (most explicit)
        if (map.containsKey(WIDTH) && map.containsKey(HEIGHT)) {
            int w = converter.convert(map.get(WIDTH), int.class);
            int h = converter.convert(map.get(HEIGHT), int.class);
            return new Dimension(w, h);
        }

        // Try short width/height components (w, h)
        if (map.containsKey(W) && map.containsKey(H)) {
            int w = converter.convert(map.get(W), int.class);
            int h = converter.convert(map.get(H), int.class);
            return new Dimension(w, h);
        }

        // Try standard key-based dispatch for string formats or other formats
        return dispatch(from, converter, Dimension.class, DIMENSION_KEYS);
    }

    /**
     * Converts a Map to a java.awt.Point by extracting x and y values.
     * Supports multiple map formats:
     * - {"x": x, "y": y} - X and Y components
     * - {"value": "(100,200)"} - String format value for dispatch
     *
     * @param from The Map containing point data
     * @param converter The Converter instance for type conversions
     * @return A Point instance
     * @throws IllegalArgumentException if the map cannot be converted to a Point
     */
    static Point toPoint(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;

        // Try x/y components (most explicit)
        if (map.containsKey(X) && map.containsKey(Y)) {
            int x = converter.convert(map.get(X), int.class);
            int y = converter.convert(map.get(Y), int.class);
            return new Point(x, y);
        }

        // Try standard key-based dispatch for string formats or other formats
        return dispatch(from, converter, Point.class, POINT_KEYS);
    }

    /**
     * Converts a Map to a java.awt.Rectangle by extracting x, y, width, and height values.
     * Supports multiple map formats:
     * - {"x": x, "y": y, "width": w, "height": h} - Full Rectangle components
     * - {"value": "(10,20,100,50)"} - String format value for dispatch
     *
     * @param from The Map containing rectangle data
     * @param converter The Converter instance for type conversions
     * @return A Rectangle instance
     * @throws IllegalArgumentException if the map cannot be converted to a Rectangle
     */
    static Rectangle toRectangle(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;

        // Try x/y/width/height components (most explicit)
        if (map.containsKey(X) && map.containsKey(Y) && map.containsKey(WIDTH) && map.containsKey(HEIGHT)) {
            int x = converter.convert(map.get(X), int.class);
            int y = converter.convert(map.get(Y), int.class);
            int width = converter.convert(map.get(WIDTH), int.class);
            int height = converter.convert(map.get(HEIGHT), int.class);
            return new Rectangle(x, y, width, height);
        }

        // Try standard key-based dispatch for string formats or other formats
        return dispatch(from, converter, Rectangle.class, RECTANGLE_KEYS);
    }

    /**
     * Converts a Map to a java.awt.Insets by extracting top, left, bottom, and right values.
     * Supports multiple map formats:
     * - {"top": t, "left": l, "bottom": b, "right": r} - Full Insets components
     * - {"value": "(5,10,5,10)"} - String format value for dispatch
     *
     * @param from The Map containing insets data
     * @param converter The Converter instance for type conversions
     * @return An Insets instance
     * @throws IllegalArgumentException if the map cannot be converted to Insets
     */
    static Insets toInsets(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;

        // Try top/left/bottom/right components (most explicit)
        if (map.containsKey(TOP) && map.containsKey(LEFT) && map.containsKey(BOTTOM) && map.containsKey(RIGHT)) {
            int top = converter.convert(map.get(TOP), int.class);
            int left = converter.convert(map.get(LEFT), int.class);
            int bottom = converter.convert(map.get(BOTTOM), int.class);
            int right = converter.convert(map.get(RIGHT), int.class);
            return new Insets(top, left, bottom, right);
        }

        // Try standard key-based dispatch for string formats or other formats
        return dispatch(from, converter, Insets.class, INSETS_KEYS);
    }

    /**
     * Converts a Map to a java.io.File by extracting file path.
     * Supports multiple map formats:
     * - {"file": "/path/to/file"} - File path component
     * - {"value": "/path/to/file"} - String format value for dispatch
     *
     * @param from The Map containing file data
     * @param converter The Converter instance for type conversions
     * @return A File instance
     * @throws IllegalArgumentException if the map cannot be converted to a File
     */
    static java.io.File toFile(Object from, Converter converter) {
        return dispatch(from, converter, java.io.File.class, FILE_KEYS);
    }

    /**
     * Converts a Map to a java.nio.file.Path by extracting path.
     * Supports multiple map formats:
     * - {"path": "/path/to/file"} - Path component
     * - {"value": "/path/to/file"} - String format value for dispatch
     *
     * @param from The Map containing path data
     * @param converter The Converter instance for type conversions
     * @return A Path instance
     * @throws IllegalArgumentException if the map cannot be converted to a Path
     */
    static java.nio.file.Path toPath(Object from, Converter converter) {
        return dispatch(from, converter, java.nio.file.Path.class, PATH_KEYS);
    }
}
