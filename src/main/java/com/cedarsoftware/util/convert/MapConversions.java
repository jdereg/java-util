package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
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
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.cedarsoftware.util.ArrayUtilities;
import com.cedarsoftware.util.CompactLinkedMap;
import com.cedarsoftware.util.Convention;

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
    static final String TIME = "time";
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
    static final String MILLI_SECONDS = "millis";
    static final String NANO = "nano";
    static final String NANOS = "nanos";
    static final String OFFSET_HOUR = "offsetHour";
    static final String OFFSET_MINUTE = "offsetMinute";
    static final String MOST_SIG_BITS = "mostSigBits";
    static final String LEAST_SIG_BITS = "leastSigBits";

    static final String OFFSET = "offset";

    private static final String TOTAL_SECONDS = "totalSeconds";

    static final String DATE_TIME = "dateTime";

    private static final String ID = "id";
    public static final String LANGUAGE = "language";
    public static final String VARIANT = "variant";
    public static final String JAR = "jar";
    public static final String AUTHORITY = "authority";
    public static final String REF = "ref";
    public static final String PORT = "port";
    public static final String FILE = "file";
    public static final String HOST = "host";
    public static final String PROTOCOL = "protocol";
    private static String COUNTRY = "country";

    private MapConversions() {}
    
    public static final String KEY_VALUE_ERROR_MESSAGE = "To convert from Map to %s the map must include one of the following: %s[_v], or [value] with associated values.";

    static Object toUUID(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;

        if (map.containsKey(MOST_SIG_BITS) && map.containsKey(LEAST_SIG_BITS)) {
            long most = converter.convert(map.get(MOST_SIG_BITS), long.class);
            long least = converter.convert(map.get(LEAST_SIG_BITS), long.class);
            return new UUID(most, least);
        }

        return fromMap(from, converter, UUID.class, MOST_SIG_BITS, LEAST_SIG_BITS);
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

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return fromMap(from, converter, java.sql.Date.class, EPOCH_MILLIS);
    }

    static Date toDate(Object from, Converter converter) {
        return fromMap(from, converter, Date.class, EPOCH_MILLIS);
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(TIME)) {
            long time = converter.convert(map.get(TIME), long.class);
            int ns = converter.convert(map.get(NANOS), int.class);
            Timestamp timeStamp = new Timestamp(time);
            timeStamp.setNanos(ns);
            return timeStamp;
        }

        return fromMap(map, converter, Timestamp.class, TIME, NANOS);
    }

    static TimeZone toTimeZone(Object from, Converter converter) {
        return fromMap(from, converter, TimeZone.class, ZONE);
    }

    static Calendar toCalendar(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(DATE) && map.containsKey(TIME)) {
            LocalDate localDate = converter.convert(map.get(DATE), LocalDate.class);
            LocalTime localTime = converter.convert(map.get(TIME), LocalTime.class);
            ZoneId zoneId;
            if (map.containsKey(ZONE)) {
                zoneId = converter.convert(map.get(ZONE), ZoneId.class);
            } else {
                zoneId = converter.getOptions().getZoneId();
            }
            LocalDateTime ldt = LocalDateTime.of(localDate, localTime);
            ZonedDateTime zdt = ldt.atZone(zoneId);
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zoneId));
            cal.set(Calendar.YEAR, zdt.getYear());
            cal.set(Calendar.MONTH, zdt.getMonthValue() - 1);
            cal.set(Calendar.DAY_OF_MONTH, zdt.getDayOfMonth());
            cal.set(Calendar.HOUR_OF_DAY, zdt.getHour());
            cal.set(Calendar.MINUTE, zdt.getMinute());
            cal.set(Calendar.SECOND, zdt.getSecond());
            cal.set(Calendar.MILLISECOND, zdt.getNano() / 1_000_000);
            cal.getTime();
            return cal;
        }
        return fromMap(from, converter, Calendar.class, DATE, TIME, ZONE);
    }

    static Locale toLocale(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;

        if (map.containsKey(VALUE) || map.containsKey(V)) {
            return fromMap(map, converter, Locale.class, LANGUAGE);
        }

        String language = converter.convert(map.get(LANGUAGE), String.class);
        if (language == null) {
            throw new IllegalArgumentException("java.util.Locale must specify 'language' field");
        }
        String country = converter.convert(map.get(COUNTRY), String.class);
        String variant = converter.convert(map.get(VARIANT), String.class);

        if (country == null) {
            return new Locale(language);
        }
        if (variant == null) {
            return new Locale(language, country);
        }

        return new Locale(language, country, variant);
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return fromMap(from, converter, LocalDate.class, DATE);
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        return fromMap(from, converter, LocalTime.class, TIME);
    }

    static OffsetTime toOffsetTime(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(HOUR) && map.containsKey(MINUTE)) {
            int hour = converter.convert(map.get(HOUR), int.class);
            int minute = converter.convert(map.get(MINUTE), int.class);
            int second = converter.convert(map.get(SECOND), int.class);
            int nano = converter.convert(map.get(NANO), int.class);
            int offsetHour = converter.convert(map.get(OFFSET_HOUR), int.class);
            int offsetMinute = converter.convert(map.get(OFFSET_MINUTE), int.class);
            ZoneOffset zoneOffset = ZoneOffset.ofHoursMinutes(offsetHour, offsetMinute);
            return OffsetTime.of(hour, minute, second, nano, zoneOffset);
        }
        return fromMap(from, converter, OffsetTime.class, HOUR, MINUTE, SECOND, NANO, OFFSET_HOUR, OFFSET_MINUTE);
    }

    private static final String[] OFFSET_DATE_TIME_PARAMS = new String[] { DATE, TIME, OFFSET };
    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(DATE) && map.containsKey(TIME)) {
            LocalDate date = converter.convert(map.get(DATE), LocalDate.class);
            LocalTime time = converter.convert(map.get(TIME), LocalTime.class);
            ZoneOffset zoneOffset = converter.convert(map.get(OFFSET), ZoneOffset.class);
            return OffsetDateTime.of(date, time, zoneOffset);
        }
        return fromMap(from, converter, OffsetDateTime.class, OFFSET_DATE_TIME_PARAMS);
    }

    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(DATE)) {
            LocalDate localDate = converter.convert(map.get(DATE), LocalDate.class);
            LocalTime localTime = map.containsKey(TIME) ? converter.convert(map.get(TIME), LocalTime.class) : LocalTime.MIDNIGHT;
            // validate date isn't null?
            return LocalDateTime.of(localDate, localTime);
        }
        return fromMap(from, converter, LocalDateTime.class, DATE, TIME);
    }

    static ZonedDateTime toZonedDateTime(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(ZONE) && map.containsKey(DATE_TIME)) {
            ZoneId zoneId = converter.convert(map.get(ZONE), ZoneId.class);
            LocalDateTime localDateTime = converter.convert(map.get(DATE_TIME), LocalDateTime.class);
            return ZonedDateTime.of(localDateTime, zoneId);
        }
        return fromMap(from, converter, ZonedDateTime.class, ZONE, DATE_TIME);
    }

    static Class<?> toClass(Object from, Converter converter) {
        return fromMap(from, converter, Class.class);
    }

    static Duration toDuration(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(SECONDS)) {
            long sec = converter.convert(map.get(SECONDS), long.class);
            int nanos = converter.convert(map.get(NANOS), int.class);
            return Duration.ofSeconds(sec, nanos);
        }
        return fromMap(from, converter, Duration.class, SECONDS, NANOS);
    }

    static Instant toInstant(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(SECONDS)) {
            long sec = converter.convert(map.get(SECONDS), long.class);
            long nanos = converter.convert(map.get(NANOS), long.class);
            return Instant.ofEpochSecond(sec, nanos);
        }
        return fromMap(from, converter, Instant.class, SECONDS, NANOS);
    }

    static MonthDay toMonthDay(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(MONTH) && map.containsKey(DAY)) {
            int month = converter.convert(map.get(MONTH), int.class);
            int day = converter.convert(map.get(DAY), int.class);
            return MonthDay.of(month, day);
        }
        return fromMap(from, converter, MonthDay.class, MONTH, DAY);
    }

    static YearMonth toYearMonth(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(YEAR) && map.containsKey(MONTH)) {
            int year = converter.convert(map.get(YEAR), int.class);
            int month = converter.convert(map.get(MONTH), int.class);
            return YearMonth.of(year, month);
        }
        return fromMap(from, converter, YearMonth.class, YEAR, MONTH);
    }

    static Period toPeriod(Object from, Converter converter) {

        Map<String, Object> map = (Map<String, Object>) from;

        if (map.containsKey(VALUE) || map.containsKey(V)) {
            return fromMap(from, converter, Period.class, YEARS, MONTHS, DAYS);
        }

        Number years = converter.convert(map.getOrDefault(YEARS, 0), int.class);
        Number months = converter.convert(map.getOrDefault(MONTHS, 0), int.class);
        Number days = converter.convert(map.getOrDefault(DAYS, 0), int.class);

        return Period.of(years.intValue(), months.intValue(), days.intValue());
    }

    static ZoneId toZoneId(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(ZONE)) {
            ZoneId zoneId = converter.convert(map.get(ZONE), ZoneId.class);
            return zoneId;
        } else if (map.containsKey(ID)) {
            ZoneId zoneId = converter.convert(map.get(ID), ZoneId.class);
            return zoneId;
        }
        return fromMap(from, converter, ZoneId.class, ZONE);
    }

    static ZoneOffset toZoneOffset(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>) from;
        if (map.containsKey(HOURS)) {
            int hours = converter.convert(map.get(HOURS), int.class);
            int minutes = converter.convert(map.getOrDefault(MINUTES, 0), int.class);  // optional
            int seconds = converter.convert(map.getOrDefault(SECONDS, 0), int.class);  // optional
            return ZoneOffset.ofHoursMinutesSeconds(hours, minutes, seconds);
        }
        return fromMap(from, converter, ZoneOffset.class, HOURS, MINUTES, SECONDS);
    }

    static Year toYear(Object from, Converter converter) {
        return fromMap(from, converter, Year.class, YEAR);
    }

    static URL toURL(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>)from;
        StringBuilder builder = new StringBuilder(20);

        try {
            if (map.containsKey(VALUE) || map.containsKey(V)) {
                return fromMap(map, converter, URL.class);
            }

            String protocol = (String) map.get(PROTOCOL);
            String host = (String) map.get(HOST);
            String file = (String) map.get(FILE);
            String authority = (String) map.get(AUTHORITY);
            String ref = (String) map.get(REF);
            Long port = (Long) map.get(PORT);

            builder.append(protocol);
            builder.append(':');
            if (!protocol.equalsIgnoreCase(JAR)) {
                builder.append("//");
            }
            if (authority != null && !authority.isEmpty()) {
                builder.append(authority);
            } else {
                if (host != null && !host.isEmpty()) {
                    builder.append(host);
                }
                if (!port.equals(-1L)) {
                    builder.append(":" + port);
                }
            }
            if (file != null && !file.isEmpty()) {
                builder.append(file);
            }
            if (ref != null && !ref.isEmpty()) {
                builder.append("#" + ref);
            }
            return URI.create(builder.toString()).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Cannot convert Map to URL.  Malformed URL:  '" + builder + "'");
        }
    }

    static URI toURI(Object from, Converter converter) {
        return fromMap(from, converter, URI.class);
    }

    static Map<String, ?> initMap(Object from, Converter converter) {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put(V, from);
        return map;
    }

    private static <T> T fromMap(Object from, Converter converter, Class<T> type, String...keys) {
        Map<?, ?> map = asMap(from);
        if (keys.length == 1) {
            String key = keys[0];
            if (map.containsKey(key)) {
                return converter.convert(map.get(key), type);
            }
        }
        if (map.containsKey(V)) {
            return converter.convert(map.get(V), type);
        }

        if (map.containsKey(VALUE)) {
            return converter.convert(map.get(VALUE), type);
        }

        String keyText = ArrayUtilities.isEmpty(keys) ? "" : "[" + String.join(", ", keys) + "], ";
        throw new IllegalArgumentException(String.format(KEY_VALUE_ERROR_MESSAGE, Converter.getShortName(type), keyText));
    }
    
    private static Map<?, ?> asMap(Object o) {
        Convention.throwIfFalse(o instanceof Map, "from must be an instance of map");
        return (Map<?, ?>)o;
    }

    static Map<?, ?> toMap(Object from, Converter converter) {
        Map<?, ?> source = (Map<?, ?>) from;
        Map<?, ?> copy = new LinkedHashMap<>(source);
        return copy;
    }
}
