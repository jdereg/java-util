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
    static final String TIME = "time";
    static final String ZONE = "zone";
    static final String YEAR = "year";
    static final String YEARS = "years";
    static final String MONTH = "month";
    static final String MONTHS = "months";
    static final String DAY = "day";
    static final String DAYS = "days";
    static final String HOURS = "hours";
    static final String MINUTES = "minutes";
    static final String SECONDS = "seconds";
    static final String EPOCH_MILLIS = "epochMillis";
    static final String NANOS = "nanos";
    static final String MOST_SIG_BITS = "mostSigBits";
    static final String LEAST_SIG_BITS = "leastSigBits";
    static final String OFFSET = "offset";
    static final String DATE_TIME = "dateTime";
    private static final String ID = "id";
    static final String LANGUAGE = "language";
    static final String COUNTRY = "country";
    static final String SCRIPT = "script";
    static final String VARIANT = "variant";
    static final String URI_KEY = "URI";
    static final String URL_KEY = "URL";
    static final String UUID = "UUID";

    private MapConversions() {}
    
    public static final String KEY_VALUE_ERROR_MESSAGE = "To convert from Map to %s the map must include one of the following: %s[_v], or [value] with associated values.";

    static Object toUUID(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;

        if (map.containsKey(MapConversions.UUID)) {
            return converter.convert(map.get(UUID), UUID.class);
        }

        if (map.containsKey(MOST_SIG_BITS) && map.containsKey(LEAST_SIG_BITS)) {
            long most = converter.convert(map.get(MOST_SIG_BITS), long.class);
            long least = converter.convert(map.get(LEAST_SIG_BITS), long.class);
            return new UUID(most, least);
        }

        return fromMap(from, converter, UUID.class, UUID, MOST_SIG_BITS, LEAST_SIG_BITS);
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
        if (map.containsKey(EPOCH_MILLIS)) {
            long time = converter.convert(map.get(EPOCH_MILLIS), long.class);
            int ns = converter.convert(map.get(NANOS), int.class);
            Timestamp timeStamp = new Timestamp(time);
            timeStamp.setNanos(ns);
            return timeStamp;
        }

        return fromMap(map, converter, Timestamp.class, EPOCH_MILLIS, NANOS);
    }

    static TimeZone toTimeZone(Object from, Converter converter) {
        return fromMap(from, converter, TimeZone.class, ZONE);
    }

    static Calendar toCalendar(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(EPOCH_MILLIS)) {
            return converter.convert(map.get(EPOCH_MILLIS), Calendar.class);
        } else if (map.containsKey(DATE) && map.containsKey(TIME)) {
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
        Map<String, String> map = (Map<String, String>) from;

        String language = converter.convert(map.get(LANGUAGE), String.class);
        if (StringUtilities.isEmpty(language)) {
            return fromMap(from, converter, Locale.class, LANGUAGE, COUNTRY, SCRIPT, VARIANT);
        }
        String country = converter.convert(map.get(COUNTRY), String.class);
        String script = converter.convert(map.get(SCRIPT), String.class);
        String variant = converter.convert(map.get(VARIANT), String.class);

        Locale.Builder builder = new Locale.Builder();
        builder.setLanguage(language);
        if (StringUtilities.hasContent(country)) {
            builder.setRegion(country);
        }
        if (StringUtilities.hasContent(script)) {
            builder.setScript(script);
        }
        if (StringUtilities.hasContent(variant)) {
            builder.setVariant(variant);
        }
        return builder.build();
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return fromMap(from, converter, LocalDate.class, DATE);
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        return fromMap(from, converter, LocalTime.class, TIME);
    }

    static OffsetTime toOffsetTime(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(TIME)) {
            String ot = (String) map.get(TIME);
            try {
                return OffsetTime.parse(ot);
            } catch (Exception e) {
                throw new IllegalArgumentException("Unable to parse OffsetTime: " + ot, e);
            }
        }
        return fromMap(from, converter, OffsetTime.class, TIME);
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        Map<?, ?> map = (Map<?, ?>) from;
        if (map.containsKey(DATE) && map.containsKey(TIME)) {
            LocalDate date = converter.convert(map.get(DATE), LocalDate.class);
            LocalTime time = converter.convert(map.get(TIME), LocalTime.class);
            ZoneOffset zoneOffset = converter.convert(map.get(OFFSET), ZoneOffset.class);
            return OffsetDateTime.of(date, time, zoneOffset);
        }
        return fromMap(from, converter, OffsetDateTime.class, DATE, TIME, OFFSET);
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
            return converter.convert(map.get(ZONE), ZoneId.class);
        } else if (map.containsKey(ID)) {
            return converter.convert(map.get(ID), ZoneId.class);
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
        String url = (String) map.get(URL_KEY);
        if (StringUtilities.isEmpty(url)) {
            throw new IllegalArgumentException("null or empty string cannot be used to create URL");
        }
        try {
            return URI.create(url).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Unable to create URL from: " + url, e);
        }
    }

    static URI toURI(Object from, Converter converter) {
        Map<String, Object> map = (Map<String, Object>)from;
        String uri = (String) map.get(URI_KEY);
        if (StringUtilities.isEmpty(uri)) {
            throw new IllegalArgumentException("null or empty string cannot be used to create URI");
        }
        try {
            return URI.create(uri);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to create URI from: " + uri, e);
        }
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

}
