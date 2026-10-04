package com.cedarsoftware.util.convert;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * java.sql.Time conversions (4.113.0). A Time is JDBC's time of day, written "HH:mm:ss" (as Time.toString()), with
 * ".SSS" when it has milliseconds.
 * <p>
 * Before, Time had no conversions of its own: converting anything to a Time returned a java.util.Date (the Date
 * conversion, found through Time's superclass), "13:14:15" and a LocalTime could not become one, and a Time could not
 * become a String, an Instant or a Map (Time.toInstant() throws UnsupportedOperationException).
 * <p>
 * The pairs are in ConverterEverythingTest too (loadSqlTimeTests), round-tripped through the test-scope json-io
 * 4.112.0; this class keeps the checks a pair cannot state: the result is a Time (not a Date) from every source,
 * and a copy is a new object.
 */
class SqlTimeConversionsTest {
    private final Converter converter = new Converter(new DefaultConverterOptions());

    private static final Time T = Time.valueOf("13:14:15");
    private static final Time T123 = new Time(T.getTime() + 123);

    @Test
    void everySourceThatReachesADateReachesATimeAsATime() {
        // One value of each source the Converter turns into a Date; each must now become a Time, of the same instant
        Map<Object, String> sources = new HashMap<>();
        sources.put(T123.getTime(), "Long");
        sources.put(T123.getTime() / 1000.0, "Double");
        sources.put(java.math.BigInteger.valueOf(T123.getTime()), "BigInteger");
        sources.put(java.math.BigDecimal.valueOf(T123.getTime(), 3), "BigDecimal");
        sources.put(new java.util.concurrent.atomic.AtomicLong(T123.getTime()), "AtomicLong");
        sources.put(new Date(T123.getTime()), "Date");
        sources.put(new Timestamp(T123.getTime()), "Timestamp");
        sources.put(java.sql.Date.valueOf("2026-09-27"), "java.sql.Date");
        sources.put(Instant.ofEpochMilli(T123.getTime()), "Instant");
        sources.put(ZonedDateTime.of(2026, 9, 27, 13, 14, 15, 0, ZoneId.of("Asia/Tokyo")), "ZonedDateTime");
        sources.put(ZonedDateTime.of(2026, 9, 27, 13, 14, 15, 0, ZoneId.of("Asia/Tokyo")).toOffsetDateTime(),
                "OffsetDateTime");
        sources.put(java.time.LocalDateTime.of(2026, 9, 27, 13, 14, 15), "LocalDateTime");
        sources.put(java.time.LocalDate.of(2026, 9, 27), "LocalDate");
        sources.put(java.time.Duration.ofMillis(T123.getTime()), "Duration");
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(T123.getTime());
        sources.put(calendar, "Calendar");

        for (Map.Entry<Object, String> source : sources.entrySet()) {
            Object time = converter.convert(source.getKey(), Time.class);
            assertSame(Time.class, time.getClass(), source.getValue() + " -> Time");
            Date date = converter.convert(source.getKey(), Date.class);
            assertEquals(date.getTime(), ((Time) time).getTime(), source.getValue() + ": the same instant as its Date");
        }
        assertNull(converter.convert(null, Time.class));
    }

    @Test
    void epochMillisRoundTripExactly() {
        assertEquals(T123, converter.convert(T123.getTime(), Time.class));
        assertEquals(T123.getTime(), (long) converter.convert(T123, Long.class));
        Time copy = converter.convert(T123, Time.class);
        assertEquals(T123, copy);
        assertTrue(copy != T123, "a copy, as for every mutable Date");
    }

    @Test
    void stringsAreTimesOfDay() {
        assertEquals("13:14:15", converter.convert(T, String.class));
        assertEquals("13:14:15.123", converter.convert(T123, String.class));
        assertEquals("13:14:15.005", converter.convert(new Time(T.getTime() + 5), String.class));
        assertEquals("13:14:15.050", converter.convert(new Time(T.getTime() + 50), String.class));

        assertEquals(T, converter.convert("13:14:15", Time.class));
        assertEquals(T123, converter.convert("13:14:15.123", Time.class));
        assertEquals(Time.valueOf("13:14:00"), converter.convert("13:14", Time.class));
        assertEquals(T, converter.convert("  13:14:15  ", Time.class));
        // A date-time keeps its time of day, in its own zone
        assertEquals(T, converter.convert("2026-09-27T13:14:15+09:00", Time.class));
        assertNull(converter.convert("", Time.class));

        // What a CharSequence, StringBuilder or StringBuffer holds, too
        assertEquals(T123, converter.convert(new StringBuilder("13:14:15.123"), Time.class));
        assertEquals("13:14:15.123", converter.convert(T123, StringBuilder.class).toString());
    }

    @Test
    void localTimesKeepTheirMilliseconds() {
        assertEquals(LocalTime.of(13, 14, 15, 123_000_000), converter.convert(T123, LocalTime.class));
        assertEquals(T123, converter.convert(LocalTime.of(13, 14, 15, 123_456_789), Time.class));
        assertEquals(T, converter.convert(LocalTime.of(13, 14, 15), Time.class));
    }

    @Test
    void mapsHoldTheTimeOfDayAndReadTheOldJsonIoForm() {
        Map<?, ?> map = converter.convert(T123, Map.class);
        assertEquals(1, map.size());
        assertEquals("13:14:15.123", map.get("sqlTime"));
        assertEquals(T123, converter.convert(map, Time.class));

        assertEquals(T, converter.convert(mapOf("value", "13:14:15"), Time.class));
        assertEquals(T123, converter.convert(mapOf("epochMillis", T123.getTime()), Time.class));
        // json-io wrote a Time as {"date": epochMillis} before Time had conversions
        assertEquals(T123, converter.convert(mapOf("date", T123.getTime()), Time.class));
    }

    @Test
    void aTimeReachesEveryTargetADateDoes() {
        // Time.toInstant() throws UnsupportedOperationException, and the Date conversions that used it failed
        Set<Class<?>> dateTargets = Converter.allSupportedConversions().get(Date.class);
        for (Class<?> target : dateTargets) {
            if (!target.isPrimitive()) {
                converter.convert(T123, target);   // must not throw
            }
        }
        assertEquals(Instant.ofEpochMilli(T123.getTime()), converter.convert(T123, Instant.class));
    }

    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
