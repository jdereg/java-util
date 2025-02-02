package com.cedarsoftware.util.convert;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.ClassUtilities;
import com.cedarsoftware.util.CollectionUtilities;
import com.cedarsoftware.util.CompactMap;
import com.cedarsoftware.util.ReflectionUtils;
import com.cedarsoftware.util.StringUtilities;

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
    static final String TIME = "time";
    static final String TIMESTAMP = "timestamp";
    static final String LOCAL_DATE = "localDate";
    static final String LOCAL_TIME = "localTime";
    static final String LOCAL_DATE_TIME = "localDateTime";
    static final String OFFSET_DATE_TIME = "offsetDateTime";
    static final String ZONED_DATE_TIME = "zonedDateTime";
    static final String ZONE = "zone";
    static final String YEAR = "year";
    static final String YEARS = "years";
    static final String MONTH = "month";
    static final String MONTHS = "months";
    static final String DAY = "day";
    static final String DAYS = "days";
    static final String HOUR = "hour";
    static final String HOURS = "hours";
    static final String MINUTE = "minute";
    static final String MINUTES = "minutes";
    static final String SECOND = "second";
    static final String SECONDS = "seconds";
    static final String EPOCH_MILLIS = "epochMillis";
    static final String NANOS = "nanos";
    static final String MOST_SIG_BITS = "mostSigBits";
    static final String LEAST_SIG_BITS = "leastSigBits";
    static final String OFFSET = "offset";
    static final String OFFSET_HOUR = "offsetHour";
    static final String OFFSET_MINUTE = "offsetMinute";
    static final String ID = "id";
    static final String LANGUAGE = "language";
    static final String COUNTRY = "country";
    static final String SCRIPT = "script";
    static final String VARIANT = "variant";
    static final String URI_KEY = "URI";
    static final String URL_KEY = "URL";
    static final String UUID = "UUID";
    static final String CLASS = "class";
    static final String MESSAGE = "message";
    static final String DETAIL_MESSAGE = "detailMessage";
    static final String CAUSE = "cause";
    static final String CAUSE_MESSAGE = "causeMessage";
    static final String OPTIONAL = " (optional)";

    private MapConversions() {}
    
    static Object toUUID(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;

        Object uuid = map.get(UUID);
        if (uuid != null) {
            return converter.convert(uuid, UUID.class);
        }

        Object mostSigBits = map.get(MOST_SIG_BITS);
        Object leastSigBits = map.get(LEAST_SIG_BITS);
        if (mostSigBits != null && leastSigBits != null) {
            long most = converter.convert(mostSigBits, long.class);
            long least = converter.convert(leastSigBits, long.class);
            return new UUID(most, least);
        }

        return fromMap(from, converter, UUID.class, new String[]{UUID}, new String[]{MOST_SIG_BITS, LEAST_SIG_BITS});
    }

    static Byte toByte(Object from, Converter converter) {
        return fromMap(from, converter, Byte.class);
    }

    static Short toShort(Object from, Converter converter) {
        return fromMap(from, converter, Short.class);
    }

    static Integer toInt(Object from, Converter converter) {
        return fromMap(from, converter, Integer.class);
    }

    static Long toLong(Object from, Converter converter) {
        return fromMap(from, converter, Long.class);
    }

    static Float toFloat(Object from, Converter converter) {
        return fromMap(from, converter, Float.class);
    }

    static Double toDouble(Object from, Converter converter) {
        return fromMap(from, converter, Double.class);
    }

    static Boolean toBoolean(Object from, Converter converter) {
        return fromMap(from, converter, Boolean.class);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        return fromMap(from, converter, BigDecimal.class);
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        return fromMap(from, converter, BigInteger.class);
    }

    static String toString(Object from, Converter converter) {
        return fromMap(from, converter, String.class);
    }

    static StringBuffer toStringBuffer(Object from, Converter converter) {
        return fromMap(from, converter, StringBuffer.class);
    }

    static StringBuilder toStringBuilder(Object from, Converter converter) {
        return fromMap(from, converter, StringBuilder.class);
    }

    static Character toCharacter(Object from, Converter converter) {
        return fromMap(from, converter, char.class);
    }

    static AtomicInteger toAtomicInteger(Object from, Converter converter) {
        return fromMap(from, converter, AtomicInteger.class);
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
        return fromMap(from, converter, AtomicLong.class);
    }

    static AtomicBoolean toAtomicBoolean(Object from, Converter converter) {
        return fromMap(from, converter, AtomicBoolean.class);
    }

    private static final String[] SQL_DATE_KEYS = {SQL_DATE, VALUE, V, EPOCH_MILLIS};

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object sqlDate = null;

        for (String key : SQL_DATE_KEYS) {
            Object candidate = map.get(key);
            if (candidate != null && (!(candidate instanceof String) || StringUtilities.hasContent((String) candidate))) {
                sqlDate = candidate;
                break;
            }
        }

        // Handle strings by delegating to the String conversion method.
        if (sqlDate instanceof String) {
            return StringConversions.toSqlDate(sqlDate, converter);
        }

        // Handle numeric values as UTC-based
        if (sqlDate instanceof Number) {
            long num = ((Number) sqlDate).longValue();
            LocalDate ld = Instant.ofEpochMilli(num)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();
            return java.sql.Date.valueOf(ld.toString());
        }

        // Fallback conversion if no valid key/value is found.
        return fromMap(from, converter, java.sql.Date.class, new String[]{SQL_DATE}, new String[]{EPOCH_MILLIS});
    }
    
    private static final String[] DATE_KEYS = {DATE, VALUE, V, EPOCH_MILLIS};

    static Date toDate(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object time = null;
        for (String key : DATE_KEYS) {
            time = map.get(key);
            if (time != null && (!(time instanceof String) || StringUtilities.hasContent((String)time))) {
                break;
            }
        }

        if (time instanceof String && StringUtilities.hasContent((String)time)) {
            return StringConversions.toDate(time, converter);
        }

        // Handle case where value is a number (epoch millis)
        if (time instanceof Number) {
            return NumberConversions.toDate(time, converter);
        }

        // Map.Entry<Long, Integer> return has key of epoch-millis
        return fromMap(from, converter, Date.class, new String[]{DATE}, new String[]{EPOCH_MILLIS});
    }

    private static final String[] TIMESTAMP_KEYS = {TIMESTAMP, VALUE, V, EPOCH_MILLIS};

    /**
     * If the time String contains seconds resolution better than milliseconds, it will be kept.  For example,
     * If the time was "08.37:16.123456789" the sub-millisecond portion here will take precedence over a separate
     * key/value of "nanos" mapped to a value.  However, if "nanos" is specific as a key/value, and the time does
     * not include nanosecond resolution, then a value > 0 specified in the "nanos" key will be incorporated into
     * the resolution of the time.
     */
    static Timestamp toTimestamp(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object time = null;
        for (String key : TIMESTAMP_KEYS) {
            time = map.get(key);
            if (time != null && (!(time instanceof String) || StringUtilities.hasContent((String) time))) {
                break;
            }
        }

        // If the 'time' value is a non-empty String, parse it
        if (time instanceof String && StringUtilities.hasContent((String) time)) {
            return StringConversions.toTimestamp(time, converter);
        }

        if (time instanceof Number) {
            return NumberConversions.toTimestamp(time, converter);
        }

        // Fallback conversion if none of the above worked
        return fromMap(from, converter, Timestamp.class, new String[]{TIMESTAMP}, new String[]{EPOCH_MILLIS});
    }

    static TimeZone toTimeZone(Object from, Converter converter) {
        return fromMap(from, converter, TimeZone.class, new String[]{ZONE});
    }

    private static final String[] CALENDAR_KEYS = {CALENDAR, VALUE, V, EPOCH_MILLIS};

    static Calendar toCalendar(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        
        Object calStr = null;
        for (String key : CALENDAR_KEYS) {
            calStr = map.get(key);
            if (calStr != null && (!(calStr instanceof String) || StringUtilities.hasContent((String)calStr))) {
                break;
            }
        }
        if (calStr instanceof String && StringUtilities.hasContent((String)calStr)) {
            return StringConversions.toCalendar(calStr, converter);
        }
        if (calStr instanceof Number) {
            return NumberConversions.toCalendar(calStr, converter);
        }

        return fromMap(from, converter, Calendar.class, new String[]{CALENDAR});
    }
    
    static Locale toLocale(Object from, Converter converter) {
        Map<String, String> map = (Map<String, String>) from;

        String language = converter.convert(map.get(LANGUAGE), String.class);
        if (StringUtilities.isEmpty(language)) {
            return fromMap(from, converter, Locale.class, new String[] {LANGUAGE, COUNTRY + OPTIONAL, SCRIPT + OPTIONAL, VARIANT + OPTIONAL});
        }
        String country = converter.convert(map.get(COUNTRY), String.class);
        String script = converter.convert(map.get(SCRIPT), String.class);
        String variant = converter.convert(map.get(VARIANT), String.class);

        Locale.Builder builder = new Locale.Builder();
        try {
            builder.setLanguage(language);
        } catch (Exception e) {
            throw new IllegalArgumentException("Locale language '" + language + "' invalid.", e);
        }
        if (StringUtilities.hasContent(country)) {
            try {
                builder.setRegion(country);
            } catch (Exception e) {
                throw new IllegalArgumentException("Locale region '" + country + "' invalid.", e);
            }
        }
        if (StringUtilities.hasContent(script)) {
            try {
                builder.setScript(script);
            } catch (Exception e) {
                throw new IllegalArgumentException("Locale script '" + script + "' invalid.", e);
            }
        }
        if (StringUtilities.hasContent(variant)) {
            try {
                builder.setVariant(variant);
            } catch (Exception e) {
                throw new IllegalArgumentException("Locale variant '" + variant + "' invalid.", e);
            }
        }
        return builder.build();
    }

    private static final String[] LOCAL_DATE_KEYS = {LOCAL_DATE, VALUE, V};

    static LocalDate toLocalDate(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object localDate = null;
        for (String key : LOCAL_DATE_KEYS) {
            localDate = map.get(key);
            if (localDate != null && (!(localDate instanceof String) || StringUtilities.hasContent((String) localDate))) {
                break;
            }
        }

        if (localDate instanceof String && StringUtilities.hasContent((String) localDate)) {
            return StringConversions.toLocalDate(localDate, converter);
        }

        if (localDate instanceof Number) {
            return NumberConversions.toLocalDate(localDate, converter);
        }

        return fromMap(from, converter, LocalDate.class, new String[]{LOCAL_DATE});
    }

    private static final String[] LOCAL_TIME_KEYS = {LOCAL_TIME, VALUE, V};

    static LocalTime toLocalTime(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object localTime = null;
        for (String key : LOCAL_TIME_KEYS) {
            localTime = map.get(key);
            if (localTime != null && (!(localTime instanceof String) || StringUtilities.hasContent((String) localTime))) {
                break;
            }
        }

        if (localTime instanceof String && StringUtilities.hasContent((String) localTime)) {
            return StringConversions.toLocalTime(localTime, converter);
        }

        if (localTime instanceof Number) {
            return LongConversions.toLocalTime(((Number)localTime).longValue(), converter);
        }

        return fromMap(from, converter, LocalTime.class, new String[]{LOCAL_TIME});
    }

    static OffsetTime toOffsetTime(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object hour = map.get(HOUR);
        Object minute = map.get(MINUTE);
        Object second = map.get(SECOND);
        Object nano = map.get(NANOS);
        Object oh = map.get(OFFSET_HOUR);
        Object om = map.get(OFFSET_MINUTE);
        if (hour != null && minute != null) {
            int h = converter.convert(hour, int.class);
            int m = converter.convert(minute, int.class);
            int s = converter.convert(second, int.class);
            int n = converter.convert(nano, int.class);
            ZoneOffset zoneOffset;
            if (oh != null && om != null) {
                int offsetHour = converter.convert(oh, int.class);
                int offsetMinute = converter.convert(om, int.class);
                try {
                    zoneOffset = ZoneOffset.ofHoursMinutes(offsetHour, offsetMinute);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Offset 'hour' and 'minute' are not correct", e);
                }
                return OffsetTime.of(h, m, s, n, zoneOffset);
            }
        }

        Object time = map.get(TIME);
        if (time != null) {
            return converter.convert(time, OffsetTime.class);
        }
        return fromMap(from, converter, OffsetTime.class, new String[] {TIME}, new String[] {HOUR, MINUTE, SECOND + OPTIONAL, NANOS + OPTIONAL, OFFSET_HOUR, OFFSET_MINUTE});
    }

    private static final String[] OFFSET_KEYS = {OFFSET_DATE_TIME, VALUE, V};

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object dateTime = null;
        for (String key : OFFSET_KEYS) {
            dateTime = map.get(key);
            if (dateTime != null && (!(dateTime instanceof String) || StringUtilities.hasContent((String) dateTime))) {
                break;
            }
        }

        // If the 'offsetDateTime' value is a non-empty String, parse it (allows for value, _v, epochMillis as String)
        if (dateTime instanceof String && StringUtilities.hasContent((String) dateTime)) {
            return StringConversions.toOffsetDateTime(dateTime, converter);
        }

        // Otherwise, if epoch_millis is provided, use it with the nanos (if any)
        Object epochMillis = map.get(EPOCH_MILLIS);
        if (epochMillis != null) {
            long ms = converter.convert(epochMillis, long.class);
            return NumberConversions.toOffsetDateTime(ms, converter);
        }
        
        return fromMap(from, converter, OffsetDateTime.class, new String[] {OFFSET_DATE_TIME}, new String[] {EPOCH_MILLIS});
    }

    private static final String[] LDT_KEYS = {LOCAL_DATE_TIME, VALUE, V, EPOCH_MILLIS};

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object dateTime = null;
        for (String key : LDT_KEYS) {
            dateTime = map.get(key);
            if (dateTime != null && (!(dateTime instanceof String) || StringUtilities.hasContent((String) dateTime))) {
                break;
            }
        }

        // If the 'offsetDateTime' value is a non-empty String, parse it (allows for value, _v, epochMillis as String)
        if (dateTime instanceof String && StringUtilities.hasContent((String) dateTime)) {
            return StringConversions.toLocalDateTime(dateTime, converter);
        }

        if (dateTime instanceof Number) {
            return NumberConversions.toLocalDateTime(dateTime, converter);
        }

        return fromMap(from, converter, LocalDateTime.class, new String[] {LOCAL_DATE_TIME}, new String[] {EPOCH_MILLIS});
    }

    private static final String[] ZDT_KEYS = {ZONED_DATE_TIME, VALUE, V, EPOCH_MILLIS};

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object dateTime = null;
        for (String key : ZDT_KEYS) {
            dateTime = map.get(key);
            if (dateTime != null && (!(dateTime instanceof String) || StringUtilities.hasContent((String) dateTime))) {
                break;
            }
        }

        // If the 'zonedDateTime' value is a non-empty String, parse it (allows for value, _v, epochMillis as String)
        if (dateTime instanceof String && StringUtilities.hasContent((String) dateTime)) {
            return StringConversions.toZonedDateTime(dateTime, converter);
        }

        // Otherwise, if epoch_millis is provided, use it with the nanos (if any)
        if (dateTime instanceof Number) {
            return NumberConversions.toZonedDateTime(dateTime, converter);
        }

        return fromMap(from, converter, ZonedDateTime.class, new String[] {ZONED_DATE_TIME}, new String[] {EPOCH_MILLIS});
    }

    static Class<?> toClass(Object from, Converter converter) {
        return fromMap(from, converter, Class.class);
    }

    static Duration toDuration(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object seconds = map.get(SECONDS);
        if (seconds != null) {
            long sec = converter.convert(seconds, long.class);
            int nanos = converter.convert(map.get(NANOS), int.class);
            return Duration.ofSeconds(sec, nanos);
        }
        return fromMap(from, converter, Duration.class, new String[] {SECONDS, NANOS + OPTIONAL});
    }

    static Instant toInstant(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object seconds = map.get(SECONDS);
        if (seconds != null) {
            long sec = converter.convert(seconds, long.class);
            long nanos = converter.convert(map.get(NANOS), long.class);
            return Instant.ofEpochSecond(sec, nanos);
        }
        return fromMap(from, converter, Instant.class, new String[] {SECONDS, NANOS + OPTIONAL});
    }

    static MonthDay toMonthDay(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object month = map.get(MONTH);
        Object day = map.get(DAY);
        if (month != null && day != null) {
            int m = converter.convert(month, int.class);
            int d = converter.convert(day, int.class);
            return MonthDay.of(m, d);
        }
        return fromMap(from, converter, MonthDay.class, new String[] {MONTH, DAY});
    }

    static YearMonth toYearMonth(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object year = map.get(YEAR);
        Object month = map.get(MONTH);
        if (year != null && month != null) {
            int y = converter.convert(year, int.class);
            int m = converter.convert(month, int.class);
            return YearMonth.of(y, m);
        }
        return fromMap(from, converter, YearMonth.class, new String[] {YEAR, MONTH});
    }

    static Period toPeriod(Object from, Converter converter) {

        Map<String, Object> map = (Map<String, Object>) from;

        if (map.containsKey(VALUE) || map.containsKey(V)) {
            return fromMap(from, converter, Period.class, new String[] {YEARS, MONTHS, DAYS});
        }

        Number years = converter.convert(map.getOrDefault(YEARS, 0), int.class);
        Number months = converter.convert(map.getOrDefault(MONTHS, 0), int.class);
        Number days = converter.convert(map.getOrDefault(DAYS, 0), int.class);

        return Period.of(years.intValue(), months.intValue(), days.intValue());
    }

    static ZoneId toZoneId(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        Object zone = map.get(ZONE);
        if (zone != null) {
            return converter.convert(zone, ZoneId.class);
        }
        Object id = map.get(ID);
        if (id != null) {
            return converter.convert(id, ZoneId.class);
        }
        return fromMap(from, converter, ZoneId.class, new String[] {ZONE}, new String[] {ID});
    }

    static ZoneOffset toZoneOffset(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(HOURS)) {
            int hours = converter.convert(map.get(HOURS), int.class);
            int minutes = converter.convert(map.getOrDefault(MINUTES, 0), int.class);  // optional
            int seconds = converter.convert(map.getOrDefault(SECONDS, 0), int.class);  // optional
            return ZoneOffset.ofHoursMinutesSeconds(hours, minutes, seconds);
        }
        return fromMap(from, converter, ZoneOffset.class, new String[] {HOURS, MINUTES + OPTIONAL, SECONDS + OPTIONAL});
    }

    static Year toYear(Object from, Converter converter) {
        return fromMap(from, converter, Year.class, new String[] {YEAR});
    }

    static URL toURL(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        String url = (String) map.get(URL_KEY);
        if (StringUtilities.hasContent(url)) {
            return converter.convert(url, URL.class);
        }
        return fromMap(from, converter, URL.class, new String[] {URL_KEY});
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
    
    static URI toURI(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        String uri = (String) map.get(URI_KEY);
        if (StringUtilities.hasContent(uri)) {
            return converter.convert(map.get(URI_KEY), URI.class);
        }
        return fromMap(from, converter, URI.class, new String[] {URI_KEY});
    }

    static Map<String, ?> initMap(Object from, Converter converter) {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(V, from);
        return map;
    }

    private static <T> T fromMap(Object from, Converter converter, Class<T> type, String[]...keySets) {
        Map<String, Object> map = (Map<String, Object>) from;

        // For any single-key Map types, convert them
        for (String[] keys : keySets) {
            if (keys.length == 1) {
                String key = keys[0];
                if (map.containsKey(key)) {
                    return converter.convert(map.get(key), type);
                }
            }
        }
        
        if (map.containsKey(V)) {
            return converter.convert(map.get(V), type);
        }

        if (map.containsKey(VALUE)) {
            return converter.convert(map.get(VALUE), type);
        }

        StringBuilder builder = new StringBuilder("To convert from Map to '" + Converter.getShortName(type) + "' the map must include: ");

        for (String[] keySet : keySets) {
            builder.append("[");
            // Convert the inner String[] to a single string, joined by ", "
            builder.append(String.join(", ", keySet));
            builder.append("]");
            builder.append(", ");
        }

        builder.append("[value]");
        if (keySets.length > 0) {
            builder.append(",");
        }
        builder.append(" or [_v] as keys with associated values.");
        throw new IllegalArgumentException(builder.toString());
    }
}
